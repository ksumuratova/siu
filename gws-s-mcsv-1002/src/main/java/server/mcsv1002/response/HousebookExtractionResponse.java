//
// This file was generated by the JavaTM Architecture for XML Binding(JAXB) Reference Implementation, v2.2.4-2 
// See <a href="http://java.sun.com/xml/jaxb">http://java.sun.com/xml/jaxb</a> 
// Any modifications to this file will be lost upon recompilation of the source schema. 
// Generated on: 2013.10.22 at 02:06:10 PM MSK 
//


package server.mcsv1002.response;

import java.util.ArrayList;
import java.util.List;
import javax.xml.bind.annotation.XmlAccessType;
import javax.xml.bind.annotation.XmlAccessorType;
import javax.xml.bind.annotation.XmlRootElement;
import javax.xml.bind.annotation.XmlType;


/**
 * <p>Java class for housebookExtractionResponse complex type.
 * 
 * <p>The following schema fragment specifies the expected content contained within this class.
 * 
 * <pre>
 * &lt;complexType name="housebookExtractionResponse">
 *   &lt;complexContent>
 *     &lt;extension base="{http://housebookExtraction.messageTypes}housebookExtractionRequest">
 *       &lt;sequence>
 *         &lt;element name="owner" type="{http://housebookExtraction.messageTypes}Owner" maxOccurs="unbounded" minOccurs="0"/>
 *         &lt;element name="registeredPerson" type="{http://housebookExtraction.messageTypes}RegisteredPerson" maxOccurs="unbounded" minOccurs="0"/>
 *         &lt;element name="comment" type="{http://www.w3.org/2001/XMLSchema}string" minOccurs="0"/>
 *       &lt;/sequence>
 *     &lt;/extension>
 *   &lt;/complexContent>
 * &lt;/complexType>
 * </pre>
 * 
 * 
 */
@XmlAccessorType(XmlAccessType.FIELD)
@XmlType(name = "housebookExtractionResponse", propOrder = {
    "owner",
    "registeredPerson",
    "comment"
})
@XmlRootElement
public class HousebookExtractionResponse
    extends HousebookExtractionRequest
{

    protected List<Owner> owner;
    protected List<RegisteredPerson> registeredPerson;
    protected String comment;

    /**
     * Gets the value of the owner property.
     * 
     * <p>
     * This accessor method returns a reference to the live list,
     * not a snapshot. Therefore any modification you make to the
     * returned list will be present inside the JAXB object.
     * This is why there is not a <CODE>set</CODE> method for the owner property.
     * 
     * <p>
     * For example, to add a new item, do as follows:
     * <pre>
     *    getOwner().add(newItem);
     * </pre>
     * 
     * 
     * <p>
     * Objects of the following type(s) are allowed in the list
     * {@link Owner }
     * 
     * 
     */
    public List<Owner> getOwner() {
        if (owner == null) {
            owner = new ArrayList<Owner>();
        }
        return this.owner;
    }

    /**
     * Gets the value of the registeredPerson property.
     * 
     * <p>
     * This accessor method returns a reference to the live list,
     * not a snapshot. Therefore any modification you make to the
     * returned list will be present inside the JAXB object.
     * This is why there is not a <CODE>set</CODE> method for the registeredPerson property.
     * 
     * <p>
     * For example, to add a new item, do as follows:
     * <pre>
     *    getRegisteredPerson().add(newItem);
     * </pre>
     * 
     * 
     * <p>
     * Objects of the following type(s) are allowed in the list
     * {@link RegisteredPerson }
     * 
     * 
     */
    public List<RegisteredPerson> getRegisteredPerson() {
        if (registeredPerson == null) {
            registeredPerson = new ArrayList<RegisteredPerson>();
        }
        return this.registeredPerson;
    }

    /**
     * Gets the value of the comment property.
     * 
     * @return
     *     possible object is
     *     {@link String }
     *     
     */
    public String getComment() {
        return comment;
    }

    /**
     * Sets the value of the comment property.
     * 
     * @param value
     *     allowed object is
     *     {@link String }
     *     
     */
    public void setComment(String value) {
        this.comment = value;
    }

}
