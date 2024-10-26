package de.doubleSlash.speedTracker.laptop;

import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.io.BufferedWriter;
import java.io.File;
import java.io.FileOutputStream;
import java.io.FileWriter;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.URL;
import java.net.URLConnection;
import java.text.SimpleDateFormat;
import java.util.Date;
import de.doubleSlash.speedTracker.DebugListener;
import de.doubleSlash.speedTracker.DownloadTracker;
import de.doubleSlash.speedTracker.DownloadTrackerUtils;
import de.doubleSlash.speedTracker.GPSAdapter;
import de.doubleSlash.speedTracker.GPSListener;
import de.doubleSlash.speedTracker.Tracker;
import de.doubleSlash.speedTracker.TrackingListener;
import de.doubleSlash.speedTracker.TrackingPoint;

/**
 * SpeedTracker application for desktop usage
 * 
 * @author cbalzer
 */
public class SpeedTracker implements GPSListener, TrackingListener, ActionListener, DebugListener, DownloadTrackerUtils {

    /** speedTracker main window */
    private MainFrame mainFrame = null;

    /** Manages tracking process */
    private Tracker tracker = null;

    /** Represents the current GPS adapter */
    private GPSAdapter adapter = null;

    /** BufferedWriter used by DebugListener */
    private BufferedWriter debugBuffer = null;

    /** State of the DebugLog  */
    private boolean isDebugEnabled = false;

    /** Date format for every debug file */
    private static SimpleDateFormat formatFilename = new SimpleDateFormat("yyyy-MM-dd_HH-mm-ss");

    /** Date format for every debug row */
    private static SimpleDateFormat formatItem = new SimpleDateFormat("yyyy-MM-dd,HH:mm:ss");

    /** Default read timeout for url connection */
    private int urlConnectionTimeout = 1000;

    /**
	 * Save log file counter value
	 */
    private long logCounter = 0;

    /**
	 * Save download counter value
	 */
    private long downloadCounter = 0;

    /**
	 * MediaPlayer to play simple wave sound
	 */
    private MediaPlayer player = null;

    /**
	 * Main application that shows up the SpeedTracker window
	 */
    public SpeedTracker(String args[]) {
        adapter = new BluetoothAdapter(this, this);
        tracker = new DownloadTracker(this, this, this);
        boolean expertMode = false;
        for (String s : args) {
            if (s.equals("-debug")) {
                setDebugEnabled(true);
            } else if (s.equals("-expert")) {
                expertMode = true;
            }
        }
        player = new MediaPlayer();
        mainFrame = new MainFrame(this, expertMode);
        showInitialMessages();
        mainFrame.setVisible(true);
        adapter.connect();
    }

    /**
	 * Shows the default gps-data and tracker messages
	 */
    private void showInitialMessages() {
        mainFrame.setTrackingButtonEnabled(true);
        mainFrame.setGPSData("0.0", "0.0");
        mainFrame.setGPSStatus("<font color='red'>Offline</font>");
        mainFrame.setTrackingResults("0", "0", "0", "");
        mainFrame.setTrackingStatus("<font color='red'>Stopped</font>");
        mainFrame.showTrackingDownloadIcon(false);
        this.downloadCounter = 0;
        this.logCounter = 0;
    }

    /**
	 * Start the SpeedTracker-application and display the main window
	 */
    public static void main(String args[]) {
        new SpeedTracker(args);
    }

    /**
     * If the GPSReader looses his connection to the GPSAdapter the lost connection event is called 
     */
    public void adapterLostConnectionEvent() {
        if (tracker.isTracking()) player.play();
        mainFrame.setGPSStatus("<font color='red'>Reconnecting...</font>");
        adapter.reconnect();
    }

    /**
	 * Notifies that the adapter has successfully established the connection 
	 */
    public void adapterConnectedEvent() {
        mainFrame.setGPSStatus("<font color='green'>Available</font>");
    }

    /**
	 * A new TrackingPoint object is defined by the GPSReader
	 */
    public void adapterReceivedDataEvent(TrackingPoint data) {
        if (data != null) {
            String latitude = Double.toString(data.getLatitude());
            if (latitude.substring(latitude.indexOf(".")).length() > 7) latitude = latitude.substring(0, latitude.indexOf(".") + 7);
            String longitude = Double.toString(data.getLongitude());
            if (longitude.substring(longitude.indexOf(".")).length() > 7) longitude = longitude.substring(0, longitude.indexOf(".") + 7);
            mainFrame.setGPSData(latitude, longitude);
            mainFrame.setGPSStatus("<font color='green'>Available</font>");
            tracker.setCurrentTrackingPoint(data);
        }
    }

    /**
	 * Receives an error message, so close the adapter connection, show message in gui and stop tracking
	 * @see de.de.doubleSlash.speedTracker.laptop.gps.doubleslash.speedtracker.gps.GPSListener#adapterErrorMessage(java.lang.String)
	 */
    public void adapterErrorEvent(String message) {
        if (tracker.isTracking()) player.play();
        showInitialMessages();
        if (adapter.isConnected()) adapter.close();
        mainFrame.setGPSStatus("<font color='red'>" + message + "</font>");
        if (tracker.isTracking()) tracker.stopTracking();
        mainFrame.setTrackingButtonEnabled(true);
    }

