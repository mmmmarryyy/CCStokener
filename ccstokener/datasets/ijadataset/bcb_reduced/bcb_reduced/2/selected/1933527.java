package au.edu.diasb.annotation.danno.protocol;

import java.io.IOException;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import org.apache.http.Header;
import org.apache.http.HttpResponse;
import org.apache.http.ProtocolException;
import org.apache.http.StatusLine;
import org.apache.http.client.ClientProtocolException;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.client.methods.HttpUriRequest;
import org.apache.http.cookie.Cookie;
import org.apache.http.impl.client.DefaultHttpClient;
import org.apache.http.impl.cookie.BasicClientCookie2;
import org.apache.log4j.Logger;

/**
 * This DannoClientFactory starts by logging into the server using
 * a specified username and password.  This is a bit sketchy at the moment.
 * 
 * @author scrawley
 */
public class LoginDannoClientFactory implements DannoClientFactory {

    public static class LoggedInDannoClient extends DannoClientBase {

        private final String cookie;

        private final String cookie2;

        private final String auth;

        public LoggedInDannoClient(String cookie, String cookie2, String auth, boolean useHttps) {
            super(useHttps);
            this.cookie = cookie;
            this.cookie2 = cookie2;
            this.auth = auth;
        }

        @Override
        protected void doExecute(HttpUriRequest request) throws IOException, ClientProtocolException, ProtocolException {
            if (cookie != null) {
                request.addHeader("Cookie", cookie);
            }
            if (cookie2 != null) {
                request.addHeader("Cookie2", cookie2);
            }
            if (auth != null) {
                request.addHeader("Authorization", auth);
            }
            super.doExecute(request);
        }
    }

    private String user;

    private String password;

    private String loginUrl;

    protected String cookie;

    protected String cookie2;

    protected String auth;

    private boolean loggedIn;

    private boolean useHttps;

    private final Logger logger = Logger.getLogger(this.getClass());

    public LoginDannoClientFactory() {
    }

    public LoginDannoClientFactory(String loginUrl, String user, String password) {
        super();
        this.user = user;
        this.password = password;
        this.loginUrl = loginUrl;
    }

    protected Logger getLogger() {
        return logger;
    }

    /**
     * Setting this property to true will cause all requests made using clients
     * created by this factory to be made using HTTPS.
     * 
     * @param useHttps the property value
     */
    public final void setUseHttps(boolean useHttps) {
        this.useHttps = useHttps;
    }

    public final void setUser(String user) {
        this.user = user;
    }

    public final void setPassword(String password) {
        this.password = password;
    }

    public final void setLoginUrl(String loginUrl) {
        this.loginUrl = loginUrl;
    }

    @Override
    public DannoClient createClient(HttpServletRequest request, String uriString) throws ClientProtocolException, IOException {
        if (!loggedIn) {
            doAuthentication();
            loggedIn = true;
        }
        return new LoggedInDannoClient(cookie, cookie2, auth, useHttps);
    }

    protected void doAuthentication() throws ClientProtocolException, IOException {
        if (loginUrl == null || user == null) {
            return;
        }
        String url = loginUrl + "?j_username=" + user + "&j_password=" + (password == null ? "" : password);
        DefaultHttpClient client = new DefaultHttpClient();
        try {
            HttpPost post = new HttpPost(url);
            if (logger.isDebugEnabled()) {
                logger.debug("Attempting login: request is " + post.toString());
            }
            HttpResponse hr = client.execute(post);
            StatusLine sl = hr.getStatusLine();
            switch(sl.getStatusCode()) {
                case HttpServletResponse.SC_OK:
                    break;
                case HttpServletResponse.SC_MOVED_TEMPORARILY:
                    Header h = hr.getFirstHeader("Location");
                    if (h != null && h.getValue().contains("loggedIn.html")) {
                        break;
                    }
                default:
                    logger.error("Login using url '" + url + "' failed: " + sl);
                    return;
            }
            StringBuilder sb1 = new StringBuilder();
            StringBuilder sb2 = new StringBuilder();
            for (Cookie c : client.getCookieStore().getCookies()) {
                StringBuilder sb = (c instanceof BasicClientCookie2) ? sb2 : sb1;
                if (sb.length() > 0) {
                    sb.append("; ");
                }
                sb.append(c.getName());
                sb.append('=');
                sb.append(c.getValue());
            }
            if (sb1.length() > 0) {
                cookie = sb1.toString();
            }
            if (sb2.length() > 0) {
                cookie2 = sb2.toString();
            }
        } finally {
            client.getConnectionManager().shutdown();
        }
    }
}
