package com.luntzel.FeedScraper;

import com.sun.syndication.feed.synd.*;
import com.sun.syndication.io.*;
import java.io.*;
import java.net.URL;
import java.net.*;
import java.util.*;
import java.sql.*;

public class FeedScraper {

    /** Global database parameters */
    static Statement st;

    static Connection con;

    String dburl = "jdbc:mysql://localhost:3306/feedfilter";

    String dbuser = "feeder";

    String dbpass = "feeder";

    /** Parses RSS or Atom to instantiate a SyndFeed. */
    private SyndFeedInput input;

    /** Transforms SyndFeed to RSS or Atom XML. */
    private SyndFeedOutput output;

    /**
     * Default constructor.
     */
    public FeedScraper() throws SQLException, ClassNotFoundException {
        input = new SyndFeedInput();
        output = new SyndFeedOutput();
        Class.forName("com.mysql.jdbc.Driver");
        con = DriverManager.getConnection(dburl, dbuser, dbpass);
    }

    public String processFeed(URL url, String outFormat) throws IOException, FeedException, SQLException, InstantiationException, IllegalAccessException, ClassNotFoundException, ParsingFeedException {
        URLConnection urlConnection = url.openConnection();
        urlConnection.setRequestProperty("User-Agent", "myuseragent");
        SyndFeed feed = input.build(new XmlReader(urlConnection));
        feed.setFeedType(outFormat);
        Iterator entryIter = feed.getEntries().iterator();
        while (entryIter.hasNext()) {
            SyndEntry entry = (SyndEntry) entryIter.next();
            String description = entry.getDescription().getValue().toString();
            String title = entry.getTitle();
            String nohtmlDescription = description.replaceAll("\\<.*?>", "");
            String nohtmlDescription2 = nohtmlDescription.replaceAll("\"", "\'");
            String nohtmlTitle = title.replaceAll("\\<.*?>", "");
            String nohtmlTitle2 = nohtmlTitle.replaceAll("\"", "\'");
            st = con.createStatement();
            st.executeUpdate("REPLACE INTO feeddata(id, storyid, pubdate, url, title, summary) VALUES ((SELECT id FROM feeds WHERE feeds.feedlink = \'" + url + "\'), 0," + "\"" + entry.getPublishedDate() + "\", \"" + entry.getUri() + "\"," + "\"" + nohtmlTitle2 + "\"," + "\"" + nohtmlDescription2 + "\")");
        }
        StringWriter writer = new StringWriter();
        output.output(feed, writer);
        return writer.toString();
    }

    /**
     * Main method 
     */
    public static void main(String[] args) throws Exception, SQLException {
        FeedScraper filter = new FeedScraper();
        st = con.createStatement();
        ResultSet rs;
        rs = st.executeQuery("SELECT feedlink FROM feeds");
        while (rs.next()) {
            filter.processFeed(new URL(rs.getString(1)), "rss_2.0");
            String processedFeed = filter.processFeed(new URL(rs.getString(1)), "rss_2.0");
        }
        System.out.println("Done scraping");
    }
}
