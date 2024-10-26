package org.bodega.servlet;

import java.io.*;
import java.net.*;
import java.util.*;
import org.json.*;

/** An HTTP client to POST requests to a URL and capture the responses.
*
* @author Paul Copeland
*/
public class HttpClient {

    private static PrintStream out = System.err;

    private static boolean verbose = false;

    private static boolean formatJson = true;

    public static void main(String[] args) throws Exception {
        verbose = Boolean.getBoolean("verbose");
        if (System.getProperties().get("formatJson") != null) formatJson = Boolean.getBoolean("formatJson");
        for (int i = 1; i < args.length; i++) {
            out.println("[connecting to URL]=" + args[0]);
            out.println("[sending message]=" + args[i]);
            out.println();
            HttpClient http = new HttpClient(args[0], args[i]);
            http.request();
            http.response();
        }
    }

    private HttpURLConnection connection;

    private String jsonMessage;

    public HttpClient(String urlString, String jsonMessage) throws Exception {
        this.jsonMessage = jsonMessage;
        connection = (HttpURLConnection) (new URL(urlString)).openConnection();
        connection.setDoOutput(true);
        connection.setDoInput(true);
        connection.setRequestMethod("POST");
        connection.setRequestProperty("Content-type", "text/plain");
    }

    private void request() throws Exception {
        connection.connect();
        OutputStream os = connection.getOutputStream();
        PrintWriter pw = new PrintWriter(os);
        pw.println(jsonMessage);
        pw.close();
        os.close();
    }

    private void response() throws Exception {
        try {
            out.println("[RESPONSE]");
            if (verbose) {
                out.println("defaultUseCaches = " + connection.getDefaultUseCaches());
                out.println("useCaches = " + connection.getUseCaches());
                String field;
                for (int fieldId = 1; (field = connection.getHeaderFieldKey(fieldId)) != null; fieldId++) out.println("Header '" + field + "'=" + connection.getHeaderField(field));
            }
            out.println("Response Code= " + connection.getResponseCode());
            int len;
            char cbuf[] = new char[512];
            StringBuffer sbuf = new StringBuffer();
            InputStreamReader reader = new InputStreamReader(connection.getInputStream());
            while ((len = reader.read(cbuf)) != -1) sbuf.append(cbuf, 0, len);
            if (formatJson) {
                String str = sbuf.toString();
                if (str.charAt(0) == '(' && str.lastIndexOf(')') > -1) {
                    str = str.substring(1, str.lastIndexOf(')'));
                    String jsonResultStr = (str.charAt(0) == '[') ? (new JSONArray(str)).toString(4) : (new JSONObject(str)).toString(4);
                    out.println(jsonResultStr);
                } else out.println(sbuf);
            } else out.println(sbuf);
            out.println();
        } catch (Exception ex) {
            out.println("[Exception] " + ex);
            InputStream errorStream = connection.getErrorStream();
            if (errorStream == null) {
                throw ex;
            } else {
                char buf[] = new char[256];
                InputStreamReader reader = new InputStreamReader(errorStream);
                PrintWriter writer = new PrintWriter(out);
                int count;
                while ((count = reader.read(buf)) != -1) writer.write(buf, 0, count);
                writer.close();
            }
        } finally {
            connection.disconnect();
        }
    }
}
