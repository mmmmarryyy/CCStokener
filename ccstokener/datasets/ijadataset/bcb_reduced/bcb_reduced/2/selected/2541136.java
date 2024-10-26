package com.appengine.news.service;

import java.io.IOException;
import java.io.InputStream;
import java.net.HttpURLConnection;
import java.net.URL;
import java.util.Date;
import java.util.HashSet;
import java.util.Hashtable;
import java.util.Iterator;
import java.util.List;
import java.util.Set;
import java.util.logging.Level;
import java.util.logging.Logger;
import javax.jdo.PersistenceManager;
import javax.jdo.Query;
import javax.servlet.ServletException;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import net.htmlparser.jericho.Attribute;
import net.htmlparser.jericho.Attributes;
import net.htmlparser.jericho.Element;
import net.htmlparser.jericho.Source;
import uk.org.catnip.eddie.Author;
import uk.org.catnip.eddie.Detail;
import uk.org.catnip.eddie.Entry;
import uk.org.catnip.eddie.FeedData;
import uk.org.catnip.eddie.parser.Parser;
import com.appengine.news.entity.RssFeedEntity;
import com.appengine.news.entity.RssImageEntity;
import com.appengine.news.entity.RssItemEntity;
import com.appengine.news.utils.StringUtils;
import com.google.appengine.api.datastore.Blob;
import com.google.appengine.api.datastore.Text;
import com.google.appengine.api.labs.taskqueue.Queue;
import com.google.appengine.api.labs.taskqueue.QueueFactory;
import com.google.appengine.api.labs.taskqueue.TaskOptions;
import com.google.appengine.api.labs.taskqueue.TaskOptions.Builder;

/**
 * Updater task
 * 
 * @author Aliaksandr_Spichakou
 * 
 */
public class UpdateTask extends AbstractService {

    private static final Logger log = Logger.getLogger(UpdateTask.class.getName());

    /**
	 * Serial version
	 */
    private static final long serialVersionUID = 6756062815262556982L;

    @Override
    protected void service(HttpServletRequest arg0, HttpServletResponse arg1) throws ServletException, IOException {
        log.entering(UpdateTask.class.getName(), "service");
        final RssFeedEntity feed2Update = getFeed2Update(arg0);
        if (feed2Update == null) {
            log.exiting(UpdateTask.class.getName(), "service");
            return;
        }
        InputStream inputStream = null;
        InputStream inputStreamTst = null;
        try {
            log.log(Level.WARNING, "Update feed " + feed2Update.getUrl());
            inputStream = getInputStream(feed2Update);
            inputStreamTst = getInputStream(feed2Update);
        } catch (Exception e) {
            log.log(Level.SEVERE, "Get input stream fail: " + feed2Update.getUrl());
            log.exiting(UpdateTask.class.getName(), "service");
            return;
        }
        byte[] bs = new byte[inputStreamTst.available()];
        inputStreamTst.read(bs);
        String streamtst = new String(bs);
        log.log(Level.WARNING, "Feed content: " + streamtst);
        final FeedData feedData = parseInputStream(inputStream);
        if (feedData == null) {
            log.exiting(UpdateTask.class.getName(), "service");
            return;
        }
        final Iterator<Entry> entries = feedData.entries();
        final PersistenceManager pm = getPersistenceManager();
        final Set<Long> createdIds = new HashSet<Long>();
        try {
            while (entries.hasNext()) {
                final Entry next = entries.next();
                final int hashCode = getHashCode(next);
                final Long id = feed2Update.getId();
                boolean checkExistedItem = checkExistedItem(id, hashCode);
                if (checkExistedItem) {
                    log.log(Level.INFO, "Add row");
                    final RssItemEntity item = createRssItem(next, id, hashCode);
                    pm.makePersistent(item);
                    createdIds.add(item.getId());
                } else {
                    log.log(Level.INFO, "Found items; entry skipped ");
                    continue;
                }
            }
        } catch (Exception e) {
            log.log(Level.SEVERE, "Something is wrong", e);
        } finally {
            createEmailTasks(createdIds);
            updateFeedInfo(feed2Update, feedData);
            pm.close();
        }
        log.exiting(UpdateTask.class.getName(), "service");
    }

    /**
	 * Create email tasks
	 * @param createdIds
	 */
    private void createEmailTasks(Set<Long> createdIds) {
        log.entering(UpdateTask.class.getName(), "createEmailTasks");
        String createdIdsStr = "";
        final Iterator<Long> iterator = createdIds.iterator();
        int i = 0;
        while (iterator.hasNext()) {
            final Long next = iterator.next();
            createdIdsStr += next;
            if (iterator.hasNext()) {
                createdIdsStr += ",";
            }
            if (i > 1) {
                sendTask(createdIdsStr);
                createdIdsStr = "";
                i = 0;
            }
            i++;
        }
        log.exiting(UpdateTask.class.getName(), "createEmailTasks");
    }

    /**
	 * Send tasks
	 * @param createdIdsStr
	 */
    private void sendTask(String createdIdsStr) {
        final Queue queue = QueueFactory.getQueue(DEFAULT_QUEUE);
        TaskOptions options = Builder.url(MAIL_TASK);
        options = options.param(VALUE_IDS, createdIdsStr);
        options = options.param(VALUE_MAILTO, cfg.getStoreEmail());
        queue.add(options);
    }

