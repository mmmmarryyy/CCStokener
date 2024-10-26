package junit.vo;

import junit.framework.*;
import java.io.*;
import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import org.apache.http.*;
import org.apache.http.client.*;
import org.apache.http.client.methods.*;
import org.apache.http.impl.client.*;
import org.apache.http.util.EntityUtils;
import org.w3c.dom.Document;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;
import org.xml.sax.InputSource;

public class VOParseTest extends TestCase {

    private static final String _EXPECTED_TITLE = "Boulder Magnetic Observatory";

    private static final String _EXPECTED_LATITUDE = "49.86";

    private String getXml(String url) throws Exception {
        HttpClient httpclient = new DefaultHttpClient();
        HttpGet httpget = new HttpGet(url);
        HttpResponse response = httpclient.execute(httpget);
        HttpEntity entity = response.getEntity();
        String results = null;
        if (entity != null) {
            long len = entity.getContentLength();
            if (len != -1 && len < 2048) {
                results = EntityUtils.toString(entity);
            } else {
            }
        }
        return (results);
    }

    private Document parseXmlString(String xml) throws Exception {
        DocumentBuilderFactory factory = DocumentBuilderFactory.newInstance();
        DocumentBuilder builder = factory.newDocumentBuilder();
        InputSource is = new InputSource(new StringReader(xml));
        return builder.parse(is);
    }

    private String findNodeContent(Document parsedDocument) {
        NodeList nl = parsedDocument.getChildNodes();
        Node result = nl.item(0);
        NodeList nl2 = result.getChildNodes();
        Node item = nl2.item(3);
        NodeList nl3 = item.getChildNodes();
        Node item2 = nl3.item(1);
        NodeList nl4 = item2.getChildNodes();
        Node item3 = nl4.item(5);
        NodeList nl5 = item3.getChildNodes();
        Node item4 = nl5.item(1);
        return item4.getTextContent();
    }

    private void confirm(String mds, String expected) {
        String field = null;
        try {
            String xml = this.getXml(mds);
            Document d = this.parseXmlString(xml);
            field = findNodeContent(d);
        } catch (Exception x) {
            x.printStackTrace();
        } finally {
            assertTrue("FAIL, unexpected value parsed '" + field + "'", (field != null && field.equals(expected)));
        }
    }

    public void testTitle() {
        confirm("http://spidr.ngdc.noaa.gov/spidrvo/outersearch?sourceUrl=http%3A%2F%2Fspidr.ngdc.noaa.gov%2Fspidrvo%2F&searchAction=outersearch&section=GeomStations&keyCitationTitle=Boulder&output=keyCitationTitle&strictSearch=true", _EXPECTED_TITLE);
    }

    public void testLatitude() {
        confirm("http://spidr.ngdc.noaa.gov/spidrvo/outersearch?sourceUrl=http%3A%2F%2Fspidr.ngdc.noaa.gov%2Fspidrvo%2F&searchAction=outersearch&section=GeomStations&keyCitationTitle=Boulder&output=keySpdomNorth&strictSearch=true", _EXPECTED_LATITUDE);
    }
}
