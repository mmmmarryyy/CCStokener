package pubfetch;

import java.util.*;
import java.io.*;
import java.net.*;
import java.util.regex.*;

/**
 * Fetches Agricola documents in MEDLINE like format.
 */
public class AgricolaFetcher implements Fetcher {

    private Date mindate;

    private Date maxdate;

    private String searchfor;

    private int resultSize = 0;

    private List documents;

    private int sessionId;

    private int batchSize;

    /** Class constructor */
    public AgricolaFetcher() {
        this.documents = new ArrayList();
        this.batchSize = 50;
    }

    /**
     * The method to set the query.
     *
     * @param queryFor the query
     */
    public void setQueryFor(String queryFor) {
        try {
            searchfor = java.net.URLEncoder.encode(queryFor, "UTF-8");
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    /**
     * The method to set the minimum date
     *
     * @param date the date in the form "yyyy/MM/dd".
     */
    public void setMinDate(String date) {
        mindate = DateParser.parse(date);
    }

    /**
     * The method to set the maximum date
     *
     * @param date the date in the form "yyyy/MM/dd".
     */
    public void setMaxDate(String date) {
        maxdate = DateParser.parse(date);
    }

    /**
     * The method to excecute the query on Agricola Database.
     */
    public void doQuery() throws FetchError {
        try {
            populateSessionId();
            populateResultSize();
            populateDocuments();
        } catch (Exception e) {
            throw new FetchError(e);
        }
    }

    /**
     * The method to restrict documents based on date.
     */
    private String makeDateRestriction() throws NumberFormatException {
        final int EPOCH = 1900;
        int minyear = mindate.getYear() + EPOCH;
        int maxyear = maxdate.getYear() + EPOCH;
        String url_date_part = null;
        String dateQulifier = "E";
        if (minyear != maxyear) {
            dateQulifier = "R";
            url_date_part = "&DTBL=" + dateQulifier + "&DATE=" + minyear + "&DATE2=" + maxyear;
        } else {
            url_date_part = "&DTBL=" + dateQulifier + "&DATE=" + minyear;
        }
        return url_date_part;
    }

    private void populateSessionId() throws IOException, java.net.MalformedURLException {
        String general_search_url = "http://agricola.nal.usda.gov/cgi-bin/Pwebrecon.cgi?" + "DB=local&CNT=1&Search_Arg=RNAi&Search_Code=GKEY&STARTDB=AGRIDB";
        String sidString = "", inputLine;
        BufferedReader in = new BufferedReader(new InputStreamReader((new URL(general_search_url)).openStream()));
        while ((inputLine = in.readLine()) != null) {
            if (inputLine.startsWith("<INPUT TYPE=HIDDEN NAME=PID VALUE=")) {
                sidString = (inputLine.substring(inputLine.indexOf("PID VALUE=") + 11, inputLine.indexOf(">") - 1));
            }
        }
        sessionId = Integer.parseInt(sidString.trim());
    }

    /**
     * The method to get result size.
     */
    private void populateResultSize() throws IOException, FetchError {
        BufferedReader in = openSearchForResultSize();
        searchForResultSize(in);
    }

    /**
     * The method to get the input stream
     *
     * @return BufferedReader 
     */
    private BufferedReader openSearchForResultSize() throws IOException {
        String date_part = makeDateRestriction();
        String agricola_base_url = "http://agricola.nal.usda.gov/cgi-bin/Pwebrecon.cgi?BOOL1=any+of+these&" + "FLD1=Keyword+Anywhere+%28GKEY%29&GRP1=AND+with+next+set";
        String query_url = agricola_base_url + "&SAB1=" + searchfor + date_part + "&PID=" + sessionId;
        BufferedReader in = new BufferedReader(new InputStreamReader((new URL(query_url)).openStream()));
        return in;
    }

    /**
     * The method to get the result size from the input stream.
     *
     * @param in the input stream BufferedReader.
     */
    private void searchForResultSize(BufferedReader in) throws IOException, FetchError {
        Pattern pattern = Pattern.compile("search results: displaying \\d+ through \\d+ of (\\d+) entries", Pattern.CASE_INSENSITIVE);
        boolean patternFound = false;
        String inputLine;
        while ((inputLine = in.readLine()) != null) {
            Matcher matcher = pattern.matcher(inputLine);
            if (matcher.find()) {
                resultSize = Integer.parseInt(matcher.group(1));
                patternFound = true;
                break;
            }
        }
        if (!patternFound) {
            throw new FetchError("result size cound not be found");
        }
    }

    private void populateDocuments() throws IOException, FetchError {
        String quoted_query = java.net.URLEncoder.encode(searchfor, "UTF-8");
        try {
            for (int i = 0; i < resultSize; i = i + batchSize) {
                String url = "http://agricola.nal.usda.gov/cgi-bin/Pwebrecon.cgi?" + "SAB1=" + quoted_query + "&BOOL1=all+of+these&FLD1=Keyword+Anywhere+%28GKEY%29&GRP1=AND+with+next+set" + "&PID=" + sessionId + "&CNT=" + batchSize + "&HIST=1";
                if (i > 0) {
                    url += ("&ti=" + (i + 1) + ",0");
                }
                fetchBatchAgricolaDocuments(url);
            }
        } catch (Exception e) {
            System.out.println(e);
            throw new FetchError(e);
        }
    }

    /**
     * The method to fetch agricola records in batches.
     *
     * @param url The agricola URL containing batch size and query.
     */
    private void fetchBatchAgricolaDocuments(String url) throws FetchError {
        List hids = parseUrlGetUids(url);
        StringBuffer marcRecordUrl = new StringBuffer(url);
        for (int i = 0; i < hids.size(); i++) {
            marcRecordUrl.append("&HID=" + hids.get(i));
        }
        marcRecordUrl.append("&REC=1&RD=3&SAVE=Format+for+Print+or+Save&RC=1&LIMITBUTTON=0");
        try {
            InputStream is = (new URL(marcRecordUrl.toString())).openStream();
            extractMapsFromInputStream(is);
        } catch (Exception e) {
            throw new FetchError(e);
        }
    }

    /**
     * The method to filter records based on date.
     *
     * @param records A list of records.
     *
     * @return A list of records that falls in the date range.
     */
    private List filterWithDate(List records) {
        List filteredResults = new ArrayList();
        for (int i = 0; i < records.size(); i++) {
            MedlineMap medlineMap = (MedlineMap) records.get(i);
            if (medlineMap.getDate() == null) {
                filteredResults.add(medlineMap);
            } else {
                if (dateBetween(medlineMap.getDate())) {
                    filteredResults.add(medlineMap);
                }
            }
        }
        return filteredResults;
    }

    /**
     * The method to find if a date (thisDate) is between two date periods (min & max).
     * @return <code>true</code> if (min <= thisDate <= max)
     */
    private boolean dateBetween(Date thisDate) {
        boolean afterMin = true;
        boolean beforeMax = true;
        if (mindate != null) {
            afterMin = (mindate.compareTo(thisDate) <= 0);
        }
        if (maxdate != null) {
            beforeMax = (thisDate.compareTo(maxdate) <= 0);
        }
        return afterMin && beforeMax;
    }

    /**
     * The method to get HIDs (Hidden IDs) for each article in a page.
     *
     * @param url the URL for agricola query.
     *
     * @return hids. The Hidden IDs as a list.
     */
    private List parseUrlGetUids(String url) throws FetchError {
        List hids = new ArrayList();
        try {
            InputStream is = (new URL(url)).openStream();
            BufferedReader in = new BufferedReader(new InputStreamReader(is));
            StringBuffer buffer = new StringBuffer();
            String inputLine = "";
            Pattern pattern = Pattern.compile("\\<input\\s+type=hidden\\s+name=hid\\s+value=(\\d+)\\s?\\>", Pattern.CASE_INSENSITIVE);
            while ((inputLine = in.readLine()) != null) {
                Matcher matcher = pattern.matcher(inputLine);
                if (matcher.find()) {
                    String id = matcher.group(1);
                    if (!hids.contains(id)) {
                        hids.add(id);
                    }
                }
            }
        } catch (Exception e) {
            System.out.println(e);
            throw new FetchError(e);
        }
        return hids;
    }

    /**
     * The method to extract MEDLINE map form the inputstream.
     * and add them to ducuments collection
     *
     * @param is the InputStream.
     */
    public void extractMapsFromInputStream(InputStream is) throws FetchError {
        List maps = new ArrayList();
        try {
            MarcToMedline marctoMed = new MarcToMedline();
            marctoMed.parse(is);
            maps = marctoMed.getMaps();
            for (int i = 0; i < maps.size(); i++) {
                normalizeMedlineMap((MedlineMap) maps.get(i));
            }
        } catch (IOException e) {
            throw new FetchError(e);
        }
        List filteredRecords = maps;
        if (mindate != null && maxdate != null) {
            filteredRecords = filterWithDate(maps);
        }
        this.documents.addAll(filteredRecords);
    }

    /**
     * The method to normalize MEDLINE map for ISSN and PG. 
     * Note: PG contains only pageStart.
     * @param map MedlineMap
     *
     */
    private void normalizeMedlineMap(MedlineMap map) {
        if (!StringUtils.isEmpty(map.getFirst("SO"))) {
            Pattern p = Pattern.compile("\\|x\\s(\\w+-\\w+)\\s");
            Matcher m = p.matcher(map.getFirst("SO"));
            if (m.find()) {
                map.set("IS", m.group(1));
            }
        }
        if (!StringUtils.isEmpty(map.getFirst("PG"))) {
            Pattern p = Pattern.compile("^p\\.\\s+(\\w{0,3}\\d+)[\\s|\\-]?", Pattern.CASE_INSENSITIVE);
            Matcher m = p.matcher(map.getFirst("PG"));
            if (m.find()) {
                map.set("PG", m.group(1));
            } else {
            }
        }
    }

    /**
     * This method returns a list of documents.
     *
     * @return A list of documents.
     */
    public List documents() {
        return documents;
    }

    /**
     * This method returns the result size for the query.
     *
     * @return Result size in int.
     */
    public int total() {
        return documents.size();
    }
}