    /**
	 * Update fed info
	 * 
	 * @param feed2Update
	 * @param feedData
	 */
    private void updateFeedInfo(RssFeedEntity feed2Update, FeedData feedData) {
        log.entering(UpdateTask.class.getName(), "updateFeedInfo");
        feed2Update.setLastUpdate(new Date());
        final Detail feedDetail = feedData.getTitle();
        final Detail subtitle = feedData.getSubtitle();
        if (feedDetail != null) {
            final String value = feedDetail.getValue();
            final Text title = new Text(value);
            feed2Update.setTitle(title);
        }
        if (subtitle != null) {
            final String value = subtitle.getValue();
            final Text subtitle2 = new Text(value);
            feed2Update.setSubtitle(subtitle2);
        }
        final PersistenceManager pm = getPersistenceManager();
        pm.makePersistent(feed2Update);
        log.log(Level.INFO, "New feed info: " + feed2Update.getSubtitle() + "; " + feed2Update.getTitle() + "; " + feed2Update.getLastUpdate());
        log.exiting(UpdateTask.class.getName(), "updateFeedInfo");
    }

    /**
	 * Get entry hashcode if summary
	 * 
	 * @param entry
	 * @return
	 */
    private int getHashCode(Entry entry) {
        log.entering(UpdateTask.class.getName(), "getHashCode");
        final Hashtable hashtable = entry.getHashtable();
        final Detail summary = entry.getSummary();
        String value = "";
        int hashCode = -1;
        if (summary != null) {
            value = summary.getValue();
            hashCode = value.hashCode();
        } else {
            hashCode = hashtable.hashCode();
        }
        log.exiting(UpdateTask.class.getName(), "getHashCode");
        return hashCode;
    }

    private boolean checkExistedItem(Long feedId, int hashCode) {
        log.entering(UpdateTask.class.getName(), "checkExistedItem");
        final String query = "select from " + RssItemEntity.class.getName() + " where hashCode==" + hashCode + " && feedId==" + feedId;
        final PersistenceManager pm = getPersistenceManager();
        Query newQuery = pm.newQuery(query);
        log.log(Level.INFO, "Select query: " + query);
        final List<RssFeedEntity> items = (List<RssFeedEntity>) newQuery.execute();
        log.log(Level.INFO, "Found items list size: " + items.size());
        log.exiting(UpdateTask.class.getName(), "checkExistedItem");
        return items.isEmpty();
    }

    /**
	 * Create Rss Feed Item
	 * 
	 * @param entry
	 * @param feedId
	 * @param hashCode
	 * @return
	 * @throws Exception
	 */
    private RssItemEntity createRssItem(Entry entry, Long feedId, int hashCode) throws Exception {
        log.entering(UpdateTask.class.getName(), "createRssItem");
        final RssItemEntity item = new RssItemEntity();
        final Author a = entry.getAuthor();
        if (a != null) {
            item.setAuthorEmail(a.getEmail());
            item.setAuthorHref(a.getHref());
            item.setAuthorName(a.getName());
        }
        final Detail copyright = entry.getCopyright();
        if (copyright != null) {
            item.setCopyright(copyright.getValue());
        }
        item.setCreated(entry.getCreated());
        item.setExpired(entry.getExpired());
        item.setFeedId(feedId);
        item.setHashCode(hashCode);
        item.setIssued(entry.getIssued());
        item.setModified(entry.getModified());
        final Iterator links = entry.links();
        if (links != null && links.hasNext()) {
            final uk.org.catnip.eddie.Link link = (uk.org.catnip.eddie.Link) links.next();
            item.setLink(link.getValue());
        }
        final Author p = entry.getPublisher();
        if (p != null) {
            item.setPublisherEmail(p.getEmail());
            item.setPublisherHref(p.getHref());
            item.setPublisherName(p.getName());
        }
        final Detail summary = entry.getSummary();
        if (summary != null) {
            final String value = summary.getValue();
            Text sum = new Text(value);
            String replaceImagURLs = value;
            try {
                replaceImagURLs = replaceImagURLs(value);
                sum = new Text(replaceImagURLs);
            } catch (Exception e) {
                log.log(Level.WARNING, "Unable to ger image data, used old url", e);
            }
            item.setSummury(sum);
        }
        final Detail title = entry.getTitle();
        if (title != null) {
            final Text title2 = new Text(title.getValue());
            item.setTitle(title2);
        }
        log.exiting(UpdateTask.class.getName(), "createRssItem");
        return item;
    }

