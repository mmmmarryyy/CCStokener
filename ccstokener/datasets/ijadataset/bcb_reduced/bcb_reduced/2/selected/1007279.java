package org.xaware.ide.xadev.tools.gui.packagetool;

import java.io.BufferedOutputStream;
import java.io.ByteArrayOutputStream;
import java.io.InputStream;
import java.io.StringReader;
import java.net.HttpURLConnection;
import java.net.URL;
import java.util.Arrays;
import java.util.Enumeration;
import java.util.Iterator;
import java.util.Vector;
import javax.swing.tree.DefaultMutableTreeNode;
import javax.swing.tree.MutableTreeNode;
import javax.swing.tree.TreeNode;
import org.jdom.Element;
import org.jdom.Namespace;
import org.jdom.input.SAXBuilder;
import org.xaware.shared.util.XAHttpClient;
import org.xaware.shared.util.XAwareConstants;
import org.xaware.shared.util.logging.XAwareLogger;

/**
 * Tree node used in displaying host server directory structure <br>
 * <br>
 * 
 * @author Saritha
 * @version 1.0
 */
public class XAHostFileTreeNode extends DefaultMutableTreeNode implements Comparable {

    /** Serializable unique ID */
    private static final long serialVersionUID = 1L;

    /** Name space */
    public static final Namespace ns = XAwareConstants.xaNamespace;

    /** Class level logger */
    private static final XAwareLogger logger = XAwareLogger.getXAwareLogger(XAHostFileTreeNode.class.getName());

    /** Part of URL string for publish related requests */
    public static String BIZVIEW = "publish";

    /** URI string */
    public static String bizViewString = "xaware/XAServlet?_BIZVIEW=" + BIZVIEW;

    /** Host Server url */
    public static URL hostURL;

    /** Host server name */
    public static String hostString = "";

    /** Biz file name */
    private String myFileName;

    /** Complete file path */
    private String fullPath;

    /** Flag for checking directory */
    boolean isDir;

    /** Flag for checking the given node is root node */
    boolean isRoot;

    /** Child nodes */
    Vector children;

    /** User name */
    private String uid = null;

    /** Password */
    private String pwd = null;

    /** Root path for the directory tree */
    private String rootPath = "/";

    /**
     * Creates a new XAHostFileTreeNode object.
     * 
     * @param hostString
     *            String
     * @param bizViewString
     *            String
     * @param inFile
     *            String
     * @param uid
     *            String
     * @param pwd
     *            String
     */
    public XAHostFileTreeNode(final String hostString, final String bizViewString, final String inFile, final String uid, final String pwd) {
        myFileName = inFile;
        parent = null;
        isDir = true;
        isRoot = true;
        fullPath = inFile;
        rootPath = inFile;
        this.uid = uid;
        this.pwd = pwd;
        XAHostFileTreeNode.hostString = hostString;
        XAHostFileTreeNode.bizViewString = bizViewString;
    }

    /**
     * Creates a new XAHostFileTreeNode object.
     * 
     * @param hostString
     *            String
     * @param bizViewString
     *            String
     * @param inFile
     *            String
     * @param inParent
     *            String
     * @param uid
     *            String
     * @param pwd
     *            String
     * @param rootPath
     *            String
     */
    private XAHostFileTreeNode(final String hostString, final String bizViewString, final String inFile, final XAHostFileTreeNode inParent, final String uid, final String pwd, final String rootPath) {
        super(inFile);
        XAHostFileTreeNode.hostString = hostString;
        XAHostFileTreeNode.bizViewString = bizViewString;
        parent = inParent;
        this.uid = uid;
        this.pwd = pwd;
        this.rootPath = rootPath;
        myFileName = inFile;
        fullPath = myFileName;
        XAHostFileTreeNode curPar = (XAHostFileTreeNode) parent;
        if (curPar != null) {
            while (!curPar.isFileRoot()) {
                fullPath = curPar.toString() + "/" + fullPath;
                curPar = (XAHostFileTreeNode) curPar.getParent();
            }
        }
        if (!isRoot) {
            fullPath = rootPath + fullPath;
        }
    }

