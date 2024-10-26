package com.umc;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.net.MalformedURLException;
import java.net.URL;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Hashtable;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import org.apache.log4j.Logger;
import org.apache.xmlbeans.XmlException;
import com.umc.beans.Param;
import com.umc.collector.Publisher;
import com.umc.helper.ProxyConnection;
import com.umc.helper.UMCConstants;
import de.umcProject.xmlbeans.UmcConfigDocument;
import de.umcProject.xmlbeans.UmcConfigDocument.UmcConfig;
import de.umcProject.xmlbeans.UmcConfigDocument.UmcConfig.Libraries.Library;
import de.umcProject.xmlbeans.UmcConfigDocument.UmcConfig.Libraries.Library.MovieDirToGenre;
import de.umcProject.xmlbeans.UmcConfigDocument.UmcConfig.Libraries.Library.MovieGroup;
import de.umcProject.xmlbeans.UmcConfigDocument.UmcConfig.Libraries.Library.MovieScanDir;
import de.umcProject.xmlbeans.UmcConfigDocument.UmcConfig.Libraries.Library.MovieTitleFromDir;
import de.umcProject.xmlbeans.UmcConfigDocument.UmcConfig.Mediacenter.Rss.Feed;

/**
 * Diese Klasse dient dazu divere Sache vor dem Start des UMC zu initialisieren
 * 
 * @author DonGyros
 * 
 * @version 0.1 07.11.2008
 *
 */
public class UMCInitializer {

    private static Logger log = Logger.getLogger("com.umc.file");

    public static void start() throws Exception {
        log.info("UMC wird initialisiert...bitte warten");
        executeSQLScript();
        String proxyHost = Publisher.getInstance().getParamProxyHost();
        if (proxyHost != null && !proxyHost.equals("")) {
            ProxyConnection pc = new ProxyConnection(proxyHost, Publisher.getInstance().getParamProxyPort(), Publisher.getInstance().getParamProxyUser(), Publisher.getInstance().getParamProxyPwd());
        }
        if (Publisher.getInstance().isParamAutoUpdateEnabled()) {
            log.info("Auto Update wird durchgeführt..");
            executeDBPatchFile();
        } else {
            log.info("Auto Update wird nicht durchgeführt");
        }
        createFrontendDirectoryStructure();
    }

