package com.tx.farm.activity;

import java.io.BufferedReader;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.URL;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import org.apache.http.HttpResponse;
import org.apache.http.client.ClientProtocolException;
import org.apache.http.client.HttpClient;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.impl.client.DefaultHttpClient;
import org.json.JSONException;
import org.json.JSONObject;
import android.app.Activity;
import android.app.ActivityManager;
import android.app.AlertDialog;
import android.app.ProgressDialog;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.SharedPreferences;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Color;
import android.graphics.Matrix;
import android.net.Uri;
import android.os.AsyncTask;
import android.os.Bundle;
import android.os.Handler;
import android.os.SystemClock;
import android.util.DisplayMetrics;
import android.util.Log;
import android.view.KeyEvent;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.view.Window;
import android.view.View.OnClickListener;
import android.view.ViewGroup.LayoutParams;
import android.view.animation.Animation;
import android.view.animation.TranslateAnimation;
import android.webkit.JsResult;
import android.webkit.WebChromeClient;
import android.webkit.WebSettings;
import android.webkit.WebView;
import android.webkit.WebViewClient;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.Toast;
import com.tx.bean.Ad;
import com.tx.bean.TxMenu;
import com.tx.bean.User;
import com.tx.db.UserDao;
import com.tx.farm.R;
import com.tx.util.AuthUtils;
import com.tx.util.JsonParseUtil;
import com.tx.util.UrlConfigUtil;

/**
 * 天下客户端主界面
 *
 * @author Crane
 *
 */
public class TxActivity extends Activity {

    private String TAG = "TxActivity";

    private WebView webView;

    private String url = "http://mobile.tx.com.cn:8081/client/ahref.do";

    private StringBuffer sb = new StringBuffer();

    private List<TxMenu> listMenu = new ArrayList<TxMenu>();

    private User user;

    private String imsi;

    ProgressDialog progressDialog;

    boolean showDialog = false;

    String accessType = "local";

    Bundle savedInstanceState;

    LinearLayout adBannerLayout;

    ImageView iv;

    String imgUrl;

    private Animation showAction, hideAction;

    private boolean adBannerShowed = false;

    ;

    Handler handler = new Handler();

    @Override
    public void onCreate(Bundle savedInstanceState) {
        this.savedInstanceState = savedInstanceState;
        super.onCreate(savedInstanceState);
        setContentView(R.layout.tx_activity);
        SharedPreferences sp = getSharedPreferences("imsi.txt", 0);
        imsi = sp.getString("imsi", "");
        UserDao dao = new UserDao(this);
        user = dao.getUser(imsi);
        if (user == null) {
            Toast.makeText(TxActivity.this, getString(R.string.login_sys_err), Toast.LENGTH_SHORT).show();
        } else {
            url = "http://mobile.tx.com.cn:8081/client/ahref.do";
            sb.append("?viewerId=");
            sb.append(user.getUserId());
            sb.append("&imsi=");
            sb.append(user.getImsi());
            sb.append("&phonenum=");
            sb.append(user.getPhoneNum());
            progressDialog = new ProgressDialog(TxActivity.this);
            showDialog = true;
            progressDialog.setProgressStyle(ProgressDialog.STYLE_SPINNER);
            String loading = getString(R.string.loading);
            progressDialog.setMessage(loading);
            progressDialog.show();
            accessType = "local";
            webView = (WebView) findViewById(R.id.txactivity);
            webView.setBackgroundColor(Color.parseColor("#FFFFFF"));
            webView.setWebViewClient(new MyWebViewClient() {

                public void onProgressChanged(WebView view, int progress) {
                    context.setProgress(progress * 100);
                }
            });
            webView.setWebChromeClient(new MyWebChromeClient());
            WebSettings settings = webView.getSettings();
            settings.setJavaScriptEnabled(true);
            webView.setHorizontalScrollBarEnabled(false);
            webView.setVerticalScrollBarEnabled(false);
            webView.loadUrl(url + sb.toString() + "&type=2");
        }
        InputStream is;
        try {
            is = getAssets().open(UrlConfigUtil.MENU_URL);
            listMenu = JsonParseUtil.listMenu(is);
        } catch (IOException e) {
            e.printStackTrace();
        }
        adBannerLayout = (LinearLayout) findViewById(R.id.adBannerLayout);
        adBannerLayout.setVisibility(View.GONE);
        showAction = new TranslateAnimation(Animation.RELATIVE_TO_PARENT, 0.0f, Animation.RELATIVE_TO_PARENT, 0.0f, Animation.RELATIVE_TO_PARENT, 1.0f, Animation.RELATIVE_TO_PARENT, 0.0f);
        showAction.setDuration(5000);
        hideAction = new TranslateAnimation(Animation.RELATIVE_TO_PARENT, 0.0f, Animation.RELATIVE_TO_PARENT, 0.0f, Animation.RELATIVE_TO_PARENT, 0.0f, Animation.RELATIVE_TO_PARENT, 1.0f);
        hideAction.setDuration(5000);
        iv = (ImageView) findViewById(R.id.banner);
        iv.setOnClickListener(new OnClickListener() {

            @Override
            public void onClick(View v) {
                Intent intent = new Intent();
                intent.setAction("android.intent.action.VIEW");
                System.out.println("^^^^^^^^^^^^^^^^^^ url: " + imgUrl);
                Uri content_uri_browsers = Uri.parse(imgUrl);
                intent.setData(content_uri_browsers);
                intent.setClassName("com.android.browser", "com.android.browser.BrowserActivity");
                startActivity(intent);
                Log.i("Ad click", "************************");
            }
        });
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        System.exit(0);
    }

