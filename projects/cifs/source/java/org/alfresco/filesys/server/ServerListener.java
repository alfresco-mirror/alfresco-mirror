package org.alfresco.filesys.server;

/**
 * Server Listener Interface
 * <p>
 * The server listener allows external components to receive notification of server startup,
 * shutdown and error events.
 */
public interface ServerListener
{
    // Server event types

    public static final int ServerStartup = 0;
    public static final int ServerActive = 1;
    public static final int ServerShutdown = 2;
    public static final int ServerError = 3;

    /**
     * Receive a server event notification
     * 
     * @param server NetworkServer
     * @param event int
     */
    public void serverStatusEvent(NetworkServer server, int event);
}
