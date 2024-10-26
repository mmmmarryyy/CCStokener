package com.memoire.vfs;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.URL;
import java.net.URLConnection;
import java.net.URLStreamHandler;
import com.memoire.fu.FuLib;

/**
 * Ram protocol.
 */
public class VfsUrlRam {

    private static class Connection extends URLConnection {

        private boolean connected_;

        private InputStream in_;

        private OutputStream out_;

        private long modified_;

        private int length_ = -1;

        private String type_;

        public Connection(URL _url) {
            super(_url);
        }

        public synchronized void connect() throws IOException {
            if (connected_) return;
            VfsFileRam f = new VfsFileRam(getURL().getFile());
            if (getDoInput()) in_ = f.getInputStream();
            if (getDoOutput()) out_ = f.getOutputStream();
            modified_ = f.lastModified();
            type_ = guessContentTypeFromName(f.getPath());
            if (f.length() < Integer.MAX_VALUE) length_ = (int) f.length();
            connected_ = true;
        }

        public InputStream getInputStream() throws IOException {
            connect();
            return in_;
        }

        public OutputStream getOutputStream() throws IOException {
            connect();
            return out_;
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
        FuLib.setUrlHandler("ram", h);
        VfsFileRam.init();
    }
}
