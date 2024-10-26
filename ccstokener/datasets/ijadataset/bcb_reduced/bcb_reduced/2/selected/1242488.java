package com.tx.util;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.net.HttpURLConnection;
import java.net.MalformedURLException;
import java.net.URL;
import java.util.ArrayList;
import java.util.List;
import java.util.zip.ZipException;

/**
 * 文件管理工具类
 * @author Crane
 *
 */
public class FileUtil {

    /**
	 * 删除指定目录下的全部内容。
	 * 
	 * @param path 要删除的路径
	 * @param rootFlag true:根目录删除/false:根目录保留
	 */
    public static void deleteAll(String path, boolean rootFlag) {
        File root = new File(path);
        if (root.isFile()) {
            root.delete();
        } else {
            String[] list = root.list();
            List<String> fLst = new ArrayList<String>(0);
            for (int i = 0; list != null && i < list.length; i++) {
                File temp = new File(path + File.separator + list[i]);
                if (temp.isFile()) {
                    temp.delete();
                } else {
                    fLst.add(temp.getAbsolutePath());
                }
            }
            for (String dir : fLst) {
                deleteAll(dir, rootFlag);
                File tempDir = new File(dir);
                tempDir.delete();
            }
            if (rootFlag) {
                root.delete();
            }
        }
    }

    public static void downloadFile(String appUrl) {
        URL url;
        try {
            url = new URL(appUrl);
            HttpURLConnection conn = (HttpURLConnection) url.openConnection();
            conn.setConnectTimeout(5000);
            int resCode = conn.getResponseCode();
            if (resCode == 200) {
                InputStream is = conn.getInputStream();
                install(is, appUrl);
            }
        } catch (MalformedURLException e) {
            e.printStackTrace();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    private static void install(InputStream is, String appUrl) {
        String fileWholeName = appUrl.substring(appUrl.lastIndexOf(File.separator) + 1);
        String fileName = fileWholeName.substring(0, fileWholeName.indexOf("."));
        FileOutputStream fos = null;
        File zipFile = null;
        try {
            byte[] tempByte = new byte[is.available()];
            int i = 0;
            File appFile = new File(UrlConfigUtil.APP_URL + File.separator + fileName + File.separator);
            if (!appFile.exists()) {
                appFile.mkdirs();
            }
            zipFile = new File(appFile + File.separator + fileWholeName);
            if (!zipFile.exists()) {
                zipFile.createNewFile();
                fos = new FileOutputStream(zipFile);
                while ((i = is.read(tempByte)) != -1) {
                    fos.write(tempByte, 0, i);
                    fos.flush();
                }
            }
            if (zipFile != null) {
                try {
                    ZipUtils.upZipFile(zipFile, UrlConfigUtil.APP_URL + File.separator + fileName);
                } catch (ZipException e) {
                    e.printStackTrace();
                } catch (IOException e) {
                    e.printStackTrace();
                }
            }
        } catch (IOException e) {
        } finally {
            try {
                if (fos != null) fos.close();
                if (is != null) is.close();
            } catch (IOException e) {
            }
        }
    }
}
