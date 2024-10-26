package com.jawspeak.fetcher;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.net.URL;
import java.util.*;

/**
 * User: jwolter
 * Date: Nov 26, 2007
 */
public class Downloader {

    protected Map<URL, String> dowloadURLsAsStrings(Set<URL> urls) {
        Map<URL, String> map = new HashMap<URL, String>();
        for (URL url : urls) {
            try {
                String str = downloadURLtoString(url);
                map.put(url, str);
            } catch (IOException e) {
                System.err.println("An error occured downloading a URL: " + url + " " + e.getMessage());
            }
        }
        return map;
    }

    protected String downloadURLtoString(URL url) throws IOException {
        BufferedReader in = new BufferedReader(new InputStreamReader(url.openStream()));
        StringBuffer sb = new StringBuffer(100 * 1024);
        String str;
        while ((str = in.readLine()) != null) {
            sb.append(str);
        }
        in.close();
        return sb.toString();
    }

    protected void downloadMediaFiles(Set<URL> toDownload) {
        int defaultMaxSimultaneousDownloads = 3;
        List<Thread> throttlingList = new ArrayList<Thread>();
        String outputDirectory = "ram_downloads";
        for (URL url : toDownload) {
            try {
                url = new URL(downloadURLtoString(url));
                Thread t = new Thread(new Wgetter(url, outputDirectory));
                t.start();
                throttlingList.add(t);
                if (throttlingList.size() > defaultMaxSimultaneousDownloads) {
                    Thread.sleep(100);
                    System.out.println("throttling downloads until some finish");
                    for (Thread thread : throttlingList) {
                        thread.join();
                    }
                    throttlingList.clear();
                }
            } catch (IOException e) {
                e.printStackTrace();
            } catch (InterruptedException e) {
                e.printStackTrace();
            }
        }
    }

    protected class Wgetter implements Runnable {

        URL url;

        String outputDir;

        public Wgetter(URL url, String outputDir) {
            this.url = url;
            this.outputDir = outputDir;
        }

        public void run() {
            System.out.println("wgetter running");
            String[] commands = new String[] { "wget", url.toString(), "-P" + outputDir };
            Process child = null;
            try {
                child = Runtime.getRuntime().exec(commands);
                System.out.println("wgetter will start to download " + url.toString());
                child.waitFor();
                System.out.println("  download complete for " + url.toString());
            } catch (InterruptedException e) {
                e.printStackTrace();
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
    }
}
