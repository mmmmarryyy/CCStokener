package com.coboltforge.dontmind.coboltfm;

import java.io.BufferedReader;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.UnsupportedEncodingException;
import java.net.HttpURLConnection;
import java.net.MalformedURLException;
import java.net.URL;
import java.net.URLConnection;
import java.net.URLDecoder;
import java.net.URLEncoder;
import java.net.UnknownHostException;
import java.util.ArrayList;
import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;
import org.xmlpull.v1.XmlPullParserFactory;
import org.xmlpull.v1.XmlSerializer;
import android.content.Context;
import android.media.MediaPlayer;
import android.media.MediaPlayer.OnBufferingUpdateListener;
import android.os.ConditionVariable;
import android.os.Handler;
import android.os.Looper;
import android.os.Message;
import android.util.Log;
import com.coboltforge.dontmind.coboltfm.PlayerService.LastFMNotificationListener;
import com.coboltforge.dontmind.coboltfm.Utils.ParseException;

public class PlayerThread extends Thread {

    public static final int MESSAGE_ADJUST = 0;

    public static final int MESSAGE_STOP = 1;

    public static final int MESSAGE_UPDATE_PLAYLIST = 2;

    public static final int MESSAGE_SKIP = 3;

    public static final int MESSAGE_CACHE_TRACK_INFO = 4;

    public static final int MESSAGE_SCROBBLE_NOW_PLAYING = 5;

    public static final int MESSAGE_SUBMIT_TRACK = 6;

    public static final int MESSAGE_LOVE = 7;

    public static final int MESSAGE_BAN = 8;

    public static final int MESSAGE_SHARE = 9;

    public static final int MESSAGE_CACHE_FRIENDS_LIST = 10;

    public static final int MESSAGE_LOGIN = 11;

    public static final int MESSAGE_PAUSE = 12;

    public static final int MESSAGE_UNPAUSE = 13;

    private static final String TAG = "PlayerThread";

    private static final String XMLRPC_URL = "http://ws.audioscrobbler.com/1.0/rw/xmlrpc.php";

    private static final String WS_URL = "http://ws.audioscrobbler.com/1.0";

    public Handler mHandler;

    public ConditionVariable mInitLock = new ConditionVariable();

    private String mSession;

    private String mBaseURL;

    private String mVersionString = "1.0";

    public void setVersionString(String ver) {
        mVersionString = ver;
    }

    public void setPreBuffer(int percent) {
        mPreBuffer = percent;
        Log.d(TAG, "prebuffer set to " + mPreBuffer);
    }

    MediaPlayer mFrontMP = null;

    boolean mFrontPaused = false;

    int mBufferedFront;

    MediaPlayer mBackMP = null;

    int mBufferedBack;

    private int mPreBuffer;

    private boolean mAlternateConn = false;

    private ArrayList<XSPFTrackInfo> mPlaylist;

    private int mNextPlaylistItem;

    private XSPFTrackInfo mCurrentTrack;

    private XSPFTrackInfo mNextTrack;

    private long mStartPlaybackTime;

    private String mCurrentTrackRating;

    LastFMError mError = null;

    public static class NotEnoughContentError extends LastFMError {

        /**
		 * 
		 */
        private static final long serialVersionUID = -6594205637078329839L;

        public NotEnoughContentError() {
            super("Not enough content for this station");
        }
    }

    public static class LastFMXmlRpcError extends LastFMError {

        /**
		 * 
		 */
        private static final long serialVersionUID = 1L;

        String faultString = "";

        public LastFMXmlRpcError(String faultString) {
            super("XmlRPC error: " + faultString);
            this.faultString = faultString;
        }

        public String getFaultString() {
            return faultString;
        }
    }

    public static class BadCredentialsError extends LastFMError {

        private static final long serialVersionUID = -2749238650889906413L;

        String faultString = "";

        public static int BAD_USERNAME = 0;

        public static int BAD_PASSWORD = 1;

        private int badItem;

        public BadCredentialsError(String faultString, int badItem) {
            super(faultString);
            this.faultString = faultString;
            this.badItem = badItem;
        }

        public String getFaultString() {
            return faultString;
        }

        public int getBadItem() {
            return badItem;
        }
    }

    public LastFMError getError() {
        return mError;
    }

    private void setErrorState(LastFMError e) {
        mError = e;
    }

