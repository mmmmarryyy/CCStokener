package com.k42b3.xoxa;

import java.net.URL;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.List;
import org.apache.http.HttpResponse;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.impl.client.DefaultHttpClient;
import org.apache.http.params.BasicHttpParams;
import org.apache.http.params.HttpConnectionParams;
import org.apache.http.params.HttpParams;
import com.sun.syndication.feed.synd.SyndEntry;
import com.sun.syndication.feed.synd.SyndFeed;
import com.sun.syndication.io.SyndFeedInput;
import com.sun.syndication.io.XmlReader;

/**
 * FeedBot
 *
 * @author     Christoph Kappestein <k42b3.x@gmail.com>
 * @license    http://www.gnu.org/licenses/gpl.html GPLv3
 * @link       http://code.google.com/p/delta-quadrant
 * @version    $Revision: 61 $
 */
public class FeedBot extends BotAbstract {

    protected ArrayList<String> sources;

    protected SyndFeedInput input;

    public FeedBot(String host, int port, String nick, String pass, String channel, boolean ssl, int minInterval, int maxInterval, ArrayList<String> sources) {
        super(host, port, nick, pass, channel, ssl, minInterval, maxInterval);
        this.sources = sources;
        this.input = new SyndFeedInput();
    }

    public ArrayList<Resource> getResources(int limit) {
        try {
            ArrayList<Resource> resources = new ArrayList<Resource>(limit);
            for (int i = 0; i < this.sources.size(); i++) {
                URL url = new URL(this.sources.get(i));
                List<SyndEntry> entries = this.requestFeed(this.sources.get(i));
                if (entries != null) {
                    for (int j = 0; j < entries.size() && resources.size() < limit; j++) {
                        SyndEntry entry = entries.get(j);
                        if (entry.getPublishedDate().after(this.getLastUpdated())) {
                            Resource res = new Resource();
                            res.setId(entry.getUri());
                            res.setTitle(url.getHost() + ": " + entry.getTitle());
                            res.setLink(entry.getUri());
                            res.setDate(entry.getPublishedDate());
                            resources.add(res);
                        }
                    }
                }
            }
            return resources;
        } catch (Exception e) {
            logger.warning(e.getMessage());
            return null;
        }
    }

    public List<SyndEntry> requestFeed(String url) {
        try {
            HttpParams httpParams = new BasicHttpParams();
            HttpConnectionParams.setConnectionTimeout(httpParams, 6000);
            DefaultHttpClient httpClient = new DefaultHttpClient(httpParams);
            HttpGet httpGet = new HttpGet(url);
            SimpleDateFormat sdf = new SimpleDateFormat("EEE, d MMM yyyy HH:mm:ss z");
            httpGet.setHeader("If-Modified-Since", sdf.format(this.getLastUpdated()));
            logger.fine("Executing request " + httpGet.getRequestLine());
            HttpResponse httpResponse = httpClient.execute(httpGet);
            logger.fine("Received " + httpResponse.getStatusLine());
            XmlReader reader = new XmlReader(httpResponse.getEntity().getContent());
            SyndFeed feed = this.input.build(reader);
            return feed.getEntries();
        } catch (Exception e) {
            logger.warning(e.getMessage());
            return null;
        }
    }
}
