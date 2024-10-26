package org.las.crawler;

import com.sun.syndication.feed.synd.*;
import com.sun.syndication.io.*;
import java.io.*;
import java.net.*;
import java.util.*;
import org.las.tools.URLCanonicalizer;
import org.las.tools.MIMEFormater.MIMEFormater;

public class FeedParser {

    public Set<URLEntity> parse(PageEntity page) {
        Set<URLEntity> links = new HashSet<URLEntity>();
        SyndFeedInput input = new SyndFeedInput();
        SyndFeed feed = null;
        try {
            feed = input.build(new XmlReader(new ByteArrayInputStream(page.getContent())));
            String baseUrl = feed.getLink();
            for (Object o : feed.getEntries()) {
                SyndEntry entry = (SyndEntry) o;
                URLEntity link = new URLEntity();
                URL url = URLCanonicalizer.getCanonicalURL(baseUrl, entry.getLink());
                link.setUrl(url.toExternalForm());
                link.setParent_url(page.getUrl());
                link.setSuffix(MIMEFormater.JudgeURLFormat(entry.getUri()));
                link.setTitle(entry.getTitle());
                link.setPublishData(entry.getPublishedDate());
                if (entry.getDescription() != null) {
                    link.setDiscription(entry.getDescription().getValue());
                }
                links.add(link);
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
        return links;
    }

    public static void main(String[] args) {
        try {
            String urlStr = "http://blog.csdn.net/yefei679/category/487716.aspx/rss";
            URLConnection feedUrl = new URL(urlStr).openConnection();
            feedUrl.setRequestProperty("User-Agent", "Mozilla/4.0 (compatible; MSIE 5.0; Windows NT; DigExt)");
            SyndFeedInput input = new SyndFeedInput();
            SyndFeed feed = input.build(new XmlReader(feedUrl));
            System.out.println("---------------Begin Output--------------");
            for (Object o : feed.getEntries()) {
                SyndEntry entry = (SyndEntry) o;
                System.out.println("Title: " + entry.getTitle());
                System.out.println("PubDate: " + entry.getPublishedDate());
                System.out.println("Link: " + entry.getUri());
                System.out.println();
            }
            System.out.println("----------------End Output---------------");
        } catch (Exception e) {
            e.printStackTrace();
        }
    }
}
