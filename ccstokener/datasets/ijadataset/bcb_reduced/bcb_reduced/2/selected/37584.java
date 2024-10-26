package org.jdna.minecraft.tools;

import java.awt.Color;
import java.awt.EventQueue;
import java.awt.Font;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.Insets;
import java.awt.SystemColor;
import java.awt.Toolkit;
import java.awt.datatransfer.DataFlavor;
import java.awt.datatransfer.Transferable;
import java.awt.datatransfer.UnsupportedFlavorException;
import java.awt.dnd.DropTarget;
import java.awt.dnd.DropTargetDropEvent;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.io.File;
import java.io.IOException;
import java.net.URI;
import java.net.URL;
import java.net.URLConnection;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import javax.swing.JButton;
import javax.swing.JComponent;
import javax.swing.JDialog;
import javax.swing.JFileChooser;
import javax.swing.JFrame;
import javax.swing.JMenu;
import javax.swing.JMenuBar;
import javax.swing.JMenuItem;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JPopupMenu;
import javax.swing.JScrollPane;
import javax.swing.JSeparator;
import javax.swing.JTable;
import javax.swing.JTextPane;
import javax.swing.ListSelectionModel;
import javax.swing.SwingConstants;
import javax.swing.SwingUtilities;
import javax.swing.UIManager;
import javax.swing.UnsupportedLookAndFeelException;
import javax.swing.border.EmptyBorder;
import javax.swing.border.LineBorder;
import javax.swing.filechooser.FileFilter;
import javax.swing.table.DefaultTableModel;
import org.apache.log4j.Logger;
import org.dom4j.Document;
import org.dom4j.Element;
import org.dom4j.io.SAXReader;

public class MainFrame extends JFrame implements IStatusHandler {

    private static final String MODS_URL = "http://modmanager-for-minecraft.googlecode.com/svn/trunk/MincraftModManager/src/res/xml/mods.xml";

    private Logger log;

    private static final long serialVersionUID = 1L;

    private JPanel contentPane;

    private JTable table;

    private Preferences prefs;

    private JButton btnInstallMods;

    private JMenu mnMods;

    /**
	 * Launch the application.
	 */
    public static void main(String[] args) {
        try {
            UIManager.setLookAndFeel(UIManager.getSystemLookAndFeelClassName());
        } catch (ClassNotFoundException e1) {
            e1.printStackTrace();
        } catch (InstantiationException e1) {
            e1.printStackTrace();
        } catch (IllegalAccessException e1) {
            e1.printStackTrace();
        } catch (UnsupportedLookAndFeelException e1) {
            e1.printStackTrace();
        }
        EventQueue.invokeLater(new Runnable() {

            public void run() {
                try {
                    MainFrame frame = new MainFrame();
                    frame.setVisible(true);
                } catch (Exception e) {
                    e.printStackTrace();
                }
            }
        });
    }

