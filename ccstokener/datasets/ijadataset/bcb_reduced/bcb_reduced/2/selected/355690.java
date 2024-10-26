package fr.slvn.badass.parser;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.MalformedURLException;
import java.net.URL;

public class BadassParser {

    StringBuilder mText = new StringBuilder();

    String mUrl;

    public BadassParser(String pUrl) {
        this.mUrl = pUrl;
    }

    public String parse() {
        try {
            URL url = new URL(mUrl);
            HttpURLConnection connection = (HttpURLConnection) url.openConnection();
            connection.setRequestMethod("GET");
            connection.setDoOutput(true);
            connection.connect();
            BufferedReader reader = new BufferedReader(new InputStreamReader(connection.getInputStream()));
            String line;
            boolean flag1 = false;
            while ((line = reader.readLine()) != null) {
                line = line.trim();
                if (!flag1 && line.contains("</center>")) flag1 = true;
                if (flag1 && line.contains("<br><center>")) break;
                if (flag1) {
                    mText.append(line);
                }
            }
        } catch (MalformedURLException e) {
            e.printStackTrace();
        } catch (IOException e) {
            e.printStackTrace();
        }
        return mText.toString();
    }
}
