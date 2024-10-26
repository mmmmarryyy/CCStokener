package br.usp.pcs.weather.impl;

import java.io.IOException;
import java.io.InputStream;
import java.net.URL;
import java.net.URLConnection;
import java.util.PropertyResourceBundle;
import org.jdom.Document;
import org.jdom.Element;
import org.jdom.JDOMException;
import org.jdom.Namespace;
import org.jdom.input.SAXBuilder;

/**
 * Class that reads the XML document of the RSS Fedd of Yahoo! Weather.
 * 
 * @author Marcelo Li Koga
 * 
 */
public class XMLWeatherReader {

    private String baseUrl = "http://weather.yahooapis.com/forecastrss";

    private char unit = 'c';

    /**
	 * The XML Document object.
	 */
    private Document docin;

    private Namespace namespace = Namespace.getNamespace("yweather", "http://xml.weather.yahoo.com/ns/rss/1.0");

    private Element root;

    private Element channel;

    private Element item;

    /**
	 * Creates a new reader, given the url of the xml file.
	 * 
	 * @param url
	 *            url of the Yahoo! RSS
	 * @throws IOException 
	 * @throws IOException
	 * @throws JDOMException
	 */
    public XMLWeatherReader(String cityName) throws IOException {
        String locationId = CityCodes.getString(cityName);
        try {
            URL url = new URL(baseUrl + "?p=" + locationId + "&u=" + unit);
            URLConnection urlconn = url.openConnection();
            InputStream in = urlconn.getInputStream();
            SAXBuilder builder = new SAXBuilder();
            this.docin = builder.build(in);
            root = this.docin.getRootElement();
            channel = root.getChild("channel");
            item = channel.getChild("item");
        } catch (JDOMException e) {
            throw new IOException(e.getMessage());
        }
    }

    /**
	 * Verifies whether the reader is connected or not.
	 * 
	 * @return true, if it is connected.
	 */
    public boolean isConnected() {
        return this.docin != null;
    }

    public String getRoot() {
        Element nroot = this.docin.getRootElement();
        return nroot.getName();
    }

    /**
	 * Return the current ambient temperature
	 * @return temperature in Celcius degrees.
	 */
    public double getTemperature() {
        Element units = channel.getChild("units", this.namespace);
        Element condition = item.getChild("condition", this.namespace);
        String content = condition.getAttributeValue("temp");
        double temperature = Double.parseDouble(content);
        if (units.getAttributeValue("temperature").equals("F")) {
            temperature = (temperature - 32) * 5 / 9;
        }
        return temperature;
    }

    /**
	 * Return the Highest temperature for the current day.
	 * @return temperature in Celcius degrees.
	 */
    public double getHigh() {
        Element units = channel.getChild("units", this.namespace);
        Element forecast = item.getChild("forecast", this.namespace);
        String content = forecast.getAttributeValue("high");
        double high = Double.parseDouble(content);
        if (units.getAttributeValue("temperature").equals("F")) {
            high = (high - 32) * 5 / 9;
        }
        return high;
    }

    /**
	 * Return the Lowest temperature for the current day.
	 * @return temperature in Celcius degrees.
	 */
    public double getLow() {
        Element units = channel.getChild("units", this.namespace);
        Element forecast = item.getChild("forecast", this.namespace);
        String content = forecast.getAttributeValue("low");
        double low = Double.parseDouble(content);
        if (units.getAttributeValue("temperature").equals("F")) {
            low = (low - 32) * 5 / 9;
        }
        return low;
    }

    /**
	 * Return the current humidity in the air
	 * @return temperature in Celcius degrees.
	 */
    public int getRelativeHumidity() {
        Element atmosphere = channel.getChild("atmosphere", this.namespace);
        String content = atmosphere.getAttributeValue("humidity");
        int humidity = Integer.parseInt(content);
        return humidity;
    }

    public String getDescription() {
        Element root = this.docin.getRootElement();
        Element channel = root.getChild("channel");
        Element item = channel.getChild("item");
        Element condition = item.getChild("condition", this.namespace);
        String text = condition.getAttributeValue("text");
        return text;
    }

    public String getCityName() {
        Element atmosphere = channel.getChild("location", this.namespace);
        String content = atmosphere.getAttributeValue("city");
        return content;
    }
}
