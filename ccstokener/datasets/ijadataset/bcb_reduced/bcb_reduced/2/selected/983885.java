package org.mmt.gui;

import java.awt.BorderLayout;
import java.awt.Cursor;
import java.awt.Desktop;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.MouseEvent;
import java.awt.event.MouseListener;
import java.io.BufferedReader;
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.PrintWriter;
import java.net.URI;
import java.net.URL;
import javax.swing.Icon;
import javax.swing.ImageIcon;
import javax.swing.JDialog;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JMenu;
import javax.swing.JMenuBar;
import javax.swing.JMenuItem;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JTabbedPane;
import javax.swing.UIManager;
import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.transform.OutputKeys;
import javax.xml.transform.Transformer;
import javax.xml.transform.TransformerFactory;
import javax.xml.transform.dom.DOMSource;
import javax.xml.transform.stream.StreamResult;
import org.mmt.core.DataRecorder;
import org.mmt.gui.charts.ChartsPanel;
import org.mmt.gui.home.HomePanel;
import org.mmt.gui.processlist.ProcessListPanel;
import org.mmt.gui.recording.RecordingPanel;
import org.mmt.gui.servervariables.ServerVariablesPanel;
import org.mmt.gui.servervariables.ServerVariablesTableModel;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;

public class Application extends JFrame implements ActionListener, MouseListener {

    /**
	 * 
	 */
    private static final long serialVersionUID = 1L;

    private static final String propertyFile = "config/property.xml";

    private static final String version = "1.2.1";

    private JTabbedPane mainPanel;

    public static String errorMessage = "<html><center>The server is not properly connected. Please check the home tab.</center></html>";

    private JMenuItem configureMenuItem = null;

    private JMenuItem aboutMenuItem = null;

    private JMenuItem exitMenuItem = null;

    private JMenuItem checkForNewVersionMenuItem = null;

    private String[] newVersionData = null;

    private JLabel statusLabel = null;

    static {
        synchronized (Application.class) {
            File serverFile = new File(propertyFile);
            if (!serverFile.exists()) {
                FileWriter outFile = null;
                PrintWriter out = null;
                try {
                    outFile = new FileWriter(serverFile);
                    out = new PrintWriter(outFile);
                    out.println("<?xml version=\"1.0\" encoding=\"UTF-8\"?>");
                    out.println();
                    out.print("<properties />");
                    out.close();
                } catch (IOException e) {
                    Application.showError(e.getMessage());
                } finally {
                    try {
                        if (out != null) out.close();
                        if (outFile != null) outFile.close();
                    } catch (IOException e) {
                        Application.showError(e.getMessage());
                    }
                }
            }
        }
    }

    public Application() {
        super();
        try {
            UIManager.setLookAndFeel(UIManager.getSystemLookAndFeelClassName());
        } catch (Exception e) {
            showError(e.getMessage());
        }
        setIconImage(new ImageIcon(ClassLoader.getSystemResource("resources/icon.gif")).getImage());
        setSize(800, 600);
        setLocation(0, 0);
        setTitle("Mysql Monitoring Tool");
        setExtendedState(getExtendedState() | JFrame.MAXIMIZED_BOTH);
        setupMenu();
        setDefaultCloseOperation(EXIT_ON_CLOSE);
        setVisible(true);
        ServerGroupChooserDialog sgcd = new ServerGroupChooserDialog();
        String group = sgcd.getSelectedServerGroup();
        if (group == null) System.exit(0);
        JDialog dialog = new JDialog();
        dialog.setTitle("Loading...");
        Icon icon = new ImageIcon(ClassLoader.getSystemResource("resources/loading.png"));
        dialog.getContentPane().add(new JLabel(icon));
        dialog.setSize(450, 500);
        dialog.setLocationRelativeTo(null);
        dialog.setUndecorated(true);
        dialog.setVisible(true);
        DataRecorder.getInstance().setServerGroup(group);
        mainPanel = new JTabbedPane(JTabbedPane.LEFT);
        mainPanel.add("Home", new HomePanel(group));
        mainPanel.add("ProcessList", new ProcessListPanel(group));
        mainPanel.add("Charts", new ChartsPanel(group));
        mainPanel.add("Status Vars", new ServerVariablesPanel(group, ServerVariablesTableModel.STATUS_VARIABLES));
        mainPanel.add("Server Vars", new ServerVariablesPanel(group, ServerVariablesTableModel.SERVER_VARIABLES));
        mainPanel.add("Recording", new RecordingPanel(group));
        JPanel content = new JPanel();
        content.setLayout(new BorderLayout());
        content.add(mainPanel, BorderLayout.CENTER);
        newVersionData = doVersionCheck();
        if (newVersionData != null) {
            statusLabel = new JLabel("<html><center><b>Mysql Monitor Tool version " + newVersionData[0] + " released. <a href=''>Click here to download it</a></b></center></html>");
            JPanel labelPanel = new JPanel();
            labelPanel.add(statusLabel);
            statusLabel.addMouseListener(this);
            content.add(labelPanel, BorderLayout.SOUTH);
        }
        add(content);
        validate();
        dialog.setVisible(false);
    }

