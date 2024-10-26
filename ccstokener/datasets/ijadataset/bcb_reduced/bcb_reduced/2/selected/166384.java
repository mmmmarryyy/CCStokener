package edu.washington.mysms.server.sample.weather;

import java.io.InputStream;
import java.net.URL;
import java.net.URLConnection;
import java.sql.SQLException;
import java.util.Properties;
import org.xml.sax.Attributes;
import org.xml.sax.InputSource;
import org.xml.sax.XMLReader;
import org.xml.sax.helpers.DefaultHandler;
import org.xml.sax.helpers.XMLReaderFactory;
import edu.washington.mysms.coding.ResultTable;
import edu.washington.mysms.security.SqlAccount;
import edu.washington.mysms.server.DatabaseInterface;

public class WeatherUpdater {

    private WeatherUpdater() {
    }

    public static final boolean DEBUG = true;

    private static DatabaseInterface dbi;

    public static void main(String[] args) {
        Properties p = new Properties();
        try {
            InputStream pStream = ClassLoader.getSystemResourceAsStream("sample_weather.properties");
            p.load(pStream);
        } catch (Exception e) {
            System.err.println("Could not load properties file.");
            System.err.println(e.getMessage());
            e.printStackTrace();
            return;
        }
        if (WeatherUpdater.DEBUG) {
            System.out.println("hostname: " + p.getProperty("hostname"));
        }
        if (WeatherUpdater.DEBUG) {
            System.out.println("database: " + p.getProperty("database"));
        }
        if (WeatherUpdater.DEBUG) {
            System.out.println("username: " + p.getProperty("username"));
        }
        if (WeatherUpdater.DEBUG) {
            System.out.println("password: " + p.getProperty("password"));
        }
        SqlAccount sqlAccount = new SqlAccount(p.getProperty("hostname"), p.getProperty("database"), p.getProperty("username"), p.getProperty("password"));
        try {
            dbi = new DatabaseInterface(sqlAccount);
        } catch (Exception e) {
            System.err.println(e.getMessage());
            e.printStackTrace();
            return;
        }
        XMLReader xr = null;
        try {
            xr = XMLReaderFactory.createXMLReader();
        } catch (Exception e) {
            System.err.println("Could not create an XML reader.");
            System.err.println(e.getMessage());
            e.printStackTrace();
            return;
        }
        for (int zipCode = 99999; zipCode > 30737; zipCode--) {
            try {
                if (isBlacklisted(zipCode)) {
                    continue;
                }
            } catch (Exception e) {
                if (DEBUG) {
                    System.err.println("Could not check if zip is blacklisted: " + zipCode);
                    System.err.println(e.getMessage());
                    e.printStackTrace();
                }
            }
            xr.setContentHandler(new WeatherXMLHandler(Integer.toString(zipCode)));
            queryZip(zipCode, xr);
            try {
                Thread.sleep(200);
            } catch (Exception e) {
            }
        }
    }

    private static void queryZip(int zipCode, XMLReader xr) {
        URL url;
        try {
            url = new URL("http://xoap.weather.com/weather/local/" + zipCode + "?cc=*");
        } catch (Exception e) {
            System.err.println("Could not create a valid URL for zip " + zipCode);
            System.err.println(e.getMessage());
            e.printStackTrace();
            return;
        }
        URLConnection urlConn;
        try {
            urlConn = url.openConnection();
            urlConn.setDoInput(true);
            urlConn.setDoOutput(false);
            urlConn.setUseCaches(false);
        } catch (Exception e) {
            System.err.println("Could not open a connection to given url: " + url);
            System.err.println(e.getMessage());
            e.printStackTrace();
            return;
        }
        try {
            xr.parse(new InputSource(urlConn.getInputStream()));
        } catch (Exception e) {
            System.err.println("Could not parse data at given url: " + url);
            System.err.println(e.getMessage());
            e.printStackTrace();
            return;
        }
    }

