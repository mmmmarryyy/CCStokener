package org.iwidget.desktop.ui;

import org.iwidget.desktop.core.*;
import java.awt.*;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.io.*;
import java.net.URL;
import java.net.URLConnection;
import java.util.*;
import javax.swing.*;

/**
 *
 * @author Muhammad Hakim A
 */
public class UpdateFrame extends JDialog implements ActionListener {

    public UpdateFrame(Iwidget iwidget, Properties widgetVersions) {
        this.iwidget = iwidget;
        this.widgetVersions = widgetVersions;
        setTitle("iWidget Updater");
        setSize(420, 400);
        setLocation(100, 100);
        getContentPane().setLayout(new BorderLayout());
        status = new JLabel("  Checking for updates...");
        getContentPane().add(status, "North");
        JPanel p = new JPanel();
        p.setLayout(new BorderLayout());
        JPanel p2 = new JPanel();
        p2.setLayout(new BorderLayout());
        p.add(p2, "West");
        updatePanel = new JPanel();
        updatePanel.setLayout(new BoxLayout(updatePanel, 3));
        p2.add(updatePanel, "North");
        scroller = new JScrollPane();
        scroller.setVerticalScrollBarPolicy(20);
        scroller.setHorizontalScrollBarPolicy(30);
        scroller.getViewport().add(p);
        scroller.setAutoscrolls(true);
        getContentPane().add(scroller, "Center");
        siteUpdates = null;
        buildUpdates();
        setVisible(true);
    }

    private void buildUpdates() {
        if (siteUpdates == null) {
            siteUpdates = new Properties();
            String updateData = fetchURL("http://code.google.com/p/iwidget/updates_" + Iwidget.IWIDGET_VERSION_MAJOR + "_" + Iwidget.IWIDGET_VERSION_MINOR + ".txt");
            if (updateData != null) try {
                siteUpdates.load(new StringBufferInputStream(updateData));
            } catch (Exception e) {
            }
        }
        if (siteUpdates != null) {
            status.setText("  Comparing versions...");
            updatePanel.removeAll();
            int updateCount = 0;
            widgetVersions.put("NewIwidget", Iwidget.IWIDGET_VERSION_MAJOR + "." + Iwidget.IWIDGET_VERSION_MINOR + "." + Iwidget.IWIDGET_VERSION_BUILD);
            Enumeration enum1 = widgetVersions.keys();
            do {
                if (!enum1.hasMoreElements()) break;
                String widgetName = (String) enum1.nextElement();
                String version = widgetVersions.getProperty(widgetName, "1.0.0");
                String siteData = siteUpdates.getProperty(Iwidget.replace(widgetName, " ", "_", 0), null);
                if (siteData != null) {
                    StringTokenizer tokens = new StringTokenizer(siteData, "|");
                    String siteVersion = (String) tokens.nextElement();
                    String siteURL = (String) tokens.nextElement();
                    String siteFile = (String) tokens.nextElement();
                    String siteRestart = null;
                    try {
                        siteRestart = (String) tokens.nextElement();
                    } catch (Exception e) {
                    }
                    tokens = new StringTokenizer(version, ".");
                    int myMajor = Integer.parseInt((String) tokens.nextElement());
                    int myMinor = Integer.parseInt((String) tokens.nextElement());
                    int myBuild = Integer.parseInt((String) tokens.nextElement());
                    tokens = new StringTokenizer(siteVersion, ".");
                    int siteMajor = Integer.parseInt((String) tokens.nextElement());
                    int siteMinor = Integer.parseInt((String) tokens.nextElement());
                    int siteBuild = Integer.parseInt((String) tokens.nextElement());
                    if (siteMajor > myMajor || siteMinor > myMinor || siteBuild > myBuild) {
                        JPanel p = new JPanel();
                        p.setLayout(new FlowLayout(0));
                        JButton update = new JButton("Update");
                        update.setPreferredSize(new Dimension(80, 25));
                        update.setActionCommand(widgetName + "|" + siteURL + "|" + siteFile + "|" + siteVersion);
                        update.addActionListener(this);
                        p.add(update);
                        JLabel label = new JLabel();
                        String text = widgetName + " for " + version + " to " + siteVersion;
                        if (widgetName.equals("NewIwidget")) text = "New Version of Iwidget available";
                        if (siteRestart != null) text = text + "  (Iwidget restart required)";
                        label.setText(text);
                        p.add(label);
                        updateCount++;
                        updatePanel.add(p);
                    }
                }
            } while (true);
            if (updateCount > 0) status.setText("  Update list complete"); else status.setText("  No more updates available");
            repaint();
        }
    }

