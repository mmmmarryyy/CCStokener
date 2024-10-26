package com.smth.infobox;

import com.smth.infobox.utils.SecurityUtils;
import android.app.Activity;
import android.app.ProgressDialog;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.res.Configuration;
import android.os.AsyncTask;
import android.os.Bundle;
import android.text.Editable;
import android.text.TextWatcher;
import android.util.Log;
import android.view.View;
import android.view.Window;
import android.view.View.OnClickListener;
import android.widget.Button;
import android.widget.CheckBox;
import android.widget.TextView;
import android.widget.Toast;
import java.io.BufferedInputStream;
import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.net.MalformedURLException;
import java.net.URL;
import java.net.URLConnection;

public class Login extends Activity implements OnClickListener, TextWatcher {

    private Button btnLogin;

    private TextView txtAccount;

    private TextView txtPassword;

    private CheckBox chkRememberMe;

    private TextView txtVersionInfo;

    ProgressDialog pd;

    Context context;

    public static String ACCOUNT_KEY = "ACCOUNT_KEY";

    public static String PASSWORD_KEY = "PASSWORD_KEY";

    public static String REMEMBER_KEY = "REMEMBER_KEY";

    public static String URL_STRING = "url_string";

    public static String IS_URL = "is_url";

    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        requestWindowFeature(Window.FEATURE_NO_TITLE);
        setContentView(R.layout.login);
        txtAccount = (TextView) findViewById(R.id.account);
        txtPassword = (TextView) findViewById(R.id.password);
        txtVersionInfo = (TextView) findViewById(R.id.version_info);
        btnLogin = (Button) findViewById(R.id.Btn_login);
        chkRememberMe = (CheckBox) findViewById(R.id.remembered);
        chkRememberMe.setChecked(false);
        btnLogin.setOnClickListener(this);
        txtAccount.addTextChangedListener(this);
        txtPassword.addTextChangedListener(this);
        txtVersionInfo.setText("version 1.0");
        context = this;
        loadActivityPreferences();
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
    }

    @Override
    public void onConfigurationChanged(Configuration newConfig) {
        super.onConfigurationChanged(newConfig);
    }

    private class LoginTask extends AsyncTask<URL, Integer, Boolean> {

        private ProgressDialog waitingDialog;

        @Override
        protected Boolean doInBackground(URL... arg0) {
            URLConnection urlConnection;
            try {
                urlConnection = arg0[0].openConnection();
                BufferedInputStream in = new BufferedInputStream(urlConnection.getInputStream());
                BufferedReader br = new BufferedReader(new InputStreamReader(in));
                String result = br.readLine();
                return result.equals("1");
            } catch (IOException e) {
                e.printStackTrace();
            }
            return Boolean.FALSE;
        }

        @Override
        protected void onPreExecute() {
            waitingDialog = new ProgressDialog(Login.this);
            waitingDialog.setCancelable(false);
            waitingDialog.setTitle(getString(R.string.app_name));
            waitingDialog.setMessage(getString(R.string.login_now));
            waitingDialog.show();
        }

        @Override
        protected void onPostExecute(Boolean result) {
            waitingDialog.dismiss();
            if (!result) {
                Toast.makeText(Login.this, R.string.login_failed, Toast.LENGTH_LONG).show();
            } else {
                finish();
                String URL = "http://wap.infobox.cn/index_activities.php?";
                URL += "user_name=" + Session.getInstance().getUserName() + "&user_password=" + SecurityUtils.toMD5(Session.getInstance().getPassword().getBytes());
                Intent intent = new Intent(Login.this, Infobox.class);
                intent.putExtra(IS_URL, true);
                intent.putExtra(URL_STRING, URL);
                startActivity(intent);
            }
        }
    }

    @Override
    public void onClick(View v) {
        SharedPreferences activityPreferences = getPreferences(Activity.MODE_PRIVATE);
        saveActivityPreferences();
        try {
            URL loginURL = new URL("http://wap.infobox.cn/login.php?user_name=" + activityPreferences.getString(ACCOUNT_KEY, "") + "&user_password=" + SecurityUtils.toMD5(activityPreferences.getString(PASSWORD_KEY, "").getBytes()));
            Log.d("Infobox", loginURL.toString());
            new LoginTask().execute(loginURL);
        } catch (MalformedURLException e) {
        }
    }

    @Override
    public void afterTextChanged(Editable s) {
    }

    @Override
    public void beforeTextChanged(CharSequence s, int start, int count, int after) {
    }

    @Override
    public void onTextChanged(CharSequence s, int start, int before, int count) {
        if (txtAccount.length() > 0 && txtPassword.length() > 0) {
            btnLogin.setEnabled(true);
        } else {
            btnLogin.setEnabled(false);
        }
    }

    private void saveActivityPreferences() {
        SharedPreferences activityPreferences = getPreferences(Activity.MODE_PRIVATE);
        SharedPreferences.Editor editor = activityPreferences.edit();
        Session.getInstance().setUserName(txtAccount.getText().toString());
        Session.getInstance().setPassword(txtPassword.getText().toString());
        if (chkRememberMe.isChecked()) {
            editor.putString(ACCOUNT_KEY, txtAccount.getText().toString());
            editor.putString(PASSWORD_KEY, txtPassword.getText().toString());
            editor.putBoolean(REMEMBER_KEY, true);
        } else {
            editor.clear();
        }
        editor.commit();
    }

    private void loadActivityPreferences() {
        SharedPreferences activityPreferences = getPreferences(Activity.MODE_PRIVATE);
        chkRememberMe.setChecked(activityPreferences.getBoolean(REMEMBER_KEY, false));
        txtAccount.setText(activityPreferences.getString(ACCOUNT_KEY, ""));
        txtPassword.setText(activityPreferences.getString(PASSWORD_KEY, ""));
    }
}
