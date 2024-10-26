package jcomicdownloader.tools;

import java.awt.Font;
import jcomicdownloader.table.*;
import java.util.logging.Level;
import java.util.logging.Logger;
import jcomicdownloader.encode.*;
import jcomicdownloader.module.*;
import jcomicdownloader.*;
import java.io.*;
import java.util.*;
import java.net.HttpURLConnection;
import java.net.MalformedURLException;
import java.net.URL;
import java.net.URLConnection;
import java.net.URLDecoder;
import java.net.URLEncoder;
import java.text.*;
import java.util.zip.*;
import javax.sound.sampled.AudioFormat;
import javax.sound.sampled.AudioInputStream;
import javax.sound.sampled.AudioSystem;
import javax.sound.sampled.DataLine;
import javax.sound.sampled.SourceDataLine;
import javax.swing.JOptionPane;
import javax.swing.UIManager;
import javax.swing.table.DefaultTableModel;
import jcomicdownloader.enums.*;

/**
 *
 * 大部分的通用方法都放在這邊，全宣告為靜態，方便使用。
 */
public class Common {

    public static String recordDirectory = getNowAbsolutePath();

    public static String tempDirectory = getNowAbsolutePath() + "temp" + getSlash();

    public static String downloadDirectory = getNowAbsolutePath() + "down" + getSlash();

    public static String tempVolumeFileName = "temp_volume.txt";

    public static String tempUrlFileName = "temp_url.txt";

    public static String tempVolumeInformationFileName = "temp_volume_information.txt";

    public static boolean isMainPage = false;

    public static int missionCount = 0;

    public static int bookmarkCount = 0;

    public static int recordCount = 0;

    public static boolean downloadLock = false;

    public static Thread downloadThread;

    public static boolean urlIsUnknown = false;

    public static String prevClipString;

    public static String consoleThreadName = "Thread-console-version";

    public static String setFileName = "set.ini";

    public static int reconnectionTimes = 3;

    public static String defaultAudioString = "使用預設音效";

    public static String defaultSingleDoneAudio = "single_done.wav";

    public static String defaultAllDoneAudio = "all_done.wav";

    public static String mainIcon = "main_icon.png";

    public static String playAudioPic = "play.png";

    public static String getZero() {
        int length = SetUp.getFileNameLength();
        String zero = "";
        for (int i = 0; i < length; i++) {
            zero += "0";
        }
        return zero;
    }

    public static String getZero(int zeroAmount) {
        String zero = "";
        for (int i = 0; i < zeroAmount; i++) {
            zero += "0";
        }
        return zero;
    }

    public static void errorReport(String errorString) {
        System.out.println(errorString);
        Run.isLegal = false;
    }

    public static void debugPrintln(String print) {
        if (Debug.debugMode) {
            System.out.println(print);
        }
    }

    public static void debugPrint(String print) {
        if (Debug.debugMode) {
            System.out.print(print);
        }
    }

    public static void processPrintln(String print) {
        System.out.println(print);
    }

    public static void processPrint(String print) {
        System.out.print(print);
    }

    public static void checkDirectory(String dir) {
        if (!new File(dir).exists()) {
            new File(dir).mkdirs();
        }
    }

    public static void downloadManyFile(String[] webSite, String outputDirectory, String picFrontName, String extensionName) {
        NumberFormat formatter = new DecimalFormat(Common.getZero());
        String[] pathStrings = outputDirectory.split("[\\\\]|/");
        String nowDownloadTitle = pathStrings[pathStrings.length - 2];
        String nowDownloadVolume = pathStrings[pathStrings.length - 1];
        String mainMessage = "下載 " + nowDownloadTitle + " / " + nowDownloadVolume + " ";
        for (int i = 1; i <= webSite.length && Run.isAlive; i++) {
            String[] tempStrings = webSite[i - 1].split("/|\\.");
            if (tempStrings[tempStrings.length - 1].length() == 3 || tempStrings[tempStrings.length - 1].length() == 4) {
                extensionName = tempStrings[tempStrings.length - 1];
            }
            String fileName = picFrontName + formatter.format(i) + "." + extensionName;
            String nextFileName = picFrontName + formatter.format(i + 1) + "." + extensionName;
            if (webSite[i - 1] != null) {
                if (!new File(outputDirectory + nextFileName).exists() || !new File(outputDirectory + fileName).exists()) {
                    CommonGUI.stateBarMainMessage = mainMessage;
                    CommonGUI.stateBarDetailMessage = "  :  " + "共" + webSite.length + "頁" + "，第" + i + "頁下載中";
                    if (Common.withGUI()) {
                        ComicDownGUI.trayIcon.setToolTip(CommonGUI.stateBarMainMessage + CommonGUI.stateBarDetailMessage);
                    }
                    CommonGUI.stateBarDetailMessage += " : " + fileName;
                    downloadFile(webSite[i - 1], outputDirectory, fileName, false, "", false, SetUp.getRetryTimes(), false, false);
                }
                System.out.print(i + " ");
            }
        }
    }

    public static void slowDownloadFile(String webSite, String outputDirectory, String outputFileName, int delayMillisecond, boolean needCookie, String cookieString) {
        try {
            Thread.currentThread().sleep(delayMillisecond);
        } catch (InterruptedException ex) {
            ex.printStackTrace();
        }
        downloadFile(webSite, outputDirectory, outputFileName, needCookie, cookieString, false, SetUp.getRetryTimes(), false, false);
    }

