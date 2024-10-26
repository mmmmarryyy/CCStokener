package uk.org.windswept.feedreader;

import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.Reader;
import java.net.InetAddress;
import java.security.KeyManagementException;
import java.security.NoSuchAlgorithmException;
import java.security.cert.CertificateException;
import java.security.cert.X509Certificate;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.TimerTask;
import javax.net.ssl.SSLContext;
import javax.net.ssl.TrustManager;
import javax.net.ssl.X509TrustManager;
import org.apache.http.HttpHost;
import org.apache.http.HttpResponse;
import org.apache.http.HttpStatus;
import org.apache.http.auth.AuthScope;
import org.apache.http.auth.Credentials;
import org.apache.http.auth.NTCredentials;
import org.apache.http.auth.UsernamePasswordCredentials;
import org.apache.http.client.CookieStore;
import org.apache.http.client.HttpClient;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.client.protocol.ClientContext;
import org.apache.http.conn.params.ConnRoutePNames;
import org.apache.http.conn.scheme.Scheme;
import org.apache.http.conn.ssl.SSLSocketFactory;
import org.apache.http.impl.client.BasicCookieStore;
import org.apache.http.impl.client.DefaultHttpClient;
import org.apache.http.impl.cookie.BasicClientCookie;
import org.apache.http.protocol.BasicHttpContext;
import org.apache.http.protocol.HttpContext;
import org.apache.log4j.Logger;
import uk.org.windswept.httpclient.jcifs.NTLMSchemeFactory;
import com.sun.syndication.feed.synd.SyndEntry;
import com.sun.syndication.feed.synd.SyndFeed;
import com.sun.syndication.io.FeedException;
import com.sun.syndication.io.SyndFeedInput;
import com.sun.syndication.io.SyndFeedOutput;
import com.sun.syndication.io.XmlReader;

/**
 * @version    : $Revision: 87 $
 * @author     : $Author: satkinson $
 * Last Change : $Date: 2011-03-14 18:01:49 -0400 (Mon, 14 Mar 2011) $
 * URL         : $HeadURL: http://javafeedreader.svn.sourceforge.net/svnroot/javafeedreader/trunk/src/main/java/uk/org/windswept/feedreader/FeedReaderTimerTask.java $
 * ID          : $Id: FeedReaderTimerTask.java 87 2011-03-14 22:01:49Z satkinson $
 */
public class FeedReaderTimerTask extends TimerTask {

    private static final Logger LOGGER = Logger.getLogger(FeedReaderTimerTask.class);

    private FeedInfo _feedInfo;

    private HttpHost _proxy;

    private FeedReaderEntryDisplay _entryDisplay;

    private boolean firstRun = true;

    private long runCounter = 0;

    FeedReaderTimerTask(FeedInfo feedInfo, HttpHost proxy, FeedReaderEntryDisplay entryDisplay) {
        LOGGER.debug("Creating FeedReaderTimerTask for " + feedInfo);
        _feedInfo = feedInfo;
        _proxy = proxy;
        _entryDisplay = entryDisplay;
    }

    public String readStreamToString(InputStream is) throws IOException {
        final char[] buffer = new char[0x10000];
        StringBuilder out = new StringBuilder();
        Reader in = new InputStreamReader(is);
        int read;
        do {
            read = in.read(buffer, 0, buffer.length);
            if (read > 0) {
                out.append(buffer, 0, read);
            }
        } while (read >= 0);
        return out.toString();
    }

