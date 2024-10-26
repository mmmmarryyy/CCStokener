package org.gdi3d.xnavi.navigator;

import java.awt.Cursor;
import java.awt.Toolkit;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.awt.event.WindowEvent;
import java.awt.event.WindowListener;
import java.io.BufferedOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.net.URL;
import java.net.URLConnection;
import java.util.Locale;
import java.util.ResourceBundle;
import java.util.Vector;
import java.util.zip.ZipEntry;
import java.util.zip.ZipInputStream;
import javax.swing.ImageIcon;
import javax.swing.JButton;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JTextPane;
import javax.swing.border.TitledBorder;
import org.gdi3d.xnavi.swing.I3Label;

/**
 * @author arne
 *
 */
public class Updater implements WindowListener {

    private ResourceBundle i18n;

    public void setI18n(ResourceBundle i18n) {
        this.i18n = i18n;
    }

    public void doUpdate(String version) {
        try {
            final String hyperlink_url = "http://xnavigator.sourceforge.net/dist/";
            JFrame updateInfoFrame = null;
            try {
                JPanel panel = new JPanel();
                panel.setLayout(null);
                panel.setBackground(new java.awt.Color(255, 255, 255));
                panel.setBorder(new TitledBorder(""));
                ClassLoader cl = this.getClass().getClassLoader();
                int BORDER_TOP = 10;
                int PANEL_WIDTH = 400;
                int TEXT_WIDTH = 360;
                int TEXT_HEIGHT = 50;
                int TEXT_LEFT = 20;
                int y = BORDER_TOP;
                I3Label title = new I3Label("XNavigator Update");
                title.setBounds(30, y, 350, 25);
                panel.add(title);
                ImageIcon splash3 = new ImageIcon(Toolkit.getDefaultToolkit().getImage(cl.getResource("resources/splash3.jpg")));
                JButton left = new JButton(splash3);
                left.setBounds(20, y += 30, 350, 235);
                left.setBorder(null);
                left.setFocusPainted(false);
                panel.add(left);
                JTextPane informText = new JTextPane();
                informText.setLayout(null);
                informText.setBounds(TEXT_LEFT, y += 235, TEXT_WIDTH, TEXT_HEIGHT);
                informText.setBackground(new java.awt.Color(255, 255, 255));
                informText.setEditable(false);
                informText.setFocusable(false);
                panel.add(informText);
                JTextPane progressText = new JTextPane();
                progressText.setLayout(null);
                progressText.setBounds(TEXT_LEFT, y += TEXT_HEIGHT, TEXT_WIDTH, TEXT_HEIGHT);
                progressText.setBackground(new java.awt.Color(255, 255, 255));
                progressText.setEditable(false);
                progressText.setFocusable(false);
                panel.add(progressText);
                updateInfoFrame = new JFrame();
                updateInfoFrame.setUndecorated(false);
                updateInfoFrame.setTitle("XNavigator Update");
                updateInfoFrame.setSize(400, 430);
                updateInfoFrame.getContentPane().add(panel);
                updateInfoFrame.setVisible(true);
                updateInfoFrame.setEnabled(true);
                updateInfoFrame.setResizable(false);
                updateInfoFrame.setLocation(300, 150);
                updateInfoFrame.addWindowListener(this);
                panel.repaint();
                informText.setText(i18n.getString("UPDATE_CHECK_INSTANCES"));
                String message0 = i18n.getString("UPDATE_INSTANCES");
                JLabel label01 = new JLabel("<html><head><style type=\"text/css\"><!--.Stil2 {font-size: 10px;font-weight: bold;}--></style></head><body><span class=\"Stil2\">XNavigator Update</span></body></html>");
                JLabel label02 = new JLabel("<html><head><style type=\"text/css\"><!--.Stil2 {font-size: 10px;font-weight: normal;}--></style></head><body><span class=\"Stil2\">" + "<br>" + message0 + " " + "</span></body></html>");
                Object[] objects0 = { label01, label02 };
                Object[] options0 = { i18n.getString("CONTINUE"), i18n.getString("CANCEL") };
                int option = JOptionPane.showOptionDialog(null, objects0, "Update", JOptionPane.DEFAULT_OPTION, JOptionPane.INFORMATION_MESSAGE, null, options0, options0[0]);
                if (option == 0) {
                } else {
                    updateInfoFrame.dispose();
                    return;
                }
                informText.setText(i18n.getString("UPDATE_CHECK_ENVIRONMENT"));
                if ((new File(".project")).exists()) {
                    Object[] objects = { "Im Eclipse Projekt solltest Du besser die neueste Version aus dem SVN ziehen -Arne-", "Update abgebrochen" };
                    JOptionPane.showMessageDialog(null, objects, "Update Error", JOptionPane.ERROR_MESSAGE);
                    updateInfoFrame.dispose();
                    return;
                }
                Object[] objects1 = { i18n.getString("UPDATE_WARNING") };
                Object[] options1 = { i18n.getString("CONTINUE"), i18n.getString("CANCEL") };
                int opt = JOptionPane.showOptionDialog(null, objects1, i18n.getString("WARNING"), JOptionPane.DEFAULT_OPTION, JOptionPane.INFORMATION_MESSAGE, null, options1, options1[0]);
                if (opt == 1) {
                    updateInfoFrame.dispose();
                    return;
                }
                updateInfoFrame.requestFocus();
                updateInfoFrame.requestFocusInWindow();
                informText.setText(i18n.getString("UPDATE_DOWNLOADING"));
                String updateFile = "XNavigator-" + version + ".zip";
                URL url = new URL(hyperlink_url + updateFile);
                URLConnection conn = url.openConnection();
                int fileSize = conn.getContentLength();
                String urlString = url.toString();
                progressText.setText("Download " + urlString + " ... 0%");
                java.io.BufferedInputStream in = new java.io.BufferedInputStream(url.openStream());
                java.io.FileOutputStream fos = new java.io.FileOutputStream(updateFile);
                java.io.BufferedOutputStream bout = new BufferedOutputStream(fos, 1024);
                int BUFFER_SIZE = 1024;
                byte data[] = new byte[BUFFER_SIZE];
                int count = 0;
                int size = 0;
                int prev_perc = 0;
                while ((count = in.read(data, 0, BUFFER_SIZE)) > 0) {
                    bout.write(data, 0, count);
                    size += count;
                    int perc = (100 * size) / fileSize;
                    if (perc > prev_perc) {
                        progressText.setText("Download " + urlString + " ... " + perc + "%");
                        prev_perc = perc;
                    }
                }
                bout.close();
                fos.close();
                in.close();
                progressText.setText("Download " + url.toString() + " ... ok.");
                informText.setText(i18n.getString("UPDATE_EXTRACTING"));
                boolean deleted = deleteFiles(new File("./lib"), false);
                if (!deleted) {
                    updateInfoFrame.dispose();
                    return;
                }
                extractZipFile(updateFile, progressText);
                progressText.setText(i18n.getString("UPDATE_COMPLETE"));
                Object[] objects = { i18n.getString("UPDATE_COMPLETE") };
                Object[] options = { i18n.getString("OK") };
                JOptionPane.showOptionDialog(null, objects, "Success", JOptionPane.DEFAULT_OPTION, JOptionPane.INFORMATION_MESSAGE, null, options, options[0]);
                System.exit(0);
            } catch (Exception e) {
                e.printStackTrace();
                String message = "";
                String hyperlink = "";
                message = i18n.getString("UPDATE_FAILED");
                hyperlink = "<a href='" + hyperlink_url + "'>" + hyperlink_url + "</a>";
                JLabel label2 = new JLabel("<html><head><style type=\"text/css\"><!--.Stil2 {font-size: 10px;font-weight: normal;}--></style></head><body><span class=\"Stil2\">" + "<br>" + message + " " + "</span></body></html>");
                JLabel label3 = new JLabel("<html><head><style type=\"text/css\"><!--.Stil2 {font-size: 10px;font-weight: normal;}--></style></head><body><span class=\"Stil2\">" + hyperlink + "<br>" + "</span></body></html>");
                JLabel label4 = new JLabel("<html><head><style type=\"text/css\"><!--.Stil2 {font-size: 10px;font-weight: normal;}--></style></head><body><span class=\"Stil2\">" + version + "</span></body></html>");
                label3.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
                label3.addMouseListener(new MouseAdapter() {

                    @Override
                    public void mouseClicked(MouseEvent e) {
                        if (e.getClickCount() > 0) {
                            try {
                                javax.jnlp.BasicService basicService;
                                basicService = (javax.jnlp.BasicService) javax.jnlp.ServiceManager.lookup("javax.jnlp.BasicService");
                                basicService.showDocument(new URL(hyperlink_url));
                            } catch (Exception e1) {
                                e1.printStackTrace();
                                try {
                                    Runtime.getRuntime().exec("cmd.exe /c start " + hyperlink_url);
                                } catch (IOException e2) {
                                    e2.printStackTrace();
                                }
                            }
                        }
                    }
                });
                Object[] objects = { label2, label3, label4 };
                Object[] options = { i18n.getString("OK") };
                updateInfoFrame.dispose();
                JOptionPane.showOptionDialog(null, objects, "Error", JOptionPane.DEFAULT_OPTION, JOptionPane.ERROR_MESSAGE, null, options, options[0]);
            }
            updateInfoFrame.setVisible(false);
            updateInfoFrame.dispose();
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    private boolean deleteFiles(File fileOrDirectory, boolean deleteDir) {
        boolean deleted = true;
        if (fileOrDirectory.exists()) {
            if (fileOrDirectory.isDirectory()) {
                File[] files = fileOrDirectory.listFiles();
                for (File file : files) {
                    deleted = deleteFiles(file, true);
                    if (!deleted) return false;
                }
                if (deleteDir) {
                    deleted = fileOrDirectory.delete();
                    if (!deleted) {
                        Object[] options = { i18n.getString("OK") };
                        Object[] objects = { i18n.getString("UPDATE_COULD_NOT_DELETE") + " " + fileOrDirectory.getAbsoluteFile() + ". " + i18n.getString("UPDATE_ABORT") };
                        JOptionPane.showOptionDialog(null, objects, "Error", JOptionPane.DEFAULT_OPTION, JOptionPane.ERROR_MESSAGE, null, options, options[0]);
                        return false;
                    }
                }
            } else {
                if (fileOrDirectory.getName().equals("xnavigator.jar") || fileOrDirectory.getName().equals("resources.jar")) {
                } else {
                    deleted = fileOrDirectory.delete();
                    if (!deleted) {
                        Object[] options = { i18n.getString("OK") };
                        Object[] objects = { i18n.getString("UPDATE_COULD_NOT_DELETE") + " " + fileOrDirectory.getAbsoluteFile() + ". " + i18n.getString("UPDATE_FILE_IN_USE") + " " + i18n.getString("UPDATE_ABORT") };
                        JOptionPane.showOptionDialog(null, objects, "Error", JOptionPane.DEFAULT_OPTION, JOptionPane.ERROR_MESSAGE, null, options, options[0]);
                        return false;
                    }
                }
            }
        }
        return deleted;
    }

    private void extractZipFile(String filename, JTextPane progressText) throws IOException {
        String destinationname = "";
        byte[] buf = new byte[1024];
        ZipInputStream zipinputstream = null;
        ZipEntry zipentry;
        zipinputstream = new ZipInputStream(new FileInputStream(filename));
        while ((zipentry = zipinputstream.getNextEntry()) != null) {
            String entryName = zipentry.getName();
            if (progressText != null) {
                progressText.setText("extracting " + entryName);
            }
            int n;
            FileOutputStream fileoutputstream;
            if (zipentry.isDirectory()) {
                (new File(destinationname + entryName)).mkdir();
                continue;
            }
            fileoutputstream = new FileOutputStream(destinationname + entryName);
            while ((n = zipinputstream.read(buf, 0, 1024)) > -1) fileoutputstream.write(buf, 0, n);
            fileoutputstream.close();
            zipinputstream.closeEntry();
        }
        if (progressText != null) {
            progressText.setText("Files extracted");
        }
        zipinputstream.close();
    }

    public static void main(String[] args) {
        String languageCode = "en";
        if (args.length > 0) {
            String updateToVersion = args[0];
            if (args.length > 1) {
                languageCode = args[1];
            }
            Locale currentLocale = new Locale(languageCode);
            ResourceBundle i18n = ResourceBundle.getBundle("org.gdi3d.xnavi.navigator.Messages", currentLocale);
            Updater updater = new Updater();
            updater.setI18n(i18n);
            updater.doUpdate(updateToVersion);
        }
    }

    @Override
    public void windowActivated(WindowEvent e) {
    }

    @Override
    public void windowClosed(WindowEvent e) {
    }

    @Override
    public void windowClosing(WindowEvent e) {
        System.exit(0);
    }

    @Override
    public void windowDeactivated(WindowEvent e) {
    }

    @Override
    public void windowDeiconified(WindowEvent e) {
    }

    @Override
    public void windowIconified(WindowEvent e) {
    }

    @Override
    public void windowOpened(WindowEvent e) {
    }
}
