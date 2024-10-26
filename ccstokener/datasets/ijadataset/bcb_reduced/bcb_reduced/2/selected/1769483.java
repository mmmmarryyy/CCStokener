package edu.berkeley.cs160.bravo.project;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.UnsupportedEncodingException;
import java.net.MalformedURLException;
import java.net.URL;
import java.util.ArrayList;
import java.util.Random;
import org.apache.http.HttpEntity;
import org.apache.http.HttpResponse;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.client.methods.HttpPut;
import org.apache.http.client.methods.HttpDelete;
import org.apache.http.client.methods.HttpUriRequest;
import org.apache.http.entity.StringEntity;
import org.apache.http.impl.client.DefaultHttpClient;
import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;
import android.app.Activity;
import android.app.AlertDialog;
import android.app.Dialog;
import android.content.DialogInterface;
import android.graphics.drawable.Drawable;
import android.os.Bundle;
import android.os.Handler;
import android.os.Message;
import android.view.View;
import android.widget.ArrayAdapter;
import android.widget.AutoCompleteTextView;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.Toast;

public class Home extends Activity {

    private boolean add_fish_mode = false;

    private int number_of_fish = 0;

    private ArrayList<String> fishList = new ArrayList<String>();

    private ArrayAdapter<String> adapter;

    private Handler tank_feed_handler = new Handler();

    private Button b_manage, b_settings;

    private Dialog manageTankDialog, settingsDialog;

    private ImageView iv_tank_cam;

    private AutoCompleteTextView actv_name;

    private Button b_newfish, b_save, b_revert, b_delete, b_compatibility_help, b_done;

    private EditText et_species, et_color, et_gender, et_birthday;

    private LinearLayout ll_fishlist;

    private Button b_settings_done;

