package org.statcato.file;

import java.io.*;
import java.net.*;
import java.util.Random;

/**
 * A class for downloading a file from a given URL to a file
 * in a specified directory.
 * 
 * @author Margaret Yau
 * @version %I%, %G%
 * @since 1.0
 */
public class DownloadFile {

    private String URL;

    public DownloadFile(String URL) {
        this.URL = URL;
    }

    public int download(String fileName) {
        OutputStream out = null;
        URLConnection conn = null;
        InputStream in = null;
        int numRead = 0;
        int totalRead = 0;
        try {
            URL url = new URL(URL);
            out = new BufferedOutputStream(new FileOutputStream(fileName));
            conn = url.openConnection();
            in = conn.getInputStream();
            byte[] buffer = new byte[1024];
            while ((numRead = in.read(buffer)) != -1) {
                out.write(buffer, 0, numRead);
                totalRead += numRead;
            }
        } catch (IOException exception) {
            totalRead = 0;
        } finally {
            try {
                if (in != null) {
                    in.close();
                }
                if (out != null) {
                    out.close();
                }
            } catch (IOException ioe) {
            }
            return totalRead;
        }
    }

    /**
     * Downloads file specified by the URL to a temporary file in 
     * saveDir.
     * 
     * @return string name of the download file in saveDir
     */
    public String download() {
        int totalRead = 0;
        String fileName = tempFileName();
        totalRead = download(fileName);
        if (totalRead <= 0) return null; else return fileName;
    }

    /**
     * return a temporary file name
     * 
     * @return string name of a temporary file
     */
    private String tempFileName() {
        Random generator = new Random();
        int num = Math.abs(generator.nextInt());
        return "temp" + num;
    }
}
