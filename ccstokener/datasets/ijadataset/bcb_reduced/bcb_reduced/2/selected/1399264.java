package net.sf.vlm;

import java.io.*;
import java.net.*;
import java.util.*;
import java.util.logging.*;

/**
 * The NotificationServer class is used for authentication, notification,
 * presence, requesting new SB sessions, etc. It connects to the official server
 * and performs many initializations between the client and the server.
 * 
 * @author wensi
 * @since Version 0.1
 */
class NotificationServer {

    /**
     * The constant string of the server's URL.
     */
    final String SERVER_URL = "messenger.hotmail.com";

    /**
     * The port used for VLM. It is always 1863.
     */
    final int SERVER_PORT = 1863;

    private Logger logger;

    private Socket hSocket;

    private BufferedReader in;

    private PrintWriter out;

    private Mediator mediator;

    private Dispatcher dispatcher;

    private long replyTime;

    private String username;

    private String password;

    private String challenge;

    private String initStatus = Command.NLN;

    /**
     * A vector of string containing the names of the groups.
     * 
     * @deprecated
     */
    private Vector<String> groups;

    /**
     * A vector of string containing the names of the contacts.
     * 
     * @deprecated
     */
    private Vector<String> contacts;

    /**
     * The constructor accepts an instance of the mediator and initializes the
     * logger.
     * 
     * @param mediator
     *            The mediator
     */
    NotificationServer(Mediator mediator) {
        this.mediator = mediator;
        logger = Client.getLogger(NotificationServer.class.getName());
    }

    /**
     * Setter of the username and password.
     * 
     * @param username
     * @param password
     */
    public void setLogin(String username, String password) {
        this.username = username;
        this.password = password;
    }

    /**
     * Getter of the username.
     * 
     * @return The username
     */
    public String getLogin() {
        return username;
    }

    /**
     * Getter of the replyTime.
     * 
     * @return The replyTime
     */
    public long getReplyTime() {
        return replyTime;
    }

    /**
     * Setter of the user's status.
     * 
     * @param status
     */
    public void setStatus(String status) {
        initStatus = status;
    }

    /**
     * Getter of the user's status.
     * 
     * @return The user's current status.
     */
    public String getStatus() {
        return initStatus;
    }

    public void updateStatus(int status) {
        String s = Command.NLN;
        switch(status) {
            case 0:
                s = Command.NLN;
                break;
            case 1:
                s = Command.BSY;
                break;
            case 2:
                s = Command.BRB;
                break;
            case 3:
                s = Command.AWY;
                break;
            case 4:
                s = Command.PHN;
                break;
            case 5:
                s = Command.LUN;
                break;
            case 6:
                s = Command.HDN;
                break;
            default:
                logger.warning("Error status code!");
        }
        out.print(Command.CHG(s));
        out.flush();
    }

    /**
     * This method changes the display name of a principal (possibly the user
     * him/herself).
     * 
     * @param contact
     *            The account name of the contact.
     * @param name
     *            The name which is going to be set.
     */
    public void changeName(String contact, String name) {
        try {
            out.print(Command.REA(contact, URLEncoder.encode(name, "UTF-8")));
        } catch (UnsupportedEncodingException e) {
            logger.warning("Exception URL-Encoding for the String: " + contact + " and the String: " + name);
        }
        out.flush();
    }

    /**
     * Setter of the challenge string.
     * 
     * @param challenge
     */
    public void setChallenge(String challenge) {
        this.challenge = challenge;
    }

    /**
     * A request to stop the dispatcher.
     * 
     * @param stop
     *            Whether to stop the dispatcher or not.
     */
    public void stopDispatcher(boolean stop) {
        if (dispatcher != null) dispatcher.stop(stop);
    }

    /**
     * Requests a switch board from the server.
     */
    public void requestSession() {
        out.print(Command.XFR());
        out.flush();
    }

    /**
     * Pings the server.
     */
    public void pingServer() {
        replyTime = System.currentTimeMillis();
        out.print(Command.PNG());
        out.flush();
    }

    /**
     * Disconnects from the server.
     * 
     * @throws IOException
     */
    public void disconnect() throws IOException {
        if (out != null) {
            out.print(Command.OUT());
            out.flush();
            logger.info("Connection with notification server is closed!");
        } else logger.warning("Socket is null when shutting down!");
        if (in != null) in.close();
        if (out != null) out.close();
        if (hSocket != null) hSocket.close();
    }

