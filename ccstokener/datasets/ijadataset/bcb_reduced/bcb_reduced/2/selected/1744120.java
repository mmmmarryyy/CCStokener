package com.android.sdklib.internal.repository;

import com.android.sdklib.annotations.VisibleForTesting;
import com.android.sdklib.annotations.VisibleForTesting.Visibility;
import com.android.sdklib.repository.SdkAddonsListConstants;
import org.w3c.dom.Document;
import org.w3c.dom.NamedNodeMap;
import org.w3c.dom.Node;
import org.xml.sax.InputSource;
import org.xml.sax.SAXException;
import org.xml.sax.SAXParseException;
import java.io.ByteArrayInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;
import java.net.URL;
import java.util.ArrayList;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import javax.net.ssl.SSLKeyException;
import javax.xml.XMLConstants;
import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.ParserConfigurationException;
import javax.xml.transform.stream.StreamSource;
import javax.xml.validation.Schema;
import javax.xml.validation.SchemaFactory;
import javax.xml.validation.Validator;

/**
 * Fetches and loads an sdk-addons-list XML.
 * <p/>
 * Such an XML contains a simple list of add-ons site that are to be loaded by default by the
 * SDK Manager. <br/>
 * The XML must conform to the sdk-addons-list-N.xsd. <br/>
 * Constants used in the XML are defined in {@link SdkAddonsListConstants}.
 */
public class AddonsListFetcher {

    /**
     * An immutable structure representing an add-on site.
     */
    public static class Site {

        private final String mUrl;

        private final String mUiName;

        private Site(String url, String uiName) {
            mUrl = url.trim();
            mUiName = uiName;
        }

        public String getUrl() {
            return mUrl;
        }

        public String getUiName() {
            return mUiName;
        }
    }

    /**
     * Fetches the addons list from the given URL.
     *
     * @param monitor A monitor to report errors. Cannot be null.
     * @param url The URL of an XML file resource that conforms to the latest sdk-addons-list-N.xsd.
     *   For the default operation, use {@link SdkAddonsListConstants#URL_ADDON_LIST}.
     *   Cannot be null.
     * @return An array of {@link Site} on success (possibly empty), or null on error.
     */
    public Site[] fetch(ITaskMonitor monitor, String url) {
        url = url == null ? "" : url.trim();
        monitor.setProgressMax(4);
        monitor.setDescription("Fetching %1$s", url);
        monitor.incProgress(1);
        Exception[] exception = new Exception[] { null };
        Boolean[] validatorFound = new Boolean[] { Boolean.FALSE };
        String[] validationError = new String[] { null };
        Document validatedDoc = null;
        String validatedUri = null;
        ByteArrayInputStream xml = fetchUrl(url, exception);
        if (xml != null) {
            monitor.setDescription("Validate XML");
            int version = getXmlSchemaVersion(xml);
            if (version >= 1 && version <= SdkAddonsListConstants.NS_LATEST_VERSION) {
                String uri = validateXml(xml, url, version, validationError, validatorFound);
                if (uri != null) {
                    validatedDoc = getDocument(xml, monitor);
                    validatedUri = uri;
                }
            } else if (version > SdkAddonsListConstants.NS_LATEST_VERSION) {
                return null;
            }
        }
        if (exception[0] != null) {
            String reason = null;
            if (exception[0] instanceof FileNotFoundException) {
                reason = "File not found";
            } else if (exception[0] instanceof SSLKeyException) {
                reason = "HTTPS SSL error. You might want to force download through HTTP in the settings.";
            } else if (exception[0].getMessage() != null) {
                reason = exception[0].getMessage();
            } else {
                reason = String.format("Unknown (%1$s)", exception[0].getClass().getName());
            }
            monitor.setResult("Failed to fetch URL %1$s, reason: %2$s", url, reason);
        }
        if (validationError[0] != null) {
            monitor.setResult("%s", validationError[0]);
        }
        if (validatedDoc == null) {
            return null;
        }
        monitor.incProgress(1);
        Site[] result = null;
        if (xml != null) {
            monitor.setDescription("Parse XML");
            monitor.incProgress(1);
            result = parseAddonsList(validatedDoc, validatedUri, monitor);
        }
        monitor.incProgress(1);
        return result;
    }

