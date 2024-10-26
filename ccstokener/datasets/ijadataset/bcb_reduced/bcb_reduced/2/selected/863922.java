package com.clouds.aic.tools;

import java.io.BufferedReader;
import java.io.ByteArrayOutputStream;
import java.io.DataOutputStream;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.net.HttpURLConnection;
import java.net.URL;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import org.apache.http.HttpResponse;
import org.apache.http.NameValuePair;
import org.apache.http.client.HttpClient;
import org.apache.http.client.entity.UrlEncodedFormEntity;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.impl.client.DefaultHttpClient;
import org.apache.http.message.BasicNameValuePair;
import org.apache.http.util.EntityUtils;
import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.util.Log;

public class FileManagementMiddleLayerService {

    /**
	 * store a stream into the local phone.
	 * @param stream which you want to store into the phone
	 * @param the local phone file path. eg. localFilePath = "/sdcard"
	 * @param the file name you set to this stream. eg. wei.mp3, wei.doc 
	 * @return a status string indicate whether it is successful or not
	 */
    public static String storeStreamIntoLocalPhone(InputStream stream, String localFilePath, String fileName) {
        String resultJsonString = "This function has been moved, some problem existed inside the create_new_tag() function if you see this string";
        return resultJsonString;
    }

    /**
	 * download a specific file from CLOUD using the file key
	 * @param sessionid a string that communicate with the CLOUD remote server for the specific session
	 * @param key use the key file to specify the file you want to download
	 */
    public static InputStream download_file(String sessionid, String key) {
        InputStream is = null;
        String urlString = "https://s2.cloud.cm/rpc/raw?c=Storage&m=download_file&key=" + key;
        try {
            String apple = "";
            URL url = new URL(urlString);
            Log.d("current running function name:", "download_file");
            HttpURLConnection conn = (HttpURLConnection) url.openConnection();
            conn.setRequestProperty("Cookie", "PHPSESSID=" + sessionid);
            conn.setRequestMethod("POST");
            conn.setDoInput(true);
            is = conn.getInputStream();
            return is;
        } catch (Exception e) {
            e.printStackTrace();
            Log.d("download problem", "download problem");
        }
        return is;
    }

    private static byte[] InputStreamToByte(InputStream is) throws IOException {
        ByteArrayOutputStream bytestream = new ByteArrayOutputStream();
        int ch;
        while ((ch = is.read()) != -1) {
            bytestream.write(ch);
        }
        byte imgdata[] = bytestream.toByteArray();
        bytestream.close();
        return imgdata;
    }
}
