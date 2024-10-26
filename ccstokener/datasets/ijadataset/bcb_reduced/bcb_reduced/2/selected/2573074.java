package threads;

import java.io.BufferedReader;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.net.MalformedURLException;
import java.net.URL;
import java.net.URLConnection;
import java.util.Vector;
import data.LinkDepth;
import data.Site;

public class CrawlThread extends Thread {

    private CrawlMonitor monitor;

    private Site site = null;

    private boolean idle = true;

    private boolean stopped = false;

    public CrawlThread(CrawlMonitor monitor) {
        this.monitor = monitor;
        this.setPriority(MIN_PRIORITY);
    }

    private Vector<String> getAllLinks(String page) {
        Vector<String> v = new Vector<String>();
        String[] link = page.split("src\\=");
        for (int i = 1; i < link.length; i++) {
            link[i] = link[i].split(" ", 2)[0].split(">", 2)[0];
            while (link[i].charAt(0) == '"') link[i] = link[i].substring(1);
            while (link[i].endsWith("\"")) link[i] = link[i].substring(0, link[i].length() - 1);
            while (link[i].charAt(0) == '\'') link[i] = link[i].substring(1);
            while (link[i].endsWith("\'")) link[i] = link[i].substring(0, link[i].length() - 1);
            v.add(link[i]);
        }
        link = page.split("href\\=");
        for (int i = 1; i < link.length; i++) {
            link[i] = link[i].split(" ", 2)[0].split(">", 2)[0];
            while (link[i].charAt(0) == '"') link[i] = link[i].substring(1);
            while (link[i].endsWith("\"")) link[i] = link[i].substring(0, link[i].length() - 1);
            while (link[i].charAt(0) == '\'') link[i] = link[i].substring(1);
            while (link[i].endsWith("\'")) link[i] = link[i].substring(0, link[i].length() - 1);
            v.add(link[i]);
        }
        return v;
    }

    public void run() {
        String url;
        Integer dep;
        Vector<String> links;
        Vector<Integer> depths;
        LinkDepth ld;
        String page, tmp;
        while (!monitor.stop()) {
            while (monitor.pause()) {
                try {
                    sleep(200);
                } catch (InterruptedException e) {
                    e.printStackTrace();
                }
            }
            if (site != null) {
                ld = site.getLinksAndDepths();
                links = ld.getLinks();
                depths = ld.getDepths();
                for (int p = 0; p < links.size(); p++) {
                    url = links.get(p);
                    dep = depths.get(p);
                    if (dep <= monitor.getLinkDepth()) {
                        try {
                            URLConnection urc = new URL(url).openConnection();
                            if (urc.getContentType() != null) {
                                if (urc.getContentType().indexOf("text/html") >= 0) {
                                    page = "";
                                    BufferedReader in = new BufferedReader(new InputStreamReader(urc.getInputStream()));
                                    while ((tmp = in.readLine()) != null) page += tmp;
                                    in.close();
                                    Vector<String> v = getAllLinks(page);
                                    monitor.incFound(v.size());
                                    for (int i = 0; i < v.size(); i++) {
                                        String check = v.get(i);
                                        if (check.indexOf("http://") == 0) {
                                            String str = new URL(check).getHost();
                                            if (monitor.isSiteAcceptable(str)) monitor.tryAddSite(str, site.getDepth() + 1, check, dep + 1);
                                        } else if (hasNoProto(check)) {
                                            String path = new URL(url).getPath();
                                            if (path.length() == 0) path = "/"; else {
                                                if (path.charAt(0) != '/') path = "/" + path;
                                                path = path.substring(0, path.lastIndexOf('/') + 1);
                                            }
                                            if (check.charAt(0) == '/') check = check.substring(1);
                                            if (monitor.isSiteAcceptable(new URL(url).getHost())) site.addLink("http://" + new URL(url).getHost() + path + check, dep + 1);
                                        } else {
                                            monitor.incGarbage();
                                        }
                                    }
                                } else {
                                    String contentType = urc.getContentType().split("\\/")[0];
                                    String path = new URL(url).getPath();
                                    if (monitor.isDesiredDownload(contentType, path)) {
                                        monitor.incDownload();
                                        InputStream is = urc.getInputStream();
                                        byte[] fileData = new byte[2048];
                                        int i;
                                        OutputStream os;
                                        if ((os = site.getNextFile(urc.getURL().getHost(), monitor, urc.getContentType().split("\\/")[1])) != null) {
                                            while ((i = is.read(fileData)) != -1) {
                                                os.write(fileData, 0, i);
                                                os.flush();
                                            }
                                            os.close();
                                            is.close();
                                        }
                                    } else {
                                        monitor.incGarbage();
                                    }
                                }
                            }
                        } catch (Exception e) {
                            monitor.incGarbage();
                        }
                    }
                }
                monitor.reCache(site, links.size());
                site = null;
                System.gc();
            } else {
                site = monitor.getNextSite(this);
            }
        }
        stopped = true;
    }

    private boolean hasNoProto(String check) {
        try {
            new URL(check).getProtocol();
            return false;
        } catch (MalformedURLException e) {
            return true;
        }
    }

    public void setIdle(boolean idle) {
        this.idle = idle;
    }

    public boolean idle() {
        return idle;
    }

    public boolean stopped() {
        return stopped;
    }
}
