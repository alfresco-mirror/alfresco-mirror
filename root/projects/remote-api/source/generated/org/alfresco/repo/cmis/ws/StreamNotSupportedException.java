
package org.alfresco.repo.cmis.ws;

import javax.xml.ws.WebFault;


/**
 * This class was generated by Apache CXF 2.0.6
 * Tue Jul 29 18:21:47 EEST 2008
 * Generated source version: 2.0.6
 * 
 */

@WebFault(name = "streamNotSupportedException", targetNamespace = "http://www.cmis.org/ns/1.0")

public class StreamNotSupportedException extends Exception {
    public static final long serialVersionUID = 20080729182147L;
    
    private org.alfresco.repo.cmis.ws.BasicFault streamNotSupportedException;

    public StreamNotSupportedException() {
        super();
    }
    
    public StreamNotSupportedException(String message) {
        super(message);
    }
    
    public StreamNotSupportedException(String message, Throwable cause) {
        super(message, cause);
    }

    public StreamNotSupportedException(String message, org.alfresco.repo.cmis.ws.BasicFault streamNotSupportedException) {
        super(message);
        this.streamNotSupportedException = streamNotSupportedException;
    }

    public StreamNotSupportedException(String message, org.alfresco.repo.cmis.ws.BasicFault streamNotSupportedException, Throwable cause) {
        super(message, cause);
        this.streamNotSupportedException = streamNotSupportedException;
    }

    public org.alfresco.repo.cmis.ws.BasicFault getFaultInfo() {
        return this.streamNotSupportedException;
    }
}
