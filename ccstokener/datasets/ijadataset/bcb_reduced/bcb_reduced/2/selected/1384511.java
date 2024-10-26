package net.etherstorm.jopenrpg;

import java.awt.Color;
import java.awt.Dimension;
import java.awt.Point;
import java.awt.Toolkit;
import java.io.BufferedInputStream;
import java.io.BufferedReader;
import java.io.DataInputStream;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStreamReader;
import java.lang.reflect.Constructor;
import java.net.URL;
import java.net.URLClassLoader;
import java.net.URLConnection;
import java.util.ArrayList;
import java.util.Hashtable;
import java.util.Iterator;
import java.util.StringTokenizer;
import java.util.Properties;
import java.util.Date;
import java.util.Locale;
import java.util.jar.JarFile;
import java.util.jar.Manifest;
import java.text.MessageFormat;
import java.text.DateFormat;
import javax.swing.JFileChooser;
import javax.swing.JInternalFrame;
import net.etherstorm.jopenrpg.event.ChatMessageEvent;
import net.etherstorm.jopenrpg.event.ChatMessageListener;
import net.etherstorm.jopenrpg.event.ConnectionStateEvent;
import net.etherstorm.jopenrpg.event.ConnectionStateListener;
import net.etherstorm.jopenrpg.event.GroupMessageEvent;
import net.etherstorm.jopenrpg.event.GroupMessageEventListener;
import net.etherstorm.jopenrpg.event.MapMessageEvent;
import net.etherstorm.jopenrpg.event.MapMessageListener;
import net.etherstorm.jopenrpg.event.OpenRPGMessageEvent;
import net.etherstorm.jopenrpg.event.OpenRPGMessageListener;
import net.etherstorm.jopenrpg.event.PlayerMessageEvent;
import net.etherstorm.jopenrpg.event.PlayerMessageEventListener;
import net.etherstorm.jopenrpg.net.AbstractMessage;
import net.etherstorm.jopenrpg.net.ChatMessage;
import net.etherstorm.jopenrpg.net.CreateGroupMessage;
import net.etherstorm.jopenrpg.net.GroupMessage;
import net.etherstorm.jopenrpg.net.JoinGroupMessage;
import net.etherstorm.jopenrpg.net.MapMessage;
import net.etherstorm.jopenrpg.net.OpenRPGConnection;
import net.etherstorm.jopenrpg.net.PlayerMessage;
import net.etherstorm.jopenrpg.net.TreeMessage;
import net.etherstorm.jopenrpg.swing.event.GameTreeEvent;
import net.etherstorm.jopenrpg.swing.event.GameTreeListener;
import net.etherstorm.jopenrpg.swing.nodehandlers.PreferencesNode;
import net.etherstorm.jopenrpg.util.ExceptionHandler;
import org.apache.log4j.Logger;
import org.jdom.Document;
import org.jdom.Element;

/**
 * Core acts as the messaging interface between the application and the class
 * that handles socket level communications. Core also offeres a few services
 * that appear unrelated, but require interaction with the
 * <em>OpenRPGConnection</em> class.
 * 
 * @author $Author: tedberg $
 * @version $Revision: 1.43 $ $Date: 2006/11/16 19:58:52 $
 */
public class Core implements OpenRPGMessageListener {

    OpenRPGConnection comm;

    ArrayList<ConnectionStateListener> connectionStateListeners;

    ArrayList<ChatMessageListener> chatMessageListeners;

    ArrayList<GameTreeListener> gameTreeListeners;

    ArrayList<GroupMessageEventListener> groupListeners;

    ArrayList<PlayerMessageEventListener> playerListeners;

    ArrayList<MapMessageListener> mapMessageListeners;

    ConnectionStateEvent cse;

    ChatMessageEvent cme;

    ReferenceManager referenceManager;

    SharedDataManager sdm;

    protected static Logger logger = Logger.getLogger(Core.class);

    Properties props;

    /**
	 * Constructor declaration
	 * 
	 * 
	 */
    public Core() {
        connectionStateListeners = new ArrayList<ConnectionStateListener>();
        chatMessageListeners = new ArrayList<ChatMessageListener>();
        gameTreeListeners = new ArrayList<GameTreeListener>();
        groupListeners = new ArrayList<GroupMessageEventListener>();
        playerListeners = new ArrayList<PlayerMessageEventListener>();
        mapMessageListeners = new ArrayList<MapMessageListener>();
        referenceManager = ReferenceManager.getInstance();
        try {
            props = new Properties();
            props.load(Core.class.getResource("/application.properties").openStream());
        } catch (Exception ex) {
            ex.printStackTrace();
        }
    }

