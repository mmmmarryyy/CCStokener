package org.jma.lib.utils.http;

import java.io.File;
import java.io.FileOutputStream;
import java.io.InputStream;
import java.net.URL;
import java.net.URLConnection;
import java.util.ArrayList;

public class FileDownloader {

    private ArrayList FileDownloadedListeners = new ArrayList();

    private String repository;

    private String fileName;

    private String urlString;

    private String name;

    /**
	 * @return the name
	 */
    public String getName() {
        return name;
    }

    /**
	 * @param name the name to set
	 */
    public void setName(String name) {
        this.name = name;
    }

    public FileDownloader(String repository, String fileName, String urlString) {
        this.repository = repository;
        this.fileName = fileName;
        this.urlString = urlString;
    }

    public void run() {
        try {
            File f = new File(repository + fileName);
            if (!f.exists()) {
                URL url = new URL(urlString);
                URLConnection urlConnection = url.openConnection();
                urlConnection.connect();
                InputStream dis = url.openStream();
                File dir = new File(repository);
                if (!dir.exists()) dir.mkdirs();
                f.createNewFile();
                FileOutputStream fos = new FileOutputStream(f);
                byte[] buffer = new byte[4096];
                int len = 0;
                while ((len = dis.read(buffer)) > -1) fos.write(buffer, 0, len);
                fos.close();
                dis.close();
            }
            fireFileDownloadedListener(fileName);
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    private void fireFileDownloadedListener(String file) {
        if (!FileDownloadedListeners.isEmpty()) {
            for (int i = 0; i < FileDownloadedListeners.size(); i++) {
                FileDownloadedListener ful = (FileDownloadedListener) FileDownloadedListeners.get(i);
                ful.fileDownloaded(new FileDownloadedEvent(file, this));
            }
        }
    }

    public void addFileDownloadedListener(FileDownloadedListener ful) {
        FileDownloadedListeners.add(ful);
    }

    public void removeFileDownloadedListener(FileDownloadedListener ful) {
        FileDownloadedListeners.remove(ful);
    }
}
