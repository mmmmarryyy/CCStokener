package collabed.network;

import collabed.util.Util;
import java.io.*;
import java.net.*;

public class Net {

    private static String debug = "NET";

    public static final byte PING = 0;

    public static final byte JOIN_GROUP = 1;

    public static final byte GROUP_ID = 2;

    public static final byte GROUP_NUMBER = 3;

    public static final byte GROUP_INFO = 4;

    public static final byte GROUPS = 5;

    public static final byte IS_GROUP_HOSTED = 6;

    public static final byte SET_ID_INFO = 7;

    public static final byte REGISTER_RENDEZVOUS = 11;

    public static final byte SEND_RENDEZVOUS = 12;

    public static final byte SEND_RENDEZVOUS_IDS = 13;

    /** The Key of the first message of every GroupClient. **/
    public static final MessageKey JOINING_KEY = new MessageKey("@peer.joIning");

    /** The Key of the last message of every GroupClient. **/
    public static final MessageKey LEAVING_KEY = new MessageKey("@peer.leAving");

    /** ID for sending a MessageEvent to all group members (inclusive). **/
    public static final int TO_ALL = -1;

    /** ID for sending a MessageEvent to all group members (exclusive). **/
    public static final int TO_OTHERS = -2;

    /** The amount of time to block for a requestNow method call. **/
    private static final int REQUEST_TIMEOUT = 5000;

    /** The String used for ignore a password since a one is always expected. **/
    public static final String NULL_PW = "";

    /**
    * Calls {@link requestNow(byte, String, int, String, Object)}
    * with null as the optional Object input.
    **/
    public static Object requestNow(byte serverRequest, String host, int port, String serverPW) {
        return requestNow(serverRequest, host, port, serverPW, null);
    }

    /** 
    * Opens a socket with the GroupServer residing at <b>host:port</b> to
    * <em>request</em> some action to be taken immediately, passing along the
    * <em>input</em> needed to perform this action, returning the result and
    * closing the socket.
    *
    * @return  The Object sent by the Server in response to the action or
    *          null if a problem occurs during the request.
    **/
    public static Object requestNow(byte serverRequest, String host, int port, String serverPW, Object input) {
        Socket s = null;
        Object response = null;
        Util.debugln(debug, "REQUEST NOW TYPE " + serverRequest);
        try {
            s = new Socket(host, port);
            ObjectOutputStream out = new ObjectOutputStream(new BufferedOutputStream(s.getOutputStream()));
            s.setKeepAlive(true);
            s.setTcpNoDelay(true);
            s.setSoTimeout(REQUEST_TIMEOUT);
            out.writeObject(serverPW);
            Util.debugln(debug, "Net: SENT sPW='" + serverPW + "'");
            out.writeByte(serverRequest);
            Util.debugln(debug, "Net: SENT REQUEST - " + serverRequest);
            if (input != null) {
                out.writeObject(input);
                Util.debugln(debug, "Net: SENT INPUT='" + input + "'");
            }
            out.flush();
            ObjectInputStream in = new ObjectInputStream(new BufferedInputStream(s.getInputStream()));
            response = in.readObject();
            Util.debugln(debug, "Net: READ RESPONSE='" + response + "'");
        } catch (Exception e) {
            Util.error(Thread.currentThread().getName(), "Problem with requestNow; " + "serverRequest=" + serverRequest, e);
        }
        if (s != null) try {
            s.close();
        } catch (IOException ioe) {
            ioe.printStackTrace();
        }
        return response;
    }

    /**
    * Get the local host address and return it.
    * @return the IP address of the local host and if there is an error it
    *         returns "localhost".
    **/
    public static String getMyLocalIP() {
        try {
            return InetAddress.getLocalHost().getHostAddress();
        } catch (UnknownHostException e) {
            return "localhost";
        }
    }

    /** http://popper.cs-i.brandeis.edu:8088/kgg/demo/ip.servlet **/
    public static final String IPSERVER = "http://popper.cs-i.brandeis.edu:8088/kgg/demo/ip.servlet";

    /**
    * Tries to return your Global IP address by openning a connection with a
    * servlet on our web server that generates a page that only contains the
    * IP address of the connected client.
    * @return Your global IP address or null if any problems occuring while
    *         trying to determine your address.
    **/
    public static String getMyGlobalIP() {
        try {
            URL url = new URL(IPSERVER);
            HttpURLConnection con = (HttpURLConnection) url.openConnection();
            BufferedReader in = new BufferedReader(new InputStreamReader(con.getInputStream()));
            String ip = in.readLine();
            in.close();
            con.disconnect();
            return ip;
        } catch (Exception e) {
            return null;
        }
    }
}
