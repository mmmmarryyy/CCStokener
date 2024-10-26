package org.spartanrobotics;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.UnsupportedEncodingException;
import java.util.Arrays;
import java.util.Iterator;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.logging.Level;
import java.util.logging.Logger;
import org.apache.http.Header;
import org.apache.http.HttpEntity;
import org.apache.http.HttpResponse;
import org.apache.http.client.ClientProtocolException;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.client.methods.HttpUriRequest;
import org.apache.http.conn.scheme.PlainSocketFactory;
import org.apache.http.conn.scheme.Scheme;
import org.apache.http.conn.scheme.SchemeRegistry;
import org.apache.http.conn.ssl.SSLSocketFactory;
import org.apache.http.entity.StringEntity;
import org.apache.http.impl.client.DefaultHttpClient;
import org.apache.http.impl.client.DefaultRedirectHandler;
import org.apache.http.impl.conn.tsccm.ThreadSafeClientConnManager;
import org.apache.http.params.BasicHttpParams;
import org.apache.http.params.HttpParams;
import org.apache.http.protocol.HttpContext;
import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;
import android.accounts.Account;
import android.accounts.AccountManager;
import android.accounts.AuthenticatorException;
import android.accounts.OperationCanceledException;
import android.app.Activity;
import android.content.SharedPreferences;
import android.content.SharedPreferences.Editor;
import android.content.res.AssetManager;
import android.content.res.Resources;
import android.webkit.WebView;

/**
 * A class that can be bound to the javascript name "bridge" and used by the javascript to load views etc.
 * @author User
 */
public class JSBridge {

    private interface GetPageCallback {

        public void success(String result);

        public void error();
    }

    private static class Runner extends Thread {

        private final BlockingQueue<Runnable> queue = new LinkedBlockingQueue<Runnable>();

        @Override
        public void run() {
            while (true) {
                try {
                    queue.take().run();
                } catch (InterruptedException e) {
                }
            }
        }

        public void add(Runnable runnable) {
            queue.add(runnable);
        }
    }

    private static final Runner runner = new Runner();

    static {
        runner.start();
    }

    private final AssetManager assets;

    private final JSONObject gameMap;

    private final AccountManager accountsManager;

    private final Resources resources;

    private JSONObject data;

    private final SharedPreferences preferences;

    private final Activity act;

    private final WebView browser;

    private static final DefaultHttpClient httpclient;

    static {
        HttpParams params = new BasicHttpParams();
        SchemeRegistry registry = new SchemeRegistry();
        registry.register(new Scheme("http", PlainSocketFactory.getSocketFactory(), 80));
        registry.register(new Scheme("https", SSLSocketFactory.getSocketFactory(), 443));
        ThreadSafeClientConnManager manager = new ThreadSafeClientConnManager(params, registry);
        httpclient = new DefaultHttpClient(manager, params);
        httpclient.setRedirectHandler(new DefaultRedirectHandler() {

            @Override
            public boolean isRedirectRequested(HttpResponse response, HttpContext context) {
                for (Header c : response.getHeaders("Set-Cookie")) {
                    if (c.getValue().startsWith("ACSID=")) {
                        return false;
                    }
                }
                return super.isRedirectRequested(response, context);
            }
        });
    }

    public JSBridge(AssetManager assets, Activity act, WebView browser) {
        this.assets = assets;
        JSONObject temp = null;
        try {
            temp = new JSONObject(Scouter.readAsset("generated/comp_map.json", assets));
        } catch (JSONException e) {
            Logger.getLogger(JSBridge.class.getName()).log(Level.SEVERE, "Could not get comp map.", e);
        }
        gameMap = temp;
        accountsManager = AccountManager.get(act);
        resources = act.getResources();
        preferences = act.getPreferences(0);
        this.browser = browser;
        this.act = act;
    }

    public String getView(String view, String comp) {
        Logger.getLogger(JSBridge.class.getName()).log(Level.INFO, "Getting view '" + view + "'.");
        try {
            return Scouter.readAsset("generated/views/" + view + ((comp.equals("undefined") || comp.equals("null") || view.equals("mine")) ? "" : "-" + gameMap.getString(comp)) + ".html", assets);
        } catch (JSONException e) {
            Logger.getLogger(JSBridge.class.getName()).log(Level.WARNING, "Could not load JSON.", e);
            return null;
        }
    }

