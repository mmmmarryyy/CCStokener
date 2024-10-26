package com.google.code.dhillon;

import java.io.IOException;
import java.net.MalformedURLException;
import java.net.URL;
import java.net.URLEncoder;
import java.util.ArrayList;
import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;
import android.app.Dialog;
import android.app.ListActivity;
import android.app.ProgressDialog;
import android.content.Context;
import android.os.AsyncTask;
import android.os.Bundle;
import android.view.View;
import android.view.View.OnClickListener;
import android.view.inputmethod.InputMethodManager;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.EditText;
import android.widget.Toast;
import com.google.code.dhillon.util.Util;

public class ActivityUI extends ListActivity {

    private static final int DIALOG_LOADING = 0;

    private EditText mSearch = null;

    private Button mSubmit = null;

    /** Called when the activity is first created. */
    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.main);
        mSearch = (EditText) findViewById(R.id.search);
        mSubmit = (Button) findViewById(R.id.search_button);
        mSubmit.setOnClickListener(new OnClickListener() {

            public void onClick(View v) {
                new DownloadTwitterFeed().execute(mSearch.getText());
                InputMethodManager imm = (InputMethodManager) getSystemService(Context.INPUT_METHOD_SERVICE);
                imm.hideSoftInputFromWindow(mSearch.getWindowToken(), 0);
            }
        });
    }

    @Override
    protected Dialog onCreateDialog(int id) {
        if (id == DIALOG_LOADING) {
            ProgressDialog dialog = new ProgressDialog(this);
            dialog.setMessage(getText(R.string.twitter_load));
            dialog.setIndeterminate(true);
            return dialog;
        }
        return null;
    }

    private class DownloadTwitterFeed extends AsyncTask<CharSequence, Void, ArrayList<String>> {

        @Override
        protected void onPreExecute() {
            showDialog(DIALOG_LOADING);
            super.onPreExecute();
        }

        @Override
        protected ArrayList<String> doInBackground(CharSequence... params) {
            String username = params[0] + "";
            ArrayList<String> nowPlayingStatuses = new ArrayList<String>();
            try {
                URL url = new URL("http://twitter.com/statuses/user_timeline/" + URLEncoder.encode(username, "UTF-8") + ".json?count=200");
                String response = Util.convertStreamToString(url.openStream());
                JSONArray statuses = new JSONArray(response);
                for (int i = 0; i < statuses.length(); i++) {
                    JSONObject status = statuses.getJSONObject(i);
                    String statusText = status.getString("text");
                    if (statusText.toLowerCase().contains("#nowplaying")) {
                        nowPlayingStatuses.add(statusText);
                    }
                }
            } catch (MalformedURLException e) {
                e.printStackTrace();
                cancel(false);
            } catch (IOException e) {
                e.printStackTrace();
                cancel(false);
            } catch (JSONException e) {
                e.printStackTrace();
                cancel(false);
            }
            return nowPlayingStatuses;
        }

        @Override
        protected void onCancelled() {
            super.onCancelled();
            dismissDialog(DIALOG_LOADING);
            Toast.makeText(ActivityUI.this, getText(R.string.fetch_error), Toast.LENGTH_LONG).show();
        }

        protected void onPostExecute(ArrayList<String> statuses) {
            ArrayAdapter<String> adapter = new ArrayAdapter<String>(ActivityUI.this, android.R.layout.simple_list_item_1, statuses);
            setListAdapter(adapter);
            dismissDialog(DIALOG_LOADING);
        }
    }
}
