package com.xohm.cm.gui;

import java.awt.*;
import java.io.*;
import java.net.*;
import java.util.Date;
import java.util.Vector;
import java.util.logging.Level;
import java.util.logging.Logger;
import javax.swing.*;
import org.lobobrowser.html.FormInput;
import org.lobobrowser.html.UserAgentContext;
import org.lobobrowser.html.gui.*;
import org.lobobrowser.html.test.*;
import com.xohm.base.Driver;
import com.xohm.base.logging.XohmLogger;
import com.xohm.base.structs.ConnectionStatistics;
import com.xohm.base.structs.LinkStatusInfo;
import com.xohm.multi.session.MultiController;
import com.xohm.platform.api.DriverInformation;
import com.xohm.platform.api.OSAPIFactory;
import com.xohm.session.StateChangedCallback;
import com.xohm.session.StateChangedListener;
import com.xohm.upgrades.autoupdate.*;
import com.xohm.utils.*;
import java.awt.Color;

/**
 * <B>This class handles the GUI for the Open source WiMAX connection manager.
 * The connection manager gui is comprised of three major sections<br>
 * <ul>
 * <li>Reskinnable Header:<br>
 * 		The header is a Lobobrowser component and loads any html file/url
 * specified in configuration file of connection manager.	
 * <li>Connection Panel:<br>
 * 		The connection panel is consists of signal bars and the connect/disconnect button.
 * It also shows the status messages and few other values related to connection.
 * <li>Details Panel:<br>
 * 		The details panel is a tabbed panel and contains several tabs.
 * 	Each tab adds/provides specific functionality to connection manager. 
 * </ul>
 * <img src="../../../../resources/xohm_cm.jpg"><br><br>
 * </B><br><br>
 *
 * <font size=-1>Open source WiMAX connection manager<br>
 * � Copyright Sprint Nextel Corp. 2008</font><br><br>
 *
 * @author Sachin Kumar 
 */
public class CMJFrame extends JFrame implements StateChangedListener {

    private static final long serialVersionUID = 1L;

    private HtmlPanel htmlHeaderPanel;

    private JButton signal;

    private JLabel status;

    private JLabel timeElasped;

    private JLabel bytesUploaded;

    private JLabel bytesDownloaded;

    private JButton connectButton;

    private JProgressBar progressBar;

    private JButton detailsButton;

    private TabbedPanels detailsPanel;

    private MultiController multiController = null;

    private boolean detailsOpen = false;

    private boolean headerLoaded = false;

    private boolean connected = false;

    private boolean manualDisconnect = false;

    private boolean isHttpUrl = false;

    private boolean launchedOnce = false;

    private boolean checkedOnce = false;

    private boolean updateCheckDone = false;

    private Thread animationThread = null;

    private Thread timerThread = null;

    private Date startTime = null;

    private long uploadOffset = 0;

    private long downloadOffset = 0;

    private long bytesSent = 0;

    private long bytesReceived = 0;

    private NetworkList networkList;

    private ImageIcon upArrow;

    private ImageIcon downArrow;

    private int prevSignalStrength = 0;

    private ImageIcon noBar;

    private ImageIcon oneBar;

    private ImageIcon twoBar;

    private ImageIcon threeBar;

    private ImageIcon oneBarNC;

    private ImageIcon twoBarNC;

    private ImageIcon threeBarNC;

    private boolean animate = false;

    private ImageIcon connectIcon;

    private ImageIcon connectingIcon;

    private ImageIcon disconnectIcon;

    /**
	 * Default Constructor<br><br>
	 */
    public CMJFrame() {
        super();
        initialize();
    }

    /**
	 * This method initiates the session for wimax cards.
	 */
    public void start() {
        UpdateManager.getInstance().addProgressListener(Properties.CMSoftwareName, new UpdateProgress());
        if (Properties.enableAutoUpdates) checkForUpdates();
        OSAPIFactory.getConnectionManager().setMaxSizeForNativeTypes();
        readByteTransferredFromFile();
        initSession();
    }

