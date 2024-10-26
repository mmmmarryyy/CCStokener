package com.android.microweather;

import java.io.BufferedOutputStream;
import java.io.File;
import java.io.FileOutputStream;
import java.io.FilenameFilter;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.Calendar;
import javax.xml.parsers.SAXParser;
import javax.xml.parsers.SAXParserFactory;
import org.apache.http.HttpResponse;
import org.apache.http.HttpStatus;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.impl.client.DefaultHttpClient;
import org.xml.sax.Attributes;
import org.xml.sax.SAXException;
import org.xml.sax.SAXParseException;
import org.xml.sax.helpers.DefaultHandler;
import android.content.Context;
import android.os.Handler;
import android.preference.PreferenceManager;

public class Networker extends DefaultHandler implements Runnable {

    /**
	 * XML tag names, attributes.
	 */
    private static final String AWS_URI_PREFIX = new String("http://www.aws.com/aws");

    private static final String AWS_FORECAST = new String("forecast");

    private static final String AWS_TITLE = new String("title");

    private static final String AWS_SHORT_PREDICTION = new String("short-prediction");

    private static final String AWS_PREDICTION = new String("prediction");

    private static final String AWS_IMAGE = new String("image");

    private static final String AWS_HIGH = new String("high");

    private static final String AWS_LOW = new String("low");

    private static final String AWS_WEB_URL = new String("WebURL");

    /**
	 * URL, parameters, etc. - anything used in HTTP GET to WeatherBug.
	 */
    private static final String GET_URL = new String("http://api.wxbug.net/getForecastRSS.aspx");

    private static final String GET_PARAM_ACODE = null;

    private static final String GET_PARAM_ACODE_PREFIX = new String("ACode");

    private static final String GET_PARAM_ZIP_PREFIX = new String("zipCode");

    private static final String GET_PARAM_CITY_CODE_PREFIX = new String("cityCode");

    private static final String GET_PARAM_UNIT_PREFIX = new String("unittype");

    /**
	 * According to http://code.google.com/android/toolbox/performance.html#avoid_enums
	 * we should avoid using enums in Android Java. That's why below constants are not
	 * members of the same enumeration.
	 */
    private static final int XML_TAG_OUTOFINTEREST = 0;

    private static final int XML_TAG_FORECAST = 1;

    private static final int XML_TAG_TITLE = 2;

    private static final int XML_TAG_SHORT_PREDICTION = 3;

    private static final int XML_TAG_PREDICTION = 4;

    private static final int XML_TAG_IMAGE = 5;

    private static final int XML_TAG_HIGH = 6;

    private static final int XML_TAG_LOW = 7;

    private int currentlyProcessedTag = XML_TAG_OUTOFINTEREST;

    private Context context = null;

    private Handler handler = null;

    private ArrayList<City> citiesToBeUpdated = null;

    private ArrayList<ForecastedDay> forecast = null;

    private String title = null;

    private String shortPrediction = null;

    private String prediction = null;

    private String imageURL = null;

    private String high;

    private String low;

    private class XmlProcessingException extends Exception {

        private String message;

        public XmlProcessingException(String message) {
            super();
            this.message = message;
        }

        public String getMessage() {
            return message;
        }
    }

    public Networker(Context context, Handler handler, ArrayList<City> citiesToBeUpdated) {
        super();
        this.context = context;
        this.handler = handler;
        this.citiesToBeUpdated = citiesToBeUpdated == null ? DataModel.getInstance().getCityList() : citiesToBeUpdated;
    }

    @Override
    public void run() {
        HttpGet httpGet = null;
        try {
            DefaultHttpClient httpClient = new DefaultHttpClient();
            DataModel model = DataModel.getInstance();
            for (City city : citiesToBeUpdated) {
                String preferredUnitType = PreferenceManager.getDefaultSharedPreferences(context).getString(context.getString(R.string.settings_units_key), context.getString(R.string.settings_units_default_value));
                String codePrefix = city.getCountryName().startsWith("United States") ? GET_PARAM_ZIP_PREFIX : GET_PARAM_CITY_CODE_PREFIX;
                String requestUri = new String(GET_URL + "?" + GET_PARAM_ACODE_PREFIX + "=" + GET_PARAM_ACODE + "&" + codePrefix + "=" + city.getId() + "&" + GET_PARAM_UNIT_PREFIX + "=" + preferredUnitType);
                httpGet = new HttpGet(requestUri);
                HttpResponse response = httpClient.execute(httpGet);
                if (response.getStatusLine().getStatusCode() == HttpStatus.SC_OK) {
                    processXML(response.getEntity().getContent());
                    for (ForecastedDay day : forecast) {
                        int pos = day.getImageURL().lastIndexOf('/');
                        if (pos < 0 || pos + 1 == day.getImageURL().length()) throw new Exception("Invalid image URL");
                        final String imageFilename = day.getImageURL().substring(pos + 1);
                        File downloadDir = context.getDir(ForecastedDay.DOWNLOAD_DIR, Context.MODE_PRIVATE);
                        File[] imagesFilteredByName = downloadDir.listFiles(new FilenameFilter() {

                            @Override
                            public boolean accept(File dir, String filename) {
                                if (filename.equals(imageFilename)) return true; else return false;
                            }
                        });
                        if (imagesFilteredByName.length == 0) {
                            httpGet = new HttpGet(day.getImageURL());
                            response = httpClient.execute(httpGet);
                            if (response.getStatusLine().getStatusCode() == HttpStatus.SC_OK) {
                                BufferedOutputStream bus = null;
                                try {
                                    bus = new BufferedOutputStream(new FileOutputStream(downloadDir.getAbsolutePath() + "/" + imageFilename));
                                    response.getEntity().writeTo(bus);
                                } finally {
                                    bus.close();
                                }
                            }
                        }
                    }
                    city.setDays(forecast);
                    city.setLastUpdated(Calendar.getInstance().getTime());
                    model.saveCity(city);
                }
            }
        } catch (Exception e) {
            httpGet.abort();
            e.printStackTrace();
        } finally {
            handler.sendEmptyMessage(1);
        }
    }

