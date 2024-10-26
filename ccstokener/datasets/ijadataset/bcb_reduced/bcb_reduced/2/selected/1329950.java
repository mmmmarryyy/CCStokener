package org.onemind.swingweb.image;

import java.awt.Graphics;
import java.awt.image.BufferedImage;
import java.io.IOException;
import java.net.URL;
import java.util.logging.Logger;
import javax.imageio.ImageIO;

/**
 * An image from a URL
 * @author TiongHiang Lee (thlee@onemindsoft.org)
 * 
 */
public class URLImage extends AbstractImage {

    /** the logger **/
    private static final Logger _logger = Logger.getLogger(URLImage.class.getName());

    /** the url **/
    private URL _url;

    /**
     * Constructor
     * @param g the graphics
     * @param url the url
     */
    public URLImage(Graphics g, URL url) {
        super(g);
        _url = url;
        BufferedImage bufferedImage;
        try {
            bufferedImage = ImageIO.read(_url.openStream());
            setWidth(bufferedImage.getWidth());
            setHeight(bufferedImage.getHeight());
        } catch (IOException e) {
            _logger.throwing(getClass().getName(), "constructor", e);
        }
    }

    /**
     * Return the url
     * @return the url
     */
    public URL getURL() {
        return _url;
    }
}
