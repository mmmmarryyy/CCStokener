package org.nlsde.ipv6.sengine.download;

import java.io.BufferedOutputStream;
import java.io.BufferedReader;
import java.io.File;
import java.io.FileOutputStream;
import java.io.InputStreamReader;
import java.net.ConnectException;
import java.net.HttpURLConnection;
import java.net.URL;
import java.util.Vector;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import org.nlsde.ipv6.sengine.util.InfoPrint;

public class DownloadPage implements Runnable {

    private Vector<String> urlSet;

    private String savePath;

    public void setSavePath(String path) {
        savePath = path;
    }

    public void setUrlSet(Vector<String> v) {
        urlSet = v;
    }

    public Vector<String> getUrlSet() {
        return urlSet;
    }

    public synchronized void addUrl(String url) {
        assert (urlSet != null);
        long start_t = System.currentTimeMillis();
        if (!urlSet.contains(url)) urlSet.add(url);
        long end_t = System.currentTimeMillis();
        System.out.println(Thread.currentThread().getName() + " cost " + (end_t - start_t) + " ms for adding...");
    }

    public synchronized String getUrl() {
        assert (urlSet != null);
        if (urlSet.isEmpty()) return null; else return urlSet.remove(0);
    }

    /**
	 * the run
	 */
    @SuppressWarnings("deprecation")
    public void run() {
        while (true) {
            String urlstring = getUrl();
            if (urlstring == null) {
                Thread.currentThread().stop();
                return;
            }
            BufferedReader br = null;
            HttpURLConnection httpcon;
            StringBuffer sf = new StringBuffer();
            FileOutputStream fout;
            BufferedOutputStream bout;
            try {
                URL url = new URL(urlstring);
                httpcon = (HttpURLConnection) url.openConnection();
                br = new BufferedReader(new InputStreamReader(httpcon.getInputStream()));
                String temp = null;
                sf.append(urlstring + "\n");
                temp = br.readLine();
                while (temp != null) {
                    sf.append(temp);
                    String regex = "<a href=[\"]http://" + ".*?" + ">";
                    Pattern pt = Pattern.compile(regex);
                    Matcher mt = pt.matcher(temp);
                    if (mt.find()) {
                        temp = mt.group().replaceAll("<a href=|>", "");
                        temp = temp.substring(1);
                        int index = temp.indexOf("\"");
                        if (index > 0) {
                            temp = temp.substring(0, index);
                            addUrl(temp);
                        }
                    }
                    temp = br.readLine();
                }
                urlstring = urlstring.replaceAll("/|:|\\?|\\*|\"|<|>|\\|", ".");
                fout = new FileOutputStream(new File(savePath + urlstring.substring(7) + ".html"));
                bout = new BufferedOutputStream(fout);
                bout.write(sf.toString().getBytes());
                bout.flush();
                bout.close();
                fout.close();
                System.gc();
            } catch (ConnectException coe) {
                continue;
            } catch (Exception e) {
                continue;
            }
        }
    }

    public DownloadPage(Vector<String> set) {
        setUrlSet(set);
    }

    public DownloadPage(Vector<String> set, String savePath) {
        setUrlSet(set);
        setSavePath(savePath);
    }

    public static void main(String args[]) {
        String savepath = "D:\\fileS\\";
        String[] seeds = { "http://www.sina.com", "http://www.sohu.com", "http://www.cnn.com", "http://www.163.com", "http://www.qq.com", "http://www.265.com/", "http://www.yahoo.com" };
        Vector<String> set = new Vector<String>();
        for (String url : seeds) set.add(url);
        Thread prntT = new Thread(new InfoPrint(set), "print_d");
        prntT.start();
        for (int i = 0; i < seeds.length; i++) {
            DownloadPage dp = new DownloadPage(set, savepath);
            Thread t = new Thread(dp, "DownloadPage-" + i);
            t.start();
        }
    }
}
