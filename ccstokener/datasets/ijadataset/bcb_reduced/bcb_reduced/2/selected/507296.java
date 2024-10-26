package rdown;

import java.util.*;
import java.util.regex.*;
import java.io.*;
import java.net.*;
import javax.swing.*;

public class Downloader {

    public static void set(String var, String val) {
        vals.put(var, val);
    }

    public static String get(String var) {
        return (String) vals.get(var);
    }

    public static synchronized void start() {
        int maxsub = Integer.valueOf((String) vals.get("maxsub")).intValue();
        if (maxsub < 0 || maxsub > 256) {
            append("Bad maxsub value!\n");
            return;
        }
        File file = new File((String) get("dir"));
        if (!file.exists()) {
            append("Download directory doesn't exists!");
            return;
        }
        process((String) vals.get("url"), maxsub);
        append("Done.\n");
    }

    private static synchronized String fetch(String addr) {
        try {
            StringBuffer buf = new StringBuffer(1024 * 40);
            URL url = new URL(addr);
            URLConnection con = url.openConnection();
            long length = con.getContentLength();
            long downloaded = 0;
            int read;
            char[] buffer = new char[1024];
            InputStream uin = con.getInputStream();
            BufferedReader in = new BufferedReader(new InputStreamReader(uin));
            while ((read = in.read(buffer)) != -1) {
                downloaded += read;
                buf.append(buffer);
                if (bar != null) {
                    bar.setValue((int) (((double) downloaded / (double) length) * 100));
                }
            }
            if (bar != null) {
                bar.setValue(100);
            }
            return buf.toString();
        } catch (Exception e) {
            return null;
        }
    }

    private static void downloadFile(String addr) {
        try {
            String filename, ext, path;
            path = (String) vals.get("dir");
            filename = addr.substring(addr.lastIndexOf('/') + 1, addr.lastIndexOf('.'));
            ext = addr.substring(addr.lastIndexOf('.'));
            URL url = new URL(addr);
            URLConnection con = url.openConnection();
            long length = con.getContentLength();
            InputStream in = con.getInputStream();
            File file = new File(path + File.separator + filename + ext);
            if (file.length() == length) {
                in.close();
                return;
            }
            for (int x = 0; file.exists(); x++) {
                file = new File(path + File.separator + filename + String.valueOf(x + 1) + ext);
            }
            file.createNewFile();
            FileOutputStream out = new FileOutputStream(file);
            append("Downloading file " + file.toString() + "\n");
            int read;
            double percent;
            long count = 0;
            while ((read = in.read()) != -1) {
                if (bar != null && length != -1 && (count % 1024) == 0) {
                    count += read;
                    percent = (double) count / (double) length;
                    bar.setValue((int) (percent * 100));
                }
                out.write(read);
                count++;
            }
            if (bar != null) {
                bar.setValue(100);
            }
            out.close();
            in.close();
        } catch (Exception e) {
        }
    }

    private static URLBuffer getUrls(String addr, String text) {
        return new URLBuffer(addr, text);
    }

    private static void process(String address, int level) {
        append("Processing " + ((address.length() > 60) ? (address.substring(0, 60) + " ...") : address) + '\n');
        String content = fetch(address);
        if (content == null) {
            return;
        }
        URLBuffer urls = getUrls(address, content);
        String url;
        while ((url = urls.next()) != null) {
            if (isSearchedURL(url)) {
                downloadFile(url);
            } else if (isHTML(url)) {
                if (level > 0) {
                    process(url, level - 1);
                }
            }
        }
    }

    private static boolean isHTML(String url) {
        int dot = url.lastIndexOf('.');
        int slash = url.lastIndexOf('/');
        if (url.charAt(url.length() - 1) == '/' || dot == -1 || slash >= dot || url.substring(dot + 1).equalsIgnoreCase("html") || url.substring(dot + 1).equalsIgnoreCase("htm") || url.substring(dot + 1).equalsIgnoreCase("php")) {
            return true;
        }
        return false;
    }

    private static boolean isSearchedURL(String url) {
        StringTokenizer token = new StringTokenizer(get("ext"), ";");
        while (token.hasMoreTokens()) {
            String ext = token.nextToken();
            if (url.endsWith("." + ext)) {
                return true;
            }
        }
        return false;
    }

    private static void append(String buf) {
        if (log == null) {
            System.out.print(buf);
        } else {
            log.append(buf);
        }
    }

    private static HashMap vals = new HashMap();

    public static JTextArea log = null;

    public static JProgressBar bar = null;
}
