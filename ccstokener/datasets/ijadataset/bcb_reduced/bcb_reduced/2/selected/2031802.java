package org.light.portal.mobile.portlets;

import java.io.BufferedInputStream;
import java.net.URL;
import java.net.URLConnection;
import java.util.ArrayList;
import org.light.portal.mobile.EntityAdapter;
import org.light.portal.mobile.R;
import org.light.portal.mobile.model.Entity;
import org.light.portal.mobile.model.ForumPost;
import org.light.portal.mobile.util.Configuration;
import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;

public class ForumContentAdapter extends EntityAdapter {

    public ForumContentAdapter(Context context, int textViewResourceId, ArrayList<Entity> items) {
        super(context, textViewResourceId, items);
    }

    @Override
    public View getView(int position, View convertView, ViewGroup parent) {
        View v = convertView;
        if (v == null) {
            LayoutInflater vi = (LayoutInflater) this.getContext().getSystemService(Context.LAYOUT_INFLATER_SERVICE);
            v = vi.inflate(R.layout.forum_post_item, null);
        }
        ForumPost o = (ForumPost) this.getItem(position);
        if (o != null) {
            TextView title = (TextView) v.findViewById(R.id.forumItemName);
            title.setText(o.getTitle());
            final ImageView imageView = (ImageView) v.findViewById(R.id.forumPhoto);
            String imageUrl = o.getPhotoUrl();
            if ((imageUrl != null) && !imageUrl.equals("")) {
                try {
                    if (!imageUrl.startsWith("http")) imageUrl = Configuration.getDomain() + imageUrl;
                    URL url = new URL(imageUrl);
                    URLConnection conn = url.openConnection();
                    conn.connect();
                    BufferedInputStream bis = new BufferedInputStream(conn.getInputStream());
                    Bitmap bm = BitmapFactory.decodeStream(bis);
                    bis.close();
                    if (bm != null) imageView.setImageBitmap(bm);
                } catch (Exception e) {
                }
            }
        }
        return v;
    }
}
