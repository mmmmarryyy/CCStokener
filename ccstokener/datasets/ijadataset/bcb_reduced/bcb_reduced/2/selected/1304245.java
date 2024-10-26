package org.discover.trading.analysis;

import java.io.IOException;
import java.net.MalformedURLException;
import java.net.URL;
import java.net.URLConnection;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Iterator;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import org.discover.trading.core.IQuote;
import org.discover.trading.core.LiveQuote;
import org.discover.trading.core.RegexQuoteTemplate;
import sun.net.www.http.HttpClient;

public class URLQuoteFetcherThread implements Runnable {

    private String symbol = null;

    private String urlString = null;

    private URLConnection urlConnection;

    private URL url = null;

    private RegexQuoteTemplate template;

    private String dateFormat = "dd-MMM-yyyy";

    URLQuoteFetcherThread(String symbol, String url, RegexQuoteTemplate template) {
        this.symbol = symbol;
        this.urlString = url;
        this.template = template;
        try {
            this.url = new URL(urlString + symbol);
        } catch (MalformedURLException e) {
            e.printStackTrace();
        }
    }

    URLQuoteFetcherThread(URL url, RegexQuoteTemplate template) {
        this.url = url;
        this.template = template;
    }

    public void run() {
        while (true) {
            StringBuffer strData = new StringBuffer();
            try {
                urlConnection = url.openConnection();
                urlConnection.setConnectTimeout(0);
            } catch (IOException e) {
                e.printStackTrace();
            }
            IQuote quote = fetchQuote(strData);
        }
    }

    private IQuote fetchQuote(StringBuffer strData) {
        LiveQuote quote = new LiveQuote();
        Iterator<String> iterator = template.getFields().iterator();
        while (iterator.hasNext()) {
            String field = iterator.next();
            String pattern = template.getPattern(field);
            Matcher matcher = Pattern.compile(pattern, Pattern.MULTILINE).matcher(strData.toString());
            int index = 0;
            if (matcher.find(index)) {
                String match = matcher.group(matcher.groupCount() - 4);
                if (field.equals("date")) {
                    try {
                        quote.setDate(new SimpleDateFormat(dateFormat).parse(match));
                    } catch (ParseException e) {
                        e.printStackTrace();
                    }
                } else if (field.equals("open")) {
                    quote.setOpen(Double.parseDouble(match));
                } else if (field.equals("high")) {
                    quote.setHigh(Double.parseDouble(match));
                } else if (field.equals("low")) {
                    quote.setLow(Double.parseDouble(match));
                } else if (field.equals("volume")) {
                    quote.setVolume(Integer.parseInt(match));
                } else if (field.equals("lastTradedPrice")) {
                    quote.setLastTradedPrice(Double.parseDouble(match));
                } else if (field.equals("lastTradedTime")) {
                    try {
                        quote.setLastTradedTime(new SimpleDateFormat(dateFormat).parse(match));
                    } catch (ParseException e) {
                        e.printStackTrace();
                    }
                } else if (field.equals("ask")) {
                    quote.setAsk(Double.parseDouble(match));
                } else if (field.equals("askSize")) {
                    quote.setAskSize(Integer.parseInt(match));
                } else if (field.equals("bid")) {
                    quote.setBid(Double.parseDouble(match));
                } else if (field.equals("bidSize")) {
                    quote.setBidSize(Integer.parseInt(match));
                }
            }
        }
        return quote;
    }
}
