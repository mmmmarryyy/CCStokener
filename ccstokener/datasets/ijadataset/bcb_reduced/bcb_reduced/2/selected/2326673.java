package com.litt.core.net;

import java.io.BufferedInputStream;
import java.io.BufferedOutputStream;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.net.HttpURLConnection;
import org.apache.log4j.Logger;
import com.litt.core.common.Utility;

/**
 * 通过HTTP采集远程文件
 * @author <a href="mailto:littcai@hotmail.com">空心大白菜</a>
 * @since 2006-08-30
 * @version 1.0
 * 
 */
public class URLFile {

    private static final Logger logger = Logger.getLogger(URLFile.class);

    private String remoteUrl;

    private String filePath;

    private String fileName;

    /**
	 * 
	 * 根据文件url地址采集并创建到本地
	 * 
	 * @param remoteUrl  远程文件url路径
	 * 
	 * @param filePathAndName
	 *            预创建到本地的文件的绝对路径含文件名已经扩展名
	 * 
	 * @return	
	 * 
	 */
    public boolean readFile(String remoteUrl, String filePath, String fileName) {
        boolean bea = false;
        java.net.URL urlfile = null;
        HttpURLConnection httpUrl = null;
        BufferedInputStream bis = null;
        BufferedOutputStream bos = null;
        if (fileName == null || fileName.equals("")) fileName = Utility.getUrlFileName(remoteUrl);
        File f = new File(filePath, fileName);
        try {
            urlfile = new java.net.URL(remoteUrl);
            httpUrl = (HttpURLConnection) urlfile.openConnection();
            httpUrl.connect();
            bis = new BufferedInputStream(httpUrl.getInputStream());
        } catch (Exception e) {
            logger.error("远程文件读取失败", e);
        }
        try {
            bos = new BufferedOutputStream(new FileOutputStream(f));
            byte[] buf = new byte[1024];
            int bufsize = 0;
            while ((bufsize = bis.read(buf, 0, buf.length)) != -1) {
                bos.write(buf, 0, bufsize);
            }
            bea = true;
            logger.info(remoteUrl + " 采集成功！文件已存储至：" + filePath + fileName);
        } catch (IOException e) {
            bea = false;
            logger.error(remoteUrl + "采集失败", e);
        } finally {
            try {
                bos.flush();
                bis.close();
                httpUrl.disconnect();
            } catch (Exception e) {
                logger.error("关闭HTTP连接失败", e);
            }
        }
        return bea;
    }

    /**
	 * 同构函数
	 * @return
	 */
    public boolean readFile() {
        return readFile(remoteUrl, filePath, fileName);
    }

    /**
	 * @param args
	 */
    public static void main(String[] args) {
        String s1 = "http://localhost:8080/TEST/test.txt";
        String s2 = "d:\\";
        URLFile fo = new URLFile();
        fo.readFile(s1, s2, null);
    }

    public String getFileName() {
        return fileName;
    }

    public void setFileName(String fileName) {
        this.fileName = fileName;
    }

    public String getFilePath() {
        return filePath;
    }

    public void setFilePath(String filePath) {
        this.filePath = filePath;
    }

    public String getRemoteUrl() {
        return remoteUrl;
    }

    public void setRemoteUrl(String remoteUrl) {
        this.remoteUrl = remoteUrl;
    }
}
