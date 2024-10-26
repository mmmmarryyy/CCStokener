package bgg4j.http;

import java.io.BufferedInputStream;
import java.io.BufferedOutputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.HttpURLConnection;
import java.net.URL;
import java.util.HashMap;
import java.util.Map;
import bgg4j.BggException;

public class HttpClient {

    private final int UNAUTHORIZED = 401;

    private final int FORBIDDEN = 403;

    private int retryCount = 1;

    private int retryIntervalMillis = 10000;

    public Response get(String url) throws BggException {
        System.out.println(url);
        return httpRequest(url, null);
    }

    public Response get(String url, String charset) throws BggException {
        System.out.println(url);
        return httpRequest(url, charset);
    }

    public void getFile(String url, String filepath) throws BggException {
        System.out.println(url);
        int retry = retryCount + 1;
        lastURL = url;
        for (retriedCount = 0; retriedCount < retry; retriedCount++) {
            int responseCode = -1;
            try {
                HttpURLConnection con = null;
                BufferedInputStream bis = null;
                OutputStream osw = null;
                try {
                    con = (HttpURLConnection) new URL(url).openConnection();
                    con.setDoInput(true);
                    setHeaders(con);
                    con.setRequestMethod("GET");
                    responseCode = con.getResponseCode();
                    bis = new BufferedInputStream(con.getInputStream());
                    int data;
                    BufferedOutputStream bos = new BufferedOutputStream(new FileOutputStream(filepath));
                    while ((data = bis.read()) != -1) bos.write(data);
                    bos.flush();
                    bos.close();
                    break;
                } finally {
                    try {
                        bis.close();
                    } catch (Exception ignore) {
                    }
                    try {
                        osw.close();
                    } catch (Exception ignore) {
                    }
                    try {
                        con.disconnect();
                    } catch (Exception ignore) {
                    }
                }
            } catch (IOException ioe) {
                if (responseCode == UNAUTHORIZED || responseCode == FORBIDDEN) {
                    throw new BggException(ioe.getMessage(), responseCode);
                }
                if (retriedCount == retryCount) {
                    throw new BggException(ioe.getMessage(), responseCode);
                }
            }
            try {
                Thread.sleep(retryIntervalMillis);
            } catch (InterruptedException ignore) {
            }
        }
    }

    int retriedCount = 0;

    String lastURL;

    private Response httpRequest(String url, String charset) throws BggException {
        int retry = retryCount + 1;
        Response res = null;
        lastURL = url;
        for (retriedCount = 0; retriedCount < retry; retriedCount++) {
            int responseCode = -1;
            try {
                HttpURLConnection con = null;
                InputStream is = null;
                OutputStream osw = null;
                try {
                    con = (HttpURLConnection) new URL(url).openConnection();
                    con.setDoInput(true);
                    setHeaders(con);
                    con.setRequestMethod("GET");
                    responseCode = con.getResponseCode();
                    is = con.getInputStream();
                    if (charset != null) {
                        res = new Response(con.getResponseCode(), is, charset);
                    } else {
                        res = new Response(con.getResponseCode(), is);
                    }
                    break;
                } finally {
                    try {
                        is.close();
                    } catch (Exception ignore) {
                    }
                    try {
                        osw.close();
                    } catch (Exception ignore) {
                    }
                    try {
                        con.disconnect();
                    } catch (Exception ignore) {
                    }
                }
            } catch (IOException ioe) {
                if (responseCode == UNAUTHORIZED || responseCode == FORBIDDEN) {
                    throw new BggException(ioe.getMessage(), responseCode);
                }
                if (retriedCount == retryCount) {
                    throw new BggException(ioe.getMessage(), responseCode);
                }
            }
            try {
                Thread.sleep(retryIntervalMillis);
            } catch (InterruptedException ignore) {
            }
        }
        return res;
    }

    private void setHeaders(HttpURLConnection connection) {
        for (String key : requestHeaders.keySet()) {
            connection.addRequestProperty(key, requestHeaders.get(key));
        }
    }

    private Map<String, String> requestHeaders = new HashMap<String, String>();
}
