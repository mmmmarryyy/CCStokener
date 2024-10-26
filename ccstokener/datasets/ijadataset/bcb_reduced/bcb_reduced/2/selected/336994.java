package net.mjrz.fm.ui.utils.notifications;

import static net.mjrz.fm.utils.Messages.tr;
import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Desktop;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.URI;
import java.net.URL;
import javax.swing.Box;
import javax.swing.BoxLayout;
import javax.swing.JButton;
import javax.swing.JCheckBox;
import javax.swing.JComponent;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.SwingWorker;
import javax.swing.border.LineBorder;
import net.mjrz.fm.ui.FinanceManagerUI;
import net.mjrz.fm.ui.utils.NotificationHandler;
import net.mjrz.fm.ui.utils.TimerButton;
import net.mjrz.fm.ui.utils.UIDefaults;
import net.mjrz.fm.ui.utils.notifications.types.UINotification;
import net.mjrz.fm.utils.MiscUtils;
import org.apache.log4j.Logger;

public class UpdateCheckNotificationFrame extends JFrame implements NotificationDisplay {

    private static final long serialVersionUID = 1L;

    private JLabel message = null;

    private FinanceManagerUI frame = null;

    private JButton ok, close;

    private JCheckBox cb;

    private static Logger logger = Logger.getLogger(UpdateCheckNotificationFrame.class.getName());

    private java.text.MessageFormat form = new java.text.MessageFormat(tr("New version \"{0}\" is available."));

    public UpdateCheckNotificationFrame(UINotification notification, FinanceManagerUI frame) {
        this.frame = frame;
        initialize();
    }

    private void initialize() {
        getContentPane().setLayout(new BorderLayout());
        message = new JLabel("");
        getContentPane().add(Box.createVerticalStrut(5), BorderLayout.NORTH);
        getContentPane().add(Box.createHorizontalStrut(10), BorderLayout.EAST);
        getContentPane().add(Box.createHorizontalStrut(10), BorderLayout.WEST);
        JPanel south = new JPanel();
        south.setLayout(new BoxLayout(south, BoxLayout.LINE_AXIS));
        close = new TimerButton("Close", FinanceManagerUI.WAIT_DURATION / 1000);
        close.addActionListener(new ActionListener() {

            public void actionPerformed(ActionEvent e) {
                boolean checked = !cb.isSelected();
                String prop = "MAIN.CHECKUPDTONSTARTUP";
                String value = Boolean.toString(checked);
                try {
                    if (cb.isSelected()) net.mjrz.fm.utils.PropertiesUtils.saveSettings("settings.properties", prop, value);
                } catch (Exception ex) {
                    logger.error(net.mjrz.fm.utils.MiscUtils.stackTrace2String(ex));
                }
                frame.hideSheet();
            }
        });
        ok = new JButton("Check");
        ok.addActionListener(new ActionListener() {

            public void actionPerformed(ActionEvent e) {
                boolean checked = !cb.isSelected();
                String prop = "MAIN.CHECKUPDTONSTARTUP";
                String value = Boolean.toString(checked);
                try {
                    checkForUpdates();
                    net.mjrz.fm.utils.PropertiesUtils.saveSettings("settings.properties", prop, value);
                } catch (Exception ex) {
                    logger.error(net.mjrz.fm.utils.MiscUtils.stackTrace2String(ex));
                }
            }
        });
        cb = new JCheckBox(tr("Do not ask this again"));
        cb.setOpaque(true);
        cb.setBackground(NotificationHandler.NOTIF_COLOR);
        south.add(cb);
        south.add(Box.createHorizontalGlue());
        south.add(ok);
        south.add(close);
        south.setBackground(NotificationHandler.NOTIF_COLOR);
        getContentPane().add(message, BorderLayout.CENTER);
        getContentPane().add(south, BorderLayout.SOUTH);
        getContentPane().setBackground(NotificationHandler.NOTIF_COLOR);
        ((JComponent) getContentPane()).setBorder(new LineBorder(Color.BLACK, 1));
        pack();
    }

    private void checkForUpdates() {
        ok.setEnabled(false);
        SwingWorker<String, Object> worker = new SwingWorker<String, Object>() {

            public String doInBackground() {
                BufferedReader in = null;
                try {
                    URL url = new URL(net.mjrz.fm.ui.utils.UIDefaults.LATEST_VERSION_URL);
                    HttpURLConnection conn = (HttpURLConnection) url.openConnection();
                    int status = conn.getResponseCode();
                    if (status == 200) {
                        in = new BufferedReader(new InputStreamReader(conn.getInputStream()));
                        StringBuilder str = new StringBuilder();
                        while (true) {
                            String line = in.readLine();
                            if (line == null) break;
                            str.append(line);
                        }
                        return str.toString();
                    } else {
                        logger.error("Unable to retrieve latest version: HTTP ERROR CODE: " + status);
                        return "";
                    }
                } catch (Exception e) {
                    logger.error("Unable to retrieve latest version: HTTP ERROR CODE: " + e.getMessage());
                    return null;
                } finally {
                    if (in != null) {
                        try {
                            in.close();
                        } catch (Exception e) {
                        }
                    }
                }
            }

            public void done() {
                try {
                    dispose();
                    String str = get();
                    if (str == null || str.length() == 0) {
                        JOptionPane.showMessageDialog(frame, tr("Unable to retrieve version information.\nPlease check network connectivity"), tr("Error"), JOptionPane.ERROR_MESSAGE);
                        return;
                    }
                    net.mjrz.fm.Version v = net.mjrz.fm.Version.getVersion();
                    if (v.isVersionGreater(str.toString())) {
                        String[] args = { str };
                        String msg = form.format(args);
                        int n = JOptionPane.showConfirmDialog(frame, msg + "\n" + tr("Do you want to download the latest version?"), tr("Message"), JOptionPane.YES_NO_OPTION);
                        if (n == JOptionPane.YES_OPTION) {
                            java.awt.Desktop d = Desktop.getDesktop();
                            if (Desktop.isDesktopSupported()) {
                                d.browse(new URI(UIDefaults.PRODUCT_DOWNLOAD_URL));
                            }
                        }
                    } else {
                        JOptionPane.showMessageDialog(frame, tr("No new updates are available"));
                    }
                } catch (Exception e) {
                    logger.error(MiscUtils.stackTrace2String(e));
                } finally {
                    frame.hideSheet();
                }
            }
        };
        worker.execute();
    }

    @Override
    public void setMessageText(String text) {
        message.setText(text);
    }
}
