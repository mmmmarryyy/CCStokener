package com.bluebrim.font.test;

import javax.swing.*;
import java.awt.*;
import javax.swing.text.*;
import java.net.*;
import java.io.*;
import java.util.*;

public class FontTestType1 extends JFrame {

    private String fontURL = null;

    private TreeMap fontMap = new TreeMap();

    public FontTestType1(String fontURL) {
        setSize(400, 400);
        this.fontURL = fontURL;
        System.out.println("Trying to use " + fontURL + " ...");
        Font font = getFont(fontURL);
        fontMap.put(font.getFamily(), font);
        StyleContext sc = new StyleContext();
        Style style = sc.addStyle("Paragraph Style", null);
        StyleConstants.setFontFamily(style, font.getFamily());
        StyleConstants.setFontSize(style, 22);
        FontDocument doc = new FontDocument(sc);
        doc.setLogicalStyle(0, style);
        JTextPane tp = new JTextPane(doc);
        Container c = getContentPane();
        c.add(tp, BorderLayout.CENTER);
    }

    /**
     * * Method that will load a font file from an URL and return a Font object *
     * 
     * @return the Font object
     */
    public Font getFont(String urlToFont) {
        Font testFont = null;
        try {
            InputStream inps = (new URL(urlToFont)).openStream();
            testFont = Font.createFont(Font.TRUETYPE_FONT, inps);
        } catch (FontFormatException ffe) {
            ffe.printStackTrace();
        } catch (IOException ioe) {
            JOptionPane.showMessageDialog(null, "Could not load font - " + urlToFont, "Unable to load font", JOptionPane.WARNING_MESSAGE);
        } catch (Exception e) {
            e.printStackTrace();
        }
        return testFont;
    }

    class FontDocument extends DefaultStyledDocument {

        public FontDocument(StyleContext sc) {
            super(sc);
        }

        public FontDocument() {
            super();
        }

        public Font getFont(AttributeSet attr) {
            String fontFamily = StyleConstants.getFontFamily(attr);
            if (fontMap.containsKey(fontFamily)) {
                float size = StyleConstants.getFontSize(attr);
                Font f = ((Font) fontMap.get(fontFamily)).deriveFont(size);
                return f;
            } else return super.getFont(attr);
        }
    }

    public static void main(String args[]) {
        FontTestType1 f = new FontTestType1("http://www.cs.umb.edu/~bill/java/tools/frame-6.0/fminit/fontdir/TimesNewRoman-Bold.pfa");
        f.show();
    }
}
