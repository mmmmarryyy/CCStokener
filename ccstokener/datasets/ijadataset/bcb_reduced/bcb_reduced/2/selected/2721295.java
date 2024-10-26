package project1;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;
import java.net.URL;
import java.net.URLConnection;
import java.net.URLEncoder;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;

/**
 * Relevance Feedback based search engine which uses Ide-Dec-Hi query expansion
 * and topological ordering methodology.
 */
public class MySearch {

    public static final int COUNT = 10;

    private String yahoo_ap_id = "6QhF_yTV34FLYQAciaGIddCeix4zkgmdpuZqsAI0S_vsuygUiLX7FlJ4Tps_y.Sauq4-";

    String queryString;

    double desiredPrecision;

    int noOfItr = 0;

    ArrayList<String> dictionary = new ArrayList<String>();

    JSONArray ja;

    HashMap<Integer, HashMap<String, Double>> relevant = new HashMap<Integer, HashMap<String, Double>>();

    HashMap<Integer, HashMap<String, Double>> nonRelevant = new HashMap<Integer, HashMap<String, Double>>();

    double[][] weightMatrix;

    double[] consolidatedWeights;

    TermInfo top1NonRelevantTermInfo;

    Top2RelevantTermInfo top2RelevantTermInfo;

    /** Constructor. */
    public MySearch(String queryString, double desiredPrecision, String appId) throws IOException, JSONException {
        this.desiredPrecision = desiredPrecision;
        this.queryString = queryString;
        yahoo_ap_id = appId;
        startProcess();
    }

    /**
	 * Start the process of querying, parsing response, fetching feedback,
	 * processing feedback and creating new query
	 */
    private void startProcess() throws IOException, JSONException {
        clearState();
        double precision = makeQuery(queryString);
        System.out.println("Precision:" + precision + " desired Precision:" + desiredPrecision);
        if (precision == 0) {
            System.out.println("Unable to reach the desired precision because of fewer/no results");
            return;
        }
        if (precision >= desiredPrecision) {
            System.out.println("No of Iterations required to achieve desired precision : " + noOfItr);
            return;
        }
        noOfItr++;
        System.out.println("Indexing...");
        queryExpansionIdeDecHi();
        startProcess();
    }

    /** Clears the state. */
    private void clearState() {
        dictionary.clear();
        relevant.clear();
        nonRelevant.clear();
    }

    /** Queries the Yahoo Boss, fetches and parses the response. */
    private double makeQuery(String query) throws IOException, JSONException {
        double precision = 0;
        int relevantRecordsCount = 0;
        System.out.println("\nQuerying Yahoo for " + query);
        query = URLEncoder.encode(query, "UTF-8");
        URL url = new URL("http://boss.yahooapis.com/ysearch/web/v1/" + query + "?appid=" + yahoo_ap_id + "&count=10&format=json");
        URLConnection con = url.openConnection();
        String line;
        StringBuilder builder = new StringBuilder();
        BufferedReader reader = new BufferedReader(new InputStreamReader(con.getInputStream()));
        while ((line = reader.readLine()) != null) {
            builder.append(line);
        }
        String response = builder.toString();
        JSONObject json = new JSONObject(response);
        System.out.println("\nResults:");
        System.out.println("Total results = " + json.getJSONObject("ysearchresponse").getString("deephits"));
        System.out.println();
        if (Long.parseLong(json.getJSONObject("ysearchresponse").getString("deephits")) < 10) return 0;
        ja = json.getJSONObject("ysearchresponse").getJSONArray("resultset_web");
        System.out.println("\nResults:");
        for (int i = 0; i < ja.length(); i++) {
            System.out.println("Result " + (i + 1) + "\n[ ");
            JSONObject j = ja.getJSONObject(i);
            String title = Utils.filterBold(j.getString("title"));
            System.out.println("Title: " + title);
            String urlPage = Utils.filterBold(j.getString("url"));
            System.out.println("Url: " + urlPage);
            String summary = Utils.filterBold(j.getString("abstract"));
            System.out.println("Summary: " + summary);
            System.out.println("]");
            System.out.println("\nRelevant (Y/N)? ");
            BufferedReader br = new BufferedReader(new InputStreamReader(System.in));
            String userRating = null;
            HashMap<String, Double> documentVector;
            userRating = br.readLine();
            if (userRating.toLowerCase().equals("y")) {
                relevantRecordsCount++;
                documentVector = convertToTermFrequencyVector(j);
                relevant.put(i, documentVector);
            } else if (userRating.toLowerCase().equals("n")) {
                documentVector = convertToTermFrequencyVector(j);
                nonRelevant.put(i, documentVector);
            } else {
                System.out.println("Invalid Relevance Value : Enter Y/N");
            }
        }
        precision = relevantRecordsCount * 0.1;
        return precision;
    }

