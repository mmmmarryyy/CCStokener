package za.co.skywalk.isgd;

import java.io.BufferedInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.UnsupportedEncodingException;
import java.net.HttpURLConnection;
import java.net.MalformedURLException;
import java.net.URL;
import java.net.URLEncoder;

/**
 *
 * @author quintin
 */
public class MakeTiny {

    private static final String IS_GD_API_URL = "http://is.gd/api.php?longurl=";

    private static final String ENCODING = "UTF-8";

    private static final int BUF_SIZE = 1024;

    private URL url;

    /**
   * Thrown when compression failed on the part of is.gd (not for IO exceptions
   * or invalid response codes, etc.)
   */
    public static class CompressionFailedException extends Exception {

        public CompressionFailedException(String msg) {
            super(msg);
        }

        public CompressionFailedException(String msg, Exception e) {
            super(msg, e);
        }
    }

    /**
   * Constructs a new compressor for a given URL
   * @param url
   */
    public MakeTiny(URL url) {
        this.url = url;
    }

    /**
   * Compresses the URL.
   *
   * @return the compressed URL
   * @throws java.io.IOException
   */
    public URL getTinyURL() throws IOException, CompressionFailedException {
        HttpURLConnection tinyCon = makeConnection();
        return getTinyURL(tinyCon);
    }

    /**
   * Fetches the compressed URL from the inputstream
   * @param inputStream
   * @return
   */
    private URL getTinyURL(HttpURLConnection tinyCon) throws IOException, CompressionFailedException {
        int responseCode = tinyCon.getResponseCode();
        InputStream inputStream;
        if (responseCode == 200) {
            inputStream = tinyCon.getInputStream();
        } else if (responseCode == 500) {
            inputStream = tinyCon.getErrorStream();
        } else {
            throw new IOException("Error: " + tinyCon.getResponseMessage() + " (HTTP " + responseCode + ")");
        }
        BufferedInputStream in = new BufferedInputStream(inputStream);
        StringBuilder result = new StringBuilder();
        byte[] data = new byte[BUF_SIZE];
        int read = 0;
        while ((read = in.read(data)) > 0) {
            result.append(new String(data, 0, read, ENCODING));
        }
        if (responseCode == 500) {
            throw new CompressionFailedException(result.toString());
        } else {
            try {
                return new URL(result.toString());
            } catch (MalformedURLException e) {
                throw new CompressionFailedException("An invalid compressed URL was returned by is.gd: " + result.toString(), e);
            }
        }
    }

    /**
   * Makes the connection to is.gd
   * @return the input stream for the created connection
   */
    private HttpURLConnection makeConnection() throws IOException {
        String encodedUrl;
        try {
            encodedUrl = URLEncoder.encode(url.toExternalForm(), ENCODING);
        } catch (UnsupportedEncodingException e) {
            encodedUrl = url.toExternalForm();
        }
        try {
            URL url = new URL(IS_GD_API_URL + encodedUrl);
            return (HttpURLConnection) url.openConnection();
        } catch (MalformedURLException e) {
            throw new IOException("Failed to create connection for compressing URL.", e);
        }
    }
}
