package com.tiny.core.resources;

import java.awt.Font;
import java.net.URL;

/**
 * Carrega e faz cahce de recursos de font para o jogo
 * 
 * @author Erick Zanardo
 * 
 */
public class FontManager extends ResourceManager<Font> {

    public static final int PLAIN = 0;

    public static final int BOLD = 1;

    public static final int ITALIC = 2;

    public static final int BOLD_ITALIC = BOLD | ITALIC;

    public Font loadFont(String fileName, int size, int style) {
        Font font = load(fileName);
        font = font.deriveFont((float) size);
        if ((style & BOLD) == BOLD) {
            font = font.deriveFont(Font.BOLD);
        }
        if ((style & ITALIC) == ITALIC) {
            font = font.deriveFont(Font.ITALIC);
        }
        return font;
    }

    @Override
    protected Font processUrl(URL url) {
        try {
            return Font.createFont(Font.TRUETYPE_FONT, url.openStream());
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }
}
