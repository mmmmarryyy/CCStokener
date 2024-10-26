package ch.HaagWeirich.Agenda;

import java.io.*;
import java.net.URL;
import java.net.URLConnection;
import java.net.UnknownHostException;
import java.sql.ResultSet;
import java.util.Enumeration;
import java.util.Hashtable;
import java.util.jar.Attributes;
import java.util.jar.JarFile;
import java.util.jar.Manifest;
import javax.swing.JLabel;
import ch.rgw.IO.FileTool;
import ch.rgw.net.Downloader;
import ch.rgw.swingtools.PlainTextField;
import ch.rgw.swingtools.SwingHelper;
import ch.rgw.tools.*;
import ch.rgw.tools.JdbcLink.Stm;

/**
 * Automatisch nach updates suchen und ggf. downloaden
 */
public class AutoUpdate {

    static final String Version = "2.1.7";

    private static final String remoteURL = "http://www.haag-weirich.ch";

    private static final String updateURL = remoteURL + "/cgi-bin/getagendaversion.pl?v=35";

    private static final String downloadURL = remoteURL + "/agenda/prog/";

    private static final String filenameMask = "JavaAgenda-[0-9]{8,8}\\.jar";

    private static Log log;

    public static void WebUpdate() {
        log = Log.get("AutoUpdate");
        Log.setAlert(Agenda.client);
        String basedir = FileTool.getBasePath();
        log.log("Pr�fe Verzeichnis " + basedir, Log.INFOS);
        File ldir = new File(basedir);
        String[] files = ldir.list();
        int n = -1;
        if (files != null) {
            for (int i = 0; i < files.length; i++) {
                if (files[i].matches(filenameMask)) {
                    if ((n == -1) || (files[i].compareTo(files[n]) > 0)) {
                        n = i;
                    }
                }
            }
        }
        if (n == -1) {
            log.log("Keine Agenda-Jar gefunden! ", Log.ERRORS);
            Log.setAlert(null);
            return;
        }
        String local = files[n];
        log.log("Lokale Version: " + local, Log.DEBUGMSG);
        try {
            URL url = new URL(updateURL);
            URLConnection conn = url.openConnection();
            InputStream cont = (InputStream) conn.getContent();
            BufferedReader br = new BufferedReader(new InputStreamReader(cont));
            String remote = br.readLine();
            log.log("Remote Version: " + remote, Log.DEBUGMSG);
            if (local.compareTo(remote) < 0) {
                PlainTextField plf = new PlainTextField("Es wurde eine neuere Version gefunden. Soll diese Version heruntergeladen werden? (Sie wird erst beim n�chsten Start aktiv)", true);
                if (SwingHelper.showConfirm("Neuere Version gefunden", plf, Agenda.client, SwingHelper.YESNO) == true) {
                    File fNew = new File(basedir + "upd_" + remote);
                    Downloader dl = new Downloader();
                    Agenda.setWaitCursor(true);
                    if (dl.download(downloadURL + remote, fNew, Agenda.client, true) != Downloader.OK) {
                        fNew.delete();
                        Agenda.setWaitCursor(false);
                        SwingHelper.alert(Agenda.client, "Abbruch", "Der Download wurde abgebrochen");
                        return;
                    }
                    Agenda.setWaitCursor(false);
                    File fDef = new File(basedir + remote);
                    JarFile jf = new JarFile(fNew);
                    Manifest mf = jf.getManifest();
                    Attributes att = mf.getMainAttributes();
                    String v = att.getValue("Implementation-Version");
                    jf.close();
                    String[] vs = v.split(" +");
                    VersionInfo vi = new VersionInfo(vs[0]);
                    VersionInfo vMine = new VersionInfo(Agenda.Version);
                    String question = "";
                    if (vi.maior().compareTo(vMine.maior()) > 0) {
                        question = "<html><p>Die heruntergeladene Version enth�lt grundlegende Neuerungen.<br>" + "Es wird empfohlen, die Installation manuell durchzuf�hren. Vorher sollte unbedingt die Datenbank gesichert werden.</p><br>+" + "Soll das update trotzdem jetzt automatisch installiert werden?</html>";
                    } else if (vi.minor().compareTo(vMine.minor()) > 0) {
                        question = "<html><p>Die heruntergeladene Version enth�lt gr�ssere �nderungen. Es wird empfohlen:</p>" + "<ul><li>Die Installation nur dann durchzuf�hren, wenn keine anderen Klienten aktiv sind</li>" + "<li>Vor der Installation eine Sicherung der Datenbank durchzuf�hren</li></ul><br>" + "Soll das update jetzt installiert werden?</html>";
                    } else {
                        question = "<html><p>Das Update wurde erfolgreich heruntergeladen. Es sollte sich problemlos " + "automatisch installieren lassen<br>Update jetzt installieren?</html>";
                    }
                    if (SwingHelper.showConfirm("Installation durchf�hren?", new JLabel(question), Agenda.client, SwingHelper.YESNO) == true) {
                        if (fNew.renameTo(fDef) == true) {
                            SwingHelper.alert(Agenda.client, "Update erfolgreich", "Das update wurde heruntergeladen. Es wird beim n�chsten Programmstart aktiv.");
                        } else {
                            SwingHelper.alert(Agenda.client, "Fehler beim Umbenennen", "Das Update konnte nicht installiert werden. Es liegt unter " + fNew.getAbsolutePath());
                        }
                    } else {
                        SwingHelper.alert(Agenda.client, "Update nicht durchgef�hrt", "Das Update wurde nich durchgef�hrt. Die heruntergeladene Datei liegt unter " + fNew.getAbsolutePath());
                    }
                }
            } else {
                SwingHelper.alert(Agenda.client, "Kein update", "Es wurde keine neuere Version gefunden");
            }
        } catch (UnknownHostException ex) {
            log.log("Update Site nicht erreichbar.", Log.ERRORS);
        } catch (Exception ex) {
            log.log("Konnte Verbindung nicht herstellen", Log.ERRORS);
            ExHandler.handle(ex);
        }
        Log.setAlert(null);
    }

