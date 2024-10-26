package net.sourceforge.gedapi.servlet;

import genj.io.SniffedInputStream;
import genj.util.EnvironmentChecker;
import genj.util.Origin;
import gnu.java.net.protocol.file.Handler;
import java.io.BufferedOutputStream;
import java.io.BufferedWriter;
import java.io.ByteArrayInputStream;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStreamWriter;
import java.io.PrintWriter;
import java.io.StringWriter;
import java.net.URL;
import java.net.URLConnection;
import java.nio.channels.FileLock;
import java.nio.channels.OverlappingFileLockException;
import java.nio.charset.Charset;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.logging.Level;
import java.util.logging.Logger;
import javax.servlet.ServletConfig;
import javax.servlet.ServletException;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.ParserConfigurationException;
import javax.xml.transform.OutputKeys;
import javax.xml.transform.Transformer;
import javax.xml.transform.TransformerConfigurationException;
import javax.xml.transform.TransformerException;
import javax.xml.transform.TransformerFactory;
import javax.xml.transform.dom.DOMSource;
import javax.xml.transform.stream.StreamResult;
import net.sf.json.JSONObject;
import net.sf.json.JSONSerializer;
import net.sourceforge.gedapi.io.GLinkPullParser;
import net.sourceforge.gedapi.io.GegPullHandler;
import net.sourceforge.gedapi.io.GigPullHandler;
import net.sourceforge.gedapi.util.Environment;
import net.sourceforge.gedapi.util.GLinkPattern;
import net.sourceforge.gedapi.util.GLinkURL;
import net.sourceforge.gedapi.view.GLinkView;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;
import org.xml.sax.InputSource;
import org.xml.sax.SAXException;
import org.xml.sax.ext.DefaultHandler2;
import com.liferay.portal.servlet.filters.context.sso.SSOSubject;

public class GLinkServlet extends HttpServlet {

    private static final Logger LOG = Logger.getLogger(GLinkServlet.class.getName());

    private static final long serialVersionUID = 124124129123213L;

    private static enum GLinkAction {

        CREATE_GLINK, DELETE_GLINK, DELETE_ALL_GLINKS, GENERATE_GIG, GENERATE_GEG
    }

    ;

    private static final SimpleDateFormat isoFormat = new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss.SSSZ");

    private static final String XSD_LOCATION;

    static {
        String xsdLocation = Environment.getProperty("GLinksXSD");
        XSD_LOCATION = (xsdLocation.indexOf("://") == -1 ? "file://" + (xsdLocation.startsWith("/") ? xsdLocation : "/" + xsdLocation) : xsdLocation);
        LOG.finest("The XSD_LOCATION is: " + XSD_LOCATION);
    }

    public void init(ServletConfig config) throws ServletException {
        super.init(config);
    }

    public void doPost(HttpServletRequest request, HttpServletResponse response) throws IOException, ServletException {
        throw new ServletException("SEND MSG TO BROWSER: This servlet does not support the POST action.");
    }

