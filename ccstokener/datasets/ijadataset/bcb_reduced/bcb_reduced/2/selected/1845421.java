package com.squareshoot.picCheckin;

import java.io.ByteArrayInputStream;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.List;
import org.apache.http.HttpEntity;
import org.apache.http.HttpResponse;
import org.apache.http.NameValuePair;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.cookie.Cookie;
import org.apache.http.entity.mime.HttpMultipartMode;
import org.apache.http.entity.mime.MultipartEntity;
import org.apache.http.entity.mime.content.ContentBody;
import org.apache.http.entity.mime.content.FileBody;
import org.apache.http.entity.mime.content.InputStreamBody;
import org.apache.http.entity.mime.content.StringBody;
import org.apache.http.impl.client.DefaultHttpClient;
import org.apache.http.message.BasicNameValuePair;
import org.apache.http.params.CoreProtocolPNames;
import org.apache.http.util.EntityUtils;
import org.json.JSONException;
import org.json.JSONObject;
import android.app.Activity;
import android.app.AlertDialog;
import android.app.Dialog;
import android.app.ProgressDialog;
import android.content.ContentValues;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.res.Resources;
import android.database.Cursor;
import android.graphics.Bitmap;
import android.graphics.drawable.Drawable;
import android.media.MediaScannerConnection;
import android.media.MediaScannerConnection.MediaScannerConnectionClient;
import android.net.Uri;
import android.os.AsyncTask;
import android.os.Build;
import android.os.Bundle;
import android.os.Environment;
import android.os.Handler;
import android.os.Message;
import android.provider.MediaStore;
import android.util.Log;
import android.view.Gravity;
import android.view.KeyEvent;
import android.view.View;
import android.view.View.OnClickListener;
import android.widget.Button;
import android.widget.CompoundButton;
import android.widget.CompoundButton.OnCheckedChangeListener;
import android.widget.EditText;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;
import android.widget.ToggleButton;

public class CheckinToVenue extends Activity {

    private CheckinTask checkinTask;

    private final int DIALOG_LOADING = 444;

    private final int DIALOG_PHOTO = 333;

    private static final int ACTIVITY_GALLERY = 10;

    private static final int CAMERA = 90;

    private Uri photoUri;

    MediaScannerConnection m_pScanner;

    double latitude, longitude;

    private DefaultHttpClient httpclient;

    ;

    private String username, password, venueId, venue;

    private String checkinResult;

    private String photoPath = null;

    private Thread getMayorPic;

    private Thread getBadgePic;

