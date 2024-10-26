package net.sylvek.where;

import android.os.Build;
import org.apache.http.HttpResponse;
import org.apache.http.NameValuePair;
import org.apache.http.client.ClientProtocolException;
import org.apache.http.client.HttpClient;
import org.apache.http.client.entity.UrlEncodedFormEntity;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.impl.client.DefaultHttpClient;
import org.apache.http.message.BasicNameValuePair;
import org.apache.http.params.BasicHttpParams;
import org.apache.http.params.HttpParams;
import org.apache.http.params.HttpProtocolParams;
import org.apache.http.protocol.HTTP;
import org.apache.http.util.EntityUtils;
import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

public class SylvekClient {

    public static final String CURRENT_VERSION = "v2.3beta-3";

    public static final String CURRENT_API_VERSION = "1.0";

    public static final String HOST = "https://ou-android.appspot.com/";

    public static final String API_VERSION = HOST + "api.version";

    public static final String HELP_WEB_PAGE = HOST + "help.html";

    public static final String STATIC_WEB_MAP = HOST + "static.jsp";

    private static final String FRIENDS_URI = HOST + "rest/friends";

    private static final String UIDS_URI = HOST + "rest/uids?kind=all&elements=50";

    private static final String UIDS_ONLINE_URI = HOST + "rest/uids?kind=online&elements=50";

    private static final String UIDS_NEARME_URI = HOST + "rest/uids?kind=nearme&elements=50";

    private static final String UPDATE_URI = HOST + "rest/update";

    private static final String DELETE_URI = HOST + "rest/delete";

    private static HttpParams params = new BasicHttpParams();

    static {
        HttpProtocolParams.setUserAgent(params, "Android/" + Build.DISPLAY + "/" + CURRENT_VERSION);
    }

    private SylvekClient() {
    }

    public static String getApiVersion() throws ClientProtocolException, IOException {
        HttpClient client = new DefaultHttpClient(params);
        HttpGet get = new HttpGet(API_VERSION);
        HttpResponse response = client.execute(get);
        if (response.getStatusLine().getStatusCode() == 200) {
            return EntityUtils.toString(response.getEntity());
        }
        throw new IOException("bad http response:" + response.getStatusLine().getReasonPhrase());
    }

    public static JSONArray getFriends(long[] uids) throws ClientProtocolException, IOException, JSONException {
        HttpClient client = new DefaultHttpClient(params);
        HttpPost post = new HttpPost(FRIENDS_URI);
        List<NameValuePair> parameters = new ArrayList<NameValuePair>();
        parameters.add(new BasicNameValuePair("uids", arrayToString(uids, ",")));
        post.setEntity(new UrlEncodedFormEntity(parameters));
        HttpResponse response = client.execute(post);
        if (response.getStatusLine().getStatusCode() == 200) {
            String res = EntityUtils.toString(response.getEntity());
            return new JSONArray(res);
        }
        throw new IOException("bad http response:" + response.getStatusLine().getReasonPhrase());
    }

    public static JSONObject getFriend(long uid) throws ClientProtocolException, IOException, JSONException {
        HttpClient client = new DefaultHttpClient(params);
        HttpPost post = new HttpPost(FRIENDS_URI);
        List<NameValuePair> parameters = new ArrayList<NameValuePair>();
        parameters.add(new BasicNameValuePair("uids", arrayToString(new long[] { uid }, ",")));
        post.setEntity(new UrlEncodedFormEntity(parameters));
        HttpResponse response = client.execute(post);
        if (response.getStatusLine().getStatusCode() == 200) {
            String res = EntityUtils.toString(response.getEntity());
            JSONArray result = new JSONArray(res);
            return result.getJSONObject(0);
        }
        throw new IOException("bad http response:" + response.getStatusLine().getReasonPhrase());
    }

    public static long[] getOnlineUids(String myUid) throws ClientProtocolException, IOException, JSONException {
        HttpClient client = new DefaultHttpClient(params);
        HttpGet get = new HttpGet(UIDS_ONLINE_URI);
        HttpResponse response = client.execute(get);
        if (response.getStatusLine().getStatusCode() == 200) {
            String res = EntityUtils.toString(response.getEntity());
            JSONArray result = new JSONArray(res);
            long[] friends = new long[result.length()];
            int uid = Integer.parseInt(myUid);
            for (int i = 0; i < result.length(); i++) {
                if (uid != result.getInt(i)) {
                    friends[i] = result.getInt(i);
                }
            }
            return friends;
        }
        throw new IOException("bad http response:" + response.getStatusLine().getReasonPhrase());
    }