    public void doGet(HttpServletRequest request, HttpServletResponse response) throws IOException, ServletException {
        LOG.finest("Invoking the doGet method of the GLinkServlet.");
        GLinkURL fromURL = new GLinkURL(request.getParameter("fromURL"));
        String fromName = request.getParameter("fromName");
        String fromRelValue = request.getParameter("fromRelValue");
        if (fromRelValue.endsWith(".")) {
            fromRelValue = fromRelValue.substring(0, fromRelValue.length() - 1);
        }
        GLinkURL toURL = new GLinkURL(request.getParameter("toURL"));
        String toName = request.getParameter("toName");
        String toRelValue = request.getParameter("toRelValue");
        if (toRelValue.endsWith(".")) {
            toRelValue = toRelValue.substring(0, toRelValue.length() - 1);
        }
        String action = request.getParameter("action");
        SSOSubject authenticated = (SSOSubject) request.getAttribute(SSOSubject.SSOSUBJECT_KEY);
        if (authenticated == null && !action.equals("generateGig") && !action.equals("generateGeg")) {
            throw new ServletException("The request does not have an " + SSOSubject.SSOSUBJECT_KEY + " attribute, this is not allowed!!!");
        }
        boolean applyToBoth = Boolean.valueOf(request.getParameter("applyToBoth")).booleanValue();
        LOG.finest("action: " + action + " fromName: " + fromName + " fromURL: " + fromURL + " fromRelValue: " + fromRelValue + " toName: " + toName + " toURL: " + toURL + " toRelValue: " + toRelValue);
        if (!(empty(fromName) || empty(fromURL.toString()) || empty(fromRelValue) || empty(toName) || empty(toURL.toString()) || empty(toRelValue)) && !action.equals("generateGig") && !action.equals("generateGeg")) {
            if (fromURL.equals(toURL)) {
                throw new ServletException("The URLs for the two individual's you are attempting to GLink are exactly the same. PLEASE choose a different individual to GLink: " + fromURL);
            }
            if (action.equals("createGLink")) {
                processGLink(authenticated, fromName, fromURL, fromRelValue, toName, toURL, toRelValue, GLinkAction.CREATE_GLINK);
                if (applyToBoth) {
                    processGLink(authenticated, toName, toURL, toRelValue, fromName, fromURL, fromRelValue, GLinkAction.CREATE_GLINK);
                }
            } else if (action.equals("deleteGLink")) {
                processGLink(authenticated, fromName, fromURL, fromRelValue, toName, toURL, toRelValue, GLinkAction.DELETE_GLINK);
                if (applyToBoth) {
                    processGLink(authenticated, toName, toURL, toRelValue, fromName, fromURL, fromRelValue, GLinkAction.DELETE_GLINK);
                }
            } else if (action.equals("deleteAllGLinks")) {
                processGLink(authenticated, fromName, fromURL, fromRelValue, toName, toURL, toRelValue, GLinkAction.DELETE_ALL_GLINKS);
                if (applyToBoth) {
                    processGLink(authenticated, toName, toURL, toRelValue, fromName, fromURL, fromRelValue, GLinkAction.DELETE_ALL_GLINKS);
                }
            } else {
                throw new ServletException("The action '" + action + "' is not supported!!");
            }
        } else if (action.equals("generateGig")) {
            generateGLink(authenticated, fromName, fromURL, fromRelValue, toName, toURL, toRelValue, applyToBoth, "generateGig.jsp", GLinkAction.GENERATE_GIG, request, response);
            return;
        } else if (action.equals("generateGeg")) {
            generateGLink(authenticated, fromName, fromURL, fromRelValue, toName, toURL, toRelValue, applyToBoth, "generateGeg.jsp", GLinkAction.GENERATE_GEG, request, response);
            return;
        }
        GLinkView glinkView = new GLinkView();
        glinkView.fromName = fromName;
        glinkView.toName = toName;
        glinkView.action = action;
        glinkView.applyToBoth = applyToBoth;
        JSONObject jsonObject = (JSONObject) JSONSerializer.toJSON(glinkView);
        response.setHeader("Cache-Control", "max-age=0");
        response.setContentType("application/json");
        PrintWriter printWriter = response.getWriter();
        printWriter.print(jsonObject.toString());
        printWriter.flush();
        printWriter.close();
    }

    public boolean empty(String val) {
        return (val == null || val.trim().length() == 0);
    }

