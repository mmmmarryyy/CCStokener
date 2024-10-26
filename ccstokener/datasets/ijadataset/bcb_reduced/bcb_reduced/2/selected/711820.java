package test;

import java.io.BufferedInputStream;
import java.io.BufferedReader;
import java.io.FileReader;
import java.io.IOException;
import java.net.MalformedURLException;
import java.net.URL;
import java.net.URLConnection;
import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.Map;
import java.util.TreeMap;
import org.discover.trading.core.IQuote;
import org.discover.trading.core.RegexQuoteTemplate;
import org.discover.trading.core.Security;
import org.discover.trading.io.CSVQuoteWriter;
import org.discover.trading.io.IQuoteReader;
import org.discover.trading.io.IQuoteWriter;
import org.discover.trading.io.TextQuoteReader;

public class TestYahoo {

    private static RegexQuoteTemplate iciciDirectTemplate = null;

    private static long waitTime;

    private static String stockSymbol = "NIFTY";

    private static String url = "http://uk.old.finance.yahoo.com/d/quotes.csv?s=%5EFTSE&f=sl1d1t1c1ohgv&e=.csv";

    static void run() {
        String prevVal = null;
        while (true) {
            BufferedInputStream reader = null;
            try {
                URLConnection connection = new URL(url).openConnection();
                reader = new BufferedInputStream(connection.getInputStream());
                connection.setReadTimeout(2000);
            } catch (MalformedURLException e1) {
                e1.printStackTrace();
            } catch (IOException e1) {
                e1.printStackTrace();
            }
            int c = 0;
            String source = "";
            try {
                while ((c = reader.read()) != -1) source = source + (char) c;
                if (prevVal == null) {
                    prevVal = source;
                    System.out.print(source + Calendar.getInstance().getTime().toString());
                } else if (!prevVal.equals(source)) System.out.print(source + Calendar.getInstance().getTime().toString());
                prevVal = source;
            } catch (IOException e1) {
                e1.printStackTrace();
            } finally {
                try {
                    reader.close();
                } catch (IOException e) {
                    e.printStackTrace();
                }
            }
            try {
                Thread.sleep(5000);
            } catch (InterruptedException e) {
                e.printStackTrace();
            }
        }
    }

    /**
	 * @param args
	 */
    public static void main(String[] args) {
        while (true) {
            run();
        }
    }

    private static boolean isMarketClosed() {
        int hour = Calendar.getInstance().get(Calendar.HOUR_OF_DAY);
        int minute = Calendar.getInstance().get(Calendar.MINUTE);
        if ((hour == 15 && minute >= 35) || hour > 15) return true; else return false;
    }

    private static boolean isMartketOpen() {
        int hour = Calendar.getInstance().get(Calendar.HOUR_OF_DAY);
        int minute = Calendar.getInstance().get(Calendar.MINUTE);
        int day = Calendar.getInstance().get(Calendar.DAY_OF_WEEK);
        if (day == Calendar.SATURDAY || day == Calendar.SUNDAY) {
            waitTime = 8 * 60 * 60 * 1000;
            return false;
        }
        if (hour < 9 || hour > 16) {
            waitTime = 50 * 60 * 1000;
            return false;
        } else if ((hour == 9 && minute >= 50) || hour > 9) return true; else {
            waitTime = 10 * 1000;
            return false;
        }
    }
}
