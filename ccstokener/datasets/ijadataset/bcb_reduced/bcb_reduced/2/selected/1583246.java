package org.chemicalcovers.utils;

import java.io.BufferedReader;
import java.io.DataOutputStream;
import java.io.IOException;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.URL;
import java.net.URLEncoder;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.Locale;
import java.util.TimeZone;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class URLExtractor {

    private StringBuilder pageBuffer = new StringBuilder();

    private ArrayList<String> results = new ArrayList<String>();

    public StringBuilder getPage(String url) {
        BufferedReader reader = null;
        try {
            pageBuffer.delete(0, pageBuffer.length());
            HttpURLConnection httpConn = (HttpURLConnection) new URL(url).openConnection();
            prepareConnection(httpConn);
            httpConn.setRequestMethod("GET");
            reader = new BufferedReader(new InputStreamReader(httpConn.getInputStream()));
            for (String line = reader.readLine(); line != null; line = reader.readLine()) {
                pageBuffer.append(line);
            }
            return pageBuffer;
        } catch (Throwable error) {
            pageBuffer.delete(0, pageBuffer.length());
            System.out.println("URLExtractor.getPage(): " + error);
            return null;
        } finally {
            try {
                if (reader != null) reader.close();
            } catch (IOException ex) {
                ex.printStackTrace();
            }
        }
    }

    public StringBuilder postPage(String url, String content) {
        BufferedReader reader = null;
        try {
            pageBuffer.delete(0, pageBuffer.length());
            HttpURLConnection httpConn = (HttpURLConnection) new URL(url).openConnection();
            prepareConnection(httpConn);
            httpConn.setRequestMethod("POST");
            httpConn.setDoInput(true);
            httpConn.setDoOutput(true);
            httpConn.setUseCaches(false);
            httpConn.setRequestProperty("Content-Type", "application/x-www-form-urlencoded");
            DataOutputStream outputStream = new DataOutputStream(httpConn.getOutputStream());
            outputStream.writeBytes(content);
            outputStream.close();
            reader = new BufferedReader(new InputStreamReader(httpConn.getInputStream()));
            for (String line = reader.readLine(); line != null; line = reader.readLine()) {
                pageBuffer.append(line);
            }
            reader.close();
            return pageBuffer;
        } catch (Throwable error) {
            pageBuffer.delete(0, pageBuffer.length());
            System.out.println("URLExtractor.getPage(): " + error);
            return null;
        } finally {
            try {
                if (reader != null) reader.close();
            } catch (IOException ex) {
                ex.printStackTrace();
            }
        }
    }

    private void prepareConnection(HttpURLConnection connection) {
        connection.setRequestProperty("Host", connection.getURL().getHost());
        connection.setRequestProperty("User-Agent", "Mozilla/5.0 (Windows; U; Windows NT 5.2; en-US; rv:1.9.0.2) Gecko/2008091620 Firefox/3.0.2");
        connection.setRequestProperty("Accept", "text/html,application/xhtml+xml,application/xml;q=0.9,*/*;q=0.8");
        connection.setRequestProperty("Accept-Language", "en-us,en;q=0.5");
        connection.setRequestProperty("Accept-Charset", "ISO-8859-1,utf-8;q=0.7,*;q=0.7");
        connection.setRequestProperty("Keep-Alive", "300");
        connection.setRequestProperty("Proxy-Connection", "keep-alive");
        Date date = new Date();
        SimpleDateFormat fo = new SimpleDateFormat("EEE, dd MMM yyyy HH:mm:ss 'GMT'", Locale.US);
        fo.setTimeZone(TimeZone.getTimeZone("GMT"));
        connection.setRequestProperty("If-Modified-Since", fo.format(date));
    }

    @SuppressWarnings("unchecked")
    public ArrayList<String> extract(String regex, int[] regexGroups) {
        results.clear();
        if (pageBuffer.length() > 0) {
            Pattern pattern = Pattern.compile(regex);
            Matcher matcher = pattern.matcher(pageBuffer);
            int regexGroupCount = regexGroups.length;
            while (matcher.find()) {
                for (int i = 0; i < regexGroupCount; i++) results.add(matcher.group(regexGroups[i]));
            }
        }
        return (ArrayList<String>) results.clone();
    }

    public ArrayList<String> extract(String regex, int regexGroup) {
        return extract(regex, new int[] { regexGroup });
    }

    public ArrayList<String> extract(String regex) {
        return extract(regex, new int[] { 0 });
    }

    public ArrayList<String> extract(String url, String regex, int[] regexGroups) {
        getPage(url);
        return extract(regex, regexGroups);
    }

    @SuppressWarnings("deprecation")
    public static void main(String[] args) throws Throwable {
        System.getProperties().put("http.proxyHost", "adsl");
        System.getProperties().put("http.proxyPort", "80");
        URLExtractor mainExtractor = new URLExtractor();
        String artist = URLEncoder.encode("chemical");
        String baseurl = "http://www.allcdcovers.com";
        int currentPage = 1;
        while (true) {
            System.out.println("Page " + currentPage);
            ArrayList<String> results = mainExtractor.extract(baseurl + "/search/all/all/" + artist + "/" + currentPage, "<a href=\"(/show/\\d*/([^/]*)/([^\"]*))\">", new int[] { 1 });
            for (String str : results) {
                System.out.println(baseurl + str);
            }
            if (mainExtractor.extract("<a href=\"(/search/all/all/" + artist + "/" + (++currentPage) + ")\" title=\"Next page\">", new int[] { 1 }).size() == 0) {
                break;
            }
        }
    }
}
