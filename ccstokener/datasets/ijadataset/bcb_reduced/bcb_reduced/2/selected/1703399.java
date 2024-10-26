package org.aiotrade.platform.modules.dataserver.basic;

import java.awt.Image;
import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.net.URL;
import java.text.ParseException;
import java.util.Calendar;
import java.util.Date;
import java.util.StringTokenizer;
import org.aiotrade.math.timeseries.Frequency;
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
public class MetaStockAsciiQuoteServer extends QuoteServer {

    private Calendar cal = Calendar.getInstance();

    private QuoteContract contract;

    protected boolean connect() {
        return true;
    }

    protected void request() throws Exception {
        cal.clear();
        contract = getCurrentContract();
        URL url = new URL(contract.getUrlString());
        setInputStream(url.openStream());
    }

    protected long read() throws Exception {
        if (getInputStream() == null) {
            return 0;
        }
        BufferedReader reader = new BufferedReader(new InputStreamReader(getInputStream()));
        int iCode = 0;
        int iDate = 0;
        int iTime = 0;
        int iOpen = 0;
        int iHigh = 0;
        int iLow = 0;
        int iClose = 0;
        int iVolume = 0;
        String s = reader.readLine();
        StringTokenizer tTilte = new StringTokenizer(s, ",");
        String token = null;
        int tokenCount = tTilte.countTokens();
        for (int i = 0; i < tokenCount; i++) {
            token = tTilte.nextToken().trim().toUpperCase();
            if (token.contains("TICKER")) {
                iCode = i;
            } else if (token.contains("YYMMDD")) {
                iDate = i;
            } else if (token.contains("TIME")) {
                iTime = i;
            } else if (token.contains("OPEN")) {
                iOpen = i;
            } else if (token.contains("HIGH")) {
                iHigh = i;
            } else if (token.contains("LOW")) {
                iLow = i;
            } else if (token.contains("CLOSE")) {
                iClose = i;
            } else if (token.contains("VOLUME")) {
                iVolume = i;
            }
        }
        long newestTime = -Long.MAX_VALUE;
        resetCount();
        cal.clear();
        while ((s = reader.readLine()) != null) {
            String[] items;
            items = s.split(",");
            if (items.length < 5 || contract.getSymbol().contains(items[iCode]) == false) {
                break;
            }
            String dateStr = items[iDate] + " " + items[iTime];
            try {
                Date date = getDateFormat().parse(dateStr.trim());
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
            float open = Float.parseFloat(items[iOpen].trim());
            float high = Float.parseFloat(items[iHigh].trim());
            float low = Float.parseFloat(items[iLow].trim());
            float close = Float.parseFloat(items[iClose].trim());
            float volume = Float.parseFloat(items[iVolume].trim()) / 100f;
            Quote quote = borrowQuote();
            quote.setTime(time);
            quote.setOpen(open);
            quote.setHigh(high);
            quote.setLow(low);
            quote.setClose(close);
            quote.setVolume(volume);
            quote.setAmount(-1);
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
        return "MetaStock ASCII File";
    }

    public String getDefaultDateFormatString() {
        return "yyyyMMdd HHmmss";
    }

    public byte getSourceSerialNumber() {
        return (byte) 3;
    }

    @Override
    public Frequency[] getSupportedFreqs() {
        return null;
    }

    @Override
    public Image getIcon() {
        return Utilities.loadImage("org/aiotrade/platform/modules/dataserver/basic/netbeans/resources/favicon_metastock.png");
    }
}
