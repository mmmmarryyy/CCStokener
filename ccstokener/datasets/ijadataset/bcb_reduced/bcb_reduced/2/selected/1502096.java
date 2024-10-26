package libretunes;

import java.awt.AWTException;
import java.awt.Image;
import java.awt.MenuItem;
import java.awt.PopupMenu;
import java.awt.SystemTray;
import java.awt.Toolkit;
import java.awt.TrayIcon;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.image.BufferedImage;
import java.io.BufferedReader;
import java.io.File;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.RandomAccessFile;
import java.net.HttpURLConnection;
import java.net.URL;
import java.net.URLEncoder;
import javax.swing.JFrame;
import javax.swing.JOptionPane;
import javax.swing.UIManager;

/**
 * Main class
 * @author Daniel Dreibrodt
 */
public class Main {

    public static final String VERSION = "1.2";

    private static final String updateURL_s = "http://libretunes.sourceforge.net/update.php";

    private static TrayIcon trayIcon;

    private static iTunesConnector connector;

    public static Image logo16 = getImage("logo16.png");

    public static Image logo16_grey = getImage("logo16_grey.png");

    public static Image last_fm_icon = getImage("last.fm.png");

    public static Image libre_fm_icon = getImage("libre.fm.png");

    /**
   * @param args the command line arguments
   */
    public static void main(String[] args) {
        try {
            UIManager.setLookAndFeel(UIManager.getSystemLookAndFeelClassName());
        } catch (Exception e) {
        }
        JFrame.setDefaultLookAndFeelDecorated(true);
        Config.load();
        Language.loadLanguageByCode(Config.get("language"));
        if (Boolean.parseBoolean(Config.get("autoupdate"))) update();
        showTrayIcon();
        if (Boolean.parseBoolean(Config.get("last.fm.enabled"))) {
            new AudioScrobbler(Config.get("last.fm.server"), Config.get("last.fm.user"), Config.get("last.fm.passHash"));
        }
        if (Boolean.parseBoolean(Config.get("libre.fm.enabled"))) {
            new AudioScrobbler(Config.get("libre.fm.server"), Config.get("libre.fm.user"), Config.get("libre.fm.passHash"));
        }
        if (Boolean.parseBoolean(Config.get("extra.enabled"))) {
            new AudioScrobbler(Config.get("extra.server"), Config.get("extra.user"), Config.get("extra.passHash"));
        }
        connector = new iTunesConnector();
        connector.start();
    }

    /**
   * Shows an error messsage
   * @param title The message title
   * @param message The message
   */
    public static void showErrorMessage(String title, String message) {
        JOptionPane.showMessageDialog(null, message, title, JOptionPane.ERROR_MESSAGE);
    }

    /**
   * Initializes and shows the program tray icon
   */
    public static void showTrayIcon() {
        if (SystemTray.isSupported()) {
            SystemTray tray = SystemTray.getSystemTray();
            if (trayIcon != null) return;
            trayIcon = new TrayIcon(logo16, "LibreTunes " + VERSION);
            MenuItem cfg = new MenuItem(Language.get("MENU_CONFIG"));
            cfg.addActionListener(new ActionListener() {

                public void actionPerformed(ActionEvent e) {
                    Config.showOptions();
                }
            });
            MenuItem quit = new MenuItem(Language.get("MENU_QUIT"));
            quit.addActionListener(new ActionListener() {

                public void actionPerformed(ActionEvent e) {
                    exit();
                }
            });
            trayIcon.setPopupMenu(new PopupMenu("LibreTunes " + VERSION));
            trayIcon.getPopupMenu().add(cfg);
            trayIcon.getPopupMenu().add(quit);
            trayIcon.addActionListener(new ActionListener() {

                public void actionPerformed(ActionEvent e) {
                    Config.showOptions();
                }
            });
            try {
                tray.add(trayIcon);
            } catch (AWTException e) {
                showErrorMessage("ERROR_TRAY_TITLE", "ERROR_TRAY_MSG");
            }
        }
    }

    /**
   * Changes the tooltip of the tray icon
   * @param t The new tooltip text
   */
    public static void setTrayIconText(String t) {
        trayIcon.setToolTip(t);
    }

    /**
   * Changes the tray icon image
   * @param imageFile The new icon image file
   */
    public static void setTrayIcon(String imageFile) {
        trayIcon.setImage(getImage(imageFile));
    }

    /**
   * Changes the tray icon image
   * @param imageFile The new icon image
   */
    public static void setTrayIcon(Image i) {
        trayIcon.setImage(i);
    }

