package org.xaware.ide.xadev.bizview.publish;

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
import org.jdom.Element;
import org.jdom.Namespace;
import org.jdom.input.SAXBuilder;
import org.xaware.ide.xadev.datamodel.DefaultMutableTreeNode;
import org.xaware.ide.xadev.datamodel.MutableTreeNode;
import org.xaware.shared.util.XAHttpClient;
import org.xaware.shared.util.XAwareConstants;
import org.xaware.shared.util.logging.XAwareLogger;

/**
 * Tree node used in displaying host server directory structure
 *
 * @author T Vasu
 * @author Saritha
 * @version 1.0
*/
public class XAHostFileTreeNode extends DefaultMutableTreeNode implements Comparable {

    /** Serializable unique ID */
    private static final long serialVersionUID = -2393671619389882792L;

    /** Name space */
    private static final Namespace ns = XAwareConstants.xaNamespace;

    /** Part of URL string for publish related requests */
    private static final String BIZVIEW = "publish";

    /** URI string */
    private static String bizViewString = "xaware/XAServlet?_BIZVIEW=" + BIZVIEW;

    /** Host Server url */
    private static URL hostURL;

    /** Hoat server name */
    private static String hostString = "";

    /** Class level logger */
    private static final XAwareLogger logger = XAwareLogger.getXAwareLogger(XAHostFileTreeNode.class.getName());

    /** Biz file name */
    private String myFileName;

    /** Complete path */
    private String fullPath;

    /** Flag for checking directory */
    private boolean isDir;

    /** Flag for checking the given node is root node */
    private boolean isRoot;

    /** User name */
    private String uid = null;

    /** Password */
    private String pwd = null;

    /** Root path for the directory tree */
    private String rootPath = "/";

    /**
	 * Creates a new XAHostFileTreeNode object.
	 *
	 * @param hostString String
	 * @param bizViewString String
	 * @param inFile String
	 * @param uid String
	 * @param pwd String
	 */
    public XAHostFileTreeNode(final String hostString, final String bizViewString, final String inFile, final String uid, final String pwd) {
        super(inFile);
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
	 * @param hostString String
	 * @param bizViewString String
	 * @param inFile String
	 * @param inParent XAHostFileTreeNode
	 * @param uid String
	 * @param pwd String
	 * @param rootPath String
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

    public void insert(final DefaultMutableTreeNode child, final int index) {
        super.insert(child, index);
    }

    /**
	 * Removes child element at the given index
	 *
	 * @param index int
	 */
    @Override
    public void remove(final int index) {
        myChildren().removeElementAt(index);
    }

    /**
	 * Removes given tree node and its children if any
	 *
	 * @param node DefaultMutableTreeNode
	 */
    public void remove(final DefaultMutableTreeNode node) {
        myChildren().removeElement(node);
    }

    /**
	 * Dummy implementation
	 */
    @Override
    public void removeFromParent() {
    }

    /**
	 * Sets the given Tree node as parent
	 *
	 * @param newParent DefaultMutableTreeNode
	 */
    @Override
    public void setParent(final DefaultMutableTreeNode newParent) {
        parent = newParent;
    }

    /**
	 * Sets the given object as input
	 *
	 * @param object Object
	 */
    @Override
    public void setUserObject(final Object object) {
        userObject = object;
    }

    @Override
    public Enumeration children() {
        return myChildren().elements();
    }

    /**
	 * Checks whether the node has children
	 *
	 * @return boolean
	 */
    @Override
    public boolean getAllowsChildren() {
        return isDir;
    }

    /**
	 * Returns the child node at given index
	 *
	 * @param index int
	 *
	 * @return MutableTreeNode
	 */
    @Override
    public MutableTreeNode getChildAt(final int index) {
        return (DefaultMutableTreeNode) myChildren().elementAt(index);
    }

    /**
	 * Returns no.of children
	 *
	 * @return int
	 */
    @Override
    public int getChildCount() {
        return myChildren().size();
    }

    /**
	 * Returns index for the given node
	 *
	 * @param node DefaultMutableTreeNode
	 *
	 * @return int
	 */
    public int getIndex(final DefaultMutableTreeNode node) {
        return myChildren().indexOf(node);
    }

    /**
	 * Returns parent node
	 *
	 * @return MutableTreeNode
	 */
    @Override
    public MutableTreeNode getParent() {
        return parent;
    }

    /**
	 * Returns the status of the node whether is leaf node
	 *
	 * @return boolean
	 */
    @Override
    public boolean isLeaf() {
        return !isDir;
    }

    /**
	 * Returns children
	 *
	 * @return Vector
	 */
    public synchronized Vector myChildren() {
        if (children == null) {
            final Vector tmpVec = new Vector();
            try {
                if (isDirectory()) {
                    try {
                        final String request = " <List xmlns:xa=\"http://xaware.org/xas/ns1\" xa:currentPath=\"" + fullPath + "\"  />";
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
                        logger.info("Exception caught browsing host: " + e);
                        logger.printStackTrace(e);
                    }
                }
            } catch (final Exception e) {
                logger.info("Exception getting children : " + e);
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
	 * @param updatedString String
	 * @param urlString String
	 * @param uid String
	 * @param pwd String
	 *
	 * @return Element
	 */
    public static Element postMessageWithAuthentication(final String updatedString, final String urlString, final String uid, final String pwd) {
        Element res = null;
        XAHttpClient httpConnector = null;
        try {
            httpConnector = new XAHttpClient(urlString);
            httpConnector.init(uid, pwd);
            final SAXBuilder xsb = new SAXBuilder();
            final Element requestElem = xsb.build(new StringReader(updatedString)).getRootElement();
            final InputStream responseStream = httpConnector.executePost(requestElem);
            res = xsb.build(responseStream).getRootElement();
        } catch (final Throwable ex) {
            logger.info("Error connecting to new host. Exception:" + ex);
        } finally {
            try {
                httpConnector.close();
            } catch (final Exception ex) {
            }
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
	 * @param o1 Object
	 *
	 * @return int
	 */
    public synchronized int compareTo(Object o1) {
        if (o1 instanceof Vector) {
            o1 = ((Vector) o1).get(0);
        }
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
	 * @param in Object
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
	 * @param data String
	 *
	 * @return Element
	 *
	 * @throws Exception Thrown in case of I/O error
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
            logger.info("Exception closing bos : " + e);
        }
        final InputStream bis = urlConn.getInputStream();
        final ByteArrayOutputStream baos = new ByteArrayOutputStream();
        final byte[] buffer = new byte[1024];
        int count = 0;
        while ((count = bis.read(buffer)) > -1) {
            baos.write(buffer, 0, count);
        }
        final SAXBuilder sb = new SAXBuilder();
        logger.info("Received XML response from server: " + baos.toString());
        return sb.build(new StringReader(baos.toString())).getRootElement();
    }

    /**
	 * Returns host URL
	 *
	 * @return URL
	 *
	 * @throws Exception Thrown either no legal protocol could be found in a
	 *         specification string or the string could not be parsed.
	 */
    private static URL getHostURL() throws Exception {
        String urlString = hostString;
        if (!urlString.endsWith("/")) {
            urlString += "/";
        }
        urlString += bizViewString;
        hostURL = new URL(urlString);
        logger.info("URL=" + hostURL);
        return hostURL;
    }
}
