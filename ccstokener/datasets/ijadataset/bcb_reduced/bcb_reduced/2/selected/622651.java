package com.android.sdklib.internal.repository;

import com.android.sdklib.internal.repository.Archive.Arch;
import com.android.sdklib.internal.repository.Archive.Os;
import com.android.sdklib.repository.SdkRepository;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.NamedNodeMap;
import org.w3c.dom.Node;
import org.xml.sax.InputSource;
import org.xml.sax.SAXException;
import java.io.ByteArrayInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;
import java.net.URL;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
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
 * An sdk-repository source, i.e. a download site.
 * It may be a full repository or an add-on only repository.
 * A repository describes one or {@link Package}s available for download.
 */
public class RepoSource implements IDescription {

    private String mUrl;

    private final boolean mUserSource;

    private Package[] mPackages;

    private String mDescription;

    private String mFetchError;

    /**
     * Constructs a new source for the given repository URL.
     * @param url The source URL. Cannot be null. If the URL ends with a /, the default
     *            repository.xml filename will be appended automatically.
     * @param userSource True if this a user source (add-ons & packages only.)
     */
    public RepoSource(String url, boolean userSource) {
        if (url.endsWith("/")) {
            url += SdkRepository.URL_DEFAULT_XML_FILE;
        }
        mUrl = url;
        mUserSource = userSource;
        setDefaultDescription();
    }

    /**
     * Two repo source are equal if they have the same userSource flag and the same URL.
     */
    @Override
    public boolean equals(Object obj) {
        if (obj instanceof RepoSource) {
            RepoSource rs = (RepoSource) obj;
            return rs.isUserSource() == this.isUserSource() && rs.getUrl().equals(this.getUrl());
        }
        return false;
    }

    @Override
    public int hashCode() {
        return mUrl.hashCode() ^ Boolean.valueOf(mUserSource).hashCode();
    }

    /** Returns true if this is a user source. We only load addon and extra packages
     * from a user source and ignore the rest. */
    public boolean isUserSource() {
        return mUserSource;
    }

    /** Returns the URL of the repository.xml file for this source. */
    public String getUrl() {
        return mUrl;
    }

    /**
     * Returns the list of known packages found by the last call to load().
     * This is null when the source hasn't been loaded yet.
     */
    public Package[] getPackages() {
        return mPackages;
    }

    /**
     * Clear the internal packages list. After this call, {@link #getPackages()} will return
     * null till load() is called.
     */
    public void clearPackages() {
        mPackages = null;
    }

    public String getShortDescription() {
        return mUrl;
    }

    public String getLongDescription() {
        return mDescription == null ? "" : mDescription;
    }

    /**
     * Returns the last fetch error description.
     * If there was no error, returns null.
     */
    public String getFetchError() {
        return mFetchError;
    }

