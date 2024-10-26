package de.annotatio.client;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.io.StringReader;
import java.io.StringWriter;
import java.io.UnsupportedEncodingException;
import java.net.HttpURLConnection;
import java.net.MalformedURLException;
import java.net.URL;
import java.net.URLConnection;
import java.net.URLDecoder;
import java.net.URLEncoder;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.text.DateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.Iterator;
import java.util.ListIterator;
import java.util.Locale;
import java.util.Map;
import java.util.Properties;
import java.util.Random;
import java.util.Stack;
import java.util.Vector;
import javax.net.ssl.SSLHandshakeException;
import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.ParserConfigurationException;
import javax.xml.transform.OutputKeys;
import javax.xml.transform.Transformer;
import javax.xml.transform.TransformerConfigurationException;
import javax.xml.transform.TransformerException;
import javax.xml.transform.TransformerFactory;
import javax.xml.transform.TransformerFactoryConfigurationError;
import javax.xml.transform.dom.DOMResult;
import javax.xml.transform.dom.DOMSource;
import javax.xml.transform.stream.StreamResult;
import javax.xml.transform.stream.StreamSource;
import org.cyberneko.html.parsers.DOMParser;
import org.w3c.dom.DOMException;
import org.w3c.dom.Document;
import org.w3c.dom.DocumentType;
import org.w3c.dom.Element;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;
import org.w3c.dom.Text;
import org.w3c.dom.traversal.DocumentTraversal;
import org.w3c.dom.traversal.NodeFilter;
import org.w3c.dom.traversal.NodeIterator;
import org.xml.sax.InputSource;
import org.xml.sax.SAXException;
import org.xml.sax.SAXParseException;

/**
 * @author alex
 *
 * TODO To change the template for this generated type comment go to
 * Window - Preferences - Java - Code Style - Code Templates
 */
class ResultItem {

    ResultItem(String aword, int aoccurences) {
        word = aword;
        occurences = aoccurences;
    }

    String word;

    int occurences;
}

class ContainerAnnotationPost {

    String url;

    String context;

    ArrayList contentRangeList;

    /**
	 * @return Returns the context.
	 */
    public String getContext() {
        return context;
    }

    /**
	 * @param context The context to set.
	 */
    public void setContext(String context) {
        this.context = context;
    }

    /**
	 * @return Returns the url.
	 */
    public String getUrl() {
        return url;
    }

    /**
	 * @param url The url to set.
	 */
    public void setUrl(String url) {
        this.url = url;
    }

    /**
	 * @return Returns the contentRangeList.
	 */
    public ArrayList getContentRangeList() {
        return contentRangeList;
    }

    /**
	 * @param contentRangeList The contentRangeList to set.
	 */
    public void setContentRangeList(ArrayList contentRangeList) {
        this.contentRangeList = contentRangeList;
    }
}

public class AnnotationManager {

    private static final String IO_ERROR_CONNECTING = "<html><head><title>Error connecting to remote annotation server</title></head><body><h2>Error - The remote annotation server could not be reached</h2>" + "<p>Reason:<br>" + "While trying to connect to the remote annotation server %s as user %s" + " a connection error occured. The error message I got is:</p><p>%s</p><p>" + "Check whether your network connection to that server is working and if you got response code 401, check your credentials.</p></body></html>";

    private static final String NO_VALID_RESPONSE_FROM_REMOTE = "<html><head><title>Error parsing annotations</title></head><body><h2>Error - Annotations could not be processed</h2>" + "<p>Reason:<br>" + "Annotatio did not get a valid response from the annotation server. Either the server could not be reached or the response was cripled.<br>" + "Maybe the annotation server is incompatible to Annotatio-Client</p><p>Use your browsers back button to return to the last page.</p></body></html>";

    private static final String DOCUMENT_COULD_NOT_BE_PARSED = "<html><head><title>Document could not be parsed</title></head><body><h2>Error - Document could not be parsed</h2>" + "<p>Reason:<br>" + "Annotatio only supports HTML and well-formed XML files. Maybe your document posed as an XML file but was not well parsed." + "</p><p>Use your browsers back button to return to the last page.</p></body></html>";

    private static final String SECURE_CONNECTION_NOT_SUPPORTED = "<html><head><title>Secure Connection not supported</title></head><body><h2>Error - Secure connection via SSL not supported</h2>" + "<p>Reason:<br>" + "The remote secure site presented a security certificate unknown to the System." + "</p><p>Use your browsers back button to return to the last page.</p></body></html>";

    private Connection con = null;

    private Connection user_db = null;

    protected String annoNS = "http://www.w3.org/2000/10/annotation-ns#";

    protected String rdfNS = "http://www.w3.org/1999/02/22-rdf-syntax-ns#";

    protected String dubNS = "http://purl.org/dc/elements/1.0/";

    protected String threadNS = "http://www.w3.org/2001/03/thread#";

    protected String htmlNS = "http://www.w3.org/1999/xx/http#";

    protected String alNS = "http://www.annotatio.de/al";

    private String r_username = null;

    private String r_password = null;

    private String r_url = null;

    private String file_storage_path;

    private String db_storage_path;

    private String realname;

    private Transformer xformer = null;

    private DocumentBuilderFactory factory;

    private DocumentBuilder builder;

    private ArrayList containerAnnotation = new ArrayList();

    /**
	 * Main class which is responsible for handing out the apropiate subclasses
	 * @param props Configuration parameters that could have been read from an configuration file
	 */
    public AnnotationManager(Properties props) {
        this.file_storage_path = props.getProperty("root") == null ? "" : props.getProperty("root");
        this.db_storage_path = props.getProperty("db_root") == null ? "" : props.getProperty("db_root");
        realname = props.getProperty("realname") == null ? "" : props.getProperty("realname");
        r_url = props.getProperty("r_url");
        r_url = (r_url != null) ? r_url.trim() : null;
        r_username = props.getProperty("r_user");
        r_username = (r_username != null) ? r_username.trim() : null;
        r_password = props.getProperty("r_password");
        r_password = (r_password != null) ? r_password.trim() : null;
        try {
            Class.forName("org.hsqldb.jdbcDriver");
        } catch (Exception e) {
            System.out.println("Error: failed to load internal database driver.\n Please put hsqldb into your classpath!");
            return;
        }
        try {
            String url = this.db_storage_path.matches("") ? "annotatio_db" : this.db_storage_path + "/" + "annotatio_db";
            con = DriverManager.getConnection("jdbc:hsqldb:file:" + url, "sa", "");
        } catch (SQLException e1) {
            System.out.println("Error establishing connection to annotation database. Make sure local database file is" + " accessible and writable.\n Maybe you need to remove lock file\n" + e1.getLocalizedMessage());
            return;
        }
        try {
            String url = getDb_storage_path().matches("") ? "user_db" : getDb_storage_path() + "/" + "user_db";
            user_db = DriverManager.getConnection("jdbc:hsqldb:file:" + url, "sa", "");
        } catch (SQLException e1) {
            System.out.println("Error establishing connection to user database. Make sure local database file is" + " accessible and writable.\n Maybe you need to remove lock file\n" + e1.getLocalizedMessage());
            return;
        }
        try {
            xformer = TransformerFactory.newInstance().newTransformer();
        } catch (TransformerConfigurationException e1) {
            e1.printStackTrace();
        } catch (TransformerFactoryConfigurationError e1) {
            e1.printStackTrace();
        }
        try {
            factory = DocumentBuilderFactory.newInstance();
            factory.setNamespaceAware(true);
            builder = factory.newDocumentBuilder();
        } catch (ParserConfigurationException pce) {
            pce.printStackTrace();
        }
    }

    Transformer getTransformer() {
        return xformer;
    }

    DocumentBuilder getBuilder() {
        return builder;
    }

