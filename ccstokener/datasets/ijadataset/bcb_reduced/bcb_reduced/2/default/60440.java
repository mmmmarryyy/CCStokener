import com.sun.org.apache.xpath.internal.XPathAPI;
import com.sun.org.apache.xpath.internal.objects.XObject;
import java.io.BufferedWriter;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStreamWriter;
import java.io.PrintWriter;
import java.net.URL;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.ResourceBundle;
import java.util.Stack;
import java.util.StringTokenizer;
import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.ParserConfigurationException;
import javax.xml.transform.TransformerException;
import org.apache.regexp.RE;
import org.apache.regexp.RESyntaxException;
import org.w3c.dom.Document;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;
import org.xml.sax.SAXException;

/** 
 * TraitMap�f?[�^�x?[�X�y�щ{���V�X�e��, �Q�O�O�R�N�P��<br>
 * �S����?F�L�c�N�Y?A�Q�m���m���x?[�X�����J���`?[��?E�C���t�H�}�e�B�N�X��Վ{?�?EGSC?ERIKEN
 * 
 * @version 2.0
 * @author Moroda
 */
public class TraitXpath {

    private static final double POS_RATE = 0.1;

    private static final String META_TAG = "<meta http-equiv=\"Content-Type\" content=\"text/html; charset=UTF-8\">\n";

    private static final int MAP_POS_VAL = 100000;

    Document sourceDoc;

    String sourceUri;

    String outDir;

    String baseNodeXpath;

    String highlightXpath;

    String[] titleKeyList;

    String[] filenameKeyList;

    HashMap keywordsMap;

    HashMap parameterMap;

    Node baseNode;

    String urlName;

    String urlPath;

    String urlOtherParameters;

    /**
	 * @author Administrator
	 *
	 * To change this generated comment edit the template variable "typecomment":
	 * Window>Preferences>Java>Templates.
	 * To enable and disable the creation of type comments go to
	 * Window>Preferences>Java>Code Generation.
	 */
    public TraitXpath() {
        clear();
    }

    private Document getDocument(URL url) throws SAXException, IOException {
        InputStream is;
        try {
            is = url.openStream();
        } catch (IOException io) {
            System.out.println("parameter error : The specified reading data is mistaken.");
            System.out.println(" Request URL is " + sourceUri);
            throw new IOException("\t" + io.toString());
        }
        DocumentBuilderFactory factory = DocumentBuilderFactory.newInstance();
        DocumentBuilder builder = null;
        try {
            builder = factory.newDocumentBuilder();
        } catch (ParserConfigurationException pce) {
            System.out.println("error : The error of DocumentBuilder instance generation");
            throw new RuntimeException(pce.toString());
        }
        Document doc;
        try {
            doc = builder.parse(is);
        } catch (Exception e) {
            System.out.println("error : parse of reading data went wrong.");
            System.out.println(" Request URL is " + sourceUri);
            throw new RuntimeException(e.toString());
        }
        return doc;
    }

    private String getNodeTextValue(Node node) {
        String nodeValue = "";
        switch(node.getNodeType()) {
            case Node.ELEMENT_NODE:
                if (node.getFirstChild() == null) {
                    return nodeValue;
                }
                nodeValue = node.getFirstChild().getNodeValue();
                break;
            default:
                nodeValue = node.getNodeValue();
                break;
        }
        return nodeValue;
    }

    private String getXPathObjectVal(String paraVal) throws TransformerException {
        String retVal = "";
        XObject xobj = XPathAPI.eval(baseNode, paraVal);
        switch(xobj.getType()) {
            case XObject.CLASS_STRING:
                retVal = xobj.str();
                break;
            case XObject.CLASS_NODESET:
                NodeList nl4 = xobj.nodelist();
                for (int count = 0; count < nl4.getLength(); count++) {
                    retVal += this.getNodeTextValue(nl4.item(count));
                }
                break;
            default:
                break;
        }
        return retVal;
    }

    private PrintWriter prepareHtml(String fileName) throws IOException, RESyntaxException {
        PrintWriter pw = null;
        RE re = new RE("[*]");
        fileName = re.subst(fileName, "", RE.REPLACE_ALL);
        RE re2 = new RE("[:space:]");
        fileName = re2.subst(fileName, "", RE.REPLACE_ALL);
        RE re3 = new RE("[\\:]");
        fileName = re3.subst(fileName, "-", RE.REPLACE_ALL);
        File file = new File(this.outDir + File.separator + fileName);
        FileOutputStream fos = new FileOutputStream(file);
        OutputStreamWriter osw = null;
        osw = new OutputStreamWriter(fos, "EUC_JP");
        pw = new PrintWriter(new BufferedWriter(osw));
        return pw;
    }

    private String[] spritDictString(String str, String mark) {
        try {
            StringTokenizer strToken = new StringTokenizer(str, mark, true);
            if (!strToken.hasMoreElements()) return null;
            Stack st = new Stack();
            String temp = "";
            while (strToken.hasMoreElements()) {
                String val = strToken.nextElement().toString();
                if (val.endsWith(mark)) {
                    temp += val.substring(0, val.length() - 1);
                    st.push(temp);
                    temp = "";
                } else {
                    temp += val;
                }
            }
            st.push(temp);
            String[] vals = new String[st.size()];
            int i = st.size() - 1;
            while (!st.empty()) {
                vals[i] = (String) st.pop();
                int a = 1;
                i--;
            }
            return vals;
        } catch (Exception e) {
            return null;
        }
    }

