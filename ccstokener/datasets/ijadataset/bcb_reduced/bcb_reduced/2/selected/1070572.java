package org.telscenter.sail.webapp.presentation.util.http;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.OutputStreamWriter;
import java.net.URL;
import java.net.URLConnection;

/**
 * @author patrick lawler
 * @version $Id:$
 */
public class Connector {

    /**
	 * Makes a request to the given <code>String</code> url with the given
	 * <code>String</code> parameters and returns the <code>String</code>
	 * response string.
	 * 
	 * @param urlStr
	 * @param params
	 * @return
	 * @throws IOException
	 */
    public static String request(String urlStr, String params) throws IOException {
        URL url = new URL(urlStr);
        URLConnection conn = url.openConnection();
        conn.setDoOutput(true);
        writeParameters(conn, params);
        return getResponseString(conn);
    }

    /**
	 * Makes a request to the given <code>String</code> url with the given
	 * and returns the <code>String</code> response string.
	 * 
	 * @param urlStr
	 * @param params
	 * @return
	 * @throws IOException
	 */
    public static String request(String urlStr) throws IOException {
        URL url = new URL(urlStr);
        URLConnection conn = url.openConnection();
        conn.setDoOutput(false);
        return getResponseString(conn);
    }

    /**
	 * Given a <code>URLConnection</code> and the <code>String</code> parameters, writes the
	 * parameters to the connection.
	 */
    private static void writeParameters(URLConnection conn, String params) throws IOException {
        conn.setDoOutput(true);
        OutputStreamWriter paramWriter = new OutputStreamWriter(conn.getOutputStream());
        paramWriter.write(params);
        paramWriter.flush();
        paramWriter.close();
    }

    /**
	 * Given a <code>URLConnection</code>, reads and returns the <code>String</code> response.
	 * 
	 * @param conn
	 * @return
	 * @throws IOException
	 */
    private static String getResponseString(URLConnection conn) throws IOException {
        BufferedReader br = new BufferedReader(new InputStreamReader(conn.getInputStream()));
        String line;
        String response = "";
        while ((line = br.readLine()) != null) {
            response += line;
        }
        br.close();
        return response;
    }
}
