package net.allblog.commons;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.net.MalformedURLException;
import java.net.URL;
import java.util.Iterator;
import net.allblog.testbed.FeedAnalyst;
import net.allblog.testbed.SearchUtil;
import com.sun.syndication.feed.synd.SyndEntry;
import com.sun.syndication.io.FeedException;

/**
 * 
 * @author zannavi
 *
 */
public class LoadSpeedChecker {

    public LoadSpeedChecker() {
    }

    /**
	 * @return  loading time(mili-seconds) of web page for given url
	 * @see http://forums.sun.com/thread.jspa?messageID=10289274
	 */
    public int doCheck(URL url) throws IOException {
        long start = (System.currentTimeMillis());
        BufferedReader in = new BufferedReader(new InputStreamReader(url.openStream()));
        String inputLine;
        while ((inputLine = in.readLine()) != null) {
        }
        in.close();
        long end = (System.currentTimeMillis());
        return (int) (end - start);
    }

    /**
	 * @author zannavi
	 * @see It's a test method.
	 */
    public static void main(String args[]) throws MalformedURLException, IOException, FeedException {
        LoadSpeedChecker chkr = new LoadSpeedChecker();
        FeedAnalyst analyst = new FeedAnalyst();
        Iterator iter = analyst.getEntryIterator(new URL("http://rss.allblog.net/AllPosts.xml"));
        while (iter.hasNext()) {
            SyndEntry entry = (SyndEntry) iter.next();
            System.out.print(entry.getTitle() + ": ");
            URL url = new URL(SearchUtil.getSource(entry.getLink()));
            System.out.println(": " + url);
            System.out.println("(" + chkr.doCheck(url) + "ms)");
            InputStream is = url.openStream();
            String contents = SearchUtil.getContents(is);
            int fcnt = SearchUtil.countMatches(contents, "<meta http-equiv=");
            if (fcnt > 0) {
                System.out.println("found: " + fcnt);
            }
        }
    }
}
