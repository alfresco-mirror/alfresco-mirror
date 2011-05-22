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

package org.alfresco.repo.workflow.activiti.properties;

import java.io.Serializable;
import java.util.Date;

import org.activiti.engine.delegate.DelegateTask;
import org.activiti.engine.task.Task;
import org.alfresco.repo.workflow.WorkflowModel;
import org.alfresco.repo.workflow.activiti.ActivitiTaskPropertyHandler;
import org.alfresco.service.cmr.dictionary.TypeDefinition;
import org.alfresco.service.namespace.QName;

/**
 * @since 3.5
 * @author Frederik Heremans
 *
 */
public class ActivitiDueDatePropertyHandler extends ActivitiTaskPropertyHandler
{

    /**
    * {@inheritDoc}
    */
    
    @Override
    protected Object handleTaskProperty(Task task, TypeDefinition type, QName key, Serializable value)
    {
        checkType(key, value, Date.class);
        task.setDueDate((Date) value);
        return DO_NOT_ADD;
    }

    /**
    * {@inheritDoc}
    */
    @Override
    protected Object handleDelegateTaskProperty(DelegateTask task, TypeDefinition type, QName key, Serializable value)
    {
        checkType(key, value, Date.class);
        task.setDueDate((Date) value);
        return DO_NOT_ADD;
    }
    
    /**
    * {@inheritDoc}
    */
    
    @Override
    protected QName getKey()
    {
        return WorkflowModel.PROP_DUE_DATE;
    }

}