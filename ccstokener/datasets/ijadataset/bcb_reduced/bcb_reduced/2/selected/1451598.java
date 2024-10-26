package nz.org.venice.quote;

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
import nz.org.venice.prefs.PreferencesManager;
import nz.org.venice.util.Find;
import nz.org.venice.util.Locale;
import nz.org.venice.util.Report;
import nz.org.venice.util.TradingDate;

/**
 * Import end-of-day quotes from finance.google.com into Venice.
 *
 * @author Andrew Leppard
 * @see FileEODQuoteImport
 * @see ImportQuoteModule
 */
public class GoogleEODQuoteImport {

    private static final String SYMBOL = "_SYM_";

    private static final String START_DAY = "_SD_";

    private static final String START_MONTH = "_SM_";

    private static final String START_YEAR = "_SY_";

    private static final String END_DAY = "_ED_";

    private static final String END_MONTH = "_EM_";

    private static final String END_YEAR = "_EY_";

    private static final int MAX_NUMBER_OF_RETRIEVAL_DAYS = 100;

    private static final String GOOGLE_PATTERN = ("?q=" + SYMBOL + "&startdate=" + START_MONTH + "+" + START_DAY + "%2C+" + START_YEAR + "&enddate=" + END_MONTH + "+" + END_DAY + "%2C+" + END_YEAR + "&output=csv");

    private static final String GOOGLE_URL_PATTERN = ("http://finance.google.com/finance/historical" + GOOGLE_PATTERN);

    private GoogleEODQuoteImport() {
        assert false;
    }

    /**
     * Retrieve quotes from Google. Will fire multiple request
     * if the specified period is above the maximum number of
     * quotes google supports.
     *
     * @param report report to log warnings and errors
     * @param symbol symbol to import
     * @param prefix optional prefix to prepend (e.g. "ASX:"). This prefix tells
     *               Google which exchange the symbol belongs to.
     * @param startDate start of date range to import
     * @param endDate end of date range to import
     * @return list of quotes
     * @exception ImportExportException if there was an error retrieving the quotes
     */
    public static List importSymbol(Report report, Symbol symbol, String prefix, TradingDate startDate, TradingDate endDate) throws ImportExportException {
        List result = new ArrayList();
        TradingDate retrievalStartDate;
        TradingDate retrievalEndDate = endDate;
        do {
            retrievalStartDate = retrievalEndDate.previous(MAX_NUMBER_OF_RETRIEVAL_DAYS);
            if (retrievalStartDate.before(startDate)) {
                retrievalStartDate = startDate;
            }
            List quotes = retrieveQuotes(report, symbol, prefix, retrievalStartDate, retrievalEndDate);
            result.addAll(quotes);
            retrievalEndDate = retrievalStartDate.previous(1);
        } while (!retrievalEndDate.before(startDate));
        if (result.size() == 0) {
            report.addError(Locale.getString("GOOGLE_DISPLAY_URL") + ":" + symbol + ":" + Locale.getString("ERROR") + ": " + Locale.getString("NO_QUOTES_FOUND"));
        }
        return result;
    }

    /**
     * Retrieve quotes from Google.
     * Do not exceed the specified MAX_NUMBER_OF_RETRIEVAL_DAYS!
     *
     * @param report report to log warnings and errors
     * @param symbol symbol to import
     * @param prefix optional prefix to prepend (e.g. "ASX:"). This prefix tells
     *               Google which exchange the symbol belongs to.
     * @param startDate start of date range to import
     * @param endDate end of date range to import
     * @return list of quotes
     * @exception ImportExportException if there was an error retrieving the quotes
     */
    private static List retrieveQuotes(Report report, Symbol symbol, String prefix, TradingDate startDate, TradingDate endDate) throws ImportExportException {
        List quotes = new ArrayList();
        String URLString = constructURL(symbol, prefix, startDate, endDate);
        EODQuoteFilter filter = new GoogleEODQuoteFilter(symbol);
        PreferencesManager.ProxyPreferences proxyPreferences = PreferencesManager.getProxySettings();
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
                        report.addError(Locale.getString("GOOGLE_DISPLAY_URL") + ":" + symbol + ":" + Locale.getString("ERROR") + ": " + e.getMessage());
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
     * the given dates from Google.
     *
     * @param symbol the symbol to retrieve
     * @param prefix optional prefix to prepend (e.g. "ASX:"). This prefix tells
     *               Google which exchange the symbol belongs to.
     * @param start the start date to retrieve
     * @param end the end date to retrieve
     * @return URL string
     */
    private static String constructURL(Symbol symbol, String prefix, TradingDate start, TradingDate end) {
        String URLString = GOOGLE_URL_PATTERN;
        String months[] = { "Jan", "Feb", "Mar", "Apr", "May", "Jun", "Jul", "Aug", "Sep", "Oct", "Nov", "Dec" };
        String symbolString = symbol.toString();
        if (prefix.length() > 0) {
            if (!prefix.endsWith(":")) prefix += ":";
            symbolString = (prefix + symbolString);
        }
        URLString = Find.replace(URLString, SYMBOL, symbolString);
        URLString = Find.replace(URLString, START_DAY, Integer.toString(start.getDay()));
        URLString = Find.replace(URLString, START_MONTH, months[start.getMonth() - 1]);
        URLString = Find.replace(URLString, START_YEAR, Integer.toString(start.getYear()));
        URLString = Find.replace(URLString, END_DAY, Integer.toString(end.getDay()));
        URLString = Find.replace(URLString, END_MONTH, months[end.getMonth() - 1]);
        URLString = Find.replace(URLString, END_YEAR, Integer.toString(end.getYear()));
        return URLString;
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
                report.addWarning(Locale.getString("GOOGLE_DISPLAY_URL") + ":" + quote.getSymbol() + ":" + quote.getDate() + ":" + Locale.getString("WARNING") + ": " + message);
            }
        }
    }
}
