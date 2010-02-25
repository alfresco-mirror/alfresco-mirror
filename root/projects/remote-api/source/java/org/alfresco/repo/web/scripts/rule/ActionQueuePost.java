/*
 * Copyright (C) 2005-2009 Alfresco Software Limited.
 *
 * This program is free software; you can redistribute it and/or
 * modify it under the terms of the GNU General Public License
 * as published by the Free Software Foundation; either version 2
 * of the License, or (at your option) any later version.

 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.

 * You should have received a copy of the GNU General Public License
 * along with this program; if not, write to the Free Software
 * Foundation, Inc., 51 Franklin Street, Fifth Floor, Boston, MA 02110-1301, USA.

 * As a special exception to the terms and conditions of version 2.0 of 
 * the GPL, you may redistribute this Program in connection with Free/Libre 
 * and Open Source Software ("FLOSS") applications as described in Alfresco's 
 * FLOSS exception.  You should have recieved a copy of the text describing 
 * the FLOSS exception, and it is also available here: 
 * http://www.alfresco.com/legal/licensing"
 */
package org.alfresco.repo.web.scripts.rule;

import java.io.IOException;
import java.util.HashMap;
import java.util.Map;

import org.alfresco.repo.action.ActionImpl;
import org.alfresco.service.cmr.repository.NodeRef;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.json.JSONException;
import org.json.JSONObject;
import org.json.JSONTokener;
import org.springframework.extensions.webscripts.Cache;
import org.springframework.extensions.webscripts.Status;
import org.springframework.extensions.webscripts.WebScriptException;
import org.springframework.extensions.webscripts.WebScriptRequest;

/**
 * @author unknown
 *
 */
public class ActionQueuePost extends AbstractRuleWebScript
{
    @SuppressWarnings("unused")
    private static Log logger = LogFactory.getLog(ActionQueuePost.class);
    
    public static final String STATUS = "actionExecStatus";
    public static final String STATUS_SUCCESS = "success";
    public static final String STATUS_FAIL = "fail";
    public static final String STATUS_QUEUED = "queued";
    
    @Override
    protected Map<String, Object> executeImpl(WebScriptRequest req, Status status, Cache cache)
    {
        Map<String, Object> model = new HashMap<String, Object>();
        
        // get request parameters
        boolean async = Boolean.parseBoolean(req.getParameter("async"));
        
        ActionImpl action = null;
        JSONObject json = null;
        
        try
        {
            // read request json
            json = new JSONObject(new JSONTokener(req.getContent().getContent()));
            
            // parse request json
            action = parseJsonAction(json);
            NodeRef actionedUponNode = action.getNodeRef();
            
            // clear nodeRef for action
            action.setNodeRef(null);
            json.remove("actionedUponNode");
            
            if (async)
            {
                model.put(STATUS, STATUS_QUEUED);
            }
            else
            {
                model.put(STATUS, STATUS_SUCCESS);
            }
            
            try
            {
                actionService.executeAction(action, actionedUponNode, true, async);
            }
            catch(Throwable e)
            {
                model.put(STATUS, STATUS_FAIL);
                model.put("exception", e);
            }
            
            model.put("actionedUponNode", actionedUponNode.toString());
            model.put("action", json);            
        }
        catch (IOException iox)
        {
            throw new WebScriptException(Status.STATUS_BAD_REQUEST,
                    "Could not read content from req.", iox);
        }
        catch (JSONException je)
        {
            throw new WebScriptException(Status.STATUS_BAD_REQUEST,
                        "Could not parse JSON from req.", je);
        }
        
        return model;
    }    
}