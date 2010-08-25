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

package org.alfresco.repo.replication;

import java.io.Serializable;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import javax.transaction.UserTransaction;

import junit.framework.TestCase;

import org.alfresco.model.ContentModel;
import org.alfresco.repo.lock.JobLockService;
import org.alfresco.repo.model.Repository;
import org.alfresco.repo.security.authentication.AuthenticationUtil;
import org.alfresco.repo.transfer.TransferServiceImpl;
import org.alfresco.repo.transfer.TransferTransmitter;
import org.alfresco.repo.transfer.UnitTestInProcessTransmitterImpl;
import org.alfresco.repo.transfer.UnitTestTransferManifestNodeFactory;
import org.alfresco.repo.transfer.manifest.TransferManifestNodeFactory;
import org.alfresco.service.cmr.action.ActionService;
import org.alfresco.service.cmr.action.ActionStatus;
import org.alfresco.service.cmr.action.ActionTrackingService;
import org.alfresco.service.cmr.lock.LockService;
import org.alfresco.service.cmr.lock.UnableToReleaseLockException;
import org.alfresco.service.cmr.replication.ReplicationDefinition;
import org.alfresco.service.cmr.replication.ReplicationService;
import org.alfresco.service.cmr.replication.ReplicationServiceException;
import org.alfresco.service.cmr.repository.ChildAssociationRef;
import org.alfresco.service.cmr.repository.ContentReader;
import org.alfresco.service.cmr.repository.ContentService;
import org.alfresco.service.cmr.repository.NodeRef;
import org.alfresco.service.cmr.repository.NodeService;
import org.alfresco.service.cmr.repository.Path;
import org.alfresco.service.cmr.transfer.TransferDefinition;
import org.alfresco.service.cmr.transfer.TransferException;
import org.alfresco.service.cmr.transfer.TransferReceiver;
import org.alfresco.service.cmr.transfer.TransferService;
import org.alfresco.service.cmr.transfer.TransferTarget;
import org.alfresco.service.namespace.NamespaceService;
import org.alfresco.service.namespace.QName;
import org.alfresco.service.transaction.TransactionService;
import org.alfresco.util.ApplicationContextHelper;
import org.alfresco.util.GUID;
import org.alfresco.util.Pair;
import org.springframework.context.ConfigurableApplicationContext;

/**
 * Unit tests for the Replication Service.
 * Handles its own transactions, as in a few cases it needs
 *  to run async actions and know how they'll behave
 * @author Nick Burch
 */
public class ReplicationServiceIntegrationTest extends TestCase
{
   private static ConfigurableApplicationContext ctx = 
      (ConfigurableApplicationContext)ApplicationContextHelper.getApplicationContext();
   
    private ReplicationActionExecutor replicationActionExecutor;
    private ReplicationService replicationService;
    private TransactionService transactionService;
    private TransferService transferService;
    private ContentService contentService;
    private JobLockService jobLockService;
    private ActionService actionService;
    private NodeService nodeService;
    private LockService lockService;
    private Repository repositoryHelper;
    private ActionTrackingService actionTrackingService;
    
    private NodeRef replicationRoot;
    
    private NodeRef destinationFolder;
    private NodeRef folder1;
    private NodeRef folder2;
    private NodeRef folder2a;
    private NodeRef folder2b;
    private NodeRef content1_1;
    private NodeRef content1_2;
    private NodeRef thumbnail1_3;  // Thumbnail extends content
    private NodeRef authority1_4;  // Authority doesn't
    private NodeRef content2a_1;
    private NodeRef thumbnail2a_2; // Thumbnail extends content
    private NodeRef zone2a_3;      // Zone doesn't
    
    private final String ACTION_NAME  = "testName";
    private final String ACTION_NAME2 = "testName2";
    private final QName  ACTION_QNAME  = QName.createQName(null, ACTION_NAME);
    private final QName  ACTION_QNAME2 = QName.createQName(null, ACTION_NAME2);
    
    private final String TRANSFER_TARGET = "TestTransferTarget";
    
