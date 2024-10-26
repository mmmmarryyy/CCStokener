package com.ideo.jso.junit;

import java.net.URL;
import java.net.URLConnection;
import java.util.Iterator;
import com.ideo.jso.conf.Group;

public class Util {

    public static long getFileTimeStamp(String fileClassPath, String mockWebUrl) throws Exception {
        if (fileClassPath == null) return 0;
        if (fileClassPath.startsWith("/")) {
            fileClassPath = fileClassPath.substring(1, fileClassPath.length());
        } else if (mockWebUrl != null && fileClassPath.startsWith(mockWebUrl)) {
            fileClassPath = fileClassPath.substring(mockWebUrl.length());
        }
        URL url = Thread.currentThread().getContextClassLoader().getResource(fileClassPath);
        URLConnection urlCnx = null;
        try {
            if (url == null) {
                return 0;
            }
            urlCnx = url.openConnection();
            return urlCnx.getLastModified();
        } finally {
            if (urlCnx != null && urlCnx.getInputStream() != null) urlCnx.getInputStream().close();
        }
    }

    public static long getJSFilesTimestamp(Group group) throws Exception {
        long maxtimestamp = 0;
        Iterator i = group.getJsNames().iterator();
        while (i.hasNext()) {
            String file = (String) i.next();
            maxtimestamp = Math.max(maxtimestamp, getFileTimeStamp(file, group.getLocation()));
        }
        return maxtimestamp;
    }

    public static long getCSSFilesTimestamp(Group group) throws Exception {
        long maxtimestamp = 0;
        Iterator i = group.getCssNames().iterator();
        while (i.hasNext()) {
            String file = (String) i.next();
            maxtimestamp = Math.max(maxtimestamp, getFileTimeStamp(file, group.getLocation()));
        }
        return maxtimestamp;
    }
}
