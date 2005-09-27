/*
 * Copyright (C) 2005 Alfresco, Inc.
 *
 * Licensed under the Mozilla Public License version 1.1 
 * with a permitted attribution clause. You may obtain a
 * copy of the License at
 *
 *   http://www.alfresco.org/legal/license.txt
 *
 * Unless required by applicable law or agreed to in writing,
 * software distributed under the License is distributed on an
 * "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND,
 * either express or implied. See the License for the specific
 * language governing permissions and limitations under the
 * License.
 */
package org.alfresco.repo.content;

import java.io.IOException;
import java.io.OutputStream;

import javax.transaction.RollbackException;
import javax.transaction.UserTransaction;

import org.alfresco.model.ContentModel;
import org.alfresco.repo.policy.JavaBehaviour;
import org.alfresco.repo.policy.PolicyComponent;
import org.alfresco.service.ServiceRegistry;
import org.alfresco.service.cmr.repository.ChildAssociationRef;
import org.alfresco.service.cmr.repository.ContentData;
import org.alfresco.service.cmr.repository.ContentIOException;
import org.alfresco.service.cmr.repository.ContentReader;
import org.alfresco.service.cmr.repository.ContentService;
import org.alfresco.service.cmr.repository.ContentWriter;
import org.alfresco.service.cmr.repository.NoTransformerException;
import org.alfresco.service.cmr.repository.NodeRef;
import org.alfresco.service.cmr.repository.NodeService;
import org.alfresco.service.cmr.repository.StoreRef;
import org.alfresco.service.namespace.NamespaceService;
import org.alfresco.service.namespace.QName;
import org.alfresco.service.transaction.TransactionService;
import org.alfresco.util.BaseSpringTest;
import org.alfresco.util.GUID;
import org.alfresco.util.PropertyMap;

/**
 * @see org.alfresco.repo.content.RoutingContentService
 * 
 * @author Derek Hulley
 */
public class RoutingContentServiceTest extends BaseSpringTest
{
    private static final String SOME_CONTENT = "ABC";
        
    private static final String TEST_NAMESPACE = "http://www.alfresco.org/test/RoutingContentServiceTest";
    
    private ContentService contentService;
    private PolicyComponent policyComponent;
    private NodeService nodeService;
    private NodeRef rootNodeRef;
    private NodeRef contentNodeRef;
    
    public RoutingContentServiceTest()
    {
    }
    
    @Override
    public void onSetUpInTransaction() throws Exception
    {
        super.onSetUpInTransaction();
        nodeService = (NodeService) applicationContext.getBean("dbNodeService");
        contentService = (ContentService) applicationContext.getBean(ServiceRegistry.CONTENT_SERVICE.getLocalName());
        this.policyComponent = (PolicyComponent)this.applicationContext.getBean("policyComponent");
        // create a store and get the root node
        StoreRef storeRef = new StoreRef(StoreRef.PROTOCOL_WORKSPACE, getName());
        if (!nodeService.exists(storeRef))
        {
            storeRef = nodeService.createStore(storeRef.getProtocol(), storeRef.getIdentifier());
        }
        rootNodeRef = nodeService.getRootNode(storeRef);
        // create a content node
        ContentData contentData = new ContentData(null, "text/plain", 0L, "UTF-16");
        
        PropertyMap properties = new PropertyMap();
        properties.put(ContentModel.PROP_CONTENT, contentData);
        
        ChildAssociationRef assocRef = nodeService.createNode(
                rootNodeRef,
                ContentModel.ASSOC_CHILDREN,
                QName.createQName(TEST_NAMESPACE, GUID.generate()),
                ContentModel.TYPE_CONTENT,
                properties);
        contentNodeRef = assocRef.getChildRef();
    }
    
    private UserTransaction getUserTransaction()
    {
        TransactionService transactionService = (TransactionService)applicationContext.getBean("transactionComponent");
        return (UserTransaction) transactionService.getUserTransaction();
    }
    
    public void testSetUp() throws Exception
    {
        assertNotNull(contentService);
        assertNotNull(nodeService);
        assertNotNull(rootNodeRef);
        assertNotNull(contentNodeRef);
        assertNotNull(getUserTransaction());
        assertFalse(getUserTransaction() == getUserTransaction());  // ensure txn instances aren't shared 
    }
    
