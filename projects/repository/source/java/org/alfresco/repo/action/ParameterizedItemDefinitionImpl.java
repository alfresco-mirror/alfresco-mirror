/*
 * Copyright (C) 2005 Alfresco, Inc.
 *
 * Licensed under the GNU Lesser General Public License as
 * published by the Free Software Foundation; either version
 * 2.1 of the License, or (at your option) any later version.
 * You may obtain a copy of the License at
 *
 *     http://www.gnu.org/licenses/lgpl.txt
 *
 * Unless required by applicable law or agreed to in writing,
 * software distributed under the License is distributed on an
 * "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND,
 * either express or implied. See the License for the specific
 * language governing permissions and limitations under the
 * License.
 */
package org.alfresco.repo.action;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;

import org.alfresco.service.cmr.action.ParameterDefinition;
import org.alfresco.service.cmr.action.ParameterizedItemDefinition;
import org.alfresco.service.cmr.rule.RuleServiceException;

/**
 * Rule item implementation class
 * 
 * @author Roy Wetherall
 */
public abstract class ParameterizedItemDefinitionImpl implements ParameterizedItemDefinition, Serializable
{
    /**
     * The name of the rule item
     */
    private String name;
    
    /**
     * The title of the rule item
     */
    private String title;
    
    /**
     * The description of the rule item
     */
    private String description;
        
    /**
     * Indicates whether adHocProperties are allowed
     */
    private boolean adhocPropertiesAllowed = false;
    
    /**
     * The list of parameters associated with the rule item
     */
    private List<ParameterDefinition> parameterDefinitions = new ArrayList<ParameterDefinition>();
    
    /**
     * A map of the parameter definitions by name
     */
    private Map<String, ParameterDefinition> paramDefinitionsByName;

    /**
     * Error messages
     */
    private static final String ERR_NAME_DUPLICATION = "The names " +
            "given to parameter definitions must be unique within the " +
            "scope of the rule item definition.";

    /**
     * Constructor
     * 
     * @param name                  the name 
     */
    public ParameterizedItemDefinitionImpl(String name)
    {
        this.name = name;        
    }

    /**
     * @see org.alfresco.service.cmr.action.ParameterizedItemDefinition#getName()
     */
    public String getName()
    {
        return this.name;
    }

    /**
     * Set the title of the rule item
     * 
     * @param title  the title
     */
    public void setTitle(String title)
    {
        this.title = title;
    }
    
    /**
     * @see org.alfresco.service.cmr.action.ParameterizedItemDefinition#getTitle()
     */
    public String getTitle()
    {
        return this.title;
    }

    /**
     * Set the description of the rule item.
     * 
     * @param description  the description
     */
    public void setDescription(String description)
    {
        this.description = description;
    }
    
    /**
     * @see org.alfresco.service.cmr.action.ParameterizedItemDefinition#getDescription()
     */
    public String getDescription()
    {
        return this.description;
    }

    /**
     * @see org.alfresco.service.cmr.action.ParameterizedItemDefinition#getAdhocPropertiesAllowed()
     */
    public boolean getAdhocPropertiesAllowed()
    {
    	return this.adhocPropertiesAllowed;
    }
    
    /**
     * Set whether adhoc properties are allowed
     * 
     * @param adhocPropertiesAllowed	true is adhoc properties are allowed, false otherwise
     */
    public void setAdhocPropertiesAllowed(boolean adhocPropertiesAllowed)
	{
		this.adhocPropertiesAllowed = adhocPropertiesAllowed;
	}
    
    /**
     * Set the parameter definitions for the rule item
     * 
     * @param parameterDefinitions  the parameter definitions
     */
    public void setParameterDefinitions(
            List<ParameterDefinition> parameterDefinitions)
    {
        if (hasDuplicateNames(parameterDefinitions) == true)
        {
            throw new RuleServiceException(ERR_NAME_DUPLICATION);
        }
        
        this.parameterDefinitions = parameterDefinitions;
        
        // Create a map of the definitions to use for subsequent calls
        this.paramDefinitionsByName = new HashMap<String, ParameterDefinition>(this.parameterDefinitions.size());
        for (ParameterDefinition definition : this.parameterDefinitions)
        {
            this.paramDefinitionsByName.put(definition.getName(), definition);
        }
    }
    
    /**
     * Determines whether the list of parameter defintions contains duplicate
     * names of not.
     * 
     * @param parameterDefinitions  a list of parmeter definitions
     * @return                      true if there are name duplications, false
     *                              otherwise
     */
    private boolean hasDuplicateNames(List<ParameterDefinition> parameterDefinitions)
    {
        boolean result = false;
        if (parameterDefinitions != null)
        {
            HashSet<String> temp = new HashSet<String>(parameterDefinitions.size());
            for (ParameterDefinition definition : parameterDefinitions)
            {
                temp.add(definition.getName());
            }
            result = (parameterDefinitions.size() != temp.size());
        }
        return result;
    }
    
    /**
     * @see org.alfresco.service.cmr.action.ParameterizedItemDefinition#getParameterDefinitions()
     */
    public List<ParameterDefinition> getParameterDefinitions()
    {
        return this.parameterDefinitions;
    }
    
    /**
     * @see org.alfresco.service.cmr.action.ParameterizedItemDefinition#getParameterDefintion(java.lang.String)
     */
    public ParameterDefinition getParameterDefintion(String name)
    {
        ParameterDefinition result = null;
        if (paramDefinitionsByName != null)
        {
            result = this.paramDefinitionsByName.get(name);
        }
        return result;
    }
}
