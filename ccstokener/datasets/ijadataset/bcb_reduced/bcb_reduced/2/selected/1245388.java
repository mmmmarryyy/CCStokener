package org.transdroid.search.ThePirateBay;

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
 * An adapter that provides access to The Pirate Bay torrent searches by parsing
 * the raw HTML output.
 * 
 * @author Eric Kok
 */
public class ThePirateBayAdapter implements ISearchAdapter {

    private static final String QUERYURL = "http://thepiratebay.org/search/%s/%s/%s/100,200,300,400,600/";

    private static final String SORT_COMPOSITE = "99";

    private static final String SORT_SEEDS = "7";

    private static final int CONNECTION_TIMEOUT = 20000;

    @Override
    public List<SearchResult> search(String query, SortOrder order, int maxResults) throws Exception {
        if (query == null) {
            return null;
        }
        String encodedQuery = "";
        try {
            encodedQuery = URLEncoder.encode(query, "UTF-8");
        } catch (UnsupportedEncodingException e) {
            throw e;
        }
        final int startAt = 0;
        final int pageNr = (startAt - 1) / 30;
        final String url = String.format(QUERYURL, encodedQuery, String.valueOf(pageNr), (order == SortOrder.BySeeders ? SORT_SEEDS : SORT_COMPOSITE));
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
            final String RESULTS = "<table id=\"searchResult\">";
            final String TORRENT = "<div class=\"detName\">";
            List<SearchResult> results = new ArrayList<SearchResult>();
            int resultsStart = html.indexOf(RESULTS) + RESULTS.length();
            int torStart = html.indexOf(TORRENT, resultsStart);
            while (torStart >= 0) {
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
        return "The Pirate Bay";
    }

    private SearchResult parseHtmlItem(String htmlItem) {
        final String DETAILS = "<a href=\"";
        final String DETAILS_END = "\" class=\"detLink\"";
        final String NAME = "\">";
        final String NAME_END = "</a>";
        final String MAGNET_LINK = "<a href=\"";
        final String MAGNET_LINK_END = "\" title=\"Download this torrent using magnet";
        final String LINK = "<a href=\"";
        final String LINK_END = "\" title=\"Download this torrent\"";
        final String DATE = "detDesc\">Uploaded ";
        final String DATE_END = ", Size ";
        final String SIZE = ", Size ";
        final String SIZE_END = ", ULed by";
        final String SEEDERS = "<td align=\"right\">";
        final String SEEDERS_END = "</td>";
        final String LEECHERS = "<td align=\"right\">";
        final String LEECHERS_END = "</td>";
        String prefixDetails = "http://thepiratebay.org";
        String prefixYear = (new Date().getYear() + 1900) + " ";
        SimpleDateFormat df1 = new SimpleDateFormat("yyyy MM-dd HH:mm");
        SimpleDateFormat df2 = new SimpleDateFormat("MM-dd yyyy");
        int detailsStart = htmlItem.indexOf(DETAILS) + DETAILS.length();
        String details = htmlItem.substring(detailsStart, htmlItem.indexOf(DETAILS_END, detailsStart));
        details = prefixDetails + details;
        int nameStart = htmlItem.indexOf(NAME, detailsStart) + NAME.length();
        String name = htmlItem.substring(nameStart, htmlItem.indexOf(NAME_END, nameStart));
        int magnetLinkStart = htmlItem.indexOf(MAGNET_LINK, nameStart) + MAGNET_LINK.length();
        String magnetLink = htmlItem.substring(magnetLinkStart, htmlItem.indexOf(MAGNET_LINK_END, magnetLinkStart));
        int linkStart = htmlItem.indexOf(LINK, magnetLinkStart);
        int linkEnd = htmlItem.indexOf(LINK_END, magnetLinkStart);
        String link = linkEnd >= 0 && linkStart < linkEnd ? htmlItem.substring(linkStart + LINK.length(), linkEnd) : null;
        int dateStart = htmlItem.indexOf(DATE, magnetLinkStart) + DATE.length();
        String dateText = htmlItem.substring(dateStart, htmlItem.indexOf(DATE_END, dateStart));
        dateText = dateText.replace("&nbsp;", " ");
        Date date = null;
        try {
            date = df1.parse(prefixYear + dateText);
        } catch (ParseException e) {
            try {
                date = df2.parse(dateText);
            } catch (ParseException e1) {
            }
        }
        int sizeStart = htmlItem.indexOf(SIZE, dateStart) + SIZE.length();
        String size = htmlItem.substring(sizeStart, htmlItem.indexOf(SIZE_END, sizeStart));
        size = size.replace("&nbsp;", " ");
        int seedersStart = htmlItem.indexOf(SEEDERS, sizeStart) + SEEDERS.length();
        String seedersText = htmlItem.substring(seedersStart, htmlItem.indexOf(SEEDERS_END, seedersStart));
        int seeders = Integer.parseInt(seedersText);
        int leechersStart = htmlItem.indexOf(LEECHERS, seedersStart) + LEECHERS.length();
        String leechersText = htmlItem.substring(leechersStart, htmlItem.indexOf(LEECHERS_END, leechersStart));
        int leechers = Integer.parseInt(leechersText);
        return new SearchResult(name, link != null ? link : magnetLink, details, size, date, seeders, leechers);
    }
}
