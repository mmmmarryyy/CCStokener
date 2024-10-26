package com.umc.helper;

import java.awt.AlphaComposite;
import java.awt.Color;
import java.awt.Dimension;
import java.awt.Graphics2D;
import java.awt.LinearGradientPaint;
import java.awt.RenderingHints;
import java.awt.Transparency;
import java.awt.color.ColorSpace;
import java.awt.geom.Point2D;
import java.awt.image.BufferedImage;
import java.awt.image.BufferedImageOp;
import java.awt.image.ColorConvertOp;
import java.io.File;
import java.io.IOException;
import java.net.ConnectException;
import java.net.HttpURLConnection;
import java.net.SocketTimeoutException;
import java.net.URL;
import java.net.URLConnection;
import java.util.Locale;
import javax.imageio.IIOImage;
import javax.imageio.ImageIO;
import javax.imageio.ImageWriteParam;
import javax.imageio.ImageWriter;
import javax.imageio.plugins.jpeg.JPEGImageWriteParam;
import javax.imageio.stream.ImageOutputStream;
import org.apache.log4j.Logger;

/**
 * Diese Klasse stellt diverse Methoden bereit für das Erstellen, Bearbeiten, Manipulieren 
 * und Editieren von Grafiken.
 * 
 * 
 * @author DonGyros, heho
 *
 * @version 0.1 19.02.2009
 */
public class UMCImageUtils {

    private Logger log = null;

    /**Gibt die Positionierung "mittig" an*/
    public static final int CENTER = 0;

    /**Gibt die Positionierung "oben links" an*/
    public static final int NORTHWEST = 1;

    /**Gibt die Positionierung "oben rechts" an*/
    public static final int NORTHEAST = 2;

    /**Gibt die Positionierung "unten links" an*/
    public static final int SOUTHEAST = 3;

    /**Gibt die Positionierung "unten rechts" an*/
    public static final int SOUTWEST = 4;

    public UMCImageUtils(Logger aLogger) {
        this.log = aLogger;
    }

    /**
	 * Ein Bild laden.
	 * 
	 * @param ref vollstündiger Pfad zum Bild
	 * @return Das geladene Bild
	 */
    public BufferedImage loadImage(String ref) {
        BufferedImage bimg = null;
        try {
            bimg = ImageIO.read(new File(ref));
        } catch (Exception e) {
            log.error("Bild " + ref + " konnte nicht geladen werden", e);
        }
        return bimg;
    }

    public String exists(String URLName) {
        String url = URLName;
        try {
            HttpURLConnection.setFollowRedirects(false);
            HttpURLConnection con = (HttpURLConnection) new URL(url).openConnection();
            con.setRequestMethod("HEAD");
            con.setConnectTimeout(10000);
            con.setReadTimeout(10000);
            if (con.getResponseCode() == HttpURLConnection.HTTP_MOVED_TEMP) {
                url = con.getHeaderField("Location");
                con = (HttpURLConnection) new URL(url).openConnection();
                con.setConnectTimeout(10000);
                con.setReadTimeout(10000);
                con.setRequestMethod("HEAD");
            }
            if (con.getResponseCode() == HttpURLConnection.HTTP_OK || con.getResponseCode() == HttpURLConnection.HTTP_FORBIDDEN) return url;
            return null;
        } catch (SocketTimeoutException exc) {
            log.error("SocketTimeout: " + url);
            return null;
        } catch (ConnectException ce) {
            log.error("ConnectionTimeout: " + url);
            return null;
        } catch (Exception e) {
            log.error("Check URL " + url, e);
            return null;
        }
    }

