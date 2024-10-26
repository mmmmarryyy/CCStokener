package com.commonsware.android.EMusicDownloader;

import java.io.BufferedInputStream;
import java.io.BufferedWriter;
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.BufferedReader;
import java.net.HttpURLConnection;
import java.net.URL;
import java.util.List;
import java.util.HashMap;
import javax.xml.parsers.SAXParser;
import javax.xml.parsers.SAXParserFactory;
import org.apache.http.HttpResponse;
import org.apache.http.client.HttpClient;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.impl.client.DefaultHttpClient;
import org.xml.sax.InputSource;
import org.xml.sax.XMLReader;
import android.app.Activity;
import android.app.AlertDialog;
import android.app.Dialog;
import android.content.ActivityNotFoundException;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.res.Configuration;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.net.Uri;
import android.os.Bundle;
import android.os.Environment;
import android.os.Handler;
import android.os.Message;
import android.preference.PreferenceManager;
import android.util.Log;
import android.view.KeyEvent;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.view.Window;
import android.view.ViewGroup.LayoutParams;
import android.widget.Button;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.ProgressBar;
import android.widget.ScrollView;
import android.widget.TextView;
import android.widget.ListView;
import android.widget.Toast;
import android.os.StatFs;
import android.widget.AdapterView;
import android.view.ContextMenu;
import android.view.ContextMenu.ContextMenuInfo;
import com.jd.oifilemanager.intents.FileManagerIntents;

public class DownloadManager extends Activity {

    public DownloadManager thisActivity;

    public ImageView pauseButton;

    public LinearLayout pbar2;

    public TextView statusTextView;

    public TextView parseTextView;

    public TextView mbTextView;

    public ListView downloadListView;

    public int nFiles;

    public int[] trackStatus;

    private int nfilesNew = -1;

    public String[] trackNames;

    public String[] trackImageURLs;

    public String[] trackURLs;

    public String[] trackNums;

    public String[] trackArtists;

    public String[] trackAlbums;

    public long[] trackIds;

    public int[] trackBookFlags;

    public String cookieString;

    public String musicPath;

    public String bookPath;

    public String statString;

    private String storageRoot;

    private String XMLAddress;

    private Bitmap albumArtBitmap;

    public static final int ABOUT_ID = Menu.FIRST + 1;

    public static final int STATUS_ID = Menu.FIRST + 2;

    public static final int PREF_ID = Menu.FIRST + 3;

    public static final int CLEAR_COMPLETE_ID = Menu.FIRST + 4;

    public static final int CLEAR_ALL_ID = Menu.FIRST + 5;

    public static final int EMX_ID = Menu.FIRST + 6;

    public Boolean vLoop = false;

    public Boolean vCancel = false;

    public Boolean vartExists = false;

    public Boolean vStorage = false;

    public Boolean vConnecting = false;

    public Context thisContext;

    public final String mimetype = "audio/mpeg";

    protected static final int REQUEST_CODE_PICK_FILE_OR_DIRECTORY = 1;

    public String fileNameSeparatorPref = null;

    public String customFileDirectoryPref = null;

    public int iFileNamePref = 0;

    public int iFileDirPref = 0;

    public int iBookDirPref = 0;

    private int megaInt;

    private List<History> historys;

    private List<Download> downloads;

    private HashMap<String, Bitmap> coverBitmapHash = new HashMap<String, Bitmap>();

    public HashMap<Long, Integer> layoutHash;

    public DownloadListAdapter adapter;

