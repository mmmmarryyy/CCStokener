package be.lalameliegeoise.website.tools.sync.sitemap;

import com.redfin.sitemapgenerator.WebSitemapGenerator;
import com.redfin.sitemapgenerator.WebSitemapUrl;
import java.io.IOException;
import java.net.HttpURLConnection;
import java.net.MalformedURLException;
import java.net.URL;
import java.util.Date;
import java.util.Map;

/** Facade for Sitemap generator SitemapGen4j (<a href="http://code.google.com/p/sitemapgen4j/">http://code.google.com/p/sitemapgen4j/</a>)
 * @author Cyril Briquet
 */
public class Sitemap {

    private final WebSitemapGenerator wsg;

    private final String webServerUrl;

    private final Map<String, Boolean> unmappedFiles;

    private final Date now;

    private static final double hiPriority = 1.0;

    private static final double regularPriority = 0.5;

    private static final String[] searchEngineURLs = new String[] { "http://www.google.com/webmasters/tools/ping?sitemap=", "http://search.yahooapis.com/SiteExplorerService/V1/updateNotification?appid=SitemapWriter&url=" };

    protected Sitemap(WebSitemapGenerator wsg, String webServerUrl, Map<String, Boolean> unmappedFiles) {
        if (wsg == null) {
            throw new NullPointerException("illegal sitemap generator");
        }
        if (webServerUrl == null) {
            throw new NullPointerException("illegal web server URL");
        }
        if (unmappedFiles == null) {
            throw new NullPointerException("illegal unmapped files");
        }
        this.wsg = wsg;
        this.webServerUrl = webServerUrl;
        this.unmappedFiles = unmappedFiles;
        this.now = new Date();
    }

    public synchronized void addURL(String url) throws MalformedURLException {
        if (url == null) {
            throw new NullPointerException("illegal URL");
        }
        if (url.startsWith(this.webServerUrl)) {
            throw new IllegalArgumentException("illegal URL " + url + ", should be absolute, not starting with " + this.webServerUrl);
        }
        if (this.unmappedFiles.get(url) != null) {
            System.out.println("\tUnmapped file " + url);
            return;
        }
        for (String unmappedFile : this.unmappedFiles.keySet()) {
            if (url.startsWith(unmappedFile) && (this.unmappedFiles.get(unmappedFile) == true)) {
                return;
            }
        }
        final double priority = isHighPriorityURL(url) ? hiPriority : regularPriority;
        url = this.webServerUrl + url;
        final WebSitemapUrl sitemapUrl = new WebSitemapUrl.Options(url).lastMod(this.now).priority(priority).build();
        this.wsg.addUrl(sitemapUrl);
    }

    public synchronized void write() {
        this.wsg.write();
    }

    public synchronized void notifySearchEngines() {
        for (String searchEngineURL : searchEngineURLs) {
            final String sitemapUrl = searchEngineURL + this.webServerUrl + "sitemap/sitemap.xml";
            notifySearchEngine(sitemapUrl);
        }
    }

    private synchronized void notifySearchEngine(String sitemapUrl) {
        if (sitemapUrl == null) {
            throw new NullPointerException("illegal sitemap URL");
        }
        try {
            final URL url = new URL(sitemapUrl);
            final HttpURLConnection httpCon = (HttpURLConnection) url.openConnection();
            httpCon.setDoOutput(true);
            httpCon.connect();
            System.out.println("\t" + ((httpCon.getResponseCode() == 200) ? "Submitted" : "Could not submit") + " sitemap to " + sitemapUrl);
            httpCon.disconnect();
        } catch (MalformedURLException e) {
            throw new IllegalStateException("illegal URL " + sitemapUrl);
        } catch (IOException e) {
            System.out.println("\tCould not submit sitemap to " + sitemapUrl + "\n\t" + e);
        }
    }

    private boolean isHighPriorityURL(String url) {
        if (url == null) {
            throw new NullPointerException("illegal URL");
        }
        if (url.equals("/")) {
            return true;
        }
        if (url.indexOf("/") > -1) {
            return false;
        }
        return url.endsWith("index.html") || url.endsWith("index") || url.endsWith("index.php") || url.endsWith("php") || url.endsWith("welcome.html") || url.endsWith("welcome") || url.endsWith("bienvenue.html") || url.endsWith("bienvenue");
    }
}
