package util.webSearch;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.Reader;
import java.io.StringWriter;
import java.io.UnsupportedEncodingException;
import java.io.Writer;
import java.net.URISyntaxException;
import java.net.URL;
import java.net.URLConnection;
import java.net.URLEncoder;
import java.util.HashSet;
import java.util.LinkedList;
import org.apache.commons.httpclient.HttpClient;
import org.apache.commons.httpclient.HttpException;
import org.apache.commons.httpclient.URI;
import org.apache.commons.httpclient.methods.GetMethod;
import org.apache.commons.httpclient.params.HttpMethodParams;
import org.json.JSONArray;
import org.json.JSONObject;
import util.io.URLInput;
import util.parser.JsonParser;
import util.parser.ParseGoogleSuggestions;

public class GoogleQuery {

    private static final String HTTP_REFERER = "http://www.example11.com/";

    static HttpClient client = new HttpClient();

    public static LinkedList<WebResult> googleQuery(String query, int results) {
        int r = results;
        LinkedList<WebResult> res = new LinkedList<WebResult>();
        int index = 0;
        while (r >= 0) {
            makeQuery(query, index, results, res);
            index += 8;
            r -= 8;
        }
        return res;
    }

    public static LinkedList<String> getRelatedSearches(String query, String path, long delay) throws UnsupportedEncodingException {
        String url = "http://google.com/complete/search?q=" + URLEncoder.encode(query, "utf8") + "&output=toolbar";
        LinkedList<String> set = new LinkedList<String>();
        String post = null;
        try {
            Thread.sleep(delay);
            post = URLInput.submitURLRequest(url, path);
        } catch (HttpException e) {
            e.printStackTrace();
        } catch (IOException e) {
            e.printStackTrace();
        } catch (InterruptedException e) {
            e.printStackTrace();
        } catch (org.apache.http.HttpException e) {
            e.printStackTrace();
        } catch (URISyntaxException e) {
            e.printStackTrace();
        }
        System.out.println(post);
        set = ParseGoogleSuggestions.suggestions(query, post);
        return set;
    }

    public static LinkedList<String> getRelatedSearchesUDF(String query, String path, long delay) throws UnsupportedEncodingException, org.apache.http.HttpException {
        String url = "http://google.com/complete/search?q=" + URLEncoder.encode(query, "utf8") + "&output=toolbar";
        LinkedList<String> set = new LinkedList<String>();
        String post = null;
        try {
            Thread.sleep(delay);
            post = URLInput.submitURLRequestUDF(url, path);
        } catch (HttpException e) {
            e.printStackTrace();
        } catch (IOException e) {
            e.printStackTrace();
        } catch (InterruptedException e) {
            e.printStackTrace();
        } catch (URISyntaxException e) {
            e.printStackTrace();
        }
        set = ParseGoogleSuggestions.suggestions(query, post);
        return set;
    }

    private static void makeQuery(String query, int showFromRankedesult, int limit, LinkedList<WebResult> results) {
        try {
            query = URLEncoder.encode(query, "UTF-8");
            URL url = new URL("http://ajax.googleapis.com/ajax/menzisservices/search/web?start=" + String.valueOf(showFromRankedesult) + "&rsz=large&v=1.0&q=" + query);
            URLConnection connection = url.openConnection();
            connection.addRequestProperty("Referer", HTTP_REFERER);
            String line;
            StringBuilder builder = new StringBuilder();
            BufferedReader reader = new BufferedReader(new InputStreamReader(connection.getInputStream()));
            while ((line = reader.readLine()) != null) {
                builder.append(line);
            }
            String response = builder.toString();
            JSONObject json = new JSONObject(response);
            JSONArray ja = json.getJSONObject("responseData").getJSONArray("results");
            for (int i = 0; i < ja.length() && results.size() < limit; i++) {
                JSONObject j = ja.getJSONObject(i);
                WebResult result = new WebResult();
                result.setSnippet(j.getString("content"));
                result.setTitle(j.getString("titleNoFormatting"));
                result.setUrl(j.getString("url"));
                DeliciousURLInfo del = JsonParser.parseDeliciousInfoFromURL(j.getString("url").trim());
                result.setDelicious(del);
                result.setRank(i + 1);
                result.setEngine(WebResult.GOOGLE);
                results.add(result);
            }
        } catch (Exception e) {
            System.err.println("Something went wrong...");
            e.printStackTrace();
        }
    }

    public static void makeQuery(String query) {
        System.out.println("\nQuerying for " + query);
        try {
            query = URLEncoder.encode(query, "UTF-8");
            URL url = new URL("http://ajax.googleapis.com/ajax/services/search/web?start=8&rsz=large&v=2.0&q=" + query);
            URLConnection connection = url.openConnection();
            connection.addRequestProperty("Referer", HTTP_REFERER);
            String line;
            StringBuilder builder = new StringBuilder();
            BufferedReader reader = new BufferedReader(new InputStreamReader(connection.getInputStream()));
            while ((line = reader.readLine()) != null) {
                builder.append(line);
            }
            String response = builder.toString();
            JSONObject json = new JSONObject(response);
            System.out.println("Total results = " + json.getJSONObject("responseData").getJSONObject("cursor").getString("estimatedResultCount"));
            JSONArray ja = json.getJSONObject("responseData").getJSONArray("results");
            System.out.println("\nResults:");
            for (int i = 0; i < ja.length(); i++) {
                System.out.print((i + 1) + ". ");
                JSONObject j = ja.getJSONObject(i);
                System.out.println(j.getString("titleNoFormatting"));
                System.out.println(j.getString("url"));
                System.out.println(j.getString("content"));
            }
        } catch (Exception e) {
            System.err.println("Something went wrong...");
            e.printStackTrace();
        }
    }

    public static void main(String args[]) {
        String AGENT_LIST = "/home/sergio/projects/dictionary/user_agent_list.txt";
        makeQuery("buenas");
    }
}
