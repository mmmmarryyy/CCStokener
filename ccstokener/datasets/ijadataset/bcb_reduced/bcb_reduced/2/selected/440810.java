package org.hlj.commons.html;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.File;
import java.io.FileOutputStream;
import java.io.InputStreamReader;
import java.io.OutputStreamWriter;
import java.net.HttpURLConnection;
import java.net.URL;
import java.net.URLConnection;

/**
 * 
 */
public class FileConverter {

    /**
	 * url对象
	 */
    private URL url;

    /**
	 * 输入流
	 */
    private BufferedReader in = null;

    /**
	 * 输出流
	 */
    private BufferedWriter out = null;

    /**
	 * url连接
	 */
    private URLConnection urlConn = null;

    /**
	 * 文件对象
	 */
    private File file = null;

    /**
	 * 输出文件夹对象
	 */
    private File folder = null;

    /**
	 * 转换代码
	 * @param urlPath 	转换源url地址
	 * @param savePath	转换后保存地址 
	 * @param fileName	转换后文件名称
	 * @return 1成功 0失败 -1出现异常
	 */
    public int converter(String urlPath, String savePath, String fileName) {
        int state = 0;
        try {
            url = new URL(urlPath);
            if (!exists(url)) {
                return 0;
            }
            urlConn = url.openConnection();
            in = new BufferedReader(new InputStreamReader(urlConn.getInputStream(), "utf-8"));
            folder = new File(savePath);
            if (!folder.exists()) {
                folder.mkdirs();
            }
            String filePath = savePath + "\\" + fileName;
            file = new File(filePath);
            if (!file.exists()) {
                file.createNewFile();
            }
            out = new BufferedWriter(new OutputStreamWriter(new FileOutputStream(file), "utf-8"));
            out.write("<%@ page language=\"java\" pageEncoding=\"UTF-8\"%> \r\n");
            int c = 0;
            while ((c = in.read()) != -1) {
                out.write(c);
            }
            state = 1;
        } catch (Exception e) {
            state = -1;
            e.printStackTrace();
        } finally {
            this.close();
        }
        return state;
    }

    /**
	 * 判断连接是否存在
	 * @param url
	 * @return
	 */
    private boolean exists(URL url) {
        try {
            HttpURLConnection connection = (HttpURLConnection) url.openConnection();
            if (connection.getResponseCode() == 200) {
                return true;
            } else {
                return false;
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
        return false;
    }

    /**
	 * 关闭流
	 */
    private void close() {
        try {
            if (this.in != null) {
                this.in.close();
            }
            if (this.out != null) {
                this.out.close();
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    public static void main(String[] args) {
        FileConverter fc = new FileConverter();
        fc.converter("http://www.earth-soft.com/dvd-tools/dvd-ripper.html", "D:\\", "a.jsp");
    }
}
