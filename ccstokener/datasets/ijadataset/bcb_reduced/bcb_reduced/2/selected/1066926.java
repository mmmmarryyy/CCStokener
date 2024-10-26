package net.sf.jftp;

import java.awt.BorderLayout;
import java.awt.Component;
import java.awt.Dimension;
import java.awt.FlowLayout;
import java.awt.Image;
import java.awt.Point;
import java.awt.Toolkit;
import java.awt.datatransfer.DataFlavor;
import java.awt.datatransfer.Transferable;
import java.awt.dnd.DnDConstants;
import java.awt.dnd.DropTarget;
import java.awt.dnd.DropTargetDragEvent;
import java.awt.dnd.DropTargetDropEvent;
import java.awt.dnd.DropTargetEvent;
import java.awt.dnd.DropTargetListener;
import java.awt.event.ActionListener;
import java.awt.event.ComponentEvent;
import java.awt.event.ComponentListener;
import java.awt.event.WindowEvent;
import java.awt.event.WindowListener;
import java.io.BufferedInputStream;
import java.io.BufferedOutputStream;
import java.io.DataInputStream;
import java.io.File;
import java.io.FileOutputStream;
import java.io.PrintWriter;
import java.io.Reader;
import java.io.StringWriter;
import java.net.URL;
import java.util.Hashtable;
import javax.swing.JDesktopPane;
import javax.swing.JFrame;
import javax.swing.JInternalFrame;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JScrollBar;
import javax.swing.JScrollPane;
import javax.swing.JSplitPane;
import javax.swing.JTabbedPane;
import javax.swing.JTextArea;
import javax.swing.JToolBar;
import javax.swing.SwingUtilities;
import javax.swing.UIManager;
import javax.swing.event.ChangeEvent;
import javax.swing.event.ChangeListener;
import javax.swing.event.InternalFrameEvent;
import javax.swing.event.InternalFrameListener;
import javax.swing.plaf.basic.BasicInternalFrameTitlePane.RestoreAction;
import com.sshtools.j2ssh.SshClient;
import net.sf.jftp.config.Settings;
import net.sf.jftp.gui.base.AppMenuBar;
import net.sf.jftp.gui.base.DownloadList;
import net.sf.jftp.gui.base.DownloadQueue;
import net.sf.jftp.gui.base.LocalDir;
import net.sf.jftp.gui.base.LogFlusher;
import net.sf.jftp.gui.base.RemoteDir;
import net.sf.jftp.gui.base.StatusPanel;
import net.sf.jftp.gui.base.dir.Dir;
import net.sf.jftp.gui.base.dir.DirEntry;
import net.sf.jftp.gui.framework.FileTransferable;
import net.sf.jftp.gui.framework.GUIDefaults;
import net.sf.jftp.gui.framework.HDesktopBackground;
import net.sf.jftp.gui.framework.HImage;
import net.sf.jftp.gui.hostchooser.HostChooser;
import net.sf.jftp.gui.tasks.HostInfo;
import net.sf.jftp.net.BasicConnection;
import net.sf.jftp.net.ConnectionHandler;
import net.sf.jftp.net.ConnectionListener;
import net.sf.jftp.net.FilesystemConnection;
import net.sf.jftp.net.FtpConnection;
import net.sf.jftp.net.SftpConnection;
import net.sf.jftp.system.LocalIO;
import net.sf.jftp.system.UpdateDaemon;
import net.sf.jftp.system.logging.Log;
import net.sf.jftp.system.logging.Logger;
import net.sf.jftp.tools.RSSFeeder;

public class JFtp extends JPanel implements WindowListener, ComponentListener, Logger, ChangeListener, InternalFrameListener {

    public static boolean mainUsed = false;

    public static StatusPanel statusP;

    public static JLabel statusL = new JLabel("Powua file manager...                                                            ");

    public static JFrame mainFrame;

    public static Dir localDir;

    public static Dir remoteDir;

    public static DownloadList dList = new DownloadList();

    public static DownloadQueue dQueue = new DownloadQueue();

    public static boolean uiBlocked = false;

    public static HostInfo hostinfo = new HostInfo();

