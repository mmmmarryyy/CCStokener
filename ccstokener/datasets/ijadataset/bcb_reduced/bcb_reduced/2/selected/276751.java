package au.com.georgi.wave.reader;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.URL;
import java.util.logging.Logger;

/**
 * The page in HTML format is great for getting image information
 */
public class HTMLWebsiteReader {

    private static final Logger log = Logger.getLogger(HTMLWebsiteReader.class.getName());

    public static String readWebsite(URL url) {
        HttpURLConnection connection;
        log.warning("Attempting to open URL connection...");
        try {
            connection = (HttpURLConnection) url.openConnection();
            InputStream inputStream = connection.getInputStream();
            return convertStreamToString(inputStream);
        } catch (IOException e) {
            throw new RuntimeException("Could not read website for url: " + url.getPath());
        } catch (Exception e) {
            throw new RuntimeException("Could not read website for url: " + url.getPath());
        }
    }

    private static String convertStreamToString(InputStream is) {
        BufferedReader reader = new BufferedReader(new InputStreamReader(is));
        StringBuilder sb = new StringBuilder();
        String line = null;
        try {
            while ((line = reader.readLine()) != null) {
                sb.append(line + "\n");
            }
        } catch (IOException e) {
            log.severe(e.getMessage());
        } finally {
            try {
                is.close();
            } catch (IOException e) {
                log.severe(e.getMessage());
            }
        }
        return sb.toString();
    }
}
