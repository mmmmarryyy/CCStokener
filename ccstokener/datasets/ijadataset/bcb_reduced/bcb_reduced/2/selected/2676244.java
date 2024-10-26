package com.potix.image;

import java.io.File;
import java.io.InputStream;
import java.io.FileInputStream;
import java.io.ByteArrayInputStream;
import java.io.Reader;
import java.io.IOException;
import java.net.URL;
import java.util.Iterator;
import javax.swing.ImageIcon;
import javax.imageio.ImageIO;
import javax.imageio.ImageReader;
import javax.imageio.stream.ImageInputStream;
import com.potix.lang.Objects;
import com.potix.lang.SystemException;
import com.potix.util.logging.Log;
import com.potix.io.Files;
import com.potix.util.media.ContentTypes;

/**
 * Represents an image.
 * Unlike java.awt.Image and javax.swing.ImageIcon, this class holds
 * the raw image data, i.e., the original format, as opaque.
 *
 * <p>In other words, it is used to retrieve and store the opaque data
 * as polymorphic thru the {@link com.potix.util.media.Media} interface.
 *
 * @author <a href="mailto:tomyeh@potix.com">tomyeh@potix.com</a>
 */
public class AImage implements Image {

    private static final Log log = Log.lookup(AImage.class);

    /** The raw data. */
    protected byte[] _data;

    /** The format name, e.g., "jpeg", "gif" and "png". */
    protected final String _format;

    /** The content type. */
    protected final String _ctype;

    /** The name (usually filename). */
    protected final String _name;

    /** The width. */
    protected final int _width;

    /** The height. */
    protected final int _height;

    public AImage(String name, byte[] data) throws IOException {
        if (data == null) throw new IllegalArgumentException("null data");
        _name = name;
        _data = data;
        String format = null;
        try {
            final ImageInputStream imis = ImageIO.createImageInputStream(new ByteArrayInputStream(data));
            final Iterator it = ImageIO.getImageReaders(imis);
            if (it.hasNext()) {
                final ImageReader rd = (ImageReader) it.next();
                format = rd.getFormatName().toLowerCase();
            }
        } catch (IOException ex) {
        }
        if (format == null) {
            _format = getFormatByName(name);
            if (_format == null) throw new IOException("Unknown image format: " + name);
            log.warning("Unsupported image format: " + _format + "; its width and height are assumed to zero");
            _width = _height = 0;
        } else {
            _format = format;
            final ImageIcon ii = new ImageIcon(_data);
            _width = ii.getIconWidth();
            _height = ii.getIconHeight();
        }
        _ctype = getContentType(_format);
    }

    public AImage(String filename) throws IOException {
        this(filename, Files.readAll(new FileInputStream(filename)));
    }

    public AImage(File file) throws IOException {
        this(file.getName(), Files.readAll(new FileInputStream(file)));
    }

    public AImage(InputStream is) throws IOException {
        this(null, Files.readAll(is));
    }

    public AImage(URL url) throws IOException {
        this(getName(url), Files.readAll(url.openStream()));
    }

    private static String getName(URL url) {
        String name = url.getPath();
        if (name != null) {
            {
                final int j = name.lastIndexOf(File.pathSeparatorChar);
                if (j >= 0) name = name.substring(j + 1);
            }
            if (File.pathSeparatorChar != '/') {
                final int j = name.lastIndexOf('/');
                if (j >= 0) name = name.substring(j + 1);
            }
        }
        return name;
    }

    private static String getContentType(String format) {
        final String ctype = ContentTypes.getContentType(format);
        return ctype != null ? ctype : "image/" + format;
    }

    private static String getFormatByName(String name) {
        if (name != null) {
            final int j = name.lastIndexOf('.') + 1, k = name.lastIndexOf('/') + 1;
            if (j > k && j < name.length()) return name.substring(j);
        }
        return null;
    }

    public final boolean isBinary() {
        return true;
    }

    public final boolean inMemory() {
        return true;
    }

    public byte[] getByteData() {
        return _data;
    }

    /** Always throws IllegalStateException.
	 */
    public final String getStringData() {
        throw new IllegalStateException("Use getByteData() instead");
    }

    /** An input stream on top of {@link #getByteData}.
	 */
    public final InputStream getStreamData() {
        return new ByteArrayInputStream(_data);
    }

    /** Always throws IllegalStateException.
	 */
    public final Reader getReaderData() {
        throw new IllegalStateException("Use getStreamData() instead");
    }

    public final String getName() {
        return _name;
    }

    public final String getFormat() {
        return _format;
    }

    public final String getContentType() {
        return _ctype;
    }

    /** Returns the width.
	 */
    public final int getWidth() {
        return _width;
    }

    /** Returns the height.
	 */
    public final int getHeight() {
        return _height;
    }

    /** Converts to an image icon.
	 */
    public final ImageIcon toImageIcon() {
        return new ImageIcon(_data, _format);
    }
}
