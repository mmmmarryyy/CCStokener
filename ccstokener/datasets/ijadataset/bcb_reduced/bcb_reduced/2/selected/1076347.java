package com.lowagie.text;

import java.io.File;
import java.io.InputStream;
import java.io.IOException;
import java.net.MalformedURLException;
import java.net.URL;

/**
 * An <CODE>Gif</CODE> is the representation of a graphic element (GIF)
 * that has to be inserted into the document
 *
 * @see		Element
 * @see		Image
 * @see		Jpeg
 * 
 * @author  bruno@lowagie.com
 * @version 0.37 2000/10/05
 * @since   iText0.31
 */
public class Gif extends Image implements Element {

    /**
	 * Constructs a <CODE>Gif</CODE>-object, using a <VAR>filename</VAR>.
	 *
	 * @param		filename	a <CODE>String</CODE>-representation of the file that contains the Image.
	 * @param		width		the width you want the image to have
	 * @param		height		the height you want the image to have
	 *
	 * @since		iText0.31
	 */
    public Gif(String filename, int width, int height) throws BadElementException, MalformedURLException, IOException {
        this(Image.toURL(filename), width, height);
    }

    /**
	 * Constructs a <CODE>Jpeg</CODE>-object, using an <VAR>url</VAR>.
	 *
	 * @param		url			the <CODE>URL</CODE> where the image can be found.
	 * @param		width		the width you want the image to have
	 * @param		height		the height you want the image to have
	 *
	 * @since		iText0.31
	 */
    public Gif(URL url, int width, int height) throws BadElementException, IOException {
        this(url);
        scaledWidth = width;
        scaledHeight = height;
    }

    /**
	 * Constructs a <CODE>Gif</CODE>-object, using a <VAR>filename</VAR>.
	 *
	 * @param		filename	a <CODE>String</CODE>-representation of the file that contains the Image.
	 *
	 * @since		iText0.36
	 */
    public Gif(String filename) throws BadElementException, MalformedURLException, IOException {
        this(Image.toURL(filename));
    }

    /**
	 * Constructs a <CODE>Jpeg</CODE>-object, using an <VAR>url</VAR>.
	 *
	 * @param		url			the <CODE>URL</CODE> where the image can be found.
	 *
	 * @since		iText0.36
	 */
    public Gif(URL url) throws BadElementException, IOException {
        super(url);
        type = GIF;
        InputStream is = null;
        try {
            is = url.openStream();
            if (is.read() != 'G' || is.read() != 'I' || is.read() != 'F') {
                throw new BadElementException(url.toString() + " is not a valid GIF-file.");
            }
            skip(is, 3);
            scaledWidth = is.read() + (is.read() << 8);
            setRight((int) scaledWidth);
            scaledHeight = is.read() + (is.read() << 8);
            setTop((int) scaledHeight);
        } finally {
            if (is != null) {
                is.close();
            }
            plainWidth = width();
            plainHeight = height();
        }
    }

    /**
	 * Returns a representation of this <CODE>Rectangle</CODE>.
	 *
	 * @return		a <CODE>String</CODE>
	 *
	 * @since		iText0.31
	 */
    public String toString() {
        StringBuffer buf = new StringBuffer("<GIF>");
        buf.append(super.toString());
        buf.append("</GIF>");
        return buf.toString();
    }
}