    @Override
    public void onCreate(Bundle icicle) {
        super.onCreate(icicle);
        requestWindowFeature(Window.FEATURE_INDETERMINATE_PROGRESS);
        setContentView(R.layout.downloadmanager2);
        setProgressBarIndeterminateVisibility(false);
        pauseButton = (ImageView) findViewById(R.id.pausebutton);
        pbar2 = (LinearLayout) findViewById(R.id.pbar2);
        statusTextView = (TextView) findViewById(R.id.header_textview);
        parseTextView = (TextView) findViewById(R.id.status_textview);
        mbTextView = (TextView) findViewById(R.id.mbavailable);
        downloadListView = (ListView) findViewById(R.id.downloadlist);
        statString = "Status - No current download.";
        thisActivity = this;
        thisContext = this;
        getFileStoragePaths();
        if (Utils.droidDB == null) {
            Utils.droidDB = new downloadDB(this);
        }
        if (vStorage) {
            StatFs stat = new StatFs(musicPath);
            double sdAvailSize = (double) stat.getAvailableBlocks() * (double) stat.getBlockSize();
            double megaAvailable = sdAvailSize / 1048576;
            megaInt = (int) megaAvailable;
            mbTextView.setText("" + megaInt + "MB Available" + "\nPress Menu to Clean List");
            Log.d("EMD - ", "Megabytes Available " + megaAvailable);
            if (Downloader.vLoop) {
                vLoop = true;
                if (!Downloader.vCancel) {
                    statusTextView.setText(R.string.downloading_dot);
                    pauseButton.setImageResource(R.drawable.pause);
                } else {
                    statusTextView.setText(R.string.canceling);
                    pauseButton.setImageResource(R.drawable.restart);
                }
                getFileInfoFromDownloader();
                Intent rintent = getIntent();
                if (rintent.getData() != null) {
                    if (megaInt > 20) {
                        vConnecting = true;
                        XMLAddress = rintent.getDataString();
                        cookieString = rintent.getStringExtra("keycookie");
                        getFileInfoFromXML();
                    } else {
                        Toast.makeText(thisActivity, "You need more than 20 MB free to start a new download.", Toast.LENGTH_LONG).show();
                    }
                }
            } else {
                getFileInfoFromDB();
                Intent rintent = getIntent();
                if (rintent.getData() == null) {
                } else {
                    if (rintent.getDataString().contains("DLFinished")) {
                        vLoop = false;
                    } else {
                        if (megaInt > 20) {
                            vConnecting = true;
                            XMLAddress = rintent.getDataString();
                            cookieString = rintent.getStringExtra("keycookie");
                            getFileInfoFromXML();
                        } else {
                            Toast.makeText(thisActivity, "You need more than 20 MB free to start a new download.", Toast.LENGTH_LONG).show();
                        }
                    }
                }
            }
        }
        Downloader.setMainActivity(thisActivity);
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
        Log.d("EMD", " - Destroyed");
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
        menu.add(Menu.NONE, STATUS_ID, Menu.NONE, R.string.status);
        menu.add(Menu.NONE, PREF_ID, Menu.NONE, R.string.preferences);
        menu.add(Menu.NONE, CLEAR_COMPLETE_ID, Menu.NONE, "Clear Complete");
        menu.add(Menu.NONE, CLEAR_ALL_ID, Menu.NONE, "Clear All");
        menu.add(Menu.NONE, EMX_ID, Menu.NONE, "emx Files");
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
                        if (keyCode != KeyEvent.KEYCODE_DPAD_LEFT) {
                            this.dismiss();
                        }
                        return true;
                    }
                };
                dialog.setTitle(R.string.about_the_download_manager);
                dialog.addContentView(scwv, new LinearLayout.LayoutParams(LinearLayout.LayoutParams.FILL_PARENT, LinearLayout.LayoutParams.FILL_PARENT));
                dialog.show();
                return (true);
            case STATUS_ID:
                statusDialog();
                return (true);
            case PREF_ID:
                startActivity(new Intent(this, EditPreferences.class));
                return (true);
            case CLEAR_COMPLETE_ID:
                clearComplete();
                return (true);
            case CLEAR_ALL_ID:
                historyPressed(null);
                return (true);
            case EMX_ID:
                getFileName();
                return (true);
        }
        return (false);
    }

    public void pausePressed(View pausebutton) {
        if (vLoop && !vCancel) {
            vCancel = true;
            Downloader.setvCancel(true);
            statusTextView.setText(R.string.canceling);
            pauseButton.setImageResource(R.drawable.restart);
        } else if (!vLoop) {
            vLoop = true;
            Downloader.setvLoop(true);
            vCancel = false;
            Downloader.setvCancel(false);
            statusTextView.setText(R.string.downloading_dot);
            pauseButton.setImageResource(R.drawable.pause);
            startDownloader();
        } else {
        }
    }

    public void historyPressed(View historybutton) {
        String histmessage = "Clear all downloads.";
        new AlertDialog.Builder(this).setTitle(R.string.history).setMessage(histmessage).setPositiveButton("Yes", new DialogInterface.OnClickListener() {

            @Override
            public void onClick(DialogInterface dialog, int which) {
                clearAll();
            }
        }).setNegativeButton("No", null).show();
    }

    public void coverPressed(View buttoncover) {
        statusDialog();
    }

    private void startDownloader() {
        if (vStorage) {
            Downloader.setMainActivity(thisActivity);
            Intent svc = new Intent(thisActivity, Downloader.class);
            startService(svc);
        } else {
            new AlertDialog.Builder(this).setIcon(android.R.drawable.ic_dialog_alert).setTitle(R.string.no_storage_available).setMessage(R.string.no_storage).setPositiveButton(R.string.quit, new DialogInterface.OnClickListener() {

                @Override
                public void onClick(DialogInterface dialog, int which) {
                    DownloadManager.this.finish();
                }
            }).show();
            vLoop = false;
            Downloader.setvLoop(false);
            Downloader.setvCancel(false);
        }
    }

    private void getFileName() {
        String fileNameBegin = storageRoot;
        Intent intent = new Intent(FileManagerIntents.ACTION_PICK_FILE);
        intent.setData(Uri.parse("file://" + fileNameBegin));
        intent.putExtra(FileManagerIntents.EXTRA_TITLE, getString(R.string.pick_a_file));
        intent.putExtra(FileManagerIntents.EXTRA_BUTTON_TEXT, "Open");
        try {
            startActivityForResult(intent, REQUEST_CODE_PICK_FILE_OR_DIRECTORY);
        } catch (ActivityNotFoundException e) {
            Toast.makeText(this, R.string.file_manager_missing, Toast.LENGTH_SHORT).show();
        }
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        switch(requestCode) {
            case REQUEST_CODE_PICK_FILE_OR_DIRECTORY:
                if (resultCode == RESULT_OK && data != null) {
                    XMLAddress = data.getDataString();
                    if (XMLAddress != null) {
                        vConnecting = true;
                        getFileInfoFromXML();
                    }
                }
                break;
        }
    }

    public void getStatus() {
        getFileStatus();
        int nTracksFailed = 0;
        int nTracksCompleted = 0;
        int nTracksWaiting = 0;
        int nTracksDownloading = 0;
        int nPreviousDownloads = 0;
        int nTracksExpired = 0;
        int nTracksIncomplete = 0;
        if (nFiles == 0) {
        } else {
            statString = getString(R.string.status);
        }
        for (int ifile = 0; ifile < nFiles; ifile++) {
            if (trackStatus[ifile] == 10) {
                nTracksWaiting++;
            } else if (trackStatus[ifile] == 1) {
                nTracksCompleted++;
            } else if (trackStatus[ifile] == 2) {
                nPreviousDownloads++;
            } else if (trackStatus[ifile] == 3) {
                nTracksExpired++;
            } else if (trackStatus[ifile] == 16) {
                nTracksFailed++;
            } else if (trackStatus[ifile] > 10 && trackStatus[ifile] % 2 == 1) {
                nTracksDownloading++;
            } else if (trackStatus[ifile] > 10 && trackStatus[ifile] % 2 == 0) {
                nTracksIncomplete++;
            }
        }
        if (nTracksCompleted > 0) {
            statString += " - " + nTracksCompleted + " " + getString(R.string.completed);
        }
        if (nTracksDownloading > 0) {
            statString += " - " + nTracksDownloading + " " + getString(R.string.downloading);
        }
        if (nTracksWaiting > 0) {
            statString += " - " + nTracksWaiting + " " + getString(R.string.remaining);
        }
        if (nTracksIncomplete > 0) {
            statString += " - " + nTracksIncomplete + " " + getString(R.string.incomplete);
        }
        if (nTracksFailed > 0) {
            statString += " - " + nTracksFailed + " " + getString(R.string.failed);
        }
        if (nPreviousDownloads > 0) {
            statString += " - " + nPreviousDownloads + " " + getString(R.string.previously_downloaded);
        }
        if (nTracksExpired > 0) {
            statString += " - " + nTracksExpired + " " + getString(R.string.missing_expired);
        }
        nfilesNew = nFiles - nPreviousDownloads - nTracksExpired - nTracksCompleted;
    }

    private void statusDialog() {
        getStatus();
        View ruler = new View(this);
        ruler.setBackgroundColor(0xFF696969);
        String statusString = "\n";
        ScrollView statusScrollView = new ScrollView(this);
        LinearLayout statusLinLay = new LinearLayout(this);
        statusLinLay.setOrientation(1);
        statusLinLay.setPadding(16, 0, 16, 16);
        float size = (float) 18.0;
        TextView statusHeaderTextView = new TextView(this);
        statusHeaderTextView.setText(statString);
        statusLinLay.addView(statusHeaderTextView);
        statusLinLay.addView(ruler, new LayoutParams(LinearLayout.LayoutParams.FILL_PARENT, 1));
        for (int ifile = 0; ifile < nFiles; ifile++) {
            LinearLayout trackLinLay = new LinearLayout(this);
            TextView infotv = new TextView(this);
            infotv.setText(" " + trackNames[ifile]);
            infotv.setTextSize(size);
            ImageView imgv = new ImageView(this);
            if (trackStatus[ifile] == 10) {
                imgv.setImageResource(R.drawable.icon0);
            } else if (trackStatus[ifile] == 3) {
                imgv.setImageResource(R.drawable.icon3);
            } else if (trackStatus[ifile] == 16) {
                imgv.setImageResource(R.drawable.icon3);
            } else if (trackStatus[ifile] == 2) {
                imgv.setImageResource(R.drawable.icon2);
            } else if (trackStatus[ifile] == 1) {
                imgv.setImageResource(R.drawable.icon2);
            } else if (trackStatus[ifile] > 10 && trackStatus[ifile] % 2 == 1) {
                imgv.setImageResource(R.drawable.icon1);
            } else if (trackStatus[ifile] > 10 && trackStatus[ifile] % 2 == 0) {
                imgv.setImageResource(R.drawable.icon3);
            }
            trackLinLay.setPadding(0, 5, 0, 5);
            trackLinLay.addView(imgv);
            trackLinLay.addView(infotv);
            statusLinLay.addView(trackLinLay);
            View rulerin = new View(this);
            rulerin.setBackgroundColor(0xFF696969);
            statusLinLay.addView(rulerin, new LayoutParams(LinearLayout.LayoutParams.FILL_PARENT, 1));
        }
        statusScrollView.addView(statusLinLay);
        Dialog statdialog = new Dialog(this) {

            public boolean onKeyDown(int keyCode, KeyEvent event) {
                if (keyCode != KeyEvent.KEYCODE_DPAD_LEFT) this.dismiss();
                return true;
            }
        };
        statdialog.setTitle(R.string.status);
        statdialog.addContentView(statusScrollView, new LinearLayout.LayoutParams(LinearLayout.LayoutParams.FILL_PARENT, LinearLayout.LayoutParams.FILL_PARENT));
        statdialog.show();
    }

    private void getFileInfoFromDB() {
        getStatus();
        getFileLists();
    }

    private void getFileInfoFromDownloader() {
        getStatus();
        getFileLists();
    }

    private void getFileInfoFromXML() {
        setProgressBarIndeterminateVisibility(true);
        pbar2.setVisibility(0);
        parseTextView.setVisibility(0);
        parseTextView.setText(R.string.parsing_album_info_please_wait_);
        Thread t2 = new Thread() {

            public void run() {
                Message msg = new Message();
                String msgText = "";
                for (int nfailed = 0; nfailed < 1; nfailed++) {
                    if (vConnecting) {
                        try {
                            boolean vnew = true;
                            int icount = 0;
                            try {
                                historys = Utils.droidDB.getHistory();
                                for (History history : historys) {
                                    icount++;
                                    if (XMLAddress.contains(history.url) || XMLAddress.replace("%20", " ").contains(history.urllocal)) {
                                        File cachedfile = new File(history.urllocal.replace("file://", ""));
                                        if (cachedfile.exists()) {
                                            XMLAddress = history.urllocal;
                                            vnew = false;
                                            handlerUsingCache.sendEmptyMessage(0);
                                        }
                                    }
                                }
                            } catch (Exception e2) {
                                final Exception ef = e2;
                            }
                            Log.d("EMD - ", "Parsing emx - " + XMLAddress);
                            InputStream content;
                            if (XMLAddress.contains("http")) {
                                HttpGet httpGet = new HttpGet(XMLAddress);
                                httpGet.setHeader("cookie", cookieString);
                                HttpClient httpclient = new DefaultHttpClient();
                                HttpResponse response = httpclient.execute(httpGet);
                                content = response.getEntity().getContent();
                            } else {
                                URL url = new URL(XMLAddress);
                                content = url.openStream();
                            }
                            SAXParserFactory spf = SAXParserFactory.newInstance();
                            SAXParser sp = spf.newSAXParser();
                            XMLReader xr = sp.getXMLReader();
                            XMLHandlerDownloads myXMLHandler = new XMLHandlerDownloads();
                            xr.setContentHandler(myXMLHandler);
                            xr.parse(new InputSource(content));
                            int newFiles = myXMLHandler.nTracks + 1;
                            String newArtURL = myXMLHandler.albumArt;
                            String newArtURLSml = myXMLHandler.albumArtSml;
                            String newGenre = myXMLHandler.genre;
                            int iBookFlag = 0;
                            if (newGenre.contains("Audiobooks")) {
                                iBookFlag = 1;
                            }
                            String newArtist = Utils.stringCleanUp(myXMLHandler.artist);
                            String newAlbum = Utils.stringCleanUp(myXMLHandler.album);
                            if (newFiles > 101) {
                                Message handlermsg = new Message();
                                String debugText = " " + newFiles + " ";
                                handlermsg.obj = debugText;
                                handler101Warn.sendMessage(handlermsg);
                                newFiles = 101;
                            }
                            String[] newTrackURLs = new String[newFiles];
                            String[] newTrackNums = new String[newFiles];
                            String[] newTrackNames = new String[newFiles];
                            String[] futureFileNames = new String[newFiles];
                            int[] newTrackStatus = new int[newFiles];
                            newTrackNames[0] = "AlbumArt.jpg";
                            newTrackURLs[0] = newArtURL;
                            newTrackStatus[0] = 10;
                            newTrackNums[0] = "0";
                            long ifs = 0;
                            String emxFutureFile = "";
                            if (vnew && vStorage) {
                                if (newFiles == 2) {
                                    emxFutureFile = newAlbum + " - " + myXMLHandler.downloadNames[0];
                                    emxFutureFile = Utils.stringCleanUp(emxFutureFile);
                                    emxFutureFile = storageRoot + "/" + emxFutureFile + ".emx";
                                } else {
                                    emxFutureFile = storageRoot + "/" + newAlbum + ".emx";
                                }
                            } else {
                                emxFutureFile = storageRoot + "/tmp";
                            }
                            BufferedWriter emxCacheFile = new BufferedWriter(new FileWriter(emxFutureFile, false));
                            if (vnew && vStorage) {
                                File sddir = new File(storageRoot);
                                sddir.mkdir();
                                emxCacheFile.write("<?xml version='1.0' encoding=\"UTF-8\"?>");
                                emxCacheFile.newLine();
                                emxCacheFile.write("  <PACKAGE>");
                            }
                            String pathTempChoice = "";
                            if (iBookFlag == 0) {
                                pathTempChoice = musicPath;
                            } else {
                                pathTempChoice = bookPath;
                            }
                            final String pathTemp = pathTempChoice;
                            final int newFilesFin = newFiles;
                            try {
                                parseTextView.post(new Runnable() {

                                    public void run() {
                                        parseTextView.setText(getString(R.string.checking_file_1_of) + " " + newFilesFin);
                                    }
                                });
                                URL currentTrackURL = new URL(newTrackURLs[0]);
                                HttpURLConnection currentTrackConnection = (HttpURLConnection) currentTrackURL.openConnection();
                                ifs = currentTrackConnection.getContentLength();
                                if (ifs == -1) {
                                    currentTrackConnection.disconnect();
                                    currentTrackConnection = (HttpURLConnection) currentTrackURL.openConnection();
                                    ifs = currentTrackConnection.getContentLength();
                                }
                                currentTrackConnection.disconnect();
                                if (ifs > 0) {
                                    final String filepath = pathTemp + newArtist + "/" + newAlbum + "/";
                                    File futureFile = new File(filepath, newTrackNames[0]);
                                    if (futureFile.exists()) {
                                        final long itmp = futureFile.length();
                                        if (itmp == ifs && itmp != 0) {
                                            newTrackStatus[0] = 2;
                                        }
                                    }
                                } else {
                                    newTrackURLs[0] = newArtURLSml;
                                    URL u2 = new URL(newTrackURLs[0]);
                                    HttpURLConnection c2 = (HttpURLConnection) u2.openConnection();
                                    ifs = c2.getContentLength();
                                    if (ifs == -1) {
                                        c2.disconnect();
                                        c2 = (HttpURLConnection) u2.openConnection();
                                        ifs = c2.getContentLength();
                                    }
                                    c2.disconnect();
                                    final String filepath = pathTemp + newArtist + "/" + newAlbum + "/";
                                    File futureFile = new File(filepath, newTrackNames[0]);
                                    if (futureFile.exists()) {
                                        final long itmp = futureFile.length();
                                        if (itmp == ifs && itmp != 0) {
                                            newTrackStatus[0] = 2;
                                        }
                                    }
                                }
                            } catch (Exception e) {
                            }
                            for (int i = 1; i < newFiles; i++) {
                                final int ifinal = i + 1;
                                parseTextView.post(new Runnable() {

                                    public void run() {
                                        parseTextView.setText(getString(R.string.checking_file_n) + " " + ifinal + " " + getString(R.string.of) + " " + newFilesFin);
                                    }
                                });
                                newTrackNums[i] = myXMLHandler.trackNumbers[i - 1];
                                newTrackURLs[i] = myXMLHandler.downloadURLs[i - 1];
                                newTrackNames[i] = myXMLHandler.downloadNames[i - 1].replace(":", "");
                                if (vnew && vStorage) {
                                    emxCacheFile.newLine();
                                    emxCacheFile.write("    <TRACK>");
                                    emxCacheFile.newLine();
                                    emxCacheFile.write("      <TRACKURL>" + newTrackURLs[i] + "</TRACKURL>");
                                    emxCacheFile.newLine();
                                    emxCacheFile.write("      <ALBUM>" + myXMLHandler.album.replace("&", "&amp;") + "</ALBUM>");
                                    emxCacheFile.newLine();
                                    emxCacheFile.write("      <ARTIST>" + myXMLHandler.artist.replace("&", "&amp;") + "</ARTIST>");
                                    emxCacheFile.newLine();
                                    emxCacheFile.write("      <TITLE>" + newTrackNames[i].replace("&", "&amp;") + "</TITLE>");
                                    emxCacheFile.newLine();
                                    emxCacheFile.write("      <GENRE>" + newGenre.replace("&", "&amp;") + "</GENRE>");
                                    emxCacheFile.newLine();
                                    emxCacheFile.write("      <TRACKNUM>" + newTrackNums[i].replace("&", "&amp;") + "</TRACKNUM>");
                                    emxCacheFile.newLine();
                                    emxCacheFile.write("      <ALBUMART>" + newArtURLSml + "</ALBUMART>");
                                    emxCacheFile.newLine();
                                    emxCacheFile.write("      <ALBUMARTLARGE>" + newArtURL + "</ALBUMARTLARGE>");
                                    emxCacheFile.newLine();
                                    emxCacheFile.write("    </TRACK>");
                                }
                                futureFileNames[i] = Utils.getTrackFileName(newTrackNames[i], newArtist, newAlbum, newTrackNums[i], thisActivity);
                                ifs = 0;
                                newTrackStatus[i] = 10;
                                try {
                                    URL currentTrackURL = new URL(newTrackURLs[i]);
                                    HttpURLConnection c = (HttpURLConnection) currentTrackURL.openConnection();
                                    ifs = c.getContentLength();
                                    if (ifs == -1) {
                                        c.disconnect();
                                        c = (HttpURLConnection) currentTrackURL.openConnection();
                                        ifs = c.getContentLength();
                                    }
                                    String fileType = c.getContentType();
                                    c.disconnect();
                                    final String filePath = pathTemp + newArtist + "/" + newAlbum + "/";
                                    File futureFile = new File(filePath, futureFileNames[i]);
                                    if (futureFile.exists()) {
                                        final long itmp = futureFile.length();
                                        if (itmp == ifs && itmp != 0) {
                                            newTrackStatus[i] = 2;
                                        } else if (itmp < ifs && itmp != 0) {
                                            newTrackStatus[i] = 12;
                                        }
                                    }
                                    if (fileType.lastIndexOf("audio") == -1 && fileType.lastIndexOf("octet") == -1) {
                                        newTrackStatus[i] = 3;
                                    }
                                } catch (Exception e) {
                                    newTrackStatus[i] = 3;
                                }
                            }
                            final String talbum = newAlbum;
                            final String tartist = newArtist;
                            final int tnfiles = newFiles;
                            final int tnfilesnew = nfilesNew;
                            try {
                                while (Utils.droidDB.isLocked()) {
                                    Thread.currentThread().sleep(1000);
                                }
                                if (Utils.droidDB.isLocked()) {
                                    Toast.makeText(thisActivity, R.string.database_locked, Toast.LENGTH_SHORT).show();
                                } else {
                                    if (vnew && vStorage) {
                                        emxCacheFile.newLine();
                                        emxCacheFile.write("  </PACKAGE>");
                                        emxCacheFile.close();
                                        Utils.droidDB.insertHistory(newAlbum, XMLAddress, "file://" + emxFutureFile);
                                    }
                                    List<Download> downloadsTemp = Utils.droidDB.getDownloads();
                                    for (int i = 0; i < newFiles; i++) {
                                        Boolean vFound = false;
                                        for (Download download : downloadsTemp) {
                                            if (download.trackurl.contains(newTrackURLs[i])) {
                                                vFound = true;
                                            }
                                        }
                                        if (!vFound) {
                                            Utils.droidDB.insertDownload(newArtist, newAlbum, newTrackNames[i], newArtURLSml, newTrackURLs[i], newTrackNums[i], iBookFlag, newTrackStatus[i]);
                                        }
                                    }
                                }
                            } catch (Exception e2) {
                                final Exception ef = e2;
                                Log.d("EMD - ", "Failed to insert download or history" + ef);
                            }
                            statusTextView.post(new Runnable() {

                                public void run() {
                                    statusTextView.setText(R.string.nothing_currently_downloading);
                                }
                            });
                            vConnecting = false;
                        } catch (Exception e) {
                            delay(1000);
                            Log.e("EMD - ", "Failed to parse " + nfailed);
                            Log.e("EMD - ", "Error message " + e);
                            if (nfailed == 0) {
                                final Exception ef = e;
                                handlerFail.sendEmptyMessage(0);
                                Log.d("EMD - ", "Parse failed " + ef);
                                parseTextView.post(new Runnable() {

                                    public void run() {
                                        parseTextView.setText(getString(R.string.cant_parse));
                                    }
                                });
                                vConnecting = false;
                                msgText = "Fail";
                            }
                        }
                    }
                }
                msg.obj = msgText;
                handlerDoneLoading.sendMessage(msg);
            }
        };
        t2.start();
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
            vartExists = false;
            if (ifs > 0) {
                conn.connect();
                InputStream is = conn.getInputStream();
                BufferedInputStream bis = new BufferedInputStream(is);
                bm = BitmapFactory.decodeStream(bis);
                bis.close();
                is.close();
                vartExists = true;
            }
            conn.disconnect();
        } catch (IOException e) {
            vartExists = false;
        }
        return bm;
    }

    private static void delay(int n) {
        long t0, t1;
        t0 = System.currentTimeMillis();
        do {
            t1 = System.currentTimeMillis();
        } while (t1 - t0 < n);
    }

    public void logoPressed(View buttoncover) {
        String browseurl = "http://www.emusic.com?fref=400062";
        Intent browseIntent = new Intent(this, WebWindowBrowse.class);
        browseIntent.putExtra("keyurl", browseurl);
        startActivity(browseIntent);
    }

    @Override
    protected void onPause() {
        super.onPause();
    }

    @Override
    protected void onStop() {
        super.onStop();
    }

    @Override
    protected void onNewIntent(Intent intent) {
        super.onNewIntent(intent);
        final Intent fintent = intent;
        if (intent.getData() == null) {
        } else if (intent.getDataString().contains("DLFinished")) {
        } else {
            if (megaInt > 20) {
                vConnecting = true;
                XMLAddress = intent.getDataString();
                getFileInfoFromXML();
            } else {
                Toast.makeText(thisActivity, "You need more than 20 MB of free space to make a purchase.", Toast.LENGTH_LONG).show();
            }
        }
    }

    private Handler handler101Warn = new Handler() {

        @Override
        public void handleMessage(Message msg) {
            Object texttotoast = msg.obj;
            String strt = getString(R.string.max_100_part1) + texttotoast + getString(R.string.max_100_part2);
            TextView wvt = new TextView(thisActivity);
            wvt.setPadding(16, 0, 16, 16);
            wvt.setText(strt);
            Dialog dialog = new Dialog(thisActivity) {

                public boolean onKeyDown(int keyCode, KeyEvent event) {
                    if (keyCode != KeyEvent.KEYCODE_DPAD_LEFT) this.dismiss();
                    return true;
                }
            };
            dialog.setTitle(R.string.warning);
            dialog.addContentView(wvt, new LinearLayout.LayoutParams(300, 110));
            dialog.show();
        }
    };

    private Handler handlerDoneLoading = new Handler() {

        @Override
        public void handleMessage(Message msg) {
            String textMessage = (String) msg.obj;
            setProgressBarIndeterminateVisibility(false);
            pbar2.setVisibility(8);
            if (!textMessage.contains("Fail")) {
                parseTextView.setVisibility(8);
            }
            try {
                adapter.imageLoader.stopThread();
                adapter = null;
            } catch (Exception ef) {
            }
            getStatus();
            getFileLists();
            if (!vLoop) {
                vLoop = true;
                Downloader.setvLoop(true);
                vCancel = false;
                Downloader.setvCancel(false);
                statusTextView.setText(R.string.downloading_dot);
                pauseButton.setImageResource(R.drawable.pause);
                startDownloader();
            }
        }
    };

    private Handler handlerUsingCache = new Handler() {

        @Override
        public void handleMessage(Message msg) {
            Toast.makeText(thisActivity, R.string.using_cache, Toast.LENGTH_LONG).show();
        }
    };

    private Handler handlerFail = new Handler() {

        @Override
        public void handleMessage(Message msg) {
            Toast.makeText(thisActivity, R.string.cant_parse, Toast.LENGTH_LONG).show();
        }
    };

    @Override
    protected void onSaveInstanceState(Bundle outState) {
    }

    @Override
    protected void onResume() {
        super.onResume();
        SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(this);
        int iFileNamePrefNew = Integer.parseInt(prefs.getString("filenamelist", "1"));
        int iFileDirPrefNew = Integer.parseInt(prefs.getString("filedirlist", "1"));
        int iBookDirPrefNew = Integer.parseInt(prefs.getString("bookdirlist", "1"));
        String fileNameSeparatorPrefNew = prefs.getString("filenameseparator", " - ");
        String customFileDirectoryPrefNew = prefs.getString("customdirname", "eMusic");
        if ((iFileDirPrefNew != iFileDirPref && iFileDirPref != 0) || (iFileNamePrefNew != iFileNamePref && iFileNamePref != 0) || (iBookDirPrefNew != iFileNamePref && iBookDirPref != 0) || (!(fileNameSeparatorPrefNew.equals(fileNameSeparatorPref)) && fileNameSeparatorPref != null) || (!(customFileDirectoryPrefNew.equals(customFileDirectoryPref)) && customFileDirectoryPref != null)) {
            getFileStoragePaths();
            getFileStatus();
            getFileLists();
        }
    }

    @Override
    public void onConfigurationChanged(Configuration newConfig) {
        String tempStatusString = statusTextView.getText().toString();
        int parseVisibility = parseTextView.getVisibility();
        String tempParseString = parseTextView.getText().toString();
        super.onConfigurationChanged(newConfig);
        setContentView(R.layout.downloadmanager2);
        pauseButton = (ImageView) findViewById(R.id.pausebutton);
        statusTextView = (TextView) findViewById(R.id.header_textview);
        parseTextView = (TextView) findViewById(R.id.status_textview);
        downloadListView = (ListView) findViewById(R.id.downloadlist);
        mbTextView = (TextView) findViewById(R.id.mbavailable);
        mbTextView.setText("" + megaInt + "MB Available" + "\nPress Menu to Clean List");
        statusTextView.setText(tempStatusString);
        parseTextView.setText(tempParseString);
        parseTextView.setVisibility(parseVisibility);
        if (vLoop) {
            pauseButton.setImageResource(R.drawable.pause);
        } else {
            pauseButton.setImageResource(R.drawable.restart);
        }
        int ivisible = pbar2.getVisibility();
        pbar2 = (LinearLayout) findViewById(R.id.pbar2);
        pbar2.setVisibility(ivisible);
        getFileStatus();
        getFileLists();
    }

    private void getFileStoragePaths() {
        SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(this);
        iFileDirPref = Integer.parseInt(prefs.getString("filedirlist", "1"));
        iBookDirPref = Integer.parseInt(prefs.getString("bookdirlist", "1"));
        customFileDirectoryPref = prefs.getString("customdirname", "eMusic");
        iFileNamePref = Integer.parseInt(prefs.getString("filenamelist", "1"));
        fileNameSeparatorPref = prefs.getString("filenameseparator", " - ");
        storageRoot = Utils.getStorageDirectory(thisActivity);
        Log.d("EMD - ", "storageRoot " + storageRoot);
        if (storageRoot == null) {
            vStorage = false;
            new AlertDialog.Builder(this).setIcon(android.R.drawable.ic_dialog_alert).setTitle(R.string.no_storage_available).setMessage(R.string.no_storage).setPositiveButton(R.string.quit, new DialogInterface.OnClickListener() {

                @Override
                public void onClick(DialogInterface dialog, int which) {
                    DownloadManager.this.finish();
                }
            }).show();
        } else {
            vStorage = true;
            bookPath = Utils.getAudioDirectory(thisActivity, "Book");
            musicPath = Utils.getAudioDirectory(thisActivity, "Music");
        }
    }

    private void clearComplete() {
        try {
            while (Utils.droidDB.isLocked()) {
                Thread.currentThread().sleep(1000);
            }
            if (Utils.droidDB.isLocked()) {
                Toast.makeText(thisActivity, R.string.database_locked, Toast.LENGTH_SHORT).show();
            } else {
                downloads = Utils.droidDB.getDownloads();
                for (Download download : downloads) {
                    if (download.status == 1 || download.status == 2 || download.status == 3) {
                        Utils.droidDB.deleteDownload(download.downloadId);
                    }
                }
            }
        } catch (Exception ef) {
            Log.d("EMD - ", "Failed to delete" + ef);
        }
        getFileStatus();
        getFileLists();
    }

    private void clearAll() {
        try {
            while (Utils.droidDB.isLocked()) {
                Thread.currentThread().sleep(1000);
            }
            if (Utils.droidDB.isLocked()) {
                Toast.makeText(thisActivity, R.string.database_locked, Toast.LENGTH_SHORT).show();
            } else {
                downloads = Utils.droidDB.getDownloads();
                for (Download download : downloads) {
                    Utils.droidDB.deleteDownload(download.downloadId);
                }
            }
        } catch (Exception ef) {
            Log.d("EMD - ", "Failed to delete" + ef);
        }
        getFileStatus();
        getFileLists();
    }

    private void getFileStatus() {
        try {
            downloads = Utils.droidDB.getDownloads();
        } catch (Exception ef) {
            Log.d("EMD - ", "Failed to get downloads" + ef);
        }
        nFiles = 0;
        try {
            nFiles = downloads.size();
        } catch (Exception ef) {
            Log.d("EMD - ", "Failed to get nFiles" + ef);
        }
        trackImageURLs = new String[nFiles];
        trackNames = new String[nFiles];
        trackArtists = new String[nFiles];
        trackAlbums = new String[nFiles];
        trackURLs = new String[nFiles];
        trackNums = new String[nFiles];
        trackBookFlags = new int[nFiles];
        trackStatus = new int[nFiles];
        trackIds = new long[nFiles];
        int icount = 0;
        for (Download download : downloads) {
            trackImageURLs[icount] = download.imageurl;
            trackNames[icount] = download.track;
            trackArtists[icount] = download.artist;
            trackAlbums[icount] = download.album;
            trackURLs[icount] = download.trackurl;
            trackNums[icount] = download.number;
            trackBookFlags[icount] = download.bookflag;
            trackStatus[icount] = download.status;
            trackIds[icount] = download.downloadId;
            icount++;
        }
        Log.d("EMD - ", "Loading List View" + downloads.size());
    }

    private void getFileLists() {
        String[] images = new String[nFiles];
        String[] listText = new String[nFiles];
        String[] trackStatusText = new String[nFiles];
        layoutHash = new HashMap<Long, Integer>();
        for (int icount = 0; icount < nFiles; icount++) {
            layoutHash.put(trackIds[icount], icount);
            images[icount] = trackImageURLs[icount];
            listText[icount] = trackAlbums[icount] + "\n" + trackArtists[icount] + "\n" + Utils.getTrackFileName(trackNames[icount], trackArtists[icount], trackAlbums[icount], trackNums[icount], thisActivity);
            if (trackStatus[icount] == 10) {
                trackStatusText[icount] = "--";
            } else if (trackStatus[icount] == 16) {
                trackStatusText[icount] = "✖";
            } else if (trackStatus[icount] > 10 && trackStatus[icount] % 2 == 1) {
                trackStatusText[icount] = "0%";
            } else if (trackStatus[icount] == 1) {
                trackStatusText[icount] = "✔";
            } else if (trackStatus[icount] == 2) {
                trackStatusText[icount] = "✔";
            } else if (trackStatus[icount] == 3) {
                trackStatusText[icount] = "Exp.";
            } else if (trackStatus[icount] > 10 && trackStatus[icount] % 2 == 0) {
                trackStatusText[icount] = "Inc.";
            }
        }
        adapter = new DownloadListAdapter(thisActivity, images, listText, trackStatusText, coverBitmapHash);
        downloadListView.setAdapter(adapter);
        registerForContextMenu(downloadListView);
    }

    AdapterView.OnItemClickListener listClickListener = new AdapterView.OnItemClickListener() {

        @Override
        public void onItemClick(AdapterView<?> a, View v, int position, long id) {
            Log.d("EMD - ", "Position Pressed: " + position);
            View row = (View) a.getChildAt(position);
            TextView statusView = (TextView) row.findViewById(R.id.statustext);
            if (statusView.getText().toString().contains("A")) {
                statusView.setText("B");
            } else {
                statusView.setText("A");
            }
        }
    };

    public void onCreateContextMenu(ContextMenu menu, View view, ContextMenuInfo menuInfo) {
        AdapterView.AdapterContextMenuInfo info = (AdapterView.AdapterContextMenuInfo) menuInfo;
        int ifile = info.position;
        switch(trackStatus[ifile]) {
            case 1:
                menu.setHeaderTitle("Completed");
                break;
            case 2:
                menu.setHeaderTitle("Previosly Downloaded");
                break;
            case 3:
                menu.setHeaderTitle("Expired");
                break;
            case 10:
                menu.setHeaderTitle("Waiting");
                break;
            case 11:
                menu.setHeaderTitle("Downloading");
                break;
            case 12:
                menu.setHeaderTitle("Waiting to Retry");
                break;
            case 13:
                menu.setHeaderTitle("Downloading");
                break;
            case 14:
                menu.setHeaderTitle("Waiting to Retry");
                break;
            case 15:
                menu.setHeaderTitle("Downloading");
                break;
            case 16:
                menu.setHeaderTitle("Failed");
                break;
        }
        if (trackStatus[ifile] == 16 || trackStatus[ifile] <= 3) {
            menu.add(0, 1000, 0, "Retry Download");
        } else {
            menu.add(0, 1000, 0, "Cancel Download");
        }
    }

    public boolean onContextItemSelected(MenuItem item) {
        AdapterView.AdapterContextMenuInfo info;
        try {
            info = (AdapterView.AdapterContextMenuInfo) item.getMenuInfo();
        } catch (ClassCastException e) {
            return false;
        }
        int ifile = info.position;
        if (trackStatus[ifile] == 16 || trackStatus[ifile] < 4) {
            trackStatus[ifile] = 10;
            try {
                while (Utils.droidDB.isLocked()) {
                    Thread.currentThread().sleep(1000);
                }
                if (Utils.droidDB.isLocked()) {
                    Toast.makeText(thisActivity, R.string.database_locked, Toast.LENGTH_SHORT).show();
                } else {
                    Utils.droidDB.updateDownload(trackIds[ifile], trackArtists[ifile], trackAlbums[ifile], trackNames[ifile], trackImageURLs[ifile], trackURLs[ifile], trackNums[ifile], trackBookFlags[ifile], trackStatus[ifile]);
                    int firstPosition = downloadListView.getFirstVisiblePosition();
                    int wantedChild = ifile - firstPosition;
                    if (wantedChild < 0 || wantedChild >= downloadListView.getChildCount()) {
                    } else {
                        adapter.updateStatus(ifile, "--");
                        TextView statView = (TextView) downloadListView.getChildAt(wantedChild).findViewById(R.id.statustext);
                        statView.setText("--");
                    }
                }
            } catch (Exception ef) {
            }
        } else {
            trackStatus[ifile] = 16;
            try {
                while (Utils.droidDB.isLocked()) {
                    Thread.currentThread().sleep(1000);
                }
                if (Utils.droidDB.isLocked()) {
                    Toast.makeText(thisActivity, R.string.database_locked, Toast.LENGTH_SHORT).show();
                } else {
                    Utils.droidDB.updateDownload(trackIds[ifile], trackArtists[ifile], trackAlbums[ifile], trackNames[ifile], trackImageURLs[ifile], trackURLs[ifile], trackNums[ifile], trackBookFlags[ifile], trackStatus[ifile]);
                    int firstPosition = downloadListView.getFirstVisiblePosition();
                    int wantedChild = ifile - firstPosition;
                    if (wantedChild < 0 || wantedChild >= downloadListView.getChildCount()) {
                    } else {
                        adapter.updateStatus(ifile, "✖");
                        TextView statView = (TextView) downloadListView.getChildAt(wantedChild).findViewById(R.id.statustext);
                        statView.setText("✖");
                    }
                }
            } catch (Exception ef) {
            }
        }
        return true;
    }
}
