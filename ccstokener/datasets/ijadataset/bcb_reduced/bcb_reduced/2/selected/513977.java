package org.tranche.gui.module;

import org.tranche.gui.util.GUIUtil;
import org.tranche.gui.*;
import org.tranche.util.PreferencesUtil;
import java.awt.Color;
import java.awt.Component;
import java.awt.Dimension;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.Insets;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.net.URL;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import javax.swing.JButton;
import javax.swing.JFileChooser;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JTable;
import javax.swing.JTextArea;
import javax.swing.ListSelectionModel;
import javax.swing.SwingUtilities;
import javax.swing.table.TableCellRenderer;
import org.tranche.exceptions.TodoException;
import org.tranche.get.GetFileTool;
import org.tranche.hash.BigHash;
import org.tranche.util.IOUtil;

/**
 * Module panel used with generic popup frame to create the Module Manager.
 * @author Bryan Smith <bryanesmith at gmail.com>
 */
public class ModulePanel extends JPanel {

    private static boolean moduleListLoaded = false;

    private static final File moduleListFile = new File(GUIUtil.getGUIDirectory() + File.separator + "modules.list");

    private JFrame frame = null;

    public static final Dimension RECOMMENDED_DIMENSION = new Dimension(750, 600);

    private JButton loadFromDiskButton = null, loadFromNetworkButton = null, infoButton = null, saveButton = null, removeModuleButton = null;

    private final int MARGIN = 12;

    private GenericTable installedModulesTable, networkModulesTable;

    private ModulesTableModel installedModulesModel;

    private RemoteModulesTableModel networkModulesModel;

    private JLabel installFromServerLabel;

    private JTextArea installFromServerDescription;

