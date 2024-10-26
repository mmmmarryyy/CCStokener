package net.sf.unlpbot.searchengines.google;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.URL;
import java.net.URLConnection;
import java.util.*;
import com.meterware.httpunit.WebLink;
import java.util.regex.*;

/**
 * uNLPBot: Unintelligent Natural Language Processing chatter BOT<p>												
 * Copyright (C) 2004. All rights reserved.<p>
 * Use is subject to license terms.<p>																							
 * Initial developer(s):	gni.<p>		
 * @author gni at users.sourceforge.net<p>
 */
public class uGoogleWhatIs extends uGoogle {

    public String what = null;

    public uGoogleWhatIs(String what) {
        super();
        this.what = what;
        String question = "\"What is " + what + "\"";
        String answer = "\"" + what + " is\"";
        String query = question + " " + answer;
        this.setQuery(query);
    }

    public uGoogleWhatIs(String what, int num) {
        super();
        this.what = what;
        String question = "\"What is " + what + "\"";
        String answer = "\"" + what + " is\"";
        String query = question + " " + answer;
        this.setQuery(query);
        this.setMaxresults(num);
    }

    protected String process(String what, String str) {
        if (str.contains(what + " is")) {
            Pattern unpattern = Pattern.compile("[^\\p{Alnum}������]");
            Matcher unmatcher = unpattern.matcher(str);
            String cleanedmessage = unmatcher.replaceAll(" ");
            return cleanedmessage;
        } else {
            return null;
        }
    }

    public Vector getDefinitions() {
        Vector v = null;
        v = new Vector();
        if (this.wlv == null) {
            this.wlv = this.search();
        }
        if (wlv != null) {
            int n = wlv.size();
            for (int i = 0; i < n; i++) {
                WebLink wl = wlv.weblinkAt(i);
                String sdesc = wl.getText();
                String surl = wl.getURLString();
                if (surl.startsWith("http://") && !surl.contains(".google.") && !(sdesc.compareTo("Cached") == 0) && !surl.endsWith("pdf")) {
                    HttpURLConnection.setFollowRedirects(false);
                    URL url = null;
                    try {
                        url = new URL(surl);
                    } catch (Exception e) {
                        break;
                    }
                    URLConnection c = null;
                    try {
                        c = url.openConnection();
                    } catch (Exception e) {
                        break;
                    }
                    InputStream istream = null;
                    try {
                        istream = c.getInputStream();
                    } catch (Exception e) {
                        break;
                    }
                    BufferedReader reader = null;
                    try {
                        reader = new BufferedReader(new InputStreamReader(istream));
                        String str = null;
                        while ((str = reader.readLine()) != null) {
                            String ans = this.process(this.what, str);
                            if (ans != null) {
                                v.add(ans);
                            }
                        }
                    } catch (IOException e) {
                        break;
                    }
                }
            }
        }
        return v;
    }

    public Vector getDefinitions(int i) {
        Vector v = null;
        v = new Vector();
        if (this.wlv == null) {
            this.wlv = this.search();
        }
        int n = 0;
        if (wlv != null) {
            n = this.wlv.size();
        }
        int j = 0;
        int k = 0;
        boolean found = false;
        if (n > 0 && i < n) {
            while (!found && j < n) {
                WebLink wl = wlv.weblinkAt(j);
                String sdesc = wl.getText();
                String surl = wl.getURLString();
                if (surl.startsWith("http://") && !surl.contains(".google.") && !(sdesc.compareTo("Cached") == 0) && !surl.endsWith("pdf")) {
                    found = true && (k == i);
                    HttpURLConnection.setFollowRedirects(false);
                    URL url = null;
                    try {
                        url = new URL(surl);
                    } catch (Exception e) {
                    }
                    URLConnection c = null;
                    try {
                        c = url.openConnection();
                    } catch (Exception e) {
                    }
                    InputStream istream = null;
                    try {
                        istream = c.getInputStream();
                    } catch (Exception e) {
                    }
                    BufferedReader reader = null;
                    try {
                        reader = new BufferedReader(new InputStreamReader(istream));
                        String str = null;
                        while ((str = reader.readLine()) != null) {
                            String ans = this.process(this.what, str);
                            if (ans != null) {
                                v.add(ans);
                            }
                        }
                    } catch (IOException e) {
                    }
                    k = k + 1;
                }
                j = j + 1;
            }
        }
        return v;
    }
}
