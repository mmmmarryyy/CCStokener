package net.sf.bt747.j2se.app.osm;

import java.io.BufferedInputStream;
import java.io.BufferedOutputStream;
import java.io.DataOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.UnsupportedEncodingException;
import java.net.HttpURLConnection;
import java.net.MalformedURLException;
import java.net.URL;
import bt747.sys.Generic;

/**
 * Small java class that allows to upload gpx files to www.openstreetmap.org
 * via its api call.
 * 
 * @author Christof Dallermassl
 * @author Mario De Weerd
 */
public class OsmGpxUpload {

    /** The API version of the OSM interface to use. */
    public static final String API_VERSION = "0.6";

    /** How big the buffer should be. */
    private static final int BUFFER_SIZE = 65535;

    private static final String BASE64_ENC = "ABCDEFGHIJKLMNOPQRSTUVWXYZabcdefghijklmnopqrstuvwxyz0123456789+/";

    /** A boundary for the encoded message */
    private static final String BOUNDARY = "----------------------------d10f7aa230e8";

    /** Line ending constant. */
    private static final String LINE_END = "\r\n";

    /**
     * @param username
     * @param password
     * @param description
     * @param tags
     * @param gpxFile
     * @throws IOException
     */
    public static final void upload(final String username, final String password, final String description, final String tags, final File gpxFile, final String visibility) throws IOException {
        Generic.debug("uploading " + gpxFile.getAbsolutePath() + " to openstreetmap.org");
        try {
            final String urlDesc = description.length() == 0 ? "No description" : description.replaceAll("\\.;&?,/", "_");
            final String urlTags = tags.replaceAll("\\\\.;&?,/", "_");
            final URL url = new URL("http://www.openstreetmap.org/api/" + API_VERSION + "/gpx/create");
            Generic.debug("url: " + url);
            final HttpURLConnection con = (HttpURLConnection) url.openConnection();
            con.setConnectTimeout(15000);
            con.setRequestMethod("POST");
            con.setDoOutput(true);
            con.addRequestProperty("Authorization", "Basic " + encodeBase64(username + ":" + password));
            con.addRequestProperty("Content-Type", "multipart/form-data; boundary=" + BOUNDARY);
            con.addRequestProperty("Connection", "close");
            con.addRequestProperty("Expect", "");
            con.connect();
            DataOutputStream out = new DataOutputStream(new BufferedOutputStream(con.getOutputStream()));
            writeContentDispositionFile(out, "file", gpxFile);
            writeContentDisposition(out, "description", urlDesc);
            writeContentDisposition(out, "tags", urlTags);
            writeContentDisposition(out, "visibility", visibility);
            out.writeBytes("--" + BOUNDARY + "--" + LINE_END);
            out.flush();
            final int retCode = con.getResponseCode();
            String retMsg = con.getResponseMessage();
            Generic.debug("\nreturn code: " + retCode + " " + retMsg);
            if (retCode != 200) {
                if (con.getHeaderField("Error") != null) {
                    retMsg += "\n" + con.getHeaderField("Error");
                }
                con.disconnect();
                throw new RuntimeException(retCode + " " + retMsg);
            }
            out.close();
            con.disconnect();
            Generic.debug(gpxFile.getAbsolutePath());
        } catch (UnsupportedEncodingException ignore) {
        } catch (MalformedURLException e) {
            e.printStackTrace();
        }
    }

    /**
     * @param out
     *            Where to write the content disposition.
     * @param name
     *            The name for the content.
     * @param gpxFile
     *            The filename for the content.
     * @throws IOException
     *             Throws io exceptions when trouble with streams.
     */
    private static final void writeContentDispositionFile(final DataOutputStream out, final String name, final File gpxFile) throws IOException {
        out.writeBytes("--" + BOUNDARY + LINE_END);
        out.writeBytes("Content-Disposition: form-data; name=\"" + name + "\"; filename=\"" + gpxFile.getName() + "\"" + LINE_END);
        out.writeBytes("Content-Type: application/octet-stream" + LINE_END);
        out.writeBytes(LINE_END);
        final byte[] buffer = new byte[BUFFER_SIZE];
        int read;
        int sumread = 0;
        InputStream in = new BufferedInputStream(new FileInputStream(gpxFile));
        Generic.debug("Transferring data to server");
        while ((read = in.read(buffer)) >= 0) {
            out.write(buffer, 0, read);
            out.flush();
            sumread += read;
        }
        in.close();
        out.writeBytes(LINE_END);
    }

    /**
     * @param out
     *            Where to write the content disposition.
     * @param name
     *            The name for the content.
     * @param value
     *            The value to write.
     * @throws IOException
     *             Exceptions related to output stream.
     */
    public static final void writeContentDisposition(final DataOutputStream out, final String name, final String value) throws IOException {
        if (value.length() != 0) {
            out.writeBytes("--" + BOUNDARY + LINE_END);
            out.writeBytes("Content-Disposition: form-data; name=\"" + name + "\"" + LINE_END);
            out.writeBytes(LINE_END);
            Generic.debug("name:" + name + "=" + value);
            out.writeBytes(value + LINE_END);
        }
    }

    /**
     * Encode a string to base64.
     * 
     * @param s
     *            The string to encode.
     * @return The encoded string.
     */
    public static final String encodeBase64(final String s) {
        final StringBuilder out = new StringBuilder();
        for (int i = 0; i < (s.length() + 2) / 3; ++i) {
            final int l = Math.min(3, s.length() - i * 3);
            final String buf = s.substring(i * 3, i * 3 + l);
            out.append(BASE64_ENC.charAt(buf.charAt(0) >> 2));
            out.append(BASE64_ENC.charAt((buf.charAt(0) & 0x03) << 4 | (l == 1 ? 0 : (buf.charAt(1) & 0xf0) >> 4)));
            out.append(l > 1 ? BASE64_ENC.charAt((buf.charAt(1) & 0x0f) << 2 | (l == 2 ? 0 : (buf.charAt(2) & 0xc0) >> 6)) : '=');
            out.append(l > 2 ? BASE64_ENC.charAt(buf.charAt(2) & 0x3f) : '=');
        }
        return out.toString();
    }
}