    public boolean onCreateOptionsMenu(Menu menu) {
        for (TxMenu txMenu : listMenu) {
            int order = txMenu.getOrder();
            String name = txMenu.getName();
            menu.add(0, 0, order, name);
        }
        return true;
    }

    final Activity context = this;

    @Override
    public boolean onMenuItemSelected(int featureId, MenuItem item) {
        int order = item.getOrder();
        System.out.println("*************** url = " + url + sb.toString());
        switch(item.getOrder()) {
            case 100:
                showDialog = false;
                final ActivityManager am = (ActivityManager) getSystemService(Context.ACTIVITY_SERVICE);
                am.restartPackage(getPackageName());
                finish();
                break;
            case 1:
                webView.loadUrl(url + sb.toString() + "&type=11");
                break;
            case 2:
                webView.loadUrl(url + sb.toString() + "&type=21");
                break;
            case 3:
                webView.loadUrl(url + sb.toString() + "&type=24");
                break;
            case 4:
                webView.loadUrl(url + sb.toString() + "&type=18");
                break;
            case 5:
                webView.loadUrl(url + sb.toString() + "&type=22");
                break;
            case 6:
                webView.loadUrl(url + sb.toString() + "&type=17");
                break;
            case 7:
                webView.loadUrl(url + sb.toString() + "&type=2");
                break;
            case 8:
                webView.loadUrl(url + sb.toString() + "&type=15");
                break;
            case 9:
                webView.loadUrl(url + sb.toString() + "&type=6");
                break;
            case 10:
                webView.loadUrl(url + sb.toString() + "&type=23");
                break;
        }
        return super.onMenuItemSelected(featureId, item);
    }

    private class MyWebViewClient extends WebViewClient {

        @Override
        public boolean shouldOverrideUrlLoading(WebView view, String url) {
            Log.i(TAG, "url is========>" + url);
            view.loadUrl(url);
            return true;
        }

        @Override
        public void onPageStarted(WebView view, String url, Bitmap favicon) {
            super.onPageStarted(view, url, favicon);
        }

        @Override
        public void onPageFinished(WebView view, String url) {
            String urlStr = "http://rest.tx.com.cn/api/c/ad/10241?uid=0&ip=0&imsi=0&width=0";
            new LoadAdTask().execute(urlStr);
        }

        @Override
        public void onReceivedError(WebView view, int errorCode, String description, String failingUrl) {
            super.onReceivedError(view, errorCode, description, failingUrl);
        }
    }

