package com.freebase.api;

import java.io.IOException;
import java.io.InputStreamReader;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import org.apache.http.HttpEntity;
import org.apache.http.HttpResponse;
import org.apache.http.NameValuePair;
import org.apache.http.client.ClientProtocolException;
import org.apache.http.client.HttpClient;
import org.apache.http.client.entity.UrlEncodedFormEntity;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.client.methods.HttpRequestBase;
import org.apache.http.client.utils.URLEncodedUtils;
import org.apache.http.impl.client.DefaultHttpClient;
import org.apache.http.message.BasicNameValuePair;
import com.freebase.json.JSON;
import static com.freebase.json.JSON.o;
import static com.freebase.json.JSON.a;

/**
 * This class is the main access point to the Freebase API.
 * 
 * You can obtain two flavors of this class, one that operates
 * on freebase.com and one that operates on sandbox-freebase.com.
 * 
 * sandbox-freebase.com is a scratch system that works exactly like
 * freebase.com but its data get reset every week and brought back
 * in synch with freebase.com.
 * 
 * If your programs write data in Freebase, we highly encourage 
 * to use sandbox first for extensive testing before turning the
 * writes over to Freebase.com.
 * 
 * Use <code>Freebase.getFreebase()</code> to obtain a instance
 * of this class that talks to freebase.com and <code>Freebase.getFreebaseSandbox()</code>
 * to obtain one that talks to sandbox-freebase.com.
 *
 */
public class Freebase extends JSONTransport {

    private static final String FREEBASE_API_URL = "http://www.freebase.com";

    private static final String FREEBASE_SANDBOX_API_URL = "http://www.sandbox-freebase.com";

    private static final String LOGIN_API = "/api/account/login";

    private static final String MQLREAD_API = "/api/service/mqlread";

    private static final String MQLWRITE_API = "/api/service/mqlwrite";

    private static final String SEARCH_API = "/api/service/search";

    private static final String GEOSEARCH_API = "/api/service/geosearch";

    private static final String UPLOAD_API = "/api/service/upload";

    private static final String BLOB_API_PREFIX = "/api/trans/";

    private static final String TOPIC_API_PREFIX = "/experimental/topic/";

    /**
     * Obtain an instance of this class that connects to the API on freebase.com
     * NOTE: if your program writes data, we highly encourage you to test it
     * against the sandbox instance first.
     */
    public static Freebase getFreebase() {
        return new Freebase(FREEBASE_API_URL);
    }

    /**
     * Obtain an instance of this class that connects to the API on sandbox-freebase.com
     */
    public static Freebase getFreebaseSandbox() {
        return new Freebase(FREEBASE_SANDBOX_API_URL);
    }

    private final String host;

    private String credential_cookie = null;

    private Freebase(String host) {
        this.host = host;
    }

    /**
     * For API calls that write data in Freebase, your requests must be authenticated.
     * Calling this method with your Freebase username and password will 
     * provide enough credentials for your write API calls to be authorized. 
     * NOTE: if you don't have a Freebase account, you can get one for free at 
     * <a href="http://www.freebase.com">freebase.com</a>
     */
    public void sign_in(String username, String password) {
        try {
            HttpClient httpclient = new DefaultHttpClient();
            HttpPost post = new HttpPost(host + LOGIN_API);
            List<NameValuePair> formparams = new ArrayList<NameValuePair>();
            formparams.add(new BasicNameValuePair("username", username));
            formparams.add(new BasicNameValuePair("password", password));
            post.setEntity(new UrlEncodedFormEntity(formparams, "UTF-8"));
            post.setHeader("Content-Type", "application/x-www-form-urlencoded");
            HttpResponse response = httpclient.execute(post);
            this.credential_cookie = response.getFirstHeader("Set-Cookie").getValue();
        } catch (ClientProtocolException e) {
            throw new FreebaseException(e);
        } catch (IOException e) {
            throw new FreebaseException(e);
        } catch (IllegalStateException e) {
            throw new FreebaseException(e);
        } catch (ClassCastException e) {
            throw new FreebaseException(e);
        }
    }

    /**
     * Sign out and clean up the authentication credentials.
     */
    public void sign_out() {
        this.credential_cookie = null;
    }

