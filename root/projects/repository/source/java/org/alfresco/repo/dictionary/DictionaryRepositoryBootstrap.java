/*
 * Copyright (C) 2005-2010 Alfresco Software Limited.
 *
 * This file is part of Alfresco
 *
 * Alfresco is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * Alfresco is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with Alfresco. If not, see <http://www.gnu.org/licenses/>.
 */
package org.alfresco.repo.dictionary;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.alfresco.error.AlfrescoRuntimeException;
import org.alfresco.model.ContentModel;
import org.alfresco.repo.i18n.MessageDeployer;
import org.alfresco.repo.i18n.MessageService;
import org.alfresco.repo.tenant.TenantAdminService;
import org.alfresco.repo.tenant.TenantDeployer;
import org.alfresco.repo.transaction.RetryingTransactionHelper.RetryingTransactionCallback;
import org.alfresco.service.cmr.repository.ChildAssociationRef;
import org.alfresco.service.cmr.repository.ContentReader;
import org.alfresco.service.cmr.repository.ContentService;
import org.alfresco.service.cmr.repository.NodeRef;
import org.alfresco.service.cmr.repository.NodeService;
import org.alfresco.service.cmr.repository.StoreRef;
import org.alfresco.service.namespace.NamespaceService;
import org.alfresco.service.namespace.QName;
import org.alfresco.service.namespace.RegexQNamePattern;
import org.alfresco.service.transaction.TransactionService;
import org.springframework.extensions.surf.util.AbstractLifecycleBean;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.springframework.context.ApplicationEvent;

/**
 * Bootstrap the dictionary from specified locations within the repository
 * 
 * @author Roy Wetherall, JanV
 */
public class DictionaryRepositoryBootstrap extends AbstractLifecycleBean implements TenantDeployer, DictionaryListener, MessageDeployer
{
    // Logging support
    private static Log logger = LogFactory
            .getLog("org.alfresco.repo.dictionary.DictionaryRepositoryBootstrap");

    /** Locations in the repository from which models should be loaded */
    private List<RepositoryLocation> repositoryModelsLocations = new ArrayList<RepositoryLocation>();

    /** Locations in the repository from which messages should be loaded */
    private List<RepositoryLocation> repositoryMessagesLocations = new ArrayList<RepositoryLocation>();

    /** Dictionary DAO */
    private DictionaryDAO dictionaryDAO = null;
    
    /** The content service */
    private ContentService contentService;

    /** The node service */
    private NodeService nodeService;
    
    /** The tenant admin service */
    private TenantAdminService tenantAdminService;

    /** The namespace service */
    private NamespaceService namespaceService;

    /** The message service */
    private MessageService messageService;

    /** The transaction service */
    private TransactionService transactionService;
      
    /**
     * Sets the Dictionary DAO
     * 
     * @param dictionaryDAO
     */
    public void setDictionaryDAO(DictionaryDAO dictionaryDAO)
    {
        this.dictionaryDAO = dictionaryDAO;
    }
    
    /** 
     * Set the content service
     * 
     * @param contentService    the content service
     */
    public void setContentService(ContentService contentService)
    {
        this.contentService = contentService;
    }

    /**
     * Set the node service
     * 
     * @param nodeService   the node service
     */
    public void setNodeService(NodeService nodeService)
    {
        this.nodeService = nodeService;
    }
    
    /**
     * Set the tenant admin service
     * 
     * @param tenantAdminService    the tenant admin service
     */
    public void setTenantAdminService(TenantAdminService tenantAdminService)
    {
        this.tenantAdminService = tenantAdminService;
    }

    /**
     * Set the namespace service
     * 
     * @param namespaceService the namespace service
     */
    public void setNamespaceService(NamespaceService namespaceService)
    {
        this.namespaceService = namespaceService;
    }

    /**
     * Set the message service
     * 
     * @param messageService    the message service
     */
    public void setMessageService(MessageService messageService)
    {
        this.messageService = messageService;
    }

    /**
     * Set the transaction service
     * 
     * @param transactionService    the transaction service
     */
    public void setTransactionService(TransactionService transactionService)
    {
        this.transactionService = transactionService;
    }
    
    /**
     * Set the repository models locations
     * 
     * @param repositoryModelsLocations   list of the repository models locations
     */    public void setRepositoryModelsLocations(
            List<RepositoryLocation> repositoryLocations)
    {
        this.repositoryModelsLocations = repositoryLocations;
    }
        
    /**
     * Set the repository messages (resource bundle) locations
     * 
     * @param repositoryLocations
     *            list of the repository messages locations
     */
    public void setRepositoryMessagesLocations(
            List<RepositoryLocation> repositoryLocations)
    {
        this.repositoryMessagesLocations = repositoryLocations;
    }


    /**
     * Initialise - after bootstrap of schema and tenant admin service
     */
    public void init()
    {
        transactionService.getRetryingTransactionHelper().doInTransaction(new RetryingTransactionCallback<Object>()
        {
            public Object execute() throws Exception
            {
                onDictionaryInit();
                initMessages();
                
                return (Object)null;
            }
        }, transactionService.isReadOnly(), false);
    }
    
