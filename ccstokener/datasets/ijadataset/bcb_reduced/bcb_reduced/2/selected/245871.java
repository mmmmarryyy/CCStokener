package codesearch.test;

import com.google.gdata.client.Service.GDataRequest;
import com.google.gdata.client.Service.GDataRequestFactory;
import com.google.gdata.client.Service.GDataRequest.RequestType;
import com.google.gdata.client.codesearch.CodeSearchService;
import com.google.gdata.data.codesearch.CodeSearchEntry;
import com.google.gdata.data.codesearch.CodeSearchFeed;
import com.google.gdata.util.ContentType;
import com.google.gdata.util.ServiceException;
import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.net.InetSocketAddress;
import java.net.Proxy;
import java.net.SocketAddress;
import java.net.URL;
import java.net.URLConnection;
import java.util.List;
import junit.framework.TestCase;

/**
 *
 * @author <a href="mailto:jvu@zuhlke.com">Jaksa Vuckovic</a>
 *
 */
public class ProxyTest extends TestCase {

    public void testGenericConnectionThroughProxy() throws Exception {
        SocketAddress socket = new InetSocketAddress("proxy.zuehlke.com", 8080);
        Proxy proxy = new Proxy(Proxy.Type.HTTP, socket);
        URL url = new URL("http://www.google.com/codesearch/feeds/search?q=" + "System.out");
        URLConnection connection = url.openConnection(proxy);
        BufferedReader in = new BufferedReader(new InputStreamReader((InputStream) connection.getContent()));
        String string;
        String contents = "";
        while ((string = in.readLine()) != null) {
            contents += string;
        }
        assertTrue(contents.length() > 0);
    }

    public void testGoogleDataAPIConnectionThroughProxy() throws Exception {
        System.setProperty("http.proxyHost", "proxy.zuehlke.com");
        System.setProperty("http.proxyPort", "8080");
        System.setProperty("http.proxySet", "true");
        CodeSearchService service = new CodeSearchService("exampleCo-exampleApp-1");
        URL feedUrl = new URL("http://www.google.com/codesearch/feeds/search?q=" + "System.out");
        CodeSearchFeed feed = service.getFeed(feedUrl, CodeSearchFeed.class);
        List<CodeSearchEntry> entries = feed.getEntries();
        assertTrue(entries.size() > 0);
    }
}
