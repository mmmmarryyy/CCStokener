package org.light.portal.mobile.portlets;

import static org.light.portal.mobile.util.Configuration.JSON_SERVICE_MICROBLOG;
import java.io.BufferedInputStream;
import java.net.URL;
import java.net.URLConnection;
import java.net.URLEncoder;
import org.json.JSONArray;
import org.json.JSONObject;
import org.light.portal.mobile.BaseListActivity;
import org.light.portal.mobile.R;
import org.light.portal.mobile.Session;
import org.light.portal.mobile.model.Microblog;
import org.light.portal.mobile.model.User;
import org.light.portal.mobile.util.Configuration;
import org.light.portal.mobile.util.HttpHelper;
import android.content.Intent;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.view.View.OnClickListener;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.ListView;
import android.widget.TextView;

public class MicroblogActivity extends BaseListActivity {

    public static final String tag = "Microblog";

    private static int pageId;

    /** Called when the activity is first created. */
    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.microblog);
        final TextView previous = (TextView) findViewById(R.id.microblogPrevious);
        previous.setOnClickListener(new OnClickListener() {

            public void onClick(View v) {
                try {
                    if (pageId > 0) {
                        pageId--;
                        startActivity(new Intent(getApplicationContext(), MicroblogActivity.class));
                    }
                    Log.i(tag, "Previous onClick complete.");
                } catch (Exception e) {
                    Log.e(tag, "error: " + e.getMessage());
                }
            }
        });
        final TextView refresh = (TextView) findViewById(R.id.microblogRefresh);
        refresh.setOnClickListener(new OnClickListener() {

            public void onClick(View v) {
                try {
                    pageId = 0;
                    loadData();
                    Log.i(tag, "Previous onClick complete.");
                } catch (Exception e) {
                    Log.e(tag, "error: " + e.getMessage());
                }
            }
        });
        final TextView next = (TextView) findViewById(R.id.microblogNext);
        next.setOnClickListener(new OnClickListener() {

            public void onClick(View v) {
                try {
                    pageId++;
                    startActivity(new Intent(getApplicationContext(), MicroblogActivity.class));
                    Log.i(tag, "Next onClick complete.");
                } catch (Exception e) {
                    Log.e(tag, "error: " + e.getMessage());
                }
            }
        });
        final Button postButton = (Button) findViewById(R.id.microblogPost);
        postButton.setOnClickListener(new OnClickListener() {

            public void onClick(View v) {
                try {
                    final EditText text = (EditText) findViewById(R.id.microblogText);
                    String content = text.getText().toString();
                    if (content != null && content.length() > 0) {
                        String result = HttpHelper.getIntance().get(getPostURL(content));
                        if (result != null) {
                            text.setText("");
                            JSONObject contents = new JSONObject(result);
                            JSONArray list = contents.getJSONArray("microblogs");
                            Session.currentPortletContents.clear();
                            for (int i = 0; i < list.length(); i++) {
                                JSONObject entity = list.getJSONObject(i);
                                try {
                                    Session.currentPortletContents.add(new Microblog(entity.getLong("id"), entity.getString("content"), entity.getString("displayName"), entity.getString("photoUrl"), entity.getString("date")));
                                } catch (Exception e) {
                                    Log.e(tag, "error: " + e.getMessage());
                                }
                                JSONArray comments = entity.getJSONArray("comments");
                                for (int j = 0; j < comments.length(); j++) {
                                    JSONObject comment = comments.getJSONObject(j);
                                    try {
                                        Session.currentPortletContents.add(new Microblog(comment.getLong("id"), comment.getString("comment"), comment.getString("displayName"), comment.getString("photoUrl"), comment.getString("date"), true));
                                    } catch (Exception e) {
                                        Log.e(tag, "error: " + e.getMessage());
                                    }
                                }
                            }
                            listAdapter.notifyDataSetChanged();
                        }
                    }
                    Log.i(tag, "post onClick complete.");
                } catch (Exception e) {
                    Log.e(tag, "error: " + e.getMessage());
                    e.printStackTrace();
                }
            }
        });
        bindSync();
    }

    @Override
    protected void onResume() {
        if (Session.refreshParentActivity) {
            Session.refreshParentActivity = false;
            loadData();
        }
        super.onResume();
    }

    private String getPostURL(String content) {
        StringBuilder url = new StringBuilder();
        url.append(Configuration.getDomain()).append(JSON_SERVICE_MICROBLOG).append(Configuration.getSuffix()).append("?action=add").append("&content=").append(URLEncoder.encode(content));
        return url.toString();
    }

    @Override
    protected void bindAdapter() {
        this.listAdapter = new MicroblogAdapter(this, R.layout.microblog_item, Session.currentPortletContents);
        setListAdapter(this.listAdapter);
    }

    @Override
    protected boolean needLoad() {
        return true;
    }

    @Override
    protected boolean isCachable() {
        return false;
    }

    @Override
    protected String getURL() {
        StringBuilder url = new StringBuilder();
        url.append(Configuration.getDomain()).append(JSON_SERVICE_MICROBLOG).append(Configuration.getSuffix()).append("?max=").append(Configuration.getMaxShowRowMicroblog());
        if (pageId > 0) {
            url.append("&page=").append(pageId + 1);
        }
        ;
        return url.toString();
    }

    @Override
    protected void processData(String data) {
        try {
            JSONObject contents = new JSONObject(data);
            final TextView name = (TextView) findViewById(R.id.microblogUserName);
            String userName = contents.getString("userName");
            name.setText(userName);
            final ImageView imageView = (ImageView) findViewById(R.id.microblogUserPhoto);
            String photoUrl = contents.getString("userPhotoUrl");
            String imageUrl = photoUrl;
            if ((imageUrl != null) && !imageUrl.equals("")) {
                try {
                    URL url = new URL(Configuration.getDomain() + imageUrl);
                    URLConnection conn = url.openConnection();
                    conn.connect();
                    BufferedInputStream bis = new BufferedInputStream(conn.getInputStream());
                    Bitmap bm = BitmapFactory.decodeStream(bis);
                    bis.close();
                    imageView.setImageBitmap(bm);
                    Session.user = new User(userName, photoUrl, bm);
                } catch (Exception e) {
                    Log.e(tag, "error: " + e.getMessage());
                }
            }
            JSONArray list = contents.getJSONArray("microblogs");
            Session.currentPortletContents.clear();
            for (int i = 0; i < list.length(); i++) {
                JSONObject entity = list.getJSONObject(i);
                try {
                    Session.currentPortletContents.add(new Microblog(entity.getLong("id"), entity.getString("content"), entity.getString("displayName"), entity.getString("photoUrl"), entity.getString("date")));
                } catch (Exception e) {
                    Log.e(tag, "error: " + e.getMessage());
                }
                JSONArray comments = entity.getJSONArray("comments");
                for (int j = 0; j < comments.length(); j++) {
                    JSONObject comment = comments.getJSONObject(j);
                    try {
                        Session.currentPortletContents.add(new Microblog(comment.getLong("id"), comment.getString("comment"), comment.getString("displayName"), comment.getString("photoUrl"), comment.getString("date"), true));
                    } catch (Exception e) {
                        Log.e(tag, "error: " + e.getMessage());
                    }
                }
            }
            listAdapter.notifyDataSetChanged();
        } catch (Exception e) {
            Log.e(tag, "error: " + e.getMessage());
        }
    }

    @Override
    protected void onListItemClick(ListView l, View v, int position, long id) {
    }
}
