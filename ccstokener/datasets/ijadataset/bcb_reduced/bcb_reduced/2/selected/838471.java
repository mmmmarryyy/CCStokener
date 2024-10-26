package org.artags.android.app.ar;

import org.artags.android.app.tag.TagParser;
import android.util.Log;
import java.net.URL;
import java.util.List;
import javax.xml.parsers.SAXParser;
import javax.xml.parsers.SAXParserFactory;
import org.artags.android.app.Security;
import org.xml.sax.InputSource;
import org.xml.sax.XMLReader;

/**
 *
 * @author Pierre Levy
 */
public class POIService {

    /**
     * Gets a list of POI
     * @param lat The latitude
     * @param lon The longitude
     * @param maxPOIs Max POI
     * @return The list
     */
    public static List<GenericPOI> getPOIs(double lat, double lon, int maxPOIs) {
        List<GenericPOI> list = null;
        try {
            URL url = new URL(buildUrl(lat, lon, maxPOIs));
            SAXParserFactory spf = SAXParserFactory.newInstance();
            SAXParser sp = spf.newSAXParser();
            XMLReader xr = sp.getXMLReader();
            TagParser parser = new TagParser();
            xr.setContentHandler(parser);
            xr.parse(new InputSource(url.openStream()));
            list = parser.getGenericPOIs();
        } catch (Exception e) {
            Log.e("ARTags", "POIService", e);
        }
        return list;
    }

    /**
     * Build the url
     * @param lat The latitude
     * @param lon The longitude
     * @param maxPOIs Max POI
     * @return The url
     */
    public static String buildUrl(double lat, double lon, int maxPOIs) {
        String url = Security.URL_TAGS;
        url += "&lat=" + lat;
        url += "&lon=" + lon;
        url += "&max=" + maxPOIs;
        return url;
    }
}
