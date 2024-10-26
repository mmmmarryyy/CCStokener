package de.byteholder.geoclipse.weather;

import java.io.IOException;
import java.io.InputStream;
import java.io.UnsupportedEncodingException;
import java.net.MalformedURLException;
import java.net.URL;
import java.net.URLEncoder;
import java.text.DateFormat;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import org.w3c.dom.DOMException;
import org.w3c.dom.Document;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;
import org.xml.sax.InputSource;
import org.xml.sax.SAXException;
import com.sun.org.apache.xerces.internal.parsers.DOMParser;

/**
 * Get weather from Google Weather API
 * 
 * http://www.google.com/ig/api?weather=New%20York,%20USA
 * 
 * 
 * @author Veit Edunjobi
 * 
 */
public class WeatherProvider {

    public static final String baseUrlImages = "http://img0.gmodules.com/ig";

    private static final String baseUrlService = "http://www.google.com/ig/";

    private URL url;

    List<WeatherForecastConditions> weatherDataForecasts;

    WeatherCurrentCondition weatherCurrentCondition;

    WeatherForecastInformation weatherForecastInformation;

    private String encodeQuery;

    /**
	 * 
	 * @param query i.e. New%20York,%20USA
	 */
    public WeatherProvider(String query) {
        try {
            encodeQuery = URLEncoder.encode(query, "utf8");
            url = new URL(baseUrlService + "api?weather=" + encodeQuery);
            Activator.log("Querying weather: " + url);
            System.out.println("Querying weather: " + url);
        } catch (MalformedURLException e) {
            e.printStackTrace();
        } catch (UnsupportedEncodingException e) {
            e.printStackTrace();
        }
        weatherForecastInformation = new WeatherForecastInformation();
        weatherCurrentCondition = new WeatherCurrentCondition();
        weatherDataForecasts = new ArrayList<WeatherForecastConditions>();
        readData();
    }

    /**
	 * @param args
	 */
    public static void main(String[] args) {
        WeatherProvider weatherProvider = new WeatherProvider("New%20York,%20USA");
        WeatherForecastInformation weatherForecastInformation = weatherProvider.getWeatherForecastInformation();
        System.out.println(weatherForecastInformation.getCity() + " " + weatherForecastInformation.getCurrentDateTime() + " " + weatherForecastInformation.getForecastDate());
        WeatherCurrentCondition weatherCurrentCondition = weatherProvider.getWeatherCurrentCondition();
        System.out.println(weatherCurrentCondition.getCondition() + " " + weatherCurrentCondition.getHumidity() + " " + weatherCurrentCondition.getTempc() + " " + weatherCurrentCondition.getTempf() + " " + weatherCurrentCondition.getWindCondition() + " " + weatherCurrentCondition.getIcon());
        List<WeatherForecastConditions> data = weatherProvider.getWeatherDataForecasts();
        for (WeatherForecastConditions weatherForecastConditions : data) {
            System.out.println(weatherForecastConditions.getDayOfWeek() + " " + weatherForecastConditions.getCondition() + " " + weatherForecastConditions.getHigh() + " " + weatherForecastConditions.getLow() + " " + weatherForecastConditions.getIcon());
        }
    }

    /**
	 * 
	 * @param url
	 * @return the inputStream or null
	 */
    private InputStream getStream() {
        InputStream result = null;
        try {
            Activator.log("Querying weather: " + url.toString());
            result = url.openStream();
        } catch (IOException e) {
            e.printStackTrace();
        }
        return result;
    }

    public List<WeatherForecastConditions> getWeatherDataForecasts() {
        return weatherDataForecasts;
    }

    public WeatherCurrentCondition getWeatherCurrentCondition() {
        return weatherCurrentCondition;
    }

    public WeatherForecastInformation getWeatherForecastInformation() {
        return weatherForecastInformation;
    }

