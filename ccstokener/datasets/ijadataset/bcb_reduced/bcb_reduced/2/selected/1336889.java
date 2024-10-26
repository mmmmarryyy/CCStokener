package com.twilight.SofaStream;

import java.io.BufferedInputStream;
import java.io.File;
import java.io.FileOutputStream;
import java.io.FileWriter;
import java.io.IOException;
import java.io.InputStream;
import java.net.Authenticator;
import java.net.PasswordAuthentication;
import java.net.URLConnection;
import java.util.ArrayList;
import java.util.Currency;
import java.util.HashMap;
import org.apache.http.Header;
import org.apache.http.HttpEntity;
import org.apache.http.HttpResponse;
import org.apache.http.client.ClientProtocolException;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.impl.client.DefaultHttpClient;
import android.app.Notification;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.app.Service;
import android.content.ContentValues;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.graphics.drawable.BitmapDrawable;
import android.media.MediaPlayer;
import android.media.MediaPlayer.OnBufferingUpdateListener;
import android.media.MediaPlayer.OnCompletionListener;
import android.media.MediaPlayer.OnErrorListener;
import android.net.Uri;
import android.os.Environment;
import android.os.Handler;
import android.os.HandlerThread;
import android.os.IBinder;
import android.os.Looper;
import android.os.Message;
import android.os.RemoteCallbackList;
import android.os.RemoteException;
import android.os.StatFs;
import android.preference.PreferenceManager;
import android.text.format.Formatter;
import android.util.Log;
import org.apache.http.impl.client.DefaultHttpClient;
import org.apache.http.message.BasicHeader;
import org.apache.http.message.BasicHttpResponse;

public class DownloadService extends Service {

    private NotificationManager nm;

    private static final int DOWNLOADING_NOTIFY_ID = 0;

    public static final int STOP_DOWNLOAD = 0;

    public static final int QUEUED_DOWNLOAD = 0;

    public static final int START_DOWNLOAD = 1;

    public static final int DOWNLOADING = 2;

    public static final int DOWNLOADED = 3;

    public static final int DOWNLOAD_ERROR = 4;

    public static final int DOWNLOAD_STOPPED = 5;

    private RemoteCallbackList<IDownloadServiceCallback> mCallbacks = new RemoteCallbackList<IDownloadServiceCallback>();

    private ArrayList<RSSItem> mDownloads = new ArrayList<RSSItem>();

    private RSSItem mCurrent = null;

    URLConnection mConnection;

    DefaultHttpClient mClient;

    public static String storagePath = "/sofastream/episodes/";

    private String mErrorString = "";

    private downloadThread mDownloadThread;

    boolean stopFlag = false;

    Handler mUpdateHandler = new Handler() {

        public void handleMessage(Message msg) {
            RSSItem item = (RSSItem) msg.obj;
            switch(msg.what) {
                case START_DOWNLOAD:
                    postNotification(true, "Downloading " + item.title);
                    processCallbacks(msg.what, msg.arg1, item);
                    break;
                case DOWNLOADING:
                    processCallbacks(msg.what, msg.arg1, item);
                    break;
                case DOWNLOAD_ERROR:
                    postNotification(true, "Error downloading " + item.title);
                    processCallbacks(msg.what, msg.arg1, item);
                    break;
                case DOWNLOAD_STOPPED:
                    postNotification(true, "Stopped downloading " + item.title);
                    processCallbacks(msg.what, msg.arg1, item);
                    break;
                case DOWNLOADED:
                    postNotification(true, "Finshed downloading " + item.title);
                    processCallbacks(msg.what, msg.arg1, item);
                    break;
                default:
                    super.handleMessage(msg);
            }
        }
    };

    public void postNotification(boolean ticker, String mesg) {
        Notification notification = new Notification(R.drawable.icon, null, System.currentTimeMillis());
        if (ticker) notification.tickerText = mesg;
        notification.icon = R.drawable.icon;
        PendingIntent contentIntent = PendingIntent.getActivity(this, 0, new Intent(this, EpisodeList.class), 0);
        notification.setLatestEventInfo(this, "SofaStream", mesg, contentIntent);
        nm.notify(DOWNLOADING_NOTIFY_ID, notification);
    }

    @Override
    public void onCreate() {
        super.onCreate();
        nm = (NotificationManager) getSystemService(NOTIFICATION_SERVICE);
    }

    @Override
    public void onDestroy() {
        mCallbacks.kill();
    }