    /**
	 * @param url url des zu ladendnen bild
	 * @return bufferedImage bei Erfolg oder null
	 */
    public BufferedImage loadImageFromURL(String url) {
        if (UMCConstants.debug) log.debug("check image url: " + url);
        String downloadURL = exists(url);
        if (downloadURL == null) return null;
        if (UMCConstants.debug) log.debug("loading image from url: " + url);
        long milis = System.currentTimeMillis();
        BufferedImage image = null;
        try {
            URL u = new URL(downloadURL);
            URLConnection con = u.openConnection();
            con.setConnectTimeout(15000);
            con.setReadTimeout(30000);
            con.setUseCaches(true);
            image = ImageIO.read(con.getInputStream());
        } catch (SocketTimeoutException exc) {
            log.error("Download " + url + " Timeout", exc);
            return null;
        } catch (Exception e) {
            log.error("Download " + url + " Fehlgeschlagen", e);
            return null;
        }
        log.info("Download " + url + " in " + (System.currentTimeMillis() - milis) + "ms");
        return image;
    }

    /**
	 * @param url url des zu ladendnen bild
	 * @return bufferedImage bei Erfolg oder null
	 */
    public BufferedImage loadImageFromURL(String url, Dimension d) {
        String downloadURL = exists(url);
        if (downloadURL == null) return null;
        BufferedImage image = null;
        try {
            URL u = new URL(downloadURL);
            URLConnection con = u.openConnection();
            con.setConnectTimeout(15000);
            con.setReadTimeout(30000);
            con.setUseCaches(true);
            BufferedImage imageo = ImageIO.read(con.getInputStream());
            if (imageo == null) return null;
            image = getScaledInstance(imageo, (int) d.getWidth(), (int) d.getHeight(), true);
        } catch (SocketTimeoutException exc) {
            log.error("Download " + url + " Timeout", exc);
            return null;
        } catch (Exception e) {
            log.error("Download " + url + " Fehlgeschlagen", e);
            return null;
        }
        return image;
    }

    public BufferedImage getScaledInstance(BufferedImage img, int width, int height, boolean higherQuality, int anchor) {
        String type = "";
        BufferedImage ret = null;
        double fw = img.getWidth() / (double) width;
        double fh = img.getHeight() / (double) height;
        double sf = Math.min(fw, fh);
        int sclWidth = (int) (img.getWidth() / sf);
        int sclHeight = (int) (img.getHeight() / sf);
        if (img.getWidth() == width && img.getHeight() == height) {
            type = "None";
            ret = img;
        } else if (img.getWidth() >= width && img.getHeight() >= height) {
            type = "downscale";
            ret = downscale(img, sclWidth, sclHeight, higherQuality);
        } else {
            type = "Upscale";
            ret = upscale(img, sclWidth, sclHeight, higherQuality);
        }
        int offsetX = 0;
        int offsetY = 0;
        switch(anchor) {
            case 0:
                offsetX = (int) (width - sclWidth) / 2;
                offsetY = (int) (height - sclHeight) / 2;
                break;
            case 1:
                offsetX = 0;
                offsetY = 0;
                break;
            case 2:
                offsetX = width - sclWidth;
                offsetY = 0;
                break;
            case 3:
                offsetX = width - sclWidth;
                offsetY = height - sclHeight;
                break;
            case 4:
                offsetX = 0;
                offsetY = width - sclHeight;
                break;
        }
        BufferedImage dst = new BufferedImage(width, height, BufferedImage.TYPE_INT_RGB);
        Graphics2D g = dst.createGraphics();
        g.drawImage(ret, offsetX, offsetY, null);
        g.dispose();
        return dst;
    }

    /**
     * Convenience method that returns a scaled instance of the
     * provided {@code BufferedImage}.
     *
     * @param img the original image to be scaled
     * @param targetWidth the desired width of the scaled instance,
     *    in pixels
     * @param targetHeight the desired height of the scaled instance,
     *    in pixels
     * @param hint one of the rendering hints that corresponds to
     *    {@code RenderingHints.KEY_INTERPOLATION} (e.g.
     *    {@code RenderingHints.VALUE_INTERPOLATION_NEAREST_NEIGHBOR},
     *    {@code RenderingHints.VALUE_INTERPOLATION_BILINEAR},
     *    {@code RenderingHints.VALUE_INTERPOLATION_BICUBIC})
     * @param higherQuality if true, this method will use a multi-step
     *    scaling technique that provides higher quality than the usual
     *    one-step technique (only useful in downscaling cases, where
     *    {@code targetWidth} or {@code targetHeight} is
     *    smaller than the original dimensions, and generally only when
     *    the {@code BILINEAR} hint is specified)
     * @return a scaled version of the original {@code BufferedImage}
     */
    public BufferedImage getScaledInstance(BufferedImage img, int width, int height, boolean higherQuality) {
        String type = "";
        BufferedImage ret = null;
        if (img.getWidth() == width && img.getHeight() == height) {
            type = "None";
            ret = img;
        } else if (img.getWidth() >= width && img.getHeight() >= height) {
            type = "downscale";
            ret = downscale(img, width, height, higherQuality);
        } else {
            type = "Upscale";
            ret = upscale(img, width, height, higherQuality);
        }
        return ret;
    }