    private void setupMenu() {
        JMenuBar bar = new JMenuBar();
        JMenu menu = new JMenu("Mysql Monitor Tool");
        configureMenuItem = new JMenuItem("Configure servers");
        configureMenuItem.addActionListener(this);
        menu.add(configureMenuItem);
        menu.addSeparator();
        checkForNewVersionMenuItem = new JMenuItem("Check for new version");
        checkForNewVersionMenuItem.addActionListener(this);
        menu.add(checkForNewVersionMenuItem);
        aboutMenuItem = new JMenuItem("About");
        aboutMenuItem.addActionListener(this);
        menu.add(aboutMenuItem);
        menu.addSeparator();
        exitMenuItem = new JMenuItem("Exit");
        exitMenuItem.addActionListener(this);
        menu.add(exitMenuItem);
        bar.add(menu);
        setJMenuBar(bar);
    }

    public static void showError(String message) {
        JOptionPane.showMessageDialog(null, message, "Error", JOptionPane.ERROR_MESSAGE);
    }

    public static void main(String[] args) {
        new Application();
    }

    @Override
    public void actionPerformed(ActionEvent e) {
        if (e.getSource() == configureMenuItem) {
            ServerManagementDialog smd = new ServerManagementDialog();
            smd.setVisible(true);
        } else if (e.getSource() == aboutMenuItem) {
            new AboutDialog().setVisible(true);
        } else if (e.getSource() == exitMenuItem) {
            System.exit(0);
        } else if (e.getSource() == checkForNewVersionMenuItem) {
            checkNewVersion();
        }
    }

    public static String getVersion() {
        return version;
    }

    public static void checkNewVersion() {
        String[] data = doVersionCheck();
        if (data != null) {
            String newVersion = data[0];
            String downloadUrl = data[1];
            int result = JOptionPane.showConfirmDialog(null, "New version " + newVersion + " released. Do you want to download it now?", "New version released", JOptionPane.YES_NO_OPTION, JOptionPane.QUESTION_MESSAGE);
            if (result == JOptionPane.YES_OPTION) {
                try {
                    Desktop.getDesktop().browse(new URI(downloadUrl));
                } catch (Exception e) {
                    Application.showError("Error downloading new version: " + e.getMessage());
                }
            }
        } else JOptionPane.showMessageDialog(null, "No new version available.");
    }

