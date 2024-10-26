package org.onemind.swingweb;

import java.awt.*;
import java.awt.image.*;
import java.io.*;
import java.net.MalformedURLException;
import java.net.URL;
import java.util.Map;
import java.util.logging.Logger;
import javax.imageio.ImageIO;
import javax.swing.RepaintManager;
import org.onemind.awtbridge.BridgeContext;
import org.onemind.awtbridge.BridgeToolkit;
import org.onemind.commons.java.lang.ConfigurationException;
import org.onemind.swingweb.awt.SwingWebRepaintManager;
import org.onemind.swingweb.image.DummyImageProducer;
import org.onemind.swingweb.image.EmptyImage;
import org.onemind.swingweb.resource.*;
import org.onemind.swingweb.session.SwingWebSession;

/**
 * SwingWeb context
 * @author TiongHiang Lee (thlee@onemindsoft.org)
 * 
 */
public class SwingWebContext extends BridgeContext {

    /** the logger * */
    private static final Logger _logger = Logger.getLogger(SwingWebContext.class.getName());

    private static final SwingWebRepaintManager DEFAULT_REPAINT_MANGER = new SwingWebRepaintManager();

    /** the environments * */
    private Map _environment;

    /** the url prefix **/
    private String _urlPrefix = "";

    /** the resource cache **/
    private ResourceCache _resCache = new ResourceCache();

    private boolean _useSessionCache = false;

    /**
     * Constructor a thinkient tcontext with given environment
     * @param tk the toolkit
     * @throws ConfigurationException if there's render config problem
     */
    public SwingWebContext() {
        super((BridgeToolkit) Toolkit.getDefaultToolkit());
        RepaintManager.setCurrentManager(DEFAULT_REPAINT_MANGER);
    }

    /**
     * Set the environment
     * @param prop the environment 
     */
    public final void setEnvironment(Map prop) {
        _environment = prop;
    }

    /**
     * Get the environments
     * @return the environments
     */
    public final Map getEnvironment() {
        return _environment;
    }

    /** 
     * {@inheritDoc}
     */
    public int checkImage(Image image, int width, int height, ImageObserver observer) {
        return ImageObserver.ALLBITS;
    }

    /** 
     * {@inheritDoc}
     */
    public Image createImage(byte[] imagedata, int imageoffset, int imagelength) {
        try {
            return ImageIO.read(new ByteArrayInputStream(imagedata, imageoffset, imagelength));
        } catch (Exception e) {
            _logger.throwing(getClass().getName(), "createImage", e);
            return new EmptyImage(getGraphics());
        }
    }

    /**
     * Register a resource 
     * @param image the image
     * @return the URI for the resource
     */
    public String registerImageAsResource(Image image) {
        String uri = "#";
        if (image instanceof BufferedImage) {
            uri = registerResource(new BufferedImageResource(image.toString(), "image", (BufferedImage) image));
        }
        return uri;
    }

    private ResourceCache getResourceCache() {
        if (_useSessionCache) {
            return ((SwingWebSession) getSession()).getResourceCache();
        } else {
            return _resCache;
        }
    }

    /**
     * Register the url
     * @param image the image
     * @return the uri
     */
    public String registerResource(Resource res) {
        String id = _resCache.cacheResource(res);
        return _urlPrefix + "?swresource=" + id;
    }

    /**
     * Get the URL object identified by uri 
     * @param uri the uri
     * @return the URL object
     */
    public Resource getResource(String uri) {
        return (Resource) _resCache.getResource(uri);
    }

    /** 
     * {@inheritDoc}
     */
    public Image createImage(ImageProducer producer) {
        if (producer instanceof DummyImageProducer) {
            return ((DummyImageProducer) producer).getImage();
        } else {
            _logger.warning("Cannot create image with producer " + producer + ". Use empty image.");
            return new EmptyImage(getGraphics());
        }
    }

    /** 
     * {@inheritDoc}
     */
    public Image createImage(String filename) {
        File f = new File(filename);
        if (f.exists()) {
            try {
                return createImage(f.toURL());
            } catch (MalformedURLException e) {
                _logger.throwing(getClass().getName(), "createImage", e);
            }
        }
        _logger.warning("Cannot create image with filename " + filename + ". Use empty image.");
        return new EmptyImage(getGraphics());
    }

    /** 
     * {@inheritDoc}
     */
    public Image createImage(URL url) {
        try {
            String resId = "image:" + url.toString();
            Resource res = getResourceCache().getResourceByKey(resId);
            if (res != null) {
                return (Image) res.getResourceObject();
            } else {
                BufferedImage buffImg = ImageIO.read(url.openStream());
                if (buffImg != null) {
                    registerResource(new BufferedImageResource(resId, "image", buffImg));
                }
                return buffImg;
            }
        } catch (IOException e) {
            e.printStackTrace();
            return new EmptyImage(getGraphics());
        }
    }

    /** 
     * {@inheritDoc}
     */
    public Image getImage(String filename) {
        return createImage(filename);
    }

    /** 
     * {@inheritDoc}
     */
    public Image getImage(URL url) {
        return createImage(url);
    }

    /** 
     * {@inheritDoc}
     */
    public int getScreenResolution() throws HeadlessException {
        return 0;
    }

    /** 
     * {@inheritDoc}
     */
    public Dimension getScreenSize() throws HeadlessException {
        return new Dimension(1024, 768);
    }

    /** 
     * {@inheritDoc}
     */
    public boolean prepareImage(Image image, int width, int height, ImageObserver observer) {
        return true;
    }
}
