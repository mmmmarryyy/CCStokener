package com.webeclubbin.mynpr;

import java.io.BufferedInputStream;
import java.io.BufferedOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.net.HttpURLConnection;
import java.net.URL;
import java.net.URLConnection;
import java.util.Calendar;
import java.util.Vector;
import android.app.Activity;
import android.app.NotificationManager;
import android.app.Service;
import android.content.Context;
import android.content.Intent;
import android.media.AudioManager;
import android.media.MediaPlayer;
import android.os.Debug;
import android.os.IBinder;
import android.os.Process;
import android.telephony.PhoneStateListener;
import android.telephony.TelephonyManager;
import android.util.Log;

/**
 * MediaPlayer does not yet support "Shoutcast"-like streaming from external URLs so this class provides a pseudo-streaming function
 * by downloading the content incrementally & playing as soon as we get enough audio in our temporary storage.
 */
public class StreamingMediaPlayer extends Service {

    public static final String AUDIO_MPEG = "audio/mpeg";

    public static final String BITERATE_HEADER = "icy-br";

    private int INTIAL_KB_BUFFER;

    private final int BIT = 8;

    private final int SECONDS = 30;

    private File downloadingMediaFile;

    private final String DOWNFILE = "downloadingMediaFile";

    private int totalKbRead;

    private Context context;

    private int counter;

    private int playedcounter;

    private Vector<MediaPlayer> mediaplayers;

    private boolean started;

    private boolean processHasStarted;

    private boolean regularStream;

    private BufferedInputStream stream;

    private URL url;

    private URLConnection urlConn;

    private String station;

    private String audiourl;

    private Intent startingIntent = null;

    private boolean stopping;

    Thread preparringthread;

    boolean waitingForPlayer;

    LogtoFile ltf = null;

    private void setupVars() {
        totalKbRead = 0;
        counter = 0;
        playedcounter = 0;
        mediaplayers = new Vector<MediaPlayer>(3);
        started = false;
        processHasStarted = false;
        regularStream = false;
        stream = null;
        url = null;
        urlConn = null;
        station = null;
        audiourl = null;
        stopping = false;
        preparringthread = null;
        waitingForPlayer = false;
    }

    final PhoneStateListener myPhoneListener = new PhoneStateListener() {

        public void onCallStateChanged(int state, String incomingNumber) {
            String TAG = "PhoneStateListener";
            switch(state) {
                case TelephonyManager.CALL_STATE_RINGING:
                    Log.d(TAG, "Someone's calling. Let us stop the service");
                    sendMessage(PlayListTab.STOP);
                    break;
                case TelephonyManager.CALL_STATE_OFFHOOK:
                    break;
                case TelephonyManager.CALL_STATE_IDLE:
                    break;
                default:
                    Log.d(TAG, "Unknown phone state = " + state);
            }
        }
    };

    private final IStreamingMediaPlayer.Stub ourBinder = new IStreamingMediaPlayer.Stub() {

        String TAG = "IStreamingMediaPlayer.Stub";

        public String getStation() {
            Log.d(TAG, "getStation");
            return station;
        }

        public String getUrl() {
            Log.d(TAG, "getUrl");
            return audiourl;
        }

        public boolean playing() {
            Log.d(TAG, "playing?");
            return isPlaying();
        }

        public void startAudio() {
            Log.d(TAG, "startAudio");
            raiseThreadPriority();
            Runnable r = new Runnable() {

                public void run() {
                    onStart(startingIntent, 0);
                }
            };
            new Thread(r).start();
        }

        public void stopAudio() {
            Log.d(TAG, "stopAudio");
            stop();
        }
    };

    @Override
    public void onCreate() {
        super.onCreate();
        context = this;
        ltf = new LogtoFile(context);
        String TAG = "StreamingMediaPlayer - onCreate";
        Log.d(TAG, "START");
        Log.d(TAG, "Setup Phone listener");
        TelephonyManager tm = (TelephonyManager) getSystemService(TELEPHONY_SERVICE);
        tm.listen(myPhoneListener, PhoneStateListener.LISTEN_CALL_STATE);
    }

