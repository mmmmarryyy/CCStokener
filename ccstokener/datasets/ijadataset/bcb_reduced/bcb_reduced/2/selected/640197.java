package com.infocetera.util;

import java.applet.*;
import java.awt.*;
import java.io.*;
import java.net.*;
import java.util.Hashtable;

/**
 * Class SocketApplet _more_
 *
 *
 * @author IDV Development Team
 * @version $Revision: 1.3 $
 */
public abstract class SocketApplet extends IfcApplet implements Runnable {

    /** _more_ */
    public static final String MSG_CLOSE = "CLOSE";

    /** _more_ */
    public static final String MSG_STATE = "STATE";

    /** _more_ */
    public static final String MSG_ERROR = "ERROR";

    /** _more_ */
    public static final String MSG_MESSAGE = "MESSAGE";

    /** _more_ */
    public static final String MSG_LOGIN = "LOGIN";

    /** _more_ */
    public static final String ATTR_ID = "id";

    /** _more_ */
    public static final String ATTR_INTERVAL = "interval";

    /** _more_ */
    public static final String ATTR_FROM = "from";

    /** _more_ */
    public static final String ATTR_TYPE = "type";

    /** _more_ */
    public static final String ATTR_USERID = "userid";

    /** _more_ */
    public static final String ATTR_USERNAME = "username";

    /** _more_ */
    public static boolean debug;

    /** _more_ */
    protected Hashtable properties = new Hashtable();

    /** _more_ */
    public String filePrefix = "";

    /**
     */
    public static SocketApplet theApplet = null;

    /**
     *  Is this applet in polling mode (as opposed to having a persistent socket
     *  connection)
     */
    private boolean polling = false;

    /**
     *  When in polling mode this keeps track of the number of  consecutive  errors
     */
    private int pollErrorCount = 0;

    /**
     *  This is the url which we write messages to when we are in polling mode.
     */
    private String pollWriteUrl;

    /**
     *  This is the template used to create the post data for writing messages.
     */
    private String pollPostTemplate;

    /**
     *  This is the url which we read messages from when we are in polling mode.
     */
    private String pollReadUrl;

    /**
     *  This is how long (in milliseconds) we sleep between polls to the server
     *  when in polling mode.
     */
    private int pollInterval;

    /**
     *  This keeps track of the last message id (if there was one).
     *  We use it on our poll based requests.
     */
    private String lastMsgId = null;

    /**
     *  If we are in socket mode this is the socket used to connect to the server
     */
    private Socket socket;

    /**
     *  The InputStream we listen in on for server messages.
     */
    private DataInputStream inputStream;

    /**
     *  The OutputStream to write messages to the server
     */
    private DataOutputStream outputStream;

    /** _more_ */
    static final int BUFFER_SIZE = 1000000;

    /** _more_ */
    byte[] mainBuffer = new byte[BUFFER_SIZE];

    /** _more_ */
    byte[] extraBuffer = new byte[BUFFER_SIZE];

    /** _more_ */
    public String myid = "";

    /**
     *  The name of the current user
     */
    public String userName;

    /** _more_ */
    public String userId;

    /** _more_ */
    protected String room;

    /** _more_ */
    protected String roomName;

    /**
     *  An identifier for the connection to the server
     */
    protected String sessionId;

    /**
     *  This gets set to true if the initialization failed
     */
    protected boolean initFailed = false;

    /** _more_ */
    private boolean isConnected = true;

    /** _more_ */
    Thread runThread;

    /** _more_ */
    String chatPort;

    /** _more_ */
    int port;

    /**
     * _more_
     */
    public SocketApplet() {
        theApplet = this;
    }

    /**
     * _more_
     *
     * @return _more_
     */
    public boolean shouldMakeConnection() {
        return true;
    }

    /**
     * _more_
     */
    public void initInner() {
        filePrefix = getProperty("fileprefix");
        room = getProperty("channel", "BADID");
        roomName = getProperty("channelname", room);
        sessionId = getProperty("sessionid", "BADID");
        userId = userName = getProperty("username", "ANON");
        pollInterval = getProperty("pollinterval", 2000);
        polling = getProperty("polling", false);
        if (polling) {
            pollPostTemplate = getProperty("pollposttemplate");
            pollWriteUrl = getProperty("pollwriteurl");
            pollReadUrl = getProperty("pollreadurl");
            if ((pollWriteUrl == null) || (pollReadUrl == null)) {
                errorMsg("No poll url given");
            }
        }
        chatPort = getProperty("port");
        if (chatPort != null) {
            try {
                port = Integer.decode(chatPort).intValue();
            } catch (Exception exc) {
                errorMsg("Initialization error " + exc);
            }
        }
    }

    /**
     * _more_
     *
     * @param name _more_
     *
     * @return _more_
     */
    public String getProperty(String name) {
        String value = (String) properties.get(name);
        if (value == null) {
            value = getParameter(name);
        }
        return value;
    }

