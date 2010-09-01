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
package org.alfresco.repo.web.scripts.audit;

import java.util.Map;

import org.alfresco.repo.content.MimetypeMap;
import org.alfresco.repo.security.authentication.AuthenticationException;
import org.alfresco.repo.security.authentication.AuthenticationUtil;
import org.alfresco.repo.security.authentication.AuthenticationUtil.RunAsWork;
import org.alfresco.repo.web.scripts.BaseWebScriptTest;
import org.alfresco.service.cmr.audit.AuditService;
import org.alfresco.service.cmr.audit.AuditService.AuditApplication;
import org.alfresco.service.cmr.security.AuthenticationService;
import org.json.JSONArray;
import org.json.JSONObject;
import org.springframework.context.ApplicationContext;
import org.springframework.extensions.webscripts.Status;
import org.springframework.extensions.webscripts.TestWebScriptServer;
import org.springframework.extensions.webscripts.TestWebScriptServer.Response;

/**
 * Test the audit web scripts
 * 
 * @author Derek Hulley
 * @since 3.4
 */
public class AuditWebScriptTest extends BaseWebScriptTest
{
    private ApplicationContext ctx;
    private AuditService auditService;
    private AuthenticationService authenticationService;
    private String admin;
    private boolean wasGloballyEnabled;
    boolean wasRepoEnabled;

    @Override
    protected void setUp() throws Exception
    {
        super.setUp();
        ctx = getServer().getApplicationContext();
        authenticationService = (AuthenticationService) ctx.getBean("AuthenticationService");
        auditService = (AuditService) ctx.getBean("AuditService");
        admin = AuthenticationUtil.getAdminUserName();
        
        AuthenticationUtil.setFullyAuthenticatedUser(admin);
        
        wasGloballyEnabled = auditService.isAuditEnabled();
        wasRepoEnabled = auditService.isAuditEnabled(APP_REPO_NAME, APP_REPO_PATH);
        // Only enable if required
        if (!wasGloballyEnabled)
        {
            auditService.setAuditEnabled(true);
        }
        if (!wasRepoEnabled)
        {
            auditService.enableAudit(APP_REPO_NAME, APP_REPO_PATH);
        }
    }

    @Override
    protected void tearDown() throws Exception
    {
        super.tearDown();
        // Leave audit in correct state
        try
        {
            if (!wasGloballyEnabled)
            {
                auditService.setAuditEnabled(false);
            }
        }
        catch (Throwable e)
        {
            throw new RuntimeException("Failed to set audit back to globally enabled/disabled state", e);
        }
        try
        {
            if (wasRepoEnabled)
            {
                auditService.enableAudit(APP_REPO_NAME, APP_REPO_PATH);
            }
            else
            {
                auditService.disableAudit(APP_REPO_NAME, APP_REPO_PATH);
            }
        }
        catch (Throwable e)
        {
            throw new RuntimeException("Failed to set repo audit back to enabled/disabled state", e);
        }
    }
    
    public void testGetWithoutPermissions() throws Exception
    {
        String url = "/api/audit/control";
        TestWebScriptServer.GetRequest req = new TestWebScriptServer.GetRequest(url);
        sendRequest(req, 401, AuthenticationUtil.getGuestRoleName());
    }
    
    public void testGetIsAuditEnabledGlobally() throws Exception
    {
        boolean wasEnabled = auditService.isAuditEnabled();
        Map<String, AuditApplication> checkApps = auditService.getAuditApplications();

        String url = "/api/audit/control";
        TestWebScriptServer.GetRequest req = new TestWebScriptServer.GetRequest(url);
        
        Response response = sendRequest(req, Status.STATUS_OK, admin);
        JSONObject json = new JSONObject(response.getContentAsString());
        boolean enabled = json.getBoolean(AbstractAuditWebScript.JSON_KEY_ENABLED);
        assertEquals("Mismatched global audit enabled", wasEnabled, enabled);
        JSONArray apps = json.getJSONArray(AbstractAuditWebScript.JSON_KEY_APPLICATIONS);
        assertEquals("Incorrect number of applications reported", checkApps.size(), apps.length());
    }
    
    public void testGetIsAuditEnabledMissingApp() throws Exception
    {
        String url = "/api/audit/control/xxx";
        TestWebScriptServer.GetRequest req = new TestWebScriptServer.GetRequest(url);
        
        sendRequest(req, 404, admin);
    }
    
    public void testSetAuditEnabledGlobally() throws Exception
    {
        boolean wasEnabled = auditService.isAuditEnabled();

        if (wasEnabled)
        {
            String url = "/api/audit/control?enable=false";
            TestWebScriptServer.PostRequest req = new TestWebScriptServer.PostRequest(url, "", MimetypeMap.MIMETYPE_JSON);
            sendRequest(req, Status.STATUS_OK, admin);
        }
        else
        {
            String url = "/api/audit/control?enable=true";
            TestWebScriptServer.PostRequest req = new TestWebScriptServer.PostRequest(url, "", MimetypeMap.MIMETYPE_JSON);
            sendRequest(req, Status.STATUS_OK, admin);
        }
        
        // Check that it worked
        testGetIsAuditEnabledGlobally();
    }
    
