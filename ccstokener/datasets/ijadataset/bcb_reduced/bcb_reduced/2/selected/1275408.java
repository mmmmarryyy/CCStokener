package edu.fub.pub2search.app.util;

import java.io.BufferedInputStream;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStreamWriter;
import java.io.UnsupportedEncodingException;
import java.net.URL;
import java.net.URLConnection;
import java.net.URLEncoder;
import java.util.Iterator;
import java.util.Map;
import java.util.Set;
import org.apache.http.util.ByteArrayBuffer;
import android.content.Context;
import android.util.Log;

/**
 * An utility class that manage I/O operations. 
 * 
 * @author Josef
 */
public class AndroidIOUtil {

    /**
	 * Call the Pub2Search service and then read the returned response. 
	 * 
	 * @param endpoint The endpoint of Pub2Search service.
	 * @param var The service parameters.
	 * @return List of pubs in JSON format.
	 */
    public static String getHttpGetRequestResponse(String endpoint, Map<String, String> params) {
        String address = UrlBuilder(endpoint, params);
        String data = null;
        try {
            URL url = new URL(address);
            URLConnection ucon = url.openConnection();
            InputStream is = ucon.getInputStream();
            BufferedInputStream bis = new BufferedInputStream(is);
            ByteArrayBuffer baf = new ByteArrayBuffer(50);
            int current = 0;
            while ((current = bis.read()) != -1) {
                baf.append((byte) current);
            }
            data = new String(baf.toByteArray());
        } catch (Exception e) {
            Log.e("AndroidIOUtil.java", "I/O Exception: Error in getting HTTP Request response.");
        }
        return data;
    }

    /**
	 * Write the data into an output file.
	 * 
	 * @param filename The name of the output file.
	 * @param data The data string.
	 * @param ctx The application context.
	 * @return Returns true if the file is successfully created and vice-versa.
	 */
    public static boolean writeDataToFile(String filename, String data, Context ctx) {
        try {
            FileOutputStream fout = ctx.openFileOutput(filename, Context.MODE_PRIVATE);
            OutputStreamWriter osw = new OutputStreamWriter(fout);
            osw.write(data);
            osw.flush();
            osw.close();
            return true;
        } catch (IOException e) {
            Log.e("AndroidIOUtil.java", "I/O Exception: Error in writing a file.");
            return false;
        }
    }

    /**
     * Read a file and retrieve its content.
     * 
     * @param filename The name of the file.
     * @param ctx The application context.
     * @return The content of the file.
     */
    public static String readDataFromFile(String filename, Context ctx) {
        String data = null;
        try {
            FileInputStream fin = ctx.openFileInput(filename);
            BufferedInputStream bis = new BufferedInputStream(fin);
            ByteArrayBuffer baf = new ByteArrayBuffer(50);
            int current = 0;
            while ((current = bis.read()) != -1) {
                baf.append((byte) current);
            }
            data = new String(baf.toByteArray());
        } catch (IOException e) {
            Log.e("AndroidIOUtil.java", "I/O Exception: Error in reading file.");
        }
        return data;
    }

    private static String UrlBuilder(String endpoint, Map<String, String> params) {
        String url = endpoint;
        if (params != null && params.size() > 0) {
            try {
                StringBuilder builder = new StringBuilder(url);
                builder.append("?");
                Set<String> keys = params.keySet();
                Iterator<String> iter = keys.iterator();
                while (true) {
                    String key = iter.next();
                    builder.append(key).append("=").append(URLEncoder.encode(params.get(key), "UTF-8"));
                    if (iter.hasNext()) {
                        builder.append("&");
                    } else {
                        break;
                    }
                }
                url = builder.toString();
            } catch (UnsupportedEncodingException e) {
                Log.e("AndroidIOUtil.java", "Unsupported Encoding Exception");
            }
        }
        return url;
    }
}