    /**
	 * Query for annotations to a given and embed them into the document
	 * @param url URL under which the document for which the annotations are to be queried, can be found
	 * @param htmlcompat Whether the document described by URL is HTML compatible and not an XML Document
	 * @return Source of document with annotations build in (in HTML compat mode as website)
	 */
    public String deliver_general(String url, boolean htmlcompat, PermissionManager permMan) {
        StringWriter resp = new StringWriter();
        try {
            org.apache.xerces.parsers.DOMParser parser = new org.apache.xerces.parsers.DOMParser();
            try {
                parser.parse(url);
            } catch (SSLHandshakeException essl) {
                return (SECURE_CONNECTION_NOT_SUPPORTED);
            } catch (SAXParseException saxep) {
                System.out.println("Parsing the document XML compliant failed. Trying to parse HTML compliant.");
                parser = new org.cyberneko.html.parsers.DOMParser();
                parser.parse(url);
            } catch (Exception e) {
                return (DOCUMENT_COULD_NOT_BE_PARSED);
            }
            Document htmldoc = parser.getDocument();
            Document div_template = null;
            if (htmlcompat) {
                addHTMLBaseElement(url, htmldoc);
                parser.parse("file:annotation.html");
                div_template = parser.getDocument();
            }
            String queryanswer = (localConnection()) ? query(url, false, permMan) : query_remote(url, r_url, r_username, r_password);
            StringReader strread = new StringReader(queryanswer);
            Document queryans = builder.newDocument();
            try {
                xformer.transform(new StreamSource(strread), new DOMResult(queryans));
            } catch (TransformerException e) {
                return (NO_VALID_RESPONSE_FROM_REMOTE);
            }
            NodeList annotations = queryans.getElementsByTagNameNS(annoNS, "Annotation");
            System.out.println(annotations.getLength() + " Annotation(s) found.");
            for (int annotation_count = 0; annotation_count < annotations.getLength(); annotation_count++) {
                try {
                    AnnotationBean ab = new AnnotationBean();
                    ab.setAnnotationResource((Element) annotations.item(annotation_count));
                    ab.setAnnotationBody(returnAnnoBody(ab.getAnnotationBodyURL()));
                    NodeRange xprange = null;
                    if (ab.getAnnoText() == null || ab.getContextWords() == null) {
                        XPointer xptr = new XPointer(htmldoc);
                        xprange = xptr.getRange(ab.getAnnoContext(), htmldoc);
                    } else {
                        xprange = getRangeFromText(htmldoc, ab.getAnnoText(), ab.getContextWords());
                    }
                    if (xprange == null || xprange.getFirstnode() == null) {
                        System.out.println("The context was not found!");
                    } else {
                        System.out.println("Range found: \"" + xprange.getSearchText() + "\", Begin: " + xprange.getBegin() + ", Length: " + xprange.getLength());
                        Node node = xprange.getFirstnode();
                        ArrayList<Node> nl = findFollowingNodes(xprange, node);
                        int tmplen = xprange.getLength();
                        Iterator<Node> list = nl.iterator();
                        while (list.hasNext()) {
                            Element tagged = createWrappingElement(htmlcompat, htmldoc, div_template, annotation_count, ab);
                            Node cnode = list.next();
                            if (nl.indexOf(cnode) == 0) {
                                int nodelen = cnode.getNodeValue().length();
                                Text spannednode = ((Text) cnode).splitText(xprange.getBegin() - 1);
                                if (nl.size() == 1) {
                                    spannednode.splitText(xprange.getLength());
                                }
                                tmplen = tmplen - (nodelen - cnode.getNodeValue().length());
                                cnode.getParentNode().insertBefore(tagged, spannednode);
                                tagged.appendChild(spannednode);
                            } else if (nl.indexOf(cnode) == nl.size() - 1) {
                                ((Text) cnode).splitText(tmplen);
                                cnode.getParentNode().insertBefore(tagged, cnode);
                                tagged.appendChild(cnode);
                            } else {
                                cnode.getParentNode().insertBefore(tagged, cnode);
                                tagged.appendChild(cnode);
                                tmplen = tmplen - cnode.getNodeValue().trim().length();
                            }
                        }
                    }
                } catch (XPointerRangeException xptrexp) {
                    System.out.println("Error at resolving XPointer for Annotation " + (annotation_count + 1) + ":\n" + xptrexp.getLocalizedMessage());
                }
            }
            if (htmlcompat) {
                DocumentTraversal htravers = (DocumentTraversal) htmldoc;
                NodeIterator hiterator = htravers.createNodeIterator(htmldoc, NodeFilter.SHOW_ELEMENT, null, true);
                Node node;
                while ((node = hiterator.nextNode()) != null) {
                    String nname = node.getNodeName().toLowerCase();
                    if (!node.hasChildNodes()) {
                        if (nname.equals("script") || nname.equals("style") || nname.equals("a") || nname.equals("div")) {
                            node.appendChild(htmldoc.createComment("."));
                        }
                    }
                }
            }
            Transformer transformer = TransformerFactory.newInstance().newTransformer();
            DocumentType doctype = htmldoc.getDoctype();
            transformer.setOutputProperty(OutputKeys.INDENT, "no");
            if (doctype != null) {
                transformer.setOutputProperty(OutputKeys.DOCTYPE_PUBLIC, doctype.getPublicId());
                if (doctype.getSystemId() != null) transformer.setOutputProperty(OutputKeys.DOCTYPE_SYSTEM, doctype.getSystemId());
            }
            transformer.transform(new DOMSource(htmldoc), new StreamResult(resp));
        } catch (IOException e) {
            return (String.format(IO_ERROR_CONNECTING, r_url, r_username, e.getLocalizedMessage()));
        } catch (Exception e) {
            e.printStackTrace();
            return ("");
        }
        return resp.toString();
    }

    private void addHTMLBaseElement(String url, Document htmldoc) throws FileNotFoundException, IOException {
        Element base = htmldoc.createElement("base");
        base.setAttribute("href", url);
        BufferedReader in = new BufferedReader(new FileReader("script.js"));
        String script_text = "\n";
        while (in.ready()) {
            script_text = script_text.concat(in.readLine() + "\n");
        }
        Element script = htmldoc.createElement("script");
        script.setAttribute("type", "text/javascript");
        script.appendChild(htmldoc.createTextNode(script_text));
        BufferedReader in2 = new BufferedReader(new FileReader("anno_style"));
        String anno_style = "\n";
        while (in2.ready()) {
            anno_style = anno_style.concat(in2.readLine() + "\n");
        }
        Element style = htmldoc.createElement("style");
        style.setAttribute("type", "text/css");
        style.appendChild(htmldoc.createComment(anno_style));
        NodeList headlist = htmldoc.getElementsByTagName("head");
        if (headlist.getLength() > 0) {
            Element head = (Element) headlist.item(0);
            NodeList linkl = head.getElementsByTagName("link");
            if (linkl.getLength() > 0) {
                head.insertBefore(base, linkl.item(0));
            } else {
                head.insertBefore(base, head.getFirstChild());
            }
            head.appendChild(style);
            head.appendChild(script);
        } else {
            Element head = htmldoc.createElement("head");
            htmldoc.getDocumentElement().insertBefore(head, htmldoc.getDocumentElement().getFirstChild());
            head.appendChild(base);
            head.appendChild(style);
            head.appendChild(script);
        }
    }

    private Element createWrappingElement(boolean htmlcompat, Document htmldoc, Document div_template, int annotation_count, AnnotationBean ab) {
        Element tagged;
        if (htmlcompat) {
            tagged = htmldoc.createElement("span");
            tagged.setAttribute("style", "background-color:yellow;color:black");
            tagged.setAttribute("onMouseOver", "showWMTT('anno" + annotation_count + "')");
            tagged.setAttribute("onMouseOut", "hideWMTT()");
            NodeList bdlist = htmldoc.getElementsByTagName("body");
            if (bdlist.getLength() > 0) {
                Node body = bdlist.item(0);
                Element new_div = (Element) htmldoc.importNode(div_template.getElementsByTagName("div").item(0), true);
                Node htmlb = ab.getAnnotationHTMLBodyNode();
                if (htmlb != null) {
                    Node anno_body = htmldoc.importNode(htmlb, true);
                    NodeList annob_childr = anno_body.getChildNodes();
                    Node tmpelem = new_div.getElementsByTagName("anno-text").item(0);
                    Element sp = htmldoc.createElement("span");
                    DateFormat df2 = DateFormat.getDateTimeInstance(DateFormat.LONG, DateFormat.SHORT, Locale.getDefault());
                    Node authelem = new_div.getElementsByTagName("anno_author").item(0);
                    Node auth_t = htmldoc.createTextNode(ab.getAnnoAuthor() + " on " + df2.format(ab.getAnnoDate()));
                    authelem.getParentNode().replaceChild(auth_t, authelem);
                    tmpelem.getParentNode().replaceChild(sp, tmpelem);
                    int anno_body_child_len = annob_childr.getLength();
                    for (int ch_counter = 0; ch_counter < anno_body_child_len; ch_counter++) {
                        sp.appendChild(annob_childr.item(0));
                    }
                    body.insertBefore(new_div, body.getFirstChild());
                    new_div.setAttribute("id", "anno" + new Integer(annotation_count).toString());
                }
            }
        } else {
            tagged = (Element) htmldoc.importNode(ab.getAnnotationElement(), false);
            tagged.setAttribute("xmlns:a", annoNS);
            tagged.setAttribute("xmlns:r", rdfNS);
            tagged.setAttribute("xmlns:d", dubNS);
            tagged.appendChild(htmldoc.importNode(ab.getAnnotationElement().getElementsByTagNameNS(annoNS, "body").item(0), true));
            tagged.appendChild(htmldoc.importNode(ab.getAnnotationElement().getElementsByTagNameNS(rdfNS, "type").item(0), true));
        }
        return tagged;
    }

