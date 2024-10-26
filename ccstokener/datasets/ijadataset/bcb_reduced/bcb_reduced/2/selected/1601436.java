package au.reba.PlaylistManager.Player;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.net.InetAddress;
import java.net.InterfaceAddress;
import java.net.NetworkInterface;
import java.net.Socket;
import java.net.URI;
import java.net.URL;
import java.net.URLConnection;
import java.util.Enumeration;
import java.util.TreeMap;
import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import org.w3c.dom.Document;
import au.reba.PlaylistManager.PlaylistManager;
import au.reba.PlaylistManager.Util.Utils;
import au.reba.PlaylistManager.Util.XMLParseErrorHandler;

/**
 * Class to encapsulate configuration and running data for a VLC player
 * instance.<br>
 * <br>
 * This class spawns a single instance of the VLC player with VLC's http engine
 * as an extra interface on port 9597 to allow commands changing the state of 
 * the player to be sent to it and responses to be read.
 * 
 * TODO: make the http port number configurable
 * TODO: allow a playlist to be started on a particular song, passed in to the 
 * 		method when a playlist is first loaded
 * TODO: provide feedback to the main process about the currently playing song
 *
 * @author Reba Kearns
 */
public class VLCPlayer extends Player {

    private static final String NAME = "VLC";

    private PlayerState playerState = null;

    private static String localIpAddress;

    static {
        try {
            Socket local = null;
            InetAddress localAddr = null;
            try {
                local = new Socket("www.google.com", 80);
                localAddr = local.getLocalAddress();
                localIpAddress = localAddr.getHostAddress();
            } catch (Exception ex) {
            } finally {
                if (local != null) {
                    try {
                        local.close();
                    } catch (IOException ioe) {
                    }
                }
            }
        } catch (Exception e) {
            System.out.println(e.getMessage());
        }
    }

    private static final String extraInterfaceOpt = "--extraintf";

    private static final String httpInterfaceVal = "http";

    private static final String extraInterfaceValSeparator = ":";

    private static final String httpHostOpt = "--http-host";

    private static final String HTTP_PORT_NUMBER = "9597";

    private static final String httpHostVal = ((localIpAddress != null) ? localIpAddress : "localhost") + ":" + HTTP_PORT_NUMBER;

    private static final String vlcRandom = "random";

    private static final String vlcSongNode = "leaf";

    private static final String vlcId = "id";

    private static final String vlcUri = "uri";

    private static final String vlcCurrent = "current";

    /**
     * @param systemStartCommandLine May be full path name or just "vlc" if
     * user has their PATH variable setup to contain it.
     */
    public VLCPlayer(String systemStartCommandLine) {
        super(systemStartCommandLine, NAME);
    }

    /**
     * Invoke the system command to spawn an instance of the Player loading the
     * supplied m3u file
     *
     * @param m3uFileName the m3u file on the system to load on when the player
     *                      starts
     * @param shuffle whether or not the player should be started in shuffle
     *                  (random play) mode
     */
    public void startPlayer(String m3uFileName, boolean shuffle) throws Exception {
        changePlaylist(m3uFileName, shuffle);
    }

    /**
     * Load a new playlist into the existing player instance (or start a new
     * one if necessary) from the supplied m3u file
     *
     * @param newM3UFileName the new m3u file to load into the player
     * @param shuffle whether or not the player should be set to shuffle
     *                  (random play) mode
     */
    public void changePlaylist(String newM3UFileName, boolean shuffle) throws Exception {
        URL url;
        URLConnection conn;
        InputStreamReader rd;
        terminatePlayer();
        startPlayer();
        getPlayerStatus();
        if ((playerState.random && !shuffle) || (!playerState.random && shuffle)) {
            url = new URL("http://" + httpHostVal + "/requests/status.xml?command=pl_random");
            conn = url.openConnection();
            conn.connect();
            rd = new InputStreamReader(conn.getInputStream());
            rd.close();
        }
        newM3UFileName = newM3UFileName.replace('\\', '/');
        URI uri = new URI("file", "///" + newM3UFileName, null);
        url = new URL("http://" + httpHostVal + "/requests/status.xml?command=in_play&input=" + uri);
        conn = url.openConnection();
        conn.connect();
        rd = new InputStreamReader(conn.getInputStream());
        rd.close();
        getPlayerStatus();
    }

    /**
     * Terminate the current player instance. 
     * 
	 * @throws InterruptedException 
	 */
    public void terminatePlayer() {
        playerState = null;
        super.terminatePlayer();
    }

