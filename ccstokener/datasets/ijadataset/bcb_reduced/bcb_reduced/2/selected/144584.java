package com.jBible.utils;

import java.io.BufferedInputStream;
import java.io.BufferedReader;
import java.io.File;
import java.io.FileFilter;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.net.URL;
import java.net.URLConnection;
import java.util.ArrayList;
import java.util.HashMap;
import org.apache.http.util.ByteArrayBuffer;
import android.content.Context;
import android.os.Environment;

public class FileUtilities {

    private static final String TAG = "Module";

    /**
	 * Выполняет поиск папок с модулями Цитаты на внешнем носителе устройства
	 * @return Возвращает ArrayList со списком ini-файлов модулей 
	 */
    public static ArrayList<String> SearchModules() {
        Log.i(TAG, "SearchModules()");
        ArrayList<String> iniFiles = new ArrayList<String>();
        String state = Environment.getExternalStorageState();
        if (!Environment.MEDIA_MOUNTED.equals(state)) {
            return iniFiles;
        }
        File fileSearchDir = new File(Environment.getExternalStorageDirectory().toString() + "/jBible/modules/");
        if (!fileSearchDir.exists()) {
            return iniFiles;
        }
        try {
            SearchBQIni(fileSearchDir, iniFiles);
        } catch (Exception e) {
            Log.i(TAG, "Exception in SearchModules(): \r\n" + e.getLocalizedMessage());
            return iniFiles;
        }
        return iniFiles;
    }

    /**
	 * Рекурсивная функция проходит по всем каталогам в поисках ini-файлов
	 * Цитаты
	 */
    private static void SearchBQIni(File currentFile, ArrayList<String> iniFiles) throws IOException {
        try {
            OnlyBQIni filter = new OnlyBQIni();
            File[] files = currentFile.listFiles(filter);
            if (files == null) {
                return;
            }
            for (File file : files) {
                if (!file.canRead()) {
                    continue;
                } else if (file.isDirectory()) {
                    SearchBQIni(file, iniFiles);
                } else {
                    iniFiles.add(file.getAbsolutePath());
                    break;
                }
            }
        } catch (Exception e) {
            Log.i(TAG, e.getMessage());
        }
    }

    public static BufferedReader OpenFile(String path, String encoding) {
        File iniFile = new File(path);
        return OpenFile(iniFile, encoding);
    }

    public static BufferedReader OpenFile(File file, String encoding) {
        Log.i(TAG, "FileUtilities.OpenFile(" + file + ", " + encoding + ")");
        if (!file.exists()) {
            return null;
        }
        BufferedReader bReader = null;
        try {
            InputStreamReader iReader;
            iReader = new InputStreamReader(new FileInputStream(file), encoding);
            bReader = new BufferedReader(iReader);
        } catch (Exception e) {
            Log.i(TAG, e.toString());
            return null;
        }
        return bReader;
    }

    public static InputStream getAssetStream(Context context, String paramString) throws IOException {
        return context.getResources().getAssets().open(paramString);
    }

    public static String getAssetString(Context context, String paramString) {
        InputStream localInputStream;
        try {
            localInputStream = context.getResources().getAssets().open(paramString);
            StringBuilder sBuilder = new StringBuilder();
            InputStreamReader localInputStreamReader = new InputStreamReader(localInputStream);
            BufferedReader localBufferedReader = new BufferedReader(localInputStreamReader);
            for (String str = localBufferedReader.readLine(); str != null; str = localBufferedReader.readLine()) sBuilder.append(str).append("\n");
            return sBuilder.toString();
        } catch (IOException e) {
            return "";
        }
    }

    public static String getModuleEncoding(File file) {
        Log.i(TAG, "FileUtilities.isUnicode(" + file + ")");
        String encoding = "cp1251";
        if (!file.exists()) {
            return encoding;
        }
        HashMap<String, String> charsets = new HashMap<String, String>();
        charsets.put("0", "ISO-8859-1");
        charsets.put("1", "US-ASCII");
        charsets.put("77", "MacRoman");
        charsets.put("78", "Shift_JIS");
        charsets.put("79", "ms949");
        charsets.put("80", "GB2312");
        charsets.put("81", "Big5");
        charsets.put("82", "johab");
        charsets.put("83", "MacHebrew");
        charsets.put("84", "MacArabic");
        charsets.put("85", "MacGreek");
        charsets.put("86", "MacTurkish");
        charsets.put("87", "MacThai");
        charsets.put("88", "cp1250");
        charsets.put("89", "cp1251");
        charsets.put("128", "MS932");
        charsets.put("129", "ms949");
        charsets.put("130", "ms1361");
        charsets.put("134", "ms936");
        charsets.put("136", "ms950");
        charsets.put("161", "cp1253");
        charsets.put("162", "cp1254");
        charsets.put("163", "cp1258");
        charsets.put("177", "cp1255");
        charsets.put("178", "cp1256");
        charsets.put("186", "cp1257");
        charsets.put("201", "cp1252");
        charsets.put("204", "cp1251");
        charsets.put("222", "ms874");
        charsets.put("238", "cp1250");
        charsets.put("254", "cp437");
        charsets.put("255", "cp850");
        String str = "", key, value;
        BufferedReader bReader = OpenFile(file, "utf-8");
        try {
            while ((str = bReader.readLine()) != null) {
                int pos = str.indexOf("//");
                if (pos >= 0) str = str.substring(0, pos);
                int delimiterPos = str.indexOf("=");
                if (delimiterPos == -1) {
                    continue;
                }
                key = str.substring(0, delimiterPos).trim().toLowerCase();
                delimiterPos++;
                value = delimiterPos >= str.length() ? "" : str.substring(delimiterPos, str.length()).trim();
                if (key.equals("desiredfontcharset")) {
                    return charsets.containsKey(value) ? charsets.get(value) : encoding;
                } else if (key.equals("defaultencoding")) {
                    return value;
                }
            }
        } catch (IOException e) {
            return encoding;
        }
        return encoding;
    }

    public static boolean loadContentFromURL(String fromURL, String toFile) {
        try {
            URL url = new URL("http://bible-desktop.com/xml" + fromURL);
            File file = new File(toFile);
            URLConnection ucon = url.openConnection();
            InputStream is = ucon.getInputStream();
            BufferedInputStream bis = new BufferedInputStream(is);
            ByteArrayBuffer baf = new ByteArrayBuffer(50);
            int current = 0;
            while ((current = bis.read()) != -1) {
                baf.append((byte) current);
            }
            FileOutputStream fos = new FileOutputStream(file);
            fos.write(baf.toByteArray());
            fos.close();
        } catch (IOException e) {
            Log.e(TAG, e);
            return false;
        }
        return true;
    }
}

class OnlyBQIni implements FileFilter {

    private String filter;

    public OnlyBQIni() {
        this.filter = "bibleqt.ini";
    }

    public OnlyBQIni(String filter) {
        this.filter = filter;
    }

    @Override
    public boolean accept(File myFile) {
        return myFile.getName().toLowerCase().equals(this.filter) || myFile.isDirectory();
    }
}
