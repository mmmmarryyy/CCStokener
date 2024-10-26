package org.jtweet;

import java.io.BufferedInputStream;
import java.io.BufferedWriter;
import java.io.ByteArrayInputStream;
import java.io.OutputStreamWriter;
import java.net.HttpURLConnection;
import java.net.Socket;
import java.net.URL;
import java.net.URLEncoder;
import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import org.jtweet.exception.JTweetException;
import org.jtweet.util.Base64;
import org.w3c.dom.Document;
import org.w3c.dom.Node;

public class Twitter {

    private static Twitter instance;

    private static final String TWITTER_BASE = "http://www.twitter.com/", TWITTER_URL = TWITTER_BASE + "statuses/", TWITTER_USERS_URL = TWITTER_BASE + "users/show/";

    private static Thread thread;

    private DocumentBuilderFactory factory;

    private DocumentBuilder builder = null;

    private Document document = null;

    private boolean isDisposed;

    private Twitter() {
        isDisposed = false;
        factory = DocumentBuilderFactory.newInstance();
    }

    public static final Twitter get() {
        if (instance == null) {
            instance = new Twitter();
        }
        return instance;
    }

    public final void dispose() {
        isDisposed = true;
    }

    public final Node getPublicTimeline() throws JTweetException {
        return openConnection(TWITTER_URL + "public_timeline.xml");
    }

    public final Node getPublicTimeline(int since) throws JTweetException {
        return openConnection(TWITTER_URL + "public_timeline.xml?since_id=" + since);
    }

    public final Node getFriendsTimeline(String username, String password) throws JTweetException {
        return openAuthConnection(username, password, TWITTER_URL + "friends_timeline.xml");
    }

    public final Node getFriendsTimeline(String username, String password, String id) throws JTweetException {
        return openAuthConnection(username, password, TWITTER_URL + "friends_timeline/" + id + ".xml");
    }

    public final Node getFriendsTimeline(String username, String password, int count) throws JTweetException {
        return openAuthConnection(username, password, TWITTER_URL + "friends_timeline.xml?count=" + count);
    }

    public final Node getUserTimeline(String username, String password) throws JTweetException {
        return openAuthConnection(username, password, TWITTER_URL + "user_timeline.xml");
    }

    public final Node update(String username, String password, String message) throws JTweetException {
        return postAuthConnection(username, password, "/statuses/update.xml", "status", message);
    }

    public final Node getFriends(String username, String password) throws JTweetException {
        return openAuthConnection(username, password, TWITTER_URL + "friends.xml");
    }

    public final Node getUsersFriends(String username, String password, String user) throws JTweetException {
        return openAuthConnection(username, password, TWITTER_URL + "friends/ " + user + ".xml");
    }

    public final Node getUsersFriends(String username, String password, int user) throws JTweetException {
        return openAuthConnection(username, password, TWITTER_URL + "friends/ " + user + ".xml");
    }

    public final Node getFollowers(String username, String password) throws JTweetException {
        return openAuthConnection(username, password, TWITTER_URL + "followers.xml");
    }

    public final Node showUser(String username, String password, String user) throws JTweetException {
        return openAuthConnection(username, password, TWITTER_USERS_URL + user + ".xml");
    }

    public final Node showUser(String username, String password, int user) throws JTweetException {
        return openAuthConnection(username, password, TWITTER_USERS_URL + user + ".xml");
    }

    public final Node getDirectMessages(String username, String password) throws JTweetException {
        return openAuthConnection(username, password, TWITTER_BASE + "direct_messages.xml");
    }

    public final Node getDirectMessages(String username, String password, String since) throws JTweetException {
        return openAuthConnection(username, password, TWITTER_BASE + "direct_messages.xml?since=" + since);
    }

    public final Node sendDirectMessage(String username, String password, String user, String text) throws JTweetException {
        return null;
    }

    private final Node openConnection(String connection) throws JTweetException {
        try {
            URL url = new URL(connection);
            HttpURLConnection conn = (HttpURLConnection) url.openConnection();
            conn.connect();
            BufferedInputStream reader = new BufferedInputStream(conn.getInputStream());
            if (builder == null) {
                builder = factory.newDocumentBuilder();
            }
            document = builder.parse(reader);
            reader.close();
            conn.disconnect();
        } catch (Exception e) {
            throw new JTweetException(e);
        }
        return document.getFirstChild();
    }

    private final Node openAuthConnection(String username, String password, String connection) throws JTweetException {
        try {
            URL url = new URL(connection);
            HttpURLConnection conn = (HttpURLConnection) url.openConnection();
            String encode = Base64.encode(username + ":" + password);
            conn.addRequestProperty("Authorization", "Basic " + encode);
            conn.connect();
            BufferedInputStream reader = new BufferedInputStream(conn.getInputStream());
            if (builder == null) {
                builder = factory.newDocumentBuilder();
            }
            document = builder.parse(reader);
            reader.close();
            conn.disconnect();
        } catch (Exception e) {
            throw new JTweetException(e);
        }
        return document.getFirstChild();
    }

    private final Node postAuthConnection(final String username, final String password, String path, String key, String message) throws JTweetException {
        try {
            Socket socket = new Socket("twitter.com", 80);
            String data = URLEncoder.encode(key, "UTF-8") + "=" + URLEncoder.encode(message, "UTF-8");
            String encode = Base64.encode(username + ":" + password);
            BufferedWriter wr = new BufferedWriter(new OutputStreamWriter(socket.getOutputStream(), "UTF-8"));
            wr.write("POST " + path + " HTTP/1.1\r\n");
            wr.write("Authorization: Basic " + encode + "\r\n");
            wr.write("Content-Length: " + data.length() + "\r\n");
            wr.write("Content-Type: application/x-www-form-urlencoded\r\n");
            wr.write("\r\n");
            wr.write(data);
            wr.write("\r\n");
            wr.flush();
            BufferedInputStream reader = new BufferedInputStream(socket.getInputStream());
            StringBuffer b = new StringBuffer();
            int line;
            while ((line = reader.read()) != -1) {
                b.append((char) line);
            }
            if (builder == null) {
                builder = factory.newDocumentBuilder();
            }
            int x = b.indexOf("<?xml");
            if (x >= 0) {
                String out = b.substring(x, b.length());
                document = builder.parse(new ByteArrayInputStream(out.getBytes()));
            } else {
                document = null;
            }
            reader.close();
            wr.close();
            socket.close();
        } catch (Exception e) {
            throw new JTweetException(e);
        }
        return document == null ? null : document.getFirstChild();
    }
}
