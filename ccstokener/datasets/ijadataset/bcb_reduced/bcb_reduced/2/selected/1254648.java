package org.mov.quote;

import java.io.BufferedReader;
import java.io.FileNotFoundException;
import java.io.InputStreamReader;
import java.io.IOException;
import java.lang.reflect.Constructor;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.net.NoRouteToHostException;
import java.net.MalformedURLException;
import java.net.BindException;
import java.net.ConnectException;
import java.net.UnknownHostException;
import java.net.URL;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.Vector;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import javax.swing.LookAndFeel;
import javax.swing.UIManager;
import org.mov.ui.DesktopManager;
import org.mov.ui.ProgressDialog;
import org.mov.ui.ProgressDialogManager;
import org.mov.prefs.PreferencesManager;
import org.mov.util.Locale;
import org.mov.util.TradingDate;

/**
 * Provides functionality to obtain stock quotes from the internet. The entire
 * quote sourcce interface has not been implemented so this source is only
 * suitable for importing the quotes, rather than accessing them directly.
 */
public class InternetQuoteSource implements QuoteSource {

    private static final String SYMBOL = "_SYM_";

    private static final String START_DAY = "_SD_";

    private static final String START_MONTH = "_SM_";

    private static final String START_YEAR = "_SY_";

    private static final String END_DAY = "_ED_";

    private static final String END_MONTH = "_EM_";

    private static final String END_YEAR = "_EY_";

    private static final String YAHOO_FORMAT = ("?s=" + SYMBOL + "&a=" + START_MONTH + "&b=" + START_DAY + "&c=" + START_YEAR + "&d=" + END_MONTH + "&e=" + END_DAY + "&f=" + END_YEAR + "&g=d&ignore=.csv");

    private static final String YAHOOTODAY_FORMAT = ("?s=" + SYMBOL + "&f=sl1d1t1c1ohgv&e=.csv");

    private static final String[] sources = { "Yahoo", "http://ichart.finance.yahoo.com/table.csv" + YAHOO_FORMAT, "YahooToday", "http://finance.yahoo.com/d/quotes.csv" + YAHOOTODAY_FORMAT };

    private static final int numberExchanges = (sources.length / 2);

    private String name;

    private String URLPattern;

    private TradingDate startDate;

    private TradingDate endDate;

    /**
     * Create a new quote source from the given exchange between the given dates.
     *
     * @param exchange the exchange.
     * @param startDate the start date.
     * @param endDate the end date.
     */
    public InternetQuoteSource(int exchange, TradingDate startDate, TradingDate endDate) {
        assert exchange < numberExchanges;
        name = sources[exchange * 2];
        URLPattern = sources[exchange * 2 + 1];
        this.startDate = startDate;
        this.endDate = endDate;
    }

    /**
     * Return a list of all the stock exchanges we support.
     *
     * @return array of stock exchanges
     */
    public static Object[] getExchanges() {
        Object[] exchanges = new Object[numberExchanges];
        for (int i = 0; i < numberExchanges; i++) exchanges[i] = sources[i * 2];
        return exchanges;
    }

    /**
     * Returns the company name associated with the given symbol.
     *
     * @param	symbol	the stock symbol
     * @return	the company name
     */
    public String getSymbolName(Symbol symbol) {
        throw new UnsupportedOperationException();
    }

    /**
     * Returns the symbol associated with the given company.
     *
     * @param	partialCompanyName	a partial company name
     * @return	the company symbol
     */
    public Symbol getSymbol(String partialCompanyName) {
        throw new UnsupportedOperationException();
    }

    /**
     * Returns whether we have any quotes for the given symbol.
     *
     * @param	symbol	the symbol we are searching for
     * @return	whether the symbol was found or not
     */
    public boolean symbolExists(Symbol symbol) {
        throw new UnsupportedOperationException();
    }

    /**
     * Return the latest date we have any stock quotes for.
     *
     * @return	the most recent quote date
     */
    public TradingDate getLastDate() {
        return endDate;
    }

    /**
     * Return the earliest date we have any stock quotes for.
     *
     * @return	the oldest quote date
     */
    public TradingDate getFirstDate() {
        return startDate;
    }

    /**
     * Load the given quote range into the quote cache.
     *
     * @param	quoteRange	the range of quotes to load
     * @return  <code>TRUE</code> if the operation suceeded
     * @see Quote
     * @see QuoteCache
     */
    public boolean loadQuoteRange(QuoteRange quoteRange) {
        if (quoteRange.getType() != QuoteRange.GIVEN_SYMBOLS || quoteRange.getFirstDate() == null || quoteRange.getLastDate() == null) throw new UnsupportedOperationException(); else {
            List symbols = quoteRange.getAllSymbols();
            boolean success = true;
            QuoteCache quoteCache = QuoteCache.getInstance();
            Thread thread = Thread.currentThread();
            ProgressDialog progress = ProgressDialogManager.getProgressDialog();
            progress.setNote(Locale.getString("DOWNLOADING_QUOTES"));
            if (symbols.size() > 1) {
                progress.setMaximum(symbols.size());
                progress.setProgress(0);
                progress.setIndeterminate(false);
            } else progress.setIndeterminate(true);
            for (Iterator iterator = symbols.iterator(); iterator.hasNext(); ) {
                Symbol symbol = (Symbol) iterator.next();
                if (!loadSymbol(quoteCache, symbol, quoteRange.getFirstDate(), quoteRange.getLastDate()) || thread.isInterrupted()) {
                    success = false;
                    break;
                }
                progress.increment();
            }
            ProgressDialogManager.closeProgressDialog(progress);
            return success;
        }
    }

