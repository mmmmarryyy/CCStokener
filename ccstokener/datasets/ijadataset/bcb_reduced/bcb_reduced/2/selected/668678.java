package cn.LocationStation;

import java.io.BufferedReader;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.net.URL;
import java.util.Date;
import org.apache.http.HttpEntity;
import org.apache.http.HttpResponse;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.entity.StringEntity;
import org.apache.http.impl.client.DefaultHttpClient;
import org.json.JSONArray;
import org.json.JSONObject;
import android.app.Activity;
import android.content.Context;
import android.os.Bundle;
import android.telephony.TelephonyManager;
import android.telephony.gsm.GsmCellLocation;
import android.view.View;
import android.widget.Button;
import android.widget.TextView;

public class LocationStation extends Activity {

    TextView mTextView;

    Button mButton;

    TelephonyManager tm;

    /** Called when the activity is first created. */
    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.main);
        mTextView = (TextView) findViewById(R.id.textView001);
        mButton = (Button) findViewById(R.id.Button001);
        tm = (TelephonyManager) this.getSystemService(Context.TELEPHONY_SERVICE);
        mButton.setOnClickListener(new Button.OnClickListener() {

            @Override
            public void onClick(View v) {
                GsmCellLocation gcl = (GsmCellLocation) tm.getCellLocation();
                int cid = gcl.getCid();
                int lac = gcl.getLac();
                int mcc = Integer.valueOf(tm.getNetworkOperator().substring(0, 3));
                int mnc = Integer.valueOf(tm.getNetworkOperator().substring(3, 5));
                try {
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
                    JSONObject jsonObject = new JSONObject(sb.toString());
                    JSONObject jsonObject1 = new JSONObject(jsonObject.getString("location"));
                    getAddress(jsonObject1.getString("latitude"), jsonObject1.getString("longitude"));
                } catch (Exception e) {
                }
            }
        });
    }

    void getAddress(String lat, String lag) {
        try {
            URL url = new URL("http://maps.google.cn/maps/geo?key=abcdefg&q=" + lat + "," + lag);
            InputStream inputStream = url.openConnection().getInputStream();
            InputStreamReader inputReader = new InputStreamReader(inputStream, "utf-8");
            BufferedReader bufReader = new BufferedReader(inputReader);
            String line = "", lines = "";
            while ((line = bufReader.readLine()) != null) {
                lines += line;
            }
            if (!lines.equals("")) {
                JSONObject jsonobject = new JSONObject(lines);
                JSONArray jsonArray = new JSONArray(jsonobject.get("Placemark").toString());
                for (int i = 0; i < jsonArray.length(); i++) {
                    mTextView.setText(mTextView.getText() + "\n" + jsonArray.getJSONObject(i).getString("address"));
                }
            }
        } catch (Exception e) {
            ;
        }
    }
}
