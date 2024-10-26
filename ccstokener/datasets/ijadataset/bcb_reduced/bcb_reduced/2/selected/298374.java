package net.sf.jsharing.components;

import java.awt.Component;
import java.awt.Desktop;
import java.awt.Dimension;
import java.awt.Insets;
import java.awt.Rectangle;
import java.awt.Toolkit;
import java.io.BufferedReader;
import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.FileReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.PrintWriter;
import java.net.URI;
import java.net.URL;
import java.net.URLConnection;
import java.util.Calendar;
import java.util.Date;
import java.util.LinkedHashMap;
import java.util.Map.Entry;
import java.util.Properties;
import javax.swing.JOptionPane;
import javax.swing.filechooser.FileSystemView;
import net.sf.jsharing.boundary.LogConsole;
import net.sf.jsharing.boundary.MainWindow;
import org.apache.log4j.Level;
import pratikabu.logging.log4j.FileLog;
import pratikabu.logging.log4j.Log;

/**
 * 
 * @author Pratik
 */
public class UsefulMethods {

    public static int DOWNLOAD_FILES = 1, PROCESS_TRANSFERRABLE_OBJECT = 2;

    public static Properties props = new Properties();

    public static LinkedHashMap<String, SavedIPInfo> savedIPs = new LinkedHashMap<String, SavedIPInfo>();

    public static final String P_PORT_NUMBER_KEY = "portNumber";

    public static final String P_LAST_SAVE_LOCATION = "lastSaveLoc";

    public static final String P_SERVER_NAME = "serverName";

    public static final String P_MAIN_WINDOW_SHOW_ON_LOAD = "mainWindowShow";

    public static final String P_SYSTEM_ICON_LOAD = "systemIconLoad";

    public static final String P_CHUNK_SIZE = "chunkSize";

    public static final String P_LAST_UPLOAD_COUNT = "lastUploadCount";

    public static final String P_LAST_DOWNLOAD_COUNT = "lastDownloadCount";

    public static final String LOCAL_HOST_IP = "127.0.0.1";

    public static final String URL_HOME_PAGE = "http://pratikabu.users.sourceforge.net";

    public static final String URL_JSHARING_HOME_PAGE = "http://jsharing.sourceforge.net";

    public static final String URL_HOW_TO_USE_PAGE = URL_JSHARING_HOME_PAGE + "/how_to_use/index.html";

    public static final String URL_LATEST_VERSION = URL_JSHARING_HOME_PAGE + "/latest_vesrion.txt";

    public static final String URL_UPDATE_PAGE = URL_JSHARING_HOME_PAGE + "/update/index.html";

    public static Log log;

    public static LogConsole serverConsole, clientConsole;

    public static final String APPLICATION_VERSION = "0.0.1";

    private static long totalUploadedBytesCount = 0, totalDownloadedBytesCount = 0;

    public static void loadData() {
        loadProperties();
        loadSavedIPs();
        File folder = new File(FileModule.LOG_FOLDER);
        if (!folder.exists()) folder.mkdirs();
        Calendar cal = Calendar.getInstance();
        String date = cal.get(Calendar.DATE) + "-" + cal.get(Calendar.MONDAY) + "-" + cal.get(Calendar.YEAR);
        try {
            log = new FileLog("JSharing Logs", FileModule.LOG_FOLDER + File.separatorChar + date + ".xml", true);
        } catch (IOException e) {
            e.printStackTrace();
        }
        totalUploadedBytesCount = getLastSavedUploadCount();
        totalDownloadedBytesCount = getLastSavedDownloadCount();
        serverConsole = new LogConsole("Server Console");
        clientConsole = new LogConsole("Client Console");
    }

    /**
	 * Fetches the port number from the properties file. If available then
	 * returns it else sets the default port number.
	 * 
	 * @return
	 */
    public static Integer getPortNumber() {
        String value = props.getProperty(P_PORT_NUMBER_KEY);
        try {
            return Integer.parseInt(value);
        } catch (Exception e) {
            return 21212;
        }
    }

    /**
	 * Gives the last used save location.
	 * 
	 * @return
	 */
    public static String getLastSavedLocation() {
        String value = props.getProperty(P_LAST_SAVE_LOCATION);
        if (value == null) value = FileSystemView.getFileSystemView().getDefaultDirectory().getAbsolutePath();
        return value;
    }

    /**
	 * Get the object of Saved IPs.
	 * 
	 * @param ip
	 * @return
	 */
    public static SavedIPInfo getSavedIPInfo(String ip) {
        return savedIPs.get(ip);
    }

    /**
	 * Get the saved port number for the passed IP.
	 * 
	 * @param ip
	 * @return
	 */
    public static Integer getPortNumberForIP(String ip) {
        SavedIPInfo sip = getSavedIPInfo(ip);
        if (sip != null) return sip.getPort(); else return getPortNumber();
    }

    public static boolean isIPSaved(String ip) {
        return savedIPs.containsKey(ip);
    }

    public static String getShortNameOfIP(String ip) {
        SavedIPInfo sip = getSavedIPInfo(ip);
        if (sip != null) return sip.getName(); else return ip;
    }

    /**
	 * Representable form of the file size.
	 * 
	 * @param size
	 *            The file size.
	 * @return
	 */
    public static String getFileSize(long size) {
        String strSize;
        double tempSize = size / 1024.0;
        if (size < 1024) strSize = String.format("%.2f", tempSize * 1024.0) + " Bytes"; else if (tempSize < 1024) strSize = String.format("%.2f", tempSize) + " KB"; else if ((tempSize = tempSize / 1024.0) < 1024) strSize = String.format("%.2f", tempSize) + " MB"; else strSize = String.format("%.2f", tempSize / 1024.0) + " GB";
        return strSize;
    }