    /**
	 * Given a range text and a starting node, find all other affected document nodes
	 * @param xprange
	 * @param node
	 * @return List of nodes affected
	 */
    private ArrayList<Node> findFollowingNodes(NodeRange xprange, Node node) {
        ArrayList<Node> nl = new ArrayList<Node>();
        int pos = 0;
        while (pos < (xprange.getBegin() + xprange.getLength())) {
            if (node.getNodeType() == Node.TEXT_NODE) {
                String nodetext = node.getNodeValue().trim();
                pos = pos + nodetext.length();
                if (pos >= xprange.getBegin()) {
                    node.setNodeValue(node.getNodeValue().trim());
                    node.setNodeValue(node.getNodeValue().replaceAll("\\s", " "));
                    node.setNodeValue(node.getNodeValue().replaceAll("\\s{2,}", " "));
                    nl.add(node);
                }
            }
            if (node.hasChildNodes()) {
                node = node.getFirstChild();
            } else {
                Node tmp = node.getNextSibling();
                Node tmp2 = node;
                while (tmp == null) {
                    tmp = tmp2.getParentNode().getNextSibling();
                    tmp2 = tmp2.getParentNode();
                }
                node = tmp;
            }
        }
        return nl;
    }

    void publishToRemoteServer(Document annodoc) {
        OutputStream outpstr = null;
        URL remURL;
        try {
            remURL = new URL(r_url);
            HttpURLConnection url_con = (HttpURLConnection) remURL.openConnection();
            url_con.setRequestMethod("POST");
            if (useAuthorization()) {
                System.out.println("Using authorization to Post");
                url_con.setRequestProperty("Authorization", "Basic " + getBasicAuthorizationString());
            }
            url_con.setRequestProperty("Content-Type", "application/xml");
            url_con.setRequestProperty("charset", "utf-8");
            url_con.setDoInput(true);
            url_con.setDoOutput(true);
            outpstr = url_con.getOutputStream();
            url_con.connect();
            Transformer transformer = TransformerFactory.newInstance().newTransformer();
            transformer.setOutputProperty(OutputKeys.ENCODING, "UTF-8");
            transformer.transform(new DOMSource(annodoc), new StreamResult(outpstr));
            outpstr.flush();
            outpstr.close();
            System.out.println(url_con.getResponseMessage());
            url_con.disconnect();
        } catch (MalformedURLException e1) {
            e1.printStackTrace();
        } catch (IOException e) {
            e.printStackTrace();
        } catch (TransformerConfigurationException e) {
            e.printStackTrace();
        } catch (TransformerFactoryConfigurationError e) {
            e.printStackTrace();
        } catch (TransformerException e) {
            e.printStackTrace();
        }
    }

    /**
	 * Return version of Annotatio
	 * @return
	 */
    public String return_version() {
        return ("Annotatio 1.1 Beta 1");
    }

    /**
	 * Return a the hashed version of username and password so it can be given to the remote
	 * webserver for BASIC authorization
	 * @return Base64 hash of username:password
	 */
    private String getBasicAuthorizationString() {
        if (localConnection()) return null;
        return new sun.misc.BASE64Encoder().encode(new String(r_username + ":" + r_password).getBytes());
    }

    /**
	 * Returns whether the annotations manager is supposed to query local database or the remote database
	 * @return
	 */
    private boolean localConnection() {
        if (r_url == null) return true;
        return false;
    }

    /**
	 * Return whether annotatio is supposed to authorize to the remote server
	 * @return
	 */
    private boolean useAuthorization() {
        if (r_username != null) return true;
        return false;
    }

    /**
	 * Returns the Document of a URL that represents a HTML Document
	 * Currently it is used for returning the Document of the Annotation
	 * @param url
	 * @return
	 */
    private Element returnAnnoBody(final String url) {
        DOMParser parser = new DOMParser();
        try {
            URL bodyURL = new URL(url);
            URLConnection url_con = bodyURL.openConnection();
            if (useAuthorization()) {
                url_con.setRequestProperty("Authorization", "Basic " + getBasicAuthorizationString());
            }
            InputStream content = url_con.getInputStream();
            InputSource insource = new InputSource(content);
            parser.parse(insource);
        } catch (SAXException e) {
            e.printStackTrace();
            return null;
        } catch (IOException e) {
            e.printStackTrace();
            return null;
        }
        Document annodoc = parser.getDocument();
        return annodoc.getDocumentElement();
    }

    /**
	 * Recursive funktion which returns the trimmed content of all text node which
	 * are below and on the same level as the passed node
	 * @param node 
	 * @return Text of all Text-Nodes
	 */
    String extractTextFromNode(Node node) {
        String text = "";
        if (node.getNodeType() == Node.TEXT_NODE) {
            return (node.getNodeValue().trim());
        } else {
            NodeList childlist = node.getChildNodes();
            for (int i = 0; i < childlist.getLength(); i++) {
                text = text + " " + extractTextFromNode(childlist.item(i));
            }
        }
        return text;
    }

    public AnnotationBean getAnnoDocumentByHash(String hash, PermissionManager permMan) {
        File rfile = new File(this.file_storage_path + "/annotation/" + hash);
        if (permMan.checkReadPermission(hash)) {
            if (rfile.canRead()) {
                try {
                    Document rdoc = builder.parse(rfile);
                    AnnotationBean anno = new AnnotationBean();
                    NodeList annL = rdoc.getElementsByTagNameNS(annoNS, "Annotation");
                    if (annL.getLength() == 0) {
                        System.out.println("Error while parsing annotation from resource file. No Annotation Element was found.");
                        return null;
                    }
                    anno.setAnnotationResource((Element) annL.item(0));
                    return anno;
                } catch (SAXException e) {
                    e.printStackTrace();
                } catch (IOException e) {
                    e.printStackTrace();
                }
            } else {
                System.out.println("Tried to open resource file " + rfile.getAbsolutePath() + " but couldn't open it for reading. " + "\n Check whether file exists and is readable");
            }
        } else {
            System.out.println("User " + permMan.getPrincipal() + " has no read permissions for " + hash + " - skipping.");
        }
        return null;
    }

    /**
	 * Returns an array of all annotations the authentificated user is allowed to search for
	 * @param user
	 * @return
	 */
    public ArrayList searchByUser(PermissionManager permMan) {
        return permMan.getUserSearchableAnnotations(permMan.getPrincipal());
    }

    public HashMap searchByUserBean(PermissionManager permMan) {
        HashMap hashm = new HashMap();
        ArrayList hashlist = searchByUser(permMan);
        ListIterator hashIt = hashlist.listIterator();
        while (hashIt.hasNext()) {
            String hashs = (String) hashIt.next();
            hashm.put(hashs, getAnnoDocumentByHash(hashs, permMan));
        }
        return hashm;
    }

    /**
	 * Queries the database to return a list of Annotation hashes for a given URL.
	 * Uses the view ANNOURL in the database and searches for matching strings containing the
	 * passed URL
	 * @param url URL to match
	 * @return ArrayList of Strings
	 */
    private ArrayList searchByURL(String url) {
        ArrayList rlist = new ArrayList();
        try {
            String query = ("select HASH from ANNOURL where URL='" + url + "'");
            ResultSet rs = con.createStatement().executeQuery(query);
            while (rs.next()) {
                rlist.add(rs.getString("hash"));
            }
        } catch (SQLException e) {
            System.out.println("Query String: select HASH from ANNOURL where URL='" + url + "' was wrong");
            System.out.println(e.getMessage());
            return rlist;
        }
        return rlist;
    }

