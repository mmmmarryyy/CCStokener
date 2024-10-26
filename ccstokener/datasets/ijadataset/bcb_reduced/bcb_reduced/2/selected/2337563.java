package com.extentech.ExtenXLS.web;

import com.extentech.ExtenXLS.*;
import org.docsfree.legacy.auth.*;
import com.extentech.swingtools.ProgressDialog;
import com.extentech.toolkit.Logger;
import com.extentech.toolkit.StringTool;
import java.util.List;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.zip.GZIPInputStream;
import java.io.BufferedReader;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileWriter;
import java.io.InputStreamReader;
import java.io.StringReader;
import java.net.*;
import java.io.ByteArrayOutputStream;
import java.io.ByteArrayInputStream;
import java.io.InputStream;
import java.io.BufferedInputStream;
import java.sql.Connection;
import java.sql.SQLException;
import org.custommonkey.xmlunit.DetailedDiff;
import org.custommonkey.xmlunit.Diff;
import org.custommonkey.xmlunit.Difference;
import org.custommonkey.xmlunit.DifferenceConstants;
import org.custommonkey.xmlunit.NodeDetail;
import org.custommonkey.xmlunit.XMLUnit;
import org.jaxen.JaxenException;
import org.jaxen.UnresolvableException;
import org.jaxen.XPath;
import org.jaxen.dom.DOMXPath;
import org.jaxen.jdom.JDOMXPath;
import org.jaxen.SimpleNamespaceContext;
import org.jdom.Document;
import org.jdom.Element;
import org.jdom.Attribute;
import org.jdom.Namespace;
import org.jdom.Text;
import org.jdom.input.SAXBuilder;
import org.jdom.output.DOMOutputter;
import org.jdom.output.Format;
import org.jdom.output.XMLOutputter;
import org.w3c.dom.NodeList;
import org.w3c.dom.Node;
import java.io.IOException;

/**
 * The WebWorkBook class provides additional functionality to the WorkBookHandle
 * class to allow it to participate in a Service-Oriented-Architecture.
 * 
 * The additional functionality of the WebWorkBook relates to version and access
 * control using a hosted architecture to provide for the security and
 * versioning repository.
 * 
 * @author John McMahon -- Copyright &copy;2010 <a href =
 *         "http://www.extentech.com">Extentech Inc.</a>
 * @see com.extentech.ExtenXLS.WorkBook
 * @see WorkBookHandle
 * @see http://www.sheetster.com
 * 
 * 
 **/
public class WebWorkBook extends MemeWorkBook implements com.extentech.ExtenXLS.WorkBook {

    private String session;

    /**
	 * constructor for file comparisons - testing purposes
	 * 
	 * @param fCurrent
	 *            original XML filename
	 * @param fLatest
	 *            updated XML filename
	 */
    public WebWorkBook(String fCurrent, String fLatest, String outputDir, boolean bDEBUG) {
        WEB = false;
        DEBUG = bDEBUG;
        latestFile = fLatest;
        currentFile = fCurrent;
        if (outputDir == null) this.outputDir = StringTool.splitFilepath(latestFile)[0]; else this.outputDir = outputDir;
        diffFile = outputDir + StringTool.stripPath(latestFile.substring(0, latestFile.lastIndexOf(".")) + ".diff");
    }

    /**
	 * Constructor that loads a spreadsheet from a file path.
	 * 
	 * @param path
	 */
    public WebWorkBook(String path) {
        myBook = new WorkBookHandle(path);
    }

    /**
	 * constructor with a meme_id
	 * 
	 * @return
	 */
    public WebWorkBook(Connection cx, int mix, boolean storeAsFile, boolean xmlStorage) {
        super(cx, mix, storeAsFile, xmlStorage);
        this.setFormulaCalculationMode(WorkBookHandle.CALCULATE_ALWAYS);
    }

    /**
	 * constructor that loads workbook from a URL
	 * 
	 * @return
	 * @deprecated - use FileStream constructor Move user handling to more
	 *             logical spot...
	 */
    public WebWorkBook(URL urlx, User user) {
        myBook = new WorkBookHandle(urlx);
        if (user.isValid()) {
            this.setOwnerId(user.getId());
            this.addUser(user);
        }
        this.setFormulaCalculationMode(WorkBookHandle.CALCULATE_ALWAYS);
    }

    /**
	 * constructor that loads workbook from an InputStream
	 * 
	 * @return
	 */
    public WebWorkBook(InputStream urlx, User user) {
        myBook = new WorkBookHandle(urlx);
        if (user.isValid()) {
            this.setOwnerId(user.getId());
            this.addUser(user);
        }
        this.setFormulaCalculationMode(WorkBookHandle.CALCULATE_ALWAYS);
    }

