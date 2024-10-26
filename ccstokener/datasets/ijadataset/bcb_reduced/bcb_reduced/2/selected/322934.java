package edu.berkeley.cs160.bravo.project;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.UnsupportedEncodingException;
import java.net.MalformedURLException;
import java.net.URL;
import java.util.ArrayList;
import java.util.List;
import org.apache.http.HttpEntity;
import org.apache.http.HttpResponse;
import org.apache.http.NameValuePair;
import org.apache.http.client.ClientProtocolException;
import org.apache.http.client.entity.UrlEncodedFormEntity;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.client.methods.HttpUriRequest;
import org.apache.http.impl.client.DefaultHttpClient;
import org.apache.http.message.BasicNameValuePair;
import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;
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

public class Temperature extends Activity {

    private static final int min_temperature = 60;

    private static final int max_temperature = 100;

    private int temperature = 68;

    private Handler tank_feed_handler = new Handler();

    private Button b_decrement, b_increment, b_help, b_set;

    private EditText et_temperature;

    private ImageView iv_tank_cam;

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.temperature);
        b_decrement = (Button) findViewById(R.id.b_decrement);
        b_increment = (Button) findViewById(R.id.b_increment);
        b_help = (Button) findViewById(R.id.b_help);
        b_set = (Button) findViewById(R.id.b_set);
        et_temperature = (EditText) findViewById(R.id.et_temperature);
        iv_tank_cam = (ImageView) findViewById(R.id.iv_tank_cam);
        b_decrement.setOnClickListener(clickListener);
        b_increment.setOnClickListener(clickListener);
        b_help.setOnClickListener(clickListener);
        b_set.setOnClickListener(clickListener);
        et_temperature.setOnEditorActionListener(editorActionListener);
        et_temperature.setText(String.valueOf(temperature));
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
        int requestedTemperature = Integer.parseInt(et_temperature.getText().toString());
        if (requestedTemperature < min_temperature) {
            temperature = min_temperature;
        } else if (requestedTemperature > max_temperature) {
            temperature = max_temperature;
        } else {
            temperature = requestedTemperature;
        }
    }

    private void showHelpDialog() {
        new Thread(new Runnable() {

            public void run() {
                Message msg_toast = new Message();
                msg_toast.obj = "Loading help information...";
                toast_handler.sendMessage(msg_toast);
                try {
                    AlertDialog.Builder alertBuilder = new AlertDialog.Builder(Temperature.this);
                    alertBuilder.setTitle("Temperature Help");
                    alertBuilder.setCancelable(false);
                    String result = "";
                    HttpGet httpGetRequest = new HttpGet(Feesh.hostURL + "fish/_all_docs");
                    JSONObject jsonResult_fishList = sendCouchRequest(httpGetRequest);
                    int number_of_fish = jsonResult_fishList.getInt("total_rows");
                    JSONArray fishArray = jsonResult_fishList.getJSONArray("rows");
                    for (int i = 0; i < number_of_fish; i++) {
                        String fish_id = ((JSONObject) fishArray.get(i)).getString("id");
                        httpGetRequest = new HttpGet(Feesh.hostURL + "fish/" + fish_id);
                        JSONObject jsonResult_fish = sendCouchRequest(httpGetRequest);
                        String fishName = jsonResult_fish.getString("name").toLowerCase();
                        httpGetRequest = new HttpGet(Feesh.hostURL + "help/" + fishName);
                        JSONObject jsonResult_help = sendCouchRequest(httpGetRequest);
                        result = result.concat(jsonResult_help.getString("temperature") + "\n");
                    }
                    if (result.length() == 0) result = "No fish to display help information for.  Add fish using \"Manage Tank\" first.";
                    alertBuilder.setMessage(result);
                    alertBuilder.setPositiveButton("Thanks", new DialogInterface.OnClickListener() {

                        public void onClick(DialogInterface dialog, int id) {
                            dialog.dismiss();
                        }
                    });
                    Message msg_help_dialog = new Message();
                    msg_help_dialog.obj = alertBuilder;
                    help_dialog_handler.sendMessage(msg_help_dialog);
                } catch (JSONException e) {
                    e.printStackTrace();
                }
            }
        }).start();
    }

    private void showTempSetDialog() {
        AlertDialog.Builder alertBuilder = new AlertDialog.Builder(this);
        alertBuilder.setTitle("Done");
        alertBuilder.setMessage("Adjusted heater thermostat to " + temperature + "�F.");
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

    final Handler help_dialog_handler = new Handler() {

        public void handleMessage(Message msg_help_dialog) {
            AlertDialog alert = ((AlertDialog.Builder) msg_help_dialog.obj).create();
            alert.show();
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
            et_temperature.setText(Integer.toString(temperature));
            return true;
        }
    };

    final Handler toast_handler = new Handler() {

        public void handleMessage(Message msg_toast) {
            Toast.makeText(Temperature.this, (String) msg_toast.obj, Toast.LENGTH_LONG).show();
        }
    };

    private Button.OnClickListener clickListener = new Button.OnClickListener() {

        @Override
        public void onClick(View v) {
            if (v == b_decrement) {
                getNewValues();
                if (temperature > min_temperature) et_temperature.setText(String.valueOf(temperature - 1)); else et_temperature.setText(String.valueOf(temperature));
            } else if (v == b_increment) {
                getNewValues();
                if (temperature < max_temperature) et_temperature.setText(String.valueOf(temperature + 1)); else et_temperature.setText(String.valueOf(temperature));
            } else if (v == b_help) {
                showHelpDialog();
            } else if (v == b_set) {
                getNewValues();
                new Thread(new Runnable() {

                    public void run() {
                        try {
                            HttpPost httpPostRequest = new HttpPost(Feesh.device_URL);
                            List<NameValuePair> nameValuePairs = new ArrayList<NameValuePair>();
                            nameValuePairs.add(new BasicNameValuePair("c", "temperature"));
                            nameValuePairs.add(new BasicNameValuePair("amount", String.valueOf(temperature)));
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
                showTempSetDialog();
            }
        }
    };

    private static JSONObject sendCouchRequest(HttpUriRequest request) {
        try {
            HttpResponse httpResponse = (HttpResponse) new DefaultHttpClient().execute(request);
            HttpEntity entity = httpResponse.getEntity();
            if (entity != null) {
                InputStream instream = entity.getContent();
                String resultString = convertStreamToString(instream);
                instream.close();
                JSONObject jsonResult = new JSONObject(resultString);
                return jsonResult;
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
        return null;
    }

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
