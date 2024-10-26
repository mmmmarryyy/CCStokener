package org.ofbiz.testtools.seleniumxml;

import java.io.IOException;
import java.io.UnsupportedEncodingException;
import java.net.URLEncoder;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Set;
import net.sf.json.JSONException;
import net.sf.json.JSONObject;
import org.apache.http.Header;
import org.apache.http.HttpEntity;
import org.apache.http.HttpResponse;
import org.apache.http.HttpVersion;
import org.apache.http.client.CookieStore;
import org.apache.http.client.HttpResponseException;
import org.apache.http.client.ResponseHandler;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.client.protocol.ClientContext;
import org.apache.http.conn.ClientConnectionManager;
import org.apache.http.conn.scheme.PlainSocketFactory;
import org.apache.http.conn.scheme.Scheme;
import org.apache.http.conn.scheme.SchemeRegistry;
import org.apache.http.conn.scheme.SocketFactory;
import org.apache.http.cookie.Cookie;
import org.apache.http.impl.client.BasicCookieStore;
import org.apache.http.impl.client.BasicResponseHandler;
import org.apache.http.impl.client.DefaultConnectionKeepAliveStrategy;
import org.apache.http.impl.client.DefaultHttpClient;
import org.apache.http.impl.conn.tsccm.ThreadSafeClientConnManager;
import org.apache.http.params.BasicHttpParams;
import org.apache.http.params.HttpParams;
import org.apache.http.params.HttpProtocolParams;
import org.apache.http.protocol.BasicHttpContext;
import org.jdom.Element;

public class RemoteRequest {

    public static final String module = RemoteRequest.class.getName();

    /**     
     * The default parameters.
     * Instantiated in {@link #setup setup}.
     */
    private static HttpParams defaultParameters = null;

    /**
     * The scheme registry.
     * Instantiated in {@link #setup setup}.
     */
    private static SchemeRegistry supportedSchemes;

    private static final String JsonHandleMode = "JSON_HANDLE";

    private static final String HttpHandleMode = "HTTP_HANDLE";

    private SeleniumXml parent;

    private SeleniumXml currentTest;

    private List<Element> children;

    private Map<String, Object> inMap;

    private Map<String, String> outMap;

    private String requestUrl;

    private String host;

    private String responseHandlerMode;

    private String loginAsUrl;

    private String loginAsUserParam;

    private String loginAsPasswordParam;

    private int currentRowIndx;

    static {
        supportedSchemes = new SchemeRegistry();
        SocketFactory sf = PlainSocketFactory.getSocketFactory();
        supportedSchemes.register(new Scheme("http", sf, 80));
        HttpParams params = new BasicHttpParams();
        HttpProtocolParams.setVersion(params, HttpVersion.HTTP_1_1);
        HttpProtocolParams.setContentCharset(params, "UTF-8");
        HttpProtocolParams.setUseExpectContinue(params, true);
        defaultParameters = params;
    }

    public RemoteRequest(SeleniumXml parent, List<Element> children, List<Element> loginAs, String requestUrl, String hostString, String responseHandlerMode) {
        this(parent, children, requestUrl, hostString, responseHandlerMode);
        if (loginAs != null && !loginAs.isEmpty()) {
            Element elem = loginAs.get(0);
            this.loginAsUserParam = elem.getAttributeValue("username-param");
            this.loginAsPasswordParam = elem.getAttributeValue("password-param");
            this.loginAsUrl = elem.getAttributeValue("url");
        }
    }

    public RemoteRequest(SeleniumXml parent, List<Element> children, String requestUrl, String hostString, String responseHandlerMode) {
        super();
        this.parent = parent;
        this.requestUrl = requestUrl;
        this.host = hostString;
        this.children = children;
        this.responseHandlerMode = (HttpHandleMode.equals(responseHandlerMode)) ? HttpHandleMode : JsonHandleMode;
        System.out.println("RemoteRequest, requestUrl: " + this.requestUrl);
        System.out.println("RemoteRequest, host: " + this.host);
        initData();
    }

    private void initData() {
        this.inMap = new HashMap();
        this.outMap = new HashMap();
        String nm, name, value, fieldName = null;
        for (Element elem : this.children) {
            nm = elem.getName();
            if (nm.equals("param-in")) {
                name = elem.getAttributeValue("name");
                value = this.parent.replaceParam(elem.getAttributeValue("value"));
                System.out.println("RemoteRequest, param-in, name: " + name + ", value: " + value);
                this.inMap.put(name, value);
            } else if (nm.equals("param-out")) {
                name = elem.getAttributeValue("result-name");
                fieldName = elem.getAttributeValue("field-name");
                if (fieldName == null || fieldName.length() == 0) {
                    fieldName = name;
                }
                this.outMap.put(name, fieldName);
            }
        }
        return;
    }

