package mh.common;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStreamWriter;
import java.io.UnsupportedEncodingException;
import java.net.HttpURLConnection;
import java.net.URL;
import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.Date;
import java.util.zip.ZipEntry;
import java.util.zip.ZipInputStream;
import mh.common.mail.MailSenderInfo;
import mh.common.mail.SimpleMailSender;
import org.apache.commons.lang.StringUtils;
import org.apache.commons.fileupload.FileItem;
import org.apache.log4j.Logger;
import mh.common.DateUtils;

public class CommonUtil {

    private static Logger logger = Logger.getLogger(CommonUtil.class);

    /**
	 * 获得指定URL的内容
	 * @param url
	 * @return String 
	 */
    public static String getUrlContent(String url) {
        if (StringUtils.isBlank(url)) return "";
        StringBuffer rtn = new StringBuffer();
        HttpURLConnection huc = null;
        BufferedReader br = null;
        try {
            huc = (HttpURLConnection) new URL(url).openConnection();
            huc.connect();
            InputStream stream = huc.getInputStream();
            br = new BufferedReader(new InputStreamReader(stream, "utf-8"));
            String line;
            while ((line = br.readLine()) != null) {
                if (line.trim().length() > 0) {
                    rtn.append(line);
                }
            }
            br.close();
            huc.disconnect();
        } catch (IOException ioe) {
            ioe.printStackTrace();
        }
        return rtn.toString();
    }

    public static void sendMail(String toEmail, String title, String content) {
        MailSenderInfo mailInfo = new MailSenderInfo();
        mailInfo.setMailServerHost("smtp.163.com");
        mailInfo.setMailServerPort("25");
        mailInfo.setValidate(true);
        mailInfo.setUserName("mmdease@163.com");
        mailInfo.setPassword("www>163>com");
        mailInfo.setFromAddress("mmdease@163.com");
        mailInfo.setToAddress(toEmail);
        mailInfo.setSubject(title);
        mailInfo.setContent(content);
        SimpleMailSender sms = new SimpleMailSender();
        sms.sendHtmlMail(mailInfo);
    }

    /**
     * 返回当前年
     */
    public static String getSysDateYear() {
        return DateUtils.formatDate2(new Date()).substring(0, 4);
    }

    /**
	 * 获取系统时间.
	 * 
	 * @return yyyy-MM-dd
	 */
    public static String getSysDate2() {
        String d = DateUtils.formatDate2(new Date());
        return d;
    }

    /**
	 * 获取系统时间.
	 * 
	 * @return yyyy-MM-dd
	 */
    public static String formatDate13() {
        String d = DateUtils.formatDate13(new Date());
        return d;
    }

    /**
	 * 获取系统时间.
	 * 
	 * @return yyyyMMddHHmiss
	 */
    public static String getSysDate5() {
        String d = DateUtils.formatDate5(new Date());
        return d;
    }

    /**
	 * iso-8859-1转换成utf8
	 * @param s
	 * @return
	 */
    public static String isoToUtf8(String s) {
        if (StringUtils.isBlank(s)) return null;
        String rtn = "";
        try {
            rtn = new String(s.getBytes("ISO-8859-1"), "UTF-8");
        } catch (UnsupportedEncodingException e) {
            e.printStackTrace();
        }
        return rtn;
    }

    /**
	 * iso-8859-1转换成utf8
	 * @param s
	 * @return
	 */
    public static String gbkToUtf8(String s) {
        String rtn = "";
        try {
            rtn = new String(s.getBytes("GBK"), "UTF-8");
        } catch (UnsupportedEncodingException e) {
            e.printStackTrace();
        }
        return rtn;
    }

