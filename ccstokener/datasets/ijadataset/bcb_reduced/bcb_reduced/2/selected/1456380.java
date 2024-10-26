package jgnash.net.currency;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.math.BigDecimal;
import java.net.URL;
import java.net.URLConnection;
import java.util.logging.Logger;

/**
 * A CurrencyParser for the Yahoo finance site.
 * 
 * @author Craig Cavanaugh
 * 
 * $Id: YahooParser.java 675 2008-06-17 01:36:01Z ccavanaugh $
 */
public class YahooParser implements CurrencyParser {

    private BigDecimal result = null;

    private URLConnection connection = null;

    public synchronized void parse(String source, String target) {
        String label = source + target;
        StringBuffer url = new StringBuffer("http://finance.yahoo.com/d/quotes.csv?s=");
        url.append(label);
        url.append("=X&f=sl1d1t1ba&e=.csv");
        try {
            connection = new URL(url.toString()).openConnection();
            BufferedReader in = new BufferedReader(new InputStreamReader(connection.getInputStream()));
            String l = in.readLine();
            String[] fields = l.split(",");
            if (!"\"N/A\"".equals(fields[2])) {
                result = new BigDecimal(fields[1]);
            }
            connection = null;
        } catch (Exception e) {
            Logger.getAnonymousLogger().severe(e.toString());
        }
    }

    public synchronized URLConnection getConnection() {
        return connection;
    }

    public synchronized BigDecimal getConversion() {
        return result;
    }
}
