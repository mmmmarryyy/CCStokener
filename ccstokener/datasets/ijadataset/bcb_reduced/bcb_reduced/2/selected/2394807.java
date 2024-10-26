package bioevent.semanticsimilarity;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.File;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.net.MalformedURLException;
import java.net.URL;
import java.net.URLConnection;
import java.net.URLEncoder;
import java.sql.SQLException;
import java.util.HashMap;
import java.util.Map;
import org.json.JSONException;
import org.json.JSONObject;
import org.json.JSONTokener;
import bioevent.core.Util;
import bioevent.db.entities.SearchResultCount;

/**
 * This implements the Normalized Google Distance (NGD) as described in
 * R.L. Cilibrasi and P.M.B. Vitanyi, "The Google Similarity Distance",
 * IEEE Trans. Knowledge and Data Engineering, 19:3(2007), 370 - 383
 */
public class SearchEngineDistanceCalculator {

    public enum SimilarityMethod {

        PMI, NGD
    }

    public enum PhraseHandleMethod {

        EXACT, MAX, ROUGH
    }

    public enum Corpus {

        WIKI, PUBMED, Unconstrained, NIH, WORDNET
    }

    public enum SearchEngine {

        Google, Yahoo
    }

    SimilarityMethod similarityMethod;

    PhraseHandleMethod phraseMethod;

    Corpus corpus;

    SearchEngine searchEngine;

    long TOTAL_DOC_GOOGLE = 38000000000000L;

    long WIKI_PAGE_GOOGLE = 79000000L;

    long NIH_PAGE_GOOGLE = 32000000L;

    long WORDNET_PAGE_GOOGLE = 21400L;

    long TOTAL_DOC_YAHOO = 12000000000000L;

    long WIKI_PAGE_YAHOO = 144000000L;

    long NIH_PAGE_YAHOO = 100000000L;

    long WORDNET_PAGE_YAHOO = 74000L;

    /** A Google URL that will return the number of matches, among other things. */
    private static final String GOOGLE_SEARCH_SITE_PREFIX = "http://ajax.googleapis.com/ajax/services/search/web?v=1.0&";

    /** A Yahoo URL that will return the number of matches, among other things. */
    private static final String YAHOO_SEARCH_SITE_PREFIX = "http://boss.yahooapis.com/ysearch/web/v1/";

    /** The file in the eclipse install directory containing a textual rep.
	 * of the cache.	 */
    protected static final String CACHE_FILE_NAME = "google.cache";

    static int counter = 0;

    /** The logarithm of a number that is (hopefully) greater than or equal
	 *  to the (unpublished) indexed number of Google documents.
	 *  http://googleblog.blogspot.com/2008/07/we-knew-web-was-big.html
	 *  puts this at a trillion or more.  */
    protected static final double logN = Math.log(1.0e12);

    Map<String, Double> cache = new HashMap<String, Double>();

    /** Holds the new terms we entered (these are also in the cache) */
    Map<String, Double> newCache = new HashMap<String, Double>();

    /** The key to use for querying Yahoo.   This is read in via the system
	 * property "yahooApiKey".  */
    private static String yahooApiKey = "Lj_QEjnV34Hjib6aReUGN_EvBAj4n2Mfu3NDCE8seRHM.tDPquoSvtNxc4qP_ju1HWDN71or";

    public SearchEngineDistanceCalculator(SimilarityMethod pSimilarityMethod, PhraseHandleMethod pPhraseMethod, Corpus pCorpus, SearchEngine pSearchEngine) throws NumberFormatException, IOException {
        cache = setupCache(CACHE_FILE_NAME);
        similarityMethod = pSimilarityMethod;
        phraseMethod = pPhraseMethod;
        corpus = pCorpus;
        searchEngine = pSearchEngine;
    }

    public void clearCache() {
        cache = new HashMap<String, Double>();
        newCache = new HashMap<String, Double>();
        File cacheFile = new File(CACHE_FILE_NAME);
        cacheFile.delete();
    }

    protected Map<String, Double> setupCache(String filename) throws NumberFormatException, IOException {
        File cacheFile = new File(filename);
        if (cacheFile.canRead()) {
            BufferedReader reader = new BufferedReader(new FileReader(filename));
            Map<String, Double> cache = new HashMap<String, Double>();
            String line;
            while ((line = reader.readLine()) != null) {
                int lastSpaceIndex = line.lastIndexOf(' ');
                String token = line.substring(0, lastSpaceIndex);
                double count = Double.parseDouble(line.substring(lastSpaceIndex + 1));
                cache.put(token, count);
            }
            reader.close();
        }
        return cache;
    }