    /**
	 * Create a new WebWorkBook with the passed in user.
	 * 
	 * The user param will be the owner of the new workbook
	 */
    public WebWorkBook(User user) {
        this();
        if (!user.isValid()) throw new WorkBookException("WebWorkBook: Null User", WorkBookException.RUNTIME_ERROR);
        this.setOwnerId(user.getId());
        this.addUser(user);
        this.setFormulaCalculationMode(WorkBookHandle.CALCULATE_ALWAYS);
    }

    /**
	 * constructor retrieves workbook referenced by midi
	 * 
	 * @return
	 */
    public WebWorkBook(int midi, User user) {
        this();
        memeId = midi;
        this.addUser(user);
        this.init();
    }

    /**
	 * Create a new WebWorkbook from a workbook passed in.
	 * 
	 * 
	 * @author Administrator [ May 2, 2007 ]
	 * @param bk
	 * @param user
	 * @throws SQLException
	 */
    public WebWorkBook(Connection dbc, byte[] in, User user, String description) throws SQLException {
        super(dbc, in);
        addUser(user);
        if (description == null) description = "New WorkBook";
        setFormulaCalculationMode(WorkBookHandle.CALCULATE_ALWAYS);
        setScript(description);
        setOwnerId(user.getId());
        setName(description);
        storeNewDocument();
        user.initOwnedACL();
    }

    public WebWorkBook(String finpath, ProgressDialog progdialog) {
        myBook = new WorkBookHandle(finpath, progdialog);
        this.setFormulaCalculationMode(WorkBookHandle.CALCULATE_ALWAYS);
    }

    public WebWorkBook() {
        myBook = new WorkBookHandle();
        this.setFormulaCalculationMode(WorkBookHandle.CALCULATE_ALWAYS);
    }

    public WebWorkBook(Connection dbcon, File fx) {
        super(dbcon, fx);
    }

    public WebWorkBook(Connection dbcon, URL u) {
        super.connection = dbcon;
        myBook = new WorkBookHandle(u);
    }

    /**
	 * Constructor which creates a spreadsheet from XLS or XLSX bytes
	 * 
	 * @param xls or xlsx file bytes
	 */
    public WebWorkBook(byte[] b) {
        super(b);
    }

    /**
	 * 
	 * 
	 * @author John McMahon [ Aug 28, 2008 ]
	 */
    public void init() {
        this.setFormulaCalculationMode(WorkBookHandle.CALCULATE_ALWAYS);
        try {
            if (memeId < 0) {
            } else {
                conurl = new URL(ServerURL + "?meme_id=" + memeId);
                java.io.InputStream xmlstr = conurl.openStream();
                this.removeAllWorkSheets();
                this.setFormulaCalculationMode(WorkBookHandle.CALCULATE_EXPLICIT);
                this.setStringEncodingMode(WorkBookHandle.STRING_ENCODING_UNICODE);
                this.setDupeStringMode(WorkBookHandle.SHAREDUPES);
                ExtenXLS.parseNBind(this, xmlstr);
                this.setFormulaCalculationMode(WorkBookHandle.CALCULATE_ALWAYS);
            }
        } catch (Exception ex) {
            throw new WorkBookException("Error while connecting to: " + ServerURL + ":" + ex.toString(), WorkBookException.RUNTIME_ERROR);
        }
    }

    /**
	 * commit changes from the existing workbook as compared to the stored
	 * version retrieve changes as diff and store
	 * 
	 * @return boolean truth of "commit was successful"
	 */
    public boolean commit() {
        Document latest = null, current = null;
        if (WEB) {
            try {
                if (memeId == -1) {
                    memeId = storeNewDocument();
                    return true;
                }
                latest = getDocument(getXMLStream(memeId), "");
                current = getDocument(ExtenXLS.getXML(this), toString());
            } catch (Exception e) {
                Logger.logInfo("WebWorkBook.commit: obtaining document " + e.toString());
                return false;
            }
            if (DEBUG) {
                printDocument(latest, "c:/eclipse/workspace/ExtenXLS/testFiles/Versioning/Latest.xml");
                printDocument(current, "c:/eclipse/workspace/ExtenXLS/testFiles/Versioning/Current.xml");
            }
        } else {
            try {
                latest = getDocument(new FileInputStream(latestFile), latestFile);
                current = getDocument(new FileInputStream(currentFile), currentFile);
            } catch (Exception e) {
                Logger.logInfo("WebWorkBook.commit: could not open test files: " + e.toString());
                return false;
            }
        }
        StringBuffer diffResults = getDiff(current, latest);
        try {
            if (WEB) {
                storeDiff(memeId, diffResults);
                ExtenXLS.parseNBind(this, WebWorkBook.convertDocumentToInputStream(current));
                save(memeId);
            } else {
                FileWriter f = new FileWriter(diffFile);
                f.write(diffResults.toString());
                f.flush();
                f.close();
            }
        } catch (Exception e) {
            Logger.logInfo("WebWorkBook.commit " + e.toString());
            return false;
        }
        return true;
    }

