/*
 * Copyright (C) 2005 Alfresco, Inc.
 *
 * Licensed under the GNU Lesser General Public License as
 * published by the Free Software Foundation; either version
 * 2.1 of the License, or (at your option) any later version.
 * You may obtain a copy of the License at
 *
 *     http://www.gnu.org/licenses/lgpl.txt
 *
 * Unless required by applicable law or agreed to in writing,
 * software distributed under the License is distributed on an
 * "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND,
 * either express or implied. See the License for the specific
 * language governing permissions and limitations under the
 * License.
 */
package org.alfresco.repo.action.executer;

import java.io.Serializable;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.alfresco.model.ContentModel;
import org.alfresco.repo.action.ParameterDefinitionImpl;
import org.alfresco.service.cmr.action.Action;
import org.alfresco.service.cmr.action.ParameterDefinition;
import org.alfresco.service.cmr.dictionary.PropertyTypeDefinition;
import org.alfresco.service.cmr.repository.NodeRef;
import org.alfresco.service.cmr.repository.NodeService;
import org.alfresco.service.namespace.QName;

/**
 * Simple workflow action executor
 * 
 * @author Roy Wetherall
 */
public class SimpleWorkflowActionExecuter extends ActionExecuterAbstractBase 
{
	public static final String NAME = "simple-workflow";
	public static final String PARAM_APPROVE_STEP = "approve-step";
	public static final String PARAM_APPROVE_FOLDER = "approve-folder";
	public static final String PARAM_APPROVE_MOVE = "approve-move";
	public static final String PARAM_REJECT_STEP = "reject-step";
	public static final String PARAM_REJECT_FOLDER = "reject-folder";
	public static final String PARAM_REJECT_MOVE = "reject-move";
	
	private NodeService nodeService;

	public void setNodeService(NodeService nodeService) 
	{
		this.nodeService = nodeService;
	}

	@Override
	protected void addParameterDefintions(List<ParameterDefinition> paramList) 
	{
		paramList.add(new ParameterDefinitionImpl(PARAM_APPROVE_STEP, PropertyTypeDefinition.TEXT, false, getParamDisplayLabel(PARAM_APPROVE_STEP)));
		paramList.add(new ParameterDefinitionImpl(PARAM_APPROVE_FOLDER, PropertyTypeDefinition.NODE_REF, false, getParamDisplayLabel(PARAM_APPROVE_FOLDER)));
		paramList.add(new ParameterDefinitionImpl(PARAM_APPROVE_MOVE, PropertyTypeDefinition.BOOLEAN, false, getParamDisplayLabel(PARAM_APPROVE_MOVE)));
		paramList.add(new ParameterDefinitionImpl(PARAM_REJECT_STEP, PropertyTypeDefinition.TEXT, false, getParamDisplayLabel(PARAM_REJECT_STEP)));
		paramList.add(new ParameterDefinitionImpl(PARAM_REJECT_FOLDER, PropertyTypeDefinition.NODE_REF, false, getParamDisplayLabel(PARAM_REJECT_FOLDER)));
		paramList.add(new ParameterDefinitionImpl(PARAM_REJECT_MOVE, PropertyTypeDefinition.BOOLEAN, false, getParamDisplayLabel(PARAM_REJECT_MOVE)));		
	}

	/**
	 * @see org.alfresco.repo.action.executer.ActionExecuterAbstractBase#executeImpl(org.alfresco.service.cmr.repository.NodeRef, org.alfresco.service.cmr.repository.NodeRef)
	 */
	@Override
	protected void executeImpl(
			Action ruleAction,
			NodeRef actionedUponNodeRef) 
	{
		if (this.nodeService.exists(actionedUponNodeRef) == true)
		{
			// Get the parameter values
			String approveStep = (String)ruleAction.getParameterValue(PARAM_APPROVE_STEP);
			NodeRef approveFolder = (NodeRef)ruleAction.getParameterValue(PARAM_APPROVE_FOLDER);
			Boolean approveMove = (Boolean)ruleAction.getParameterValue(PARAM_APPROVE_MOVE);
			String rejectStep = (String)ruleAction.getParameterValue(PARAM_REJECT_STEP);
			NodeRef rejectFolder = (NodeRef)ruleAction.getParameterValue(PARAM_REJECT_FOLDER);
			Boolean rejectMove = (Boolean)ruleAction.getParameterValue(PARAM_REJECT_MOVE);
			
			// Set the property values
			Map<QName, Serializable> propertyValues = new HashMap<QName, Serializable>();
			propertyValues.put(ContentModel.PROP_APPROVE_STEP, approveStep);
			propertyValues.put(ContentModel.PROP_APPROVE_FOLDER, approveFolder);
			if (approveMove != null)
			{
				propertyValues.put(ContentModel.PROP_APPROVE_MOVE, approveMove.booleanValue());
			}						
			propertyValues.put(ContentModel.PROP_REJECT_STEP, rejectStep);
			propertyValues.put(ContentModel.PROP_REJECT_FOLDER, rejectFolder);
	        if (rejectMove != null)
	        {
	        	propertyValues.put(ContentModel.PROP_REJECT_MOVE, rejectMove.booleanValue());
	        }
			
			// Apply the simple workflow aspect to the node
			this.nodeService.addAspect(actionedUponNodeRef, ContentModel.ASPECT_SIMPLE_WORKFLOW, propertyValues);
		}
	}
}
