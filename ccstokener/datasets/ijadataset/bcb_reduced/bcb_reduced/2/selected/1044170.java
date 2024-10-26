package com.inetmon.jn.nwd;

import hsqldb.core.ui.DbClass;
import java.io.BufferedOutputStream;
import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.io.PrintWriter;
import java.net.HttpURLConnection;
import java.net.InetSocketAddress;
import java.net.MalformedURLException;
import java.net.URL;
import java.net.UnknownHostException;
import java.nio.ByteBuffer;
import java.nio.CharBuffer;
import java.nio.channels.SocketChannel;
import java.nio.charset.Charset;
import java.nio.charset.CharsetDecoder;
import java.nio.charset.CharsetEncoder;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.Enumeration;
import java.util.List;
import java.util.Vector;
import java.util.zip.ZipEntry;
import java.util.zip.ZipFile;
import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.Platform;
import org.eclipse.core.runtime.Status;
import org.eclipse.core.runtime.jobs.ILock;
import org.eclipse.core.runtime.jobs.Job;
import org.eclipse.jface.action.Action;
import org.eclipse.jface.preference.PreferenceStore;
import org.eclipse.jface.resource.ImageDescriptor;
import org.eclipse.swt.SWT;
import org.eclipse.swt.widgets.Display;
import org.eclipse.swt.widgets.Shell;
import org.eclipse.swt.widgets.ToolTip;
import org.eclipse.swt.widgets.TrayItem;
import org.eclipse.ui.PlatformUI;
import org.eclipse.ui.plugin.AbstractUIPlugin;
import org.eclipse.ui.progress.IProgressConstants;
import org.osgi.framework.BundleContext;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.Node;
import com.inetmon.jn.core.CorePlugin;
import com.inetmon.jn.nwd.toaster.Popup;
import com.inetmon.jn.nwd.toaster.Toaster;
import com.inetmon.jn.nwd.views.NWDMainView;

/**
 * The main plugin class to be used in the desktop.
 * 
 * @author Aw Chen Loong & Lo Peng Foo
 * @version 1.0.0
 */
public class Activator extends AbstractUIPlugin {

    private static Activator plugin;

    private static TrayItem trayItem;

    private static int status = NWDStatus.ENABLED;

    public static DbClass wormDB;

    private static String appPath = Platform.getLocation().toString();

    private static Vector result = null;

    private static ILock lock = Platform.getJobManager().newLock();

    public static int hash_array[] = new int[512];

    public static List list;

    public static PreferenceStore prefStore;

    static Job updateJob;

    static boolean updateRunning = false;

    private static String distributor;

    private static String MAC;

    private static String serial;

    private static URL url;

    private static String download;

    private static String site;

    private static String htmlMessage;

    private static String latestVersion;

    private static String fileLoc;

    private static String[] oldLoc = { appPath + "/temp/wormDB.script", appPath + "/temp/wormDB.data" };

    private static String[] newLoc = { appPath + "/db/wormDB.script", appPath + "/db/wormDB.data" };

    private static Toaster toaster;

    private static PacketPrinter p;

    public static Vector wormlist;

    static Popup pop;

    public static boolean popExist = false;

    /**
	 * The constructor.
	 */
    public Activator() {
        plugin = this;
    }

    /**
	 * This method is called upon plug-in activation
	 */
    public void start(BundleContext context) throws Exception {
        super.start(context);
        prefStore = new PreferenceStore(appPath + "/nwd.pref");
        wormlist = new Vector();
        p = new PacketPrinter();
        CorePlugin.getDefault().getRawPacketHandler().addRawPacketListener(p);
        try {
            prefStore.load();
        } catch (IOException e) {
            setValue(NWDConfigKey.ALERT_POPUP, true);
            setValue(NWDConfigKey.ALERT_BALLOON, false);
            setValue(NWDConfigKey.ALERT_VOICE, true);
            setValue(NWDConfigKey.WARN_POPUP, false);
            setValue(NWDConfigKey.WARN_BALLOON, true);
            setValue(NWDConfigKey.WARN_VOICE, true);
            setValue(NWDConfigKey.AUTO_UPDATE, true);
            savePrefStore();
        }
        setValue(NWDConfigKey.DETECTION, true);
        initialArray();
    }

