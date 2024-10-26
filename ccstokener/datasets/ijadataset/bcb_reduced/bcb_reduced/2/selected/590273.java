package net.sf.jerkbot.util;

import org.apache.commons.lang.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import java.io.BufferedReader;
import java.io.Closeable;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.URL;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Properties;

/**
 * @author Yves Zoundi <yveszoundi at users dot sf dot net>
 *         Utility class for IO related tasks, didn't want to introduce commons-io for few things
 * @version 0.0.1
 */
public class IOUtil {

    /**
     * The Constant Log.
     */
    private static final Logger Log = LoggerFactory.getLogger(IOUtil.class.getName());

    /**
     * Returns the lines of a given string in a list
     *
     * @param is The inputstream to read
     * @return the lines of a given string in a list
     */
    public static List<String> addLinesToList(InputStream is) {
        List<String> aList = new ArrayList<String>();
        BufferedReader reader = null;
        String line = null;
        try {
            reader = new BufferedReader(new InputStreamReader(is, "UTF-8"));
            while ((line = reader.readLine()) != null) {
                aList.add(line);
            }
        } catch (Exception ioe) {
            Log.error(ioe.getMessage(), ioe);
        } finally {
            closeQuietly(reader);
            closeQuietly(is);
        }
        return aList;
    }

    /**
     * Returns the values of a properties file
     * @param is The inputstream of the properties file
     * @return the values of a properties file
     */
    public static Collection<Object> getPropertiesValues(InputStream is) {
        Properties props = new Properties();
        try {
            props.load(is);
        } catch (Exception ioe) {
            Log.error(ioe.getMessage(), ioe);
        } finally {
            IOUtil.closeQuietly(is);
        }
        return props.values();
    }

    /**
     * Gets the tiny url.
     *
     * @param fullUrl the full url
     * @return the tiny url
     * @throws Exception the exception
     */
    public static String getTinyUrl(String fullUrl) {
        String shortURL = fullUrl;
        HttpURLConnection urlConnection = null;
        try {
            URL url = new URL("http://is.gd/api.php?longurl=" + fullUrl);
            urlConnection = (HttpURLConnection) url.openConnection();
            urlConnection.setRequestMethod("GET");
            urlConnection.setInstanceFollowRedirects(true);
            urlConnection.setConnectTimeout(7500);
            urlConnection.connect();
            int tucode = urlConnection.getResponseCode();
            if (tucode == HttpURLConnection.HTTP_OK) {
                BufferedReader reader = null;
                try {
                    reader = new BufferedReader(new InputStreamReader(urlConnection.getInputStream()));
                    shortURL = reader.readLine();
                } catch (IOException ioe) {
                    Log.error(ioe.getMessage(), ioe);
                } finally {
                    closeQuietly(reader);
                }
            }
        } catch (Exception e) {
            Log.error(e.getMessage(), e);
        } finally {
            if (urlConnection != null) {
                urlConnection.disconnect();
            }
        }
        if (StringUtils.isBlank(shortURL)) {
            return fullUrl;
        }
        return shortURL;
    }

    /**
     * Close a resource
     * @param closeable a resource
     */
    public static void closeQuietly(Closeable closeable) {
        try {
            if (closeable != null) {
                closeable.close();
            }
        } catch (IOException ioe) {
            Log.error(ioe.getMessage(), ioe);
        }
    }
}
