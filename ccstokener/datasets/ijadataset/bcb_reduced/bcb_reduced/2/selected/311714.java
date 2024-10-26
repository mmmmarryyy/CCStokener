package connectivity;

import exceptions.BadTypeException;
import exceptions.HTTPConnectionException;
import exceptions.IncorrectLoginException;
import general.Ban;
import gui.LocalPanel;
import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.io.PrintWriter;
import java.net.HttpURLConnection;
import java.net.MalformedURLException;
import java.net.URL;
import java.net.URLConnection;
import java.net.URLEncoder;
import java.sql.SQLException;
import java.util.ArrayList;
import javax.swing.JProgressBar;
import state.ProgramState;
import storage.DB;

/**
 * @author Antragon
 * Used to upload and download bans from Banlist.nl
 */
public class BanlistNLBans {

    /**
	 * Constant indicating that the old Banlist.nl scripts should be used.
	 */
    public static final int OLD_SCRIPT = 0;

    /**
	 * Constant indicating that the new Banlist.nl scripts should be used.
	 */
    public static final int NEW_SCRIPT = 1;

    /**
	 * Constant indicating that the users buddies' bans should be downloaded.  For use with the old Banlist.nl scripts only
	 */
    public static final String OTHERS = "others";

    /**
	 * Constant indicating that the users own bans should be downloaded.  For use with the old Banlist.nl scripts only
	 */
    public static final String OWN = "own";

    private DB b = DB.instance();

    private ProgramState programState = ProgramState.instance();

    private static final String OLD_LOGIN = "http://www.banlist.nl/banlist_login.php?login=";

    private static final String OLD_UPLOAD = "http://www.banlist.nl/banlist_upload.php";

    private static final String OLD_DOWNLOAD = "http://www.banlist.nl/banlist_download.php?list=";

    private static final String NEW_UPLOAD = "http://www.banlist.nl/bl_upload.php";

    private static final String NEW_SERVER = "http://www.banlist.nl/bl_server.php?";

    public BanlistNLBans() {
    }

    /**
	 * Connects to the old Banlist.nl login scripts
	 * 
	 * @return Returns a URLConnection to Banlist.nl
	 * @throws MalformedURLException Thrown when the URL given is not a valid URL
	 * @throws IOException Thrown when there is an error connecting
	 */
    private URLConnection connect() throws MalformedURLException, IOException {
        URL bbURL = null;
        URLConnection conn = null;
        bbURL = new URL(OLD_LOGIN + programState.getBanlistName() + "&pass=" + programState.getPassword());
        conn = bbURL.openConnection();
        return conn;
    }

    /**
	 * Gets a session key for use with the old Banlist.nl scripts
	 * 
	 * @param conn A URLConnection to the Banlist.nl login scripts
	 * @return Returns a String with the session key
	 * @throws IOException Thrown when there is an error connecting to the server
	 * @throws IncorrectLoginException Thrown when the supplied login information is incorrect
	 */
    private static String sessionKey(URLConnection conn) throws IOException, IncorrectLoginException {
        String info = "";
        BufferedReader in = new BufferedReader(new InputStreamReader(conn.getURL().openStream()));
        String tmp;
        if (in != null) {
            while ((tmp = in.readLine()) != null) {
                info = info + tmp;
            }
        }
        if (in != null) {
            in.close();
        }
        String sessionKey = "";
        if (info == "Login Error") {
            throw new IncorrectLoginException();
        } else {
            sessionKey = info;
        }
        return sessionKey;
    }

    /**
	 * This upload function uploads using the new server scripts
	 * 
	 * @param prog Progress bar showing status of the upload
	 * @throws SQLException Caused by database errors
	 * @throws IOException Usually caused by a problem with the connection to Banlist.nl
	 * @throws HTTPConnectionException Thrown when a connection occurs
	 */
    public void uploadBans(JProgressBar prog) throws SQLException, IOException, IncorrectLoginException, HTTPConnectionException {
        if (b.getHostForumName(programState.getHostID()) != null) {
            newUploadBans(prog);
        }
        if (b.getHostBanlistName(programState.getHostID()) != null) {
            oldUploadBans(prog);
        }
    }

