package Tflame;

import Action.lineMode.accessionSearch.AccessionSearchCriteria;
import com.sun.org.apache.xml.internal.serialize.OutputFormat;
import com.sun.org.apache.xml.internal.serialize.XMLSerializer;
import java.io.BufferedInputStream;
import java.io.BufferedWriter;
import java.io.ByteArrayInputStream;
import java.io.File;
import java.io.FileOutputStream;
import java.io.FileWriter;
import java.io.IOException;
import java.io.InputStream;
import java.io.PrintWriter;
import java.io.StringReader;
import java.io.StringWriter;
import java.net.URL;
import java.net.URLConnection;
import java.sql.Connection;
import java.sql.ResultSet;
import java.util.ArrayList;
import java.util.Enumeration;
import java.util.Hashtable;
import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.ParserConfigurationException;
import javax.xml.transform.Templates;
import javax.xml.transform.Transformer;
import javax.xml.transform.TransformerFactory;
import javax.xml.transform.dom.DOMSource;
import javax.xml.transform.stream.StreamResult;
import javax.xml.transform.stream.StreamSource;
import org.apache.log4j.Logger;
import org.apache.regexp.RE;
import org.apache.regexp.RESyntaxException;
import org.apache.torque.Torque;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.xml.sax.InputSource;
import org.xml.sax.SAXException;

/**
 * TraitMap�f�[�^�x�[�X�y�щ{���V�X�e��, �Q�O�O�R�N�P�� <br>
 * �S���ҁF�L�c�N�Y�A�Q�m���m���x�[�X�����J���`�[���E�C���t�H�}�e�B�N�X��Վ{�݁EGSC�ERIKEN
 * 
 * @version 2.0
 * @author isobe
 */
public final class Utils {

    static Logger log = Logger.getLogger(Utils.class);

    /**
	 * Method getRealPath. <br>
	 * web application��virtual path����real path���擾����B
	 * 
	 * @param virtalPath
	 * @return String real path
	 */
    public static String getRealPath(String virtalPath) {
        String RealPath;
        if (virtalPath.length() != 0) {
            RealPath = Garage.getServletContext().getRealPath(virtalPath);
        } else {
            RealPath = virtalPath;
        }
        return RealPath;
    }

    /**
	 * Method getDocFile. virutal path�Ŏw�肳���xml����DOM�𐶐�����B
	 * 
	 * @param virtualPath
	 * @return Document document object
	 */
    public static Document getDocFile(String virtualPath) {
        File file = new File(getRealPath(virtualPath));
        DocumentBuilderFactory _factory = DocumentBuilderFactory.newInstance();
        _factory.setNamespaceAware(false);
        _factory.setValidating(false);
        DocumentBuilder builder = null;
        Document _doc = null;
        try {
            if (file.isFile() && file.canRead()) {
                builder = _factory.newDocumentBuilder();
                _doc = builder.parse(file);
            }
        } catch (Exception e) {
            _doc = null;
            log.error(file.getAbsolutePath(), e);
        }
        return _doc;
    }

    /**
	 * Method getWebDoc. uri�Ŏw�肳���xml����DOM�𐶐�����B
	 * 
	 * @param prot
	 *            protcol
	 * @param host
	 *            hostname
	 * @param port
	 *            port
	 * @param path
	 *            path
	 * @return Document
	 */
    public static Document getWebDoc(String prot, String host, int port, String path) {
        URL url_to_connect;
        InputStream stream;
        Document doc = null;
        try {
            DocumentBuilderFactory dbf = DocumentBuilderFactory.newInstance();
            DocumentBuilder db = dbf.newDocumentBuilder();
            url_to_connect = new URL(prot, host, port, path);
            stream = url_to_connect.openStream();
            doc = db.parse(stream);
        } catch (Exception e) {
            log.error(e);
        }
        return doc;
    }

    /**
	 * Method getWebDoc. url�Ŏw�肳���xml����DOM�𐶐�����B
	 * 
	 * @param url
	 * @return Document
	 */
    public static Document getWebDoc(String url) {
        URL url_to_connect;
        BufferedInputStream stream = null;
        Document doc = null;
        try {
            DocumentBuilderFactory dbf = DocumentBuilderFactory.newInstance();
            DocumentBuilder db = dbf.newDocumentBuilder();
            url_to_connect = new URL(url);
            URLConnection connection = url_to_connect.openConnection();
            stream = new BufferedInputStream(connection.getInputStream());
            doc = db.parse(stream);
        } catch (Exception e) {
            log.error(e + "\n" + url);
            try {
                stream.close();
            } catch (Exception e1) {
            }
        }
        return doc;
    }

