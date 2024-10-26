package Model;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.net.MalformedURLException;
import java.net.URL;
import java.net.URLConnection;
import java.util.Date;
import java.util.StringTokenizer;
import javax.swing.JComponent;
import javax.swing.JFrame;

/**
 * @author Hipis
 */
public class Downloader {

    public static void createTempFolder(String dirName) {
        deleteDir(new File(dirName));
        new File(dirName).mkdir();
    }

    public static boolean deleteDir(File dir) {
        if (dir.isDirectory()) {
            String[] children = dir.list();
            for (int i = 0; i < children.length; i++) {
                boolean success = deleteDir(new File(dir, children[i]));
                if (!success) {
                    return false;
                }
            }
        }
        return dir.delete();
    }

    public static void downloadFile(String htmlUrl, String dirUrl) {
        try {
            URL url = new URL(htmlUrl);
            System.out.println("Opening connection to " + htmlUrl + "...");
            URLConnection urlC = url.openConnection();
            InputStream is = url.openStream();
            Date date = new Date(urlC.getLastModified());
            System.out.println(", modified on: " + date.toLocaleString() + ")...");
            System.out.flush();
            FileOutputStream fos = null;
            String localFile = null;
            StringTokenizer st = new StringTokenizer(url.getFile(), "/");
            while (st.hasMoreTokens()) localFile = st.nextToken();
            fos = new FileOutputStream(dirUrl + "/" + localFile);
            int oneChar, count = 0;
            while ((oneChar = is.read()) != -1) {
                fos.write(oneChar);
                count++;
            }
            is.close();
            fos.close();
            System.out.println(count + " byte(s) copied");
        } catch (MalformedURLException e) {
            System.err.println(e.toString());
        } catch (IOException e) {
            System.err.println(e.toString());
        }
    }
}
