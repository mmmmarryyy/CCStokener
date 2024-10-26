package com.golden.gamedev.util;

import java.awt.Color;
import java.awt.Font;
import java.awt.FontFormatException;
import java.awt.FontMetrics;
import java.awt.Graphics2D;
import java.awt.Transparency;
import java.awt.image.BufferedImage;
import java.io.IOException;
import java.net.URL;

/**
 * Utility class for manipulating <code>java.awt.Font</code> object.
 */
public class FontUtil {

    private FontUtil() {
    }

    /**
   * Creates java.awt.Font from specified True Type Font URL (*.ttf).
   * 
   * @see com.golden.gamedev.engine.BaseIO#getURL(String)
   * @see com.golden.gamedev.object.GameFontManager#getFont(Font)
   */
    public static Font createTrueTypeFont(URL url, int style, float size) {
        Font f = null;
        try {
            f = Font.createFont(Font.TRUETYPE_FONT, url.openStream());
        } catch (IOException e) {
            System.err.println("ERROR: " + url + " is not found or can not be read");
            f = new Font("Verdana", 0, 0);
        } catch (FontFormatException e) {
            System.err.println("ERROR: " + url + " is not a valid true type font");
            f = new Font("Verdana", 0, 0);
        }
        return f.deriveFont(style, size);
    }

    /**
   * Creates bitmap font from specified java.awt.Font that can be used by
   * {@link com.golden.gamedev.object.font.AdvanceBitmapFont}. <br>
   * The bitmap is created using standard <i>Bitmap Font Writer</i>, created by
   * Stefan Pettersson (http://www.stefan-pettersson.nu).
   * <p>
   * 
   * @return Font images using standard Bitmap Font Writer technique.
   * 
   * @see com.golden.gamedev.object.font.AdvanceBitmapFont
   * @see com.golden.gamedev.object.GameFontManager#getFont(BufferedImage)
   */
    public static BufferedImage createBitmapFont(Font f, Color col) {
        Graphics2D g = ImageUtil.createImage(1, 1).createGraphics();
        FontMetrics fm = g.getFontMetrics(f);
        g.dispose();
        byte[] bytes = new byte[95];
        for (int i = 0; i < bytes.length; i++) {
            bytes[i] = (byte) (32 + i);
        }
        String st = new String(bytes);
        int w = fm.stringWidth(st), h = fm.getHeight(), len = st.length(), x = 0, y = h - fm.getDescent() + 1;
        Color delim = Color.GREEN;
        if (delim.equals(col)) {
            delim = Color.YELLOW;
        }
        BufferedImage bitmap = ImageUtil.createImage(w, h, Transparency.BITMASK);
        g = bitmap.createGraphics();
        g.setFont(f);
        for (int i = 0; i < len; i++) {
            char c = st.charAt(i);
            g.setColor(delim);
            g.drawLine(x, 0, x, 0);
            g.setColor(col);
            g.drawString(String.valueOf(c), x, y);
            x += fm.charWidth(c);
        }
        g.dispose();
        return bitmap;
    }
}
