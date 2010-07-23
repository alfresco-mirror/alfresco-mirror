/*
 * Copyright (C) 2005-2010 Alfresco Software Limited.
 *
 * This file is part of Alfresco
 *
 * Alfresco is free software: you can redistribute it and/or modify
 * it under the terms of the GNU Lesser General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * Alfresco is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU Lesser General Public License for more details.
 *
 * You should have received a copy of the GNU Lesser General Public License
 * along with Alfresco. If not, see <http://www.gnu.org/licenses/>.
 */
package org.alfresco.repo.action;

import static org.alfresco.repo.action.ActionServiceImplTest.assertBefore;

import java.util.Date;

import javax.transaction.UserTransaction;

import junit.framework.TestCase;

import org.alfresco.model.ContentModel;
import org.alfresco.repo.action.ActionServiceImplTest.CancellableSleepAction;
import org.alfresco.repo.action.ActionServiceImplTest.SleepActionExecuter;
import org.alfresco.repo.action.executer.MoveActionExecuter;
import org.alfresco.repo.cache.EhCacheAdapter;
import org.alfresco.repo.content.MimetypeMap;
import org.alfresco.repo.security.authentication.AuthenticationUtil;
import org.alfresco.repo.transaction.RetryingTransactionHelper;
import org.alfresco.service.cmr.action.Action;
import org.alfresco.service.cmr.action.ActionService;
import org.alfresco.service.cmr.action.ActionStatus;
import org.alfresco.service.cmr.action.ActionTrackingService;
import org.alfresco.service.cmr.action.ExecutionDetails;
import org.alfresco.service.cmr.action.ExecutionSummary;
import org.alfresco.service.cmr.repository.ContentData;
import org.alfresco.service.cmr.repository.NodeRef;
import org.alfresco.service.cmr.repository.NodeService;
import org.alfresco.service.cmr.repository.StoreRef;
import org.alfresco.service.namespace.QName;
import org.alfresco.service.transaction.TransactionService;
import org.alfresco.util.ApplicationContextHelper;
import org.springframework.context.ConfigurableApplicationContext;

/**
 * Action tracking service tests. These mostly need
 *  careful control over the transactions they use.
 * 
 * @author Nick Burch
 */
public class ActionTrackingServiceImplTest extends TestCase
{
    private static ConfigurableApplicationContext ctx = 
       (ConfigurableApplicationContext)ApplicationContextHelper.getApplicationContext();
    
    private StoreRef storeRef;
    private NodeRef rootNodeRef;
    
    private NodeRef nodeRef;
    private NodeRef folder;
    private NodeService nodeService;
    private ActionService actionService;
    private TransactionService transactionService;
    private RuntimeActionService runtimeActionService;
    private ActionTrackingService actionTrackingService;
    private EhCacheAdapter<String, ExecutionDetails> executingActionsCache;
    
    @Override
    protected void setUp() throws Exception {
        this.nodeService = (NodeService)ctx.getBean("nodeService");
        this.actionService = (ActionService)ctx.getBean("actionService");
        this.runtimeActionService = (RuntimeActionService)ctx.getBean("actionService");
        this.actionTrackingService = (ActionTrackingService)ctx.getBean("actionTrackingService");
        this.transactionService = (TransactionService)ctx.getBean("transactionService");
        this.executingActionsCache = (EhCacheAdapter<String, ExecutionDetails>)ctx.getBean("executingActionsSharedCache");

        AuthenticationUtil.setRunAsUserSystem();
        
        UserTransaction txn = transactionService.getUserTransaction();
        txn.begin();
        
        // Where to put things
        this.storeRef = this.nodeService.createStore(StoreRef.PROTOCOL_WORKSPACE, "Test_" + System.currentTimeMillis());
        this.rootNodeRef = this.nodeService.getRootNode(this.storeRef);
        
        // Create the node used for tests
        this.nodeRef = this.nodeService.createNode(
                this.rootNodeRef,
                ContentModel.ASSOC_CHILDREN,
                QName.createQName("{test}testnode"),
                ContentModel.TYPE_CONTENT).getChildRef();
        this.nodeService.setProperty(
                this.nodeRef,
                ContentModel.PROP_CONTENT,
                new ContentData(null, MimetypeMap.MIMETYPE_TEXT_PLAIN, 0L, null));
        this.folder = this.nodeService.createNode(
                this.rootNodeRef,
                ContentModel.ASSOC_CHILDREN,
                QName.createQName("{test}testFolder"),
                ContentModel.TYPE_FOLDER).getChildRef();
        
        txn.commit();
        
        // Cache should start empty each time
        for(String key : executingActionsCache.getKeys()) {
           executingActionsCache.remove(key);
        }
        
        // Reset the execution instance IDs, so we
        //  can predict what they'll be
        ((ActionTrackingServiceImpl)actionTrackingService).resetNextExecutionId();
        
        // Register the test executor, if needed
        SleepActionExecuter.registerIfNeeded(ctx);
    }

