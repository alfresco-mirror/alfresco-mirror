
package org.alfresco.repo.lotus.ws;

import javax.xml.bind.annotation.XmlAccessType;
import javax.xml.bind.annotation.XmlAccessorType;
import javax.xml.bind.annotation.XmlElement;
import javax.xml.bind.annotation.XmlRootElement;
import javax.xml.bind.annotation.XmlType;


/**
 * <p>Java class for anonymous complex type.
 * 
 * <p>The following schema fragment specifies the expected content contained within this class.
 * 
 * <pre>
 * &lt;complexType>
 *   &lt;complexContent>
 *     &lt;restriction base="{http://www.w3.org/2001/XMLSchema}anyType">
 *       &lt;sequence>
 *         &lt;element name="updateDocumentReturn" type="{http://webservices.clb.content.ibm.com}ClbResponse"/>
 *       &lt;/sequence>
 *     &lt;/restriction>
 *   &lt;/complexContent>
 * &lt;/complexType>
 * </pre>
 * 
 * 
 */
@XmlAccessorType(XmlAccessType.FIELD)
@XmlType(name = "", propOrder = {
    "updateDocumentReturn"
})
@XmlRootElement(name = "updateDocumentResponse")
public class UpdateDocumentResponse {

    @XmlElement(required = true)
    protected ClbResponse updateDocumentReturn;

    /**
     * Gets the value of the updateDocumentReturn property.
     * 
     * @return
     *     possible object is
     *     {@link ClbResponse }
     *     
     */
    public ClbResponse getUpdateDocumentReturn() {
        return updateDocumentReturn;
    }

    /**
     * Sets the value of the updateDocumentReturn property.
     * 
     * @param value
     *     allowed object is
     *     {@link ClbResponse }
     *     
     */
    public void setUpdateDocumentReturn(ClbResponse value) {
        this.updateDocumentReturn = value;
    }

}
