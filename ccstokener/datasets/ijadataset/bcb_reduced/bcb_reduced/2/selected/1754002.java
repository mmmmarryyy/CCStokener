package JWoWSystem;

import java.awt.Color;
import java.awt.Dimension;
import java.awt.Font;
import java.awt.Point;
import java.awt.Rectangle;
import java.awt.Toolkit;
import java.io.BufferedInputStream;
import java.io.BufferedOutputStream;
import java.io.BufferedReader;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStreamWriter;
import java.net.MalformedURLException;
import java.net.URL;
import java.net.URLConnection;
import java.net.URLEncoder;
import java.text.DateFormat;
import java.util.Date;
import java.util.Enumeration;
import java.util.Iterator;
import java.util.List;
import java.util.Locale;
import java.util.zip.ZipEntry;
import java.util.zip.ZipFile;
import javax.swing.ActionMap;
import javax.swing.BorderFactory;
import javax.swing.ImageIcon;
import javax.swing.JButton;
import javax.swing.JCheckBox;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JProgressBar;
import javax.swing.JScrollPane;
import javax.swing.JTabbedPane;
import javax.swing.JTable;
import javax.swing.JTextField;
import javax.swing.JTextPane;
import javax.swing.JToggleButton;
import javax.swing.SwingConstants;
import javax.swing.UIManager;
import javax.swing.table.DefaultTableModel;
import javax.swing.text.DefaultCaret;
import javax.swing.text.html.HTMLDocument;
import javax.swing.text.html.HTMLEditorKit;
import org.jdom.Document;
import org.jdom.Element;
import org.jdom.JDOMException;
import org.jdom.input.SAXBuilder;
import org.jdom.output.Format;
import org.jdom.output.XMLOutputter;

public class launcher extends JFrame {

    private static final long serialVersionUID = 5409074042624983905L;

    private static final String localXMLFile = "default.xml";

    private static final String serverURL = "http://launcher.wowsystem.bogala.org/";

    private static final String localAVersion = "1.0";

    private static boolean verifXML = false;

    private static Element RootXMLTotal = null;

    private static Element rootXMLNews = null;

    private static String newsXMLFile = "news_en.xml";

    private JTabbedPane tabControl = null;

    private int currentRow = 0;

    private JPanel newsPanel = new JPanel();

    private JPanel rosterPanel = new JPanel();

    private JPanel modsPanel = new JPanel();

    private JPanel aboutPanel = new JPanel();

    private JLabel jLabel1 = null;

    private JLabel jLabel2 = new JLabel();

    private JLabel newsLabel1 = null;

    private JTextPane textNews1 = null;

    private JLabel newsLabel2 = null;

    private JTextPane textNews2 = null;

    private JLabel newsLabel3 = null;

    private JTextPane textNews3 = null;

    private JLabel urlRosterLabel = null;

    private JTextField urlRosterValue = null;

    private JButton RosterMajButton = null;

    private JCheckBox rosterCheckUpdate = null;

    private DefaultTableModel tmn = null;

    private DefaultTableModel tModelMods = null;

    private JButton modsUpdateButton = null;

    private JToggleButton modsInstallButton = null;

    private JPanel formPanel = null;

    private JLabel lLoading = new JLabel();

    private JProgressBar pbLoading = new JProgressBar();

    private JButton wowLaunch = null;

    private JButton wowWeb = null;

    private JButton wsWeb = null;

    private JButton langEn = null;

    private JScrollPane rosterScrollProfiles = null;

    private JTable rosterProfiles = null;

    private JToggleButton modsAUButton = null;

    private JScrollPane modsListScrollPane = null;

    private JTable modsListTable = null;

    private JLabel versionningLabel = null;

