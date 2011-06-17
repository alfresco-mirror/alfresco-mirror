/**
 * 
 */
package org.alfresco.module.org_alfresco_module_dod5015.disposition;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Date;
import java.util.List;
import java.util.Map;

import org.alfresco.error.AlfrescoRuntimeException;
import org.alfresco.model.ContentModel;
import org.alfresco.module.org_alfresco_module_dod5015.RecordsManagementService;
import org.alfresco.module.org_alfresco_module_dod5015.RecordsManagementServiceRegistry;
import org.alfresco.module.org_alfresco_module_dod5015.model.RecordsManagementModel;
import org.alfresco.repo.policy.BehaviourFilter;
import org.alfresco.service.cmr.dictionary.DictionaryService;
import org.alfresco.service.cmr.repository.ChildAssociationRef;
import org.alfresco.service.cmr.repository.NodeRef;
import org.alfresco.service.cmr.repository.NodeService;
import org.alfresco.service.namespace.NamespaceService;
import org.alfresco.service.namespace.QName;
import org.alfresco.service.namespace.RegexQNamePattern;
import org.alfresco.util.ParameterCheck;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.springframework.beans.BeansException;
import org.springframework.context.ApplicationContext;
import org.springframework.context.ApplicationContextAware;

/**
 * Disposition service implementation.
 * 
 * @author Roy Wetherall
 */
public class DispositionServiceImpl implements DispositionService, RecordsManagementModel, ApplicationContextAware
{
    /** Logger */
    private static Log logger = LogFactory.getLog(DispositionServiceImpl.class);
    
    /** Node service */
    private NodeService nodeService;
    
    /** Dictionary service */
    private DictionaryService dictionaryService;
    
    /** Behaviour filter */
    private BehaviourFilter behaviourFilter;
    
    /** Records management service */
    private RecordsManagementService rmService;
    
    /** Records management service registry */
    private RecordsManagementServiceRegistry serviceRegistry;
    
    /** Disposition selection strategy */
    private DispositionSelectionStrategy dispositionSelectionStrategy;
    
    /** Application context */
    private ApplicationContext applicationContext;
    
    /**
     * Set node service
     * 
     * @param nodeService   the node service
     */
    public void setNodeService(NodeService nodeService)
    {
        this.nodeService = nodeService;
    }
    
    /**
     * Set the dictionary service
     * 
     * @param dictionaryServic  the dictionary service
     */
    public void setDictionaryService(DictionaryService dictionaryService)
    {
        this.dictionaryService = dictionaryService;
    }
    
    /**
     * Set the behaviour filter.
     * 
     * @param behaviourFilter   the behaviour filter
     */
    public void setBehaviourFilter(BehaviourFilter behaviourFilter)
    {
        this.behaviourFilter = behaviourFilter;
    }
   
    /**
     * Set the records management service registry
     * 
     * @param serviceRegistry   records management registry service
     */
    public void setRecordsManagementServiceRegistry(RecordsManagementServiceRegistry serviceRegistry)
    {
        this.serviceRegistry = serviceRegistry;
    }
    
    /**
     * Get the records management service 
     * NOTE: have to pull it out of the app context manually to prevent Spring circular dependancy issue
     * 
     * @return
     */
    public RecordsManagementService getRmService()
    {
        if (rmService == null)
        {
            rmService = (RecordsManagementService)applicationContext.getBean("recordsManagementService");
        }
        return rmService;
    }
    
    /**
     * @see org.springframework.context.ApplicationContextAware#setApplicationContext(org.springframework.context.ApplicationContext)
     */
    @Override
    public void setApplicationContext(ApplicationContext applicationContext) throws BeansException
    {
        this.applicationContext = applicationContext;
    }
    
    /**
     * Set the dispositionSelectionStrategy bean.
     * 
     * @param dispositionSelectionStrategy
     */
    public void setDispositionSelectionStrategy(DispositionSelectionStrategy dispositionSelectionStrategy)
    {
        this.dispositionSelectionStrategy = dispositionSelectionStrategy;
    }
    
    /** ========= Disposition Schedule Methods ========= */
    
