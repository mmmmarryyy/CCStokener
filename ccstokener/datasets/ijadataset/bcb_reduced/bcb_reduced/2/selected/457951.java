package jvs.vfs.dom;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.URI;
import java.net.URL;
import java.util.ArrayList;
import java.util.Hashtable;
import java.util.List;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.transform.TransformerException;
import jvs.vfs.IFileBaseImpl;
import org.w3c.dom.CDATASection;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;
import x.java.lang.System;
import com.sun.org.apache.xpath.internal.XPathAPI;

/**
 * @author qiangli
 * 
 */
public class JfsFileImpl extends IFileBaseImpl {

    private static DocumentBuilderFactory factory = DocumentBuilderFactory.newInstance();

    private static final String TAG = "node";

    private static final String A_NAME = "name";

    private static final String A_TYPE = "type";

    private static final String A_SIZE = "size";

    private static final String A_MODIFIED = "modified";

    private static final String V_FOLDER = "folder";

    private static final String V_FILE = "file";

    private static final String V_LINK = "link";

    private static Hashtable pools = new Hashtable();

    private String url = null;

    private String path = null;

    private Element el = null;

    private Document doc = null;

    protected JfsFileImpl() {
    }

    /**
	 * @param uri - jfs:<url>!<path>
	 */
    public JfsFileImpl(URI uri) {
        super(uri);
        String s = uri.toString();
        int lidx = s.lastIndexOf("!");
        path = s.substring(lidx + 1);
        int idx = s.indexOf(":");
        url = s.substring(idx + 1, lidx);
        doc = getDoc(url);
        el = getElement(path);
    }

    private static synchronized Document getDoc(String url) {
        Document doc = null;
        try {
            doc = (Document) pools.get(url);
            if (doc == null) {
                InputStream is = new URL(url).openStream();
                doc = factory.newDocumentBuilder().parse(is);
                doc.normalize();
                doc.getDocumentElement().setAttribute(A_TYPE, V_FOLDER);
                is.close();
                pools.put(url, doc);
            }
        } catch (Exception e) {
        }
        return doc;
    }

    private Element getElement(String path) {
        try {
            if (path == null || path.equals("") || path.equals("/")) {
                return doc.getDocumentElement();
            }
            String[] pa = path.split("/");
            StringBuffer xpath = new StringBuffer("/root");
            for (int i = 0; i < pa.length; i++) {
                if (pa[i].length() > 0) {
                    xpath.append("/node[@name='" + pa[i] + "']");
                }
            }
            NodeList nl = XPathAPI.selectNodeList(doc, xpath.toString());
            if (nl.getLength() > 0) {
                return (Element) nl.item(0);
            } else {
            }
        } catch (TransformerException e) {
            debug(e);
        }
        return null;
    }

    private Element getParentEl() {
        int idx = path.lastIndexOf("/");
        Element pel = null;
        if (idx > 0) {
            String p = path.substring(0, idx);
            pel = getElement(p);
        } else {
            pel = doc.getDocumentElement();
        }
        return pel;
    }

    private String getElName() {
        int idx = path.lastIndexOf("/");
        String name = path.substring(idx + 1);
        return name;
    }

    public boolean create() {
        if (el != null) {
            return false;
        }
        Element pel = getParentEl();
        String name = getElName();
        el = doc.createElement(TAG);
        el.setAttribute(A_NAME, name);
        el.setAttribute(A_TYPE, V_FILE);
        el.setAttribute(A_SIZE, "0");
        el.setAttribute(A_MODIFIED, System.currentTimeMillis() + "");
        el.appendChild(doc.createCDATASection(""));
        pel.appendChild(el);
        return true;
    }

    public boolean delete() {
        if (el == null) {
            return false;
        }
        el.getParentNode().removeChild(el);
        return true;
    }

    public boolean exists() {
        return el != null;
    }

    private CDATASection getDataSection() {
        if (el == null) {
            return null;
        }
        Node n = el.getFirstChild();
        if (n == null) {
            return null;
        }
        do {
            if (n instanceof CDATASection) {
                CDATASection cdata = (CDATASection) n;
                return cdata;
            }
        } while ((n = n.getNextSibling()) != null);
        return null;
    }

    public InputStream getInputStream() {
        CDATASection d = getDataSection();
        if (!exists()) {
            throw new RuntimeException("file does not exist: " + uri + " url");
        }
        String s = "";
        if (d != null) {
            s = d.getData();
        }
        return new ByteArrayInputStream(s.getBytes());
    }

