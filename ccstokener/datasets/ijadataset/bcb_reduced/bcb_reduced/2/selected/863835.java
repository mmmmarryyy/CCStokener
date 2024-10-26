package com.jdiv.extensions;

import java.awt.Color;
import java.awt.GraphicsConfiguration;
import java.awt.GraphicsEnvironment;
import java.awt.image.BufferedImage;
import java.io.BufferedInputStream;
import java.io.DataInputStream;
import java.io.FileInputStream;
import java.io.InputStream;
import java.net.URL;
import java.nio.channels.FileChannel;
import java.util.ArrayList;
import com.jdiv.JDiv;
import com.jdiv.util.MaskColorImage;
import com.jdiv.util.UnsignedInteger;

/**
 * @author  Joyal
 */
public class JFpg {

    private ArrayList<JFpgImage> img = new ArrayList<JFpgImage>();

    private DataInputStream in;

    private BufferedImage bmpPaleta;

    /**
	 * @uml.property  name="fpgType"
	 */
    private String fpgType;

    private Color maskColor = new Color(0, 0, 0);

    public JFpg() {
        JDiv.fpgs.add(this);
    }

    public JFpg(String archivo) {
        JDiv.fpgs.add(this);
        loadFpg(archivo);
    }

    public JFpg(URL url) {
        JDiv.fpgs.add(this);
        loadFpg(url);
    }

    public BufferedImage getImage(int code) {
        BufferedImage tmpImg = null;
        for (int i = 0; i < img.size(); i++) {
            if (img.get(i).getCodigo() == code) {
                tmpImg = img.get(i).getImagen();
                break;
            }
        }
        return tmpImg;
    }

    public JFpgImage getFpgImage(int code) {
        return img.get(code);
    }

    public ArrayList<JFpgImage> getFpg() {
        return img;
    }

    /**
	 * @return
	 * @uml.property  name="fpgType"
	 */
    public String getFpgType() {
        return fpgType;
    }

    public int getSize() {
        return img.size();
    }

    public void loadFpg(URL url) {
        try {
            InputStream fin = url.openStream();
            in = new DataInputStream(new BufferedInputStream(fin));
            readFile();
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    public void loadFpg(String archivo) {
        try {
            FileInputStream file = new FileInputStream(archivo);
            in = new DataInputStream(new BufferedInputStream(file));
            readFile();
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    public void readFile() {
        byte[] type = new byte[3];
        byte[] version = new byte[5];
        byte[] palette = new byte[768];
        byte[] gamma = new byte[576];
        byte[] name = new byte[32];
        byte[] description = new byte[12];
        try {
            UnsignedInteger uint = new UnsignedInteger(in);
            in.read(type);
            in.read(version);
            fpgType = new String(type);
            if (fpgType.equals("fpg")) {
                in.read(palette);
                in.read(gamma);
                drawPalette(palette);
            }
            int rgbInt = 0, r = 0, g = 0, b = 0;
            ArrayList<JFlag> pControl = null;
            while (in.available() != 0) {
                int cod = uint.readUnsignedInt();
                int tam = uint.readUnsignedInt();
                in.read(name);
                in.read(description);
                int an = uint.readUnsignedInt();
                int al = uint.readUnsignedInt();
                int fl = uint.readUnsignedInt();
                String nom = new String(name);
                String des = new String(description);
                if (fl > 0) {
                    pControl = new ArrayList<JFlag>();
                    for (int i = 0; i < fl; i++) {
                        byte[] bx = new byte[2];
                        byte[] by = new byte[2];
                        int x, y;
                        in.read(bx);
                        in.read(by);
                        x = bx[0] + bx[1];
                        y = by[0] + by[1];
                        if (x >= 0 && y >= 0) {
                            JFlag flag = new JFlag(x, y);
                            pControl.add(flag);
                        }
                    }
                }
                GraphicsConfiguration CONFIG = GraphicsEnvironment.getLocalGraphicsEnvironment().getDefaultScreenDevice().getDefaultConfiguration();
                BufferedImage bmp = CONFIG.createCompatibleImage(an, al, BufferedImage.TYPE_INT_RGB);
                if (fpgType.equals("fpg")) {
                    byte[] datos = new byte[an * al];
                    in.read(datos);
                    for (int i = 0; i < al; i++) {
                        for (int j = 0; j < an; j++) {
                            int posPix = datos[j + (i * an)] & 0xFF;
                            r = palette[posPix * 3] & 0xFF;
                            g = palette[(posPix * 3) + 1] & 0xFF;
                            b = palette[(posPix * 3) + 2] & 0xFF;
                            rgbInt = 256 * 256 * r + 256 * g + b;
                            bmp.setRGB(j, i, rgbInt);
                        }
                    }
                } else {
                    for (int i = 0; i < al; i++) {
                        for (int j = 0; j < an; j++) {
                            int byte1 = in.readUnsignedByte();
                            int byte2 = in.readUnsignedByte();
                            int s = byte2 * 256 + byte1;
                            r = (((s >> 11) * 255) / 31) & 0xFF;
                            g = ((((s << 21) >> 26) * 255) / 63) & 0xFF;
                            b = ((((s << 27) >> 27) * 255) / 31) & 0xFF;
                            if (fpgType.equals("f16")) rgbInt = 256 * 256 * r + 256 * g + b; else if (fpgType.equals("c16")) rgbInt = 256 * 256 * b + 256 * g + r;
                            bmp.setRGB(j, i, rgbInt);
                        }
                    }
                }
                BufferedImage image = MaskColorImage.maskImage(bmp);
                img.add(new JFpgImage(image, cod, tam, des, nom, an, al, fl, pControl));
            }
            in.close();
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    private void drawPalette(byte paleta[]) {
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

    public BufferedImage getPaleta() {
        return bmpPaleta;
    }
}
