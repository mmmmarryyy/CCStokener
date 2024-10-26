package org.bing.engine.utility.helper;

import java.io.BufferedInputStream;
import java.io.BufferedOutputStream;
import java.io.Closeable;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.InetSocketAddress;
import java.net.Socket;
import java.net.URL;
import java.net.URLConnection;
import org.bing.engine.common.logging.Log;
import org.bing.engine.common.logging.LogFactory;

public class UrlHelper {

    private static final Log logger = LogFactory.getLog(UrlHelper.class);

    public static void call(String url) throws IOException {
        URLConnection conn = new URL(url).openConnection();
        conn.getInputStream();
    }

    public static boolean isAlive(String src, int tryNum, int interval) {
        Socket socket = null;
        for (int i = 0; i < tryNum; i++) {
            try {
                URL url = new URL(src);
                socket = new Socket();
                InetSocketAddress addr = new InetSocketAddress(url.getHost(), url.getPort());
                socket.connect(addr, 50);
                return true;
            } catch (IOException e) {
                ThreadHelper.sleep(interval);
            } finally {
                if (socket != null) {
                    try {
                        socket.close();
                    } catch (IOException e) {
                        socket = null;
                    }
                }
            }
        }
        return false;
    }

    /**
     * Read little content from url, max len is 4096
     */
    public static String read(String url, int maxLen) {
        maxLen = maxLen <= 0 ? 1024 : maxLen > 4096 ? 4096 : maxLen;
        StringBuilder sb = new StringBuilder(maxLen);
        URLConnection conn = null;
        try {
            conn = new URL(url).openConnection();
            int num = 0;
            byte[] buf = new byte[maxLen];
            InputStream ins = new BufferedInputStream(conn.getInputStream(), maxLen);
            while ((num = ins.read(buf)) != -1) {
                int rem = Math.min(maxLen - sb.length(), num);
                sb.append(new String(buf, 0, rem));
                if (sb.length() == maxLen) {
                    break;
                }
            }
            return sb.toString();
        } catch (Exception e) {
            logger.error("Fail to read url " + url + " - " + e);
            return null;
        }
    }

    public static void readToFile(String url, String file) {
        OutputStream fos = null;
        URLConnection conn = null;
        try {
            File f = new File(file);
            if (!f.getParentFile().exists()) {
                f.getParentFile().mkdirs();
            }
            fos = new BufferedOutputStream(new FileOutputStream(f), 32);
            conn = new URL(url).openConnection();
            InputStream ins = conn.getInputStream();
            int num = 0;
            byte[] buf = new byte[32 * 1024];
            while ((num = ins.read(buf)) != -1) {
                fos.write(buf, 0, num);
            }
        } catch (Exception e) {
            File f = new File(file);
            if (f.exists()) {
                f.delete();
            }
            throw new RuntimeException(e);
        } finally {
            doClose(fos);
        }
    }

    private static void doClose(Closeable cls) {
        if (cls != null) {
            try {
                cls.close();
            } catch (IOException e) {
                cls = null;
                throw new RuntimeException(e);
            }
        }
    }

    public static void main(String args[]) throws Exception {
        String url = "http://www.google.com/";
        UrlHelper.read(url, 32);
        System.out.println(UrlHelper.read(url, 32));
        String url2 = "http://localhost:8081/x-engine?cmd=shutdown";
        System.out.println(UrlHelper.isAlive(url2, 3, 100));
    }
}
