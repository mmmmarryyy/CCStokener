package com.google.apps.easyconnect.easyrp.client.basic.util;

import java.io.IOException;
import java.net.HttpURLConnection;
import java.net.URL;
import java.util.logging.Logger;
import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;
import com.google.common.annotations.VisibleForTesting;

/**
 * Wraps the request to the googleapis.com.
 * 
 * @author guibinkong@google.com (Guibin Kong)
 */
public class GitServiceClientImpl implements GitServiceClient {

    private static final Logger log = Logger.getLogger(GitServiceClient.class.getName());

    private static final String VERIFY_URL = "https://www.googleapis.com/rpc?pp=1&key=";

    private String developerKey;

    public GitServiceClientImpl(String developerKey) {
        this.developerKey = developerKey;
    }

    public String getDeveloperKey() {
        return developerKey;
    }

    /**
   * Builds the post data for the request to googleapi.com.
   * 
   * @param requestUri the request URI of the IDP response.
   * @param postBody the post data of the IDP response.
   * @return the post data in JSONArray format as required by googleapi.com.
   */
    @VisibleForTesting
    JSONArray buildPostData(String requestUri, String postBody) {
        JSONArray requests = new JSONArray();
        JSONObject request = new JSONObject();
        JSONObject params = new JSONObject();
        try {
            requests.put(request);
            request.put("method", "identitytoolkit.relyingparty.verifyAssertion");
            request.put("apiVersion", "v1");
            request.put("params", params);
            params.put("requestUri", requestUri);
            params.put("postBody", postBody);
            params.put("returnOauthToken", true);
        } catch (JSONException e) {
            log.severe(e.getMessage());
        }
        return requests;
    }

    /**
   * Verifying the response of IDP, and return the profile data if success.
   * 
   * @param requestUri the request URI of the IDP response.
   * @param postBody the post data of the IDP response.
   * @return the profile data of the user, or nothing in it if failed.
   */
    public JSONObject verifyResponse(String requestUri, String postBody) {
        log.fine("verifyResponse:\nrequestUri = [" + requestUri + "]\npostBody = [" + postBody + "]");
        JSONObject result = new JSONObject();
        try {
            URL url = new URL(VERIFY_URL + this.developerKey);
            String postData = buildPostData(requestUri, postBody).toString();
            log.fine("verifyResponse postData:\n" + postData);
            HttpURLConnection httpurlconnection = (HttpURLConnection) url.openConnection();
            httpurlconnection.setDoOutput(true);
            httpurlconnection.setRequestProperty("Content-Type", "application/json");
            httpurlconnection.setRequestMethod("POST");
            httpurlconnection.getOutputStream().write(postData.getBytes());
            httpurlconnection.getOutputStream().flush();
            httpurlconnection.getOutputStream().close();
            String content = Utils.streamToString(httpurlconnection.getInputStream(), httpurlconnection.getContentEncoding());
            log.fine("verifyResponse return: " + content);
            try {
                JSONArray response = new JSONArray(content);
                if (response != null && response.length() > 0) {
                    JSONObject ret = response.getJSONObject(0);
                    result = convertJson(ret);
                }
            } catch (JSONException e) {
                log.severe(e.getMessage());
            }
        } catch (IOException e) {
            log.severe(e.getMessage());
        } catch (RuntimeException e) {
            log.severe(e.getMessage());
        }
        return result;
    }

    /**
   * Changes the format of the returned JSONObject. RP can overwrite this method to transform the
   * profile JSONObject as its special requirement.
   * 
   * @param json the JSONObject from googleapis.com
   * @return the transformed JSONObject
   * @throws JSONException if error occurs when put data into JSONObject
   */
    protected JSONObject convertJson(JSONObject json) throws JSONException {
        JSONObject ret = new JSONObject();
        if (json.has("error")) {
            ret.put("error", json.get("error"));
        } else if (json.has("result") && (json.get("result") instanceof JSONObject) && json.getJSONObject("result") != null) {
            JSONObject result = json.getJSONObject("result");
            if (result.has("verifiedEmail")) {
                ret.put("email", result.get("verifiedEmail"));
                ret.put("trusted", true);
            } else if (result.has("email")) {
                ret.put("email", result.get("email"));
                ret.put("trusted", false);
            }
            if (result.has("firstName")) {
                ret.put("firstName", result.get("firstName"));
            }
            if (result.has("lastName")) {
                ret.put("lastName", result.get("lastName"));
            }
            if (result.has("fullName")) {
                ret.put("fullName", result.get("fullName"));
                String fullName = result.getString("fullName").trim();
                int index = fullName.lastIndexOf(" ");
                index = (index < 0) ? fullName.length() : index;
                if (!result.has("firstName")) {
                    ret.put("firstName", fullName.substring(0, index));
                }
                if (index < fullName.length() && !result.has("lastName")) {
                    ret.put("lastName", fullName.substring(index + 1).trim());
                }
            }
            if (result.has("photoUrl")) {
                ret.put("photoUrl", result.get("photoUrl"));
            }
            if (result.has("context")) {
                ret.put("context", result.get("context"));
            }
            if (result.has("oauthAccessToken")) {
                ret.put("oauthAccessToken", result.get("oauthAccessToken"));
            }
            if (result.has("oauthExpireIn")) {
                ret.put("oauthExpireIn", result.get("oauthExpireIn"));
            }
            if (result.has("oauthRefreshToken")) {
                ret.put("oauthRefreshToken", result.getInt("oauthRefreshToken"));
            }
            if (result.has("oauthRequestToken")) {
                ret.put("oauthRequestToken", result.get("oauthRequestToken"));
            }
        }
        return ret;
    }
}