    /**
	 * Method declaration
	 * 
	 * 
	 * @param l
	 * 
	 */
    public void addConnectionStateListener(ConnectionStateListener l) {
        connectionStateListeners.add(l);
    }

    /**
	 * Method declaration
	 * 
	 * 
	 * @param l
	 * 
	 */
    public void removeConnectionStateListener(ConnectionStateListener l) {
        connectionStateListeners.remove(l);
    }

    /**
	 * Method declaration
	 * 
	 * 
	 */
    public void fireConnectionStateChanged() {
        Iterator iter = connectionStateListeners.iterator();
        while (iter.hasNext()) {
            ((ConnectionStateListener) iter.next()).connectionStateChanged(cse);
        }
    }

    /**
	 * Method declaration
	 * 
	 * 
	 * @param l
	 * 
	 */
    public void addChatMessageListener(ChatMessageListener l) {
        chatMessageListeners.add(l);
    }

    /**
	 * Method declaration
	 * 
	 * 
	 * @param l
	 * 
	 */
    public void removeChatMessageListener(ChatMessageListener l) {
        chatMessageListeners.remove(l);
    }

    /**
	 * Method declaration
	 * 
	 * 
	 * @param type
	 * @param msg
	 * @param from
	 * @param to
	 * @param group
	 * 
	 */
    public void fireChatMessageReceivedEvent(int type, String msg, String from, String to, String group) {
        if (cme == null) {
            cme = new ChatMessageEvent(this);
        }
        cme.setMessageType(type);
        cme.setMessage(msg);
        cme.setFrom(from);
        cme.setTo(to);
        cme.setGroupId(group);
        Iterator iter = chatMessageListeners.iterator();
        while (iter.hasNext()) {
            ((ChatMessageListener) iter.next()).chatMessageReceived(cme);
        }
    }

    /**
	 * Method declaration
	 * 
	 * 
	 * @param msg
	 * 
	 */
    public void fireChatMessageReceivedEvent(ChatMessage msg) {
        if (cme == null) {
            cme = new ChatMessageEvent(this);
        }
        cme.setMsg(msg);
        Iterator iter = chatMessageListeners.iterator();
        while (iter.hasNext()) {
            ((ChatMessageListener) iter.next()).chatMessageReceived(cme);
        }
    }

    /**
	 * Method declaration
	 * 
	 * 
	 * @param l
	 * 
	 */
    public void addGameTreeListener(GameTreeListener l) {
        gameTreeListeners.add(l);
    }

    /**
	 * Method declaration
	 * 
	 * 
	 * @param l
	 * 
	 */
    public void removeGameTreeListener(GameTreeListener l) {
        gameTreeListeners.remove(l);
    }

    /**
	 * Method declaration
	 * 
	 * 
	 * @param msg
	 * 
	 */
    public void fireReceivedTreeNode(TreeMessage msg) {
        GameTreeEvent gte = new GameTreeEvent(this, msg);
        Iterator iter = gameTreeListeners.iterator();
        while (iter.hasNext()) {
            ((GameTreeListener) iter.next()).receivedTreeNode(gte);
        }
    }

    /**
	 * Method declaration
	 * 
	 * 
	 * @param l
	 * 
	 */
    public void addGroupMessageEventListener(GroupMessageEventListener l) {
        groupListeners.add(l);
    }

    /**
	 * Method declaration
	 * 
	 * 
	 * @param l
	 * 
	 */
    public void removeGroupMessageEventListener(GroupMessageEventListener l) {
        groupListeners.remove(l);
    }

    /**
	 * Method declaration
	 * 
	 * 
	 * @param msg
	 * 
	 */
    public void fireGroupUpdateReceived(GroupMessage msg) {
        GroupMessageEvent gme = new GroupMessageEvent(this, msg);
        Iterator iter = groupListeners.iterator();
        while (iter.hasNext()) {
            ((GroupMessageEventListener) iter.next()).groupMessageReceived(gme);
        }
    }

    /**
	 * 
	 * 
	 */
    public void addPlayerMessageEventListener(PlayerMessageEventListener l) {
        playerListeners.add(l);
    }

    /**
	 * 
	 * 
	 */
    public void removePlayerMessageEventListener(PlayerMessageEventListener l) {
        playerListeners.remove(l);
    }

    /**
	 * 
	 * 
	 */
    public void firePlayerMessageReceived(PlayerMessage msg) {
        PlayerMessageEvent pme = new PlayerMessageEvent(this, msg);
        Iterator iter = playerListeners.iterator();
        while (iter.hasNext()) ((PlayerMessageEventListener) iter.next()).playerMessageReceived(pme);
    }

