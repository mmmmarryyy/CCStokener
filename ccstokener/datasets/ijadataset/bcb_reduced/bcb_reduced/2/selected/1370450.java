package org.pbdb.manager;

import java.io.*;
import java.net.*;
import java.util.*;

public class PBCommsManager extends Thread {

    private static final String RETRIEVE_PLIST = "plist";

    private static final int NORMAL = 0;

    private static final int RETRIEVE_PLIST_MODE = 1;

    private static final int RETRIEVE_PLIST_AND_CAPTURE_MODE = 2;

    private int _mode = 0;

    private Socket _socket = null;

    private BufferedWriter _out = null;

    private BufferedReader _in = null;

    private boolean _gogogo = false;

    private PBManagerConfig _config = null;

    private boolean _plistSent = false;

    private boolean _waiting = false;

    private Vector _playerList = new Vector();

    public PBCommsManager(PBManagerConfig config) {
        try {
            _config = config;
            System.out.println("Connecting to '" + config.getIPAddress() + "' on port " + config.getPort());
            _socket = new Socket(config.getIPAddress(), config.getPort());
            _out = new BufferedWriter(new OutputStreamWriter(_socket.getOutputStream()));
            _in = new BufferedReader(new InputStreamReader(_socket.getInputStream()));
            start();
        } catch (Exception e) {
            System.out.println("Failed to connect to host");
            System.exit(1);
        }
    }

    private String getDateTimeString() {
        long time = System.currentTimeMillis();
        return (Long.toHexString(time));
    }

    private String stripAndChange(String source) {
        source = source.substring(0, source.indexOf("."));
        source += ".jpg";
        return (source);
    }

    private String convertFile(String source) {
        try {
            String cmdLine = _config.getPBCTOJPGLocation() + "/pbctojpg -q 75 " + source;
            Runtime rt = Runtime.getRuntime();
            Process p = rt.exec(cmdLine);
            p.waitFor();
        } catch (Exception e) {
            System.out.println("Unable to execute pbctojpg.exe ensure its in your pbdb directory");
            return (null);
        }
        try {
            File f = new File(source);
            f.delete();
            source = stripAndChange(source);
        } catch (Exception e) {
            System.out.println("Unable to delete file '" + source + "'");
            return (null);
        }
        return (source);
    }

    private void copyFile(String src, String dest) {
        System.out.println("Copying file '" + src + "' to '" + dest + "'");
        try {
            FileInputStream fIn = new FileInputStream(src);
            FileOutputStream fOut = new FileOutputStream(dest);
            byte[] buffer = new byte[65535];
            int bytesRead;
            while ((bytesRead = fIn.read(buffer)) != -1) {
                fOut.write(buffer, 0, bytesRead);
            }
            fIn.close();
            fOut.close();
            System.out.println("File copy complete");
        } catch (Exception e) {
            System.out.println("Failed to copy file!!");
            return;
        }
    }

    private void copyFile(String sourceDir, String sourceName, String dest, String nick) {
        String dateTimeString = getDateTimeString();
        String destFilename = dest + "_" + dateTimeString + ".pbc";
        String pbcDestFilename = destFilename;
        copyFile(sourceDir + sourceName, sourceName);
        copyFile(sourceDir + sourceName, _config.getDestinationDir() + "/pbc/" + destFilename);
        sourceName = convertFile(sourceName);
        if (sourceName != null) {
            destFilename = stripAndChange(destFilename);
            copyFile(sourceName, _config.getDestinationDir() + "/jpg/" + destFilename);
            File f = new File(sourceName);
            f.delete();
            HashMap data = new HashMap();
            data.put("Nickname", nick);
            data.put("WonId", dest);
            data.put("Filename", "jpg/" + destFilename);
            data.put("PBCFilename", "pbc/" + pbcDestFilename);
            System.out.println("Updating Website...");
            postRequest(_config.getLoggingURL() + "/logscreenshot.asp", data);
            System.out.println("Complete");
        }
    }

