package gate.gui.creole.manager;

import gate.Gate;
import gate.gui.MainFrame;
import gate.resources.img.svg.AddIcon;
import gate.resources.img.svg.AdvancedIcon;
import gate.resources.img.svg.AvailableIcon;
import gate.resources.img.svg.DownloadIcon;
import gate.resources.img.svg.EditIcon;
import gate.resources.img.svg.GATEUpdateSiteIcon;
import gate.resources.img.svg.InvalidIcon;
import gate.resources.img.svg.OpenFileIcon;
import gate.resources.img.svg.RefreshIcon;
import gate.resources.img.svg.RemoveIcon;
import gate.resources.img.svg.UpdateSiteIcon;
import gate.resources.img.svg.UpdatesIcon;
import gate.resources.img.svg.UserPluginIcon;
import gate.swing.CheckBoxTableCellRenderer;
import gate.swing.IconTableCellRenderer;
import gate.swing.SpringUtilities;
import gate.swing.XJFileChooser;
import gate.swing.XJTable;
import gate.util.Files;
import gate.util.OptionsMap;
import gate.util.VersionComparator;
import java.awt.AWTException;
import java.awt.BorderLayout;
import java.awt.Component;
import java.awt.Dimension;
import java.awt.Robot;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.InputEvent;
import java.awt.event.KeyEvent;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.beans.PropertyChangeEvent;
import java.beans.PropertyChangeListener;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.net.URI;
import java.net.URISyntaxException;
import java.net.URL;
import java.net.URLConnection;
import java.text.Collator;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import javax.swing.AbstractAction;
import javax.swing.Action;
import javax.swing.BorderFactory;
import javax.swing.Box;
import javax.swing.BoxLayout;
import javax.swing.GrayFilter;
import javax.swing.Icon;
import javax.swing.ImageIcon;
import javax.swing.JButton;
import javax.swing.JCheckBox;
import javax.swing.JComponent;
import javax.swing.JDialog;
import javax.swing.JFileChooser;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JTabbedPane;
import javax.swing.JTextField;
import javax.swing.JToolBar;
import javax.swing.KeyStroke;
import javax.swing.ListSelectionModel;
import javax.swing.SpringLayout;
import javax.swing.SwingUtilities;
import javax.swing.event.ListSelectionEvent;
import javax.swing.event.ListSelectionListener;
import javax.swing.table.AbstractTableModel;
import org.apache.tools.ant.Project;
import org.apache.tools.ant.Target;
import org.apache.tools.ant.taskdefs.Expand;

/**
 * The CREOLE plugin manager which includes the ability to download and
 * install/update plugins from remote update sites.
 *
 * @author Mark A. Greenwood
 */
@SuppressWarnings("serial")
public class PluginUpdateManager extends JDialog {

    private PluginTableModel availableModel = new PluginTableModel(3);

    private PluginTableModel updatesModel = new PluginTableModel(4);

    private UpdateSiteModel sitesModel = new UpdateSiteModel();

    private AvailablePlugins installed = new AvailablePlugins();

    private ProgressPanel progressPanel = new ProgressPanel();

    private JPanel panel = new JPanel(new BorderLayout());

    private JTabbedPane tabs = new JTabbedPane();

    private static File userPluginDir;

    private JFrame owner;

    private List<RemoteUpdateSite> updateSites = new ArrayList<RemoteUpdateSite>();

    private static final String GATE_USER_PLUGINS = "gate.user.plugins";

    private static final String GATE_UPDATE_SITES = "gate.update.sites";

    private static final String SUPPRESS_USER_PLUGINS = "suppress.user.plugins";

    private static final String SUPPRESS_UPDATE_INSTALLED = "suppress.update.install";

    private static final String[] defaultUpdateSites = new String[] { "Austrian Research Institute for AI (OFAI)", "http://www.ofai.at/~johann.petrak/GATE/gate-update-site.xml", "Semantic Software Lab", "http://creole.semanticsoftware.info/gate-update-site.xml", "City University Centre for Health Informatics", "http://vega.soi.city.ac.uk/~abdy181/software/GATE/gate-update-site.xml" };

    public static File getUserPluginsHome() {
        if (userPluginDir == null) {
            String upd = System.getProperty(GATE_USER_PLUGINS, Gate.getUserConfig().getString(GATE_USER_PLUGINS));
            if (upd != null) {
                userPluginDir = new File(upd);
                if (!userPluginDir.exists() || !userPluginDir.isDirectory() || !userPluginDir.canWrite()) {
                    userPluginDir = null;
                    Gate.getUserConfig().remove(GATE_USER_PLUGINS);
                }
            }
        }
        return userPluginDir;
    }

