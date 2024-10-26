package sk.sigp.tetras.crawl.parser;

import java.io.IOException;
import java.net.URISyntaxException;
import java.util.ArrayList;
import java.util.List;
import java.util.StringTokenizer;
import org.apache.commons.io.IOUtils;
import org.apache.commons.lang.StringEscapeUtils;
import org.apache.http.HttpEntity;
import org.apache.http.HttpException;
import org.apache.http.HttpResponse;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.client.params.ClientPNames;
import org.apache.http.impl.client.DefaultHttpClient;
import org.apache.log4j.Logger;
import sk.sigp.tetras.crawl.client.CrawlerHttpClient;
import sk.sigp.tetras.dao.TrafficDao;
import sk.sigp.tetras.entity.Firma;
import sk.sigp.tetras.entity.VyhladavaciAlgoritmus;
import sk.sigp.tetras.service.PreferenceService;

public abstract class CompanyParser {

    public abstract Logger getLogger();

    public abstract String convertCategoryName(Integer numberx);

    private IParserAdapter adapter = null;

    private TrafficDao trafficDao;

    private String baseUrl;

    private int nowIterating = 0;

    public String getBaseUrl() {
        return baseUrl;
    }

    public abstract String[] getCategories();

    /**
	 * sets baseurl or parser
	 * 
	 * @param adapter
	 */
    public void setBaseUrl(String baseUrl) {
        this.baseUrl = baseUrl;
    }

    /**
	 * sets adapter for listening async events published by parser
	 * 
	 * @param adapter
	 */
    public void setAdapter(IParserAdapter adapter) {
        this.adapter = adapter;
    }

    public IParserAdapter getAdapter() {
        return adapter;
    }

    /**
	 * will return companies per page
	 * 
	 * @return
	 */
    public abstract int getCompaniesPerPage();

    private PreferenceService preferenceService;

    public PreferenceService getPreferenceService() {
        return preferenceService;
    }

    public void setPreferenceService(PreferenceService preferenceService) {
        this.preferenceService = preferenceService;
    }

    /**
	 * will return head code for parsing companies
	 * @return
	 */
    protected abstract String getHeadCode();

    /**
	 * will parse fetched content into companies, internal method, cycling is everywhere same
	 * 
	 * @param pageDump
	 * @return
	 */
    protected abstract int parseFeaturedCompany(String pageDump, int index, Firma company) throws IllegalStateException, IOException, HttpException, InterruptedException, URISyntaxException;

    public static final int CYCLE_PROOF_LIMIT = 60;

    protected List<Firma> parseContent(String pageDump) throws IllegalStateException, IOException, HttpException, InterruptedException, URISyntaxException {
        List<Firma> result = new ArrayList<Firma>();
        int index = 0;
        String headCode = getHeadCode();
        int xa = 0;
        while (pageDump.indexOf(headCode, index) != -1) {
            xa++;
            if (xa > CYCLE_PROOF_LIMIT) {
                getLogger().warn("Theres probably problem with parser and it needs service");
                break;
            }
            Firma company = new Firma();
            index = pageDump.indexOf(headCode, index);
            index = parseFeaturedCompany(pageDump, index, company);
            if (precheckForStabilization(company)) result.add(company);
        }
        return result;
    }

    /**
	 * will precheck for stabilization
	 * @param company
	 * @return
	 */
    protected abstract boolean precheckForStabilization(Firma company);

    /**
	 * builds link with categories parsed from category page and parameters
	 * provided as parameters of this method
	 * http://www.europages.cz/seznam-spolecnosti/did-14/cc-CHE/pg-40/vysledky.html
	 * 
	 * @param type
	 * 
	 * @param country
	 * @param categories
	 * @param offsetNo
	 * @return
	 */
    protected abstract String buildLinkWithOffsetNo(String type, String country, long offsetNo);

    /**
	 * fetches page from url (stupid version)
	 * 
	 * @param url
	 * @return
	 * @throws IllegalStateException
	 * @throws IOException
	 * @throws HttpException
	 * @throws InterruptedException
	 * @throws URISyntaxException
	 */
    protected String httpToStringStupid(String url) throws IllegalStateException, IOException, HttpException, InterruptedException, URISyntaxException {
        String result = httpToStringStupid(url, "UTF-8");
        return result;
    }

