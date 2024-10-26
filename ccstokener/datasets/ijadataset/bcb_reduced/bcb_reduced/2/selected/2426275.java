package it.unicaradio.android.services;

import it.unicaradio.android.R;
import it.unicaradio.android.activities.StreamingActivity;
import it.unicaradio.android.events.OnInfoListener;
import it.unicaradio.android.gui.TrackInfos;
import it.unicaradio.android.streamers.IcecastStreamer;
import it.unicaradio.android.streamers.Streamer;
import java.io.IOException;
import java.net.MalformedURLException;
import java.net.URL;
import java.net.URLConnection;
import java.text.MessageFormat;
import android.app.Notification;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.app.Service;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.net.ConnectivityManager;
import android.os.Binder;
import android.os.IBinder;
import android.telephony.PhoneStateListener;
import android.telephony.TelephonyManager;
import android.util.Log;
import com.spoledge.aacdecoder.AACPlayer;

/**
 * @author Paolo Cortis
 */
public class StreamingService extends Service {

    public static final String ACTION_TRACK_INFO = "it.unicaradio.android.intent.action.TRACK_INFO";

    private static final String LOG = StreamingService.class.getName();

    private static final String STREAM_URL = "http://streaming.unicaradio.it:80/unica64.aac";

    private static final int NOTIFICATION_ID = 1;

    private final IBinder mBinder = new LocalBinder();

    private NotificationManager notificationManager;

    private URLConnection conn;

    private Streamer streamer;

    private AACPlayer player;

    private final TrackInfos infos = new TrackInfos();

    private boolean isPlaying;

    private String error = "";

    private final BroadcastReceiver connectivityBroadcastReceiver = new BroadcastReceiver() {

        @Override
        public void onReceive(Context context, Intent intent) {
            boolean noConnectivity = intent.getBooleanExtra(ConnectivityManager.EXTRA_NO_CONNECTIVITY, false);
            if (noConnectivity) {
                stop();
            }
        }
    };

    private final BroadcastReceiver telephonyBroadcastReceiver = new BroadcastReceiver() {

        @Override
        public void onReceive(Context context, Intent intent) {
            PhoneStateListener phoneListener = new PhoneStateListener() {

                @Override
                public void onCallStateChanged(int state, String incomingNumber) {
                    switch(state) {
                        case TelephonyManager.CALL_STATE_OFFHOOK:
                            stop();
                            break;
                        case TelephonyManager.CALL_STATE_RINGING:
                            stop();
                            break;
                    }
                }
            };
            TelephonyManager telephony = (TelephonyManager) context.getSystemService(Context.TELEPHONY_SERVICE);
            telephony.listen(phoneListener, PhoneStateListener.LISTEN_CALL_STATE);
        }
    };

    @Override
    public IBinder onBind(Intent intent) {
        return mBinder;
    }

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        notificationManager = (NotificationManager) getSystemService(Context.NOTIFICATION_SERVICE);
        return START_NOT_STICKY;
    }

    @Override
    public void onDestroy() {
        stop();
        super.onDestroy();
    }

    public boolean isPlaying() {
        return isPlaying;
    }

    public void play() {
        if (conn == null) {
            registerReceiver(connectivityBroadcastReceiver, new IntentFilter(ConnectivityManager.CONNECTIVITY_ACTION));
            registerReceiver(telephonyBroadcastReceiver, new IntentFilter(TelephonyManager.ACTION_PHONE_STATE_CHANGED));
            try {
                URL url = new URL(STREAM_URL);
                _play(url);
            } catch (MalformedURLException e) {
                stopWithException("Errore: l'indirizzo di streaming non è corretto.", e);
            } catch (IOException e) {
                stopWithException(e);
            }
        } else {
            stop();
        }
    }

    public void stop() {
        try {
            unregisterReceiver(connectivityBroadcastReceiver);
        } catch (IllegalArgumentException e) {
        }
        try {
            unregisterReceiver(telephonyBroadcastReceiver);
        } catch (IllegalArgumentException e) {
        }
        if (player != null) {
            player.stop();
            player = null;
            streamer = null;
            conn = null;
            isPlaying = false;
            infos.clean();
            notifyChange();
        }
        clearNotification();
    }

    private void _play(URL url) throws IOException {
        if (conn == null) {
            conn = url.openConnection();
            conn.addRequestProperty("Icy-MetaData", "1");
            conn.connect();
            streamer = new IcecastStreamer(conn);
            streamer.addOnInfoListener(new OnInfoListener() {

                @Override
                public void onInfo(TrackInfos trackInfos) {
                    infos.setTrackInfos(trackInfos);
                    notifyChange();
                }
            });
            player = new AACPlayer();
            Thread playThread = new Thread(new Runnable() {

                @Override
                public void run() {
                    try {
                        isPlaying = true;
                        player.play(streamer);
                    } catch (Exception e) {
                        stopWithException(e);
                    }
                }
            });
            playThread.start();
        }
    }

    private void stopWithException(String message, Throwable t) {
        error = MessageFormat.format("{0}: {1}", message, t.getMessage());
        stop();
        Log.d(LOG, message, t);
    }

    private void stopWithException(Exception e) {
        stopWithException("E' avvenuto un problema. Riprova.", e);
    }

    public void notifyChange() {
        Intent i = new Intent(ACTION_TRACK_INFO);
        i.putExtra("author", infos.getAuthor());
        i.putExtra("title", infos.getTitle());
        i.putExtra("isplaying", isPlaying);
        i.putExtra("error", error);
        sendBroadcast(i);
        if (!infos.isClean()) {
            if (infos.getTitle().equals("")) {
                sendNotification(infos.getCleanedAuthor(), "");
            } else {
                sendNotification(infos.getTitle(), infos.getCleanedAuthor());
            }
        }
        error = "";
    }

    private void clearNotification() {
        NotificationManager notificationManager = (NotificationManager) getSystemService(Context.NOTIFICATION_SERVICE);
        notificationManager.cancel(NOTIFICATION_ID);
    }

    private void sendNotification(String title, String message) {
        Intent intent = new Intent(this, StreamingActivity.class);
        PendingIntent pIntent = PendingIntent.getActivity(this, 0, intent, PendingIntent.FLAG_CANCEL_CURRENT);
        Notification notification = new Notification(R.drawable.logo, title, System.currentTimeMillis());
        notification.flags |= Notification.FLAG_NO_CLEAR;
        notification.setLatestEventInfo(this, title, message, pIntent);
        notificationManager.notify(NOTIFICATION_ID, notification);
    }

    public class LocalBinder extends Binder {

        public StreamingService getService() {
            return StreamingService.this;
        }
    }
}
