package fr.wbr;

import java.net.URL;
import java.net.HttpURLConnection;
import java.io.*;
import java.util.Properties;

/**
 * Created by IntelliJ IDEA.
 * User: Christophe
 * Date: 25 ao�t 2006
 * Time: 22:23:14
 * To change this template use File | Settings | File Templates.
 */
public class HTTPClient {

    protected URL url_;

    protected HttpURLConnection httpURLConnection_;

    protected SiteThread siteThread_;

    public static final String iso88591 = "ISO-8859-1";

    public static final String windows1252 = "Cp1252";

    public static final String utf8 = "UTF-8";

    /**
     * @param szUrl: String object for the URL
     */
    public HTTPClient(String szUrl, SiteThread siteThread) throws Exception {
        try {
            url_ = new URL(szUrl);
        } catch (Exception e) {
            throw new Exception("Invalid URL '" + szUrl + "'");
        }
        siteThread_ = siteThread;
        siteThread.storeHTTPClient(this);
    }

    /**
     * @param method: String object for client method (POST, GET,...)
     */
    public void connect(String method, String data) throws Exception {
        connect(method, data, null, null);
    }

    /**
     * @param method: String object for client method (POST, GET,...)
     */
    public void connect(String method, String data, Properties properties) throws Exception {
        connect(method, data, null, properties);
    }

    /**
     * @param method: String object for client method (POST, GET,...)
     */
    public void connect(String method, String data, String urlString, Properties properties) throws Exception {
        connect(method, data, urlString, properties, true);
    }

    /**
     * @param method: String object for client method (POST, GET,...)
     */
    public void connect(String method, String data, String urlString, Properties properties, boolean allowredirect) throws Exception {
        if (urlString != null) {
            try {
                url_ = new URL(url_, urlString);
            } catch (Exception e) {
                throw new Exception("Invalid URL");
            }
        }
        try {
            httpURLConnection_ = (HttpURLConnection) url_.openConnection(siteThread_.getProxy());
            httpURLConnection_.setDoInput(true);
            httpURLConnection_.setDoOutput(true);
            httpURLConnection_.setUseCaches(false);
            httpURLConnection_.setRequestMethod(method);
            httpURLConnection_.setRequestProperty("Content-type", "application/x-www-form-urlencoded");
            httpURLConnection_.setInstanceFollowRedirects(allowredirect);
            if (properties != null) {
                for (Object propertyKey : properties.keySet()) {
                    String propertyValue = properties.getProperty((String) propertyKey);
                    if (propertyValue.equalsIgnoreCase("Content-Length")) {
                        httpURLConnection_.setFixedLengthStreamingMode(0);
                    }
                    httpURLConnection_.setRequestProperty((String) propertyKey, propertyValue);
                }
            }
            int connectTimeout = httpURLConnection_.getConnectTimeout();
            if (data != null) {
                post(data);
            }
            httpURLConnection_.connect();
        } catch (Exception e) {
            throw new Exception("Connection failed with url " + url_);
        }
    }

    public void disconnect() {
        httpURLConnection_.disconnect();
    }

    public void post(String s) throws Exception {
        PrintWriter bw = null;
        try {
            bw = new PrintWriter(httpURLConnection_.getOutputStream());
            bw.write(s, 0, s.length());
        } catch (Exception e) {
            throw new Exception("Unable to write to output stream");
        } finally {
            if (bw != null) {
                bw.close();
            }
        }
    }

    HttpURLConnection getHTTURLConnection() {
        return httpURLConnection_;
    }

    String getHeaderValue(String key) {
        return httpURLConnection_.getHeaderField(key);
    }

    int getResponseCode() {
        try {
            return httpURLConnection_.getResponseCode();
        } catch (IOException e) {
            return -1000;
        }
    }

    int getContentLength() {
        return httpURLConnection_.getContentLength();
    }

    public void displayResponse(String filePathToken, StringBuilder response, String charset) throws Exception {
        displayResponse(filePathToken, response, charset, true);
    }

    public void displayResponse(String filePathToken, StringBuilder response, String charset, boolean stopOnFirstHtml) throws Exception {
        String line;
        if (charset == null) {
            charset = HTTPClient.utf8;
        }
        BufferedReader reader = null;
        BufferedWriter writer = null;
        File file = new File("log");
        file.mkdir();
        try {
            String path = "log" + File.separator + "output." + siteThread_.getSiteToken() + (siteThread_.isNonRegression() ? "." + siteThread_.getSearchedTaxon() : "") + (filePathToken == null ? "" : "." + filePathToken) + ".html";
            writer = new BufferedWriter(new FileWriter(path));
            reader = new BufferedReader(new InputStreamReader(httpURLConnection_.getInputStream(), charset));
            while ((line = reader.readLine()) != null) {
                if (writer != null) {
                    writer.write(line);
                    writer.write('\n');
                    writer.flush();
                }
                if (response != null) {
                    response.append(line);
                    response.append('\n');
                }
                if (stopOnFirstHtml && line.contains("</html>")) {
                    break;
                }
            }
        } catch (Exception e) {
            throw new Exception(siteThread_.getSiteToken() + ": Unable to read input stream: " + e.getMessage());
        } finally {
            if (writer != null) {
                writer.close();
            }
            if (reader != null) {
                reader.close();
            }
        }
        if (response.length() == 0) {
            throw new Exception(siteThread_.getSiteToken() + ": Received empty page from url " + url_);
        }
    }
}