    public static String[] getCookieStringsTest(String urlString, String postString) {
        String[] tempCookieStrings = null;
        try {
            URL url = new URL(urlString);
            HttpURLConnection connection = (HttpURLConnection) url.openConnection();
            connection.setRequestProperty("User-Agent", "User-Agent: Mozilla/5.0 (Windows; U; Windows NT 6.1; zh-TW; rv:1.9.2.8) Gecko/20100722 Firefox/3.6.8");
            connection.setDoInput(true);
            connection.setDoOutput(true);
            connection.setRequestMethod("POST");
            connection.getOutputStream().write(postString.getBytes());
            connection.getOutputStream().flush();
            connection.getOutputStream().close();
            int code = connection.getResponseCode();
            System.out.println("code   " + code);
            tempCookieStrings = tryConnect(connection);
        } catch (Exception ex) {
            ex.printStackTrace();
        }
        String[] cookieStrings = tempCookieStrings;
        int cookieCount = 0;
        if (tempCookieStrings != null) {
            for (int i = 0; i < tempCookieStrings.length; i++) {
                if (tempCookieStrings[i] != null) {
                    cookieStrings[cookieCount++] = tempCookieStrings[i];
                    System.out.println(cookieCount + " " + tempCookieStrings[i]);
                }
            }
        }
        return cookieStrings;
    }

    public static String getCookieString(String urlString) {
        String[] cookies = getCookieStrings(urlString);
        String cookie = "";
        for (int i = 0; i < cookies.length && cookies[i] != null; i++) {
            cookie = cookies[i] + "; ";
        }
        return cookie;
    }

    public static String[] getCookieStrings(String urlString) {
        String[] tempCookieStrings = null;
        try {
            URL url = new URL(urlString);
            HttpURLConnection connection = (HttpURLConnection) url.openConnection();
            connection.setRequestProperty("User-Agent", "User-Agent: Mozilla/5.0 (Windows; U; Windows NT 6.1; zh-TW; rv:1.9.2.8) Gecko/20100722 Firefox/3.6.8");
            tempCookieStrings = tryConnect(connection);
        } catch (Exception ex) {
            ex.printStackTrace();
        }
        String[] cookieStrings = tempCookieStrings;
        int cookieCount = 0;
        if (tempCookieStrings != null) {
            for (int i = 0; i < tempCookieStrings.length; i++) {
                if (tempCookieStrings[i] != null) {
                    cookieStrings[cookieCount++] = tempCookieStrings[i];
                    System.out.println(cookieCount + ": " + tempCookieStrings[i]);
                }
            }
            Common.debugPrintln("共有" + cookieCount + "串cookie");
        }
        return cookieStrings;
    }

    public static void downloadFile(String webSite, String outputDirectory, String outputFileName, boolean needCookie, String cookieString) {
        downloadFile(webSite, outputDirectory, outputFileName, needCookie, cookieString, false, SetUp.getRetryTimes(), false, false);
    }

    public static void downloadGZIPInputStreamFile(String webSite, String outputDirectory, String outputFileName, boolean needCookie, String cookieString) {
        downloadFile(webSite, outputDirectory, outputFileName, needCookie, cookieString, false, SetUp.getRetryTimes(), true, false);
    }

    public static void downloadFileByForce(String webSite, String outputDirectory, String outputFileName, boolean needCookie, String cookieString) {
        downloadFile(webSite, outputDirectory, outputFileName, needCookie, cookieString, false, SetUp.getRetryTimes(), false, true);
    }

    public static void downloadFileFast(String webSite, String outputDirectory, String outputFileName, boolean needCookie, String cookieString) {
        downloadFile(webSite, outputDirectory, outputFileName, needCookie, cookieString, true, SetUp.getRetryTimes(), false, false);
    }

