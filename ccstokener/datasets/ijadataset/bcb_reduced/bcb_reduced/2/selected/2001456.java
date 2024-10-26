package parts;

import java.io.File;
import java.io.InputStream;
import java.io.BufferedInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.FileNotFoundException;
import java.net.URL;
import java.net.MalformedURLException;
import java.net.URLConnection;
import java.awt.Component;
import java.util.Enumeration;
import java.util.zip.*;
import javax.swing.*;

public class PartFilesInstaller extends Thread {

    private final int BUFFER_SIZE = 4096;

    private final int COPY_BUFFER = 2048;

    private byte buffer[] = new byte[COPY_BUFFER];

    private final String zipFilename = "partfiles.zip";

    private ProgressMonitor monitor;

    private UpdateProgress progUpdater;

    private UpdateNote noteUpdater;

    private Component parentComp;

    private String urlName;

    private String installDir;

    private DoneListener listener = null;

    private DoneListenerThread doneThread = new DoneListenerThread();

    public PartFilesInstaller() {
        this.parentComp = null;
    }

    public PartFilesInstaller(Component parentComp, String urlName, String installDir) {
        this.parentComp = parentComp;
        this.urlName = urlName;
        this.installDir = installDir;
    }

    public void run() {
        install(parentComp, urlName, installDir);
    }

    public void setDoneListener(DoneListener listener) {
        this.listener = listener;
    }

    public void install(Component parentComp, String urlName, String installDir) {
        try {
            URL url = new URL(urlName);
            monitor = new ProgressMonitor(parentComp, "Installing Part Files", "", 0, 1000);
            progUpdater = new UpdateProgress(monitor);
            noteUpdater = new UpdateNote(monitor);
            updateProgress(0);
            downloadPartFiles(parentComp, url, installDir);
        } catch (MalformedURLException e) {
            JOptionPane.showMessageDialog(parentComp, new String("Invalid URL: " + urlName), "jLug Error", JOptionPane.ERROR_MESSAGE);
        } catch (FileNotFoundException e) {
            JOptionPane.showMessageDialog(parentComp, new String("Can't find: " + urlName), "jLug Error", JOptionPane.ERROR_MESSAGE);
        } catch (IOException e) {
            JOptionPane.showMessageDialog(parentComp, e.toString(), "jLug Error", JOptionPane.ERROR_MESSAGE);
        } finally {
            fireDoneListener();
        }
    }

    public void downloadPartFiles(Component parentComp, URL url, String installDir) throws IOException {
        updateNote("Downloading: " + url.getFile());
        FileOutputStream out = new FileOutputStream(zipFilename);
        URLConnection conn = url.openConnection();
        int size = conn.getContentLength();
        InputStream in = new BufferedInputStream(conn.getInputStream(), BUFFER_SIZE);
        int len = 0;
        int lenRead = 0;
        while ((len = in.read(buffer, 0, COPY_BUFFER)) > 0) {
            out.write(buffer, 0, len);
            lenRead += len;
            updateProgress((int) ((size - (size - lenRead)) * (500.0f / size)));
        }
        in.close();
        out.flush();
        unzip(parentComp, zipFilename, installDir);
    }

    public void unzip(Component parentComp, String filename, String installDir) throws IOException {
        if (!installDir.endsWith(File.separator)) {
            installDir = new String(installDir + File.separator);
        }
        ZipFile zipFile = new ZipFile(filename);
        int sizePartFileList = zipFile.size();
        int count = 0;
        Enumeration partFileList = zipFile.entries();
        while (partFileList.hasMoreElements()) {
            count++;
            updateProgress((int) (500 + ((sizePartFileList - (sizePartFileList - count)) * (500.0f / sizePartFileList))));
            ZipEntry zippedPartFile = (ZipEntry) partFileList.nextElement();
            if (!zippedPartFile.isDirectory()) {
                String partFilename = zippedPartFile.getName();
                if (partFilename.endsWith(".dat") || partFilename.endsWith(".ldr") || partFilename.endsWith(".lst")) {
                    updateNote("Unzipping: " + partFilename);
                    BufferedInputStream in = new BufferedInputStream(zipFile.getInputStream(zippedPartFile), BUFFER_SIZE);
                    File partFile = new File(installDir + partFilename);
                    File parentFile = partFile.getParentFile();
                    if (parentFile != null && !parentFile.exists()) {
                        parentFile.mkdirs();
                    }
                    FileOutputStream out = new FileOutputStream(installDir + partFilename);
                    int size = (int) zippedPartFile.getSize();
                    int len = 0;
                    while (size > 0 && (len = in.read(buffer, 0, Math.min(COPY_BUFFER, size))) != -1) {
                        out.write(buffer, 0, len);
                        size -= len;
                    }
                    in.close();
                    out.flush();
                }
            }
        }
    }

    public void fireDoneListener() {
        SwingUtilities.invokeLater(doneThread);
    }

    public void updateProgress(int progress) {
        if (progUpdater.lastProgress != progress) {
            progUpdater.setProgress(progress);
            SwingUtilities.invokeLater(progUpdater);
        }
    }

    public void updateNote(String note) {
        noteUpdater.setNote(note);
        SwingUtilities.invokeLater(noteUpdater);
    }

    private class DoneListenerThread extends Thread {

        public void run() {
            if (listener != null) {
                listener.installDone();
            }
        }
    }

    private class UpdateProgress extends Thread {

        private ProgressMonitor monitor;

        private int progress;

        public int lastProgress = -1;

        public UpdateProgress(ProgressMonitor monitor) {
            this.monitor = monitor;
        }

        public void setProgress(int progress) {
            this.progress = progress;
            lastProgress = progress;
        }

        public void run() {
            monitor.setProgress(progress);
        }
    }

    private class UpdateNote extends Thread {

        private ProgressMonitor monitor;

        public UpdateNote(ProgressMonitor monitor) {
            this.monitor = monitor;
        }

        private String note;

        public void setNote(String note) {
            this.note = note;
        }

        public void run() {
            monitor.setNote(note);
        }
    }

    interface DoneListener {

        public void installDone();
    }
}