    /** Creating cache keys */
    public void testCreateCacheKeys() throws Exception
    {
       ActionImpl action = (ActionImpl)createWorkingSleepAction("1234");
       assertEquals("sleep-action", action.getActionDefinitionName());
       assertEquals("1234", action.getId());
       assertEquals(-1, action.getExecutionInstance());
       
       // Give it a predictable execution instance
       action.setExecutionInstance(1);
       
       // From an action
       String key = ActionTrackingServiceImpl.generateCacheKey(action);
       assertEquals("sleep-action=1234=1", key);
       
       // From an ExecutionSummary
       ExecutionSummary s = new ExecutionSummary("sleep-action", "1234", 1);
       key = ActionTrackingServiceImpl.generateCacheKey(s);
       assertEquals("sleep-action=1234=1", key);
    }
    
    /** Creating ExecutionDetails and ExecutionSummary */
    public void testExecutionDetailsSummary() throws Exception
    {
       // Create an action with a known execution instance
       Action action = createWorkingSleepAction("1234");
       ((ActionImpl)action).setExecutionInstance(1);
       
       // Create the ExecutionSummary from an action
       String key = ActionTrackingServiceImpl.generateCacheKey(action);
       
       ExecutionSummary s = ActionTrackingServiceImpl.buildExecutionSummary(action);
       assertEquals("sleep-action", s.getActionType());
       assertEquals("1234", s.getActionId());
       assertEquals(1, s.getExecutionInstance());
       
       // Create the ExecutionSummery from a key
       s = ActionTrackingServiceImpl.buildExecutionSummary(key);
       assertEquals("sleep-action", s.getActionType());
       assertEquals("1234", s.getActionId());
       assertEquals(1, s.getExecutionInstance());
       
       // Now create ExecutionDetails
       ExecutionDetails d = ActionTrackingServiceImpl.buildExecutionDetails(action);
       assertNotNull(d.getExecutionSummary());
       assertEquals("sleep-action", d.getActionType());
       assertEquals("1234", d.getActionId());
       assertEquals(1, d.getExecutionInstance());
       assertEquals(null, d.getPersistedActionRef());
       assertEquals(null, d.getStartedAt());
       
       // TODO Check machine details
    }
    
    /** Running an action gives it an execution ID */
    public void testExecutionInstanceAssignment()
    {
       // TODO
    }
    
    /** 
     * The correct things happen with the cache
     *  when you mark things as working / failed / etc 
     */
    public void testInOutCache() throws Exception
    {
       Action action = createWorkingSleepAction("1234");
       assertEquals(ActionStatus.New, action.getExecutionStatus());
       
       String key = ActionTrackingServiceImpl.generateCacheKey(action);
       assertEquals(null, executingActionsCache.get(key));
       
       
       // Can complete or fail, won't be there
       actionTrackingService.recordActionComplete(action);
       key = ActionTrackingServiceImpl.generateCacheKey(action);
       assertEquals(ActionStatus.Completed, action.getExecutionStatus());
       assertEquals(null, executingActionsCache.get(key));
       
       actionTrackingService.recordActionFailure(action, new Exception("Testing"));
       key = ActionTrackingServiceImpl.generateCacheKey(action);
       assertEquals(ActionStatus.Failed, action.getExecutionStatus());
       assertEquals("Testing", action.getExecutionFailureMessage());
       assertEquals(null, executingActionsCache.get(key));
       
       
       // Pending won't add it in either
       actionTrackingService.recordActionPending(action);
       key = ActionTrackingServiceImpl.generateCacheKey(action);
       assertEquals(ActionStatus.Pending, action.getExecutionStatus());
       assertEquals(null, executingActionsCache.get(key));
       
       
       // Run it, will go into the cache
       actionTrackingService.recordActionExecuting(action);
       key = ActionTrackingServiceImpl.generateCacheKey(action);
       assertEquals(ActionStatus.Running, action.getExecutionStatus());
       assertNotNull(null, executingActionsCache.get(key));
       
       ExecutionSummary s = ActionTrackingServiceImpl.buildExecutionSummary(action);
       ExecutionDetails d = actionTrackingService.getExecutionDetails(s);
       assertNotNull(d.getExecutionSummary());
       assertEquals("sleep-action", d.getActionType());
       assertEquals("1234", d.getActionId());
       assertEquals(1, d.getExecutionInstance());
       assertEquals(null, d.getPersistedActionRef());
       assertNotNull(null, d.getStartedAt());
       
       
       // Completion removes it
       actionTrackingService.recordActionComplete(action);
       key = ActionTrackingServiceImpl.generateCacheKey(action);
       assertEquals(ActionStatus.Completed, action.getExecutionStatus());
       assertEquals(null, executingActionsCache.get(key));
       
       // Failure removes it
       actionTrackingService.recordActionExecuting(action);
       key = ActionTrackingServiceImpl.generateCacheKey(action);
       assertNotNull(null, executingActionsCache.get(key));
       
       actionTrackingService.recordActionFailure(action, new Exception("Testing"));
       key = ActionTrackingServiceImpl.generateCacheKey(action);
       assertEquals(ActionStatus.Failed, action.getExecutionStatus());
       assertEquals("Testing", action.getExecutionFailureMessage());
       assertEquals(null, executingActionsCache.get(key));
    }
    
