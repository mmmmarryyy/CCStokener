package itjava.tests;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.net.URL;
import java.net.URLConnection;
import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.jsoup.select.Elements;
import org.junit.Test;

public class JSoupTest {

    @Test
    public void SimpleParse() {
        try {
            URL url = new URL("http://www.javapractices.com/topic/TopicAction.do?Id=87");
            URLConnection conn = url.openConnection();
            BufferedReader reader = new BufferedReader(new InputStreamReader(url.openStream()));
            String inputLine;
            String finalContents = "";
            while ((inputLine = reader.readLine()) != null) {
                finalContents += "\n" + inputLine.replaceAll("<code", "<pre").replaceAll("code>", "pre>");
            }
            Document doc = Jsoup.parse(finalContents);
            Elements eles = doc.getElementsByTag("pre");
            for (Element ele : eles) {
                System.out.println(ele.text());
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
    }
}
