package fb4java;

import fb4java.beans.BasicUserInfo;
import fb4java.beans.interfaces.Parameterable;
import fb4java.http.HttpResponseMessage;
import fb4java.service.DataRetrievalService;
import fb4java.util.Utility;
import org.apache.http.HttpEntity;
import org.apache.http.HttpResponse;
import org.apache.http.StatusLine;
import org.apache.http.auth.AuthenticationException;
import org.apache.http.client.ClientProtocolException;
import org.apache.http.client.HttpClient;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.impl.client.DefaultHttpClient;
import org.json.JSONException;
import java.io.IOException;
import java.io.InputStream;
import java.net.URI;
import java.net.URISyntaxException;
import java.util.ArrayList;
import java.util.Date;
import java.util.Map;

/**
 * 
 * fb4java<br />
 * fb4java
 * 
 * @author Choongsan Ro
 * @version 1.0 2010. 3. 9.
 */
public class Facebook4j {

    private String sessionKey;

    private String apiKey;

    private String apiSecret;

    private String appId;

    private Long userId;

    private Date sessionExpires;

    private HttpClient httpclient;

    /**
     * Protocol that facebook REST server uses.
     */
    private static final String PROTOCOL = "HTTPS";

    private static final String SERVER_ADDRESS = "api.facebook.com";

    private static final String RESOURCE_URL = "/restserver.php";

    private static final String CONTENT_TYPE = "application/x-www-form-urlencoded";

    /**
     * current Facebook API version
     */
    public static final String VERSION = "1.0";

    /**
     * 
     * @param apiKey
     *            Facebook API Key
     * @param apiSecret
     *            Facebook API Secret
     */
    public Facebook4j(String apiKey, String apiSecret) {
        this.apiKey = apiKey;
        this.apiSecret = apiSecret;
        httpclient = new DefaultHttpClient();
    }

    /**
     * Personal Information that's provided by Facebook.
     * 
     * @return BasicUserInfo object containing viewer's information
     * @see BasicUserInfo
     */
    public BasicUserInfo getMyInfo() {
        BasicUserInfo userInfo = new BasicUserInfo();
        DataRetrievalService dataRetSvc = new DataRetrievalService(this);
        ArrayList<Parameterable> myInfo = new ArrayList<Parameterable>();
        try {
            dataRetSvc.getUserInfo(new Long[] { userId }, myInfo, userInfo.getClass());
        } catch (Exception e) {
            e.printStackTrace();
            return null;
        }
        if (myInfo.size() <= 0) {
            return null;
        } else {
            userInfo = (BasicUserInfo) myInfo.get(0);
        }
        return userInfo;
    }

    public String getSessionKey() {
        return sessionKey;
    }

    public long getUserId() {
        return userId;
    }

    /**
     * 
     * @param listOfMethods
     * @return
     * @throws URISyntaxException
     * @throws ClientProtocolException
     * @throws IOException
     * @throws AuthenticationException
     */
    public HttpResponseMessage sendGetRequest(Map<String, String> listOfMethods) throws URISyntaxException, ClientProtocolException, IOException {
        HttpResponseMessage rslt = null;
        String sig = Utility.convetToSignature(listOfMethods, apiSecret);
        URI uri = Utility.createURI(listOfMethods, PROTOCOL, SERVER_ADDRESS, RESOURCE_URL, sig, null);
        HttpGet get = new HttpGet(uri);
        HttpResponse resp = httpclient.execute(get);
        StatusLine status = resp.getStatusLine();
        HttpEntity respEntity = resp.getEntity();
        InputStream content = respEntity.getContent();
        rslt = new HttpResponseMessage("GET", uri.toURL(), status.getStatusCode(), content);
        return rslt;
    }

    /**
     * 
     * @param listOfMethods
     */
    public void setApiKeyParam(Map<String, String> listOfMethods) {
        listOfMethods.put("api_key", apiKey);
        listOfMethods.put("format", "json");
        listOfMethods.put("v", VERSION);
    }

    /**
     * Sets Application ID
     * 
     * @param appId
     *            Application ID that's displayed on <a
     *            href='http://www.facebook.com/developers/apps.php'>My
     *            Applications Page</a>
     */
    public void setAppId(String appId) {
        this.appId = appId;
    }

    /**
     * Sets a data structure for method. Requirement Params consists of
     * session_key, api_key, format, and v.
     * 
     * @param listOfMethods
     *            An Object that methods and it's value to be set.
     */
    public void setRequirementParam(Map<String, String> listOfMethods) {
        setApiKeyParam(listOfMethods);
        listOfMethods.put("session_key", sessionKey);
    }

    /**
     * Sets Session Expiration date for this connection.
     * 
     * @param d
     *            Date object
     */
    public void setSessionExpires(Date d) {
        this.sessionExpires = d;
    }

    /**
     * Sets Session Key for this connection. Session key will live until
     * expiration date. If session expiration is set null, it is considered as
     * this session has never expiring session.
     * 
     * @param sessionKey
     */
    public void setSessionKey(String sessionKey) {
        this.sessionKey = sessionKey;
    }

    /**
     * 
     * @param userid
     */
    public void setUserId(long userid) {
        this.userId = userid;
    }
}