    /** Working actions go into the cache, then out */
    public void testWorkingActions() throws Exception 
    {
       final SleepActionExecuter sleepActionExec = 
          (SleepActionExecuter)ctx.getBean(SleepActionExecuter.NAME);
       sleepActionExec.setSleepMs(10000);

       // Have it run asynchronously
       UserTransaction txn = transactionService.getUserTransaction();
       txn.begin();
       Action action = createWorkingSleepAction("54321");
       assertNull(action.getExecutionStartDate());
       assertNull(action.getExecutionEndDate());
       assertNull(action.getExecutionFailureMessage());
       assertEquals(ActionStatus.New, action.getExecutionStatus());
       
       String key = ActionTrackingServiceImpl.generateCacheKey(action);
       assertEquals(null, executingActionsCache.get(key));
       
       this.actionService.executeAction(action, this.nodeRef, false, true);
       
       
       // End the transaction. Should allow the async action
       //  to be started
       txn.commit();
       Thread.sleep(150);

       
       // Will get an execution instance id, so a new key
       key = ActionTrackingServiceImpl.generateCacheKey(action);
       
       
       // Check it's in the cache
       System.out.println("Checking the cache for " + key);
       assertNotNull(executingActionsCache.get(key));
       
       ExecutionSummary s = ActionTrackingServiceImpl.buildExecutionSummary(action);
       ExecutionDetails d = actionTrackingService.getExecutionDetails(s);
       assertNotNull(d.getExecutionSummary());
       assertEquals("sleep-action", d.getActionType());
       assertEquals("54321", d.getActionId());
       assertEquals(1, d.getExecutionInstance());
       assertEquals(null, d.getPersistedActionRef());
       assertNotNull(null, d.getStartedAt());

       
       // Tell it to stop sleeping
       sleepActionExec.getExecutingThread().interrupt();
       Thread.sleep(100);
       
       
       // Ensure it went away again
       assertEquals(ActionStatus.Completed, action.getExecutionStatus());
       assertEquals(null, executingActionsCache.get(key));
       
       d = actionTrackingService.getExecutionDetails(s);
       assertEquals(null, d);
    }
    
