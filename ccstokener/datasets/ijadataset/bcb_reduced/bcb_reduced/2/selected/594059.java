package ch.rgw.net;

import java.awt.Container;
import java.io.*;
import java.net.URL;
import javax.swing.JProgressBar;
import ch.rgw.swingtools.InfoWindow;
import ch.rgw.tools.ExHandler;
import ch.rgw.tools.Log;

public class Downloader {

    private String remote;

    private Log log;

    InfoWindow iw;

    public Downloader() {
        log = Log.get("Downloader");
    }

    public static final int OK = 0;

    public static final int COULDNT_CREATE = 1;

    public static final int COULDNT_WRITE = 2;

    public static final int CANCELLED = 3;

    public int download(String src, File fNew, Container parent, boolean errorOutput) throws Exception {
        remote = src;
        if (errorOutput == true) {
            Log.setAlert(parent);
        }
        if (!fNew.exists()) {
            if (fNew.createNewFile() == false) {
                log.log("Konnte Datei " + fNew.getAbsolutePath() + " nicht erstellen", Log.ERRORS);
                return COULDNT_CREATE;
            }
            if (fNew.canWrite() == false) {
                log.log("Keine Schreibberechtigung f�r Datei " + fNew.getAbsolutePath(), Log.ERRORS);
                return COULDNT_WRITE;
            }
            DLTask worker = new DLTask(fNew, null);
            Thread doit = new Thread(worker);
            doit.start();
            while (doit.isAlive()) {
                log.log("geladen: " + worker.total, Log.INFOS);
                Thread.sleep(100);
            }
            if (worker.bStop == true) {
                log.log("Download Abgebrochen", Log.INFOS);
                return CANCELLED;
            }
        }
        return OK;
    }

    class DLTask implements Runnable {

        File dest;

        int total;

        boolean bStop;

        int r;

        JProgressBar jpb;

        DLTask(File d, JProgressBar show) {
            dest = d;
            jpb = show;
        }

        public void run() {
            try {
                BufferedOutputStream bos = new BufferedOutputStream(new FileOutputStream(dest));
                URL url = new URL(remote);
                InputStream is = url.openStream();
                BufferedInputStream bis = new BufferedInputStream(is);
                byte[] buffer = new byte[64535];
                total = 0;
                while (true) {
                    r = bis.read(buffer);
                    if (r == -1) {
                        break;
                    }
                    bos.write(buffer, 0, r);
                    total += r;
                    Thread.sleep(100);
                    if (bStop == true) {
                        break;
                    }
                }
                bis.close();
                bos.close();
            } catch (Exception ex) {
                ExHandler.handle(ex);
            }
        }
    }
}
