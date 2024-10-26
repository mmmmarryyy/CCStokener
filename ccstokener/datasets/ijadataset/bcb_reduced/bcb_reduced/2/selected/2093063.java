package net.joindesk.util;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.io.UnsupportedEncodingException;
import java.net.URL;
import java.net.URLConnection;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.StringTokenizer;
import net.sf.json.util.JSONStringer;

public class Utils {

    private static SimpleDateFormat sf;

    private static String encoding;

    static {
        encoding = "UTF-8";
        sf = new SimpleDateFormat("yyyy-MM-dd");
    }

    public static String encode(String str) {
        if (str == null) return str;
        str = new JSONStringer().object().key("html").value(str).endObject().toString();
        str = str.substring(8);
        str = str.substring(0, str.length() - 1);
        return str;
    }

    public static void setDateFormat(String format) {
        sf = new SimpleDateFormat(format);
    }

    public static Date getDate(String str) throws ParseException {
        return sf.parse(str);
    }

    public static String getDateString(Date date) {
        return sf.format(date);
    }

    public static String checkDir(String dir, boolean isAutoCreate) {
        if (dir == null && dir.length() == 0) return dir;
        dir = trimLeft(trimRight(dir));
        if (File.separatorChar == '/') dir = replaceMarkAll(dir, "\\", "" + File.separatorChar); else dir = replaceMarkAll(dir, "/", "" + File.separatorChar);
        if (dir.charAt(dir.length() - 1) != File.separatorChar) dir = dir + File.separatorChar;
        if (isAutoCreate) mkDir(dir);
        return dir;
    }

    public static String getShortName(String filename) {
        int last = filename.lastIndexOf('\\');
        if (last != -1) filename = filename.substring(last + 1);
        last = filename.lastIndexOf('/');
        if (last != -1) filename = filename.substring(last + 1);
        return filename;
    }

    public static void mkDir(String dir) {
        if (dir == null && dir.length() == 0) return;
        String[] dirs = split(dir, File.separatorChar);
        if (dirs != null && dirs.length > 0) {
            String tempDir = "";
            for (int i = 0; i < dirs.length; i++) {
                tempDir = tempDir + File.separatorChar + dirs[i];
                File dirFile = new File(tempDir);
                if (dirFile.exists() == false) dirFile.mkdir();
            }
        }
    }

    public static String[] split(String source, String delim) {
        String[] wordLists;
        if (source == null) {
            wordLists = new String[1];
            wordLists[0] = source;
            return wordLists;
        }
        if (delim == null) {
            delim = ",";
        }
        StringTokenizer st = new StringTokenizer(source, delim);
        int total = st.countTokens();
        wordLists = new String[total];
        for (int i = 0; i < total; i++) {
            wordLists[i] = st.nextToken();
        }
        return wordLists;
    }

    public static String[] split(String source, char delim) {
        return split(source, String.valueOf(delim));
    }

    public static String[] split(String source) {
        return split(source, ",");
    }

    public static String trimLeft(String value) {
        String result = value;
        if (result == null) return result;
        char ch[] = result.toCharArray();
        int index = -1;
        for (int i = 0; i < ch.length; i++) {
            if (Character.isWhitespace(ch[i])) {
                index = i;
            } else {
                break;
            }
        }
        if (index != -1) {
            result = result.substring(index + 1);
        }
        return result;
    }

    public static String trimRight(String value) {
        String result = value;
        if (result == null) return result;
        char ch[] = result.toCharArray();
        int endIndex = -1;
        for (int i = ch.length - 1; i > -1; i--) {
            if (Character.isWhitespace(ch[i])) {
                endIndex = i;
            } else {
                break;
            }
        }
        if (endIndex != -1) {
            result = result.substring(0, endIndex);
        }
        return result;
    }

    public static String replaceMarkAll(String str, String destStr, String srcStr) {
        StringBuffer retVal = new StringBuffer();
        int findStation = str.indexOf(destStr);
        int resumStation = 0;
        while (findStation > -1) {
            String findStr = str.substring(resumStation, findStation);
            retVal.append(findStr);
            retVal.append(srcStr);
            resumStation = findStation + destStr.length();
            findStation = str.indexOf(destStr, resumStation);
        }
        retVal.append(str.substring(resumStation));
        return retVal.toString();
    }

    public static String read(String filename) throws IOException {
        FileInputStream fis = new FileInputStream(filename);
        try {
            InputStreamReader isr = new InputStreamReader(fis, encoding);
            String retStr = "";
            BufferedReader stdin = new BufferedReader(isr);
            String str = stdin.readLine();
            while (str != null) {
                retStr = retStr + str + "\n";
                str = stdin.readLine();
            }
            return retStr;
        } finally {
            fis.close();
        }
    }

