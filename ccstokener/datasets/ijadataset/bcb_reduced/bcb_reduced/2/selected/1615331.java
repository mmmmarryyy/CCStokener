package cn.poco.util;

import java.io.InputStream;
import java.io.OutputStream;
import java.net.HttpURLConnection;
import java.net.URL;
import java.net.URLEncoder;
import java.util.Map;

public class SendPostRequest {

    public static boolean sendPostRequest(String path, Map<String, String> params, String encoding) throws Exception {
        StringBuilder sb = new StringBuilder("");
        if (params != null && !params.isEmpty()) {
            for (Map.Entry<String, String> entry : params.entrySet()) {
                sb.append(entry.getKey()).append('=').append(URLEncoder.encode(entry.getValue(), encoding)).append('&');
            }
            sb.deleteCharAt(sb.length() - 1);
        }
        byte[] data = sb.toString().getBytes();
        URL url = new URL(path);
        HttpURLConnection conn = (HttpURLConnection) url.openConnection();
        conn.setRequestMethod("POST");
        conn.setConnectTimeout(5 * 1000);
        conn.setDoOutput(true);
        conn.setRequestProperty("Content-Type", "application/x-www-form-urlencoded");
        conn.setRequestProperty("Content-Length", String.valueOf(data.length));
        OutputStream outStream = conn.getOutputStream();
        outStream.write(data);
        outStream.flush();
        outStream.close();
        if (conn.getResponseCode() == 200) {
            InputStream inputStream = conn.getInputStream();
            return ResponseResult.parseXML(inputStream);
        }
        return false;
    }
}
