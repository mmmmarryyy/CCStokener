package com.dushyant.framework;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.net.MalformedURLException;
import java.net.URL;
import java.util.ArrayList;
import java.util.List;
import javax.swing.JLabel;
import com.dushyant.framework.bean.Log;
import com.dushyant.framework.bean.Tweets;
import com.dushyant.framework.ui.Main;

public class Twitter {

    public void get(String user, int pages, int j) {
        for (int i = pages; i <= j; i++) {
            addLine(user, "http://twitter.com/statuses/user_timeline.xml?screen_name=" + user + "&page=", i);
        }
        Main.setLblPage(new JLabel("DONE"));
    }

    public List<String> addLine(String username, String URL, int page) {
        List<String> rss = new ArrayList<String>();
        try {
            URL url = new URL(URL + page);
            BufferedReader reader = new BufferedReader(new InputStreamReader(url.openStream()));
            String line;
            System.out.println(reader.readLine());
            while ((line = reader.readLine()) != null) {
                String string = "<text>";
                String string1 = "</text>";
                if (line.contains(string) && !line.contains("@") && !line.contains("http")) {
                    String tweet = line.replace(string, "").replace(string1, "").replace("'", "").trim();
                    final Tweets tweets = new Tweets(username, tweet, page, false);
                    int save = tweets.save();
                    tweets.setId((long) save);
                    Thread thread = new Thread(new Runnable() {

                        @Override
                        public void run() {
                            Main.addRow(tweets);
                        }
                    });
                    thread.start();
                    System.out.println(tweet);
                }
            }
            reader.close();
        } catch (MalformedURLException e) {
            Log.put(e.toString());
            System.out.println(e.toString());
        } catch (IOException e) {
            Log.put(e.toString());
            System.out.println(e.toString());
        } catch (Exception e) {
            Log.put(e.toString());
            System.out.println(e.toString());
        }
        return rss;
    }
}
