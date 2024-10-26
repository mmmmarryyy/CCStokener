package net.sf.javarisk.three.resources;

import java.awt.image.BufferedImage;
import java.io.IOException;
import java.io.Serializable;
import java.net.URL;
import java.util.MissingResourceException;
import java.util.logging.Level;
import java.util.logging.Logger;
import javax.annotation.Nonnull;
import javax.imageio.ImageIO;
import javax.imageio.ImageReader;
import net.sf.javarisk.three.Constants;
import net.sf.javarisk.tools.GeneralTools;

/**
 * <p>
 * The ResourceLoader can be used to load resources like images, sounds etc.<br/>
 * It assumes that the resources are located in sub-folders relative to its own location, e.g. if an image would be
 * stored at
 * <tt>net/sf/javarisk/three/resources/images/myImage.png</tt>, it could be loaded by calling
 * <tt>loadImage("myImage.png");</tt>.
 * </p>
 * 
 * @author <a href='mailto:sebastiankirsch@users.sourceforge.net'>Sebastian Kirsch</a>
 * @version 0.1; $Rev: 243 $
 * @since 0.1
 * @see #loadImage(String)
 */
public class ResourceLoader implements Serializable {

    private static final long serialVersionUID = Constants.SERIAL_VERSION;

    private static final Logger LOG = Logger.getLogger(ResourceLoader.class.getName());

    /**
     * Creates a new instance of <code>ResourceLoader</code>.
     * 
     * @since 0.1
     */
    public ResourceLoader() {
        super();
    }

    /**
     * <p>
     * Loads the specified image file.<br/>
     * The loader assumes that the file is located in the <tt>images</tt> folder.</p>
     * 
     * @param fileName
     *            the name of the image file
     * @return a <code>BufferedImage</code> providing the image data
     * @throws IllegalArgumentException
     *             if <code>fileName</code> is <code>null</code> or the <i>empty <code>String</code></i>
     * @throws MissingResourceException
     *             if the file is not available or accessible
     * @throws IOException
     *             if the image cannot be loaded
     * @since 0.1
     */
    @Nonnull
    public BufferedImage loadImage(String fileName) throws IllegalArgumentException, MissingResourceException, IOException {
        GeneralTools.notEmpty("fileName", fileName);
        URL url = getClass().getResource("images/" + fileName);
        if (url == null) {
            String msg = "Cannot locate image [" + fileName + "]!";
            if (LOG.isLoggable(Level.INFO)) {
                LOG.log(Level.INFO, msg);
            }
            throw new MissingResourceException(msg, getClass().getName(), fileName);
        }
        ImageReader reader = ImageIO.getImageReadersBySuffix("png").next();
        BufferedImage image;
        try {
            reader.setInput(ImageIO.createImageInputStream(url.openStream()), true, true);
            image = reader.read(0);
        } catch (IOException ioE) {
            if (LOG.isLoggable(Level.INFO)) {
                LOG.log(Level.INFO, "Failed to load image [" + fileName + "]!");
            }
            throw ioE;
        } finally {
            reader.dispose();
        }
        if (LOG.isLoggable(Level.FINER)) LOG.log(Level.FINER, "Loaded image [" + fileName + "].");
        return image;
    }
}