    public static String read(File file) throws IOException {
        if (file == null) return null;
        FileInputStream fis = new FileInputStream(file);
        try {
            InputStreamReader isr = new InputStreamReader(fis, encoding);
            String ret = "";
            BufferedReader stdin = new BufferedReader(isr);
            String str = stdin.readLine();
            while (str != null) {
                ret = ret + str + "\n";
                str = stdin.readLine();
            }
            return ret;
        } finally {
            fis.close();
        }
    }

    public static String read(URL url) throws IOException {
        if (url == null) return null;
        InputStreamReader isr = new InputStreamReader(url.openStream(), encoding);
        try {
            String ret = "";
            BufferedReader stdin = new BufferedReader(isr);
            String str = stdin.readLine();
            while (str != null) {
                ret = ret + str + "\n";
                str = stdin.readLine();
            }
            return ret;
        } finally {
            isr.close();
        }
    }

    public static void write(URL url, String str) throws IOException {
        URLConnection uc = url.openConnection();
        uc.setDoOutput(true);
        OutputStream os = uc.getOutputStream();
        try {
            os.write(str.getBytes(encoding));
            os.flush();
        } finally {
            os.close();
        }
    }

    public static void write(String filename, String xml) throws IOException {
        FileOutputStream file = new FileOutputStream(filename);
        try {
            if (xml != null) file.write(xml.getBytes(encoding));
            file.flush();
        } finally {
            file.close();
        }
    }

    public static void write(File file, String xml) throws IOException {
        FileOutputStream fo = new FileOutputStream(file);
        try {
            if (xml != null) fo.write(xml.getBytes(encoding));
            fo.flush();
        } finally {
            fo.close();
        }
    }

    /**
	 * 将字符串src中的子字符串fnd全部替换为新子字符串rep.<br>
	 * 功能相当于java sdk 1.4的string.replaceall方法.<br>
	 * 不同之处在于查找时不是使用正则表达式而是普通字符串.
	 */
    public static String replaceall(String src, String fnd, String rep) {
        if (src == null || src.equals("")) {
            return "";
        }
        String dst = src;
        int idx = dst.indexOf(fnd);
        while (idx >= 0) {
            dst = dst.substring(0, idx) + rep + dst.substring(idx + fnd.length(), dst.length());
            idx = dst.indexOf(fnd, idx + rep.length());
        }
        return dst;
    }

    /**
	 * 转换为html编码.<br>
	 */
    public static String htmlencoder(String src) {
        if (src == null || src.equals("")) {
            return "";
        }
        String dst = src;
        dst = replaceall(dst, "<", "&lt;");
        dst = replaceall(dst, ">", "&rt;");
        dst = replaceall(dst, "\"", "&quot;");
        dst = replaceall(dst, "", "&#039;");
        return dst;
    }

    /**
	 * 转换为html文字编码.<br>
	 */
    public static String htmltextencoder(String src) {
        if (src == null || src.equals("")) {
            return "";
        }
        String dst = src;
        dst = replaceall(dst, "<", "&lt;");
        dst = replaceall(dst, ">", "&rt;");
        dst = replaceall(dst, "\"", "&quot;");
        dst = replaceall(dst, "", "&#039;");
        dst = replaceall(dst, " ", "&nbsp;");
        dst = replaceall(dst, "\r\n", "<br>");
        dst = replaceall(dst, "\r", "<br>");
        dst = replaceall(dst, "\n", "<br>");
        return dst;
    }

    /**
	 * 转换为url编码.<br>
	 * 
	 * @throws UnsupportedEncodingException
	 */
    public static String urlencoder(String src, String enc) throws UnsupportedEncodingException {
        return java.net.URLEncoder.encode(src, enc);
    }

    /**
	 * 转换为xml编码.<br>
	 */
    public static String xmlencoder(String src) {
        if (src == null || src.equals("")) {
            return "";
        }
        String dst = src;
        dst = replaceall(dst, "&", "&amp;");
        dst = replaceall(dst, "<", "&lt;");
        dst = replaceall(dst, ">", "&gt;");
        dst = replaceall(dst, "\"", "&quot;");
        dst = replaceall(dst, "\'", "&apos;");
        return dst;
    }

    /**
	 * 转换为sql编码.<br>
	 */
    public static String sqlencoder(String src) {
        if (src == null || src.equals("")) {
            return "";
        }
        return replaceall(src, "", "");
    }

    /**
	 * 转换为javascript编码.<br>
	 */
    public static String jsencoder(String src) {
        if (src == null || src.equals("")) {
            return "";
        }
        String dst = src;
        dst = replaceall(dst, "", "\\");
        dst = replaceall(dst, "\"", "\\\"");
        dst = replaceall(dst, "\n", "\\\n");
        dst = replaceall(dst, "\r", "\\\n");
        return dst;
    }
}
