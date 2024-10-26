package org.extwind.osgi.console.ebr.service.internal;

import java.io.BufferedInputStream;
import java.io.BufferedOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.net.URL;
import java.util.HashMap;
import java.util.Map;
import java.util.jar.JarFile;
import java.util.zip.ZipEntry;
import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.ParserConfigurationException;
import javax.xml.transform.OutputKeys;
import javax.xml.transform.Transformer;
import javax.xml.transform.TransformerConfigurationException;
import javax.xml.transform.TransformerFactory;
import javax.xml.transform.dom.DOMSource;
import javax.xml.transform.stream.StreamResult;
import org.extwind.osgi.console.ebr.service.ScalableEbr;
import org.extwind.osgi.console.ebr.service.EbrBundle;
import org.osgi.framework.Version;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;
import org.xml.sax.EntityResolver;
import org.xml.sax.ErrorHandler;
import org.xml.sax.InputSource;
import org.xml.sax.SAXException;
import org.xml.sax.SAXParseException;

/**
 * @author donf.yang
 * 
 */
public class EBRHelper {

    private static final ErrorHandler errorHandler = new XMLErrorHandler();

    private static final EntityResolver entityResolver = new XMLEntityResolver();

    private static Transformer transformer = createTransformer();

    private static final Logger logger = LoggerFactory.getLogger(EBRHelper.class);

    private static final String BUNDLE_FILE_EXTENSION = ".jar";

    private static final String UNDERLINE = "_";

    private static final String SCHEMA_LANGUAGE_ATTRIBUTE = "http://java.sun.com/xml/jaxp/properties/schemaLanguage";

    private static final String XSD_SCHEMA_LANGUAGE = "http://www.w3.org/2001/XMLSchema";

    public static final String OSGI_BUNDLE_MANIFEST = "META-INF/MANIFEST.MF";

    public static final String MANIFEST = "MANIFEST.MF";

    private EBRHelper() {
    }

    public static void createConfigFile(File ebrBase) throws Exception {
        File configFile = new File(ebrBase, ScalableEbr.CONFIGFILE);
        if (configFile.exists()) {
            throw new Exception("Unable to create config file for ebr as it already exists - " + configFile.getAbsolutePath());
        }
        if (!configFile.createNewFile()) {
            throw new Exception("Unable to create config file - " + configFile.getAbsolutePath());
        }
        DocumentBuilderFactory factory = DocumentBuilderFactory.newInstance();
        factory.setValidating(false);
        factory.setNamespaceAware(true);
        Document document = factory.newDocumentBuilder().newDocument();
        Element rootElement = document.createElement(ScalableEbr.ELEMENT_EBR);
        rootElement.setAttribute("xmlns", "http://www.extwind.org/schema/ebr");
        rootElement.setAttribute("xmlns:xsi", "http://www.w3.org/2001/XMLSchema-instance");
        rootElement.setAttribute("xsi:schemaLocation", "http://www.extwind.org/schema/ebr http://www.extwind.org/schema/ebr.xsd");
        Element bundlesElement = document.createElement(ScalableEbr.ELEMENT_BUNDLES);
        rootElement.appendChild(bundlesElement);
        DOMSource source = new DOMSource(rootElement);
        StreamResult result = new StreamResult(configFile);
        transformer.transform(source, result);
    }

    public static void parse(File file, EBRImpl ebr, Map<String, EbrBundle> bundles) throws Exception {
        DocumentBuilder builder = createDocumentBuilder();
        InputStream stream = null;
        try {
            stream = new FileInputStream(file);
            Document document = builder.parse(new InputSource(stream));
            internalParse(document, ebr, bundles);
        } finally {
            if (stream != null) {
                try {
                    stream.close();
                } catch (Exception e) {
                }
            }
        }
    }

    private static void internalParse(Document document, EBRImpl ebr, Map<String, EbrBundle> bundles) throws Exception {
        Element root = document.getDocumentElement();
        NodeList nodes = root.getChildNodes();
        for (int i = 0; i < nodes.getLength(); i++) {
            Node node = nodes.item(i);
            if (!(node instanceof Element)) {
                continue;
            }
            Element element = (Element) node;
            String name = element.getAttribute(ScalableEbr.ATTRIBUTE_NAME);
            if (name != null && name.trim().length() > 0) {
                ebr.name = name;
            }
            String nodeName = element.getNodeName();
            if (nodeName.equals(ScalableEbr.ELEMENT_DESCRIPTION)) {
                ebr.description = element.getTextContent();
            } else if (nodeName.equals(ScalableEbr.ELEMENT_BUNDLES)) {
                internalParseBundles(element, bundles);
            }
        }
    }

