package com.byjyate.rssdreamwork;

import java.io.*;
import java.net.*;
import java.util.*;

public class HtmlFetcher {

    private URL url;

    public static final String STANDARDENCODING = "utf-8";

    private static final Integer FETCHLIMIT = 196608;

    private static final String DEFAULTPROTOCAL = "http://";

    public HtmlFetcher(String url) throws MalformedURLException {
        if (url == null || url.isEmpty()) throw new NullPointerException();
        url = url.trim();
        if (!url.toLowerCase().startsWith("http")) url = DEFAULTPROTOCAL + url;
        this.url = new URL(url);
    }

    public BufferedReader getInputStream() throws IOException {
        String encoding = getEncoding();
        return new BufferedReader(new InputStreamReader(url.openStream(), encoding));
    }

    public String getContent() throws IOException {
        StringBuilder contentBuilder = new StringBuilder();
        BufferedReader reader = null;
        try {
            reader = getInputStream();
            String line;
            while ((line = reader.readLine()) != null && contentBuilder.length() <= FETCHLIMIT) contentBuilder.append(line).append('\n');
        } finally {
            if (reader != null) reader.close();
        }
        String content = contentBuilder.toString();
        if (content.length() > FETCHLIMIT) content.substring(0, FETCHLIMIT - 1);
        return content;
    }

    private String getEncoding() throws IOException {
        BufferedReader reader = null;
        String encoding = null;
        try {
            URLConnection connection = url.openConnection();
            Map<String, List<String>> header = connection.getHeaderFields();
            for (Map.Entry<String, List<String>> entry : header.entrySet()) {
                if (entry.getKey().toLowerCase().equals("content-type")) {
                    String item = entry.getValue().toString().toLowerCase();
                    if (item.contains("charset")) {
                        encoding = extractEncoding(item);
                        if (encoding != null && !encoding.isEmpty()) return encoding;
                    }
                }
            }
            reader = new BufferedReader(new InputStreamReader(connection.getInputStream()));
            String line;
            while ((line = reader.readLine()) != null) {
                line = line.toLowerCase();
                if (line.contains("charset") || line.contains("encoding")) {
                    encoding = extractEncoding(line);
                    if (encoding != null && !encoding.isEmpty()) return encoding;
                }
            }
            return STANDARDENCODING;
        } finally {
            if (reader != null) reader.close();
        }
    }

    private String extractEncoding(String item) {
        if (item.contains("gb2312")) return "gb2312";
        if (item.contains("gbk")) return "gbk";
        if (item.contains("utf-8")) return "utf-8";
        if (item.contains("big5")) return "big5";
        return null;
    }
}
