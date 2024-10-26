package test.de.offis.semanticmm4u.media_elements_connector;

import java.awt.image.BufferedImage;
import java.awt.image.Raster;
import java.io.IOException;
import java.io.InputStream;
import java.net.MalformedURLException;
import java.net.URL;
import java.util.Calendar;
import javax.imageio.ImageIO;
import junit.framework.TestCase;
import com.sun.image.codec.jpeg.JPEGCodec;
import com.sun.image.codec.jpeg.JPEGImageDecoder;
import com.sun.jimi.core.Jimi;
import com.sun.jimi.core.JimiException;
import com.sun.jimi.core.raster.JimiRasterImage;
import de.offis.semanticmm4u.global.Constants;
import de.offis.semanticmm4u.media_elements_connector.media_elements_creators.ImageInfo;

public class TestJPEGHeaderBenchmark extends TestCase {

    private String images[] = new String[] { "DSCN0040.JPG", "DSCN0041.JPG", "DSCN0042.JPG", "DSCN0062.JPG", "DSCN0091.JPG", "DSCN0084.JPG", "DSCN0080.JPG", "DSCN0078.JPG", "DSCN0077.JPG" };

    public void testImageIO() throws MalformedURLException, IOException {
        System.out.println("ImageIO:");
        long start = Calendar.getInstance().getTimeInMillis();
        for (int i = 0; i < images.length; i++) {
            String url = Constants.getDefaultURIMediaConnectorBasePath() + "albums/hund/" + images[i];
            BufferedImage image = ImageIO.read(new URL(url));
            int width = image.getWidth();
            int height = image.getHeight();
            System.out.println(width + "x" + height);
        }
        long stop = Calendar.getInstance().getTimeInMillis();
        System.out.println("zeit: " + (stop - start));
    }

    public void testImageInfo() throws MalformedURLException, IOException {
        System.out.println("ImageInfo:");
        long start = Calendar.getInstance().getTimeInMillis();
        for (int i = 0; i < images.length; i++) {
            String url = Constants.getDefaultURIMediaConnectorBasePath() + "albums/hund/" + images[i];
            InputStream istream = (new URL(url)).openStream();
            ImageInfo ii = new ImageInfo();
            ii.setInput(istream);
            assertTrue("Not a supported image file format.", ii.check());
            int width = ii.getWidth();
            int height = ii.getHeight();
            System.out.println(width + "x" + height);
        }
        long stop = Calendar.getInstance().getTimeInMillis();
        System.out.println("zeit: " + (stop - start));
    }

    public void testJPEGBuffImage() throws MalformedURLException, IOException {
        System.out.println("JPEGCodec BufferedImage:");
        long start = Calendar.getInstance().getTimeInMillis();
        for (int i = 0; i < images.length; i++) {
            String url = Constants.getDefaultURIMediaConnectorBasePath() + "albums/hund/" + images[i];
            InputStream istream = (new URL(url)).openStream();
            JPEGImageDecoder dec = JPEGCodec.createJPEGDecoder(istream);
            BufferedImage image = dec.decodeAsBufferedImage();
            int width = image.getWidth();
            int height = image.getHeight();
            istream.close();
            System.out.println("w: " + width + " - h: " + height);
        }
        long stop = Calendar.getInstance().getTimeInMillis();
        System.out.println("zeit: " + (stop - start));
    }

    public void testJPEGRaster() throws MalformedURLException, IOException {
        System.out.println("JPEGCodec RasterImage:");
        long start = Calendar.getInstance().getTimeInMillis();
        for (int i = 0; i < images.length; i++) {
            String url = Constants.getDefaultURIMediaConnectorBasePath() + "albums/hund/" + images[i];
            InputStream istream = (new URL(url)).openStream();
            JPEGImageDecoder dec = JPEGCodec.createJPEGDecoder(istream);
            Raster raster = dec.decodeAsRaster();
            int width = raster.getWidth();
            int height = raster.getHeight();
            istream.close();
            System.out.println("w: " + width + " - h: " + height);
        }
        long stop = Calendar.getInstance().getTimeInMillis();
        System.out.println("zeit: " + (stop - start));
    }

    public void testJIMI() throws IOException, MalformedURLException, JimiException {
        System.out.println("JIMI:");
        long start = Calendar.getInstance().getTimeInMillis();
        for (int i = 0; i < images.length; i++) {
            String url = Constants.getDefaultURIMediaConnectorBasePath() + "albums/hund/" + images[i];
            JimiRasterImage image = Jimi.getRasterImage(new URL(url));
            int width = image.getWidth();
            int height = image.getHeight();
            System.out.println(width + "x" + height);
        }
        long stop = Calendar.getInstance().getTimeInMillis();
        System.out.println("zeit: " + (stop - start));
    }
}
