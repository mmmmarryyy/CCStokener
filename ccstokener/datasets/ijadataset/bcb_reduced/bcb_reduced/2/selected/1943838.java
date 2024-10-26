package cockfight.net;

import java.io.BufferedInputStream;
import java.io.BufferedOutputStream;
import java.io.BufferedWriter;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.FileWriter;
import java.io.IOException;
import java.net.MalformedURLException;
import java.net.URL;
import java.net.URLConnection;
import javax.swing.JOptionPane;
import cockfight.ui.Console;

/**
 * This class is used to save server settings locally and upload to an FTP
 * server.
 * 
 * @author William Wynn
 */
public class FtpServer {

    private String serverName, mapName, ipAddr, fileName, source;

    private int maxPlayers, curPlayers = 0;

    private String ftpServer = "ftp.freakycowbot.com";

    private final String user = "cockfight";

    private final String password = "cfight";

    private final String serverFile = "serverlist.txt";

    /**
	 * Upload a file to a FTP server (Master Server). A FTP URL is generated
	 * with the following syntax: ftp://user:password@host:port/filePath;type=i.
	 * ftp://cockfight:cfight@ftp.freakycowbot.com/file.txt/
	 * 
	 * @throws MalformedURLException
	 *             IOException on error.
	 */
    public void upload() throws MalformedURLException, IOException {
        if (ftpServer != null && fileName != null && source != null) {
            StringBuffer sb = new StringBuffer("ftp://");
            if (user != null && password != null) {
                sb.append(user);
                sb.append(':');
                sb.append(password);
                sb.append('@');
            }
            sb.append(ftpServer);
            sb.append('/');
            sb.append(fileName);
            sb.append(";type=i");
            BufferedInputStream bis = null;
            BufferedOutputStream bos = null;
            try {
                URL url = new URL(sb.toString());
                URLConnection urlc = url.openConnection();
                bos = new BufferedOutputStream(urlc.getOutputStream());
                bis = new BufferedInputStream(new FileInputStream(source));
                int i;
                while ((i = bis.read()) != -1) {
                    bos.write(i);
                }
            } finally {
                if (bis != null) try {
                    bis.close();
                } catch (IOException ioe) {
                    error();
                }
                if (bos != null) try {
                    bos.close();
                } catch (IOException ioe) {
                    error();
                }
            }
        } else {
            Console.println("Error with server data. Please restart server.");
        }
    }

    /**
	 * Download a file from the server. An FTP URL is generated with the
	 * following syntax: ftp://user:password@host:port/filePath;type=i.
	 * 
	 * @throws MalformedURLException
	 *             , IOException on error.
	 */
    public void download() throws MalformedURLException, IOException {
        String destination = "config" + File.separator + "servers" + File.separator + serverFile;
        if (ftpServer != null && destination != null) {
            StringBuffer sb = new StringBuffer("ftp://");
            if (user != null && password != null) {
                sb.append(user);
                sb.append(':');
                sb.append(password);
                sb.append('@');
            }
            sb.append(ftpServer);
            sb.append('/');
            sb.append("cockfight/" + serverFile);
            sb.append(";type=i");
            BufferedInputStream bis = null;
            BufferedOutputStream bos = null;
            try {
                URL url = new URL(sb.toString());
                URLConnection urlc = url.openConnection();
                bis = new BufferedInputStream(urlc.getInputStream());
                bos = new BufferedOutputStream(new FileOutputStream(new File(destination)));
                int i;
                while ((i = bis.read()) != -1) {
                    bos.write(i);
                }
            } finally {
                if (bis != null) try {
                    bis.close();
                } catch (IOException ioe) {
                    error();
                }
                if (bos != null) try {
                    bos.close();
                } catch (IOException ioe) {
                    error();
                }
            }
        } else {
            System.out.println("Input not available");
        }
    }

    /**
	 * Saves current status to the server file
	 */
    public void save() {
        String text = ipAddr + "\n" + serverName + "\n" + mapName + "\n" + curPlayers + "\n" + maxPlayers;
        try {
            BufferedWriter output = new BufferedWriter(new FileWriter(source));
            output.write(text);
            output.close();
        } catch (Exception e1) {
            new JOptionPane("Error Creating Config File! Permission Denied.");
        }
    }

    /**
	 * @param serverName
	 *            Name of current server
	 */
    public void setServerName(String serverName) {
        this.serverName = serverName;
    }

    /**
	 * @param mapName
	 *            Name of current map
	 */
    public void setMapName(String mapName) {
        this.mapName = mapName;
    }

    /**
	 * @param ipAddr
	 *            IP Address of server
	 */
    public void setIpAddr(String ipAddr) {
        this.ipAddr = ipAddr;
    }

    /**
	 * Increase number of players currently connected to server
	 */
    public void addPlayer() {
        curPlayers++;
    }

    /**
	 * Decrease number of players currently connected to server
	 */
    public void removePlayer() {
        curPlayers--;
    }

    /**
	 * @param maxPlayers
	 *            Max players allowed to connect to server
	 */
    public void setMaxPlayers(int maxPlayers) {
        this.maxPlayers = maxPlayers;
    }

    /**
	 * @param file
	 *            Name of server info file
	 */
    public void setFileName(String file) {
        fileName = "cockfight/servers/" + file + ".host";
        source = "config" + File.separator + file + ".host";
    }

    /**
	 * Error Popup when couldn't connect to Master Server
	 */
    public void error() {
        JOptionPane.showMessageDialog(null, "Could not connect to Master Server!\n" + "You may still connect to a server if you know the IP.\n" + "Try the server list again later.");
    }
}
