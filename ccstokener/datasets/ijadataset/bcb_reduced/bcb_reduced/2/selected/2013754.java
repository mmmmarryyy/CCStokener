package co.edu.unal.ungrid.image.util;

import java.awt.image.BufferedImage;
import java.io.BufferedInputStream;
import java.net.MalformedURLException;
import java.net.URL;
import java.util.HashMap;
import java.util.Map;
import javax.imageio.ImageIO;
import co.edu.unal.ungrid.image.AbstractImage;
import co.edu.unal.ungrid.image.AbstractPlane;
import co.edu.unal.ungrid.image.DoubleImage;
import co.edu.unal.ungrid.image.analyze.AnalyzeUtil;
import co.edu.unal.ungrid.image.dicom.core.DicomInputStream;
import co.edu.unal.ungrid.image.dicom.display.SourceImage;
import co.edu.unal.ungrid.image.nifti.NiftiUtil;

public abstract class ImageFactory<Plane extends AbstractPlane> {

    protected abstract AbstractImage<Plane> create();

    public static <P extends AbstractPlane> void register(final String cls, final ImageFactory<P> factory) {
        m_factories.put(cls, factory);
    }

    @SuppressWarnings("unchecked")
    public static final <P extends AbstractPlane> AbstractImage<P> getInstance(final String cls) {
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
        @SuppressWarnings("rawtypes") ImageFactory factory = m_factories.get(cls);
        return factory.create();
    }

    public static boolean isDicom(final URL url) {
        assert url != null;
        boolean isDicom = false;
        if (url != null) {
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
        }
        return isDicom;
    }

    public static BufferedImage readDicom(final SourceImage src, final URL url) {
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

    public static boolean isNifti(final URL url) {
        assert url != null;
        return (url != null ? NiftiUtil.isNifti(url.getFile()) : false);
    }

    public static AbstractImage<AbstractPlane> readNifti(final URL url) {
        assert url != null;
        return (url != null ? NiftiUtil.readImage(url.getFile()) : null);
    }

    public static boolean isAnalyze(final URL url) {
        assert url != null;
        return (url != null ? AnalyzeUtil.isAnalyze(url.getFile()) : false);
    }

    public static AbstractImage<AbstractPlane> readAnalyze(final URL url) {
        assert url != null;
        return (url != null ? AnalyzeUtil.readImage(url.getFile()) : null);
    }

    public static URL getURL(String sUrl) {
        assert sUrl != null;
        URL url = null;
        if (sUrl != null && sUrl.length() > 0) {
            int p = sUrl.indexOf(FILE_PROTOCOL);
            if (p < 0) {
                int c = sUrl.indexOf(':');
                if (c > 0) {
                    sUrl = FILE_PROTOCOL + '/' + sUrl;
                } else {
                    sUrl = FILE_PROTOCOL + sUrl;
                }
            }
            try {
                url = new URL(sUrl);
            } catch (Exception exc) {
                System.out.println("ImageUtil::getURL():" + sUrl + ": " + exc);
            }
        }
        return url;
    }

    public static BufferedImage readNonMedical(final String sUrl) {
        return readNonMedical(getURL(sUrl));
    }

    public static BufferedImage readNonMedical(final URL url) {
        assert url != null;
        BufferedImage bi = null;
        if (url != null) {
            try {
                bi = ImageIO.read(url);
            } catch (Exception exc) {
                System.out.println("ImageFactory::readNonMedical(" + url + "): exc=" + exc);
            }
        }
        return bi;
    }

    public static void testFactory(String[] args) {
        String cls = DoubleImage.class.getName();
        System.out.println("img: " + getInstance(cls));
    }

    public static void testIsDicom(String[] args) {
        for (int i = 0; i < args.length; i++) {
            System.out.println(args[i] + " is DICOM: " + isDicom(getURL(args[i])));
        }
    }

    public static void testImageIO(String[] args) {
        if (args.length > 0) {
            try {
                BufferedImage bi = readNonMedical(new URL(args[0]));
                if (bi != null) {
                    ImageUtil.showImage(bi, 100, 100, args[0]);
                }
            } catch (MalformedURLException exc) {
                exc.printStackTrace();
            }
        }
    }

    public static void main(String[] args) {
        testImageIO(args);
    }

    private static final int DICOM_PREAMBLE_SIZE = 128;

    private static final byte[] DICM = { 'D', 'I', 'C', 'M' };

    private static Map<String, ImageFactory<?>> m_factories = new HashMap<String, ImageFactory<?>>();

    public static final String FILE_PROTOCOL = "file://";
}
