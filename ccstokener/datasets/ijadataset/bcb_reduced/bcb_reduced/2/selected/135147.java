package edu.berkeley.cs160.bravo.project;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.UnsupportedEncodingException;
import java.net.MalformedURLException;
import java.net.URL;
import java.text.DecimalFormat;
import java.util.ArrayList;
import java.util.List;
import org.apache.http.HttpEntity;
import org.apache.http.HttpResponse;
import org.apache.http.NameValuePair;
import org.apache.http.client.ClientProtocolException;
import org.apache.http.client.entity.UrlEncodedFormEntity;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.impl.client.DefaultHttpClient;
import org.apache.http.message.BasicNameValuePair;
import android.app.Activity;
import android.app.AlertDialog;
import android.content.DialogInterface;
import android.graphics.drawable.Drawable;
import android.os.Bundle;
import android.os.Handler;
import android.os.Message;
import android.view.KeyEvent;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

public class Oxygen extends Activity {

    private static final double min_time = 0.1;

    private static final double max_time = 30;

    private static final DecimalFormat df = new DecimalFormat("0.0");

    private double time = 5;

    Handler tank_feed_handler = new Handler();

    private Button b_decrement, b_increment, b_help, b_aerate;

    private EditText et_time;

    private ImageView iv_tank_cam;

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.oxygen);
        b_decrement = (Button) findViewById(R.id.b_decrement);
        b_increment = (Button) findViewById(R.id.b_increment);
        b_help = (Button) findViewById(R.id.b_help);
        b_aerate = (Button) findViewById(R.id.b_aerate);
        et_time = (EditText) findViewById(R.id.et_time);
        iv_tank_cam = (ImageView) findViewById(R.id.iv_tank_cam);
        b_decrement.setOnClickListener(clickListener);
        b_increment.setOnClickListener(clickListener);
        b_help.setOnClickListener(clickListener);
        b_aerate.setOnClickListener(clickListener);
        et_time.setOnEditorActionListener(editorActionListener);
        et_time.setText(df.format(time));
    }

    public void onResume() {
        super.onResume();
        iv_tank_cam.setImageDrawable(Feesh.tank_feed_cache);
        tank_feed_handler.postDelayed(refreshFeed, Feesh.refresh_interval);
    }

    public void onPause() {
        super.onPause();
        tank_feed_handler.removeCallbacks(refreshFeed);
    }

    private Drawable retrieveImage(String url) {
        Drawable d = null;
        try {
            InputStream is = (InputStream) this.fetch(url);
            d = Drawable.createFromStream(is, "src");
        } catch (MalformedURLException e) {
            e.printStackTrace();
        } catch (IOException e) {
            e.printStackTrace();
        }
        if (d != null) {
            Feesh.tank_feed_cache = d;
        }
        return d;
    }

    public Object fetch(String address) throws MalformedURLException, IOException {
        URL url = new URL(address);
        return url.getContent();
    }

    public void getNewValues() {
        double requestedTime = Double.parseDouble(et_time.getText().toString());
        requestedTime = Double.parseDouble(df.format(requestedTime));
        if (requestedTime < min_time) {
            time = min_time;
        } else if (requestedTime > max_time) {
            time = max_time;
        } else {
            time = requestedTime;
        }
    }

    private void showHelpDialog() {
        AlertDialog.Builder alertBuilder = new AlertDialog.Builder(this);
        alertBuilder.setTitle("Oxygen Help");
        alertBuilder.setMessage("Oxygen levels should be maintained at 5.0 to 7.0 mg/L.");
        alertBuilder.setCancelable(false);
        alertBuilder.setPositiveButton("Thanks", new DialogInterface.OnClickListener() {

            public void onClick(DialogInterface dialog, int id) {
                dialog.dismiss();
            }
        });
        AlertDialog alert = alertBuilder.create();
        alert.show();
    }

    private void showAeratingDialog() {
        AlertDialog.Builder alertBuilder = new AlertDialog.Builder(this);
        alertBuilder.setTitle("Aerating...");
        alertBuilder.setMessage("Aerating tank for " + df.format(time) + " minutes.");
        alertBuilder.setCancelable(false);
        alertBuilder.setPositiveButton("OK", new DialogInterface.OnClickListener() {

            public void onClick(DialogInterface dialog, int id) {
                dialog.dismiss();
            }
        });
        AlertDialog alert = alertBuilder.create();
        alert.show();
    }

    final Runnable refreshFeed = new Runnable() {

        public void run() {
            new Thread(new Runnable() {

                public void run() {
                    Message msg_frame = new Message();
                    msg_frame.obj = retrieveImage(Feesh.tank_cam_URL);
                    frame_handler.sendMessage(msg_frame);
                }
            }).start();
            tank_feed_handler.postDelayed(refreshFeed, Feesh.refresh_interval);
        }
    };

    final Handler frame_handler = new Handler() {

        public void handleMessage(Message msg_frame) {
            iv_tank_cam.setImageDrawable((Drawable) msg_frame.obj);
        }
    };

    private EditText.OnEditorActionListener editorActionListener = new EditText.OnEditorActionListener() {

        @Override
        public boolean onEditorAction(TextView v, int actionId, KeyEvent event) {
            getNewValues();
            et_time.setText(df.format(time));
            return true;
        }
    };

    final Handler toast_handler = new Handler() {

        public void handleMessage(Message msg_toast) {
            Toast.makeText(Oxygen.this, (String) msg_toast.obj, Toast.LENGTH_LONG).show();
        }
    };

    private Button.OnClickListener clickListener = new Button.OnClickListener() {

        @Override
        public void onClick(View v) {
            if (v == b_decrement) {
                getNewValues();
                if (time > min_time) et_time.setText(df.format(time - 0.1)); else et_time.setText(df.format(time));
            } else if (v == b_increment) {
                getNewValues();
                if (time < max_time) et_time.setText(df.format(time + 0.1)); else et_time.setText(df.format(time));
            } else if (v == b_help) {
                showHelpDialog();
            } else if (v == b_aerate) {
                getNewValues();
                new Thread(new Runnable() {

                    public void run() {
                        try {
                            HttpPost httpPostRequest = new HttpPost(Feesh.device_URL);
                            List<NameValuePair> nameValuePairs = new ArrayList<NameValuePair>();
                            nameValuePairs.add(new BasicNameValuePair("c", "oxygen"));
                            nameValuePairs.add(new BasicNameValuePair("amount", String.valueOf(time)));
                            httpPostRequest.setEntity(new UrlEncodedFormEntity(nameValuePairs));
                            HttpResponse httpResponse = (HttpResponse) new DefaultHttpClient().execute(httpPostRequest);
                            HttpEntity entity = httpResponse.getEntity();
                            String resultString = "";
                            if (entity != null) {
                                InputStream instream = entity.getContent();
                                resultString = convertStreamToString(instream);
                                instream.close();
                            }
                            Message msg_toast = new Message();
                            msg_toast.obj = resultString;
                            toast_handler.sendMessage(msg_toast);
                        } catch (UnsupportedEncodingException e) {
                            e.printStackTrace();
                        } catch (ClientProtocolException e) {
                            e.printStackTrace();
                        } catch (IOException e) {
                            e.printStackTrace();
                        }
                    }
                }).start();
                showAeratingDialog();
            }
        }
    };

    private static String convertStreamToString(InputStream is) {
        BufferedReader reader = new BufferedReader(new InputStreamReader(is));
        StringBuilder sb = new StringBuilder();
        String line = null;
        try {
            while ((line = reader.readLine()) != null) {
                sb.append(line + "\n");
            }
        } catch (IOException e) {
            e.printStackTrace();
        } finally {
            try {
                is.close();
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
        return sb.toString();
    }
}