    /**
   * Responsible for pushing some of the config date for the plugin manager into
   * the main user config. Note that this doesn't actually persist the data,
   * that is only done on a clean exit of the GUI by code hidden somewhere else.
   */
    private void saveConfig() {
        Map<String, String> sites = new HashMap<String, String>();
        for (RemoteUpdateSite rus : updateSites) {
            sites.put(rus.uri.toString(), (rus.enabled ? "1" : "0") + rus.name);
        }
        OptionsMap userConfig = Gate.getUserConfig();
        userConfig.put(GATE_UPDATE_SITES, sites);
        if (userPluginDir != null) userConfig.put(GATE_USER_PLUGINS, userPluginDir.getAbsolutePath());
    }

    /**
   * Load all the data about available plugins/updates from the remote update
   * sites as well as checking what is installed in the user plugin directory
   */
    private void loadData() {
        progressPanel.messageChanged("Loading CREOLE Plugin Information...");
        progressPanel.rangeChanged(0, 0);
        if (getUserPluginsHome() == null) {
            tabs.setEnabledAt(1, false);
            tabs.setEnabledAt(2, false);
            showProgressPanel(false);
            return;
        }
        new Thread() {

            @Override
            public void run() {
                availableModel.data.clear();
                updatesModel.data.clear();
                for (RemoteUpdateSite rus : updateSites) {
                    if (rus.enabled && (rus.valid == null || rus.valid)) {
                        for (CreolePlugin p : rus.getCreolePlugins()) {
                            if (p != null) {
                                int index = availableModel.data.indexOf(p);
                                if (index == -1) {
                                    availableModel.data.add(p);
                                } else {
                                    CreolePlugin pp = availableModel.data.get(index);
                                    if (VersionComparator.compareVersions(p.version, pp.version) > 0) {
                                        availableModel.data.remove(pp);
                                        availableModel.data.add(p);
                                    }
                                }
                            }
                        }
                    }
                }
                if (userPluginDir.exists() && userPluginDir.isDirectory()) {
                    File[] plugins = userPluginDir.listFiles();
                    for (File f : plugins) {
                        if (f.isDirectory()) {
                            File pluginInfo = new File(f, "creole.xml");
                            if (pluginInfo.exists()) {
                                try {
                                    CreolePlugin plugin = CreolePlugin.load(pluginInfo.toURI().toURL());
                                    if (plugin != null) {
                                        int index = availableModel.data.indexOf(plugin);
                                        if (index != -1) {
                                            CreolePlugin ap = availableModel.data.remove(index);
                                            if (VersionComparator.compareVersions(ap.version, plugin.version) > 0) {
                                                ap.installed = plugin.version;
                                                ap.dir = f;
                                                updatesModel.data.add(ap);
                                            }
                                        }
                                    }
                                    Gate.addKnownPlugin(f.toURI().toURL());
                                } catch (Exception e) {
                                    e.printStackTrace();
                                }
                            }
                        }
                    }
                }
                SwingUtilities.invokeLater(new Thread() {

                    @Override
                    public void run() {
                        installed.reInit();
                        updatesModel.dataChanged();
                        availableModel.dataChanged();
                        sitesModel.dataChanged();
                        tabs.setEnabledAt(1, updatesModel.data.size() > 0);
                        tabs.setEnabledAt(2, true);
                        showProgressPanel(false);
                    }
                });
            }
        }.start();
    }

    private void showProgressPanel(final boolean visible) {
        if (visible == getRootPane().getGlassPane().isVisible()) return;
        if (visible) {
            remove(panel);
            add(progressPanel, BorderLayout.CENTER);
        } else {
            remove(progressPanel);
            add(panel, BorderLayout.CENTER);
        }
        getRootPane().getGlassPane().setVisible(visible);
        validate();
    }

