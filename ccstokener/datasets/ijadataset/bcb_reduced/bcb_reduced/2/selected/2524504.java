package eyes.blue;

import java.io.IOException;
import java.io.UnsupportedEncodingException;
import java.net.MalformedURLException;
import java.net.URI;
import java.net.URISyntaxException;
import java.net.URL;
import java.util.ArrayList;
import org.apache.http.HttpResponse;
import org.apache.http.client.ClientProtocolException;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.entity.StringEntity;
import org.apache.http.impl.client.DefaultHttpClient;
import org.apache.http.util.EntityUtils;
import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;
import android.content.Context;
import android.util.Log;

public class LogRepoter {

    public final int version = 1;

    static long id = 1;

    static String tag = null;

    static URI respURI = null;

    static JSONObject jobj = new JSONObject();

    static JSONArray msg = null;

    static long dev = -1;

    static boolean enable = true;

    static boolean reporting = false;

    static Object lockey = new Object();

    public static void setRecever(Context context, String respURLPath, long ida, String taga) throws MalformedURLException, URISyntaxException {
        msg = new JSONArray();
        id = ida;
        tag = taga;
        String UID = android.provider.Settings.Secure.getString(context.getContentResolver(), android.provider.Settings.Secure.ANDROID_ID);
        Log.d("LamrimReader", "Android ID=" + UID);
        dev = UID.hashCode();
        respURI = new URI(respURLPath);
    }

    public static void reportMachineType() {
        StringBuffer sb = new StringBuffer();
        sb.append("BOARD:" + android.os.Build.BOARD + "\n");
        sb.append("BOOTLOADER:" + android.os.Build.BOOTLOADER + "\n");
        sb.append("BRAND:" + android.os.Build.BRAND + "\n");
        sb.append("CPU_ABI2:" + android.os.Build.CPU_ABI2 + "\n");
        sb.append("DEVICE:" + android.os.Build.DEVICE + "\n");
        sb.append("DISPLAY:" + android.os.Build.DISPLAY + "\n");
        sb.append("FINGERPRINT:" + android.os.Build.FINGERPRINT + "\n");
        sb.append("HARDWARE:" + android.os.Build.HARDWARE + "\n");
        sb.append("HOST:" + android.os.Build.HOST + "\n");
        sb.append("ID:" + android.os.Build.ID + "\n");
        sb.append("MANUFACTURER:" + android.os.Build.MANUFACTURER + "\n");
        sb.append("MODEL:" + android.os.Build.MODEL + "\n");
        sb.append("PRODUCT:" + android.os.Build.PRODUCT + "\n");
        sb.append("RADIO:" + android.os.Build.RADIO + "\n");
        sb.append("TAGS:" + android.os.Build.TAGS + "\n");
        sb.append("TIME:" + android.os.Build.TIME + "\n");
        sb.append("TYPE:" + android.os.Build.TYPE + "\n");
        sb.append("USER:" + android.os.Build.USER + "\n");
        sb.append("RELEASE:" + android.os.Build.VERSION.RELEASE + "\n");
        log(sb.toString());
    }

    public static void log(String log) {
        if (!enable) return;
        msg.put(log);
        synchronized (lockey) {
            if (!reporting) reporting = true;
            Thread t = new Thread() {

                public void run() {
                    flush();
                }
            };
            t.start();
        }
    }

    private static void flush() {
        int respCode = -1;
        HttpPost request = new HttpPost(respURI);
        JSONObject param = new JSONObject();
        try {
            param.put("ID", id);
            param.put("DEV", dev);
            param.put("TAG", tag);
            param.put("MSG", msg);
            param.put("TIME", System.currentTimeMillis());
        } catch (JSONException e) {
            e.printStackTrace();
        }
        synchronized (lockey) {
            reporting = false;
        }
        Log.d("LamrimReader", "gson=" + param.toString());
        StringEntity se;
        try {
            se = new StringEntity(param.toString());
            request.setEntity(se);
            HttpResponse httpResp = new DefaultHttpClient().execute(request);
            respCode = httpResp.getStatusLine().getStatusCode();
            msg = new JSONArray();
        } catch (UnsupportedEncodingException e) {
            e.printStackTrace();
        } catch (ClientProtocolException e) {
            e.printStackTrace();
        } catch (IOException e) {
            e.printStackTrace();
        }
        Log.d("LogRepoter", "Write msg to remote host return: " + respCode);
    }

    public static boolean isEnable() {
        return enable;
    }

    public static void setEnable(boolean enable) {
        LogRepoter.enable = enable;
    }

    public static void setTag(String tag) {
        LogRepoter.tag = tag;
    }
}
