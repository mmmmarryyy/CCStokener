package co.edu.unal.ungrid.image;

import java.awt.image.BufferedImage;
import java.io.BufferedInputStream;
import java.io.File;
import java.net.URL;
import java.util.HashMap;
import java.util.Map;
import javax.imageio.ImageIO;
import co.edu.unal.ungrid.image.dicom.core.DicomInputStream;
import co.edu.unal.ungrid.image.dicom.display.SourceImage;

public abstract class ImageFactory<Plane extends AbstractPlane> {

    protected abstract AbstractImage<Plane> create();

    public static <P extends AbstractPlane> void register(String cls, ImageFactory<P> factory) {
        m_factories.put(cls, factory);
    }

    @SuppressWarnings("unchecked")
    public static final <P extends AbstractPlane> AbstractImage<P> getInstance(String cls) {
        assert cls != null;
        if (!m_factories.containsKey(cls)) {
            try {
                Class.forName(cls);
            } catch (ClassNotFoundException exc) {
                return null;
            }
            if (!m_factories.containsKey(cls)) {
                return null;
            }
        }
        ImageFactory factory = m_factories.get(cls);
        return factory.create();
    }

    public static URL getURL(String sUrl) {
        assert sUrl != null;
        URL url = null;
        try {
            url = new URL(sUrl);
        } catch (Exception exc) {
            System.out.println("ImageFactory::getURL():" + sUrl + ": " + exc);
        }
        return url;
    }

    public static URL getURL(File f) {
        assert f != null;
        URL url = null;
        try {
            url = f.toURI().toURL();
        } catch (Exception exc) {
            System.out.println("ImageFactory::getURL():" + f + ": " + exc);
        }
        return url;
    }

    public static boolean isDicom(String sUrl) {
        URL url = getURL(sUrl);
        return (url == null ? false : isDicom(url));
    }

    public static boolean isDicom(URL url) {
        assert url != null;
        boolean isDicom = false;
        BufferedInputStream is = null;
        try {
            is = new BufferedInputStream(url.openStream());
            is.skip(DICOM_PREAMBLE_SIZE);
            byte[] buf = new byte[DICM.length];
            is.read(buf);
            if (buf[0] == DICM[0] && buf[1] == DICM[1] && buf[2] == DICM[2] && buf[3] == DICM[3]) {
                isDicom = true;
            }
        } catch (Exception exc) {
            System.out.println("ImageFactory::isDicom(): exc=" + exc);
        } finally {
            if (is != null) {
                try {
                    is.close();
                } catch (Exception exc) {
                }
            }
        }
        return isDicom;
    }

    public static BufferedImage readDicom(final URL url, final SourceImage src) {
        assert url != null;
        assert src != null;
        BufferedImage bi = null;
        try {
            DicomInputStream dis = new DicomInputStream(new BufferedInputStream(url.openStream()));
            src.read(dis);
            dis.close();
            bi = src.getBufferedImage();
        } catch (Exception exc) {
            System.out.println("ImageFactory::readDicom(): exc=" + exc);
        }
        return bi;
    }

    public static BufferedImage readStandard(URL url) {
        BufferedImage bi = null;
        try {
            bi = ImageIO.read(url);
        } catch (Exception exc) {
            System.out.println("ImageFactory::readStandard(): exc=" + exc);
        }
        return bi;
    }

    public static BufferedImage load(final URL url, final SourceImage src) {
        assert url != null;
        assert src != null;
        BufferedImage bi = null;
        if (isDicom(url)) {
            bi = readDicom(url, src);
        } else {
            bi = readStandard(url);
        }
        return bi;
    }

    public static BufferedImage load(final String sUrl, final SourceImage src) {
        URL url = getURL(sUrl);
        return (url == null ? null : load(url, src));
    }

    public static BufferedImage load(final File f, final SourceImage src) {
        URL url = getURL(f);
        return (url == null ? null : load(url, src));
    }

    public static void testFactory(String[] args) {
        String cls = "co.edu.unal.ungrid.image.VoxelImage";
        System.out.println("img: " + getInstance(cls));
    }

    public static void testIsDicom(String[] args) {
        for (int i = 0; i < args.length; i++) {
            System.out.println(args[i] + " is DICOM: " + isDicom(args[i]));
        }
    }

    public static void main(String[] args) {
        testFactory(args);
    }

    private static final int DICOM_PREAMBLE_SIZE = 128;

    private static final byte[] DICM = { 'D', 'I', 'C', 'M' };

    @SuppressWarnings("unchecked")
    private static Map<String, ImageFactory> m_factories = new HashMap<String, ImageFactory>();
}
