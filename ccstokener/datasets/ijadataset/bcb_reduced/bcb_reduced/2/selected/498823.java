package visad.install;

import java.io.BufferedInputStream;
import java.io.BufferedOutputStream;
import java.io.File;
import java.io.FileOutputStream;
import java.io.InputStream;
import java.io.IOException;
import java.net.URL;
import java.net.URLConnection;

/**
 * Download a file from a URL to a local directory.
 */
public abstract class Download {

    /**
   * Save the file found at the URL to the specified directory.<br>
   * <br>
   * If <tt>saveFile</tt> exists and is a file, it is overwritten.
   *
   * @param url the file to download
   * @param saveFile the directory or file to which the downloaded
   *                 file is written
   * @param verbose <tt>true</tt> if a running commentary of the
   *                download's progress is desired.
   */
    public static void getFile(URL url, File saveFile, boolean verbose) {
        getFile(url, saveFile, false, verbose);
    }

    /**
   * Save the file found at the URL to the specified directory.
   *
   * @param url the file to download
   * @param saveFile the directory or file to which the downloaded
   *                 file is written
   * @param backUpExisting <tt>true</tt> if any existing <tt>saveFile</tt>
   *                       should be backed up
   * @param verbose <tt>true</tt> if a running commentary of the
   *                download's progress is desired.
   */
    public static void getFile(URL url, File saveFile, boolean backUpExisting, boolean verbose) {
        if (verbose) {
            System.err.println("Downloading " + url + " to " + saveFile);
        }
        File target;
        String baseName;
        if (!saveFile.isDirectory()) {
            target = saveFile;
            baseName = saveFile.getName();
        } else {
            File baseFile = new File(url.getFile());
            baseName = baseFile.getName();
            if (baseName.length() == 0) {
                baseName = "file";
            }
            target = new File(saveFile, baseName);
        }
        URLConnection conn;
        try {
            conn = url.openConnection();
        } catch (IOException ioe) {
            System.err.println("Couldn't open \"" + url + "\"");
            return;
        }
        if (target.exists()) {
            conn.setIfModifiedSince(target.lastModified());
        }
        if (conn.getContentLength() < 0) {
            if (verbose) {
                System.err.println(url + " is not newer than " + target);
            }
            return;
        }
        if (backUpExisting && target.exists()) {
            int idx = 0;
            while (true) {
                File tmpFile = new File(saveFile, baseName + "." + idx);
                if (!tmpFile.exists()) {
                    if (!target.renameTo(tmpFile)) {
                        System.err.println("Couldn't rename \"" + target + "\" to \"" + tmpFile + "\"");
                        target.delete();
                    }
                    break;
                }
                idx++;
            }
        }
        BufferedInputStream in;
        try {
            InputStream uIn = conn.getInputStream();
            in = new BufferedInputStream(uIn);
        } catch (IOException ioe) {
            System.err.println("Couldn't read \"" + url + "\"");
            return;
        }
        BufferedOutputStream out;
        try {
            FileOutputStream fOut = new FileOutputStream(target);
            out = new BufferedOutputStream(fOut);
        } catch (IOException ioe) {
            System.err.println("Couldn't write \"" + target + "\"");
            return;
        }
        byte[] block = new byte[1024];
        while (true) {
            int len;
            try {
                len = in.read(block);
            } catch (IOException ioe) {
                ioe.printStackTrace();
                break;
            }
            if (len < 0) {
                break;
            }
            try {
                out.write(block, 0, len);
            } catch (IOException ioe) {
                ioe.printStackTrace();
                break;
            }
        }
        try {
            out.close();
        } catch (IOException ioe) {
        }
        try {
            in.close();
        } catch (IOException ioe) {
        }
        long connMod = conn.getLastModified();
        if (connMod != 0) {
            target.setLastModified(connMod);
        }
        if (verbose) {
            System.out.println("Successfully updated " + target);
        }
    }
}
