package test;

import java.io.BufferedInputStream;
import java.io.BufferedOutputStream;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.net.MalformedURLException;
import java.net.URL;

public class FileDownload {

    public static void downloadFile(String url) throws MalformedURLException, IOException {
        url = url.trim();
        BufferedInputStream in = new java.io.BufferedInputStream(new URL(url).openStream());
        createURLFolders(url);
        File f = new File(getFilePath(url));
        f.createNewFile();
        FileOutputStream fos = new java.io.FileOutputStream(f);
        BufferedOutputStream bout = new BufferedOutputStream(fos, 1024);
        byte data[] = new byte[1024];
        while (in.read(data, 0, 1024) >= 0) {
            bout.write(data);
        }
        bout.close();
        in.close();
    }

    public static void createURLFolders(String url) {
        System.out.println("url=" + url);
        String path = getPath(url);
        System.out.println(path);
        new File(path).mkdirs();
    }

    /** Returns file location with out the file name. */
    public static String getPath(String url) {
        url = url.replaceAll("http://", "");
        int lastIndexOf = url.lastIndexOf("/");
        String pageName = url.substring(lastIndexOf + 1, url.length());
        if (!pageName.equals("/")) {
            url = url.replace(pageName, "");
        }
        String path = "cache" + File.separatorChar;
        for (String p : url.split("/")) {
            path += p + File.separatorChar;
        }
        return path;
    }

    /** Returns the file path with the file name corresponding to the url. */
    public static String getFilePath(String url) {
        return getPath(url) + File.separatorChar + Math.abs(url.hashCode()) + ".html";
    }

    public static void main(String args[]) throws MalformedURLException, IOException {
        String url = "http://www.tomshardware.com/reviews/Components,1/CPU,1/";
        downloadFile(url);
    }
}
