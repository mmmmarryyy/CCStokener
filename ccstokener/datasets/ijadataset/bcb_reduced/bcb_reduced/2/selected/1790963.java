package net.sylvek.where;

import android.app.AlertDialog;
import android.app.Dialog;
import android.app.NotificationManager;
import android.app.ProgressDialog;
import android.app.AlertDialog.Builder;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.DialogInterface.OnClickListener;
import android.graphics.Bitmap;
import android.location.Address;
import android.location.Geocoder;
import android.location.Location;
import android.net.Uri;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.os.Message;
import android.os.PowerManager;
import android.os.PowerManager.WakeLock;
import android.preference.PreferenceManager;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.view.Window;
import android.webkit.JsResult;
import android.webkit.WebChromeClient;
import android.webkit.WebView;
import android.webkit.WebViewClient;
import android.widget.Button;
import android.widget.Toast;
import com.google.android.maps.GeoPoint;
import com.google.android.maps.MapActivity;
import com.google.android.maps.MapView;
import com.google.android.maps.OverlayItem;
import com.restfb.Connection;
import com.restfb.DefaultFacebookClient;
import com.restfb.Facebook;
import com.restfb.FacebookClient;
import com.restfb.FacebookException;
import com.restfb.Parameter;
import com.restfb.types.User;
import net.sylvek.where.MyOverlay.OnSaveMyPosition;
import org.apache.http.HttpResponse;
import org.apache.http.client.ClientProtocolException;
import org.apache.http.client.HttpClient;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.impl.client.DefaultHttpClient;
import org.apache.http.util.EntityUtils;
import org.json.JSONException;
import org.json.JSONObject;
import java.io.IOException;
import java.net.URLEncoder;
import java.security.NoSuchAlgorithmException;
import java.util.List;
import java.util.concurrent.Executors;

public class Where extends MapActivity {

    static final String PREF_PRIVATE_MODE = "net.sylvek.where.private";

    static final String PREF_FRIENDS_MODE = "net.sylvek.where.friends";

    static final String PREF_UPDATE_DISTANCE = "net.sylvek.where.service.distance";

    static final String PREF_UPDATE_TIME = "net.sylvek.where.service.time";

    static final String PREF_UPDATE_SERVICE = "net.sylvek.where.service.update_location";

    static final String PREF_CHECK_EXT_PERM = "net.sylvek.where.permission_check";

    static final String PREF_PUBLISH_AUTO = "net.sylvek.where.permission_auto";

    static final String MY_FRIENDS_MODE = "my.friends.mode";

    static final String LAST_FRIENDS_MODE = "last.friends.mode";

    static final String NEAR_ME_MODE = "nearme.friends.mode";

    static final String ONLINE_FRIENDS_MODE = "online.friends.mode";

    static final int BEGIN = 200;

    static final int MENU_ABOUT = 0;

    static final int MENU_CENTER_ME = 1;

    static final int MENU_IMPORT_FRIENDS = 2;

    static final int MENU_BAD_API_VERSION = 3;

    static final int MENU_FIND = 5;

    static final int MENU_SHARE = 7;

    static final int MENU_SETTINGS = 8;

    static final int MENU_CONNECTION = 11;

    static final int MENU_RESET = 12;

    static final int MENU_REGISTRATION = 13;

    static final int MENU_QUIT = 15;

    static final int SETTINGS = 1;

    static final int INTERNAL_LONG = 60000;

    static final int UID = 56464;

    static final String STORED_ACCESS_TOKEN = "net.sylvek.where.stored_access_token";

    static final String STORED_ID = "net.sylvek.where.stored_id";

    static final String STORED_NAME = "net.sylvek.where.stored_name";

    MapView map;

    MyOverlay myLocation;

    FriendsOverlay facebook;

    Handler process;

    Handler message;

    Handler handler = new Handler();

    long[] friends = new long[0];

    ProgressDialog welcome, importFriends;

    SharedPreferences pref;

    FacebookClient facebookClient;

    WakeLock lock;

    boolean goodApi = false;

    final Runnable hideWelcome = new Runnable() {

        public void run() {
            welcome.hide();
        }
    };

