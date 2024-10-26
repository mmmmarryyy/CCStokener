package eu.etaxonomy.security.shibboleth.shibproxy;

import java.io.IOException;
import java.io.UnsupportedEncodingException;
import java.net.URI;
import java.net.URISyntaxException;
import java.net.URLEncoder;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.apache.http.Header;
import org.apache.http.HttpException;
import org.apache.http.HttpHost;
import org.apache.http.HttpRequest;
import org.apache.http.HttpRequestInterceptor;
import org.apache.http.HttpResponse;
import org.apache.http.HttpResponseInterceptor;
import org.apache.http.HttpStatus;
import org.apache.http.ProtocolException;
import org.apache.http.auth.AuthScope;
import org.apache.http.auth.Credentials;
import org.apache.http.client.RedirectHandler;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.client.methods.HttpHead;
import org.apache.http.client.methods.HttpOptions;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.client.methods.HttpPut;
import org.apache.http.client.methods.HttpTrace;
import org.apache.http.conn.ClientConnectionManager;
import org.apache.http.entity.StringEntity;
import org.apache.http.impl.client.DefaultHttpClient;
import org.apache.http.message.BasicHeader;
import org.apache.http.params.HttpParams;
import org.apache.http.protocol.BasicHttpContext;
import org.apache.http.protocol.HTTP;
import org.apache.http.protocol.HttpContext;
import org.jdom.JDOMException;

/**
 * TODO Class ShibbolethHttpClient
 * 
 * @author Lutz Suhrbier (suhrbier@inf.fu-berlin.de)
 * 
 */
public class ShibbolethHttpClient extends DefaultHttpClient implements HttpRequestInterceptor, HttpResponseInterceptor, RedirectHandler {

    /** The logger of class ShibbolethHttpClient. */
    private static final Log logger = LogFactory.getLog(ShibbolethHttpClient.class);

    /** URI of an identified IdP. */
    private URI IdPURI = null;

    /** URI of an SP. */
    private URI SPURI = null;

    public ShibbolethHttpClient(ClientConnectionManager ccm, HttpParams params, Credentials userCredentials) {
        super(ccm, params);
        this.getCredentialsProvider().setCredentials(new AuthScope(AuthScope.ANY), userCredentials);
        this.addResponseInterceptor(this);
        this.addRequestInterceptor(this);
        this.setRedirectHandler(this);
    }

    /**
     * Main method to handle the HTTP-request in order to realise the Shibboleth
     * authentication protocol based on the configuration parameters determined
     * by the
     * {@link #ShibbolethHttpClient(String, int, String, String, String, String)
     * constructor}. It handles all the Shibboleth communication and returns
     * only the final response to the caller.
     * 
     * @param request
     *            incoming request.
     * @return final response from the service.
     * @throws IOException
     * @throws InterruptedException 
     * @throws HttpException 
     * @throws IllegalStateException
     * @throws JDOMException
     * @throws URISyntaxException
     */
    public HttpResponse executeShibboleth(HttpHost target, HttpRequest request) throws IOException, HttpException, InterruptedException {
        HttpResponse response = null;
        request.removeHeaders(HTTP.CONTENT_LEN);
        BasicHttpContext shibContext = new BasicHttpContext();
        HttpResponse responseIdP = super.execute(target, request, shibContext);
        LogUtils.trace(logger, "CookieStore=" + getCookieStore().toString());
        if (isIdPResponse(responseIdP)) {
            IdPResponseParser idpParser = new IdPResponseParser(responseIdP);
            try {
                response = super.execute(target, createSPRequest(idpParser), shibContext);
                if (isShibbolethSPToResourceRedirect(response, shibContext)) response = super.execute(target, request, shibContext);
            } catch (URISyntaxException e) {
                String message = "Error handling request to " + idpParser.getTarget() + ": " + e.getMessage() + "(" + e.getClass().getName() + ")";
                LogUtils.fatal(logger, message);
                throw new HttpException(message);
            }
        } else response = responseIdP;
        LogUtils.trace(logger, "CookieStore2=" + getCookieStore().toString());
        return response;
    }

