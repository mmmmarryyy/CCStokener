package net.mjrz.fm.ui.dialogs;

import javax.swing.*;
import org.apache.log4j.Logger;
import net.mjrz.fm.Main;
import net.mjrz.fm.ui.FinanceManagerUI;
import net.mjrz.fm.utils.MiscUtils;
import net.mjrz.fm.utils.ZProperties;
import static net.mjrz.fm.utils.Messages.tr;
import java.awt.event.*;
import java.awt.*;
import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.File;
import java.io.FileWriter;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.URI;
import java.net.URL;

public class UpdateCheckDialog extends JDialog {

    private static final long serialVersionUID = 1L;

    private JButton ok, cancel;

    private JCheckBox cb;

    private static Logger logger = Logger.getLogger(ExitPromptDialog.class.getName());

    public static final int EXIT = 1;

    public static final int LOGOUT = 2;

    private int type = 0;

    private String userName = null;

    private FinanceManagerUI parent;

    public UpdateCheckDialog(String username, FinanceManagerUI parent) {
        super(parent, "Confirm", true);
        this.userName = username;
        this.parent = parent;
        initialize();
        net.mjrz.fm.ui.utils.GuiUtilities.addWindowClosingActionMap(this);
    }

    private void initialize() {
        Container cp = getContentPane();
        cp.setLayout(new BorderLayout(5, 5));
        JLabel l = null;
        l = new JLabel(tr("Do you want to check for updated version of iFreeBudget?"), new ImageIcon("icons/question.png"), JLabel.LEADING);
        JPanel p = new JPanel();
        p.setLayout(new BorderLayout());
        p.add(l, BorderLayout.CENTER);
        cp.add(p, BorderLayout.NORTH);
        ok = new JButton(tr("Yes"));
        ok.setPreferredSize(new Dimension(75, 25));
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
        cancel = new JButton(tr("No"));
        cancel.setPreferredSize(new Dimension(75, 25));
        cancel.addActionListener(new ActionListener() {

            public void actionPerformed(ActionEvent e) {
                boolean checked = !cb.isSelected();
                String prop = "MAIN.CHECKUPDTONSTARTUP";
                String value = Boolean.toString(checked);
                try {
                    if (cb.isSelected()) net.mjrz.fm.utils.PropertiesUtils.saveSettings("settings.properties", prop, value);
                } catch (Exception ex) {
                    logger.error(net.mjrz.fm.utils.MiscUtils.stackTrace2String(ex));
                }
                dispose();
            }
        });
        cb = new JCheckBox(tr("Do not ask this again"));
        JPanel s = new JPanel();
        s.setLayout(new BoxLayout(s, BoxLayout.LINE_AXIS));
        s.add(cb);
        s.add(Box.createHorizontalGlue());
        s.add(ok);
        s.add(Box.createHorizontalStrut(5));
        s.add(cancel);
        s.add(Box.createHorizontalStrut(5));
        cp.add(s, BorderLayout.SOUTH);
        ok.requestFocusInWindow();
        setDefaultCloseOperation(JDialog.DISPOSE_ON_CLOSE);
        setMinimumSize(new Dimension(500, 200));
    }

    public void setDialogFocus() {
        if (ok != null) ok.requestFocusInWindow();
    }

    private void checkForUpdates() {
        SwingWorker<String, Object> worker = new SwingWorker<String, Object>() {

            public String doInBackground() {
                ok.setEnabled(false);
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
                            logger.error(e);
                        }
                    }
                }
            }

            public void done() {
                try {
                    dispose();
                    String str = get();
                    if (str == null || str.length() == 0) {
                        JOptionPane.showMessageDialog(parent, tr("Unable to retrieve version information.\nPlease check network connectivity"), tr("Error"), JOptionPane.ERROR_MESSAGE);
                        return;
                    }
                    net.mjrz.fm.Version v = net.mjrz.fm.Version.getVersion();
                    if (v.isVersionGreater(str.toString())) {
                        String[] args = { str };
                        String msg = form.format(args);
                        int n = JOptionPane.showConfirmDialog(parent, msg + "\n" + tr("Do you want to download the latest version?"), tr("Message"), JOptionPane.YES_NO_OPTION);
                        if (n == JOptionPane.YES_OPTION) {
                            java.awt.Desktop d = Desktop.getDesktop();
                            if (Desktop.isDesktopSupported()) {
                                d.browse(new URI("http://www.mjrz.net/dl.html"));
                            }
                        }
                    } else {
                        JOptionPane.showMessageDialog(UpdateCheckDialog.this, tr("No new updates are available"));
                    }
                } catch (Exception e) {
                    logger.error(MiscUtils.stackTrace2String(e));
                }
            }
        };
        worker.execute();
    }

    java.text.MessageFormat form = new java.text.MessageFormat(tr("New version \"{0}\" is available."));
}
