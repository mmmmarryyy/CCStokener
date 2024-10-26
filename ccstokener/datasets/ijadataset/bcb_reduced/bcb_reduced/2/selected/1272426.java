package org.dict.server;

import java.io.*;
import java.net.URL;
import java.net.URLEncoder;
import java.util.Properties;

/**
 * @author duc
 * 
 */
public class NetUtils {

    static Properties MimeTypes;

    static String DEF_TYPE = "application/octet-stream";

    public static Properties getMimeTypes() {
        if (MimeTypes == null) {
            MimeTypes = new Properties();
            try {
                String root = System.getProperty("ROOT", ".");
                File f = new File(root, "mime.properties");
                InputStream fis = new FileInputStream(f);
                MimeTypes.load(fis);
                fis.close();
            } catch (Exception e) {
                MimeTypes.put("txt", "text/plain");
                MimeTypes.put("htm", "text/html");
                MimeTypes.put("html", "text/html");
                MimeTypes.put("jpg", "image/jpeg");
                MimeTypes.put("gif", "image/gif");
            }
        }
        return MimeTypes;
    }

    public static String getMimeType(String name) {
        String s = name;
        if (s == null) {
            return DEF_TYPE;
        }
        int idx = s.lastIndexOf('.');
        if (idx < 0 || idx >= s.length() - 1) {
            return DEF_TYPE;
        }
        String suf = s.substring(idx + 1).toLowerCase();
        return getMimeTypes().getProperty(suf, DEF_TYPE);
    }

    static String readTag(InputStream is) {
        StringBuffer sb = new StringBuffer("<");
        char c = '<';
        try {
            while ((c = (char) is.read()) != '>') {
                sb.append(c);
            }
        } catch (IOException e) {
        }
        sb.append(c);
        return sb.toString();
    }

    public static void saveChangeLink(URL url, OutputStream os) {
        try {
            BufferedInputStream is = new BufferedInputStream(url.openStream());
            int i;
            while ((i = is.read()) != -1) if ((char) i == '<') {
                String s = readTag(is);
                String s1 = convertTag(url, s);
                os.write(s1.getBytes());
            } else {
                os.write((byte) i);
            }
        } catch (Exception _ex) {
        }
    }

    static String convertTag(URL url, String s) {
        try {
            String s1 = s.toUpperCase();
            int i;
            boolean link = false;
            if (s1.startsWith("<BASE")) {
                return "";
            } else if (s1.startsWith("<A HREF")) {
                i = s1.indexOf("HREF");
                link = true;
            } else if (s1.startsWith("<FRAME ")) {
                i = s1.indexOf("SRC");
                link = true;
            } else if (s1.startsWith("<AREA")) {
                i = s1.indexOf("HREF");
                link = true;
            } else if (s1.startsWith("<IMG ")) {
                i = s1.indexOf("SRC");
            } else if (s1.startsWith("<APPLET ")) {
                i = s1.indexOf("CODEBASE");
            } else if (s1.startsWith("<A CLASS")) {
                i = s1.indexOf("HREF");
            } else {
                return s;
            }
            int j = s1.indexOf("=", i);
            if (j == -1) {
                return s;
            }
            int k;
            for (k = j + 1; isSpace(s1.charAt(k)); k++) ;
            if (s1.charAt(k) == '"') {
                k++;
            }
            int l;
            for (l = k + 1; !isSpace(s1.charAt(l)) && s1.charAt(l) != '"' && s1.charAt(l) != '>'; l++) ;
            String s2 = s.substring(k, l);
            if (s2.indexOf(":") == -1) {
                s2 = (new URL(url, s2)).toString();
            }
            if (link) {
                s = s1.substring(0, k) + "/redir?url=" + URLEncoder.encode(s2) + s1.substring(l, s1.length());
            } else {
                s = s1.substring(0, k) + s2 + s1.substring(l, s1.length());
            }
        } catch (Throwable t) {
        }
        return s;
    }

    static boolean isSpace(char c) {
        return Character.isSpace(c);
    }

    static boolean isRemoteTextLink(String s) {
        if (s.lastIndexOf(".htm") >= 0 || s.lastIndexOf(".txt") >= 0 || s.lastIndexOf(".shtml") >= 0) {
            return true;
        }
        if (s.endsWith("/")) {
            return true;
        }
        if (s.lastIndexOf("/") != -1) {
            String s1 = s.substring(s.lastIndexOf("/"));
            if (s1.indexOf("?") == -1) {
                return true;
            }
        }
        return false;
    }
}
