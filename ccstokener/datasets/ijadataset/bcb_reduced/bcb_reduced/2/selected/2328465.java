package com.softwaresmithy;

import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.Bitmap.CompressFormat;
import android.graphics.BitmapFactory;
import android.os.AsyncTask;
import android.util.Log;
import com.softwaresmithy.library.LibStatus;
import com.softwaresmithy.library.LibStatus.STATUS;
import com.softwaresmithy.library.Library;
import com.softwaresmithy.metadata.MetadataProvider;
import org.apache.http.HttpResponse;
import org.apache.http.client.HttpClient;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.impl.client.DefaultHttpClient;
import java.io.File;
import java.io.FileOutputStream;
import java.net.URI;

/**
 * Capsule class to perform network operations to gather metadata in a background thread,
 * and off the android UI thread. Current operations are
 * 1) gathering text metadata
 * 2) save to the database
 * 3) retrieve and save thumbnail image
 * 4) retrieve status from library (and update database row)
 *
 * @author SZY4ZQ
 */
public class DownloadDataTask extends AsyncTask<String, Void, Boolean> {

    /**
   * Passed in application context.
   */
    private Context c;

    /**
   * Passed in database accessor.
   */
    private WishlistDbAdapter mDbHelper;

    /**
   * library accessor.
   */
    private Library library;

    /**
   * metadata accessor.
   */
    private MetadataProvider data;

    /**
   * JPEG Compression ratio
   */
    private static final int COMPRESSION_RATIO = 95;

    private static final HttpClient client = new DefaultHttpClient();

    /**
   * Default constructor, just sets parameters to local class variables.
   *
   * @param c  application context
   * @param db database accessor
   * @param l  library status provider
   * @param dp metadata provider
   */
    public DownloadDataTask(Context c, WishlistDbAdapter db, Library l, MetadataProvider dp) {
        this.c = c;
        this.mDbHelper = db;
        this.library = l;
        this.data = dp;
    }

    @Override
    protected Boolean doInBackground(String... params) {
        String isbn = params[0];
        BookJB jb = addItemToDB(isbn);
        if (jb != null) {
            try {
                URI dest = new URI(jb.getThumbUrl());
                HttpGet cover = new HttpGet(dest);
                HttpResponse resp = client.execute(cover);
                if (resp.getEntity().getContentType().getValue().equals("image/jpeg")) {
                    Bitmap image = BitmapFactory.decodeStream(resp.getEntity().getContent());
                    File file = new File(c.getExternalCacheDir(), jb.getVolumeId() + ".jpg");
                    image.compress(CompressFormat.JPEG, COMPRESSION_RATIO, new FileOutputStream(file));
                }
            } catch (Exception e) {
                Log.e(DownloadDataTask.class.getName(), "Unable to save thumbnail", e);
            }
            STATUS retVal;
            if (library instanceof LibStatus) {
                retVal = ((LibStatus) library).checkAvailability(jb.getIsbn());
                if (retVal != null) {
                    jb.setState(retVal.name());
                    mDbHelper.updateItem(jb);
                }
            }
        }
        return jb != null;
    }

    /**
   * Looks up item metadata based on passed ISBN number then stores that info in the DB.
   *
   * @param isbn ISBN Number
   * @return BookJB populated with metadata
   */
    private BookJB addItemToDB(String isbn) {
        BookJB jb = data.getInfo(isbn);
        if (jb != null) {
            long id = mDbHelper.createItem(jb);
            jb.set_id(id);
            return jb;
        }
        return null;
    }
}
