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

package org.alfresco.jlan.smb.server.nio.win32;

import java.io.IOException;
import java.util.Iterator;
import java.util.Vector;

import org.alfresco.jlan.debug.Debug;
import org.alfresco.jlan.netbios.NetBIOSName;
import org.alfresco.jlan.netbios.win32.NetBIOSSelectionKey;
import org.alfresco.jlan.netbios.win32.NetBIOSSelector;
import org.alfresco.jlan.netbios.win32.NetBIOSSocket;
import org.alfresco.jlan.server.SessionHandlerList;
import org.alfresco.jlan.server.config.InvalidConfigurationException;
import org.alfresco.jlan.smb.server.CIFSConfigSection;
import org.alfresco.jlan.smb.server.CifsConnectionsHandler;
import org.alfresco.jlan.smb.server.PacketHandler;
import org.alfresco.jlan.smb.server.SMBServer;
import org.alfresco.jlan.smb.server.SMBSrvSession;
import org.alfresco.jlan.smb.server.nio.RequestHandler;
import org.alfresco.jlan.smb.server.nio.RequestHandlerListener;

/**
 * Asynchronous Winsock NIO Connections Handler Class
 * 
 * <p>Initializes the configured CIFS session handlers and listens for incoming requests using a single thread.
 * 
 * @author gkspencer
 */
public class AsyncWinsockCifsConnectionsHandler implements CifsConnectionsHandler, RequestHandlerListener, Runnable {

	// Constants
	//
	// Number of session socket channels each request handler thread monitors
	//
	// Note: Windows has an OS limit of 64 events
	
	public static final int SessionSocketsPerHandler	= 64;
	
	// File server and workstation NetBIOS names to listen for incoming connections on

	private NetBIOSName m_srvNbName;
	private NetBIOSName m_wksNbName;
	
	// File server LANA
	
	private int m_srvLANA;
	
	// List of session handlers that are waiting for incoming requests
	
	private SessionHandlerList m_handlerList;
	
	// Selector used to monitor incoming connections
	
	private NetBIOSSelector m_nbSelector;
	
	// Session request handler(s)
	//
	// Each handler processes the socket read events for a number of session socket channels
	
	private Vector<AsyncWinsockCIFSRequestHandler> m_requestHandlers;
	
	// SMB server
	
	private SMBServer m_server;
	
	// Connection handler thread
	
	private Thread m_thread;

	// Shutdown request flag
	
	private boolean m_shutdown;
	
	// Session id
	
	private int m_sessId;
	
	// Debug output
	
	private boolean m_debug;
	
	/**
	 * Class constructor
	 */
	public AsyncWinsockCifsConnectionsHandler() {
		m_handlerList = new SessionHandlerList();
	}
	
	/**
	 * Check if debug output is enabled
	 * 
	 * @return boolean
	 */
	public final boolean hasDebug() {
		return m_debug;
	}

	/**
	 *  Return the count of active session handlers
	 *  
	 *  @return int
	 */
	public int numberOfSessionHandlers() {
		return m_handlerList.numberOfHandlers();
	}
	
	/**
	 * Initialize the connections handler
	 * 
	 * @param srv SMBServer
	 * @param config CIFSConfigSection
	 * @exception InvalidConfigurationException
	 */
	public final void initializeHandler( SMBServer srv, CIFSConfigSection config)
		throws InvalidConfigurationException {

		// Save the server the handler is associated with
		
		m_server = srv;
		
		// Check if socket debug output is enabled

		if ( (config.getSessionDebugFlags() & SMBSrvSession.DBG_SOCKET) != 0)
			m_debug = true;

		// Create the native SMB/port 445 session handler, if enabled
		
		if ( config.hasWin32NetBIOS()) {
			
			// Get the Win32 NetBIOS file server name

			String srvName = null;
			
			if ( srv.getCIFSConfiguration().getWin32ServerName() != null)
				srvName = srv.getCIFSConfiguration().getWin32ServerName();
			else
				srvName = srv.getCIFSConfiguration().getServerName();
			
			// Create the local NetBIOS names to listen for incoming connections on

			m_srvNbName = new NetBIOSName( srvName, NetBIOSName.FileServer, false);
			m_wksNbName = new NetBIOSName( srvName, NetBIOSName.WorkStation, false);
			
			// Create the session handlers to listen for incoming requests
			
			try {
				
				// Get the NetBIOS LANA to use, or -1 if the first available should be used
				
				int lana = srv.getCIFSConfiguration().getWin32LANA();

				// Create the session listener for the file server name
				
				AsyncWinsockNetBIOSSessionHandler sessHandler = new AsyncWinsockNetBIOSSessionHandler( lana, m_srvNbName, srv);
				sessHandler.initializeSessionHandler( m_server);
				m_handlerList.addHandler( sessHandler);
				
				// Get the server LANA
				
				m_srvLANA = sessHandler.getLANA();
				
				// Create the session listener for the workstation name
				
				sessHandler = new AsyncWinsockNetBIOSSessionHandler( lana, m_wksNbName, srv);
				sessHandler.initializeSessionHandler( m_server);
				m_handlerList.addHandler( sessHandler);
			}
			catch (IOException ex) {
				
				// DEBUG
				
				if ( Debug.EnableInfo && hasDebug())
					Debug.println( "[SMB] Error initializing session handler, " + ex.getMessage());

				throw new InvalidConfigurationException( ex.getMessage());
			}
		}
		
		// Check if any session handlers were created
		
		if ( m_handlerList.numberOfHandlers() == 0)
			throw new InvalidConfigurationException( "No CIFS session handlers enabled");
		
		// Create the session request handler list and add the first handler

		m_requestHandlers = new Vector<AsyncWinsockCIFSRequestHandler>();
		
		AsyncWinsockCIFSRequestHandler reqHandler = new AsyncWinsockCIFSRequestHandler( m_srvNbName, m_srvLANA, m_server.getThreadPool(), SessionSocketsPerHandler);
		reqHandler.setListener( this);
		reqHandler.setDebug( hasDebug());
		
		m_requestHandlers.add( reqHandler);
	}
	
