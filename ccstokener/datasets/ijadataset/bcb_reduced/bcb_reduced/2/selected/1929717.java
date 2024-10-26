package com.linktone.market.client.service;

import android.app.Notification;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.app.Service;
import android.content.Intent;
import android.os.IBinder;
import com.linktone.market.client.R;
import com.linktone.market.client.activity.ActivityMain;
import com.linktone.market.client.bean.AppInfo;
import com.linktone.market.client.database.MarketDatabase;
import java.io.BufferedInputStream;
import java.io.File;
import java.io.RandomAccessFile;
import java.net.URL;
import java.net.URLConnection;
import java.util.*;

/**
 * @author mxf <a href="mailto:maxuefengs@gmail.com">mxf</a>
 *         11-8-19 ����10:22
 * @since version 1.0
 */
public class DownloadService extends Service {

    public static Map<Integer, Thread> downLoadingMap = new HashMap<Integer, Thread>();

    public static Set<Integer> pauseDownloadSet = new HashSet<Integer>();

    public static final String ACTION_DOWNLOAD = "action_download";

    public static final String ACTION_RESUME = "action_resume";

    public static final String ACTION_PAUSE = "action_pause";

    public static final String ACTION_NOTIFY_START = "action_notify_start";

    public static final String ACTION_NOTIFY_PROGRESS = "action_notify_progress";

    public static final String ACTION_NOTIFY_FINISH = "action_notify_finish";

    public static final String ACTION_NOTIFY_INTERRUPT = "action_notify_interrupt";

    public static final String ACTION_NOTIFY_DELETE = "action_notify_delete";

    MarketDatabase mdb;

    public IBinder onBind(Intent intent) {
        return null;
    }

    @Override
    public void onCreate() {
        super.onCreate();
        mdb = MarketDatabase.getInstance(this);
    }

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        int id = intent.getIntExtra("id", 0);
        if (intent.getAction().equals(ACTION_DOWNLOAD)) {
            AppInfo info = mdb.getAppInfo(id);
            File f = new File(info.getLocalPath());
            long start = f.length();
            if (downLoadingMap.get(id) == null) {
                Thread t = new Thread(new DownloadThread(start, info));
                downLoadingMap.put(info.getId(), t);
                t.start();
                Intent it = new Intent(ACTION_NOTIFY_START);
                it.putExtra("id", info.getId());
                DownloadService.this.sendBroadcast(it);
                pauseDownloadSet.remove(id);
            }
        } else if (intent.getAction().equals(ACTION_PAUSE)) {
            if (downLoadingMap.get(id) != null) {
                downLoadingMap.get(id).interrupt();
                downLoadingMap.remove(id);
                pauseDownloadSet.add(id);
            }
        } else if (intent.getAction().equals(ACTION_RESUME)) {
            AppInfo info = mdb.getAppInfo(id);
            File f = new File(info.getLocalPath());
            long start = f.length();
            if (downLoadingMap.get(info.getId()) == null) {
                Thread t = new Thread(new DownloadThread(start, info));
                downLoadingMap.put(info.getId(), t);
                t.start();
                pauseDownloadSet.remove(id);
            }
        }
        NotificationManager manager = (NotificationManager) this.getSystemService(NOTIFICATION_SERVICE);
        Intent it = new Intent(this, ActivityMain.class);
        PendingIntent pi = PendingIntent.getActivity(this, 0, it, 0);
        String mes;
        if (downLoadingMap.size() > 0) {
            mes = "market:" + downLoadingMap.size() + "������������";
        } else if (pauseDownloadSet.size() > 0) {
            mes = "market:" + pauseDownloadSet.size() + "��������ͣ��";
        } else {
            mes = "market:����������";
            this.stopSelf();
        }
        Notification nf = new Notification(R.drawable.icon, mes, System.currentTimeMillis());
        nf.setLatestEventInfo(this, "���������Ϣ", "�����鿴�����������", pi);
        manager.notify(0, nf);
        return super.onStartCommand(intent, flags, startId);
    }

    @Override
    public void onDestroy() {
        mdb.close();
        super.onDestroy();
    }

    class DownloadThread implements Runnable {

        long start;

        long count;

        AppInfo info;

        Timer timer = new Timer();

        public DownloadThread(long start, AppInfo info) {
            this.start = start;
            this.info = info;
            count = start;
        }

        public void run() {
            try {
                URL url = new URL(info.getUrl());
                URLConnection conn = url.openConnection();
                conn.setRequestProperty("Range", "bytes=" + start + "-");
                if (info.getTotalSize() == 0) {
                    info.setTotalSize(conn.getContentLength());
                }
                BufferedInputStream bis = new BufferedInputStream(conn.getInputStream());
                RandomAccessFile raf = new RandomAccessFile(info.getLocalPath(), "rw");
                raf.seek(start);
                int c = 0;
                byte[] b = new byte[1024 * 10];
                timer.schedule(new TimerTask() {

                    @Override
                    public void run() {
                        int percent = (Double.valueOf((count * 1.0 / info.getTotalSize() * 100))).intValue();
                        Intent intent = new Intent(ACTION_NOTIFY_PROGRESS);
                        intent.putExtra("id", info.getId());
                        intent.putExtra("percent", percent);
                        DownloadService.this.sendBroadcast(intent);
                    }
                }, new Date(), 2000);
                while ((c = bis.read(b)) != -1 && !Thread.currentThread().isInterrupted()) {
                    raf.write(b, 0, c);
                    count += c;
                }
                timer.cancel();
                downLoadingMap.remove(info.getId());
                if (!Thread.currentThread().isInterrupted()) {
                    mdb.updateDowloadApp(info.getId(), AppInfo.WAIT_INSTALL);
                    Intent intent = new Intent(ACTION_NOTIFY_FINISH);
                    intent.putExtra("id", info.getId());
                    intent.putExtra("path", info.getLocalPath());
                    DownloadService.this.sendBroadcast(intent);
                } else {
                    Intent intent = new Intent(ACTION_NOTIFY_INTERRUPT);
                    intent.putExtra("id", info.getId());
                    DownloadService.this.sendBroadcast(intent);
                }
            } catch (Exception e) {
                timer.cancel();
                downLoadingMap.remove(info.getId());
                Intent intent = new Intent(ACTION_NOTIFY_INTERRUPT);
                intent.putExtra("id", info.getId());
                DownloadService.this.sendBroadcast(intent);
                e.printStackTrace();
                System.out.println(e.toString());
            }
        }
    }
}
