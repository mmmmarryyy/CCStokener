package com.gm.core.io.download;

import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;
import java.io.RandomAccessFile;
import java.net.HttpURLConnection;
import java.net.MalformedURLException;
import java.net.URL;
import java.util.Date;

public class DT extends Thread {

    String urlt;

    int startl;

    int end;

    String fileName;

    RandomAccessFile osf;

    public DT(int i, String url, String fileName, int start, int end) {
        this.setName("t" + i);
        this.urlt = url;
        this.fileName = fileName;
        this.startl = start;
        this.end = end;
    }

    public void run() {
        try {
            osf = new RandomAccessFile(fileName, "rw");
            URL url = new URL(urlt);
            HttpURLConnection http2 = (HttpURLConnection) url.openConnection();
            http2.setRequestProperty("User-Agent", "NetFox");
            http2.setRequestProperty("RANGE", "bytes=" + startl + "-");
            osf.seek(startl);
            InputStream input = http2.getInputStream();
            byte b[] = new byte[1024];
            Date d = new Date();
            int l;
            int i;
            l = 0;
            System.out.println(this.getName() + " 开始下载。。。");
            while ((i = input.read(b, 0, 1024)) != -1 && l < end) {
                osf.write(b, 0, i);
                b = new byte[1024];
                l += i;
            }
            Date d2 = new Date();
            System.out.println(this.getName() + " 线程耗时： " + (d2.getTime() - d.getTime()) / 1000 + " 秒,实际共下载：" + l + "字节");
        } catch (FileNotFoundException e1) {
            e1.printStackTrace();
        } catch (MalformedURLException e) {
            e.printStackTrace();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }
}