    final Runnable hideImportFriends = new Runnable() {

        public void run() {
            importFriends.hide();
        }

        ;
    };

    private void initHandler() {
        message = new Handler() {

            @Override
            public void handleMessage(Message msg) {
                Toast.makeText(Where.this, msg.getData().getString("message"), Toast.LENGTH_LONG).show();
            }
        };
        process = new Handler() {

            FriendOverlay[] friends = null;

            int nbFriends = 0;

            @Override
            public void handleMessage(Message msg) {
                getWindow().setFeatureInt(Window.FEATURE_PROGRESS, msg.what);
                switch(msg.what) {
                    case FindFriends.EMPTY:
                        friends = new FriendOverlay[FriendsOverlay.MAX];
                        nbFriends = 0;
                        break;
                    default:
                        if (nbFriends < FriendsOverlay.MAX) {
                            Bundle data = msg.getData();
                            double latitude = data.getDouble(FriendOverlay.LATITUDE);
                            double longitude = data.getDouble(FriendOverlay.LONGITUDE);
                            String name = data.getString(FriendOverlay.NAME);
                            int uid = data.getInt(FriendOverlay.UID);
                            boolean isOnline = data.getBoolean(FriendOverlay.ONLINE);
                            String update = data.getString(FriendOverlay.SORTING);
                            GeoPoint p = new GeoPoint((int) (latitude * 1E6), (int) (longitude * 1E6));
                            FriendOverlay f = new FriendOverlay(p, name, uid, update, isOnline);
                            friends[nbFriends] = f;
                            nbFriends++;
                        }
                        break;
                    case FindFriends.DONE:
                        facebook.update(friends, nbFriends);
                        map.invalidate();
                        break;
                }
            }
        };
    }

    private void initMap() {
        map = (MapView) findViewById(R.id.mapview);
        map.setClickable(true);
        map.setEnabled(true);
        map.setFocusable(true);
        map.setFocusableInTouchMode(true);
        map.setBuiltInZoomControls(true);
    }

    private void initWakeLock() {
        PowerManager powerManager = (PowerManager) getSystemService(Context.POWER_SERVICE);
        lock = powerManager.newWakeLock(PowerManager.FULL_WAKE_LOCK | PowerManager.ON_AFTER_RELEASE, "O�? tag");
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        requestWindowFeature(Window.FEATURE_PROGRESS);
        setContentView(R.layout.where);
        pref = PreferenceManager.getDefaultSharedPreferences(this);
        initMap();
        initHandler();
        initOverlays();
        initWelcomeDialog();
        initFirstTimeDialog();
        initWakeLock();
        goodApi = checkVersionApi();
        NotificationManager m = (NotificationManager) getSystemService(Context.NOTIFICATION_SERVICE);
        m.cancel(Where.UID);
    }

    private void launchImportFriends() {
        Executors.newCachedThreadPool().execute(new Runnable() {

            @Override
            public void run() {
                try {
                    Looper.prepare();
                    importFriends();
                } catch (Exception e) {
                    Log.e("get.friends", e.getMessage());
                } finally {
                    Looper.loop();
                }
            }
        });
    }

    public static class UserLocation extends User {

        @Facebook("hometown_location")
        String hometownLocation;

        public String getHometownLocation() {
            if (hometownLocation == null) {
                return null;
            }
            try {
                return new JSONObject(hometownLocation).optString("city", null);
            } catch (JSONException e) {
                Log.e("parse.hometown_location", e.getMessage());
                return null;
            }
        }
    }

