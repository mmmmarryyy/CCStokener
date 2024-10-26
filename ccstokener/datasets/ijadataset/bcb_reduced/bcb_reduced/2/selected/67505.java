package com.commonsware.android.EMusicDownloader;

import java.io.BufferedInputStream;
import java.io.BufferedReader;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.URL;
import javax.xml.parsers.SAXParser;
import javax.xml.parsers.SAXParserFactory;
import org.xml.sax.InputSource;
import org.xml.sax.XMLReader;
import org.apache.commons.lang.StringEscapeUtils;
import org.apache.commons.lang.StringEscapeUtils.*;
import android.app.Activity;
import android.app.Dialog;
import android.app.ProgressDialog;
import android.content.Intent;
import android.content.res.Configuration;
import android.content.res.Resources;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.media.AudioManager;
import android.media.MediaPlayer;
import android.os.Bundle;
import android.os.Environment;
import android.os.Handler;
import android.os.Message;
import android.util.Log;
import android.view.KeyEvent;
import android.view.View;
import android.view.Window;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.ListView;
import android.widget.RatingBar;
import android.widget.ScrollView;
import android.widget.TextView;
import android.widget.Toast;

public class SingleAlbum extends Activity implements AdapterView.OnItemClickListener {

    private Activity thisActivity;

    private ImageView reviewsButton;

    private ImageView albumArt;

    private TextView nameTextView;

    private TextView artistTextView;

    private TextView genreTextView;

    private TextView labelTextView;

    private LinearLayout genreLayout;

    private LinearLayout labelLayout;

    private LinearLayout artistLayout;

    private ListView trackList;

    private RatingBar ratingBar;

    private int numberOfTracks;

    private int samplePlayPosition;

    private int statuscode = 200;

    private int version;

    private String albumId;

    private String genreId = "";

    private String labelId = "";

    private String artist;

    private String album;

    private String genre = "";

    private String mp3Address;

    private String currentPlayingTrack;

    private String label;

    private String artistId;

    private String date;

    private String rating;

    private String urlAddress;

    private String emusicURL;

    private String imageURL;

    private String[] trackNames;

    private Boolean vSamplesExist = false;

    private Boolean vArtExists = false;

    private Boolean vKilled = false;

    private Boolean vLoaded = false;

    private Bitmap albumArtBitmap;

