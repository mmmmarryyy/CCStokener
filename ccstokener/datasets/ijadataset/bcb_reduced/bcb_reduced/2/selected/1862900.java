package com.memoire.vfs;

import java.io.ByteArrayInputStream;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.MalformedURLException;
import java.net.URL;
import java.net.URLConnection;
import java.net.URLStreamHandler;
import java.util.Enumeration;
import java.util.zip.ZipEntry;
import java.util.zip.ZipFile;
import com.memoire.fu.FuLib;
import com.memoire.fu.FuVectorString;

/**
 * Zip protocol.
 */
public class VfsUrlZip {

    private static class Connection extends URLConnection {

        private boolean connected_;

        private InputStream in_;

        private long modified_;

        private int length_ = -1;

        private String type_ = "text/plain";

        public Connection(URL _url) {
            super(_url);
        }

        public synchronized void connect() throws IOException {
            if (connected_) return;
            String s = getURL().getFile();
            if (s.startsWith("file:")) s = s.substring(5);
            while (s.startsWith("//")) s = s.substring(1);
            int p = s.indexOf("!/");
            String t = s.substring(p + 2);
            s = s.substring(0, p);
            boolean isDirectory = t.equals("") || t.endsWith("/");
            File f = new File(s);
            if (!f.exists()) throw new IOException(s + " does not exist");
            if (!f.canRead()) throw new IOException(s + " is not readable");
            if (getDoInput()) {
                ZipFile zf = new ZipFile(f, ZipFile.OPEN_READ);
                if (isDirectory) {
                    Enumeration e = zf.entries();
                    FuVectorString v = new FuVectorString();
                    while (e.hasMoreElements()) {
                        ZipEntry ze = (ZipEntry) e.nextElement();
                        String ne = ze.getName();
                        if (ne.equals(t)) {
                            modified_ = ze.getTime();
                        } else if (ne.startsWith(t)) {
                            ne = ne.substring(t.length());
                            int q = ne.indexOf("/");
                            if (q >= 0) ne = ne.substring(0, q + 1);
                            v.addElement(ne);
                        }
                    }
                    v.sort();
                    v.uniq();
                    s = FuLib.join(v.toArray(), '\n') + '\n';
                    byte[] b = s.getBytes();
                    type_ = "text/plain";
                    length_ = b.length;
                    in_ = new ByteArrayInputStream(b);
                } else {
                    ZipEntry ze = zf.getEntry(t);
                    if (ze == null) throw new FileNotFoundException("entry not found: " + t);
                    in_ = zf.getInputStream(ze);
                    modified_ = ze.getTime();
                    type_ = guessContentTypeFromName(t);
                    long sz = ze.getSize();
                    if (sz > Integer.MAX_VALUE) sz = -1;
                    length_ = (int) sz;
                }
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

    private static boolean init_;

    static {
        init();
    }

    public static void init() {
        if (init_) return;
        init_ = true;
        URLStreamHandler h = new URLStreamHandler() {

            protected URLConnection openConnection(URL _url) throws IOException {
                URL url = _url;
                String n = url.getFile();
                int p = n.indexOf("!/");
                if (p < 0) return new URL(n).openConnection();
                String u = n.substring(0, p);
                if (!n.startsWith("file:")) {
                    try {
                        VfsCache cache = VfsLib.getCache();
                        if (cache != null) {
                            File f = cache.getFile(new URL(u), VfsCache.CHECK_AND_DOWNLOAD);
                            if (f != null) {
                                n = f.toURL() + n.substring(p);
                                url = new URL("zip:" + n);
                            }
                        }
                    } catch (MalformedURLException ex) {
                        throw new IOException("not an URL: " + u);
                    }
                }
                if (!n.startsWith("file:")) throw new IOException("not a local zip: " + u);
                return new Connection(url);
            }
        };
        FuLib.setUrlHandler("zip", h);
    }
}