    private void importFriends() throws ClientProtocolException, NoSuchAlgorithmException, IOException, JSONException, FacebookException {
        try {
            handler.post(new Runnable() {

                @Override
                public void run() {
                    lock.acquire();
                    importFriends.show();
                }
            });
            Geocoder geocoder = new Geocoder(this);
            final Connection<User> myFriends = facebookClient.fetchConnection("me/friends", User.class);
            handler.post(new Runnable() {

                @Override
                public void run() {
                    importFriends.setMax(myFriends.getData().size());
                }
            });
            int total = 0;
            for (User user : myFriends.getData()) {
                final int tt = total++;
                handler.post(new Runnable() {

                    @Override
                    public void run() {
                        importFriends.setProgress(tt);
                    }
                });
                List<UserLocation> location = null;
                try {
                    location = facebookClient.executeQuery("SELECT hometown_location.city FROM user WHERE uid=" + user.getId(), UserLocation.class);
                } catch (FacebookException e) {
                    Log.e("getting.hometown_location", e.getMessage());
                    continue;
                }
                UserLocation friend = location.get(0);
                if (friend.getLocation() != null) {
                    List<Address> address = geocoder.getFromLocationName(friend.getHometownLocation(), 1);
                    if (!address.isEmpty()) {
                        double lat = address.get(0).getLatitude();
                        double lon = address.get(0).getLongitude();
                        JSONObject result = SylvekClient.update(user.getName(), user.getId(), lat, lon, false);
                        if (!result.getBoolean("success")) {
                            Log.d("insert.friend", "impossible to add " + user.getName());
                        } else {
                            Log.d("insert.friend", "import of " + user.getName());
                        }
                        Bitmap bitmap = WhereUtils.getImageBitmap(FbClient.HTTP_GRAPH + user.getId() + "/picture");
                        boolean success = WhereUtils.storeToCache(Where.this, user.getId(), bitmap);
                        if (success) {
                            Log.d("store.photo", "store photo of " + user.getName());
                        } else {
                            Log.d("store.photo", "impossible to store photo of " + user.getName());
                        }
                    }
                }
            }
        } finally {
            handler.post(hideImportFriends);
            handler.post(new Runnable() {

                @Override
                public void run() {
                    lock.release();
                }
            });
            displayFriends();
        }
    }