    /**
	 * Queries the system to return a list of Annotation hashes for a given URL.
	 * Builds a frequency table from the Document passed in URL and compares it with
	 * the frequency table found in the FREQS table.
	 * 
	 * @param url URL of the document to compare with the frequency table
	 * @return ArrayList of Strings containing the hash value
	 */
    private ArrayList searchByFrequency(String url) {
        int tolerance = 10;
        ArrayList rlist = new ArrayList();
        DOMParser parser = new DOMParser();
        Iterator it = null;
        try {
            parser.parse(url);
            Document origdoc = parser.getDocument();
            WordFreq wf = new WordFreq(extractTextFromNode(origdoc));
            it = wf.getSortedWordlist();
        } catch (SAXException e) {
            e.printStackTrace();
            return rlist;
        } catch (IOException e) {
            e.printStackTrace();
            return rlist;
        }
        Map.Entry ent;
        String word;
        int count;
        int i = 0;
        String querylist = "";
        ArrayList freqs = new ArrayList();
        while (it != null && it.hasNext()) {
            ent = (Map.Entry) it.next();
            word = ((String) ent.getKey());
            count = ((Counter) ent.getValue()).count;
            if ((word.length() > 4) && (i < 10)) {
                if (i > 0) querylist = querylist + " OR";
                querylist = (querylist + " WORD='" + word + "'");
                freqs.add(new ResultItem(word, count));
                i++;
            }
        }
        String query = ("SELECT DISTINCT ANNOID FROM FREQS WHERE " + querylist);
        try {
            ResultSet rs = con.createStatement().executeQuery(query);
            while (rs.next()) {
                int poss_anno = rs.getInt("AnnoID");
                ResultItem tmp = (ResultItem) freqs.get(0);
                int maxdist = tmp.occurences;
                int maxdistcounter = 0;
                int distance = 0;
                query = ("SELECT WORD, OCCURENCE FROM FREQS WHERE ANNOID=" + poss_anno);
                ResultSet rs2 = con.createStatement().executeQuery(query);
                Map m = new HashMap();
                while (rs2.next()) {
                    m.put(rs2.getString("word"), new Integer(rs2.getInt("occurence")));
                }
                for (int ii = 0; ii < freqs.size(); ii++) {
                    tmp = (ResultItem) freqs.get(ii);
                    Integer occ = (Integer) m.get(tmp.word);
                    if (occ == null) {
                        maxdistcounter++;
                    } else {
                        int currdistance = Math.abs(occ.intValue() - tmp.occurences);
                        distance = distance + currdistance;
                        if (currdistance > maxdist) maxdist = currdistance;
                    }
                }
                distance = distance + (maxdistcounter * maxdist);
                int percentdistance = ((distance * 100) / (maxdist * freqs.size()));
                System.out.println("Possible Annotation " + poss_anno + " found to be " + percentdistance + "% different than document. " + "(" + distance + "/" + maxdist * freqs.size() + ")");
                if (percentdistance <= tolerance) {
                    try {
                        query = ("select HASH from ANNOTATION where AnnoID=" + poss_anno);
                        ResultSet rs3 = con.createStatement().executeQuery(query);
                        while (rs3.next()) {
                            rlist.add(rs3.getString("hash"));
                        }
                    } catch (SQLException e) {
                        System.out.println("Querying for Hash with possible annotation did not work\n" + "Query was: " + query);
                        System.out.println(e.getMessage());
                    }
                }
            }
        } catch (SQLException e1) {
            System.out.println("Error querying freqency list\n" + "Query was: " + query);
            e1.printStackTrace();
        }
        return rlist;
    }

    /**
	 * Query a remote Annotea compatible server (like Annotatio) for annotations to a given URL
	 * @param url Document for which we are trying to find annotations
	 * @param r_url URL of the remote Annotea server
	 * @param r_username Username for authorization
	 * @param r_password Password for authorization
	 * @return
	 * @throws IOException
	 */
    public String query_remote(String url, String r_url, String r_username, String r_password) throws IOException {
        URL remURL = new URL(r_url + "?w3c_annotates=" + URLEncoder.encode(url, "UTF-8").toString());
        System.out.println("Remote URL to query " + remURL);
        URLConnection url_con = remURL.openConnection();
        if (useAuthorization()) {
            url_con.setRequestProperty("Authorization", "Basic " + getBasicAuthorizationString());
        }
        url_con.setRequestProperty("Accept", "application/xml");
        InputStream content = url_con.getInputStream();
        BufferedReader inRead = new BufferedReader(new InputStreamReader(content, "UTF-8"));
        String out = "";
        while (inRead.ready()) out = out + inRead.readLine() + "\n";
        return out;
    }

    public String query(String url, boolean includebody, PermissionManager permMan) {
        System.out.println("Annotation query for: " + url);
        ArrayList hashlist = searchByFrequency(url);
        if (hashlist.size() == 0) {
            System.out.println("Nothing found via Freqs search, falling back on URL search...");
            hashlist = searchByURL(url);
        }
        System.out.println("Found " + hashlist.size() + " result(s).");
        Document newdoc = builder.newDocument();
        Element rootelem = newdoc.createElementNS(rdfNS, "r:RDF");
        rootelem.setAttribute("xmlns:r", rdfNS);
        rootelem.setAttribute("xmlns:a", annoNS);
        rootelem.setAttribute("xmlns:d", dubNS);
        rootelem.setAttribute("xmlns:t", threadNS);
        newdoc.appendChild(rootelem);
        while (hashlist.size() > 0) {
            String hash = (String) hashlist.remove(0);
            AnnotationBean annobean = getAnnoDocumentByHash(hash, permMan);
            if (annobean != null) {
                Element newnode = (Element) newdoc.importNode(annobean.getAnnotationElement(), true);
                if (includebody) {
                    NodeList annobodylist = newnode.getElementsByTagNameNS(annoNS, "body");
                    if (annobodylist.getLength() > 0) {
                        Element annob = (Element) annobodylist.item(0);
                        String annobres = annob.getAttributeNS(rdfNS, "resource");
                        annob.removeAttributeNS(rdfNS, "resource");
                        annob.setAttribute("xmlns:h", htmlNS);
                        Element htmlmess = newdoc.createElementNS(htmlNS, "h:Message");
                        annob.appendChild(htmlmess);
                        Element htmlcont = newdoc.createElementNS(htmlNS, "h:ContentType");
                        htmlmess.appendChild(htmlcont);
                        htmlcont.appendChild(newdoc.createTextNode("text/html"));
                        Element htmlbody = newdoc.createElementNS(htmlNS, "h:Body");
                        htmlbody.setAttributeNS(rdfNS, "r:parseType", "Literal");
                        htmlmess.appendChild(htmlbody);
                        try {
                            DOMParser parser = new DOMParser();
                            parser.parse(annobres);
                            Document annobodydoc = parser.getDocument();
                            Document anbd = builder.newDocument();
                            Transformer transformer = TransformerFactory.newInstance().newTransformer();
                            transformer.transform(new DOMSource(annobodydoc), new DOMResult(anbd));
                            htmlbody.appendChild(newdoc.importNode(anbd.getDocumentElement(), true));
                        } catch (DOMException dome) {
                            dome.printStackTrace();
                        } catch (TransformerConfigurationException e) {
                            e.printStackTrace();
                        } catch (TransformerFactoryConfigurationError e) {
                            e.printStackTrace();
                        } catch (TransformerException e) {
                            e.printStackTrace();
                        } catch (SAXException e) {
                            e.printStackTrace();
                        } catch (IOException e) {
                            e.printStackTrace();
                        }
                    }
                }
                rootelem.appendChild(newnode);
            }
        }
        StringWriter resp = new StringWriter();
        try {
            Transformer transformer = TransformerFactory.newInstance().newTransformer();
            transformer.setOutputProperty(OutputKeys.ENCODING, "UTF-8");
            transformer.setOutputProperty(OutputKeys.STANDALONE, "true");
            transformer.setOutputProperty(OutputKeys.INDENT, "yes");
            transformer.transform(new DOMSource(newdoc), new StreamResult(resp));
        } catch (TransformerException e) {
            e.printStackTrace();
            return "";
        }
        return resp.toString();
    }

