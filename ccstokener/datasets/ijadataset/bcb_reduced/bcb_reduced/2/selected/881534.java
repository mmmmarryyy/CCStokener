package org.chemicalcovers.utils;

import java.awt.Dimension;
import java.awt.Graphics2D;
import java.awt.Image;
import java.awt.RenderingHints;
import java.awt.image.BufferedImage;
import java.awt.image.ImageObserver;
import java.awt.image.PixelGrabber;
import java.io.IOException;
import java.io.InputStream;
import java.net.URL;
import java.util.HashMap;
import java.util.Map;
import org.chemicalcovers.ChemicalCovers;

/**
 * 
 * @author Louis Antoine
 *
 */
public class ImageUtilities {

    public static double PSNR_THRESHOLD_IDENTICAL = 50.0;

    public static double PSNR_THRESHOLD_SAME = 20.0;

    public static double PSNR_THRESHOLD_LIKE = 12.0;

    public static double PSNR_THRESHOLD_MINIMAL = 8.0;

    private static double HANDICAP_PER_PIXEL = 0;

    private static volatile int nbComparisons = 0;

    private static Map<Long, Image> resizeCache = new HashMap<Long, Image>();

    private static int BUF_SIZE = 4096 * 1024;

    private static byte[] _buffer = new byte[BUF_SIZE];

    private static int[] buffer = new int[BUF_SIZE];

    private static int offset = 0;

    public static void clearResizeCache() {
        nbComparisons = 0;
        synchronized (resizeCache) {
            ChemicalCovers.LOGGER.info("ImageUtilities - Cleaning resize cache containing " + resizeCache.size() + " images");
            for (Image image : resizeCache.values()) image.flush();
            resizeCache.clear();
        }
    }

    private static Image getResizedImage(Image srcImage, int w, int h) {
        if (srcImage.getWidth(null) == w && srcImage.getHeight(null) == h) return srcImage;
        synchronized (resizeCache) {
            Long cacheKey = new Long(srcImage.hashCode() + ((long) w << 32) + ((long) h << 48));
            if (resizeCache.containsKey(cacheKey)) return resizeCache.get(cacheKey);
            BufferedImage dstImage = new BufferedImage(w, h, BufferedImage.TYPE_INT_ARGB);
            Graphics2D g = dstImage.createGraphics();
            g.setRenderingHint(RenderingHints.KEY_INTERPOLATION, RenderingHints.VALUE_INTERPOLATION_BILINEAR);
            g.drawImage(srcImage, 0, 0, w, h, null);
            g.dispose();
            resizeCache.put(cacheKey, dstImage);
            return dstImage;
        }
    }

    public static double getMaxMse(double minPsnr) {
        return Math.pow(0xFF / Math.pow(10, minPsnr / 20), 2);
    }

    /**
		imageSimilarity

		The similarity check is based on Mean Squared Error (MSE) and Peak Signal-to-Noise Ratio (PSNR) 
		- MSE = Sum{x,y} [(Ra{x,y}-Rb{x,y})^2 + (Ga{x,y}-Gb{x,y})^2 + (Ba{x,y}-Bb{x,y})^2] / (3 * W * H);
		- PSNR = 20 * log10(PEAK / SQRT(MSE)) 
		You can consider images are the same if PSNR >= 25

		Images compared with this algorithm must have the same size.
		Image are thus resized at median dimensions (WA+WB)/2 x (HA+HB)/2.
		This also ensures the symmetry of the relation 'is similar to'.

		To take the size and ratio deltas into account, MSE is computed on an image with dimensions MAX(WA,WB) x MAX(HA,HB).
		All pixels that are not occupied by the resized images are considered as having a color difference of HANDICAP_PER_PIXEL.
		If you don't want size and ratio deltas to be taken into account, just set HANDICAP_PER_PIXEL to 0.

		Thanks to normSize and reduceFactor you can reduce the image size before the images are actually compared on their MSE. 
		When to use normSize and when to use reduceFactor ?

		If you use normSize:
		- you control the size of the images that will be compared on their MSE
		- but you loose the information of the size difference between the two original images 
		  (i.e. this difference will not be taken into account for the similarity measure) 

		If you use reduceFactor:
		- you don't control the size of the images that will be compared on their MSE
		- but you keep the information of the size difference between the two original images 
		  (and you can control the effect of this difference on the PSNR thanks to HANDICAP_PER_PIXEL) 

	 */
    public static double imageSimilarity(Image imagea, Image imageb, double maxMse) {
        return imageSimilarity(imagea, imageb, maxMse, false, 80, 0);
    }