    /**
	 * 
	 * 
	 */
    public void addMapMessageListener(MapMessageListener l) {
        mapMessageListeners.add(l);
    }

    /**
	 * 
	 * 
	 */
    public void removeMapMessageListener(MapMessageListener l) {
        mapMessageListeners.remove(l);
    }

    /**
	 * 
	 * 
	 */
    public void fireMapMessageReceived(MapMessage msg) {
        MapMessageEvent mme = new MapMessageEvent(this, msg.getDocument());
        Iterator iter = mapMessageListeners.iterator();
        while (iter.hasNext()) ((MapMessageListener) iter.next()).mapMessageReceived(mme);
    }

    String getColorByte(int value) {
        String result = Integer.toHexString(value);
        if (result.length() == 1) {
            result = "0" + result;
        }
        return result;
    }

    /**
	 * 
	 * 
	 */
    public String getStringFromColor(Color c) {
        return "#" + getColorByte(c.getRed()) + getColorByte(c.getGreen()) + getColorByte(c.getBlue());
    }

    /**
	 * 
	 * 
	 */
    public Color getColorFromString(String value) {
        Color result = Color.black;
        try {
            int r = Integer.parseInt(value.substring(1, 3), 16);
            int g = Integer.parseInt(value.substring(3, 5), 16);
            int b = Integer.parseInt(value.substring(5, 7), 16);
            result = new Color(r, g, b);
        } catch (Exception ex) {
            ExceptionHandler.handleException(ex);
        }
        return result;
    }

    Hashtable<URL, File> extensionCache = new Hashtable<URL, File>();

    /**
	 * Alpha function to cache a jar and launch the main class from it. This is
	 * <strong>not</strong> safe to use in production. Any jar calling
	 * System.exit <em>will</em> terminate the application.
	 * 
	 */
    public javax.swing.JInternalFrame loadExtensionFromURL(URL u) {
        try {
            if (!extensionCache.contains(u)) {
                File file = File.createTempFile("extn", "jar");
                URLConnection urlcon = u.openConnection();
                int len = urlcon.getContentLength();
                logger.debug("extension size :" + len);
                DataInputStream dis = new DataInputStream(new BufferedInputStream(urlcon.getInputStream()));
                StringBuffer buffer = new StringBuffer();
                while (len > 0) {
                    byte[] buf = new byte[len];
                    int read = dis.read(buf);
                    buffer.append(new String(buf));
                    len -= read;
                }
                dis.close();
                logger.debug("** saving extension to :" + file);
                FileOutputStream fos = new FileOutputStream(file);
                fos.write(buffer.toString().getBytes());
                fos.close();
                extensionCache.put(u, file);
            }
            JarFile jar = new JarFile(extensionCache.get(u));
            Manifest manifest = jar.getManifest();
            logger.debug("** manifest :" + manifest);
            String mainClass = manifest.getMainAttributes().getValue("Main-Class");
            logger.debug("** loading class " + mainClass + " **");
            URLClassLoader classLoader = new URLClassLoader(new URL[] { u });
            Class klass = classLoader.loadClass(mainClass);
            logger.debug("** declared methods of " + mainClass + " : " + foo(klass.getDeclaredMethods()));
            logger.debug("** methods of " + mainClass + " : " + foo(klass.getMethods()));
            logger.debug("** declared fields of " + mainClass + " : " + foo(klass.getDeclaredFields()));
            logger.debug("** fields of " + mainClass + " : " + foo(klass.getFields()));
            logger.debug("** declared constructors of " + mainClass + " : " + foo(klass.getDeclaredConstructors()));
            logger.debug("** constructors of " + mainClass + " : " + foo(klass.getConstructors()));
            Class[] arg_types = { ReferenceManager.class };
            Constructor ctor = klass.getConstructor(arg_types);
            Object[] args = { ReferenceManager.getInstance() };
            Object obj = ctor.newInstance(args);
            return (JInternalFrame) obj;
        } catch (Exception ex) {
            ExceptionHandler.handleException(ex);
            return null;
        }
    }

    String foo(Object[] data) {
        String result = "\n";
        for (int loop = 0; loop < data.length; loop++) result += "\t" + data[loop].toString() + "\n";
        return result;
    }

    /**
	 * 
	 * 
	 */
    public String loadResourceString(String ref) {
        return loadURLString(Core.class.getResource(ref));
    }

