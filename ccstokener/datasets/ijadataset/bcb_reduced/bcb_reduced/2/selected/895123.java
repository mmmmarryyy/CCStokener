package org.kablink.teaming.applets.fileedit;

import java.awt.*;
import java.awt.event.*;
import java.applet.*;
import java.net.*;
import javax.swing.*;
import java.security.*;
import java.io.*;
import java.util.Properties;
import netscape.javascript.JSObject;

/**
 * Class: Launcher
 * Purpose: This Class is the starting point for the file edit applet.
 *          This signed applet allows a user to download, edit, and upload
 *          a file in one click.
 *
 *          This class in particular gets the parameters from the http server,
 *          and then looks on the users disk to see if the file already exists.
 *          If it does, then it asks the user whether or not to use that file,
 *          otherwise, it downloads the file from the http server.
 */
public class launcher extends JApplet {

    FEData data = new FEData();

    JButton downloadB;

    final String FS = System.getProperty("file.separator");

    final String DOT = ".";

    static int retVal = 0;

    LaunchDoc launch;

    JRadioButton newCopyB, useCopyB;

    public void stop() {
        if ((downloadB != null) && (!downloadB.getText().equals(new String("foo")))) {
            super.stop();
        } else super.stop();
    }

    public void init() {
        data.setApplet(this);
        data.setOrigFileName(getParameter("fileName"));
        data.setPostUrl(getParameter("postURL"));
        data.setDocumentURL(getParameter("getURL"));
        data.setUpdateInForum(getParameter("updateFlag"));
        data.setRelativePath(getParameter("relativePath"));
        data.setForumName(getParameter("forumName"));
        data.setDocId(getParameter("docId"));
        data.setZoneName(getParameter("zoneName"));
        data.setParamText("saveText", "Save changes to this file");
        data.setParamText("abandonText", "Discard the edit");
        data.setParamText("unlockText", "Unlock the entry");
        data.setParamText("prevText", "Save previous versions");
        data.setParamText("reserveText", "Reserve and edit file attachment");
        data.setParamText("savedText", "A copy of this file is already saved to your computer.");
        data.setParamText("wantText", "Do you want to:");
        data.setParamText("downloadText", "Download a new copy of this file for editing");
        data.setParamText("useText", "Use the file copy on disk");
        data.setParamText("okButtonText", "OK");
        data.setParamText("cancelButtonText", "Cancel");
        data.setParamText("helpButtonText", "Help");
        data.setParamText("editingText", "Editing ");
        data.setParamText("cautionText", "Caution: If you leave this page, your edits will not be saved to the Forum");
        data.setParamText("processingText", "Processing...");
        data.setParamText("noProcText", "No process instantiated");
        data.setParamText("browserGoneText", "The browser page changed or the browser was killed, edit's will not be saved");
        data.setParamText("badFileText", "The file was not launched properly, exit value was:");
        data.setParamText("saveDiscardText", "Save or discard changes and return to Forum");
        data.setParamText("saveAbandonText", "Save or abandon changes?");
        data.setParamText("notificationBlockText", "Do not send notifications for this action");
        data.setParamText("notificationBlock", "no");
        if (data.getParamText("notificationBlock").equalsIgnoreCase("yes")) {
            data.setUseNotificationBlock(true);
        } else {
            data.setUseNotificationBlock(false);
        }
        repaint();
    }

    /**
   * This is an event handler for the button press. When called, download the document
   * from the host, and then launch it.
   */
    public void start() {
        int temp;
        int c;
        URL url;
        HttpURLConnection urlConn = null;
        DataInputStream dis = null;
        FileOutputStream fos = null;
        String javaVersion = System.getProperty("java.version");
        if (javaVersion.startsWith("1.1") || javaVersion.startsWith("1.2") || javaVersion.startsWith("1.3")) {
            displayJavaVersionError();
            this.destroy();
            return;
        }
        Security.addProvider(new com.sun.net.ssl.internal.ssl.Provider());
        System.setProperty("java.protocol.handler.pkgs", "com.sun.net.ssl.internal.www.protocol");
        System.setProperty("javax.net.debug", "");
        try {
            String tempDir = getTempDirName();
            String tmpFileName = data.getTempFileName();
            data.setLocalFileName(new String(tempDir + FS + tmpFileName));
            File prevFile = new File(data.getLocalFileName());
            if (prevFile.exists()) {
                askWhichToUse();
            } else downloadFile(data);
        } catch (Exception generic) {
            System.out.println(generic.toString());
        } finally {
            try {
                dis.close();
            } catch (Exception ex) {
            }
            try {
                fos.close();
            } catch (Exception exe) {
            }
        }
        return;
    }