    @Override
    protected void setUp() throws Exception
    {
        replicationActionExecutor = (ReplicationActionExecutor) ctx.getBean("replicationActionExecutor");
        replicationService = (ReplicationService) ctx.getBean("replicationService");
        transactionService = (TransactionService) ctx.getBean("transactionService");
        transferService = (TransferService) ctx.getBean("transferService");
        contentService = (ContentService) ctx.getBean("contentService");
        jobLockService = (JobLockService) ctx.getBean("jobLockService");
        actionService = (ActionService) ctx.getBean("actionService");
        nodeService = (NodeService) ctx.getBean("nodeService");
        lockService = (LockService) ctx.getBean("lockService");
        repositoryHelper = (Repository) ctx.getBean("repositoryHelper");
        actionTrackingService = (ActionTrackingService) ctx.getBean("actionTrackingService");
        
        // Set the current security context as admin
        AuthenticationUtil.setFullyAuthenticatedUser(AuthenticationUtil.getAdminUserName());
        
        UserTransaction txn = transactionService.getUserTransaction();
        txn.begin();
        
        // Zap any existing replication entries
        replicationRoot = ReplicationDefinitionPersisterImpl.REPLICATION_ACTION_ROOT_NODE_REF;
        for(ChildAssociationRef child : nodeService.getChildAssocs(replicationRoot)) {
           QName type = nodeService.getType( child.getChildRef() );
           if(ReplicationDefinitionPersisterImpl.ACTION_TYPES.contains(type)) {
              nodeService.deleteNode(child.getChildRef());
           }
        }
        
        // Create the test folder structure
        destinationFolder = makeNode(repositoryHelper.getCompanyHome(), ContentModel.TYPE_FOLDER, "ReplicationTransferDestination");
        folder1 = makeNode(repositoryHelper.getCompanyHome(), ContentModel.TYPE_FOLDER);
        folder2 = makeNode(repositoryHelper.getCompanyHome(), ContentModel.TYPE_FOLDER);
        folder2a = makeNode(folder2, ContentModel.TYPE_FOLDER);
        folder2b = makeNode(folder2, ContentModel.TYPE_FOLDER);
        
        content1_1 = makeNode(folder1, ContentModel.TYPE_CONTENT);
        content1_2 = makeNode(folder1, ContentModel.TYPE_CONTENT);
        thumbnail1_3 = makeNode(folder1, ContentModel.TYPE_THUMBNAIL);
        authority1_4 = makeNode(folder1, ContentModel.TYPE_AUTHORITY);
        content2a_1 = makeNode(folder2a, ContentModel.TYPE_CONTENT);
        thumbnail2a_2 = makeNode(folder2a, ContentModel.TYPE_THUMBNAIL);
        zone2a_3 = makeNode(folder2a, ContentModel.TYPE_ZONE);
        
        // Tell the transfer service not to use HTTP
        makeTransferServiceLocal();
        
        // Finish setup
        txn.commit();
    }
    
    @Override
    protected void tearDown() throws Exception {
       UserTransaction txn = transactionService.getUserTransaction();
       txn.begin();
       
      // Zap our test folders
      if(folder1 != null) {
         nodeService.deleteNode(folder1);
      }
      if(folder2 != null) {
         nodeService.deleteNode(folder2);
      }
      if(destinationFolder != null) {
         nodeService.deleteNode(destinationFolder);
      }
      
      txn.commit();
      txn = transactionService.getUserTransaction();
      txn.begin();
      
      // Zap our test transfer target
      try {
         transferService.deleteTransferTarget(TRANSFER_TARGET);
      } catch(TransferException e) {}
      
      txn.commit();
   }



   public void testCreation() throws Exception
    {
       ReplicationDefinition replicationAction =
          replicationService.createReplicationDefinition(ACTION_NAME, "Test Definition");
       assertNotNull(replicationAction);
       assertEquals("Test Definition", replicationAction.getDescription());
       assertEquals(ACTION_NAME, replicationAction.getReplicationName());
       assertEquals(ACTION_QNAME, replicationAction.getReplicationQName());
       
       String id = replicationAction.getId();
       assertNotNull(id);
       assertTrue(id.length() > 0);
       
       assertNotNull(replicationAction.getPayload());
       assertEquals(0, replicationAction.getPayload().size());
       
       assertNull(replicationAction.getLocalTransferReport());
       assertNull(replicationAction.getRemoteTransferReport());
    }
    
    public void testCreateSaveLoad() throws Exception
    {
       ReplicationDefinition replicationAction =
          replicationService.createReplicationDefinition(ACTION_NAME, "Test Definition");
       String initialId = replicationAction.getId();
       replicationAction.getPayload().add(
             new NodeRef("workspace://SpacesStore/Testing")
       );
       replicationAction.getPayload().add(
             new NodeRef("workspace://SpacesStore/Testing2")
       );
       assertEquals(2, replicationAction.getPayload().size());
       
       replicationService.saveReplicationDefinition(replicationAction);
       
       // Load it again, should have the same details still
       ReplicationDefinition retrieved =
          replicationService.loadReplicationDefinition(ACTION_NAME);
       assertNotNull(retrieved);
       assertEquals(initialId, retrieved.getId());
       assertEquals(ACTION_NAME, retrieved.getReplicationName());
       assertEquals(ACTION_QNAME, retrieved.getReplicationQName());
       assertEquals("Test Definition", retrieved.getDescription());
       assertEquals(2, retrieved.getPayload().size());
       
       // Load a 2nd copy, won't be any changes
       ReplicationDefinition second =
          replicationService.loadReplicationDefinition(ACTION_NAME);
       assertNotNull(second);
       assertEquals(initialId, second.getId());
       assertEquals(ACTION_NAME, second.getReplicationName());
       assertEquals(ACTION_QNAME, second.getReplicationQName());
       assertEquals("Test Definition", second.getDescription());
       assertEquals(2, second.getPayload().size());
    }
    
