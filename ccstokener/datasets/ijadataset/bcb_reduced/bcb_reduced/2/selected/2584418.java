package net.sf.cruisemonitor;

import javax.swing.*;
import java.io.BufferedReader;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStreamReader;
import java.net.MalformedURLException;
import java.net.URL;
import java.net.URLConnection;
import java.util.Properties;

public class CruiseConnector {

    private String cruiseUrl;

    private URLConnection connection;

    private Properties properties;

    public CruiseConnector(String cruiseUrl) {
        this.cruiseUrl = cruiseUrl;
        properties = new Properties();
        try {
            properties.load(new FileInputStream("messages.properties"));
        } catch (IOException e) {
        }
    }

    public CruiseConnector(URLConnection connection) {
        this.connection = connection;
    }

    public CruiseConnector() {
    }

    public boolean connect() {
        try {
            URL url = createURL();
            if (url != null) {
                connection = url.openConnection();
                connection.connect();
                return true;
            }
        } catch (IOException e) {
            JOptionPane.showMessageDialog(null, properties.getProperty("connection.not.established"));
        }
        return false;
    }

    private URL createURL() {
        try {
            return new URL(cruiseUrl);
        } catch (MalformedURLException e) {
            JOptionPane.showMessageDialog(null, properties.getProperty("invalid.url"));
        }
        return null;
    }

    public boolean reader() {
        if (!connect()) return false;
        BufferedReader in;
        String inputLine;
        try {
            in = new BufferedReader(new InputStreamReader(connection.getInputStream()));
            while ((inputLine = in.readLine()) != null) {
                if (inputLine.contains(">BUILD COMPLETE")) return true;
            }
            in.close();
        } catch (Exception e) {
            e.printStackTrace();
        }
        return false;
    }
}