    private Bitmap mayorPicture;

    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.checkindialog);
        setTitle(getString(R.string.checkinTitle));
        httpclient = new DefaultHttpClient();
        Cookie cookie = Common.recupCookie(this);
        if (Common.HIGHDEBUG) Log.i(Common.TAG, "cookie : " + cookie);
        httpclient.getCookieStore().addCookie(cookie);
        httpclient.getParams().setBooleanParameter(CoreProtocolPNames.USE_EXPECT_CONTINUE, false);
        Bundle extras = getIntent().getExtras();
        username = extras.getString("username");
        password = extras.getString("password");
        venueId = extras.getString("venueId");
        photoPath = extras.getString("photoPath");
        latitude = extras.getDouble("latitude");
        longitude = extras.getDouble("longitude");
        venue = extras.getString("venue");
        photoUri = extras.getParcelable("photoUri");
        if (extras.containsKey("mayorPic")) mayorPicture = Common.getRoundedCornerBitmap((Bitmap) extras.getParcelable("mayorPic"));
        final ToggleButton boxPrivate = (ToggleButton) findViewById(R.id.checkBoxPrivate);
        final ToggleButton boxTwitter = (ToggleButton) findViewById(R.id.checkBoxTwitter);
        final ToggleButton boxFacebook = (ToggleButton) findViewById(R.id.checkBoxFacebook);
        final EditText message = (EditText) findViewById(R.id.checkinText);
        boxPrivate.setOnCheckedChangeListener(new OnCheckedChangeListener() {

            public void onCheckedChanged(CompoundButton buttonView, boolean isChecked) {
                if (isChecked) {
                    boxTwitter.setChecked(false);
                    boxFacebook.setChecked(false);
                }
            }
        });
        boxTwitter.setChecked(extras.getBoolean("sendtotwitter"));
        boxFacebook.setChecked(extras.getBoolean("sendtofacebook"));
        setTitle(getString(R.string.checkinTitle) + " " + venue);
        ImageButton choosePhotos = (ImageButton) findViewById(R.id.addPictureButton);
        choosePhotos.setOnClickListener(new OnClickListener() {

            public void onClick(View v) {
                updateExternalStorageState();
                if (mExternalStorageWriteable) {
                    showDialog(DIALOG_PHOTO);
                } else {
                    Toast erreurToast = Toast.makeText(CheckinToVenue.this, getString(R.string.sdnotavailable), Toast.LENGTH_LONG);
                    erreurToast.setGravity(Gravity.CENTER, 0, 0);
                    erreurToast.show();
                }
            }
        });
        if (savedInstanceState != null) {
            photoPath = savedInstanceState.getString("photoPath");
            photoUri = (Uri) savedInstanceState.get("photoUri");
        }
        if (photoUri != null) setImage();
        Button checkinButton = (Button) findViewById(R.id.checkinButton);
        checkinButton.setOnClickListener(new OnClickListener() {

            public void onClick(View v) {
                String checkinPrivate, twitter, facebook;
                showDialog(DIALOG_LOADING);
                if (boxPrivate.isChecked()) checkinPrivate = "1"; else checkinPrivate = "0";
                if (boxTwitter.isChecked()) twitter = "1"; else twitter = "0";
                if (boxFacebook.isChecked()) facebook = "1"; else facebook = "0";
                checkinTask = new CheckinTask();
                checkinTask.setActivity(CheckinToVenue.this);
                Log.d(Common.TAG, "message : " + message.getText().toString());
                checkinTask.execute(photoPath, message.getText().toString(), checkinPrivate, twitter, facebook);
            }
        });
        Button xmasButton = (Button) findViewById(R.id.xmasButton);
        xmasButton.setOnClickListener(new OnClickListener() {

            public void onClick(View v) {
                message.setText(message.getText().toString() + " #xmastree");
            }
        });
        final Button showHidePref = (Button) findViewById(R.id.ButtonPrivacy);
        Resources res = getResources();
        final Drawable up = res.getDrawable(R.drawable.arrow_white_left);
        final Drawable down = res.getDrawable(R.drawable.arrow_white_down);
        Button privacyButton = (Button) findViewById(R.id.ButtonPrivacy);
        privacyButton.setOnClickListener(new OnClickListener() {

            public void onClick(View v) {
                if (findViewById(R.id.linearCheckBox).getVisibility() == View.GONE) {
                    findViewById(R.id.linearCheckBox).setVisibility(View.VISIBLE);
                    showHidePref.setText(R.string.shareSettingsOff);
                    showHidePref.setCompoundDrawablesWithIntrinsicBounds(down, null, null, null);
                } else {
                    findViewById(R.id.linearCheckBox).setVisibility(View.GONE);
                    showHidePref.setText(R.string.shareSettings);
                    showHidePref.setCompoundDrawablesWithIntrinsicBounds(up, null, null, null);
                }
            }
        });
        Object threadRetained = getLastNonConfigurationInstance();
        if (threadRetained != null) {
            if (threadRetained instanceof CheckinTask) {
                checkinTask = (CheckinTask) threadRetained;
                checkinTask.setActivity(this);
            }
        }
    }

    public Object onRetainNonConfigurationInstance() {
        if (checkinTask != null) {
            checkinTask.setActivity(null);
            return checkinTask;
        }
        return null;
    }

    @Override
    public void onSaveInstanceState(Bundle outState) {
        super.onSaveInstanceState(outState);
        if (Common.HIGHDEBUG) Log.w(Common.TAG, "CheckinToVenue : onSaveInstantState");
        outState.putParcelable("photoUri", photoUri);
        if (photoPath != null) outState.putString("photoPath", photoPath);
    }

    protected void printResultat() {
        setTitle(getString(R.string.checkinOK));
        findViewById(R.id.checkinScroll).setVisibility(View.GONE);
        findViewById(R.id.checkinFinishScroll).setVisibility(View.VISIBLE);
        TextView message = (TextView) findViewById(R.id.checkedinMessage);
        TextView picture = (TextView) findViewById(R.id.pictureLink);
        TextView mayorText = (TextView) findViewById(R.id.mayorText);
        final ImageView mayorPic = (ImageView) findViewById(R.id.mayorPic);
        try {
            JSONObject jResultat = new JSONObject(checkinResult);
            final String photoLink;
            if (jResultat.has("photoLink")) {
                photoLink = jResultat.getString("photoLink");
                picture.setVisibility(View.VISIBLE);
                picture.setText(photoLink);
                picture.setOnClickListener(new OnClickListener() {

                    public void onClick(View v) {
                        Intent i = new Intent(Intent.ACTION_VIEW);
                        i.setData(Uri.parse(photoLink));
                        startActivity(i);
                    }
                });
            } else {
                picture.setVisibility(View.GONE);
            }
            jResultat = jResultat.getJSONObject("checkin");
            message.setText(jResultat.getString("message"));
            if (jResultat.has("mayor")) {
                JSONObject jMayor = jResultat.getJSONObject("mayor");
                mayorText.setText(jMayor.getString("message"));
                Handler h = new Handler() {

                    public void handleMessage(Message msg) {
                        Bundle data = msg.getData();
                        if (data.containsKey("photo")) {
                            mayorPic.setImageBitmap(Common.getRoundedCornerBitmap((Bitmap) data.getParcelable("photo")));
                        } else {
                            if (Common.HIGHDEBUG) Log.d(Common.TAG, "Error while downloading mayorPic : " + data.getString("eMessage"));
                        }
                    }
                };
                if (jMayor.has("user")) {
                    getMayorPic = threadGetImage(h, jMayor.getJSONObject("user").getString("photo"));
                    getMayorPic.start();
                } else {
                    mayorPic.setImageBitmap(mayorPicture);
                }
            } else {
                findViewById(R.id.mayorDialog).setVisibility(View.GONE);
            }
            if (jResultat.has("badges")) {
                findViewById(R.id.badgeTable).setVisibility(View.VISIBLE);
                final ImageView badgePic = (ImageView) findViewById(R.id.badgePic);
                TextView badgeText = (TextView) findViewById(R.id.badgeText);
                JSONObject jBadge = jResultat.getJSONArray("badges").getJSONObject(0);
                badgeText.setText(getString(R.string.gotBadge) + " " + jBadge.getString("name") + " badge: " + jBadge.getString("description"));
                Handler h2 = new Handler() {

                    public void handleMessage(Message msg) {
                        Bundle data = msg.getData();
                        if (data.containsKey("photo")) {
                            badgePic.setImageBitmap((Bitmap) data.getParcelable("photo"));
                        } else {
                            if (Common.HIGHDEBUG) Log.d(Common.TAG, "Error while downloading badgePic : " + data.getString("eMessage"));
                        }
                    }
                };
                getBadgePic = threadGetImage(h2, jBadge.getString("icon"));
                getBadgePic.start();
            } else {
                findViewById(R.id.badgeTable).setVisibility(View.GONE);
            }
        } catch (JSONException e) {
            e.printStackTrace();
        }
    }

    protected Dialog onCreateDialog(int id) {
        switch(id) {
            case DIALOG_LOADING:
                ProgressDialog progressLoading = new ProgressDialog(CheckinToVenue.this);
                progressLoading.setTitle(getString(R.string.loading));
                progressLoading.setMessage(getString(R.string.pleasewait));
                return progressLoading;
            case DIALOG_PHOTO:
                final CharSequence[] items = { getString(R.string.chooseFromGallery), getString(R.string.takePhoto) };
                AlertDialog.Builder builder = new AlertDialog.Builder(this);
                builder.setTitle(getString(R.string.addPhoto));
                builder.setItems(items, new DialogInterface.OnClickListener() {

                    public void onClick(DialogInterface dialog, int item) {
                        if (item == 0) startGallery();
                        if (item == 1) startCamera();
                    }
                });
                AlertDialog photo = builder.create();
                return photo;
        }
        return null;
    }

    private void startGallery() {
        updateExternalStorageState();
        if (mExternalStorageWriteable) {
            Intent i = new Intent(Intent.ACTION_PICK);
            i.setType(MediaStore.Images.Media.CONTENT_TYPE);
            startActivityForResult(i, ACTIVITY_GALLERY);
        } else {
            Toast erreurToast = Toast.makeText(CheckinToVenue.this, getString(R.string.sdnotavailable), Toast.LENGTH_LONG);
            erreurToast.setGravity(Gravity.CENTER, 0, 0);
            erreurToast.show();
        }
    }

    private void startCamera() {
        photoUri = null;
        String filename = "sqshoot-" + String.valueOf(System.currentTimeMillis() + ".jpg");
        String path = Environment.getExternalStorageDirectory().toString() + Common.DIR;
        File file = new File(path, filename);
        photoPath = file.getPath();
        if (Common.HIGHDEBUG) Log.d(Common.TAG, "new photo path: " + photoPath);
        Intent cam = new Intent(android.provider.MediaStore.ACTION_IMAGE_CAPTURE);
        cam.putExtra(MediaStore.EXTRA_OUTPUT, Uri.fromFile(file));
        startActivityForResult(cam, CAMERA);
    }

    boolean mExternalStorageAvailable = false;

    boolean mExternalStorageWriteable = false;

    public boolean fromGallerie = false;

    void updateExternalStorageState() {
        String state = Environment.getExternalStorageState();
        if (Environment.MEDIA_MOUNTED.equals(state)) {
            mExternalStorageAvailable = mExternalStorageWriteable = true;
        } else if (Environment.MEDIA_MOUNTED_READ_ONLY.equals(state)) {
            mExternalStorageAvailable = true;
            mExternalStorageWriteable = false;
        } else {
            mExternalStorageAvailable = mExternalStorageWriteable = false;
        }
    }

    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        Log.d(Common.TAG, "VenueDetails : OnActivityResult");
        super.onActivityResult(requestCode, resultCode, data);
        switch(requestCode) {
            case ACTIVITY_GALLERY:
                if (resultCode == RESULT_OK) {
                    photoUri = data.getData();
                    if (Common.HIGHDEBUG) Log.d(Common.TAG, "Selected image: " + photoUri);
                    setImage();
                }
                break;
            case CAMERA:
                if (Common.HIGHDEBUG) {
                    Log.d(Common.TAG, "ResultCode : " + resultCode);
                    Log.d(Common.TAG, "Retour Camera photoUri : " + photoUri);
                    Log.d(Common.TAG, "Retour Camera photoPath : " + photoPath);
                    if (data != null) {
                        Log.d(Common.TAG, "data not null");
                    } else {
                        Log.d(Common.TAG, "data null");
                    }
                }
                if (resultCode == Activity.RESULT_OK) {
                    ContentValues values = new ContentValues();
                    values.put(MediaStore.Images.Media.DATA, photoPath);
                    values.put(MediaStore.Images.Media.MIME_TYPE, "image/jpeg");
                    values.put(MediaStore.Images.Media.LATITUDE, latitude);
                    values.put(MediaStore.Images.Media.LONGITUDE, longitude);
                    photoUri = getContentResolver().insert(MediaStore.Images.Media.EXTERNAL_CONTENT_URI, values);
                    if (Common.HIGHDEBUG) Log.d(Common.TAG, "New uri: " + photoUri);
                    setImage();
                    m_pScanner = new MediaScannerConnection(this, new MediaScannerConnectionClient() {

                        public void onMediaScannerConnected() {
                            m_pScanner.scanFile(photoPath, null);
                        }

                        public void onScanCompleted(String path, Uri uri) {
                            if (Common.HIGHDEBUG) Log.d(Common.TAG, "Scan completed. Path=" + path + " Uri=" + uri);
                        }
                    });
                    m_pScanner.connect();
                } else {
                    photoPath = null;
                    photoUri = null;
                }
                break;
        }
    }

    private void setImage() {
        if (Common.HIGHDEBUG) Log.d(Common.TAG, "Venue Details URI a afficher : " + photoUri);
        ImageButton selected = (ImageButton) findViewById(R.id.addPictureButton);
        Cursor c = managedQuery(photoUri, null, null, null, null);
        int column_data = c.getColumnIndexOrThrow(MediaStore.Images.Media.DATA);
        int column_id = c.getColumnIndexOrThrow(MediaStore.Images.Media._ID);
        c.moveToFirst();
        Bitmap bm = null;
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.ECLAIR) {
            if (Common.HIGHDEBUG) Log.w(Common.TAG, "SDK=" + Build.VERSION.SDK_INT + " on utilise queryminthumbnails");
        } else {
            if (Common.HIGHDEBUG) Log.w(Common.TAG, "SDK=" + Build.VERSION.SDK_INT + " on utilise getThumbnails");
            bm = MediaStore.Images.Thumbnails.getThumbnail(getContentResolver(), c.getLong(column_id), MediaStore.Images.Thumbnails.MINI_KIND, null);
        }
        photoPath = c.getString(column_data);
        selected.setImageBitmap(bm);
        c.close();
        if (Common.HIGHDEBUG) Log.i(Common.TAG, "setImage() photoUri :" + photoUri.toString() + " photoPath: " + photoPath);
    }

    @Override
    public boolean onKeyDown(int code, KeyEvent event) {
        if (event.getKeyCode() == 80) {
            startCamera();
            return true;
        }
        if (event.getKeyCode() == 4) {
            finish();
            return true;
        }
        return false;
    }

    private void onThreadCheckinCompleted(Bundle data) {
        if (!data.containsKey("eMessage")) {
            checkinResult = data.getString("checkinResult");
            JSONObject jResultat = null;
            try {
                jResultat = new JSONObject(checkinResult);
                jResultat = jResultat.getJSONObject("checkin");
                printResultat();
            } catch (JSONException e) {
                String error = null;
                try {
                    error = jResultat.getString("error");
                } catch (JSONException e2) {
                    error = getString(R.string.checkinError);
                }
                Toast erreurToast = Toast.makeText(CheckinToVenue.this, error, Toast.LENGTH_LONG);
                erreurToast.setGravity(Gravity.CENTER, 0, 0);
                erreurToast.show();
                if (Common.HIGHDEBUG) Log.d(Common.TAG, error);
            }
        } else {
            Toast erreurToast = Toast.makeText(CheckinToVenue.this, data.getString("eMessage"), Toast.LENGTH_SHORT);
            erreurToast.setGravity(Gravity.CENTER, 0, 0);
            erreurToast.show();
        }
    }

    private class CheckinTask extends AsyncTask<String, String, Message> {

        private CheckinToVenue activity;

        private boolean completed = false;

        private Message msg;

        protected void onPreExecute() {
            activity.showDialog(DIALOG_LOADING);
            msg = Message.obtain();
        }

        protected Message doInBackground(String... parameters) {
            String photo = parameters[0];
            String shout = parameters[1];
            String checkinPrivate = parameters[2];
            String twitter = parameters[3];
            String facebook = parameters[4];
            Bundle data = new Bundle();
            String result;
            List<NameValuePair> params = new ArrayList<NameValuePair>();
            params.add(new BasicNameValuePair("vid", venueId));
            params.add(new BasicNameValuePair("shout", shout));
            params.add(new BasicNameValuePair("user", username));
            params.add(new BasicNameValuePair("password", password));
            params.add(new BasicNameValuePair("private", checkinPrivate));
            params.add(new BasicNameValuePair("twitter", twitter));
            params.add(new BasicNameValuePair("facebook", facebook));
            if (Common.HIGHDEBUG) {
                Log.d(Common.TAG, "Checkin private:" + params.get(4).getValue());
                Log.d(Common.TAG, "Checkin twitter:" + params.get(5).getValue());
                Log.d(Common.TAG, "Checkin facebook:" + params.get(6).getValue());
            }
            String postURL = Common.getBaseUrl() + "/upload";
            Log.d(Common.TAG, "URL : " + postURL);
            DefaultHttpClient threadHttpClient = activity.httpclient;
            if (photo != null) {
                long MAX_UPLOAD_SIZE = 300 * 1024;
                File file = new File(photo);
                long imageSize = file.length();
                if (Common.HIGHDEBUG) {
                    Log.d(Common.TAG, "photo : " + photo);
                    Log.d(Common.TAG, "Size : " + imageSize);
                }
                ContentBody bin = null;
                if (imageSize <= MAX_UPLOAD_SIZE) {
                    if (Common.HIGHDEBUG) Log.d(Common.TAG, "Photo is small enough. No resize.");
                    bin = new FileBody(file);
                } else {
                    byte[] image = Common.getLittlePicture(photo, CheckinToVenue.this);
                    bin = new InputStreamKnownSizeBody(new ByteArrayInputStream(image), image.length, "image/jpeg", photo);
                }
                try {
                    HttpPost post = new HttpPost(postURL);
                    MultipartEntity reqEntity = new MultipartEntity(HttpMultipartMode.BROWSER_COMPATIBLE);
                    reqEntity.addPart("myPicture", bin);
                    post.setEntity(reqEntity);
                    for (int i = 0; i < params.size(); i++) {
                        StringBody sb = new StringBody(params.get(i).getValue());
                        reqEntity.addPart(params.get(i).getName(), sb);
                    }
                    HttpResponse response = threadHttpClient.execute(post);
                    HttpEntity resEntity = response.getEntity();
                    if (resEntity != null) {
                        result = EntityUtils.toString(resEntity);
                        data.putString("checkinResult", result);
                    }
                } catch (Exception e) {
                    data.putString("eMessage", e.getMessage());
                    if (Common.HIGHDEBUG) {
                        Log.e(Common.TAG, "Exception uploadImage: " + e.getMessage());
                        e.printStackTrace();
                    }
                }
            } else {
                try {
                    result = Common.postUrlData(threadHttpClient, postURL, params);
                    data.putString("checkinResult", result);
                } catch (IOException e) {
                    data.putString("eMessage", e.getMessage());
                }
            }
            if (Common.HIGHDEBUG) Log.d(Common.TAG, "serverCheckin : " + data.getString("checkinResult"));
            msg.setData(data);
            return msg;
        }

        protected void onPostExecute(Message msg) {
            completed = true;
            if (activity != null) endTask();
        }

        private void setActivity(CheckinToVenue activity) {
            this.activity = activity;
            if (completed) {
                endTask();
            }
        }

        private void endTask() {
            activity.checkinTask = null;
            activity.dismissDialog(DIALOG_LOADING);
            activity.onThreadCheckinCompleted(msg.getData());
        }
    }

    class InputStreamKnownSizeBody extends InputStreamBody {

        private int length;

        public InputStreamKnownSizeBody(final InputStream in, final int length, final String mimeType, final String filename) {
            super(in, mimeType, filename);
            this.length = length;
        }

        @Override
        public long getContentLength() {
            return this.length;
        }
    }

    protected Thread threadGetImage(final Handler h, final String url) {
        Thread t = new Thread() {

            public void run() {
                Message msg = Message.obtain();
                Bundle data = new Bundle();
                if (url == null) {
                    if (Common.HIGHDEBUG) Log.w(Common.TAG, "VenueDetails : url null");
                } else {
                    try {
                        Bitmap bm = Common.downloadImage(url);
                        if (bm != null) data.putParcelable("photo", bm);
                    } catch (IOException e) {
                        if (Common.HIGHDEBUG) Log.d(Common.TAG, "get image : " + e.getMessage());
                    }
                }
                msg.setData(data);
                h.sendMessage(msg);
            }
        };
        return t;
    }
}
