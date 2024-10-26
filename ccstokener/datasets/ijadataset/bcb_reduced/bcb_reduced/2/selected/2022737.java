package usar.mobile;

import android.location.Location;
import android.util.Log;
import org.apache.http.HttpResponse;
import org.apache.http.NameValuePair;
import org.apache.http.client.ClientProtocolException;
import org.apache.http.client.HttpClient;
import org.apache.http.client.entity.UrlEncodedFormEntity;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.impl.client.DefaultHttpClient;
import org.apache.http.message.BasicNameValuePair;
import java.io.IOException;
import java.io.UnsupportedEncodingException;
import java.util.ArrayList;
import java.util.List;

public class DataTransferUtil {

    private DataTransferUtil() {
    }

    public static void uploadData(Location location) {
        String url = "http://urbansearchrescue.appspot.com/upload";
        HttpClient httpclient = new DefaultHttpClient();
        HttpPost httpPost = new HttpPost(url);
        try {
            if (location == null) {
                return;
            }
            double longitude = location.getLongitude();
            double latitude = location.getLatitude();
            List<NameValuePair> nameValuePairs = new ArrayList<NameValuePair>(2);
            nameValuePairs.add(new BasicNameValuePair("x_coord", Double.toString(longitude)));
            nameValuePairs.add(new BasicNameValuePair("y_coord", Double.toString(latitude)));
            httpPost.setEntity(new UrlEncodedFormEntity(nameValuePairs));
            Log.v("DataTransferUtil", "executing request " + httpPost.getRequestLine());
            HttpResponse response = httpclient.execute(httpPost);
        } catch (UnsupportedEncodingException e) {
            e.printStackTrace();
        } catch (ClientProtocolException e) {
            e.printStackTrace();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }
}