    public int getCurrentPosition() {
        if (mFrontMP != null) try {
            return mFrontMP.getCurrentPosition();
        } catch (IllegalStateException e) {
            return 0;
        } else return 0;
    }

    public int getCurrentBuffered() {
        if (mFrontMP != null) return mBufferedFront; else return 0;
    }

    public XSPFTrackInfo getCurrentTrack() {
        return mCurrentTrack;
    }

    public int getNextBuffered() {
        if (mBackMP != null) return mBufferedBack; else return 0;
    }

    public boolean getIsPaused() {
        return mFrontPaused;
    }

    private LastFMNotificationListener mLastFMNotificationListener = null;

    public void setLastFMNotificationListener(LastFMNotificationListener listener) {
        this.mLastFMNotificationListener = listener;
    }

    String mUsername;

    String mPassword;

    protected ArrayList<FriendInfo> mFriendsList;

    public PlayerThread(Context c, String username, String password, int preBuffer, boolean alternateConn) {
        super();
        mUsername = username;
        mPassword = password;
        mPreBuffer = preBuffer;
        mAlternateConn = alternateConn;
    }

    public void run() {
        Looper.prepare();
        mHandler = new Handler() {

            public void handleMessage(Message msg) {
                if (isInterrupted()) {
                    Log.d(TAG, "INTERRUPTED, bailing out!");
                    stopPlaying();
                    getLooper().quit();
                    return;
                }
                try {
                    switch(msg.what) {
                        case PlayerThread.MESSAGE_LOGIN:
                            Log.d(TAG, "got LOGIN message");
                            if (!login(mUsername, mPassword)) getLooper().quit();
                            break;
                        case PlayerThread.MESSAGE_STOP:
                            Log.d(TAG, "got STOP message");
                            stopPlaying();
                            getLooper().quit();
                            break;
                        case PlayerThread.MESSAGE_PAUSE:
                            Log.d(TAG, "got PAUSE message");
                            pausePlaying(true);
                            break;
                        case PlayerThread.MESSAGE_UNPAUSE:
                            Log.d(TAG, "got UNPAUSE message");
                            pausePlaying(false);
                            break;
                        case PlayerThread.MESSAGE_SUBMIT_TRACK:
                            Log.d(TAG, "got SUBMIT message");
                            TrackSubmissionParams params = (TrackSubmissionParams) msg.obj;
                            mScrobbler.submit(params.mTrack, params.mPlaybackStartTime, params.mRating);
                            break;
                        case PlayerThread.MESSAGE_SCROBBLE_NOW_PLAYING:
                            Log.d(TAG, "got NOW_PLAYING message");
                            XSPFTrackInfo currTrack = getCurrentTrack();
                            mScrobbler.nowPlaying(currTrack.getCreator(), currTrack.getTitle(), currTrack.getAlbum(), currTrack.getDuration());
                            break;
                        case PlayerThread.MESSAGE_SHARE:
                            Log.d(TAG, "got SHARE message");
                            try {
                                TrackShareParams msgParams = (TrackShareParams) msg.obj;
                                scrobblerRpcCall("recommendItem", new String[] { msgParams.mTrack.getCreator(), msgParams.mTrack.getTitle(), "track", msgParams.mRecipient, msgParams.mMessage, msgParams.mLanguage });
                                if (mLastFMNotificationListener != null) mLastFMNotificationListener.onShared(true, null);
                            } catch (LastFMXmlRpcError e) {
                                if (mLastFMNotificationListener != null) mLastFMNotificationListener.onShared(false, e.faultString);
                            }
                            break;
                        case PlayerThread.MESSAGE_LOVE:
                            Log.d(TAG, "got LOVE message");
                            try {
                                setCurrentTrackRating("L");
                                XSPFTrackInfo currentTrack = getCurrentTrack();
                                scrobblerRpcCall("loveTrack", new String[] { currentTrack.getCreator(), currentTrack.getTitle() });
                                if (mLastFMNotificationListener != null) mLastFMNotificationListener.onLoved(true, null);
                            } catch (LastFMXmlRpcError e) {
                                if (mLastFMNotificationListener != null) mLastFMNotificationListener.onLoved(false, e.faultString);
                            }
                            break;
                        case PlayerThread.MESSAGE_BAN:
                            Log.d(TAG, "got BAN message");
                            try {
                                setCurrentTrackRating("B");
                                XSPFTrackInfo currentTrack2 = getCurrentTrack();
                                scrobblerRpcCall("banTrack", new String[] { currentTrack2.getCreator(), currentTrack2.getTitle() });
                                if (mLastFMNotificationListener != null) mLastFMNotificationListener.onBanned(true, null);
                            } catch (LastFMXmlRpcError e) {
                                if (mLastFMNotificationListener != null) mLastFMNotificationListener.onBanned(false, e.faultString);
                            }
                            playNextTrack();
                            break;
                        case PlayerThread.MESSAGE_SKIP:
                            Log.d(TAG, "got SKIP message");
                            setCurrentTrackRating("S");
                            playNextTrack();
                            break;
                        case PlayerThread.MESSAGE_CACHE_TRACK_INFO:
                            Log.d(TAG, "got CACHE_TRACK_INFO message");
                            getCurrentTrack().downloadImageBitmap();
                            break;
                        case PlayerThread.MESSAGE_CACHE_FRIENDS_LIST:
                            Log.d(TAG, "got CACHE_FRIENDS_LIST message");
                            mFriendsList = downloadFriendsList(mUsername);
                            break;
                        case PlayerThread.MESSAGE_UPDATE_PLAYLIST:
                            Log.d(TAG, "got UPDATE_PLAYLIST message");
                            mPlaylist = getPlaylist();
                            if (mPlaylist != null) {
                                mNextPlaylistItem = 0;
                            } else throw new LastFMError("Playlist fetch failed");
                            break;
                        case PlayerThread.MESSAGE_ADJUST:
                            Log.d(TAG, "got ADJUST message");
                            if (adjust((String) msg.obj)) {
                                if (isInterrupted()) return;
                                mPlaylist = getPlaylist();
                                if (isInterrupted()) return;
                                if (mPlaylist != null) {
                                    mNextPlaylistItem = 0;
                                    startPlaying();
                                } else throw new LastFMError("Playlist fetch failed");
                            } else throw new LastFMError("Failed to tune to a station. Please try again or choose a different station.");
                            break;
                    }
                } catch (LastFMError e) {
                    setErrorState(e);
                }
            }

            private void setCurrentTrackRating(String string) {
                mCurrentTrackRating = string;
            }
        };
        Message.obtain(mHandler, PlayerThread.MESSAGE_LOGIN).sendToTarget();
        mInitLock.open();
        Looper.loop();
        Log.d(TAG, "Saying Goodbye");
    }

