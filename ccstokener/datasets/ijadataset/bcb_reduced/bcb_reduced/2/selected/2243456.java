package net.cryff.io;

import java.io.BufferedOutputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.URL;
import java.net.URLConnection;
import java.io.*;

/**
 * The <code>Download</code> class
 * is a simple Thread to download
 * a file. 
 * <br>
 * I don't know if the max
 * and current variables are working yet
 * but they shall enable the preview
 * of a percentage bar.
 * TODO testing of the percentage
 * 
 * @author Nino Wagensonner
 * @version 1.0 06/05/2008
 * @since CFF V.0.1r-2
 */
public class Download extends Thread {

    private String source;

    private String path;

    /**
	 * Default Constructor
	 * @param source the source of the download in String format, e.g http://www.google.de/index.html
	 * @param path where the file shall be saved, e.g. /home/index.html
	 */
    public Download(String source, String path) {
        this.source = source;
        this.path = path;
    }

    /**
	 * Method to start the Download Thread
	 */
    public void run() {
        downloadFile(source, path);
    }

    /**
	 * the filesize
	 */
    public static long max = 0;

    /**
	 * the numbers of bits that have already been downloaded
	 */
    public static long current = 0;

    private void downloadFile(String source, String path) {
        File f = new File(path);
        if (!f.exists()) {
            File f2 = new File(f.getParent());
            f2.mkdir();
        }
        OutputStream out = null;
        URLConnection urlc = null;
        InputStream in = null;
        try {
            URL url = new URL(source);
            out = new BufferedOutputStream(new FileOutputStream(path));
            urlc = url.openConnection();
            max = urlc.getContentLength();
            in = urlc.getInputStream();
            byte[] buffer = new byte[1024];
            int numRead;
            long numWritten = 0;
            while ((numRead = in.read(buffer)) != -1) {
                out.write(buffer, 0, numRead);
                numWritten += numRead;
                current = numWritten;
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
                ioe.printStackTrace();
            }
        }
    }
}