    private void processXML(InputStream input) throws XmlProcessingException {
        try {
            SAXParser parser = SAXParserFactory.newInstance().newSAXParser();
            parser.parse(input, this);
        } catch (Exception e) {
            throw new XmlProcessingException(e.getMessage());
        }
    }

    @Override
    public void endElement(String uri, String localName, String name) throws SAXException {
        if (AWS_FORECAST.equals(localName) && AWS_URI_PREFIX.equals(uri)) {
            ForecastedDay day = new ForecastedDay();
            day.setTitle(title);
            day.setShortPrediction(shortPrediction);
            day.setImageURL(imageURL);
            day.setTemperatureHigh(high);
            day.setTemperatureLow(low);
            day.setPrediction(prediction.replace("&deg;", "�"));
            forecast.add(day);
            prediction = null;
        }
        currentlyProcessedTag = XML_TAG_OUTOFINTEREST;
    }

    @Override
    public void startElement(String uri, String localName, String name, Attributes attributes) throws SAXException {
        if (AWS_FORECAST.equals(localName) && AWS_URI_PREFIX.equals(uri)) {
            currentlyProcessedTag = XML_TAG_FORECAST;
        } else if (AWS_TITLE.equals(localName) && AWS_URI_PREFIX.equals(uri)) {
            currentlyProcessedTag = XML_TAG_TITLE;
        } else if (AWS_SHORT_PREDICTION.equals(localName) && AWS_URI_PREFIX.equals(uri)) {
            currentlyProcessedTag = XML_TAG_SHORT_PREDICTION;
        } else if (AWS_PREDICTION.equals(localName) && AWS_URI_PREFIX.equals(uri)) {
            currentlyProcessedTag = XML_TAG_PREDICTION;
        } else if (AWS_IMAGE.equals(localName) && AWS_URI_PREFIX.equals(uri)) {
            currentlyProcessedTag = XML_TAG_IMAGE;
        } else if (AWS_HIGH.equals(localName) && AWS_URI_PREFIX.equals(uri)) {
            currentlyProcessedTag = XML_TAG_HIGH;
        } else if (AWS_LOW.equals(localName) && AWS_URI_PREFIX.equals(uri)) {
            currentlyProcessedTag = XML_TAG_LOW;
        }
    }

    @Override
    public void startDocument() throws SAXException {
        forecast = new ArrayList<ForecastedDay>();
    }

    @Override
    public void characters(char[] ch, int start, int length) throws SAXException {
        switch(currentlyProcessedTag) {
            case XML_TAG_TITLE:
                title = new String(ch, start, length);
                break;
            case XML_TAG_SHORT_PREDICTION:
                shortPrediction = new String(ch, start, length);
                break;
            case XML_TAG_PREDICTION:
                if (prediction == null) prediction = new String(ch, start, length); else prediction += new String(ch, start, length);
                break;
            case XML_TAG_IMAGE:
                imageURL = new String(ch, start, length);
                break;
            case XML_TAG_HIGH:
                high = temperatureValidityCheck(ch, start, length);
                break;
            case XML_TAG_LOW:
                low = temperatureValidityCheck(ch, start, length);
                break;
            default:
                break;
        }
    }

    @Override
    public void error(SAXParseException e) throws SAXException {
        super.error(e);
    }

    @Override
    public void fatalError(SAXParseException e) throws SAXException {
        super.fatalError(e);
    }

    private String temperatureValidityCheck(char[] ch, int start, int length) {
        String temperature = new String(ch, start, length);
        try {
            Integer.parseInt(new String(ch, start, length));
        } catch (NumberFormatException e) {
            temperature = new String("n/a");
        }
        return temperature;
    }
}