    public void save(String dataToSend) {
        getPage("/xhr_data_update", dataToSend, new GetPageCallback() {

            @Override
            public void success(String result) {
                browser.loadUrl("javascript:save_succ('" + result + "');false");
            }

            @Override
            public void error() {
                browser.loadUrl("javascript:save_err();false");
            }
        });
    }

    private void doGetData(String view, String comp, String key) {
        if (data != null) {
            Editor editor = preferences.edit();
            editor.putString("data", data.toString());
            editor.commit();
            try {
                JSONArray raw = new JSONArray();
                JSONObject extras = new JSONObject();
                if (view.equals("index")) {
                    JSONArray r = new JSONArray();
                    @SuppressWarnings("unchecked") Iterator<String> i = data.keys();
                    String c;
                    while (i.hasNext()) {
                        c = i.next();
                        r.put(new JSONArray(Arrays.asList(c, data.getJSONArray(c).getString(0))));
                    }
                    raw.put(r);
                    extras.put("title", "Competitions");
                } else if (view.equals("mine")) {
                    JSONArray r = new JSONArray(), wrapper = new JSONArray();
                    wrapper.put(r);
                    wrapper.put(resources.getInteger(R.integer.page_size));
                    @SuppressWarnings("unchecked") Iterator<String> i = data.getJSONArray(comp).getJSONObject(1).keys();
                    String c;
                    while (i.hasNext()) {
                        c = i.next();
                        r.put(new JSONArray(Arrays.asList(c, data.getJSONArray(comp).getJSONObject(1).getJSONObject(c).getString("match"), data.getJSONArray(comp).getJSONObject(1).getJSONObject(c).getString("team"))));
                    }
                    raw.put(wrapper);
                } else if (view.equals("auto") || view.equals("tele") || view.equals("overview")) {
                    raw.put(data.getJSONArray(comp).getJSONObject(1).getJSONObject(key));
                } else {
                    Logger.getLogger(JSBridge.class.getName()).log(Level.WARNING, "Unknown view to get data for \"" + view + "\".");
                    getDataError();
                }
                raw.put(extras);
                browser.loadUrl("javascript:data_succ('" + raw.toString() + "');false");
            } catch (JSONException e) {
                Logger.getLogger(JSBridge.class.getName()).log(Level.WARNING, "JSON issues.", e);
                getDataError();
            }
        } else {
            Logger.getLogger(JSBridge.class.getName()).log(Level.WARNING, "Could not find data for \"" + view + "\".");
            getDataError();
        }
    }

    private void getDataError() {
        browser.loadUrl("javascript:data_err();false");
    }

    public void getData(final String view, final String comp, final String key) {
        getPage("/getAndroidData", null, new GetPageCallback() {

            @Override
            public void success(String result) {
                try {
                    data = new JSONObject(result);
                    doGetData(view, comp, key);
                } catch (JSONException e) {
                    Logger.getLogger(JSBridge.class.getName()).log(Level.WARNING, "Could not parse server's JSON.", e);
                    Logger.getLogger(JSBridge.class.getName()).log(Level.INFO, "JSON was \"" + result + "\".");
                    getDataError();
                }
            }

            @Override
            public void error() {
                if (preferences.contains("data")) {
                    try {
                        data = new JSONObject(preferences.getString("data", null));
                        doGetData(view, comp, key);
                    } catch (JSONException e) {
                        Logger.getLogger(JSBridge.class.getName()).log(Level.SEVERE, "Could not parse JSON in preferences. Removing it.", e);
                        getDataError();
                        Editor editor = preferences.edit();
                        editor.remove("data");
                        editor.commit();
                    }
                } else {
                    try {
                        data = new JSONObject(Scouter.readAsset("generated/dataDefault", assets));
                        doGetData(view, comp, key);
                    } catch (JSONException e) {
                        Logger.getLogger(JSBridge.class.getName()).log(Level.SEVERE, "Could not parse default JSON in assets.", e);
                        getDataError();
                    }
                }
            }
        });
    }

