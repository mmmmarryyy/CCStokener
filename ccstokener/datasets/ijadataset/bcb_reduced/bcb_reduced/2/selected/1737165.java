package com.test;

import java.io.DataOutputStream;
import java.io.FileInputStream;
import java.io.InputStream;
import java.net.HttpURLConnection;
import java.net.URL;
import android.app.Activity;
import android.app.AlertDialog;
import android.content.DialogInterface;
import android.os.Bundle;
import android.view.View;
import android.widget.Button;
import android.widget.TextView;

public class Main extends Activity {

    private String newName = "image.jpg";

    private String uploadFile = "/data/image.jpg";

    private String actionUrl = "http://l27.0.0.1/upload/upload.jsp";

    private TextView mText1;

    private TextView mText2;

    private Button mButton;

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.main);
        mText1 = (TextView) findViewById(R.id.myText2);
        mText1.setText("�ļ�·����\n" + uploadFile);
        mText2 = (TextView) findViewById(R.id.myText3);
        mText2.setText("�ϴ���ַ��\n" + actionUrl);
        mButton = (Button) findViewById(R.id.myButton);
        mButton.setOnClickListener(new View.OnClickListener() {

            public void onClick(View v) {
                uploadFile();
            }
        });
    }

    private void uploadFile() {
        String end = "\r\n";
        String twoHyphens = "--";
        String boundary = "*****";
        try {
            URL url = new URL(actionUrl);
            HttpURLConnection con = (HttpURLConnection) url.openConnection();
            con.setDoInput(true);
            con.setDoOutput(true);
            con.setUseCaches(false);
            con.setRequestMethod("POST");
            con.setRequestProperty("Connection", "Keep-Alive");
            con.setRequestProperty("Charset", "UTF-8");
            con.setRequestProperty("Content-Type", "multipart/form-data;boundary=" + boundary);
            DataOutputStream ds = new DataOutputStream(con.getOutputStream());
            ds.writeBytes(twoHyphens + boundary + end);
            ds.writeBytes("Content-Disposition: form-data; " + "name=\"file1\";filename=\"" + newName + "\"" + end);
            ds.writeBytes(end);
            FileInputStream fStream = new FileInputStream(uploadFile);
            int bufferSize = 1024;
            byte[] buffer = new byte[bufferSize];
            int length = -1;
            while ((length = fStream.read(buffer)) != -1) {
                ds.write(buffer, 0, length);
            }
            ds.writeBytes(end);
            ds.writeBytes(twoHyphens + boundary + twoHyphens + end);
            fStream.close();
            ds.flush();
            InputStream is = con.getInputStream();
            int ch;
            StringBuffer b = new StringBuffer();
            while ((ch = is.read()) != -1) {
                b.append((char) ch);
            }
            showDialog(b.toString().trim());
            ds.close();
        } catch (Exception e) {
            showDialog("" + e);
        }
    }

    private void showDialog(String mess) {
        new AlertDialog.Builder(Main.this).setTitle("Message").setMessage(mess).setNegativeButton("ȷ��", new DialogInterface.OnClickListener() {

            public void onClick(DialogInterface dialog, int which) {
            }
        }).show();
    }
}
