package com.andrewj.parachute.upnp;

import java.io.*;
import java.net.*;
import java.util.*;
import javax.xml.parsers.*;
import javax.xml.soap.*;
import javax.xml.xpath.*;
import javax.xml.namespace.*;
import org.w3c.dom.*;
import org.xml.sax.SAXException;

/**
 * Class to represent an IGD and its abilities
 * @author Andrew Jobson (andyjobson85@gmail.com)
 * @see <a href="http://upnp.org/specs/gw/UPnP-gw-InternetGatewayDevice-v1-Device.pdf">IGD Device Specification from UPNP.org</a>
 */
public class InternetGateway {

    private String urlBase;

    private String friendlyName;

    private String location;

    private InetAddress localAddress;

    private String serviceType;

    private String controlURL;

    public InternetGateway() {
    }

    /**
	 * @return the urlBase
	 */
    public String getBaseURL() {
        return urlBase;
    }

    /**
	 * @return the friendlyName
	 */
    public String getFriendlyName() {
        return friendlyName;
    }

    /**
	 * @return the localAddress
	 */
    public InetAddress getLocalAddress() {
        return localAddress;
    }

    public void setLocalAddress(InetAddress i) {
        localAddress = i;
    }

    public void setLocation(String l) {
        location = l;
    }

    /**
	 * This method is called once when the IGD object is instantiated, and never again for the life of that object
	 * it reads the necessary information from the IGD description to populate the fields of the object
	 * @throws IOException
	 * @throws SAXException 
	 * @throws ParserConfigurationException 
	 * @throws XPathExpressionException 
	 */
    public void getInfo() throws IOException, SAXException, ParserConfigurationException, XPathExpressionException {
        URLConnection urlConn = new URL(location).openConnection();
        DocumentBuilderFactory factory = DocumentBuilderFactory.newInstance();
        DocumentBuilder builder = factory.newDocumentBuilder();
        Document d = builder.parse(urlConn.getInputStream());
        XPath xPath = XPathFactory.newInstance().newXPath();
        Element e = (Element) (xPath.evaluate("//URLBase", d, XPathConstants.NODE));
        urlBase = e.getTextContent();
        if (urlBase.endsWith("/")) urlBase = urlBase.substring(0, urlBase.length() - 1);
        e = (Element) (xPath.evaluate("//deviceType[text()='urn:schemas-upnp-org:device:InternetGatewayDevice:1']/../friendlyName", d, XPathConstants.NODE));
        friendlyName = e.getTextContent();
        e = (Element) (xPath.evaluate("//deviceType[text()='urn:schemas-upnp-org:device:WANConnectionDevice:1']/../serviceList/service/serviceType[text()='urn:schemas-upnp-org:service:WANIPConnection:1' or text()='urn:schemas-upnp-org:service:WANPPPConnection:1']", d, XPathConstants.NODE));
        serviceType = e.getTextContent();
        e = (Element) (xPath.evaluate("../controlURL", e, XPathConstants.NODE));
        controlURL = formatURL(e.getTextContent());
        if (!controlURL.startsWith("/")) controlURL = "/" + controlURL;
    }

    /**
	 * Some routers advertise control URLS as 'urlBase' followed by path. Others simply have the path
	 * This function formats a given url and gets rid of the 'urlBase' from the front if it exists
	 * pass in 'http://192.168.0.1:51234/upnp/something' and get back '/upnp/something'
	 * @param s the string to be formatted
	 * @return a formatted String
	 */
    private String formatURL(String s) {
        if (s.startsWith(urlBase)) {
            return s.replaceFirst(urlBase, "");
        } else {
            return s;
        }
    }

