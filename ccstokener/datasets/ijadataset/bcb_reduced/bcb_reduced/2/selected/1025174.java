package output;

import toplist.TopList;
import mp3.MP3;
import config.Tables;
import MYSQL.MysqlDb;
import config.HelpFiles;
import java.io.InputStream;
import java.net.URL;
import java.net.URLConnection;
import java.util.Random;
import java.util.Scanner;
import org.jivesoftware.smack.XMPPConnection;
import org.jivesoftware.smack.packet.Message;
import org.jivesoftware.smackx.muc.MultiUserChat;
import filetransfer.FileTransfer;
import java.util.Hashtable;

/**
 * This Class is responsible for the output for a requested pattern.
 *
 * @author stylesuxx
 */
public class Output {

    private MysqlDb sqldb;

    private MultiUserChat muc;

    private XMPPConnection conn;

    private Answer asw;

    private HelpFiles help;

    private boolean toplistSupport = false, mp3Support = false, fileSupport = false;

    private String pattern = "", body = "";

    private String from = "", JID = "", nick = "";

    private Message message;

    private FileTransfer ft;

    private TopList toplist;

    private MP3 mp3;

    private Tables tbl = new Tables();

    private String[] commands = tbl.returnValidCommands();

    public Output(MultiUserChat muc, MysqlDb sqldb, XMPPConnection conn, Answer asw, HelpFiles help) {
        this.conn = conn;
        this.sqldb = sqldb;
        this.muc = muc;
        this.asw = asw;
        this.help = help;
    }

    public void addToplistSupport() {
        this.toplist = new TopList(muc, tbl, help, sqldb, asw);
        toplistSupport = true;
    }

    public void addMp3Support(Hashtable config) {
        this.mp3 = new MP3(muc, tbl, sqldb, conn, asw, help, config);
        mp3Support = true;
    }

    public void addFileSupport(Hashtable config) {
        this.ft = new FileTransfer(muc, tbl, sqldb, conn, asw, help, config);
        fileSupport = true;
    }

    /**
     * Process given message body and username
     *
     * @param nick Nick who send the request.
     * @param body Message to process
     */
    public void processMessage(Message msg) {
        this.message = msg;
        this.body = message.getBody().toLowerCase();
        this.pattern = body.split("\\s+")[0].substring(1);
        this.from = message.getFrom();
        this.JID = muc.getOccupant(from).getJid().split("/")[0];
        this.nick = from.split("/")[1];
        boolean isValid = false;
        for (int i = 0; i < commands.length; i++) if (pattern.equals(commands[i])) isValid = true;
        if (isValid) makeString();
    }

    /**
     * Returns String to given pattern.
     *
     * @return Returns a String to given pattern.
     */
    private String makeString() {
        if (pattern.equals("file") && fileSupport) ft.processMessage(message);
        if (pattern.equals("mp3") && mp3Support) mp3.processMessage(message);
        if (pattern.equals("top") && toplistSupport) toplist.processMessage(message);
        if (pattern.equals("help")) asw.print(help.mainHelp(JID), false, from);
        if (pattern.equals("version")) asw.print("Version: 0.2.1-Alpha", false, from);
        if (pattern.equals("roll")) asw.print(nick + " hat gewürfelt: " + (new Random().nextInt(6) + 1), !message.getType().equals(message.getType().valueOf("chat")), from);
        if (pattern.equals("bier")) asw.print(nick + " gibt ne Runde Bier aus!", !message.getType().equals(message.getType().valueOf("chat")), from);
        if (pattern.equals("upupdowndownleftrightleftrightbabaselectstart")) asw.print(nick + " hat 90 Continues bekommen!", !message.getType().equals(message.getType().valueOf("chat")), from);
        if (pattern.equals("quote")) {
            String Output = "Random GBO Quote:";
            try {
                InputStream is = null;
                URL url = new URL("http://www.german-bash.org/action/random");
                URLConnection connection = url.openConnection();
                is = connection.getInputStream();
                String page = new Scanner(is).useDelimiter("\\Z").next();
                is.close();
                String[] test = page.split("<div class=\"zitat\">");
                String[] bam = test[1].split("</span>");
                for (int i = 0; i < (bam.length) - 1; i++) {
                    bam[i] = bam[i].replaceAll("<.*?>", "");
                    bam[i] = bam[i].replaceAll("&lt;", "<");
                    bam[i] = bam[i].replaceAll("&gt;", ">");
                    bam[i] = bam[i].replaceAll("&quot;", "\"");
                    bam[i] = bam[i].trim();
                    if (!bam[i].equals("")) Output = Output + "\n" + bam[i];
                }
            } catch (Exception ex) {
                System.out.println("URL fehlerhaft");
            }
            asw.print(Output, !message.getType().equals(message.getType().valueOf("chat")), from);
        }
        return "";
    }
}