    private void getPage(final String path, final String dataToSend, final GetPageCallback callback) {
        runner.add(new Runnable() {

            @Override
            public void run() {
                String url = "http://" + resources.getString(R.string.host) + path;
                HttpUriRequest req;
                if (dataToSend == null) {
                    req = new HttpGet(url);
                } else {
                    req = new HttpPost(url);
                    try {
                        ((HttpPost) req).setEntity(new StringEntity(dataToSend));
                    } catch (UnsupportedEncodingException e) {
                        Logger.getLogger(JSBridge.class.getName()).log(Level.SEVERE, "Unsupported encoding.", e);
                    }
                }
                req.addHeader("Cookie", getAuthCookie(false));
                try {
                    HttpResponse response = httpclient.execute(req);
                    Logger.getLogger(JSBridge.class.getName()).log(Level.INFO, "Response status is '" + response.getStatusLine() + "'.");
                    HttpEntity entity = response.getEntity();
                    if (entity != null) {
                        InputStream instream = entity.getContent();
                        try {
                            BufferedReader in = new BufferedReader(new InputStreamReader(instream));
                            StringBuilder b = new StringBuilder();
                            String line;
                            boolean first = true;
                            while ((line = in.readLine()) != null) {
                                b.append(line);
                                if (first) {
                                    first = false;
                                } else {
                                    b.append("\r\n");
                                }
                            }
                            in.close();
                            callback.success(b.toString());
                            return;
                        } catch (RuntimeException ex) {
                            throw ex;
                        } finally {
                            instream.close();
                        }
                    }
                } catch (ClientProtocolException e) {
                    Logger.getLogger(JSBridge.class.getName()).log(Level.SEVERE, "HTTP protocol violated.", e);
                } catch (IOException e) {
                    Logger.getLogger(JSBridge.class.getName()).log(Level.WARNING, "Could not load '" + path + "'.", e);
                }
                Logger.getLogger(JSBridge.class.getName()).log(Level.INFO, "Calling error from JSBridge.getPage because of previous errors.");
                callback.error();
            }
        });
    }

    private String getAuthCookie(boolean invalidate) {
        if (resources.getBoolean(R.bool.dev)) {
            return "dev_appserver_login=get_view@localhost.devel:false:18580476422013912411";
        } else {
            try {
                Account[] accounts = accountsManager.getAccountsByType("com.google");
                Account account = null;
                while (!(accounts.length > 0)) {
                    accountsManager.addAccount("com.google", "ah", null, null, act, null, null).getResult();
                    accounts = accountsManager.getAccountsByType("com.google");
                }
                if (account == null) {
                    account = accounts[0];
                }
                String authToken = accountsManager.getAuthToken(account, "ah", null, act, null, null).getResult().get(AccountManager.KEY_AUTHTOKEN).toString();
                if (invalidate || authToken == null) {
                    Logger.getLogger(JSBridge.class.getName()).log(Level.INFO, "Invalidating auth token.");
                    accountsManager.invalidateAuthToken("com.google", authToken);
                    return getAuthCookie(false);
                }
                HttpGet httpget = new HttpGet("http://" + resources.getString(R.string.host) + "/_ah/login?auth=" + authToken);
                HttpResponse response = httpclient.execute(httpget);
                for (Header c : response.getHeaders("Set-Cookie")) {
                    if (c.getValue().startsWith("ACSID=")) {
                        return c.getValue();
                    }
                }
                return getAuthCookie(false);
            } catch (ClientProtocolException e) {
                Logger.getLogger(JSBridge.class.getName()).log(Level.SEVERE, "HTTP protocol violated.", e);
            } catch (OperationCanceledException e) {
                Logger.getLogger(JSBridge.class.getName()).log(Level.WARNING, "Login canceled.", e);
            } catch (AuthenticatorException e) {
                Logger.getLogger(JSBridge.class.getName()).log(Level.WARNING, "Authentication failed.", e);
            } catch (IOException e) {
                Logger.getLogger(JSBridge.class.getName()).log(Level.SEVERE, "Login failed.", e);
            }
            return getAuthCookie(true);
        }
    }

    public String getEmail() {
        return "jsbridge@notreal.x";
    }

    public void signout() {
        Logger.getLogger(JSBridge.class.getName()).log(Level.INFO, "Should sign out.");
    }
}
