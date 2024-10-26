package org.transdroid.search.Isohunt;

import java.io.InputStream;
import java.io.UnsupportedEncodingException;
import java.net.URLEncoder;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import org.apache.http.HttpResponse;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.impl.client.DefaultHttpClient;
import org.apache.http.params.BasicHttpParams;
import org.apache.http.params.HttpConnectionParams;
import org.apache.http.params.HttpParams;
import org.json.JSONArray;
import org.json.JSONObject;
import org.transdroid.search.ISearchAdapter;
import org.transdroid.search.SearchResult;
import org.transdroid.search.SortOrder;
import org.transdroid.util.HttpHelper;

/**
 * An adapter that provides easy access to isoHunt torrent searches. Communication
 * is handled via the isoHunt JSON REST API.
 * 
 * @author Eric Kok
 */
public class IsohuntAdapter implements ISearchAdapter {

    private static final String RPC_QUERYURL = "http://isohunt.com/js/json.php?ihq=%s&start=%s&rows=%s";

    private static final String RPC_SORT_COMPOSITE = "";

    private static final String RPC_SORT_SEEDS = "&sort=seeds";

    private static final int CONNECTION_TIMEOUT = 10000;

    private static final String RPC_RESULTS = "total_results";

    private static final String RPC_ITEMS = "items";

    private static final String RPC_ITEMS_LIST = "list";

    private static final String RPC_ITEM_TITLE = "title";

    private static final String RPC_ITEM_LINK = "link";

    private static final String RPC_ITEM_URL = "enclosure_url";

    private static final String RPC_ITEM_SIZE = "size";

    private static final String RPC_ITEM_SEEDS = "Seeds";

    private static final String RPC_ITEM_LEECHERS = "leechers";

    private static final String RPC_ITEM_PUBDATE = "pubDate";

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
        String url = String.format(RPC_QUERYURL, encodedQuery, String.valueOf(startAt), String.valueOf(maxResults));
        if (order == SortOrder.BySeeders) {
            url += RPC_SORT_SEEDS;
        } else {
            url += RPC_SORT_COMPOSITE;
        }
        HttpParams httpparams = new BasicHttpParams();
        HttpConnectionParams.setConnectionTimeout(httpparams, CONNECTION_TIMEOUT);
        HttpConnectionParams.setSoTimeout(httpparams, CONNECTION_TIMEOUT);
        DefaultHttpClient httpclient = new DefaultHttpClient(httpparams);
        HttpGet httpget = new HttpGet(url);
        HttpResponse response = httpclient.execute(httpget);
        InputStream instream = response.getEntity().getContent();
        String result = HttpHelper.ConvertStreamToString(instream);
        result = result.replace(query, encodedQuery);
        JSONObject json = new JSONObject(result);
        instream.close();
        if (json.getInt(RPC_RESULTS) == 0) {
            return new ArrayList<SearchResult>();
        }
        JSONObject list = json.getJSONObject(RPC_ITEMS);
        JSONArray items = list.getJSONArray(RPC_ITEMS_LIST);
        List<SearchResult> results = new ArrayList<SearchResult>();
        for (int i = 0; i < items.length(); i++) {
            JSONObject item = items.getJSONObject(i);
            results.add(new SearchResult(item.getString(RPC_ITEM_TITLE).replaceAll("\\<.*?>", ""), item.getString(RPC_ITEM_URL), item.getString(RPC_ITEM_LINK), item.getString(RPC_ITEM_SIZE), new Date(Date.parse(item.getString(RPC_ITEM_PUBDATE))), item.getInt(RPC_ITEM_SEEDS), item.getInt(RPC_ITEM_LEECHERS)));
        }
        return results;
    }

    @Override
    public String buildRssFeedUrlFromSearch(String query, SortOrder order) {
        String encodedQuery = "";
        try {
            encodedQuery = URLEncoder.encode(query, "UTF-8");
        } catch (UnsupportedEncodingException e) {
            e.printStackTrace();
        }
        return "http://isohunt.com/js/rss/" + encodedQuery;
    }

    @Override
    public String getSiteName() {
        return "isoHunt";
    }
}
