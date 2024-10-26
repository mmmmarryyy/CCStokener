package net.sourceforge.juploader.upload.server;

import java.io.InputStream;
import java.net.HttpURLConnection;
import java.net.URL;
import java.util.Random;
import java.util.Scanner;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 *
 * @author Adam Pawelec
 */
public class Odsiebie extends BasicServer {

    public String[] uploadFile() {
        try {
            String srv = String.format("srv%d", new Random().nextInt(10) + 10);
            String id = getUploadId(srv);
            System.out.println(srv + " " + id);
        } catch (Exception ex) {
            Logger.getLogger(Odsiebie.class.getName()).log(Level.SEVERE, null, ex);
        } finally {
            return null;
        }
    }

    private String getUploadId(String srv) {
        String id = null;
        try {
            URL url = new URL("http://" + srv + ".odsiebie.com/link_upload.php");
            HttpURLConnection conn = (HttpURLConnection) url.openConnection();
            InputStream stream = conn.getInputStream();
            Scanner in = new Scanner(stream);
            in.findInLine("\"");
            in.useDelimiter("\"");
            id = in.next();
            conn.disconnect();
        } catch (Exception ex) {
            Logger.getLogger(Odsiebie.class.getName()).log(Level.SEVERE, null, ex);
        } finally {
            return id;
        }
    }

    public Servers getServer() {
        return Servers.ODSIEBIE;
    }

    public boolean isAcceptable(String fileName) {
        return true;
    }
}
