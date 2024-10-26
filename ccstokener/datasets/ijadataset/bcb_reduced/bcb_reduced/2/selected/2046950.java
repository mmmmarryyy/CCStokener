package net.etherstorm.jOpenRPG;

import net.etherstorm.jOpenRPG.commlib.*;
import net.etherstorm.jOpenRPG.utils.ExceptionHandler;
import java.util.*;
import net.etherstorm.jOpenRPG.event.*;
import java.io.IOException;
import org.jdom.Document;
import org.jdom.Element;
import java.awt.Dimension;
import java.awt.Window;
import java.awt.Toolkit;
import java.awt.Point;
import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.net.URL;
import java.awt.Color;

/**
 * Core acts as the messaging interface between the application and the class that
 * handles socket level communications.  Core also offeres a few services that appear
 * unrelated, but require interaction with the <em>OpenRPGConnection</em> class.
 *
 * @author $Author: tedberg $
 * @version $Revision: 352 $
 * $Date: 2002-02-01 02:32:11 -0500 (Fri, 01 Feb 2002) $
 */
public class Core implements OpenRPGMessageListener {

    OpenRPGConnection comm;

    ArrayList connectionStateListeners;

    ArrayList chatMessageListeners;

    ArrayList gameTreeListeners;

    ArrayList groupListeners;

    ArrayList playerListeners;

    ArrayList mapMessageListeners;

    ConnectionStateEvent cse;

    ChatMessageEvent cme;

    ReferenceManager referenceManager;

    SharedDataManager sdm;

