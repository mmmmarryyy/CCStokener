package org.owasp.oss.client;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.HttpURLConnection;
import java.net.URL;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Set;
import org.apache.log4j.Logger;

/**
 * Class providing functionality to communicate with a RESTful server
 * architecture
 */
public class RESTClient {

    private static Logger log = Logger.getLogger(RESTClient.class);

    private String _sessionId;

    private boolean _sessionOpen = false;

    public boolean isSessionOpen() {
        return _sessionOpen;
    }

    private void parseHeader(HttpURLConnection conn) {
        Map<String, List<String>> h = conn.getHeaderFields();
        Set<String> k = h.keySet();
        Iterator<String> i = k.iterator();
        while (i.hasNext()) {
            String key = i.next();
            System.out.println(key);
            if (key != null && key.equals("Set-Cookie")) {
                _sessionId = h.get(key).get(0);
                _sessionOpen = true;
            }
            List<String> l = h.get(key);
            Iterator<String> il = l.iterator();
            while (il.hasNext()) {
                System.out.println(il.next());
            }
        }
    }

    /**
	 * Retrieve a resource by issuing a GET request.
	 * 
	 * @param url
	 *            The URL of the resource to retrieve
	 * @return An array of bytes of the request contents or null
	 * @throws IOException
	 */
    public byte[] doGET(URL url) {
        byte[] response = null;
        HttpURLConnection conn = null;
        try {
            conn = (HttpURLConnection) url.openConnection();
            conn.setRequestMethod("GET");
            conn.setDoInput(true);
            conn.setUseCaches(false);
            conn.setRequestProperty("Content-Type", "application/octet-stream");
            conn.setRequestProperty("Content-Transfer-Encoding", "binary");
            conn.setRequestProperty("accept", "application/octet-stream");
            if (_sessionOpen) conn.setRequestProperty("Cookie", _sessionId);
            conn.connect();
            if (conn.getResponseCode() == 200) {
                System.out.println("Response code: " + conn.getResponseCode() + " (" + conn.getResponseMessage() + ")");
                parseHeader(conn);
                response = getRequestBytes(conn.getInputStream());
            } else {
                System.out.println("Response code: " + conn.getResponseCode() + " (" + conn.getResponseMessage() + ")");
            }
        } catch (IOException e) {
            System.out.println("GET failed!" + e);
        } finally {
        }
        return response;
    }

    /**
	 * Sending and retrieve a resource by issuing a POST request.
	 * 
	 * @param url
	 *            The URL of the resource to retrieve
	 * @param body
	 *            Content which is sent to URL
	 * @return An array of bytes of the request contents or null
	 * @throws IOException
	 */
    public byte[] doPost(URL url, byte[] body) {
        byte[] response = null;
        HttpURLConnection conn = null;
        try {
            conn = (HttpURLConnection) url.openConnection();
            conn.setRequestMethod("POST");
            conn.setDoOutput(true);
            conn.setDoInput(true);
            conn.setUseCaches(false);
            conn.setRequestProperty("Content-Type", "application/octet-stream");
            conn.setRequestProperty("Content-Transfer-Encoding", "binary");
            conn.setRequestProperty("accept", "application/octet-stream");
            if (_sessionOpen) conn.setRequestProperty("Cookie", _sessionId);
            conn.connect();
            OutputStream post_data = conn.getOutputStream();
            post_data.write(body);
            post_data.close();
            if (conn.getResponseCode() == 200) {
                System.out.println("Response code: " + conn.getResponseCode() + " (" + conn.getResponseMessage() + ")");
                parseHeader(conn);
                response = getRequestBytes(conn.getInputStream());
            } else {
                System.out.println("Response code: " + conn.getResponseCode() + " (" + conn.getResponseMessage() + ")");
            }
        } catch (IOException e) {
            System.out.println("Error while sending POST " + e);
        } finally {
            if (null != conn) conn.disconnect();
        }
        return response;
    }

    /**
	 * Get request contents (e.g. a POST request contents) into a byte array.
	 * 
	 * @param request_stream
	 *            InputStream of request contents
	 * @return An array of bytes of the request contents or null
	 * @throws IOException
	 */
    public static byte[] getRequestBytes(InputStream request_stream) throws IOException {
        if (request_stream == null) {
            return null;
        }
        int buffer_size = 1024;
        byte[] byte_buffer = new byte[buffer_size];
        int bytes_read = 0;
        ByteArrayOutputStream byte_array_stream = new ByteArrayOutputStream(buffer_size * 2);
        try {
            while ((bytes_read = request_stream.read(byte_buffer)) != -1) {
                byte_array_stream.write(byte_buffer, 0, bytes_read);
            }
        } catch (Throwable e) {
            e.printStackTrace(System.out);
        }
        return byte_array_stream.toByteArray();
    }
}
