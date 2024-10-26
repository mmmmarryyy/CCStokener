package cn.chengdu.in.android.location;

import java.util.ArrayList;
import org.apache.http.HttpEntity;
import org.apache.http.HttpResponse;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.entity.StringEntity;
import org.apache.http.impl.client.DefaultHttpClient;
import org.apache.http.params.BasicHttpParams;
import org.apache.http.params.HttpConnectionParams;
import org.apache.http.params.HttpParams;
import org.apache.http.util.EntityUtils;
import org.json.JSONArray;
import org.json.JSONObject;
import cn.chengdu.in.android.config.Config;
import android.content.Context;
import android.os.AsyncTask;
import android.util.Log;

public class GearsLocationClient {

    private static final boolean DEBUG = Config.DEBUG;

    private GearsLocationTask mGearsLocationTask;

    private CellIDInfoManager mCellIDInfoManager;

    private WifiInfoManager mWifiInfoManager;

    private String[] mPostData;

    private CellInfo mCellInfo;

    private Context mContext;

    private OnGearsLocationListener mOnGearsLocationListener;

    private boolean isFirstLocation = true;

    public static final String RESULT_FIRST_FAILE = "error";

    public GearsLocationClient(Context context) {
        mContext = context;
        mCellIDInfoManager = new CellIDInfoManager();
        mWifiInfoManager = new WifiInfoManager();
    }

    public String[] start() {
        isFirstLocation = true;
        mPostData = createPostData();
        return mPostData;
    }

    public void end(String result) {
        if (mOnGearsLocationListener != null) {
            mOnGearsLocationListener.onGearsLocationReturn(result);
        }
    }

    public void stop() {
        if (mGearsLocationTask != null) {
            mGearsLocationTask.cancel(true);
        }
    }

    public boolean requestGearsLocation() {
        return requestGearsLocation(0);
    }

    /**
     * 请求gears的基站定位
     * count 为0时 使用带基站信息的参数, 
     * count 为1时, 不带基站信息
     * @return
     */
    public boolean requestGearsLocation(int count) {
        if (mPostData == null) {
            return false;
        }
        if (mGearsLocationTask != null) {
            mGearsLocationTask.cancel(true);
        }
        mGearsLocationTask = new GearsLocationTask();
        mGearsLocationTask.execute(mPostData[count]);
        return true;
    }

    public void setOnGearsLocationListener(OnGearsLocationListener l) {
        mOnGearsLocationListener = l;
    }

    /**
     * 拼接post json字符串
     * @param cellInfo
     * @param radioType
     * @return 一个长度2的数组, 分别为带基站信息和不带基站信息
     */
    private String[] createPostData() {
        JSONObject data = new JSONObject();
        try {
            data.put("version", "1.1.0");
            data.put("host", "maps.google.com");
            data.put("request_address", true);
            data.put("address_language", "zh_CN");
            try {
                ArrayList<WifiInfo> wifiInfos = mWifiInfoManager.getWifiInfo(mContext);
                if (wifiInfos != null && wifiInfos.size() > 0) {
                    JSONArray wifis = new JSONArray();
                    for (WifiInfo wifiInfo : wifiInfos) {
                        JSONObject wifi = new JSONObject();
                        wifi.put("mac_address", wifiInfo.mac);
                        wifi.put("ssid", wifiInfo.ssid);
                        wifi.put("signal_strength", wifiInfo.strength);
                        wifi.put("age", 0);
                        wifis.put(wifi);
                    }
                    data.put("wifi_towers", wifis);
                }
            } catch (Exception e) {
                e.printStackTrace();
            }
            String str2 = data.toString();
            try {
                ArrayList<CellInfo> cellInfos = mCellIDInfoManager.getCellIDInfo(mContext);
                if (cellInfos != null && cellInfos.size() > 0) {
                    CellInfo cellInfo = cellInfos.get(0);
                    mCellInfo = cellInfo;
                    data.put("radio_type", cellInfo.radioType);
                    JSONArray cells = new JSONArray();
                    JSONObject cell = new JSONObject();
                    cell.put("cell_id", cellInfo.cellId);
                    cell.put("location_area_code", cellInfo.locationAreaCode);
                    cell.put("mobile_country_code", cellInfo.mobileCountryCode);
                    cell.put("mobile_network_code", cellInfo.mobileNetworkCode);
                    cell.put("age", 0);
                    cells.put(cell);
                    data.put("cell_towers", cells);
                }
            } catch (Exception e) {
                e.printStackTrace();
            }
            String str1 = data.toString();
            String[] result = new String[] { str1, str2 };
            return result;
        } catch (Exception e) {
            e.printStackTrace();
        }
        return null;
    }

    public interface OnGearsLocationListener {

        public void onGearsLocationReturn(String data);
    }

    public CellInfo getCellInfo() {
        return mCellInfo;
    }

    private class GearsLocationTask extends AsyncTask<String, Void, String> {

        @Override
        protected String doInBackground(String... params) {
            try {
                final HttpParams param = new BasicHttpParams();
                HttpConnectionParams.setConnectionTimeout(param, 30000);
                HttpConnectionParams.setSoTimeout(param, 30000);
                DefaultHttpClient client = new DefaultHttpClient(param);
                HttpPost post = new HttpPost("http://www.google.com/loc/json");
                post.setEntity(new StringEntity(params[0]));
                if (DEBUG) Log.d("Location", params[0]);
                HttpResponse resp = client.execute(post);
                if (resp.getStatusLine().getStatusCode() == 200) {
                    HttpEntity entity = resp.getEntity();
                    String result = EntityUtils.toString(entity);
                    return result;
                } else {
                    if (isFirstLocation) {
                        requestGearsLocation(1);
                        isFirstLocation = false;
                        return RESULT_FIRST_FAILE;
                    }
                }
            } catch (Exception e) {
                e.printStackTrace();
            }
            return null;
        }

        @Override
        protected void onPostExecute(String result) {
            if (!RESULT_FIRST_FAILE.equals(result)) {
                end(result);
            }
        }
    }
}
