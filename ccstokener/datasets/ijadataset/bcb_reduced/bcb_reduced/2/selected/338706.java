package org.jfor.jfor.rtflib.rtfdoc;

import org.jfor.jfor.rtflib.rtfdoc.RtfElement;
import org.jfor.jfor.rtflib.rtfdoc.RtfContainer;
import org.jfor.jfor.rtflib.rtfdoc.RtfAttributes;
import org.jfor.jfor.tools.ImageConstants;
import org.jfor.jfor.tools.ImageUtil;
import org.jfor.jfor.tools.jpeg.JpegEncoderFactory;
import org.jfor.jfor.tools.jpeg.JPEGException;
import org.jfor.jfor.tools.jpeg.IJpegEncoder;
import java.io.BufferedInputStream;
import java.io.BufferedOutputStream;
import java.io.InputStreamReader;
import java.io.ByteArrayOutputStream;
import java.io.InputStream;
import java.io.IOException;
import java.io.Writer;
import java.io.File;
import java.net.URL;
import java.net.MalformedURLException;
import java.util.Hashtable;

/**
 * Creates an RTF image from an external graphic file.
 * This class belongs to the <fo:external-graphic> tag processing. <br>
 *
 * Supports relative path like "../test.gif", too (01-08-24) <br>
 *
 * Limitations:
 * <li>    Only the image types PNG, JPEG and EMF are supported
 * <li>    The GIF is supported, too, but will be converted to JPG
 * <li>    Only the attributes SRC (required), WIDTH, HEIGHT, SCALING are supported
 * <li>    The SCALING attribute supports (uniform | non-uniform)
 *
 * Known Bugs:
 * <li>    If the emf image has a desired size, the image will be clipped
 * <li>    The emf, jpg & png image will not be displayed in correct size
 *
 *  @author <a href="mailto:a.putz@skynamics.com">Andreas Putz</a>
 */
public class RtfExternalGraphic extends RtfElement {

    /** Exception thrown when an image file/URL cannot be read */
    public static class ExternalGraphicException extends IOException {

        ExternalGraphicException(String reason) {
            super(reason);
        }
    }

    /**
	 * The url of the image
	 */
    protected URL url = null;

    /**
	 * The height of the image
	 */
    protected int height = -1;

    /**
	 * The desired percent value of the height
	 */
    protected int heightPercent = -1;

    /**
	 * The desired height
	 */
    protected int heightDesired = -1;

    /**
	 * Flag whether the desired height is a percentage
	 */
    protected boolean perCentH = false;

    /**
	 * The width of the image
	 */
    protected int width = -1;

    /**
	 * The desired percent value of the width
	 */
    protected int widthPercent = -1;

    /**
	 * The desired width
	 */
    protected int widthDesired = -1;

    /**
	 * Flag whether the desired width is a percentage
	 */
    protected boolean perCentW = false;

    /**
	 * Flag whether the image size shall be adjusted
	 */
    protected boolean scaleUniform = false;

    /**
	 * Graphic compression rate
	 */
    protected int graphicCompressionRate = 80;

    /**
	 * Default constructor.
	 * Create an RTF element as a child of given container.
	 *
	 * @param container a <code>RtfContainer</code> value
	 * @param writer a <code>Writer</code> value
	 */
    public RtfExternalGraphic(RtfContainer container, Writer writer) throws IOException {
        super(container, writer);
    }

    /**
	 * Default constructor.
	 *
	 * @param container a <code>RtfContainer</code> value
	 * @param writer a <code>Writer</code> value
	 * @param attributes a <code>RtfAttributes</code> value
	 */
    public RtfExternalGraphic(RtfContainer container, Writer writer, RtfAttributes attributes) throws IOException {
        super(container, writer, attributes);
    }

    /** RtfElement override - catches ExternalGraphicException and writes a warning
		 *  message to the document if image cannot be read
		 */
    protected void writeRtfContent() throws IOException {
        try {
            writeRtfContentWithException();
        } catch (ExternalGraphicException ie) {
            writeExceptionInRtf(ie);
        }
    }