    public void testLoadList() throws Exception
    {
       assertEquals(0, replicationService.loadReplicationDefinitions().size());

       // Create and store
       ReplicationDefinition rd1 = replicationService.createReplicationDefinition(ACTION_NAME, "Test 1");
       ReplicationDefinition rd2 = replicationService.createReplicationDefinition(ACTION_NAME2, "Test 2");
       
       assertEquals(0, replicationService.loadReplicationDefinitions().size());

       replicationService.saveReplicationDefinition(rd1);
       
       assertEquals(1, replicationService.loadReplicationDefinitions().size());
       assertEquals(ACTION_NAME, replicationService.loadReplicationDefinitions().get(0).getReplicationName());
       
       replicationService.saveReplicationDefinition(rd2);
       assertEquals(2, replicationService.loadReplicationDefinitions().size());
    }
    
    public void testLoadByTarget() throws Exception
    {
       assertEquals(0, replicationService.loadReplicationDefinitions().size());
       assertEquals(0, replicationService.loadReplicationDefinitions("TestTarget").size());
       assertEquals(0, replicationService.loadReplicationDefinitions("TestTarget2").size());
       
       // Store some
       ReplicationDefinition rdTT = replicationService.createReplicationDefinition(ACTION_NAME, "Test");
       rdTT.setTargetName("TestTarget");
       replicationService.saveReplicationDefinition(rdTT);
       
       // Check it shows up correctly
       assertEquals(1, replicationService.loadReplicationDefinitions().size());
       assertEquals(1, replicationService.loadReplicationDefinitions("TestTarget").size());
       assertEquals(0, replicationService.loadReplicationDefinitions("TestTarget2").size());
    }
    
    /**
     * Ensures that deletion works correctly
     */
    public void testDeletion() throws Exception
    {
       // Delete does nothing if not persisted
       assertEquals(0, replicationService.loadReplicationDefinitions().size());
       ReplicationDefinition rd1 = replicationService.createReplicationDefinition(ACTION_NAME, "Test");
       assertEquals(0, replicationService.loadReplicationDefinitions().size());
       
       replicationService.deleteReplicationDefinition(rd1);
       assertEquals(0, replicationService.loadReplicationDefinitions().size());
       
       
       // Create and save two
       ReplicationDefinition rd2 = replicationService.createReplicationDefinition(ACTION_NAME2, "Test2");
       replicationService.saveReplicationDefinition(rd1);
       replicationService.saveReplicationDefinition(rd2);
       assertEquals(2, replicationService.loadReplicationDefinitions().size());
       
       
       // Delete one - the correct one goes!
       replicationService.deleteReplicationDefinition(rd2);
       assertEquals(1, replicationService.loadReplicationDefinitions().size());
       assertEquals(ACTION_NAME, replicationService.loadReplicationDefinitions().get(0).getReplicationName());
       assertNotNull(replicationService.loadReplicationDefinition(ACTION_NAME));
       assertNull(replicationService.loadReplicationDefinition(ACTION_NAME2));
       
       
       // Re-delete already deleted, no change
       replicationService.deleteReplicationDefinition(rd2);
       assertEquals(1, replicationService.loadReplicationDefinitions().size());
       assertEquals(ACTION_NAME, replicationService.loadReplicationDefinitions().get(0).getReplicationName());
       assertNotNull(replicationService.loadReplicationDefinition(ACTION_NAME));
       assertNull(replicationService.loadReplicationDefinition(ACTION_NAME2));
       
       
       // Delete the 2nd
       replicationService.deleteReplicationDefinition(rd1);
       assertEquals(0, replicationService.loadReplicationDefinitions().size());
       assertNull(replicationService.loadReplicationDefinition(ACTION_NAME));
       assertNull(replicationService.loadReplicationDefinition(ACTION_NAME2));
       
       
       // Can add back in again after being deleted
       replicationService.saveReplicationDefinition(rd1);
       assertEquals(1, replicationService.loadReplicationDefinitions().size());
       assertEquals(ACTION_NAME, replicationService.loadReplicationDefinitions().get(0).getReplicationName());
    }
    
