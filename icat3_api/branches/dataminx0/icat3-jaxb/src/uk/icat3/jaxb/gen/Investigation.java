//
// This file was generated by the JavaTM Architecture for XML Binding(JAXB) Reference Implementation, vhudson-jaxb-ri-2.1-382 
// See <a href="http://java.sun.com/xml/jaxb">http://java.sun.com/xml/jaxb</a> 
// Any modifications to this file will be lost upon recompilation of the source schema. 
// Generated on: 2007.10.09 at 05:12:05 PM BST 
//


package uk.icat3.jaxb.gen;

import java.util.ArrayList;
import java.util.List;
import javax.xml.bind.annotation.XmlAccessType;
import javax.xml.bind.annotation.XmlAccessorType;
import javax.xml.bind.annotation.XmlAttribute;
import javax.xml.bind.annotation.XmlElement;
import javax.xml.bind.annotation.XmlType;


/**
 * <p>Java class for Investigation complex type.
 * 
 * <p>The following schema fragment specifies the expected content contained within this class.
 * 
 * <pre>
 * &lt;complexType name="Investigation">
 *   &lt;complexContent>
 *     &lt;restriction base="{http://www.w3.org/2001/XMLSchema}anyType">
 *       &lt;sequence>
 *         &lt;element name="inv_number" type="{}Inv_Number"/>
 *         &lt;element name="visit_id" type="{}Visit_Id"/>
 *         &lt;element name="instrument" type="{}Instrument"/>
 *         &lt;element name="title" type="{}str255" minOccurs="0"/>
 *         &lt;element name="investigator" type="{}Investigator" maxOccurs="unbounded" minOccurs="0"/>
 *         &lt;element name="inv_abstract" type="{}str4000" minOccurs="0"/>
 *         &lt;element name="inv_type" type="{}Inv_Type"/>
 *         &lt;element name="prev_inv_number" type="{}str255" minOccurs="0"/>
 *         &lt;element name="bcat_inv_str" type="{}str255" minOccurs="0"/>
 *         &lt;element name="grant_id" type="{}str255" minOccurs="0"/>
 *         &lt;element name="facility_cycle" type="{}Facility_Cycle" minOccurs="0"/>
 *         &lt;element name="keyword" type="{}Keyword" maxOccurs="unbounded" minOccurs="0"/>
 *         &lt;element name="topic" type="{}Topic" maxOccurs="unbounded" minOccurs="0"/>
 *         &lt;element name="publication" type="{}Publication" maxOccurs="unbounded" minOccurs="0"/>
 *         &lt;element name="sample" type="{}Sample" maxOccurs="unbounded" minOccurs="0"/>
 *         &lt;element name="dataset" type="{}Dataset" maxOccurs="unbounded" minOccurs="0"/>
 *       &lt;/sequence>
 *       &lt;attribute name="trusted" type="{http://www.w3.org/2001/XMLSchema}boolean" default="true" />
 *     &lt;/restriction>
 *   &lt;/complexContent>
 * &lt;/complexType>
 * </pre>
 * 
 * 
 */
@XmlAccessorType(XmlAccessType.FIELD)
@XmlType(name = "Investigation", propOrder = {
    "invNumber",
    "visitId",
    "instrument",
    "title",
    "investigator",
    "invAbstract",
    "invType",
    "prevInvNumber",
    "bcatInvStr",
    "grantId",
    "facilityCycle",
    "keyword",
    "topic",
    "publication",
    "sample",
    "dataset"
})
public class Investigation {

    @XmlElement(name = "inv_number", required = true)
    protected String invNumber;
    @XmlElement(name = "visit_id", required = true)
    protected String visitId;
    @XmlElement(required = true)
    protected String instrument;
    protected String title;
    protected List<Investigator> investigator;
    @XmlElement(name = "inv_abstract")
    protected String invAbstract;
    @XmlElement(name = "inv_type", required = true)
    protected String invType;
    @XmlElement(name = "prev_inv_number")
    protected String prevInvNumber;
    @XmlElement(name = "bcat_inv_str")
    protected String bcatInvStr;
    @XmlElement(name = "grant_id")
    protected String grantId;
    @XmlElement(name = "facility_cycle")
    protected String facilityCycle;
    protected List<Keyword> keyword;
    protected List<Topic> topic;
    protected List<Publication> publication;
    protected List<Sample> sample;
    protected List<Dataset> dataset;
    @XmlAttribute
    protected Boolean trusted;

