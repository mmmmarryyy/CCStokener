package com.commonsware.android.EMusicDownloader;

import java.net.URL;
import java.util.HashMap;
import java.util.List;
import javax.xml.parsers.SAXParser;
import javax.xml.parsers.SAXParserFactory;
import org.xml.sax.InputSource;
import org.xml.sax.XMLReader;
import android.net.Uri;
import android.app.Activity;
import android.app.Dialog;
import android.content.Intent;
import android.content.res.Configuration;
import android.content.res.Resources;
import android.graphics.Bitmap;
import android.os.Bundle;
import android.os.Handler;
import android.os.Message;
import android.preference.PreferenceManager;
import android.util.Log;
import android.view.KeyEvent;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.view.Window;
import android.view.View.OnClickListener;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.LinearLayout;
import android.widget.ListView;
import android.widget.ScrollView;
import android.widget.TabHost;
import android.widget.TextView;
import android.widget.ImageView;
import android.widget.Toast;
import android.content.SharedPreferences;

public class MainWindow extends Activity implements AdapterView.OnItemClickListener {

    public MainWindow thisActivity;

    private int statuscode = 200;

    private TextView errorTextView;

    private ListView albumList;

    private ListView artistList;

    private ListView bookList;

    private ImageView searchButton;

    private LinearLayout loadingProgBarLayout;

    private TabHost tabHost;

    private int numberOfResults = 0;

    private String[] albums;

    private String[] books;

    private String[] artists;

    private String[] displayAlbums;

    private String[] displayBooks;

    private String[] albumURLs;

    private String[] bookURLs;

    private String[] artistURLs;

    private String[] albumArtists;

    private String[] authors;

    private String[] albumIds;

    private String[] bookIds;

    private String[] artistIds;

    private String[] albumImages;

    private String[] bookImages;

    private String countryCode;

    private List<AlbumCache> albumCaches;

    private List<BookCache> bookCaches;

    private List<ArtistCache> artistCaches;

    private emuDB droidDB;

    private HashMap<String, Bitmap> albumBitmapHash = new HashMap<String, Bitmap>();

    private HashMap<String, Bitmap> bookBitmapHash = new HashMap<String, Bitmap>();

    private boolean vArtistCacheSaved = false;

    private boolean vAlbumCacheSaved = false;

    private boolean vBookCacheSaved = false;

    private LazyAdapter albumAdapter;

    private LazyAdapter bookAdapter;

    public static final int ABOUT_ID = Menu.FIRST + 1;

    public static final int DONATE_ID = Menu.FIRST + 2;

    public static final int SAVED_ID = Menu.FIRST + 3;

    public static final int FREE_ID = Menu.FIRST + 4;

    public static final int CLEAR_ID = Menu.FIRST + 7;

    public static final int SIGNUP_ID = Menu.FIRST + 5;

    public static final int PREF_ID = Menu.FIRST + 6;

    String[] XMLAddressOrig = { "http://api.emusic.com/album/charts?filter=downloadedToday&primarySort=albumDownloadsToday&" + Secrets.apikey + "&imageSize=thumbnail&country=", "http://api.emusic.com/book/charts?primarySort=bookDownloadsToday&" + Secrets.apikey + "&imageSize=thumbnail&country=", "http://api.emusic.com/artist/charts?filter=downloadedToday&primarySort=artistDownloadsToday&" + Secrets.apikey + "&country=" };

    String[] XMLAddress;

