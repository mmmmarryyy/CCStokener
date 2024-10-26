package jp.mp3;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.UnsupportedEncodingException;
import java.net.MalformedURLException;
import java.net.URL;
import android.app.Activity;
import android.media.AsyncPlayer;
import android.media.MediaPlayer;
import android.net.Uri;
import android.os.Bundle;
import android.webkit.WebView;
import android.widget.TextView;

public class mp3 extends Activity {

    /** Called when the activity is first created. */
    @Override
    public void onCreate(Bundle icicle) {
        super.onCreate(icicle);
        TextView tv = new TextView(this);
        String greet = "Hello MP3 from internet.";
        tv.setText(greet);
        talktext("%82%B6%82%B6%82%B6%82%A9%82%F1%82%AC%82%EA%82%EA%82%EA%82%EA%82%EA%82%EA%82%EA%82%EA");
    }

    public void talktext(String msg) {
        try {
            main("http://yomoyomo.jp/CreateSounds.php?t=" + msg);
        } catch (IOException e1) {
            e1.printStackTrace();
        }
        Uri uri = null;
        String url = null;
        url = "http://android.adinterest.biz/sample.mp3";
        uri = Uri.parse(url);
        MediaPlayer _mp;
        _mp = MediaPlayer.create(this, uri);
        try {
            _mp.start();
        } catch (Exception e) {
        }
    }

    private static void main(String mp3Path) throws IOException {
        String convPath = "http://android.adinterest.biz/wav2mp3.php?k=";
        String uri = convPath + mp3Path;
        URL rssurl = new URL(uri);
        InputStream is = rssurl.openStream();
        BufferedReader br = new BufferedReader(new InputStreamReader(is, "UTF-8"));
        String buf = "";
        while ((buf = br.readLine()) != null) {
        }
        is.close();
        br.close();
    }
}
