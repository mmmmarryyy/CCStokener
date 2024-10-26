package dbaccess.geom;

import wdc.settings.*;
import java.io.*;
import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import org.apache.http.*;
import org.apache.http.client.*;
import org.apache.http.client.methods.*;
import org.apache.http.impl.client.*;
import org.apache.http.util.EntityUtils;
import org.apache.log4j.Logger;
import org.w3c.dom.Document;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;
import org.xml.sax.InputSource;

/**
 * This class creates an abstraction for accessing SPIDRVO's metadata for particular 
 * metadata components of Geomagnetic Observatories.
 * 
 * It allows developers to access these piecewise bits of information through method calls
 * rather than having to continually parse XML or HTML to get them, since it does those things
 * for you.
 *
 */
public class GeomVirtualObservatory {

    private static final Logger _log = Logger.getLogger(GeomExportHrlyIAGA.class);

    private String _metadataSearchPrefix;

    public GeomVirtualObservatory() {
        _metadataSearchPrefix = Settings.get("locations.metadataSearch");
    }

    /**
	 * Takes a parameters string and creates a fully formed URL necessary to make 
	 * a spidrvo call
	 * 
	 * @param parameters
	 */
    private String buildUrl(String parameters) {
        String postFix = _metadataSearchPrefix;
        String suffix = "outersearch?sourceUrl=";
        postFix = postFix.replaceAll("/", "%2F");
        postFix = postFix.replaceAll(":", "%3A");
        suffix += postFix + parameters;
        return suffix;
    }

    /**
	 * Stubbed out until VO has this metadata somewhere
	 * 
	 * @param stationCode
	 * @return
	 */
    public String getElevation(String stationCode) {
        return ("");
    }

    /**
	 * Stubbed out until VO has this metadata somewhere
	 * 
	 * @param stationCode
	 * @return
	 */
    public String getSamplingRate(String stationCode) {
        return ("");
    }

    /**
	 * @return originator or null
	 */
    public String getOriginator(String stationCode) {
        String parameters = "&searchAction=outersearch&section=GeomStations&keyStationCode=" + stationCode + "&output=keyCitationOrigin&strictSearch=true";
        return findNodeContent(parseXmlString(getXml(buildUrl(parameters))));
    }

    /**
	 * @return station name or null
	 */
    public String getStationName(String stationCode) {
        String parameters = "&searchAction=outersearch&section=GeomStations&keyStationCode=" + stationCode + "&output=keyCitationTitle&strictSearch=true";
        return findNodeContent(parseXmlString(getXml(buildUrl(parameters))));
    }

    /**
	 * @return latitude or null
	 */
    public String getLatitude(String stationCode) {
        String parameters = "&searchAction=outersearch&section=GeomStations&keyStationCode=" + stationCode + "&output=keySpdomNorth&strictSearch=true";
        return findNodeContent(parseXmlString(getXml(buildUrl(parameters))));
    }

    /**
	 * @return longitude or null
	 */
    public String getLongitude(String stationCode) {
        String parameters = "&searchAction=outersearch&section=GeomStations&keyStationCode=" + stationCode + "&output=keySpdomWest&strictSearch=true";
        return findNodeContent(parseXmlString(getXml(buildUrl(parameters))));
    }

    /**
	 * given a url, return associated spidrvo xml content
	 * 
	 * @param url - where to access spidrvo
	 * @return
	 */
    private String getXml(String url) {
        String results = null;
        try {
            url = _metadataSearchPrefix + url;
            HttpClient httpclient = new DefaultHttpClient();
            HttpGet httpget = new HttpGet(url);
            HttpResponse response = httpclient.execute(httpget);
            HttpEntity entity = response.getEntity();
            if (entity != null) {
                long len = entity.getContentLength();
                if (len != -1 && len < 2048) {
                    results = EntityUtils.toString(entity);
                } else {
                }
            }
        } catch (IOException iox) {
            iox.printStackTrace();
            _log.error("unable to process metadata from spidrvo");
        }
        return results;
    }

    /**
	 * given an xml document/string, parse and return a Document (SAX/DomBuilder in javax)
	 * 
	 * @param xml - the content to parse
	 * @return the document
	 */
    private Document parseXmlString(String xml) {
        Document result = null;
        try {
            DocumentBuilderFactory factory = DocumentBuilderFactory.newInstance();
            DocumentBuilder builder = factory.newDocumentBuilder();
            InputSource is = new InputSource(new StringReader(xml));
            result = builder.parse(is);
        } catch (Exception x) {
            x.printStackTrace();
            _log.error("unable to parse xml from spidrvo");
        }
        return result;
    }

    /**
	 * given a Document object, parse only the needed piece
	 * this method makes assumptions relative to the structure of xml stored
	 * in spidrvo
	 * 
	 * @param parsedDocument
	 * @return
	 */
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
}
