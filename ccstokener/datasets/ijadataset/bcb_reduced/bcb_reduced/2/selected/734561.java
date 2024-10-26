package org.opencdspowered.opencds.ui.update;

import org.opencdspowered.opencds.core.lang.DynamicLocalisation;
import org.opencdspowered.opencds.core.update.*;
import org.opencdspowered.opencds.core.config.ConfigurationManager;
import org.opencdspowered.opencds.core.util.Constants;
import org.opencdspowered.opencds.ui.main.*;
import org.opencdspowered.opencds.ui.util.wizard.*;
import javax.swing.*;
import java.awt.Dimension;
import java.awt.BorderLayout;
import java.awt.Component;
import java.awt.Color;
import java.util.*;
import java.io.*;
import java.net.URL;
import java.net.URLConnection;

/**
 * The download update page of the update wizard.
 * 
 * @author  Lars 'Levia' Wesselius
*/
public class DownloadUpdatesPage extends WizardPage implements UpdateProgressListener {

    private MainFrame m_MainFrame;

    private SelectUpdatesPage m_SelectUpdatesPage;

    private JProgressBar m_DownloadProgress;

    private SelectedDownloadsList m_SelectedDownloadsList;

    private JLabel m_DownloadLabel;

    private int m_TotalUpdates = 0;

    private WizardDialog m_WizardDialog;

    private boolean m_IsDownloading = false;

    private boolean m_Lock = false;

    private JLabel m_CompletedMessage;

    public DownloadUpdatesPage(MainFrame frame, ImageIcon icon, SelectUpdatesPage page, WizardDialog dlg) {
        super(false, true);
        m_MainFrame = frame;
        m_SelectUpdatesPage = page;
        m_WizardDialog = dlg;
        initialize(icon);
    }

    public void initialize(ImageIcon icon) {
        DynamicLocalisation loc = m_MainFrame.getLocalisation();
        m_Panel = new JPanel();
        m_Panel.setOpaque(true);
        m_Panel.setLayout(new BorderLayout());
        JLabel label = new JLabel();
        label.setIcon(icon);
        label.setBorder(BorderFactory.createEmptyBorder(0, 0, 0, 5));
        JPanel rightPanel = new JPanel();
        rightPanel.setLayout(new BoxLayout(rightPanel, BoxLayout.PAGE_AXIS));
        m_DownloadLabel = new JLabel(loc.getMessage("Updater.DownloadingUpdates"));
        m_DownloadLabel.setAlignmentX(Component.LEFT_ALIGNMENT);
        m_DownloadProgress = new JProgressBar();
        m_DownloadProgress.setStringPainted(true);
        m_DownloadProgress.setBackground(Color.BLUE);
        m_DownloadProgress.setAlignmentX(Component.LEFT_ALIGNMENT);
        m_SelectedDownloadsList = new SelectedDownloadsList(m_MainFrame);
        m_SelectedDownloadsList.setMaximumSize(new Dimension(800, 50));
        m_SelectedDownloadsList.setAlignmentX(Component.LEFT_ALIGNMENT);
        JScrollPane pane = new JScrollPane(m_SelectedDownloadsList);
        pane.setAlignmentX(Component.LEFT_ALIGNMENT);
        m_CompletedMessage = new JLabel();
        m_CompletedMessage.setIcon(new ImageIcon(DownloadUpdatesPage.class.getResource("/org/opencdspowered/opencds/ui/icons/wizard-update-completed.png")));
        m_CompletedMessage.setText(loc.getMessage("Updater.Pages.Completed"));
        m_CompletedMessage.setVisible(false);
        m_CompletedMessage.setAlignmentX(Component.LEFT_ALIGNMENT);
        rightPanel.add(m_DownloadLabel);
        rightPanel.add(Box.createRigidArea(new Dimension(0, 5)));
        rightPanel.add(pane);
        rightPanel.add(Box.createRigidArea(new Dimension(0, 5)));
        rightPanel.add(m_DownloadProgress);
        rightPanel.add(Box.createRigidArea(new Dimension(0, 10)));
        rightPanel.add(m_CompletedMessage);
        rightPanel.add(Box.createRigidArea(new Dimension(0, 10)));
        m_Panel.add(label, BorderLayout.WEST);
        m_Panel.add(rightPanel);
    }

