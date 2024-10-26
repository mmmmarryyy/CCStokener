package bgpanalyzer.functions.downloadFile;

import bgpanalyzer.functions.Matrix.MatrixController;
import bgpanalyzer.util.TempDir;
import java.io.File;
import java.io.FileOutputStream;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.URL;
import java.net.URLConnection;

public class DownloadFile extends Thread {

    private DownloadFileView downloadFileView = null;

    private RefreshDownloadFileData refreshDownloadFileData = null;

    private RefreshDownloadFileMeter refreshDownloadFileMeter = null;

    private String path = null;

    private String file = null;

    private int size = 0;

    private int downloaded = 0;

    private boolean active = true;

    private TempDir tmp = new TempDir();

    /** Creates a new instance of DownloadFile */
    public DownloadFile(DownloadFileView downloadFileView, String path, String file) {
        this.downloadFileView = downloadFileView;
        this.path = path;
        this.file = file;
        start();
    }

    public void run() {
        refreshDownloadFileData = new RefreshDownloadFileData(this);
        refreshDownloadFileMeter = new RefreshDownloadFileMeter(this);
        try {
            byte[] buffer = new byte[1024];
            URL url = new URL(path + file);
            URLConnection urlc = url.openConnection();
            InputStream in = urlc.getInputStream();
            OutputStream out = new FileOutputStream(tmp.getTemp() + "/" + file);
            size = urlc.getContentLength();
            int n = 0;
            while (!(n < 0) && active) {
                n = in.read(buffer);
                if (!(n < 0)) {
                    out.write(buffer, 0, n);
                    if (size != -1) {
                        downloaded = downloaded + n;
                    }
                }
            }
            in.close();
            out.close();
        } catch (Exception e) {
            e.printStackTrace();
        }
        MatrixController mc = MatrixController.getInstance();
        mc.addFileToProcess(file);
        refreshDownloadFileData.setActive(false);
        downloadFileView.close();
    }

    public int getSize() {
        return size;
    }

    public int getDownloaded() {
        return downloaded;
    }

    public void setTextLblDownloaded(String text) {
        downloadFileView.setTextLblDownloaded(text);
    }

    public void setTextLblRemainingTime(String text) {
        downloadFileView.setTextLblRemainingTime(text);
    }

    public void setTextLblRateOfTransference(String text) {
        downloadFileView.setTextLblRateOfTransference(text);
    }

    public void setPbProcessValue(int value) {
        downloadFileView.setPbProcessValue(value);
    }

    public void desactive() {
        refreshDownloadFileData.setActive(false);
        refreshDownloadFileMeter.setActive(false);
        this.active = false;
    }
}