    /**
   * Removes the program icon from the system tray
   */
    public static void hideTrayIcon() {
        if (SystemTray.isSupported()) {
            SystemTray tray = SystemTray.getSystemTray();
            tray.remove(trayIcon);
        }
        trayIcon = null;
    }

    /**
   * Gets an image from the program's resources
   * @param name
   * @return
   */
    public static Image getImage(String name) {
        try {
            URL url = Main.class.getResource(name);
            if (url != null) return Toolkit.getDefaultToolkit().createImage(url); else return new BufferedImage(16, 16, BufferedImage.TYPE_INT_ARGB);
        } catch (Exception ex) {
            ex.printStackTrace();
        }
        return new BufferedImage(16, 16, BufferedImage.TYPE_INT_ARGB);
    }

    /**
   * Quits the application
   */
    public static void exit() {
        Config.save();
        connector.doStop();
        AudioScrobbler.stopAll();
        System.exit(0);
    }

    /**
   * Checks for updates and downloads and installs them if chosen by user
   */
    private static void update() {
        if (VERSION.contains("dev")) return;
        try {
            URL updateURL = new URL(updateURL_s + "?v=" + URLEncoder.encode(VERSION, "UTF-8") + "&os=" + URLEncoder.encode(System.getProperty("os.name"), "UTF-8"));
            InputStream uis = updateURL.openStream();
            InputStreamReader uisr = new InputStreamReader(uis);
            BufferedReader ubr = new BufferedReader(uisr);
            String header = ubr.readLine();
            if (header.equals("LIBRETUNESUPDATEPAGE")) {
                String cver = ubr.readLine();
                String cdl = ubr.readLine();
                if (!cver.equals(VERSION)) {
                    System.out.println("Update available!");
                    int i = JOptionPane.showConfirmDialog(null, Language.get("UPDATE_MSG").replaceAll("%v", VERSION).replaceAll("%c", cver), Language.get("UPDATE_TITLE"), JOptionPane.YES_NO_OPTION);
                    if (i == 0) {
                        URL url = new URL(cdl);
                        HttpURLConnection connection = (HttpURLConnection) url.openConnection();
                        connection.connect();
                        if (connection.getResponseCode() / 100 != 2) {
                            throw new Exception("Server error! Response code: " + connection.getResponseCode());
                        }
                        int contentLength = connection.getContentLength();
                        if (contentLength < 1) {
                            throw new Exception("Invalid content length!");
                        }
                        int size = contentLength;
                        File tempfile = File.createTempFile("libretunes_update", ".zip");
                        tempfile.deleteOnExit();
                        RandomAccessFile file = new RandomAccessFile(tempfile, "rw");
                        InputStream stream = connection.getInputStream();
                        int downloaded = 0;
                        ProgressWindow pwin = new ProgressWindow(null, Language.get("DOWNLOAD_PROGRESS"));
                        pwin.setVisible(true);
                        pwin.setProgress(0);
                        pwin.setText(Language.get("CONNECT_PROGRESS"));
                        while (downloaded < size) {
                            byte buffer[];
                            if (size - downloaded > 1024) {
                                buffer = new byte[1024];
                            } else {
                                buffer = new byte[size - downloaded];
                            }
                            int read = stream.read(buffer);
                            if (read == -1) break;
                            file.write(buffer, 0, read);
                            downloaded += read;
                            pwin.setProgress(downloaded / size);
                        }
                        file.close();
                        System.out.println("Downloaded file to " + tempfile.getAbsolutePath());
                        pwin.setVisible(false);
                        pwin.dispose();
                        pwin = null;
                        Helper.unzip(tempfile);
                        JOptionPane.showMessageDialog(null, Language.get("UPDATE_SUCCESS_MSG"), Language.get("UPDATE_SUCCESS_TITLE"), JOptionPane.INFORMATION_MESSAGE);
                        Runtime.getRuntime().exec("LibreTunes.exe");
                        System.exit(0);
                    } else {
                    }
                }
                ubr.close();
                uisr.close();
                uis.close();
            } else {
                ubr.close();
                uisr.close();
                uis.close();
                throw new Exception("Update page had invalid header: " + header);
            }
        } catch (Exception ex) {
            ex.printStackTrace();
            JOptionPane.showMessageDialog(null, ex.toString(), Language.get("ERROR_UPDATE_TITLE"), JOptionPane.ERROR_MESSAGE);
        }
    }
}
