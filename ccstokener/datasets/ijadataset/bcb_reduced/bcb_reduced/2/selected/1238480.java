package net.atoom.android.l2;

import java.io.BufferedInputStream;
import java.io.BufferedOutputStream;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.net.URL;
import java.util.LinkedList;
import java.util.List;
import android.app.Activity;
import android.graphics.Typeface;
import android.media.MediaPlayer;
import android.media.MediaPlayer.OnPreparedListener;
import android.net.Uri;
import android.os.Bundle;
import android.os.Environment;
import android.os.Handler;
import android.os.Message;
import android.view.View;
import android.view.Window;
import android.view.WindowManager;
import android.view.View.OnClickListener;
import android.widget.AdapterView;
import android.widget.Gallery;
import android.widget.ImageView;
import android.widget.MediaController;
import android.widget.TextView;
import android.widget.VideoView;
import android.widget.AdapterView.OnItemClickListener;

public class L2Activity extends Activity {

    public static final String LOGGING_TAG = "AtoomL2";

    public static final String YOUTUBE_FEED = "http://gdata.youtube.com/feeds/api/users/louloublog/uploads";

    public static final int CMD_STOP_SPLASH = 0;

    public static final int CMD_NEW_ENTRY = 10;

    public static final int CMD_LOAD_FEED = 1;

    private static final int IO_BUFFER_SIZE = 4 * 1024;

    private File m_CacheDir;

    private boolean m_IsPlaying = false;

    private List<YouTubeFeedEntry> m_YouTubeFeedEntries = new LinkedList<YouTubeFeedEntry>();

