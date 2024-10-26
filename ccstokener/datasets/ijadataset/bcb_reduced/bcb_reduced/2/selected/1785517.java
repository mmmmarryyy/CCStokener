package jomm.utils;

import org.dom4j.Document;
import org.dom4j.DocumentException;
import org.dom4j.io.*;
import org.xml.sax.InputSource;
import org.xml.sax.ErrorHandler;
import org.xml.sax.SAXParseException;
import org.xml.sax.SAXException;
import javax.xml.transform.*;
import javax.xml.transform.stream.StreamResult;
import javax.xml.transform.stream.StreamSource;
import java.util.Map;
import java.util.Hashtable;
import java.io.*;
import java.net.MalformedURLException;
import java.net.URL;
import java.net.URLConnection;
import java.net.HttpURLConnection;

/**
 * @author Jorge Machado
 * @date 6/Nov/2009
 * @time 23:39:18
 * @email machadofisher@gmail.com
 */
public class XmlUtils {

    public static String escape(String text) {
        if (text == null) return null;
        StringBuilder stringBuilder = new StringBuilder();
        for (int i = 0; i < text.length(); i++) {
            if (text.charAt(i) == '<') {
                stringBuilder.append("&lt;");
            } else if (text.charAt(i) == '>') {
                stringBuilder.append("&gt;");
            } else if (text.charAt(i) == '&') {
                stringBuilder.append("&amp;");
            } else {
                stringBuilder.append(text.charAt(i));
            }
        }
        return stringBuilder.toString();
    }

    public static Document styleDocument(Document document, String stylesheet, boolean xslInPath, Map<String, Object> parameters) throws Exception {
        Transformer transformer = XmlUtils.getTransformer(stylesheet, xslInPath);
        if (parameters != null) {
            for (Map.Entry<String, Object> entry : parameters.entrySet()) {
                transformer.setParameter(entry.getKey(), entry.getValue());
            }
        }
        DocumentSource source = new DocumentSource(document);
        DocumentResult result = new DocumentResult();
        transformer.transform(source, result);
        return result.getDocument();
    }

    public static void styleDocument(Document document, String stylesheet, boolean xslInPath, Map<String, Object> parameters, OutputStream out) throws Exception {
        Transformer transformer = XmlUtils.getTransformer(stylesheet, xslInPath);
        if (parameters != null) {
            for (Map.Entry<String, Object> entry : parameters.entrySet()) {
                transformer.setParameter(entry.getKey(), entry.getValue());
            }
        }
        transformer.setOutputProperty(OutputKeys.OMIT_XML_DECLARATION, "yes");
        DocumentSource source = new DocumentSource(document);
        StreamResult sresult = new StreamResult(out);
        transformer.transform(source, sresult);
    }

    public static void write(Document document, Writer writerStream) throws IOException {
        XMLWriter writer = new XMLWriter(writerStream);
        writer.write(document);
        writer.close();
    }

    public static void write(Document document, Writer writerStream, String encoding) throws IOException {
        OutputFormat ou = new OutputFormat();
        ou.setEncoding(encoding);
        XMLWriter writer = new XMLWriter(writerStream, ou);
        writer.write(document);
        writer.close();
    }

    private static TransformerFactory transFact = TransformerFactory.newInstance();

    private static Map<String, Templates> templates = new Hashtable<String, Templates>();

    private static Transformer getTransformer(String xsltFile, boolean inClassPath) throws TransformerConfigurationException, IOException {
        Templates tpl = XmlUtils.templates.get(xsltFile);
        if (tpl == null) {
            InputStream stream;
            if (inClassPath) stream = Thread.currentThread().getContextClassLoader().getResourceAsStream(xsltFile); else stream = new FileInputStream(xsltFile);
            Source xsltSource = new StreamSource(stream);
            tpl = XmlUtils.transFact.newTemplates(xsltSource);
            XmlUtils.templates.put(xsltFile, tpl);
        }
        return tpl.newTransformer();
    }

    public static Document parse(InputSource source) throws DocumentException, MalformedURLException {
        SAXReader reader = new SAXReader();
        XmlUtils.createIgnoreErrorHandler(reader);
        return reader.read(source);
    }

    public static Document parse(InputStream stream, String encoding) throws DocumentException, MalformedURLException {
        InputSource inputSource = new InputSource(stream);
        inputSource.setEncoding(encoding);
        SAXReader reader = new SAXReader();
        XmlUtils.createIgnoreErrorHandler(reader);
        return reader.read(inputSource);
    }

    public static Document parse(InputStream stream) throws DocumentException, MalformedURLException {
        InputSource inputSource = new InputSource(stream);
        SAXReader reader = new SAXReader();
        XmlUtils.createIgnoreErrorHandler(reader);
        return reader.read(inputSource);
    }

    public static void writeSout(Document document) throws IOException {
        OutputFormat format = OutputFormat.createCompactFormat();
        XMLWriter writer = new XMLWriter(System.out, format);
        writer.write(document);
    }

    public static Document parse(String xml) throws DocumentException {
        SAXReader reader = new SAXReader();
        XmlUtils.createIgnoreErrorHandler(reader);
        return reader.read(new StringReader(xml));
    }

    public static Document parse(URL url) throws DocumentException, IOException {
        URLConnection urlConnection;
        DataInputStream inStream;
        urlConnection = url.openConnection();
        ((HttpURLConnection) urlConnection).setRequestMethod("GET");
        urlConnection.setDoInput(true);
        urlConnection.setDoOutput(false);
        urlConnection.setUseCaches(false);
        inStream = new DataInputStream(urlConnection.getInputStream());
        byte[] bytes = new byte[1024];
        int read;
        StringBuilder builder = new StringBuilder();
        while ((read = inStream.read(bytes)) >= 0) {
            String readed = new String(bytes, 0, read, "UTF-8");
            builder.append(readed);
        }
        SAXReader reader = new SAXReader();
        XmlUtils.createIgnoreErrorHandler(reader);
        Document dom = reader.read(new StringReader(builder.toString()));
        inStream.close();
        return dom;
    }

    public static void createIgnoreErrorHandler(SAXReader reader) {
        reader.setValidation(false);
        reader.setErrorHandler(new ErrorHandler() {

            public void warning(SAXParseException exception) throws SAXException {
            }

            public void error(SAXParseException exception) throws SAXException {
            }

            public void fatalError(SAXParseException exception) throws SAXException {
            }
        });
    }

    public static void write(Document document, OutputStream stream) throws IOException {
        OutputFormat outputFormat = OutputFormat.createPrettyPrint();
        outputFormat.setIndent(true);
        outputFormat.setEncoding("UTF-8");
        XMLWriter writer = new XMLWriter(stream, outputFormat);
        writer.write(document);
        writer.close();
    }
}
