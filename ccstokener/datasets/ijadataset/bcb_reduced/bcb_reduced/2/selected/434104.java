package com.tx.http.util;

import java.io.ByteArrayOutputStream;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.HttpURLConnection;
import java.net.URL;
import java.util.Map;

/**
 * 和服务端http通讯工具类
 * @author Crane
 *
 */
public class HttpUtil {

    public static String postQueryMap(String url, Map<String, String> queryParams) {
        try {
            String queryParam = compQueryParams(queryParams);
            HttpURLConnection conn = (HttpURLConnection) new URL(url).openConnection();
            conn.setDoOutput(true);
            conn.setDoInput(true);
            conn.setRequestMethod("POST");
            conn.setRequestProperty("Charset", "UTF-8");
            conn.setRequestProperty("Content-Length", String.valueOf(queryParam != null ? queryParam.length() : 0));
            conn.setReadTimeout(5000);
            conn.setConnectTimeout(5000);
            if (queryParams != null) {
                OutputStream os = conn.getOutputStream();
                os.write(queryParam.getBytes("utf-8"));
                os.flush();
                os.close();
            }
            int resStatus = conn.getResponseCode();
            if (resStatus == HttpURLConnection.HTTP_OK) {
                String result = getStr(conn.getInputStream());
                return result;
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
        return "";
    }

    public static String postJson(String url, String json) {
        try {
            HttpURLConnection conn = (HttpURLConnection) new URL(url).openConnection();
            conn.setDoOutput(true);
            conn.setDoInput(true);
            conn.setRequestMethod("POST");
            conn.setRequestProperty("Charset", "UTF-8");
            conn.setRequestProperty("Content-Length", String.valueOf(json != null ? json.length() : 0));
            conn.setReadTimeout(5000);
            conn.setConnectTimeout(5000);
            if (json != null) {
                OutputStream os = conn.getOutputStream();
                os.write(json.getBytes("utf-8"));
                os.flush();
                os.close();
            }
            int resStatus = conn.getResponseCode();
            if (resStatus == HttpURLConnection.HTTP_OK) {
                String result = getStr(conn.getInputStream());
                return result;
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
        return "";
    }

    public static byte[] download(String url, Map<String, String> queryParams) {
        try {
            String queryParam = compQueryParams(queryParams);
            HttpURLConnection conn = (HttpURLConnection) new URL(url).openConnection();
            conn.setDoOutput(true);
            conn.setDoInput(true);
            conn.setRequestMethod("POST");
            conn.setRequestProperty("Charset", "UTF-8");
            conn.setRequestProperty("Content-Length", String.valueOf(queryParam != null ? queryParam.length() : 0));
            conn.setReadTimeout(5000);
            conn.setConnectTimeout(5000);
            if (queryParams != null) {
                OutputStream os = conn.getOutputStream();
                os.write(queryParam.getBytes("utf-8"));
                os.flush();
                os.close();
            }
            int resStatus = conn.getResponseCode();
            if (resStatus == HttpURLConnection.HTTP_OK) {
                byte[] result = getByte(conn.getInputStream());
                return result;
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
        return null;
    }

    private static String compQueryParams(Map<String, String> queryParams) {
        if (queryParams == null) {
            return null;
        }
        StringBuffer sb = new StringBuffer();
        for (String name : queryParams.keySet()) {
            sb.append(name);
            sb.append("=");
            sb.append(queryParams.get(name));
            sb.append("&");
        }
        sb.deleteCharAt(sb.toString().length() - 1);
        return sb.toString();
    }

    public static String getStr(InputStream is) throws Exception {
        byte[] buffer = new byte[1024];
        int len;
        ByteArrayOutputStream bos = new ByteArrayOutputStream();
        while ((len = is.read(buffer)) != -1) {
            bos.write(buffer, 0, len);
        }
        String str = new String(bos.toByteArray(), "utf-8");
        bos.close();
        is.close();
        return str;
    }

    public static byte[] getByte(InputStream is) throws Exception {
        byte[] buffer = new byte[1024];
        int len = -1;
        ByteArrayOutputStream bos = new ByteArrayOutputStream();
        while ((len = is.read(buffer)) != -1) {
            bos.write(buffer, 0, len);
        }
        byte[] data = bos.toByteArray();
        bos.close();
        is.close();
        return data;
    }
}
