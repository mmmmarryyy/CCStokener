package com.nibri.shred.util;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.URL;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import net.sf.json.JSONArray;
import net.sf.json.JSONObject;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.apache.solr.util.NamedList;
import org.apache.xerces.parsers.DOMParser;
import org.w3c.dom.Document;
import org.xml.sax.InputSource;
import org.xml.sax.SAXException;
import com.nibri.shred.model.FacetCounts;
import com.nibri.shred.model.FacetCount;
import com.nibri.shred.model.Facet;
import com.nibri.shred.model.FacetCountsMap;

/**
 * Created on Mar 27, 2008
 * @author Michael Jones
 * 
 */
public class SolrClient {

    protected final Log logger = LogFactory.getLog(getClass());

    private String host;

    private String url;

    private String port;

    /**
     * The default is to get all docs with "*:*" but it can also be set int the context configuration
     */
    private static String DEFAULT_ALL_DOCS_QUERY = "*:*";

    private String allDocsQuery = DEFAULT_ALL_DOCS_QUERY;

    private boolean showEmptyFacets;

    private int lastNumberOfDocs;

    public int getLastNumberOfDocs() {
        return lastNumberOfDocs;
    }

    private void setLastNumberOfDocs(int lastNumberOfDocs) {
        logger.debug("Setting num of docs: " + lastNumberOfDocs);
        this.lastNumberOfDocs = lastNumberOfDocs;
    }

    public List<Map<String, String>> getDocsFromLuceneQuery(String query, int start, int rows) throws IOException {
        List<Map<String, String>> docList = new ArrayList<Map<String, String>>();
        NamedList<String> params = new NamedList<String>();
        params.add("q", appendQuery(query));
        params.add("rows", "" + rows);
        params.add("start", "" + start);
        JSONObject json = getJSONOutput(params);
        JSONObject response = JSONObject.fromObject(json.get("response"));
        setLastNumberOfDocs(response.getInt("numFound"));
        JSONArray docs = JSONArray.fromObject(response.get("docs"));
        for (int i = 0; i < docs.length(); i++) {
            JSONObject doc = docs.getJSONObject(i);
            Iterator<String> keys = doc.keys();
            Map<String, String> field2facet = new HashMap<String, String>();
            while (keys.hasNext()) {
                String field = keys.next();
                String facet = doc.getString(field);
                field2facet.put(field, facet);
            }
            docList.add(field2facet);
        }
        return docList;
    }

    public List<Map<String, String>> getDocsFromLuceneQuery(String query, int rows) throws IOException {
        List<Map<String, String>> docList = new ArrayList<Map<String, String>>();
        NamedList<String> params = new NamedList<String>();
        params.add("q", appendQuery(query));
        params.add("rows", "" + rows);
        JSONObject json = getJSONOutput(params);
        JSONObject response = JSONObject.fromObject(json.get("response"));
        setLastNumberOfDocs(response.getInt("numFound"));
        JSONArray docs = JSONArray.fromObject(response.get("docs"));
        for (int i = 0; i < docs.length(); i++) {
            JSONObject doc = docs.getJSONObject(i);
            Iterator<String> keys = doc.keys();
            Map<String, String> field2facet = new HashMap<String, String>();
            while (keys.hasNext()) {
                String field = keys.next();
                String facet = doc.getString(field);
                field2facet.put(field, facet);
            }
            docList.add(field2facet);
        }
        return docList;
    }

    public List<Map<String, String>> getDocsFromLuceneQuery(String query) throws Exception {
        List<Map<String, String>> docList = new ArrayList<Map<String, String>>();
        NamedList<String> params = new NamedList<String>();
        params.add("q", appendQuery(query));
        int rows = 10000;
        params.add("rows", "" + rows);
        JSONObject json = getJSONOutput(params);
        JSONObject response = JSONObject.fromObject(json.get("response"));
        int numFound = response.getInt("numFound");
        setLastNumberOfDocs(numFound);
        if (numFound == 0) {
            throw new IllegalArgumentException("No Document Elements found for: ");
        }
        for (int start = 0; start < numFound; start += rows) {
            List<Map<String, String>> subDocList = getDocsFromLuceneQuery(params, start);
            logger.debug("Geting sub docs: " + start);
            docList.addAll(subDocList);
        }
        return docList;
    }

    private String appendQuery(String query) {
        return getAllDocsQuery() + "+AND+" + query;
    }

    private List<Map<String, String>> getDocsFromLuceneQuery(NamedList<String> params, int start) throws Exception {
        List<Map<String, String>> docList = new ArrayList<Map<String, String>>();
        params = params.clone();
        params.add("start", "" + start);
        JSONObject json = getJSONOutput(params);
        JSONObject response = JSONObject.fromObject(json.get("response"));
        setLastNumberOfDocs(response.getInt("numFound"));
        JSONArray docs = JSONArray.fromObject(response.get("docs"));
        for (int i = 0; i < docs.length(); i++) {
            JSONObject doc = docs.getJSONObject(i);
            Iterator<String> keys = doc.keys();
            Map<String, String> field2facet = new HashMap<String, String>();
            while (keys.hasNext()) {
                String field = keys.next();
                String facet = doc.getString(field);
                field2facet.put(field, facet);
            }
            docList.add(field2facet);
        }
        return docList;
    }