    /**
	 * This method is called when the plug-in is stopped
	 */
    public void stop(BundleContext context) throws Exception {
        super.stop(context);
        if (!wormDB.getConn().isClosed()) shutdownDB();
        plugin = null;
    }

    /**
	 * Returns the shared instance.
	 *
	 * @return the shared instance.
	 */
    public static Activator getDefault() {
        return plugin;
    }

    /**
	 * Returns an image descriptor for the image file at the given
	 * plug-in relative path.
	 *
	 * @param path the path
	 * @return the image descriptor
	 */
    public static ImageDescriptor getImageDescriptor(String path) {
        return AbstractUIPlugin.imageDescriptorFromPlugin("com.inetmon.jn.nwd", path);
    }

    /**
	 * Set the status of Network Detection System.
	 *  
	 * @param state Contants value of NWDStatus 
	 * @see Activator.getStatus() 
	 */
    public static void setStatus(int state) {
        status = state;
    }

    /**
	 * Return the status of Network Worm Detection.
	 * 
	 * @return Contstants value of NWDStatus
	 * @see Activator.setStatus(int)
	 */
    public static int getStatus() {
        return status;
    }

    /**
	 * Initialize Operation of Network Worm Detection Database.
	 *
	 * @see Activator.shutdownDB()
	 */
    public static void initializeDB() {
        try {
            wormDB = new DbClass("wormDB", appPath + "/db/");
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    /**
	 * Shutdown Operation of Network Worm Detection Database.
	 * 
	 * @see Activator.initializeDB()
	 */
    public static void shutdownDB() {
        try {
            wormDB.shutdown();
        } catch (SQLException e) {
            e.printStackTrace();
        }
    }

    /**
	 * Set the Network Worm Detection Configuration Value.
	 * 
	 * @param key Constants value of NWDConfigKey
	 * @param value boolean value
	 * @see Activator.getValue(String)
	 */
    public static void setValue(String key, boolean value) {
        prefStore.setValue(key, value);
    }

    /**
	 * Get the value of Network Worm Detection Configuration
	 * based on the key specified.
	 * 
	 * @param key Constants value of NWDConfigKey
	 * @return boolean value.
	 * @see Activator.setValue(String, boolean)
	 */
    public static boolean getValue(String key) {
        return prefStore.getBoolean(key);
    }

    /**
	 * Save the Network Worm Detection Configuration.
	 */
    public static void savePrefStore() {
        try {
            prefStore.save(new FileOutputStream(appPath + "/nwd.pref"), "NWD Configuration");
        } catch (IOException e) {
        }
    }

    /**
	 * Database Query for SELECT Command.
	 *  
	 * @param queryString SELECT Command
	 * @return query result in Vector type
	 * @see Activator.updateDB(String) 
	 */
    public static Vector queryDB(String queryString) {
        result = null;
        initializeDB();
        try {
            result = wormDB.queryDB(queryString);
        } catch (SQLException e) {
            e.printStackTrace();
        }
        shutdownDB();
        return result;
    }

    /**
	 * Database Query for UPDATE Command.
	 * 
	 * @param updateString UPDATE Command
	 * @see Activator.queryDB(String)
	 */
    public void updateDB(String updateString) {
        initializeDB();
        try {
            wormDB.updateDB(updateString);
        } catch (SQLException e) {
            e.printStackTrace();
        }
        shutdownDB();
    }

    public static void dbUpdate(String fileURL) {
        initializeDB();
        wormDB.runScript(fileURL);
        shutdownDB();
    }

    /**
	 * Get the current working folder's path.
	 * 
	 * @return the working folder's path
	 */
    public static String getAppPath() {
        return appPath;
    }

    /**
	 * Get version value of Network Worm Detection Database.
	 * 
	 * @return version of Network Worm Detection Database
	 * @see Activator.getDBID()
	 * @see Activator.getDBLastUpdated()
	 */
    public static String getDBVersion() {
        Vector element = new Vector();
        initializeDB();
        try {
            result = wormDB.queryDB("SELECT VERSION_NO FROM VERSION");
        } catch (SQLException e) {
            e.printStackTrace();
        }
        shutdownDB();
        element = (Vector) result.firstElement();
        return element.firstElement().toString();
    }

    /**
	 * Get the ID value of Network Worm Detection Database.
	 * 
	 * @return ID of Network Worm Detection Database
	 * @see Activator.getDBVersion()
	 * @see Activator.getDBLastUpdated()
	 */
    public static String getDBID() {
        Vector element = new Vector();
        initializeDB();
        try {
            result = wormDB.queryDB("SELECT VERSION_ID FROM VERSION");
        } catch (SQLException e) {
            e.printStackTrace();
        }
        shutdownDB();
        element = (Vector) result.firstElement();
        return element.firstElement().toString();
    }

    /**
	 * Get the Last Updated Date of Network Worm Detection Database
	 *  
	 * @return the date in String type, "yyyy-mm-hh"
	 * @see Activator.getDBVersion()
	 * @see Activator.getDBID()
	 */
    public static String getDBLastUpdated() {
        Vector element = new Vector();
        initializeDB();
        try {
            result = wormDB.queryDB("SELECT LAST_UPDATED FROM VERSION");
        } catch (SQLException e) {
            e.printStackTrace();
        }
        element = (Vector) result.firstElement();
        shutdownDB();
        return element.firstElement().toString();
    }

    /**
	 * Check updates for Network Worm Detection Database.
	 */
    public static void runDBUpdate() {
        if (!updateRunning) {
            if (updateJob != null) updateJob.cancel();
            updateJob = new Job("Worm Database Update") {

                protected IStatus run(IProgressMonitor monitor) {
                    try {
                        updateRunning = true;
                        distributor = getFromFile("[SERVER]", "csz", getAppPath() + "/server.ini");
                        MAC = getFromFile("[SPECIFICINFO]", "MAC", getAppPath() + "/register.ini");
                        serial = getFromFile("[SPECIFICINFO]", "serial", getAppPath() + "/register.ini");
                        if (MAC.equals("not_found") || serial.equals("not_found") || !serial.startsWith(distributor)) {
                            try {
                                MAC = getFromFile("[SPECIFICINFO]", "MAC", getAppPath() + "/register.ini");
                                serial = getFromFile("[SPECIFICINFO]", "serial", getAppPath() + "/register.ini");
                            } catch (Exception e) {
                                System.out.println(e);
                            }
                        } else {
                            try {
                                url = new URL("http://" + getFromFile("[SERVER]", "url", getAppPath() + "/server.ini"));
                            } catch (MalformedURLException e) {
                                System.out.println(e);
                            }
                            download = "/download2.php?distributor=" + distributor + "&&mac=" + MAC + "&&serial=" + serial;
                            readXML();
                            if (htmlMessage.contains("error")) {
                                try {
                                    PrintWriter pw = new PrintWriter(getAppPath() + "/register.ini");
                                    pw.write("");
                                    pw.close();
                                } catch (IOException e) {
                                    System.out.println(e);
                                }
                                setProperty(IProgressConstants.ICON_PROPERTY, IconImg.liveUpIco);
                                if (isModal(this)) {
                                    showResults2();
                                } else {
                                    setProperty(IProgressConstants.KEEP_PROPERTY, Boolean.TRUE);
                                    setProperty(IProgressConstants.ACTION_PROPERTY, getUpdateCompletedAction2());
                                }
                            } else {
                                if (!getDBVersion().equals(latestVersion)) {
                                    try {
                                        OutputStream out = null;
                                        HttpURLConnection conn = null;
                                        InputStream in = null;
                                        int size;
                                        try {
                                            URL url = new URL(fileLoc);
                                            String outFile = getAppPath() + "/temp/" + getFileName(url);
                                            File oFile = new File(outFile);
                                            oFile.delete();
                                            out = new BufferedOutputStream(new FileOutputStream(outFile));
                                            monitor.beginTask("Connecting to NWD Server", 100);
                                            conn = (HttpURLConnection) url.openConnection();
                                            conn.setConnectTimeout(20000);
                                            conn.connect();
                                            if (conn.getResponseCode() / 100 != 2) {
                                                System.out.println("Error: " + conn.getResponseCode());
                                                return null;
                                            }
                                            monitor.worked(100);
                                            monitor.done();
                                            size = conn.getContentLength();
                                            monitor.beginTask("Download Worm Definition", size);
                                            in = conn.getInputStream();
                                            byte[] buffer;
                                            String downloadedSize;
                                            long numWritten = 0;
                                            boolean status = true;
                                            while (status) {
                                                if (size - numWritten > 1024) {
                                                    buffer = new byte[1024];
                                                } else {
                                                    buffer = new byte[(int) (size - numWritten)];
                                                }
                                                int read = in.read(buffer);
                                                if (read == -1) {
                                                    break;
                                                }
                                                out.write(buffer, 0, read);
                                                numWritten += read;
                                                downloadedSize = Long.toString(numWritten / 1024) + " KB";
                                                monitor.worked(read);
                                                monitor.subTask(downloadedSize + " of " + Integer.toString(size / 1024) + " KB");
                                                if (size == numWritten) {
                                                    status = false;
                                                }
                                                if (monitor.isCanceled()) return Status.CANCEL_STATUS;
                                            }
                                            if (in != null) {
                                                in.close();
                                            }
                                            if (out != null) {
                                                out.close();
                                            }
                                            try {
                                                ZipFile zFile = new ZipFile(outFile);
                                                Enumeration all = zFile.entries();
                                                while (all.hasMoreElements()) {
                                                    ZipEntry zEntry = (ZipEntry) all.nextElement();
                                                    long zSize = zEntry.getSize();
                                                    if (zSize > 0) {
                                                        if (zEntry.getName().endsWith("script")) {
                                                            InputStream instream = zFile.getInputStream(zEntry);
                                                            FileOutputStream fos = new FileOutputStream(oldLoc[0]);
                                                            int ch;
                                                            while ((ch = instream.read()) != -1) {
                                                                fos.write(ch);
                                                            }
                                                            instream.close();
                                                            fos.close();
                                                        } else if (zEntry.getName().endsWith("data")) {
                                                            InputStream instream = zFile.getInputStream(zEntry);
                                                            FileOutputStream fos = new FileOutputStream(oldLoc[1]);
                                                            int ch;
                                                            while ((ch = instream.read()) != -1) {
                                                                fos.write(ch);
                                                            }
                                                            instream.close();
                                                            fos.close();
                                                        }
                                                    }
                                                }
                                                File xFile = new File(outFile);
                                                xFile.deleteOnExit();
                                            } catch (Exception e) {
                                                e.printStackTrace();
                                            }
                                            try {
                                                monitor.done();
                                                monitor.beginTask("Install Worm Definition", 10000);
                                                monitor.worked(2500);
                                                CorePlugin.getDefault().getRawPacketHandler().removeRawPacketListener(p);
                                                p = null;
                                                if (!wormDB.getConn().isClosed()) {
                                                    shutdownDB();
                                                }
                                                System.out.println(wormDB.getConn().isClosed());
                                                for (int i = 0; i < 2; i++) {
                                                    try {
                                                        Process pid = Runtime.getRuntime().exec("cmd /c copy \"" + oldLoc[i].replace("/", "\\") + "\" \"" + newLoc[i].replace("/", "\\") + "\"/y");
                                                    } catch (Exception e) {
                                                        e.printStackTrace();
                                                    }
                                                    new File(oldLoc[i]).deleteOnExit();
                                                }
                                                monitor.worked(2500);
                                                initialArray();
                                                p = new PacketPrinter();
                                                CorePlugin.getDefault().getRawPacketHandler().addRawPacketListener(p);
                                                monitor.worked(2500);
                                                monitor.done();
                                                setProperty(IProgressConstants.ICON_PROPERTY, IconImg.liveUpIco);
                                                if (isModal(this)) {
                                                    showResults();
                                                } else {
                                                    setProperty(IProgressConstants.KEEP_PROPERTY, Boolean.TRUE);
                                                    setProperty(IProgressConstants.ACTION_PROPERTY, getUpdateCompletedAction());
                                                }
                                            } catch (Exception e) {
                                                setProperty(IProgressConstants.ICON_PROPERTY, IconImg.liveUpIco);
                                                if (isModal(this)) {
                                                    showResults2();
                                                } else {
                                                    setProperty(IProgressConstants.KEEP_PROPERTY, Boolean.TRUE);
                                                    setProperty(IProgressConstants.ACTION_PROPERTY, getUpdateCompletedAction2());
                                                }
                                                System.out.println(e);
                                            } finally {
                                            }
                                        } catch (Exception e) {
                                            e.printStackTrace();
                                        }
                                    } catch (Exception e) {
                                        setProperty(IProgressConstants.ICON_PROPERTY, IconImg.liveUpIco);
                                        if (isModal(this)) {
                                            showResults2();
                                        } else {
                                            setProperty(IProgressConstants.KEEP_PROPERTY, Boolean.TRUE);
                                            setProperty(IProgressConstants.ACTION_PROPERTY, getUpdateCompletedAction2());
                                        }
                                        e.printStackTrace();
                                    }
                                } else {
                                    cancel();
                                    setProperty(IProgressConstants.ICON_PROPERTY, IconImg.liveUpIco);
                                    if (isModal(this)) {
                                        showResults1();
                                    } else {
                                        setProperty(IProgressConstants.KEEP_PROPERTY, Boolean.TRUE);
                                        setProperty(IProgressConstants.ACTION_PROPERTY, getUpdateCompletedAction1());
                                    }
                                }
                            }
                        }
                        return Status.OK_STATUS;
                    } catch (Exception e) {
                        showResults2();
                        return Status.OK_STATUS;
                    } finally {
                        lock.release();
                        updateRunning = false;
                        if (getValue("AUTO_UPDATE")) schedule(10800000);
                    }
                }
            };
            updateJob.schedule();
        }
    }

    /**
	 * Check whether the job is a background job or critical job.
	 * 
	 * @param job Job
	 * @return boolean value
	 */
    public static boolean isModal(Job job) {
        Boolean isModal = (Boolean) job.getProperty(IProgressConstants.PROPERTY_IN_DIALOG);
        if (isModal == null) return true;
        return isModal.booleanValue();
    }

    /**
	 * Show Update Operation Complete Result.
	 */
    protected static void showResults() {
        PlatformUI.getWorkbench().getDisplay().asyncExec(new Runnable() {

            public void run() {
                getUpdateCompletedAction().run();
            }
        });
    }

    protected static void showResults1() {
        PlatformUI.getWorkbench().getDisplay().asyncExec(new Runnable() {

            public void run() {
                getUpdateCompletedAction1().run();
            }
        });
    }

    protected static void showResults2() {
        PlatformUI.getWorkbench().getDisplay().asyncExec(new Runnable() {

            public void run() {
                getUpdateCompletedAction2().run();
            }
        });
    }

    /**
	 * Action that popup the update complete notification.
	 * 
	 * @return Action
	 */
    protected static Action getUpdateCompletedAction() {
        return new Action() {

            public void run() {
                NWDMainView.syncDBView();
                toaster = new Toaster();
                toaster.showToaster("Update Completed", getDBVersion());
            }
        };
    }

    protected static Action getUpdateCompletedAction1() {
        return new Action() {

            public void run() {
                NWDMainView.syncDBView();
                toaster = new Toaster();
                toaster.showToaster("Database is Up-to-date", getDBVersion());
            }
        };
    }

    protected static Action getUpdateCompletedAction2() {
        return new Action() {

            public void run() {
                NWDMainView.syncDBView();
                toaster = new Toaster();
                toaster.showToaster("Update Failed", "");
            }
        };
    }

    /**
	 * Initial Operation of Array Hashing. 
	 */
    public static void initialArray() {
        Vector signArray = null;
        signArray = queryDB("SELECT WORM_STRING FROM WORM_PATTERN");
        String sign[] = new String[signArray.size()];
        Vector signature;
        int hash_index;
        String s1;
        String s2;
        String s3;
        String s4;
        if (!signArray.isEmpty()) {
            for (int i = 0; i < signArray.size(); i++) {
                signature = (Vector) signArray.elementAt(i);
                sign[i] = signature.firstElement().toString();
            }
        }
        list = new ArrayList(Arrays.asList(sign));
        Collections.sort(list);
        for (int i = 0; i < hash_array.length; i++) hash_array[i] = -1;
        for (int i = 0; i < list.size() - 1; i++) {
            s1 = list.get(i).toString();
            s2 = list.get(i + 1).toString();
            s3 = s1.substring(0, 1);
            s4 = s2.substring(0, 1);
            if (i == 0) {
                hash_index = s3.hashCode();
                hash_array[hash_index] = list.indexOf(s1);
            }
            if (s3.compareTo(s4) != 0) {
                hash_index = s4.hashCode();
                hash_array[hash_index] = list.indexOf(s2);
            }
        }
    }

    public static void switchTrayDefault() {
        final TrayItem tray = CorePlugin.getDefault().getTrayItem();
        if (getStatus() == NWDStatus.ENABLED) {
            tray.setImage(IconTray.enabledIco.createImage());
            tray.setToolTipText(TrayToolTip.ENABLED);
        } else if (getStatus() == NWDStatus.DISABLED) {
            tray.setImage(IconTray.disabledIco.createImage());
            tray.setToolTipText(TrayToolTip.DISABLED);
        }
    }

    /**
	 * Get information from ini file.
	 * 
	 * @param title
	 * @param item
	 * @param filename
	 * @return value
	 */
    public static String getFromFile(String title, String item, String filename) {
        try {
            BufferedReader in = new BufferedReader(new FileReader(filename));
            String temp = null;
            while ((temp = in.readLine()) != null) {
                if (temp.equals(title)) {
                    while ((temp = in.readLine()) != null) {
                        if (temp.contains(item)) {
                            int index = temp.indexOf("=");
                            in.close();
                            return temp.substring(index + 1, temp.length());
                        }
                    }
                }
            }
        } catch (FileNotFoundException e) {
        } catch (IOException e) {
        }
        return "not_found";
    }

    /**
	 * Download XML from server and get information from file
	 * Information retrieved:
	 * sites: download site
	 * html message: error?
	 * download path: file path
	 * new location: new file location
	 *
	 */
    public static void readXML() {
        SocketChannel channel = null;
        try {
            Charset charset = Charset.forName("ISO-8859-1");
            CharsetDecoder decoder = charset.newDecoder();
            CharsetEncoder encoder = charset.newEncoder();
            ByteBuffer buffer = ByteBuffer.allocateDirect(1024);
            CharBuffer charbuffer = CharBuffer.allocate(1024);
            InetSocketAddress socketAddress = new InetSocketAddress(url.getHost(), 80);
            channel = SocketChannel.open();
            channel.connect(socketAddress);
            String request = "Get " + url + download + " \r\n\r\n";
            channel.write(encoder.encode(CharBuffer.wrap(request)));
            BufferedWriter bw = new BufferedWriter(new FileWriter("down.xml", true));
            while (channel.read(buffer) != -1) {
                buffer.flip();
                decoder.decode(buffer, charbuffer, false);
                charbuffer.flip();
                bw.append(charbuffer.toString());
                buffer.clear();
                charbuffer.clear();
            }
            bw.close();
        } catch (UnknownHostException e) {
            System.out.println(e);
        } catch (IOException e) {
            System.out.println(e);
        } catch (Exception e) {
        } finally {
            if (channel != null) {
                try {
                    channel.close();
                } catch (IOException e) {
                    System.out.println(e);
                }
            }
        }
        File xmlFile = new File("down.xml");
        Document doc = null;
        try {
            DocumentBuilderFactory dbf = DocumentBuilderFactory.newInstance();
            DocumentBuilder db = dbf.newDocumentBuilder();
            doc = db.parse(xmlFile);
            Element root = doc.getDocumentElement();
            for (Node child = root.getFirstChild(); child != null; child = child.getNextSibling()) {
                if (child.getNodeName().equals("CONSTANTS")) {
                    for (Node subChild = child.getFirstChild(); subChild != null; subChild = subChild.getNextSibling()) {
                        if (subChild.hasAttributes()) {
                            if (subChild.getAttributes().item(0).getTextContent().contains("%site%")) {
                                site = subChild.getAttributes().item(1).getTextContent();
                            }
                        }
                    }
                }
                if (child.getNodeName().equals("HTMLMESSAGE")) {
                    for (Node subChild = child.getFirstChild(); subChild != null; subChild = subChild.getNextSibling()) {
                        if (subChild.getNodeName().equals("URL")) {
                            htmlMessage = subChild.getTextContent().replace("%site%", site);
                        }
                    }
                }
                if (child.getNodeName().equals("CONSTANTS")) {
                    for (Node subChild = child.getFirstChild(); subChild != null; subChild = subChild.getNextSibling()) {
                        if (subChild.hasAttributes()) {
                            if (subChild.getAttributes().item(0).getTextContent().contains("%version%")) {
                                latestVersion = subChild.getAttributes().item(1).getTextContent();
                            }
                        }
                    }
                }
                if (child.getNodeName().equals("FILES")) {
                    for (Node child1 = child.getFirstChild(); child1 != null; child1 = child1.getNextSibling()) {
                        if (child1.getNodeName().equals("FILE")) {
                            for (Node child2 = child1.getFirstChild(); child2 != null; child2 = child2.getNextSibling()) {
                                if (child2.getNodeName().equals("ACTION")) {
                                    if (child2.getAttributes().item(0).getTextContent().contains("download")) {
                                        for (Node child3 = child2.getFirstChild(); child3 != null; child3 = child3.getNextSibling()) {
                                            if (child3.getNodeName().equals("LOCATION")) {
                                                fileLoc = child3.getTextContent().replace("%site%", site);
                                            }
                                        }
                                    }
                                    if (child2.getAttributes().item(0).getTextContent().contains("copy")) {
                                        for (Node child3 = child2.getFirstChild(); child3 != null; child3 = child3.getNextSibling()) {
                                            if (child3.getNodeName().equals("OLDLOCATION")) {
                                            }
                                            if (child3.getNodeName().equals("NEWLOCATION")) {
                                            }
                                        }
                                    }
                                }
                            }
                        }
                    }
                }
            }
        } catch (Exception e) {
        }
        xmlFile.delete();
    }

    /**
	 * Get the filename from the URL given
	 * 
	 * @param u
	 * @return
	 */
    public static String getFileName(URL u) {
        return u.getFile().substring(u.getFile().lastIndexOf('/') + 1);
    }

    public static void showBalloon(String message, int severity) {
        TrayItem tray = CorePlugin.getDefault().getTrayItem();
        Display disp = tray.getParent().getDisplay();
        Shell shell = disp.getActiveShell();
        if (shell == null) {
            Shell[] shell2 = disp.getShells();
            for (int i = 0; i < shell2.length; i++) {
                if (shell2[i] != null) {
                    shell = shell2[i];
                }
            }
        }
        if (severity == Severity.Alert) {
            final ToolTip tip = new ToolTip(shell, SWT.BALLOON | SWT.ICON_INFORMATION);
            tip.setMessage(message);
            tip.setText("Alert : Worm is detected!");
            tray.setToolTip(tip);
            tip.setVisible(true);
        } else if (severity == Severity.Warning) {
            final ToolTip tip = new ToolTip(shell, SWT.BALLOON | SWT.ICON_INFORMATION);
            tip.setMessage(message);
            tip.setText("Warning : Suspicious Packet.");
            tray.setToolTip(tip);
            tip.setVisible(true);
        } else if (severity == 11) {
            final ToolTip tip = new ToolTip(shell, SWT.BALLOON | SWT.ICON_INFORMATION);
            tip.setMessage(message);
            tip.setText("Database Update");
            tray.setToolTip(tip);
            tip.setVisible(true);
        }
    }

    public static boolean showPop() {
        return pop.notifyFlag;
    }
}
