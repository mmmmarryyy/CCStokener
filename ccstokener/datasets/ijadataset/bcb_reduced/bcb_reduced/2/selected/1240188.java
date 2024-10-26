package org.opensourcearcade.jinvaders;

import java.awt.Color;
import java.awt.Font;
import java.awt.FontMetrics;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.GraphicsConfiguration;
import java.awt.GraphicsEnvironment;
import java.awt.image.BufferedImage;
import java.io.BufferedReader;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.io.InputStream;
import java.net.URL;
import javax.imageio.ImageIO;

public final class ToolBox {

    public static final String RES_DIR = "/res/";

    public static final ToolBox SELF = new ToolBox();

    public static URL getURL(String path) throws FileNotFoundException {
        URL url = SELF.getClass().getResource(RES_DIR + path);
        if (url == null) throw new FileNotFoundException(RES_DIR + path);
        return url;
    }

    public static boolean createHomeDirectory() {
        if (runningAsApplet()) return false;
        String homeDir = System.getProperty("user.home");
        String fileSp = System.getProperty("file.separator");
        boolean success = false;
        try {
            String dir = getPackageName();
            File file = new File(homeDir + fileSp + '.' + dir);
            success = (file.exists()) ? true : file.mkdir();
        } catch (Exception e) {
            System.err.println("Unable to create home directory " + e.getLocalizedMessage());
        }
        return success;
    }

    public static boolean saveGame(String content) {
        if (runningAsApplet()) return false;
        String fileSp = getFileSeparator();
        String name = getPackageName();
        String file = getHomeDir() + fileSp + '.' + name + fileSp + name;
        FileWriter writer = null;
        try {
            writer = new FileWriter(file);
            try {
                writer.write(content);
            } finally {
                writer.close();
            }
        } catch (Exception e) {
            System.err.println(e.getLocalizedMessage());
            return false;
        }
        return true;
    }

    public static String loadGame() {
        if (runningAsApplet()) return null;
        String fileSp = getFileSeparator();
        String name = getPackageName();
        String file = getHomeDir() + fileSp + '.' + name + fileSp + name;
        String result = null;
        BufferedReader reader = null;
        try {
            reader = new BufferedReader(new FileReader(file));
            try {
                result = reader.readLine();
            } finally {
                reader.close();
            }
        } catch (Exception e) {
            System.err.println(e.getLocalizedMessage());
        }
        return result;
    }

    public static String getHomeDir() {
        if (runningAsApplet()) return null;
        return System.getProperty("user.home");
    }

    public static String getFileSeparator() {
        if (runningAsApplet()) return null;
        return System.getProperty("file.separator");
    }

    public static BufferedImage loadImage(URL url) throws IOException {
        BufferedImage img = ImageIO.read(url);
        return convertToCompatibleImage(img);
    }

    public static void drawImage(Graphics g, BufferedImage image, int x, int y, int frame) {
        int iw = image.getWidth() / 3;
        int ih = image.getHeight();
        g.drawImage(image, x, y, x + iw, y + ih, iw * frame, 0, iw, ih, null);
    }

    public static void drawImageCentered(Graphics g, BufferedImage image, int x, int y, int frame) {
        int iw = image.getWidth() / 3;
        int ih = image.getHeight();
        g.drawImage(image, x - iw / 2, y - ih / 2, x + iw / 2, y + ih / 2, iw * frame, 0, iw, ih, null);
    }

    public static BufferedImage convertToCompatibleImage(BufferedImage img) {
        BufferedImage result = null;
        try {
            GraphicsEnvironment ge = GraphicsEnvironment.getLocalGraphicsEnvironment();
            GraphicsConfiguration gc = ge.getDefaultScreenDevice().getDefaultConfiguration();
            int transparency = img.getColorModel().getTransparency();
            result = gc.createCompatibleImage(img.getWidth(), img.getHeight(), transparency);
            Graphics2D g2d = result.createGraphics();
            g2d.drawImage(img, 0, 0, null);
            g2d.dispose();
        } catch (Exception e) {
            result = img;
        }
        return result;
    }

    public static Font loadFont(URL url) {
        Font font;
        try {
            InputStream is = url.openStream();
            font = Font.createFont(Font.TRUETYPE_FONT, is).deriveFont(24f);
            is.close();
        } catch (Exception e) {
            font = new Font("arial", Font.PLAIN, 12);
            System.err.println("Unable to load font: " + url.getPath());
        }
        return font;
    }

    public static boolean checkCollision(Entity e1, Entity e2) {
        int rx1 = (int) e1.x;
        int rx2 = rx1 + e1.w;
        int ry1 = (int) e1.y;
        int ry2 = ry1 + e1.h;
        return ((int) e2.x <= rx2 && rx1 <= (int) e2.x + e2.w && (int) e2.y <= ry2 && ry1 <= (int) e2.y + e2.h);
    }

    public static void drawText(Graphics g, String text, int x, int y, Color color) {
        FontMetrics metrics = g.getFontMetrics(g.getFont());
        int w = metrics.stringWidth(text);
        int h = metrics.getHeight();
        Color c = g.getColor();
        g.setColor(color);
        g.drawString(text, x - w / 2, y - h / 2);
        if (c != null) g.setColor(c);
    }

    public static void drawText(Graphics g, String text, int y, Color color) {
        drawText(g, text, Game.WIDTH / 2, y, color);
    }

    public static String getPackageName() {
        String pkg = SELF.getClass().getPackage().getName();
        return pkg.substring(pkg.lastIndexOf('.') + 1);
    }

    public static boolean runningAsApplet() {
        return System.getSecurityManager() != null;
    }
}
