package com.hp.hpl.jena.reasoner.dig;

import java.io.*;
import java.net.*;
import java.util.*;
import javax.xml.parsers.*;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.apache.xml.serialize.*;
import org.w3c.dom.*;
import com.hp.hpl.jena.util.FileUtils;

/**
 * <p>
 * Encapsulates the connection to a DIG reasoner.
 * </p>
 *
 * @author Ian Dickinson, HP Labs (<a href="mailto:Ian.Dickinson@hp.com">email</a>)
 * @version Release @release@ ($Id: DIGConnection.java,v 1.14 2006/03/22 13:52:53 andy_seaborne Exp $)
 */
public class DIGConnection {

    /** Default URL for connecting to a local DIG reasoner on port 8081 */
    public static final String DEFAULT_REASONER_URL = "http://localhost:8081";

    /** Namespace for XSI */
    public static final String XSI = "http://www.w3.org/2001/XMLSchema-instance";

    private static Log log = LogFactory.getLog(DIGConnection.class);

    /** The URL to connect to, initialised to the default URL */
    protected String m_extReasonerURL = DEFAULT_REASONER_URL;

    /** URI of current KB */
    private String m_kbURI;

    /** The XML document builder we are using */
    protected DocumentBuilderFactory m_factory = DocumentBuilderFactory.newInstance();

    /** List of most recent warnings */
    private List m_warnings = new ArrayList();

    /** Flag to control whether we log incoming and outgoing messages */
    protected boolean m_logCommunications = true;

    /**
     * <p>Send a verb to the attached DIG reasoner and answer the result. The verb is encoded as an XML
     * document object.</p>
     * @param digVerb A DIG verb (information request, ask or tell) as an XML document
     * @return The resulting XML document formed from the response from the reasoner
     * @exception DigReasonerException for any errors in XML encoding or HTTP transmission
     */
    public Document sendDigVerb(Document digVerb, DIGProfile profile) {
        try {
            Element verb = digVerb.getDocumentElement();
            if (!verb.hasAttribute(DIGProfile.URI)) {
                verb.setAttribute(DIGProfile.URI, m_kbURI);
            }
            URL url = new URL(m_extReasonerURL);
            HttpURLConnection conn = (HttpURLConnection) url.openConnection();
            StringWriter out = new StringWriter();
            serialiseDocument(digVerb, out);
            conn.setDoOutput(true);
            conn.setRequestMethod("POST");
            conn.setRequestProperty("Content-Length", Integer.toString(out.getBuffer().length()));
            conn.setRequestProperty("Content-Type", profile.getContentType());
            logMessage(true, digVerb);
            conn.connect();
            PrintWriter pw = FileUtils.asPrintWriterUTF8(conn.getOutputStream());
            pw.print(out.getBuffer());
            pw.flush();
            pw.close();
            Document response = getDigResponse(conn);
            logMessage(false, response);
            errorCheck(response, profile);
            return response;
        } catch (IOException e) {
            throw new DIGWrappedException(e);
        }
    }

    /**
     * <p>Serialise the given document to the given output writer.</p>
     * @param doc An XML document to serialise
     * @param out A writer that will consume the seralised form of the document
     */
    public void serialiseDocument(Document doc, Writer out) {
        try {
            XMLSerializer serializer = new XMLSerializer(out, createXMLFormatter(doc));
            serializer.asDOMSerializer();
            serializer.serialize(doc);
        } catch (IOException e) {
            throw new DIGWrappedException(e);
        }
    }

    /**
     * <p>Bind a DIG KB to this adapter, by requesting a KB URI through the newKB
     * verb.  If there is already a binding, do nothing unless rebind is true.
     * @param rebind If true, any existing KB will be released before binding
     * to a new KB
     */
    public void bindKB(boolean rebind, DIGProfile profile) {
        if (rebind && m_kbURI != null) {
            Document release = createDigVerb(DIGProfile.RELEASEKB, profile);
            Document response = sendDigVerb(release, profile);
            errorCheck(response, profile);
            if (warningCheck(response)) {
                log.warn("DIG reasoner warning: " + getWarnings().next());
            }
            m_kbURI = null;
        }
        if (m_kbURI == null) {
            Document response = sendDigVerb(createDigVerb(DIGProfile.NEWKB, profile), profile);
            errorCheck(response, profile);
            Element kb = (Element) response.getDocumentElement().getElementsByTagName(DIGProfile.KB).item(0);
            if (kb == null) {
                throw new DIGReasonerException("Could not locate DIG KB identifier in return value from newKB");
            } else {
                m_kbURI = kb.getAttribute(DIGProfile.URI);
            }
        }
    }

    /**
     * <p>Check the response from the DIG server to see if there is an error code,
     * and raise an excption if so.</p>
     * @param response The response from the DIG server
     */
    public void errorCheck(Document response, DIGProfile profile) {
        Element root = response.getDocumentElement();
        NodeList errs = root.getElementsByTagName(DIGProfile.ERROR);
        if (errs != null && errs.getLength() > 0) {
            Element error = (Element) errs.item(0);
            String errCode = error.getAttribute(DIGProfile.CODE);
            int code = (errCode == null || errCode.length() == 0) ? 0 : Integer.parseInt(errCode);
            String msgAttr = error.getAttribute(DIGProfile.MESSAGE);
            NodeList messages = error.getChildNodes();
            String message = (messages.getLength() > 0) ? ((Text) messages.item(0)).getNodeValue().trim() : "(no message)";
            if (message.equals(profile.getInconsistentKBMessage())) {
                throw new DIGInconsistentKBException(message, msgAttr, code);
            } else {
                throw new DIGErrorResponseException(message, msgAttr, code);
            }
        }
    }

