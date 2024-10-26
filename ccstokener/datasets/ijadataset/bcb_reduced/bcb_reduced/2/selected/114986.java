package in.raster.oviyam.config;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.net.URL;
import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import org.apache.log4j.Logger;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.NodeList;
import org.w3c.dom.Text;
import org.xml.sax.InputSource;
import com.sun.org.apache.xml.internal.serialize.OutputFormat;
import com.sun.org.apache.xml.internal.serialize.XMLSerializer;

/**
 * 
 * @author Manikandan
 * @version 0.7
 *
 */
public class ServerXmlConfiguration {

    /**
	 * Initialize Logger.
	 */
    private static Logger log = Logger.getLogger(ServerXmlConfiguration.class);

    Document dom;

    File documentNameFile;

    public static String fname;

    public ServerXmlConfiguration() {
        try {
            DocumentBuilderFactory documentBuilderFactory = DocumentBuilderFactory.newInstance();
            DocumentBuilder documentBuilder = documentBuilderFactory.newDocumentBuilder();
            dom = documentBuilder.newDocument();
        } catch (Exception e) {
            e.printStackTrace();
            log.error(e.getMessage(), e);
        }
    }

    /**
	 * It reads and checks whether the file exists or not. If the file does not exist then it creates 
	 * a new xml document with the specified name.
	 * 
	 */
    public void createAndParseXML() {
        DocumentBuilderFactory dbf = DocumentBuilderFactory.newInstance();
        try {
            DocumentBuilder db = dbf.newDocumentBuilder();
            String xmlFileName = this.getClass().getResource("/conf/oviyam-config.xml").toString();
            if (xmlFileName.indexOf("default") > 0) documentNameFile = new File(xmlFileName.substring(5, xmlFileName.indexOf("default")) + "default/" + fname); else documentNameFile = new File(this.getClass().getClassLoader().getResource("/conf/oviyam-config.xml").toURI());
            if (documentNameFile.exists()) {
                documentNameFile.delete();
            }
            dom = db.newDocument();
        } catch (Exception e) {
            e.printStackTrace();
            log.error(e.getMessage(), e);
            System.exit(1);
        }
    }

    /**
	 * Creates the root element for XML document.
	 * @param serverConfiguration ServerConfiguration instance to keep the xml element values. 
	 */
    public void createRootElement(ServerConfiguration serverConfiguration) {
        try {
            Element rootEle = dom.createElement("configuration");
            if (!dom.hasChildNodes()) {
                dom.appendChild(rootEle);
            }
            Element serverEle = dom.createElement("server");
            Element aetitleEle = dom.createElement("aetitle");
            Text aetitleText = dom.createTextNode(serverConfiguration.getAeTitle());
            aetitleEle.appendChild(aetitleText);
            serverEle.appendChild(aetitleEle);
            Element hostnameEle = dom.createElement("hostname");
            Text hostnameText = dom.createTextNode(serverConfiguration.getHostName());
            hostnameEle.appendChild(hostnameText);
            serverEle.appendChild(hostnameEle);
            Element portEle = dom.createElement("port");
            Text portText = dom.createTextNode(serverConfiguration.getPort());
            portEle.appendChild(portText);
            serverEle.appendChild(portEle);
            Element wadoportEle = dom.createElement("wadoport");
            Text wadoportText = dom.createTextNode(serverConfiguration.getWadoPort());
            wadoportEle.appendChild(wadoportText);
            serverEle.appendChild(wadoportEle);
            Element dcmProtocol = dom.createElement("dcmprotocol");
            Text dcmProtocoltext = dom.createTextNode(serverConfiguration.getDcmProtocol());
            dcmProtocol.appendChild(dcmProtocoltext);
            serverEle.appendChild(dcmProtocol);
            Element root = dom.getDocumentElement();
            root.appendChild(serverEle);
        } catch (Exception e) {
            e.printStackTrace();
            log.error("Unable to create Elements for document", e);
            return;
        }
    }

    /**
	 * Writes the element values to the XML document.
	 */
    public void printToFile() {
        try {
            OutputFormat format = new OutputFormat(dom);
            format.setIndenting(true);
            XMLSerializer serializer = new XMLSerializer(new FileOutputStream(documentNameFile), format);
            serializer.serialize(dom);
            log.info("XML document has been created successfully.");
        } catch (IOException ioe) {
            log.error("Unable to write the element values to the XML document", ioe);
            ioe.printStackTrace();
            return;
        }
    }

