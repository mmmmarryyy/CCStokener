package com.azureus.plugins.aztsearch;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.MalformedURLException;
import java.net.URL;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.GregorianCalendar;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Implements support for The Pirate Bay torrent search engine.
 * 
 * @version 0.4
 * @author Dalmazio Brisinda
 * 
 * <p>
 * This software is licensed under the 
 * <a href="http://creativecommons.org/licenses/GPL/2.0/">CC-GNU GPL.</a>
 */
public class TSPirateBayEngine extends TSEngine {

    private static final String PIRATEBAY_URL = "http://thepiratebay.org";

    private static final String PIRATEBAY_SEARCH_URL = PIRATEBAY_URL + "/search/%s/0/7/0";

    private static final String PIRATEBAY_NAME = "The Pirate Bay";

    private static final String PIRATEBAY_CATEGORY_RE = "\\<td[^\\>]*\\>\\s*\\<a[^\\>]+\\>([^\\<]*)\\</a\\>\\s*\\</td\\>";

    private static final String PIRATEBAY_NAME_RE = "\\<td[^\\>]*\\>\\s*\\<a[^\"]+\"([^\"]*)\"[^\\>]+\\>([^\\<]*)\\</a\\>\\s*\\</td\\>";

    private static final String PIRATEBAY_DATE_RE = "\\<td[^\\>]*\\>(\\d{2})-(\\d{2})\\s+(\\d{4}|\\d{2}:\\d{2})\\</td\\>";

    private static final String PIRATEBAY_DOWNLINK_RE = "\\<td[^\\>]*\\>\\s*\\<a[^\"]+\"([^\"]*)\".*?\\</td\\>";

    private static final String PIRATEBAY_SIZE_RE = "\\<td[^\\>]*\\>(\\d*\\.??\\d*)\\s+(\\w*)\\</td\\>";

    private static final String PIRATEBAY_SEEDS_RE = "\\<td[^\\>]*\\>\\s*(\\d+)\\s*\\</td\\>";

    private static final String PIRATEBAY_PEERS_RE = "\\<td[^\\>]*\\>\\s*(\\d+)\\s*\\</td\\>";

    private static final String PIRATEBAY_FORMATTED_RE = "\\<tr[^\\>]*\\>\\s*%s\\s*%s\\s*%s\\s*%s\\s*%s\\s*%s\\s*%s\\s*\\</tr\\>";

    private static final String PIRATEBAY_RE = String.format(PIRATEBAY_FORMATTED_RE, PIRATEBAY_CATEGORY_RE, PIRATEBAY_NAME_RE, PIRATEBAY_DATE_RE, PIRATEBAY_DOWNLINK_RE, PIRATEBAY_SIZE_RE, PIRATEBAY_SEEDS_RE, PIRATEBAY_PEERS_RE);

    private static final Pattern PIRATEBAY_PATTERN = Pattern.compile(PIRATEBAY_RE, Pattern.DOTALL);

    private static final String PIRATEBAY_NEXTPAGE_FORMATTED_RE = "\\<a\\s+href=\"(/search/[^\"]+)\"\\>\\d+\\</a\\>";

    private static final String PIRATEBAY_CATEGORY_APPLICATION = "application";

    /**
	 * Constructs a new Pirate Bay search engine.
	 * 
	 * @param controller the main controller for the plugin arbitrating between
	 * the model (search results) and the GUI view.
	 * 
	 * @param resultsManager the search results manager for all searches.
	 */
    public TSPirateBayEngine(TSController controller, TSSearchResultsManager resultsManager) {
        super(controller, resultsManager);
    }