    public void run() {
        runCounter++;
        try {
            LOGGER.info("Fetching feed [" + runCounter + "] " + _feedInfo);
            DefaultHttpClient httpClient = new DefaultHttpClient();
            HttpContext localContext = new BasicHttpContext();
            disableSSLCertificateChecking(httpClient);
            if (_proxy != null && _feedInfo.getUseProxy()) {
                LOGGER.info("Configuring proxy " + _proxy);
                httpClient.getParams().setParameter(ConnRoutePNames.DEFAULT_PROXY, _proxy);
            }
            if (_feedInfo.getUsername() != null) {
                Credentials credentials;
                if (_feedInfo.getUsername().contains("/")) {
                    String username = _feedInfo.getUsername().substring(_feedInfo.getUsername().indexOf("/") + 1);
                    String domain = _feedInfo.getUsername().substring(0, _feedInfo.getUsername().indexOf("/"));
                    String workstation = InetAddress.getLocalHost().getHostName();
                    LOGGER.info("Configuring NT credentials : username=[" + username + "] domain=[" + domain + "] workstation=[" + workstation + "]");
                    credentials = new NTCredentials(username, _feedInfo.getPassword(), workstation, domain);
                    httpClient.getAuthSchemes().register("ntlm", new NTLMSchemeFactory());
                    httpClient.getCredentialsProvider().setCredentials(AuthScope.ANY, credentials);
                } else {
                    credentials = new UsernamePasswordCredentials(_feedInfo.getUsername(), _feedInfo.getPassword());
                    LOGGER.info("Configuring Basic credentials " + credentials);
                    httpClient.getCredentialsProvider().setCredentials(AuthScope.ANY, credentials);
                }
            }
            if (_feedInfo.getCookie() != null) {
                BasicClientCookie cookie = new BasicClientCookie(_feedInfo.getCookie().getName(), _feedInfo.getCookie().getValue());
                cookie.setVersion(0);
                if (_feedInfo.getCookie().getDomain() != null) cookie.setDomain(_feedInfo.getCookie().getDomain());
                if (_feedInfo.getCookie().getPath() != null) cookie.setPath(_feedInfo.getCookie().getPath());
                LOGGER.info("Adding cookie " + cookie);
                CookieStore cookieStore = new BasicCookieStore();
                cookieStore.addCookie(cookie);
                localContext.setAttribute(ClientContext.COOKIE_STORE, cookieStore);
            }
            HttpGet httpget = new HttpGet(_feedInfo.getUrl());
            HttpResponse response = httpClient.execute(httpget, localContext);
            LOGGER.info("Response Status : " + response.getStatusLine());
            LOGGER.debug("Headers : " + Arrays.toString(response.getAllHeaders()));
            if (response.getStatusLine().getStatusCode() != HttpStatus.SC_OK) {
                LOGGER.error("Request was unsuccessful for " + _feedInfo + " : " + response.getStatusLine());
            } else {
                SyndFeedInput input = new SyndFeedInput();
                XmlReader reader = new XmlReader(response.getEntity().getContent());
                SyndFeed feed = input.build(reader);
                if (feed.getTitle() != null) _feedInfo.setTitle(feed.getTitle());
                LOGGER.debug("Feed : " + new SyndFeedOutput().outputString(feed));
                LOGGER.info("Feed [" + feed.getTitle() + "] contains " + feed.getEntries().size() + " entries");
                @SuppressWarnings("unchecked") List<SyndEntry> entriesList = feed.getEntries();
                Collections.sort(entriesList, new SyndEntryPublishedDateComparator());
                for (SyndEntry entry : entriesList) {
                    if (VisitedEntries.getInstance().isAlreadyVisited(entry.getUri())) {
                        LOGGER.debug("Already received " + entry.getUri());
                    } else {
                        _feedInfo.addEntry(entry);
                        LOGGER.debug("New entry " + entry.toString());
                        _entryDisplay.displayEntry(feed, entry, firstRun);
                    }
                }
                LOGGER.info("Completing entries for feed " + feed.getTitle());
                if (firstRun) firstRun = false;
            }
        } catch (IllegalArgumentException e) {
            LOGGER.error(e.getMessage(), e);
        } catch (FeedException e) {
            LOGGER.error(e.getMessage(), e);
        } catch (IOException e) {
            LOGGER.error(e.getMessage(), e);
        } catch (KeyManagementException e) {
            LOGGER.error(e.getMessage(), e);
        } catch (NoSuchAlgorithmException e) {
            LOGGER.error(e.getMessage(), e);
        }
    }

    public static void disableSSLCertificateChecking(HttpClient httpClient) throws KeyManagementException, NoSuchAlgorithmException {
        LOGGER.info("Disable SSL Certificate checking");
        TrustManager easyTrustManager = new X509TrustManager() {

            @Override
            public X509Certificate[] getAcceptedIssuers() {
                LOGGER.debug("getAcceptedIssuers");
                return null;
            }

            @Override
            public void checkClientTrusted(X509Certificate[] arg0, String arg1) throws CertificateException {
                LOGGER.debug("checkClientTrusted");
            }

            @Override
            public void checkServerTrusted(X509Certificate[] arg0, String arg1) throws CertificateException {
                LOGGER.debug("checkServerTrusted");
            }
        };
        SSLContext sslcontext = SSLContext.getInstance("TLS");
        sslcontext.init(null, new TrustManager[] { easyTrustManager }, null);
        SSLSocketFactory socketFactory = new SSLSocketFactory(sslcontext);
        Scheme sch = new Scheme("https", socketFactory, 443);
        httpClient.getConnectionManager().getSchemeRegistry().register(sch);
    }
}
