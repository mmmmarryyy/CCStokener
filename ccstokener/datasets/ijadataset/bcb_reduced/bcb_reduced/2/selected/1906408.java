package com.baozou.app.activity;

import java.io.IOException;
import java.net.MalformedURLException;
import java.net.URL;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.os.AsyncTask;
import android.os.Bundle;
import android.os.Handler;
import android.os.Message;
import android.util.Log;
import android.view.MotionEvent;
import android.view.View;
import android.view.View.OnTouchListener;
import android.view.animation.AnimationUtils;
import android.widget.ImageView;
import android.widget.ProgressBar;
import android.widget.RelativeLayout;
import android.widget.TextView;
import android.widget.Toast;
import android.widget.ViewFlipper;
import android.widget.ZoomControls;
import com.baozou.R;
import com.baozou.app.robot.LatestRobot;
import com.baozou.app.util.AppParams;
import com.baozou.app.view.DragImageView;
import com.baozou.framework.base.BaseActivity;
import com.baozou.framework.util.SdFileUtil;

public class LatestActivity extends BaseActivity {

    public static final String D = "LatestActivity";

    RelativeLayout loading;

    RelativeLayout main;

    ViewFlipper flipper;

    ZoomControls zc;

    DragImageView iv;

    TextView page;

    ProgressBar pb;

    TextView textLoading;

    GroupActivity parent;

    List<Map<String, String>> imgs = new ArrayList<Map<String, String>>();

    LatestRobot robot = null;

    LatestActivity latestActivity = this;

    SdFileUtil fileUtil = null;

    int menuType = 0;

    String startUrl = "";

    String endUrl = "";

    int totalPage = 0;

    String folder = "";

    MyHandler handler = null;

    Runnable runnable = null;

    public void init() {
        switch(menuType) {
            case 1:
                startUrl = AppParams.latestStartUrl;
                endUrl = AppParams.latestEndUrl;
                totalPage = AppParams.latestTotalPage;
                folder = "latest/";
                break;
            case 2:
                startUrl = AppParams.todayStartUrl;
                endUrl = AppParams.todayEndUrl;
                totalPage = AppParams.todayTotalPage;
                folder = "today/";
                break;
            case 3:
                startUrl = AppParams.yesterdayStartUrl;
                endUrl = AppParams.yesterdayEndUrl;
                totalPage = AppParams.yesterdayTotalPage;
                folder = "yesterday/";
                break;
            case 4:
                startUrl = AppParams.weekStartUrl;
                endUrl = AppParams.weekEndUrl;
                totalPage = AppParams.weekTotalPage;
                folder = "week/";
                break;
            case 5:
                startUrl = AppParams.monthStartUrl;
                endUrl = AppParams.monthEndUrl;
                totalPage = AppParams.monthTotalPage;
                folder = "month/";
                break;
            case 6:
                startUrl = AppParams.yearStartUrl;
                endUrl = AppParams.yearEndUrl;
                totalPage = AppParams.yearTotalPage;
                folder = "year/";
                break;
        }
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        Log.d(D, "onCreate");
        setContentView(R.layout.latest);
        flipper = (ViewFlipper) findViewById(R.id.latest_flipper);
        main = (RelativeLayout) findViewById(R.id.latest_main);
        loading = (RelativeLayout) findViewById(R.id.latest_loading);
        zc = (ZoomControls) findViewById(R.id.latest_zoom);
        pb = (ProgressBar) findViewById(R.id.latest_progressbar);
        textLoading = (TextView) findViewById(R.id.latest_text_loading);
        parent = (GroupActivity) this.getParent();
        menuType = parent.getMenuType();
        page = parent.getPage();
        init();
        zc.setIsZoomOutEnabled(true);
        zc.setIsZoomOutEnabled(true);
        zc.setOnZoomInClickListener(new ZoomControls.OnClickListener() {

            @Override
            public void onClick(View v) {
                Log.d(D, "zoomIn");
                iv.zoomIn();
            }
        });
        zc.setOnZoomOutClickListener(new ZoomControls.OnClickListener() {

            @Override
            public void onClick(View v) {
                Log.d(D, "zoomOut");
                iv.zoomOut();
            }
        });
        Log.d(D, "Thread id : " + Thread.currentThread().getId());
        runnable = new Runnable() {

            public void run() {
                while (true) {
                    if (parent.getWhIsOk()) break; else try {
                        Thread.sleep(200);
                    } catch (InterruptedException e) {
                        e.printStackTrace();
                    }
                }
                int index = 0;
                Log.d(D, "Thread id : " + Thread.currentThread().getId());
                if (fileUtil == null) {
                    fileUtil = new SdFileUtil(latestActivity, folder);
                    handler.updateProgressBar(10);
                }
                if (robot == null) {
                    robot = new LatestRobot(startUrl, endUrl, totalPage, handler);
                } else {
                    Log.d(D, "robot.nextPage()");
                    index = imgs.size();
                    robot.nextPage();
                }
                List<String> imgUrls = robot.getImgsFromDoc();
                for (String imgUrl : imgUrls) {
                    Map<String, String> map = new HashMap<String, String>();
                    map.put("url", imgUrl);
                    map.put("isOk", "n");
                    imgs.add(map);
                }
                handler.updateProgressBar(100, index);
            }
        };
        handler = new MyHandler();
        new Thread(runnable).start();
    }

    public class LoadImgAsyncTask extends AsyncTask<Integer, Object, Boolean> {