    /**
	 * iso-8859-1转换成utf8
	 * @param s
	 * @return
	 */
    public static String testStrEncod(String rtn) {
        if (null == rtn) return "";
        try {
            System.out.println("UTF-8**GB2312>>" + new String(rtn.getBytes("UTF-8"), "GB2312"));
            System.out.println("UTF-8**GBK>>" + new String(rtn.getBytes("UTF-8"), "GBK"));
            System.out.println("UTF-8**ASCII>>" + new String(rtn.getBytes("UTF-8"), "ASCII"));
            System.out.println("UTF-8**ISO8859-1>>" + new String(rtn.getBytes("UTF-8"), "ISO8859-1"));
            System.out.println();
            System.out.println("GB2312**UTF-8>>" + new String(rtn.getBytes("GB2312"), "UTF-8"));
            System.out.println("GB2312**GBK>>" + new String(rtn.getBytes("GB2312"), "GBK"));
            System.out.println("GB2312**ISO8859-1>>" + new String(rtn.getBytes("GB2312"), "ISO8859-1"));
            System.out.println("GB2312**ASCII>>" + new String(rtn.getBytes("GB2312"), "ASCII"));
            System.out.println();
            System.out.println("GBK**UTF-8>>" + new String(rtn.getBytes("GBK"), "UTF-8"));
            System.out.println("GBK**GB2312>>" + new String(rtn.getBytes("GBK"), "GB2312"));
            System.out.println("GBK**ISO8859-1>>" + new String(rtn.getBytes("GBK"), "ISO8859-1"));
            System.out.println("GBK**ASCII>>" + new String(rtn.getBytes("GBK"), "ASCII"));
            System.out.println();
            System.out.println("ASCII**UTF-8>>" + new String(rtn.getBytes("ASCII"), "UTF-8"));
            System.out.println("ASCII**GBK>>" + new String(rtn.getBytes("ASCII"), "GBK"));
            System.out.println("ASCII**ISO8859-1>>" + new String(rtn.getBytes("ASCII"), "ISO8859-1"));
            System.out.println("ASCII**GB2312>>" + new String(rtn.getBytes("ASCII"), "GB2312"));
            System.out.println();
            System.out.println("ISO8859-1**UTF-8>>" + new String(rtn.getBytes("ISO8859-1"), "UTF-8"));
            System.out.println("ISO8859-1**GBK>>" + new String(rtn.getBytes("ISO8859-1"), "GBK"));
            System.out.println("ISO8859-1**ASCII>>" + new String(rtn.getBytes("ISO8859-1"), "ASCII"));
            System.out.println("ISO8859-1**GB2312>>" + new String(rtn.getBytes("ISO8859-1"), "GB2312"));
        } catch (UnsupportedEncodingException e) {
            e.printStackTrace();
        }
        return rtn;
    }

    /**
	 * 将FileItime保存到硬盘
	 * @param item
	 * @param fileFullPath
	 * @return
	 */
    public static boolean saveFileItemToDisk(FileItem item, String fileFullPath) {
        File savedFile;
        savedFile = new File(fileFullPath);
        String folderPath = savedFile.getParent();
        File savedFileFolder = new File(folderPath);
        if (!savedFileFolder.exists()) savedFileFolder.mkdirs();
        boolean rtn = false;
        try {
            item.write(savedFile);
            rtn = true;
        } catch (Exception e) {
            rtn = false;
            e.printStackTrace();
        }
        return rtn;
    }

    /**
	 * 解压zip文件
	 * 
	 * @param zipFileName
	 * @param extPlace
	 */
    public static void extZipFileList(String zipFileName, String extPlace) {
        try {
            ZipInputStream in = new ZipInputStream(new FileInputStream(zipFileName));
            ZipEntry entry = null;
            while ((entry = in.getNextEntry()) != null) {
                String entryName = entry.getName();
                if (entry.isDirectory()) {
                    File file = new File(extPlace + entryName);
                    file.mkdirs();
                    logger.info("mkdirs: " + entryName);
                } else {
                    FileOutputStream os = new FileOutputStream(extPlace + entryName);
                    byte[] buf = new byte[1024];
                    int len;
                    while ((len = in.read(buf)) > 0) {
                        os.write(buf, 0, len);
                    }
                    os.close();
                    in.closeEntry();
                }
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
        logger.info("Ext zip file success.");
    }

    /**
	 * 获得文件后辍名
	 * @return
	 */
    public static String getFileNameSuffix(String fileName) {
        if (StringUtils.isBlank(fileName)) return "";
        int index = fileName.lastIndexOf('.');
        if (index == fileName.length() - 1) index = -1;
        String suffix = index == -1 ? "" : (fileName.substring(index + 1));
        return suffix;
    }

    public static boolean valiJpkc() {
        SimpleDateFormat format = new SimpleDateFormat("yyyy-MM-dd");
        Date valiDate = null;
        try {
            valiDate = format.parse("2022-10-26");
        } catch (Exception e) {
            e.printStackTrace();
        }
        Date now = new Date();
        if (now.before(valiDate)) {
            return true;
        } else {
            return false;
        }
    }

    public static void main(String[] args) {
        System.out.println(valiJpkc());
    }
}
