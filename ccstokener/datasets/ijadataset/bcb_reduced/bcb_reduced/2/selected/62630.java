package fr.upmfgrenoble.rssreader.activity;

import java.net.MalformedURLException;
import java.net.URL;
import javax.xml.parsers.SAXParser;
import javax.xml.parsers.SAXParserFactory;
import org.xml.sax.InputSource;
import fr.upmfgrenoble.rssreader.R;
import fr.upmfgrenoble.rssreader.database.RssReader;
import fr.upmfgrenoble.rssreader.parser.RssHandler;
import android.app.Activity;
import android.database.Cursor;
import android.net.Uri;
import android.os.Bundle;
import android.util.Log;

public class UpdateFeed extends Activity {

    private static final SAXParserFactory spf = SAXParserFactory.newInstance();

    private SAXParser sp;

    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.updatefeed);
        Uri uri = getIntent().getData();
        Cursor c = getContentResolver().query(uri, new String[] { RssReader.RssFeed.URL }, "_id = " + uri.getLastPathSegment(), null, null);
        String url = null;
        while (c.moveToNext()) {
            url = c.getString(c.getColumnIndex(RssReader.RssFeed.URL));
            url.trim();
        }
        try {
            parse(new URL(url), uri);
        } catch (MalformedURLException e) {
            e.printStackTrace();
        }
    }

    protected void parse(final URL url, final Uri feedId) {
        new Thread(new Runnable() {

            public void run() {
                Log.v("Parser", "parse feed...");
                try {
                    if (sp == null) sp = spf.newSAXParser();
                    InputSource is = new InputSource(url.openStream());
                    RssHandler handler = new RssHandler(UpdateFeed.this, feedId);
                    sp.parse(is, handler);
                    setResult(RESULT_OK);
                } catch (Exception e) {
                    e.printStackTrace();
                    setResult(RESULT_CANCELED);
                }
                finish();
            }
        }).start();
    }
}