    /**
     * Get all facet counts
     * @param facets
     * @return
     * @throws Exception
     */
    public FacetCountsMap getFacetCounts(List<Facet> facets) throws Exception {
        NamedList<String> params = new NamedList<String>();
        params.add("rows", "0");
        params.add("q", getAllDocsQuery());
        params.add("facet.limit", "" + -1);
        params.add("indent", "on");
        params.add("facet", "true");
        return getFacetCounts(params, facets);
    }

    /**
     * Get facet counts limited by query
     * @param facets
     * @param query
     * @return
     * @throws IOException
     */
    public FacetCountsMap getFacetCounts(List<Facet> facets, String query) throws IOException {
        NamedList<String> params = new NamedList<String>();
        params.add("rows", "0");
        params.add("q", appendQuery(query));
        params.add("facet.limit", "" + -1);
        params.add("indent", "on");
        params.add("facet", "true");
        return getFacetCounts(params, facets);
    }

    private FacetCountsMap getFacetCounts(NamedList<String> params, List<Facet> facets) throws IOException {
        Map<String, Facet> id2FacetMap = new HashMap<String, Facet>();
        for (Facet facet : facets) {
            params.add("facet.field", facet.getId());
            id2FacetMap.put(facet.getId(), facet);
        }
        FacetCountsMap facetCountsMap = new FacetCountsMap();
        JSONObject json = getJSONOutput(params);
        JSONObject response = JSONObject.fromObject(json.get("response"));
        setLastNumberOfDocs(response.getInt("numFound"));
        JSONObject facetsCounts = JSONObject.fromObject(json.get("facet_counts"));
        JSONObject facetFields = JSONObject.fromObject(facetsCounts.get("facet_fields"));
        Iterator it = facetFields.keys();
        while (it.hasNext()) {
            String facetField = it.next().toString();
            List<FacetCount> facetCountsList = new ArrayList<FacetCount>();
            JSONArray counts = JSONArray.fromObject(facetFields.get(facetField));
            if (isShowEmptyFacets() || counts.length() != 0) {
                for (int i = 0; i < counts.length() - 1; i += 2) {
                    String facet = counts.getString(i);
                    Integer count = counts.getInt(i + 1);
                    if (count > 0) {
                        facetCountsList.add(new FacetCount(facet, count));
                    }
                }
                facetCountsMap.put(id2FacetMap.get(facetField), new FacetCounts(facetCountsList));
            }
        }
        return facetCountsMap;
    }

    private String getStringFromInputStream(InputStream is) throws IOException {
        BufferedReader in = new BufferedReader(new InputStreamReader(is));
        StringBuilder buffer = new StringBuilder();
        String line;
        while ((line = in.readLine()) != null) {
            buffer.append(line);
        }
        return buffer.toString();
    }

    public String getHost() {
        return host;
    }

    public void setHost(String host) {
        this.host = host;
    }

    public String getUrl() {
        return url;
    }

    public void setUrl(String url) {
        this.url = url;
    }

    public String getPort() {
        return port;
    }

    public void setPort(String port) {
        this.port = port;
    }

    public String getAllDocsQuery() {
        return allDocsQuery;
    }

    public void setAllDocsQuery(String allDocsQuery) {
        this.allDocsQuery = allDocsQuery;
    }

    public boolean isShowEmptyFacets() {
        return showEmptyFacets;
    }

    public void setShowEmptyFacets(boolean showEmptyFacets) {
        this.showEmptyFacets = showEmptyFacets;
    }

    public JSONObject getJSONOutput(NamedList<String> params) throws IOException {
        params = params.clone();
        params.add("wt", "json");
        return JSONObject.fromObject(getStringFromInputStream(doRemoteCall(params)));
    }

    public Document getXMLOutput(NamedList<String> params) throws Exception {
        params = params.clone();
        params.add("wt", "standard");
        DOMParser parser = new DOMParser();
        try {
            parser.parse(new InputSource(doRemoteCall(params)));
        } catch (SAXException se) {
            se.printStackTrace();
            throw se;
        } catch (IOException ioe) {
            ioe.printStackTrace();
            throw ioe;
        }
        return parser.getDocument();
    }

    public InputStream doRemoteCall(NamedList<String> params) throws IOException {
        String protocol = "http";
        String host = getHost();
        int port = Integer.parseInt(getPort());
        StringBuilder sb = new StringBuilder();
        for (Map.Entry entry : params) {
            Object key = entry.getKey();
            Object value = entry.getValue();
            sb.append(key).append("=").append(value).append("&");
        }
        sb.setLength(sb.length() - 1);
        String file = "/" + getUrl() + "/?" + sb.toString();
        URL url = new URL(protocol, host, port, file);
        logger.debug(url.toString());
        InputStream stream;
        HttpURLConnection conn = (HttpURLConnection) url.openConnection();
        try {
            stream = conn.getInputStream();
        } catch (IOException ioe) {
            InputStream is = conn.getErrorStream();
            if (is != null) {
                String msg = getStringFromInputStream(conn.getErrorStream());
                throw new IOException(msg);
            } else {
                throw ioe;
            }
        }
        return stream;
    }
}
