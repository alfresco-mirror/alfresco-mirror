/*
 * Copyright (C) 2005-2011 Alfresco Software Limited.
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
package org.alfresco.filesys.repo;

import java.io.IOException;
import java.io.InputStream;
import java.io.Serializable;
import java.net.InetAddress;
import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

import javax.transaction.UserTransaction;

import junit.framework.TestCase;

import org.alfresco.jlan.server.NetworkServer;
import org.alfresco.jlan.server.SrvSession;
import org.alfresco.jlan.server.config.ServerConfiguration;
import org.alfresco.jlan.server.core.DeviceContext;
import org.alfresco.jlan.server.core.DeviceContextException;
import org.alfresco.jlan.server.core.SharedDevice;
import org.alfresco.jlan.server.filesys.AccessMode;
import org.alfresco.jlan.server.filesys.DiskSharedDevice;
import org.alfresco.jlan.server.filesys.FileAction;
import org.alfresco.jlan.server.filesys.FileAttribute;
import org.alfresco.jlan.server.filesys.FileExistsException;
import org.alfresco.jlan.server.filesys.FileInfo;
import org.alfresco.jlan.server.filesys.FileOpenParams;
import org.alfresco.jlan.server.filesys.NetworkFile;
import org.alfresco.jlan.server.filesys.NetworkFileServer;
import org.alfresco.jlan.server.filesys.SearchContext;
import org.alfresco.jlan.server.filesys.TreeConnection;
import org.alfresco.model.ContentModel;
import org.alfresco.model.ForumModel;
import org.alfresco.repo.action.evaluator.NoConditionEvaluator;
import org.alfresco.repo.management.subsystems.ApplicationContextFactory;
import org.alfresco.repo.model.Repository;
import org.alfresco.repo.security.authentication.AuthenticationUtil;
import org.alfresco.repo.transaction.RetryingTransactionHelper;
import org.alfresco.repo.transaction.RetryingTransactionHelper.RetryingTransactionCallback;
import org.alfresco.repo.transfer.TransferModel;
import org.alfresco.service.cmr.action.Action;
import org.alfresco.service.cmr.action.ActionCondition;
import org.alfresco.service.cmr.action.ActionService;
import org.alfresco.service.cmr.action.CompositeAction;
import org.alfresco.service.cmr.repository.ChildAssociationRef;
import org.alfresco.service.cmr.repository.ContentData;
import org.alfresco.service.cmr.repository.ContentService;
import org.alfresco.service.cmr.repository.ContentWriter;
import org.alfresco.service.cmr.repository.MLText;
import org.alfresco.service.cmr.repository.NodeRef;
import org.alfresco.service.cmr.repository.NodeService;
import org.alfresco.service.cmr.rule.Rule;
import org.alfresco.service.cmr.rule.RuleService;
import org.alfresco.service.cmr.rule.RuleType;
import org.alfresco.service.namespace.NamespaceService;
import org.alfresco.service.namespace.QName;
import org.alfresco.service.transaction.TransactionService;
import org.alfresco.util.ApplicationContextHelper;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.springframework.context.ApplicationContext;
import org.springframework.core.io.ClassPathResource;
import org.springframework.extensions.config.element.GenericConfigElement;

/**
 * Unit tests for Alfresco Repository ContentDiskDriver
 */
public class ContentDiskDriverTest extends TestCase
{
    private Repository repositoryHelper;
    private CifsHelper cifsHelper;
    private ContentDiskDriver driver;
    private NodeService mlAwareNodeService;
    private NodeService nodeService;
    private TransactionService transactionService;
    private ContentService contentService;
    private RuleService ruleService;
    private ActionService actionService;
    
    private static Log logger = LogFactory.getLog(ContentDiskDriverTest.class);

    final String SHARE_NAME = "test";
    final String STORE_NAME = "workspace://SpacesStore";
    final String ROOT_PATH = "/app:company_home";
    
    private ApplicationContext applicationContext;
    
    private final String TEST_ROOT_PATH="ContentDiskDriverTest";
    private final String TEST_ROOT_DOS_PATH="\\"+TEST_ROOT_PATH;
    
    @Override
    protected void setUp() throws Exception
    {
        applicationContext = ApplicationContextHelper.getApplicationContext();
        repositoryHelper = (Repository)this.applicationContext.getBean("repositoryHelper");
        ApplicationContextFactory fileServers = (ApplicationContextFactory) this.applicationContext.getBean("fileServers");
        cifsHelper = (CifsHelper) fileServers.getApplicationContext().getBean("cifsHelper");
        driver = (ContentDiskDriver) fileServers.getApplicationContext().getBean("contentDiskDriver");
        mlAwareNodeService = (NodeService) this.applicationContext.getBean("mlAwareNodeService"); 
        nodeService = (NodeService)applicationContext.getBean("nodeService");
        transactionService = (TransactionService)applicationContext.getBean("transactionService");
        contentService = (ContentService)applicationContext.getBean("contentService");
        ruleService = (RuleService)applicationContext.getBean("ruleService");
        actionService = (ActionService)this.applicationContext.getBean("actionService");
        
        assertNotNull("content disk driver is null", driver);
        assertNotNull("repositoryHelper is null", repositoryHelper);
        assertNotNull("mlAwareNodeService is null", mlAwareNodeService);
        assertNotNull("nodeService is null", nodeService);
        assertNotNull("transactionService is null", transactionService);
        assertNotNull("contentService is null", contentService);
        assertNotNull("ruleService is null", ruleService);
        assertNotNull("actionService is null", actionService);
        
        AuthenticationUtil.setRunAsUserSystem();
        
        // remove our test root 
        RetryingTransactionCallback<Void> removeRootCB = new RetryingTransactionCallback<Void>() {

            @Override
            public Void execute() throws Throwable
            {
                NodeRef companyHome = repositoryHelper.getCompanyHome();
                NodeRef rootNode = nodeService.getChildByName(companyHome, ContentModel.ASSOC_CONTAINS, TEST_ROOT_PATH);
                if(rootNode != null)
                {
                    logger.debug("Clean up test root node");
                    nodeService.deleteNode(rootNode);
                }
                return null;
            }
        };
        final RetryingTransactionHelper tran = transactionService.getRetryingTransactionHelper();
        
        tran.doInTransaction(removeRootCB, false, true);
        
    }
    
    @Override
    protected void tearDown() throws Exception
    {
//        UserTransaction txn = transactionService.getUserTransaction();
//        assertNotNull("transaction leaked", txn);
//        txn.getStatus();
//        txn.rollback();
    }
     
    /**
     * Test create context.
     * 
     * Must have "store" and "rootPath"
     */
    public void testCreateContext() throws Exception
    {
        logger.debug("testCreateContext");
        
        GenericConfigElement cfg1 =  new GenericConfigElement("filesystem");
        
        GenericConfigElement store =  new GenericConfigElement("store");
        store.setValue(STORE_NAME);
        cfg1.addChild(store);
        
        GenericConfigElement rootPath =  new GenericConfigElement("rootPath");
        rootPath.setValue(ROOT_PATH);
        cfg1.addChild(rootPath);
        
        /**
         * Step 1: Call create context and expect it to succeed
         */
        DeviceContext context = driver.createContext(SHARE_NAME, cfg1);
       
        assertTrue (context instanceof ContentContext);
        assertNotNull (context);
        
        ContentContext ctx = (ContentContext)context;
        
        assertEquals("Device name wrong", SHARE_NAME, ctx.getDeviceName());
        assertEquals("Root Path wrong", ROOT_PATH, ctx.getRootPath());
        
        context.CloseContext();
        
        /**
         *  Step 2: Negative test - missing store property
         */
        try
        {
            GenericConfigElement cfg2 =  new GenericConfigElement("filesystem");
            cfg2.addChild(rootPath);
            driver.createContext(SHARE_NAME, cfg2);
            fail("missing store not detected");
        }
        catch (DeviceContextException de)
        {
            // expect to go here
        }
        
        /**
         *  Step 3: Negative test - missing rootPath property
         */
        try
        {
            GenericConfigElement cfg2 =  new GenericConfigElement("filesystem");
            cfg2.addChild(store);
            driver.createContext(SHARE_NAME, cfg2);
            fail("missing store not detected");
        }
        catch (DeviceContextException de)
        {
            // expect to go here
        }
    }

    private DiskSharedDevice getDiskSharedDevice() throws DeviceContextException
    {
        ServerConfiguration scfg = new ServerConfiguration("testServer");
        TestServer testServer = new TestServer("testServer", scfg);
        SrvSession testSession = new TestSrvSession(666, testServer, "test", "remoteName");
    
        GenericConfigElement cfg1 =  new GenericConfigElement("filesystem");
           
        GenericConfigElement store =  new GenericConfigElement("store");
        store.setValue(STORE_NAME);
        cfg1.addChild(store);
    
        GenericConfigElement rootPath =  new GenericConfigElement("rootPath");
        rootPath.setValue(ROOT_PATH);
        cfg1.addChild(rootPath);
    
        ContentContext filesysContext = (ContentContext) driver.createContext(STORE_NAME, cfg1);
      
        DiskSharedDevice share = new DiskSharedDevice("test", driver, filesysContext);
        
        return share;
    }

    /**
     * Test Create File
     */
    public void testCreateFile() throws Exception
    {
        logger.debug("testCreatedFile");
        ServerConfiguration scfg = new ServerConfiguration("testServer");
        TestServer testServer = new TestServer("testServer", scfg);
        SrvSession testSession = new TestSrvSession(666, testServer, "test", "remoteName");
        DiskSharedDevice share = getDiskSharedDevice();
        TreeConnection testConnection = testServer.getTreeConnection(share);
        final RetryingTransactionHelper tran = transactionService.getRetryingTransactionHelper();
        
        class TestContext
        {     
            NodeRef testNodeRef;    
        };
        
        final TestContext testContext = new TestContext();
      
        /**
          * Step 1 : Create a new file in read/write mode and add some content.
           */
        int openAction = FileAction.CreateNotExist;
        
        final String FILE_NAME="testCreateFile.new";
        final String FILE_PATH="\\"+FILE_NAME;
                  
        FileOpenParams params = new FileOpenParams(FILE_PATH, openAction, AccessMode.ReadWrite, FileAttribute.NTNormal, 0);
                
        final NetworkFile file = driver.createFile(testSession, testConnection, params);
        assertNotNull("file is null", file);
        assertFalse("file is read only, should be read-write", file.isReadOnly());
        
        RetryingTransactionCallback<Void> writeStuffCB = new RetryingTransactionCallback<Void>() {

            @Override
            public Void execute() throws Throwable
            {
                byte[] stuff = "Hello World".getBytes();
                file.writeFile(stuff, stuff.length, 0, 0);
                file.close();  // needed to actually flush content to node
                return null;
            }
        };
        tran.doInTransaction(writeStuffCB);
        
        RetryingTransactionCallback<Void> validateCB = new RetryingTransactionCallback<Void>() {

            @Override
            public Void execute() throws Throwable
            {
                NodeRef companyHome = repositoryHelper.getCompanyHome();
                NodeRef newNode = nodeService.getChildByName(companyHome, ContentModel.ASSOC_CONTAINS, FILE_NAME);
                testContext.testNodeRef = newNode;
                assertNotNull("can't find new node", newNode);
                Serializable content = nodeService.getProperty(newNode, ContentModel.PROP_CONTENT);
                assertNotNull("content is null", content);       
                return null;
            }
        };
        tran.doInTransaction(validateCB);
        
        // now validate that the new node is in the correct location and has the correct name
        FileInfo info = driver.getFileInformation(testSession, testConnection, FILE_PATH);
        assertNotNull("info is null", info);
        
        NodeRef n2 = driver.getNodeForPath(testConnection, FILE_PATH);
        assertEquals("get Node For Path returned different node", testContext.testNodeRef, n2);
        
        /**
         * Step 2 : Negative Test Attempt to create the same file again
         */
        try
        {
            driver.createFile(testSession, testConnection, params);
            fail("File exists not detected");
        }
        catch (FileExistsException fe)
        {
            // expect to go here
        }
        
        // Clean up so we could run the test again
        driver.deleteFile(testSession, testConnection, FILE_PATH);

        /**
         * Step 3 : create a file in a new directory in read only mode
         */        
        String FILE2_PATH = TEST_ROOT_DOS_PATH + FILE_PATH;
        
        FileOpenParams dirParams = new FileOpenParams(TEST_ROOT_DOS_PATH, openAction, AccessMode.ReadOnly, FileAttribute.NTDirectory, 0);
        driver.createDirectory(testSession, testConnection, dirParams);
        
        FileOpenParams file2Params = new FileOpenParams(FILE2_PATH, openAction, AccessMode.ReadOnly, FileAttribute.NTNormal, 0);
        NetworkFile file2 = driver.createFile(testSession, testConnection, file2Params);
        
        // clean up so we could run the test again
        driver.deleteFile(testSession, testConnection, FILE2_PATH);
    }
    