    /**
	 * Reads stream with DOMParser and fills the local data HashMap and weatherDataForcast List.
	 */
    private void readData() {
        DOMParser domParser = new DOMParser();
        try {
            domParser.parse(new InputSource(getStream()));
            Document doc = domParser.getDocument();
            Node node = doc.getElementsByTagName("forecast_information").item(0);
            if (node == null) {
                weatherDataForecasts.clear();
                return;
            }
            initWeatherForecastInformation(doc);
            initWeatherCurrentConditions(doc);
            initWeatherForecasts(doc);
        } catch (SAXException e) {
            e.printStackTrace();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    private void initWeatherForecastInformation(Document doc) {
        Node node = doc.getElementsByTagName("forecast_information").item(0);
        NodeList nodeListForecastInformation = node.getChildNodes();
        String city = nodeListForecastInformation.item(0).getAttributes().item(0).getNodeValue();
        String currentDateTime = nodeListForecastInformation.item(5).getAttributes().item(0).getNodeValue();
        String forecastDate = nodeListForecastInformation.item(4).getAttributes().item(0).getNodeValue();
        Date currentDateT = null;
        Date forecastDat = null;
        SimpleDateFormat simpleDateFormat = new SimpleDateFormat("yyyy-MM-dd");
        try {
            forecastDat = simpleDateFormat.parse(forecastDate);
            currentDateT = simpleDateFormat.parse(currentDateTime);
            weatherForecastInformation.setCity(city);
            weatherForecastInformation.setCurrentDateTime(currentDateT);
            weatherForecastInformation.setForecastDate(forecastDat);
        } catch (DOMException e) {
            e.printStackTrace();
        } catch (ParseException e) {
            e.printStackTrace();
        }
    }

    private void initWeatherCurrentConditions(Document doc) {
        Node node;
        node = doc.getElementsByTagName("current_conditions").item(0);
        NodeList nodeListCurrentConditions = node.getChildNodes();
        for (int i = 0; i < nodeListCurrentConditions.getLength(); i++) {
            Node childNode = nodeListCurrentConditions.item(i);
            String elementName = childNode.getNodeName();
            String data = childNode.getAttributes().item(0).getNodeValue();
            if ("condition".equals(elementName)) {
                weatherCurrentCondition.setCondition(data);
            } else if ("temp_f".equals(elementName)) {
                weatherCurrentCondition.setTempf(data);
            } else if ("temp_c".equals(elementName)) {
                weatherCurrentCondition.setTempc(data);
            } else if ("humidity".equals(elementName)) {
                weatherCurrentCondition.setHumidity(data);
            } else if ("icon".equals(elementName)) {
                weatherCurrentCondition.setIcon(data);
            } else if ("wind_condition".equals(elementName)) {
                weatherCurrentCondition.setWindCondition(data);
            }
        }
    }

    private void initWeatherForecasts(Document doc) {
        NodeList nodeListForecastConditions = doc.getElementsByTagName("forecast_conditions");
        WeatherForecastConditions weatherDataForecast = null;
        for (int i = 0; i < nodeListForecastConditions.getLength(); i++) {
            weatherDataForecast = new WeatherForecastConditions();
            Node childNodeForecastCondition = nodeListForecastConditions.item(i);
            NodeList subElements = childNodeForecastCondition.getChildNodes();
            for (int j = 0; j < subElements.getLength(); j++) {
                Node subElement = subElements.item(j);
                String nodeName = subElement.getNodeName();
                if ("condition".equals(nodeName)) {
                    weatherDataForecast.setCondition(subElement.getAttributes().item(0).getNodeValue());
                } else if ("day_of_week".equals(nodeName)) {
                    weatherDataForecast.setDayOfWeek(subElement.getAttributes().item(0).getNodeValue());
                } else if ("high".equals(nodeName)) {
                    weatherDataForecast.setHigh(subElement.getAttributes().item(0).getNodeValue());
                } else if ("low".equals(nodeName)) {
                    weatherDataForecast.setLow(subElement.getAttributes().item(0).getNodeValue());
                } else if ("icon".equals(nodeName)) {
                    weatherDataForecast.setIcon(baseUrlImages + subElement.getAttributes().item(0).getNodeValue());
                }
            }
            weatherDataForecasts.add(weatherDataForecast);
        }
    }
}
