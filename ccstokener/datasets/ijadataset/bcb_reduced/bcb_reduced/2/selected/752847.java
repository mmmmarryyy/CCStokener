package fr.loria.ecoo.lpbcast.util;

import java.io.BufferedOutputStream;
import java.io.File;
import java.io.FileOutputStream;
import java.io.InputStream;
import java.io.ObjectOutputStream;
import java.net.HttpURLConnection;
import java.net.URI;
import java.net.URL;
import java.net.URLConnection;

/**
 * DOCUMENT ME!
 *
 * @author $author$
 * @version $Revision$
 */
public class NetUtil {

    /** DOCUMENT ME! */
    public static int READ_TIME_OUT = 60000;

    /**
     * DOCUMENT ME!
     *
     * @param url DOCUMENT ME!
     *
     * @return DOCUMENT ME!
     *
     * @throws Exception DOCUMENT ME!
     */
    public static synchronized File getFileViaHTTPRequest(URL url) throws Exception {
        URLConnection con = url.openConnection();
        if ((con == null) || con.getHeaderField("Content-type").equals("null")) {
            return null;
        }
        File file = File.createTempFile("state", ".zip");
        BufferedOutputStream bos = new BufferedOutputStream(new FileOutputStream(file.getAbsolutePath()));
        InputStream in = con.getInputStream();
        byte[] buffer = new byte[1024];
        int numRead;
        while ((numRead = in.read(buffer)) != -1) {
            bos.write(buffer, 0, numRead);
            bos.flush();
        }
        return file;
    }

    /**
     * DOCUMENT ME!
     *
     * @param url DOCUMENT ME!
     * @param object DOCUMENT ME!
     *
     * @throws Exception DOCUMENT ME!
     */
    public static synchronized void sendObjectViaHTTPRequest(URL url, Object object) throws Exception {
        HttpURLConnection init = (HttpURLConnection) url.openConnection();
        init.setConnectTimeout(READ_TIME_OUT);
        init.setReadTimeout(READ_TIME_OUT);
        init.setUseCaches(false);
        init.setDoOutput(true);
        init.setRequestProperty("Content-type", "application/octet-stream");
        ObjectOutputStream out = new ObjectOutputStream(init.getOutputStream());
        out.writeObject(object);
        out.flush();
        init.getResponseCode();
        out.close();
        init.disconnect();
    }

    /**
     * DOCUMENT ME!
     *
     * @param url DOCUMENT ME!
     * @param header DOCUMENT ME!
     *
     * @return DOCUMENT ME!
     *
     * @throws Exception DOCUMENT ME!
     */
    public static synchronized boolean testHTTPRequestHeader(URL url, String header) throws Exception {
        HttpURLConnection init = (HttpURLConnection) url.openConnection();
        init.setConnectTimeout(READ_TIME_OUT);
        init.setReadTimeout(READ_TIME_OUT);
        init.setUseCaches(false);
        init.setRequestMethod("GET");
        return (init.getHeaderField(header) != null);
    }

    /**
     * DOCUMENT ME!
     *
     * @param url DOCUMENT ME!
     *
     * @return DOCUMENT ME!
     *
     * @throws Exception DOCUMENT ME!
     */
    public static String normalize(String url) throws Exception {
        URI uri = new URI(url);
        uri = uri.normalize();
        String path = uri.getPath();
        if (!path.startsWith("/")) {
            path = "/" + path;
        }
        if (path.endsWith("/")) {
            path = path.substring(0, path.length() - 1);
        }
        String urlStr = uri.getScheme() + "://" + uri.getHost();
        int port = uri.getPort();
        if (port != -1) {
            urlStr = urlStr + ":" + port;
        }
        urlStr = urlStr + path;
        return urlStr;
    }
}