    /**
	 * return an Iterator of diff history -- list[0] is the most current
	 * changes, applying list[0] to the current document brings it back 1
	 * version. applying list[1] after applying list[0] brings the current
	 * document back 2 versions. and so on.
	 * 
	 */
    public List getDiffs() {
        if (memeId == -1) {
            Logger.logWarn("WebWorkBook.getDiffs: memeId not set");
            return null;
        }
        try {
            return getAllDiffs(memeId);
        } catch (Exception e) {
            Logger.logInfo("WebWorkBook.getDiffs: " + e.toString());
        }
        return null;
    }

    /**
	 * apply a single diff to a Document
	 * 
	 * @param diff
	 *            StringBuffer of diffs in specific diff format
	 * @return WorkBook representing changes
	 * 
	 */
    public com.extentech.ExtenXLS.WorkBook applyDiff(StringBuffer diff) {
        if (memeId == -1) {
            Logger.logWarn("WebWorkBook.applyDiff: memeId not set");
            return this;
        }
        if (diff.length() <= 4) {
            Logger.logWarn("WebWorkBook.applyDiff: no changes specified");
            return this;
        }
        Document current = getDocument(ExtenXLS.getXML(this), toString());
        current = applyDiffToDocument(diff, current);
        try {
            ExtenXLS.parseNBind(this, WebWorkBook.convertDocumentToInputStream(current));
        } catch (Exception e) {
            Logger.logInfo("WebWorkBook.applyDiff: saving book: " + e.toString());
        }
        return this;
    }

    /**
	 * parse each difference in diffResults, applying each to the Document
	 * 
	 * @param diffResults
	 *            StringBuff of diffs in specific format of Diff ID, Value0,
	 *            XPath0, Value1, XPath1
	 * @param current
	 *            Document
	 * @return updated Document
	 * @see getDiff
	 * @see update
	 * @see applyDiff
	 */
    private Document applyDiffToDocument(StringBuffer diffResults, Document current) {
        ns = new SimpleNamespaceContext();
        setupNamespaces(ns, current.getRootElement());
        String[] eachChange = com.extentech.toolkit.StringTool.splitString(diffResults.toString(), ROWDELIM);
        for (int i = 0; i < eachChange.length; i++) {
            if (!eachChange[i].trim().equals("")) {
                try {
                    String[] s = com.extentech.toolkit.StringTool.splitString(eachChange[i], VALUEDELIM);
                    int memeId = -1;
                    String v0 = "", v1 = "", xpath0 = "", xpath1 = "";
                    for (int j = 0; j < s.length; j++) {
                        s[j] = s[j].trim();
                        switch(j) {
                            case 0:
                                memeId = Integer.parseInt(s[j]);
                                break;
                            case 1:
                                v0 = s[j];
                                break;
                            case 2:
                                xpath0 = s[j];
                                break;
                            case 3:
                                v1 = s[j];
                                break;
                            case 4:
                                xpath1 = s[j];
                                break;
                        }
                    }
                    applyDiff(current, memeId, v0, xpath0, v1, xpath1);
                } catch (Exception e) {
                    Logger.logInfo("WebWorkBook.update: " + e.toString());
                }
            }
        }
        return current;
    }

    public StringBuffer getDiff(Document oldDoc, Document newDoc) {
        StringBuffer[] changes = new StringBuffer[3];
        for (int i = 0; i < 3; i++) {
            changes[i] = new StringBuffer();
        }
        HashMap contentLookedAt = new HashMap();
        try {
            XMLUnit.setIgnoreWhitespace(true);
            Element root = oldDoc.getRootElement();
            ns = new SimpleNamespaceContext();
            setupNamespaces(ns, root);
            String xpathRoot = getQualifiedName(root) + "[1]";
            DOMOutputter outputter = new DOMOutputter();
            org.w3c.dom.Document newD = outputter.output(newDoc);
            org.w3c.dom.Document oldD = outputter.output(oldDoc);
            DetailedDiff df = new DetailedDiff(new Diff(newD, oldD));
            parseAllDiffs(df, changes, xpathRoot);
            StringBuffer retn = new StringBuffer();
            retn.append(changes[DIFFCHANGES]);
            retn.append(changes[DIFFADDITIONS]);
            retn.append(changes[DIFFREMOVALS]);
            return retn;
        } catch (Exception e) {
            Logger.logInfo("WebWorkBook.getDiff " + e.toString());
        }
        return null;
    }

    /**
	 * utility that actually parses the diff entity and fills three
	 * stringbuffers: changes, additions and removals, with code to carry out
	 * the appropriate action
	 * 
	 * @param diff
	 *            the DetailedDiff entity to parse
	 * @param changes
	 *            return stringbuffer array for changes, additions and removals
	 * @param xpath0
	 * @see getDiff, applyDiff
	 */
    void parseAllDiffs(DetailedDiff diff, StringBuffer[] changes, String xpath0) {
        List l = diff.getAllDifferences();
        for (int k = 0; k < l.size(); k++) {
            Difference d = (Difference) l.get(k);
            try {
                parseDiff(d.getId(), d.getControlNodeDetail(), d.getTestNodeDetail(), changes, xpath0);
            } catch (Exception e) {
                if (DEBUG) Logger.logInfo("WebWorkBook.parseAllDiffs: " + e.toString());
            }
        }
    }

