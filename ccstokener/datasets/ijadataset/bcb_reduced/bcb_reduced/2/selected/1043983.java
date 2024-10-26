package com.commonsware.android.EMusicDownloader;

import java.net.URL;
import javax.xml.parsers.SAXParser;
import javax.xml.parsers.SAXParserFactory;
import org.xml.sax.InputSource;
import org.xml.sax.XMLReader;
import android.app.Activity;
import android.app.ProgressDialog;
import android.content.Intent;
import android.content.SharedPreferences;
import android.preference.PreferenceManager;
import android.content.res.Configuration;
import android.os.Bundle;
import android.os.Handler;
import android.os.Message;
import android.view.View;
import android.view.Window;
import android.webkit.WebView;
import android.widget.RatingBar;
import android.widget.TextView;

public class SingleArtist extends Activity {

    private TextView headerTextView;

    private TextView datesTextView;

    private TextView decadesTextView;

    private WebView bioBlurb;

    private int statuscode = 200;

    private String artistId;

    private String artist;

    private String bio;

    private String emusicURL;

    private String born;

    private String death;

    private String rating;

    private String decade;

    private String bioFontSize;

    private String urlAddress;

    private RatingBar ratingBar;

    private Boolean vLoaded = false;

    /** Called when the activity is first created. */
    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        requestWindowFeature(Window.FEATURE_INDETERMINATE_PROGRESS);
        setContentView(R.layout.singleartist);
        Intent myIntent = getIntent();
        artistId = myIntent.getStringExtra("keyartistid");
        headerTextView = (TextView) findViewById(R.id.theader);
        datesTextView = (TextView) findViewById(R.id.tdates);
        decadesTextView = (TextView) findViewById(R.id.tdecades);
        ratingBar = (RatingBar) findViewById(R.id.rbar);
        bioBlurb = (WebView) findViewById(R.id.blurb);
        bioBlurb.setBackgroundColor(0);
        urlAddress = "http://api.emusic.com/artist/info?" + Secrets.apikey + "&artistId=" + artistId + "&include=artistEditorial,artistRating";
        getInfoFromXML();
    }

    private void getInfoFromXML() {
        final ProgressDialog dialog = ProgressDialog.show(this, "", getString(R.string.loading), true, true);
        setProgressBarIndeterminateVisibility(true);
        Thread t3 = new Thread() {

            public void run() {
                waiting(200);
                try {
                    URL url = new URL(urlAddress);
                    SAXParserFactory spf = SAXParserFactory.newInstance();
                    SAXParser sp = spf.newSAXParser();
                    XMLReader xr = sp.getXMLReader();
                    XMLHandlerSingleArtist myXMLHandler = new XMLHandlerSingleArtist();
                    xr.setContentHandler(myXMLHandler);
                    xr.parse(new InputSource(url.openStream()));
                    artist = myXMLHandler.artist;
                    emusicURL = myXMLHandler.url;
                    bio = myXMLHandler.bio;
                    born = myXMLHandler.born;
                    death = myXMLHandler.death;
                    decade = myXMLHandler.decade;
                    rating = myXMLHandler.rating;
                    statuscode = myXMLHandler.statuscode;
                    if (statuscode != 200 && statuscode != 206) {
                        throw new Exception();
                    }
                    handlerSetContent.sendEmptyMessage(0);
                } catch (Exception e) {
                    headerTextView.post(new Runnable() {

                        public void run() {
                            headerTextView.setText(R.string.couldnt_get_artist_info);
                        }
                    });
                }
                dialog.dismiss();
                handlerDoneLoading.sendEmptyMessage(0);
            }
        };
        t3.start();
    }

    private Handler handlerSetContent = new Handler() {

        @Override
        public void handleMessage(Message msg) {
            vLoaded = true;
            headerTextView.setText(artist);
            datesTextView.setText(getString(R.string.born) + " " + born + " " + getString(R.string.death) + " " + death);
            decadesTextView.setText(getString(R.string.active_decades) + " " + decade);
            bio = bio.replaceAll("<a href.[^<]*>", "");
            bio = bio.replace("</a>", "");
            String blurb = "<font color=white size=" + bioFontSize + ">Description: " + bio + "</font>";
            bioBlurb.loadDataWithBaseURL(null, blurb, "text/html", "utf-8", "about:blank");
            try {
                ratingBar.setRating(Float.parseFloat(rating));
            } catch (Exception ef) {
            }
        }
    };

    private static void waiting(int n) {
        long t0, t1;
        t0 = System.currentTimeMillis();
        do {
            t1 = System.currentTimeMillis();
        } while (t1 - t0 < n);
    }

    private Handler handlerDoneLoading = new Handler() {

        @Override
        public void handleMessage(Message msg) {
            setProgressBarIndeterminateVisibility(false);
        }
    };

    public void albumsButtonPressed(View button) {
        Intent myIntent = new Intent(this, SearchListWindow.class);
        String stringtype = "album";
        myIntent.putExtra("keytype", stringtype);
        String urlad = "http://api.emusic.com/" + stringtype + "/search?" + Secrets.apikey + "&artistId=" + artistId;
        myIntent.putExtra("keyurl", urlad);
        String totalsearch = "artistId=" + artistId;
        myIntent.putExtra("keyquery", totalsearch);
        startActivity(myIntent);
    }

    public void relatedButtonPressed(View button) {
        Intent myIntent = new Intent(this, SearchListWindow.class);
        String stringtype = "artist";
        myIntent.putExtra("keytype", stringtype);
        String stringtitle = getString(R.string.artists_similar_to) + " " + artist;
        myIntent.putExtra("keytitle", stringtitle);
        String urlad = "http://api.emusic.com/" + stringtype + "/related?" + Secrets.apikey + "&artistId=" + artistId;
        myIntent.putExtra("keyurl", urlad);
        String totalsearch = "artistId=" + artistId;
        myIntent.putExtra("keyquery", totalsearch);
        startActivity(myIntent);
    }

    public void emusicButtonPressed(View button) {
        Intent myIntent = new Intent(this, WebWindowBrowse.class);
        myIntent.putExtra("keyurl", emusicURL);
        startActivity(myIntent);
    }

    public void logoPressed(View buttoncover) {
        String browseurl = "http://www.emusic.com?fref=400062";
        Intent browseIntent = new Intent(this, WebWindowBrowse.class);
        browseIntent.putExtra("keyurl", browseurl);
        startActivity(browseIntent);
    }

    @Override
    protected void onResume() {
        super.onResume();
        SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(this);
        bioFontSize = prefs.getString("textsizelist", "2");
    }

    @Override
    public void onConfigurationChanged(Configuration newConfig) {
        super.onConfigurationChanged(newConfig);
        setContentView(R.layout.singleartist);
        headerTextView = (TextView) findViewById(R.id.theader);
        datesTextView = (TextView) findViewById(R.id.tdates);
        decadesTextView = (TextView) findViewById(R.id.tdecades);
        ratingBar = (RatingBar) findViewById(R.id.rbar);
        bioBlurb = (WebView) findViewById(R.id.blurb);
        bioBlurb.setBackgroundColor(0);
        if (vLoaded) {
            headerTextView.setText(artist);
            datesTextView.setText(getString(R.string.born) + " " + born + " " + getString(R.string.death) + " " + death);
            decadesTextView.setText(getString(R.string.active_decades) + " " + decade);
            bio = bio.replaceAll("<a href.[^<]*>", "");
            bio = bio.replace("</a>", "");
            String blurb = "<font color=white size=" + bioFontSize + ">Description: " + bio + "</font>";
            bioBlurb.loadDataWithBaseURL(null, blurb, "text/html", "utf-8", "about:blank");
            try {
                ratingBar.setRating(Float.parseFloat(rating));
            } catch (Exception ef) {
            }
        }
    }
}
