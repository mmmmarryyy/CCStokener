package org.phill84.twitsync.mod.twitter;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.UnsupportedEncodingException;
import java.net.URL;
import java.net.URLDecoder;
import java.util.List;
import java.util.Stack;
import javax.jdo.PersistenceManager;
import org.jdom.Document;
import org.jdom.Element;
import org.jdom.JDOMException;
import org.jdom.input.SAXBuilder;
import org.phill84.twitsync.db.Profile;
import org.phill84.twitsync.util.PMF;

public class Twitter {

    @SuppressWarnings("unchecked")
    public static List<Element> getTwitter(String username) throws IOException, JDOMException {
        URL url = new URL("http://phill84.org/twitter.php?u=" + username);
        BufferedReader reader = new BufferedReader(new InputStreamReader(url.openStream()));
        SAXBuilder builder = new SAXBuilder();
        Document twitFeed = builder.build(reader);
        Element root = twitFeed.getRootElement();
        List<Element> status = root.getChildren();
        return status;
    }

    public static Stack<String> process(String name, List<Element> status) throws UnsupportedEncodingException {
        Stack<String> stack = new Stack<String>();
        PersistenceManager pm = null;
        try {
            pm = PMF.get().getPersistenceManager();
            Profile profile = pm.getObjectById(Profile.class, name);
            long lastStatusId = profile.getLastStatusId();
            long newLastStatusId = Long.valueOf(status.get(0).getChildText("id"));
            if (newLastStatusId > lastStatusId) {
                profile.setLastStatusId(newLastStatusId);
                for (Element e : status) {
                    String text = e.getChildText("text");
                    long id = Long.valueOf(e.getChildText("id"));
                    if (text.charAt(0) != '@' && id > lastStatusId) {
                        text = URLDecoder.decode(text, "utf-8");
                        stack.add(text);
                    }
                    if (id == lastStatusId) {
                        break;
                    }
                }
            }
        } finally {
            pm.close();
        }
        return stack;
    }
}
