package com.facebook.android;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.MalformedURLException;
import java.net.URL;
import java.util.ArrayList;
import java.util.Arrays;
import org.apache.http.HttpEntity;
import org.apache.http.HttpResponse;
import org.apache.http.NameValuePair;
import org.apache.http.client.HttpClient;
import org.apache.http.client.entity.UrlEncodedFormEntity;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.impl.client.DefaultHttpClient;
import org.apache.http.message.BasicNameValuePair;
import org.json.JSONException;
import org.json.JSONObject;
import android.app.Activity;
import android.app.ProgressDialog;
import android.content.Intent;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.view.View.OnClickListener;
import android.widget.Button;
import android.widget.TextView;
import com.facebook.android.SessionEvents.AuthListener;
import com.facebook.android.SessionEvents.LogoutListener;

public class PennQuiz extends Activity {

    public static final String APP_ID = "200186583340324";

    private LoginButton mLoginButton;

    private TextView mText;

    private Button mChange;

    private Button mScoreboard;

    private Button mSinglePlay;

    private Button mNotifications;

    private Facebook facebook;

    private AsyncFacebookRunner mAsyncRunner;

    private static String me;

    private static String myID;

    /** Called when the activity is first created. */
    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        if (APP_ID == null) {
            Util.showAlert(this, "Warning", "Facebook Applicaton ID must be " + "specified before running this example: see Example.java");
        }
        setContentView(R.layout.main);
        mLoginButton = (LoginButton) findViewById(R.id.login);
        mText = (TextView) PennQuiz.this.findViewById(R.id.txt);
        mChange = (Button) findViewById(R.id.changeButton);
        mScoreboard = (Button) findViewById(R.id.scoreBoard);
        mSinglePlay = (Button) findViewById(R.id.singlePlay);
        mNotifications = (Button) findViewById(R.id.notifications);
        facebook = new Facebook(APP_ID);
        mAsyncRunner = new AsyncFacebookRunner(facebook);
        SessionStore.restore(facebook, this);
        SessionEvents.addAuthListener(new SampleAuthListener());
        SessionEvents.addLogoutListener(new SampleLogoutListener());
        getData(facebook);
        mLoginButton.init(this, facebook, new String[] { "publish_stream", "offline_access" });
        mNotifications.setVisibility(facebook.isSessionValid() ? View.VISIBLE : View.INVISIBLE);
        mNotifications.setOnClickListener(new OnClickListener() {

            @Override
            public void onClick(View v) {
                final ProgressDialog progress = ProgressDialog.show(PennQuiz.this, "", "Retrieving notifications...", true);
                String myFriends = getFriendsJSONString(facebook);
                final Intent intent = new Intent(v.getContext(), Notifications.class);
                intent.putExtra("api", APP_ID);
                intent.putExtra("friends", myFriends);
                intent.putExtra("myID", myID);
                new Thread(new Runnable() {

                    public void run() {
                        startActivity(intent);
                        progress.dismiss();
                    }
                }).start();
            }
        });
        mSinglePlay.setVisibility(facebook.isSessionValid() ? View.VISIBLE : View.INVISIBLE);
        mSinglePlay.setOnClickListener(new OnClickListener() {

            @Override
            public void onClick(View v) {
                final ProgressDialog progress = ProgressDialog.show(PennQuiz.this, "", "Loading...", true);
                String[] questions = getQuestions();
                final Intent intent = new Intent(v.getContext(), SinglePlay.class);
                intent.putExtra("api", APP_ID);
                intent.putExtra("questions", questions);
                intent.putExtra("myID", myID);
                intent.putExtra("score", 0);
                new Thread(new Runnable() {

                    public void run() {
                        startActivity(intent);
                        progress.dismiss();
                    }
                }).start();
            }
        });
        mChange.setVisibility(facebook.isSessionValid() ? View.VISIBLE : View.INVISIBLE);
        mChange.setOnClickListener(new OnClickListener() {

            @Override
            public void onClick(View v) {
                final ProgressDialog progress = ProgressDialog.show(PennQuiz.this, "", "Retrieving friends...", true);
                String myFriends = getFriendsJSONString(facebook);
                final Intent intent = new Intent(v.getContext(), Challenge.class);
                intent.putExtra("api", APP_ID);
                intent.putExtra("friends", myFriends);
                intent.putExtra("myID", myID);
                new Thread(new Runnable() {

                    public void run() {
                        startActivity(intent);
                        progress.dismiss();
                    }
                }).start();
            }
        });
        mScoreboard.setVisibility(facebook.isSessionValid() ? View.VISIBLE : View.INVISIBLE);
        mScoreboard.setOnClickListener(new OnClickListener() {

            @Override
            public void onClick(View v) {
                final ProgressDialog progress = ProgressDialog.show(PennQuiz.this, "", "Getting scores...", true);
                String myFriends = getFriendsJSONString(facebook);
                final Intent intent = new Intent(v.getContext(), Scoreboard.class);
                intent.putExtra("api", APP_ID);
                intent.putExtra("friends", myFriends);
                intent.putExtra("myID", myID);
                new Thread(new Runnable() {

                    public void run() {
                        startActivity(intent);
                        progress.dismiss();
                    }
                }).start();
            }
        });
    }

    @Override
    public void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        facebook.authorizeCallback(requestCode, resultCode, data);
    }

    public class SampleAuthListener implements AuthListener {

        public void onAuthSucceed() {
            mText.setText("You have logged in! ");
            mNotifications.setVisibility(View.VISIBLE);
            mSinglePlay.setVisibility(View.VISIBLE);
            mChange.setVisibility(View.VISIBLE);
            mScoreboard.setVisibility(View.VISIBLE);
            getData(facebook);
            String domain = "http://www.pennquiz.com/users.php";
            ArrayList<NameValuePair> nameValuePairs = new ArrayList<NameValuePair>();
            nameValuePairs.add(new BasicNameValuePair("uid", myID));
            nameValuePairs.add(new BasicNameValuePair("funct", "addNewUser"));
            RequestPoster rp = new RequestPoster(domain, nameValuePairs);
            rp.execute();
        }

        public void onAuthFail(String error) {
            mText.setText("Login Failed: " + error);
        }
    }

    public class SampleLogoutListener implements LogoutListener {

        public void onLogoutBegin() {
            mText.setText("Logging out...");
        }

        public void onLogoutFinish() {
            mText.setText("You have logged out! ");
            mNotifications.setVisibility(View.INVISIBLE);
            mSinglePlay.setVisibility(View.INVISIBLE);
            mChange.setVisibility(View.INVISIBLE);
            mScoreboard.setVisibility(View.INVISIBLE);
        }
    }

    public class SampleRequestListener extends BaseRequestListener {

        public void onComplete(final String response, final Object state) {
            try {
                Log.d("Facebook-Example", "Response: " + response.toString());
                JSONObject json = Util.parseJson(response);
                final String name = json.getString("name");
                PennQuiz.this.runOnUiThread(new Runnable() {

                    public void run() {
                        mText.setText("Hello there, " + name + "!");
                    }
                });
            } catch (JSONException e) {
                Log.w("Facebook-Example", "JSON Error in response");
            } catch (FacebookError e) {
                Log.w("Facebook-Example", "Facebook Error: " + e.getMessage());
            }
        }
    }

    public class SampleUploadListener extends BaseRequestListener {

        public void onComplete(final String response, final Object state) {
            try {
                Log.d("Facebook-Example", "Response: " + response.toString());
                JSONObject json = Util.parseJson(response);
                final String src = json.getString("src");
                PennQuiz.this.runOnUiThread(new Runnable() {

                    public void run() {
                        mText.setText("Hello there, photo has been uploaded at \n" + src);
                    }
                });
            } catch (JSONException e) {
                Log.w("Facebook-Example", "JSON Error in response");
            } catch (FacebookError e) {
                Log.w("Facebook-Example", "Facebook Error: " + e.getMessage());
            }
        }
    }

    public class WallPostRequestListener extends BaseRequestListener {

        public void onComplete(final String response, final Object state) {
            Log.d("Facebook-Example", "Got response: " + response);
            String message = "<empty>";
            try {
                JSONObject json = Util.parseJson(response);
                message = json.getString("message");
            } catch (JSONException e) {
                Log.w("Facebook-Example", "JSON Error in response");
            } catch (FacebookError e) {
                Log.w("Facebook-Example", "Facebook Error: " + e.getMessage());
            }
            final String text = "Your Wall Post: " + message;
            PennQuiz.this.runOnUiThread(new Runnable() {

                public void run() {
                    mText.setText(text);
                }
            });
        }
    }

    public class WallPostDeleteListener extends BaseRequestListener {

        public void onComplete(final String response, final Object state) {
            if (response.equals("true")) {
                Log.d("Facebook-Example", "Successfully deleted wall post");
                PennQuiz.this.runOnUiThread(new Runnable() {

                    public void run() {
                        mText.setText("Deleted Wall Post");
                    }
                });
            } else {
                Log.d("Facebook-Example", "Could not delete wall post");
            }
        }
    }

    public class SampleDialogListener extends BaseDialogListener {

        public void onComplete(Bundle values) {
            final String postId = values.getString("post_id");
            if (postId != null) {
                Log.d("Facebook-Example", "Dialog Success! post_id=" + postId);
                mAsyncRunner.request(postId, new WallPostRequestListener());
            } else {
                Log.d("Facebook-Example", "No wall post made");
            }
        }
    }

    private static String getFriendsJSONString(Facebook facebook) {
        String myFriends = "You haven't allowed permissions";
        try {
            myFriends = facebook.request("me/friends");
        } catch (MalformedURLException e) {
            e.printStackTrace();
        } catch (IOException e) {
            e.printStackTrace();
        }
        return myFriends;
    }

    private static void getData(Facebook facebook) {
        try {
            me = facebook.request("me");
            JSONObject json = new JSONObject(me);
            myID = json.getString("id");
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    private static String[] getQuestions() {
        InputStream is = null;
        String result = "";
        String domain = "http://www.pennquiz.com/users.php";
        ArrayList<NameValuePair> library = new ArrayList<NameValuePair>();
        library.add(new BasicNameValuePair("funct", "getQs"));
        try {
            HttpClient httpclient = new DefaultHttpClient();
            HttpPost httppost = new HttpPost(domain);
            httppost.setEntity(new UrlEncodedFormEntity(library));
            HttpResponse response = httpclient.execute(httppost);
            HttpEntity entity = response.getEntity();
            is = entity.getContent();
        } catch (Exception e) {
            Log.e("log_tag", "Error in http connection " + e.toString());
        }
        try {
            BufferedReader reader = new BufferedReader(new InputStreamReader(is, "iso-8859-1"), 8);
            StringBuilder sb = new StringBuilder();
            String line = null;
            while ((line = reader.readLine()) != null) {
                sb.append(line);
            }
            is.close();
            result = sb.toString().substring(1, sb.length() - 1);
            return result.split(",");
        } catch (Exception e) {
            Log.e("log_tag", "Error converting result " + e.toString());
        }
        return null;
    }
}
