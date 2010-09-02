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
package org.alfresco.module.org_alfresco_module_wcmquickstart.model;

import java.io.Serializable;
import java.util.Date;
import java.util.HashMap;
import java.util.Map;

import org.alfresco.module.org_alfresco_module_wcmquickstart.publish.PublishService;
import org.alfresco.module.org_alfresco_module_wcmquickstart.rendition.RenditionHelper;
import org.alfresco.repo.content.ContentServicePolicies;
import org.alfresco.repo.copy.CopyBehaviourCallback;
import org.alfresco.repo.copy.CopyDetails;
import org.alfresco.repo.copy.CopyServicePolicies;
import org.alfresco.repo.copy.DefaultCopyBehaviourCallback;
import org.alfresco.repo.copy.CopyServicePolicies.OnCopyNodePolicy;
import org.alfresco.repo.node.NodeServicePolicies;
import org.alfresco.repo.policy.BehaviourFilter;
import org.alfresco.repo.policy.JavaBehaviour;
import org.alfresco.repo.policy.PolicyComponent;
import org.alfresco.repo.policy.Behaviour.NotificationFrequency;
import org.alfresco.service.cmr.repository.NodeRef;
import org.alfresco.service.cmr.repository.NodeService;
import org.alfresco.service.namespace.QName;

/**
 * ws:webasset aspect behaviours.
 * 
 * @author Brian
 */
public class WebAssetAspect implements WebSiteModel, 
									   CopyServicePolicies.OnCopyNodePolicy,
									   ContentServicePolicies.OnContentUpdatePolicy,
									   NodeServicePolicies.OnAddAspectPolicy
{
	/** Policy component */
    private PolicyComponent policyComponent;
    
    /** Node service */
    private NodeService nodeService;
    
    /** Behaviour filter */
    private BehaviourFilter behaviourFilter;
    
    /** Publish service */
    private PublishService publishService;
    
    /** Rendition helper */
    private RenditionHelper renditionHelper;
    
    /**
     * Set the policy component
     * 
     * @param policyComponent
     *            policy component
     */
    public void setPolicyComponent(PolicyComponent policyComponent)
    {
        this.policyComponent = policyComponent;
    }

    /**
     * Set the node service
     * @param nodeService	node service
     */
    public void setNodeService(NodeService nodeService)
    {
	    this.nodeService = nodeService;
    }
    
    /**
     * Set rendition helper
     * @param renditionHelper	rendition helper
     */
    public void setRenditionHelper(RenditionHelper renditionHelper)
    {
	    this.renditionHelper = renditionHelper;
    }
    
    /**
     * Set the behaviour filter
     * @param behaviourFilter	behaviour filter
     */
    public void setBehaviourFilter(BehaviourFilter behaviourFilter)
    {
        this.behaviourFilter = behaviourFilter;
    }

    /**
     * Set the publish service
     * @param publishService	publish service
     */
    public void setPublishService(PublishService publishService)
    {
        this.publishService = publishService;
    }
    
    /**
     * Init method. Binds model behaviours to policies.
     */
    public void init()
    {
        policyComponent.bindClassBehaviour(OnCopyNodePolicy.QNAME, ASPECT_WEBASSET, new JavaBehaviour(
                this, "getCopyCallback"));
        policyComponent.bindClassBehaviour(ContentServicePolicies.OnContentUpdatePolicy.QNAME, 
        								   ASPECT_WEBASSET, 
        								   new JavaBehaviour(this, "onContentUpdate", NotificationFrequency.TRANSACTION_COMMIT));
        policyComponent.bindClassBehaviour(NodeServicePolicies.OnAddAspectPolicy.QNAME, 
        								   ASPECT_WEBASSET, 
        								   new JavaBehaviour(this, "onAddAspect", NotificationFrequency.TRANSACTION_COMMIT));
        policyComponent.bindClassBehaviour(NodeServicePolicies.OnUpdatePropertiesPolicy.QNAME, ASPECT_WEBASSET,
                new JavaBehaviour(this, "onUpdatePropertiesEachEvent", NotificationFrequency.EVERY_EVENT));
        policyComponent.bindClassBehaviour(NodeServicePolicies.BeforeDeleteNodePolicy.QNAME, ASPECT_WEBASSET,
                new JavaBehaviour(this, "beforeDeleteNodeEachEvent", NotificationFrequency.EVERY_EVENT));
    }

    /**
     * Before delete, fired on each event
     * @param nodeRef	node reference of deleted node
     */
    public void beforeDeleteNodeEachEvent(NodeRef nodeRef)
    {
        publishService.enqueueRemovedNodes(nodeRef);
    }
    
    /**
     * @see org.alfresco.repo.copy.CopyServicePolicies.OnCopyNodePolicy#getCopyCallback(org.alfresco.service.namespace.QName, org.alfresco.repo.copy.CopyDetails)
     */
    @Override
    public CopyBehaviourCallback getCopyCallback(QName classRef, CopyDetails copyDetails)
    {
        return WebAssetAspectCopyBehaviourCallback.INSTANCE;
    }

    /**
     * 
     * @param nodeRef
     * @param before
     * @param after
     */
    public void onUpdatePropertiesEachEvent(
            NodeRef nodeRef,
            Map<QName, Serializable> before,
            Map<QName, Serializable> after)
    {
        //If the "published" flag is changing to true, then set the published time to "now".
        Boolean afterPublished = (Boolean)after.get(PROP_PUBLISHED);
        Boolean beforePublished = (Boolean)before.get(PROP_PUBLISHED);
        if (afterPublished != null && !afterPublished.equals(beforePublished) && afterPublished)
        {
            behaviourFilter.disableBehaviour(nodeRef, ASPECT_WEBASSET);
            try
            {
                nodeService.setProperty(nodeRef, PROP_PUBLISHED_TIME, new Date());
            }
            finally
            {
                behaviourFilter.enableBehaviour(nodeRef, ASPECT_WEBASSET);
            }
        }
    }
        
    /**
     * WebAsset aspect copy behaviour callback class
     */
    private static class WebAssetAspectCopyBehaviourCallback extends DefaultCopyBehaviourCallback
    {
        private static final CopyBehaviourCallback INSTANCE = new WebAssetAspectCopyBehaviourCallback();

        @Override
        public Map<QName, Serializable> getCopyProperties(QName classQName, CopyDetails copyDetails,
                Map<QName, Serializable> properties)
        {
            Map<QName, Serializable> propertiesToCopy = new HashMap<QName, Serializable>(properties);
            //We don't want to copy across the original node's record of the website sections it's in.
            //This property will be calculated afresh on the copy
            propertiesToCopy.remove(PROP_PARENT_SECTIONS);
            return propertiesToCopy;
        }
    }

    /**
     * @see org.alfresco.repo.content.ContentServicePolicies.OnContentUpdatePolicy#onContentUpdate(org.alfresco.service.cmr.repository.NodeRef, boolean)
     */
	@Override
    public void onContentUpdate(NodeRef nodeRef, boolean newContent)
    {
		if (newContent == true)
		{					
			renditionHelper.createRenditions(nodeRef);
		}
    }

	/**
	 * @see org.alfresco.repo.node.NodeServicePolicies.OnAddAspectPolicy#onAddAspect(org.alfresco.service.cmr.repository.NodeRef, org.alfresco.service.namespace.QName)
	 */
	@Override
    public void onAddAspect(NodeRef nodeRef, QName aspectTypeQName)
    {
		renditionHelper.createRenditions(nodeRef);
    }
}