import java.io.*;
import java.net.SocketException;
import java.net.URL;
import java.net.URLConnection;
import java.net.UnknownHostException;
import java.util.Scanner;
import ch.ubique.inieditor.IniEditor;

public class Main {

    public static void main(String[] args) {
        IniEditor settings = new IniEditor();
        try {
            settings.load("settings.ini");
        } catch (IOException e) {
            e.printStackTrace();
        }
        final Rcon rcon = new Rcon(settings.get("server", "ip"), Integer.parseInt(settings.get("server", "port")), settings.get("server", "rcon"), getIP(), Integer.parseInt(settings.get("server", "myport")));
        final GatherBot bot = new GatherBot(settings, rcon);
        rcon.bot = bot;
        Thread test = new Thread() {

            public void run() {
                Scanner input = new Scanner(System.in);
                while (true) {
                    String cmd = input.next();
                    if (cmd.equals("startgather")) bot.startGather();
                    if (cmd.equals("endgather")) bot.endGather();
                    if (cmd.equals("upload")) bot.upload();
                    if (cmd.equals("ready")) bot.readyCheck();
                    if (cmd.equals("rcon")) try {
                        bot.rcon.connect();
                    } catch (SocketException e) {
                        e.printStackTrace();
                    } catch (UnknownHostException e) {
                        e.printStackTrace();
                    }
                }
            }
        };
        test.start();
    }

    static String getIP() {
        try {
            URL url = new URL("http://automation.whatismyip.com/n09230945.asp");
            final URLConnection conn = url.openConnection();
            conn.setDoOutput(true);
            BufferedReader rd = new BufferedReader(new InputStreamReader(conn.getInputStream()));
            return rd.readLine();
        } catch (IOException e) {
            e.printStackTrace();
            return null;
        }
    }
}
