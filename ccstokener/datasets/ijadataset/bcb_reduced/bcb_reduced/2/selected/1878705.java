package org.aiotrade.platform.modules.dataserver.basic;

import java.awt.Image;
import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.URL;
import java.text.ParseException;
import java.util.Calendar;
import java.util.Date;
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
public class NetfondsSEQuoteServer extends QuoteServer {

    private Calendar cal = Calendar.getInstance();

    private QuoteContract contract;

    protected boolean connect() {
        return true;
    }

    protected void request() throws Exception {
        cal.clear();
        contract = getCurrentContract();
        Date begDate = new Date();
        Date endDate = new Date();
        if (getFromTime() <= ANCIENT_TIME) {
            begDate = contract.getBegDate();
            endDate = contract.getEndDate();
        } else {
            cal.setTimeInMillis(getFromTime());
            begDate = cal.getTime();
        }
        cal.setTime(begDate);
        int a = cal.get(Calendar.MONTH);
        int b = cal.get(Calendar.DAY_OF_MONTH);
        int c = cal.get(Calendar.YEAR);
        cal.setTime(endDate);
        int d = cal.get(Calendar.MONTH);
        int e = cal.get(Calendar.DAY_OF_MONTH);
        int f = cal.get(Calendar.YEAR);
        StringBuffer urlStr = new StringBuffer(60);
        urlStr.append("http://www.netfonds.se/quotes/paperhistory.php").append("?paper=");
        urlStr.append(contract.getSymbol());
        URL url = new URL(urlStr.toString());
        System.out.println(url);
        HttpURLConnection conn = (HttpURLConnection) url.openConnection();
        conn.setAllowUserInteraction(true);
        conn.setRequestMethod("GET");
        conn.setInstanceFollowRedirects(true);
        setInputStream(conn.getInputStream());
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
        int iOpen = 3;
        int iHigh = 4;
        int iLow = 5;
        int iClose = 6;
        int iVolume = 7;
        int iAmount = 8;
        long newestTime = -Long.MAX_VALUE;
        resetCount();
        cal.clear();
        while ((s = reader.readLine()) != null) {
            String[] items;
            items = s.split("\t");
            if (items.length < 6) {
                break;
            }
            try {
                Date date = getDateFormat().parse(items[iDateTime].trim());
                cal.clear();
                cal.setTime(date);
            } catch (ParseException ex) {
                ex.printStackTrace();
                continue;
            }
            long time = cal.getTimeInMillis();
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
            quote.setAmount(Float.parseFloat(items[iAmount].trim()) / 100f);
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
        return "Netfonds.se Internet";
    }

    public String getDefaultDateFormatString() {
        return "yyyyMMdd";
    }

    public byte getSourceSerialNumber() {
        return (byte) 4;
    }

    @Override
    public Image getIcon() {
        return Utilities.loadImage("org/aiotrade/platform/modules/dataserver/basic/netbeans/resources/favicon_netfondsSE.png");
    }
}
