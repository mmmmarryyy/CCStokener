package twilio.client;

import org.apache.http.*;
import org.apache.http.auth.AuthScope;
import org.apache.http.auth.Credentials;
import org.apache.http.auth.UsernamePasswordCredentials;
import org.apache.http.client.*;
import org.apache.http.client.entity.UrlEncodedFormEntity;
import org.apache.http.client.methods.*;
import org.apache.http.client.params.AllClientPNames;
import org.apache.http.impl.client.*;
import org.apache.http.message.BasicNameValuePair;
import org.apache.http.util.EntityUtils;
import com.thoughtworks.xstream.XStream;
import twilio.internal.httpclient.*;
import twilio.internal.xstream.XStreamFactory;
import java.io.UnsupportedEncodingException;
import java.net.URLEncoder;
import java.text.SimpleDateFormat;
import java.util.*;

public class TwilioClient {

    public static final String TWILIO_ENDPOINT_BASE = "https://api.twilio.com/2008-08-01/";

    private HttpClient httpClient;

    private boolean compressionEnabled = false;

    private String accountSid;

    private String authToken;

    private Credentials credentials;

    public TwilioClient(String accountSid, String authToken) {
        this(new DefaultHttpClient(), accountSid, authToken);
    }

    public TwilioClient(HttpClient hClient, String accountSid, String authToken) {
        this.httpClient = hClient;
        setCredentials(accountSid, authToken);
    }

    protected HttpClient createHttpClient() {
        DefaultHttpClient hclient = new DefaultHttpClient();
        setUserAgent("twilio-java-client");
        setConnectionTimeout(10 * 1000);
        setSocketTimeout(25 * 1000);
        return hclient;
    }

    public void setCredentials(String accountSid, String authToken) {
        this.accountSid = accountSid;
        this.authToken = authToken;
        credentials = new UsernamePasswordCredentials(this.accountSid, this.authToken);
    }

    public String getAccountSid() {
        return this.accountSid;
    }

    public String getAuthToken() {
        return this.authToken;
    }

    public void setUserAgent(String ua) {
        this.httpClient.getParams().setParameter(AllClientPNames.USER_AGENT, ua);
    }

    public void setConnectionTimeout(int milliseconds) {
        httpClient.getParams().setIntParameter(AllClientPNames.CONNECTION_TIMEOUT, milliseconds);
    }

    public void setSocketTimeout(int milliseconds) {
        httpClient.getParams().setIntParameter(AllClientPNames.SO_TIMEOUT, milliseconds);
    }

    protected TwilioResponse sendTwilioRequest(String httpMethod, CharSequence url) throws TwilioException {
        return sendTwilioRequest(httpMethod, url, new HashMap<String, String>());
    }

    protected TwilioResponse sendTwilioRequest(String httpMethod, CharSequence url, Map<String, String> params) throws TwilioException {
        String xmlResponse = sendHttpRequest(httpMethod, url, params);
        TwilioResponse r = fromXml(xmlResponse);
        r.setXml(xmlResponse);
        if (r.getTwilioException() != null) {
            throw r.getTwilioException();
        }
        if (r.getRecordings() != null) {
            setMp3Url(r.getRecordings());
        }
        if (r.getRecording() != null) {
            setMp3Url(r.getRecording());
        }
        return r;
    }

    protected TwilioResponse fromXml(String xml) {
        XStream xstream = XStreamFactory.createClientXStream();
        TwilioResponse r = (TwilioResponse) xstream.fromXML(xml);
        return r;
    }

