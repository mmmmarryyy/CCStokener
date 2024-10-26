package net.sf.syncopate.rc;

import java.io.IOException;
import java.io.OutputStream;
import java.io.InputStream;
import java.net.HttpURLConnection;
import java.net.Socket;
import java.net.URL;
import java.net.URLConnection;

public class ConnectionManager {

    protected String baseUrl;

    public ConnectionManager(String baseUrl) {
        System.out.println("ConnectionManager.ConnectionManager");
        System.out.println("baseUrl = " + baseUrl);
        this.baseUrl = baseUrl;
    }

    public URLConnection openConnection() throws IOException {
        URL url = new URL(baseUrl);
        return url.openConnection();
    }

    public Socket openSocket() throws IOException {
        Socket socket = new Socket(baseUrl, 8888);
        StringBuffer buf = new StringBuffer("POST /syncopate/__$$_decorateSampler HTTP/1.1\n");
        buf.append("Connection: keep-alive\n");
        buf.append("Host: localhost\n");
        buf.append("Accept: text/html, image/gif, image/jpeg, *; q=.2, */*; q=.2\n");
        buf.append("Content-type: application/x-www-form-urlencoded\n\n");
        socket.getOutputStream().write(buf.toString().getBytes());
        return socket;
    }

    public HttpURLConnection openDecoratorConnection() throws IOException {
        URL url = new URL(baseUrl + "/__$$_decorateSampler");
        HttpURLConnection retVal = (HttpURLConnection) url.openConnection();
        retVal.setDoOutput(true);
        retVal.setDoInput(true);
        return retVal;
    }

    public void sendDecoratorRequest(String s) throws IOException {
        System.out.println("s = " + s);
        HttpURLConnection conn = openDecoratorConnection();
        System.out.println("connection opened");
        byte[] byteArr = s.getBytes();
        conn.setFixedLengthStreamingMode(byteArr.length);
        OutputStream out = conn.getOutputStream();
        out.write(byteArr);
        System.out.println("Stream written");
        out.flush();
        System.out.println("Stream flushed");
        out.close();
        System.out.println("Stream closed");
    }
}
