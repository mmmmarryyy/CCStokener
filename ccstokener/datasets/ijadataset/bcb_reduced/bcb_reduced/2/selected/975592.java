package net.sf.cclearly.conn.reflector;

import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.net.HttpURLConnection;
import java.net.URL;
import java.util.HashMap;

public class PostMethod {

    private final URL url;

    private HashMap<String, String> postParameters = new HashMap<String, String>();

    private HashMap<String, byte[]> postFiles = new HashMap<String, byte[]>();

    private HttpURLConnection connection = null;

    private int statusCode;

    public PostMethod(URL uri) {
        this.url = uri;
    }

    public void setParameter(String key, String value) {
        postParameters.put(key, value);
    }

    public boolean execute() {
        try {
            connection = (HttpURLConnection) url.openConnection();
            connection.setDoOutput(true);
            connection.setDoInput(true);
            connection.setFollowRedirects(true);
            connection.setRequestMethod("POST");
            connection.setRequestProperty("Content-Type", "multipart/form-data, boundary=AaB03x");
            DataOutputStream dataOut = new DataOutputStream(connection.getOutputStream());
            for (String key : postParameters.keySet()) {
                dataOut.writeBytes("--AaB03x\r\n");
                String string = postParameters.get(key);
                dataOut.writeBytes("Content-Length: " + string.length() + "\r\n");
                dataOut.writeBytes("content-disposition: form-data; name=" + key + "\r\n");
                dataOut.writeBytes("\r\n");
                dataOut.writeBytes(string + "\r\n");
            }
            for (String key : postFiles.keySet()) {
                byte[] data = postFiles.get(key);
                dataOut.writeBytes("--AaB03x\r\n");
                dataOut.writeBytes("Content-Disposition: form-data; name=\"" + key + "\"; filename=\"file\"\r\n");
                dataOut.writeBytes("Content-Type: image/jpeg\r\n");
                dataOut.writeBytes("Content-Transfer-Encoding: binary");
                dataOut.writeBytes("Content-Length: " + data.length + "\r\n");
                dataOut.writeBytes("\r\n");
                dataOut.write(data);
                dataOut.writeBytes("\r\n");
            }
            dataOut.writeBytes("--AaB03x--\r\n");
            dataOut.flush();
            dataOut.close();
            statusCode = connection.getResponseCode();
            if (statusCode == HttpURLConnection.HTTP_OK) {
                return true;
            } else {
                return false;
            }
        } catch (IOException e) {
            return false;
        }
    }

    /**
     * 
     * @return null if execute() has not been called..
     * @throws IOException 
     */
    public String getResponseBodyAsString() throws IOException {
        if (connection == null) {
            return null;
        }
        InputStream input = connection.getInputStream();
        StringBuilder result = new StringBuilder();
        byte[] data = new byte[1024];
        int read = 0;
        while ((read = input.read(data, 0, 1024)) != -1) {
            result.append(new String(data, 0, read));
        }
        return result.toString();
    }

    public int getStatusCode() {
        return statusCode;
    }

    public void releaseConnection() {
        if (connection == null) {
            return;
        }
        connection.disconnect();
    }

    public void addPart(String key, String value) {
        postParameters.put(key, value);
    }

    public void addFilePart(String key, byte[] value) {
        postFiles.put(key, value);
    }

    public void flush() {
        try {
            connection.getContent();
        } catch (IOException e) {
        }
    }

    public String getResponseHeader(String key) {
        return connection.getHeaderField(key);
    }

    public InputStream getResponseBodyAsStream() throws IOException {
        return connection.getInputStream();
    }
}
