package com.hamdyghanem.holyquran;

import java.io.BufferedInputStream;
import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.URL;
import java.net.URLConnection;
import org.apache.http.util.ByteArrayBuffer;
import android.graphics.Bitmap;
import android.os.Environment;
import android.util.Log;

public class ImageManager {

    public static String DownloadFromUrl(String ImageType, String strActivePath, String imgName, String fileName) {
        try {
            URL url = new URL(strActivePath + "/img/" + ImageType + "/" + imgName + ".img");
            File file = new File(fileName);
            long startTime = System.currentTimeMillis();
            Log.d("ImageManager", "downloaded file name:" + fileName + " - url:" + url.toString());
            URLConnection ucon = url.openConnection();
            InputStream is = ucon.getInputStream();
            BufferedInputStream bis = new BufferedInputStream(is, 8192);
            ByteArrayBuffer baf = new ByteArrayBuffer(50);
            int current = 0;
            while ((current = bis.read()) != -1) {
                baf.append((byte) current);
            }
            FileOutputStream fos = new FileOutputStream(file);
            fos.write(baf.toByteArray());
            fos.close();
            Log.d("ImageManager", "download ready in" + ((System.currentTimeMillis() - startTime) / 1000) + " sec");
            return "";
        } catch (IOException e) {
            Log.d("ImageManager", "Error: " + e);
            return e.getMessage();
        }
    }

    public static String DownloadDBFromUrl(String strActivePath) {
        try {
            URL url = new URL(strActivePath + "/hquran.dat");
            File file = new File(Environment.getExternalStorageDirectory().getAbsolutePath() + "/hQuran/hquran.dat");
            file.deleteOnExit();
            long startTime = System.currentTimeMillis();
            Log.d("ImageManager", "downloaded file name:" + "hquran.dat");
            URLConnection ucon = url.openConnection();
            int lenghtOfFile = ucon.getContentLength();
            FileOutputStream fos = new FileOutputStream(file);
            InputStream is = ucon.getInputStream();
            byte data[] = new byte[1024];
            int count = 0;
            long total = 0;
            int progress = 0;
            while ((count = is.read(data)) != -1) {
                total += count;
                int progress_temp = (int) total * 100 / lenghtOfFile;
                if (progress_temp % 10 == 0 && progress != progress_temp) {
                    progress = progress_temp;
                    Log.v("Downloading", "total = " + progress);
                }
                fos.write(data, 0, count);
            }
            is.close();
            fos.close();
            Log.d("ImageManager", "download ready in" + ((System.currentTimeMillis() - startTime) / 1000) + " sec");
            return "";
        } catch (IOException e) {
            Log.d("ImageManager", "Error: " + e);
            return e.getMessage();
        }
    }

    public static String DownloadActivePath() {
        try {
            URL url = new URL("http://dl.dropbox.com/u/27675084/ActivePath.txt");
            File file = new File(Environment.getExternalStorageDirectory().getAbsolutePath() + "/hQuran/ActivePath.txt");
            file.deleteOnExit();
            long startTime = System.currentTimeMillis();
            Log.d("ImageManager", "downloaded file name:" + "ActivePath.txt");
            URLConnection ucon = url.openConnection();
            int lenghtOfFile = ucon.getContentLength();
            FileOutputStream fos = new FileOutputStream(file);
            InputStream is = ucon.getInputStream();
            byte data[] = new byte[1024];
            int count = 0;
            long total = 0;
            int progress = 0;
            while ((count = is.read(data)) != -1) {
                total += count;
                int progress_temp = (int) total * 100 / lenghtOfFile;
                if (progress_temp % 10 == 0 && progress != progress_temp) {
                    progress = progress_temp;
                    Log.v("Downloading", "total = " + progress);
                }
                fos.write(data, 0, count);
            }
            is.close();
            fos.close();
            Log.d("ImageManager", "download ready in" + ((System.currentTimeMillis() - startTime) / 1000) + " sec");
            return "";
        } catch (IOException e) {
            Log.d("ImageManager", "Error: " + e);
            return e.getMessage();
        }
    }

    public static String saveToSDCard(Bitmap resourceImage, String str, Boolean inimgTypes) {
        StringBuffer createdFile = new StringBuffer();
        String finalName = Environment.getExternalStorageDirectory().getAbsolutePath();
        if (inimgTypes) finalName += "/hQuran/imgTypes/" + str + ".img"; else finalName += "/hQuran/img/" + str + ".img";
        File externalStorageFile = new File(finalName);
        ByteArrayOutputStream bytes = new ByteArrayOutputStream();
        resourceImage.compress(Bitmap.CompressFormat.JPEG, 100, bytes);
        byte b[] = bytes.toByteArray();
        try {
            externalStorageFile.createNewFile();
            OutputStream filoutputStream = new FileOutputStream(externalStorageFile);
            filoutputStream.write(b);
            filoutputStream.flush();
            filoutputStream.close();
            createdFile.append(externalStorageFile.getAbsolutePath());
        } catch (IOException e) {
            return e.getMessage();
        }
        return createdFile.toString();
    }

