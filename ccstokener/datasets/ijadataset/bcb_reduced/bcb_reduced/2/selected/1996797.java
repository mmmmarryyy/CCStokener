package edu.fudan.cse.medlab.event.crawler;

import java.io.IOException;
import java.net.HttpURLConnection;
import java.net.MalformedURLException;
import java.net.URL;
import java.net.URLConnection;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class ScanHtml {

    public static String[] scanhtml(URL site, URL url, String html) {
        if (html == null) return null;
        String regex = "((href=[^ ] )|href=\"[^\"'> ]*\")|(href='[^\"'> ]*')|(href=[^\"'> ]*>)";
        Pattern p = Pattern.compile(regex);
        Matcher m = p.matcher(html);
        String urllist = "";
        String temp;
        while (m.find()) {
            temp = m.group();
            int dd;
            if (temp.endsWith(" ")) temp = temp.substring(temp.indexOf("=") + 1, temp.lastIndexOf(" ")); else if (temp.contains("\"")) temp = temp.substring(temp.indexOf("\"") + 1, temp.lastIndexOf("\"")); else if (temp.contains("\'")) temp = temp.substring(temp.indexOf("\'") + 1, temp.lastIndexOf("\'")); else if (temp.contains(">")) temp = temp.substring(temp.indexOf("=") + 1, temp.lastIndexOf(">")); else continue;
            temp = temp.trim();
            URL linkurl1 = null;
            try {
                linkurl1 = new URL(temp);
            } catch (MalformedURLException e) {
                try {
                    if (temp.startsWith("?")) {
                        if (url.toString().contains("?")) linkurl1 = new URL(url.toString().substring(0, url.toString().indexOf("?")) + temp); else linkurl1 = new URL(url.toString() + temp);
                    } else linkurl1 = new URL(url, temp);
                } catch (MalformedURLException e2) {
                    continue;
                }
            }
            if (linkurl1 != null && linkurl1.getAuthority() != null && linkurl1.getAuthority().equals(site.getAuthority()) && ScanHtml.isPage(linkurl1.toString())) {
                String lurl = linkurl1.toString().trim();
                if (lurl.contains(".asp") || lurl.contains(".aspx") || lurl.contains(".Asp") || lurl.contains(".Aspx")) lurl = lurl.toLowerCase();
                if (lurl.contains("#")) lurl = lurl.substring(0, lurl.indexOf("#"));
                lurl = lurl.replace("../", "");
                if (urllist.equals("")) urllist = lurl; else urllist += "!" + lurl;
            }
        }
        if (urllist == "") return null;
        String[] s = urllist.split("!");
        return s;
    }

    public static HttpURLConnection getValidConnection(URL url) {
        HttpURLConnection httpurlconnection = null;
        try {
            URLConnection urlconnection = url.openConnection();
            urlconnection.connect();
            if (!(urlconnection instanceof HttpURLConnection)) {
                return null;
            }
            httpurlconnection = (HttpURLConnection) urlconnection;
        } catch (IOException ioexception) {
            if (httpurlconnection != null) {
                httpurlconnection.disconnect();
            }
            return null;
        }
        return httpurlconnection;
    }

    public static boolean isPage(String s) {
        if (s.endsWith("?")) return false;
        if (s.contains("?") && s.substring(s.lastIndexOf("?") + 1).contains("/")) return false;
        if (s.endsWith("/")) s = s.substring(0, s.length() - 1);
        if (s.contains("/")) s = s.substring(s.lastIndexOf("/") + 1);
        if (s == "" || !s.contains(".") || s.contains(".html") || s.contains(".htm") || s.contains(".xhtml") || s.contains(".asp") || s.contains(".jsp") || s.contains(".php")) return true; else return false;
    }
}