    /**
	 * 
	 * 
	 */
    public String loadURLString(java.net.URL url) {
        try {
            BufferedReader br = new BufferedReader(new InputStreamReader(url.openStream()));
            StringBuffer buf = new StringBuffer();
            String s = "";
            while (br.ready() && s != null) {
                s = br.readLine();
                if (s != null) {
                    buf.append(s);
                    buf.append("\n");
                }
            }
            return buf.toString();
        } catch (IOException ex) {
            return "";
        } catch (NullPointerException npe) {
            return "";
        }
    }

    /**
	 * Method declaration
	 * 
	 * 
	 * @param text
	 * 
	 * @return buf.toString
	 * 
	 */
    public String normNewlines(String text) {
        StringBuffer buf = new StringBuffer(text);
        int index = buf.toString().indexOf("\n");
        while (index > -1) {
            buf.replace(index, index + 1, "<br>");
            index = buf.toString().indexOf("\n");
        }
        return buf.toString();
    }

    /**
	 * Method declaration
	 * 
	 * 
	 * @param text
	 * 
	 * @return out.toString
	 * 
	 */
    public String parseDieRolls(String text) {
        try {
            if (text == null || text.length() == 0) {
                return text;
            }
            String buf = "";
            StringTokenizer tok = new StringTokenizer(text, "[]");
            boolean inRoll = (text.charAt(0) == '[');
            StringBuffer out = new StringBuffer();
            while (tok.hasMoreTokens()) {
                buf = "";
                if (inRoll) {
                    getDieParser().evalDieCode(tok.nextToken());
                    buf = getDieParser().getDescription();
                    inRoll = false;
                } else {
                    buf = tok.nextToken();
                    inRoll = true;
                }
                out.append(buf);
            }
            return out.toString();
        } catch (Exception ex) {
            ExceptionHandler.handleException(ex);
            return text;
        }
    }

    public void setDieParser(net.etherstorm.jopenrpg.dicerollers.original.OriginalParser p) {
        _parser = p;
    }

    net.etherstorm.jopenrpg.dicerollers.original.OriginalParser _parser;

    public net.etherstorm.jopenrpg.dicerollers.original.OriginalParser getDieParser() {
        if (_parser == null) {
            setDieParser(new net.etherstorm.jopenrpg.dicerollers.original.OriginalParser());
        }
        return _parser;
    }

    /**
	 * Method declaration
	 * 
	 * 
	 * @param child
	 * @param background
	 * 
	 */
    public void centerWindow(java.awt.Window child, java.awt.Window background) {
        if (background == null) {
            Dimension screensize = Toolkit.getDefaultToolkit().getScreenSize();
            Dimension windowsize = child.getSize();
            windowsize.height = Math.min(windowsize.height, screensize.height);
            windowsize.width = Math.min(windowsize.width, screensize.width);
            child.setLocation((screensize.width - windowsize.width) / 2, (screensize.height - windowsize.height) / 2);
        } else {
            Point loc = background.getLocation();
            Dimension csize = child.getSize();
            Dimension bsize = background.getSize();
            child.setLocation((bsize.width - csize.width) / 2 + loc.x, (bsize.height - csize.height) / 2 + loc.y);
        }
    }

    /**
	 * Method declaration
	 * 
	 * 
	 * @param d
	 * @param f
	 * @param normalize
	 * 
	 */
    public void writeDomToFile(Document d, java.io.File f, boolean normalize) {
        try {
            org.jdom.output.XMLOutputter xout = new org.jdom.output.XMLOutputter("  ", true);
            xout.setTextNormalize(normalize);
            xout.setTextTrim(true);
            xout.setLineSeparator("\n");
            java.io.FileOutputStream fout = new java.io.FileOutputStream(f);
            xout.output(d, fout);
            fout.close();
        } catch (Exception ex) {
            ExceptionHandler.handleException(ex);
        }
    }

    /**
	 * Method declaration
	 * 
	 * 
	 * @param d
	 * @param f
	 * 
	 */
    public void writeDomToFile(Document d, java.io.File f) {
        writeDomToFile(d, f, false);
    }

    /**
	 * Method declaration
	 * 
	 * 
	 * @param d
	 * @param normalize
	 * 
	 */
    public void writeDomToFile(Document d, boolean normalize) {
        try {
            javax.swing.JFileChooser fc = referenceManager.getDefaultFileChooser();
            int result = fc.showSaveDialog(referenceManager.getMainFrame());
            if (result == JFileChooser.APPROVE_OPTION) {
                java.io.File f = fc.getSelectedFile();
                writeDomToFile(d, f, normalize);
            }
        } catch (Exception ex) {
            ExceptionHandler.handleException(ex);
        }
    }