    /**
     * Unit test of delete file
     */
    public void testDeleteFile() throws Exception
    {
        logger.debug("testDeleteFile");
        
        ServerConfiguration scfg = new ServerConfiguration("testServer");
        TestServer testServer = new TestServer("testServer", scfg);
        SrvSession testSession = new TestSrvSession(666, testServer, "test", "remoteName");
        
        GenericConfigElement cfg1 =  new GenericConfigElement("filesystem");
                
        GenericConfigElement store =  new GenericConfigElement("store");
        store.setValue(STORE_NAME);
        cfg1.addChild(store);
        
        GenericConfigElement rootPath =  new GenericConfigElement("rootPath");
        rootPath.setValue(ROOT_PATH);
        cfg1.addChild(rootPath);
        
        ContentContext filesysContext = (ContentContext) driver.createContext(STORE_NAME, cfg1);
        
        DiskSharedDevice share = new DiskSharedDevice("test", driver, filesysContext);
        TreeConnection testConnection = testServer.getTreeConnection(share);
        
        final RetryingTransactionHelper tran = transactionService.getRetryingTransactionHelper();


        /**
         * Step 1 : Create a new file in read/write mode and add some content.
        */
        int openAction = FileAction.CreateNotExist;
        String FILE_PATH="\\testDeleteFile.new";
          
        FileOpenParams params = new FileOpenParams(FILE_PATH, openAction, AccessMode.ReadWrite, FileAttribute.NTNormal, 0);
                
        final NetworkFile file = driver.createFile(testSession, testConnection, params);
        assertNotNull("file is null", file);
        assertFalse("file is read only, should be read-write", file.isReadOnly());
            
        RetryingTransactionCallback<Void> writeStuffCB = new RetryingTransactionCallback<Void>() {

            @Override
            public Void execute() throws Throwable
            {
                byte[] stuff = "Hello World".getBytes();
                file.writeFile(stuff, stuff.length, 0, 0);
                file.close();  // needed to actually flush content to node
                return null;
            }
        };
        tran.doInTransaction(writeStuffCB);
                 
        /**
          * Step 1: Delete file by path
          */
        driver.deleteFile(testSession, testConnection, FILE_PATH);
        
        /**
         * Step 2: Negative test - Delete file again
         */
        try
        {
            driver.deleteFile(testSession, testConnection, FILE_PATH);
            fail("delete a non existent file");
        }
        catch (IOException fe)
        {
                // expect to go here
        }
    }
    
    /**
     * Test Set Info
     * 
     * Three flags set
     * <ol>
     * <li>SetDeleteOnClose</li>
     * <li>SetCreationDate</li>
     * <li>SetModifyDate</li>
     * </ol>
     */
    /*
     * MER : I can't see what DeleteOnClose does.  Test commented out  
     */
    public void testSetFileInfo() throws Exception
    {
        logger.debug("testSetFileInfo");
        ServerConfiguration scfg = new ServerConfiguration("testServer");
        TestServer testServer = new TestServer("testServer", scfg);
        final SrvSession testSession = new TestSrvSession(666, testServer, "test", "remoteName");
        DiskSharedDevice share = getDiskSharedDevice();
        final TreeConnection testConnection = testServer.getTreeConnection(share);
        final RetryingTransactionHelper tran = transactionService.getRetryingTransactionHelper();
        
        Date now = new Date();
        
        // CREATE 6 hours ago
        final Date CREATED = new Date(now.getTime() - 1000 * 60 * 60 * 6);
        // Modify one hour ago
        final Date MODIFIED = new Date(now.getTime() - 1000 * 60 * 60 * 1);
        
        class TestContext
        {     
            NodeRef testNodeRef;    
        };
        
        final TestContext testContext = new TestContext();
      
        /**
          * Step 1 : Create a new file in read/write mode and add some content.
          * Call SetInfo to set the creation date
          */
        int openAction = FileAction.CreateNotExist;
        
        final String FILE_NAME="testSetFileInfo.txt";
        final String FILE_PATH="\\"+FILE_NAME;
                  
        final FileOpenParams params = new FileOpenParams(FILE_PATH, openAction, AccessMode.ReadWrite, FileAttribute.NTNormal, 0);
                
        final NetworkFile file = driver.createFile(testSession, testConnection, params);
        assertNotNull("file is null", file);
        assertFalse("file is read only, should be read-write", file.isReadOnly());
        
        RetryingTransactionCallback<Void> writeStuffCB = new RetryingTransactionCallback<Void>() {

            @Override
            public Void execute() throws Throwable
            {
                byte[] stuff = "Hello World".getBytes();
                file.writeFile(stuff, stuff.length, 0, 0);
                file.close();  // needed to actually flush content to node
              
                FileInfo info = driver.getFileInformation(testSession, testConnection, FILE_PATH);
                info.setFileInformationFlags(FileInfo.SetModifyDate);
                info.setModifyDateTime(MODIFIED.getTime());
                driver.setFileInformation(testSession, testConnection, FILE_PATH, info);
                return null;
            }
        };
        tran.doInTransaction(writeStuffCB);
        
        RetryingTransactionCallback<Void> validateCB = new RetryingTransactionCallback<Void>() {

            @Override
            public Void execute() throws Throwable
            {
                NodeRef companyHome = repositoryHelper.getCompanyHome();
                NodeRef newNode = nodeService.getChildByName(companyHome, ContentModel.ASSOC_CONTAINS, FILE_NAME);
                testContext.testNodeRef = newNode;
                assertNotNull("can't find new node", newNode);
                Serializable content = nodeService.getProperty(newNode, ContentModel.PROP_CONTENT);
                assertNotNull("content is null", content);     
                Date modified = (Date)nodeService.getProperty(newNode, ContentModel.PROP_MODIFIED);
                assertEquals("modified time not set correctly", MODIFIED, modified);
                return null;
            }
        };
        tran.doInTransaction(validateCB);
        
        /**
         * Step 2: Change the created date
         */
        logger.debug("Step 2: Change the created date");
        RetryingTransactionCallback<Void> changeCreatedCB = new RetryingTransactionCallback<Void>() {

            @Override
            public Void execute() throws Throwable
            {
                FileInfo info = driver.getFileInformation(testSession, testConnection, FILE_PATH);
                info.setFileInformationFlags(FileInfo.SetCreationDate);
                info.setCreationDateTime(CREATED.getTime());
                driver.setFileInformation(testSession, testConnection, FILE_PATH, info);
                return null;
            }
        };
        tran.doInTransaction(changeCreatedCB);
  
        RetryingTransactionCallback<Void> validateCreatedCB = new RetryingTransactionCallback<Void>() {

            @Override
            public Void execute() throws Throwable
            {
                NodeRef companyHome = repositoryHelper.getCompanyHome();
                NodeRef newNode = nodeService.getChildByName(companyHome, ContentModel.ASSOC_CONTAINS, FILE_NAME);
                testContext.testNodeRef = newNode;
                assertNotNull("can't find new node", newNode);
                Serializable content = nodeService.getProperty(newNode, ContentModel.PROP_CONTENT);
                assertNotNull("content is null", content);     
                Date created = (Date)nodeService.getProperty(newNode, ContentModel.PROP_CREATED);
                assertEquals("created time not set correctly", CREATED, created);
                return null;
            }
        };
        tran.doInTransaction(validateCreatedCB);
        
//        /**
//         * Step 3: Test 
//         */
//        logger.debug("Step 3: test deleteOnClose");
//        RetryingTransactionCallback<Void> deleteOnCloseCB = new RetryingTransactionCallback<Void>() {
//
//            @Override
//            public Void execute() throws Throwable
//            {
//               NetworkFile f2 = driver.openFile(testSession, testConnection, params);
//                 
//               FileInfo info = driver.getFileInformation(testSession, testConnection, FILE_PATH);
//               info.setFileInformationFlags(FileInfo.SetDeleteOnClose);
//               driver.setFileInformation(testSession, testConnection, FILE_PATH, info);
//                
//               byte[] stuff = "Update".getBytes();
//               f2.writeFile(stuff, stuff.length, 0, 0);
//               f2.close();  // needed to actually flush content to node
//     
//               return null;
//            }
//        };
//        tran.doInTransaction(deleteOnCloseCB);
//  
//        RetryingTransactionCallback<Void> validateDeleteOnCloseCB = new RetryingTransactionCallback<Void>() {
//
//            @Override
//            public Void execute() throws Throwable
//            {
//                NodeRef companyHome = repositoryHelper.getCompanyHome();
//                NodeRef newNode = nodeService.getChildByName(companyHome, ContentModel.ASSOC_CONTAINS, FILE_NAME);
//                assertNull("can still find new node", newNode);
//                return null;
//            }
//        };
//        tran.doInTransaction(validateDeleteOnCloseCB);
        
        // clean up so we could run the test again
        driver.deleteFile(testSession, testConnection, FILE_PATH);    
        
    } // test set file info

    
    /**
     * Test Open File
     * 
     * MER DISABLED TEST 22/03/2011 won't run.
     */
    public void DISABLED_testOpenFile() throws Exception
    {    
        logger.debug("testOpenFile");
        ServerConfiguration scfg = new ServerConfiguration("testServer");
        TestServer testServer = new TestServer("testServer", scfg);
        SrvSession testSession = new TestSrvSession(666, testServer, "test", "remoteName");
        DiskSharedDevice share = getDiskSharedDevice();
        TreeConnection testConnection = testServer.getTreeConnection(share);
        final RetryingTransactionHelper tran = transactionService.getRetryingTransactionHelper();

        final String FILE_NAME="testOpenFileY.whatever";

        /**
         * Step 1 : Negative test - try to open a file that does not exist
         */ 
        String FILE_PATH="\\" + FILE_NAME;
          
        FileOpenParams params = new FileOpenParams(FILE_PATH, FileAction.CreateNotExist, AccessMode.ReadWrite, FileAttribute.NTNormal, 0);
        try
        {
            NetworkFile file = driver.openFile(testSession, testConnection, params);
            fail ("managed to open non existant file!");
        }
        catch (IOException ie)
        {
           // expect to go here
        }
        
        /**
         * Step 2: Now create the file through the node service and open it.
         */
        RetryingTransactionCallback<Void> createFileCB = new RetryingTransactionCallback<Void>() {

            @Override
            public Void execute() throws Throwable
            {
                NodeRef companyHome = repositoryHelper.getCompanyHome();
                ChildAssociationRef ref = nodeService.createNode(companyHome, ContentModel.ASSOC_CONTAINS, QName.createQName(NamespaceService.CONTENT_MODEL_1_0_URI, FILE_NAME), ContentModel.TYPE_CONTENT);
                nodeService.setProperty(ref.getChildRef(), ContentModel.PROP_NAME, FILE_NAME);
                return null;
            }
        };
        tran.doInTransaction(createFileCB, false, true);
        
        NetworkFile file = driver.openFile(testSession, testConnection, params);
        assertNotNull(file);
        
        driver.deleteFile(testSession, testConnection, FILE_PATH);
    } // testOpenFile

    
    /**
     * Unit test of file exists
     */
    public void testFileExists() throws Exception
    {
        logger.debug("testFileExists");
        ServerConfiguration scfg = new ServerConfiguration("testServer");
        TestServer testServer = new TestServer("testServer", scfg);
        SrvSession testSession = new TestSrvSession(666, testServer, "test", "remoteName");
        DiskSharedDevice share = getDiskSharedDevice();
        TreeConnection testConnection = testServer.getTreeConnection(share);
        final RetryingTransactionHelper tran = transactionService.getRetryingTransactionHelper();
        
        String FILE_PATH="\\testFileExists.new";
  
        /**
         * Step 1 : Call FileExists for a file which does not exist
         */
        int status = driver.fileExists(testSession, testConnection, FILE_PATH);
        assertEquals(status, 0);
        
        /**
         * Step 2: Create a new file in read/write mode and add some content.
         */
        int openAction = FileAction.CreateNotExist;

        FileOpenParams params = new FileOpenParams(FILE_PATH, openAction, AccessMode.ReadWrite, FileAttribute.NTNormal, 0);
                
        final NetworkFile file = driver.createFile(testSession, testConnection, params);
        assertNotNull("file is null", file);
        assertFalse("file is read only, should be read-write", file.isReadOnly());
        
        RetryingTransactionCallback<Void> createFileCB = new RetryingTransactionCallback<Void>() {

            @Override
            public Void execute() throws Throwable
            {
                byte[] stuff = "Hello World".getBytes();
                file.writeFile(stuff, stuff.length, 0, 0);
                file.close();
            
                return null;
            }
        };
        tran.doInTransaction(createFileCB, false, true);
         
        status = driver.fileExists(testSession, testConnection, FILE_PATH);
        assertEquals(status, 1);
         
        /**
          * Step 3 : Delete the node - check status goes back to 0
          */
        driver.deleteFile(testSession, testConnection, FILE_PATH);
        
        status = driver.fileExists(testSession, testConnection, FILE_PATH);
        assertEquals(status, 0); 
    
        // BODGE - there's a dangling transaction that needs getting rid of
        // Work around for ALF-7674 
        UserTransaction txn = transactionService.getUserTransaction();
        assertNotNull("transaction leaked", txn);
        txn.getStatus();
        txn.rollback();
    
   
    } // testFileExists
    
