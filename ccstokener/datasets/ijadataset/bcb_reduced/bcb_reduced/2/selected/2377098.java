package com.google.code.sapwcrawler.robotstxt;

import java.util.*;
import java.util.regex.Pattern;
import java.util.regex.Matcher;
import java.io.*;
import java.net.*;
import com.google.code.sapwcrawler.CrawlerConfig;

public class RobotsTxtConfig implements CrawlerConfig {

    private int delay = 0;

    private Set<String> disallowed = Collections.synchronizedSet(new HashSet<String>());

    private URL url;

    private String robotName;

    public RobotsTxtConfig(String host, String robotName) throws MalformedURLException {
        this(new URL("http://" + host + "/robots.txt"), robotName);
    }

    public RobotsTxtConfig(String host) throws MalformedURLException {
        this(new URL("http://" + host + "/robots.txt"));
    }

    public RobotsTxtConfig(URL url, String robotName) {
        this.url = url;
        this.robotName = robotName;
    }

    public RobotsTxtConfig(URL url) {
        this.url = url;
        this.robotName = "sapwcrawler";
    }

    public Integer getCrawlDelay() {
        return delay;
    }

    public Set<String> getDisallowedPaths() {
        return disallowed;
    }

    public Boolean isEnabled() {
        return true;
    }

    public URL getURL() {
        return url;
    }

    public void init() throws Exception {
        HttpURLConnection conn = (HttpURLConnection) url.openConnection();
        int code = conn.getResponseCode();
        if (code != 200) throw new IOException("Error fetching robots.txt; respose code is " + code);
        BufferedReader reader = new BufferedReader(new InputStreamReader(conn.getInputStream()));
        String buff;
        StringBuilder builder = new StringBuilder();
        while ((buff = reader.readLine()) != null) builder.append(buff);
        parseRobots(builder.toString());
    }

    private static Pattern pRobotsEntry = Pattern.compile("^([^:#]+):([^:#]+?)($|#)");

    private static enum RobotsTxtSectionType {

        Special, General
    }

    private void parseRobots(String data) {
        boolean isMyOptions = true;
        RobotsTxtSectionType sectionType = RobotsTxtSectionType.General;
        Matcher m = pRobotsEntry.matcher(data);
        while (m.find()) {
            String key = m.group(1).trim().toLowerCase();
            String val = m.group(2).trim();
            if (key.equals("user-agent")) {
                if (val.equals("*")) {
                    isMyOptions = true;
                    sectionType = RobotsTxtSectionType.General;
                } else if (isMe(val)) {
                    isMyOptions = true;
                    sectionType = RobotsTxtSectionType.Special;
                } else {
                    isMyOptions = false;
                    sectionType = RobotsTxtSectionType.General;
                }
                continue;
            }
            if (!isMyOptions) continue;
            if (key.equals("disallow")) {
                disallowed.add(val);
            }
            if (key.equals("allow") && sectionType == RobotsTxtSectionType.Special && disallowed.contains(val)) {
                disallowed.remove(val);
            }
            if (key.equals("crawl-delay")) {
                if (sectionType == RobotsTxtSectionType.Special || this.delay == 0) this.delay = Integer.parseInt(val);
            }
        }
    }

    private boolean isMe(String useragent) {
        return useragent.toLowerCase().startsWith(robotName);
    }
}
