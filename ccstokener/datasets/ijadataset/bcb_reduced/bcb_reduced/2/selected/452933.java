package com.luciddreamingapp.beta.util;

import java.io.BufferedInputStream;
import java.io.BufferedOutputStream;
import java.io.ByteArrayInputStream;
import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.InputStream;
import java.io.OutputStream;
import java.io.UnsupportedEncodingException;
import java.net.HttpURLConnection;
import java.net.MalformedURLException;
import java.net.URL;
import java.util.zip.GZIPOutputStream;
import android.content.Context;
import android.util.Log;

public class FileUploader extends Thread {

    Context context;

    public static final String TAG = "file uploader";

    public static final boolean D = false;

    HttpURLConnection connection = null;

    DataOutputStream outputStream = null;

    DataInputStream inputStream = null;

    InputStream is = null;

    FileInputStream fileInputStream = null;

    String pathToOurFile = "/sdcard/application data/lucid dreaming app/smarttimerconfig.txt.gzip";

    String urlServer = "http://luciddreamingapp.com/fileuploads/uploader.php";

    URL url;

    String lineEnd = "\r\n";

    String twoHyphens = "--";

    String boundary = "*****";

    int bytesRead, bytesAvailable, bufferSize;

    byte[] buffer;

    int maxBufferSize = 1 * 1024 * 1024;

    String uuid;

    public FileUploader(String uuid, String serverUrl) {
        urlServer = serverUrl;
        try {
            url = new URL(urlServer);
        } catch (MalformedURLException e) {
            if (D) e.printStackTrace();
        }
        this.uuid = uuid;
    }

    public void uploadMass() {
        for (File file : FileHelper.getFiles("/sdcard/application data/lucid dreaming app/graph data/", null)) {
            if (D) Log.w("file uploader", "file: " + file.getName());
            if (D) Log.e("uploader", "file exists?: " + file.exists());
            upload(file, file.getName() + uuid);
        }
    }

    public boolean openConnection() {
        try {
            connection = (HttpURLConnection) url.openConnection();
            connection.setDoInput(true);
            connection.setDoOutput(true);
            connection.setUseCaches(true);
            connection.setRequestMethod("POST");
            connection.setRequestProperty("Connection", "Keep-Alive");
            connection.setRequestProperty("Content-Type", "multipart/form-data;boundary=" + boundary);
            connection.connect();
            outputStream = new DataOutputStream(connection.getOutputStream());
        } catch (Exception e) {
            if (D) e.printStackTrace();
            return false;
        }
        return true;
    }

    public boolean closeConnection() {
        try {
            if (fileInputStream != null) fileInputStream.close();
            if (is != null) is.close();
            outputStream.flush();
            outputStream.close();
            connection.disconnect();
            return true;
        } catch (Exception e) {
            if (D) e.printStackTrace();
            return false;
        }
    }

    public String upload(String s, String filename) {
        if (D) Log.e(TAG, "*upload(string,string)*");
        try {
            is = new ByteArrayInputStream(s.getBytes("UTF-8"));
        } catch (UnsupportedEncodingException e) {
            e.printStackTrace();
            return null;
        }
        String temp;
        try {
            temp = writeData(filename, is);
            sleep(1000);
            return temp;
        } catch (InterruptedException e) {
            e.printStackTrace();
        } catch (Exception e) {
            e.printStackTrace();
        }
        return null;
    }

    public String upload(File f, String filename) {
        if (D) Log.e(TAG, "*upload(file,string)*");
        try {
            if (!f.exists()) return null;
            fileInputStream = new FileInputStream(f);
            String temp = writeData(filename, fileInputStream);
            sleep(1000);
            return temp;
        } catch (Exception ex) {
            ex.printStackTrace();
            return null;
        }
    }

    private String writeZipData(String filename, InputStream input) throws Exception {
        outputStream.writeBytes(twoHyphens + boundary + lineEnd);
        Log.w("uploader", "Content-Disposition: form-data; name=\"uploadedfile\";filename=\"" + filename + "\"" + lineEnd);
        outputStream.writeBytes("Content-Disposition: form-data; name=\"uploadedfile\";filename=\"" + filename + "\"" + lineEnd);
        outputStream.writeBytes(lineEnd);
        bytesAvailable = input.available();
        bufferSize = Math.min(bytesAvailable, maxBufferSize);
        buffer = new byte[bufferSize];
        bytesRead = input.read(buffer, 0, bufferSize);
        while (bytesRead > 0) {
            outputStream.write(buffer, 0, bufferSize);
            bytesAvailable = input.available();
            bufferSize = Math.min(bytesAvailable, maxBufferSize);
            bytesRead = input.read(buffer, 0, bufferSize);
        }
        outputStream.writeBytes(lineEnd);
        outputStream.writeBytes(twoHyphens + boundary + twoHyphens + lineEnd);
        int serverResponseCode = connection.getResponseCode();
        String serverResponseMessage = connection.getResponseMessage();
        if (D) Log.e("uploader", "responseCode: " + serverResponseCode);
        if (D) Log.e("uploader", "responseMessage: " + serverResponseMessage);
        return serverResponseMessage;
    }

    private String writeData(String filename, InputStream input) throws Exception {
        outputStream.writeBytes(twoHyphens + boundary + lineEnd);
        if (D) Log.w("uploader", "Content-Disposition: form-data; name=\"uploadedfile\";filename=\"" + filename + "\"" + lineEnd);
        outputStream.writeBytes("Content-Disposition: form-data; name=\"uploadedfile\";filename=\"" + filename + "\"" + lineEnd);
        outputStream.writeBytes(lineEnd);
        bytesAvailable = input.available();
        bufferSize = Math.min(bytesAvailable, maxBufferSize);
        buffer = new byte[bufferSize];
        bytesRead = input.read(buffer, 0, bufferSize);
        while (bytesRead > 0) {
            outputStream.write(buffer, 0, bufferSize);
            bytesAvailable = input.available();
            bufferSize = Math.min(bytesAvailable, maxBufferSize);
            bytesRead = input.read(buffer, 0, bufferSize);
        }
        outputStream.writeBytes(lineEnd);
        outputStream.writeBytes(twoHyphens + boundary + twoHyphens + lineEnd);
        int serverResponseCode = connection.getResponseCode();
        String serverResponseMessage = connection.getResponseMessage();
        if (D) Log.e("uploader", "responseCode: " + serverResponseCode);
        if (D) Log.e("uploader", "responseMessage: " + serverResponseMessage);
        return serverResponseMessage;
    }
}
