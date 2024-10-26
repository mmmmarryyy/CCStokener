package org.net2map.misc;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.UnsupportedEncodingException;
import java.net.InetAddress;
import java.net.MalformedURLException;
import java.net.URL;
import java.net.URLConnection;
import java.net.UnknownHostException;
import org.apache.log4j.Logger;

/**
 * @author moi
 *
 */
public final class NetConfigDetector {

    private static final Logger LOGGER = Logger.getLogger(NetConfigDetector.class);

    /**
	 * Downloads the web page specified by the given <code>URL</code>
	 * object.
	 *
	 * @param url The <code>URL</code> object that the page will be
	 * downloaded from.
	 *
	 * @return A <code>String</code> containing the contents of the
	 * page.  No extra parsing work is done on the page.  */
    public static String getWebPage(final URL url) {
        StringBuffer page = new StringBuffer();
        try {
            URLConnection connection = url.openConnection();
            String line;
            BufferedReader in;
            if (connection.getContentEncoding() == null) {
                in = new BufferedReader(new InputStreamReader(url.openStream()));
            } else {
                in = new BufferedReader(new InputStreamReader(url.openStream(), connection.getContentEncoding()));
            }
            while ((line = in.readLine()) != null) {
                page.append(line).append('\n');
            }
            in.close();
        } catch (UnsupportedEncodingException e) {
            System.err.println("WebPage.getWebPage(): " + e);
        } catch (IOException e) {
            System.err.println("WebPage.getWebPage(): " + e);
        }
        return page.toString();
    }

    /**
	 * Retrieves a named string on a web page.
	 * For example, if you have a PHP web page that says "Your IP address is:123.123.123.123"
	 * calling getStringOnWebPage("Your IP address is:","www.yoursite.org/getIP.php") will give you your IP address.
	 * The requested string should be followed by a BR html tag for this to work correctly.
	 * @param paramStringName The text which is just before the string you want to retrieve.
	 * @param paramWebPage The web page's address
	 * @return The string, or null if the string could not be retrieved.
	 */
    public static String getStringInWebPage(final String _stringName, final String _url) {
        String result = null;
        try {
            String pageContent = getWebPage(new URL(_url));
            int namePosition = pageContent.indexOf(_stringName);
            if (-1 != namePosition) {
                int endOfStringPosition = pageContent.indexOf("<br>", namePosition + _stringName.length());
                if (-1 != endOfStringPosition) {
                    result = pageContent.substring(namePosition + _stringName.length(), endOfStringPosition);
                } else {
                    LOGGER.fatal("Couldn't find end of string!");
                }
            }
        } catch (MalformedURLException e) {
            e.printStackTrace();
        }
        return (result);
    }

    public static String getMyHostName() {
        String result = null;
        try {
            InetAddress inetAddress = InetAddress.getLocalHost();
            result = inetAddress.getHostName();
        } catch (UnknownHostException e) {
            e.printStackTrace();
        }
        return (result);
    }

    public static String getPublicIPstr() {
        return (getStringInWebPage("Your public IP address is:", "http://www.net2map.org/client_ip.php"));
    }

    /**
	 * @param args
	 */
    public static void main(String[] args) {
        System.out.println("Public IP:" + getStringInWebPage("Your public IP address is:", "http://www.net2map.org/client_ip.php"));
        System.out.println("My host name: " + getMyHostName());
    }
}
