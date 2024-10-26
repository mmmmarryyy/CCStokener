package net.sf.jood.download.http;

import java.io.BufferedInputStream;
import java.io.BufferedOutputStream;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.net.HttpURLConnection;
import java.net.MalformedURLException;
import java.net.URL;
import net.sf.jood.download.Download;
import net.sf.jood.download.DownloadException;

/**
 * An implementation of the abstract {@code Download} class for the HTTP
 * protocol
 * 
 * @author Firas Al Khalil
 * 
 */
public class HttpDownload extends Download {

    private URL url = null;

    private HttpURLConnection connection = null;

    private boolean append = false;

    public HttpDownload(String path, File file) throws MalformedURLException {
        super(file);
        this.url = new URL(path);
    }

    @Override
    public void start() throws DownloadException {
        try {
            if (getFile().exists()) if (getFile().isFile()) this.append = true; else return; else getFile().createNewFile();
            if (isDownloading() == false) setDownloading(true);
            this.connection = (HttpURLConnection) url.openConnection();
            this.connection.setRequestProperty("Range", "bytes=" + getFile().length() + "-");
            this.connection.connect();
            setContentType(this.connection.getContentType());
            setSize(this.connection.getContentLength());
            if (getSize() == -1) setResumable(false); else setResumable(true);
            if (getSize() <= getFile().length() && getSize() != -1) {
                setDownloading(false);
                return;
            } else {
                new Thread(this).start();
            }
        } catch (IOException ex) {
            throw new DownloadException(ex.getMessage());
        } finally {
            speedTimer.cancel();
        }
    }

    @Override
    public void stop() {
        setDownloading(false);
        speedTimer.cancel();
    }

    @Override
    public void run() {
        try {
            BufferedInputStream bis = new BufferedInputStream(connection.getInputStream());
            BufferedOutputStream bos = new BufferedOutputStream(new FileOutputStream(getFile(), append));
            byte[] buffer = new byte[1024];
            int downloaded = 0;
            while (isDownloading() && ((downloaded = bis.read(buffer)) != -1)) {
                bos.write(buffer, 0, downloaded);
                bytesDownloaded += (long) downloaded;
            }
            bos.close();
            bis.close();
        } catch (IOException ex) {
            ex.printStackTrace();
        } finally {
            speedTimer.cancel();
        }
    }
}
