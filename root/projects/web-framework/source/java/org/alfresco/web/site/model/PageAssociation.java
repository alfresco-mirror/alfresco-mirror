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
public class PageAssociation extends ModelObject
{
    public static String TYPE_NAME = "page-association";
    public static String CHILD_ASSOCIATION_TYPE_ID = "child";
    public static String PROP_SOURCE_ID = "source-id";
    public static String PROP_DEST_ID = "dest-id";
    public static String PROP_ASSOC_TYPE = "assoc-type";
    public static String PROP_ORDER_ID = "order-id";
    

    public PageAssociation(Document document)
    {
        super(document);
    }

    @Override
    public String toString()
    {
        return "Page Association: " + getId() + ", " + toXML();
    }

    public String getSourceId()
    {
        return getProperty(PROP_SOURCE_ID);
    }

    public void setSourceId(String sourceId)
    {
        setProperty(PROP_SOURCE_ID, sourceId);
    }

    public String getDestId()
    {
        return getProperty(PROP_DEST_ID);
    }

    public void setDestId(String destId)
    {
        setProperty(PROP_DEST_ID, destId);
    }

    public String getAssociationType()
    {
        return getProperty(PROP_ASSOC_TYPE);
    }

    public void setAssociationType(String associationType)
    {
        setProperty(PROP_ASSOC_TYPE, associationType);
    }

    public String getOrderId()
    {
        return getProperty(PROP_ORDER_ID);
    }

    public void setOrderId(String orderId)
    {
        setProperty(PROP_ORDER_ID, orderId);
    }

    // Helpers

    public ModelObject getSourceObject(RequestContext context)
    {
        // either 'global', template or page
        return context.getModel().loadObject(context, getSourceId());
    }

    public ModelObject getDestObject(RequestContext context)
    {
        // either 'global', template or page
        return context.getModel().loadObject(context, getDestId());
    }

    public Page getSourcePage(RequestContext context)
    {
        return (Page) getSourceObject(context);
    }

    public Page getDestPage(RequestContext context)
    {
        return (Page) getDestObject(context);
    }
    
    public String getTypeName() 
    {
        return TYPE_NAME;
    }
    
}
