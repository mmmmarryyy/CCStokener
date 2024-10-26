package org.light.portal.mobile.portlets;

import java.io.BufferedInputStream;
import java.net.URL;
import java.net.URLConnection;
import java.util.ArrayList;
import org.light.portal.mobile.EntityAdapter;
import org.light.portal.mobile.R;
import org.light.portal.mobile.model.Entity;
import org.light.portal.mobile.model.SearchEntity;
import org.light.portal.mobile.model.User;
import org.light.portal.mobile.util.Configuration;
import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;

public class SearchAdapter extends EntityAdapter {

    public SearchAdapter(Context context, int textViewResourceId, ArrayList<Entity> items) {
        super(context, textViewResourceId, items);
    }

    @Override
    public View getView(int position, View convertView, ViewGroup parent) {
        View v = convertView;
        if (v == null) {
            LayoutInflater vi = (LayoutInflater) this.getContext().getSystemService(Context.LAYOUT_INFLATER_SERVICE);
            v = vi.inflate(R.layout.search_item, null);
        }
        try {
            if (this.getCount() > position) {
                Object o = this.getItem(position);
                if (o != null) {
                    if (o instanceof User) {
                        User user = (User) o;
                        TextView title = (TextView) v.findViewById(R.id.searchItemName);
                        title.setText(user.getName());
                        final ImageView imageView = (ImageView) v.findViewById(R.id.searchPhoto);
                        String imageUrl = user.getPhotoUrl();
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
                    } else if (o instanceof SearchEntity) {
                        SearchEntity item = (SearchEntity) o;
                        TextView title = (TextView) v.findViewById(R.id.searchItemName);
                        title.setText(item.getType() + ": " + item.getName());
                        final ImageView imageView = (ImageView) v.findViewById(R.id.searchPhoto);
                        String imageUrl = item.getPhotoUrl();
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
                }
            }
        } catch (Exception e) {
        }
        return v;
    }
}