    ScrobblerClient mScrobbler;

    public boolean stopPlaying() {
        if (mCurrentTrack != null) submitCurrentTrackDelayed();
        if (mFrontMP != null) {
            mFrontMP.stop();
            mFrontMP.release();
            mFrontPaused = false;
            mFrontMP = null;
        }
        if (mBackMP != null) {
            mBackMP.stop();
            mBackMP.release();
            mBackMP = null;
        }
        return true;
    }

    private boolean pausePlaying(boolean pause) {
        if (mFrontMP == null) return false;
        if (pause) {
            int pos;
            try {
                pos = mFrontMP.getCurrentPosition();
            } catch (IllegalStateException e) {
                pos = 0;
            }
            if (pos > 0 && mFrontMP.isPlaying()) {
                mFrontMP.pause();
                mFrontPaused = true;
                Log.d(TAG, "Paused front player");
            }
        } else {
            if (mFrontPaused) {
                mFrontMP.start();
                mFrontPaused = false;
                Log.d(TAG, "Unpaused front player");
            }
        }
        return true;
    }

    public final ArrayList<FriendInfo> getFriendsList() {
        return mFriendsList;
    }

    private XSPFTrackInfo getNextTrack() {
        try {
            if (mPlaylist.size() == 0) return null;
            if (mNextPlaylistItem >= mPlaylist.size()) {
                mPlaylist = getPlaylist();
                mNextPlaylistItem = 1;
                return mPlaylist.get(0);
            } else {
                mNextPlaylistItem++;
                if (mNextPlaylistItem == mPlaylist.size() - 1) updatePlaylistDelayed();
                return mPlaylist.get(mNextPlaylistItem - 1);
            }
        } catch (NullPointerException e) {
            return null;
        }
    }

    MediaPlayer.OnCompletionListener mOnTrackCompletionListener = new MediaPlayer.OnCompletionListener() {

        @Override
        public void onCompletion(MediaPlayer mp) {
            try {
                Log.d(TAG, "completed at " + mp.getCurrentPosition() + " of " + mp.getDuration());
                mp.seekTo(mp.getDuration());
                playNextTrack();
            } catch (LastFMError e) {
                setErrorState(e);
            }
        }
    };