    /**
     * Ensures that we can create, save, edit, save
     *  load, edit, save, load etc, all without
     *  problems, and without creating duplicates
     */
    public void testEditing() throws Exception
    {
       ReplicationDefinition rdTT = replicationService.createReplicationDefinition(ACTION_NAME, "Test");
       rdTT.setTargetName("TestTarget");
       replicationService.saveReplicationDefinition(rdTT);
       
       // Load, and check it hasn't changed
       rdTT = replicationService.loadReplicationDefinition(ACTION_NAME);
       assertEquals(ACTION_NAME, rdTT.getReplicationName());
       assertEquals("Test", rdTT.getDescription());
       assertEquals("TestTarget", rdTT.getTargetName());
       assertEquals(true, rdTT.isEnabled());
       assertEquals(0, rdTT.getPayload().size());
       
       // Save and re-load without changes
       replicationService.saveReplicationDefinition(rdTT);
       rdTT = replicationService.loadReplicationDefinition(ACTION_NAME);
       assertEquals(ACTION_NAME, rdTT.getReplicationName());
       assertEquals("Test", rdTT.getDescription());
       assertEquals("TestTarget", rdTT.getTargetName());
       assertEquals(true, rdTT.isEnabled());
       assertEquals(0, rdTT.getPayload().size());
       
       // Make some small changes
       rdTT.setDescription("Test Description");
       rdTT.getPayload().add(folder2a);
       rdTT.setEnabled(false);
       
       // Check we see them on save/load
       replicationService.saveReplicationDefinition(rdTT);
       rdTT = replicationService.loadReplicationDefinition(ACTION_NAME);
       assertEquals(ACTION_NAME, rdTT.getReplicationName());
       assertEquals("Test Description", rdTT.getDescription());
       assertEquals("TestTarget", rdTT.getTargetName());
       assertEquals(false, rdTT.isEnabled());
       assertEquals(1, rdTT.getPayload().size());
       assertEquals(folder2a, rdTT.getPayload().get(0));
       
       // And some more changes
       rdTT.setDescription("Another One");
       rdTT.getPayload().clear();
       rdTT.getPayload().add(folder1);
       rdTT.getPayload().add(folder2b);
       assertEquals(2, rdTT.getPayload().size());
       
       // Ensure these also come with save/load
       replicationService.saveReplicationDefinition(rdTT);
       rdTT = replicationService.loadReplicationDefinition(ACTION_NAME);
       assertEquals(ACTION_NAME, rdTT.getReplicationName());
       assertEquals("Another One", rdTT.getDescription());
       assertEquals("TestTarget", rdTT.getTargetName());
       assertEquals(false, rdTT.isEnabled());
       assertEquals(2, rdTT.getPayload().size());
       assertEquals(folder1, rdTT.getPayload().get(0));
       assertEquals(folder2b, rdTT.getPayload().get(1));
       
       // And more payload changes
       rdTT.getPayload().clear();
       rdTT.getPayload().add(content1_1);
       assertEquals(1, rdTT.getPayload().size());
       rdTT.setEnabled(true);
       
       replicationService.saveReplicationDefinition(rdTT);
       rdTT = replicationService.loadReplicationDefinition(ACTION_NAME);
       assertEquals(ACTION_NAME, rdTT.getReplicationName());
       assertEquals("Another One", rdTT.getDescription());
       assertEquals("TestTarget", rdTT.getTargetName());
       assertEquals(true, rdTT.isEnabled());
       assertEquals(1, rdTT.getPayload().size());
       assertEquals(content1_1, rdTT.getPayload().get(0));
    }
    
    /**
     * Tests that we can rename definitions
     */
    public void testRenaming() throws Exception
    {
       // Create one instance
       ReplicationDefinition rdTT = replicationService.createReplicationDefinition(ACTION_NAME, "Test");
       rdTT.setTargetName("TestTarget");
       replicationService.saveReplicationDefinition(rdTT);
       assertEquals(1, replicationService.loadReplicationDefinitions().size());
       
       
       // Rename it
       replicationService.renameReplicationDefinition(ACTION_NAME, ACTION_NAME2);
       
       assertEquals(1, replicationService.loadReplicationDefinitions().size());
       assertEquals(null, replicationService.loadReplicationDefinition(ACTION_NAME));
       
       rdTT = replicationService.loadReplicationDefinition(ACTION_NAME2);
       assertNotNull(rdTT);
       assertEquals(ACTION_NAME2, rdTT.getReplicationName());
       assertEquals(ACTION_QNAME2, rdTT.getReplicationQName());
       
       
       // If the source name doesn't exist, does nothing
       replicationService.renameReplicationDefinition(ACTION_NAME, ACTION_NAME2);
       
       assertEquals(1, replicationService.loadReplicationDefinitions().size());
       assertEquals(null, replicationService.loadReplicationDefinition(ACTION_NAME));
       
       rdTT = replicationService.loadReplicationDefinition(ACTION_NAME2);
       assertNotNull(rdTT);
       assertEquals(ACTION_NAME2, rdTT.getReplicationName());
       assertEquals(ACTION_QNAME2, rdTT.getReplicationQName());

       
       // Renaming to a duplicate name breaks
       rdTT = replicationService.createReplicationDefinition(ACTION_NAME, "Test");
       rdTT.setTargetName("TestTarget");
       replicationService.saveReplicationDefinition(rdTT);
       assertEquals(2, replicationService.loadReplicationDefinitions().size());

       try {
          replicationService.renameReplicationDefinition(ACTION_NAME, ACTION_NAME2);
          fail("Shouldn't be able to rename onto a duplicate name");
       } catch(ReplicationServiceException e) {}
    }