    /**
     * _more_
     *
     * @param name _more_
     * @param dflt _more_
     *
     * @return _more_
     */
    public String getProperty(String name, String dflt) {
        String value = getProperty(name);
        return ((value != null) ? value : dflt);
    }

    /**
     * _more_
     *
     * @param name _more_
     * @param dflt _more_
     *
     * @return _more_
     */
    public int getProperty(String name, int dflt) {
        String value = getProperty(name);
        if (value == null) {
            return dflt;
        }
        try {
            return new Integer(value).intValue();
        } catch (Exception exc) {
            errorMsg("Initialization error for:" + name + " " + exc);
        }
        return dflt;
    }

    /**
     * _more_
     *
     * @param name _more_
     * @param dflt _more_
     *
     * @return _more_
     */
    public boolean getProperty(String name, boolean dflt) {
        String value = getProperty(name);
        if (value == null) {
            return dflt;
        }
        try {
            return new Boolean(value).booleanValue();
        } catch (Exception exc) {
            errorMsg("Initialization error for:" + name + " " + exc);
        }
        return dflt;
    }

    /**
     * _more_
     *
     * @param name _more_
     * @param dflt _more_
     *
     * @return _more_
     */
    public Color getProperty(String name, Color dflt) {
        String value = getProperty(name);
        if (value == null) {
            return dflt;
        }
        return GuiUtils.getColor(value);
    }

    /**
     *  Just a hook so derived classes can display some text to the user.
     *
     * @param s _more_
     */
    public void putText(String s) {
    }

    /**
     * _more_
     */
    public void run() {
        if (polling) {
            readPolling();
        } else {
            readSocket();
        }
    }

    /**
     *  Read in any input from the given url connection.
     *  The buffer is used so we don't have to create on
     *  locally everytime we call this.
     *
     * @param connection _more_
     * @param buffer _more_
     */
    private void readFromConnection(URLConnection connection, byte[] buffer) {
        try {
            int bytesRead;
            int totalBytesRead = 0;
            InputStream istream = connection.getInputStream();
            int expectedBytes = connection.getContentLength();
            while (totalBytesRead < expectedBytes) {
                bytesRead = istream.read(buffer, totalBytesRead, buffer.length);
                if (bytesRead <= 0) {
                    break;
                } else {
                    totalBytesRead += bytesRead;
                }
            }
            try {
                istream.close();
            } catch (Exception closeException) {
            }
            if (!isConnected) {
                return;
            }
            debug("readFromConnection: expectedBytes=" + expectedBytes + " actually read:" + totalBytesRead);
            if (totalBytesRead < expectedBytes) {
                if (++pollErrorCount > 10) {
                    errorMsg("The chat server does not seem to be responding");
                    pollErrorCount = 0;
                }
            } else {
                handleMessages(new String(buffer, 0, totalBytesRead));
                pollErrorCount = 0;
            }
        } catch (IOException ioe) {
            debug("readFromConnection: got an IOException:" + ioe);
            pollErrorCount++;
        }
    }

    /**
     * _more_
     */
    private void readPolling() {
        String urlString = processUrl(pollReadUrl);
        URL url;
        try {
            url = new URL(urlString);
        } catch (MalformedURLException mue) {
            errorMsg("The polling url is malformed:\n" + urlString);
            isConnected = false;
            return;
        }
        pollErrorCount = 0;
        try {
            initConnection();
            while (isConnected) {
                URLConnection connection = url.openConnection();
                connection.setDoInput(true);
                readFromConnection(connection, mainBuffer);
                debug("readPolling: sleeping for: " + pollInterval);
                try {
                    Thread.currentThread().sleep(pollInterval);
                } catch (Exception exc) {
                }
            }
        } catch (Exception exc) {
            errorMsg("An error  occurred polling the server:\n" + exc);
        }
    }

