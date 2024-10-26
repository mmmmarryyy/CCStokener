package itjava.model;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.net.URL;
import java.net.URLEncoder;
import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.jsoup.select.Elements;
import com.google.api.client.http.javanet.NetHttpTransport;
import com.google.api.client.json.jackson.JacksonFactory;
import com.google.api.services.customsearch.Customsearch;
import com.google.api.services.customsearch.model.Result;
import com.google.api.services.customsearch.model.Search;

/**
 * @author Aniket, Matt
 *
 */
public class LinkStore {

    public static LinkedHashSet<String> CreateLinks(String query) {
        LinkedHashSet<String> _setOfLinks;
        _setOfLinks = new LinkedHashSet<String>();
        _setOfLinks.addAll(GoogleSearch(query));
        _setOfLinks.addAll(BingSearch(query));
        return _setOfLinks;
    }

    private static ArrayList<String> GoogleSearch(String query) {
        ArrayList<String> googleSearchResults = new ArrayList<String>();
        String googleSearchContext = "011045704107476092050:liivikkqg0k";
        String googleSearchAPIKey = "AIzaSyCdECK7u3Erfb23uPqCEl2lESCDw4cwOXA";
        try {
            NetHttpTransport nht = new NetHttpTransport();
            JacksonFactory jf = new JacksonFactory();
            Customsearch customsearch = new Customsearch(nht, jf);
            Customsearch.Cse.List request = customsearch.cse().list("Java Examples for " + query);
            request.setKey(googleSearchAPIKey);
            request.setCx(googleSearchContext);
            for (long iResultIndex = 1; iResultIndex < 50; iResultIndex += 10) {
                request.setStart(iResultIndex);
                Search results = request.execute();
                List<Result> searchResults = results.getItems();
                for (Result item : searchResults) {
                    String urlText = item.getLink();
                    String urlEnding = urlText.toUpperCase();
                    if (!urlEnding.endsWith(".PDF") && !urlEnding.endsWith(".DOC") && !urlEnding.endsWith(".PPT")) {
                        googleSearchResults.add(urlText);
                    }
                    System.out.println("GoogleResult: " + urlText);
                }
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
        return googleSearchResults;
    }

    private static ArrayList<String> BingSearch(String query) {
        ArrayList<String> bingSearchResults = new ArrayList<String>();
        try {
            String request = "http://api.bing.net/xml.aspx?AppId=731DD1E61BE6DE4601A3008DC7A0EB379149EC29" + "&Version=2.2&Market=en-US&Query=" + URLEncoder.encode("Java example for " + query, "UTF-8") + "&Sources=web+spell&Web.Count=30";
            URL url = new URL(request);
            System.out.println("Host : " + url.getHost());
            url.openConnection();
            BufferedReader reader = new BufferedReader(new InputStreamReader(url.openStream()));
            String inputLine;
            String finalContents = "";
            while ((inputLine = reader.readLine()) != null) {
                finalContents += "\n" + inputLine;
            }
            Document doc = Jsoup.parse(finalContents);
            Elements eles = doc.getElementsByTag("web:Url");
            for (Element ele : eles) {
                String urlText = ele.text();
                if (!urlText.endsWith(".pdf") && !urlText.endsWith(".doc") && !urlText.endsWith(".ppt") && !urlText.endsWith(".PDF") && !urlText.endsWith(".DOC") && !urlText.endsWith(".PPT")) bingSearchResults.add(ele.text());
                System.out.println("BingResult: " + ele.text());
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
        return bingSearchResults;
    }

    private static ArrayList<String> YahooSearch(String query) {
        ArrayList<String> yahooSearchResults = new ArrayList<String>();
        try {
            String request = "http://boss.yahooapis.com/ysearch/web/v1/" + URLEncoder.encode("Java example for " + query, "UTF-8") + "?appid=zfau5aPV34ETbq9mWU0ui5e04y0rIewg1zwvzHb1tGoBFK2nSCU1SKS2D4zphh2rd3Wf" + "&format=xml&count=30&type=-msoffice,-pdf";
            URL url = new URL(request);
            System.out.println("Host : " + url.getHost());
            url.openConnection();
            BufferedReader reader = new BufferedReader(new InputStreamReader(url.openStream()));
            String inputLine;
            String finalContents = "";
            while ((inputLine = reader.readLine()) != null) {
                finalContents += "\n" + inputLine;
            }
            Document doc = Jsoup.parse(finalContents);
            Elements eles = doc.getElementsByTag("url");
            for (Element ele : eles) {
                yahooSearchResults.add(ele.text());
                System.out.println("YahooResult: " + ele.text());
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
        return yahooSearchResults;
    }
}