    /**
     * Test that the action service can find the executor
     *  for us, and that it has everything it needs
     */
    public void testBasicExecution() throws Exception
    {
       // We need the test transfer target for this test
       makeTransferTarget();
       
       // Ensure the destination is empty 
       // (don't want to get confused with older runs)
       assertEquals(0, nodeService.getChildAssocs(destinationFolder).size());
       
       
       // First one with no target, which isn't allowed
       ReplicationDefinition rd = replicationService.createReplicationDefinition(ACTION_NAME, "Test");
       try {
          actionService.executeAction(rd, replicationRoot);
          fail("Shouldn't be permitted with no Target defined");
       } catch(ReplicationServiceException e) {}
       
       
       // Now no payload, also not allowed
       rd.setTargetName(TRANSFER_TARGET);
       try {
          actionService.executeAction(rd, replicationRoot);
          fail("Shouldn't be permitted with no payload defined");
       } catch(ReplicationServiceException e) {}
       
       
       // Now disabled, not allowed
       assertEquals(true, rd.isEnabled());
       rd.setEnabled(false);
       assertEquals(false, rd.isEnabled());
       try {
          actionService.executeAction(rd, replicationRoot);
          fail("Shouldn't be permitted when disabled");
       } catch(ReplicationServiceException e) {}
       
       
       // Invalid Transfer Target, not allowed
       rd = replicationService.createReplicationDefinition(ACTION_NAME, "Test");
       rd.setTargetName("I am an invalid target that isn't there");
       rd.getPayload().add( folder1 );
       try {
          actionService.executeAction(rd, replicationRoot);
          fail("Shouldn't be permitted with an invalid transfer target");
       } catch(ReplicationServiceException e) {}
       
       
       // Can't send Folder2a if Folder2 isn't there, as it
       //  won't have anywhere to put it
       rd = replicationService.createReplicationDefinition(ACTION_NAME, "Test");
       rd.setTargetName(TRANSFER_TARGET);
       rd.getPayload().add( folder2a );
       try {
          actionService.executeAction(rd, replicationRoot);
          fail("Shouldn't be able to send Folder2a when Folder2 is missing!");
       } catch(ReplicationServiceException e) {}
       
       
       // Next a proper one with a transient definition,
       //  and a sensible set of folders
       rd = replicationService.createReplicationDefinition(ACTION_NAME, "Test");
       rd.setTargetName(TRANSFER_TARGET);
       rd.getPayload().add( folder1 );
       
       // Will execute without error
       UserTransaction txn = transactionService.getUserTransaction();
       txn.begin();
       actionService.executeAction(rd, replicationRoot);
       txn.commit();
       
       
       // Now with one that's in the repo
       ReplicationDefinition rd2 = replicationService.createReplicationDefinition(ACTION_NAME2, "Test");
       rd2.setTargetName(TRANSFER_TARGET);
       rd2.getPayload().add(
             folder2
       );
       replicationService.saveReplicationDefinition(rd2);
       rd2 = replicationService.loadReplicationDefinition(ACTION_NAME2);
       
       // Again no errors
       txn = transactionService.getUserTransaction();
       txn.begin();
       actionService.executeAction(rd2, replicationRoot);
       txn.commit();
    }
    