    /**
     * Checks that the URL, mimetype and encoding are automatically set on the readers
     * and writers
     */
    public void testAutoSettingOfProperties() throws Exception
    {
        // get a writer onto the node
        ContentWriter writer = contentService.getWriter(contentNodeRef, ContentModel.PROP_CONTENT, true);
        assertNotNull("Writer should not be null", writer);
        assertNotNull("Content URL should not be null", writer.getContentUrl());
        assertNotNull("Content mimetype should not be null", writer.getMimetype());
        assertNotNull("Content encoding should not be null", writer.getEncoding());
        
        // write some content
        writer.putContent(SOME_CONTENT);
        
        // get the reader
        ContentReader reader = contentService.getReader(contentNodeRef, ContentModel.PROP_CONTENT);
        assertNotNull("Reader should not be null", reader);
        assertNotNull("Content URL should not be null", reader.getContentUrl());
        assertNotNull("Content mimetype should not be null", reader.getMimetype());
        assertNotNull("Content encoding should not be null", reader.getEncoding());
        
        // check that the content length is correct
        // - note encoding is important as we get the byte length
        long length = SOME_CONTENT.getBytes(reader.getEncoding()).length;  // ensures correct decoding
        long checkLength = reader.getSize();
        assertEquals("Content length incorrect", length, checkLength);

        // check the content - the encoding will come into effect here
        String contentCheck = reader.getContentString();
        assertEquals("Content incorrect", SOME_CONTENT, contentCheck);
    }
	
	/**
	 * Tests simple writes that don't automatically update the node content URL
	 */
	public void testSimpleWrite() throws Exception
	{
		// get a writer to an arbitrary node
		ContentWriter writer = contentService.getWriter(contentNodeRef, ContentModel.PROP_CONTENT, false);   // no updating of URL
		assertNotNull("Writer should not be null", writer);
		
		// put some content
		writer.putContent(SOME_CONTENT);
		
		// get the reader for the node
		ContentReader reader = contentService.getReader(contentNodeRef, ContentModel.PROP_CONTENT);
		assertNull("No reader should yet be available for the node", reader);
	}
	
	private boolean policyFired = false;
	
	/**
	 * Tests that the content update policy firs correctly
	 */
	public void testOnContentUpdatePolicy()
	{
		// Register interest in the content update event for a versionable node
		this.policyComponent.bindClassBehaviour(
                QName.createQName(NamespaceService.ALFRESCO_URI, "onContentUpdate"),
				ContentModel.ASPECT_VERSIONABLE,
				new JavaBehaviour(this, "onContentUpdateBehaviourTest"));
		
		// First check that the policy is not fired when the versionable aspect is not present
		ContentWriter contentWriter = this.contentService.getWriter(contentNodeRef, ContentModel.PROP_CONTENT, true);
		contentWriter.putContent("content update one");
		assertFalse(this.policyFired);
		
		// Now check that the policy is fired when the versionable aspect is present
		this.nodeService.addAspect(this.contentNodeRef, ContentModel.ASPECT_VERSIONABLE, null);
		ContentWriter contentWriter2 = this.contentService.getWriter(contentNodeRef, ContentModel.PROP_CONTENT, true);
		contentWriter2.putContent("content update two");
		assertTrue(this.policyFired);
		this.policyFired = false;
		
		// Check that the policy is not fired when using a non updating content writer
		ContentWriter contentWriter3 = this.contentService.getWriter(contentNodeRef, ContentModel.PROP_CONTENT, false);
		contentWriter3.putContent("content update three");
		assertFalse(this.policyFired);
	}
	
	public void onContentUpdateBehaviourTest(NodeRef nodeRef)
	{
		assertEquals(this.contentNodeRef, nodeRef);
		assertTrue(this.nodeService.hasAspect(nodeRef, ContentModel.ASPECT_VERSIONABLE));
		this.policyFired = true;
	}
    
    public void testTempWrite() throws Exception
    {
        // get a temporary writer
        ContentWriter writer1 = contentService.getTempWriter();
        // and another
        ContentWriter writer2 = contentService.getTempWriter();
        
        // check
        assertNotSame("Temp URLs must be different",
                writer1.getContentUrl(), writer2.getContentUrl());
    }
    
