package com.memoire.fu;

import java.io.*;
import java.net.*;
import java.util.*;
import java.lang.reflect.*;

/**
 * File in memory (ram disk).
 */
public class FuFileRam extends FuFile {

    private static class Connection extends URLConnection {

        private boolean connected_ = false;

        private InputStream in_ = null;

        private OutputStream out_ = null;

        private long modified_ = 0l;

        private long length_ = 0l;

        private String type_ = "content/unknown";

        public Connection(URL _url) {
            super(_url);
        }

        public synchronized void connect() throws IOException {
            if (connected_) return;
            FuFileRam f = new FuFileRam(getURL().getFile());
            if (getDoInput()) in_ = f.getInputStream();
            if (getDoOutput()) out_ = f.getOutputStream();
            modified_ = f.lastModified();
            length_ = f.length();
            type_ = guessContentTypeFromName(f.getPath());
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

        public long getLength() throws IOException {
            connect();
            return length_;
        }

        public String getContentType() {
            return type_;
        }
    }

    private static final void setAccessible(Member _m, boolean _b) {
        try {
            Method m = _m.getClass().getMethod("setAccessible", new Class[] { Boolean.TYPE });
            m.invoke(_m, new Object[] { (_b ? Boolean.TRUE : Boolean.FALSE) });
        } catch (Exception ex) {
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
                System.err.println("FFR: openConnection(" + _url + ")");
                return new Connection(_url);
            }
        };
        System.err.println("FFR: set the ram protocol for URLs");
        try {
            Field field = URL.class.getDeclaredField("handlers");
            setAccessible(field, true);
            Hashtable t = (Hashtable) field.get(null);
            t.put("ram", h);
        } catch (Exception ex) {
            System.err.println(ex);
        }
        try {
            Method m = File.class.getMethod("listRoots", new Class[0]);
            File[] l = (File[]) m.invoke(null, new Object[0]);
            for (int i = 0; i < l.length; i++) {
                String p = l[i].getPath();
                createRootPath(p);
            }
        } catch (NoSuchMethodException ex1) {
        } catch (Exception ex2) {
            ex2.printStackTrace();
        }
        createPropertyPath("user.dir");
        createPropertyPath("user.home");
        createPropertyPath("java.io.tmpdir");
    }

    private static void createRootPath(String _path) {
        if (_path != null) new FuFileRam(_path).mkdir();
    }

    private static void createPropertyPath(String _name) {
        String p = null;
        try {
            p = System.getProperty(_name);
        } catch (SecurityException ex) {
        }
        if (p != null) {
            new FuFileRam(p).mkdirs();
        }
    }

    public FuFileRam(File _file, String _name) {
        super(_file, _name);
    }

    public FuFileRam(String _path) {
        super(_path);
    }

    public FuFileRam(String _file, String _name) {
        super(_file, _name);
    }

    public final FuFile createChild(String _name) {
        return new FuFileRam(this, _name);
    }

    public InputStream getInputStream() throws IOException {
        if (!canRead()) return null;
        byte[] bytes;
        if (isDirectory()) {
            String separator;
            try {
                separator = System.getProperty("line.separator");
            } catch (SecurityException ex) {
                separator = "\n";
            }
            String[] f = list();
            int l = f.length;
            StringBuffer b = new StringBuffer(l * 80);
            for (int i = 0; i < l; i++) {
                b.append(f[i]);
                b.append(separator);
            }
            bytes = b.toString().getBytes();
        } else {
            bytes = FuRamDisk.getInstance().getOutputStream(getAbsolutePath()).toByteArray();
        }
        return new ByteArrayInputStream(bytes);
    }

    public OutputStream getOutputStream() throws IOException {
        if (!canWrite()) return null;
        if (isDirectory()) throw new UnknownServiceException("protocol doesn't support output");
        String p = getAbsolutePath();
        FuRamDisk.getInstance().createFile(p);
        return FuRamDisk.getInstance().getOutputStream(p);
    }

    public String getViewText() {
        return FuLib.reducedPath(getAbsolutePath());
    }

    public boolean canRead() {
        return FuRamDisk.getInstance().canRead(getAbsolutePath());
    }

    public boolean canWrite() {
        return FuRamDisk.getInstance().canWrite(getAbsolutePath());
    }

    public boolean delete() {
        return FuRamDisk.getInstance().delete(getAbsolutePath());
    }

