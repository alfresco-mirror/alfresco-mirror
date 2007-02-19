/*
 * Copyright (C) 2005-2007 Alfresco Software Limited.
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
package org.alfresco.sample;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.List;
import java.util.StringTokenizer;

import org.alfresco.repo.action.ParameterDefinitionImpl;
import org.alfresco.repo.action.executer.ActionExecuterAbstractBase;
import org.alfresco.service.cmr.action.Action;
import org.alfresco.service.cmr.action.ParameterDefinition;
import org.alfresco.service.cmr.dictionary.DataTypeDefinition;
import org.alfresco.service.cmr.repository.NodeRef;
import org.alfresco.service.cmr.repository.NodeService;
import org.alfresco.service.namespace.QName;

/**
 * Tag action executer.
 * 
 * This action adds the tag:taggable aspect to a node.
 * 
 * @author gavinc
 */
public class TagActionExecuter extends ActionExecuterAbstractBase
{
   /** The name of the action */
   public static final String NAME = "tag";

   /** The parameter names */
   public static final String PARAM_TAGS = "tags";

   /**
    * The node service
    */
   private NodeService nodeService;
   
    /**
     * Sets the node service
     * 
     * @param nodeService   the node service
     */
   public void setNodeService(NodeService nodeService) 
   {
      this.nodeService = nodeService;
   }
   
   /**
    * This action will take the comma separated list of tags and add them
    * separately to the tags property after applying the taggable aspect.
    * 
    * If no tags are supplied the aspect is still applied.
    */
   @Override
   protected void executeImpl(Action action, NodeRef actionedUponNodeRef)
   {
      if (this.nodeService.exists(actionedUponNodeRef) == true)
      {
         // add the aspect if it is not already present on the node
         QName tagAspect = QName.createQName("extension.tags", "taggable");
         if (this.nodeService.hasAspect(actionedUponNodeRef, tagAspect) == false)
         {
            this.nodeService.addAspect(actionedUponNodeRef, tagAspect, null);
         }
         
         // create the tags as a list
         String tags = (String)action.getParameterValue(PARAM_TAGS);
         List<String> tagsList = new ArrayList<String>();
         if (tags != null && tags.length() > 0)
         {
            StringTokenizer tokenizer = new StringTokenizer(tags, ",");
            while (tokenizer.hasMoreTokens())
            {
               tagsList.add(tokenizer.nextToken().trim());
            }
         }
         
         // set the tags property
         QName tagsProp = QName.createQName("extension.tags", "tags");
         this.nodeService.setProperty(actionedUponNodeRef, tagsProp, (Serializable)tagsList);
      }
  }
   
  /**
   * @see org.alfresco.repo.action.ParameterizedItemAbstractBase#addParameterDefinitions(java.util.List)
   */
   @Override
   protected void addParameterDefinitions(List<ParameterDefinition> paramList)
   {
      // Specify the parameters
      paramList.add(new ParameterDefinitionImpl(PARAM_TAGS,
            DataTypeDefinition.TEXT, true, getParamDisplayLabel(PARAM_TAGS)));
   }
}
