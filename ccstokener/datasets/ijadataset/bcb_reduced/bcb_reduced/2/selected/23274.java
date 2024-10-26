package fr.ana.anaballistics.component.io.net;

import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.net.MalformedURLException;
import java.net.URL;
import java.net.URLConnection;

public class RemoteFileDownloader implements Runnable {

    private final int BUFFER_SIZE = 1024;

    private URL inputUrl;

    private String outputFile;

    private boolean isRunning, isPause, isStop, isDone, isError;

    private RemoteFileDownloaderListener rfdl;

    public RemoteFileDownloader(RemoteFileDownloaderListener rfdl) {
        inputUrl = null;
        outputFile = null;
        isRunning = false;
        isPause = false;
        isStop = false;
        isDone = false;
        isError = false;
        this.rfdl = rfdl;
    }

    public void start() {
        Thread thread = new Thread(this);
        isDone = false;
        isRunning = true;
        isStop = false;
        isPause = false;
        isError = false;
        thread.start();
    }

    @Override
    public void run() {
        if (inputUrl != null && outputFile != null) {
            try {
                URLConnection urlConn = inputUrl.openConnection();
                if (urlConn.getContentLength() == -1) {
                    if (rfdl != null) rfdl.notifyFileDownloaded(RemoteFileDownloaderListener.FILEDOWNLOADED_ERROR);
                    isError = true;
                } else {
                    InputStream inputStream = urlConn.getInputStream();
                    FileOutputStream outputStream = new FileOutputStream(outputFile);
                    byte[] buffer = new byte[BUFFER_SIZE];
                    int readedBytes, downloadBytes = 0;
                    if (rfdl != null) {
                        rfdl.notifyFileSize(urlConn.getContentLength());
                        rfdl.notifyFileType(urlConn.getContentType());
                        rfdl.notifyCurrentDownloadedBytes(0);
                    }
                    while (!isStop && !isDone) {
                        while (isPause) Thread.yield();
                        downloadBytes += (readedBytes = inputStream.read(buffer));
                        if (readedBytes > 0) {
                            outputStream.write(buffer, 0, readedBytes);
                            if (rfdl != null) rfdl.notifyCurrentDownloadedBytes(downloadBytes);
                        } else {
                            isDone = true;
                            if (rfdl != null) rfdl.notifyFileDownloaded(RemoteFileDownloaderListener.FILEDOWNLOADED_SUCCESS);
                        }
                    }
                    outputStream.flush();
                    outputStream.close();
                    inputStream.close();
                }
            } catch (IOException e) {
                e.printStackTrace();
                if (rfdl != null) rfdl.notifyFileDownloaded(RemoteFileDownloaderListener.FILEDOWNLOADED_ERROR);
            }
        }
        isRunning = false;
    }

    public void setUrl(String url) {
        try {
            this.inputUrl = new URL(url);
        } catch (MalformedURLException e) {
            e.printStackTrace();
        }
    }

    public void setUrl(URL url) {
        this.inputUrl = url;
    }

    public URL getUrl() {
        return inputUrl;
    }

    public String getOutputFile() {
        return outputFile;
    }

    public void setOutputFile(String outputFile) {
        this.outputFile = outputFile;
    }

    public RemoteFileDownloaderListener getRfdl() {
        return rfdl;
    }

    public void setRfdl(RemoteFileDownloaderListener rfdl) {
        this.rfdl = rfdl;
    }

    public boolean isRunning() {
        return isRunning;
    }

    public boolean isPause() {
        return isPause;
    }

    public boolean isStop() {
        return isStop;
    }

    public boolean isDone() {
        return isDone;
    }

    public boolean isError() {
        return isError;
    }

    public void pause() {
        isPause = true;
    }

    public void resume() {
        isPause = false;
    }

    public void stop() {
        isStop = true;
    }
}