    @Override
    public void onStart(Intent intent, int startId) {
        super.onStart(intent, startId);
        final String TAG = "StreamingMediaPlayer - onStart";
        Log.d(TAG, "START");
        Log.d(TAG, "Setup Vars");
        setupVars();
        Log.d(TAG, "Intent: " + intent.getStringExtra(PlayListTab.URL));
        Log.d(TAG, "Station: " + intent.getStringExtra(PlayListTab.STATION));
        Log.d(TAG, "RegularStream: " + intent.getBooleanExtra(PlayListTab.REGULARSTREAM, false));
        processHasStarted = true;
        audiourl = intent.getStringExtra(PlayListTab.URL);
        station = intent.getStringExtra(PlayListTab.STATION);
        if (intent.getBooleanExtra(PlayListTab.REGULARSTREAM, false)) {
            regularStream = true;
        }
        Log.d(TAG, "Run startStreaming function");
        downloadingMediaFile = new File(context.getCacheDir(), DOWNFILE + counter);
        downloadingMediaFile.deleteOnExit();
        Runnable r = new Runnable() {

            public void run() {
                try {
                    startStreaming(audiourl);
                } catch (IOException e) {
                    Log.d(TAG, e.toString());
                }
            }
        };
        Thread t = new Thread(r);
        t.start();
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
        String TAG = "StreamingMediaPlayer - onDestroy";
        Log.d(TAG, "START");
    }

    @Override
    public IBinder onBind(Intent intent) {
        String TAG = "StreamingMediaPlayer - onBind";
        Log.d(TAG, "START");
        Log.d(TAG, "Intent: " + intent.getStringExtra(PlayListTab.URL));
        Log.d(TAG, "Station: " + intent.getStringExtra(PlayListTab.STATION));
        startingIntent = intent;
        context = this;
        return ourBinder;
    }

    /**  
     * Progressivly download the media to a temporary location and update the MediaPlayer as new content becomes available.
     */
    public void startStreaming(final String mediaUrl) throws IOException {
        final String TAG = "startStreaming";
        int bitrate = 56;
        sendMessage(PlayListTab.CHECKRIORITY);
        sendMessage(PlayListTab.RAISEPRIORITY);
        sendMessage(PlayListTab.START);
        try {
            url = new URL(mediaUrl);
            urlConn = (HttpURLConnection) url.openConnection();
            urlConn.setReadTimeout(1000 * 20);
            urlConn.setConnectTimeout(1000 * 5);
            String ctype = urlConn.getContentType();
            if (ctype == null) {
                ctype = "";
            } else {
                ctype = ctype.toLowerCase();
            }
            Log.d(TAG, "Content Type: " + ctype);
            if (ctype.contains(AUDIO_MPEG) || ctype.equals("")) {
                String temp = urlConn.getHeaderField(BITERATE_HEADER);
                Log.d(TAG, "Bitrate: " + temp);
                if (temp != null) {
                    bitrate = new Integer(temp).intValue();
                }
            } else {
                Log.e(TAG, "Does not look like we can play this audio type: " + ctype);
                Log.e(TAG, "Or we could not connect to audio");
                sendMessage(PlayListTab.TROUBLEWITHAUDIO);
                stop();
                return;
            }
        } catch (IOException ioe) {
            Log.e(TAG, "Could not connect to " + mediaUrl);
            sendMessage(PlayListTab.TROUBLEWITHAUDIO);
            stop();
            return;
        }
        if (regularStream) {
            Log.d(TAG, "Setup regular stream");
            Runnable r = new Runnable() {

                public void run() {
                    try {
                        downloadAudio(mediaUrl);
                    } catch (IOException e) {
                        Log.e(TAG, "Unable to initialize the MediaPlayer for Audio Url = " + mediaUrl, e);
                        sendMessage(PlayListTab.TROUBLEWITHAUDIO);
                        stop();
                        return;
                    }
                }
            };
            Thread t = new Thread(r);
            t.start();
        } else {
            Log.d(TAG, "Setup incremental stream");
            INTIAL_KB_BUFFER = bitrate * SECONDS / BIT;
            Runnable r = new Runnable() {

                public void run() {
                    try {
                        downloadAudioIncrement(mediaUrl);
                    } catch (IOException e) {
                        Log.e(TAG, "Unable to initialize the MediaPlayer for Audio Url = " + mediaUrl, e);
                        sendMessage(PlayListTab.TROUBLEWITHAUDIO);
                        stop();
                        return;
                    }
                }
            };
            Thread t = new Thread(r);
            t.start();
        }
    }

