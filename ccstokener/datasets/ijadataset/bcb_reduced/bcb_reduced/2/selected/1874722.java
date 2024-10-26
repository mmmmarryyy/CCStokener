package com.lowagie.text;

import java.io.IOException;
import java.io.InputStream;
import java.net.MalformedURLException;
import java.net.URL;
import com.lowagie.text.pdf.PdfTemplate;
import com.lowagie.text.pdf.codec.wmf.InputMeta;
import com.lowagie.text.pdf.codec.wmf.MetaDo;

/**
 * An <CODE>ImgWMF</CODE> is the representation of a windows metafile
 * that has to be inserted into the document
 *
 * @see		Element
 * @see		Image
 */
public class ImgWMF extends Image {

    ImgWMF(Image image) {
        super(image);
    }

    /**
     * Constructs an <CODE>ImgWMF</CODE>-object, using an <VAR>url</VAR>.
     *
     * @param url the <CODE>URL</CODE> where the image can be found
     * @throws BadElementException on error
     * @throws IOException on error
     */
    public ImgWMF(URL url) throws BadElementException, IOException {
        super(url);
        processParameters();
    }

    /**
     * Constructs an <CODE>ImgWMF</CODE>-object, using a <VAR>filename</VAR>.
     *
     * @param filename a <CODE>String</CODE>-representation of the file that contains the image.
     * @throws BadElementException on error
     * @throws MalformedURLException on error
     * @throws IOException on error
     */
    public ImgWMF(String filename) throws BadElementException, MalformedURLException, IOException {
        this(Utilities.toURL(filename));
    }

    /**
     * Constructs an <CODE>ImgWMF</CODE>-object from memory.
     *
     * @param img the memory image
     * @throws BadElementException on error
     * @throws IOException on error
     */
    public ImgWMF(byte[] img) throws BadElementException, IOException {
        super((URL) null);
        rawData = img;
        originalData = img;
        processParameters();
    }

    /**
 * This method checks if the image is a valid WMF and processes some parameters.
 * @throws BadElementException
 * @throws IOException
 */
    private void processParameters() throws BadElementException, IOException {
        type = IMGTEMPLATE;
        originalType = ORIGINAL_WMF;
        InputStream is = null;
        try {
            String errorID;
            if (rawData == null) {
                is = url.openStream();
                errorID = url.toString();
            } else {
                is = new java.io.ByteArrayInputStream(rawData);
                errorID = "Byte array";
            }
            InputMeta in = new InputMeta(is);
            if (in.readInt() != 0x9AC6CDD7) {
                throw new BadElementException(errorID + " is not a valid placeable windows metafile.");
            }
            in.readWord();
            int left = in.readShort();
            int top = in.readShort();
            int right = in.readShort();
            int bottom = in.readShort();
            int inch = in.readWord();
            dpiX = 72;
            dpiY = 72;
            scaledHeight = (float) (bottom - top) / inch * 72f;
            setTop(scaledHeight);
            scaledWidth = (float) (right - left) / inch * 72f;
            setRight(scaledWidth);
        } finally {
            if (is != null) {
                is.close();
            }
            plainWidth = getWidth();
            plainHeight = getHeight();
        }
    }

    /** Reads the WMF into a template.
     * @param template the template to read to
     * @throws IOException on error
     * @throws DocumentException on error
     */
    public void readWMF(PdfTemplate template) throws IOException, DocumentException {
        setTemplateData(template);
        template.setWidth(getWidth());
        template.setHeight(getHeight());
        InputStream is = null;
        try {
            if (rawData == null) {
                is = url.openStream();
            } else {
                is = new java.io.ByteArrayInputStream(rawData);
            }
            MetaDo meta = new MetaDo(is, template);
            meta.readAll();
        } finally {
            if (is != null) {
                is.close();
            }
        }
    }
}
