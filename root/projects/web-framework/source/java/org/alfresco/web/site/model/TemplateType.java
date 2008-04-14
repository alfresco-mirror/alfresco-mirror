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

import org.dom4j.Document;

/**
 * @author muzquiano
 */
public class TemplateType extends ModelObject
{
    public TemplateType(Document document)
    {
        super(document);
    }

    @Override
    public String toString()
    {
        return "TemplateType Instance: " + getId() + ", " + toXML();
    }

    public String getURI()
    {
        return getProperty("uri");
    }

    public void setURI(String uri)
    {
        setProperty("uri", uri);
    }

    public void setRenderer(String renderer)
    {
        setProperty("renderer", renderer);
    }

    public String getRenderer()
    {
        return getProperty("renderer");
    }

    public void setRendererType(String rendererType)
    {
        setProperty("renderer-type", rendererType);
    }

    public String getRendererType()
    {
        return getProperty("renderer-type");
    }

}
