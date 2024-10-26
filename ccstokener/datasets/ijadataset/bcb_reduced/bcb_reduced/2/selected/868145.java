package com.shengyijie.activity.share;

import java.net.HttpURLConnection;
import java.net.URL;
import java.util.Set;
import java.util.TreeSet;
import org.json.JSONObject;
import weibo4android.Status;
import weibo4android.Weibo;
import weibo4android.http.ImageItem;
import com.shengyijie.activity.R;
import com.shengyijie.context.ContextApplication;
import com.shengyijie.util.Utility;
import com.tencent.weibo.api.T_API;
import com.tencent.weibo.utils.Configuration;
import android.app.Activity;
import android.app.ProgressDialog;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.DialogInterface.OnKeyListener;
import android.graphics.drawable.Drawable;
import android.os.AsyncTask;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.os.Message;
import android.view.KeyEvent;
import android.view.View;
import android.view.Window;
import android.view.View.OnClickListener;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

public class ShareDialog extends Activity implements OnClickListener {

    private Button share;

    private EditText content;

    public Task task;

    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        requestWindowFeature(Window.FEATURE_CUSTOM_TITLE);
        setContentView(R.layout.sharedialog);
        getWindow().setFeatureInt(Window.FEATURE_CUSTOM_TITLE, R.layout.title);
        initView();
    }

    public void initView() {
        share = (Button) this.findViewById(R.id.right);
        share.setVisibility(View.VISIBLE);
        share.setOnClickListener(this);
        content = (EditText) this.findViewById(R.id.content);
        content.setText(ContextApplication.shareContent);
        mypDialog = new ProgressDialog(this);
        mypDialog.setProgressStyle(ProgressDialog.STYLE_SPINNER);
        mypDialog.setIndeterminate(false);
        mypDialog.setCancelable(false);
        mypDialog.setOnKeyListener(new DialogOnKeyListener());
    }

    public class DialogOnKeyListener implements OnKeyListener {

        public boolean onKey(DialogInterface dialog, int keyCode, KeyEvent event) {
            if (keyCode == KeyEvent.KEYCODE_BACK) {
                try {
                    mypDialog.hide();
                    if (task.getStatus() == AsyncTask.Status.RUNNING) {
                        task.cancel(true);
                    }
                } catch (Exception e) {
                }
            }
            return true;
        }
    }

    String shareContent = "";

    @Override
    public void onClick(View v) {
        switch(v.getId()) {
            case R.id.right:
                if (ShareConstant.share_type == ShareConstant.SHARE_SINA) {
                    System.setProperty("weibo4j.oauth.consumerKey", Weibo.CONSUMER_KEY);
                    System.setProperty("weibo4j.oauth.consumerSecret", Weibo.CONSUMER_SECRET);
                    shareContent = content.getText().toString();
                    task = new Task();
                    task.execute(TASK_SHARE_SINA);
                } else if (ShareConstant.share_type == ShareConstant.SHARE_RENREN) {
                    task = new Task();
                    task.execute(TASK_SHARE_RENREN);
                } else if (ShareConstant.share_type == ShareConstant.SHARE_TENCNT) {
                    task = new Task();
                    task.execute(TASK_SHARE_TENCENT);
                }
                break;
        }
    }

    Drawable d = null;

    public String shareSinaWeibo() {
        String result = "fail";
        try {
            Weibo weibo = new Weibo();
            weibo.setToken(ShareConstant.token, ShareConstant.tokenSecret);
            Status status = weibo.updateStatus(shareContent);
            if (status != null && status.getId() > 10) {
                result = "success";
            } else {
                result = "fail";
                errmessage = "对不起，分享失败。";
            }
        } catch (Exception e) {
            errmessage = "对不起，分享失败。";
        }
        return result;
    }

    public String shareTencentWeibo() {
        String result = "fail";
        try {
            T_API tapi = new T_API();
            ShareSelectActivity.oauth.setOauth_token(ShareConstant.token);
            ShareSelectActivity.oauth.setOauth_token_secret(ShareConstant.tokenSecret);
            String clientIp = Configuration.wifiIp;
            String sss = content.getText().toString();
            sss = Utility.getLimitLengthString(sss, 100);
            String response = tapi.add(ShareSelectActivity.oauth, "json", sss, clientIp, "", "");
            JSONObject json = new JSONObject(response);
            try {
                int code = json.getInt("ret");
                if (code == 0) result = "success"; else {
                    result = "fail";
                    if (code == 3) {
                        errmessage = "对不起，您的登录会话已过期，请重新登录。";
                        TokenStore.clear(this, ShareConstant.SHARE_TENCNT);
                    } else if (code == 2) {
                        errmessage = "对不起，您发表的频率太快，休息一会吧。";
                    } else if (code == 4) {
                        int errorCode = json.getInt("errcode");
                        if (errorCode == 10) {
                            errmessage = "对不起，您发表的频率太快，休息一会吧。";
                        } else if (errorCode == 13) {
                            errmessage = "对不起，您发表的微博重复了。";
                        } else {
                            errmessage = "对不起，分享失败。";
                        }
                    }
                }
            } catch (Exception e) {
                errmessage = "对不起，分享失败。";
            }
        } catch (Exception e) {
            errmessage = "对不起，分享失败。";
        }
        return result;
    }

    String errmessage = "";

    public String shareRenren() {
        String result = "fail";
        try {
            Bundle params = prepareParams(ShareConstant.token, content.getText().toString(), ShareConstant.RENREN_API_SECRET);
            int code = openUrl("http://api.renren.com/restserver.do", "POST", params);
            if (code == 1) {
                result = "success";
            } else {
                result = "fail";
                if (code == 303) errmessage = "对不起，您今天发表的新鲜事次数超过限额。"; else if (code == 450 || code == 6) {
                    errmessage = "对不起，您的登录会话已过期，请重新登录。";
                    TokenStore.clear(this, ShareConstant.SHARE_RENREN);
                } else {
                    errmessage = "对不起，分享失败。";
                }
            }
        } catch (Exception e) {
            errmessage = "对不起，分享失败。";
        }
        return result;
    }

    public int openUrl(String url, String method, Bundle params) {
        int result = 0;
        try {
            if (method.equals("GET")) {
                url = url + "?" + Utility.encodeUrl(params);
            }
            String response = "";
            HttpURLConnection conn = (HttpURLConnection) new URL(url).openConnection();
            conn.setRequestProperty("User-Agent", System.getProperties().getProperty("http.agent") + " RenrenAndroidSDK");
            if (!method.equals("GET")) {
                conn.setRequestMethod("POST");
                conn.setDoOutput(true);
                conn.getOutputStream().write(Utility.encodeUrl(params).getBytes("UTF-8"));
            }
            response = Utility.read(conn.getInputStream());
            JSONObject json = new JSONObject(response);
            try {
                int code = json.getInt("result");
                if (code > 0) result = 1;
            } catch (Exception e) {
                result = json.getInt("error_code");
                errmessage = json.getString("error_msg");
            }
        } catch (Exception e) {
            result = -1;
        }
        return result;
    }

    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        if (requestCode == 1) {
            if (resultCode != RESULT_OK) {
                return;
            }
        }
    }

    private Bundle prepareParams(String session_key, String status, String apiSecret) {
        Bundle params = new Bundle();
        params.putString("call_id", String.valueOf(System.currentTimeMillis()));
        params.putString("v", "1.0");
        params.putString("access_token", session_key);
        params.putString("method", "status.set");
        params.putString("format", "JSON");
        String sss = Utility.ToSBC(status);
        sss = Utility.getLimitLengthString(sss, 100);
        params.putString("status", status);
        StringBuffer sb = new StringBuffer();
        Set<String> keys = new TreeSet<String>(params.keySet());
        for (String key : keys) {
            sb.append(key);
            sb.append("=");
            sb.append(params.getString(key));
        }
        sb.append(apiSecret);
        params.putString("sig", Utility.md5(sb.toString()));
        return params;
    }

    private ProgressDialog mypDialog;

    private static final int ALERT_LOADING = 2;

    private static final int RESULT_SHARE_OK = 3;

    private static final int RESULT_SHARE_FAIL = 4;

    private static final String TASK_SHARE_RENREN = "renren";

    private static final String TASK_SHARE_SINA = "sina";

    private static final String TASK_SHARE_TENCENT = "tencent";

    private final Handler handler = new Handler(Looper.getMainLooper()) {

        public void handleMessage(Message msg) {
            super.handleMessage(msg);
            switch(msg.what) {
                case ALERT_LOADING:
                    mypDialog.setMessage("正在分享...");
                    mypDialog.show();
                    break;
                case RESULT_SHARE_OK:
                    try {
                        mypDialog.hide();
                        Bundle bd = new Bundle();
                        bd.putString("state", "exit");
                        Intent it = new Intent();
                        it.putExtras(bd);
                        Toast.makeText(ShareDialog.this, "分享成功", Toast.LENGTH_SHORT).show();
                        ShareDialog.this.setResult(RESULT_OK, it);
                        ShareDialog.this.finish();
                    } catch (Exception e) {
                    }
                    break;
                case RESULT_SHARE_FAIL:
                    try {
                        mypDialog.hide();
                        Toast.makeText(ShareDialog.this, errmessage, Toast.LENGTH_SHORT).show();
                    } catch (Exception e) {
                    }
                    break;
            }
        }
    };

    private class Task extends AsyncTask<String, String, String> {

        @Override
        protected String doInBackground(String... params) {
            if (params[0].equals(TASK_SHARE_RENREN)) {
                return shareRenren();
            } else if (params[0].equals(TASK_SHARE_TENCENT)) {
                return shareTencentWeibo();
            } else if (params[0].equals(TASK_SHARE_SINA)) {
                return shareSinaWeibo();
            } else {
                return "";
            }
        }

        @Override
        protected void onPostExecute(String result) {
            super.onPostExecute(result);
            if (result.equals("fail")) {
                Message msg = Message.obtain();
                msg.what = RESULT_SHARE_FAIL;
                handler.sendMessage(msg);
            } else if (result.equals("success")) {
                Message msg = Message.obtain();
                msg.what = RESULT_SHARE_OK;
                handler.sendMessage(msg);
            }
        }

        @Override
        protected void onPreExecute() {
            super.onPreExecute();
            Message msg = Message.obtain();
            msg.what = ALERT_LOADING;
            handler.sendMessage(msg);
        }

        @Override
        protected void onProgressUpdate(String... values) {
            super.onProgressUpdate(values);
        }
    }
}
