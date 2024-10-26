package entagged.covers;

import java.awt.image.BufferedImage;
import java.io.BufferedInputStream;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.net.HttpURLConnection;
import java.net.MalformedURLException;
import java.net.URL;
import javax.imageio.ImageIO;
import javax.swing.ImageIcon;

public class Cover {

    private String artist, album;

    private URL url;

    private BufferedImage img = null;

    public Cover(String artist, String album, URL url) {
        this.artist = artist;
        this.album = album;
        this.url = url;
    }

    public Cover(String artist, String album, String s) throws CoverException {
        this.artist = artist;
        this.album = album;
        try {
            this.url = new URL(s);
        } catch (MalformedURLException e) {
            throw new CoverException(e.getMessage());
        }
    }

    public String getArtist() {
        return this.artist;
    }

    public String getAlbum() {
        return this.album;
    }

    public URL getURL() {
        return this.url;
    }

    public ImageIcon getImageIcon() {
        if (!loadImageIfNeeded()) return new ImageIcon();
        return new ImageIcon(img);
    }

    public String getImageExtension() {
        return Utils.getExtension(url);
    }

    public void saveAs(File f) throws CoverException {
        FileOutputStream fw = null;
        BufferedInputStream in = null;
        try {
            HttpURLConnection httpConn = (HttpURLConnection) url.openConnection();
            httpConn.setDoInput(true);
            in = new BufferedInputStream(httpConn.getInputStream());
            f.delete();
            fw = new FileOutputStream(f);
            int b;
            while ((b = in.read()) != -1) fw.write(b);
            fw.close();
            in.close();
        } catch (IOException e) {
            throw new CoverException(e.getMessage());
        } finally {
            try {
                if (fw != null) fw.close();
                if (in != null) in.close();
            } catch (IOException ex) {
                System.err.println("Glurps this is severe: " + ex.getMessage());
            }
        }
    }

    public int getImagePixels() {
        if (!loadImageIfNeeded()) return 0;
        return img.getWidth() * img.getHeight();
    }

    private boolean loadImageIfNeeded() {
        if (img != null) return true;
        try {
            this.img = ImageIO.read(url);
            return true;
        } catch (IOException e) {
            return false;
        }
    }

    public int compareTo(Object o) {
        if (o == null) return 1;
        Cover cover = (Cover) o;
        return this.getImagePixels() - cover.getImagePixels();
    }

    public String toString() {
        StringBuffer sb = new StringBuffer();
        sb.append("Cover Result:\nArtist: ").append(this.artist).append("\nAlbum: ").append(this.album).append("\nImage:").append(this.url);
        return sb.toString();
    }
}
