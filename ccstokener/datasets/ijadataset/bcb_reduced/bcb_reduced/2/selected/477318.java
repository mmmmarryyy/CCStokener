package com.squareshoot.picCheckin;

import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.FilterInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.net.HttpURLConnection;
import java.net.URI;
import java.net.URISyntaxException;
import java.net.URL;
import java.util.List;
import org.apache.http.HttpEntity;
import org.apache.http.HttpException;
import org.apache.http.HttpHost;
import org.apache.http.HttpRequest;
import org.apache.http.HttpRequestInterceptor;
import org.apache.http.HttpResponse;
import org.apache.http.NameValuePair;
import org.apache.http.ParseException;
import org.apache.http.auth.AuthScope;
import org.apache.http.auth.AuthState;
import org.apache.http.auth.Credentials;
import org.apache.http.auth.UsernamePasswordCredentials;
import org.apache.http.client.ClientProtocolException;
import org.apache.http.client.CredentialsProvider;
import org.apache.http.client.HttpClient;
import org.apache.http.client.entity.UrlEncodedFormEntity;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.client.protocol.ClientContext;
import org.apache.http.cookie.Cookie;
import org.apache.http.impl.auth.BasicScheme;
import org.apache.http.impl.client.DefaultHttpClient;
import org.apache.http.impl.cookie.BasicClientCookie;
import org.apache.http.protocol.ExecutionContext;
import org.apache.http.protocol.HTTP;
import org.apache.http.protocol.HttpContext;
import org.apache.http.util.EntityUtils;
import android.content.Context;
import android.content.SharedPreferences;
import android.content.pm.PackageInfo;
import android.content.pm.PackageManager;
import android.content.pm.PackageManager.NameNotFoundException;
import android.database.Cursor;
import android.graphics.Bitmap;
import android.graphics.Bitmap.Config;
import android.graphics.BitmapFactory;
import android.graphics.Canvas;
import android.graphics.Paint;
import android.graphics.PorterDuff.Mode;
import android.graphics.PorterDuffXfermode;
import android.graphics.Rect;
import android.graphics.RectF;
import android.os.Build;
import android.os.Environment;
import android.preference.PreferenceManager;
import android.util.Log;

public class Common {

    protected static final String TAG = "squareshoot";

    protected static final String SERVICE_TAG = "sqService";

    protected static final String LINE_SEPARATOR = System.getProperty("line.separator");

    protected static final String PACKAGENAME = "com.squareshoot.picCheckin";

    protected static final String FEEDBACK_EMAIL_ADDRESS = "dev@squareshoot.com";

    protected static final String MARKET_URL = "market://search?q=pname:com.squareshoot.picCheckin";

    protected static final String FOURSQ_SETTINGS_URL = "https://foursquare.com/settings";

    protected static final String CLIENT_ID = "KHWD2M2VAACJSPDUN4ZHHXOY22FJRLUQZK4EKA5VFZCTPE3M";

    protected static final String CLIENT_SECRET = "R1EHJ0GTAFMPEGXNADILXDHDWBSOFZSXM1THQA3AIHKEVAS2";

    protected static String TEST_BASE_URL = "http://xxxxxx/api";

    protected static String TEST_BASE_CHANGELOG_URL = "http://xxxxxxx/changelog";

    protected static String TEST_BASE_IMGS_URL = "http://xxxxxx/";

    protected static String BASE_URL = "http://www.squareshoot.com/api";

    protected static String BASE_CHANGELOG_URL = "http://www.squareshoot.com/changelog";

    protected static String BASE_IMGS_URL = "http://www.squareshoot.com/";

    protected static String defaultCatUrl = "http://foursquare.com/img/categories/question.png";

    protected static String DIR = "/Squareshoot";

    protected static String CACHE = "/Squareshoot/.cache";

    protected static boolean DEBUG = true;

    protected static boolean HIGHDEBUG = true;

    protected static boolean DEBUG_URL = true;

    public static int WHD = 960;

    public static int HHD = 640;

    public static int WMD = 720;

    public static int HMD = 480;

    public static int WLD = 480;

    public static int HLD = 320;

    public static String getBaseImgsUrl() {
        if (DEBUG_URL) return TEST_BASE_IMGS_URL;
        return BASE_IMGS_URL;
    }