    public ModulePanel(List<TrancheModule> modules) {
        this.infoButton = new JButton("About Modules");
        this.infoButton.setFont(Styles.FONT_12PT);
        this.saveButton = new JButton("Save Preferences");
        this.saveButton.setFont(Styles.FONT_12PT_BOLD);
        this.loadFromDiskButton = new JButton("Load a Module from Disk...");
        this.loadFromDiskButton.setFont(Styles.FONT_12PT_BOLD);
        this.loadFromNetworkButton = new JButton("Install Selected Module from Server");
        this.loadFromNetworkButton.setFont(Styles.FONT_12PT_BOLD);
        this.loadFromNetworkButton.setEnabled(false);
        this.removeModuleButton = new JButton("Remove Selected Module");
        this.removeModuleButton.setFont(Styles.FONT_12PT_BOLD);
        this.removeModuleButton.setEnabled(false);
        String[] h1 = { "Name", "Description", "Enabled" };
        String[] h2 = { "Name", "Description", "File name" };
        this.installedModulesModel = new ModulesTableModel(h1);
        this.networkModulesModel = new RemoteModulesTableModel(h2);
        this.installedModulesTable = new GenericTable(this.installedModulesModel, ListSelectionModel.SINGLE_SELECTION);
        this.installedModulesTable.setBorder(Styles.BORDER_NONE);
        this.installedModulesTable.setBackground(Color.WHITE);
        this.installedModulesTable.setAutoResizeMode(JTable.AUTO_RESIZE_ALL_COLUMNS);
        this.networkModulesTable = new GenericTable(this.networkModulesModel, ListSelectionModel.SINGLE_SELECTION);
        this.networkModulesTable.setBorder(Styles.BORDER_NONE);
        this.networkModulesTable.setBackground(Color.WHITE);
        this.networkModulesTable.setAutoResizeMode(JTable.AUTO_RESIZE_ALL_COLUMNS);
        this.setBackground(Styles.COLOR_BACKGROUND);
        setLayout(new GridBagLayout());
        GridBagConstraints gbc = new GridBagConstraints();
        int row = 0;
        {
            this.installFromServerLabel = new GenericLabel("Install modules from server");
            this.installFromServerLabel.setFont(Styles.FONT_14PT_BOLD);
            this.installFromServerLabel.setBorder(Styles.UNDERLINE_BLACK);
            gbc.gridwidth = GridBagConstraints.REMAINDER;
            gbc.anchor = GridBagConstraints.PAGE_START;
            gbc.weightx = 1;
            gbc.weighty = 0;
            gbc.fill = GridBagConstraints.HORIZONTAL;
            gbc.insets = new Insets((int) (MARGIN * 1.5), MARGIN, 0, MARGIN);
            gbc.gridx = 0;
            gbc.gridy = row++;
            this.add(this.installFromServerLabel, gbc);
            this.installFromServerDescription = new GenericTextArea("Download and install modules available on our server.");
            this.installFromServerDescription.setFont(Styles.FONT_12PT);
            this.installFromServerDescription.setEditable(false);
            this.installFromServerDescription.setOpaque(false);
            this.installFromServerDescription.setBorder(Styles.BORDER_NONE);
            this.installFromServerDescription.setWrapStyleWord(true);
            this.installFromServerDescription.setLineWrap(true);
            gbc.gridwidth = GridBagConstraints.REMAINDER;
            gbc.anchor = GridBagConstraints.PAGE_START;
            gbc.weightx = 1;
            gbc.weighty = 0;
            gbc.fill = GridBagConstraints.HORIZONTAL;
            gbc.insets = new Insets(1, MARGIN, 0, MARGIN);
            gbc.gridx = 0;
            gbc.gridy = row++;
            this.add(this.installFromServerDescription, gbc);
        }
        {
            gbc.gridwidth = GridBagConstraints.REMAINDER;
            gbc.anchor = GridBagConstraints.PAGE_START;
            gbc.weightx = 1;
            gbc.weighty = .5;
            gbc.fill = GridBagConstraints.BOTH;
            gbc.insets = new Insets(MARGIN, MARGIN, 0, MARGIN);
            gbc.gridx = 0;
            gbc.gridy = row++;
            GenericScrollPane pane = new GenericScrollPane(this.networkModulesTable);
            pane.setBackground(Color.GRAY);
            pane.setBorder(Styles.BORDER_BLACK_1);
            pane.setPreferredSize(new Dimension(100, 100));
            GenericScrollBar bar = new GenericScrollBar();
            bar.setBackground(Styles.COLOR_BACKGROUND);
            pane.setVerticalScrollBar(bar);
            this.add(pane, gbc);
        }
        {
            gbc.gridwidth = GridBagConstraints.REMAINDER;
            gbc.anchor = GridBagConstraints.PAGE_START;
            gbc.weightx = 1;
            gbc.weighty = 0;
            gbc.fill = GridBagConstraints.HORIZONTAL;
            gbc.insets = new Insets(MARGIN, MARGIN, 0, MARGIN);
            gbc.gridx = 0;
            gbc.gridy = row++;
            this.add(this.loadFromNetworkButton, gbc);
        }
        {
            this.installFromServerLabel = new GenericLabel("Install modules from disk");
            this.installFromServerLabel.setFont(Styles.FONT_14PT_BOLD);
            this.installFromServerLabel.setBorder(Styles.UNDERLINE_BLACK);
            gbc.gridwidth = GridBagConstraints.REMAINDER;
            gbc.anchor = GridBagConstraints.PAGE_START;
            gbc.weightx = 1;
            gbc.weighty = 0;
            gbc.fill = GridBagConstraints.HORIZONTAL;
            gbc.insets = new Insets((int) (MARGIN * 1.5), MARGIN, 0, MARGIN);
            gbc.gridx = 0;
            gbc.gridy = row++;
            this.add(this.installFromServerLabel, gbc);
            this.installFromServerDescription = new GenericTextArea("Install a module manually. Select \"" + this.infoButton.getText() + "\" for more information.");
            this.installFromServerDescription.setFont(Styles.FONT_12PT);
            this.installFromServerDescription.setEditable(false);
            this.installFromServerDescription.setOpaque(false);
            this.installFromServerDescription.setBorder(Styles.BORDER_NONE);
            this.installFromServerDescription.setWrapStyleWord(true);
            this.installFromServerDescription.setLineWrap(true);
            gbc.gridwidth = GridBagConstraints.REMAINDER;
            gbc.anchor = GridBagConstraints.PAGE_START;
            gbc.weightx = 1;
            gbc.weighty = 0;
            gbc.fill = GridBagConstraints.HORIZONTAL;
            gbc.insets = new Insets(1, MARGIN, 0, MARGIN);
            gbc.gridx = 0;
            gbc.gridy = row++;
            this.add(this.installFromServerDescription, gbc);
            gbc.gridwidth = GridBagConstraints.REMAINDER;
            gbc.anchor = GridBagConstraints.PAGE_START;
            gbc.weightx = 1;
            gbc.weighty = 0;
            gbc.fill = GridBagConstraints.HORIZONTAL;
            gbc.insets = new Insets(MARGIN, MARGIN, 0, MARGIN);
            gbc.gridx = 0;
            gbc.gridy = row++;
            this.add(this.loadFromDiskButton, gbc);
        }
        {
            JLabel label = new GenericLabel("Installed modules");
            label.setFont(Styles.FONT_14PT_BOLD);
            label.setBorder(Styles.UNDERLINE_BLACK);
            gbc.gridwidth = GridBagConstraints.REMAINDER;
            gbc.anchor = GridBagConstraints.PAGE_START;
            gbc.weightx = 1;
            gbc.weighty = 0;
            gbc.fill = GridBagConstraints.HORIZONTAL;
            gbc.insets = new Insets((int) (MARGIN * 1.5), MARGIN, 0, MARGIN);
            gbc.gridx = 0;
            gbc.gridy = row++;
            this.add(label, gbc);
            JTextArea descBox = new GenericTextArea("The modules are located in \"" + TrancheModulesUtil.MODULE_DIRECTORY.getAbsolutePath() + "\".");
            descBox.setFont(Styles.FONT_12PT);
            descBox.setEditable(false);
            descBox.setOpaque(false);
            descBox.setBorder(Styles.BORDER_NONE);
            descBox.setWrapStyleWord(true);
            descBox.setLineWrap(true);
            gbc.gridwidth = GridBagConstraints.REMAINDER;
            gbc.anchor = GridBagConstraints.PAGE_START;
            gbc.weightx = 1;
            gbc.weighty = 0;
            gbc.fill = GridBagConstraints.HORIZONTAL;
            gbc.insets = new Insets(1, MARGIN, 0, MARGIN);
            gbc.gridx = 0;
            gbc.gridy = row++;
            this.add(descBox, gbc);
        }
        {
            gbc.gridwidth = GridBagConstraints.REMAINDER;
            gbc.anchor = GridBagConstraints.PAGE_START;
            gbc.weightx = 1;
            gbc.weighty = .5;
            gbc.fill = GridBagConstraints.BOTH;
            gbc.insets = new Insets(MARGIN, MARGIN, 0, MARGIN);
            gbc.gridx = 0;
            gbc.gridy = row++;
            GenericScrollPane pane = new GenericScrollPane(this.installedModulesTable);
            pane.setBackground(Color.WHITE);
            pane.setBorder(Styles.BORDER_BLACK_1);
            pane.setPreferredSize(new Dimension(100, 100));
            pane.setVerticalScrollBar(new GenericScrollBar());
            this.add(pane, gbc);
            gbc.gridwidth = GridBagConstraints.REMAINDER;
            gbc.anchor = GridBagConstraints.PAGE_START;
            gbc.weightx = 1;
            gbc.weighty = 0;
            gbc.fill = GridBagConstraints.HORIZONTAL;
            gbc.insets = new Insets(MARGIN, MARGIN, 0, MARGIN);
            gbc.gridx = 0;
            gbc.gridy = row++;
            this.add(this.removeModuleButton, gbc);
        }
        {
            gbc.gridwidth = 1;
            gbc.anchor = GridBagConstraints.PAGE_START;
            gbc.weightx = .5;
            gbc.weighty = 0;
            gbc.fill = GridBagConstraints.HORIZONTAL;
            gbc.insets = new Insets(MARGIN * 2, MARGIN, (int) (MARGIN * 1.5), 0);
            gbc.gridx = 0;
            gbc.gridy = row;
            this.add(this.infoButton, gbc);
            gbc.gridwidth = GridBagConstraints.REMAINDER;
            gbc.anchor = GridBagConstraints.PAGE_START;
            gbc.weightx = .5;
            gbc.weighty = 0;
            gbc.fill = GridBagConstraints.HORIZONTAL;
            gbc.insets = new Insets(MARGIN * 2, MARGIN, (int) (MARGIN * 1.5), MARGIN);
            gbc.gridx = 1;
            gbc.gridy = row;
            this.add(this.saveButton, gbc);
        }
        this.infoButton.addActionListener(new ActionListener() {

            public void actionPerformed(ActionEvent e) {
                Thread t = new Thread() {

                    public void run() {
                        GUIUtil.displayURL("http://tranche.proteomecommons.org/dev/modules.html");
                    }
                };
                t.setDaemon(true);
                t.start();
            }
        });
        this.saveButton.addActionListener(new ActionListener() {

            public void actionPerformed(ActionEvent e) {
                Thread t = new Thread() {

                    public void run() {
                        try {
                            saveModulesToPreferences();
                            GenericOptionPane.showMessageDialog(frame, "Your preferences were saved.", "Preferences saved", JOptionPane.INFORMATION_MESSAGE);
                            return;
                        } catch (Exception ex) {
                            ErrorFrame ef = new ErrorFrame();
                            ef.show(ex, frame);
                        }
                    }
                };
                t.setDaemon(true);
                t.start();
            }
        });
        this.loadFromDiskButton.addActionListener(new ActionListener() {

            public void actionPerformed(ActionEvent e) {
                Thread t = new Thread() {

                    public void run() {
                        loadModuleFromDisk();
                    }
                };
                t.setDaemon(true);
                t.start();
            }
        });
        this.loadFromNetworkButton.addActionListener(new ActionListener() {

            public void actionPerformed(ActionEvent e) {
                Thread t = new Thread() {

                    public void run() {
                        loadModuleFromNetwork();
                    }
                };
                t.setDaemon(true);
                t.start();
            }
        });
        this.removeModuleButton.addActionListener(new ActionListener() {

            public void actionPerformed(ActionEvent e) {
                Thread t = new Thread() {

                    public void run() {
                        removeSelectedModule();
                    }
                };
                t.setDaemon(true);
                t.start();
            }
        });
        this.networkModulesTable.addMouseListener(new TableMouseListener(this.loadFromNetworkButton, this.networkModulesTable));
        this.installedModulesTable.addMouseListener(new TableMouseListener(this.removeModuleButton, this.installedModulesTable));
        clearTablesAndShowModules(modules);
    }

