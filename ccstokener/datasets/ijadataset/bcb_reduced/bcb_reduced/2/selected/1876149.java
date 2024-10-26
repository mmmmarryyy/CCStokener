package org.aiotrade.platform.modules.dataserver.basic;

import java.awt.Image;
import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.URL;
import java.text.DateFormat;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.Date;
import java.util.Locale;
import org.aiotrade.platform.core.dataserver.QuoteContract;
import org.aiotrade.platform.core.dataserver.QuoteServer;
import org.aiotrade.platform.core.sec.Quote;
import org.openide.util.Utilities;

/**
 * This class will load the quote datas from data source to its data storage: quotes.
 * @TODO it will be implemented as a Data Server ?
 *
 * @author Caoyuan Deng
 */
public class YahooQuoteServer extends QuoteServer {

    private Calendar calendar = Calendar.getInstance();

    private DateFormat dateFormat = new SimpleDateFormat("yyyy-MM-dd", Locale.US);

    private DateFormat dateFormat_old = new SimpleDateFormat("dd-MMM-yy", Locale.US);

    private QuoteContract contract;

    protected boolean connect() {
        return true;
    }

    /**
     * Template:
     * http://table.finance.yahoo.com/table.csv?s=^HSI&a=01&b=20&c=1990&d=07&e=18&f=2005&g=d&ignore=.csv
     */
    protected void request() throws Exception {
        calendar.clear();
        contract = getCurrentContract();
        Date begDate = new Date();
        Date endDate = new Date();
        if (getFromTime() <= ANCIENT_TIME) {
            begDate = contract.getBegDate();
            endDate = contract.getEndDate();
        } else {
            calendar.setTimeInMillis(getFromTime());
            begDate = calendar.getTime();
        }
        calendar.setTime(begDate);
        int a = calendar.get(Calendar.MONTH);
        int b = calendar.get(Calendar.DAY_OF_MONTH);
        int c = calendar.get(Calendar.YEAR);
        calendar.setTime(endDate);
        int d = calendar.get(Calendar.MONTH);
        int e = calendar.get(Calendar.DAY_OF_MONTH);
        int f = calendar.get(Calendar.YEAR);
        StringBuilder urlStr = new StringBuilder(30);
        urlStr.append("http://table.finance.yahoo.com/table.csv").append("?s=");
        urlStr.append(contract.getSymbol());
        urlStr.append("&a=" + a + "&b=" + b + "&c=" + c + "&d=" + d + "&e=" + e + "&f=" + f);
        urlStr.append("&g=d&ignore=.csv");
        URL url = new URL(urlStr.toString());
        System.out.println(url);
        if (url != null) {
            HttpURLConnection conn = (HttpURLConnection) url.openConnection();
            conn.setAllowUserInteraction(true);
            conn.setRequestMethod("GET");
            conn.setInstanceFollowRedirects(true);
            setInputStream(conn.getInputStream());
        }
    }

    /**
     * @return readed time
     */
    protected long read() throws Exception {
        if (getInputStream() == null) {
            return 0;
        }
        BufferedReader reader = new BufferedReader(new InputStreamReader(getInputStream()));
        String s = reader.readLine();
        int iDateTime = 0;
        int iOpen = 1;
        int iHigh = 2;
        int iLow = 3;
        int iClose = 4;
        int iVolume = 5;
        int iAdjClose = 6;
        long newestTime = -Long.MAX_VALUE;
        resetCount();
        calendar.clear();
        while ((s = reader.readLine()) != null) {
            String[] items;
            items = s.split(",");
            if (items.length < 6) {
                break;
            }
            Date date = null;
            try {
                date = dateFormat.parse(items[iDateTime].trim());
            } catch (ParseException ex1) {
                try {
                    date = dateFormat_old.parse(items[iDateTime].trim());
                } catch (ParseException ex2) {
                    continue;
                }
            }
            calendar.clear();
            calendar.setTime(date);
            long time = calendar.getTimeInMillis();
            if (time < getFromTime()) {
                continue;
            }
            Quote quote = borrowQuote();
            quote.setTime(time);
            quote.setOpen(Float.parseFloat(items[iOpen].trim()));
            quote.setHigh(Float.parseFloat(items[iHigh].trim()));
            quote.setLow(Float.parseFloat(items[iLow].trim()));
            quote.setClose(Float.parseFloat(items[iClose].trim()));
            quote.setVolume(Float.parseFloat(items[iVolume].trim()) / 100f);
            quote.setAmount(-1);
            quote.setClose_adj(Float.parseFloat(items[iAdjClose].trim()));
            if (quote.getHigh() * quote.getLow() * quote.getClose() == 0) {
                returnQuote(quote);
                continue;
            }
            getStorage(contract).add(quote);
            newestTime = Math.max(newestTime, time);
            countOne();
        }
        return newestTime;
    }

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

    public String getDisplayName() {
        return "Yahoo! Finance Internet";
    }

    public String getDefaultDateFormatString() {
        return "yyyy-mm-dd";
    }

    public byte getSourceSerialNumber() {
        return (byte) 1;
    }

    @Override
    public Image getIcon() {
        return Utilities.loadImage("org/aiotrade/platform/modules/dataserver/basic/netbeans/resources/favicon_yahoo.png");
    }
}