    public void generateGLink(SSOSubject authenticated, String fromName, GLinkURL fromURL, String fromRelValue, String toName, GLinkURL toURL, String toRelValue, boolean applyToBoth, String forwardTo, GLinkAction action, HttpServletRequest request, HttpServletResponse response) throws IOException, ServletException {
        try {
            if (!(empty(fromName) || empty(fromURL.toString()) || empty(fromRelValue) || empty(toName) || empty(toURL.toString()) || empty(toRelValue))) {
                if (fromURL.equals(toURL)) {
                    throw new ServletException("The URLs for the two individual's you are attempting to GLink are exactly the same. PLEASE choose a different individual to GLink: " + fromURL);
                }
                if (action.equals(GLinkAction.GENERATE_GIG)) {
                    generateGigFile(authenticated, fromName, fromURL, fromRelValue, toName, toURL, toRelValue, action, request, false);
                    if (applyToBoth) {
                        generateGigFile(authenticated, toName, toURL, toRelValue, fromName, fromURL, fromRelValue, action, request, true);
                    }
                } else if (action.equals(GLinkAction.GENERATE_GEG)) {
                    generateGegFile(authenticated, fromName, fromURL, fromRelValue, toName, toURL, toRelValue, action, request, false);
                    if (applyToBoth) {
                        generateGegFile(authenticated, toName, toURL, toRelValue, fromName, fromURL, fromRelValue, action, request, true);
                    }
                }
                request.getRequestDispatcher(forwardTo).forward(request, response);
            }
        } catch (InterruptedException ex) {
            throw new ServletException(ex);
        }
    }

    public void generateGigFile(SSOSubject authenticated, String fromName, GLinkURL fromURL, String fromRelValue, String toName, GLinkURL toURL, String toRelValue, GLinkAction action, HttpServletRequest request, boolean doingToURL) throws IOException, InterruptedException {
        LOG.finest("The individualId is: " + fromURL.getIndividualId());
        Object downloadRunner = "";
        String filename = null;
        File downloadedFile = null;
        while (downloadRunner != null) {
            downloadedFile = fromURL.downloadGedcomURL(true, request);
            downloadRunner = request.getAttribute("X-Downloading-GEDCOM");
            String localGedcomPath = downloadedFile.getAbsolutePath();
            filename = "file://" + (localGedcomPath.startsWith("/") ? localGedcomPath : "/" + localGedcomPath);
            if (downloadRunner != null) {
                try {
                    request.removeAttribute("X-Downloading-GEDCOM");
                    Thread.sleep(1000);
                } catch (InterruptedException ex) {
                    ex.printStackTrace();
                }
            }
        }
        LOG.finest("The gedcomURL is: " + fromURL.getGedcomURL());
        if (doingToURL) {
            request.setAttribute("toGedcomURL", fromURL.getGedcomURL());
        } else {
            request.setAttribute("fromGedcomURL", fromURL.getGedcomURL());
        }
        File generatedGigFile = fromURL.getGeneratedGigFile(downloadedFile, request.getSession(true).getId());
        if (generatedGigFile.exists() && doingToURL) {
            String localGedcomPath = generatedGigFile.getAbsolutePath();
            filename = "file://" + (localGedcomPath.startsWith("/") ? localGedcomPath : "/" + localGedcomPath);
            generatedGigFile = fromURL.getGeneratedGigFileToURL(generatedGigFile);
        }
        Origin origin = Origin.create(filename, new Handler());
        LOG.finest("Generating GIG into file: " + generatedGigFile + " while reading from file: " + origin);
        generatedGigFile.getParentFile().mkdirs();
        FileOutputStream fos = new FileOutputStream(generatedGigFile);
        BufferedOutputStream bos = new BufferedOutputStream(fos);
        SniffedInputStream inStream = new SniffedInputStream(origin.open());
        Charset charset = inStream.getCharset();
        if (inStream.getWarning() != null) {
            LOG.warning(inStream.getWarning());
        }
        String charsetName = EnvironmentChecker.getProperty(this, "genj.gedcom.charset", null, "checking for forced charset for read of inputStream");
        if (charsetName != null) {
            try {
                charset = Charset.forName(charsetName);
            } catch (Throwable t) {
                LOG.log(Level.WARNING, "Can't force charset " + charset, t);
            }
        }
        OutputStreamWriter out = new OutputStreamWriter(bos, charset);
        InputStreamReader in = new InputStreamReader(inStream, charset);
        GigPullHandler handler = new GigPullHandler();
        handler.setInsertDelta(1);
        handler.setToName(toName);
        handler.setToURL(toURL);
        handler.setToRelation(toRelValue);
        handler.setFromRelation(fromRelValue);
        handler.setOutWriter(out);
        GLinkPattern pattern = new GLinkPattern("0 @" + fromURL.getIndividualId() + "@ INDI");
        pattern.setCompareSize(pattern.getToMatch().length() + 10);
        pattern.setNumberOfMatches(1);
        GLinkPullParser pullParser = new GLinkPullParser();
        pullParser.setReader(in);
        pullParser.addPullHander(pattern, handler);
        pullParser.parse();
        out.flush();
        out.close();
        bos.flush();
        bos.close();
        fos.flush();
        fos.close();
        in.close();
        inStream.close();
        LOG.finest("The generatedGigFile is: " + generatedGigFile);
        if (doingToURL) {
            request.setAttribute("toGeneratedGigFile", generatedGigFile);
        } else {
            request.setAttribute("fromGeneratedGigFile", generatedGigFile);
        }
    }