    /**
   * Diese Funktion erledigt Datenbank�nderungen, die im Rahmen eines Updates n�tig sind
   * Die versions enth�lt eine Versionsliste, cmds ein Kommando f�r jede dieser Versionen.
   * Ein Kommando ist entweder <ul>
   * <li>direkt ein SQL-Befehl,
   * <li>eine ; getrennte Liste von SQL-Befehlen
   * <li>ein Tx kommando, wobei T jeden Termin einliest und wieder wegschreibt, 
   * w�hrend x zus�tzliche Modifikationen definiert.
   * <li>ein Sx Kommando, wobei S eine spezielle Anweisung ist, die mit x n�her definiert wird.</ul>
   * Diese Version kann keine DB �lter als 3.1.0 updaten. 
   */
    static final String[] databases = { "com.mysql.jdbc.Driver", "com.fourd.jdbc.DriverImpl", "org.hsqldb.jdbcDriver" };

    static final String[] versions = { "3.1.1", "3.1.2", "3.1.3", "3.1.4" };

    static final String[] hsqlcmds = { "ALTER TABLE agnTermine ADD COLUMN nGrund longvarchar;T1;" + "ALTER TABLE agnTermine DROP COLUMN Grund;" + "ALTER TABLE agnTermine ALTER COLUMN nGrund RENAME TO Grund", "S1;CREATE TABLE agnResourceDef(name varchar(30) primary key, stdDauer integer, maxCount integer);" + "CREATE CACHED TABLE agnResourceRes(UID varchar(25) primary key,name varchar(30), beginn integer,ende integer);" + "DELETE FROM agnConfig WHERE param='TypFarben';DELETE FROM agnConfig WHERE param='StatusFarben';" + "DELETE FROM agnConfig WHERE param='StdLogLevel';DELETE FROM agnConfig WHERE param='StdLogFile'", "ALTER TABLE agnMandanten ADD COLUMN resource longvarchar", "ALTER TABLE agnMandanten ADD COLUMN Map4D varchar(20)" };

    static final String[] mysqlcmds = { "ALTER TABLE agnTermine MODIFY COLUMN Grund text", "S1;CREATE TABLE agnResourceDef(name varchar(30) primary key, stdDauer integer, maxCount integer);" + "CREATE TABLE agnResourceRes(UID varchar(25) primary key,name varchar(30), beginn integer,ende integer);" + "DELETE FROM agnConfig WHERE param='TypFarben';DELETE FROM agnConfig WHERE param='StatusFarben';" + "DELETE FROM agnConfig WHERE param='StdLogLevel';DELETE FROM agnConfig WHERE param='StdLogFile'", "ALTER TABLE agnMandanten ADD COLUMN resource text", "ALTER TABLE agnMandanten ADD COLUMN Map4D varchar(20)" };

