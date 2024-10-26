package com.k42b3.neodym;

import java.io.StringReader;
import java.text.DateFormat;
import java.util.Date;
import java.util.HashMap;
import java.util.Map;
import java.util.Set;
import java.util.logging.Logger;
import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import org.apache.http.Header;
import org.apache.http.HttpEntity;
import org.apache.http.HttpRequest;
import org.apache.http.HttpResponse;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.client.methods.HttpRequestBase;
import org.apache.http.entity.StringEntity;
import org.apache.http.impl.client.DefaultHttpClient;
import org.apache.http.params.BasicHttpParams;
import org.apache.http.params.HttpConnectionParams;
import org.apache.http.params.HttpParams;
import org.apache.http.util.EntityUtils;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.xml.sax.InputSource;
import org.xml.sax.SAXException;
import com.k42b3.neodym.oauth.Oauth;

/**
 * Handles http requests. If an oauth object is set you can also send signed
 * requests. Here an simple example code howto use the http class
 * 
 * <code>
 * Http http = new Http();
 * String response = http.request(Http.GET, "http://google.de");
 * </code>
 *
 * @author     Christoph Kappestein <k42b3.x@gmail.com>
 * @license    http://www.gnu.org/licenses/gpl.html GPLv3
 * @link       http://code.google.com/p/delta-quadrant
 * @version    $Revision: 227 $
 */
public class Http {

    public static int GET = 0x1;

    public static int POST = 0x2;

    private Oauth oauth;

    private TrafficListenerInterface trafficListener;

    private CacheManager cacheManager = new CacheManager();

    private HttpRequest lastRequest;

    private HttpResponse lastResponse;

    private Logger logger = Logger.getLogger("com.k42b3.neodym");

    public Http(TrafficListenerInterface trafficListener) {
        this.trafficListener = trafficListener;
    }

    public Http() {
        this(null);
    }

    public String request(int method, String url, Map<String, String> header, String body, boolean signed) throws Exception {
        String cacheKey = url;
        if (method == Http.GET) {
            Cache cache = cacheManager.get(cacheKey);
            if (cache != null) {
                logger.info("Found cache for " + cacheKey + " expires in " + DateFormat.getInstance().format(cache.getExpire()));
                return cache.getResponse();
            }
        }
        HttpParams httpParams = new BasicHttpParams();
        HttpConnectionParams.setConnectionTimeout(httpParams, 6000);
        DefaultHttpClient httpClient = new DefaultHttpClient(httpParams);
        HttpRequestBase httpRequest;
        if (method == Http.GET) {
            httpRequest = new HttpGet(url);
        } else if (method == Http.POST) {
            httpRequest = new HttpPost(url);
            if (body != null && !body.isEmpty()) {
                ((HttpPost) httpRequest).setEntity(new StringEntity(body));
            }
        } else {
            throw new Exception("Invalid request method");
        }
        if (header != null) {
            Set<String> keys = header.keySet();
            for (String k : keys) {
                httpRequest.addHeader(k, header.get(k));
            }
        }
        if (oauth != null && signed) {
            oauth.signRequest(httpRequest);
        }
        logger.info("Request: " + httpRequest.getRequestLine().toString());
        HttpResponse httpResponse = httpClient.execute(httpRequest);
        logger.info("Response: " + httpResponse.getStatusLine().toString());
        HttpEntity entity = httpResponse.getEntity();
        String responseContent = EntityUtils.toString(entity);
        if (trafficListener != null) {
            TrafficItem trafficItem = new TrafficItem();
            trafficItem.setRequest(httpRequest);
            trafficItem.setRequestContent(body);
            trafficItem.setResponse(httpResponse);
            trafficItem.setResponseContent(responseContent);
            trafficListener.handleRequest(trafficItem);
        }
        int statusCode = httpResponse.getStatusLine().getStatusCode();
        if (!(statusCode >= 200 && statusCode < 300)) {
            if (!responseContent.isEmpty()) {
                String message = responseContent.length() > 128 ? responseContent.substring(0, 128) + "..." : responseContent;
                throw new Exception(message);
            } else {
                throw new Exception("No successful status code");
            }
        }
        lastRequest = httpRequest;
        lastResponse = httpResponse;
        if (method == Http.GET) {
            Date expire = null;
            Header[] headers = httpResponse.getAllHeaders();
            for (int i = 0; i < headers.length; i++) {
                if (headers[i].getName().toLowerCase().equals("expires")) {
                    try {
                        expire = DateFormat.getInstance().parse(headers[i].getValue());
                    } catch (Exception e) {
                    }
                }
            }
            if (expire != null && expire.compareTo(new Date()) > 0) {
                Cache cache = new Cache(cacheKey, responseContent, expire);
                cacheManager.add(cache);
                logger.info("Add to cache " + cacheKey + " expires in " + DateFormat.getInstance().format(expire));
            }
        }
        return responseContent;
    }