    /**
     * Create a fully qualified URL for the web service identified by the given path
     * (this is used to switch between freebase.com and sandbox-freebase.com)
     */
    protected String getURL(String path) {
        return this.host + path;
    }

    /**
     * Add authentication credentials to the HTTP request method
     */
    protected void sign(HttpRequestBase method) {
        if (this.credential_cookie == null) throw new FreebaseException("Can't sign the request since there are no authentication credentials. Have you signed in yet?");
        method.setHeader("Cookie", this.credential_cookie);
    }

    public JSON mqlread(JSON query) {
        return mqlread(query, null, null);
    }

    /**
     * http://www.freebase.com/docs/web_services/mqlread
     */
    public JSON mqlread(JSON query, JSON envelope, JSON params) {
        if (query == null) throw new FreebaseException("Query can't be null");
        if (envelope == null) envelope = o();
        envelope.put("query", jsonize(query));
        envelope.put("escape", false);
        List<NameValuePair> qparams = transform_params(params);
        qparams.add(new BasicNameValuePair("query", envelope.toString()));
        return invoke(MQLREAD_API, qparams);
    }

    public JSON mqlread_multiple(JSON queries) {
        return mqlread_multiple(queries, null, null);
    }

    /**
     * http://www.freebase.com/docs/web_services/mqlread
     */
    @SuppressWarnings("unchecked")
    public JSON mqlread_multiple(JSON queries, JSON envelopes, JSON params) {
        if (queries == null || queries.object().keySet().size() == 0) throw new FreebaseException("Query can't be null or empty");
        if (envelopes == null) envelopes = o();
        JSON q = o();
        for (Object entry : queries.object().entrySet()) {
            Map.Entry<String, JSON> e = (Map.Entry<String, JSON>) entry;
            JSON envelope = envelopes.get(e.getKey());
            if (envelope == null) envelope = o();
            envelope.put("query", jsonize(e.getValue()));
            envelope.put("escape", false);
            q.put(e.getKey(), envelope);
        }
        List<NameValuePair> qparams = transform_params(params);
        qparams.add(new BasicNameValuePair("queries", q.toString()));
        return invoke(MQLREAD_API, qparams);
    }

    /**
     * http://www.freebase.com/docs/web_services/search
     */
    public JSON search(String query) {
        return search(query, null);
    }

    /**
     * http://www.freebase.com/docs/web_services/search
     */
    public JSON search(String query, JSON options) {
        if (query == null || query.trim().length() == 0) throw new FreebaseException("You must provide a string to search");
        List<NameValuePair> qparams = transform_params(options);
        qparams.add(new BasicNameValuePair("query", query));
        qparams.add(new BasicNameValuePair("format", "json"));
        return invoke(SEARCH_API, qparams);
    }

    /**
     * http://www.freebase.com/docs/geosearch
     */
    public JSON geosearch(String query) {
        return geosearch(query, null);
    }

    /**
     * http://www.freebase.com/docs/geosearch
     */
    public JSON geosearch(String location, JSON options) {
        if (location == null || location.trim().length() == 0) throw new FreebaseException("You must provide a location to geoearch");
        List<NameValuePair> qparams = transform_params(options);
        qparams.add(new BasicNameValuePair("location", location));
        qparams.add(new BasicNameValuePair("format", "json"));
        return invoke(GEOSEARCH_API, qparams);
    }

    public String get_blob(String id) {
        return get_blob(id, null);
    }

