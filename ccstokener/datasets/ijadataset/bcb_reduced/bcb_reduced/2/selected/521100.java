package net.dongliu.jalus.service;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.UnsupportedEncodingException;
import java.net.HttpURLConnection;
import java.net.URL;
import java.net.URLEncoder;
import java.text.DateFormat;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import javax.cache.Cache;
import javax.cache.CacheException;
import javax.cache.CacheFactory;
import javax.cache.CacheManager;
import net.dongliu.jalus.pojo.Post;
import net.dongliu.jalus.pojo.ReaderFeed;
import org.apache.commons.lang.StringUtils;
import org.jdom.Document;
import org.jdom.Element;
import org.jdom.JDOMException;
import org.jdom.Namespace;
import org.jdom.input.SAXBuilder;
import com.google.appengine.api.memcache.jsr107cache.GCacheFactory;
import com.sun.syndication.io.FeedException;

/**
 * google reader handler
 * @author dongliu
 *
 */
public class ReaderService {

    private static class Holder {

        private static ReaderService instance = new ReaderService();
    }

    private static final String loginUrl = "https://www.google.com/accounts/ClientLogin";

    private static final String api0Prefix = "http://www.google.com/reader/api/0/";

    private static final String feedListUrl = api0Prefix + "subscription/list";

    private static final String atomPrefix = "http://www.google.com/reader/atom/";

    @SuppressWarnings("unused")
    private static final String subscriptionUrl = atomPrefix + "user/-/pref/com.google/subscriptions";

    @SuppressWarnings("unused")
    private static final String readingListUrl = atomPrefix + "user/-/state/com.google/reading-list";

    private static final String sharedListUrl = atomPrefix + "user/-/state/com.google/broadcast";

    private String sid;

    private Date lastDate;

    private Cache cache;

    private ReaderService() {
        Map<Object, Object> props = new HashMap<Object, Object>();
        props.put(GCacheFactory.EXPIRATION_DELTA, 60 * 60);
        try {
            CacheFactory cacheFactory = CacheManager.getInstance().getCacheFactory();
            cache = cacheFactory.createCache(props);
        } catch (CacheException e) {
            cache = null;
        }
    }

    /**
	 * 获得实例
	 * @return
	 */
    public static ReaderService getInstance() {
        return Holder.instance;
    }

    public void login() throws IOException {
        Date date = new Date();
        if (sid != null && date.getTime() - lastDate.getTime() < 1000 * 60 * 30) {
            return;
        }
        String postStr = "Email=" + URLEncoder.encode(ConfigureService.getInstance().getString("googleuserid"), "UTF-8") + "&Passwd=" + ConfigureService.getInstance().getString("googlepasswd");
        URL url = new URL(loginUrl + "?" + postStr);
        HttpURLConnection connection = (HttpURLConnection) url.openConnection();
        connection.setRequestMethod("GET");
        BufferedReader reader = new BufferedReader(new InputStreamReader(connection.getInputStream()));
        String line;
        while ((line = reader.readLine()) != null) {
            String[] key_value = line.split("=");
            if (key_value[0].equalsIgnoreCase("sid")) {
                sid = key_value[1];
            } else if (key_value[0].equalsIgnoreCase("error")) {
            }
        }
        this.lastDate = date;
    }

    @SuppressWarnings("unchecked")
    public List<net.dongliu.jalus.pojo.Feed> getFeedList() throws IOException, IllegalArgumentException {
        String key = "getFeedList";
        if (cache.containsKey(key)) {
            return (List<net.dongliu.jalus.pojo.Feed>) cache.get(key);
        }
        URL url = new URL(feedListUrl);
        HttpURLConnection connection = (HttpURLConnection) url.openConnection();
        connection.setRequestProperty("Cookie", "SID=" + sid);
        connection.setRequestMethod("GET");
        BufferedReader reader = new BufferedReader(new InputStreamReader(connection.getInputStream(), "UTF-8"));
        SAXBuilder builder = new SAXBuilder(false);
        Document doc;
        try {
            doc = builder.build(reader);
        } catch (JDOMException e1) {
            return null;
        }
        Element root = doc.getRootElement();
        Element listroot = root.getChild("list");
        List<Element> list = listroot.getChildren("object");
        List<net.dongliu.jalus.pojo.Feed> feedList = new ArrayList<net.dongliu.jalus.pojo.Feed>();
        for (Element e : list) {
            List<Element> elist = e.getChildren();
            net.dongliu.jalus.pojo.Feed feed = new net.dongliu.jalus.pojo.Feed();
            for (Element a : elist) {
                if (a.getAttribute("name").getValue().equalsIgnoreCase("id")) {
                    feed.setUri(URLEncoder.encode(a.getValue(), "UTF-8"));
                } else if (a.getAttribute("name").getValue().equalsIgnoreCase("title")) {
                    feed.setTitle(a.getValue());
                } else if (a.getAttribute("name").getValue().equalsIgnoreCase("categories")) {
                } else if (a.getAttribute("name").getValue().equalsIgnoreCase("firstitemmsec")) {
                    Calendar cd = Calendar.getInstance();
                    cd.setTimeInMillis(Long.valueOf(a.getValue()));
                    feed.setPublishDate(cd.getTime());
                }
            }
            feedList.add(feed);
        }
        cache.put(key, feedList);
        return feedList;
    }