    /**
	 * Adds the contents of newCache to the specified file
	 * @param filename
	 */
    protected void updateCache(String filename) {
        if (counter++ >= 20) {
            BufferedWriter writer = null;
            try {
                writer = new BufferedWriter(new FileWriter(filename, true));
                for (Map.Entry<String, Double> entry : newCache.entrySet()) {
                    writer.append(entry.getKey() + " " + entry.getValue() + "\n");
                }
                newCache = new HashMap<String, Double>();
                counter = 0;
            } catch (IOException e) {
            } finally {
                if (writer != null) {
                    try {
                        writer.close();
                    } catch (IOException e) {
                        e.printStackTrace();
                    }
                }
            }
        }
    }

    protected double numResultsFromWeb(String term, int step) throws IOException, SQLException {
        double result = 0;
        boolean retry = false;
        if (cache.containsKey(term)) {
            result = cache.get(term);
        } else {
            String source_string = searchEngine.name().toLowerCase() + "-" + corpus.name();
            result = SearchResultCount.findSearchResultCount(term, source_string);
            if (result == -1) {
                try {
                    Thread.sleep(200);
                } catch (InterruptedException e) {
                    e.printStackTrace();
                }
                URL url = null;
                InputStream stream = null;
                try {
                    url = makeQueryURL(term);
                    URLConnection connection = url.openConnection();
                    connection.setRequestProperty("Referer", "http://www.patternsintext.com/");
                    System.setProperty("http.agent", "");
                    stream = connection.getInputStream();
                    InputStreamReader inputReader = new InputStreamReader(stream);
                    BufferedReader bufferedReader = new BufferedReader(inputReader);
                    double count = getCountFromQuery(bufferedReader);
                    cache.put(term, count);
                    newCache.put(term, count);
                    updateCache(CACHE_FILE_NAME);
                    SearchResultCount.insertSearchResultCount(term, source_string, (int) count);
                    result = count;
                } catch (Exception e) {
                    retry = true;
                } finally {
                    if (stream != null) {
                        try {
                            stream.close();
                        } catch (IOException e) {
                            Util.log(e.toString(), 3);
                        }
                    }
                }
            }
        }
        if (retry && step < 20) {
            try {
                Thread.sleep(5000 * step);
            } catch (InterruptedException e) {
                e.printStackTrace();
            }
            Util.log("Retrying " + term, 1);
            result = numResultsFromWeb(term, step + 1);
        }
        return result;
    }

    private double getCountFromQuery(BufferedReader reader) throws IOException, JSONException {
        double count = 0;
        switch(searchEngine) {
            case Google:
                count = getCountFromGoogleQuery(reader);
                break;
            case Yahoo:
                count = getCountFromYahooQuery(reader);
                break;
        }
        return count;
    }

    private double getCountFromYahooQuery(BufferedReader reader) throws IOException, JSONException {
        String line;
        StringBuilder builder = new StringBuilder();
        while ((line = reader.readLine()) != null) {
            builder.append(line);
        }
        String response = builder.toString();
        JSONObject json = new JSONObject(response);
        JSONObject searchResponse = json.getJSONObject("ysearchresponse");
        double count = searchResponse.getDouble("totalhits");
        return count;
    }

    @SuppressWarnings("unused")
    private double getCountFromGoogleQuery(BufferedReader bufferedReader) throws JSONException {
        JSONObject json = new JSONObject(new JSONTokener(bufferedReader));
        JSONObject responseData = json.getJSONObject("responseData");
        JSONObject cursor = responseData.getJSONObject("cursor");
        double count = 0;
        try {
            count = cursor.getDouble("estimatedResultCount");
        } catch (JSONException e) {
            count = 0;
        }
        return count;
    }

    protected URL makeQueryURL(String term) throws MalformedURLException, IOException {
        String searchTerm = URLEncoder.encode(term, "UTF-8");
        URL url;
        String urlString = "";
        switch(searchEngine) {
            case Google:
                urlString = makeGoogleQueryString(searchTerm);
                break;
            case Yahoo:
                urlString = makeYahooQueryString(searchTerm);
                break;
        }
        url = new URL(urlString);
        return url;
    }

    /**
	 * Builds a query string suitable for Google
	 * @param searchTerm
	 * @return
	 */
    @SuppressWarnings("unused")
    private String makeGoogleQueryString(String searchTerm) {
        switch(corpus) {
            case WIKI:
                searchTerm += "+site:wikipedia.org";
                break;
            case NIH:
                searchTerm += "+site:nih.gov";
                break;
            case WORDNET:
                searchTerm += "+site:wordnetweb.princeton.edu/perl/webwn";
                break;
        }
        String urlString = GOOGLE_SEARCH_SITE_PREFIX + "q=" + searchTerm + " ";
        return urlString;
    }

    /**
	 * Builds a query string suitable for Yahoo
	 * @param searchTerm
	 * @return
	 */
    private String makeYahooQueryString(String searchTerm) {
        String urlString = YAHOO_SEARCH_SITE_PREFIX + searchTerm + "?appid=" + yahooApiKey + "&count=0&format=json";
        return urlString;
    }

