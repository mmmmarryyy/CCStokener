package net.sf.imca.androidsync.client;

import android.accounts.Account;
import android.content.Context;
import android.os.Handler;
import android.util.Log;
import net.sf.imca.androidsync.authenticator.AuthenticatorActivity;
import org.apache.http.HttpEntity;
import org.apache.http.HttpResponse;
import org.apache.http.HttpStatus;
import org.apache.http.NameValuePair;
import org.apache.http.ParseException;
import org.apache.http.auth.AuthenticationException;
import org.apache.http.client.HttpClient;
import org.apache.http.client.entity.UrlEncodedFormEntity;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.conn.params.ConnManagerParams;
import org.apache.http.impl.client.DefaultHttpClient;
import org.apache.http.message.BasicNameValuePair;
import org.apache.http.params.HttpConnectionParams;
import org.apache.http.params.HttpParams;
import org.apache.http.util.EntityUtils;
import org.json.JSONException;
import org.w3c.dom.Document;
import org.w3c.dom.NodeList;
import org.xml.sax.SAXException;
import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.UnsupportedEncodingException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.TimeZone;
import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.ParserConfigurationException;

/**
 * Provides utility methods for communicating with the server.
 */
public class NetworkUtilities {

    private static final String TAG = "NetworkUtilities";

    public static final String PARAM_USERNAME = "e";

    public static final String PARAM_PASSWORD = "p";

    public static final String PARAM_UPDATED = "timestamp";

    public static final String USER_AGENT = "AuthenticationService/1.0";

    public static final int REGISTRATION_TIMEOUT = 30 * 1000;

    public static final String BASE_URL = "http://www.moth-sailing.org";

    public static final String AUTH_URI = BASE_URL + "/imca/faces/login.jsp";

    public static final String FETCH_FRIEND_UPDATES_URI = BASE_URL + "/imca/faces/riders.xml";

    public static final String FETCH_STATUS_URI = BASE_URL + "/imca/faces/riders.xml";

    private static HttpClient mHttpClient;

    /**
     * Configures the httpClient to connect to the URL provided.
     */
    public static void maybeCreateHttpClient() {
        if (mHttpClient == null) {
            mHttpClient = new DefaultHttpClient();
            final HttpParams params = mHttpClient.getParams();
            HttpConnectionParams.setConnectionTimeout(params, REGISTRATION_TIMEOUT);
            HttpConnectionParams.setSoTimeout(params, REGISTRATION_TIMEOUT);
            ConnManagerParams.setTimeout(params, REGISTRATION_TIMEOUT);
        }
    }

    /**
     * Executes the network requests on a separate thread.
     * 
     * @param runnable The runnable instance containing network mOperations to
     *        be executed.
     */
    public static Thread performOnBackgroundThread(final Runnable runnable) {
        final Thread t = new Thread() {

            @Override
            public void run() {
                try {
                    runnable.run();
                } finally {
                }
            }
        };
        t.start();
        return t;
    }

    /**
     * Connects to the Voiper server, authenticates the provided username and
     * password.
     * 
     * @param username The user's username
     * @param password The user's password
     * @param handler The hander instance from the calling UI thread.
     * @param context The context of the calling Activity.
     * @return boolean The boolean result indicating whether the user was
     *         successfully authenticated.
     */
    public static boolean authenticate(String username, String password, Handler handler, final Context context) {
        final HttpResponse resp;
        final ArrayList<NameValuePair> params = new ArrayList<NameValuePair>();
        params.add(new BasicNameValuePair(PARAM_USERNAME, username));
        params.add(new BasicNameValuePair(PARAM_PASSWORD, password));
        HttpEntity entity = null;
        try {
            entity = new UrlEncodedFormEntity(params);
        } catch (final UnsupportedEncodingException e) {
            throw new AssertionError(e);
        }
        final HttpPost post = new HttpPost(AUTH_URI);
        post.addHeader(entity.getContentType());
        post.setEntity(entity);
        maybeCreateHttpClient();
        try {
            resp = mHttpClient.execute(post);
            if (resp.getStatusLine().getStatusCode() == HttpStatus.SC_OK) {
                if (Log.isLoggable(TAG, Log.VERBOSE)) {
                    Log.v(TAG, "Successful authentication");
                }
                sendResult(true, handler, context);
                return true;
            } else {
                if (Log.isLoggable(TAG, Log.VERBOSE)) {
                    Log.v(TAG, "Error authenticating" + resp.getStatusLine());
                }
                sendResult(false, handler, context);
                return false;
            }
        } catch (final IOException e) {
            if (Log.isLoggable(TAG, Log.VERBOSE)) {
                Log.v(TAG, "IOException when getting authtoken", e);
            }
            sendResult(false, handler, context);
            return false;
        } finally {
            if (Log.isLoggable(TAG, Log.VERBOSE)) {
                Log.v(TAG, "getAuthtoken completing");
            }
        }
    }