    /**
	 * Perform the search by sending the query string out to The Pirate Bay 
	 * torrent site, collecting, parsing, and organizing the results.
	 * 
	 * @param query the search string trimmed and encoded for URL search engine
	 * submission.
	 */
    public void performSearch(String query) {
        super.performSearch(query);
        try {
            String html = this.getHtmlPage(new URL(String.format(PIRATEBAY_SEARCH_URL, query)));
            if (html == null) {
                String message = "Pirate Bay: Error getting HTML.";
                System.err.println(message);
                TSMainViewPlugin.getLoggerChannel().log(message);
                return;
            }
            Matcher torrentMatcher = PIRATEBAY_PATTERN.matcher(html);
            int endOffset = this.extractResults(torrentMatcher);
            if (terminateSearch) return;
            Pattern nextPagePattern = Pattern.compile(PIRATEBAY_NEXTPAGE_FORMATTED_RE, Pattern.DOTALL);
            Matcher nextPageMatcher = nextPagePattern.matcher(html);
            nextPageMatcher.region(endOffset, nextPageMatcher.regionEnd());
            ArrayList<URL> pageList = new ArrayList<URL>();
            while (nextPageMatcher.find()) {
                pageList.add(new URL((PIRATEBAY_URL + nextPageMatcher.group(1)).replaceAll(" ", "+")));
            }
            for (URL url : pageList) {
                html = this.getHtmlPage(url);
                if (html == null) {
                    String message = "Pirate Bay: Error getting HTML.";
                    System.err.println(message);
                    TSMainViewPlugin.getLoggerChannel().log(message);
                    return;
                }
                torrentMatcher.reset(html);
                this.extractResults(torrentMatcher);
                if (terminateSearch) return;
            }
        } catch (MalformedURLException e) {
            e.printStackTrace();
        }
    }

    /**
	 * Get the specified HTML page. This method requires special handling of the
	 * Pirate Bay domain, by setting various cookies prior to reading the
	 * InputStream. Consequently the parent implementation is overridden.
	 * 
	 * @param url the URL of the page to fetch.
	 * @return the HTML page as a string or null if an error occurred.
	 */
    public String getHtmlPage(URL url) {
        String html = null;
        try {
            HttpURLConnection httpConn = (HttpURLConnection) url.openConnection();
            httpConn.setRequestProperty("Cookie", "language=en; searchTitle=1");
            BufferedReader br = new BufferedReader(new InputStreamReader(httpConn.getInputStream()));
            StringBuffer sb = new StringBuffer();
            String line;
            while ((line = br.readLine()) != null) {
                sb.append(line).append("\n");
            }
            br.close();
            html = sb.toString().replaceAll("&nbsp;", " ").replaceAll("&amp;", "&");
        } catch (MalformedURLException e) {
            e.printStackTrace();
        } catch (IOException e) {
            e.printStackTrace();
        }
        return html;
    }

    /**
	 * Extract the items from the matcher and populate the search results.
	 * 
	 * @param m the matcher.
	 * @return the offset after the last character of the last successful match.
	 */
    private int extractResults(Matcher m) {
        int end = 0;
        Calendar now = Calendar.getInstance();
        debugMatchCount = 0;
        while (m.find()) {
            end = m.end();
            int seeds = Integer.parseInt(m.group(10));
            int peers = Integer.parseInt(m.group(11));
            if (!controller.includeZeroSeedTorrents() && seeds <= 0) {
                terminateSearch = true;
                break;
            }
            if (!controller.includeDeadTorrents() && seeds <= 0 && peers <= 0) {
                terminateSearch = true;
                break;
            }
            int year;
            if (m.group(6).contains(":")) {
                year = now.get(Calendar.YEAR);
            } else {
                year = Integer.parseInt(m.group(6));
            }
            GregorianCalendar cal = new GregorianCalendar(year, Integer.parseInt(m.group(4)) - 1, Integer.parseInt(m.group(5)));
            URL downloadLink = null, pageLink = null;
            try {
                downloadLink = new URL(m.group(7));
                pageLink = new URL(PIRATEBAY_URL + m.group(2));
            } catch (MalformedURLException e) {
                e.printStackTrace();
            }
            float size = Float.parseFloat(m.group(8));
            TSSearchItem item = new TSSearchItem(cal.getTime(), this.getCategory(m.group(1)), m.group(3), pageLink, downloadLink, size, m.group(9).replace("i", ""), seeds, peers, PIRATEBAY_NAME);
            resultsManager.addToSearchResults(item);
            if (++resultsCount >= controller.maxResultsPerEngine()) {
                terminateSearch = true;
                break;
            }
        }
        return end;
    }

    /**
	 * Get the general category for the given PirateBay category designation.
	 * 
	 * @param category the PirateBay category designation.
	 * @return the original category all lower case, or a general string
	 * category (TSConstants.CATEGORY_SOFTWARE if the original category was
	 * PIRATEBAY_CATEGORY_APPLICATION).
	 */
    private String getCategory(String category) {
        String cat = category.toLowerCase();
        if (cat.contains(PIRATEBAY_CATEGORY_APPLICATION)) return TSConstants.CATEGORY_SOFTWARE; else return cat;
    }
}
