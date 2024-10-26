package de.ulrich_fuchs.jtypeset.stream;

import java.awt.image.BufferedImage;
import java.awt.image.RenderedImage;
import java.net.URL;
import java.net.URLConnection;
import java.util.Iterator;
import javax.imageio.ImageIO;
import javax.imageio.ImageReader;
import javax.imageio.stream.ImageInputStream;

/**
 *
 * @author  ulrich
 */
public class InlineImageChunk extends Chunk {

    private URL url;

    private double width;

    private double ascent;

    private double descent;

    /** Creates a new instance of TextChunk */
    public InlineImageChunk(URL url, double width, double ascent, double descent) {
        super();
        this.url = url;
        this.width = width;
        this.ascent = ascent;
        this.descent = descent;
    }

    /** Creates a new instance of InlineImageChunk */
    public InlineImageChunk(URL url) {
        super();
        this.url = url;
        try {
            URLConnection urlConn = url.openConnection();
            urlConn.setReadTimeout(15000);
            ImageInputStream iis = ImageIO.createImageInputStream(urlConn.getInputStream());
            Iterator<ImageReader> readers = ImageIO.getImageReaders(iis);
            if (readers.hasNext()) {
                ImageReader reader = readers.next();
                reader.setInput(iis, true);
                this.width = reader.getWidth(0);
                this.ascent = reader.getHeight(0);
                this.descent = 0;
                reader.dispose();
            } else System.err.println("cannot read width and height of image " + url + " - no suitable reader!");
        } catch (Exception exc) {
            System.err.println("cannot read width and height of image " + url + " due to exception:");
            System.err.println(exc);
            exc.printStackTrace(System.err);
        }
    }

    public URL getUrl() {
        return url;
    }

    public void setUrl(URL url) {
        this.url = url;
    }

    public double getWidth() {
        return width;
    }

    public void setWidth(double width) {
        this.width = width;
    }

    public double getAscent() {
        return ascent;
    }

    public void setAscent(double ascent) {
        this.ascent = ascent;
    }

    public double getDescent() {
        return descent;
    }

    public void setDescent(double descent) {
        this.descent = descent;
    }
}
