package su.msk.dunno.httpbot.support;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.io.UnsupportedEncodingException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import org.apache.http.Header;
import org.apache.http.HttpEntity;
import org.apache.http.HttpResponse;
import org.apache.http.MalformedChunkCodingException;
import org.apache.http.NameValuePair;
import org.apache.http.client.ClientProtocolException;
import org.apache.http.client.entity.UrlEncodedFormEntity;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.client.methods.HttpRequestBase;
import org.apache.http.conn.ClientConnectionManager;
import org.apache.http.conn.scheme.PlainSocketFactory;
import org.apache.http.conn.scheme.Scheme;
import org.apache.http.conn.scheme.SchemeRegistry;
import org.apache.http.cookie.Cookie;
import org.apache.http.impl.client.ContentEncodingHttpClient;
import org.apache.http.impl.conn.tsccm.ThreadSafeClientConnManager;
import org.apache.http.message.BasicNameValuePair;
import org.apache.http.params.HttpParams;
import org.apache.http.protocol.HTTP;
import org.apache.http.util.EntityUtils;
import org.apache.log4j.Logger;
import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;
import org.xml.sax.SAXException;
import su.msk.dunno.httpbot.support.parsers.HtmlParser;
import org.apache.http.entity.mime.MultipartEntity;
import org.apache.http.entity.mime.content.ContentBody;
import org.apache.http.entity.mime.content.StringBody;

public class Conn {

    private static final Logger log = Logger.getLogger(Conn.class);

    private ThreadSafeClientConnManager cm;

    private ContentEncodingHttpClient client = new ContentEncodingHttpClient();

    private String host = "";

    private String currentContext = "";

    private String currentUrl = "";

    private HttpGet get;

    private HttpPost post;

    private JSONObject custom_headers = new JSONObject();

    private JSONObject custom_formdata = new JSONObject();

    public Integer currentStatusCode = 0;

    public String currentTextStatus = "";

    public HashMap<String, String> currentHeaders = new HashMap<String, String>();

    public StringBuilder currentCookies = new StringBuilder();

    public String currentHtml = "";

    private static Conn instance;

    public static Conn instance() {
        if (instance == null) instance = new Conn();
        return instance;
    }

    public Conn() {
        SchemeRegistry schemeRegistry = new SchemeRegistry();
        schemeRegistry.register(new Scheme("http", PlainSocketFactory.getSocketFactory(), 80));
        cm = new ThreadSafeClientConnManager(schemeRegistry);
        client = new ContentEncodingHttpClient(cm, null);
    }

    public void addHeader(String name, String value) throws JSONException {
        custom_headers.put(name, value);
    }

    public void addPostData(JSONObject formData) throws JSONException, UnsupportedEncodingException {
        custom_formdata = new JSONObject();
        JSONArray cf_names = formData.names();
        if (cf_names != null) {
            for (int i = 0; i < cf_names.length(); i++) {
                String name = cf_names.getString(i);
                Object o = formData.get(name);
                custom_formdata.put(name, o);
            }
        }
    }

    public void addMultipartPostData(JSONObject formData) throws JSONException, UnsupportedEncodingException {
        addPostData(formData);
        JSONArray cf_names = custom_formdata.names();
        if (cf_names != null) {
            for (int i = 0; i < cf_names.length(); i++) {
                String name = cf_names.getString(i);
                Object o = custom_formdata.get(name);
                if (o.getClass().equals(String.class)) custom_formdata.put(name, new StringBody(o.toString()));
            }
        }
    }

    public void executePost(String link) throws ClientProtocolException, IOException, JSONException, SAXException {
        executePost(link, false);
    }

    public void executeMultipartPost(String link) throws ClientProtocolException, IOException, JSONException, SAXException {
        executePost(link, true);
    }