    /**
	 * Imports Annotations RDFs into Annotatio
	 * @param url URL of XML with saved annotations
	 * @return
	 */
    String import_annotations(String url) {
        int success_counter = 0;
        org.apache.xerces.parsers.DOMParser parser = new org.apache.xerces.parsers.DOMParser();
        try {
            parser.parse(url);
        } catch (SAXException e) {
            e.printStackTrace();
            return ("");
        } catch (IOException e) {
            e.printStackTrace();
            return ("");
        }
        Document annodoc = parser.getDocument();
        NodeList annolist = annodoc.getElementsByTagNameNS(annoNS, "Annotation");
        for (int i = 0; i < annolist.getLength(); i++) {
            int num_found = -1;
            Element annotation = (Element) annolist.item(i);
            String annoid = annotation.getAttributeNS(alNS, "id");
            if (annoid == null) {
                System.out.println("Could not find annotation ID.");
            } else {
                String query = "SELECT COUNT(*) FROM ANNOTATION WHERE HASH='" + annoid + "'";
                ResultSet rs;
                try {
                    rs = con.createStatement().executeQuery(query);
                    if (rs.next()) {
                        num_found = rs.getInt(1);
                    }
                } catch (SQLException e1) {
                    e1.printStackTrace();
                }
                if (num_found != 0) {
                    System.out.println("AnnotationID (" + annoid + ") was found already " + num_found + " time(s) in the database, skipping import.");
                } else {
                    Element annob = (Element) annotation.getElementsByTagNameNS(annoNS, "body").item(0);
                    NodeList htmll = annob.getElementsByTagName("html");
                    if (htmll.getLength() < 1) {
                        htmll = annob.getElementsByTagName("HTML");
                    }
                    Node htmlbody = htmll.item(0);
                    annob.removeChild(annob.getFirstChild());
                    annob.setAttributeNS(rdfNS, "r:resource", "http://127.0.0.1:8080/annotation/body/" + annoid);
                    Element rdfElem = (Element) annodoc.getElementsByTagNameNS(rdfNS, "RDF").item(0);
                    Document resanno = builder.newDocument();
                    Node nrdf = resanno.importNode(rdfElem, false);
                    Node nanno = resanno.importNode(annotation, true);
                    resanno.appendChild(nrdf);
                    nrdf.appendChild(nanno);
                    store(resanno.getDocumentElement());
                    createAnnoResource(resanno.getDocumentElement(), annoid);
                    createAnnoBody(htmlbody, annoid);
                    System.out.println("Imported Annotation " + annoid);
                    success_counter++;
                }
            }
        }
        return ("<html><body>Imported " + success_counter + " annotations.</body></html>");
    }

    /**
	 * Creates the database entries for our annotation document
	 * @param document The DOM-Document holding the annotation object we got from the client
	 */
    void store(Element document) {
        String annoNS = "http://www.w3.org/2000/10/annotation-ns#";
        String rdfNS = "http://www.w3.org/1999/02/22-rdf-syntax-ns#";
        String query;
        Element annotatesn = (Element) document.getElementsByTagNameNS(annoNS, "annotates").item(0);
        String url = annotatesn.getAttributeNS(rdfNS, "resource");
        int urlID = getURLID(url);
        if (urlID == 0) return;
        Element elem = (Element) document.getElementsByTagNameNS(annoNS, "body").item(0);
        String hash = elem.getAttributeNS(rdfNS, "resource");
        hash = hash.substring(hash.lastIndexOf("/") + 1);
        query = "INSERT INTO Annotation (AnnoID, Hash, Date) VALUES (NEXT VALUE FOR ANNOSEQ, '" + hash + "', now())";
        int annoID = 0;
        try {
            int rs = con.createStatement().executeUpdate(query);
            if (rs == 1) {
                query = ("SELECT AnnoID from Annotation WHERE hash='" + hash + "'");
                ResultSet rs2 = con.createStatement().executeQuery(query);
                if (rs2.next()) {
                    annoID = rs2.getInt("AnnoID");
                }
            }
        } catch (SQLException e1) {
            System.out.println("Error inserting Annotation");
            e1.printStackTrace();
        }
        if (annoID == 0) return;
        query = ("INSERT INTO ANNOMAP VALUES(" + urlID + "," + annoID + ")");
        try {
            int rs = con.createStatement().executeUpdate(query);
        } catch (SQLException e1) {
            System.out.println("Error inserting URL AND Annotation");
            e1.printStackTrace();
        }
        Element docident = (Element) annotatesn.getElementsByTagNameNS(alNS, "document-identifier").item(0);
        NodeList wordlist = docident.getElementsByTagNameNS(alNS, "word");
        for (int counter = 0; counter < wordlist.getLength(); counter++) {
            Element wordelem = (Element) wordlist.item(counter);
            String word = wordelem.getFirstChild().getNodeValue();
            String occur = wordelem.getAttributeNS(alNS, "freq");
            query = ("INSERT INTO FREQS VALUES(" + annoID + ",'" + word + "'," + occur + ")");
            try {
                int rs = con.createStatement().executeUpdate(query);
            } catch (SQLException e1) {
                System.out.println("Error inserting freqency word\n" + "Query was: " + query);
                e1.printStackTrace();
            }
        }
    }

    /**
	 * Looks up the URL string in the database and returns the mapped URL ID if it exists,
	 * if not it will create it and return the newly created ID
	 * @param url String refering to the URL of the annotated document
	 * @return The ID of the URL
	 */
    private int getURLID(String url) {
        int urlid = 0;
        try {
            String query = ("select URLID from URLMAP where URL='" + url + "'");
            ResultSet rs = con.createStatement().executeQuery(query);
            if (rs.next()) {
                urlid = rs.getInt("URLID");
            }
        } catch (SQLException e) {
            System.out.println(e.getMessage());
        }
        if (urlid == 0) {
            String query = ("INSERT INTO URLMAP (URLID, URL) VALUES (NEXT VALUE FOR URLSEQ, '" + url + "')");
            try {
                int rs = con.createStatement().executeUpdate(query);
            } catch (SQLException e1) {
                System.out.println("Error inserting URL");
                e1.printStackTrace();
            }
            try {
                String queryb = ("select URLID FROM URLMAP where URL='" + url + "'");
                ResultSet rs = con.createStatement().executeQuery(queryb);
                if (rs.next()) {
                    urlid = rs.getInt("URLID");
                }
            } catch (SQLException e) {
                System.out.println(e.getMessage());
            }
        }
        return urlid;
    }

    /**
	 * Searches the document for a text string and returns the found Node Range
	 * @param document
	 * @param text Text which has been annotated
	 * @param context Words of the context the text stands in
	 * @return
	 */
    NodeRange getRangeFromText(Document document, String text, String context) {
        ArrayList nodelist = getRangeListFromText(document, text);
        if (nodelist == null) return null;
        if (nodelist.size() == 1) {
            return (NodeRange) nodelist.get(0);
        }
        if (nodelist.size() > 1) {
            System.out.println("More than one possible range found, using context information \"" + context + "\" to find the right one.");
            String[] contextwlist = context.split(",");
            int highscore = 0;
            NodeRange highscoreRange = null;
            for (int nrcounter = 0; nrcounter < nodelist.size(); nrcounter++) {
                Vector pre = getVicinityWordList((NodeRange) nodelist.get(nrcounter), -10);
                Vector post = getVicinityWordList((NodeRange) nodelist.get(nrcounter), 10);
                pre.addAll(post);
                int matchscore = 0;
                for (int concounter = 0; concounter < contextwlist.length; concounter++) {
                    if (pre.contains(contextwlist[concounter])) matchscore++;
                }
                if (matchscore > highscore) {
                    highscoreRange = (NodeRange) nodelist.get(nrcounter);
                    highscore = matchscore;
                }
                System.out.println("Matchscore:" + matchscore);
            }
            return highscoreRange;
        }
        return null;
    }

    ArrayList getRangeListFromText(Document document, String osearchtext) {
        ArrayList nodelist = new ArrayList();
        Stack cutposstack = new Stack();
        Stack nodepos = new Stack();
        String searchtext = osearchtext.replaceAll("\\s", "");
        int searchlen = searchtext.length();
        DocumentTraversal travers = (DocumentTraversal) document;
        NodeIterator iterator = travers.createNodeIterator(document, NodeFilter.SHOW_TEXT, null, true);
        Node node;
        String rawtext = "";
        int oldpos = 0;
        int sumpos = 0;
        Node firstnode = null;
        Node lastnode = null;
        while ((node = iterator.nextNode()) != null) {
            String cntext = node.getNodeValue().replaceAll("\\s", "");
            sumpos = sumpos + cntext.length();
            nodepos.push(new Integer(sumpos));
            rawtext = rawtext + cntext;
        }
        int foundpos = rawtext.lastIndexOf(searchtext);
        while (foundpos >= 0) {
            ListIterator listit = nodepos.listIterator();
            String content = "";
            while (((Integer) nodepos.peek()).intValue() > (foundpos + searchlen)) {
                nodepos.pop();
                node = iterator.previousNode();
            }
            content = node.getNodeValue().trim();
            lastnode = node;
            while (((Integer) nodepos.peek()).intValue() > (foundpos)) {
                nodepos.pop();
                node = iterator.previousNode();
                content = node.getNodeValue().trim() + content;
            }
            NodeRange nrange = new NodeRange(document, searchtext, content);
            nrange.setFirstnode(node);
            nrange.setLastnode(lastnode);
            foundpos = rawtext.lastIndexOf(searchtext, foundpos - 1);
            nodelist.add(nrange);
        }
        return nodelist;
    }

    public Node findChildNode(String tag, Node nnode) {
        if (nnode.getNodeName().equals(tag)) {
            return nnode;
        } else {
            for (int i = 0; i < nnode.getChildNodes().getLength(); i++) {
                Node tempnode = findChildNode(tag, nnode.getChildNodes().item(i));
                if (tempnode != null) {
                    return (tempnode);
                }
            }
            return null;
        }
    }