    /**
	 * Calculates the normalized Google Distance (NGD) between the two terms
	 * specified.  NOTE: this number can change between runs, because it is
	 * based on the number of web pages found by Google, which changes.
	 * @return a number from 0 (minimally distant) to 1 (maximally distant),
	 *   unless an exception occurs in which case, it is negative
	 *   (RefactoringConstants.UNKNOWN_DISTANCE)
	 * @throws IOException 
	 * @throws JSONException 
	 * @throws SQLException 
	 */
    public Double calculateSimilarity(String term1, String term2) throws JSONException, IOException, SQLException {
        term1 = term1.trim();
        term2 = term2.trim();
        if (term1.equals("") || term2.equals("") || term1.equals("\"\"") || term2.equals("\"\"")) return 0.0;
        Double similarity = 0.0;
        long TOTAL_DOC_COUNT = 0;
        long WIKI_PAGE_COUNT = 0;
        long NIH_PAGE_COUNT = 0;
        long WORDNET_PAGE_COUNT = 0;
        switch(searchEngine) {
            case Google:
                TOTAL_DOC_COUNT = TOTAL_DOC_GOOGLE;
                WIKI_PAGE_COUNT = WIKI_PAGE_GOOGLE;
                NIH_PAGE_COUNT = NIH_PAGE_GOOGLE;
                WORDNET_PAGE_COUNT = WIKI_PAGE_GOOGLE;
                break;
            case Yahoo:
                TOTAL_DOC_COUNT = TOTAL_DOC_YAHOO;
                WIKI_PAGE_COUNT = WIKI_PAGE_YAHOO;
                NIH_PAGE_COUNT = NIH_PAGE_YAHOO;
                WORDNET_PAGE_COUNT = WIKI_PAGE_YAHOO;
                break;
        }
        long total_pages = 0;
        switch(corpus) {
            case NIH:
                total_pages = NIH_PAGE_COUNT;
                break;
            case WIKI:
                total_pages = WIKI_PAGE_COUNT;
                break;
            case Unconstrained:
                total_pages = TOTAL_DOC_COUNT;
                break;
            case WORDNET:
                total_pages = WORDNET_PAGE_COUNT;
                break;
        }
        double x_count = numResultsFromWeb(term1, 1);
        double y_count = numResultsFromWeb(term2, 1);
        double xy_count = numResultsFromWeb(term1 + " AND " + term2, 1);
        double px = x_count / total_pages;
        double py = y_count / total_pages;
        double pxy = xy_count / total_pages;
        switch(similarityMethod) {
            case PMI:
                similarity = -Math.log(pxy / (px * py)) / Math.log(pxy);
                similarity = (similarity + 1) / 2;
                break;
            case NGD:
                double log_x_count = Math.log(x_count);
                double log_y_count = Math.log(y_count);
                double log_xy_count = Math.log(xy_count);
                similarity = Math.max(log_x_count, log_y_count) - log_xy_count;
                similarity = 1 - (similarity / (Math.log(total_pages) - Math.min(log_x_count, log_y_count)));
                break;
        }
        if (similarity.isInfinite() || similarity.isNaN()) similarity = 0.0;
        Util.log(term1 + " - " + term2 + " = " + similarity, 1);
        return similarity;
    }

    public String getFullMethodName() {
        String name = similarityMethod.name() + "-" + searchEngine.name().charAt(0) + "-" + corpus.name() + "-" + phraseMethod.name();
        return name;
    }

    public double calculateSimilarityTwoPhrase(String phrase1, String phrase2) throws JSONException, IOException, SQLException {
        double similarity = 0.0;
        switch(phraseMethod) {
            case EXACT:
                similarity = getExactSimilarity(phrase1, phrase2);
                break;
            case MAX:
                similarity = getMaxSimilarity(phrase1, phrase2);
                break;
            case ROUGH:
                similarity = getRoughSimilarity(phrase1, phrase2);
                break;
        }
        return similarity;
    }

    double getMaxSimilarity(String phrase1, String phrase2) throws JSONException, IOException, SQLException {
        double maxSimilarity = 0.0;
        String[] parts1 = phrase1.split(" ");
        String[] parts2 = phrase2.split(" ");
        for (String part1 : parts1) for (String part2 : parts2) {
            double simlarity = calculateSimilarity("\"" + part1 + "\"", "\"" + part2 + "\"");
            if (simlarity > maxSimilarity) maxSimilarity = simlarity;
        }
        return maxSimilarity;
    }

    double getRoughSimilarity(String phrase1, String phrase2) throws JSONException, IOException, SQLException {
        double simlarity = calculateSimilarity(phrase1, phrase2);
        return simlarity;
    }

    double getExactSimilarity(String phrase1, String phrase2) throws JSONException, IOException, SQLException {
        double simlarity = calculateSimilarity("\"" + phrase1 + "\"", "\"" + phrase2 + "\"");
        return simlarity;
    }
}
