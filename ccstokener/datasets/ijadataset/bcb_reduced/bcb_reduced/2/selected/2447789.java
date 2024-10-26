package org.artags.android.app.util.http;

import android.util.Log;
import java.io.File;
import java.nio.charset.Charset;
import java.util.HashMap;
import org.apache.http.HttpEntity;
import org.apache.http.HttpResponse;
import org.apache.http.client.HttpClient;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.entity.mime.HttpMultipartMode;
import org.apache.http.entity.mime.MultipartEntity;
import org.apache.http.entity.mime.content.FileBody;
import org.apache.http.entity.mime.content.StringBody;
import org.apache.http.impl.client.DefaultHttpClient;
import org.apache.http.util.EntityUtils;

/**
 *
 * @author Pierre Levy
 */
public class HttpUtil {

    /**
     * Post data and attachements
     * @param url The POST url
     * @param params Parameters
     * @param files Files
     * @return The return value
     * @throws HttpException If an error occurs
     */
    public static String post(String url, HashMap<String, String> params, HashMap<String, File> files) throws HttpException {
        String ret = "";
        try {
            HttpClient client = new DefaultHttpClient();
            HttpPost post = new HttpPost(url);
            MultipartEntity reqEntity = new MultipartEntity(HttpMultipartMode.BROWSER_COMPATIBLE);
            for (String key : files.keySet()) {
                FileBody bin = new FileBody(files.get(key));
                reqEntity.addPart(key, bin);
            }
            for (String key : params.keySet()) {
                String val = params.get(key);
                reqEntity.addPart(key, new StringBody(val, Charset.forName("UTF-8")));
            }
            post.setEntity(reqEntity);
            HttpResponse response = client.execute(post);
            HttpEntity resEntity = response.getEntity();
            if (resEntity != null) {
                ret = EntityUtils.toString(resEntity);
                Log.i("ARTags:HttpUtil:Post:Response", ret);
            }
        } catch (Exception e) {
            Log.e("ARTags:HttpUtil", "Error : " + e.getMessage());
            throw new HttpException(e.getMessage());
        }
        return ret;
    }
}
