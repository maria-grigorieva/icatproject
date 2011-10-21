
package uk.icat3.client;

import javax.xml.ws.WebFault;


/**
 * This class was generated by the JAX-WS RI.
 * JAX-WS RI 2.1.4-b01-
 * Generated source version: 2.1
 * 
 */
@WebFault(name = "ICATAPIException", targetNamespace = "client.icat3.uk")
public class ICATAPIException_Exception
    extends Exception
{

    /**
     * Java type that goes as soapenv:Fault detail element.
     * 
     */
    private ICATAPIException faultInfo;

    /**
     * 
     * @param message
     * @param faultInfo
     */
    public ICATAPIException_Exception(String message, ICATAPIException faultInfo) {
        super(message);
        this.faultInfo = faultInfo;
    }

    /**
     * 
     * @param message
     * @param faultInfo
     * @param cause
     */
    public ICATAPIException_Exception(String message, ICATAPIException faultInfo, Throwable cause) {
        super(message, cause);
        this.faultInfo = faultInfo;
    }

    /**
     * 
     * @return
     *     returns fault bean: uk.icat3.client.ICATAPIException
     */
    public ICATAPIException getFaultInfo() {
        return faultInfo;
    }

}