    /**
	 * Method transform. DOM�ƂȂ��Ă���xml��xsl�ŕϊ�����B
	 * 
	 * @param inputDoc
	 *            soure document
	 * @param xslPath
	 *            xsl file path (real)
	 * @return Document transformed document
	 */
    public static Document transform(Document inputDoc, String xslPath) {
        Transformer transformer = null;
        StreamResult xmlOutput = null;
        StringWriter xmlWriter = null;
        String xmlString = null;
        Document outputDoc = null;
        try {
            TransformerFactory factory = TransformerFactory.newInstance();
            try {
                transformer = factory.newTransformer(new StreamSource(Utils.getRealPath(xslPath)));
            } catch (Exception e) {
                System.err.println(e.getMessage());
                transformer = factory.newTransformer();
            }
            xmlWriter = new StringWriter(2048);
            xmlOutput = new StreamResult(xmlWriter);
            transformer.transform(new DOMSource(inputDoc), xmlOutput);
        } catch (Exception e) {
            log.error(e);
        }
        String tmpxmlString = xmlWriter.toString();
        int pcnt = tmpxmlString.indexOf("?>");
        int scnt = tmpxmlString.indexOf("<!");
        int ecnt = tmpxmlString.indexOf(">", scnt);
        if (scnt != -1) {
            xmlString = tmpxmlString.substring(ecnt + 1, tmpxmlString.length());
        } else if (pcnt != -1) {
            xmlString = tmpxmlString.substring(pcnt + 2, tmpxmlString.length());
        }
        try {
            DocumentBuilderFactory dbf = DocumentBuilderFactory.newInstance();
            dbf.setNamespaceAware(false);
            dbf.setValidating(false);
            DocumentBuilder db = dbf.newDocumentBuilder();
            outputDoc = db.parse(new InputSource(new StringReader(xmlString)));
        } catch (Exception e) {
        }
        return outputDoc;
    }

    /**
	 * Method transform. DOM�ƂȂ��Ă���xml��xsl�ŕϊ�����B
	 * 
	 * @param inputDoc
	 *            soure document
	 * @param xslPath
	 *            xsl file path (real)
	 * @param parameters
	 *            xsl parameters
	 * @return Document transformed document
	 */
    public static Document transform(Document inputDoc, String xslPath, Hashtable parameters) {
        Templates xslTemplate = null;
        Transformer transformer = null;
        StreamResult xmlOutput = null;
        StringWriter xmlWriter = null;
        String xmlString = null;
        Document outputDoc = null;
        try {
            TransformerFactory factory = TransformerFactory.newInstance();
            xslTemplate = factory.newTemplates(new StreamSource(Utils.getRealPath(xslPath)));
        } catch (Exception e) {
        }
        try {
            transformer = xslTemplate.newTransformer();
            if (parameters != null) {
                Enumeration _enum = parameters.keys();
                while (_enum.hasMoreElements()) {
                    String _key = (String) _enum.nextElement();
                    String _value = (String) parameters.get(_key);
                    transformer.setParameter(_key, _value);
                }
            }
        } catch (Exception e) {
        }
        try {
            xmlWriter = new StringWriter(2048);
            xmlOutput = new StreamResult(xmlWriter);
            transformer.transform(new DOMSource(inputDoc), xmlOutput);
        } catch (Exception e) {
        }
        int pcnt = 0;
        int scnt = 0;
        int ecnt = 0;
        String tmpxmlString = xmlWriter.toString();
        pcnt = tmpxmlString.indexOf("?>");
        scnt = tmpxmlString.indexOf("<!");
        ecnt = tmpxmlString.indexOf(">", scnt);
        if (scnt != -1) {
            xmlString = tmpxmlString.substring(ecnt + 1, tmpxmlString.length());
        } else if (pcnt != -1) {
            xmlString = tmpxmlString.substring(pcnt + 2, tmpxmlString.length());
        }
        try {
            DocumentBuilderFactory dbf = DocumentBuilderFactory.newInstance();
            dbf.setNamespaceAware(false);
            dbf.setValidating(false);
            DocumentBuilder db = dbf.newDocumentBuilder();
            outputDoc = db.parse(new InputSource(new StringReader(xmlString)));
        } catch (Exception e) {
        }
        return outputDoc;
    }