    /**
     * @see org.alfresco.module.org_alfresco_module_dod5015.disposition.DispositionService#getDispositionSchedule(org.alfresco.service.cmr.repository.NodeRef)
     */
    public DispositionSchedule getDispositionSchedule(NodeRef nodeRef)
    {   
        DispositionSchedule di = null;
        NodeRef diNodeRef = null;
        if (getRmService().isRecord(nodeRef) == true)
        {
            // Get the record folders for the record
            List<NodeRef> recordFolders = getRmService().getRecordFolders(nodeRef);
            // At this point, we may have disposition instruction objects from 1..n folders.
            diNodeRef = dispositionSelectionStrategy.selectDispositionScheduleFrom(recordFolders);
        }
        else
        {
            // Get the disposition instructions for the node reference provided
            diNodeRef = getDispositionScheduleImpl(nodeRef);
        }
        
        if (diNodeRef != null)
        {
            di = new DispositionScheduleImpl(serviceRegistry, nodeService, diNodeRef);
        }
        
        return di;
    }
    
    /**
     * This method returns a NodeRef
     * Gets the disposition instructions 
     * 
     * @param nodeRef
     * @return
     */
    private NodeRef getDispositionScheduleImpl(NodeRef nodeRef)
    {
        NodeRef result = getAssociatedDispositionScheduleImpl(nodeRef);
        
        if (result == null)
        {
            NodeRef parent = this.nodeService.getPrimaryParent(nodeRef).getParentRef();
            if (parent != null && getRmService().isRecordsManagementContainer(parent) == true)
            {
                result = getDispositionScheduleImpl(parent);
            }
        }
        return result;
    }
    
    /**
     * @see org.alfresco.module.org_alfresco_module_dod5015.disposition.DispositionService#getAssociatedDispositionSchedule(org.alfresco.service.cmr.repository.NodeRef)
     */
    public DispositionSchedule getAssociatedDispositionSchedule(NodeRef nodeRef)
    {
        DispositionSchedule ds = null;
        
        // Check the noderef parameter
        ParameterCheck.mandatory("nodeRef", nodeRef);
        if (nodeService.exists(nodeRef) == true)
        {        
            // Get the associated disposition schedule node reference
            NodeRef dsNodeRef = getAssociatedDispositionScheduleImpl(nodeRef);
            if (dsNodeRef != null)
            {
                // Cerate disposition schedule object
                ds = new DispositionScheduleImpl(serviceRegistry, nodeService, dsNodeRef);
            }
        }
        
        return ds;
    }
    
    /**
     * Gets the node reference of the disposition schedule associated with the container.
     * 
     * @param nodeRef   node reference of the container
     * @return {@link NodeRef}  node reference of the disposition schedule, null if none
     */
    private NodeRef getAssociatedDispositionScheduleImpl(NodeRef nodeRef)
    {
        NodeRef result = null;     
        ParameterCheck.mandatory("nodeRef", nodeRef);
        
        // Make sure we are dealing with an RM node
        if (getRmService().isRecordsManagmentComponent(nodeRef) == false)
        {
            throw new AlfrescoRuntimeException("Can not find the associated disposition schedule for a non records management component. (nodeRef=" + nodeRef.toString() + ")");
        }

        if (this.nodeService.hasAspect(nodeRef, ASPECT_SCHEDULED) == true)
        {
            List<ChildAssociationRef> childAssocs = this.nodeService.getChildAssocs(nodeRef, ASSOC_DISPOSITION_SCHEDULE, RegexQNamePattern.MATCH_ALL);
            if (childAssocs.size() != 0)
            {
                ChildAssociationRef firstChildAssocRef = childAssocs.get(0);
                result = firstChildAssocRef.getChildRef();
            }
        }
        
        return result;
    }
    
    /**
     * @see org.alfresco.module.org_alfresco_module_dod5015.disposition.DispositionService#getAssociatedRecordsManagementContainer(org.alfresco.module.org_alfresco_module_dod5015.disposition.DispositionSchedule)
     */
    @Override
    public NodeRef getAssociatedRecordsManagementContainer(DispositionSchedule dispositionSchedule)
    {
        ParameterCheck.mandatory("dispositionSchedule", dispositionSchedule);
        NodeRef result = null;
        
        NodeRef dsNodeRef = dispositionSchedule.getNodeRef();
        if (nodeService.exists(dsNodeRef) == true)
        {
            List<ChildAssociationRef> assocs = this.nodeService.getParentAssocs(dsNodeRef, ASSOC_DISPOSITION_SCHEDULE, RegexQNamePattern.MATCH_ALL);
            if (assocs.size() != 0)
            {
                if (assocs.size() != 1)
                {
                    // TODO in the future we should be able to support disposition schedule reuse, but for now just warn that
                    //      only the first disposition schedule will be considered
                    if (logger.isWarnEnabled() == true)
                    {
                        logger.warn("Disposition schedule has more than one associated records management container.  " +
                        		    "This is not currently supported so only the first container will be considered. " +
                        		    "(dispositionScheduleNodeRef=" + dispositionSchedule.getNodeRef().toString() + ")");
                    }
                }
                
                // Get the container reference
                ChildAssociationRef assoc = assocs.get(0);
                result = assoc.getParentRef();
            }
        }
        
        return result;
    }
    