    void close() {
        try {
            con.close();
        } catch (SQLException e) {
            e.printStackTrace();
        }
    }

    /**
	 * Helps posting an annotation via browser. The Information is put into a container
	 * element and the user gets a form where he can enter the annotation body.
	 * In the form we save an index to the information he submitted now.
	 * @param url
	 * @param context
	 * @return HTML document as String
	 */
    String post_via_browser(String url, String context) {
        if (context.length() == 0) {
            return ("<html><head><title>Selection Empty</title></head><body><h2>Error - Selection received is empty</h2>" + "<p>Possible Reasons:<br><ul>" + "<li>You have forgotten to select something.</li>" + "<li>The site you are annotating is framed. Try to load the frame in a separate window.</li>" + "<li>Maybe Javascript is broken.</li>" + "</ul></p></body></html>");
        }
        org.cyberneko.html.parsers.DOMParser parser = new org.cyberneko.html.parsers.DOMParser();
        ContainerAnnotationPost conAnno = new ContainerAnnotationPost();
        conAnno.setUrl(url);
        conAnno.setContext(context);
        try {
            parser.parse("file:annotate.html");
            Document templatedoc = parser.getDocument();
            Text contexttext = templatedoc.createTextNode("\"" + context + "\"");
            Node tmpelem = templatedoc.getElementsByTagName("context_text").item(0);
            tmpelem.getParentNode().replaceChild(contexttext, tmpelem);
            DOMParser parser2 = new DOMParser();
            try {
                parser2.parse(conAnno.getUrl());
            } catch (SSLHandshakeException essl) {
                return ("<html><head><title>Secure Connection not supported</title></head><body><h2>Error - Secure connection via SSL not supported</h2>" + "<p><b>Reason:</b><br>" + "The remote secure site presented a security certificate unknown to the System." + "</p>" + "<p><b>Possible Solution:</b><br>Try accessing the content from a non-secure connection or ask your" + " system administrator to install this site's security certificate.</body></html>");
            } catch (DOMException domessl) {
                return ("<html><head><title>Document could not be parsed</title></head><body><h2>Error - Document could not be parsed</h2>" + "<p>Reason:<br>" + "Annotatio only supports HTML and well-formed XML files. Maybe your document posed as an XML file but was not well parsed." + "</p></body></html>");
            } catch (SAXException e) {
                e.printStackTrace();
                return ("Error parsing HTML Document");
            } catch (IOException e) {
                e.printStackTrace();
                return ("Error parsing HTML Document, accessing failed");
            }
            Document htmldoc = parser2.getDocument();
            ArrayList ranges = getRangeListFromText(htmldoc, conAnno.getContext());
            if (ranges == null || ranges.size() < 1) {
                return ("<html><head><title>Selection could not be found in Document</title></head><body><h2>Error - Selection could not be found</h2>" + "<p><b>Reason:</b><br>" + "The browser passed the selection to Annotatio, which looks for it in the Document. It could not be found." + "</p>" + "<p><b>Possible Solution:</b></br><ul>" + "<li>Try another bookmarklet type. The selection I got is:<br> \"" + context + "\".<br> If you " + "see special characters such as Umlaute incorrectly here, the selection was not encoded right. I expect UTF-8" + " encoding. - Try installing the Netscape Bookmarklet or simply change the text in the bookmarklet from escape to encodeURL.</li>" + "<li>The document is outdated. Maybe the document has changed on the server between loading in your" + " browser and Annotation loading it again.</li></body></html>");
            } else if (ranges.size() > 10) {
                return ("<html><body>There were " + ranges.size() + " ranges found.<br>I can only display up to ten. Please" + " consider selecting a wider range of text to annotate!</body><html>");
            } else if (ranges.size() > 1) {
                Element select_para = templatedoc.getElementById("context_information");
                if (select_para == null) return ("Error while parsing template HTML file. select_para is missing");
                select_para.appendChild(templatedoc.createTextNode("There were " + ranges.size() + " ranges found."));
                select_para.appendChild(templatedoc.createElement("br"));
                select_para.appendChild(templatedoc.createTextNode("Please chose the correct one!"));
                select_para.appendChild(templatedoc.createElement("br"));
                for (int selectcounter = ranges.size(); selectcounter > 0; selectcounter--) {
                    Element inputelement = templatedoc.createElement("input");
                    inputelement.setAttribute("type", "radio");
                    inputelement.setAttribute("name", "context_selection");
                    inputelement.setAttribute("value", Integer.toString(selectcounter - 1));
                    NodeRange nr = (NodeRange) ranges.get(selectcounter - 1);
                    Element para = templatedoc.createElement("p");
                    select_para.appendChild(para);
                    inputelement.appendChild(templatedoc.createTextNode("..." + getVicinityText(nr, -80)));
                    Element spanelement = templatedoc.createElement("span");
                    spanelement.setAttribute("style", "color:red");
                    spanelement.appendChild(templatedoc.createTextNode(" " + context + " "));
                    inputelement.appendChild(spanelement);
                    inputelement.appendChild(templatedoc.createTextNode(getVicinityText(nr, +80) + "..."));
                    para.appendChild(inputelement);
                }
            }
            conAnno.setContentRangeList(ranges);
            containerAnnotation.add(conAnno);
            Integer positionAdd = new Integer(containerAnnotation.lastIndexOf(conAnno));
            Element hiddenindex = templatedoc.createElement("input");
            hiddenindex.setAttribute("type", "hidden");
            hiddenindex.setAttribute("name", "container_index");
            hiddenindex.setAttribute("value", positionAdd.toString());
            templatedoc.getElementById("mainform").appendChild(hiddenindex);
            StringWriter resp = new StringWriter();
            xformer.transform(new DOMSource(templatedoc), new StreamResult(resp));
            return resp.toString();
        } catch (SAXException e) {
            e.printStackTrace();
            return ("<html><body>Error while parsing template file annotate.html</body></html>");
        } catch (IOException e) {
            e.printStackTrace();
            return ("<html><body>Error while accessing template file annotate.html.<br>Is it existing and accessible?</body></html>");
        } catch (TransformerException e) {
            e.printStackTrace();
            return ("<html><body>Error while parsing template file annotate.html</body></html>");
        }
    }

