package fr.loria.ecoo.pbcast;

import java.io.BufferedInputStream;
import java.io.BufferedOutputStream;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.ObjectOutputStream;
import java.net.HttpURLConnection;
import java.net.URI;
import java.net.URL;
import java.net.URLConnection;

public class NetUtil {

    public static int READ_TIME_OUT = 60000;

    public static synchronized File getFileViaHTTPRequest(URL url) throws Exception {
        File file = File.createTempFile("state", ".zip");
        BufferedOutputStream bos = new BufferedOutputStream(new FileOutputStream(file.getAbsolutePath()));
        URLConnection con = url.openConnection();
        InputStream in = con.getInputStream();
        byte[] buffer = new byte[1024];
        int numRead;
        while ((numRead = in.read(buffer)) != -1) {
            bos.write(buffer, 0, numRead);
            bos.flush();
        }
        return file;
    }

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
        System.out.println("http:" + init.getResponseMessage());
    }

    public static synchronized boolean testHTTPRequestHeader(URL url, String header) throws Exception {
        HttpURLConnection init = (HttpURLConnection) url.openConnection();
        init.setConnectTimeout(READ_TIME_OUT);
        init.setReadTimeout(READ_TIME_OUT);
        init.setUseCaches(false);
        init.setRequestMethod("GET");
        if (init.getHeaderField(header) != null) {
            return true;
        }
        return false;
    }

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
