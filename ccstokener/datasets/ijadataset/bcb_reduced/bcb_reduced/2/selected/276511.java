package com.angis.fx.activity.map;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.net.URI;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import org.apache.http.HttpEntity;
import org.apache.http.HttpResponse;
import org.apache.http.client.ClientProtocolException;
import org.apache.http.client.HttpClient;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.entity.StringEntity;
import org.apache.http.impl.client.DefaultHttpClient;
import org.json.JSONArray;
import org.json.JSONObject;
import android.app.AlertDialog;
import android.app.ProgressDialog;
import android.app.AlertDialog.Builder;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.DialogInterface.OnClickListener;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Canvas;
import android.graphics.Point;
import android.location.LocationManager;
import android.os.Bundle;
import android.telephony.TelephonyManager;
import android.telephony.gsm.GsmCellLocation;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.MotionEvent;
import android.view.View;
import android.view.View.OnTouchListener;
import android.widget.AdapterView;
import android.widget.CheckedTextView;
import android.widget.ListView;
import android.widget.Toast;
import android.widget.AdapterView.OnItemClickListener;
import com.angis.fx.activity.Enforcement;
import com.angis.fx.activity.R;
import com.angis.fx.activity.jcdj.DailyCheckResultActivity;
import com.angis.fx.activity.service.GPSService;
import com.angis.fx.activity.service.GSMService;
import com.angis.fx.adapter.ImageMultipleAdapter;
import com.angis.fx.adapter.RadioAdapter;
import com.angis.fx.data.ChangsuoInformation;
import com.angis.fx.data.ChangsuoType;
import com.angis.fx.util.DataParseUtil;
import com.angis.fx.util.GeoLatLng;
import com.angis.fx.util.OffsetInChina;
import com.google.android.maps.GeoPoint;
import com.google.android.maps.MapView;
import com.google.android.maps.Overlay;
import com.google.android.maps.Projection;

/**
 * @author app2
 * 
 */
public class SearchInMapActivity extends com.google.android.maps.MapActivity {

    private Double l_lon = 120.14181;

    private Double l_lat = 30.27056;

    private boolean isGPSOn;

    private boolean isTrackOn;

    private MapView mMapView;

    private boolean isGPSNew;

    private boolean isGPSSearch;

    private List<Overlay> mMapOverlays;

    private Overlay mGPSOverlay;

    private ChangsuoInformation mCsInfo;

    private UpdateReceiver receiver;

    private GSMUpdateReceiver gsmReceiver;

    private TelephonyManager tm;

    private ImageMultipleAdapter mMultipleDialogListAdapter;

    private ListView mListView;

    private LayoutInflater mLayoutInflater;

    private View mView;

    private AlertDialog mDialogBuilder;

    private CheckedTextView mCheckTextView;

    private List<String> mCsNameList;

    private List<CheckedTextView> mChooseCtv;

    private CheckedTextView mRadioTextView;

    private int mSelectedPosition = -1;

    private List<ChangsuoInformation> mCSList;

    private ProgressDialog mProgressDialog;