    private boolean downloadFile(long id) {
        RSSDB db = new RSSDB(this);
        RSSItem article = db.getArticle(id);
        if (article.remotefile == null) {
            mErrorString = "This article has no download.";
            return false;
        }
        mDownloads.add(article);
        ContentValues values = new ContentValues();
        values.put("downloadstate", RSSItem.downloadstates.QUEUED.ordinal());
        db.updateArticle(id, values);
        postNotification(true, "Queued: " + article.title);
        try {
            startDownload();
            return true;
        } catch (DownloadException e) {
            e.printStackTrace();
            mErrorString = e.getMessage();
            return false;
        }
    }

    private boolean startDownload() throws DownloadException {
        if (mCurrent != null) return false;
        if (mDownloads.size() < 1) return false;
        mDownloadThread = new downloadThread(this);
        mDownloadThread.start();
        return true;
    }

    public boolean cancelDownload(long id) {
        if (mCurrent != null && mCurrent.id == id) {
            stopFlag = true;
        } else {
            for (int i = 0; i < mDownloads.size(); i++) {
                if (mDownloads.get(i).id == id) {
                    mDownloads.remove(i);
                    break;
                }
            }
        }
        RSSDB db = new RSSDB(this);
        ContentValues values = new ContentValues();
        values.put("downloadstate", RSSItem.downloadstates.UNDOWNLOADED.ordinal());
        db.updateArticle(id, values);
        return true;
    }

    public boolean deleteDownload(long id) {
        RSSDB db = new RSSDB(this);
        RSSItem article = db.getArticle(id);
        String storageFS = Environment.getExternalStorageDirectory().toString();
        File f = new File(storageFS + storagePath + article.localfile);
        f.delete();
        ContentValues values = new ContentValues();
        values.put("downloadstate", RSSItem.downloadstates.UNDOWNLOADED.ordinal());
        db.updateArticle(id, values);
        return true;
    }

    public void deleteDownloads() {
        String storageFS = Environment.getExternalStorageDirectory().toString();
        File f = new File(storageFS + storagePath);
        File[] files = f.listFiles();
        for (int i = 0; i < files.length; i++) files[i].delete();
    }

    private void processCallbacks(int type, int progress, RSSItem item) {
        final int N = mCallbacks.beginBroadcast();
        for (int i = 0; i < N; i++) {
            try {
                mCallbacks.getBroadcastItem(i).update(type, item.id, progress);
            } catch (RemoteException e) {
            }
        }
        mCallbacks.finishBroadcast();
    }

    public final class downloadThread extends HandlerThread {

        Context c;

        Handler mHandler;

        public downloadThread(Context context) {
            super("downloadThread");
            c = context;
        }

