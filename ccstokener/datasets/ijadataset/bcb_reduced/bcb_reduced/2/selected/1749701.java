package com.imoresoft.magic.app.weather;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.MalformedURLException;
import java.net.URL;
import com.imoresoft.magic.property.AppConstants;

/**
 * @author
 * @Date: 2009-5-11 Time: 13:34:10 模拟Http访问的工具类
 */
public class HttpConnectionUtil {

    public static String getHttpContent(String url) {
        return getHttpContent(url, AppConstants.APPLICATION_ENCODING);
    }

    public static String getHttpContent(String url, String charSet) {
        HttpURLConnection connection = null;
        String content = "";
        try {
            URL address_url = new URL(url);
            connection = (HttpURLConnection) address_url.openConnection();
            System.setProperty("sun.net.client.defaultConnectTimeout", "30000");
            System.setProperty("sun.net.client.defaultReadTimeout", "30000");
            int response_code = connection.getResponseCode();
            if (response_code == HttpURLConnection.HTTP_OK) {
                InputStream in = connection.getInputStream();
                BufferedReader reader = new BufferedReader(new InputStreamReader(in, charSet));
                String line = null;
                while ((line = reader.readLine()) != null) {
                    content += line;
                }
            }
        } catch (MalformedURLException e) {
        } catch (IOException e) {
        } finally {
            if (connection != null) {
                connection.disconnect();
            }
        }
        return content;
    }

    public static void main(String[] args) {
        String content = HttpConnectionUtil.getHttpContent("http://www.baidu.com");
        System.out.println("content = " + content);
    }
}