    /** 
     * Actually start the VLC player with no playlist
     * @throws Exception
     * 
     * TODO: Check for another running instance of the VLC player with a http 
     * interface using port HTTP_PORT_NUMBER, to avoid bind errors.
     */
    private void startPlayer() throws Exception {
        String extraintf = getExistingVLCExtraInterfaceConfig();
        if (Utils.nullOrEmptyString(extraintf)) extraintf = httpInterfaceVal; else if (!extraintf.contains(httpInterfaceVal)) extraintf += extraInterfaceValSeparator + httpInterfaceVal;
        String[] cmdLine = new String[5];
        cmdLine[0] = systemStartCommandLine;
        cmdLine[1] = extraInterfaceOpt;
        cmdLine[2] = extraintf;
        cmdLine[3] = httpHostOpt;
        cmdLine[4] = httpHostVal;
        String logStr = "Invoking player with '";
        for (int i = 0; i < cmdLine.length; i++) {
            logStr += ((i != 0) ? " " : "") + cmdLine[i];
        }
        logStr += "'";
        logger.progInfo(logStr);
        primaryProc = Runtime.getRuntime().exec(cmdLine);
        startProcessOutputThreads();
        Thread.sleep(1500);
    }

    /**
     * Reads the vlcrc file from the file system and returns the value for the 
     * extraintf variable if it is active (not commented out). 
     * 
     * @return null if file could not be read or value is commented out, otherwise, 
     * returns the value from the file.
     */
    private String getExistingVLCExtraInterfaceConfig() {
        String answer = null;
        File file = null;
        switch(PlaylistManager.OS) {
            case UNIX_LIKE:
                String userHomeDir = System.getenv("HOME");
                file = new File(userHomeDir + "/.config/vlc/vlcrc");
                if (!file.exists()) file = new File(userHomeDir + "/.vlc/vlcrc");
                break;
            case MAC:
                file = new File("HOME/Library/Preferences/VLC");
                break;
            case WIN:
                String OSName = System.getProperty("os.name");
                if (OSName.contains("95") || OSName.contains("98") || OSName.contains("ME")) file = new File("C:\\Windows\\Application Data\\vlc\\vlcrc"); else {
                    String username = System.getenv("USERNAME");
                    file = new File("C:\\Documents and Settings\\" + username + "\\Application Data\\vlc\\vlcrc");
                }
                break;
        }
        if ((file != null) && file.exists() && file.canRead()) {
            BufferedReader reader = null;
            try {
                reader = new BufferedReader(new FileReader(file));
                String line = reader.readLine();
                while (line != null) {
                    if (line.startsWith("extraintf")) {
                        int index = line.indexOf("=");
                        if (index != -1) answer = line.substring(index + 1);
                        break;
                    }
                    line = reader.readLine();
                }
            } catch (FileNotFoundException e) {
            } catch (IOException ex) {
            } finally {
                if (reader != null) {
                    try {
                        reader.close();
                    } catch (IOException ex) {
                    }
                }
            }
        }
        return answer;
    }

    /**
     * Query the current status of the player instance for such things as 
     * random/shuffle value, and instantiate the PlayerState object with the
     * info
     * 
     * @throws Exception
     */
    private void getPlayerStatus() throws Exception {
        playerState = new PlayerState();
        DocumentBuilderFactory dbf = DocumentBuilderFactory.newInstance();
        dbf.setValidating(false);
        dbf.setIgnoringElementContentWhitespace(true);
        dbf.setIgnoringComments(true);
        DocumentBuilder docBuild;
        try {
            docBuild = dbf.newDocumentBuilder();
            docBuild.setErrorHandler(new XMLParseErrorHandler());
        } catch (Exception e) {
            throw new Exception("Error: could not query player status: " + e.getMessage());
        }
        URL url = new URL("http://" + httpHostVal + "/requests/status.xml");
        URLConnection conn = url.openConnection();
        conn.connect();
        Document state = docBuild.parse(conn.getInputStream());
        String random = state.getElementsByTagName(vlcRandom).item(0).getFirstChild().getNodeValue().trim();
        playerState.random = Integer.valueOf(random).intValue() == 1;
    }

    /**
     * Helper class to store information about the current state of the VLC
     * instance that is currently running, such as random/shuffle setting, 
     * currently-playing song, position in the playlist etc.
     * 
     * @author Reba Kearns
     */
    private static class PlayerState {

        private boolean random;

        private String currentSongURI;

        private TreeMap<String, String> songNodeIds = new TreeMap<String, String>();
    }
}
