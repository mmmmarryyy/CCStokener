package com.wantmeet.castloader;

import java.io.BufferedInputStream;
import java.io.File;
import java.io.FileOutputStream;
import java.net.URL;
import java.net.URLConnection;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

public class FileDownLoadThread implements Runnable {

    String urlStr = null;

    String saveAsFileName = null;

    private static final Log log = LogFactory.getLog(FileDownLoadThread.class);

    public FileDownLoadThread(String urlStr, String saveAsFileName) {
        this.urlStr = urlStr;
        this.saveAsFileName = saveAsFileName;
    }

    public void run() {
        try {
            url2SaveAsFile(urlStr, saveAsFileName);
        } catch (Exception e) {
            log.error("Error in downloading file.");
        }
    }

    public void url2SaveAsFile(String urlStr, String saveAsFileName) throws Exception {
        URL url = new URL(urlStr);
        URLConnection uc = url.openConnection();
        File f = new File(saveAsFileName);
        log.info("Downloading from " + urlStr);
        if (!f.exists()) {
            FileOutputStream fos = new FileOutputStream(f);
            BufferedInputStream bis = new BufferedInputStream(uc.getInputStream());
            byte[] buffer = new byte[4096];
            int readCount = 0;
            while ((readCount = bis.read(buffer)) != -1) {
                fos.write(buffer, 0, readCount);
            }
            fos.flush();
            fos.close();
            bis.close();
        }
    }
}
