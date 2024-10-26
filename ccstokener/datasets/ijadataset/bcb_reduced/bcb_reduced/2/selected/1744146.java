package com.weespers.download;

import java.io.BufferedInputStream;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.net.URL;
import java.net.URLConnection;

public abstract class Downloader {

    protected String location;

    protected String target;

    protected int size = -1;

    protected boolean canceled = false;

    protected int bufferSize = 2048;

    public Downloader() {
    }

    public int getBufferSize() {
        return bufferSize;
    }

    public Downloader(String location, String target) {
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
            bis = new BufferedInputStream(url.openStream());
            fos = new FileOutputStream(new File(target));
            StringBuffer sb = new StringBuffer();
            byte[] buffer = new byte[bufferSize];
            int r = bis.read(buffer);
            int total = r;
            while (r != -1) {
                if (r == bufferSize) {
                    fos.write(buffer);
                } else {
                    for (int i = 0; i < r; i++) {
                        fos.write(buffer[i]);
                    }
                }
                r = bis.read(buffer);
                if (isCanceled()) {
                    return true;
                } else if (r != -1) {
                    total += r;
                    downloaded(total);
                }
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
                bis.close();
            } catch (IOException ex) {
                ex.printStackTrace();
            }
        }
    }

    public abstract void downloaded(int bytes);

    public abstract void done();
}
