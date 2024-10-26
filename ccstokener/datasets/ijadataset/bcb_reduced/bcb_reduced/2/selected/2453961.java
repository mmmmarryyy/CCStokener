package org.mov.quote;

import java.io.*;
import java.net.*;
import java.util.*;
import java.util.prefs.*;
import java.util.regex.*;
import org.mov.util.*;
import org.mov.ui.DesktopManager;
import org.mov.ui.ProgressDialog;
import org.mov.ui.ProgressDialogManager;

/**
 * Provides functionality to obtain stock quotes directly from Sanford's
 * webiste. This class implements the QuoteSource interface to allow users to
 * access quotes directly from the internet without the need for local copies.
 *
 * Example:
 * <pre>
 *	Vector quotes = Quote.getSource().getQuotesForSymbol("CBA");
 * </pre>
 *
 * @see Quote
 */
public class SanfordQuoteSource implements QuoteSource {

    public static final String HOST = "www.sanford.com.au";

    public static final String PROTOCOL = "https";

    private String cookie;

    private static final String FILTER = "Ezy Chart";

    private QuoteFilter filter;

    private String username;

    private String password;

    private HashMap symbolToName = new HashMap();

    private boolean connected = false;

    /**
     * Creates a new quote source by downloading directly from Sanford's
     * web site. 
     *
     * @param	username	Sanford login username
     * @param	password	Sanford login password
     */
    public SanfordQuoteSource(String username, String password) {
        this.username = username;
        this.password = password;
        filter = QuoteFilterList.getInstance().getFilter(FILTER);
        login();
    }

    private void login() {
        ProgressDialog p = ProgressDialogManager.getProgressDialog();
        p.setNote("Logging into Sanford");
        try {
            URL url = new URL(PROTOCOL, HOST, "/sanford/Public/Home/Login.asp");
            URLConnection connection = url.openConnection();
            connection.setDoOutput(true);
            OutputStream ostream = connection.getOutputStream();
            PrintWriter writer = new PrintWriter(ostream);
            writer.print("username=" + username + "&password=" + password);
            writer.close();
            cookie = connection.getHeaderField("Set-Cookie");
            connected = true;
        } catch (java.io.IOException io) {
            DesktopManager.showErrorMessage("Can't connect to Sanford");
        }
        ProgressDialogManager.closeProgressDialog();
    }

    /** 
     * Returns the company name associated with the given symbol. 
     * 
     * @param	symbol	the stock symbol.
     * @return	the company name.
     */
    public String getCompanyName(String symbol) {
        if (!connected) return null;
        String companyName;
        companyName = (String) symbolToName.get(symbol);
        if (companyName == null) {
            ProgressDialog p = ProgressDialogManager.getProgressDialog();
            p.setNote("Retrieving stock name");
            boolean symbolFound = false;
            try {
                URL url = new URL(PROTOCOL, HOST, "/sanford/Members/MarketInfo/MarketWatch.asp");
                URLConnection connection = url.openConnection();
                connection.setDoOutput(true);
                connection.setRequestProperty("Cookie", cookie);
                OutputStream os = connection.getOutputStream();
                PrintWriter writer = new PrintWriter(os);
                writer.print("Code=" + symbol + "&" + "type=Basic");
                writer.close();
                InputStreamReader isr = new InputStreamReader(connection.getInputStream());
                BufferedReader reader = new BufferedReader(isr);
                String line;
                while ((line = reader.readLine()) != null) {
                    System.out.println(line);
                    if (line.indexOf("<p><font face") != -1) {
                        Pattern pat = Pattern.compile("<[^>]*>");
                        Matcher m = pat.matcher(line);
                        companyName = m.replaceAll("");
                        pat = Pattern.compile("  ");
                        m = pat.matcher(companyName);
                        companyName = m.replaceAll("");
                        symbolToName.put(symbol, companyName);
                        break;
                    }
                }
                reader.close();
            } catch (java.io.IOException io) {
                DesktopManager.showErrorMessage("Error talking to Sanford");
            }
            ProgressDialogManager.closeProgressDialog();
        }
        return companyName;
    }

    /**
     * Returns the symbol associated with the given company. 
     * 
     * @param	symbol	a partial company name.
     * @return	the company symbol.
     */
    public String getCompanySymbol(String partialCompanyName) {
        return null;
    }

    /**
     * Returns whether we have any quotes for the given symbol.
     *
     * @param	symbol	the symbol we are searching for.
     * @return	whether the symbol was found or not.
     */
    public boolean symbolExists(String symbol) {
        return (getCompanyName(symbol) == null) ? false : true;
    }

    /**
     * Return the latest date we have any stock quotes for.
     *
     * @return	the most recent quote date.
     */
    public TradingDate getLatestQuoteDate() {
        TradingDate date = new TradingDate();
        date = date.previous(1);
        return date;
    }

