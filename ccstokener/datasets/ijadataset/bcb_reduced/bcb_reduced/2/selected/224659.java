package com.xjms.CoverLocator;

import java.net.URL;
import java.net.URLConnection;
import java.net.URLEncoder;
import java.io.DataOutputStream;
import java.io.DataInputStream;
import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.lang.StringBuffer;
import java.net.UnknownHostException;

public class CoverLocator {

    String discID;

    String imageURL;

    String artist;

    String title;

    String lookupName;

    long startTime;

    StringBuffer status;

    static final int NO_MATCH = 0;

    static final int LOW_MATCH = 4;

    static final int MED_MATCH = 5;

    static final int HIGH_MATCH = 9;

    static final int EXACT_MATCH = 10;

    int matchType = NO_MATCH;

    public CoverLocator(String artist, String title) {
        this.artist = artist;
        this.title = title.trim();
    }

    public String getLog() {
        return status.toString();
    }

    public String locate() {
        startTime = System.currentTimeMillis();
        imageURL = "";
        lookupName = artist + ", " + title;
        status = new StringBuffer(lookupName);
        status.append(". ");
        status.append("Attempting Level 1 Lookup. ");
        try {
            lookup();
        } catch (Exception e) {
            status.append("Exception: " + e.toString());
            return "";
        }
        if (matchType == NO_MATCH) {
            status.append("Attempting Level 2 Lookup. ");
            this.title = reduceTitle();
            try {
                lookup();
            } catch (Exception e) {
                status.append("Exception: " + e.toString());
                return "";
            }
        }
        if (matchType == NO_MATCH) {
            status.append("No Matches found. ");
        }
        status.append(" Duration: " + (System.currentTimeMillis() - startTime) + "ms");
        return imageURL;
    }

    private final String reduceTitle() {
        String newTitle = title;
        newTitle = reduceTitle(newTitle, "(");
        newTitle = reduceTitle(newTitle, "-");
        newTitle = reduceTitle(newTitle, "[");
        newTitle = reduceTitle(newTitle, "disc");
        return newTitle;
    }

    private final String reduceTitle(String title, String sub) {
        String newTitle = title;
        int idx = title.toUpperCase().indexOf(sub.toUpperCase());
        if (idx > 1) {
            newTitle = title.substring(0, idx);
            newTitle.trim();
        }
        return newTitle;
    }

    private final void lookup() throws Exception {
        try {
            URL url;
            URLConnection urlConn;
            DataOutputStream printout;
            BufferedReader input;
            url = new URL("http://www.amazon.com/exec/obidos/search-handle-form");
            urlConn = url.openConnection();
            urlConn.setDoInput(true);
            urlConn.setDoOutput(true);
            urlConn.setUseCaches(false);
            urlConn.setRequestProperty("Content-Type", "application/x-www-form-urlencoded");
            printout = new DataOutputStream(urlConn.getOutputStream());
            String content = "page=" + URLEncoder.encode("1") + "&index=" + URLEncoder.encode("music") + "&field-artist=" + URLEncoder.encode(artist) + "&field-title=" + URLEncoder.encode(title) + "&field-binding=" + URLEncoder.encode("");
            printout.writeBytes(content);
            printout.flush();
            printout.close();
            input = new BufferedReader(new InputStreamReader(urlConn.getInputStream()));
            String str;
            String keyword = "handle-buy-box=";
            int matches = 0;
            while (null != ((str = input.readLine()))) {
                int idStart = str.indexOf(keyword);
                if (idStart > 0) {
                    idStart = idStart + keyword.length();
                    String id = str.substring(idStart, idStart + 10);
                    status.append("Match: ");
                    status.append(id);
                    status.append(". ");
                    if (verifyMatch(id, title)) {
                        discID = id;
                        imageURL = "http://images.amazon.com/images/P/" + id + ".01.LZZZZZZZ.jpg";
                        matchType = EXACT_MATCH;
                    }
                }
            }
            input.close();
        } catch (Exception e) {
            throw e;
        }
    }

    private final boolean verifyMatch(String disc_id, String title) {
        try {
            URL url;
            URLConnection urlConn;
            DataOutputStream printout;
            BufferedReader input;
            url = new URL("http://www.amazon.com/exec/obidos/ASIN/" + disc_id);
            urlConn = url.openConnection();
            urlConn.setDoInput(true);
            urlConn.setDoOutput(true);
            urlConn.setUseCaches(false);
            urlConn.setRequestProperty("Content-Type", "application/x-www-form-urlencoded");
            input = new BufferedReader(new InputStreamReader(urlConn.getInputStream()));
            String str;
            boolean goodMatch = false;
            boolean match = false;
            while (null != ((str = input.readLine()))) {
                String keyword = title.toUpperCase();
                int idStart = str.toUpperCase().indexOf((keyword));
                if (idStart > 0) {
                    if (str.toUpperCase().endsWith(title.toUpperCase())) {
                        goodMatch = true;
                    } else {
                        match = true;
                    }
                }
            }
            input.close();
            if (goodMatch) {
                status.append("Exact Match. ");
                return true;
            } else if (match) {
                status.append("Inexact Match. ");
                return true;
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
        return true;
    }

    public static void main(String[] args) {
        CoverLocator b = new CoverLocator("Rush", "Chronicles Disc 2");
        System.out.println(b.locate());
        System.out.println(b.status.toString());
    }
}
