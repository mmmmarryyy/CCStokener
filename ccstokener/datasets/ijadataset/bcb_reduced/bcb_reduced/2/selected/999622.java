package oss.jthinker.interop;

import java.io.BufferedReader;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.IOException;
import java.io.OutputStream;
import java.net.HttpURLConnection;
import java.net.MalformedURLException;
import java.net.URL;
import java.util.LinkedList;
import java.util.List;

/**
 * Utility class for accessing the remote server.
 * 
 * @author iappel
 */
public class InteropUtils {

    private static String _rootURL;

    public static URL getAccessURL(String page) throws InteropException {
        if (_rootURL == null) {
            _rootURL = "http://jthinker-server.appspot.com";
        }
        if (_rootURL.endsWith("/")) {
            if (page.startsWith("/")) {
                page = page.substring(1);
            }
        } else {
            if (!page.startsWith("/")) {
                page = "/" + page;
            }
        }
        try {
            return new URL(_rootURL + page);
        } catch (MalformedURLException ex) {
            throw new InteropException(page, ex);
        }
    }

    /**
     * Requests data from the remote server.
     * Makes an HTTP GET request and returns the
     * result as a list of lines.
     * <br />
     * Usage of this method implies that accessed
     * data is a plain-text, rather than XML.
     *
     * @param urlName URL to fetch
     * @return fetch result as a list of lines
     */
    public static List<String> request(String urlName) throws InteropException {
        URL accessUrl = getAccessURL(urlName);
        InputStream istream;
        try {
            istream = accessUrl.openStream();
        } catch (MalformedURLException muex) {
            throw new InteropException(accessUrl, muex);
        } catch (IOException ioex) {
            throw new InteropException(accessUrl, ioex);
        }
        BufferedReader reader = new BufferedReader(new InputStreamReader(istream));
        List<String> result = new LinkedList<String>();
        try {
            while (true) {
                String line = reader.readLine();
                if (line == null) {
                    return result;
                } else {
                    result.add(line);
                }
            }
        } catch (IOException ioex) {
            throw new InteropException(accessUrl, ioex);
        }
    }

    /**
     * Sets the active remove server's access URL.
     *
     * @param accessURL access URL
     */
    public static synchronized void setAccessURL(String accessURL) {
        _rootURL = accessURL;
    }

    /**
     * Does an HTTP POST request.
     * Server's response is ignored.
     *
     * @param urlName URL to POST
     * @param data binary data to POST
     * @param contentType MIME type of posted content
     * @param cookieData cookie data
     */
    public static void doHttpPost(String urlName, byte[] data, String contentType, String cookieData) throws InteropException {
        URL url = getAccessURL(urlName);
        try {
            HttpURLConnection connection = (HttpURLConnection) url.openConnection();
            connection.setDoOutput(true);
            connection.setRequestMethod("POST");
            connection.setRequestProperty("Cookie", cookieData);
            connection.setRequestProperty("Content-type", contentType);
            connection.setRequestProperty("Content-length", "" + data.length);
            OutputStream stream = connection.getOutputStream();
            stream.write(data);
            stream.flush();
            stream.close();
            connection.connect();
            InputStream inputStream = connection.getInputStream();
            inputStream.close();
        } catch (IOException ex) {
            throw new InteropException("Error POSTing to " + urlName, ex);
        }
    }
}