    /** Failing actions go into the cache, then out */
    public void testFailingActions() throws Exception
    {
       final SleepActionExecuter sleepActionExec = 
          (SleepActionExecuter)ctx.getBean(SleepActionExecuter.NAME);
       sleepActionExec.setSleepMs(10000);

       // Have it run asynchronously
       UserTransaction txn = transactionService.getUserTransaction();
       txn.begin();
       Action action = createFailingSleepAction("54321");
       assertNull(action.getExecutionStartDate());
       assertNull(action.getExecutionEndDate());
       assertNull(action.getExecutionFailureMessage());
       assertEquals(ActionStatus.New, action.getExecutionStatus());
       
       String key = ActionTrackingServiceImpl.generateCacheKey(action);
       assertEquals(null, executingActionsCache.get(key));
       
       this.actionService.executeAction(action, this.nodeRef, false, true);
       
       
       // End the transaction. Should allow the async action
       //  to be started
       txn.commit();
       Thread.sleep(150);
       
       
       // Will get an execution instance id, so a new key
       key = ActionTrackingServiceImpl.generateCacheKey(action);
       
       
       // Check it's in the cache
       System.out.println("Checking the cache for " + key);
       assertNotNull(executingActionsCache.get(key));
       
       ExecutionSummary s = ActionTrackingServiceImpl.buildExecutionSummary(action);
       ExecutionDetails d = actionTrackingService.getExecutionDetails(s);
       assertNotNull(d.getExecutionSummary());
       assertEquals("sleep-action", d.getActionType());
       assertEquals("54321", d.getActionId());
       assertEquals(1, d.getExecutionInstance());
       assertEquals(null, d.getPersistedActionRef());
       assertNotNull(null, d.getStartedAt());

       
       // Tell it to stop sleeping
       sleepActionExec.getExecutingThread().interrupt();
       Thread.sleep(100);
       
       
       // Ensure it went away again
       assertEquals(ActionStatus.Failed, action.getExecutionStatus());
       assertEquals("Bang!", action.getExecutionFailureMessage());
       assertEquals(null, executingActionsCache.get(key));
       
       d = actionTrackingService.getExecutionDetails(s);
       assertEquals(null, d);
    }
    
    /** Ensure that the listing functions work */
    public void testListings() throws Exception
    {
       // All listings start blank
       assertEquals(
             0, actionTrackingService.getAllExecutingActions().size()
       );
       assertEquals(
             0, actionTrackingService.getExecutingActions("test").size()
       );
       assertEquals(
             0, actionTrackingService.getExecutingActions(SleepActionExecuter.NAME).size()
       );
       assertEquals(
             0, actionTrackingService.getExecutingActions(createWorkingSleepAction(null)).size()
       );
       
       // Create some actions
       Action sleepAction1 = createWorkingSleepAction("12345");
       Action sleepAction2 = createWorkingSleepAction("54321");
       Action moveAction = createFailingMoveAction();
          
       // Start putting them in
       actionTrackingService.recordActionExecuting(sleepAction1);
       assertEquals(
             1, actionTrackingService.getAllExecutingActions().size()
       );
       assertEquals(
             0, actionTrackingService.getExecutingActions("test").size()
       );
       assertEquals(
             1, actionTrackingService.getExecutingActions(SleepActionExecuter.NAME).size()
       );
       assertEquals(
             1, actionTrackingService.getExecutingActions(sleepAction1).size()
       );
       assertEquals(
             0, actionTrackingService.getExecutingActions(sleepAction2).size()
       );
       assertEquals(
             0, actionTrackingService.getExecutingActions(moveAction).size()
       );
       
       actionTrackingService.recordActionExecuting(moveAction);
       assertEquals(
             2, actionTrackingService.getAllExecutingActions().size()
       );
       assertEquals(
             0, actionTrackingService.getExecutingActions("test").size()
       );
       assertEquals(
             1, actionTrackingService.getExecutingActions(SleepActionExecuter.NAME).size()
       );
       assertEquals(
             1, actionTrackingService.getExecutingActions(sleepAction1).size()
       );
       assertEquals(
             0, actionTrackingService.getExecutingActions(sleepAction2).size()
       );
       assertEquals(
             1, actionTrackingService.getExecutingActions(moveAction).size()
       );
       
       actionTrackingService.recordActionExecuting(sleepAction2);
       assertEquals(
             3, actionTrackingService.getAllExecutingActions().size()
       );
       assertEquals(
             0, actionTrackingService.getExecutingActions("test").size()
       );
       assertEquals(
             2, actionTrackingService.getExecutingActions(SleepActionExecuter.NAME).size()
       );
       assertEquals(
             1, actionTrackingService.getExecutingActions(sleepAction1).size()
       );
       assertEquals(
             1, actionTrackingService.getExecutingActions(sleepAction2).size()
       );
       assertEquals(
             1, actionTrackingService.getExecutingActions(moveAction).size()
       );
       
       // Now have some finish, should leave the cache
       actionTrackingService.recordActionComplete(sleepAction2);
       assertEquals(
             2, actionTrackingService.getAllExecutingActions().size()
       );
       assertEquals(
             0, actionTrackingService.getExecutingActions("test").size()
       );
       assertEquals(
             1, actionTrackingService.getExecutingActions(SleepActionExecuter.NAME).size()
       );
       assertEquals(
             1, actionTrackingService.getExecutingActions(sleepAction1).size()
       );
       assertEquals(
             0, actionTrackingService.getExecutingActions(sleepAction2).size()
       );
       assertEquals(
             1, actionTrackingService.getExecutingActions(moveAction).size()
       );
       
       actionTrackingService.recordActionComplete(sleepAction1);
       assertEquals(
             1, actionTrackingService.getAllExecutingActions().size()
       );
       assertEquals(
             0, actionTrackingService.getExecutingActions("test").size()
       );
       assertEquals(
             0, actionTrackingService.getExecutingActions(SleepActionExecuter.NAME).size()
       );
       assertEquals(
             0, actionTrackingService.getExecutingActions(sleepAction1).size()
       );
       assertEquals(
             0, actionTrackingService.getExecutingActions(sleepAction2).size()
       );
       assertEquals(
             1, actionTrackingService.getExecutingActions(moveAction).size()
       );
       
       // TODO Multiple actions of the same instance
    }
    
