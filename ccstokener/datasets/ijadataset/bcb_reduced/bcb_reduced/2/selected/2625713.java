package malgnsoft.util;

import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.DocumentBuilder;
import org.xml.sax.InputSource;
import org.w3c.dom.*;
import javax.servlet.jsp.JspWriter;
import java.io.*;
import java.util.HashMap;
import java.net.URL;
import malgnsoft.db.*;
import malgnsoft.util.Malgn;

/**
 * <pre>
 * SimpleParser sp = new SimpleParase("/data/test.xml");
 * //SimpleParser sp = new SimpleParase("http://aaa.com/data/test.xml");
 * //sp.setDebug(out);
 * DataSet ds = sp.getDataSet("//rss/item");
 * m.p(ds);
 * </pre>
 */
public class SimpleParser {

    private JspWriter out = null;

    private boolean debug = false;

    private String path = null;

    private Document doc = null;

    public String errMsg = "";

    public String encoding = "UTF-8";

    public SimpleParser(String filepath) throws Exception {
        this.path = filepath;
        InputStream is = null;
        try {
            if (this.path.indexOf("http") == 0) {
                URL url = new URL(this.path);
                is = url.openStream();
            } else if (this.path.indexOf("<?xml") == 0) {
                is = new ByteArrayInputStream(filepath.getBytes(encoding));
            } else {
                File f = new File(this.path);
                if (!f.exists()) {
                    setError("File not found : " + this.path);
                } else {
                    is = new FileInputStream(f);
                }
            }
            if (is != null) {
                DocumentBuilderFactory dbFactory = DocumentBuilderFactory.newInstance();
                DocumentBuilder dBuilder = dbFactory.newDocumentBuilder();
                doc = dBuilder.parse(is);
                doc.getDocumentElement().normalize();
            }
        } catch (Exception ex) {
            Malgn.errorLog("{SimpleParser.constructor} Path:" + filepath + " " + ex.getMessage());
            setError("Parser Error : " + ex.getMessage());
        } finally {
            if (is != null) is.close();
        }
    }

    public void setDebug(JspWriter out) {
        this.out = out;
        this.debug = true;
    }

    private void setError(String msg) {
        this.errMsg = msg;
        try {
            if (out != null && debug == true) {
                out.print("<hr>" + msg + "<hr>\n");
            }
        } catch (Exception ex) {
        }
    }

    private NodeList getElements(Element elm, String[] nodeArr, int j) {
        if (j == nodeArr.length || "".equals(nodeArr[j])) return null;
        NodeList nodes = elm.getElementsByTagName(nodeArr[j]);
        if (nodes == null) return null;
        j++;
        if (j == nodeArr.length) return nodes;
        for (int i = 0, max = nodes.getLength(); i < max; i++) {
            NodeList nodes2 = getElements((Element) nodes.item(i), nodeArr, j);
            if (nodes2 != null) return nodes2;
        }
        return null;
    }

    public DataSet getDataSet(String node) throws Exception {
        DataSet result = new DataSet();
        if (doc == null) return result;
        try {
            String[] nodeArr = node.substring(2).split("/");
            if (!nodeArr[0].equals(doc.getDocumentElement().getTagName())) return result;
            NodeList nodes = null;
            if (nodeArr.length == 1) {
                nodes = doc.getElementsByTagName(nodeArr[0]);
            } else {
                nodes = getElements(doc.getDocumentElement(), nodeArr, 1);
            }
            if (nodes == null) return result;
            for (int i = 0, n = nodes.getLength(); i < n; i++) {
                result.addRow();
                NodeList xx = nodes.item(i).getChildNodes();
                if (xx == null) continue;
                if (xx.getLength() > 1) {
                    for (int j = 0, k = xx.getLength(); j < k; j++) {
                        Node cnode = xx.item(j);
                        if (cnode.getNodeType() == Node.ELEMENT_NODE && cnode.getFirstChild() != null) {
                            result.put(cnode.getNodeName(), cnode.getFirstChild().getNodeValue());
                        } else {
                            result.put(cnode.getNodeName(), cnode.getNodeValue());
                        }
                    }
                } else {
                    if (nodes.item(i).getNodeType() == Node.ELEMENT_NODE) {
                        result.put(nodes.item(i).getNodeName(), (nodes.item(i)).getFirstChild().getNodeValue());
                    } else {
                        result.put(nodes.item(i).getNodeName(), nodes.item(i).getNodeValue());
                    }
                }
            }
        } catch (Exception ex) {
            Malgn.errorLog("{SimpleParser.getDataSet} Node:" + node + " " + ex.getMessage());
            setError("XPath Error : " + ex.getMessage());
        }
        result.first();
        return result;
    }
}