    private static void internalParseBundles(Node bundlesNode, Map<String, EbrBundle> bundles) {
        NodeList nodes = bundlesNode.getChildNodes();
        for (int i = 0; i < nodes.getLength(); i++) {
            Node node = nodes.item(i);
            if (node.getNodeName().equals(ScalableEbr.ELEMENT_BUNDLE)) {
                EbrBundle bundle = internalParseBundle((Element) node);
                bundles.put(createBundleId(bundle), bundle);
            }
        }
    }

    private static EbrBundle internalParseBundle(Element bundleElement) {
        String name = bundleElement.getAttribute(ScalableEbr.ATTRIBUTE_NAME);
        String version = bundleElement.getAttribute(ScalableEbr.ATTRIBUTE_VERSION);
        EBRBundleImpl bundle = new EBRBundleImpl(name, new Version(version));
        NodeList nodes = bundleElement.getChildNodes();
        for (int i = 0; i < nodes.getLength(); i++) {
            Node node = nodes.item(i);
            String nodeName = node.getNodeName();
            if (nodeName.equals(ScalableEbr.ELEMENT_DESCRIPTION)) {
                String repository = node.getTextContent();
                bundle.addRepository(repository);
            }
        }
        return bundle;
    }

    public static String createBundleId(EbrBundle bundle) {
        return bundle.getSymbolicName() + "_" + bundle.getVersion().toString();
    }

    public static DocumentBuilder createDocumentBuilder() throws ParserConfigurationException {
        DocumentBuilderFactory factory = DocumentBuilderFactory.newInstance();
        factory.setValidating(true);
        factory.setNamespaceAware(true);
        try {
            factory.setAttribute(SCHEMA_LANGUAGE_ATTRIBUTE, XSD_SCHEMA_LANGUAGE);
        } catch (IllegalArgumentException ex) {
            ParserConfigurationException pcex = new ParserConfigurationException("Unable to validate using XSD: Your JAXP provider [" + factory + "] does not support XML Schema. Are you running on Java 1.4 with Apache Crimson? " + "Upgrade to Apache Xerces (or Java 1.5) for full XSD support.");
            pcex.initCause(ex);
            throw pcex;
        }
        DocumentBuilder builder = factory.newDocumentBuilder();
        builder.setErrorHandler(errorHandler);
        builder.setEntityResolver(entityResolver);
        return builder;
    }

    private static class XMLErrorHandler implements ErrorHandler {

        public void error(SAXParseException exception) throws SAXException {
            throw exception;
        }

        public void fatalError(SAXParseException exception) throws SAXException {
            throw exception;
        }

        public void warning(SAXParseException exception) throws SAXException {
            logger.warn("Ignored XML validation warning", exception);
        }
    }

    private static class XMLEntityResolver implements EntityResolver {

        protected static final String SCHEMA_COMPONENT = "http://www.extwind.org/schema/ebr.xsd";

        protected static final String SCHEMA_COMPONENT_RESOURCE = "org/extwind/osgi/console/ebr/schema/ebr.xsd";

        protected static final Map<String, String> schemaMapping = new HashMap<String, String>(1);

        static {
            schemaMapping.put(SCHEMA_COMPONENT, SCHEMA_COMPONENT_RESOURCE);
        }

        public InputSource resolveEntity(String publicId, String systemId) throws SAXException, IOException {
            String resource = schemaMapping.get(systemId);
            if (resource != null) {
                URL url = getClass().getClassLoader().getResource(resource);
                return new InputSource(url.openStream());
            }
            return null;
        }
    }

    private static Transformer createTransformer() {
        TransformerFactory transformerFactory = TransformerFactory.newInstance();
        Transformer transformer = null;
        try {
            transformer = transformerFactory.newTransformer();
            transformer.setOutputProperty(OutputKeys.INDENT, "yes");
            transformer.setOutputProperty(OutputKeys.METHOD, "xml");
        } catch (TransformerConfigurationException e) {
            e.printStackTrace();
        }
        return transformer;
    }

    public static synchronized void removeBundleFromConfigFile(String ebrBase, EbrBundle bundle) throws Exception {
        File configFile = new File(ebrBase, ScalableEbr.CONFIGFILE);
        if (!configFile.exists()) {
            throw new Exception("Missing EBR configration file - " + configFile.getAbsolutePath());
        }
        DocumentBuilder builder = createDocumentBuilder();
        InputStream stream = null;
        try {
            stream = new FileInputStream(configFile);
            Document document = builder.parse(new InputSource(stream));
            Node bundlesNode = document.getElementsByTagName(ScalableEbr.ELEMENT_BUNDLES).item(0);
            NodeList nodes = bundlesNode.getChildNodes();
            for (int i = 0; i < nodes.getLength(); i++) {
                Node node = nodes.item(i);
                if (node instanceof Element) {
                    Element bundleNode = (Element) node;
                    String name = bundleNode.getAttribute(ScalableEbr.ATTRIBUTE_NAME);
                    String version = bundleNode.getAttribute(ScalableEbr.ATTRIBUTE_VERSION);
                    if (name.equals(bundle.getSymbolicName()) && version.equals(bundle.getVersion().toString())) {
                        bundlesNode.removeChild(bundleNode);
                        DOMSource source = new DOMSource(document);
                        StreamResult result = new StreamResult(configFile);
                        createTransformer().transform(source, result);
                        return;
                    }
                }
            }
        } finally {
            if (stream != null) {
                try {
                    stream.close();
                } catch (Exception e) {
                }
            }
        }
    }