    /**
	 * given a diff memeId and comparison nodes, write diff information to
	 * appropriate changes buffer
	 * 
	 * @param memeId
	 *            diff ID
	 * @param n0
	 *            NodeDetail0 - contains xpath + Node + value (meaning depends
	 *            upon ID)
	 * @param n1
	 *            NodeDetail1 - contains xpath + Node + value (meaning depends
	 *            upon ID)
	 * @param changes
	 *            StringBuffer[] - DIFFCHANGES, DIFFREMOVALS, DIFFADDITIONS
	 * @param xpath0
	 *            root XPath
	 */
    void parseDiff(int memeId, NodeDetail n0, NodeDetail n1, StringBuffer[] changes, String xpath0) {
        String xp0 = xpath0, xp1 = xpath0;
        int z = -1;
        if ((z = n0.getXpathLocation().substring(1).indexOf("/")) >= 0) xp0 += n0.getXpathLocation().substring(z + 1);
        if ((z = n1.getXpathLocation().substring(1).indexOf("/")) >= 0) xp1 += n1.getXpathLocation().substring(z + 1);
        switch(memeId) {
            case DifferenceConstants.CHILD_NODELIST_SEQUENCE_ID:
            case DifferenceConstants.ATTR_SEQUENCE_ID:
            case DifferenceConstants.HAS_CHILD_NODES_ID:
                break;
            case DifferenceConstants.ELEMENT_NUM_ATTRIBUTES_ID:
                org.w3c.dom.NamedNodeMap attrs0 = n0.getNode().getAttributes();
                ArrayList testAttrs = convertNodeListToList(n1.getNode().getAttributes());
                for (int m = 0; m < attrs0.getLength(); m++) {
                    String attrName = getQualifiedName((Node) attrs0.item(m));
                    boolean bFoundIt = false;
                    for (int n = 0; n < testAttrs.size() && !bFoundIt; n++) {
                        String testAttrName = ((Node) testAttrs.get(n)).getNodeName();
                        if (attrName.equals(testAttrName)) {
                            bFoundIt = true;
                            testAttrs.remove(n);
                        }
                    }
                    if (!bFoundIt) {
                        changes[DIFFREMOVALS].append(ROWDELIM + DifferenceConstants.ATTR_NAME_NOT_FOUND_ID);
                        changes[DIFFREMOVALS].append(VALUEDELIM + attrName + VALUEDELIM + xp0);
                        changes[DIFFREMOVALS].append(VALUEDELIM + " " + VALUEDELIM + xp1);
                        changes[DIFFREMOVALS].append("\r\n");
                    }
                }
                for (int m = 0; m < testAttrs.size(); m++) {
                    changes[DIFFADDITIONS].append(ROWDELIM + ATTRIBUTE_INSERT_ID);
                    changes[DIFFADDITIONS].append(VALUEDELIM + ((Node) testAttrs.get(m)).getNodeName() + VALUEDELIM + " ");
                    changes[DIFFADDITIONS].append(VALUEDELIM + ((Node) testAttrs.get(m)).getNodeValue() + VALUEDELIM + xp0);
                    changes[DIFFADDITIONS].append("\r\n");
                }
                break;
            case DifferenceConstants.CHILD_NODELIST_LENGTH_ID:
                NodeList nodes = n0.getNode().getChildNodes();
                ArrayList testNodes = convertNodeListToList(n1.getNode().getChildNodes());
                for (int m = 0; m < nodes.getLength(); m++) {
                    String nodeName = nodes.item(m).getNodeName();
                    boolean bFoundIt = false;
                    for (int n = 0; n < testNodes.size() && !bFoundIt; n++) {
                        String testNodeName = ((Node) testNodes.get(n)).getNodeName();
                        if (nodeName.equals(testNodeName)) {
                            bFoundIt = true;
                            testNodes.remove(n);
                        }
                    }
                    if (!bFoundIt) {
                        String s = ROWDELIM + ELEMENT_NAME_NOT_FOUND_ID;
                        s += VALUEDELIM + nodeName + VALUEDELIM + " ";
                        s += VALUEDELIM + " " + VALUEDELIM + xp0 + "/" + getXPath(nodes.item(m));
                        s += "\r\n";
                        changes[DIFFREMOVALS].insert(0, s);
                    }
                }
                for (int n = 0; n < testNodes.size(); n++) {
                    Node testNode = (Node) testNodes.get(n);
                    if (!(testNode.getNodeName().equals("#text"))) {
                        int nThisNode = 0;
                        if (insertedNodes.get(n0.getNode() + "/" + testNode.getNodeName()) == null) nThisNode = getnChildElements(n0.getNode(), testNode.getNodeName()); else nThisNode = ((Integer) insertedNodes.get(n0.getNode() + "/" + testNode.getNodeName())).intValue();
                        insertedNodes.put(n0.getNode() + "/" + testNode.getNodeName(), new Integer(nThisNode + 1));
                        changes[DIFFADDITIONS].append(getInsertNodeCode(testNode, xp0, nThisNode));
                    } else if (!testNode.getNodeValue().trim().equals("")) {
                        changes[DIFFCHANGES].append(ROWDELIM + DifferenceConstants.TEXT_VALUE_ID);
                        changes[DIFFCHANGES].append(VALUEDELIM + testNode.getNodeValue() + VALUEDELIM + xp0);
                        changes[DIFFCHANGES].append(VALUEDELIM + " " + VALUEDELIM + xp1);
                        changes[DIFFCHANGES].append("\r\n");
                    }
                }
                break;
            default:
                changes[DIFFCHANGES].append(ROWDELIM + memeId);
                changes[DIFFCHANGES].append(VALUEDELIM + n0.getValue() + VALUEDELIM + xp0);
                changes[DIFFCHANGES].append(VALUEDELIM + n1.getValue() + VALUEDELIM + xp1);
                changes[DIFFCHANGES].append("\r\n");
                break;
        }
    }

