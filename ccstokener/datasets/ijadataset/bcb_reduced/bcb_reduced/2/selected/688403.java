package de.sonivis.tool.mwapiconnector;

import java.io.IOException;
import java.io.InputStreamReader;
import org.apache.http.HttpEntity;
import org.apache.http.HttpHost;
import org.apache.http.HttpResponse;
import org.apache.http.HttpStatus;
import org.apache.http.client.ClientProtocolException;
import org.apache.http.client.HttpClient;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.protocol.BasicHttpContext;
import org.apache.http.protocol.HttpContext;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * HTTP Get request for threaded execution by {@link ThreadedMWApiBot}.
 * <p>
 * This class wraps the functionality of a {@link HttpGet} request in a thread.
 * </p>
 * 
 * @author Andreas Erber
 * @version $Revision: 1299 $, $Date: 2009-12-01 08:58:19 -0500 (Tue, 01 Dec 2009) $
 */
public class HttpGetThread extends Thread {

    /**
	 * Logger.
	 */
    private static final Logger LOGGER = LoggerFactory.getLogger(HttpGetThread.class);

    /**
	 * {@link HttpClient} to execute the request.
	 */
    private final HttpClient httpClient;

    /**
	 * {@link HttpContext}.
	 */
    private final HttpContext context;

    /**
	 * {@link HttpGet} request to be executed.
	 */
    private final HttpGet httpget;

    /**
	 * The {@link IWikiApiRequest} that is to be trasnformed into the {@link HttpGet} request.
	 */
    private final IWikiApiRequest iwar;

    /**
	 * The {@link HttpHost} to send the request to.
	 */
    private final HttpHost target;

    /**
	 * Constructor.
	 * <p>
	 * All of the arguments are required to be not <code>null</code>. The specified
	 * {@link HttpClient} should be properly setup and ready for execution. The <a href="httpHost"
	 * target="_blank">target</a> is supposed to be readily configured <a href="httpHost}."
	 * target="_blank">The specified {@link HttpGet httpGet</a> request object is based on the
	 * specified {@link IWikiApiRequest}.
	 * </p>
	 * 
	 * @param httpClient
	 *            {@link HttpClient} ready to execute the query.
	 * @param target
	 *            {@link HttpHost} that is to be queried.
	 * @param httpget
	 *            Readily configured <a href="httpGet" target="_blank">HTTP GET</a> request.
	 * @param iwar
	 *            {@link IWikiApiRequest} the specified {@link HttpGet} was created from.
	 * @throws IllegalArgumentException
	 *             if one of the arguments is <code>null</code>.
	 */
    public HttpGetThread(final HttpClient httpClient, final HttpHost target, final HttpGet httpget, final IWikiApiRequest iwar) {
        if (httpClient == null) {
            throw new IllegalArgumentException("Argument httpClient is null.");
        }
        if (target == null) {
            throw new IllegalArgumentException("Argument target is null.");
        }
        if (httpget == null) {
            throw new IllegalArgumentException("Argument httpGet is null.");
        }
        if (iwar == null) {
            throw new IllegalArgumentException("Argument iwar is null.");
        }
        this.httpClient = httpClient;
        this.target = target;
        this.context = new BasicHttpContext();
        this.httpget = httpget;
        this.iwar = iwar;
    }

    /**
	 * Execute the {@link HttpGet} request.
	 * <p>
	 * The functionality is equal to {@link ThreadedMWApiBot#performRequest(IWikiApiRequest)}.
	 * </p>
	 * 
	 * @see ThreadedMWApiBot#performRequest(IWikiApiRequest)
	 */
    @Override
    public final void run() {
        HttpEntity entity = null;
        InputStreamReader inReader = null;
        try {
            final HttpResponse response = httpClient.execute(this.target, this.httpget, this.context);
            entity = response.getEntity();
            final int statusCode = response.getStatusLine().getStatusCode();
            this.iwar.setRequestResultStatus(statusCode);
            if (statusCode > HttpStatus.SC_OK) {
                if (LOGGER.isErrorEnabled()) {
                    LOGGER.error("Server Status: " + statusCode + " - " + response.getStatusLine().getReasonPhrase());
                }
                entity.consumeContent();
            } else {
                if (entity != null) {
                    inReader = new InputStreamReader(entity.getContent());
                    if (inReader == null) {
                        this.iwar.setXMLResult(null);
                    } else {
                        if (!inReader.getEncoding().substring(0, 2).equalsIgnoreCase(ThreadedMWApiBot.CHARSET.substring(0, 2))) {
                            if (LOGGER.isErrorEnabled()) {
                                LOGGER.error("The request body is not properly encoded - found " + inReader.getEncoding());
                            }
                        }
                        if (this.iwar.getFormat().equalsIgnoreCase("xml")) {
                            this.iwar.setXMLResult(XMLPreparer.getXMLRoot(inReader));
                        }
                    }
                }
            }
        } catch (ClientProtocolException e) {
            if (LOGGER.isErrorEnabled()) {
                LOGGER.error("Request failed due to client protocol problem. Query was: " + this.iwar.getExecutableQuery(), e);
            }
            httpget.abort();
        } catch (IOException e) {
            if (LOGGER.isErrorEnabled()) {
                LOGGER.error("Request failed due to IO problem. Query was: " + this.iwar.getExecutableQuery(), e);
            }
            httpget.abort();
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
        this.iwar.processRequestResult();
    }
}