    /**
     * @see org.alfresco.module.org_alfresco_module_dod5015.disposition.DispositionService#hasDisposableItems(org.alfresco.module.org_alfresco_module_dod5015.disposition.DispositionSchedule)
     */    
    @Override
    public boolean hasDisposableItems(DispositionSchedule dispositionSchdule) 
    {
    	return !getDisposableItems(dispositionSchdule).isEmpty();
    }
    
    /**
     * @see org.alfresco.module.org_alfresco_module_dod5015.disposition.DispositionService#getDisposableItems(org.alfresco.module.org_alfresco_module_dod5015.disposition.DispositionSchedule)
     */
    public List<NodeRef> getDisposableItems(DispositionSchedule dispositionSchedule)
    {
        ParameterCheck.mandatory("dispositionSchedule", dispositionSchedule);
        
        // Get the associated container
        NodeRef rmContainer = getAssociatedRecordsManagementContainer(dispositionSchedule);
        
        // Return the disposable items
        return getDisposableItemsImpl(dispositionSchedule.isRecordLevelDisposition(), rmContainer);
    }
    
    /**
     * 
     * @param isRecordLevelDisposition
     * @param rmContainer
     * @param root
     * @return
     */
    private List<NodeRef> getDisposableItemsImpl(boolean isRecordLevelDisposition, NodeRef rmContainer)
    {
        List<NodeRef> items = getRmService().getAllContained(rmContainer);
        List<NodeRef> result = new ArrayList<NodeRef>(items.size());
        for (NodeRef item : items)
        {
            if (getRmService().isRecordFolder(item) == true)
            {            
                if (isRecordLevelDisposition == true)
                {
                    result.addAll(getRmService().getRecords(item));
                }
                else
                {
                    result.add(item);
                }
            }
            else if (getRmService().isRecordsManagementContainer(item) == true)
            {
                if (getAssociatedDispositionScheduleImpl(item) == null)
                {
                    result.addAll(getDisposableItemsImpl(isRecordLevelDisposition, item));
                }
            }
        }
        return result;
    }
    
    /**
     * @see org.alfresco.module.org_alfresco_module_dod5015.disposition.DispositionService#createDispositionSchedule(org.alfresco.service.cmr.repository.NodeRef, java.util.Map)
     */
    @Override
    public DispositionSchedule createDispositionSchedule(NodeRef nodeRef, Map<QName, Serializable> props)
    {
        NodeRef dsNodeRef = null;
        
        // Check mandatory parameters
        ParameterCheck.mandatory("nodeRef", nodeRef);
        
        // Check exists
        if (nodeService.exists(nodeRef) == false)
        {
            throw new AlfrescoRuntimeException("Unable to create disposition schedule, because node does not exist. (nodeRef=" + nodeRef.toString() + ")");
        }
        
        // Check is sub-type of rm:recordsManagementContainer
        QName nodeRefType = nodeService.getType(nodeRef);
        if (TYPE_RECORDS_MANAGEMENT_CONTAINER.equals(nodeRefType) == false &&
            dictionaryService.isSubClass(nodeRefType, TYPE_RECORDS_MANAGEMENT_CONTAINER) == false)
        {
            throw new AlfrescoRuntimeException("Unable to create disposition schedule on a node that is not a records management container.");
        }
        
        behaviourFilter.disableBehaviour(nodeRef, ASPECT_SCHEDULED);
        try
        {        
            // Add the schedules aspect if required
            if (nodeService.hasAspect(nodeRef, ASPECT_SCHEDULED) == false)
            {
                nodeService.addAspect(nodeRef, ASPECT_SCHEDULED, null);
            }
             
            // Check whether there is already a disposition schedule object present            
            List<ChildAssociationRef> assocs = nodeService.getChildAssocs(nodeRef, ASSOC_DISPOSITION_SCHEDULE, RegexQNamePattern.MATCH_ALL);            
            if (assocs.size() == 0)
            {
            	DispositionSchedule currentDispositionSchdule = getDispositionSchedule(nodeRef);
            	if (currentDispositionSchdule != null)
            	{
            		List<NodeRef> items = getDisposableItemsImpl(currentDispositionSchdule.isRecordLevelDisposition(), nodeRef);
            		if (items.size() != 0)
            		{
            			throw new AlfrescoRuntimeException("Can not create a disposition schedule if there are disposable items already under the control of an other disposition schedule");
            		}
            	}
            	
                // Create the disposition schedule object
                dsNodeRef = nodeService.createNode(
                        nodeRef, 
                        ASSOC_DISPOSITION_SCHEDULE, 
                        QName.createQName(NamespaceService.CONTENT_MODEL_1_0_URI, QName.createValidLocalName("dispositionSchedule")),
                        TYPE_DISPOSITION_SCHEDULE,
                        props).getChildRef();
            } 
            else
            {
                // Error since the node already has a disposition schedule set
                throw new AlfrescoRuntimeException("Unable to create disposition schedule on node that already has a disposition schedule.");
            }
        }
        finally
        {
            behaviourFilter.enableBehaviour(nodeRef, ASPECT_SCHEDULED);
        }
                
        // Create the return object        
        return new DispositionScheduleImpl(serviceRegistry, nodeService, dsNodeRef);
    }
    