    /**
     * Fetches the document at the given URL and returns it as a stream.
     * Returns null if anything wrong happens.
     *
     * References: <br/>
     * Java URL Connection: http://java.sun.com/docs/books/tutorial/networking/urls/readingWriting.html <br/>
     * Java URL Reader: http://java.sun.com/docs/books/tutorial/networking/urls/readingURL.html <br/>
     * Java set Proxy: http://java.sun.com/docs/books/tutorial/networking/urls/_setProxy.html <br/>
     *
     * @param urlString The URL to load, as a string.
     * @param outException If non null, where to store any exception that happens during the fetch.
     */
    private ByteArrayInputStream fetchUrl(String urlString, Exception[] outException) {
        URL url;
        try {
            url = new URL(urlString);
            InputStream is = null;
            int inc = 65536;
            int curr = 0;
            byte[] result = new byte[inc];
            try {
                is = url.openStream();
                int n;
                while ((n = is.read(result, curr, result.length - curr)) != -1) {
                    curr += n;
                    if (curr == result.length) {
                        byte[] temp = new byte[curr + inc];
                        System.arraycopy(result, 0, temp, 0, curr);
                        result = temp;
                    }
                }
                return new ByteArrayInputStream(result, 0, curr);
            } finally {
                if (is != null) {
                    try {
                        is.close();
                    } catch (IOException e) {
                    }
                }
            }
        } catch (Exception e) {
            if (outException != null) {
                outException[0] = e;
            }
        }
        return null;
    }

    /**
     * Manually parses the root element of the XML to extract the schema version
     * at the end of the xmlns:sdk="http://schemas.android.com/sdk/android/addons-list/$N"
     * declaration.
     *
     * @return 1..{@link SdkAddonsListConstants#NS_LATEST_VERSION} for a valid schema version
     *         or 0 if no schema could be found.
     */
    @VisibleForTesting(visibility = Visibility.PRIVATE)
    protected int getXmlSchemaVersion(InputStream xml) {
        if (xml == null) {
            return 0;
        }
        Document doc = null;
        try {
            xml.reset();
            DocumentBuilderFactory factory = DocumentBuilderFactory.newInstance();
            factory.setIgnoringComments(false);
            factory.setValidating(false);
            factory.setNamespaceAware(false);
            DocumentBuilder builder = factory.newDocumentBuilder();
            doc = builder.parse(xml);
            factory.setNamespaceAware(true);
            builder = factory.newDocumentBuilder();
        } catch (Exception e) {
        }
        if (doc == null) {
            return 0;
        }
        Pattern nsPattern = Pattern.compile(SdkAddonsListConstants.NS_PATTERN);
        String prefix = null;
        for (Node child = doc.getFirstChild(); child != null; child = child.getNextSibling()) {
            if (child.getNodeType() == Node.ELEMENT_NODE) {
                prefix = null;
                String name = child.getNodeName();
                int pos = name.indexOf(':');
                if (pos > 0 && pos < name.length() - 1) {
                    prefix = name.substring(0, pos);
                    name = name.substring(pos + 1);
                }
                if (SdkAddonsListConstants.NODE_SDK_ADDONS_LIST.equals(name)) {
                    NamedNodeMap attrs = child.getAttributes();
                    String xmlns = "xmlns";
                    if (prefix != null) {
                        xmlns += ":" + prefix;
                    }
                    Node attr = attrs.getNamedItem(xmlns);
                    if (attr != null) {
                        String uri = attr.getNodeValue();
                        if (uri != null) {
                            Matcher m = nsPattern.matcher(uri);
                            if (m.matches()) {
                                String version = m.group(1);
                                try {
                                    return Integer.parseInt(version);
                                } catch (NumberFormatException e) {
                                    return 0;
                                }
                            }
                        }
                    }
                }
            }
        }
        return 0;
    }

