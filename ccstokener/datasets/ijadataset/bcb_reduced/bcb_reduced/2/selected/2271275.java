package froxy.urlfetch;

import java.io.IOException;
import java.util.Enumeration;
import javax.servlet.http.HttpServletRequest;
import org.apache.commons.codec.binary.Base64;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.apache.http.HttpHost;
import org.apache.http.HttpResponse;
import org.apache.http.HttpStatus;
import org.apache.http.client.ClientProtocolException;
import org.apache.http.client.methods.HttpUriRequest;
import org.apache.http.client.params.ClientPNames;
import org.apache.http.conn.params.ConnRoutePNames;
import org.apache.http.cookie.Cookie;
import org.apache.http.cookie.CookieOrigin;
import org.apache.http.cookie.CookieSpec;
import org.apache.http.cookie.CookieSpecFactory;
import org.apache.http.cookie.MalformedCookieException;
import org.apache.http.impl.client.DefaultHttpClient;
import org.apache.http.impl.cookie.BrowserCompatSpec;
import org.apache.http.params.HttpParams;
import froxy.client.GUIActoin;
import froxy.client.Helper;
import froxy.client.SwingUI;
import froxy.handler.ProxyHandler;

public abstract class DirectFetch implements UrlFetch {

    private static Log logger = LogFactory.getLog(DirectPostUrlFetch.class);

    private GUIActoin GUI;

    public HttpResponse fetch(HttpServletRequest request) throws IOException {
        GUI = SwingUI.getApplicatoin();
        DefaultHttpClient httpclient = new DefaultHttpClient();
        CookieSpecFactory csf = new CookieSpecFactory() {

            public CookieSpec newInstance(HttpParams params) {
                return new BrowserCompatSpec() {

                    @Override
                    public void validate(Cookie cookie, CookieOrigin origin) throws MalformedCookieException {
                    }
                };
            }
        };
        if (Helper.useProxy()) {
            HttpHost proxy = new HttpHost(Helper.getProxyServer(), Helper.getProxyPort());
            httpclient.getParams().setParameter(ConnRoutePNames.DEFAULT_PROXY, proxy);
        }
        httpclient.getCookieSpecs().register("easy", csf);
        httpclient.getParams().setParameter(ClientPNames.COOKIE_POLICY, "easy");
        String currentRemoteGAEHost = Helper.getRemoteServer();
        try {
            HttpUriRequest httpRequest = createRequest(request);
            addHeader(request, httpRequest);
            HttpResponse response = httpclient.execute(httpRequest);
            if (response.getStatusLine().getStatusCode() == HttpStatus.SC_FORBIDDEN) {
                challengeProxy(currentRemoteGAEHost);
            }
            logger.info(Helper.count.incrementAndGet() + " Response received from " + request.getRequestURL().toString() + ", status is " + response.getStatusLine());
            GUI.updateFetchCount();
            return response;
        } catch (ClientProtocolException e) {
            logger.error("Fetch ClientProtocol Error", e);
            throw e;
        } catch (IOException e) {
            logger.error("Fetch IO Error", e);
            throw e;
        }
    }

    private void challengeProxy(String server) {
        Helper.remoteLock.lock();
        if (server.equals(Helper.getRemoteServer())) {
            if (++Helper.REMOTE_SERVER_FAIL > Helper.REMOTE_SERVER_FAIL_THRESHOLD) {
                Helper.changeProxyServer();
                Helper.REMOTE_SERVER_FAIL = 0;
                logger.info("Auto update the remote server to " + Helper.getRemoteServer());
                GUI.updateRemoteServer();
            }
        }
        Helper.remoteLock.unlock();
    }

    protected abstract HttpUriRequest createRequest(HttpServletRequest request);

    protected void addHeader(HttpServletRequest request, HttpUriRequest httpRequest) {
        Enumeration<?> headerNames = request.getHeaderNames();
        while (headerNames.hasMoreElements()) {
            String headerName = (String) headerNames.nextElement();
            if (headerName.equalsIgnoreCase(Helper.HOST) || headerName.equalsIgnoreCase(Helper.Content_Length)) {
                continue;
            }
            httpRequest.addHeader(headerName, request.getHeader(headerName));
        }
        if (!ProxyHandler.isEncript()) {
            httpRequest.addHeader(Helper.ENCRIPT_HEADER, Helper.FALSE);
        }
    }

    protected String base64URL(String URL) {
        String result = disarrange(new String(new Base64().encode((URL.getBytes()))));
        return Helper.getRemoteServer() + result;
    }

    private String disarrange(String URL) {
        String result = Helper.EMPTYSTRING;
        for (int i = 0; i < URL.length(); i = i + 2) {
            if (i + 1 < URL.length()) {
                result = result + URL.charAt(i + 1);
                result = result + URL.charAt(i);
            } else {
                result = result + URL.charAt(i);
            }
        }
        return result;
    }

    protected String addQueryString(HttpServletRequest request, String askedURL) {
        if (request.getQueryString() != null && !request.getQueryString().isEmpty()) {
            askedURL = askedURL + "?" + request.getQueryString();
        }
        return askedURL;
    }
}