    /**
   * This gets called in order to download the file from the server.
   */
    public void downloadFile(FEData data) {
        int temp;
        int c;
        URL url;
        HttpURLConnection urlConn = null;
        DataInputStream dis = null;
        FileOutputStream fos = null;
        try {
            url = new URL(data.getDocumentURL());
            urlConn = (HttpURLConnection) url.openConnection();
            urlConn.setDoInput(true);
            urlConn.setUseCaches(false);
            urlConn.setAllowUserInteraction(false);
            urlConn.setContentHandlerFactory(null);
            dis = new DataInputStream(urlConn.getInputStream());
            fos = new FileOutputStream(data.getLocalFileName(), false);
            while ((c = dis.read()) != -1) fos.write(c);
            fos.close();
            LaunchDoc launch = new LaunchDoc(data);
            launch.start();
            updateScreenInfo();
        } catch (MalformedURLException ex) {
            System.err.println(ex);
        } catch (java.io.IOException iox) {
            System.out.println(iox);
        } catch (Exception generic) {
            System.out.println(generic.toString());
        } finally {
            try {
                dis.close();
            } catch (Exception ex) {
            }
            try {
                fos.close();
            } catch (Exception exe) {
            }
        }
        return;
    }

    public void askWhichToUse() {
        try {
            JSObject win = JSObject.getWindow(data.getApplet());
            String args[] = { data.getParamText("reserveText") };
            Object foo = win.call("changeTitle", args);
        } catch (Exception je) {
        }
        SwingUtilities.invokeLater(new Runnable() {

            public void run() {
                JApplet applet = data.getApplet();
                Container pane = applet.getContentPane();
                GridBagLayout gridbag = new GridBagLayout();
                GridBagConstraints c = new GridBagConstraints();
                c.fill = GridBagConstraints.BOTH;
                pane.setLayout(gridbag);
                pane.setBackground(java.awt.Color.white);
                pane.setForeground(java.awt.Color.black);
                JPanel reUsePane = new JPanel();
                reUsePane.setBackground(java.awt.Color.white);
                reUsePane.setForeground(java.awt.Color.black);
                reUsePane.setSize(400, 200);
                Util.setFont(data, reUsePane, data.PLAIN);
                GridBagLayout gb = new GridBagLayout();
                GridBagConstraints gbc = new GridBagConstraints();
                reUsePane.setLayout(gb);
                gbc.fill = GridBagConstraints.VERTICAL;
                JLabel alreadyLabel = new JLabel(data.getParamText("savedText"));
                c.gridx = 0;
                c.gridy = 0;
                c.insets = new Insets(0, 0, 0, 0);
                c.weightx = 0.5;
                c.weighty = 0.2;
                c.gridheight = 1;
                c.gridwidth = 5;
                c.ipady = 0;
                c.anchor = GridBagConstraints.WEST;
                c.fill = GridBagConstraints.WEST;
                gridbag.setConstraints(alreadyLabel, c);
                Util.setFont(data, alreadyLabel, data.PLAIN);
                pane.add(alreadyLabel);
                JLabel wantLabel = new JLabel("Do you want to:");
                c.gridx = 0;
                c.gridy = 1;
                c.insets = new Insets(0, 0, 0, 0);
                c.weightx = 0.5;
                c.weighty = 0.2;
                c.gridheight = 1;
                c.gridwidth = 5;
                c.ipady = 0;
                c.anchor = GridBagConstraints.WEST;
                c.fill = GridBagConstraints.WEST;
                gridbag.setConstraints(wantLabel, c);
                Util.setFont(data, wantLabel, data.PLAIN);
                pane.add(wantLabel);
                newCopyB = new JRadioButton(data.getParamText("downloadText"));
                gbc.gridx = 0;
                gbc.gridy = 0;
                gbc.gridwidth = 5;
                gbc.insets = new Insets(0, 0, 0, 0);
                gbc.weightx = 0.1;
                gbc.weighty = 0.1;
                gbc.ipady = 0;
                gbc.anchor = GridBagConstraints.WEST;
                gb.setConstraints(newCopyB, gbc);
                newCopyB.setBackground(java.awt.Color.white);
                newCopyB.setForeground(java.awt.Color.black);
                newCopyB.setSelected(true);
                Util.setFont(data, newCopyB, data.PLAIN);
                reUsePane.add(newCopyB);
                useCopyB = new JRadioButton(data.getParamText("useText"));
                gbc.gridx = 0;
                gbc.gridy = 1;
                gbc.insets = new Insets(0, 0, 0, 0);
                gbc.weightx = 0.1;
                gbc.weighty = 0.1;
                gbc.ipady = 0;
                gbc.anchor = GridBagConstraints.WEST;
                gb.setConstraints(useCopyB, gbc);
                useCopyB.setBackground(java.awt.Color.white);
                useCopyB.setForeground(java.awt.Color.black);
                Util.setFont(data, useCopyB, data.PLAIN);
                reUsePane.add(useCopyB);
                ButtonGroup group = new ButtonGroup();
                group.add(newCopyB);
                group.add(useCopyB);
                c.gridx = 0;
                c.gridy = 2;
                c.insets = new Insets(0, 0, 0, 0);
                c.weightx = 0.5;
                c.weighty = 0.5;
                c.gridheight = 1;
                c.gridwidth = 5;
                c.ipady = 0;
                c.anchor = GridBagConstraints.WEST;
                c.fill = GridBagConstraints.WEST;
                gridbag.setConstraints(reUsePane, c);
                pane.add(reUsePane);
                JPanel buttonPane = new JPanel();
                buttonPane.setBackground(java.awt.Color.white);
                buttonPane.setForeground(java.awt.Color.black);
                buttonPane.setSize(400, 200);
                Util.setFont(data, buttonPane, data.BOLD);
                buttonPane.setLayout(gb);
                gbc.fill = GridBagConstraints.VERTICAL;
                JButton okB = new JButton(data.getParamText("okButtonText"));
                gbc.gridx = 0;
                gbc.gridy = 0;
                gbc.weightx = 0.1;
                gbc.weighty = 0.1;
                gbc.gridheight = 1;
                gbc.gridwidth = 1;
                gbc.ipady = 0;
                gbc.insets = new Insets(30, 30, 30, 30);
                gbc.fill = GridBagConstraints.HORIZONTAL;
                gbc.anchor = GridBagConstraints.WEST;
                gridbag.setConstraints(okB, gbc);
                okB.setForeground(java.awt.Color.black);
                Util.setFont(data, okB, data.BOLD);
                buttonPane.add(okB);
                JButton cancelB = new JButton(data.getParamText("cancelButtonText"));
                gbc.gridx = 1;
                gbc.gridy = 0;
                gbc.weightx = 0.1;
                gbc.weighty = 0.1;
                gbc.gridheight = 1;
                gbc.gridwidth = 1;
                gbc.ipady = 0;
                gridbag.setConstraints(cancelB, gbc);
                cancelB.setForeground(java.awt.Color.black);
                Util.setFont(data, cancelB, data.BOLD);
                buttonPane.add(cancelB);
                JButton helpB = new JButton(data.getParamText("helpButtonText"));
                gbc.gridx = 2;
                gbc.gridy = 0;
                gbc.weightx = 0.1;
                gbc.weighty = 0.1;
                gbc.gridheight = 1;
                gbc.gridwidth = 1;
                gbc.ipady = 0;
                gridbag.setConstraints(helpB, gbc);
                helpB.setForeground(java.awt.Color.black);
                Util.setFont(data, helpB, data.BOLD);
                buttonPane.add(helpB);
                c.gridx = 0;
                c.gridy = 3;
                c.insets = new Insets(30, 30, 30, 30);
                c.weightx = 0.5;
                c.weighty = 0.2;
                c.gridheight = 1;
                c.gridwidth = 3;
                c.ipady = 0;
                c.anchor = GridBagConstraints.WEST;
                c.fill = GridBagConstraints.WEST;
                gridbag.setConstraints(buttonPane, c);
                pane.add(buttonPane);
                applet.validate();
                applet.repaint();
                okB.addActionListener(new java.awt.event.ActionListener() {

                    public void actionPerformed(ActionEvent e) {
                        if (useCopyB.isSelected()) {
                            LaunchDoc launch = new LaunchDoc(data);
                            launch.start();
                            updateScreenInfo();
                        } else downloadFile(data);
                    }
                });
                cancelB.addActionListener(new java.awt.event.ActionListener() {

                    public void actionPerformed(ActionEvent e) {
                        data.setUploadFlag(false);
                        data.setUnlockFlag(true);
                        data.setSavePrevFlag(false);
                        final_actionPerformed(e, data);
                    }
                });
                helpB.addActionListener(new java.awt.event.ActionListener() {

                    public void actionPerformed(ActionEvent e) {
                        try {
                            JSObject win = JSObject.getWindow(data.getApplet());
                            String args[] = { "Help is here" };
                            Object foo = win.call("showHelp", args);
                        } catch (Exception je) {
                        }
                    }
                });
            }
        });
    }