	/**
	 * Tests the automatic updating of nodes' content URLs
	 */
    public void testUpdatingWrite() throws Exception
    {
        // check that the content URL property has not been set
        ContentData contentData = (ContentData) nodeService.getProperty(
                contentNodeRef,
                ContentModel.PROP_CONTENT); 
        assertNull("Content URL should be null", contentData.getContentUrl());
        
        // before the content is written, there should not be any reader available
        ContentReader reader = contentService.getReader(contentNodeRef, ContentModel.PROP_CONTENT);
        assertNull("No reader should be available for new node", reader);
        
        // get the writer
        ContentWriter writer = contentService.getWriter(contentNodeRef, ContentModel.PROP_CONTENT, true);
        assertNotNull("No writer received", writer);
        // write some content directly
        writer.putContent(SOME_CONTENT);
        
        // make sure that we can't reuse the writer
        try
        {
            writer.putContent("DEF");
            fail("Failed to prevent repeated use of the content writer");
        }
        catch (ContentIOException e)
        {
            // expected
        }
        
        // check that there is a reader available
        reader = contentService.getReader(contentNodeRef, ContentModel.PROP_CONTENT);
        assertNotNull("No reader available for node", reader);
        String contentCheck = reader.getContentString();
        assertEquals("Content fetched doesn't match that written", SOME_CONTENT, contentCheck);

        // check that the content data was set
        contentData = (ContentData) nodeService.getProperty(
                contentNodeRef,
                ContentModel.PROP_CONTENT);
        assertNotNull("Content data not set", contentData);
        assertEquals("Mismatched URL between writer and node",
                writer.getContentUrl(), contentData.getContentUrl());
        
        // check that the content size was set
        assertEquals("Reader content length and node content length different",
                reader.getSize(), contentData.getSize());
        
        // check that the mimetype was set
        assertEquals("Mimetype not set on content data", "text/plain", contentData.getMimetype());
        // check encoding
        assertEquals("Encoding not set", "UTF-16", contentData.getEncoding());
    }
    
    /**
     * Checks that multiple writes can occur to the same node outside of any transactions.
     * <p>
     * It is only when the streams are closed that the node is updated.
     */
    public void testConcurrentWritesNoTxn() throws Exception
    {
        // ensure that the transaction is ended - ofcourse, we need to force a commit
        setComplete();
        endTransaction();
        
        ContentWriter writer1 = contentService.getWriter(contentNodeRef, ContentModel.PROP_CONTENT, true);
        ContentWriter writer2 = contentService.getWriter(contentNodeRef, ContentModel.PROP_CONTENT, true);
        ContentWriter writer3 = contentService.getWriter(contentNodeRef, ContentModel.PROP_CONTENT, true);
        
        writer1.putContent("writer1 wrote this");
        writer2.putContent("writer2 wrote this");
        writer3.putContent("writer3 wrote this");

        // get the content
        ContentReader reader = contentService.getReader(contentNodeRef, ContentModel.PROP_CONTENT);
        String contentCheck = reader.getContentString();
        assertEquals("Content check failed", "writer3 wrote this", contentCheck);
    }
    
    public void testConcurrentWritesWithSingleTxn() throws Exception
    {
        // want to operate in a user transaction
        setComplete();
        endTransaction();
        
        UserTransaction txn = getUserTransaction();
        txn.begin();
        txn.setRollbackOnly();

        ContentWriter writer1 = contentService.getWriter(contentNodeRef, ContentModel.PROP_CONTENT, true);
        ContentWriter writer2 = contentService.getWriter(contentNodeRef, ContentModel.PROP_CONTENT, true);
        ContentWriter writer3 = contentService.getWriter(contentNodeRef, ContentModel.PROP_CONTENT, true);
        
        writer1.putContent("writer1 wrote this");
        writer2.putContent("writer2 wrote this");
        writer3.putContent("writer3 wrote this");

        // get the content
        ContentReader reader = contentService.getReader(contentNodeRef, ContentModel.PROP_CONTENT);
        String contentCheck = reader.getContentString();
        assertEquals("Content check failed", "writer3 wrote this", contentCheck);
        
        try
        {
            txn.commit();
            fail("Transaction has been marked for rollback");
        }
        catch (RollbackException e)
        {
            // expected
        }
        
        // rollback and check that the content has 'disappeared'
        txn.rollback();
        reader = contentService.getReader(contentNodeRef, ContentModel.PROP_CONTENT);
        assertNull("Transaction was rolled back - no content should be visible", reader);
    }
    
