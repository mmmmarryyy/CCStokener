package pl.softech.gpw.dao.ds;

import java.io.BufferedInputStream;
import java.io.BufferedOutputStream;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.net.HttpURLConnection;
import java.net.URL;
import java.util.logging.Level;
import java.util.logging.Logger;
import pl.softech.gpw.dao.FileDownloadMonitor;
import pl.softech.gpw.dao.FileDownloadMonitor.FileDownloadEvent;
import pl.softech.gpw.dao.FileDownloadMonitor.FileDownloadEvent.Type;

public class Utils {

    private static final Logger logger = Logger.getLogger(Utils.class.getName());

    public static InputStream getInputStream(RemoteBossaDataSource ds) throws IOException {
        HttpURLConnection http = (HttpURLConnection) ds.getUrl().openConnection();
        http.setRequestMethod("GET");
        HttpURLConnection.setFollowRedirects(true);
        http.setDoInput(true);
        http.setDoOutput(false);
        http.setUseCaches(false);
        http.setAllowUserInteraction(true);
        http.setInstanceFollowRedirects(true);
        http.setRequestProperty("Accept-Charset", "UTF-8");
        http.setRequestProperty("Host", ds.getHost());
        return http.getInputStream();
    }

    private static final String parseFileName(HttpURLConnection conn) {
        String tmp = conn.getHeaderField("content-disposition");
        if (tmp == null) {
            return null;
        }
        for (String e : tmp.split(";")) if (e.trim().startsWith("filename=")) return e.split("=")[1];
        return null;
    }

    public static File downloadFile(RemoteBossaDataSource ds, String dir, FileDownloadMonitor monitor) throws IOException {
        File file = null;
        BufferedOutputStream out = null;
        BufferedInputStream in = null;
        try {
            URL url = ds.getUrl();
            monitor.fireEvent(new FileDownloadEvent("Connecting..."));
            HttpURLConnection http = (HttpURLConnection) url.openConnection();
            http.setRequestProperty("Host", ds.getHost());
            String fileName = parseFileName(http);
            if (fileName == null) {
                fileName = new File(url.getFile()).getName();
            }
            file = new File(dir, fileName);
            out = new BufferedOutputStream(new FileOutputStream(file));
            http.connect();
            in = new BufferedInputStream(http.getInputStream());
            byte[] buff = new byte[1024];
            int len;
            long size = 0;
            try {
                size = Long.parseLong(http.getHeaderField("Content-Length"));
            } catch (Exception e) {
                logger.log(Level.WARNING, "Can't parse size of file: " + url, e);
            }
            monitor.fireEvent(new FileDownloadEvent("Start dowloading file: " + fileName, Type.START, size));
            while ((len = in.read(buff)) != -1) {
                out.write(buff, 0, len);
                monitor.fireEvent(new FileDownloadEvent(len));
            }
            out.flush();
            monitor.fireEvent(new FileDownloadEvent("Finished"));
            return file;
        } finally {
            if (out != null) out.close();
            if (in != null) in.close();
        }
    }
}
