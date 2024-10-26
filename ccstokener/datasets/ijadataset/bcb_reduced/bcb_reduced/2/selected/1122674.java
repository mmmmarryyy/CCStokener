package de.sharpner.jcmd.system;

import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.io.BufferedOutputStream;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.MalformedURLException;
import java.net.URL;
import java.net.URLConnection;
import java.net.UnknownHostException;
import javax.swing.Timer;

public class FileDownload extends Thread implements StatusCheck {

    private int status = 0;

    private long currentSize = 0;

    private long totalSize = 0;

    private String surl;

    private String dest;

    public static final int NOT_STARTED = 5;

    public static final int NO_FILE = 1;

    public static final int NO_HOST = 2;

    public static final int WRONG_URL_FORMAT = 3;

    public static final int FILE_EXISTS = 4;

    public static final int NO_PERMISSION = 6;

    public static final int START_DOWNLOAD = 0;

    private boolean[] errorCode = new boolean[7];

    private OutputStream out = null;

    private URLConnection urlc = null;

    private InputStream in = null;

    public FileDownload(String url, String dest) {
        this.surl = url;
        this.dest = dest;
    }

    public int getTotalStatus() {
        return status;
    }

    public void confirmOverwrite() {
        errorCode[FILE_EXISTS] = false;
    }

    public void run() {
        try {
            downloadFile();
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    public void connect() {
        File f = new File(dest);
        if (!f.exists()) {
            File f2 = new File(f.getParent());
            f2.mkdir();
        } else errorCode[FILE_EXISTS] = true;
        if (f.isDirectory()) {
            File f2 = new File(surl);
            f = new File(f.getAbsolutePath() + "/" + f2.getName());
            System.out.println("renamed to: " + f.getAbsolutePath());
            try {
                f.createNewFile();
            } catch (Exception e) {
                errorCode[NO_PERMISSION] = true;
            }
        }
        try {
            URL url = new URL(surl);
            out = new BufferedOutputStream(new FileOutputStream(f.getAbsolutePath()));
            urlc = url.openConnection();
            totalSize = urlc.getContentLength();
            in = urlc.getInputStream();
        } catch (MalformedURLException mue) {
            mue.printStackTrace();
            errorCode[WRONG_URL_FORMAT] = true;
        } catch (UnknownHostException uhe) {
            errorCode[NO_HOST] = true;
        } catch (FileNotFoundException fne) {
            fne.printStackTrace();
            errorCode[NO_FILE] = true;
        } catch (IOException ioe) {
            ioe.printStackTrace();
        }
    }

    private long written = 0;

    private long tempwritten = 0;

    private int seconds = 0;

    private void downloadFile() throws Exception {
        try {
            byte[] buffer = new byte[1024];
            int numRead;
            long numWritten = 0;
            Timer timer = new Timer(1000, new ActionListener() {

                public void actionPerformed(ActionEvent e) {
                    speed = (float) (written - tempwritten) / 1024.0f;
                    tempwritten = written;
                    seconds++;
                }
            });
            timer.start();
            while ((numRead = in.read(buffer)) != -1) {
                out.write(buffer, 0, numRead);
                numWritten += numRead;
                currentSize = numWritten;
                written = numWritten;
            }
            timer.stop();
        } catch (Exception ex) {
            ex.printStackTrace();
        } finally {
            try {
                if (in != null) {
                    in.close();
                }
                if (out != null) {
                    out.close();
                }
            } catch (IOException ioe) {
                ioe.printStackTrace();
            }
        }
    }

    public int getStatus() {
        if (totalSize != 0) this.status = (int) (100 * this.currentSize / this.totalSize);
        return this.status;
    }

    public float getSpeed() {
        return speed;
    }

    private float speed = 0.0f;

    public String toString() {
        return new File(surl).getName();
    }

    public int getError() {
        if (errorCode[FILE_EXISTS]) return FILE_EXISTS;
        if (errorCode[NO_FILE]) return NO_FILE;
        if (errorCode[NOT_STARTED]) return NOT_STARTED; else return START_DOWNLOAD;
    }
}