    /**
	 * given element e, get namespace-qualified name
	 * 
	 * @param e
	 *            element to qualify
	 * @return string namespace-qualified name
	 */
    String getQualifiedName(Element e) {
        String n = "";
        if (ns.translateNamespacePrefixToUri("pre") != null && e.getNamespacePrefix().equals("")) n = "/pre:" + e.getName(); else n = "/" + e.getQualifiedName();
        return n;
    }

    /**
	 * given Node node, get namespace-qualified name
	 * 
	 * @param node
	 *            node to qualify
	 * @return namespace-qualified name
	 */
    String getQualifiedName(Node node) {
        if (node.getNodeType() != Node.TEXT_NODE) {
            String n = node.getNodeName();
            if (ns.translateNamespacePrefixToUri("pre") != null && node.getPrefix() == null) n = "pre:" + node.getNodeName();
            return "/" + n;
        }
        return "/text()";
    }

    /**
	 * add namespaces to NamespaceContext obtained from Element root
	 * 
	 * @param ns
	 *            NamespaceContext
	 * @param root
	 *            root Element
	 */
    void setupNamespaces(SimpleNamespaceContext ns, Element root) {
        if (!root.getNamespace().getURI().equals("")) {
            ns.addNamespace("pre", root.getNamespace().getURI());
            ns.addNamespace(root.getNamespace().getPrefix(), root.getNamespace().getURI());
            List n = root.getAdditionalNamespaces();
            for (int k = 0; k < n.size(); k++) {
                ns.addNamespace(((org.jdom.Namespace) n.get(k)).getPrefix(), ((org.jdom.Namespace) n.get(k)).getURI());
            }
        }
    }

    /**
	 * Return correct XPath String for Node n
	 * 
	 * @param n
	 *            Node
	 * @return XPath string
	 */
    String getXPath(Node n) {
        String ret = getQualifiedName(n).substring(1);
        try {
            String element = "count(preceding-sibling::" + ret + ")+1";
            DOMXPath dxp = new DOMXPath(element);
            dxp.setNamespaceContext(ns);
            List sibs = dxp.selectNodes(n);
            double nc = 0;
            if (sibs != null) {
                nc = ((Double) sibs.get(0)).doubleValue();
            }
            ret += "[" + ((int) (nc)) + "]";
        } catch (UnresolvableException ue) {
            ret += "[1]";
        } catch (Exception e) {
            Logger.logWarn("Error in XPath " + e.toString());
        }
        return ret;
    }

    /**
	 * retrieve the lastest XML stream from the conurl URL and format into a
	 * Document object
	 * 
	 * @return Document object representing the XML stream from the URL
	 */
    public Document getLatestXML() {
        Document doc = null;
        try {
            if (conurl != null) doc = getDocument(conurl.openStream(), conurl.toString());
        } catch (Exception e) {
            System.err.println("ERROR: WebWorkBook.getLatestXML: Connecting to: " + ServerURL + ":" + e.toString());
        }
        return doc;
    }

    /**
	 * return a Document object from XML in string form
	 * 
	 * @param xml
	 *            String properly formatted XML
	 * @param name
	 * @return Document object representing XML
	 */
    protected Document getDocument(String xml, String name) {
        StringReader in = new StringReader(xml);
        Document doc = null;
        SAXBuilder build = new SAXBuilder();
        if (in == null) System.err.println("ERROR:  WebWorkBook.getDocument failed. Could not get XML from WorkBook: " + name);
        try {
            doc = build.build(in);
        } catch (Exception e) {
            System.err.println("ERROR: WebWorkBook.getDocument failed. Exception creating JDOM document: " + e);
        }
        return doc;
    }