    public static String postRequest(String urlString, HashMap data) {
        String returnData = "";
        try {
            URL url = new URL(urlString);
            URLConnection connection = url.openConnection();
            connection.setDoOutput(true);
            connection.setDoInput(true);
            PrintWriter out = new PrintWriter(connection.getOutputStream());
            Object[] keySet = data.keySet().toArray();
            Object[] values = data.values().toArray();
            for (int count = 0; count < keySet.length; count++) {
                out.print(URLEncoder.encode((String) keySet[count]) + "=" + URLEncoder.encode((String) values[count]));
                if ((count + 1) < keySet.length) out.print("&");
            }
            out.close();
            BufferedReader in = new BufferedReader(new InputStreamReader(connection.getInputStream()));
            String inputLine;
            while ((inputLine = in.readLine()) != null) {
                returnData += inputLine;
            }
            in.close();
        } catch (Exception e) {
            e.printStackTrace();
            returnData = null;
        }
        return (returnData);
    }

    private synchronized void sendCommand(String text) {
        try {
            _out.flush();
            _out.write(text + "\r\n");
            _out.flush();
        } catch (Exception e) {
            System.out.println("ERROR: " + e);
            System.exit(1);
        }
    }

    private String getText() {
        try {
            return (_in.readLine());
        } catch (Exception e) {
            System.out.println("ERROR: " + e);
            System.exit(1);
        }
        return (null);
    }

    public synchronized Vector retrieveAuthenticatedPlayerList() throws InterruptedException {
        Vector results = null;
        if (_gogogo) {
            if (!_waiting) {
                _waiting = true;
                _plistSent = false;
                _mode = RETRIEVE_PLIST_MODE;
                sendCommand(RETRIEVE_PLIST);
                System.out.println("Sent waiting");
                wait();
                _waiting = false;
                System.out.println("Finished waiting");
                results = new Vector();
                System.out.println("Creating list of Authenticated players");
                for (int count = 0; count < _playerList.size(); count++) {
                    PBDBPlayerInfo player = (PBDBPlayerInfo) _playerList.get(count);
                    if (player.state.equalsIgnoreCase(PBDBPlayerInfo.AUTHENTICATED)) {
                        results.add(player);
                    }
                }
                System.out.println("Number of authenticated players=" + results.size());
            }
        }
        return (results);
    }

    public void retrieveAllScreenshots() {
        try {
            System.out.println("Retrieving all screenshots");
            if (_gogogo) {
                Vector players = retrieveAuthenticatedPlayerList();
                if (players.size() > 0) {
                    String commandString;
                    for (int index = 0; index < players.size(); index++) {
                        PBDBPlayerInfo player = ((PBDBPlayerInfo) players.get(index));
                        System.out.println("Taking random screenshot of '" + player.name + "'");
                        commandString = "ss " + player.pbid;
                        _plistSent = true;
                        sendCommand(commandString);
                    }
                } else {
                    System.out.println("No random shot taken - no players on server");
                }
            }
        } catch (InterruptedException e) {
        }
    }

    public void retrieveScreenshots() {
        try {
            System.out.println("Retrieving a single screenshots");
            if (_gogogo) {
                Vector players = retrieveAuthenticatedPlayerList();
                if (players.size() > 0) {
                    String commandString;
                    int index = (int) (Math.random() * players.size());
                    PBDBPlayerInfo player = ((PBDBPlayerInfo) players.get(index));
                    System.out.println("Taking random screenshot of '" + player.name + "'");
                    commandString = "ss " + player.pbid;
                    _plistSent = true;
                    sendCommand(commandString);
                } else {
                    System.out.println("No random shot taken - no players on server");
                }
            }
        } catch (InterruptedException e) {
        }
    }

    public void retrieveScreenshots(String ssn) {
        if (_gogogo) {
            String commandString;
            commandString = "ssn " + ssn;
            _plistSent = false;
            sendCommand(commandString);
        }
    }

    public void disconnect() {
        System.out.println("Disconnecting and shutting down");
        try {
            _socket.close();
        } catch (Exception e) {
        }
        System.exit(0);
    }