    private EditText et_tank_cam_URL, et_refresh_interval, et_device_URL;

    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.home);
        b_manage = (Button) findViewById(R.id.b_manage);
        b_settings = (Button) findViewById(R.id.b_settings);
        iv_tank_cam = (ImageView) findViewById(R.id.iv_tank_cam);
        b_manage.setOnClickListener(clickListener);
        b_settings.setOnClickListener(clickListener);
        adapter = new ArrayAdapter<String>(this, R.layout.list_item, Feesh.SUPPORTED_FISH);
        b_manage.setText("Manage Tank\n\nFish Compatibility");
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

    private void manageTankDialog() {
        manageTankDialog = new Dialog(Home.this);
        manageTankDialog.setContentView(R.layout.manage_tank);
        manageTankDialog.setTitle("Manage Tank");
        manageTankDialog.setCancelable(true);
        new Thread(new Runnable() {

            public void run() {
                Message msg_toast = new Message();
                msg_toast.obj = "Loading tank management interface...";
                toast_handler.sendMessage(msg_toast);
                actv_name = (AutoCompleteTextView) manageTankDialog.findViewById(R.id.actv_name);
                b_save = (Button) manageTankDialog.findViewById(R.id.b_save);
                b_revert = (Button) manageTankDialog.findViewById(R.id.b_revert);
                b_delete = (Button) manageTankDialog.findViewById(R.id.b_delete);
                b_compatibility_help = (Button) manageTankDialog.findViewById(R.id.b_compatibility_help);
                b_done = (Button) manageTankDialog.findViewById(R.id.b_done);
                et_species = (EditText) manageTankDialog.findViewById(R.id.et_species);
                et_color = (EditText) manageTankDialog.findViewById(R.id.et_color);
                et_gender = (EditText) manageTankDialog.findViewById(R.id.et_gender);
                et_birthday = (EditText) manageTankDialog.findViewById(R.id.et_birthday);
                ll_fishlist = (LinearLayout) manageTankDialog.findViewById(R.id.ll_fishlist);
                b_save.setOnClickListener(dialogClickListener);
                b_revert.setOnClickListener(dialogClickListener);
                b_delete.setOnClickListener(dialogClickListener);
                b_compatibility_help.setOnClickListener(dialogClickListener);
                b_done.setOnClickListener(dialogClickListener);
                load_initial_data();
                Message msg_dialog = new Message();
                dialog_handler.sendMessage(msg_dialog);
            }
        }).start();
    }

    final void load_initial_data() {
        try {
            JSONObject jsonResult_fish0 = update_fish_buttons();
            Message msg_fish0data = new Message();
            Bundle fish0_data = new Bundle();
            if (number_of_fish > 0) {
                fish0_data.putString("name", jsonResult_fish0.getString("name"));
                fish0_data.putString("species", jsonResult_fish0.getString("species"));
                fish0_data.putString("color", jsonResult_fish0.getString("color"));
                fish0_data.putString("gender", jsonResult_fish0.getString("gender"));
                fish0_data.putString("birthday", jsonResult_fish0.getString("birthday"));
                msg_fish0data.setData(fish0_data);
                fish_handler.sendMessage(msg_fish0data);
                b_save.setTag(jsonResult_fish0);
                b_revert.setTag(jsonResult_fish0);
                b_delete.setTag(jsonResult_fish0);
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    final JSONObject update_fish_buttons() {
        JSONObject jsonResult_fish0 = null;
        Message msg_clearAll = new Message();
        msg_clearAll.obj = "clearAll";
        fishlist_handler.sendMessage(msg_clearAll);
        fishList.clear();
        try {
            HttpGet httpGetRequest = new HttpGet(Feesh.hostURL + "fish/_all_docs");
            JSONObject jsonResult_fishList = sendCouchRequest(httpGetRequest);
            number_of_fish = jsonResult_fishList.getInt("total_rows");
            JSONArray fishArray = jsonResult_fishList.getJSONArray("rows");
            for (int i = 0; i < number_of_fish; i++) {
                Message msg_fish = new Message();
                String fish_id = ((JSONObject) fishArray.get(i)).getString("id");
                httpGetRequest = new HttpGet(Feesh.hostURL + "fish/" + fish_id);
                JSONObject jsonResult_fish = sendCouchRequest(httpGetRequest);
                if (i == 0) {
                    jsonResult_fish0 = jsonResult_fish;
                }
                String fishName = jsonResult_fish.getString("name");
                fishList.add(fishName);
                Button b_fish = new Button(Home.this);
                b_fish.setTag(jsonResult_fish);
                b_fish.setText(fishName);
                b_fish.setOnClickListener(fishButtonClickListener);
                msg_fish.obj = b_fish;
                fishlist_handler.sendMessage(msg_fish);
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
        Message msg_newfish = new Message();
        b_newfish = new Button(Home.this);
        b_newfish.setText("+");
        b_newfish.setOnClickListener(dialogClickListener);
        msg_newfish.obj = b_newfish;
        fishlist_handler.sendMessage(msg_newfish);
        return jsonResult_fish0;
    }

    final boolean check_compatibility(String fishA, String fishB) {
        if (fishA.equalsIgnoreCase(fishB)) return true;
        String result = "";
        try {
            HttpGet httpGetRequest = new HttpGet(Feesh.hostURL + "compatibility/" + fishA.toLowerCase());
            JSONObject jsonResult_compatibility = sendCouchRequest(httpGetRequest);
            result = jsonResult_compatibility.getString(fishB.toLowerCase());
        } catch (JSONException e) {
            e.printStackTrace();
        }
        if ((result.equals("m")) || (result.equals("n"))) return false; else return true;
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

    final Handler dialog_handler = new Handler() {

        public void handleMessage(Message msg_dialog) {
            actv_name.setAdapter(adapter);
            manageTankDialog.show();
        }
    };

    final Handler frame_handler = new Handler() {

        public void handleMessage(Message msg_frame) {
            iv_tank_cam.setImageDrawable((Drawable) msg_frame.obj);
        }
    };

    final Handler fishlist_handler = new Handler() {

        public void handleMessage(Message msg_fish) {
            if (msg_fish.obj == "clearAll") ll_fishlist.removeAllViewsInLayout(); else ll_fishlist.addView((Button) msg_fish.obj);
        }
    };

    final Handler update_action_buttons_handler = new Handler() {

        public void handleMessage(Message msg_mode) {
            Boolean mode = (Boolean) msg_mode.obj;
            add_fish_mode = mode;
            if (mode) {
                b_save.setText("Add");
                b_revert.setText("Cancel");
                b_delete.setEnabled(false);
            } else {
                b_save.setText("Save");
                b_revert.setText("Revert");
                b_delete.setEnabled(true);
            }
        }
    };

    final Handler fish_handler = new Handler() {

        public void handleMessage(Message msg_JSONObject) {
            Bundle data = msg_JSONObject.getData();
            actv_name.setText(data.getString("name"));
            et_species.setText(data.getString("species"));
            et_color.setText(data.getString("color"));
            et_gender.setText(data.getString("gender"));
            et_birthday.setText(data.getString("birthday"));
        }
    };

    final Handler toast_handler = new Handler() {

        public void handleMessage(Message msg_toast) {
            Toast.makeText(Home.this, (String) msg_toast.obj, Toast.LENGTH_LONG).show();
        }
    };

    private Button.OnClickListener clickListener = new Button.OnClickListener() {

        @Override
        public void onClick(View v) {
            if (v == b_manage) {
                manageTankDialog();
            } else if (v == b_settings) {
                settingsDialog = new Dialog(Home.this);
                settingsDialog.setContentView(R.layout.settings);
                settingsDialog.setCancelable(true);
                et_tank_cam_URL = (EditText) settingsDialog.findViewById(R.id.et_tank_cam_URL);
                et_refresh_interval = (EditText) settingsDialog.findViewById(R.id.et_refresh_interval);
                et_device_URL = (EditText) settingsDialog.findViewById(R.id.et_device_URL);
                b_settings_done = (Button) settingsDialog.findViewById(R.id.b_settings_done);
                et_tank_cam_URL.setText(Feesh.tank_cam_URL);
                et_refresh_interval.setText(String.valueOf(Feesh.refresh_interval));
                et_device_URL.setText(Feesh.device_URL);
                b_settings_done.setOnClickListener(new Button.OnClickListener() {

                    public void onClick(View v) {
                        Feesh.tank_cam_URL = et_tank_cam_URL.getText().toString();
                        Feesh.refresh_interval = Long.parseLong(et_refresh_interval.getText().toString());
                        Feesh.device_URL = et_device_URL.getText().toString();
                        settingsDialog.dismiss();
                    }
                });
                settingsDialog.setTitle("Settings");
                settingsDialog.show();
            }
        }
    };

    private Button.OnClickListener dialogClickListener = new Button.OnClickListener() {

        @Override
        public void onClick(View v) {
            if (v == b_newfish) {
                Message msg_mode = new Message();
                msg_mode.obj = true;
                update_action_buttons_handler.sendMessage(msg_mode);
                actv_name.setText("");
                et_species.setText("");
                et_color.setText("");
                et_gender.setText("");
                et_birthday.setText("");
            } else if (v == b_save) {
                new Thread(new Runnable() {

                    public void run() {
                        String fish_id = null;
                        String fish_rev = null;
                        String putText = "";
                        try {
                            if (add_fish_mode) {
                                Random rand = new Random();
                                fish_id = String.valueOf(rand.nextInt(999999999));
                            } else {
                                JSONObject jsonResult_fish = (JSONObject) b_save.getTag();
                                fish_id = jsonResult_fish.getString("_id");
                                fish_rev = jsonResult_fish.getString("_rev");
                            }
                            HttpPut httpPutRequest = new HttpPut(Feesh.hostURL + "fish/" + fish_id);
                            httpPutRequest.setHeader("Accept", "application/json");
                            httpPutRequest.setHeader("Content-type", "application/json");
                            httpPutRequest.setHeader("Authorization", "Basic");
                            if (add_fish_mode) {
                                putText = "{" + "\"_id\":\"" + fish_id + "\"" + ",\"name\":\"" + actv_name.getText().toString() + "\"" + ",\"species\":\"" + et_species.getText().toString() + "\"" + ",\"color\":\"" + et_color.getText().toString() + "\"" + ",\"gender\":\"" + et_gender.getText().toString() + "\"" + ",\"birthday\":\"" + et_birthday.getText().toString() + "\"" + "}";
                            } else {
                                putText = "{" + "\"_id\":\"" + fish_id + "\"" + ",\"_rev\":\"" + fish_rev + "\"" + ",\"name\":\"" + actv_name.getText().toString() + "\"" + ",\"species\":\"" + et_species.getText().toString() + "\"" + ",\"color\":\"" + et_color.getText().toString() + "\"" + ",\"gender\":\"" + et_gender.getText().toString() + "\"" + ",\"birthday\":\"" + et_birthday.getText().toString() + "\"" + "}";
                            }
                            StringEntity nameValuePairs = new StringEntity(putText, "UTF-8");
                            httpPutRequest.setEntity(nameValuePairs);
                            JSONObject jsonResult = sendCouchRequest(httpPutRequest);
                            update_fish_buttons();
                            Message msg_mode = new Message();
                            msg_mode.obj = false;
                            update_action_buttons_handler.sendMessage(msg_mode);
                            Message msg_toast = new Message();
                            if (jsonResult.getString("ok") == "true") if (add_fish_mode) msg_toast.obj = "Added new fish \"" + actv_name.getText().toString() + "\" (ID: " + fish_id + ")."; else msg_toast.obj = "Fish information updated!"; else msg_toast.obj = jsonResult.getString("error") + "\n" + jsonResult.getString("reason");
                            toast_handler.sendMessage(msg_toast);
                        } catch (UnsupportedEncodingException e) {
                            e.printStackTrace();
                        } catch (JSONException e) {
                            e.printStackTrace();
                        }
                    }
                }).start();
            } else if (v == b_revert) {
                try {
                    JSONObject jsonResult_fish = (JSONObject) v.getTag();
                    Message msg_fishdata = new Message();
                    Bundle fish_data = new Bundle();
                    fish_data.putString("name", jsonResult_fish.getString("name"));
                    fish_data.putString("species", jsonResult_fish.getString("species"));
                    fish_data.putString("color", jsonResult_fish.getString("color"));
                    fish_data.putString("gender", jsonResult_fish.getString("gender"));
                    fish_data.putString("birthday", jsonResult_fish.getString("birthday"));
                    msg_fishdata.setData(fish_data);
                    fish_handler.sendMessage(msg_fishdata);
                    Message msg_mode = new Message();
                    msg_mode.obj = false;
                    update_action_buttons_handler.sendMessage(msg_mode);
                    if (!add_fish_mode) {
                        Message msg_toast = new Message();
                        msg_toast.obj = "All fields returned to their original values.";
                        toast_handler.sendMessage(msg_toast);
                    }
                } catch (JSONException e) {
                    e.printStackTrace();
                }
            } else if (v == b_delete) {
                new Thread(new Runnable() {

                    public void run() {
                        try {
                            JSONObject jsonResult_fish = (JSONObject) b_delete.getTag();
                            String fish_id = jsonResult_fish.getString("_id");
                            String fish_rev = jsonResult_fish.getString("_rev");
                            String fish_name = jsonResult_fish.getString("name");
                            HttpDelete httpDeleteRequest = new HttpDelete(Feesh.hostURL + "fish/" + fish_id + "?rev=" + fish_rev);
                            httpDeleteRequest.setHeader("Accept", "application/json");
                            httpDeleteRequest.setHeader("Content-type", "application/json");
                            httpDeleteRequest.setHeader("Authorization", "Basic");
                            JSONObject jsonResult = sendCouchRequest(httpDeleteRequest);
                            update_fish_buttons();
                            load_initial_data();
                            Message msg_toast = new Message();
                            if (jsonResult.getString("ok") == "true") msg_toast.obj = "Deleted \"" + fish_name + "\" (ID: " + fish_id + ")."; else msg_toast.obj = jsonResult.getString("error") + "\n" + jsonResult.getString("reason");
                            toast_handler.sendMessage(msg_toast);
                        } catch (JSONException e) {
                            e.printStackTrace();
                        }
                    }
                }).start();
            } else if (v == b_compatibility_help) {
                AlertDialog.Builder alertBuilder = new AlertDialog.Builder(Home.this);
                alertBuilder.setTitle("Compatibility");
                int num_of_incompatibilities = 0;
                String resultMessage = "";
                ArrayList<String> checkedFish = new ArrayList<String>();
                for (int i = 0; i < fishList.size(); i++) {
                    String fishA = fishList.get(i);
                    for (int j = 0; j < fishList.size(); j++) {
                        String fishB = fishList.get(j);
                        if (checkedFish.contains(fishB)) continue;
                        if (!check_compatibility(fishA, fishB)) {
                            resultMessage = resultMessage.concat("\n\t" + fishA + " & " + fishB);
                            num_of_incompatibilities++;
                        }
                    }
                    checkedFish.add(fishA);
                }
                resultMessage = ("Found " + num_of_incompatibilities + " incompatibilities.").concat(resultMessage);
                alertBuilder.setMessage(resultMessage);
                alertBuilder.setCancelable(false);
                alertBuilder.setPositiveButton("OK", new DialogInterface.OnClickListener() {

                    public void onClick(DialogInterface dialog, int id) {
                        dialog.dismiss();
                    }
                });
                AlertDialog alert = alertBuilder.create();
                alert.show();
            } else if (v == b_done) {
                add_fish_mode = false;
                manageTankDialog.dismiss();
            }
        }
    };

    private Button.OnClickListener fishButtonClickListener = new Button.OnClickListener() {

        @Override
        public void onClick(View v) {
            if (v == b_newfish) {
                Message msg_mode = new Message();
                msg_mode.obj = true;
                update_action_buttons_handler.sendMessage(msg_mode);
            } else {
                Message msg_mode = new Message();
                msg_mode.obj = false;
                update_action_buttons_handler.sendMessage(msg_mode);
            }
            try {
                Message msg_fishdata = new Message();
                Bundle fish_data = new Bundle();
                JSONObject jsonResult_fish = (JSONObject) v.getTag();
                fish_data.putString("name", jsonResult_fish.getString("name"));
                fish_data.putString("species", jsonResult_fish.getString("species"));
                fish_data.putString("color", jsonResult_fish.getString("color"));
                fish_data.putString("gender", jsonResult_fish.getString("gender"));
                fish_data.putString("birthday", jsonResult_fish.getString("birthday"));
                msg_fishdata.setData(fish_data);
                fish_handler.sendMessage(msg_fishdata);
                b_save.setTag(jsonResult_fish);
                b_revert.setTag(jsonResult_fish);
                b_delete.setTag(jsonResult_fish);
            } catch (JSONException e) {
                e.printStackTrace();
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
