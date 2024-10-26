package com.tx.util;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.net.HttpURLConnection;
import java.net.URL;
import java.util.zip.ZipException;
import android.app.Activity;
import android.app.ProgressDialog;
import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.os.Environment;
import android.os.Handler;
import android.os.Message;
import android.util.Log;
import com.tx.bean.App;
import com.tx.db.AppDao;
import com.tx.http.util.HttpUtil;

/**
 * 进度条工具类
 * @author Crane
 *
 */
public class ProgressThread implements Runnable {

    private static String TAG = "ProgressThread";

    public static final int CONNECT_TIME = 5000;

    public static final int PROGRESS_MAX = 100;

    private int progressValue = 0;

    public int getProgressValue() {
        return progressValue;
    }

    private App app;

    public boolean isRun = true;

    public int type = 1;

    private String downloadUrl;

    private String fileName;

    private String fileExName;

    ProgressDialog progressDialog;

    Activity activity;

    /**
	 * 
	 * @param h
	 *            处理线程的句柄
	 * @param d
	 *            进度条加减的标志位
	 * @param paraMap
	 *            参数Map
	 * @param resultMap
	 *            返回结果集Map
	 * @param activity
	 *            显示进度条的画面
	 */
    public ProgressThread(Activity activity, ProgressDialog progressDialog, App app) {
        this.activity = activity;
        this.progressDialog = progressDialog;
        this.app = app;
        downloadUrl = app.getAddr();
        fileName = downloadUrl.substring(downloadUrl.lastIndexOf(File.separator) + 1);
        fileExName = fileName.substring(fileName.indexOf(".") + 1);
    }

    public void run() {
        handler.obtainMessage();
        while (isRun) {
            try {
                Thread.sleep(100);
            } catch (InterruptedException e) {
                e.printStackTrace();
            }
            uploadZip();
            if (progressValue == 80) {
                unZip();
            }
            new AppDao(activity).updateAppInstState(app.getAppId());
        }
    }

    /**
	 * 下载内容处理。
	 * 
	 * 
	 */
    public void uploadZip() {
        URL url = null;
        HttpURLConnection conn = null;
        InputStream is = null;
        FileOutputStream fos = null;
        int responseCode = 0;
        byte[] data = null;
        File dirs = null;
        File file = null;
        if (isRun) {
            try {
                sendMsg(progressValue);
                Thread.sleep(10);
                while (progressValue < 10) {
                    progressValue++;
                    sendMsg(progressValue);
                }
                url = new URL(downloadUrl);
                conn = (HttpURLConnection) url.openConnection();
                conn.setConnectTimeout(CONNECT_TIME);
                responseCode = conn.getResponseCode();
                is = conn.getInputStream();
                while (progressValue < 20) {
                    progressValue++;
                    sendMsg(progressValue);
                }
                if (responseCode == HttpURLConnection.HTTP_OK) {
                    data = HttpUtil.getByte(is);
                }
                while (progressValue < 25) {
                    progressValue++;
                    sendMsg(progressValue);
                }
                dirs = new File(UrlConfigUtil.APP_URL + File.separator + app.getEnName());
                while (progressValue < 30) {
                    progressValue++;
                    sendMsg(progressValue);
                }
                if (!dirs.exists()) {
                    dirs.mkdirs();
                }
                while (progressValue < 40) {
                    progressValue++;
                    sendMsg(progressValue);
                }
                file = new File(dirs, fileName);
                if (!file.exists()) {
                    boolean createNewFile = file.createNewFile();
                    if (createNewFile) {
                        Log.d("createNewFile", String.valueOf(createNewFile));
                    } else {
                        Log.d("createNewFile", String.valueOf(createNewFile));
                    }
                }
                while (progressValue < 50) {
                    progressValue++;
                    sendMsg(progressValue);
                }
                fos = new FileOutputStream(file);
                fos.write(data);
                while (progressValue < 60) {
                    progressValue++;
                    sendMsg(progressValue);
                }
                if (fos != null) {
                    try {
                        fos.flush();
                        fos.close();
                        fos = null;
                    } catch (IOException e) {
                        e.printStackTrace();
                    }
                }
                while (progressValue < 70) {
                    progressValue++;
                    sendMsg(progressValue);
                }
                if (is != null) {
                    try {
                        is.close();
                        is = null;
                    } catch (IOException e) {
                        e.printStackTrace();
                    }
                }
                while (progressValue < 80) {
                    progressValue++;
                    sendMsg(progressValue);
                }
            } catch (Exception e) {
                e.printStackTrace();
            }
        }
    }

    /**
	 * 解压缩文件类
	 * 
	 * @author Crane
	 * 
	 */
    public void unZip() {
        Intent intent = null;
        while (isRun) {
            while (progressValue < 90) {
                progressValue++;
                sendMsg(progressValue);
            }
            if (fileExName.equalsIgnoreCase("zip")) {
                intent = new Intent();
                intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
                intent.setAction(android.content.Intent.ACTION_VIEW);
                String downloadedDir = UrlConfigUtil.APP_URL + File.separator + app.getEnName() + fileName;
                while (progressValue < 95) {
                    progressValue++;
                    sendMsg(progressValue);
                }
                Uri uri = Uri.fromFile(new File(downloadedDir));
                intent.setDataAndType(uri, "application/vnd.android.package-archive");
                activity.setIntent(intent);
                while (progressValue < 100) {
                    try {
                        Thread.sleep(100);
                    } catch (InterruptedException e) {
                        e.printStackTrace();
                    }
                    progressValue++;
                    sendMsg(progressValue);
                }
                String targetDir = null;
                File zipFile = null;
                targetDir = UrlConfigUtil.APP_URL + File.separator + app.getEnName() + File.separator;
                zipFile = new File(targetDir + fileName);
                try {
                    ZipUtils.upZipFile(zipFile, targetDir);
                } catch (ZipException e) {
                    e.printStackTrace();
                } catch (IOException e) {
                    e.printStackTrace();
                }
            } else if (fileExName.equalsIgnoreCase("apk")) {
                String apkName = Environment.getExternalStorageDirectory() + fileName;
                Uri uri = Uri.fromFile(new File(apkName));
                Intent i = new Intent(Intent.ACTION_VIEW);
                i.setDataAndType(uri, "application/vnd.android.package-archive");
                activity.startActivity(i);
            } else {
            }
            isRun = false;
        }
    }

    /**
	 * 取消安装的处理。
	 */
    public void rollBackInstall() {
        progressDialog.dismiss();
        String dir = UrlConfigUtil.APP_URL + File.separator + app.getEnName();
        FileUtil.deleteAll(dir, true);
        isRun = false;
    }

    /**
	 * 继续安装的处理。
	 */
    public void continueInstall(ProgressThread progressThread) {
        new Thread(progressThread).start();
        type = 1;
        isRun = true;
    }

    private Handler handler = new Handler() {

        @Override
        public void handleMessage(Message msg) {
            progressValue = msg.getData().getInt("percent");
            Log.i(TAG, "PROGRESS VALUE IS:" + progressValue);
            if (progressValue == 100) {
                progressDialog.dismiss();
            } else {
                progressDialog.setProgress(progressValue);
            }
        }
    };

    private void sendMsg(int value) {
        Message message = new Message();
        Bundle data = message.getData();
        data.putInt("percent", value);
        message.setData(data);
        handler.sendMessage(message);
    }
}