    /** Called when the activity is first created. */
    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.map);
        mChooseCtv = new ArrayList<CheckedTextView>();
        tm = (TelephonyManager) this.getSystemService(Context.TELEPHONY_SERVICE);
        mMapView = (MapView) findViewById(R.id.myMapView1);
        mMapView.setBuiltInZoomControls(true);
        mMapOverlays = mMapView.getOverlays();
        GeoPoint point;
        if (getIntent().getExtras() != null && getIntent().getExtras().get("selectedcs") != null) {
            mMapView.getController().setZoom(18);
            mCsInfo = (ChangsuoInformation) getIntent().getExtras().get("selectedcs");
            if (mCsInfo.getXlonGPS() != 0 && mCsInfo.getYlatGPS() != 0) {
                GeoLatLng lResult = OffsetInChina.fromEarthToMars(new GeoLatLng(mCsInfo.getYlatGPS(), mCsInfo.getXlonGPS()));
                point = new GeoPoint((int) (lResult.latitude * 1000000), (int) (lResult.longitude * 1000000));
                mMapView.getController().animateTo(point);
                mMapOverlays.clear();
                doAddOverLay(point, mCsInfo);
            } else if (mCsInfo.getLon() != 0 && mCsInfo.getLat() != 0) {
                GeoLatLng lResult = OffsetInChina.fromEarthToMars(new GeoLatLng(mCsInfo.getLat(), mCsInfo.getLon()));
                point = new GeoPoint((int) (lResult.latitude * 1000000), (int) (lResult.longitude * 1000000));
                mMapView.getController().animateTo(point);
                mMapOverlays.clear();
                doAddOverLay(point, mCsInfo);
                double[] lCoord;
                try {
                    lCoord = getNetworkCoord();
                } catch (Exception e) {
                    lCoord = new double[] { 0, 0 };
                }
                if (lCoord[0] != 0 && lCoord[1] != 0) {
                    GeoLatLng lStandPosition = OffsetInChina.fromEarthToMars(new GeoLatLng(lCoord[1], lCoord[0]));
                    GeoPoint lStandPoint = new GeoPoint((int) (lStandPosition.latitude * 1000000), (int) (lStandPosition.longitude * 1000000));
                    mGPSOverlay = new NewPositionOverLay(lStandPoint, SearchInMapActivity.this, R.drawable.blue_dot_circle);
                    mMapOverlays.add(0, mGPSOverlay);
                    mMapView.getController().zoomToSpan(java.lang.Math.abs((lStandPoint.getLatitudeE6() - point.getLatitudeE6())) * 2, java.lang.Math.abs((lStandPoint.getLongitudeE6() - point.getLongitudeE6())) * 2);
                }
            }
        } else {
            mMapView.getController().setZoom(9);
            openGPS();
            GeoLatLng lResult = OffsetInChina.fromEarthToMars(new GeoLatLng(30.000000, 120.000000));
            ;
            try {
                Toast.makeText(SearchInMapActivity.this, "正在基站定位，请稍后", Toast.LENGTH_SHORT).show();
                openGSM();
            } catch (Exception e) {
                Toast.makeText(SearchInMapActivity.this, "基站定位出错", Toast.LENGTH_SHORT).show();
            }
            point = new GeoPoint((int) (lResult.latitude * 1000000), (int) (lResult.longitude * 1000000));
            mMapView.getController().animateTo(point);
        }
    }

    public void openGSM() {
        gsmReceiver = new GSMUpdateReceiver();
        IntentFilter filter = new IntentFilter();
        filter.addAction("sendgsm");
        this.registerReceiver(gsmReceiver, filter);
        Intent intent = new Intent(SearchInMapActivity.this, GSMService.class);
        startService(intent);
    }

    public class GSMUpdateReceiver extends BroadcastReceiver {

        @Override
        public void onReceive(Context context, Intent intent) {
            GeoLatLng strGpsValue = (GeoLatLng) intent.getExtras().get("geolatlon");
            GeoLatLng lResult = OffsetInChina.fromEarthToMars(strGpsValue);
            GeoPoint point = new GeoPoint((int) (lResult.latitude * 1000000), (int) (lResult.longitude * 1000000));
            l_lat = strGpsValue.latitude;
            l_lon = strGpsValue.longitude;
            closeGSM();
            if (isGPSSearch) {
                isGPSSearch = false;
                mMapOverlays.clear();
                if (mMapOverlays != null && mMapOverlays.size() > 0 && mMapOverlays.get(0) instanceof NewPositionOverLay) mMapOverlays.remove(0);
                mGPSOverlay = new NewPositionOverLay(point, SearchInMapActivity.this, R.drawable.blue_dot_circle);
                mMapOverlays.add(0, mGPSOverlay);
                mMapView.getController().animateTo(point);
                double minx = l_lon - Enforcement.GPSExtent * 0.0065 / 500;
                double maxx = l_lon + Enforcement.GPSExtent * 0.0065 / 500;
                double miny = l_lat - Enforcement.GPSExtent * 0.0065 / 500;
                double maxy = l_lat + Enforcement.GPSExtent * 0.0065 / 500;
                String lUrl = retrieveURL(String.valueOf(minx), String.valueOf(miny), String.valueOf(maxx), String.valueOf(maxy));
                String lResp = searchResults(lUrl);
                if (lResp.indexOf("&") != -1 && lResp.indexOf("=") != -1 && lResp.indexOf("^") != -1) {
                    mCSList = DataParseUtil.parseSimpleChangsuoInfo(DataParseUtil.handleResponse(lResp), l_lon, l_lat);
                    mCsNameList = new ArrayList<String>();
                    for (ChangsuoInformation lCsInfo : mCSList) {
                        mCsNameList.add(lCsInfo.getTitle());
                    }
                    LayoutInflater layoutInflater = LayoutInflater.from(SearchInMapActivity.this);
                    View lRadioView = layoutInflater.inflate(R.layout.radio_dialog, null);
                    RadioAdapter lRegisterDialogListAdapter = new RadioAdapter(SearchInMapActivity.this);
                    ListView lListView = (ListView) lRadioView.findViewById(R.id.radio_dialoglist);
                    lRegisterDialogListAdapter.setData(mCsNameList);
                    lListView.setAdapter(lRegisterDialogListAdapter);
                    lListView.setOnItemClickListener(new OnItemClickListener() {

                        @Override
                        public void onItemClick(AdapterView<?> parent, View view, int position, long id) {
                            mRadioTextView = (CheckedTextView) view.findViewById(R.id.radio_dialog_item_ctv);
                            if (mRadioTextView.isChecked()) {
                                mRadioTextView.toggle();
                                mSelectedPosition = -1;
                            } else if (!mRadioTextView.isChecked()) {
                                for (int i = 0; i < mChooseCtv.size(); i++) {
                                    CheckedTextView mRadio = (CheckedTextView) mChooseCtv.get(i);
                                    mRadio.setChecked(false);
                                }
                                mChooseCtv.add(mRadioTextView);
                                mRadioTextView.setChecked(true);
                                mSelectedPosition = position;
                            }
                        }
                    });
                    new AlertDialog.Builder(SearchInMapActivity.this).setTitle("确定").setView(lRadioView).setPositiveButton(R.string.confirm, new DialogInterface.OnClickListener() {

                        public void onClick(DialogInterface dialog, int whichButton) {
                            if (mSelectedPosition != -1) {
                                ChangsuoInformation lCsInfo = mCSList.get(mSelectedPosition);
                                ChangsuoInformation lYwInfo = getChangsuoInfoById(lCsInfo.getCsId());
                                if (lYwInfo != null) {
                                    Intent lIntent = new Intent();
                                    lIntent.putExtra("selectedcs", lYwInfo);
                                    lIntent.setClass(SearchInMapActivity.this, DailyCheckResultActivity.class);
                                    Enforcement.mCurrentChangsuo = lCsInfo;
                                    SearchInMapActivity.this.startActivity(lIntent);
                                } else {
                                    Toast.makeText(SearchInMapActivity.this, "业务库中不存在该场所的相关信息", Toast.LENGTH_LONG).show();
                                }
                            } else {
                                Toast.makeText(SearchInMapActivity.this, "请选择要检查的场所", Toast.LENGTH_LONG).show();
                            }
                        }
                    }).setNegativeButton(R.string.cancel, new DialogInterface.OnClickListener() {

                        public void onClick(DialogInterface dialog, int whichButton) {
                        }
                    }).create().show();
                } else {
                    Toast.makeText(SearchInMapActivity.this, "当前地图范围内没有查询结果", Toast.LENGTH_LONG).show();
                }
                mProgressDialog.dismiss();
            } else {
                mMapView.getController().setZoom(18);
                mMapView.getController().animateTo(point);
                if (mMapOverlays != null && mMapOverlays.size() > 0 && mMapOverlays.get(0) instanceof NewPositionOverLay) mMapOverlays.remove(0);
                mGPSOverlay = new NewPositionOverLay(point, SearchInMapActivity.this, R.drawable.blue_dot_circle);
                mMapOverlays.add(0, mGPSOverlay);
                mMapView.invalidate();
            }
        }
    }

    @Override
    protected boolean isRouteDisplayed() {
        return false;
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        MenuInflater inflater = getMenuInflater();
        inflater.inflate(R.layout.mapsearchmenu, menu);
        return true;
    }

    private double[] getNetworkCoord() throws Exception {
        GsmCellLocation gcl = (GsmCellLocation) tm.getCellLocation();
        int cid = gcl.getCid();
        int lac = gcl.getLac();
        int mcc = Integer.valueOf(tm.getNetworkOperator().substring(0, 3));
        int mnc = Integer.valueOf(tm.getNetworkOperator().substring(3, 5));
        JSONObject holder = new JSONObject();
        holder.put("version", "1.1.0");
        holder.put("host", "maps.google.com");
        holder.put("request_address", true);
        JSONArray array = new JSONArray();
        JSONObject data = new JSONObject();
        data.put("cell_id", cid);
        data.put("location_area_code", lac);
        data.put("mobile_country_code", mcc);
        data.put("mobile_network_code", mnc);
        array.put(data);
        holder.put("cell_towers", array);
        DefaultHttpClient client = new DefaultHttpClient();
        HttpPost post = new HttpPost("http://www.google.com/loc/json");
        StringEntity se = new StringEntity(holder.toString());
        post.setEntity(se);
        HttpResponse resp = client.execute(post);
        HttpEntity entity = resp.getEntity();
        BufferedReader br = new BufferedReader(new InputStreamReader(entity.getContent()));
        StringBuffer sb = new StringBuffer();
        String result = br.readLine();
        while (result != null) {
            sb.append(result);
            result = br.readLine();
        }
        l_lat = Double.parseDouble(sb.toString().split(":")[2].split(",")[0]);
        l_lon = Double.parseDouble(sb.toString().split(":")[3].split(",")[0]);
        return new double[] { l_lon, l_lat };
    }

    /**
	 * 注册了监听事件，只需调用一次
	 */
    private void openGPS() {
        try {
            isGPSOn = true;
            receiver = new UpdateReceiver();
            IntentFilter filter = new IntentFilter();
            filter.addAction("sendgps");
            this.registerReceiver(receiver, filter);
            Intent intent = new Intent(SearchInMapActivity.this, GPSService.class);
            startService(intent);
            LocationManager lMan = (LocationManager) getSystemService(Context.LOCATION_SERVICE);
            if (lMan.isProviderEnabled(LocationManager.GPS_PROVIDER) != true) {
                Intent callGPSSettingIntent = new Intent(android.provider.Settings.ACTION_LOCATION_SOURCE_SETTINGS);
                startActivity(callGPSSettingIntent);
            }
        } catch (Exception e) {
            Toast.makeText(SearchInMapActivity.this, "打开GPS报错", Toast.LENGTH_SHORT).show();
        }
    }

    public class UpdateReceiver extends BroadcastReceiver {

        @Override
        public void onReceive(Context context, Intent intent) {
            GeoLatLng strGpsValue = (GeoLatLng) intent.getExtras().get("geolatlon");
            GeoLatLng lResult = OffsetInChina.fromEarthToMars(strGpsValue);
            GeoPoint point = new GeoPoint((int) (lResult.latitude * 1000000), (int) (lResult.longitude * 1000000));
            l_lat = strGpsValue.latitude;
            l_lon = strGpsValue.longitude;
            if (isTrackOn) {
                mMapView.getController().setZoom(18);
                mMapView.getController().animateTo(point);
                if (mMapOverlays != null && mMapOverlays.size() > 0 && mMapOverlays.get(0) instanceof NewPositionOverLay) mMapOverlays.remove(0);
                mGPSOverlay = new NewPositionOverLay(point, SearchInMapActivity.this, R.drawable.blue_dot_circle);
                mMapOverlays.add(0, mGPSOverlay);
                mMapView.invalidate();
            }
            if (isGPSNew) {
                isGPSNew = false;
                if (!isTrackOn) {
                    if (mMapOverlays != null && mMapOverlays.size() > 0 && mMapOverlays.get(0) instanceof NewPositionOverLay) mMapOverlays.remove(0);
                    mGPSOverlay = new NewPositionOverLay(point, SearchInMapActivity.this, R.drawable.blue_dot_circle);
                    mMapOverlays.add(0, mGPSOverlay);
                }
                try {
                    Thread.sleep(3000);
                } catch (InterruptedException e) {
                    e.printStackTrace();
                }
                Intent lIntent = new Intent();
                Toast.makeText(SearchInMapActivity.this, strGpsValue.latitude + "," + strGpsValue.latitude, Toast.LENGTH_SHORT).show();
                lIntent.putExtra("geopoint", strGpsValue);
                lIntent.setClass(SearchInMapActivity.this, NewChangsuoActivity.class);
                SearchInMapActivity.this.startActivity(lIntent);
            }
        }
    }

    /**
	 * GPS关闭方法
	 */
    private void closeGPS() {
        isGPSOn = false;
        try {
            this.unregisterReceiver(receiver);
        } catch (Exception e) {
        }
    }

    private void closeGSM() {
        try {
            this.unregisterReceiver(gsmReceiver);
            Intent intent = new Intent(SearchInMapActivity.this, GSMService.class);
            stopService(intent);
        } catch (Exception e) {
        }
    }

    private void initMultipleDialog(List<String> lDataList) {
        mMultipleDialogListAdapter.setData(lDataList);
        mListView.setAdapter(mMultipleDialogListAdapter);
        mListView.setOnItemClickListener(new OnItemClickListener() {

            public void onItemClick(AdapterView<?> parent, View view, int position, long id) {
                mCheckTextView = (CheckedTextView) view.findViewById(R.id.multiple_dialog_item_ctv);
                if (mCheckTextView.isChecked()) {
                    mMultipleDialogListAdapter.removePos(position);
                    mCheckTextView.setChecked(false);
                } else if (!mCheckTextView.isChecked()) {
                    mMultipleDialogListAdapter.addPos(position);
                    mCheckTextView.setChecked(true);
                }
            }
        });
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        try {
            switch(item.getItemId()) {
                case R.id.button_cstype:
                    if (mMultipleDialogListAdapter == null) {
                        mMultipleDialogListAdapter = new ImageMultipleAdapter(SearchInMapActivity.this);
                        mLayoutInflater = LayoutInflater.from(SearchInMapActivity.this);
                        mView = mLayoutInflater.inflate(R.layout.multiple_dialog, null);
                        mListView = (ListView) mView.findViewById(R.id.multiple_dialoglist);
                        mListView.setItemsCanFocus(false);
                        List<String> lDataList = new ArrayList<String>();
                        for (ChangsuoType lType : Enforcement.mCSTypes) {
                            lDataList.add(lType.getId());
                        }
                        initMultipleDialog(lDataList);
                        mDialogBuilder = new AlertDialog.Builder(SearchInMapActivity.this).setTitle("场所类型").setView(mView).setPositiveButton(R.string.confirm, new DialogInterface.OnClickListener() {

                            public void onClick(DialogInterface dialog, int whichButton) {
                            }
                        }).create();
                    }
                    mDialogBuilder.show();
                    break;
                case R.id.button_extentsearch:
                    if (mMapView.getZoomLevel() >= 18) {
                        GeoPoint lPoint = mMapView.getMapCenter();
                        GeoLatLng lLatLon = OffsetInChina.fromMarToEarth(new GeoLatLng((double) lPoint.getLatitudeE6() / 1000000, (double) lPoint.getLongitudeE6() / 1000000));
                        int lLonSpan = java.lang.Math.abs(mMapView.getLongitudeSpan());
                        int lLatSpan = java.lang.Math.abs(mMapView.getLatitudeSpan());
                        System.out.print(lLonSpan);
                        System.out.print(lLatSpan);
                        Log.d("lLonSpan", String.valueOf(lLonSpan));
                        Log.d("lLatSpan", String.valueOf(lLatSpan));
                        int minx = (int) (lLatLon.longitude * 1000000) - lLonSpan / 2;
                        int maxx = (int) (lLatLon.longitude * 1000000) + lLonSpan / 2;
                        int miny = (int) (lLatLon.latitude * 1000000) - lLatSpan / 2;
                        int maxy = (int) (lLatLon.latitude * 1000000) + lLatSpan / 2;
                        String lUrl = retrieveURL(String.valueOf((double) minx / 1000000), String.valueOf((double) miny / 1000000), String.valueOf((double) maxx / 1000000), String.valueOf((double) maxy / 1000000));
                        String lResp = searchResults(lUrl);
                        if (lResp.indexOf("&") != -1 && lResp.indexOf("=") != -1 && lResp.indexOf("^") != -1) {
                            List<ChangsuoInformation> lCSList = DataParseUtil.parseSimpleChangsuoInfo(DataParseUtil.handleResponse(lResp));
                            GeoPoint lOverLayPoint;
                            for (ChangsuoInformation lCsInfo : lCSList) {
                                if (lCsInfo != null && lCsInfo.getLat() != 0 && lCsInfo.getLon() != 0) {
                                    GeoLatLng lResult = OffsetInChina.fromEarthToMars(new GeoLatLng(lCsInfo.getLat(), lCsInfo.getLon()));
                                    lOverLayPoint = new GeoPoint((int) (lResult.latitude * 1000000), (int) (lResult.longitude * 1000000));
                                    doAddOverLay(lOverLayPoint, lCsInfo);
                                }
                            }
                            mMapView.invalidate();
                        } else {
                            Toast.makeText(SearchInMapActivity.this, "当前地图范围内没有查询结果", Toast.LENGTH_LONG).show();
                        }
                    } else Toast.makeText(SearchInMapActivity.this, "地图范围太大，请继续放大地图至合适范围", Toast.LENGTH_LONG).show();
                    break;
                case R.id.button_gpssearch:
                    mProgressDialog = ProgressDialog.show(SearchInMapActivity.this, "", "正在定位，请稍后", true);
                    isGPSSearch = true;
                    try {
                        openGSM();
                    } catch (Exception e) {
                        Toast.makeText(SearchInMapActivity.this, "获取数据出错", Toast.LENGTH_LONG).show();
                    }
                    break;
                case R.id.button_gpstrack:
                    if (!isTrackOn) {
                        Toast.makeText(SearchInMapActivity.this, "GPS跟踪已开启", Toast.LENGTH_SHORT).show();
                        item.setIcon(R.drawable.maps_alt);
                        isTrackOn = true;
                        if (!isGPSOn) openGPS();
                    } else {
                        item.setIcon(R.drawable.maps_alt1);
                        isTrackOn = false;
                        Toast.makeText(SearchInMapActivity.this, "GPS跟踪已关闭", Toast.LENGTH_SHORT).show();
                    }
                    break;
                case R.id.button_mapnewcs:
                    mMapView.getOverlays().clear();
                    mMapView.setOnTouchListener(new OnTouchListener() {

                        @Override
                        public boolean onTouch(View v, MotionEvent event) {
                            float pixelX = event.getX();
                            float pixelY = event.getY();
                            mMapView.setOnTouchListener(null);
                            GeoPoint point = mMapView.getProjection().fromPixels((int) (pixelX), (int) (pixelY));
                            doAddOverLay(point, null);
                            GeoLatLng latlon = OffsetInChina.fromMarToEarth(new GeoLatLng((double) point.getLatitudeE6() / 1000000, (double) point.getLongitudeE6() / 1000000));
                            Intent lIntent = new Intent();
                            lIntent.putExtra("geopoint", latlon);
                            lIntent.setClass(SearchInMapActivity.this, NewChangsuoActivity.class);
                            SearchInMapActivity.this.startActivity(lIntent);
                            return true;
                        }
                    });
                    break;
                case R.id.button_gpsnewcs:
                    Toast.makeText(SearchInMapActivity.this, "等待GPS获取位置信息", Toast.LENGTH_LONG).show();
                    isGPSNew = true;
                    break;
                default:
                    break;
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
        return super.onOptionsItemSelected(item);
    }

    private void doAddOverLay(GeoPoint lOverLayPoint, ChangsuoInformation lCsInfo) {
        if (lCsInfo != null) {
            for (ChangsuoType lCsType : Enforcement.mCSTypes) {
                if (lCsInfo.getAreaType() != null && lCsInfo.getAreaType().equals(lCsType.getId())) {
                    mMapOverlays.add(new MyPositionOverlay(lOverLayPoint, this, getResources().getIdentifier(lCsType.getImage(), "drawable", "com.angis.fx.activity"), lCsInfo));
                    return;
                }
            }
        }
        mMapOverlays.add(new MyPositionOverlay(lOverLayPoint, this, R.drawable.qt, lCsInfo));
    }

    private String searchResults(String pUrl) {
        try {
            HttpClient lClient = new DefaultHttpClient();
            HttpGet lGet = new HttpGet(pUrl);
            HttpResponse lResponse;
            lResponse = lClient.execute(lGet);
            StringBuilder lBuilder = new StringBuilder();
            BufferedReader reader = new BufferedReader(new InputStreamReader(lResponse.getEntity().getContent()));
            for (String s = reader.readLine(); s != null; s = reader.readLine()) {
                lBuilder.append(s);
            }
            return lBuilder.toString();
        } catch (ClientProtocolException e) {
            e.printStackTrace();
        } catch (IOException e) {
            e.printStackTrace();
        }
        return "";
    }

    private String retrieveURL(String pXMin, String pYMin, String pXMax, String pYMax) {
        StringBuilder lBuilder = new StringBuilder();
        lBuilder.append("http://").append(Enforcement.HOST).append("/ZJWHServiceTest/GIS_WHCS.asmx/GetWHCSInfo_GIS").append("?");
        lBuilder.append("lonleft=").append(pXMin);
        lBuilder.append("&").append("lonRight=").append(pXMax);
        lBuilder.append("&").append("latTop=").append(pYMax);
        lBuilder.append("&").append("latBottom=").append(pYMin);
        StringBuilder whcsTypes = new StringBuilder();
        if (mMultipleDialogListAdapter != null && !mMultipleDialogListAdapter.getPos().isEmpty()) {
            Iterator<Integer> lIt = mMultipleDialogListAdapter.getPos().iterator();
            while (lIt.hasNext()) {
                whcsTypes.append(Enforcement.mCSTypes.get(lIt.next())).append(",");
            }
        }
        lBuilder.append("&").append("whcsTypes=").append(whcsTypes.toString());
        lBuilder.append("&").append("areacode=").append("");
        lBuilder.append("&").append("isMap=").append("true");
        return lBuilder.toString();
    }

    @Override
    protected void onDestroy() {
        this.closeGPS();
        Intent intent = new Intent(SearchInMapActivity.this, GPSService.class);
        stopService(intent);
        super.onDestroy();
    }

    @Override
    protected void onPause() {
        this.closeGPS();
        super.onPause();
    }

    @Override
    protected void onResume() {
        this.openGPS();
        super.onResume();
    }

    public static ChangsuoInformation getChangsuoInfoById(String csId) {
        HttpClient lClient = new DefaultHttpClient();
        StringBuilder lBuilder = new StringBuilder();
        HttpGet lGet = new HttpGet();
        HttpResponse lResponse;
        BufferedReader reader;
        try {
            lBuilder.append("http://").append(Enforcement.HOST).append("/ZJWHServiceTest/GIS_WHCS.asmx/GetSingleWHCSByID").append("?").append("csid=").append(csId);
            lGet.setURI(new URI(lBuilder.toString()));
            lResponse = lClient.execute(lGet);
            lBuilder = new StringBuilder();
            reader = new BufferedReader(new InputStreamReader(lResponse.getEntity().getContent()));
            for (String s = reader.readLine(); s != null; s = reader.readLine()) {
                lBuilder.append(s);
            }
            String lResponseString = lBuilder.toString();
            if (lResponseString.indexOf("^") != -1 && lResponseString.indexOf("=") != -1) {
                return DataParseUtil.parseChangsuoInfo2(DataParseUtil.handleResponse(lResponseString));
            }
        } catch (Exception e) {
            e.printStackTrace();
            return null;
        }
        return null;
    }
}

class NewPositionOverLay extends Overlay {

    private GeoPoint geoPoint;

    private Context context;

    private int drawable;

    public NewPositionOverLay(GeoPoint geoPoint, Context context, int drawable) {
        super();
        this.geoPoint = geoPoint;
        this.context = context;
        this.drawable = drawable;
    }

    @Override
    public void draw(Canvas canvas, MapView mapView, boolean shadow) {
        try {
            Projection projection = mapView.getProjection();
            Point point = new Point();
            projection.toPixels(geoPoint, point);
            Bitmap bitmap = BitmapFactory.decodeResource(context.getResources(), drawable);
            canvas.drawBitmap(bitmap, point.x - bitmap.getWidth(), point.y - bitmap.getHeight(), null);
        } catch (Exception e) {
            e.printStackTrace();
        }
        super.draw(canvas, mapView, shadow);
    }
}

class MyPositionOverlay extends Overlay {

    private GeoPoint geoPoint;

    private Context context;

    private int drawable;

    private ChangsuoInformation csInfo;

    public MyPositionOverlay(GeoPoint geoPoint, Context context, int drawable) {
        super();
        this.geoPoint = geoPoint;
        this.context = context;
        this.drawable = drawable;
    }

    public MyPositionOverlay(GeoPoint geoPoint, Context context, int drawable, ChangsuoInformation csInfo) {
        super();
        this.geoPoint = geoPoint;
        this.context = context;
        this.drawable = drawable;
        this.csInfo = csInfo;
    }

    @Override
    public void draw(Canvas canvas, MapView mapView, boolean shadow) {
        try {
            Projection projection = mapView.getProjection();
            Point point = new Point();
            projection.toPixels(geoPoint, point);
            Bitmap bitmap = BitmapFactory.decodeResource(context.getResources(), drawable);
            canvas.drawBitmap(bitmap, point.x - bitmap.getWidth(), point.y - bitmap.getHeight(), null);
        } catch (Exception e) {
            e.printStackTrace();
        }
        super.draw(canvas, mapView, shadow);
    }

    @Override
    public boolean onTap(GeoPoint p, MapView mapView) {
        super.onTap(p, mapView);
        int size = mapView.getOverlays().size();
        Point pointTp = new Point();
        mapView.getProjection().toPixels(p, pointTp);
        Point pointCs;
        int selectedIndex = -1;
        double minDistance = 10000;
        double distance = 0;
        for (int i = 0; i < size; i++) {
            if (mapView.getOverlays().get(i) instanceof MyPositionOverlay) {
                pointCs = new Point();
                mapView.getProjection().toPixels(((MyPositionOverlay) mapView.getOverlays().get(i)).getGeoPoint(), pointCs);
                distance = java.lang.Math.sqrt((pointCs.x - pointTp.x) * (pointCs.x - pointTp.x) + (pointCs.y - pointTp.y) * (pointCs.y - pointTp.y));
                if (distance < 20) {
                    if (distance < minDistance) {
                        minDistance = distance;
                        selectedIndex = i;
                    }
                }
            }
        }
        if (selectedIndex != -1) {
            final ChangsuoInformation lInfo = ((MyPositionOverlay) mapView.getOverlays().get(selectedIndex)).getCsInfo();
            if (lInfo != null && lInfo.getTitle() != null && !lInfo.getTitle().trim().equals("")) {
                Builder lBuilder = new AlertDialog.Builder(context);
                lBuilder.setMessage(lInfo.getTitle());
                lBuilder.setPositiveButton("日常检查", new OnClickListener() {

                    @Override
                    public void onClick(DialogInterface dialog, int which) {
                        ChangsuoInformation lCsInfo = SearchInMapActivity.getChangsuoInfoById(csInfo.getCsId());
                        if (lCsInfo != null) {
                            csInfo = lCsInfo;
                            Enforcement.mCurrentChangsuo = SearchInMapActivity.getChangsuoInfoById(csInfo.getCsId());
                            Intent lIntent = new Intent();
                            lIntent.putExtra("selectedcs", SearchInMapActivity.getChangsuoInfoById(csInfo.getCsId()));
                            lIntent.setClass(context, DailyCheckResultActivity.class);
                            context.startActivity(lIntent);
                        } else {
                            Toast.makeText(context, "业务库中不存在该场所的相关信息", Toast.LENGTH_LONG).show();
                        }
                    }
                });
                lBuilder.setNegativeButton("取消", new OnClickListener() {

                    @Override
                    public void onClick(DialogInterface dialog, int which) {
                    }
                });
                lBuilder.show();
            }
        }
        return true;
    }

    public GeoPoint getGeoPoint() {
        return geoPoint;
    }

    public void setGeoPoint(GeoPoint geoPoint) {
        this.geoPoint = geoPoint;
    }

    public Context getContext() {
        return context;
    }

    public void setContext(Context context) {
        this.context = context;
    }

    public int getDrawable() {
        return drawable;
    }

    public void setDrawable(int drawable) {
        this.drawable = drawable;
    }

    public ChangsuoInformation getCsInfo() {
        return csInfo;
    }

    public void setCsInfo(ChangsuoInformation csInfo) {
        this.csInfo = csInfo;
    }
}