    /**
     * <p>Append any warning messages from this response to the list of recent warnings,
     * which is first cleared.</p>
     * @param response The response from the DIG server
     * @return True if any warnings were detected.
     */
    public boolean warningCheck(Document response) {
        Element root = response.getDocumentElement();
        NodeList ok = root.getElementsByTagName(DIGProfile.OK);
        m_warnings.clear();
        if (ok != null && ok.getLength() > 0) {
            Element e = (Element) ok.item(0);
            NodeList warnings = e.getElementsByTagName(DIGProfile.WARNING);
            if (warnings != null && warnings.getLength() > 0) {
                for (int i = 0; i < warnings.getLength(); i++) {
                    Element warn = (Element) warnings.item(i);
                    m_warnings.add(warn.getAttribute(DIGProfile.MESSAGE));
                }
                return true;
            }
        }
        return false;
    }

    /**
     * <p>Answer an iterator over the warnings received since the last tell operation</p>
     * @return An iterator over warnings
     */
    public Iterator getWarnings() {
        return m_warnings.iterator();
    }

    /**
     * <p>Release this connection back to the connection pool.</p>
     */
    public void release() {
        DIGConnectionPool.getInstance().release(this);
    }

    /**
     * <p>Answer the URL of the external reasoner this connection is bound to.</p>
     * @return The current external reasoner URL
     */
    public String getReasonerURL() {
        return m_extReasonerURL;
    }

    /**
     * <p>Set the URL of the external reasoner with which this connection communicates.</p>
     * @param url The URL of the new external reasoner connection point
     */
    public void setReasonerURL(String url) {
        m_extReasonerURL = url;
        m_kbURI = null;
    }

    /**
     * <p>Answer the XML document object that resulted from the most recent request.</p>
     * @param conn The current HTTP connection
     * @return The response from the DIG reasoner, as an XML object
     * @exception DigReasonerException if the underling connection or XML parser raises
     * an error.
     */
    protected Document getDigResponse(HttpURLConnection conn) {
        try {
            if (conn.getResponseCode() != HttpURLConnection.HTTP_OK) {
                throw new DIGReasonerException("DIG reasoner returned failure code " + conn.getResponseCode() + ": " + conn.getResponseMessage());
            }
            BufferedInputStream in = new BufferedInputStream(conn.getInputStream());
            StringBuffer buf = new StringBuffer();
            int ch = in.read();
            while (ch > 0) {
                buf.append((char) ch);
                ch = in.read();
            }
            DocumentBuilder builder = m_factory.newDocumentBuilder();
            return builder.parse(new ByteArrayInputStream(buf.toString().getBytes()));
        } catch (Exception e) {
            e.printStackTrace(System.err);
            throw new DIGWrappedException(e);
        }
    }

    /**
     * <p>Answer an XML formatter object for the given document
     * @param doc The XML document to be serialised
     * @return An XML formatter object for the document
     */
    protected OutputFormat createXMLFormatter(Document doc) {
        OutputFormat format = new OutputFormat(doc);
        format.setIndenting(true);
        format.setLineWidth(0);
        format.setPreserveSpace(false);
        return format;
    }

    /**
     * <p>Create a DIG verb as an xml element in a new document object.</p>
     * @param verbName The name of the DIG verb, as a string
     * @return An XML DOM element representing the DIG verb
     */
    protected Document createDigVerb(String verbName, DIGProfile profile) {
        try {
            DocumentBuilder builder = m_factory.newDocumentBuilder();
            Document doc = builder.newDocument();
            Element root = doc.createElementNS(profile.getDIGNamespace(), verbName);
            doc.appendChild(root);
            root.setAttribute("xmlns", profile.getDIGNamespace());
            root.setAttribute("xmlns:xsi", XSI);
            root.setAttributeNS(XSI, "xsi:schemaLocation", profile.getDIGNamespace() + " " + profile.getSchemaLocation());
            if (m_kbURI != null) {
                root.setAttribute(DIGProfile.URI, m_kbURI);
            }
            return doc;
        } catch (FactoryConfigurationError e) {
            throw new DIGWrappedException(e);
        } catch (ParserConfigurationException e) {
            throw new DIGWrappedException(e);
        }
    }

    /**
     * <p>Log the messages going to and from DIG</p>
     * @param outgoing True for send, false for receive
     * @param msg The document sent or received.
     */
    protected void logMessage(boolean outgoing, Document msg) {
        if (m_logCommunications) {
            StringWriter out = new StringWriter();
            serialiseDocument(msg, out);
            if (log.isDebugEnabled()) {
                log.debug(outgoing ? "Sending to DIG reasoner ..." : "Received from DIG reasoner ...");
                log.debug(out);
            }
        }
    }
}
