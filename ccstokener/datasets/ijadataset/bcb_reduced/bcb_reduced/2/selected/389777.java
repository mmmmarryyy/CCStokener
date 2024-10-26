package algutil.parser;

import java.io.BufferedReader;
import java.io.FileReader;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.net.URL;
import java.net.URLConnection;
import javax.swing.tree.TreeNode;
import org.apache.log4j.Logger;
import org.apache.log4j.xml.DOMConfigurator;
import algutil.parser.exception.HTMLFileParserException;

public class HTMLFileParser {

    private static final Logger log = Logger.getLogger(HTMLFileParser.class);

    private String allDocInOneLine;

    public HTMLFileParser(String url) throws Exception {
        BufferedReader br = null;
        InputStream httpStream = null;
        if (url.startsWith("http")) {
            URL fileURL = new URL(url);
            URLConnection urlConnection = fileURL.openConnection();
            httpStream = urlConnection.getInputStream();
            br = new BufferedReader(new InputStreamReader(httpStream, "ISO-8859-1"));
        } else {
            br = new BufferedReader(new FileReader(url));
        }
        StringBuffer sbAllDoc = new StringBuffer();
        String ligne = null;
        while ((ligne = br.readLine()) != null) {
            sbAllDoc.append(ligne + " ");
        }
        allDocInOneLine = sbAllDoc.toString();
    }

    public void parseBodyOnly() throws HTMLFileParserException {
        if (allDocInOneLine.indexOf("<body") == -1) {
            throw new HTMLFileParserException("Pas de balise <body> dans le document");
        }
        if (allDocInOneLine.indexOf("</body>") == -1) {
            throw new HTMLFileParserException("Pas de balise </body> dans le document");
        }
        String bodyDoc = allDocInOneLine.substring(allDocInOneLine.indexOf("<body>"), allDocInOneLine.indexOf("</body>") + 7);
        while (bodyDoc.indexOf("\t") > 0) {
            bodyDoc = bodyDoc.replaceAll("\t", " ");
        }
        while (bodyDoc.indexOf("  ") > 0) {
            bodyDoc = bodyDoc.replaceAll("  ", " ");
        }
        log.info(bodyDoc);
        Balise body = new Balise();
        body.parse(bodyDoc);
    }

    public void cleanForMA() {
        allDocInOneLine = allDocInOneLine.replace("<body >", "<body>");
    }

    public static void main(String[] args) throws Exception {
        DOMConfigurator.configure("conf/log4j.xml");
        HTMLFileParser h = new HTMLFileParser("d:/tmp/test.html");
        h.cleanForMA();
        h.parseBodyOnly();
    }
}
