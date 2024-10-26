package rssgate;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.PrintWriter;
import java.net.HttpURLConnection;
import java.net.URL;
import java.util.Date;
import java.util.Hashtable;
import java.util.Iterator;
import java.util.List;
import java.util.logging.Logger;
import javax.jdo.PersistenceManager;
import javax.jdo.Query;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import net.htmlparser.jericho.Attribute;
import net.htmlparser.jericho.Attributes;
import net.htmlparser.jericho.Element;
import net.htmlparser.jericho.Source;
import rssgate.dao.PMF;
import rssgate.dao.entity.Feed;
import rssgate.dao.entity.RSSImage;
import rssgate.dao.entity.RSSItem;
import rssgate.util.LogUtils;
import rssgate.util.RSSUpdateDispatcherCache;
import uk.org.catnip.eddie.Author;
import uk.org.catnip.eddie.Detail;
import uk.org.catnip.eddie.Entry;
import uk.org.catnip.eddie.FeedData;
import uk.org.catnip.eddie.parser.Parser;
import com.google.appengine.api.datastore.Blob;
import com.google.appengine.api.datastore.Text;

@SuppressWarnings("serial")
public class UpdateServiceServlet extends HttpServlet implements ManageCommandConstants {

    private static final Logger log = Logger.getLogger(UpdateServiceServlet.class.getName());

    private PrintWriter writer;

    private PersistenceManager persistenceManager;

