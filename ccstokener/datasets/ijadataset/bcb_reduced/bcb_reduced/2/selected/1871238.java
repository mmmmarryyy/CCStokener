package itjava.model;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.net.URL;
import java.net.URLEncoder;
import java.util.ArrayList;
import java.util.Collection;
import java.util.LinkedHashSet;
import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.jsoup.select.Elements;

/**
 * @author Aniket, Matt
 *
 */
public class LinkStoreScrape {

    public static LinkedHashSet<String> CreateLinks(String query) {
        LinkedHashSet<String> _setOfLinks;
        _setOfLinks = new LinkedHashSet<String>();
        _setOfLinks.addAll(StackOverflowSearch(query));
        return _setOfLinks;
    }

    private static ArrayList<String> StackOverflowSearch(String query) {
        ArrayList<String> bingSearchResults = new ArrayList<String>();
        try {
            String stackQuery = query.replace(" ", "+");
            String request = "http://www.google.com/search?q=[java]+" + stackQuery + "&sitesearch=stackoverflow.com%2Fquestions";
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
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
        return bingSearchResults;
    }
}