    private Handler m_CommandHandler = new Handler() {

        @Override
        public void handleMessage(Message msg) {
            switch(msg.what) {
                case CMD_STOP_SPLASH:
                    setContentView(R.layout.main);
                    break;
                case CMD_NEW_ENTRY:
                    addYouTubeFeedEntry((YouTubeFeedEntry) msg.obj);
            }
            super.handleMessage(msg);
        }
    };

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        if (savedInstanceState != null) {
        }
        requestWindowFeature(Window.FEATURE_NO_TITLE);
        getWindow().addFlags(WindowManager.LayoutParams.FLAG_FULLSCREEN);
        getWindow().setTitle(getResources().getText(R.string.hello));
        getWindow().setSoftInputMode(1);
        setContentView(R.layout.main);
        showSplashScreen();
        m_CommandHandler.postDelayed(new Runnable() {

            @Override
            public void run() {
                createCacheDir();
            }
        }, 1000);
        m_CommandHandler.postDelayed(new Runnable() {

            @Override
            public void run() {
                loadYoutubeFeed();
            }
        }, 1000);
    }

    @Override
    protected void onSaveInstanceState(Bundle outState) {
        super.onSaveInstanceState(outState);
        outState.putString("STATE", "blah blah blah");
    }

    private void addYouTubeFeedEntry(YouTubeFeedEntry youTubeFeedEntry) {
        m_YouTubeFeedEntries.add(youTubeFeedEntry);
        if (!m_IsPlaying) {
            showYouTubeEntryDetails(youTubeFeedEntry);
            showYouTubeEntryVideo(youTubeFeedEntry);
            m_IsPlaying = true;
        }
        cacheYouTubeEntryThumbnail(youTubeFeedEntry);
        addYouTubeEntryToScroller(youTubeFeedEntry);
    }

    private void cacheYouTubeEntryThumbnail(YouTubeFeedEntry youTubeFeedEntry) {
        File cacheFile = getCacheFile(youTubeFeedEntry);
        if (!cacheFile.exists()) {
            downloadFile(cacheFile, youTubeFeedEntry.getThumbnail());
        }
    }

    private void addYouTubeEntryToScroller(YouTubeFeedEntry youTubeFeedEntry) {
        File cacheFile = getCacheFile(youTubeFeedEntry);
        if (cacheFile.exists()) {
            Gallery scrollist = (Gallery) findViewById(R.id.scrolllist);
            if (scrollist.getAdapter() == null) {
                scrollist.setAdapter(new ImageAdapter(this));
                scrollist.setOnItemClickListener(new OnItemClickListener() {

                    @Override
                    public void onItemClick(AdapterView parent, View v, int position, long id) {
                        showYouTubeEntryVideo(m_YouTubeFeedEntries.get(position));
                        showYouTubeEntryDetails(m_YouTubeFeedEntries.get(position));
                    }
                });
            }
            ((ImageAdapter) scrollist.getAdapter()).addImageFile(cacheFile.getAbsolutePath());
            scrollist.setAdapter(scrollist.getAdapter());
        }
    }

    private File getCacheFile(YouTubeFeedEntry youTubeFeedEntry) {
        return new File(m_CacheDir, thumbUrlToFilename(youTubeFeedEntry.getThumbnail()));
    }

    private void showYouTubeEntryVideo(YouTubeFeedEntry youTubeFeedEntry) {
        VideoView videoView = (VideoView) findViewById(R.id.video);
        MediaController mc = new MediaController(L2Activity.this);
        mc.setAnchorView(videoView);
        Uri video = Uri.parse(youTubeFeedEntry.getUrl());
        videoView.setMediaController(mc);
        videoView.setVideoURI(video);
        videoView.setOnPreparedListener(new OnPreparedListener() {

            @Override
            public void onPrepared(MediaPlayer mp) {
                hideSplashScreen();
            }
        });
        videoView.start();
    }

    private void showYouTubeEntryDetails(YouTubeFeedEntry youTubeFeedEntry) {
        TextView titleView = (TextView) findViewById(R.id.title);
        titleView.setTypeface(Typeface.SANS_SERIF);
        titleView.setTextSize(20f);
        titleView.setText(youTubeFeedEntry.getTitle() + " (" + youTubeFeedEntry.getPublished() + ")");
        TextView textView = (TextView) findViewById(R.id.text);
        textView.setText(youTubeFeedEntry.getContent());
    }

    private void showSplashScreen() {
        ImageView splashImage = (ImageView) findViewById(R.id.splashscreen);
        splashImage.setVisibility(View.VISIBLE);
    }

    private void hideSplashScreen() {
        ImageView splashImage = (ImageView) findViewById(R.id.splashscreen);
        splashImage.setVisibility(View.GONE);
    }

    private void loadYoutubeFeed() {
        YouTubeFeedLoader loader = new YouTubeFeedLoader(YOUTUBE_FEED, m_CommandHandler);
        loader.start();
    }

    private void createCacheDir() {
        File root = Environment.getExternalStorageDirectory();
        m_CacheDir = new File(root, LOGGING_TAG);
        m_CacheDir.mkdir();
    }

    private String thumbUrlToFilename(String url) {
        return LOGGING_TAG + "_" + url.hashCode() + ".jpg";
    }

    private void downloadFile(File file, String url) {
        String state = Environment.getExternalStorageState();
        if (Environment.MEDIA_MOUNTED.equals(state)) {
            InputStream in = null;
            BufferedOutputStream out = null;
            try {
                in = new BufferedInputStream(new URL(url).openStream(), IO_BUFFER_SIZE);
                final FileOutputStream outStream = new FileOutputStream(file);
                out = new BufferedOutputStream(outStream, IO_BUFFER_SIZE);
                byte[] bytes = new byte[IO_BUFFER_SIZE];
                while (in.read(bytes) > 0) {
                    out.write(bytes);
                }
            } catch (IOException e) {
                e.printStackTrace();
            } finally {
                if (in != null) {
                    try {
                        in.close();
                    } catch (IOException e) {
                        e.printStackTrace();
                    }
                }
                if (out != null) {
                    try {
                        out.close();
                    } catch (IOException e) {
                        e.printStackTrace();
                    }
                }
            }
        }
    }

    class VideoOnClickListener implements OnClickListener {

        private YouTubeFeedEntry m_Entry;

        public VideoOnClickListener(YouTubeFeedEntry entry) {
            m_Entry = entry;
        }

        @Override
        public void onClick(View v) {
            showYouTubeEntryVideo(m_Entry);
            showYouTubeEntryDetails(m_Entry);
        }
    }
}
