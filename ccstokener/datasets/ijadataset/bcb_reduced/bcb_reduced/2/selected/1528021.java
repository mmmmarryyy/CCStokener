package genj.util;

import java.awt.*;
import java.net.*;
import java.io.*;
import java.util.*;

/**
 * Class that represents an improved image that doesn't depend
 * on Swing
 */
public class ImgIcon {

    private Object swingicon = null;

    private Image image = null;

    private Dimension size;

    private static Hashtable zoomed = new Hashtable();

    private static final Component component = new TextField();

    private static final MediaTracker tracker = new MediaTracker(component);

    private static final int CLUSTER = 1024 * 4;

    /**
   * Constructor #4
   */
    public ImgIcon(Image image) {
        this.image = image;
        size = calcDimension(image);
    }

    /**
   * Constructor #3
   */
    public ImgIcon(Object from, String name) {
        InputStream in = from.getClass().getResourceAsStream(name);
        if (in == null) throw new RuntimeException("Couldn't read image from resources " + from.getClass().getName() + "/" + name);
        createImageFrom(in);
    }

    /**
   * Constructor #2
   */
    public ImgIcon(InputStream in) throws IOException {
        createImageFrom(in);
    }

    /**
   * Constructor #1
   */
    public ImgIcon(URL url) throws IOException {
        createImageFrom(url.openStream());
    }

    /**
   * Calculates dimension of image (eventually waiting)
   */
    private static Dimension calcDimension(Image image) {
        synchronized (tracker) {
            tracker.addImage(image, 0);
            try {
                tracker.waitForID(0, 5000);
            } catch (InterruptedException ex) {
                Debug.log(Debug.WARNING, ImgIcon.class, "Interrupted while loading image");
                return new Dimension(0, 0);
            } finally {
                tracker.removeImage(image, 0);
            }
        }
        return new Dimension(image.getWidth(null), image.getHeight(null));
    }

    /**
   * Helper which actually loads the image
   */
    private void createImageFrom(InputStream in) {
        byte[] imgData;
        try {
            imgData = new ByteArray(in).getBytes();
        } catch (IOException ex) {
            return;
        }
        image = Toolkit.getDefaultToolkit().createImage(imgData, 0, imgData.length);
        size = calcDimension(image);
        imgData = null;
    }

    /**
   * Returns image's height
   */
    public int getIconHeight() {
        return size.height;
    }

    /**
   * Returns image's width
   */
    public int getIconWidth() {
        return size.width;
    }

    /**
   * Returns this image
   */
    public Image getImage() {
        return image;
    }

    /**
   * Paints the image
   */
    public void paintIcon(Component c, Graphics g, int x, int y) {
        g.drawImage(image, x, y, c);
    }

    /**
   * Paints the image
   */
    public void paintIcon(Component c, Graphics g, int x, int y, double zoom) {
        if (zoom == 1.0) {
            paintIcon(c, g, x, y);
            return;
        }
        int w = (int) (size.width * zoom), h = (int) (size.height * zoom);
        String key = image + "/" + w + "x" + h;
        Object o = zoomed.get(key);
        Image i;
        if (o != null) {
            i = (Image) o;
        } else {
            i = image.getScaledInstance(w, h, Image.SCALE_DEFAULT);
            calcDimension(i);
            zoomed.put(key, i);
        }
        g.drawImage(i, x, y, null);
    }

    /**
   * Paints the image
   */
    public void paintIcon(Graphics g, int x, int y) {
        g.drawImage(image, x, y, null);
    }

    /**
   * Paints the image
   */
    public void paintIcon(Graphics g, int x, int y, double zoom) {
        paintIcon(null, g, x, y, zoom);
    }

    /**
   * Untyped setter for cached SwingIcon
   */
    public void setSwingIcon(Object o) {
        swingicon = o;
    }

    /** 
   * Untyped getter for cached SwingIcon
   */
    public Object getSwingIcon() {
        return swingicon;
    }
}