    private void applyChanges() {
        progressPanel.messageChanged("Updating CREOLE Plugin Configuration...");
        progressPanel.rangeChanged(0, updatesModel.data.size() + availableModel.data.size());
        showProgressPanel(true);
        new Thread() {

            @Override
            public void run() {
                if (getUserPluginsHome() != null) {
                    Expander expander = new Expander();
                    expander.setOverwrite(true);
                    expander.setDest(getUserPluginsHome());
                    List<CreolePlugin> failed = new ArrayList<CreolePlugin>();
                    boolean hasBeenWarned = Gate.getUserConfig().getBoolean(SUPPRESS_UPDATE_INSTALLED);
                    Iterator<CreolePlugin> it = updatesModel.data.iterator();
                    while (it.hasNext()) {
                        CreolePlugin p = it.next();
                        if (p.install) {
                            if (!hasBeenWarned) {
                                if (JOptionPane.showConfirmDialog(PluginUpdateManager.this, "<html><body style='width: 350px;'><b>UPDATE WARNING!</b><br><br>" + "Updating installed plugins will remove any customizations you may have made. " + "Are you sure you wish to continue?</body></html>", "CREOLE Plugin Manager", JOptionPane.OK_CANCEL_OPTION, JOptionPane.QUESTION_MESSAGE, new DownloadIcon(48, 48)) == JOptionPane.OK_OPTION) {
                                    hasBeenWarned = true;
                                } else {
                                    SwingUtilities.invokeLater(new Thread() {

                                        @Override
                                        public void run() {
                                            showProgressPanel(false);
                                        }
                                    });
                                    return;
                                }
                            }
                            progressPanel.messageChanged("Updating CREOLE Plugin Configuration...<br>Currently Updating: " + p.getName());
                            try {
                                File downloaded = File.createTempFile("gate-plugin", ".zip");
                                downloadFile(p.getName(), p.downloadURL, downloaded);
                                File renamed = new File(getUserPluginsHome(), "renamed-" + System.currentTimeMillis());
                                if (!p.dir.renameTo(renamed)) {
                                    failed.add(p);
                                } else {
                                    Files.rmdir(renamed);
                                    expander.setSrc(downloaded);
                                    expander.execute();
                                    if (!downloaded.delete()) downloaded.deleteOnExit();
                                }
                            } catch (IOException ex) {
                                ex.printStackTrace();
                                failed.add(p);
                            }
                        }
                        progressPanel.valueIncrement();
                    }
                    it = availableModel.data.iterator();
                    while (it.hasNext()) {
                        CreolePlugin p = it.next();
                        if (p.install) {
                            progressPanel.messageChanged("Updating CREOLE Plugin Configuration...<br>Currently Installing: " + p.getName());
                            try {
                                File downloaded = File.createTempFile("gate-plugin", ".zip");
                                downloadFile(p.getName(), p.downloadURL, downloaded);
                                expander.setSrc(downloaded);
                                expander.execute();
                                if (!downloaded.delete()) downloaded.deleteOnExit();
                            } catch (IOException ex) {
                                ex.printStackTrace();
                                failed.add(p);
                            }
                            progressPanel.valueIncrement();
                        }
                    }
                    if (failed.size() > 0) JOptionPane.showMessageDialog(PluginUpdateManager.this, "<html><body style='width: 350px;'><b>Installation of " + failed.size() + " plugins failed!</b><br><br>" + "Try unloading all plugins and then restarting GATE before trying to install or update plugins.</body></html>", PluginUpdateManager.this.getTitle(), JOptionPane.ERROR_MESSAGE);
                }
                progressPanel.messageChanged("Updating CREOLE Plugin Configuration...");
                Set<URL> failedPlugins = installed.updateAvailablePlugins();
                if (!failedPlugins.isEmpty()) {
                    JOptionPane.showMessageDialog(PluginUpdateManager.this, "<html><body style='width: 350px;'><b>Loading of " + failedPlugins.size() + " plugins failed!</b><br><br>" + "See the message pane for more details.</body></html>", PluginUpdateManager.this.getTitle(), JOptionPane.ERROR_MESSAGE);
                }
                loadData();
            }
        }.start();
    }

    @Override
    public void dispose() {
        MainFrame.getGuiRoots().remove(this);
        super.dispose();
    }

