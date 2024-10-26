package de.droidcachebox;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.MalformedURLException;
import java.net.URL;
import java.net.URLConnection;
import java.util.ArrayList;
import CB_Core.Config;
import CB_Core.FileIO;
import CB_Core.FilterProperties;
import CB_Core.GlobalCore;
import CB_Core.Api.GroundspeakAPI;
import CB_Core.Events.CachListChangedEventList;
import CB_Core.Log.ILog;
import CB_Core.Log.Logger;
import CB_Core.TranslationEngine.SelectedLangChangedEventList;
import CB_Core.Types.Cache;
import CB_Core.Types.Categories;
import CB_Core.Types.Category;
import CB_Core.Types.Coordinate;
import CB_Core.Types.GpxFilename;
import CB_Core.Types.LogEntry;
import CB_Core.Types.Waypoint;
import de.droidcachebox.ExtAudioRecorder;
import de.droidcachebox.Components.CacheNameView;
import de.droidcachebox.Components.search;
import de.droidcachebox.Custom_Controls.DebugInfoPanel;
import de.droidcachebox.Custom_Controls.DescriptionViewControl;
import de.droidcachebox.Custom_Controls.Mic_On_Flash;
import de.droidcachebox.Custom_Controls.downSlider;
import de.droidcachebox.Custom_Controls.IconContextMenu.IconContextMenu;
import de.droidcachebox.Custom_Controls.IconContextMenu.IconContextMenu.IconContextItemSelectedListener;
import de.droidcachebox.Custom_Controls.QuickButtonList.HorizontalListView;
import de.droidcachebox.Custom_Controls.QuickButtonList.QuickButtonItem;
import de.droidcachebox.DAO.CacheDAO;
import de.droidcachebox.DAO.CacheListDAO;
import de.droidcachebox.DAO.CategoryDAO;
import de.droidcachebox.DAO.LogDAO;
import de.droidcachebox.DAO.WaypointDAO;
import de.droidcachebox.Enums.Actions;
import de.droidcachebox.Events.GpsStateChangeEvent;
import de.droidcachebox.Events.GpsStateChangeEventList;
import de.droidcachebox.Events.PositionEventList;
import CB_Core.Events.SelectedCacheEvent;
import CB_Core.Events.SelectedCacheEventList;
import de.droidcachebox.Events.ViewOptionsMenu;
import de.droidcachebox.Locator.GPS;
import de.droidcachebox.Locator.GPS.GpsStatusListener;
import de.droidcachebox.Locator.Locator;
import de.droidcachebox.Map.Descriptor;
import de.droidcachebox.Map.Layer;
import de.droidcachebox.Map.Descriptor.PointD;
import de.droidcachebox.Ui.ActivityUtils;
import de.droidcachebox.Ui.AllContextMenuCallHandler;
import de.droidcachebox.Ui.Sizes;
import de.droidcachebox.Views.AboutView;
import de.droidcachebox.Views.CacheListView;
import de.droidcachebox.Views.CompassView;
import de.droidcachebox.Views.EmptyViewTemplate;
import de.droidcachebox.Views.FieldNotesView;
import de.droidcachebox.Views.JokerView;
import de.droidcachebox.Views.LogView;
import de.droidcachebox.Views.MapView;
import de.droidcachebox.Views.NotesView;
import de.droidcachebox.Views.SolverView;
import de.droidcachebox.Views.SpoilerView;
import de.droidcachebox.Views.TrackListView;
import de.droidcachebox.Views.WaypointView;
import de.droidcachebox.Views.DescriptionView;
import de.droidcachebox.Views.FilterSettings.EditFilterSettings;
import de.droidcachebox.Views.FilterSettings.PresetListView;
import de.droidcachebox.Views.FilterSettings.PresetListViewItem;
import de.droidcachebox.Views.Forms.GcApiLogin;
import de.droidcachebox.Views.Forms.HintDialog;
import de.droidcachebox.Views.Forms.ImportDialog;
import de.droidcachebox.Views.Forms.MessageBoxButtons;
import de.droidcachebox.Views.Forms.MessageBoxIcon;
import de.droidcachebox.Views.Forms.ScreenLock;
import de.droidcachebox.Views.Forms.SelectDB;
import de.droidcachebox.Views.Forms.Settings;
import de.droidcachebox.Views.Forms.MessageBox;
import de.droidcachebox.Views.MapView.SmoothScrollingTyp;
import de.droidcachebox.Database;
import CB_Core.Types.CacheList;
import android.app.Activity;
import android.app.AlertDialog;
import android.app.ProgressDialog;
import android.database.Cursor;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.pm.ActivityInfo;
import android.content.ContentValues;
import android.graphics.Color;
import android.graphics.PorterDuff.Mode;
import android.hardware.Sensor;
import android.hardware.SensorEvent;
import android.hardware.SensorEventListener;
import android.hardware.SensorManager;
import android.location.Criteria;
import android.location.Location;
import android.location.LocationListener;
import android.location.LocationManager;
import android.media.AudioManager;
import android.net.Uri;
import android.os.Bundle;
import android.os.CountDownTimer;
import android.os.Handler;
import android.os.Message;
import android.os.PowerManager;
import android.os.Vibrator;
import android.provider.MediaStore;
import android.util.Log;
import android.view.ContextMenu;
import android.view.ContextMenu.ContextMenuInfo;
import android.view.KeyEvent;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.View.OnClickListener;
import android.view.ViewGroup;
import android.view.ViewGroup.LayoutParams;
import android.view.ViewParent;
import android.view.Window;
import android.widget.AdapterView;
import android.widget.BaseAdapter;
import android.widget.FrameLayout;
import android.widget.ImageButton;
import android.widget.LinearLayout;
import android.widget.Toast;
import android.widget.AdapterView.OnItemClickListener;
import android.location.GpsStatus;

public class main extends Activity implements SelectedCacheEvent, LocationListener, CB_Core.Events.CacheListChangedEvent, GpsStatus.NmeaListener, ILog, GpsStateChangeEvent {

    public static Integer aktViewId = -1;

    private static long GPSTimeStamp = 0;

    public static MapView mapView = null;

    private static CacheListView cacheListView = null;

    public static WaypointView waypointView = null;

    private static LogView logView = null;

    public static DescriptionView descriptionView = null;

    private static SpoilerView spoilerView = null;

    private static NotesView notesView = null;

    private static SolverView solverView = null;

    private static CompassView compassView = null;

    public static FieldNotesView fieldNotesView = null;

    private static EmptyViewTemplate TestEmpty = null;

    private static AboutView aboutView = null;

    private static JokerView jokerView = null;

    private static TrackListView tracklistView = null;

    public static LinearLayout strengthLayout;

    public LinearLayout searchLayout;

    private search Search;

    /**
	 * Night Mode aktive
	 */
    public static Boolean N = false;

    private static final int CAPTURE_IMAGE_ACTIVITY_REQUEST_CODE = 61216516;

    private static final int CAPTURE_VIDEO_ACTIVITY_REQUEST_CODE = 61216517;

    private static Uri cameraVideoURI;

    private static File mediafile = null;

    private static String mediafilename = null;

    private static String mediaTimeString = null;

    private static String basename = null;

    private static String mediaCacheName = null;

    private static Coordinate mediaCoordinate = null;

    private static Boolean mVoiceRecIsStart = false;

    public static Activity mainActivity;

    public static Boolean isRestart = false;

    public static Boolean isFirstStart = true;

    private LayoutInflater inflater;

    private ExtAudioRecorder extAudioRecorder = null;

    private boolean initialResortAfterFirstFixCompleted = false;

    private boolean initialFixSoundCompleted = false;

    private boolean approachSoundCompleted = false;

    private boolean runsWithAkku = true;

    private ImageButton buttonDB;

    private ImageButton buttonCache;

    private ImageButton buttonNav;

    private ImageButton buttonTools;

    private ImageButton buttonMisc;

    private FrameLayout frame;