    /**
	 * Constructor declaration
	 * 
	 * 
	 */
    public Core() {
        connectionStateListeners = new ArrayList();
        chatMessageListeners = new ArrayList();
        gameTreeListeners = new ArrayList();
        groupListeners = new ArrayList();
        playerListeners = new ArrayList();
        mapMessageListeners = new ArrayList();
        referenceManager = ReferenceManager.getInstance();
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

    Hashtable extensionCache = new Hashtable();

    /**
	 * Alpha function to cache a jar and launch the main class from it.  This is
	 * <strong>not</strong> safe to use in production.	Any jar calling System.exit
	 * <em>will</em> terminate the application.
	 *
	 */
    public javax.swing.JInternalFrame loadExtensionFromURL(URL u) {
        try {
            if (!extensionCache.contains(u)) {
                java.io.File file = java.io.File.createTempFile("extn", "jar");
                java.net.URLConnection urlcon = u.openConnection();
                int len = urlcon.getContentLength();
                System.err.println("extension size :" + len);
                java.io.DataInputStream dis = new java.io.DataInputStream(new java.io.BufferedInputStream(urlcon.getInputStream()));
                StringBuffer buffer = new StringBuffer();
                while (len > 0) {
                    byte[] buf = new byte[len];
                    int read = dis.read(buf);
                    buffer.append(new String(buf));
                    len -= read;
                }
                dis.close();
                System.err.println("** saving extension to :" + file);
                java.io.FileOutputStream fos = new java.io.FileOutputStream(file);
                fos.write(buffer.toString().getBytes());
                fos.close();
                extensionCache.put(u, file);
            }
            ;
            java.util.jar.JarFile jar = new java.util.jar.JarFile((java.io.File) extensionCache.get(u));
            java.util.jar.Manifest manifest = jar.getManifest();
            System.err.println("** manifest :" + manifest);
            String mainClass = manifest.getMainAttributes().getValue("Main-Class");
            System.err.println("** loading class " + mainClass + " **");
            java.net.URLClassLoader classLoader = new java.net.URLClassLoader(new URL[] { u });
            Class klass = classLoader.loadClass(mainClass);
            System.err.println("** declared methods of " + mainClass + " : " + foo(klass.getDeclaredMethods()));
            System.err.println("** methods of " + mainClass + " : " + foo(klass.getMethods()));
            System.err.println("** declared fields of " + mainClass + " : " + foo(klass.getDeclaredFields()));
            System.err.println("** fields of " + mainClass + " : " + foo(klass.getFields()));
            System.err.println("** declared constructors of " + mainClass + " : " + foo(klass.getDeclaredConstructors()));
            System.err.println("** constructors of " + mainClass + " : " + foo(klass.getConstructors()));
            Class[] arg_types = { ReferenceManager.class };
            java.lang.reflect.Constructor ctor = klass.getConstructor(arg_types);
            Object[] args = { ReferenceManager.getInstance() };
            Object obj = ctor.newInstance(args);
            return (javax.swing.JInternalFrame) obj;
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
            net.etherstorm.jOpenRPG.dicerollers.original.OriginalParser parser = new net.etherstorm.jOpenRPG.dicerollers.original.OriginalParser();
            String buf = "";
            StringTokenizer tok = new StringTokenizer(text, "[]");
            boolean inRoll = (text.charAt(0) == '[');
            StringBuffer out = new StringBuffer();
            while (tok.hasMoreTokens()) {
                buf = "";
                if (inRoll) {
                    parser.evalDieCode(tok.nextToken());
                    buf = parser.getDescription();
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
            xout.setLineSeparator((normalize) ? "\n" : "");
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
            if (result == fc.APPROVE_OPTION) {
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
        StringBuffer buf = new StringBuffer();
        buf.append(referenceManager.getSettingAttribute("version.info", "", "major", "0"));
        buf.append(".");
        buf.append(referenceManager.getSettingAttribute("version.info", "", "minor", "0"));
        buf.append(".");
        buf.append(referenceManager.getSettingAttribute("version.info", "", "micro", "0"));
        buf.append(".");
        buf.append(referenceManager.getSettingAttribute("version.info", "", "build", "0"));
        return buf.toString();
    }

    public String getBuildDate() {
        return referenceManager.getSettingAttribute("version.info", "", "date", "");
    }

    /**
	 * Method declaration
	 * 
	 * 
	 * @param ioe
	 * 
	 */
    public void handleLostConnection(IOException ioe) {
        fireChatMessageReceivedEvent(cme.SYSTEM_MESSAGE, "Connectino to server was lost", "0", "all", "0");
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
            comm = new OpenRPGConnection(address);
            comm.addOpenRPGMessageListener(this);
            comm.connect(referenceManager.getPreference("handle", "No Name"));
            clearServerInfo();
            comm.pump();
            cse = new ConnectionStateEvent(comm);
            fireConnectionStateChanged();
            fireChatMessageReceivedEvent(cme.INFO_MESSAGE, "Connected to server " + address, "0", comm.getId(), "0");
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
            fireChatMessageReceivedEvent(cme.INFO_MESSAGE, "Disconnected from server", "0", "0", "0");
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
            return referenceManager.getPreference("handle", "No Name");
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
        fireChatMessageReceivedEvent(cme.INFO_MESSAGE, getPlayerChatHandle(id) + " ( enter )", "0", "all", getGroupId());
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
        fireChatMessageReceivedEvent(cme.INFO_MESSAGE, getPlayerChatHandle(id) + " ( exit )", "0", "all", getGroupId());
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
            return "(0) " + referenceManager.getPreference("handle", "No Name");
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
                System.out.println("status will be updated");
                referenceManager.getSharedDataManager().setPlayerStatus(getId(), text);
                System.out.println("local status set to " + text);
                comm.sendStatusMessage(text);
                System.out.println("status message sent");
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
            if (isConnected()) {
                comm.setMyName(handle);
                referenceManager.getSharedDataManager().setPlayerHandle(comm.getId(), handle);
            }
            referenceManager.setPreference("handle", handle);
            comm.sendStatusMessage(referenceManager.getPreference("passive.text", "Idle"));
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
            System.out.println(evt.getDocument().toString());
            PlayerMessage pm = new PlayerMessage();
            pm.fromXML(evt.getDocument());
            pm.setIncoming(true);
            System.out.println(pm);
            pm.send();
            if (pm.getAction().equals("group")) comm.setMyGroup(pm.getGroup());
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
    public synchronized void systemMessageReceived(OpenRPGMessageEvent evt) {
        try {
            System.out.println(evt.getDocument().toString());
            Element elem = evt.getDocument().getRootElement().getChild("system.message");
            String action = elem.getAttributeValue("action");
            String msg = elem.getText();
            if (action.equals("disconnected")) {
                try {
                    comm.disconnect();
                } catch (NullPointerException npe) {
                } catch (Exception ex) {
                    ExceptionHandler.handleException(ex);
                }
                comm = null;
                cse = null;
                clearServerInfo();
                fireConnectionStateChanged();
                fireChatMessageReceivedEvent(cme.SYSTEM_MESSAGE, "Disconnected from server", "0", "all", "0");
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
            System.out.println(evt.getDocument().toString());
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
            } else if (type.equals("chat.text")) {
                String text = elem.getText();
                ChatMessage cm = new ChatMessage();
                cm.fromXML(evt.getDocument());
                cm.setIncoming(true);
                cm.send();
            }
        } catch (Exception ex) {
            ExceptionHandler.handleException(ex);
            org.jdom.output.XMLOutputter xout = new org.jdom.output.XMLOutputter();
            System.err.println("Offending xml is:" + xout.outputString(evt.getDocument()));
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
            System.out.println(evt.getDocument().toString());
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
        System.out.println("Creating Group");
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
}