    /**
     * Check that the locking works.
     * Take a 10 second lock on the job, then execute.
     * Ensure that we really wait a little over 10 seconds.
     */
    public void testReplicationExectionLocking() throws Exception
    {
       // We need the test transfer target for this test
       makeTransferTarget();

       // Create a task
       ReplicationDefinition rd = replicationService.createReplicationDefinition(ACTION_NAME, "Test");
       rd.setTargetName(TRANSFER_TARGET);
       rd.getPayload().add(folder1);
       rd.getPayload().add(folder2);
       
       // Get the lock, and run
       long start = System.currentTimeMillis();
       String token = jobLockService.getLock(
             rd.getReplicationQName(),
             10 * 1000,
             1,
             1
       );
       
       UserTransaction txn = transactionService.getUserTransaction();
       txn.begin();
       actionService.executeAction(rd, replicationRoot);
       txn.commit();
       long end = System.currentTimeMillis();
       
       assertTrue(
            "Should wait for the lock, but didn't (waited " + 
               ((end-start)/1000.0) + " seconds, not 10)",
            end-start > 10000
       );
    }
    
    /**
     * Check that cancelling works.
     * Does this by taking a lock on the job, cancelling,
     *  releasing and seeing it abort.
     */
    public void testReplicationExectionCancelling() throws Exception
    {
       // We need the test transfer target for this test
       makeTransferTarget();

       // Create a task
       ReplicationDefinition rd = replicationService.createReplicationDefinition(ACTION_NAME, "Test");
       rd.setTargetName(TRANSFER_TARGET);
       rd.getPayload().add(folder1);
       rd.getPayload().add(folder2);
       
       // Get the lock for 2 seconds
       String token = jobLockService.getLock(
             rd.getReplicationQName(),
             2 * 1000,
             1,
             1
       );
       
       // Request it be run async
       UserTransaction txn = transactionService.getUserTransaction();
       txn.begin();
       actionService.executeAction(rd, replicationRoot, false, true);
       assertEquals(ActionStatus.Pending, rd.getExecutionStatus());
       txn.commit();
       
       
       // Let it get going, will be waiting for the lock
       //  having registered with the action tracking service
       Thread.sleep(500);
       assertEquals(ActionStatus.Running, rd.getExecutionStatus());
       
       // Now request the cancel
       actionTrackingService.requestActionCancellation(rd);
       
       // Release our lock, should allow the replication task
       //  to get going and spot the cancel
       jobLockService.releaseLock(token, rd.getReplicationQName());
       
       // Let the main replication task run to cancelled/completed
       // This can take quite some time though...
       for(int i=0; i<10; i++) {
          if(rd.getExecutionStatus() == ActionStatus.Running) {
             Thread.sleep(1000);
          } else {
             // It has finished running, check it
             break;
          }
       }
       
       // Ensure it was cancelled
       assertEquals(null, rd.getExecutionFailureMessage());
       assertEquals(ActionStatus.Cancelled, rd.getExecutionStatus());
    }
    
