package de.fuberlin.wiwiss.ng4j.semwebclient;

import java.io.ByteArrayInputStream;
import java.io.InputStream;
import java.io.IOException;
import java.io.StringReader;
import java.io.StringWriter;
import java.net.HttpURLConnection;
import java.net.MalformedURLException;
import java.net.SocketTimeoutException;
import java.net.URL;
import java.net.URLConnection;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import javax.xml.transform.Transformer;
import javax.xml.transform.dom.DOMSource;
import javax.xml.transform.stream.StreamResult;
import org.cyberneko.html.parsers.DOMParser;
import org.xml.sax.InputSource;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import com.hp.hpl.jena.rdf.model.Model;
import com.hp.hpl.jena.rdf.model.ModelFactory;
import com.hp.hpl.jena.rdf.model.impl.RDFDefaultErrorHandler;
import de.fuberlin.wiwiss.ng4j.NamedGraph;
import de.fuberlin.wiwiss.ng4j.NamedGraphSet;
import de.fuberlin.wiwiss.ng4j.NamedGraphSetFactory;
import de.fuberlin.wiwiss.ng4j.impl.NamedGraphImpl;
import de.fuberlin.wiwiss.ng4j.semwebclient.threadutils.Task;
import de.fuberlin.wiwiss.ng4j.semwebclient.threadutils.TaskExecutorBase;

/**
 * The DereferencerThread executes a given DereferencingTask. It opens a
 * HttpURLConnection, creates an InputStream and tries to parse it. If the
 * Thread is finished it delivers the retrieval result.
 * 
 * @author Tobias Gauß
 * @author Olaf Hartig
 * @author Hannes Mühleisen
 */
public class DereferencerThread extends TaskExecutorBase {

    private HttpURLConnection connection;

    protected final NamedGraphSetFactory ngsFactory;

    private NamedGraphSet tempNgs = null;

    private int maxfilesize = -1;

    private boolean enableRDFa = false;

    private int connectTimeout = 0;

    private int readTimeout = 0;

    private URL url;

    private Transformer transformerForRDFa;

    private Log log = LogFactory.getLog(DereferencerThread.class);

    public DereferencerThread(NamedGraphSetFactory ngsFactory) {
        this.ngsFactory = ngsFactory;
        setPriority(getPriority() - 1);
    }

    public Class<?> getTaskType() {
        return DereferencingTask.class;
    }

    protected void executeTask(Task task) {
        DereferencingResult result = executeTask((DereferencingTask) task);
        synchronized (this) {
            if (isStopped()) return;
            ((DereferencingTask) task).notifyListeners(result);
        }
    }

    /**
	 * @return Returns true if the DereferencerThread is available for new
	 *         tasks.
	 */
    public synchronized boolean isAvailable() {
        return !hasTask() && !isStopped();
    }

    /**
	 * Starts to execute the DereferencingTask task. Returns true if the
	 * retrieval process is started false if the thread is unable to execute the
	 * task.
	 * @deprecated Please use {@link TaskExecutorBase#startTask} instead.
	 * 
	 * @param task
	 *            The task to execute.
	 */
    public synchronized boolean startDereferencingIfAvailable(DereferencingTask task) {
        if (!isAvailable()) {
            return false;
        }
        startTask(task);
        return true;
    }

    /**
	 * Creates a new DereferencingResult which contains information about the
	 * retrieval failure.
	 * 
	 * @param errorCode
	 *            the error code
	 * @param exception
	 *            the thrown exception
	 * @return
	 */
    private DereferencingResult createErrorResult(DereferencingTask task, int errorCode, Exception exception, Map<String, List<String>> headerFields) {
        return new DereferencingResult(task, errorCode, null, exception, headerFields);
    }

    /**
	 * Creates a new DereferencingResult which contains information about the
	 * retrieval failure.
	 * 
	 * @param errorCode
	 *            the error code
	 * @param exception
	 *            the thrown exception
	 * @return
	 */
    private DereferencingResult createNewUrisResult(DereferencingTask task, int errorCode, ArrayList<String> urilist) {
        return new DereferencingResult(task, errorCode, urilist, connection.getHeaderFields());
    }