    /**
	 * fetches page from url (stupid version)
	 * 
	 * @param url
	 * @return
	 * @throws IllegalStateException
	 * @throws IOException
	 * @throws HttpException
	 * @throws InterruptedException
	 * @throws URISyntaxException
	 */
    protected String httpToStringStupid(String url, String encoding) throws IllegalStateException, IOException, HttpException, InterruptedException, URISyntaxException {
        DefaultHttpClient httpclient = new CrawlerHttpClient(getPreferenceService());
        httpclient.getParams().setParameter(ClientPNames.COOKIE_POLICY, org.apache.http.client.params.CookiePolicy.BROWSER_COMPATIBILITY);
        getLogger().debug("url fetch: " + url);
        HttpGet httpget = new HttpGet(url);
        HttpResponse response = httpclient.execute(httpget);
        HttpEntity entity = response.getEntity();
        String pageDump = IOUtils.toString(entity.getContent(), encoding);
        return pageDump;
    }

    /**
	 * strips HTML tags from source
	 * 
	 * @param source
	 * @return
	 */
    protected String stripTags(String source) {
        String sourcex = "";
        for (int i = 0; i < source.length(); i++) if (source.charAt(i) != '|') sourcex += source.charAt(i);
        return sourcex.replaceAll("\\<.*?>", "");
    }

    protected String revertString(String source) {
        String result = "";
        for (int i = 0; i < source.length(); i++) result += source.charAt(source.length() - i - 1);
        return result;
    }

    /**
	 * returns type string for generating type name in url from category type
	 * 
	 * @param type
	 * @return
	 */
    public String makeTypeString(String type) {
        String result = "";
        boolean learn = true;
        for (int i = 0; i < type.length(); i++) {
            char ch = type.charAt(i);
            if (Character.isLetter(ch)) learn = true; else {
                if (learn) result += "-";
                learn = false;
            }
            if (learn) result += ch;
        }
        if (result.charAt(result.length() - 1) == '-') result = result.substring(0, result.length() - 1);
        return result.toLowerCase();
    }

    /**
	 * fetches companies asynchronously
	 * 
	 * @param type
	 * @param page
	 * @param callbackId
	 */
    public void asynchronousFetch(final String type, final long page, final long callbackId) {
        Thread tx = new Thread(new Runnable() {

            public void run() {
                try {
                    List<Firma> companies = synchronousFetch(type, page);
                    if (adapter != null) adapter.parserAsyncReturnPoint(companies, callbackId);
                } catch (Exception e) {
                    getLogger().error(e);
                    if (adapter != null) adapter.parserAsyncFailNotification(callbackId, e);
                }
            }
        });
        tx.setName("Company parser thread " + getBaseUrl());
        tx.setPriority(Thread.MIN_PRIORITY);
        tx.start();
    }

    /**
	 * synchronously fetches companies
	 * 
	 * @param writtenType
	 * @param type
	 * @param country
	 * @param amount
	 * @return
	 * @throws Exception
	 */
    public List<Firma> synchronousFetch(String type, long page) throws IllegalStateException, IOException, HttpException, InterruptedException, URISyntaxException {
        getLogger().debug("Initializing fetch");
        List<Firma> result = new ArrayList<Firma>();
        result.addAll(synchronousFetchBase(type, null, page));
        getLogger().debug("Parsing all done");
        return result;
    }

    /**
	 * synchronously fetches companies
	 * 
	 * @param writtenType
	 * @param type
	 * @param country
	 * @param amount
	 * @return
	 * @throws Exception
	 */
    public List<Firma> synchronousFetchBase(String type, String country, long no) throws IllegalStateException, IOException, HttpException, InterruptedException, URISyntaxException {
        List<Firma> result = new ArrayList<Firma>();
        String url = buildLinkWithOffsetNo(type, country, no);
        getLogger().info("Fetching url: " + url);
        String pageDump = httpToStringStupid(url);
        List<Firma> companies = parseContent(pageDump);
        result.addAll(companies);
        getLogger().debug("Parsed " + companies.size() + " in sync fetch base");
        return result;
    }

    protected abstract Long countFoundCompanies(String html);

    public Long countCompaniesInCateg(String categ) throws Exception {
        String url = buildLinkWithOffsetNo(categ, null, 1);
        getLogger().info("Fetching content of category " + url);
        String pageDump = httpToStringStupid(url);
        return countFoundCompanies(pageDump);
    }

