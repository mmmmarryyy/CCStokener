package org.transdroid.search.Demonoid;

import java.io.InputStream;
import java.io.UnsupportedEncodingException;
import java.net.URLEncoder;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import org.apache.http.HttpResponse;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.impl.client.DefaultHttpClient;
import org.apache.http.params.BasicHttpParams;
import org.apache.http.params.HttpConnectionParams;
import org.apache.http.params.HttpParams;
import org.transdroid.search.ISearchAdapter;
import org.transdroid.search.SearchResult;
import org.transdroid.search.SortOrder;
import org.transdroid.util.HttpHelper;

/**
 * An adapter that provides access to demonoid torrent searches by parsing
 * the raw HTML output.
 * 
 * @author Gabor Tanka
 */
public class DemonoidAdapter implements ISearchAdapter {

    private static final String QUERYURL = "http://www.demonoid.me/files/?to=0&uid=0&category=0&subcategory=0&language=0&seeded=2&quality=0&external=2&query=%s&sort=%s&page=0";

    private static final String SORT_COMPOSITE = "H";

    private static final String SORT_SEEDS = "S";

    private static final int CONNECTION_TIMEOUT = 10000;

    private int maxResults;

    @Override
    public List<SearchResult> search(String query, SortOrder order, int maxResults) throws Exception {
        if (query == null) {
            return null;
        }
        this.maxResults = maxResults;
        String encodedQuery = "";
        try {
            encodedQuery = URLEncoder.encode(query, "UTF-8");
        } catch (UnsupportedEncodingException e) {
            throw e;
        }
        final String url = String.format(QUERYURL, encodedQuery, (order == SortOrder.BySeeders ? SORT_SEEDS : SORT_COMPOSITE));
        HttpParams httpparams = new BasicHttpParams();
        HttpConnectionParams.setConnectionTimeout(httpparams, CONNECTION_TIMEOUT);
        HttpConnectionParams.setSoTimeout(httpparams, CONNECTION_TIMEOUT);
        DefaultHttpClient httpclient = new DefaultHttpClient(httpparams);
        httpclient.getParams().setParameter("http.useragent", "Mozilla/5.0 (Windows; U; Windows NT 6.1; en-US; rv:1.9.2.2) Gecko/20100316 Firefox/3.6.2");
        HttpGet httpget = new HttpGet(url);
        HttpResponse response = httpclient.execute(httpget);
        InputStream instream = response.getEntity().getContent();
        String html = HttpHelper.ConvertStreamToString(instream);
        instream.close();
        return parseHtml(html);
    }

    protected List<SearchResult> parseHtml(String html) throws Exception {
        try {
            final String RESULTS = "<td class=\"torrent_header_2\">";
            final String TORRENT = "<!-- tstart -->";
            List<SearchResult> results = new ArrayList<SearchResult>();
            int resultsStart = html.indexOf(RESULTS) + RESULTS.length();
            int torStart = html.indexOf(TORRENT, resultsStart);
            while (torStart >= 0 && results.size() < maxResults) {
                int nextTorrentIndex = html.indexOf(TORRENT, torStart + TORRENT.length());
                if (nextTorrentIndex >= 0) {
                    results.add(parseHtmlItem(html.substring(torStart + TORRENT.length(), nextTorrentIndex)));
                } else {
                    results.add(parseHtmlItem(html.substring(torStart + TORRENT.length())));
                }
                torStart = nextTorrentIndex;
            }
            return results;
        } catch (OutOfMemoryError e) {
            throw new Exception(e);
        } catch (Exception e) {
            throw new Exception(e);
        }
    }

    @Override
    public String buildRssFeedUrlFromSearch(String query, SortOrder order) {
        return null;
    }

    @Override
    public String getSiteName() {
        return "Demonoid";
    }

    private SearchResult parseHtmlItem(String htmlItem) {
        final String DETAILS = "><a href=\"";
        final String DETAILS_END = "\" target=\"_blank";
        final String NAME = "\">";
        final String NAME_END = "</a>";
        final String LINK = "align=\"center\"><a href=\"";
        final String LINK_END = "\"><img src=\"/images/dmi.gif\"";
        final String SIZE = "align=\"right\">";
        final String SIZE_END = "</td>";
        final String SEEDERS = "class=\"green\">";
        final String SEEDERS_END = "</font>";
        final String LEECHERS = "class=\"red\">";
        final String LEECHERS_END = "</font>";
        final String DATE = ">Added on ";
        final String DATE_END = "</td>";
        String prefix = "http://www.demonoid.me";
        SimpleDateFormat df = new SimpleDateFormat("EEEE, MMM dd, yyyy");
        int detailsStart = htmlItem.indexOf(DETAILS) + DETAILS.length();
        String details = htmlItem.substring(detailsStart, htmlItem.indexOf(DETAILS_END, detailsStart));
        details = prefix + details;
        int nameStart = htmlItem.indexOf(NAME, detailsStart) + NAME.length();
        String name = htmlItem.substring(nameStart, htmlItem.indexOf(NAME_END, nameStart));
        int linkStart = htmlItem.indexOf(LINK, nameStart) + LINK.length();
        String link = htmlItem.substring(linkStart, htmlItem.indexOf(LINK_END, linkStart));
        link = prefix + link;
        int sizeStart = htmlItem.indexOf(SIZE, linkStart) + SIZE.length();
        String size = htmlItem.substring(sizeStart, htmlItem.indexOf(SIZE_END, sizeStart));
        int seedersStart = htmlItem.indexOf(SEEDERS, sizeStart) + SEEDERS.length();
        String seedersText = htmlItem.substring(seedersStart, htmlItem.indexOf(SEEDERS_END, seedersStart));
        int seeders = Integer.parseInt(seedersText);
        int leechersStart = htmlItem.indexOf(LEECHERS, seedersStart) + LEECHERS.length();
        String leechersText = htmlItem.substring(leechersStart, htmlItem.indexOf(LEECHERS_END, leechersStart));
        int leechers = Integer.parseInt(leechersText);
        int dateStart = htmlItem.indexOf(DATE, leechersStart) + DATE.length();
        String dateText = htmlItem.substring(dateStart, htmlItem.indexOf(DATE_END, dateStart));
        Date date = null;
        try {
            date = df.parse(dateText);
        } catch (ParseException e) {
        }
        return new SearchResult(name, link, details, size, date, seeders, leechers);
    }
}