    /** Called when the activity is first created. */
    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        requestWindowFeature(Window.FEATURE_INDETERMINATE_PROGRESS);
        setContentView(R.layout.main);
        setProgressBarIndeterminateVisibility(false);
        Resources res = getResources();
        searchButton = (ImageView) findViewById(R.id.searchbutton);
        errorTextView = (TextView) findViewById(R.id.terror);
        albumList = (ListView) findViewById(R.id.alblist);
        artistList = (ListView) findViewById(R.id.artlist);
        bookList = (ListView) findViewById(R.id.bklist);
        loadingProgBarLayout = (LinearLayout) findViewById(R.id.pbarll);
        albumList.setOnItemClickListener(this);
        artistList.setOnItemClickListener(this);
        bookList.setOnItemClickListener(this);
        int version = android.os.Build.VERSION.SDK_INT;
        tabHost = (TabHost) findViewById(R.id.tabhost);
        tabHost.setup();
        TabHost.TabSpec spec = tabHost.newTabSpec("tag1");
        spec.setContent(R.id.alblist);
        if (version > 4) {
            spec.setIndicator(getString(R.string.top_albums), res.getDrawable(R.drawable.album));
        } else {
            spec.setIndicator(getString(R.string.top_albums), res.getDrawable(R.drawable.albumold));
        }
        tabHost.addTab(spec);
        spec = tabHost.newTabSpec("tag2");
        spec.setContent(R.id.artlist);
        if (version > 4) {
            spec.setIndicator(getString(R.string.top_artists), res.getDrawable(R.drawable.fav));
        } else {
            spec.setIndicator(getString(R.string.top_artists), res.getDrawable(R.drawable.favold));
        }
        tabHost.addTab(spec);
        spec = tabHost.newTabSpec("tag3");
        spec.setContent(R.id.bklist);
        if (version > 4) {
            spec.setIndicator(getString(R.string.top_books), res.getDrawable(R.drawable.book));
        } else {
            spec.setIndicator(getString(R.string.top_books), res.getDrawable(R.drawable.bookold));
        }
        tabHost.addTab(spec);
        thisActivity = this;
        SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(this);
        countryCode = prefs.getString("localelist", "US");
        XMLAddress = new String[3];
        XMLAddress[0] = XMLAddressOrig[0] + countryCode;
        XMLAddress[1] = XMLAddressOrig[1] + countryCode;
        XMLAddress[2] = XMLAddressOrig[2] + countryCode;
        loadListsFromCache();
        getListFromXML();
    }

    public void onItemClick(AdapterView<?> a, View v, int position, long id) {
        if (a.getId() == R.id.alblist) {
            Intent myIntent = new Intent(this, SingleAlbum.class);
            myIntent.putExtra("keyalbumid", albumIds[position]);
            myIntent.putExtra("keyalbum", albums[position]);
            myIntent.putExtra("keyartist", albumArtists[position]);
            myIntent.putExtra("keyexturl", albumURLs[position]);
            startActivity(myIntent);
        } else if (a.getId() == R.id.bklist) {
            Intent myIntent = new Intent(this, SingleBook.class);
            myIntent.putExtra("keyalbumid", bookIds[position]);
            myIntent.putExtra("keyalbum", books[position]);
            myIntent.putExtra("keyartist", authors[position]);
            myIntent.putExtra("keyexturl", bookURLs[position]);
            startActivity(myIntent);
        } else if (a.getId() == R.id.artlist) {
            Intent myIntent = new Intent(this, SingleArtist.class);
            myIntent.putExtra("keyartistid", artistIds[position]);
            startActivity(myIntent);
        }
    }

    @Override
    protected void onResume() {
        super.onResume();
        SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(this);
        String countryCodeNew = prefs.getString("localelist", "US");
        if (!countryCodeNew.equals(countryCode)) {
            countryCode = countryCodeNew;
            XMLAddress[0] = XMLAddressOrig[0] + countryCode;
            XMLAddress[1] = XMLAddressOrig[1] + countryCode;
            XMLAddress[2] = XMLAddressOrig[2] + countryCode;
            long currenttime = 0;
            emuDB droidDB2 = new emuDB(thisActivity);
            if (droidDB2.isLocked()) {
                Toast.makeText(this, R.string.database_locked, Toast.LENGTH_SHORT).show();
            } else {
                droidDB2.updateCachetime(currenttime);
                droidDB2.close();
                if (loadingProgBarLayout.getVisibility() != 0) {
                    Toast.makeText(this, "Region Changed. Reloading Charts.", Toast.LENGTH_SHORT).show();
                    getListFromXML();
                } else {
                    Toast.makeText(this, "Region Changed. Please exit and re-enter the app.", Toast.LENGTH_SHORT).show();
                }
            }
        }
        Log.d("EMD - ", "Resumed main");
    }

    public void loadListsFromCache() {
        try {
            droidDB = new emuDB(this);
            numberOfResults = droidDB.getAlbumCacheSize();
            if (numberOfResults > 0) {
                albumCaches = droidDB.getAlbumCache();
                albums = new String[numberOfResults];
                displayAlbums = new String[numberOfResults];
                albumArtists = new String[numberOfResults];
                albumURLs = new String[numberOfResults];
                albumIds = new String[numberOfResults];
                albumImages = new String[numberOfResults];
                int i = 0;
                for (AlbumCache albumcache : albumCaches) {
                    albums[i] = albumcache.albumname;
                    displayAlbums[i] = albumcache.albumdisplay;
                    albumImages[i] = albumcache.albumartist;
                    albumURLs[i] = albumcache.albumurl;
                    albumIds[i] = albumcache.emusicid;
                    i++;
                }
                try {
                    albumAdapter.imageLoader.stopThread();
                    albumAdapter = null;
                } catch (Exception baef) {
                }
                albumAdapter = new LazyAdapter(thisActivity, albumImages, displayAlbums, albumBitmapHash);
                albumList.setAdapter(albumAdapter);
            } else {
                Log.d("EMD - ", "No album cache available to load");
            }
            numberOfResults = droidDB.getBookCacheSize();
            if (numberOfResults > 0) {
                bookCaches = droidDB.getBookCache();
                books = new String[numberOfResults];
                displayBooks = new String[numberOfResults];
                authors = new String[numberOfResults];
                bookURLs = new String[numberOfResults];
                bookIds = new String[numberOfResults];
                bookImages = new String[numberOfResults];
                int i = 0;
                for (BookCache bookcache : bookCaches) {
                    books[i] = bookcache.bookname;
                    displayBooks[i] = bookcache.bookdisplay;
                    bookImages[i] = bookcache.bookauthor;
                    bookURLs[i] = bookcache.bookurl;
                    bookIds[i] = bookcache.emusicid;
                    i++;
                }
                try {
                    bookAdapter.imageLoader.stopThread();
                    bookAdapter = null;
                } catch (Exception baef) {
                }
                bookAdapter = new LazyAdapter(thisActivity, bookImages, displayBooks, bookBitmapHash);
                bookList.setAdapter(bookAdapter);
            } else {
                Log.d("EMD - ", "No book cache available to load");
            }
            numberOfResults = droidDB.getArtistCacheSize();
            if (numberOfResults > 0) {
                artistCaches = droidDB.getArtistCache();
                artists = new String[numberOfResults];
                artistURLs = new String[numberOfResults];
                artistIds = new String[numberOfResults];
                int i = 0;
                for (ArtistCache artistcache : artistCaches) {
                    artists[i] = artistcache.artistname;
                    artistURLs[i] = artistcache.artisturl;
                    artistIds[i] = artistcache.emusicid;
                    i++;
                }
                artistList.setAdapter(new ArrayAdapter<String>(thisActivity, R.layout.artistlist_item, R.id.text, artists));
            } else {
                Log.d("EMD - ", "No artist cache available to load");
            }
            droidDB.close();
        } catch (Exception ef) {
            Log.d("EMD - ", "Failed to load cache");
        }
    }

    public void searchPressed(View buttoncover) {
        Intent myIntent = new Intent(this, SearchWindow.class);
        startActivity(myIntent);
    }

    public void browsePressed(View buttoncover) {
        Intent myIntent = new Intent(this, Browse.class);
        startActivity(myIntent);
    }

    public void logoPressed(View buttoncover) {
        String browseurl = "http://www.emusic.com?fref=400062";
        Intent browseIntent = new Intent(this, WebWindowBrowse.class);
        browseIntent.putExtra("keyurl", browseurl);
        startActivity(browseIntent);
    }

    public void dmPressed(View buttoncover) {
        Intent myIntent = new Intent(this, DownloadManager.class);
        startActivity(myIntent);
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        populateMenu(menu);
        return (super.onCreateOptionsMenu(menu));
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        return (applyMenuChoice(item) || super.onOptionsItemSelected(item));
    }

    private void populateMenu(Menu menu) {
        menu.add(Menu.NONE, ABOUT_ID, Menu.NONE, R.string.about);
        menu.add(Menu.NONE, DONATE_ID, Menu.NONE, R.string.donate);
        menu.add(Menu.NONE, SAVED_ID, Menu.NONE, R.string.saved_items);
        menu.add(Menu.NONE, FREE_ID, Menu.NONE, R.string.free_daily_download);
        menu.add(Menu.NONE, SIGNUP_ID, Menu.NONE, R.string.signup);
        menu.add(Menu.NONE, PREF_ID, Menu.NONE, R.string.preferences);
    }

    private boolean applyMenuChoice(MenuItem item) {
        switch(item.getItemId()) {
            case ABOUT_ID:
                ScrollView scwv = new ScrollView(this);
                TextView wv = new TextView(this);
                wv.setPadding(16, 0, 16, 16);
                wv.setText(R.string.about_text);
                scwv.addView(wv);
                Dialog dialog = new Dialog(this) {

                    public boolean onKeyDown(int keyCode, KeyEvent event) {
                        if (keyCode != KeyEvent.KEYCODE_DPAD_LEFT) this.dismiss();
                        return true;
                    }
                };
                dialog.setTitle(R.string.about_foss_emusic);
                dialog.addContentView(scwv, new LinearLayout.LayoutParams(LinearLayout.LayoutParams.FILL_PARENT, LinearLayout.LayoutParams.FILL_PARENT));
                dialog.show();
                return (true);
            case SIGNUP_ID:
                String signurl = "https://www.emusic.com/registration/1.html?fref=400062";
                Intent mySignIntent = new Intent(this, WebWindowBrowse.class);
                mySignIntent.putExtra("keyurl", signurl);
                startActivity(mySignIntent);
                return true;
            case CLEAR_ID:
                LazyAdapter adapter;
                adapter = new LazyAdapter(thisActivity);
                adapter.imageLoader.clearCache();
                adapter = null;
                long currenttime = 0;
                emuDB droidDB2 = new emuDB(thisActivity);
                if (droidDB2.isLocked()) {
                    Toast.makeText(this, R.string.database_locked, Toast.LENGTH_SHORT).show();
                } else {
                    droidDB2.updateCachetime(currenttime);
                    droidDB2.updateArttime(currenttime);
                    droidDB2.close();
                    Toast.makeText(this, R.string.cleared_cache, Toast.LENGTH_SHORT).show();
                }
                return true;
            case DONATE_ID:
                Intent goToMarket = null;
                goToMarket = new Intent(Intent.ACTION_VIEW, Uri.parse("market://details?id=com.jd.android.droidianemusic"));
                try {
                    startActivity(goToMarket);
                } catch (Exception ef) {
                    Toast.makeText(this, "Market Not Installed", Toast.LENGTH_SHORT).show();
                }
                return true;
            case SAVED_ID:
                String savedurl = "http://www.emusic.com/profile/saveforlater.html?fref=400062";
                Intent myIntent = new Intent(this, WebWindowBrowse.class);
                myIntent.putExtra("keyurl", savedurl);
                startActivity(myIntent);
                return true;
            case FREE_ID:
                String freeurl = "http://www.emusic.com/dailydownloads/toolbar/main.html?fref=400062";
                Intent myFreeIntent = new Intent(this, WebWindow.class);
                myFreeIntent.putExtra("keyurl", freeurl);
                startActivity(myFreeIntent);
                return true;
            case PREF_ID:
                startActivity(new Intent(this, EditPreferences.class));
                return true;
        }
        return (false);
    }

    private void getListFromXML() {
        Thread ta = new Thread() {

            public void run() {
                try {
                    handlerBeginLoading.sendEmptyMessage(0);
                    long currenttime = System.currentTimeMillis();
                    droidDB = new emuDB(thisActivity);
                    long cachetime = droidDB.getCachetime();
                    long arttime = droidDB.getArttime();
                    if ((currenttime - arttime) >= 604800000) {
                        droidDB.updateArttime(currenttime);
                        LazyAdapter adapter = new LazyAdapter(thisActivity);
                        adapter.imageLoader.clearCache();
                        adapter = null;
                    }
                    droidDB.close();
                    if ((currenttime - cachetime) > 14400000) {
                        XMLHandlerAlbums myXMLHandler = null;
                        XMLHandlerArtists myXMLHandlerArt = null;
                        for (int ilist = 0; ilist < 3; ilist++) {
                            URL url = new URL(XMLAddress[ilist]);
                            SAXParserFactory spf = SAXParserFactory.newInstance();
                            SAXParser sp = spf.newSAXParser();
                            XMLReader xr = sp.getXMLReader();
                            if (ilist < 2) {
                                myXMLHandler = new XMLHandlerAlbums(ilist);
                            } else {
                                myXMLHandlerArt = new XMLHandlerArtists();
                            }
                            Message handlermsg = new Message();
                            handlermsg.arg1 = ilist;
                            if (ilist == 0) {
                                xr.setContentHandler(myXMLHandler);
                                xr.parse(new InputSource(url.openStream()));
                                numberOfResults = myXMLHandler.nItems;
                                handlermsg.arg2 = numberOfResults;
                                statuscode = myXMLHandler.statuscode;
                                if (statuscode != 200 && statuscode != 206) {
                                    throw new Exception();
                                }
                                albums = new String[numberOfResults];
                                displayAlbums = new String[numberOfResults];
                                albumArtists = new String[numberOfResults];
                                albumURLs = new String[numberOfResults];
                                albumIds = new String[numberOfResults];
                                albumImages = new String[numberOfResults];
                                for (int i = 0; i < numberOfResults; i++) {
                                    albums[i] = myXMLHandler.albums[i];
                                    albumArtists[i] = myXMLHandler.artists[i];
                                    albumIds[i] = myXMLHandler.albumIds[i];
                                    albumImages[i] = myXMLHandler.images[i];
                                    displayAlbums[i] = albums[i] + " - " + albumArtists[i];
                                    albumURLs[i] = myXMLHandler.urls[i];
                                }
                                vAlbumCacheSaved = true;
                            } else if (ilist == 1) {
                                xr.setContentHandler(myXMLHandler);
                                xr.parse(new InputSource(url.openStream()));
                                numberOfResults = myXMLHandler.nItems;
                                handlermsg.arg2 = numberOfResults;
                                statuscode = myXMLHandler.statuscode;
                                if (statuscode != 200) {
                                    throw new Exception();
                                }
                                books = new String[numberOfResults];
                                bookIds = new String[numberOfResults];
                                bookImages = new String[numberOfResults];
                                authors = new String[numberOfResults];
                                bookURLs = new String[numberOfResults];
                                displayBooks = new String[numberOfResults];
                                for (int i = 0; i < numberOfResults; i++) {
                                    books[i] = myXMLHandler.albums[i];
                                    authors[i] = myXMLHandler.artists[i];
                                    displayBooks[i] = books[i] + " - " + authors[i];
                                    bookIds[i] = myXMLHandler.albumIds[i];
                                    bookImages[i] = myXMLHandler.images[i];
                                    bookURLs[i] = myXMLHandler.urls[i];
                                }
                                vBookCacheSaved = true;
                            } else if (ilist == 2) {
                                xr.setContentHandler(myXMLHandlerArt);
                                xr.parse(new InputSource(url.openStream()));
                                numberOfResults = myXMLHandler.nItems;
                                handlermsg.arg2 = numberOfResults;
                                statuscode = myXMLHandler.statuscode;
                                if (statuscode != 200) {
                                    throw new Exception();
                                }
                                artists = new String[numberOfResults];
                                artistIds = new String[numberOfResults];
                                artistURLs = new String[numberOfResults];
                                for (int i = 0; i < numberOfResults; i++) {
                                    artists[i] = myXMLHandlerArt.artists[i];
                                    artistIds[i] = myXMLHandlerArt.artistsId[i];
                                    artistURLs[i] = myXMLHandlerArt.urls[i];
                                }
                                vArtistCacheSaved = true;
                            }
                            handlerSetContent.sendMessage(handlermsg);
                        }
                        errorTextView.post(new Runnable() {

                            public void run() {
                                errorTextView.setVisibility(8);
                            }
                        });
                    } else {
                        Log.d("EMD - ", "Not time to update yet");
                    }
                } catch (Exception e) {
                    Log.d("EMD - ", "Failed to get chart info - " + e);
                    errorTextView.post(new Runnable() {

                        public void run() {
                            errorTextView.setText(R.string.failed_to_update_chart_info);
                            errorTextView.setVisibility(0);
                        }
                    });
                }
                handlerDoneLoading.sendEmptyMessage(0);
            }
        };
        ta.start();
    }

    private Handler handlerSetContent = new Handler() {

        @Override
        public void handleMessage(Message msg) {
            final int mlength = msg.arg2;
            if (msg.arg1 == 0) {
                albumAdapter = new LazyAdapter(thisActivity, albumImages, displayAlbums, albumBitmapHash);
                albumList.setAdapter(albumAdapter);
            } else if (msg.arg1 == 1) {
                bookAdapter = new LazyAdapter(thisActivity, bookImages, displayBooks, bookBitmapHash);
                bookList.setAdapter(bookAdapter);
            } else if (msg.arg1 == 2) {
                artistList.setAdapter(new ArrayAdapter<String>(thisActivity, R.layout.artistlist_item, R.id.text, artists));
                if (vArtistCacheSaved && vAlbumCacheSaved && vBookCacheSaved) {
                    Thread tart = new Thread() {

                        public void run() {
                            try {
                                emuDB droidDB2 = new emuDB(thisActivity);
                                if (droidDB2.isLocked()) {
                                    Log.d("EMD - ", "DB Locked artists");
                                    try {
                                        sleep(2000);
                                    } catch (Exception eff) {
                                    }
                                }
                                albumCaches = droidDB2.getAlbumCache();
                                for (AlbumCache albumcache : albumCaches) {
                                    droidDB2.deleteAlbumCache(albumcache.albumcacheId);
                                }
                                for (int i = 0; i < mlength; i++) {
                                    droidDB2.insertAlbumCache(albums[i], albumURLs[i], albumImages[i], displayAlbums[i], albumIds[i]);
                                }
                                bookCaches = droidDB2.getBookCache();
                                for (BookCache bookcache : bookCaches) {
                                    droidDB2.deleteBookCache(bookcache.bookcacheId);
                                }
                                for (int i = 0; i < mlength; i++) {
                                    droidDB2.insertBookCache(books[i], bookURLs[i], bookImages[i], displayBooks[i], bookIds[i]);
                                }
                                artistCaches = droidDB2.getArtistCache();
                                for (ArtistCache artistcache : artistCaches) {
                                    droidDB2.deleteArtistCache(artistcache.artistcacheId);
                                }
                                for (int i = 0; i < mlength; i++) {
                                    droidDB2.insertArtistCache(artists[i], artistURLs[i], artistIds[i]);
                                }
                                Log.d("EMD - ", "Correctly saved cache");
                                long currenttime = System.currentTimeMillis();
                                droidDB2.updateCachetime(currenttime);
                                droidDB2.close();
                            } catch (Exception ef) {
                                Log.d("EMD - ", "Failed to save cache");
                            }
                        }
                    };
                    tart.start();
                }
            }
        }
    };

    private Handler handlerDoneLoading = new Handler() {

        @Override
        public void handleMessage(Message msg) {
            loadingProgBarLayout.setVisibility(8);
            setProgressBarIndeterminateVisibility(false);
        }
    };

    private Handler handlerBeginLoading = new Handler() {

        @Override
        public void handleMessage(Message msg) {
            loadingProgBarLayout.setVisibility(0);
            setProgressBarIndeterminateVisibility(true);
        }
    };

    @Override
    public void onConfigurationChanged(Configuration newConfig) {
        super.onConfigurationChanged(newConfig);
        setContentView(R.layout.main);
        Resources res = getResources();
        String currenttab = tabHost.getCurrentTabTag();
        errorTextView = (TextView) findViewById(R.id.terror);
        albumList = (ListView) findViewById(R.id.alblist);
        artistList = (ListView) findViewById(R.id.artlist);
        bookList = (ListView) findViewById(R.id.bklist);
        int ivisible = loadingProgBarLayout.getVisibility();
        loadingProgBarLayout = (LinearLayout) findViewById(R.id.pbarll);
        loadingProgBarLayout.setVisibility(ivisible);
        albumList.setOnItemClickListener(this);
        artistList.setOnItemClickListener(this);
        bookList.setOnItemClickListener(this);
        int version = android.os.Build.VERSION.SDK_INT;
        tabHost = (TabHost) findViewById(R.id.tabhost);
        tabHost.setup();
        TabHost.TabSpec spec = tabHost.newTabSpec("tag1");
        spec.setContent(R.id.alblist);
        if (version > 4) {
            spec.setIndicator(getString(R.string.top_albums), res.getDrawable(R.drawable.album));
        } else {
            spec.setIndicator(getString(R.string.top_albums), res.getDrawable(R.drawable.albumold));
        }
        tabHost.addTab(spec);
        spec = tabHost.newTabSpec("tag2");
        spec.setContent(R.id.artlist);
        if (version > 4) {
            spec.setIndicator(getString(R.string.top_artists), res.getDrawable(R.drawable.fav));
        } else {
            spec.setIndicator(getString(R.string.top_artists), res.getDrawable(R.drawable.favold));
        }
        tabHost.addTab(spec);
        spec = tabHost.newTabSpec("tag3");
        spec.setContent(R.id.bklist);
        if (version > 4) {
            spec.setIndicator(getString(R.string.top_books), res.getDrawable(R.drawable.book));
        } else {
            spec.setIndicator(getString(R.string.top_books), res.getDrawable(R.drawable.bookold));
        }
        tabHost.addTab(spec);
        tabHost.setCurrentTabByTag(currenttab);
        loadListsFromCache();
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
        try {
            bookAdapter.imageLoader.stopThread();
            albumAdapter.imageLoader.stopThread();
            bookAdapter = null;
            albumAdapter = null;
        } catch (Exception ef) {
        }
        Log.d("EMD Main - ", "Destroyed");
    }
}
