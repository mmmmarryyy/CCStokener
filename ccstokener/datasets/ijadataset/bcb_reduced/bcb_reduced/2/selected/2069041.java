package com.android.sdklib.internal.repository;

import com.android.sdklib.annotations.Nullable;
import com.android.sdklib.annotations.VisibleForTesting;
import com.android.sdklib.annotations.VisibleForTesting.Visibility;
import com.android.sdklib.repository.RepoConstants;
import com.android.sdklib.repository.SdkAddonConstants;
import com.android.sdklib.repository.SdkRepoConstants;
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
import java.net.MalformedURLException;
import java.net.URL;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
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
 * An sdk-addon or sdk-repository source, i.e. a download site.
 * It may be a full repository or an add-on only repository.
 * A repository describes one or {@link Package}s available for download.
 */
public abstract class SdkSource implements IDescription {

    private String mUrl;

    private Package[] mPackages;

    private String mDescription;

    private String mFetchError;

    private final String mUiName;

    /**
     * Constructs a new source for the given repository URL.
     * @param url The source URL. Cannot be null. If the URL ends with a /, the default
     *            repository.xml filename will be appended automatically.
     * @param uiName The UI-visible name of the source. Can be null.
     */
    public SdkSource(String url, String uiName) {
        if (url == null) {
            url = "";
        }
        url = url.trim();
        if (url.endsWith("/")) {
            url += getUrlDefaultXmlFile();
        }
        mUrl = url;
        mUiName = uiName;
        setDefaultDescription();
    }

    /**
     * Returns true if this is an addon source.
     * We only load addons and extras from these sources.
     */
    public abstract boolean isAddonSource();

    /** Returns SdkRepoConstants.URL_DEFAULT_XML_FILE or SdkAddonConstants.URL_DEFAULT_XML_FILE */
    protected abstract String getUrlDefaultXmlFile();

    /** Returns SdkRepoConstants.NS_LATEST_VERSION or SdkAddonConstants.NS_LATEST_VERSION. */
    protected abstract int getNsLatestVersion();

    /** Returns SdkRepoConstants.NS_URI or SdkAddonConstants.NS_URI. */
    protected abstract String getNsUri();

    /** Returns SdkRepoConstants.NS_PATTERN or SdkAddonConstants.NS_PATTERN. */
    protected abstract String getNsPattern();

    /** Returns SdkRepoConstants.getSchemaUri() or SdkAddonConstants.getSchemaUri(). */
    protected abstract String getSchemaUri(int version);

    protected abstract String getRootElementName();

    /** Returns SdkRepoConstants.getXsdStream() or SdkAddonConstants.getXsdStream(). */
    protected abstract InputStream getXsdStream(int version);

    /**
     * In case we fail to load an XML, examine the XML to see if it matches a <b>future</b>
     * schema that as at least a <code>tools</code> node that we could load to update the
     * SDK Manager.
     *
     * @param xml The input XML stream. Can be null.
     * @return Null on failure, otherwise returns an XML DOM with just the tools we
     *   need to update this SDK Manager.
     * @null Can return null on failure.
     */
    protected abstract Document findAlternateToolsXml(@Nullable InputStream xml) throws IOException;

    /**
     * Two repo source are equal if they have the same URL.
     */
    @Override
    public boolean equals(Object obj) {
        if (obj instanceof SdkSource) {
            SdkSource rs = (SdkSource) obj;
            return rs.getUrl().equals(this.getUrl());
        }
        return false;
    }

    @Override
    public int hashCode() {
        return mUrl.hashCode();
    }

    /**
     * Returns the UI-visible name of the source. Can be null.
     */
    public String getUiName() {
        return mUiName;
    }

    /** Returns the URL of the XML file for this source. */
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

    /**
     * Returns the short description of the source, if not null.
     * Otherwise returns the default Object toString result.
     * <p/>
     * This is mostly helpful for debugging.
     * For UI display, use the {@link IDescription} interface.
     */
    @Override
    public String toString() {
        String s = getShortDescription();
        if (s != null) {
            return s;
        }
        return super.toString();
    }

