package org.omegat.util;

import java.io.BufferedWriter;
import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.io.OutputStreamWriter;
import java.net.HttpURLConnection;
import java.net.URL;
import java.net.URLEncoder;
import java.util.Map;

/**
 * Import pages from MediaWiki
 *
 * @author Kim Bruning
 * @author Alex Buloichik (alex73mail@gmail.com)
 */
public class WikiGet {

    protected static final String CHARSET_MARK = "charset=";

    /** 
     * ~inverse of String.split() 
     * refactor note: In future releases, this might best be moved to a different file 
     */
    public static String joinString(String separator, String[] items) {
        if (items.length < 1) return "";
        StringBuffer joined = new StringBuffer();
        for (int i = 0; i < items.length; i++) {
            joined.append(items[i]);
            if (i != items.length - 1) joined.append(separator);
        }
        return joined.toString();
    }

    /** 
     * Gets mediawiki wiki-code data from remote server.
     * The get strategy is determined by the url format.
     * @param remote_url string representation of well-formed URL of wikipage 
     * to be retrieved
     * @param projectdir string representation of path to the project-dir 
     * where the file should be saved.
     */
    public static void doWikiGet(String remote_url, String projectdir) {
        try {
            String joined = null;
            String name = null;
            if (remote_url.indexOf("index.php?title=") > 0) {
                String[] splitted = remote_url.split("index.php\\?title=");
                String s = splitted[splitted.length - 1];
                name = s;
                s = s.replaceAll(" ", "_");
                splitted[splitted.length - 1] = s;
                joined = joinString("index.php?title=", splitted);
                joined = joined + "&action=raw";
            } else {
                String[] splitted = remote_url.split("/");
                String s = splitted[splitted.length - 1];
                name = s;
                s = s.replaceAll(" ", "_");
                splitted[splitted.length - 1] = s;
                joined = joinString("/", splitted);
                joined = joined + "?action=raw";
            }
            String page = getURL(joined);
            saveUTF8(projectdir, name + ".UTF8", page);
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    /** 
     * Print UTF-8 text to stdout 
     * (useful for debugging) 
     * @param output  The UTF-8 format string to be printed.
     */
    public static void printUTF8(String output) {
        try {
            BufferedWriter out = UTF8WriterBuilder(System.out);
            out.write(output);
            out.flush();
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    /** 
     * Creates new BufferedWriter configured for UTF-8 output and connects it 
     * to an OutputStream
     * @param out  Outputstream to connect to.
     */
    public static BufferedWriter UTF8WriterBuilder(OutputStream out) throws Exception {
        return new BufferedWriter(new OutputStreamWriter(out, "UTF-8"));
    }

    /** 
     * Save UTF-8 format data to file.
     * @param dir	directory to write to.
     * @param filename filename of file to write.
     * @param output  UTF-8 format text to write
     */
    public static void saveUTF8(String dir, String filename, String output) {
        try {
            filename = filename.replaceAll("[\\\\/:\\*\\?\\\"\\|\\<\\>]", "_");
            File path = new File(dir, filename);
            FileOutputStream f = new FileOutputStream(path);
            BufferedWriter out = UTF8WriterBuilder(f);
            out.write(output);
            out.close();
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    /** 
     * Obtain UTF-8 format text from remote URL.
     * @param target  String representation of well-formed URL.
     */
    public static String getURL(String target) {
        StringBuffer page = new StringBuffer();
        try {
            URL url = new URL(target);
            InputStream in = url.openStream();
            byte[] b = new byte[4096];
            for (int n; (n = in.read(b)) != -1; ) {
                page.append(new String(b, 0, n, "UTF-8"));
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
        return page.toString();
    }

    /**
     * Post data to the remote URL.
     * 
     * @param address
     *            address to post
     * @param params
     *            parameters
     * @return sever output
     */
    public static String post(String address, Map<String, String> params) throws IOException {
        URL url = new URL(address);
        ByteArrayOutputStream pout = new ByteArrayOutputStream();
        for (Map.Entry<String, String> p : params.entrySet()) {
            if (pout.size() > 0) {
                pout.write('&');
            }
            pout.write(p.getKey().getBytes(OConsts.UTF8));
            pout.write('=');
            pout.write(URLEncoder.encode(p.getValue(), OConsts.UTF8).getBytes(OConsts.UTF8));
        }
        HttpURLConnection conn = (HttpURLConnection) url.openConnection();
        try {
            conn.setRequestMethod("POST");
            conn.setRequestProperty("Content-Type", "application/x-www-form-urlencoded");
            conn.setRequestProperty("Content-Length", Integer.toString(pout.size()));
            conn.setDoInput(true);
            conn.setDoOutput(true);
            OutputStream cout = conn.getOutputStream();
            cout.write(pout.toByteArray());
            cout.flush();
            if (conn.getResponseCode() != HttpURLConnection.HTTP_OK) {
                throw new IOException(conn.getResponseMessage());
            }
            String contentType = conn.getHeaderField("Content-Type");
            int cp = contentType != null ? contentType.indexOf(CHARSET_MARK) : -1;
            String charset = cp >= 0 ? contentType.substring(cp + CHARSET_MARK.length()) : "ISO8859-1";
            ByteArrayOutputStream res = new ByteArrayOutputStream();
            InputStream in = conn.getInputStream();
            try {
                LFileCopy.copy(in, res);
            } finally {
                in.close();
            }
            return new String(res.toByteArray(), charset);
        } finally {
            conn.disconnect();
        }
    }
}
