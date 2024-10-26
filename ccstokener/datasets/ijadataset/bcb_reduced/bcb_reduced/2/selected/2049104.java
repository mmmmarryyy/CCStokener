package ch.ethz.dcg.spamato.filter.domainator.search;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.UnsupportedEncodingException;
import java.net.HttpURLConnection;
import java.net.ProtocolException;
import java.net.URL;
import java.net.URLEncoder;
import java.nio.charset.Charset;
import java.util.StringTokenizer;
import org.json.JSONException;
import org.json.JSONObject;

/**
 * Does different searches on google.com for domains contained in a mail.
 *
 * @author Christian Wassmer
 */
public class GoogleDomainSearch implements IDomainSearcher {

    private static final int CONNECT_TIMEOUT = 2 * 1000;

    /**
	 * The Google AJAX Search API key for all URLs referring from <tt>http://www.spamato.net/filters/</tt>
	 */
    public static final String GOOGLE_AJAX_SEARCH_API_KEY = "ABQIAAAAskabN-t6GtnzSCLKe0ODdRTNxglyOJ8VxWq5n32aJCtHkgTWhxTwzbssU5xQrjINVi0V_SJMuZWlnA";

    public static final Charset GOOGLE_CHARSET = Charset.forName("UTF-8");

    enum SearchPattern {

        DOMAIN(SearchResult.SearchType.DOMAIN, "'DOMAIN'"), WWW_LINK(SearchResult.SearchType.WWW_LINK, "link:www.DOMAIN"), RELATED(SearchResult.SearchType.RELATED, "related:DOMAIN"), SITE(SearchResult.SearchType.SITE, "site:DOMAIN"), DOMAIN_SPAM(SearchResult.SearchType.DOMAIN_SPAM, "DOMAIN+spam"), DOMAIN_BLACKLIST(SearchResult.SearchType.DOMAIN_BLACKLIST, "DOMAIN+blacklist"), DOMAIN_SPAM_BLACKLIST(SearchResult.SearchType.DOMAIN_SPAM_BLACKLIST, "DOMAIN+blacklist+spam");

        SearchResult.SearchType type;

        String pattern;

        SearchPattern(SearchResult.SearchType type, String pattern) {
            this.type = type;
            this.pattern = pattern;
        }

        public SearchResult.SearchType getType() {
            return type;
        }

        public String getPattern() {
            return pattern;
        }
    }

    public GoogleDomainSearch() {
    }

    /**
	 * Does searches with a given domain using all search-patterns.
	 */
    public SearchResult doSearch(String domain, String userID) {
        SearchResult searchResult = new SearchResult(domain);
        int noResultCounter = 0;
        for (SearchPattern pattern : SearchPattern.values()) {
            try {
                String query = getSearchURL(pattern.getType(), domain);
                int result = execute(query, userID);
                if (result != SearchResult.RESULT_UNKNOWN) {
                    searchResult.setResult(pattern.getType(), result);
                } else {
                    searchResult.setResult(pattern.getType(), SearchResult.RESULT_UNKNOWN);
                    noResultCounter++;
                }
            } catch (Exception e) {
            }
        }
        return noResultCounter == SearchPattern.values().length ? null : searchResult;
    }

    /**
	 * Build the request for google.com. For example: /search?q=site:bluewin.ch
	 */
    private String buildQuery(String pattern, String domain) {
        return pattern.replaceAll("DOMAIN", domain);
    }

    public String getSearchURL(SearchResult.SearchType type, String domain) throws UnsupportedEncodingException {
        String pattern = null;
        switch(type) {
            case DOMAIN:
                pattern = SearchPattern.DOMAIN.getPattern();
                break;
            case WWW_LINK:
                pattern = SearchPattern.WWW_LINK.getPattern();
                break;
            case RELATED:
                pattern = SearchPattern.RELATED.getPattern();
                break;
            case SITE:
                pattern = SearchPattern.SITE.getPattern();
                break;
            case DOMAIN_SPAM:
                pattern = SearchPattern.DOMAIN_SPAM.getPattern();
                break;
            case DOMAIN_BLACKLIST:
                pattern = SearchPattern.DOMAIN_BLACKLIST.getPattern();
                break;
            case DOMAIN_SPAM_BLACKLIST:
                pattern = SearchPattern.DOMAIN_SPAM_BLACKLIST.getPattern();
                break;
            default:
                return null;
        }
        return "http://ajax.googleapis.com/ajax/services/search/web?v=1.0&key=" + GOOGLE_AJAX_SEARCH_API_KEY + "&q=" + URLEncoder.encode(buildQuery(pattern, domain), "UTF-8");
    }

    private int execute(String query, String userID) {
        int result = SearchResult.RESULT_UNKNOWN;
        BufferedReader reader = null;
        HttpURLConnection connection = null;
        try {
            URL url = new URL(query);
            connection = (HttpURLConnection) url.openConnection();
            configureGoogleConnection(connection, userID);
            connection.connect();
            connection.getResponseCode();
            String line;
            final StringBuilder builder = new StringBuilder();
            reader = new BufferedReader(new InputStreamReader(connection.getInputStream()));
            while ((line = reader.readLine()) != null) {
                builder.append(line);
            }
            JSONObject json = new JSONObject(builder.toString());
            result = analyseResponse(json);
        } catch (Exception e) {
            result = SearchResult.RESULT_UNKNOWN;
        } finally {
            if (reader != null) {
                try {
                    reader.close();
                } catch (IOException e) {
                }
            }
        }
        return result;
    }

    private static void configureGoogleConnection(HttpURLConnection connection, String userID) throws ProtocolException {
        connection.setUseCaches(true);
        connection.setAllowUserInteraction(false);
        connection.setDoInput(true);
        connection.setDoOutput(false);
        connection.setConnectTimeout(CONNECT_TIMEOUT);
        connection.setInstanceFollowRedirects(true);
        connection.setRequestMethod("GET");
        connection.addRequestProperty("Referer", "http://www.spamato.net/filters/" + userID);
    }

    /**
	 * Extract 'number of hits' from the returned page.
	 * 
	 * @param buffer The page.
	 * @return number of hits
	 */
    private int analyseResponse(String buffer) {
        int result;
        if (buffer.indexOf("Showing web page information for") > 0) {
            result = 1;
        } else {
            int idxOfAbout = buffer.indexOf("of about");
            int idxOf = buffer.indexOf("of");
            if (idxOf > 0) {
                String rest = buffer.substring(idxOfAbout > 0 ? idxOfAbout : idxOf);
                StringTokenizer tokenizer = new StringTokenizer(rest, " ");
                tokenizer.nextToken();
                if (idxOfAbout > 0) {
                    tokenizer.nextToken();
                }
                String numberToken = tokenizer.nextToken();
                String number = numberToken.substring(3, numberToken.length() - 4);
                result = Integer.decode(number.replaceAll(",", "")).intValue();
            } else {
                return 0;
            }
        }
        return result;
    }

    /**
	 * Extract 'number of hits' from the returned page.
	 *
	 * @param buffer The page.
	 * @return number of hits
	 */
    protected int analyseResponse(JSONObject json) {
        int numberOfResults = 0;
        try {
            json = json.getJSONObject("responseData");
            json = json.getJSONObject("cursor");
            numberOfResults = json.getInt("estimatedResultCount");
        } catch (JSONException e) {
        }
        return numberOfResults;
    }
}
