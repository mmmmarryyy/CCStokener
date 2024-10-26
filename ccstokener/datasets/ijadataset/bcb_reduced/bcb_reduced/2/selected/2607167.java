package net.exclaimindustries.fotobilder;

import java.io.*;
import java.net.*;

/**
 * <code>URLPictureData</code> takes picture data from a URL.  Unlike <code>FilePictureData</code>,
 * though, this will cache the data first.  The cache can be cleared if need be,
 * of course.
 *
 * @author Nicholas Killewald
 */
public class URLPictureData extends AbstractPictureData {

    private byte[] pictureData;

    private URL url;

    private Exception lastException;

    /** Creates a new instance of URLPictureData */
    public URLPictureData(URL url) throws IOException {
        InputStream is = url.openStream();
        ByteArrayOutputStream baos = new ByteArrayOutputStream();
        byte[] b = new byte[512];
        int read;
        while ((read = is.read(b)) != -1) {
            baos.write(b, 0, read);
        }
        b = null;
        is.close();
        pictureData = baos.toByteArray();
        baos.close();
        this.url = url;
        setCached(false);
    }

    public synchronized void loadCache() {
        if (isCached()) return;
        try {
            InputStream is = url.openStream();
            ByteArrayOutputStream baos = new ByteArrayOutputStream();
            byte[] b = new byte[512];
            int read;
            while ((read = is.read(b)) != -1) {
                baos.write(b, 0, read);
            }
            b = null;
            is.close();
            pictureData = baos.toByteArray();
            baos.close();
        } catch (Exception e) {
            lastException = e;
            pictureData = null;
            setCached(false);
        }
    }

    public byte[] getByteArray() {
        loadCache();
        return pictureData;
    }

    public long getSize() {
        loadCache();
        return pictureData.length;
    }

    public String getMD5() {
        loadCache();
        return getMD5From(pictureData);
    }

    public String getMagic() {
        loadCache();
        if (!isCached()) return null;
        return (getMagicFrom(pictureData));
    }

    public String getFilename() {
        return url.getFile();
    }

    public void clearCache() {
        pictureData = null;
        setCached(false);
    }

    public void reload() {
        clearCache();
        loadCache();
    }

    public InputStream getInputStream() throws IOException {
        if (isCached()) {
            return new ByteArrayInputStream(pictureData);
        } else {
            return url.openConnection().getInputStream();
        }
    }

    public Exception getLastException() {
        return lastException;
    }
}
