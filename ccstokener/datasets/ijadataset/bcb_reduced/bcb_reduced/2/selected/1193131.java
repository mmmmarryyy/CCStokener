package org.alexd.jsonrpc;

import java.io.IOException;
import java.io.UnsupportedEncodingException;
import javax.security.cert.CertificateException;
import org.apache.http.HttpEntity;
import org.apache.http.HttpResponse;
import org.apache.http.ProtocolVersion;
import org.apache.http.client.ClientProtocolException;
import org.apache.http.client.HttpClient;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.impl.client.DefaultHttpClient;
import org.apache.http.params.BasicHttpParams;
import org.apache.http.params.HttpConnectionParams;
import org.apache.http.params.HttpParams;
import org.apache.http.params.HttpProtocolParams;
import org.apache.http.util.EntityUtils;
import org.json.JSONException;
import org.json.JSONObject;
import biz.source_code.base64Coder.Base64Coder;
import android.content.Context;
import android.util.Log;

/**
 * Implementation of JSON-RPC over HTTP/POST
 */
public class JSONRPCHttpClient extends JSONRPCClient {

    private HttpClient httpClient;

    private String serviceUri;

    private String serviceUser;

    private String servicePass;

    private static final ProtocolVersion PROTOCOL_VERSION = new ProtocolVersion("HTTP", 1, 0);

    /**
	 * Construct a JsonRPCClient with the given service uri
	 * 
	 * @param uri
	 *            uri of the service
	 */
    public JSONRPCHttpClient(Context context, String uri, String user, String pass, String lastTrustedCert) {
        httpClient = HttpClientTrustAll.getNewHttpClient(context, lastTrustedCert);
        serviceUri = uri;
        serviceUser = user;
        servicePass = pass;
    }

    protected JSONObject doJSONRequest(JSONObject jsonRequest) throws JSONRPCException {
        HttpPost request = new HttpPost(serviceUri);
        HttpParams params = new BasicHttpParams();
        HttpConnectionParams.setConnectionTimeout(params, getConnectionTimeout());
        HttpConnectionParams.setSoTimeout(params, getSoTimeout());
        HttpProtocolParams.setVersion(params, PROTOCOL_VERSION);
        request.setParams(params);
        request.addHeader("Authorization", "Basic " + Base64Coder.encodeString(serviceUser + ":" + servicePass));
        HttpEntity entity;
        try {
            entity = new JSONEntity(jsonRequest);
        } catch (UnsupportedEncodingException e1) {
            throw new JSONRPCException("Unsupported encoding", e1);
        }
        request.setEntity(entity);
        try {
            long t = System.currentTimeMillis();
            HttpResponse response = httpClient.execute(request);
            t = System.currentTimeMillis() - t;
            Log.d("json-rpc", "Request time :" + t);
            String responseString = EntityUtils.toString(response.getEntity());
            responseString = responseString.trim();
            JSONObject jsonResponse = new JSONObject(responseString);
            if (jsonResponse.has("error")) {
                Object jsonError = jsonResponse.get("error");
                if (!jsonError.equals(null)) throw new JSONRPCException(jsonResponse.get("error"));
                return jsonResponse;
            } else {
                return jsonResponse;
            }
        } catch (ClientProtocolException e) {
            throw new JSONRPCException("HTTP error", e);
        } catch (IOException e) {
            throw new JSONRPCException("IO error", e);
        } catch (JSONException e) {
            throw new JSONRPCException("Invalid JSON response", e);
        }
    }
}