    public static String getBaseChangelogUrl() {
        if (DEBUG_URL) return TEST_BASE_CHANGELOG_URL;
        return BASE_CHANGELOG_URL;
    }

    public static String getBaseUrl() {
        if (DEBUG_URL) return TEST_BASE_URL;
        return BASE_URL;
    }

    public static String getUrlDataAuth(String url, String username, String password) throws IOException, ClientProtocolException {
        String reponse = null;
        DefaultHttpClient httpclient = new DefaultHttpClient();
        if (Common.DEBUG) Log.d(Common.TAG, "GET auth : " + url);
        HttpGet httpget = new HttpGet(url);
        HttpRequestInterceptor preemptiveAuth = new HttpRequestInterceptor() {

            public void process(final HttpRequest request, final HttpContext context) throws HttpException, IOException {
                AuthState authState = (AuthState) context.getAttribute(ClientContext.TARGET_AUTH_STATE);
                CredentialsProvider credsProvider = (CredentialsProvider) context.getAttribute(ClientContext.CREDS_PROVIDER);
                HttpHost targetHost = (HttpHost) context.getAttribute(ExecutionContext.HTTP_TARGET_HOST);
                if (authState.getAuthScheme() == null) {
                    AuthScope authScope = new AuthScope(targetHost.getHostName(), targetHost.getPort());
                    Credentials creds = credsProvider.getCredentials(authScope);
                    if (creds != null) {
                        authState.setAuthScheme(new BasicScheme());
                        authState.setCredentials(creds);
                    }
                }
            }
        };
        AuthScope authScope = new AuthScope(httpget.getURI().getHost(), httpget.getURI().getPort(), AuthScope.ANY_REALM);
        UsernamePasswordCredentials userpass = new UsernamePasswordCredentials(username, password);
        httpclient.getCredentialsProvider().setCredentials(authScope, userpass);
        httpclient.addRequestInterceptor(preemptiveAuth, 0);
        try {
            HttpResponse response = httpclient.execute(httpget);
            if (Common.DEBUG) Log.i(Common.TAG, response.getStatusLine().toString());
            if (response.getStatusLine().getStatusCode() == 200) {
                HttpEntity entity = response.getEntity();
                if (Common.DEBUG) Log.d(Common.TAG, "Response size=" + entity.getContentLength());
                if (entity != null) {
                    reponse = Common.convertStreamToString(entity);
                } else {
                    reponse = response.getStatusLine().toString();
                }
            } else {
                ClientProtocolException e = new ClientProtocolException(response.getStatusLine().toString());
                throw e;
            }
        } catch (ClientProtocolException e) {
            throw e;
        } catch (IOException e) {
            throw e;
        }
        if (Common.DEBUG) Log.i(Common.TAG, "GET AUTH response : " + reponse);
        return reponse;
    }

    public static String postUrlData(HttpClient httpclient, String url, List<NameValuePair> params) throws ClientProtocolException, IOException {
        String reponse = null;
        if (Common.DEBUG) Log.d(Common.TAG, "POST : " + url);
        HttpPost post = new HttpPost(url);
        try {
            UrlEncodedFormEntity ent = new UrlEncodedFormEntity(params, HTTP.UTF_8);
            post.setEntity(ent);
            HttpResponse responsePOST = httpclient.execute(post);
            if (Common.DEBUG) Log.i(Common.TAG, "reponse " + responsePOST.getStatusLine());
            HttpEntity entity = responsePOST.getEntity();
            if (Common.DEBUG) Log.d(Common.TAG, "Response size=" + entity.getContentLength());
            if (responsePOST.getStatusLine().getStatusCode() == 200) {
                if (entity != null) {
                    reponse = Common.convertStreamToString(entity);
                } else {
                    reponse = responsePOST.getStatusLine().toString();
                }
            } else {
                ClientProtocolException e = new ClientProtocolException(responsePOST.getStatusLine().toString());
                throw e;
            }
        } catch (ClientProtocolException e) {
            throw e;
        } catch (IOException e) {
            throw e;
        }
        if (Common.DEBUG) Log.d(Common.TAG, "POST reponse: " + reponse);
        return reponse;
    }