    public synchronized void testConcurrentWritesWithMultipleTxns() throws Exception
    {
        // commit node so that threads can see node
        setComplete();
        endTransaction();
        
        UserTransaction txn = getUserTransaction();
        txn.begin();
        
        // ensure that there is no content to read on the node
        ContentReader reader = contentService.getReader(contentNodeRef, ContentModel.PROP_CONTENT);
        assertNull("Reader should not be available", reader);
        
        ContentWriter threadWriter = contentService.getWriter(contentNodeRef, ContentModel.PROP_CONTENT, true);
        String threadContent = "Thread content";
        WriteThread thread = new WriteThread(threadWriter, threadContent);
        // kick off thread
        thread.start();
        // wait for thread to get to its wait points
        while (!thread.isWaiting())
        {
            wait(10);
        }
        
        // write to the content
        ContentWriter writer = contentService.getWriter(contentNodeRef, ContentModel.PROP_CONTENT, true);
        writer.putContent(SOME_CONTENT);
        
        // fire thread up again
        synchronized(threadWriter)
        {
            threadWriter.notifyAll();
        }
        // thread is released - but we have to wait for it to complete
        while (!thread.isDone())
        {
            wait(10);
        }

        // the thread has finished and has committed its changes - check the visibility
        reader = contentService.getReader(contentNodeRef, ContentModel.PROP_CONTENT);
        assertNotNull("Reader should now be available", reader);
        String checkContent = reader.getContentString();
        assertEquals("Content check failed", SOME_CONTENT, checkContent);
        
        // rollback the txn
        txn.rollback();
        
        // check content has taken on thread's content
        reader = contentService.getReader(contentNodeRef, ContentModel.PROP_CONTENT);
        assertNotNull("Reader should now be available", reader);
        checkContent = reader.getContentString();
        assertEquals("Content check failed", threadContent, checkContent);
    }
    
    public void testTransformation() throws Exception
    {
        // get a regular writer
        ContentWriter writer = contentService.getTempWriter();
        writer.setMimetype("text/xml");
        // write some stuff
        String content = "<blah></blah>";
        writer.putContent(content);
        // get a reader onto the content
        ContentReader reader = writer.getReader();
        
        // get a new writer for the transformation
        writer = contentService.getTempWriter();
        writer.setMimetype("audio/x-wav");     // no such conversion possible
        try
        {
            contentService.transform(reader, writer);
            fail("Transformation attempted with invalid mimetype");
        }
        catch (NoTransformerException e)
        {
            // expected
        }
        writer.setMimetype("text/plain");
        contentService.transform(reader, writer);
        // get the content from the writer
        reader = writer.getReader();
        assertEquals("Mimetype of target reader incorrect",
                writer.getMimetype(), reader.getMimetype());
        String contentCheck = reader.getContentString();
        assertEquals("Content check failed", content, contentCheck);
    }
    
    /**
     * Writes some content to the writer's output stream and then aquires
     * a lock on the writer, waits until notified and then closes the
     * output stream before terminating.
     * <p>
     * When firing thread up, be sure to call <code>notify</code> on the
     * writer in order to let the thread run to completion.
     */
    private class WriteThread extends Thread
    {
        private ContentWriter writer;
        private String content;
        private boolean isWaiting;
        private boolean isDone;
        
        public WriteThread(ContentWriter writer, String content)
        {
            this.writer = writer;
            this.content = content;
            isWaiting = false;
            isDone = false;
        }
        
        public boolean isWaiting()
        {
            return isWaiting;
        }
        
        public boolean isDone()
        {
            return isDone;
        }

        public void run()
        {
            isWaiting = false;
            isDone = false;
            UserTransaction txn = getUserTransaction();
            OutputStream os = writer.getContentOutputStream();
            try
            {
                txn.begin();    // not testing transactions - this is not a safe pattern
                // put the content
                if (writer.getEncoding() == null)
                {
                    os.write(content.getBytes());
                }
                else
                {
                    os.write(content.getBytes(writer.getEncoding()));
                }
                synchronized (writer)
                {
                    isWaiting = true;
                    writer.wait();   // wait until notified
                }
                os.close();
                os = null;
                txn.commit();
            }
            catch (Throwable e)
            {
                try {txn.rollback(); } catch (Exception ee) {}
                throw new RuntimeException("Failed writing to output stream for writer: " + writer, e);
            }
            finally
            {
                if (os != null)
                {
                    try { os.close(); } catch (IOException e) {}
                }
            }
            isDone = true;
        }
    }
}
