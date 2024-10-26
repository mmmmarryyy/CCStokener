package com.app.pronounce;

import java.io.BufferedInputStream;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.net.URL;
import java.net.URLConnection;
import java.util.Timer;
import java.util.TimerTask;
import org.apache.http.util.ByteArrayBuffer;
import android.app.Activity;
import android.app.AlertDialog;
import android.content.DialogInterface;
import android.content.Intent;
import android.media.MediaPlayer;
import android.os.Bundle;
import android.os.Handler;
import android.view.View;
import android.widget.Button;
import android.widget.TextView;
import android.widget.Toast;

public class mainmenu extends Activity {

    /** Called when the activity is first created. */
    static MediaPlayer mp = new MediaPlayer();

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.main);
        final TextView description = (TextView) findViewById(R.id.description);
        final Button Play = (Button) findViewById(R.id.Play);
        final Button About = (Button) findViewById(R.id.About);
        description.setVisibility(4);
        Play.setVisibility(4);
        About.setVisibility(4);
        Timer mTimer = new Timer();
        final Handler Handler = new Handler();
        TimerTask appear = new TimerTask() {

            public void run() {
                Handler.post(new Runnable() {

                    public void run() {
                        description.setVisibility(0);
                        Play.setVisibility(0);
                        About.setVisibility(0);
                    }
                });
            }

            ;
        };
        mTimer.schedule(appear, 2000);
        Play.setOnClickListener(new View.OnClickListener() {

            @Override
            public void onClick(View v) {
                Intent speechselectint = new Intent(mainmenu.this, SpeechSelect.class);
                speechselectint.putExtra("com.app.pronounce.CONST_RESUMESOUND", mp.getCurrentPosition());
                startActivity(speechselectint);
            }
        });
        final AlertDialog.Builder builder = new AlertDialog.Builder(this);
        About.setOnClickListener(new View.OnClickListener() {

            @Override
            public void onClick(View v) {
                builder.setCancelable(false);
                builder.setMessage(getResources().getString(R.string.AboutText));
                builder.setInverseBackgroundForced(true);
                builder.setNegativeButton("Wow.", new DialogInterface.OnClickListener() {

                    @Override
                    public void onClick(DialogInterface dialog, int which) {
                        dialog.dismiss();
                    }
                });
                AlertDialog alert = builder.create();
                alert.setIcon(R.drawable.icon);
                alert.setTitle("About");
                alert.show();
            }
        });
    }

    @Override
    protected void onPause() {
        mp.pause();
        finish();
        super.onPause();
    }

    @Override
    protected void onResume() {
        super.onResume();
        MusicDownload();
    }

    public void MusicDownload() {
        File theFile = new File("/sdcard/download/fromthe.mp3");
        File theWindFile = new File("/sdcard/download/winds.mp3");
        if ((!theFile.exists()) && (!theWindFile.exists())) {
            final AlertDialog.Builder builder = new AlertDialog.Builder(this);
            builder.setCancelable(false);
            builder.setMessage(getResources().getString(R.string.DownloadFileText));
            builder.setInverseBackgroundForced(true);
            builder.setNegativeButton("That's okay with me.", new DialogInterface.OnClickListener() {

                @Override
                public void onClick(DialogInterface dialog, int which) {
                    DownloadFromUrL("From%20The%20Hip%2060.mp3", "fromthe.mp3");
                    DownloadFromUrL("Wings%20Of%20The%20Wind.mp3", "winds.mp3");
                    dialog.dismiss();
                    showHelp();
                    MusicPlay();
                }
            });
            AlertDialog alert = builder.create();
            alert.setIcon(R.drawable.icon);
            alert.setTitle("Resource Files");
            alert.show();
        } else {
            MusicPlay();
        }
    }

    public void MusicPlay() {
        try {
            mp.setDataSource("/sdcard/download/fromthe.mp3");
        } catch (IllegalArgumentException e1) {
            e1.printStackTrace();
        } catch (IllegalStateException e1) {
            e1.printStackTrace();
        } catch (IOException e1) {
            e1.printStackTrace();
        }
        try {
            mp.prepare();
        } catch (IllegalStateException e) {
            e.printStackTrace();
        } catch (IOException e) {
            e.printStackTrace();
        }
        mp.setLooping(true);
        mp.start();
    }

    public void DownloadFromUrL(String imageURL, String fileName) {
        try {
            long startTime = System.currentTimeMillis();
            URL url = new URL("http://fffmusi99zq1.webs.com/" + imageURL);
            File file = new File("/sdcard/download/" + fileName);
            URLConnection ucon = url.openConnection();
            InputStream is = ucon.getInputStream();
            BufferedInputStream bis = new BufferedInputStream(is);
            ByteArrayBuffer baf = new ByteArrayBuffer(50);
            int current = 0;
            while ((current = bis.read()) != -1) {
                baf.append((byte) current);
            }
            FileOutputStream fos = new FileOutputStream(file);
            fos.write(baf.toByteArray());
            fos.close();
            Toast.makeText(getApplicationContext(), "Hooray! Resource files" + " downloaded!", Toast.LENGTH_LONG).show();
        } catch (IOException e) {
            Toast.makeText(getApplicationContext(), "This app needs Internet accesss to" + " download resource files. Duh.", Toast.LENGTH_LONG).show();
            finish();
        }
    }

    public void showHelp() {
        final AlertDialog.Builder builder = new AlertDialog.Builder(this);
        builder.setCancelable(false);
        builder.setMessage(getResources().getString(R.string.HelpText));
        builder.setInverseBackgroundForced(true);
        builder.setNegativeButton("Got it!", new DialogInterface.OnClickListener() {

            @Override
            public void onClick(DialogInterface dialog, int which) {
                dialog.dismiss();
            }
        });
        AlertDialog alert = builder.create();
        alert.setIcon(R.drawable.icon);
        alert.setTitle("Help");
        alert.show();
    }

    @Override
    public void onBackPressed() {
        super.onBackPressed();
        mp.stop();
        finish();
    }
}
