package xkcd;

import java.awt.Graphics;
import java.awt.image.BufferedImage;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;
import java.io.PrintWriter;
import java.net.MalformedURLException;
import java.net.URL;
import java.util.Scanner;
import javax.imageio.ImageIO;

public class main {

    public static final String website = "http://xkcd.com";

    public static final String website2 = "http://www.smbc-comics.com/";

    public static final String pic_folder = "/home/ladanz/Pictures/BG/xkcd/";

    public static final String pic_folder2 = "/home/ladanz/Pictures/BG/smbc/";

    public static void main(String[] args) {
        String loc_img = saveLatestPic();
        String loc_img2 = saveLatestPic2();
        if (loc_img != null) {
            File infofile = new File("/home/ladanz/.config/xfce4/desktop/hintergrundbilder.list");
            PrintWriter pw;
            try {
                pw = new PrintWriter(infofile);
                pw.println("# xfce backdrop list");
                pw.println(loc_img);
                if (loc_img2 != null) pw.println(loc_img2);
                pw.close();
            } catch (FileNotFoundException e) {
                e.printStackTrace();
            }
            try {
                Runtime.getRuntime().exec("xfdesktop");
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
    }

    /**
	 * 
	 * @return [img_url]
	 */
    private static String saveLatestPic() {
        try {
            URL url = new URL(website);
            InputStream is = (url.openStream());
            Scanner sc = new Scanner(is);
            String[] t = getImagePath(sc);
            String img_path = t[0];
            String title = t[1];
            System.out.println("ImagePath= " + img_path);
            String filename = img_path.split("/")[img_path.split("/").length - 1];
            System.out.println("Filename: " + filename);
            File loc = new File(pic_folder + filename);
            if (!loc.exists()) {
                loc.createNewFile();
                BufferedImage bi = ImageIO.read(new URL(img_path));
                double widfac = 1150. / bi.getWidth();
                double heifac = 700. / bi.getHeight();
                double fac = Math.min(1, Math.min(widfac, heifac));
                BufferedImage bic = new BufferedImage((int) (bi.getWidth() * fac), (int) (bi.getHeight() * fac) + 40, bi.getType());
                Graphics g = bic.getGraphics();
                g.drawImage(bi, 0, 0, (int) (bi.getWidth() * fac), (int) (bi.getHeight() * fac), null);
                int x = ((int) (bi.getWidth() * fac) - title.length() * 6) / 2;
                g.drawString(title, x, (int) (bi.getHeight() * fac) + 20);
                ImageIO.write(bic, "png", ImageIO.createImageOutputStream(loc));
            }
            return loc.getAbsolutePath();
        } catch (MalformedURLException e) {
            e.printStackTrace();
        } catch (IOException e) {
            e.printStackTrace();
        }
        return null;
    }

    private static String saveLatestPic2() {
        try {
            URL url = new URL(website2);
            InputStream is = (url.openStream());
            Scanner sc = new Scanner(is);
            String img_path = website2 + getImagePath2(sc);
            System.out.println("ImagePath= " + img_path);
            String filename = img_path.split("/")[img_path.split("/").length - 1];
            System.out.println("Filename: " + filename);
            File loc = new File(pic_folder2 + filename);
            if (!loc.exists()) {
                loc.createNewFile();
                BufferedImage bi = ImageIO.read(new URL(img_path));
                double widfac = 1150. / bi.getWidth();
                double heifac = 700. / bi.getHeight();
                double fac = Math.min(1, Math.min(widfac, heifac));
                BufferedImage bic = new BufferedImage((int) (bi.getWidth() * fac), (int) (bi.getHeight() * fac), bi.getType());
                Graphics g = bic.getGraphics();
                g.drawImage(bi, 0, 0, (int) (bi.getWidth() * fac), (int) (bi.getHeight() * fac), null);
                ImageIO.write(bic, "png", ImageIO.createImageOutputStream(loc));
            }
            return loc.getAbsolutePath();
        } catch (MalformedURLException e) {
            e.printStackTrace();
        } catch (IOException e) {
            e.printStackTrace();
        }
        return null;
    }

    /**
	 * 
	 * @param sc
	 * @return [img_url,title]
	 */
    private static String[] getImagePath(Scanner sc) {
        String pat = "<img src=";
        int patt_count = 0;
        String erg[] = new String[2];
        while (sc.hasNext()) {
            String s = sc.nextLine();
            if (s.contains(pat)) {
                patt_count++;
                if (patt_count > 1) {
                    erg[0] = s;
                    break;
                }
            }
        }
        if (erg != null) {
            erg[1] = erg[0].split("\"")[3];
            erg[0] = erg[0].split("\"")[1];
        }
        return erg;
    }

    private static String getImagePath2(Scanner sc) {
        String pat = "<img src=\"/comics";
        int patt_count = 1;
        String erg = null;
        while (sc.hasNext()) {
            String s = sc.nextLine();
            if (s.contains(pat)) {
                patt_count++;
                if (patt_count > 1) {
                    erg = s;
                    break;
                }
            }
        }
        if (erg != null) {
            erg = erg.split("\"")[1];
        }
        return erg;
    }
}
