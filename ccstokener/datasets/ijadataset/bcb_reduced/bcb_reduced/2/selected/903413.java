package net.sourceforge.juploader.gui;

import java.io.InputStream;
import java.net.URL;
import java.net.URLConnection;
import java.util.Locale;
import java.util.Scanner;
import javax.swing.JOptionPane;
import net.sourceforge.juploader.app.Application;
import net.sourceforge.juploader.app.update.UpdateChecker;
import net.sourceforge.juploader.filedownload.ProgressIndicator;

/**
 *
 * @author  proktor
 */
public class UpdateDialog extends javax.swing.JDialog {

    private UpdateChecker update = new UpdateChecker();

    private String changelogUrl;

    private java.awt.Frame parent;

    private boolean updated = false;

    /** Creates new form UpdateDialog */
    public UpdateDialog(java.awt.Frame parent, boolean modal) {
        super(parent, modal);
        initComponents();
        this.setModal(true);
        this.parent = parent;
        updateLabel.setText(java.util.ResourceBundle.getBundle("net/sourceforge/juploader/gui/Bundle").getString("NewVersionAvailable") + update.getRemoteVersion() + "!");
        this.setTitle(java.util.ResourceBundle.getBundle("net/sourceforge/juploader/gui/Bundle").getString("NewVersion"));
        fillChangelogField();
        changelogText.setVisible(true);
        downloadButton.setVisible(true);
        progressPanel.setVisible(true);
        setLocationRelativeTo(parent);
        setVisible(true);
    }

    private void setChangelogName() {
        if (Locale.getDefault().getLanguage().equals("pl")) {
            changelogUrl = Application.changelog;
        } else {
            changelogUrl = Application.changelog_en;
        }
    }

    private void fillChangelogField() {
        {
            setChangelogName();
            try {
                URL url = new URL(changelogUrl);
                URLConnection conn = url.openConnection();
                InputStream inStream = conn.getInputStream();
                Scanner in = new Scanner(inStream, "UTF-8");
                String line = in.nextLine();
                String changelog = "";
                while (in.hasNext() && !line.contains(Application.getAppVersion())) {
                    changelog += line + "\n";
                    line = in.nextLine();
                }
                in.close();
                inStream.close();
                changelogText.setText(changelog);
            } catch (Exception e) {
            }
        }
    }