    public OutputStream getOutputStream() {
        OutputStream os = new ByteArrayOutputStream() {

            public void close() throws IOException {
                if (el == null) {
                    create();
                }
                CDATASection d = getDataSection();
                if (d != null) {
                    String data = this.toString();
                    d.setData(data);
                    el.setAttribute(A_SIZE, data.length() + "");
                }
            }
        };
        return os;
    }

    public boolean isDirectory() {
        if (el == null) {
            return false;
        }
        String attr = el.getAttribute(A_TYPE);
        return attr != null && (attr.equalsIgnoreCase(V_FOLDER));
    }

    public boolean isFile() {
        if (el == null) {
            return false;
        }
        String attr = el.getAttribute(A_TYPE);
        return attr != null && attr.equalsIgnoreCase(V_FILE);
    }

    public boolean isLink() {
        if (el == null) {
            return false;
        }
        String attr = el.getAttribute(A_TYPE);
        return attr != null && attr.equalsIgnoreCase(V_LINK);
    }

    public String[] list() {
        if (el == null) {
            return null;
        }
        NodeList nl = el.getChildNodes();
        List l = new ArrayList();
        for (int i = 0; i < nl.getLength(); i++) {
            if (nl.item(i) instanceof Element) {
                l.add(((Element) nl.item(i)).getAttribute(A_NAME));
            }
        }
        return (String[]) l.toArray(new String[0]);
    }

    public boolean mkdir() {
        if (el != null) {
            return false;
        }
        try {
            Element pel = getParentEl();
            String name = getElName();
            el = doc.createElement(TAG);
            el.setAttribute(A_NAME, name);
            el.setAttribute(A_TYPE, V_FOLDER);
            el.setAttribute(A_SIZE, "0");
            el.setAttribute(A_MODIFIED, System.currentTimeMillis() + "");
            pel.appendChild(el);
            return true;
        } catch (Exception e) {
            debug(e);
        }
        return false;
    }

    public boolean move(URI dest) {
        if (dest.getScheme().equalsIgnoreCase("jfs")) {
            JfsFileImpl d = new JfsFileImpl(dest);
            if (d.exists()) {
                return false;
            }
            if (d.url.equals(this.url)) {
                Element p = getParentEl();
                p.removeChild(el);
                el.setAttribute(A_NAME, d.getElName());
                d.getParentEl().appendChild(el);
                return true;
            }
        }
        return false;
    }

    public boolean copy(URI dest) {
        if (dest.getScheme().equalsIgnoreCase("jfs")) {
            JfsFileImpl d = new JfsFileImpl(dest);
            if (d.exists()) {
                return false;
            }
            if (d.url.equals(this.url)) {
                Element cl = (Element) el.cloneNode(true);
                cl.setAttribute(A_NAME, d.getElName());
                d.getParentEl().appendChild(cl);
                return true;
            }
        }
        return false;
    }

    public String getContent() {
        CDATASection d = getDataSection();
        if (d == null) {
            return null;
        }
        return d.getData();
    }

    public long getLastModified() {
        if (el == null) {
            return 0;
        }
        try {
            String m = el.getAttribute(A_MODIFIED);
            return Long.parseLong(m);
        } catch (NumberFormatException e) {
        } catch (Exception e) {
        }
        return 0;
    }

    public long getLength() {
        if (el == null) {
            return -1;
        }
        try {
            String s = el.getAttribute(A_SIZE);
            if (s == null || s.trim().length() == 0) {
                s = getContent();
                return s.length();
            } else {
                return Long.parseLong(s.trim());
            }
        } catch (Exception e) {
        }
        return 0;
    }

    public boolean setContent(String s) {
        try {
            CDATASection d = getDataSection();
            d.setData(s);
            return true;
        } catch (Exception e) {
            debug(e);
        }
        return false;
    }

    public boolean setLastModified(long time) {
        try {
            el.setAttribute(A_MODIFIED, time + "");
            return true;
        } catch (Exception e) {
        }
        return false;
    }

    public boolean setLength(long len) {
        return false;
    }

    public Object getAttribute(String name) {
        if (el == null) {
            return null;
        }
        return el.getAttribute(name);
    }

    public boolean setAttribute(String name, Object value) {
        if (el == null) {
            return false;
        }
        if (value instanceof String) {
            el.setAttribute(name, value.toString());
            return true;
        }
        return false;
    }
}