    /**
	 * Diese Methode erstellt das Zielverzeichnis für das Frontend (UltimateMediaCenter)
	 */
    private static void createFrontendDirectoryStructure() {
        try {
            String dir = Publisher.getInstance().getParamMediaCenterLocation();
            File destination = new File(dir);
            if (!destination.exists()) {
                log.debug("Verzeichnis " + dir + " wird angelegt");
                if (destination.mkdir()) {
                    log.debug("Verzeichnis wurde angelegt");
                } else {
                    log.fatal("Verzeichnis konnte nicht angelegt werden. UMC wird beendet");
                    System.exit(0);
                }
            }
            destination = new File(dir + File.separator + "script");
            if (!destination.exists()) {
                log.debug("Unterverzeichnis " + destination.getName() + " wird angelegt");
                destination.mkdir();
            }
            destination = new File(dir + File.separator + "css");
            if (!destination.exists()) {
                log.debug("Unterverzeichnis " + destination.getName() + " wird angelegt");
                destination.mkdir();
            }
            destination = new File(dir + File.separator + "ums");
            if (!destination.exists()) {
                log.debug("Unterverzeichnis " + destination.getName() + " wird angelegt");
                destination.mkdir();
            }
            destination = new File(dir + File.separator + "playlists");
            if (!destination.exists()) {
                log.debug("Unterverzeichnis " + destination.getName() + " wird angelegt");
                destination.mkdir();
            }
            destination = new File(dir + File.separator + "playlists/video");
            if (!destination.exists()) {
                log.debug("Unterverzeichnis " + destination.getName() + " wird angelegt");
                destination.mkdir();
            }
            destination = new File(dir + File.separator + "playlists/audio");
            if (!destination.exists()) {
                log.debug("Unterverzeichnis " + destination.getName() + " wird angelegt");
                destination.mkdir();
            }
            destination = new File(dir + File.separator + "playlists/photos");
            if (!destination.exists()) {
                log.debug("Unterverzeichnis " + destination.getName() + " wird angelegt");
                destination.mkdir();
            }
            destination = new File(dir + File.separator + "script");
            if (!destination.exists()) {
                log.debug("Unterverzeichnis " + destination.getName() + " wird angelegt");
                destination.mkdir();
            }
            destination = new File(dir + File.separator + "pics");
            if (!destination.exists()) {
                log.debug("Unterverzeichnis " + destination.getName() + " wird angelegt");
                destination.mkdir();
            }
            destination = new File(dir + File.separator + "pics/Cover");
            if (!destination.exists()) {
                log.debug("Unterverzeichnis " + destination.getName() + " wird angelegt");
                destination.mkdir();
            }
            destination = new File(dir + File.separator + "pics/Cover/Movies");
            if (!destination.exists()) {
                log.debug("Unterverzeichnis " + destination.getName() + " wird angelegt");
                destination.mkdir();
            }
            destination = new File(dir + File.separator + "pics/Cover/Series");
            if (!destination.exists()) {
                log.debug("Unterverzeichnis " + destination.getName() + " wird angelegt");
                destination.mkdir();
            }
            destination = new File(dir + File.separator + "pics/Banner");
            if (!destination.exists()) {
                log.debug("Unterverzeichnis " + destination.getName() + " wird angelegt");
                destination.mkdir();
            }
            destination = new File(dir + File.separator + "pics/Banner/Movies");
            if (!destination.exists()) {
                log.debug("Unterverzeichnis " + destination.getName() + " wird angelegt");
                destination.mkdir();
            }
            destination = new File(dir + File.separator + "pics/Banner/Series");
            if (!destination.exists()) {
                log.debug("Unterverzeichnis " + destination.getName() + " wird angelegt");
                destination.mkdir();
            }
            destination = new File(dir + File.separator + "pics/Backdrops");
            if (!destination.exists()) {
                log.debug("Unterverzeichnis " + destination.getName() + " wird angelegt");
                destination.mkdir();
            }
            destination = new File(dir + File.separator + "pics/Backdrops/Movies");
            if (!destination.exists()) {
                log.debug("Unterverzeichnis " + destination.getName() + " wird angelegt");
                destination.mkdir();
            }
            destination = new File(dir + File.separator + "pics/Backdrops/Series");
            if (!destination.exists()) {
                log.debug("Unterverzeichnis " + destination.getName() + " wird angelegt");
                destination.mkdir();
            }
            destination = new File(dir + File.separator + "pics/Backgrounds");
            if (!destination.exists()) {
                log.debug("Unterverzeichnis " + destination.getName() + " wird angelegt");
                destination.mkdir();
            }
            destination = new File(dir + File.separator + "pics/Keyboard");
            if (!destination.exists()) {
                log.debug("Unterverzeichnis " + destination.getName() + " wird angelegt");
                destination.mkdir();
            }
        } catch (Exception exc) {
            log.fatal("Verzeichnis konnte nicht angelegt werden. UMC wird beendet", exc);
            System.exit(0);
        }
    }

    private static void executeSQLScript() {
        File f = new File(System.getProperty("user.dir") + "/resources/umc.sql");
        if (f.exists()) {
            Connection con = null;
            PreparedStatement pre_stmt = null;
            try {
                Class.forName("org.sqlite.JDBC");
                con = DriverManager.getConnection("jdbc:sqlite:database/umc.db", "", "");
                BufferedReader br = new BufferedReader(new FileReader(f));
                String line = "";
                con.setAutoCommit(false);
                while ((line = br.readLine()) != null) {
                    if (!line.equals("") && !line.startsWith("--") && !line.contains("--")) {
                        log.debug(line);
                        pre_stmt = con.prepareStatement(line);
                        pre_stmt.executeUpdate();
                    }
                }
                con.commit();
                File dest = new File(f.getAbsolutePath() + ".executed");
                if (dest.exists()) dest.delete();
                f.renameTo(dest);
                f.delete();
            } catch (Throwable exc) {
                log.error("Fehler bei Ausführung der SQL Datei", exc);
                try {
                    con.rollback();
                } catch (SQLException exc1) {
                }
            } finally {
                try {
                    if (pre_stmt != null) pre_stmt.close();
                    if (con != null) con.close();
                } catch (SQLException exc2) {
                    log.error("Fehler bei Ausführung von SQL Datei", exc2);
                }
            }
        }
    }

