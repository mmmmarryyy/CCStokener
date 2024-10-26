package websphinx;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.io.PrintStream;
import java.io.RandomAccessFile;
import java.net.HttpURLConnection;
import java.net.URL;
import java.net.URLConnection;
import java.util.Vector;

public class Access {

    private File tempDir;

    private Vector temps = new Vector();

    public Access() {
        String tempDirName;
        try {
            tempDirName = System.getProperty("websphinx.temp.directory");
        } catch (SecurityException e) {
            tempDirName = null;
        }
        if (tempDirName == null) {
            tempDirName = System.getProperty("java.io.tmpdir");
        }
        if (tempDirName == null) {
            String os = System.getProperty("os.name");
            tempDirName = (os.startsWith("Windows")) ? "c:\\temp\\" : "/tmp/";
        }
        if (!(tempDirName.endsWith("/") || tempDirName.endsWith(File.separator))) tempDirName += "/";
        tempDir = new File(tempDirName);
    }

    public URLConnection openConnection(URL url) throws IOException {
        URLConnection conn = url.openConnection();
        conn.connect();
        return conn;
    }

    public URLConnection openConnection(Link link) throws IOException {
        int method = link.getMethod();
        URL url;
        switch(method) {
            case Link.GET:
                url = link.getPageURL();
                break;
            case Link.POST:
                url = link.getServiceURL();
                break;
            default:
                throw new IOException("Unknown HTTP method " + link.getMethod());
        }
        URLConnection conn = url.openConnection();
        DownloadParameters dp = link.getDownloadParameters();
        if (dp != null) {
            conn.setAllowUserInteraction(dp.getInteractive());
            conn.setUseCaches(dp.getUseCaches());
            String userAgent = dp.getUserAgent();
            if (userAgent != null) {
                conn.setRequestProperty("User-Agent", userAgent);
            }
            String acceptLanguage = dp.getAcceptLanguage();
            if (acceptLanguage != null) {
                conn.setRequestProperty("Accept-Language", acceptLanguage);
            }
            String types = dp.getAcceptedMIMETypes();
            if (types != null) {
                conn.setRequestProperty("accept", types);
            }
        }
        if (method == Link.POST) {
            if (conn instanceof HttpURLConnection) ((HttpURLConnection) conn).setRequestMethod("POST");
            String query = link.getQuery();
            if (query.startsWith("?")) query = query.substring(1);
            conn.setDoOutput(true);
            conn.setRequestProperty("Content-type", "application/x-www-form-urlencoded");
            conn.setRequestProperty("Content-length", String.valueOf(query.length()));
            PrintStream out = new PrintStream(conn.getOutputStream());
            out.print(query);
            out.flush();
        }
        conn.connect();
        return conn;
    }

    public InputStream readFile(File file) throws IOException {
        return new FileInputStream(file);
    }

    public OutputStream writeFile(File file, boolean append) throws IOException {
        return new FileOutputStream(file.toString(), append);
    }

    public RandomAccessFile readWriteFile(File file) throws IOException {
        return new RandomAccessFile(file, "rw");
    }

    public void makeDir(File file) throws IOException {
        file.mkdirs();
    }

    public File getTemporaryDirectory() {
        return tempDir;
    }

    public File makeTemporaryFile(String basename, String extension) {
        File dir = getTemporaryDirectory();
        File f;
        synchronized (temps) {
            do f = new File(dir, basename + String.valueOf((int) (Math.random() * 999999)) + extension); while (temps.contains(f) || f.exists());
            temps.addElement(f);
        }
        return f;
    }

    public void deleteAllTempFiles() {
        synchronized (temps) {
            for (int i = 0; i < temps.size(); ++i) {
                File f = (File) temps.elementAt(i);
                f.delete();
            }
            temps.setSize(0);
        }
    }

    private static Access theAccess = new Access();

    public static Access getAccess() {
        return theAccess;
    }

    public static void setAccess(Access access) {
        theAccess = access;
    }
}
