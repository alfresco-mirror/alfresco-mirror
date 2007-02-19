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
package org.alfresco.repo.jscript;

import java.util.Set;

import org.alfresco.model.ContentModel;
import org.alfresco.repo.security.authority.AuthorityDAO;
import org.alfresco.service.ServiceRegistry;
import org.alfresco.service.cmr.repository.NodeRef;
import org.alfresco.service.cmr.security.AuthorityService;
import org.alfresco.service.cmr.security.AuthorityType;
import org.alfresco.service.cmr.security.PersonService;
import org.mozilla.javascript.Scriptable;

/**
 * Scripted People service for describing and executing actions against People & Groups.
 * 
 * @author davidc
 */
public final class People extends BaseScriptImplementation implements Scopeable
{
    /** Repository Service Registry */
    private ServiceRegistry services;
    private AuthorityDAO authorityDAO;

    /** Root scope for this object */
    private Scriptable scope;

    /**
     * Set the service registry
     * 
     * @param serviceRegistry	the service registry
     */
    public void setServiceRegistry(ServiceRegistry serviceRegistry)
    {
    	this.services = serviceRegistry;
    }

    /**
     * Set the authority DAO
     *
     * @param authorityDAO  authority dao
     */
    public void setAuthorityDAO(AuthorityDAO authorityDAO)
    {
        this.authorityDAO = authorityDAO;
    }
    
    /**
     * @see org.alfresco.repo.jscript.Scopeable#setScope(org.mozilla.javascript.Scriptable)
     */
    public void setScope(Scriptable scope)
    {
        this.scope = scope;
    }
    
    /**
     * Gets the Person given the username
     * 
     * @param username  the username of the person to get
     * @return the person node (type cm:person) or null if no such person exists 
     */
    public Node getPerson(String username)
    {
        Node person = null;
        PersonService personService = services.getPersonService();
        if (personService.personExists(username))
        {
            NodeRef personRef = personService.getPerson(username);
            person = new Node(personRef, services, scope);
        }
        return person;
    }

    /**
     * Gets the Group given the group name
     * 
     * @param groupName  name of group to get
     * @return  the group node (type usr:authorityContainer) or null if no such group exists
     */
    public Node getGroup(String groupName)
    {
        Node group = null;
        NodeRef groupRef = authorityDAO.getAuthorityNodeRefOrNull(groupName);
        if (groupRef != null)
        {
            group = new Node(groupRef, services, scope);
        }
        return group;
    }
    
    /**
     * Gets the members (people) of a group (including all sub-groups)
     * 
     * @param group  the group to retrieve members for
     * @param recurse  recurse into sub-groups
     * @return  the members of the group
     */
    public Node[] getMembers(Node group)
    {
        return getContainedAuthorities(group, AuthorityType.USER, true);
    }

    /**
     * Gets the members (people) of a group
     * 
     * @param group  the group to retrieve members for
     * @param recurse  recurse into sub-groups
     * @return  the members of the group
     */
    public Node[] getMembers(Node group, boolean recurse)
    {
        return getContainedAuthorities(group, AuthorityType.USER, recurse);
    }
    
    /**
     * Get Contained Authorities
     * 
     * @param container  authority containers
     * @param type  authority type to filter by
     * @param recurse  recurse into sub-containers
     * @return  contained authorities
     */
    private Node[] getContainedAuthorities(Node container, AuthorityType type, boolean recurse)
    {
        Node[] members = null;
        if (container.getType().equals(ContentModel.TYPE_AUTHORITY_CONTAINER))
        {
            AuthorityService authorityService = services.getAuthorityService();
            String groupName = (String)container.getProperties().get(ContentModel.PROP_AUTHORITY_NAME);
            Set<String> authorities = authorityService.getContainedAuthorities(type, groupName, !recurse);
            members = new Node[authorities.size()];
            int i = 0;
            for (String authority : authorities)
            {
                AuthorityType authorityType = AuthorityType.getAuthorityType(authority);
                if (authorityType.equals(AuthorityType.GROUP))
                {
                    Node group = getGroup(authority);
                    if (group != null)
                    {
                        members[i++] = group; 
                    }
                }
                else if (authorityType.equals(AuthorityType.USER))
                {
                    Node person = getPerson(authority);
                    if (person != null)
                    {
                        members[i++] = person; 
                    }
                }
            }
        }
        return members;
    }
    
}
