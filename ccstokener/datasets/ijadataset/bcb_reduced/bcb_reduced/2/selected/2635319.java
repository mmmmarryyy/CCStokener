package com.bluebrim.resource.shared;

import java.awt.*;
import java.io.*;
import java.net.*;
import java.nio.charset.*;
import javax.swing.*;

/**
 * Utility class that reads an image and creates an instance of <code>Icon</code> for this image.
 * The name of the image file is always specified relative to the path of a class that serves
 * as a resource anchor.
 * <br>
 * I.e. if <code>CoObjectIF</code> is in package com.bluebrim.base then the code
 * <code><pre>
 *	CoImageIconLoader.loadIcon(CoObjectIF.class, "test.gif"); 
 * </pre></code>
 * assumes that the path to image file, relative to some entry in the classpath,
 * is com/bluebrim/base/test.gif.
 *
 * @author Lasse Svad�ngs
 * @author Markus Persson 2000-02-08
 */
public class CoResourceLoader {

    private static CoResourceLoader loader = new CoResourceLoader();

    private static Icon NULL_ICON = new NullIcon();

    private static class NullIcon implements Icon {

        private static final int DIMENSION = 16;

        public int getIconWidth() {
            return DIMENSION;
        }

        public int getIconHeight() {
            return DIMENSION;
        }

        public void paintIcon(Component c, Graphics g, int x, int y) {
            g.setColor(Color.WHITE);
            g.fillRect(x, y, x + DIMENSION, y + DIMENSION);
            g.setColor(Color.RED);
            g.drawRect(x + 1, y + 1, x + DIMENSION - 3, y + DIMENSION - 3);
            g.drawLine(x + 1, y + 1, x + DIMENSION - 2, y + DIMENSION - 2);
            g.drawLine(x + 1, y + DIMENSION - 2, x + DIMENSION - 2, y + 1);
        }
    }

    public static Icon loadIcon(Class domainAnchor, String filename) {
        return loadIcon(domainAnchor, filename, false);
    }

    public static Icon loadIcon(Class domainAnchor, String filename, boolean useFallback) {
        String daName = domainAnchor.getName();
        int endDot = daName.indexOf('.', 13);
        if (daName.startsWith("com.bluebrim.") && (endDot > 13)) {
            int slash = filename.lastIndexOf('/');
            return loadIcon(daName.substring(13, endDot), filename.substring(slash + 1), useFallback);
        } else {
            return NULL_ICON;
        }
    }

    public static Icon getNullIcon() {
        return NULL_ICON;
    }

    public static Icon loadIcon(String domain, String fileName) {
        return loadIcon(domain, fileName, true);
    }

    public static Icon loadIcon(String domain, String fileName, boolean useFallback) {
        Icon icon = null;
        int domainEnd = domain.indexOf('.', 13);
        if (domain.startsWith("com.bluebrim.") && (domainEnd > 13)) domain = domain.substring(13, domainEnd);
        String iconPath = domain + "/" + fileName;
        URL url = loader.getClass().getClassLoader().getResource(iconPath);
        if (url != null) {
            icon = new ImageIcon(url, fileName);
        }
        if (useFallback && (icon == null)) {
            System.err.println("The icon " + iconPath + " is missing.");
            return NULL_ICON;
        } else {
            return icon;
        }
    }

    public static URL getURL(Class domainAnchor, String fileName) {
        String daName = domainAnchor.getName();
        int domainEnd = daName.indexOf('.', 13);
        if (daName.startsWith("com.bluebrim.") && (domainEnd > 13)) {
            String domain = daName.substring(13, domainEnd);
            return loader.getClass().getClassLoader().getResource(domain + "/" + fileName);
        }
        return domainAnchor.getResource(fileName);
    }

    public static InputStream getStream(Class domainAnchor, String fileName) {
        URL url = getURL(domainAnchor, fileName);
        try {
            return url != null ? url.openStream() : null;
        } catch (IOException e) {
            return null;
        }
    }

    public static BufferedReader getReader(Class domainAnchor, String fileName) {
        return new BufferedReader(new InputStreamReader(getStream(domainAnchor, fileName)));
    }

    public static BufferedReader getReader(Class domainAnchor, String fileName, String encoding) throws UnsupportedEncodingException {
        return new BufferedReader(new InputStreamReader(getStream(domainAnchor, fileName), encoding));
    }

    public static BufferedReader getReader(Class domainAnchor, String fileName, Charset charset) {
        return new BufferedReader(new InputStreamReader(getStream(domainAnchor, fileName), charset));
    }

    public static BufferedReader getReader(Class domainAnchor, String fileName, CharsetDecoder csd) {
        return new BufferedReader(new InputStreamReader(getStream(domainAnchor, fileName), csd));
    }
}
