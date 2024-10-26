package uk.co.thirstybear.hectorj;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.net.URL;
import java.util.logging.Level;
import java.util.logging.Logger;

public class HudsonScraper {

    static Logger logger = Logger.getLogger("uk.co.thirstybear.hectorj");

    static HectorPreferences prefs = HectorPreferences.getInstance();

    public String fetchRssAllData() {
        String hudsonUrlString = prefs.getHudsonUrl() + "/rssAll";
        String data = "";
        try {
            URL hudsonUrl = new URL(hudsonUrlString);
            data = readDataFromUrl(hudsonUrl);
        } catch (IOException e) {
            logger.log(Level.WARNING, "There was a problem getting data from the URL " + hudsonUrlString, e);
        }
        return data;
    }

    public String fetchRssLatestData() {
        String hudsonUrlString = prefs.getHudsonUrl() + "/rssLatest";
        String data = "";
        try {
            URL hudsonUrl = new URL(hudsonUrlString);
            data = readDataFromUrl(hudsonUrl);
        } catch (IOException e) {
            logger.log(Level.WARNING, "There was a problem getting data from the URL " + hudsonUrlString, e);
        }
        return data;
    }

    private String readDataFromUrl(URL url) throws IOException {
        InputStream inputStream = null;
        InputStreamReader streamReader = null;
        BufferedReader in = null;
        StringBuffer data = new StringBuffer();
        try {
            inputStream = url.openStream();
            streamReader = new InputStreamReader(inputStream);
            in = new BufferedReader(streamReader);
            String inputLine;
            while ((inputLine = in.readLine()) != null) data.append(inputLine);
        } finally {
            if (in != null) {
                in.close();
            }
            if (streamReader != null) {
                streamReader.close();
            }
            if (inputStream != null) {
                inputStream.close();
            }
        }
        return data.toString();
    }
}
