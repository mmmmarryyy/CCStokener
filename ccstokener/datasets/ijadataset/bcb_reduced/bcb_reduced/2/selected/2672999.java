package au.com.bidmetender.util.countryregions;

import java.io.IOException;
import java.io.InputStream;
import java.net.URL;
import java.net.URLConnection;
import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.ParserConfigurationException;
import org.w3c.dom.Document;
import org.xml.sax.SAXException;

public class Connect {

    public static final String WIKI_HTTP = "http://en.wikipedia.org";

    private static final int MAX_RETRY_ATTEMPTS = 3;

    private static final long RETRY_PERIOD = 1000;

    private static final int CONNECTION_TIMEOUT = 20 * 60 * 1000;

    public static final int READ_TIMEOUT = 5 * 60 * 60 * 1000;

    public Document getWebPage(URL url) {
        URL targetUrl = url;
        Document document = null;
        long retries = 0;
        DocumentBuilderFactory factory = DocumentBuilderFactory.newInstance();
        factory.setValidating(false);
        factory.setNamespaceAware(false);
        factory.setIgnoringElementContentWhitespace(true);
        InputStream urlStream = null;
        while (true) {
            try {
                URLConnection urlConnection = targetUrl.openConnection();
                urlConnection.setAllowUserInteraction(false);
                urlConnection.setConnectTimeout(CONNECTION_TIMEOUT);
                urlConnection.setReadTimeout(READ_TIMEOUT);
                Thread.yield();
                urlConnection.connect();
                urlStream = urlConnection.getInputStream();
                String type = urlConnection.getContentType();
                if (type != null && type.startsWith("text/html")) {
                    DocumentBuilder builder = factory.newDocumentBuilder();
                    document = builder.parse(urlStream);
                    urlStream.close();
                } else {
                    if (type != null) {
                        System.out.println("URL Type: '" + type + "' for '" + targetUrl.getPath() + "'");
                    } else {
                        System.out.println("URLConnection Type is null for '" + targetUrl.getPath() + "'");
                    }
                }
                Thread.yield();
                break;
            } catch (IOException e) {
                e.printStackTrace();
                if (urlStream != null) {
                    try {
                        urlStream.close();
                    } catch (IOException e1) {
                    }
                }
                if (retries < MAX_RETRY_ATTEMPTS) {
                    System.out.println(targetUrl.getPath() + " IOException, retry attempt: " + retries);
                    retries++;
                    try {
                        Thread.sleep(retries * RETRY_PERIOD);
                    } catch (InterruptedException e1) {
                        e1.printStackTrace();
                    }
                    continue;
                } else {
                    break;
                }
            } catch (SAXException e) {
                e.printStackTrace();
                if (retries < MAX_RETRY_ATTEMPTS) {
                    System.out.println(targetUrl.getPath() + " SAXException, retry attempt: " + retries);
                    retries++;
                    try {
                        Thread.sleep(retries * RETRY_PERIOD);
                    } catch (InterruptedException e1) {
                        e1.printStackTrace();
                    }
                    continue;
                } else {
                    break;
                }
            } catch (ParserConfigurationException e) {
                e.printStackTrace();
                break;
            }
        }
        return document;
    }

    public static String formatMilliseconds(long ms) {
        long milliseconds = ms;
        long seconds = 0;
        long minutes = 0;
        long hours = 0;
        String result = "";
        if (milliseconds > 1000) {
            seconds = milliseconds / 1000;
            milliseconds -= seconds * 1000;
            if (seconds > 60) {
                minutes = seconds / 60;
                seconds -= minutes * 60;
                if (minutes > 60) {
                    hours = minutes / 60;
                    minutes -= hours * 60;
                    result = result.concat(hours + " hr ");
                }
                result = result.concat(String.format("%2d min ", minutes));
            }
            result = result.concat(String.format("%2d sec ", seconds));
        }
        result = result.concat(String.format("%3d ms", milliseconds));
        return result;
    }
}