    /**
	 * Method doc2is.
	 * 
	 * @param inputDoc
	 * @return InputSource
	 */
    public static InputSource doc2is(Document inputDoc) {
        Transformer transformer = null;
        StreamResult xmlOutput = null;
        StringWriter xmlWriter = null;
        try {
            TransformerFactory factory = TransformerFactory.newInstance();
            transformer = factory.newTransformer();
        } catch (Exception e) {
        }
        try {
            xmlWriter = new StringWriter(2048);
            xmlOutput = new StreamResult(xmlWriter);
            transformer.transform(new DOMSource(inputDoc), xmlOutput);
        } catch (Exception e) {
        }
        String tmpxmlString = xmlWriter.toString();
        InputSource is = new InputSource(new StringReader(tmpxmlString));
        return is;
    }

    public static void saveDoc(Document doc, String path) {
        try {
            FileOutputStream stringOut = new FileOutputStream(getRealPath(path));
            OutputFormat format = new OutputFormat(doc, "UTF-8", true);
            XMLSerializer serial = new XMLSerializer(stringOut, format);
            serial.serialize(doc);
        } catch (Exception e) {
        }
    }

    public static void outputNote(String no) {
        String filePath = Utils.getRealPath("/WEB-INF/classes/note/note.txt");
        try {
            PrintWriter pw = new PrintWriter(new BufferedWriter(new FileWriter(filePath, true)));
            pw.println(no);
            pw.close();
        } catch (IOException e) {
        }
    }

    public static void outputNote(int no) {
        String filePath = Utils.getRealPath("/WEB-INF/classes/note/note.txt");
        try {
            PrintWriter pw = new PrintWriter(new BufferedWriter(new FileWriter(filePath, true)));
            pw.println(no);
            pw.close();
        } catch (IOException e) {
        }
    }

    public static Document getNewDocument() {
        DocumentBuilder db = null;
        try {
            DocumentBuilderFactory dbf = DocumentBuilderFactory.newInstance();
            dbf.setNamespaceAware(false);
            dbf.setValidating(false);
            db = dbf.newDocumentBuilder();
        } catch (ParserConfigurationException e) {
        }
        return db.newDocument();
    }

    public static final Document getNewDocument(String rootElementName) {
        Document _doc = getNewDocument();
        _doc.appendChild(_doc.createElement(rootElementName));
        return _doc;
    }

    public static final Element createElementHasText(Document doc, String elementName, String textValue) {
        Element _elm = doc.createElement(elementName);
        _elm.appendChild(doc.createTextNode(textValue));
        return _elm;
    }

    public static final Document parseXmlFromString(String xmlString) throws SAXException {
        ByteArrayInputStream bis = new ByteArrayInputStream(xmlString.getBytes());
        DocumentBuilder builder = null;
        Document doc = null;
        try {
            builder = DocumentBuilderFactory.newInstance().newDocumentBuilder();
            doc = builder.parse(bis);
        } catch (SAXException se) {
            se.printStackTrace();
            throw se;
        } catch (Exception e) {
            log.error(e);
        }
        return doc;
    }

    public static final String escapeXmlString(String str) {
        if (str == null) {
            return null;
        }
        StringBuffer buf = new StringBuffer(str.length() * 2);
        for (int i = 0; i < str.length(); i++) {
            char ch = str.charAt(i);
            switch(ch) {
                case 34:
                    buf.append("&quot;");
                    break;
                case 38:
                    buf.append("&amp;");
                    break;
                case 60:
                    buf.append("&lt;");
                    break;
                case 62:
                    buf.append("&gt;");
                    break;
                default:
                    buf.append(ch);
            }
        }
        return buf.toString();
    }

    public static final String escapeSqlString(String str) {
        if (str == null) {
            return null;
        }
        StringBuffer buf = new StringBuffer(str.length() * 2);
        for (int i = 0; i < str.length(); i++) {
            char ch = str.charAt(i);
            if (ch == '\'') {
                buf.append("\\'");
            } else if (ch == '\\') {
                buf.append("\\\\");
            } else if (ch == '(') {
            } else if (ch == ')') {
            } else if (ch == ';') {
            } else {
                buf.append(ch);
            }
        }
        return buf.toString();
    }

