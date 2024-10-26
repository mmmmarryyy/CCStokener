package ua.com.stormlabs.jsitemapper;

import org.apache.log4j.Logger;
import org.apache.commons.lang.StringUtils;
import org.apache.commons.io.IOUtils;
import java.net.*;
import java.io.IOException;
import java.io.InputStream;
import java.util.List;

/**
 * User: storm
 * Date: 26.03.2009
 */
public class Crawler {

    private static final Logger log = Logger.getLogger(Crawler.class);

    private String proxyHost;

    private Integer proxyPort;

    private Thread thread;

    private final int id;

    private long pause;

    public Crawler(int id) {
        this.id = id;
    }

    public void setPause(long pause) {
        this.pause = pause;
    }

    public void setProxyHost(String proxyHost) {
        this.proxyHost = proxyHost;
    }

    public void setProxyPort(int proxyPort) {
        this.proxyPort = proxyPort;
    }

    public void go() {
        log.info("Starting crawler #" + id);
        thread = new Thread() {

            @Override
            public void run() {
                try {
                    crawl();
                } catch (InterruptedException e) {
                    log.error("Crawler #" + id + " is interrupted");
                }
                log.info("Crawler #" + id + " is done");
            }
        };
        thread.start();
        log.info("Crawler #" + id + " started");
    }

    private void crawl() throws InterruptedException {
        LinkPool linkPool = LinkPool.getInstance();
        if (linkPool.isEmpty()) {
            log.info("LinkPool is empty");
            return;
        }
        while (true) {
            URL url = linkPool.getAwaiting();
            while (url == null && !linkPool.isEmpty()) {
                log.debug("No awaiting links, but linkPool is not empty. Let's wait.");
                Thread.sleep(1000L);
                url = linkPool.getAwaiting();
            }
            if (url == null) {
                log.info("Failed to get awaiting link. LinkPool is empty now");
                return;
            }
            try {
                process(url);
                linkPool.addMapped(url);
            } catch (IOException e) {
                log.error("Failed to process URL " + url + ". Error: " + e, e);
                linkPool.addFailed(url);
            }
            if (pause != 0) {
                Thread.sleep(pause);
            }
        }
    }

    private void process(URL url) throws IOException {
        log.debug("Crawler #" + id + " processing URL " + url);
        long tt = System.currentTimeMillis();
        String html = fetchHtml(url);
        log.debug("HTML fetched in " + (System.currentTimeMillis() - tt) + " ms. Size: " + html.length() + " bytes");
        List<String> links = LinkExtractor.getInstance().getLinks(html, url.toExternalForm());
        log.debug("Extracted " + links.size() + " links from URL " + url);
        int addedCount = 0;
        for (String link : links) {
            boolean added = LinkPool.getInstance().addAwaiting(link);
            if (added) addedCount++;
        }
        log.debug("Added " + addedCount + " links from URL " + url);
    }

    private String fetchHtml(URL url) throws IOException {
        URLConnection connection;
        if (StringUtils.isNotBlank(proxyHost) && proxyPort != null) {
            Proxy proxy = new Proxy(Proxy.Type.HTTP, InetSocketAddress.createUnresolved(proxyHost, proxyPort));
            connection = url.openConnection(proxy);
        } else {
            connection = url.openConnection();
        }
        Object content = connection.getContent();
        if (content instanceof InputStream) {
            return IOUtils.toString(InputStream.class.cast(content));
        } else {
            String msg = "Bad content type! " + content.getClass();
            log.error(msg);
            throw new IOException(msg);
        }
    }

    public void stop() {
        log.info("Stopping crawler #" + id);
        try {
            if (thread.isAlive()) {
                log.debug("Interrupting...");
                thread.interrupt();
                Thread.sleep(5000L);
            }
            if (thread.isAlive()) {
                log.warn("Failed to interrupt. Stopping...");
                thread.stop();
                Thread.sleep(5000L);
            }
            if (thread.isAlive()) {
                log.error("Failed to stop crawler #" + id);
            } else {
                log.info("Crawler #" + id + " is stopped");
            }
        } catch (InterruptedException e) {
            log.error(e, e);
        }
    }

    public void join() {
        try {
            thread.join();
        } catch (InterruptedException e) {
            log.error("Join interrupted!");
        }
    }
}