    /**
	 * Writes the RTF content to m_writer - this one throws ExternalGraphicExceptions
	 *
	 * @exception IOException On error
	 */
    protected void writeRtfContentWithException() throws IOException {
        if (m_writer == null) {
            return;
        }
        if (url == null) {
            throw new ExternalGraphicException("The attribute 'url' of <fo:external-graphic> is null.");
        }
        String linkToRoot = System.getProperty("jfor_link_to_root");
        if (linkToRoot != null) {
            m_writer.write("{\\field {\\* \\fldinst { INCLUDEPICTURE \"");
            m_writer.write(linkToRoot);
            File urlFile = new File(url.getFile());
            m_writer.write(urlFile.getName());
            m_writer.write("\" \\\\* MERGEFORMAT \\\\d }}}");
            return;
        }
        getRtfFile().getLog().logInfo("Writing image '" + url + "'.");
        byte[] data = null;
        try {
            final BufferedInputStream bin = new BufferedInputStream(url.openStream());
            final ByteArrayOutputStream bout = new ByteArrayOutputStream();
            while (true) {
                final int datum = bin.read();
                if (datum == -1) break;
                bout.write(datum);
            }
            bout.flush();
            data = bout.toByteArray();
        } catch (Exception e) {
            throw new ExternalGraphicException("The attribute 'src' of <fo:external-graphic> has a invalid value: '" + url + "' (" + e + ")");
        }
        if (data == null) {
            return;
        }
        String file = url.getFile();
        int type = determineImageType(data, file.substring(file.lastIndexOf(".") + 1));
        if (type >= ImageConstants.I_TO_CONVERT_BASIS) {
            int to = ImageConstants.CONVERT_TO[type - ImageConstants.I_TO_CONVERT_BASIS];
            if (to == ImageConstants.I_JPG) {
                try {
                    final IJpegEncoder jpgEncoder = new JpegEncoderFactory().getEncoder();
                    data = jpgEncoder.encodeJPEG(graphicCompressionRate, data);
                    type = to;
                } catch (JPEGException e) {
                    throw new IOException("JPEG conversion error, src = '" + url + "' (" + e + ")");
                }
            } else {
                type = ImageConstants.I_NOT_SUPPORTED;
            }
        }
        if (type == ImageConstants.I_NOT_SUPPORTED) {
            throw new ExternalGraphicException("The tag <fo:external-graphic> does not support " + file.substring(file.lastIndexOf(".") + 1) + " - image type.");
        }
        String rtfImageCode = ImageConstants.RTF_TAGS[type];
        writeGroupMark(true);
        writeStarControlWord("shppict");
        writeGroupMark(true);
        writeControlWord("pict");
        StringBuffer buf = new StringBuffer(data.length * 3);
        writeControlWord(rtfImageCode);
        if (type == ImageConstants.I_PNG) {
            width = ImageUtil.getIntFromByteArray(data, 16, 4, true);
            height = ImageUtil.getIntFromByteArray(data, 20, 4, true);
        } else if (type == ImageConstants.I_JPG) {
            int basis = -1;
            byte ff = (byte) 0xff;
            byte c0 = (byte) 0xc0;
            for (int i = 0; i < data.length; i++) {
                byte b = data[i];
                if (b != ff) continue;
                if (i == data.length - 1) continue;
                b = data[i + 1];
                if (b != c0) continue;
                basis = i + 5;
                break;
            }
            if (basis != -1) {
                width = ImageUtil.getIntFromByteArray(data, basis + 2, 2, true);
                height = ImageUtil.getIntFromByteArray(data, basis, 2, true);
            }
        } else if (type == ImageConstants.I_EMF) {
            width = ImageUtil.getIntFromByteArray(data, 151, 4, false);
            height = ImageUtil.getIntFromByteArray(data, 155, 4, false);
        }
        if (width != -1) {
            writeControlWord("picw" + width);
        }
        if (height != -1) {
            writeControlWord("pich" + height);
        }
        if (widthDesired != -1) {
            if (perCentW) {
                writeControlWord("picscalex" + widthDesired);
            } else {
                writeControlWord("picscalex" + widthDesired * 100 / width);
            }
        } else if (scaleUniform && heightDesired != -1) {
            if (perCentH) {
                writeControlWord("picscalex" + heightDesired);
            } else {
                writeControlWord("picscalex" + heightDesired * 100 / height);
            }
        }
        if (heightDesired != -1) {
            if (perCentH) {
                writeControlWord("picscaley" + heightDesired);
            } else {
                writeControlWord("picscaley" + heightDesired * 100 / height);
            }
        } else if (scaleUniform && widthDesired != -1) {
            if (perCentW) {
                writeControlWord("picscaley" + widthDesired);
            } else {
                writeControlWord("picscaley" + widthDesired * 100 / width);
            }
        }
        for (int i = 0; i < data.length; i++) {
            int iData = data[i];
            if (iData < 0) iData += 256;
            if (iData < 16) {
                buf.append('0');
            }
            buf.append(Integer.toHexString(iData));
        }
        int len = buf.length();
        char[] chars = new char[len];
        buf.getChars(0, len, chars, 0);
        m_writer.write(chars);
        writeGroupMark(false);
        writeGroupMark(false);
    }

    /**
	 * Sets the desired height of the image.
	 *
	 * @param theHeight The desired image height
	 */
    public void setHeight(String theHeight) {
        this.heightDesired = ImageUtil.getInt(theHeight);
        this.perCentH = ImageUtil.isPercent(theHeight);
    }