    /**
	 * Send a UPnP command to the device represented by this object
	 * @param url the url to send the message to e.g. http://192.168.0.1:51234/upnp/control/WANIPConnection
	 * @param svc the service to invoke e.g. urn:schemas-upnp-org:service:WANIPConnection:1
	 * @param action the action to perform (a UPnP function name) e.g. addPortMapping
	 * @param args the arguments for the action
	 * @return a map representing the response
	 * @throws IOException
	 */
    private Map<String, String> upnpCommand(String url, String svc, String action, Map<String, String> args) throws Exception {
        SOAPConnectionFactory sfc = SOAPConnectionFactory.newInstance();
        SOAPConnection connection = sfc.createConnection();
        MessageFactory mf = MessageFactory.newInstance();
        SOAPMessage sm = mf.createMessage();
        sm.getSOAPHeader().detachNode();
        SOAPBody sb = sm.getSOAPBody();
        QName bodyName = new QName(svc, action, "m");
        SOAPBodyElement bodyElement = sb.addBodyElement(bodyName);
        if (args != null && args.size() > 0) {
            for (String argument : args.keySet()) {
                QName argName = new QName(argument);
                SOAPElement argElement = bodyElement.addChildElement(argName);
                argElement.addTextNode(args.get(argument));
            }
        }
        MimeHeaders headers = sm.getMimeHeaders();
        headers.addHeader("SOAPAction", svc + "#" + action);
        SOAPMessage res = connection.call(sm, url);
        Map<String, String> output = new HashMap<String, String>();
        @SuppressWarnings("rawtypes") Iterator it = res.getSOAPBody().getChildElements();
        while (it.hasNext()) {
            javax.xml.soap.Node e = (javax.xml.soap.Node) it.next();
            if (e.getNodeType() == javax.xml.soap.Node.ELEMENT_NODE) {
                NodeList nl = e.getChildNodes();
                for (int i = 0; i < nl.getLength(); i++) {
                    if (nl.item(i).getNodeType() == javax.xml.soap.Node.ELEMENT_NODE) {
                        output.put(nl.item(i).getNodeName(), nl.item(i).getTextContent());
                    }
                }
                break;
            }
        }
        return output;
    }

    /**
	 * Map a port on the IGD represented by this device
	 * @param exPort the external port
	 * @param inPort the internal port
	 * @param intClient the internal client address
	 * @param protocol the protocol to use
	 * @param description the description of the port
	 * @return boolean indicating success/failure
	 */
    public boolean addPortMapping(int exPort, int inPort, String intClient, String protocol, String description) throws Exception {
        Map<String, String> a = new LinkedHashMap<String, String>();
        a.put("NewRemoteHost", "");
        a.put("NewExternalPort", Integer.toString(exPort));
        a.put("NewProtocol", protocol);
        a.put("NewInternalPort", Integer.toString(inPort));
        a.put("NewInternalClient", intClient);
        a.put("NewEnabled", Integer.toString(1));
        a.put("NewPortMappingDescription", description);
        a.put("NewLeaseDuration", Integer.toString(0));
        Map<String, String> r = upnpCommand(urlBase + controlURL, serviceType, "AddPortMapping", a);
        return r.get("errorCode") == null;
    }

    /**
	 * Get the external IP as reported by this IGD
	 * @return the IP as a string
	 */
    public String getExternalAddress() throws Exception {
        Map<String, String> r = upnpCommand(urlBase + controlURL, serviceType, "GetExternalIPAddress", null);
        return r.get("NewExternalIPAddress");
    }

    /**
	 * returns true if the given port/protocol pair are mapped already on this IGD, false otherwise
	 * calls UPnP action 'GetSpecificPortMapping'
	 * @param extPort the port to test for
	 * @param protocol the protocol to test for
	 * @return true if port/protocol pair is mapped, false otherwise
	 */
    public boolean isPortMapped(int extPort, String protocol) throws Exception {
        Map<String, String> args = new LinkedHashMap<String, String>();
        args.put("NewRemoteHost", "");
        args.put("NewExternalPort", Integer.toString(extPort));
        args.put("NewProtocol", protocol);
        Map<String, String> r = upnpCommand(urlBase + controlURL, serviceType, "GetSpecificPortMappingEntry", args);
        return r.get("NewInternalPort") != null;
    }

    /**
	 * deletes the port mapping specified by the given external port and protocol
	 * @param extPort the external port
	 * @param protocol the protocol
	 */
    public void removePortMapping(int extPort, String protocol) throws Exception {
        Map<String, String> a = new LinkedHashMap<String, String>();
        a.put("NewRemoteHost", "");
        a.put("NewExternalPort", Integer.toString(extPort));
        a.put("NewProtocol", protocol);
        upnpCommand(urlBase + controlURL, serviceType, "DeletePortMapping", a);
    }
}
