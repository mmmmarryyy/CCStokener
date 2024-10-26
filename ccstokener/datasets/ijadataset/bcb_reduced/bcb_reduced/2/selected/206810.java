package cat.udl.eps.esoft3.nicescreenscraper.server;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.StringReader;
import java.io.UnsupportedEncodingException;
import java.net.MalformedURLException;
import java.net.URL;
import javax.xml.transform.Source;
import javax.xml.transform.Transformer;
import javax.xml.transform.TransformerException;
import javax.xml.transform.TransformerFactory;
import javax.xml.transform.dom.DOMSource;
import javax.xml.transform.stream.StreamResult;
import javax.xml.transform.stream.StreamSource;
import javax.xml.xpath.XPathExpressionException;
import org.w3c.dom.Document;
import org.w3c.tidy.Tidy;

public class NiceScreenScraperEngine {

    private URL url;

    private String transformation;

    public NiceScreenScraperEngine(String url, String transformation) throws MalformedURLException {
        this.url = new URL(url);
        this.transformation = transformation;
    }

    /**
	 * Get URL HTML source and convert it to XHTML
	 * @return DOM document 
	 * @throws IOException 
	 */
    private Document wrapHTMLtoXHTML() throws IOException {
        Document doc = null;
        Tidy tidy = new Tidy();
        tidy.setQuiet(true);
        tidy.setXHTML(true);
        tidy.setAltText("none");
        tidy.setOnlyErrors(true);
        tidy.setShowWarnings(false);
        tidy.setInputEncoding("utf-8");
        doc = tidy.parseDOM(url.openStream(), null);
        return doc;
    }

    /**
	 * Parse XHTML document with XSL rules
	 * @param doc DOM document to be parsed
	 * @return XML file generated
	 * @throws XPathExpressionException
	 * @throws UnsupportedEncodingException
	 * @throws TransformerException
	 */
    private String transformDocument(Document doc) throws XPathExpressionException, UnsupportedEncodingException, TransformerException, IOException, Exception {
        ByteArrayOutputStream output = new ByteArrayOutputStream();
        Source source = new DOMSource(doc);
        StreamSource stylesource = new StreamSource(new StringReader(transformation));
        TransformerFactory tf = TransformerFactory.newInstance("org.apache.xalan.processor.TransformerFactoryImpl", Thread.currentThread().getContextClassLoader());
        Transformer transformer = tf.newTransformer(stylesource);
        StreamResult result = new StreamResult(output);
        transformer.transform(source, result);
        return output.toString();
    }

    public String scrapeURL() throws Exception {
        String result = null;
        try {
            Document doc = wrapHTMLtoXHTML();
            result = transformDocument(doc);
        } catch (IOException e) {
            System.out.println("File/URL not found Exception: " + e.getMessage());
            e.printStackTrace();
            throw e;
        } catch (Exception e) {
            e.printStackTrace();
            throw e;
        }
        return result;
    }
}