    /**
     * Connects to the server and performs the following operations: 1.
     * Initialize the dispatcher. 2. Check if the protocol version is supported.
     * 3. Redirect to the appropriate server. 4. Authenticate with the web
     * server. 5. Synchronize the contact list.
     * 
     * @throws IOException
     */
    public void connect() throws IOException {
        Socket socket = new Socket(SERVER_URL, SERVER_PORT);
        hSocket = socket;
        in = new BufferedReader(new InputStreamReader(socket.getInputStream(), "UTF-8"));
        out = new PrintWriter(new OutputStreamWriter(socket.getOutputStream(), "UTF-8"));
        dispatcher = new Dispatcher(mediator, socket, in, out, this);
        dispatcher.start();
        checkVersion();
        redirect();
    }

    /**
     * Getter of the contacts field.
     * 
     * @return contacts.
     * @deprecated
     */
    public Vector<String> getContacts() {
        return contacts;
    }

    /**
     * Getter of the groups field.
     * 
     * @return groups.
     * @deprecated
     */
    public Vector<String> getGroups() {
        return groups;
    }

    /**
     * Transfer to the server on the ip:port.
     * 
     * @param ip
     *            The IP of the server.
     * @param port
     *            The port to connect.
     */
    public void transfer(String ip, String port) {
        try {
            Socket socket = null;
            socket = new Socket(ip, Integer.parseInt(port));
            hSocket = socket;
            if (socket != null) logger.info("Redirected to " + ip + " port: " + port); else logger.severe("Cannot redirect to " + ip + " port: " + port);
            in = new BufferedReader(new InputStreamReader(socket.getInputStream(), "UTF-8"));
            out = new PrintWriter(new OutputStreamWriter(socket.getOutputStream(), "UTF-8"));
            dispatcher.setReader(in, out, socket);
        } catch (IOException e) {
            logger.severe("IOException when transfering server!");
        }
        checkVersion();
        out.print(Command.USR('I', username));
        out.flush();
    }

    /**
     * Check if the protocol version is supported.
     */
    private void checkVersion() {
        out.print(Command.VER());
        out.flush();
        out.print(Command.CVR(username));
        out.flush();
    }

    /**
     * Send the redirect command to the server.
     * 
     * @throws IOException
     */
    private void redirect() throws IOException {
        out.print(Command.USR('I', username));
        out.flush();
    }

    /**
     * Authenticate with the server. TWN ("Tweener") authentication is the way
     * MSN Messenger plugs into Microsoft's Passport Authentication framework.
     * Passport authentication is Microsoft's attempt to provide a "single
     * sign-on" system for all Internet services, from Messenger to on-line
     * banking. Some background information about it is available on MSDN.
     * Understanding Tweener doesn't require any background knowledge of
     * Passport, but you will need a basic understanding of the HyperText
     * Transfer Protocol version 1.0 or 1.1.
     * 
     * @throws IOException
     */
    public void authentication() throws IOException {
        URL nexus = new URL("https://nexus.passport.com/rdr/pprdr.asp");
        logger.fine("Connected to Nexus");
        HttpURLConnection connection = (HttpURLConnection) nexus.openConnection();
        logger.fine("Nexus response: " + connection.getResponseCode());
        String s = connection.getHeaderField("PassportURLs");
        s = "https://" + s.substring(s.indexOf("DALogin=") + 8);
        s = s.substring(0, s.indexOf(','));
        logger.fine("Redirected URL: " + s);
        URL url = new URL(s);
        connection = (HttpURLConnection) url.openConnection();
        String header = "Passport1.4 OrgVerb=GET,OrgURL=http%3A%2F%2F" + "messenger%2Emsn%2Ecom,sign-in=" + username + ",pwd=" + password + "," + challenge;
        connection.setRequestProperty("Authorization", header);
        int code = connection.getResponseCode();
        logger.info("Authentication response: " + code);
        if (code == 302) {
            String location = connection.getHeaderField("Location");
            url = new URL(location);
            connection = (HttpURLConnection) url.openConnection();
            connection.setRequestProperty("Authorization", header);
            code = connection.getResponseCode();
        }
        switch(code) {
            case 200:
                logger.fine("Authentication Success: " + code);
                s = connection.getHeaderField("Authentication-Info");
                logger.fine("Authentication Info: " + s);
                String ticket;
                ticket = s.substring(s.indexOf('\'') + 1, s.lastIndexOf('\''));
                logger.fine("Ticket: " + ticket);
                out.print(Command.USR('S', ticket));
                out.flush();
                break;
            case 401:
                logger.severe("Authentication Error: " + code);
                Error.translateError("401");
                break;
            default:
                logger.severe("Authentication Error: " + code);
        }
        connection.disconnect();
    }

    /**
     * Retrieve (Synchronize) the contact list with the server.
     * 
     * @throws IOException
     */
    public void synchronization() throws IOException {
        out.print(Command.SYN(0));
        out.flush();
    }
}
