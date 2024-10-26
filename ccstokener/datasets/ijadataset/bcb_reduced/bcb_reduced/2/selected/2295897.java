package com.android.footmap.locationservice;

import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.HttpURLConnection;
import java.net.URL;
import java.net.URLConnection;
import java.util.Timer;
import java.util.TimerTask;
import android.content.Context;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.telephony.TelephonyManager;
import android.telephony.gsm.GsmCellLocation;
import android.util.Log;
import com.android.footmap.common.Constants;
import com.android.footmap.dbservice.DBManager;
import com.android.footmap.dbservice.DBUtil;

public class CellIDService implements LocationService {

    GsmCellLocation gsmlocation;

    TelephonyManager tm;

    private double[] m_location = new double[2];

    private int cellID;

    private int lac;

    private CellIDThread serviceThread;

    private boolean isRunning;

    private Context m_ctx;

    public CellIDService(Context ctx) {
        init(ctx);
    }

    private void init(Context ctx) {
        m_ctx = ctx;
        serviceThread = new CellIDThread();
        isRunning = false;
        tm = (TelephonyManager) m_ctx.getSystemService(Context.TELEPHONY_SERVICE);
    }

    public boolean isServiceAvailble() {
        return true;
    }

    public int getCurrentLocation(double[] location) {
        Log.d("Get", "enter cellid get location");
        int result = Constants.DAO_OK;
        DBManager dbmanager = DBManager.getDBManager();
        SQLiteDatabase con = dbmanager.openDBConnetion();
        String[] columns = new String[] { "value" };
        Cursor cursor = con.query(Constants.TABLE_GLOBAL_SETTING, columns, "section='location_service' and key='current_location'", null, null, null, null, null);
        if (cursor != null) {
            cursor.moveToFirst();
            String location_str = cursor.getString(0);
            location[0] = Double.parseDouble(location_str.substring(0, location_str.indexOf(';')));
            location[1] = Double.parseDouble(location_str.substring(location_str.indexOf(';') + 1));
            dbmanager.closeDBConnetion();
            Log.d("GPS latitude", "" + location[0]);
            Log.d("GPS longitude", "" + location[1]);
        } else result = Constants.DAO_ERROR;
        return result;
    }

    private int getCurrentLocation_google_map_api(double[] location, int cellID, int lac) throws Exception {
        int result = Constants.DAO_OK;
        String urlString = Constants.GoogleCellID_URL;
        URL url;
        url = new URL(urlString);
        URLConnection conn = url.openConnection();
        HttpURLConnection httpConn = (HttpURLConnection) conn;
        httpConn.setRequestMethod("POST");
        httpConn.setDoOutput(true);
        httpConn.setDoInput(true);
        httpConn.setConnectTimeout(10000);
        httpConn.connect();
        OutputStream outputStream = httpConn.getOutputStream();
        WriteData(outputStream, cellID, lac);
        InputStream inputStream = httpConn.getInputStream();
        DataInputStream dataInputStream = new DataInputStream(inputStream);
        dataInputStream.readShort();
        dataInputStream.readByte();
        int code = dataInputStream.readInt();
        if (code == 0) {
            double lat = (double) dataInputStream.readInt() / 1000000D;
            double lng = (double) dataInputStream.readInt() / 1000000D;
            dataInputStream.readInt();
            dataInputStream.readInt();
            dataInputStream.readUTF();
            location[Constants.Latitude] = lat;
            location[Constants.Longitude] = lng;
        } else {
            result = Constants.DAO_ERROR;
        }
        return result;
    }

    private void WriteData(OutputStream out, int cellID, int lac) throws IOException {
        DataOutputStream dataOutputStream = new DataOutputStream(out);
        dataOutputStream.writeShort(21);
        dataOutputStream.writeLong(0);
        dataOutputStream.writeUTF("en");
        dataOutputStream.writeUTF("Android");
        dataOutputStream.writeUTF("1.0");
        dataOutputStream.writeUTF("Web");
        dataOutputStream.writeByte(27);
        dataOutputStream.writeInt(0);
        dataOutputStream.writeInt(0);
        dataOutputStream.writeInt(3);
        dataOutputStream.writeUTF("");
        dataOutputStream.writeInt(cellID);
        dataOutputStream.writeInt(lac);
        dataOutputStream.writeInt(0);
        dataOutputStream.writeInt(0);
        dataOutputStream.writeInt(0);
        dataOutputStream.writeInt(0);
        dataOutputStream.flush();
    }

    public int resumeService() {
        if (!isRunning) {
            serviceThread.startTimer();
            isRunning = true;
        } else {
            serviceThread.resumeTimer();
        }
        Log.d("CELLID", "resume CellID service");
        return 0;
    }

    public int suspendService() {
        serviceThread.suspendTimer();
        Log.d("CELLID", "Suspend CellID Service");
        return 0;
    }

    private class CellIDThread {

        private Timer timer;

        private TimerTask task;

        public CellIDThread() {
            timer = new Timer();
            task = new CellIDTask();
        }

        public void startTimer() {
            if (timer == null) timer = new Timer();
            if (task == null) task = new CellIDTask();
            timer.schedule(task, 1, 10000);
        }

        public void suspendTimer() {
            if (timer == null) timer = new Timer();
            if (task == null) task = new CellIDTask();
            task.cancel();
            timer.cancel();
            task = null;
            timer = null;
        }

        public void resumeTimer() {
            if (task == null) task = new CellIDTask();
            if (timer == null) timer = new Timer();
            timer.schedule(task, 1, 10000);
        }

        public void stop() {
            if (task != null && timer != null) {
                task.cancel();
                timer.cancel();
                task = null;
                timer = null;
            }
        }
    }

    private class CellIDTask extends TimerTask {

        public void run() {
            Log.d("CELLID", "Task is triggered");
            gsmlocation = (GsmCellLocation) tm.getCellLocation();
            cellID = gsmlocation.getCid();
            lac = gsmlocation.getLac();
            cellID = 20442;
            lac = 6015;
            Log.d("Cell ID", "" + cellID);
            Log.d("LAC", "" + lac);
            cellID = 20442;
            lac = 6015;
            try {
                getCurrentLocation_google_map_api(m_location, cellID, lac);
                Log.d("CellID Latitude", "" + m_location[0]);
                Log.d("CellID Longitute", "" + m_location[1]);
                DBUtil.traceLocationInDB(m_location[0], m_location[1]);
                DBUtil.updateGlobalSetting("location_service", "current_location", m_location[0] + ";" + m_location[1], null);
            } catch (NumberFormatException nfe) {
            } catch (Exception e) {
                Log.e("LocateMe", e.toString(), e);
            }
        }
    }

    public int stopService() {
        serviceThread.stop();
        return 0;
    }

    public int startService() {
        Log.d("CELLID", "Start CellID service");
        resumeService();
        return 0;
    }
}
