package gov.lanl.permalink;

import java.io.ByteArrayOutputStream;
import java.io.InputStream;
import java.net.HttpURLConnection;
import java.net.URL;
import java.util.concurrent.BlockingQueue;

public class RecordPuller extends Thread {

    private String strurl;

    private BlockingQueue q;

    boolean done = false;

    String name = "";

    int total = 0;

    int total1 = 0;

    int total2 = 0;

    int total3 = 0;

    int total4 = 0;

    int total5 = 0;

    int total6 = 0;

    int total7 = 0;

    int total8 = 0;

    public RecordPuller(String name, BlockingQueue<String> q) {
        this.q = q;
        this.name = name;
    }

    public void run() {
        try {
            while (!done) {
                String strurl = (String) q.take();
                if (strurl.equals("done")) {
                    done = true;
                } else {
                    long start1 = System.currentTimeMillis();
                    readStream(strurl);
                    long end1 = System.currentTimeMillis();
                    double elapsed = end1 - start1;
                    total = total + 1;
                    if (elapsed <= 500) {
                        total1 = total1 + 1;
                    } else if (elapsed >= 500 && elapsed < 1000) {
                        total2 = total2 + 1;
                    } else if (elapsed >= 1000 && elapsed < 1200) {
                        total3 = total3 + 1;
                    } else if (elapsed >= 1200 && elapsed < 2000) {
                        total4 = total4 + 1;
                    } else if (elapsed >= 2000 && elapsed < 5000) {
                        total5 = total5 + 1;
                    } else if (elapsed >= 10000 && elapsed < 14000) {
                        total6 = total6 + 1;
                    } else if (elapsed >= 14000 && elapsed < 20000) {
                        total7 = total7 + 1;
                    } else if (elapsed >= 20000) {
                        total8 = total8 + 1;
                    }
                }
            }
        } catch (Exception e) {
            System.err.println("Exception in Consumer");
        }
    }

    public void readStream(String strurl) throws Exception {
        URL url = new URL(strurl);
        HttpURLConnection huc = (HttpURLConnection) (url.openConnection());
        int code = huc.getResponseCode();
        ByteArrayOutputStream out = new ByteArrayOutputStream();
        if (code == 200) {
            InputStream is = huc.getInputStream();
            byte[] bytes = new byte[1024];
            int len;
            while ((len = is.read(bytes)) != -1) {
                out.write(bytes, 0, len);
            }
            out.close();
            is.close();
        } else {
            System.err.println("An error of type " + code + " occurred for:" + strurl);
        }
    }
}
