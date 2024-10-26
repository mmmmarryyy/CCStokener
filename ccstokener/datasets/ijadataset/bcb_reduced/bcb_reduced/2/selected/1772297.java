package com.ad_oss.merkat;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.net.MalformedURLException;
import java.net.URL;
import java.net.URLConnection;
import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;
import org.json.JSONTokener;
import android.os.AsyncTask;
import android.util.Log;

public class MarketListLoader extends AsyncTask<MarketsAdapter, Integer, MarketsAdapter> {

    private JSONArray mJSONList;

    @Override
    protected MarketsAdapter doInBackground(MarketsAdapter... params) {
        try {
            URL url = new URL("http://merkat.sourceforge.net/webservice/markets.php");
            URLConnection conn = url.openConnection();
            conn.setDoOutput(false);
            conn.setDefaultUseCaches(true);
            conn.setUseCaches(true);
            conn.setConnectTimeout(5000);
            InputStream is = conn.getInputStream();
            Log.v(Main.TAG, "loading from " + url);
            String jsonData = readStream(is);
            Log.v(Main.TAG, "loaded data " + jsonData);
            JSONObject object = (JSONObject) new JSONTokener(jsonData).nextValue();
            mJSONList = object.getJSONArray("markets");
        } catch (MalformedURLException e) {
            e.printStackTrace();
        } catch (IOException e) {
            e.printStackTrace();
        } catch (JSONException e) {
            e.printStackTrace();
        }
        return params[0];
    }

    @Override
    protected void onPostExecute(MarketsAdapter result) {
        super.onPostExecute(result);
        try {
            if (mJSONList != null) result.updateList(mJSONList);
        } catch (JSONException e) {
            e.printStackTrace();
        }
    }

    static String readStream(InputStream inputStream) {
        ByteArrayOutputStream outputStream = new ByteArrayOutputStream();
        byte buf[] = new byte[1024];
        int len;
        try {
            while ((len = inputStream.read(buf)) != -1) {
                outputStream.write(buf, 0, len);
            }
            outputStream.close();
            inputStream.close();
        } catch (IOException e) {
        }
        return outputStream.toString();
    }
}
