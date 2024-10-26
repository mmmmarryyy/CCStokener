package com.monkygames.sc2bob.io;

import java.io.*;
import java.net.*;

/**
 * Handles downloading a file from a server.
 * Used for downloading the data sychronously or asychronously (through a thread).
 * TODO: need to develop a way to get status.
 * @version 1.0
 */
public class FileDownloader extends Thread {

    private String src;

    private String srcURL;

    public String dest;

    /**
     * Creates a new FileDownloader with the specified args.
     * @param srcURL the source URL up to .com/.edu/.net etc.
     * @param src the rest of the past to the file to be downloaded.
     * @param dest the file path to write the file to.
     **/
    public FileDownloader(String srcURL, String src, String dest) {
        this.srcURL = srcURL;
        this.src = src;
        this.dest = dest;
    }

    public void download() {
        try {
            URL url = new URL(srcURL + src);
            URLConnection urlc = url.openConnection();
            InputStream is = urlc.getInputStream();
            BufferedInputStream bis = new BufferedInputStream(is);
            FileOutputStream fos = new FileOutputStream(dest);
            BufferedOutputStream bos = new BufferedOutputStream(fos);
            byte[] buffer = new byte[1000000];
            int readSize;
            readSize = bis.read(buffer);
            while (readSize > 0) {
                bos.write(buffer, 0, readSize);
                readSize = bis.read(buffer);
            }
            bos.close();
            fos.close();
            bis.close();
            is.close();
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    public void run() {
        download();
    }
}
