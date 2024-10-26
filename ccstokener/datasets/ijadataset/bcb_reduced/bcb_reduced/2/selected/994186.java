package com.wei.t3;

import java.io.BufferedReader;
import java.io.DataOutputStream;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.URL;
import android.app.Activity;
import android.app.AlertDialog;
import android.content.Context;
import android.content.DialogInterface;
import android.os.Bundle;
import android.view.View;
import android.widget.Button;
import android.widget.TextView;
import android.widget.Toast;

public class t3 extends Activity {

    private String uploadFile = "/sdcard/test.mp3";

    private String srcPath = "/data/data/record16.JPG";

    private String actionUrl = "http://192.168.1.21:9090/upload_file_service/upload.jsp";

    /** Called when the activity is first created. */
    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        TextView tv = new TextView(this);
        tv.setText("Sending");
        setContentView(tv);
        String FILENAME = "hello_file.txt";
        String string = "hello world changed8!";
        try {
            FileOutputStream fos = openFileOutput(FILENAME, Context.MODE_PRIVATE);
            fos.write(string.getBytes());
            fos.close();
        } catch (Exception e) {
        }
        uploadFile();
    }

    private void uploadFile() {
        String uploadUrl = "https://s2.cloud.cm/rpc/json/?session_id=eqibotp20094kg2qsv4usvflk3&c=Storage&m=upload_file&tag=@/Pictures/Sample";
        String end = "\r\n";
        String twoHyphens = "--";
        String boundary = "******";
        try {
            URL url = new URL(uploadUrl);
            HttpURLConnection httpURLConnection = (HttpURLConnection) url.openConnection();
            httpURLConnection.setDoInput(true);
            httpURLConnection.setDoOutput(true);
            httpURLConnection.setUseCaches(false);
            httpURLConnection.setRequestMethod("POST");
            httpURLConnection.setRequestProperty("Connection", "Keep-Alive");
            httpURLConnection.setRequestProperty("Charset", "UTF-8");
            httpURLConnection.setRequestProperty("Content-Type", "multipart/form-data;boundary=" + boundary);
            DataOutputStream dos = new DataOutputStream(httpURLConnection.getOutputStream());
            dos.writeBytes(twoHyphens + boundary + end);
            dos.writeBytes("Content-Disposition: form-data; name=\"file\"; filename=\"" + srcPath.substring(srcPath.lastIndexOf("/") + 1) + "\"" + end);
            dos.writeBytes(end);
            FileInputStream fis = new FileInputStream(srcPath);
            byte[] buffer = new byte[8192];
            int count = 0;
            while ((count = fis.read(buffer)) != -1) {
                dos.write(buffer, 0, count);
            }
            fis.close();
            dos.writeBytes(end);
            dos.writeBytes(twoHyphens + boundary + twoHyphens + end);
            dos.flush();
            InputStream is = httpURLConnection.getInputStream();
            InputStreamReader isr = new InputStreamReader(is, "utf-8");
            BufferedReader br = new BufferedReader(isr);
            String result = br.readLine();
            Toast.makeText(this, result, Toast.LENGTH_LONG).show();
            dos.close();
            is.close();
        } catch (Exception e) {
            e.printStackTrace();
            setTitle(e.getMessage());
        }
    }
}