    /**
	 * Method declaration
	 * 
	 * 
	 * @param d
	 * 
	 */
    public void writeDomToFile(Document d) {
        writeDomToFile(d, false);
    }

    /**
	 * Method declaration
	 * 
	 * 
	 * @return StringBuffer
	 * 
	 */
    public String getVersionString() {
        try {
            String msg = "{0}.{1}.{2}.{3}";
            return MessageFormat.format(msg, new Object[] { props.getProperty("major", "0"), props.getProperty("minor", "0"), props.getProperty("micro", "0"), props.getProperty("build.number", "0") });
        } catch (Exception ex) {
            ex.printStackTrace();
        }
        return "Err";
    }

    public synchronized String getClientVersion() {
        return props.getProperty("client.version", "0.9.5");
    }

    public synchronized String getClientString() {
        return props.getProperty("client.name", "jOpenRPG") + " " + getVersionString();
    }

    public synchronized String getProtocolVersion() {
        return props.getProperty("protocol.version", "1.2");
    }

    public String getBuildDate() {
        try {
            String d = props.getProperty("built.on", new Date().toString());
            DateFormat df = DateFormat.getDateTimeInstance(DateFormat.MEDIUM, DateFormat.MEDIUM, Locale.US);
            Date date = df.parse(d);
            return df.format(date);
        } catch (Exception ex) {
            ex.printStackTrace();
        }
        return "Err";
    }

    protected String _role;

    public static final String ROLE_LURKER = "Lurker";

    public static final String ROLE_PLAYER = "Player";

    public static final String ROLE_GM = "GM";

    /**
	 * Method declaration
	 * 
	 * 
	 * @param ioe
	 * 
	 */
    public void handleLostConnection(IOException ioe) {
        fireChatMessageReceivedEvent(ChatMessageEvent.SYSTEM_MESSAGE, "Connectino to server was lost", "0", "all", "0");
        disconnect();
    }

    /**
	 * Method declaration
	 * 
	 * 
	 * @param address
	 * 
	 */
    public void connectTo(String address) {
        try {
            java.net.URL url = new java.net.URL("http://" + address);
            int port = (url.getPort() == -1 ? 6774 : url.getPort());
            comm = new OpenRPGConnection(url.getHost(), port);
            comm.addOpenRPGMessageListener(this);
            PreferencesNode pn = new PreferencesNode();
            String name = comm.connect(pn.getHandle());
            if (name == null) return;
            clearServerInfo();
            comm.pump();
            cse = new ConnectionStateEvent(comm);
            fireConnectionStateChanged();
            fireChatMessageReceivedEvent(ChatMessageEvent.INFO_MESSAGE, "Connected to server " + address, "0", comm.getId(), "0");
        } catch (Exception ex) {
            ExceptionHandler.handleException(ex);
        }
    }

    /**
	 * Method declaration
	 * 
	 * 
	 */
    public void disconnect() {
        try {
            if (isConnected()) {
                comm.disconnect();
            }
        } catch (Exception ex) {
            ExceptionHandler.handleException(ex);
        } finally {
            comm = null;
            cse = null;
            clearServerInfo();
            fireConnectionStateChanged();
            fireChatMessageReceivedEvent(ChatMessageEvent.INFO_MESSAGE, "Disconnected from server", "0", "0", "0");
        }
    }

    /**
	 * Method declaration
	 * 
	 * 
	 * @return IsConnected
	 * 
	 */
    public boolean isConnected() {
        return comm != null;
    }

    /**
	 * Method declaration
	 * 
	 * 
	 * @return getId
	 * 
	 */
    public String getId() {
        try {
            return comm.getId();
        } catch (Exception ex) {
            return "0";
        }
    }

    /**
	 * Method declaration
	 * 
	 * 
	 * @return getGroupId
	 * 
	 */
    public String getGroupId() {
        try {
            return comm.getMyGroup();
        } catch (Exception ex) {
            return "-1";
        }
    }

    /**
	 * Method declaration
	 * 
	 * 
	 * @return getLocalIP
	 */
    public String getIp() {
        try {
            return comm.getLocalIp();
        } catch (Exception ex) {
            return "127.0.0.1";
        }
    }

    /**
	 * Method declaration
	 * 
	 * 
	 * @return getMyName
	 * 
	 */
    public String getMyHandle() {
        try {
            return comm.getMyName();
        } catch (Exception ex) {
            PreferencesNode pn = new PreferencesNode();
            return pn.getHandle();
        }
    }