    private static String[] doVersionCheck() {
        URL url = null;
        InputStreamReader isr = null;
        BufferedReader br = null;
        try {
            url = new URL("http://mysqlmt.svn.sourceforge.net/viewvc/mysqlmt/MysqlMonitorTool/version");
            isr = new InputStreamReader(url.openStream());
            br = new BufferedReader(isr);
            String newVersion = br.readLine();
            String downloadUrl = br.readLine();
            if (compareVersions(getVersion(), newVersion)) {
                return new String[] { newVersion, downloadUrl };
            }
        } catch (Exception e) {
            e.printStackTrace();
        } finally {
            try {
                if (br != null) br.close();
                if (isr != null) isr.close();
            } catch (Exception e) {
                e.printStackTrace();
            }
        }
        return null;
    }

    private static boolean compareVersions(String current, String released) {
        if (current == null || released == null) return false;
        String[] currentParts = current.split("\\.");
        String[] releasedParts = released.split("\\.");
        if (currentParts.length != 3 || releasedParts.length != 3) return false;
        int[] currentInts = new int[3];
        int[] releasedInts = new int[3];
        try {
            for (int i = 0; i < 3; i++) {
                currentInts[i] = Integer.parseInt(currentParts[i]);
                releasedInts[i] = Integer.parseInt(releasedParts[i]);
            }
        } catch (NumberFormatException e) {
            e.printStackTrace();
            return false;
        }
        if (currentInts[0] < releasedInts[0]) return true;
        if (currentInts[0] > releasedInts[0]) return false;
        if (currentInts[1] < releasedInts[1]) return true;
        if (currentInts[1] > releasedInts[1]) return false;
        if (currentInts[2] < releasedInts[2]) return true;
        return false;
    }

    @Override
    public void mouseClicked(MouseEvent e) {
        if (e.getSource() == statusLabel) {
            if (newVersionData != null) {
                try {
                    Desktop.getDesktop().browse(new URI(newVersionData[1]));
                } catch (Exception ex) {
                    Application.showError("Unable to download new version: " + ex.getMessage());
                }
            }
        }
    }

    @Override
    public void mouseEntered(MouseEvent e) {
        if (e.getSource() == statusLabel) setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
    }

    @Override
    public void mouseExited(MouseEvent e) {
        if (e.getSource() == statusLabel) setCursor(Cursor.getDefaultCursor());
    }

    @Override
    public void mousePressed(MouseEvent e) {
    }

    @Override
    public void mouseReleased(MouseEvent e) {
    }

    public static String getProperty(String key, String defaultReturn) throws Exception {
        DocumentBuilderFactory docFactory = DocumentBuilderFactory.newInstance();
        DocumentBuilder docBuilder = docFactory.newDocumentBuilder();
        Document doc = docBuilder.parse(propertyFile);
        NodeList nodes = doc.getElementsByTagName("property");
        for (int i = 0; i < nodes.getLength(); i++) {
            Node n = nodes.item(i);
            if (!n.getAttributes().getNamedItem("name").getTextContent().equals(key)) continue;
            return n.getTextContent();
        }
        return defaultReturn;
    }

    public static void setProperty(String key, String value) throws Exception {
        DocumentBuilderFactory docFactory = DocumentBuilderFactory.newInstance();
        DocumentBuilder docBuilder = docFactory.newDocumentBuilder();
        Document doc = docBuilder.parse(propertyFile);
        NodeList nodes = doc.getElementsByTagName("property");
        boolean found = false;
        for (int i = 0; i < nodes.getLength(); i++) {
            Node n = nodes.item(i);
            if (!n.getAttributes().getNamedItem("name").getTextContent().equals(key)) continue;
            n.setTextContent(value);
            found = true;
            break;
        }
        if (!found) {
            Element el = doc.createElement("property");
            el.setAttribute("name", key);
            el.setTextContent(value);
            doc.getFirstChild().appendChild(el);
        }
        Transformer transformer = TransformerFactory.newInstance().newTransformer();
        transformer.setOutputProperty(OutputKeys.INDENT, "yes");
        StreamResult result = new StreamResult(new FileWriter(propertyFile));
        DOMSource source = new DOMSource(doc);
        transformer.transform(source, result);
        result.getWriter().flush();
    }
}