    /**
     * Inserts the node at the given index
     * 
     * @param child
     *            MutableTreeNode
     * @param index
     *            int
     */
    @Override
    public void insert(final MutableTreeNode child, final int index) {
        super.insert(child, index);
    }

    /**
     * Removes the child node at the given index
     * 
     * @param index
     *            int
     */
    @Override
    public void remove(final int index) {
        myChildren().removeElementAt(index);
    }

    /**
     * Removes the given node and its children
     * 
     * @param node
     *            MutableTreeNode
     */
    @Override
    public void remove(final MutableTreeNode node) {
        myChildren().removeElement(node);
    }

    /**
     * Dummy implementation
     */
    @Override
    public void removeFromParent() {
    }

    /**
     * Sets the given node as parent node
     * 
     * @param newParent
     *            MutableTreeNode
     */
    @Override
    public void setParent(final MutableTreeNode newParent) {
        parent = newParent;
    }

    /**
     * Sets the display object
     * 
     * @param object
     *            Object
     */
    @Override
    public void setUserObject(final Object object) {
        userObject = object;
    }

    /**
     * Returns the children of this node
     * 
     * @return Enumeration
     */
    @Override
    public Enumeration children() {
        return myChildren().elements();
    }

    /**
     * Returns whether the node is directory or not
     * 
     * @return boolean
     */
    @Override
    public boolean getAllowsChildren() {
        return isDir;
    }

    /**
     * Returns node at the given index
     * 
     * @param index
     *            int
     * 
     * @return TreeNode
     */
    @Override
    public TreeNode getChildAt(final int index) {
        return (TreeNode) myChildren().elementAt(index);
    }

    /**
     * Returns child count
     * 
     * @return int
     */
    @Override
    public int getChildCount() {
        return myChildren().size();
    }

    /**
     * Returns the index of the given node
     * 
     * @param node
     *            TreeNode
     * 
     * @return int
     */
    @Override
    public int getIndex(final TreeNode node) {
        return myChildren().indexOf(node);
    }

    /**
     * Returns parent node
     * 
     * @return TreeNode
     */
    @Override
    public TreeNode getParent() {
        return parent;
    }

    /**
     * Returns whether the node is leaf node or not
     * 
     * @return boolean
     */
    @Override
    public boolean isLeaf() {
        return !isDir;
    }

    /**
     * Returns the chidren of the root node
     * 
     * @return Vector
     */
    public synchronized Vector myChildren() {
        if (children == null) {
            final Vector tmpVec = new Vector();
            try {
                if (isDirectory()) {
                    try {
                        final String request = " <List xa:currentPath=\"" + fullPath + "\" xmlns:xa=\"http://xaware.org/xas/ns1\" />";
                        String urlString = hostString;
                        if (!urlString.endsWith("/")) {
                            urlString += "/";
                        }
                        urlString += bizViewString;
                        final Element res = postMessageWithAuthentication(request, urlString, uid, pwd);
                        if (res != null) {
                            final Iterator itr = res.getChildren().iterator();
                            while (itr.hasNext()) {
                                final Element cur = (Element) itr.next();
                                final String name = cur.getText();
                                final String type = cur.getAttributeValue("type", ns);
                                final XAHostFileTreeNode aChild = new XAHostFileTreeNode(hostString, bizViewString, name, this, uid, pwd, rootPath);
                                if ((type != null) && type.equals("directory")) {
                                    aChild.isDir = true;
                                } else {
                                    aChild.isDir = false;
                                }
                                tmpVec.add(aChild);
                            }
                        }
                    } catch (final Exception e) {
                        logger.finest("Exception caught browsing host: " + e);
                    }
                }
            } catch (final Exception e) {
                logger.finest("Exception getting children : " + e);
            }
            final Object[] childArray = tmpVec.toArray();
            Arrays.sort(childArray, null);
            children = new Vector(Arrays.asList(childArray));
        }
        return children;
    }

