package conversationator370;

import java.io.*;
import java.net.*;
import java.util.regex.*;

/**
 * Class used to figure out what URL to use to find knowledge
 *
 * @author Team Us
 */
public class KnowledgeBase {

    /**
     * Uses Yahoo to find URL to get knowledge to be passed to Scraper, and then formatter.
     *
     * @param subject subject to look up in wiki
     *
     * @return legitURL - String
     *
     * @author Team Us
     */
    public String findURL(String subject) throws IOException {
        try {
            String subject2 = subject.replace(' ', '+');
            String urlName = "https://www.google.com/search?q=java";
            String urlName2 = "http://search.yahoo.com/bin/search?p=" + subject2 + "+wikipedia";
            URL url = new URL(urlName2);
            HttpURLConnection connection = (HttpURLConnection) url.openConnection();
            connection.setRequestMethod("GET");
            connection.connect();
            InputStream stream = connection.getInputStream();
            BufferedReader in = new BufferedReader(new InputStreamReader(connection.getInputStream()));
            String line;
            while ((line = in.readLine()) != null) {
                int beginURL = line.indexOf("href=\"http://en.wikipedia.org/wiki/");
                line = line.substring(beginURL + 6);
                int endurl = line.indexOf("\"");
                String legitUrl = line.substring(0, endurl);
                return legitUrl;
            }
        } catch (MalformedURLException e) {
            System.out.println("Invalid URL");
        }
        return null;
    }
}