    public static void downloadFile(String webSite, String outputDirectory, String outputFileName, boolean needCookie, String cookieString, boolean fastMode, int retryTimes, boolean gzipEncode, boolean forceDownload) {
        int fileGotSize = 0;
        if (CommonGUI.stateBarDetailMessage == null) {
            CommonGUI.stateBarMainMessage = "下載網頁進行分析 : ";
            CommonGUI.stateBarDetailMessage = outputFileName + " ";
        }
        if (Run.isAlive || forceDownload) {
            try {
                ComicDownGUI.stateBar.setText(webSite + " 連線中...");
                URL url = new URL(webSite);
                HttpURLConnection connection = (HttpURLConnection) url.openConnection();
                connection.setRequestProperty("User-Agent", "Mozilla/4.0 (compatible; MSIE 6.0; Windows 2000)");
                connection.setFollowRedirects(true);
                connection.setDoInput(true);
                connection.setUseCaches(false);
                connection.setAllowUserInteraction(false);
                connection.setRequestProperty("Content-Type", "application/x-www-form-urlencoded");
                connection.setRequestProperty("Accept-Language", "zh-cn");
                connection.setRequestProperty("Cache-Control", "no-cache");
                connection.setRequestProperty("Pragma", "no-cache");
                connection.setRequestProperty("Host", "biz.finance.sina.com.cn");
                connection.setRequestProperty("Accept", "text/html, image/gif, image/jpeg, *; q=.2, */*; q=.2");
                connection.setRequestProperty("Connection", "keep-alive");
                connection.setConnectTimeout(10000);
                if (needCookie) {
                    connection.setRequestMethod("GET");
                    connection.setDoOutput(true);
                    connection.setRequestProperty("Cookie", cookieString);
                }
                int responseCode = 0;
                if ((fastMode && connection.getResponseCode() != 200) || (fastMode && connection.getContentLength() == 10771)) {
                    return;
                }
                Timer timer = new Timer();
                if (SetUp.getTimeoutTimer() > 0) {
                    timer.schedule(new TimeoutTask(), SetUp.getTimeoutTimer() * 1000);
                }
                tryConnect(connection);
                int fileSize = connection.getContentLength() / 1000;
                if (Common.isPicFileName(outputFileName) && (fileSize == 21 || fileSize == 22)) {
                    Common.debugPrintln("似乎連到盜連圖，停一秒後重新連線......");
                    try {
                        Thread.sleep(1000);
                    } catch (InterruptedException iex) {
                    }
                    tryConnect(connection);
                }
                if (connection.getResponseCode() != 200) {
                    Common.errorReport("錯誤回傳碼(responseCode): " + connection.getResponseCode() + " : " + webSite);
                    return;
                }
                Common.checkDirectory(outputDirectory);
                OutputStream os = new FileOutputStream(outputDirectory + outputFileName);
                InputStream is = null;
                if (gzipEncode && fileSize < 17) {
                    try {
                        is = new GZIPInputStream(connection.getInputStream());
                    } catch (IOException ex) {
                        is = connection.getInputStream();
                    }
                } else {
                    is = connection.getInputStream();
                }
                Common.debugPrint("(" + fileSize + " k) ");
                String fileSizeString = fileSize > 0 ? "" + fileSize : " ? ";
                byte[] r = new byte[1024];
                int len = 0;
                while ((len = is.read(r)) > 0 && (Run.isAlive || forceDownload)) {
                    if (fileSize > 1024 || !Flag.timeoutFlag) {
                        os.write(r, 0, len);
                    } else {
                        break;
                    }
                    fileGotSize += (len / 1000);
                    if (Common.withGUI()) {
                        int percent = 100;
                        String downloadText = "";
                        if (fileSize > 0) {
                            percent = (fileGotSize * 100) / fileSize;
                            downloadText = fileSizeString + "Kb ( " + percent + "% ) ";
                        } else {
                            downloadText = fileSizeString + " Kb ( " + fileGotSize + "Kb ) ";
                        }
                        ComicDownGUI.stateBar.setText(CommonGUI.stateBarMainMessage + CommonGUI.stateBarDetailMessage + " : " + downloadText);
                    }
                }
                is.close();
                os.flush();
                os.close();
                if (Common.withGUI()) {
                    ComicDownGUI.stateBar.setText(CommonGUI.stateBarMainMessage + CommonGUI.stateBarDetailMessage + " : " + fileSizeString + "Kb ( 100% ) ");
                }
                connection.disconnect();
                int realFileGotSize = (int) new File(outputDirectory + outputFileName).length() / 1000;
                if (realFileGotSize + 1 < fileGotSize && retryTimes > 0) {
                    String messageString = realFileGotSize + " < " + fileGotSize + " -> 等待兩秒後重新嘗試下載" + outputFileName + "（" + retryTimes + "/" + SetUp.getRetryTimes() + "）";
                    Common.debugPrintln(messageString);
                    ComicDownGUI.stateBar.setText(messageString);
                    Thread.sleep(2000);
                    downloadFile(webSite, outputDirectory, outputFileName, needCookie, cookieString, fastMode, retryTimes - 1, gzipEncode, false);
                }
                if (fileSize < 1024 && Flag.timeoutFlag) {
                    new File(outputDirectory + outputFileName).delete();
                    Common.debugPrintln("刪除不完整檔案：" + outputFileName);
                    ComicDownGUI.stateBar.setText("下載逾時，跳過" + outputFileName);
                }
                timer.cancel();
                Flag.timeoutFlag = false;
                Common.debugPrintln(webSite + " downloads successful!");
            } catch (Exception e) {
                e.printStackTrace();
            }
        }
    }

    public static boolean urlIsOK(String urlString) {
        boolean isOK = false;
        try {
            URL url = new URL(urlString);
            HttpURLConnection connection = (HttpURLConnection) url.openConnection();
            connection.setRequestProperty("User-Agent", "User-Agent: Mozilla/5.0 (Windows; U; Windows NT 6.1; zh-TW; rv:1.9.2.8) Gecko/20100722 Firefox/3.6.8");
            connection.connect();
            if (connection.getResponseCode() == HttpURLConnection.HTTP_OK) {
                isOK = true;
                Common.debugPrintln(urlString + " 測試連線結果: OK");
            } else {
                isOK = false;
                Common.debugPrintln(urlString + " 測試連線結果: 不OK ( " + connection.getResponseCode() + " )");
            }
            connection.disconnect();
        } catch (Exception e) {
            e.printStackTrace();
        }
        return isOK;
    }

    public static String[] tryConnect(HttpURLConnection connection) {
        return tryConnect(connection, null);
    }

    public static String[] tryConnect(HttpURLConnection connection, String postString) {
        String[] cookieStrings = new String[100];
        try {
            connection.connect();
            String headerName = "";
            for (int i = 1; (headerName = connection.getHeaderFieldKey(i)) != null; i++) {
                if (headerName.equals("Set-Cookie")) {
                    cookieStrings[i - 1] = new String(connection.getHeaderField(i));
                }
            }
        } catch (Exception ex) {
            try {
                if (connection.getResponseCode() != 200 && !Flag.timeoutFlag) {
                    try {
                        Thread.sleep(1000);
                    } catch (InterruptedException iex) {
                    }
                    Common.debugPrintln("重新嘗試連線......");
                    if (Common.withGUI()) {
                        ComicDownGUI.stateBar.setText("重新嘗試連線......");
                        connection.connect();
                    }
                }
            } catch (Exception exx) {
                exx.printStackTrace();
            }
        }
        return cookieStrings;
    }

    public static boolean isLegalURL(String webSite) {
        String regex = "^(https?|ftp|file)://[-a-zA-Z0-9+&@#/%?=~_|!:,.;]*[-a-zA-Z0-9+&@#/%=~_|]";
        if (webSite.matches(regex)) {
            return true;
        } else {
            return false;
        }
    }