    /**
     * Test that when we execute a replication task, the
     *  right stuff ends up being moved for us
     */
    public void testExecutionResult() throws Exception
    {
       UserTransaction txn = transactionService.getUserTransaction();
       txn.begin();
       
       // Destination is empty
       assertEquals(0, nodeService.getChildAssocs(destinationFolder).size());
       
       // We need the test transfer target for this test
       makeTransferTarget();

       // Put in Folder 2, so we can send Folder 2a
       String folder2Name = (String)nodeService.getProperties(folder2).get(ContentModel.PROP_NAME);
       NodeRef folderT2 = makeNode(destinationFolder, ContentModel.TYPE_FOLDER, folder2Name);
       txn.commit();
       
       
       // Run a transfer
       ReplicationDefinition rd = replicationService.createReplicationDefinition(ACTION_NAME, "Test");
       rd.setTargetName(TRANSFER_TARGET);
       rd.getPayload().add(folder1);
       rd.getPayload().add(folder2a);
       
       assertEquals(null, rd.getLocalTransferReport());
       assertEquals(null, rd.getRemoteTransferReport());
       
       txn = transactionService.getUserTransaction();
       txn.begin();
       actionService.executeAction(rd, replicationRoot);
       txn.commit();
       
       // Correct things have turned up
       assertEquals(2, nodeService.getChildAssocs(destinationFolder).size());
       NodeRef c1 = nodeService.getChildAssocs(destinationFolder).get(0).getChildRef();
       NodeRef c2 = nodeService.getChildAssocs(destinationFolder).get(1).getChildRef();
       
       // The destination should have folder 1 (transfered) 
       //  and folder 2 (created). folder 2 will have
       //  folder 2a (transfered) but not 2b
       NodeRef folderT1 = null;
       boolean foundT1 = false;
       boolean foundT2 = false;
       if(nodeService.getProperty(folder1, ContentModel.PROP_NAME).equals(
             nodeService.getProperty(c1, ContentModel.PROP_NAME) )) {
          folderT1 = c1;
          foundT1 = true;
       }
       if(nodeService.getProperty(folder1, ContentModel.PROP_NAME).equals(
             nodeService.getProperty(c2, ContentModel.PROP_NAME) )) {
          folderT1 = c2;
          foundT1 = true;
       }
       if(c1.equals(folderT2) || c2.equals(folderT2)) {
          foundT2 = true;
       }

       if(!foundT1) {
          fail("Folder 1 not found in the destination");
       }
       if(!foundT2) {
          fail("Folder 2 not found in the destination");
       }
       
       // Folder 1 has 2*content + thumbnail
       assertEquals(3, nodeService.getChildAssocs(folderT1).size());
       // Won't have the authority, as that gets skipped
       for(ChildAssociationRef r : nodeService.getChildAssocs(folderT1)) {
          if(nodeService.getType(r.getChildRef()).equals(ContentModel.TYPE_AUTHORITY)) {
             fail("Found authority as " + r.getChildRef() + " but it shouldn't be transfered!");
          }
       }
       
       // Folder 2 has 2a but not 2b, since only
       //  2a was transfered
       assertEquals(1, nodeService.getChildAssocs(folderT2).size());
       NodeRef folderT2a = nodeService.getChildAssocs(folderT2).get(0).getChildRef();
       assertEquals(
             nodeService.getProperty(folder2a, ContentModel.PROP_NAME),
             nodeService.getProperty(folderT2a, ContentModel.PROP_NAME)
       );
       // Won't have Folder 2b, as it wasn't on the payload
       for(ChildAssociationRef r : nodeService.getChildAssocs(folderT2)) {
          assertNotSame(
                nodeService.getProperty(folder2b, ContentModel.PROP_NAME),
                nodeService.getProperty(r.getChildRef(), ContentModel.PROP_NAME)
          );
       }
       
       // Folder 2a has content + thumbnail
       assertEquals(2, nodeService.getChildAssocs(folderT2a).size());
       // Won't have the zone, as that gets skipped
       for(ChildAssociationRef r : nodeService.getChildAssocs(folderT2a)) {
          if(nodeService.getType(r.getChildRef()).equals(ContentModel.TYPE_ZONE)) {
             fail("Found zone as " + r.getChildRef() + " but it shouldn't be transfered!");
          }
       }
       
       // Check we got transfer reports, and they look sensible
       NodeRef localReport = rd.getLocalTransferReport();
       assertNotNull(localReport);
       NodeRef remoteReport = rd.getRemoteTransferReport();
       assertNotNull(remoteReport);
       
       txn = transactionService.getUserTransaction();
       txn.begin();
       
       ContentReader localReader = 
          contentService.getReader(localReport, ContentModel.PROP_CONTENT);
       String localReportContent = localReader.getContentString();
       
       assertTrue("XML not found in:\n" + localReportContent, localReportContent.contains("<?xml"));
       assertTrue("Report XML not found in:\n" + localReportContent, localReportContent.contains("<report:transferReport"));

       ContentReader remoteReader = 
           contentService.getReader(remoteReport, ContentModel.PROP_CONTENT);
       String remoteReportContent = remoteReader.getContentString();
       
       // TODO: update these tests when transfer report is converted to XML
       assertFalse("XML found in:\n" + remoteReportContent, remoteReportContent.contains("<?xml"));
       assertTrue("Report Status not found in:\n" + remoteReportContent, remoteReportContent.contains("Status update: COMPLETE"));

       txn.commit();
    }
    
    /**
     * Test that we turn a list of payload node starting points
     *  into the correct set of nodes to pass to the 
     *  transfer service.
     */
    public void testReplicationPayloadExpansion() throws Exception
    {
       ReplicationDefinition rd = replicationService.createReplicationDefinition(ACTION_NAME, "Test");
       Set<NodeRef> expanded;
       
       // Empty folder -> just itself
       rd.getPayload().clear();
       rd.getPayload().add(folder2b);
       expanded = replicationActionExecutor.expandPayload(rd);
       assertEquals(1, expanded.size());
       assertTrue(expanded.contains(folder2b));
       
       // Folder with content and thumbnails - just content + thumbnail + folder
       rd.getPayload().clear();
       rd.getPayload().add(folder1);
       expanded = replicationActionExecutor.expandPayload(rd);
       assertEquals(4, expanded.size());
       assertTrue(expanded.contains(folder1));
       assertTrue(expanded.contains(content1_1));
       assertTrue(expanded.contains(content1_2));
       assertTrue(expanded.contains(thumbnail1_3));
       assertFalse(expanded.contains(authority1_4)); // Wrong type, won't be there
       
       // Folder with folders - descends properly
       rd.getPayload().clear();
       rd.getPayload().add(folder2);
       expanded = replicationActionExecutor.expandPayload(rd);
       assertEquals(5, expanded.size());
       assertTrue(expanded.contains(folder2));
       assertTrue(expanded.contains(folder2a));
       assertTrue(expanded.contains(content2a_1));
       assertTrue(expanded.contains(thumbnail2a_2));
       assertFalse(expanded.contains(zone2a_3)); // Wrong type, won't be there
       assertTrue(expanded.contains(folder2b));
       
       // Multiple things - gets each in turn
       rd.getPayload().clear();
       rd.getPayload().add(folder1);
       rd.getPayload().add(folder2);
       expanded = replicationActionExecutor.expandPayload(rd);
       assertEquals(9, expanded.size());
       assertTrue(expanded.contains(folder1));
       assertTrue(expanded.contains(content1_1));
       assertTrue(expanded.contains(content1_2));
       assertTrue(expanded.contains(thumbnail1_3));
       assertTrue(expanded.contains(folder2));
       assertTrue(expanded.contains(folder2a));
       assertTrue(expanded.contains(content2a_1));
       assertTrue(expanded.contains(thumbnail2a_2));
       assertTrue(expanded.contains(folder2b));
       
       // TODO Test how options like permissions and renditions
       //  affects what gets sent back
    }

