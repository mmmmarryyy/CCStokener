package net;

import util.Util;
import java.net.URL;
import java.net.Socket;
import java.net.ServerSocket;
import java.net.InetAddress;
import java.net.UnknownHostException;
import java.net.HttpURLConnection;
import java.io.InputStream;
import java.io.OutputStream;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.io.IOException;
import java.lang.SecurityException;

public class NetUtil {

    /** http://popper.cs-i.brandeis.edu:8088/kgg/demo/ip.servlet **/
    public static final String IPSERVER = "http://popper.cs-i.brandeis.edu:8088/kgg/demo/ip.servlet";

    public static final String NULL_PW = "";

    public static final int REQUEST_TIMEOUT = 5000;

    private static final int MAX_PORT = (256 * 256) - 1;

    private static int lastIssuedPort = 0;

    private static Object This = (Object) NetUtil.class;

    /**
    * Tries to open a ServerSocket on port, return false if it fails and
    * true otherwise. Closes the ServerSocket before returning.
    **/
    public static boolean isAvailablePort(int port) {
        boolean available = false;
        try {
            ServerSocket ss = new ServerSocket(port);
            available = true;
            ss.close();
        } catch (SecurityException e) {
            Util.syslog(This, "Security blocked port check.", e);
        } catch (IOException e) {
            Util.syslog(This, "An I/O error occured when opening a socket.", e);
        }
        return available;
    }

    /**
    * Checks the ports between min and max for an available port using
    * {@link getAvailabePort(int)} as the test and return the first it finds.
    * If none is found then it returns -1
    **/
    public static int getAvailablePort(int min, int max) {
        int p = min, newMax = -1;
        if (min <= lastIssuedPort && lastIssuedPort < max) {
            p = lastIssuedPort + 1;
            newMax = lastIssuedPort;
        }
        while (!isAvailablePort(p)) if (p < max) p++; else if (p == max && newMax != -1) {
            p = min;
            max = newMax;
            newMax = -1;
        } else return -1;
        return p;
    }

    /** Returns an available port. **/
    public static int getAvailablePort() {
        int port = -1;
        try {
            ServerSocket ss = new ServerSocket();
            port = ss.getLocalPort();
            ss.close();
        } catch (SecurityException e) {
            Util.syslog(This, "Security blocked port check.", e);
        } catch (IOException e) {
            Util.syslog(This, "An I/O error occured when opening a socket.", e);
        }
        return port;
    }

    /** Scans all ports and print out their availability status. **/
    public static void scan() {
        scan(0, (MAX_PORT));
    }

    /**
    * Scans all the ports between min and max (inclusive) and print out their
    * availability status.
    **/
    public static void scan(int min, int max) {
        int free = 0, nfree = 0;
        for (; min <= max; min++) if (!isAvailablePort(min)) {
            System.out.println("Not Available: " + min);
            nfree++;
        } else free++;
        System.out.println("Available: " + free + "\nNot Available: " + nfree + "\nTotal: " + (free + nfree));
    }

    /**
    * Tries to return <b>port</b> as an int but throws a
    * NumberFormatException if port is not a number or in the range of ports.
    **/
    public static int parsePort(String port) throws NumberFormatException {
        int p = port == null ? -1 : Integer.parseInt(port.trim());
        if (p >= 0 && MAX_PORT >= p) return p; else throw new NumberFormatException("Out of range! Ports: 0 - 65535");
    }

    /**
    * Get the local host address and return it.
    * @return the IP address of the local host and if there is an error it
    *         returns null
    **/
    public static InetAddress getMyLocalIP() {
        try {
            return InetAddress.getLocalHost();
        } catch (UnknownHostException e) {
            return null;
        }
    }

    /**
    * Tries to return your Global IP address by openning a connection with a
    * servlet on our web server that generates a page that only contains the
    * IP address of the connected client.
    * @return Your global IP address or null if any problems occuring while
    *         trying to determine your address.
    **/
    public static InetAddress getMyGlobalIP() {
        try {
            URL url = new URL(IPSERVER);
            HttpURLConnection con = (HttpURLConnection) url.openConnection();
            BufferedReader in = new BufferedReader(new InputStreamReader(con.getInputStream()));
            String ip = in.readLine();
            in.close();
            con.disconnect();
            return InetAddress.getByName(ip);
        } catch (Exception e) {
            Util.syslog(This, "getMyGobalIP()", e);
            return null;
        }
    }

    /**
    * Checks if you are using NAT (Network Address Translation) by comparing
    * your local ip address and your global ip address.
    * @return true if your local IP address is not equal to your global IP 
    *         address and false otherwise.
    **/
    public static boolean usingNAT() {
        return !getMyLocalIP().equals(getMyGlobalIP());
    }

    /**
    * Checks if the <b>ip</b> is in any of the special use ranges.
    * <br>
    * "Private Use" IP addresses: InetAddress.isSiteLocalAddress()<br>
    * &nbsp;&nbsp; 10.0.0.0 - 10.255.255.255
    * &nbsp;&nbsp; 172.16.0.0 - 172.31.255.255
    * &nbsp;&nbsp; 192.168.0.0 - 192.168.255.255
    * <br>
    * "Autoconfiguration" IP Addresses: InetAddress.isLinkLocalAddress()<br>
    * &nbsp;&nbsp; 169.254.0.0 - 169.254.255.255
    * <br>
    * "Loopback" IP addresses: InetAddress.isLoopbackAddress()<br>
    * &nbsp;&nbsp; 127.0.0.0 - 127.255.255.255
    * <br> 
    * Multicast IP addresses: InetAddress.isMulticastAddress()<br>
    * &nbsp;&nbsp; 224.0.0.0 - 239.255.255.255
    *
    * @return true if the IP address is in any of the IANA specified special
    *          ranges and false otherwise.
    **/
    public static boolean isSpecialUseIP(InetAddress ip) {
        return (ip.isSiteLocalAddress() || ip.isLinkLocalAddress() || ip.isLoopbackAddress() || ip.isMulticastAddress());
    }