    /**
     * Unit test of rename file
     */
    public void testRenameFile() throws Exception
    {  
        logger.debug("testRenameFile");
        ServerConfiguration scfg = new ServerConfiguration("testServer");
        TestServer testServer = new TestServer("testServer", scfg);
        SrvSession testSession = new TestSrvSession(666, testServer, "test", "remoteName");
        DiskSharedDevice share = getDiskSharedDevice();
        final TreeConnection testConnection = testServer.getTreeConnection(share);
        final RetryingTransactionHelper tran = transactionService.getRetryingTransactionHelper();
        
        final String FILE_PATH1=TEST_ROOT_DOS_PATH + "\\SourceFile1.new";
        final String FILE_NAME2 = "SourceFile2.new";
        final String FILE_PATH2=TEST_ROOT_DOS_PATH +"\\" + FILE_NAME2;
        final String FILE_PATH3=TEST_ROOT_DOS_PATH +"\\SourceFile3.new";
        
        FileOpenParams dirParams = new FileOpenParams(TEST_ROOT_DOS_PATH, 0, AccessMode.ReadOnly, FileAttribute.NTDirectory, 0);
        driver.createDirectory(testSession, testConnection, dirParams);

        FileOpenParams params1 = new FileOpenParams(FILE_PATH1, 0, AccessMode.ReadWrite, FileAttribute.NTNormal, 0);
        final NetworkFile file1 = driver.createFile(testSession, testConnection, params1);
  
        FileOpenParams params3 = new FileOpenParams(FILE_PATH3, 0, AccessMode.ReadWrite, FileAttribute.NTNormal, 0);
        final NetworkFile file3 = driver.createFile(testSession, testConnection, params3);
        
        /**
         * Step 1 : Negative test, Call Rename for a file which does not exist
         */
        try
        {
            driver.renameFile(testSession, testConnection, "\\Wibble\\wobble", FILE_PATH1);
            fail("rename did not detect missing file");
        }
        catch (IOException e)
        {
            // expect to go here
        }
          
        /**
         * Step 2: Negative test, Call Rename for a destination that does not exist.
         */
        try
        {
            driver.renameFile(testSession, testConnection, FILE_PATH1, "\\wibble\\wobble");
            fail("rename did not detect missing file");
        }
        catch (IOException e)
        {
            // expect to go here
        }
        
        /**
         * Step 3: Rename a file to a destination that is a file rather than a directory
         */
        try
        {
            driver.renameFile(testSession, testConnection, FILE_PATH1, FILE_PATH3);
            fail("rename did not detect missing file");
        }
        catch (IOException e)
        {
            // expect to go here
        }
                 
        /**
         * Step 4: Successfully rename a file - check the name, props and content.
         */
        final String LAST_NAME= "Bloggs";
        
        RetryingTransactionCallback<Void> setPropertiesCB = new RetryingTransactionCallback<Void>() {

            @Override
            public Void execute() throws Throwable
            {
                final NodeRef file1NodeRef = driver.getNodeForPath(testConnection, FILE_PATH1);
                assertNotNull("node ref not found", file1NodeRef);
                nodeService.setProperty(file1NodeRef, ContentModel.PROP_LASTNAME, LAST_NAME);
         
                return null;
            }
        };
        tran.doInTransaction(setPropertiesCB, false, true);
        
        driver.renameFile(testSession, testConnection, FILE_PATH1, FILE_PATH2);
       
        RetryingTransactionCallback<Void> validateCB = new RetryingTransactionCallback<Void>() {

            @Override
            public Void execute() throws Throwable
            {
                NodeRef file2NodeRef = driver.getNodeForPath(testConnection, FILE_PATH2);
                //assertEquals("node ref has changed on a rename", file1NodeRef, file2NodeRef);
                assertEquals(nodeService.getProperty(file2NodeRef, ContentModel.PROP_LASTNAME), LAST_NAME);
                ChildAssociationRef parentRef = nodeService.getPrimaryParent(file2NodeRef);
                assertTrue("file has wrong assoc local name", parentRef.getQName().getLocalName().equals(FILE_NAME2));
                assertTrue("not primary assoc", parentRef.isPrimary());

                return null;
            }
        };
        tran.doInTransaction(validateCB, false, true);

        /**
         * Step 5: Rename to another directory
         */
        String DIR_NEW_PATH = TEST_ROOT_DOS_PATH + "\\NewDir";
        String NEW_PATH = DIR_NEW_PATH + "\\File2";
        FileOpenParams params5 = new FileOpenParams(DIR_NEW_PATH, 0, AccessMode.ReadWrite, FileAttribute.NTNormal, 0);
        driver.createDirectory(testSession, testConnection, params5);
        
        NodeRef newDirNodeRef = driver.getNodeForPath(testConnection, DIR_NEW_PATH);
        
        driver.renameFile(testSession, testConnection, FILE_PATH2, NEW_PATH);
        
        NodeRef file5NodeRef = driver.getNodeForPath(testConnection, NEW_PATH);
        ChildAssociationRef parentRef5 = nodeService.getPrimaryParent(file5NodeRef);
        
        assertTrue(parentRef5.getParentRef().equals(newDirNodeRef));
        
//        /** 
//         * Step 5: rename to self - check no damage.
//         */
//        try
//        {
//            driver.renameFile(testSession, testConnection, FILE_PATH2, FILE_PATH2);
//            fail("rename did not detect rename to self");
//        }
//        catch (IOException e)
//        {
            // expect to go here
//        }
        
    } // testRenameFile


    /**
     * Unit test of rename versionable file
     */
    public void testScenarioRenameVersionableFile() throws Exception
    {  
        logger.debug("testScenarioRenameVersionableFile");
        
        ServerConfiguration scfg = new ServerConfiguration("testServer");
        TestServer testServer = new TestServer("testServer", scfg);
        SrvSession testSession = new TestSrvSession(666, testServer, "test", "remoteName");
        DiskSharedDevice share = getDiskSharedDevice();
        final TreeConnection testConnection = testServer.getTreeConnection(share);
        final RetryingTransactionHelper tran = transactionService.getRetryingTransactionHelper();

        final String FILE_PATH1=TEST_ROOT_DOS_PATH + "\\SourceFile1.new";
        final String FILE_PATH2=TEST_ROOT_DOS_PATH + "\\SourceFile2.new";
        
        class TestContext
        {
        };
        
        final TestContext testContext = new TestContext();
   
        FileOpenParams dirParams = new FileOpenParams(TEST_ROOT_DOS_PATH, 0, AccessMode.ReadOnly, FileAttribute.NTDirectory, 0);
        driver.createDirectory(testSession, testConnection, dirParams);

        FileOpenParams params1 = new FileOpenParams(FILE_PATH1, 0, AccessMode.ReadWrite, FileAttribute.NTNormal, 0);
        NetworkFile file1 = driver.createFile(testSession, testConnection, params1);
        
        /**
         * Make Node 1 versionable
         */
        final String LAST_NAME= "Bloggs";
         
        RetryingTransactionCallback<Void> makeVersionableCB = new RetryingTransactionCallback<Void>() {

            @Override
            public Void execute() throws Throwable
            {
                NodeRef file1NodeRef = driver.getNodeForPath(testConnection, FILE_PATH1);
                nodeService.addAspect(file1NodeRef, ContentModel.ASPECT_VERSIONABLE, null);
                
                ContentWriter contentWriter2 = contentService.getWriter(file1NodeRef, ContentModel.PROP_CONTENT, true);
                contentWriter2.putContent("test rename versionable");
                
                nodeService.setProperty(file1NodeRef, ContentModel.PROP_LASTNAME, LAST_NAME);
                nodeService.setProperty(file1NodeRef, TransferModel.PROP_ENDPOINT_PROTOCOL, "http");

                return null;
            }
        };
        tran.doInTransaction(makeVersionableCB, false, true);
  
        /**
         * Step 1: Successfully rename a versionable file - check the name, props and content.
         * TODO Check primary assoc, peer assocs, child assocs, modified date, created date, nodeid, permissions.
         */
        driver.renameFile(testSession, testConnection, FILE_PATH1, FILE_PATH2);
        
        RetryingTransactionCallback<Void> validateVersionableCB = new RetryingTransactionCallback<Void>() {

            @Override
            public Void execute() throws Throwable
            {
                NodeRef file2NodeRef = driver.getNodeForPath(testConnection, FILE_PATH2);
                assertNotNull("file2 node ref is null", file2NodeRef);
                //assertEquals(nodeService.getProperty(file2NodeRef, ContentModel.PROP_LASTNAME), LAST_NAME);
                assertTrue("does not have versionable aspect", nodeService.hasAspect(file2NodeRef, ContentModel.ASPECT_VERSIONABLE));   
                assertTrue("sample property is null", nodeService.getProperty(file2NodeRef, TransferModel.PROP_ENDPOINT_PROTOCOL) != null);     
                
                return null;
            }
        };
        tran.doInTransaction(validateVersionableCB, false, true);

    } // testRenameVersionable
    

