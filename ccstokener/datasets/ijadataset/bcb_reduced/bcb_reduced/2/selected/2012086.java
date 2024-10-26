package pubfetch;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.net.URL;
import java.util.ArrayList;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import org.xml.sax.XMLReader;

/**
 * Fetches PubMed documents in MEDLINE Display format.
 */
public class PubmedFetcher implements Fetcher {

    private String pubmed_link_url = "http://eutils.ncbi.nlm.nih.gov/entrez/eutils/elink.fcgi?dbfrom=pubmed&tool=pubfetch&email=vnarayan@mcw.edu";

    private String pubmed_search_url = "http://eutils.ncbi.nlm.nih.gov/entrez/eutils/esearch.fcgi?db=pubmed&tool=pubfetch&email=vnarayan@mcw.edu";

    private String pubmed_fetch_url = "http://eutils.ncbi.nlm.nih.gov/entrez/eutils/efetch.fcgi?db=pubmed&tool=pubfetch&email=vnarayan@mcw.edu";

    private String pmc_url = "http://www.pubmedcentral.nih.gov/articlerender.fcgi?tool=pubmed";

    private String pubmed_pmc_url = "http://eutils.ncbi.nlm.nih.gov/entrez/eutils/esearch.fcgi?db=pmc&tool=pubfetch&email=vnarayan@mcw.edu";

    private String mindate;

    private String maxdate;

    private String searchfor;

    private ArrayList documents;

    /** Class constructor */
    public PubmedFetcher() {
        this.documents = new ArrayList();
    }

    /**
     * The method to set the query.
     *
     * @param queryfor the query
     */
    public void setQueryFor(String queryfor) {
        searchfor = queryfor;
    }

    /**
     * The method to set the minimum date
     *
     * @param date the date in the form "yyyy/MM/dd".
     */
    public void setMinDate(String date) {
        this.mindate = date;
    }

    /**
     * The method to set the maximum date
     *
     * @param date the date in the form "yyyy/MM/dd".
     */
    public void setMaxDate(String date) {
        this.maxdate = date;
    }

    /**
     * The method to get the handler of the EutilParser
     */
    public EutilParser getHandler(String url) throws Exception {
        EutilParser handler = new EutilParser();
        XMLReader xr = new org.apache.xerces.parsers.SAXParser();
        xr.setContentHandler(handler);
        xr.setErrorHandler(handler);
        xr.parse(url);
        return handler;
    }

    /**
     * Returns number of records available in PubMed database for a specific query.
     * 
     * @return The number of records as integer.
     */
    public int total() {
        try {
            String url = constructSearchUrl(1);
            String total = "";
            EutilParser handler = getHandler(url);
            total = handler.getCount();
            return Integer.parseInt(total);
        } catch (Exception e) {
            System.out.println("result size cound not be found from pubmed");
        }
        return 0;
    }

    /**
        Returns the pubmed search url.
      */
    private String constructSearchUrl(int retmax) {
        String url = pubmed_search_url + "&term=" + urlQuote(searchfor) + "&retmax=" + retmax + "&mindate=" + urlQuote(mindate) + "&maxdate=" + urlQuote(maxdate);
        Log.getLogger(this.getClass()).debug("pubmedURL: " + url);
        return url;
    }

    /** Small helper function to quote URL values. */
    private String urlQuote(String value) {
        try {
            return java.net.URLEncoder.encode(value, "UTF-8");
        } catch (java.io.UnsupportedEncodingException e) {
            throw new RuntimeException(e);
        }
    }

