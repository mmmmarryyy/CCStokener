package com.geoffholden.xfngraph.spider;

import java.io.IOException;
import java.net.HttpURLConnection;
import java.net.URL;
import java.net.URLConnection;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Hashtable;
import java.util.Iterator;
import java.util.List;
import java.util.ListIterator;
import java.util.Map;
import java.util.Set;
import org.htmlparser.Node;
import org.htmlparser.NodeFilter;
import org.htmlparser.Parser;
import org.htmlparser.filters.NodeClassFilter;
import org.htmlparser.filters.OrFilter;
import org.htmlparser.http.ConnectionManager;
import org.htmlparser.tags.FrameTag;
import org.htmlparser.tags.LinkTag;
import org.htmlparser.tags.TitleTag;
import org.htmlparser.util.NodeList;
import org.htmlparser.util.ParserException;

public class Spider {

    private static final NodeFilter NODE_FILTER = new OrFilter(new OrFilter(new NodeClassFilter(LinkTag.class), new NodeClassFilter(TitleTag.class)), new NodeClassFilter(FrameTag.class));

    private Set sites = new HashSet();

    private Set spideredSites = new HashSet();

    private Set links = new HashSet();

    private Object sitesLock = new Object();

    private Object linksLock = new Object();

    protected List processQueue = new ArrayList();

    protected SpiderProcessThread[] threads;

    public static final Map xfnTypes;

    public static final int FRIENDSHIP = 0;

    public static final int PHYSICAL = 1;

    public static final int PROFESSIONAL = 2;

    public static final int GEOGRAPHICAL = 3;

    public static final int FAMILY = 4;

    public static final int ROMANTIC = 5;

    public static final int IDENTITY = 6;

    public Spider(int numThreads) {
        if (numThreads <= 0) {
            throw new IllegalArgumentException("numThreads must be a positive integer.");
        }
        Hashtable parserProperties = ConnectionManager.getDefaultRequestProperties();
        parserProperties.put("User-Agent", "XFNGraph/1.3");
        ConnectionManager.setDefaultRequestProperties(parserProperties);
        threads = new SpiderProcessThread[numThreads];
    }

    public void spider(Site site, int depth, SpiderProgressListener progress) {
        site.depthFound = depth;
        processQueue.add(site);
        for (int i = 0; i < threads.length; i++) {
            threads[i] = new SpiderProcessThread(this, progress);
            if (i == 0) {
                threads[0].running = true;
            }
            threads[i].start();
        }
        boolean running = true;
        while (running) {
            synchronized (this) {
                try {
                    this.wait(100);
                } catch (InterruptedException e) {
                }
            }
            synchronized (processQueue) {
                running = !processQueue.isEmpty();
                for (int i = 0; i < threads.length; i++) {
                    synchronized (threads[i]) {
                        running = running || threads[i].running;
                    }
                }
            }
        }
        return;
    }

    public void spider(Site site, SpiderProgressListener progress) {
        synchronized (sitesLock) {
            if (!sites.contains(site)) sites.add(site);
        }
        for (ListIterator it = site.urls.listIterator(); it.hasNext(); ) {
            String url = (String) it.next();
            synchronized (progress) {
                progress.setProgressText("Spidering " + url + "...");
            }
            try {
                HttpURLConnection.setFollowRedirects(false);
                URLConnection connection = new URL(url).openConnection();
                connection.setAllowUserInteraction(false);
                if (connection instanceof HttpURLConnection) {
                    HttpURLConnection httpConnection = (HttpURLConnection) connection;
                    int responseCode = httpConnection.getResponseCode();
                    if (responseCode >= 300 && responseCode < 400) {
                        String newLocation = httpConnection.getHeaderField("Location");
                        if (null != newLocation && !"".equals(newLocation)) {
                            processIdentityLink(site, it, newLocation);
                        }
                        continue;
                    } else if (responseCode != 200) {
                        continue;
                    }
                }
                Parser parser = new Parser(connection);
                NodeList list = parser.extractAllNodesThatMatch(NODE_FILTER);
                for (int i = 0; i < list.size(); i++) {
                    Node tag = list.elementAt(i);
                    if (tag instanceof TitleTag) {
                        processTitleTag(site, (TitleTag) tag);
                    } else if (tag instanceof FrameTag) {
                        processIdentityLink(site, it, ((FrameTag) tag).getFrameLocation().trim());
                    } else {
                        processLink(site, it, (LinkTag) tag);
                    }
                }
            } catch (ParserException e) {
            } catch (IOException e) {
            } catch (RuntimeException e) {
            }
        }
    }

    private void processTitleTag(Site site, TitleTag tag) {
        String pageTitle = tag.getTitle();
        if (pageTitle != null && !pageTitle.equals("") && site.names.size() == 0) {
            site.names.add(pageTitle.trim());
        }
    }

