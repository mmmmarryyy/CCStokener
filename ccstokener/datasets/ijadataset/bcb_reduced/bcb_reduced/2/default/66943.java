import com.sun.org.apache.xpath.internal.XPathAPI;
import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStreamWriter;
import java.io.PrintWriter;
import java.net.URL;
import java.util.ResourceBundle;
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
 * TraitMap�f�[�^�x�[�X�y�щ{���V�X�e��, �Q�O�O�R�N�P��<br>
 * �S���ҁF�L�c�N�Y�A�Q�m���m���x�[�X�����J���`�[���E�C���t�H�}�e�B�N�X��Վ{�݁EGSC�ERIKEN
 * 
 * @version 2.0
 * @author Moroda
 */
public class TraitMapDataView {

    String outDir;

    String baseNodeXpath;

    String filenameXpath;

    String urlNameXpath;

    Node baseNode;

    public TraitMapDataView() {
        clear();
    }

    private Document getDocument(URL url) throws SAXException, IOException {
        InputStream is;
        try {
            is = url.openStream();
        } catch (IOException io) {
            System.out.println("parameter error : The specified reading data is mistaken.");
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
            throw new RuntimeException(e.toString());
        }
        return doc;
    }

    private void makeHtml(Node _baseNode) throws TransformerException, RESyntaxException, IOException {
        String urlName = "";
        baseNode = _baseNode;
        StringBuffer filenameBuf = new StringBuffer();
        NodeList nl1 = XPathAPI.selectNodeList(baseNode, filenameXpath);
        for (int i = 0; i < nl1.getLength(); i++) {
            Node nd1 = nl1.item(i);
            if (filenameBuf.length() > 0) filenameBuf.append("-");
            filenameBuf.append(getNodeTextValue(nd1));
        }
        String outputFileName = outDir + File.separator + filenameBuf.toString() + ".htm";
        NodeList nl2 = XPathAPI.selectNodeList(baseNode, urlNameXpath);
        for (int j = 0; j < nl2.getLength(); j++) {
            Node nd2 = nl2.item(j);
            urlName = getNodeTextValue(nd2);
        }
        URL url = new URL(urlName);
        BufferedReader br = new BufferedReader(new InputStreamReader(url.openStream()));
        StringBuffer htmlString = new StringBuffer();
        String line = "";
        while ((line = br.readLine()) != null) {
            htmlString.append(line + "\n");
        }
        br.close();
        PrintWriter pw = prepareHtml(outputFileName);
        pw.println(htmlString.toString());
        pw.close();
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

    private PrintWriter prepareHtml(String filePath) throws IOException, RESyntaxException {
        PrintWriter pw = null;
        RE re = new RE("[*]");
        filePath = re.subst(filePath, "", RE.REPLACE_ALL);
        RE re2 = new RE("[:space:]");
        filePath = re2.subst(filePath, "", RE.REPLACE_ALL);
        RE re3 = new RE("[\\:]");
        filePath = re3.subst(filePath, "-", RE.REPLACE_ALL);
        FileOutputStream fos = new FileOutputStream(new File(filePath));
        OutputStreamWriter osw = null;
        osw = new OutputStreamWriter(fos, "EUC_JP");
        pw = new PrintWriter(new BufferedWriter(osw));
        return pw;
    }

    private void setPropertiesData(ResourceBundle rb) {
        baseNodeXpath = rb.getString("BASE_NODE");
        filenameXpath = rb.getString("FILE_NAME");
        urlNameXpath = rb.getString("URL_NAME");
    }

    public void clear() {
        outDir = null;
        baseNodeXpath = null;
        filenameXpath = null;
        urlNameXpath = null;
        baseNode = null;
    }

    public static void main(String[] args) {
        TraitMapDataView tmdv = new TraitMapDataView();
        if (args == null) {
            System.out.println("The parameter is not specified.");
            return;
        } else if (args.length != 3) {
            System.out.println("Please check specification of reading data, ");
            System.out.println("a HTML output place directory, and a setting file name.");
            return;
        }
        String _uri = args[0];
        String _outDir = args[1];
        ResourceBundle rb;
        try {
            rb = ResourceBundle.getBundle(args[2]);
        } catch (Exception e) {
            e.printStackTrace();
            System.out.println("parameter error : The specified setting file is not found.");
            System.out.println("\t" + e.getMessage());
            return;
        }
        tmdv.setPropertiesData(rb);
        try {
            File dir = new File(_outDir);
            if (dir.isDirectory()) {
                tmdv.outDir = _outDir;
            } else {
                System.out.println("parameter error : " + _outDir + " is the directory which does not exist or it is not a directory.");
                IOException ioe = new IOException(_outDir + " is not a directory.");
                throw ioe;
            }
            URL url = new URL(_uri);
            Document sourceDoc = tmdv.getDocument(url);
            Node rootNode = sourceDoc.getDocumentElement();
            NodeList nl = XPathAPI.selectNodeList(rootNode, tmdv.baseNodeXpath);
            if (nl == null || nl.getLength() == 0) {
                System.out.println("error : root node is not found");
                System.out.println("\t" + "Reading data may be wrong.");
                return;
            }
            for (int i = 0; i < nl.getLength(); i++) {
                tmdv.makeHtml(nl.item(i));
            }
        } catch (Exception e) {
            System.out.println(e.toString());
            e.printStackTrace(System.err);
        }
    }
}
