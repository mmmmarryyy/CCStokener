package net.dzzd.utils.io;

import java.io.*;
import java.awt.Image;
import java.awt.Toolkit;
import java.awt.MediaTracker;
import java.awt.Component;
import java.applet.Applet;
import java.applet.AudioClip;
import java.net.URL;
import java.net.URLConnection;
import net.dzzd.utils.Log;
import net.dzzd.access.IProgressListener;
import java.awt.image.*;

public class IOManager implements ImageObserver {

    public static synchronized URLConnection openURLConnection(URL url, boolean allowUserInteraction, boolean doInput, boolean doOutput, boolean useCaches) throws IOException {
        return openURLConnection(url, allowUserInteraction, doInput, doOutput, useCaches, null, null);
    }

    public static synchronized URLConnection openURLConnection(URL url, boolean allowUserInteraction, boolean doInput, boolean doOutput, boolean useCaches, String userAgent, String contentType) throws IOException {
        URLConnection uc;
        uc = url.openConnection();
        uc.setAllowUserInteraction(allowUserInteraction);
        uc.setDoInput(doInput);
        uc.setDoOutput(doOutput);
        uc.setUseCaches(useCaches);
        if (userAgent != null) uc.setRequestProperty("User-Agent", userAgent);
        if (contentType != null) uc.setRequestProperty("Content-Type", contentType);
        return uc;
    }

    public static synchronized void writeData(OutputStream out, byte[] data) throws Exception {
        DataOutputStream dos = null;
        try {
            dos = new DataOutputStream(new BufferedOutputStream(out));
            dos.write(data, 0, data.length);
        } catch (Exception e) {
            throw e;
        } finally {
            if (dos != null) dos.close();
        }
    }

    public static synchronized void writeString(OutputStream out, String sdata) throws Exception {
        DataOutputStream dos = null;
        try {
            dos = new DataOutputStream(new BufferedOutputStream(out));
            dos.writeBytes(sdata);
        } catch (Exception e) {
            throw e;
        } finally {
            if (dos != null) dos.close();
        }
    }

    public static void getResourceInfo(URLConnection c, URLResourceInfo info) {
        info.reset();
        try {
            info.headerStatus = c.getHeaderField(0);
        } catch (Exception e) {
        }
        try {
            info.headerContentEncoding = c.getContentEncoding();
        } catch (Exception e) {
        }
        try {
            info.headerContentLength = c.getContentLength();
        } catch (Exception e) {
        }
        try {
            info.headerContentType = c.getContentType();
        } catch (Exception e) {
        }
        try {
            info.headerDate = c.getDate();
        } catch (Exception e) {
        }
    }

    public static synchronized InputStream openStream(String filename, boolean cache) {
        return openStream(filename, IOManager.class, null, cache);
    }

    public static synchronized InputStream openStream(String filename, Class loader, URLResourceInfo info, boolean cache) {
        URL url;
        URLConnection c = null;
        if (info == null) info = new URLResourceInfo();
        if (loader == null) loader = IOManager.class;
        try {
            url = new URL(filename);
            c = url.openConnection();
            c.setUseCaches(cache);
        } catch (Exception e) {
            Log.log(IOManager.class, e);
            try {
                url = loader.getResource(filename);
                c = url.openConnection();
                c.setUseCaches(cache);
            } catch (Exception e2) {
                Log.log(IOManager.class, e2);
                return null;
            }
        }
        getResourceInfo(c, info);
        try {
            InputStream i = c.getInputStream();
            return i;
        } catch (IOException e) {
            Log.log(IOManager.class, e);
            return null;
        }
    }

    private static synchronized void closeStream(InputStream f) {
        try {
            f.close();
        } catch (Exception e) {
        }
    }

    public static synchronized byte[] downloadData(String resource, IProgressListener pl, boolean cache) {
        return downloadData(resource, IOManager.class, null, pl, cache);
    }

