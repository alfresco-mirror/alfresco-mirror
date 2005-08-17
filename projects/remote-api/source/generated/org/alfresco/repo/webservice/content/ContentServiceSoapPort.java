/**
 * ContentServiceSoapPort.java
 *
 * This file was auto-generated from WSDL
 * by the Apache Axis 1.2.1 Jun 14, 2005 (09:15:57 EDT) WSDL2Java emitter.
 */

package org.alfresco.repo.webservice.content;

public interface ContentServiceSoapPort extends java.rmi.Remote {

    /**
     * Describes one or more content streams.
     */
    public org.alfresco.repo.webservice.types.Content[] describe(org.alfresco.repo.webservice.types.Predicate[] items) throws java.rmi.RemoteException, org.alfresco.repo.webservice.content.ContentFault;

    /**
     * Retrieves content from the repository.
     */
    public org.alfresco.repo.webservice.content.ReadResult read(org.alfresco.repo.webservice.types.Reference node) throws java.rmi.RemoteException, org.alfresco.repo.webservice.content.ContentFault;

    /**
     * Retrieves content from the repository in chunks.
     */
    public org.alfresco.repo.webservice.content.ReadResult readChunk(org.alfresco.repo.webservice.types.Reference node, org.alfresco.repo.webservice.content.ContentSegment segment) throws java.rmi.RemoteException, org.alfresco.repo.webservice.content.ContentFault;

    /**
     * Retrieves the next chunk of content from the given read session.
     */
    public org.alfresco.repo.webservice.content.ReadResult readNext(java.lang.String readSession) throws java.rmi.RemoteException, org.alfresco.repo.webservice.content.ContentFault;

    /**
     * Writes content to the repository.
     */
    public void write(org.alfresco.repo.webservice.types.Reference node, byte[] content) throws java.rmi.RemoteException, org.alfresco.repo.webservice.content.ContentFault;

    /**
     * Creates new content in the repository.
     */
    public org.alfresco.repo.webservice.types.Content create(org.alfresco.repo.webservice.types.ParentReference parent, org.alfresco.repo.webservice.types.ContentFormat format, byte[] content) throws java.rmi.RemoteException, org.alfresco.repo.webservice.content.ContentFault;

    /**
     * Deletes content from the repository.
     */
    public org.alfresco.repo.webservice.types.Reference[] delete(org.alfresco.repo.webservice.types.Predicate items) throws java.rmi.RemoteException, org.alfresco.repo.webservice.content.ContentFault;

    /**
     * Determines whether content exists in the repository.
     */
    public org.alfresco.repo.webservice.content.ExistsResult[] exists(org.alfresco.repo.webservice.types.Predicate items) throws java.rmi.RemoteException, org.alfresco.repo.webservice.content.ContentFault;
}
