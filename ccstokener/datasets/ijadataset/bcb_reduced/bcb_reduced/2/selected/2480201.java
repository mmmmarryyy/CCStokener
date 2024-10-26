package gnu.protocol.zip;

import gnu.protocol.*;
import java.io.*;
import java.net.*;
import java.util.zip.*;
import java.util.jar.*;

public abstract class ZipEntryInputStream {

    public static TerminatedInputStream find(InputStream stream, String entryName) throws IOException {
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

    public static TerminatedInputStream find(URL url, String entryName) throws IOException {
        if (url.getProtocol().equals("file")) {
            return find(new File(url.getFile()), entryName);
        } else {
            return find(url.openStream(), entryName);
        }
    }

    public static TerminatedInputStream find(File file, String entryName) throws IOException {
        entryName = entryName.replace(File.separatorChar, '/');
        ZipFile zip;
        try {
            zip = new JarFile(file);
        } catch (NoClassDefFoundError x) {
            zip = new ZipFile(file);
        }
        ZipEntry entry = zip.getEntry(entryName);
        if (entry != null) {
            return new TerminatedInputStream(zip.getInputStream(entry), entry.getSize());
        }
        zip.close();
        return null;
    }
}
