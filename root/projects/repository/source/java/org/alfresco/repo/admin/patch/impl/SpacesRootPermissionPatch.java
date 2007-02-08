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
package org.alfresco.repo.admin.patch.impl;

import org.alfresco.i18n.I18NUtil;
import org.alfresco.repo.admin.patch.AbstractPatch;
import org.alfresco.repo.importer.ImporterBootstrap;
import org.alfresco.service.cmr.repository.NodeRef;
import org.alfresco.service.cmr.repository.NodeService;
import org.alfresco.service.cmr.security.PermissionService;

/**
 * Change Spaces Root Node permission from Guest to Read
 * 
 * Guest (now Consumer) permission is not valid for sys:store_root type.
 */
public class SpacesRootPermissionPatch extends AbstractPatch
{
    private static final String MSG_SUCCESS = "patch.spacesRootPermission.result";

    private ImporterBootstrap spacesBootstrap;
    private NodeService nodeService;
    private PermissionService permissionService;
    
    
    public SpacesRootPermissionPatch()
    {
        super();
    }

    public void setSpacesBootstrap(ImporterBootstrap spacesBootstrap)
    {
        this.spacesBootstrap = spacesBootstrap;
    }

    public void setNodeService(NodeService nodeService)
    {
        this.nodeService = nodeService;
    }

    public void setPermissionService(PermissionService permissionService)
    {
        this.permissionService = permissionService;
    }
    
    
    @Override
    protected String applyInternal() throws Exception
    {
        NodeRef rootNodeRef = nodeService.getRootNode(spacesBootstrap.getStoreRef());
        permissionService.deletePermission(rootNodeRef, PermissionService.ALL_AUTHORITIES, PermissionService.CONSUMER);
        permissionService.setPermission(rootNodeRef, PermissionService.ALL_AUTHORITIES, PermissionService.READ, true);

        return I18NUtil.getMessage(MSG_SUCCESS);
    }

}