    public void runTest() {
        ClientConnectionManager ccm = new ThreadSafeClientConnManager(defaultParameters, supportedSchemes);
        DefaultHttpClient client = new DefaultHttpClient(ccm, defaultParameters);
        client.setKeepAliveStrategy(new DefaultConnectionKeepAliveStrategy());
        HttpEntity entity = null;
        ResponseHandler<String> responseHandler = null;
        try {
            BasicHttpContext localContext = new BasicHttpContext();
            CookieStore cookieStore = new BasicCookieStore();
            localContext.setAttribute(ClientContext.COOKIE_STORE, cookieStore);
            Header sessionHeader = null;
            if (this.loginAsUrl != null) {
                String loginAsUri = this.host + this.loginAsUrl;
                String loginAsParamString = "?" + this.loginAsUserParam + "&" + this.loginAsPasswordParam;
                HttpGet req2 = new HttpGet(loginAsUri + loginAsParamString);
                System.out.println("loginAsUrl:" + loginAsUri + loginAsParamString);
                req2.setHeader("Connection", "Keep-Alive");
                HttpResponse rsp = client.execute(req2, localContext);
                Header[] headers = rsp.getAllHeaders();
                for (int i = 0; i < headers.length; i++) {
                    Header hdr = headers[i];
                    String headerValue = hdr.getValue();
                    if (headerValue.startsWith("JSESSIONID")) {
                        sessionHeader = hdr;
                    }
                    System.out.println("login: " + hdr.getName() + " : " + hdr.getValue());
                }
                List<Cookie> cookies = cookieStore.getCookies();
                System.out.println("cookies.size(): " + cookies.size());
                for (int i = 0; i < cookies.size(); i++) {
                    System.out.println("Local cookie(0): " + cookies.get(i));
                }
            }
            if (HttpHandleMode.equals(this.responseHandlerMode)) {
            } else {
                responseHandler = new JsonResponseHandler(this);
            }
            String paramString = urlEncodeArgs(this.inMap, false);
            String thisUri = null;
            if (sessionHeader != null) {
                String sessionHeaderValue = sessionHeader.getValue();
                int pos1 = sessionHeaderValue.indexOf("=");
                int pos2 = sessionHeaderValue.indexOf(";");
                String sessionId = sessionHeaderValue.substring(pos1 + 1, pos2);
                thisUri = this.host + this.requestUrl + ";jsessionid=" + sessionId + "?" + paramString;
            } else {
                thisUri = this.host + this.requestUrl + "?" + paramString;
            }
            System.out.println("thisUri: " + thisUri);
            HttpGet req = new HttpGet(thisUri);
            if (sessionHeader != null) {
                req.setHeader(sessionHeader);
            }
            String responseBody = client.execute(req, responseHandler, localContext);
        } catch (HttpResponseException e) {
            System.out.println(e.getMessage());
        } catch (IOException e) {
            System.out.println(e.getMessage());
        } finally {
            try {
                if (entity != null) entity.consumeContent();
            } catch (IOException e) {
                System.out.println("in 'finally'  " + e.getMessage());
            }
        }
        return;
    }

    private void login(DefaultHttpClient client, BasicHttpContext localContext) throws IOException {
        String paramString = "USERNAME=" + this.parent.getUserName() + "&PASSWORD=" + this.parent.getPassword();
        String thisUri = this.host + "/eng/control/login?" + paramString;
        HttpGet req = new HttpGet(thisUri);
        req.setHeader("Connection", "Keep-Alive");
        client.execute(req, localContext);
        return;
    }

    /** URL Encodes a Map of arguements */
    public static String urlEncodeArgs(Map<String, ? extends Object> args, boolean useExpandedEntites) {
        StringBuilder buf = new StringBuilder();
        if (args != null) {
            for (Map.Entry<String, ? extends Object> entry : args.entrySet()) {
                String name = entry.getKey();
                Object value = entry.getValue();
                String valueStr = null;
                if (name != null && value != null) {
                    if (value instanceof String) {
                        valueStr = (String) value;
                    } else {
                        valueStr = value.toString();
                    }
                    if (valueStr != null && valueStr.length() > 0) {
                        if (buf.length() > 0) {
                            if (useExpandedEntites) {
                                buf.append("&amp;");
                            } else {
                                buf.append("&");
                            }
                        }
                        try {
                            buf.append(URLEncoder.encode(name, "UTF-8"));
                        } catch (UnsupportedEncodingException e) {
                        }
                        buf.append('=');
                        try {
                            buf.append(URLEncoder.encode(valueStr, "UTF-8"));
                        } catch (UnsupportedEncodingException e) {
                        }
                    }
                }
            }
        }
        return buf.toString();
    }

    public class JsonResponseHandler extends BasicResponseHandler {

        private RemoteRequest parentRemoteRequest;

        public JsonResponseHandler(RemoteRequest parentRemoteRequest) {
            super();
            this.parentRemoteRequest = parentRemoteRequest;
        }

        public String handleResponse(org.apache.http.HttpResponse response) throws HttpResponseException, IOException {
            String bodyString = super.handleResponse(response);
            JSONObject jsonObject = null;
            try {
                jsonObject = JSONObject.fromObject(bodyString);
            } catch (JSONException e) {
                throw new HttpResponseException(0, e.getMessage());
            }
            Set<Map.Entry<String, String>> paramSet = this.parentRemoteRequest.outMap.entrySet();
            Iterator<Map.Entry<String, String>> paramIter = paramSet.iterator();
            Map parentDataMap = this.parentRemoteRequest.parent.getMap();
            while (paramIter.hasNext()) {
                Map.Entry<String, String> paramPair = paramIter.next();
                if (jsonObject.containsKey(paramPair.getKey())) {
                    Object obj = jsonObject.get(paramPair.getKey());
                    System.out.println("RemoteRequest, param-out, name: " + paramPair.getKey() + ", value: " + obj);
                    parentDataMap.put(paramPair.getKey(), obj);
                }
            }
            return bodyString;
        }
    }
}