    private String convertHead(String sPos, String ePos) {
        String strPos = "";
        if (sPos == null || sPos.equals("") || ePos == null || ePos.equals("")) {
            return strPos;
        }
        double staPos = Double.parseDouble(sPos);
        double endPos = Double.parseDouble(ePos);
        double len = endPos - staPos;
        if (len > MAP_POS_VAL) {
            len = len * POS_RATE;
            staPos = staPos - len;
            endPos = endPos + len;
        } else {
            if (staPos - MAP_POS_VAL > 0) {
                staPos = staPos - MAP_POS_VAL;
            } else {
                staPos = 1;
            }
            endPos += MAP_POS_VAL;
        }
        strPos += String.valueOf(new Double(staPos).intValue()) + "-" + String.valueOf(new Double(endPos).intValue());
        return strPos;
    }

    private String convertUrl(String url) throws RESyntaxException {
        String retUrl = "";
        RE r1 = new RE("[(]");
        retUrl = r1.subst(url, ".");
        RE r2 = new RE("[)]");
        retUrl = r2.subst(retUrl, ".");
        RE r3 = new RE("[:space:]");
        retUrl = r3.subst(retUrl, ".");
        return retUrl;
    }

    private StringBuffer makeScript(StringBuffer sb) {
        sb.append("<script language=\"JavaScript\">\n");
        sb.append("<!--\n");
        sb.append("\t" + "function gotoIndex(url)\n");
        sb.append("\t" + "{\n");
        sb.append("\t\t" + "location.href = url;\n");
        sb.append("\t\t" + "return;\n");
        sb.append("\t" + "}\n");
        sb.append("//-->\n");
        sb.append("</script>\n");
        return sb;
    }

    private void setPropertiesData(ResourceBundle rb) {
        baseNodeXpath = rb.getString("BASE_NODE");
        parameterMap.put("halias", rb.getString("H_ALIAS"));
        parameterMap.put("hchrom", rb.getString("H_CHROM"));
        parameterMap.put("hstart", rb.getString("H_START"));
        parameterMap.put("hend", rb.getString("H_END"));
        parameterMap.put("hcheck", rb.getString("H_CHECK"));
        parameterMap.put("valias", rb.getString("V_ALIAS"));
        parameterMap.put("vchrom", rb.getString("V_CHROM"));
        parameterMap.put("vstart", rb.getString("V_START"));
        parameterMap.put("vend", rb.getString("V_END"));
        parameterMap.put("vcheck", rb.getString("V_CHECK"));
        highlightXpath = rb.getString("HIGHRIGHT");
        titleKeyList = spritDictString(rb.getString("TITLE_NODE"), ",");
        filenameKeyList = spritDictString(rb.getString("FILE_NAME"), ",");
        String[] h1KeyList = spritDictString(rb.getString("H1"), ",");
        String[] h2KeyList = spritDictString(rb.getString("H2"), ",");
        String[] h3KeyList = spritDictString(rb.getString("H3"), ",");
        String[] h4KeyList = spritDictString(rb.getString("H4"), ",");
        String[] h5KeyList = spritDictString(rb.getString("H5"), ",");
        String[] h6KeyList = spritDictString(rb.getString("H6"), ",");
        String[] strongKeyList = spritDictString(rb.getString("STRONG"), ",");
        String[] pKeyList = spritDictString(rb.getString("P"), ",");
        keywordsMap.put("h1", h1KeyList);
        keywordsMap.put("h2", h2KeyList);
        keywordsMap.put("h3", h3KeyList);
        keywordsMap.put("h4", h4KeyList);
        keywordsMap.put("h5", h5KeyList);
        keywordsMap.put("h6", h6KeyList);
        keywordsMap.put("strong", strongKeyList);
        keywordsMap.put("p", pKeyList);
    }

    private void clear() {
        sourceDoc = null;
        sourceUri = null;
        baseNodeXpath = null;
        highlightXpath = null;
        titleKeyList = null;
        filenameKeyList = null;
        keywordsMap = new HashMap();
        parameterMap = new HashMap();
        outDir = null;
        baseNode = null;
        urlName = null;
        urlPath = null;
    }

    private void fileDelete(String outputFileName) {
        File file = new File(outputFileName);
        if (file.exists()) file.delete();
        return;
    }

    private static final String getOtherQueryParameters(URL url) {
        String[] ommitParams = { "hHead", "hCheck", "vHead", "vCheck" };
        HashSet _paramSet = new HashSet();
        StringTokenizer _queries = new StringTokenizer(url.getQuery(), "&");
        while (_queries.hasMoreElements()) {
            String _param = _queries.nextToken();
            StringTokenizer _st = new StringTokenizer(_param, "=");
            String _paramName = _st.nextToken();
            boolean _match = false;
            for (int i = 0; i < ommitParams.length; i++) {
                if (_paramName.equals(ommitParams[i])) {
                    _match = true;
                }
            }
            if (!_match) {
                _paramSet.add(_param);
            }
        }
        String queryParams = "";
        Iterator ite = _paramSet.iterator();
        while (ite.hasNext()) {
            queryParams += "&" + ite.next();
        }
        return queryParams;
    }
}
