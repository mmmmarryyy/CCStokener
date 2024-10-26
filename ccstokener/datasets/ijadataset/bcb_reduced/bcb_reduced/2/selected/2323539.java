package org.mov.util;

import java.io.*;
import java.net.*;

public class SanfordDataSource implements InternetDataSource {

    public static void main(String[] args) {
        SanfordDataSource source = new SanfordDataSource();
        source.update();
    }

    public static final String HOST = "www.sanford.com.au";

    public void updateStockQuotes() {
        TradingDate latestQuoteDate = Database.getInstance().getLatestQuoteDate();
        TradingDate today = new TradingDate();
    }

    public void update() {
        try {
            System.out.println("url");
            URL url = new URL("https", HOST, "/sanford/Login.asp");
            System.out.println("connection");
            URLConnection connection = url.openConnection();
            System.out.println("content");
            Object content = connection.getContent();
            System.out.println("Content: ");
            System.out.println(content);
            System.out.println("done");
        } catch (java.io.IOException io) {
            System.out.println("io exception" + io);
        }
    }
}