    /**
     * This test tries to simulate the shuffling that is done by MS Word 2003 upon file save
     * 
     * a) TEST.DOC
     * b) Save to ~WRDnnnn.TMP
     * c) Delete ~WRLnnnn.TMP
     * d) Rename TEST.DOC ~WDLnnnn.TMP
     * e) Delete TEST.DOC
     * f) Rename ~WRDnnnn.TMP to TEST.DOC
     * g) Delete ~WRLnnnn.TMP
     * 
     * We need to check that properties, aspects, primary assocs, secondary assocs, peer assocs, node type, 
     * version history, creation date are maintained.
     */
    public void testScenarioMSWord2003SaveShuffle() throws Exception
    {
        logger.debug("testScenarioMSWord2003SaveShuffle");
        final String FILE_NAME = "TEST.DOC";
        final String FILE_TITLE = "Test document";
        final String FILE_DESCRIPTION = "This is a test document to test CIFS shuffle";
        final String FILE_OLD_TEMP = "~WRL0002.TMP";
        final String FILE_NEW_TEMP = "~WRD0002.TMP";
        
        final QName RESIDUAL_MTTEXT = QName.createQName("{gsxhjsx}", "whatever");
        
        class TestContext
        {
            NetworkFile firstFileHandle;
            NetworkFile newFileHandle;
            NetworkFile oldFileHandle;
            
            NodeRef testNodeRef;   // node ref of test.doc
            
            Serializable testCreatedDate;
        };
        
        final TestContext testContext = new TestContext();
        
        final String TEST_DIR = TEST_ROOT_DOS_PATH + "\\testScenarioMSWord2003SaveShuffle";
        
        ServerConfiguration scfg = new ServerConfiguration("testServer");
        TestServer testServer = new TestServer("testServer", scfg);
        final SrvSession testSession = new TestSrvSession(666, testServer, "test", "remoteName");
        DiskSharedDevice share = getDiskSharedDevice();
        final TreeConnection testConnection = testServer.getTreeConnection(share);
        final RetryingTransactionHelper tran = transactionService.getRetryingTransactionHelper();        

        /**
         * Clean up just in case garbage is left from a previous run
         */
        RetryingTransactionCallback<Void> deleteGarbageFileCB = new RetryingTransactionCallback<Void>() {

            @Override
            public Void execute() throws Throwable
            {
                driver.deleteFile(testSession, testConnection, TEST_DIR + "\\" + FILE_NAME);
                return null;
            }
        };
     
        /**
         * Create a file in the test directory
         */    
        
        try
        {
            tran.doInTransaction(deleteGarbageFileCB);
        }
        catch (Exception e)
        {
            // expect to go here
        }
        
        RetryingTransactionCallback<Void> createFileCB = new RetryingTransactionCallback<Void>() {

            @Override
            public Void execute() throws Throwable
            {
  
                /**
                 * Create the test directory we are going to use 
                 */
                FileOpenParams createRootDirParams = new FileOpenParams(TEST_ROOT_DOS_PATH, 0, AccessMode.ReadWrite, FileAttribute.NTNormal, 0);
                FileOpenParams createDirParams = new FileOpenParams(TEST_DIR, 0, AccessMode.ReadWrite, FileAttribute.NTNormal, 0);
                driver.createDirectory(testSession, testConnection, createRootDirParams);
                driver.createDirectory(testSession, testConnection, createDirParams);
                
                /**
                 * Create the file we are going to use
                 */
                FileOpenParams createFileParams = new FileOpenParams(TEST_DIR + "\\" + FILE_NAME, 0, AccessMode.ReadWrite, FileAttribute.NTNormal, 0);
                testContext.firstFileHandle = driver.createFile(testSession, testConnection, createFileParams);
                assertNotNull(testContext.firstFileHandle);
                
                // now load up the node with lots of other stuff that we will test to see if it gets preserved during the
                // shuffle.
                testContext.testNodeRef = driver.getNodeForPath(testConnection, TEST_DIR + "\\" + FILE_NAME);
                
                // test non CM namespace property
                nodeService.setProperty(testContext.testNodeRef, TransferModel.PROP_ENABLED, true);
                // test CM property not related to an aspect
                nodeService.setProperty(testContext.testNodeRef, ContentModel.PROP_ADDRESSEE, "Fred");
                
                nodeService.setProperty(testContext.testNodeRef, ContentModel.PROP_TITLE, FILE_TITLE);
                nodeService.setProperty(testContext.testNodeRef, ContentModel.PROP_DESCRIPTION, FILE_DESCRIPTION);
                
                /**
                 * MLText value - also a residual value in a non cm namespace
                 */
                MLText mltext = new MLText();
                mltext.addValue(Locale.FRENCH, "Bonjour");
                mltext.addValue(Locale.ENGLISH, "Hello");
                mltext.addValue(Locale.ITALY, "Buongiorno");
                mlAwareNodeService.setProperty(testContext.testNodeRef, RESIDUAL_MTTEXT, mltext);

                // classifiable chosen since its not related to any properties.
                nodeService.addAspect(testContext.testNodeRef, ContentModel.ASPECT_CLASSIFIABLE, null);
                //nodeService.createAssociation(testContext.testNodeRef, targetRef, assocTypeQName);
        
                return null;
            }
        };
        tran.doInTransaction(createFileCB, false, true);
        
        /**
         * Write some content to the test file
         */
        RetryingTransactionCallback<Void> writeFileCB = new RetryingTransactionCallback<Void>() {

            @Override
            public Void execute() throws Throwable
            {
                String testContent = "MS Word 2003 shuffle test";
                byte[] testContentBytes = testContent.getBytes();
                testContext.firstFileHandle.writeFile(testContentBytes, testContentBytes.length, 0, 0);
                testContext.firstFileHandle.close();   
                
                testContext.testCreatedDate = nodeService.getProperty(testContext.testNodeRef, ContentModel.PROP_CREATED);
                
                MLText multi = (MLText)mlAwareNodeService.getProperty(testContext.testNodeRef, RESIDUAL_MTTEXT) ;
                multi.getValues();
     
     
                return null;
            }
        };
        tran.doInTransaction(writeFileCB, false, true);
        
        /**
         * b) Save the new file
         */
        RetryingTransactionCallback<Void> saveNewFileCB = new RetryingTransactionCallback<Void>() {

            @Override
            public Void execute() throws Throwable
            {
                FileOpenParams createFileParams = new FileOpenParams(TEST_DIR + "\\" + FILE_NEW_TEMP, 0, AccessMode.ReadWrite, FileAttribute.NTNormal, 0);
                testContext.newFileHandle = driver.createFile(testSession, testConnection, createFileParams);
                assertNotNull(testContext.newFileHandle);
                String testContent = "MS Word 2003 shuffle test This is new content";
                byte[] testContentBytes = testContent.getBytes();
                testContext.newFileHandle.writeFile(testContentBytes, testContentBytes.length, 0, 0);
                testContext.newFileHandle.close();
              
                return null;
            }
        };
        tran.doInTransaction(saveNewFileCB, false, true);
        
        /**
         * rename the old file
         */
        RetryingTransactionCallback<Void> renameOldFileCB = new RetryingTransactionCallback<Void>() {

            @Override
            public Void execute() throws Throwable
            {
                driver.renameFile(testSession, testConnection, TEST_DIR + "\\" + FILE_NAME, TEST_DIR + "\\" + FILE_OLD_TEMP);
                return null;
            }
        };
        tran.doInTransaction(renameOldFileCB, false, true);
        

        RetryingTransactionCallback<Void> validateOldFileGoneCB = new RetryingTransactionCallback<Void>() {

            @Override
            public Void execute() throws Throwable
            {   
                try
                {
                    driver.deleteFile(testSession, testConnection, TEST_DIR + "\\" + FILE_NAME);
                }
                catch (IOException e)
                { 
                    // expect to go here since previous step renamed the file.
                }
                
                return null;
            }
        };
        tran.doInTransaction(validateOldFileGoneCB, false, true);
        
        /**
         * Move the new file into place, stuff should get shuffled
         */
        RetryingTransactionCallback<Void> moveNewFileCB = new RetryingTransactionCallback<Void>() {

            @Override
            public Void execute() throws Throwable
            {
                driver.renameFile(testSession, testConnection, TEST_DIR + "\\" + FILE_NEW_TEMP, TEST_DIR + "\\" + FILE_NAME); 
                return null;
            }
        };
        
        tran.doInTransaction(moveNewFileCB, false, true);
        
        RetryingTransactionCallback<Void> validateCB = new RetryingTransactionCallback<Void>() {

            @Override
            public Void execute() throws Throwable
            {
               NodeRef shuffledNodeRef = driver.getNodeForPath(testConnection, TEST_DIR + "\\" + FILE_NAME);
               
               Map<QName, Serializable> props = nodeService.getProperties(shuffledNodeRef);
           
               // Check trx:enabled has been shuffled.
               assertTrue("node does not contain shuffled ENABLED property", props.containsKey(TransferModel.PROP_ENABLED));
               // check my residual MLText has been transferred
               assertTrue(props.containsKey(RESIDUAL_MTTEXT));
               
               // Check the titled aspect is correct
               assertEquals("name wrong", FILE_NAME, nodeService.getProperty(shuffledNodeRef, ContentModel.PROP_NAME) );
               assertEquals("title wrong", FILE_TITLE, nodeService.getProperty(shuffledNodeRef, ContentModel.PROP_TITLE) );
               assertEquals("description wrong", FILE_DESCRIPTION, nodeService.getProperty(shuffledNodeRef, ContentModel.PROP_DESCRIPTION) );

               // commented out due to ALF-7641
               // CIFS shuffle, does not preseve MLText values.
               // Map<QName, Serializable> mlProps = mlAwareNodeService.getProperties(shuffledNodeRef);
               
               // MLText multi = (MLText)mlAwareNodeService.getProperty(shuffledNodeRef, RESIDUAL_MTTEXT) ;
               // multi.getValues();
               
               // check auditable properties 
               // commented out due to ALF-7635
               // assertEquals("creation date not preserved", ((Date)testContext.testCreatedDate).getTime(), ((Date)nodeService.getProperty(shuffledNodeRef, ContentModel.PROP_CREATED)).getTime());
               
               // commented out due to ALF-7628 
               // assertEquals("ADDRESSEE PROPERTY Not copied", "Fred", nodeService.getProperty(shuffledNodeRef, ContentModel.PROP_ADDRESSEE));
               // assertTrue("CLASSIFIABLE aspect not present", nodeService.hasAspect(shuffledNodeRef, ContentModel.ASPECT_CLASSIFIABLE));
               
               // commented out due to ALF-7584.
               // assertEquals("noderef changed", testContext.testNodeRef, shuffledNodeRef);
               return null;
            }
        };
        
        tran.doInTransaction(validateCB, true, true);
        
        /**
         * Clean up just in case garbage is left from a previous run
         */
        RetryingTransactionCallback<Void> deleteOldFileCB = new RetryingTransactionCallback<Void>() {

            @Override
            public Void execute() throws Throwable
            {
                driver.deleteFile(testSession, testConnection, TEST_DIR + "\\" + FILE_OLD_TEMP);
                return null;
            }
        };
        
        tran.doInTransaction(deleteOldFileCB, false, true);
        
    } // testScenarioMSWord2003SaveShuffle
    
    
    /**
     * This test tries to simulate the shuffling that is done by MS Word 2003 
     * with backup enabled upon file save
     * 
     * a) TEST.DOC
     * b) Save to ~WRDnnnn.TMP
     * c) Delete "Backup of TEST.DOC"
     * d) Rename TEST.DOC to "Backup of TEST.DOC"
     * e) Delete TEST.DOC
     * f) Rename ~WRDnnnn.TMP to TEST.DOC
     * 
     * We need to check that properties, aspects, primary assocs, secondary assocs, peer assocs, node type, 
     * version history, creation date are maintained.
     */
    public void testScenarioMSWord2003SaveShuffleWithBackup() throws Exception
    {
        logger.debug("testScenarioMSWord2003SaveShuffleWithBackup");
        final String FILE_NAME = "TEST.DOC";
        final String FILE_OLD_TEMP = "Backup of TEST.DOC";
        final String FILE_NEW_TEMP = "~WRD0002.TMP";
        
        class TestContext
        {
            NetworkFile firstFileHandle;
            NetworkFile newFileHandle;            
            NodeRef testNodeRef;   // node ref of test.doc
        };
        
        final TestContext testContext = new TestContext();
        
        final String TEST_ROOT_DIR = "\\ContentDiskDriverTest";
        final String TEST_DIR = "\\ContentDiskDriverTest\\testScenarioMSWord2003SaveShuffleWithBackup";
        
        ServerConfiguration scfg = new ServerConfiguration("testServer");
        TestServer testServer = new TestServer("testServer", scfg);
        final SrvSession testSession = new TestSrvSession(666, testServer, "test", "remoteName");
        DiskSharedDevice share = getDiskSharedDevice();
        final TreeConnection testConnection = testServer.getTreeConnection(share);
        final RetryingTransactionHelper tran = transactionService.getRetryingTransactionHelper();
             
        /**
         * Create a file in the test directory
         */            
        RetryingTransactionCallback<Void> createFileCB = new RetryingTransactionCallback<Void>() {

            @Override
            public Void execute() throws Throwable
            {
                /**
                 * Create the test directory we are going to use 
                 */
                FileOpenParams createRootDirParams = new FileOpenParams(TEST_ROOT_DIR, 0, AccessMode.ReadWrite, FileAttribute.NTNormal, 0);
                FileOpenParams createDirParams = new FileOpenParams(TEST_DIR, 0, AccessMode.ReadWrite, FileAttribute.NTNormal, 0);
                driver.createDirectory(testSession, testConnection, createRootDirParams);
                driver.createDirectory(testSession, testConnection, createDirParams);
                
                /**
                 * Create the file we are going to use
                 */
                FileOpenParams createFileParams = new FileOpenParams(TEST_DIR + "\\" + FILE_NAME, 0, AccessMode.ReadWrite, FileAttribute.NTNormal, 0);
                testContext.firstFileHandle = driver.createFile(testSession, testConnection, createFileParams);
                assertNotNull(testContext.firstFileHandle);
                
                // now load up the node with lots of other stuff that we will test to see if it gets preserved during the
                // shuffle.
                testContext.testNodeRef = driver.getNodeForPath(testConnection, TEST_DIR + "\\" + FILE_NAME);
                // test non CM namespace property
                nodeService.setProperty(testContext.testNodeRef, TransferModel.PROP_ENABLED, true);
                // test CM property not related to an aspect
                nodeService.setProperty(testContext.testNodeRef, ContentModel.PROP_ADDRESSEE, "Fred");
                nodeService.getProperty(testContext.testNodeRef, ContentModel.PROP_CREATED);
                // classifiable chosen since its not related to any properties.
                nodeService.addAspect(testContext.testNodeRef, ContentModel.ASPECT_CLASSIFIABLE, null);
                //nodeService.createAssociation(testContext.testNodeRef, targetRef, assocTypeQName);
        
                return null;
            }
        };
        tran.doInTransaction(createFileCB, false, true);
        
        /**
         * Write some content to the test file
         */
        RetryingTransactionCallback<Void> writeFileCB = new RetryingTransactionCallback<Void>() {

            @Override
            public Void execute() throws Throwable
            {
                String testContent = "MS Word 2003 shuffle test";
                byte[] testContentBytes = testContent.getBytes();
                testContext.firstFileHandle.writeFile(testContentBytes, testContentBytes.length, 0, 0);
                testContext.firstFileHandle.close();            
                return null;
            }
        };
        tran.doInTransaction(writeFileCB, false, true);
        
        /**
         * b) Save the new file
         */
        RetryingTransactionCallback<Void> saveNewFileCB = new RetryingTransactionCallback<Void>() {

            @Override
            public Void execute() throws Throwable
            {
                FileOpenParams createFileParams = new FileOpenParams(TEST_DIR + "\\" + FILE_NEW_TEMP, 0, AccessMode.ReadWrite, FileAttribute.NTNormal, 0);
                testContext.newFileHandle = driver.createFile(testSession, testConnection, createFileParams);
                assertNotNull(testContext.newFileHandle);
                String testContent = "MS Word 2003 shuffle test This is new content";
                byte[] testContentBytes = testContent.getBytes();
                testContext.newFileHandle.writeFile(testContentBytes, testContentBytes.length, 0, 0);
                testContext.newFileHandle.close();
              
                return null;
            }
        };
        tran.doInTransaction(saveNewFileCB, false, true);
        
        /**
         * rename the old file
         */
        RetryingTransactionCallback<Void> renameOldFileCB = new RetryingTransactionCallback<Void>() {

            @Override
            public Void execute() throws Throwable
            {
                driver.renameFile(testSession, testConnection, TEST_DIR + "\\" + FILE_NAME, TEST_DIR + "\\" + FILE_OLD_TEMP);
                return null;
            }
        };
        tran.doInTransaction(renameOldFileCB, false, true);
        

        RetryingTransactionCallback<Void> validateOldFileGoneCB = new RetryingTransactionCallback<Void>() {

            @Override
            public Void execute() throws Throwable
            {   
                try
                {
                    driver.deleteFile(testSession, testConnection, TEST_DIR + "\\" + FILE_NAME);
                }
                catch (IOException e)
                { 
                    // expect to go here since previous step renamed the file.
                }
                
                return null;
            }
        };
        tran.doInTransaction(validateOldFileGoneCB, false, true);
        
        /**
         * Move the new file into place, stuff should get shuffled
         */
        RetryingTransactionCallback<Void> moveNewFileCB = new RetryingTransactionCallback<Void>() {

            @Override
            public Void execute() throws Throwable
            {
                driver.renameFile(testSession, testConnection, TEST_DIR + "\\" + FILE_NEW_TEMP, TEST_DIR + "\\" + FILE_NAME); 
                return null;
            }
        };
        
        tran.doInTransaction(moveNewFileCB, false, true);
        
        RetryingTransactionCallback<Void> validateCB = new RetryingTransactionCallback<Void>() {

            @Override
            public Void execute() throws Throwable
            {
               NodeRef shuffledNodeRef = driver.getNodeForPath(testConnection, TEST_DIR + "\\" + FILE_NAME);
               
               Map<QName, Serializable> props = nodeService.getProperties(shuffledNodeRef);
               assertTrue("node does not contain shuffled ENABLED property", props.containsKey(TransferModel.PROP_ENABLED));
               
               assertEquals("name wrong", FILE_NAME, nodeService.getProperty(shuffledNodeRef, ContentModel.PROP_NAME) );
               
               // commented out due to ALF-7628 
               //assertEquals("ADDRESSEE PROPERTY Not copied", "Fred", nodeService.getProperty(shuffledNodeRef, ContentModel.PROP_ADDRESSEE));
               //assertEquals("created date changed", testContext.testCreatedDate, (Date)nodeService.getProperty(shuffledNodeRef, ContentModel.PROP_CREATED));
               
               // assertTrue("CLASSIFIABLE aspect not present", nodeService.hasAspect(shuffledNodeRef, ContentModel.ASPECT_CLASSIFIABLE));
               
               //assertEquals("noderef changed", testContext.testNodeRef, shuffledNodeRef);
               return null;
            }
        };
        
        tran.doInTransaction(validateCB, false, true);

    } // testScenarioMSWord2003SaveShuffleWithBackup
    