    private static ConnectionHandler defaultConnectionHandler = new ConnectionHandler();

    public static JDesktopPane desktop = new JDesktopPane();

    private static JScrollPane logSp;

    public static JTextArea log;

    public static AppMenuBar menuBar = null;

    public static final int CAPACITY = 9;

    public static final int CONNECTION_DATA_LENGTH = 10;

    public static DropTarget dropTarget;

    public static DropTargetListener dtListener;

    public static int acceptableActions = DnDConstants.ACTION_COPY;

    private LogFlusher flusher;

    private boolean initSize = true;

    public JTabbedPane remoteConnectionPanel = new JTabbedPane();

    public JTabbedPane localConnectionPanel = new JTabbedPane();

    private String oldText = "";

    private HDesktopBackground background;

    /** JSplitPane that holds the directory panes and the log/dl JSplitPane */
    private JSplitPane workP = null;

    /** JSplitPane that holds the log download parts */
    private JSplitPane logP = null;

    private JInternalFrame j1;

    private JInternalFrame j2;

    private JInternalFrame j3;

    private JInternalFrame j4;

    private JInternalFrame j5;

    private Hashtable internalFrames = new Hashtable();

    public HostChooser hc;

    private String buffer = "";

    private long oldtime = 0;

    private UpdateDaemon daemon;

    public RSSFeeder feeder;

    private SshClient ssh;

    public void preinit() {
        Log.setLogger(this);
        if (statusP != null) {
            statusP.remove(statusP.close);
        }
        init();
    }

    public JFtp(boolean mainUsed) {
        Log.setLogger(this);
        this.mainUsed = mainUsed;
        init();
    }

    public void init() {
        dtListener = new DTListener();
        dropTarget = new DropTarget(this, this.acceptableActions, dtListener, true);
        setLayout(new BorderLayout());
        setBackground(GUIDefaults.mainBack);
        setForeground(GUIDefaults.front);
        statusP = new StatusPanel(this);
        add("North", statusP);
        localDir = (Dir) new LocalDir(System.getProperty("user.home"));
        localDir.setDownloadList(dList);
        remoteDir = (Dir) new RemoteDir(ssh);
        remoteDir.setDownloadList(dList);
        desktop.setDropTarget(this.dropTarget);
        Dimension d = Settings.getWindowSize();
        setPreferredSize(d);
        setSize(d);
        int width = (int) d.getWidth();
        int height = (int) d.getHeight();
        dList.setMinimumSize(new Dimension((int) (width / 2.5), (int) (height * 0.20)));
        dList.setPreferredSize(new Dimension((int) (width / 2.5), (int) (height * 0.25)));
        dList.setSize(new Dimension((int) (width / 2.5), (int) (height * 0.25)));
        desktop.setDragMode(JDesktopPane.OUTLINE_DRAG_MODE);
        addBackgroundImage();
        j1 = new JInternalFrame("Local filesystem", true, false, true, true);
        j1.setMinimumSize(new Dimension(300, 300));
        j1.setLocation(5, 5);
        localConnectionPanel.addTab("file://", null, (Component) localDir, "Filesystem");
        localConnectionPanel.setSelectedIndex(0);
        localConnectionPanel.addChangeListener(this);
        j1.getContentPane().add(localConnectionPanel);
        localDir.fresh();
        desktop.add(j1);
        j1.setDefaultCloseOperation(JInternalFrame.DO_NOTHING_ON_CLOSE);
        j1.addInternalFrameListener(this);
        j1.pack();
        j1.setSize(new Dimension(400, j1.getSize().height));
        j1.show();
        j2 = new JInternalFrame("Remote connections", true, false, true, true);
        j2.setLocation(430, 5);
        remoteConnectionPanel.addTab("file://", null, (Component) remoteDir, "Filesystem");
        remoteConnectionPanel.setSelectedIndex(0);
        remoteConnectionPanel.addChangeListener(this);
        j2.getContentPane().add(remoteConnectionPanel);
        desktop.add(j2);
        j2.setDefaultCloseOperation(JInternalFrame.DO_NOTHING_ON_CLOSE);
        j2.addInternalFrameListener(this);
        j2.pack();
        j2.setSize(new Dimension(415, j1.getSize().height));
        j2.show();
        log = new JTextArea();
        log.setBackground(GUIDefaults.light);
        log.setForeground(GUIDefaults.front);
        log.setEditable(false);
        logSp = new JScrollPane(log);
        logSp.setSize(new Dimension(438, 148));
        j5 = new JInternalFrame("Queue System", true, false, true, true);
        j5.setLocation(5, 400);
        j5.getContentPane().add(dQueue, BorderLayout.CENTER);
        desktop.add(j5);
        j5.pack();
        j5.setSize(new Dimension(440, 150));
        j5.show();
        j3 = new JInternalFrame("Log", true, false, true, true);
        int x = (int) (desktop.getSize().width / 2);
        j3.setLocation(5, 425);
        j3.getContentPane().add(logSp, BorderLayout.CENTER);
        desktop.add(j3);
        j3.pack();
        j3.setSize(new Dimension(440, 150));
        j3.show();
        j4 = new JInternalFrame("Download Manager", true, false, true, true);
        j4.setLocation(450, 425);
        j4.getContentPane().add(dList, BorderLayout.CENTER);
        desktop.add(j4);
        j4.pack();
        j4.setSize(new Dimension(380, 150));
        j4.show();
        j1.toFront();
        j2.toFront();
        add("Center", desktop);
        JToolBar j = new JToolBar();
        j.setFloatable(false);
        j.add(statusP.status, FlowLayout.LEFT);
        if (Settings.getEnableRSS()) {
            feeder = new RSSFeeder();
            j.add(feeder);
        }
        add("South", j);
        addComponentListener(this);
        componentResized(new ComponentEvent(log, 0));
        restoreInternalPositions();
        validate();
        setVisible(true);
        if (!mainUsed) {
            SwingUtilities.invokeLater(new Runnable() {

                public void run() {
                }
            });
        } else {
            chooseHost();
        }
        flusher = new LogFlusher();
        daemon = new UpdateDaemon(this);
    }