    /**
	 * Calculates term frequencies.
	 */
    private HashMap<String, Double> convertToTermFrequencyVector(JSONObject j) throws JSONException {
        HashMap<String, Double> docVec = new HashMap<String, Double>();
        String[] term = Utils.filterString(j.getString("abstract"), 0).split(" ");
        for (int i = 0; i < term.length; i++) {
            if (StopWordsFilter.isStopWord(term[i])) {
                continue;
            }
            formDictionary(term[i]);
            if (docVec.containsKey(term[i])) {
                docVec.put(term[i], docVec.get(term[i]) + 1);
            } else {
                docVec.put(term[i], 1.0);
            }
        }
        term = Utils.filterString(j.getString("url"), 0).split(" ");
        for (int i = 0; i < term.length; i++) {
            if (StopWordsFilter.isStopWord(term[i])) {
                continue;
            }
            formDictionary(term[i]);
            if (docVec.containsKey(term[i])) {
                docVec.put(term[i], docVec.get(term[i]) + 1);
            } else {
                docVec.put(term[i], 1.0);
            }
        }
        term = Utils.filterString(j.getString("title")).split(" ");
        for (int i = 0; i < term.length; i++) {
            if (StopWordsFilter.isStopWord(term[i])) {
                continue;
            }
            formDictionary(term[i]);
            if (docVec.containsKey(term[i])) {
                docVec.put(term[i], docVec.get(term[i]) + 1);
            } else {
                docVec.put(term[i], 3.0);
            }
        }
        return docVec;
    }

    /**
	 * Calculates tf-idf of the terms and using ide-dec-hi algorithm appends the
	 * top two favorable words to the query.
	 */
    private void queryExpansionIdeDecHi() throws IOException, JSONException {
        weightMatrix = new double[dictionary.size()][COUNT];
        consolidatedWeights = new double[dictionary.size()];
        for (int i = 0; i < dictionary.size(); i++) {
            for (int j = 0; j < COUNT; j++) {
                weightMatrix[i][j] = getTermFrequencyInDoc(i, j);
            }
        }
        for (int i = 0; i < dictionary.size(); i++) {
            double idf = getInverseDocumentFrequency(i);
            for (int j = 0; j < COUNT; j++) {
                weightMatrix[i][j] *= idf;
            }
        }
        consolidateTermWeights();
        String top1 = dictionary.get(top2RelevantTermInfo.firstTerm.termIndex);
        String top2 = "";
        top2 = " " + dictionary.get(top2RelevantTermInfo.secondTerm.termIndex);
        queryString += " " + top1 + top2;
        int top1Index = top2RelevantTermInfo.firstTerm.termIndex;
        queryString = orderQueryTermsOnTitle(queryString, top1Index);
    }

    /**
	 * Add all tf-idf of terms and evaluate the top 2 relevant terms and top
	 * most non-relevant term.
	 */
    private void consolidateTermWeights() {
        top1NonRelevantTermInfo = getTopNonRelevantTerm();
        top2RelevantTermInfo = new Top2RelevantTermInfo();
        for (int i = 0; i < dictionary.size(); i++) {
            for (Integer j : relevant.keySet()) {
                consolidatedWeights[i] += weightMatrix[i][j];
            }
            if (i == top1NonRelevantTermInfo.termIndex) {
                consolidatedWeights[i] -= top1NonRelevantTermInfo.highestWeight;
            }
            top2RelevantTermInfo.evaluateInfo(i, consolidatedWeights[i]);
        }
    }

