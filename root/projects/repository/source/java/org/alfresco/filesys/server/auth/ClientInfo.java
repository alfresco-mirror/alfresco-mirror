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
package org.alfresco.filesys.server.auth;

import org.alfresco.service.cmr.repository.NodeRef;

import net.sf.acegisecurity.Authentication;

/**
 * Client Information Class
 * 
 * <p>The client information class holds the details of a remote user from a session setup or tree
 * connect request.
 * 
 * @author GKSpencer
 */
public class ClientInfo
{

    // Logon types

    public final static int LogonNormal = 0;
    public final static int LogonGuest = 1;
    public final static int LogonNull = 2;
    public final static int LogonAdmin = 3;

    // Logon type strings

    private static final String[] _logonTypStr = { "Normal", "Guest", "Null", "Administrator" };

    // User name and password

    private String m_user;
    private byte[] m_password;

    // ANSI encrypted password

    private byte[] m_ansiPwd;

    // Logon type

    private int m_logonType;

    // User's domain

    private String m_domain;

    // Operating system type

    private String m_opsys;

    // Remote network address

    private String m_ipAddr;

    // PID of the logon process for multi-stage logons
    
    private int m_pid = -1;
    
    // Authentication token
    
    private Authentication m_authToken;
    
    // Authentication ticket, used for web access without having to re-authenticate
    
    private String m_authTicket;
    
    // Home folder node
    
    private NodeRef m_homeNode;

	//	Group and user id
	
	private int m_gid = -1;
	private int m_uid = -1;
	
	//	List of groups for this user
	
	private int[] m_groups;
	
	//	NFS authentication type
	
	private int m_nfsAuthType = -1;
    
    /**
     * Default constructor
     */
    public ClientInfo()
    {
        setUserName("");
    }
    
    /**
     * Class constructor
     * 
     * @param user User name
     * @param pwd Password
     */
    public ClientInfo(String user, byte[] pwd)
    {
        setUserName(user);
        setPassword(pwd);
    }

    /**
     * Get the remote users domain.
     * 
     * @return String
     */
    public final String getDomain()
    {
        return m_domain;
    }

    /**
     * Get the remote operating system
     * 
     * @return String
     */
    public final String getOperatingSystem()
    {
        return m_opsys;
    }

    /**
     * Get the password.
     * 
     * @return String.
     */
    public final byte[] getPassword()
    {
        return m_password;
    }

    /**
     * Return the password as a string
     * 
     * @return String
     */
    public final String getPasswordAsString()
    {
        if (m_password != null)
            return new String(m_password);
        return null;
    }

    /**
     * Return the password as a character array
     * 
     * @return char[]
     */
    public final char[] getPasswordAsCharArray()
    {
        char[] cpwd = null;

        if (m_password != null)
        {
            String pwd = new String(m_password);
            cpwd = new char[pwd.length()];
            pwd.getChars(0, pwd.length(), cpwd, 0);
        }
        return cpwd;
    }

    /**
     * Determine if the client has specified an ANSI password
     * 
     * @return boolean
     */
    public final boolean hasANSIPassword()
    {
        return m_ansiPwd != null ? true : false;
    }

    /**
     * Return the ANSI encrypted password
     * 
     * @return byte[]
     */
    public final byte[] getANSIPassword()
    {
        return m_ansiPwd;
    }

    /**
     * Return the ANSI password as a string
     * 
     * @return String
     */
    public final String getANSIPasswordAsString()
    {
        if (m_ansiPwd != null)
            return new String(m_ansiPwd);
        return null;
    }

    /**
     * Get the user name.
     * 
     * @return String
     */
    public final String getUserName()
    {
        return m_user;
    }

    /**
     * Return the logon type
     * 
     * @return int
     */
    public final int getLogonType()
    {
        return m_logonType;
    }

    /**
     * Return the logon type as a string
     * 
     * @return String
     */
    public final String getLogonTypeString()
    {
        return _logonTypStr[m_logonType];
    }

    /**
     * Determine if the user is logged on as a guest
     * 
     * @return boolean
     */
    public final boolean isGuest()
    {
        return m_logonType == LogonGuest ? true : false;
    }

    /**
     * Determine if the session is a null session
     * 
     * @return boolean
     */
    public final boolean isNullSession()
    {
        return m_logonType == LogonNull ? true : false;
    }

    /**
     * Determine if the user if logged on as an administrator
     * 
     * @return boolean
     */
    public final boolean isAdministrator()
    {
        return m_logonType == LogonAdmin ? true : false;
    }

    /**
     * Determine if the client network address has been set
     * 
     * @return boolean
     */
    public final boolean hasClientAddress()
    {
        return m_ipAddr != null ? true : false;
    }

    /**
     * Return the client network address
     * 
     * @return String
     */
    public final String getClientAddress()
    {
        return m_ipAddr;
    }

    /**
     * Check if the client has an authentication token
     * 
     * @return boolean
     */
    public final boolean hasAuthenticationToken()
    {
        return m_authToken != null ? true : false;
    }
    
    /**
     * Return the authentication token
     * 
     * @return Authentication
     */
    public final Authentication getAuthenticationToken()
    {
        return m_authToken;
    }

    /**
     * Check if the client has an authentication ticket
     * 
     * @return boolean
     */
    public final boolean hasAuthenticationTicket()
    {
    	return m_authTicket != null ? true : false;
    }
    
