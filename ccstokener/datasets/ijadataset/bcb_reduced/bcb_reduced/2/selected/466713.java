package net.sf.cruisemonitor;

import java.awt.AWTException;
import java.awt.Image;
import java.awt.PopupMenu;
import java.awt.SystemTray;
import java.awt.Toolkit;
import java.awt.TrayIcon;
import java.io.IOException;
import java.net.MalformedURLException;
import java.net.URL;
import java.net.URLConnection;
import java.util.Properties;
import javax.swing.JDialog;
import javax.swing.JOptionPane;
import net.sf.cruisemonitor.utils.FileUtils;

public class Tray {

    private TrayIcon trayIcon = null;

    public CruiseConnector cruiseConnector;

    public static String imagePath = "icons/smile.gif";

    public String cruiseUrl;

    public int pingInterval;

    private String urlFileName;

    private static final int DEFAULT_PING_INTERVAL = 5;

    Properties properties;

    public Tray(String urlFile) {
        loadProperties();
        urlFileName = urlFile;
        pingInterval = DEFAULT_PING_INTERVAL;
        cruiseUrl = FileUtils.readUrlFromFile(urlFileName);
        if (cruiseUrl != null) {
            cruiseConnector = new CruiseConnector(cruiseUrl);
        }
    }

    public Tray() {
    }

    private void loadProperties() {
        properties = new Properties();
        try {
            properties.load(getClass().getClassLoader().getResourceAsStream("messages.properties"));
        } catch (IOException e) {
        }
    }

    private PopupMenu menu() {
        PopupMenu popup = new PopupMenu();
        popup.add(new UrlConfigureMenu().createUrlConfigMenuItem(this));
        popup.add(new PingTimeConfigureMenu().createPingTimeConfigMenuItem(this));
        popup.add(new StatusMenu().createStatusMenuItem(this));
        popup.add(new ExitMenu().createExitMenuItem());
        return popup;
    }

    public TrayIcon getTrayIcon() {
        return trayIcon;
    }

    public Properties getProperties() {
        return properties;
    }

    public void showTray() {
        SystemTray tray = SystemTray.getSystemTray();
        if (SystemTray.isSupported()) {
            Image image = createImage(imagePath);
            trayIcon = createTrayIcon(image, menu());
            try {
                tray.add(trayIcon);
            } catch (AWTException e) {
                JOptionPane.showMessageDialog(null, properties.getProperty("icon.not.added"));
            }
        }
    }

    JDialog createDialogBox() {
        JDialog parentComponent = new JDialog();
        parentComponent.setDefaultCloseOperation(JDialog.DO_NOTHING_ON_CLOSE);
        return parentComponent;
    }

    void connect() {
        cruiseConnector = new CruiseConnector(cruiseUrl);
        if (cruiseConnector.connect()) {
            JOptionPane.showMessageDialog(new JDialog(), "Connected to " + cruiseUrl);
            if (!cruiseConnector.reader()) showBuildFailedMessage();
        }
    }

    void urlFromUser() {
        cruiseUrl = JOptionPane.showInputDialog(createDialogBox(), properties.getProperty("cruise.url"), "Cruise URL", 1);
        if (cruiseUrl == null) return;
        if (!cruiseUrl.isEmpty() && isValidUrl()) {
            FileUtils.writeUrlToFile(urlFileName, cruiseUrl);
        } else {
            JOptionPane.showMessageDialog(null, properties.getProperty("invalid.url"), "Error", 2);
            urlFromUser();
        }
    }

    private boolean isValidUrl() {
        URL url;
        try {
            url = new URL(cruiseUrl);
            if (url != null) {
                URLConnection connection = url.openConnection();
                connection.connect();
                return true;
            }
        } catch (MalformedURLException e) {
            e.printStackTrace();
            return false;
        } catch (IOException e) {
            e.printStackTrace();
            return false;
        }
        return false;
    }

    private void urlFromFile() {
        cruiseUrl = FileUtils.readUrlFromFile(urlFileName);
    }

    private Image createImage(String path) {
        return Toolkit.getDefaultToolkit().getImage(getClass().getClassLoader().getResource(path));
    }

    private TrayIcon createTrayIcon(Image image, PopupMenu popup) {
        TrayIcon trayIcon = new TrayIcon(image, "Cruise Control", popup);
        trayIcon.setImageAutoSize(true);
        trayIcon.setToolTip(properties.getProperty("title"));
        return trayIcon;
    }

    public void getCruiseUrl() {
        if (cruiseUrl == null) urlFromUser(); else urlFromFile();
    }

    void showBuildFailedMessage() {
        imagePath = "icons/cry.gif";
        trayIcon.setImage(createImage(imagePath));
        trayIcon.displayMessage(properties.getProperty("title"), properties.getProperty("build.failed"), TrayIcon.MessageType.ERROR);
    }

    void showBuildPassedMessage() {
        imagePath = "icons/smile.gif";
        trayIcon.setImage(createImage(imagePath));
        trayIcon.displayMessage(properties.getProperty("title"), properties.getProperty("build.passed"), TrayIcon.MessageType.INFO);
    }

    private void sleep() throws InterruptedException {
        Thread.sleep(pingInterval * 1000);
    }

    private void checkCruiseStatus() throws InterruptedException {
        boolean showFailure = true;
        sleep();
        while (true) {
            if (showFailure && !cruiseConnector.reader()) {
                showBuildFailedMessage();
                trayIcon.setToolTip(properties.getProperty("build.failed"));
                showFailure = false;
            }
            while (true) {
                if (cruiseConnector.reader()) {
                    trayIcon.setToolTip(properties.getProperty("build.passed"));
                    showFailure = true;
                    break;
                }
            }
            sleep();
        }
    }

    public void start() throws InterruptedException {
        showTray();
        getCruiseUrl();
        cruiseConnector = new CruiseConnector(cruiseUrl);
        trayIcon.setToolTip(properties.getProperty("build.passed"));
        if (isValidUrl()) checkCruiseStatus(); else JOptionPane.showMessageDialog(null, properties.getProperty("configure.url"), "Error", 2);
    }
}