    private void processLink(Site site, ListIterator iterator, LinkTag tag) {
        String rel = tag.getAttribute("rel");
        if (rel != null) {
            String[] attrs = rel.split(" ");
            for (int j = 0; j < attrs.length; j++) {
                attrs[j] = attrs[j].replaceAll(",$", "");
            }
            if (attrs.length == 1 && new Integer(IDENTITY).equals(xfnTypes.get(attrs[0]))) {
                processIdentityLink(site, iterator, tag.getLink().trim());
            } else {
                Link link = new Link();
                for (int j = 0; j < attrs.length; j++) {
                    if (xfnTypes.keySet().contains(attrs[j]) && !new Integer(IDENTITY).equals(xfnTypes.get(attrs[j]))) {
                        link.type.add(attrs[j]);
                    }
                }
                if (link.type.size() > 0) {
                    link.src = site;
                    link.dest = new Site();
                    link.dest.urls.add(tag.getLink().trim());
                    link.dest.depthFound = site.depthFound - 1;
                    synchronized (sitesLock) {
                        for (Iterator it2 = sites.iterator(); it2.hasNext(); ) {
                            Site scanSite = (Site) it2.next();
                            if (link.dest.equals(scanSite)) {
                                link.dest = scanSite;
                                break;
                            }
                        }
                    }
                    link.dest.names.add(tag.getLinkText().trim());
                    String linkTitle = tag.getAttribute("title");
                    if (linkTitle != null && !linkTitle.equals("")) {
                        link.dest.names.add(linkTitle.trim());
                    }
                    if (addLink(link)) {
                        addSite(link.dest, site.depthFound);
                    }
                }
            }
        }
    }

    private boolean addLink(Link link) {
        boolean result = false;
        synchronized (linksLock) {
            if (links.contains(link.reverseLink())) {
                for (Iterator iter = links.iterator(); iter.hasNext(); ) {
                    Link oldLink = (Link) iter.next();
                    if (oldLink.equals(link.reverseLink())) {
                        oldLink.getTypes().addAll(link.getTypes());
                        oldLink.setReciprocal(true);
                        break;
                    }
                }
            } else {
                links.add(link);
                result = true;
            }
        }
        return result;
    }

    private synchronized void addSite(Site site, int depth) {
        if (!spideredSites.contains(site)) {
            synchronized (sitesLock) {
                if (!sites.contains(site)) {
                    sites.add(site);
                }
            }
            if (depth > 1) {
                spideredSites.add(site);
                synchronized (processQueue) {
                    processQueue.add(site);
                }
            }
        }
    }

    private void processIdentityLink(Site currentSite, ListIterator iterator, String location) {
        Site newSite = new Site();
        newSite.urls.add(location);
        if (!newSite.equals(currentSite)) {
            synchronized (sitesLock) {
                for (Iterator it2 = sites.iterator(); it2.hasNext(); ) {
                    Site foundSite = (Site) it2.next();
                    if (newSite.equals(foundSite)) {
                        synchronized (linksLock) {
                            for (Iterator it3 = links.iterator(); it3.hasNext(); ) {
                                Link link = (Link) it3.next();
                                if (link.src.equals(newSite)) link.src = currentSite;
                                if (link.dest.equals(newSite)) link.dest = currentSite;
                            }
                        }
                        it2.remove();
                    }
                }
            }
            synchronized (currentSite.urls) {
                iterator.add(location);
            }
            iterator.previous();
            synchronized (linksLock) {
                this.links = new HashSet(this.links);
            }
            synchronized (sitesLock) {
                this.sites = new HashSet(this.sites);
            }
        }
    }

    static {
        xfnTypes = new HashMap();
        xfnTypes.put("contact", new Integer(FRIENDSHIP));
        xfnTypes.put("acquaintance", new Integer(FRIENDSHIP));
        xfnTypes.put("friend", new Integer(FRIENDSHIP));
        xfnTypes.put("met", new Integer(PHYSICAL));
        xfnTypes.put("coworker", new Integer(PROFESSIONAL));
        xfnTypes.put("co-worker", new Integer(PROFESSIONAL));
        xfnTypes.put("colleague", new Integer(PROFESSIONAL));
        xfnTypes.put("coresident", new Integer(GEOGRAPHICAL));
        xfnTypes.put("co-resident", new Integer(GEOGRAPHICAL));
        xfnTypes.put("neighbour", new Integer(GEOGRAPHICAL));
        xfnTypes.put("neighbor", new Integer(GEOGRAPHICAL));
        xfnTypes.put("child", new Integer(FAMILY));
        xfnTypes.put("parent", new Integer(FAMILY));
        xfnTypes.put("sibling", new Integer(FAMILY));
        xfnTypes.put("spouse", new Integer(FAMILY));
        xfnTypes.put("kin", new Integer(FAMILY));
        xfnTypes.put("muse", new Integer(ROMANTIC));
        xfnTypes.put("crush", new Integer(ROMANTIC));
        xfnTypes.put("date", new Integer(ROMANTIC));
        xfnTypes.put("dating", new Integer(ROMANTIC));
        xfnTypes.put("sweetheart", new Integer(ROMANTIC));
        xfnTypes.put("me", new Integer(IDENTITY));
    }

    public Map getXFNTypes() {
        return xfnTypes;
    }

    public Collection getSites() {
        synchronized (sitesLock) {
            Collection result = new ArrayList();
            result.addAll(sites);
            return result;
        }
    }

    public Collection getLinks() {
        synchronized (linksLock) {
            Collection result = new ArrayList();
            result.addAll(links);
            return result;
        }
    }
}