    /**
     * Return the authentication ticket
     * 
     * @return String
     */
    public final String getAuthenticationTicket()
    {
    	return m_authTicket;
    }
    
    /**
     * Check if the client has a home folder node
     * 
     * @return boolean
     */
    public final boolean hasHomeFolder()
    {
        return m_homeNode != null ? true : false;
    }
    
    /**
     * Return the home folder node
     * 
     * @return NodeRef
     */
    public final NodeRef getHomeFolder()
    {
        return m_homeNode;
    }
    
	/**
	 * Get the group id
	 * 
	 * @return int
	 */
	public final int getGid()
	{
		return m_gid;
	}
	
	/**
	 * Return the user id
	 * 
	 * @return int
	 */
	public final int getUid()
	{
		return m_uid;
	}

	/**
	 * Determine if the client has additional groups
	 * 
	 * @return boolean
	 */
	public final boolean hasGroupsList()
	{
		return m_groups != null ? true : false;
	}
	
	/**
	 * Return the additional groups list
	 * 
	 * @return int[]
	 */
	public final int[] getGroupsList()
	{
		return m_groups;
	}
	
	/**
	 * Return the NFS authentication type
	 * 
	 * @return int
	 */	
	public final int getNFSAuthenticationType()
	{
		return m_nfsAuthType;
	}
	
    /**
     * Return the process id
     * 
     * @return int
     */
    public final int getProcessId()
    {
    	return m_pid;
    }
    
    /**
     * Set the process id
     * 
     * @param pid int
     */
    public final void setProcessId( int pid)
    {
    	m_pid = pid;
    }
    
    /**
     * Set the remote users domain
     * 
     * @param domain Remote users domain
     */
    public final void setDomain(String domain)
    {
        m_domain = domain;
    }

    /**
     * Set the remote users operating system type.
     * 
     * @param opsys Remote operating system
     */
    public final void setOperatingSystem(String opsys)
    {
        m_opsys = opsys;
    }

    /**
     * Set the password.
     * 
     * @param pwd byte[]
     */
    public final void setPassword(byte[] pwd)
    {
        m_password = pwd;
    }

    /**
     * Set the ANSI encrypted password
     * 
     * @param pwd byte[]
     */
    public final void setANSIPassword(byte[] pwd)
    {
        m_ansiPwd = pwd;
    }

    /**
     * Set the password
     * 
     * @param pwd Password string.
     */
    public final void setPassword(String pwd)
    {
        if (pwd != null)
            m_password = pwd.getBytes();
        else
            m_password = null;
    }

    /**
     * Set the user name
     * 
     * @param user User name string.
     */
    public final void setUserName(String user)
    {
        m_user = user;
    }

    /**
     * Set the logon type
     * 
     * @param logonType int
     */
    public final void setLogonType(int logonType)
    {
        m_logonType = logonType;
    }

    /**
     * Set the guest logon flag
     * 
     * @param guest boolean
     */
    public final void setGuest(boolean guest)
    {
        setLogonType(guest == true ? LogonGuest : LogonNormal);
    }

    /**
     * Set the client network address
     * 
     * @param addr String
     */
    public final void setClientAddress(String addr)
    {
        m_ipAddr = addr;
    }

    /**
     * Set the authentication toekn
     * 
     * @param token Authentication
     */
    public final void setAuthenticationToken(Authentication token)
    {
        m_authToken = token;
    }

    /**
     * Set the authentication ticket
     * 
     * @param ticket String
     */
    public final void setAuthenticationTicket(String ticket)
    {
    	m_authTicket = ticket;
    }
    
    /**
     * Set the home folder node
     * 
     * @param homeNode NodeRef
     */
    public final void setHomeFolder(NodeRef homeNode)
    {
        m_homeNode = homeNode;
    }
    
	/**
	 * Set the group id
	 * 
	 * @param gid int
	 */
	public final void setGid(int gid)
	{
		m_gid = gid;
	}
	
	/**
	 * Set the user id
	 * 
	 * @param uid int
	 */
	public final void setUid(int uid)
	{
		m_uid = uid;
	}

	/**
	 * Set the groups list
	 * 
	 * @param groups int[]
	 */
	public final void setGroupsList(int[] groups)
	{
		m_groups = groups;
	}
	
	/**
	 * Set the NFS authentication type
	 * 
	 * @param type int
	 */	
	public final void setNFSAuthenticationType(int type)
	{
		m_nfsAuthType = type;
	}
	
    /**
     * Display the client information as a string
     * 
     * @return String
     */
    public String toString()
    {
        StringBuffer str = new StringBuffer();
        str.append("[");
        str.append(getUserName());
        str.append(":");
        str.append(getPassword());
        str.append(",");
        str.append(getDomain());
        str.append(",");
        str.append(getOperatingSystem());

        if (hasClientAddress())
        {
            str.append(",");
            str.append(getClientAddress());
        }

        if ( hasAuthenticationToken())
        {
            str.append(",token=");
            str.append(getAuthenticationToken());
        }

        if ( hasAuthenticationTicket())
        {
        	str.append(",ticket=");
        	str.append(getAuthenticationTicket());
        }
        
        if (isGuest())
            str.append(",Guest");
        str.append("]");

        return str.toString();
    }
}