    /**
     * Gets the value of the invNumber property.
     * 
     * @return
     *     possible object is
     *     {@link String }
     *     
     */
    public String getInvNumber() {
        return invNumber;
    }

    /**
     * Sets the value of the invNumber property.
     * 
     * @param value
     *     allowed object is
     *     {@link String }
     *     
     */
    public void setInvNumber(String value) {
        this.invNumber = value;
    }

    /**
     * Gets the value of the visitId property.
     * 
     * @return
     *     possible object is
     *     {@link String }
     *     
     */
    public String getVisitId() {
        return visitId;
    }

    /**
     * Sets the value of the visitId property.
     * 
     * @param value
     *     allowed object is
     *     {@link String }
     *     
     */
    public void setVisitId(String value) {
        this.visitId = value;
    }

    /**
     * Gets the value of the instrument property.
     * 
     * @return
     *     possible object is
     *     {@link String }
     *     
     */
    public String getInstrument() {
        return instrument;
    }

    /**
     * Sets the value of the instrument property.
     * 
     * @param value
     *     allowed object is
     *     {@link String }
     *     
     */
    public void setInstrument(String value) {
        this.instrument = value;
    }

    /**
     * Gets the value of the title property.
     * 
     * @return
     *     possible object is
     *     {@link String }
     *     
     */
    public String getTitle() {
        return title;
    }

    /**
     * Sets the value of the title property.
     * 
     * @param value
     *     allowed object is
     *     {@link String }
     *     
     */
    public void setTitle(String value) {
        this.title = value;
    }

    /**
     * Gets the value of the investigator property.
     * 
     * <p>
     * This accessor method returns a reference to the live list,
     * not a snapshot. Therefore any modification you make to the
     * returned list will be present inside the JAXB object.
     * This is why there is not a <CODE>set</CODE> method for the investigator property.
     * 
     * <p>
     * For example, to add a new item, do as follows:
     * <pre>
     *    getInvestigator().add(newItem);
     * </pre>
     * 
     * 
     * <p>
     * Objects of the following type(s) are allowed in the list
     * {@link Investigator }
     * 
     * 
     */
    public List<Investigator> getInvestigator() {
        if (investigator == null) {
            investigator = new ArrayList<Investigator>();
        }
        return this.investigator;
    }

    /**
     * Gets the value of the invAbstract property.
     * 
     * @return
     *     possible object is
     *     {@link String }
     *     
     */
    public String getInvAbstract() {
        return invAbstract;
    }

    /**
     * Sets the value of the invAbstract property.
     * 
     * @param value
     *     allowed object is
     *     {@link String }
     *     
     */
    public void setInvAbstract(String value) {
        this.invAbstract = value;
    }

    /**
     * Gets the value of the invType property.
     * 
     * @return
     *     possible object is
     *     {@link String }
     *     
     */
    public String getInvType() {
        return invType;
    }

    /**
     * Sets the value of the invType property.
     * 
     * @param value
     *     allowed object is
     *     {@link String }
     *     
     */
    public void setInvType(String value) {
        this.invType = value;
    }

    /**
     * Gets the value of the prevInvNumber property.
     * 
     * @return
     *     possible object is
     *     {@link String }
     *     
     */
    public String getPrevInvNumber() {
        return prevInvNumber;
    }

    /**
     * Sets the value of the prevInvNumber property.
     * 
     * @param value
     *     allowed object is
     *     {@link String }
     *     
     */
    public void setPrevInvNumber(String value) {
        this.prevInvNumber = value;
    }

    /**
     * Gets the value of the bcatInvStr property.
     * 
     * @return
     *     possible object is
     *     {@link String }
     *     
     */
    public String getBcatInvStr() {
        return bcatInvStr;
    }

    /**
     * Sets the value of the bcatInvStr property.
     * 
     * @param value
     *     allowed object is
     *     {@link String }
     *     
     */
    public void setBcatInvStr(String value) {
        this.bcatInvStr = value;
    }

    /**
     * Gets the value of the grantId property.
     * 
     * @return
     *     possible object is
     *     {@link String }
     *     
     */
    public String getGrantId() {
        return grantId;
    }

    /**
     * Sets the value of the grantId property.
     * 
     * @param value
     *     allowed object is
     *     {@link String }
     *     
     */
    public void setGrantId(String value) {
        this.grantId = value;
    }