    /**
     * Analyses, if the response is from an IdP-instance.
     * 
     * @param response
     *            response to be analysed.
     * @return true, if IdP response, false otherwise.
     */
    private boolean isIdPResponse(HttpResponse response) {
        boolean ret = false;
        if (response.getStatusLine().getStatusCode() == HttpStatus.SC_OK) if (response.containsHeader("Set-Cookie")) if (response.getHeaders("Set-Cookie")[0].getValue().indexOf("JSESSIONID") >= 0) ret = true;
        return ret;
    }

    /**
     * Creates the request to the IdP based on the response of the IdP. This
     * way, it adds the Referrer header to the original request headers.
     * 
     * @param idpParser
     *            parser result from an IdP response
     * @param requestHeaders
     *            original request headers.
     * @return created request
     * @throws URISyntaxException
     * @throws UnsupportedEncodingException
     */
    private HttpRequest createSPRequest(IdPResponseParser idpParser) throws URISyntaxException, UnsupportedEncodingException {
        HttpRequest request = null;
        if (idpParser.getParamMethod().equalsIgnoreCase("post")) {
            request = new HttpPost(idpParser.getParamAction());
            ((HttpPost) request).setEntity(createEntity(idpParser));
        } else if (idpParser.getParamMethod().equalsIgnoreCase("get")) request = new HttpGet(idpParser.getParamAction()); else if (idpParser.getParamMethod().equalsIgnoreCase("head")) request = new HttpHead(idpParser.getParamAction()); else if (idpParser.getParamMethod().equalsIgnoreCase("option")) request = new HttpOptions(idpParser.getParamAction()); else if (idpParser.getParamMethod().equalsIgnoreCase("put")) {
            request = new HttpPut(idpParser.getParamAction());
            ((HttpPut) request).setEntity(createEntity(idpParser));
        } else if (idpParser.getParamMethod().equalsIgnoreCase("trace")) request = new HttpTrace(idpParser.getParamAction()); else LogUtils.fatal(logger, "Unknown Method: " + idpParser.getParamMethod());
        request.addHeader(new BasicHeader("Referrer", IdPURI.toASCIIString()));
        return request;
    }

    /**
     * Generates the entity for the SP request generated based on the
     * SAMLResponse included in the IdP response.
     * 
     * @param idpParser
     *            parser result from an IdP response
     * @return generated entity.
     * @throws UnsupportedEncodingException
     */
    private StringEntity createEntity(IdPResponseParser idpParser) throws UnsupportedEncodingException {
        StringEntity entity = new StringEntity("TARGET=" + idpParser.getTarget() + "&SAMLResponse=" + URLEncoder.encode(idpParser.getSAMLResponse(), "UTF-8"));
        entity.setContentType("application/x-www-form-urlencoded");
        return entity;
    }

    /**
     * Implements the {@link HttpRequestInterceptor HttpRequestInterceptor}
     * interface. Currently, just logs the request.
     * 
     * @see org.apache.http.HttpRequestInterceptor#process(org.apache.http.HttpRequest,
     *      org.apache.http.protocol.HttpContext)
     */
    public void process(HttpRequest request, HttpContext context) {
        LogUtils.trace(logger, "process(HttpRequest, HttpContext - start");
        LogUtils.trace(logger, LogUtils.logContext(context) + "\n" + LogUtils.logRequest(request));
        LogUtils.trace(logger, "process(HttpRequest, HttpContext) - end");
    }

    /**
     * Implements the {@link HttpResponseInterceptor HttpResponseInterceptor}
     * interface. Currently, just logs the response.
     * 
     * @see org.apache.http.HttpResponseInterceptor#process(org.apache.http.HttpResponse,
     *      org.apache.http.protocol.HttpContext)
     */
    public void process(HttpResponse response, HttpContext context) {
        LogUtils.trace(logger, "process(HttpResponse, HttpContext - start");
        LogUtils.trace(logger, LogUtils.logContext(context) + "\n" + LogUtils.logResponse(response));
        LogUtils.trace(logger, "process(HttpResponse, HttpContext) - end");
    }