    protected void chooseHost() {
        hc = new HostChooser(this);
        if (!mainUsed) {
            hc.update();
        }
    }

    public static String getHost() {
        return statusP.getHost();
    }

    public static void setHost(String which) {
        statusP.setHost(which);
    }

    public static void localUpdate() {
        localDir.fresh();
    }

    public static void remoteUpdate() {
        remoteDir.fresh();
    }

    private void saveInternalPositions() {
        saveInternalPosition(j1, "local");
        saveInternalPosition(j2, "remote");
        saveInternalPosition(j5, "queue");
        saveInternalPosition(j3, "log");
        saveInternalPosition(j4, "manager");
    }

    private void restoreInternalPositions() {
        if (Settings.getProperty("jftp.iframes.resize").equals("true")) {
        } else {
            restoreInternalPosition(j1, "local");
            restoreInternalPosition(j2, "remote");
            restoreInternalPosition(j5, "queue");
            restoreInternalPosition(j3, "log");
            restoreInternalPosition(j4, "manager");
        }
    }

    private void restoreInternalPosition(JInternalFrame f, String desc) {
        String x = Settings.getProperty("jftp.iframes." + desc + ".x");
        String y = Settings.getProperty("jftp.iframes." + desc + ".y");
        String w = Settings.getProperty("jftp.iframes." + desc + ".width");
        String h = Settings.getProperty("jftp.iframes." + desc + ".height");
        if (x.indexOf(".") >= 0) x = x.substring(0, x.indexOf("."));
        if (y.indexOf(".") >= 0) y = y.substring(0, y.indexOf("."));
        try {
            f.setLocation(Integer.parseInt(x), Integer.parseInt(y));
            f.setSize(Integer.parseInt(w), Integer.parseInt(h));
        } catch (Exception ex) {
            Log.out("Can not set internal fram position for: " + desc);
            ex.printStackTrace();
        }
    }

