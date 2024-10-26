package com.weespers.download;

import java.io.BufferedInputStream;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.net.URL;
import java.net.URLConnection;
import com.smaxe.uv.fformat.FlvTool;
import com.weespers.util.LogUtil;

public abstract class FastDownloader {

    public static void main(String[] args) throws Exception {
        String dir = "/Users/dimitrioskolovos/Desktop/";
        String videoId = "KdrVkPBy5nw";
        RealVideoExtractor extractor = new RealVideoExtractor(videoId);
        RealVideo realVideo = extractor.getHighQualityRealVideo();
        String url = realVideo.getUrl();
        System.err.println(url);
        FastDownloader downloader = new FastDownloader(url, dir + videoId + ".flv") {

            @Override
            public void downloaded(int bytes) {
                System.err.println("Downloaded " + bytes + " of " + getSize());
            }

            @Override
            public void done() {
                System.err.println("Done");
            }
        };
        downloader.download();
    }

    protected String location;

    protected String target;

    protected int size = -1;

    protected boolean canceled = false;

    protected int bufferSize = 512;

    protected int chunkSize = 1000000;

    public FastDownloader() {
    }

    public int getBufferSize() {
        return bufferSize;
    }

    public FastDownloader(String location, String target) {
        this.location = location;
        this.target = target;
    }

    public boolean isCanceled() {
        return canceled;
    }

    public void setCanceled(boolean canceled) {
        this.canceled = canceled;
    }

    public String getTarget() {
        return target;
    }

    public void setTarget(String target) {
        this.target = target;
    }

    public String getLocation() {
        return location;
    }

    public void setLocation(String location) {
        this.location = location;
        size = -1;
    }

    public int getSize() {
        if (size == -1) {
            try {
                URL url = new URL(location);
                URLConnection connection = url.openConnection();
                size = connection.getContentLength();
            } catch (Exception ex) {
                size = 0;
            }
        }
        return size;
    }

    public boolean download(int attempts) {
        for (int i = 0; i < attempts; i++) {
            if (download()) return true; else {
                try {
                    Thread.sleep(1000);
                } catch (Exception ex) {
                }
            }
        }
        return false;
    }

    public boolean download() {
        BufferedInputStream bis = null;
        FileOutputStream fos = null;
        try {
            URL url = new URL(location);
            int chunks = (getSize() / chunkSize) + 1;
            fos = new FileOutputStream(new File(target));
            for (int chunk = 0; chunk < chunks; chunk++) {
                int downloaded = 0;
                URLConnection connection = url.openConnection();
                if (chunk > 0) {
                    connection.addRequestProperty("Range", "bytes=" + (chunk * chunkSize) + "-");
                }
                bis = new BufferedInputStream(connection.getInputStream());
                StringBuffer sb = new StringBuffer();
                byte[] buffer = new byte[bufferSize];
                int r = bis.read(buffer);
                int total = r;
                while (r != -1) {
                    boolean shouldBreak = false;
                    if ((downloaded + r) > chunkSize) {
                        r = chunkSize - downloaded;
                        shouldBreak = true;
                    }
                    if (r == bufferSize) {
                        fos.write(buffer);
                    } else {
                        for (int i = 0; i < r; i++) {
                            fos.write(buffer[i]);
                        }
                    }
                    if (isCanceled()) {
                        return true;
                    } else if (r != -1) {
                        downloaded += r;
                        total += r;
                        downloaded(total);
                    }
                    if (shouldBreak) {
                        break;
                    }
                    r = bis.read(buffer);
                }
                bis.close();
            }
            done();
            return true;
        } catch (Exception ex) {
            ex.printStackTrace();
            return false;
        } finally {
            try {
                fos.flush();
                fos.close();
            } catch (IOException ex) {
                ex.printStackTrace();
            }
        }
    }

    public abstract void downloaded(int bytes);

    public abstract void done();
}
