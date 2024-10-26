package de.lichtflut.infra.ui;

import java.awt.Image;
import java.io.IOException;
import java.net.URL;
import java.util.HashMap;
import java.util.Map;
import java.util.Properties;
import javax.imageio.ImageIO;
import de.lichtflut.infra.io.ResourceException;
import de.lichtflut.infra.io.SystemResourceLoader;
import de.lichtflut.infra.logging.Log;

/**
 * Provider/Reader for Resources.
 * 
 * Created: 17.07.2008
 *
 * @author Oliver Tigges
 */
public class ResourceProvider {

    private static Map<String, Image> imageMap = new HashMap<String, Image>();

    private static Map<String, Properties> propsMap = new HashMap<String, Properties>();

    /**
	 * Loads the image with given URI.
	 */
    public static Image getImage(final String uri) {
        if (imageMap.containsKey(uri)) {
            return imageMap.get(uri);
        }
        final URL url = SystemResourceLoader.getInstance().findResource(uri);
        Log.info(ResourceProvider.class, "resource '" + uri + "' --> " + url);
        Image image;
        try {
            image = ImageIO.read(url);
        } catch (IOException e) {
            throw new ResourceException("Couldn't load image: " + uri, e);
        }
        imageMap.put(uri, image);
        return image;
    }

    /**
	 * Loads properties with given URI.
	 */
    public static Properties getProperties(final String uri) {
        if (propsMap.containsKey(uri)) {
            return propsMap.get(uri);
        }
        final URL url = SystemResourceLoader.getInstance().findResource(uri);
        Log.info(ResourceProvider.class, "resource '" + uri + "' --> " + url);
        Properties props = new Properties();
        try {
            props.load(url.openStream());
        } catch (IOException e) {
            throw new ResourceException("Couldn't load image: " + uri, e);
        }
        propsMap.put(uri, props);
        return props;
    }
}
