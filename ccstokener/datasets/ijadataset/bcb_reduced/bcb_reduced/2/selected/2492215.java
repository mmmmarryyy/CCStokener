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
import nz.org.venice.util.Currency;
import nz.org.venice.util.ExchangeRate;
import nz.org.venice.util.Find;
import nz.org.venice.util.Locale;

/**
 * Import exchange rate quotes from Yahoo into Venice.
 *
 * @author Andrew Leppard
 * @see YahooExchangeRateFilter
 */
public class YahooExchangeRateImport {

    private static final String SOURCE_CURRENCY = "_SRC_";

    private static final String DESTINATION_CURRENCY = "_DEST_";

    private static final String YAHOO_URL_PATTERN = "http://finance.yahoo.com/d/quotes.csv?s=_SRC__DEST_=X&f=sl1d1ba&e=.csv";

    private YahooExchangeRateImport() {
        assert false;
    }

    /**
     * Retrieve a sinlge exchange rate from Yahoo.
     *
     * @param sourceCurrency      the currency to convert from
     * @param destinationCurrency the currency to convert to
     * @return exchange rate
     * @exception ImportExportException if there was an error retrieving the exchange rate
     */
    public static ExchangeRate importExchangeRate(Currency sourceCurrency, Currency destinationCurrency) throws ImportExportException {
        ExchangeRate rate = null;
        YahooExchangeRateFilter filter = new YahooExchangeRateFilter();
        String URLString = constructURL(sourceCurrency, destinationCurrency);
        PreferencesManager.ProxyPreferences proxyPreferences = PreferencesManager.getProxySettings();
        try {
            URL url = new URL(URLString);
            InputStreamReader input = new InputStreamReader(url.openStream());
            BufferedReader bufferedInput = new BufferedReader(input);
            String line = bufferedInput.readLine();
            try {
                rate = filter.toExchangeRate(line);
            } catch (ExchangeRateFormatException e) {
                System.out.println(e);
                throw new ImportExportException(Locale.getString("ERROR_DOWNLOADING_QUOTES"));
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
            throw new ImportExportException(Locale.getString("ERROR_DOWNLOADING_QUOTES"));
        } catch (IOException e) {
            throw new ImportExportException(Locale.getString("ERROR_DOWNLOADING_QUOTES"));
        }
        return rate;
    }

    private static String constructURL(Currency sourceCurrency, Currency destinationCurrency) {
        String URLString = YAHOO_URL_PATTERN;
        URLString = Find.replace(URLString, SOURCE_CURRENCY, sourceCurrency.getCurrencyCode());
        URLString = Find.replace(URLString, DESTINATION_CURRENCY, destinationCurrency.getCurrencyCode());
        return URLString;
    }
}