    protected String sendHttpRequest(String httpMethod, CharSequence url, Map<String, String> params) {
        HttpClient c = getHttpClient();
        HttpUriRequest request = null;
        if (params == null) {
            params = new HashMap<String, String>();
        }
        if ("GET".equalsIgnoreCase(httpMethod)) {
            request = new HttpGet(buildUrl(url, params));
        } else if ("POST".equalsIgnoreCase(httpMethod)) {
            HttpPost post = new HttpPost(url.toString());
            try {
                post.setEntity(new UrlEncodedFormEntity(buildNameValuePairList(params)));
            } catch (UnsupportedEncodingException e) {
                throw new RuntimeException(e);
            }
            request = post;
        } else if ("DELETE".equalsIgnoreCase(httpMethod)) {
            request = new HttpDelete(url.toString());
        } else if ("PUT".equalsIgnoreCase(httpMethod)) {
            request = new HttpPut(url.toString());
        } else {
            throw new RuntimeException("unsupported method: " + httpMethod);
        }
        DefaultHttpClient defaultClient = (DefaultHttpClient) httpClient;
        defaultClient.getCredentialsProvider().setCredentials(AuthScope.ANY, credentials);
        HttpResponse response = null;
        HttpEntity entity = null;
        try {
            response = c.execute(request);
            StatusLine status = response.getStatusLine();
            if ((status.getStatusCode() < 200) || (status.getStatusCode() > 299)) {
                throw new RuntimeException("HTTP " + request.getMethod() + "  " + url + "  response status code = " + status.getStatusCode());
            }
            entity = response.getEntity();
            return EntityUtils.toString(entity);
        } catch (RuntimeException ex) {
            throw ex;
        } catch (Exception ex) {
            throw new RuntimeException(ex);
        } finally {
        }
    }

    protected String buildUrl(CharSequence url, Map<String, String> params) {
        String queryString = buildQueryString(params);
        String s = url + queryString;
        return s;
    }

    private String buildQueryString(Map<String, String> params) {
        StringBuffer query = new StringBuffer();
        if (params.size() > 0) {
            query.append("?");
            for (String key : params.keySet()) {
                query.append(key);
                query.append("=");
                query.append(encodeParameter(params.get(key)));
                query.append("&");
            }
            if (query.charAt(query.length() - 1) == '&') {
                query.deleteCharAt(query.length() - 1);
            }
        }
        return query.toString();
    }

    protected String encodeParameter(String s) {
        try {
            return URLEncoder.encode(s, "UTF-8");
        } catch (Exception ex) {
            throw new RuntimeException(ex);
        }
    }

    protected HttpClient getHttpClient() {
        if (this.httpClient == null) {
            this.httpClient = createHttpClient();
        }
        if (this.httpClient instanceof DefaultHttpClient) {
            DefaultHttpClient defaultClient = (DefaultHttpClient) httpClient;
            defaultClient.removeRequestInterceptorByClass(GzipRequestInterceptor.class);
            defaultClient.removeResponseInterceptorByClass(GzipResponseInterceptor.class);
            if (this.isCompressionEnabled()) {
                defaultClient.addRequestInterceptor(GzipRequestInterceptor.getInstance());
                defaultClient.addResponseInterceptor(GzipResponseInterceptor.getInstance());
            }
        }
        return this.httpClient;
    }

    public boolean isCompressionEnabled() {
        return compressionEnabled;
    }

    public void setCompressionEnabled(boolean b) {
        this.compressionEnabled = b;
    }

    /**
	 * 
	 * 
	 *   send HTTP GET
	 *   
	 *   This method can be used to retrieve images  (JPEG, PNG, GIF)
	 *   or any other file type (MP3, WAV, etc)
	 *   
	 *   @return byte array
	 *  
	 */
    public byte[] getBytesFromUrl(CharSequence url) {
        try {
            HttpGet get = new HttpGet(url.toString());
            HttpResponse response = this.getHttpClient().execute(get);
            HttpEntity entity = response.getEntity();
            if (entity == null) {
                throw new RuntimeException("response body was empty");
            }
            return EntityUtils.toByteArray(entity);
        } catch (RuntimeException ex) {
            throw ex;
        } catch (Exception ex) {
            throw new RuntimeException(ex);
        }
    }

