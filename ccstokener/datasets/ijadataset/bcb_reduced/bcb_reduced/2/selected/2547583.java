package com.download;

import java.io.BufferedReader;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.UnsupportedEncodingException;
import java.net.HttpURLConnection;
import java.net.MalformedURLException;
import java.net.URL;
import java.net.URLEncoder;
import android.R.integer;
import com.utils.FileUtils;

public class DownLoader {

    URL url = null;

    public String download(String urlStr) {
        StringBuffer sb = new StringBuffer();
        String line = null;
        BufferedReader bufferedReader = null;
        try {
            URL url = new URL(urlString2UTF8(urlStr));
            HttpURLConnection urlConnection = (HttpURLConnection) url.openConnection();
            urlConnection.setConnectTimeout(3000);
            InputStreamReader inputStreamReader = new InputStreamReader(urlConnection.getInputStream(), "gb2312");
            String string = inputStreamReader.getEncoding();
            bufferedReader = new BufferedReader(inputStreamReader);
            while ((line = bufferedReader.readLine()) != null) {
                sb.append(line);
            }
        } catch (Exception e) {
            e.printStackTrace();
        } finally {
            try {
                bufferedReader.close();
            } catch (Exception e2) {
                e2.printStackTrace();
            }
        }
        return sb.toString();
    }

    /**
	 * 
	 * @param urlStr
	 * @param path
	 * @param fileName
	 * @return -1表示出错； 0表示成功； 1表示文件已经存在
	 */
    public int downFile(String urlStr, String path, String fileName) {
        InputStream inputStream = null;
        try {
            FileUtils fileUtils = new FileUtils();
            if (fileUtils.isFileExist(fileName, path)) {
                return 1;
            } else {
                inputStream = getInputStreamFromUrl(urlStr);
                File resultFile = fileUtils.write2SDFromInput(path, fileName, inputStream);
                if (resultFile == null) {
                    return -1;
                }
            }
        } catch (Exception e) {
            e.printStackTrace();
            return -1;
        } finally {
            try {
                inputStream.close();
            } catch (Exception e2) {
            }
        }
        return 0;
    }

    public InputStream getInputStreamFromUrl(String urlStr) throws MalformedURLException, IOException {
        url = new URL(urlString2UTF8(urlStr));
        HttpURLConnection connection = (HttpURLConnection) url.openConnection();
        connection.setRequestMethod("GET");
        InputStream inputStream = connection.getInputStream();
        return inputStream;
    }

    public String urlString2UTF8(String string) throws UnsupportedEncodingException {
        int index = string.lastIndexOf('/');
        String utf8String = string.substring(0, index + 1) + URLEncoder.encode(string.substring(index + 1), "UTF-8");
        utf8String = utf8String.replaceAll("\\+", "%20");
        return utf8String;
    }
}