    /**
	 * Create the frame.
	 */
    public MainFrame() {
        prefs = Application.getInstance().getPreferences();
        log = Logger.getLogger(this.getClass());
        setIconImage(Toolkit.getDefaultToolkit().getImage(MainFrame.class.getResource("/res/icons/icon_256.png")));
        setTitle("Stuckless Mod Manager - " + Version.VERSION);
        setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        setBounds(100, 100, 603, 402);
        JMenuBar menuBar = new JMenuBar();
        setJMenuBar(menuBar);
        JMenu mnFile = new JMenu("File");
        menuBar.add(mnFile);
        JMenuItem mntmAddMod = new JMenuItem("Add Mods...");
        mntmAddMod.addActionListener(new ActionListener() {

            public void actionPerformed(ActionEvent arg0) {
                addModAction();
            }
        });
        mntmAddMod.setIcon(Application.icon("list-add"));
        mnFile.add(mntmAddMod);
        JMenuItem mntmExit = new JMenuItem("Exit");
        mntmExit.addActionListener(new ActionListener() {

            public void actionPerformed(ActionEvent arg0) {
                exit();
            }
        });
        mntmExit.setIcon(Application.icon("system-log-out"));
        JMenuItem mntmClearMods = new JMenuItem("Clear Mods");
        mntmClearMods.addActionListener(new ActionListener() {

            public void actionPerformed(ActionEvent arg0) {
                ((DefaultTableModel) table.getModel()).getDataVector().clear();
                table.repaint();
            }
        });
        mntmClearMods.setIcon(Application.icon("list-remove"));
        mnFile.add(mntmClearMods);
        JMenuItem mntmPreferences = new JMenuItem("Preferences");
        mnFile.add(mntmPreferences);
        mntmPreferences.addActionListener(new ActionListener() {

            public void actionPerformed(ActionEvent arg0) {
                PreferencesDialog dialog = new PreferencesDialog();
                dialog.setDefaultCloseOperation(JDialog.DISPOSE_ON_CLOSE);
                dialog.setVisible(true);
            }
        });
        mntmPreferences.setIcon(Application.icon("applications-system"));
        JMenuItem mntmRestore = new JMenuItem("Restore...");
        mntmRestore.setIcon(Application.icon("edit-undo"));
        mntmRestore.addActionListener(new ActionListener() {

            @Override
            public void actionPerformed(ActionEvent e) {
                RestoreDialog dialog = new RestoreDialog();
                dialog.setDefaultCloseOperation(JDialog.DISPOSE_ON_CLOSE);
                dialog.setVisible(true);
            }
        });
        mnFile.add(mntmRestore);
        JSeparator separator = new JSeparator();
        mnFile.add(separator);
        mnFile.add(mntmExit);
        mnMods = new JMenu("Mods");
        menuBar.add(mnMods);
        JMenuItem mntmGotoMinecraftMods = new JMenuItem("Minecraft Mods Forum");
        mntmGotoMinecraftMods.addActionListener(new ActionListener() {

            public void actionPerformed(ActionEvent arg0) {
                Application.getInstance().openUrl("http://www.minecraftforum.net/forum/51-released-mods/");
            }
        });
        mntmGotoMinecraftMods.setIcon(Application.icon("internet-web-browser"));
        mnMods.add(mntmGotoMinecraftMods);
        JMenuItem mntmRefreshModList = new JMenuItem("Refresh Mod List");
        mntmRefreshModList.addActionListener(new ActionListener() {

            public void actionPerformed(ActionEvent e) {
                refreshMods(MODS_URL);
            }
        });
        mntmRefreshModList.setIcon(Application.icon("view-refresh"));
        mnMods.add(mntmRefreshModList);
        JSeparator separator_1 = new JSeparator();
        mnMods.add(separator_1);
        contentPane = new JPanel();
        contentPane.setBorder(new EmptyBorder(5, 5, 5, 5));
        setContentPane(contentPane);
        GridBagLayout gbl_contentPane = new GridBagLayout();
        gbl_contentPane.columnWidths = new int[] { 0, 0 };
        gbl_contentPane.rowHeights = new int[] { 0, 0, 0, 0 };
        gbl_contentPane.columnWeights = new double[] { 1.0, Double.MIN_VALUE };
        gbl_contentPane.rowWeights = new double[] { 0.0, 1.0, 0.0, Double.MIN_VALUE };
        contentPane.setLayout(gbl_contentPane);
        JPanel panel = new JPanel();
        panel.setBorder(new LineBorder(SystemColor.windowBorder, 2, true));
        GridBagConstraints gbc_panel = new GridBagConstraints();
        gbc_panel.fill = GridBagConstraints.BOTH;
        gbc_panel.insets = new Insets(0, 0, 5, 0);
        gbc_panel.gridx = 0;
        gbc_panel.gridy = 0;
        contentPane.add(panel, gbc_panel);
        GridBagLayout gbl_panel = new GridBagLayout();
        gbl_panel.columnWidths = new int[] { 0, 0, 0 };
        gbl_panel.rowHeights = new int[] { 0, 0 };
        gbl_panel.columnWeights = new double[] { 1.0, 0.0, Double.MIN_VALUE };
        gbl_panel.rowWeights = new double[] { 0.0, Double.MIN_VALUE };
        panel.setLayout(gbl_panel);
        JTextPane txtpnDragModsFrom = new JTextPane();
        txtpnDragModsFrom.setForeground(Color.GRAY);
        GridBagConstraints gbc_txtpnDragModsFrom = new GridBagConstraints();
        gbc_txtpnDragModsFrom.fill = GridBagConstraints.HORIZONTAL;
        gbc_txtpnDragModsFrom.insets = new Insets(0, 0, 0, 5);
        gbc_txtpnDragModsFrom.gridx = 0;
        gbc_txtpnDragModsFrom.gridy = 0;
        panel.add(txtpnDragModsFrom, gbc_txtpnDragModsFrom);
        txtpnDragModsFrom.setFont(new Font("Dialog", Font.BOLD | Font.ITALIC, 12));
        txtpnDragModsFrom.setBackground(Color.WHITE);
        txtpnDragModsFrom.setEditable(false);
        txtpnDragModsFrom.setText("Drag Mods from your file manager to the Table below and click 'Install Mods' or use File -> Add Mods from the menu to add mods");
        {
            ImagePanel img = new ImagePanel(Application.imageRes("res/donate.png"));
            GridBagConstraints gbc_lblTest = new GridBagConstraints();
            gbc_lblTest.gridx = 1;
            gbc_lblTest.gridy = 0;
            panel.add(img, gbc_lblTest);
            img.addActionListener(new ActionListener() {

                @Override
                public void actionPerformed(ActionEvent e) {
                    Application.getInstance().openUrl(prefs.getDonateLink());
                }
            });
        }
        JScrollPane scrollPane = new JScrollPane();
        setupDND(scrollPane);
        GridBagConstraints gbc_scrollPane = new GridBagConstraints();
        gbc_scrollPane.fill = GridBagConstraints.BOTH;
        gbc_scrollPane.insets = new Insets(0, 0, 5, 0);
        gbc_scrollPane.gridx = 0;
        gbc_scrollPane.gridy = 1;
        contentPane.add(scrollPane, gbc_scrollPane);
        table = new JTable();
        setupTable(table);
        scrollPane.setViewportView(table);
        btnInstallMods = new JButton("Install Mods");
        btnInstallMods.addActionListener(new ActionListener() {

            public void actionPerformed(ActionEvent arg0) {
                installMods();
            }
        });
        btnInstallMods.setIcon(Application.icon("go-next"));
        btnInstallMods.setVerticalTextPosition(SwingConstants.CENTER);
        btnInstallMods.setHorizontalTextPosition(SwingConstants.LEFT);
        GridBagConstraints gbc_btnInstallMods = new GridBagConstraints();
        gbc_btnInstallMods.gridx = 0;
        gbc_btnInstallMods.gridy = 2;
        contentPane.add(btnInstallMods, gbc_btnInstallMods);
        SwingUtilities.invokeLater(new Runnable() {

            @Override
            public void run() {
                loadModMenu(mnMods);
            }
        });
    }