    /**
	 * Method declaration
	 * 
	 * 
	 * @param id
	 * @param handle
	 * @param status
	 * 
	 */
    public void addPlayer(String id, String handle, String status) {
        referenceManager.getSharedDataManager().addPlayer(id, handle, status);
        fireChatMessageReceivedEvent(ChatMessageEvent.INFO_MESSAGE, getPlayerChatHandle(id) + " ( enter )", "0", "all", getGroupId());
    }

    /**
	 * Method declaration
	 * 
	 * 
	 * @param id
	 * @param handle
	 * @param status
	 * 
	 */
    public void updatePlayer(String id, String handle, String status) {
        referenceManager.getSharedDataManager().setPlayerInfo(id, handle, status);
    }

    /**
	 * Method declaration
	 * 
	 * 
	 * @param id
	 * 
	 */
    public void removePlayer(String id) {
        fireChatMessageReceivedEvent(ChatMessageEvent.INFO_MESSAGE, getPlayerChatHandle(id) + " ( exit )", "0", "all", getGroupId());
        referenceManager.getSharedDataManager().removePlayer(id);
    }

    /**
	 * Method declaration
	 * 
	 * 
	 * @return getMyName
	 * 
	 */
    public synchronized String getPlayerChatHandle() {
        try {
            return "( " + comm.getId() + " ) " + comm.getMyName();
        } catch (Exception ex) {
            PreferencesNode pn = new PreferencesNode();
            return "(0) " + pn.getHandle();
        }
    }

    /**
	 * Method declaration
	 * 
	 * 
	 * @param id
	 * 
	 * @return id
	 * 
	 */
    public synchronized String getPlayerChatHandle(String id) {
        return referenceManager.getSharedDataManager().getPlayerChatHandle(id);
    }

    /**
	 * Method declaration
	 * 
	 * 
	 */
    public void clearServerInfo() {
        referenceManager.getSharedDataManager().clearPlayerList();
        referenceManager.getSharedDataManager().clearGroupList();
    }

    /**
	 * Method declaration
	 * 
	 * 
	 * @param name
	 * @param passwd
	 * @param bootpw
	 * 
	 */
    public void createGroup(String name, String passwd, String bootpw) {
        try {
            comm.sendCreateGroupMessage(name, passwd, bootpw);
        } catch (IOException ioe) {
            handleLostConnection(ioe);
        } catch (Exception ex) {
            ExceptionHandler.handleException(ex);
        }
    }

    /**
	 * Method declaration
	 * 
	 * 
	 * @param gid
	 * @param pwd
	 * 
	 */
    public void joinGroup(String gid, String pwd) {
        try {
            comm.sendJoinGroupMessage(gid, pwd);
        } catch (IOException ioe) {
            handleLostConnection(ioe);
        } catch (Exception ex) {
            ExceptionHandler.handleException(ex);
        }
    }

    /**
	 * Method declaration
	 * 
	 * 
	 * @param text
	 * 
	 */
    public void sendStatus(String text) {
        try {
            if (!text.equals(referenceManager.getSharedDataManager().getPlayerStatus(getId()))) {
                logger.debug("status will be updated");
                referenceManager.getSharedDataManager().setPlayerStatus(getId(), text);
                logger.debug("local status set to " + text);
                comm.sendStatusMessage(text);
                logger.debug("status message sent");
            }
        } catch (NullPointerException npe) {
        } catch (IOException ioe) {
            handleLostConnection(ioe);
        } catch (Exception ex) {
            ExceptionHandler.handleException(ex);
        }
    }

    ;

    /**
	 * Method declaration
	 * 
	 * 
	 * @param msg
	 * 
	 */
    public void broadcastChatMessage(String msg) {
        try {
            if (msg == null) {
                return;
            }
            String st = normNewlines(msg);
            if (st != null) {
                sendChatMessage("all", msg);
            }
        } catch (NullPointerException npe) {
        } catch (Exception ex) {
            ExceptionHandler.handleException(ex);
        }
    }

    /**
	 * Method declaration
	 * 
	 * 
	 * @param header
	 * @param msg
	 * 
	 */
    public void sendChatMessage(String header, String msg) {
        try {
            if (msg == null) {
                return;
            }
            StringTokenizer tok = new StringTokenizer(header, ",");
            while (tok.hasMoreTokens()) {
                comm.sendChatMessage(tok.nextToken().trim(), msg);
            }
        } catch (NullPointerException ex) {
        } catch (IOException ioe) {
            handleLostConnection(ioe);
        } catch (Exception ex) {
            ExceptionHandler.handleException(ex);
        }
    }

