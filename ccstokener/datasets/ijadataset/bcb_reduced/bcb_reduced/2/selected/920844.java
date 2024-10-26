package org.neblinux.net;

import java.io.BufferedInputStream;
import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.OutputStream;
import java.net.HttpURLConnection;
import java.net.URL;
import java.net.URLConnection;

@Deprecated
public class Wget2 {

    public static byte[] leerUrl(String url) throws IOException {
        Wget2 w = new Wget2(url, null);
        return w.getBytes();
    }

    private OutputStream by = null;

    public Wget2(String url, File f) throws IOException {
        this(new URL(url), f);
    }

    public Wget2(URL url, File f) throws IOException {
        System.out.println("bajando: " + url);
        if (f == null) {
            by = new ByteArrayOutputStream();
        } else {
            by = new FileOutputStream(f);
        }
        URLConnection uc = url.openConnection();
        if (uc instanceof HttpURLConnection) {
            leerHttp((HttpURLConnection) uc);
        } else {
            throw new IOException("solo se pueden descargar url http");
        }
    }

    public void close() throws IOException {
        by.close();
    }

    private byte[] getBytes() {
        if (by instanceof ByteArrayOutputStream) {
            return ((ByteArrayOutputStream) by).toByteArray();
        } else {
            return null;
        }
    }

    private void leerHttp(HttpURLConnection url) throws IOException {
        if (url.getResponseCode() != HttpURLConnection.HTTP_OK) {
            throw new IOException("La url especificada no existe o se presento un error en la conexion");
        }
        int i = url.getContentLength();
        System.out.println("tamaño: " + i);
        if (i > 12000000) {
            System.out.println("muyyy grandeee");
            return;
        }
        BufferedInputStream is = new BufferedInputStream(url.getInputStream());
        byte[] bi = new byte[2048];
        int l = is.read(bi);
        while (l != -1) {
            by.write(bi, 0, l);
            l = is.read(bi);
        }
        is.close();
        by.close();
        url.disconnect();
    }
}