    /**
	 * Sets the desired width of the image.
	 *
	 * @param theWidth The desired image width
	 */
    public void setWidth(String theWidth) {
        this.widthDesired = ImageUtil.getInt(theWidth);
        this.perCentW = ImageUtil.isPercent(theWidth);
    }

    /**
	 * Sets the flag whether the image size shall be adjusted.
	 *
	 * @param value
	 * true    image width or height shall be adjusted automatically\n
	 * false   no adjustment
	 */
    public void setScaling(String value) {
        if (value.equalsIgnoreCase("uniform")) {
            this.scaleUniform = true;
        }
    }

    /**
	 * Sets the url of the image.
	 *
	 * @param urlString Image url like "file://..."
	 * @throws IOException On error
	 */
    public void setURL(String urlString) throws IOException {
        URL tmpUrl = null;
        try {
            tmpUrl = new URL(urlString);
        } catch (MalformedURLException e) {
            try {
                tmpUrl = new File(urlString).toURL();
            } catch (MalformedURLException ee) {
                throw new ExternalGraphicException("The attribute 'src' of <fo:external-graphic> has a invalid value: '" + urlString + "' (" + ee + ")");
            }
        }
        this.url = tmpUrl;
    }

    /**
	 * Gets  the compression rate for the image in percent.
	 * @return Compression rate
	 */
    public int getCompressionRate() {
        return graphicCompressionRate;
    }

    /**
	 * Sets the compression rate for the image in percent.
	 *
	 * @param percent Compression rate
	 * @return
	 *  true:   The compression rate is valid (0..100)\n
	 *  false:  The compression rate is invalid
	 */
    public boolean setCompressionRate(int percent) {
        if (percent < 1 || percent > 100) return false;
        graphicCompressionRate = percent;
        return true;
    }

    /**
	 * Determines wheter the image is a jpeg.
	 *
	 * @param data Image
	 *
	 * @return
	 * true    If JPEG type\n
	 * false   Other type
	 */
    private boolean isJPEG(byte[] data) {
        byte[] pattern = new byte[] { (byte) 0xFF, (byte) 0xD8 };
        return ImageUtil.compareHexValues(pattern, data, 0, true);
    }

    /**
	 * Determines wheter the image is a png.
	 *
	 * @param data Image
	 *
	 * @return
	 * true    If PNG type\n
	 * false   Other type
	 */
    private boolean isPNG(byte[] data) {
        byte[] pattern = new byte[] { (byte) 0x50, (byte) 0x4E, (byte) 0x47 };
        return ImageUtil.compareHexValues(pattern, data, 1, true);
    }

    /**
	 * Determines wheter the image is a emf.
	 *
	 * @param data Image
	 *
	 * @return
	 * true    If EMF type\n
	 * false   Other type
	 */
    private boolean isEMF(byte[] data) {
        byte[] pattern = new byte[] { (byte) 0x01, (byte) 0x00, (byte) 0x00 };
        return ImageUtil.compareHexValues(pattern, data, 0, true);
    }

    /**
	 * Determines wheter the image is a gif.
	 *
	 * @param data Image
	 *
	 * @return
	 * true    If GIF type\n
	 * false   Other type
	 */
    private boolean isGIF(byte[] data) {
        byte[] pattern = new byte[] { (byte) 0x47, (byte) 0x49, (byte) 0x46, (byte) 0x38 };
        return ImageUtil.compareHexValues(pattern, data, 0, true);
    }

    /**
	 * Determines wheter the image is a gif.
	 *
	 * @param data Image
	 *
	 * @return
	 * true    If BMP type\n
	 * false   Other type
	 */
    private boolean isBMP(byte[] data) {
        byte[] pattern = new byte[] { (byte) 0x42, (byte) 0x4D };
        return ImageUtil.compareHexValues(pattern, data, 0, true);
    }

    /**
	 * Determine image file format.
	 *
	 * @param data Image
	 * @param ext Image extension
	 *
	 * @return Image type by ImageConstants.java
	 */
    private int determineImageType(byte[] data, String ext) {
        int type = ImageConstants.I_NOT_SUPPORTED;
        if (isPNG(data)) {
            type = ImageConstants.I_PNG;
        } else if (isJPEG(data)) {
            type = ImageConstants.I_JPG_C;
        } else if (isEMF(data)) {
            type = ImageConstants.I_EMF;
        } else if (isGIF(data)) {
            type = ImageConstants.I_GIF;
        } else {
            Object tmp = ImageConstants.SUPPORTED_IMAGE_TYPES.get(ext.toLowerCase());
            if (tmp != null) {
                type = ((Integer) tmp).intValue();
            }
        }
        return type;
    }

    /** true if this element would generate no "useful" RTF content */
    public boolean isEmpty() {
        return url == null;
    }
}
