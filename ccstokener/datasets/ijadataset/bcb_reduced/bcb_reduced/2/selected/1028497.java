package jorgan.skin;

import java.awt.Font;
import java.io.IOException;
import java.io.InputStream;
import java.net.URL;
import java.util.HashMap;
import java.util.Map;

/**
 * A cache of fonts.
 */
public class FontCache {

    private static Map<URL, Font> fonts = new HashMap<URL, Font>();

    /**
	 * Flush all cached fonts.
	 */
    public static void flush() {
        fonts.clear();
    }

    /**
	 * Get an image for the given URL.
	 * 
	 * @param url
	 *            url to get image for
	 * @return image
	 */
    public static Font getFont(URL url) {
        if (url == null) {
            throw new IllegalArgumentException("url must not be null");
        }
        Font font = fonts.get(url);
        if (font == null) {
            try {
                InputStream input = url.openStream();
                try {
                    font = Font.createFont(Font.TRUETYPE_FONT, input);
                } finally {
                    try {
                        input.close();
                    } catch (IOException ignore) {
                    }
                }
            } catch (Exception e) {
            }
            fonts.put(url, font);
        }
        return font;
    }
}