    public void executePost(String link, boolean isMultiPartData) throws ClientProtocolException, IOException, JSONException, SAXException {
        link = analyzeLink(link);
        String url = constructUrl(link);
        post = new HttpPost(url);
        setHeaders(post);
        JSONArray cf_names = custom_formdata.names();
        if (cf_names != null) {
            if (isMultiPartData) {
                MultipartEntity reqEntity = new MultipartEntity();
                for (int i = 0; i < cf_names.length(); i++) {
                    String name = cf_names.getString(i);
                    ContentBody cb = (ContentBody) custom_formdata.get(name);
                    reqEntity.addPart(name, cb);
                }
                post.setEntity(reqEntity);
            } else {
                List<NameValuePair> nvps = new ArrayList<NameValuePair>();
                for (int i = 0; i < cf_names.length(); i++) {
                    String name = cf_names.getString(i);
                    String value = custom_formdata.getString(name);
                    nvps.add(new BasicNameValuePair(name, value));
                }
                post.setEntity(new UrlEncodedFormEntity(nvps, HTTP.UTF_8));
            }
            custom_formdata = new JSONObject();
        }
        currentUrl = post.getURI().toString();
        HttpResponse response = client.execute(post);
        HttpEntity entity = response.getEntity();
        currentStatusCode = response.getStatusLine().getStatusCode();
        currentTextStatus = response.getStatusLine().toString();
        currentHeaders = new HashMap<String, String>();
        for (Header h : response.getAllHeaders()) {
            currentHeaders.put(h.getName(), h.getValue());
        }
        currentCookies = new StringBuilder();
        for (Cookie c : client.getCookieStore().getCookies()) {
            currentCookies.append(c + "\n");
        }
        if (entity != null) {
            try {
                currentHtml = EntityUtils.toString(entity);
                entity.consumeContent();
            } catch (MalformedChunkCodingException mcce) {
            }
        }
        log.debug("executed post: " + currentUrl);
        metaRefresh();
        analyzeStatusCode();
    }

    public void executeGet(String link) throws ClientProtocolException, IOException, JSONException, SAXException {
        link = analyzeLink(link);
        String url = constructUrl(link);
        get = new HttpGet(url);
        setHeaders(get);
        currentUrl = get.getURI().toString();
        HttpResponse response = client.execute(get);
        HttpEntity entity = response.getEntity();
        currentStatusCode = response.getStatusLine().getStatusCode();
        currentTextStatus = response.getStatusLine().toString();
        currentHeaders = new HashMap<String, String>();
        for (Header h : response.getAllHeaders()) {
            currentHeaders.put(h.getName(), h.getValue());
        }
        currentCookies = new StringBuilder();
        for (Cookie c : client.getCookieStore().getCookies()) {
            currentCookies.append(c + "\n");
        }
        if (entity != null) {
            try {
                currentHtml = EntityUtils.toString(entity);
                entity.consumeContent();
            } catch (MalformedChunkCodingException mcce) {
                mcce.printStackTrace();
            }
        }
        log.debug("executed get: " + currentUrl);
        metaRefresh();
        analyzeStatusCode();
    }

    public void getBinaryData(OutputStream os, String link) throws JSONException, ClientProtocolException, IOException {
        link = analyzeLink(link);
        String url = constructUrl(link);
        get = new HttpGet(url);
        setHeaders(get);
        currentUrl = this.get.getURI().toString();
        HttpResponse response = client.execute(get);
        HttpEntity entity = response.getEntity();
        currentStatusCode = response.getStatusLine().getStatusCode();
        currentTextStatus = response.getStatusLine().toString();
        currentHeaders = new HashMap<String, String>();
        for (Header h : response.getAllHeaders()) {
            currentHeaders.put(h.getName(), h.getValue());
        }
        currentCookies = new StringBuilder();
        for (Cookie c : client.getCookieStore().getCookies()) {
            currentCookies.append(c + "\n");
        }
        if (entity != null) {
            try {
                InputStream in = entity.getContent();
                byte[] buffer = new byte[1024];
                int count = -1;
                while ((count = in.read(buffer)) != -1) {
                    os.write(buffer, 0, count);
                }
                os.close();
                entity.consumeContent();
            } catch (MalformedChunkCodingException mcce) {
                mcce.printStackTrace();
            }
        }
        log.debug("image retrieved: " + currentUrl);
    }

