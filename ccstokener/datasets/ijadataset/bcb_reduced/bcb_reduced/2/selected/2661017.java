package com.xohm.upgrades.autoupdate;

import java.io.*;
import java.net.*;
import com.xohm.base.logging.XohmLogger;
import com.xohm.platform.api.OSAPIFactory;

/**
 * <B>This class handles the software downloads. It downloads the
 * software releases from the provided url to the directory specified
 * in the configuration file, verifies the download using the 
 * provided file size and then notifies the clients when download 
 * is complete.  
 * </B><br><br>
 *
 * <font size=-1>Open source WiMAX connection manager<br>
 * � Copyright Sprint Nextel Corp. 2008</font><br><br>
 *
 * @author Sachin Kumar 
 */
public class Downloader implements Runnable {

    private String softwareName = null;

    private long softwareSize = 0l;

    private String softwareLocation = null;

    private UpdateProgressListener listener = null;

    /**
	 * Constructs a downloader object to download the software for
	 * specified software name, of the specified size and from the 
	 * specified location.
	 * 
	 * @param softwareName String - software name
	 * @param softwareSize long - software size in bytes
	 * @param softwareLocation String - software location url
	 */
    public Downloader(String softwareName, long softwareSize, String softwareLocation) {
        this.softwareName = softwareName;
        this.softwareSize = softwareSize;
        this.softwareLocation = softwareLocation;
    }

    /**
	 * This methods adds the listener for the download progress notifications.
	 * 
	 * @param listener UpdateProgressListener - listener object
	 */
    public void setProgressListener(UpdateProgressListener listener) {
        this.listener = listener;
    }

    /**
	 * This method downloads the file from the software location to
	 * download directory specified in configuration file. 
	 * If a file already present in the download directory with the
	 * same name and size then it does not download the file again.
	 */
    public void run() {
        OutputStream out = null;
        InputStream in = null;
        boolean success = false;
        String absoluteFileName = "";
        try {
            String fileName = getFileName(softwareLocation);
            File downloadFolder = new File(Properties.downloadFolder);
            if (downloadFolder.exists()) {
                if (downloadFolder.isDirectory()) {
                    fileName = downloadFolder.getPath() + File.separator + fileName;
                }
            } else {
                downloadFolder.mkdir();
                fileName = downloadFolder.getPath() + File.separator + fileName;
            }
            File softwareFile = new File(fileName);
            absoluteFileName = softwareFile.getAbsolutePath();
            if (softwareFile.exists() && softwareFile.length() == softwareSize) {
                XohmLogger.debugPrintln("Software file already exists. Exiting...");
                listener.downloadComplete(true, softwareName, absoluteFileName);
                return;
            } else {
                try {
                    File[] oldFiles = downloadFolder.listFiles();
                    for (int i = 0; i < oldFiles.length; i++) {
                        oldFiles[i].delete();
                    }
                } catch (Exception ex) {
                }
            }
            File softwareTempFile = File.createTempFile("XOHMCM", null);
            URL url = new URL(softwareLocation);
            out = new BufferedOutputStream(new FileOutputStream(softwareTempFile));
            URLConnection connection = url.openConnection();
            in = connection.getInputStream();
            listener.downloadStarted(softwareName);
            byte[] buffer = new byte[1024];
            int numRead;
            long numWritten = 0;
            while ((numRead = in.read(buffer)) != -1) {
                out.write(buffer, 0, numRead);
                numWritten += numRead;
                listener.downloadProgressNotification(softwareName, numWritten, softwareSize);
            }
            out.flush();
            out.close();
            in.close();
            if (copyFile(softwareTempFile, softwareFile)) {
                XohmLogger.debugPrintln("Download complete: " + absoluteFileName + "\t" + numWritten);
                success = true;
                softwareTempFile.delete();
            }
        } catch (Exception ex) {
            XohmLogger.warningPrintln("Software Update download failed - " + ex.getMessage(), null, null);
            ex.printStackTrace();
        }
        listener.downloadComplete(success, softwareName, absoluteFileName);
    }

    /**
	 * Utility method that takes the software location url/path as argument 
	 * and returns the filename. 
	 * @param softwareLocation String - software location url/path
	 * @return String - software file name
	 */
    private String getFileName(String softwareLocation) {
        String fileName = "";
        int index = softwareLocation.lastIndexOf("/");
        if (index >= 0 && index < softwareLocation.length() - 1) {
            fileName = softwareLocation.substring(index + 1);
        } else {
            XohmLogger.debugPrintln("File name not found. Setting it to default.");
            if (OSAPIFactory.getConnectionManager().isWindows()) {
                fileName = "setup.exe";
            } else if (OSAPIFactory.getConnectionManager().isMac()) {
                fileName = "setup.app";
            } else {
                fileName = "setup.pkg";
            }
        }
        return fileName;
    }

    /**
	 * This method copies the source file to destination file.
	 * 
	 * @param srcFile File - source file
	 * @param destFile File - destination file
	 * @return boolean - true if file is successfully copied.
	 */
    private boolean copyFile(File srcFile, File destFile) {
        boolean success = false;
        try {
            byte[] buffer = new byte[1024];
            InputStream in = new FileInputStream(srcFile);
            OutputStream out = new FileOutputStream(destFile);
            int bytesRead;
            while ((bytesRead = in.read(buffer)) >= 0) {
                out.write(buffer, 0, bytesRead);
            }
            out.close();
            in.close();
            success = true;
        } catch (IOException ex) {
            XohmLogger.debugPrintln("Failed to save the file at " + destFile.getAbsolutePath());
            ex.printStackTrace();
        }
        return success;
    }
}