    private void initComponents() {
        updateLabel = new javax.swing.JLabel();
        changelogPane = new javax.swing.JScrollPane();
        changelogText = new javax.swing.JTextPane();
        jPanel1 = new javax.swing.JPanel();
        downloadButton = new javax.swing.JButton();
        closeButton = new javax.swing.JButton();
        progressPanel = new javax.swing.JPanel();
        progressBar = new javax.swing.JProgressBar();
        setDefaultCloseOperation(javax.swing.WindowConstants.DISPOSE_ON_CLOSE);
        setResizable(false);
        updateLabel.setHorizontalAlignment(javax.swing.SwingConstants.CENTER);
        java.util.ResourceBundle bundle = java.util.ResourceBundle.getBundle("net/sourceforge/juploader/gui/Bundle");
        updateLabel.setText(bundle.getString("UpdateDialog.updateLabel.text"));
        changelogText.setEditable(false);
        changelogText.setFont(new java.awt.Font("Monospaced", 0, 11));
        changelogPane.setViewportView(changelogText);
        downloadButton.setText(bundle.getString("UpdateDialog.downloadButton.text"));
        downloadButton.addActionListener(new java.awt.event.ActionListener() {

            public void actionPerformed(java.awt.event.ActionEvent evt) {
                downloadButtonActionPerformed(evt);
            }
        });
        jPanel1.add(downloadButton);
        closeButton.setText(bundle.getString("Close"));
        closeButton.addActionListener(new java.awt.event.ActionListener() {

            public void actionPerformed(java.awt.event.ActionEvent evt) {
                closeButtonActionPerformed(evt);
            }
        });
        jPanel1.add(closeButton);
        progressPanel.setLayout(new java.awt.CardLayout());
        progressBar.setString(bundle.getString("UpdateDialog.progressBar.string"));
        progressBar.setStringPainted(true);
        progressPanel.add(progressBar, "card2");
        javax.swing.GroupLayout layout = new javax.swing.GroupLayout(getContentPane());
        getContentPane().setLayout(layout);
        layout.setHorizontalGroup(layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING).addGroup(layout.createSequentialGroup().addContainerGap().addGroup(layout.createParallelGroup(javax.swing.GroupLayout.Alignment.CENTER).addComponent(jPanel1, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE).addComponent(changelogPane, javax.swing.GroupLayout.DEFAULT_SIZE, 470, Short.MAX_VALUE)).addContainerGap()).addGroup(javax.swing.GroupLayout.Alignment.CENTER, layout.createSequentialGroup().addGap(12, 12, 12).addComponent(updateLabel, javax.swing.GroupLayout.DEFAULT_SIZE, 470, Short.MAX_VALUE).addGap(12, 12, 12)).addGroup(layout.createSequentialGroup().addGap(12, 12, 12).addComponent(progressPanel, javax.swing.GroupLayout.DEFAULT_SIZE, 470, Short.MAX_VALUE).addContainerGap(12, Short.MAX_VALUE)));
        layout.setVerticalGroup(layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING).addGroup(layout.createSequentialGroup().addContainerGap().addComponent(updateLabel, javax.swing.GroupLayout.PREFERRED_SIZE, 19, javax.swing.GroupLayout.PREFERRED_SIZE).addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED).addComponent(changelogPane, javax.swing.GroupLayout.PREFERRED_SIZE, 194, javax.swing.GroupLayout.PREFERRED_SIZE).addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED).addComponent(progressPanel, javax.swing.GroupLayout.PREFERRED_SIZE, 27, javax.swing.GroupLayout.PREFERRED_SIZE).addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.UNRELATED).addComponent(jPanel1, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE).addGap(26, 26, 26)));
        pack();
    }

    private void downloadButtonActionPerformed(java.awt.event.ActionEvent evt) {
        Runnable runnable = new Runnable() {

            public void run() {
                downloadButton.setEnabled(false);
                ProgressIndicator indicator = new ProgressIndicator() {

                    public void started(int total) {
                        progressBar.setMaximum(total);
                        progressBar.setString(java.util.ResourceBundle.getBundle("net/sourceforge/juploader/gui/Bundle").getString("AutoUpdate.updating"));
                    }

                    public void progress(int current) {
                        progressBar.setValue(current);
                    }

                    public void finished() {
                        progressBar.setString(java.util.ResourceBundle.getBundle("net/sourceforge/juploader/gui/Bundle").getString("AutoUpdate.complete"));
                        Application.setWasUpdate(true);
                        closeButton.setEnabled(true);
                        UpdateDialog.this.updated = true;
                        if (!UpdateDialog.this.isVisible()) {
                            JOptionPane.showMessageDialog(parent, java.util.ResourceBundle.getBundle("net/sourceforge/juploader/gui/Bundle").getString("AutoUpdate.complete"), java.util.ResourceBundle.getBundle("net/sourceforge/juploader/gui/Bundle").getString("Information"), JOptionPane.INFORMATION_MESSAGE);
                        }
                    }
                };
                try {
                    Application.updateProgram(indicator);
                } catch (Exception ex) {
                    ex.printStackTrace();
                    Application.setWasUpdate(false);
                    downloadButton.setEnabled(true);
                    progressBar.setString(java.util.ResourceBundle.getBundle("net/sourceforge/juploader/gui/Bundle").getString("AutoUpdate.failed"));
                }
            }
        };
        new Thread(runnable).start();
    }

    private void closeButtonActionPerformed(java.awt.event.ActionEvent evt) {
        if (updated) {
            System.exit(0);
        } else {
            dispose();
        }
    }

    private javax.swing.JScrollPane changelogPane;

    private javax.swing.JTextPane changelogText;

    private javax.swing.JButton closeButton;

    private javax.swing.JButton downloadButton;

    private javax.swing.JPanel jPanel1;

    private javax.swing.JProgressBar progressBar;

    private javax.swing.JPanel progressPanel;

    private javax.swing.JLabel updateLabel;
}
