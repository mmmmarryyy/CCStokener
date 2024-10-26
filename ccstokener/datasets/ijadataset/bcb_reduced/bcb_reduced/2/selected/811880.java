package mars.mp3player.download;

import java.io.BufferedInputStream;
import java.io.BufferedOutputStream;
import java.io.BufferedReader;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.net.HttpURLConnection;
import java.net.MalformedURLException;
import java.net.URL;
import mars.mp3player.service.AppConstant;
import mars.mp3player.utils.FileUtils;
import android.util.Log;

public class HttpDownloader {

    public String download(String urlString) {
        StringBuilder builder = new StringBuilder();
        BufferedReader reader = null;
        try {
            URL url = new URL(urlString);
            HttpURLConnection urlConn = (HttpURLConnection) url.openConnection();
            reader = new BufferedReader(new InputStreamReader(urlConn.getInputStream()));
            String line = null;
            while ((line = reader.readLine()) != null) {
                builder.append(line);
            }
        } catch (MalformedURLException e) {
            Log.e("exception", e.getMessage());
        } catch (IOException e) {
            Log.e("exception", e.getMessage());
        } finally {
            try {
                reader.close();
            } catch (IOException e) {
                Log.e("exception", e.getMessage());
            }
        }
        return builder.toString();
    }

    public int downloadFile(String path, String fileName, String urlString) throws IOException {
        FileUtils fileUtils = new FileUtils();
        int flag = fileUtils.createSDCardFile(path, fileName);
        if (flag == AppConstant.DownloadVars.SUCCESS) {
            File file = new File(fileUtils.getSDPATH() + path + File.separator + fileName);
            URL url = new URL(urlString);
            HttpURLConnection urlConn = (HttpURLConnection) url.openConnection();
            if (urlConn.getResponseCode() != 404) {
                BufferedOutputStream outPut = new BufferedOutputStream(new FileOutputStream(file));
                BufferedInputStream inPut = new BufferedInputStream(urlConn.getInputStream());
                byte[] buffer = new byte[1024 * 1024];
                int hasRead = 0;
                while ((hasRead = inPut.read(buffer, 0, buffer.length)) != -1) {
                    outPut.write(buffer, 0, hasRead);
                    outPut.flush();
                }
                inPut.close();
                outPut.close();
            } else {
                return AppConstant.DownloadVars.NO_FILE;
            }
        }
        return flag;
    }
}