    /**
     * Helper method to load all modules in tables.
     * @param modules Installed modules only. Network modules will be discovered.
     */
    private void clearTablesAndShowModules(List<TrancheModule> modules) {
        this.installedModulesModel.clear();
        for (TrancheModule module : modules) {
            if (module.isInstalled) {
                this.installedModulesModel.addRow(new TableModelRow(module));
            }
        }
        getModulesInfoFromNetwork();
        this.installedModulesModel.fireTableDataChanged();
        this.networkModulesModel.fireTableDataChanged();
    }

    /**
     * Helper method to load a module
     */
    private void loadModuleFromDisk() {
        final IndeterminateProgressBar progress = new IndeterminateProgressBar("Loading module...");
        progress.setLocationRelativeTo(frame);
        try {
            File jar = null;
            JFileChooser jfc = GUIUtil.makeNewFileChooser();
            jfc.setFileSelectionMode(jfc.FILES_ONLY);
            jfc.setDialogTitle("Select Module (*.jar)");
            GenericFileFilter filter = new GenericFileFilter(".jar", "Tranche Module (*.jar)");
            jfc.setFileFilter(filter);
            if (PreferencesUtil.get(PreferencesUtil.PREF_DOWNLOAD_FILE) != null) {
                jfc.setCurrentDirectory(new File(PreferencesUtil.get(PreferencesUtil.PREF_DOWNLOAD_FILE)));
            }
            int action = jfc.showOpenDialog(frame);
            if (action == FileChooser.APPROVE_OPTION) {
                jar = jfc.getSelectedFile();
            } else {
                return;
            }
            File dest = new File(TrancheModulesUtil.MODULE_DIRECTORY.getAbsolutePath(), jar.getName());
            if (dest.exists()) {
                int n = GenericOptionPane.showConfirmDialog(frame, "A module with that name already exists. Replace and continue?", "Module Exists", JOptionPane.YES_NO_OPTION);
                if (n == JOptionPane.NO_OPTION) {
                    progress.stop();
                    return;
                }
                IOUtil.safeDelete(dest);
            }
            progress.start();
            IOUtil.copyFile(jar, dest);
            progress.stop();
            TrancheModulesUtil.reloadModules();
            clearTablesAndShowModules(TrancheModulesUtil.getModules());
            saveModulesToPreferences();
            if (TrancheModulesUtil.wasMostRecentModuleLoadedCorrectly()) {
                GenericOptionPane.showMessageDialog(frame, "The module was successfully loaded.", "Module loaded.", JOptionPane.INFORMATION_MESSAGE);
            } else {
                throw new RuntimeException("The module named \"" + jar.getName() + "\" could not be loaded.");
            }
        } catch (Exception ex) {
            progress.stop();
            ErrorFrame ef = new ErrorFrame();
            ef.show(ex, frame);
        }
    }

