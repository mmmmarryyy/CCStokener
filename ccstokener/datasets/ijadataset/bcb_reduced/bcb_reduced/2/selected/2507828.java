package coopnetclient.utils;

import java.io.BufferedInputStream;
import java.io.BufferedOutputStream;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.net.MalformedURLException;

public class FileDownloader {

    /**
     * downloads the file at url in local file destionation
     */
    public static boolean downloadFile(String url, String destination) {
        BufferedInputStream bi = null;
        BufferedOutputStream bo = null;
        File destfile;
        try {
            java.net.URL fileurl;
            try {
                fileurl = new java.net.URL(url);
            } catch (MalformedURLException e) {
                return false;
            }
            bi = new BufferedInputStream(fileurl.openStream());
            destfile = new File(destination);
            if (!destfile.createNewFile()) {
                destfile.delete();
                destfile.createNewFile();
            }
            bo = new BufferedOutputStream(new FileOutputStream(destfile));
            int readedbyte;
            while ((readedbyte = bi.read()) != -1) {
                bo.write(readedbyte);
            }
            bo.flush();
        } catch (IOException ex) {
            return false;
        } finally {
            try {
                bi.close();
                bo.close();
            } catch (Exception ex) {
            }
        }
        return true;
    }
}
