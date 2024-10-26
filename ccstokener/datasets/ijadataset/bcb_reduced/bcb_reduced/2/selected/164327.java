package com.memoire.vfs;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.MalformedURLException;
import java.net.URL;
import java.net.URLConnection;
import java.net.URLStreamHandler;
import com.memoire.fu.FuLib;

/**
 * Data protocol.
 */
public class VfsUrlData {

    private static class Connection extends URLConnection {

        private boolean connected_;

        private InputStream in_;

        private long modified_;

        private int length_ = -1;

        private String type_;

        private byte[] data_;

        public Connection(URL _url) {
            super(_url);
        }

        public synchronized void connect() throws IOException {
            if (connected_) return;
            String s = getURL().toString();
            int p = s.indexOf(',');
            if (p < 0) throw new MalformedURLException(s);
            String data = s.substring(p + 1);
            if (s.substring(0, p).endsWith("base64")) data_ = FuLib.decodeBase64(data); else data_ = FuLib.decodeWwwFormUrl(data).getBytes();
            length_ = data_.length;
            s = s.substring(5, p);
            String[] t = FuLib.split(s, ';');
            if (t.length == 0) type_ = "text/plain"; else type_ = t[0];
            if (getDoInput()) in_ = new ByteArrayInputStream(data_);
            connected_ = true;
        }

        public InputStream getInputStream() throws IOException {
            connect();
            return in_;
        }

        public OutputStream getOutputStream() throws IOException {
            connect();
            return null;
        }

        public long getLastModified() {
            return modified_;
        }

        public int getContentLength() {
            return length_;
        }

        public String getContentType() {
            return type_;
        }
    }

    private static boolean init_ = false;

    static {
        init();
    }

    public static void init() {
        if (init_) return;
        init_ = true;
        URLStreamHandler h = new URLStreamHandler() {

            protected URLConnection openConnection(URL _url) throws IOException {
                return new Connection(_url);
            }
        };
        FuLib.setUrlHandler("data", h);
    }
}