    /**
     * Remove a module
     */
    private void removeSelectedModule() {
        int row = installedModulesTable.getSelectedRow();
        String jarFilePath = (String) installedModulesModel.getModuleJarPath(row);
        String moduleName = (String) installedModulesModel.getModuleName(row);
        try {
            TrancheModulesUtil.runAnyUninstallHooks(moduleName);
        } catch (Exception ex) {
            System.err.println("Exception raised when running uninstall hook for " + moduleName + ": " + ex.getMessage());
            ex.printStackTrace(System.err);
        }
        File jarFile = new File(jarFilePath);
        IOUtil.safeDelete(jarFile);
        TrancheModulesUtil.reloadModules();
        clearTablesAndShowModules(TrancheModulesUtil.getModules());
        try {
            saveModulesToPreferences();
        } catch (Exception ex) {
        }
    }

    /**
     * Save all information to preferences
     */
    private void saveModulesToPreferences() throws Exception {
        TrancheModulesUtil.setModules(getAllModules());
        PreferencesUtil.save();
    }

    public void loadModuleFromNetwork() {
        final IndeterminateProgressBar downloadAndInstallProgress = new IndeterminateProgressBar("Downloading and installing module...");
        downloadAndInstallProgress.setLocationRelativeTo(frame);
        File jarFile = null;
        try {
            int row = networkModulesTable.getSelectedRow();
            String jarFilename = (String) networkModulesModel.getValueAt(row, 2);
            jarFile = new File(TrancheModulesUtil.MODULE_DIRECTORY, jarFilename.trim());
            if (jarFile.exists()) {
                int n = GenericOptionPane.showConfirmDialog(frame, "A module with that name already exists. Continue and overwrite?", "Module already exists", JOptionPane.YES_NO_OPTION);
                if (n == JOptionPane.NO_OPTION) {
                    return;
                }
            }
            BigHash moduleHash = networkModulesModel.getHashForModule(row);
            downloadAndInstallProgress.start();
            if (true) {
                throw new TodoException();
            }
            try {
                if (!TrancheModulesUtil.MODULE_DIRECTORY.exists()) {
                    TrancheModulesUtil.MODULE_DIRECTORY.mkdirs();
                }
                BigHash hash = moduleHash;
                GetFileTool gft = new GetFileTool();
                gft.setValidate(false);
                gft.setHash(hash);
                gft.setSaveFile(jarFile);
                gft.getFile();
            } catch (Exception ex) {
                ErrorFrame ef = new ErrorFrame();
                ef.show(ex, frame);
                downloadAndInstallProgress.stop();
                return;
            }
            downloadAndInstallProgress.stop();
            try {
                TrancheModulesUtil.reloadModules();
                clearTablesAndShowModules(TrancheModulesUtil.getModules());
                saveModulesToPreferences();
            } catch (Exception ex) {
                System.out.println(ex.getMessage());
                ex.printStackTrace(System.err);
            }
        } catch (Exception e) {
            downloadAndInstallProgress.stop();
            System.err.println(e.getMessage());
            e.printStackTrace(System.err);
            GenericOptionPane.showMessageDialog(frame, "The file could not be download at this time. Please try again later.", "File could not be downloaded", JOptionPane.INFORMATION_MESSAGE);
        }
    }

