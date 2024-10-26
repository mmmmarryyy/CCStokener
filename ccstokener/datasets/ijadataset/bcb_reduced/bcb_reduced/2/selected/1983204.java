package icm.unicore.plugins.dbbrowser.util;

import java.io.*;
import java.net.*;
import javax.swing.*;
import java.awt.Component;

/** This class downloads files from URLs.
 * @author Michal Wronski (wrona@mat.uni.torun.pl)
 * @version 1.1
 */
public class SaveUrlThread extends JFrame implements Runnable {

    Component parent;

    String urlString;

    String dest;

    boolean end;

    boolean verbose;

    /** Create new SaveUrlThread
	 * @param parent Parent's frame
	 * @param urlString Source (should be valid url string)
	 * @param dest Destination file path
	 * @param verbose Verbose mode
	 */
    public SaveUrlThread(Component parent, String urlString, String dest, boolean verbose) {
        super("Download");
        this.parent = parent;
        this.urlString = urlString;
        this.dest = dest;
        this.verbose = verbose;
        if (verbose) initComponents();
    }

    private void initComponents() {
        mainPanel = new javax.swing.JPanel();
        upperPanel = new javax.swing.JPanel();
        destLabel = new javax.swing.JLabel();
        centerPanel = new javax.swing.JPanel();
        saveProgressBar = new javax.swing.JProgressBar();
        bottomPanel = new javax.swing.JPanel();
        cancelButton = new javax.swing.JButton();
        addWindowListener(new java.awt.event.WindowAdapter() {

            public void windowClosing(java.awt.event.WindowEvent evt) {
                end = true;
                dispose();
            }
        });
        mainPanel.setLayout(new javax.swing.BoxLayout(mainPanel, javax.swing.BoxLayout.Y_AXIS));
        upperPanel.setLayout(new java.awt.FlowLayout(java.awt.FlowLayout.LEFT));
        destLabel.setText("Destination: " + dest);
        upperPanel.add(destLabel);
        mainPanel.add(upperPanel);
        saveProgressBar.setPreferredSize(new java.awt.Dimension(250, 14));
        centerPanel.add(saveProgressBar);
        mainPanel.add(centerPanel);
        cancelButton.setText("Cancel");
        cancelButton.addActionListener(new java.awt.event.ActionListener() {

            public void actionPerformed(java.awt.event.ActionEvent evt) {
                end = true;
                dispose();
            }
        });
        bottomPanel.add(cancelButton);
        mainPanel.add(bottomPanel);
        getContentPane().add(mainPanel, java.awt.BorderLayout.CENTER);
        pack();
    }

    private void deleteFile() {
        java.io.File f = new java.io.File(dest);
        f.delete();
    }

    /** Shows dialog
	 */
    public void show() {
        int xpos = (int) (parent.getLocation().getX() + parent.getWidth() / 2 - getPreferredSize().getWidth() / 2);
        int ypos = (int) (parent.getLocation().getY() + parent.getHeight() / 2 - getPreferredSize().getHeight() / 2);
        setLocation(xpos, ypos);
        super.show();
    }

    /** Runs thread
	 */
    public void run() {
        end = false;
        if (verbose) show();
        try {
            URL url = new URL(urlString);
            URLConnection uc = url.openConnection();
            int contentLength = uc.getContentLength();
            if ((contentLength == -1) && verbose) saveProgressBar.setEnabled(false);
            InputStream raw = uc.getInputStream();
            InputStream in = new BufferedInputStream(raw);
            byte[] data = new byte[4096];
            int bytesRead = 0;
            int total = 0;
            BufferedOutputStream out = new BufferedOutputStream(new FileOutputStream(dest));
            if (contentLength != -1) {
                while (total < contentLength) {
                    if (end) {
                        out.close();
                        deleteFile();
                        break;
                    }
                    bytesRead = in.read(data, 0, 4096);
                    if (bytesRead == -1) break;
                    total += bytesRead;
                    out.write(data, 0, bytesRead);
                    if (verbose) saveProgressBar.setValue((int) (100 * ((float) total / (float) contentLength)));
                }
            } else {
                while ((bytesRead = in.read(data, 0, 4096)) != -1) {
                    if (end) {
                        out.close();
                        deleteFile();
                        break;
                    }
                    out.write(data, 0, bytesRead);
                }
            }
            in.close();
            if (!end) {
                out.flush();
                out.close();
            }
        } catch (Exception e) {
            JOptionPane.showMessageDialog(parent, "Can't download file\n" + e.toString(), "Error", JOptionPane.ERROR_MESSAGE);
        }
        if (verbose) dispose();
    }

    private javax.swing.JPanel mainPanel;

    private javax.swing.JPanel upperPanel;

    private javax.swing.JLabel destLabel;

    private javax.swing.JPanel centerPanel;

    private javax.swing.JProgressBar saveProgressBar;

    private javax.swing.JPanel bottomPanel;

    private javax.swing.JButton cancelButton;
}