    public String request(int method, String url, Map<String, String> header, String body) throws Exception {
        return this.request(method, url, header, body, true);
    }

    public String request(int method, String url, Map<String, String> header) throws Exception {
        return this.request(method, url, header, null, true);
    }

    public String request(int method, String url) throws Exception {
        return this.request(method, url, null, null, true);
    }

    public String requestNotSigned(int method, String url, Map<String, String> header, String body) throws Exception {
        return this.request(method, url, header, body, false);
    }

    public String requestNotSigned(int method, String url, Map<String, String> header) throws Exception {
        return this.requestNotSigned(method, url, header, null);
    }

    public String requestNotSigned(int method, String url) throws Exception {
        return this.requestNotSigned(method, url, null);
    }

    public Document requestXml(int method, String url, Map<String, String> header, String body, boolean signed) throws Exception {
        if (header == null) {
            header = new HashMap<String, String>();
        }
        if (!header.containsKey("Accept")) {
            header.put("Accept", "application/xml");
        }
        String responseContent = this.request(method, url, header, body, signed);
        try {
            DocumentBuilderFactory dbf = DocumentBuilderFactory.newInstance();
            DocumentBuilder db = dbf.newDocumentBuilder();
            InputSource is = new InputSource();
            is.setCharacterStream(new StringReader(responseContent));
            Document doc = db.parse(is);
            Element rootElement = (Element) doc.getDocumentElement();
            rootElement.normalize();
            return doc;
        } catch (SAXException e) {
            String text = responseContent.length() > 32 ? responseContent.substring(0, 32) + "..." : responseContent;
            throw new Exception(text);
        }
    }

    public Document requestXml(int method, String url, Map<String, String> header, String body) throws Exception {
        return this.requestXml(method, url, header, body, true);
    }

    public Document requestXml(int method, String url, Map<String, String> header) throws Exception {
        return this.requestXml(method, url, header, null);
    }

    public Document requestXml(int method, String url) throws Exception {
        return this.requestXml(method, url, null);
    }

    public Document requestNotSignedXml(int method, String url, Map<String, String> header, String body) throws Exception {
        return this.requestXml(method, url, header, body, false);
    }

    public Document requestNotSignedXml(int method, String url, Map<String, String> header) throws Exception {
        return this.requestNotSignedXml(method, url, header, null);
    }

    public Document requestNotSignedXml(int method, String url) throws Exception {
        return this.requestNotSignedXml(method, url, null);
    }

    public HttpRequest getLastRequest() {
        return lastRequest;
    }

    public HttpResponse getLastResponse() {
        return lastResponse;
    }

    public void setOauth(Oauth oauth) {
        this.oauth = oauth;
    }

    public Oauth getOauth() {
        return oauth;
    }

    public void setTrafficListener(TrafficListenerInterface trafficListener) {
        this.trafficListener = trafficListener;
    }

    public TrafficListenerInterface getTrafficListener() {
        return trafficListener;
    }

    public CacheManager getCacheManager() {
        return cacheManager;
    }

    public static String appendQuery(String url, String query) {
        if (url.indexOf('?') == -1) {
            return url + '?' + query;
        } else {
            return url + '&' + query;
        }
    }
}
