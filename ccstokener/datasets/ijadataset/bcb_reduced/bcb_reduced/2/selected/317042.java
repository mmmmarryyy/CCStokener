package jdvi.font;

import java.awt.*;
import java.awt.image.*;
import java.awt.event.*;
import java.applet.*;
import java.util.*;
import java.io.*;
import java.net.*;
import javatex.Frame1;

/**
 * This class capsules the information from a pk font file. It knows
 * how to parse a .pk file.
 * @author (2000) Tim Hoffmann
 */
public final class Font implements Serializable {

    public String name;

    public double size;

    public Glyph chars[];

    public int ds;

    public int cs;

    public int hppp;

    public int vppp;

    public double conv_fac;

    public Font() {
        chars = new Glyph[256];
    }

    public Font(String path, String fontName) {
        this();
        StringTokenizer tok = new StringTokenizer(path, ";");
        NybbleInputStream str = null;
        while (str == null & tok.hasMoreTokens()) {
            try {
                URL url = new URL(tok.nextToken() + "/");
                url = new URL(url, fontName);
                System.out.println(url.toString());
                str = new NybbleInputStream(url.openStream());
                parsePkStream(str);
                str.close();
                name = fontName;
            } catch (java.io.IOException e) {
            }
        }
    }

    public static synchronized Font loadFont(String path, String fontName) {
        Font f = null;
        StringTokenizer tok = new StringTokenizer(path, ";");
        NybbleInputStream str = null;
        if (tok.hasMoreTokens()) tok.nextToken();
        while (str == null && tok.hasMoreTokens()) {
            try {
                String bla = tok.nextToken();
                URL url = new URL(bla);
                url = new URL("file", "localhost", url.getFile() + fontName);
                str = new NybbleInputStream(url.openStream());
            } catch (java.io.IOException e) {
                Frame1.writelog(e.toString());
            }
        }
        if (str == null) {
            f = new Font();
            InputStream istr = f.getClass().getResourceAsStream(fontName + ".123");
            if (istr != null) str = new NybbleInputStream(istr);
        }
        if (str != null) {
            if (f == null) f = new Font();
            try {
                f.parsePkStream(str);
                str.close();
            } catch (java.io.IOException e) {
            }
            return f;
        }
        return null;
    }

    public Font(URL name) throws java.io.IOException {
        this();
        NybbleInputStream str = new NybbleInputStream(name.openStream());
        parsePkStream(str);
        str.close();
    }

    private void parsePkStream(NybbleInputStream s) throws java.io.IOException {
        int header = s.read();
        int id = s.read();
        if ((header != 247) | (id != 89)) {
            System.err.println("this is no pk file!");
            return;
        }
        id = s.read();
        byte idchars[] = new byte[id];
        s.read(idchars);
        ds = s.read4();
        cs = s.read4();
        hppp = s.read4();
        conv_fac = ds / 72057594037927936.0 * hppp;
        vppp = s.read4();
        header = s.read();
        while (header != 245) {
            if (header < 240) {
                if ((header & 7) > 3) {
                    if ((header & 7) > 6) {
                        int pl = s.readInt();
                        Glyph g = Glyph.readGlyph(s, header);
                        chars[g.cc] = g;
                    } else {
                        int pl = (header % 4) * 65536 + s.readUnsignedShort();
                        Glyph g = Glyph.readGlyph(s, header);
                        chars[g.cc] = g;
                    }
                } else {
                    int pl = (header % 4) * 256 + s.read();
                    Glyph g = Glyph.readGlyph(s, header);
                    chars[g.cc] = g;
                }
            }
            if (header == 240) {
                id = s.read();
                idchars = new byte[id];
                s.read(idchars);
            }
            if (header == 244) {
                s.read4();
            }
            if (header == 246) {
            }
            header = s.read();
        }
    }

    /**
     * The <code>flush</code> method is used to tell all glyphs in teh
     * font to disgard their cached images. This is a Internet
     * Explorer workaround since IE only stops applets on pages you
     * leave but creates a new instance if you go back to them And
     * since IE security manager throws exeptions if there are too
     * many images around, we throw the cached images away whenever
     * the applet gets stopped.
     *
     */
    public void flush() {
        for (int i = 0; i < 256; i++) if (chars[i] != null) chars[i].flush();
    }
}