    private static void loadProperties() {
        try {
            FileInputStream fis = new FileInputStream(new File(FileModule.PROPERTIES_LOCATION));
            props.load(fis);
            fis.close();
        } catch (IOException e) {
        }
    }

    private static void loadSavedIPs() {
        try {
            FileReader fr = new FileReader(new File(FileModule.SAVED_IP_LOCATION));
            BufferedReader br = new BufferedReader(fr);
            String line;
            while ((line = br.readLine()) != null) {
                try {
                    SavedIPInfo sip = new SavedIPInfo();
                    String[] data = line.split(",");
                    sip.setIp(data[0]);
                    sip.setName(data[1]);
                    sip.setPort(Integer.parseInt(data[2]));
                    savedIPs.put(sip.getIp(), sip);
                } catch (Exception e) {
                }
            }
            br.close();
            fr.close();
        } catch (IOException e) {
        }
    }

    public static void saveData() {
        props.put(P_LAST_UPLOAD_COUNT, totalUploadedBytesCount + "");
        props.put(P_LAST_DOWNLOAD_COUNT, totalDownloadedBytesCount + "");
        saveProperties();
        saveSavedIPs();
    }

    public static void saveProperties() {
        try {
            props.store(new FileOutputStream(new File(FileModule.PROPERTIES_LOCATION)), "The properties have been modified at: " + new Date());
        } catch (IOException e) {
            log.log(Level.ERROR, "Error saving properties file.", e);
        }
    }

    public static void saveSavedIPs() {
        try {
            PrintWriter pw = new PrintWriter(new File(FileModule.SAVED_IP_LOCATION));
            boolean firstLine = true;
            for (Entry<String, SavedIPInfo> entry : savedIPs.entrySet()) {
                SavedIPInfo sip = entry.getValue();
                if (firstLine) firstLine = false; else pw.println();
                pw.print(sip.getIp() + "," + sip.getName() + "," + sip.getPort());
            }
            pw.close();
        } catch (IOException e) {
            log.log(Level.ERROR, "Error saving properties file.", e);
        }
    }

    public static void placeAtRightBottomLocation(Component c) {
        Toolkit toolkit = Toolkit.getDefaultToolkit();
        Dimension d = toolkit.getScreenSize();
        Insets i = toolkit.getScreenInsets(c.getGraphicsConfiguration());
        Rectangle r = c.getBounds();
        d.width = d.width - (i.left + i.right);
        d.height = d.height - (i.bottom + i.top);
        r.x = d.width - c.getWidth();
        r.y = d.height - c.getHeight();
        c.setBounds(r);
    }

    public static String getComputerName() {
        String value = props.getProperty(P_SERVER_NAME);
        if (value != null) return value; else return System.getProperty("user.name");
    }

    public static int getChunkSize() {
        int chunkSize;
        try {
            chunkSize = Integer.parseInt(props.getProperty(P_CHUNK_SIZE));
        } catch (Exception e) {
            chunkSize = 1024;
        }
        return chunkSize;
    }

    public static boolean getBooleanDefaultTrue(String key) {
        String value = UsefulMethods.props.getProperty(key);
        if (value != null) {
            try {
                return Boolean.parseBoolean(value);
            } catch (Exception e) {
            }
        }
        return true;
    }

    public static void browse(String urlStr) {
        if (Desktop.isDesktopSupported()) {
            try {
                Desktop.getDesktop().browse(new URI(urlStr));
            } catch (Exception e) {
            }
        } else {
            JOptionPane.showMessageDialog(MainWindow.mw, "Cannot open the URL. Kindly open this url\n" + "from your browser:\n" + urlStr);
        }
    }

    /**
	 * 
	 * @param urlString
	 *            the address of the source file
	 * @param outputFile
	 *            the location of the destination file
	 * @return
	 */
    public static String checkForUpdate() {
        InputStream is = null;
        ByteArrayOutputStream bos = null;
        try {
            URL url = new URL(URL_LATEST_VERSION);
            URLConnection ucnn = url.openConnection();
            bos = new ByteArrayOutputStream();
            is = ucnn.getInputStream();
            byte[] data = new byte[256];
            int offset;
            while ((offset = is.read(data)) != -1) {
                bos.write(data, 0, offset);
            }
            String version = bos.toString();
            if (version == null || version.equals("null")) version = null;
            return version;
        } catch (Exception ex) {
        } finally {
            try {
                is.close();
            } catch (Exception e) {
            }
            try {
                bos.close();
            } catch (Exception e) {
            }
        }
        return null;
    }

    public static synchronized void addUploadedBytes(long bytes) {
        totalUploadedBytesCount += bytes;
    }

    public static synchronized void addDownloadedBytes(long bytes) {
        totalDownloadedBytesCount += bytes;
    }

    public static long getTotalDownloadedBytesCount() {
        return totalDownloadedBytesCount;
    }

    public static long getTotalUploadedBytesCount() {
        return totalUploadedBytesCount;
    }

    public static synchronized void clearCounters() {
        totalDownloadedBytesCount = 0;
        totalUploadedBytesCount = 0;
    }

    public static long getLastSavedUploadCount() {
        try {
            return Long.parseLong(props.getProperty(P_LAST_UPLOAD_COUNT));
        } catch (Exception e) {
            return 0;
        }
    }

    public static long getLastSavedDownloadCount() {
        try {
            return Long.parseLong(props.getProperty(P_LAST_DOWNLOAD_COUNT));
        } catch (Exception e) {
            return 0;
        }
    }
}