    public PluginUpdateManager(JFrame owner) {
        super(owner, true);
        this.owner = owner;
        Map<String, String> sites = Gate.getUserConfig().getMap(GATE_UPDATE_SITES);
        for (Map.Entry<String, String> site : sites.entrySet()) {
            try {
                updateSites.add(new RemoteUpdateSite(site.getValue().substring(1), new URI(site.getKey()), site.getValue().charAt(0) == '1'));
            } catch (URISyntaxException e) {
                e.printStackTrace();
            }
        }
        if (defaultUpdateSites.length % 2 == 0) {
            for (int i = 0; i < defaultUpdateSites.length; ++i) {
                try {
                    RemoteUpdateSite rus = new RemoteUpdateSite(defaultUpdateSites[i], new URI(defaultUpdateSites[++i]), false);
                    if (!updateSites.contains(rus)) updateSites.add(rus);
                } catch (URISyntaxException e) {
                    e.printStackTrace();
                }
            }
        }
        setTitle("CREOLE Plugin Manager");
        setDefaultCloseOperation(DISPOSE_ON_CLOSE);
        setLayout(new BorderLayout());
        panel.setBorder(BorderFactory.createEmptyBorder(5, 5, 5, 5));
        panel.add(tabs, BorderLayout.CENTER);
        tabs.addTab("Installed Plugins", new AvailableIcon(20, 20), installed);
        tabs.addTab("Available Updates", new UpdatesIcon(20, 20), buildUpdates());
        tabs.addTab("Available to Install", new DownloadIcon(20, 20), buildAvailable());
        tabs.addTab("Configuration", new AdvancedIcon(20, 20), buildConfig());
        tabs.setDisabledIconAt(1, new ImageIcon(GrayFilter.createDisabledImage((new UpdatesIcon(20, 20)).getImage())));
        tabs.setDisabledIconAt(2, new ImageIcon(GrayFilter.createDisabledImage((new DownloadIcon(20, 20)).getImage())));
        tabs.setEnabledAt(1, false);
        tabs.setEnabledAt(2, false);
        JPanel pnlButtons = new JPanel();
        pnlButtons.setLayout(new BoxLayout(pnlButtons, BoxLayout.X_AXIS));
        pnlButtons.setBorder(BorderFactory.createEmptyBorder(5, 0, 0, 0));
        JButton btnApply = new JButton("Apply All");
        getRootPane().setDefaultButton(btnApply);
        btnApply.addActionListener(new ActionListener() {

            @Override
            public void actionPerformed(ActionEvent e) {
                PluginUpdateManager.this.applyChanges();
            }
        });
        Action cancelAction = new AbstractAction("Close") {

            @Override
            public void actionPerformed(ActionEvent e) {
                boolean changes = false;
                for (CreolePlugin p : availableModel.data) {
                    changes = changes || p.install;
                    if (changes) break;
                }
                if (!changes) {
                    for (CreolePlugin p : updatesModel.data) {
                        changes = changes || p.install;
                        if (changes) break;
                    }
                }
                if (!changes) changes = installed.unsavedChanges();
                if (changes && JOptionPane.showConfirmDialog(PluginUpdateManager.this, "<html><body style='width: 350px;'><b>Changes Have Not Yet Been Applied!</b><br><br>" + "Would you like to apply your changes now?</body></html>", "CREOLE Plugin Manager", JOptionPane.YES_NO_OPTION, JOptionPane.QUESTION_MESSAGE) == JOptionPane.OK_OPTION) {
                    applyChanges();
                }
                PluginUpdateManager.this.setVisible(false);
            }
        };
        JButton btnCancel = new JButton(cancelAction);
        Action helpAction = new AbstractAction("Help") {

            @Override
            public void actionPerformed(ActionEvent e) {
                MainFrame.getInstance().showHelpFrame("sec:howto:plugins", "gate.gui.creole.PluginUpdateManager");
            }
        };
        JButton btnHelp = new JButton(helpAction);
        pnlButtons.add(btnHelp);
        pnlButtons.add(Box.createHorizontalGlue());
        pnlButtons.add(btnApply);
        pnlButtons.add(Box.createHorizontalStrut(5));
        pnlButtons.add(btnCancel);
        panel.add(pnlButtons, BorderLayout.SOUTH);
        add(panel, BorderLayout.CENTER);
        getRootPane().registerKeyboardAction(cancelAction, KeyStroke.getKeyStroke(KeyEvent.VK_ESCAPE, 0), JComponent.WHEN_IN_FOCUSED_WINDOW);
        getRootPane().registerKeyboardAction(helpAction, KeyStroke.getKeyStroke(KeyEvent.VK_F1, 0), JComponent.WHEN_IN_FOCUSED_WINDOW);
        pack();
        Dimension screenSize = getGraphicsConfiguration().getBounds().getSize();
        Dimension dialogSize = getPreferredSize();
        int width = dialogSize.width > screenSize.width ? screenSize.width * 3 / 4 : dialogSize.width;
        int height = dialogSize.height > screenSize.height ? screenSize.height * 2 / 3 : dialogSize.height;
        setSize(width, height);
        validate();
        setLocationRelativeTo(owner);
    }