    public void generateGegFile(SSOSubject authenticated, String fromName, GLinkURL fromURL, String fromRelValue, String toName, GLinkURL toURL, String toRelValue, GLinkAction action, HttpServletRequest request, boolean doingToURL) throws IOException, InterruptedException {
        LOG.finest("Generating a GEG!");
        LOG.finest("The individualId is: " + fromURL.getIndividualId());
        String glinkXMLURL = fromURL.getGLinkXMLURL();
        LOG.finer("Generating a GEG for: " + glinkXMLURL);
        if (doingToURL) {
            request.setAttribute("toGLinkURL", glinkXMLURL);
        } else {
            request.setAttribute("fromGLinkURL", glinkXMLURL);
        }
        URL url = new URL(glinkXMLURL);
        URLConnection uc = url.openConnection();
        InputStream in = null;
        try {
            in = uc.getInputStream();
        } catch (Exception ex) {
            if (LOG.isLoggable(Level.FINEST)) {
                StringWriter exWriter = new StringWriter();
                PrintWriter writer = new PrintWriter(exWriter);
                ex.printStackTrace(writer);
                exWriter.close();
                writer.close();
                LOG.finest("generateGegFile Connect exception: " + exWriter.toString());
            }
            String newGLinkXML = "<?xml version=\"1.0\" encoding=\"UTF-8\"?>" + "<g:glinks xmlns:g=\"http://www.gedcombrowser.org/gedapi/glinks-schema\"" + " xmlns:xsi=\"http://www.w3.org/2001/XMLSchema-instance\" version=\"1.0\"" + " xsi:schemaLocation=\"http://www.gedcombrowser.org/gedapi/glinks-schema" + " http://www.gedcombrowser.org/gedapi/glinks-schema/glinks_1_0.xsd\">" + "<g:individual>" + fromURL + "</g:individual>" + "</g:glinks>";
            in = new ByteArrayInputStream(newGLinkXML.getBytes());
        }
        File generatedGegFile = fromURL.getGeneratedGegFile(request.getSession(true).getId());
        LOG.finest("Generating GEG into file: " + generatedGegFile + " while reading from file: " + glinkXMLURL);
        SniffedInputStream inStream = new SniffedInputStream(in);
        Charset charset = inStream.getCharset();
        if (inStream.getWarning() != null) {
            LOG.warning(inStream.getWarning());
        }
        String charsetName = EnvironmentChecker.getProperty(this, "genj.gedcom.charset", null, "checking for forced charset for read of inputStream");
        if (charsetName != null) {
            try {
                charset = Charset.forName(charsetName);
            } catch (Throwable t) {
                LOG.log(Level.WARNING, "Can't force charset " + charset, t);
            }
        }
        InputStreamReader inReader = new InputStreamReader(inStream, charset);
        generatedGegFile.getParentFile().mkdirs();
        FileOutputStream fos = new FileOutputStream(generatedGegFile);
        BufferedOutputStream bos = new BufferedOutputStream(fos);
        OutputStreamWriter out = new OutputStreamWriter(bos, charset);
        GLinkPattern pattern = new GLinkPattern("</g:glinks>");
        pattern.setCompareSize(pattern.getToMatch().length());
        pattern.setNumberOfMatches(1);
        GegPullHandler handler = new GegPullHandler();
        handler.setInsertDelta(-(pattern.getToMatch().length()) - 5);
        handler.setToName(toName);
        handler.setToURL(toURL);
        handler.setToRelation(toRelValue);
        handler.setFromRelation(fromRelValue);
        handler.setOutWriter(out);
        handler.setAuthenticated(authenticated);
        GLinkPullParser pullParser = new GLinkPullParser();
        pullParser.setReader(inReader);
        pullParser.addPullHander(pattern, handler);
        pullParser.parse();
        inStream.close();
        inReader.close();
        in.close();
        out.flush();
        out.close();
        bos.flush();
        bos.close();
        fos.flush();
        fos.close();
        LOG.finest("The generated GLink xml file is: " + generatedGegFile);
        if (doingToURL) {
            request.setAttribute("toGeneratedGegFile", generatedGegFile);
        } else {
            request.setAttribute("fromGeneratedGegFile", generatedGegFile);
        }
    }

