package com.xmxsuperstar.app.gg;

import java.io.BufferedInputStream;
import java.io.RandomAccessFile;
import java.net.URL;
import java.net.URLConnection;
import java.util.concurrent.CountDownLatch;

public class DownloadThread implements Runnable {

    private CountDownLatch latch;

    private URL url;

    private RandomAccessFile file;

    private long begin;

    private long end;

    public DownloadThread(CountDownLatch latch, URL url, RandomAccessFile file, long begin, long end) {
        this.latch = latch;
        this.url = url;
        this.file = file;
        this.begin = begin;
        this.end = end;
    }

    @Override
    public void run() {
        try {
            long pos = begin;
            byte[] buf = new byte[1024];
            URLConnection cn = url.openConnection();
            Utils.setHeader(cn);
            cn.setRequestProperty("Range", "bytes=" + begin + "-" + end);
            BufferedInputStream bis = new BufferedInputStream(cn.getInputStream());
            int len;
            while ((len = bis.read(buf)) > 0) {
                synchronized (file) {
                    file.seek(pos);
                    file.write(buf, 0, len);
                }
                pos += len;
                Statics.getInstance().addComleted(len);
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
        latch.countDown();
    }
}