    /**
     * Return a vector of quotes for all stocks in the given date range.
     * The vector will be in order of date then stock symbol.
     *
     * @param	startDate	the start of the date range (inclusive).
     * @param	endDate		the end of the date range (inclusive).
     * @param	type		the type of the search.
     * @return	a vector of stock quotes.
     * @see Quote
     */
    public Vector getQuotesForDates(TradingDate startDate, TradingDate endDate, int type) {
        Vector quotes = new Vector();
        if (!connected) return quotes;
        Vector dates = Converter.dateRangeToTradingDateVector(startDate, endDate);
        Iterator iterator = dates.iterator();
        TradingDate date;
        ProgressDialog p = ProgressDialogManager.getProgressDialog();
        p.setTitle("Loading quotes " + startDate.toShortString() + " to " + endDate.toShortString());
        p.setMaximum(dates.size());
        while (iterator.hasNext()) {
            date = (TradingDate) iterator.next();
            quotes.addAll((Collection) getQuotesForDate(date, type));
            p.increment();
        }
        ProgressDialogManager.closeProgressDialog();
        return quotes;
    }

    /**
     * Return all quotes for the given symbols between the given dates. 
     * They will be returned in order of date.
     *
     * @param	symbols	the symbols to query.
     * @param	startDate	the first trading date to query for
     * @param	endDate		the last trading date to query for
     * @return	a vector of stock quotes.
     * @see Quote
     */
    public Vector getQuotesForSymbolsAndDates(Vector symbols, TradingDate startDate, TradingDate endDate) {
        return new Vector();
    }

    /**
     * Return a vector of all quotes in the given date.
     * The vector will be in order of stock symbol.
     *
     * @param	date	the date to return quotes for.
     * @param	type	the type of the search.
     * @return	a vector of stock quotes.
     * @see Quote
     */
    public Vector getQuotesForDate(TradingDate date, int type) {
        Vector quotes = new Vector();
        if (!connected) return quotes;
        ProgressDialog p = ProgressDialogManager.getProgressDialog();
        p.setNote("Loading quotes");
        try {
            Quote quote;
            URL url = new URL(PROTOCOL, HOST, "/sanford/research/HistoricalData.asp");
            URLConnection connection = url.openConnection();
            connection.setDoOutput(true);
            connection.setRequestProperty("Cookie", cookie);
            OutputStream os = connection.getOutputStream();
            PrintWriter writer = new PrintWriter(os);
            writer.print("HistDataDate=" + date.toString("dd/mm/yy"));
            writer.close();
            InputStreamReader isr = new InputStreamReader(connection.getInputStream());
            BufferedReader reader = new BufferedReader(isr);
            String line;
            while ((line = reader.readLine()) != null) {
                quote = filter.toQuote(line);
                if (quote != null && isType(quote, type)) {
                    quotes.add(quote);
                    p.increment();
                }
            }
            reader.close();
        } catch (java.io.IOException io) {
            DesktopManager.showErrorMessage("Error talking to Sanford");
        }
        ProgressDialogManager.closeProgressDialog();
        return quotes;
    }

    /**
     * Return all quotes for the given symbol. They will be returned in
     * order of date.
     *
     * @param	symbol	the symbol to query.
     * @return	a vector of stock quotes.
     * @see Quote
     */
    public Vector getQuotesForSymbol(String symbol) {
        Vector quotes = new Vector();
        if (!connected) return quotes;
        ProgressDialog p = ProgressDialogManager.getProgressDialog();
        p.setNote("Loading quotes for " + symbol);
        try {
            Quote quote;
            URL url = new URL(PROTOCOL, HOST, "/sanford/Members/Research/HistoricalData.asp");
            URLConnection connection = url.openConnection();
            connection.setDoOutput(true);
            connection.setRequestProperty("Cookie", cookie);
            OutputStream os = connection.getOutputStream();
            PrintWriter writer = new PrintWriter(os);
            TradingDate startDate = new TradingDate();
            startDate = startDate.previous(365 * 2);
            writer.print("ASXCode=" + symbol + "&" + "StartDate=" + startDate.toString("dd/mm/yy"));
            writer.close();
            InputStreamReader isr = new InputStreamReader(connection.getInputStream());
            BufferedReader reader = new BufferedReader(isr);
            String line;
            while ((line = reader.readLine()) != null) {
                quote = filter.toQuote(line);
                if (quote != null) {
                    quotes.add(quote);
                    p.increment();
                }
            }
            reader.close();
        } catch (java.io.IOException io) {
            DesktopManager.showErrorMessage("Error talking to Sanford");
        }
        ProgressDialogManager.closeProgressDialog();
        return quotes;
    }

    private boolean isType(Quote quote, int type) {
        boolean match = false;
        if (type == INDICES) {
            if (quote.getSymbol().startsWith("x")) match = true;
        } else if (type == COMPANIES_AND_FUNDS) {
            if (quote.getSymbol().length() == 3 && !quote.getSymbol().startsWith("x")) match = true;
        } else if (type == ALL_COMMODITIES) {
            if (!quote.getSymbol().startsWith("x")) match = true;
        } else match = true;
        return match;
    }

    /** 
     * Return all the dates which we have quotes for.
     *
     * @return	a vector of dates
     */
    public Vector getDates() {
        return null;
    }
}