    OnBufferingUpdateListener mOnFrontBufferingUpdateListener = new MediaPlayer.OnBufferingUpdateListener() {

        public void onBufferingUpdate(MediaPlayer mp, int percent) {
            if (percent < 100 && mCurrentTrack != null) Log.d(TAG, "front player buffered " + percent + " %, (" + mPreBuffer + " needed) of " + mCurrentTrack.getTitle());
            mBufferedFront = percent;
            if (percent >= mPreBuffer && mFrontMP != null && !mFrontMP.isPlaying() && !mFrontPaused) mFrontMP.start();
            if (percent < mPreBuffer && mFrontMP != null && mFrontMP.isPlaying()) mFrontMP.pause();
            if (percent == 100 && mBackMP == null) try {
                bufferNextTrack();
            } catch (LastFMError e) {
                Log.e(TAG, "buffering next track failed:");
                e.printStackTrace();
            }
            if (mLastFMNotificationListener != null) mLastFMNotificationListener.onBuffer(percent);
        }
    };

    OnBufferingUpdateListener mOnBackBufferingUpdateListener = new MediaPlayer.OnBufferingUpdateListener() {

        public void onBufferingUpdate(MediaPlayer mp, int percent) {
            if (percent < 100 && mNextTrack != null) Log.d(TAG, "back player buffered " + percent + "% of " + mNextTrack.getTitle());
            mBufferedBack = percent;
        }
    };

    private static class TrackSubmissionParams {

        public XSPFTrackInfo mTrack;

        public long mPlaybackStartTime;

        public String mRating;

        public TrackSubmissionParams(XSPFTrackInfo track, long playbackStartTime, String rating) {
            mTrack = track;
            mPlaybackStartTime = playbackStartTime;
            mRating = rating;
        }
    }

    public static class TrackShareParams {

        public XSPFTrackInfo mTrack;

        public String mRecipient;

        public String mMessage;

        public String mLanguage;

        public TrackShareParams(XSPFTrackInfo track, String recipient, String message, String language) {
            mTrack = track;
            mMessage = message;
            mRecipient = recipient;
            mLanguage = language;
        }
    }

    private void submitCurrentTrackDelayed() {
        XSPFTrackInfo curTrack = getCurrentTrack();
        if (curTrack.getDuration() > 30 && mFrontMP != null) {
            Log.d(TAG, "submitCurrentTrackDelayed(), player pos at " + mFrontMP.getCurrentPosition() + ", is currently " + (mFrontMP.isPlaying() ? "playing" : "not playing") + ", track lasts " + curTrack.getDuration() + ", it's rated " + mCurrentTrackRating);
            if ((mFrontMP.getCurrentPosition() > 240000 || mFrontMP.getCurrentPosition() >= curTrack.getDuration() / 2) || (mCurrentTrackRating != null && mCurrentTrackRating.equals("L")) || (mCurrentTrackRating != null && mCurrentTrackRating.equals("B"))) {
                TrackSubmissionParams params = new TrackSubmissionParams(curTrack, mStartPlaybackTime, mCurrentTrackRating);
                Message.obtain(mHandler, PlayerThread.MESSAGE_SUBMIT_TRACK, params).sendToTarget();
            }
        }
    }

    private void startPlaying() throws LastFMError {
        Log.d(TAG, "startPlaying()");
        playNextTrack();
    }

    private void bufferNextTrack() throws LastFMError {
        mNextTrack = getNextTrack();
        if (mNextTrack == null) throw new NotEnoughContentError();
        String streamUrl = mNextTrack.getLocation();
        try {
            if (mBackMP != null) {
                mBackMP.release();
                mBackMP = null;
            }
            Log.d(TAG, "pre-buffering from stream " + streamUrl);
            mBackMP = new MediaPlayer();
            mBufferedBack = 0;
            mBackMP.setDataSource(streamUrl);
            mBackMP.setOnBufferingUpdateListener(mOnBackBufferingUpdateListener);
            mBackMP.prepareAsync();
        } catch (IllegalArgumentException e) {
            Log.e(TAG, "in bufferNextTrack", e);
            if (mBackMP != null) mBackMP.release();
            mBackMP = null;
            throw new LastFMError(e.toString());
        } catch (IllegalStateException e) {
            if (mBackMP != null) mBackMP.release();
            mBackMP = null;
            Log.e(TAG, "in bufferNextTrack", e);
            throw new LastFMError(e.toString());
        } catch (IOException e) {
            if (mBackMP != null) mBackMP.release();
            mBackMP = null;
            Log.e(TAG, "in bufferNextTrack", e);
            throw new LastFMError(e.toString());
        }
    }