    /**
     * http://www.freebase.com/docs/web_services/trans_raw
     * http://www.freebase.com/docs/web_services/trans_blurb
     */
    public String get_blob(String id, JSON options) {
        if (id == null || id.trim().length() == 0) throw new FreebaseException("You must provide the id of the blob you want");
        String path = BLOB_API_PREFIX;
        String mode = (options != null && options.has("mode")) ? options.get("mode").string() : "raw";
        if ("raw".equals(mode) || "unsafe".equals(mode) || "blurb".equals(mode)) {
            path += mode;
        } else {
            throw new FreebaseException("Invalid mode; must be 'raw' or 'blurb' or 'unsafe'");
        }
        path += id;
        List<NameValuePair> qparams = transform_params(options);
        String url = host + path + "?" + URLEncodedUtils.format(qparams, "UTF-8");
        try {
            HttpClient httpclient = new DefaultHttpClient();
            HttpRequestBase method = new HttpGet(url);
            method.setHeader("X-Requested-With", "1");
            HttpResponse response = httpclient.execute(method);
            HttpEntity entity = response.getEntity();
            if (entity != null) {
                return stringify(new InputStreamReader(entity.getContent(), "UTF-8"));
            } else {
                throw new FreebaseException("Response was empty");
            }
        } catch (ClientProtocolException e) {
            throw new FreebaseException(e);
        } catch (IOException e) {
            throw new FreebaseException(e);
        } catch (IllegalStateException e) {
            throw new FreebaseException(e);
        } catch (ClassCastException e) {
            throw new FreebaseException(e);
        }
    }

    /**
     * http://www.freebase.com/docs/topic_api
     */
    public JSON get_topic(String id) {
        return get_topic(id, null);
    }

    /**
     * http://www.freebase.com/docs/topic_api
     */
    public JSON get_topic(String id, JSON options) {
        if (id == null || id.trim().length() == 0) throw new FreebaseException("Invalid Topic ID");
        if (id.indexOf(',') > -1) throw new FreebaseException("Use get_topic_multi if you want to retrieve multiple topics");
        return topic(a(id), options).get(id);
    }

    /**
     * http://www.freebase.com/docs/topic_api
     */
    public JSON get_topic_multiple(JSON ids) {
        return get_topic_multiple(ids, null);
    }

    /**
     * http://www.freebase.com/docs/topic_api
     */
    public JSON get_topic_multiple(JSON ids, JSON options) {
        if (ids == null || ids.array().size() == 0) throw new FreebaseException("You must provide a non-empty array of Topic IDs");
        return topic(ids, options);
    }

    private JSON topic(JSON ids, JSON options) {
        String path = TOPIC_API_PREFIX;
        String mode = (options != null && options.has("mode")) ? options.get("mode").string() : "standard";
        if ("standard".equals(mode) || "basic".equals(mode)) {
            path += mode;
        } else {
            throw new FreebaseException("Invalid mode; must be 'basic' or 'standard'");
        }
        List<NameValuePair> qparams = transform_params(options);
        qparams.add(new BasicNameValuePair("id", join(ids, ",")));
        return invoke(path, qparams);
    }

    /**
     * http://www.freebase.com/docs/web_services/mqlwrite
     */
    public JSON mqlwrite(JSON query) {
        return mqlwrite(query, null, null);
    }

    /**
     * http://www.freebase.com/docs/web_services/mqlwrite
     */
    public JSON mqlwrite(JSON query, JSON envelope, JSON params) {
        if (this.credential_cookie == null) throw new FreebaseException("Can't write without being signed in");
        if (query == null) throw new FreebaseException("Query can't be null");
        if (envelope == null) envelope = o();
        envelope.put("query", jsonize(query));
        envelope.put("escape", false);
        List<NameValuePair> qparams = transform_params(params);
        qparams.add(new BasicNameValuePair("query", envelope.toString()));
        return post(MQLWRITE_API, qparams, true);
    }

    /**
     * http://www.freebase.com/docs/web_services/upload
     */
    public JSON upload(String content, String media_type) {
        return upload(content, media_type, null);
    }

    /**
     * http://www.freebase.com/docs/web_services/upload
     */
    public JSON upload(String content, String media_type, JSON options) {
        if (this.credential_cookie == null) throw new FreebaseException("Can't write without being signed in");
        if (content == null || content.trim().length() == 0) throw new FreebaseException("You must specify what content to upload");
        if (media_type == null || media_type.trim().length() == 0) throw new FreebaseException("You must specify a media type for the content to upload");
        Map<String, String> headers = new HashMap<String, String>();
        headers.put("content-type", media_type);
        List<NameValuePair> qparams = transform_params(options);
        String url = getURL(UPLOAD_API) + "?" + URLEncodedUtils.format(qparams, "UTF-8");
        return post(url, headers, content, true);
    }
}
