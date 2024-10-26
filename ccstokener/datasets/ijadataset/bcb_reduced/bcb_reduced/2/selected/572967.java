package org.mov.quote;

import java.io.BufferedReader;
import java.io.FileNotFoundException;
import java.io.InputStreamReader;
import java.io.IOException;
import java.net.NoRouteToHostException;
import java.net.MalformedURLException;
import java.net.BindException;
import java.net.ConnectException;
import java.net.UnknownHostException;
import java.net.URL;
import java.util.Iterator;
import java.util.ArrayList;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import org.mov.prefs.PreferencesManager;
import org.mov.util.Locale;
import org.mov.util.Report;
import org.mov.util.TradingDate;

/**
 * Import end-of-day quotes from Yahoo into Venice.
 *
 * @author Andrew Leppard
 * @see FileEODQuoteImport
 * @see ImportQuoteModule
 */
public class YahooEODQuoteImport {

    private static final String SYMBOL = "_SYM_";

    private static final String START_DAY = "_SD_";

    private static final String START_MONTH = "_SM_";

    private static final String START_YEAR = "_SY_";

    private static final String END_DAY = "_ED_";

    private static final String END_MONTH = "_EM_";

    private static final String END_YEAR = "_EY_";

    private static final int MAX_NUMBER_OF_RETRIEVAL_DAYS = 100;

    private static final String YAHOO_PATTERN = ("?s=" + SYMBOL + "&a=" + START_MONTH + "&b=" + START_DAY + "&c=" + START_YEAR + "&d=" + END_MONTH + "&e=" + END_DAY + "&f=" + END_YEAR + "&g=d&ignore=.csv");

    private static final String YAHOO_URL_PATTERN = ("http://ichart.finance.yahoo.com/table.csv" + YAHOO_PATTERN);

    private YahooEODQuoteImport() {
        assert false;
    }

    /**
     * Retrieve quotes from Yahoo. Will fire multiple request
     * if the specified period is above the maximum number of
     * quotes yahoo supports.
     *
     * @param report report to log warnings and errors
     * @param symbol symbol to import
     * @param startDate start of date range to import
     * @param endDate end of date range to import
     * @return list of quotes
     * @exception ImportExportException if there was an error retrieving the quotes
     */
    public static List importSymbol(Report report, Symbol symbol, TradingDate startDate, TradingDate endDate) throws ImportExportException {
        List result = new ArrayList();
        TradingDate retrievalStartDate;
        TradingDate retrievalEndDate = endDate;
        do {
            retrievalStartDate = retrievalEndDate.previous(MAX_NUMBER_OF_RETRIEVAL_DAYS);
            if (retrievalStartDate.before(startDate)) {
                retrievalStartDate = startDate;
            }
            List quotes = retrieveQuotes(report, symbol, retrievalStartDate, retrievalEndDate);
            result.addAll(quotes);
            retrievalEndDate = retrievalStartDate.previous(1);
        } while (!retrievalEndDate.before(startDate));
        if (result.size() == 0) {
            report.addError(Locale.getString("YAHOO") + ":" + symbol + ":" + Locale.getString("ERROR") + ": " + Locale.getString("NO_QUOTES_FOUND"));
        }
        return result;
    }