    private synchronized void parseList() {
        System.out.println("Parsing PLIST");
        boolean retrieved = false, retrieving = false;
        String inLine;
        _playerList.clear();
        while (!retrieved) {
            inLine = getText().trim();
            retrieved = (inLine.startsWith("Next ID ="));
            if ((retrieving) && (!retrieved) && (inLine.indexOf(' ') != -1)) {
                try {
                    PBDBPlayerInfo player = new PBDBPlayerInfo();
                    player.serverIp = inLine.substring(inLine.lastIndexOf(' ') + 1);
                    inLine = inLine.substring(0, inLine.lastIndexOf(' '));
                    player.state = inLine.substring(inLine.lastIndexOf("] ") + 2);
                    inLine = inLine.substring(0, inLine.lastIndexOf("] ") + 1);
                    player.frags = inLine.substring(inLine.lastIndexOf(' ') + 1);
                    inLine = inLine.substring(0, inLine.lastIndexOf(' '));
                    player.time = inLine.substring(inLine.lastIndexOf(' ') + 1);
                    inLine = inLine.substring(0, inLine.lastIndexOf(' '));
                    String ipAndIds = inLine.substring(inLine.lastIndexOf(' ') + 1);
                    inLine = inLine.substring(0, inLine.lastIndexOf(' '));
                    player.adr = ipAndIds.substring(0, ipAndIds.indexOf(','));
                    ipAndIds = ipAndIds.substring(ipAndIds.indexOf(',') + 1);
                    player.wonid = ipAndIds.substring(0, ipAndIds.indexOf(','));
                    ipAndIds = ipAndIds.substring(ipAndIds.indexOf(',') + 1);
                    player.hlid = ipAndIds;
                    player.name = inLine.substring(inLine.indexOf(' ')).trim();
                    inLine = inLine.substring(0, inLine.indexOf(' '));
                    player.pbid = inLine.trim();
                    _playerList.add(player);
                } catch (Exception e) {
                    System.out.println("Parse exception: ignoring...");
                }
            }
            if (inLine.startsWith("***REMOTE ")) {
                if (inLine.endsWith(RETRIEVE_PLIST)) retrieving = true; else break;
            }
        }
        System.out.println("PLIST parsed");
        notifyAll();
    }

    public void run() {
        String inLine;
        int inChar = 0;
        Vector players = new Vector();
        while (inChar != -1) {
            inLine = "";
            inChar = 0;
            while ((inChar != -1) && (inChar != 0x0D)) {
                try {
                    inChar = _in.read();
                    if ((inChar != -1) && (inChar != 0x0A) && (inChar != 0x0D)) {
                        inLine += new Character((char) inChar).charValue();
                        if (((char) inChar == ':') && (!_gogogo)) {
                            sendCommand(_config.getPBPassword());
                            System.out.println("Password accepted");
                            _gogogo = true;
                        }
                    }
                } catch (IOException e) {
                }
            }
            if (inLine.startsWith("Capture Denied (A) from")) {
                retrieveScreenshots();
            }
            if (inLine.indexOf("Completed Capture from ") != -1) {
                String id;
                String wonId;
                String nick;
                inLine = inLine.substring(inLine.indexOf("Completed Capture from "));
                inLine = inLine.substring(0, inLine.lastIndexOf(")"));
                inLine = inLine.substring(0, inLine.indexOf(")"));
                wonId = inLine.substring(inLine.indexOf("(") + 1);
                nick = inLine.substring(inLine.indexOf("from ") + 5, inLine.indexOf("(") - 1);
                if (!_config.getDisableScreenshotCataloguing()) {
                    copyFile(_config.getPBCaptureDirectory(), wonId + ".pbc", wonId, nick);
                    System.out.println("Capture received and processed...");
                }
                try {
                    if (_config.announceScreenshot()) RCONManager.say("Screenshot taken of '" + nick + "' and added to archive");
                } catch (Exception e) {
                }
            }
            if (_mode == RETRIEVE_PLIST_MODE) {
                parseList();
                _mode = NORMAL;
            }
        }
    }
}
