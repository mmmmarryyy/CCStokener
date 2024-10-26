package cn.edu.bit.whitesail.parser;

import java.io.IOException;
import java.io.InputStream;
import java.net.MalformedURLException;
import java.net.URL;
import org.cyberneko.html.HTMLConfiguration;
import org.w3c.dom.Attr;
import org.w3c.dom.CDATASection;
import org.w3c.dom.Comment;
import org.w3c.dom.DOMConfiguration;
import org.w3c.dom.DOMException;
import org.w3c.dom.DOMImplementation;
import org.w3c.dom.Document;
import org.w3c.dom.DocumentFragment;
import org.w3c.dom.DocumentType;
import org.w3c.dom.Element;
import org.w3c.dom.EntityReference;
import org.w3c.dom.NamedNodeMap;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;
import org.w3c.dom.ProcessingInstruction;
import org.w3c.dom.Text;
import org.w3c.dom.UserDataHandler;
import org.w3c.dom.html.HTMLCollection;
import org.w3c.dom.html.HTMLDocument;
import org.w3c.dom.html.HTMLElement;
import org.xml.sax.InputSource;
import org.xml.sax.SAXException;
import org.xml.sax.SAXNotRecognizedException;
import org.xml.sax.SAXNotSupportedException;

/**
 *
 * 
 * @author baifan
 * @since 
 */
public class MyDOMParser extends org.apache.xerces.parsers.DOMParser {

    /** Error reporting feature identifier. */
    private static final String REPORT_ERRORS = "http://cyberneko.org/html/features/report-errors";

    /** Augmentations feature identifier. */
    private static final String AUGMENTATIONS = "http://cyberneko.org/html/features/augmentations";

    /** Filters property identifier. */
    private static final String FILTERS = "http://cyberneko.org/html/properties/filters";

    /** Element case settings. possible values: "upper", "lower", "match" */
    private static final String TAG_NAME_CASE = "http://cyberneko.org/html/properties/names/elems";

    /** Attribute case settings. possible values: "upper", "lower", "no-change" */
    private static final String ATTRIBUTE_NAME_CASE = "http://cyberneko.org/html/properties/names/attrs";

    private MyDOMParser(HTMLConfiguration configuration) {
        super(configuration);
    }

    public static MyDOMParser createDOMParser() throws SAXNotRecognizedException, SAXNotSupportedException {
        final HTMLConfiguration configuration = new HTMLConfiguration();
        MyDOMParser parser = new MyDOMParser(configuration);
        parser.setProperty(DOCUMENT_CLASS_NAME, "");
        parser.setFeature("http://xml.org/sax/features/namespaces", false);
        return parser;
    }

    public static void main(String[] args) throws MalformedURLException, IOException, SAXException {
        URL url = new URL("http://www.google.com");
        InputStream in = url.openConnection().getInputStream();
        MyDOMParser parser = createDOMParser();
        parser.parse(new InputSource(in));
        HTMLDocument document = (HTMLDocument) parser.getDocument();
        in.close();
        HTMLCollection link = document.getLinks();
        int length = link.getLength();
        for (int i = 0; i < length; i++) {
            System.out.println(i + ": " + link.item(i).getAttributes().getNamedItem("href"));
        }
        System.out.println(length);
    }
}
