package slimewarrior;

import java.io.InputStreamReader;
import java.net.MalformedURLException;
import java.net.URL;
import java.net.URLConnection;
import java.io.BufferedReader;
import java.io.IOException;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 *
 * @author Dirk de Boer
 */
public class ChatReader implements Runnable {

    public void readData() {
        while (true) {
            try {
                Thread.sleep(1500);
            } catch (InterruptedException ex) {
                Logger.getLogger(ChatReader.class.getName()).log(Level.SEVERE, null, ex);
            }
            String nextLine;
            URL url = null;
            URLConnection urlConn = null;
            InputStreamReader inStream = null;
            BufferedReader buff = null;
            try {
                url = new URL("ftp://stofkat:rotspelikaan@ftp.drivehq.com/chatlog.txt");
                urlConn = url.openConnection();
                inStream = new InputStreamReader(urlConn.getInputStream());
                buff = new BufferedReader(inStream);
                while (true) {
                    nextLine = buff.readLine();
                    if (nextLine != null) {
                        System.out.println(nextLine);
                        Main.chatbox = nextLine;
                    } else {
                        break;
                    }
                }
            } catch (MalformedURLException e) {
                System.out.println("URL lijkt niet te kloppen:" + e.toString());
            } catch (IOException e1) {
                System.out.println("Je internet is kapot!: " + e1.toString());
            }
        }
    }

    public void run() {
        readData();
    }
}
