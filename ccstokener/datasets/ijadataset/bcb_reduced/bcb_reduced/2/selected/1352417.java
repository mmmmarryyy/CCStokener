package com.hamdyghanem.holyquran;

import java.io.BufferedInputStream;
import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.net.URL;
import java.net.URLConnection;
import java.util.List;
import java.util.zip.ZipEntry;
import java.util.zip.ZipInputStream;
import org.apache.http.util.ByteArrayBuffer;
import com.hamdyghanem.holyquran.R;
import android.app.Activity;
import android.app.ProgressDialog;
import android.content.Intent;
import android.graphics.Typeface;
import android.net.Uri;
import android.os.Bundle;
import android.os.Environment;
import android.os.Handler;
import android.os.Message;
import android.util.Log;
import android.view.Display;
import android.view.View;
import android.view.Window;
import android.view.View.OnClickListener;
import android.view.WindowManager.LayoutParams;
import android.widget.Button;
import android.widget.CheckBox;
import android.widget.EditText;
import android.widget.RadioButton;
import android.widget.TableRow;
import android.widget.TextView;
import android.widget.Toast;

public class DownloadActivity extends Activity {

    /** Called when the activity is first created. */
    ProgressDialog dialog;

    int increment = 0;

    int allincrement = 0;

    String strResult = "";

    String baseDir = "";

