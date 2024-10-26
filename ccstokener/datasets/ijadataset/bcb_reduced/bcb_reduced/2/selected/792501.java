package net.sf.urlchecker.v2.commands;

import java.io.IOException;
import net.sf.urlchecker.commands.Result;
import net.sf.urlchecker.v2.communication.CommunicationFactory;
import org.apache.commons.configuration.ConfigurationException;
import org.apache.commons.lang.ArrayUtils;
import org.apache.http.HttpResponse;
import org.apache.http.client.ClientProtocolException;
import org.apache.http.client.HttpClient;
import org.apache.http.client.methods.HttpRequestBase;
import org.apache.http.protocol.BasicHttpContext;
import org.apache.http.protocol.HttpContext;
import org.apache.log4j.Logger;

/**
 * The Class CheckUrlsProcess.
 * 
 * <p>
 * <b> $Id: CheckUrlsProcess.java 186 2010-12-18 09:14:15Z georgosn $</b>
 * </p>
 * 
 * @author $LastChangedBy: georgosn $
 * @version $LastChangedRevision: 186 $
 */
public class CheckUrlsProcess implements Runnable {

    /** The Constant LOGGER. */
    private static final Logger LOGGER = Logger.getLogger(CheckUrlsProcess.class.getName());

    /** The client. */
    private final HttpClient client;

    /** The result. */
    private final Result result;

    /** The valid codes. */
    private final Integer[] validCodes;

    /** The context. */
    private final HttpContext context;

    private final HttpRequestBase method;

    /**
     * Instantiates a new check URLs process.
     * 
     * @param aClient
     *            the a client
     * @param localContext
     *            the local context
     * @param res
     *            the res
     * @throws org.apache.commons.configuration.ConfigurationException
     *             the configuration exception
     */
    public CheckUrlsProcess(HttpClient aClient, HttpContext localContext, Result res) throws ConfigurationException {
        client = aClient;
        result = res;
        validCodes = CommunicationFactory.getInstance().getOnlyValidCodes(result.getTarget());
        method = CommunicationFactory.getInstance().getMethod(result.getTarget());
        if (null == localContext) {
            context = new BasicHttpContext();
        } else {
            context = localContext;
        }
    }

    /**
     * Gets the result.
     * 
     * @return the result
     */
    public Result getResult() {
        return result;
    }

    /**
     * <p>
     * run
     * </p>
     */
    public void run() {
        result.setValid(false);
        try {
            final HttpResponse response = client.execute(method, context);
            result.setValid(ArrayUtils.contains(validCodes, response.getStatusLine().getStatusCode()));
            result.setResult(response.getStatusLine().getStatusCode());
        } catch (final ClientProtocolException e) {
            LOGGER.error(e);
            result.setValid(false);
        } catch (final IOException e) {
            LOGGER.error(e);
            result.setValid(false);
        }
    }
}
