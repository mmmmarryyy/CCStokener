package com.sts.webmeet.tests.server;

import java.net.*;
import java.io.*;
import javax.net.ssl.*;
import java.util.Date;
import java.lang.reflect.*;

public class HTTPSTest extends Thread {

    public static void main(String[] args) throws Exception {
        if (args.length < 4) {
            System.out.println("usage: HTTPSTest" + " url... pause threads hits");
            System.exit(1);
        }
        String strVendor = System.getProperty("java.vendor");
        if (-1 < strVendor.indexOf("Microsoft")) {
            try {
                Class clsFactory = Class.forName("com.ms.net.wininet.WininetStreamHandlerFactory");
                if (null != clsFactory) URL.setURLStreamHandlerFactory((URLStreamHandlerFactory) clsFactory.newInstance());
            } catch (ClassNotFoundException cfe) {
                throw new Exception("Unable to load the Microsoft SSL " + "stream handler.  Check classpath." + cfe.toString());
            }
        } else {
            HttpsURLConnection.setDefaultHostnameVerifier(new HostnameVerifier() {

                public boolean verify(String hostname, SSLSession session) {
                    return true;
                }
            });
        }
        String[] theUrls = new String[args.length - 3];
        System.arraycopy(args, 0, theUrls, 0, theUrls.length);
        bContinue = true;
        int iDelay = Integer.parseInt(args[args.length - 3]);
        int iThreads = Integer.parseInt(args[args.length - 2]);
        int iHits = Integer.parseInt(args[args.length - 1]);
        HTTPSTest[] testers = new HTTPSTest[iThreads];
        Date dateStart = new Date();
        for (int i = 0; i < iThreads; i++) {
            testers[i] = new HTTPSTest(theUrls, iDelay, iHits);
        }
        System.out.println(iThreads + " threads created.");
        for (int i = 0; i < iThreads; i++) {
            testers[i].start();
        }
        System.out.println(iThreads + " threads started.");
        if (iHits == -1) {
            System.in.read();
            bContinue = false;
        }
        for (int j = 0; j < iThreads; j++) {
            testers[j].join();
        }
        Date dateFinish = new Date();
        long lElapsed = dateFinish.getTime() - dateStart.getTime();
        System.out.println(lReadTotal + " bytes read in " + (lElapsed / 1000) + " seconds (" + (lReadTotal / lElapsed) + "KB/sec)");
        System.out.println("(" + (((lReadTotal * 8) / lElapsed) / 1000) + "mbps)");
        System.out.println(lTotalRequests + " requests handled in " + (lElapsed / 1000) + " seconds (" + ((lTotalRequests / lElapsed) / 1000) + " requests/sec)");
        System.out.println("Maximum response time: " + (lMaxRequestTime) + " milliseconds.");
        System.out.println("Minimum response time: " + (lMinRequestTime) + " milliseconds.");
        if (iHits != -1) {
            long lAverageAverage = 0L;
            for (int i = 0; i < testers.length; i++) {
                lAverageAverage += testers[i].getAverage();
            }
            System.out.println("Average Response time: " + lAverageAverage / testers.length + " milliseconds.");
        }
    }

    private URLConnection urlConn;

    private InputStream is;

    private String[] urls;

    private int iPause;

    public static boolean bContinue;

    public static long lReadTotal;

    private long lRead;

    private long lRequests;

    private long lMaxRequest;

    private long lMinRequest = Long.MAX_VALUE;

    private long lAverage;

    private int iHits;

    private long[] alResponseTimes;

    public static long lMaxRequestTime;

    public static long lMinRequestTime = Long.MAX_VALUE;

    public static long lTotalRequests;

    static Object objClassLock = new Object();

    public HTTPSTest(String[] urls, int iPause, int iHits) {
        this.urls = urls;
        this.iPause = iPause;
        this.iHits = iHits;
        if (iHits > 0) {
            alResponseTimes = new long[iHits];
        }
    }

    public void run() {
        try {
            long lTime = 0L;
            long lAvg = 0L;
            for (int j = 0; bContinue && (iHits == -1 || j < iHits); j++) {
                lAvg = 0L;
                for (int i = 0; i < urls.length; i++) {
                    Date reqStart = new Date();
                    urlConn = (new URL(urls[i])).openConnection();
                    urlConn.setUseCaches(false);
                    urlConn.setAllowUserInteraction(true);
                    urlConn.connect();
                    is = urlConn.getInputStream();
                    lRead += drainStream(is);
                    Date reqStop = new Date();
                    lTime = reqStop.getTime() - reqStart.getTime();
                    lAvg += lTime;
                    lMaxRequest = Math.max(lMaxRequest, lTime);
                    lMinRequest = Math.min(lMinRequest, lTime);
                    lRequests++;
                    Thread.currentThread().sleep(iPause);
                }
                if (iHits > 0) {
                    alResponseTimes[j] = lAvg / urls.length;
                }
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
        for (int i = 0; i < iHits; i++) {
            lAverage += alResponseTimes[i];
        }
        lAverage /= iHits;
        synchronized (objClassLock) {
            lReadTotal += lRead;
            lTotalRequests += lRequests;
            lMaxRequestTime = Math.max(lMaxRequestTime, lMaxRequest);
            lMinRequestTime = Math.min(lMinRequestTime, lMinRequest);
        }
        lReadTotal += lRead;
    }

    public long getAverage() {
        return lAverage;
    }

    public static int drainStream(InputStream is) throws IOException {
        byte[] ba = new byte[4096];
        int iRead = 0;
        int iLastRead = 0;
        for (iLastRead = is.read(ba); iLastRead > -1; iLastRead = is.read(ba)) {
            iRead += iLastRead;
        }
        return iRead;
    }
}