    public void service(HttpServletRequest req, HttpServletResponse resp) throws IOException {
        final PersistenceManager pm = getPersistenceManager();
        writer = resp.getWriter();
        final String command = req.getParameter(COMMAND);
        LogUtils.logMsg(log, "INFO: request param: " + command + " ", writer);
        Feed nextFeed2Update = null;
        if (UPDATE_COMMAND.equalsIgnoreCase(command)) {
            final String strId = req.getParameter(VALUE_ID);
            LogUtils.logMsg(log, "INFO: request param: " + strId + " ", writer);
            Long id = null;
            try {
                id = Long.decode(strId);
            } catch (Exception e) {
                LogUtils.logError(log, "ERROR: Given id is not Long: " + e.getMessage(), writer);
            }
            LogUtils.logMsg(log, "INFO: feed ID is " + id, writer);
            nextFeed2Update = findRss(id);
        }
        if (nextFeed2Update == null) {
            nextFeed2Update = getNextFeed2Update();
        }
        if (nextFeed2Update == null) {
            LogUtils.logMsg(log, "INFO: No feeds to update", writer);
            resp.sendRedirect("/rsslist.jsp");
            return;
        }
        LogUtils.logMsg(log, "INFO: Update " + nextFeed2Update.getUrl() + " feed ", writer);
        final Parser parser = new Parser();
        final String spec = nextFeed2Update.getUrl();
        final URL url = new URL(spec);
        final HttpURLConnection connection = (HttpURLConnection) url.openConnection();
        connection.connect();
        final InputStream inputStream;
        try {
            inputStream = connection.getInputStream();
        } catch (Exception e) {
            RSSUpdateDispatcherCache.cache.add(nextFeed2Update.getId());
            LogUtils.logError(log, "ERROR: " + e.getMessage(), writer);
            resp.sendRedirect("/rsslist.jsp");
            return;
        }
        if (inputStream == null) {
            LogUtils.logError(log, "ERROR: Input stream is null", writer);
        }
        FeedData parse;
        try {
            parse = parser.parse(inputStream);
            LogUtils.logError(log, "INFO: Parsing complited ", writer);
        } catch (Exception e) {
            RSSUpdateDispatcherCache.cache.add(nextFeed2Update.getId());
            LogUtils.logError(log, "ERROR: " + e.getMessage(), writer);
            resp.sendRedirect("/rsslist.jsp");
            return;
        }
        final Iterator<uk.org.catnip.eddie.Entry> entries = parse.entries();
        if (!entries.hasNext()) {
            LogUtils.logError(log, "INFO: Entries unavailable ", writer);
            BufferedReader reader = new BufferedReader(new InputStreamReader(url.openStream()));
            String line;
            while ((line = reader.readLine()) != null) {
            }
            reader.close();
            int available = inputStream.available();
            LogUtils.logError(log, "INFO: Not Entries: content: " + url + "; available: \n\r" + available + "\n\r, " + line, writer);
            resp.sendRedirect("/rsslist.jsp");
        }
        try {
            while (entries.hasNext()) {
                final Entry next = entries.next();
                final Hashtable hashtable = next.getHashtable();
                final Detail summary = next.getSummary();
                String value = "";
                int hashCode = -1;
                if (summary != null) {
                    value = summary.getValue();
                    hashCode = value.hashCode();
                } else {
                    hashCode = hashtable.hashCode();
                }
                final String query = "select from " + RSSItem.class.getName() + " where hashCode==" + hashCode + " && feedId==" + nextFeed2Update.getId();
                Query newQuery = pm.newQuery(query);
                LogUtils.logError(log, "INFO: Select query: " + query, writer);
                final List<RSSItem> items = (List<RSSItem>) newQuery.execute();
                LogUtils.logError(log, "INFO: Found items list size: " + items.size(), writer);
                if (items.isEmpty()) {
                    LogUtils.logError(log, "INFO: add row: ", writer);
                    final RSSItem item = new RSSItem();
                    final Author a = next.getAuthor();
                    if (a != null) {
                        item.setAuthorEmail(a.getEmail());
                        item.setAuthorHref(a.getHref());
                        item.setAuthorName(a.getName());
                    }
                    final Detail copyright = next.getCopyright();
                    if (copyright != null) {
                        item.setCopyright(copyright.getValue());
                    }
                    item.setCreated(next.getCreated());
                    item.setExpired(next.getExpired());
                    item.setFeedId(nextFeed2Update.getId());
                    item.setHashCode(hashCode);
                    item.setIssued(next.getIssued());
                    item.setModified(next.getModified());
                    final Iterator links = next.links();
                    if (links != null && links.hasNext()) {
                        final uk.org.catnip.eddie.Link link = (uk.org.catnip.eddie.Link) links.next();
                        item.setLink(link.getValue());
                    }
                    final Author p = next.getPublisher();
                    if (p != null) {
                        item.setPublisherEmail(p.getEmail());
                        item.setPublisherHref(p.getHref());
                        item.setPublisherName(p.getName());
                    }
                    final String replaceImagURLs = replaceImagURLs(value);
                    final Text sum = new Text(replaceImagURLs);
                    item.setSummury(sum);
                    final Detail title = next.getTitle();
                    if (title != null) {
                        final Text title2 = new Text(title.getValue());
                        item.setTitle(title2);
                    }
                    pm.makePersistent(item);
                } else {
                    LogUtils.logError(log, "INFO: Found items list size: " + items.get(0).getSummury(), writer);
                    continue;
                }
            }
        } finally {
            nextFeed2Update.setLastUpdate(new Date());
            final Detail feedDetail = parse.getTitle();
            final Detail subtitle = parse.getSubtitle();
            if (feedDetail != null) {
                final String value = feedDetail.getValue();
                final Text title = new Text(value);
                nextFeed2Update.setTitle(title);
            }
            if (subtitle != null) {
                final String value = subtitle.getValue();
                final Text subtitle2 = new Text(value);
                nextFeed2Update.setSubtitle(subtitle2);
            }
            pm.makePersistent(nextFeed2Update);
            LogUtils.logError(log, "INFO: new feed info: " + nextFeed2Update.getSubtitle() + "; " + nextFeed2Update.getTitle() + "; " + nextFeed2Update.getLastUpdate(), writer);
            pm.close();
        }
        resp.sendRedirect("/rsslist.jsp");
    }