    private void saveInternalPosition(JInternalFrame f, String desc) {
        Point p = f.getLocation();
        Settings.setProperty("jftp.iframes." + desc + ".x", "" + p.getX());
        Settings.setProperty("jftp.iframes." + desc + ".y", "" + p.getY());
        Settings.setProperty("jftp.iframes." + desc + ".width", f.getWidth());
        Settings.setProperty("jftp.iframes." + desc + ".height", f.getHeight());
    }

    public void windowClosing(WindowEvent e) {
        saveInternalPositions();
        Settings.setProperty("jftp.window.width", this.getWidth());
        Settings.setProperty("jftp.window.height", this.getHeight());
        if (!mainUsed) {
            Settings.setProperty("jftp.window.x", (int) this.getLocationOnScreen().getX());
            Settings.setProperty("jftp.window.y", (int) this.getLocationOnScreen().getY());
        } else {
            Settings.setProperty("jftp.window.x", (int) mainFrame.getLocationOnScreen().getX());
            Settings.setProperty("jftp.window.y", (int) mainFrame.getLocationOnScreen().getY());
        }
        Settings.save();
        if (Settings.isStandalone) {
            System.exit(0);
        } else {
            mainFrame.dispose();
        }
    }

    public static void safeDisconnect() {
        BasicConnection con = remoteDir.getCon();
        if ((con != null) && con.isConnected()) {
            try {
                con.disconnect();
            } catch (Exception ex) {
            }
        }
        FilesystemConnection c = new FilesystemConnection();
        c.addConnectionListener((ConnectionListener) remoteDir);
        remoteDir.setCon(c);
        if (!c.chdir("/")) {
            c.chdir("C:\\");
        }
    }

    public void windowClosed(WindowEvent e) {
    }

    public void windowActivated(WindowEvent e) {
    }

    public void windowDeactivated(WindowEvent e) {
    }

    public void windowIconified(WindowEvent e) {
    }

    public void windowDeiconified(WindowEvent e) {
    }

    public void windowOpened(WindowEvent e) {
    }

    public void componentHidden(ComponentEvent e) {
    }

    public void componentMoved(ComponentEvent e) {
    }

    public void componentShown(ComponentEvent e) {
    }

    public void componentResized(ComponentEvent e) {
        localDir.actionPerformed(this, "local");
        remoteDir.actionPerformed(this, "remote");
        desktop.remove(background);
        addBackgroundImage();
        validate();
    }

    public void addBackgroundImage() {
        try {
            background = new HDesktopBackground(Settings.background, null);
            background.setBounds(0, 0, getSize().width, getSize().height);
            desktop.add(background, new Integer(Integer.MIN_VALUE));
        } catch (Exception ex) {
            Log.out(Settings.background + " missing, no background image used");
        }
    }

    private static void compile() {
    }

    private static void setSocksProxyOptions(String proxy, String port) {
        if (proxy.equals("") || port.equals("")) {
            return;
        }
        java.util.Properties sysprops = System.getProperties();
        sysprops.remove("socksProxyHost");
        sysprops.remove("socksProxyPort");
        sysprops.put("socksProxyHost", proxy);
        sysprops.put("socksProxyPort", port);
        Log.out("socks proxy: " + sysprops.get("socksProxyHost") + ":" + sysprops.get("socksProxyPort"));
    }

