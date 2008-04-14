/*
 * Copyright (C) 2005-2008 Alfresco Software Limited.
 * 
 * This program is free software; you can redistribute it and/or modify it under
 * the terms of the GNU General Public License as published by the Free Software
 * Foundation; either version 2 of the License, or (at your option) any later
 * version.
 * 
 * This program is distributed in the hope that it will be useful, but WITHOUT
 * ANY WARRANTY; without even the implied warranty of MERCHANTABILITY or FITNESS
 * FOR A PARTICULAR PURPOSE. See the GNU General Public License for more
 * details.
 * 
 * You should have received a copy of the GNU General Public License along with
 * this program; if not, write to the Free Software Foundation, Inc., 51
 * Franklin Street, Fifth Floor, Boston, MA 02110-1301, USA.
 * 
 * As a special exception to the terms and conditions of version 2.0 of the GPL,
 * you may redistribute this Program in connection with Free/Libre and Open
 * Source Software ("FLOSS") applications as described in Alfresco's FLOSS
 * exception. You should have recieved a copy of the text describing the FLOSS
 * exception, and it is also available here:
 * http://www.alfresco.com/legal/licensing"
 */
package org.alfresco.web.site.model;

import org.alfresco.web.site.RequestContext;
import org.dom4j.Document;

/**
 * @author muzquiano
 */
public class Component extends ModelObject
{
    public Component(Document document)
    {
        super(document);
    }

    public String getRegionId()
    {
        return getProperty("region-id");
    }

    public void setRegionId(String regionId)
    {
        setProperty("region-id", regionId);
    }

    public String getSourceId()
    {
        return getProperty("source-id");
    }

    public void setSourceId(String sourceId)
    {
        setProperty("source-id", sourceId);
    }

    public String getScope()
    {
        return getProperty("scope");
    }

    public void setScope(String scope)
    {
        setProperty("scope", scope);
    }

    public String getComponentTypeId()
    {
        return getProperty("component-type-id");
    }

    public void setComponentTypeId(String componentTypeId)
    {
        setProperty("component-type-id", componentTypeId);
    }

    // Helpers

    // TODO
    public ModelObject getSourceObject(RequestContext context)
    {
        // either 'global', template or page
        return context.getModelManager().loadObject(context, getSourceId());
    }

    public ComponentType getComponentType(RequestContext context)
    {
        // either 'global', template or page
        return context.getModelManager().loadComponentType(context,
                getComponentTypeId());
    }

}
