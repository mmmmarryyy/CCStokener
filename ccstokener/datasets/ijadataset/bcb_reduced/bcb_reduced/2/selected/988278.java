package downloadPackage;

import java.awt.datatransfer.Clipboard;
import java.awt.datatransfer.DataFlavor;
import java.awt.datatransfer.Transferable;
import java.io.File;
import java.io.FileOutputStream;
import java.io.InputStream;
import java.net.URL;
import java.net.URLConnection;
import javax.swing.SwingWorker;
import filePackage.MP3FileInformation;
import headFrame.SuperFrame;

public class DownloadVideoThread extends SwingWorker<Void, Void> {

    SuperFrame mframe;

    MP3FileInformation mp3;

    Clipboard clipboard;

    public DownloadVideoThread(SuperFrame mframe, MP3FileInformation mp3) {
        this.mframe = mframe;
        this.mp3 = mp3;
        clipboard = mframe.getToolkit().getSystemClipboard();
    }

    public Void doInBackground() {
        Transferable clipData = clipboard.getContents(this);
        File file = new File("Videos/" + (mp3.getArtist() + " - " + mp3.getTitle() + ".jpg").replace("/", "").replace("\\", ""));
        try {
            String test = (String) (clipData.getTransferData(DataFlavor.stringFlavor));
            String testje = test.toLowerCase();
            if (testje.indexOf(".flv") > 0 || testje.indexOf(".wmv") > 0 || testje.indexOf(".mpg") > 0 || testje.indexOf(".mpeg") > 0 || testje.indexOf(".avi") > 0 || testje.indexOf(".avi") > 0 || testje.indexOf(".divx") > 0 || testje.indexOf(".avi") > 0) {
                URL url = new URL(test);
                (new File("Videos/")).mkdirs();
                System.out.println("Saving video to " + file);
                try {
                    URLConnection urlC = url.openConnection();
                    InputStream is = url.openStream();
                    System.out.flush();
                    FileOutputStream fos = null;
                    fos = new FileOutputStream(file);
                    byte[] buf = new byte[32768];
                    int len;
                    while ((len = is.read(buf)) > 0) {
                        fos.write(buf, 0, len);
                    }
                    is.close();
                    fos.close();
                } catch (Exception e) {
                    System.out.println("Error saving video from url: " + url);
                    mp3.setVideo(file.getAbsolutePath());
                }
            }
        } catch (Exception ex) {
            System.out.println("not a valid video file");
            ex.printStackTrace();
        }
        return null;
    }

    protected void done() {
        mframe.mfa.updateTable();
    }
}
