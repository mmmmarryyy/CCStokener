package com.memoire.vfs;

import java.io.BufferedInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.URL;
import java.net.URLConnection;
import java.net.URLStreamHandler;
import java.util.zip.GZIPInputStream;
import com.memoire.fu.FuLib;

/**
 * Gzip protocol.
 */
public class VfsUrlGzip {

    private static class Connection extends URLConnection {

        private boolean connected_;

        private InputStream in_;

        private long modified_;

        private int length_ = -1;

        private String type_;

        public Connection(URL _url) {
            super(_url);
        }

        public synchronized void connect() throws IOException {
            if (connected_) return;
            String f = getURL().getFile();
            URL u = new URL(f);
            URLConnection c = u.openConnection();
            modified_ = c.getLastModified();
            length_ = -1;
            type_ = c.getContentType();
            if (getDoInput()) {
                in_ = new BufferedInputStream(c.getInputStream());
                if (FuLib.isGziped(in_)) in_ = new GZIPInputStream(in_);
            }
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
        FuLib.setUrlHandler("gzip", h);
    }
}