    @Override
    protected Dialog onCreateDialog(int id) {
        switch(id) {
            case MENU_BAD_API_VERSION:
                return new AlertDialog.Builder(this).setTitle(R.string.bad_api_version).setMessage(R.string.need_upgrade).setCancelable(false).setPositiveButton(android.R.string.ok, new OnClickListener() {

                    @Override
                    public void onClick(DialogInterface dialog, int which) {
                        startActivity(new Intent(Intent.ACTION_VIEW, Uri.parse("market://search?q=pname:net.sylvek.where")));
                    }
                }).setNegativeButton(android.R.string.cancel, new OnClickListener() {

                    @Override
                    public void onClick(DialogInterface dialog, int which) {
                        finish();
                    }
                }).create();
            case MENU_IMPORT_FRIENDS:
                return new AlertDialog.Builder(this).setTitle(R.string.import_friends).setMessage(R.string.sure_to_import).setCancelable(false).setPositiveButton(android.R.string.yes, new OnClickListener() {

                    @Override
                    public void onClick(DialogInterface dialog, int which) {
                        launchImportFriends();
                    }
                }).setNegativeButton(android.R.string.no, null).create();
            case MENU_RESET:
                return new AlertDialog.Builder(this).setTitle(R.string.reset).setMessage(R.string.sure_to_delete).setPositiveButton(android.R.string.yes, new OnClickListener() {

                    @Override
                    public void onClick(DialogInterface dialog, int which) {
                        try {
                            SylvekClient.delete(FbClient.ID);
                            pref.edit().remove(STORED_ACCESS_TOKEN).remove(STORED_ID).remove(STORED_NAME).commit();
                        } catch (Exception e) {
                            Log.e("delete.user", e.getMessage());
                        }
                        showDialog(MENU_CONNECTION);
                    }
                }).setNegativeButton(android.R.string.no, null).create();
            case MENU_CONNECTION:
                Builder connectBuilder = new AlertDialog.Builder(Where.this).setCancelable(false);
                View connect = LayoutInflater.from(this).inflate(R.layout.connect, null);
                final Button getAbout = (Button) connect.findViewById(R.id.get_help);
                getAbout.setOnClickListener(new View.OnClickListener() {

                    @Override
                    public void onClick(View v) {
                        showDialog(MENU_ABOUT);
                    }
                });
                connectBuilder.setNegativeButton(R.string.quit, new OnClickListener() {

                    @Override
                    public void onClick(DialogInterface dialog, int which) {
                        finish();
                    }
                });
                connectBuilder.setPositiveButton(R.string.validate, new OnClickListener() {

                    public void onClick(DialogInterface arg0, int arg1) {
                        showDialog(MENU_REGISTRATION);
                    }
                });
                connectBuilder.setTitle(R.string.app_name);
                connectBuilder.setView(connect);
                return connectBuilder.setCancelable(false).create();
            case MENU_REGISTRATION:
                WebView facebook = new WebView(this) {

                    @Override
                    public boolean onCheckIsTextEditor() {
                        return true;
                    }
                };
                final AlertDialog dlg = new AlertDialog.Builder(this).setView(facebook).create();
                facebook.setWebChromeClient(new WebChromeClient() {

                    @Override
                    public boolean onJsAlert(WebView view, String url, String message, JsResult result) {
                        if (url.startsWith(FbClient.URL_REDIRECT)) {
                            dlg.cancel();
                            try {
                                HttpClient client = new DefaultHttpClient();
                                String target = FbClient.URL_ACCESS_TOKEN + URLEncoder.encode(message);
                                HttpResponse response = client.execute(new HttpGet(target));
                                if (response.getStatusLine().getStatusCode() == 200) {
                                    String access_token = EntityUtils.toString(response.getEntity());
                                    if (access_token.startsWith(FbClient.ACCESS_TOKEN_KEY)) {
                                        String accessToken = FbClient.parseAccessToken(access_token);
                                        Log.d("access_token", accessToken);
                                        facebookClient = new DefaultFacebookClient(accessToken);
                                        User me = facebookClient.fetchObject("me", User.class, Parameter.with("fields", "id, name"));
                                        FbClient.ID = me.getId();
                                        FbClient.NAME = me.getName();
                                        pref.edit().putString(STORED_ACCESS_TOKEN, accessToken).putString(STORED_ID, FbClient.ID).putString(STORED_NAME, FbClient.NAME).commit();
                                        launchImportFriends();
                                    }
                                } else {
                                    showDialog(MENU_REGISTRATION);
                                }
                            } catch (Exception e) {
                                Log.e("get.access_token", e.getMessage());
                                Toast.makeText(Where.this, e.getMessage(), Toast.LENGTH_LONG).show();
                                showDialog(MENU_REGISTRATION);
                            }
                            return true;
                        }
                        return false;
                    }
                });
                facebook.setWebViewClient(new WebViewClient() {

                    @Override
                    public boolean shouldOverrideUrlLoading(WebView view, String url) {
                        view.loadUrl(url);
                        return true;
                    }
                });
                facebook.getSettings().setJavaScriptEnabled(true);
                facebook.loadUrl(FbClient.URL_AUTH + FbClient.API_KEY);
                return dlg;
            case MENU_ABOUT:
                WebView webview = new WebView(this);
                webview.loadUrl(SylvekClient.HELP_WEB_PAGE);
                webview.setBackgroundColor(0);
                return new AlertDialog.Builder(this).setTitle(R.string.app_about).setView(webview).create();
        }
        return null;
    }

    private void initWelcomeDialog() {
        welcome = new ProgressDialog(this);
        welcome.setMessage(getResources().getString(R.string.app_desc));
        welcome.setCancelable(false);
        welcome.setButton(getString(R.string.app_about), new DialogInterface.OnClickListener() {

            @Override
            public void onClick(DialogInterface dialog, int which) {
                showDialog(MENU_ABOUT);
            }
        });
    }

    private void initFirstTimeDialog() {
        importFriends = new ProgressDialog(this);
        importFriends.setProgressStyle(ProgressDialog.STYLE_HORIZONTAL);
        importFriends.setMessage(getResources().getString(R.string.success));
        importFriends.setCancelable(false);
    }

