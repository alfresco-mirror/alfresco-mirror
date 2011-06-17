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
package org.alfresco.module.org_alfresco_module_dod5015.disposition;

import java.util.ArrayList;
import java.util.List;

import org.alfresco.service.namespace.QName;

/**
 * Spring bean to allow configuration of properties used for calculating
 * dates in disposition schedules.
 * 
 * @author Gavin Cornwell
 */
public class DispositionPeriodProperties
{
    public static final String BEAN_NAME = "DispositionPeriodProperties";
    
    private List<QName> periodProperties;
    
    public void setPropertyList(List<String> propertyList)
    {
        periodProperties = new ArrayList<QName>(propertyList.size());
        for (String property : propertyList)
        {
            periodProperties.add(QName.createQName(property));
        }
    }
    
    public List<QName> getPeriodProperties()
    {
        return periodProperties;
    }
}