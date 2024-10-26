package iwant.util.gmap;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.URL;
import java.net.URLEncoder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.ParserConfigurationException;
import javax.xml.xpath.XPath;
import javax.xml.xpath.XPathConstants;
import javax.xml.xpath.XPathExpressionException;
import javax.xml.xpath.XPathFactory;
import org.w3c.dom.Document;
import org.w3c.dom.NodeList;
import org.xml.sax.InputSource;
import org.xml.sax.SAXException;

public class GeocodeProcessor {

    private static final String GEOCODE_REQUEST_PREFIX = "http://maps.google.com/maps/api/geocode/xml";

    private static final String GEOCODE_QUERY = "New+York,+NY";

    private static String XPATH_EXPRESSION = "//text()";

    public String _xpath = null;

    public Document _xml = null;

    public static void main(String[] args) throws IOException, ParserConfigurationException, SAXException {
        BufferedReader input = new BufferedReader(new InputStreamReader(System.in));
        String inputQuery, urlString, xPathString = null;
        System.out.println("Enter the Geocode Request (default is 'New York, NY'): ");
        inputQuery = input.readLine();
        if (inputQuery.equals("")) {
            inputQuery = GEOCODE_QUERY;
        }
        urlString = GEOCODE_REQUEST_PREFIX + "?address=" + URLEncoder.encode(inputQuery, "UTF-8") + "&sensor=false";
        System.out.println(urlString);
        URL url = new URL(urlString);
        System.out.println("Enter the XPath expression to evaluate the response (default is '//text()'): ");
        xPathString = input.readLine();
        if (xPathString.equals("")) {
            xPathString = XPATH_EXPRESSION;
        }
        HttpURLConnection conn = (HttpURLConnection) url.openConnection();
        Document geocoderResultDocument = null;
        try {
            conn.connect();
            InputSource geocoderResultInputSource = new InputSource(conn.getInputStream());
            geocoderResultDocument = DocumentBuilderFactory.newInstance().newDocumentBuilder().parse(geocoderResultInputSource);
        } finally {
            conn.disconnect();
        }
        NodeList nodes = process(geocoderResultDocument, xPathString);
        for (int i = 0; i < nodes.getLength(); i++) {
            String nodeString = nodes.item(i).getTextContent();
            System.out.print(nodeString);
            System.out.print("\n");
        }
    }

    public static NodeList process(Document xml, String xPathString) {
        NodeList result = null;
        System.out.print("Geocode Processor 1.0\n");
        XPathFactory factory = XPathFactory.newInstance();
        XPath xpath = factory.newXPath();
        try {
            result = (NodeList) xpath.evaluate(xPathString, xml, XPathConstants.NODESET);
        } catch (XPathExpressionException ex) {
        }
        return result;
    }
}