    public JFtp(SshClient ssh) {
        this.ssh = ssh;
        long start = System.currentTimeMillis();
        Log.out("starting up jftp...");
        compile();
        System.setProperty("sshtools.logfile", Settings.appHomeDir + "log4.txt");
        if (Settings.autoUpdate) {
            checkForUpgrade();
        }
        Settings.enableResuming = true;
        setSocksProxyOptions(Settings.getSocksProxyHost(), Settings.getSocksProxyPort());
        preinit();
        UIManager.getLookAndFeelDefaults().put("ClassLoader", this.getClass().getClassLoader());
        String tmp = Settings.getLookAndFeel();
        if (tmp != null) {
            this.setLookAndFeel(Settings.getLookAndFeel());
        } else {
            this.setLookAndFeel("net.sourceforge.mlf.metouia.MetouiaLookAndFeel");
        }
        if ((Settings.getLookAndFeel() == null) || !Settings.getLookAndFeel().equals("com.incors.plaf.kunststoff.KunststoffLookAndFeel")) {
            try {
                Class.forName("com.incors.plaf.kunststoff.KunststoffLookAndFeel");
                UIManager.installLookAndFeel("Kunststoff", "com.incors.plaf.kunststoff.KunststoffLookAndFeel");
            } catch (ClassNotFoundException cnfe) {
            }
        }
        if ((Settings.getLookAndFeel() == null) || !Settings.getLookAndFeel().equals("net.sourceforge.mlf.metouia.MetouiaLookAndFeel")) {
            try {
                Class.forName("net.sourceforge.mlf.metouia.MetouiaLookAndFeel");
                UIManager.installLookAndFeel("Metouia", "net.sourceforge.mlf.metouia.MetouiaLookAndFeel");
            } catch (ClassNotFoundException cnfe) {
            }
        }
        mainFrame = new JFrame();
        mainFrame.setLocation(Settings.getWindowLocation());
        mainFrame.setTitle(Settings.title);
        mainFrame.setResizable(Settings.resize);
        mainFrame.addWindowListener(this);
        Image icon = HImage.getImage(this, Settings.iconImage);
        mainFrame.setIconImage(icon);
        mainFrame.setFont(GUIDefaults.font);
        mainFrame.getContentPane().setLayout(new BorderLayout());
        mainFrame.getContentPane().add("Center", this);
        SwingUtilities.updateComponentTreeUI(mainFrame);
        mainFrame.pack();
        mainFrame.validate();
        mainFrame.setVisible(true);
        Log.out("jftp is up and running.");
        long end = System.currentTimeMillis();
        Log.out("startup time: " + (end - start) + "ms.");
    }

    private void log(String msg) {
        if (msg.startsWith("200") || msg.startsWith("227")) {
            if ((msg.indexOf("NOOP") > 0) || (msg.indexOf("Type") > 0) || (msg.indexOf("MODE") > 0) || (msg.indexOf("Passive") > 0)) {
                if (Settings.hideStatus) {
                    return;
                }
            }
        } else if (log == null) {
            return;
        }
        if (!msg.equals("")) {
            buffer = buffer + " " + msg;
        }
        log.append(buffer);
        buffer = "";
        long time = System.currentTimeMillis();
        if (((time - oldtime) < Settings.uiRefresh)) {
            UpdateDaemon.updateLog();
            return;
        }
        oldtime = time;
        JScrollBar bar = logSp.getVerticalScrollBar();
        bar.setValue(bar.getMaximum());
        repaint();
        revalidate();
    }

    private void logRaw(String msg) {
        log.append(" " + msg);
        Log.out("NOTE: logRaw called");
        paintImmediately(0, 0, getSize().width, getSize().height);
    }

    public void clearLog() {
        log.setText("");
        logSp.paintImmediately(0, 0, logSp.getSize().width, logSp.getSize().height);
    }

    private void log(String msg, Throwable throwable) {
        PrintWriter p = new PrintWriter(new StringWriter());
        throwable.printStackTrace(p);
        log(msg);
        log(p.toString());
    }

    public void debug(String msg) {
        log(msg + "\n");
    }

    public void debugRaw(String msg) {
        logRaw(msg);
    }

    public void debug(String msg, Throwable throwable) {
        log(msg, throwable);
    }

    public void warn(String msg) {
        log(msg);
    }

    public void warn(String msg, Throwable throwable) {
        log(msg, throwable);
    }

    public void error(String msg) {
        log(msg);
    }

    public void error(String msg, Throwable throwable) {
        log(msg, throwable);
    }

    public void info(String msg) {
        log(msg);
    }

    public void info(String msg, Throwable throwable) {
        log(msg, throwable);
    }

    public void fatal(String msg) {
        log(msg);
    }

    public void fatal(String msg, Throwable throwable) {
        log(msg, throwable);
    }

    public void debugSize(int size, boolean recv, boolean last, String file) {
    }

