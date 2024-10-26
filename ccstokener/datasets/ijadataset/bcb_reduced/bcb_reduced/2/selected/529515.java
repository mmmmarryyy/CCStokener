package com.rajeshpg.techfeeds.utils;

import java.io.InputStream;
import java.net.InetSocketAddress;
import java.net.Proxy;
import java.net.URL;
import java.util.List;
import sun.net.www.protocol.http.HttpURLConnection;
import com.rajeshpg.techfeeds.RSSParser.RSSDomParser;
import com.rajeshpg.techfeeds.logger.TechFeedsLogger;
import com.rajeshpg.techfeeds.vo.FeedsVO;
import com.rajeshpg.techfeeds.vo.RssFeeds;

public class RssUtils {

    public static RssFeeds getRssFeeds(String urlString) {
        TechFeedsLogger.debug("RssUtils::getRssFeeds()::START");
        String temp = null;
        RssFeeds rssFeeds = null;
        try {
            System.setProperty("http.proxyHost", "proxy.in.ml.com");
            System.setProperty("http.proxyPort", "8083");
            URL url = new URL(urlString);
            HttpURLConnection connection = (HttpURLConnection) url.openConnection();
            InputStream is = connection.getInputStream();
            rssFeeds = RSSDomParser.parseRss(is);
        } catch (Exception e) {
            e.printStackTrace();
        }
        TechFeedsLogger.debug("RssUtils::getRssFeeds()::END");
        return rssFeeds;
    }

    private static String buildSyndication(final RssFeeds rssFeeds) {
        TechFeedsLogger.debug("RssUtils::buildSyndication()::START");
        List l = rssFeeds.getRssItemsList();
        StringBuffer sb = new StringBuffer();
        FeedsVO feedsVO = null;
        sb.append("<a href='");
        sb.append(rssFeeds.getBlogLink());
        sb.append("' class='blogTitle' >");
        sb.append(rssFeeds.getBlogTitle());
        sb.append("</a>");
        sb.append("<br/>");
        for (int i = 0; i < l.size(); i++) {
            feedsVO = (FeedsVO) l.get(i);
            sb.append("<a href='");
            sb.append(feedsVO.getLink());
            sb.append("' >");
            sb.append(feedsVO.getTitle());
            sb.append("</a>");
            sb.append("<br/>");
        }
        TechFeedsLogger.debug("RssUtils::buildSyndication()::END");
        return sb.toString();
    }
}