    public static synchronized void addBundleToConfigFile(String ebrBase, EbrBundle bundle) throws Exception {
        File configFile = new File(ebrBase, ScalableEbr.CONFIGFILE);
        if (!configFile.exists()) {
            throw new Exception("Missing EBR configration file - " + configFile.getAbsolutePath());
        }
        DocumentBuilder builder = createDocumentBuilder();
        InputStream stream = null;
        try {
            stream = new FileInputStream(configFile);
            Document document = builder.parse(new InputSource(stream));
            Node bundlesNode = document.getElementsByTagName(ScalableEbr.ELEMENT_BUNDLES).item(0);
            bundlesNode.appendChild(createBundleElement(bundle, document));
            DOMSource source = new DOMSource(document);
            StreamResult result = new StreamResult(configFile);
            transformer.transform(source, result);
        } finally {
            if (stream != null) {
                try {
                    stream.close();
                } catch (Exception e) {
                }
            }
        }
    }

    private static Element createBundleElement(EbrBundle bundle, Document document) {
        Element bundleElement = document.createElement(ScalableEbr.ELEMENT_BUNDLE);
        bundleElement.setAttribute(ScalableEbr.ATTRIBUTE_NAME, bundle.getSymbolicName());
        bundleElement.setAttribute(ScalableEbr.ATTRIBUTE_VERSION, bundle.getVersion().toString());
        for (String repo : bundle.getRepositories()) {
            Element repoElement = document.createElement(ScalableEbr.ELEMENT_REPOSITORY);
            repoElement.setTextContent(repo);
            bundleElement.appendChild(repoElement);
        }
        return bundleElement;
    }

    public static synchronized void removeBundle(String bundlePath, EbrBundle bundle) throws Exception {
        String bundleFileName = getBundleFileName(bundle);
        File bundleFile = new File(bundlePath, bundleFileName);
        bundleFile.delete();
        File manifest = new File(bundlePath, MANIFEST);
        manifest.delete();
        File parent = bundleFile.getParentFile();
        String[] list = parent.list();
        while (list != null && parent.list().length == 0) {
            parent.delete();
            parent = parent.getParentFile();
        }
    }

    public static synchronized void writerBundle(String bundlePath, EbrBundle bundle, InputStream stream) throws Exception {
        File baseFile = new File(bundlePath);
        String bundleFileName = getBundleFileName(bundle);
        File bundleFile = new File(bundlePath, bundleFileName);
        if (bundleFile.exists()) {
            throw new Exception("Bundle already exists - " + bundleFile.getAbsolutePath());
        }
        if (!baseFile.exists() && !baseFile.mkdirs()) {
            throw new Exception("Unable create bundle dir - " + baseFile.getAbsolutePath());
        }
        logger.debug("Create temp bundle file - " + bundleFile.getAbsolutePath());
        bundleFile.createNewFile();
        writeFile(bundleFile, stream);
        try {
            JarFile jarFile = new JarFile(bundleFile);
            writeManifest(bundlePath, jarFile);
        } catch (Exception e) {
            try {
                removeBundle(bundlePath, bundle);
            } catch (Exception ex) {
            }
            throw e;
        }
    }

    public static synchronized void writeManifest(String basePath, JarFile jarFile) throws Exception {
        ZipEntry zipEntry = jarFile.getEntry(OSGI_BUNDLE_MANIFEST);
        InputStream stream = jarFile.getInputStream(zipEntry);
        File file = new File(basePath, MANIFEST);
        file.createNewFile();
        writeFile(file, stream);
    }

    public static String getBundleFileName(EbrBundle bundle) {
        return bundle.getSymbolicName() + UNDERLINE + bundle.getVersion().toString() + BUNDLE_FILE_EXTENSION;
    }

    public static void writeFile(File file, InputStream stream) throws Exception {
        BufferedInputStream bis = null;
        BufferedOutputStream bos = null;
        try {
            bis = new BufferedInputStream(stream);
            bos = new BufferedOutputStream(new FileOutputStream(file));
            byte[] buffer = new byte[1024];
            int read = -1;
            while ((read = bis.read(buffer)) != -1) {
                bos.write(buffer, 0, read);
            }
            bos.flush();
        } finally {
            if (bis != null) {
                try {
                    bis.close();
                } catch (IOException e) {
                }
            }
            if (bos != null) {
                try {
                    bos.close();
                } catch (IOException e) {
                }
            }
        }
    }
}