    protected void refreshMods(String modsUrl) {
        while (true) {
            JMenuItem mi = mnMods.getItem(mnMods.getItemCount() - 1);
            if (mi == null) break;
            mnMods.remove(mnMods.getItemCount() - 1);
        }
        try {
            log.info("Loading mods from " + modsUrl);
            URL url = new URL(modsUrl);
            URLConnection conn = url.openConnection();
            conn.setUseCaches(false);
            SAXReader reader = new SAXReader();
            Document document = reader.read(conn.getInputStream());
            Element root = document.getRootElement();
            for (Iterator i = root.elementIterator("mod"); i.hasNext(); ) {
                final Element mod = (Element) i.next();
                JMenuItem mi = new JMenuItem(mod.element("name").getTextTrim());
                mi.setIcon(Application.icon("applications-other"));
                mi.addActionListener(new ActionListener() {

                    @Override
                    public void actionPerformed(ActionEvent e) {
                        Application.getInstance().openUrl(mod.element("siteUrl").getTextTrim());
                    }
                });
                mnMods.add(mi);
            }
        } catch (Throwable t) {
            t.printStackTrace();
            log.warn("Failed to dynamically add mod links");
        }
    }

    protected void loadModMenu(JMenu mnMods) {
        refreshMods(prefs.getModsUrl().toExternalForm());
    }

    protected void exit() {
        System.exit(0);
    }

    public void addModAction() {
        JFileChooser fc = new JFileChooser(prefs.getMinecraftModsDir());
        fc.setDialogTitle("Select Mod");
        fc.setFileSelectionMode(JFileChooser.FILES_ONLY);
        fc.setMultiSelectionEnabled(true);
        fc.setFileFilter(new FileFilter() {

            @Override
            public String getDescription() {
                return "Mod Files";
            }

            @Override
            public boolean accept(File f) {
                return f.isDirectory() || f.getName().toLowerCase().endsWith(".zip") || f.getName().toLowerCase().endsWith(".rar");
            }
        });
        fc.showOpenDialog(this);
        File files[] = fc.getSelectedFiles();
        if (files != null) {
            for (File f : files) {
                prefs.setMinecraftModsDir(f.getParentFile());
                addModFile(f);
            }
        }
    }