    public synchronized void processGLink(SSOSubject authenticated, String fromName, GLinkURL fromURL, String fromRelValue, String toName, GLinkURL toURL, String toRelValue, GLinkAction action) {
        FileOutputStream fileLockStream = null;
        FileLock fileLock = null;
        try {
            File glinkXMLFile = fromURL.getGLinkXMLFile();
            File glinksFileLock = fromURL.getGLinkXMLLockFile();
            boolean causedException = false;
            do {
                if (causedException) {
                    try {
                        Thread.sleep(20);
                    } catch (InterruptedException ex) {
                        if (LOG.isLoggable(Level.FINEST)) ex.printStackTrace();
                    }
                }
                try {
                    glinksFileLock.getParentFile().mkdirs();
                    fileLockStream = new FileOutputStream(glinksFileLock);
                    fileLock = fileLockStream.getChannel().lock();
                } catch (FileNotFoundException ex) {
                    causedException = true;
                    if (LOG.isLoggable(Level.FINEST)) {
                        ex.printStackTrace();
                    }
                } catch (IOException ex) {
                    causedException = true;
                    if (LOG.isLoggable(Level.FINEST)) {
                        ex.printStackTrace();
                    }
                } catch (OverlappingFileLockException ex) {
                    causedException = true;
                    if (LOG.isLoggable(Level.FINEST)) {
                        ex.printStackTrace();
                    }
                }
            } while (causedException);
            DocumentBuilderFactory dbf = DocumentBuilderFactory.newInstance();
            dbf.setValidating(true);
            dbf.setNamespaceAware(true);
            dbf.setIgnoringElementContentWhitespace(true);
            Document doc = null;
            DocumentBuilder builder = dbf.newDocumentBuilder();
            builder.setErrorHandler(new DefaultHandler2());
            boolean foundToURL = false;
            Element docRoot = null;
            try {
                LOG.finest("Parsing .glink.xml file: " + glinkXMLFile);
                doc = builder.parse(new InputSource(new FileReader(glinkXMLFile)));
                NodeList nodes = doc.getElementsByTagName("g:individual");
                LOG.finest("nodes.getLength(): " + nodes.getLength());
                if (nodes.getLength() > 1) {
                    throw new RuntimeException("SEND MSG TO BROWSER: There are too many <individual/> tags in the glinks xml file located at '" + glinkXMLFile + "'");
                }
                LOG.finest("nodes.item(0): " + nodes.item(0));
                LOG.finest("nodes.item(0).getTextContent(): " + nodes.item(0).getTextContent());
                LOG.finest("nodes.item(0).getTextContent().equals(fromURL.toString()): " + nodes.item(0).getTextContent().equals(fromURL.toString()));
                if (!nodes.item(0).getTextContent().equals(fromURL.toString())) {
                    throw new RuntimeException("SEND MSG TO BROWSER: The individual, '" + nodes.item(0).getTextContent() + "', declared in the glinks xml file, '" + glinkXMLFile + "', does not match the from individual '" + fromURL + "'!!");
                }
                nodes = doc.getElementsByTagName("g:link");
                List<Node> removeNodes = new ArrayList<Node>();
                for (int i = 0; (!foundToURL && i < nodes.getLength()); i++) {
                    LOG.finest("The parent node is: " + nodes.item(i).getNodeName());
                    Node anchor = nodes.item(i).getFirstChild();
                    LOG.finest("The first child node is: " + anchor.getNodeName());
                    if (!anchor.getNodeName().equals("g:glinkAnchor")) {
                        throw new SAXException("Expected a node with name 'g:glinkAnchor' but it was '" + anchor.getNodeName() + "' instead.");
                    }
                    Node glink = anchor.getNextSibling();
                    if (action.equals(GLinkAction.DELETE_ALL_GLINKS)) {
                        removeNodes.add(nodes.item(i));
                    } else if (glink.getTextContent().trim().equals(toURL)) {
                        if (action.equals(GLinkAction.CREATE_GLINK)) {
                            anchor.setTextContent(toName);
                            Node relation = glink.getNextSibling();
                            if ("g:relation".equals(relation.getNodeName())) {
                                relation.setTextContent(fromRelValue + GLinkPattern.RELATION_SEPARATOR + toRelValue);
                            } else {
                                Node newRelation = doc.createElement("g:relation");
                                LOG.finest("2) Creating text node: " + fromRelValue + GLinkPattern.RELATION_SEPARATOR + toRelValue);
                                Node textNode = doc.createTextNode(fromRelValue + GLinkPattern.RELATION_SEPARATOR + toRelValue);
                                newRelation.appendChild(textNode);
                                nodes.item(i).insertBefore(newRelation, relation);
                                relation = newRelation;
                            }
                            Node lastAuthor = relation.getNextSibling();
                            lastAuthor.setTextContent(authenticated.getScreenName());
                            Node lastChange = lastAuthor.getNextSibling();
                            lastChange.setTextContent(isoFormat.format(new Date()));
                        } else if (action.equals(GLinkAction.DELETE_GLINK)) {
                            removeNodes.add(nodes.item(i));
                        }
                        foundToURL = true;
                    }
                }
                for (Node node : removeNodes) {
                    node.getParentNode().removeChild(node);
                }
            } catch (FileNotFoundException ex) {
                if (action.equals(GLinkAction.CREATE_GLINK)) {
                    docRoot = newGLinkDocument(builder, fromURL);
                    doc = docRoot.getOwnerDocument();
                }
                if (LOG.isLoggable(Level.FINEST)) {
                    ex.printStackTrace();
                }
            } catch (SAXException ex) {
                if (LOG.isLoggable(Level.SEVERE)) {
                    ex.printStackTrace();
                }
                docRoot = newGLinkDocument(builder, fromURL);
                doc = docRoot.getOwnerDocument();
                File corruptedGLinksFile = fromURL.getGLinkXMLCorruptedFile();
                glinkXMLFile.renameTo(corruptedGLinksFile);
                LOG.finest("The number of bytes in the corruptedGLinksFile is: " + corruptedGLinksFile.length());
                if (corruptedGLinksFile.length() <= 0) {
                    try {
                        corruptedGLinksFile.delete();
                    } catch (SecurityException exx) {
                        exx.printStackTrace();
                    }
                }
            }
            if (!foundToURL && action.equals(GLinkAction.CREATE_GLINK)) {
                if (docRoot == null) {
                    docRoot = doc.getDocumentElement();
                }
                Element glinkElem = doc.createElement("g:link");
                Element childElem = doc.createElement("g:glinkAnchor");
                LOG.finest("3) Creating text node: " + toName);
                Node textNode = doc.createTextNode(toName);
                childElem.appendChild(textNode);
                glinkElem.appendChild(childElem);
                childElem = doc.createElement("g:glink");
                LOG.finest("4) Creating text node: " + toURL.toString());
                textNode = doc.createTextNode(toURL.toString());
                childElem.appendChild(textNode);
                glinkElem.appendChild(childElem);
                childElem = doc.createElement("g:relation");
                LOG.finest("5) Creating text node: " + fromRelValue + GLinkPattern.RELATION_SEPARATOR + toRelValue);
                textNode = doc.createTextNode(fromRelValue + GLinkPattern.RELATION_SEPARATOR + toRelValue);
                childElem.appendChild(textNode);
                glinkElem.appendChild(childElem);
                childElem = doc.createElement("g:lastAuthorId");
                LOG.finest("6) Creating text node: " + authenticated.getScreenName());
                textNode = doc.createTextNode(authenticated.getScreenName());
                childElem.appendChild(textNode);
                glinkElem.appendChild(childElem);
                childElem = doc.createElement("g:lastChangeDate");
                LOG.finest("7) Creating text node: " + isoFormat.format(new Date()));
                textNode = doc.createTextNode(isoFormat.format(new Date()));
                childElem.appendChild(textNode);
                glinkElem.appendChild(childElem);
                docRoot.appendChild(glinkElem);
            }
            if (doc != null) {
                glinkXMLFile.getParentFile().mkdirs();
                PrintWriter out = new PrintWriter(new BufferedWriter(new FileWriter(glinkXMLFile)));
                DOMSource domSource = new DOMSource(doc);
                StreamResult streamResult = new StreamResult(out);
                TransformerFactory tf = TransformerFactory.newInstance();
                Transformer serializer = tf.newTransformer();
                serializer.setOutputProperty(OutputKeys.ENCODING, "UTF-8");
                serializer.setOutputProperty(OutputKeys.INDENT, "no");
                serializer.transform(domSource, streamResult);
                out.close();
                File forceNetworkReload = fromURL.getForceNetworkReloadFile();
                forceNetworkReload.getParentFile().mkdirs();
                forceNetworkReload.createNewFile();
            }
        } catch (ParserConfigurationException ex) {
            if (LOG.isLoggable(Level.FINEST)) {
                ex.printStackTrace();
            }
        } catch (IOException ex) {
            if (LOG.isLoggable(Level.FINEST)) {
                ex.printStackTrace();
            }
        } catch (TransformerConfigurationException ex) {
            if (LOG.isLoggable(Level.FINEST)) {
                ex.printStackTrace();
            }
        } catch (TransformerException ex) {
            if (LOG.isLoggable(Level.FINEST)) {
                ex.printStackTrace();
            }
        } finally {
            if (fileLockStream != null) {
                try {
                    if (fileLock != null) {
                        fileLock.release();
                    }
                    fileLockStream.close();
                } catch (IOException ex) {
                    if (LOG.isLoggable(Level.FINEST)) {
                        ex.printStackTrace();
                    }
                }
            }
        }
    }

    public Element newGLinkDocument(DocumentBuilder builder, GLinkURL fromURL) {
        Document doc = builder.newDocument();
        Element docRoot = doc.createElement("g:glinks");
        docRoot.setAttribute("xmlns:g", "http://www.gedcombrowser.org/gedapi/glinks-schema");
        docRoot.setAttribute("xmlns:xsi", "http://www.w3.org/2001/XMLSchema-instance");
        docRoot.setAttribute("xsi:schemaLocation", "http://www.gedcombrowser.org/gedapi/glinks-schema " + XSD_LOCATION);
        docRoot.setAttribute("version", "1.0");
        doc.appendChild(docRoot);
        Element individualElem = doc.createElement("g:individual");
        LOG.finest("1) Creating text node: " + fromURL.toString());
        Node fromURLNode = doc.createTextNode(fromURL.toString());
        individualElem.appendChild(fromURLNode);
        docRoot.appendChild(individualElem);
        return docRoot;
    }
}
