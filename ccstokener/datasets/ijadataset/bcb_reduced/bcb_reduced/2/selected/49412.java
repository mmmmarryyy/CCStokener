package org.light.portal.mobile.portlets;

import java.io.BufferedInputStream;
import java.net.URL;
import java.net.URLConnection;
import org.light.portal.mobile.BaseActivity;
import org.light.portal.mobile.R;
import org.light.portal.mobile.Session;
import org.light.portal.mobile.model.SearchEntity;
import org.light.portal.mobile.model.User;
import org.light.portal.mobile.util.Configuration;
import android.content.Intent;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.net.Uri;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.view.View.OnClickListener;
import android.webkit.WebView;
import android.widget.ImageView;
import android.widget.TextView;

public class SearchContentActivity extends BaseActivity {

    public static final String tag = "Search";

    /** Called when the activity is first created. */
    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        if (Session.currentSearchEntity instanceof User) {
            showUserDetail((User) Session.currentSearchEntity);
        } else if (Session.currentSearchEntity instanceof SearchEntity) {
            showSearchEntity((SearchEntity) Session.currentSearchEntity);
        }
    }

    private void showUserDetail(final User user) {
        setContentView(R.layout.profile);
        try {
            this.setTitle(user.getName());
            final TextView name = (TextView) findViewById(R.id.userName);
            name.setText(user.getUrl());
            name.setOnClickListener(new OnClickListener() {

                public void onClick(View v) {
                    Intent browserIntent = new Intent(Intent.ACTION_VIEW, Uri.parse(user.getUrl()));
                    startActivity(browserIntent);
                }
            });
            final ImageView imageView = (ImageView) findViewById(R.id.userImageView);
            String imageUrl = user.getPhotoUrl();
            if ((imageUrl != null) && !imageUrl.equals("")) {
                URL url = new URL(Configuration.getDomain() + imageUrl);
                URLConnection conn = url.openConnection();
                conn.connect();
                BufferedInputStream bis = new BufferedInputStream(conn.getInputStream());
                Bitmap bm = BitmapFactory.decodeStream(bis);
                bis.close();
                imageView.setImageBitmap(bm);
            }
        } catch (Exception e) {
            Log.i(tag, e.getMessage());
        }
    }

    private void showSearchEntity(SearchEntity entity) {
        setContentView(R.layout.search_content);
        final WebView webview = (WebView) findViewById(R.id.searchWebView);
        webview.loadUrl(entity.getLink());
        this.setTitle(entity.getLink());
    }

    @Override
    protected boolean needLoad() {
        return false;
    }

    @Override
    protected boolean isCachable() {
        return false;
    }

    @Override
    protected String getURL() {
        StringBuilder url = new StringBuilder();
        return url.toString();
    }

    @Override
    protected void processData(String data) {
    }
}