    public DereferencingResult executeTask(DereferencingTask task) {
        DereferencingResult result = null;
        this.tempNgs = ngsFactory.create();
        try {
            url = new URL(task.getURI());
        } catch (MalformedURLException ex) {
            return createErrorResult(task, DereferencingResult.STATUS_MALFORMED_URL, ex, null);
        }
        try {
            URLConnection con = url.openConnection();
            con.setConnectTimeout(connectTimeout);
            con.setReadTimeout(readTimeout);
            if (task.conditional) {
                con.setIfModifiedSince(task.ifModifiedSince);
            }
            connection = (HttpURLConnection) con;
        } catch (IOException e) {
            log.debug("Creating a connection to <" + url.toString() + "> caused a " + e.getClass().getName() + ": " + e.getMessage(), e);
            return createErrorResult(task, DereferencingResult.STATUS_UNABLE_TO_CONNECT, e, null);
        }
        connection.setInstanceFollowRedirects(false);
        connection.addRequestProperty("Accept", "application/rdf+xml;q=1," + "text/xml;q=0.6,text/rdf+n3;q=0.9," + "application/octet-stream;q=0.5," + "application/xml q=0.5,application/rss+xml;q=0.5," + "text/plain; q=0.5,application/x-turtle;q=0.5," + "application/x-trig;q=0.5," + "application/xhtml+xml;q=0.5, " + "text/html;q=0.5");
        try {
            connection.connect();
        } catch (SocketTimeoutException e) {
            log.debug("Connecting to <" + url.toString() + "> caused a " + e.getClass().getName() + ": " + e.getMessage());
            connection.disconnect();
            connection = null;
            return createErrorResult(task, DereferencingResult.STATUS_TIMEOUT, e, null);
        } catch (IOException e) {
            log.debug("Connecting to <" + url.toString() + "> caused a " + e.getClass().getName() + ": " + e.getMessage(), e);
            connection.disconnect();
            connection = null;
            return createErrorResult(task, DereferencingResult.STATUS_UNABLE_TO_CONNECT, e, null);
        } catch (RuntimeException e) {
            log.debug("Connecting to <" + url.toString() + "> caused a " + e.getClass().getName() + ": " + e.getMessage());
            connection.disconnect();
            connection = null;
            return createErrorResult(task, DereferencingResult.STATUS_UNABLE_TO_CONNECT, e, null);
        }
        try {
            this.log.debug(this.connection.getResponseCode() + " " + this.url + " (" + this.connection.getContentType() + ")");
            if ((this.connection.getResponseCode() == 301) || (this.connection.getResponseCode() == 302) || (this.connection.getResponseCode() == 303)) {
                String redirectURI = this.connection.getHeaderField("Location");
                DereferencingResult r = new DereferencingResult(task, DereferencingResult.STATUS_REDIRECTED, redirectURI, connection.getHeaderFields());
                connection.disconnect();
                connection = null;
                return r;
            }
            if (this.connection.getResponseCode() == 304) {
                DereferencingResult r = new DereferencingResult(task, DereferencingResult.STATUS_UNMODIFIED, null, null, connection.getHeaderFields());
                connection.disconnect();
                connection = null;
                return r;
            }
            if (this.connection.getResponseCode() != 200) {
                DereferencingResult r = createErrorResult(task, DereferencingResult.STATUS_UNABLE_TO_CONNECT, new Exception("Unexpected response code (" + connection.getResponseCode() + ")"), connection.getHeaderFields());
                connection.disconnect();
                connection = null;
                return r;
            }
            if (connection.getContentType() == null) {
                DereferencingResult r = createErrorResult(task, DereferencingResult.STATUS_UNABLE_TO_CONNECT, new Exception("Unknown content type"), connection.getHeaderFields());
                connection.disconnect();
                connection = null;
                return r;
            }
            String lang = setLang();
            try {
                result = this.parseRdf(task, lang);
            } catch (Exception ex) {
                this.log.debug(ex.getMessage());
                DereferencingResult r = createErrorResult(task, DereferencingResult.STATUS_PARSING_FAILED, ex, connection.getHeaderFields());
                connection.disconnect();
                connection = null;
                return r;
            }
        } catch (SocketTimeoutException e) {
            log.debug("Accessing the connection to <" + url.toString() + "> caused a " + e.getClass().getName() + ": " + e.getMessage());
            DereferencingResult r = createErrorResult(task, DereferencingResult.STATUS_TIMEOUT, e, null);
            connection.disconnect();
            connection = null;
            return r;
        } catch (IOException e) {
            log.debug("Accessing the connection to <" + url.toString() + "> caused a " + e.getClass().getName() + ": " + e.getMessage(), e);
            DereferencingResult r = createErrorResult(task, DereferencingResult.STATUS_UNABLE_TO_CONNECT, e, null);
            connection.disconnect();
            connection = null;
            return r;
        }
        connection.disconnect();
        connection = null;
        return result;
    }