    public static boolean inSameScope(InetAddress ip1, InetAddress ip2) {
        return (ip1 != null && ip2 != null && ((ip1.isSiteLocalAddress() && ip2.isSiteLocalAddress()) || (ip1.isLinkLocalAddress() && ip2.isLinkLocalAddress()) || (ip1.isLoopbackAddress() && ip2.isLoopbackAddress())));
    }

    public static boolean isGlobalIP(InetAddress ip) {
        return ip != null && !isSpecialUseIP(ip) || ip.isMCGlobal();
    }

    public static boolean isGlobalIP(String ip) {
        try {
            return isGlobalIP(InetAddress.getByName(ip));
        } catch (Exception e) {
            return false;
        }
    }

    public static void closeSocket(Socket s, InputStream in, OutputStream out) {
        try {
            if (out != null) out.close();
        } catch (IOException e) {
            Util.syslog(This, "Unclean close: " + out, e);
        }
        try {
            if (in != null) in.close();
        } catch (IOException e) {
            Util.syslog(This, "Unclean close: " + in, e);
        }
        try {
            if (s != null) s.close();
        } catch (IOException e) {
            Util.syslog(This, "Unclean close: " + s, e);
        }
    }

    /**
    * Calls {@link immediateRequest(String, int, String, byte, Object)}
    * with null as the optional Object input.
    **/
    public static Object immediateRequest(String host, int port, String serverPW, byte serverAction) {
        return immediateRequest(host, port, serverPW, serverAction, null);
    }

    /** 
    * Opens a socket with the GroupServer residing at <b>host:port</b> to
    * request some <b>action</b> be taken immediately, passing along the
    * <b>input</b> needed to perform this action, returning the result and
    * closing the socket.
    *
    * @return  The Object sent by the Server in response to the action or
    *          null if a problem occurs during the request.
    **/
    public static Object immediateRequest(String host, int port, String serverPW, byte serverAction, Object input) {
        Socket s = null;
        ObjectOutputStream out = null;
        ObjectInputStream in = null;
        Object response = null;
        try {
            s = new Socket(host, port);
            out = new ObjectOutputStream(s.getOutputStream());
            in = new ObjectInputStream(s.getInputStream());
            s.setKeepAlive(true);
            s.setTcpNoDelay(true);
            s.setSoTimeout(REQUEST_TIMEOUT);
            out.writeObject(serverPW);
            out.writeByte(serverAction);
            if (input != null) out.writeObject(input);
            out.flush();
            response = in.readObject();
        } catch (Exception e) {
            Util.syslog(This, "Problem with an immediateRequest; " + "serverAction=" + serverAction, e);
        }
        closeSocket(s, in, out);
        return response;
    }

    /**
    * Returns whether or not info's group exists at the host specified by info.
    **/
    public static Boolean isRendezvous(RendezvousInfo info) {
        return isRendezvous(info, info.getGroupname());
    }

    /**
    * Returns whether or not the group exists at the host specified by info.
    **/
    public static Boolean isRendezvous(RendezvousInfo info, String group) {
        return (Boolean) NetUtil.immediateRequest(info.getHost(), info.getPort(), info.getServerPassword(), GroupServer.IS_GROUP_HOSTED, group);
    }

    public static Boolean isRendezvous(String user, String group, String host, int port, String serverPW) {
        return (Boolean) NetUtil.immediateRequest(host, port, serverPW, GroupServer.IS_GROUP_HOSTED, group);
    }

    public static Boolean isRendezvousMember(String user, String group, String host, int port, String sPW) {
        return (Boolean) NetUtil.immediateRequest(host, port, sPW, GroupServer.IS_CLIENT_NAME_USED, new String[] { group, user });
    }

    /**
    * Returns whether or not mem is a member of info's group,
    * assuming the group exists.
    **/
    public static Boolean isRendezvousMember(RendezvousInfo info, String mem) {
        return (Boolean) NetUtil.immediateRequest(info.getHost(), info.getPort(), info.getServerPassword(), GroupServer.IS_CLIENT_NAME_USED, new String[] { info.getGroupname(), mem });
    }

    /**
    * Returns whether or not the info's user is a member of the info's group,
    * assuming the group exists.
    **/
    public static Boolean isRendezvousMember(RendezvousInfo info) {
        return isRendezvousMember(info, info.getUsername());
    }

    public static boolean isLegalName(String name) {
        return (name != null && !name.equals("") && name.indexOf("\\") == -1 && name.indexOf("/") == -1 && name.indexOf("*") == -1);
    }

    public static void main(String[] args) throws Exception {
        String acts = "actions:\n  help, quit, send [h p pw s i] ";
        System.out.print(acts + "\n\n>>");
        java.io.BufferedReader reader = new java.io.BufferedReader(new java.io.InputStreamReader(System.in));
        String line;
        String[] split;
        while ((line = reader.readLine()) != null) {
            split = line.split(" ");
            if (split[0].equalsIgnoreCase("help")) System.out.print(acts); else if (split[0].equalsIgnoreCase("quit")) System.exit(0); else if (split[0].equalsIgnoreCase("send")) System.out.println(immediateRequest(split[1], parsePort(split[2]), split[3], Byte.parseByte(split[4]), split[5])); else if (split[0].equalsIgnoreCase("sendA")) System.out.println(Util.arrayToString((Object[]) immediateRequest(split[1], parsePort(split[2]), split[3], Byte.parseByte(split[4]), split[5])));
            System.out.print("\n\n>>");
        }
    }
}