    /**
	 * This upload function interacts with the new Banlist.nl scripts, which use the forum login
	 * 
	 * @param prog The JProgressBar to update
	 * @throws SQLException Thrown when there is an error with the local database
	 * @throws IOException Thrown when the server returns an error
	 * @throws HTTPConnectionException Thrown when there is an error from the server
	 */
    private String newUploadBans(JProgressBar prog) throws SQLException, IOException, HTTPConnectionException {
        String newline = null;
        try {
            newline = System.getProperty("line.separator");
        } catch (Exception e) {
            newline = "\n";
        }
        String username = programState.getForumName();
        String password = programState.getForumPassword();
        ArrayList<String> banlist = b.getBans(programState.getHostID());
        if (banlist.size() == 0) {
            return "You have no bans to upload";
        } else {
            String bans = "";
            for (int x = 0; x < banlist.size(); x++) {
                String[] info = banlist.get(x).split("\t");
                String reason;
                try {
                    reason = info[1];
                } catch (Exception e) {
                    reason = "leaver";
                }
                if (x != banlist.size() - 1) {
                    bans = bans + info[0] + "\t0\t0\t" + reason + "\n";
                } else {
                    bans = bans + info[0] + "\t0\t0\t" + reason;
                }
            }
            if (bans == null) {
                throw new IOException();
            }
            StringBuffer sb = new StringBuffer();
            URL url = null;
            String body = null;
            url = new URL(NEW_UPLOAD);
            body = URLEncoder.encode("username", "UTF-8") + "=" + URLEncoder.encode(username, "UTF-8");
            body += "&" + URLEncoder.encode("password", "UTF-8") + "=" + URLEncoder.encode(password, "UTF-8");
            body += "&" + URLEncoder.encode("bans", "UTF-8") + "=" + URLEncoder.encode(bans, "UTF-8");
            prog.setValue(prog.getValue() + 10);
            HttpURLConnection conn = (HttpURLConnection) url.openConnection();
            conn.setRequestMethod("POST");
            conn.setAllowUserInteraction(false);
            conn.setDoOutput(true);
            conn.setRequestProperty("Content-type", "application/x-www-form-urlencoded");
            conn.setRequestProperty("Content-length", Integer.toString(body.length()));
            prog.setValue(prog.getValue() + 10);
            OutputStream rawOutStream = conn.getOutputStream();
            PrintWriter pw = new PrintWriter(rawOutStream);
            pw.print(body);
            pw.flush();
            pw.close();
            prog.setValue(prog.getValue() + 10);
            InputStream rawInStream;
            try {
                rawInStream = conn.getInputStream();
                BufferedReader rdr = new BufferedReader(new InputStreamReader(rawInStream));
                String line;
                while ((line = rdr.readLine()) != null) {
                    sb.append(line);
                    sb.append(newline);
                }
                prog.setValue(prog.getValue() + 10);
                if (sb.toString().length() > 0) {
                    return sb.toString();
                }
                return null;
            } catch (IOException e) {
                InputStream is = conn.getErrorStream();
                BufferedReader r = new BufferedReader(new InputStreamReader(is));
                String l;
                StringBuffer errorSB = new StringBuffer();
                while ((l = r.readLine()) != null) {
                    errorSB.append(l);
                    errorSB.append(newline);
                }
                throw new HTTPConnectionException(conn.getResponseCode(), errorSB.toString());
            }
        }
    }

    /**
	 * Uploads bans to the old Banlist.nl scripts
	 * 
	 * @param prog The ProgressBar to be updated
	 * @return Returns any output from the server
	 * @throws SQLException When there is a problem with the local database, an SQLException is thrown
	 * @throws IOException Thrown when there are no bans to upload or when there is an error from the server
	 * @throws IncorrectLoginException Thrown when the supplied login information is incorrect
	 * @throws HTTPConnectionException 
	 */
    private String oldUploadBans(JProgressBar prog) throws SQLException, IOException, IncorrectLoginException, HTTPConnectionException {
        URLConnection keyConn = connect();
        String sessionKey = sessionKey(keyConn);
        ArrayList<String> banlist = b.getBans(programState.getHostID());
        if (banlist.size() == 0) {
            return "You have no bans to upload";
        } else {
            String bans = "";
            for (int x = 0; x < banlist.size(); x++) {
                String[] info = banlist.get(x).split("\t");
                String reason;
                try {
                    reason = info[1];
                } catch (Exception e) {
                    reason = "leaver";
                }
                if (x != banlist.size() - 1) {
                    bans = bans + info[0] + "\t" + reason + "\t0" + "\r\n";
                } else {
                    bans = bans + info[0] + "\t0\t0\t" + reason + "\t0";
                }
            }
            if (bans == null) {
                throw new IOException();
            }
            StringBuffer sb = new StringBuffer();
            URL url = new URL(OLD_UPLOAD);
            String body = URLEncoder.encode("PHPSESSID", "UTF-8") + "=" + URLEncoder.encode(sessionKey, "UTF-8");
            body += "&" + URLEncoder.encode("bans", "UTF-8") + "=" + URLEncoder.encode(bans, "UTF-8");
            prog.setValue(prog.getValue() + 10);
            HttpURLConnection conn = (HttpURLConnection) url.openConnection();
            conn.setRequestMethod("POST");
            conn.setAllowUserInteraction(false);
            conn.setDoOutput(true);
            conn.setRequestProperty("Content-type", "application/x-www-form-urlencoded");
            conn.setRequestProperty("Content-length", Integer.toString(body.length()));
            prog.setValue(prog.getValue() + 10);
            OutputStream rawOutStream = conn.getOutputStream();
            PrintWriter pw = new PrintWriter(rawOutStream);
            pw.print(body);
            pw.flush();
            pw.close();
            prog.setValue(prog.getValue() + 10);
            InputStream rawInStream;
            try {
                rawInStream = conn.getInputStream();
                BufferedReader rdr = new BufferedReader(new InputStreamReader(rawInStream));
                String line;
                while ((line = rdr.readLine()) != null) {
                    sb.append(line);
                    sb.append("\n");
                }
                prog.setValue(100);
                if (sb.toString().length() > 0) {
                    return sb.toString();
                } else {
                    return null;
                }
            } catch (IOException e) {
                InputStream is = conn.getErrorStream();
                BufferedReader r = new BufferedReader(new InputStreamReader(is));
                String l;
                StringBuffer errorSB = new StringBuffer();
                while ((l = r.readLine()) != null) {
                    errorSB.append(l);
                    errorSB.append("\n");
                }
                throw new HTTPConnectionException(conn.getResponseCode(), errorSB.toString());
            }
        }
    }

