package org.filbyte.torrent.scraper;

import java.io.*;
import java.util.*;
import java.util.regex.*;
import org.apache.http.*;
import org.apache.http.client.methods.*;
import org.apache.http.impl.client.DefaultHttpClient;
import org.filbyte.StringUtils;

public class PirateBay {

    private static final Pattern verifier = Pattern.compile("[A-Za-z\\d]*");

    private static final Pattern p2 = Pattern.compile("\\<div class=\\\"detName\\\"\\>\\<a href\\=\\\"(.*?)\\\"");

    private static final Pattern p = Pattern.compile("([^\\>]*?)\\<\\/a\\>\\<\\/div\\>\n\t\t\t\\<a" + " href\\=\\\"(.*?)\\\" title\\=\\\"Download this torrent\\\"\\>\\<img src\\=\\\"http\\:" + "\\/\\/static\\.thepiratebay\\.org\\/img\\/dl\\.gif\\\" class\\=\\\"dl\\\" alt\\=" + "\\\"Download\\\" \\/\\>\\<\\/a\\>\\<a href\\=\\\"(.*?)\\\" title\\=\\\"Download " + "this torrent using magnet\\\"\\>\\<img src\\=\\\"http\\:\\/\\/static\\.thepirate" + "bay\\.org\\/img\\/icon-magnet\\.gif\\\" alt\\=\\\"Magnet link\\\" \\/\\>\\<\\/a\\>" + "(?:(?:\\<a [^\\>]*\\>)?\\<img [^\\>]*\\>(?:\\<\\/a\\>)?)*\n\t\t\t" + "\\<font class\\=\\\"detDesc\\\"\\>Uploaded [^z]*ze ([^\\,]*)\\, " + "ULed by \\<a.*?\n.*?\n.*?" + "\\<td align\\=\\\"right\\\"\\>(.*?)\\<\\/td\\>\n\t\t" + "\\<td align\\=\\\"right\\\"\\>(.*?)\\<\\/td\\>");

    private static DefaultHttpClient httpclient = new DefaultHttpClient();

    private static String getProperty(String text, Pattern p) {
        Matcher m = p.matcher(text);
        m.find();
        return m.group(1);
    }

    private static String getData(String url) throws IOException {
        HttpGet httpget = new HttpGet(url);
        HttpResponse response = httpclient.execute(httpget);
        HttpEntity entity = response.getEntity();
        if (entity == null) {
            throw new IOException();
        }
        StringBuffer sb = new StringBuffer();
        BufferedReader br = new BufferedReader(new InputStreamReader(entity.getContent()));
        String line;
        while (null != (line = br.readLine())) sb.append(line + "\n");
        entity.consumeContent();
        return sb.toString();
    }

    public static List<SwarmData> doSearch(String query) throws Exception {
        List<SwarmData> result = new ArrayList<SwarmData>();
        if (!verifier.matcher(query).matches()) throw new IllegalArgumentException();
        String str = getData("http://thepiratebay.org/search/" + query);
        Matcher m = p.matcher(str);
        while (m.find()) {
            SwarmData sd = new SwarmData();
            sd.name = m.group(1);
            sd.url = m.group(2);
            sd.size = m.group(4).replace("&nbsp;", " ");
            sd.seeders = Integer.parseInt(m.group(5));
            sd.leechers = Integer.parseInt(m.group(6));
            result.add(sd);
        }
        return result;
    }

    public static void cleanup() {
        httpclient.getConnectionManager().shutdown();
    }
}