    /**
     * This test tries to simulate the cifs shuffling that is done to
     * support MS Word 2007 
     * 
     * a) TEST.DOCX
     * b) Save new to 00000001.TMP
     * c) Rename TEST.DOCX to 00000002.TMP
     * d) Rename 000000001.TMP to TEST.DOCX
     * e) Delete 000000002.TMP
     */
    public void testScenarioMSWord2007Save() throws Exception
    {
        logger.debug("testScenarioMSWord2007SaveShuffle");
        final String FILE_NAME = "TEST.DOCX";
        final String FILE_OLD_TEMP = "00000001.TMP";
        final String FILE_NEW_TEMP = "00000002.TMP";
        
        class TestContext
        {
            NetworkFile firstFileHandle;
            NetworkFile newFileHandle;            
            NodeRef testNodeRef;   // node ref of test.doc
        };
        
        final TestContext testContext = new TestContext();
        
        final String TEST_ROOT_DIR = "\\ContentDiskDriverTest";
        final String TEST_DIR = "\\ContentDiskDriverTest\\testScenarioMSWord2007Save";
        ServerConfiguration scfg = new ServerConfiguration("testServer");
        TestServer testServer = new TestServer("testServer", scfg);
        final SrvSession testSession = new TestSrvSession(666, testServer, "test", "remoteName");
        DiskSharedDevice share = getDiskSharedDevice();
        final TreeConnection testConnection = testServer.getTreeConnection(share);
        final RetryingTransactionHelper tran = transactionService.getRetryingTransactionHelper();
        
        /**
         * Create a file in the test directory
         */           
        RetryingTransactionCallback<Void> createFileCB = new RetryingTransactionCallback<Void>() {

            @Override
            public Void execute() throws Throwable
            {
                /**
                 * Create the test directory we are going to use 
                 */
                FileOpenParams createRootDirParams = new FileOpenParams(TEST_ROOT_DIR, 0, AccessMode.ReadWrite, FileAttribute.NTNormal, 0);
                FileOpenParams createDirParams = new FileOpenParams(TEST_DIR, 0, AccessMode.ReadWrite, FileAttribute.NTNormal, 0);
                driver.createDirectory(testSession, testConnection, createRootDirParams);
                driver.createDirectory(testSession, testConnection, createDirParams);
                
                /**
                 * Create the file we are going to use
                 */
                FileOpenParams createFileParams = new FileOpenParams(TEST_DIR + "\\" + FILE_NAME, 0, AccessMode.ReadWrite, FileAttribute.NTNormal, 0);
                testContext.firstFileHandle = driver.createFile(testSession, testConnection, createFileParams);
                assertNotNull(testContext.firstFileHandle);
                
                // no need to test lots of different properties, that's already been tested above
                testContext.testNodeRef = driver.getNodeForPath(testConnection, TEST_DIR + "\\" + FILE_NAME);
                nodeService.setProperty(testContext.testNodeRef, TransferModel.PROP_ENABLED, true);
                        
                return null;
            }
        };
        tran.doInTransaction(createFileCB, false, true);
        
        /**
         * a) Write some content to the test file
         */
        RetryingTransactionCallback<Void> writeFileCB = new RetryingTransactionCallback<Void>() {

            @Override
            public Void execute() throws Throwable
            {
                String testContent = "MS Word 2007 shuffle test";
                byte[] testContentBytes = testContent.getBytes();
                testContext.firstFileHandle.writeFile(testContentBytes, testContentBytes.length, 0, 0);
                testContext.firstFileHandle.close();            
                return null;
            }
        };
        tran.doInTransaction(writeFileCB, false, true);
        
        /**
         * b) Save the new file
         */
        RetryingTransactionCallback<Void> saveNewFileCB = new RetryingTransactionCallback<Void>() {

            @Override
            public Void execute() throws Throwable
            {
                FileOpenParams createFileParams = new FileOpenParams(TEST_DIR + "\\" + FILE_NEW_TEMP, 0, AccessMode.ReadWrite, FileAttribute.NTNormal, 0);
                testContext.newFileHandle = driver.createFile(testSession, testConnection, createFileParams);
                assertNotNull(testContext.newFileHandle);
                String testContent = "MS Word 2007 shuffle test This is new content";
                byte[] testContentBytes = testContent.getBytes();
                testContext.newFileHandle.writeFile(testContentBytes, testContentBytes.length, 0, 0);
                testContext.newFileHandle.close();
              
                return null;
            }
        };
        tran.doInTransaction(saveNewFileCB, false, true);
        
        /**
         * c) rename the old file
         */
        RetryingTransactionCallback<Void> renameOldFileCB = new RetryingTransactionCallback<Void>() {

            @Override
            public Void execute() throws Throwable
            {
                driver.renameFile(testSession, testConnection, TEST_DIR + "\\" + FILE_NAME, TEST_DIR + "\\" + FILE_OLD_TEMP);
                return null;
            }
        };
        tran.doInTransaction(renameOldFileCB, false, true);
        
        
        /**
         * d) Move the new file into place, stuff should get shuffled
         */
        RetryingTransactionCallback<Void> moveNewFileCB = new RetryingTransactionCallback<Void>() {

            @Override
            public Void execute() throws Throwable
            {
                driver.renameFile(testSession, testConnection, TEST_DIR + "\\" + FILE_NEW_TEMP, TEST_DIR + "\\" + FILE_NAME); 
                return null;
            }
        };
        
        tran.doInTransaction(moveNewFileCB, false, true);
        
        RetryingTransactionCallback<Void> deleteOldFileCB = new RetryingTransactionCallback<Void>() {

            @Override
            public Void execute() throws Throwable
            {   
                driver.deleteFile(testSession, testConnection, TEST_DIR + "\\" + FILE_OLD_TEMP);
                return null;
            }
        };
        tran.doInTransaction(deleteOldFileCB, false, true);
        
        RetryingTransactionCallback<Void> validateCB = new RetryingTransactionCallback<Void>() {

            @Override
            public Void execute() throws Throwable
            {
               NodeRef shuffledNodeRef = driver.getNodeForPath(testConnection, TEST_DIR + "\\" + FILE_NAME);
               
               Map<QName, Serializable> props = nodeService.getProperties(shuffledNodeRef);
               assertTrue("node does not contain shuffled ENABLED property", props.containsKey(TransferModel.PROP_ENABLED));
               assertEquals("name wrong", FILE_NAME, nodeService.getProperty(shuffledNodeRef, ContentModel.PROP_NAME) );    
               return null;
            }
        };
        
        tran.doInTransaction(validateCB, false, true);

    } // testScenarioWord2007 save
    
