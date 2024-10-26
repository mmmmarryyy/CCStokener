package org.aiotrade.platform.modules.dataserver.basic;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.URL;
import java.text.ParseException;
import java.util.Calendar;
import java.util.Collection;
import java.util.Date;
import org.aiotrade.platform.core.dataserver.TickerContract;
import org.aiotrade.platform.core.dataserver.TickerServer;
import org.aiotrade.platform.core.sec.Ticker;
import org.aiotrade.platform.core.sec.TickerSnapshot;

/**
 * This class will load the quote datas from data source to its data storage: quotes.
 * @TODO it will be implemented as a Data Server ?
 *
 * @author Caoyuan Deng
 */
public class YahooTickerServer extends TickerServer {

    /**
     * !NOTICE
     * If the remote datafeed keeps only one inputstream for all subscriiebed
     * symbols, one singleton instance is enough. If each symbol need a separate
     * session, you may create new data server instance for each symbol.
     */
    private static YahooTickerServer singletonInstance;

    private Calendar cal = Calendar.getInstance();

    protected boolean connect() {
        return true;
    }

    /**
     * Template:
     * http://quote.yahoo.com/download/javasoft.beans?symbols=^HSI+YHOO+SUMW&&format=sl1d1t1c1ohgvbap
     */
    protected void request() throws Exception {
        cal.clear();
        StringBuilder urlStr = new StringBuilder(90);
        urlStr.append("http://quote.yahoo.com/d/quotes.csv").append("?s=");
        Collection<TickerContract> contracts = getSubscribedContracts();
        if (contracts.size() == 0) {
            setInputStream(null);
            setLoadedTime(getFromTime());
            return;
        }
        for (TickerContract contract : contracts) {
            urlStr.append(contract.getSymbol()).append("+");
        }
        urlStr = urlStr.deleteCharAt(urlStr.length() - 1);
        urlStr.append("&d=t&f=sl1d1t1c1ohgvbap");
        String urlStrForName = urlStr.append("&d=t&f=snx").toString();
        URL url = new URL(urlStr.toString());
        System.out.println(url);
        HttpURLConnection conn = (HttpURLConnection) url.openConnection();
        conn.setAllowUserInteraction(true);
        conn.setRequestMethod("GET");
        conn.setInstanceFollowRedirects(true);
        setInputStream(conn.getInputStream());
    }

    protected long read() throws Exception {
        if (getInputStream() == null) {
            return 0;
        }
        BufferedReader reader = new BufferedReader(new InputStreamReader(getInputStream()));
        long newestTime = -Long.MAX_VALUE;
        resetCount();
        cal.clear();
        boolean EOF = false;
        while (!EOF) {
            String s = reader.readLine();
            if (s == null) {
                break;
            }
            String[] items;
            items = s.split(",");
            if (items.length > 11) {
                String symbol = items[0].toUpperCase().replace('"', ' ').trim();
                String dateStr = items[2].replace('"', ' ').trim();
                String timeStr = items[3].replace('"', ' ').trim();
                if (dateStr.equalsIgnoreCase("N/A") || timeStr.equalsIgnoreCase("N/A")) {
                    continue;
                }
                try {
                    Date date = getDateFormat().parse(dateStr + " " + timeStr);
                    cal.clear();
                    cal.setTime(date);
                } catch (ParseException ex) {
                    ex.printStackTrace();
                    continue;
                }
                long time = cal.getTimeInMillis();
                if (time == 0) {
                    System.out.println("time of ticker: " + symbol + " is 0!");
                }
                TickerSnapshot tickerSnapshot = getTickerSnapshot(symbol);
                tickerSnapshot.setTime(time);
                tickerSnapshot.set(Ticker.LAST_PRICE, items[1].equalsIgnoreCase("N/A") ? 0 : Float.parseFloat(items[1].trim()));
                tickerSnapshot.set(Ticker.DAY_CHANGE, items[4].equalsIgnoreCase("N/A") ? 0 : Float.parseFloat(items[4].trim()));
                tickerSnapshot.set(Ticker.DAY_OPEN, items[5].equalsIgnoreCase("N/A") ? 0 : Float.parseFloat(items[5].trim()));
                tickerSnapshot.set(Ticker.DAY_HIGH, items[6].equalsIgnoreCase("N/A") ? 0 : Float.parseFloat(items[6].trim()));
                tickerSnapshot.set(Ticker.DAY_LOW, items[7].equalsIgnoreCase("N/A") ? 0 : Float.parseFloat(items[7].trim()));
                tickerSnapshot.set(Ticker.DAY_VOLUME, items[8].equalsIgnoreCase("N/A") ? 0 : Float.parseFloat(items[8].trim()) / 100f);
                tickerSnapshot.set(Ticker.BID_PRICE, items[9].equalsIgnoreCase("N/A") ? 0 : Float.parseFloat(items[9].trim()));
                tickerSnapshot.set(Ticker.ASK_PRICE, items[10].equalsIgnoreCase("N/A") ? 0 : Float.parseFloat(items[10].trim()));
                tickerSnapshot.set(Ticker.PREV_CLOSE, items[11].equalsIgnoreCase("N/A") ? 0 : Float.parseFloat(items[11].trim()));
                tickerSnapshot.setFullName(symbol);
                tickerSnapshot.notifyObservers();
                newestTime = Math.max(newestTime, time);
                countOne();
            }
        }
        if (getCount() > 0) {
            return getFromTime();
        } else {
            return newestTime;
        }
    }

    /**
     * Retrive data from Yahoo finance website
     * Template:
     * http://quote.yahoo.com/download/javasoft.beans?symbols=^HSI+YHOO+SUMW&&format=sl1d1t1c1ohgvbap
     *
     * @param afterThisTime from time
     */
    protected long loadFromSource(long afterThisTime) {
        setFromTime(afterThisTime + 1);
        long loadedTime = getLoadedTime();
        if (!connect()) {
            return loadedTime;
        }
        try {
            request();
            loadedTime = read();
        } catch (Exception ex) {
            System.out.println("Error in loading from source: " + ex.getMessage());
        }
        return loadedTime;
    }

    @Override
    public YahooTickerServer createNewInstance() {
        if (singletonInstance == null) {
            singletonInstance = (YahooTickerServer) super.createNewInstance();
            singletonInstance.init();
        }
        return singletonInstance;
    }

    public String getDisplayName() {
        return "Yahoo! Finance Internet";
    }

    public String getDefaultDateFormatString() {
        return "MM/dd/yyyy h:mma";
    }

    public byte getSourceSerialNumber() {
        return (byte) 1;
    }
}
