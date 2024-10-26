package it.unical.inf.wsportal.server;

import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.net.MalformedURLException;
import java.net.URI;
import java.net.URISyntaxException;
import java.net.URL;
import java.net.URLConnection;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.NamedNodeMap;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;
import org.w3c.tidy.Tidy;

/**
 *
 * @author Simone Spaccarotella {spa.simone@gmail.com}, Carmine Dodaro {carminedodaro@gmail.com}
 */
public class WsdlInfoRetreiver {

    private String wsdlFile;

    private Document document;

    private static final String WSDL_NS = "http://schemas.xmlsoap.org/wsdl/";

    private static final String SOAP_NS = "http://schemas.xmlsoap.org/wsdl/soap/";

    /**
     *
     * @param wsdlFile
     * @throws URISyntaxException
     * @throws MalformedURLException
     * @throws IOException
     */
    public WsdlInfoRetreiver(String wsdlFile) throws URISyntaxException, MalformedURLException, IOException {
        this.wsdlFile = wsdlFile;
        generateDOM();
    }

    /**
     * 
     * @return
     */
    public String getServiceUrl() {
        String soapPrefix = getSoapPrefix();
        return findServiceUrl(soapPrefix);
    }

    /**
     * 
     * @return
     */
    public String getServiceName() {
        String wsdlPrefix = getWsdlPrefix();
        return findServiceName(wsdlPrefix);
    }

    /**
     * 
     * @return
     */
    public String getPortName() {
        String soapPrefix = getSoapPrefix();
        return findPortName(soapPrefix);
    }

    /**
     * Get the package name of the WSDL file.
     *
     * @return the package name
     */
    public String getPackageName() {
        URL url = null;
        StringBuilder packageName = new StringBuilder();
        try {
            url = new URI(getServiceUrl()).toURL();
            String temp = url.getHost();
            String[] split = temp.split("[.]");
            for (int i = split.length - 1; i >= 0; i--) {
                packageName.append(split[i] + "_");
            }
            temp = url.getPath();
            temp = temp.replaceFirst("/", "");
            temp = temp.replaceAll("/", "_");
            temp = temp.replaceAll("[.]", "_");
            packageName.append(temp);
        } catch (URISyntaxException ex) {
        } catch (MalformedURLException ex) {
        } catch (IOException ex) {
        }
        return packageName.toString().toLowerCase();
    }

    private InputStream getInputStream() throws URISyntaxException, MalformedURLException, IOException {
        InputStream inStream = null;
        try {
            URL url = new URI(wsdlFile).toURL();
            URLConnection connection = url.openConnection();
            connection.connect();
            inStream = connection.getInputStream();
        } catch (IllegalArgumentException ex) {
            inStream = new FileInputStream(wsdlFile);
        }
        return inStream;
    }

    private void generateDOM() throws URISyntaxException, MalformedURLException, IOException {
        Tidy parser = new Tidy();
        parser.setXmlTags(true);
        InputStream inStream = getInputStream();
        document = parser.parseDOM(inStream, null);
        inStream.close();
    }

    private String getWsdlPrefix() {
        String wsdlPrefix = null;
        Element element = document.getDocumentElement();
        NamedNodeMap attributes = element.getAttributes();
        for (int i = 0; i < attributes.getLength(); i++) {
            attributes.item(i).getNodeValue();
            if (attributes.item(i).getNodeValue().equals(WSDL_NS)) {
                wsdlPrefix = attributes.item(i).getNodeName().split(":")[1];
                break;
            }
        }
        return wsdlPrefix;
    }

    private String getSoapPrefix() {
        String soapPrefix = null;
        Element element = document.getDocumentElement();
        NamedNodeMap attributes = element.getAttributes();
        for (int i = 0; i < attributes.getLength(); i++) {
            attributes.item(i).getNodeValue();
            if (attributes.item(i).getNodeValue().equals(SOAP_NS)) {
                soapPrefix = attributes.item(i).getNodeName().split(":")[1];
                break;
            }
        }
        return soapPrefix;
    }

    private String findServiceName(String wsdlPrefix) {
        NodeList list = document.getElementsByTagName(wsdlPrefix + ":service");
        NamedNodeMap attributes = list.item(0).getAttributes();
        Node name = attributes.getNamedItem("name");
        return name.getNodeValue();
    }

    private String findServiceUrl(String soapPrefix) {
        NodeList list = document.getElementsByTagName(soapPrefix + ":address");
        NamedNodeMap attributes = list.item(0).getAttributes();
        Node location = attributes.getNamedItem("location");
        return location.getNodeValue();
    }

    private String findPortName(String soapPrefix) {
        Node node = document.getElementsByTagName(soapPrefix + ":address").item(0);
        node = node.getParentNode();
        NamedNodeMap attributes = node.getAttributes();
        Node location = attributes.getNamedItem("name");
        return location.getNodeValue();
    }
}
