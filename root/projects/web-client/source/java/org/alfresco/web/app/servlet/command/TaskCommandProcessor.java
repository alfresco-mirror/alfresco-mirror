/*
 * Copyright (C) 2005 Alfresco, Inc.
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
 * Foundation, Inc., 51 Franklin Street, Fifth Floor, Boston, MA  02110-1301, USA.
 */
package org.alfresco.web.app.servlet.command;

import java.io.PrintWriter;
import java.util.HashMap;
import java.util.Map;

import javax.servlet.ServletContext;
import javax.servlet.http.HttpServletRequest;

import org.alfresco.error.AlfrescoRuntimeException;
import org.alfresco.service.ServiceRegistry;

/**
 * Task specific command processor implementation.
 * <p>
 * Responsible for executing workflow task operations.
 * 
 * @author David Caruana
 */
public final class TaskCommandProcessor implements CommandProcessor
{
   private String taskId;
   private String transition = null;
   private String command;
   
   static
   {
      // add our commands to the command registry
      CommandFactory.getInstance().registerCommand("end", EndTaskCommand.class);
   }

   
   /* (non-Javadoc)
    * @see org.alfresco.web.app.servlet.command.CommandProcessor#validateArguments(javax.servlet.ServletContext, java.lang.String, java.util.Map, java.lang.String[])
    */
   public boolean validateArguments(ServletContext sc, String command, Map<String, String> args, String[] urlElements)
   {
       if (urlElements.length == 0)
       {
          throw new IllegalArgumentException("Not enough URL arguments passed to command servlet.");
       }
       taskId = urlElements[0];
       if (urlElements.length == 2)
       {
           transition = urlElements[1];
       }
       return true;
   }
   
   /**
    * @see org.alfresco.web.app.servlet.command.CommandProcessor#process(org.alfresco.service.ServiceRegistry, javax.servlet.http.HttpServletRequest, java.lang.String)
    */
   public void process(ServiceRegistry serviceRegistry, HttpServletRequest request, String command)
   {
      Map<String, Object> properties = new HashMap<String, Object>(1, 1.0f);
      // all workflow commands use a "target" Node property as an argument
      properties.put(EndTaskCommand.PROP_TASK_ID, taskId);
      if (transition != null)
      {
          properties.put(EndTaskCommand.PROP_TRANSITION, transition);
      }
      Command cmd = CommandFactory.getInstance().createCommand(command);
      if (cmd == null)
      {
         throw new AlfrescoRuntimeException("Unregistered workflow command specified: " + command);
      }
      cmd.execute(serviceRegistry, properties);
      this.command = command;
   }

   /**
    * @see org.alfresco.web.app.servlet.command.CommandProcessor#outputStatus(java.io.PrintWriter)
    */
   public void outputStatus(PrintWriter out)
   {
      out.print("Task command: '");
      out.print(command);
      out.print("' executed against task: ");
      out.println(taskId);
   }

}
