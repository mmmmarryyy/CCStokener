package com.elibera.ccs.app;

import java.io.BufferedReader;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.net.HttpURLConnection;
import java.net.URL;
import com.elibera.ccs.parser.HelperContentPackage;

public class Post {

    public static String HTTP_BOUNDARY = "AaB03x";

    public static String HTTP_REQUEST_CONTENT_DISPOSITION = "Content-Disposition: form-data; name=\"";

    public static String HTTP_REQUEST_CONTENT_TYPE = "Content-Type";

    public static String HTTP_REQUEST_CONTENT_LENGTH = "Content-Length: ";

    public static boolean post(String[] params, byte[][] values, String[] contentTypes, String surl, StringBuffer ret, ApplicationParams applet) {
        try {
            URL url = new URL(surl);
            HttpURLConnection con = (HttpURLConnection) url.openConnection();
            con.setRequestMethod("POST");
            con.setDoInput(true);
            con.setDoOutput(true);
            con.setUseCaches(false);
            con.setDefaultUseCaches(false);
            con.setRequestProperty("Accept", "*/*");
            con.setRequestProperty(HTTP_REQUEST_CONTENT_TYPE, "multipart/form-data; boundary=" + HTTP_BOUNDARY);
            con.setRequestProperty("Connection", "Keep-Alive");
            con.setRequestProperty("Cache-Control", "no-cache");
            HelperContentPackage.setStdRequestHeadersForPlatformServer(con, applet);
            byte[] data = getPOSTContent(params, values, contentTypes);
            OutputStream out = con.getOutputStream();
            System.out.println("sending content:" + data.length);
            out.write(data);
            out.flush();
            out.close();
            InputStream is = con.getInputStream();
            InputStreamReader isr = new InputStreamReader(is);
            BufferedReader br = new BufferedReader(isr);
            String line = null;
            while ((line = br.readLine()) != null) {
                System.out.println("line: " + line);
                ret.append(line);
            }
            return true;
        } catch (Throwable t) {
            t.printStackTrace();
            ret.append("\n");
            ret.append(t.getMessage());
        }
        return false;
    }

    /**
     * generiert aus den key-Value-Paires einen versandfertigen HTTP-Content
     * @param names
     * @param values
     * @return
     */
    public static byte[] getPOSTContent(String[] keys, byte[][] values, String[] contentTypes) {
        ByteArrayOutputStream content = new ByteArrayOutputStream();
        for (int i = 0; i < keys.length; i++) {
            if (contentTypes[i] == null) setString(keys[i], new String(values[i]), content); else setBinary(keys[i], values[i], contentTypes[i], content);
        }
        finishContent(content);
        return (content.toByteArray());
    }

    /**
     * schreibt einen String heraus --> also text/plain
     * @param id
     * @param value
     * @param content
     */
    public static void setString(String id, String value, ByteArrayOutputStream content) {
        byte[] d = null;
        try {
            d = value.getBytes("UTF-8");
        } catch (Exception e) {
            d = value.getBytes();
        }
        String c = "--" + HTTP_BOUNDARY + "\r\n" + HTTP_REQUEST_CONTENT_DISPOSITION + id + "\"" + "\r\n" + HTTP_REQUEST_CONTENT_LENGTH + (d.length) + "\r\n" + HTTP_REQUEST_CONTENT_TYPE + ": text/plain; charset=UTF-8" + "\r\n" + "content-transfer-encoding: binary\r\n" + "\r\n";
        write(c.getBytes(), content);
        write(d, content);
        write("\r\n".getBytes(), content);
    }

    /**
     * schreibt eine Binary in den Streams
     * @param id
     * @param data
     * @param contentType
     * @param content
     */
    public static void setBinary(String id, byte[] data, String contentType, ByteArrayOutputStream content) {
        byte[] encoded = data;
        String c = "--" + HTTP_BOUNDARY + "\r\n" + HTTP_REQUEST_CONTENT_DISPOSITION + id + "\"; filename=\"" + id + "\"" + "\r\n" + HTTP_REQUEST_CONTENT_TYPE + ": " + contentType + "\r\n" + HTTP_REQUEST_CONTENT_LENGTH + encoded.length + "\r\n" + "Content-Encoding: Binary" + "\r\n\r\n";
        write(c.getBytes(), content);
        write(encoded, content);
        write("\r\n".getBytes(), content);
    }

    /**
     * beendet die POST-Seite
     *
     */
    public static void finishContent(ByteArrayOutputStream content) {
        String c = "--" + HTTP_BOUNDARY + "--\r\n";
        write(c.getBytes(), content);
    }

    /**
     * schreiben der Daten in den byte[] stream
     * exceptions werden geschluckt
     * @param data
     * @param content
     */
    private static void write(byte[] data, ByteArrayOutputStream content) {
        try {
            content.write(data);
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    /**
     * liest einen HTTP InputStream und gibt das Ergebnis als byte zurück
     * @param in
     * @return
     * @throws IOException
     */
    public static byte[] getResultFromHTTPInputStream(InputStream in) throws IOException {
        ByteArrayOutputStream bout = new ByteArrayOutputStream();
        int BUFFER_SIZE = 1024;
        byte data[] = new byte[BUFFER_SIZE];
        int count = 0;
        while ((count = in.read(data, 0, BUFFER_SIZE)) != -1) {
            bout.write(data, 0, count);
        }
        return bout.toByteArray();
    }
}
