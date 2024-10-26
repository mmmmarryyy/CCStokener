package slimewarrior;

import java.io.*;
import java.net.*;
import java.util.*;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 *
 * @author Dirk de Boer
 */
public class OnlineConnection implements Runnable {

    public void uploadconnection() {
        try {
            SimpleFTP ftp = new SimpleFTP();
            ftp.connect("ftp://ftp.drivehq.com/", 21, "stofkat", "rotspelikaan");
            ftp.bin();
            while (true) {
                try {
                    Thread.sleep(2000);
                } catch (InterruptedException ex) {
                    Logger.getLogger(OnlineConnection.class.getName()).log(Level.SEVERE, null, ex);
                }
                String playerscore = Integer.toString(Player.score);
                try {
                    BufferedWriter out = new BufferedWriter(new FileWriter("chatlog.txt", true));
                    out.write(Main.chattext);
                    out.newLine();
                    out.flush();
                    out.close();
                } catch (IOException e) {
                }
                download();
                ftp.stor(new File("chatlog.txt"));
            }
        } catch (IOException e) {
        }
    }

    public void download() {
        String nextLine;
        URL url = null;
        URLConnection urlConn = null;
        InputStreamReader inStream = null;
        BufferedReader buff = null;
        try {
            url = new URL("http://86.87.28.143/GIGASET_HTTP/chatlog.txt");
            urlConn = url.openConnection();
            inStream = new InputStreamReader(urlConn.getInputStream());
            buff = new BufferedReader(inStream);
            while (true) {
                nextLine = buff.readLine();
                if (nextLine != null) {
                    System.out.println(nextLine);
                    Main.chattext = nextLine;
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

    public void run() {
    }
}
