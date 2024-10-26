package uk.ac.soton.horserace.webscraping.betgenius;

import java.io.IOException;
import java.io.InputStream;
import java.net.CookieHandler;
import java.net.CookieManager;
import java.net.CookiePolicy;
import java.net.CookieStore;
import java.net.HttpCookie;
import java.net.MalformedURLException;
import java.net.URI;
import java.net.URISyntaxException;
import java.net.URL;
import java.net.URLConnection;
import java.util.HashMap;
import java.util.Map;
import org.apache.log4j.Logger;

public class OddsFormat {

    private static final Logger logger = Logger.getLogger(OddsFormat.class);

    /**
	 * Odds format
	 */
    public static final String FRANCTIONAL_ODDS_FORMAT = "Fraction";

    public static final String DECIMAL_ODDS_FORMAT = "Decimal";

    public static final String AMERICAN_ODDS_FORMAT = "American";

    public static final String DEFAULT_ODDS_FORMAT = DECIMAL_ODDS_FORMAT;

    public static final String ODDS_FORMAT_COOKIE_NAME = "OddsFormat";

    public static final String ODDS_FORMAT_COOKIE_PATH = "/";

    /**
	 * Special odds symbol map
	 */
    private Map<String, Double> specialSymbolMap = new HashMap<String, Double>();

    private String oddsFormat;

    private RaceRegistry raceRegistry;

    public OddsFormat() {
        specialSymbolMap.put("SP", -1.0);
        specialSymbolMap.put("NR", -2.0);
        specialSymbolMap.put("NQ", -3.0);
    }

    public String getOddsFormat() {
        return this.oddsFormat;
    }

    public void setOddsFormat(String oddsFormat) {
        this.oddsFormat = oddsFormat;
        setUp();
    }

    public void setRaceRegistry(RaceRegistry raceRegistry) {
        this.raceRegistry = raceRegistry;
    }

    private void setUp() {
        if (!(FRANCTIONAL_ODDS_FORMAT.equals(oddsFormat) || AMERICAN_ODDS_FORMAT.equals(oddsFormat) || DECIMAL_ODDS_FORMAT.equals(oddsFormat))) {
            oddsFormat = DEFAULT_ODDS_FORMAT;
        }
        CookieManager cookieManager = new CookieManager();
        cookieManager.setCookiePolicy(CookiePolicy.ACCEPT_ALL);
        CookieStore cookieStore = cookieManager.getCookieStore();
        for (String site : raceRegistry.getAllSites()) {
            HttpCookie cookie = new HttpCookie(ODDS_FORMAT_COOKIE_NAME, oddsFormat);
            cookie.setPath(ODDS_FORMAT_COOKIE_PATH);
            try {
                URI uri = new URI("http://" + site);
                cookieStore.add(uri, cookie);
            } catch (URISyntaxException e) {
                logger.error(e);
            }
        }
        CookieHandler.setDefault(cookieManager);
        logger.info("Odds format being used: " + oddsFormat);
        for (String site : raceRegistry.getAllSites()) {
            try {
                URL url = new URL("http://" + site + "/horse-racing/");
                URLConnection conn = url.openConnection();
                InputStream is = conn.getInputStream();
                is.close();
            } catch (MalformedURLException e) {
                logger.error(e.getLocalizedMessage(), e);
            } catch (IOException e) {
                logger.error(e.getLocalizedMessage(), e);
            }
        }
    }

    /**
	 * TODO not quite sure about the fractional odds format like 7/4.
	 * @param oddsStr
	 * @return
	 */
    public double parse(String oddsStr) {
        double d = processSpecialSymbols(oddsStr);
        if (d < 0) return d;
        return parseAnyOdds(oddsStr);
    }

    private double parseAnyOdds(String oddsStr) {
        if (FRANCTIONAL_ODDS_FORMAT.equals(oddsFormat)) return parseFranctionalOdds(oddsStr);
        return Double.parseDouble(oddsStr);
    }

    private double parseFranctionalOdds(String oddsStr) {
        if (oddsStr.indexOf('/') > 0) {
            String[] dd = oddsStr.split("/");
            return Double.parseDouble(dd[0]) / Double.parseDouble(dd[1]) + 1;
        } else {
            return Double.parseDouble(oddsStr) + 1;
        }
    }

    /**
	 * Process special symbols like SP, NR, NQ ...
	 * @param oddsStr
	 * @return negative if it is special symbol, otherwise zero.
	 */
    private double processSpecialSymbols(String oddsStr) {
        Double d = specialSymbolMap.get(oddsStr);
        if (d == null) return 0.0; else return d;
    }
}