    /**
	 * Show tracking results in the user interface
	 * @see de.doubleSlash.speedTracker.TrackingListener#trackingReceivedResultsEvent
	 */
    public void trackingReceivedResultsEvent(long latency, long rate, long downloadCounter, long logCounter, String trackingFile) {
        mainFrame.setTrackingResults(Long.toString(latency), Long.toString((long) (rate / 1024.0)), Long.toString(tracker.getTrackingDownloadFileSize()), trackingFile);
        this.downloadCounter = downloadCounter;
        this.logCounter = logCounter;
    }

    /**
	 * Sets the current tracking pending event - download is pending
	 */
    public void trackingPendingEvent() {
        mainFrame.showTrackingDownloadIcon(true);
        mainFrame.setTrackingStatus("<font color='black'>Pending... (D:" + this.downloadCounter + " L:" + this.logCounter + ")</font>");
    }

    /**
	 * Sets the current tracking error message
	 * @param message
	 */
    public void trackingErrorEvent(String message) {
        if (tracker.isTracking()) player.play();
        mainFrame.setTrackingStatus("<font color='red'>" + message + "</font>");
    }

    /**
	 * Sets the current tracking downloading event - download has started
	 */
    public void trackingDownloadingEvent() {
        mainFrame.showTrackingDownloadIcon(false);
        mainFrame.setTrackingStatus("<font color='black'>Downloading...</font>");
    }

    /**
	 * Sets the current tracking error message AND stops tracking!
	 * @param message
	 */
    public void trackingStopEvent(String message) {
        if (tracker.isTracking()) player.play();
        showInitialMessages();
        adapter.close();
        if (tracker.isTracking()) tracker.stopTracking();
        mainFrame.setTrackingButtonEnabled(true);
        mainFrame.setTrackingStatus("<font color='red'>" + message + "</font>");
    }

    /**
	 * Handles user actions and performs the required tasks from MainFrame events
	 * @param ActionEvent event
	 */
    public void actionPerformed(ActionEvent event) {
        String command = event.getActionCommand();
        if (command.equals("quit")) {
            System.exit(0);
        } else if (command.equals("homepage")) {
            try {
                BareBonesBrowserLaunch.openURL("http://speedtrack.doubleSlash.de");
            } catch (Exception e) {
                logDebug(this, "failed to open url http://speedtrack.doubleSlash.de : " + e.toString());
            }
        } else if (command.equals("help")) {
            try {
                BareBonesBrowserLaunch.openURL("http://speedtrack.doubleSlash.de/help");
            } catch (Exception e) {
                logDebug(this, "failed to open url http://speedtrack.doubleSlash.de/help : " + e.toString());
            }
        } else if (command.equals("start_tracking")) {
            mainFrame.setTrackingButtonEnabled(false);
            if (!adapter.isConnected()) adapter.connect();
            try {
                tracker.setTrackingDownloadFile(mainFrame.getTrackingFile());
                tracker.setTrackingGPSDevice(mainFrame.getTrackingGPSDevice());
                tracker.setTrackingHardware(mainFrame.getTrackingHardware());
                tracker.setTrackingModem(mainFrame.getTrackingModem());
                tracker.setTrackingMovementType(String.valueOf(mainFrame.getTrackingMovementType()));
                tracker.setTrackingNetwork(String.valueOf(mainFrame.getTrackingNetwork()));
            } catch (IllegalArgumentException e) {
                this.trackingStopEvent("Failed to read settings");
                return;
            }
            mainFrame.setTrackingStatus("<font color='green'>Started</font>");
            mainFrame.setTrackingButtonEnabled(false);
            tracker.startTracking();
        } else if (command.equals("stop_tracking")) {
            if (tracker.isTracking()) tracker.stopTracking();
            mainFrame.setTrackingButtonEnabled(true);
            showInitialMessages();
        }
    }

    /**
     * Enables or diabled the tracking log
     * @param enabled
     */
    public void setDebugEnabled(boolean enabled) {
        this.isDebugEnabled = enabled;
    }

    /**
     * Log message to debug log file
     * @param object 
     * @param message String
     */
    public void logDebug(Object object, String message) {
        if (!this.isDebugEnabled) return;
        try {
            if (debugBuffer == null) {
                File debugLog = new File("debug_" + formatFilename.format(new Date()) + ".log");
                FileWriter debugWriter = new FileWriter(debugLog);
                debugBuffer = new BufferedWriter(debugWriter);
            }
            String time = "[" + formatItem.format(new Date()) + "] ";
            debugBuffer.append(time + object.getClass().getName() + ": " + message);
            debugBuffer.newLine();
            debugBuffer.flush();
            System.out.println(time + object.getClass().getName() + ": " + message);
        } catch (IOException e) {
        }
    }

    /**
	 * Creates a new URL based connection InputStream
	 * @return
	 */
    public InputStream getDownloadURLInputStream() throws Exception {
        URL url = new URL(tracker.getTrackingDownloadFile());
        URLConnection connection = url.openConnection();
        connection.setUseCaches(false);
        connection.setConnectTimeout(this.urlConnectionTimeout);
        return connection.getInputStream();
    }

    /**
	 * Creates a new File based connection OutputStream
	 * @return
	 */
    public OutputStream getTrackingFileOutputStream() throws Exception {
        tracker.setTrackingFileName(formatFilename.format(new Date()) + ".log");
        FileOutputStream trackingOutput = new FileOutputStream(new File(tracker.getFullTrackingFilePath()));
        logDebug(this, "getTrackingFileOutputStream(): tracking file: " + tracker.getFullTrackingFilePath());
        return trackingOutput;
    }

    /**
	 * Created a timestamp of the current time
	 */
    public long getTimestamp() {
        return System.currentTimeMillis();
    }
}