    private static void executeDBPatchFile() throws Exception {
        Connection con = null;
        PreparedStatement pre_stmt = null;
        ResultSet rs = null;
        try {
            InputStream is = null;
            URL url = new URL("http://www.hdd-player.de/umc/UMC-DB-Update-Script.sql");
            is = url.openStream();
            BufferedReader br = new BufferedReader(new InputStreamReader(is));
            Class.forName("org.sqlite.JDBC");
            con = DriverManager.getConnection("jdbc:sqlite:database/umc.db", "", "");
            double dbVersion = -1;
            pre_stmt = con.prepareStatement("SELECT * FROM DB_VERSION WHERE ID_MODUL = 0");
            rs = pre_stmt.executeQuery();
            if (rs.next()) {
                dbVersion = rs.getDouble("VERSION");
            }
            String line = "";
            con.setAutoCommit(false);
            boolean collectSQL = false;
            ArrayList<String> sqls = new ArrayList<String>();
            double patchVersion = 0;
            while ((line = br.readLine()) != null) {
                if (line.startsWith("[")) {
                    Pattern p = Pattern.compile("\\[.*\\]");
                    Matcher m = p.matcher(line);
                    m.find();
                    String value = m.group();
                    value = value.substring(1, value.length() - 1);
                    patchVersion = Double.parseDouble(value);
                }
                if (patchVersion == dbVersion + 1) collectSQL = true;
                if (collectSQL) {
                    if (!line.equals("") && !line.startsWith("[") && !line.startsWith("--") && !line.contains("--")) {
                        if (line.endsWith(";")) line = line.substring(0, line.length() - 1);
                        sqls.add(line);
                    }
                }
            }
            if (pre_stmt != null) pre_stmt.close();
            if (rs != null) rs.close();
            for (String sql : sqls) {
                log.debug("Führe SQL aus Patch Datei aus: " + sql);
                pre_stmt = con.prepareStatement(sql);
                pre_stmt.execute();
            }
            if (patchVersion > 0) {
                log.debug("aktualisiere Versionsnummer in DB");
                if (pre_stmt != null) pre_stmt.close();
                if (rs != null) rs.close();
                pre_stmt = con.prepareStatement("UPDATE DB_VERSION SET VERSION = ? WHERE ID_MODUL = 0");
                pre_stmt.setDouble(1, patchVersion);
                pre_stmt.execute();
            }
            con.commit();
        } catch (MalformedURLException exc) {
            log.error(exc.toString());
            throw new Exception("SQL Patch Datei konnte nicht online gefunden werden", exc);
        } catch (IOException exc) {
            log.error(exc.toString());
            throw new Exception("SQL Patch Datei konnte nicht gelesen werden", exc);
        } catch (Throwable exc) {
            log.error("Fehler bei Ausführung der SQL Patch Datei", exc);
            try {
                con.rollback();
            } catch (SQLException exc1) {
            }
            throw new Exception("SQL Patch Datei konnte nicht ausgeführt werden", exc);
        } finally {
            try {
                if (pre_stmt != null) pre_stmt.close();
                if (con != null) con.close();
            } catch (SQLException exc2) {
                log.error("Fehler bei Ausführung von SQL Patch Datei", exc2);
            }
        }
    }

