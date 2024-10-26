package org.ikasan.platform.service;

import java.io.IOException;
import org.apache.http.auth.AuthScheme;
import org.apache.http.auth.AuthScope;
import org.apache.http.auth.AuthState;
import org.apache.http.auth.Credentials;
import org.apache.http.auth.UsernamePasswordCredentials;
import org.apache.http.HttpException;
import org.apache.http.HttpHost;
import org.apache.http.HttpRequest;
import org.apache.http.HttpRequestInterceptor;
import org.apache.http.HttpResponse;
import org.apache.http.HttpStatus;
import org.apache.http.client.CredentialsProvider;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.client.protocol.ClientContext;
import org.apache.http.impl.auth.BasicScheme;
import org.apache.http.impl.client.DefaultHttpClient;
import org.apache.http.protocol.BasicHttpContext;
import org.apache.http.protocol.ExecutionContext;
import org.apache.http.protocol.HttpContext;
import org.apache.log4j.Logger;
import org.ikasan.platform.service.HousekeeperService;

/**
 * A HTTP Implementation of the Housekeeper interface
 * 
 * @author Ikasan Development Team
 */
public class HousekeeperServiceHttpImpl implements HousekeeperService {

    /** Logger for this class */
    private static Logger logger = Logger.getLogger(HousekeeperServiceHttpImpl.class);

    /** The URL to execute the Wiretap Event Housekeeping */
    private String wiretapEventHousekeepingUrl;

    /** User name for login */
    private String wiretapEventHousekeepingUserName;

    /** Password required for log in */
    private String wiretapEventHousekeepingPassword;

    /**
	 * Constructor
	 * 
	 * @param wiretapEventHousekeepingUrl
	 *            - wiretapEventHousekeepingUrl to set
	 * @param wiretapEventHousekeepingUserName - userName
	 * @param wiretapEventHousekeepingPassword - password
	 */
    public HousekeeperServiceHttpImpl(String wiretapEventHousekeepingUrl, String wiretapEventHousekeepingUserName, String wiretapEventHousekeepingPassword) {
        if (wiretapEventHousekeepingUrl == null) {
            throw new IllegalArgumentException("wiretapEventHouseKeeping Url is null");
        }
        if (wiretapEventHousekeepingUserName == null) {
            throw new IllegalArgumentException("wiretapEventHouseKeeping User name is null");
        }
        if (wiretapEventHousekeepingPassword == null) {
            throw new IllegalArgumentException("wiretapEventHouseKeeping Password is null");
        }
        this.wiretapEventHousekeepingUrl = wiretapEventHousekeepingUrl;
        this.wiretapEventHousekeepingUserName = wiretapEventHousekeepingUserName;
        this.wiretapEventHousekeepingPassword = wiretapEventHousekeepingPassword;
    }

    /**
	 * Housekeeps Wiretapped Events
	 * 
	 * TODO Error handling is lazy and captures any exception, this may actually
	 * be OK but needs review.
	 */
    public void housekeepWiretapEvents() {
        DefaultHttpClient httpclient = new DefaultHttpClient();
        Credentials defaultcreds = new UsernamePasswordCredentials(this.wiretapEventHousekeepingUserName, this.wiretapEventHousekeepingPassword);
        httpclient.getCredentialsProvider().setCredentials(AuthScope.ANY, defaultcreds);
        BasicHttpContext localcontext = new BasicHttpContext();
        BasicScheme basicAuth = new BasicScheme();
        localcontext.setAttribute("preemptive-auth", basicAuth);
        httpclient.addRequestInterceptor(new PreemptiveAuth(), 0);
        HttpPost httppost = new HttpPost(this.wiretapEventHousekeepingUrl);
        try {
            HttpResponse response = httpclient.execute(httppost, localcontext);
            int statusCode = response.getStatusLine().getStatusCode();
            if (!(statusCode == HttpStatus.SC_MOVED_TEMPORARILY || statusCode == HttpStatus.SC_OK)) {
                logger.error("Call failed, Status Code = [" + statusCode + "]");
                throw new HttpException();
            }
            logger.info("housekeepWiretapEvents was called successfully.");
        } catch (IOException e) {
            logger.error("Call to housekeep failed.", e);
        } catch (HttpException e) {
            logger.error("Call to housekeep failed.", e);
        } finally {
            httpclient.getConnectionManager().shutdown();
        }
    }

    /** Helper class to sort out preemptive auth */
    static class PreemptiveAuth implements HttpRequestInterceptor {

        public void process(final HttpRequest request, final HttpContext context) throws HttpException, IOException {
            AuthState authState = (AuthState) context.getAttribute(ClientContext.TARGET_AUTH_STATE);
            if (authState.getAuthScheme() == null) {
                AuthScheme authScheme = (AuthScheme) context.getAttribute("preemptive-auth");
                CredentialsProvider credsProvider = (CredentialsProvider) context.getAttribute(ClientContext.CREDS_PROVIDER);
                HttpHost targetHost = (HttpHost) context.getAttribute(ExecutionContext.HTTP_TARGET_HOST);
                if (authScheme != null) {
                    Credentials creds = credsProvider.getCredentials(new AuthScope(targetHost.getHostName(), targetHost.getPort()));
                    if (creds == null) {
                        throw new HttpException("No credentials for preemptive authentication");
                    }
                    authState.setAuthScheme(authScheme);
                    authState.setCredentials(creds);
                }
            }
        }
    }
}