    /**
     * This test tries to simulate the cifs shuffling that is done to
     * support EMACS 
     * 
     * a) emacsTest.txt
     * b) Rename original file to emacsTest.txt~
     * c) Create emacsTest.txt
     */
    public void DISABLED_testScenarioEmacsSave() throws Exception
    {
        logger.debug("testScenarioEmacsSave");
        final String FILE_NAME = "emacsTest.txt";
        final String FILE_OLD_TEMP = "emacsTest.txt~";
        
        class TestContext
        {
            NetworkFile firstFileHandle;
            NetworkFile newFileHandle;            
            NodeRef testNodeRef;   // node ref of test.doc
        };
        
        final TestContext testContext = new TestContext();
        
        final String TEST_ROOT_DIR = "\\ContentDiskDriverTest";
        final String TEST_DIR = "\\ContentDiskDriverTest\\testScenarioEmacsSave";
        
        ServerConfiguration scfg = new ServerConfiguration("testServer");
        TestServer testServer = new TestServer("testServer", scfg);
        final SrvSession testSession = new TestSrvSession(666, testServer, "test", "remoteName");
        DiskSharedDevice share = getDiskSharedDevice();
        final TreeConnection testConnection = testServer.getTreeConnection(share);
        final RetryingTransactionHelper tran = transactionService.getRetryingTransactionHelper();
                
        /**
         * Create a file in the test directory
         */    
        RetryingTransactionCallback<Void> createFileCB = new RetryingTransactionCallback<Void>() {

            @Override
            public Void execute() throws Throwable
            {
                /**
                 * Create the test directory we are going to use 
                 */
                FileOpenParams createRootDirParams = new FileOpenParams(TEST_ROOT_DIR, 0, AccessMode.ReadWrite, FileAttribute.NTNormal, 0);
                FileOpenParams createDirParams = new FileOpenParams(TEST_DIR, 0, AccessMode.ReadWrite, FileAttribute.NTNormal, 0);
                driver.createDirectory(testSession, testConnection, createRootDirParams);
                driver.createDirectory(testSession, testConnection, createDirParams);
                
                /**
                 * Create the file we are going to use
                 */
                FileOpenParams createFileParams = new FileOpenParams(TEST_DIR + "\\" + FILE_NAME, 0, AccessMode.ReadWrite, FileAttribute.NTNormal, 0);
                testContext.firstFileHandle = driver.createFile(testSession, testConnection, createFileParams);
                assertNotNull(testContext.firstFileHandle);
                
                // no need to test lots of different properties, that's already been tested above
                testContext.testNodeRef = driver.getNodeForPath(testConnection, TEST_DIR + "\\" + FILE_NAME);
                nodeService.setProperty(testContext.testNodeRef, TransferModel.PROP_ENABLED, true);
                        
                return null;
            }
        };
        tran.doInTransaction(createFileCB);
        
        /**
         * a) Write some content to the test file
         */
        RetryingTransactionCallback<Void> writeFileCB = new RetryingTransactionCallback<Void>() {

            @Override
            public Void execute() throws Throwable
            {
                String testContent = "Emacs shuffle test";
                byte[] testContentBytes = testContent.getBytes();
                testContext.firstFileHandle.writeFile(testContentBytes, testContentBytes.length, 0, 0);
                testContext.firstFileHandle.close();            
                return null;
            }
        };
        tran.doInTransaction(writeFileCB);
        
        /**
         * b) rename the old file out of the way
         */
        RetryingTransactionCallback<Void> renameOldFileCB = new RetryingTransactionCallback<Void>() {

            @Override
            public Void execute() throws Throwable
            {
                driver.renameFile(testSession, testConnection, TEST_DIR + "\\" + FILE_NAME, TEST_DIR + "\\" + FILE_OLD_TEMP);
                return null;
            }
        };
        tran.doInTransaction(renameOldFileCB);
        
        /**
         * c) Save the new file
         */
        RetryingTransactionCallback<Void> saveNewFileCB = new RetryingTransactionCallback<Void>() {

            @Override
            public Void execute() throws Throwable
            {
                FileOpenParams createFileParams = new FileOpenParams(TEST_DIR + "\\" + FILE_NAME, 0, AccessMode.ReadWrite, FileAttribute.NTNormal, 0);
                testContext.newFileHandle = driver.createFile(testSession, testConnection, createFileParams);
                assertNotNull(testContext.newFileHandle);
                String testContent = "EMACS shuffle test This is new content";
                byte[] testContentBytes = testContent.getBytes();
                testContext.newFileHandle.writeFile(testContentBytes, testContentBytes.length, 0, 0);
                testContext.newFileHandle.close();
              
                return null;
            }
        };
        tran.doInTransaction(saveNewFileCB);
        
        RetryingTransactionCallback<Void> validateCB = new RetryingTransactionCallback<Void>() {

            @Override
            public Void execute() throws Throwable
            {
               NodeRef shuffledNodeRef = driver.getNodeForPath(testConnection, TEST_DIR + "\\" + FILE_NAME);
               
               Map<QName, Serializable> props = nodeService.getProperties(shuffledNodeRef);
               assertTrue("node does not contain shuffled ENABLED property", props.containsKey(TransferModel.PROP_ENABLED));
               return null;
            }
        };
        
        tran.doInTransaction(validateCB);

    } // testScenarioEmacs save

    /**
     * This test tries to simulate the cifs shuffling that is done to
     * support vi 
     * 
     * a) viTest.txt
     * b) Rename original file to viTest.txt~
     * c) Create viTest.txt
     */
    public void DISABLED_testScenarioViSave() throws Exception
    {
        logger.debug("testScenarioViSave");
        final String FILE_NAME = "viTest.txt";
        final String FILE_OLD_TEMP = "viTest.txt~";
        
        class TestContext
        {
            NetworkFile firstFileHandle;
            NetworkFile newFileHandle;            
            NodeRef testNodeRef;   // node ref of test.doc
        };
        
        final TestContext testContext = new TestContext();
        
        final String TEST_ROOT_DIR = "\\ContentDiskDriverTest";
        final String TEST_DIR = "\\ContentDiskDriverTest\\testScenarioViSave";
        
        ServerConfiguration scfg = new ServerConfiguration("testServer");
        TestServer testServer = new TestServer("testServer", scfg);
        final SrvSession testSession = new TestSrvSession(666, testServer, "test", "remoteName");
        DiskSharedDevice share = getDiskSharedDevice();
        final TreeConnection testConnection = testServer.getTreeConnection(share);
        final RetryingTransactionHelper tran = transactionService.getRetryingTransactionHelper();
            
        /**
         * Create a file in the test directory
         */            
        RetryingTransactionCallback<Void> createFileCB = new RetryingTransactionCallback<Void>() {

            @Override
            public Void execute() throws Throwable
            {
                /**
                 * Create the test directory we are going to use 
                 */
                FileOpenParams createRootDirParams = new FileOpenParams(TEST_ROOT_DIR, 0, AccessMode.ReadWrite, FileAttribute.NTNormal, 0);
                FileOpenParams createDirParams = new FileOpenParams(TEST_DIR, 0, AccessMode.ReadWrite, FileAttribute.NTNormal, 0);
                driver.createDirectory(testSession, testConnection, createRootDirParams);
                driver.createDirectory(testSession, testConnection, createDirParams);
                
                /**
                 * Create the file we are going to use
                 */
                FileOpenParams createFileParams = new FileOpenParams(TEST_DIR + "\\" + FILE_NAME, 0, AccessMode.ReadWrite, FileAttribute.NTNormal, 0);
                testContext.firstFileHandle = driver.createFile(testSession, testConnection, createFileParams);
                assertNotNull(testContext.firstFileHandle);
                
                // no need to test lots of different properties, that's already been tested above
                testContext.testNodeRef = driver.getNodeForPath(testConnection, TEST_DIR + "\\" + FILE_NAME);
                nodeService.setProperty(testContext.testNodeRef, TransferModel.PROP_ENABLED, true);
                        
                return null;
            }
        };
        tran.doInTransaction(createFileCB);
        
        /**
         * a) Write some content to the test file
         */
        RetryingTransactionCallback<Void> writeFileCB = new RetryingTransactionCallback<Void>() {

            @Override
            public Void execute() throws Throwable
            {
                String testContent = "Emacs shuffle test";
                byte[] testContentBytes = testContent.getBytes();
                testContext.firstFileHandle.writeFile(testContentBytes, testContentBytes.length, 0, 0);
                testContext.firstFileHandle.close();            
                return null;
            }
        };
        tran.doInTransaction(writeFileCB);
        
        /**
         * b) rename the old file out of the way
         */
        RetryingTransactionCallback<Void> renameOldFileCB = new RetryingTransactionCallback<Void>() {

            @Override
            public Void execute() throws Throwable
            {
                driver.renameFile(testSession, testConnection, TEST_DIR + "\\" + FILE_NAME, TEST_DIR + "\\" + FILE_OLD_TEMP);
                return null;
            }
        };
        tran.doInTransaction(renameOldFileCB);
        
        /**
         * c) Save the new file
         */
        RetryingTransactionCallback<Void> saveNewFileCB = new RetryingTransactionCallback<Void>() {

            @Override
            public Void execute() throws Throwable
            {
                FileOpenParams createFileParams = new FileOpenParams(TEST_DIR + "\\" + FILE_NAME, 0, AccessMode.ReadWrite, FileAttribute.NTNormal, 0);
                testContext.newFileHandle = driver.createFile(testSession, testConnection, createFileParams);
                assertNotNull(testContext.newFileHandle);
                String testContent = "EMACS shuffle test This is new content";
                byte[] testContentBytes = testContent.getBytes();
                testContext.newFileHandle.writeFile(testContentBytes, testContentBytes.length, 0, 0);
                testContext.newFileHandle.close();
              
                return null;
            }
        };
        tran.doInTransaction(saveNewFileCB);
        
        RetryingTransactionCallback<Void> validateCB = new RetryingTransactionCallback<Void>() {

            @Override
            public Void execute() throws Throwable
            {
               NodeRef shuffledNodeRef = driver.getNodeForPath(testConnection, TEST_DIR + "\\" + FILE_NAME);
               assertNotNull("shuffledNodeRef is null", shuffledNodeRef);
               
               Map<QName, Serializable> props = nodeService.getProperties(shuffledNodeRef);
               assertEquals("name wrong", FILE_NAME, nodeService.getProperty(shuffledNodeRef, ContentModel.PROP_NAME) );   
               assertTrue("node does not contain shuffled ENABLED property", props.containsKey(TransferModel.PROP_ENABLED));
 
               return null;
            }
        };
        
        tran.doInTransaction(validateCB);

    } // testScenarioViSave
    
