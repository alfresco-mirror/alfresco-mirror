/**
 * 
 */
package org.alfresco.repo.avm.actions;

import java.util.ArrayList;
import java.util.List;

import org.alfresco.error.AlfrescoRuntimeException;
import org.alfresco.repo.action.ParameterDefinitionImpl;
import org.alfresco.repo.action.executer.ActionExecuterAbstractBase;
import org.alfresco.repo.avm.AVMNodeConverter;
import org.alfresco.service.cmr.action.Action;
import org.alfresco.service.cmr.action.ParameterDefinition;
import org.alfresco.service.cmr.avm.AVMNodeDescriptor;
import org.alfresco.service.cmr.avm.AVMService;
import org.alfresco.service.cmr.avmsync.AVMDifference;
import org.alfresco.service.cmr.avmsync.AVMSyncService;
import org.alfresco.service.cmr.dictionary.DataTypeDefinition;
import org.alfresco.service.cmr.repository.NodeRef;
import org.alfresco.util.Pair;
import org.apache.log4j.Logger;

/**
 * Revert a single path to a specified node. The path in head is passed
 * as actionedUponNodeRef.  The node to revert to is passed as an AVMNodeDescriptor
 * parameter.
 * @author britt
 */
public class AVMRevertToVersionAction extends ActionExecuterAbstractBase 
{
    private static Logger fgLogger = Logger.getLogger(AVMRevertToVersionAction.class);
    
    public static final String NAME = "avm-revert-to-version";
    // The node to revert to. Passed as an AVMNodeDescriptor.
    public static final String TOREVERT = "to-revert";
    
    private AVMService fAVMService;

    private AVMSyncService fAVMSyncService;
    
    /**
     * Set the AVMService.
     */
    public void setAvmService(AVMService service)
    {
        fAVMService = service;
    }
    
    /**
     * Set the AVMSyncService.  
     */  
    public void setAvmSyncService(AVMSyncService service)
    {
        fAVMSyncService = service;
    }
    
    /* (non-Javadoc)
     * @see org.alfresco.repo.action.executer.ActionExecuterAbstractBase#executeImpl(org.alfresco.service.cmr.action.Action, org.alfresco.service.cmr.repository.NodeRef)
     */
    @Override
    protected void executeImpl(Action action, NodeRef actionedUponNodeRef) 
    {
        Pair<Integer, String> versionPath = 
            AVMNodeConverter.ToAVMVersionPath(actionedUponNodeRef);
        AVMNodeDescriptor toRevert = 
            (AVMNodeDescriptor)action.getParameterValue(TOREVERT);
        List<Pair<Integer, String>> paths = fAVMService.getPaths(toRevert);
        if (paths.size() == 0)
        {
            fgLogger.error("Unable to find path for: " + toRevert);
            throw new AlfrescoRuntimeException("Could not find path for: " + toRevert);
        }
        AVMDifference diff = new AVMDifference(paths.get(0).getFirst(), paths.get(0).getSecond(),
                                               -1, versionPath.getSecond(),
                                               AVMDifference.NEWER);
        List<AVMDifference> diffs = new ArrayList<AVMDifference>(1);
        diffs.add(diff);
        String message = "Reverted " + versionPath.getSecond() + " to version in snapshot " + paths.get(0).getFirst() + ".";
        fAVMSyncService.update(diffs, null, false, false, true, true, "Reverted", message);
    }

    /* (non-Javadoc)
     * @see org.alfresco.repo.action.ParameterizedItemAbstractBase#addParameterDefinitions(java.util.List)
     */
    @Override
    protected void addParameterDefinitions(List<ParameterDefinition> paramList) 
    {
        paramList.add(
                new ParameterDefinitionImpl(TOREVERT,
                                            DataTypeDefinition.ANY,
                                            true,
                                            getParamDisplayLabel(TOREVERT)));
    }
}