    private void playNextTrack() throws LastFMError {
        Log.d(TAG, "playNextTrack()");
        if (mCurrentTrack != null) submitCurrentTrackDelayed();
        if (mNextTrack != null) {
            mCurrentTrack = mNextTrack;
            mNextTrack = null;
        } else mCurrentTrack = getNextTrack();
        if (mCurrentTrack == null) throw new NotEnoughContentError();
        String streamUrl = mCurrentTrack.getLocation();
        try {
            if (mFrontMP != null) {
                mFrontMP.stop();
                mFrontMP.release();
                mFrontPaused = false;
                mFrontMP = null;
            }
            if (mBackMP != null) {
                Log.d(TAG, "using pre-buffered stream " + streamUrl);
                mFrontMP = mBackMP;
                mBufferedFront = mBufferedBack;
                mFrontMP.setOnCompletionListener(mOnTrackCompletionListener);
                mFrontMP.setOnBufferingUpdateListener(mOnFrontBufferingUpdateListener);
                mBackMP = null;
            } else {
                Log.d(TAG, "playing from stream " + streamUrl);
                mFrontMP = new MediaPlayer();
                mBufferedFront = 0;
                mFrontMP.setDataSource(streamUrl);
                mFrontMP.setOnCompletionListener(mOnTrackCompletionListener);
                mFrontMP.setOnBufferingUpdateListener(mOnFrontBufferingUpdateListener);
                mFrontMP.prepareAsync();
            }
            if (mMuted) mFrontMP.setVolume(0, 0);
            syncMuteState();
            mStartPlaybackTime = System.currentTimeMillis() / 1000;
            mCurrentTrackRating = "";
            Message.obtain(mHandler, PlayerThread.MESSAGE_CACHE_TRACK_INFO).sendToTarget();
            Message.obtain(mHandler, PlayerThread.MESSAGE_SCROBBLE_NOW_PLAYING).sendToTarget();
            if (mLastFMNotificationListener != null) mLastFMNotificationListener.onStartTrack(mCurrentTrack);
        } catch (IllegalArgumentException e) {
            Log.e(TAG, "in playNextTrack", e);
            throw new LastFMError(e.toString());
        } catch (IllegalStateException e) {
            Log.e(TAG, "in playNextTrack", e);
            throw new LastFMError(e.toString());
        } catch (IOException e) {
            Log.e(TAG, "in playNextTrack", e);
            throw new LastFMError(e.toString());
        }
    }

    private void updatePlaylistDelayed() {
        Message.obtain(mHandler, PlayerThread.MESSAGE_UPDATE_PLAYLIST).sendToTarget();
    }

    public static ArrayList<FriendInfo> downloadFriendsList(String username) {
        try {
            URL url;
            url = new URL(WS_URL + "/user/" + URLEncoder.encode(username, "UTF-8") + "/friends.xml");
            HttpURLConnection conn = (HttpURLConnection) url.openConnection();
            conn.connect();
            InputStream is = conn.getInputStream();
            DocumentBuilderFactory dbFac = DocumentBuilderFactory.newInstance();
            DocumentBuilder db = dbFac.newDocumentBuilder();
            Document doc = db.parse(is);
            NodeList friends = doc.getElementsByTagName("user");
            ArrayList<FriendInfo> result = new ArrayList<FriendInfo>();
            for (int i = 0; i < friends.getLength(); i++) try {
                result.add(new FriendInfo((Element) friends.item(i)));
            } catch (Utils.ParseException e) {
                Log.e(TAG, "in downloadFriendsList", e);
                return null;
            }
            return result;
        } catch (Exception e) {
            Log.e(TAG, "in downloadFriendsList", e);
            return null;
        }
    }

