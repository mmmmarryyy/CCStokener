package com.jxva.http;

import java.io.BufferedInputStream;
import java.io.BufferedReader;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.PrintWriter;
import java.net.Authenticator;
import java.net.HttpURLConnection;
import java.net.MalformedURLException;
import java.net.PasswordAuthentication;
import java.net.URL;
import java.util.Properties;
import com.jxva.entity.Encoding;
import com.jxva.util.Assert;

/**
 *
 * @author  The Jxva Framework Foundation
 * @since   1.0
 * @version 2009-04-01 14:13:14 by Jxva
 */
public class HttpTransfer {

    private static final int BUFFER_SIZE = 4096;

    private String url;

    public HttpTransfer(String url) {
        this.url = url;
    }

    public void setProxy(String proxyHost, String proxyPort) {
        Properties prop = System.getProperties();
        prop.setProperty("proxySet", "true");
        prop.setProperty("http.proxyHost", proxyHost);
        prop.setProperty("http.proxyPort", proxyPort);
    }

    public void setProxyAuthorization(final String username, final String password) {
        Authenticator.setDefault(new Authenticator() {

            protected PasswordAuthentication getPasswordAuthentication() {
                return new PasswordAuthentication(username, new String(password).toCharArray());
            }
        });
    }

    public <T> T execute(HttpURLConnectionCallback<T> action) throws HttpException {
        Assert.notNull(action, "HttpURLConnectionCallback object must not be null");
        try {
            return action.doInConnection((HttpURLConnection) new URL(url).openConnection());
        } catch (MalformedURLException e) {
            throw new HttpException(url, e);
        } catch (IOException e) {
            throw new HttpException(url, e);
        }
    }

    public String post(final String postParam, final String encoding) {
        return execute(new HttpURLConnectionCallback<String>() {

            public String doInConnection(HttpURLConnection conn) throws HttpException {
                try {
                    conn.setRequestMethod("POST");
                    conn.setRequestProperty("Content-Type", "text/html;charset=UTF-8");
                    conn.setRequestProperty("Accept-Charset", "UTF-8");
                    conn.setDoOutput(true);
                    conn.setDoInput(true);
                    conn.connect();
                    PrintWriter out = new PrintWriter(conn.getOutputStream());
                    out.print(postParam);
                    out.flush();
                    out.close();
                    int res = conn.getResponseCode();
                    if (res == 200) {
                        BufferedReader reader = new BufferedReader(new InputStreamReader(conn.getInputStream(), encoding));
                        String line = null;
                        StringBuilder sb = new StringBuilder();
                        while ((line = reader.readLine()) != null) {
                            sb.append(line).append('\n');
                        }
                        return sb.toString();
                    } else {
                        throw new HttpException("accept data found error from " + url);
                    }
                } catch (IOException e) {
                    throw new HttpException(e);
                } finally {
                    conn.disconnect();
                }
            }
        });
    }

    public String get(final String encoding) {
        return execute(new HttpURLConnectionCallback<String>() {

            public String doInConnection(HttpURLConnection conn) throws HttpException {
                BufferedReader reader = null;
                try {
                    conn.setRequestProperty("Content-Type", "text/html;charset=UTF-8");
                    conn.setRequestProperty("Accept-Charset", "UTF-8");
                    conn.setDoInput(true);
                    conn.setDoOutput(true);
                    conn.setRequestMethod("GET");
                    conn.setUseCaches(false);
                    reader = new BufferedReader(new InputStreamReader(conn.getInputStream(), encoding));
                    String line = null;
                    StringBuilder sb = new StringBuilder();
                    while ((line = reader.readLine()) != null) {
                        sb.append(line).append('\n');
                    }
                    return sb.toString();
                } catch (IOException e) {
                    throw new HttpException(e);
                } finally {
                    if (reader != null) try {
                        reader.close();
                    } catch (IOException e) {
                    }
                    conn.disconnect();
                }
            }
        });
    }

    public boolean saveAsFile(final String filename) {
        Assert.notNull(filename, "filename must not be null");
        return execute(new HttpURLConnectionCallback<Boolean>() {

            public Boolean doInConnection(HttpURLConnection conn) throws HttpException {
                FileOutputStream fos = null;
                BufferedInputStream bis = null;
                try {
                    bis = new BufferedInputStream(conn.getInputStream());
                    fos = new FileOutputStream(filename);
                    byte[] buf = new byte[BUFFER_SIZE];
                    int size = 0;
                    while ((size = bis.read(buf)) != -1) {
                        fos.write(buf, 0, size);
                    }
                    return true;
                } catch (IOException e) {
                    throw new HttpException(e);
                } finally {
                    if (fos != null) try {
                        fos.flush();
                        fos.close();
                    } catch (IOException e) {
                    }
                    if (bis != null) try {
                        bis.close();
                    } catch (IOException e) {
                    }
                    conn.disconnect();
                }
            }
        });
    }

    /**
	 * @param args
	 */
    public static void main(String[] args) {
        System.out.println(new HttpTransfer("http://www.baidu.com/").get(Encoding.UTF_8));
        System.out.println(new HttpTransfer("http://www.baidu.com/img/baidu_logo.gif").saveAsFile("C:/baidu.gif"));
    }
}
