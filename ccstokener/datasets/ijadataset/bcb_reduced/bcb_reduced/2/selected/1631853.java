package com.wizzer.m3g;

import java.awt.image.BufferedImage;
import java.io.*;
import java.net.*;
import java.util.*;
import com.wizzer.m3g.toolkit.png.PNGDecoder;

/**
 * The <code>ExternalReference</code> is a utility for reading/writing
 * an External Reference from/to a M3G file. It is not a runtime
 * construct called out by the M3G Specification.
 * <p>
 * This class is used to encapsulate the external reference URI.
 * </p>
 * 
 * @author Mark Millard
 */
public class ExternalReference extends M3GObject {

    private String m_uri;

    private M3GObject m_reference;

    private static File g_cwd = null;

    ExternalReference() {
    }

    public ExternalReference(String uri) {
        m_uri = uri;
    }

    public static File getCwd() {
        return g_cwd;
    }

    public static void setCwd(File dir) throws IOException {
        if ((!dir.isDirectory()) || (!dir.canRead())) throw new IOException("ExternalReference: invalid directory"); else g_cwd = dir;
    }

    public static void setCwd(String dirname) throws IOException {
        File dir = new File(dirname);
        if ((!dir.isDirectory()) || (!dir.canRead())) {
            throw new IOException("ExternalReference: invalid directory");
        } else g_cwd = dir;
    }

    public String getURI() {
        return m_uri;
    }

    public void setURI(String uri) {
        m_uri = uri;
    }

    public int getObjectType() {
        return EXTERNAL_REFERENCE;
    }

    public M3GObject getReference() {
        return m_reference;
    }

    protected void unmarshall(M3GInputStream is, ArrayList table) throws IOException {
        m_uri = is.readString();
        try {
            URI uri = new URI(m_uri);
            if (uri.getScheme() == null) {
                if (getCwd() != null) {
                    URI path = getCwd().toURI();
                    uri = path.resolve(uri);
                }
            }
            m_reference = resolvePNG(uri.normalize());
            if (m_reference == null) {
                throw new IOException("ExternalReference: external M3G file not implemented");
            } else return;
        } catch (URISyntaxException ex) {
            throw new IOException(ex.getMessage());
        }
    }

    protected void marshall(M3GOutputStream os, ArrayList table) throws IOException {
        os.writeString(m_uri);
    }

    private Image2D resolvePNG(URI uri) {
        Image2D image2D = null;
        if (uri.getScheme() != null) {
            try {
                URL url = new URL(uri.toString());
                InputStream is = url.openStream();
                BufferedImage image = PNGDecoder.decode(is);
                int imageType = Image2D.RGB;
                if (image.getType() == BufferedImage.TYPE_INT_RGB) imageType = Image2D.RGB; else if (image.getType() == BufferedImage.TYPE_INT_ARGB) imageType = Image2D.RGBA;
                image2D = new Image2D(imageType, image);
            } catch (IOException ex) {
            }
        } else {
            String path = uri.getPath();
            File file = new File(path);
            if (file.getAbsoluteFile().exists()) {
                try {
                    FileInputStream is = new FileInputStream(file);
                    BufferedImage image = PNGDecoder.decode(is);
                    int imageType = Image2D.RGB;
                    if (image.getType() == BufferedImage.TYPE_INT_RGB) imageType = Image2D.RGB; else if (image.getType() == BufferedImage.TYPE_INT_ARGB) imageType = Image2D.RGBA;
                    image2D = new Image2D(imageType, image);
                } catch (FileNotFoundException ex) {
                } catch (IOException ex) {
                }
            }
        }
        return image2D;
    }
}