    /**
	 * This method constructs the ui for the connection manager.
	 *
	 */
    private void initialize() {
        try {
            Logger.getLogger("org.lobobrowser").setLevel(Level.SEVERE);
            setDefaultCloseOperation(WindowConstants.EXIT_ON_CLOSE);
            this.addWindowListener(new java.awt.event.WindowAdapter() {

                public void windowClosing(java.awt.event.WindowEvent e) {
                    saveByteTransferredToFile();
                    status.setText("Shutdown in progress...");
                    if (multiController != null) {
                        int waitTime = multiController.isConnected() ? 4000 : 1500;
                        multiController.shutdown();
                        try {
                            Thread.sleep(waitTime);
                        } catch (Exception ex) {
                        }
                    }
                    Runtime.getRuntime().exit(0);
                }
            });
            loadImages();
            JDesktopPane jDesktopPane1 = new JDesktopPane();
            getContentPane().add(jDesktopPane1, BorderLayout.CENTER);
            jDesktopPane1.setLayout((new BoxLayout(jDesktopPane1, BoxLayout.PAGE_AXIS)));
            jDesktopPane1.setBackground(Color.decode(Properties.backgroundColor));
            htmlHeaderPanel = new HtmlPanel();
            htmlHeaderPanel.setBackground(Color.decode(Properties.backgroundColor));
            JScrollPane scrollPane1 = new JScrollPane();
            scrollPane1.setPreferredSize(new Dimension(520, 104));
            scrollPane1.setHorizontalScrollBarPolicy(JScrollPane.HORIZONTAL_SCROLLBAR_NEVER);
            scrollPane1.setVerticalScrollBarPolicy(JScrollPane.VERTICAL_SCROLLBAR_NEVER);
            scrollPane1.setViewportView(htmlHeaderPanel);
            scrollPane1.setBorder(BorderFactory.createEmptyBorder());
            jDesktopPane1.add(scrollPane1);
            loadHtmlHeader();
            ConnectPanel jPanel1 = new ConnectPanel();
            jDesktopPane1.add(jPanel1);
            jPanel1.setPreferredSize(new Dimension(520, 62));
            jPanel1.setLayout(null);
            signal = new JButton();
            jPanel1.add(signal);
            signal.setBounds(12, 7, 48, 48);
            signal.setBorder(BorderFactory.createEmptyBorder());
            signal.setIcon(noBar);
            signal.setContentAreaFilled(false);
            connectButton = new JButton();
            jPanel1.add(connectButton);
            connectButton.setBounds(455, 7, 48, 48);
            connectButton.setBorder(BorderFactory.createEmptyBorder());
            connectButton.setIcon(connectIcon);
            connectButton.setContentAreaFilled(false);
            connectButton.addActionListener(new java.awt.event.ActionListener() {

                public void actionPerformed(java.awt.event.ActionEvent e) {
                    if (connected) {
                        disconnect();
                    } else {
                        connect();
                    }
                }
            });
            status = new JLabel();
            jPanel1.add(status);
            status.setBounds(75, 10, 350, 14);
            JLabel jLabel1 = new JLabel();
            jPanel1.add(jLabel1);
            jLabel1.setText("Time");
            jLabel1.setToolTipText("Time elasped since connected");
            jLabel1.setBounds(85, 30, 58, 14);
            timeElasped = new JLabel();
            jPanel1.add(timeElasped);
            timeElasped.setBounds(75, 42, 85, 14);
            JLabel jLabel2 = new JLabel();
            jPanel1.add(jLabel2);
            jLabel2.setText("Uploaded");
            jLabel2.setToolTipText("Total Bytes Uploaded");
            jLabel2.setBounds(185, 30, 72, 14);
            bytesUploaded = new JLabel();
            jPanel1.add(bytesUploaded);
            bytesUploaded.setBounds(200, 42, 90, 14);
            JLabel jLabel3 = new JLabel();
            jPanel1.add(jLabel3);
            jLabel3.setText("Downloaded");
            jLabel3.setToolTipText("Total Bytes Downloaded");
            jLabel3.setBounds(312, 30, 92, 14);
            bytesDownloaded = new JLabel();
            jPanel1.add(bytesDownloaded);
            bytesDownloaded.setBounds(327, 42, 90, 14);
            JPanel jPanel2 = new JPanel();
            jDesktopPane1.add(jPanel2);
            jPanel2.setPreferredSize(new Dimension(520, 20));
            jPanel2.setLayout(null);
            detailsButton = new JButton();
            jPanel2.add(detailsButton);
            detailsButton.setBounds(0, 0, 520, 20);
            detailsButton.setIcon(downArrow);
            detailsButton.addActionListener(new java.awt.event.ActionListener() {

                public void actionPerformed(java.awt.event.ActionEvent e) {
                    if (detailsOpen) {
                        hideDetails();
                    } else {
                        showDetails();
                    }
                }
            });
            detailsButton.addMouseListener(new java.awt.event.MouseAdapter() {

                public void mouseEntered(java.awt.event.MouseEvent evt) {
                    detailsButton.setOpaque(true);
                    progressBar.setOpaque(false);
                }

                public void mouseExited(java.awt.event.MouseEvent evt) {
                    if (progressBar.getValue() > 0) {
                        detailsButton.setOpaque(false);
                        progressBar.setOpaque(true);
                    } else {
                        detailsButton.setOpaque(true);
                        progressBar.setOpaque(false);
                    }
                }
            });
            progressBar = new JProgressBar(0, 100);
            jPanel2.add(progressBar);
            progressBar.setBounds(0, 0, 520, 20);
            progressBar.setStringPainted(true);
            detailsPanel = new TabbedPanels();
            detailsPanel.setPreferredSize(new Dimension(520, 216));
            this.setSize(520, 216);
            this.setResizable(false);
            this.setTitle(Properties.frameTitle);
            this.setVisible(true);
            this.setLocationRelativeTo(null);
            this.pack();
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    /**
	 * This method loads all the images used by the connection manager.
	 *
	 */
    private void loadImages() {
        upArrow = new ImageIcon(getClass().getResource("/up_arrow.png"));
        downArrow = new ImageIcon(getClass().getResource("/down_arrow.png"));
        oneBarNC = new ImageIcon(getClass().getResource("/signal_strength_nc_1.png"));
        twoBarNC = new ImageIcon(getClass().getResource("/signal_strength_nc_2.png"));
        threeBarNC = new ImageIcon(getClass().getResource("/signal_strength_nc_3.png"));
        noBar = new ImageIcon(getClass().getResource("/WIMAX_012-blank.png"));
        oneBar = new ImageIcon(getClass().getResource("/WIMAX_015-average_Green.png"));
        twoBar = new ImageIcon(getClass().getResource("/WIMAX_016-good_Green.png"));
        threeBar = new ImageIcon(getClass().getResource("/WIMAX_017-excellent_Green.png"));
        connectIcon = new ImageIcon(getClass().getResource("/connect_button.png"));
        connectingIcon = new ImageIcon(getClass().getResource("/connecting_button.gif"));
        disconnectIcon = new ImageIcon(getClass().getResource("/disconnect_button.gif"));
    }

    /**
	 * This method is used to load the html header component. It reads the skin location 
	 * from the configuration file and loads the html page into the header.
	 *
	 */
    private void loadHtmlHeader() {
        String skinUrl = getClass().getResource("/" + Properties.defaultSkinFileName).toString();
        if (Properties.headerSkin != null && !Properties.headerSkin.equals("")) {
            try {
                URL url = new URL(Properties.headerSkin);
                if (url.getProtocol().equalsIgnoreCase("http")) {
                    isHttpUrl = true;
                    HttpURLConnection.setFollowRedirects(false);
                    HttpURLConnection httpConn = (HttpURLConnection) url.openConnection();
                    httpConn.setRequestMethod("HEAD");
                    boolean urlExists = (httpConn.getResponseCode() == HttpURLConnection.HTTP_OK);
                    if (urlExists) skinUrl = Properties.headerSkin;
                } else if (url.getProtocol().equalsIgnoreCase("jar")) {
                    String jarFile = Properties.headerSkin.substring(9).split("!")[0];
                    File skinFile = new File(jarFile);
                    if (skinFile.exists() && skinFile.canRead()) skinUrl = Properties.headerSkin;
                } else if (url.getProtocol().equalsIgnoreCase("file")) {
                    File skinFile = new File(Properties.headerSkin.substring(5));
                    if (skinFile.exists() && skinFile.canRead()) skinUrl = Properties.headerSkin;
                } else {
                    File skinFile = new File(Properties.headerSkin);
                    if (skinFile.exists() && skinFile.canRead()) skinUrl = Properties.headerSkin;
                }
            } catch (Exception ex) {
                XohmLogger.debugPrintln("Header skin url not valid. " + ex.getMessage());
                XohmLogger.debugPrintln("Loading the default skin.");
                ex.printStackTrace();
            }
        }
        XohmLogger.debugPrintln("Header skin file = " + skinUrl);
        try {
            LocalHtmlRendererContext rendererContext = new LocalHtmlRendererContext(htmlHeaderPanel, new SimpleUserAgentContext());
            rendererContext.navigate(skinUrl);
            headerLoaded = true;
        } catch (IOException urlEx) {
            XohmLogger.debugPrintln("Exception occured while loading the skin. " + urlEx.getMessage());
        }
    }

    /**
	 * Method to show the details panel.
	 *
	 */
    private void showDetails() {
        this.getContentPane().add(detailsPanel, BorderLayout.SOUTH);
        this.setSize(520, 440);
        detailsButton.setIcon(upArrow);
        detailsOpen = true;
        detailsPanel.setVisible(true);
        this.pack();
    }

    /**
	 * Method to hide the details panel.
	 *
	 */
    private void hideDetails() {
        this.setSize(520, 216);
        this.getContentPane().remove(detailsPanel);
        detailsButton.setIcon(downArrow);
        detailsOpen = false;
        detailsPanel.setVisible(false);
        this.pack();
    }

    /**
	 * This method initializes the multicontroller and creates the 
	 * sessions for all the installed drivers.
	 *
	 */
    private void initSession() {
        boolean successful = false;
        multiController = new MultiController();
        multiController.addListener(this);
        multiController.addListener(detailsPanel);
        Vector<DriverInformation> drivers = OSAPIFactory.getConnectionManager().getAvailableDriverInformation();
        for (int i = 0; i < drivers.size(); i++) {
            if (multiController.addSession(drivers.elementAt(i).getDriverLocation(), drivers.elementAt(i).getDriverName())) {
                successful = true;
            } else {
                XohmLogger.debugPrintln("Binding failed for the driver: " + drivers.elementAt(i).getDriverLocation() + drivers.elementAt(i).getDriverName());
            }
        }
        if (successful) {
            multiController.goReady();
            try {
                Thread.sleep(1000);
            } catch (Exception e) {
            }
            multiController.initialize();
            goReady();
        } else {
            status.setText("No drivers for WiMAX card found");
            connectButton.setEnabled(false);
        }
    }

    /**
	 * This method is used to request multicontroller to go to Ready state
	 * if manually disconnected earlier. It also enables/disables the 
	 * connect button depending on the card availability.
	 *
	 */
    private void goReady() {
        if (manualDisconnect) {
            multiController.goReady();
            try {
                Thread.sleep(1000);
            } catch (Exception e) {
            }
        }
        connected = false;
        prevSignalStrength = 0;
        connectButton.setIcon(connectIcon);
        bytesDownloaded.setText("");
        bytesUploaded.setText("");
        detailsPanel.setBytesTransferredOffsets(downloadOffset, uploadOffset);
        boolean hasCard = multiController.hasCardConnected();
        if (manualDisconnect || hasCard) {
            connectButton.setEnabled(true);
            status.setText("Ready");
            if (manualDisconnect && hasCard) multiController.initialize();
            if (Properties.enableAutoConnect && !manualDisconnect) connect();
        } else {
            connectButton.setEnabled(false);
            status.setText("Please Insert WiMAX Card");
            OSAPIFactory.getConnectionManager().addDeviceInsertListener(this, "deviceInsertCallback");
        }
    }

    /**
	 * Callback method for device insert notification.
	 * Enables the connect button and connects if auto-connect enabled.
	 *
	 */
    public void deviceInsertCallback() {
        connectButton.setEnabled(true);
        connectButton.setIcon(connectIcon);
        status.setText("Ready");
        if (manualDisconnect) multiController.initialize();
        if (Properties.enableAutoConnect) connect();
    }

    /**
	 * This method is used to request multicontroller to go to Connect state.
	 * 
	 */
    private void connect() {
        if (multiController.hasCardConnected()) {
            multiController.connect();
            animate = true;
            connected = true;
            manualDisconnect = false;
            status.setText("Connecting...");
            connectButton.setIcon(connectingIcon);
            if (animationThread == null || !animationThread.isAlive()) {
                animationThread = new Thread(connectAnimation);
                animationThread.start();
            }
        } else {
            goReady();
        }
    }

    /**
	 * This method is used to request multicontroller to go to Disconnect state.
	 * 
	 */
    private void disconnect() {
        status.setText("Disconnecting...");
        connectButton.setIcon(connectIcon);
        multiController.disconnect();
        animate = false;
        connected = false;
        launchedOnce = false;
        manualDisconnect = true;
        connectButton.setEnabled(false);
        startTime = null;
        timerThread = null;
        uploadOffset = bytesSent;
        downloadOffset = bytesReceived;
    }

    /**
	 * This method is used to check for new updates.
	 *
	 */
    private void checkForUpdates() {
        try {
            UpdateManager mgr = UpdateManager.getInstance();
            int numUpdates = mgr.checkForUpdates(Properties.CMSoftwareVendor, Properties.CMSoftwareName, Properties.CMVersion);
            if (numUpdates > 0) {
                int value = JOptionPane.showConfirmDialog(this, "A new version for XOHM Connection Manager is available. Do you want to install it now?", "Confirmation", JOptionPane.YES_NO_OPTION);
                if (value == JOptionPane.YES_OPTION) {
                    mgr.installNewRelease(Properties.CMSoftwareName, true);
                    detailsButton.setOpaque(false);
                    progressBar.setOpaque(true);
                } else {
                    updateCheckDone = true;
                }
            }
        } catch (UpdateException upEx) {
            XohmLogger.debugPrintln("Exception occured while checking for software updates. " + upEx.getMessage());
        }
    }

    /**
	 * This method is used to format the time elapsed and then
	 * display the formatted time on gui.
	 * 
	 * @param timeElapsed long - time elasped in milliseconds.
	 */
    private void timeElapsedSinceConnected(long timeElapsed) {
        String TIME_DELIMITER = ":";
        long elapsedTimeSec = timeElapsed / 1000;
        long elapsedTimeMin = timeElapsed / 1000 / 60;
        long elapsedTimeHour = timeElapsed / 1000 / 60 / 60;
        String seconds = convertToString(elapsedTimeSec - (elapsedTimeMin * 60));
        String mins = convertToString(elapsedTimeMin - (elapsedTimeHour * 60));
        String hours = convertToString(elapsedTimeHour);
        String time = hours + TIME_DELIMITER + mins + TIME_DELIMITER + seconds;
        timeElasped.setText(time);
    }

    /**
	 * Utility method to convert long value to string and 
	 * adds 0 in front in case of single digit. 
	 * @param value long - time
	 * @return String - converted string
	 */
    private String convertToString(long value) {
        String sValue = String.valueOf(value);
        if (sValue.length() == 1) sValue = "0" + sValue;
        return sValue;
    }

    /**
	 * This method save the byte transferred values to the file.
	 *
	 */
    private void saveByteTransferredToFile() {
        FileOutputStream fileOut = null;
        ObjectOutputStream objOut = null;
        uploadOffset = bytesSent;
        downloadOffset = bytesReceived;
        try {
            File offsetFile = new File(Properties.offsetFileName);
            if (!offsetFile.exists()) {
                offsetFile.createNewFile();
            }
            String offsets = downloadOffset + ":" + uploadOffset;
            String filePath = offsetFile.getPath();
            fileOut = new FileOutputStream(filePath);
            objOut = new ObjectOutputStream(fileOut);
            objOut.writeObject(offsets);
            fileOut.close();
            objOut.close();
        } catch (Exception ex) {
            try {
                if (fileOut != null) fileOut.close();
                if (objOut != null) objOut.close();
            } catch (Exception e) {
            }
        }
    }

    /**
	 * This method reads the bytes transferred values from file
	 *
	 */
    private void readByteTransferredFromFile() {
        FileInputStream fileIn = null;
        ObjectInputStream objIn = null;
        try {
            File offsetFile = new File(Properties.offsetFileName);
            if (offsetFile.exists()) {
                String filePath = offsetFile.getPath();
                fileIn = new FileInputStream(filePath);
                objIn = new ObjectInputStream(fileIn);
                String offsets = (String) objIn.readObject();
                if (offsets != null) {
                    String[] offsetsArr = offsets.split(":");
                    downloadOffset = Long.parseLong(offsetsArr[0]);
                    uploadOffset = Long.parseLong(offsetsArr[1]);
                }
                fileIn.close();
                objIn.close();
            }
        } catch (Exception ex) {
            ex.printStackTrace();
            try {
                if (fileIn != null) fileIn.close();
                if (objIn != null) objIn.close();
            } catch (Exception e) {
            }
        }
    }

    /**
	 * This method shows the signal strength when not connected
	 */
    private void showSignalStrength(Driver driver) {
        if (driver != null && driver.getNSPInfo() != null && driver.getNSPInfo().length > 0) {
            CMUtilInterface cmUtil = CMUtils.getInstance();
            LinkStatusInfo linkStatusInfo = new LinkStatusInfo();
            linkStatusInfo.setRssi(driver.getNSPInfo()[0].getRssiByte());
            int signalStrength = cmUtil.getSignalStrength(linkStatusInfo);
            switch(signalStrength) {
                case 3:
                    signal.setIcon(threeBarNC);
                    break;
                case 2:
                    signal.setIcon(twoBarNC);
                    break;
                case 1:
                    signal.setIcon(oneBarNC);
                    break;
                case 0:
                    signal.setIcon(noBar);
                    break;
            }
        }
    }

    /**
	 * Callback method for the notifications from the multicontroller.
	 * @see com.xohm.session.StateChangedListener#stateChanged(com.xohm.session.StateChangedCallback)
	 * 
	 * This method manages the gui state based on the notification.
	 * 
	 * @param notification com.xohm.session.StateChangedCallback
	 */
    public void stateChanged(StateChangedCallback notification) {
        ConnectionStatistics statistics = null;
        LinkStatusInfo linkStatus = null;
        Driver driver = notification.getDriver();
        if (driver != null) {
            if (driver.getConnectionStatistics() != null && driver.getConnectionStatistics().length > 0) statistics = driver.getConnectionStatistics()[0];
            if (driver.getLinkStatusInfo() != null) linkStatus = driver.getLinkStatusInfo();
        }
        XohmLogger.println(notification.getMessage(), linkStatus, statistics);
        String mesg = notification.getMessage();
        status.setText(mesg);
        switch(notification.getMessageType()) {
            case StateChangedCallback.CardRemoved:
                status.setText("Please Insert WiMAX Card");
                connectButton.setEnabled(false);
                break;
            case StateChangedCallback.Initialized:
                showSignalStrength(driver);
                if (networkList == null) networkList = new NetworkList(driver); else networkList.stop();
                new Thread(networkList).start();
                status.setText("Ready");
                break;
            case StateChangedCallback.Connecting:
                connected = true;
                connectButton.setIcon(connectingIcon);
                if (animationThread == null || !animationThread.isAlive()) {
                    animationThread = new Thread(connectAnimation);
                    animationThread.start();
                }
                break;
            case StateChangedCallback.Connected:
                animate = false;
                animationThread = null;
                connected = true;
                connectButton.setEnabled(true);
                status.setText("Connected.");
                connectButton.setIcon(disconnectIcon);
                if (linkStatus != null) {
                    prevSignalStrength = 0;
                    linkStatusInfo(linkStatus);
                }
                if (timerThread == null || !timerThread.isAlive()) {
                    startTime = new Date();
                    timerThread = new Thread(timer);
                    timerThread.start();
                }
                if (!launchedOnce) {
                    launchedOnce = true;
                    if (Properties.enableAutoLaunch) {
                        OSAPIFactory.getConnectionManager().openUrl(Properties.autoLaunchPage);
                    }
                }
                if (!checkedOnce) {
                    checkedOnce = true;
                    if (isHttpUrl) {
                        headerLoaded = false;
                        loadHtmlHeader();
                        headerLoaded = true;
                    }
                    if (Properties.enableAutoUpdates && !updateCheckDone) {
                        checkForUpdates();
                    }
                }
                break;
            case StateChangedCallback.Disconnected:
                animate = false;
                animationThread = null;
                startTime = null;
                timerThread = null;
                uploadOffset = bytesSent;
                downloadOffset = bytesReceived;
                goReady();
                break;
        }
        XohmLogger.debugPrintln("Notification Received: " + mesg);
    }

    /**
	 * Callback method to get registration notification from the multicontroller. 
	 * Receives whether card is registered or not.
	 * @see com.xohm.session.StateChangedListener#registered(java.lang.Boolean)
	 * 
	 * @param registered java.lang.Boolean
	 */
    public void registered(Boolean registered) {
        if (!registered) {
            String statusText = status.getText().split(" -")[0];
            status.setText(statusText + " - Card is not Registered");
        }
    }

    /**
	 * Callback method to get time elapsed from the multicontroller. 
	 * Receives the time elapsed since connected.
	 * @see com.xohm.session.StateChangedListener#timeElapsed(long)
	 * 
	 * @param timeElapsed long
	 */
    public void timeElapsed(long timeElapsed) {
    }

    /**
	 * Callback method to get bytes transferred from the multicontroller. 
	 * Receives the uploaded/downloaded bytes in current session.
	 * @see com.xohm.session.StateChangedListener#bytesTransfered(long, long, long)
	 * 
	 * @param totalBytes long
	 * @param totalBytesReceived long
	 * @param totalBytesTransmitted long
	 */
    public void bytesTransfered(long totalBytes, long totalBytesReceived, long totalBytesTransmitted) {
        long download = 0;
        long upload = 0;
        String unit = "Mb";
        if (downloadOffset >= totalBytesReceived || uploadOffset >= totalBytesTransmitted) {
            downloadOffset = uploadOffset = 0;
        }
        long totalOffset = downloadOffset + uploadOffset;
        if ((totalBytes - totalOffset) > 1048576) {
            download = (totalBytesReceived - downloadOffset) / 1048576;
            upload = (totalBytesTransmitted - uploadOffset) / 1048576;
            unit = "Mb";
        } else {
            download = (totalBytesReceived - downloadOffset) / 1024;
            upload = (totalBytesTransmitted - uploadOffset) / 1024;
            unit = "Kb";
        }
        bytesDownloaded.setText(download + " " + unit);
        bytesUploaded.setText(upload + " " + unit);
        bytesSent = totalBytesTransmitted;
        bytesReceived = totalBytesReceived;
    }

    /**
	 * Callback method to get LinkStatusInfo from the multicontroller. 
	 * Receives the link Status info for the current session.
	 * @see com.xohm.session.StateChangedListener#linkStatusInfo(com.xohm.base.structs.LinkStatusInfo)
	 * 
	 * Used to display the bars based on the signal strength.
	 * 
	 * @param linkStatusInfo com.xohm.base.structs.LinkStatusInfo
	 */
    public void linkStatusInfo(LinkStatusInfo linkStatusInfo) {
        CMUtilInterface cmUtil = CMUtils.getInstance();
        int signalStrength = cmUtil.getSignalStrength(linkStatusInfo);
        if (prevSignalStrength != signalStrength) {
            switch(signalStrength) {
                case 3:
                    signal.setIcon(threeBar);
                    break;
                case 2:
                    signal.setIcon(twoBar);
                    break;
                case 1:
                    signal.setIcon(oneBar);
                    break;
                case 0:
                    signal.setIcon(noBar);
                    break;
            }
            prevSignalStrength = signalStrength;
        }
    }

    /**
	 * Thread to compute the time elaspsed since connected.
	 */
    private Runnable timer = new Runnable() {

        public void run() {
            if (startTime == null) startTime = new Date();
            while (startTime != null) {
                long time = new Date().getTime() - startTime.getTime();
                timeElapsedSinceConnected(time);
                detailsPanel.timeElapsedSinceConnected(time);
                try {
                    Thread.sleep(1000);
                } catch (Exception e) {
                }
            }
            timeElapsedSinceConnected(0);
            detailsPanel.timeElapsedSinceConnected(0);
        }
    };

    /**
	 * Thread to display the animation while connecting.
	 */
    private Runnable connectAnimation = new Runnable() {

        public void run() {
            ImageIcon[] icons = { noBar, oneBarNC, twoBarNC, threeBarNC, threeBar };
            while (animate) {
                for (int i = 0; i < icons.length; i++) {
                    signal.setIcon(icons[i]);
                    try {
                        Thread.sleep(200);
                    } catch (Exception e) {
                    }
                }
            }
        }
    };

    /**
	 * A local class to get Network List
	 *
	 */
    private class NetworkList implements Runnable {

        private Driver driver;

        private boolean running = false;

        public NetworkList(Driver driver) {
            this.driver = driver;
        }

        public void run() {
            running = true;
            while (running && !connected) {
                driver.getConnection().getNetworkList(driver);
                showSignalStrength(driver);
                try {
                    Thread.sleep(5000);
                } catch (Exception e) {
                }
            }
        }

        public void stop() {
            running = false;
        }
    }

    /**
	 * A local class for html header renderer context. 
	 * Used to open links in html header in the default browser.
	 *
	 */
    private class LocalHtmlRendererContext extends SimpleHtmlRendererContext {

        public LocalHtmlRendererContext(HtmlPanel contextComponent, UserAgentContext ucontext) {
            super(contextComponent, ucontext);
        }

        public void submitForm(final String method, final java.net.URL action, final String target, final String enctype, final FormInput[] formInputs) {
            if (!headerLoaded) {
                super.submitForm(method, action, target, enctype, formInputs);
            } else {
                OSAPIFactory.getConnectionManager().openUrl(action.toString());
            }
        }
    }

    /**
	 * A local class for update progress notifications.
	 *
	 */
    private class UpdateProgress extends UpdateProgressListenerAdapter {

        /**
		 * Callback method for the download started notification from the 
		 * Update Manager.
		 * @see com.xohm.upgrades.autoupdate.UpdateProgressListener
		 */
        public void downloadStarted(String softwareName) {
            detailsButton.setOpaque(false);
            progressBar.setOpaque(true);
            progressBar.setValue(0);
            progressBar.setString("Download in progress...");
        }

        /**
		 * Callback method for the download complete notification from the 
		 * Update Manager.
		 * @see com.xohm.upgrades.autoupdate.UpdateProgressListener
		 * 
		 * @param success boolean - true if download is successful.
		 * @param softwareName String - name of software downloaded.
		 * @param filename String - name of downloaded file.
		 */
        public void downloadComplete(boolean success, String softwareName, String filename) {
            if (success) progressBar.setString("Download Completed."); else progressBar.setString("Download Failed.");
        }

        /**
		 * Callback method for the download progress notifications from the 
		 * Update Manager.
		 * @see com.xohm.upgrades.autoupdate.UpdateProgressListener
		 * 
		 * @param softwareName String - software name.
		 * @param bytesDownloaded long - number of bytes downloaded.
		 * @param totalBytes long - total number of bytes.
		 */
        public void downloadProgressNotification(String softwareName, long bytesDownloaded, long totalBytes) {
            int downloadPercentage = Math.round(((float) bytesDownloaded / (float) totalBytes) * 100);
            progressBar.setValue(downloadPercentage);
            progressBar.setString("Download Progress: " + downloadPercentage + "%");
        }

        /**
		 * Callback method for the installation started notification from the 
		 * Update Manager.
		 * @see com.xohm.upgrades.autoupdate.UpdateProgressListener
		 */
        public void installationStarted(boolean success, String softwareName) {
            detailsButton.setOpaque(false);
            progressBar.setOpaque(true);
            if (success) progressBar.setString("Installation Started."); else progressBar.setString("Installation Failed.");
            progressBar.setValue(0);
        }
    }
}
