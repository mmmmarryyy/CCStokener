package clubmixer.client.utils;

import java.io.IOException;
import java.io.InputStream;
import java.net.MalformedURLException;
import java.net.URL;

/**
 *
 * @author Alexander Schindler
 */
public class URLGrabber {

    /**
     * Method description
     *
     *
     * @param url
     *
     * @return
     *
     * @throws IOException
     */
    public static InputStream getDocumentAsInputStream(URL url) throws IOException {
        return url.openStream();
    }

    /**
     * Method description
     *
     *
     *
     * @param urlString
     *
     * @return
     *
     * @throws IOException
     * @throws MalformedURLException
     */
    public static InputStream getDocumentAsInputStream(String urlString) throws MalformedURLException, IOException {
        URL url = new URL(urlString);
        return getDocumentAsInputStream(url);
    }

    /**
     * Method description
     *
     *
     * @param url
     *
     * @return
     *
     * @throws IOException
     */
    public static String getDocumentAsString(URL url) throws IOException {
        StringBuffer result = new StringBuffer();
        InputStream inStream = url.openStream();
        int character;
        while ((character = inStream.read()) != -1) {
            result.append((char) character);
        }
        return result.toString();
    }

    /**
     * Method description
     *
     *
     * @param url
     *
     * @return
     *
     * @throws IOException
     * @throws MalformedURLException
     */
    public static String getDocumentAsString(String urlString) throws MalformedURLException, IOException {
        URL url = new URL(urlString);
        return getDocumentAsString(url);
    }
}