    static String[] cmds = null;

    static VersionInfo vi;

    static void dbUpdate() {
        final String minimalVersion = "3.1.0";
        log = Log.get("dbUpdate");
        String dbv = Agenda.remoteCfg.get("dbVersion", null);
        if (dbv == null) {
            log.log("Kann keine Version lesen", Log.ERRORS);
            vi = new VersionInfo("2.0.2");
        } else {
            vi = new VersionInfo(dbv);
        }
        if (vi.isOlder(minimalVersion)) {
            log.log("DBVersion zu alt: " + vi.version(), 1);
            SwingHelper.alert(Agenda.client, "Start nicht m�glich", "Die Datenbank-Version ist zu alt f�r diese Agenda-Version. Bitte wenden Sie sich an den Support");
            System.exit(5);
        }
        String driver = Agenda.j.dbDriver();
        if (driver == null) {
            driver = "unbekannt";
        }
        if (driver.equals(databases[0])) {
            cmds = mysqlcmds;
        } else if (driver.equals(databases[2])) {
            cmds = hsqlcmds;
        } else {
            SwingHelper.alert(Agenda.client, "AutoUpdate nicht m�glich", "F�r Ihre Datenbank ist leider keine automatische Update-Funktion implementiert");
            return;
        }
        if (Agenda.termin == null) {
            Agenda.termin = new AgendaEntry(Agenda.theInstance);
        }
        Stm stm = Agenda.j.getStatement();
        for (int i = 0; i < versions.length; i++) {
            if (vi.isOlder(versions[i])) {
                String[] cmd = cmds[i].split(";");
                log.log("Update auf " + versions[i], Log.ERRORS);
                for (int c = 0; c < cmd.length; c++) {
                    if (cmd[c].matches("T[0-9]+")) {
                        try {
                            int cnum = Integer.parseInt(cmd[c].substring(1));
                            ResultSet res = stm.query("SELECT * from agnTermine");
                            AgendaEntry t = Agenda.termin.getInstance();
                            while ((res != null) && (res.next() == true)) {
                                t.fetch(res);
                                switch(cnum) {
                                    case 1:
                                        stm.exec("UPDATE agnTermine set nGrund='" + t.Grund + "' WHERE ID=" + JdbcLink.wrap(t.TID));
                                        break;
                                    default:
                                        log.log("Undefinierte Aktion bei Update", Log.ERRORS);
                                }
                                t.flush();
                            }
                        } catch (Exception ex) {
                            ExHandler.handle(ex);
                        }
                    } else if (cmd[c].matches("S[0-9]+")) {
                        int cnum = Integer.parseInt(cmd[c].substring(1));
                        switch(cnum) {
                            case 1:
                                String col = null;
                                try {
                                    col = Agenda.remoteCfg.get("StdFarben", null).trim().replaceFirst(",$", "");
                                } catch (Exception ex) {
                                }
                                if (col != null) {
                                    String col1 = col.replaceAll("([a-zA-Z0-9\\- \\.\\/]+)", "A$1");
                                    col1 = "FS1," + col1.replaceAll("AA", "A");
                                    Hashtable oldcols = StringTool.foldStrings(col1.replaceAll(",", StringTool.flattenSeparator));
                                    Enumeration en = oldcols.keys();
                                    while (en.hasMoreElements()) {
                                        String k = (String) en.nextElement();
                                        String hk = null;
                                        col = (String) oldcols.get(k);
                                        if (k.startsWith("tColor")) {
                                            hk = k.substring(6);
                                            Agenda.localCfg.set("farben/typ/" + hk, col);
                                        } else if (k.startsWith("sColor")) {
                                            hk = k.substring(6);
                                            Agenda.localCfg.set("farben/status/" + hk, col);
                                        }
                                    }
                                }
                                Agenda.saveLocalSettings();
                                break;
                        }
                    } else {
                        stm.exec(cmd[c]);
                    }
                }
            }
        }
        Agenda.remoteCfg.set("dbVersion", Agenda.DBVersion);
        Agenda.remoteCfg.set("AgendaVersion", Agenda.Version);
        Agenda.remoteCfg.flush();
        Agenda.j.releaseStatement(stm);
    }
}
