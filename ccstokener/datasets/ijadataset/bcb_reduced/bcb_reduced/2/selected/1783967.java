package com.dukesoftware.utils.test;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.MalformedURLException;
import java.net.URL;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Iterator;

public class YoutubeGoogleSearchDemo {

    public static void main(String[] args) {
        Collection<String> c = new ArrayList<String>();
        c.add("ABC");
        String urlString = "http://jp.youtube.com/watch?v=VsJBGWl79xU";
        URL url = null;
        try {
            url = new URL(urlString);
        } catch (MalformedURLException e) {
            e.printStackTrace();
            return;
        }
        HttpURLConnection con = null;
        BufferedReader bis = null;
        try {
            con = (HttpURLConnection) url.openConnection();
            con.setRequestMethod("GET");
            con.setRequestProperty("User-Agent", "");
            con.setInstanceFollowRedirects(true);
            InputStream is = con.getInputStream();
            InputStreamReader isr = new InputStreamReader(is, "Shift_JIS");
            bis = new BufferedReader(isr);
            String line = null;
            System.out.println("Contents");
            while ((line = bis.readLine()) != null) {
                System.out.println(line);
            }
        } catch (Exception e) {
            e.printStackTrace();
        } finally {
            if (bis != null) {
                try {
                    bis.close();
                } catch (IOException e) {
                }
            }
            if (con != null) {
                con.disconnect();
            }
        }
    }

    public static String baseURL = "http://www.google.com/search?hl=en&safe=off&q=";

    public static String imageBaseURL = "http://images.google.com/images?ndsp=21&um=1&hl=en&safe=off&q=Google&start=63&sa=N";

    public static final String createBaseQuery(Collection<String> keywords) {
        return createQuery(baseURL, keywords);
    }

    public static final String createImageQuery(Collection<String> keywords) {
        return createQuery(imageBaseURL, keywords);
    }

    protected static final String createQuery(String baseURL, Collection<String> keywords) {
        StringBuffer buf = new StringBuffer(baseURL);
        for (Iterator<String> it = keywords.iterator(); it.hasNext(); ) {
            buf.append(it.next()).append("+");
        }
        return buf.subSequence(0, buf.length() - 1).toString();
    }
}