    /**
	 * Parses an RDF String.
	 */
    private DereferencingResult parseRdf(DereferencingTask task, String lang) throws Exception {
        if ((lang != null) && (lang.toUpperCase().equals("HTML"))) {
            String htmlContent = DereferencerThread.readout(this.connection.getInputStream());
            ArrayList<String> l = HtmlLinkFetcher.fetchLinks(htmlContent);
            if (!l.isEmpty()) {
                Iterator<String> iter = l.iterator();
                ArrayList<String> urilist = new ArrayList<String>();
                while (iter.hasNext()) {
                    String link = iter.next();
                    link = link.replace("&amp;", "&");
                    link = link.replace("&gt;", ">");
                    link = link.replace("&lt;", "<");
                    try {
                        URL newURL = new URL(url, link);
                        urilist.add(newURL.toString());
                    } catch (MalformedURLException e) {
                        log.debug("Creating a URL from the link <" + link + "> fetched for <" + url.toString() + "> caused an exception (" + e.getMessage() + ").", e);
                    }
                }
                return createNewUrisResult(task, DereferencingResult.STATUS_NEW_URIS_FOUND, urilist);
            }
            if (this.enableRDFa) {
                log.debug("Parsing HTML from <" + url.toString() + "> for RDFa");
                StringWriter rdfxml = new StringWriter();
                DOMParser parser = new DOMParser();
                parser.setFeature("http://xml.org/sax/features/namespaces", false);
                parser.setFeature("http://cyberneko.org/html/features/balance-tags", true);
                parser.setFeature("http://cyberneko.org/html/features/balance-tags/ignore-outside-content", true);
                parser.parse(new InputSource(new StringReader(htmlContent)));
                transformerForRDFa.transform(new DOMSource(parser.getDocument(), url.toString()), new StreamResult(rdfxml));
                StringReader rdfParserIn = new StringReader(rdfxml.getBuffer().toString());
                tempNgs.read(rdfParserIn, "RDF/XML", url.toString());
                int triplesCount = 0;
                Iterator<NamedGraph> graphIt = tempNgs.listGraphs();
                while (graphIt.hasNext()) {
                    NamedGraph graph = graphIt.next();
                    triplesCount += graph.size();
                }
                if (triplesCount > 0) {
                    log.debug("Found RDFa in HTML from <" + url.toString() + ">");
                    return new DereferencingResult(task, DereferencingResult.STATUS_OK, this.tempNgs, null, connection.getHeaderFields());
                } else {
                    log.debug("No RDFa in HTML from <" + url.toString() + ">");
                }
            }
            return createNewUrisResult(task, DereferencingResult.STATUS_NEW_URIS_FOUND, new ArrayList<String>());
        }
        RDFDefaultErrorHandler.silent = true;
        LimitedInputStream lis = new LimitedInputStream(this.connection.getInputStream(), this.maxfilesize);
        this.tempNgs.read(lis, lang, this.url.toString());
        return new DereferencingResult(task, DereferencingResult.STATUS_OK, this.tempNgs, null, connection.getHeaderFields());
    }

    /**
	 * Tries to guess a lang String from a connection.
	 * 
	 * @return
	 */
    private String setLang() {
        String type = this.connection.getContentType();
        if (type == null) return null;
        if (type.startsWith("application/rdf+xml") || type.startsWith("text/xml") || type.startsWith("application/xml") || type.startsWith("application/rss+xml") || type.startsWith("text/plain")) return "RDF/XML";
        if (type.startsWith("application/n3") || type.startsWith("application/x-turtle") || type.startsWith("text/rdf+n3")) return "N3";
        if (type.contains("html")) return "HTML";
        return type;
    }

    public synchronized void setMaxfilesize(int size) {
        this.maxfilesize = size;
    }

    public synchronized void setEnableRDFa(boolean r) {
        enableRDFa = r;
    }

    public synchronized void setRDFaTransformer(Transformer t) {
        transformerForRDFa = t;
    }

    public synchronized void setConnectTimeout(int t) {
        connectTimeout = t;
    }

    public synchronized void setReadTimeout(int t) {
        readTimeout = t;
    }

    public static String readout(InputStream in) throws IOException {
        StringBuffer out = new StringBuffer();
        byte[] b = new byte[4096];
        for (int n; (n = in.read(b)) != -1; ) {
            out.append(new String(b, 0, n));
        }
        return out.toString();
    }
}