    /**
	 * Downloads bans from the old Banlist.nl scripts
	 * 
	 * @param list The list to be downloaded.
	 * @return Returns a String with any information sent by the server
	 * @throws IOException Thrown when the server gives an error
	 * @throws MalformedURLException Thrown when the given URL is incorrect
	 * @throws BadTypeException Thrown when an incorrect list type is given
	 * @throws IncorrectLoginException Thrown when incorrect login information is given
	 */
    public String downloadBans(String list) throws IOException, MalformedURLException, BadTypeException, IncorrectLoginException {
        URLConnection connect = connect();
        String sessionKey = sessionKey(connect);
        StringBuffer sb = new StringBuffer();
        URL url = null;
        if (list != OTHERS && list != OWN) {
            throw new BadTypeException();
        }
        url = new URL(OLD_DOWNLOAD + list);
        String body = URLEncoder.encode("PHPSESSID", "UTF-8") + "=" + URLEncoder.encode(sessionKey, "UTF-8");
        HttpURLConnection conn = null;
        try {
            conn = (HttpURLConnection) url.openConnection();
            conn.setRequestMethod("POST");
            conn.setAllowUserInteraction(false);
            conn.setDoOutput(true);
            conn.setRequestProperty("Content-type", "application/x-www-form-urlencoded");
            conn.setRequestProperty("Content-length", Integer.toString(body.length()));
            OutputStream rawOutStream = conn.getOutputStream();
            PrintWriter pw = new PrintWriter(rawOutStream);
            pw.print(body);
            pw.flush();
            pw.close();
            InputStream rawInStream = conn.getInputStream();
            BufferedReader rdr = new BufferedReader(new InputStreamReader(rawInStream));
            String line;
            while ((line = rdr.readLine()) != null) {
                sb.append(line + "\n");
            }
        } catch (IOException e) {
            InputStream is = conn.getErrorStream();
            BufferedReader r = new BufferedReader(new InputStreamReader(is));
            String l;
            StringBuffer errorSB = new StringBuffer();
            while ((l = r.readLine()) != null) {
                errorSB.append(l);
                errorSB.append("\n");
            }
            return errorSB.toString();
        }
        return sb.toString();
    }

    /**
	 * Downloads a list of bans from the new Banlist.nl scripts 
	 * 
	 * @param lists An String[] of usernames to download
	 * @return An ArrayList<String> of bans
	 * @throws MalformedURLException Thrown when a given URL is invalid
	 * @throws IOException Thrown when the server gives an error
	 */
    public ArrayList<String> newDownloadBanlists(String[] lists) throws MalformedURLException, IOException {
        String[][] info = getDownloadServerInfo(lists);
        ArrayList<String> bans = new ArrayList<String>();
        for (int x = 0; x < info.length; x++) {
            String[] bansTemp = newDownloadBanlist(info[x][0], Integer.parseInt(info[x][1]));
            ArrayList<String> temp = new ArrayList<String>();
            for (String ban : bansTemp) {
                temp.add(ban);
            }
            bans.addAll(temp);
        }
        return bans;
    }