    /**
     * _more_
     */
    public void readSocket() {
        try {
            socket = new Socket(myHost, port);
            inputStream = new DataInputStream(socket.getInputStream());
            outputStream = new DataOutputStream(socket.getOutputStream());
            initConnection();
        } catch (Exception exc) {
            errorMsg("An error has occurred trying to connect to:" + myHost + " on port: " + port + "\nUnable to connect to the chat server.\nThe server  may be down or you may be behind a firewall that \nis disallowing access", exc);
            runThread = null;
            return;
        }
        Throwable errorExc = null;
        String errorMsg = null;
        try {
            String msgBuffer = "";
            byte[] buffer = new byte[1000];
            while ((inputStream != null) && isConnected) {
                int length = inputStream.readInt();
                if (!isConnected) {
                    break;
                }
                if (length > 1000000) {
                    throw new IllegalArgumentException("Bad length read:" + length);
                }
                int total = 0;
                if (buffer.length < length) {
                    buffer = new byte[length];
                }
                while (total < length) {
                    int howMany = inputStream.read(buffer, total, length - total);
                    if (!isConnected) {
                        break;
                    }
                    if (howMany <= 0) {
                        break;
                    }
                    total += howMany;
                }
                if (!isConnected) {
                    break;
                }
                if (total != length) {
                    break;
                }
                String s = new String(buffer, 0, length);
                handleMessages(s);
            }
        } catch (java.net.SocketException exc) {
            errorExc = exc;
            errorMsg = "read - The connection to the chat server has been lost";
        } catch (Throwable exc) {
            errorExc = exc;
            errorMsg = "read - Thread error";
        }
        if (isConnected) {
            closeSockets();
            if (errorMsg != null) {
                errorMsg(errorMsg, errorExc);
                notifyDisconnect();
            }
        }
        runThread = null;
    }

    /**
     *  Just a hook method that gets called after we have opened the  connection.
     *  May be overwritten by derived classes to do any initial message sending.
     */
    public void initConnection() {
    }

    /**
     * _more_
     *
     * @param message _more_
     */
    public void processState(XmlNode message) {
        String v = message.getAttribute(ATTR_INTERVAL);
        if (v != null) {
            pollInterval = new Integer(v).intValue();
        }
        v = message.getAttribute(ATTR_USERNAME);
        if (v != null) {
            userName = v;
        }
        v = message.getAttribute(ATTR_USERID);
        if (v != null) {
            userId = v;
        }
    }

    /**
     * _more_
     *
     * @param type _more_
     * @param message _more_
     */
    public void processMessage(String type, XmlNode message) {
        if (type.equals(MSG_STATE)) {
            processState(message);
        } else if (type.equals(MSG_ERROR)) {
            errorMsg(message.getChildValue());
        } else if (type.equals(MSG_MESSAGE)) {
            message(message.getChildValue());
        } else {
            String body = message.getChildValue();
            System.err.println("Unknown message:" + message);
            if (body == null) {
                body = message.toString();
            }
            putText("Unknown message:" + body);
        }
    }

    /**
     *  This method is called when the applet receives a  message.
     *  messages is of the form:
     *  <message type="foo">body</message>  ...   <message type="bar">body</message>
     *
     * @param messages _more_
     */
    public synchronized void handleMessages(String messages) {
        messages = messages.trim();
        if (messages.length() == 0) {
            return;
        }
        XmlNode root;
        try {
            root = XmlNode.parse(messages);
        } catch (Exception exc) {
            errorMsg("An error occurred while processing messages.\n" + exc);
            return;
        }
        processMessages(root);
    }

    /**
     * _more_
     *
     * @param root _more_
     */
    public synchronized void processMessages(XmlNode root) {
        for (int i = 0; i < root.size(); i++) {
            XmlNode message = root.get(i);
            handleMessage(message);
        }
    }

    /**
     * _more_
     *
     * @param message _more_
     */
    public synchronized void handleMessage(XmlNode message) {
        String type = message.getAttribute(ATTR_TYPE);
        if (type == null) {
            System.err.println("Error: bad message format:" + message);
            return;
        }
        if (debug) {
            System.err.println("handleMessage:" + message.toString().trim());
        }
        String msgId = message.getAttribute(ATTR_ID);
        if (msgId != null) {
            lastMsgId = msgId;
        }
        processMessage(type, message);
    }

    /**
     * _more_
     */
    public void start() {
        if (initFailed || !shouldMakeConnection()) {
            return;
        }
        if (runThread == null) {
            try {
                runThread = new Thread(this);
                runThread.start();
            } catch (Exception exc) {
                errorMsg("An error has occurred:\n" + exc);
                runThread = null;
                socket = null;
            }
        }
    }

    /**
     * _more_
     */
    public void destroy() {
        disconnect();
    }

    /**
     * _more_
     */
    public void stop() {
        if (runThread == null) {
            return;
        }
        debug("stop called");
        disconnect();
    }

    /**
     * _more_
     *
     * @param comp _more_
     */
    public static void recurseDisable(Component comp) {
        comp.setEnabled(false);
        comp.setBackground(Color.lightGray);
        if (comp instanceof Container) {
            Component[] comps = ((Container) comp).getComponents();
            for (int i = 0; i < comps.length; i++) {
                recurseDisable(comps[i]);
            }
        }
    }

    /**
     * _more_
     */
    public void notifyDisconnect() {
        recurseDisable(this);
    }

    /**
     * _more_
     */
    public synchronized void disconnect() {
        if (!isConnected) {
            return;
        }
        isConnected = false;
        try {
            write(MSG_CLOSE, "");
        } catch (Throwable exc) {
        }
        closeSockets();
        runThread = null;
    }