    /**
     * Sends the authentication response from server back to the caller main UI
     * thread through its handler.
     * 
     * @param result The boolean holding authentication result
     * @param handler The main UI thread's handler instance.
     * @param context The caller Activity's context.
     */
    private static void sendResult(final Boolean result, final Handler handler, final Context context) {
        if (handler == null || context == null) {
            return;
        }
        handler.post(new Runnable() {

            public void run() {
                ((AuthenticatorActivity) context).onAuthenticationResult(result);
            }
        });
    }

    /**
     * Attempts to authenticate the user credentials on the server.
     * 
     * @param username The user's username
     * @param password The user's password to be authenticated
     * @param handler The main UI thread's handler instance.
     * @param context The caller Activity's context
     * @return Thread The thread on which the network mOperations are executed.
     */
    public static Thread attemptAuth(final String username, final String password, final Handler handler, final Context context) {
        final Runnable runnable = new Runnable() {

            public void run() {
                authenticate(username, password, handler, context);
            }
        };
        return NetworkUtilities.performOnBackgroundThread(runnable);
    }

    /**
     * Fetches the list of friend data updates from the server
     * 
     * @param account The account being synced.
     * @param authtoken The authtoken stored in AccountManager for this account
     * @param lastUpdated The last time that sync was performed
     * @return list The list of updates received from the server.
     */
    public static List<User> fetchFriendUpdates(Account account, String authtoken, Date lastUpdated) throws JSONException, ParseException, IOException, AuthenticationException {
        final ArrayList<User> friendList = new ArrayList<User>();
        final ArrayList<NameValuePair> params = new ArrayList<NameValuePair>();
        params.add(new BasicNameValuePair(PARAM_USERNAME, account.name));
        params.add(new BasicNameValuePair(PARAM_PASSWORD, authtoken));
        if (lastUpdated != null) {
            final SimpleDateFormat formatter = new SimpleDateFormat("yyyy/MM/dd HH:mm");
            formatter.setTimeZone(TimeZone.getTimeZone("UTC"));
            params.add(new BasicNameValuePair(PARAM_UPDATED, formatter.format(lastUpdated)));
        }
        Log.i(TAG, params.toString());
        HttpEntity entity = null;
        entity = new UrlEncodedFormEntity(params);
        final HttpPost post = new HttpPost(FETCH_FRIEND_UPDATES_URI);
        post.addHeader(entity.getContentType());
        post.setEntity(entity);
        maybeCreateHttpClient();
        final HttpResponse resp = mHttpClient.execute(post);
        final String response = EntityUtils.toString(resp.getEntity());
        if (resp.getStatusLine().getStatusCode() == HttpStatus.SC_OK) {
            try {
                DocumentBuilderFactory factory = DocumentBuilderFactory.newInstance();
                DocumentBuilder builder = factory.newDocumentBuilder();
                Document document = builder.parse(new ByteArrayInputStream(response.getBytes()));
                NodeList nodeList = document.getElementsByTagName("rider");
                for (int i = 0; i < nodeList.getLength(); i++) {
                    User user = User.valueOf(nodeList.item(i));
                    if (user != null) {
                        friendList.add(user);
                    }
                }
            } catch (SAXException saxE) {
                Log.e(TAG, response, saxE);
            } catch (ParserConfigurationException e) {
                Log.e(TAG, e.getMessage(), e);
            }
        } else {
            if (resp.getStatusLine().getStatusCode() == HttpStatus.SC_UNAUTHORIZED) {
                Log.e(TAG, "Authentication exception in fetching remote contacts");
                throw new AuthenticationException();
            } else {
                Log.e(TAG, "Server error in fetching remote contacts: " + resp.getStatusLine());
                throw new IOException();
            }
        }
        return friendList;
    }

    /**
     * Fetches status messages for the user's friends from the server
     * 
     * @param account The account being synced.
     * @param authtoken The authtoken stored in the AccountManager for the
     *        account
     * @return list The list of status messages received from the server.
     */
    public static List<User.Status> fetchFriendStatuses(Account account, String authtoken) throws JSONException, ParseException, IOException, AuthenticationException {
        final ArrayList<User.Status> statusList = new ArrayList<User.Status>();
        final ArrayList<NameValuePair> params = new ArrayList<NameValuePair>();
        params.add(new BasicNameValuePair(PARAM_USERNAME, account.name));
        params.add(new BasicNameValuePair(PARAM_PASSWORD, authtoken));
        HttpEntity entity = null;
        entity = new UrlEncodedFormEntity(params);
        final HttpPost post = new HttpPost(FETCH_STATUS_URI);
        post.addHeader(entity.getContentType());
        post.setEntity(entity);
        maybeCreateHttpClient();
        final HttpResponse resp = mHttpClient.execute(post);
        if (resp.getStatusLine().getStatusCode() == HttpStatus.SC_OK) {
            for (int i = 0; i < statusList.size(); i++) {
                statusList.add(new User.Status(i, "OK"));
            }
        } else {
            if (resp.getStatusLine().getStatusCode() == HttpStatus.SC_UNAUTHORIZED) {
                Log.e(TAG, "Authentication exception in fetching friend status list");
                throw new AuthenticationException();
            } else {
                Log.e(TAG, "Server error in fetching friend status list");
                throw new IOException();
            }
        }
        return statusList;
    }
}