    private class MyWebChromeClient extends WebChromeClient {

        public void onProgressChanged(WebView view, int progress) {
            if (showDialog) {
                TxActivity.this.getWindow().setFeatureInt(Window.FEATURE_PROGRESS, progress * 100);
                if (progress == 100) {
                    progressDialog.dismiss();
                }
                progressDialog.setProgress(progress);
            }
            super.onProgressChanged(view, progress);
        }

        @Override
        public boolean onJsAlert(WebView view, String url, String message, final JsResult result) {
            AlertDialog.Builder b = new AlertDialog.Builder(TxActivity.this);
            b.setTitle("Alert");
            b.setMessage(message);
            b.setPositiveButton(android.R.string.ok, new AlertDialog.OnClickListener() {

                public void onClick(DialogInterface dialog, int which) {
                    result.confirm();
                }
            });
            b.setCancelable(false);
            b.create();
            b.show();
            return true;
        }

        ;

        @Override
        public boolean onJsConfirm(WebView view, String url, String message, final JsResult result) {
            AlertDialog.Builder b = new AlertDialog.Builder(TxActivity.this);
            b.setTitle("Confirm");
            b.setMessage(message);
            b.setPositiveButton(android.R.string.ok, new AlertDialog.OnClickListener() {

                public void onClick(DialogInterface dialog, int which) {
                    result.confirm();
                }
            });
            b.setNegativeButton(android.R.string.cancel, new DialogInterface.OnClickListener() {

                public void onClick(DialogInterface dialog, int which) {
                    result.cancel();
                }
            });
            b.setCancelable(false);
            b.create();
            b.show();
            return true;
        }

        ;
    }

    public void showUpdateDialog() {
        @SuppressWarnings("unused") AlertDialog alert = new AlertDialog.Builder(this).setTitle("提示").setIcon(R.drawable.tx).setMessage("确定退出" + getString(R.string.flat_name_farm) + "?").setPositiveButton("确定", new DialogInterface.OnClickListener() {

            public void onClick(DialogInterface dialog, int which) {
                finish();
            }
        }).setNegativeButton("取消", new DialogInterface.OnClickListener() {

            public void onClick(DialogInterface dialog, int which) {
                dialog.cancel();
            }
        }).show();
    }

    public boolean onKeyDown(int keyCode, KeyEvent event) {
        if (keyCode == KeyEvent.KEYCODE_BACK && webView.canGoBack()) {
            webView.goBack();
            return true;
        }
        if (keyCode != KeyEvent.KEYCODE_BACK) {
            return super.onKeyDown(keyCode, event);
        }
        showUpdateDialog();
        return true;
    }

    private Map fetchAdData(String url) throws ClientProtocolException, IOException {
        String app = "1";
        String owner = "tx";
        String session = "";
        String sdk = "ad1.0";
        String version = "txLove1.0";
        String timestamp = String.valueOf(System.currentTimeMillis());
        String sign = "";
        String appSecret = "test";
        Map<String, String> protocal = new HashMap<String, String>();
        protocal.put(AuthUtils.AUTH_APP, app);
        protocal.put(AuthUtils.AUTH_OWNER, owner);
        protocal.put(AuthUtils.AUTH_SESSION, session);
        protocal.put(AuthUtils.SDK, sdk);
        protocal.put(AuthUtils.VERSION, version);
        protocal.put(AuthUtils.TIMESTAMP, timestamp);
        Map<String, String> parameter = new HashMap<String, String>();
        parameter.put("uid", String.valueOf(user.getUserId()));
        parameter.put("ip", "0");
        parameter.put("imsi", imsi);
        parameter.put("width", "0");
        sign = AuthUtils.sign(protocal, parameter, appSecret);
        HttpClient client = new DefaultHttpClient();
        HttpGet request = new HttpGet(url.toString());
        request.setHeader(AuthUtils.AUTH_APP, app);
        request.setHeader(AuthUtils.AUTH_OWNER, owner);
        request.setHeader(AuthUtils.AUTH_SESSION, session);
        request.setHeader(AuthUtils.SDK, sdk);
        request.setHeader(AuthUtils.VERSION, version);
        request.setHeader(AuthUtils.TIMESTAMP, timestamp);
        request.setHeader(AuthUtils.SIGN, sign);
        HttpResponse response = client.execute(request);
        if (response.getStatusLine().getStatusCode() == 200) {
            BufferedReader reader = new BufferedReader(new InputStreamReader(response.getEntity().getContent()));
            String line = reader.readLine();
            JSONObject object;
            Map map = new HashMap();
            try {
                System.out.println("##################### line = " + line);
                object = new JSONObject(line);
                if (object != null) {
                    System.out.println(object.toString());
                    map.put("imgAddress", object.getString("imgurl"));
                    map.put("imgUrl", object.getString("url"));
                    return map;
                }
            } catch (JSONException e) {
                e.printStackTrace();
            }
        }
        return null;
    }