    private void initFriends() {
        String friendsMode = pref.getString(PREF_FRIENDS_MODE, MY_FRIENDS_MODE);
        try {
            if (NEAR_ME_MODE.equals(friendsMode)) {
                Location location = myLocation.getLastFix();
                if (location != null) {
                    double lat = location.getLatitude();
                    double lon = location.getLongitude();
                    friends = SylvekClient.getUidsNearMe(FbClient.ID, lat, lon);
                } else {
                    myLocation.runOnFirstFix(new Runnable() {

                        @Override
                        public void run() {
                            Looper.prepare();
                            displayFriends();
                            Looper.loop();
                        }
                    });
                }
            }
            if (MY_FRIENDS_MODE.equals(friendsMode)) {
                final Connection<User> myFriends = facebookClient.fetchConnection("me/friends", User.class);
                long[] f = new long[myFriends.getData().size()];
                for (int i = 0; i < myFriends.getData().size(); i++) {
                    User friend = myFriends.getData().get(i);
                    f[i] = Long.parseLong(friend.getId());
                }
                friends = f;
            }
            if (LAST_FRIENDS_MODE.equals(friendsMode)) {
                friends = SylvekClient.getUids(FbClient.ID);
            }
            if (ONLINE_FRIENDS_MODE.equals(friendsMode)) {
                friends = SylvekClient.getOnlineUids(FbClient.ID);
            }
        } catch (Exception e) {
            Log.e("init.friends", e.getMessage());
            friends = new long[0];
        }
    }

    private void initOverlays() {
        facebook = new FriendsOverlay(this);
        myLocation = new MyOverlay(FbClient.NAME, this, map, new OnSaveMyPosition() {

            @Override
            public void onSave(Location lastFix) {
                Where.this.savePosition(lastFix, false);
            }
        });
        map.getOverlays().add(myLocation);
        map.getOverlays().add(facebook);
    }

    private void displayFriends() {
        Executors.newCachedThreadPool().execute(new Runnable() {

            public void run() {
                synchronized (process) {
                    initFriends();
                    FindFriends foundFriends = new FindFriends(process, friends);
                    foundFriends.run();
                }
            }
        });
        Toast.makeText(Where.this, R.string.refreshing, Toast.LENGTH_LONG).show();
    }

    private void askRegistration() {
        showDialog(MENU_CONNECTION);
    }

    @Override
    protected void onStop() {
        super.onStop();
        welcome.dismiss();
        importFriends.dismiss();
    }

    void savePosition(final Location location, final boolean online) {
        if (location != null && !pref.getBoolean(PREF_PRIVATE_MODE, false)) {
            Executors.newCachedThreadPool().execute(new Runnable() {

                @Override
                public void run() {
                    try {
                        if (FbClient.NAME != null && FbClient.ID != null) {
                            JSONObject result = SylvekClient.update(FbClient.NAME, FbClient.ID, location.getLatitude(), location.getLongitude(), online);
                            if (!result.getBoolean("success")) {
                                Log.e("save.position", "impossible to update location");
                            }
                        }
                    } catch (Exception e) {
                        Log.e("user.update", e.getMessage());
                    }
                }
            });
        }
    }

    private boolean checkVersionApi() {
        try {
            String currentApiVersion = SylvekClient.getApiVersion();
            return currentApiVersion.equals(SylvekClient.CURRENT_API_VERSION);
        } catch (Exception e) {
            Log.e("api.version", e.getMessage());
        }
        return true;
    }

    @Override
    protected void onResume() {
        super.onResume();
        myLocation.enableMyLocation();
        if (goodApi) {
            String accessToken = pref.getString(STORED_ACCESS_TOKEN, null);
            if (accessToken != null) {
                facebookClient = new DefaultFacebookClient(accessToken);
                FbClient.ID = pref.getString(STORED_ID, null);
                FbClient.NAME = pref.getString(STORED_NAME, null);
                displayFriends();
            } else {
                askRegistration();
            }
        } else {
            showDialog(MENU_BAD_API_VERSION);
        }
    }

    @Override
    protected void onPause() {
        super.onPause();
        myLocation.disableMyLocation();
    }