    public static String postUrlDataAuth(String url, String username, String password, List<NameValuePair> params) throws ClientProtocolException, IOException {
        String reponse = null;
        DefaultHttpClient httpclient = new DefaultHttpClient();
        if (Common.DEBUG) Log.d(Common.TAG, "POST auth : " + url);
        HttpPost post = new HttpPost(url);
        HttpRequestInterceptor preemptiveAuth = new HttpRequestInterceptor() {

            public void process(final HttpRequest request, final HttpContext context) throws HttpException, IOException {
                AuthState authState = (AuthState) context.getAttribute(ClientContext.TARGET_AUTH_STATE);
                CredentialsProvider credsProvider = (CredentialsProvider) context.getAttribute(ClientContext.CREDS_PROVIDER);
                HttpHost targetHost = (HttpHost) context.getAttribute(ExecutionContext.HTTP_TARGET_HOST);
                if (authState.getAuthScheme() == null) {
                    AuthScope authScope = new AuthScope(targetHost.getHostName(), targetHost.getPort());
                    Credentials creds = credsProvider.getCredentials(authScope);
                    if (creds != null) {
                        authState.setAuthScheme(new BasicScheme());
                        authState.setCredentials(creds);
                    }
                }
            }
        };
        AuthScope authScope = new AuthScope(post.getURI().getHost(), post.getURI().getPort(), AuthScope.ANY_REALM);
        UsernamePasswordCredentials userpass = new UsernamePasswordCredentials(username, password);
        httpclient.getCredentialsProvider().setCredentials(authScope, userpass);
        httpclient.addRequestInterceptor(preemptiveAuth, 0);
        try {
            UrlEncodedFormEntity ent = new UrlEncodedFormEntity(params, HTTP.UTF_8);
            post.setEntity(ent);
            HttpResponse responsePOST = httpclient.execute(post);
            if (Common.DEBUG) Log.i(Common.TAG, "reponse " + responsePOST.getStatusLine());
            HttpEntity entity = responsePOST.getEntity();
            if (Common.DEBUG) Log.d(Common.TAG, "Response size=" + entity.getContentLength());
            if (responsePOST.getStatusLine().getStatusCode() == 200) {
                if (entity != null) {
                    reponse = Common.convertStreamToString(entity);
                } else {
                    reponse = responsePOST.getStatusLine().toString();
                }
            } else {
                ClientProtocolException e = new ClientProtocolException(responsePOST.getStatusLine().toString());
                throw e;
            }
        } catch (ClientProtocolException e) {
            throw e;
        } catch (IOException e) {
            throw e;
        }
        if (Common.DEBUG) Log.d(Common.TAG, "POST auth reponse: " + reponse);
        return reponse;
    }

    public static String convertStreamToString(HttpEntity entity) {
        String result = null;
        try {
            result = EntityUtils.toString(entity);
        } catch (ParseException e) {
            e.printStackTrace();
        } catch (IOException e) {
            e.printStackTrace();
        }
        return result;
    }

    public static Bitmap downloadImage(String url) throws IOException {
        Bitmap bm = null;
        InputStream is = null;
        URL photoURL;
        try {
            photoURL = new URL(url);
            HttpURLConnection conn = (HttpURLConnection) photoURL.openConnection();
            conn.connect();
            is = conn.getInputStream();
            bm = BitmapFactory.decodeStream(new FlushedInputStream(is));
            if (is != null) is.close();
        } catch (IOException e1) {
            throw e1;
        }
        return bm;
    }