    private static double imageSimilarity(Image imagea, Image imageb, double maxMse, boolean compareInGrayscale, int normSize, int reduceFactor) {
        nbComparisons++;
        double wa = imagea.getWidth(null);
        double wb = imageb.getWidth(null);
        double ha = imagea.getHeight(null);
        double hb = imageb.getHeight(null);
        if (normSize > 0) {
            double ratioa = wa / ha;
            double ratiob = wb / hb;
            wa = ratioa > 1 ? normSize : normSize * ratioa;
            ha = ratioa > 1 ? normSize / ratioa : normSize;
            wb = ratiob > 1 ? normSize : normSize * ratiob;
            hb = ratiob > 1 ? normSize / ratiob : normSize;
        }
        if (reduceFactor > 0) {
            wa /= reduceFactor;
            wb /= reduceFactor;
            ha /= reduceFactor;
            hb /= reduceFactor;
        }
        int wmax = (int) Math.round(wa > wb ? wa : wb);
        int hmax = (int) Math.round(ha > hb ? ha : hb);
        int surfacemax = wmax * hmax;
        int w = (int) Math.round((wa + wb) / 2.0);
        int h = (int) Math.round((ha + hb) / 2.0);
        Image dstimga = getResizedImage(imagea, w, h);
        Image dstimgb = getResizedImage(imageb, w, h);
        int[] pixelsa = new int[w * h];
        int[] pixelsb = new int[w * h];
        PixelGrabber pga = new PixelGrabber(dstimga, 0, 0, w, h, pixelsa, 0, w);
        PixelGrabber pgb = new PixelGrabber(dstimgb, 0, 0, w, h, pixelsb, 0, w);
        try {
            pga.grabPixels();
            pgb.grabPixels();
        } catch (InterruptedException e) {
            System.err.println("interrupted waiting for pixels!");
            return 0;
        }
        if ((pga.getStatus() & ImageObserver.ABORT) != 0 || (pgb.getStatus() & ImageObserver.ABORT) != 0) {
            System.err.println("image fetch aborted or errored");
            return 0;
        }
        double mse = HANDICAP_PER_PIXEL != 0 ? Math.pow(HANDICAP_PER_PIXEL, 2.0) * (1.0 - ((w * h) / surfacemax)) : 0;
        boolean maxErrorreached = mse > maxMse;
        for (int j = 0; j < h; j++) {
            for (int i = 0; i < w; i++) {
                if (maxErrorreached) break;
                int rgba = pixelsa[j * w + i];
                int rgbb = pixelsb[j * w + i];
                double delta = 0.0;
                if (compareInGrayscale) {
                    long graya = Math.round(.299 * ((rgba >> 16) & 0xFF) + .587 * ((rgba >> 8) & 0xFF) + .114 * (rgba & 0xFF));
                    long grayb = Math.round(.299 * ((rgbb >> 16) & 0xFF) + .587 * ((rgbb >> 8) & 0xFF) + .114 * (rgbb & 0xFF));
                    long deltag = graya - grayb;
                    delta = deltag * deltag;
                } else {
                    long deltar = ((rgba >> 16) & 0xFF) - ((rgbb >> 16) & 0xFF);
                    long deltag = ((rgba >> 8) & 0xFF) - ((rgbb >> 8) & 0xFF);
                    long deltab = (rgba & 0xFF) - (rgbb & 0xFF);
                    delta = (deltar * deltar) + (deltag * deltag) + (deltab * deltab) / 3.0;
                }
                mse += delta / surfacemax;
                if (mse > maxMse) {
                    maxErrorreached = true;
                    break;
                }
            }
        }
        if (maxErrorreached) return 0;
        if (mse == 0) return PSNR_THRESHOLD_IDENTICAL;
        return Math.min(PSNR_THRESHOLD_IDENTICAL - 1, 20 * Math.log10(0xFF / Math.sqrt(mse)));
    }

    public static Dimension getNewImageSize(Dimension originalDimensions, Dimension maxDimensions) {
        Dimension newDimensions = new Dimension(0, 0);
        if (maxDimensions == null || (maxDimensions.getWidth() == 0 && maxDimensions.getHeight() == 0)) return originalDimensions;
        double originalWidth = originalDimensions.getWidth();
        double originalHeight = originalDimensions.getHeight();
        double maxWidth = maxDimensions.getWidth();
        double maxHeight = maxDimensions.getHeight();
        double originalRatio = originalDimensions.getWidth() / originalDimensions.getHeight();
        double maxRatio = maxDimensions.getWidth() / maxDimensions.getHeight();
        if (maxWidth == 0) maxWidth = originalRatio * maxHeight;
        if (maxHeight == 0) maxHeight = maxWidth / originalRatio;
        if (originalRatio > maxRatio) {
            newDimensions.setSize(maxWidth, (maxWidth / originalWidth) * originalHeight);
        } else {
            newDimensions.setSize((maxHeight / originalHeight) * originalWidth, maxHeight);
        }
        return newDimensions;
    }

    public static synchronized Dimension getImageDimensionsFromJPEGHeader(URL url, int kB) throws IOException {
        Dimension dimension = new Dimension(0, 0);
        InputStream stream = url.openConnection().getInputStream();
        offset = 0;
        offset = readBytes(stream, 10);
        if (offset < 0) return dimension;
        if (!(buffer[0] == 0xFF && buffer[1] == 0xD8 && buffer[2] == 0xFF && buffer[3] == 0xE0)) return dimension;
        if (!(buffer[6] == 'J' && buffer[7] == 'F' && buffer[8] == 'I' && buffer[9] == 'F' && buffer[10] == 0x00)) return dimension;
        for (int position = 2; position + 4 < BUF_SIZE; ) {
            if (offset < position + 4) {
                int bytesRead = readBytes(stream, position + 4 - offset);
                if (bytesRead < 0) break;
                offset += bytesRead;
            }
            int readBlockSize = readBlock(position, dimension);
            if (readBlockSize <= 0) break;
            position += readBlockSize;
        }
        stream.close();
        return dimension;
    }

    private static int readBytes(InputStream stream, int minBytes) throws IOException {
        int bytesRead = 0;
        while (bytesRead < minBytes) {
            int read = stream.read(_buffer, offset + bytesRead, 4096);
            if (read < 0) return read;
            bytesRead += read;
        }
        for (int b = offset; b < offset + bytesRead; b++) buffer[b] = _buffer[b] < 0 ? _buffer[b] + 256 : _buffer[b];
        return bytesRead;
    }

    private static int readBlock(int position, Dimension dimension) {
        if (buffer[position] != 0xFF) return -1;
        if (buffer[position + 1] == 0xC0) {
            dimension.height = buffer[position + 5] * 256 + buffer[position + 6];
            dimension.width = buffer[position + 7] * 256 + buffer[position + 8];
            return 0;
        }
        return 2 + (buffer[position + 2] * 256) + buffer[position + 3];
    }
}
