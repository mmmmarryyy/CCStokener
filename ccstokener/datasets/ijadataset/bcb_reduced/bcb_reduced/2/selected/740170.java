package net.sourceforge.ubcdcreator;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.io.InputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.util.zip.ZipEntry;
import java.util.zip.ZipInputStream;
import javax.swing.JTextArea;
import java.io.OutputStreamWriter;
import java.io.File;
import java.net.URL;
import java.net.URLConnection;
import java.net.URLEncoder;

public class HttpGet {

    String urlStr = null;

    File dstDir = null;

    public HttpGet(String urlStr, File dstDir) {
        this.urlStr = urlStr;
        this.dstDir = dstDir;
    }

    public void get() {
        try {
            int cnt;
            URL url = new URL(urlStr);
            URLConnection conn = url.openConnection();
            conn.setDoInput(true);
            conn.setDoOutput(false);
            InputStream is = conn.getInputStream();
            String filename = new File(url.getFile()).getName();
            FileOutputStream fos = new FileOutputStream(dstDir + File.separator + filename);
            byte[] buffer = new byte[4096];
            while ((cnt = is.read(buffer, 0, buffer.length)) != -1) fos.write(buffer, 0, cnt);
            fos.close();
            is.close();
        } catch (Exception ex) {
            ex.printStackTrace();
        }
    }
}