    ApplicationController AC;

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        try {
            final boolean customTitleSupported = requestWindowFeature(Window.FEATURE_CUSTOM_TITLE);
            setContentView(R.layout.download);
            AC = (ApplicationController) getApplicationContext();
            Typeface arabicFont = Typeface.createFromAsset(getAssets(), "fonts/DroidSansArabic.ttf");
            if (customTitleSupported) {
                getWindow().setFeatureInt(Window.FEATURE_CUSTOM_TITLE, R.layout.mytitle);
            }
            final TextView myTitleText = (TextView) findViewById(R.id.myTitle);
            if (myTitleText != null) {
                myTitleText.setTypeface(arabicFont);
                myTitleText.setText(AC.getTextbyLanguage(R.string.download));
            }
            getWindow().setLayout(LayoutParams.FILL_PARENT, LayoutParams.FILL_PARENT);
            ((Button) findViewById(R.id.btnSettDownload)).setTypeface(arabicFont);
            ((Button) findViewById(R.id.btnSettDownload)).setText(AC.getTextbyLanguage(R.string.btn_download_pages));
            ((Button) findViewById(R.id.btnTafserDownload)).setTypeface(arabicFont);
            ((Button) findViewById(R.id.btnTafserDownload)).setText(AC.getTextbyLanguage(R.string.btn_download_tafser));
            ((Button) findViewById(R.id.btnTaareefDownload)).setTypeface(arabicFont);
            ((Button) findViewById(R.id.btnTaareefDownload)).setText(AC.getTextbyLanguage(R.string.btn_download_tareef));
            ((Button) findViewById(R.id.btnDictionayDownload)).setTypeface(arabicFont);
            ((Button) findViewById(R.id.btnDictionayDownload)).setText(AC.getTextbyLanguage(R.string.btn_download_dictionary));
            ((Button) findViewById(R.id.btnDatabaseDownload)).setTypeface(arabicFont);
            ((Button) findViewById(R.id.btnDatabaseDownload)).setText(AC.getTextbyLanguage(R.string.btn_download_database));
            ((Button) findViewById(R.id.btnAudioDownload)).setTypeface(arabicFont);
            ((Button) findViewById(R.id.btnAudioDownload)).setText(AC.getTextbyLanguage(R.string.downloadaudiofiles));
            Display display = getWindowManager().getDefaultDisplay();
            ((Button) findViewById(R.id.btnDatabaseDownload)).setWidth(display.getWidth());
        } catch (Throwable t) {
            Toast.makeText(this, "Request failed: " + t.toString(), Toast.LENGTH_LONG).show();
        }
    }

    public void downloadPages(View view) {
        Intent intent = new Intent(Intent.ACTION_VIEW);
        intent.setData(Uri.parse("market://details?id=com.hamdyghanem.holyquranimg" + AC.CurrentImageType));
        startActivity(intent);
    }

    public void downloadTafser(View view) {
        downloadTafserNow();
    }

    public void downloadDatabase(View view) {
        downloadDatabaseNow();
    }

    public void downloadTareef(View view) {
        downloadTareefNow();
    }

    public void downloadDicttonary(View view) {
        downloadDictionaryNow();
    }

    public void downloadAudio(View view) {
        startActivity(new Intent(this, DownloadRecitationActivity.class));
    }

    private Boolean isConnected() {
        Boolean bOK = AC.isNetConnected();
        if (!bOK) {
            Toast.makeText(this, AC.getTextbyLanguage(R.string.NoInternet), Toast.LENGTH_LONG).show();
        } else {
            AC.GetActivePath();
            baseDir = Environment.getExternalStorageDirectory().getAbsolutePath() + "/hQuran/img/0/";
            String strFile = baseDir + "1.img";
            strResult = ImageManager.DownloadFromUrl("0", AC.ActivePath, "1", strFile);
            Log.d(">>>>>>>>", strResult);
            if (strResult.length() > 0) {
                Toast.makeText(this, AC.getTextbyLanguage(R.string.DownloadFailed), Toast.LENGTH_LONG).show();
                Intent intent = new Intent();
                intent.setClass(this, DownloadFailedActivity.class);
                startActivity(intent);
                bOK = false;
            }
        }
        return bOK;
    }

    private boolean unpackZip() {
        InputStream is;
        ZipInputStream zis;
        String path = Environment.getExternalStorageDirectory().getAbsolutePath() + "/hQuran/img/" + AC.CurrentImageType;
        String fileName = path + ".zip";
        path += "/";
        try {
            is = new FileInputStream(fileName);
            zis = new ZipInputStream(new BufferedInputStream(is));
            ZipEntry ze;
            while ((ze = zis.getNextEntry()) != null) {
                ByteArrayOutputStream baos = new ByteArrayOutputStream();
                byte[] buffer = new byte[1024];
                int count;
                String filename = ze.getName();
                FileOutputStream fout = new FileOutputStream(path + filename);
                while ((count = zis.read(buffer)) != -1) {
                    baos.write(buffer, 0, count);
                    byte[] bytes = baos.toByteArray();
                    fout.write(bytes);
                    baos.reset();
                }
                fout.close();
                zis.closeEntry();
            }
            zis.close();
        } catch (IOException e) {
            e.printStackTrace();
            return false;
        }
        return true;
    }

    public void downloadImages() {
        if (!isConnected()) return;
        dialog = new ProgressDialog(this);
        dialog.setCancelable(true);
        dialog.setMessage(AC.getTextbyLanguage(R.string.downloadingpages));
        dialog.setProgressStyle(ProgressDialog.STYLE_HORIZONTAL);
        dialog.setProgress(0);
        dialog.setMax(100);
        dialog.show();
        Thread background = new Thread(new Runnable() {

            public void run() {
                try {
                    allincrement = 0;
                    String strFile = Environment.getExternalStorageDirectory().getAbsolutePath() + "/hQuran/img/" + AC.CurrentImageType + ".zip";
                    URL url = new URL(AC.ActivePath + "/img/" + AC.CurrentImageType + ".zip");
                    long startTime = System.currentTimeMillis();
                    Log.d("ImageManager", "downloaded file name>:" + strFile + " - url:" + url.toString());
                    URLConnection ucon = url.openConnection();
                    dialog.setMax(ucon.getContentLength() / 1024);
                    InputStream is = ucon.getInputStream();
                    BufferedInputStream bis = new BufferedInputStream(is, 8192);
                    ByteArrayBuffer baf = new ByteArrayBuffer(50);
                    double current = 0;
                    while ((current = bis.read()) != -1) {
                        baf.append((byte) current);
                        allincrement += current;
                        increment = allincrement / 1024;
                        progressHandler.sendMessage(progressHandler.obtainMessage());
                    }
                    FileOutputStream fos = new FileOutputStream(strFile);
                    fos.write(baf.toByteArray());
                    fos.close();
                } catch (IOException e) {
                    Log.d("ImageManager", "Error: " + e);
                }
                dialog.cancel();
            }
        });
        background.start();
    }

    public void downloadNow() {
        baseDir = Environment.getExternalStorageDirectory().getAbsolutePath() + "/hQuran/img/" + AC.CurrentImageType + "/";
        if (!isConnected()) return;
        dialog = new ProgressDialog(this);
        dialog.setCancelable(true);
        dialog.setMessage(AC.getTextbyLanguage(R.string.downloadingpages));
        dialog.setProgressStyle(ProgressDialog.STYLE_HORIZONTAL);
        dialog.setProgress(0);
        dialog.setMax(605);
        dialog.show();
        Thread background = new Thread(new Runnable() {

            public void run() {
                while (increment < 605) {
                    String strFile = baseDir + Integer.toString(increment) + ".img";
                    File f = new File(strFile);
                    strResult = "";
                    if (!f.exists()) {
                        strResult = ImageManager.DownloadFromUrl(AC.CurrentImageType, AC.ActivePath, Integer.toString(increment), strFile);
                        if (strResult.length() > 0) {
                            Log.d("DOWNLOAD FAILED", strResult);
                            dialog.cancel();
                            return;
                        }
                    }
                    increment++;
                    progressHandler.sendMessage(progressHandler.obtainMessage());
                }
                dialog.cancel();
            }
        });
        background.start();
    }

    public void downloadDatabaseNow() {
        if (!isConnected()) return;
        dialog = new ProgressDialog(this);
        dialog.setCancelable(true);
        dialog.setMessage(AC.getTextbyLanguage(R.string.downloadingdatabase));
        dialog.setProgressStyle(ProgressDialog.STYLE_SPINNER);
        increment = 0;
        dialog.show();
        Thread background = new Thread(new Runnable() {

            public void run() {
                strResult = ImageManager.DownloadDBFromUrl(AC.ActivePath);
                progressHandler.sendMessage(progressHandler.obtainMessage());
                if (strResult.length() > 0) {
                    Log.d("DOWNLOAD FAILED", strResult);
                    dialog.cancel();
                    return;
                }
                dialog.cancel();
            }
        });
        background.start();
    }

    public void downloadTafserNow() {
        if (!isConnected()) return;
        baseDir = Environment.getExternalStorageDirectory().getAbsolutePath() + "/hQuran/tafseer/";
        dialog = new ProgressDialog(this);
        dialog.setCancelable(true);
        dialog.setMessage(AC.getTextbyLanguage(R.string.downloadingtafseer));
        dialog.setProgressStyle(ProgressDialog.STYLE_HORIZONTAL);
        increment = 0;
        dialog.setProgress(0);
        dialog.setMax(605);
        dialog.show();
        Thread background = new Thread(new Runnable() {

            public void run() {
                while (increment < 605) {
                    String strFile = baseDir + Integer.toString(increment) + ".html";
                    File f = new File(strFile);
                    if (!f.exists()) {
                        strResult = ImageManager.DownloadTafserFromUrl(AC.ActivePath, Integer.toString(increment), strFile);
                        if (strResult.length() > 0) {
                            Log.d("DOWNLOAD FAILED", strResult);
                            dialog.cancel();
                            return;
                        }
                    }
                    increment++;
                    progressHandler.sendMessage(progressHandler.obtainMessage());
                }
                dialog.cancel();
            }
        });
        background.start();
    }

    public void downloadTareefNow() {
        if (!isConnected()) return;
        baseDir = Environment.getExternalStorageDirectory().getAbsolutePath() + "/hQuran/taareef/";
        dialog = new ProgressDialog(this);
        dialog.setCancelable(true);
        dialog.setMessage(AC.getTextbyLanguage(R.string.downloadingtareef));
        dialog.setProgressStyle(ProgressDialog.STYLE_HORIZONTAL);
        increment = 0;
        dialog.setProgress(0);
        dialog.setMax(605);
        dialog.show();
        Thread background = new Thread(new Runnable() {

            public void run() {
                while (increment < 605) {
                    String strFile = baseDir + Integer.toString(increment) + ".html";
                    File f = new File(strFile);
                    if (!f.exists()) {
                        strResult = ImageManager.DownloadTareefFromUrl(AC.ActivePath, Integer.toString(increment), strFile);
                        if (strResult.length() > 0) {
                            Log.d("DOWNLOAD FAILED", strResult);
                            dialog.cancel();
                            return;
                        }
                    }
                    increment++;
                    progressHandler.sendMessage(progressHandler.obtainMessage());
                }
                dialog.cancel();
            }
        });
        background.start();
    }

    public void downloadDictionaryNow() {
        if (!isConnected()) return;
        baseDir = Environment.getExternalStorageDirectory().getAbsolutePath() + "/hQuran/dictionary/";
        dialog = new ProgressDialog(this);
        dialog.setCancelable(true);
        dialog.setMessage(AC.getTextbyLanguage(R.string.downloadingdictionary));
        dialog.setProgressStyle(ProgressDialog.STYLE_HORIZONTAL);
        increment = 0;
        dialog.setProgress(0);
        dialog.setMax(605);
        dialog.show();
        Thread background = new Thread(new Runnable() {

            public void run() {
                while (increment < 605) {
                    String strFile = baseDir + Integer.toString(increment) + ".html";
                    File f = new File(strFile);
                    if (!f.exists()) {
                        strResult = ImageManager.DownloadDictionaryFromUrl(AC.ActivePath, Integer.toString(increment), strFile);
                        if (strResult.length() > 0) {
                            Log.d("DOWNLOAD FAILED", strResult);
                            dialog.cancel();
                            return;
                        }
                    }
                    increment++;
                    progressHandler.sendMessage(progressHandler.obtainMessage());
                }
                dialog.cancel();
            }
        });
        background.start();
    }

    Handler progressHandler = new Handler() {

        public void handleMessage(Message msg) {
            dialog.setProgress(increment);
        }
    };

    private OnClickListener ok_listener = new OnClickListener() {

        @Override
        public void onClick(View v) {
            AC.iLanguage = 0;
            RadioButton rb = ((RadioButton) findViewById(R.id.optEnglish));
            if (rb.isChecked()) AC.iLanguage = 1;
            Intent intent = new Intent();
            intent.putExtra("returnKey", 1);
            setResult(RESULT_OK, intent);
            finish();
        }
    };

    private OnClickListener cancel_listener = new OnClickListener() {

        @Override
        public void onClick(View v) {
            finish();
        }
    };
}
