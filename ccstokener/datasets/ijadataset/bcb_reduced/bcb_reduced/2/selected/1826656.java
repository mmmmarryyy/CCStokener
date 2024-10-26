package ces.platform.infoplat.utils.HTMLParser;

import java.io.BufferedInputStream;
import java.io.BufferedOutputStream;
import java.io.File;
import java.io.FileOutputStream;
import java.io.PrintWriter;
import java.net.HttpURLConnection;
import java.net.URL;
import java.net.URLConnection;
import java.util.Date;
import sun.net.www.protocol.file.FileURLConnection;
import ces.coral.encrypt.MD5;
import ces.coral.log.Logger;

/**
 * <p>Title: ����ƽ̨��Ʒ_��Ϣƽ̨2.5</p>
 * <p>Description: httpЭ�鳣�ù�����
 * <p>Copyright: Copyright (c) 2003</p>
 * <p>Company: ������Ϣ��չ���޹�˾</p>
 * @author ����  ƽ̨С��GOP
 * @version 1.0
 */
public class HttpUtils {

    /**
     * ���캯��
     */
    public HttpUtils() {
    }

    public static Logger log = new Logger(HttpUtils.class);

    /**
     * ������Դ��Ӳ��ָ��Ŀ¼
     * @param strUrl:���ʵ�����URL���磺"http://www.google.com/intl/zh-CN_ALL/images/logo.gif"
     * @param strFileName:���������Ŀ¼(�����ļ����磺��c:\temp\����������ļ���ֲ���<br>
     * ��������ļ������������ļ�����油��3λ������׺��һ�����ֲ���
     * @return �ļ���
     */
    public static String downloadRes(String urlString, String strFilePath) {
        log.debug("ces.platform.infoplat.utils.HTMLParser.HttpUtils.downloadRes urlString:= " + urlString + "  strFilePath:= " + strFilePath);
        URL url;
        URLConnection connection;
        StringBuffer buffer;
        PrintWriter out;
        String fileName = null;
        try {
            File filePath = new File(strFilePath);
            if (!filePath.exists()) {
                filePath.mkdirs();
            }
            fileName = urlString.substring(urlString.lastIndexOf("/") + 1);
            if ((new File(strFilePath + fileName)).exists()) {
                MD5 md5 = new MD5();
                fileName = fileName.substring(0, fileName.indexOf(".")) + String.valueOf(Math.round(Math.random() * 100)) + fileName.substring(fileName.indexOf("."));
                fileName = md5.getMD5ofStr(fileName.substring(0, fileName.indexOf(".")) + String.valueOf((new Date()).getTime())) + fileName.substring(fileName.indexOf("."));
            }
            if (urlString.trim().toLowerCase().startsWith("file")) {
                url = new URL(urlString);
                connection = (FileURLConnection) url.openConnection();
            } else if (urlString.trim().toLowerCase().startsWith("http")) {
                url = new URL(urlString);
                connection = (HttpURLConnection) url.openConnection();
            } else {
                return fileName;
            }
            connection.setDoOutput(true);
            connection.setDoInput(true);
            connection.setUseCaches(false);
            BufferedInputStream bis = new BufferedInputStream(connection.getInputStream());
            FileOutputStream fo = new FileOutputStream(strFilePath + fileName);
            BufferedOutputStream bos = new BufferedOutputStream(fo);
            byte[] buff = new byte[2048];
            int bytesRead;
            while (-1 != (bytesRead = bis.read(buff, 0, buff.length))) {
                bos.write(buff, 0, bytesRead);
            }
            bis.close();
            bos.close();
        } catch (Exception e) {
            log.error("������Դʧ��!", e);
        }
        return fileName;
    }

    /**
     * main Function for unit Test
     * @param args
     */
    public static void main(String[] args) throws Exception {
        for (int i = 0; i < 10; i++) {
            System.out.println(downloadRes("http://www.google.com/intl/zh-CN_ALL/images", "c:\\temp\\"));
        }
    }
}
