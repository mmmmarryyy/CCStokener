package etracks.app;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.URL;
import java.net.URLConnection;
import org.json.JSONObject;
import android.app.Activity;
import android.app.ProgressDialog;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.media.MediaPlayer;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.ImageView;
import android.widget.TextView;

public class playmix extends Activity {

    MediaPlayer mixPlayer = new MediaPlayer();

    String TAG = "ET";

    private File mediaFile;

    private File auxMediaFile;

    private boolean loading = false, skipping = false, skipallowed = true;

    ProgressDialog pd;

    String token = "", info = "";

    public void onCreate(Bundle savedInstanceState) {
        setTheme(android.R.style.Theme_NoTitleBar_Fullscreen);
        super.onCreate(savedInstanceState);
        setContentView(R.layout.playpage);
        Bundle bun = getIntent().getExtras();
        info = bun.getString("param_string");
        token = bun.getString("param_string1");
        String cover_url = bun.getString("param_string2");
        String title = bun.getString("param_string3");
        ImageView PlayCover = (ImageView) findViewById(R.id.PlayingMixIMG);
        TextView titleTV = (TextView) findViewById(R.id.playPageTitle);
        titleTV.setText(title);
        String Req = "http://8tracks.com/sets/" + token + "/play.json?mix_id=" + info + "?api_key=44c6da02712ba6681580ae23a035fea62635a970";
        URL ReqURL;
        try {
            PlayCover.setImageBitmap(fetchImage(cover_url));
            ReqURL = new URL(Req);
            HttpURLConnection conn = (HttpURLConnection) ReqURL.openConnection();
            conn.setRequestProperty("User-Agent", "Agent");
            BufferedReader buff = new BufferedReader(new InputStreamReader(conn.getInputStream()));
            String source = buff.readLine();
            JSONObject js = new JSONObject(source);
            JSONObject set = js.getJSONObject("set");
            String maySkip = set.getString("skip_allowed");
            Log.e(TAG, maySkip);
            if (maySkip.equals("false")) {
                skipallowed = false;
            }
            JSONObject track = set.getJSONObject("track");
            String thisMusicInfo = track.getString("release_name");
            thisMusicInfo += " " + track.getString("performer");
            TextView musicTv = (TextView) findViewById(R.id.playPageMusicsPlayed);
            musicTv.setText(thisMusicInfo);
            final String currMusic = track.getString("url");
            Log.e(TAG, currMusic);
            loading = true;
            pd = ProgressDialog.show(this, "", "Downloading mix..", true, false);
            Runnable progDial = new Runnable() {

                @Override
                public void run() {
                    showSpinner();
                }
            };
            new Thread(progDial).start();
            Runnable r = new Runnable() {

                public void run() {
                    playAudio(currMusic);
                }
            };
            new Thread(r).start();
        } catch (Exception e) {
        }
    }

    protected void showSpinner() {
        while (loading) ;
        pd.dismiss();
    }

    private Bitmap fetchImage(String urlstr) throws Exception {
        URL url;
        url = new URL(urlstr);
        HttpURLConnection c = (HttpURLConnection) url.openConnection();
        c.setDoInput(true);
        c.setRequestProperty("User-Agent", "Agent");
        c.connect();
        InputStream is = c.getInputStream();
        Bitmap img;
        img = BitmapFactory.decodeStream(is);
        return img;
    }

    public void playMixHandler(View v) {
        if (!mixPlayer.isPlaying()) mixPlayer.start();
    }

    public void pauseMixHandler(View v) {
        if (mixPlayer.isPlaying()) ;
        mixPlayer.pause();
    }

    public void forwardMixHandler(View v) throws InterruptedException {
        if (skipallowed) {
            if (!skipping) {
                Log.e(TAG, "ON SKIPING");
                skipping = true;
                String Req = "http://8tracks.com/sets/" + token + "/skip.json?mix_id=" + info + "?api_key=44c6da02712ba6681580ae23a035fea62635a970";
                URL ReqURL;
                try {
                    ReqURL = new URL(Req);
                    HttpURLConnection conn = (HttpURLConnection) ReqURL.openConnection();
                    conn.setRequestProperty("User-Agent", "Agent");
                    BufferedReader buff = new BufferedReader(new InputStreamReader(conn.getInputStream()));
                    String source = buff.readLine();
                    JSONObject js = new JSONObject(source);
                    JSONObject set = js.getJSONObject("set");
                    String maySkip = set.getString("skip_allowed");
                    if (maySkip.equals("false")) {
                        skipallowed = false;
                    }
                    JSONObject track = set.getJSONObject("track");
                    String thisMusicInfo = "\n" + track.getString("release_name");
                    thisMusicInfo += " " + track.getString("performer");
                    TextView musicTv = (TextView) findViewById(R.id.playPageMusicsPlayed);
                    musicTv.append(thisMusicInfo);
                    final String currMusic = track.getString("url");
                    loading = true;
                    pd = ProgressDialog.show(this, "", "Downloading next music..", true, false);
                    Runnable progDial = new Runnable() {

                        @Override
                        public void run() {
                            showSpinner();
                        }
                    };
                    new Thread(progDial).start();
                    mixPlayer.reset();
                    mediaFile.delete();
                    Runnable r = new Runnable() {

                        public void run() {
                            playAudio(currMusic);
                        }
                    };
                    new Thread(r).start();
                    skipping = false;
                } catch (Exception e) {
                }
            }
        } else {
            Log.e(TAG, "No skip allowed");
            TextView tv = (TextView) findViewById(R.id.SkipsAutorized);
            tv.setText("No more skips allowed");
        }
    }

    private void playAudio(String mediaUrl) {
        try {
            URLConnection cn = new URL(mediaUrl).openConnection();
            InputStream is = cn.getInputStream();
            mediaFile = new File(this.getCacheDir(), "mediafile");
            FileOutputStream fos = new FileOutputStream(mediaFile);
            byte buf[] = new byte[16 * 1024];
            Log.i("FileOutputStream", "Download");
            do {
                int numread = is.read(buf);
                if (numread <= 0) break;
                fos.write(buf, 0, numread);
            } while (true);
            fos.flush();
            fos.close();
            Log.i("FileOutputStream", "Saved");
            MediaPlayer.OnCompletionListener listener = new MediaPlayer.OnCompletionListener() {

                public void onCompletion(MediaPlayer mp) {
                    mp.release();
                    Log.i("MediaPlayer.OnCompletionListener", "MediaPlayer Released");
                }
            };
            mixPlayer.setOnCompletionListener(listener);
            FileInputStream fis = new FileInputStream(mediaFile);
            mixPlayer.setDataSource(fis.getFD());
            mixPlayer.prepare();
            Log.i("MediaPlayer", "Start Player");
            loading = false;
        } catch (Exception e) {
            e.printStackTrace();
        }
    }
}
