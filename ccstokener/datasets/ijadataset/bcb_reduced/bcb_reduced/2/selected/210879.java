package com.google.code.javascribd.connection;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.HttpURLConnection;
import java.net.Proxy;
import java.net.URL;
import java.util.Map;

public class SimpleConnection implements ScribdConnection {

    private Proxy proxy = Proxy.NO_PROXY;

    private String url = null;

    public void setProxy(Proxy proxy) {
        if (proxy == null) {
            proxy = Proxy.NO_PROXY;
        }
        this.proxy = proxy;
    }

    public void setUrl(String url) {
        if (url == null) {
            throw new IllegalArgumentException("url must be not null");
        }
        this.url = url;
    }

    public Proxy getProxy() {
        return proxy;
    }

    public String getUrl() {
        return url;
    }

    public <T extends ScribdResponse> T getRequest(ScribdMethod<T> method, ScribdResponseParser interpretor) throws ScribdConnectionException, IOException {
        return excute(method, interpretor, new GetRequest());
    }

    public <T extends ScribdResponse> T postRequest(ScribdMethod<T> method, ScribdResponseParser interpretor) throws ScribdConnectionException, IOException {
        return excute(method, interpretor, new PostRequest());
    }

    private <T extends ScribdResponse> T excute(ScribdMethod<T> method, ScribdResponseParser interpretor, Request request) throws ScribdConnectionException, IOException {
        HttpURLConnection connection = null;
        try {
            connection = getConnection(method);
            request.request(connection, method);
            if (connection.getResponseCode() != HttpURLConnection.HTTP_OK) {
                throw new ScribdConnectionException(connection.getResponseCode(), connection.getResponseMessage());
            } else {
                return interpretor.parse(connection.getInputStream(), method.getResponseType());
            }
        } finally {
            if (connection != null) connection.disconnect();
        }
    }

    private <T extends ScribdResponse> HttpURLConnection getConnection(ScribdMethod<T> method) throws IOException {
        HttpURLConnection connection;
        String urlParameters = method.getGETParametersForURL();
        URL url = new URL(this.getUrl() + "?" + urlParameters);
        System.out.println("call url " + url);
        System.out.println(url);
        assert this.getProxy() != null;
        connection = (HttpURLConnection) url.openConnection(this.getProxy());
        return connection;
    }

    private interface Request {

        public <T extends ScribdResponse> void request(HttpURLConnection connection, ScribdMethod<T> method) throws IOException;
    }

    private static class GetRequest implements Request {

        public <T extends ScribdResponse> void request(HttpURLConnection connection, ScribdMethod<T> method) throws IOException {
        }
    }

    private static class PostRequest implements Request {

        private static String boundary = "----------V2ymHFg04e5bqgZTaKO6jy";

        public <T extends ScribdResponse> void request(HttpURLConnection connection, ScribdMethod<T> method) throws IOException {
            connection.setDoOutput(true);
            connection.setRequestProperty("Content-Type", "multipart/form-data; boundary=" + boundary);
            connection.setRequestMethod("POST");
            OutputStream dout = connection.getOutputStream();
            Map<String, StreamableData> dataMap = method.getPOSTParameters();
            for (String key : dataMap.keySet()) {
                StreamableData data = dataMap.get(key);
                String fileName = data.getName();
                String getMimeType = data.getMimeType();
                String startBoundery = generateStartBoundery(boundary, key, fileName, getMimeType);
                String endBoundary = generateEndBoundery(boundary);
                dout.write(startBoundery.getBytes());
                InputStream in = data.getInputStream();
                byte[] buf = new byte[1024];
                int len;
                while ((len = in.read(buf)) > 0) {
                    dout.write(buf, 0, len);
                }
                dout.write(endBoundary.getBytes());
                in.close();
            }
            dout.close();
        }

        private String generateStartBoundery(String boundary, String key, String fileName, String mimeType) {
            StringBuffer res = new StringBuffer();
            res.append("--");
            res.append(boundary);
            res.append("\r\n");
            res.append("Content-Disposition: form-data; name=\"");
            res.append(key);
            res.append("\"; filename=\"");
            res.append(fileName);
            res.append("\"\r\n");
            res.append("Content-Type: ");
            res.append(mimeType);
            res.append("\r\n\r\n");
            return res.toString();
        }

        private String generateEndBoundery(String boundary) {
            return "\r\n--" + boundary + "--\r\n";
        }
    }
}