    public void pageOpened(WizardDialog dlg) {
        if (m_Lock) {
            return;
        } else {
            m_Lock = true;
        }
        m_WizardDialog.setBackEnabled(false);
        m_WizardDialog.setCancelEnabled(false);
        if (m_SelectedDownloadsList.getRowCount() != 0) {
            m_SelectedDownloadsList.removeAllRows();
        }
        Enumeration<Update> entries = m_SelectUpdatesPage.getSelectedUpdates();
        for (; entries.hasMoreElements(); ) {
            m_SelectedDownloadsList.addUpdate(entries.nextElement());
        }
        if (m_SelectUpdatesPage.getTotalSelectedUpdates() <= 0) {
            finished();
            return;
        }
        Thread t = new Thread(new Runnable() {

            public void run() {
                m_IsDownloading = true;
                Enumeration<Update> entries = m_SelectUpdatesPage.getSelectedUpdates();
                int updateNumber = 0;
                m_TotalUpdates = m_SelectUpdatesPage.getTotalUpdates();
                for (; entries.hasMoreElements(); ) {
                    ++updateNumber;
                    Update update = entries.nextElement();
                    m_DownloadProgress.setMaximum(update.getSize());
                    m_DownloadProgress.setString("Downloading update " + updateNumber + "/" + m_TotalUpdates + " ");
                    downloadUpdate(update);
                    m_SelectedDownloadsList.removeUpdate(update);
                }
                finished();
            }
        });
        t.start();
    }

    public boolean downloadUpdate(Update upd) {
        upd.addListener(this);
        OutputStream out = null;
        URLConnection conn = null;
        InputStream in = null;
        try {
            URL url = new URL(Constants.UPDATE_DIR + upd.getPath());
            String path = upd.getPath();
            if (path.startsWith("./")) {
                path = path.substring(2, path.length());
            }
            path = "updates/" + path;
            int index = path.lastIndexOf("/");
            if (index != -1) {
                String dirs = path.substring(0, path.lastIndexOf("/"));
                System.out.println(dirs);
                File file = new File(dirs);
                if (!file.exists()) {
                    System.out.println(file.mkdirs());
                }
            }
            File file = new File(path);
            if (!file.exists()) {
                if (file.isDirectory()) {
                    file.mkdirs();
                } else {
                    file.createNewFile();
                }
            }
            System.out.println(11);
            out = new BufferedOutputStream(new FileOutputStream(file));
            conn = url.openConnection();
            in = conn.getInputStream();
            byte[] buffer = new byte[1024];
            int numRead;
            int numWritten = 0;
            System.out.println(12);
            while ((numRead = in.read(buffer)) != -1) {
                System.out.println(13);
                out.write(buffer, 0, numRead);
                numWritten += numRead;
                upd.setProgress(numWritten);
            }
        } catch (Exception exception) {
            org.opencdspowered.opencds.core.logging.Logger.getInstance().logException(exception);
            return false;
        } finally {
            try {
                if (in != null) {
                    in.close();
                }
                if (out != null) {
                    out.close();
                }
            } catch (IOException ioe) {
                org.opencdspowered.opencds.core.logging.Logger.getInstance().logException(ioe);
                return false;
            }
        }
        upd.removeListener(this);
        return true;
    }

    public void progress(int totalFileSize, int currentFileSize, double kbsec) {
        m_DownloadProgress.setValue(currentFileSize);
        String ret = m_DownloadProgress.getString();
        ret = ret.substring(0, ret.indexOf(" ", 20));
        m_DownloadProgress.setString(ret + " " + String.valueOf(kbsec) + " KB/Sec");
    }

    public void finished() {
        m_WizardDialog.setBackEnabled(true);
        String completed = m_MainFrame.getLocalisation().getMessage("Updater.CompletedAllUpdates");
        if (m_SelectUpdatesPage.getTotalSelectedUpdates() <= 0) {
            completed = m_MainFrame.getLocalisation().getMessage("Updater.NoUpdatesDownloaded");
            m_CompletedMessage.setText(m_MainFrame.getLocalisation().getMessage("Updater.Pages.NoUpdatesDownloaded"));
        }
        m_DownloadLabel.setText(completed);
        m_DownloadProgress.setString(completed);
        m_IsDownloading = false;
        m_CompletedMessage.setVisible(true);
        m_WizardDialog.setCancelEnabled(false);
        m_WizardDialog.setFinishEnabled(true);
    }

    public void cancelPressed(WizardDialog dlg) {
        if (!m_IsDownloading) {
            dlg.dispose();
        }
    }

    public void finishPressed(WizardDialog dlg) {
        DynamicLocalisation loc = m_MainFrame.getLocalisation();
        if (m_SelectUpdatesPage.getTotalSelectedUpdates() != 0) {
            ConfigurationManager.getInstance().setValue("Updater.ShowChangelog", "true");
            MenuBar bar = (MenuBar) m_MainFrame.getFrame().getJMenuBar();
            bar.updatesInstalled();
            m_MainFrame.stopOpenCDS(2);
        } else {
            dlg.dispose();
        }
    }
}
