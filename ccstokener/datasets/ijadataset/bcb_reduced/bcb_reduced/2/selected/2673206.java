package MP3Search;

import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.net.URL;
import java.net.URLConnection;
import javax.swing.JLabel;
import javax.swing.JProgressBar;
import javax.swing.Timer;
import Capture.CaptureSystem;
import Main.Commun;
import Main.MainForm;

public class FileDownloader implements Runnable, ActionListener {

    private String addr = "";

    private String fileDir = "";

    private String fileTitle = "";

    private int fileSize = 0;

    private int downSize = 0;

    private int sizePrec = 0;

    private String speed = "0.0 KB/s";

    private boolean stop = false;

    private boolean isFinished = false;

    private JProgressBar pbar;

    private JLabel lblStatus;

    private Timer timerRefresh = new Timer(1000, this);

    public FileDownloader(String addr, String fileDir, String fileTitle, JProgressBar pbar, JLabel lblStatus) {
        this.addr = addr;
        this.fileDir = fileDir;
        this.fileTitle = fileTitle.replaceAll("[\\\\/:*?\"<>|]", "_");
        this.pbar = pbar;
        this.lblStatus = lblStatus;
        this.timerRefresh.start();
        this.timerRefresh.setRepeats(true);
    }

    public void run() {
        try {
            URL url = new URL(this.addr);
            URLConnection connection = url.openConnection();
            this.fileSize = connection.getContentLength();
            CaptureSystem.filterSize = this.fileSize;
            try {
                Thread.sleep(1000);
            } catch (InterruptedException e1) {
                Commun.logError(e1);
            }
            this.pbar.setMaximum(this.fileSize);
            if (this.fileSize == -1) {
                this.lblStatus.setText(MainForm.lang.lang_table[50]);
                this.isFinished = true;
                return;
            }
            String dir = this.fileDir + File.separator + this.fileTitle;
            try {
                InputStream input = connection.getInputStream();
                File fic = new File("temp");
                if (!fic.exists()) fic.mkdir();
                FileOutputStream writeFile = new FileOutputStream(dir);
                byte[] buffer = new byte[2048];
                int read;
                while (((read = input.read(buffer)) > 0) && (!this.stop)) {
                    writeFile.write(buffer, 0, read);
                    this.downSize += read;
                    this.pbar.setValue(this.downSize);
                    this.lblStatus.setText(MainForm.lang.lang_table[51] + " ... (" + Commun.sizeConvert(this.downSize) + " / " + Commun.sizeConvert(this.fileSize) + ") at " + this.speed);
                }
                writeFile.flush();
                writeFile.close();
                input.close();
            } catch (Exception e) {
                this.lblStatus.setText(MainForm.lang.lang_table[50]);
                this.isFinished = true;
                return;
            }
            this.isFinished = true;
            if (this.stop) {
                File f = new File(dir);
                f.delete();
            }
        } catch (IOException e) {
            this.isFinished = true;
            this.lblStatus.setText(MainForm.lang.lang_table[50]);
        }
    }

    public void actionPerformed(ActionEvent e) {
        this.speed = Commun.sizeConvert(this.downSize - this.sizePrec) + "/s";
        this.sizePrec = this.downSize;
    }

    public void stopDownload() {
        this.stop = true;
    }

    public boolean isFinished() {
        return this.isFinished;
    }
}