    /**
     * Tries to fetch the repository index for the given URL.
     */
    public void load(ITaskMonitor monitor, boolean forceHttp) {
        monitor.setProgressMax(4);
        setDefaultDescription();
        String url = mUrl;
        if (forceHttp) {
            url = url.replaceAll("https://", "http://");
        }
        monitor.setDescription("Fetching %1$s", url);
        monitor.incProgress(1);
        mFetchError = null;
        String[] validationError = new String[] { null };
        Exception[] exception = new Exception[] { null };
        ByteArrayInputStream xml = fetchUrl(url, exception);
        Document validatedDoc = null;
        boolean usingAlternateXml = false;
        String validatedUri = null;
        if (xml != null) {
            monitor.setDescription("Validate XML");
            String uri = validateXml(xml, url, validationError);
            if (uri != null) {
                validatedDoc = getDocument(xml, monitor);
                validatedUri = uri;
            } else {
                validatedDoc = findAlternateToolsXml(xml);
                if (validatedDoc != null) {
                    validationError[0] = null;
                    validatedUri = SdkRepository.NS_SDK_REPOSITORY;
                    usingAlternateXml = true;
                }
            }
        }
        if (validatedDoc == null && !url.endsWith(SdkRepository.URL_DEFAULT_XML_FILE)) {
            if (!url.endsWith("/")) {
                url += "/";
            }
            url += SdkRepository.URL_DEFAULT_XML_FILE;
            xml = fetchUrl(url, exception);
            if (xml != null) {
                String uri = validateXml(xml, url, validationError);
                if (uri != null) {
                    validatedDoc = getDocument(xml, monitor);
                    validatedUri = uri;
                } else {
                    validatedDoc = findAlternateToolsXml(xml);
                    if (validatedDoc != null) {
                        validationError[0] = null;
                        validatedUri = SdkRepository.NS_SDK_REPOSITORY;
                        usingAlternateXml = true;
                    }
                }
            }
            if (validatedDoc != null) {
                monitor.setResult("Repository found at %1$s", url);
                mUrl = url;
            }
        }
        if (exception[0] != null) {
            mFetchError = "Failed to fetch URL";
            String reason = null;
            if (exception[0] instanceof FileNotFoundException) {
                reason = "File not found";
                mFetchError += ": " + reason;
            } else if (exception[0] instanceof SSLKeyException) {
                reason = "HTTPS SSL error. You might want to force download through HTTP in the settings.";
                mFetchError += ": HTTPS SSL error";
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
            return;
        }
        if (usingAlternateXml) {
            String info = "This repository requires a more recent version of the Tools. Please update.";
            mFetchError = mFetchError == null ? info : mFetchError + ". " + info;
            mDescription = "This repository requires a more recent version of the Tools.\nYou must update it before you can see other new packages.";
        }
        monitor.incProgress(1);
        if (xml != null) {
            monitor.setDescription("Parse XML");
            monitor.incProgress(1);
            parsePackages(validatedDoc, validatedUri, monitor);
            if (mPackages == null || mPackages.length == 0) {
                mDescription += "\nNo packages found.";
            } else if (mPackages.length == 1) {
                mDescription += "\nOne package found.";
            } else {
                mDescription += String.format("\n%1$d packages found.", mPackages.length);
            }
        }
        monitor.incProgress(1);
    }

    private void setDefaultDescription() {
        if (mUserSource) {
            mDescription = String.format("Add-on Source: %1$s", mUrl);
        } else {
            mDescription = String.format("SDK Source: %1$s", mUrl);
        }
    }

    /**
     * Fetches the document at the given URL and returns it as a string.
     * Returns null if anything wrong happens and write errors to the monitor.
     *
     * References:
     * Java URL Connection: http://java.sun.com/docs/books/tutorial/networking/urls/readingWriting.html
     * Java URL Reader: http://java.sun.com/docs/books/tutorial/networking/urls/readingURL.html
     * Java set Proxy: http://java.sun.com/docs/books/tutorial/networking/urls/_setProxy.html
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
            outException[0] = e;
        }
        return null;
    }

    /**
     * Validates this XML against one of the possible SDK Repository schema, starting
     * by the most recent one.
     * If the XML was correctly validated, returns the schema that worked.
     * If no schema validated the XML, returns null.
     */
    private String validateXml(ByteArrayInputStream xml, String url, String[] outError) {
        String lastError = null;
        String extraError = null;
        for (int version = SdkRepository.NS_LATEST_VERSION; version >= 1; version--) {
            try {
                Validator validator = getValidator(version);
                if (validator == null) {
                    lastError = "XML verification failed for %1$s.\nNo suitable XML Schema Validator could be found in your Java environment. Please consider updating your version of Java.";
                    continue;
                }
                xml.reset();
                validator.validate(new StreamSource(xml));
                return SdkRepository.getSchemaUri(version);
            } catch (Exception e) {
                lastError = "XML verification failed for %1$s.\nError: %2$s";
                extraError = e.getMessage();
                if (extraError == null) {
                    extraError = e.getClass().getName();
                }
            }
        }
        if (lastError != null) {
            outError[0] = String.format(lastError, url, extraError);
        }
        return null;
    }

    /**
     * The purpose of this method is to support forward evolution of our schema.
     * <p/>
     * At this point, we know that xml does not point to any schema that this version of
     * the tool know how to process, so it's not one of the possible 1..N versions of our
     * XSD schema.
     * <p/>
     * We thus try to interpret the byte stream as a possible XML stream. It may not be
     * one at all in the first place. If it looks anything line an XML schema, we try to
     * find its &lt;tool&gt; elements. If we find any, we recreate a suitable document
     * that conforms to what we expect from our XSD schema with only those elements.
     * To be valid, the &lt;tool&gt; element must have at least one &lt;archive&gt;
     * compatible with this platform.
     *
     * If we don't find anything suitable, we drop the whole thing.
     *
     * @param xml The input XML stream. Can be null.
     * @return Either a new XML document conforming to our schema with at least one &lt;tool&gt;
     *         element or null.
     */
    protected Document findAlternateToolsXml(InputStream xml) {
        if (xml == null) {
            return null;
        }
        try {
            xml.reset();
        } catch (IOException e1) {
        }
        Document oldDoc = null;
        Document newDoc = null;
        try {
            DocumentBuilderFactory factory = DocumentBuilderFactory.newInstance();
            factory.setIgnoringComments(false);
            factory.setValidating(false);
            factory.setNamespaceAware(false);
            DocumentBuilder builder = factory.newDocumentBuilder();
            oldDoc = builder.parse(xml);
            factory.setNamespaceAware(true);
            builder = factory.newDocumentBuilder();
            newDoc = builder.newDocument();
        } catch (Exception e) {
        }
        if (oldDoc == null || newDoc == null) {
            return null;
        }
        Pattern nsPattern = Pattern.compile(SdkRepository.NS_SDK_REPOSITORY_PATTERN);
        Node oldRoot = null;
        String prefix = null;
        for (Node child = oldDoc.getFirstChild(); child != null; child = child.getNextSibling()) {
            if (child.getNodeType() == Node.ELEMENT_NODE) {
                prefix = null;
                String name = child.getNodeName();
                int pos = name.indexOf(':');
                if (pos > 0 && pos < name.length() - 1) {
                    prefix = name.substring(0, pos);
                    name = name.substring(pos + 1);
                }
                if (SdkRepository.NODE_SDK_REPOSITORY.equals(name)) {
                    NamedNodeMap attrs = child.getAttributes();
                    String xmlns = "xmlns";
                    if (prefix != null) {
                        xmlns += ":" + prefix;
                    }
                    Node attr = attrs.getNamedItem(xmlns);
                    if (attr != null) {
                        String uri = attr.getNodeValue();
                        if (uri != null && nsPattern.matcher(uri).matches()) {
                            oldRoot = child;
                            break;
                        }
                    }
                }
            }
        }
        if (oldRoot == null || prefix == null || prefix.length() == 0) {
            return null;
        }
        final String ns = SdkRepository.NS_SDK_REPOSITORY;
        Element newRoot = newDoc.createElementNS(ns, SdkRepository.NODE_SDK_REPOSITORY);
        newRoot.setPrefix(prefix);
        newDoc.appendChild(newRoot);
        int numTool = 0;
        Node tool = null;
        while ((tool = findChild(oldRoot, tool, prefix, SdkRepository.NODE_TOOL)) != null) {
            try {
                Node revision = findChild(tool, null, prefix, SdkRepository.NODE_REVISION);
                Node archives = findChild(tool, null, prefix, SdkRepository.NODE_ARCHIVES);
                if (revision == null || archives == null) {
                    continue;
                }
                int rev = 0;
                try {
                    String content = revision.getTextContent();
                    content = content.trim();
                    rev = Integer.parseInt(content);
                    if (rev < 1) {
                        continue;
                    }
                } catch (NumberFormatException ignore) {
                    continue;
                }
                Element newTool = newDoc.createElementNS(ns, SdkRepository.NODE_TOOL);
                newTool.setPrefix(prefix);
                appendChild(newTool, ns, prefix, SdkRepository.NODE_REVISION, Integer.toString(rev));
                Element newArchives = appendChild(newTool, ns, prefix, SdkRepository.NODE_ARCHIVES, null);
                int numArchives = 0;
                Node archive = null;
                while ((archive = findChild(archives, archive, prefix, SdkRepository.NODE_ARCHIVE)) != null) {
                    try {
                        Os os = (Os) XmlParserUtils.getEnumAttribute(archive, SdkRepository.ATTR_OS, Os.values(), null);
                        Arch arch = (Arch) XmlParserUtils.getEnumAttribute(archive, SdkRepository.ATTR_ARCH, Arch.values(), Arch.ANY);
                        if (os == null || !os.isCompatible() || arch == null || !arch.isCompatible()) {
                            continue;
                        }
                        Node node = findChild(archive, null, prefix, SdkRepository.NODE_URL);
                        String url = node == null ? null : node.getTextContent().trim();
                        if (url == null || url.length() == 0) {
                            continue;
                        }
                        node = findChild(archive, null, prefix, SdkRepository.NODE_SIZE);
                        long size = 0;
                        try {
                            size = Long.parseLong(node.getTextContent());
                        } catch (Exception e) {
                        }
                        if (size < 1) {
                            continue;
                        }
                        node = findChild(archive, null, prefix, SdkRepository.NODE_CHECKSUM);
                        if (node == null) {
                            continue;
                        }
                        NamedNodeMap attrs = node.getAttributes();
                        Node typeNode = attrs.getNamedItem(SdkRepository.ATTR_TYPE);
                        if (typeNode == null || !SdkRepository.ATTR_TYPE.equals(typeNode.getNodeName()) || !SdkRepository.SHA1_TYPE.equals(typeNode.getNodeValue())) {
                            continue;
                        }
                        String sha1 = node == null ? null : node.getTextContent().trim();
                        if (sha1 == null || sha1.length() != SdkRepository.SHA1_CHECKSUM_LEN) {
                            continue;
                        }
                        Element ar = appendChild(newArchives, ns, prefix, SdkRepository.NODE_ARCHIVE, null);
                        ar.setAttributeNS(ns, SdkRepository.ATTR_OS, os.getXmlName());
                        ar.setAttributeNS(ns, SdkRepository.ATTR_ARCH, arch.getXmlName());
                        appendChild(ar, ns, prefix, SdkRepository.NODE_URL, url);
                        appendChild(ar, ns, prefix, SdkRepository.NODE_SIZE, Long.toString(size));
                        Element cs = appendChild(ar, ns, prefix, SdkRepository.NODE_CHECKSUM, sha1);
                        cs.setAttributeNS(ns, SdkRepository.ATTR_TYPE, SdkRepository.SHA1_TYPE);
                        numArchives++;
                    } catch (Exception ignore1) {
                    }
                }
                if (numArchives > 0) {
                    newRoot.appendChild(newTool);
                    numTool++;
                }
            } catch (Exception ignore2) {
            }
        }
        return numTool > 0 ? newDoc : null;
    }

    /**
     * Helper method used by {@link #findAlternateToolsXml(InputStream)} to find a given
     * element child in a root XML node.
     */
    private Node findChild(Node rootNode, Node after, String prefix, String nodeName) {
        nodeName = prefix + ":" + nodeName;
        Node child = after == null ? rootNode.getFirstChild() : after.getNextSibling();
        for (; child != null; child = child.getNextSibling()) {
            if (child.getNodeType() == Node.ELEMENT_NODE && nodeName.equals(child.getNodeName())) {
                return child;
            }
        }
        return null;
    }

    /**
     * Helper method used by {@link #findAlternateToolsXml(InputStream)} to create a new
     * XML element into a parent element.
     */
    private Element appendChild(Element rootNode, String namespaceUri, String prefix, String nodeName, String nodeValue) {
        Element node = rootNode.getOwnerDocument().createElementNS(namespaceUri, nodeName);
        node.setPrefix(prefix);
        if (nodeValue != null) {
            node.setTextContent(nodeValue);
        }
        rootNode.appendChild(node);
        return node;
    }

    /**
     * Helper method that returns a validator for our XSD, or null if the current Java
     * implementation can't process XSD schemas.
     *
     * @param version The version of the XML Schema.
     *        See {@link SdkRepository#getXsdStream(int)}
     */
    private Validator getValidator(int version) throws SAXException {
        InputStream xsdStream = SdkRepository.getXsdStream(version);
        SchemaFactory factory = SchemaFactory.newInstance(XMLConstants.W3C_XML_SCHEMA_NS_URI);
        if (factory == null) {
            return null;
        }
        Schema schema = factory.newSchema(new StreamSource(xsdStream));
        Validator validator = schema == null ? null : schema.newValidator();
        return validator;
    }

    /**
     * Parse all packages defined in the SDK Repository XML and creates
     * a new mPackages array with them.
     */
    protected boolean parsePackages(Document doc, String nsUri, ITaskMonitor monitor) {
        assert doc != null;
        Node root = getFirstChild(doc, nsUri, SdkRepository.NODE_SDK_REPOSITORY);
        if (root != null) {
            ArrayList<Package> packages = new ArrayList<Package>();
            HashMap<String, String> licenses = new HashMap<String, String>();
            for (Node child = root.getFirstChild(); child != null; child = child.getNextSibling()) {
                if (child.getNodeType() == Node.ELEMENT_NODE && nsUri.equals(child.getNamespaceURI()) && child.getLocalName().equals(SdkRepository.NODE_LICENSE)) {
                    Node id = child.getAttributes().getNamedItem(SdkRepository.ATTR_ID);
                    if (id != null) {
                        licenses.put(id.getNodeValue(), child.getTextContent());
                    }
                }
            }
            for (Node child = root.getFirstChild(); child != null; child = child.getNextSibling()) {
                if (child.getNodeType() == Node.ELEMENT_NODE && nsUri.equals(child.getNamespaceURI())) {
                    String name = child.getLocalName();
                    Package p = null;
                    try {
                        if (SdkRepository.NODE_ADD_ON.equals(name)) {
                            p = new AddonPackage(this, child, licenses);
                        } else if (SdkRepository.NODE_EXTRA.equals(name)) {
                            p = new ExtraPackage(this, child, licenses);
                        } else if (!mUserSource) {
                            if (SdkRepository.NODE_PLATFORM.equals(name)) {
                                p = new PlatformPackage(this, child, licenses);
                            } else if (SdkRepository.NODE_DOC.equals(name)) {
                                p = new DocPackage(this, child, licenses);
                            } else if (SdkRepository.NODE_TOOL.equals(name)) {
                                p = new ToolPackage(this, child, licenses);
                            } else if (SdkRepository.NODE_SAMPLE.equals(name)) {
                                p = new SamplePackage(this, child, licenses);
                            }
                        }
                        if (p != null) {
                            packages.add(p);
                            monitor.setDescription("Found %1$s", p.getShortDescription());
                        }
                    } catch (Exception e) {
                    }
                }
            }
            mPackages = packages.toArray(new Package[packages.size()]);
            Arrays.sort(mPackages, null);
            return true;
        }
        return false;
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

    /**
     * Takes an XML document as a string as parameter and returns a DOM for it.
     *
     * On error, returns null and prints a (hopefully) useful message on the monitor.
     */
    private Document getDocument(ByteArrayInputStream xml, ITaskMonitor monitor) {
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
}