    /**  
     * Download the url stream to a temporary location and then call the setDataSource  
     * for that local file
     */
    public void downloadAudioIncrement(String mediaUrl) throws IOException {
        final String TAG = "downloadAudioIncrement";
        Debug.startMethodTracing("mynpr");
        int bufsizeForDownload = 1024;
        int bufsizeForfile = 64 * 1024;
        stream = new BufferedInputStream(urlConn.getInputStream(), bufsizeForDownload);
        if (stream == null) {
            Log.e(TAG, "Unable to create InputStream for mediaUrl: " + mediaUrl);
            sendMessage(PlayListTab.TROUBLEWITHAUDIO);
            stop();
        }
        Log.d(TAG, "File name: " + downloadingMediaFile);
        BufferedOutputStream bout = new BufferedOutputStream(new FileOutputStream(downloadingMediaFile), bufsizeForfile);
        byte buf[] = new byte[bufsizeForDownload];
        int totalBytesRead = 0, incrementalBytesRead = 0, numread = 0;
        if (stopping == true) {
            stream = null;
            Log.d(TAG, "null out stream ");
        }
        do {
            if (bout == null) {
                counter++;
                Log.d(TAG, "FileOutputStream is null, Create new one: " + DOWNFILE + counter);
                downloadingMediaFile = new File(context.getCacheDir(), DOWNFILE + counter);
                downloadingMediaFile.deleteOnExit();
                bout = new BufferedOutputStream(new FileOutputStream(downloadingMediaFile), bufsizeForfile);
            }
            try {
                numread = stream.read(buf);
            } catch (IOException e) {
                Log.e(TAG, e.toString());
                Log.d(TAG, "Bad read. Let's quit.");
                sendMessage(PlayListTab.TROUBLEWITHAUDIO);
                stop();
            } catch (NullPointerException e) {
                break;
            }
            if (numread < 0) {
                Log.e(TAG, "Bad read from stream. We got some number less than 0: " + numread + " Let's quit");
                sendMessage(PlayListTab.TROUBLEWITHAUDIO);
                stop();
                break;
            } else if (numread >= 1) {
                bout.write(buf, 0, numread);
                totalBytesRead += numread;
                incrementalBytesRead += numread;
                totalKbRead = totalBytesRead / 1000;
            }
            if (totalKbRead >= INTIAL_KB_BUFFER && stopping != true) {
                sendMessage(PlayListTab.CHECKRIORITY);
                Log.v(TAG, "Reached Buffer amount we want: " + "totalKbRead: " + totalKbRead + " INTIAL_KB_BUFFER: " + INTIAL_KB_BUFFER);
                bout.flush();
                bout.close();
                bout = null;
                setupplayer(downloadingMediaFile);
                totalBytesRead = 0;
            }
        } while (stream != null);
        Log.d(TAG, "Done with streaming");
        Debug.stopMethodTracing();
    }

