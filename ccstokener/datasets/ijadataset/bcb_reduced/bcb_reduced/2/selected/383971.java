package org.minions.stigma.databases;

import java.awt.image.BufferedImage;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.net.MalformedURLException;
import java.net.URI;
import java.net.URISyntaxException;
import java.net.URL;
import java.net.URLConnection;
import java.util.zip.GZIPInputStream;
import javax.imageio.ImageIO;
import org.minions.stigma.globals.GlobalConfig;
import org.minions.utils.logger.Log;

/**
 * Class which is responsible for returning coherent streams
 * to data, using proper application configuration.
 * @see GlobalConfig
 */
public abstract class Resourcer {

    private Resourcer() {
    }

    /**
     * Computes given resource root URI and current resource
     * path into single URL. It is independent from
     * client/server resources storage differences.
     * @param base
     *            URI to resources root
     * @param path
     *            path to current resource
     * @return URL for resource, based on {@code base} and
     *         {@code path}
     */
    public static URL getXMLResourceUrl(URI base, String path) {
        if (GlobalConfig.globalInstance().isResourceCompression()) {
            path += ".gz";
        }
        path = base.getPath() + '/' + path;
        URI file;
        try {
            file = new URI(base.getScheme(), base.getUserInfo(), base.getHost(), base.getPort(), path, base.getQuery(), base.getFragment());
        } catch (URISyntaxException e) {
            Log.logger.error("URI creation failed: " + e);
            return null;
        }
        URL url;
        if (file.isAbsolute()) try {
            url = file.toURL();
        } catch (MalformedURLException e) {
            Log.logger.error("URI -> URL failed: " + e);
            return null;
        } else {
            try {
                url = new File(file.getPath()).toURI().toURL();
            } catch (MalformedURLException e) {
                Log.logger.error("relative URI -> URL failed: " + e);
                return null;
            }
        }
        return url;
    }

    /**
     * Returns input stream for requested resource. It is
     * independent from client/server resources storage
     * differences.
     * @param url
     *            URL to given resource
     * @return input stream for given resource, or null on
     *         error
     */
    public static InputStream getXMLResourceByUrl(URL url) {
        InputStream in;
        try {
            in = url.openStream();
        } catch (IOException e) {
            Log.logger.error("URL.openStream failed: " + e);
            return null;
        }
        if (GlobalConfig.globalInstance().isResourceCompression()) {
            try {
                return new GZIPInputStream(in);
            } catch (IOException e) {
                Log.logger.error("GZIPInputStream creation failed!", e);
                return null;
            }
        }
        return in;
    }

    /**
     * Loads image, independent from application/applet
     * differences. Call is synchronous. May be used to load
     * images from inside of JAR package file.
     * @param name
     *            name of image
     * @return loaded image
     */
    public static BufferedImage loadImage(String name) {
        try {
            URL url = Resourcer.class.getClassLoader().getResource(name);
            if (url != null) return ImageIO.read(url);
            return ImageIO.read(new File(name));
        } catch (IOException e) {
            return null;
        }
    }

    /**
     * Loads a file as an array of bytes, independent from
     * application/applet differences. Call is synchronous.
     * May be used to load files from inside of JAR package
     * file.
     * @param name
     *            name of file
     * @return loaded file
     */
    public static byte[] loadFileBytes(String name) {
        try {
            URL url = Resourcer.class.getClassLoader().getResource(name);
            if (url != null) {
                URLConnection connection = url.openConnection();
                InputStream inputStream = connection.getInputStream();
                byte[] buffer = new byte[connection.getContentLength()];
                for (int read = 0; read < buffer.length; read += inputStream.read(buffer, read, buffer.length - read)) ;
                return buffer;
            } else {
                FileInputStream inputStream = new FileInputStream(name);
                byte[] buffer = new byte[(int) inputStream.getChannel().size()];
                for (int read = 0; read < buffer.length; read += inputStream.read(buffer, read, buffer.length - read)) ;
                return buffer;
            }
        } catch (IOException e) {
            return null;
        }
    }

    /**
     * Returns input stream for requested resource. It is
     * independent from client/server resources storage
     * differences.
     * @param uri
     *            URI to root of resources
     * @param path
     *            path to requested resource
     * @return input stream for given resource, or null on
     *         error
     */
    public static InputStream getXMLResource(URI uri, String path) {
        URL url = getXMLResourceUrl(uri, path);
        if (url == null) return null;
        return getXMLResourceByUrl(url);
    }
}