	/**
	 * Start the connection handler thread
	 */
	public final void startHandler() {
		
		// Start the connection handler in its own thread
		
		m_thread = new Thread( this);
		m_thread.setName( "WinsockCIFSConnectionsHandler");
		m_thread.setDaemon( false);
		m_thread.start();
	}

	/**
	 * Stop the connections handler
	 */
	public final void stopHandler() {
	
		// Check if the thread is running
		
		if ( m_thread != null) {
			
			// Set the shutdown flag
			
			m_shutdown = true;

			// Close the first session handler socket to wakeup the listener thread

			if ( m_handlerList.numberOfHandlers() > 0) {
				
				// Get the first handler and close the listening socket
				
				AsyncWinsockNetBIOSSessionHandler sessHandler = (AsyncWinsockNetBIOSSessionHandler) m_handlerList.getHandlerAt( 0);
				NetBIOSSocket srvSock = sessHandler.getSocket();
				
				if ( srvSock != null) {
					try {
						srvSock.closeSocket();
					}
					catch (Exception ex) {
						Debug.println( ex);
					}
				}
			}
		}
	}
	
	/**
	 * Run the connections handler in a seperate thread
	 */
	public void run() {
		
		// Clear the shutdown flag, may have been restarted
		
		m_shutdown = false;
		
		// Initialize the socket selector
		
		try {
			
			// Create the selector
			
			m_nbSelector = new NetBIOSSelector();

			// Register the server sockets with the selector
			
			for ( int idx = 0; idx < m_handlerList.numberOfHandlers(); idx++) {
				
				// Get the current Winsock NetBIOS server socket and register with the selector for socket accept events
				
				AsyncWinsockNetBIOSSessionHandler curHandler = (AsyncWinsockNetBIOSSessionHandler) m_handlerList.getHandlerAt( idx);
				
				// Get the NetBIOSSocket and register with the selector

				NetBIOSSocket srvSocket = curHandler.getSocket();
				srvSocket.configureBlocking( false);
				
				srvSocket.register( m_nbSelector, NetBIOSSelectionKey.OP_ACCEPT, curHandler);
				
				// DEBUG
				
				if ( Debug.EnableInfo && hasDebug())
					Debug.println( "[SMB] Listening for connections on " + curHandler);
			}
		}
		catch ( IOException ex) {
			
			// DEBUG
			
			if ( Debug.EnableInfo && hasDebug()) {
				Debug.println( "[SMB] Error opening/registering Selector");
				Debug.println( ex);
			}
			
			m_shutdown = true;
		}
		
		// Loop until shutdown
		
		while ( m_shutdown != true) {
			
			// DEBUG
			
			if ( Debug.EnableInfo && hasDebug())
				Debug.println( "[SMB] Waiting for new connection ...");
			
			// Wait until there are some connections
			
			int connCnt = 0;
			
			try {
				connCnt = m_nbSelector.select();
			}
			catch ( IOException ex) {
				
				// DEBUG
				
				if ( Debug.EnableError && hasDebug()) {
					Debug.println( "[SMB] Error waiting for connection");
					Debug.println(ex);
				}
			}
			
			// Check if there are any connection events to process
			
			if ( connCnt == 0)
				continue;
			
			// Iterate the selected keys
			
			Iterator<NetBIOSSelectionKey> keysIter = m_nbSelector.selectedKeys().iterator();
			
			while ( keysIter.hasNext()) {
				
				// Get the current selection key and check if there is an incoming connection
				
				NetBIOSSelectionKey selKey = keysIter.next();
				if ( selKey.isAcceptable()) {
					
					try {

						// Get the listening server socket, accept the new client connection
					
						AsyncWinsockNetBIOSSessionHandler channelHandler = (AsyncWinsockNetBIOSSessionHandler) selKey.attachment();
						NetBIOSSocket clientSock = channelHandler.getSocket().accept();
						
						// Check if the connection is to the file server name
						
						if ( channelHandler.getNetBIOSName().getType() == NetBIOSName.FileServer) {
							
							// Create a packet handler for the new connection
							
							PacketHandler pktHandler = channelHandler.createPacketHandler( clientSock);
							
							// Create the new session 
							
							SMBSrvSession sess = SMBSrvSession.createSession( pktHandler, m_server, ++m_sessId);
							
							// DEBUG
							
							if ( Debug.EnableInfo && hasDebug())
								Debug.println( "[SMB] Connection from " + clientSock.getName() + ", handler=" + channelHandler + ", sess=" + sess.getUniqueId());
							
							// Add the new session to a request handler thread
	
							queueSessionToHandler( sess);
						}
						else {
							
							// Connection to the workstation name, this is used for the NetBIOS selector wakeup

							AsyncWinsockCIFSRequestHandler reqHandler = m_requestHandlers.firstElement();

							if ( reqHandler != null) {
								
								// DEBUG
								
								if ( Debug.EnableInfo && hasDebug())
									Debug.println( "[SMB] Winsock wakeup connection");
									
								// Set the server-side wakeup socket for the request handler
								
								reqHandler.setServerSideWakeupSocket( clientSock);
							}
							else if ( Debug.EnableInfo && hasDebug()) {
								
								// DEBUG
								
								Debug.println("[SMB] Connection to " + m_wksNbName + ", but no active request handler");
							}
						}
					}
					catch ( IOException ex) {
						
						// DEBUG
						
						if ( Debug.EnableError && hasDebug()) {
							Debug.println( "[SMB] Failed to accept connection");
							Debug.println( ex);
						}
					}
				}
				
				// Remove the key from the selected list
				
				keysIter.remove();
			}
		}
		
		// Close the session handlers
		
		for ( int idx = 0; idx < m_handlerList.numberOfHandlers(); idx++) {
			
			// Close the current session handler
			
			AsyncWinsockNetBIOSSessionHandler sessHandler = (AsyncWinsockNetBIOSSessionHandler) m_handlerList.getHandlerAt( idx);
			sessHandler.closeSessionHandler( null);
			
			// DEBUG
			
			if ( Debug.EnableInfo && hasDebug())
				Debug.println( "[SMB] Closed session handler " + sessHandler);
		}
		
		// Close the request handlers
		
		while ( m_requestHandlers.size() > 0) {
			
			// Close the current request handler
			
			AsyncWinsockCIFSRequestHandler reqHandler = m_requestHandlers.remove( 0);
			reqHandler.closeHandler();
			
			// DEBUG
			
			if ( Debug.EnableInfo && hasDebug())
				Debug.println( "[SMB] Closed request handler, " + reqHandler.getName());
		}
		
		// Close the selector
		
		if ( m_nbSelector != null) {
			
			try {
				m_nbSelector.close();
			}
			catch (Exception ex) {
				
				// DEBUG
				
				if ( Debug.EnableError && hasDebug())
					Debug.println( "[SMB] Error closing socket selector, " + ex.getMessage());
			}
		}
		
		// Clear the active thread before exiting
		
		m_thread = null;
	}

