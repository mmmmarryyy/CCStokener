package xbrlcore.taxonomy;

import java.io.BufferedInputStream;
import java.io.BufferedOutputStream;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.net.Proxy;
import java.net.URL;
import java.net.URLConnection;

/**
 * <p>
 * The {@link CachingFileLoader} caches all externally (from the internet) loaded files
 * in a local temp folder. This avoids expensive loading of these files again and again.
 * </p>
 * 
 * @author Marvin Froehlich (INFOLOG GmbH)
 */
public class CachingFileLoader extends FileLoader {

    private final File cacheFolder;

    public CachingFileLoader(File cacheFolder) {
        this(cacheFolder, null);
    }

    public CachingFileLoader(File cacheFolder, Proxy proxy) {
        super(proxy);
        if (!cacheFolder.exists()) throw new IllegalArgumentException("Cache folder \"" + cacheFolder.getAbsolutePath() + "\" not found.");
        if (!cacheFolder.canWrite()) throw new IllegalArgumentException("Cache folder \"" + cacheFolder.getAbsolutePath() + "\" not writable.");
        this.cacheFolder = cacheFolder;
    }

    private void downloadFile(URL src, File dst) throws IOException {
        BufferedInputStream in = null;
        BufferedOutputStream out = null;
        try {
            in = new BufferedInputStream(super.openConnection(src).getInputStream());
        } catch (FileNotFoundException e) {
            File f2 = new File(dst.getParentFile(), dst.getName() + "-not_found");
            f2.createNewFile();
            return;
        }
        try {
            out = new BufferedOutputStream(new FileOutputStream(dst));
            byte[] buffer = new byte[1024];
            int n = 0;
            while ((n = in.read(buffer, 0, Math.min(buffer.length, in.available() + 1))) >= 0) {
                if (n > 0) out.write(buffer, 0, n);
            }
        } finally {
            if (out != null) try {
                out.close();
            } catch (IOException e) {
            }
            try {
                in.close();
            } catch (IOException e) {
            }
        }
    }

    @Override
    public URLConnection openConnection(URL url) throws IOException {
        if (!url.getProtocol().equals("file")) {
            String name = url.getFile();
            File f = new File(cacheFolder, ((name.charAt(0) == '/') ? name.substring(1) : name).replace('/', File.separatorChar));
            if (!f.exists()) {
                File f2 = new File(f.getParentFile(), f.getName() + "-not_found");
                if (!f2.exists()) {
                    try {
                        f.getParentFile().mkdirs();
                        downloadFile(url, f);
                    } catch (IOException e) {
                        f.delete();
                        throw e;
                    }
                }
            }
            return f.toURI().toURL().openConnection();
        }
        return super.openConnection(url);
    }
}
