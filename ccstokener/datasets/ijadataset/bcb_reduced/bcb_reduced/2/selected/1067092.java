package open.gps.gopens.location.log;

import java.io.BufferedInputStream;
import java.io.BufferedOutputStream;
import java.io.DataOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.net.HttpURLConnection;
import java.net.URL;
import java.text.SimpleDateFormat;
import java.util.GregorianCalendar;
import open.gps.gopens.R;
import android.content.Context;
import android.util.Log;

/**
 * Small java class that allows to upload gpx files to www.openstreetmap.org via
 * its api call.
 * 
 * @author cdaller
 * @author Nicolas Gramlich
 * @author Cyrille Mortier
 */
public class OSMUploader {

    public static final String API_VERSION = "0.6";

    private static final int BUFFER_SIZE = 65535;

    private static final String BASE64_ENC = "ABCDEFGHIJKLMNOPQRSTUVWXYZabcdefghijklmnopqrstuvwxyz0123456789+/";

    private static final String BOUNDARY = "----------------------------d10f7aa230e8";

    private static final String LINE_END = "\r\n";

    private static final String DEFAULT_DESCRIPTION = "GoPenS - automatically created route.";

    private static final String DEFAULT_TAGS = "GoPenS";

    public static final SimpleDateFormat FILE_NAME_FORMAT = new SimpleDateFormat("yyyyMMdd'_'HHmmss'_'SSS");

    private static final SimpleDateFormat AUTO_TAG_FORMAT = new SimpleDateFormat("MMMM yyyy");

    private static String filename;

    private static Context ctx;

    private static String mess = null;

    private static File file;

    /**
	 * Constructor.
	 * 
	 * @param file GPX file to send to OpenStreetMap.
	 * @param ctx The context.
	 * @throws IOException
	 * @throws InterruptedException
	 */
    public OSMUploader(File file, Context ctx) throws IOException, InterruptedException {
        this.ctx = ctx;
        filename = file.getName();
        this.file = file;
    }

    /**
	 * NOTE: This method is not blocking! (Code runs in thread)
	 * 
	 * @param username
	 *            <code>not null</code> and <code>not empty</code>. Valid
	 *            OSM-username
	 * @param password
	 *            <code>not null</code> and <code>not empty</code>. Valid
	 *            password to the OSM-username.
	 * @param description
	 *            <code>not null</code>
	 * @param tags
	 *            if <code>null</code> addDateTags is treated as
	 *            <code>true</code>
	 * @param addDateTags
	 *            adds Date Tags to the existing Tags (i.e. "
	 *            <code>October 2008</code>")
	 * @throws IOException
	 * @throws InterruptedException 
	 */
    public static void uploadAsync(final String username, final String password, final String description, final String tags, final boolean addDateTags) throws IOException, InterruptedException {
        if (username == null || username.length() == 0) {
            throw new IOException(ctx.getString(R.string.EmptyLogin));
        }
        if (password == null || password.length() == 0) {
            throw new IOException(ctx.getString(R.string.EmptyPWD));
        }
        if (description == null || description.length() == 0) {
            return;
        }
        if (tags == null || tags.length() == 0) {
            return;
        }
        Thread thr = new Thread(new Runnable() {

            public void run() {
                try {
                    final FileInputStream gpxInputStream = new FileInputStream(file);
                    String tagsToUse = tags;
                    if (addDateTags || tagsToUse == null) {
                        if (tagsToUse == null) {
                            tagsToUse = AUTO_TAG_FORMAT.format(new GregorianCalendar().getTime());
                        } else {
                            tagsToUse = tagsToUse + " " + AUTO_TAG_FORMAT.format(new GregorianCalendar().getTime());
                        }
                    }
                    Log.d("UPLOAD", "Uploading " + filename + " to openstreetmap.org");
                    final String urlDesc = (description == null) ? DEFAULT_DESCRIPTION : description.replaceAll("\\.;&?,/", "_");
                    final String urlTags = (tagsToUse == null) ? DEFAULT_TAGS : tagsToUse.replaceAll("\\\\.;&?,/", "_");
                    final URL url = new URL("http://www.openstreetmap.org/api/" + API_VERSION + "/gpx/create");
                    Log.d("UPLOAD", "Destination Url: " + url);
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
                    writeContentDispositionFile(out, "file", gpxInputStream);
                    writeContentDisposition(out, "description", urlDesc);
                    writeContentDisposition(out, "tags", urlTags);
                    writeContentDisposition(out, "public", "1");
                    out.writeBytes("--" + BOUNDARY + "--" + LINE_END);
                    Log.i("UPLOAD", "data : " + out.size());
                    out.flush();
                    final int retCode = con.getResponseCode();
                    String retMsg = con.getResponseMessage();
                    Log.d("UPLOAD", "return code: " + retCode + " " + retMsg);
                    if (retCode != 200) {
                        if (con.getHeaderField("Error") != null) {
                            retMsg += "\n" + con.getHeaderField("Error");
                        }
                        out.close();
                        con.disconnect();
                        throw new IOException(ctx.getString(R.string.errorLogin));
                    }
                    out.close();
                    con.disconnect();
                } catch (Exception e) {
                    Log.e("UPLOAD", "OSMUpload Error", e);
                    mess = e.getMessage();
                }
            }
        }, "OSMUpload-Thread");
        thr.start();
        thr.join();
        if (mess != null) {
            throw new IOException(mess);
        }
    }

    /**
	 * 
	 * @param username OpenStreetMap login ID.
	 * @param password OpenStreetMap login password.
	 * @param description 
	 * @param tags
	 * @param addDateTags
	 * @throws IOException
	 * @throws InterruptedException
	 */
    public static void upload(final String username, final String password, final String description, final String tags, final boolean addDateTags) throws IOException, InterruptedException {
        uploadAsync(username, password, description, tags, addDateTags);
    }

    /**
	 * @param out
	 * @param string
	 * @param gpxFile
	 * @throws IOException
	 */
    private static void writeContentDispositionFile(final DataOutputStream out, final String name, final InputStream gpxInputStream) throws IOException {
        out.writeBytes("--" + BOUNDARY + LINE_END);
        out.writeBytes("Content-Disposition: form-data; name=\"" + name + "\"; filename=\"" + filename + "\"" + LINE_END);
        out.writeBytes("Content-Type: application/octet-stream" + LINE_END);
        out.writeBytes(LINE_END);
        final byte[] buffer = new byte[BUFFER_SIZE];
        int read;
        int sumread = 0;
        final InputStream in = new BufferedInputStream(gpxInputStream);
        while ((read = in.read(buffer)) >= 0) {
            out.write(buffer, 0, read);
            out.flush();
            sumread += read;
        }
        in.close();
        out.writeBytes(LINE_END);
    }

    /**
	 * @param string
	 * @param urlDesc
	 * @throws IOException
	 */
    private static void writeContentDisposition(final DataOutputStream out, final String name, final String value) throws IOException {
        out.writeBytes("--" + BOUNDARY + LINE_END);
        out.writeBytes("Content-Disposition: form-data; name=\"" + name + "\"" + LINE_END);
        out.writeBytes(LINE_END);
        out.writeBytes(value + LINE_END);
    }

    /**
	 * 
	 * @param s
	 * @return s endoded with Base64.
	 */
    private static String encodeBase64(final String s) {
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
