package com.tx.util;

import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.net.HttpURLConnection;
import java.net.URL;
import android.app.Activity;
import android.app.AlertDialog;
import android.content.DialogInterface;
import com.tx.http.util.HttpUtil;

/**
 * HTTP通讯工具类
 * @author Crane
 *
 */
public class HtmlParseUtil {

    public static String parseHtml(String fileUrl) {
        InputStream is = null;
        ByteArrayOutputStream bos = null;
        try {
            File myFile = new File(fileUrl);
            is = new FileInputStream(myFile);
            byte[] buffer = new byte[1024];
            int len;
            bos = new ByteArrayOutputStream();
            while ((len = is.read(buffer)) != -1) {
                bos.write(buffer, 0, len);
            }
            String str = new String(bos.toByteArray(), "utf-8");
            return str;
        } catch (IOException e) {
            e.printStackTrace();
        } finally {
            try {
                is.close();
                bos.close();
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
        return "";
    }

    public static String getRemoteHtml(String urlpath, final Activity activity) {
        try {
            URL url = new URL(urlpath);
            HttpURLConnection conn = (HttpURLConnection) url.openConnection();
            conn.setConnectTimeout(5000);
            if (conn.getResponseCode() == 200) {
                byte[] data = HttpUtil.getByte(conn.getInputStream());
                return new String(data, "utf-8");
            }
        } catch (Exception e) {
            AlertDialog.Builder b = new AlertDialog.Builder(activity);
            b.setTitle("Alert");
            b.setMessage("对不起，连接已超时，请稍后重试");
            b.setPositiveButton(android.R.string.ok, new AlertDialog.OnClickListener() {

                public void onClick(DialogInterface dialog, int which) {
                    activity.finish();
                }
            });
            b.setCancelable(false);
            b.create();
            b.show();
            return "";
        }
        return "";
    }
}