    public static void updateOrAdd(int zipCode, String condition, int temperature, int humidity) throws NullPointerException, SQLException, ClassNotFoundException {
        dbi.executeQuery("REPLACE INTO Current_Weather VALUES ('" + zipCode + "', '" + condition + "', '" + temperature + "', '" + humidity + "')");
    }

    public static boolean isBlacklisted(int zipCode) throws NullPointerException, SQLException, ClassNotFoundException {
        ResultTable table = dbi.executeQuery("SELECT * FROM `Zip_Blacklist` WHERE `ZipCode`='" + zipCode + "'");
        if (table.size() > 0) {
            return true;
        }
        return false;
    }

    public static void blacklistZip(int zipCode) throws NullPointerException, SQLException, ClassNotFoundException {
        dbi.executeQuery("REPLACE INTO Zip_Blacklist VALUES ('" + zipCode + "', NOW())");
    }

    private static class WeatherXMLHandler extends DefaultHandler {

        private StringBuffer currentElement;

        private String zipCode;

        private String condition;

        private String temperature;

        private String humidity;

        public WeatherXMLHandler(String zipCode) {
            this.zipCode = zipCode;
        }

        public void startDocument() {
            currentElement = new StringBuffer(101);
            condition = null;
            temperature = null;
            humidity = null;
            System.out.println();
        }

        public void endDocument() {
            System.out.println();
            if (zipCode == null || condition == null || temperature == null || humidity == null) {
                if (DEBUG) {
                    System.err.println("Could not parse this xml file into an entry.  Values are null.");
                }
                return;
            }
            try {
                int zipCode = Integer.parseInt(this.zipCode);
                int temperature = Integer.parseInt(this.temperature);
                int humidity = Integer.parseInt(this.humidity);
                updateOrAdd(zipCode, condition, temperature, humidity);
            } catch (Exception e) {
                if (DEBUG) {
                    System.err.println("Could not add entry to database for zip: " + zipCode);
                    System.err.println(e.getMessage());
                    e.printStackTrace();
                }
            }
        }

        public void startElement(String uri, String name, String qName, Attributes atts) {
            if (currentElement.length() > 0) {
                currentElement.append('.');
            }
            currentElement.append(qName);
            if (qName.compareToIgnoreCase("loc") == 0) {
                String currentElement = this.currentElement.toString();
                if (currentElement.compareToIgnoreCase("weather.loc") == 0) {
                    zipCode = atts.getValue("id");
                    if (DEBUG) {
                        System.out.println("Zip Code: " + zipCode);
                    }
                }
            }
        }

        public void endElement(String uri, String name, String qName) {
            String toRemove;
            if (currentElement.lastIndexOf(".") == -1) {
                toRemove = qName;
            } else {
                toRemove = "." + qName;
            }
            int i = currentElement.lastIndexOf(toRemove);
            currentElement.delete(i, i + toRemove.length());
        }

        public void characters(char ch[], int start, int length) {
            String currentElement = this.currentElement.toString();
            String value = new String(ch, start, length).trim();
            if (currentElement.compareToIgnoreCase("error.err") == 0) {
                if (value.compareToIgnoreCase("Invalid location provided.") == 0) {
                    try {
                        blacklistZip(Integer.parseInt(zipCode));
                    } catch (Exception e) {
                        if (DEBUG) {
                            System.err.println("Could not blacklist zip: " + zipCode);
                            System.err.println(e.getMessage());
                            e.printStackTrace();
                        }
                    }
                    if (DEBUG) {
                        System.out.println("Given Zip Code is invalid (adding to blacklist): " + zipCode);
                    }
                }
            }
            if (currentElement.compareToIgnoreCase("weather.cc.t") == 0) {
                condition = value;
                if (DEBUG) {
                    System.out.println("Condition: " + condition);
                }
            }
            if (currentElement.compareToIgnoreCase("weather.cc.tmp") == 0) {
                temperature = value;
                if (DEBUG) {
                    System.out.println("Temperature: " + temperature);
                }
            }
            if (currentElement.compareToIgnoreCase("weather.cc.hmid") == 0) {
                humidity = value;
                if (DEBUG) {
                    System.out.println("Humidity: " + humidity);
                }
            }
        }
    }
}