        public void run() {
            Looper.prepare();
            RSSDB db = new RSSDB(c);
            ContentValues values = new ContentValues();
            while (mDownloads.size() > 0) {
                try {
                    try {
                        mCurrent = mDownloads.get(0);
                    } catch (IndexOutOfBoundsException e) {
                        throw new DownloadException("Error collecting next queued download.");
                    }
                    RSSFeed feed = db.getFeed(mCurrent.feedId);
                    if (feed.secure) {
                        SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(DownloadService.this);
                        String feedId = String.format("feed_%d", feed.id);
                        String username = prefs.getString(String.format("%s_username", feedId.toString()), "");
                        String password = prefs.getString(String.format("%s_password", feedId.toString()), "");
                        mClient = new DownloadHttpClient(username, password);
                    } else {
                        mClient = new DefaultHttpClient();
                    }
                    HttpGet GET = new HttpGet(mCurrent.remotefile.toString());
                    BasicHttpResponse response = (BasicHttpResponse) mClient.execute(GET);
                    if (response.getStatusLine().getStatusCode() == 401) {
                        throw new DownloadException("Unathorised access - check your username and password.");
                    } else if (response.getStatusLine().getStatusCode() != 200) {
                        throw new DownloadException("Error connecting to server.");
                    }
                    HttpEntity entity = response.getEntity();
                    long fileSize = entity.getContentLength();
                    String[] fragments = mCurrent.remotefile.getFile().split("/");
                    String filename = fragments[fragments.length - 1];
                    String storageState = Environment.getExternalStorageState();
                    if (storageState.compareTo("mounted") != 0) {
                        throw new DownloadException("No external storage available: " + storageState);
                    }
                    String storageFS = Environment.getExternalStorageDirectory().toString();
                    StatFs storageStats = new StatFs(storageFS);
                    int blockSize = storageStats.getBlockSize();
                    int availableBlocks = storageStats.getAvailableBlocks();
                    int availableSize = blockSize * availableBlocks;
                    if (availableSize < fileSize) {
                        throw new DownloadException(String.format("File is %s but external storage only has %s available.", readableSize(fileSize), readableSize(availableSize)));
                    }
                    File path = new File(storageFS + storagePath);
                    path.mkdirs();
                    File f = new File(storageFS + storagePath + filename);
                    values.put("downloadstate", RSSItem.downloadstates.DOWNLOADING.ordinal());
                    db.updateArticle(mCurrent.id, values);
                    postMessage(START_DOWNLOAD, mCurrent, 0);
                    try {
                        assert (f.createNewFile() == true);
                        FileOutputStream out = new FileOutputStream(f);
                        InputStream is = entity.getContent();
                        BufferedInputStream bi = new BufferedInputStream(is);
                        byte[] b = new byte[10000];
                        int result, percent;
                        long index = 0;
                        long time = System.currentTimeMillis();
                        while (true) {
                            if (stopFlag) {
                                stopFlag = false;
                                throw new DownloadStoppedException();
                            }
                            result = bi.read(b);
                            if (result == -1) break;
                            index += result;
                            if (time < System.currentTimeMillis() + 3000) {
                                time = System.currentTimeMillis();
                                percent = (int) (index * 100 / fileSize);
                            }
                            out.write(b, 0, result);
                        }
                        entity.consumeContent();
                        bi.close();
                        is.close();
                    } catch (IOException e) {
                        throw new DownloadException("Failed to download file.");
                    }
                    values.put("downloadstate", RSSItem.downloadstates.DOWNLOADED.ordinal());
                    values.put("localfile", filename);
                    db.updateArticle(mCurrent.id, values);
                    postMessage(DOWNLOADED, mCurrent, 0);
                } catch (DownloadException e1) {
                    e1.printStackTrace();
                    mErrorString = e1.getMessage();
                    postMessage(DOWNLOAD_ERROR, mCurrent, 0);
                    values.put("downloadstate", RSSItem.downloadstates.FAILED.ordinal());
                    db.updateArticle(mCurrent.id, values);
                } catch (DownloadStoppedException e1) {
                    postMessage(DOWNLOAD_STOPPED, mCurrent, 0);
                    values.put("downloadstate", RSSItem.downloadstates.UNDOWNLOADED.ordinal());
                    db.updateArticle(mCurrent.id, values);
                } catch (ClientProtocolException e1) {
                    e1.printStackTrace();
                    mErrorString = e1.getMessage();
                    postMessage(DOWNLOAD_ERROR, mCurrent, 0);
                    db.updateArticle(mCurrent.id, values);
                } catch (IOException e1) {
                    e1.printStackTrace();
                    mErrorString = e1.getMessage();
                    postMessage(DOWNLOAD_ERROR, mCurrent, 0);
                    db.updateArticle(mCurrent.id, values);
                }
                mDownloads.remove(0);
            }
            mCurrent = null;
            nm.cancel(DOWNLOADING_NOTIFY_ID);
        }

        private void postMessage(int type, Object obj, int progress) {
            Handler daddyHandler = DownloadService.this.mUpdateHandler;
            Message m = daddyHandler.obtainMessage(type, progress, 0, obj);
            daddyHandler.sendMessage(m);
        }
    }

    public IBinder onBind(Intent intent) {
        return mBinder;
    }

    /**
     * The IRemoteInterface is defined through IDL
     */
    private final IDownloadService.Stub mBinder = new IDownloadService.Stub() {

        public void registerCallback(IDownloadServiceCallback cb) {
            if (cb != null) mCallbacks.register(cb);
        }

        public void unregisterCallback(IDownloadServiceCallback cb) {
            if (cb != null) mCallbacks.unregister(cb);
        }

        public boolean download(long id) throws RemoteException {
            return downloadFile(id);
        }

        public boolean stop_download(long id) throws RemoteException {
            return cancelDownload(id);
        }

        public boolean delete_download(long id) throws RemoteException {
            return deleteDownload(id);
        }

        public String getError() {
            return mErrorString;
        }

        public long currentDownloadId() {
            if (mCurrent != null) {
                return mCurrent.id;
            } else {
                return -1;
            }
        }

        public void clearDownloads() {
            DownloadService.this.deleteDownloads();
        }
    };

    private String readableSize(long value) {
        return Formatter.formatFileSize(this, value);
    }
}
