package com.lowagie.text;

import java.io.*;
import java.net.*;
import com.lowagie.text.pdf.*;
import com.lowagie.text.pdf.codec.postscript.*;
import java.util.StringTokenizer;

/**
 * An <CODE>ImgPostscript</CODE> is the representation of an EPS
 * that has to be inserted into the document
 *
 * @see		Element
 * @see		Image
 */
public class ImgPostscript extends Image implements Element {

    ImgPostscript(Image image) {
        super(image);
    }

    /**
     * Constructs an <CODE>ImgPostscript</CODE>-object, using an <VAR>url</VAR>.
     *
     * @param url the <CODE>URL</CODE> where the image can be found
     * @throws BadElementException on error
     * @throws IOException on error
     */
    public ImgPostscript(URL url) throws BadElementException, IOException {
        super(url);
        processParameters();
    }

    /**
     * Constructs an <CODE>ImgPostscript</CODE>-object, using a <VAR>filename</VAR>.
     *
     * @param filename a <CODE>String</CODE>-representation of the file that contains the image.
     * @throws BadElementException on error
     * @throws MalformedURLException on error
     * @throws IOException on error
     */
    public ImgPostscript(String filename) throws BadElementException, MalformedURLException, IOException {
        this(Image.toURL(filename));
    }

    /**
     * Constructs an <CODE>ImgPostscript</CODE>-object from memory.
     *
     * @param img the memory image
     * @throws BadElementException on error
     * @throws IOException on error
     */
    public ImgPostscript(byte[] img) throws BadElementException, IOException {
        super((URL) null);
        rawData = img;
        originalData = img;
        processParameters();
    }

    /**
     * This method checks if the image is a valid Postscript and processes some parameters.
     * @throws BadElementException
     * @throws IOException
     */
    private void processParameters() throws BadElementException, IOException {
        type = IMGTEMPLATE;
        originalType = ORIGINAL_PS;
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
            String boundingbox = null;
            Reader r = new BufferedReader(new InputStreamReader(is));
            while (r.ready()) {
                char c;
                StringBuffer sb = new StringBuffer();
                while ((c = ((char) r.read())) != '\n') {
                    sb.append(c);
                }
                if (sb.toString().startsWith("%%BoundingBox:")) {
                    boundingbox = sb.toString();
                }
                if (sb.toString().startsWith("%%TemplateBox:")) {
                    boundingbox = sb.toString();
                }
                if (sb.toString().startsWith("%%EndComments")) {
                    break;
                }
            }
            if (boundingbox == null) return;
            StringTokenizer st = new StringTokenizer(boundingbox, ": \r\n");
            st.nextElement();
            String xx1 = st.nextToken();
            String yy1 = st.nextToken();
            String xx2 = st.nextToken();
            String yy2 = st.nextToken();
            int left = Integer.parseInt(xx1);
            int top = Integer.parseInt(yy1);
            int right = Integer.parseInt(xx2);
            int bottom = Integer.parseInt(yy2);
            int inch = 1;
            dpiX = 72;
            dpiY = 72;
            scaledHeight = (float) (bottom - top) / inch * 1f;
            scaledHeight = 800;
            setTop(scaledHeight);
            scaledWidth = (float) (right - left) / inch * 1f;
            scaledWidth = 800;
            setRight(scaledWidth);
        } finally {
            if (is != null) {
                is.close();
            }
            plainWidth = width();
            plainHeight = height();
        }
    }

    /** Reads the Postscript into a template.
     * @param template the template to read to
     * @throws IOException on error
     * @throws DocumentException on error
     */
    public void readPostscript(PdfTemplate template) throws IOException, DocumentException {
        setTemplateData(template);
        template.setWidth(width());
        template.setHeight(height());
        InputStream is = null;
        try {
            if (rawData == null) {
                is = url.openStream();
            } else {
                is = new java.io.ByteArrayInputStream(rawData);
            }
            MetaDoPS meta = new MetaDoPS(is, template);
            meta.readAll();
        } finally {
            if (is != null) {
                is.close();
            }
        }
    }
}
