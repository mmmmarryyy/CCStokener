package com.jdiv.extensions;

import java.util.ArrayList;
import java.awt.Color;
import java.awt.image.BufferedImage;
import java.io.BufferedInputStream;
import java.io.DataInputStream;
import java.io.FileInputStream;
import java.io.InputStream;
import java.net.URL;
import com.jdiv.util.MaskColorImage;
import com.jdiv.util.UnsignedInteger;

public class JFnt {

    private DataInputStream in;

    private BufferedImage bmpPaleta;

    private ArrayList<JFontImage> infoFont = new ArrayList<JFontImage>();

    public JFnt() {
    }

    public JFnt(String archivo) {
        loadFnt(archivo);
    }

    public JFnt(URL archivo) {
        loadFnt(archivo);
    }

    public void loadFnt(URL url) {
        byte[] file_type = new byte[3];
        byte[] fnt_code = new byte[5];
        byte[] paleta = new byte[768];
        byte[] igno = new byte[576];
        try {
            infoFont.clear();
            InputStream file = url.openStream();
            in = new DataInputStream(new BufferedInputStream(file));
            UnsignedInteger uint = new UnsignedInteger(in);
            in.read(file_type);
            in.read(fnt_code);
            in.read(paleta);
            in.read(igno);
            int flags = uint.readUnsignedInt();
            dibujaPaleta(paleta);
            for (int i = 0; i < 256; i++) {
                JFontImage header = new JFontImage();
                header.setAncho(uint.readUnsignedInt());
                header.setAlto(uint.readUnsignedInt());
                header.setVoffset(uint.readUnsignedInt());
                header.setOffset(uint.readUnsignedInt());
                header.setTamano(header.getAncho() * header.getAlto());
                infoFont.add(header);
            }
            int r, g, b, rgbInt;
            MaskColorImage.setMaskColor(new Color(0, 0, 0));
            for (int i = 0; i < 256; i++) {
                DataInputStream in2 = new DataInputStream(new BufferedInputStream(url.openStream()));
                JFontImage h = infoFont.get(i);
                byte[] skip = new byte[h.getOffset()];
                byte[] datos = new byte[h.getTamano()];
                in2.skip(h.getOffset());
                in2.read(datos);
                int tipo = BufferedImage.TYPE_INT_RGB;
                BufferedImage bmp;
                if (h.getAncho() == 0 && h.getAlto() == 0) bmp = new BufferedImage(1, 1, tipo); else bmp = new BufferedImage(h.getAncho(), h.getAlto(), tipo);
                int pos = 0, pX = 0, pY = 0;
                for (int j = 0; j < h.getTamano(); j++) {
                    pos = datos[j] & 0xFF;
                    r = paleta[pos * 3] & 0xFF;
                    g = paleta[(pos * 3) + 1] & 0xFF;
                    b = paleta[(pos * 3) + 2] & 0xFF;
                    if (pX == h.getAncho()) {
                        pY++;
                        pX = 0;
                    }
                    rgbInt = 256 * 256 * r + 256 * g + b;
                    bmp.setRGB(pX, pY, rgbInt);
                    pX++;
                }
                h.setImgFont(MaskColorImage.maskImage(bmp));
            }
            file.close();
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    public void loadFnt(String archivo) {
        byte[] file_type = new byte[3];
        byte[] fnt_code = new byte[5];
        byte[] paleta = new byte[768];
        byte[] igno = new byte[576];
        try {
            infoFont.clear();
            FileInputStream file = new FileInputStream(archivo);
            in = new DataInputStream(new BufferedInputStream(file));
            UnsignedInteger uint = new UnsignedInteger(in);
            in.read(file_type);
            in.read(fnt_code);
            in.read(paleta);
            in.read(igno);
            int flags = uint.readUnsignedInt();
            dibujaPaleta(paleta);
            for (int i = 0; i < 256; i++) {
                JFontImage header = new JFontImage();
                header.setAncho(uint.readUnsignedInt());
                header.setAlto(uint.readUnsignedInt());
                header.setVoffset(uint.readUnsignedInt());
                header.setOffset(uint.readUnsignedInt());
                header.setTamano(header.getAncho() * header.getAlto());
                infoFont.add(header);
            }
            int r, g, b, rgbInt;
            MaskColorImage.setMaskColor(new Color(0, 0, 0));
            for (int i = 0; i < 256; i++) {
                DataInputStream in2 = new DataInputStream(new BufferedInputStream(new FileInputStream(archivo)));
                JFontImage h = infoFont.get(i);
                byte[] skip = new byte[h.getOffset()];
                byte[] datos = new byte[h.getTamano()];
                in2.skip(h.getOffset());
                in2.read(datos);
                int tipo = BufferedImage.TYPE_INT_RGB;
                BufferedImage bmp;
                if (h.getAncho() == 0 && h.getAlto() == 0) bmp = new BufferedImage(1, 1, tipo); else bmp = new BufferedImage(h.getAncho(), h.getAlto(), tipo);
                int pos = 0, pX = 0, pY = 0;
                for (int j = 0; j < h.getTamano(); j++) {
                    pos = datos[j] & 0xFF;
                    r = paleta[pos * 3] & 0xFF;
                    g = paleta[(pos * 3) + 1] & 0xFF;
                    b = paleta[(pos * 3) + 2] & 0xFF;
                    if (pX == h.getAncho()) {
                        pY++;
                        pX = 0;
                    }
                    rgbInt = 256 * 256 * r + 256 * g + b;
                    bmp.setRGB(pX, pY, rgbInt);
                    pX++;
                }
                h.setImgFont(MaskColorImage.maskImage(bmp));
            }
            file.close();
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    public BufferedImage getPaleta() {
        return bmpPaleta;
    }

    public ArrayList<JFontImage> getFnt() {
        return infoFont;
    }

    public JFontImage getFont(int ascii) {
        return infoFont.get(ascii);
    }

    public BufferedImage getImgFont(int ascii) {
        return infoFont.get(ascii).getImgFont();
    }

    private void dibujaPaleta(byte paleta[]) {
        int tipo, r, g, b, rgbInt, yy = 0, xx = 0;
        tipo = BufferedImage.TYPE_INT_RGB;
        bmpPaleta = new BufferedImage(325, 350, tipo);
        for (int i = 0; i < 768; i++) {
            paleta[i] = (byte) (paleta[i] << 2);
        }
        for (int i = 0; i < 256; i++) {
            r = paleta[i * 3] & 0xFF;
            g = paleta[(i * 3) + 1] & 0xFF;
            b = paleta[(i * 3) + 2] & 0xFF;
            rgbInt = 256 * 256 * r + 256 * g + b;
            if (xx == 16) {
                xx = 0;
                yy++;
            }
            for (int y = yy * 20; y < ((20 * yy) + 20); ) {
                for (int x = xx * 20; x < ((20 * xx) + 20); x++) {
                    bmpPaleta.setRGB(x, y, rgbInt);
                }
                y++;
            }
            xx++;
        }
    }
}
