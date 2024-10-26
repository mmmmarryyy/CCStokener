package com.yxl.util.socket;

import java.io.BufferedInputStream;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.RandomAccessFile;
import java.net.HttpURLConnection;
import java.net.URL;

/**
 * 下载文件，支持断点续传功能
 * @version 1.0
 */
public class DownLoader {

    /** 临时文件后缀 */
    String BACK_SUFFIX = ".yy";

    /**
	 * 下载文件接口
	 * @param downLoadUrl 下载地址
	 * @param saveUrl	  保存文件路径
	 * @return 返回状态码   1：成功   -1：失败   404：没有找到要下载的文件   500：网络连接失败
	 */
    public int down(String downLoadUrl, String saveUrl) {
        int status = 1;
        long fileSize = 0;
        int len = 0;
        byte[] bt = new byte[1024];
        RandomAccessFile raFile = null;
        long totalSize = 0;
        URL url = null;
        HttpURLConnection httpConn = null;
        BufferedInputStream bis = null;
        try {
            url = new URL(downLoadUrl);
            httpConn = (HttpURLConnection) url.openConnection();
            if (httpConn.getHeaderField("Content-Length") == null) {
                status = 500;
            } else {
                totalSize = Long.parseLong(httpConn.getHeaderField("Content-Length"));
                System.out.println("文件大小:" + totalSize / 1000000 + " M");
                httpConn.disconnect();
                httpConn = (HttpURLConnection) url.openConnection();
                fileSize = loadFileSize(saveUrl + BACK_SUFFIX);
                System.out.println("已下载:" + fileSize / 1000000 + " M");
                httpConn.setRequestProperty("RANGE", "bytes=" + fileSize + "-");
                httpConn.setRequestProperty("Accept", "image/gif,image/x-xbitmap,application/msword,*/*");
                raFile = new RandomAccessFile(saveUrl + BACK_SUFFIX, "rw");
                raFile.seek(fileSize);
                bis = new BufferedInputStream(httpConn.getInputStream());
                while ((len = bis.read(bt)) > 0) {
                    raFile.write(bt, 0, len);
                    float progress = 0.f;
                    float downSize = raFile.length();
                    progress = downSize / totalSize;
                    System.out.println(progress * 100 + "%" + "\t\t" + downSize / 1000000 + "M");
                }
            }
        } catch (FileNotFoundException e) {
            status = 404;
        } catch (Exception e) {
            e.printStackTrace();
        } finally {
            try {
                if (bis != null) bis.close();
                if (raFile != null) raFile.close();
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
        if (loadFileSize(saveUrl + BACK_SUFFIX) == totalSize) {
            fileRename(saveUrl + BACK_SUFFIX, saveUrl);
        }
        return status;
    }

    /**
	 * 读取已经下载了的文件大小
	 * @param pathAndFile
	 * @return
	 */
    private long loadFileSize(String pathAndFile) {
        File file = new File(pathAndFile);
        return file.length();
    }

    /**
	 * 文件改名
	 */
    private void fileRename(String fName, String nName) {
        File file = new File(fName);
        file.renameTo(new File(nName));
        file.delete();
    }

    /**
	 * 测试
	 */
    public static void main(String[] args) {
        String downLoadUrl = "http://plugins.jquery.com/files/jquery.validate_15.zip";
        String saveUrl = "d:/dl2.rar";
        DownLoader dl = new DownLoader();
        int status = dl.down(downLoadUrl, saveUrl);
        System.out.println("返回值: " + status);
    }
}
