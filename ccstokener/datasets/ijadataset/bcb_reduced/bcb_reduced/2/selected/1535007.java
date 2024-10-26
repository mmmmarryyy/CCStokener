package com.ray.project.ndbc;

import org.xml.sax.InputSource;
import org.xml.sax.XMLReader;
import com.ray.project.oceanicbuoy.OceanicBuoy;
import java.net.URL;
import java.util.ArrayList;
import javax.xml.parsers.SAXParser;
import javax.xml.parsers.SAXParserFactory;

public class OceanicBuoyDataGetter {

    private static final String URL = "http://www.ndbc.noaa.gov/rss/ndbc_obs_search.php?";

    private static final String URLLATITUDE = "lat=";

    private static final String URLLONGITUDE = "&lon=";

    private static final String URLRADIUS = "&radius=";

    private String url;

    private String latitude;

    private String longitude;

    private String radius;

    public OceanicBuoyDataGetter(String lat, String lon, String rad) {
        this.latitude = lat;
        this.longitude = lon;
        this.radius = rad;
        this.url = OceanicBuoyDataGetter.URL + OceanicBuoyDataGetter.URLLATITUDE + this.latitude + OceanicBuoyDataGetter.URLLONGITUDE + this.longitude + OceanicBuoyDataGetter.URLRADIUS + this.radius;
    }

    public ArrayList<OceanicBuoy> getBuoyData() {
        ArrayList<OceanicBuoy> oceanicBuoys = new ArrayList<OceanicBuoy>();
        try {
            URL url = new URL(this.url);
            SAXParserFactory saxpf = SAXParserFactory.newInstance();
            SAXParser saxParser = saxpf.newSAXParser();
            XMLReader xmlReader = saxParser.getXMLReader();
            OceanicBuoyDataManager dataManager = new OceanicBuoyDataManager();
            xmlReader.setContentHandler(dataManager);
            xmlReader.parse(new InputSource(url.openStream()));
            oceanicBuoys = dataManager.getBuoyDataList();
        } catch (Exception e) {
            e.printStackTrace();
        }
        return oceanicBuoys;
    }
}
