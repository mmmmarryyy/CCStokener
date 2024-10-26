package org.spamerdos.app;

import java.io.*;
import java.net.*;
import java.util.*;

/**
@author Thomas Cherry jceaser@mac.com
*/
public class SpamCop extends SpamList {

    private static final String markStart = "<a href=\"/sc?track=http://";

    private static final String markStop = "\">";

    private static final String site = "http://www.spamcop.net";

    private static final String path = "/w3m?action=inprogress&type=www";

    private long timeStamp;

    public SpamCop() throws java.net.MalformedURLException {
        super(new URL(site + path));
    }

    /** Unit Test */
    public static void main(String args[]) {
        try {
            SpamCop sc = new SpamCop();
            System.out.println(sc.toString());
        } catch (java.net.MalformedURLException mue) {
            System.err.println(mue.toString());
        }
    }

    protected void loadList() {
        String page = this.getData();
        int index = 0;
        this.timeStamp = System.currentTimeMillis();
        while (index < page.length()) {
            int start = page.indexOf(markStart, index);
            int end = 0;
            if (start > -1) {
                start += markStart.length();
                end = page.indexOf(markStop, start);
                String spamer = page.substring(start, end);
                if (spamer.startsWith("www.")) {
                    spamer = spamer.substring(4, spamer.length());
                }
                this.spamers.add(spamer);
            } else {
                break;
            }
            index = start;
        }
    }

    private String getData() {
        int letter;
        String data = "";
        try {
            StringBuffer dataBuffer = new StringBuffer();
            InputStream in = this.url.openStream();
            while ((letter = in.read()) != -1) {
                dataBuffer.append((char) letter);
            }
            data = dataBuffer.toString();
        } catch (java.io.IOException ioe) {
            System.err.println();
            System.err.println("Error in '" + this.getClass().getName() + "' " + ioe.toString());
        }
        return data;
    }

    public boolean reloadList() {
        return (System.currentTimeMillis() - this.timeStamp) > 300000;
    }

    public String toString() {
        return "SpamCop[url=" + this.url.toString() + ", count=" + this.spamers.size() + "]";
    }
}