    /**
	 * return a Document object from an xmlstream
	 * 
	 * @param xmlstream
	 * @param name
	 * @return Document representing XML stream
	 */
    protected Document getDocument(java.io.InputStream xmlstream, String name) {
        Document doc = null;
        SAXBuilder build = new SAXBuilder();
        if (xmlstream == null) System.err.println("ERROR:  WebWorkBook.getDocument failed. Could not get XML from WorkBook: " + name);
        try {
            BufferedReader r = new BufferedReader(new InputStreamReader(xmlstream));
            doc = build.build(r);
        } catch (Exception e) {
            System.err.println("ERROR: WebWorkBook.getDocument failed. Exception creating JDOM document: " + e);
        }
        return doc;
    }

    protected boolean applyDiff(Document doc, int memeId, String value0, String xpath0, String value1, String xpath1) throws JaxenException {
        Object obj = null;
        Element e = null;
        if (!xpath1.equals("")) {
            XPath xp = new JDOMXPath(xpath1);
            xp.setNamespaceContext(ns);
            obj = xp.selectSingleNode(doc);
        }
        try {
            switch(memeId) {
                case DifferenceConstants.ATTR_NAME_NOT_FOUND_ID:
                    if (DEBUG) Logger.logInfo("Attribute name not found:");
                    if (obj instanceof Attribute) e = ((Attribute) obj).getParent(); else e = (Element) obj;
                    if (e != null) {
                        if (DEBUG) Logger.logInfo(" Element= " + e.getQualifiedName() + "\tAttribute " + value0);
                        e.removeAttribute(value0);
                        if (DEBUG) Logger.logInfo(" Removing...  ");
                    } else {
                        System.err.println("ERROR: WebWorkBook.applyDiff: unable to remove Attribute at " + xpath1 + ":" + value0 + ". Not an Attribute. ");
                    }
                    break;
                case DifferenceConstants.ATTR_VALUE_ID:
                    if (DEBUG) Logger.logInfo("Attribute value:");
                    if (obj instanceof Attribute) {
                        Attribute a = (Attribute) obj;
                        if (DEBUG) Logger.logInfo(" Name= " + a.getQualifiedName() + "\tOriginal Value= " + a.getValue());
                        a.setValue(value1);
                        if (DEBUG) Logger.logInfo("\tChanged To= " + a.getValue() + "\tXpath=" + xpath1);
                    } else {
                        System.err.println("ERROR: WebWorkBook.applyDiff: unable to change Attribute at " + xpath1 + ":" + value1 + ". Not an Attribute.");
                    }
                    break;
                case DifferenceConstants.CDATA_VALUE_ID:
                    if (DEBUG) Logger.logInfo("Different CData Values");
                    break;
                case DifferenceConstants.COMMENT_VALUE_ID:
                    if (DEBUG) Logger.logInfo("Different Comment value");
                    break;
                case DifferenceConstants.ELEMENT_TAG_NAME_ID:
                    if (DEBUG) Logger.logInfo("Element Name:");
                    if (obj instanceof Element) {
                        e = (Element) obj;
                        if (DEBUG) Logger.logInfo(" Original Name= " + e.getQualifiedName());
                        e.setName(value1);
                        if (DEBUG) Logger.logInfo("\tChanged To= " + e.getQualifiedName() + "\tXpath=" + xpath1);
                    } else {
                        System.err.println("ERROR: WebWorkBook.applyDiff: unable to change Element Tag name at " + xpath1 + ":" + value1 + ". Not an Element.");
                    }
                    break;
                case DifferenceConstants.TEXT_VALUE_ID:
                    if (DEBUG) Logger.logInfo("Text Value:");
                    if (obj instanceof Element) {
                        e = (Element) obj;
                        if (DEBUG) Logger.logInfo(" Original Value= " + e.getText());
                        e.setText(value1);
                        if (DEBUG) Logger.logInfo("\tChanged to= " + e.getText() + "\tXpath=" + xpath1);
                    } else if (obj instanceof Text) {
                        Text t = (Text) obj;
                        if (DEBUG) Logger.logInfo(" Original Value= " + t.getText());
                        t.setText(value1);
                        if (DEBUG) Logger.logInfo("\tChanged to= " + t.getText() + "\tXpath=" + xpath1);
                    } else {
                        System.err.println("ERROR: WebWorkBook.applyDiff: unable to change Text value at " + xpath1 + ". Not an Element.");
                    }
                    break;
                case ELEMENT_NAME_NOT_FOUND_ID:
                    if (DEBUG) Logger.logInfo("Element Name not Found:");
                    if (obj instanceof Element) {
                        e = (Element) obj;
                        Element toRemove = e;
                        e = e.getParentElement();
                        int index = e.indexOf(toRemove);
                        if (index > 0) {
                            e.removeContent(index);
                            if (index < e.getContent().size()) {
                                if (e.getContent(index) instanceof Text) e.removeContent(index);
                            }
                            if (DEBUG) Logger.logInfo(" Removing " + value0 + " from " + e.getQualifiedName() + "\tXpath=" + xpath1);
                        } else {
                            if (DEBUG) Logger.logWarn(" COULD NOT FIND ELEMENT " + toRemove.getQualifiedName() + "\tXpath=" + xpath1);
                        }
                    } else if (obj instanceof Text) {
                        Text t = (Text) obj;
                        e = t.getParentElement();
                        int index = e.indexOf(t);
                        if (index >= 0) {
                            e.removeContent(index);
                            if (DEBUG) Logger.logInfo(" Removing " + value0 + " from " + e.getQualifiedName() + "\tXpath=" + xpath1);
                        } else {
                            if (DEBUG) Logger.logWarn(" COULD NOT FIND TEXT NODE " + t.getText() + "\tXpath=" + xpath1);
                        }
                    } else {
                        System.err.println("ERROR: WebWorkBook.applyDiff: unable to Remove Element at " + xpath1 + ". Not an Element.");
                    }
                    break;
                case ELEMENT_NAME_INSERT_ID:
                    if (DEBUG) Logger.logInfo("Element Name Insert:");
                    if (obj instanceof Element) {
                        e = (Element) obj;
                        if (DEBUG) Logger.logInfo(" Name= " + value1 + "\tXpath=" + xpath1);
                        String prefix = "";
                        if (value1.indexOf(":") > 0) {
                            int z = value1.indexOf(":");
                            prefix = value1.substring(0, z);
                            value1 = value1.substring(z + 1);
                        }
                        Element newChild = new Element(value1);
                        newChild.setNamespace(Namespace.getNamespace(prefix, ns.translateNamespacePrefixToUri(prefix)));
                        XPath xpLastSib = new JDOMXPath(value0);
                        xpLastSib.setNamespaceContext(ns);
                        obj = xpLastSib.selectSingleNode(doc);
                        if (obj instanceof Element) {
                            Element p = ((Element) obj).getParentElement();
                            int index = p.indexOf((Element) obj) + 1;
                            e.addContent(index, newChild);
                        } else {
                            e.addContent(newChild);
                        }
                        int i = e.indexOf(newChild);
                        if (i > 0) {
                            int nLevels = xpath1.split("/").length - 1;
                            String formattingString = "\t\t\t\t\t\t\t\t\t\t\t".substring(0, nLevels);
                            Text t = new Text("\n" + formattingString);
                            e.addContent(i, t);
                        }
                    } else System.err.println("ERROR: WebWorkBook.applyDiff: unable to insert Element at " + xpath1 + ". Not an Element.");
                    break;
                case ATTRIBUTE_INSERT_ID:
                    if (DEBUG) Logger.logInfo("Attribute Name Insert:");
                    String attrName = value0;
                    if (obj instanceof Element) {
                        e = (Element) obj;
                        if (DEBUG) Logger.logInfo(" Name= " + attrName + "\tValue=" + value1 + "\tXpath=" + xpath1);
                        String prefix = "";
                        if (attrName.indexOf(":") > 0) {
                            int z = attrName.indexOf(":");
                            prefix = attrName.substring(0, z);
                            attrName = attrName.substring(z + 1);
                        }
                        Attribute a = new Attribute(attrName, value1);
                        a.setNamespace(Namespace.getNamespace(prefix, ns.translateNamespacePrefixToUri(prefix)));
                        e.setAttribute(a);
                    } else {
                        System.err.println("ERROR: WebWorkBook.applyDiff: unable to insert Element Attribute at " + xpath1 + ". Not an Element.");
                    }
                    break;
                default:
                    System.err.println("ERROR: WebWorkBook.applyDiff:  Unknown memeId: " + memeId);
                    break;
            }
            return true;
        } catch (Exception ee) {
            Logger.logInfo("WebWorkBook.applyDiff ID= " + memeId + ": " + ee.toString());
        }
        return false;
    }

