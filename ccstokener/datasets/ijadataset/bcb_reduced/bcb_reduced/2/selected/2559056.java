package com.umc;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.net.ConnectException;
import java.net.HttpURLConnection;
import java.net.SocketTimeoutException;
import java.net.URL;
import java.net.URLConnection;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.Scanner;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import org.apache.log4j.Logger;
import com.umc.helper.UMCConstants;

public class UMCUpdater extends Thread {

    private static Logger log = Logger.getLogger("com.umc.file");

    private static UMCUpdater myInstance = null;

    private LinkedList<String> dbSQLStatements = new LinkedList<String>();

    private HashMap<String, String[]> optionalUpdates = new HashMap<String, String[]>();

    private HashMap<String, String[]> recommendedUpdates = new HashMap<String, String[]>();

    public static UMCUpdater getInstance() {
        if (myInstance == null) {
            myInstance = new UMCUpdater();
            myInstance.start();
        }
        return myInstance;
    }

    public void run() {
    }

    /**
	 * Checks if an database update is available.
	 * If an update is availabe the file will be downloaded 
	 * to the update folder and 'true' will be returned. 
	 * Else 'false'.
	 * 
	 * @return true/false
	 */
    private boolean isDatabaseUpdateAvailable() {
        log.info("[Update] Checking for new update...");
        String downloadURL = exists(UMCConstants.updateDatabaseURL);
        if (downloadURL == null) return false;
        InputStream in = null;
        long milis = System.currentTimeMillis();
        try {
            URL u = new URL(downloadURL);
            URLConnection con = u.openConnection();
            con.setConnectTimeout(15000);
            con.setReadTimeout(15000);
            con.setUseCaches(true);
            in = u.openStream();
            fillListFromStream(in);
            if (recommendedUpdates.size() > 0 || optionalUpdates.size() > 0) return true;
            return false;
        } catch (SocketTimeoutException exc) {
            log.error("'Database Update' timeout", exc);
            return false;
        } catch (Exception e) {
            log.error("'Database-Update' failed", e);
            return false;
        } finally {
            try {
                if (in != null) in.close();
            } catch (IOException e) {
            }
        }
    }

    public void fillListFromStream(InputStream is) throws IOException {
        if (is != null) {
            String line;
            double patchVersion = 0;
            String patchType = "";
            String patchDescription = "";
            String patchSQL = "";
            Connection con = null;
            try {
                Class.forName("org.sqlite.JDBC");
                con = DriverManager.getConnection("jdbc:sqlite:database/umc.db", "", "");
                double dbVersion = -1;
                PreparedStatement pre_stmt = con.prepareStatement("SELECT * FROM DB_VERSION WHERE ID_MODUL = 0");
                ResultSet rs = pre_stmt.executeQuery();
                if (rs.next()) {
                    dbVersion = rs.getDouble("VERSION");
                }
                double nextPatchVersion = dbVersion + 1;
                boolean collectSQL = false;
                BufferedReader reader = new BufferedReader(new InputStreamReader(is, "UTF-8"));
                while ((line = reader.readLine()) != null) {
                    if (line.startsWith("[")) {
                        Pattern p = Pattern.compile("\\[.*\\]");
                        Matcher m = p.matcher(line);
                        m.find();
                        String value = m.group();
                        value = value.substring(1, value.length() - 1);
                        int i = 1;
                        Scanner sc = new Scanner(value).useDelimiter("\\|");
                        while (sc.hasNext()) {
                            if (i == 1) patchVersion = Double.parseDouble(sc.next());
                            if (i == 2) patchType = sc.next();
                            if (i == 3) patchDescription = sc.next();
                            if (i == 4) patchSQL = sc.next();
                            i++;
                        }
                        if (patchVersion == nextPatchVersion) {
                            collectSQL = true;
                            nextPatchVersion++;
                        } else {
                            collectSQL = false;
                        }
                        if (collectSQL) {
                            if (line.endsWith(";")) line = line.substring(0, line.length() - 1);
                            String[] s = { patchDescription, line };
                            if (patchType.equals("recommended")) recommendedUpdates.put(patchVersion + "", s); else if (patchType.equals("optional")) optionalUpdates.put(patchVersion + "", s); else log.warn("SQL patch type incorrrect. " + line + " disgarded");
                        }
                    }
                }
            } catch (SQLException exc) {
                log.error(exc);
            } catch (ClassNotFoundException exc) {
                log.error(exc);
            } finally {
                is.close();
                try {
                    con.close();
                } catch (SQLException exc) {
                }
            }
        } else {
        }
    }

    /**
	 * Executes apreviously downloaded database update.
	 */
    private void executeDatabaseUpdate() {
    }

    private String exists(String URLName) {
        String url = URLName;
        try {
            HttpURLConnection.setFollowRedirects(false);
            HttpURLConnection con = (HttpURLConnection) new URL(url).openConnection();
            con.setRequestMethod("HEAD");
            con.setConnectTimeout(10000);
            con.setReadTimeout(10000);
            if (con.getResponseCode() == HttpURLConnection.HTTP_MOVED_TEMP) {
                url = con.getHeaderField("Location");
                con = (HttpURLConnection) new URL(url).openConnection();
                con.setConnectTimeout(10000);
                con.setReadTimeout(10000);
                con.setRequestMethod("HEAD");
            }
            if (con.getResponseCode() == HttpURLConnection.HTTP_OK) return url;
            return null;
        } catch (SocketTimeoutException exc) {
            log.error("SocketTimeout: " + url);
            return null;
        } catch (ConnectException ce) {
            log.error("ConnectionTimeout: " + url);
            return null;
        } catch (Exception e) {
            log.error("Check URL " + url, e);
            return null;
        }
    }

    /**
	 * Checks if an application update is available.
	 * If an update is availabe the file will be downloaded 
	 * to the update folder and 'true' will be returned. 
	 * Else 'false'.
	 * 
	 * @return true/false
	 */
    private static boolean isApplicationUpdateAvailable() {
        return false;
    }

    /**
	 * Downloads and executes an application update.
	 */
    private static void executeApplicationUpdate() {
    }
}