    public launcher() {
        try {
            jbInit();
            Dimension screenSize = Toolkit.getDefaultToolkit().getScreenSize();
            Dimension frameSize = this.getSize();
            if (frameSize.height > screenSize.height) {
                frameSize.height = screenSize.height;
            }
            if (frameSize.width > screenSize.width) {
                frameSize.width = screenSize.width;
            }
            this.setLocation((screenSize.width - frameSize.width) / 2, (screenSize.height - frameSize.height) / 2);
            this.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
            this.setVisible(true);
            lLoading.setText("downloading news...");
            newsXMLFile = downloadFile("http://wowsystem.bogala.org/news_en.xml");
            rootXMLNews = OpenSetRootXMLFile(newsXMLFile);
            afficheNews();
            lLoading.setText("news downloaded !");
            String nomFichier = "jgetTotalXML_en.xml";
            File fichier = new File(nomFichier);
            if (fichier.exists()) {
                fichier.delete();
            }
            downloadFile(serverURL + nomFichier);
            File fichierLocal = new File(localXMLFile);
            if (!fichierLocal.exists()) {
                fichier.renameTo(fichierLocal);
            } else {
            }
            RootXMLTotal = OpenSetRootXMLFile(localXMLFile);
            lLoading.setText("Inititalisation AboutBox...");
            initAboutBox();
            initRosterTab();
            addOnsGetList();
            lLoading.setText("Done.");
            pbLoading.setMinimum(0);
            pbLoading.setMaximum(100);
            pbLoading.setValue(100);
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    private void jbInit() throws Exception {
        this.getContentPane().setLayout(null);
        versionningLabel = new JLabel();
        versionningLabel.setBounds(new java.awt.Rectangle(1, 90, 508, 15));
        versionningLabel.setHorizontalAlignment(javax.swing.SwingConstants.CENTER);
        versionningLabel.setText("Last Release version on Server : 1.0 - Beta not available");
        urlRosterLabel = new JLabel();
        urlRosterLabel.setBounds(new java.awt.Rectangle(4, 3, 110, 22));
        urlRosterLabel.setText("URL :");
        this.setSize(new Dimension(532, 372));
        this.setBackground(new Color(230, 235, 245));
        this.setTitle("JWoWSystem 1.0");
        this.setResizable(false);
        this.addWindowListener(new java.awt.event.WindowAdapter() {

            public void windowClosing(java.awt.event.WindowEvent e) {
                System.out.println("windowClosing()");
                saveData();
            }
        });
        formPanel = new JPanel();
        formPanel.setLayout(null);
        formPanel.setBounds(new java.awt.Rectangle(0, 0, 526, 340));
        formPanel.setName("formPanel");
        this.getContentPane().add(formPanel, null);
        tabControl = new JTabbedPane();
        formPanel.add(tabControl, null);
        formPanel.add(lLoading, null);
        formPanel.add(pbLoading, null);
        formPanel.add(getWowLaunch(), null);
        formPanel.add(getWowWeb(), null);
        formPanel.add(getWsWeb(), null);
        formPanel.add(getLangEn(), null);
        lLoading.setText("Loading...");
        lLoading.setBounds(new Rectangle(5, 255, 515, 15));
        pbLoading.setBounds(new Rectangle(5, 270, 515, 15));
        newsLabel2 = new JLabel();
        newsLabel2.setBounds(new java.awt.Rectangle(6, 72, 500, 21));
        newsLabel2.setBackground(this.getBackground());
        newsLabel2.setText("");
        newsLabel1 = new JLabel();
        newsLabel1.setBounds(new java.awt.Rectangle(6, 2, 500, 21));
        newsLabel1.setBackground(this.getBackground());
        newsLabel1.setText("");
        tabControl.setBounds(new java.awt.Rectangle(5, 5, 515, 250));
        newsPanel.setLayout(null);
        newsPanel.setToolTipText("null");
        newsPanel.setAutoscrolls(true);
        newsPanel.add(newsLabel1, null);
        newsPanel.add(getTextNews1(), null);
        newsPanel.add(newsLabel2, null);
        newsPanel.add(getTextNews2(), null);
        newsPanel.add(getNewsLabel3(), null);
        newsPanel.add(getTextNews3(), null);
        rosterPanel.setLayout(null);
        rosterPanel.add(urlRosterLabel, null);
        rosterPanel.add(getUrlRosterValue(), null);
        rosterPanel.add(getRosterMajButton(), null);
        rosterPanel.add(getRosterCheckUpdate(), null);
        rosterPanel.add(getRosterScrollProfiles(), null);
        modsPanel.setLayout(null);
        modsPanel.add(getModsUpdateButton(), null);
        modsPanel.add(getModsInstallButton(), null);
        modsPanel.add(getModsAUButton(), null);
        modsPanel.add(getModsListScrollPane(), null);
        aboutPanel.setLayout(null);
        jLabel1 = new JLabel();
        jLabel1.setIcon(new ImageIcon(getClass().getResource("/JWoWSystem/wowsystem2.png")));
        jLabel1.setHorizontalTextPosition(SwingConstants.CENTER);
        jLabel1.setHorizontalAlignment(SwingConstants.CENTER);
        jLabel1.setBounds(new Rectangle(0, 0, 510, 75));
        tabControl.addTab("News", newsPanel);
        tabControl.addTab("Roster", rosterPanel);
        tabControl.addTab("Mods", modsPanel);
        aboutPanel.add(jLabel2, null);
        aboutPanel.add(jLabel1, null);
        aboutPanel.add(versionningLabel, null);
        tabControl.addTab("About...", aboutPanel);
        jLabel2.setText("WoWSystem, Java Edition 1.0");
        jLabel2.setHorizontalAlignment(SwingConstants.CENTER);
        jLabel2.setFont(new Font("Dialog", 1, 14));
        jLabel2.setBounds(new Rectangle(0, 75, 510, 15));
    }

    private JLabel getNewsLabel3() {
        if (newsLabel3 == null) {
            newsLabel3 = new JLabel();
            newsLabel3.setBounds(new java.awt.Rectangle(6, 140, 500, 21));
            newsLabel3.setBackground(this.getBackground());
            newsLabel3.setText("");
        }
        return newsLabel3;
    }

    /**
	 * This method initializes textNews1
	 * 
	 * @return javax.swing.JTextPane
	 */
    private JTextPane getTextNews1() {
        if (textNews1 == null) {
            textNews1 = new JTextPane();
            textNews1.setBounds(new java.awt.Rectangle(50, 23, 457, 50));
            textNews1.setBackground(this.getBackground());
            textNews1.setEditorKit(new HTMLEditorKit());
            textNews1.setDocument(new HTMLDocument());
            textNews1.setText("");
            textNews1.setActionMap(new ActionMap());
            textNews1.setBorder(BorderFactory.createEmptyBorder(0, 0, 0, 0));
            textNews1.setCaret(new DefaultCaret());
            textNews1.setEditable(false);
            textNews1.setDoubleBuffered(true);
            textNews1.setInheritsPopupMenu(true);
            textNews1.setToolTipText("");
        }
        return textNews1;
    }

    /**
	 * This method initializes textNews2
	 * 
	 * @return javax.swing.JTextPane
	 */
    private JTextPane getTextNews2() {
        if (textNews2 == null) {
            textNews2 = new JTextPane();
            textNews2.setBounds(new java.awt.Rectangle(50, 92, 457, 50));
            textNews2.setBackground(this.getBackground());
            textNews2.setEditorKit(new HTMLEditorKit());
            textNews2.setDocument(new HTMLDocument());
            textNews2.setText("");
            textNews2.setActionMap(new ActionMap());
            textNews2.setBorder(BorderFactory.createEmptyBorder(0, 0, 0, 0));
            textNews2.setCaret(new DefaultCaret());
            textNews2.setEditable(false);
            textNews2.setDoubleBuffered(true);
            textNews2.setInheritsPopupMenu(true);
            textNews2.setToolTipText("");
        }
        return textNews2;
    }

    /**
	 * This method initializes textNews3
	 * 
	 * @return javax.swing.JTextPane
	 */
    private JTextPane getTextNews3() {
        if (textNews3 == null) {
            textNews3 = new JTextPane();
            textNews3.setBounds(new java.awt.Rectangle(50, 161, 457, 50));
            textNews3.setBackground(this.getBackground());
            textNews3.setEditorKit(new HTMLEditorKit());
            textNews3.setDocument(new HTMLDocument());
            textNews3.setText("");
            textNews3.setActionMap(new ActionMap());
            textNews3.setBorder(BorderFactory.createEmptyBorder(0, 0, 0, 0));
            textNews3.setCaret(new DefaultCaret());
            textNews3.setEditable(false);
            textNews3.setDoubleBuffered(true);
            textNews3.setInheritsPopupMenu(true);
            textNews3.setToolTipText("null");
        }
        return textNews3;
    }

    /**
	 * This method initializes urlRosterValue
	 * 
	 * @return javax.swing.JTextField
	 */
    private JTextField getUrlRosterValue() {
        if (urlRosterValue == null) {
            urlRosterValue = new JTextField();
            urlRosterValue.setBounds(new java.awt.Rectangle(117, 3, 390, 23));
        }
        return urlRosterValue;
    }

    /**
	 * This method initializes RosterMajButton
	 * 
	 * @return javax.swing.JButton
	 */
    private JButton getRosterMajButton() {
        if (RosterMajButton == null) {
            RosterMajButton = new JButton();
            RosterMajButton.setBounds(new java.awt.Rectangle(2, 191, 504, 29));
            RosterMajButton.setText("Update Changes");
            RosterMajButton.addMouseListener(new java.awt.event.MouseAdapter() {

                public void mouseClicked(java.awt.event.MouseEvent e) {
                    rosterUpdate("url", urlRosterValue.getText());
                    if (rosterCheckUpdate.isSelected()) {
                        rosterUpdate("upload", "yes");
                    } else {
                        rosterUpdate("upload", "no");
                    }
                }
            });
        }
        return RosterMajButton;
    }

    /**
	 * This method initializes rosterCheckUpdate
	 * 
	 * @return javax.swing.JCheckBox
	 */
    private JCheckBox getRosterCheckUpdate() {
        if (rosterCheckUpdate == null) {
            rosterCheckUpdate = new JCheckBox();
            rosterCheckUpdate.setBounds(new java.awt.Rectangle(4, 171, 502, 20));
            rosterCheckUpdate.setText("Send automaticaly?");
            rosterCheckUpdate.setSelected(true);
        }
        return rosterCheckUpdate;
    }

    /**
	 * This method initializes modsUpdateButton
	 * 
	 * @return javax.swing.JButton
	 */
    private JButton getModsUpdateButton() {
        if (modsUpdateButton == null) {
            modsUpdateButton = new JButton();
            modsUpdateButton.setBounds(new java.awt.Rectangle(271, 183, 234, 33));
            modsUpdateButton.setText("Add to Archive");
        }
        return modsUpdateButton;
    }

    /**
	 * This method initializes modsInstallButton
	 * 
	 * @return javax.swing.JToggleButton
	 */
    private JToggleButton getModsInstallButton() {
        if (modsInstallButton == null) {
            modsInstallButton = new JToggleButton();
            modsInstallButton.setBounds(new java.awt.Rectangle(3, 183, 132, 33));
            modsInstallButton.setText("Install");
            modsInstallButton.addChangeListener(new javax.swing.event.ChangeListener() {

                public void stateChanged(javax.swing.event.ChangeEvent e) {
                    if (modsInstallButton.isSelected()) {
                        modsInstallButton.setText("Uninstall");
                    } else {
                        modsInstallButton.setText("Install");
                    }
                }
            });
            modsInstallButton.addMouseListener(new java.awt.event.MouseAdapter() {

                public void mouseReleased(java.awt.event.MouseEvent e) {
                    if (currentRow >= 0) {
                        if (modsInstallButton.isSelected()) {
                            tModelMods.setValueAt("yes", currentRow, 2);
                            addOnUpdate(modsListTable.getValueAt(currentRow, 0).toString(), "toinstall", "yes");
                        } else {
                            tModelMods.setValueAt("no", currentRow, 2);
                            addOnUpdate(modsListTable.getValueAt(currentRow, 0).toString(), "toinstall", "no");
                        }
                    }
                }
            });
        }
        return modsInstallButton;
    }

    /**
	 * This method initializes wowLaunch
	 * 
	 * @return javax.swing.JButton
	 */
    private JButton getWowLaunch() {
        if (wowLaunch == null) {
            wowLaunch = new JButton();
            wowLaunch.setBounds(new java.awt.Rectangle(330, 290, 190, 45));
            wowLaunch.setFont(new java.awt.Font("Dialog", java.awt.Font.BOLD, 16));
            wowLaunch.setText("Launch WoW");
            wowLaunch.addMouseListener(new java.awt.event.MouseAdapter() {

                public void mouseClicked(java.awt.event.MouseEvent e) {
                    saveData();
                    ExecuteCMD("wow", true);
                    if (rosterCheckUpdate.isSelected()) {
                    }
                    dispose();
                }
            });
        }
        return wowLaunch;
    }

    /**
	 * This method initializes wowWeb
	 * 
	 * @return javax.swing.JButton
	 */
    private JButton getWowWeb() {
        if (wowWeb == null) {
            wowWeb = new JButton();
            wowWeb.setBounds(new java.awt.Rectangle(190, 295, 135, 40));
            wowWeb.setText("WoWEurope");
            wowWeb.addMouseListener(new java.awt.event.MouseAdapter() {

                public void mouseClicked(java.awt.event.MouseEvent e) {
                    ExecuteURL("http://www.wow-europe.com/");
                }
            });
        }
        return wowWeb;
    }

    /**
	 * This method initializes wsWeb
	 * 
	 * @return javax.swing.JButton
	 */
    private JButton getWsWeb() {
        if (wsWeb == null) {
            wsWeb = new JButton();
            wsWeb.setBounds(new java.awt.Rectangle(50, 295, 135, 40));
            wsWeb.setText("WoWSystem");
            wsWeb.addMouseListener(new java.awt.event.MouseAdapter() {

                public void mouseClicked(java.awt.event.MouseEvent e) {
                    ExecuteURL("http://wowsystem.bogala.org/");
                }
            });
        }
        return wsWeb;
    }

    /**
	 * This method initializes langEn
	 * 
	 * @return javax.swing.JButton
	 */
    private JButton getLangEn() {
        if (langEn == null) {
            langEn = new JButton();
            langEn.setIcon(new ImageIcon(getClass().getResource("/JWoWSystem/en.gif")));
            langEn.setSize(new java.awt.Dimension(40, 40));
            langEn.setLocation(new java.awt.Point(5, 295));
        }
        return langEn;
    }

    /**
	 * This method initializes rosterScrollProfiles
	 * 
	 * @return javax.swing.JScrollPane
	 */
    private JScrollPane getRosterScrollProfiles() {
        if (rosterScrollProfiles == null) {
            rosterScrollProfiles = new JScrollPane();
            rosterScrollProfiles.setBounds(new java.awt.Rectangle(4, 30, 501, 137));
            rosterScrollProfiles.setViewportView(getRosterProfiles());
        }
        return rosterScrollProfiles;
    }

    /**
	 * This method initializes rosterProfiles
	 * 
	 * @return javax.swing.JTable
	 */
    private JTable getRosterProfiles() {
        if (rosterProfiles == null) {
            rosterProfiles = new JTable();
            tmn = new DefaultTableModel();
            rosterProfiles.setModel(tmn);
            tmn.addColumn("Profile");
            tmn.addColumn("Last Update");
            Object[] vals = { new String("Taichin"), new String("24/05/2006") };
            tmn.addRow(vals);
        }
        return rosterProfiles;
    }

    /**
	 * This method initializes modsAUButton
	 * 
	 * @return javax.swing.JToggleButton
	 */
    private JToggleButton getModsAUButton() {
        if (modsAUButton == null) {
            modsAUButton = new JToggleButton();
            modsAUButton.setBounds(new java.awt.Rectangle(137, 183, 132, 33));
            modsAUButton.setText("Auto Update");
            modsAUButton.addChangeListener(new javax.swing.event.ChangeListener() {

                public void stateChanged(javax.swing.event.ChangeEvent e) {
                    if (modsAUButton.isSelected()) {
                        modsAUButton.setText("AutoUpdate : Yes");
                    } else {
                        modsAUButton.setText("AutoUpdate : No");
                    }
                }
            });
            modsAUButton.addMouseListener(new java.awt.event.MouseAdapter() {

                public void mouseReleased(java.awt.event.MouseEvent e) {
                    System.out.println("mouseReleased()");
                    if (currentRow >= 0) {
                        if (modsAUButton.isSelected()) {
                            tModelMods.setValueAt("yes", currentRow, 3);
                            addOnUpdate(modsListTable.getValueAt(currentRow, 0).toString(), "aupdate", "yes");
                        } else {
                            tModelMods.setValueAt("no", currentRow, 3);
                            addOnUpdate(modsListTable.getValueAt(currentRow, 0).toString(), "aupdate", "no");
                        }
                    }
                }
            });
        }
        return modsAUButton;
    }

    /**
	 * This method initializes modsListScrollPane
	 * 
	 * @return javax.swing.JScrollPane
	 */
    private JScrollPane getModsListScrollPane() {
        if (modsListScrollPane == null) {
            modsListScrollPane = new JScrollPane();
            modsListScrollPane.setBounds(new java.awt.Rectangle(3, 3, 502, 178));
            modsListScrollPane.setViewportView(getModsListTable());
        }
        return modsListScrollPane;
    }

    /**
	 * This method initializes modsListTable
	 * 
	 * @return javax.swing.JTable
	 */
    private JTable getModsListTable() {
        if (modsListTable == null) {
            modsListTable = new JTable();
            modsListTable.getTableHeader().setReorderingAllowed(false);
            tModelMods = new DefaultTableModel();
            modsListTable.setModel(tModelMods);
            tModelMods.addColumn("AddOns");
            tModelMods.addColumn("Version");
            tModelMods.addColumn("Install");
            tModelMods.addColumn("AUpdate");
            modsListTable.addMouseListener(new java.awt.event.MouseAdapter() {

                public void mouseClicked(java.awt.event.MouseEvent e) {
                    System.out.println("mouseClicked()");
                    Point p = e.getPoint();
                    currentRow = modsListTable.rowAtPoint(p);
                    if (currentRow >= 0) {
                        if (modsListTable.getValueAt(currentRow, 2).toString().compareToIgnoreCase("yes") == 0) {
                            modsInstallButton.setSelected(true);
                        } else {
                            modsInstallButton.setSelected(false);
                        }
                        if (modsListTable.getValueAt(currentRow, 3).toString().compareToIgnoreCase("yes") == 0) {
                            modsAUButton.setSelected(true);
                        } else {
                            modsAUButton.setSelected(false);
                        }
                    }
                }
            });
            Object[] vals = { new String("CT_RaidAssist"), new String("v1.35 - LOC1.42"), new String("Yes"), new String("Yes") };
            tModelMods.addRow(vals);
        }
        return modsListTable;
    }

    public static void affiche(Document direct) {
        try {
            XMLOutputter sortie = new XMLOutputter(Format.getPrettyFormat());
            sortie.output(direct, System.out);
        } catch (java.io.IOException e) {
        }
    }

    public void enregistre(String fichier, Document direct) {
        try {
            XMLOutputter sortie = new XMLOutputter(Format.getPrettyFormat());
            sortie.output(direct, new FileOutputStream(fichier));
        } catch (java.io.IOException e) {
        }
    }

    public void afficheNews() {
        List newsSearch = rootXMLNews.getChildren("news");
        int numNews;
        Iterator iNews = newsSearch.iterator();
        numNews = 1;
        while (iNews.hasNext()) {
            Element courant = (Element) iNews.next();
            if (numNews == 1) {
                newsLabel1.setText(courant.getChild("title").getTextNormalize());
                textNews1.setText(courant.getChild("text").getTextNormalize());
            } else if (numNews == 2) {
                newsLabel2.setText(courant.getChild("title").getTextNormalize());
                textNews2.setText(courant.getChild("text").getTextNormalize());
            } else if (numNews == 3) {
                newsLabel3.setText(courant.getChild("title").getTextNormalize());
                textNews3.setText(courant.getChild("text").getTextNormalize());
            }
            numNews = numNews + 1;
        }
    }

    public void addOnsGetList() {
        lLoading.setText("AddOns Update");
        Element appNode = RootXMLTotal.getChild("addOns");
        lLoading.setText("AddOns List creation");
        pbLoading.setMinimum(0);
        pbLoading.setMaximum(appNode.getChildren().size());
        int n = 0;
        pbLoading.setValue(n);
        List list = appNode.getChildren();
        tModelMods = new DefaultTableModel();
        modsListTable.setModel(tModelMods);
        tModelMods.addColumn("AddOns");
        tModelMods.addColumn("Version");
        tModelMods.addColumn("Install");
        tModelMods.addColumn("AUpdate");
        Iterator i = list.iterator();
        while (i.hasNext()) {
            Element appAddOn = (Element) i.next();
            n++;
            pbLoading.setValue(n);
            Object[] vals = { appAddOn.getName(), appAddOn.getChild("sversion").getTextNormalize(), appAddOn.getChild("toinstall").getTextNormalize(), appAddOn.getChild("aupdate").getTextNormalize() };
            tModelMods.addRow(vals);
        }
    }

    public void addOnsMAJ() {
        lLoading.setText("AddOns Update");
        Element appNode = RootXMLTotal.getChild("addOns");
        List list = appNode.getChildren();
        while (list.iterator().hasNext()) {
            Element appAddOn = (Element) list.iterator().next();
            lLoading.setText("Update AddOn " + appAddOn.getName());
            if (appAddOn.getChild("toinstall").getTextNormalize().compareToIgnoreCase("yes") == 0) {
                if (appAddOn.getChild("aupdate").getTextNormalize().compareToIgnoreCase("yes") == 0) {
                    File f = new File("Interface" + File.separator + "AddOns" + File.separator + appAddOn.getName());
                    System.out.println("Check Interface" + File.separator + "AddOns" + File.separator + appAddOn.getName());
                    if ((appAddOn.getChild("version").getTextNormalize().compareToIgnoreCase(appAddOn.getChild("sversion").getTextNormalize()) > 0) || !f.exists()) ;
                    {
                        downloadFile(appAddOn.getChildTextNormalize("zipFile"));
                        UnZipFile(appAddOn.getName() + ".zip");
                        File zf = new File(appAddOn.getName() + ".zip");
                        zf.delete();
                        addOnUpdate(appAddOn.getName(), "version", appAddOn.getChild("sversion").getTextNormalize());
                    }
                }
            } else {
                File f = new File(appAddOn.getName());
                if (f.exists()) {
                    lLoading.setText("Uninstall " + appAddOn.getName());
                    f.delete();
                }
            }
        }
    }

    public void addOnUpdate(String addOnName, String nodeName, String nodeValue) {
        verifXML = true;
        Element appNode = RootXMLTotal.getChild("addOns");
        Element appNodeVal = appNode.getChild(addOnName);
        Element appNodeEl = appNodeVal.getChild(nodeName);
        appNodeEl.setText(nodeValue);
        appNodeVal.removeChild(nodeName);
        appNodeVal.addContent(appNodeEl);
        appNode.removeChild(addOnName);
        appNode.addContent(appNodeVal);
        RootXMLTotal.removeChild("addOns");
        RootXMLTotal.addContent(appNode);
        verifXML = true;
    }

    public void rosterUpdate(String nodeName, String nodeValue) {
        verifXML = true;
        Element appNode = RootXMLTotal.getChild("roster");
        Element appNodeVal = appNode.getChild(nodeName);
        appNodeVal.setText(nodeValue);
        appNode.removeChild(nodeName);
        appNode.addContent(appNodeVal);
        RootXMLTotal.removeChild("roster");
        RootXMLTotal.addContent(appNode);
    }

    public void applicationUpdate(String nodeName, String nodeValue) {
        verifXML = true;
        Element appNode = RootXMLTotal.getChild("application");
        Element appNodeVal = appNode.getChild(nodeName);
        appNodeVal.setText(nodeValue);
        appNode.removeChild(nodeName);
        appNode.addContent(appNodeVal);
        RootXMLTotal.removeChild("application");
        RootXMLTotal.addContent(appNode);
    }

    public void applicationBetaUpdate(String nodeName, String nodeValue) {
        Element appNode = RootXMLTotal.getChild("application");
        Element appNodeVal = appNode.getChild("beta");
        Element appNodeEl = appNodeVal.getChild(nodeName);
        if (appNodeEl.getTextNormalize().compareToIgnoreCase(nodeValue) > 0) {
            appNodeEl.setText(nodeValue);
            appNodeVal.removeChild(nodeName);
            appNodeVal.addContent(appNodeEl);
            appNode.removeChild("beta");
            appNode.addContent(appNodeVal);
            RootXMLTotal.removeChild("application");
            RootXMLTotal.addContent(appNode);
            verifXML = true;
        }
    }

    public void saveXML() {
        if (verifXML) {
            File source = new File(localXMLFile);
            File destination = new File("default_old.xml");
            if (destination.exists()) {
                destination.delete();
            }
            source.renameTo(destination);
            enregistre(localXMLFile, RootXMLTotal.getDocument());
        }
    }

    public void initRosterTab() {
        Locale locale = Locale.getDefault();
        DateFormat dateFormat = DateFormat.getDateInstance(DateFormat.SHORT, locale);
        lLoading.setText("Roster Data Loading...");
        urlRosterValue.setText(RootXMLTotal.getChild("roster").getChild("url").getTextNormalize());
        rosterCheckUpdate.setSelected(RootXMLTotal.getChild("roster").getChild("upload").getTextNormalize().compareToIgnoreCase("yes") == 0);
        lLoading.setText("Roster Data Loaded...");
        tmn = new DefaultTableModel();
        rosterProfiles.setModel(tmn);
        tmn.addColumn("Profile");
        tmn.addColumn("Last Update");
        File wtf = new File("WTF");
        if (wtf.isDirectory()) {
            File accounts = new File("WTF" + File.separator + "Account");
            if (accounts.isDirectory()) {
                for (String cpt : accounts.list()) {
                    File cptF = new File("WTF" + File.separator + "Account" + File.separator + cpt);
                    if (cptF.isDirectory()) {
                        File characterProfiler = new File("WTF" + File.separator + "Account" + File.separator + cpt + File.separator + "SavedVariables" + File.separator + "WRFR_CharacterProfiler.lua");
                        if (characterProfiler.exists()) {
                            Object[] vals = { cpt, dateFormat.format(new Date(characterProfiler.lastModified())) };
                            tmn.addRow(vals);
                        } else {
                            Object[] vals = { cpt, new String("No Profiler") };
                            tmn.addRow(vals);
                        }
                    }
                }
            }
        }
    }

    public void initAboutBox() {
        String labelName;
        labelName = RootXMLTotal.getChild("application").getChild("name").getTextNormalize();
        labelName = labelName + " " + localAVersion;
        jLabel2.setText(labelName);
        String VersionningLabelName;
        VersionningLabelName = "Last Release version on Server :";
        VersionningLabelName = VersionningLabelName + " " + RootXMLTotal.getChild("application").getChild("version").getTextNormalize();
        if (RootXMLTotal.getChild("application").getChild("beta").getChild("accessible").getTextNormalize().compareToIgnoreCase("no") == 0) {
            VersionningLabelName = VersionningLabelName + " - No Beta available ";
        }
        if (RootXMLTotal.getChild("application").getChild("beta").getChild("accessible").getTextNormalize().compareToIgnoreCase("yes") == 0) {
            VersionningLabelName = VersionningLabelName + " - Beta available for " + RootXMLTotal.getChild("application").getChild("beta").getChild("version").getTextNormalize();
        }
        versionningLabel.setText(VersionningLabelName);
    }

    public static Element OpenSetRootXMLFile(String fichier) {
        org.jdom.Document document;
        SAXBuilder sxb = new SAXBuilder();
        document = new org.jdom.Document();
        try {
            document = sxb.build(fichier);
        } catch (JDOMException se) {
            System.out.println("Erreur lors du parsing du document " + fichier);
            System.out.println("lors de l'appel � construteur.parse(xml)");
            return null;
        } catch (IOException ioe) {
            System.out.println("Erreur d'entr�e/sortie");
            System.out.println("lors de l'appel � construteur.parse(xml)");
            return null;
        }
        Element docRecup = document.getRootElement();
        return docRecup;
    }

    private void saveData() {
        saveXML();
    }

    public void UnZipFile(String path) {
        try {
            lLoading.setText("Unzip " + path);
            int BUFFER = 2048;
            BufferedOutputStream dest = null;
            BufferedInputStream is = null;
            ZipEntry entry;
            ZipFile zipfile = new ZipFile(path);
            Enumeration e = zipfile.entries();
            while (e.hasMoreElements()) {
                entry = (ZipEntry) e.nextElement();
                String url = entry.getName();
                String URLfichier = url;
                File URL = new File(URLfichier);
                if (isFile(URL.getName())) {
                    is = new BufferedInputStream(zipfile.getInputStream(entry));
                    int count;
                    byte data[] = new byte[BUFFER];
                    FileOutputStream fos = new FileOutputStream(URLfichier);
                    dest = new BufferedOutputStream(fos, BUFFER);
                    while ((count = is.read(data, 0, BUFFER)) != -1) {
                        dest.write(data, 0, count);
                    }
                    dest.flush();
                    dest.close();
                    is.close();
                } else {
                    URL.mkdirs();
                }
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    private boolean isFile(String nom) {
        for (int i = nom.length() - 1; i >= 0; i--) {
            if (nom.charAt(i) == '.') {
                return true;
            }
        }
        return false;
    }

    public String downloadFile(String host) {
        try {
            URL rootURL = new URL(host);
            return getFile(rootURL);
        } catch (MalformedURLException e) {
            System.err.println(host + " : URL non comprise.");
            return null;
        } catch (IOException e) {
            System.err.println(e);
            return null;
        }
    }

    public String getFile(URL urlFile) throws IOException {
        URLConnection urlConn = urlFile.openConnection();
        @SuppressWarnings("unused") String FileType = urlConn.getContentType();
        int FileLenght = urlConn.getContentLength();
        if (FileLenght == -1) {
            throw new IOException("Fichier non valide.");
        }
        InputStream brut = urlConn.getInputStream();
        InputStream entree = new BufferedInputStream(brut);
        byte[] donnees = new byte[FileLenght];
        int BitRead = 0;
        int deplacement = 0;
        lLoading.setText("Download " + urlConn.toString().substring(urlConn.toString().lastIndexOf('/') + 1));
        pbLoading.setMinimum(0);
        pbLoading.setMaximum(FileLenght);
        pbLoading.setValue(deplacement);
        while (deplacement < FileLenght) {
            BitRead = entree.read(donnees, deplacement, donnees.length - deplacement);
            if (BitRead == -1) break;
            deplacement += BitRead;
            pbLoading.setValue(deplacement);
        }
        entree.close();
        if (deplacement != FileLenght) {
            throw new IOException("Nous n'avons lu que " + deplacement + " octets au lieu des " + FileLenght + " attendus");
        }
        String FileName = urlFile.getFile();
        FileName = FileName.substring(FileName.lastIndexOf('/') + 1);
        FileOutputStream WritenFile = new FileOutputStream(FileName);
        WritenFile.write(donnees);
        WritenFile.flush();
        WritenFile.close();
        return FileName;
    }

    public void doPost(String adresse) {
        OutputStreamWriter writer = null;
        BufferedReader reader = null;
        try {
            String donnees = URLEncoder.encode("clef", "UTF-8") + "=" + URLEncoder.encode("valeur", "UTF-8");
            donnees += "&" + URLEncoder.encode("autreClef", "UTF-8") + "=" + URLEncoder.encode("autreValeur", "UTF-8");
            URL url = new URL(adresse);
            URLConnection conn = url.openConnection();
            conn.setDoOutput(true);
            writer = new OutputStreamWriter(conn.getOutputStream());
            writer.write(donnees);
            writer.flush();
            reader = new BufferedReader(new InputStreamReader(conn.getInputStream()));
            String ligne;
            while ((ligne = reader.readLine()) != null) {
                System.out.println(ligne);
            }
        } catch (Exception e) {
            e.printStackTrace();
        } finally {
            try {
                writer.close();
            } catch (Exception e) {
            }
            try {
                reader.close();
            } catch (Exception e) {
            }
        }
    }

    public static void Excec(String cmd, Boolean wait) {
        try {
            Runtime r = Runtime.getRuntime();
            Process p = r.exec(cmd);
            if (wait) p.waitFor();
        } catch (Exception e) {
            System.out.println("erreur d'execution " + cmd + " " + e.toString());
        }
    }

    public static void ExecuteCMD(String cmd, Boolean wait) {
        try {
            Runtime r = Runtime.getRuntime();
            Process p = r.exec(cmd);
            if (wait) p.waitFor();
        } catch (Exception e) {
            System.out.println("erreur d'execution " + cmd + " " + e.toString());
        }
    }

    public static void ExecuteURL(String cmd) {
        try {
            String browser = "";
            Runtime r = Runtime.getRuntime();
            if (System.getProperty("os.name").startsWith("Mac")) {
                try {
                    Process p = Runtime.getRuntime().exec("which open");
                    if (p.waitFor() == 0) {
                        browser = ("open");
                    }
                } catch (IOException e) {
                } catch (InterruptedException e) {
                }
            } else if (System.getProperty("os.name").startsWith("Windows")) {
                browser = ("rundll32 url.dll,FileProtocolHandler ");
            } else {
                try {
                    Process p = Runtime.getRuntime().exec("which konqueror");
                    if (p.waitFor() == 0) {
                        browser = ("konqueror");
                    }
                } catch (IOException e) {
                } catch (InterruptedException e) {
                }
            }
            r.exec(browser + " " + cmd);
        } catch (Exception e) {
            System.out.println("erreur d'execution " + cmd + " " + e.toString());
        }
    }

    public static void main(String[] args) {
        try {
            UIManager.setLookAndFeel("javax.swing.plaf.metal.MetalLookAndFeel");
        } catch (Exception e) {
            e.printStackTrace();
        }
        new launcher();
    }
}
