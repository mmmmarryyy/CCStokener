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
import android.content.SharedPreferences;
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
import android.preference.PreferenceManager;
import android.util.Log;
import android.view.KeyEvent;
import android.view.View;
import android.view.Window;
import android.webkit.WebView;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.RatingBar;
import android.widget.ScrollView;
import android.widget.TextView;
import android.widget.Toast;

public class SingleBook extends Activity {

    private Activity thisActivity;

    private ImageView albumArt;

    private TextView nameTextView;

    private TextView authorTextView;

    private TextView editionTextView;

    private TextView genreTextView;

    private WebView bioBlurb;

    private LinearLayout authorLayout;

    private RatingBar ratingBar;

    private String albumId;

    private String artist;

    private String authorId;

    private String album;

    private String genre = "";

    private String publisher;

    private String narrator;

    private String date;

    private String mp3Address;

    private String rating;

    private String bioFontSize;

    private String urlAddress;

    private String emusicURL;

    private String edition = "";

    private String imageURL;

    private String blurb;

    private String blurbSource;

    private String sampleURL = "";

    private Boolean vArtExists = false;

    private Boolean vKilled = false;

    private Boolean vLoaded = false;

    private Bitmap albumArtBitmap;

    private int statuscode = 200;

    private int version;

    /** Called when the activity is first created. */
    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        requestWindowFeature(Window.FEATURE_INDETERMINATE_PROGRESS);
        setContentView(R.layout.singlebook);
        Intent myIntent = getIntent();
        albumId = myIntent.getStringExtra("keyalbumid");
        emusicURL = myIntent.getStringExtra("keyexturl");
        album = myIntent.getStringExtra("keyalbum");
        artist = myIntent.getStringExtra("keyartist");
        thisActivity = this;
        nameTextView = (TextView) findViewById(R.id.tname);
        authorTextView = (TextView) findViewById(R.id.tauthor);
        editionTextView = (TextView) findViewById(R.id.tedition);
        genreTextView = (TextView) findViewById(R.id.tgenre);
        bioBlurb = (WebView) findViewById(R.id.blurb);
        ratingBar = (RatingBar) findViewById(R.id.rbar);
        albumArt = (ImageView) findViewById(R.id.albumart);
        authorLayout = (LinearLayout) findViewById(R.id.llauthor);
        version = android.os.Build.VERSION.SDK_INT;
        Resources res = getResources();
        if (version < 11) {
            authorLayout.setBackgroundDrawable(res.getDrawable(android.R.drawable.list_selector_background));
        } else {
            authorLayout.setBackgroundResource(R.drawable.list_selector_holo_dark);
        }
        authorLayout.setFocusable(true);
        bioBlurb.setBackgroundColor(0);
        urlAddress = "http://api.emusic.com/book/info?" + Secrets.apikey + "&bookId=" + albumId + "&include=bookEditorial,bookRating&&imageSize=small";
        Log.d("EMD - ", urlAddress);
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
                    XMLHandlerSingleBook myXMLHandler = new XMLHandlerSingleBook();
                    xr.setContentHandler(myXMLHandler);
                    xr.parse(new InputSource(url.openStream()));
                    genre = myXMLHandler.genre;
                    publisher = myXMLHandler.publisher;
                    narrator = myXMLHandler.narrator;
                    edition = myXMLHandler.edition;
                    artist = myXMLHandler.author;
                    authorId = myXMLHandler.authorId;
                    if (edition == null) {
                        edition = "";
                    }
                    date = myXMLHandler.releaseDate;
                    rating = myXMLHandler.rating;
                    sampleURL = myXMLHandler.sampleURL;
                    imageURL = myXMLHandler.imageURL;
                    statuscode = myXMLHandler.statuscode;
                    if (statuscode != 200 && statuscode != 206) {
                        throw new Exception();
                    }
                    blurb = myXMLHandler.blurb;
                    blurb = blurb.replace("<br> ", "<br>");
                    blurbSource = myXMLHandler.blurbSource;
                    handlerSetContent.sendEmptyMessage(0);
                    dialog.dismiss();
                    updateImage();
                } catch (Exception e) {
                    final Exception ef = e;
                    nameTextView.post(new Runnable() {

                        public void run() {
                            nameTextView.setText(R.string.couldnt_get_book_info);
                        }
                    });
                    dialog.dismiss();
                }
                handlerDoneLoading.sendEmptyMessage(0);
            }
        };
        t3.start();
    }

    private Handler handlerSetContent = new Handler() {

        @Override
        public void handleMessage(Message msg) {
            vLoaded = true;
            String namestring = album;
            String authorstring = artist;
            String editionstring = "Edition: " + edition;
            String genrestring = "Genre: " + genre;
            try {
                namestring = StringEscapeUtils.unescapeHtml(namestring);
                authorstring = StringEscapeUtils.unescapeHtml(authorstring);
                editionstring = StringEscapeUtils.unescapeHtml(editionstring);
                genrestring = StringEscapeUtils.unescapeHtml(genrestring);
            } catch (Exception em) {
                Log.e("EMD - URLDecode", "");
            }
            try {
                ratingBar.setRating(Float.parseFloat(rating));
            } catch (Exception ef) {
            }
            nameTextView.setText(namestring);
            authorTextView.setText(authorstring);
            editionTextView.setText(editionstring);
            genreTextView.setText(genrestring);
            String blurbt = "<font color=white size=" + bioFontSize + "><b>Narrated by</b> " + narrator + " <br><br><b>Description:</b> " + blurb + " - Source: " + blurbSource + "</font>";
            blurbt = blurbt.replaceAll("<a href.[^<]*>", "");
            bioBlurb.loadDataWithBaseURL(null, blurbt, "text/html", "utf-8", "about:blank");
        }
    };

    private Handler handlerDoneLoading = new Handler() {

        @Override
        public void handleMessage(Message msg) {
            setProgressBarIndeterminateVisibility(false);
        }
    };

    private String fixQuotes(String input) {
        char ctemp = 8220;
        String output = input.replaceAll(Character.toString(ctemp), "\"");
        ctemp = 8217;
        output = output.replaceAll(Character.toString(ctemp), "%27");
        ctemp = 8221;
        output = output.replaceAll(Character.toString(ctemp), "\"");
        return output;
    }

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
        startActivity(myIntent);
    }

    public void reviewsButtonPressed(View button) {
        Intent myIntent = new Intent(this, ReviewList.class);
        String stringtitle = "User reviews of " + album;
        myIntent.putExtra("keytitle", stringtitle);
        String urlad = "http://api.emusic.com/book/reviews?" + Secrets.apikey + "&bookId=" + albumId;
        myIntent.putExtra("keyurl", urlad);
        startActivity(myIntent);
    }

    public void authorButtonPressed(View button) {
        Intent myIntent = new Intent(this, SearchListWindow.class);
        String stringtype = "book";
        myIntent.putExtra("keytype", stringtype);
        String stringtitle = "Books by " + artist;
        myIntent.putExtra("keytitle", stringtitle);
        String urlad = "http://api.emusic.com/book/charts?" + Secrets.apikey + "&authorId=" + authorId;
        myIntent.putExtra("keyurl", urlad);
        String totalsearch = "authorId=" + authorId;
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
            }
        } catch (IOException e) {
            vArtExists = false;
            final IOException ef = e;
            final String urlf = url;
        }
        return bm;
    }

    public void sampleButtonPressed(View button) {
        Log.d("EMD - ", "Attempting to play sample");
        if (sampleURL.contains("sample")) {
            final ProgressDialog dialog = ProgressDialog.show(this, "", getString(R.string.getting_sample_loation), true, true);
            Thread t5 = new Thread() {

                public void run() {
                    String addresstemp = "";
                    try {
                        URL u = new URL(sampleURL);
                        HttpURLConnection c = (HttpURLConnection) u.openConnection();
                        c.setRequestMethod("GET");
                        c.setFollowRedirects(true);
                        c.setInstanceFollowRedirects(true);
                        c.connect();
                        InputStream in = c.getInputStream();
                        InputStreamReader inputreader = new InputStreamReader(in);
                        BufferedReader buffreader = new BufferedReader(inputreader);
                        String line;
                        while ((line = buffreader.readLine()).length() > 0) {
                            if (line.contains(".mp3") || line.contains("samples.emusic") || line.contains("samples.nl.emusic")) {
                                addresstemp = line;
                                mp3Address = addresstemp;
                                if (!vKilled && dialog.isShowing()) {
                                    handlerPlay.sendEmptyMessage(0);
                                }
                                if (dialog.isShowing()) {
                                    dialog.dismiss();
                                }
                            }
                        }
                        in.close();
                        c.disconnect();
                        if (dialog.isShowing()) {
                            dialog.dismiss();
                        }
                    } catch (Exception ef) {
                        Log.d("EMD - ", "Getting mp3 address failed ");
                        if (dialog.isShowing()) {
                            dialog.dismiss();
                        }
                    }
                }
            };
            t5.start();
        } else {
            Toast.makeText(thisActivity, R.string.no_sample_available, Toast.LENGTH_SHORT).show();
        }
    }

    private Handler handlerPlay = new Handler() {

        @Override
        public void handleMessage(Message msg) {
            if (!vKilled) {
                Dialog dialog = new Dialog(thisActivity) {

                    Dialog thisDialog = this;

                    LinearLayout dialogLinLay = new LinearLayout(thisActivity);

                    ScrollView dialogScrollView = new ScrollView(thisActivity);

                    TextView sampleInfoTextView = new TextView(thisActivity);

                    private MediaPlayer sampleMediaPlayer = new MediaPlayer();

                    boolean vDownloaded;

                    Thread t4;

                    String storagePath;

                    public boolean onKeyDown(int keyCode, KeyEvent event) {
                        if (keyCode == KeyEvent.KEYCODE_BACK) {
                            Thread t7 = new Thread() {

                                public void run() {
                                    try {
                                        t4.interrupt();
                                    } catch (Exception ef) {
                                        Log.e("EMD - ", "Cant stop thread");
                                    }
                                    try {
                                        sampleMediaPlayer.stop();
                                        sampleMediaPlayer.release();
                                    } catch (Exception ef) {
                                        Log.e("EMD - ", "Cant stop playback");
                                    }
                                }
                            };
                            t7.start();
                            this.dismiss();
                            return true;
                        }
                        return false;
                    }

                    public void downloadSample() {
                        try {
                            vDownloaded = false;
                            waiting(100);
                            sampleInfoTextView.post(new Runnable() {

                                public void run() {
                                    sampleInfoTextView.setText(getString(R.string.buffering) + " " + getString(R.string._3g_wifi_suggested));
                                }
                            });
                            storagePath = Environment.getExternalStorageDirectory() + "/emxsamples";
                            final String filePath = storagePath;
                            final String fileName = "lastsample.mp3";
                            File futureDirectory = new File(filePath);
                            futureDirectory.mkdir();
                            if (futureDirectory.exists()) {
                            } else {
                                throw new Exception();
                            }
                            File noMediaFile = new File(filePath + "/.nomedia");
                            noMediaFile.createNewFile();
                            try {
                                File oldSampleFile = new File(Environment.getExternalStorageDirectory() + "/eMusic/lastsample.mp3");
                                if (oldSampleFile.exists()) {
                                    oldSampleFile.delete();
                                }
                            } catch (Exception ef) {
                            }
                            File futureFile = new File(filePath, fileName);
                            long itmp = 0;
                            itmp = futureFile.length();
                            URL u = new URL(mp3Address);
                            HttpURLConnection mp3HttpConnection = (HttpURLConnection) u.openConnection();
                            mp3HttpConnection.setRequestMethod("GET");
                            mp3HttpConnection.setDoOutput(true);
                            mp3HttpConnection.connect();
                            long itfs = 0;
                            itfs = mp3HttpConnection.getContentLength();
                            Log.d("EMD - ", "CONTENT LENGTH " + itfs);
                            if (itfs == -1) {
                                mp3HttpConnection.disconnect();
                                mp3HttpConnection = (HttpURLConnection) u.openConnection();
                                mp3HttpConnection.setRequestMethod("GET");
                                mp3HttpConnection.setDoOutput(true);
                                mp3HttpConnection.connect();
                                itfs = mp3HttpConnection.getContentLength();
                            }
                            FileOutputStream bufferFile = new FileOutputStream(futureFile, false);
                            final long ifs = itfs;
                            final long jfs = ifs * 100 / 1024 / 1024;
                            Log.d("EMD - ", "ifs " + ifs);
                            InputStream in = mp3HttpConnection.getInputStream();
                            long i = 0;
                            byte[] buffer = new byte[1024];
                            int len1 = 0;
                            long jtprev = -1;
                            while ((len1 = in.read(buffer)) > 0) {
                                if (vKilled || !thisDialog.isShowing()) {
                                    break;
                                }
                                bufferFile.write(buffer, 0, len1);
                                i += len1;
                                long jt = 100 * i / 1024 / 1024;
                                final double rfs = (double) jt / 100.0;
                                if (jt != jtprev) {
                                    jtprev = jt;
                                    final int j = (int) jt;
                                    sampleInfoTextView.post(new Runnable() {

                                        public void run() {
                                            sampleInfoTextView.setText(getString(R.string.buffering) + " " + rfs + " " + getString(R.string.mb_3g_wifi_suggested_));
                                        }
                                    });
                                }
                            }
                            bufferFile.close();
                            vDownloaded = true;
                        } catch (Exception eff) {
                            Log.d("EMD - ", "Cant get sample " + eff);
                            sampleInfoTextView.post(new Runnable() {

                                public void run() {
                                    sampleInfoTextView.setText(R.string.no_storage_to_buffer);
                                }
                            });
                        }
                    }

                    public void startTimer() {
                        t4 = new Thread() {

                            public void run() {
                                try {
                                    sleep(60000);
                                } catch (Exception ef) {
                                }
                                try {
                                    sampleMediaPlayer.stop();
                                    sampleMediaPlayer.release();
                                } catch (Exception ef) {
                                }
                                try {
                                    thisDialog.dismiss();
                                } catch (Exception ef) {
                                }
                            }
                        };
                        t4.start();
                    }

                    public void show() {
                        dialogLinLay.setOrientation(1);
                        dialogLinLay.setPadding(16, 0, 16, 16);
                        dialogLinLay.addView(sampleInfoTextView);
                        dialogScrollView.addView(dialogLinLay);
                        this.setContentView(dialogScrollView, new LinearLayout.LayoutParams(LinearLayout.LayoutParams.FILL_PARENT, LinearLayout.LayoutParams.WRAP_CONTENT));
                        super.show();
                        sampleMediaPlayer.setLooping(false);
                        MediaPlayer.OnPreparedListener listener = new MediaPlayer.OnPreparedListener() {

                            public void onPrepared(MediaPlayer mpin) {
                                if (!vKilled && thisDialog.isShowing()) {
                                    Thread t8 = new Thread() {

                                        public void run() {
                                            try {
                                                sampleMediaPlayer.start();
                                            } catch (Exception ef) {
                                            }
                                        }
                                    };
                                    t8.start();
                                    sampleInfoTextView.setText(getString(R.string.now_playing) + " " + album + " " + getString(R.string._3g_wifi_suggested));
                                    startTimer();
                                }
                            }
                        };
                        MediaPlayer.OnCompletionListener complistener = new MediaPlayer.OnCompletionListener() {

                            public void onCompletion(MediaPlayer mpin) {
                                Log.d("EMD - ", "Completed");
                                try {
                                    sampleMediaPlayer.release();
                                } catch (Exception ef) {
                                }
                                thisDialog.dismiss();
                            }
                        };
                        MediaPlayer.OnErrorListener errorlistener = new MediaPlayer.OnErrorListener() {

                            public boolean onError(MediaPlayer mpin, int what, int extra) {
                                Log.d("EMD - ", "Error");
                                return true;
                            }
                        };
                        MediaPlayer.OnInfoListener infolistener = new MediaPlayer.OnInfoListener() {

                            public boolean onInfo(MediaPlayer mpin, int what, int extra) {
                                Log.d("EMD - ", "Info");
                                return true;
                            }
                        };
                        MediaPlayer.OnBufferingUpdateListener bufferlistener = new MediaPlayer.OnBufferingUpdateListener() {

                            public void onBufferingUpdate(MediaPlayer mpin, int percent) {
                                Log.d("EMD - ", "Buffer " + percent);
                            }
                        };
                        sampleMediaPlayer.setAudioStreamType(AudioManager.STREAM_MUSIC);
                        sampleMediaPlayer.setOnPreparedListener(listener);
                        sampleMediaPlayer.setOnCompletionListener(complistener);
                        sampleMediaPlayer.setOnErrorListener(errorlistener);
                        sampleMediaPlayer.setOnInfoListener(infolistener);
                        sampleMediaPlayer.setOnBufferingUpdateListener(bufferlistener);
                        if (!vKilled && thisDialog.isShowing()) {
                            Thread t12 = new Thread() {

                                public void run() {
                                    try {
                                        downloadSample();
                                        mp3Address = "file:/" + storagePath + "/lastsample.mp3";
                                        if (!vKilled && thisDialog.isShowing() && vDownloaded) {
                                            File file = new File(storagePath + "/lastsample.mp3");
                                            FileInputStream fis = new FileInputStream(file);
                                            sampleMediaPlayer.setDataSource(fis.getFD());
                                            sampleMediaPlayer.prepareAsync();
                                        }
                                    } catch (Exception ef) {
                                        Log.e("EMD - ", "MediaPlayer Failed");
                                    }
                                }
                            };
                            t12.start();
                        } else {
                            try {
                                sampleMediaPlayer.release();
                            } catch (Exception ef) {
                            }
                        }
                    }
                };
                dialog.setTitle(R.string.audio_sample);
                dialog.show();
            }
        }
    };

    @Override
    public void onDestroy() {
        super.onDestroy();
        vKilled = true;
        Log.d("EMD Book - ", "Destroyed");
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
        setContentView(R.layout.singlebook);
        nameTextView = (TextView) findViewById(R.id.tname);
        authorTextView = (TextView) findViewById(R.id.tauthor);
        editionTextView = (TextView) findViewById(R.id.tedition);
        genreTextView = (TextView) findViewById(R.id.tgenre);
        bioBlurb = (WebView) findViewById(R.id.blurb);
        ratingBar = (RatingBar) findViewById(R.id.rbar);
        albumArt = (ImageView) findViewById(R.id.albumart);
        bioBlurb.setBackgroundColor(0);
        authorLayout = (LinearLayout) findViewById(R.id.llauthor);
        Resources res = getResources();
        if (version < 11) {
            authorLayout.setBackgroundDrawable(res.getDrawable(android.R.drawable.list_selector_background));
        } else {
            authorLayout.setBackgroundResource(R.drawable.list_selector_holo_dark);
        }
        authorLayout.setFocusable(true);
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
}
