/*
 * Copyright (C) 2006-2008 Alfresco Software Limited.
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

package org.alfresco.jlan.smb.server;

import java.net.InetAddress;
import java.net.Socket;

import org.alfresco.jlan.debug.Debug;
import org.alfresco.jlan.server.SocketSessionHandler;
import org.alfresco.jlan.server.config.ServerConfiguration;
import org.alfresco.jlan.smb.mailslot.TcpipNetBIOSHostAnnouncer;

/**
 * NetBIOS Socket Session Handler Class
 * 
 * @author gkspencer
 */
public class NetBIOSSessionSocketHandler extends SocketSessionHandler {

	// Thread group

	private static final ThreadGroup NetBIOSGroup = new ThreadGroup("NetBIOSSessions");

	/**
	 * Class constructor
	 * 
	 * @param srv SMBServer
	 * @param port int
	 * @param bindAddr InetAddress
	 * @param debug boolean
	 */
	public NetBIOSSessionSocketHandler(SMBServer srv, int port, InetAddress bindAddr, boolean debug) {
		super("NetBIOS", "SMB", srv, bindAddr, port);
		
		// Enable/disable debug output
		
		setDebug( debug);
	}

	/**
	 * Accept a new connection on the specified socket
	 * 
	 * @param sock Socket
	 */
	protected void acceptConnection(Socket sock) {

		try {

			// Create a packet handler for the session

			SMBServer smbServer = (SMBServer) getServer();
			PacketHandler pktHandler = new NetBIOSPacketHandler( sock, smbServer.getPacketPool());

			// Create a server session for the new request, and set the session id.

			SMBSrvSession srvSess = SMBSrvSession.createSession(pktHandler, smbServer, getNextSessionId());

			// Start the new session in a seperate thread

			Thread srvThread = new Thread(NetBIOSGroup, srvSess);
			srvThread.setDaemon(true);
			srvThread.setName("Sess_N" + srvSess.getSessionId() + "_" + sock.getInetAddress().getHostAddress());
			srvThread.start();
		}
		catch (Exception ex) {

			// Debug

			if ( Debug.EnableError && hasDebug())
				Debug.println("[SMB] NetBIOS Failed to create session, " + ex.toString());
		}
	}

	/**
	 * Create the TCP/IP NetBIOS session socket handlers for the main SMB/CIFS server
	 * 
	 * @param server SMBServer
	 * @param sockDbg boolean
	 * @exception Exception
	 */
	public final static void createSessionHandlers(SMBServer server, boolean sockDbg)
		throws Exception {

		// Access the CIFS server configuration

		ServerConfiguration config = server.getConfiguration();
		CIFSConfigSection cifsConfig = (CIFSConfigSection) config.getConfigSection(CIFSConfigSection.SectionName);

		// Create the NetBIOS SMB handler

		SocketSessionHandler sessHandler = new NetBIOSSessionSocketHandler( server, cifsConfig.getSessionPort(), cifsConfig.getSMBBindAddress(), sockDbg);
		sessHandler.initializeSessionHandler( server);

		// Add the session handler to the list of active handlers

//		server.addSessionHandler(sessHandler);

		// Run the NetBIOS session handler in a seperate thread

		Thread nbThread = new Thread(sessHandler);
		nbThread.setName("NetBIOS_Handler");
		nbThread.start();

		// DEBUG

		if ( Debug.EnableError && sockDbg)
			Debug.println("[SMB] TCP NetBIOS session handler created");

		// Check if a host announcer should be created

		if ( cifsConfig.hasEnableAnnouncer()) {

			// Create the TCP NetBIOS host announcer

			TcpipNetBIOSHostAnnouncer announcer = new TcpipNetBIOSHostAnnouncer();

			// Set the host name to be announced

			announcer.addHostName(cifsConfig.getServerName());
			announcer.setDomain(cifsConfig.getDomainName());
			announcer.setComment(cifsConfig.getComment());
			announcer.setBindAddress(cifsConfig.getSMBBindAddress());
			if ( cifsConfig.getHostAnnouncerPort() != 0)
				announcer.setPort(cifsConfig.getHostAnnouncerPort());

			// Check if there are alias names to be announced

			if ( cifsConfig.hasAliasNames())
				announcer.addHostNames(cifsConfig.getAliasNames());

			// Set the announcement interval

			if ( cifsConfig.getHostAnnounceInterval() > 0)
				announcer.setInterval(cifsConfig.getHostAnnounceInterval());

			try {
				announcer.setBroadcastAddress(cifsConfig.getBroadcastMask());
			}
			catch (Exception ex) {
			}

			// Set the server type flags

			announcer.setServerType(cifsConfig.getServerType());

			// Enable debug output

			if ( cifsConfig.hasHostAnnounceDebug())
				announcer.setDebug(true);

			// Add the host announcer to the SMS servers list

//			server.addHostAnnouncer(announcer);

			// Start the host announcer thread

			announcer.start();

			// DEBUG

			if ( Debug.EnableError && sockDbg)
				Debug.println("[SMB] TCP NetBIOS host announcer created");
		}
	}
}