    /** ========= Disposition Action Definition Methods ========= */
    
    /**
     * 
     */
    public DispositionActionDefinition addDispositionActionDefinition(
                                            DispositionSchedule schedule,
                                            Map<QName, Serializable> actionDefinitionParams)
    {
        // make sure at least a name has been defined
        String name = (String)actionDefinitionParams.get(PROP_DISPOSITION_ACTION_NAME);
        if (name == null || name.length() == 0)
        {
            throw new IllegalArgumentException("'name' parameter is mandatory when creating a disposition action definition");
        }
        
        // TODO: also check the action name is valid?
        
        // create the child association from the schedule to the action definition
        NodeRef actionNodeRef = this.nodeService.createNode(schedule.getNodeRef(), 
                    RecordsManagementModel.ASSOC_DISPOSITION_ACTION_DEFINITIONS, 
                    QName.createQName(NamespaceService.CONTENT_MODEL_1_0_URI, 
                    QName.createValidLocalName(name)),
                    RecordsManagementModel.TYPE_DISPOSITION_ACTION_DEFINITION, actionDefinitionParams).getChildRef();
        
        // get the updated disposition schedule and retrieve the new action definition
        NodeRef scheduleParent = this.nodeService.getPrimaryParent(schedule.getNodeRef()).getParentRef();
        DispositionSchedule updatedSchedule = this.getDispositionSchedule(scheduleParent);
        return updatedSchedule.getDispositionActionDefinition(actionNodeRef.getId());
    }    
    
    /**
     * @see org.alfresco.module.org_alfresco_module_dod5015.disposition.DispositionService#removeDispositionActionDefinition(org.alfresco.module.org_alfresco_module_dod5015.disposition.DispositionSchedule, org.alfresco.module.org_alfresco_module_dod5015.disposition.DispositionActionDefinition)
     */
    public void removeDispositionActionDefinition(DispositionSchedule schedule, DispositionActionDefinition actionDefinition)
    {
        // check first whether action definitions can be removed
        if (hasDisposableItems(schedule) == true)
        {
            throw new AlfrescoRuntimeException("Can not remove action definitions from schedule '" + 
                        schedule.getNodeRef() + "' as one or more record or record folders are present.");
        }
        
        // remove the child node representing the action definition
        this.nodeService.removeChild(schedule.getNodeRef(), actionDefinition.getNodeRef());
    }
    
    /**
     * Updates the given disposition action definition belonging to the given disposition
     * schedule.
     * 
     * @param schedule The DispositionSchedule the action belongs to
     * @param actionDefinition The DispositionActionDefinition to update
     * @param actionDefinitionParams Map of parameters to use to update the action definition
     * @return The updated DispositionActionDefinition
     */
    public DispositionActionDefinition updateDispositionActionDefinition(
                                                DispositionActionDefinition actionDefinition, 
                                                Map<QName, Serializable> actionDefinitionParams)
    {
        // update the node with properties
        this.nodeService.addProperties(actionDefinition.getNodeRef(), actionDefinitionParams);
        
        // get the updated disposition schedule and retrieve the updated action definition
        NodeRef ds = this.nodeService.getPrimaryParent(actionDefinition.getNodeRef()).getParentRef();
        DispositionSchedule updatedSchedule = new DispositionScheduleImpl(serviceRegistry, nodeService, ds);
        return updatedSchedule.getDispositionActionDefinition(actionDefinition.getId());     
    }
    
