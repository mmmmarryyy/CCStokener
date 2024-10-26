package se.ramfelt.psn.web.eu;

import java.io.IOException;
import java.net.MalformedURLException;
import java.util.ArrayList;
import java.util.List;
import org.apache.http.HttpEntity;
import org.apache.http.HttpResponse;
import org.apache.http.NameValuePair;
import org.apache.http.client.ClientProtocolException;
import org.apache.http.client.HttpClient;
import org.apache.http.client.entity.UrlEncodedFormEntity;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.client.params.HttpClientParams;
import org.apache.http.client.protocol.ClientContext;
import org.apache.http.impl.client.BasicCookieStore;
import org.apache.http.impl.client.DefaultHttpClient;
import org.apache.http.message.BasicNameValuePair;
import org.apache.http.params.BasicHttpParams;
import org.apache.http.params.HttpParams;
import org.apache.http.protocol.BasicHttpContext;
import org.apache.http.protocol.HTTP;
import se.ramfelt.psn.web.BadRequestResponseException;
import se.ramfelt.psn.web.ForbiddenResponseException;
import se.ramfelt.psn.web.FriendListRetrievalException;
import se.ramfelt.psn.web.UnauthorizedResponseException;
import se.ramfelt.psn.web.UnexpectedResponseCodeException;

public class EuPlaystationSite {

    private final String username;

    private final String password;

    private final HttpClient httpClient;

    private BasicHttpContext context;

    private BasicCookieStore cookieStore;

    private boolean isLoggedIn = false;

    public EuPlaystationSite(String username, String password) {
        this.username = username;
        this.password = password;
        HttpParams httpParams = new BasicHttpParams();
        HttpClientParams.setRedirecting(httpParams, false);
        httpClient = new DefaultHttpClient(httpParams);
        cookieStore = new BasicCookieStore();
        context = new BasicHttpContext();
        context.setAttribute(ClientContext.COOKIE_STORE, cookieStore);
    }

    public void login() throws MalformedURLException, IOException, UnexpectedResponseCodeException, UnauthorizedResponseException, ForbiddenResponseException, BadRequestResponseException {
        List<NameValuePair> parametersBody = new ArrayList<NameValuePair>();
        parametersBody.add(new BasicNameValuePair("loginName", username));
        parametersBody.add(new BasicNameValuePair("password", password));
        parametersBody.add(new BasicNameValuePair("returnURL", "https://secure.eu.playstation.com/sign-in/confirmation/"));
        HttpPost postReq = new HttpPost("https://store.playstation.com/external/login.action");
        postReq.setEntity(new UrlEncodedFormEntity(parametersBody, HTTP.UTF_8));
        HttpResponse response = httpClient.execute(postReq, context);
        response.getEntity().consumeContent();
        switch(response.getStatusLine().getStatusCode()) {
            case 302:
                HttpGet getReq = new HttpGet(response.getHeaders("Location")[0].getValue());
                response = httpClient.execute(getReq, context);
                response.getEntity().consumeContent();
                isLoggedIn = true;
                break;
            case 200:
                throw new UnauthorizedResponseException();
            case 401:
            case 403:
                throw new ForbiddenResponseException();
            case 400:
            case 402:
            case 404:
            case 405:
            case 406:
            case 410:
                throw new BadRequestResponseException();
            default:
                throw new UnexpectedResponseCodeException(response.getStatusLine().getStatusCode());
        }
    }

    public HttpEntity getFriendListXml() throws IOException, FriendListRetrievalException {
        HttpGet getReq = new HttpGet("https://secure.eu.playstation.com/ajax/mypsn/friend/presence/");
        HttpResponse response = httpClient.execute(getReq, context);
        if (response.getStatusLine().getStatusCode() == 200) {
            return response.getEntity();
        }
        response.getEntity().consumeContent();
        throw new FriendListRetrievalException("Can not retrieve friend list without logging in.");
    }

    public HttpEntity getHttpEntity(String path) throws ClientProtocolException, IOException {
        HttpGet getReq = new HttpGet("https://secure.eu.playstation.com/" + path);
        return httpClient.execute(getReq).getEntity();
    }

    public boolean isLoggedIn() {
        return isLoggedIn;
    }
}