    /**
     * Test that we turn a replication definition correctly
     *  into a transfer definition
     */
    public void testTransferDefinitionBuilding() throws Exception
    {
       ReplicationDefinition rd = replicationService.createReplicationDefinition(ACTION_NAME, "Test");
       
       Set<NodeRef> nodes = new HashSet<NodeRef>();
       nodes.add(folder1);
       nodes.add(content1_1);
       
       TransferDefinition td = replicationActionExecutor.buildTransferDefinition(rd, nodes);
       assertEquals(true, td.isSync());
//       assertEquals(true, td.isReadOnly());// TODO Make read only, and fix tests
       assertEquals(2, td.getNodes().size());
       assertEquals(true, td.getNodes().contains(folder1));
       assertEquals(true, td.getNodes().contains(content1_1));
    }
    

    private NodeRef makeNode(NodeRef parent, QName nodeType)
    {
        String uuid = GUID.generate();
        return makeNode(parent, nodeType, uuid);
    }
    private NodeRef makeNode(NodeRef parent, QName nodeType, String name)
    {
        Map<QName, Serializable> props = new HashMap<QName, Serializable>();
        
        QName newName = QName.createQName(NamespaceService.APP_MODEL_1_0_URI, name);
        NodeRef existing = nodeService.getChildByName(parent, ContentModel.ASSOC_CONTAINS, name);
        if(existing != null) {
           System.err.println("Zapped existing node " + existing + " for name " + name);
           
           try {
              lockService.unlock(existing, true);
           } catch(UnableToReleaseLockException e) {}
           nodeService.deleteNode(existing);
        }
        
        props.put(ContentModel.PROP_NAME, name);
        ChildAssociationRef assoc = nodeService.createNode(parent, ContentModel.ASSOC_CONTAINS, newName, nodeType, props);
        return assoc.getChildRef();
    }
    
    private void makeTransferTarget() {
       String name = TRANSFER_TARGET;
       String title = "title";
       String description = "description";
       String endpointProtocol = "http";
       String endpointHost = "localhost";
       int endpointPort = 8080;
       String endpointPath = "rhubarb";
       String username = "admin";
       char[] password = "password".toCharArray();
     
       TransferTarget ret = transferService.createAndSaveTransferTarget(name, title, description, endpointProtocol, endpointHost, endpointPort, endpointPath, username, password);
       assertNotNull("Transfer Target not correctly built", ret);
    }
    
    private void makeTransferServiceLocal() {
       TransferReceiver receiver = (TransferReceiver)ctx.getBean("transferReceiver");
       TransferManifestNodeFactory transferManifestNodeFactory = (TransferManifestNodeFactory)ctx.getBean("transferManifestNodeFactory");
       TransferServiceImpl transferServiceImpl = (TransferServiceImpl) ctx.getBean("transferService");
       ContentService contentService = (ContentService) ctx.getBean("contentService");
       
       TransferTransmitter transmitter = 
          new UnitTestInProcessTransmitterImpl(receiver, contentService, transactionService);
       transferServiceImpl.setTransmitter(transmitter);
       
       UnitTestTransferManifestNodeFactory testNodeFactory = 
          new UnitTestTransferManifestNodeFactory(transferManifestNodeFactory); 
       transferServiceImpl.setTransferManifestNodeFactory(testNodeFactory);
       
       // Map company_home to the special destination folder
       List<Pair<Path, Path>> pathMap = testNodeFactory.getPathMap();
       pathMap.add(new Pair<Path,Path>(
             nodeService.getPath(repositoryHelper.getCompanyHome()),
             nodeService.getPath(destinationFolder)
       ));
    }
}
