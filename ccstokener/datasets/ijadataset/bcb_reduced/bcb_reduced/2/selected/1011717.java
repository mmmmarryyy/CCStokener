package com.marcolino.utils;

import java.io.BufferedReader;
import java.io.ByteArrayOutputStream;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.URL;
import java.nio.charset.Charset;
import org.apache.http.impl.client.DefaultHttpClient;
import org.apache.http.params.HttpConnectionParams;
import org.apache.http.params.HttpParams;

public class HttpUtil {

    private static HttpUtil _instance = null;

    private static DefaultHttpClient httpClient = new DefaultHttpClient();

    private static HttpURLConnection urlc;

    private HttpUtil() {
        HttpParams params = httpClient.getParams();
        HttpConnectionParams.setConnectionTimeout(params, 60000);
        HttpConnectionParams.setSoTimeout(params, 60000);
    }

    public static HttpUtil getInstance() {
        if (_instance == null) _instance = new HttpUtil();
        return _instance;
    }

    /**
     * Gets data from URL as String throws {@link RuntimeException} If anything goes wrong
     * 
     * @return The content of the URL as a String
     * @throws ServerConnectinoException
     */
    public String getDataAsString(String url) throws Exception {
        try {
            String responseBody = "";
            urlc = (HttpURLConnection) new URL(url).openConnection();
            urlc.setUseCaches(false);
            urlc.setRequestProperty("Content-Type", "application/x-www-form-urlencoded");
            urlc.setRequestProperty("User-Agent", "Mozilla/5.0 (X11; U; Linux x86_64; en-GB; rv:1.9.1.9) Gecko/20100414 Iceweasel/3.5.9 (like Firefox/3.5.9)");
            urlc.setRequestProperty("Accept-Encoding", "gzip");
            InputStreamReader re = new InputStreamReader(urlc.getInputStream());
            BufferedReader rd = new BufferedReader(new InputStreamReader(urlc.getInputStream()));
            String line = "";
            while ((line = rd.readLine()) != null) {
                responseBody += line;
                responseBody += "\n";
            }
            rd.close();
            re.close();
            return responseBody;
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    /**
     * Gets data from URL as byte[] throws {@link RuntimeException} If anything goes wrong
     * 
     * @return The content of the URL as a byte[]
     * @throws ServerConnectinoException
     */
    public byte[] getDataAsByteArray(String url) {
        try {
            byte[] dat = null;
            urlc = (HttpURLConnection) new URL(url).openConnection();
            urlc.setDoOutput(true);
            urlc.setUseCaches(false);
            urlc.setRequestMethod("POST");
            urlc.setRequestProperty("Content-Type", "application/x-www-form-urlencoded");
            urlc.setRequestProperty("User-Agent", "Mozilla/5.0 (X11; U; Linux x86_64; en-GB; rv:1.9.1.9) Gecko/20100414 Iceweasel/3.5.9 (like Firefox/3.5.9)");
            urlc.setRequestProperty("Accept-Encoding", "gzip");
            InputStream is = urlc.getInputStream();
            int len = urlc.getContentLength();
            if (len < 0) {
                ByteArrayOutputStream bao = new ByteArrayOutputStream();
                byte[] buf = new byte[4096];
                for (; ; ) {
                    int nb = is.read(buf);
                    if (nb <= 0) break;
                    bao.write(buf, 0, nb);
                }
                dat = bao.toByteArray();
                bao.close();
            } else {
                dat = new byte[len];
                int i = 0;
                while (i < len) {
                    int n = is.read(dat, i, len - i);
                    if (n <= 0) break;
                    i += n;
                }
            }
            is.close();
            return dat;
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    /**
     * Gets data from URL as char[] throws {@link RuntimeException} If anything goes wrong
     * 
     * @return The content of the URL as a char[]
     * @throws ServerConnectinoException
     */
    public char[] getDataAsCharArray(String url) {
        try {
            char[] dat = null;
            urlc = (HttpURLConnection) new URL(url).openConnection();
            urlc.setDoOutput(true);
            urlc.setUseCaches(false);
            urlc.setRequestMethod("POST");
            urlc.setRequestProperty("Content-Type", "application/x-www-form-urlencoded");
            urlc.setRequestProperty("User-Agent", "Mozilla/5.0 (X11; U; Linux x86_64; en-GB; rv:1.9.1.9) Gecko/20100414 Iceweasel/3.5.9 (like Firefox/3.5.9)");
            urlc.setRequestProperty("Accept-Encoding", "gzip");
            InputStream is = urlc.getInputStream();
            BufferedReader reader = new BufferedReader(new InputStreamReader(is, Charset.defaultCharset()));
            int len = urlc.getContentLength();
            dat = new char[len];
            int i = 0;
            int c;
            while ((c = reader.read()) != -1) {
                char character = (char) c;
                dat[i] = character;
                i++;
            }
            is.close();
            return dat;
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }
}