	/**
	 * Queue a new session to a request handler
	 * 
	 * @param sess SMBSrvSession
	 */
	private final void queueSessionToHandler( SMBSrvSession sess) {

		// Check if the current handler has room for a new session
		
		AsyncWinsockCIFSRequestHandler reqHandler = m_requestHandlers.firstElement();
		
		if ( reqHandler == null || reqHandler.hasFreeSessionSlot() == false) {
			
			// Create a new session request handler and add to the head of the list
			
			reqHandler = new AsyncWinsockCIFSRequestHandler( m_srvNbName, m_srvLANA, m_server.getThreadPool(), SessionSocketsPerHandler);
			reqHandler.setListener( this);
			reqHandler.setDebug( hasDebug());
			
			m_requestHandlers.add( 0, reqHandler);

			// DEBUG
			
			if ( Debug.EnableInfo && hasDebug())
				Debug.println( "[SMB] Added new CIFS request handler, " + reqHandler);
		}

		// Queue the new session to the current request handler
		
		reqHandler.queueSessionToHandler( sess);
	}
	
	/**
	 * Enable/disable debug output
	 * 
	 * @param ena boolean
	 */
	public final void setDebug( boolean ena) {
		m_debug = ena;
	}

	/**
	 * Request handler has no sessions to listen for events for
	 * 
	 * @param reqHandler RequestHandler
	 */
	public void requestHandlerEmpty(RequestHandler reqHandler) {
		
		synchronized ( m_handlerList) {

			// Check if the request handler is the current head of the handler list, if not then we can close
			// this request handler
		
			if ( m_requestHandlers.get( 0).getName().equals( reqHandler.getName()) == false) {
			
				// Remove the handler from the request handler list
			
				m_requestHandlers.remove( reqHandler);
				
				// Close the request handler
				
				reqHandler.closeHandler();
				
				// DEBUG
				
				if ( Debug.EnableInfo && hasDebug())
					Debug.println( "[SMB] Removed empty request handler, " + reqHandler.getName());
			}
		}
	}
}
