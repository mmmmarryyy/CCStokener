package net.sf.isnake.server;

import java.io.IOException;
import java.io.PrintWriter;
import java.net.InetAddress;
import java.net.NetworkInterface;
import java.net.ServerSocket;
import java.net.Socket;
import java.net.SocketException;
import java.net.SocketTimeoutException;
import java.net.URL;
import java.net.URLConnection;
import java.net.UnknownHostException;
import java.util.Enumeration;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/** Web interface server of isnake. Initially it informs the central web server that it is online.
 * Later on it responds its status for each connection made to it.
 *
 *@author Suraj Sapkota [ ssapkota<at>gmail<dot>com ]
 *@version $Id: StatusServer.java 130 2008-04-28 16:28:12Z sursata $
 */
public class StatusServer extends Thread {

    Logger log = LoggerFactory.getLogger(StatusServer.class);

    private String CentralWebServer = "http://isnake.sourceforge.net/addnewgameserver.php";

    private String LocalGameServer = null;

    private int GameServerPort = 9669;

    private int GameStatusServerPort = 9670;

    private boolean stop = false;

    private ServerCore core = null;

    /** Creates a new instance of StatusServer */
    public StatusServer(int Port, ServerCore core) {
        this.GameStatusServerPort = Port;
        this.LocalGameServer = null;
        this.core = core;
        Enumeration netInterfaces = null;
        try {
            System.out.print("Determining this server public IP...");
            System.out.flush();
            netInterfaces = NetworkInterface.getNetworkInterfaces();
            if (netInterfaces == null) throw new SocketException("No Interfaces found.");
            while (netInterfaces.hasMoreElements()) {
                NetworkInterface ni = (NetworkInterface) netInterfaces.nextElement();
                InetAddress ip = (InetAddress) ni.getInetAddresses().nextElement();
                if (!ip.isSiteLocalAddress() && !ip.isLoopbackAddress() && ip.getHostAddress().indexOf(":") == -1) {
                    this.LocalGameServer = ip.getHostAddress();
                    break;
                } else {
                    this.LocalGameServer = null;
                }
            }
            if (this.LocalGameServer == null) throw new SocketException("Public IP not found.");
        } catch (SocketException ex) {
            System.out.println(" ==> FAILED.  Set to local IP [ 127.0.0.1 ]");
            this.LocalGameServer = "127.0.0.1";
            return;
        }
        System.out.println(" ==> OK  [ " + this.LocalGameServer + " ]");
    }

    public boolean sendInfo(int LocalGameServerPort) {
        this.GameServerPort = LocalGameServerPort;
        URL url = null;
        System.out.println("Connecting to Game server Manager...");
        System.out.flush();
        try {
            url = new URL(CentralWebServer.concat("?hostname=" + this.LocalGameServer + "&port=9670&location=Kathmandu&gameservername=iSnake Game Server"));
            URLConnection connection = url.openConnection();
            connection.setConnectTimeout(3000);
            connection.connect();
            connection.getContent();
            System.out.println("Connected to Game Server Manager: [" + this.CentralWebServer + "]");
            System.out.println("Nourishing with following info:\n\t hostname=" + this.LocalGameServer + "\n\t port=9670&location=Dhulikhel\n\t gameservername=Sata");
            return true;
        } catch (IOException ex) {
            System.out.println("Cannot connect to Game Server Manager: [" + this.CentralWebServer + "]");
            return false;
        }
    }

    public boolean sendInfo(int LocalGameServerPort, String GSMServer) {
        this.CentralWebServer = GSMServer;
        return this.sendInfo(LocalGameServerPort);
    }

    public void run() {
        try {
            ServerSocket server = new ServerSocket(GameStatusServerPort);
            server.setSoTimeout(1000);
            Socket soc = null;
            while (!isStop()) {
                try {
                    soc = server.accept();
                } catch (SocketTimeoutException e) {
                    if (this.isStop()) {
                        return;
                    }
                }
                if (soc != null) {
                    log.info(soc.getRemoteSocketAddress().toString().substring(1).split(":")[0] + " is requesting Server Information.");
                    PrintWriter out = new PrintWriter(soc.getOutputStream());
                    out.write("<?xml version=\"1.0\" encoding=\"UTF-8\"?>");
                    out.write("<iSnake>");
                    out.write("<GameServerData>");
                    try {
                        InetAddress a = InetAddress.getLocalHost();
                        out.write("<GameServerAddress>");
                        out.write(this.LocalGameServer);
                        out.write("</GameServerAddress>");
                    } catch (UnknownHostException ex) {
                    }
                    out.write("<GameServerPort>");
                    out.write("" + this.GameServerPort);
                    out.write("</GameServerPort>");
                    out.write("<NoOfPlayersOnline>");
                    out.write("" + core.getPlayerCount());
                    out.write("</NoOfPlayersOnline>");
                    out.write("<GameServerStatus>");
                    out.write("" + (core.isGameLock() ? "Running" : "Waiting"));
                    out.write("</GameServerStatus>");
                    out.write("</GameServerData>");
                    out.write("</iSnake>");
                    out.flush();
                    soc.close();
                    soc = null;
                }
            }
        } catch (IOException ex) {
            log.error("Cannot start \"status server\" at " + this.GameStatusServerPort);
            ex.printStackTrace();
        }
    }

    public boolean isStop() {
        return stop;
    }

    public void setStop(boolean stop) {
        this.stop = stop;
    }
}