    /**
	 * Downloads bans of the given user from the given server (New Banlist.nl style)
	 * 
	 * @param server The server to download the bans from
	 * @param userID The user ID of the user to download from (from the Banlist.nl server)
	 * @return Returns a String[] of bans
	 * @throws MalformedURLException Thrown when the given URL is invalid
	 * @throws IOExceptiion Thrown when there is an error from the server
	 */
    public String[] newDownloadBanlist(String server, int userID) throws MalformedURLException, IOException {
        URL downloadURL = new URL(server + "?u=" + userID);
        InputStream is = downloadURL.openStream();
        BufferedReader rdr = new BufferedReader(new InputStreamReader(is));
        StringBuffer sb = new StringBuffer();
        String line;
        while ((line = rdr.readLine()) != null) {
            sb.append(line + "\n");
        }
        String bansAsString = sb.toString();
        String[] bans = bansAsString.split("\n");
        return bans;
    }

    /**
	 * Returns the URLs (in order) for the given users.  If the user doesn't exist, the URL is return null
	 * 
	 * @param users String[] of Banlist.nl (forum) usernames
	 * @return A String[][] of String[] = { URL, USER ID }
	 * @throws MalformedURLException Thrown when the given URL is invalid
	 * @throws IOException Thrown when the server returns an error
	 */
    private String[][] getDownloadServerInfo(String[] users) throws MalformedURLException, IOException {
        String urlAddition = "";
        for (String s : users) {
            urlAddition += "u[]=" + s;
        }
        URL server = new URL(NEW_SERVER + urlAddition);
        HttpURLConnection c = (HttpURLConnection) server.openConnection();
        InputStream is = c.getInputStream();
        BufferedReader rdr = new BufferedReader(new InputStreamReader(is));
        StringBuffer sb = new StringBuffer();
        String line;
        while ((line = rdr.readLine()) != null) {
            sb.append(line + "\n");
        }
        String info = sb.toString();
        String[] usersInfo = info.split("\n");
        if (usersInfo.length == users.length) {
            String[][] serverInfo = new String[users.length][2];
            for (int x = 0; x < users.length; x++) {
                String[] data = usersInfo[x].split("\t");
                serverInfo[x][0] = data[4];
                serverInfo[x][1] = data[1];
            }
            return serverInfo;
        } else {
            String[][] serverInfo = new String[users.length][2];
            for (int x = 0; x < users.length; x++) {
                if (users[x].equals(usersInfo[x].split("\t")[0])) {
                    serverInfo[x][0] = usersInfo[x].split("\t")[4];
                    serverInfo[x][1] = usersInfo[x].split("\t")[1];
                } else {
                    serverInfo[x] = null;
                }
            }
            return serverInfo;
        }
    }

    /**
	 * Restores a banlist from the server
	 * 
	 * @param scripts Which banlist.nl scripts to restore from (old- uses Banlist.nl login, new- uses forum login)
	 * @throws IOException Thrown when the server has an error
	 * @throws SQLException Thrown when there is an error in the local database
	 * @throws IncorrectLoginException Thrown when the given login information is incorrect
	 * @throws BadTypeException Thrown when an invalid type of list is given
	 */
    public void restoreListFromSite(int scripts) throws IOException, SQLException, BadTypeException, IncorrectLoginException {
        switch(scripts) {
            case OLD_SCRIPT:
                String bans = downloadBans(OWN);
                String[] lines = bans.split("\n");
                for (String ban : lines) {
                    String[] data = ban.split("\t");
                    if (data[3].equals("0")) {
                        DB.instance().addBan(new Ban(data[0], data[2], data[1], LocalPanel.date(), LocalPanel.date(), Ban.BAN));
                    } else if (data[3].equals("2")) {
                        DB.instance().addBan(new Ban(data[0], data[2], data[1], LocalPanel.date(), LocalPanel.date(), Ban.INFO));
                    }
                }
                break;
            case NEW_SCRIPT:
                String[] self = { ProgramState.instance().getForumName() };
                ArrayList<String> newBans = newDownloadBanlists(self);
                for (String ban : newBans) {
                    String[] data = ban.split("\t");
                    DB.instance().addBan(new Ban(data[0], ProgramState.instance().getForumName(), data[2], LocalPanel.date(), LocalPanel.date(), Ban.BAN));
                }
                break;
            default:
                String defBans = downloadBans(OWN);
                String[] defLines = defBans.split("\n");
                for (String ban : defLines) {
                    String[] data = ban.split("\t");
                    if (data[3].equals("0")) {
                        DB.instance().addBan(new Ban(data[0], data[2], data[1], LocalPanel.date(), LocalPanel.date(), Ban.BAN));
                    } else if (data[3].equals("2")) {
                        DB.instance().addBan(new Ban(data[0], data[2], data[1], LocalPanel.date(), LocalPanel.date(), Ban.INFO));
                    }
                }
        }
    }
}