    private ArrayList<XSPFTrackInfo> getPlaylist() {
        try {
            Log.d(TAG, "Getting playlist started");
            String urlString = "http://" + mBaseURL + "/xspf.php?sk=" + mSession + "&discovery=0&desktop=1.4.1.57486";
            if (mAlternateConn) {
                urlString += "&api_key=9d1bbaef3b443eb97973d44181d04e4b";
                Log.d(TAG, "Using alternate connection method");
            }
            URL url = new URL(urlString);
            HttpURLConnection conn = (HttpURLConnection) url.openConnection();
            conn.connect();
            InputStream is = conn.getInputStream();
            DocumentBuilderFactory dbFac = DocumentBuilderFactory.newInstance();
            DocumentBuilder db = dbFac.newDocumentBuilder();
            Document doc = db.parse(is);
            Element root = doc.getDocumentElement();
            NodeList titleNs = root.getElementsByTagName("title");
            String stationName = "<unknown station>";
            if (titleNs.getLength() > 0) {
                Element titleElement = (Element) titleNs.item(0);
                String res = "";
                for (int i = 0; i < titleElement.getChildNodes().getLength(); i++) {
                    Node item = titleElement.getChildNodes().item(i);
                    if (item.getNodeType() == Node.TEXT_NODE) res += item.getNodeValue();
                }
                stationName = URLDecoder.decode(res, "UTF-8");
            }
            NodeList tracks = doc.getElementsByTagName("track");
            ArrayList<XSPFTrackInfo> result = new ArrayList<XSPFTrackInfo>();
            for (int i = 0; i < tracks.getLength(); i++) try {
                result.add(new XSPFTrackInfo(stationName, (Element) tracks.item(i)));
            } catch (Utils.ParseException e) {
                Log.e(TAG, "in getPlaylist", e);
                return null;
            }
            Log.d(TAG, "Getting playlist successful");
            return result;
        } catch (Exception e) {
            Log.e(TAG, "in getPlaylist", e);
            return null;
        }
    }

    private boolean adjust(String stationUrl) throws LastFMError {
        try {
            URL url = new URL("http://" + mBaseURL + "/adjust.php?session=" + mSession + "&url=" + URLEncoder.encode(stationUrl, "UTF-8"));
            HttpURLConnection conn = (HttpURLConnection) url.openConnection();
            conn.connect();
            InputStream is = conn.getInputStream();
            InputStreamReader reader = new InputStreamReader(is);
            BufferedReader stringReader = new BufferedReader(reader);
            Utils.OptionsParser options = new Utils.OptionsParser(stringReader);
            if (!options.parse()) options = null;
            stringReader.close();
            if ("OK".equals(options.get("response"))) {
                return true;
            } else {
                Log.e(TAG, "Adjust failed: \"" + options.get("response") + "\"");
                return false;
            }
        } catch (MalformedURLException e) {
            Log.e(TAG, "in adjust", e);
            throw new LastFMError("Adjust failed:" + e.toString());
        } catch (UnsupportedEncodingException e) {
            Log.e(TAG, "in adjust", e);
            throw new LastFMError("Adjust failed:" + e.toString());
        } catch (IOException e) {
            Log.e(TAG, "in adjust", e);
            throw new LastFMError("Station not found:" + stationUrl);
        }
    }

    void scrobblerRpcCall(String method, String[] params) throws LastFMError {
        String timestamp = Long.toString(System.currentTimeMillis() / 1000);
        String auth = Utils.md5String(Utils.md5String(mPassword) + timestamp);
        String[] authParams = new String[3 + params.length];
        authParams[0] = mUsername;
        authParams[1] = timestamp;
        authParams[2] = auth;
        for (int i = 0; i < params.length; i++) authParams[i + 3] = params[i];
        xmlRpcCall(method, authParams);
    }

    private static final String HOST = "http://ws.audioscrobbler.com";

    boolean checkIfUserExists(String username) throws IOException {
        try {
            URL url = new URL(WS_URL + "/user/" + URLEncoder.encode(username, "UTF-8") + "/profile.xml");
            HttpURLConnection conn = (HttpURLConnection) url.openConnection();
            conn.connect();
            InputStream is = conn.getInputStream();
            is.close();
            return true;
        } catch (FileNotFoundException e) {
            return false;
        }
    }