    public static String DownloadTafserFromUrl(String strActivePath, String imgName, String fileName) {
        try {
            URL url = new URL(strActivePath + "/tafseer/" + imgName + ".html");
            File file = new File(fileName);
            long startTime = System.currentTimeMillis();
            Log.d("ImageManager", "downloaded file name:" + fileName);
            URLConnection ucon = url.openConnection();
            InputStream is = ucon.getInputStream();
            BufferedInputStream bis = new BufferedInputStream(is, 8192);
            ByteArrayBuffer baf = new ByteArrayBuffer(50);
            int current = 0;
            while ((current = bis.read()) != -1) {
                baf.append((byte) current);
            }
            FileOutputStream fos = new FileOutputStream(file);
            fos.write(baf.toByteArray());
            fos.close();
            Log.d("ImageManager", "download ready in" + ((System.currentTimeMillis() - startTime) / 1000) + " sec");
            return "";
        } catch (IOException e) {
            Log.d("ImageManager", "Error: " + e);
            return e.getMessage();
        }
    }

    public static String DownloadTareefFromUrl(String strActivePath, String imgName, String fileName) {
        try {
            URL url = new URL(strActivePath + "/taareef/" + imgName + ".html");
            File file = new File(fileName);
            long startTime = System.currentTimeMillis();
            Log.d("ImageManager", "downloaded file name:" + fileName);
            URLConnection ucon = url.openConnection();
            InputStream is = ucon.getInputStream();
            BufferedInputStream bis = new BufferedInputStream(is, 8192);
            ByteArrayBuffer baf = new ByteArrayBuffer(50);
            int current = 0;
            while ((current = bis.read()) != -1) {
                baf.append((byte) current);
            }
            FileOutputStream fos = new FileOutputStream(file);
            fos.write(baf.toByteArray());
            fos.close();
            Log.d("ImageManager", "download ready in" + ((System.currentTimeMillis() - startTime) / 1000) + " sec");
            return "";
        } catch (IOException e) {
            Log.d("ImageManager", "Error: " + e);
            return e.getMessage();
        }
    }

    public static String DownloadDictionaryFromUrl(String strActivePath, String imgName, String fileName) {
        try {
            URL url = new URL(strActivePath + "/dictionary/" + imgName + ".html");
            File file = new File(fileName);
            long startTime = System.currentTimeMillis();
            Log.d("ImageManager", "downloaded file name:" + fileName);
            URLConnection ucon = url.openConnection();
            InputStream is = ucon.getInputStream();
            BufferedInputStream bis = new BufferedInputStream(is, 8192);
            ByteArrayBuffer baf = new ByteArrayBuffer(50);
            int current = 0;
            while ((current = bis.read()) != -1) {
                baf.append((byte) current);
            }
            FileOutputStream fos = new FileOutputStream(file);
            fos.write(baf.toByteArray());
            fos.close();
            Log.d("ImageManager", "download ready in" + ((System.currentTimeMillis() - startTime) / 1000) + " sec");
            return "";
        } catch (IOException e) {
            Log.d("ImageManager", "Error: " + e);
            return e.getMessage();
        }
    }

    public static String DownloadAudioFromUrl(String Reciter, String strActivePath, String imgName, String fileName) {
        try {
            URL url = new URL(strActivePath + "/audio/" + Reciter + "/" + imgName + ".aud");
            File file = new File(fileName);
            long startTime = System.currentTimeMillis();
            Log.d("ImageManager", "downloaded audio file name:" + fileName + " * path:" + url.getFile());
            URLConnection ucon = url.openConnection();
            InputStream is = ucon.getInputStream();
            BufferedInputStream bis = new BufferedInputStream(is, 8192);
            ByteArrayBuffer baf = new ByteArrayBuffer(50);
            int current = 0;
            while ((current = bis.read()) != -1) {
                baf.append((byte) current);
            }
            FileOutputStream fos = new FileOutputStream(file);
            if (file.length() == 0) file.deleteOnExit();
            fos.write(baf.toByteArray());
            fos.close();
            Log.d("ImageManager", "download ready in" + ((System.currentTimeMillis() - startTime) / 1000) + " sec");
            return "";
        } catch (IOException e) {
            Log.d("ImageManager", "Error: " + e);
            return e.getMessage();
        }
    }
}