    public void displayJavaVersionError() {
        SwingUtilities.invokeLater(new Runnable() {

            public void run() {
                JApplet applet = data.getApplet();
                Container pane = applet.getContentPane();
                String javaVersion = System.getProperty("java.version");
                Object[] options = { "Download the 1.4 Plugin", "Don't use this applet" };
                int n = JOptionPane.showOptionDialog(pane, "This applet requires Java 1.4. Your browser is using version:  " + javaVersion + "Would you like to download the 1.4 Plugin?", "Download the Plugin?", JOptionPane.YES_NO_OPTION, JOptionPane.QUESTION_MESSAGE, null, options, options[0]);
                try {
                    if (n == JOptionPane.YES_OPTION) data.getApplet().getAppletContext().showDocument(new URL("http://java.sun.com/getjava/download.html"), "_self"); else data.getApplet().getAppletContext().showDocument(new URL(data.getAbandonURL()), "_self");
                } catch (Exception u) {
                }
            }
        });
    }

    public String getTempDirName() {
        String tmpdir = System.getProperty("java.io.tmpdir");
        if (tmpdir != null) return tmpdir;
        return ("./");
    }

    public String ok() {
        return "OK";
    }

    public void updateScreenInfo() {
        try {
            JSObject win = JSObject.getWindow(data.getApplet());
            String args[] = { data.getParamText("editingText") + " " + data.getOrigFileName() };
            Object foo = win.call("changeTitle", args);
        } catch (Exception je) {
        }
        SwingUtilities.invokeLater(new Runnable() {

            public void run() {
                JApplet applet = data.getApplet();
                Container pane = applet.getContentPane();
                pane.removeAll();
                GridBagLayout gridbag = new GridBagLayout();
                GridBagConstraints c = new GridBagConstraints();
                c.fill = GridBagConstraints.HORIZONTAL;
                pane.setLayout(gridbag);
                pane.setBackground(java.awt.Color.white);
                pane.setForeground(java.awt.Color.black);
                pane.setSize(400, 200);
                JPanel updateInfoPane = new JPanel();
                updateInfoPane.setBackground(java.awt.Color.white);
                updateInfoPane.setForeground(java.awt.Color.red);
                updateInfoPane.setSize(400, 200);
                Util.setFont(data, updateInfoPane, data.PLAIN);
                GridBagLayout gb = new GridBagLayout();
                GridBagConstraints gbc = new GridBagConstraints();
                updateInfoPane.setLayout(gb);
                gbc.fill = GridBagConstraints.HORIZONTAL;
                String newInfo = new String(data.getParamText("cautionText"));
                JLabel infoLabel = new JLabel(newInfo);
                infoLabel.setSize(400, 200);
                c.gridx = 0;
                c.gridy = 0;
                c.insets = new Insets(0, 0, 0, 0);
                c.weightx = 0.5;
                c.weighty = 0.2;
                c.gridheight = 1;
                c.gridwidth = 5;
                c.ipady = 0;
                c.anchor = GridBagConstraints.WEST;
                c.fill = GridBagConstraints.WEST;
                gridbag.setConstraints(infoLabel, c);
                Util.setFont(data, infoLabel, data.PLAIN);
                pane.add(infoLabel);
                applet.validate();
                applet.repaint();
            }
        });
    }

    public boolean final_actionPerformed(ActionEvent e, FEData data) {
        Util.progressBar(data, data.getParamText("processingText"));
        PostFileConnection poster = new PostFileConnection(data);
        return true;
    }
}