    public static String getVersion() {
        try {
            URL u = ClassLoader.getSystemResource(Settings.readme);
            if (u == null) {
                u = HImage.class.getResource("/" + Settings.readme);
            }
            DataInputStream i = new DataInputStream(u.openStream());
            String tmp = i.readLine();
            tmp = tmp.substring(tmp.lastIndexOf(">") + 1);
            tmp = tmp.substring(0, tmp.indexOf("<"));
            return tmp;
        } catch (Exception ex) {
        }
        return "";
    }

    private static void checkForUpgrade() {
        try {
            Log.out("Checking for updates...");
            URL u = ClassLoader.getSystemResource(Settings.readme);
            if (u == null) {
                u = HImage.class.getResource("/" + Settings.readme);
            }
            DataInputStream i = new DataInputStream(u.openStream());
            String tmp = i.readLine();
            tmp = tmp.substring(tmp.lastIndexOf(">"));
            tmp = tmp.substring(tmp.indexOf(".") + 1);
            tmp = tmp.substring(0, tmp.indexOf("<"));
            int x = Integer.parseInt(tmp) + 1;
            String nextVersion = "jftp-1.";
            if (x < 10) {
                nextVersion = nextVersion + "0";
            }
            nextVersion = nextVersion + x + ".tar.gz";
            File dl = new File(nextVersion);
            if (!dl.exists() || (dl.length() <= 0)) {
                URL url = new URL("http://osdn.dl.sourceforge.net/sourceforge/j-ftp/" + nextVersion);
                BufferedOutputStream f = new BufferedOutputStream(new FileOutputStream(dl));
                BufferedInputStream in = new BufferedInputStream(url.openStream());
                byte[] buf = new byte[4096];
                int stat = 1;
                Log.out("\ndownloading update: " + dl.getAbsolutePath() + "\n\n");
                while (stat > 0) {
                    stat = in.read(buf);
                    if (stat == -1) {
                        break;
                    }
                    f.write(buf, 0, stat);
                    System.out.print(".");
                }
                f.flush();
                f.close();
                in.close();
            }
            Log.out("\n\n\na newer version was found!\nplease install the File " + dl.getAbsolutePath() + " or even better visit the homepage to download the latest version...\n" + "you can turn this feature off if you don't like it (view readme for details)\n\nStarting anyway...\n\n");
        } catch (Exception ex) {
        }
        Log.out("finished check...");
    }

    public static ConnectionHandler getConnectionHandler() {
        BasicConnection con = remoteDir.getCon();
        if ((con != null) && con instanceof FtpConnection) {
            return ((FtpConnection) con).getConnectionHandler();
        } else {
            return defaultConnectionHandler;
        }
    }

    public void fireUpdate() {
        LocalIO.pause(200);
        repaint();
    }

    public void ensureLogging() {
        if (buffer.equals("")) {
            JScrollBar bar;
            if ((logSp == null) || ((bar = logSp.getVerticalScrollBar()) == null) || bar == null || (bar.getValue() == bar.getMaximum()) || bar.getValueIsAdjusting()) {
                return;
            } else {
                bar.setValue(bar.getMaximum());
                logSp.repaint();
                repaint();
                revalidate();
            }
            return;
        }
        Log.out("flushing log buffer...");
        oldtime = 0;
        log("");
    }

    public void setLookAndFeel(String name) {
        if (name == null) {
            return;
        }
        try {
            UIManager.setLookAndFeel(name);
            if (mainFrame != null) {
                SwingUtilities.invokeLater(new Runnable() {

                    public void run() {
                        SwingUtilities.updateComponentTreeUI(mainFrame);
                        SwingUtilities.updateComponentTreeUI(JFtp.statusP);
                    }
                });
            }
        } catch (Exception ex) {
            Log.debug("Error setting look and feel: " + ex);
        }
    }

    public void addConnection(String name, BasicConnection con) {
        con.addConnectionListener((ConnectionListener) localDir);
        Dir tmp = (Dir) new RemoteDir(ssh);
        tmp.setDownloadList(dList);
        con.addConnectionListener((ConnectionListener) tmp);
        tmp.setCon(con);
        int x = remoteConnectionPanel.getSelectedIndex();
        remoteConnectionPanel.addTab(parse(name), null, (Component) tmp, "Switch to: " + parse(name));
        remoteConnectionPanel.setSelectedIndex(x + 1);
        j2.setClosable(true);
    }

