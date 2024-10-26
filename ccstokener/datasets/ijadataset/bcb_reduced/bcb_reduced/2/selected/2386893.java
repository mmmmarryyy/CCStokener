package org.blogtrader.platform.modules.dataserver.basic;

import java.io.*;
import java.net.HttpURLConnection;
import java.net.URL;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.Date;
import java.util.List;
import java.util.Locale;
import java.util.Set;
import org.blogtrader.platform.core.data.dataserver.AbstractTickerDataServer;
import org.blogtrader.platform.core.analysis.descriptor.TickerDataSourceDescriptor;
import org.blogtrader.platform.core.data.dataserver.AbstractTickerDataServer.IntervalLastTicker;
import org.blogtrader.platform.core.stock.Ticker;

/**
 * This class will load the quote datas from data source to its data storage: quotes.
 * @TODO it will be implemented as a Data Server ?
 *
 * @author Caoyuan Deng
 */
public class YahooTickerDataServer extends AbstractTickerDataServer {

    private YahooTickerDataServer singletonInstance;

    private long firstTime;

    private long lastTime;

    /**
     * Retrive data from Yahoo finance website
     * @param String stock code
     *
     * Template:
     * http://quote.yahoo.com/download/javasoft.beans?symbols=^HSI+YHOO+SUMW&&format=sl1d1t1c1ohgvbap
     */
    protected long loadFromSource(long afterThisTime) {
        TickerDataSourceDescriptor tickerDataSourceDescriptor = (TickerDataSourceDescriptor) dataSourceDescriptor;
        Calendar calendar = Calendar.getInstance();
        calendar.clear();
        SimpleDateFormat sdf = new SimpleDateFormat(tickerDataSourceDescriptor.dateFormat, Locale.US);
        Date startDate = new Date();
        if (afterThisTime == FIRST_TIME_LOAD) {
        } else {
            calendar.setTimeInMillis(afterThisTime);
            startDate = calendar.getTime();
        }
        BufferedReader dis;
        boolean EOF = false;
        StringBuffer urlStr = new StringBuffer();
        urlStr.append("http://quote.yahoo.com/d/quotes.csv").append("?s=");
        Set<String> symbols = registeredTimeSeriesMap.keySet();
        if (symbols.size() == 0) {
            return afterThisTime;
        }
        for (String symbol : symbols) {
            urlStr.append(symbol).append("+");
        }
        urlStr = urlStr.deleteCharAt(urlStr.length() - 1);
        urlStr.append("&d=t&f=sl1d1t1c1ohgvbap");
        String urlStrForName = urlStr + "&d=t&f=snx";
        try {
            URL url = new URL(urlStr.toString());
            System.out.println(url);
            HttpURLConnection conn = (HttpURLConnection) url.openConnection();
            conn.setAllowUserInteraction(true);
            conn.setRequestMethod("GET");
            conn.setInstanceFollowRedirects(true);
            InputStreamReader isr = new InputStreamReader(conn.getInputStream());
            dis = new BufferedReader(isr);
            count = 0;
            calendar.clear();
            while (!EOF) {
                String s = dis.readLine();
                if (s == null) {
                    break;
                }
                String[] items;
                items = s.split(",");
                if (items.length > 11) {
                    String symbol = items[0].toUpperCase().replace('"', ' ').trim();
                    List<Ticker> dataPool = dataPools.get(symbol);
                    if (dataPool == null) {
                        continue;
                    }
                    String dateStr = items[2].replace('"', ' ').trim();
                    String timeStr = items[3].replace('"', ' ').trim();
                    if (dateStr.equalsIgnoreCase("N/A") || timeStr.equalsIgnoreCase("N/A")) {
                        continue;
                    }
                    Date date = null;
                    try {
                        date = sdf.parse(dateStr + " " + timeStr);
                    } catch (ParseException ex) {
                        ex.printStackTrace();
                        continue;
                    }
                    calendar.clear();
                    calendar.setTime(date);
                    long time = calendar.getTimeInMillis();
                    if (time == 0) {
                        System.out.println("time of ticker: " + symbol + " is 0!");
                    }
                    float dayVolume = (items[8].equalsIgnoreCase("N/A")) ? 0 : Float.parseFloat(items[8].trim()) / 100f;
                    IntervalLastTicker tickerHelper = tickerHelperMap.get(symbol);
                    Ticker prevTicker = currentTickerMap.get(symbol);
                    if (tickerHelper != null && prevTicker != null) {
                        boolean tickerRenewed = (dayVolume != prevTicker.dayVolume) ? true : false;
                        if (tickerRenewed == false) {
                            continue;
                        }
                    }
                    Ticker ticker = new Ticker();
                    ticker.fullName = symbol;
                    ticker.time = time;
                    ticker.lastPrice = (items[1].equalsIgnoreCase("N/A")) ? 0 : Float.parseFloat(items[1].trim());
                    ticker.dayChange = (items[4].equalsIgnoreCase("N/A")) ? 0 : Float.parseFloat(items[4].trim());
                    ticker.dayOpen = (items[5].equalsIgnoreCase("N/A")) ? 0 : Float.parseFloat(items[5].trim());
                    ticker.dayHigh = (items[6].equalsIgnoreCase("N/A")) ? 0 : Float.parseFloat(items[6].trim());
                    ticker.dayLow = (items[7].equalsIgnoreCase("N/A")) ? 0 : Float.parseFloat(items[7].trim());
                    ticker.dayVolume = dayVolume;
                    ticker.bidPrice = (items[9].equalsIgnoreCase("N/A")) ? 0 : Float.parseFloat(items[9].trim());
                    ticker.askPrice = (items[10].equalsIgnoreCase("N/A")) ? 0 : Float.parseFloat(items[10].trim());
                    ticker.prevClose = (items[11].equalsIgnoreCase("N/A")) ? 0 : Float.parseFloat(items[11].trim());
                    dataPool.add(ticker);
                    if (count == 0) {
                        firstTime = time;
                    }
                    lastTime = time;
                    ascending = (lastTime >= firstTime) ? true : false;
                    count++;
                }
            }
        } catch (EOFException ex) {
            EOF = true;
        } catch (IOException ex) {
            System.out.println("Error in Reading File");
        }
        long newestTime = (lastTime >= firstTime) ? lastTime : firstTime;
        if (count > 0) {
            System.out.println("ticker count : " + count);
            return afterThisTime + 1;
        } else {
            return newestTime;
        }
    }

    public YahooTickerDataServer createNewInstance() {
        if (singletonInstance == null) {
            singletonInstance = (YahooTickerDataServer) super.createNewInstance();
        }
        return singletonInstance;
    }

    public String getName() {
        return "Yahoo! Finance Internet";
    }

    public String getDefaultDateFormat() {
        return "MM/dd/yyyy h:mma";
    }
}