    /**
	 * Utility to convert a NodeList to an ArrayList
	 * 
	 * @param nodes
	 * @return
	 */
    protected ArrayList convertNodeListToList(NodeList nodes) {
        ArrayList l = new ArrayList();
        for (int i = 0; i < nodes.getLength(); i++) {
            l.add(nodes.item(i));
        }
        return l;
    }

    /**
	 * Utility to convert a NamedNodeMap to an ArrayList
	 * 
	 * @param nodes
	 * @return
	 */
    protected ArrayList convertNodeListToList(org.w3c.dom.NamedNodeMap nodes) {
        ArrayList l = new ArrayList();
        for (int i = 0; i < nodes.getLength(); i++) {
            l.add(nodes.item(i));
        }
        return l;
    }

    int getnChildElements(Node n, String element) {
        int nc = 0;
        if (n.hasChildNodes()) {
            NodeList l = n.getChildNodes();
            for (int i = 0; i < l.getLength(); i++) {
                if (l.item(i).getNodeName().equals(element)) nc++;
            }
        }
        return nc;
    }

    protected String getParentXpath(String xp) {
        return xp.substring(0, xp.lastIndexOf("/"));
    }

    /**
	 * given xpath and node, traverse hierachy to create code string to insert
	 * all child nodes and attributes associated with the node; called
	 * recursively
	 * 
	 * @param n
	 *            node to insert
	 * @param xPath
	 *            original xpath
	 * @param nSiblings
	 *            number of siblings of node (for xpath creation)
	 * @return string of insertion code
	 */
    protected String getInsertNodeCode(Node n, String xPath, int nSiblings) {
        StringBuffer changes = new StringBuffer();
        String insertName = getQualifiedName(n).substring(1);
        changes.append(ROWDELIM + ELEMENT_NAME_INSERT_ID);
        changes.append(VALUEDELIM + xPath + "/" + insertName + "[" + (nSiblings) + "]" + VALUEDELIM + " ");
        changes.append(VALUEDELIM + insertName + VALUEDELIM + xPath);
        changes.append("\r\n");
        xPath += "/" + insertName + "[" + (nSiblings + 1) + "]";
        if (n.getNodeValue() != null) {
            changes.append(ROWDELIM + DifferenceConstants.TEXT_VALUE_ID);
            changes.append(VALUEDELIM + " " + VALUEDELIM + " ");
            changes.append(VALUEDELIM + n.getNodeValue() + VALUEDELIM + xPath + "/text()");
        }
        if (n.hasAttributes()) {
            for (int i = 0; i < n.getAttributes().getLength(); i++) {
                changes.append(ROWDELIM + ATTRIBUTE_INSERT_ID);
                changes.append(VALUEDELIM + n.getAttributes().item(i).getNodeName() + VALUEDELIM + " ");
                changes.append(VALUEDELIM + n.getAttributes().item(i).getNodeValue() + VALUEDELIM + xPath);
                changes.append("\r\n");
            }
        }
        if (n.hasChildNodes()) {
            Node n1 = n.getFirstChild();
            HashMap insertedNodes = new HashMap();
            do {
                if (!n1.getNodeName().equals("#text")) {
                    int nc = 0;
                    if (insertedNodes.get(xPath + "/" + n1.getNodeName()) != null) {
                        nc = ((Integer) insertedNodes.get(xPath + "/" + n1.getNodeName())).intValue();
                    }
                    insertedNodes.put(xPath + "/" + n1.getNodeName(), new Integer(nc + 1));
                    changes.append(getInsertNodeCode(n1, xPath, nc));
                } else {
                }
            } while ((n1 = n1.getNextSibling()) != null);
        }
        return changes.toString();
    }

