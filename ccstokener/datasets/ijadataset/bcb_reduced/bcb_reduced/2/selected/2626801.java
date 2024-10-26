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
public class CsvQuoteServer extends QuoteServer {

    private Calendar cal = Calendar.getInstance();

    private QuoteContract contract;

    protected boolean connect() {
        return true;
    }

    protected void request() throws Exception {
        cal.clear();
        contract = getCurrentContract();
        if (contract.getInputStream() == null) {
            URL url = new URL(contract.getUrlString());
            setInputStream(url.openStream());
        } else {
            setInputStream(contract.getInputStream());
        }
    }

    protected long read() throws Exception {
        if (getInputStream() == null) {
            return 0;
        }
        BufferedReader reader = new BufferedReader(new InputStreamReader(getInputStream()));
        String s = reader.readLine();
        resetCount();
        cal.clear();
        long newestTime = -Long.MAX_VALUE;
        while ((s = reader.readLine()) != null) {
            StringTokenizer t = new StringTokenizer(s, ",");
            if (t.countTokens() >= 5) {
                try {
                    Date date = getDateFormat().parse(t.nextToken().trim());
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
                quote.setOpen(Float.parseFloat(t.nextToken().trim()));
                quote.setHigh(Float.parseFloat(t.nextToken().trim()));
                quote.setLow(Float.parseFloat(t.nextToken().trim()));
                quote.setClose(Float.parseFloat(t.nextToken().trim()));
                quote.setVolume(Float.parseFloat(t.nextToken().trim()) / 100f);
                quote.setAmount(-1);
                getStorage(contract).add(quote);
                newestTime = Math.max(newestTime, time);
                countOne();
            }
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
        return "CSV ASCII File";
    }

    public String getDefaultDateFormatString() {
        return "dd-MMM-yy";
    }

    @Override
    public Frequency[] getSupportedFreqs() {
        return null;
    }

    public byte getSourceSerialNumber() {
        return (byte) 2;
    }

    @Override
    public Image getIcon() {
        return Utilities.loadImage("org/aiotrade/platform/modules/dataserver/basic/netbeans/resources/favicon_csv.png");
    }
}