    /**
     * Gets the value of the facilityCycle property.
     * 
     * @return
     *     possible object is
     *     {@link String }
     *     
     */
    public String getFacilityCycle() {
        return facilityCycle;
    }

    /**
     * Sets the value of the facilityCycle property.
     * 
     * @param value
     *     allowed object is
     *     {@link String }
     *     
     */
    public void setFacilityCycle(String value) {
        this.facilityCycle = value;
    }

    /**
     * Gets the value of the keyword property.
     * 
     * <p>
     * This accessor method returns a reference to the live list,
     * not a snapshot. Therefore any modification you make to the
     * returned list will be present inside the JAXB object.
     * This is why there is not a <CODE>set</CODE> method for the keyword property.
     * 
     * <p>
     * For example, to add a new item, do as follows:
     * <pre>
     *    getKeyword().add(newItem);
     * </pre>
     * 
     * 
     * <p>
     * Objects of the following type(s) are allowed in the list
     * {@link Keyword }
     * 
     * 
     */
    public List<Keyword> getKeyword() {
        if (keyword == null) {
            keyword = new ArrayList<Keyword>();
        }
        return this.keyword;
    }

    /**
     * Gets the value of the topic property.
     * 
     * <p>
     * This accessor method returns a reference to the live list,
     * not a snapshot. Therefore any modification you make to the
     * returned list will be present inside the JAXB object.
     * This is why there is not a <CODE>set</CODE> method for the topic property.
     * 
     * <p>
     * For example, to add a new item, do as follows:
     * <pre>
     *    getTopic().add(newItem);
     * </pre>
     * 
     * 
     * <p>
     * Objects of the following type(s) are allowed in the list
     * {@link Topic }
     * 
     * 
     */
    public List<Topic> getTopic() {
        if (topic == null) {
            topic = new ArrayList<Topic>();
        }
        return this.topic;
    }

    /**
     * Gets the value of the publication property.
     * 
     * <p>
     * This accessor method returns a reference to the live list,
     * not a snapshot. Therefore any modification you make to the
     * returned list will be present inside the JAXB object.
     * This is why there is not a <CODE>set</CODE> method for the publication property.
     * 
     * <p>
     * For example, to add a new item, do as follows:
     * <pre>
     *    getPublication().add(newItem);
     * </pre>
     * 
     * 
     * <p>
     * Objects of the following type(s) are allowed in the list
     * {@link Publication }
     * 
     * 
     */
    public List<Publication> getPublication() {
        if (publication == null) {
            publication = new ArrayList<Publication>();
        }
        return this.publication;
    }

    /**
     * Gets the value of the sample property.
     * 
     * <p>
     * This accessor method returns a reference to the live list,
     * not a snapshot. Therefore any modification you make to the
     * returned list will be present inside the JAXB object.
     * This is why there is not a <CODE>set</CODE> method for the sample property.
     * 
     * <p>
     * For example, to add a new item, do as follows:
     * <pre>
     *    getSample().add(newItem);
     * </pre>
     * 
     * 
     * <p>
     * Objects of the following type(s) are allowed in the list
     * {@link Sample }
     * 
     * 
     */
    public List<Sample> getSample() {
        if (sample == null) {
            sample = new ArrayList<Sample>();
        }
        return this.sample;
    }

    /**
     * Gets the value of the dataset property.
     * 
     * <p>
     * This accessor method returns a reference to the live list,
     * not a snapshot. Therefore any modification you make to the
     * returned list will be present inside the JAXB object.
     * This is why there is not a <CODE>set</CODE> method for the dataset property.
     * 
     * <p>
     * For example, to add a new item, do as follows:
     * <pre>
     *    getDataset().add(newItem);
     * </pre>
     * 
     * 
     * <p>
     * Objects of the following type(s) are allowed in the list
     * {@link Dataset }
     * 
     * 
     */
    public List<Dataset> getDataset() {
        if (dataset == null) {
            dataset = new ArrayList<Dataset>();
        }
        return this.dataset;
    }

    /**
     * Gets the value of the trusted property.
     * 
     * @return
     *     possible object is
     *     {@link Boolean }
     *     
     */
    public boolean isTrusted() {
        if (trusted == null) {
            return true;
        } else {
            return trusted;
        }
    }

    /**
     * Sets the value of the trusted property.
     * 
     * @param value
     *     allowed object is
     *     {@link Boolean }
     *     
     */
    public void setTrusted(Boolean value) {
        this.trusted = value;
    }

}