    /** Called when the activity is first created. */
    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        requestWindowFeature(Window.FEATURE_INDETERMINATE_PROGRESS);
        setContentView(R.layout.singlealbum);
        version = android.os.Build.VERSION.SDK_INT;
        Intent myIntent = getIntent();
        albumId = myIntent.getStringExtra("keyalbumid");
        emusicURL = myIntent.getStringExtra("keyexturl");
        album = myIntent.getStringExtra("keyalbum");
        artist = myIntent.getStringExtra("keyartist");
        thisActivity = this;
        genreLayout = (LinearLayout) findViewById(R.id.llgenre);
        artistLayout = (LinearLayout) findViewById(R.id.llartist);
        labelLayout = (LinearLayout) findViewById(R.id.lllabel);
        nameTextView = (TextView) findViewById(R.id.tname);
        artistTextView = (TextView) findViewById(R.id.tartist);
        genreTextView = (TextView) findViewById(R.id.tgenre);
        labelTextView = (TextView) findViewById(R.id.tlabel);
        Resources res = getResources();
        if (version < 11) {
            genreLayout.setBackgroundDrawable(res.getDrawable(android.R.drawable.list_selector_background));
            labelLayout.setBackgroundDrawable(res.getDrawable(android.R.drawable.list_selector_background));
            artistLayout.setBackgroundDrawable(res.getDrawable(android.R.drawable.list_selector_background));
        } else {
            genreLayout.setBackgroundResource(R.drawable.list_selector_holo_dark);
            labelLayout.setBackgroundResource(R.drawable.list_selector_holo_dark);
            artistLayout.setBackgroundResource(R.drawable.list_selector_holo_dark);
        }
        genreLayout.setFocusable(true);
        labelLayout.setFocusable(true);
        artistLayout.setFocusable(true);
        albumArt = (ImageView) findViewById(R.id.albumart);
        reviewsButton = (ImageView) findViewById(R.id.reviewsbutton);
        trackList = (ListView) findViewById(R.id.trklist);
        ratingBar = (RatingBar) findViewById(R.id.rbar);
        trackList.setOnItemClickListener(this);
        urlAddress = "http://api.emusic.com/album/info?" + Secrets.apikey + "&albumId=" + albumId + "&include=albumRating,label&imageSize=small";
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
                    XMLHandlerSingleAlbum myXMLHandler = new XMLHandlerSingleAlbum();
                    xr.setContentHandler(myXMLHandler);
                    xr.parse(new InputSource(url.openStream()));
                    statuscode = myXMLHandler.statuscode;
                    if (statuscode != 200 && statuscode != 206) {
                        throw new Exception();
                    }
                    genre = myXMLHandler.genre;
                    genreId = myXMLHandler.genreId;
                    labelId = myXMLHandler.labelId;
                    label = myXMLHandler.label;
                    date = myXMLHandler.releaseDate;
                    rating = myXMLHandler.rating;
                    imageURL = myXMLHandler.imageURL;
                    artist = myXMLHandler.artist;
                    artistId = myXMLHandler.artistId;
                    numberOfTracks = myXMLHandler.nItems;
                    trackNames = myXMLHandler.tracks;
                    handlerSetContent.sendEmptyMessage(0);
                    dialog.dismiss();
                    updateImage();
                } catch (Exception e) {
                    final Exception ef = e;
                    nameTextView.post(new Runnable() {

                        public void run() {
                            nameTextView.setText(R.string.couldnt_get_album_info);
                        }
                    });
                }
                if (dialog.isShowing()) {
                    dialog.dismiss();
                }
                handlerDoneLoading.sendEmptyMessage(0);
            }
        };
        t3.start();
    }

    private Handler handlerToast = new Handler() {

        @Override
        public void handleMessage(Message msg) {
            Object texttotoast = msg.obj;
            Toast.makeText(thisActivity, "" + texttotoast, Toast.LENGTH_LONG).show();
        }
    };

    private Handler handlerSetContent = new Handler() {

        @Override
        public void handleMessage(Message msg) {
            vLoaded = true;
            reviewsButton.setVisibility(0);
            String namestring = album;
            String artiststring = artist;
            String genrestring = genre;
            String labelstring = label;
            try {
                namestring = StringEscapeUtils.unescapeHtml(namestring);
                artiststring = StringEscapeUtils.unescapeHtml(artiststring);
                genrestring = StringEscapeUtils.unescapeHtml(genrestring);
                labelstring = StringEscapeUtils.unescapeHtml(labelstring);
            } catch (Exception em) {
            }
            nameTextView.setText(namestring);
            artistTextView.setText(artiststring);
            if (genre != null && genre != "") {
                genreTextView.setText(genrestring);
                genreLayout.setVisibility(0);
            }
            if (label != null && label != "") {
                labelTextView.setText(labelstring);
                labelLayout.setVisibility(0);
            }
            try {
                ratingBar.setRating(Float.parseFloat(rating));
            } catch (Exception ef2) {
            }
            trackList.setAdapter(new ArrayAdapter<String>(thisActivity, R.layout.item, R.id.label, trackNames));
        }
    };

    private Handler handlerDoneLoading = new Handler() {

        @Override
        public void handleMessage(Message msg) {
            setProgressBarIndeterminateVisibility(false);
        }
    };

    private static void waiting(int n) {
        long t0, t1;
        t0 = System.currentTimeMillis();
        do {
            t1 = System.currentTimeMillis();
        } while (t1 - t0 < n);
    }

    public void buyButtonPressed(View button) {
        Intent myIntent = new Intent(this, WebWindow.class);
        myIntent.putExtra("keyurl", emusicURL);
        Log.d("EMD - ", "New WebWindow " + emusicURL);
        startActivity(myIntent);
    }

    public void artistButtonPressed(View button) {
        Intent myIntent = new Intent(this, SingleArtist.class);
        myIntent.putExtra("keyartistid", artistId);
        startActivity(myIntent);
    }

    public void reviewsButtonPressed(View button) {
        Intent myIntent = new Intent(this, ReviewList.class);
        String stringtitle = "User reviews of " + album;
        myIntent.putExtra("keytitle", stringtitle);
        String urlad = "http://api.emusic.com/album/reviews?" + Secrets.apikey + "&albumId=" + albumId;
        myIntent.putExtra("keyurl", urlad);
        startActivity(myIntent);
    }

    public void genreButtonPressed(View button) {
        Intent myIntent = new Intent(this, SearchListWindow.class);
        String stringtype = "album";
        myIntent.putExtra("keytype", stringtype);
        String stringtitle = "Genre: " + genre;
        myIntent.putExtra("keytitle", stringtitle);
        String urlad = "http://api.emusic.com/album/charts?" + Secrets.apikey + "&genreId=" + genreId;
        myIntent.putExtra("keyurl", urlad);
        String totalsearch = "genreId=" + genreId;
        myIntent.putExtra("keyquery", totalsearch);
        startActivity(myIntent);
    }

    public void labelButtonPressed(View button) {
        Intent myIntent = new Intent(this, SearchListWindow.class);
        String stringtype = "album";
        myIntent.putExtra("keytype", stringtype);
        String stringtitle = "Label: " + label;
        myIntent.putExtra("keytitle", stringtitle);
        String urlad = "http://api.emusic.com/album/charts?" + Secrets.apikey + "&labelId=" + labelId;
        myIntent.putExtra("keyurl", urlad);
        String totalsearch = "labelId=" + labelId;
        myIntent.putExtra("keyquery", totalsearch);
        startActivity(myIntent);
    }

    private void updateImage() {
        albumArtBitmap = getImageBitmap(imageURL);
        if (vArtExists) {
            albumArt.post(new Runnable() {

                public void run() {
                    albumArt.setImageBitmap(albumArtBitmap);
                }
            });
        } else {
            albumArtBitmap = getImageBitmap(imageURL);
            if (vArtExists) {
                albumArt.post(new Runnable() {

                    public void run() {
                        albumArt.setImageBitmap(albumArtBitmap);
                    }
                });
            } else {
                albumArt.post(new Runnable() {

                    public void run() {
                        albumArt.setImageResource(R.drawable.noalbum);
                        ;
                    }
                });
            }
        }
    }

    private Bitmap getImageBitmap(String url) {
        Bitmap bm = null;
        try {
            URL aURL = new URL(url);
            HttpURLConnection conn = (HttpURLConnection) aURL.openConnection();
            long ifs = 0;
            ifs = conn.getContentLength();
            if (ifs == -1) {
                conn.disconnect();
                conn = (HttpURLConnection) aURL.openConnection();
                ifs = conn.getContentLength();
            }
            vArtExists = false;
            if (ifs > 0) {
                conn.connect();
                InputStream is = conn.getInputStream();
                BufferedInputStream bis = new BufferedInputStream(is);
                bm = BitmapFactory.decodeStream(bis);
                bis.close();
                is.close();
                vArtExists = true;
            } else {
                Log.e("EMD - ", "art fail ifs 0 " + ifs + " " + url);
            }
        } catch (IOException e) {
            vArtExists = false;
            Log.e("EMD - ", "art fail");
        }
        return bm;
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
        vKilled = true;
    }

    @Override
    public void onStop() {
        super.onStop();
        vKilled = true;
    }

    @Override
    public void onResume() {
        super.onResume();
        vKilled = false;
    }

    public void logoPressed(View buttoncover) {
        Log.d("EMD - ", "logo pressed");
        String browseurl = "http://www.emusic.com?fref=400062";
        Intent browseIntent = new Intent(this, WebWindowBrowse.class);
        browseIntent.putExtra("keyurl", browseurl);
        startActivity(browseIntent);
    }

    @Override
    public void onConfigurationChanged(Configuration newConfig) {
        super.onConfigurationChanged(newConfig);
        setContentView(R.layout.singlealbum);
        nameTextView = (TextView) findViewById(R.id.tname);
        artistTextView = (TextView) findViewById(R.id.tartist);
        genreTextView = (TextView) findViewById(R.id.tgenre);
        labelTextView = (TextView) findViewById(R.id.tlabel);
        genreLayout = (LinearLayout) findViewById(R.id.llgenre);
        artistLayout = (LinearLayout) findViewById(R.id.llartist);
        labelLayout = (LinearLayout) findViewById(R.id.lllabel);
        Resources res = getResources();
        if (version < 11) {
            genreLayout.setBackgroundDrawable(res.getDrawable(android.R.drawable.list_selector_background));
            labelLayout.setBackgroundDrawable(res.getDrawable(android.R.drawable.list_selector_background));
            artistLayout.setBackgroundDrawable(res.getDrawable(android.R.drawable.list_selector_background));
        } else {
            genreLayout.setBackgroundResource(R.drawable.list_selector_holo_dark);
            labelLayout.setBackgroundResource(R.drawable.list_selector_holo_dark);
            artistLayout.setBackgroundResource(R.drawable.list_selector_holo_dark);
        }
        genreLayout.setFocusable(true);
        labelLayout.setFocusable(true);
        artistLayout.setFocusable(true);
        albumArt = (ImageView) findViewById(R.id.albumart);
        trackList = (ListView) findViewById(R.id.trklist);
        ratingBar = (RatingBar) findViewById(R.id.rbar);
        trackList.setOnItemClickListener(this);
        reviewsButton = (ImageView) findViewById(R.id.reviewsbutton);
        if (vLoaded) {
            handlerSetContent.sendEmptyMessage(0);
            if (vArtExists) {
                albumArt.setImageBitmap(albumArtBitmap);
            } else {
                albumArt.setImageResource(R.drawable.noalbum);
                ;
            }
        }
    }

    public void onItemClick(AdapterView<?> a, View v, int position, long id) {
    }
}