    /**
	 * Post via browser the second step
	 * @param formdata
	 * @return
	 */
    String post_via_browser2(String formdata) {
        String annotation_body = "";
        String anno_type = "Comment";
        int container_index = -1;
        int context_selection = 0;
        Document annobody_d = null;
        String[] variables = formdata.split("\\&|\\s");
        for (int i = 0; i < variables.length; i++) {
            if (variables[i].lastIndexOf("=") >= 0) {
                String[] pai = variables[i].split("=");
                if (pai[0].matches("container_index")) {
                    container_index = Integer.parseInt(pai[1]);
                } else if (pai[0].matches("anno_type")) {
                    anno_type = pai[1];
                } else if (pai[0].matches("annotation_body")) {
                    if (pai.length > 1) annotation_body = pai[1];
                    try {
                        annotation_body = URLDecoder.decode(annotation_body, "UTF-8");
                        DOMParser dparser = new DOMParser();
                        StringReader stread = new StringReader("<html><body>" + annotation_body + "<body></html>");
                        InputSource inps = new InputSource(stread);
                        dparser.parse(inps);
                        annobody_d = dparser.getDocument();
                    } catch (UnsupportedEncodingException e3) {
                        e3.printStackTrace();
                        return ("<html><body>Annotatio Error:<br>The contents of the form where not understood. Make sure your browser" + "supports encoding in UTF-8.</body><html>");
                    } catch (SAXException e) {
                        System.out.println("Error while parsing the body of the annotation. Maybe the entered HTML markup was incorrect.");
                        e.printStackTrace();
                    } catch (IOException e) {
                        e.printStackTrace();
                    }
                } else if (pai[0].matches("context_selection")) {
                    context_selection = Integer.parseInt(pai[1]);
                }
            }
        }
        ContainerAnnotationPost conAnno;
        try {
            conAnno = (ContainerAnnotationPost) containerAnnotation.get(container_index);
        } catch (IndexOutOfBoundsException ie) {
            return ("Data is outdated. Please reselect.");
        }
        ArrayList ranges = conAnno.getContentRangeList();
        if (context_selection < 0 && context_selection > ranges.size() - 1) {
            return ("Error passing context_selection.");
        }
        NodeRange nr = (NodeRange) ranges.get(context_selection);
        Document htmldoc = nr.getDocument();
        XPointer xptr = new XPointer(htmldoc);
        String absXPath = "";
        try {
            absXPath = xptr.createXPath(nr.getFirstnode().getParentNode());
        } catch (XPointerRangeException e1) {
            e1.printStackTrace();
        }
        String xptrstring = "";
        if (nr.getFirstnode() == nr.getLastnode()) {
            xptrstring = (conAnno.getUrl() + "#xpointer(string-range(" + absXPath.toLowerCase() + ", \"\", " + nr.getBegin() + ", " + nr.getLength() + "))");
        } else {
            String absEndPath = "";
            try {
                absEndPath = xptr.createXPath(nr.getLastnode().getParentNode());
            } catch (XPointerRangeException e2) {
                e2.printStackTrace();
            }
            xptrstring = (conAnno.getUrl() + "#xpointer(start-point(string-range(" + absXPath.toLowerCase() + ", \"\" ," + nr.getBegin() + ",1))/range-to(end-point(string-range(" + absEndPath.toLowerCase() + ", \"\"," + (nr.getEnd() - 1) + ", 1))))");
        }
        System.out.println(xptrstring);
        Document annoDoc = getBuilder().newDocument();
        Element rootelem = annoDoc.createElementNS(rdfNS, "r:RDF");
        rootelem.setAttribute("xmlns:r", rdfNS);
        rootelem.setAttribute("xmlns:a", annoNS);
        rootelem.setAttribute("xmlns:d", dubNS);
        rootelem.setAttribute("xmlns:t", threadNS);
        rootelem.setAttribute("xmlns:h", htmlNS);
        annoDoc.appendChild(rootelem);
        Element descrElem = annoDoc.createElementNS(rdfNS, "r:Description");
        rootelem.appendChild(descrElem);
        Element type1Elem = annoDoc.createElementNS(rdfNS, "r:type");
        type1Elem.setAttributeNS(rdfNS, "r:resource", "http://www.w3.org/2000/10/annotation-ns#Annotation");
        Element type2Elem = annoDoc.createElementNS(rdfNS, "r:type");
        type2Elem.setAttributeNS(rdfNS, "r:resource", "http://www.w3.org/2000/10/annotationType#" + anno_type);
        Element annotatesElem = annoDoc.createElementNS(annoNS, "a:annotates");
        annotatesElem.setAttributeNS(rdfNS, "r:resource", conAnno.getUrl());
        descrElem.appendChild(type1Elem);
        descrElem.appendChild(type2Elem);
        descrElem.appendChild(annotatesElem);
        Element creator = annoDoc.createElementNS(dubNS, "d:creator");
        creator.appendChild(annoDoc.createTextNode(getRealname()));
        Element created = annoDoc.createElementNS(annoNS, "a:created");
        String dateString = ISO8601DateParser.toString(new Date());
        created.appendChild(annoDoc.createTextNode(dateString));
        Element dateElem = annoDoc.createElementNS(dubNS, "d:date");
        dateElem.appendChild(annoDoc.createTextNode(dateString));
        descrElem.appendChild(creator);
        descrElem.appendChild(created);
        descrElem.appendChild(dateElem);
        Element context = annoDoc.createElementNS(annoNS, "a:context");
        context.appendChild(annoDoc.createTextNode(xptrstring));
        descrElem.appendChild(context);
        Element context_elem = annoDoc.createElementNS(alNS, "al:context-element");
        context_elem.setAttributeNS(alNS, "al:text", conAnno.getContext());
        context_elem.appendChild(annoDoc.createTextNode(generateContextString(nr)));
        context.appendChild(context_elem);
        context_elem.setAttribute("xmlns:al", alNS);
        Element language = annoDoc.createElementNS(dubNS, "d:language");
        language.appendChild(annoDoc.createTextNode("de"));
        descrElem.appendChild(language);
        Element annobody = annoDoc.createElementNS(annoNS, "a:body");
        descrElem.appendChild(annobody);
        Element message = annoDoc.createElementNS(htmlNS, "h:message");
        annobody.appendChild(message);
        Element contentType = annoDoc.createElementNS(htmlNS, "h:ContentType");
        contentType.appendChild(annoDoc.createTextNode("text/html"));
        message.appendChild(contentType);
        Element hbody = annoDoc.createElementNS(htmlNS, "h:Body");
        hbody.setAttributeNS(rdfNS, "r:parseType", "Literal");
        message.appendChild(hbody);
        Element html = (Element) annoDoc.importNode(annobody_d.getDocumentElement(), true);
        html.setAttribute("xmlns", "http://www.w3.org/1999/xhtml");
        hbody.appendChild(html);
        if (localConnection()) {
            PostParser pp = new PostParser(annoDoc, this, htmldoc);
            String location = pp.getLocation();
        } else {
            publishToRemoteServer(annoDoc);
        }
        return ("<html><body onLoad=\"javascript:window.close();\">Annotation sucessfully posted</body></html>");
    }

    String getVicinityText(NodeRange nr, int parameter) {
        if (parameter == 0) return "";
        boolean wholedoc = (Math.abs(parameter) == 1) ? true : false;
        if (parameter > 0) {
            Node currnode = nr.getLastnode();
            String text = "";
            if (nr.getLastnode().getNodeType() == Node.TEXT_NODE) {
                text = nr.getLastnode().getNodeValue().trim();
                text = text.replaceAll("\\s{2,}", " ");
                text = text.replaceAll("\\s", " ");
                text = text.substring(nr.getEnd() - 1);
            } else {
                System.out.println("I thought I only get Text Nodes.(getVicinityText)");
            }
            currnode = getNextNode(currnode);
            while (currnode != null && (text.length() < parameter || wholedoc)) {
                if (currnode.getNodeType() == Node.TEXT_NODE) {
                    text = text + " " + currnode.getNodeValue().trim().replaceAll("\\s{2,}", " ").replaceAll("\\s", " ");
                }
                currnode = getNextNode(currnode);
            }
            return wholedoc ? text : text.substring(0, parameter);
        }
        if (parameter < 0) {
            Node currnode = nr.getFirstnode();
            String text = "";
            if (nr.getFirstnode().getNodeType() == Node.TEXT_NODE) {
                if (nr.getBegin() > 0) {
                    text = nr.getFirstnode().getNodeValue().trim();
                    text = text.replaceAll("\\s{2,}", " ");
                    text = text.replaceAll("\\s", " ");
                    text = text.substring(0, nr.getBegin() - 1);
                }
            } else {
                System.out.println("I thought I only get Text Nodes.(getVicinityText)");
            }
            currnode = getPriorNode(currnode);
            while (currnode != null && (text.length() < Math.abs(parameter) || wholedoc)) {
                if (currnode.getNodeType() == Node.TEXT_NODE) {
                    text = currnode.getNodeValue().trim().replaceAll("\\s{2,}", " ").replaceAll("\\s", " ") + " " + text;
                }
                currnode = getPriorNode(currnode);
            }
            if (wholedoc) return text;
            return (text.length() <= 100) ? text : text.substring(text.length() - 100);
        }
        return "";
    }

    Vector getVicinityWordList(NodeRange nr, int parameter) {
        Vector result = new Vector();
        if (parameter == 0) return null;
        if (parameter > 0) {
            Node currnode = nr.getLastnode();
            String text = "";
            if (nr.getLastnode().getNodeType() == Node.TEXT_NODE) {
                text = nr.getLastnode().getNodeValue();
                text = text.replaceAll("^\\s+", "");
                text = text.replaceAll("\\s{2,}", " ");
                text = text.replaceAll("\\s", " ");
                text = text.substring(nr.getEnd() - 1);
            } else {
                System.out.println("I thought I only get Text Nodes.(getVicinityText)");
            }
            text = text.replaceAll("\\p{Punct}", " ");
            text = text.replaceAll("\\s{2,}", " ");
            String[] tmp = text.split("\\s");
            for (int i = 0; i < tmp.length; i++) result.add(tmp[i]);
            currnode = getNextNode(currnode);
            while (currnode != null && (result.size() < parameter)) {
                if (currnode.getNodeType() == Node.TEXT_NODE) {
                    text = currnode.getNodeValue();
                    text = text.replaceAll("\\p{Punct}", " ");
                    text = text.replaceAll("\\s{2,}", " ");
                    tmp = text.split("\\s");
                    for (int i = 0; i < tmp.length; i++) result.add(tmp[i]);
                }
                currnode = getNextNode(currnode);
            }
            result.setSize(parameter);
            result.trimToSize();
            return result;
        }
        if (parameter < 0) {
            Node currnode = nr.getFirstnode();
            String text = "";
            if (nr.getFirstnode().getNodeType() == Node.TEXT_NODE) {
                if (nr.getBegin() > 0) {
                    text = nr.getFirstnode().getNodeValue().trim();
                    text = text.replaceAll("\\s{2,}", " ");
                    text = text.replaceAll("\\s", " ");
                    text = text.substring(0, nr.getBegin() - 1);
                }
            } else {
                System.out.println("I thought I only get Text Nodes.(getVicinityText)");
            }
            text = text.replaceAll("\\p{Punct}", " ");
            text = text.replaceAll("\\s{2,}", " ");
            String[] tmp = text.split("\\s");
            for (int i = tmp.length; i > 0; i--) result.add(tmp[i - 1]);
            currnode = getPriorNode(currnode);
            while (currnode != null && (result.size() < Math.abs(parameter))) {
                if (currnode.getNodeType() == Node.TEXT_NODE) {
                    text = currnode.getNodeValue();
                    text = text.replaceAll("\\p{Punct}", " ");
                    text = text.replaceAll("\\s{2,}", " ");
                    tmp = text.split("\\s");
                    for (int i = tmp.length; i > 0; i--) result.add(tmp[i - 1]);
                }
                currnode = getPriorNode(currnode);
            }
            result.setSize(Math.abs(parameter));
            result.trimToSize();
            return result;
        }
        return null;
    }