    public void actionPerformed(ActionEvent e) {
        StringTokenizer tokens = new StringTokenizer(e.getActionCommand(), "|");
        String updateName = (String) tokens.nextElement();
        String updateURL = (String) tokens.nextElement();
        String updateFile = (String) tokens.nextElement();
        String updateVersion = (String) tokens.nextElement();
        if (updateName.equals("NewIwidget")) {
            IwidgetMethods.openURL("http://code.google.com/p/iwidget/downloads.html");
        } else {
            status.setText("Updating " + updateName + "...");
            status.repaint();
            if (fetchURL2File(updateName, updateURL, updateFile)) {
                status.setText("Updating " + updateName + " completed successfully");
                status.repaint();
                widgetVersions.put(Iwidget.replace(updateName, " ", "_", 0), updateVersion);
                iwidget.closeWidget(updateName);
                if (updateFile.startsWith("/library/")) IwidgetRepository.getInstance().clearCache();
                widgetVersions.put(updateName, updateVersion);
                buildUpdates();
            } else {
                status.setText("Updating " + updateName + " failed");
                status.repaint();
            }
        }
    }

    public boolean fetchURL2File(String updateName, String urlString, String updateFile) {
        boolean result = true;
        try {
            URL url = new URL("http://code.google.com/p/iwidget/" + urlString);
            URLConnection connection = url.openConnection();
            connection.setDoInput(true);
            connection.setDoOutput(true);
            connection.setUseCaches(false);
            connection.setRequestProperty("Content-Type", "application/x-www-form-urlencoded");
            byte buffer[] = new byte[8192];
            InputStream is = connection.getInputStream();
            int iCtr = is.read(buffer);
            int iTotal = iCtr;
            String testResult = new String(buffer);
            if (testResult.startsWith("<html><head>")) {
                result = false;
            } else {
                FileOutputStream fos = new FileOutputStream(System.getProperty("user.dir") + updateFile);
                while (iCtr > -1) {
                    status.setText("Updating " + updateName + " bytes transferred:" + iTotal);
                    fos.write(buffer, 0, iCtr);
                    iCtr = is.read(buffer);
                    iTotal += iCtr;
                }
                fos.close();
            }
            is.close();
        } catch (Exception e) {
            e.printStackTrace();
            result = false;
        }
        return result;
    }

    public String fetchURL(String urlString) {
        try {
            StringBuffer sb;
            sb = new StringBuffer();
            BufferedReader br = postServerCommand(urlString);
            if (br == null) return "";
            for (String line = br.readLine(); line != null; line = br.readLine()) sb.append(line).append("\n");
            br.close();
            return sb.toString();
        } catch (Exception e) {
            e.printStackTrace();
            return "";
        }
    }

    public BufferedReader postServerCommand(String serverURL) {
        try {
            BufferedReader dI;
            URL url = new URL(serverURL);
            URLConnection connection = url.openConnection();
            connection.setDoInput(true);
            connection.setDoOutput(true);
            connection.setUseCaches(false);
            connection.setRequestProperty("Content-Type", "application/x-www-form-urlencoded");
            dI = new BufferedReader(new InputStreamReader(connection.getInputStream()));
            return dI;
        } catch (Exception e) {
            return null;
        }
    }

    private static final long serialVersionUID = 0x2d34383231373532L;

    private Hashtable widgetList;

    JPanel updatePanel;

    JScrollPane scroller;

    JLabel status;

    Properties siteUpdates;

    Properties widgetVersions;

    Iwidget iwidget;
}
