package com.squareshoot.picCheckin;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.util.ArrayList;
import java.util.List;
import org.apache.http.HttpResponse;
import org.apache.http.NameValuePair;
import org.apache.http.client.HttpClient;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.cookie.Cookie;
import org.apache.http.impl.client.DefaultHttpClient;
import org.apache.http.impl.cookie.BasicClientCookie;
import org.apache.http.message.BasicNameValuePair;
import org.apache.http.params.CoreProtocolPNames;
import org.json.JSONException;
import org.json.JSONObject;
import android.app.Activity;
import android.content.Intent;
import android.content.SharedPreferences;
import android.database.Cursor;
import android.net.Uri;
import android.os.AsyncTask;
import android.os.Bundle;
import android.os.Message;
import android.preference.PreferenceManager;
import android.util.Log;
import android.view.Gravity;
import android.widget.Toast;

public class Main extends Activity {

    private static final int FIRSTSTART = 10;

    DefaultHttpClient httpclient = new DefaultHttpClient();

    String login;

    String password;

    String userProfile;

    private ProfileDb mDbHelper;

    Uri photo;

    boolean photoSelected = false;

    private AuthenticationTask authTask;

    /** Called when the activity is first created. */
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        if (Common.HIGHDEBUG) Log.d(Common.TAG, "Main : onCreate");
        if (Common.DEBUG) {
            Log.v(Common.TAG, Common.getVersion(this));
            Log.v(Common.TAG, Common.getSystemInfo(this));
        }
        setContentView(R.layout.main);
        mDbHelper = new ProfileDb(Main.this);
        if (savedInstanceState == null) {
            Intent sendIntent = this.getIntent();
            if (sendIntent.getAction().compareTo(Intent.ACTION_SEND) == 0) {
                Bundle sendBundle = sendIntent.getExtras();
                photo = (Uri) sendBundle.get(Intent.EXTRA_STREAM);
                photoSelected = true;
                if (Common.HIGHDEBUG) Log.d(Common.TAG, "Data : " + sendBundle.get(Intent.EXTRA_STREAM));
            }
            httpclient.getParams().setBooleanParameter(CoreProtocolPNames.USE_EXPECT_CONTINUE, false);
            newAuth();
        } else {
            if (getLastNonConfigurationInstance() != null) {
                authTask = (AuthenticationTask) getLastNonConfigurationInstance();
                authTask.setActivity(this);
            } else newAuth();
        }
        if (Common.HIGHDEBUG) Log.d(Common.TAG, "Main : onCreate done.");
    }

    public Object onRetainNonConfigurationInstance() {
        if (authTask != null) {
            authTask.setActivity(null);
            return authTask;
        }
        return null;
    }

    private void newAuth() {
        if (getLoginAndPass()) {
            mDbHelper.open();
            Cursor c = mDbHelper.recupCookie(1);
            mDbHelper.close();
            if (Common.HIGHDEBUG) Log.e(Common.TAG, "########### COUNT :: " + String.valueOf(c.getCount()));
            if (c.getCount() > 0 && c.getString(c.getColumnIndexOrThrow(ProfileDb.KEY_COOKIE)) != null) {
                Cookie cookie = isAlreadyConnected(c.getString(c.getColumnIndexOrThrow(ProfileDb.KEY_COOKIE)));
                if (Common.HIGHDEBUG) Log.e(Common.TAG, "Le cookie existe : " + cookie);
                httpclient.getCookieStore().addCookie(cookie);
            } else {
                if (Common.HIGHDEBUG) Log.i(Common.TAG, "Pas de cookie");
            }
            c.close();
            authenticate();
        } else {
            startFirstStart();
        }
    }

    private boolean getLoginAndPass() {
        mDbHelper.open();
        Cursor c = mDbHelper.fetchProfile(1);
        mDbHelper.close();
        int count = c.getCount();
        if (count > 0) {
            login = c.getString(c.getColumnIndexOrThrow(ProfileDb.KEY_EMAIL));
            password = c.getString(c.getColumnIndexOrThrow(ProfileDb.KEY_PWD));
            c.close();
            return true;
        } else {
            c.close();
            if (Common.HIGHDEBUG) Log.e(Common.TAG, "Les login/pass ne sont pas renseignés");
            return false;
        }
    }

    private void authenticate() {
        authTask = new AuthenticationTask();
        authTask.setActivity(this);
        authTask.execute(login, password);
    }

    private void startFirstStart() {
        Intent i = new Intent(Main.this, FirstStart.class);
        startActivityForResult(i, FIRSTSTART);
    }

    private void startHome() {
        SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(this);
        SharedPreferences.Editor editor = prefs.edit();
        editor.putBoolean("isAuthent", true);
        editor.commit();
        if (Common.HIGHDEBUG) Log.i(Common.TAG, "Login OK, starting Intent Home");
        Intent i = new Intent(Main.this, Home.class);
        Bundle bundle = new Bundle();
        bundle.putString("user", userProfile);
        bundle.putString("username", login);
        bundle.putString("password", password);
        if (photoSelected) bundle.putParcelable("photo", photo);
        i.putExtras(bundle);
        startActivity(i);
        Log.d(Common.TAG, "Finishing Main...");
        finish();
    }

    protected String getCookieText(Cookie cookie) {
        return cookie.getVersion() + "::" + cookie.getName() + "::" + cookie.getValue() + "::" + cookie.getDomain() + "::" + cookie.getPath();
    }

    @Override
    public void onResume() {
        super.onResume();
        if (Common.HIGHDEBUG) Log.d(Common.TAG, "Main : onResume");
        Uri uri = this.getIntent().getData();
        Log.d(Common.TAG, "uri=" + uri);
        if (uri != null) {
            String code = null;
            if ((code = uri.getQueryParameter("code")) != null) {
                try {
                    Log.d(Common.TAG, "code=" + code);
                    JSONObject tokenJson = executeHttpGet("https://foursquare.com/oauth2/access_token" + "?client_id=" + Common.CLIENT_ID + "&client_secret=" + Common.CLIENT_SECRET + "&grant_type=authorization_code" + "&redirect_uri=sqshoot-android://" + "&code=" + code);
                    String token = tokenJson.getString("access_token");
                    Log.d(Common.TAG, "token=" + token);
                    JSONObject userJson = executeHttpGet("https://api.foursquare.com/v2/" + "users/self" + "?oauth_token=" + token);
                    int returnCode = Integer.parseInt(userJson.getJSONObject("meta").getString("code"));
                    if (returnCode == 200) {
                        Log.i("LoginTest", userJson.getJSONObject("response").getJSONObject("user").toString());
                    } else {
                        Toast.makeText(this, "Wrong return code: " + code, Toast.LENGTH_SHORT).show();
                    }
                } catch (Exception exp) {
                    Log.e("LoginTest", "Login to Foursquare failed", exp);
                }
            } else if ((code = uri.getQueryParameter("error")) != null) {
                Toast.makeText(this, code, Toast.LENGTH_SHORT).show();
            } else {
                Toast.makeText(this, "Unknown login error", Toast.LENGTH_SHORT).show();
            }
        }
    }

    private JSONObject executeHttpGet(String uri) throws Exception {
        HttpGet req = new HttpGet(uri);
        HttpClient client = new DefaultHttpClient();
        HttpResponse resLogin = client.execute(req);
        BufferedReader r = new BufferedReader(new InputStreamReader(resLogin.getEntity().getContent()));
        StringBuilder sb = new StringBuilder();
        String s = null;
        while ((s = r.readLine()) != null) {
            sb.append(s);
        }
        return new JSONObject(sb.toString());
    }

    @Override
    protected void onSaveInstanceState(Bundle outState) {
        super.onSaveInstanceState(outState);
        if (Common.HIGHDEBUG) Log.i(Common.TAG, "Main : onSaveInstantState");
    }

    protected void onActivityResult(int requestCode, int resultCode, Intent intent) {
        super.onActivityResult(requestCode, resultCode, intent);
        Bundle extras = null;
        if (intent != null) {
            extras = intent.getExtras();
        }
        if (Common.HIGHDEBUG) Log.d(Common.TAG, "Main : onActivityResult");
        switch(requestCode) {
            case FIRSTSTART:
                if (extras == null) finish();
                if (extras.containsKey("quoi")) {
                    if (Common.HIGHDEBUG) Log.i(Common.TAG, "on quitte depuis firststart");
                    finish();
                } else {
                    if (Common.HIGHDEBUG) Log.d(Common.TAG, "Main : retour de firststart");
                    login = extras.getString("login");
                    password = extras.getString("password");
                    authenticate();
                }
                break;
        }
    }

    private Cookie isAlreadyConnected(String cookieTxt) {
        String[] temp = cookieTxt.split("::");
        BasicClientCookie cookie = new BasicClientCookie(temp[1], "value");
        cookie.setVersion(Integer.parseInt(temp[0]));
        cookie.setValue(temp[2]);
        cookie.setDomain(temp[3]);
        cookie.setPath(temp[4]);
        return cookie;
    }

    private class AuthenticationTask extends AsyncTask<String, String, Message> {

        private Message msg;

        private Main activity;

        private boolean completed = false;

        protected Message doInBackground(String... parameters) {
            Bundle data = new Bundle();
            msg = Message.obtain();
            List<NameValuePair> params = new ArrayList<NameValuePair>();
            params.add(new BasicNameValuePair("user", parameters[0]));
            params.add(new BasicNameValuePair("password", parameters[1]));
            try {
                String reponse = Common.postUrlData(httpclient, Common.getBaseUrl() + "/login", params);
                data.putString("reponse", reponse);
            } catch (IOException e) {
                data.putString("eMessage", e.getMessage());
            }
            msg.setData(data);
            return msg;
        }

        protected void onPostExecute(Message msg) {
            completed = true;
            if (activity != null) endTask();
        }

        private void setActivity(Main activity) {
            this.activity = activity;
            if (completed) {
                endTask();
            }
        }

        private void endTask() {
            activity.authTask = null;
            verifyAuth();
        }

        private void verifyAuth() {
            Bundle data = msg.getData();
            if (data.containsKey("reponse")) userProfile = data.getString("reponse");
            if (Common.HIGHDEBUG) Log.i(Common.TAG, "alors alors : " + userProfile + "##");
            if (!data.containsKey("eMessage")) {
                JSONObject jUserProfile = null;
                try {
                    jUserProfile = new JSONObject(userProfile);
                    if (jUserProfile.has("picuserid")) {
                        Cookie cookie = httpclient.getCookieStore().getCookies().get(0);
                        if (Common.HIGHDEBUG) Log.d(Common.TAG, "cookie save : " + cookie);
                        mDbHelper.open();
                        mDbHelper.updateCookie(1, getCookieText(cookie));
                        mDbHelper.updateUserProfile(1, userProfile);
                        mDbHelper.close();
                        startHome();
                    } else {
                        String error = null;
                        boolean startFirstStart = false;
                        if (jUserProfile.has("error")) {
                            error = jUserProfile.getString("error");
                            startFirstStart = true;
                        } else error = "Unknown error";
                        Toast erreurToast = Toast.makeText(Main.this, getString(R.string.loginFailed) + " : " + error, Toast.LENGTH_LONG);
                        erreurToast.setGravity(Gravity.CENTER, 0, 0);
                        erreurToast.show();
                        Log.e(Common.TAG, "Login Failed : " + error);
                        if (startFirstStart) startFirstStart();
                    }
                } catch (JSONException e) {
                    Toast erreurToast = Toast.makeText(Main.this, R.string.loginFailed, Toast.LENGTH_LONG);
                    erreurToast.setGravity(Gravity.CENTER, 0, 0);
                    erreurToast.show();
                    Log.e(Common.TAG, "JSONException : " + e.getMessage());
                    finish();
                }
            } else {
                Toast erreurToast = Toast.makeText(Main.this, data.getString("eMessage"), Toast.LENGTH_LONG);
                erreurToast.setGravity(Gravity.CENTER, 0, 0);
                erreurToast.show();
                Log.e(Common.TAG, "IOException: " + data.getString("eMessage"));
                finish();
            }
        }
    }
}
