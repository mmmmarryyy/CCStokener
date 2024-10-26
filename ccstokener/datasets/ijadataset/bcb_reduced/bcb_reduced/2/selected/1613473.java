package ubibook;

import java.io.*;
import java.net.*;
import java.util.*;
import javax.xml.parsers.*;
import org.xml.sax.SAXException;
import org.xml.sax.EntityResolver;
import org.xml.sax.InputSource;
import org.w3c.dom.*;

/**
 * Describe class <code>DOMResource</code> here.
 *
 * @author <a href="mailto:kleiba@dfki.de">Thomas Kleinbauer</a>
 * @version 1.0
 */
public class DOMResource extends AbstractResource {

    protected Document document;

    public DOMResource(URL url) throws EmbedException, URISyntaxException, IOException, SAXException, ParserConfigurationException {
        this(readDocument(url.openStream()), url.toURI());
    }

    public DOMResource(File file) throws EmbedException, IOException, SAXException, ParserConfigurationException {
        this(readDocument(new FileInputStream(file)), file.toURI());
    }

    public DOMResource(Document document, URI uri) throws EmbedException {
        super(uri);
        this.document = document;
        try {
            javax.xml.transform.TransformerFactory factory = javax.xml.transform.TransformerFactory.newInstance();
            javax.xml.transform.Transformer transformer = factory.newTransformer();
            transformer.setOutputProperty(javax.xml.transform.OutputKeys.METHOD, "xml");
            transformer.setOutputProperty(javax.xml.transform.OutputKeys.INDENT, "yes");
            transformer.setOutputProperty("{http://xml.apache.org/xslt}indent-amount", "2");
            transformer.transform(new javax.xml.transform.dom.DOMSource(document), new javax.xml.transform.stream.StreamResult(System.out));
        } catch (Throwable t) {
            t.printStackTrace();
        }
        Queue<Node> queue = new LinkedList<Node>();
        queue.offer(document.getDocumentElement());
        while (!queue.isEmpty()) {
            Node node = queue.poll();
            if (node instanceof Element) {
                Element element = (Element) node;
                String src = element.getAttribute("src");
                if (src != null && !src.trim().equals("")) {
                    try {
                        embedResource(element.getTagName().toLowerCase(), uri.resolve(src).toURL());
                    } catch (FormatException fe) {
                        throw new EmbedException(fe);
                    } catch (IOException ioe) {
                        throw new EmbedException(ioe);
                    } catch (URISyntaxException use) {
                        throw new EmbedException(use);
                    }
                }
            }
            NodeList children = node.getChildNodes();
            for (int i = children.getLength() - 1; i >= 0; i--) {
                queue.offer(children.item(i));
            }
        }
    }

    private static Document readDocument(InputStream in) throws IOException, SAXException, ParserConfigurationException {
        DocumentBuilderFactory factory = DocumentBuilderFactory.newInstance();
        factory.setValidating(false);
        factory.setFeature("http://apache.org/xml/features/nonvalidating/load-external-dtd", false);
        DocumentBuilder documentBuilder = factory.newDocumentBuilder();
        return documentBuilder.parse(in);
    }

    public String getMediaType() {
        return "application";
    }

    public Document getDocument() {
        return document;
    }

    private void embedResource(String type, URL url) throws IOException, URISyntaxException, FormatException {
        if (type.equals("img")) {
            embeddedResources.add(new ImageResource(url));
        } else {
            throw new FormatException("Can't embed element of type '" + type + "'");
        }
    }

    public static void main(String[] args) throws Exception {
        if (args.length != 1) {
            System.err.println("USAGE: java ubibook.DOMResource <xml-file>");
            System.exit(1);
        }
        new DOMResource(new File(args[0]));
    }
}
