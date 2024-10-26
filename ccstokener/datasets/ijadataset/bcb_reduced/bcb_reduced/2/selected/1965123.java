package com.ridanlabs.onelist.android.test;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import org.apache.http.HttpResponse;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.entity.StringEntity;
import org.apache.http.impl.client.DefaultHttpClient;
import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;
import org.json.JSONTokener;
import com.google.android.c2dm.C2DMessaging;
import android.app.Activity;
import android.app.Dialog;
import android.app.ProgressDialog;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.view.Window;
import android.view.View.OnClickListener;
import android.widget.Button;
import com.ridanlabs.onelist.android.Config;
import com.ridanlabs.onelist.android.R;
import com.ridanlabs.onelist.android.sync.JSONRPCMethod;
import com.ridanlabs.onelist.android.sync.RPCClient;
import com.ridanlabs.onelist.rpclib.OneListProtocol;

public class TestRPCActivity extends Activity {

    public static final int PING_SERVER_DIALOG = 0;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        requestWindowFeature(Window.FEATURE_NO_TITLE);
        setContentView(R.layout.testrpclayout);
        initRPCButtons();
    }

    public void initRPCButtons() {
        initRegisterUserButton();
        initPingButton();
        initC2DMRegisterButton();
        initCreateListButton();
        initAddUserToListButton();
        initAddItemToList();
        initSyncButton();
    }

    protected void initSyncButton() {
        Button b = (Button) findViewById(R.id.testSyncButton);
        if (b == null) Log.e(Config.SS_TAG, "null value for button"); else {
            b.setOnClickListener(new OnClickListener() {

                @Override
                public void onClick(View v) {
                    Log.d(Config.SS_TAG, "Add List Item button called.");
                    JSONRPCMethod createList;
                    try {
                        createList = new JSONRPCMethod.Sync("yorkethegod@gmail.com");
                        JSONObject returnValue = RPCClient.execute(createList);
                        RPCClient.testSyncData(returnValue);
                        Log.e(Config.SS_TAG, returnValue.getString("success"));
                        Log.d(Config.SS_TAG, returnValue.toString());
                    } catch (JSONException e) {
                        e.printStackTrace();
                    }
                }
            });
        }
    }

    protected void initAddItemToList() {
        Button b = (Button) findViewById(R.id.testAddListItemButton);
        if (b == null) Log.e(Config.SS_TAG, "null value for button"); else {
            b.setOnClickListener(new OnClickListener() {

                @Override
                public void onClick(View v) {
                    Log.d(Config.SS_TAG, "Add List Item button called.");
                    try {
                        JSONRPCMethod createList = new JSONRPCMethod.AddListItem("item1", "list1", "nadirabid@gmail.com", 5);
                        JSONObject returnValue = RPCClient.execute(createList);
                        String ret = returnValue.getString(OneListProtocol.AddListItem.RETURN[0]);
                        Log.d(Config.SS_TAG, "Message Received: " + ret);
                    } catch (JSONException e) {
                        Log.e(Config.SS_TAG, "We got an error while making the object");
                        e.printStackTrace();
                    }
                }
            });
        }
    }

    protected void initAddUserToListButton() {
        Button b = (Button) findViewById(R.id.testAddUserToListButton);
        if (b == null) Log.e(Config.SS_TAG, "null value for button"); else {
            b.setOnClickListener(new OnClickListener() {

                @Override
                public void onClick(View v) {
                    Log.d(Config.SS_TAG, "Add User to List button called.");
                    try {
                        JSONRPCMethod createList = new JSONRPCMethod.AddUserToList("test@gmail.com", "list1");
                        JSONObject returnValue = RPCClient.execute(createList);
                        String ret = returnValue.getString(OneListProtocol.AddUserToList.RETURN[0]);
                        Log.d(Config.SS_TAG, "Message Received: " + ret);
                    } catch (JSONException e) {
                        Log.e(Config.SS_TAG, "We got an error while making the object");
                        e.printStackTrace();
                    }
                }
            });
        }
    }

    protected void initCreateListButton() {
        Button b = (Button) findViewById(R.id.testCreatListButton);
        if (b == null) Log.e(Config.SS_TAG, "null value for button"); else {
            b.setOnClickListener(new OnClickListener() {

                @Override
                public void onClick(View v) {
                    Log.d(Config.SS_TAG, "Create list button called.");
                    try {
                        JSONRPCMethod createList = new JSONRPCMethod.CreateList("list1", "nadirabid@gmail.com");
                        JSONObject returnValue = RPCClient.execute(createList);
                        String ret = returnValue.getString(OneListProtocol.CreateList.RETURN[0]);
                        Log.d(Config.SS_TAG, "Message Received: " + ret);
                    } catch (JSONException e) {
                        Log.e(Config.SS_TAG, "We got an error while making the object");
                        e.printStackTrace();
                    }
                }
            });
        }
    }

    protected void initRegisterUserButton() {
        Button registerUserButton = (Button) findViewById(R.id.registerDeviceWithUser);
        if (registerUserButton == null) Log.e(Config.SS_TAG, "null value for button"); else {
            registerUserButton.setOnClickListener(new OnClickListener() {

                @Override
                public void onClick(View v) {
                    Log.d(Config.SS_TAG, "RegisterUser button called.");
                    try {
                        JSONRPCMethod registerUser = new JSONRPCMethod.RegisterDeviceWithUser("test2@gmail.com", C2DMessaging.getRegistrationId(TestRPCActivity.this));
                        JSONObject returnValue = RPCClient.execute(registerUser);
                        String ret = returnValue.getString(OneListProtocol.RegisterDeviceWithUser.RETURN[0]);
                        Log.d(Config.SS_TAG, "Message Received: " + ret);
                    } catch (JSONException e) {
                        Log.e(Config.SS_TAG, "We got an error while making the object");
                        e.printStackTrace();
                    }
                }
            });
        }
    }

    protected void initPingButton() {
        Button testRPCPingButton = (Button) findViewById(R.id.testRPCPingButton);
        testRPCPingButton.setOnClickListener(new OnClickListener() {

            @Override
            public void onClick(View v) {
                Log.d(Config.SS_TAG, "Sending POST request to server...");
                DefaultHttpClient httpClient = new DefaultHttpClient();
                HttpPost httpPost = new HttpPost(Config.RPC_SERVLET_URL);
                JSONObject requestJson = new JSONObject();
                JSONArray callsJson = new JSONArray();
                try {
                    JSONObject callJson = new JSONObject();
                    callJson.put("method", "ping");
                    callJson.put("void", "null");
                    callsJson.put(0, callJson);
                    requestJson.put("calls", callsJson);
                    httpPost.setEntity(new StringEntity(requestJson.toString(), "UTF-8"));
                    HttpResponse httpResponse = httpClient.execute(httpPost);
                    final int responseStatusCode = httpResponse.getStatusLine().getStatusCode();
                    if (200 <= responseStatusCode && responseStatusCode < 300) {
                        Log.d(Config.SS_TAG, "Successful ping - status code: " + responseStatusCode);
                        BufferedReader reader = new BufferedReader(new InputStreamReader(httpResponse.getEntity().getContent(), "UTF-8"), 8 * 1024);
                        StringBuilder sb = new StringBuilder();
                        String line;
                        while ((line = reader.readLine()) != null) {
                            sb.append(line).append("\n");
                        }
                        JSONTokener tokener = new JSONTokener(sb.toString());
                        JSONObject responseJson = new JSONObject(tokener);
                        JSONArray resultsJson = responseJson.getJSONArray("results");
                        JSONObject result = resultsJson.getJSONObject(0);
                        String returnValue = result.getJSONObject("data").getString("return");
                        Log.d(Config.SS_TAG, "Response message: " + returnValue);
                    } else {
                        Log.e(Config.SS_TAG, "Unsuccessful ping...");
                    }
                } catch (Exception e) {
                    Log.e(Config.SS_TAG, "Error while trying to ping rpc servlet");
                    e.printStackTrace();
                }
            }
        });
    }

    protected void initC2DMRegisterButton() {
        Button c2dmRegisterButton = (Button) findViewById(R.id.c2dmRegisterButton);
        c2dmRegisterButton.setOnClickListener(new OnClickListener() {

            @Override
            public void onClick(View v) {
                RPCClient.registerDevice(TestRPCActivity.this);
            }
        });
    }

    @Override
    protected Dialog onCreateDialog(int id) {
        switch(id) {
            case PING_SERVER_DIALOG:
                Log.d(Config.SS_TAG, "Opening PING dialog...");
                ProgressDialog dialog = new ProgressDialog(this);
                dialog.setMessage("Pinging server...");
                dialog.setIndeterminate(true);
                dialog.setCancelable(true);
                return dialog;
        }
        return null;
    }
}