    private static final String APP_REPO_NAME = "AlfrescoRepository";
    private static final String APP_REPO_PATH = "/repository";
    public void testGetIsAuditEnabledRepo() throws Exception
    {
        boolean wasEnabled = auditService.isAuditEnabled(APP_REPO_NAME, null);

        String url = "/api/audit/control/" + APP_REPO_NAME + APP_REPO_PATH;
        TestWebScriptServer.GetRequest req = new TestWebScriptServer.GetRequest(url);
        
        if (wasEnabled)
        {
            Response response = sendRequest(req, Status.STATUS_OK, admin);
            JSONObject json = new JSONObject(response.getContentAsString());
            JSONArray apps = json.getJSONArray(AbstractAuditWebScript.JSON_KEY_APPLICATIONS);
            assertEquals("Incorrect number of applications reported", 1, apps.length());
            JSONObject app = apps.getJSONObject(0);
            String appName = app.getString(AbstractAuditWebScript.JSON_KEY_NAME);
            String appPath = app.getString(AbstractAuditWebScript.JSON_KEY_PATH);
            boolean appEnabled = app.getBoolean(AbstractAuditWebScript.JSON_KEY_ENABLED);
            assertEquals("Mismatched application audit enabled", wasEnabled, appEnabled);
            assertEquals("Mismatched application audit name", APP_REPO_NAME, appName);
            assertEquals("Mismatched application audit path", APP_REPO_PATH, appPath);
        }
        else
        {
            
        }
    }
    
    public void testSetAuditEnabledRepo() throws Exception
    {
        boolean wasEnabled = auditService.isAuditEnabled(APP_REPO_NAME, APP_REPO_PATH);

        if (wasEnabled)
        {
            String url = "/api/audit/control/" + APP_REPO_NAME + APP_REPO_PATH + "?enable=false";
            TestWebScriptServer.PostRequest req = new TestWebScriptServer.PostRequest(url, "", MimetypeMap.MIMETYPE_JSON);
            sendRequest(req, Status.STATUS_OK, admin);
        }
        else
        {
            String url = "/api/audit/control/" + APP_REPO_NAME + APP_REPO_PATH + "?enable=true";
            TestWebScriptServer.PostRequest req = new TestWebScriptServer.PostRequest(url, "", MimetypeMap.MIMETYPE_JSON);
            sendRequest(req, Status.STATUS_OK, admin);
        }
        
        // Check that it worked
        testGetIsAuditEnabledRepo();
    }
    
    /**
     * Perform a failed login attempt
     */
    private void loginWithFailure() throws Exception
    {
        // Force a failed login
        RunAsWork<Void> failureWork = new RunAsWork<Void>()
        {
            @Override
            public Void doWork() throws Exception
            {
                try
                {
                    authenticationService.authenticate("domino", "crud".toCharArray());
                    fail("Failed to force authentication failure");
                }
                catch (AuthenticationException e)
                {
                    // Expected
                }
                return null;
            }
        };
        AuthenticationUtil.runAs(failureWork, AuthenticationUtil.getSystemUserName());
    }
    
    public void testClearAuditRepo() throws Exception
    {
        long now = System.currentTimeMillis();
        long future = Long.MAX_VALUE;
        
        loginWithFailure();
        
        // Delete audit entries that could not have happened
        String url = "/api/audit/clear/" + APP_REPO_NAME + "?fromTime=" + future;
        TestWebScriptServer.PostRequest req = new TestWebScriptServer.PostRequest(url, "", MimetypeMap.MIMETYPE_JSON);
        Response response = sendRequest(req, Status.STATUS_OK, admin);
        JSONObject json = new JSONObject(response.getContentAsString());
        int cleared = json.getInt(AbstractAuditWebScript.JSON_KEY_CLEARED);
        assertEquals("Could not have cleared more than 0", 0, cleared);

        url = "/api/audit/clear/" + APP_REPO_NAME + "?fromTime=" + now + "&toTime=" + future;
        req = new TestWebScriptServer.PostRequest(url, "", MimetypeMap.MIMETYPE_JSON);
        response = sendRequest(req, Status.STATUS_OK, admin);
        json = new JSONObject(response.getContentAsString());
        cleared = json.getInt(AbstractAuditWebScript.JSON_KEY_CLEARED);
        assertTrue("Should have cleared at least 1 entry", cleared > 0);
    }
    
    public void testQueryAuditRepo() throws Exception
    {
        long now = System.currentTimeMillis();
        long future = Long.MAX_VALUE;
        
        auditService.setAuditEnabled(true);
        auditService.enableAudit(APP_REPO_NAME, APP_REPO_PATH);

        loginWithFailure();
        
        // Delete audit entries that could not have happened
        String url = "/api/audit/query/" + APP_REPO_NAME + "?fromTime=" + now + "&verbose=true";
        TestWebScriptServer.GetRequest req = new TestWebScriptServer.GetRequest(url);
        Response response = sendRequest(req, Status.STATUS_OK, admin);
        JSONObject json = new JSONObject(response.getContentAsString());
        JSONArray jsonEntries = json.getJSONArray(AbstractAuditWebScript.JSON_KEY_ENTRIES);
        assertTrue("Expected at least one entry", jsonEntries.length() > 0);
    }
}
