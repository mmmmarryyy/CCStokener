package com.memoire.vfs;

import java.io.BufferedReader;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.net.MalformedURLException;
import java.net.URL;
import java.net.URLConnection;
import com.memoire.fu.FuEmptyArrays;
import com.memoire.fu.FuLog;
import com.memoire.fu.FuVectorString;

/**
 * Remote file.
 */
public class VfsFileUrl extends VfsFile implements FuEmptyArrays {

    protected URL url_;

    protected String name_;

    protected boolean built_;

    protected boolean exists_;

    protected long lastModified_;

    protected long length_;

    protected String type_;

    protected String[] list_;

    protected boolean canRead_;

    protected boolean canWrite_;

    public VfsFileUrl(URL _a, String _n) throws MalformedURLException {
        this(new URL(_a, _n));
    }

    public VfsFileUrl(URL _url) {
        super(_url.getFile());
        url_ = _url;
        list_ = STRING0;
        built_ = false;
        canRead_ = true;
        canWrite_ = true;
    }

    public final void build() {
        if (!built_) {
            built_ = true;
            final boolean[] done = new boolean[] { false };
            Runnable runnable = new Runnable() {

                public void run() {
                    try {
                        exists_ = true;
                        URL url = getContentURL();
                        URLConnection cnx = url.openConnection();
                        cnx.connect();
                        lastModified_ = cnx.getLastModified();
                        length_ = cnx.getContentLength();
                        type_ = cnx.getContentType();
                        if (isDirectory()) {
                            InputStream in = cnx.getInputStream();
                            BufferedReader nr = new BufferedReader(new InputStreamReader(in));
                            FuVectorString v = readList(nr);
                            nr.close();
                            v.sort();
                            v.uniq();
                            list_ = v.toArray();
                        }
                    } catch (Exception ex) {
                        exists_ = false;
                    }
                    done[0] = true;
                }
            };
            Thread t = new Thread(runnable, "VfsFileUrl connection " + getContentURL());
            t.setPriority(Math.max(Thread.MIN_PRIORITY, t.getPriority() - 1));
            t.start();
            for (int i = 0; i < 100; i++) {
                if (done[0]) break;
                try {
                    Thread.sleep(300L);
                } catch (InterruptedException ex) {
                }
            }
            if (!done[0]) {
                t.interrupt();
                exists_ = false;
                canRead_ = false;
                FuLog.warning("VFS: fail to get " + url_);
            }
        }
    }

    public final boolean isBuilt() {
        return built_;
    }

    protected FuVectorString readList(BufferedReader _nr) throws IOException {
        FuLog.error("VFS: readList not defined in " + getClass().getName());
        return new FuVectorString(0, 1);
    }

    public URL getContentURL() {
        return url_;
    }

    public String getContentType() {
        build();
        return type_;
    }

    public final VfsFile createChild(String _name) {
        VfsFile r = null;
        try {
            r = createFile(new URL(url_, _name));
        } catch (MalformedURLException ex) {
        }
        return r;
    }

    public final InputStream getInputStream() throws IOException {
        InputStream r = null;
        if (canRead()) {
            try {
                URLConnection cnx = getContentURL().openConnection();
                cnx.setDoInput(true);
                r = cnx.getInputStream();
            } catch (IOException ex) {
            }
        }
        if (r == null) {
            canRead_ = false;
            throw new IOException("Can not read " + getContentURL());
        }
        return r;
    }

    public final OutputStream getOutputStream() throws IOException {
        OutputStream r = null;
        if (canWrite()) {
            try {
                URLConnection cnx = getContentURL().openConnection();
                cnx.setDoOutput(true);
                r = cnx.getOutputStream();
            } catch (IOException ex) {
            }
        }
        if (r == null) {
            canWrite_ = false;
            throw new IOException("Can not write " + getContentURL());
        }
        return r;
    }

    public String getSeparator() {
        return "/";
    }

    public final String getViewText() {
        return "[" + url_.getHost() + "]" + url_.getFile();
    }

    public final boolean canRead() {
        return canRead_;
    }

    public final boolean canWrite() {
        return canWrite_;
    }

    public final boolean createNewFile() {
        return false;
    }

    public final boolean delete() {
        return false;
    }

    public final void deleteOnExit() {
    }

    public final boolean equals(Object _o) {
        if ((_o == null) || !(_o instanceof File)) return false;
        String a = getAbsolutePath();
        String b = ((File) _o).getAbsolutePath();
        if (a.endsWith("/")) a = a.substring(0, a.length() - 1);
        if (b.endsWith("/")) b = b.substring(0, b.length() - 1);
        return a.equals(b);
    }

    public final int hashCode() {
        String a = getAbsolutePath();
        if (a.endsWith("/")) a = a.substring(0, a.length() - 1);
        return a.hashCode();
    }

    public final boolean exists() {
        build();
        return exists_;
    }

    public String getAbsolutePath() {
        return url_.toString();
    }

    public final String getCanonicalPath() {
        return url_.toString();
    }

    public final String getName() {
        if (name_ != null) return name_;
        String r;
        try {
            r = url_.getFile();
            if ((r != null) && r.endsWith("/")) r = r.substring(0, r.length() - 1);
            if (r != null) {
                int i = r.lastIndexOf('/');
                r = r.substring(i + 1);
            }
        } catch (Exception ex) {
            r = null;
        }
        return r;
    }

    public final String getParent() {
        String r;
        try {
            r = url_.toString();
            if ((r != null) && r.endsWith("!/")) return null; else if ((r != null) && r.endsWith("/")) r = r.substring(0, r.length() - 1);
            int i = r == null ? 0 : r.lastIndexOf('/');
            if (i >= 0 && r != null) {
                r = r.substring(0, i + 1);
                r = new URL(url_, r).toString();
            } else r = null;
        } catch (Exception ex) {
            r = null;
        }
        return r;
    }

    public final String getPath() {
        return url_.toString();
    }

    public final boolean isAbsolute() {
        return true;
    }

    public final boolean isDirectory() {
        String n = url_.getFile();
        return (n == null) || n.endsWith("/");
    }

    public final boolean isFile() {
        return !isDirectory();
    }

    public final boolean isHidden() {
        String n = getName();
        return (n != null) && n.startsWith(".");
    }

    public final long lastModified() {
        build();
        return lastModified_;
    }

    public final long length() {
        build();
        return length_;
    }

    public final String[] list() {
        build();
        return list_;
    }

    public final File[] listFiles() {
        build();
        int l = list_.length;
        File[] r = new File[l];
        for (int i = 0; i < l; i++) r[i] = createChild(list_[i]);
        return r;
    }

    public boolean mkdir() {
        return false;
    }

    public final boolean mkdirs() {
        return false;
    }

    public final boolean renameTo(File _dest) {
        return false;
    }

    public final boolean setLastModified(long _time) {
        return false;
    }

    public final boolean setReadOnly() {
        canWrite_ = false;
        return true;
    }

    public final String toString() {
        return url_.toString();
    }

    public final URL toURL() {
        return url_;
    }
}
