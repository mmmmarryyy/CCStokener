package de.shandschuh.jaolt.tools.download.core;

import java.io.File;
import java.io.FileOutputStream;
import java.io.InputStream;
import java.net.URL;
import java.net.URLConnection;
import javax.swing.JProgressBar;
import de.shandschuh.jaolt.tools.url.URLHelper;

public class SimpleFileDownloader {

    public static File downloadFile(URL url, File file, JProgressBar progressBar) throws Exception {
        URLConnection urlConnection = url.openConnection();
        urlConnection.setConnectTimeout(5000);
        int size = urlConnection.getContentLength();
        if (progressBar != null) {
            progressBar.setMinimum(0);
            progressBar.setMaximum(size);
            progressBar.setValue(0);
        }
        InputStream inputStream = urlConnection.getInputStream();
        if (file.isDirectory()) {
            file = new File(file + File.separator + url.getPath().substring(url.getPath().lastIndexOf("/")));
        }
        FileOutputStream fileOutputStream = new FileOutputStream(file);
        int read = 0;
        byte[] buffer = new byte[1024];
        while (read != -1) {
            fileOutputStream.write(buffer, 0, read);
            read = inputStream.read(buffer);
            if (progressBar != null) {
                progressBar.setValue(progressBar.getValue() + read);
            }
        }
        inputStream.close();
        fileOutputStream.close();
        return file;
    }

    public static File downloadFile(String uri, File file, JProgressBar progressBar) throws Exception {
        return downloadFile(URLHelper.contructURL(uri), file, progressBar);
    }
}