    /**
	 * returns begin index of element
	 * 
	 * @param pageDump
	 * @param code
	 * @param fromIndex
	 * @return
	 */
    protected int getTmpIndex(String pageDump, String code, int fromIndex) {
        return pageDump.indexOf(code, fromIndex) + code.length();
    }

    protected int getTmpIndexSafe(String pageDump, String code, int fromIndex) {
        int x = pageDump.indexOf(code, fromIndex);
        if (x == -1) return -1;
        return x + code.length();
    }

    /**
	 * returns element
	 * 
	 * @param pageDump
	 * @param trailCode
	 * @param index
	 * @return
	 */
    protected String getElement(String pageDump, String trailCode, int index) {
        return convertString(pageDump.substring(index, index + (pageDump.indexOf(trailCode, index) - index))).trim();
    }

    /**
	 * transforms comma separated value string into list of string
	 */
    protected List<String> comaSepataredValuesToListOfString(String values) {
        List<String> result = new ArrayList<String>();
        StringTokenizer st = new StringTokenizer(values, ",", false);
        while (st.hasMoreTokens()) result.add(st.nextToken().trim());
        return result;
    }

    /**
	 * returns true if passed character is splitter for PSC
	 * 
	 * @param c
	 * @return
	 */
    private boolean isSplitterForPsc(char c) {
        switch(c) {
            case ' ':
                return true;
            case '-':
                return true;
            case ':':
                return true;
            default:
                return false;
        }
    }

    /**
	 * method will parse PSC out of address string
	 * 
	 * @param address
	 * @param company
	 */
    protected void parsePscOutOfAddress(String address, Firma company) {
        List<String> numbers = new ArrayList<String>();
        List<String> allnumbers = new ArrayList<String>();
        List<String> all = new ArrayList<String>();
        String tmp = "";
        boolean inside = false;
        boolean containsDigit = false;
        boolean noDigit = false;
        for (int i = 0; i < address.length(); i++) {
            char c = address.charAt(i);
            if (!isSplitterForPsc(c)) {
                if (inside) tmp += c; else {
                    tmp += c;
                    inside = true;
                }
                if (Character.isDigit(c)) containsDigit = true; else noDigit = true;
            } else {
                if (inside) {
                    if (tmp.length() > 3 && tmp.length() < 6 && (!noDigit)) numbers.add(tmp);
                    if (tmp.length() > 1 && (!noDigit)) allnumbers.add(tmp);
                    if (tmp.length() > 1 && containsDigit) all.add(tmp);
                    tmp = "";
                    inside = false;
                    containsDigit = false;
                    noDigit = false;
                }
            }
        }
        if (numbers.size() > 0) company.getFirmaData().setPsc(numbers.get(numbers.size() - 1)); else if (allnumbers.size() >= 2) company.getFirmaData().setPsc(allnumbers.get(allnumbers.size() - 2) + allnumbers.get(allnumbers.size() - 1)); else if (all.size() >= 2) {
            company.getFirmaData().setPsc(all.get(all.size() - 2) + all.get(all.size() - 1));
        }
    }

    /**
	 * delete obsolete chars from desired string
	 * 
	 * @param str
	 * @return
	 */
    protected static String convertString(String str) {
        String res = str.replaceAll("\\<.*?>", "");
        res = res.replaceAll("&nbsp;", " ");
        res = res.replaceAll("\r\n", " ");
        res = res.replaceAll("\n", " ");
        res = res.replaceAll("\t", " ");
        return res;
    }

    public static void main(String[] args) {
    }

    public static final String unescapeHTML(String source) {
        return StringEscapeUtils.unescapeHtml(source);
    }

    public static final String escapeHTML(String source) {
        return StringEscapeUtils.escapeHtml(source);
    }

    public int getNowIterating() {
        return nowIterating;
    }

    public void setNowIterating(int nowIterating) {
        this.nowIterating = nowIterating;
    }

    /**
	 * will return if algorithm should iterate, 
	 * depending on everyNthIteration databse setted property for each single algorithm
	 * @return
	 */
    public boolean shouldIterate(VyhladavaciAlgoritmus algorithm) {
        nowIterating++;
        if (nowIterating >= algorithm.getEveryNthIteration()) {
            nowIterating = 0;
            return true;
        }
        return false;
    }

    public TrafficDao getTrafficDao() {
        return trafficDao;
    }

    public void setTrafficDao(TrafficDao trafficDao) {
        this.trafficDao = trafficDao;
    }
}
