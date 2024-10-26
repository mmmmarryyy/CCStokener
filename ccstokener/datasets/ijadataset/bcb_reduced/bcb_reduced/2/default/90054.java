import java.io.BufferedInputStream;
import java.io.BufferedOutputStream;
import java.io.File;
import java.io.FileOutputStream;

public class FileDownloader {

    /**
     * downloads the file at url in local file destionation
     */
    public static boolean downloadFile(String url, String destination) throws Exception {
        BufferedInputStream bi = null;
        BufferedOutputStream bo = null;
        File destfile;
        java.net.URL fileurl;
        fileurl = new java.net.URL(url);
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
        bi.close();
        bo.close();
        return true;
    }
}
