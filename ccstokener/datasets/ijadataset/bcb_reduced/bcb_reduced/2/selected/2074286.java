package Picasa;

import Core.Files;
import com.google.gdata.data.PlainTextConstruct;
import com.google.gdata.data.photos.PhotoEntry;
import com.google.gdata.util.ServiceException;
import java.awt.image.BufferedImage;
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
public class Photo {

    private PhotoEntry photo;

    private File thumb_1;

    private File thumb_2;

    private File thumb_3;

    private File image;

    private int size;

    public Photo(PhotoEntry photo, int i) {
        this.photo = photo;
        size = i;
        updateThumb();
    }

    private void updateThumb() {
        switch(size) {
            case 0:
                {
                    thumb_1 = new File("TMP/1_" + photo.getAlbumId() + photo.getTitle().getPlainText());
                    try {
                        if (!thumb_1.exists()) {
                            thumb_1.createNewFile();
                            Files.saveTmpFile(thumb_1, photo.getMediaThumbnails().get(0).getUrl());
                        }
                    } catch (Exception ex) {
                        ex.printStackTrace();
                    }
                    return;
                }
            case 1:
                {
                    thumb_2 = new File("TMP/2_" + photo.getAlbumId() + photo.getTitle().getPlainText());
                    try {
                        if (!thumb_2.exists()) {
                            thumb_2.createNewFile();
                            Files.saveTmpFile(thumb_2, photo.getMediaThumbnails().get(1).getUrl());
                        }
                    } catch (Exception ex) {
                        ex.printStackTrace();
                    }
                    return;
                }
            case 2:
                {
                    thumb_3 = new File("TMP/3_" + photo.getAlbumId() + photo.getTitle().getPlainText());
                    try {
                        if (!thumb_3.exists()) {
                            thumb_3.createNewFile();
                            Files.saveTmpFile(thumb_3, photo.getMediaThumbnails().get(2).getUrl());
                        }
                    } catch (Exception ex) {
                        ex.printStackTrace();
                    }
                    return;
                }
        }
    }

    public String getWebURL() {
        return photo.getHtmlLink().getHref();
    }

    private void rename(String name) {
    }

    public String getImageURL() {
        return photo.getMediaContents().get(0).getUrl();
    }

    public long getWidth() {
        return photo.getMediaThumbnails().get(0).getWidth();
    }

    public int getHeight() {
        return photo.getMediaThumbnails().get(0).getHeight();
    }

    public int getFileSize() {
        long size = 0;
        try {
            size = photo.getSize();
        } catch (ServiceException ex) {
            ex.printStackTrace();
        }
        return (int) size;
    }

    public String getImage(JProgressBar bar) {
        long size = 0;
        try {
            size = photo.getSize();
        } catch (ServiceException ex) {
            ex.printStackTrace();
        }
        try {
            bar.setMaximum((int) size);
        } catch (Exception ex) {
            ex.printStackTrace();
        }
        bar.setValue(0);
        image = new File("TMP/" + photo.getTitle().getPlainText());
        try {
            if (!image.exists()) {
                image.createNewFile();
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
        return "<html><body bgcolor=\"rgb(240, 240, 240)\"><center><img src=\"file:\\" + image.getAbsolutePath() + "\" width=" + photo.getMediaContents().get(0).getWidth() + " height=" + photo.getMediaContents().get(0).getHeight() + "></img></center></body></html>";
    }

    public String downloadAndOpen(JProgressBar bar) {
        long size = 0;
        try {
            size = photo.getSize();
        } catch (ServiceException ex) {
            ex.printStackTrace();
        }
        try {
            bar.setMaximum((int) size);
        } catch (Exception ex) {
            ex.printStackTrace();
        }
        bar.setValue(0);
        image = new File("TMP/" + photo.getTitle().getPlainText());
        try {
            if (!image.exists()) {
                image.createNewFile();
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
        return image.getAbsolutePath();
    }

    public void updateInfo(String name, String des) {
        updateTitle(name);
        updateDescription(des);
        update();
    }

    public void updateTitle(String title) {
        photo.setTitle(new PlainTextConstruct((title)));
        rename(title);
    }

    public void updateDescription(String des) {
        photo.setDescription(new PlainTextConstruct((des)));
    }

    public void update() {
        try {
            photo.update();
        } catch (Exception ex) {
            ex.printStackTrace();
        }
    }

    public String getName() {
        return photo.getTitle().getPlainText();
    }

    public String getIcon() {
        switch(size) {
            case 0:
                {
                    return "file:\\" + thumb_1.getAbsolutePath();
                }
            case 1:
                {
                    return "file:\\" + thumb_2.getAbsolutePath();
                }
            case 2:
                {
                    return "file:\\" + thumb_3.getAbsolutePath();
                }
        }
        return "file:\\" + thumb_1.getAbsolutePath();
    }

    public String getDescription() {
        return photo.getDescription().getPlainText();
    }

    public String getDate() {
        try {
            return photo.getTimestamp().toGMTString();
        } catch (ServiceException ex) {
            ex.printStackTrace();
            return "";
        }
    }

    public void deletePhoto() {
        try {
            photo.delete();
        } catch (Exception ex) {
            ex.printStackTrace();
        }
    }

    public String toString() {
        return "<html>  <p>   <center><img src=\"" + getIcon() + "\" width=" + photo.getMediaThumbnails().get(size).getWidth() + " height=" + photo.getMediaThumbnails().get(size).getHeight() + "></img></center></p>" + "<center><p><font color=\"#000000\" size=\"3\" face=\"Verdana\"><strong>" + getName() + "</strong></font></center></p>" + "<center>" + getDate() + "</center>";
    }
}