    /**
     * Returns true if the search on PubMed returns records. 
     * Returns false if the query does not return any records (returns error message).
     * 
     * @return true or false
     */
    public boolean fetchCheck() {
        boolean check = false;
        try {
            String url = constructSearchUrl(1);
            EutilParser handler = getHandler(url);
            check = handler.hasDocuments();
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
        return check;
    }

    /**
     * Returns a list of PMIDs that succesfully match the
     * query 
     *  
     * @return A list of PubMed Identifiers (PMIDs).
     */
    public List fetchPMID() {
        int total = total();
        List pubmed_ids = new ArrayList();
        try {
            String url = constructSearchUrl(total);
            EutilParser handler = getHandler(url);
            pubmed_ids = handler.getID();
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
        return pubmed_ids;
    }

    /**
     * The method to excecute the query on PubMed Database.
     */
    public void doQuery() throws FetchError {
        List pmids = fetchPMID();
        int BATCHSIZE = 900;
        long SLEEPDELAY = 3000;
        try {
            for (int i = 0; i < pmids.size(); i += BATCHSIZE) {
                int start = i;
                int stop = Math.min(i + BATCHSIZE, pmids.size());
                fetchBatchPubmedDocuments(pmids.subList(start, stop));
            }
        } catch (Exception e) {
            System.out.println(e);
            Log.getLogger(this.getClass()).warn(e);
            throw new FetchError(e);
        }
    }

    /** Given a list of PMIDs, returns documents as a list.  Each
     * document is a separate String in MEDLINE Display format.
     * @return A list of PubMed Documents in MEDLINE Display format.
     */
    public List documents() {
        return this.documents;
    }

    /**
     * The method to fetch pubmed documents.
     *
     * @param ids A list of pubmed ids. Maximum number of ids that can be queried at a time is 900.
     *
     */
    private void fetchBatchPubmedDocuments(List ids) throws FetchError {
        String url = pubmed_fetch_url + "&id=" + join(",", ids) + "&retmode=MEDLINE&rettype=MEDLINE";
        Log.getLogger(this.getClass()).debug(url);
        try {
            InputStream is = (new URL(url)).openStream();
            extractMapsFromInputStream(is);
        } catch (Exception e) {
            throw new FetchError(e);
        }
    }

    /**
     * The method to extract Medline Maps from the InputStream containing Medline Format record.
     *
     * @param is InputStream with Medline format records.
     */
    public void extractMapsFromInputStream(InputStream is) throws FetchError {
        try {
            MedlineReader reader = new MedlineReader();
            BufferedReader in = new BufferedReader(new InputStreamReader(is));
            reader.parse(in);
            List records = reader.recordsAsList();
            Log.getLogger(this.getClass()).debug("records size : " + records.size());
            for (int i = 0; i < records.size(); i++) {
                normalizeMedlineMap((MedlineMap) records.get(i));
            }
            praseFullTextURLs(records);
            this.documents.addAll(records);
        } catch (IOException e) {
            throw new FetchError(e);
        }
    }

    /** XML PARSING of E-Link output to get the full -text URL
     * TODO: try Batch processing of XML with upto 900 PMIDs at a time
     * Decide on subscription required cases (do we need URL even if subscription is required?)
     */
    public void praseFullTextURLs(List records) {
        try {
            List pmids = new ArrayList();
            for (int i = 0; i < records.size(); i++) {
                String pmid = (((MedlineMap) records.get(i)).getFirst("PMID"));
                String issn = (((MedlineMap) records.get(i)).getFirst("IS"));
                String doi = (((MedlineMap) records.get(i)).getDoi());
                FullTextURLResolver resolver = new FullTextURLResolver(pmid, issn, doi);
                String fullText = resolver.resolve();
                ((MedlineMap) records.get(i)).set("URLF", fullText);
            }
        } catch (Exception e) {
            System.out.println(e);
        }
    }

    /**
     * Joins a list of strings together using a delimiter 'delim'.
     * @param delim The desired delimiter e.g. ','.
     * @param v The list of strings to be joined e.g. List of PMIDs.
     * @return The joined string. e.g. 98745,8942,20598,...
     */
    private static String join(String delim, java.util.List v) {
        if (v.size() == 0) {
            return "";
        }
        if (v.size() == 1) {
            return "" + v.get(0);
        }
        StringBuffer result = new StringBuffer("" + v.get(0));
        for (int i = 1; i < v.size(); i++) {
            result.append(delim);
            result.append("" + v.get(i));
        }
        return result.toString();
    }

    /**
     * The method to normalize a Medline map for PG tag such that PG tag conatins only page start.
     *
     * @param map A medline map.
     */
    private void normalizeMedlineMap(MedlineMap map) {
        if (!StringUtils.isEmpty(map.getFirst("PG"))) {
            Pattern p = Pattern.compile("\\s?(\\d+)-(\\d+)\\s?");
            Matcher m = p.matcher(map.getFirst("PG"));
            if (m.find()) {
                map.set("PG", m.group(1));
            }
        }
    }
}
