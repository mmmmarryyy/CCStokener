package servers;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.UnsupportedEncodingException;
import java.net.InetAddress;
import java.net.MalformedURLException;
import java.net.NetworkInterface;
import java.net.SocketException;
import java.net.URL;
import java.net.URLConnection;
import java.net.URLEncoder;
import java.net.UnknownHostException;
import java.util.Enumeration;
import org.apache.log4j.Logger;
import org.apache.log4j.PropertyConfigurator;
import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;
import utils.ConsoleIO;

public class Google extends Thread {

    public Logger logger = Logger.getLogger("Google");

    public static final int SEARCH_WEB = 0;

    public static final int SEARCH_LOCAL = 1;

    public static final int SEARCH_VIDEO = 2;

    public static final int SEARCH_NEWS = 3;

    public static final int SEARCH_IMAGE = 4;

    public static final int SEARCH_BOOK = 5;

    private final String WEB = "http://ajax.googleapis.com/ajax/services/search/web";

    private final String LOCAL = "http://ajax.googleapis.com/ajax/services/search/local";

    private final String VIDEO = "http://ajax.googleapis.com/ajax/services/search/video";

    private final String NEWS = "http://ajax.googleapis.com/ajax/services/search/news";

    private final String IMAGE = "http://ajax.googleapis.com/ajax/services/search/images";

    private final String BOOK = "http://ajax.googleapis.com/ajax/services/search/books";

    private final String HTTP_REFERER = "http://www.olaconmuchospeces.com.ar";

    private final String KEY = "ABQIAAAAyvu57pK9OhMkfbiRi38A1xRlLa_u2cIblWfv-yIMGBodIxK-8BQM_u0TDLEXURoqn8jRQYO1IQ7c7A";

    private String q = null;

    private String v = "1.0";

    private String userip = null;

    private String rsz = "large";

    private String hl = "es";

    private String start = "0";

    private String callback = "cb";

    private String safe = "off";

    private String lr = "lang_es";

    private String gl = "ar";

    private String imgsz = "medium";

    private String imgtype = "face";

    private String num = "20";

    private String as_filetype = "jpg";

    private GoogleClient sc;

    private URL url = null;

    private Thread runner;

    public Google(GoogleClient sc) {
        this.sc = sc;
        userip = getIp();
    }

    /**
	 * 	
	 * @param searcher que tipo de busqueda quiero efectuar
	 * @param query la frase a buscar
	 * @param limit cuantos resultados como maximo quiero (1-8)
	 * @param currentIndex que p�gina del recorset quiero consultar
	 */
    public void search(int searcher, String query, int currentIndex) {
        logger.info("STARTING GOOGLE SEARCH");
        start = Integer.toString(currentIndex);
        runner = new Thread(this, "Google Search in progress");
        switch(searcher) {
            case 0:
                compileUrlWeb(query);
                break;
            case 1:
                break;
            case 2:
                break;
            case 3:
                break;
            case 4:
                compileUrlImages(query);
                break;
            case 5:
                compileUrlBooks(query);
                break;
            default:
                break;
        }
        runner.start();
    }

    protected void searchBooks(String query) {
    }

    private void compileUrlBooks(String query) {
        logger.info("Compiling BOOK SEARCH with: " + query);
        try {
            q = URLEncoder.encode(query, "UTF-8");
            url = new URL(BOOK + "?" + "&v=" + v + "&key=" + KEY + "&userip=" + userip + "&q=" + q + "&rsz=" + rsz + "&hl=" + hl + "&start=" + start);
        } catch (MalformedURLException e) {
            e.printStackTrace();
        } catch (UnsupportedEncodingException e) {
            e.printStackTrace();
        }
    }

    private void compileUrlImages(String query) {
        logger.info("Compiling IMAGE SEARCH with: " + query);
        try {
            q = URLEncoder.encode(query, "UTF-8");
            url = new URL(IMAGE + "?" + "&v=" + v + "&key=" + KEY + "&userip=" + userip + "&q=" + q + "&rsz=" + rsz + "&hl=" + hl + "&start=" + start + "&imgtype=" + imgtype + "&num=" + num + "&as_filetype=" + as_filetype);
        } catch (MalformedURLException e) {
            e.printStackTrace();
        } catch (UnsupportedEncodingException e) {
            e.printStackTrace();
        }
    }

    private void compileUrlWeb(String query) {
        try {
            q = URLEncoder.encode(query, "UTF-8");
            url = new URL(WEB + "?" + "&v=" + v + "&key=" + KEY + "&userip=" + userip + "&q=" + q + "&rsz=" + rsz + "&hl=" + hl + "&start=" + start);
        } catch (MalformedURLException e) {
            e.printStackTrace();
        } catch (UnsupportedEncodingException e) {
            e.printStackTrace();
        }
    }

    public synchronized void run() {
        logger.info("SEARCH STARTED");
        JSONObject json = null;
        logger.info("Opening urlConnection");
        URLConnection connection = null;
        try {
            connection = url.openConnection();
            connection.addRequestProperty("Referer", HTTP_REFERER);
        } catch (IOException e) {
            logger.warn("PROBLEM CONTACTING GOOGLE");
            e.printStackTrace();
        }
        String line;
        StringBuilder builder = new StringBuilder();
        BufferedReader reader;
        try {
            reader = new BufferedReader(new InputStreamReader(connection.getInputStream()));
            while ((line = reader.readLine()) != null) {
                builder.append(line);
            }
        } catch (IOException e) {
            logger.warn("PROBLEM RETREIVING DATA FROM GOOGLE");
            e.printStackTrace();
        }
        logger.info("Google RET: " + builder.toString());
        try {
            json = new JSONObject(builder.toString());
            json.append("query", q);
        } catch (JSONException e) {
            logger.warn("PROBLEM RETREIVING DATA FROM GOOGLE");
            e.printStackTrace();
        }
        sc.onSearchFinished(json);
    }

    private String getIp() {
        Enumeration netInterfaces;
        String ip = null;
        try {
            netInterfaces = NetworkInterface.getNetworkInterfaces();
            while (netInterfaces.hasMoreElements()) {
                NetworkInterface ni = (NetworkInterface) netInterfaces.nextElement();
                InetAddress addr = (InetAddress) ni.getInetAddresses().nextElement();
                if (!addr.isLoopbackAddress() && addr.isSiteLocalAddress() && !(addr.getHostAddress().indexOf(":") > -1)) {
                    ip = addr.getHostAddress();
                }
            }
        } catch (SocketException e1) {
            e1.printStackTrace();
        }
        return ip;
    }
}
