package bee.core;

import java.awt.image.BufferedImage;
import java.io.File;
import java.io.InputStream;
import java.net.URL;
import javax.imageio.ImageIO;

/**
 * This class supports resource loading either from jar-file or local file system.
 * @author boto
 */
public class Resource {

    private static Resource instance;

    private Class loader;

    private String pathPrefix = "";

    /**
     * Avoid instanciation of this singleton.
     */
    private Resource() {
    }

    /**
     * Get the single instance of Resource.
     */
    public static Resource get() {
        if (instance == null) instance = new Resource();
        return instance;
    }

    /**
     * Setup the resource loader before first usage.
     * The resource loader class may be the main application entry class.
     */
    public void setup(Class resourceloader, String prefix) {
        loader = resourceloader;
        pathPrefix = prefix;
    }

    /**
     * Set the path prefix for loading resources.
     */
    public void setPathPrefix(String prefix) {
        pathPrefix = prefix;
    }

    /**
     * Get the path prefix.
     */
    public String getPathPrefix() {
        return pathPrefix;
    }

    /** 
     * Given a resource name return its url.
     */
    public URL getURL(String resname) throws Exception {
        if (loader == null) {
            throw new Exception("Resource: resource loader has not been setup");
        }
        URL url = loader.getResource(pathPrefix + "/" + resname);
        if (url == null) {
            File f = new File(resname);
            if (f.isAbsolute()) {
                url = new URL("file", "", -1, resname);
            }
        }
        if (url == null) {
            throw new Exception("Resource: cannot find resource " + resname + ", path prefix: " + pathPrefix);
        }
        return url;
    }

    /**
     * Load an image given its resource file name.
     */
    public BufferedImage loadImage(String resname) throws Exception {
        BufferedImage img = ImageIO.read(getURL(resname));
        return img;
    }

    public InputStream getFileStream(String resname) throws Exception {
        URL url = getURL(resname);
        return url.openStream();
    }
}
