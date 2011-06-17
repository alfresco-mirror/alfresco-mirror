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
package org.alfresco.service.cmr.publishing;

import java.util.Collection;

import org.alfresco.service.cmr.repository.NodeRef;

/**
 * An extension of the {@link PublishingPackage} interface that permits changes to be made
 * @author Brian
 *
 */
public interface MutablePublishingPackage extends PublishingPackage
{
    void addNodesToUnpublish(NodeRef... nodesToRemove);

    void addNodesToUnpublish(Collection<NodeRef> nodesToRemove);

    void addNodesToPublish(NodeRef... nodesToPublish);
    
    void addNodesToPublish(Collection<NodeRef> nodesToPublish);
}