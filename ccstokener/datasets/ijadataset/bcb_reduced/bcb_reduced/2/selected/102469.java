package it.jwallpaper.util;

import it.jwallpaper.JWallpaperChanger;
import it.jwallpaper.platform.Platform;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.net.HttpURLConnection;
import java.net.URL;
import org.apache.commons.io.IOUtils;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

public class HttpUtils {

    private static final int DEFAULT_BUFFER_SIZE = 1024 * 4;

    private static Log logger = LogFactory.getLog(HttpUtils.class);

    public static void downloadImage(File file, String imageUrl) throws IOException {
        int size = 0;
        int copied = 0;
        InputStream in = null;
        FileOutputStream out = null;
        try {
            URL url;
            url = new URL(imageUrl);
            HttpURLConnection httpURLConnection = (HttpURLConnection) url.openConnection();
            httpURLConnection.setDoInput(true);
            httpURLConnection.setDoOutput(false);
            httpURLConnection.setUseCaches(false);
            httpURLConnection.setRequestMethod("GET");
            httpURLConnection.connect();
            size = httpURLConnection.getContentLength();
            in = httpURLConnection.getInputStream();
            out = new FileOutputStream(file);
            byte[] buffer = new byte[DEFAULT_BUFFER_SIZE];
            int n = 0;
            int percent = 0;
            int lastPercent = 0;
            while (-1 != (n = in.read(buffer))) {
                out.write(buffer, 0, n);
                copied += n;
                percent = copied * 100 / size;
                if (lastPercent != percent) {
                    lastPercent = percent;
                    String message = MessageUtils.getMessage(JWallpaperChanger.class, "downloadPercent", "" + percent + "%");
                    Platform.getPlatform().setTrayCaption(message);
                }
            }
            out.flush();
        } finally {
            Platform.getPlatform().setTrayCaption(null);
            IOUtils.closeQuietly(in);
            IOUtils.closeQuietly(out);
        }
    }
}