    /**
	 * test utilty to increase the version number (-n.XML) for file comparisons
	 * 
	 * @return
	 */
    public static String incrementVersionNumber(String origFName) {
        String s = origFName;
        try {
            int i = origFName.lastIndexOf("-");
            int j = origFName.lastIndexOf(".");
            if (i > 0) {
                String ver = origFName.substring(i + 1, j);
                int n = 0;
                try {
                    n = Integer.parseInt(ver);
                } catch (Exception e) {
                }
                s = origFName.replaceAll(ver + ".", String.valueOf(n + 1) + ".");
            } else {
                s = origFName.substring(0, j) + "-1" + origFName.substring(j);
            }
        } catch (Exception e) {
        }
        return s;
    }

    public static void printDocument(Document doc, String fName) {
        XMLOutputter outputter = new XMLOutputter();
        try {
            java.io.BufferedWriter bf = new java.io.BufferedWriter(new java.io.FileWriter(fName));
            outputter.getFormat().setLineSeparator("\r\n");
            outputter.getFormat().setTextMode(Format.TextMode.TRIM_FULL_WHITE);
            outputter.getFormat().setIndent("\t");
            outputter.output(doc, bf);
        } catch (Exception e) {
            System.err.println("ERROR: WebWorkBook.printDocument: writing output file: " + e.toString());
        }
    }

    public static InputStream convertDocumentToInputStream(Document doc) throws IOException {
        XMLOutputter outputter = new XMLOutputter();
        ByteArrayOutputStream os = new ByteArrayOutputStream();
        outputter.output(doc, os);
        return (new BufferedInputStream(new ByteArrayInputStream(os.toByteArray())));
    }

    /**
	 * revert current workbook to the most recent previous version
	 * 
	 * @return reverted Workbook or null
	 */
    public DocumentHandle revertDiff() {
        if (memeId == -1) {
            Logger.logWarn("WebWorkBook.revert: memeId not set");
            return null;
        }
        try {
            StringBuffer diff = new StringBuffer(getLatestDiff(memeId));
            applyDiff(diff);
        } catch (Exception e) {
            Logger.logWarn("WebWorkBook.revert: " + e.toString());
        }
        return this;
    }
}
