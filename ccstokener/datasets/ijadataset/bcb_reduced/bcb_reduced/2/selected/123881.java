package oracle.toplink.essentials.ejb.cmp3.persistence;

import java.io.BufferedInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.net.URL;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.logging.Logger;
import java.util.jar.JarInputStream;
import java.util.zip.ZipEntry;

/**
 * This is an implementation of {@link Archive} which is used when container
 * returns some form of URL from which an InputStream in jar format can be
 * obtained. e.g. jar:file:/tmp/a_ear/b.war!/WEB-INF/lib/pu.jar
 *
 * @author Sanjeeb.Sahoo@Sun.COM
 */
public class JarInputStreamURLArchive implements Archive {

    private URL url;

    private List<String> entries = new ArrayList<String>();

    private Logger logger;

    public JarInputStreamURLArchive(URL url) throws IOException {
        this(url, Logger.global);
    }

    public JarInputStreamURLArchive(URL url, Logger logger) throws IOException {
        logger.entering("JarInputStreamURLArchive", "JarInputStreamURLArchive", new Object[] { url });
        this.logger = logger;
        this.url = url;
        init();
    }

    private void init() throws IOException {
        JarInputStream jis = new JarInputStream(new BufferedInputStream(url.openStream()));
        try {
            do {
                ZipEntry ze = jis.getNextEntry();
                if (ze == null) {
                    break;
                }
                if (!ze.isDirectory()) {
                    entries.add(ze.getName());
                }
            } while (true);
        } finally {
            jis.close();
        }
    }

    public Iterator<String> getEntries() {
        return entries.iterator();
    }

    public InputStream getEntry(String entryPath) throws IOException {
        if (!entries.contains(entryPath)) {
            return null;
        }
        JarInputStream jis = new JarInputStream(new BufferedInputStream(url.openStream()));
        do {
            ZipEntry ze = jis.getNextEntry();
            if (ze == null) {
                break;
            }
            if (ze.getName().equals(entryPath)) {
                return jis;
            }
        } while (true);
        assert (false);
        return null;
    }

    public URL getEntryAsURL(String entryPath) throws IOException {
        URL result = entries.contains(entryPath) ? result = new URL("jar:" + url + "!/" + entryPath) : null;
        return result;
    }

    public URL getRootURL() {
        return url;
    }
}
