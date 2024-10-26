package activities;

import java.io.BufferedReader;
import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.IOException;
import java.io.InputStreamReader;
import java.net.InetAddress;
import java.net.NetworkInterface;
import java.net.ServerSocket;
import java.net.Socket;
import java.net.SocketException;
import java.net.URI;
import java.net.URISyntaxException;
import java.net.UnknownHostException;
import java.util.Enumeration;
import org.apache.http.HttpResponse;
import org.apache.http.client.ClientProtocolException;
import org.apache.http.client.HttpClient;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.impl.client.DefaultHttpClient;
import org.apache.http.params.BasicHttpParams;
import org.apache.http.params.HttpParams;
import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;
import toz.android.R;
import android.app.Activity;
import android.app.AlertDialog;
import android.content.DialogInterface;
import android.content.Intent;
import android.os.Bundle;
import android.view.View;
import android.view.View.OnClickListener;
import android.widget.Button;
import android.widget.LinearLayout;
import android.widget.TextView;
import engine.Engine;

/**
 * 
 * @author Stephen Sarquah
 * @copyright
 * Copyright 2012 Stephen Sarquah
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
public class ArenaActivity extends Activity {

    private Activity activity;

    private LinearLayout arenaroot;

    private final String registerUrl = "http://90.184.203.73:8080/axis2/services/TOZ-ArenaService/register";

    private final String findOppponentUrl = "http://90.184.203.73:8080/axis2/services/TOZ-ArenaService/findOpponent";

    private final String unregisterUrl = "http://90.184.203.73:8080/axis2/services/TOZ-ArenaService/unregister";

    private final String json = "response=application/json";

    private String ip;

    private final int port = 8005;

    private Socket client;

    private DataInputStream dataInputStream;

    private DataOutputStream dataOutputStream;

    private Socket socket;

    private ServerSocket serverSocket;

    private final String challengeRequest = "ArenaChallengeRequest";

    @Override
    public void onCreate(final Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.arena);
        activity = this;
        arenaroot = (LinearLayout) findViewById(R.id.arenaroot);
        HttpParams httpParams = new BasicHttpParams();
        HttpClient httpclient = new DefaultHttpClient(httpParams);
        HttpGet request;
        HttpGet request2;
        try {
            request = new HttpGet(new URI(registerUrl + "?playername=" + Engine.engine.getPlayer().getName() + "&level=" + Engine.engine.getPlayer().getLevel() + "&creatureclass=" + Engine.engine.getPlayer().getCreatureClass().toString() + "&gold=" + Engine.engine.getPlayer().getGold() + "&ip=" + getLocalIp() + "&" + json));
            request2 = new HttpGet(new URI(findOppponentUrl + "?" + json));
            try {
                httpclient.execute(request);
                HttpResponse result2 = httpclient.execute(request2);
                BufferedReader reader = new BufferedReader(new InputStreamReader(result2.getEntity().getContent()));
                String line = reader.readLine();
                try {
                    JSONObject jsonObject = new JSONObject(line);
                    JSONArray jsonArray = jsonObject.getJSONArray("return");
                    for (int i = 0; i < jsonArray.length(); i++) {
                        JSONObject player = jsonArray.getJSONObject(i);
                        ip = player.getString("ip");
                        if (ip != getLocalIp()) {
                            String creatureclass = player.getString("creatureclass");
                            String level = player.getString("level");
                            String playername = player.getString("playername");
                            TextView playernameText = new TextView(arenaroot.getContext());
                            playernameText.setText("Name: " + playername);
                            arenaroot.addView(playernameText);
                            TextView classText = new TextView(arenaroot.getContext());
                            classText.setText("Class: " + creatureclass);
                            arenaroot.addView(classText);
                            TextView levelText = new TextView(arenaroot.getContext());
                            levelText.setText("Level: " + level);
                            arenaroot.addView(levelText);
                            Button button = new Button(arenaroot.getContext());
                            button.setText("Challenge");
                            button.setOnClickListener(new OnClickListener() {

                                @Override
                                public void onClick(View v) {
                                    socket = null;
                                    dataOutputStream = null;
                                    try {
                                        socket = new Socket(InetAddress.getByName(ip), port);
                                        dataOutputStream = new DataOutputStream(socket.getOutputStream());
                                        dataOutputStream.writeByte(1);
                                        dataOutputStream.writeUTF(challengeRequest);
                                        dataOutputStream.flush();
                                    } catch (UnknownHostException e) {
                                        e.printStackTrace();
                                    } catch (IOException e) {
                                        e.printStackTrace();
                                    }
                                }
                            });
                            arenaroot.addView(button);
                        }
                    }
                    serverSocket = new ServerSocket(port);
                    Thread listeningThread = new Thread(new ServerThread());
                    listeningThread.start();
                } catch (JSONException e) {
                    e.printStackTrace();
                }
            } catch (ClientProtocolException e) {
                e.printStackTrace();
            } catch (IOException e) {
                e.printStackTrace();
            }
        } catch (URISyntaxException e1) {
            e1.printStackTrace();
        }
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
    }

    @Override
    protected void onPause() {
        super.onPause();
    }

    @Override
    protected void onResume() {
        super.onResume();
    }

    @Override
    protected void onStop() {
        HttpParams httpParams = new BasicHttpParams();
        HttpClient httpclient = new DefaultHttpClient(httpParams);
        HttpGet request;
        String ip = getLocalIp();
        try {
            request = new HttpGet(new URI(unregisterUrl + "?ip=" + ip + "&" + json));
            try {
                httpclient.execute(request);
            } catch (ClientProtocolException e) {
                e.printStackTrace();
            } catch (IOException e) {
                e.printStackTrace();
            }
        } catch (URISyntaxException e1) {
            e1.printStackTrace();
        }
        if (client != null) {
            try {
                client.close();
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
        if (socket != null) {
            try {
                socket.close();
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
        if (dataInputStream != null) {
            try {
                dataInputStream.close();
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
        if (dataOutputStream != null) {
            try {
                dataOutputStream.close();
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
        super.onStop();
    }

    private String getLocalIp() {
        try {
            for (Enumeration<NetworkInterface> en = NetworkInterface.getNetworkInterfaces(); en.hasMoreElements(); ) {
                NetworkInterface intf = en.nextElement();
                for (Enumeration<InetAddress> enumIpAddr = intf.getInetAddresses(); enumIpAddr.hasMoreElements(); ) {
                    InetAddress inetAddress = enumIpAddr.nextElement();
                    if (!inetAddress.isLoopbackAddress()) {
                        return inetAddress.getHostAddress().toString();
                    }
                }
            }
        } catch (SocketException e) {
            e.printStackTrace();
        }
        return null;
    }

    public class ServerThread implements Runnable {

        @Override
        public void run() {
            while (!Engine.engine.isPlayingMultiplayer()) {
                try {
                    client = serverSocket.accept();
                    dataInputStream = new DataInputStream(client.getInputStream());
                    byte messageType = dataInputStream.readByte();
                    switch(messageType) {
                        case 1:
                            String message = dataInputStream.readUTF();
                            System.out.println(message);
                            AlertDialog.Builder builder;
                            builder = new AlertDialog.Builder(activity);
                            builder.setMessage("Accept challenge from:\nName: ").setCancelable(true).setPositiveButton("Yes", new DialogInterface.OnClickListener() {

                                @Override
                                public void onClick(DialogInterface dialog, int which) {
                                }
                            }).setNegativeButton("No", new DialogInterface.OnClickListener() {

                                @Override
                                public void onClick(DialogInterface dialog, int which) {
                                }
                            });
                            AlertDialog alert = builder.create();
                            alert.show();
                    }
                    break;
                } catch (IOException e) {
                    e.printStackTrace();
                }
            }
        }
    }
}
