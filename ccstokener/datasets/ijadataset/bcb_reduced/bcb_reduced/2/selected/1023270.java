package org.aiotrade.platform.modules.dataserver.basic;

import java.io.DataInputStream;
import java.io.EOFException;
import java.net.URL;
import java.util.Calendar;
import org.aiotrade.math.timeseries.Frequency;
import org.aiotrade.platform.core.dataserver.QuoteContract;
import org.aiotrade.platform.core.dataserver.QuoteServer;
import org.aiotrade.platform.core.sec.Quote;

/**
 * This class will load the quote datas from data source to its data storage: quotes.
 * @TODO it will be implemented as a Data Server ?
 *
 * @author Caoyuan Deng
 */
public class SlonQuoteServer extends QuoteServer {

    private Calendar cal = Calendar.getInstance();

    private QuoteContract contract;

    protected boolean connect() {
        return true;
    }

    protected void request() throws Exception {
        cal.clear();
        contract = getCurrentContract();
        URL url = new URL(contract.getUrlString() + contract.getSymbol() + ".day");
        setInputStream(url.openStream());
    }

    protected long read() throws Exception {
        if (getInputStream() == null) {
            return 0;
        }
        DataInputStream reader = new DataInputStream(getInputStream());
        long newestTime = -Long.MAX_VALUE;
        resetCount();
        cal.clear();
        boolean EOF = false;
        try {
            while (!EOF) {
                int iDate = toProperFormat(reader.readInt());
                int year = iDate / 10000;
                int month = (iDate - year * 10000) / 100;
                int day = iDate - year * 10000 - month * 100;
                cal.clear();
                cal.set(year, month - 1, day);
                long time = cal.getTimeInMillis();
                if (time < getFromTime()) {
                    continue;
                }
                Quote quote = borrowQuote();
                quote.setTime(time);
                quote.setOpen(toProperFormat(reader.readInt()) / 1000f);
                quote.setClose(toProperFormat(reader.readInt()) / 1000f);
                quote.setHigh(toProperFormat(reader.readInt()) / 1000f);
                quote.setLow(toProperFormat(reader.readInt()) / 1000f);
                quote.setAmount(toProperFormat(reader.readInt()));
                quote.setVolume(toProperFormat(reader.readInt()));
                reader.skipBytes(12);
                getStorage(contract).add(quote);
                newestTime = Math.max(newestTime, time);
                countOne();
            }
        } catch (EOFException e) {
            EOF = true;
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

    /**
     * Convert data from ShenLong data file to proper format.
     * @param java int type length data
     */
    private int toProperFormat(int a) {
        int c = 0x00000000;
        c |= 0x00FF0000 & (a << 8);
        c |= 0xFF000000 & (a << 24);
        c |= 0x0000FF00 & (a >> 8);
        c |= 0x000000FF & (a >> 24);
        return ~c;
    }

    public String getDisplayName() {
        return "Slon Data File";
    }

    public String getDefaultDateFormatString() {
        return "yyyyMMdd hhmmss";
    }

    public byte getSourceSerialNumber() {
        return (byte) 5;
    }

    @Override
    public Frequency[] getSupportedFreqs() {
        return null;
    }
}
