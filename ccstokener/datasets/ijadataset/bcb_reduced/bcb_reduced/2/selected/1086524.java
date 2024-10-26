package net.sf.vorg.vorgautopilot.core;

import java.io.BufferedReader;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.OutputStreamWriter;
import java.io.UnsupportedEncodingException;
import java.io.Writer;
import java.net.HttpURLConnection;
import java.net.MalformedURLException;
import java.net.ProtocolException;
import java.net.URL;
import java.net.URLEncoder;
import java.util.Enumeration;
import java.util.Hashtable;
import java.util.logging.Level;
import java.util.logging.Logger;

public class VORGURLRequest {

    private static Logger logger = Logger.getLogger("net.sf.vorg.vorgautopilot.internals");

    static {
        logger.setLevel(Level.OFF);
    }

    private static String protocol = "http";

    private static String gameHost = "volvogame.virtualregatta.com";

    private String request = "";

    private URL url;

    private HttpURLConnection conn;

    public VORGURLRequest(String request) {
        logger.info("Preparing URL: " + protocol + "://" + gameHost + request);
        this.request = request;
    }

    public void executeGET(String cookies) throws MalformedURLException, ProtocolException, IOException {
        url = new URL(protocol, gameHost, request);
        conn = (HttpURLConnection) url.openConnection();
        conn.setRequestMethod("GET");
        conn.setRequestProperty("Content-Type", "application/x-www-form-urlencoded");
        conn.setRequestProperty("Content-Language", "en-US");
        conn.setRequestProperty("User-Agent", "Mozilla/5.0 (Windows; U; Windows NT 5.1; en-GB; rv:1.9.0.5) Gecko/2008120122 Firefox/3.0.5");
        conn.setRequestProperty("Accept", "text/javascript, text/html, application/xml, text/xml");
        conn.setRequestProperty("Referer", "http://www.volvooceanracegame.org/home.php");
        if (null != cookies) conn.setRequestProperty("Cookie", cookies);
        conn.setUseCaches(false);
        conn.setDoInput(false);
        conn.setDoOutput(true);
    }

    public void excutePOST(String cookies, Hashtable<String, String> parameters) {
        try {
            StringBuffer urlParameters = new StringBuffer();
            Enumeration<String> pit = parameters.keys();
            int paramCounter = 0;
            while (pit.hasMoreElements()) {
                String name = pit.nextElement();
                String value = parameters.get(name);
                if (paramCounter > 1) urlParameters.append('&');
                urlParameters.append(name).append(URLEncoder.encode(value, "UTF-8"));
                paramCounter++;
            }
            url = new URL(protocol, gameHost, request);
            conn = (HttpURLConnection) url.openConnection();
            conn.setRequestMethod("POST");
            conn.setRequestProperty("Content-Type", "application/x-www-form-urlencoded");
            conn.setRequestProperty("Content-Language", "en-US");
            conn.setRequestProperty("User-Agent", "Mozilla/5.0 (Windows; U; Windows NT 5.1; en-GB; rv:1.9.0.5) Gecko/2008120122 Firefox/3.0.5");
            conn.setRequestProperty("Accept", "text/javascript, text/html, application/xml, text/xml");
            conn.setRequestProperty("Referer", "http://www.volvooceanracegame.org/home.php");
            conn.setRequestProperty("Cookie", cookies);
            conn.setUseCaches(false);
            conn.setDoInput(true);
            conn.setDoOutput(true);
            Writer post = new OutputStreamWriter(conn.getOutputStream());
            post.write(urlParameters.toString());
            post.write("\r\n");
            post.flush();
        } catch (MalformedURLException mue) {
            mue.printStackTrace();
        } catch (UnsupportedEncodingException uee) {
            uee.printStackTrace();
        } catch (IOException ioe) {
            ioe.printStackTrace();
        }
    }

    public String getData() {
        conn.setDoInput(true);
        StringBuffer data = new StringBuffer();
        try {
            BufferedReader in = new BufferedReader(new InputStreamReader(conn.getInputStream()));
            String line = null;
            while ((line = in.readLine()) != null) data.append(line).append("\n");
            String headerName = null;
            for (int i = 1; (headerName = conn.getHeaderFieldKey(i)) != null; i++) if (headerName.equals("Set-Cookie")) {
                String cookie = conn.getHeaderField(i);
                cookie = cookie.substring(0, cookie.indexOf(";"));
                String cookieName = cookie.substring(0, cookie.indexOf("="));
                String cookieValue = cookie.substring(cookie.indexOf("=") + 1, cookie.length());
            }
            in.close();
        } catch (MalformedURLException ex) {
            System.err.println(ex);
        } catch (FileNotFoundException ex) {
            System.err.println("Failed to open stream to URL: " + ex);
        } catch (IOException ex) {
            System.err.println("Error reading URL content: " + ex);
        }
        return data.toString();
    }
}
