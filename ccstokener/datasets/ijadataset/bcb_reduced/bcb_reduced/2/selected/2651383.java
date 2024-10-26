package net.sf.downloadr;

import java.io.BufferedInputStream;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.net.URL;
import java.net.URLConnection;
import net.sf.downloadr.flickr.Photo;

public class PhotoDownloader {

    public StringBuffer downloadOriginalPhoto(Photo photo, String targetFilename) throws IOException {
        return downloadUrl(photo.getOriginalPhotoUrl(), targetFilename);
    }

    StringBuffer downloadUrl(URL url, String filename) throws IOException {
        StringBuffer buffer = new StringBuffer();
        long start = System.currentTimeMillis();
        System.out.print("Getting " + url.toString() + " => " + filename + "...");
        buffer.append("Getting " + url.toString() + " => " + filename + "...");
        URLConnection connection = url.openConnection();
        connection.connect();
        BufferedInputStream inStream = null;
        FileOutputStream fos = null;
        try {
            inStream = new BufferedInputStream(connection.getInputStream());
            File newFile = new File(filename);
            fos = new FileOutputStream(newFile);
            int read;
            while ((read = inStream.read()) != -1) {
                fos.write(read);
            }
        } catch (IOException e) {
            e.printStackTrace();
        } finally {
            if (fos != null) {
                fos.flush();
                fos.close();
            }
            if (inStream != null) {
                inStream.close();
            }
        }
        String elapsedTime = new ElapsedTimeCalculator().calculateElapsedTime(start, System.currentTimeMillis());
        System.out.println(elapsedTime);
        buffer.append(elapsedTime + "\n");
        return buffer;
    }
}