    /**
	 * Method declaration
	 * 
	 * 
	 * @param msg
	 * 
	 */
    public void sendLocalMessage(String msg) {
    }

    /**
	 * Method declaration
	 * 
	 * 
	 * @param id
	 * @param e
	 * 
	 */
    public void sendTreeMessage(String id, Element e) {
        try {
            comm.sendTreeMessage(id, e);
        } catch (Exception ex) {
            ExceptionHandler.handleException(ex);
        }
    }

    /**
	 * Method declaration
	 * 
	 * 
	 * @param handle
	 * 
	 */
    public void changeHandle(String handle) {
        try {
            PreferencesNode pn = new PreferencesNode();
            pn.setHandle(handle);
            if (isConnected()) {
                comm.setMyName(handle);
                referenceManager.getSharedDataManager().setPlayerHandle(comm.getId(), handle);
                comm.sendStatusMessage(pn.getIdleText());
            } else {
                referenceManager.getSharedDataManager().setPlayerHandle("0", handle);
            }
        } catch (NullPointerException npe) {
            npe.printStackTrace();
        } catch (IOException ioe) {
            handleLostConnection(ioe);
        } catch (Exception ex) {
            ExceptionHandler.handleException(ex);
        }
    }

    /**
	 * Method declaration
	 * 
	 * 
	 * @param evt
	 * 
	 */
    public synchronized void playerMessageReceived(OpenRPGMessageEvent evt) {
        try {
            logger.debug(evt.getDocument().toString());
            PlayerMessage pm = new PlayerMessage();
            pm.fromXML(evt.getDocument());
            pm.setIncoming(true);
            logger.debug(pm);
            pm.send();
            if (pm.getAction().equals("group")) comm.setMyGroup(pm.getGroup());
        } catch (Exception ex) {
            ExceptionHandler.handleException(ex);
        }
    }

    public synchronized void roleMessageReceived(OpenRPGMessageEvent evt) {
        try {
            logger.debug(evt.getDocument().toString());
            Element elem = evt.getDocument().getRootElement();
            org.jdom.output.XMLOutputter xout = new org.jdom.output.XMLOutputter();
            System.out.println(xout.outputString(evt.getDocument()));
            String action = elem.getAttributeValue("action");
            String id = elem.getAttributeValue("id");
            String role = elem.getAttributeValue("role");
            referenceManager.getSharedDataManager().setRole(id, role);
            logger.debug("role action is :" + action);
        } catch (Exception ex) {
            ex.printStackTrace();
        }
    }

    /**
	 * Method declaration
	 * 
	 * 
	 * @param evt
	 * 
	 */
    public synchronized void systemMessageReceived(OpenRPGMessageEvent evt) {
        try {
            logger.debug(evt.getDocument().toString());
            Element elem = evt.getDocument().getRootElement().getChild("system.message");
            String action = elem.getAttributeValue("action");
            if (action.equals("disconnected")) {
                logger.info(action);
                try {
                    if (isConnected()) comm.disconnect();
                } catch (NullPointerException npe) {
                }
                {
                    comm = null;
                    cse = null;
                    clearServerInfo();
                    fireConnectionStateChanged();
                    fireChatMessageReceivedEvent(ChatMessageEvent.SYSTEM_MESSAGE, "Disconnected from server", "0", "all", "0");
                }
            }
        } catch (Exception ex) {
            ExceptionHandler.handleException(ex);
        }
    }

    /**
	 * Method declaration
	 * 
	 * 
	 * @param evt
	 * 
	 */
    public synchronized void msgMessageReceived(OpenRPGMessageEvent evt) {
        try {
            logger.debug(evt.getDocument().toString());
            Element elem = (Element) evt.getDocument().getRootElement().getChildren().get(1);
            String type = elem.getName();
            if (type.equals("tree")) {
                TreeMessage tm = new TreeMessage();
                tm.fromXML(evt.getDocument());
                tm.setIncoming(true);
                tm.send();
                ChatMessage cm = new ChatMessage();
                cm.setIncoming(true);
                cm.setInfo();
                cm.setMessage(getPlayerChatHandle(tm.getFrom()) + " sent you a gametree node.");
                cm.send();
            } else if (type.equals("map")) {
                MapMessage mm = new MapMessage();
                mm.setDocument(evt.getDocument());
                mm.setIncoming(true);
                mm.send();
            } else if (type.equals("chat")) {
                ChatMessage cm = new ChatMessage(true);
                cm.fromXML(evt.getDocument());
                cm.send();
            }
        } catch (Exception ex) {
            ExceptionHandler.handleException(ex);
            logger.error("looks like a null document");
            org.jdom.output.XMLOutputter xout = new org.jdom.output.XMLOutputter();
            System.out.println(evt);
            System.out.println(evt.getDocument());
            System.out.println("Offending xml is " + xout.outputString(evt.getDocument()));
            logger.debug("Offending xml is:" + xout.outputString(evt.getDocument()));
        }
    }

