package org.discover.trading.io;

import java.io.BufferedInputStream;
import java.io.IOException;
import java.io.PrintWriter;
import java.net.MalformedURLException;
import java.net.URL;
import java.net.URLConnection;
import org.discover.trading.core.LiveQuote;
import org.discover.trading.core.RegexQuoteTemplate;
import org.discover.trading.core.RegexQuoteTemplateManager;
import org.discover.trading.core.Security;
import org.discover.trading.core.SimpleQuote;
import test.QuoteFetcher;

public class QuoteFetcherThread implements Runnable {

    private String stockName = null;

    private String url = null;

    private RegexQuoteTemplate quoteTemplate = null;

    private PrintWriter writer = null;

    public QuoteFetcherThread(String stockName, String url, RegexQuoteTemplate quoteTemplate, PrintWriter writer) {
        this.stockName = stockName;
        this.url = url;
        this.quoteTemplate = quoteTemplate;
        this.writer = writer;
    }

    private synchronized SimpleQuote getCalculatedQuote(LiveQuote currQuote, LiveQuote prevQuote) {
        SimpleQuote quote = new SimpleQuote();
        if (prevQuote == null) {
        } else {
            quote.setDate(currQuote.getLastTradedTime());
            quote.setOpen(prevQuote.getLastTradedPrice());
            quote.setClose(currQuote.getLastTradedPrice());
            quote.setHigh(prevQuote.getLastTradedPrice() > currQuote.getLastTradedPrice() ? prevQuote.getLastTradedPrice() : currQuote.getLastTradedPrice());
            quote.setLow(prevQuote.getLastTradedPrice() < currQuote.getLastTradedPrice() ? prevQuote.getLastTradedPrice() : currQuote.getLastTradedPrice());
            quote.setVolume(currQuote.getVolume() - prevQuote.getVolume());
        }
        return quote;
    }

    public void run() {
        TextQuoteReader quoteReader = new TextQuoteReader(new Security(), null, quoteTemplate);
        try {
            String src = GetHtmlSource();
            LiveQuote currQuote = (LiveQuote) quoteReader.read(src);
            LiveQuote prevQuote = (LiveQuote) QuoteFetcher.getQuoteManager().getLastQuote(stockName);
            if (currQuote != null && (!currQuote.equals(prevQuote))) {
                QuoteFetcher.getQuoteManager().add(stockName, currQuote);
                if (prevQuote != null) {
                    SimpleQuote quote = getCalculatedQuote(currQuote, prevQuote);
                    writer.println(quote);
                    System.out.println(quote);
                }
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
        try {
            Thread.sleep(1000);
        } catch (InterruptedException e) {
            e.printStackTrace();
        }
    }

    private String GetHtmlSource() {
        BufferedInputStream reader = null;
        String source = null;
        try {
            URLConnection connection = new URL(url).openConnection();
            reader = new BufferedInputStream(connection.getInputStream());
            connection.setReadTimeout(10000);
            int c = 0;
            source = "";
            while ((c = reader.read()) != -1) source = source + (char) c;
        } catch (MalformedURLException e1) {
            e1.printStackTrace();
        } catch (IOException e1) {
            e1.printStackTrace();
        } finally {
            try {
                reader.close();
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
        return source;
    }
}