    /**
     * Retrieve quotes from Yahoo. 
     * Do not exceed the specified MAX_NUMBER_OF_RETRIEVAL_DAYS!
     *
     * @param report report to log warnings and errors
     * @param symbol symbol to import
     * @param startDate start of date range to import
     * @param endDate end of date range to import
     * @return list of quotes
     * @exception ImportExportException if there was an error retrieving the quotes
     */
    private static List retrieveQuotes(Report report, Symbol symbol, TradingDate startDate, TradingDate endDate) throws ImportExportException {
        List quotes = new ArrayList();
        String URLString = constructURL(symbol, startDate, endDate);
        EODQuoteFilter filter = new YahooEODQuoteFilter(symbol);
        PreferencesManager.ProxyPreferences proxyPreferences = PreferencesManager.loadProxySettings();
        try {
            URL url = new URL(URLString);
            InputStreamReader input = new InputStreamReader(url.openStream());
            BufferedReader bufferedInput = new BufferedReader(input);
            String line = bufferedInput.readLine();
            while (line != null) {
                line = bufferedInput.readLine();
                if (line != null) {
                    try {
                        EODQuote quote = filter.toEODQuote(line);
                        quotes.add(quote);
                        verify(report, quote);
                    } catch (QuoteFormatException e) {
                        report.addError(Locale.getString("YAHOO") + ":" + symbol + ":" + Locale.getString("ERROR") + ": " + e.getMessage());
                    }
                }
            }
            bufferedInput.close();
        } catch (BindException e) {
            throw new ImportExportException(Locale.getString("UNABLE_TO_CONNECT_ERROR", e.getMessage()));
        } catch (ConnectException e) {
            throw new ImportExportException(Locale.getString("UNABLE_TO_CONNECT_ERROR", e.getMessage()));
        } catch (UnknownHostException e) {
            throw new ImportExportException(Locale.getString("UNKNOWN_HOST_ERROR", e.getMessage()));
        } catch (NoRouteToHostException e) {
            throw new ImportExportException(Locale.getString("DESTINATION_UNREACHABLE_ERROR", e.getMessage()));
        } catch (MalformedURLException e) {
            throw new ImportExportException(Locale.getString("INVALID_PROXY_ERROR", proxyPreferences.host, proxyPreferences.port));
        } catch (FileNotFoundException e) {
        } catch (IOException e) {
            throw new ImportExportException(Locale.getString("ERROR_DOWNLOADING_QUOTES"));
        }
        return quotes;
    }

    /**
     * Construct the URL necessary to retrieve all the quotes for the given symbol between
     * the given dates from Yahoo.
     *
     * @param symbol the symbol to retrieve
     * @param start the start date to retrieve
     * @param end the end date to retrieve
     * @return URL string
     */
    private static String constructURL(Symbol symbol, TradingDate start, TradingDate end) {
        String URLString = YAHOO_URL_PATTERN;
        URLString = replace(URLString, SYMBOL, symbol.toString());
        URLString = replace(URLString, START_DAY, Integer.toString(start.getDay()));
        URLString = replace(URLString, START_MONTH, Integer.toString(start.getMonth() - 1));
        URLString = replace(URLString, START_YEAR, Integer.toString(start.getYear()));
        URLString = replace(URLString, END_DAY, Integer.toString(end.getDay()));
        URLString = replace(URLString, END_MONTH, Integer.toString(end.getMonth() - 1));
        URLString = replace(URLString, END_YEAR, Integer.toString(end.getYear()));
        return URLString;
    }

    /**
     * Perform a find replace on a string.
     *
     * @param string the source string
     * @param oldSubString the text which to replace
     * @param newSubString the text to replace with
     * @return the new string
     */
    private static String replace(String string, String oldSubString, String newSubString) {
        Pattern pattern = Pattern.compile(oldSubString);
        Matcher matcher = pattern.matcher(string);
        return matcher.replaceAll(newSubString);
    }

    /**
     * Verify the quote is valid. Log any problems to the report and try to clean
     * it up the best we can.
     *
     * @param report the report
     * @param quote the quote
     */
    private static void verify(Report report, EODQuote quote) {
        try {
            quote.verify();
        } catch (QuoteFormatException e) {
            List messages = e.getMessages();
            for (Iterator iterator = messages.iterator(); iterator.hasNext(); ) {
                String message = (String) iterator.next();
                report.addWarning(Locale.getString("YAHOO") + ":" + quote.getSymbol() + ":" + quote.getDate() + ":" + Locale.getString("WARNING") + ": " + message);
            }
        }
    }
}
