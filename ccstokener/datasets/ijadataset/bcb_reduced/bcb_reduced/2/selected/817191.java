package com.vzaar.transport.httpclient4;

import java.io.ByteArrayInputStream;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.io.UnsupportedEncodingException;
import java.net.URLEncoder;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import org.apache.http.HttpException;
import org.apache.http.HttpResponse;
import org.apache.http.NameValuePair;
import org.apache.http.client.HttpClient;
import org.apache.http.client.entity.UrlEncodedFormEntity;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.client.methods.HttpUriRequest;
import org.apache.http.conn.scheme.Scheme;
import org.apache.http.conn.scheme.SchemeRegistry;
import org.apache.http.entity.StringEntity;
import org.apache.http.entity.mime.MultipartEntity;
import org.apache.http.entity.mime.content.FileBody;
import org.apache.http.entity.mime.content.StringBody;
import org.apache.http.impl.client.DefaultHttpClient;
import org.apache.http.message.BasicNameValuePair;
import org.apache.http.protocol.BasicHttpContext;
import org.apache.http.protocol.HttpContext;
import org.apache.http.util.EntityUtils;
import oauth.signpost.OAuthConsumer;
import oauth.signpost.exception.OAuthExpectationFailedException;
import oauth.signpost.exception.OAuthMessageSignerException;
import oauth.signpost.signature.SignatureMethod;
import com.vzaar.UploadProgressCallback;
import com.vzaar.VzaarException;
import com.vzaar.transport.VzaarTransport;
import com.vzaar.transport.VzaarTransportResponse;

/**
 *	VzaarTransport implementation using commons HttpClient 4.
 * 
 * @author Marc G. Smith
 */
class HttpClientTransport implements VzaarTransport {

    private HttpClient client;

    private HttpContext state;

    private String apiUrl;

    private OAuthConsumer consumer;

    private OutputStream debugOut;

    /**
	 * Constructor.
	 */
    public HttpClientTransport() {
        this.client = new DefaultHttpClient();
        this.state = new BasicHttpContext();
    }

    /**
	 * Send any get request. If OAuth credentials have been added then OAuth
	 * authentication headers are sent.
	 * 
	 * @param uri the uri of the request which is appended to the API URL
	 * @param parameters request parameters.
	 */
    public VzaarTransportResponse sendGetRequest(String uri, Map<String, String> parameters) throws VzaarException {
        try {
            String url = constructGetUrl(uri, parameters);
            if (debugOut != null) {
                debugOut.write(("\n>> Request URL = " + url + "\n").getBytes());
                debugOut.write((">> Request Method = GET\n").getBytes());
            }
            HttpGet method = new HttpGet(url);
            return excuteMethod(method);
        } catch (Exception e) {
            throw new VzaarException(e.getMessage());
        }
    }

    /**
	 * Send any post request. If OAuth credentials have been added then OAuth
	 * authentication headers are sent.
	 * 
	 * @param uri the method URI
	 * @param parameters request parameters
	 * @return the call response 
	 */
    public VzaarTransportResponse sendPostRequest(String uri, Map<String, String> parameters) throws VzaarException {
        try {
            String url = apiUrl + uri;
            if (debugOut != null) {
                debugOut.write(("\n>> Request URL = " + url + "\n").getBytes());
                debugOut.write((">> Request Method = POST\n").getBytes());
                debugOut.write((">> Request Parameters = " + parameters + "\n").getBytes());
            }
            HttpPost method = new HttpPost(url);
            addPostParameters(method, parameters);
            return excuteMethod(method);
        } catch (Exception e) {
            throw new VzaarException(e.getMessage());
        }
    }

    /**
	 * Send a post request with an XML body. If OAuth credentials have been 
	 * added then OAuth authentication headers are sent.
	 * 
	 * @param uri the method URI
	 * @param xml the XML content for the body of the request
	 * @return the call response 
	 */
    public VzaarTransportResponse sendPostXmlRequest(String uri, String xml) throws VzaarException {
        try {
            String url = apiUrl + uri;
            if (debugOut != null) {
                debugOut.write(("\n>> Request URL = " + url + "\n").getBytes());
                debugOut.write((">> Request Method = POST\n").getBytes());
                debugOut.write((">> Request Body = \n>> " + xml.replaceAll("\n", "\n>> ") + "\n").getBytes());
            }
            HttpPost method = new HttpPost(url);
            method.setHeader("Content-type", "text/xml");
            method.setEntity(new StringEntity(xml));
            return excuteMethod(method);
        } catch (Exception e) {
            throw new VzaarException(e.getMessage());
        }
    }

    /**
	 * Upload a file to S3. This method doesn't use OAuth. If the callback is
	 * not null then the method should send progress reports to the callback.
	 * There is no need to call the error or complete calls on the callback
	 * as these are done by the Vzaar calling class.
	 *
	 * @param url The full url to the S3 bucket
	 * @param paramets the query parameters to send
	 * @param file the file to upload
	 * @param callback the callback to register for updates or null if not
	 *  required
	 * @return the call response 
	 */
    public VzaarTransportResponse uploadToS3(String url, Map<String, String> parameters, File file, UploadProgressCallback callback) throws VzaarException {
        try {
            HttpPost method = new HttpPost(url);
            MultipartEntity parts = new MultipartEntity();
            for (Entry<String, String> entry : parameters.entrySet()) {
                String value = entry.getValue();
                parts.addPart(entry.getKey(), new StringBody(value));
            }
            parts.addPart("file", callback == null ? new FileBody(file) : new ProgressFileBody(file, callback));
            if (debugOut != null) {
                debugOut.write(("\n>> Request URL = " + url + "\n").getBytes());
                debugOut.write((">> Request Method = POST\n").getBytes());
                debugOut.write((">> Request Parameters = " + parameters + "\n").getBytes());
                debugOut.write((">> Request File = " + file.getAbsolutePath() + "\n").getBytes());
            }
            method.setEntity(parts);
            HttpResponse response = client.execute(method);
            String body = EntityUtils.toString(response.getEntity());
            if (body == null) body = "";
            if (debugOut != null) {
                debugOut.write(("<< Response Code = " + response.getStatusLine().getStatusCode() + "\n").getBytes());
                debugOut.write(("<< Response Line = " + response.getStatusLine().getReasonPhrase() + "\n").getBytes());
                debugOut.write(("<< Response Body = \n<< " + body.replaceAll("\n", "\n<< ") + "\n").getBytes());
            }
            return new VzaarTransportResponse(response.getStatusLine().getStatusCode(), response.getStatusLine().getReasonPhrase(), new ByteArrayInputStream(body.getBytes()));
        } catch (Exception e) {
            throw new VzaarException(e.getMessage(), e);
        }
    }