    private BufferedImage upscale(BufferedImage img, int width, int height, boolean higherQuality) {
        int type = (img.getTransparency() == Transparency.OPAQUE) ? BufferedImage.TYPE_INT_RGB : BufferedImage.TYPE_INT_ARGB;
        BufferedImage tmp = new BufferedImage(width, height, type);
        Graphics2D g2 = tmp.createGraphics();
        g2.setRenderingHint(RenderingHints.KEY_INTERPOLATION, RenderingHints.VALUE_INTERPOLATION_BICUBIC);
        g2.drawImage(img, 0, 0, width, height, null);
        g2.dispose();
        return tmp;
    }

    private BufferedImage downscale(BufferedImage img, int width, int height, boolean higherQuality) {
        int type = (img.getTransparency() == Transparency.OPAQUE) ? BufferedImage.TYPE_INT_RGB : BufferedImage.TYPE_INT_ARGB;
        BufferedImage ret = (BufferedImage) img;
        int targetWidth = width;
        int targetHeight = height;
        int w, h;
        if (higherQuality) {
            w = img.getWidth();
            h = img.getHeight();
        } else {
            w = targetWidth;
            h = targetHeight;
        }
        int halt = 0;
        do {
            halt++;
            if (higherQuality && w >= targetWidth) {
                w /= 2;
                if (w < targetWidth) {
                    w = targetWidth;
                }
            }
            if (higherQuality && h >= targetHeight) {
                h /= 2;
                if (h < targetHeight) {
                    h = targetHeight;
                }
            }
            BufferedImage tmp = new BufferedImage(w, h, type);
            Graphics2D g2 = tmp.createGraphics();
            g2.setRenderingHint(RenderingHints.KEY_INTERPOLATION, RenderingHints.VALUE_INTERPOLATION_BILINEAR);
            g2.drawImage(ret, 0, 0, w, h, null);
            g2.dispose();
            ret = tmp;
        } while (w != targetWidth || h != targetHeight || halt > 5);
        return ret;
    }