        @Override
        protected Boolean doInBackground(Integer... params) {
            int index = params[0];
            for (int i = index; i < imgs.size(); i++) {
                String fileName = getFileName(imgs.get(i).get("url"));
                boolean isOk = false;
                try {
                    isOk = fileUtil.createImg(fileName, new URL(imgs.get(i).get("url")).openStream());
                } catch (MalformedURLException e) {
                    e.printStackTrace();
                } catch (IOException e) {
                    e.printStackTrace();
                } finally {
                    imgs.get(i).put("isOk", isOk ? "y" : "n");
                }
                publishProgress(i);
            }
            return true;
        }

        @Override
        protected void onProgressUpdate(Object... values) {
            super.onProgressUpdate(values);
            int index = (Integer) values[0];
            if (index == flipper.getDisplayedChild()) {
                beforeDisplay(index);
            }
        }

        @Override
        protected void onPostExecute(Boolean result) {
            super.onPostExecute(result);
        }
    }

    public DragImageView getDragImageView() {
        iv = new DragImageView(this, parent.getContentWidth(), parent.getContentHeight());
        iv.setScaleType(ImageView.ScaleType.MATRIX);
        iv.setOnTouchListener(new OnTouchListener() {

            @Override
            public boolean onTouch(View v, MotionEvent e) {
                switch(e.getAction() & MotionEvent.ACTION_MASK) {
                    case MotionEvent.ACTION_DOWN:
                        iv.down(e.getX(), e.getY());
                        break;
                    case MotionEvent.ACTION_UP:
                        iv.up(e.getX(), e.getY());
                        break;
                    case MotionEvent.ACTION_MOVE:
                        iv.drag(e.getX(), e.getY());
                        break;
                }
                return true;
            }
        });
        iv.setDragImageViewClick(iv.new DragImageViewClick() {

            @Override
            public void next() {
                int i = flipper.getDisplayedChild();
                if (i != imgs.size() - 1) {
                    displayNext();
                } else {
                    if (i + 1 < 8 * robot.totalPage) Toast.makeText(LatestActivity.this, "��һ�ȣ��ﵶ��٣����ڻ�������ͼƬ�ء�����", Toast.LENGTH_SHORT).show(); else Toast.makeText(LatestActivity.this, "����ûͼŶ������", Toast.LENGTH_SHORT).show();
                }
                if (i + 3 == imgs.size()) {
                    if (robot.docIsOk) new Thread(runnable).start();
                }
            }

            @Override
            public void previous() {
                if (flipper.getDisplayedChild() != 0) {
                    displayPrevious();
                } else {
                    Toast.makeText(LatestActivity.this, "ǰ��ûͼŶ������", Toast.LENGTH_SHORT).show();
                }
            }
        });
        return iv;
    }

    public void beforeDisplay(int index) {
        iv.recycleBitmap();
        String path = fileUtil.readImgPath(getFileName(imgs.get(index).get("url")));
        if (path != null && imgs.get(index).get("isOk") != null && imgs.get(index).get("isOk").equals("y")) {
            Bitmap b = BitmapFactory.decodeFile(path);
            DragImageView div = (DragImageView) flipper.getChildAt(index);
            div.setNewBitmap(b);
        }
        display(index);
    }

    public void displayNext() {
        int index = flipper.getDisplayedChild() + 1;
        beforeDisplay(index);
    }

    public void displayPrevious() {
        int index = flipper.getDisplayedChild() - 1;
        beforeDisplay(index);
    }

    public void display(int index) {
        flipper.setInAnimation(AnimationUtils.loadAnimation(LatestActivity.this, android.R.anim.fade_in));
        flipper.setOutAnimation(AnimationUtils.loadAnimation(LatestActivity.this, android.R.anim.fade_out));
        flipper.setDisplayedChild(index);
        iv = (DragImageView) flipper.getCurrentView();
        page.setText("Page " + (flipper.getDisplayedChild() + 1) + ".");
    }

    public String getFileName(String str) {
        return str.substring(str.lastIndexOf("/") + 1);
    }

    public class MyHandler extends Handler {

        public MyHandler() {
            super();
        }

        @Override
        public void handleMessage(Message msg) {
            switch(msg.what) {
                case 1:
                    pb.incrementProgressBy(1);
                    pb.setProgress(msg.arg1);
                    textLoading.setText("���س�Ҫ���ģ����ڻ��ټ��أ�" + msg.arg1 + "%");
                    break;
                case 2:
                    pb.incrementProgressBy(1);
                    pb.setProgress(msg.arg1);
                    textLoading.setText("���س�Ҫ���ģ����ڻ��ټ��أ�" + msg.arg1 + "%");
                    for (int i = msg.arg2; i < imgs.size(); i++) {
                        flipper.addView(getDragImageView());
                    }
                    iv = (DragImageView) flipper.getCurrentView();
                    loading.setVisibility(View.GONE);
                    main.setVisibility(View.VISIBLE);
                    new LoadImgAsyncTask().execute(msg.arg2);
                    break;
            }
        }

        public void updateProgressBar(int... i) {
            if (i[0] == 100) {
                Message msg = handler.obtainMessage();
                msg.what = 2;
                msg.arg1 = i[0];
                msg.arg2 = i[1];
                this.sendMessage(msg);
            } else {
                Message msg = handler.obtainMessage();
                msg.what = 1;
                msg.arg1 = i[0];
                this.sendMessage(msg);
            }
        }
    }
}