    public boolean equals(Object _o) {
        if ((_o == null) || !(_o instanceof File)) return false;
        String a = getAbsolutePath();
        String b = ((File) _o).getAbsolutePath();
        if (a.endsWith("/")) a = a.substring(0, a.length() - 1);
        if (b.endsWith("/")) b = b.substring(0, b.length() - 1);
        return a.equals(b);
    }

    public boolean exists() {
        return FuRamDisk.getInstance().exists(getAbsolutePath());
    }

    public String getAbsolutePath() {
        return super.getAbsolutePath();
    }

    public String getCanonicalPath() throws IOException {
        return getAbsolutePath();
    }

    public String getParent() {
        return super.getParent();
    }

    public String getPath() {
        return super.getPath();
    }

    public int hashCode() {
        return super.hashCode();
    }

    public boolean isAbsolute() {
        return super.isAbsolute();
    }

    public boolean isDirectory() {
        return FuRamDisk.getInstance().isDirectory(getAbsolutePath());
    }

    public boolean isFile() {
        return FuRamDisk.getInstance().isFile(getAbsolutePath());
    }

    public long lastModified() {
        return FuRamDisk.getInstance().lastModified(getAbsolutePath());
    }

    public long length() {
        return FuRamDisk.getInstance().length(getAbsolutePath());
    }

    public String[] list() {
        String p = getAbsolutePath();
        if (!p.endsWith(File.separator)) p += File.separator;
        String[] paths = FuRamDisk.getInstance().getPaths();
        int lp = paths.length;
        int ls = lp;
        boolean hasParent = (getParent() != null);
        if (hasParent) ls++;
        ls++;
        String[] s = new String[ls];
        int i = 0;
        s[i] = ".";
        i++;
        if (hasParent) {
            s[i] = "..";
            i++;
        }
        int n = p.length();
        for (int j = 0; j < lp; j++) {
            String q = paths[j];
            if (!q.equals(p) && q.startsWith(p) && (q.indexOf(File.separator, n) < 0)) {
                s[i] = q.substring(n);
                i++;
            }
        }
        String[] r = new String[i];
        System.arraycopy(s, 0, r, 0, i);
        return r;
    }

    public File[] listFiles() {
        String[] s = list();
        if (s == null) return null;
        int n = s.length;
        File[] r = new File[n];
        for (int i = 0; i < n; i++) r[i] = createChild(s[i]);
        return r;
    }

    public boolean mkdir() {
        if (exists()) return false;
        FuRamDisk.getInstance().createDirectory(getAbsolutePath());
        return true;
    }

    public boolean mkdirs() {
        return exists() ? false : mkdirs0();
    }

    protected boolean mkdirs0() {
        Vector v = new Vector();
        FuFileRam f = this;
        while ((f != null) && !f.exists()) {
            v.addElement(f);
            f = new FuFileRam(f.getParent());
        }
        boolean r = true;
        for (int i = v.size() - 1; i >= 0; i--) {
            f = (FuFileRam) v.elementAt(i);
            r &= f.mkdir();
        }
        return r;
    }

    public synchronized boolean renameTo(File _dest) {
        return FuRamDisk.getInstance().rename(getAbsolutePath(), _dest.getAbsolutePath());
    }

    public String toString() {
        return super.toString();
    }

    public URL toURL() throws MalformedURLException {
        String r = getAbsolutePath();
        if (separatorChar != '/') r = r.replace(separatorChar, '/');
        if (!r.startsWith("/")) r = "/" + r;
        if (!r.endsWith("/") && isDirectory()) r = r + "/";
        return new URL("ram", "", r);
    }

    private static void test(File f) {
        try {
            System.err.println("getName : " + f.getName());
            System.err.println("getPath : " + f.getPath());
            System.err.println("isDir   : " + f.isDirectory());
            System.err.println("isFile  : " + f.isFile());
            System.err.println("canRead : " + f.canRead());
            System.err.println("canWrite: " + f.canWrite());
        } catch (Exception ex) {
            ex.printStackTrace();
        }
    }

    private static void compare(String p) {
        test(new FuFileRam(p));
        System.err.println("-------");
        test(new File(p));
        System.err.println("-----------------------------------------------");
    }

    public static void main(String[] _args) {
        if (_args.length == 0) _args = new String[] { "/tmp" };
        System.err.println("-----------------------------------------------");
        for (int i = 0; i < _args.length; i++) compare(_args[i]);
    }
}