    /**
     * This test tries to simulate the cifs shuffling that is done to
     * support smultron 
     * 
     * a) smultronTest.txt
     * b) Save new file to .dat04cd.004
     * c) Delete smultronTest.txt
     * c) Rename .dat04cd.004 to smultronTest.txt
     */
    public void DISABLED_testScenarioSmultronSave() throws Exception
    {
        logger.debug("testScenarioSmultronSave");
        final String FILE_NAME = "smultronTest.txt";
        final String FILE_NEW_TEMP = ".dat04cd.004";
        
        class TestContext
        {
            NetworkFile firstFileHandle;
            NetworkFile newFileHandle;            
            NodeRef testNodeRef;   // node ref of test.doc
        };
        
        final TestContext testContext = new TestContext();
        
        final String TEST_ROOT_DIR = "\\ContentDiskDriverTest";
        final String TEST_DIR = "\\ContentDiskDriverTest\\testScenarioSmultronSave";
        
        ServerConfiguration scfg = new ServerConfiguration("testServer");
        TestServer testServer = new TestServer("testServer", scfg);
        final SrvSession testSession = new TestSrvSession(666, testServer, "test", "remoteName");
        DiskSharedDevice share = getDiskSharedDevice();
        final TreeConnection testConnection = testServer.getTreeConnection(share);
        final RetryingTransactionHelper tran = transactionService.getRetryingTransactionHelper();
          
        /**
         * Create a file in the test directory
         */           
        RetryingTransactionCallback<Void> createFileCB = new RetryingTransactionCallback<Void>() {

            @Override
            public Void execute() throws Throwable
            {
                /**
                 * Create the test directory we are going to use 
                 */
                FileOpenParams createRootDirParams = new FileOpenParams(TEST_ROOT_DIR, 0, AccessMode.ReadWrite, FileAttribute.NTNormal, 0);
                FileOpenParams createDirParams = new FileOpenParams(TEST_DIR, 0, AccessMode.ReadWrite, FileAttribute.NTNormal, 0);
                driver.createDirectory(testSession, testConnection, createRootDirParams);
                driver.createDirectory(testSession, testConnection, createDirParams);
                
                /**
                 * Create the file we are going to use
                 */
                FileOpenParams createFileParams = new FileOpenParams(TEST_DIR + "\\" + FILE_NAME, 0, AccessMode.ReadWrite, FileAttribute.NTNormal, 0);
                testContext.firstFileHandle = driver.createFile(testSession, testConnection, createFileParams);
                assertNotNull(testContext.firstFileHandle);
                
                // no need to test lots of different properties, that's already been tested above
                testContext.testNodeRef = driver.getNodeForPath(testConnection, TEST_DIR + "\\" + FILE_NAME);
                nodeService.setProperty(testContext.testNodeRef, TransferModel.PROP_ENABLED, true);
                        
                return null;
            }
        };
        tran.doInTransaction(createFileCB);
        
        /**
         * a) Write some content to the test file
         */
        RetryingTransactionCallback<Void> writeFileCB = new RetryingTransactionCallback<Void>() {

            @Override
            public Void execute() throws Throwable
            {
                String testContent = "Smultron shuffle test";
                byte[] testContentBytes = testContent.getBytes();
                testContext.firstFileHandle.writeFile(testContentBytes, testContentBytes.length, 0, 0);
                testContext.firstFileHandle.close();            
                return null;
            }
        };
        tran.doInTransaction(writeFileCB);
        
        /**
         * b) Save the new file
         */
        RetryingTransactionCallback<Void> saveNewFileCB = new RetryingTransactionCallback<Void>() {

            @Override
            public Void execute() throws Throwable
            {
                FileOpenParams createFileParams = new FileOpenParams(TEST_DIR + "\\" + FILE_NEW_TEMP, 0, AccessMode.ReadWrite, FileAttribute.NTNormal, 0);
                testContext.newFileHandle = driver.createFile(testSession, testConnection, createFileParams);
                assertNotNull(testContext.newFileHandle);
                String testContent = "Smultron shuffle test This is new content";
                byte[] testContentBytes = testContent.getBytes();
                testContext.newFileHandle.writeFile(testContentBytes, testContentBytes.length, 0, 0);
                testContext.newFileHandle.close();
              
                return null;
            }
        };
        tran.doInTransaction(saveNewFileCB);
        
        /**
         * c) Delete the old file
         */
        RetryingTransactionCallback<Void> deleteOldFileCB = new RetryingTransactionCallback<Void>() {

            @Override
            public Void execute() throws Throwable
            {   
                driver.deleteFile(testSession, testConnection, TEST_DIR + "\\" + FILE_NAME);
                return null;
            }
        };
        tran.doInTransaction(deleteOldFileCB);
          
        /**
         * d) Move the new file into place, stuff should get shuffled
         */
        RetryingTransactionCallback<Void> moveNewFileCB = new RetryingTransactionCallback<Void>() {

            @Override
            public Void execute() throws Throwable
            {
                driver.renameFile(testSession, testConnection, TEST_DIR + "\\" + FILE_NEW_TEMP, TEST_DIR + "\\" + FILE_NAME); 
                return null;
            }
        };
        
        tran.doInTransaction(moveNewFileCB);
        
        RetryingTransactionCallback<Void> validateCB = new RetryingTransactionCallback<Void>() {

            @Override
            public Void execute() throws Throwable
            {
               NodeRef shuffledNodeRef = driver.getNodeForPath(testConnection, TEST_DIR + "\\" + FILE_NAME);
               
               Map<QName, Serializable> props = nodeService.getProperties(shuffledNodeRef);
               assertTrue("node does not contain shuffled ENABLED property", props.containsKey(TransferModel.PROP_ENABLED));
               assertEquals("name wrong", FILE_NAME, nodeService.getProperty(shuffledNodeRef, ContentModel.PROP_NAME) );    
               return null;
            }
        };
        
        tran.doInTransaction(validateCB);
        
    } // testScenarioSmultronSave

    
    /**
     * This time we create a file through the ContentDiskDriver and then delete it 
     * through the repo.   We check its no longer found by the driver.
     */
    public void testScenarioDeleteViaNodeService() throws Exception
    {
        logger.debug("testScenarioDeleteViaNodeService");
        
        ServerConfiguration scfg = new ServerConfiguration("testServer");
        TestServer testServer = new TestServer("testServer", scfg);
        SrvSession testSession = new TestSrvSession(666, testServer, "test", "remoteName");
        DiskSharedDevice share = getDiskSharedDevice();
        TreeConnection testConnection = testServer.getTreeConnection(share);
        final RetryingTransactionHelper tran = transactionService.getRetryingTransactionHelper();
              
        int openAction = FileAction.CreateNotExist;
        String FILE_PATH="\\testCreateFile.new";
          
        FileOpenParams params = new FileOpenParams(FILE_PATH, openAction, AccessMode.ReadWrite, FileAttribute.NTNormal, 0);
                
        final NetworkFile file = driver.createFile(testSession, testConnection, params);
        
        assertNotNull("file is null", file);
        assertFalse("file is read only, should be read-write", file.isReadOnly());
              
        RetryingTransactionCallback<Void> writeFileCB = new RetryingTransactionCallback<Void>() {

            @Override
            public Void execute() throws Throwable
            {

                byte[] stuff = "Hello World".getBytes();
                file.writeFile(stuff, stuff.length, 0, 0);
                file.close();
                
                NodeRef companyHome = repositoryHelper.getCompanyHome();
                NodeRef newNode = nodeService.getChildByName(companyHome, ContentModel.ASSOC_CONTAINS, "testCreateFile.new");
                assertNotNull("can't find new node", newNode);
             
                     
                return null;
            }
        };
        tran.doInTransaction(writeFileCB, false, true);
        
        /**
         * Step 1: Delete the new node via the node service
         */
        RetryingTransactionCallback<Void> deleteNodeCB = new RetryingTransactionCallback<Void>() {

            @Override
            public Void execute() throws Throwable
            {

                
                NodeRef companyHome = repositoryHelper.getCompanyHome();
                NodeRef newNode = nodeService.getChildByName(companyHome, ContentModel.ASSOC_CONTAINS, "testCreateFile.new");
                assertNotNull("can't find new node", newNode);
                nodeService.deleteNode(newNode);
                return null;
            }
        };
        tran.doInTransaction(deleteNodeCB, false, true);
        
        try
        {
            driver.getNodeForPath(testConnection, FILE_PATH);
            fail("getNode for path unexpectedly succeeded");
        } 
        catch (IOException ie)
        {
            // expect to go here
        }
        
        /**
         * Delete file by path - file should no longer exist
         */
        try
        {
            driver.deleteFile(testSession, testConnection, FILE_PATH);
            fail("delete unexpectedly succeeded");
        } 
        catch (IOException ie)
        {
            // expect to go here
        }
        
    }
    
