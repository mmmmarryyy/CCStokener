package com.prolix.editor.oics.oicsSend;

import java.io.IOException;
import java.io.InputStream;
import java.util.Iterator;
import org.apache.http.HttpEntity;
import org.apache.http.HttpException;
import org.apache.http.HttpHost;
import org.apache.http.HttpRequest;
import org.apache.http.HttpRequestInterceptor;
import org.apache.http.HttpResponse;
import org.apache.http.HttpVersion;
import org.apache.http.auth.AuthScheme;
import org.apache.http.auth.AuthScope;
import org.apache.http.auth.AuthState;
import org.apache.http.auth.Credentials;
import org.apache.http.auth.UsernamePasswordCredentials;
import org.apache.http.client.CredentialsProvider;
import org.apache.http.client.methods.HttpUriRequest;
import org.apache.http.client.protocol.ClientContext;
import org.apache.http.impl.auth.BasicScheme;
import org.apache.http.impl.client.DefaultHttpClient;
import org.apache.http.params.CoreProtocolPNames;
import org.apache.http.protocol.BasicHttpContext;
import org.apache.http.protocol.ExecutionContext;
import org.apache.http.protocol.HttpContext;
import org.apache.http.util.EntityUtils;
import org.jdom.Document;
import org.jdom.Element;
import org.jdom.JDOMException;
import org.jdom.Namespace;
import org.jdom.input.SAXBuilder;
import com.prolix.editor.systempreferences.DefaultGLMSettings;

public class OICSConnection {

    public static final Namespace atomNS = Namespace.getNamespace("http://www.w3.org/2005/Atom");

    public static final Namespace lodNS = Namespace.getNamespace("http://www.icoper.org/schema/lodv1.0");

    public static final Namespace lomNS = Namespace.getNamespace("http://ltsc.ieee.org/xsd/LOM");

    public static final Namespace metadataNS = Namespace.getNamespace("http://www.cenorm.be/xsd/SPI");

    public static final Namespace icoperNS = Namespace.getNamespace("http://www.icoper.org/schema/LOM");

    public static final Namespace paloNS = Namespace.getNamespace("http://www.icoper.org/schema/palov1.1");

    public static final String lomSource = "LOMv1.0";

    public static final String icoperSource = "ICOPERv1.0";

    public static String getBaseURL() {
        return DefaultGLMSettings.getInstance().getOICSURLPort();
    }

    private static boolean changeBaseURL = false;

    private static String OLDURL = "http://test1.km.co.at";

    public static String findIcoperIdentifier(Element element) {
        Iterator it = element.getChildren("identifier", OICSConnection.lomNS).iterator();
        while (it.hasNext()) {
            Element id = (Element) it.next();
            if (id.getChildText("catalog", OICSConnection.lomNS).equals("ICOPER")) {
                return id.getChildText("entry", OICSConnection.lomNS);
            }
        }
        System.out.println("LOM Entry without ICOPER identifier -- can't handle");
        return "";
    }

    public static String getRepositoryFromObjectHref(String href) {
        if (href == null || href.isEmpty()) return "";
        int first = href.indexOf('/');
        int last = href.lastIndexOf('/');
        if (first < 0 || last < 0 || first == last) return "";
        href = href.substring(0, last);
        first = href.lastIndexOf('/');
        return href.substring(first + 1);
    }

    public static String doChangeBaseURL(String url) {
        return url;
    }

    private DefaultHttpClient httpclient;

    private BasicHttpContext localcontext;

    protected int code;

    protected boolean isError;

    public OICSConnection() {
        setupConnection();
    }

    private void setupConnection() {
        OICSLogin login = OICSLogin.instance();
        if (login == null) {
            return;
        }
        httpclient = new DefaultHttpClient();
        httpclient.getParams().setParameter(CoreProtocolPNames.PROTOCOL_VERSION, HttpVersion.HTTP_1_1);
        httpclient.getCredentialsProvider().setCredentials(new AuthScope(DefaultGLMSettings.getInstance().getOICSURL(), DefaultGLMSettings.getInstance().getOICSPort()), new UsernamePasswordCredentials(login.getLogin(), login.getPw()));
        localcontext = new BasicHttpContext();
        BasicScheme basicAuth = new BasicScheme();
        localcontext.setAttribute("preemptive-auth", basicAuth);
        httpclient.addRequestInterceptor(new PreemptiveAuth(), 0);
    }

    public HttpUriRequest getRequest() {
        return null;
    }

    public HttpResponse execute() {
        return execute(getRequest());
    }

    public HttpResponse execute(HttpUriRequest request) {
        try {
            isError = false;
            if (httpclient == null) {
                isError = true;
                code = -2;
                return null;
            }
            HttpResponse response = httpclient.execute(request, localcontext);
            code = response.getStatusLine().getStatusCode();
            if (code < 200 || code >= 300) {
                isError = true;
                if (code == 401) {
                    OICSLogin.resetError();
                    closeConnection();
                    setupConnection();
                    return execute(request);
                }
            }
            return response;
        } catch (IOException e) {
            System.err.println("IOExe: " + e.getMessage());
            e.printStackTrace();
        }
        return null;
    }

    public void printExecute() {
        printExecute(getRequest());
    }

    public void printExecute(HttpUriRequest request) {
        HttpResponse response = execute(request);
        HttpEntity resEntity = response.getEntity();
        System.out.println(response.getStatusLine());
        try {
            if (resEntity != null) {
                System.out.println(EntityUtils.toString(resEntity));
            }
            if (resEntity != null) {
                resEntity.consumeContent();
            }
        } catch (IOException e) {
            System.err.println("IOExe1: " + e.getMessage());
            e.printStackTrace();
        }
        closeConnection();
    }

    public void closeConnection() {
        if (httpclient == null) {
            return;
        }
        httpclient.getConnectionManager().shutdown();
        httpclient = null;
    }

    public Document getDocument() {
        return getDocument(getRequest());
    }

    public Document getDocument(HttpUriRequest request) {
        SAXBuilder builder = new SAXBuilder();
        HttpResponse response = execute(request);
        if (isError) {
            return null;
        }
        Document doc = null;
        try {
            InputStream instream = response.getEntity().getContent();
            doc = builder.build(instream);
            instream.close();
        } catch (IOException e) {
            e.printStackTrace();
        } catch (JDOMException e) {
            e.printStackTrace();
        }
        closeConnection();
        return doc;
    }

    public Element getRootElement() {
        return getRootElement(getRequest());
    }

    public Element getRootElement(HttpUriRequest request) {
        Document doc = getDocument(request);
        if (doc == null) {
            return null;
        }
        return doc.getRootElement();
    }

    protected DefaultHttpClient getClient() {
        return httpclient;
    }

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

    /**
	 * @return the isError
	 */
    public boolean isError() {
        return isError;
    }

    /**
	 * @return the code
	 */
    public int getCode() {
        return code;
    }
}