    /**
     * Returns whether the source contains any quotes for the given date.
     *
     * @param date the date
     * @return wehther the source contains the given date
     */
    public boolean containsDate(TradingDate date) {
        return (date.compareTo(startDate) >= 0 && date.compareTo(endDate) <= 0);
    }

    /**
     * Return all the dates which we have quotes for.
     *
     * @return	a vector of dates
     */
    public List getDates() {
        return TradingDate.dateRangeToList(startDate, endDate);
    }

    /**
     * Is the given symbol a market index?
     *
     * @param	symbol to test
     * @return	yes or no
     */
    public boolean isMarketIndex(Symbol symbol) {
        throw new UnsupportedOperationException();
    }

    /**
     * Return the advance/decline for the given date. This returns the number
     * of all ordinary stocks that rose (day close > day open) - the number of all
     * ordinary stocks that fell.
     *
     * @param date the date
     * @exception throws MissingQuoteException if the date wasn't in the source
     */
    public int getAdvanceDecline(TradingDate date) throws MissingQuoteException {
        throw new UnsupportedOperationException();
    }

    private boolean loadSymbol(QuoteCache quoteCache, Symbol symbol, TradingDate startDate, TradingDate endDate) {
        boolean success = true;
        String URLString = constructURL(symbol, startDate, endDate);
        PreferencesManager.ProxyPreferences proxyPreferences = PreferencesManager.loadProxySettings();
        try {
            URL url;
            url = new URL(URLString);
            InputStreamReader input = new InputStreamReader(url.openStream());
            BufferedReader bufferedInput = new BufferedReader(input);
            String line;
            while ((line = bufferedInput.readLine()) != null) {
                Class cl = null;
                Constructor cnst = null;
                QuoteFilter filter = null;
                try {
                    cl = Class.forName("org.mov.quote." + name + "QuoteFilter");
                    try {
                        cnst = cl.getConstructor(new Class[] { Symbol.class });
                    } catch (SecurityException e2) {
                        e2.printStackTrace();
                    } catch (NoSuchMethodException e2) {
                        e2.printStackTrace();
                    }
                    try {
                        filter = (QuoteFilter) cnst.newInstance(new Object[] { symbol });
                    } catch (IllegalArgumentException e3) {
                        e3.printStackTrace();
                    } catch (InstantiationException e3) {
                        e3.printStackTrace();
                    } catch (IllegalAccessException e3) {
                        e3.printStackTrace();
                    } catch (InvocationTargetException e3) {
                        e3.printStackTrace();
                    }
                } catch (ClassNotFoundException e1) {
                    e1.printStackTrace();
                }
                Quote quote = filter.toQuote(line);
                if (quote != null) quoteCache.load(quote);
            }
            bufferedInput.close();
        } catch (BindException e) {
            DesktopManager.showErrorMessage(Locale.getString("UNABLE_TO_CONNECT_ERROR", e.getMessage()));
            success = false;
        } catch (ConnectException e) {
            DesktopManager.showErrorMessage(Locale.getString("UNABLE_TO_CONNECT_ERROR", e.getMessage()));
            success = false;
        } catch (UnknownHostException e) {
            DesktopManager.showErrorMessage(Locale.getString("UNKNOWN_HOST_ERROR", e.getMessage()));
            success = false;
        } catch (NoRouteToHostException e) {
            DesktopManager.showErrorMessage(Locale.getString("DESTINATION_UNREACHABLE_ERROR", e.getMessage()));
            success = false;
        } catch (MalformedURLException e) {
            DesktopManager.showErrorMessage(Locale.getString("INVALID_PROXY_ERROR", proxyPreferences.host, proxyPreferences.port));
            success = false;
        } catch (FileNotFoundException e) {
        } catch (IOException e) {
            DesktopManager.showErrorMessage(Locale.getString("ERROR_DOWNLOADING_QUOTES"));
            success = false;
        }
        return success;
    }

    private String constructURL(Symbol symbol, TradingDate start, TradingDate end) {
        String URLString = URLPattern;
        URLString = replace(URLString, SYMBOL, symbol.toString());
        URLString = replace(URLString, START_DAY, Integer.toString(start.getDay()));
        URLString = replace(URLString, START_MONTH, Integer.toString(start.getMonth() - 1));
        URLString = replace(URLString, START_YEAR, Integer.toString(start.getYear()));
        URLString = replace(URLString, END_DAY, Integer.toString(end.getDay()));
        URLString = replace(URLString, END_MONTH, Integer.toString(end.getMonth() - 1));
        URLString = replace(URLString, END_YEAR, Integer.toString(end.getYear()));
        return URLString;
    }

    private String replace(String string, String oldSubString, String newSubString) {
        Pattern pattern = Pattern.compile(oldSubString);
        Matcher matcher = pattern.matcher(string);
        return matcher.replaceAll(newSubString);
    }
}
