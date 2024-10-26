package com.ynhenc.comm.util;

import java.io.*;
import java.net.*;
import javax.swing.*;

public class WebCopy {

    public WebCopy() {
    }

    public void copy(String in, Socket out) throws IOException {
        URL url = this.getUrl(in);
        if (url != null) {
            this.copy(url.openStream(), out.getOutputStream());
        } else {
            this.copy(new FileInputStream(in), out.getOutputStream());
        }
    }

    public void copy(String in, String out) throws IOException {
        URL url = this.getUrl(in);
        if (url != null) {
            this.copy(url, new File(out));
        } else {
            this.copy(new File(in), new File(out));
        }
    }

    public void copy(String in, File out) throws IOException {
        URL url = this.getUrl(in);
        if (url != null) {
            this.copy(url, out);
        } else {
            this.copy(new File(in), out);
        }
    }

    private URL getUrl(String url) {
        try {
            URL u = new URL(url);
            return u;
        } catch (Exception e) {
            return null;
        }
    }

    public void copy(URL in, File out) throws IOException {
        this.copy(in.openStream(), new FileOutputStream(out));
    }

    public void copy(File in, File out) throws IOException {
        this.copy(new FileInputStream(in), new FileOutputStream(out));
    }

    public void copy(URL in, OutputStream out) throws IOException {
        this.copy(in.openStream(), out);
    }

    public void copy(Socket in, Socket out) throws IOException {
        this.copy(in.getInputStream(), out.getOutputStream());
    }

    public void copy(InputStream in, OutputStream out) throws IOException {
        int n;
        byte[] bytes = new byte[1024];
        while ((n = in.read(bytes)) > -1) {
            out.write(bytes, 0, n);
        }
    }
}