    /** Returns the top most non-relevant term information. */
    private TermInfo getTopNonRelevantTerm() {
        TermInfo nterminfo = new TermInfo();
        for (Integer docIndex : nonRelevant.keySet()) {
            for (int i = 0; i < dictionary.size(); i++) {
                double wt = Math.abs(weightMatrix[i][docIndex]);
                if (wt > nterminfo.highestWeight) {
                    nterminfo.highestWeight = wt;
                    nterminfo.termIndex = i;
                }
            }
        }
        return nterminfo;
    }

    /** Computes and returns idf of the given termindex. */
    private double getInverseDocumentFrequency(int termIndex) {
        int df = 0;
        for (int j = 0; j < COUNT; j++) {
            if (Math.abs(weightMatrix[termIndex][j]) > 0) {
                df++;
            }
        }
        return Math.log(COUNT / df);
    }

    /**
	 * Returns the tf of the given termIndex in the document indexed by the
	 * specified docIndex.
	 */
    private double getTermFrequencyInDoc(int termIndex, int docIndex) {
        boolean isRelevant = true;
        HashMap<String, Double> docVector = relevant.get(docIndex);
        if (docVector == null) {
            docVector = nonRelevant.get(docIndex);
            isRelevant = false;
        }
        String term = dictionary.get(termIndex);
        Double freq = docVector.get(term);
        if (freq == null) {
            freq = 0.0;
        }
        if (!isRelevant) {
            freq = freq * -1;
        }
        return freq;
    }

    /** Add the str to the dictionary. */
    private int formDictionary(String str) {
        if (!dictionary.contains(str)) {
            dictionary.add(str);
        }
        return dictionary.size() - 1;
    }

    private String orderQueryTermsOnTitle(String queryString, int top1Index) throws JSONException {
        String[] qArray = queryString.split(" ");
        String titleString = getTitle(getDocIndexWithHighestTermWeight(top1Index));
        String[] title = titleString.split(" ");
        if (qArray == null || title == null || qArray.length == 0 || title.length == 0) {
            return null;
        }
        List<String> queryList = new ArrayList<String>();
        List<String> titleList = new ArrayList<String>();
        for (String q : qArray) {
            queryList.add(q);
        }
        for (String q : title) {
            if (!queryList.contains(q)) {
                continue;
            }
            titleList.add(q);
            queryList.remove(q);
        }
        for (String q : queryList) {
            titleList.add(q);
        }
        String newq = "";
        for (String q : titleList) {
            newq += q + " ";
        }
        return newq.trim();
    }

    /** Returns the document with the highest termIndex weight. */
    private int getDocIndexWithHighestTermWeight(int termIndex) {
        double highWt = 0;
        int docIndex = -1;
        for (int i = 0; i < COUNT; i++) {
            if (weightMatrix[termIndex][i] > highWt) {
                highWt = weightMatrix[termIndex][i];
                docIndex = i;
            }
        }
        return docIndex;
    }

    /** Returns the title of the specified doc. */
    private String getTitle(int docIndex) throws JSONException {
        JSONObject js = ja.getJSONObject(docIndex);
        return Utils.filterString(js.getString("title"));
    }

    /** Returns true if the docIndex is relevant. */
    private boolean isRelevantDoc(int docIndex) {
        return relevant.containsKey(docIndex);
    }

    /** Prints the tf-idf matrix. */
    private void printMatrix() {
        for (int i = 0; i < dictionary.size(); i++) {
            for (int j = 0; j < COUNT; j++) {
                System.out.print(weightMatrix[i][j] + " ");
            }
            System.out.println(dictionary.get(i));
        }
    }

    /** Prints the relevant and non-relevant docs. */
    private void printRelevantAndNonRelevantDocs() {
        System.out.println("relevant....");
        for (Integer i : relevant.keySet()) {
            System.out.println(i);
        }
        System.out.println("non relevant...");
        for (Integer i : nonRelevant.keySet()) {
            System.out.println(i);
        }
    }

    /** Test. */
    public static void main(String args[]) throws Exception {
        if (args.length < 3) {
            System.out.println("Usage:>MySearch <query> <precision> <yahoo appId>");
            System.exit(0);
        }
        String query = args[0];
        for (int i = 1; i < args.length - 2; i++) {
            query += " " + args[i];
        }
        double precision = Double.parseDouble(args[args.length - 2]);
        String appId = args[args.length - 1];
        new MySearch(query, precision, appId);
    }
}
