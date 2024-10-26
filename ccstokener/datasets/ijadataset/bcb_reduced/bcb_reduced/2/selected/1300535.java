package com.globant.google.mendoza;

import java.io.BufferedReader;
import java.io.DataOutputStream;
import java.io.IOException;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.URL;
import java.net.URLEncoder;
import java.util.Map;
import org.apache.commons.codec.binary.Base64;

/** Sends the cart to the google checkout mock.
 */
public final class CartSender {

    /** The read buffer size for the sending end of the transport.
     */
    private static final int READ_BUFFER_SIZE = 1024;

    /** Sends a message to the server.
    *
    * @param url The url to connect to.
    *
    * @param parameters The command parameters to post.
    *
    * @return Returns the server response, usually an acknowledge or an error
    * related to the validity of the message structure.
    */
    public static String send(final String url, final Map<String, String> parameters) {
        HttpURLConnection connection = getConnection(url);
        String response = null;
        DataOutputStream printout = null;
        try {
            if (parameters != null && parameters.size() > 0) {
                connection.setRequestMethod("POST");
                printout = new DataOutputStream(connection.getOutputStream());
                String content = "";
                for (String key : parameters.keySet()) {
                    if (!"".equals(content)) {
                        content += "&";
                    }
                    content += URLEncoder.encode(key, "UTF-8") + "=";
                    content += URLEncoder.encode(parameters.get(key), "UTF-8");
                }
                printout.writeBytes(content);
                printout.flush();
                printout.close();
            }
            response = readResponse(connection);
        } catch (Exception e) {
            throw new RuntimeException(e.getMessage());
        } finally {
            connection.disconnect();
        }
        if (response == null) {
            throw new RuntimeException("Error. No response from server");
        }
        return response;
    }

    /** Gets a properly initialized connection to the server.
     *
     * @param strURL The url to connect to.
     *
     * @return Returns the connection. This function never returns null.
     */
    private static HttpURLConnection getConnection(final String strURL) {
        HttpURLConnection connection = null;
        try {
            URL url = new URL(strURL);
            connection = (HttpURLConnection) url.openConnection();
            connection.setDoOutput(true);
            connection.setUseCaches(false);
            connection.setRequestProperty("Content-Type", "application/x-www-form-urlencoded");
            String encoding = new String(Base64.encodeBase64((MendozaTestConstants.merchantId + ":" + MendozaTestConstants.merchantKey).getBytes()));
            connection.setRequestProperty("Authorization", "Basic " + encoding);
        } catch (IOException e) {
            throw new RuntimeException("Unable to connect to server", e);
        }
        if (connection == null) {
            throw new RuntimeException("Attempted to return a null connection");
        }
        return connection;
    }

    /** Reads the data from the connection and returns it as a string.
     *
     * @param connection The connection to read the data from.
     *
     * @return Returns the string with the data read from the connection. It is
     * null if an error happens.
     */
    private static String readResponse(final HttpURLConnection connection) {
        BufferedReader in = null;
        String response = null;
        try {
            in = new BufferedReader(new InputStreamReader(connection.getInputStream()));
        } catch (IOException e) {
            return null;
        }
        try {
            response = readResponse(in);
        } finally {
            if (in != null) {
                try {
                    in.close();
                } catch (IOException e) {
                    throw new RuntimeException("Error closing connection", e);
                }
            }
        }
        return response;
    }

    /** Reads the data from the stream realted to a connection and returns it as
     * a string.
     *
     * @param in The reader to read the data from.
     *
     * @return Returns the string with the data read from the connection.
     */
    private static String readResponse(final BufferedReader in) {
        StringBuffer response = new StringBuffer();
        try {
            char[] line = new char[READ_BUFFER_SIZE];
            int read = 0;
            while ((read = in.read(line)) != -1) {
                response.append(line, 0, read);
            }
        } catch (IOException e) {
            throw new RuntimeException("Unable to read response", e);
        }
        return response.toString();
    }
}