    /**
	 * 根据rss地址，获得google reader上的feed内容
	 * @param Uri   rss地址，格式/feed/ + http地址
	 * @return
	 * @throws JDOMException 
	 */
    public ReaderFeed getFeedByUri(String Uri, String currentFlag) throws IllegalArgumentException, IOException, JDOMException {
        String urlStr = atomPrefix + Uri;
        urlStr += "?n=" + ConfigureService.getInstance().getFeedNumPerpage();
        if (currentFlag != null) {
            urlStr += "&c=" + currentFlag;
        }
        return processEntrys(urlStr, currentFlag);
    }

    @SuppressWarnings("unchecked")
    private ReaderFeed processEntrys(String urlStr, String currentFlag) throws UnsupportedEncodingException, IOException, JDOMException {
        String key = "processEntrys@" + urlStr + "_" + currentFlag;
        if (cache.containsKey(key)) {
            return (ReaderFeed) cache.get(key);
        }
        List<Post> postList = new ArrayList<Post>();
        URL url = new URL(urlStr);
        HttpURLConnection connection = (HttpURLConnection) url.openConnection();
        connection.setRequestMethod("GET");
        connection.setRequestProperty("Cookie", "SID=" + sid);
        BufferedReader reader = new BufferedReader(new InputStreamReader(connection.getInputStream(), "UTF-8"));
        SAXBuilder builder = new SAXBuilder(false);
        Document doc = builder.build(reader);
        Element root = doc.getRootElement();
        Namespace grNamespace = root.getNamespace("gr");
        Namespace namespace = root.getNamespace();
        String newflag = root.getChildText("continuation", grNamespace);
        String title = root.getChildText("title", namespace);
        String subTitle = root.getChildText("subtitle", namespace);
        List<Element> entryList = root.getChildren("entry", namespace);
        DateFormat sdf = new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss'Z'");
        for (Element e : entryList) {
            Post post = new Post();
            post.setTitle(e.getChildText("title", namespace));
            try {
                post.setDate(sdf.parse(e.getChildText("published", namespace)));
            } catch (ParseException e1) {
            }
            post.setUrl(e.getChild("link", namespace).getAttributeValue("href"));
            post.setSauthor(e.getChild("author", namespace).getChildText("name", namespace));
            String content = e.getChildText("content", namespace);
            if (StringUtils.isEmpty(content)) {
                content = e.getChildText("description", namespace);
            }
            if (StringUtils.isEmpty(content)) {
                content = e.getChildText("summary", namespace);
            }
            post.setContent(content);
            postList.add(post);
        }
        ReaderFeed readerFeed = new ReaderFeed();
        readerFeed.setTitle(title);
        readerFeed.setSubTitle(subTitle);
        readerFeed.setFlag(newflag);
        readerFeed.setPostList(postList);
        cache.put(key, readerFeed);
        return readerFeed;
    }

    /**
	 * 获得所有共享文章列表
	 * @return
	 * @throws IOException 
	 * @throws FeedException 
	 * @throws IllegalArgumentException 
	 * @throws JDOMException 
	 */
    public ReaderFeed getSharedReadingList(String currentFlag) throws IOException, IllegalArgumentException, JDOMException {
        String urlStr = sharedListUrl;
        urlStr += "?n=" + ConfigureService.getInstance().getFeedNumPerpage();
        if (currentFlag != null && !currentFlag.isEmpty()) {
            urlStr += "&c=" + currentFlag;
        }
        return processEntrys(urlStr, currentFlag);
    }

    public static void main(String[] args) throws UnsupportedEncodingException, IOException, JDOMException {
    }

    public void clearCache() {
        cache.clear();
    }
}
