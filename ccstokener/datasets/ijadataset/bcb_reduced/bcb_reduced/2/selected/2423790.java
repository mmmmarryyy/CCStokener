package org.gdi3d.xnavi.panels.openurl;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.io.InputStreamReader;
import java.net.URL;
import java.net.URLConnection;
import javax.swing.JLabel;
import javax.swing.JProgressBar;
import org.gdi3d.xnavi.navigator.Navigator;
import org.gdi3d.xnavi.viewer.Java3DViewer;

/**
 * @author billen
 *
 */
public class OpenURLThread implements Runnable {

    private Java3DViewer viewer = null;

    private OpenURLDialog openURLDialog = null;

    private boolean clear = false;

    private static boolean active = false;

    private File tmpVRMLFile = null;

    private OpenURLStatusDialog statusFrame = null;

    public OpenURLThread(Java3DViewer viewer) {
        this(viewer, false);
    }

    public OpenURLThread(Java3DViewer viewer, boolean clear) {
        synchronized (OpenURLThread.class) {
            if (!active) {
                Navigator navigator = Navigator.impl;
                if (navigator != null) {
                    navigator.getFrame().setEnabled(false);
                }
                active = true;
                this.clear = clear;
                this.viewer = viewer;
                String default_url = "";
                statusFrame = new OpenURLStatusDialog(viewer);
                openURLDialog = new OpenURLDialog(viewer, default_url, "epsg:31467");
                new Thread(this).start();
            }
        }
    }

    @Override
    public void run() {
        Navigator navigator = Navigator.impl;
        System.out.println("Java3DViewer.importURL");
        try {
            synchronized (openURLDialog.waitForDialogMutex) {
                try {
                    openURLDialog.waitForDialogMutex.wait();
                } catch (InterruptedException e) {
                    e.printStackTrace();
                }
            }
            navigator.getFrame().setAlwaysOnTop(true);
            navigator.getFrame().setAlwaysOnTop(false);
            JProgressBar dataDownloadProgressBar = new JProgressBar();
            dataDownloadProgressBar.setIndeterminate(true);
            dataDownloadProgressBar.setStringPainted(true);
            statusFrame.addComponent("Data Download:", dataDownloadProgressBar);
            JLabel downloadSizeLabel = statusFrame.addItem("", "0 MB");
            String url_string = openURLDialog.getURL();
            if (url_string != null && !url_string.equals("")) {
                URL url = new URL(url_string);
                if (this.clear) {
                    viewer.clearScene();
                }
                loadURL(url, openURLDialog.getUser(), openURLDialog.getPassword(), downloadSizeLabel);
                URL newURL = new URL("file:/" + this.tmpVRMLFile.getAbsolutePath());
                dataDownloadProgressBar.setIndeterminate(false);
                dataDownloadProgressBar.setValue(100);
                JProgressBar crsTransformProgressBar = new JProgressBar();
                crsTransformProgressBar.setStringPainted(true);
                statusFrame.addComponent("Coordinate Transform:", crsTransformProgressBar);
                viewer.importURL(newURL, openURLDialog.getCRS(), crsTransformProgressBar);
            }
        } catch (Exception e) {
            e.printStackTrace();
        } finally {
            synchronized (OpenURLThread.class) {
                active = false;
                if (navigator != null) {
                    navigator.getFrame().setAlwaysOnTop(true);
                    navigator.getFrame().setAlwaysOnTop(false);
                    navigator.getFrame().setEnabled(true);
                }
                statusFrame.setVisible(false);
            }
        }
    }

    private void loadURL(URL url, String username, String password) throws IOException {
        loadURL(url, username, password, null);
    }

    private void loadURL(URL url, String username, String password, JLabel statusDialogMBLabel) throws IOException {
        URLConnection connection = url.openConnection();
        if (username != null && !username.equals("")) {
            if (password == null) {
                password = "";
            }
            String encoding = new sun.misc.BASE64Encoder().encode(new String(username + ":" + password).getBytes());
            connection.setRequestProperty("Authorization", "Basic " + encoding);
        }
        BufferedReader in = new BufferedReader(new InputStreamReader(connection.getInputStream()));
        String inputLine;
        this.tmpVRMLFile = File.createTempFile("xnavi-", null, null);
        System.out.println("Created tmp file: " + this.tmpVRMLFile.getAbsolutePath());
        FileWriter fw = new FileWriter(this.tmpVRMLFile);
        long bytesInFile = this.tmpVRMLFile.length();
        double sizeInMB = ((double) bytesInFile) / (1024.0 * 1024.0);
        String response = "";
        while ((inputLine = in.readLine()) != null) {
            response = inputLine + "\n";
            fw.write(response);
            fw.flush();
            if (statusDialogMBLabel != null) {
                bytesInFile = this.tmpVRMLFile.length();
                sizeInMB = ((double) bytesInFile) / (1024.0 * 1024.0);
                sizeInMB *= 100.0;
                sizeInMB = (double) ((int) sizeInMB);
                sizeInMB /= 100.0;
                statusDialogMBLabel.setText(sizeInMB + " MB");
                statusDialogMBLabel.repaint();
            }
        }
        fw.close();
        System.out.println("Wrote file " + this.tmpVRMLFile.getAbsolutePath());
    }

    private void writeTextFile(String value) throws IOException {
        this.tmpVRMLFile = File.createTempFile("xnavi-", null, null);
        FileWriter fw = new FileWriter(this.tmpVRMLFile);
        fw.write(value);
        fw.flush();
        fw.close();
    }
}