    /**
     * Posts request to host server gets the directory structure
     * 
     * @param updatedString
     *            String
     * @param urlString
     *            String
     * @param uid
     *            String
     * @param pwd
     *            String
     * 
     * @return Element
     */
    public static Element postMessageWithAuthentication(final String updatedString, final String urlString, final String uid, final String pwd) {
        Element res = null;
        try {
            final XAHttpClient httpConnector = new XAHttpClient(urlString);
            httpConnector.init(uid, pwd);
            final SAXBuilder xsb = new SAXBuilder();
            final Element requestElem = xsb.build(new StringReader(updatedString)).getRootElement();
            final InputStream responseStream = httpConnector.executePost(requestElem);
            res = xsb.build(responseStream).getRootElement();
        } catch (final Throwable ex) {
            logger.finest("Error connecting to new host. Exception:" + ex);
        }
        return res;
    }

    /**
     * Checks whether node is File or directory
     * 
     * @return boolean
     */
    private boolean isFileRoot() {
        return isRoot;
    }

    /**
     * Returns full directory path
     * 
     * @return String
     */
    public String getFullPath() {
        return fullPath;
    }

    /**
     * Checks whether node is directory
     * 
     * @return boolean
     */
    public boolean isDirectory() {
        return isDir;
    }

    /**
     * Verifies node with the given Object
     * 
     * @param o1
     *            Object
     * 
     * @return int
     */
    public synchronized int compareTo(final Object o1) {
        int retVal = 0;
        final XAHostFileTreeNode inFile = (XAHostFileTreeNode) o1;
        if ((isDirectory() && inFile.isDirectory()) || (!isDirectory() && !inFile.isDirectory())) {
            retVal = this.toString().compareTo(o1.toString());
        } else if (isDirectory() && !inFile.isDirectory()) {
            retVal = -1;
        } else if (!isDirectory() && inFile.isDirectory()) {
            retVal = 1;
        }
        return retVal;
    }

    /**
     * Calls compareTo method for checking equality
     * 
     * @param in
     *            Object
     * 
     * @return boolean
     */
    @Override
    public boolean equals(final Object in) {
        if (this.compareTo(in) == 0) {
            return true;
        } else {
            return false;
        }
    }

    /**
     * Returns biz file name
     * 
     * @return String
     */
    @Override
    public String toString() {
        return myFileName;
    }

    /**
     * Takes the Post URL and sends request to host server
     * 
     * @param data
     *            String
     * 
     * @return Element
     * 
     * @throws Exception
     *             Thrown in case of I/O error
     */
    public static Element postMessage(final String data) throws Exception {
        final URL theUrl = getHostURL();
        final HttpURLConnection urlConn = (HttpURLConnection) (theUrl).openConnection();
        urlConn.setRequestMethod("POST");
        urlConn.setDoInput(true);
        urlConn.setDoOutput(true);
        final BufferedOutputStream bos = new BufferedOutputStream(urlConn.getOutputStream());
        final String request = data;
        bos.write(request.getBytes(), 0, request.length());
        try {
            bos.close();
        } catch (final Exception e) {
            logger.finest("Exception closing bos : " + e);
        }
        final InputStream bis = urlConn.getInputStream();
        final ByteArrayOutputStream baos = new ByteArrayOutputStream();
        final byte[] buffer = new byte[1024];
        int count = 0;
        while ((count = bis.read(buffer)) > -1) {
            baos.write(buffer, 0, count);
        }
        final SAXBuilder sb = new SAXBuilder();
        logger.finest("Received XML response from server: " + baos.toString());
        return sb.build(new StringReader(baos.toString())).getRootElement();
    }

    /**
     * Returns host URL
     * 
     * @return URL
     * 
     * @throws Exception
     *             Thrown either no legal protocol could be found in a specification string or the string could not be
     *             parsed.
     */
    private static URL getHostURL() throws Exception {
        String urlString = hostString;
        if (!urlString.endsWith("/")) {
            urlString += "/";
        }
        urlString += bizViewString;
        hostURL = new URL(urlString);
        logger.finest("URL=" + hostURL);
        return hostURL;
    }
}