    public synchronized void getModulesInfoFromNetwork() {
        final IndeterminateProgressBar downloadListProgress = new IndeterminateProgressBar("Loading information from network...");
        downloadListProgress.setLocationRelativeTo(frame);
        networkModulesModel.clear();
        if (!moduleListLoaded) {
            downloadListProgress.start();
            try {
                GetFileTool gft = new GetFileTool();
                gft.setValidate(false);
                gft.setHash(BigHash.createHashFromString(ConfigureTrancheGUI.get(ConfigureTrancheGUI.CATEGORY_GUI, ConfigureTrancheGUI.PROP_MODULE_LIST_HASH)));
                gft.setSaveFile(ModulePanel.moduleListFile);
                gft.getFile();
            } catch (Exception e) {
                System.err.println(e.getMessage());
                try {
                    URL url = new URL(ConfigureTrancheGUI.get(ConfigureTrancheGUI.CATEGORY_GUI, ConfigureTrancheGUI.PROP_MODULE_LIST_URL));
                    IOUtil.setBytes(IOUtil.getBytes(url.openStream()), ModulePanel.moduleListFile);
                } catch (Exception f) {
                    if (!ModulePanel.moduleListFile.exists()) {
                        ErrorFrame ef = new ErrorFrame();
                        ef.show(e, frame);
                        return;
                    }
                }
            } finally {
                moduleListLoaded = true;
                downloadListProgress.stop();
            }
        }
        InputStream in = null;
        try {
            URL moduleList = ModulePanel.moduleListFile.toURI().toURL();
            try {
                in = moduleList.openStream();
            } catch (IOException ex) {
                installFromServerLabel.setText("Module server down");
                installFromServerDescription.setText("Unfortunately, the module server is temporarily down. Sorry for the inconvenience, but feel free to contact us.");
                return;
            }
            String filename, title, description, hashString;
            filename = getNextLineFromList(in);
            while (filename != null) {
                title = getNextLineFromList(in);
                description = getNextLineFromList(in);
                hashString = getNextLineFromList(in);
                boolean isHash = false;
                try {
                    BigHash.createHashFromString(hashString);
                    isHash = true;
                } catch (Exception ex) {
                }
                if (isHash) {
                    String[] row = { title, description, filename, hashString };
                    if (TrancheModulesUtil.getModuleByName(title) == null) {
                        networkModulesModel.addRow(row);
                    }
                }
                filename = getNextLineFromList(in);
            }
        } catch (Exception ex) {
            ErrorFrame ef = new ErrorFrame();
            ef.show(ex, ModulePanel.this.getParent());
        } finally {
            IOUtil.safeClose(in);
            Thread t = new Thread("Update network modules table thread.") {

                public void run() {
                    networkModulesModel.fireTableDataChanged();
                }
            };
            SwingUtilities.invokeLater(t);
        }
    }