    private void addModFile(File file) {
        log.info("Adding File: " + file.getAbsolutePath());
        DefaultTableModel dtm = (DefaultTableModel) table.getModel();
        dtm.addRow(new Object[] { true, file.getAbsolutePath(), "" });
    }

    private void setupTable(final JTable table) {
        DefaultTableModel model = new DefaultTableModel(new String[] { "", "Mod", "Status" }, 0) {

            @Override
            public boolean isCellEditable(int row, int column) {
                return false;
            }
        };
        table.setModel(model);
        table.getColumnModel().getColumn(0).setCellEditor(table.getDefaultEditor(Boolean.class));
        table.getColumnModel().getColumn(0).setCellRenderer(table.getDefaultRenderer(Boolean.class));
        table.getColumnModel().getColumn(0).setPreferredWidth(16);
        table.getColumnModel().getColumn(1).setPreferredWidth(400);
        table.getColumnModel().removeColumn(table.getColumnModel().getColumn(0));
        final JPopupMenu ctxMenu = new JPopupMenu();
        JMenuItem del = new JMenuItem("Remove Mod");
        del.setIcon(Application.icon("list-remove"));
        del.addActionListener(new ActionListener() {

            @Override
            public void actionPerformed(ActionEvent e) {
                ((DefaultTableModel) table.getModel()).removeRow(table.getSelectedRow());
            }
        });
        ctxMenu.add(del);
        table.addMouseListener(new MouseAdapter() {

            public void mousePressed(MouseEvent e) {
                showPopup(e);
            }

            public void mouseReleased(MouseEvent e) {
                showPopup(e);
            }

            private void showPopup(MouseEvent e) {
                if (e.isPopupTrigger()) {
                    ListSelectionModel selectionModel = table.getSelectionModel();
                    selectionModel.setSelectionInterval(table.rowAtPoint(e.getPoint()), table.rowAtPoint(e.getPoint()));
                    ctxMenu.show(e.getComponent(), e.getX(), e.getY());
                }
            }
        });
        setupDND(table);
    }

    private void setupDND(JComponent comp) {
        comp.setDropTarget(new DropTarget() {

            private static final long serialVersionUID = 1L;

            @Override
            public synchronized void drop(DropTargetDropEvent dtde) {
                dtde.acceptDrop(dtde.getDropAction());
                acceptTranserable(dtde.getTransferable());
                dtde.dropComplete(true);
            }
        });
    }

    private void acceptTranserable(Transferable transferable) {
        try {
            String sfiles = (String) transferable.getTransferData(DataFlavor.stringFlavor);
            String[] files = sfiles.split("\n");
            for (String f : files) {
                addModFile(new File(URI.create(f.trim())));
            }
        } catch (UnsupportedFlavorException e) {
            e.printStackTrace();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    protected void installMods() {
        if (table.getRowCount() == 0) {
            JOptionPane.showMessageDialog(this, "You must add at least one mod", "Install Message", JOptionPane.WARNING_MESSAGE);
            return;
        }
        try {
            ModInstallerDialog dialog = new ModInstallerDialog();
            dialog.setDefaultCloseOperation(JDialog.DISPOSE_ON_CLOSE);
            dialog.setVisible(true);
            List<File> files = new ArrayList<File>();
            for (int i = 0; i < table.getRowCount(); i++) {
                files.add(new File((String) table.getModel().getValueAt(i, 1)));
            }
            dialog.performUpdate(files, this);
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    private void updateStatus(File f, boolean state, String msg) {
        for (int i = 0; i < table.getRowCount(); i++) {
            String val = (String) table.getValueAt(i, 1);
            if (val != null && val.equals(f.getAbsolutePath())) {
                table.setValueAt(msg, i, 2);
                break;
            }
        }
    }

    @Override
    public void installed(File f) {
        log.info("installed mod " + f);
        updateStatus(f, true, "OK");
    }

    @Override
    public void fail(File f, Throwable t) {
        log.warn("failed to install file: " + f, t);
        updateStatus(f, false, "FAILED");
    }
}