    /**
	 * 
	 * send HTTP GET
	 * 
	 */
    public String get(String url) {
        HttpResponse response = null;
        HttpEntity entity = null;
        try {
            HttpGet get = new HttpGet(url);
            response = this.getHttpClient().execute(get);
            entity = response.getEntity();
            if (entity == null) {
                throw new RuntimeException("response body was empty");
            }
            return EntityUtils.toString(entity);
        } catch (RuntimeException ex) {
            throw ex;
        } catch (Exception ex) {
            throw new RuntimeException(ex);
        } finally {
        }
    }

    public void shutdown() {
        try {
            this.getHttpClient().getConnectionManager().shutdown();
        } catch (Exception ignore) {
        }
    }

    protected StringBuilder getTwilioEndpoint() {
        StringBuilder endpoint = new StringBuilder();
        endpoint.append(TWILIO_ENDPOINT_BASE);
        return endpoint;
    }

    protected void checkCredentials() {
        if (this.getAccountSid() == null) {
            throw new IllegalStateException("Twilio account SID is null. You may need to call setCredentials(String, String)");
        }
        if (this.getAuthToken() == null) {
            throw new IllegalStateException("Twilio auth token is null. You may need to call setCredentials(String, String)");
        }
    }

    public Call call(String from, String to, String callbackUrl) {
        Map<String, String> params = new HashMap<String, String>();
        params.put("Caller", from);
        params.put("Called", to);
        params.put("Url", callbackUrl);
        TwilioResponse r = sendTwilioRequest("POST", this.getTwilioEndpoint().append("Accounts/").append(this.getAccountSid()).append("/Calls"), params);
        return r.getCall();
    }

    public SMSMessage sendSmsMessage(String from, String to, String message) {
        Map<String, String> params = new HashMap<String, String>();
        params.put("From", from);
        params.put("To", to);
        params.put("Body", message);
        TwilioResponse r = sendTwilioRequest("POST", this.getTwilioEndpoint().append("Accounts/").append(this.getAccountSid()).append("/SMS/Messages"), params);
        return r.getSMSMessage();
    }

    public Call getCall(String callSid) {
        if ((callSid == null) || (callSid.length() == 0)) {
            throw new IllegalArgumentException("callSid parameter: '" + callSid + "'");
        }
        TwilioResponse r = sendTwilioRequest("GET", this.getTwilioEndpoint().append("Accounts/").append(getAccountSid()).append("/Calls/").append(callSid));
        return r.getCall();
    }

    public Conferences getConferences() {
        TwilioResponse r = sendTwilioRequest("GET", this.getTwilioEndpoint().append("Accounts/").append(getAccountSid()).append("/Conferences"), new HashMap<String, String>());
        return r.getConferences();
    }

    public Calls getCalls() {
        return getCalls(-1);
    }

    public Calls getCalls(int number) {
        return getCalls(null, number);
    }

    public Calls getCalls(CallSearchCriteria criteria, int number) {
        Map<String, String> params = (criteria == null) ? (new HashMap<String, String>()) : criteria.getParameterMap();
        if (number > 0) {
            params.put("num", "" + number);
        }
        TwilioResponse r = sendTwilioRequest("GET", this.getTwilioEndpoint().append("Accounts/").append(getAccountSid()).append("/Calls"), params);
        return r.getCalls();
    }

    public IncomingPhoneNumbers getIncomingPhoneNumbers() {
        Map<String, String> params = new HashMap<String, String>();
        TwilioResponse r = sendTwilioRequest("GET", this.getTwilioEndpoint().append("Accounts/").append(getAccountSid()).append("/IncomingPhoneNumbers"), params);
        return r.getIncomingPhoneNumbers();
    }

    protected List<NameValuePair> buildNameValuePairList(Map<String, String> m) {
        List<NameValuePair> l = new ArrayList<NameValuePair>();
        for (String key : m.keySet()) {
            NameValuePair nvp = new BasicNameValuePair(key, m.get(key));
            l.add(nvp);
        }
        return l;
    }