    /**
     * Skips comment lines and blank lines.
     */
    private static String getNextLineFromList(InputStream in) throws Exception {
        String next;
        while (true) {
            next = readLine(in);
            if (next == null) {
                return null;
            }
            if (!next.trim().equals("") && !next.trim().startsWith("#")) {
                return next;
            }
        }
    }

    /**
     * Modified from RemoteUtil to work for reading from module list file.
     */
    private static String readLine(InputStream is) throws Exception {
        ByteArrayOutputStream baos = new ByteArrayOutputStream();
        for (int c = is.read(); c != '\n'; c = is.read()) {
            if (c == -1) {
                if (baos.toByteArray().length == 0) {
                    return null;
                } else {
                    break;
                }
            }
            baos.write((char) c);
        }
        String s = new String(baos.toByteArray());
        if (s == null) {
            return null;
        }
        s = s.replace("\\n", "\n");
        s = s.replace("\\\\", "\\");
        return s;
    }

    public List<TrancheModule> getAllModules() {
        List<TrancheModule> modules = new ArrayList();
        modules.addAll(this.installedModulesModel.getModules());
        return modules;
    }

    public void setFrame(JFrame frame) {
        this.frame = frame;
    }

    /**
     * Table model for the modules tables
     */
    private class ModulesTableModel extends SortableTableModel {

        String[] headers;

        List<TableModelRow> rows;

        private ModulesTableModel(String[] headers) {
            this.headers = headers;
            this.rows = new ArrayList();
        }

        public void clear() {
            this.rows = new ArrayList();
        }

        public List<TrancheModule> getModules() {
            List<TrancheModule> modules = new ArrayList();
            for (TableModelRow row : rows) {
                modules.add(row.module);
            }
            return modules;
        }

        public void removeRow(int row) {
            this.rows.remove(row);
            this.fireTableRowsDeleted(row, row);
        }

        public Object getValueAt(int row, int column) {
            switch(column) {
                case 0:
                    return (String) this.rows.get(row).module.name;
                case 1:
                    return (String) this.rows.get(row).module.description;
                case 2:
                    return (Boolean) this.rows.get(row).module.isEnabled;
                default:
                    throw new RuntimeException("Should not get here!");
            }
        }

        public String getModuleJarPath(int row) {
            return TrancheModulesUtil.getJARPathContainingModule(rows.get(row).module.name);
        }

