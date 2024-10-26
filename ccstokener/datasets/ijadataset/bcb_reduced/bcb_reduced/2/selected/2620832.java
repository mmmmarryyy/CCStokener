package com.flagstone.transform.util.font;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.net.URL;
import java.net.URLConnection;
import java.util.List;
import java.util.zip.DataFormatException;

/**
 * ImageFactory is used to generate an image definition object from an image
 * stored in a file, references by a URL or read from an stream. An plug-in
 * architecture allows decoders to be registered to handle different image
 * formats. The ImageFactory provides a standard interface for using the
 * decoders.
 */
public final class FontFactory {

    /** The object used to decode the font. */
    private transient FontDecoder decoder;

    /**
     * Read a font stored in the specified file.
     *
     * @param file
     *            a file containing the abstract path to the font.
     *
     * @throws IOException
     *             if there is an error reading the file.
     *
     * @throws DataFormatException
     *             if there is a problem decoding the font, either it is in an
     *             unsupported format or an error occurred while decoding the
     *             data.
     */
    public void read(final File file) throws IOException, DataFormatException {
        String fontType;
        if (file.getName().endsWith("ttf")) {
            fontType = "ttf";
        } else if (file.getName().endsWith("swf")) {
            fontType = "swf";
        } else {
            throw new DataFormatException("Unsupported format");
        }
        decoder = FontRegistry.getFontProvider(fontType);
        decoder.read(file);
    }

    /**
     * Read a font referenced by a URL.
     *
     * @param url
     *            the Uniform Resource Locator referencing the file.
     *
     * @throws IOException
     *             if there is an error reading the file.
     *
     * @throws DataFormatException
     *             if there is a problem decoding the font, either it is in an
     *             unsupported format or an error occurred while decoding the
     *             font data.
     */
    public void read(final URL url) throws IOException, DataFormatException {
        final URLConnection connection = url.openConnection();
        final int fileSize = connection.getContentLength();
        if (fileSize < 0) {
            throw new FileNotFoundException(url.getFile());
        }
        final String mimeType = connection.getContentType();
        decoder = FontRegistry.getFontProvider(mimeType);
        if (decoder == null) {
            throw new DataFormatException("Unsupported format");
        }
        decoder.read(url);
    }

    /**
     * Get the list of fonts decoded.
     * @return a list containing a Font object for each font decoded.
     */
    public List<Font> getFonts() {
        return decoder.getFonts();
    }
}