    public static Bitmap getCachedPicture(String url) {
        Bitmap bm = null;
        try {
            bm = getImagefromSD(url);
            if (bm == null) {
                if (Common.HIGHDEBUG) Log.d(Common.TAG, "SD not accessible, downloading");
                try {
                    bm = downloadImage(url);
                } catch (IOException e) {
                    if (Common.HIGHDEBUG) Log.d(Common.TAG, "get cached picture: " + e.getMessage());
                    e.printStackTrace();
                }
            }
        } catch (FileNotFoundException e) {
            try {
                bm = downloadImage(url);
                savePicture(bm, url);
            } catch (IOException e1) {
                e1.printStackTrace();
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
        return bm;
    }

    public static Bitmap getImagefromSD(String url) throws IOException {
        Bitmap bm = null;
        String filename = urlToFilename(url);
        updateExternalStorageState();
        if (mExternalStorageWriteable) {
            FileInputStream fis;
            try {
                fis = new FileInputStream(filename);
                bm = BitmapFactory.decodeStream(new FlushedInputStream(fis));
                fis.close();
            } catch (FileNotFoundException e) {
                throw e;
            } catch (IOException e) {
                throw e;
            }
        } else {
            if (Common.DEBUG) Log.d(Common.TAG, "SD not accessible");
        }
        return bm;
    }

    public static String savePicture(Bitmap bm, String url) {
        updateExternalStorageState();
        if (mExternalStorageWriteable) {
            String filename = urlToFilename(url);
            FileOutputStream fileOutputStream = null;
            File directory = new File(Environment.getExternalStorageDirectory().toString() + CACHE);
            if (!directory.exists()) {
                directory.mkdirs();
            } else {
            }
            try {
                File file = new File(filename);
                fileOutputStream = new FileOutputStream(file);
                bm.compress(Bitmap.CompressFormat.PNG, 100, fileOutputStream);
                fileOutputStream.flush();
                fileOutputStream.close();
            } catch (FileNotFoundException e) {
                e.printStackTrace();
                if (Common.HIGHDEBUG) Log.e(Common.TAG, "filenotfound excp" + e.getMessage());
            } catch (IOException e) {
                e.printStackTrace();
                if (Common.HIGHDEBUG) Log.e(Common.TAG, "IO excp");
            }
            return filename;
        } else {
            if (Common.DEBUG) Log.e(Common.TAG, "SD error");
            return null;
        }
    }

    public static Cookie recupCookie(Context context) {
        ProfileDb mDbHelper = new ProfileDb(context);
        mDbHelper.open();
        Cursor c = mDbHelper.recupCookie(1);
        mDbHelper.close();
        String cookieTxt = c.getString(c.getColumnIndexOrThrow(ProfileDb.KEY_COOKIE));
        c.close();
        String[] temp = cookieTxt.split("::");
        BasicClientCookie cookie = new BasicClientCookie(temp[1], "value");
        cookie.setVersion(Integer.parseInt(temp[0]));
        cookie.setValue(temp[2]);
        cookie.setDomain(temp[3]);
        cookie.setPath(temp[4]);
        return cookie;
    }

    public static Bitmap getRoundedCornerBitmap(Bitmap bitmap) {
        Bitmap output = Bitmap.createBitmap(bitmap.getWidth(), bitmap.getHeight(), Config.ARGB_8888);
        Canvas canvas = new Canvas(output);
        final int color = 0xff424242;
        final Paint paint = new Paint();
        final Rect rect = new Rect(0, 0, bitmap.getWidth(), bitmap.getHeight());
        final RectF rectF = new RectF(rect);
        final float roundPx = 6;
        paint.setAntiAlias(true);
        canvas.drawARGB(0, 0, 0, 0);
        paint.setColor(color);
        canvas.drawRoundRect(rectF, roundPx, roundPx, paint);
        paint.setXfermode(new PorterDuffXfermode(Mode.SRC_IN));
        canvas.drawBitmap(bitmap, rect, rect, paint);
        return output;
    }

    static class FlushedInputStream extends FilterInputStream {

        public FlushedInputStream(InputStream inputStream) {
            super(inputStream);
        }

        @Override
        public long skip(long n) throws IOException {
            long totalBytesSkipped = 0L;
            while (totalBytesSkipped < n) {
                long bytesSkipped = in.skip(n - totalBytesSkipped);
                if (bytesSkipped == 0L) {
                    int b = read();
                    if (b < 0) {
                        break;
                    } else {
                        bytesSkipped = 1;
                    }
                }
                totalBytesSkipped += bytesSkipped;
            }
            return totalBytesSkipped;
        }
    }

    static boolean mExternalStorageAvailable = false;

    static boolean mExternalStorageWriteable = false;

    static void updateExternalStorageState() {
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

    private static String urlToFilename(String url) {
        String filename = url.substring(7);
        filename = filename.replaceAll("/", "_");
        filename = Environment.getExternalStorageDirectory().toString() + CACHE + "/" + filename;
        return filename;
    }

    public static boolean checkValidHtmlPage(String page) {
        DefaultHttpClient httpclient = new DefaultHttpClient();
        URI url = null;
        try {
            url = new URI(page);
        } catch (URISyntaxException e1) {
            e1.printStackTrace();
        }
        HttpGet httpget = new HttpGet(url);
        try {
            HttpResponse response = httpclient.execute(httpget);
            if (response.getStatusLine().getStatusCode() == 200) {
                return true;
            } else {
                return false;
            }
        } catch (ClientProtocolException e) {
            e.printStackTrace();
        } catch (IOException e) {
            e.printStackTrace();
        }
        return false;
    }

    static NewDims getNewDimensions(int oldHeight, int oldWidth, Context ctx) {
        NewDims newdims = new NewDims();
        int grandeDimension;
        SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(ctx);
        String i = prefs.getString("sizePhoto", "MD");
        if (Common.HIGHDEBUG) Log.v(Common.TAG, "size photo = " + i);
        if (i.equals("LD")) {
            grandeDimension = WLD;
        } else if (i.equals("HD")) {
            grandeDimension = WHD;
        } else {
            grandeDimension = WMD;
        }
        if (oldHeight <= grandeDimension && oldWidth <= grandeDimension) {
            newdims.resize = false;
            return newdims;
        }
        if (oldHeight > oldWidth) {
            newdims.newHeight = grandeDimension;
            float ratio = (float) oldHeight / (float) oldWidth;
            newdims.newWidth = (int) (grandeDimension / ratio);
        } else if (oldWidth > oldHeight) {
            newdims.newWidth = grandeDimension;
            float ratio = (float) oldWidth / (float) oldHeight;
            newdims.newHeight = (int) (grandeDimension / ratio);
        } else {
            newdims.newHeight = grandeDimension;
            newdims.newWidth = grandeDimension;
        }
        if (Common.HIGHDEBUG) {
            Log.v(Common.TAG, "oldHeight " + oldHeight);
            Log.v(Common.TAG, "oldWidth " + oldWidth);
            Log.v(Common.TAG, "newHeight " + newdims.newHeight);
            Log.v(Common.TAG, "newWidth " + newdims.newWidth);
        }
        return newdims;
    }

    static class NewDims {

        int newHeight;

        int newWidth;

        boolean resize = true;
    }

    public static byte[] getLittlePicture(String maPic, Context ctx) {
        Bitmap maBitmap = BitmapFactory.decodeFile(maPic);
        NewDims newdims = getNewDimensions(maBitmap.getHeight(), maBitmap.getWidth(), ctx);
        ByteArrayOutputStream outstream = new ByteArrayOutputStream();
        byte[] image = null;
        if (newdims.resize) {
            Bitmap tmpPic = Bitmap.createScaledBitmap(maBitmap, newdims.newWidth, newdims.newHeight, true);
            maBitmap.recycle();
            try {
                tmpPic.compress(Bitmap.CompressFormat.JPEG, 95, outstream);
                image = outstream.toByteArray();
                outstream.close();
                if (Common.HIGHDEBUG) Log.d(TAG, "HAHAHA length : " + image.length);
                tmpPic.recycle();
            } catch (IOException e) {
                e.printStackTrace();
                if (Common.HIGHDEBUG) Log.e(Common.TAG, "IO excp" + e.getMessage());
            }
        } else {
            if (Common.HIGHDEBUG) Log.d(TAG, "Pas de resize pour cette image. Recompression");
            try {
                maBitmap.compress(Bitmap.CompressFormat.JPEG, 95, outstream);
                image = outstream.toByteArray();
                outstream.close();
                maBitmap.recycle();
            } catch (IOException e) {
                e.printStackTrace();
                if (Common.HIGHDEBUG) Log.e(Common.TAG, "IO excp" + e.getMessage());
            }
        }
        return image;
    }

    public static boolean deletePic(Context ctx) {
        SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(ctx);
        return prefs.getBoolean("deletePic", false);
    }

    public static String getVersion(Context ctx) {
        PackageInfo pInfo = null;
        try {
            pInfo = ctx.getPackageManager().getPackageInfo(Common.PACKAGENAME, PackageManager.GET_META_DATA);
        } catch (NameNotFoundException e) {
            pInfo = null;
        }
        if (pInfo != null) return "Version : " + pInfo.packageName + " : " + pInfo.versionName + " : " + pInfo.versionCode;
        return null;
    }

    public static String getSystemInfo(Context ctx) {
        return "System :" + "SDK:" + Build.VERSION.SDK_INT + " Device: " + Build.DEVICE + " Construct :" + Build.MANUFACTURER + " Model :" + Build.MODEL + " Product :" + Build.PRODUCT;
    }
}
