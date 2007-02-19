/*
 * Copyright (C) 2005-2007 Alfresco Software Limited.
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
 * Foundation, Inc., 51 Franklin Street, Fifth Floor, Boston, MA 02110-1301, USA.

 * As a special exception to the terms and conditions of version 2.0 of 
 * the GPL, you may redistribute this Program in connection with Free/Libre 
 * and Open Source Software ("FLOSS") applications as described in Alfresco's 
 * FLOSS exception.  You should have recieved a copy of the text describing 
 * the FLOSS exception, and it is also available here: 
 * http://www.alfresco.com/legal/licensing"
 */
package org.alfresco.repo.audit.model;

import org.alfresco.repo.audit.AuditModel;
import org.alfresco.service.namespace.NamespacePrefixResolver;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.dom4j.Attribute;
import org.dom4j.Element;

/**
 * The base class for filtering.
 * 
 * This supports negating the filter, ie NOT.
 * 
 * @author Andy Hind
 */
public abstract class AbstractFilter implements XMLModelElement
{
    private static Log s_logger = LogFactory.getLog(AbstractFilter.class);
    
    private boolean invert = false;

    public AbstractFilter()
    {
        super();
    }

    public static AbstractFilter createFilter(Element filterElement, NamespacePrefixResolver namespacePrefixResolver)
    {
        AbstractFilter filter;

        Attribute typeAttribute = filterElement.attribute(AuditModel.AT_TYPE);
        if (typeAttribute == null)
        {
            throw new AuditModelException("A filter must specify it concrete type using xsi:type");
        }
        if (typeAttribute.getStringValue().endsWith("FilterSet"))
        {
            filter = new FilterSet();
        }
        else if (typeAttribute.getStringValue().endsWith("KeyFilter"))
        {
            filter = new KeyFilter();
        }
        else if (typeAttribute.getStringValue().endsWith("ParameterFilter"))
        {
            filter = new ParameterFilter();
        }
        else
        {
            throw new AuditModelException(
                    "Invalid filter type. It must be one of: FilterSet, KeyFilter, ParameterFilter ");
        }

        filter.configure(filterElement, namespacePrefixResolver);
        return filter;
    }

    public void configure(Element element, NamespacePrefixResolver namespacePrefixResolver)
    {
        Attribute invertAttribute = element.attribute(AuditModel.AT_INVERT);
        if (invertAttribute != null)
        {
            invert = Boolean.valueOf(invertAttribute.getStringValue()).booleanValue();
        }
        else
        {
            invert = false;
        }
    }

    /* package */boolean isInvert()
    {
        return invert;
    }
}
