package se.ramfelt.psn.web.us;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.net.MalformedURLException;
import java.util.ArrayList;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import org.apache.http.Header;
import org.apache.http.HttpEntity;
import org.apache.http.HttpResponse;
import org.apache.http.NameValuePair;
import org.apache.http.client.ClientProtocolException;
import org.apache.http.client.HttpClient;
import org.apache.http.client.entity.UrlEncodedFormEntity;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.client.methods.HttpRequestBase;
import org.apache.http.client.params.HttpClientParams;
import org.apache.http.client.protocol.ClientContext;
import org.apache.http.impl.client.BasicCookieStore;
import org.apache.http.message.BasicNameValuePair;
import org.apache.http.protocol.BasicHttpContext;
import org.apache.http.protocol.HTTP;
import se.ramfelt.psn.web.BadRequestResponseException;
import se.ramfelt.psn.web.ForbiddenResponseException;
import se.ramfelt.psn.web.FriendListRetrievalException;
import se.ramfelt.psn.web.ServiceUnavailableException;
import se.ramfelt.psn.web.UnauthorizedResponseException;
import se.ramfelt.psn.web.UnexpectedResponseCodeException;

public class UsPlaystationSite {

    private static final Pattern PATTERN_FRIEND_SEPARATOR = Pattern.compile("\"(.+?)\"");

    private static final Pattern PATTERN_FRIENDS_LIST = Pattern.compile("var gamers = '\\[(.*)\\]'.evalJSON()");

    private static final String HTTP_HEADER_SET_COOKIE = "Set-Cookie";

    private static final String HTTP_HEADER_X_REQUESTED_WITH = "X-Requested-With";

    private static final String HTTP_HEADER_REFERER = "Referer";

    private static final String SESSION_ID = "?sessionId=";

    private final HttpClient httpClient;

    private BasicHttpContext context;

    private BasicCookieStore cookieStore;

    private boolean isLoggedIn = false;

    private String sessionId;

    public UsPlaystationSite(HttpClient httpClient) {
        this.httpClient = httpClient;
        HttpClientParams.setRedirecting(httpClient.getParams(), false);
        cookieStore = new BasicCookieStore();
        context = new BasicHttpContext();
        context.setAttribute(ClientContext.COOKIE_STORE, cookieStore);
    }

    public void login(String username, String password) throws MalformedURLException, IOException, UnexpectedResponseCodeException, UnauthorizedResponseException, ForbiddenResponseException, BadRequestResponseException, ServiceUnavailableException {
        HttpPost postRequest = new HttpPost("https://store.playstation.com/external/login.action");
        addDefaultHeaders(postRequest);
        List<NameValuePair> parametersBody = new ArrayList<NameValuePair>();
        parametersBody.add(new BasicNameValuePair("loginName", username));
        parametersBody.add(new BasicNameValuePair("password", password));
        parametersBody.add(new BasicNameValuePair("returnURL", "https://us.playstation.com/uwps/PSNTicketRetrievalGenericServlet"));
        postRequest.setEntity(new UrlEncodedFormEntity(parametersBody, HTTP.UTF_8));
        HttpResponse response = httpClient.execute(postRequest, context);
        response.getEntity().consumeContent();
        String location;
        switch(response.getStatusLine().getStatusCode()) {
            case 302:
                location = response.getHeaders("Location")[0].getValue();
                sessionId = location.substring(location.indexOf(SESSION_ID) + SESSION_ID.length());
                HttpGet request = new HttpGet("http://us.playstation.com/uwps/PSNTicketRetrievalGenericServlet?psnAuth=true&sessionId=" + sessionId);
                addDefaultHeaders(request);
                response = httpClient.execute(request, context);
                response.getEntity().consumeContent();
                isLoggedIn = isLoggedIn(response);
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
            case 501:
            case 502:
                throw new BadRequestResponseException();
            case 500:
            case 503:
                throw new ServiceUnavailableException();
            default:
                throw new UnexpectedResponseCodeException(response.getStatusLine().getStatusCode());
        }
    }