    public void downloadAudio(final String mediaUrl) throws IOException {
        final String TAG = "downloadAudio";
        Runnable r = new Runnable() {

            public void run() {
                MediaPlayer m = new MediaPlayer();
                MediaPlayer.OnBufferingUpdateListener onBuff = new MediaPlayer.OnBufferingUpdateListener() {

                    boolean messagesent = false;

                    public void onBufferingUpdate(MediaPlayer mp, int percent) {
                        Log.d(TAG, " Precent Buffered: " + percent);
                        if (percent > 8 && messagesent == false) {
                            sendMessage(PlayListTab.STOPSPIN);
                            messagesent = true;
                        }
                    }
                };
                MediaPlayer.OnCompletionListener onComplete = new MediaPlayer.OnCompletionListener() {

                    public void onCompletion(MediaPlayer mp) {
                        String TAG = "MediaPlayer.OnCompletionListener - Normal Streaming";
                        Log.d(TAG, "Audio is done");
                        stop();
                    }
                };
                try {
                    m.setOnBufferingUpdateListener(onBuff);
                    m.setOnCompletionListener(onComplete);
                    m.setDataSource(mediaUrl);
                    Log.d(TAG, "prepare audio");
                    m.prepare();
                    m.start();
                    mediaplayers.add(m);
                    if (stopping) {
                        Log.d(TAG, "Stopping: lets get out of here");
                        m.stop();
                    }
                } catch (IllegalArgumentException e) {
                    Log.e(TAG, "Unable to initialize the MediaPlayer for Audio Url = " + mediaUrl, e);
                    sendMessage(PlayListTab.TROUBLEWITHAUDIO);
                    stop();
                } catch (IOException e) {
                    Log.e(TAG, "Unable to initialize the MediaPlayer for Audio Url = " + mediaUrl, e);
                    sendMessage(PlayListTab.TROUBLEWITHAUDIO);
                    stop();
                }
            }
        };
        Thread t = new Thread(r);
        t.start();
    }

    /**
     * Set Up player(s)
     */
    private void setupplayer(File partofaudio) {
        final File f = partofaudio;
        final String TAG = "setupplayer";
        Log.d(TAG, "File " + f.getAbsolutePath());
        Runnable r = new Runnable() {

            public void run() {
                MediaPlayer mp = new MediaPlayer();
                try {
                    MediaPlayer.OnCompletionListener listener = new MediaPlayer.OnCompletionListener() {

                        public void onCompletion(MediaPlayer mp) {
                            String TAG = "MediaPlayer.OnCompletionListener - Partial download";
                            Log.d(TAG, "Start");
                            Log.d(TAG, "Current size of mediaplayer list: " + mediaplayers.size());
                            waitingForPlayer = false;
                            boolean leave = false;
                            if (stopping) {
                                leave = true;
                            }
                            long timeInMilli = Calendar.getInstance().getTime().getTime();
                            long timeToQuit = (1000 * 30) + timeInMilli;
                            if (mediaplayers.size() <= 1 && stopping == false) {
                                Log.d(TAG, "waiting for another mediaplayer");
                                waitingForPlayer = true;
                                sendMessage(PlayListTab.SPIN);
                            }
                            while (mediaplayers.size() <= 1 && leave == false) {
                                if (timeInMilli > timeToQuit) {
                                    Log.e(TAG, "Timeout occured waiting for another media player");
                                    sendMessage(PlayListTab.TROUBLEWITHAUDIO);
                                    stop();
                                    leave = true;
                                }
                                timeInMilli = Calendar.getInstance().getTime().getTime();
                            }
                            if (waitingForPlayer == true) {
                                sendMessage(PlayListTab.STOPSPIN);
                                waitingForPlayer = false;
                            }
                            if (leave == false) {
                                MediaPlayer mp2 = mediaplayers.get(1);
                                mp2.start();
                                Log.d(TAG, "Start another player.");
                                mp.release();
                                mediaplayers.remove(mp);
                                removefile();
                            } else {
                            }
                        }
                    };
                    FileInputStream ins = new FileInputStream(f);
                    mp.setDataSource(ins.getFD());
                    mp.setAudioStreamType(AudioManager.STREAM_MUSIC);
                    Log.d(TAG, "Setup player completion listener");
                    mp.setOnCompletionListener(listener);
                    if (stopping) {
                        return;
                    }
                    if (stopping) {
                        return;
                    }
                    Log.d(TAG, "Prepare Media Player " + f);
                    if (started == false || waitingForPlayer == true) {
                        Log.d(TAG, "Prepare synchronously.");
                        mp.prepare();
                    } else {
                        Log.d(TAG, "Prepare Asynchronously.");
                        mp.prepareAsync();
                    }
                    mediaplayers.add(mp);
                    if (started == false) {
                        Log.d(TAG, "Start Media Player " + f);
                        startMediaPlayer();
                    }
                } catch (FileNotFoundException e) {
                    Log.e(TAG, e.toString());
                    Log.e(TAG, "Can't find file. Android must have deleted it on a clean up :-(");
                } catch (IllegalStateException e) {
                    Log.e(TAG, e.toString());
                    sendMessage(PlayListTab.TROUBLEWITHAUDIO);
                    stop();
                } catch (IOException e) {
                    Log.e(TAG, e.toString());
                    sendMessage(PlayListTab.TROUBLEWITHAUDIO);
                    stop();
                }
            }
        };
        preparringthread = new Thread(r);
        preparringthread.start();
        if (!started) {
            try {
                Log.d(TAG, "Start and wait for first audio clip to be prepared.");
                preparringthread.join();
            } catch (InterruptedException e) {
            }
        }
    }

