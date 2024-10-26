package divxtek.tools;

import java.net.*;
import java.awt.event.*;
import java.io.*;
import javax.imageio.*;
import java.awt.image.BufferedImage;
import java.awt.*;
import javax.swing.*;
import divxtek.*;

public class HTMLOperations {

    public static String getSourceFrom(String adresse) {
        String toreturn = null;
        adresse = adresse.replaceAll(" ", "+");
        try {
            URL url = new URL(adresse);
            URLConnection uc = url.openConnection();
            InputStream in = uc.getInputStream();
            int c = in.read();
            StringBuilder build = new StringBuilder();
            while (c != -1) {
                build.append((char) c);
                c = in.read();
            }
            toreturn = build.toString();
        } catch (MalformedURLException e) {
            e.printStackTrace();
        } catch (IOException e) {
            e.printStackTrace();
        }
        return toreturn;
    }

    public static ImageIcon extractImageFrom(String urlImage, String title) {
        Image image = null;
        try {
            URL url = new URL(urlImage);
            image = ImageIO.read(url);
            System.out.println("image lue");
        } catch (MalformedURLException e) {
            System.out.println("URL plante");
        } catch (IOException e) {
        }
        BufferedImage rendImage = toBufferedImage(image);
        return new ImageIcon(rendImage);
    }

    public static BufferedImage toBufferedImage(Image image) {
        if (image instanceof BufferedImage) return ((BufferedImage) image); else {
            image = new ImageIcon(image).getImage();
            if (image.getHeight(null) <= 0 || image.getWidth(null) <= 0) return null;
            BufferedImage bufferedImage = new BufferedImage(image.getWidth(null), image.getHeight(null), BufferedImage.TYPE_INT_RGB);
            Graphics g = bufferedImage.createGraphics();
            g.drawImage(image, 0, 0, null);
            g.dispose();
            return (bufferedImage);
        }
    }
}
