import java.net.URL;
import org.apache.xalan.xsltc.trax.SAX2DOM;
import org.apache.xpath.XPathAPI;
import org.apache.xpath.objects.XObject;
import org.ccil.cowan.tagsoup.Parser;
import org.w3c.dom.Node;
import org.xml.sax.InputSource;

public class m {

    public static final void main(String[] args) throws Exception {
        URL url = new URL("http://example.com");
        Parser p = new Parser();
        SAX2DOM sax2dom = new SAX2DOM();
        p.setContentHandler(sax2dom);
        p.parse(new InputSource(url.openStream()));
        Node doc = sax2dom.getDOM();
        String titlePath = "/html:html/html:head/html:title";
        XObject title = XPathAPI.eval(doc, titlePath);
        System.out.println("Title is '" + title + "'");
    }
}