    /**
	 * Method declaration
	 * 
	 * 
	 * @param evt
	 * 
	 */
    public synchronized void groupMessageReceived(OpenRPGMessageEvent evt) {
        try {
            logger.debug(evt.getDocument().toString());
            GroupMessage gm = new GroupMessage();
            gm.fromXML(evt.getDocument());
            gm.send();
        } catch (Exception ex) {
            ExceptionHandler.handleException(ex);
        }
    }

    /**
	 * Method declaration
	 * 
	 * 
	 * @param msg
	 * 
	 */
    public void sendLocal(ChatMessage msg) {
        System.out.println("Core.sendLocal()");
        System.out.println(msg);
        fireChatMessageReceivedEvent(msg);
    }

    /**
	 * Method declaration
	 * 
	 * 
	 * @param msg
	 * 
	 */
    public void sendRemote(ChatMessage msg) {
        try {
            if (comm != null && isConnected()) {
                comm.sendMessage(msg);
            }
        } catch (Exception ex) {
            ExceptionHandler.handleException(ex);
        }
    }

    /**
	 * Method declaration
	 * 
	 * 
	 * @param msg
	 * 
	 */
    public void sendLocal(TreeMessage msg) {
        fireReceivedTreeNode(msg);
    }

    /**
	 * Method declaration
	 * 
	 * 
	 * @param msg
	 * 
	 */
    public void sendRemote(TreeMessage msg) {
        try {
            if (comm != null && isConnected()) {
                comm.sendMessage(msg);
            }
        } catch (Exception ex) {
            ExceptionHandler.handleException(ex);
        }
    }

    /**
	 * Method declaration
	 * 
	 * 
	 * @param msg
	 * 
	 */
    public void sendLocal(MapMessage msg) {
        fireMapMessageReceived(msg);
    }

    /**
	 * Method declaration
	 * 
	 * 
	 * @param msg
	 * 
	 */
    public void sendRemote(MapMessage msg) {
        try {
            if (comm != null && isConnected()) comm.sendMessage(msg);
        } catch (Exception ex) {
            ExceptionHandler.handleException(ex);
        }
    }

    /**
	 * Method declaration
	 * 
	 * 
	 * @param msg
	 * 
	 */
    public void sendLocal(PlayerMessage msg) {
        firePlayerMessageReceived(msg);
    }

    /**
	 * Method declaration
	 * 
	 * 
	 * @param msg
	 * 
	 */
    public void sendRemote(PlayerMessage msg) {
        try {
            if (comm != null && isConnected()) comm.sendMessage(msg);
        } catch (Exception ex) {
            ExceptionHandler.handleException(ex);
        }
    }

    /**
	 * Method declaration
	 * 
	 * 
	 * @param msg
	 * 
	 */
    public void sendLocal(GroupMessage msg) {
        fireGroupUpdateReceived(msg);
    }

    /**
	 * Current protocol does not allow for client generated GroupMessages.
	 * 
	 * 
	 * @param msg
	 * 
	 */
    public void sendRemote(GroupMessage msg) {
    }

    /**
	 * Method declaration
	 * 
	 * 
	 * @param msg
	 * 
	 */
    public void sendLocal(CreateGroupMessage msg) {
    }

    /**
	 * Method declaration
	 * 
	 * 
	 * @param msg
	 * 
	 */
    public void sendRemote(CreateGroupMessage msg) {
        logger.debug("Creating Group");
        createGroup(msg.getName(), msg.getPwd(), msg.getBootPwd());
    }

    /**
	 * Method declaration
	 * 
	 * 
	 * @param msg
	 * 
	 */
    public void sendLocal(JoinGroupMessage msg) {
    }

    /**
	 * Method declaration
	 * 
	 * 
	 * @param msg
	 * 
	 */
    public void sendRemote(JoinGroupMessage msg) {
        joinGroup(msg.getGroup(), msg.getPwd());
    }

    public void sendRemote(AbstractMessage msg) {
        try {
            comm.sendMessage(msg);
        } catch (Exception ex) {
        }
    }
}