    /**
     * This test tries to simulate the shuffling that is done by MS Word 2003 
     * with regard to metadata extraction.
     * <p>
     * 1: Setup an inbound rule for ContentMetadataExtractor.
     * 2: Write ContentDiskDriverTest1 file to ContentDiskDriver.docx
     * 3: Check metadata extraction for non update test
     * Simulate a WORD 2003 CIFS shuffle
     * 4: Write ContentDiskDriverTest2 file to ~WRD0003.TMP
     * 5: Rename ContentDiskDriver.docx to ~WRL0003.TMP
     * 6: Rename ~WRD0003.TMP to ContentDiskDriver.docx
     * 7: Check metadata extraction
     */
    public void testMetadataExtraction() throws Exception
    {
        logger.debug("testMetadataExtraction");
        final String FILE_NAME = "ContentDiskDriver.docx";
        final String FILE_OLD_TEMP = "~WRL0003.TMP";
        final String FILE_NEW_TEMP = "~WRD0003.TMP";
         
        class TestContext
        {
            NodeRef testDirNodeRef;
            NodeRef testNodeRef;
            NetworkFile firstFileHandle;
            NetworkFile secondFileHandle;
        };
        
        final TestContext testContext = new TestContext();
        
        final String TEST_DIR = TEST_ROOT_DOS_PATH + "\\testMetadataExtraction";
        
        ServerConfiguration scfg = new ServerConfiguration("testServer");
        TestServer testServer = new TestServer("testServer", scfg);
        final SrvSession testSession = new TestSrvSession(666, testServer, "test", "remoteName");
        DiskSharedDevice share = getDiskSharedDevice();
        final TreeConnection testConnection = testServer.getTreeConnection(share);
        final RetryingTransactionHelper tran = transactionService.getRetryingTransactionHelper();
        
        /**
         * Clean up just in case garbage is left from a previous run
         */
        RetryingTransactionCallback<Void> deleteGarbageDirCB = new RetryingTransactionCallback<Void>() {

            @Override
            public Void execute() throws Throwable
            {
                driver.deleteDirectory(testSession, testConnection, TEST_DIR);
                return null;
            }
        };
        
        try
        {
            tran.doInTransaction(deleteGarbageDirCB);
        }
        catch (Exception e)
        {
            // expect to go here
        }
        
        logger.debug("create Test directory" + TEST_DIR);
        RetryingTransactionCallback<Void> createTestDirCB = new RetryingTransactionCallback<Void>() {

            @Override
            public Void execute() throws Throwable
            {
                /**
                 * Create the test directory we are going to use 
                 */
                FileOpenParams createRootDirParams = new FileOpenParams(TEST_ROOT_DOS_PATH, 0, AccessMode.ReadWrite, FileAttribute.NTNormal, 0);
                FileOpenParams createDirParams = new FileOpenParams(TEST_DIR, 0, AccessMode.ReadWrite, FileAttribute.NTNormal, 0);
                driver.createDirectory(testSession, testConnection, createRootDirParams);
                driver.createDirectory(testSession, testConnection, createDirParams);
                
                testContext.testDirNodeRef = driver.getNodeForPath(testConnection, TEST_DIR);
                assertNotNull("testDirNodeRef is null", testContext.testDirNodeRef);   
                
                UserTransaction txn = transactionService.getUserTransaction();
              
                return null;
                
                
            }
        };                
        tran.doInTransaction(createTestDirCB);
        logger.debug("Create rule on test dir");
        
        RetryingTransactionCallback<Void> createRuleCB = new RetryingTransactionCallback<Void>() {

            @Override
            public Void execute() throws Throwable
            {                 
                Rule rule = new Rule();
                rule.setRuleType(RuleType.INBOUND);
                rule.applyToChildren(true);
                rule.setRuleDisabled(false);
                rule.setTitle("Extract Metadata from content");
                rule.setDescription("ContentDiskDriverTest");
                
                Map<String, Serializable> props = new HashMap<String, Serializable>(1);
                Action extractAction = actionService.createAction("extract-metadata", props);
                
                ActionCondition noCondition1 = actionService.createActionCondition(NoConditionEvaluator.NAME);
                extractAction.addActionCondition(noCondition1);
                
                ActionCondition noCondition2 = actionService.createActionCondition(NoConditionEvaluator.NAME);
                CompositeAction compAction = actionService.createCompositeAction();
                compAction.setTitle("Extract Metadata");
                compAction.setDescription("Content Disk Driver Test - Extract Metadata");
                compAction.addAction(extractAction);
                compAction.addActionCondition(noCondition2);

                rule.setAction(compAction);           
                         
                ruleService.saveRule(testContext.testDirNodeRef, rule);
                
                logger.debug("rule created");
                     
                return null;
            }
        };
        tran.doInTransaction(createRuleCB, false, true);

        /**
         * Create a file in the test directory
         */  
        logger.debug("create test file in test directory");
        RetryingTransactionCallback<Void> createFileCB = new RetryingTransactionCallback<Void>() {

            @Override
            public Void execute() throws Throwable
            {               
                /**
                 * Create the file we are going to use to test
                 */
                FileOpenParams createFileParams = new FileOpenParams(TEST_DIR + "\\" + FILE_NAME, 0, AccessMode.ReadWrite, FileAttribute.NTNormal, 0);
                testContext.firstFileHandle = driver.createFile(testSession, testConnection, createFileParams);
                assertNotNull(testContext.firstFileHandle);
                
                // now load up the node with lots of other stuff that we will test to see if it gets preserved during the
                // shuffle.
                testContext.testNodeRef = driver.getNodeForPath(testConnection, TEST_DIR + "\\" + FILE_NAME);
                assertNotNull("testContext.testNodeRef is null", testContext.testNodeRef);
                
                // test non CM namespace property
                nodeService.setProperty(testContext.testNodeRef, TransferModel.PROP_ENABLED, true);
        
                return null;
            }
        };
        tran.doInTransaction(createFileCB, false, true);
        
        logger.debug("step b: write content to test file");
        
        /**
         * Write ContentDiskDriverTest1.docx to the test file,
         */
        RetryingTransactionCallback<Void> writeFileCB = new RetryingTransactionCallback<Void>() {

            @Override
            public Void execute() throws Throwable
            {
                ClassPathResource fileResource = new ClassPathResource("filesys/ContentDiskDriverTest1.docx");
                assertNotNull("unable to find test resource filesys/ContentDiskDriverTest1.docx", fileResource);

                byte[] buffer= new byte[1000];
                InputStream is = fileResource.getInputStream();
                try
                {
                    long offset = 0;
                    int i = is.read(buffer, 0, buffer.length);
                    while(i > 0)
                    {
                        testContext.firstFileHandle.writeFile(buffer, i, 0, offset);
                        offset += i;
                        i = is.read(buffer, 0, buffer.length);
                    }                 
                }
                finally
                {
                    is.close();
                }
            
                testContext.firstFileHandle.close();   
                    
                return null;
            }
        };
        tran.doInTransaction(writeFileCB, false, true);
        
        logger.debug("Step c: validate metadata has been extracted.");
        
        /**
         * c: check simple case of meta-data extraction has worked.
         */
        RetryingTransactionCallback<Void> validateFirstExtractionCB = new RetryingTransactionCallback<Void>() {

            @Override
            public Void execute() throws Throwable
            {
                Map<QName, Serializable> props = nodeService.getProperties(testContext.testNodeRef);
                
                assertTrue("Enabled property has been lost", props.containsKey(TransferModel.PROP_ENABLED));
             
                // These metadata values should be extracted.
                assertEquals("description is not correct", "This is a test file", nodeService.getProperty(testContext.testNodeRef, ContentModel.PROP_DESCRIPTION));
                assertEquals("title is not correct", "ContentDiskDriverTest", nodeService.getProperty(testContext.testNodeRef, ContentModel.PROP_TITLE));
                assertEquals("author is not correct", "mrogers", nodeService.getProperty(testContext.testNodeRef, ContentModel.PROP_AUTHOR));
                
                ContentData data = (ContentData)props.get(ContentModel.PROP_CONTENT);
                assertEquals("mimeType is wrong", "application/vnd.openxmlformats-officedocument.wordprocessingml.document", data.getMimetype());
                assertEquals("size is wrong", 11302, data.getSize());
                        
                return null;
            }
        };
        tran.doInTransaction(validateFirstExtractionCB, false, true);
        
        
        /**
         * d: Save the new file as an update file in the test directory
         */
        logger.debug("Step d: create update file in test directory " + FILE_NEW_TEMP);
        RetryingTransactionCallback<Void> createUpdateFileCB = new RetryingTransactionCallback<Void>() {

            @Override
            public Void execute() throws Throwable
            {               
                /**
                 * Create the file we are going to use to test
                 */
                FileOpenParams createFileParams = new FileOpenParams(TEST_DIR + "\\" + FILE_NEW_TEMP, 0, AccessMode.ReadWrite, FileAttribute.NTNormal, 0);
                testContext.secondFileHandle = driver.createFile(testSession, testConnection, createFileParams);
                assertNotNull(testContext.secondFileHandle);
                  
                return null;
            }
        };
        tran.doInTransaction(createUpdateFileCB, false, true);

        RetryingTransactionCallback<Void> writeFile2CB = new RetryingTransactionCallback<Void>() {

            @Override
            public Void execute() throws Throwable
            {
                ClassPathResource fileResource = new ClassPathResource("filesys/ContentDiskDriverTest2.docx");
                assertNotNull("unable to find test resource filesys/ContentDiskDriverTest2.docx", fileResource);

                byte[] buffer= new byte[1000];
                InputStream is = fileResource.getInputStream();
                try
                {
                    long offset = 0;
                    int i = is.read(buffer, 0, buffer.length);
                    while(i > 0)
                    {
                        testContext.secondFileHandle.writeFile(buffer, i, 0, offset);
                        offset += i;
                        i = is.read(buffer, 0, buffer.length);
                    }                 
                }
                finally
                {
                    is.close();
                }
            
                testContext.secondFileHandle.close();   
                    
                return null;
            }
        };
        tran.doInTransaction(writeFile2CB, false, true);
        
        /**
         * rename the old file
         */
        logger.debug("move old file out of the way.");
        RetryingTransactionCallback<Void> renameOldFileCB = new RetryingTransactionCallback<Void>() {

            @Override
            public Void execute() throws Throwable
            {
                driver.renameFile(testSession, testConnection, TEST_DIR + "\\" + FILE_NAME, TEST_DIR + "\\" + FILE_OLD_TEMP);
                return null;
            }
        };
        tran.doInTransaction(renameOldFileCB, false, true);
  
        /**
         * Check the old file has gone.
         */
        RetryingTransactionCallback<Void> validateOldFileGoneCB = new RetryingTransactionCallback<Void>() {

            @Override
            public Void execute() throws Throwable
            {   
                try
                {
                    driver.deleteFile(testSession, testConnection, TEST_DIR + "\\" + FILE_NAME);
                }
                catch (IOException e)
                { 
                    // expect to go here since previous step renamed the file.
                }
                
                return null;
            }
        };
        tran.doInTransaction(validateOldFileGoneCB, false, true);
        
//        /**
//         * Check metadata extraction on intermediate new file
//         */
//        RetryingTransactionCallback<Void> validateIntermediateCB = new RetryingTransactionCallback<Void>() {
//
//            @Override
//            public Void execute() throws Throwable
//            {
//               NodeRef updateNodeRef = driver.getNodeForPath(testConnection, TEST_DIR + "\\" + FILE_NEW_TEMP);
//               
//               Map<QName, Serializable> props = nodeService.getProperties(updateNodeRef);
//                        
//               // These metadata values should be extracted from file2.
//               assertEquals("intermediate file description is not correct", "Content Disk Test 2", props.get(ContentModel.PROP_DESCRIPTION));
//               assertEquals("intermediate file title is not correct", "Updated", props.get(ContentModel.PROP_TITLE));
//               assertEquals("intermediate file author is not correct", "mrogers", props.get(ContentModel.PROP_AUTHOR));
//
//               return null;
//            }
//        };
//        
//        tran.doInTransaction(validateIntermediateCB, true, true);
        
        /**
         * Move the new file into place, stuff should get shuffled
         */
        logger.debug("move new file into place.");
        RetryingTransactionCallback<Void> moveNewFileCB = new RetryingTransactionCallback<Void>() {

            @Override
            public Void execute() throws Throwable
            {
                driver.renameFile(testSession, testConnection, TEST_DIR + "\\" + FILE_NEW_TEMP, TEST_DIR + "\\" + FILE_NAME); 
                return null;
            }
        };
        
        tran.doInTransaction(moveNewFileCB, false, true);
        
        logger.debug("validate update has run correctly.");
        RetryingTransactionCallback<Void> validateUpdateCB = new RetryingTransactionCallback<Void>() {

            @Override
            public Void execute() throws Throwable
            {
               NodeRef shuffledNodeRef = driver.getNodeForPath(testConnection, TEST_DIR + "\\" + FILE_NAME);
               
               Map<QName, Serializable> props = nodeService.getProperties(shuffledNodeRef);
               
               // Check trx:enabled has been shuffled and not lost.
               assertTrue("node does not contain shuffled ENABLED property", props.containsKey(TransferModel.PROP_ENABLED));
               
               ContentData data = (ContentData)props.get(ContentModel.PROP_CONTENT);
               assertEquals("mimeType is wrong", "application/vnd.openxmlformats-officedocument.wordprocessingml.document", data.getMimetype());
               assertEquals("size is wrong", 11265, data.getSize());
           
               // These metadata values should be extracted from file2.   However they will not be applied in PRAGMATIC mode.
//               assertEquals("description is not correct", "Content Disk Test 2", props.get(ContentModel.PROP_DESCRIPTION));
//               assertEquals("title is not correct", "Updated", props.get(ContentModel.PROP_TITLE));
//               assertEquals("author is not correct", "mrogers", props.get(ContentModel.PROP_AUTHOR));
               
                    return null;
            }
        };
        
        tran.doInTransaction(validateUpdateCB, true, true);
        
    } // testScenarioShuffleMetadataExtraction
    
    public void testDirListing()throws Exception
    {
        logger.debug("testDirListing");
        ServerConfiguration scfg = new ServerConfiguration("testServer");
        TestServer testServer = new TestServer("testServer", scfg);
        SrvSession testSession = new TestSrvSession(666, testServer, "test", "remoteName");
        DiskSharedDevice share = getDiskSharedDevice();
        TreeConnection testConnection = testServer.getTreeConnection(share);
        final RetryingTransactionHelper tran = transactionService.getRetryingTransactionHelper();
        
        final String FOLDER_NAME = "parentFolder" + System.currentTimeMillis();
        final String HIDDEN_FOLDER_NAME = "hiddenFolder" + System.currentTimeMillis();
        RetryingTransactionCallback<NodeRef> createNodesCB = new RetryingTransactionCallback<NodeRef>() {

            @Override
            public NodeRef execute() throws Throwable
            {
                NodeRef companyHome = repositoryHelper.getCompanyHome();
                NodeRef parentNode = nodeService.createNode(companyHome, ContentModel.ASSOC_CONTAINS, QName.createQName(NamespaceService.CONTENT_MODEL_1_0_URI, FOLDER_NAME), ContentModel.TYPE_FOLDER).getChildRef();
                nodeService.setProperty(parentNode, ContentModel.PROP_NAME, FOLDER_NAME);
                
                NodeRef hiddenNode = nodeService.createNode(parentNode, ContentModel.ASSOC_CONTAINS, QName.createQName(NamespaceService.CONTENT_MODEL_1_0_URI, HIDDEN_FOLDER_NAME), ForumModel.TYPE_FORUM).getChildRef();
                nodeService.setProperty(hiddenNode, ContentModel.PROP_NAME, HIDDEN_FOLDER_NAME);
                return parentNode;
            }
        };
        final NodeRef parentFolder = tran.doInTransaction(createNodesCB);
        
        List<String> excludedTypes = new ArrayList<String>();
        excludedTypes.add(ForumModel.TYPE_FORUM.toString());
        cifsHelper.setExcludedTypes(excludedTypes);
        SearchContext result = driver.startSearch(testSession, testConnection, "\\"+FOLDER_NAME + "\\*", 0);
        while(result.hasMoreFiles())
        {
            if (result.nextFileName().equals(HIDDEN_FOLDER_NAME))
            {
                fail("Exluded types mustn't be shown in cifs");    
            } 
        }

        RetryingTransactionCallback<Void> deleteNodeCB = new RetryingTransactionCallback<Void>() {

            @Override
            public Void execute() throws Throwable
            {
                nodeService.deleteNode(parentFolder);
                return null;
            }
        };
        tran.doInTransaction(deleteNodeCB, false, true);
    } //testDirListing
    
    /**
     * Test server
     */
    public class TestServer extends NetworkFileServer
    {
        
        public TestServer(String proto, ServerConfiguration config)
        {
            super(proto, config);
            // TODO Auto-generated constructor stub
        }

        @Override
        public void startServer()
        {
                       
        }

        @Override
        public void shutdownServer(boolean immediate)
        {
            
        }
        
        public TreeConnection getTreeConnection(SharedDevice share) 
        {
            return new TreeConnection(share);
        }
    }
    
    /**
     * TestSrvSession
     */
    private class TestSrvSession extends SrvSession
    {

        public TestSrvSession(int sessId, NetworkServer srv, String proto,
                String remName)
        {
            super(sessId, srv, proto, remName);
        }

        @Override
        public InetAddress getRemoteAddress()
        {
            return null;
        }

        @Override
        public boolean useCaseSensitiveSearch()
        {
            return false;
        }
    }
}