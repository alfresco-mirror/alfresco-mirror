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
package org.alfresco.service.cmr.search;

import org.alfresco.service.cmr.dictionary.PropertyDefinition;
import org.alfresco.service.cmr.dictionary.DataTypeDefinition;

public interface QueryParameterDefinition extends NamedQueryParameterDefinition
{   
    /**
     * This parameter may apply to a well known property type.
     * 
     * May be null
     * 
     * @return
     */
    public PropertyDefinition getPropertyDefinition();
    
    /**
     * Get the property type definition for this parameter.
     * It could come from the property type definition if there is one
     * 
     * Not null
     * 
     * @return
     */
    public DataTypeDefinition getDataTypeDefinition();
    
    /**
     * Get the default value for this parameter.
     * 
     * @return
     */
    public String getDefault();
    
    /**
     * Has this parameter got a default value?
     * 
     * @return
     */
    public boolean hasDefaultValue();
}