    public void destroy()
    {    
        // NOOP - will be destroyed directly via DictionaryComponent
    }
    
    /*
     * (non-Javadoc)
     * @see org.alfresco.repo.dictionary.DictionaryListener#onInit()
     */
    public void onDictionaryInit()
    {
        if (this.repositoryModelsLocations != null)
        {
            Map<String, M2Model> modelMap = new HashMap<String, M2Model>();
            
            // Register the models found in the repository
            
            for (RepositoryLocation repositoryLocation : this.repositoryModelsLocations)
            {
                StoreRef storeRef = repositoryLocation.getStoreRef();
                
                if (! nodeService.exists(storeRef))
                {
                    logger.warn("StoreRef '"+ storeRef+"' does not exist");
                    continue; // skip this location
                }
                
                List<NodeRef> nodeRefs = null;
                
                if (repositoryLocation.getQueryLanguage().equals(RepositoryLocation.LANGUAGE_PATH))
                {
                    nodeRefs = getNodes(storeRef, repositoryLocation, ContentModel.TYPE_DICTIONARY_MODEL);
                    
                    if (nodeRefs.size() > 0)
                    {
                        for (NodeRef dictionaryModel : nodeRefs)
                        {
                            // Ignore if the node is a working copy or archived, or if its inactive
                            if (! (nodeService.hasAspect(dictionaryModel, ContentModel.ASPECT_WORKING_COPY) || nodeService.hasAspect(dictionaryModel, ContentModel.ASPECT_ARCHIVED))) 
                            {
                                Boolean isActive = (Boolean)nodeService.getProperty(dictionaryModel, ContentModel.PROP_MODEL_ACTIVE);
                                
                                if ((isActive != null) && (isActive.booleanValue() == true))
                                {
                                    M2Model model = createM2Model(dictionaryModel);
                                    if (model != null)
                                    {
                                        if (logger.isTraceEnabled())
                                        {
                                            logger.trace("onDictionaryInit: "+model.getName()+" ("+dictionaryModel+")");
                                        }
                                        
                                        for (M2Namespace namespace : model.getNamespaces())
                                        {
                                            modelMap.put(namespace.getUri(), model);
                                        }
                                    }
                                }
                            }
                        }
                    }
                }
                else
                {
                    logger.error("Unsupported query language for models location: " + repositoryLocation.getQueryLanguage());
                }
            }
            
            // Load the models ensuring that they are loaded in the correct order
            List<String> loadedModels = new ArrayList<String>();
            for (Map.Entry<String, M2Model> entry : modelMap.entrySet())
            {
                loadModel(modelMap, loadedModels, entry.getValue());
            }
        }
    }
    
    /*
     * (non-Javadoc)
     * @see org.alfresco.repo.dictionary.DictionaryListener#afterInit()
     */
    public void afterDictionaryInit()
    {
    }
    
    /*
     * (non-Javadoc)
     * @see org.alfresco.repo.dictionary.DictionaryListener#onDictionaryDestroy()
     */
    public void afterDictionaryDestroy()
    {
    }
    
    public void initMessages()
    {
        if (this.repositoryMessagesLocations != null)
        {
            // Register the messages found in the repository
            for (RepositoryLocation repositoryLocation : this.repositoryMessagesLocations)
            {
                StoreRef storeRef = repositoryLocation.getStoreRef();
                
                if (! nodeService.exists(storeRef))
                {
                    logger.warn("StoreRef '"+ storeRef+"' does not exist");
                    continue; // skip this location
                }
                
                if (repositoryLocation.getQueryLanguage().equals(RepositoryLocation.LANGUAGE_PATH))
                {
                    List<NodeRef> nodeRefs = getNodes(storeRef, repositoryLocation, ContentModel.TYPE_CONTENT);
                    
                    if (nodeRefs.size() > 0)
                    {
                        List<String> resourceBundleBaseNames = new ArrayList<String>();
                        
                        for (NodeRef messageResource : nodeRefs)
                        {
                            String resourceName = (String) nodeService.getProperty(messageResource, ContentModel.PROP_NAME);
                            
                            String bundleBaseName = messageService.getBaseBundleName(resourceName);
                            
                            if (!resourceBundleBaseNames.contains(bundleBaseName))
                            {
                                resourceBundleBaseNames.add(bundleBaseName);
                            }
                        }
                    }
                }
                else
                {
                    logger.error("Unsupported query language for messages location: " + repositoryLocation.getQueryLanguage());
                }
            }
        }
    }
    
    protected List<NodeRef> getNodes(StoreRef storeRef, RepositoryLocation repositoryLocation, QName nodeType)
    {
        List<NodeRef> nodeRefs = new ArrayList<NodeRef>();
        
        NodeRef rootNodeRef = nodeService.getRootNode(storeRef);
        String[] pathElements = repositoryLocation.getPathElements();
        
        NodeRef folderNodeRef = rootNodeRef;
        if (pathElements.length > 0)
        {
            folderNodeRef = resolveQNameFolderPath(rootNodeRef, pathElements);
        }
        
        if (folderNodeRef != null)
        {
            Set<QName> types = new HashSet<QName>(1);
            types.add(nodeType);
            List<ChildAssociationRef> childAssocRefs = nodeService.getChildAssocs(folderNodeRef, types);
            
            if (childAssocRefs.size() > 0)
            {
                nodeRefs = new ArrayList<NodeRef>(childAssocRefs.size());
                for (ChildAssociationRef childAssocRef : childAssocRefs)
                {
                    nodeRefs.add(childAssocRef.getChildRef());
                }
            }
        }
        
        return nodeRefs;
    }
    
