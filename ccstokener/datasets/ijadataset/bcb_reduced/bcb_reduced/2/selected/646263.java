package parser.htmlParser;

import java.io.InputStream;
import java.io.UnsupportedEncodingException;
import java.net.URL;
import java.net.URLEncoder;
import java.util.List;
import net.htmlparser.jericho.Attribute;
import net.htmlparser.jericho.Element;
import net.htmlparser.jericho.HTMLElementName;
import net.htmlparser.jericho.OutputDocument;
import net.htmlparser.jericho.Source;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

public class JerichoHMTLProxifier {

    private static Log logger = LogFactory.getLog(JerichoHMTLProxifier.class);

    static String proxy = "http://domain.com/proxy/";

    public static void main(String[] args) throws Exception {
        String url = "http://google.com";
        InputStream input = new URL(url).openStream();
        new JerichoHMTLProxifier().doIt(input);
    }

    String doIt(InputStream input) throws Exception {
        Source source = new Source(input);
        source.fullSequentialParse();
        OutputDocument outputDocument = new OutputDocument(source);
        proxyAnchor(source, outputDocument);
        proxyForm(source, outputDocument);
        String proxified = outputDocument.toString();
        logger.info(proxified);
        return proxified;
    }

    void proxyAnchor(Source source, OutputDocument outputDocument) {
        List<Element> elements = source.getAllElements(HTMLElementName.A);
        for (Element element : elements) {
            Attribute hrefAttribute = element.getStartTag().getAttributes().get("href");
            String href = hrefAttribute.getValue();
            String proxyTemp = getProxy(href);
            outputDocument.replace(hrefAttribute.getValueSegment(), proxyTemp);
            logger.info("Proxified " + href + " to " + proxyTemp);
        }
    }

    void proxyForm(Source source, OutputDocument outputDocument) {
        List<Element> elements = source.getAllElements(HTMLElementName.FORM);
        for (Element element : elements) {
            Attribute actionAttribute = element.getStartTag().getAttributes().get("action");
            String actionValue = actionAttribute.getValue();
            String proxyTemp = getProxy(actionValue);
            outputDocument.replace(actionAttribute.getValueSegment(), proxyTemp);
            logger.info("Proxified " + actionValue + " to " + proxyTemp);
        }
    }

    private String getProxy(String path) {
        String proxyTemp;
        try {
            proxyTemp = proxy + URLEncoder.encode(path, "UTF-8");
        } catch (UnsupportedEncodingException e) {
            throw new RuntimeException(e);
        }
        return proxyTemp;
    }
}