    /**
	 * Updates the existing element values with the new values.
	 * @param AETITLE
	 * @param HOSTNAME
	 * @param PORT
	 * @param WADOPORT
	 * @param DCMPROTOCOL
	 */
    public void updateElementValues(String AETITLE, String HOSTNAME, String PORT, String WADOPORT, String DCMPROTOCOL) {
        try {
            NodeList root = dom.getElementsByTagName("server");
            for (int i = 0; i < root.getLength(); i++) {
                Element element = (Element) root.item(i);
                NodeList aetitle = element.getElementsByTagName("aetitle");
                Element aetitleElement = (Element) aetitle.item(0);
                aetitleElement.getFirstChild().setTextContent(AETITLE);
                NodeList hostname = element.getElementsByTagName("hostname");
                Element hostnameElement = (Element) hostname.item(0);
                hostnameElement.getFirstChild().setTextContent(HOSTNAME);
                NodeList port = element.getElementsByTagName("port");
                Element portElement = (Element) port.item(0);
                portElement.getFirstChild().setTextContent(PORT);
                NodeList wadoport = element.getElementsByTagName("wadoport");
                Element wadoportElement = (Element) wadoport.item(0);
                wadoportElement.getFirstChild().setTextContent(WADOPORT);
                NodeList dcmProtocol = element.getElementsByTagName("dcmprotocol");
                Element dcmProtocolElement = (Element) dcmProtocol.item(0);
                dcmProtocolElement.getFirstChild().setTextContent(DCMPROTOCOL);
            }
            log.info("XML element values have been updated Successfully.");
            printToFile();
        } catch (Exception e) {
            e.printStackTrace();
            log.error("Unable to update the element values of XML document", e);
            return;
        }
    }

    /**
	 * Getter for ServerConfiguration object.It reads the element values
	 * from the xml document.Creates a new ServerConfiguration object, sets
	 * the properties of serverConfiguration and returns the serverConfiguration.
	 * @return The ServerConfiguration object.
	 */
    public ServerConfiguration getElementValues() {
        DocumentBuilderFactory dbf = DocumentBuilderFactory.newInstance();
        DocumentBuilder db;
        URL url;
        ServerConfiguration serverConfiguration = new ServerConfiguration();
        try {
            db = dbf.newDocumentBuilder();
            String xmlFileName = this.getClass().getResource("/conf/oviyam-config.xml").toString();
            if (xmlFileName.indexOf("default") > 0) {
                File srcFile = new File(this.getClass().getResource("/conf/oviyam-config.xml").toURI());
                File destFile = new File(xmlFileName.substring(5, xmlFileName.indexOf("default")) + "default/" + fname);
                if (!destFile.exists()) {
                    FileInputStream in = new FileInputStream(srcFile);
                    FileOutputStream out = new FileOutputStream(destFile);
                    byte[] buf = new byte[1024];
                    int len;
                    while ((len = in.read(buf)) > 0) {
                        out.write(buf, 0, len);
                    }
                    in.close();
                    out.close();
                }
                url = destFile.toURL();
            } else {
                url = this.getClass().getResource("/conf/oviyam-config.xml");
            }
            InputSource is = new InputSource(url.openStream());
            dom = db.parse(is);
            NodeList root = dom.getElementsByTagName("server");
            for (int i = 0; i < root.getLength(); i++) {
                Element element = (Element) root.item(i);
                NodeList aetitle = element.getElementsByTagName("aetitle");
                Element aetitleElement = (Element) aetitle.item(0);
                serverConfiguration.setAeTitle(aetitleElement.getFirstChild().getNodeValue());
                NodeList hostname = element.getElementsByTagName("hostname");
                Element hostnameElement = (Element) hostname.item(0);
                serverConfiguration.setHostName(hostnameElement.getFirstChild().getNodeValue());
                NodeList port = element.getElementsByTagName("port");
                Element portElement = (Element) port.item(0);
                serverConfiguration.setPort(portElement.getFirstChild().getNodeValue());
                NodeList wadoport = element.getElementsByTagName("wadoport");
                Element wadoportElement = (Element) wadoport.item(0);
                serverConfiguration.setWadoPort(wadoportElement.getFirstChild().getNodeValue());
                NodeList dcmProtocol = element.getElementsByTagName("dcmprotocol");
                Element dcmProtocolElement = (Element) dcmProtocol.item(0);
                serverConfiguration.setDcmProtocol(dcmProtocolElement.getFirstChild().getNodeValue());
            }
        } catch (Exception e) {
            log.error("Unable to get the values from the XML document.", e);
            e.printStackTrace();
        }
        return serverConfiguration;
    }

    /**
	 * Creates the new xml document with the given element values.
	 * @param aeTitle
	 * @param hostName
	 * @param port
	 * @param wadoPort
	 * @param dcmProtocol
	 */
    public void creatXml(String aeTitle, String hostName, String port, String wadoPort, String dcmProtocol) {
        createAndParseXML();
        ServerConfiguration s = new ServerConfiguration();
        s.setAeTitle(aeTitle);
        s.setHostName(hostName);
        s.setPort(port);
        s.setWadoPort(wadoPort);
        s.setDcmProtocol(dcmProtocol);
        createRootElement(s);
        printToFile();
    }
}
