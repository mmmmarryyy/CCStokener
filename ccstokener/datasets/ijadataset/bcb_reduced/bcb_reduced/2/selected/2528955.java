package gnu.java.net.protocol.jar;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.net.JarURLConnection;
import java.net.MalformedURLException;
import java.net.ProtocolException;
import java.net.URL;
import java.net.URLConnection;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Hashtable;
import java.util.Locale;
import java.util.jar.JarEntry;
import java.util.jar.JarFile;
import java.util.zip.ZipFile;

/**
 * This subclass of java.net.JarURLConnection models a URLConnection via
 * the "jar" protocol.
 *
 * @author Kresten Krab Thorup (krab@gnu.org)
 */
public final class Connection extends JarURLConnection {

    /**
   * HTTP-style DateFormat, used to format the last-modified header.
   * Lazy initialized since jar files are used during bootstrapping.
   */
    private static SimpleDateFormat dateFormat;

    private JarFile jar_file;

    private JarEntry jar_entry;

    private URL jar_url;

    public static class JarFileCache {

        private static Hashtable cache = new Hashtable();

        private static final int READBUFSIZE = 4 * 1024;

        public static synchronized JarFile get(URL url, boolean useCaches) throws IOException {
            JarFile jf;
            if (useCaches) {
                jf = (JarFile) cache.get(url);
                if (jf != null) return jf;
            }
            if ("file".equals(url.getProtocol())) {
                String fn = url.getFile();
                fn = gnu.java.net.protocol.file.Connection.unquote(fn);
                File f = new File(fn);
                jf = new JarFile(f, true, ZipFile.OPEN_READ);
            } else {
                URLConnection urlconn = url.openConnection();
                InputStream is = urlconn.getInputStream();
                byte[] buf = new byte[READBUFSIZE];
                File f = File.createTempFile("cache", "jar");
                FileOutputStream fos = new FileOutputStream(f);
                int len = 0;
                while ((len = is.read(buf)) != -1) {
                    fos.write(buf, 0, len);
                }
                fos.close();
                jf = new JarFile(f, true, ZipFile.OPEN_READ | ZipFile.OPEN_DELETE);
            }
            if (useCaches) cache.put(url, jf);
            return jf;
        }
    }

    protected Connection(URL url) throws MalformedURLException {
        super(url);
    }

    public synchronized void connect() throws IOException {
        if (connected) return;
        jar_url = getJarFileURL();
        jar_file = JarFileCache.get(jar_url, useCaches);
        String entry_name = getEntryName();
        if (entry_name != null && !entry_name.equals("")) {
            jar_entry = (JarEntry) jar_file.getEntry(entry_name);
            if (jar_entry == null) throw new IOException("No entry for " + entry_name + " exists.");
        }
        connected = true;
    }

    public InputStream getInputStream() throws IOException {
        if (!connected) connect();
        if (!doInput) throw new ProtocolException("Can't open InputStream if doInput is false");
        if (jar_entry == null) throw new IOException(jar_url + " couldn't be found.");
        return jar_file.getInputStream(jar_entry);
    }

    public synchronized JarFile getJarFile() throws IOException {
        if (!connected) connect();
        if (!doInput) throw new ProtocolException("Can't open JarFile if doInput is false");
        return jar_file;
    }

    public String getHeaderField(String field) {
        try {
            if (!connected) connect();
            if (field.equals("content-type")) return guessContentTypeFromName(getJarEntry().getName()); else if (field.equals("content-length")) return Long.toString(getJarEntry().getSize()); else if (field.equals("last-modified")) {
                synchronized (this.getClass()) {
                    if (dateFormat == null) dateFormat = new SimpleDateFormat("EEE, dd MMM yyyy hh:mm:ss 'GMT'", new Locale("En", "Us", "Unix"));
                    return dateFormat.format(new Date(getJarEntry().getTime()));
                }
            }
        } catch (IOException e) {
        }
        return null;
    }

    public int getContentLength() {
        if (!connected) return -1;
        return (int) jar_entry.getSize();
    }

    public long getLastModified() {
        if (!connected) return -1;
        try {
            return getJarEntry().getTime();
        } catch (IOException e) {
            return -1;
        }
    }
}
