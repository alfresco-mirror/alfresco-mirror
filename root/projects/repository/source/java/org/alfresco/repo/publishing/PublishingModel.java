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

package org.alfresco.repo.publishing;

import org.alfresco.service.namespace.QName;

/**
 * @author Nick Smith
 * @since 4.0
 *
 */
public interface PublishingModel
{
    public static final String NAMESPACE = "http://www.alfresco.org/model/publishing/1.0";
    public static final String PREFIX = "pub";
    
    public static final String WF_NAMESPACE = "http://www.alfresco.org/model/publishingworkflow/1.0";
    public static final String WF_PREFIX = "pubwf";
    
    public static final QName TYPE_DELIVERY_CHANNEL = QName.createQName(NAMESPACE, "DeliveryChannel");
    public static final QName TYPE_DELIVERY_SERVER = QName.createQName(NAMESPACE, "DeliveryServer");
    public static final QName TYPE_ENVIRONMENT= QName.createQName(NAMESPACE, "Environment");
    public static final QName TYPE_PUBLISHING_QUEUE = QName.createQName(NAMESPACE, "PublishingQueue");
    public static final QName TYPE_CHANNEL_CONTAINER = QName.createQName(NAMESPACE, "SiteChannelContainer");
    public static final QName TYPE_PUBLISHING_EVENT = QName.createQName(NAMESPACE, "PublishingEvent");
    
    public static final QName ASPECT_CONTENT_ROOT = QName.createQName(NAMESPACE, "ContentRoot");
    public static final QName ASPECT_CHANNEL_INFO= QName.createQName(NAMESPACE, "channelInfo");
    public static final QName ASPECT_PUBLISHED = QName.createQName(NAMESPACE, "published");

    public static final QName PROP_CHANNEL = QName.createQName(NAMESPACE, "channel");
    public static final QName PROP_CHANNEL_TYPE = QName.createQName(NAMESPACE, "channelType");
    public static final QName PROP_CHANNEL_TYPE_ID = QName.createQName(NAMESPACE, "channelTypeId");
    public static final QName PROP_PUBLISHING_EVENT_STATUS= QName.createQName(NAMESPACE, "publishingEventStatus");
    public static final QName PROP_PUBLISHING_EVENT_TIME = QName.createQName(NAMESPACE, "publishingEventTime");
    public static final QName PROP_PUBLISHING_EVENT_TIME_ZONE = QName.createQName(NAMESPACE, "publishingEventTimeZone");
    public static final QName PROP_PUBLISHING_EVENT_COMMENT = QName.createQName(NAMESPACE, "publishingEventComment");
    public static final QName PROP_PUBLISHING_EVENT_CHANNEL= QName.createQName(NAMESPACE, "publishingEventChannel");
    public static final QName PROP_PUBLISHING_EVENT_WORKFLOW_ID= QName.createQName(NAMESPACE, "publishingEventWorkflowId");
    public static final QName PROP_PUBLISHING_EVENT_PAYLOAD = QName.createQName(NAMESPACE, "publishingEventPayload");
    public static final QName PROP_PUBLISHING_EVENT_NODES_TO_PUBLISH = QName.createQName(NAMESPACE, "publishingEventNodesToPublish");
    public static final QName PROP_PUBLISHING_EVENT_NODES_TO_UNPUBLISH = QName.createQName(NAMESPACE, "publishingEventNodesToUnpublish");
    public static final QName PROP_STATUS_UPDATE_CHANNEL_NAMES = QName.createQName(NAMESPACE, "statusUpdateChannelNames");
    public static final QName PROP_STATUS_UPDATE_NODE_REF = QName.createQName(NAMESPACE, "statusUpdateNodeRef");
    public static final QName PROP_STATUS_UPDATE_MESSAGE = QName.createQName(NAMESPACE, "statusUpdateMessage");

    public static final String PROPVAL_PUBLISHING_EVENT_STATUS_SCHEDULED = "SCHEDULED";
    public static final String PROPVAL_PUBLISHING_EVENT_STATUS_IN_PROGRESS = "IN_PROGRESS";
    public static final String PROPVAL_PUBLISHING_EVENT_STATUS_CANCEL_REQUESTED = "CANCEL_REQUESTED";
    public static final String PROPVAL_PUBLISHING_EVENT_STATUS_COMPLETED = "COMPLETED";
    public static final String PROPVAL_PUBLISHING_EVENT_STATUS_FAILED = "FAILED";

    public static final QName ASSOC_PUBLISHING_QUEUE = QName.createQName(NAMESPACE, "publishingQueueAssoc");
    public static final QName ASSOC_PUBLISHING_EVENT = QName.createQName(NAMESPACE, "publishingEventAssoc");
    public static final QName ASSOC_SOURCE = QName.createQName(NAMESPACE, "source");
    public static final QName ASSOC_LAST_PUBLISHING_EVENT= QName.createQName(NAMESPACE, "lastPublishingEvent");

    // Workflow Properties
    public static final QName PROP_WF_PUBLISHING_EVENT= QName.createQName(WF_NAMESPACE, "publishingEvent");
    public static final QName PROP_WF_SCHEDULED_PUBLISH_DATE= QName.createQName(WF_NAMESPACE, "scheduledPublishDate");
    
}