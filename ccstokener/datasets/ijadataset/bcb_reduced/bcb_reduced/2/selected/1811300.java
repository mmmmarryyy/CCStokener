package org.spamerdos.app;

import java.io.*;
import java.net.*;
import java.util.*;

/**
@author Thomas Cherry jceaser@mac.com
*/
public class JoeWein extends SpamList {

    private static final String markStart = "<!--SPAMDOMAINS-->";

    private static final String markStop = "<!--/SPAMDOMAINS-->";

    private static final String site = "http://www.joewein.de";

    private static final String path = "/sw/blacklist.htm";

    private long timeStamp;

    public JoeWein() throws java.net.MalformedURLException {
        super(new URL(site + path));
    }

    /** Unit Test */
    public static void main(String args[]) {
        try {
            JoeWein jw = new JoeWein();
            System.out.println(jw.toString());
        } catch (java.net.MalformedURLException mue) {
            System.err.println(mue.toString());
        }
    }

    protected void loadList() {
        if ((System.currentTimeMillis() - this.timeStamp) >= 43200000) {
            this.timeStamp = System.currentTimeMillis();
            int letter;
            StringBuffer scan = new StringBuffer(markStop.length() + 1);
            StringBuffer dataBuffer = new StringBuffer();
            boolean store = false;
            try {
                InputStream in = this.url.openStream();
                while ((letter = in.read()) != -1) {
                    char cLetter = (char) letter;
                    if (store) {
                        if (cLetter == '\r') {
                            String spamer = dataBuffer.toString().trim();
                            if (!spamer.equals("")) {
                                this.spamers.add(spamer);
                                dataBuffer.delete(0, dataBuffer.length());
                            }
                        } else {
                            dataBuffer.append(cLetter);
                        }
                    }
                    scan.append(cLetter);
                    if (scan.length() > markStop.length()) {
                        scan.delete(0, 1);
                    }
                    String scanStr = scan.toString();
                    if (scanStr.endsWith(markStart)) {
                        store = true;
                    }
                    if (scanStr.equals(markStop)) {
                        break;
                    }
                }
            } catch (java.io.IOException ioe) {
                System.err.println("Error in '" + this.getClass().getName() + "' " + ioe.toString());
            }
        }
    }

    public boolean reloadList() {
        return (System.currentTimeMillis() - this.timeStamp) > 43200000;
    }

    public String toString() {
        return "JoeWein[url=" + this.url.toString() + ", count=" + this.spamers.size() + "]";
    }
}
