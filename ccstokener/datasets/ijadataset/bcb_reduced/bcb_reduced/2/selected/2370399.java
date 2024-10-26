package pcgen.android;

import java.io.BufferedWriter;
import java.io.InputStream;
import java.io.OutputStream;
import java.io.OutputStreamWriter;
import org.apache.commons.lang.Validate;
import org.apache.http.HttpEntity;
import org.apache.http.HttpResponse;
import org.apache.http.client.HttpClient;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.impl.client.DefaultHttpClient;
import org.json.JSONObject;

public class JSONUtils {

    public static JSONObject fromStream(InputStream stream) throws Throwable {
        Validate.notNull(stream);
        try {
            return new JSONObject(Utils.fromStreamToString(stream));
        } catch (Throwable tr) {
            Logger.e(TAG, "fromStream", tr);
            throw tr;
        }
    }

    public static JSONObject fromString(String value) throws Throwable {
        try {
            return new JSONObject(value);
        } catch (Throwable tr) {
            Logger.e(TAG, "fromString", tr);
            throw tr;
        }
    }

    public static void toStream(JSONObject json, OutputStream stream) throws Throwable {
        Validate.notNull(json);
        Validate.notNull(stream);
        try {
            BufferedWriter writer = null;
            try {
                writer = new BufferedWriter(new OutputStreamWriter(stream));
                writer.write(json.toString());
                writer.flush();
            } finally {
                try {
                    if (writer != null) writer.close();
                } catch (Exception ex) {
                }
                try {
                    if (stream != null) stream.close();
                } catch (Exception ex) {
                }
            }
        } catch (Throwable tr) {
            Logger.e(TAG, "toStream", tr);
            throw tr;
        }
    }

    public static JSONObject fromUrl(String url) throws Throwable {
        Validate.notEmpty(url);
        InputStream stream = null;
        HttpClient httpclient = null;
        try {
            httpclient = new DefaultHttpClient();
            HttpGet httpget = new HttpGet(url);
            HttpResponse response = httpclient.execute(httpget);
            if (response != null) {
                HttpEntity entity = response.getEntity();
                if (entity != null) {
                    try {
                        stream = entity.getContent();
                        return fromStream(stream);
                    } finally {
                        try {
                            if (stream != null) stream.close();
                        } catch (Exception ex) {
                        }
                    }
                }
            }
        } catch (Throwable tr) {
            Logger.e(TAG, "fromUrl", tr);
            throw tr;
        } finally {
            if (httpclient != null) httpclient.getConnectionManager().shutdown();
        }
        return null;
    }

    private static final String TAG = JSONUtils.class.getSimpleName();
}
