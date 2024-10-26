package jimo.osgi.modules.xml.api;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.net.URL;
import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.ParserConfigurationException;
import jimo.osgi.api.BundleService;
import jimo.osgi.api.util.LogUtil;
import org.osgi.framework.BundleContext;
import org.osgi.framework.ServiceReference;
import org.osgi.service.log.LogService;
import org.w3c.dom.Document;
import org.xml.sax.SAXException;

/**
 * This class reads an xml file from the bundles instance area.  If the file
 * does not exist, and a resource exists in the bundle with the same
 * filename, it is copied to the instance area before being parsed.
 * @author logicfish@hotmail.com
 *
 */
public class UtilXMLReader {

    private UtilXMLReader() {
    }

    /**
	 * Read an xml file using the bundle context. If getDataFile returns
	 * null the bundle contents is checked.  If the file is found it is
	 * copied to the instance area.
	 * @param xmlFilename
	 * @param context
	 * @return
	 */
    public static Document loadXML(String xmlFilename, BundleContext context) {
        LogService logger = LogUtil.getLogService(context);
        Document doc = null;
        try {
            File xmlFile = context.getDataFile(xmlFilename);
            DocumentBuilder docBuilder = DocumentBuilderFactory.newInstance().newDocumentBuilder();
            if (xmlFile.exists()) {
                doc = docBuilder.parse(xmlFile);
            } else {
                URL resource = context.getBundle().getResource(xmlFilename);
                if (resource == null) {
                    return doc;
                }
                InputStream in = resource.openStream();
                new File(xmlFile.getParent()).mkdirs();
                xmlFile.createNewFile();
                FileOutputStream out = new FileOutputStream(xmlFile);
                int c = 0;
                while ((c = in.read()) != -1) out.write(c);
                out.flush();
                out.close();
                doc = docBuilder.parse(xmlFile);
            }
        } catch (ParserConfigurationException e) {
            logger.log(LogService.LOG_ERROR, e.getMessage(), e);
        } catch (SAXException e) {
            logger.log(LogService.LOG_ERROR, e.getMessage(), e);
        } catch (IOException e) {
            logger.log(LogService.LOG_ERROR, e.getMessage(), e);
        }
        return doc;
    }

    /**
	 * Load xml from the filesystem
	 * @param file
	 * @return
	 */
    public static Document loadXML(File file) {
        DocumentBuilder docBuilder;
        try {
            docBuilder = DocumentBuilderFactory.newInstance().newDocumentBuilder();
            return docBuilder.parse(file);
        } catch (ParserConfigurationException e) {
            e.printStackTrace(System.err);
        } catch (SAXException e) {
            e.printStackTrace(System.err);
        } catch (IOException e) {
            e.printStackTrace(System.err);
        }
        return null;
    }

    /**
	 * Load xml from the application config folder
	 * @param xmlFilename
	 * @param context
	 * @return
	 */
    public static Document loadXMLConfig(String xmlFilename, BundleContext context) {
        Document doc = null;
        try {
            ServiceReference reference = context.getServiceReference(BundleService.class.getName());
            BundleService service = (BundleService) context.getService(reference);
            File xmlFile = service.openConfigFile(xmlFilename);
            DocumentBuilder docBuilder = DocumentBuilderFactory.newInstance().newDocumentBuilder();
            if (xmlFile.exists()) {
                doc = docBuilder.parse(xmlFile);
            }
        } catch (ParserConfigurationException pce) {
        } catch (SAXException saxe) {
        } catch (IOException ioe) {
        }
        return doc;
    }

    /**
	 * Load xml from a url
	 * @param url
	 * @return
	 */
    public static Document loadXML(URL url) {
        Document doc = null;
        try {
            DocumentBuilder docBuilder = DocumentBuilderFactory.newInstance().newDocumentBuilder();
            doc = docBuilder.parse(url.openStream());
        } catch (ParserConfigurationException pce) {
        } catch (SAXException saxe) {
        } catch (IOException ioe) {
        }
        return doc;
    }
}
