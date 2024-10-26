package net.jwde.extractor;

import java.io.*;
import java.net.*;
import net.jwde.util.XMLHelper;
import net.jwde.util.XMLHelperException;
import org.apache.log4j.Logger;
import org.jdom.*;
import org.jdom.input.*;
import org.w3c.tidy.*;

/**
 * This class is used to transform a HTML document to XHTML or XML (using XSLT).
 * Since HTML document may be poorly formed a proper document can't be
 * constructed. Thus HTML can't be treated as a document. *
 */
public class HTMLReader extends GenericReader {

    private static Logger log = Logger.getLogger("net.jwde.extractor.HTMLReader");

    private static GenericReader instance = new HTMLReader();

    public Document transform(URL url) throws IOException {
        Document doc = null;
        try {
            InputStream in = url.openStream();
            ByteArrayOutputStream baos = new ByteArrayOutputStream();
            Tidy tidy = new Tidy();
            tidy.setShowWarnings(false);
            tidy.setXmlOut(true);
            tidy.setXmlPi(false);
            tidy.setDocType("auto");
            tidy.setXHTML(false);
            tidy.setRawOut(true);
            tidy.setNumEntities(true);
            tidy.setQuiet(true);
            tidy.setFixComments(true);
            tidy.setIndentContent(true);
            tidy.setCharEncoding(org.w3c.tidy.Configuration.ASCII);
            DOMBuilder docBuilder = new DOMBuilder();
            doc = docBuilder.build(tidy.parseDOM(in, baos));
            String result = "<?xml version=\"1.0\" encoding=\"UTF-8\"?>\n" + baos.toString();
            in.close();
            baos.close();
            doc = XMLHelper.parseXMLFromString(result);
        } catch (IOException ioEx) {
            throw ioEx;
        } catch (XMLHelperException xmlEx) {
            xmlEx.printStackTrace();
        }
        return doc;
    }

    public Document transform(String urlString) throws IOException {
        Document doc = null;
        try {
            URL inputURL = new URL(urlString);
            doc = transform(inputURL);
        } catch (MalformedURLException urlEx) {
            urlEx.printStackTrace();
        } catch (IOException ioEx) {
            ioEx.printStackTrace();
            throw ioEx;
        }
        return doc;
    }

    public static void register() {
        ExtractorManagerImpl.registerGeneric(instance, "text/html");
    }

    public static GenericReader getInstance() {
        return instance;
    }
}
