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
package org.alfresco.repo.publishing.youtube;

import java.io.Serializable;
import java.util.Collections;
import java.util.Map;
import java.util.Set;

import org.alfresco.repo.publishing.AbstractChannelType;
import org.alfresco.service.cmr.action.Action;
import org.alfresco.service.cmr.action.ActionService;
import org.alfresco.service.cmr.repository.NodeRef;
import org.alfresco.service.cmr.repository.NodeService;
import org.alfresco.service.namespace.QName;

public class YouTubeChannelType extends AbstractChannelType
{
    public final static String ID = "youtube";
    private NodeService nodeService;
    private ActionService actionService;
    
    public void setNodeService(NodeService nodeService)
    {
        this.nodeService = nodeService;
    }

    public void setActionService(ActionService actionService)
    {
        this.actionService = actionService;
    }

    @Override
    public boolean canPublish()
    {
        return true;
    }

    @Override
    public boolean canPublishStatusUpdates()
    {
        return false;
    }

    @Override
    public boolean canUnpublish()
    {
        return true;
    }

    @Override
    public QName getChannelNodeType()
    {
        return YouTubePublishingModel.TYPE_DELIVERY_CHANNEL;
    }

    @Override
    public String getId()
    {
        return ID;
    }

    @Override
    public Set<QName> getSupportedContentTypes()
    {
        return Collections.emptySet();
    }

    @Override
    public Set<String> getSupportedMimetypes()
    {
        return Collections.emptySet();
    }

    @Override
    public void publish(NodeRef nodeToPublish, Map<QName, Serializable> properties)
    {
        Action youtubePublishAction = actionService.createAction(YouTubePublishAction.NAME);
        actionService.executeAction(youtubePublishAction, nodeToPublish);
    }

    @Override
    public void unpublish(NodeRef nodeToUnpublish, Map<QName, Serializable> properties)
    {
        Action youtubeUnpublishAction = actionService.createAction(YouTubeUnpublishAction.NAME);
        actionService.executeAction(youtubeUnpublishAction, nodeToUnpublish);
    }

    @Override
    public void updateStatus(String status, Map<QName, Serializable> properties)
    {
        throw new UnsupportedOperationException();
    }

    @Override
    public String getNodeUrl(NodeRef node)
    {
        String url = null;
        if (node != null && nodeService.exists(node) && nodeService.hasAspect(node, YouTubePublishingModel.ASPECT_ASSET))
        {
            url = (String)nodeService.getProperty(node, YouTubePublishingModel.PROP_PLAYER_URL);
        }
        return url;
    }
}