    /**
	* 获取指定路径，的数据。
	*
	* **/
    private byte[] getImage(String urlpath) throws Exception {
        URL url = new URL(urlpath);
        HttpURLConnection conn = (HttpURLConnection) url.openConnection();
        conn.setRequestMethod("GET");
        conn.setConnectTimeout(6 * 1000);
        if (conn.getResponseCode() == 200) {
            InputStream inputStream = conn.getInputStream();
            return readStream(inputStream);
        }
        return null;
    }

    /**
	  * 读取数据
	  * 输入流
	  *
	  * */
    public static byte[] readStream(InputStream inStream) throws Exception {
        ByteArrayOutputStream outstream = new ByteArrayOutputStream();
        byte[] buffer = new byte[1024];
        int len = -1;
        while ((len = inStream.read(buffer)) != -1) {
            outstream.write(buffer, 0, len);
        }
        outstream.close();
        inStream.close();
        return outstream.toByteArray();
    }

    private class LoadAdTask extends AsyncTask<String, Void, Ad> {

        @Override
        protected void onPostExecute(Ad ad) {
            if (ad == null || ad.getBm() == null) return;
            Bitmap bm = ad.getBm();
            int width = bm.getWidth();
            int height = bm.getHeight();
            LayoutParams params = adBannerLayout.getLayoutParams();
            DisplayMetrics dm = new DisplayMetrics();
            getWindowManager().getDefaultDisplay().getMetrics(dm);
            params.width = dm.widthPixels;
            params.height = 50 * (dm.heightPixels / 480);
            adBannerLayout.setLayoutParams(params);
            String url = ad.getImgUrl();
            Matrix matrix = new Matrix();
            float ww = 320;
            float hh = 50;
            float w = dm.widthPixels * (ww / 320) / (float) width;
            float h = dm.heightPixels * (hh / 480) / (float) height;
            matrix.postScale(w, h);
            Bitmap bm1 = Bitmap.createBitmap(bm, 0, 0, width, height, matrix, true);
            iv.setImageBitmap(bm1);
            imgUrl = url;
            if (!adBannerShowed) {
                adBannerLayout.setVisibility(View.VISIBLE);
                adBannerLayout.startAnimation(showAction);
                adBannerShowed = true;
            }
        }

        @Override
        protected Ad doInBackground(String... params) {
            try {
                Map adMap = fetchAdData(params[0]);
                if (adMap == null || adMap.size() < 2) return null;
                String imgAddress = (String) adMap.get("imgAddress");
                String imgUrl = (String) adMap.get("imgUrl");
                SystemClock.sleep(10 * 1000);
                System.out.println("img url: " + imgAddress);
                byte[] data = getImage(imgAddress);
                System.out.println("data length: " + data.length);
                Bitmap bm = BitmapFactory.decodeByteArray(data, 0, data.length);
                Ad ad = new Ad();
                ad.setBm(bm);
                ad.setImgUrl(imgUrl);
                return ad;
            } catch (ClientProtocolException e) {
                e.printStackTrace();
            } catch (IOException e) {
                e.printStackTrace();
            } catch (Exception e) {
                e.printStackTrace();
            }
            return null;
        }
    }
}