    /** Cancel related */
    public void testCancellation() throws Exception {
       // Ensure we get the right answers checking
       CancellableSleepAction sleepAction1 = (CancellableSleepAction)createWorkingSleepAction(null);
       CancellableSleepAction sleepAction2 = (CancellableSleepAction)createWorkingSleepAction(null);
       actionTrackingService.recordActionExecuting(sleepAction1);
       actionTrackingService.recordActionExecuting(sleepAction2);
       assertEquals(false, actionTrackingService.isCancellationRequested(sleepAction1));
       assertEquals(false, actionTrackingService.isCancellationRequested(sleepAction2));

       // Cancel with the action
       actionTrackingService.requestActionCancellation(sleepAction1);
       assertEquals(true, actionTrackingService.isCancellationRequested(sleepAction1));
       assertEquals(false, actionTrackingService.isCancellationRequested(sleepAction2));
       
       // Cancel with the summary
       ExecutionSummary s2 = ActionTrackingServiceImpl.buildExecutionSummary(sleepAction2);
       actionTrackingService.requestActionCancellation(s2);
       assertEquals(true, actionTrackingService.isCancellationRequested(sleepAction1));
       assertEquals(true, actionTrackingService.isCancellationRequested(sleepAction2));
       
       
       // If the action had gone missing from the cache,
       //  then a check will put it back
       CancellableSleepAction sleepAction3 = (CancellableSleepAction)createWorkingSleepAction(null);
       String key3 = ActionTrackingServiceImpl.generateCacheKey(sleepAction3);
       
       assertNull(executingActionsCache.get(key3));
       assertEquals(false, actionTrackingService.isCancellationRequested(sleepAction3));
       assertNotNull(executingActionsCache.get(key3));
       
       executingActionsCache.remove(key3);
       assertNull(executingActionsCache.get(key3));
       assertEquals(false, actionTrackingService.isCancellationRequested(sleepAction3));
       assertNotNull(executingActionsCache.get(key3));
       
       actionTrackingService.requestActionCancellation(sleepAction3);
       assertEquals(true, actionTrackingService.isCancellationRequested(sleepAction3));
       assertNotNull(executingActionsCache.get(key3));
       
       
       // Now have one execute and cancel it, ensure it does
       final SleepActionExecuter sleepActionExec = 
          (SleepActionExecuter)ctx.getBean(SleepActionExecuter.NAME);
       sleepActionExec.setSleepMs(10000);
       
       UserTransaction txn = transactionService.getUserTransaction();
       txn.begin();
       
       executingActionsCache.remove(key3);
       this.actionService.executeAction(sleepAction3, this.nodeRef, false, true);
       
       // End the transaction. Should allow the async action
       //  to be started
       txn.commit();
       Thread.sleep(150);
       
       // Get the updated key, and check
       key3 = ActionTrackingServiceImpl.generateCacheKey(sleepAction3);
       
       assertEquals(false, actionTrackingService.isCancellationRequested(sleepAction3));
       assertNotNull(executingActionsCache.get(key3));
       
       actionTrackingService.requestActionCancellation(sleepAction3);
       
       assertEquals(true, actionTrackingService.isCancellationRequested(sleepAction3));
       assertNotNull(executingActionsCache.get(key3));
       
       // Have it finish sleeping, will have been cancelled
       sleepActionExec.getExecutingThread().interrupt();
       Thread.sleep(150);

       // Ensure the proper cancelled tracking
       assertEquals(ActionStatus.Cancelled, sleepAction3.getExecutionStatus());
       assertEquals(null, sleepAction3.getExecutionFailureMessage());
    }
    
    
    // =================================================================== //

    
    /**
     * Tests that when we run an action, either
     *  synchronously or asynchronously, with it
     *  working or failing, that the action execution
     *  service correctly sets the flags
     */
    public void testExecutionTrackingOnExecution() throws Exception {
       final SleepActionExecuter sleepActionExec = 
          (SleepActionExecuter)ctx.getBean(SleepActionExecuter.NAME);
       sleepActionExec.setSleepMs(10);
       Action action;
       NodeRef actionNode;

       // We need real transactions
       UserTransaction txn = transactionService.getUserTransaction();
       txn.begin();
       
       
       // ===========================================================
       //    Execute a transient Action that works, synchronously
       // ===========================================================
       action = createWorkingSleepAction(null);
       assertNull(action.getExecutionStartDate());
       assertNull(action.getExecutionEndDate());
       assertNull(action.getExecutionFailureMessage());
       assertEquals(ActionStatus.New, action.getExecutionStatus());
       
       this.actionService.executeAction(action, this.nodeRef);
       
       assertNotNull(action.getExecutionStartDate());
       assertNotNull(action.getExecutionEndDate());
       assertBefore(action.getExecutionStartDate(), action.getExecutionEndDate());
       assertBefore(action.getExecutionEndDate(), new Date());
       assertNull(action.getExecutionFailureMessage());
       assertEquals(ActionStatus.Completed, action.getExecutionStatus());
       
       
       // ===========================================================
       //    Execute a transient Action that fails, synchronously
       // ===========================================================
       action = createFailingMoveAction();
       assertNull(action.getExecutionStartDate());
       assertNull(action.getExecutionEndDate());
       assertNull(action.getExecutionFailureMessage());
       assertEquals(ActionStatus.New, action.getExecutionStatus());
       
       try {
          this.actionService.executeAction(action, this.nodeRef);
          fail("Action should have failed, and the error been thrown");
       } catch(Exception e) {}
       
       assertNotNull(action.getExecutionStartDate());
       assertNotNull(action.getExecutionEndDate());
       assertBefore(action.getExecutionStartDate(), action.getExecutionEndDate());
       assertBefore(action.getExecutionEndDate(), new Date());
       assertNotNull(action.getExecutionFailureMessage());
       assertEquals(ActionStatus.Failed, action.getExecutionStatus());

       // Tidy up from the action failure
       txn.rollback();
       txn = transactionService.getUserTransaction();
       txn.begin();
       
       
       // ===========================================================
       //    Execute a stored Action that works, synchronously
       // ===========================================================
       action = createWorkingSleepAction(null);
       this.actionService.saveAction(this.nodeRef, action);
       actionNode = action.getNodeRef();
       assertNotNull(actionNode);
       
       assertNull(action.getExecutionStartDate());
       assertNull(action.getExecutionEndDate());
       assertNull(action.getExecutionFailureMessage());
       assertEquals(ActionStatus.New, action.getExecutionStatus());
       
       this.actionService.executeAction(action, this.nodeRef);
       
       // Check our copy
       assertNotNull(action.getExecutionStartDate());
       assertNotNull(action.getExecutionEndDate());
       assertBefore(action.getExecutionStartDate(), action.getExecutionEndDate());
       assertBefore(action.getExecutionEndDate(), new Date());
       assertNull(action.getExecutionFailureMessage());
       assertEquals(ActionStatus.Completed, action.getExecutionStatus());
       
       // Now re-load and check the stored one
       action = runtimeActionService.createAction(actionNode);
       assertNotNull(action.getExecutionStartDate());
       assertNotNull(action.getExecutionEndDate());
       assertBefore(action.getExecutionStartDate(), action.getExecutionEndDate());
       assertBefore(action.getExecutionEndDate(), new Date());
       assertNull(action.getExecutionFailureMessage());
       assertEquals(ActionStatus.Completed, action.getExecutionStatus());

       
       // ===========================================================
       //    Execute a stored Action that fails, synchronously
       // ===========================================================
       action = createFailingMoveAction();
       this.actionService.saveAction(this.nodeRef, action);
       actionNode = action.getNodeRef();
       String actionId = action.getId();
       assertNotNull(actionNode);
       
       assertNull(action.getExecutionStartDate());
       assertNull(action.getExecutionEndDate());
       assertNull(action.getExecutionFailureMessage());
       assertEquals(ActionStatus.New, action.getExecutionStatus());
       
       // Save this
       txn.commit();
       txn = transactionService.getUserTransaction();
       txn.begin();
       
       // Run the action - will fail and trigger a rollback
       try {
          this.actionService.executeAction(action, this.nodeRef);
          fail("Action should have failed, and the error been thrown");
       } catch(Exception e) {}

       // Check our copy
       assertNotNull(action.getExecutionStartDate());
       assertNotNull(action.getExecutionEndDate());
       assertBefore(action.getExecutionStartDate(), action.getExecutionEndDate());
       assertBefore(action.getExecutionEndDate(), new Date());
       assertNotNull(action.getExecutionFailureMessage());
       assertEquals(ActionStatus.Failed, action.getExecutionStatus());
       
       // Wait for the post-rollback update to complete
       // (The stored one gets updated asynchronously)
       txn.rollback();
       Thread.sleep(150);
       txn = transactionService.getUserTransaction();
       txn.begin();
       
       // Now re-load and check the stored one
       action = runtimeActionService.createAction(actionNode);
       assertEquals(actionId, action.getId());
       assertNotNull(action.getExecutionStartDate());
       assertNotNull(action.getExecutionEndDate());
       assertBefore(action.getExecutionStartDate(), action.getExecutionEndDate());
       assertBefore(action.getExecutionEndDate(), new Date());
       assertNotNull(action.getExecutionFailureMessage());
       assertEquals(ActionStatus.Failed, action.getExecutionStatus());

       // Tidy up from the action failure
       txn.commit();
       txn = transactionService.getUserTransaction();
       txn.begin();

       
       // ===========================================================
       //    Execute a transient Action that works, asynchronously
       // ===========================================================
       action = createWorkingSleepAction(null);
       assertNull(action.getExecutionStartDate());
       assertNull(action.getExecutionEndDate());
       assertNull(action.getExecutionFailureMessage());
       assertEquals(ActionStatus.New, action.getExecutionStatus());
       
       this.actionService.executeAction(action, this.nodeRef, false, true);
       assertNull(action.getExecutionStartDate());
       assertNull(action.getExecutionEndDate());
       assertNull(action.getExecutionFailureMessage());
       assertEquals(ActionStatus.Pending, action.getExecutionStatus());

       // End the transaction. Should allow the async action
       //  to be executed
       txn.commit();
       Thread.sleep(150);
       
       assertNotNull(action.getExecutionStartDate());
       assertNotNull(action.getExecutionEndDate());
       assertBefore(action.getExecutionStartDate(), action.getExecutionEndDate());
       assertBefore(action.getExecutionEndDate(), new Date());
       assertNull(action.getExecutionFailureMessage());
       assertEquals(ActionStatus.Completed, action.getExecutionStatus());
       
       // Put things back ready for the next check
       txn = transactionService.getUserTransaction();
       txn.begin();
       
       
       // ===========================================================
       //    Execute a transient Action that fails, asynchronously
       // ===========================================================
       action = createFailingMoveAction();
       assertNull(action.getExecutionStartDate());
       assertNull(action.getExecutionEndDate());
       assertNull(action.getExecutionFailureMessage());
       assertEquals(ActionStatus.New, action.getExecutionStatus());
       
       this.actionService.executeAction(action, this.nodeRef, false, true);
       assertNull(action.getExecutionStartDate());
       assertNull(action.getExecutionEndDate());
       assertNull(action.getExecutionFailureMessage());
       assertEquals(ActionStatus.Pending, action.getExecutionStatus());
       
       // End the transaction. Should allow the async action
       //  to be executed
       txn.commit();
       Thread.sleep(150);
       
       assertNotNull(action.getExecutionStartDate());
       assertNotNull(action.getExecutionEndDate());
       assertBefore(action.getExecutionStartDate(), action.getExecutionEndDate());
       assertBefore(action.getExecutionEndDate(), new Date());
       assertNotNull(action.getExecutionFailureMessage());
       assertEquals(ActionStatus.Failed, action.getExecutionStatus());
       
       // Put things back ready for the next check
       txn = transactionService.getUserTransaction();
       txn.begin();

       
       // ===========================================================
       //    Execute a stored Action that works, asynchronously
       // ===========================================================
       action = createWorkingSleepAction(null);
       this.actionService.saveAction(this.nodeRef, action);
       actionNode = action.getNodeRef();
       assertNotNull(actionNode);
       
       assertNull(action.getExecutionStartDate());
       assertNull(action.getExecutionEndDate());
       assertNull(action.getExecutionFailureMessage());
       assertEquals(ActionStatus.New, action.getExecutionStatus());
       
       this.actionService.executeAction(action, this.nodeRef, false, true);
       assertNull(action.getExecutionStartDate());
       assertNull(action.getExecutionEndDate());
       assertNull(action.getExecutionFailureMessage());
       assertEquals(ActionStatus.Pending, action.getExecutionStatus());
       
       // End the transaction. Should allow the async action
       //  to be executed
       txn.commit();
       Thread.sleep(150);
       txn = transactionService.getUserTransaction();
       txn.begin();
       
       // Check our copy
       assertNotNull(action.getExecutionStartDate());
       assertNotNull(action.getExecutionEndDate());
       assertBefore(action.getExecutionStartDate(), action.getExecutionEndDate());
       assertBefore(action.getExecutionEndDate(), new Date());
       assertNull(action.getExecutionFailureMessage());
       assertEquals(ActionStatus.Completed, action.getExecutionStatus());
       
       // Now re-load and check the stored one
       action = runtimeActionService.createAction(actionNode);
       assertNotNull(action.getExecutionStartDate());
       assertNotNull(action.getExecutionEndDate());
       assertBefore(action.getExecutionStartDate(), action.getExecutionEndDate());
       assertBefore(action.getExecutionEndDate(), new Date());
       assertNull(action.getExecutionFailureMessage());
       assertEquals(ActionStatus.Completed, action.getExecutionStatus());

       
       // ===========================================================
       //    Execute a stored Action that fails, asynchronously
       // ===========================================================
       action = createFailingMoveAction();
       this.actionService.saveAction(this.nodeRef, action);
       actionNode = action.getNodeRef();
       assertNotNull(actionNode);
       
       assertNull(action.getExecutionStartDate());
       assertNull(action.getExecutionEndDate());
       assertNull(action.getExecutionFailureMessage());
       assertEquals(ActionStatus.New, action.getExecutionStatus());
       
       this.actionService.executeAction(action, this.nodeRef, false, true);
       assertNull(action.getExecutionStartDate());
       assertNull(action.getExecutionEndDate());
       assertNull(action.getExecutionFailureMessage());
       assertEquals(ActionStatus.Pending, action.getExecutionStatus());
       
       // End the transaction. Should allow the async action
       //  to be executed
       // Need to wait longer, as we have two async actions
       //  that need to occur - action + record
       txn.commit();
       Thread.sleep(250);
       txn = transactionService.getUserTransaction();
       txn.begin();
       
       // Check our copy
       assertNotNull(action.getExecutionStartDate());
       assertNotNull(action.getExecutionEndDate());
       assertBefore(action.getExecutionStartDate(), action.getExecutionEndDate());
       assertBefore(action.getExecutionEndDate(), new Date());
       assertNotNull(action.getExecutionFailureMessage());
       assertEquals(ActionStatus.Failed, action.getExecutionStatus());
       
       // Now re-load and check the stored one
       action = runtimeActionService.createAction(actionNode);
       assertNotNull(action.getExecutionStartDate());
       assertNotNull(action.getExecutionEndDate());
       assertBefore(action.getExecutionStartDate(), action.getExecutionEndDate());
       assertBefore(action.getExecutionEndDate(), new Date());
       assertNotNull(action.getExecutionFailureMessage());
       assertEquals(ActionStatus.Failed, action.getExecutionStatus());
    }

    
    // =================================================================== //

    
    private Action createFailingMoveAction() {
       Action failingAction = this.actionService.createAction(MoveActionExecuter.NAME);
       failingAction.setParameterValue(MoveActionExecuter.PARAM_ASSOC_TYPE_QNAME, ContentModel.ASSOC_CHILDREN);
       failingAction.setParameterValue(MoveActionExecuter.PARAM_ASSOC_QNAME, ContentModel.ASSOC_CHILDREN);
       // Create a bad node ref
       NodeRef badNodeRef = new NodeRef(this.storeRef, "123123");
       failingAction.setParameterValue(MoveActionExecuter.PARAM_DESTINATION_FOLDER, badNodeRef);
       
       return failingAction;
    }
    
    private Action createFailingSleepAction(String id) throws Exception {
       return ActionServiceImplTest.createFailingSleepAction(id, actionService);
    }
    private Action createWorkingSleepAction(String id) throws Exception {
       return ActionServiceImplTest.createWorkingSleepAction(id, actionService);
    }
}