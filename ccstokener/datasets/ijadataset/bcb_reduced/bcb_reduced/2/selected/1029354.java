package model;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.net.HttpURLConnection;
import java.net.URL;
import java.net.UnknownHostException;
import java.nio.channels.Channels;
import java.nio.channels.ReadableByteChannel;

/**
 * 
 * 
 * @author Fadi Asbih
 * @email fadi_asbih@yahoo.de
 * @version 1.1.0  26/08/2011
 * @copyright 2011
 * 
 * 
 */
public class AutoUpdate {

    File f = new File(".");

    public static void downloadFile() throws IOException {
        URL google = new URL("http://ilias-userimport.googlecode.com/files/IUI_1.0.0.jar");
        ReadableByteChannel rbc = Channels.newChannel(google.openStream());
        FileOutputStream fos = new FileOutputStream("IUI_1.0.0.jar");
        fos.getChannel().transferFrom(rbc, 0, 1 << 24);
    }

    public static boolean isInternetReachable() {
        try {
            URL url = new URL("http://code.google.com/p/ilias-userimport/downloads/list");
            HttpURLConnection urlConnect = (HttpURLConnection) url.openConnection();
            Object objData = urlConnect.getContent();
        } catch (UnknownHostException e) {
            e.printStackTrace();
            return false;
        } catch (IOException e) {
            e.printStackTrace();
            return false;
        }
        return true;
    }

    public static void main(String[] args) throws IOException {
        downloadFile();
    }
}