    /**
	 * Set the base API URL, such as http://vzaar.com/api/
	 * 
	 * @param url the base API URL
	 */
    public void setUrl(String url) {
        if (!url.endsWith("/")) {
            url += "/";
        }
        if (!url.endsWith("api/")) {
            url += "api/";
        }
        this.apiUrl = url;
    }

    /**
	 * Set the OAuth 2 party tokens. If these are supplied then all calls to
	 * <a href="http://vzaar.com/">vzaar.com</a> will send OAuth credentials.
	 * 
	 * @param accessToken the users login name
	 * @param accessTokenSecret the secret token supplied by vzaar
	 */
    public void setOAuthTokens(String accessToken, String accessTokenSecret) {
        if (accessToken == null) {
            consumer = null;
        } else {
            consumer = new CommonsHttpOAuthConsumer("", "", SignatureMethod.HMAC_SHA1);
            consumer.setTokenWithSecret(accessToken, accessTokenSecret);
        }
    }

    /**
	 * Construct the url from the apiUrl and uri and parameters. The parameter
	 * values are encoded with UTF-8.
	 * 
	 * @param uri the method uri
	 * @param parameters the query parameters
	 */
    private String constructGetUrl(String uri, Map<String, String> parameters) {
        StringBuilder url = new StringBuilder(apiUrl);
        url.append(uri);
        if (parameters != null) {
            boolean first = true;
            for (Entry<String, String> entry : parameters.entrySet()) {
                String value = entry.getValue();
                if (value == null || value.equals("null")) continue;
                try {
                    url.append(first ? '?' : '&').append(entry.getKey()).append('=').append(URLEncoder.encode(value, "UTF-8"));
                } catch (UnsupportedEncodingException e) {
                    url.append(entry.getValue());
                }
                first = false;
            }
        }
        return url.toString();
    }

    /**
	 * Add string parameters to the post method.
	 * 
	 * @param the method
	 * @param the parameters  
	 */
    private void addPostParameters(HttpPost method, Map<String, String> parameters) {
        if (parameters != null) {
            List<NameValuePair> formparams = new ArrayList<NameValuePair>(parameters.size());
            for (Entry<String, String> entry : parameters.entrySet()) {
                formparams.add(new BasicNameValuePair(entry.getKey(), entry.getValue()));
            }
            try {
                UrlEncodedFormEntity entity = new UrlEncodedFormEntity(formparams, "UTF-8");
                method.setEntity(entity);
            } catch (UnsupportedEncodingException e) {
                e.printStackTrace();
            }
        }
    }

    /**
	 * Execute an HttpMethod, if OAuth credentials have been added then OAuth
	 * authentication headers are sent. 
	 * 
	 * @param method the method to execute
	 * @return the response wrapper
	 */
    private VzaarTransportResponse excuteMethod(HttpUriRequest method) throws OAuthMessageSignerException, OAuthExpectationFailedException, IOException, HttpException {
        if (consumer != null) {
            consumer.sign(method);
        }
        HttpResponse response = client.execute(method, state);
        InputStream in = null;
        if (debugOut != null) {
            String body = EntityUtils.toString(response.getEntity());
            in = new ByteArrayInputStream(body.getBytes());
            debugOut.write(("<< Response Code = " + response.getStatusLine().getStatusCode() + "\n").getBytes());
            debugOut.write(("<< Response Line = " + response.getStatusLine().getReasonPhrase() + "\n").getBytes());
            debugOut.write(("<< Response Body = \n<< " + body.replaceAll("\n", "\n<< ") + "\n").getBytes());
        } else {
            in = response.getEntity().getContent();
        }
        return new VzaarTransportResponse(response.getStatusLine().getStatusCode(), response.getStatusLine().getReasonPhrase(), in);
    }

    /**
	 * Add debugging output. Set to null to stop debugging.
	 * 
	 * @param out the output stream to send debug output to.
	 */
    public void setDebugStream(OutputStream out) {
        this.debugOut = out;
    }

    /**
	 * Set the connection factory to use SSLv3 only, i.e. no TLS. This
	 * is for an issue with production servers where SSL is failing. 
	 */
    public void setSsl3Only() {
        SchemeRegistry registry = client.getConnectionManager().getSchemeRegistry();
        Scheme scheme = registry.getScheme("https");
        if (!(scheme.getSocketFactory() instanceof Ssl3OnlyProtocolFactory)) {
            Scheme ssl3Scheme = new Scheme(scheme.getName(), new Ssl3OnlyProtocolFactory(scheme.getSocketFactory()), scheme.getDefaultPort());
            registry.register(ssl3Scheme);
        }
    }
}
