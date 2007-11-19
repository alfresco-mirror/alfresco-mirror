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
 * Foundation, Inc., 51 Franklin Street, Fifth Floor, Boston, MA 02110-1301, USA.

 * As a special exception to the terms and conditions of version 2.0 of 
 * the GPL, you may redistribute this Program in connection with Free/Libre 
 * and Open Source Software ("FLOSS") applications as described in Alfresco's 
 * FLOSS exception.  You should have recieved a copy of the text describing 
 * the FLOSS exception, and it is also available here: 
 * http://www.alfresco.com/legal/licensing"
 */
package org.alfresco.module.phpIntegration.lib;

import org.alfresco.module.phpIntegration.lib.Session.SessionWork;
import org.alfresco.service.cmr.repository.NodeRef;
import org.alfresco.service.cmr.repository.NodeService;
import org.alfresco.service.cmr.repository.StoreRef;
import org.alfresco.util.EqualsHelper;

/**
 * The store object.  Represents a store in the repository
 * 
 * @author Roy Wetherall
 */
public class Store implements ScriptObject
{
    /** The script object name */
    private static final String SCRIPT_OBJECT_NAME = "Store";
    
    /** The session */
    protected Session session;
    
    /** The store reference */
    protected StoreRef storeRef;

    /**
     * Constructor 
     * 
     * @param session   the session
     * @param storeRef  the store reference
     */
    public Store(Session session, StoreRef storeRef)
    {
        this.storeRef = storeRef;
        this.session = session;
    }
    
    /**
     * Constructor
     * 
     * @param session   the session
     * @param address   the address
     */
    public Store(Session session, String address)
    {
        this(session, address, StoreRef.PROTOCOL_WORKSPACE);
    }
    
    /**
     * Constructor
     * 
     * @param session   the session
     * @param address   the address
     * @param scheme    the scheme
     */
    public Store(Session session, String address, String scheme)
    {
        this.session = session;
        this.storeRef = new StoreRef(scheme, address);
    }
    
    /**
     * @see org.alfresco.module.phpIntegration.lib.ScriptObject#getScriptObjectName()
     */
    public String getScriptObjectName()
    {
        return SCRIPT_OBJECT_NAME;
    }
    
    /**
     * Get the store reference
     * 
     * @return  the store reference
     */
    public StoreRef getStoreRef()
    {
        return this.storeRef;
    }
    
    /**
     * Get the address of the store
     * 
     * @return  the address of the store
     */
    public String getAddress()
    {
        return this.storeRef.getIdentifier();
    }
    
    /**
     * Gets the scheme of the store
     * 
     * @return  the scheme
     */
    public String getScheme()
    {
        return this.storeRef.getProtocol();
    }
    
    /**
     * Gets the root node
     * 
     * @return  the root node of the store
     */
    public Node getRootNode()
    {
    	return this.session.doSessionWork(new SessionWork<Node>()
    	{
    		public Node doWork() 
			{
	    		// Get the node service
	    		NodeService nodeService = Store.this.session.getServiceRegistry().getNodeService();
	        
	    		// Get the root node
	    		NodeRef rootNode = nodeService.getRootNode(Store.this.storeRef);        
	    		return new Node(Store.this.session, rootNode); 
			}
    	});
    }
    
    /**
     * PHP toString method
     * 
     * @return  the store ref string
     */
    public String __toString()
    {
       return toString(); 
    }
    
    /**
     * @see java.lang.Object#toString()
     */
    public String toString()
    {
        return this.storeRef.toString();
    }
    
    /**
     * @see java.lang.Object#equals(java.lang.Object)
     */
    public boolean equals(Object o)
    {
        if (this == o)
        {
            return true;
        }
        if (!(o instanceof Store))
        {
            return false;
        }
        Store other = (Store) o;

        return (EqualsHelper.nullSafeEquals(this.storeRef, other.storeRef));
    }

    /**
     * @see java.lang.Object#hashCode()
     */
    public int hashCode()
    {
        return this.storeRef.hashCode();
    }
}