    private LinearLayout TopLayout;

    public downSlider InfoDownSlider;

    public HorizontalListView QuickButtonList;

    private Mic_On_Flash Mic_Icon;

    private static DebugInfoPanel debugInfoPanel;

    private ViewOptionsMenu aktView = null;

    private CacheNameView cacheNameView;

    private ArrayList<View> ViewList = new ArrayList<View>();

    private int lastBtnDBView = 1;

    private int lastBtnCacheView = 4;

    private int lastBtnNavView = 0;

    private int lastBtnToolsView = -1;

    private int lastBtnMiscView = 11;

    ArrayList<Integer> btnDBActionIds;

    ArrayList<Integer> btnCacheActionIds;

    ArrayList<Integer> btnNavActionIds;

    ArrayList<Integer> btnToolsActionIds;

    ArrayList<Integer> btnMiscActionIds;

    protected PowerManager.WakeLock mWakeLock;

    public static LocationManager locationManager;

    private SensorManager mSensorManager;

    private Sensor mSensor;

    private float[] mCompassValues;

    private enum nextMenuType {

        nmDB, nmCache, nmMap, nmInfo, nmMisc
    }

    private nextMenuType nextMenu = nextMenuType.nmDB;

    public Boolean getVoiceRecIsStart() {
        return mVoiceRecIsStart;
    }

    private ScreenLockTimer counter = null;

    private boolean counterStopped = false;

    private Vibrator vibrator;

    private class ScreenLockTimer extends CountDownTimer {

        public ScreenLockTimer(long millisInFuture, long countDownInterval) {
            super(millisInFuture, countDownInterval);
        }

        @Override
        public void onFinish() {
            startScreenLock();
        }

        @Override
        public void onTick(long millisUntilFinished) {
        }
    }

    /** Called when the activity is first created. */
    @Override
    public void onCreate(Bundle savedInstanceState) {
        ActivityUtils.onActivityCreateSetTheme(this);
        super.onCreate(savedInstanceState);
        requestWindowFeature(Window.FEATURE_NO_TITLE);
        if (!Config.GetBool("AllowLandscape")) {
            setRequestedOrientation(ActivityInfo.SCREEN_ORIENTATION_PORTRAIT);
        } else {
            setRequestedOrientation(ActivityInfo.SCREEN_ORIENTATION_SENSOR);
        }
        Logger.Add(this);
        N = Config.GetBool("nightMode");
        try {
            setContentView(R.layout.main);
        } catch (Exception exc) {
            Logger.Error("main.onCreate()", "setContentView", exc);
        }
        inflater = (LayoutInflater) this.getSystemService(Context.LAYOUT_INFLATER_SERVICE);
        mainActivity = this;
        AllContextMenuCallHandler.Main = this;
        mainActivity.setVolumeControlStream(AudioManager.STREAM_MUSIC);
        Sizes.initial(false, this);
        int Time = ((Config.GetInt("LockM") * 60) + Config.GetInt("LockSec")) * 1000;
        counter = new ScreenLockTimer(Time, Time);
        counter.start();
        findViewsById();
        SelectedCacheEventList.Add(this);
        CachListChangedEventList.Add(this);
        GpsStateChangeEventList.Add(this);
        vibrator = (Vibrator) getSystemService(Context.VIBRATOR_SERVICE);
        mSensorManager = (SensorManager) getSystemService(Context.SENSOR_SERVICE);
        mSensor = mSensorManager.getDefaultSensor(Sensor.TYPE_ORIENTATION);
        final PowerManager pm = (PowerManager) getSystemService(Context.POWER_SERVICE);
        this.mWakeLock = pm.newWakeLock(PowerManager.SCREEN_DIM_WAKE_LOCK, "My Tag");
        this.mWakeLock.acquire();
        initialLocationManager();
        initialMapView();
        initialViews();
        initalMicIcon();
        initialButtons();
        initialCaheInfoSlider();
        if (GlobalCore.SelectedCache() == null) {
            CacheList cacheList = Database.Data.Query;
            if (cacheList.size() > 0) {
                Cache cache = cacheList.get(0);
                GlobalCore.SelectedCache(cache);
            }
        } else {
            GlobalCore.SelectedCache(GlobalCore.SelectedCache());
        }
        if (aktViewId != -1) {
            showView(aktViewId);
        } else {
            Logger.General("------ Start Rev: " + Global.CurrentRevision + "-------");
            showView(11);
            if (N) {
                ActivityUtils.changeToTheme(mainActivity, ActivityUtils.THEME_NIGHT, true);
            }
        }
        compassView.reInit();
        Global.InitIcons(this);
        CacheListChangedEvent();
        setDebugVisible();
        if (Config.GetBool("TrackRecorderStartup")) TrackRecorder.StartRecording();
        this.registerReceiver(this.mBatInfoReceiver, new IntentFilter(Intent.ACTION_BATTERY_CHANGED));
        downSlider.isInitial = false;
        int sollHeight = (Config.GetBool("quickButtonShow") && Config.GetBool("quickButtonLastShow")) ? Sizes.getQuickButtonListHeight() : 0;
        setQuickButtonHeight(sollHeight);
        if (isFirstStart) {
            if (Config.GetBool("newInstall") && Config.GetString("GcAPI").equals("")) {
                askToGetApiKey();
            } else {
                chkGpsIsOn();
            }
        }
        Search = new search(this);
    }