    public static void compress(File source, File destination) {
        try {
            compress(source, destination, null, Deflater.NO_COMPRESSION);
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    public static void compress(File source, File destination, String comment, int level) throws IOException {
        ZipOutputStream zos = new ZipOutputStream(new FileOutputStream(destination));
        zos.setComment(comment);
        zos.setLevel(level);
        compress(zos, source.getParent(), source);
        zos.flush();
        zos.close();
    }

    private static void compress(ZipOutputStream zos, String rootpath, File source) throws IOException {
        if (source.isFile()) {
            ZipEntry zipEntry = new ZipEntry(source.getName());
            zos.putNextEntry(zipEntry);
            FileInputStream fis = new FileInputStream(source);
            byte[] buffer = new byte[1024];
            for (int length; (length = fis.read(buffer)) > 0; ) {
                zos.write(buffer, 0, length);
            }
            fis.close();
            zos.closeEntry();
        } else if (source.isDirectory()) {
            File[] files = source.listFiles();
            for (File file : files) {
                compress(zos, rootpath, file);
            }
        }
    }

    public static void deleteFolder(String folderPath) {
        Common.debugPrintln("刪除資料夾：" + folderPath);
        try {
            deleteAllFile(folderPath);
            String filePath = folderPath;
            filePath = filePath.toString();
            File myFilePath = new File(filePath);
            myFilePath.delete();
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    public static boolean deleteAllFile(String path) {
        boolean flag = false;
        File file = new File(path);
        if (!file.exists()) {
            return flag;
        }
        if (!file.isDirectory()) {
            return flag;
        }
        String[] tempList = file.list();
        File temp = null;
        for (int i = 0; i < tempList.length; i++) {
            if (path.endsWith(File.separator)) {
                temp = new File(path + tempList[i]);
            } else {
                temp = new File(path + File.separator + tempList[i]);
            }
            if (temp.isFile()) {
                temp.delete();
            }
            if (temp.isDirectory()) {
                deleteAllFile(path + "/" + tempList[i]);
                deleteFolder(path + "/" + tempList[i]);
                flag = true;
            }
        }
        return flag;
    }

    public static BufferedReader getBufferedReader(String filePath) throws IOException {
        FileReader fr = new FileReader(filePath);
        return new BufferedReader(fr);
    }

    public static void outputFile(String ouputText, String filePath, String fileName) {
        checkDirectory(filePath);
        try {
            FileOutputStream fout = new FileOutputStream(filePath + fileName);
            DataOutputStream dataout = new DataOutputStream(fout);
            byte[] data1 = ouputText.getBytes("UTF-8");
            dataout.write(data1);
            fout.close();
            Common.debugPrintln("寫出 " + filePath + fileName + " 檔案");
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    public static void outputFile(String[] outputStrings, String filePath, String fileName) {
        StringBuffer sb = new StringBuffer();
        for (int i = 0; i < outputStrings.length; i++) {
            sb.append(outputStrings[i] + "\n");
        }
        outputFile(sb.toString(), filePath, fileName);
    }

    public static void outputFile(List outputList, String filePath, String fileName) {
        StringBuffer sb = new StringBuffer();
        for (int i = 0; i < outputList.size(); i++) {
            sb.append(outputList.get(i) + "\n");
        }
        outputFile(sb.toString(), filePath, fileName);
    }

    public static void outputUrlFile(String[] urlStrings, String oldDownloadPath) {
        String[] dirStrings = oldDownloadPath.split("[\\\\]|/");
        String urlFileName = dirStrings[dirStrings.length - 1] + ".txt";
        String downloadPath = "";
        for (int i = 0; i < dirStrings.length - 1; i++) {
            downloadPath += dirStrings[i] + "/";
        }
        Common.processPrint("輸出位址文件檔: " + urlFileName);
        outputFile(urlStrings, downloadPath, urlFileName);
    }

    public static String getFileString(String filePath, String fileName) {
        String str = "";
        StringBuffer sb = new StringBuffer("");
        if (new File(filePath + fileName).exists()) {
            try {
                FileInputStream fileInputStream = new FileInputStream(filePath + fileName);
                InputStreamReader inputStreamReader = new InputStreamReader(fileInputStream, "UTF8");
                int ch = 0;
                while ((ch = inputStreamReader.read()) != -1) {
                    sb.append((char) ch);
                }
                fileInputStream.close();
            } catch (IOException e) {
                e.printStackTrace();
            }
        } else {
            Common.errorReport("沒有找到" + filePath + fileName + "此一檔案");
        }
        return sb.toString();
    }

    public static String[] getFileStrings(String filePath, String fileName) {
        String[] tempStrings = getFileString(filePath, fileName).split("\\n|\\r");
        return tempStrings;
    }

    public static String GBK2Unicode(String str) {
        StringBuffer result = new StringBuffer();
        for (int i = 0; i < str.length(); i++) {
            char chr1 = str.charAt(i);
            if (!isNeedConvert(chr1)) {
                result.append(chr1);
                continue;
            }
            result.append("&#x" + Integer.toHexString((int) chr1) + ";");
        }
        return result.toString();
    }

    public static boolean isNeedConvert(char para) {
        return ((para & (0x00FF)) != para);
    }

    public static String getTraditionalChinese(String gbString) {
        Zhcode mycode = new Zhcode();
        return mycode.convertString(gbString, mycode.GB2312, mycode.BIG5).replaceAll("[\\\\]ufffd", "_");
    }

    public static String getSimplifiedChinese(String gbString) {
        Zhcode mycode = new Zhcode();
        return mycode.convertString(gbString, mycode.GB2312, mycode.BIG5);
    }

    public static String getUtf8toUnicode(String utf8) {
        Zhcode mycode = new Zhcode();
        return mycode.convertString(utf8, mycode.UTF8, mycode.UNICODE);
    }

    public static String getUtf8toBig5(String utf8) {
        Zhcode mycode = new Zhcode();
        return mycode.convertString(utf8, mycode.UTF8, mycode.BIG5);
    }

    public static String getUtf8toGB2312(String utf8) {
        Zhcode mycode = new Zhcode();
        return mycode.convertString(utf8, mycode.UTF8, mycode.GB2312);
    }

    public static String getBig5toUtf8(String big5) {
        Zhcode mycode = new Zhcode();
        return mycode.convertString(big5, mycode.BIG5, mycode.UTF8);
    }

    public static String getGB2312toUtf8(String gb) {
        Zhcode mycode = new Zhcode();
        return mycode.convertString(gb, mycode.BIG5, mycode.UTF8);
    }

    public static void newEncodeFile(String directory, String fileName, String encodeFileName) {
        Zhcode mycode = new Zhcode();
        mycode.convertFile(directory + fileName, directory + encodeFileName, mycode.GB2312, mycode.UTF8);
    }

    public static void newEncodeFile(String directory, String fileName, String encodeFileName, int encode) {
        Zhcode mycode = new Zhcode();
        mycode.convertFile(directory + fileName, directory + encodeFileName, encode, mycode.UTF8);
    }

    public static void newEncodeBIG5File(String directory, String fileName, String encodeFileName) {
        Zhcode mycode = new Zhcode();
        mycode.convertFile(directory + fileName, directory + encodeFileName, mycode.BIG5, mycode.UTF8);
    }

    public static String getConnectStrings(String[] strings) {
        String str = "";
        for (int i = 0; i < strings.length; i++) {
            str += strings[i] + "####";
        }
        return str;
    }

    public static String[] getSeparateStrings(String connectString) {
        return connectString.split("####");
    }

    public static int getTrueCountFromStrings(String[] strings) {
        int count = 0;
        for (String str : strings) {
            if (str.equals("true")) {
                count++;
            }
        }
        return count;
    }

    public static String[] getCopiedStrings(String[] copiedStrings) {
        String[] newStrings = new String[copiedStrings.length];
        for (int i = 0; i < copiedStrings.length; i++) {
            newStrings[i] = copiedStrings[i];
        }
        return newStrings;
    }

    public static String getStringReplaceHttpCode(String oldString) {
        String string = oldString;
        string = string.replace("&#039;", "'");
        string = string.replace("&lt;", "＜");
        string = string.replace("&gt;", "＞");
        string = string.replace("&amp;", "&");
        string = string.replace("&nbsp;", "　");
        string = string.replace("&quot;", "''");
        return string;
    }

    public static String getStringRemovedIllegalChar(String oldString) {
        oldString = getStringReplaceHttpCode(oldString);
        String newString = "";
        for (int i = 0; i < oldString.length(); i++) {
            if (oldString.charAt(i) == '\\' || oldString.charAt(i) == '/' || oldString.charAt(i) == ':' || oldString.charAt(i) == '*' || oldString.charAt(i) == '?' || oldString.charAt(i) == '"' || oldString.charAt(i) == '<' || oldString.charAt(i) == '>' || oldString.charAt(i) == '|' || oldString.charAt(i) == '.') {
                newString += String.valueOf('_');
            } else {
                newString += String.valueOf(oldString.charAt(i));
            }
        }
        return Common.getReomvedUnnecessaryWord(newString);
    }

    public static String getReomvedUnnecessaryWord(String title) {
        if (title.matches("(?s).*九九漫畫")) {
            title = title.substring(0, title.length() - 4);
        } else if (title.matches("(?s).*手機漫畫")) {
            title = title.substring(0, title.length() - 4);
        } else if (title.matches("(?s).*第一漫畫")) {
            title = title.substring(0, title.length() - 4);
        } else if (title.matches("(?s).*漫畫")) {
            title = title.substring(0, title.length() - 2);
        }
        return title;
    }

    public static boolean withGUI() {
        if (Thread.currentThread().getName().equals(consoleThreadName)) {
            return false;
        } else {
            return true;
        }
    }

    public static String getStoredFileName(String outputDirectory, String defaultFileName, String defaultExtensionName) {
        int indexNameNo = 0;
        boolean over = false;
        while (over) {
            File tempFile = new File(outputDirectory + defaultFileName + indexNameNo + "." + defaultExtensionName);
            if (tempFile.exists() && (!tempFile.canRead() || !tempFile.canWrite())) {
                indexNameNo++;
            } else {
                over = true;
            }
        }
        return defaultFileName + indexNameNo + "." + defaultExtensionName;
    }

    public static String getAbsolutePath(String relativePath) {
        return new File(relativePath).getAbsolutePath();
    }

    public static boolean isWindows() {
        String os = System.getProperty("os.name").toLowerCase();
        return (os.indexOf("win") >= 0);
    }

    public static boolean isMac() {
        String os = System.getProperty("os.name").toLowerCase();
        return (os.indexOf("mac") >= 0);
    }

    public static boolean isUnix() {
        String os = System.getProperty("os.name").toLowerCase();
        return (os.indexOf("nix") >= 0 || os.indexOf("nux") >= 0);
    }

    public static String getSlash() {
        if (Common.isWindows()) {
            return "\\";
        } else {
            return "/";
        }
    }

    public static String getRegexSlash() {
        if (Common.isWindows()) {
            return "\\\\";
        } else {
            return "/";
        }
    }

    public static String getNowAbsolutePathOld() {
        return new File("").getAbsolutePath() + getSlash();
    }

    public static int getIndexOfOrderKeyword(String string, String keyword, int order) {
        int index = 0;
        for (int i = 0; i < order && index >= 0; i++) {
            index++;
            index = string.indexOf(keyword, index);
        }
        return index;
    }

    public static int getIndexOfOrderKeyword(String string, String keyword, int order, int beginIndex) {
        String newString = string.substring(beginIndex, string.length());
        int index = 0;
        for (int i = 0; i < order && index >= 0; i++) {
            index++;
            index = newString.indexOf(keyword, index);
        }
        return index;
    }

    public static int getSmallerIndexOfTwoKeyword(String string, int beginIndex, String keyword1, String keyword2) {
        int index1 = string.indexOf(keyword1, beginIndex);
        int index2 = string.indexOf(keyword2, beginIndex);
        if (index1 < 0) {
            return index2;
        } else if (index2 < 0) {
            return index1;
        } else {
            return index1 < index2 ? index1 : index2;
        }
    }

    public static int getBiggerIndexOfTwoKeyword(String string, String keyword1, String keyword2) {
        int index1 = string.lastIndexOf(keyword1);
        int index2 = string.lastIndexOf(keyword2);
        if (index1 < 0) {
            return index2;
        } else if (index2 < 0) {
            return index1;
        } else {
            return index1 > index2 ? index1 : index2;
        }
    }

    public static void outputDownTableFile(DownloadTableModel downTableModel) {
        StringBuffer sb = new StringBuffer();
        for (int row = 0; row < Common.missionCount; row++) {
            if (SetUp.getKeepUndoneDownloadMission()) {
                if (!downTableModel.getValueAt(row, DownTableEnum.STATE).toString().equals("下載完畢")) {
                    for (int col = 0; col < ComicDownGUI.getDownloadColumns().size(); col++) {
                        sb.append(downTableModel.getRealValueAt(row, col).toString());
                        sb.append("@@@@@@");
                    }
                    sb.append(ComicDownGUI.downTableUrlStrings[row]);
                    sb.append("%%%%%%");
                }
            }
            if (SetUp.getKeepDoneDownloadMission()) {
                if (downTableModel.getValueAt(row, DownTableEnum.STATE).toString().equals("下載完畢")) {
                    for (int col = 0; col < ComicDownGUI.getDownloadColumns().size(); col++) {
                        sb.append(downTableModel.getRealValueAt(row, col).toString());
                        sb.append("@@@@@@");
                    }
                    sb.append(ComicDownGUI.downTableUrlStrings[row]);
                    sb.append("%%%%%%");
                }
            }
        }
        sb.append("_OVER_");
        outputFile(sb.toString(), SetUp.getRecordFileDirectory(), "downloadList.dat");
    }

    public static void outputBookmarkTableFile(BookmarkTableModel bookmarkTableModel) {
        StringBuffer sb = new StringBuffer();
        for (int row = 0; row < Common.bookmarkCount; row++) {
            if (SetUp.getKeepBookmark()) {
                for (int col = 0; col < ComicDownGUI.getBookmarkColumns().size(); col++) {
                    sb.append(bookmarkTableModel.getValueAt(row, col).toString());
                    sb.append("@@@@@@");
                }
                sb.append(String.valueOf(bookmarkTableModel.getValueAt(row, RecordTableEnum.URL)));
                sb.append("%%%%%%");
            }
        }
        sb.append("_OVER_");
        outputFile(sb.toString(), SetUp.getRecordFileDirectory(), "bookmarkList.dat");
    }

    public static void outputRecordTableFile(RecordTableModel recordTableModel) {
        StringBuffer sb = new StringBuffer();
        for (int row = 0; row < Common.recordCount; row++) {
            if (SetUp.getKeepRecord()) {
                for (int col = 0; col < ComicDownGUI.getRecordColumns().size(); col++) {
                    sb.append(recordTableModel.getValueAt(row, col).toString());
                    sb.append("@@@@@@");
                }
                sb.append(recordTableModel.getValueAt(row, RecordTableEnum.URL).toString());
                sb.append("%%%%%%");
            }
        }
        sb.append("_OVER_");
        outputFile(sb.toString(), SetUp.getRecordFileDirectory(), "recordList.dat");
    }

    public static DownloadTableModel inputDownTableFile() {
        String dataString = getFileString(SetUp.getRecordFileDirectory(), "downloadList.dat");
        if (!dataString.matches("\\s*_OVER_\\s*")) {
            String[] rowStrings = dataString.split("%%%%%%");
            Common.debugPrint("將讀入下載任務數量: " + (rowStrings.length - 1));
            DownloadTableModel downTableModel = new DownloadTableModel(ComicDownGUI.getDownloadColumns(), rowStrings.length - 1);
            try {
                for (int row = 0; row < rowStrings.length - 1; row++) {
                    String[] colStrings = rowStrings[row].split("@@@@@@");
                    for (int col = 0; col < ComicDownGUI.getDownloadColumns().size(); col++) {
                        if (col == DownTableEnum.YES_OR_NO) {
                            downTableModel.setValueAt(Boolean.valueOf(colStrings[col]), row, col);
                        } else if (col == DownTableEnum.ORDER) {
                            downTableModel.setValueAt(new Integer(row + 1), row, col);
                        } else {
                            downTableModel.setValueAt(colStrings[col], row, col);
                        }
                    }
                    ComicDownGUI.downTableUrlStrings[row] = colStrings[ComicDownGUI.getDownloadColumns().size()];
                    Common.missionCount++;
                }
                Common.debugPrintln("   ... 讀入完畢!!");
            } catch (Exception ex) {
                Common.debugPrintln("   ... 讀入失敗!!");
                cleanDownTable();
                new File("downloadList.dat").delete();
            }
            return downTableModel;
        } else {
            return new DownloadTableModel(ComicDownGUI.getDownloadColumns(), 0);
        }
    }

    public static BookmarkTableModel inputBookmarkTableFile() {
        String dataString = getFileString(SetUp.getRecordFileDirectory(), "bookmarkList.dat");
        if (!dataString.matches("\\s*_OVER_\\s*")) {
            String[] rowStrings = dataString.split("%%%%%%");
            Common.debugPrint("將讀入書籤數量: " + (rowStrings.length - 1));
            BookmarkTableModel tableModel = new BookmarkTableModel(ComicDownGUI.getBookmarkColumns(), rowStrings.length - 1);
            try {
                for (int row = 0; row < rowStrings.length - 1; row++) {
                    String[] colStrings = rowStrings[row].split("@@@@@@");
                    for (int col = 0; col < ComicDownGUI.getBookmarkColumns().size(); col++) {
                        if (col == BookmarkTableEnum.ORDER) {
                            tableModel.setValueAt(new Integer(row + 1), row, col);
                        } else {
                            tableModel.setValueAt(colStrings[col], row, col);
                        }
                    }
                    Common.bookmarkCount++;
                }
                Common.debugPrintln("   ... 讀入完畢!!");
            } catch (Exception ex) {
                Common.debugPrintln("   ... 讀入失敗!!");
            }
            return tableModel;
        } else {
            return new BookmarkTableModel(ComicDownGUI.getBookmarkColumns(), 0);
        }
    }

    public static RecordTableModel inputRecordTableFile() {
        String dataString = getFileString(SetUp.getRecordFileDirectory(), "recordList.dat");
        if (!dataString.matches("\\s*_OVER_\\s*")) {
            String[] rowStrings = dataString.split("%%%%%%");
            Common.debugPrint("將讀入記錄數量: " + (rowStrings.length - 1));
            RecordTableModel tableModel = new RecordTableModel(ComicDownGUI.getRecordColumns(), rowStrings.length - 1);
            try {
                for (int row = 0; row < rowStrings.length - 1; row++) {
                    String[] colStrings = rowStrings[row].split("@@@@@@");
                    for (int col = 0; col < ComicDownGUI.getRecordColumns().size(); col++) {
                        if (col == RecordTableEnum.ORDER) {
                            tableModel.setValueAt(new Integer(row + 1), row, col);
                        } else {
                            tableModel.setValueAt(colStrings[col], row, col);
                        }
                    }
                    Common.recordCount++;
                }
                Common.debugPrintln("   ... 讀入完畢!!");
            } catch (Exception ex) {
                Common.debugPrintln("   ... 讀入失敗!!");
            }
            return tableModel;
        } else {
            return new RecordTableModel(ComicDownGUI.getRecordColumns(), 0);
        }
    }

    public static void deleteFile(String filePath, String fileName) {
        File file = new File(filePath + fileName);
        if (file.exists() && file.isFile()) {
            Common.debugPrintln("刪除暫存檔案：" + fileName);
            file.delete();
        }
    }

    public static void deleteFile(String fileName) {
        File file = new File(fileName);
        if (file.exists() && file.isFile()) {
            Common.debugPrintln("刪除暫存檔案：" + fileName);
            file.delete();
        }
    }

    public static void setHttpProxy(String proxyServer, String proxyPort) {
        Properties systemProperties = System.getProperties();
        systemProperties.setProperty("http.proxyHost", proxyServer);
        systemProperties.setProperty("http.proxyPort", proxyPort);
    }

    public static void closeHttpProxy() {
        Properties systemProperties = System.getProperties();
        systemProperties.setProperty("proxySet", "false");
    }

    public static boolean isPicFileName(String fileName) {
        if (fileName.matches("(?s).*\\.jpg") || fileName.matches("(?s).*\\.JPG") || fileName.matches("(?s).*\\.png") || fileName.matches("(?s).*\\.PNG") || fileName.matches("(?s).*\\.gif") || fileName.matches("(?s).*\\.GIF") || fileName.matches("(?s).*\\.jpeg") || fileName.matches("(?s).*\\.JPEG") || fileName.matches("(?s).*\\.bmp") || fileName.matches("(?s).*\\.BMP")) {
            return true;
        } else {
            return false;
        }
    }

    public static boolean existPicFile(String directory, int p) {
        NumberFormat formatter = new DecimalFormat(Common.getZero());
        String fileName = formatter.format(p);
        if (new File(directory + fileName + ".jpg").exists() || new File(directory + fileName + ".JPG").exists() || new File(directory + fileName + ".png").exists() || new File(directory + fileName + ".PNG").exists() || new File(directory + fileName + ".gif").exists() || new File(directory + fileName + ".GIF").exists() || new File(directory + fileName + ".jpeg").exists() || new File(directory + fileName + ".JPEG").exists() || new File(directory + fileName + ".bmp").exists() || new File(directory + fileName + ".BMP").exists()) {
            return true;
        } else {
            return false;
        }
    }

    public static void cleanDownTable() {
        DefaultTableModel table = ComicDownGUI.downTableModel;
        if (table != null) {
            int downListCount = table.getRowCount();
            while (table.getRowCount() > 1) {
                table.removeRow(table.getRowCount() - 1);
                Common.missionCount--;
            }
            if (Common.missionCount > 0) {
                table.removeRow(0);
            }
            ComicDownGUI.mainFrame.repaint();
            Common.missionCount = 0;
            Common.processPrintln("因讀入錯誤，將全部任務清空");
            ComicDownGUI.stateBar.setText("下載任務檔格式錯誤，無法讀取!!");
        }
    }

    public static String getHtmlStringWithColor(String string, String color) {
        return "<html><font color=" + color + string + "</font></html>";
    }

    public static int getAmountOfString(String aString, String bString) {
        int bLength = bString.length();
        int conformTimes = 0;
        for (int i = 0; i < aString.length(); i += bLength) {
            if (aString.substring(i, i + bLength).equals(bString)) {
                conformTimes++;
            }
        }
        return conformTimes;
    }

    public static String getNowAbsolutePath() {
        if (Common.isUnix()) {
            String apath = Common.class.getProtectionDomain().getCodeSource().getLocation().getPath();
            try {
                apath = URLDecoder.decode(apath, "UTF-8");
            } catch (UnsupportedEncodingException ex) {
                Logger.getLogger(Common.class.getName()).log(Level.SEVERE, null, ex);
            }
            String absolutePath;
            if (apath.endsWith(".jar")) {
                absolutePath = apath.replaceAll("([^/\\\\]+).jar$", "");
            } else {
                absolutePath = new File("").getAbsolutePath() + Common.getSlash();
            }
            return absolutePath;
        } else {
            return new File("").getAbsolutePath() + getSlash();
        }
    }

    public static void playSingleDoneAudio() {
        playSingleDoneAudio(SetUp.getSingleDoneAudioFile());
    }

    public static void playSingleDoneAudio(String fileString) {
        if (new File(fileString).exists()) {
            playAudio(fileString, false);
        } else {
            playAudio(Common.defaultSingleDoneAudio, true);
        }
    }

    public static void playAllDoneAudio() {
        playAllDoneAudio(SetUp.getAllDoneAudioFile());
    }

    public static void playAllDoneAudio(String fileString) {
        if (new File(fileString).exists()) {
            playAudio(fileString, false);
        } else {
            playAudio(Common.defaultAllDoneAudio, true);
        }
    }

    private static void playAudio(final String audioFileString, final boolean defaultResource) {
        Thread playThread = new Thread(new Runnable() {

            public void run() {
                try {
                    AudioInputStream ais;
                    if (defaultResource) {
                        URL audioFileURL = new CommonGUI().getResourceURL(audioFileString);
                        ais = AudioSystem.getAudioInputStream(audioFileURL);
                    } else {
                        File audioFile = new File(audioFileString);
                        ais = AudioSystem.getAudioInputStream(audioFile);
                    }
                    AudioFormat af = ais.getFormat();
                    DataLine.Info inf = new DataLine.Info(SourceDataLine.class, af);
                    SourceDataLine sdl = (SourceDataLine) AudioSystem.getLine(inf);
                    sdl.open(af);
                    sdl.start();
                    byte[] buf = new byte[65536];
                    for (int n = 0; (n = ais.read(buf, 0, buf.length)) > 0; ) {
                        sdl.write(buf, 0, n);
                    }
                    sdl.drain();
                    sdl.close();
                } catch (Exception e) {
                    e.printStackTrace();
                }
            }
        });
        playThread.start();
    }

    public static String getFixedChineseURL(String url) {
        try {
            String temp = "";
            for (int k = 0; k < url.length(); k++) {
                if (url.substring(k, k + 1).matches("(?s).*[-￿]+(?s).*")) {
                    temp += URLEncoder.encode(url.substring(k, k + 1), "UTF-8");
                } else {
                    temp += url.substring(k, k + 1);
                }
            }
            url = temp;
        } catch (Exception e) {
            e.printStackTrace();
        }
        url = url.replaceAll("\\s", "%20");
        return url;
    }

    public static void runCmd(String program, String file) {
        String path = file;
        String cmd = program;
        if (!new File(file).exists()) {
            String nowSkinName = UIManager.getLookAndFeel().getName();
            String colorString = "blue";
            if (nowSkinName.equals("HiFi") || nowSkinName.equals("Noire")) {
                colorString = "yellow";
            }
            JOptionPane.showMessageDialog(ComicDownGUI.mainFrame, "<html><font color=" + colorString + ">" + file + "</font>" + "不存在，無法開啟</html>", "提醒訊息", JOptionPane.INFORMATION_MESSAGE);
            return;
        }
        String[] fileList = new File(file).list();
        System.out.println(file);
        String firstZipFileName = "";
        boolean existZipFile = false;
        for (int i = 0; i < fileList.length; i++) {
            System.out.println("FILE: " + fileList[i]);
            if (fileList[i].matches("(?s).*\\.zip")) {
                firstZipFileName = fileList[i];
                existZipFile = true;
                break;
            }
        }
        if (existZipFile) {
            path = file + Common.getSlash() + firstZipFileName;
        } else {
            String[] picList = new File(file + Common.getSlash() + fileList[0]).list();
            String firstPicFileInFirstVolume = picList[0];
            path = file + Common.getSlash() + fileList[0] + Common.getSlash() + firstPicFileInFirstVolume;
        }
        Common.debugPrintln("開啟命令：" + cmd + path);
        try {
            String[] cmds = new String[] { cmd, path };
            Runtime.getRuntime().exec(cmds, null, new File(Common.getNowAbsolutePath()));
        } catch (IOException ex) {
            Logger.getLogger(ComicDownGUI.class.getName()).log(Level.SEVERE, null, ex);
        }
    }

    public static void runUnansiCmd(String program, String file) {
        if (!new File(file).exists()) {
            String nowSkinName = UIManager.getLookAndFeel().getName();
            String colorString = "blue";
            if (nowSkinName.equals("HiFi") || nowSkinName.equals("Noire")) {
                colorString = "yellow";
            }
            JOptionPane.showMessageDialog(ComicDownGUI.mainFrame, "<html><font color=" + colorString + ">" + file + "</font>" + "不存在，無法開啟</html>", "提醒訊息", JOptionPane.INFORMATION_MESSAGE);
            return;
        }
        String[] cmd = new String[] { program, file };
        Map<String, String> newEnv = new HashMap<String, String>();
        newEnv.putAll(System.getenv());
        String[] i18n = new String[cmd.length + 2];
        i18n[0] = "cmd";
        i18n[1] = "/C";
        i18n[2] = cmd[0];
        for (int counter = 1; counter < cmd.length; counter++) {
            String envName = "JENV_" + counter;
            i18n[counter + 2] = "%" + envName + "%";
            newEnv.put(envName, cmd[counter]);
        }
        cmd = i18n;
        ProcessBuilder pb = new ProcessBuilder(cmd);
        Map<String, String> env = pb.environment();
        env.putAll(newEnv);
        try {
            final Process p = pb.start();
        } catch (IOException ex) {
            Logger.getLogger(ComicDownGUI.class.getName()).log(Level.SEVERE, null, ex);
        }
    }
}

class TimeoutTask extends TimerTask {

    public void run() {
        Common.debugPrintln("超過下載時限，終止此次連線!");
        Flag.timeoutFlag = true;
    }
}