    private Component buildUpdates() {
        XJTable tblUpdates = new XJTable(updatesModel);
        tblUpdates.getColumnModel().getColumn(0).setCellRenderer(new CheckBoxTableCellRenderer());
        tblUpdates.setSortable(false);
        tblUpdates.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);
        tblUpdates.getColumnModel().getColumn(0).setMaxWidth(100);
        tblUpdates.getColumnModel().getColumn(2).setMaxWidth(100);
        tblUpdates.getColumnModel().getColumn(3).setMaxWidth(100);
        tblUpdates.getColumnModel().getColumn(1).setCellEditor(new JTextPaneTableCellRenderer());
        tblUpdates.setSortable(true);
        tblUpdates.setSortedColumn(1);
        Collator collator = Collator.getInstance(Locale.ENGLISH);
        collator.setStrength(Collator.TERTIARY);
        tblUpdates.setComparator(1, collator);
        JScrollPane scroller = new JScrollPane(tblUpdates);
        scroller.setVerticalScrollBarPolicy(JScrollPane.VERTICAL_SCROLLBAR_ALWAYS);
        return scroller;
    }

    private Component buildAvailable() {
        final XJTable tblAvailable = new XJTable();
        tblAvailable.setModel(availableModel);
        tblAvailable.getColumnModel().getColumn(0).setCellRenderer(new CheckBoxTableCellRenderer());
        tblAvailable.setSortable(false);
        tblAvailable.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);
        tblAvailable.getColumnModel().getColumn(0).setMaxWidth(100);
        tblAvailable.getColumnModel().getColumn(2).setMaxWidth(100);
        tblAvailable.getColumnModel().getColumn(1).setCellEditor(new JTextPaneTableCellRenderer());
        tblAvailable.addMouseListener(new MouseAdapter() {

            public void mousePressed(MouseEvent e) {
                process(e);
            }

            public void mouseReleased(MouseEvent e) {
                process(e);
            }

            public void mouseClicked(MouseEvent e) {
                process(e);
            }

            private void process(final MouseEvent e) {
                final int column = tblAvailable.columnAtPoint(e.getPoint());
                if (column == 1) {
                    SwingUtilities.invokeLater(new Runnable() {

                        @Override
                        public void run() {
                            try {
                                Robot robot = new Robot();
                                if (e.getID() == MouseEvent.MOUSE_PRESSED || e.getID() == MouseEvent.MOUSE_CLICKED) robot.mousePress(InputEvent.BUTTON1_MASK);
                                if (e.getID() == MouseEvent.MOUSE_RELEASED || e.getID() == MouseEvent.MOUSE_CLICKED) robot.mouseRelease(InputEvent.BUTTON1_MASK);
                            } catch (AWTException e) {
                                e.printStackTrace();
                            }
                        }
                    });
                }
            }
        });
        tblAvailable.setSortable(true);
        tblAvailable.setSortedColumn(1);
        Collator collator = Collator.getInstance(Locale.ENGLISH);
        collator.setStrength(Collator.TERTIARY);
        tblAvailable.setComparator(1, collator);
        JScrollPane scrollerAvailable = new JScrollPane(tblAvailable);
        scrollerAvailable.setVerticalScrollBarPolicy(JScrollPane.VERTICAL_SCROLLBAR_ALWAYS);
        return scrollerAvailable;
    }

    private Component buildConfig() {
        JPanel pnlUpdateSites = new JPanel(new BorderLayout());
        pnlUpdateSites.setBorder(BorderFactory.createTitledBorder("Plugin Repositories:"));
        final XJTable tblSites = new XJTable(sitesModel);
        tblSites.getColumnModel().getColumn(0).setCellRenderer(new IconTableCellRenderer());
        tblSites.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);
        JScrollPane scroller = new JScrollPane(tblSites);
        scroller.setVerticalScrollBarPolicy(JScrollPane.VERTICAL_SCROLLBAR_ALWAYS);
        pnlUpdateSites.add(scroller, BorderLayout.CENTER);
        final JPanel pnlEdit = new JPanel(new SpringLayout());
        final JTextField txtName = new JTextField(20);
        final JTextField txtURL = new JTextField(20);
        pnlEdit.add(new JLabel("Name: "));
        pnlEdit.add(txtName);
        pnlEdit.add(new JLabel("URL: "));
        pnlEdit.add(txtURL);
        SpringUtilities.makeCompactGrid(pnlEdit, 2, 2, 6, 6, 6, 6);
        JButton btnAdd = new JButton(new AddIcon(24, 24));
        btnAdd.addActionListener(new ActionListener() {

            @Override
            public void actionPerformed(ActionEvent e) {
                txtName.setText("");
                txtURL.setText("");
                final JOptionPane options = new JOptionPane(pnlEdit, JOptionPane.QUESTION_MESSAGE, JOptionPane.OK_CANCEL_OPTION, new UpdateSiteIcon(48, 48));
                final JDialog dialog = new JDialog(PluginUpdateManager.this, "Plugin Repository Info", true);
                options.addPropertyChangeListener(new PropertyChangeListener() {

                    public void propertyChange(PropertyChangeEvent e) {
                        if (options.getValue().equals(JOptionPane.UNINITIALIZED_VALUE)) return;
                        String prop = e.getPropertyName();
                        if (dialog.isVisible() && (e.getSource() == options) && (prop.equals(JOptionPane.VALUE_PROPERTY))) {
                            if (((Integer) options.getValue()).intValue() == JOptionPane.OK_OPTION) {
                                if (txtName.getText().trim().equals("")) {
                                    txtName.requestFocusInWindow();
                                    options.setValue(JOptionPane.UNINITIALIZED_VALUE);
                                    return;
                                }
                                if (txtURL.getText().trim().equals("")) {
                                    txtURL.requestFocusInWindow();
                                    options.setValue(JOptionPane.UNINITIALIZED_VALUE);
                                    return;
                                }
                            }
                            dialog.setVisible(false);
                        }
                    }
                });
                dialog.setContentPane(options);
                dialog.setDefaultCloseOperation(JDialog.DISPOSE_ON_CLOSE);
                dialog.pack();
                dialog.setLocationRelativeTo(PluginUpdateManager.this);
                dialog.setVisible(true);
                if (((Integer) options.getValue()).intValue() != JOptionPane.OK_OPTION) return;
                if (txtName.getText().trim().equals("")) return;
                if (txtURL.getText().trim().equals("")) return;
                dialog.dispose();
                try {
                    updateSites.add(new RemoteUpdateSite(txtName.getText().trim(), new URI(txtURL.getText().trim()), true));
                    showProgressPanel(true);
                    saveConfig();
                    loadData();
                } catch (Exception ex) {
                    ex.printStackTrace();
                }
            }
        });
        final JButton btnRemove = new JButton(new RemoveIcon(24, 24));
        btnRemove.setEnabled(false);
        btnRemove.setDisabledIcon(new ImageIcon(GrayFilter.createDisabledImage((new RemoveIcon(24, 24)).getImage())));
        btnRemove.addActionListener(new ActionListener() {

            @Override
            public void actionPerformed(ActionEvent e) {
                showProgressPanel(true);
                int row = tblSites.getSelectedRow();
                if (row == -1) return;
                row = tblSites.rowViewToModel(row);
                updateSites.remove(row);
                saveConfig();
                loadData();
            }
        });
        final JButton btnEdit = new JButton(new EditIcon(24, 24));
        btnEdit.setDisabledIcon(new ImageIcon(GrayFilter.createDisabledImage((new EditIcon(24, 24)).getImage())));
        btnEdit.setEnabled(false);
        btnEdit.addActionListener(new ActionListener() {

            @Override
            public void actionPerformed(ActionEvent e) {
                int row = tblSites.getSelectedRow();
                if (row == -1) return;
                row = tblSites.rowViewToModel(row);
                RemoteUpdateSite site = updateSites.get(row);
                txtName.setText(site.name);
                txtURL.setText(site.uri.toString());
                if (JOptionPane.showConfirmDialog(PluginUpdateManager.this, pnlEdit, "Update Site Info", JOptionPane.OK_CANCEL_OPTION, JOptionPane.QUESTION_MESSAGE, new UpdateSiteIcon(48, 48)) != JOptionPane.OK_OPTION) return;
                if (txtName.getText().trim().equals("")) return;
                if (txtURL.getText().trim().equals("")) return;
                try {
                    URI url = new URI(txtURL.getText().trim());
                    if (!url.equals(site.uri)) {
                        site.uri = url;
                        site.plugins = null;
                    }
                    site.name = txtName.getText().trim();
                    site.valid = null;
                    showProgressPanel(true);
                    saveConfig();
                    loadData();
                } catch (Exception ex) {
                    ex.printStackTrace();
                }
            }
        });
        final JButton btnRefresh = new JButton(new RefreshIcon(24, 24));
        btnRefresh.setDisabledIcon(new ImageIcon(GrayFilter.createDisabledImage((new RefreshIcon(24, 24)).getImage())));
        btnRefresh.setEnabled(false);
        btnRefresh.addActionListener(new ActionListener() {

            @Override
            public void actionPerformed(ActionEvent arg0) {
                int row = tblSites.getSelectedRow();
                if (row == -1) return;
                row = tblSites.rowViewToModel(row);
                RemoteUpdateSite site = updateSites.get(row);
                site.plugins = null;
                showProgressPanel(true);
                saveConfig();
                loadData();
            }
        });
        tblSites.getSelectionModel().addListSelectionListener(new ListSelectionListener() {

            @Override
            public void valueChanged(ListSelectionEvent e) {
                if (e.getValueIsAdjusting()) return;
                boolean enable = (tblSites.getSelectedRow() != -1);
                btnRemove.setEnabled(enable);
                btnEdit.setEnabled(enable);
                btnRefresh.setEnabled(enable);
            }
        });
        JToolBar toolbar = new JToolBar(JToolBar.VERTICAL);
        toolbar.setFloatable(false);
        toolbar.add(btnAdd);
        toolbar.add(btnRemove);
        toolbar.add(btnRefresh);
        toolbar.add(btnEdit);
        pnlUpdateSites.add(toolbar, BorderLayout.EAST);
        JToolBar pnlUserPlugins = new JToolBar(JToolBar.HORIZONTAL);
        pnlUserPlugins.setOpaque(false);
        pnlUserPlugins.setFloatable(false);
        pnlUserPlugins.setLayout(new BoxLayout(pnlUserPlugins, BoxLayout.X_AXIS));
        pnlUserPlugins.setBorder(BorderFactory.createEmptyBorder(2, 2, 2, 2));
        getUserPluginsHome();
        final JTextField txtUserPlugins = new JTextField(userPluginDir == null ? "" : userPluginDir.getAbsolutePath());
        txtUserPlugins.setEditable(false);
        JButton btnUserPlugins = new JButton(new OpenFileIcon(24, 24));
        btnUserPlugins.addActionListener(new ActionListener() {

            @Override
            public void actionPerformed(ActionEvent e) {
                XJFileChooser fileChooser = MainFrame.getFileChooser();
                fileChooser.setMultiSelectionEnabled(false);
                fileChooser.setFileSelectionMode(JFileChooser.DIRECTORIES_ONLY);
                fileChooser.setFileFilter(fileChooser.getAcceptAllFileFilter());
                fileChooser.setResource(GATE_USER_PLUGINS);
                if (fileChooser.showOpenDialog(PluginUpdateManager.this) == JFileChooser.APPROVE_OPTION) {
                    userPluginDir = fileChooser.getSelectedFile();
                    if (!userPluginDir.exists()) {
                        JOptionPane.showMessageDialog(owner, "<html><body style='width: 350px;'><b>Selected Folder Doesn't Exist!</b><br><br>" + "In order to install new CREOLE plugins you must choose a user plugins folder, " + "which exists and is writable.", "CREOLE Plugin Manager", JOptionPane.ERROR_MESSAGE);
                        return;
                    }
                    if (!userPluginDir.isDirectory()) {
                        JOptionPane.showMessageDialog(owner, "<html><body style='width: 350px;'><b>You Selected A File Instead Of A Folder!</b><br><br>" + "In order to install new CREOLE plugins you must choose a user plugins folder, " + "which exists and is writable.", "CREOLE Plugin Manager", JOptionPane.ERROR_MESSAGE);
                        return;
                    }
                    if (!userPluginDir.canWrite()) {
                        JOptionPane.showMessageDialog(owner, "<html><body style='width: 350px;'><b>Selected Folder Is Read Only!</b><br><br>" + "In order to install new CREOLE plugins you must choose a user plugins folder, " + "which exists and is writable.", "CREOLE Plugin Manager", JOptionPane.ERROR_MESSAGE);
                        return;
                    }
                    txtUserPlugins.setText(userPluginDir.getAbsolutePath());
                    saveConfig();
                    loadData();
                }
            }
        });
        pnlUserPlugins.setBorder(BorderFactory.createTitledBorder("User Plugin Directory: "));
        pnlUserPlugins.add(txtUserPlugins);
        pnlUserPlugins.add(btnUserPlugins);
        JPanel pnlSuppress = new JPanel();
        pnlSuppress.setLayout(new BoxLayout(pnlSuppress, BoxLayout.X_AXIS));
        pnlSuppress.setBorder(BorderFactory.createTitledBorder("Suppress Warning Messages:"));
        final JCheckBox chkUserPlugins = new JCheckBox("User Plugin Directory Not Set", Gate.getUserConfig().getBoolean(SUPPRESS_USER_PLUGINS));
        pnlSuppress.add(chkUserPlugins);
        pnlSuppress.add(Box.createHorizontalStrut(10));
        final JCheckBox chkUpdateInsatlled = new JCheckBox("Update Of Installed Plugin", Gate.getUserConfig().getBoolean(SUPPRESS_UPDATE_INSTALLED));
        pnlSuppress.add(chkUpdateInsatlled);
        ActionListener chkListener = new ActionListener() {

            @Override
            public void actionPerformed(ActionEvent arg0) {
                Gate.getUserConfig().put(SUPPRESS_USER_PLUGINS, chkUserPlugins.isSelected());
                Gate.getUserConfig().put(SUPPRESS_UPDATE_INSTALLED, chkUpdateInsatlled.isSelected());
            }
        };
        chkUpdateInsatlled.addActionListener(chkListener);
        chkUserPlugins.addActionListener(chkListener);
        JPanel panel = new JPanel(new BorderLayout());
        panel.add(pnlUpdateSites, BorderLayout.CENTER);
        panel.add(pnlUserPlugins, BorderLayout.NORTH);
        panel.add(pnlSuppress, BorderLayout.SOUTH);
        panel.setBorder(BorderFactory.createEmptyBorder(5, 5, 5, 5));
        return panel;
    }

    /**
   * Download a file from a URL into a local file while updating the progress
   * panel so the user knows how far through we are
   *
   * @param name
   *          the name of the plugin for the progress feedback
   * @param url
   *          the URL to download from
   * @param file
   *          the file to save into
   * @throws IOException
   *           if any IO related problems occur
   */
    private void downloadFile(String name, URL url, File file) throws IOException {
        InputStream in = null;
        FileOutputStream out = null;
        try {
            URLConnection conn = url.openConnection();
            conn.setConnectTimeout(10000);
            conn.setReadTimeout(10000);
            int expectedSize = conn.getContentLength();
            progressPanel.downloadStarting(name, expectedSize == -1);
            int downloaded = 0;
            byte[] buf = new byte[1024];
            int length;
            in = conn.getInputStream();
            out = new FileOutputStream(file);
            while ((in != null) && ((length = in.read(buf)) != -1)) {
                downloaded += length;
                out.write(buf, 0, length);
                if (expectedSize != -1) progressPanel.downloadProgress((downloaded * 100) / expectedSize);
            }
            out.flush();
        } finally {
            progressPanel.downloadFinished();
            if (out != null) out.close();
            if (in != null) in.close();
        }
    }

    @Override
    public void setVisible(boolean visible) {
        if (visible) {
            MainFrame.getGuiRoots().add(this);
            tabs.setSelectedIndex(0);
            installed.reInit();
            loadData();
            if (userPluginDir == null && !Gate.getUserConfig().getBoolean(SUPPRESS_USER_PLUGINS)) {
                JOptionPane.showMessageDialog(owner, "<html><body style='width: 350px;'><b>The user plugin folder has not yet been configured!</b><br><br>" + "In order to install new CREOLE plugins you must choose a user plugins folder. " + "This can be achieved from the Configuration tab of the CREOLE Plugin Manager.", "CREOLE Plugin Manager", JOptionPane.INFORMATION_MESSAGE, new UserPluginIcon(48, 48));
            }
        }
        super.setVisible(visible);
        dispose();
    }

    private static class PluginTableModel extends AbstractTableModel {

        private int columns;

        private List<CreolePlugin> data = new ArrayList<CreolePlugin>();

        public PluginTableModel(int columns) {
            this.columns = columns;
        }

        @Override
        public int getColumnCount() {
            return columns;
        }

        @Override
        public int getRowCount() {
            return data.size();
        }

        @Override
        public Object getValueAt(int row, int column) {
            CreolePlugin plugin = data.get(row);
            switch(column) {
                case 0:
                    return plugin.install;
                case 1:
                    return "<html><body>" + (plugin.getHelpURL() != null ? "<a href=\"" + plugin.getHelpURL() + "\">" + plugin.getName() + "</a>" : plugin.getName()) + plugin.compatabilityInfo() + (plugin.description != null ? "<br><span style='font-size: 80%;'>" + plugin.description + "</span>" : "") + "</body></html>";
                case 2:
                    return plugin.version;
                case 3:
                    return plugin.installed;
                default:
                    return null;
            }
        }

        @Override
        public boolean isCellEditable(int row, int column) {
            if (column > 1) return false;
            return data.get(row).compatible;
        }

        @Override
        public void setValueAt(Object value, int row, int column) {
            if (column != 0) return;
            CreolePlugin plugin = data.get(row);
            plugin.install = (Boolean) value;
        }

        @Override
        public String getColumnName(int column) {
            switch(column) {
                case 0:
                    return "<html><body style='padding: 2px; text-align: center;'>Install</body></html>";
                case 1:
                    return "<html><body style='padding: 2px; text-align: center;'>Plugin Name</body></html>";
                case 2:
                    return "<html><body style='padding: 2px; text-align: center;'>Available</body></html>";
                case 3:
                    return "<html><body style='padding: 2px; text-align: center;'>Installed</body></html>";
                default:
                    return null;
            }
        }

        @Override
        public Class<?> getColumnClass(int column) {
            switch(column) {
                case 0:
                    return Boolean.class;
                case 1:
                    return String.class;
                case 2:
                    return String.class;
                case 3:
                    return String.class;
                default:
                    return null;
            }
        }

        public void dataChanged() {
            fireTableDataChanged();
        }
    }

    private static class Expander extends Expand {

        public Expander() {
            setProject(new Project());
            getProject().init();
            setTaskType("unzip");
            setTaskName("unzip");
            setOwningTarget(new Target());
        }
    }

    private class UpdateSiteModel extends AbstractTableModel {

        private transient Icon icoSite = new UpdateSiteIcon(32, 32);

        private transient Icon icoInvalid = new InvalidIcon(32, 32);

        private transient Icon icoGATE = new GATEUpdateSiteIcon(32, 32);

        @Override
        public String getColumnName(int column) {
            switch(column) {
                case 0:
                    return "";
                case 1:
                    return "<html><body style='padding: 2px; text-align: center;'>Enabled</body></html>";
                case 2:
                    return "<html><body style='padding: 2px; text-align: center;'>Repository Info</body></html>";
                default:
                    return null;
            }
        }

        @Override
        public Class<?> getColumnClass(int column) {
            switch(column) {
                case 0:
                    return Icon.class;
                case 1:
                    return Boolean.class;
                case 2:
                    return String.class;
                default:
                    return null;
            }
        }

        @Override
        public boolean isCellEditable(int row, int column) {
            return column == 1;
        }

        @Override
        public void setValueAt(Object value, int row, int column) {
            RemoteUpdateSite site = updateSites.get(row);
            site.enabled = (Boolean) value;
            saveConfig();
            if (site.enabled) {
                showProgressPanel(true);
                loadData();
            } else {
                availableModel.data.removeAll(site.getCreolePlugins());
                updatesModel.data.removeAll(site.getCreolePlugins());
                availableModel.dataChanged();
                updatesModel.dataChanged();
                tabs.setEnabledAt(1, updatesModel.getRowCount() > 0);
            }
        }

        public void dataChanged() {
            fireTableDataChanged();
        }

        @Override
        public int getColumnCount() {
            return 3;
        }

        @Override
        public int getRowCount() {
            return updateSites.size();
        }

        @Override
        public Object getValueAt(int row, int column) {
            RemoteUpdateSite site = updateSites.get(row);
            switch(column) {
                case 0:
                    if (site.valid != null && !site.valid) return icoInvalid;
                    if (site.uri.toString().startsWith("http://gate.ac.uk")) return icoGATE;
                    return icoSite;
                case 1:
                    return site.enabled;
                case 2:
                    return "<html><body>" + site.name + "<br><span style='font-size: 80%;'>" + site.uri + "</span></body></html>";
                default:
                    return null;
            }
        }
    }
}
