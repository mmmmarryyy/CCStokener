package gnu.protocol.zip;

import gnu.protocol.*;
import java.io.*;
import java.net.*;
import java.util.zip.*;

public class ZipConnection extends URLConnection {

    public static URL url(File file, String entryName) throws IOException {
        entryName = entryName.replace(File.separatorChar, '/');
        ZipFile zip = new ZipFile(file);
        ZipEntry entry = zip.getEntry(entryName);
        if (entry != null) {
            return new URL("zip:" + FileUtil.url(file).toString() + "!/" + entryName);
        }
        return null;
    }

    public static TerminatedInputStream openStream(InputStream stream, String entryName) throws IOException {
        entryName = entryName.replace(File.separatorChar, '/');
        ZipInputStream zipStream = new ZipInputStream(stream);
        ZipEntry entry = zipStream.getNextEntry();
        while (entry != null) {
            if (entry.getName().equals(entryName)) {
                return new TerminatedInputStream(zipStream, entry.getSize());
            }
            entry = zipStream.getNextEntry();
        }
        return null;
    }

    public static TerminatedInputStream openStream(URL url, String entryName) throws IOException {
        if (url.getProtocol().equals("file")) {
            return openStream(new File(url.getFile()), entryName);
        } else {
            return openStream(url.openStream(), entryName);
        }
    }

    public static TerminatedInputStream openStream(File file, String entryName) throws IOException {
        entryName = entryName.replace(File.separatorChar, '/');
        ZipFile zip = new ZipFile(file);
        ZipEntry entry = zip.getEntry(entryName);
        if (entry != null) {
            return new TerminatedZipInputStream(zip.getInputStream(entry), entry.getSize(), zip);
        }
        zip.close();
        return null;
    }

    public ZipConnection(URL url) {
        super(url);
    }

    public void connect() throws IOException {
        if (!connected) {
            String file = getURL().getFile();
            int p = file.indexOf('!');
            String url = file.substring(0, p);
            String entryName = file.substring(p + 1);
            if (url.startsWith("/")) url = url.substring(1);
            if (entryName.startsWith("/")) entryName = entryName.substring(1);
            stream = openStream(new URL(url), entryName);
            connected = true;
        }
    }

    public String getHeaderField(String name) {
        if (name.equals("content-length")) {
            try {
                connect();
            } catch (IOException x) {
                return "-1";
            }
            return String.valueOf(stream.size());
        }
        return super.getHeaderField(name);
    }

    public InputStream getInputStream() throws IOException {
        connect();
        return stream;
    }

    private TerminatedInputStream stream = null;
}