    /**
     * Analyses, if redirect request of the response should be granted. It is
     * granted, if this this is an Shibboleth response either from the SP,
     * requesting a redirect to the IdP, or if this a response from the IdP,
     * which must be redirected to the SP.
     * 
     * @see org.apache.http.client.RedirectHandler#isRedirectRequested(org.apache.http.HttpResponse,
     *      org.apache.http.protocol.HttpContext)
     */
    public boolean isRedirectRequested(HttpResponse response, HttpContext context) {
        boolean ret = false;
        if (isShibbolethSPToIdPRedirect(response, context)) {
            if (response.containsHeader("Location")) {
                LogUtils.debug(logger, "Redirect granted to " + response.getHeaders("Location")[0].getValue());
                ret = true;
            } else LogUtils.debug(logger, "Redirect denied - Missing Location Header.");
        } else LogUtils.debug(logger, "Redirect denied - Not a Shibboleth response.\n" + LogUtils.logResponse(response));
        return ret;
    }

    /**
     * Determines, if the response allows for a redirect from an SP to an IdP.
     * Therefore, the response must include an "Set-Cookie" header with a value
     * starting with "_shibstate_".
     * 
     * @param response
     *            response to be analysed.
     * @param context
     * @return true or false.
     */
    private boolean isShibbolethSPToIdPRedirect(HttpResponse response, HttpContext context) {
        boolean ret = false;
        if (response.containsHeader("Set-Cookie")) {
            Header[] headers = response.getHeaders("Set-Cookie");
            int i = 0;
            while ((i < headers.length)) {
                if (headers[i].getValue().indexOf("_shibstate_") >= 0) {
                    LogUtils.trace(logger, "SP to IdP Redirect (_shibstate_ cookie found)");
                    ret = true;
                    break;
                } else i++;
            }
        }
        return ret;
    }

    /**
     * Determines, if the response allows for a redirect from an SP to the
     * originally requested resource. Therefore, the response must include
     * "Set-Cookie" headers with values containing "_saml_idp" and
     * "_shibsession_".
     * 
     * @param response
     *            response to be analysed.
     * @param context
     * @return true or false.
     */
    private boolean isShibbolethSPToResourceRedirect(HttpResponse response, HttpContext context) {
        boolean ret = false;
        if (response.containsHeader("Set-Cookie")) {
            Header[] headers = response.getHeaders("Set-Cookie");
            boolean saml_idp = false;
            boolean shibsession = false;
            int i = 0;
            while ((i < headers.length)) {
                if (headers[i].getValue().indexOf("_saml_idp") >= 0) {
                    saml_idp = true;
                }
                if (headers[i].getValue().indexOf("_shibsession_") >= 0) {
                    shibsession = true;
                }
                if (saml_idp && shibsession) {
                    LogUtils.trace(logger, "SP to Resource Redirect (_saml_idp_ &_shibsession_ cookies found)");
                    ret = true;
                    break;
                } else i++;
            }
        }
        return ret;
    }

    /**
     * Determines the URL to which a redirection granted within
     * {@link #isRedirectRequested(HttpResponse, HttpContext)
     * isRedirectRequested(HttpResponse, HttpContext)} executed. This depends on
     * the nature of the response (SPtoIdP, or final SPtoSP redirection).
     * 
     * @see org.apache.http.client.RedirectHandler#getLocationURI(org.apache.http.HttpResponse,
     *      org.apache.http.protocol.HttpContext)
     */
    public URI getLocationURI(HttpResponse response, HttpContext context) throws ProtocolException {
        URI returnURI = null;
        Header[] locationHeaders = response.getHeaders("Location");
        if ((locationHeaders != null) && (locationHeaders.length > 0)) {
            try {
                returnURI = new URI(locationHeaders[0].getValue());
            } catch (URISyntaxException e) {
                throw new ProtocolException("URISyntaxException \"" + e.getMessage() + "\" in getLocationURI(HttpResponse, HttpContext).");
            }
        }
        if (isShibbolethSPToIdPRedirect(response, context)) {
            IdPURI = returnURI;
            LogUtils.trace(logger, "IdP URI is " + IdPURI.toASCIIString() + ".");
        } else if (isShibbolethSPToResourceRedirect(response, context)) {
            SPURI = returnURI;
            LogUtils.trace(logger, "SP URI is " + SPURI.toASCIIString() + ".");
        } else {
            LogUtils.fatal(logger, "This point should never be reached.");
        }
        LogUtils.debug(logger, "Request redirected to " + returnURI.toASCIIString() + ".");
        return returnURI;
    }
}
