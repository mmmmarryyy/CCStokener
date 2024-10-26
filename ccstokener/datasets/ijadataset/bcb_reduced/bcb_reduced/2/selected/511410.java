package org.light.portal.mobile.portlets;

import static org.light.portal.mobile.util.Configuration.JSON_SERVICE_PROFILE;
import java.io.BufferedInputStream;
import java.net.URL;
import java.net.URLConnection;
import org.json.JSONObject;
import org.light.portal.mobile.BaseActivity;
import org.light.portal.mobile.R;
import org.light.portal.mobile.Session;
import org.light.portal.mobile.util.Configuration;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.os.Bundle;
import android.util.Log;
import android.widget.ImageView;
import android.widget.TextView;

public class ProfileActivity extends BaseActivity {

    public static final String tag = "Profile";

    /** Called when the activity is first created. */
    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.profile);
        bind();
    }

    @Override
    protected boolean needLoad() {
        return true;
    }

    @Override
    protected boolean isCachable() {
        return true;
    }

    @Override
    protected String getURL() {
        StringBuilder url = new StringBuilder();
        url.append(Configuration.getDomain()).append(JSON_SERVICE_PROFILE).append(Configuration.getSuffix());
        return url.toString();
    }

    @Override
    protected void processData(String data) {
        Session.currentEntity = null;
        try {
            JSONObject content = new JSONObject(data);
            JSONObject user = content.getJSONObject("user");
            this.setTitle(user.getString("name"));
            final TextView name = (TextView) findViewById(R.id.userName);
            name.setText(user.getString("url"));
            final ImageView imageView = (ImageView) findViewById(R.id.userImageView);
            String imageUrl = user.getString("photoUrl");
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
}
