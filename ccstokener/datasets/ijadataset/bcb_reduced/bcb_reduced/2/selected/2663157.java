package project2;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.net.URL;
import java.net.URLConnection;
import java.net.URLEncoder;
import java.util.HashMap;
import java.util.Map;
import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

public class QProbe {

    public static String yahoo_ap_id = "6QhF_yTV34FLYQAciaGIddCeix4zkgmdpuZqsAI0S_vsuygUiLX7FlJ4Tps_y.Sauq4-";

    public String site;

    double tes = 0.6;

    double tec = 100;

    HashMap<String, Double> spec = new HashMap<String, Double>();

    HashMap<String, Double> cov = new HashMap<String, Double>();

    Map<String, QueryInfo> queryInfoMap = new HashMap<String, QueryInfo>();

    JSONArray ja;

    RetrieveDocuments rd = new RetrieveDocuments();

    /** Constructor. */
    public QProbe(String host, double tes, long tec, String appId) throws IOException, JSONException {
        this.site = host;
        this.tes = tes;
        this.tec = tec;
        yahoo_ap_id = appId;
        initialize();
        System.out.println("Classifying...");
        String classify = classify("root");
        printResults();
        System.out.println();
        System.out.println("Classification: " + classify);
        System.out.println("\n \nExtracting topic content summaries...");
        rd.retrieveDocs(host, classify, queryInfoMap);
    }

    /** Initialize the datastructures. */
    private void initialize() {
        spec.put("computers", 0.0);
        spec.put("hardware", 0.0);
        spec.put("programming", 0.0);
        spec.put("sports", 0.0);
        spec.put("basketball", 0.0);
        spec.put("soccer", 0.0);
        spec.put("health", 0.0);
        spec.put("fitness", 0.0);
        spec.put("diseases", 0.0);
        spec.put("root", 0.0);
        cov.put("computers", 0.0);
        cov.put("hardware", 0.0);
        cov.put("programming", 0.0);
        cov.put("sports", 0.0);
        cov.put("basketball", 0.0);
        cov.put("soccer", 0.0);
        cov.put("health", 0.0);
        cov.put("fitness", 0.0);
        cov.put("diseases", 0.0);
        cov.put("root", 0.0);
    }

    /** QProbe recursive algorithm.  */
    private String classify(String classifier) throws IOException, JSONException {
        String result = "";
        if (!Rules.categoryTree.containsKey(classifier)) {
            return classifier;
        }
        calculateCoverageAndSpecificity(classifier);
        for (String subclass : Rules.categoryTree.get(classifier)) {
            if (spec.get(subclass) >= tes && cov.get(subclass) >= tec) {
                result = classifier + "/" + classify(subclass);
            }
        }
        if (result.equals("")) return classifier; else return result;
    }

    /** Queries the Yahoo Boss, fetches and parses the response. */
    private void calculateCoverageAndSpecificity(String mainCat) throws IOException, JSONException {
        for (String cat : Rules.categoryTree.get(mainCat)) {
            for (String queryString : Rules.queries.get(cat)) {
                String urlEncodedQueryString = URLEncoder.encode(queryString, "UTF-8");
                URL url = new URL("http://boss.yahooapis.com/ysearch/web/v1/" + urlEncodedQueryString + "?appid=" + yahoo_ap_id + "&count=4&format=json&sites=" + site);
                URLConnection con = url.openConnection();
                String line;
                StringBuilder builder = new StringBuilder();
                BufferedReader reader = new BufferedReader(new InputStreamReader(con.getInputStream()));
                while ((line = reader.readLine()) != null) {
                    builder.append(line);
                }
                String response = builder.toString();
                JSONObject json = new JSONObject(response);
                JSONObject jsonObject = json.getJSONObject("ysearchresponse");
                String totalhits = jsonObject.getString("totalhits");
                long totalhitsLong = Long.parseLong(totalhits);
                QueryInfo qinfo = new QueryInfo(queryString, totalhitsLong);
                queryInfoMap.put(queryString, qinfo);
                cov.put(cat, cov.get(cat) + totalhitsLong);
                if (totalhitsLong == 0) {
                    continue;
                }
                ja = jsonObject.getJSONArray("resultset_web");
                for (int j = 0; j < ja.length(); j++) {
                    JSONObject k = ja.getJSONObject(j);
                    String dispurl = filterBold(k.getString("url"));
                    qinfo.addUrl(dispurl);
                }
            }
        }
        calculateSpecificity(mainCat);
    }

    /** Calculates the specificity of the given category. */
    private void calculateSpecificity(String maincat) {
        double totalCov = 0;
        for (String cat : Rules.categoryTree.get(maincat)) {
            totalCov += cov.get(cat);
        }
        for (String cat : Rules.categoryTree.get(maincat)) {
            spec.put(cat, cov.get(cat) / totalCov);
        }
    }

    /** Prints the coverage and specificity values. */
    private void printResults() {
        for (String cat : Rules.categoryTree.get("root")) {
            System.out.println("Specificity for category:" + cat + " is " + spec.get(cat));
            System.out.println("Coverage for category:" + cat + " is " + cov.get(cat));
            for (String subCat : Rules.categoryTree.get(cat)) {
                Double double1 = spec.get(subCat);
                if (double1 == 0.0) {
                    continue;
                }
                System.out.println("Specificity for category:" + subCat + " is " + double1);
                System.out.println("Coverage for category:" + subCat + " is " + cov.get(subCat));
            }
        }
    }

    /** Filters out the bold html tags. */
    public static String filterBold(String value) {
        return value.replaceAll("<b>", "").replaceAll("</b>", "").replaceAll("<wbr>", "");
    }

    /** Test. */
    public static void main(String args[]) throws Exception {
        if (args.length < 4) {
            System.out.println("Usage:>QProbe  <host> <t_es> <t_ec> <YAHOO_APP_ID>");
            System.exit(0);
        }
        String host = args[0];
        double tes = Double.parseDouble(args[1]);
        long tec = Long.parseLong(args[2]);
        String appid = args[3];
        new QProbe(host, tes, tec, appid);
    }
}