    /**
     * _more_
     */
    private synchronized void closeSockets() {
        if (polling) {
            return;
        }
        try {
            if (socket != null) {
                socket.close();
            }
        } catch (Throwable exc) {
        }
        outputStream = null;
        inputStream = null;
        socket = null;
    }

    /**
     * _more_
     *
     * @param type _more_
     * @param body _more_
     */
    public void write(String type, String body) {
        write(type, "", body);
    }

    /**
     * _more_
     *
     * @param name _more_
     * @param value _more_
     *
     * @return _more_
     */
    public static String attr(String name, String value) {
        return " " + name + "=" + quote(value) + " ";
    }

    /**
     * _more_
     *
     * @param v _more_
     *
     * @return _more_
     */
    public static String quote(String v) {
        return "\"" + v + "\"";
    }

    /**
     * _more_
     *
     * @param type _more_
     * @param attrs _more_
     * @param body _more_
     */
    public void write(String type, String attrs, String body) {
        String message = "<message " + attr("type", type) + attrs + ">" + body + "</message>";
        debug("write:" + message);
        if (polling) {
            writePolling(message);
        } else {
            writeSocket(message);
        }
    }

    /**
     *  This method sends a message, via a http request, to the server
     *  when in polling mode.
     *
     * @param message _more_
     */
    public void writePolling(String message) {
        try {
            String urlString = processUrl(pollWriteUrl);
            urlString = GuiUtils.replace(urlString, "%MESSAGE%", message);
            if (lastMsgId != null) {
                urlString = GuiUtils.replace(urlString, "%LASTMESSAGEID%", lastMsgId);
            }
            debug("writePolling: url=" + urlString + "\n\tMessage=" + message);
            URL url = new URL(urlString);
            URLConnection connection = url.openConnection();
            connection.setDoOutput(true);
            connection.setDoInput(true);
            OutputStream ostream = connection.getOutputStream();
            message = URLEncoder.encode(message);
            String postMessage;
            if (pollPostTemplate != null) {
                postMessage = processUrl(pollPostTemplate);
                postMessage = GuiUtils.replace(postMessage, "%MESSAGE%", message);
            } else {
                postMessage = "MESSAGE=" + message;
            }
            ostream.write(postMessage.getBytes());
            ostream.flush();
            readFromConnection(connection, extraBuffer);
        } catch (Throwable exc) {
            errorMsg("An error sending a message has occurred:" + exc);
        }
    }

    /**
     * _more_
     *
     * @return _more_
     */
    public String getRoom() {
        return room;
    }

    /**
     * _more_
     *
     * @return _more_
     */
    public String getRoomName() {
        return roomName;
    }

    /**
     * _more_
     *
     * @return _more_
     */
    public String getUserName() {
        return userName;
    }

    /**
     * _more_
     *
     * @param url _more_
     *
     * @return _more_
     */
    public String processUrl(String url) {
        String[] from = { "%sessionid%", "%userid%", "%channel%" };
        String[] to = { sessionId, userName, room };
        for (int i = 0; i < from.length; i++) {
            url = GuiUtils.replace(url, from[i], to[i]);
        }
        return url;
    }

    /**
     * _more_
     *
     * @param s _more_
     */
    public void writeSocket(String s) {
        if (outputStream == null) {
            return;
        }
        try {
            outputStream.writeInt(s.length());
            outputStream.write(s.getBytes(), 0, s.length());
            outputStream.flush();
        } catch (java.net.SocketException exc) {
            errorMsg("write - The connection to the chat server has been lost", exc);
        } catch (Throwable exc) {
            errorMsg("writeSocket  error " + exc);
        }
    }

    /**
     * _more_
     *
     * @param urlString _more_
     * @param which _more_
     */
    public void showUrl(String urlString, String which) {
        URL url = null;
        try {
            url = new URL(urlString);
        } catch (Throwable exc1) {
            try {
                url = new URL(getFullUrl(urlString));
            } catch (Throwable exc2) {
            }
        }
        if (url != null) {
            if (which != null) {
                getAppletContext().showDocument(url, "_blank");
            } else {
                getAppletContext().showDocument(url);
            }
        } else {
            putText("Error: Malformed url: " + urlString);
        }
    }

    /**
     * _more_
     *
     * @param url _more_
     *
     * @return _more_
     */
    public String readUrl(String url) {
        if (url == null) {
            return null;
        }
        url = getFullUrl(url);
        try {
            return GuiUtils.readUrl(url);
        } catch (Exception exc) {
            System.err.println("Error reading url:" + url + " " + exc);
        }
        return url;
    }

    /**
     * _more_
     *
     * @param f _more_
     *
     * @return _more_
     */
    public String addFilePrefix(String f) {
        if (filePrefix != null) {
            return filePrefix + f;
        }
        return f;
    }
}
