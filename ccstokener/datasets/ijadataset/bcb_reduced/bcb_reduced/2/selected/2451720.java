package de.sonivis.tool.mwapiconnector;

import java.io.IOException;
import java.io.InputStreamReader;
import java.net.MalformedURLException;
import java.net.URL;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.TimeUnit;
import org.apache.http.HttpEntity;
import org.apache.http.HttpHost;
import org.apache.http.HttpRequest;
import org.apache.http.HttpResponse;
import org.apache.http.HttpStatus;
import org.apache.http.HttpVersion;
import org.apache.http.client.ClientProtocolException;
import org.apache.http.client.HttpClient;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.conn.ClientConnectionManager;
import org.apache.http.conn.params.ConnManagerParams;
import org.apache.http.conn.scheme.PlainSocketFactory;
import org.apache.http.conn.scheme.Scheme;
import org.apache.http.conn.scheme.SchemeRegistry;
import org.apache.http.conn.scheme.SocketFactory;
import org.apache.http.impl.client.DefaultHttpClient;
import org.apache.http.impl.conn.tsccm.ThreadSafeClientConnManager;
import org.apache.http.message.BasicHttpRequest;
import org.apache.http.params.BasicHttpParams;
import org.apache.http.params.CoreConnectionPNames;
import org.apache.http.params.HttpParams;
import org.apache.http.params.HttpProtocolParams;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * HTTP client for querying the <a href="http://www.mediawiki.org" target="_blank">MediaWiki</a>
 * API.
 * <p>
 * This new bot is based on <a href="http://hc.apache.org/httpcomponents-client/index.html"
 * target="_blank">Apache HttpClient</a> in Version 4.0beta2 that went through a code rewrite and
 * fixed a lot of bugs of the older versions.
 * </p>
 * <p>
 * The second difference is that it offers a method for threaded execution of a {@link Collection}
 * of requests, namely {@link #performAllRequests(String)}.
 * </p>
 * 
 * @author Andreas Erber
 * @version 0.1.3, $Date: 2010-11-03 11:49:39 -0400 (Wed, 03 Nov 2010) $
 */
public class ThreadedMWApiBot {

    /**
	 * Request response content encoding.
	 */
    public static final String CHARSET = "UTF-8";

    /**
	 * Name of the bot.
	 */
    public static final String NAME = "SONIVIS MediaWiki API Bot";

    /**
	 * Version information.
	 */
    public static final String VERSION = "0.1.3";

    /**
	 * Number of maximum active threads in threaded execution mode.
	 */
    public static final int MAX_TOTAL_CONNECTIONS = 10;

    /**
	 * Logger.
	 */
    private static final Logger LOGGER = LoggerFactory.getLogger(ThreadedMWApiBot.class);

    /**
	 * The {@link ThreadPoolExecutor}.
	 */
    private static ThreadPoolExecutor tpExecutor = null;

    /**
	 * The {@link SchemeRegistry}.
	 */
    private static final SchemeRegistry SUPPORTED_SCHEMES = new SchemeRegistry();

    /**
	 * {@link HttpParams} to use.
	 */
    private final HttpParams params = new BasicHttpParams();

    /**
	 * Internal {@link HttpClient} for request execution.
	 */
    private HttpClient client = null;

    /**
	 * The target {@link HttpHost} to query.
	 */
    private HttpHost target = null;

    /**
	 * Path to the wiki installation of a certain domain.
	 */
    private static String wikiPath;

    /**
	 * {@link InputStreamReader} created from resulting {@link InputStream}.
	 */
    private InputStreamReader inReader = null;

    /**
	 * Constructor.
	 * <p>
	 * Groups the basic setup activities for the bot. It takes care of the socket connections, the
	 * protocol schemes, the connection management setup, the user agent string, the request
	 * response content encoding and creates the internal {@link HttpClient}.
	 * </p>
	 */
    protected ThreadedMWApiBot() {
        final SocketFactory sf = PlainSocketFactory.getSocketFactory();
        SUPPORTED_SCHEMES.register(new Scheme("http", sf, 80));
        ConnManagerParams.setMaxTotalConnections(this.params, MAX_TOTAL_CONNECTIONS);
        ConnManagerParams.setTimeout(this.params, 300000);
        this.params.setParameter(CoreConnectionPNames.SO_TIMEOUT, 600000);
        HttpProtocolParams.setUserAgent(this.params, NAME + " " + VERSION);
        HttpProtocolParams.setVersion(this.params, HttpVersion.HTTP_1_1);
        HttpProtocolParams.setContentCharset(this.params, CHARSET);
        HttpProtocolParams.setUseExpectContinue(this.params, true);
        final ClientConnectionManager ccm = new ThreadSafeClientConnManager(this.params, SUPPORTED_SCHEMES);
        this.client = new DefaultHttpClient(ccm, this.params);
        tpExecutor = new ThreadPoolExecutor(1, 5, 1000, TimeUnit.SECONDS, new LinkedBlockingQueue<Runnable>());
    }

    /**
	 * Constructor.
	 * <p>
	 * Specify the {@link URL} to the base directory of a <a
	 * href="http://www.mediawiki.org" target="_blank">MediaWiki</a>
	 * installation. Do not provide something like <i>index.php</i> or
	 * <i>Main_Page</i> in the URL. Whether the trailing slash is present or not
	 * doesn't matter. Note, that the wiki path (anything else than the domain
	 * name or IP) will be statically tied to the class.
	 * </p>
	 * <p>
	 * The constructor currently has no way to tell if it is really a <a
	 * href="http://www.mediawiki.org" target="_blank">MediaWiki} installation
	 * {@link URL</a> that was provided. If not so, the
	 * 
	 * @link AbstractMWApiQuery API queries} will pretty surely fail although
	 *       construction of the bot is successful as long as the {@link URL} is
	 *       syntactically correct.
	 *       </p>
	 * 
	 * @param url
	 *            A {@link URL} to a <a href="http://www.mediawiki.org"
	 *            target="_blank">MediaWiki</a> installation.
	 * @see #ThreadedMWApiBot()
	 * @see #wikiConnect(URL)
	 */
    public ThreadedMWApiBot(final URL url) {
        this();
        this.wikiConnect(url);
    }

    /**
	 * Constructor.
	 * <p>
	 * Basically the same as {@link #ThreadedMWApiBot(URL)} except for the format of the parameter.
	 * </p>
	 * 
	 * @param url
	 *            The {@link String} holding the URL to a <a href="http://www.mediawiki.org"
	 *            target="_blank">MediaWiki</a> installation.
	 * @throws MalformedURLException
	 *             if specified <code>url</code> cannot be transformed into a {@link URL}.
	 * @see #ThreadedMWApiBot(URL)
	 */
    public ThreadedMWApiBot(final String url) throws MalformedURLException {
        this(new URL(url));
    }

    /**
	 * Connect to a Wiki.
	 * <p>
	 * The method does not actually connect but configures the HTTP target host for the specified
	 * url. The url should point to the wiki's installation folder, i.e. {@link http
	 * ://en.wikipedia.org/wiki}.
	 * </p>
	 * <p>
	 * The internal <a href="httpHost" target="_blank">target host</a> is configured with host
	 * (domain name or IP), port (if any), and protocol part (currently only HTTP is supported) of
	 * the {@link URL}. The path to the wiki is statically bound to the class in an internal field
	 * for further use.
	 * </p>
	 * <p>
	 * If the configuration is successful <code>true</code> is returned.
	 * <p>
	 * 
	 * @param url
	 *            The {@link URL} to a <a href="http://www.mediawiki.org"
	 *            target="_blank">MediaWiki</a> installation.
	 * @return <code>true</code> on successful configuration, <code>false</code> otherwise.
	 */
    private boolean wikiConnect(final URL url) {
        if (url != null) {
            final String host = url.getHost();
            final int port = url.getPort();
            final String protocol = url.getProtocol();
            final String path = url.getPath();
            this.target = new HttpHost(host, port, protocol);
            if (path.length() > 1) {
                wikiPath = path.substring(0, path.lastIndexOf("/"));
            } else {
                wikiPath = "";
            }
            return true;
        }
        return false;
    }

    /**
	 * Execute all requests pending in {@link HttpRequestQueue}.
	 * <p>
	 * The method loops through the {@link HttpRequestQueue} and polls for the top-most pending
	 * {@link IWikiApiRequest} and creates a {@link HttpGet} request with the internal wiki path
	 * settings of this class. Based on the successful creation of the request and the configured
	 * target <a href="httpHost}" target="_blank">a {@link HttpGetThread</a> is instanced and
	 * started.
	 * </p>
	 * <p>
	 * 
	 * 
	 * 
	 * 
	 * 
	 * 
	 * When the {@link HttpRequestQueue} is empty then, this thread goes to
	 * {@link Thread#sleep(long) sleep} for some seconds before it checks the queue again. This way,
	 * follow-up requests produced by the {@link IWikiApiRequest}s should also be covered. If the
	 * thread nap happens to be interrupted the method calls itself recursively.
	 * </p>
	 * <p>
	 * The argument is supposed to be a short string providing printable information on what kinds
	 * of queries are to be executed within the loop. The method is set to produce some progress
	 * information on standard out.
	 * </p>
	 * 
	 * @param comment
	 *            Information on the queries to be executed. Will be used for progress output.
	 * @see HttpGetThread
	 * @see HttpRequestQueue
	 */
    public final synchronized void performAllRequests(final String comment) {
        final int itemCount = HttpRequestQueue.size();
        final int onePercent = itemCount / 100;
        int smallCount = 0;
        int itemCounter = 0;
        if (LOGGER.isInfoEnabled()) {
            LOGGER.info("Starting: " + comment + "! Number of requests: " + itemCount);
        }
        final long tStart = System.currentTimeMillis();
        while (!HttpRequestQueue.isEmpty()) {
            tpExecutor.execute(this.createQueryThread(HttpRequestQueue.poll()));
            ++itemCounter;
            ++smallCount;
            if (smallCount > onePercent) {
                if (LOGGER.isInfoEnabled()) {
                    LOGGER.info(comment + " - progress: " + ((itemCounter * 100) / itemCount) + "%");
                }
                smallCount = 0;
            }
            if (HttpRequestQueue.isEmpty()) {
                try {
                    Thread.sleep(5000);
                } catch (InterruptedException e) {
                    this.performAllRequests(comment);
                }
            }
        }
        if (LOGGER.isInfoEnabled()) {
            LOGGER.info(comment + " took " + (System.currentTimeMillis() - tStart) + " ms.");
        }
    }

    /**
	 * Turn an {@link IWikiApiRequest} into an {@link HttpGetThread}.
	 * 
	 * @param iwar
	 *            The {@link IWikiApiRequest} to transform.
	 * @return The {@link HttpGetThread} created from the argument
	 */
    protected final HttpGetThread createQueryThread(final IWikiApiRequest iwar) {
        final HttpGet httpGet = new HttpGet(wikiPath + iwar.getExecutableQuery());
        final HttpGetThread httpGetThread = new HttpGetThread(this.client, this.target, httpGet, iwar);
        return httpGetThread;
    }

    /**
	 * Execute an HTTP request.
	 * <p>
	 * The specified {@link IWikiApiRequest} is turned into an {@link HttpRequest} with the
	 * {@link IWikiApiRequest#getExecutableQuery() executable query} string of the
	 * {@link IWikiApiRequest} having the {@link #getWikiPath() wiki path} prepended. The
	 * {@link IWikiApiRequest} defines the {@link IWikiApiRequest#getRequestMethod() request method}
	 * itself.
	 * </p>
	 * <p>
	 * After the request was executed the bot inspects the server status and frees the connection in
	 * case of error. The error information is preserved and put out, the returned server
	 * {@link StatusLine#getStatusCode() status code} is
	 * {@link IWikiApiRequest#setRequestResultStatus(int) associated} with the
	 * {@link IWikiApiRequest}.
	 * </p>
	 * <p>
	 * The response content is streamed into a reader and on success passed to the
	 * {@link XMLPreparer} to create an XML document from the content that is
	 * {@link IWikiApiRequest#setXMLResult(org.jdom.Element) fed} back to the
	 * {@link IWikiApiRequest}.
	 * </p>
	 * 
	 * @param war
	 *            The {@link IWikiApiRequest} to be executed.
	 * @see IWikiApiRequest
	 */
    public final synchronized void performRequest(final IWikiApiRequest war) {
        if (LOGGER.isDebugEnabled()) {
            LOGGER.debug("Executing: " + wikiPath + war.getExecutableQuery());
        }
        final HttpRequest req = new BasicHttpRequest(war.getRequestMethod(), wikiPath + war.getExecutableQuery());
        HttpEntity entity = null;
        try {
            final HttpResponse resp = this.client.execute(this.target, req);
            entity = resp.getEntity();
            final int statusCode = resp.getStatusLine().getStatusCode();
            war.setRequestResultStatus(statusCode);
            if (statusCode > HttpStatus.SC_OK) {
                if (LOGGER.isErrorEnabled()) {
                    LOGGER.error("Server Status: " + statusCode + " - " + resp.getStatusLine().getReasonPhrase());
                    LOGGER.error("Erroneous query: " + wikiPath + war.getExecutableQuery());
                }
                entity.consumeContent();
            } else {
                if (entity != null) {
                    this.inReader = new InputStreamReader(entity.getContent(), CHARSET);
                    if (this.inReader == null) {
                        war.setXMLResult(null);
                    } else {
                        if (!this.inReader.getEncoding().substring(0, 2).equalsIgnoreCase(CHARSET.substring(0, 2))) {
                            if (LOGGER.isErrorEnabled()) {
                                LOGGER.error("The request body is not properly encoded - found " + this.inReader.getEncoding());
                                LOGGER.error("Erroneous query: " + wikiPath + war.getExecutableQuery());
                            }
                        }
                        if (war.getFormat().equalsIgnoreCase("xml")) {
                            war.setXMLResult(XMLPreparer.getXMLRoot(this.inReader));
                        }
                    }
                }
            }
        } catch (ClientProtocolException e) {
            if (LOGGER.isErrorEnabled()) {
                LOGGER.error("Request failed due to client protocol propblem.", e);
            }
        } catch (IOException e) {
            if (LOGGER.isErrorEnabled()) {
                LOGGER.error("Request failed due to IO propblem.", e);
            }
        } finally {
            if (entity != null) {
                try {
                    entity.consumeContent();
                } catch (IOException e) {
                    if (LOGGER.isErrorEnabled()) {
                        LOGGER.error("Caught IO exception when gracefully cleaning up the mess from last failed request.");
                    }
                }
            }
        }
        war.processRequestResult();
    }

    /**
	 * Return the path to the wiki to examine.
	 * 
	 * @return path to the wiki
	 */
    public final String getWikiPath() {
        return wikiPath;
    }

    /**
	 * Retrieve the {@link HttpClient}.
	 * 
	 * @return the client
	 */
    public final HttpClient getClient() {
        return this.client;
    }

    /**
	 * Retrieve the target {@link HttpHost}.
	 * 
	 * @return the target
	 */
    public final HttpHost getTarget() {
        return this.target;
    }

    /**
	 * {@inheritDoc}
	 * 
	 * @throws Throwable
	 * 
	 * @see java.lang.Object#finalize()
	 */
    @Override
    public void finalize() throws Throwable {
        if (!tpExecutor.isTerminating()) {
            if (LOGGER.isInfoEnabled()) {
                LOGGER.info("Shutting down the ThreadExecutor.");
            }
            tpExecutor.shutdown();
        }
        super.finalize();
    }
}
