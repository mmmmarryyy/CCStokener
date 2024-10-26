package org.apache.ws.jaxme.xs.junit;

import java.net.URL;
import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.ParserConfigurationException;
import org.apache.ws.jaxme.xs.XSParser;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.NodeList;
import org.xml.sax.InputSource;
import junit.framework.TestCase;

/** <p>A test case running the NIST test suite. To invoke
 * the test suite, set the system property
 * <code>NISTXMLSchemaTestSuite.location</code>.</p>
 * <p>The NIST test suite is delivered as a ZIP file
 * <a href="http://xw2k.sdct.itl.nist.gov/brady/schema/NISTSchemaTests.zip">NISTSchemaTests.zip</a>.
 * For example, if you have extracted this file into
 * <code>c:\jwi\Workspace\ws-jaxme\Nist</code>, then you
 * should set the property to
 * <code>file:///c:/jwi/Workspace/ws-jaxme/Nist/</code>.
 * (Note the trailing slash!)</p>
 *
 * @author <a href="mailto:joe@ispsoft.de">Jochen Wiedmann</a>
 */
public class NISTTest extends TestCase {

    private int numOk;

    private int numFailed;

    private boolean verbose;

    public NISTTest(String pName) {
        super(pName);
    }

    public void setUp() {
        numOk = numFailed = 0;
        verbose = Boolean.valueOf(System.getProperty("verbose")).booleanValue();
    }

    protected void log(String pMessage) {
        if (verbose) {
            System.out.println(pMessage);
        }
    }

    public DocumentBuilder getDocumentBuilder() throws ParserConfigurationException {
        DocumentBuilderFactory dbf = DocumentBuilderFactory.newInstance();
        dbf.setValidating(false);
        dbf.setNamespaceAware(true);
        return dbf.newDocumentBuilder();
    }

    protected void runTest(URL pBaseURL, String pName, String pHref) throws Exception {
        URL url = new URL(pBaseURL, pHref);
        XSParser parser = new XSParser();
        parser.setValidating(false);
        InputSource isource = new InputSource(url.openStream());
        isource.setSystemId(url.toString());
        String result;
        try {
            parser.parse(isource);
            ++numOk;
            result = "Ok";
        } catch (Exception e) {
            ++numFailed;
            result = e.getMessage();
        }
        log("Running test " + pName + " with URL " + url + ": " + result);
    }

    protected void runTests(URL pBaseURL, String pName, String pHref) throws Exception {
        URL url = new URL(pBaseURL, pHref);
        InputSource isource = new InputSource(url.openStream());
        isource.setSystemId(url.toString());
        Document document = getDocumentBuilder().parse(isource);
        NodeList schemas = document.getElementsByTagNameNS(null, "Schema");
        for (int i = 0; i < schemas.getLength(); i++) {
            Element schema = (Element) schemas.item(i);
            runTest(url, schema.getAttribute("name"), schema.getAttribute("href"));
        }
    }

    public void testNIST() throws Exception {
        String p = "NISTXMLSchemaTestSuite.location";
        String v = System.getProperty(p);
        if (v == null || v.length() == 0) {
            System.out.println("System property " + p + " is not set, skipping this test.");
            return;
        }
        URL url = new URL(v);
        url = new URL(url, "NISTXMLSchemaTestSuite.xml");
        InputSource isource = new InputSource(url.openStream());
        isource.setSystemId(url.toString());
        Document document = getDocumentBuilder().parse(isource);
        NodeList links = document.getElementsByTagNameNS(null, "Link");
        for (int i = 0; i < links.getLength(); i++) {
            Element link = (Element) links.item(i);
            runTests(url, link.getAttribute("name"), link.getAttribute("href"));
        }
        System.out.println("Result: Passed = " + numOk + ", Failed = " + numFailed);
    }
}