    /**
	 * Replace image src urls
	 * 
	 * @param content
	 * @return
	 * @throws Exception
	 */
    private String replaceImagURLs(String content) throws Exception {
        log.entering(UpdateTask.class.getName(), "replaceImagURLs");
        if (content == null) {
            log.exiting(UpdateTask.class.getName(), "replaceImagURLs");
            return "";
        }
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
                    final RssImageEntity imgEntity = saveImageData(value);
                    if (imgEntity != null) {
                        final long longValue = imgEntity.getId().longValue();
                        content = content.replaceFirst(value, cfg.getAppUrl() + IMAGE_TASK + "/" + longValue + imgEntity.getExtention());
                    }
                }
            }
        }
        log.exiting(UpdateTask.class.getName(), "replaceImagURLs");
        return content;
    }

    /**
	 * Save or find saved image
	 * @param oldImageUrl
	 * @return
	 * @throws Exception
	 */
    private RssImageEntity saveImageData(String oldImageUrl) throws Exception {
        log.entering(UpdateTask.class.getName(), "saveImageData");
        final RssImageEntity findImageEntity = findImageEntity(oldImageUrl);
        if (findImageEntity != null) {
            return findImageEntity;
        }
        final byte[] imagedata = getImageData(oldImageUrl);
        if (imagedata == null) {
            return null;
        }
        final PersistenceManager persistenceManager = getPersistenceManager();
        final RssImageEntity imageEntity = new RssImageEntity();
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
        log.exiting(UpdateTask.class.getName(), "saveImageData");
        return imageEntity;
    }

    /**
	 * Try to find saved image 
	 * @param oldURL
	 * @return
	 */
    private RssImageEntity findImageEntity(String oldURL) {
        log.exiting(UpdateTask.class.getName(), "findImageEntity");
        final PersistenceManager persistenceManager = getPersistenceManager();
        final String query = "select from " + RssImageEntity.class.getName() + " where realURL=='" + oldURL + "'";
        final List<RssImageEntity> execute = (List<RssImageEntity>) persistenceManager.newQuery(query).execute();
        if (execute != null && execute.size() > 0) {
            final RssImageEntity rssImageEntity = execute.get(0);
            return rssImageEntity;
        }
        log.exiting(UpdateTask.class.getName(), "findImageEntity");
        return null;
    }

    /**
	 * Get image data
	 * 
	 * @param oldImageUrl
	 * @return
	 * @throws Exception
	 */
    private byte[] getImageData(String oldImageUrl) throws Exception {
        log.entering(UpdateTask.class.getName(), "getImagedata");
        byte[] oldImageData = null;
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
        log.exiting(UpdateTask.class.getName(), "getImagedata");
        return oldImageData;
    }

    /**
	 * Get input stream
	 * 
	 * @param feed2Update
	 * @return
	 * @throws Exception
	 */
    private InputStream getInputStream(RssFeedEntity feed2Update) throws Exception {
        log.entering(UpdateTask.class.getName(), "parseInputStream");
        final String spec = feed2Update.getUrl();
        final URL url = new URL(spec);
        final HttpURLConnection connection = (HttpURLConnection) url.openConnection();
        connection.connect();
        final InputStream inputStream = connection.getInputStream();
        log.exiting(UpdateTask.class.getName(), "getInputStream");
        return inputStream;
    }

    /**
	 * Parse input feed stream
	 */
    private FeedData parseInputStream(InputStream inputStream) {
        log.entering(UpdateTask.class.getName(), "parseInputStream");
        final Parser parser = new Parser();
        FeedData parse;
        try {
            final int available = inputStream.available();
            log.log(Level.WARNING, "Input stream bytes: " + available);
        } catch (IOException e1) {
            log.log(Level.SEVERE, "Input stream bytes unavaliable");
            log.exiting(UpdateTask.class.getName(), "parseInputStream");
            return null;
        }
        try {
            parse = parser.parse(inputStream);
            log.log(Level.INFO, "Parsing complited ");
        } catch (Exception e) {
            log.log(Level.SEVERE, "Parsing failed");
            log.exiting(UpdateTask.class.getName(), "parseInputStream");
            return null;
        }
        final Iterator<uk.org.catnip.eddie.Entry> entries = parse.entries();
        if (!entries.hasNext()) {
            log.log(Level.INFO, "Entries unavailable");
        }
        log.exiting(UpdateTask.class.getName(), "parseInputStream");
        return parse;
    }

    /**
	 * Get feed for update by param
	 * 
	 * @param request
	 * @return
	 */
    private RssFeedEntity getFeed2Update(HttpServletRequest request) {
        log.entering(UpdateTask.class.getName(), "getFeed2Update");
        final String feedIdStr = request.getParameter(FEED_ID);
        final Long id = StringUtils.parseString2Long(feedIdStr);
        if (id == null) {
            log.log(Level.SEVERE, "Given feed ID is empty or wrong: " + feedIdStr);
            log.exiting(UpdateTask.class.getName(), "getFeed2Update");
            return null;
        }
        final PersistenceManager persistenceManager = getPersistenceManager();
        String query = "select from " + RssFeedEntity.class.getName() + " where id==" + id;
        final List<RssFeedEntity> list = (List<RssFeedEntity>) persistenceManager.newQuery(query).execute();
        if (!list.isEmpty()) {
            final RssFeedEntity feed = list.get(0);
            log.exiting(UpdateTask.class.getName(), "getFeed2Update");
            return feed;
        }
        log.exiting(UpdateTask.class.getName(), "getFeed2Update");
        return null;
    }
}