    /**
	 * Helper-Methode um Movie Scan-Verzeichnisse und Default-Verzeichnisse für Actors,Cover und Posters 
	 * in die DB einzutragen.
	 * 
	 * Alternative -> Angabe in der Datei umc.sql
	 * 
	 * Default-Verzeichnisse für Poster, Cover und Actor nicht veränderbar -> fest vorgegeben
	 * 
	 */
    private static void insertDirs() {
        Collection<String> scanDirs = new ArrayList<String>();
        Connection con = null;
        try {
            Class.forName("org.sqlite.JDBC");
            con = DriverManager.getConnection("jdbc:sqlite:database/umc.db", "", "");
            PreparedStatement pre_stmt = null;
            ResultSet rs = null;
            boolean foundDir = false;
            int idDir = -1;
            int nextIdDir = -1;
            pre_stmt = con.prepareStatement("SELECT ID_DIR FROM MOVIE_SCAN_DIR WHERE PC_DIR=? AND ID_SCAN_TYPE=?");
            pre_stmt.setString(1, System.getProperty("user.dir") + "/resources/Cover");
            pre_stmt.setInt(2, UMCConstants.SCAN_TYPE_FANART);
            rs = pre_stmt.executeQuery();
            if (rs.next()) {
                idDir = rs.getInt("ID_DIR");
                foundDir = true;
            }
            if (!foundDir) {
                nextIdDir = -1;
                pre_stmt.close();
                pre_stmt = null;
                if (rs != null) rs.close();
                rs = null;
                pre_stmt = con.prepareStatement("select max(ID_DIR)+1 AS NEXT_ID_DIR from MOVIE_SCAN_DIR");
                rs = pre_stmt.executeQuery();
                if (rs.next()) {
                    nextIdDir = rs.getInt("NEXT_ID_DIR");
                }
                pre_stmt.close();
                pre_stmt = null;
                if (rs != null) rs.close();
                rs = null;
                pre_stmt = con.prepareStatement("insert into MOVIE_SCAN_DIR (ID_DIR,PC_DIR,ID_SCAN_TYPE,DIR_TYPE,SUBDIRS) values (?,?,?,'system',1)");
                pre_stmt.setLong(1, nextIdDir);
                pre_stmt.setString(2, System.getProperty("user.dir") + "/resources/Cover");
                pre_stmt.setInt(3, UMCConstants.SCAN_TYPE_FANART);
                pre_stmt.executeUpdate();
                idDir++;
                log.info("Default Cover Verzeichnis wurde eingetragen");
            }
            foundDir = false;
            pre_stmt.close();
            pre_stmt = null;
            if (rs != null) rs.close();
            rs = null;
            pre_stmt = con.prepareStatement("SELECT ID_DIR FROM MOVIE_SCAN_DIR WHERE PC_DIR=? AND ID_SCAN_TYPE=?");
            pre_stmt.setString(1, System.getProperty("user.dir") + "/resources/Backdrops");
            pre_stmt.setInt(2, UMCConstants.SCAN_TYPE_FANART);
            rs = pre_stmt.executeQuery();
            if (rs.next()) {
                idDir = rs.getInt("ID_DIR");
                foundDir = true;
            }
            if (!foundDir) {
                nextIdDir = -1;
                pre_stmt.close();
                pre_stmt = null;
                if (rs != null) rs.close();
                rs = null;
                pre_stmt = con.prepareStatement("select max(ID_DIR)+1 AS NEXT_ID_DIR from MOVIE_SCAN_DIR");
                rs = pre_stmt.executeQuery();
                if (rs.next()) {
                    nextIdDir = rs.getInt("NEXT_ID_DIR");
                }
                pre_stmt.close();
                pre_stmt = null;
                if (rs != null) rs.close();
                rs = null;
                pre_stmt = con.prepareStatement("insert into MOVIE_SCAN_DIR (ID_DIR,PC_DIR,ID_SCAN_TYPE,DIR_TYPE,SUBDIRS) values (?,?,?,'system',1);");
                pre_stmt.setLong(1, nextIdDir);
                pre_stmt.setString(2, System.getProperty("user.dir") + "/resources/Backdrops");
                pre_stmt.setInt(3, UMCConstants.SCAN_TYPE_FANART);
                pre_stmt.executeUpdate();
                log.info("Default Backdrop Verzeichnis wurde eingetragen");
            }
            foundDir = false;
            pre_stmt.close();
            pre_stmt = null;
            if (rs != null) rs.close();
            rs = null;
            pre_stmt = con.prepareStatement("SELECT ID_DIR FROM MOVIE_SCAN_DIR WHERE PC_DIR=? AND ID_SCAN_TYPE=?");
            pre_stmt.setString(1, System.getProperty("user.dir") + "/resources/Banner");
            pre_stmt.setInt(2, UMCConstants.SCAN_TYPE_FANART);
            rs = pre_stmt.executeQuery();
            if (rs.next()) {
                idDir = rs.getInt("ID_DIR");
                foundDir = true;
            }
            if (!foundDir) {
                nextIdDir = -1;
                pre_stmt.close();
                pre_stmt = null;
                if (rs != null) rs.close();
                rs = null;
                pre_stmt = con.prepareStatement("select max(ID_DIR)+1 AS NEXT_ID_DIR from MOVIE_SCAN_DIR");
                rs = pre_stmt.executeQuery();
                if (rs.next()) {
                    nextIdDir = rs.getInt("NEXT_ID_DIR");
                }
                pre_stmt.close();
                pre_stmt = null;
                if (rs != null) rs.close();
                rs = null;
                pre_stmt = con.prepareStatement("insert into MOVIE_SCAN_DIR (ID_DIR,PC_DIR,ID_SCAN_TYPE,DIR_TYPE,SUBDIRS) values (?,?,?,'system',1);");
                pre_stmt.setLong(1, nextIdDir);
                pre_stmt.setString(2, System.getProperty("user.dir") + "/resources/Banner");
                pre_stmt.setInt(3, UMCConstants.SCAN_TYPE_FANART);
                pre_stmt.executeUpdate();
                log.info("Default Banner Verzeichnis wurde eingetragen");
            }
            foundDir = false;
            pre_stmt.close();
            pre_stmt = null;
            if (rs != null) rs.close();
            rs = null;
            pre_stmt = con.prepareStatement("SELECT ID_DIR FROM MOVIE_SCAN_DIR WHERE PC_DIR=? AND ID_SCAN_TYPE=?");
            pre_stmt.setString(1, System.getProperty("user.dir") + "/resources/Nfo");
            pre_stmt.setInt(2, UMCConstants.SCAN_TYPE_FANART);
            rs = pre_stmt.executeQuery();
            if (rs.next()) {
                idDir = rs.getInt("ID_DIR");
                foundDir = true;
            }
            if (!foundDir) {
                nextIdDir = -1;
                pre_stmt.close();
                pre_stmt = null;
                if (rs != null) rs.close();
                rs = null;
                pre_stmt = con.prepareStatement("select max(ID_DIR)+1 AS NEXT_ID_DIR from MOVIE_SCAN_DIR");
                rs = pre_stmt.executeQuery();
                if (rs.next()) {
                    nextIdDir = rs.getInt("NEXT_ID_DIR");
                }
                pre_stmt.close();
                pre_stmt = null;
                if (rs != null) rs.close();
                rs = null;
                pre_stmt = con.prepareStatement("insert into MOVIE_SCAN_DIR (ID_DIR,PC_DIR,ID_SCAN_TYPE,DIR_TYPE,SUBDIRS) values (?,?,?,'system',1);");
                pre_stmt.setLong(1, nextIdDir);
                pre_stmt.setString(2, System.getProperty("user.dir") + "/resources/Nfo");
                pre_stmt.setInt(3, UMCConstants.SCAN_TYPE_FANART);
                pre_stmt.executeUpdate();
                log.info("Default Nfo Verzeichnis wurde eingetragen");
            }
            con.close();
        } catch (Exception exc) {
            log.error("Fehler bei Eintrag eines Verzeichnisses", exc);
        } finally {
            try {
                if (con != null) con.close();
            } catch (SQLException exc) {
            }
        }
    }
}