        public String getModuleName(int row) {
            return rows.get(row).module.name;
        }

        public Class getColumnClass(int c) {
            try {
                if (c == 2) {
                    return Class.forName("java.lang.Boolean");
                }
                return Class.forName("java.lang.String");
            } catch (Exception ex) {
                return String.class;
            }
        }

        public int getColumnCount() {
            return headers.length;
        }

        public int getRowCount() {
            return this.rows.size();
        }

        public void sort(int column) {
            for (TableModelRow row : this.rows) {
                row.selectedColumn = column;
            }
            Collections.sort(this.rows);
        }

        public String getColumnName(int col) {
            return headers[col];
        }

        public void addRow(TableModelRow row) {
            this.rows.add(row);
        }

        public boolean isCellEditable(int row, int col) {
            if (col == 2) {
                return true;
            }
            return false;
        }

        public void setValueAt(Object value, int row, int col) {
            if (col != 2) {
                throw new RuntimeException("Only column #2 is editable.");
            }
            rows.get(row).module.isEnabled = (Boolean) value;
            fireTableCellUpdated(row, col);
        }
    }

    /**
     * Abstract a row in a table model. Comparable, but not using interface b/c depends on selected row.
     */
    private class TableModelRow implements Comparable {

        public TrancheModule module;

        public TableModelRow(TrancheModule module) {
            this.module = module;
        }

        public int selectedColumn;

        public int compareTo(Object o) {
            if (o instanceof TableModelRow) {
                TableModelRow row = (TableModelRow) o;
                switch(selectedColumn) {
                    case 0:
                        return this.module.name.compareTo(row.module.name);
                    case 1:
                        return this.module.description.compareTo(row.module.description);
                    case 2:
                        if (row.module.isEnabled && !this.module.isEnabled) {
                            return -1;
                        } else if (!row.module.isEnabled && this.module.isEnabled) {
                            return 1;
                        }
                        return 0;
                    default:
                        throw new RuntimeException("Unknown column " + selectedColumn);
                }
            }
            throw new RuntimeException("Shouldn't get here!");
        }
    }

    /**
     * Table model for the modules tables
     */
    private class RemoteModulesTableModel extends SortableTableModel {

        String[] headers;

        List<String[]> rows;

        private RemoteModulesTableModel(String[] headers) {
            this.headers = headers;
            this.rows = new ArrayList();
        }

        public void clear() {
            this.rows = new ArrayList();
        }

        public Object getValueAt(int row, int column) {
            return (String) this.rows.get(row)[column];
        }

        public Class getColumnClass(int c) {
            try {
                return Class.forName("java.lang.String");
            } catch (Exception ex) {
                return String.class;
            }
        }

        public BigHash getHashForModule(int row) {
            return BigHash.createHashFromString(this.rows.get(row)[3]);
        }

        public int getColumnCount() {
            return headers.length;
        }

        public int getRowCount() {
            return this.rows.size();
        }

        public void sort(int column) {
        }

        public String getColumnName(int col) {
            return headers[col];
        }

        public void addRow(String[] row) {
            this.rows.add(row);
        }

        public void removeRow(int row) {
            this.rows.remove(row);
            this.fireTableRowsDeleted(row, row);
        }

        public boolean isCellEditable(int row, int col) {
            return false;
        }

        public void setValueAt(Object value, int row, int col) {
            fireTableCellUpdated(row, col);
        }
    }

    private class CheckBoxCellRenderer implements TableCellRenderer {

        public CheckBoxCellRenderer() {
            super();
        }

        public Component getTableCellRendererComponent(JTable table, Object value, boolean isSelected, boolean isFocused, int row, int col) {
            boolean marked = (Boolean) value;
            GenericCheckBox rendererComponent = new GenericCheckBox();
            if (marked) {
                rendererComponent.setSelected(true);
            }
            return rendererComponent;
        }
    }

    private class TableMouseListener extends MouseAdapter {

        JButton button;

        JTable table;

        public TableMouseListener(JButton button, JTable table) {
            this.button = button;
            this.table = table;
        }

        public void mouseClicked(MouseEvent e) {
            enable();
        }

        private void enable() {
            if (table.getSelectedRowCount() == 1) {
                this.button.setEnabled(true);
            } else {
                this.button.setEnabled(false);
            }
        }
    }
}
