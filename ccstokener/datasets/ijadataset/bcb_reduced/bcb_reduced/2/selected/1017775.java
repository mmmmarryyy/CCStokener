package com.lowagie.text;

import java.io.File;
import java.io.InputStream;
import java.io.IOException;
import java.net.MalformedURLException;
import java.net.URL;

/**
 * An <CODE>Png</CODE> is the representation of a graphic element (PNG)
 * that has to be inserted into the document
 *
 * @see		Element
 * @see		Image
 * @see		Gif
 * @see		Jpeg
 * 
 * @author  bruno@lowagie.com
 * @version 0.37 2000/10/05
 * @since   iText0.36
 */
public class Png extends Image implements Element {

    /** Some PNG specific values. */
    public static final int[] PNGID = { 137, 80, 78, 71, 13, 10, 26, 10 };

    /** A PNG marker. */
    public static final String IHDR = "IHDR";

    /** A PNG marker. */
    public static final String PLTE = "PLTE";

    /** A PNG marker. */
    public static final String IDAT = "IDAT";

    /** A PNG marker. */
    public static final String IEND = "IEND";

    /**
	 * Constructs a <CODE>Png</CODE>-object, using a <VAR>filename</VAR>.
	 *
	 * @param		filename	a <CODE>String</CODE>-representation of the file that contains the Image.
	 *
	 * @since		iText0.36
	 */
    public Png(String filename, int width, int height) throws MalformedURLException, BadElementException, IOException {
        this(Image.toURL(filename), width, height);
    }

    /**
	 * Constructs a <CODE>Png</CODE>-object, using an <VAR>url</VAR>.
	 *
	 * @param		url			the <CODE>URL</CODE> where the image can be found.
	 *
	 * @since		iText0.36
	 */
    public Png(URL url, int width, int height) throws BadElementException, IOException {
        this(url);
        scaledWidth = width;
        scaledHeight = height;
    }

    /**
	 * Constructs a <CODE>Png</CODE>-object, using a <VAR>filename</VAR>.
	 *
	 * @param		filename	a <CODE>String</CODE>-representation of the file that contains the Image.
	 *
	 * @since		iText0.36
	 */
    public Png(String filename) throws MalformedURLException, BadElementException, IOException {
        this(Image.toURL(filename));
    }

    /**
	 * Constructs a <CODE>Png</CODE>-object, using an <VAR>url</VAR>.
	 *
	 * @param		url			the <CODE>URL</CODE> where the image can be found.
	 *
	 * @since		iText0.36
	 */
    public Png(URL url) throws BadElementException, IOException {
        super(url);
        type = PNG;
        InputStream is = null;
        try {
            is = url.openStream();
            for (int i = 0; i < PNGID.length; i++) {
                if (PNGID[i] != is.read()) {
                    throw new BadElementException(url.toString() + " is not a valid PNG-file.");
                }
            }
            while (true) {
                int len = getInt(is);
                if (IHDR.equals(getString(is))) {
                    scaledWidth = getInt(is);
                    setRight((int) scaledWidth);
                    scaledHeight = getInt(is);
                    setTop((int) scaledHeight);
                    break;
                }
                if (IEND.equals(getString(is))) {
                    break;
                }
                skip(is, len + 4);
            }
        } finally {
            if (is != null) {
                is.close();
            }
            plainWidth = width();
            plainHeight = height();
        }
    }

    /**
	 * Gets an <CODE>int</CODE> from an <CODE>InputStream</CODE>.
	 *
	 * @param		an <CODE>InputStream</CODE>
	 * @return		the value of an <CODE>int</CODE>
	 */
    public static final int getInt(InputStream is) throws IOException {
        return (is.read() << 24) + (is.read() << 16) + (is.read() << 8) + is.read();
    }

    /**
	 * Gets a <CODE>String</CODE> from an <CODE>InputStream</CODE>.
	 *
	 * @param		an <CODE>InputStream</CODE>
	 * @return		the value of an <CODE>int</CODE>
	 */
    public static final String getString(InputStream is) throws IOException {
        StringBuffer buf = new StringBuffer();
        for (int i = 0; i < 4; i++) {
            buf.append((char) is.read());
        }
        return buf.toString();
    }

    /**
	 * Returns a representation of this <CODE>Rectangle</CODE>.
	 *
	 * @return		a <CODE>String</CODE>
	 *
	 * @since		iText0.36
	 */
    public String toString() {
        StringBuffer buf = new StringBuffer("<PNG>");
        buf.append(super.toString());
        buf.append("</PNG>");
        return buf.toString();
    }
}
