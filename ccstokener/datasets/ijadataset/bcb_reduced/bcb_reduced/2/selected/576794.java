package org.blogtrader.platform.modules.dataserver.basic;

import java.io.*;
import java.net.URL;
import java.util.Calendar;
import java.util.List;
import org.blogtrader.platform.core.data.dataserver.AbstractQuoteDataServer;
import org.blogtrader.platform.core.analysis.descriptor.QuoteDataSourceDescriptor;
import org.blogtrader.platform.core.stock.Quote;

/** 
 * This class will load the quote datas from data source to its data storage: quotes.
 * @TODO it will be implemented as a Data Server ?
 *
 * @author Caoyuan Deng
 */
public class SlonQuoteDataServer extends AbstractQuoteDataServer {

    private long firstTime;

    private long lastTime;

    /** Retrive data from ShenLong's data file
     *
     */
    protected long loadFromSource(long afterThisTime) {
        QuoteDataSourceDescriptor quoteDataSourceDescriptor = (QuoteDataSourceDescriptor) dataSourceDescriptor;
        List<Quote> dataPool = dataPools.get(quoteDataSourceDescriptor.sourceSymbol);
        Calendar calendar = Calendar.getInstance();
        calendar.clear();
        DataInputStream dis;
        boolean EOF = false;
        try {
            URL url = new URL("file:data/Slon/data/sh/day/" + quoteDataSourceDescriptor.sourceSymbol + ".day");
            dis = new DataInputStream(url.openStream());
            count = 0;
            calendar.clear();
            while (!EOF) {
                int iDate = toProperFormat(dis.readInt());
                int year = iDate / 10000;
                int month = (iDate - year * 10000) / 100;
                int day = iDate - year * 10000 - month * 100;
                calendar.clear();
                calendar.set(year, month - 1, day);
                long time = calendar.getTimeInMillis();
                if (time <= afterThisTime) {
                    continue;
                }
                Quote quote = new Quote();
                quote.time = time;
                quote.open = toProperFormat(dis.readInt()) / 1000f;
                quote.close = toProperFormat(dis.readInt()) / 1000f;
                quote.high = toProperFormat(dis.readInt()) / 1000f;
                quote.low = toProperFormat(dis.readInt()) / 1000f;
                quote.amount = toProperFormat(dis.readInt());
                quote.volume = toProperFormat(dis.readInt());
                dis.skipBytes(12);
                dataPool.add(quote);
                if (count == 0) {
                    firstTime = time;
                }
                lastTime = time;
                setAscending((lastTime >= firstTime) ? true : false);
                count++;
            }
        } catch (EOFException e) {
            EOF = true;
        } catch (IOException e) {
            System.out.println("Error in Reading File");
        }
        long newestTime = (lastTime >= firstTime) ? lastTime : firstTime;
        return newestTime;
    }

    /** Convert data from ShenLong data file to proper format.
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

    public String getName() {
        return "Slon Data File";
    }

    public String getDefaultDateFormat() {
        return "yyyyMMdd hhmmss";
    }
}