    public void addLocalConnection(String name, BasicConnection con) {
        con.addConnectionListener((ConnectionListener) remoteDir);
        Dir tmp = (Dir) new LocalDir();
        tmp.setDownloadList(dList);
        con.addConnectionListener((ConnectionListener) tmp);
        tmp.setCon(con);
        int x = localConnectionPanel.getSelectedIndex();
        localConnectionPanel.addTab(parse(name), null, (Component) tmp, "Switch to: " + parse(name));
        localConnectionPanel.setSelectedIndex(x + 1);
        j1.setClosable(true);
    }

    private String parse(String what) {
        if (what.indexOf("@") >= 0) {
            return what.substring(what.lastIndexOf("@") + 1);
        } else {
            return what;
        }
    }

    public void stateChanged(ChangeEvent e) {
        remoteDir = (Dir) remoteConnectionPanel.getSelectedComponent();
        localDir = (Dir) localConnectionPanel.getSelectedComponent();
        remoteDir.getCon().setLocalPath(localDir.getPath());
    }

    public void closeCurrentTab() {
        int x = remoteConnectionPanel.getSelectedIndex();
        if (x > 0) {
            safeDisconnect();
            remoteConnectionPanel.remove(x);
            remoteConnectionPanel.setSelectedIndex(x - 1);
        }
        if (remoteConnectionPanel.getTabCount() < 2) {
            j2.setClosable(false);
        }
    }

    public void closeCurrentLocalTab() {
        int x = localConnectionPanel.getSelectedIndex();
        if (x > 0) {
            BasicConnection con = localDir.getCon();
            if ((con != null) && con.isConnected()) {
                try {
                    con.disconnect();
                } catch (Exception ex) {
                }
            }
            localConnectionPanel.remove(x);
            localConnectionPanel.setSelectedIndex(x - 1);
        }
        if (localConnectionPanel.getTabCount() < 2) {
            j1.setClosable(false);
        }
    }

    public void addToDesktop(String title, Component c, int w, int h) {
        JInternalFrame jt = new JInternalFrame(title, false, true, false, true);
        if (w < 500) {
            jt.setLocation(200, 100);
        } else {
            jt.setLocation(80, 100);
        }
        jt.getContentPane().add(c);
        desktop.add(jt);
        internalFrames.put("" + c.hashCode(), jt);
        jt.pack();
        jt.setSize(new Dimension(w, h));
        jt.show();
    }

    public void removeFromDesktop(int component) {
        JInternalFrame f = (JInternalFrame) internalFrames.get("" + component);
        if (f != null) {
            f.dispose();
        } else {
            Log.debug("ERROR: " + component + " not found in Hashtable!");
        }
    }

    public void setClosable(int component, boolean ok) {
        JInternalFrame f = (JInternalFrame) internalFrames.get("" + component);
        if (f != null) {
            f.setClosable(ok);
        } else {
            Log.debug("ERROR: " + component + " not found in Hashtable!");
        }
    }

    public void setLocation(int component, int x, int y) {
        JInternalFrame f = (JInternalFrame) internalFrames.get("" + component);
        if (f != null) {
            f.setLocation(x, y);
        } else {
            Log.debug("ERROR: " + component + " not found in Hashtable!");
        }
    }

    public static void updateMenuBar() {
        menuBar.resetFileItems();
    }

    public void internalFrameClosing(InternalFrameEvent e) {
        if (e.getSource() == j1) {
            closeCurrentLocalTab();
        } else if (e.getSource() == j2) {
            closeCurrentTab();
        }
    }

    public void internalFrameActivated(InternalFrameEvent e) {
    }

    public void internalFrameClosed(InternalFrameEvent e) {
    }

    public void internalFrameDeactivated(InternalFrameEvent e) {
    }

    public void internalFrameDeiconified(InternalFrameEvent e) {
    }

    public void internalFrameIconified(InternalFrameEvent e) {
    }

    public void internalFrameOpened(InternalFrameEvent e) {
    }