    private String replaceImagURLs(String content) {
        if (content != null) {
            final Source source = new Source(content);
            final List<Element> elementList = source.getAllElements();
            for (Element element : elementList) {
                String name = element.getName();
                if (!"img".equalsIgnoreCase(name)) {
                    continue;
                }
                final Attributes attributes = element.getAttributes();
                final Iterator<Attribute> iterator = attributes.iterator();
                while (iterator.hasNext()) {
                    final Attribute next = iterator.next();
                    final String attributeName = next.getName();
                    if (!"src".equalsIgnoreCase(attributeName)) {
                        continue;
                    } else {
                        String value = next.getValue();
                        final RSSImage imgEntity = saveImageData(value);
                        if (imgEntity != null) {
                            final long longValue = imgEntity.getId().longValue();
                            content = content.replaceFirst(value, "http://rssnewsglobal.appspot.com/img/" + longValue + imgEntity.getExtention());
                        }
                    }
                }
            }
        }
        return content;
    }

    private RSSImage saveImageData(String oldImageUrl) {
        final byte[] imagedata = getImagedata(oldImageUrl);
        if (imagedata == null) {
            return null;
        }
        final PersistenceManager persistenceManager = getPersistenceManager();
        final RSSImage imageEntity = new RSSImage();
        final Blob blob = new Blob(imagedata);
        imageEntity.setImageData(blob);
        imageEntity.setModified(new Date());
        final int lastIndexOf = oldImageUrl.lastIndexOf(".");
        if (lastIndexOf >= 0) {
            String substring = oldImageUrl.substring(lastIndexOf, oldImageUrl.length());
            imageEntity.setExtention(substring);
        } else {
            imageEntity.setExtention("");
        }
        persistenceManager.makePersistent(imageEntity);
        return imageEntity;
    }

    private byte[] getImagedata(String oldImageUrl) {
        byte[] oldImageData = null;
        try {
            final URL url = new URL(oldImageUrl);
            final HttpURLConnection connection = (HttpURLConnection) url.openConnection();
            final InputStream inputStream = connection.getInputStream();
            int available = inputStream.available();
            oldImageData = new byte[available];
            int read = inputStream.read();
            int i = 0;
            while (read != -1 && i < available) {
                oldImageData[i] = (byte) read;
                read = inputStream.read();
                i++;
            }
            inputStream.close();
        } catch (Exception e) {
            LogUtils.logError(log, "ERROR: " + e.getMessage(), writer);
        }
        return oldImageData;
    }

    private PersistenceManager getPersistenceManager() {
        if (persistenceManager == null || persistenceManager.isClosed()) {
            persistenceManager = PMF.get().getPersistenceManager();
        }
        return persistenceManager;
    }

    private Feed getNextFeed2Update() {
        PersistenceManager pm = getPersistenceManager();
        final Date last2Past = ConstUtils.getLast2Past();
        final Query query = pm.newQuery("select from " + Feed.class.getName() + " where lastUpdate < updateDate");
        query.declareParameters("java.util.Date updateDate");
        final List<Feed> items = (List<Feed>) pm.newQuery(query).execute(last2Past);
        if (items.isEmpty()) {
            return null;
        } else {
            final Iterator<Feed> iterator = items.iterator();
            Feed feed = null;
            while (iterator.hasNext()) {
                final Feed next = iterator.next();
                if (RSSUpdateDispatcherCache.cache.contains(next.getId())) {
                    continue;
                }
                feed = next;
                break;
            }
            if (feed == null) {
                feed = items.get(0);
                RSSUpdateDispatcherCache.cache.clear();
            }
            if (RSSUpdateDispatcherCache.cache.size() >= 3) {
                RSSUpdateDispatcherCache.cache.clear();
            }
            return feed;
        }
    }

    private Feed findRss(Long id) {
        final PersistenceManager persistenceManager = getPersistenceManager();
        String query = "select from " + Feed.class.getName() + " where id==" + id;
        final List<Feed> list = (List<Feed>) persistenceManager.newQuery(query).execute();
        if (!list.isEmpty()) {
            final Feed feed = list.get(0);
            return feed;
        }
        return null;
    }
}
