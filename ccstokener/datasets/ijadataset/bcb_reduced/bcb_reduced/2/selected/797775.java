package Picasa;

import Core.Files;
import com.google.gdata.client.photos.PicasawebService;
import com.google.gdata.data.photos.PhotoEntry;
import java.io.BufferedOutputStream;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.net.URL;
import java.util.logging.Level;
import java.util.logging.Logger;
import javax.swing.JProgressBar;

/**
 *
 * @author H3R3T1C
 */
public class imagePreview {

    private PhotoEntry photo;

    private File thumb;

    private int width;

    private int hight;

    public imagePreview(PhotoEntry photo) {
        this.photo = photo;
        thumb = new File("TMP/I_" + photo.getAlbumId() + photo.getTitle().getPlainText());
        if (!thumb.exists()) {
            try {
                thumb.createNewFile();
                Files.saveTmpFile(thumb, photo.getMediaThumbnails().get(0).getUrl());
            } catch (Exception ex) {
                ex.printStackTrace();
            }
        }
        this.width = photo.getMediaContents().get(0).getWidth();
        this.hight = photo.getMediaContents().get(0).getHeight();
    }

    public String getName() {
        return photo.getTitle().getPlainText();
    }

    public void setWidth(int width) {
        this.width = width;
    }

    public void setHight(int hight) {
        this.hight = hight;
    }

    public void resetSizes() {
        this.width = photo.getMediaContents().get(0).getWidth();
        this.hight = photo.getMediaContents().get(0).getHeight();
    }

    public int getWidth() {
        return photo.getMediaContents().get(0).getWidth();
    }

    public int getHeight() {
        return photo.getMediaContents().get(0).getHeight();
    }

    public String getImage(JProgressBar bar) {
        long size = 0;
        try {
            size = photo.getSize();
            bar.setMaximum((int) size);
        } catch (Exception ex) {
            ex.printStackTrace();
        }
        bar.setValue(0);
        File image = new File("TMP/W_" + photo.getTitle().getPlainText());
        try {
            if (!image.exists()) {
                image.deleteOnExit();
                URL url = null;
                BufferedOutputStream fOut = null;
                try {
                    url = new URL(photo.getMediaContents().get(0).getUrl());
                    InputStream html = null;
                    html = url.openStream();
                    fOut = new BufferedOutputStream(new FileOutputStream(image));
                    byte[] buffer = new byte[32 * 1024];
                    int bytesRead = 0;
                    int in = 0;
                    while ((bytesRead = html.read(buffer)) != -1) {
                        in += bytesRead;
                        bar.setValue(in);
                        fOut.write(buffer, 0, bytesRead);
                    }
                    html.close();
                    fOut.close();
                } catch (Exception ex) {
                    ex.printStackTrace();
                }
            }
        } catch (Exception ex) {
            ex.printStackTrace();
        }
        return "<html><body bgcolor=\"rgb(240, 240, 240)\"><img src=\"file:\\" + image.getAbsolutePath() + "\" width=" + width + " height=" + this.hight + "></img></body></html>";
    }

    public Photo getPhoto() {
        return new Photo(photo, 0);
    }

    public String getIcon() {
        return "file:\\" + thumb.getAbsolutePath();
    }

    public String toString() {
        return "<html><img src=\"" + getIcon() + "\" width=" + photo.getMediaThumbnails().get(0).getWidth() + " height=" + photo.getMediaThumbnails().get(0).getHeight() + "></img>";
    }
}