    public static final boolean matchRegexp(String regexp, String value) {
        boolean bool = false;
        if (value == null) {
            return false;
        }
        if (regexp.length() > 0) {
            RE re = null;
            try {
                regexp = regexp.toUpperCase();
                value = value.toUpperCase();
                re = new RE(regexp, RE.MATCH_CASEINDEPENDENT + RE.MATCH_MULTILINE);
                bool = re.match(value);
            } catch (RESyntaxException e) {
            }
        } else {
            bool = true;
        }
        return bool;
    }

    public static final boolean matchKeyword(String keyword, AccessionSearchCriteria searchCriteria, String id, String label, String note) {
        if (keyword == null || keyword.equals("")) return false;
        String value = id + " " + label + " " + note;
        return matchKeyword(keyword, searchCriteria, value);
    }

    public static final boolean matchKeyword(String keyword, AccessionSearchCriteria searchCriteria, String value) {
        if (keyword == null || keyword.equals("")) return false;
        boolean result = false;
        Connection con = null;
        try {
            con = Torque.getConnection();
            String[] targetCols = new String[1];
            targetCols[0] = "\"" + value.replace("\"", "\\\"") + "\"";
            String _sql = "SELECT " + searchCriteria.matchCriteria(keyword, targetCols);
            log.debug(_sql);
            ResultSet rs = con.createStatement().executeQuery(_sql);
            if (rs.next()) {
                int flag = rs.getInt(1);
                if (flag == 1) result = true;
            }
        } catch (Exception e) {
            log.error(e);
            e.printStackTrace();
        } finally {
            Torque.closeConnection(con);
        }
        return result;
    }

    public static final String wordMatchExpression(String keyword) {
        String regex = "";
        if (keyword != null) {
            regex = "(\\W|^)" + keyword + "(\\W|$)";
        }
        return regex;
    }

    public static final String[] regexpSplit(String str, String delim) {
        String[] result = null;
        RE regexp = null;
        if (str != null) {
            try {
                regexp = new RE(delim);
            } catch (RESyntaxException e) {
                log.error(e);
                return result;
            }
            result = regexp.split(str);
        }
        return result;
    }

    /**
	 * ������u��<br>
	 *
	 * @param		source	�Ώە�����
	 * @param		oldStr	����������
	 * @param		newStr	�V����������
	 * @return		�u�����ςݕ�����
	 */
    public static String replace(String source, String oldStr, String newStr) {
        String src = new String(source);
        String dst = "";
        int findIndex = src.indexOf(oldStr);
        while (findIndex >= 0) {
            dst = dst + src.substring(0, findIndex) + newStr;
            src = src.substring(findIndex + oldStr.length());
            findIndex = src.indexOf(oldStr);
        }
        dst = dst + src;
        return dst;
    }

    /**
	 *	�����`�F�b�N�B
	 *
	 *	@param	val	�`�F�b�N����String�I�u�W�F�N�g
	 *	@return	�����̏ꍇtrue�A����ȊO��false
	 */
    public static boolean isNumber(String val) {
        try {
            Integer.valueOf(val).intValue();
        } catch (Exception exp) {
            return (false);
        }
        return (true);
    }

    public static boolean isUrlAvailable(String url) {
        boolean flag = true;
        try {
            URLConnection conn = (new URL(url)).openConnection();
            conn.setConnectTimeout(5000);
            conn.setReadTimeout(5000);
            conn.connect();
            if (conn.getDate() == 0) {
                flag = false;
            }
        } catch (IOException e) {
            log.error(e);
            flag = false;
        }
        return flag;
    }

    public static ArrayList<String> csvToList(String csv) {
        int comma = csv.indexOf(",");
        ArrayList<String> list = new ArrayList<String>();
        if (comma == -1 && csv.length() > 0) {
            list.add(csv);
            return list;
        }
        while (comma > -1) {
            list.add(csv.substring(0, comma));
            csv = csv.substring(comma + 1);
            comma = csv.indexOf(",");
        }
        return list;
    }

    public static String listToCsv(ArrayList list) {
        String csv = "";
        for (int i = 0; i < list.size(); i++) {
            csv = csv + list.get(i);
            if (i != list.size() - 1) {
                csv = csv + ",";
            }
        }
        return csv;
    }
}