    /**
     * Loads a model (and its dependents) if it does not exist in the list of loaded models.
     * 
     * @param modelMap          a map of the models to be loaded
     * @param loadedModels      the list of models already loaded
     * @param model             the model to try and load
     */
    private void loadModel(Map<String, M2Model> modelMap, List<String> loadedModels, M2Model model)
    {
        String modelName = model.getName();
        if (loadedModels.contains(modelName) == false)
        {
            for (M2Namespace importNamespace : model.getImports())
            {
                M2Model importedModel = modelMap.get(importNamespace.getUri());
                if (importedModel != null)
                {
                    // Ensure that the imported model is loaded first
                    loadModel(modelMap, loadedModels, importedModel);
                }
                // else we can assume that the imported model is already loaded, if this not the case then
                //      an error will be raised during compilation
            }
            
            try
            {
                dictionaryDAO.putModel(model);
                loadedModels.add(modelName);
            }
            catch (AlfrescoRuntimeException e)
            {
                // note: skip with warning - to allow server to start, and hence allow the possibility of fixing the broken model(s)
                logger.warn("Failed to load model '" + modelName + "' : " + e);
            }
        }        
    }

    /**
     * Create a M2Model from a dictionary model node
     * 
     * @param nodeRef   the dictionary model node reference
     * @return          the M2Model
     */
    public M2Model createM2Model(NodeRef nodeRef)
    {
        M2Model model = null;
        ContentReader contentReader = this.contentService.getReader(nodeRef, ContentModel.PROP_CONTENT);
        if (contentReader != null)
        {
            model = M2Model.createModel(contentReader.getContentInputStream());
        }
        // TODO should we inactivate the model node and put the error somewhere??
        return model;
    }
    
    @Override
    protected void onBootstrap(ApplicationEvent event)
    {
        register();
    }

    @Override
    protected void onShutdown(ApplicationEvent event)
    {
        // NOOP
    }
    
    public void onEnableTenant()
    {
        init(); // will be called in context of tenant
    }
    
    public void onDisableTenant()
    {
        destroy(); // will be called in context of tenant
    }
    
    /**
     * Register
     */
    public void register()
    {
        dictionaryDAO.destroy(); // deployer - force reload on next get
        
    	// register with Dictionary Service to allow (re-)init
    	dictionaryDAO.register(this);
    	
        // register with Message Service to allow (re-)init
        messageService.register(this);
        
        if (tenantAdminService.isEnabled())
        {
            // register dictionary repository bootstrap
            tenantAdminService.register(this);
            
            // register repository message (I18N) service
            tenantAdminService.register(messageService);
        }
    }
    
    /**
     * Unregister
     */
    protected void unregister()
    {
        if (tenantAdminService.isEnabled())
        {
            // register dictionary repository bootstrap
            tenantAdminService.unregister(this);
            
            // register repository message (I18N) service
            tenantAdminService.unregister(messageService);
        }
    }
    
    protected NodeRef resolveQNameFolderPath(NodeRef rootNodeRef, String[] pathPrefixQNameStrings)
    {
        if (pathPrefixQNameStrings.length == 0)
        {
            throw new IllegalArgumentException("Path array is empty");
        }
        // walk the folder tree
        NodeRef parentNodeRef = rootNodeRef;
        for (int i = 0; i < pathPrefixQNameStrings.length; i++)
        {
            String pathPrefixQNameString = pathPrefixQNameStrings[i];
            
            QName pathQName = null;
            if (tenantAdminService.isEnabled())
            {
                String[] parts = QName.splitPrefixedQName(pathPrefixQNameString);
                if ((parts.length == 2) && (parts[0].equals(NamespaceService.APP_MODEL_PREFIX)))
                {
                    String pathUriQNameString = new StringBuilder(64).
                        append(QName.NAMESPACE_BEGIN).
                        append(NamespaceService.APP_MODEL_1_0_URI).
                        append(QName.NAMESPACE_END).
                        append(parts[1]).toString();
                    
                    pathQName = QName.createQName(pathUriQNameString);
                }
                else
                {
                    pathQName = QName.createQName(pathPrefixQNameString, namespaceService);
                }
            }
            else
            {
                pathQName = QName.createQName(pathPrefixQNameString, namespaceService);
            }
            
            List<ChildAssociationRef> childAssocRefs = nodeService.getChildAssocs(parentNodeRef, RegexQNamePattern.MATCH_ALL, pathQName);
            if (childAssocRefs.size() != 1)
            {
                return null;
            }
            parentNodeRef = childAssocRefs.get(0).getChildRef();
        }
        return parentNodeRef;
    }
}