    private String analyzeLink(String link) {
        String new_link = new String(link);
        boolean hasHost = new_link.contains("www");
        if (new_link.contains("http")) {
            hasHost = true;
            if (new_link.contains("https")) new_link = new_link.replace("https://", ""); else new_link = new_link.replace("http://", "");
        }
        int first_slash = new_link.indexOf('/');
        if (first_slash > 0) {
            host = new_link.substring(0, first_slash);
            currentContext = "";
            new_link = new_link.substring(first_slash);
        } else if (hasHost) {
            host = new_link;
            currentContext = "";
            return "";
        }
        int last_slash = new_link.lastIndexOf('/');
        if (last_slash > 0) {
            currentContext = new_link.substring(0, last_slash + 1);
            new_link = new_link.substring(last_slash + 1);
        }
        return new_link;
    }

    private String constructUrl(String link) {
        return "http://" + host + ((host.endsWith("/") || currentContext.startsWith("/") || "".equals(currentContext)) ? "" : "/") + currentContext + ((currentContext.endsWith("/") || link.startsWith("/") || "".equals(link)) ? "" : "/") + link;
    }

    private void setHeaders(HttpRequestBase hrb) throws JSONException {
        JSONArray ch_names = custom_headers.names();
        if (ch_names != null) {
            for (int i = 0; i < ch_names.length(); i++) {
                String name = ch_names.getString(i);
                String value = custom_headers.getString(name);
                hrb.setHeader(name, value);
            }
            custom_headers = new JSONObject();
        }
        hrb.setHeader("User-Agent", "Mozilla/5.0 (Windows; U; Windows NT 5.1; ru; rv:1.9.2) Gecko/20100115 Firefox/3.6 (.NET CLR 3.5.30729)");
        hrb.setHeader("Accept", "text/html,application/xhtml+xml,application/xml;q=0.9,*/*;q=0.8");
        hrb.setHeader("Accept-Language", "ru,en-us;q=0.7,en;q=0.3");
        hrb.setHeader("Accept-Charset", "windows-1251,utf-8;q=0.7,*;q=0.7");
        hrb.setHeader("Connection", "Keep-Alive");
        hrb.setHeader("Keep-Alive", "115");
    }

    private void metaRefresh() throws JSONException, IOException, SAXException {
        boolean isRedirect = false;
        String redirectLink = "";
        JSONObject parsed = new HtmlParser().parse(currentHtml);
        if (parsed.has("redirect")) {
            isRedirect = true;
            redirectLink = parsed.getString("redirect");
        }
        if (isRedirect && !"".equals(redirectLink)) {
            log.debug("executing redirect:");
            get.setHeader("Referer", currentUrl);
            executeGet(redirectLink);
        }
    }

    private void analyzeStatusCode() throws JSONException, ClientProtocolException, IOException, SAXException {
        switch(currentStatusCode) {
            case 302:
                log.debug("http1.1 302: redirect to another page " + currentHeaders.get("Location"));
                String link = analyzeLink(currentHeaders.get("Location"));
                String url = constructUrl(link);
                get.setHeader("Referer", currentUrl);
                executeGet(url);
                break;
        }
    }

    public ContentEncodingHttpClient getClient() {
        return client;
    }

    public String getCurrentUrl() {
        return currentUrl;
    }

    public void close() {
        cm.shutdown();
    }

    public static void main(String[] args) throws ClientProtocolException, IOException, JSONException, SAXException {
        instance().executeGet("http://google.com");
        System.out.println(instance().currentHtml);
    }
}