    /**
     * Validates this XML against one of the requested SDK Repository schemas.
     * If the XML was correctly validated, returns the schema that worked.
     * If it doesn't validate, returns null and stores the error in outError[0].
     * If we can't find a validator, returns null and set validatorFound[0] to false.
     */
    @VisibleForTesting(visibility = Visibility.PRIVATE)
    protected String validateXml(InputStream xml, String url, int version, String[] outError, Boolean[] validatorFound) {
        if (xml == null) {
            return null;
        }
        try {
            Validator validator = getValidator(version);
            if (validator == null) {
                validatorFound[0] = Boolean.FALSE;
                outError[0] = String.format("XML verification failed for %1$s.\nNo suitable XML Schema Validator could be found in your Java environment. Please consider updating your version of Java.", url);
                return null;
            }
            validatorFound[0] = Boolean.TRUE;
            xml.reset();
            validator.validate(new StreamSource(xml));
            return SdkAddonsListConstants.getSchemaUri(version);
        } catch (SAXParseException e) {
            outError[0] = String.format("XML verification failed for %1$s.\nLine %2$d:%3$d, Error: %4$s", url, e.getLineNumber(), e.getColumnNumber(), e.toString());
        } catch (Exception e) {
            outError[0] = String.format("XML verification failed for %1$s.\nError: %2$s", url, e.toString());
        }
        return null;
    }

    /**
     * Helper method that returns a validator for our XSD, or null if the current Java
     * implementation can't process XSD schemas.
     *
     * @param version The version of the XML Schema.
     *        See {@link SdkAddonsListConstants#getXsdStream(int)}
     */
    private Validator getValidator(int version) throws SAXException {
        InputStream xsdStream = SdkAddonsListConstants.getXsdStream(version);
        SchemaFactory factory = SchemaFactory.newInstance(XMLConstants.W3C_XML_SCHEMA_NS_URI);
        if (factory == null) {
            return null;
        }
        Schema schema = factory.newSchema(new StreamSource(xsdStream));
        Validator validator = schema == null ? null : schema.newValidator();
        return validator;
    }

    /**
     * Takes an XML document as a string as parameter and returns a DOM for it.
     *
     * On error, returns null and prints a (hopefully) useful message on the monitor.
     */
    @VisibleForTesting(visibility = Visibility.PRIVATE)
    protected Document getDocument(InputStream xml, ITaskMonitor monitor) {
        try {
            DocumentBuilderFactory factory = DocumentBuilderFactory.newInstance();
            factory.setIgnoringComments(true);
            factory.setNamespaceAware(true);
            DocumentBuilder builder = factory.newDocumentBuilder();
            xml.reset();
            Document doc = builder.parse(new InputSource(xml));
            return doc;
        } catch (ParserConfigurationException e) {
            monitor.setResult("Failed to create XML document builder");
        } catch (SAXException e) {
            monitor.setResult("Failed to parse XML document");
        } catch (IOException e) {
            monitor.setResult("Failed to read XML document");
        }
        return null;
    }

    /**
     * Parse all sites defined in the Addaons list XML and returns an array of sites.
     */
    @VisibleForTesting(visibility = Visibility.PRIVATE)
    protected Site[] parseAddonsList(Document doc, String nsUri, ITaskMonitor monitor) {
        Node root = getFirstChild(doc, nsUri, SdkAddonsListConstants.NODE_SDK_ADDONS_LIST);
        if (root != null) {
            ArrayList<Site> sites = new ArrayList<Site>();
            for (Node child = root.getFirstChild(); child != null; child = child.getNextSibling()) {
                if (child.getNodeType() == Node.ELEMENT_NODE && nsUri.equals(child.getNamespaceURI()) && child.getLocalName().equals(SdkAddonsListConstants.NODE_ADDON_SITE)) {
                    Node url = getFirstChild(child, nsUri, SdkAddonsListConstants.NODE_URL);
                    Node name = getFirstChild(child, nsUri, SdkAddonsListConstants.NODE_NAME);
                    if (name != null && url != null) {
                        String strUrl = url.getTextContent().trim();
                        String strName = name.getTextContent().trim();
                        if (strUrl.length() > 0 && strName.length() > 0) {
                            sites.add(new Site(strUrl, strName));
                        }
                    }
                }
            }
            return sites.toArray(new Site[sites.size()]);
        }
        return null;
    }

    /**
     * Returns the first child element with the given XML local name.
     * If xmlLocalName is null, returns the very first child element.
     */
    private Node getFirstChild(Node node, String nsUri, String xmlLocalName) {
        for (Node child = node.getFirstChild(); child != null; child = child.getNextSibling()) {
            if (child.getNodeType() == Node.ELEMENT_NODE && nsUri.equals(child.getNamespaceURI())) {
                if (xmlLocalName == null || child.getLocalName().equals(xmlLocalName)) {
                    return child;
                }
            }
        }
        return null;
    }
}