    public static long[] getUids(String myUid) throws ClientProtocolException, IOException, JSONException {
        HttpClient client = new DefaultHttpClient(params);
        HttpGet get = new HttpGet(UIDS_URI);
        HttpResponse response = client.execute(get);
        if (response.getStatusLine().getStatusCode() == 200) {
            String res = EntityUtils.toString(response.getEntity());
            JSONArray result = new JSONArray(res);
            long[] friends = new long[result.length()];
            long uid = Long.parseLong(myUid);
            for (int i = 0; i < result.length(); i++) {
                if (uid != result.getLong(i)) {
                    friends[i] = result.getLong(i);
                }
            }
            return friends;
        }
        throw new IOException("bad http response:" + response.getStatusLine().getReasonPhrase());
    }

    public static long[] getUidsNearMe(String myUid, double lat, double lon) throws ClientProtocolException, IOException, JSONException {
        HttpClient client = new DefaultHttpClient(params);
        HttpPost post = new HttpPost(UIDS_NEARME_URI);
        List<NameValuePair> parameters = new ArrayList<NameValuePair>();
        parameters.add(new BasicNameValuePair("latitude", Double.toString(lat)));
        parameters.add(new BasicNameValuePair("longitude", Double.toString(lon)));
        post.setEntity(new UrlEncodedFormEntity(parameters));
        HttpResponse response = client.execute(post);
        if (response.getStatusLine().getStatusCode() == 200) {
            String res = EntityUtils.toString(response.getEntity());
            JSONArray result = new JSONArray(res);
            long[] friends = new long[result.length()];
            int uid = Integer.parseInt(myUid);
            for (int i = 0; i < result.length(); i++) {
                if (uid != result.getInt(i)) {
                    friends[i] = result.getInt(i);
                }
            }
            return friends;
        }
        throw new IOException("bad http response:" + response.getStatusLine().getReasonPhrase());
    }

    public static JSONObject delete(String uid) throws ClientProtocolException, IOException, JSONException {
        HttpClient client = new DefaultHttpClient(params);
        HttpGet get = new HttpGet(DELETE_URI + "?uid=" + uid);
        HttpResponse response = client.execute(get);
        if (response.getStatusLine().getStatusCode() == 200) {
            String res = EntityUtils.toString(response.getEntity());
            return new JSONObject(res);
        }
        throw new IOException("bad http response:" + response.getStatusLine().getReasonPhrase());
    }

    public static JSONObject update(String name, String uid, double lat, double lon, boolean online) throws ClientProtocolException, IOException, JSONException {
        HttpClient client = new DefaultHttpClient(params);
        HttpPost post = new HttpPost(UPDATE_URI);
        List<NameValuePair> parameters = new ArrayList<NameValuePair>();
        parameters.add(new BasicNameValuePair("name", name));
        parameters.add(new BasicNameValuePair("uid", uid));
        parameters.add(new BasicNameValuePair("latitude", Double.toString(lat)));
        parameters.add(new BasicNameValuePair("longitude", Double.toString(lon)));
        parameters.add(new BasicNameValuePair("online", Boolean.toString(online)));
        post.setEntity(new UrlEncodedFormEntity(parameters, HTTP.UTF_8));
        HttpResponse response = client.execute(post);
        if (response.getStatusLine().getStatusCode() == 200) {
            String res = EntityUtils.toString(response.getEntity());
            return new JSONObject(res);
        }
        throw new IOException("bad http response:" + response.getStatusLine().getReasonPhrase());
    }

    private static String arrayToString(long[] a, String separator) {
        StringBuilder result = new StringBuilder();
        if (a.length > 0) {
            result.append(a[0]);
            for (int i = 1; i < a.length; i++) {
                result.append(separator);
                result.append(a[i]);
            }
        }
        return result.toString();
    }
}