    public void drop() {
        try {
            handleDrop(null, Toolkit.getDefaultToolkit().getSystemClipboard().getContents(this));
        } catch (Exception ex) {
            ex.printStackTrace();
        }
    }

    public void handleDrop(DropTargetDropEvent e, Transferable t) throws Exception {
        System.out.println("Starting dropAttempt");
        DataFlavor chosen = DataFlavor.javaFileListFlavor;
        DataFlavor second = FileTransferable.plainTextFlavor;
        DataFlavor flavor = null;
        Object data = null;
        if (e != null) {
            flavor = e.getCurrentDataFlavors()[0];
            e.acceptDrop(acceptableActions);
            Class c = flavor.getDefaultRepresentationClass();
        }
        if (flavor == null) {
            flavor = second;
        }
        String name = "";
        if (t.isDataFlavorSupported(chosen)) {
            System.out.println("Using List DnD style");
            java.util.List myList = (java.util.List) t.getTransferData(chosen);
            File[] f = (File[]) myList.toArray();
            for (int i = 0; i < f.length; i++) {
                name = f[i].getAbsolutePath();
                System.out.println("DnD file: " + name);
            }
            draggedTransfer(f, name);
        } else if (t.isDataFlavorSupported(second)) {
            System.out.println("Using text/plain DnD style");
            data = t.getTransferData(flavor);
            String str = "";
            int i = 0;
            if (data instanceof Reader) {
                int c;
                while ((c = ((Reader) data).read()) != -1) {
                    if (((i == 1) && (c == 0))) {
                        i = -1;
                    } else {
                        str = str + new Character((char) c).toString();
                    }
                    i++;
                }
            } else {
                str = "" + data;
            }
            System.out.println("Object data: \"" + str + "\"");
            if (str.startsWith("<")) {
                Log.debug("Mozilla DnD detected (preparsing)");
                str = str.substring(str.indexOf("\"") + 1);
                str = str.substring(0, str.indexOf("\""));
                Log.debug("Parsed data: " + str);
            }
            if (str.indexOf("[") >= 0) {
                Log.debug("Windows DnD detected");
                name = str.substring(str.indexOf("[") + 1);
                name = name.substring(0, name.lastIndexOf("]"));
            } else if (new String(str).startsWith("file://")) {
                name = str.substring(7);
                Log.debug("File URL DnD detected: " + name);
            }
            if (!new File(name).exists()) {
                System.out.println("No file string in clipboard: " + name);
                return;
            }
            System.out.println("DnD file: " + name);
            File[] f1 = new File[1];
            f1[0] = new File(name);
            draggedTransfer(f1, name);
        }
    }

    private void draggedTransfer(File[] f, String name) {
        if ((f.length == 1) && f[0].isFile()) {
            String path = "";
            if (name.indexOf("/") >= 0) {
                path = name.substring(0, name.lastIndexOf("/") + 1);
                name = name.substring(name.lastIndexOf("/") + 1);
            }
            Log.debug("DnD: " + path + " -> " + name);
            if (!path.trim().equals("")) {
                ((LocalDir) localDir).chdir(path);
            }
            ((LocalDir) localDir).startTransfer(new DirEntry(name, ((ActionListener) localDir)));
        } else {
            Log.debug("Dragging multiple files or dirs is not yet supported.");
        }
    }

    class DTListener implements DropTargetListener {

        public void dragEnter(DropTargetDragEvent e) {
            e.acceptDrag(JFtp.acceptableActions);
        }

        public void dragOver(DropTargetDragEvent e) {
            e.acceptDrag(JFtp.acceptableActions);
        }

        public void dropActionChanged(DropTargetDragEvent e) {
            e.acceptDrag(JFtp.acceptableActions);
        }

        public void dragExit(DropTargetEvent e) {
        }

        public void drop(DropTargetDropEvent e) {
            try {
                handleDrop(e, e.getTransferable());
                e.dropComplete(true);
                UpdateDaemon.updateRemoteDir();
            } catch (Throwable t) {
                t.printStackTrace();
                e.dropComplete(false);
                return;
            }
        }
    }
}