    public static synchronized byte[] downloadData(String resource, Class loader, URLResourceInfo info, IProgressListener pl, boolean cache) {
        InputStream is = null;
        DataInputStream dis = null;
        if (info == null) info = new URLResourceInfo();
        try {
            is = openStream(resource, loader, info, cache);
            dis = new DataInputStream(is);
            if (dis == null) {
                return null;
            }
            if (pl != null) {
                pl.reset();
                pl.setName(resource);
                pl.setAction(IProgressListener.ACTION_FILE_DOWNLOAD);
                if (info.headerContentLength != -1) pl.setMaximumProgress(info.headerContentLength);
                pl.setUnit("KB");
            }
            int length = 1024;
            int totalAvailable = dis.available();
            if (totalAvailable > pl.getMaximumProgress()) pl.setMaximumProgress(totalAvailable);
            byte[] chunk = new byte[length];
            ByteArrayOutputStream bos = new ByteArrayOutputStream();
            int bytesRead = 0;
            int nbRead = 0;
            while ((bytesRead = dis.read(chunk, 0, length)) != -1) {
                nbRead += bytesRead;
                totalAvailable = bytesRead + dis.available();
                if (totalAvailable > pl.getMaximumProgress()) pl.setMaximumProgress(totalAvailable);
                bos.write(chunk, 0, bytesRead);
                if (pl != null) {
                    if (info.headerContentLength != -1) pl.setProgress(nbRead); else pl.setProgress(nbRead);
                }
                try {
                    Thread.sleep(0);
                    Thread.yield();
                } catch (InterruptedException ie) {
                    ie.printStackTrace();
                    return null;
                }
            }
            bos.flush();
            if (pl != null) {
                pl.setError(false);
                pl.setFinished(true);
            }
            return bos.toByteArray();
        } catch (IOException ioe) {
            ioe.printStackTrace();
            if (pl != null) {
                pl.setError(true);
                pl.setFinished(true);
            }
            return null;
        } finally {
            if (dis != null) closeStream(dis);
            if (is != null) closeStream(is);
            if (pl != null) pl.setFinished(true);
        }
    }

    /**
	 * Loads a resource, when resource is found in pool it will be removed from the pool
	 *
	 * @param URI The resource URI
	 *
	 * @return byte[] The byte array for the resource
	 */
    public static synchronized byte[] loadResource(String uri) {
        return loadResource(uri, false, null, true);
    }

    /**
	 * Loads a resource from the resource pool or URL
	 *
	 * @param URI The resource URI
	 *
	 * @return byte[] The byte array for the resource
	 */
    public static synchronized byte[] loadResource(String uri, boolean cacheFile, IProgressListener pl, boolean removeFromPool) {
        return IOManager.downloadData(uri, pl, cacheFile);
    }

    /**
	 * Loads an image from the resourcepool, classpath/jar or url
	 * Tries different strategies to load
	 *
	 * When image is found in resource pool it will be removed from the pool
	 *
	 * @param uri The image URI
	 *
	 * @return Image An Image instance for the requested image
	 */
    public static synchronized Image loadImage(String uri) {
        return loadImage(uri, null, null, true);
    }

    /**
	 * Loads an image from the resourcepool, classpath/jar or url
	 * Tries different strategies to load
	 *
	 * When image is found in resource pool it will be removed from the pool
	 *
	 * @param uri The image URI
	 * @param c The component requesting the image to load
	 *
	 * @return Image An Image instance for the requested image
	 */
    public static synchronized Image loadImage(String uri, IProgressListener pl, Component c) {
        return loadImage(uri, pl, c, true);
    }

    /**
	 * Loads an image from the resourcepool, classpath/jar or url
	 * Tries different strategies to load
	 *
	 * @param uri The image URI
	 * @param c The component requesting the image
	 * @param removeFromPool Whether the Image should be removed from the ResourcePool when loaded
	 *
	 * @return Image An Image instance for the requested image
	 */
    public static synchronized Image loadImage(String uri, IProgressListener pl, Component c, boolean removeFromPool) {
        Image img = null;
        Toolkit t = Toolkit.getDefaultToolkit();
        try {
            URL u = new URL(uri);
            img = t.getImage(u);
            t.prepareImage(img, -1, -1, null);
            int flag = 0;
            do {
                try {
                    Thread.sleep(100);
                } catch (InterruptedException ie) {
                    return null;
                }
                flag = t.checkImage(img, -1, -1, null);
            } while ((flag & (ImageObserver.ALLBITS | ImageObserver.ABORT | ImageObserver.ERROR)) == 0);
        } catch (Exception e3) {
        }
        return img;
    }

    public boolean imageUpdate(Image img, int infoflags, int x, int y, int width, int height) {
        switch(infoflags) {
            case ImageObserver.WIDTH | ImageObserver.HEIGHT:
                return true;
            case ImageObserver.SOMEBITS:
            case ImageObserver.PROPERTIES:
                return true;
            case ImageObserver.FRAMEBITS:
                return false;
            case ImageObserver.ALLBITS:
                return false;
            case ImageObserver.ERROR:
                return false;
            case ImageObserver.ABORT:
                return false;
        }
        return false;
    }
}
