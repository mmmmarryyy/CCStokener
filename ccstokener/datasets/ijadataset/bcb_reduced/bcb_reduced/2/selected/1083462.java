package uk.ac.ncl.neresc.dynasoar.axis;

import org.apache.log4j.Logger;
import org.apache.axis.utils.XMLUtils;
import org.apache.axis.AxisFault;
import org.w3c.dom.*;
import org.xml.sax.SAXException;
import com.ibm.wsdl.xml.WSDLReaderImpl;
import com.ibm.wsdl.extensions.soap.SOAPAddressImpl;
import javax.wsdl.Binding;
import javax.wsdl.BindingOperation;
import javax.wsdl.Definition;
import javax.wsdl.Input;
import javax.wsdl.Message;
import javax.wsdl.Output;
import javax.wsdl.Part;
import javax.wsdl.Port;
import javax.wsdl.Service;
import javax.wsdl.WSDLException;
import javax.wsdl.factory.WSDLFactory;
import javax.wsdl.xml.WSDLReader;
import javax.wsdl.extensions.ExtensibilityElement;
import javax.wsdl.extensions.soap.SOAPBinding;
import javax.xml.parsers.ParserConfigurationException;
import java.io.*;
import java.net.*;
import java.util.*;
import java.nio.channels.ReadableByteChannel;
import java.nio.channels.Channels;
import java.nio.channels.FileChannel;

/**
 * Created by IntelliJ IDEA.
 * User: nam48
 * Date: 12-Feb-2007
 * Time: 15:43:15
 * To change this template use File | Settings | File Templates.
 */
public class WSDLFileHandler {

    private static Logger mLog = Logger.getLogger(WSDLFileHandler.class.getName());

    public static Vector getOperations(String serviceName, String wsdlURL, String svcURL) throws AxisFault {
        mLog.debug("Inside WSDLFileHandler:getOperations");
        Vector opNames = new Vector();
        try {
            WSDLFactory wsdlfactory = WSDLFactory.newInstance();
            WSDLReader wsdlReader = wsdlfactory.newWSDLReader();
            wsdlReader.setFeature("javax.wsdl.verbose", false);
            wsdlReader.setFeature("javax.wsdl.importDocuments", true);
            Definition wsdlDef = null;
            String wsdlFileLocation = System.getProperty("java.io.tmpdir") + File.separator + serviceName + ".wsdl";
            String endpoint = svcURL + "/" + serviceName;
            File wsdlFile = new File(wsdlFileLocation);
            boolean editRequired = false;
            if (!wsdlFile.exists()) {
                endpoint = svcURL + "/" + serviceName;
                wsdlFileLocation = WSDLFileHandler.getFile(serviceName, wsdlURL, endpoint);
            }
            mLog.debug("Trying to read WSDL file from: " + wsdlFileLocation);
            try {
                wsdlDef = wsdlReader.readWSDL(null, wsdlFileLocation);
            } catch (Exception ex) {
                mLog.debug("Failed");
                ex.printStackTrace();
            }
            if (wsdlDef == null) {
                mLog.debug("Why is this null???");
                return opNames;
            }
            Map ports = WSDLFileHandler.getPortTypes(wsdlDef);
            Collection portCollection = ports.values();
            Iterator portIt = portCollection.iterator();
            while (portIt.hasNext()) {
                Port port = (Port) portIt.next();
                mLog.debug("Port name = " + port.getName());
                List operations = port.getBinding().getBindingOperations();
                if (operations == null) {
                    mLog.debug("No operations defined for this portType");
                    continue;
                }
                Iterator opIt = operations.iterator();
                while (opIt.hasNext()) {
                    BindingOperation bindOper = (BindingOperation) opIt.next();
                    javax.wsdl.Operation oper = bindOper.getOperation();
                    String operationName = oper.getName();
                    mLog.debug("Adding operation name: " + operationName);
                    opNames.add(operationName);
                }
            }
        } catch (WSDLException ex) {
            mLog.error("WSDLException: " + ex.getMessage() + ", cause: " + ex.getCause().getMessage());
            ex.printStackTrace();
            throw new AxisFault(ex.getMessage(), ex.getCause());
        } catch (AxisFault ax) {
            mLog.error("AxisFault: " + ax.getMessage() + ", cause: " + ax.getCause().getMessage());
            ax.printStackTrace();
            throw ax;
        }
        return opNames;
    }

