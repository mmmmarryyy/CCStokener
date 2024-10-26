package org.simpleframework;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.HttpURLConnection;
import java.net.InetAddress;
import java.net.URL;
import java.net.UnknownHostException;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * @author Yo
 * 
 * @version $Revision: $, $Date: $, $Name: $
 */
public class ClientTest {

    private static final ExecutorService executorService = new ThreadPoolExecutor(200, 200, 10, TimeUnit.SECONDS, new LinkedBlockingQueue<Runnable>());

    private String surl;

    public ClientTest(String host) {
        surl = "http://" + host + ":9001";
        System.out.println(surl);
    }

    /**
   * @param count
   * @throws InterruptedException
   */
    public void run(int count) throws InterruptedException {
        final CountDownLatch latch = new CountDownLatch(count);
        final AtomicInteger errors = new AtomicInteger(0);
        final AtomicInteger oks = new AtomicInteger(0);
        Runnable r = new Runnable() {

            public void run() {
                int read = 0;
                int totalRead = 0;
                HttpURLConnection conn = null;
                InputStream in = null;
                OutputStream out = null;
                try {
                    URL url = new URL(surl);
                    conn = (HttpURLConnection) url.openConnection();
                    conn.setDoOutput(true);
                    out = conn.getOutputStream();
                    out.write("some text".getBytes());
                    in = conn.getInputStream();
                    byte[] buffer = new byte[4096];
                    while ((read = in.read(buffer)) > 0) {
                        totalRead += read;
                    }
                    if (totalRead != conn.getContentLength()) {
                        System.out.println("KO " + Thread.currentThread().getName() + " " + conn.getContentLength() + " " + totalRead + " " + read);
                        errors.incrementAndGet();
                    } else {
                        oks.incrementAndGet();
                    }
                } catch (Exception ex) {
                    errors.incrementAndGet();
                    System.out.println(">>> " + Thread.currentThread().getName() + " " + ex.getMessage());
                } finally {
                    if (out != null) {
                        try {
                            out.close();
                        } catch (IOException ex) {
                            ex.printStackTrace();
                        }
                    }
                    if (in != null) {
                        try {
                            in.close();
                        } catch (IOException ex) {
                            ex.printStackTrace();
                        }
                    }
                    conn.disconnect();
                    latch.countDown();
                    long c = latch.getCount();
                    if (c % 50 == 0) {
                        System.out.println(c);
                    } else {
                        System.out.print('.');
                    }
                }
            }
        };
        for (int i = 0; i < count; ++i) {
            executorService.execute(r);
        }
        latch.await();
        System.out.println("OKS: " + oks.intValue() + " ERRORS: " + errors.intValue());
    }

    public static void main(String[] args) throws InterruptedException, UnknownHostException {
        String host = args.length == 0 ? InetAddress.getLocalHost().getHostName() : args[0];
        ClientTest test = new ClientTest(host);
        test.run(10);
        System.exit(0);
    }
}
