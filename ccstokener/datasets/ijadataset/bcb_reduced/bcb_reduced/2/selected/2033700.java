package ws.prova.remoting.http;

import java.io.BufferedReader;
import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.IOException;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.URL;

public class HttpClient {

    public static String getRequest(String ipAddress, String query) throws IOException {
        HttpURLConnection conn = open(ipAddress);
        conn.setRequestProperty("Content-Length", Integer.toString(query.length()));
        DataOutputStream out = new DataOutputStream(conn.getOutputStream());
        StringBuilder sb = new StringBuilder();
        sb.append(query);
        String content = sb.toString();
        out.writeBytes(content);
        out.flush();
        out.close();
        return receiveData(conn);
    }

    private static HttpURLConnection open(String ipAddress) throws IOException {
        URL url = new URL(ipAddress);
        HttpURLConnection conn = (HttpURLConnection) url.openConnection();
        conn.setDoInput(true);
        conn.setDoOutput(true);
        conn.setUseCaches(false);
        conn.setRequestMethod("POST");
        conn.setRequestProperty("Content-Type", "application/x-www-form-urlencoded");
        conn.setRequestProperty("Host", ipAddress);
        conn.setConnectTimeout(60000);
        conn.setReadTimeout(60000);
        return conn;
    }

    private static String receiveData(HttpURLConnection conn) throws IOException {
        DataInputStream in = new DataInputStream(conn.getInputStream());
        BufferedReader bin = new BufferedReader(new InputStreamReader(in));
        StringBuilder sbout = new StringBuilder();
        String str;
        while ((str = bin.readLine()) != null) {
            sbout.append(str);
        }
        str = sbout.toString();
        in.close();
        return str;
    }
}
