package org.hfbk.util;

import java.awt.Image;
import java.awt.image.BufferedImage;
import java.net.URL;
import javax.imageio.ImageIO;
import javax.imageio.stream.MemoryCacheImageInputStream;
import org.hfbk.vis.Prefs;

/** universal ImageLoader and thumbnailer. */
public class ImageLoader {

    public static ImageLoader defaultLoader = new ImageLoader();

    /**
	 * @param url
	 *            the url to fetch from.
	 * @return an image if found, or null if it could not be fetched.
	 */
    public BufferedImage getImg(String imageUrl) {
        ImageIO.setUseCache(false);
        if (imageUrl == null) return null;
        BufferedImage img = null;
        try {
            if (!imageUrl.matches("http.*")) imageUrl = "file:" + imageUrl;
            URL url = new URL(imageUrl);
            img = ImageIO.read(new MemoryCacheImageInputStream(url.openStream()));
        } catch (Exception e) {
            if (Prefs.current.verbose) System.out.println("ImageLoader: couldnt read " + imageUrl + ", for reason:" + e);
        }
        return img;
    }

    /**
	 * @param iconfile
	 * @return
	 */
    public static Image getRessourceImage(String iconfile) {
        try {
            return ImageIO.read(new MemoryCacheImageInputStream(ClassLoader.getSystemResourceAsStream(iconfile)));
        } catch (Exception e) {
            System.out.println("ImageLoader: couldnt read " + iconfile + ", for reason:" + e);
            return null;
        }
    }

    public String thumbUrl(String url) {
        int slash = url.lastIndexOf('/');
        if (slash == -1) slash = url.lastIndexOf('\\');
        return url.substring(0, slash + 1) + "thumbs" + url.substring(slash);
    }

    public Image getCachedThumbnail(String url) {
        if (!url.matches("http.*")) return getImg(thumbUrl(url)); else return null;
    }

    public Image getThumbnail(String url) {
        BufferedImage img = null;
        img = getImg(url);
        if (img == null) return null;
        float w = img.getWidth(), h = img.getHeight();
        if (w * h < Prefs.current.thumbpixels) return img; else {
            double scale = Math.sqrt(Prefs.current.thumbpixels / (w * h));
            return img.getScaledInstance((int) (w * scale), (int) (h * scale), BufferedImage.SCALE_FAST);
        }
    }
}