    @Override
    protected boolean isRouteDisplayed() {
        return true;
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, final Intent data) {
        switch(requestCode) {
            case SETTINGS:
                if (pref.getBoolean(PREF_PRIVATE_MODE, false)) {
                    try {
                        SylvekClient.delete(FbClient.ID);
                        Toast.makeText(this, R.string.update_private_mode_summary, Toast.LENGTH_LONG).show();
                    } catch (Exception e) {
                        Toast.makeText(this, e.getMessage(), Toast.LENGTH_LONG).show();
                    }
                }
                sendBroadcast(new Intent("net.sylvek.where.LAUNCH_UPDATE"));
                break;
            default:
                break;
        }
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        menu.add(Menu.NONE, MENU_CENTER_ME, Menu.NONE, R.string.center_me).setIcon(android.R.drawable.ic_menu_mylocation);
        menu.add(Menu.NONE, MENU_SHARE, Menu.NONE, R.string.share_location).setIcon(android.R.drawable.ic_menu_share);
        menu.add(Menu.NONE, MENU_FIND, Menu.NONE, R.string.find).setIcon(android.R.drawable.ic_menu_help);
        menu.add(Menu.NONE, MENU_SETTINGS, Menu.NONE, R.string.app_settings).setIcon(android.R.drawable.ic_menu_preferences);
        menu.add(Menu.NONE, MENU_ABOUT, Menu.NONE, R.string.app_about).setIcon(android.R.drawable.ic_menu_info_details);
        menu.add(Menu.NONE, MENU_IMPORT_FRIENDS, Menu.NONE, R.string.import_friends).setIcon(android.R.drawable.ic_menu_add);
        menu.add(Menu.NONE, MENU_RESET, Menu.NONE, R.string.reset).setIcon(android.R.drawable.ic_menu_revert);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(final MenuItem item) {
        switch(item.getItemId()) {
            default:
                showDialog(item.getItemId());
                break;
            case MENU_QUIT:
                finish();
                break;
            case MENU_FIND:
                AlertDialog dlg = new AlertDialog.Builder(this).setTitle(R.string.find).setItems(facebook.getNames(), new OnClickListener() {

                    public void onClick(DialogInterface dialog, int which) {
                        OverlayItem item = facebook.getItem(which);
                        if (item != null) {
                            facebook.setFocus(item);
                            GeoPoint point = item.getPoint();
                            myLocation.centerTo(point);
                            facebook.showProfile(item);
                            String text = WhereUtils.displayDistanceKm(item.getTitle(), myLocation.getMyLocation(), point);
                            Toast.makeText(Where.this, text, Toast.LENGTH_LONG).show();
                        }
                    }
                }).setPositiveButton(R.string.refresh, new OnClickListener() {

                    @Override
                    public void onClick(DialogInterface dialog, int which) {
                        displayFriends();
                    }
                }).setNegativeButton(R.string.close, null).create();
                dlg.show();
                break;
            case MENU_SHARE:
                if (myLocation == null || myLocation.getLastFix() == null) {
                    Toast.makeText(this, R.string.share_impossible, Toast.LENGTH_LONG).show();
                    return true;
                }
                Toast.makeText(this, R.string.share_possible, Toast.LENGTH_LONG).show();
                item.setEnabled(false);
                Executors.newCachedThreadPool().execute(new Runnable() {

                    @Override
                    public void run() {
                        String address = WhereUtils.getAddress(Where.this, myLocation.getLastFix());
                        String msg = WhereUtils.getCurrentStaticLocationUrl(Where.this, myLocation.getLastFix());
                        try {
                            msg = WhereUtils.getTinyLink(msg);
                        } catch (Exception e) {
                            Log.e("update.status", e.getMessage());
                        } finally {
                            handler.post(new Runnable() {

                                @Override
                                public void run() {
                                    item.setEnabled(true);
                                }
                            });
                        }
                        Intent t = new Intent(Intent.ACTION_SEND);
                        t.setType("text/plain");
                        t.addCategory(Intent.CATEGORY_DEFAULT);
                        t.putExtra(Intent.EXTRA_SUBJECT, getString(R.string.app_desc));
                        t.putExtra(Intent.EXTRA_TEXT, getString(R.string.currently_here, address, msg));
                        Intent share = Intent.createChooser(t, getString(R.string.share_location));
                        startActivity(share);
                    }
                });
                break;
            case MENU_SETTINGS:
                Intent about = new Intent(this, Settings.class);
                startActivityForResult(about, SETTINGS);
                break;
            case MENU_CENTER_ME:
                myLocation.centerMe(false);
                break;
        }
        return true;
    }
}
