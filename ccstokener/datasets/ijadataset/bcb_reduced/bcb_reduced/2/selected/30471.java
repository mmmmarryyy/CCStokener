package jorgan.swing;

import java.awt.Font;
import java.io.IOException;
import java.io.InputStream;
import java.lang.ref.Reference;
import java.lang.ref.SoftReference;
import java.net.URL;
import java.util.HashMap;
import java.util.Map;

/**
 * A cache of fonts.
 */
public class FontCache {

    private static Map<String, Reference<Font>> fonts = new HashMap<String, Reference<Font>>();

    private static Font get(String key) {
        Reference<Font> reference = fonts.get(key);
        if (reference != null) {
            return reference.get();
        }
        return null;
    }

    private static void put(String key, Font font) {
        fonts.put(key, new SoftReference<Font>(font));
    }

    /**
	 * Flush all cached fonts.
	 */
    public static void flush() {
        fonts.clear();
    }

    public static Font getFont(URL url) {
        String key = url.toString();
        Font font = get(key);
        if (font == null) {
            font = readFont(url);
            put(key, font);
        }
        return font;
    }

    private static Font readFont(URL url) {
        Font font = null;
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
        return font;
    }
}
