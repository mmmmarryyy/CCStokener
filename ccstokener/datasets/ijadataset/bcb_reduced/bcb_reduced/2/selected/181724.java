package org.grailrtls.solver.weather;

import java.io.IOException;
import java.io.InputStream;
import java.net.MalformedURLException;
import java.net.URL;
import java.util.HashMap;
import java.util.Map;
import org.dom4j.Document;
import org.dom4j.DocumentException;
import org.dom4j.DocumentFactory;
import org.dom4j.io.SAXReader;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Implementation of {@link WeatherServiceInterface} that interfaces with the
 * Yahoo! Weather RSS feed and scrapes weather information for a ZIP code in the
 * United States. Portions of the code are based on <a href="http://www.sonatype.com/books/mvnex-book/reference/customizing-sect-simple-weather-source.html"
 * >Maven By Example: 4.6 Simple Weather Source Code</a>.
 * 
 * @author Robert Moore
 * 
 */
public class YahooWeatherService implements WeatherServiceInterface {

    /**
	 * Logging facility.
	 */
    private static final Logger log = LoggerFactory.getLogger(YahooWeatherService.class);

    /**
	 * Base scrape URL for Yahoo! Weather forecast RSS feed. Grabbed from
	 * sonatype.org's Simple Weather example.
	 * <p />
	 * <a href="http://www.sonatype.com/books/mvnex-book/reference/customizing-sect-simple-weather-source.html"
	 * >http://www.sonatype.com/books/mvnex-book/reference/customizing-sect-
	 * simple-weather-source.html</a>
	 */
    protected static final String SCRAPE_URL = "http://weather.yahooapis.com/forecastrss?p=";

    public WeatherInfo getCurrentWeather(String zipCode) {
        String url = SCRAPE_URL + zipCode;
        WeatherInfo weather = null;
        try {
            log.info("Scraping {}", url);
            weather = this.parseWeather(new URL(url).openStream());
        } catch (MalformedURLException e) {
            log.error("URL was malformed: {}", url, e);
        } catch (IOException e) {
            log.error("IOException when retrieving weather: {}", url, e);
        }
        if (weather == null) {
            log.warn("Unable to retrieve weather using url {}" + url);
        }
        return weather;
    }

    /**
	 * Returns a WeatherInfo object based on a Yahoo! Weather RSS document.
	 * 
	 * @param in
	 *            contains a Yahoo! Weather RSS document.
	 * @return a WeatherInfo object based on the RSS document, or null if an
	 *         error occurred or none is returned.
	 */
    protected WeatherInfo parseWeather(InputStream in) {
        WeatherInfo weather = new WeatherInfo();
        log.info("Creating XML Reader");
        SAXReader xmlReader = createXmlReader();
        Document doc = null;
        try {
            doc = xmlReader.read(in);
        } catch (DocumentException de) {
            log.error("Unable to parse response message.", de);
            return null;
        }
        log.info("Parsing XML Response");
        weather.setCity(doc.valueOf("/rss/channel/y:location/@city"));
        weather.setRegion(doc.valueOf("/rss/channel/y:location/@region"));
        weather.setCountry(doc.valueOf("/rss/channel/y:location/@country"));
        weather.setCondition(doc.valueOf("/rss/channel/item/y:condition/@text"));
        weather.setTemperature(doc.valueOf("/rss/channel/item/y:condition/@temp"));
        weather.setWindChill(doc.valueOf("/rss/channel/y:wind/@chill"));
        weather.setHumidity(doc.valueOf("/rss/channel/y:atmosphere/@humidity"));
        return weather;
    }

    /**
	 * Creates a new SAXReader object that can parse the Yahoo! Weather RSS
	 * feed.
	 * 
	 * Taken from <a href="http://www.sonatype.com/books/mvnex-book/reference/customizing-sect-simple-weather-source.html"
	 * >Maven By Example: 4.6 Simple Weather Source Code</a>.
	 * 
	 * @return a SAXReader configured to parse the Yahoo! Weather RSS document.
	 */
    private SAXReader createXmlReader() {
        Map<String, String> uris = new HashMap<String, String>();
        uris.put("y", "http://xml.weather.yahoo.com/ns/rss/1.0");
        DocumentFactory factory = new DocumentFactory();
        factory.setXPathNamespaceURIs(uris);
        SAXReader xmlReader = new SAXReader();
        xmlReader.setDocumentFactory(factory);
        return xmlReader;
    }
}
