package org.openXpertya.print.pdf.text.rtf.graphic;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import org.openXpertya.print.pdf.text.DocumentException;
import org.openXpertya.print.pdf.text.Element;
import org.openXpertya.print.pdf.text.Image;
import org.openXpertya.print.pdf.text.pdf.codec.wmf.MetaDo;
import org.openXpertya.print.pdf.text.rtf.RtfElement;
import org.openXpertya.print.pdf.text.rtf.document.RtfDocument;
import org.openXpertya.print.pdf.text.rtf.text.RtfParagraph;

/**
 * The RtfImage contains one image. Supported image types are jpeg, png, wmf, bmp.
 * 
 * @version $Version:$
 * @author Mark Hall (mhall@edu.uni-klu.ac.at)
 * @author Paulo Soares
 */
public class RtfImage extends RtfElement {

    /**
     * Constant for the shape/picture group
     */
    private static final byte[] PICTURE_GROUP = "\\*\\shppict".getBytes();

    /**
     * Constant for a picture
     */
    private static final byte[] PICTURE = "\\pict".getBytes();

    /**
     * Constant for a jpeg image
     */
    private static final byte[] PICTURE_JPEG = "\\jpegblip".getBytes();

    /**
     * Constant for a png image
     */
    private static final byte[] PICTURE_PNG = "\\pngblip".getBytes();

    /**
     * Constant for a bmp image
     */
    private static final byte[] PICTURE_BMP = "\\dibitmap0".getBytes();

    /**
     * Constant for a wmf image
     */
    private static final byte[] PICTURE_WMF = "\\wmetafile8".getBytes();

    /**
     * Constant for the picture width
     */
    private static final byte[] PICTURE_WIDTH = "\\picw".getBytes();

    /**
     * Constant for the picture height
     */
    private static final byte[] PICTURE_HEIGHT = "\\pich".getBytes();

    /**
     * Constant for the picture width scale
     */
    private static final byte[] PICTURE_SCALE_X = "\\picscalex".getBytes();

    /**
     * Constant for the picture height scale
     */
    private static final byte[] PICTURE_SCALE_Y = "\\picscaley".getBytes();

    /**
     * The type of image this is.
     */
    private int imageType = Image.ORIGINAL_NONE;

    /**
     * The actual image. Already formated for direct inclusion in the rtf document
     */
    private byte[] image = new byte[0];

    /**
     * The alignment of this picture
     */
    private int alignment = Element.ALIGN_LEFT;

    /**
     * The width of this picture
     */
    private float width = 0;

    /**
     * The height of this picutre
     */
    private float height = 0;

    /**
     * The intended display width of this picture
     */
    private float plainWidth = 0;

    /**
     * The intended display height of this picture
     */
    private float plainHeight = 0;

    /**
     * Constructs a RtfImage for an Image.
     * 
     * @param doc The RtfDocument this RtfImage belongs to
     * @param image The Image that this RtfImage wraps
     * @throws DocumentException If an error occured accessing the image content
     */
    public RtfImage(RtfDocument doc, Image image) throws DocumentException {
        super(doc);
        imageType = image.getOriginalType();
        if (!(imageType == Image.ORIGINAL_JPEG || imageType == Image.ORIGINAL_BMP || imageType == Image.ORIGINAL_PNG || imageType == Image.ORIGINAL_WMF)) {
            throw new DocumentException("Only BMP, PNG, WMF and JPEG images are supported by the RTF Writer");
        }
        alignment = image.alignment();
        width = image.width();
        height = image.height();
        plainWidth = image.plainWidth();
        plainHeight = image.plainHeight();
        this.image = getImage(image);
    }

    /**
     * Extracts the image data from the Image. The data is formated for direct inclusion
     * in a rtf document
     * 
     * @param image The Image for which to extract the content
     * @return The image data formated for the rtf document
     * @throws DocumentException If an error occurs accessing the image content
     */
    private byte[] getImage(Image image) throws DocumentException {
        ByteArrayOutputStream imageTemp = new ByteArrayOutputStream();
        try {
            InputStream imageIn;
            if (imageType == Image.ORIGINAL_BMP) {
                imageIn = new ByteArrayInputStream(MetaDo.wrapBMP(image));
            } else {
                if (image.getOriginalData() == null) {
                    imageIn = image.url().openStream();
                } else {
                    imageIn = new ByteArrayInputStream(image.getOriginalData());
                }
                if (imageType == Image.ORIGINAL_WMF) {
                    long skipLength = 22;
                    while (skipLength > 0) {
                        skipLength = skipLength - imageIn.skip(skipLength);
                    }
                }
            }
            int buffer = 0;
            int count = 0;
            while ((buffer = imageIn.read()) != -1) {
                String helperStr = Integer.toHexString(buffer);
                if (helperStr.length() < 2) helperStr = "0" + helperStr;
                imageTemp.write(helperStr.getBytes());
                count++;
                if (count == 64) {
                    imageTemp.write((byte) '\n');
                    count = 0;
                }
            }
        } catch (IOException ioe) {
            throw new DocumentException(ioe.getMessage());
        }
        return imageTemp.toByteArray();
    }

    /**
     * Writes the RtfImage content
     * 
     * @return the RtfImage content
     */
    public byte[] write() {
        ByteArrayOutputStream result = new ByteArrayOutputStream();
        try {
            switch(alignment) {
                case Element.ALIGN_LEFT:
                    result.write(RtfParagraph.ALIGN_LEFT);
                    break;
                case Element.ALIGN_RIGHT:
                    result.write(RtfParagraph.ALIGN_RIGHT);
                    break;
                case Element.ALIGN_CENTER:
                    result.write(RtfParagraph.ALIGN_CENTER);
                    break;
                case Element.ALIGN_JUSTIFIED:
                    result.write(RtfParagraph.ALIGN_JUSTIFY);
                    break;
            }
            result.write(OPEN_GROUP);
            result.write(PICTURE_GROUP);
            result.write(OPEN_GROUP);
            result.write(PICTURE);
            switch(imageType) {
                case Image.ORIGINAL_JPEG:
                    result.write(PICTURE_JPEG);
                    break;
                case Image.ORIGINAL_PNG:
                    result.write(PICTURE_PNG);
                    break;
                case Image.ORIGINAL_WMF:
                case Image.ORIGINAL_BMP:
                    result.write(PICTURE_WMF);
                    break;
            }
            result.write(PICTURE_WIDTH);
            result.write(intToByteArray((int) (plainWidth * RtfElement.TWIPS_FACTOR)));
            result.write(PICTURE_HEIGHT);
            result.write(intToByteArray((int) (plainHeight * RtfElement.TWIPS_FACTOR)));
            if (width > 0) {
                result.write(PICTURE_SCALE_X);
                result.write(intToByteArray((int) (100 / width * plainWidth)));
            }
            if (height > 0) {
                result.write(PICTURE_SCALE_Y);
                result.write(intToByteArray((int) (100 / height * plainHeight)));
            }
            result.write(DELIMITER);
            result.write((byte) '\n');
            result.write(image);
            result.write(CLOSE_GROUP);
            result.write(CLOSE_GROUP);
            result.write((byte) '\n');
        } catch (IOException ioe) {
            ioe.printStackTrace();
        }
        return result.toByteArray();
    }
}