    /**
	 * Generates a string which identifies the context in which our found text stands in
	 * @param nr NodeRange for our context
	 * @return
	 */
    String generateContextString(NodeRange nr) {
        Vector prelist = getVicinityWordList(nr, 10);
        Vector postlist = getVicinityWordList(nr, -10);
        prelist.addAll(postlist);
        Random randomizer = new Random();
        String contwl = "";
        int count = 0;
        while (count < 7 && prelist.size() > 1) {
            int nextitem = randomizer.nextInt(prelist.size() - 1);
            String nextword = (String) prelist.get(nextitem);
            if (nextword != null && nextword.length() > 4) {
                contwl = contwl + (count > 0 ? "," : "") + nextword;
                count++;
            }
            prelist.remove(nextitem);
        }
        return contwl;
    }

    Node getNextNode(Node node) {
        if (node.hasChildNodes()) {
            return (node.getFirstChild());
        } else {
            Node tmp = node.getNextSibling();
            Node tmp2 = node;
            while (tmp == null) {
                if (tmp2.getParentNode() == null) return null;
                tmp = tmp2.getParentNode().getNextSibling();
                tmp2 = tmp2.getParentNode();
            }
            return (tmp);
        }
    }

    Node getPriorNode(Node node) {
        Node tmp = node.getPreviousSibling();
        if (tmp == null) return node.getParentNode();
        while (tmp.hasChildNodes()) {
            tmp = tmp.getLastChild();
        }
        return tmp;
    }

    public boolean createAnnoResource(Element resourceDoc, String hash) {
        File file = new File(this.file_storage_path + "/annotation/" + hash);
        try {
            Transformer transformer = TransformerFactory.newInstance().newTransformer();
            transformer.setOutputProperty(OutputKeys.ENCODING, "UTF-8");
            transformer.setOutputProperty(OutputKeys.STANDALONE, "no");
            transformer.setOutputProperty(OutputKeys.INDENT, "yes");
            transformer.transform(new DOMSource(resourceDoc), new StreamResult(file));
        } catch (TransformerException e) {
            System.out.println(e.getLocalizedMessage());
            System.out.println("Maybe you should create an annotation directory.");
            return false;
        }
        return true;
    }

    public boolean createAnnoBody(Node annoBodyDoc, String hash) {
        File file = new File(this.file_storage_path + "/annotation/body/" + hash);
        try {
            Transformer transformer = TransformerFactory.newInstance().newTransformer();
            transformer.setOutputProperty(OutputKeys.METHOD, "html");
            transformer.transform(new DOMSource(annoBodyDoc), new StreamResult(file));
            return true;
        } catch (TransformerConfigurationException e1) {
            e1.printStackTrace();
            return false;
        } catch (TransformerFactoryConfigurationError e1) {
            e1.printStackTrace();
            return false;
        } catch (TransformerException e) {
            System.out.println(e.getLocalizedMessage());
            System.out.println("Maybe you should create an annotation/body directory.");
            return false;
        }
    }

    /**
	 * @return Returns the db_storage_path.
	 */
    public String getDb_storage_path() {
        return db_storage_path;
    }

    /**
	 * @return Returns the user_db.
	 */
    private Connection getUser_db() {
        return user_db;
    }

    /**
	 * Returns a permission Manager object. This will object will be needed to gain access to resources later.
	 * The client needs to save this object. Web client will have to store it in their session.
	 * @return Returns the perm_man.
	 */
    public PermissionManager getPerm_man(String principal) {
        return (new PermissionManager(user_db, principal));
    }

    /**
	 * @return Returns the realname.
	 */
    public String getRealname() {
        return (realname == null) ? "Unknown Author" : this.realname;
    }

    /**
	 * @return Returns the file_storage_path.
	 */
    public String getFile_storage_path() {
        return file_storage_path;
    }
}

class NodeRange {

    private Document document;

    private String searchtext;

    private String content;

    private int begin = -1;

    private int length = 0;

    private int end = -1;

    private Node firstnode;

    private Node lastnode;

    /**
	 * @param doc XML Document for NodeRange
	 * @param st String containing the text that was looked for
	 * @param content String containing the text that was identified
	 */
    NodeRange(Document doc, String st, String content) {
        this.document = doc;
        this.searchtext = st;
        this.content = content;
    }

    NodeRange(Document doc, String content, Node FirstNode, Node LastNode, int begin, int length, int end) {
        this.document = doc;
        this.content = content;
        this.searchtext = content.replaceAll("\\s", "");
        this.firstnode = FirstNode;
        this.lastnode = LastNode;
        this.begin = begin;
        this.length = length;
        this.end = end;
    }

    String getSearchText() {
        return this.searchtext;
    }

    int getBegin() {
        if (begin == -1) {
            computeBeginLength();
        }
        return begin;
    }

    String getContentString() {
        return this.content;
    }

    /**
	 * @return Returns the firstnode.
	 */
    public Node getFirstnode() {
        return firstnode;
    }

    /**
	 * @param firstnode The firstnode to set.
	 */
    public void setFirstnode(Node firstnode) {
        this.firstnode = firstnode;
    }

    /**
	 * @return Returns the lastnode.
	 */
    public Node getLastnode() {
        return lastnode;
    }

    /**
	 * @param lastnode The lastnode to set.
	 */
    public void setLastnode(Node lastnode) {
        this.lastnode = lastnode;
    }

    int getLength() {
        if (begin == -1) {
            computeBeginLength();
        }
        return length;
    }

    /**
	 * Computes the begin (start of the string in the first node)
	 * end (end of the string in the last node)
	 * length (total spanned chars)
	 */
    private void computeBeginLength() {
        String contentstring2 = content.replaceAll("\\s", "");
        int spos = contentstring2.indexOf(searchtext);
        content = content.replaceAll("\\s", " ");
        content = content.replaceAll("\\s{2,}", " ");
        for (int p = spos; p < content.length(); p++) {
            int q = 0;
            int s = 0;
            if (content.charAt(p) == searchtext.charAt(q)) {
                boolean weiter = true;
                while (weiter) {
                    if (q + 1 >= searchtext.length()) {
                        this.begin = p + 1;
                        this.length = s + 1;
                        return;
                    } else if (content.charAt(p + s + 1) == ' ') {
                        s++;
                    } else if (content.charAt(p + s + 1) == searchtext.charAt(q + 1)) {
                        q++;
                        s++;
                    } else weiter = false;
                }
            }
        }
    }

    /**
	 * @return Returns the end.
	 */
    public int getEnd() {
        if (begin == -1) {
            computeBeginLength();
        }
        int decounter = this.getBegin() + this.getLength();
        Node node = this.getFirstnode();
        while (node != this.getLastnode()) {
            if (node.getNodeType() == Node.TEXT_NODE) {
                String nodetext = node.getNodeValue();
                nodetext = nodetext.trim();
                nodetext = nodetext.replaceAll("\\s", " ");
                nodetext = nodetext.replaceAll("\\s{2,}", " ");
                decounter = decounter - nodetext.length();
            }
            node = getNextNode(node);
        }
        return decounter;
    }

    private Node getNextNode(Node node) {
        if (node.hasChildNodes()) {
            return (node.getFirstChild());
        } else {
            Node tmp = node.getNextSibling();
            Node tmp2 = node;
            while (tmp == null) {
                tmp = tmp2.getParentNode().getNextSibling();
                tmp2 = tmp2.getParentNode();
            }
            return (tmp);
        }
    }

    /**
	 * @return Returns the document.
	 */
    public Document getDocument() {
        return document;
    }
}
