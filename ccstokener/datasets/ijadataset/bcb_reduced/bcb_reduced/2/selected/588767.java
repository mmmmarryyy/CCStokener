package netposa.contentframe;

import java.io.BufferedInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.net.MalformedURLException;
import java.net.URL;
import java.util.Timer;
import java.util.TimerTask;
import netposa.entity.PreviewImageEntity;
import netposa.network.BaseNetOperation;
import netposa.network.INetOperation;
import netposa.network.NetOperationParam;
import netposa.npm.R;
import android.app.AlertDialog;
import android.content.Context;
import android.content.DialogInterface;
import android.database.Cursor;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.drawable.Drawable;
import android.net.Uri;
import android.os.Handler;
import android.os.Message;
import android.text.SpannableString;
import android.text.SpannableStringBuilder;
import android.view.View;
import android.view.View.OnClickListener;
import android.widget.AdapterView.OnItemSelectedListener;
import android.widget.Button;
import android.widget.CheckBox;
import android.widget.ImageView;
import android.widget.ListAdapter;
import android.widget.TextView;
import android.widget.Toast;

public class PreviewViewer implements Runnable {

    private AlertDialog pd;

    private ImageView previewImage;

    private String code;

    private BaseNetOperation mNetoperation;

    private Context context;

    private Thread thread;

    private Handler handler;

    private Button previewrefresh;

    private Button previewCancel;

    private static final int FRESHIMG = 0x10;

    private static final int FRESHTIME = 0x11;

    int time = 30;

    private boolean isfirst;

    private TextView preivewTime;

    private CheckBox previewAutoHidden;

    public PreviewViewer(Context context, View view, BaseNetOperation netOperation) {
        pd = new AlertDialog.Builder(context).create();
        pd.setView(view);
        previewImage = (ImageView) view.findViewById(R.id.previewimg);
        previewImage.setImageResource(android.R.drawable.progress_indeterminate_horizontal);
        preivewTime = (TextView) view.findViewById(R.id.previewsecond);
        previewAutoHidden = (CheckBox) view.findViewById(R.id.previewautohidden);
        previewrefresh = (Button) view.findViewById(R.id.previewcontinue);
        previewrefresh.setOnClickListener(new OnClickListener() {

            @Override
            public void onClick(View v) {
                onRefresh();
            }
        });
        previewCancel = (Button) view.findViewById(R.id.previewcancel);
        previewCancel.setOnClickListener(new OnClickListener() {

            @Override
            public void onClick(View v) {
                onStop();
            }
        });
        code = null;
        mNetoperation = netOperation;
        this.context = context;
        handler = new Handler() {

            @Override
            public void handleMessage(Message msg) {
                if (msg.what == FRESHIMG) {
                    refreshImage((Bitmap) msg.obj);
                } else if (msg.what == FRESHTIME) {
                    refreshTime(msg.arg1);
                }
            }
        };
    }

    protected void onRefresh() {
        if (thread.isAlive()) {
            time = 30;
            isfirst = true;
        } else {
            isfirst = true;
            onStart();
        }
    }

    protected void refreshTime(int time) {
        int ft;
        if (time < 0) {
            ft = 0;
        } else {
            ft = time;
        }
        preivewTime.setText(Integer.toString(ft));
    }

    protected void onStart() {
        thread = new Thread(this);
        thread.start();
    }

    protected void refreshImage(Bitmap bitmap) {
        previewImage.setImageBitmap(bitmap);
        previewImage.postInvalidate();
    }

    private Bitmap getBitmapFromUrl(String imgUrl) {
        URL url;
        Bitmap bitmap = null;
        try {
            url = new URL(imgUrl);
            InputStream is = url.openConnection().getInputStream();
            BufferedInputStream bis = new BufferedInputStream(is);
            bitmap = BitmapFactory.decodeStream(bis);
            bis.close();
        } catch (MalformedURLException e) {
            e.printStackTrace();
        } catch (IOException e) {
            e.printStackTrace();
        }
        return bitmap;
    }

    protected void onStop() {
        code = null;
        if (pd.isShowing()) {
            pd.dismiss();
        }
    }

    public void setTitle(String string) {
        pd.setTitle(string);
    }

    public void setCode(String string) {
        code = string;
    }

    public void setmNetoperation(BaseNetOperation mNetoperation) {
        this.mNetoperation = mNetoperation;
    }

    public void show() {
        if (code != null) {
            pd.show();
            isfirst = true;
            onStart();
        }
    }

    @Override
    public void run() {
        time = 30;
        while (code != null && time >= 0) {
            Message msg1 = new Message();
            msg1.what = FRESHTIME;
            msg1.arg1 = time;
            handler.sendMessage(msg1);
            if (time % 2 == 0) {
                Message msg = new Message();
                msg.what = FRESHIMG;
                msg.obj = getBitmap();
                handler.sendMessage(msg);
            }
            time--;
            try {
                Thread.sleep(1000);
            } catch (InterruptedException e) {
                e.printStackTrace();
            }
        }
        if (previewAutoHidden.isChecked()) {
            onStop();
        }
    }

    private Bitmap getBitmap() {
        NetOperationParam param = new NetOperationParam();
        param.setmNOCode(INetOperation.GET_PREVIEW);
        param.addParam(code);
        param.addParam("");
        param.addParam("");
        param.addParam("0.5");
        if (isfirst) {
            param.addParam("30");
            param.addParam("1");
            isfirst = false;
        } else {
            param.addParam("");
            param.addParam("");
        }
        PreviewImageEntity entity = (PreviewImageEntity) mNetoperation.start(param);
        return entity.getBitmap();
    }
}
