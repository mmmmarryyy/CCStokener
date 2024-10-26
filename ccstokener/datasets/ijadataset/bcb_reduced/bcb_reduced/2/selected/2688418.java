package cn.edu.bit.ss.spider;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.InetAddress;
import java.net.URL;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import cn.edu.bit.dto.Source;
import cn.edu.bit.ss.SpiderMan;
import cn.edu.bit.ss.util.Queue;

public class BroadPrioSpider extends Thread {

    private Integer sleepTime;

    private Queue<String> urlPool;

    private Queue<Source> sourcePool;

    private int timeout;

    private String defaultEncoding;

    private String encoding;

    private static final Log logger = LogFactory.getLog(BroadPrioSpider.class);

    public BroadPrioSpider() {
    }

    public BroadPrioSpider(Integer sleepTime, Queue<String> urlPool, Queue<Source> sourcePool, int timeout, String defaultEncoding) {
        super();
        this.sleepTime = sleepTime;
        this.urlPool = urlPool;
        this.sourcePool = sourcePool;
        this.timeout = timeout;
        this.defaultEncoding = defaultEncoding;
    }

    public Integer getSleepTime() {
        return sleepTime;
    }

    public void setSleepTime(Integer sleepTime) {
        this.sleepTime = sleepTime;
    }

    private String getUrl() throws Exception {
        synchronized (urlPool) {
            while (SpiderMan.isWorking() && urlPool.isEmpty()) {
                urlPool.wait();
            }
            String url = urlPool.getNext();
            return url;
        }
    }

    private String getPageEncoding(HttpURLConnection connection) throws IOException {
        String encoding = null;
        encoding = connection.getContentEncoding();
        if (encoding == null) {
            BufferedReader reader = new BufferedReader(new InputStreamReader(connection.getInputStream()));
            StringBuffer sb = new StringBuffer();
            String in = null;
            int loop = 0;
            while (loop++ != 7) {
                in = reader.readLine();
                if (in == null) break;
                sb.append(in);
            }
            reader.close();
            encoding = getCharset(sb.toString());
        }
        return encoding;
    }

    private String getCharset(String sb) {
        sb = sb.toLowerCase();
        String[] result = sb.split("charset");
        if (result == null || result.length < 1) return null;
        int start = result[1].indexOf('=');
        int end = result[1].indexOf('\"');
        return result[1].substring(start + 1, end).trim();
    }

    public String getSource(String urlAdd) throws Exception {
        HttpURLConnection urlConnection = null;
        URL url = new URL(urlAdd);
        urlConnection = (HttpURLConnection) url.openConnection();
        urlConnection.setConnectTimeout(timeout);
        if (!urlConnection.getContentType().contains("text/html")) {
            throw new Exception();
        }
        if (urlConnection.getResponseCode() != 200) {
            throw new Exception();
        }
        encoding = getPageEncoding(urlConnection);
        if (encoding == null) {
            encoding = defaultEncoding;
        }
        InputStream in = url.openStream();
        byte[] buffer = new byte[12288];
        StringBuffer sb = new StringBuffer();
        int bytesRead = 0;
        while ((bytesRead = in.read(buffer)) != -1) {
            String reads = new String(buffer, 0, bytesRead, encoding);
            sb.append(reads);
        }
        in.close();
        return sb.toString();
    }

    private boolean addSource(Source source) throws Exception {
        synchronized (sourcePool) {
            while (sourcePool.isFull()) {
                return false;
            }
            sourcePool.add(source);
            sourcePool.notifyAll();
            System.out.println("sprider notifyall!");
            return true;
        }
    }

    private boolean checkIp(String url) throws Exception {
        InetAddress address = InetAddress.getByName(url);
        return SpiderMan.isIpAllowed(address.getAddress());
    }

    private String getHost(String url) {
        url = url.substring(7);
        if (url.indexOf("/") != -1) {
            return url.substring(0, url.indexOf("/"));
        }
        return url;
    }

    public void run() {
        Source source = null;
        while (SpiderMan.isWorking()) {
            try {
                source = new Source();
                source.setUrl(getUrl());
                if (!checkIp(getHost(source.getUrl()))) {
                    continue;
                }
                source.setSource(getSource(source.getUrl()));
                source.setEncoding(encoding);
                if (!addSource(source)) {
                    sleep(sleepTime);
                }
            } catch (Exception e) {
                logger.error("SomeError occurred, abort this while, continuing...");
                continue;
            }
        }
    }
}