    private void removefile() {
        String TAG = "removefile";
        File temp = new File(context.getCacheDir(), DOWNFILE + playedcounter);
        Log.d(TAG, temp.getAbsolutePath());
        temp.delete();
        playedcounter++;
    }

    private void startMediaPlayer() {
        String TAG = "startMediaPlayer";
        started = true;
        MediaPlayer mp = mediaplayers.get(0);
        Log.d(TAG, "Start Player");
        mp.start();
        sendMessage(PlayListTab.STOPSPIN);
    }

    public void stop() {
        final String TAG = "STOP";
        Log.d(TAG, "Entry");
        stopping = true;
        if (regularStream == true) {
            sendMessage(PlayListTab.RESETPLAYSTATUS);
        }
        try {
            if (mediaplayers != null) {
                if (!mediaplayers.isEmpty()) {
                    final MediaPlayer mp = mediaplayers.get(0);
                    if (mp.isPlaying()) {
                        Log.d(TAG, "Stop Player");
                        Runnable r = new Runnable() {

                            public void run() {
                                mp.stop();
                            }
                        };
                        Thread t = new Thread(r);
                        t.start();
                    }
                }
            }
            Runnable r = new Runnable() {

                public void run() {
                    if (stream != null) {
                        Log.d(TAG, "Close stream");
                        try {
                            stream.close();
                            Log.d(TAG, "Done Closing stream");
                        } catch (IOException e) {
                            Log.e(TAG, "error closing open connection");
                            sendMessage(PlayListTab.STOP);
                        }
                    }
                    stream = null;
                }
            };
            stream = null;
            Log.d(TAG, "Remove Phone listener");
            TelephonyManager tm = (TelephonyManager) getSystemService(TELEPHONY_SERVICE);
            tm.listen(myPhoneListener, PhoneStateListener.LISTEN_NONE);
            Log.d(TAG, "Turn off Notification");
            NotificationManager nm = (NotificationManager) getSystemService(Context.NOTIFICATION_SERVICE);
            nm.cancel(MyNPR.PLAYING_ID);
            processHasStarted = false;
            if (preparringthread != null) {
                preparringthread.interrupt();
            }
            stopSelf();
        } catch (ArrayIndexOutOfBoundsException e) {
            Log.e(TAG, "No items in Media player List");
            sendMessage(PlayListTab.STOP);
        }
    }

    public boolean isPlaying() {
        String TAG = "isPlaying";
        Log.d(TAG, " = " + processHasStarted);
        return processHasStarted;
    }

    private synchronized void sendMessage(int m) {
        String TAG = "sendMessage";
        Intent i = new Intent(MyNPR.tPLAY);
        i.putExtra(PlayListTab.MSG, m);
        Log.d(TAG, "Broadcast Message intent");
        context.sendBroadcast(i);
    }

    private void raiseThreadPriority() {
        String TAG = "raiseThreadPriority";
        Log.d(TAG, "Start");
        Process.setThreadPriority(Process.THREAD_PRIORITY_AUDIO);
    }
}