    public OutgoingCallerIds getOutgoingCallerIds() {
        Map<String, String> params = new HashMap<String, String>();
        TwilioResponse r = sendTwilioRequest("GET", this.getTwilioEndpoint().append("Accounts/").append(getAccountSid()).append("/OutgoingCallerIds"), params);
        return r.getOutgoingCallerIds();
    }

    public Account getAccount() {
        Map<String, String> params = new HashMap<String, String>();
        TwilioResponse r = sendTwilioRequest("GET", this.getTwilioEndpoint().append("Accounts/").append(getAccountSid()), params);
        return r.getAccount();
    }

    public SMSMessages getSmsMessages() {
        return getSmsMessages(null, null, null);
    }

    public SMSMessages getSmsMessages(String from, String to, Date dateSent) {
        Map<String, String> params = new HashMap<String, String>();
        if (from != null) {
            params.put("From", from);
        }
        if (to != null) {
            params.put("To", to);
        }
        if (dateSent != null) {
            SimpleDateFormat fmt = new SimpleDateFormat("yyyy-mm-dd");
            String yyyymmdd = fmt.format(dateSent);
            params.put("DateSent", yyyymmdd);
        }
        TwilioResponse r = sendTwilioRequest("GET", this.getTwilioEndpoint().append("Accounts/").append(getAccountSid()).append("/SMS/Messages"), params);
        SMSMessages msgs = r.getSMSMessages();
        return msgs;
    }

    /**
	 * 
	 * @return all Recordings associated with your Twilio account
	 * 
	 */
    public Recordings getRecordings() {
        return getRecordings((String) null);
    }

    public Recordings getRecordings(Call c) {
        return getRecordings(c.getSid());
    }

    /**
	 * 
	 * @param callSid
	 * 
	 * @return all recordings associated with callSid 
	 * 
	 */
    public Recordings getRecordings(String callSid) {
        Map<String, String> params = new HashMap<String, String>();
        if (callSid != null) {
            params.put("CallSid", callSid);
        }
        TwilioResponse r = sendTwilioRequest("GET", this.getTwilioEndpoint().append("Accounts/").append(getAccountSid()).append("/Recordings"), params);
        Recordings recordings = r.getRecordings();
        return recordings;
    }

    protected void setMp3Url(Recordings recordings) {
        for (Recording r : recordings) {
            setMp3Url(r);
        }
    }

    protected void setMp3Url(Recording r) {
        r.setMp3Url(TWILIO_ENDPOINT_BASE + "Account/" + getAccountSid() + "/Recordings/" + r.getSid() + RecordingFormat.MP3.getFileExtension());
    }

    public byte[] getRecordingBytes(String recordingSid) {
        return getRecordingBytes(recordingSid, RecordingFormat.DEFAULT);
    }

    public byte[] getRecordingBytes(String recordingSid, RecordingFormat format) {
        StringBuilder url = this.getTwilioEndpoint().append("Accounts/").append(getAccountSid()).append("/Recordings/").append(recordingSid).append(format.getFileExtension());
        return getBytesFromUrl(url);
    }

    public byte[] getRecordingBytes(Recording r, RecordingFormat format) {
        return getRecordingBytes(r.getSid(), format);
    }

    public byte[] getRecordingBytes(Recording r) {
        return getRecordingBytes(r.getSid(), RecordingFormat.DEFAULT);
    }

    public void deleteRecording(String recordingSid) {
        StringBuilder url = this.getTwilioEndpoint().append("Accounts/").append(getAccountSid()).append("/Recordings/").append(recordingSid);
        sendTwilioRequest("DELETE", url);
    }

    public void deleteRecording(Recording r) {
        deleteRecording(r.getSid());
    }

    public void redirectCall(String callSid, String twimlUrl) {
        StringBuilder url = this.getTwilioEndpoint().append("Accounts/").append(getAccountSid()).append("/Calls/").append(callSid);
        url.append("?CurrentUrl=");
        url.append(encodeParameter(twimlUrl));
        sendTwilioRequest("POST", url);
    }
}