    /** 
	 * Saves a BufferedImage to the given file, pathname must not have any 
	 * periods "." in it except for the one before the format, i.e. C:/images/fooimage.png 
	 * @param img 
	 * @param saveFile 
	 */
    public void saveImage(BufferedImage img, String ref) {
        try {
            if (img != null) {
                String format = (ref.endsWith(".png")) ? "png" : "jpg";
                ImageIO.write(img, format, new File(ref));
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    public void saveJPEGImage(BufferedImage img, String name) {
        try {
            int width = img.getWidth(), height = img.getHeight();
            BufferedImage image = new BufferedImage(width, height, BufferedImage.TYPE_INT_RGB);
            int[] pixels = new int[width * height];
            img.getRGB(0, 0, width, height, pixels, 0, width);
            for (int i = pixels.length; --i >= 0; pixels[i] |= 0xff000000) ;
            image.setRGB(0, 0, width, height, pixels, 0, width);
            ImageWriter writer = ImageIO.getImageWritersByFormatName("jpeg").next();
            ImageWriteParam param = new JPEGImageWriteParam(Locale.getDefault());
            param.setCompressionMode(ImageWriteParam.MODE_EXPLICIT);
            param.setCompressionQuality(.90f);
            ImageOutputStream ios = ImageIO.createImageOutputStream(new File(name + ".jpg"));
            writer.setOutput(ios);
            writer.write(null, new IIOImage(image, null, null), param);
            ios.flush();
            writer.dispose();
            ios.close();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    public void saveJPEGImage(BufferedImage img, String name, float quality) {
        try {
            int width = img.getWidth(), height = img.getHeight();
            BufferedImage image = new BufferedImage(width, height, BufferedImage.TYPE_INT_RGB);
            int[] pixels = new int[width * height];
            img.getRGB(0, 0, width, height, pixels, 0, width);
            for (int i = pixels.length; --i >= 0; pixels[i] |= 0xff000000) ;
            image.setRGB(0, 0, width, height, pixels, 0, width);
            ImageWriter writer = ImageIO.getImageWritersByFormatName("jpeg").next();
            ImageWriteParam param = new JPEGImageWriteParam(Locale.getDefault());
            param.setCompressionMode(ImageWriteParam.MODE_EXPLICIT);
            param.setCompressionQuality(quality);
            ImageOutputStream ios = ImageIO.createImageOutputStream(new File(name + ".jpg"));
            writer.setOutput(ios);
            writer.write(null, new IIOImage(image, null, null), param);
            ios.flush();
            writer.dispose();
            ios.close();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    public void savePNGImage(BufferedImage img, String name) {
        try {
            ImageWriter writer = ImageIO.getImageWritersByFormatName("png").next();
            ImageOutputStream ios = ImageIO.createImageOutputStream(new File(name + ".png"));
            writer.setOutput(ios);
            writer.write(null, new IIOImage(img, null, null), null);
            ios.flush();
            writer.dispose();
            ios.close();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    /**
	 * Das übergeben Bild/Cover wird geladen, von der Grüsse an das UMC-Layout angepasst (unter Berücksichtigung der Propotionen),
	 * erweitert und mit einem Verlauf(Grau->Transparenz) versehen.
	 * Dieses neu erzeugte Bild wird als Hintergrundbild für die GUI verwendet werden sobald ein
	 * Scan gestartet wurde.
	 *
	 * @param width Breite des zu erstellenden Bildes
	 * @param height Höhe des zu erstellenden Bildes
	 * @param image vollständiger Pfad zum Bild (Movieposter) das einen Verlauf von grau nach transparent erhalten soll
	 * 
	 * @see MovieFile
	 */
    public BufferedImage createMovieBackground(int width, int height, String image) {
        BufferedImage dimg = null;
        Graphics2D g = null;
        try {
            RenderingHints renderingHints = new RenderingHints(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
            renderingHints.add(new RenderingHints(RenderingHints.KEY_RENDERING, RenderingHints.VALUE_RENDER_QUALITY));
            renderingHints.add(new RenderingHints(RenderingHints.KEY_ALPHA_INTERPOLATION, RenderingHints.VALUE_ALPHA_INTERPOLATION_QUALITY));
            BufferedImage resizedImg = null;
            if (new File(image).exists()) {
                resizedImg = getScaledInstance(loadImage(image), width, height, false, UMCImageUtils.CENTER);
            }
            dimg = new BufferedImage(width, height, BufferedImage.TYPE_INT_ARGB);
            g = dimg.createGraphics();
            g.setRenderingHints(renderingHints);
            LinearGradientPaint grayToTransparent = null;
            if (resizedImg != null) {
                Point2D startPoint = new Point2D.Float(dimg.getWidth() - resizedImg.getWidth(), 0);
                Point2D endPoint = new Point2D.Float(dimg.getWidth(), 0);
                float[] dist = { 0.0f, 1.0f };
                Color[] colors = { new Color(0, 0, 0, 255), new Color(0, 0, 0, 0) };
                grayToTransparent = new LinearGradientPaint(startPoint, endPoint, dist, colors);
            }
            if (resizedImg != null) g.drawImage(resizedImg, null, dimg.getWidth() - resizedImg.getWidth(), 0);
            g.setColor(new Color(40, 41, 59));
            if (resizedImg != null) g.fillRect(0, 0, dimg.getWidth() - resizedImg.getWidth(), dimg.getHeight()); else g.fillRect(0, 0, dimg.getWidth(), dimg.getHeight());
            if (resizedImg != null) {
                g.setRenderingHint(RenderingHints.KEY_ALPHA_INTERPOLATION, RenderingHints.VALUE_ALPHA_INTERPOLATION_QUALITY);
                g.setPaint(grayToTransparent);
                g.fillRect(dimg.getWidth() - resizedImg.getWidth(), 0, resizedImg.getWidth(), dimg.getHeight());
            }
            g.setRenderingHints(renderingHints);
        } catch (Throwable exc) {
            log.warn("Es ist ein Fehler bei der Erstellung des Movidedetail Bildes für " + image + " aufgetreten: " + exc);
        } finally {
            if (g != null) g.dispose();
        }
        return dimg;
    }

    /**
	 * Bild horizontal spiegeln.
	 * 
	 * @param img
	 * @return
	 */
    public BufferedImage horizontalflip(BufferedImage img) {
        int w = img.getWidth();
        int h = img.getHeight();
        BufferedImage dimg = new BufferedImage(w, h, img.getType());
        Graphics2D g = dimg.createGraphics();
        g.drawImage(img, 0, 0, w, h, w, 0, 0, h, null);
        g.dispose();
        return dimg;
    }

    /**
	 * Bild vertikal spiegel.
	 * 
	 * @param img
	 * @return
	 */
    public BufferedImage verticalflip(BufferedImage img) {
        int w = img.getWidth();
        int h = img.getHeight();
        BufferedImage dimg = dimg = new BufferedImage(w, h, img.getColorModel().getTransparency());
        Graphics2D g = dimg.createGraphics();
        g.drawImage(img, 0, 0, w, h, 0, h, w, 0, null);
        g.dispose();
        return dimg;
    }

    /**
	    * Bild drehen/rotieren.
	    * 
	    * @param img
	    * @param angle
	    * @return
	    */
    public BufferedImage rotate(BufferedImage img, int angle) {
        int w = img.getWidth();
        int h = img.getHeight();
        BufferedImage dimg = dimg = new BufferedImage(w, h, img.getType());
        Graphics2D g = dimg.createGraphics();
        g.rotate(Math.toRadians(angle), w / 2, h / 2);
        g.drawImage(img, null, 0, 0);
        return dimg;
    }

    /**
		 * Convert a coloured image to grayscale.
		 * 
		 * @param source The image to be converted
		 * @return
		 */
    public BufferedImage convertToGrayscale(BufferedImage source) {
        BufferedImageOp op = new ColorConvertOp(ColorSpace.getInstance(ColorSpace.CS_GRAY), null);
        return op.filter(source, null);
    }

    /**
		 * Bild tranparent machen.
		 * 
		 * @param url
		 * @param transperancy
		 * @return
		 */
    public BufferedImage loadTranslucentImage(String url, float transperancy) {
        BufferedImage loaded = loadImage(url);
        BufferedImage aimg = new BufferedImage(loaded.getWidth(), loaded.getHeight(), BufferedImage.TRANSLUCENT);
        Graphics2D g = aimg.createGraphics();
        g.setComposite(AlphaComposite.getInstance(AlphaComposite.SRC_OVER, transperancy));
        g.drawImage(loaded, null, 0, 0);
        g.dispose();
        return aimg;
    }

    /**
		 * Eine Farbe transparent machen
		 * @param ref
		 * @param color
		 * @return
		 */
    public BufferedImage makeColorTransparent(String ref, Color color) {
        BufferedImage image = loadImage(ref);
        BufferedImage dimg = new BufferedImage(image.getWidth(), image.getHeight(), BufferedImage.TYPE_INT_ARGB);
        Graphics2D g = dimg.createGraphics();
        g.setComposite(AlphaComposite.Src);
        g.drawImage(image, null, 0, 0);
        g.dispose();
        for (int i = 0; i < dimg.getHeight(); i++) {
            for (int j = 0; j < dimg.getWidth(); j++) {
                if (dimg.getRGB(j, i) == color.getRGB()) {
                    dimg.setRGB(j, i, 0x8F1C1C);
                }
            }
        }
        return dimg;
    }
}
