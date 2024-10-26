package itjava.tests;

import java.io.BufferedReader;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.UnsupportedEncodingException;
import java.net.URL;
import java.net.URLConnection;
import java.net.URLEncoder;
import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.jsoup.select.Elements;
import org.junit.Test;

public class YahooWebServiceParseResults {

    /**
	 * This example illustrates how easy it is to parse a Yahoo! Web Service XML
	 * result with XPath.
	 */
    @Test
    public void GetYahooSearchResult() {
        String query = "Scanner%20Java%20example";
        String request = "http://boss.yahooapis.com/ysearch/web/v1/" + query + "?appid=zfau5aPV34ETbq9mWU0ui5e04y0rIewg1zwvzHb1tGoBFK2nSCU1SKS2D4zphh2rd3Wf&format=xml&count=50";
        try {
            URL url = new URL(request);
            System.out.println("Host : " + url.getHost());
            URLConnection conn = url.openConnection();
            BufferedReader reader = new BufferedReader(new InputStreamReader(url.openStream()));
            String inputLine;
            String finalContents = "";
            while ((inputLine = reader.readLine()) != null) {
                finalContents += "\n" + inputLine;
            }
            Document doc = Jsoup.parse(finalContents);
            Elements eles = doc.getElementsByTag("url");
            for (Element ele : eles) {
                System.out.println(ele.text());
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    @Test
    public void GetBingSearchResult() throws UnsupportedEncodingException {
        String query = "Scanner Java example";
        String request = "http://api.bing.net/xml.aspx?AppId=731DD1E61BE6DE4601A3008DC7A0EB379149EC29" + "&Version=2.2&Market=en-US&Query=" + URLEncoder.encode(query, "UTF-8") + "&Sources=web+spell&Web.Count=50";
        try {
            URL url = new URL(request);
            System.out.println("Host : " + url.getHost());
            BufferedReader reader = new BufferedReader(new InputStreamReader(url.openStream()));
            String inputLine;
            String finalContents = "";
            while ((inputLine = reader.readLine()) != null) {
                finalContents += "\n" + inputLine;
            }
            Document doc = Jsoup.parse(finalContents);
            Elements eles = doc.getElementsByTag("web:Url");
            for (Element ele : eles) {
                System.out.println(ele.text());
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
    }
}
