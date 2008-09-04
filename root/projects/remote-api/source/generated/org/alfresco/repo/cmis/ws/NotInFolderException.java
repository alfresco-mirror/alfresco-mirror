
package org.alfresco.repo.cmis.ws;

import javax.xml.ws.WebFault;


/**
 * This class was generated by Apache CXF 2.0.6
 * Tue Jul 29 18:22:19 EEST 2008
 * Generated source version: 2.0.6
 * 
 */

@WebFault(name = "notInFolderException", targetNamespace = "http://www.cmis.org/ns/1.0")

public class NotInFolderException extends Exception {
    public static final long serialVersionUID = 20080729182219L;
    
    private org.alfresco.repo.cmis.ws.BasicFault notInFolderException;

    public NotInFolderException() {
        super();
    }
    
    public NotInFolderException(String message) {
        super(message);
    }
    
    public NotInFolderException(String message, Throwable cause) {
        super(message, cause);
    }

    public NotInFolderException(String message, org.alfresco.repo.cmis.ws.BasicFault notInFolderException) {
        super(message);
        this.notInFolderException = notInFolderException;
    }

    public NotInFolderException(String message, org.alfresco.repo.cmis.ws.BasicFault notInFolderException, Throwable cause) {
        super(message, cause);
        this.notInFolderException = notInFolderException;
    }

    public org.alfresco.repo.cmis.ws.BasicFault getFaultInfo() {
        return this.notInFolderException;
    }
}
