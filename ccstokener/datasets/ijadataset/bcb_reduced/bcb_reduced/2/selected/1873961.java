package com.xmxsuperstar.app.gg;

import java.io.IOException;
import java.io.RandomAccessFile;
import java.net.HttpURLConnection;
import java.net.URL;
import java.net.URLConnection;
import java.util.concurrent.CountDownLatch;

public class Main {

    private static final int THREAD_COUNT = 5;

    /**
	 * @param args
	 * @throws Exception
	 */
    public static void main(String[] args) throws Exception {
        if (args.length < 1) {
            printUseage();
        } else {
            download(args[0]);
        }
    }

    private static void download(String urlString) throws IOException {
        URL url = new URL(urlString);
        url = handleRedirectUrl(url);
        URLConnection cn = url.openConnection();
        Utils.setHeader(cn);
        long fileLength = cn.getContentLength();
        Statics.getInstance().setFileLength(fileLength);
        long packageLength = fileLength / THREAD_COUNT;
        long leftLength = fileLength % THREAD_COUNT;
        String fileName = Utils.decodeURLFileName(url);
        RandomAccessFile file = new RandomAccessFile(fileName, "rw");
        System.out.println("File: " + fileName + ", Size: " + Utils.calSize(fileLength));
        CountDownLatch latch = new CountDownLatch(THREAD_COUNT + 1);
        long pos = 0;
        for (int i = 0; i < THREAD_COUNT; i++) {
            long endPos = pos + packageLength;
            if (leftLength > 0) {
                endPos++;
                leftLength--;
            }
            new Thread(new DownloadThread(latch, url, file, pos, endPos)).start();
            pos = endPos;
        }
        new Thread(new MoniterThread(latch)).start();
        try {
            latch.await();
        } catch (InterruptedException e) {
            e.printStackTrace();
        }
    }

    private static URL handleRedirectUrl(URL url) {
        try {
            URLConnection cn = url.openConnection();
            Utils.setHeader(cn);
            if (!(cn instanceof HttpURLConnection)) {
                return url;
            }
            HttpURLConnection hcn = (HttpURLConnection) cn;
            hcn.setInstanceFollowRedirects(false);
            int resCode = hcn.getResponseCode();
            if (resCode == 200) {
                System.out.println("URL: " + url);
                return url;
            }
            String location = hcn.getHeaderField("Location");
            hcn.disconnect();
            return handleRedirectUrl(new URL(location.replace(" ", "%20")));
        } catch (IOException e) {
            e.printStackTrace();
        }
        return url;
    }

    private static void printUseage() {
        System.out.println("gg url");
    }
}
