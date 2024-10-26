package com.netease.tlive.util;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.UnsupportedEncodingException;
import java.net.HttpURLConnection;
import java.net.MalformedURLException;
import java.net.URL;
import java.util.HashMap;
import java.util.Properties;
import java.util.Set;

public class HttpUtil {

    @SuppressWarnings("unchecked")
    public static String generateRequestUrl(String url, Properties props) {
        if (null == props || props.isEmpty()) {
            return "";
        }
        StringBuffer sb = new StringBuffer();
        sb.append(url).append("?");
        Set keys = props.keySet();
        boolean ifFirst = true;
        for (Object key : keys) {
            if (!ifFirst) {
                sb.append("&");
            } else {
                ifFirst = false;
            }
            sb.append(key).append("=").append(props.get(key));
        }
        return sb.toString();
    }

    @SuppressWarnings("unchecked")
    public static byte[] generatePostBody(Properties props, String encoding) {
        if (null == props || props.isEmpty()) {
            return new byte[0];
        } else {
            StringBuffer sb = new StringBuffer();
            Set keys = props.keySet();
            boolean ifFirst = true;
            for (Object key : keys) {
                if (!ifFirst) {
                    sb.append("&");
                } else {
                    ifFirst = false;
                }
                sb.append(key).append("=").append(props.get(key));
            }
            try {
                return sb.toString().getBytes("UTF-8");
            } catch (UnsupportedEncodingException e) {
                e.printStackTrace();
            }
        }
        return new byte[0];
    }

    public static String getUrlContent(String urlString) {
        StringBuffer sb = new StringBuffer();
        String line;
        try {
            URL url = new URL(urlString);
            HttpURLConnection connection = (HttpURLConnection) url.openConnection();
            BufferedReader reader = new BufferedReader(new InputStreamReader(connection.getInputStream()));
            while ((line = reader.readLine()) != null) {
                sb.append(line);
            }
            reader.close();
            return sb.toString();
        } catch (MalformedURLException e) {
            e.printStackTrace();
            return null;
        } catch (IOException e) {
            e.printStackTrace();
            return null;
        }
    }

    public static HashMap<String, String> parseResponseMessage(String responseString) {
        HashMap<String, String> result = new HashMap<String, String>();
        if (null != responseString) {
            String[] temp = responseString.split("&");
            for (String string : temp) {
                if (string.indexOf("=") > -1) {
                    String[] p = string.split("=");
                    result.put(p[0], p[1]);
                } else {
                    result.put(string, "");
                }
            }
        }
        return result;
    }
}
