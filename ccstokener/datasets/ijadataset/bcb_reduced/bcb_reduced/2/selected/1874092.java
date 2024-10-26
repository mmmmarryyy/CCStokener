package hambo.weather;

import java.net.URL;
import java.net.URLEncoder;
import java.net.HttpURLConnection;
import java.net.MalformedURLException;
import java.io.IOException;
import java.io.InputStream;
import java.util.Vector;
import org.apache.xerces.parsers.DOMParser;
import org.xml.sax.InputSource;
import org.w3c.dom.NodeList;
import org.w3c.dom.*;

/**
 * This class will fetch xml files form a web server, and if the file exists
 * it will deliver Forecast object or Forecast objects.
 *
  */
public class WeatherFetcher {

    private static final boolean DEBUG = false;

    private static final String MALFORMED_URL_MSG = "WeatherFetcher, bad url. ";

    private static final String FILE_NOT_FOUND_MSG = "WeatherFetcher, could not find xml document. ";

    private static final String COULD_NOT_CLOSE_FILE_MSG = "WeatherFetcher, could not close file. ";

    private static final String FILE_CORRUPT_MSG = "WeatherFetcher, xml file was corrupt. ";

    private String location;

    private Vector forecastlist;

    /**
     * The constructor needs the city location in the format 
     * 'ROOT.EUROPE.FR.PARIS' it will try to locate the corresponding
     * xml file containing a five day forecast for this location.
     * The result can be access by the method getForecast().
     */
    public WeatherFetcher(String location) {
        this.location = fixLocationString(location);
        forecastlist = new Vector();
        parseXMLFile();
    }

    /**
     * Get a Vector with a set of Forecast object that matches the argument
     * use in the creation of constructor of this object.
     */
    public Vector getForecast() {
        return forecastlist;
    }

    private void parseXMLFile() {
        String u = WeatherApplication.SERVER + location + ".xml";
        InputStream in = null;
        String str = null;
        try {
            URL url = new URL(u);
            HttpURLConnection con = (HttpURLConnection) url.openConnection();
            in = url.openStream();
            ParserToolXML prt = new ParserToolXML(in);
            if (prt.doc == null) {
                System.err.println(FILE_NOT_FOUND_MSG + u);
                return;
            }
            NodeList ndl = prt.doc.getElementsByTagName("weather");
            for (int i = 0; i < ndl.getLength(); i++) {
                Forecast f = new Forecast();
                str = prt.searchElementValue(ndl.item(i), "date");
                f.setDate(str);
                str = prt.searchElementValue(ndl.item(i), "daycode");
                f.setDaycode(Integer.parseInt(str.trim()));
                str = prt.searchElementValue(ndl.item(i), "nightcode");
                f.setNightcode(Integer.parseInt(str.trim()));
                str = prt.searchElementValue(ndl.item(i), "maxtemp");
                f.setDaytemp(Integer.parseInt(str.trim()));
                str = prt.searchElementValue(ndl.item(i), "mintemp");
                f.setNighttemp(Integer.parseInt(str.trim()));
                str = prt.searchElementValue(ndl.item(i), "winddirectionday");
                f.setDaywinddir(str);
                str = prt.searchElementValue(ndl.item(i), "windspeedday");
                f.setDaywindspeed(Integer.parseInt(str.trim()));
                str = prt.searchElementValue(ndl.item(i), "winddirectionnight");
                f.setNightwinddir(str);
                str = prt.searchElementValue(ndl.item(i), "windspeednight");
                f.setNightwindspeed(Integer.parseInt(str.trim()));
                forecastlist.addElement(f);
            }
        } catch (MalformedURLException e) {
            System.err.println(MALFORMED_URL_MSG + u);
            System.err.println(e.getMessage());
            e.printStackTrace();
        } catch (IOException e) {
        } catch (NumberFormatException e) {
            System.err.println(FILE_CORRUPT_MSG + u);
            System.err.println("-" + str + "-");
            System.err.println(e.getMessage());
            e.printStackTrace();
        } finally {
            if (in != null) {
                try {
                    in.close();
                } catch (IOException e) {
                    System.err.println(COULD_NOT_CLOSE_FILE_MSG + u);
                    e.printStackTrace();
                }
            }
        }
    }

    private String fixLocationString(String location) {
        if (location == null) return null;
        return URLEncoder.encode(location.replace(' ', '_').replace('/', '_').toLowerCase());
    }
}

class ParserToolXML {

    public Document doc;

    public ParserToolXML(InputStream in) {
        try {
            DOMParser parser = new DOMParser();
            parser.parse(new InputSource(in));
            doc = parser.getDocument();
            if (in != null) in.close();
        } catch (Exception e) {
            System.out.println(e);
        }
    }

    /**
     * Return the Value of the TAG called "TAG" in the tree with root
     * "node" as root NB: if the node called "TAG" has Sons, the
     * values of the sons will be included in the result
     * */
    public String searchElementValue(Node node, String TAG) {
        String chaineResult = "";
        NodeList list = node.getChildNodes();
        for (int i = 0; i < list.getLength(); i++) {
            if (list.item(i).getNodeType() == 3) {
                if (list.item(i).getParentNode().getNodeName().equals(TAG)) {
                    chaineResult = chaineResult + list.item(i).getNodeValue();
                }
            } else if (list.item(i).getNodeType() == 1) {
                if (list.item(i).getParentNode().getNodeName().equals(TAG)) {
                    NodeList listSon = list.item(i).getChildNodes();
                    for (int l = 0; l < listSon.getLength(); l++) {
                        chaineResult = chaineResult + searchElementValue(list.item(i));
                    }
                } else {
                    chaineResult = chaineResult + searchElementValue(list.item(i), TAG);
                }
            }
        }
        return chaineResult;
    }

    /**
     * Return ALL the Value IN the node "node" and of his sons
     * recursilvely
     */
    public String searchElementValue(Node node) {
        NodeList list = node.getChildNodes();
        String chaineResult = "";
        for (int i = 0; i < list.getLength(); i++) {
            if (list.item(i).getNodeType() == 3) {
                chaineResult = chaineResult + list.item(i).getNodeValue();
            } else {
                chaineResult = chaineResult + searchElementValue(list.item(i));
            }
        }
        return chaineResult;
    }
}