    /** hook into menu button for activity */
    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        return super.onCreateOptionsMenu(menu);
    }

    @Override
    public boolean onPrepareOptionsMenu(Menu menu) {
        menu.clear();
        int menuId = aktView.GetMenuId();
        if (menuId > 0) {
            getMenuInflater().inflate(menuId, menu);
        }
        aktView.BeforeShowMenu(menu);
        return super.onPrepareOptionsMenu(menu);
    }

    /** when menu button option selected */
    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        if (aktView != null) return aktView.ItemSelected(item);
        return super.onOptionsItemSelected(item);
    }

    @Override
    public void onUserInteraction() {
        if (counterStopped) return;
        if (counter != null) counter.start();
    }

    @Override
    public void SelectedCacheChanged(Cache cache, Waypoint waypoint) {
        approachSoundCompleted = false;
        cacheListView.setSelectedCacheVisible(0);
        QuickButtonList.invalidate();
    }

    public void newLocationReceived(Location location) {
        try {
            Global.Locator.setLocation(location);
        } catch (Exception e) {
            Logger.Error("main.newLocationReceived()", "Global.Locator.setLocation(location)", e);
            e.printStackTrace();
        }
        try {
            PositionEventList.Call(location);
        } catch (Exception e) {
            Logger.Error("main.newLocationReceived()", "PositionEventList.Call(location)", e);
            e.printStackTrace();
        }
        try {
            InfoDownSlider.setNewLocation(location);
        } catch (Exception e) {
            Logger.Error("main.newLocationReceived()", "InfoDownSlider.setNewLocation(location)", e);
            e.printStackTrace();
        }
        try {
            if (!initialResortAfterFirstFixCompleted && GlobalCore.LastValidPosition.Valid) {
                if (GlobalCore.SelectedCache() == null) {
                    CacheDAO cacheDAO = new CacheDAO();
                    Database.Data.Query.Resort();
                }
                initialResortAfterFirstFixCompleted = true;
            }
        } catch (Exception e) {
            Logger.Error("main.newLocationReceived()", "if (!initialResortAfterFirstFixCompleted && GlobalCore.LastValidPosition.Valid)", e);
            e.printStackTrace();
        }
        try {
            if (!initialFixSoundCompleted && GlobalCore.LastValidPosition.Valid) {
                Global.PlaySound("GPS_Fix.wav");
                initialFixSoundCompleted = true;
            }
        } catch (Exception e) {
            Logger.Error("main.newLocationReceived()", "Global.PlaySound(GPS_Fix.wav)", e);
            e.printStackTrace();
        }
        try {
            if (GlobalCore.SelectedCache() != null) {
                float distance = GlobalCore.SelectedCache().Distance(false);
                if (GlobalCore.SelectedWaypoint() != null) {
                    distance = GlobalCore.SelectedWaypoint().Distance();
                }
                if (!approachSoundCompleted && (distance < Config.GetInt("SoundApproachDistance"))) {
                    Global.PlaySound("Approach.wav");
                    approachSoundCompleted = true;
                }
            }
        } catch (Exception e) {
            Logger.Error("main.newLocationReceived()", "Global.PlaySound(Approach.wav)", e);
            e.printStackTrace();
        }
        try {
            TrackRecorder.recordPosition();
        } catch (Exception e) {
            Logger.Error("main.newLocationReceived()", "TrackRecorder.recordPosition()", e);
            e.printStackTrace();
        }
        try {
            if (!GlobalCore.ResortAtWork) {
                if (Global.autoResort && ((aktView == mapView) || (aktView == cacheListView))) {
                    int z = 0;
                    if (!(GlobalCore.NearestCache() == null)) {
                        for (Cache cache : Database.Data.Query) {
                            z++;
                            if (z >= 50) return;
                            if (cache.Distance(true) < GlobalCore.NearestCache().Distance(true)) {
                                CacheDAO cacheDAO = new CacheDAO();
                                Database.Data.Query.Resort();
                                Global.PlaySound("AutoResort.wav");
                                return;
                            }
                        }
                    }
                }
            }
        } catch (Exception e) {
            Logger.Error("main.newLocationReceived()", "Resort", e);
            e.printStackTrace();
        }
    }

    @Override
    public void onLocationChanged(Location location) {
        try {
            if (location.getProvider().equalsIgnoreCase(LocationManager.GPS_PROVIDER)) {
                newLocationReceived(location);
                GPSTimeStamp = java.lang.System.currentTimeMillis();
                return;
            }
        } catch (Exception e) {
            Logger.Error("main.onLocationChanged()", "GPS_PROVIDER", e);
            e.printStackTrace();
        }
        try {
            if (location.getProvider().equalsIgnoreCase(LocationManager.NETWORK_PROVIDER)) {
                if ((java.lang.System.currentTimeMillis() - GPSTimeStamp) > NetworkPositionTime) {
                    NetworkPositionTime = 90000;
                    newLocationReceived(location);
                    Toast.makeText(mainActivity, "Network-Position", Toast.LENGTH_SHORT).show();
                }
            }
        } catch (Exception e) {
            Logger.Error("main.onLocationChanged()", "NETWORK_PROVIDER", e);
            e.printStackTrace();
        }
    }

    private int NetworkPositionTime = 10000;

    @Override
    public void onProviderDisabled(String provider) {
    }

    @Override
    public void onProviderEnabled(String provider) {
    }

    @Override
    public void onStatusChanged(String provider, int status, Bundle extras) {
    }

    @Override
    public boolean onKeyDown(int keyCode, KeyEvent event) {
        Log.d("SolHunter", "Key event code " + keyCode);
        if (keyCode == KeyEvent.KEYCODE_BACK) {
            DialogInterface.OnClickListener dialogClickListener = new DialogInterface.OnClickListener() {

                @Override
                public void onClick(DialogInterface dialog, int which) {
                    switch(which) {
                        case -1:
                            try {
                                finish();
                            } catch (Throwable e) {
                                e.printStackTrace();
                            }
                            break;
                        case -2:
                            dialog.dismiss();
                            break;
                    }
                }
            };
            MessageBox.Show(Global.Translations.Get("QuitReally"), Global.Translations.Get("Quit?"), MessageBoxButtons.YesNo, MessageBoxIcon.Question, dialogClickListener);
            return true;
        }
        return false;
    }

    @Override
    public void CacheListChangedEvent() {
        int ButtonBackGroundResource = 0;
        if ((Global.LastFilter == null) || (Global.LastFilter.ToString().equals("")) || (PresetListViewItem.chkPresetFilter(PresetListView.presets[0], Global.LastFilter.ToString())) && !Global.LastFilter.isExtendsFilter()) {
            ButtonBackGroundResource = N ? R.drawable.night_db_button_image_selector : R.drawable.db_button_image_selector;
        } else {
            ButtonBackGroundResource = N ? R.drawable.night_db_button_image_selector_filter : R.drawable.db_button_image_selector_filter;
        }
        ;
        this.buttonDB.setBackgroundResource(ButtonBackGroundResource);
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        if (requestCode == 546132) {
            if (resultCode == RESULT_OK) {
                Database db = new Database(Database.DatabaseType.CacheBox, mainActivity);
                if (!db.StartUp(Config.GetString("DatabasePath"))) return;
                Database.Data = null;
                Database.Data = db;
                GlobalCore.Categories = new Categories();
                Global.LastFilter = (Config.GetString("Filter").length() == 0) ? new FilterProperties(PresetListView.presets[0]) : new FilterProperties(Config.GetString("Filter"));
                Database.Data.GPXFilenameUpdateCacheCount();
                String sqlWhere = Global.LastFilter.getSqlWhere();
                Logger.General("Main.ApplyFilter: " + sqlWhere);
                Database.Data.Query.clear();
                CacheListDAO cacheListDAO = new CacheListDAO();
                cacheListDAO.ReadCacheList(Database.Data.Query, sqlWhere);
                GlobalCore.SelectedCache(null);
                GlobalCore.SelectedWaypoint(null, null);
                CachListChangedEventList.Call();
                downSlider.isInitial = false;
            }
            return;
        }
        if (requestCode == CAPTURE_IMAGE_ACTIVITY_REQUEST_CODE) {
            if (resultCode == RESULT_OK) {
                Log.d("DroidCachebox", "Picture taken!!!");
                GlobalCore.SelectedCache().ReloadSpoilerRessources();
                String MediaFolder = Config.GetString("UserImageFolder");
                String TrackFolder = Config.GetString("TrackFolder");
                String relativPath = FileIO.getRelativePath(MediaFolder, TrackFolder, "/");
                mediaTimeString = Global.GetTrackDateTimeString();
                TrackRecorder.AnnotateMedia(basename + ".jpg", relativPath + "/" + basename + ".jpg", GlobalCore.LastValidPosition, mediaTimeString);
                return;
            } else {
                Log.d("DroidCachebox", "Picture NOT taken!!!");
                return;
            }
        }
        if (requestCode == CAPTURE_VIDEO_ACTIVITY_REQUEST_CODE) {
            if (resultCode == RESULT_OK) {
                Log.d("DroidCachebox", "Video taken!!!");
                String[] projection = { MediaStore.Video.Media.DATA, MediaStore.Video.Media.SIZE };
                Cursor cursor = managedQuery(cameraVideoURI, projection, null, null, null);
                int column_index_data = cursor.getColumnIndexOrThrow(MediaStore.Video.Media.DATA);
                cursor.moveToFirst();
                String recordedVideoFilePath = cursor.getString(column_index_data);
                String ext = FileIO.GetFileExtension(recordedVideoFilePath);
                String MediaFolder = Config.GetString("UserImageFolder");
                File source = new File(recordedVideoFilePath);
                File destination = new File(MediaFolder + "/" + basename + "." + ext);
                if (!source.renameTo(destination)) {
                    Log.d("DroidCachebox", "Fehler beim Umbenennen der Datei: " + source.getName());
                }
                String TrackFolder = Config.GetString("TrackFolder");
                String relativPath = FileIO.getRelativePath(MediaFolder, TrackFolder, "/");
                TrackRecorder.AnnotateMedia(basename + "." + ext, relativPath + "/" + basename + "." + ext, mediaCoordinate, mediaTimeString);
                return;
            } else {
                Log.d("DroidCachebox", "Video NOT taken!!!");
                return;
            }
        }
        if (requestCode == 12345) {
            counterStopped = false;
            counter.start();
            return;
        }
        if (requestCode == 123456) {
            return;
        }
        if (requestCode == 987654321) {
            chkGpsIsOn();
        }
        aktView.ActivityResult(requestCode, resultCode, data);
    }

    @Override
    public void onCreateContextMenu(final ContextMenu menu, View v, ContextMenuInfo menuInfo) {
        super.onCreateContextMenu(menu, v, menuInfo);
        MenuInflater inflater = getMenuInflater();
        if (v instanceof ViewOptionsMenu) {
            int id = ((ViewOptionsMenu) v).GetContextMenuId();
            if (id > 0) {
                inflater.inflate(id, menu);
                ((ViewOptionsMenu) v).BeforeShowContextMenu(menu);
            }
            return;
        }
        if (v == buttonDB) {
            AllContextMenuCallHandler.showBtnListsContextMenu();
        } else if (v == buttonCache) {
            AllContextMenuCallHandler.showBtnCacheContextMenu();
        } else if (v == buttonNav) {
            AllContextMenuCallHandler.showBtnNavContextMenu();
        } else if (v == buttonTools) {
            AllContextMenuCallHandler.showBtnToolsContextMenu();
        } else if (v == buttonMisc) {
            AllContextMenuCallHandler.showBtnMiscContextMenu();
        }
    }

    @Override
    protected void onResume() {
        super.onResume();
        if (runsWithAkku) counter.start();
        mSensorManager.registerListener(mListener, mSensor, SensorManager.SENSOR_DELAY_GAME);
        this.registerReceiver(this.mBatInfoReceiver, new IntentFilter(Intent.ACTION_BATTERY_CHANGED));
        if (!Config.GetBool("AllowLandscape")) {
            setRequestedOrientation(ActivityInfo.SCREEN_ORIENTATION_PORTRAIT);
        } else {
            setRequestedOrientation(ActivityInfo.SCREEN_ORIENTATION_SENSOR);
        }
        int sollHeight = (Config.GetBool("quickButtonShow") && Config.GetBool("quickButtonLastShow")) ? Sizes.getQuickButtonListHeight() : 0;
        ((main) main.mainActivity).setQuickButtonHeight(sollHeight);
        downSlider.isInitial = false;
        InfoDownSlider.invalidate();
    }

    @Override
    protected void onStop() {
        mSensorManager.unregisterListener(mListener);
        this.unregisterReceiver(this.mBatInfoReceiver);
        counter.cancel();
        super.onStop();
    }

    @Override
    public void onDestroy() {
        frame.removeAllViews();
        if (isRestart) {
            super.onDestroy();
            isRestart = false;
        } else {
            if (isFinishing()) {
                Config.Set("MapInitLatitude", mapView.center.Latitude);
                Config.Set("MapInitLongitude", mapView.center.Longitude);
                Config.AcceptChanges();
                this.mWakeLock.release();
                counter.cancel();
                TrackRecorder.StopRecording();
                locationManager.removeUpdates(this);
                if (extAudioRecorder != null) {
                    extAudioRecorder.stop();
                    extAudioRecorder.release();
                    extAudioRecorder = null;
                }
                GlobalCore.SelectedCache(null);
                SelectedCacheEventList.list.clear();
                PositionEventList.list.clear();
                SelectedCacheEventList.list.clear();
                SelectedLangChangedEventList.list.clear();
                CachListChangedEventList.list.clear();
                if (aktView != null) {
                    aktView.OnHide();
                    aktView.OnFree();
                }
                aktView = null;
                for (View vom : ViewList) {
                    if (vom instanceof ViewOptionsMenu) {
                        ((ViewOptionsMenu) vom).OnFree();
                    }
                }
                ViewList.clear();
                TestEmpty = null;
                cacheListView = null;
                mapView = null;
                notesView = null;
                jokerView = null;
                descriptionView = null;
                mainActivity = null;
                debugInfoPanel.OnFree();
                debugInfoPanel = null;
                InfoDownSlider = null;
                super.onDestroy();
                System.exit(0);
            } else {
                if (aktView != null) aktView.OnHide();
                super.onDestroy();
            }
        }
    }

    /**
	 * Startet die Bildschirm Sperre
	 */
    public void startScreenLock() {
        startScreenLock(false);
    }

    /**
	 * Startet die Bildschirm Sperre. Mit der der �bergabe von force = true,
	 * werden abfragen ob im Akkubetrieb oder die Zeit Einstellungen ignoriert.
	 * 
	 * @param force
	 */
    public void startScreenLock(boolean force) {
        if (!force) {
            if (!runsWithAkku) return;
            counter.cancel();
            counterStopped = true;
            if ((Config.GetInt("LockM") == 0 && Config.GetInt("LockSec") < 10)) return;
        }
        final Intent mainIntent = new Intent().setClass(this, ScreenLock.class);
        this.startActivityForResult(mainIntent, 12345);
    }

    private final View.OnClickListener ButtonOnClick = new OnClickListener() {

        @Override
        public void onClick(View v) {
            if (v == buttonDB) {
                showView(lastBtnDBView);
            } else if (v == buttonCache) {
                showView(lastBtnCacheView);
            } else if (v == buttonNav) {
                showView(lastBtnNavView);
            } else if (v == buttonTools) {
                showView(lastBtnToolsView);
            } else if (v == buttonMisc) {
                showView(lastBtnMiscView);
            }
        }
    };

    private final SensorEventListener mListener = new SensorEventListener() {

        public void onSensorChanged(SensorEvent event) {
            try {
                mCompassValues = event.values;
                Global.Locator.setCompassHeading(mCompassValues[0]);
                PositionEventList.Call(mCompassValues[0]);
            } catch (Exception e) {
                Logger.Error("main.mListener.onSensorChanged()", "", e);
                e.printStackTrace();
            }
        }

        public void onAccuracyChanged(Sensor sensor, int accuracy) {
        }
    };

    public void showView(Integer ID) {
        if (ID == -1) return;
        if (!(ID > ViewList.size())) {
            showView((ViewOptionsMenu) ViewList.get(ID));
        } else {
            switch(ID) {
                case 102:
                    final Intent mainIntent = new Intent().setClass(mainActivity, Settings.class);
                    Bundle b = new Bundle();
                    b.putSerializable("Show", -1);
                    mainIntent.putExtras(b);
                    mainActivity.startActivity(mainIntent);
                    break;
                case 101:
                    final Intent mainIntent1 = new Intent().setClass(mainActivity, EditFilterSettings.class);
                    mainActivity.startActivity(mainIntent1);
                    break;
                case 103:
                    final Intent mainIntent2 = new Intent().setClass(mainActivity, ImportDialog.class);
                    mainActivity.startActivity(mainIntent2);
                    break;
            }
        }
        if (btnDBActionIds.contains(ID)) {
            lastBtnDBView = ID;
            return;
        }
        if (btnCacheActionIds.contains(ID)) {
            lastBtnCacheView = ID;
            return;
        }
        if (btnNavActionIds.contains(ID)) {
            lastBtnNavView = ID;
            return;
        }
        if (btnToolsActionIds.contains(ID)) {
            lastBtnToolsView = ID;
            return;
        }
        if (btnMiscActionIds.contains(ID)) {
            lastBtnMiscView = ID;
            return;
        }
    }

    private void showView(ViewOptionsMenu view) {
        if (aktView != null) aktView.OnHide();
        aktView = view;
        frame.removeAllViews();
        ViewParent parent = ((View) aktView).getParent();
        if (parent != null) {
            ((FrameLayout) parent).removeAllViews();
        }
        frame.addView((View) aktView);
        aktView.OnShow();
        aktViewId = ViewList.indexOf(aktView);
        InfoDownSlider.invalidate();
        ((View) aktView).forceLayout();
    }

    public IconContextItemSelectedListener OnIconContextItemSelectedListener = new IconContextItemSelectedListener() {

        @Override
        public void onIconContextItemSelected(MenuItem item, Object info) {
            switch(item.getItemId()) {
                case R.id.miScreenLock:
                    startScreenLock(true);
                    break;
                case R.id.miDayNight:
                    switchDayNight();
                    break;
                case R.id.miSettings:
                    showView(102);
                    break;
                case R.id.miVoiceRecorder:
                    recVoice();
                    break;
                case R.id.miRecordVideo:
                    recVideo();
                    break;
                case R.id.miAbout:
                    showView(11);
                    break;
                case R.id.miImport:
                    showView(103);
                    break;
                case R.id.miLogView:
                    showView(3);
                    break;
                case R.id.miSpoilerView:
                    showView(5);
                    break;
                case R.id.miHint:
                    showHint();
                    break;
                case R.id.miFieldNotes:
                    showView(9);
                    openOptionsMenu();
                    break;
                case R.id.miTelJoker:
                    showJoker();
                    break;
                case R.id.miCompassView:
                    showView(8);
                    break;
                case R.id.miMapView:
                    showView(0);
                    break;
                case R.id.miDescription:
                    showView(4);
                    break;
                case R.id.miWaypoints:
                    showView(2);
                    break;
                case R.id.miNotes:
                    showView(6);
                    break;
                case R.id.miSolver:
                    showView(7);
                    break;
                case R.id.miCacheList:
                    showView(1);
                    break;
                case R.id.miTrackList:
                    showView(13);
                    break;
                case R.id.miFilterset:
                    showView(101);
                    break;
                case R.id.miManageDB:
                    showManageDB();
                    break;
                case R.id.miResort:
                    Database.Data.Query.Resort();
                    break;
                case R.id.miAutoResort:
                    switchAutoResort();
                    break;
                case R.id.miSearch:
                    ListSearch();
                    break;
                case R.id.miAddCache:
                    addCache();
                    break;
                case R.id.searchcaches_online:
                    searchOnline();
                    break;
                case R.id.miTbList:
                    showTbList();
                    break;
                case R.id.miTakePhoto:
                    takePhoto();
                    break;
                case R.id.miTrackRec:
                    AllContextMenuCallHandler.showTrackContextMenu();
                    break;
                case R.id.miTrackStart:
                    TrackRecorder.StartRecording();
                    break;
                case R.id.miTrackStop:
                    TrackRecorder.StopRecording();
                    break;
                case R.id.miTrackPause:
                    TrackRecorder.PauseRecording();
                    break;
                case R.id.menu_tracklistview_generate:
                    AllContextMenuCallHandler.showTrackListView_generateContextMenu();
                    break;
                case R.id.miNavigateTo:
                    NavigateTo();
                    break;
                default:
                    if (aktView != null) aktView.ItemSelected(item);
            }
        }
    };

    OnItemClickListener QuickButtonOnItemClickListner = new OnItemClickListener() {

        public void onItemClick(AdapterView<?> arg0, View arg1, int arg2, long arg3) {
            vibrator.vibrate(50);
            QuickButtonItem clicedItem = Global.QuickButtonList.get(arg2);
            switch(clicedItem.getActionId()) {
                case 0:
                    showView(4);
                    break;
                case 1:
                    showView(2);
                    break;
                case 2:
                    showView(3);
                    break;
                case 3:
                    showView(0);
                    break;
                case 4:
                    showView(8);
                    break;
                case 5:
                    showView(1);
                    break;
                case 6:
                    showView(13);
                    break;
                case 7:
                    takePhoto();
                    break;
                case 8:
                    recVideo();
                    break;
                case 9:
                    recVoice();
                    break;
                case 10:
                    MessageBox.Show("SearchAPI muss noch in eine eigene Methode refactoriert werden, damit die Suche auch von hier aus ausgel�st werden kann!");
                    break;
                case 11:
                    showView(101);
                    break;
                case 12:
                    startScreenLock(true);
                    break;
                case 13:
                    switchAutoResort();
                    QuickButtonList.invalidate();
                    break;
                case 14:
                    showView(7);
                    break;
                case 15:
                    if (GlobalCore.SelectedCache() != null && GlobalCore.SelectedCache().SpoilerExists()) showView(5);
                    break;
            }
        }
    };

    private void findViewsById() {
        TopLayout = (LinearLayout) this.findViewById(R.id.layoutTop);
        frame = (FrameLayout) this.findViewById(R.id.layoutContent);
        InfoDownSlider = (downSlider) this.findViewById(R.id.downSlider);
        debugInfoPanel = (DebugInfoPanel) this.findViewById(R.id.debugInfo);
        Mic_Icon = (Mic_On_Flash) this.findViewById(R.id.mic_flash);
        buttonDB = (ImageButton) this.findViewById(R.id.buttonDB);
        buttonCache = (ImageButton) this.findViewById(R.id.buttonCache);
        buttonNav = (ImageButton) this.findViewById(R.id.buttonMap);
        buttonTools = (ImageButton) this.findViewById(R.id.buttonInfo);
        buttonMisc = (ImageButton) this.findViewById(R.id.buttonMisc);
        cacheNameView = (CacheNameView) this.findViewById(R.id.main_cache_name_view);
        QuickButtonList = (HorizontalListView) this.findViewById(R.id.main_quick_button_list);
        strengthLayout = (LinearLayout) this.findViewById(R.id.main_strength_control);
        searchLayout = (LinearLayout) this.findViewById(R.id.searchDialog);
    }

    private void initialViews() {
        if (compassView == null) compassView = new CompassView(this, inflater);
        if (cacheListView == null) cacheListView = new CacheListView(this);
        if (waypointView == null) waypointView = new WaypointView(this, this);
        if (logView == null) logView = new LogView(this);
        if (fieldNotesView == null) fieldNotesView = new FieldNotesView(this, this);
        registerForContextMenu(fieldNotesView);
        descriptionView = new DescriptionView(this, inflater);
        if (spoilerView == null) spoilerView = new SpoilerView(this, inflater);
        notesView = new NotesView(this, inflater);
        solverView = new SolverView(this, inflater);
        if (TestEmpty == null) TestEmpty = new EmptyViewTemplate(this, inflater);
        if (aboutView == null) aboutView = new AboutView(this, inflater);
        if (jokerView == null) jokerView = new JokerView(this, this);
        if (tracklistView == null) tracklistView = new TrackListView(this, this);
        ViewList.add(mapView);
        ViewList.add(cacheListView);
        ViewList.add(waypointView);
        ViewList.add(logView);
        ViewList.add(descriptionView);
        ViewList.add(spoilerView);
        ViewList.add(notesView);
        ViewList.add(solverView);
        ViewList.add(compassView);
        ViewList.add(fieldNotesView);
        ViewList.add(TestEmpty);
        ViewList.add(aboutView);
        ViewList.add(jokerView);
        ViewList.add(tracklistView);
    }

    private void initialLocationManager() {
        try {
            if (locationManager != null) {
                return;
            }
            locationManager = (LocationManager) getSystemService(Context.LOCATION_SERVICE);
            Criteria criteria = new Criteria();
            criteria.setAccuracy(Criteria.ACCURACY_FINE);
            criteria.setAltitudeRequired(false);
            criteria.setBearingRequired(false);
            criteria.setCostAllowed(true);
            criteria.setPowerRequirement(Criteria.POWER_LOW);
            Global.Locator = new Locator();
            locationManager.requestLocationUpdates(LocationManager.GPS_PROVIDER, 100, 1, this);
            locationManager.requestLocationUpdates(LocationManager.NETWORK_PROVIDER, 10000, 500, this);
            locationManager.addNmeaListener(this);
            locationManager.addGpsStatusListener(new GPS.GpsStatusListener(locationManager));
        } catch (Exception e) {
            Logger.Error("main.initialLocationManager()", "", e);
            e.printStackTrace();
        }
    }

    private void initialMapView() {
        try {
            if (mapView == null) {
                mapView = new MapView(this, inflater);
                mapView.Initialize();
                mapView.CurrentLayer = MapView.Manager.GetLayerByName(Config.GetString("CurrentMapLayer"), Config.GetString("CurrentMapLayer"), "");
                Global.TrackDistance = Config.GetInt("TrackDistance");
                mapView.InitializeMap();
            }
        } catch (Exception e) {
            Logger.Error("main.initialMapView()", "", e);
            e.printStackTrace();
        }
    }

    private void initalMicIcon() {
        Mic_Icon.SetOff();
        Mic_Icon.setOnClickListener(new OnClickListener() {

            @Override
            public void onClick(View v) {
                setVoiceRecIsStart(false);
            }
        });
    }

    private void initialButtons() {
        registerForContextMenu(buttonDB);
        buttonDB.setOnClickListener(ButtonOnClick);
        registerForContextMenu(buttonCache);
        buttonCache.setOnClickListener(ButtonOnClick);
        registerForContextMenu(buttonNav);
        this.buttonNav.setOnClickListener(ButtonOnClick);
        registerForContextMenu(buttonTools);
        this.buttonTools.setOnClickListener(ButtonOnClick);
        registerForContextMenu(buttonMisc);
        this.buttonMisc.setOnClickListener(ButtonOnClick);
        btnDBActionIds = new ArrayList<Integer>();
        btnDBActionIds.add(1);
        btnCacheActionIds = new ArrayList<Integer>();
        btnCacheActionIds.add(4);
        btnCacheActionIds.add(2);
        btnCacheActionIds.add(6);
        btnCacheActionIds.add(7);
        btnNavActionIds = new ArrayList<Integer>();
        btnNavActionIds.add(0);
        btnNavActionIds.add(8);
        btnToolsActionIds = new ArrayList<Integer>();
        btnMiscActionIds = new ArrayList<Integer>();
        btnMiscActionIds.add(11);
    }

    private void initialCaheInfoSlider() {
        QuickButtonList.setHeight(Sizes.getQuickButtonListHeight());
        QuickButtonList.setAdapter(QuickButtonsAdapter);
        QuickButtonList.setOnItemClickListener(QuickButtonOnItemClickListner);
        String ConfigActionList = Config.GetString("quickButtonList");
        String[] ConfigList = ConfigActionList.split(",");
        Global.QuickButtonList = Actions.getListFromConfig(ConfigList);
        cacheNameView.setHeight(Sizes.getInfoSliderHeight());
    }

    private void takePhoto() {
        Log.d("DroidCachebox", "Starting camera on the phone...");
        String directory = Config.GetString("UserImageFolder");
        if (!FileIO.DirectoryExists(directory)) {
            Log.d("DroidCachebox", "Media-Folder does not exist...");
            return;
        }
        basename = Global.GetDateTimeString();
        if (GlobalCore.SelectedCache() != null) {
            String validName = FileIO.RemoveInvalidFatChars(GlobalCore.SelectedCache().GcCode + "-" + GlobalCore.SelectedCache().Name);
            mediaCacheName = validName.substring(0, (validName.length() > 32) ? 32 : validName.length());
        } else {
            mediaCacheName = "Image";
        }
        basename += " " + mediaCacheName;
        mediafile = new File(directory + "/" + basename + ".jpg");
        final Intent intent = new Intent(MediaStore.ACTION_IMAGE_CAPTURE);
        intent.putExtra(MediaStore.EXTRA_OUTPUT, Uri.fromFile(mediafile));
        intent.putExtra(MediaStore.EXTRA_VIDEO_QUALITY, 1);
        startActivityForResult(intent, CAPTURE_IMAGE_ACTIVITY_REQUEST_CODE);
    }

    private void recVideo() {
        Log.d("DroidCachebox", "Starting video on the phone...");
        String directory = Config.GetString("UserImageFolder");
        if (!FileIO.DirectoryExists(directory)) {
            Log.d("DroidCachebox", "Media-Folder does not exist...");
            return;
        }
        basename = Global.GetDateTimeString();
        if (GlobalCore.SelectedCache() != null) {
            String validName = FileIO.RemoveInvalidFatChars(GlobalCore.SelectedCache().GcCode + "-" + GlobalCore.SelectedCache().Name);
            mediaCacheName = validName.substring(0, (validName.length() > 32) ? 32 : validName.length());
        } else {
            mediaCacheName = "Video";
        }
        basename += " " + mediaCacheName;
        mediafile = new File(directory + "/" + basename + ".3gp");
        mediaTimeString = Global.GetTrackDateTimeString();
        mediaCoordinate = GlobalCore.LastValidPosition;
        ContentValues values = new ContentValues();
        values.put(MediaStore.Video.Media.TITLE, "captureTemp.mp4");
        cameraVideoURI = getContentResolver().insert(MediaStore.Video.Media.EXTERNAL_CONTENT_URI, values);
        final Intent videointent = new Intent(MediaStore.ACTION_VIDEO_CAPTURE);
        videointent.putExtra(MediaStore.EXTRA_OUTPUT, Uri.fromFile(mediafile));
        videointent.putExtra(MediaStore.EXTRA_VIDEO_QUALITY, 0);
        startActivityForResult(videointent, CAPTURE_VIDEO_ACTIVITY_REQUEST_CODE);
    }

    private void recVoice() {
        if (!getVoiceRecIsStart()) {
            Log.d("DroidCachebox", "Starting voice recorder on the phone...");
            String directory = Config.GetString("UserImageFolder");
            if (!FileIO.DirectoryExists(directory)) {
                Log.d("DroidCachebox", "Media-Folder does not exist...");
                return;
            }
            basename = Global.GetDateTimeString();
            if (GlobalCore.SelectedCache() != null) {
                String validName = FileIO.RemoveInvalidFatChars(GlobalCore.SelectedCache().GcCode + "-" + GlobalCore.SelectedCache().Name);
                mediaCacheName = validName.substring(0, (validName.length() > 32) ? 32 : validName.length());
            } else {
                mediaCacheName = "Voice";
            }
            basename += " " + mediaCacheName;
            mediafilename = (directory + "/" + basename + ".wav");
            extAudioRecorder = ExtAudioRecorder.getInstanse(false);
            extAudioRecorder.setOutputFile(mediafilename);
            extAudioRecorder.prepare();
            extAudioRecorder.start();
            String MediaFolder = Config.GetString("UserImageFolder");
            String TrackFolder = Config.GetString("TrackFolder");
            String relativPath = FileIO.getRelativePath(MediaFolder, TrackFolder, "/");
            TrackRecorder.AnnotateMedia(basename + ".wav", relativPath + "/" + basename + ".wav", GlobalCore.LastValidPosition, Global.GetTrackDateTimeString());
            Toast.makeText(mainActivity, "Start Voice Recorder", Toast.LENGTH_SHORT).show();
            setVoiceRecIsStart(true);
            counter.cancel();
            counterStopped = true;
            return;
        } else {
            Log.d("DroidCachebox", "Stoping voice recorder on the phone...");
            setVoiceRecIsStart(false);
            return;
        }
    }

    private void showHint() {
        if (GlobalCore.SelectedCache() == null) return;
        String hint = Database.Hint(GlobalCore.SelectedCache());
        if (hint.equals("")) return;
        final Intent hintIntent = new Intent().setClass(mainActivity, HintDialog.class);
        Bundle b = new Bundle();
        b.putSerializable("Hint", hint);
        hintIntent.putExtras(b);
        mainActivity.startActivity(hintIntent);
    }

    private void showJoker() {
        if (Global.Jokers.isEmpty()) {
            try {
                URL url = new URL("http://www.gcjoker.de/cachebox.php?md5=" + Config.GetString("GcJoker") + "&wpt=" + GlobalCore.SelectedCache().GcCode);
                URLConnection urlConnection = url.openConnection();
                HttpURLConnection httpConnection = (HttpURLConnection) urlConnection;
                if (httpConnection.getResponseCode() == HttpURLConnection.HTTP_OK) {
                    InputStream inputStream = httpConnection.getInputStream();
                    if (inputStream != null) {
                        String line;
                        try {
                            BufferedReader reader = new BufferedReader(new InputStreamReader(inputStream, "UTF-8"));
                            while ((line = reader.readLine()) != null) {
                                String[] s = line.split(";", 7);
                                try {
                                    if (s[0].equals("2")) {
                                        MessageBox.Show(s[1], null);
                                        break;
                                    }
                                    if (s[0].equals("1")) {
                                        MessageBox.Show(s[1], null);
                                    }
                                    if (s[0].equals("0")) {
                                        Global.Jokers.AddJoker(s[1], s[2], s[3], s[4], s[5], s[6]);
                                    }
                                } catch (Exception exc) {
                                    Logger.Error("main.initialBtnInfoContextMenu()", "HTTP response Jokers", exc);
                                    return;
                                }
                            }
                            if (Global.Jokers.isEmpty()) {
                                MessageBox.Show("Keine Joker bekannt", null);
                            } else {
                                Logger.General("Open JokerView...");
                                showView(12);
                            }
                        } finally {
                            inputStream.close();
                        }
                    }
                }
            } catch (MalformedURLException urlEx) {
                Logger.Error("main.initialBtnInfoContextMenu()", "MalformedURLException HTTP response Jokers", urlEx);
                Log.d("DroidCachebox", urlEx.getMessage());
            } catch (IOException ioEx) {
                Logger.Error("main.initialBtnInfoContextMenu()", "IOException HTTP response Jokers", ioEx);
                Log.d("DroidCachebox", ioEx.getMessage());
                MessageBox.Show("Fehler bei Internetzugriff", null);
            } catch (Exception ex) {
                Logger.Error("main.initialBtnInfoContextMenu()", "HTTP response Jokers", ex);
                Log.d("DroidCachebox", ex.getMessage());
            }
        }
    }

    private void showTbList() {
        MessageBox.Show("Die Trackable List ist noch nicht implementiert!", "Sorry", MessageBoxIcon.Asterisk);
    }

    private void switchDayNight() {
        frame.removeAllViews();
        Config.changeDayNight();
        DescriptionViewControl.mustLoadDescription = true;
        downSlider.isInitial = false;
        ActivityUtils.changeToTheme(mainActivity, Config.GetBool("nightMode") ? ActivityUtils.THEME_NIGHT : ActivityUtils.THEME_DAY);
        Toast.makeText(mainActivity, "changeDayNight", Toast.LENGTH_SHORT).show();
    }

    private void switchAutoResort() {
        Global.autoResort = !(Global.autoResort);
        Config.Set("AutoResort", Global.autoResort);
        if (Global.autoResort) {
            Database.Data.Query.Resort();
        }
    }

    private void showManageDB() {
        SelectDB.autoStart = false;
        Intent selectDBIntent = new Intent().setClass(mainActivity, SelectDB.class);
        mainActivity.startActivityForResult(selectDBIntent, 546132);
    }

    private void ListSearch() {
        Search.Show();
    }

    private void NavigateTo() {
        if (GlobalCore.SelectedCache() != null) {
            double lat = GlobalCore.SelectedCache().Latitude();
            double lon = GlobalCore.SelectedCache().Longitude();
            if (GlobalCore.SelectedWaypoint() != null) {
                lat = GlobalCore.SelectedWaypoint().Latitude();
                lon = GlobalCore.SelectedWaypoint().Longitude();
            }
            Intent implicitIntent = new Intent(Intent.ACTION_VIEW, Uri.parse("google.navigation:q=" + lat + "," + lon));
            startActivity(implicitIntent);
        }
    }

    private void addCache() {
        int status = CB_Core.Api.GroundspeakAPI.GetCacheLimits(Config.GetString("GcAPI"));
        if (status != 0) MessageBox.Show(CB_Core.Api.GroundspeakAPI.LastAPIError);
        MessageBox.Show("Cache hinzuf�gen ist noch nicht implementiert!", "Sorry", MessageBoxIcon.Asterisk);
    }

    public void GetApiAuth() {
        Intent gcApiLogin = new Intent().setClass(mainActivity, GcApiLogin.class);
        mainActivity.startActivityForResult(gcApiLogin, 987654321);
    }

    private static ProgressDialog pd;

    private String result;

    public void searchOnline() {
        premiumMember = (GroundspeakAPI.GetMembershipType(Config.GetString("GcAPI")) == 3);
        if (premiumMember) {
            searchOnlineNow();
        } else {
            MessageBox.Show(Global.Translations.Get("GC_basic"), Global.Translations.Get("GC_title"), MessageBoxButtons.OKCancel, MessageBoxIcon.Powerd_by_GC_Live, PremiumMemberResult);
        }
    }

    private DialogInterface.OnClickListener PremiumMemberResult = new DialogInterface.OnClickListener() {

        @Override
        public void onClick(DialogInterface dialog, int button) {
            switch(button) {
                case -1:
                    searchOnlineNow();
                    break;
            }
            dialog.dismiss();
        }
    };

    private void searchOnlineNow() {
        Thread thread = new Thread() {

            @Override
            public void run() {
                String accessToken = Config.GetString("GcAPI");
                Coordinate searchCoord = null;
                if (mapView.isShown()) {
                    PointD point = new PointD(0, 0);
                    point.X = mapView.screenCenter.X;
                    point.Y = mapView.screenCenter.Y;
                    mapView.lastMouseCoordinate = new Coordinate(Descriptor.TileYToLatitude(mapView.Zoom, point.Y / (256.0)), Descriptor.TileXToLongitude(mapView.Zoom, point.X / (256.0)));
                    searchCoord = mapView.lastMouseCoordinate;
                } else {
                    searchCoord = GlobalCore.LastValidPosition;
                }
                if (searchCoord == null) return;
                CategoryDAO categoryDAO = new CategoryDAO();
                Category category = categoryDAO.GetCategory(GlobalCore.Categories, "API-Import");
                if (category == null) return;
                GpxFilename gpxFilename = categoryDAO.CreateNewGpxFilename(category, "API-Import");
                if (gpxFilename == null) return;
                ArrayList<Cache> apiCaches = new ArrayList<Cache>();
                ArrayList<LogEntry> apiLogs = new ArrayList<LogEntry>();
                CB_Core.Api.SearchForGeocaches.SearchCoordinate searchC = new CB_Core.Api.SearchForGeocaches.SearchCoordinate();
                searchC.pos = searchCoord;
                searchC.distanceInMeters = 50000;
                searchC.number = 30;
                result = CB_Core.Api.SearchForGeocaches.SearchForGeocachesJSON(accessToken, searchC, apiCaches, apiLogs, gpxFilename.Id);
                if (apiCaches.size() > 0) {
                    Database.Data.myDB.beginTransaction();
                    for (Cache cache : apiCaches) {
                        cache.MapX = 256.0 * Descriptor.LongitudeToTileX(Cache.MapZoomLevel, cache.Longitude());
                        cache.MapY = 256.0 * Descriptor.LatitudeToTileY(Cache.MapZoomLevel, cache.Latitude());
                        if (Database.Data.Query.GetCacheById(cache.Id) == null) {
                            Database.Data.Query.add(cache);
                            CacheDAO cacheDAO = new CacheDAO();
                            cacheDAO.WriteToDatabase(cache);
                            for (LogEntry log : apiLogs) {
                                if (log.CacheId != cache.Id) continue;
                                LogDAO logDAO = new LogDAO();
                                logDAO.WriteToDatabase(log);
                            }
                            for (Waypoint waypoint : cache.waypoints) {
                                WaypointDAO waypointDAO = new WaypointDAO();
                                waypointDAO.WriteToDatabase(waypoint);
                            }
                        }
                    }
                    Database.Data.myDB.setTransactionSuccessful();
                    Database.Data.myDB.endTransaction();
                    Database.Data.GPXFilenameUpdateCacheCount();
                    if (mapView.isShown()) {
                        mapView.updateCacheList();
                        mapView.Render(true);
                    }
                }
                onlineSearchReadyHandler.sendMessage(onlineSearchReadyHandler.obtainMessage(1));
            }
        };
        pd = ProgressDialog.show(this, "", "Search Online", true);
        thread.start();
        return;
    }

    private boolean premiumMember = false;

    private Handler onlineSearchReadyHandler = new Handler() {

        public void handleMessage(Message msg) {
            switch(msg.what) {
                case 1:
                    {
                        pd.dismiss();
                        cacheListView.notifyCacheListChange();
                    }
            }
        }
    };

    public void setDebugVisible() {
        if (Config.GetBool("DebugShowPanel")) {
            debugInfoPanel.setVisibility(View.VISIBLE);
            debugInfoPanel.onShow();
        } else {
            debugInfoPanel.setVisibility(View.GONE);
            debugInfoPanel.onShow();
        }
    }

    String debugMsg = "";

    public void setDebugMsg(String msg) {
        debugMsg = msg;
        Thread t = new Thread() {

            public void run() {
                runOnUiThread(new Runnable() {

                    @Override
                    public void run() {
                        debugInfoPanel.setMsg(debugMsg);
                    }
                });
            }
        };
        t.start();
    }

    public void setVoiceRecIsStart(Boolean value) {
        mVoiceRecIsStart = value;
        if (mVoiceRecIsStart) {
            Mic_Icon.SetOn();
        } else {
            Mic_Icon.SetOff();
            if (extAudioRecorder != null) {
                extAudioRecorder.stop();
                extAudioRecorder.release();
                extAudioRecorder = null;
                Toast.makeText(mainActivity, "Stop Voice Recorder", Toast.LENGTH_SHORT).show();
            }
            if (runsWithAkku) {
                counterStopped = false;
                counter.start();
            }
        }
    }

    @Override
    public void onNmeaReceived(long timestamp, String nmea) {
        try {
            if (nmea.substring(0, 6).equalsIgnoreCase("$GPGGA")) {
                String[] s = nmea.split(",");
                try {
                    if (s[11].equals("")) return;
                    double altCorrection = Double.valueOf(s[11]);
                    Logger.General("AltCorrection: " + String.valueOf(altCorrection));
                    Global.Locator.altCorrection = altCorrection;
                    locationManager.removeNmeaListener(this);
                } catch (Exception exc) {
                }
            }
        } catch (Exception e) {
            Logger.Error("main.onNmeaReceived()", "", e);
            e.printStackTrace();
        }
    }

    public void setScreenLockTimerNew(int value) {
        counter.cancel();
        counter = new ScreenLockTimer(value, value);
        if (runsWithAkku) counter.start();
    }

    static class LockClass {
    }

    ;

    static LockClass lockObject = new LockClass();

    /**
	 * Empf�ngt die gelogten Meldungen und schreibt sie in die Debug.txt
	 */
    @Override
    public void receiveLog(String Msg) {
        synchronized (lockObject) {
            File file = new File(Config.WorkPath + "/debug.txt");
            FileWriter writer;
            try {
                writer = new FileWriter(file, true);
                writer.write(Msg);
                writer.close();
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
    }

    /**
	 * Empf�ngt die gelogten Meldungen in kurz Form und schreibt sie ins Debung
	 * Panel, wenn dieses sichtbar ist!
	 */
    @Override
    public void receiveShortLog(String Msg) {
        debugMsg = Msg;
        Thread t = new Thread() {

            public void run() {
                runOnUiThread(new Runnable() {

                    @Override
                    public void run() {
                        debugInfoPanel.addLogMsg(debugMsg);
                    }
                });
            }
        };
        t.start();
    }

    private BroadcastReceiver mBatInfoReceiver = new BroadcastReceiver() {

        @Override
        public void onReceive(Context arg0, Intent intent) {
            try {
                int plugged = intent.getIntExtra("plugged", -1);
                if (runsWithAkku != (plugged == 0)) {
                    runsWithAkku = plugged == 0;
                    if (!runsWithAkku) {
                        counter.cancel();
                        counterStopped = true;
                    } else {
                        counter.start();
                        counterStopped = false;
                    }
                }
            } catch (Exception e) {
                Logger.Error("main.mBatInfoReceiver.onReceive()", "", e);
                e.printStackTrace();
            }
        }
    };

    int horizontalListViewHeigt;

    public void setQuickButtonHeight(int value) {
        horizontalListViewHeigt = value;
        Thread t = new Thread() {

            public void run() {
                runOnUiThread(new Runnable() {

                    @Override
                    public void run() {
                        QuickButtonList.setHeight(horizontalListViewHeigt);
                        QuickButtonList.invalidate();
                        TopLayout.requestLayout();
                        frame.requestLayout();
                    }
                });
            }
        };
        t.start();
    }

    /**
	 * Adapter f�r die QuickButton Lists.
	 * 
	 * @author Longri
	 */
    public BaseAdapter QuickButtonsAdapter = new BaseAdapter() {

        @Override
        public int getCount() {
            return Global.QuickButtonList.size();
        }

        @Override
        public Object getItem(int position) {
            return null;
        }

        @Override
        public long getItemId(int position) {
            return 0;
        }

        @Override
        public View getView(int position, View convertView, ViewGroup parent) {
            return Global.QuickButtonList.get(position);
        }
    };

    public static int getQuickButtonHeight() {
        return ((main) mainActivity).QuickButtonList.getHeight();
    }

    /**
	 * �berpr�ft ob das GPS eingeschaltet ist. Wenn nicht, wird eine Meldung
	 * ausgegeben.
	 */
    private void chkGpsIsOn() {
        try {
            LocationManager locManager = (LocationManager) getSystemService(LOCATION_SERVICE);
            if (!locManager.isProviderEnabled(LocationManager.GPS_PROVIDER)) {
                Thread t = new Thread() {

                    public void run() {
                        runOnUiThread(new Runnable() {

                            @Override
                            public void run() {
                                MessageBox.Show(Global.Translations.Get("GPSon?"), Global.Translations.Get("GPSoff"), MessageBoxButtons.YesNo, MessageBoxIcon.Question, new DialogInterface.OnClickListener() {

                                    @Override
                                    public void onClick(DialogInterface dialog, int button) {
                                        switch(button) {
                                            case -1:
                                                Intent gpsOptionsIntent = new Intent(android.provider.Settings.ACTION_LOCATION_SOURCE_SETTINGS);
                                                startActivity(gpsOptionsIntent);
                                                break;
                                            case -2:
                                                break;
                                            case -3:
                                                break;
                                        }
                                        dialog.dismiss();
                                    }
                                });
                            }
                        });
                    }
                };
                t.start();
            }
        } catch (Exception e) {
            Logger.Error("main.chkGpsIsOn()", "", e);
            e.printStackTrace();
        }
    }

    @Override
    public void GpsStateChanged() {
        try {
            setSatStrength();
        } catch (Exception e) {
            Logger.Error("main.GpsStateChanged()", "setSatStrength()", e);
            e.printStackTrace();
        }
    }

    private static View[] balken = null;

    public static void setSatStrength() {
        try {
            de.droidcachebox.Locator.GPS.setSatStrength(strengthLayout, balken);
        } catch (Exception e) {
            Logger.Error("main.setSatStrength()", "de.droidcachebox.Locator.GPS.setSatStrength()", e);
            e.printStackTrace();
        }
    }

    private void askToGetApiKey() {
        MessageBox.Show(Global.Translations.Get("wantApi"), Global.Translations.Get("welcome"), MessageBoxButtons.YesNo, MessageBoxIcon.GC_Live, new DialogInterface.OnClickListener() {

            @Override
            public void onClick(DialogInterface dialog, int button) {
                switch(button) {
                    case -1:
                        GetApiAuth();
                        break;
                    case -2:
                        chkGpsIsOn();
                        break;
                    case -3:
                        break;
                }
                dialog.dismiss();
            }
        });
    }
}