    public String getShortDescription() {
        if (mUiName != null && mUiName.length() > 0) {
            String host = "malformed URL";
            try {
                URL u = new URL(mUrl);
                host = u.getHost();
            } catch (MalformedURLException e) {
            }
            return String.format("%1$s (%2$s)", mUiName, host);
        }
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
        Boolean[] validatorFound = new Boolean[] { Boolean.FALSE };
        String[] validationError = new String[] { null };
        Exception[] exception = new Exception[] { null };
        InputStream xml = fetchUrl(url, exception);
        Document validatedDoc = null;
        boolean usingAlternateXml = false;
        boolean usingAlternateUrl = false;
        String validatedUri = null;
        if (xml == null && !url.endsWith(getUrlDefaultXmlFile())) {
            if (!url.endsWith("/")) {
                url += "/";
            }
            url += getUrlDefaultXmlFile();
            xml = fetchUrl(url, exception);
            usingAlternateUrl = true;
        }
        if (xml != null) {
            monitor.setDescription("Validate XML");
            for (int tryOtherUrl = 0; tryOtherUrl < 2; tryOtherUrl++) {
                int version = getXmlSchemaVersion(xml);
                if (version >= 1 && version <= getNsLatestVersion()) {
                    String uri = validateXml(xml, url, version, validationError, validatorFound);
                    if (uri != null) {
                        validatedDoc = getDocument(xml, monitor);
                        validatedUri = uri;
                        if (usingAlternateUrl && validatedDoc != null) {
                            monitor.setResult("Repository found at %1$s", url);
                            mUrl = url;
                        }
                    } else if (validatorFound[0].equals(Boolean.FALSE)) {
                        mFetchError = validationError[0];
                    } else {
                    }
                } else if (version > getNsLatestVersion()) {
                    try {
                        validatedDoc = findAlternateToolsXml(xml);
                    } catch (IOException e) {
                    }
                    if (validatedDoc != null) {
                        validationError[0] = null;
                        validatedUri = getNsUri();
                        usingAlternateXml = true;
                    }
                } else if (version < 1 && tryOtherUrl == 0 && !usingAlternateUrl) {
                    mFetchError = String.format("Failed to validate the XML for the repository at URL '%1$s'", url);
                    if (!url.endsWith(getUrlDefaultXmlFile())) {
                        if (!url.endsWith("/")) {
                            url += "/";
                        }
                        url += getUrlDefaultXmlFile();
                        xml = fetchUrl(url, null);
                        if (xml != null) {
                            usingAlternateUrl = true;
                            continue;
                        }
                    }
                } else if (version < 1 && usingAlternateUrl && mFetchError == null) {
                    mFetchError = String.format("Failed to validate the XML for the repository at URL '%1$s'", url);
                }
                break;
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
            boolean isADT = false;
            try {
                Class<?> adt = Class.forName("com.android.ide.eclipse.adt.AdtPlugin");
                isADT = (adt != null);
            } catch (ClassNotFoundException e) {
            }
            String info;
            if (isADT) {
                info = "This repository requires a more recent version of ADT. Please update the Eclipse Android plugin.";
                mDescription = "This repository requires a more recent version of ADT, the Eclipse Android plugin.\nYou must update it before you can see other new packages.";
            } else {
                info = "This repository requires a more recent version of the Tools. Please update.";
                mDescription = "This repository requires a more recent version of the Tools.\nYou must update it before you can see other new packages.";
            }
            mFetchError = mFetchError == null ? info : mFetchError + ". " + info;
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
        if (isAddonSource()) {
            String desc = "";
            if (mUiName != null) {
                desc += "Add-on Provider: " + mUiName;
                desc += "\n";
            }
            desc += "Add-on URL: " + mUrl;
            mDescription = desc;
        } else {
            mDescription = String.format("SDK Source: %1$s", mUrl);
        }
    }

    /**
     * Fetches the document at the given URL and returns it as a string.
     * Returns null if anything wrong happens and write errors to the monitor.
     *
     * References: <br/>
     * Java URL Connection: http://java.sun.com/docs/books/tutorial/networking/urls/readingWriting.html <br/>
     * Java URL Reader: http://java.sun.com/docs/books/tutorial/networking/urls/readingURL.html <br/>
     * Java set Proxy: http://java.sun.com/docs/books/tutorial/networking/urls/_setProxy.html <br/>
     *
     * @param urlString The URL to load, as a string.
     * @param outException If non null, where to store any exception that happens during the fetch.
     */
    private InputStream fetchUrl(String urlString, Exception[] outException) {
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
            return getSchemaUri(version);
        } catch (SAXParseException e) {
            outError[0] = String.format("XML verification failed for %1$s.\nLine %2$d:%3$d, Error: %4$s", url, e.getLineNumber(), e.getColumnNumber(), e.toString());
        } catch (Exception e) {
            outError[0] = String.format("XML verification failed for %1$s.\nError: %2$s", url, e.toString());
        }
        return null;
    }

    /**
     * Manually parses the root element of the XML to extract the schema version
     * at the end of the xmlns:sdk="http://schemas.android.com/sdk/android/repository/$N"
     * declaration.
     *
     * @return 1..{@link SdkRepoConstants#NS_LATEST_VERSION} for a valid schema version
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
        Pattern nsPattern = Pattern.compile(getNsPattern());
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
                if (getRootElementName().equals(name)) {
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
     * Helper method that returns a validator for our XSD, or null if the current Java
     * implementation can't process XSD schemas.
     *
     * @param version The version of the XML Schema.
     *        See {@link SdkRepoConstants#getXsdStream(int)}
     */
    private Validator getValidator(int version) throws SAXException {
        InputStream xsdStream = getXsdStream(version);
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
    @VisibleForTesting(visibility = Visibility.PRIVATE)
    protected boolean parsePackages(Document doc, String nsUri, ITaskMonitor monitor) {
        Node root = getFirstChild(doc, nsUri, getRootElementName());
        if (root != null) {
            ArrayList<Package> packages = new ArrayList<Package>();
            HashMap<String, String> licenses = new HashMap<String, String>();
            for (Node child = root.getFirstChild(); child != null; child = child.getNextSibling()) {
                if (child.getNodeType() == Node.ELEMENT_NODE && nsUri.equals(child.getNamespaceURI()) && child.getLocalName().equals(RepoConstants.NODE_LICENSE)) {
                    Node id = child.getAttributes().getNamedItem(RepoConstants.ATTR_ID);
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
                        if (SdkAddonConstants.NODE_ADD_ON.equals(name)) {
                            p = new AddonPackage(this, child, nsUri, licenses);
                        } else if (RepoConstants.NODE_EXTRA.equals(name)) {
                            p = new ExtraPackage(this, child, nsUri, licenses);
                        } else if (!isAddonSource()) {
                            if (SdkRepoConstants.NODE_PLATFORM.equals(name)) {
                                p = new PlatformPackage(this, child, nsUri, licenses);
                            } else if (SdkRepoConstants.NODE_DOC.equals(name)) {
                                p = new DocPackage(this, child, nsUri, licenses);
                            } else if (SdkRepoConstants.NODE_TOOL.equals(name)) {
                                p = new ToolPackage(this, child, nsUri, licenses);
                            } else if (SdkRepoConstants.NODE_PLATFORM_TOOL.equals(name)) {
                                p = new PlatformToolPackage(this, child, nsUri, licenses);
                            } else if (SdkRepoConstants.NODE_SAMPLE.equals(name)) {
                                p = new SamplePackage(this, child, nsUri, licenses);
                            }
                        }
                        if (p != null) {
                            packages.add(p);
                            monitor.setDescription("Found %1$s", p.getShortDescription());
                        }
                    } catch (Exception e) {
                        monitor.setResult("Ignoring invalid %1$s element: %2$s", name, e.toString());
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
}
