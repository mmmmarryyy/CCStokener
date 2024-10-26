package cat.udl.eps.esoft3.simplenicescreenscraper;

import java.io.File;
import java.io.IOException;
import java.io.UnsupportedEncodingException;
import java.net.MalformedURLException;
import java.net.URL;
import java.net.URLEncoder;
import java.security.InvalidParameterException;
import javax.xml.transform.Source;
import javax.xml.transform.Transformer;
import javax.xml.transform.TransformerException;
import javax.xml.transform.TransformerFactory;
import javax.xml.transform.dom.DOMSource;
import javax.xml.transform.stream.StreamResult;
import javax.xml.transform.stream.StreamSource;
import javax.xml.xpath.XPath;
import javax.xml.xpath.XPathConstants;
import javax.xml.xpath.XPathExpression;
import javax.xml.xpath.XPathExpressionException;
import javax.xml.xpath.XPathFactory;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.Node;
import org.w3c.tidy.Tidy;

public class SimpleNiceScreenScraper {

    private URL url;

    public SimpleNiceScreenScraper(String url) throws MalformedURLException {
        this.url = new URL(url);
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
	 * Add base tag to header section with base URL scraped
	 * @param doc DOM document to modify
	 * @return Dom document with base tag
	 * @throws XPathExpressionException
	 */
    private Document addBaseElement(Document doc) throws XPathExpressionException {
        XPathFactory factory = XPathFactory.newInstance();
        XPath xpath = factory.newXPath();
        Node headNode = (Node) xpath.evaluate("/html/head", doc, XPathConstants.NODE);
        if (headNode != null) {
            Element baseElement = doc.createElement("base");
            baseElement.appendChild(doc.createTextNode(url.getProtocol() + "://" + url.getHost()));
            headNode.appendChild(baseElement);
            XPathExpression expression = xpath.compile("/html/head/base/text()");
            expression.evaluate(doc, XPathConstants.STRING).toString();
        }
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
    private File transformDocument(Document doc) throws XPathExpressionException, UnsupportedEncodingException, TransformerException {
        String outputName = "default";
        XPathFactory factory = XPathFactory.newInstance();
        XPath xpath = factory.newXPath();
        XPathExpression expression = xpath.compile("//div[@id='path']/ul/li[2]/text()");
        Object resultEvaluated = expression.evaluate(doc, XPathConstants.STRING);
        if (resultEvaluated != null) {
            outputName = (String) resultEvaluated;
        }
        File stylesheet = new File("servicaixa.xsl");
        File output = new File(URLEncoder.encode(outputName, "utf-8") + ".xml");
        Source source = new DOMSource(doc);
        StreamSource stylesource = new StreamSource(stylesheet);
        TransformerFactory tf = TransformerFactory.newInstance();
        Transformer transformer = tf.newTransformer(stylesource);
        StreamResult resultFile = new StreamResult(output);
        transformer.transform(source, resultFile);
        return output;
    }

    public void scrapeURL() {
        try {
            Document doc = wrapHTMLtoXHTML();
            doc = addBaseElement(doc);
            File file = transformDocument(doc);
            System.out.println("XML file generated in: " + file.getName());
        } catch (IOException e) {
            System.out.println("File/URL not found Exception: " + e.getMessage());
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    public static void main(String[] args) {
        String usage = "Usage: java -cp . " + SimpleNiceScreenScraper.class.getCanonicalName() + " URL" + "\nExample: java -cp . " + SimpleNiceScreenScraper.class.getCanonicalName() + " http://www.google.com";
        try {
            if (args.length > 0) {
                SimpleNiceScreenScraper scraper = new SimpleNiceScreenScraper(args[0]);
                scraper.scrapeURL();
            } else {
                throw new InvalidParameterException("Error getting URL: Invalid parameter number!");
            }
        } catch (InvalidParameterException e) {
            System.out.println(e.getMessage());
            System.out.println(usage);
        } catch (Exception e) {
            e.printStackTrace();
        }
    }
}