    public ArrayList<String> getFriendNames() throws ClientProtocolException, IOException, FriendListRetrievalException {
        HttpGet request = new HttpGet("http://us.playstation.com/playstation/psn/profile/friends?id=" + Math.random());
        addDefaultHeaders(request);
        request.addHeader(HTTP_HEADER_REFERER, "http://us.playstation.com/myfriends/index.htm");
        HttpResponse response = httpClient.execute(request, context);
        ArrayList<String> names = getFriendName(response.getEntity().getContent());
        response.getEntity().consumeContent();
        return names;
    }

    public String getFriendSummary(String name) throws ClientProtocolException, IOException {
        HttpGet request = new HttpGet("http://us.playstation.com/playstation/psn/profile/get_gamer_summary_data?id=" + name);
        addDefaultHeaders(request);
        request.addHeader(HTTP_HEADER_REFERER, "http://us.playstation.com/playstation/psn/profile/friends?id=" + Math.random());
        request.addHeader(HTTP_HEADER_X_REQUESTED_WITH, "XMLHttpRequest");
        HttpResponse response = httpClient.execute(request, context);
        String jsonString = getInputStreamAsString(response.getEntity().getContent());
        response.getEntity().consumeContent();
        return jsonString;
    }

    public HttpEntity getHttpEntity(String url) throws ClientProtocolException, IOException {
        HttpGet request = new HttpGet(url);
        addDefaultHeaders(request);
        request.addHeader(HTTP_HEADER_REFERER, "http://us.playstation.com/playstation/psn/profile/friends?id=" + Math.random());
        HttpResponse response = httpClient.execute(request, context);
        return response.getEntity();
    }

    private void addDefaultHeaders(HttpRequestBase request) {
        request.addHeader("User-Agent", "Mozilla/5.0 (Windows; U; Windows NT 5.1; sv-SE; rv:1.9.0.14) Gecko/2009082707 Firefox/3.0.14 (.NET CLR 3.5.30729)");
        request.addHeader("Accept", "text/html,application/xhtml+xml,application/xml;q=0.9,*/*;q=0.8");
        request.addHeader("Accept-Language", "sv-se,sv;q=0.8,en-us;q=0.5,en;q=0.3");
        request.addHeader("Accept-Charset", "ISO-8859-1,utf-8;q=0.7,*;q=0.7");
    }

    private boolean isLoggedIn(HttpResponse response) {
        int i = 0;
        Header[] headers = response.getHeaders(HTTP_HEADER_SET_COOKIE);
        for (Header header : headers) {
            String value = header.getValue();
            if (value.startsWith("PSNS2STICKET") || value.startsWith("TICKET") || value.startsWith("userinfo") || value.startsWith("ps-qa.si")) {
                i++;
            }
        }
        return (i == 4);
    }

    public boolean isLoggedIn() {
        return isLoggedIn;
    }

    private ArrayList<String> getFriendName(InputStream input) throws IOException, FriendListRetrievalException {
        BufferedReader reader = new BufferedReader(new InputStreamReader(input));
        String line = reader.readLine();
        while (line != null) {
            Matcher matcher = PATTERN_FRIENDS_LIST.matcher(line);
            if (matcher.find()) {
                ArrayList<String> list = new ArrayList<String>();
                matcher = PATTERN_FRIEND_SEPARATOR.matcher(matcher.group(1));
                while (matcher.find()) {
                    list.add(matcher.group(1));
                }
                return list;
            }
            line = reader.readLine();
        }
        throw new FriendListRetrievalException("Could not determine the names of your PSN friends.");
    }

    private String getInputStreamAsString(InputStream source) throws IOException {
        BufferedReader reader = new BufferedReader(new InputStreamReader(source));
        StringBuilder builder = new StringBuilder();
        String line = reader.readLine();
        while (line != null) {
            builder.append(line);
            builder.append("\n");
            line = reader.readLine();
        }
        return builder.toString();
    }
}