    boolean login(String username, String password) {
        try {
            Utils.OptionsParser opts;
            opts = handshake(username, password);
            String session = opts.get("session");
            String baseHost = opts.get("base_url");
            String basePath = opts.get("base_path");
            if (session == null || session.equals("FAILED") || baseHost == null || basePath == null) {
                String message = opts.get("msg");
                if (message == null) message = "";
                if (message.equals("no such user")) {
                    if (!checkIfUserExists(username)) setErrorState(new BadCredentialsError(message, BadCredentialsError.BAD_USERNAME)); else setErrorState(new BadCredentialsError(message, BadCredentialsError.BAD_PASSWORD));
                } else setErrorState(new LastFMError("Auth failed: " + message));
                return false;
            }
            mSession = session;
            mBaseURL = baseHost + basePath;
            if (isInterrupted()) {
                Log.d(TAG, "Login interupted");
                return false;
            }
            mScrobbler = new ScrobblerClient();
            mScrobbler.setClientVersionString(mVersionString);
            mScrobbler.handshake(username, password);
            return true;
        } catch (UnknownHostException e) {
            setErrorState(new LastFMError("Login failed, unknown host (please check your internet connection)"));
            return false;
        } catch (IOException e) {
            setErrorState(new LastFMError("Login failed due to network problem (" + e.toString() + ")"));
            return false;
        }
    }

    Utils.OptionsParser handshake(String Username, String Pass) throws IOException {
        String passMD5 = Utils.md5String(Pass);
        URL url = new URL(HOST + "/radio/handshake.php?version=1.0.0.0&platform=windows&username=" + URLEncoder.encode(Username, "UTF_8") + "&passwordmd5=" + passMD5);
        Log.d(TAG, "Shakin' hands: " + url.toString());
        HttpURLConnection conn = (HttpURLConnection) url.openConnection();
        conn.connect();
        InputStream is = conn.getInputStream();
        InputStreamReader reader = new InputStreamReader(is);
        BufferedReader stringReader = new BufferedReader(reader);
        Utils.OptionsParser options = new Utils.OptionsParser(stringReader);
        if (!options.parse()) options = null;
        stringReader.close();
        Log.d(TAG, "Handshake complete");
        return options;
    }

    static void xmlRpcCall(String method, String[] params) throws LastFMError {
        try {
            XmlPullParserFactory fac = XmlPullParserFactory.newInstance();
            XmlSerializer serializer = fac.newSerializer();
            URL url;
            url = new URL(XMLRPC_URL);
            URLConnection conn;
            conn = url.openConnection();
            conn.setRequestProperty("Content-Type", "text/xml");
            conn.setDoOutput(true);
            serializer.setOutput(conn.getOutputStream(), "UTF-8");
            serializer.startDocument("UTF-8", true);
            serializer.startTag(null, "methodCall");
            serializer.startTag(null, "methodName");
            serializer.text(method);
            serializer.endTag(null, "methodName");
            serializer.startTag(null, "params");
            for (String s : params) {
                serializer.startTag(null, "param");
                serializer.startTag(null, "value");
                serializer.startTag(null, "string");
                serializer.text(s);
                serializer.endTag(null, "string");
                serializer.endTag(null, "value");
                serializer.endTag(null, "param");
            }
            serializer.endTag(null, "params");
            serializer.endTag(null, "methodCall");
            serializer.flush();
            InputStream is = conn.getInputStream();
            DocumentBuilderFactory dbFac = DocumentBuilderFactory.newInstance();
            DocumentBuilder db = dbFac.newDocumentBuilder();
            Document doc = db.parse(is);
            try {
                String res = Utils.getChildElement(doc.getDocumentElement(), new String[] { "params", "param", "value", "string" });
                if (!res.equals("OK")) {
                    Log.e(TAG, "while xmlrpc got " + res);
                    throw new LastFMXmlRpcError("XMLRPC Call failed: " + res);
                }
            } catch (ParseException e) {
                String faultString = Utils.getChildElement(doc.getDocumentElement(), new String[] { "params", "param", "value", "struct", "member[1]", "value", "string" });
                throw new LastFMXmlRpcError(faultString);
            }
        } catch (LastFMXmlRpcError e) {
            throw e;
        } catch (Exception e) {
            Log.e(TAG, "while xmlrpc", e);
            throw new LastFMError(e.toString());
        }
    }

    Boolean mMuted = false;

    public void unmute() {
        synchronized (mMuted) {
            if (mFrontMP != null) mFrontMP.setVolume(1.0f, 1.0f);
            mMuted = false;
        }
    }

    public void mute() {
        synchronized (mMuted) {
            mMuted = true;
            if (mFrontMP != null) mFrontMP.setVolume(0, 0);
        }
    }

    public void syncMuteState() {
        synchronized (mMuted) {
            if (mMuted) mFrontMP.setVolume(0, 0); else mFrontMP.setVolume(1, 1);
        }
    }
}
