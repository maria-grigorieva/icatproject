//
// This file was generated by the JavaTM Architecture for XML Binding(JAXB) Reference Implementation, vhudson-jaxb-ri-2.1-382 
// See <a href="http://java.sun.com/xml/jaxb">http://java.sun.com/xml/jaxb</a> 
// Any modifications to this file will be lost upon recompilation of the source schema. 
// Generated on: 2007.09.25 at 11:08:32 AM BST 
//


package uk.icat3.jaxb.gen;

import javax.xml.bind.annotation.XmlEnum;
import javax.xml.bind.annotation.XmlType;


/**
 * <p>Java class for Icat_Role.
 * 
 * <p>The following schema fragment specifies the expected content contained within this class.
 * <p>
 * <pre>
 * &lt;simpleType name="Icat_Role">
 *   &lt;restriction base="{http://www.w3.org/2001/XMLSchema}string">
 *     &lt;maxLength value="255"/>
 *     &lt;enumeration value="ADMIN"/>
 *     &lt;enumeration value="CREATOR"/>
 *     &lt;enumeration value="DELETER"/>
 *     &lt;enumeration value="DOWNLOADER"/>
 *     &lt;enumeration value="READER"/>
 *     &lt;enumeration value="UPDATER"/>
 *   &lt;/restriction>
 * &lt;/simpleType>
 * </pre>
 * 
 */
@XmlType(name = "Icat_Role")
@XmlEnum
public enum IcatRole {

    ADMIN,
    CREATOR,
    DELETER,
    DOWNLOADER,
    READER,
    UPDATER;

    public String value() {
        return name();
    }

    public static IcatRole fromValue(String v) {
        return valueOf(v);
    }

}