    private static Map getPortTypes(Definition wsdlDef) {
        mLog.debug("Trying to get portTypes");
        Map servs = wsdlDef.getServices();
        Collection servcol = servs.values();
        int count = servcol.size();
        mLog.debug("number of services defined in this WSDL = " + String.valueOf(count));
        Iterator it = servcol.iterator();
        Service serv = (javax.wsdl.Service) it.next();
        String serviceQName = serv.getQName().toString();
        String servicelocationURL = serv.getQName().getNamespaceURI();
        String serviceName = serv.getQName().getLocalPart();
        mLog.debug("Service QName = " + serviceQName);
        mLog.debug("Service location URI = " + servicelocationURL);
        mLog.debug("Service name= " + serviceName);
        Map ports = serv.getPorts();
        return ports;
    }

    public static String getFile(String serviceName, String wsdlLocation, String endpoint) throws AxisFault {
        mLog.debug("Downloading WSDL file from: " + wsdlLocation);
        mLog.debug("Received endpoint: " + endpoint);
        String fileLocation = null;
        try {
            String tempDir = System.getProperty("java.io.tmpdir");
            URL url = new URL(wsdlLocation);
            String WSDLFile = tempDir + File.separator + serviceName + ".wsdl";
            String tmpWSDLFile = WSDLFile + ".tmp";
            File inputFile = new File(WSDLFile);
            File tmpFile = new File(tmpWSDLFile);
            if (!inputFile.exists() || inputFile.length() == 0) {
                mLog.debug("Downloading the WSDL");
                inputFile.createNewFile();
                InputStream in = url.openStream();
                FileOutputStream out = new FileOutputStream(inputFile);
                URLConnection con = url.openConnection();
                int fileLength = con.getContentLength();
                ReadableByteChannel channelIn = Channels.newChannel(in);
                FileChannel channelOut = out.getChannel();
                channelOut.transferFrom(channelIn, 0, fileLength);
                channelIn.close();
                channelOut.close();
                out.flush();
                out.close();
                in.close();
                Document tmpDocument = XMLUtils.newDocument(new FileInputStream(inputFile));
                NodeList nl1 = tmpDocument.getElementsByTagName("wsdlsoap:address");
                for (int i = 0; i < nl1.getLength(); i++) {
                    Node node1 = nl1.item(i);
                    if (node1.getNodeName().equals("wsdlsoap:address")) {
                        ((Element) node1).setAttribute("location", endpoint);
                    }
                }
                FileOutputStream tmpOut = new FileOutputStream(tmpFile);
                XMLUtils.DocumentToStream(tmpDocument, tmpOut);
                tmpOut.flush();
                tmpOut.close();
                boolean retVal = inputFile.delete();
                if (retVal) {
                    retVal = tmpFile.renameTo(new File(WSDLFile));
                }
                mLog.debug("Return Value: " + retVal);
            } else {
                mLog.debug("The WSDL is already at the ServiceProvider");
            }
            fileLocation = WSDLFile;
        } catch (MalformedURLException mx) {
            mLog.error("MalformedURLException: " + mx.getMessage() + ", cause: " + mx.getCause().getMessage());
            throw new AxisFault(mx.getMessage(), mx.getCause());
        } catch (IOException ix) {
            mLog.error("IOException: " + ix.getMessage() + ", cause: " + ix.getCause().getMessage());
            throw new AxisFault(ix.getMessage(), ix.getCause());
        } catch (ParserConfigurationException px) {
            mLog.error("ParserConfigurationException: " + px.getMessage() + ", cause: " + px.getCause().getMessage());
            throw new AxisFault(px.getMessage(), px.getCause());
        } catch (SAXException sx) {
            mLog.error("SAXException: " + sx.getMessage() + ", cause: " + sx.getCause().getMessage());
            throw new AxisFault(sx.getMessage(), sx.getCause());
        }
        return fileLocation;
    }
}
