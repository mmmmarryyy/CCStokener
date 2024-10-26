package com.lovehorsepower.imagemailerapp.workers;

import com.lovehorsepower.imagemailerapp.panels.GalleryManagerPanel;
import java.io.BufferedOutputStream;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.URL;
import java.net.URLConnection;
import java.util.Iterator;
import javax.swing.SwingUtilities;
import javax.swing.SwingWorker;

/**
 *
 * @author obernbergerj
 */
public class SaveGalleryWorker extends SwingWorker {

    GalleryManagerPanel gp = null;

    File file = null;

    String gName = "";

    public SaveGalleryWorker(GalleryManagerPanel gp, File file) {
        this.gp = gp;
        this.file = file;
        if (gp != null) {
            this.gName = new String(gp.currentGalleryName);
        }
    }

    @Override
    protected Object doInBackground() throws Exception {
        Iterator it = null;
        String localName = "";
        ImageBean ib = null;
        int count = 0;
        String imageName = "";
        it = gp.galleryImageBeanList.iterator();
        while (it != null && it.hasNext()) {
            ib = (ImageBean) it.next();
            System.out.println("Name: " + ib.getImageLabel().getName());
            localName = file.getAbsolutePath() + "\\" + ib.getImageLabel().getName().substring(ib.getImageLabel().getName().lastIndexOf("/") + 1);
            downloadURL(ib.getImageLabel().getName(), localName);
            imageName = ib.getImageLabel().getName().substring(ib.getImageLabel().getName().lastIndexOf("/") + 1);
            count++;
            SwingUtilities.invokeLater(new UpdateGalleryP(gp, imageName, count));
        }
        return null;
    }

    public void downloadURL(String address, String localFileName) {
        OutputStream out = null;
        URLConnection conn = null;
        InputStream in = null;
        try {
            URL url = new URL(address);
            out = new BufferedOutputStream(new FileOutputStream(localFileName));
            conn = url.openConnection();
            in = conn.getInputStream();
            byte[] buffer = new byte[1024];
            int numRead;
            while ((numRead = in.read(buffer)) != -1) {
                out.write(buffer, 0, numRead);
            }
        } catch (Exception exception) {
            exception.printStackTrace();
        } finally {
            try {
                if (in != null) {
                    in.close();
                }
                if (out != null) {
                    out.close();
                }
            } catch (IOException ioe) {
            }
        }
    }

    protected void done() {
        gp.doneSavingGallery(gName);
    }
}

class UpdateGalleryP implements Runnable {

    GalleryManagerPanel gp = null;

    int count = 0;

    String imageName = "";

    public UpdateGalleryP(GalleryManagerPanel gp, String imageName, int count) {
        this.gp = gp;
        this.imageName = imageName;
        this.count = count;
    }

    @Override
    public void run() {
        gp.progressBar.setValue(count);
        gp.statusMessageLabel.setText("Saving image " + imageName + " to disk...");
    }
}