    /**
     * 
     */
    public boolean isNextDispositionActionEligible(NodeRef nodeRef)
    {
        boolean result = false;
        
        // Get the disposition instructions
        DispositionSchedule di = getDispositionSchedule(nodeRef);
        NodeRef nextDa = getNextDispositionActionNodeRef(nodeRef);
        if (di != null &&
            this.nodeService.hasAspect(nodeRef, ASPECT_DISPOSITION_LIFECYCLE) == true &&
            nextDa != null)
        {
            // If it has an asOf date and it is greater than now the action is eligible
            Date asOf = (Date)this.nodeService.getProperty(nextDa, PROP_DISPOSITION_AS_OF);
            if (asOf != null &&
                asOf.before(new Date()) == true)
            {
                result = true;
            }
            
            if (result == false)
            {
                // If all the events specified on the action have been completed the action is eligible
                List<ChildAssociationRef> assocs = this.nodeService.getChildAssocs(nextDa, ASSOC_EVENT_EXECUTIONS, RegexQNamePattern.MATCH_ALL);
                for (ChildAssociationRef assoc : assocs)
                {
                    NodeRef eventExecution = assoc.getChildRef();
                    Boolean isCompleteValue = (Boolean)this.nodeService.getProperty(eventExecution, PROP_EVENT_EXECUTION_COMPLETE);
                    boolean isComplete = false;
                    if (isCompleteValue != null)
                    {
                        isComplete = isCompleteValue.booleanValue();
                        
                        // TODO this only works for the OR use case .. need to handle optional AND handling
                        if (isComplete == true)
                        {
                            result = true;
                            break;
                        }
                    }
                }
            }
        }
        
        return result;
    }
    
    /**
     * Get the next disposition action node.  Null if none present.
     * 
     * @param nodeRef       the disposable node reference
     * @return NodeRef      the next disposition action, null if none
     */
    private NodeRef getNextDispositionActionNodeRef(NodeRef nodeRef)
    {
        NodeRef result = null;
        List<ChildAssociationRef> assocs = this.nodeService.getChildAssocs(nodeRef, ASSOC_NEXT_DISPOSITION_ACTION, RegexQNamePattern.MATCH_ALL);
        if (assocs.size() != 0)
        {
            result = assocs.get(0).getChildRef();
        }
        return result;
    }
    
    /** ========= Disposition Action Methods ========= */
    
    /**
     * 
     */
    public DispositionAction getNextDispositionAction(NodeRef nodeRef)
    {
        DispositionAction result = null;
        NodeRef dispositionActionNodeRef = getNextDispositionActionNodeRef(nodeRef);

        if (dispositionActionNodeRef != null)
        {
            result = new DispositionActionImpl(this.serviceRegistry, dispositionActionNodeRef);
        }
        return result;
    }
     
    
    /** ========= Disposition Action History Methods ========= */
    
    public List<DispositionAction> getCompletedDispositionActions(NodeRef nodeRef)
    {
        List<ChildAssociationRef> assocs = this.nodeService.getChildAssocs(nodeRef, ASSOC_DISPOSITION_ACTION_HISTORY, RegexQNamePattern.MATCH_ALL);
        List<DispositionAction> result = new ArrayList<DispositionAction>(assocs.size());        
        for (ChildAssociationRef assoc : assocs)
        {
            NodeRef dispositionActionNodeRef = assoc.getChildRef();
            result.add(new DispositionActionImpl(serviceRegistry, dispositionActionNodeRef));
        }        
        return result;
    }
    
    public DispositionAction getLastCompletedDispostionAction(NodeRef nodeRef)
    {
       DispositionAction result = null;
       List<DispositionAction> list = getCompletedDispositionActions(nodeRef);
       if (list.isEmpty() == false)
       {
           // Get the last disposition action in the list
           result = list.get(list.size()-1);
       }       
       return result;
    }
    

    public List<QName> getDispositionPeriodProperties()
    {
        DispositionPeriodProperties dpp = (DispositionPeriodProperties)applicationContext.getBean(DispositionPeriodProperties.BEAN_NAME);

        if (dpp == null)
        {
            return Collections.emptyList();
        }
        else
        {
            return dpp.getPeriodProperties();
        }
    }


}