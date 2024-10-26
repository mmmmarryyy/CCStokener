package com.mockturtlesolutions.snifflib.flatfiletools.workbench;

import com.mockturtlesolutions.snifflib.reposconfig.database.RepositoryStorageXML;
import com.mockturtlesolutions.snifflib.reposconfig.database.RepositoryConnectivity;
import com.mockturtlesolutions.snifflib.reposconfig.database.RepositoryConnectivity;
import com.mockturtlesolutions.snifflib.reposconfig.database.RepositoryStorage;
import com.mockturtlesolutions.snifflib.datatypes.DataSetReader;
import com.mockturtlesolutions.snifflib.datatypes.DataSet;
import com.mockturtlesolutions.snifflib.datatypes.DataSetPanel;
import com.mockturtlesolutions.snifflib.flatfiletools.database.*;
import com.mockturtlesolutions.snifflib.guitools.components.IconifiedDomainNameTextField;
import com.mockturtlesolutions.snifflib.guitools.components.DomainListCellRenderer;
import com.mockturtlesolutions.snifflib.guitools.components.DomainEntry;
import com.mockturtlesolutions.snifflib.guitools.components.IconServer;
import javax.swing.event.ListSelectionEvent;
import javax.swing.event.ListSelectionListener;
import javax.swing.ListSelectionModel;
import javax.swing.event.TableModelListener;
import javax.swing.event.TableModelEvent;
import java.util.Vector;
import java.util.Iterator;
import java.util.Set;
import java.util.HashMap;
import javax.swing.event.ChangeListener;
import javax.swing.event.ChangeEvent;
import javax.swing.JFrame;
import javax.swing.JButton;
import javax.swing.JSpinner;
import javax.swing.JComboBox;
import javax.swing.JPanel;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JOptionPane;
import javax.swing.JScrollPane;
import javax.swing.JRadioButton;
import javax.swing.JTextArea;
import javax.swing.JTextField;
import javax.swing.JFileChooser;
import java.awt.Dimension;
import java.awt.GridLayout;
import java.awt.event.*;
import java.io.*;
import javax.swing.table.DefaultTableCellRenderer;
import javax.swing.DefaultCellEditor;
import java.net.URI;
import java.net.URL;
import javax.swing.Box;
import javax.swing.JScrollPane;
import javax.swing.JTable;
import javax.swing.table.*;
import javax.swing.JCheckBox;
import com.mockturtlesolutions.snifflib.reposconfig.graphical.ReposConfigFrame;
import com.mockturtlesolutions.snifflib.reposconfig.database.RepositoryListener;
import com.mockturtlesolutions.snifflib.reposconfig.database.RepositoryStorageTransferAgent;
import com.mockturtlesolutions.snifflib.reposconfig.database.RepositoryConnectionHandler;
import com.mockturtlesolutions.snifflib.guitools.components.PrefsConfigFrame;
import java.text.DateFormat;
import java.util.Date;
import java.text.SimpleDateFormat;
import java.lang.reflect.Constructor;

public class FlatFileFrame extends JFrame implements FlatFileStorage {

    private ReposConfigFrame repositoryEditor;

    protected FlatFileToolsConfig Config;

    protected FlatFileStorageConnectivity Connection;

    private JTable fileselector;

    private JButton addSources;

    private FileSourceTableModel sourcemodel;

    private JSpinner preheaderlines;

    private JSpinner postheaderlines;

    private FlatFileStorage backingStorage;

    private JTextArea commentTextArea;

    private JTextField createdOnText;

    private JTextField createdByText;

    protected RepositoryConnectionHandler connectionHandler;

    private PrefsConfigFrame prefsEditor;

    private JTextField flatfilesource;

    private JRadioButton isURLButton;

    private RepositoryStorageTransferAgent transferAgent;

    private boolean NoCallbackChangeMode;

    private Vector reposListeners;

    private JTable formatTable;

    private FormatTableModel formatmodel;

    private JTextField fieldDelimiter;

    private JTextField recordDelimiter;

    private JFileChooser chooser;

    private JButton preview;

    private JButton addFormatButton;

    private JButton uploadButton;

    private DataSetPanel previewPanel;

    private JSpinner leastcolumn;

    private JSpinner columns2show;

    private JSpinner leastrow;

    private JSpinner rows2show;

    private JButton inferButton;

    private JButton repositoryView;

    private JSpinner repeatFormatNumber;

    private JTextField singleFormatText;

    private JButton removeFormatButton;

    private JButton editPrefsButton;

    private JCheckBox hasHeaderLineBox;

    private JRadioButton enabledRadio;

    private IconifiedDomainNameTextField nicknameText;

    private IconServer iconServer;

    private FlatFileToolsPrefs Prefs;

    private HashMap map;

    private FormatTableModelListener fmtlistener;

    public FlatFileFrame() {
        super("Specify Your Flat File Data");
        try {
            Class transferAgentClass = this.getStorageTransferAgentClass();
            if (transferAgentClass == null) {
                throw new RuntimeException("Transfer agent class can not be null.");
            }
            Class[] parameterTypes = new Class[] { RepositoryStorage.class };
            Constructor constr = transferAgentClass.getConstructor(parameterTypes);
            Object[] actualValues = new Object[] { this };
            this.transferAgent = (RepositoryStorageTransferAgent) constr.newInstance(actualValues);
        } catch (Exception err) {
            throw new RuntimeException("Unable to instantiate transfer agent.", err);
        }
        this.fmtlistener = new FormatTableModelListener();
        this.map = new HashMap();
        this.NoCallbackChangeMode = false;
        this.setSize(new Dimension(1000, 400));
        this.setLayout(new GridLayout(1, 1));
        this.Config = new FlatFileToolsConfig();
        this.Config.initialize();
        this.connectionHandler = new RepositoryConnectionHandler(this.Config);
        this.Connection = (FlatFileStorageConnectivity) this.connectionHandler.getConnection("default");
        this.Prefs = new FlatFileToolsPrefs();
        this.Prefs.initialize();
        SimpleDateFormat formatter = new SimpleDateFormat("yyyy-MM-dd");
        String formatted_date = formatter.format(new Date());
        this.createdOnText = new JTextField(formatted_date);
        this.createdByText = new JTextField(this.Prefs.getConfigValue("createdby"));
        this.reposListeners = new Vector();
        this.removeFormatButton = new JButton("Remove");
        this.previewPanel = new DataSetPanel(new DataSet());
        this.previewPanel.setEditable(false);
        this.chooser = new JFileChooser();
        this.chooser.setMultiSelectionEnabled(true);
        this.enabledRadio = new JRadioButton("Enabled:");
        this.enabledRadio.setSelected(true);
        this.editPrefsButton = new JButton("Preferences...");
        this.editPrefsButton.addActionListener(new ActionListener() {

            public void actionPerformed(ActionEvent ev) {
                System.out.println("Making visible");
                prefsEditor.setVisible(true);
            }
        });
        this.commentTextArea = new JTextArea(20, 8);
        this.commentTextArea.setText("No comment.");
        this.commentTextArea.setToolTipText("A detailed (possibly formatted) description including guidance to future developers of this set.");
        this.iconServer = new IconServer();
        this.iconServer.setConfigFile(this.Prefs.getConfigValue("default", "iconmapfile"));
        this.nicknameText = new IconifiedDomainNameTextField(new FlatFileFindNameDialog(Config, iconServer), this.iconServer);
        this.nicknameText.setPreferredSize(new Dimension(200, 25));
        this.nicknameText.setText(this.Prefs.getConfigValue("default", "domainname") + ".");
        this.nicknameText.setNameTextToolTipText("Right click to search the database.");
        this.uploadButton = new JButton("Upload");
        this.uploadButton.setToolTipText("Uploads current state to repository.");
        this.uploadButton.addActionListener(new ActionListener() {

            public void actionPerformed(ActionEvent ev) {
                System.out.println("Trying to upload flat file spec...");
                try {
                    String expname = getNickname();
                    int split = expname.lastIndexOf('.');
                    String domain = "";
                    String name = "";
                    String usersdomain = Prefs.getConfigValue("default", "domainname");
                    if (split > 0) {
                        domain = expname.substring(0, split);
                        name = expname.substring(split + 1, expname.length());
                    } else {
                        name = expname;
                    }
                    name = name.trim();
                    if (name.equals("")) {
                        JOptionPane.showMessageDialog(null, "Cowardly refusing to upload with an empty flat file name...");
                        return;
                    }
                    if (!domain.equals(usersdomain)) {
                        int s = JOptionPane.showConfirmDialog(null, "If you are not the original author, you may wish to switch the current domain name " + domain + " to \nyour domain name " + usersdomain + ".  Would you like to do this?\n (If you'll be using this domain often, you may want to set it in your preferences.)", "Potential WWW name-space clash!", JOptionPane.YES_NO_CANCEL_OPTION);
                        if (s == JOptionPane.YES_OPTION) {
                            setNickname(usersdomain + "." + name);
                            executeTransfer();
                        }
                        if (s == JOptionPane.NO_OPTION) {
                            executeTransfer();
                        }
                    } else {
                        executeTransfer();
                    }
                } catch (Exception err) {
                    throw new RuntimeException("Problem uploading storage.", err);
                }
            }
        });
        this.repositoryView = new JButton("default");
        this.repositoryView.addActionListener(new ActionListener() {

            public void actionPerformed(java.awt.event.ActionEvent e) {
                repositoryEditor.setCurrentRepository(repositoryView.getText());
                repositoryEditor.setVisible(true);
            }
        });
        this.prefsEditor = new PrefsConfigFrame(this.Prefs);
        this.prefsEditor.setVisible(false);
        this.prefsEditor.addCloseListener(new ActionListener() {

            public void actionPerformed(ActionEvent ev) {
                prefsEditor.setVisible(false);
            }
        });
        this.prefsEditor.addSelectListener(new ActionListener() {

            public void actionPerformed(ActionEvent ev) {
                prefsEditor.setVisible(false);
            }
        });
        this.repositoryEditor = new ReposConfigFrame(this.Config);
        this.repositoryEditor.setVisible(false);
        this.repositoryEditor.addSelectListener(new SelectListener());
        this.repositoryEditor.addCloseListener(new CloseListener());
        this.addSources = new JButton("Source from file...");
        this.preview = new JButton("Preview");
        this.leastcolumn = new JSpinner();
        this.columns2show = new JSpinner();
        this.leastrow = new JSpinner();
        this.rows2show = new JSpinner();
        int rowCount = 10;
        JLabel sourceLabel = new JLabel("File Source");
        this.flatfilesource = new JTextField();
        this.flatfilesource.setPreferredSize(new Dimension(200, 25));
        this.flatfilesource.setMinimumSize(new Dimension(200, 25));
        this.flatfilesource.setMaximumSize(new Dimension(200, 25));
        this.isURLButton = new JRadioButton("URL");
        Box scrollBox = Box.createVerticalBox();
        Box srcBox = Box.createHorizontalBox();
        srcBox.add(this.addSources);
        srcBox.add(sourceLabel);
        srcBox.add(this.flatfilesource);
        srcBox.add(this.isURLButton);
        srcBox.add(this.preview);
        scrollBox.add(srcBox);
        Box detailsPanel = Box.createVerticalBox();
        Box detailsBox = Box.createVerticalBox();
        JLabel label;
        Box jointBox;
        jointBox = Box.createHorizontalBox();
        label = new JLabel("Pre-Header Lines:");
        this.preheaderlines = new JSpinner();
        jointBox.add(label);
        jointBox.add(this.preheaderlines);
        detailsBox.add(jointBox);
        this.preheaderlines.addChangeListener(new ChangeListener() {

            public void stateChanged(ChangeEvent ev) {
                int selectedRow = fileselector.getSelectedRow();
                if (selectedRow >= 0) {
                    String fn = (String) sourcemodel.getValueAt(selectedRow, 0);
                    if (fn != null) {
                        updateDetailsFor(fn);
                    }
                }
            }
        });
        jointBox = Box.createHorizontalBox();
        label = new JLabel("Has Header Line:");
        this.hasHeaderLineBox = new JCheckBox();
        jointBox.add(label);
        jointBox.add(this.hasHeaderLineBox);
        detailsBox.add(jointBox);
        this.hasHeaderLineBox.addActionListener(new ActionListener() {

            public void actionPerformed(ActionEvent ev) {
                int selectedRow = fileselector.getSelectedRow();
                if (selectedRow >= 0) {
                    String fn = (String) sourcemodel.getValueAt(selectedRow, 0);
                    if (fn != null) {
                        fn = fn.trim();
                        if ((fn != null) && (fn.length() > 0)) {
                            updateDetailsFor(fn);
                        }
                    }
                }
            }
        });
        jointBox = Box.createHorizontalBox();
        label = new JLabel("Post-Header Lines:");
        this.postheaderlines = new JSpinner();
        jointBox.add(label);
        jointBox.add(this.postheaderlines);
        detailsBox.add(jointBox);
        this.postheaderlines.addChangeListener(new ChangeListener() {

            public void stateChanged(ChangeEvent ev) {
                int selectedRow = fileselector.getSelectedRow();
                if (selectedRow >= 0) {
                    String fn = (String) sourcemodel.getValueAt(selectedRow, 0);
                    if (fn != null) {
                        fn = fn.trim();
                        if ((fn != null) && (fn.length() > 0)) {
                            updateDetailsFor(fn);
                        }
                    }
                }
            }
        });
        jointBox = Box.createHorizontalBox();
        label = new JLabel("Format:");
        jointBox.add(label);
        this.singleFormatText = new JTextField("%s");
        jointBox.add(this.singleFormatText);
        jointBox.add(new JLabel("Repeat"));
        this.repeatFormatNumber = new JSpinner();
        this.repeatFormatNumber.setValue(new Integer(1));
        jointBox.add(this.repeatFormatNumber);
        this.addFormatButton = new JButton("Add");
        jointBox.add(this.addFormatButton);
        this.removeFormatButton = new JButton("Remove");
        jointBox.add(this.removeFormatButton);
        detailsBox.add(jointBox);
        jointBox = Box.createHorizontalBox();
        label = new JLabel("Column Format:");
        this.formatmodel = new FormatTableModel();
        this.formatTable = new JTable(this.formatmodel);
        this.formatmodel.addTableModelListener(this.fmtlistener);
        JTable hdrTable = this.formatTable.getTableHeader().getTable();
        this.formatTable.setAutoResizeMode(JTable.AUTO_RESIZE_OFF);
        JScrollPane fsp = new JScrollPane(this.formatTable);
        fsp.setPreferredSize(new Dimension(200, 100));
        jointBox.add(label);
        jointBox.add(fsp);
        detailsBox.add(jointBox);
        jointBox = Box.createHorizontalBox();
        label = new JLabel("Field Delimiter:");
        this.fieldDelimiter = new JTextField("\\t");
        this.fieldDelimiter.addActionListener(new ActionListener() {

            public void actionPerformed(ActionEvent ev) {
                int selectedRow = fileselector.getSelectedRow();
                if (selectedRow >= 0) {
                    String fn = (String) sourcemodel.getValueAt(selectedRow, 0);
                    if (fn != null) {
                        fn = fn.trim();
                        if ((fn != null) && (fn.length() > 0)) {
                            updateDetailsFor(fn);
                        }
                    }
                }
            }
        });
        jointBox.add(label);
        jointBox.add(this.fieldDelimiter);
        this.inferButton = new JButton("Infer");
        this.inferButton.setEnabled(false);
        jointBox.add(this.inferButton);
        detailsBox.add(jointBox);
        jointBox = Box.createHorizontalBox();
        label = new JLabel("Record Delimiter:");
        this.recordDelimiter = new JTextField("\\n");
        this.recordDelimiter.addActionListener(new ActionListener() {

            public void actionPerformed(ActionEvent ev) {
                int selectedRow = fileselector.getSelectedRow();
                if (selectedRow >= 0) {
                    String fn = (String) sourcemodel.getValueAt(selectedRow, 0);
                    if (fn != null) {
                        fn = fn.trim();
                        if ((fn != null) && (fn.length() > 0)) {
                            updateDetailsFor(fn);
                        }
                    }
                }
            }
        });
        jointBox.add(label);
        jointBox.add(this.recordDelimiter);
        detailsBox.add(jointBox);
        detailsBox.add(Box.createVerticalGlue());
        detailsBox.add(Box.createVerticalGlue());
        detailsPanel.add(srcBox);
        detailsPanel.add(detailsBox);
        detailsPanel.add(previewPanel);
        this.addFormatButton.addActionListener(new ActionListener() {

            public void actionPerformed(ActionEvent ev) {
                String fmt2rep = singleFormatText.getText();
                Integer rep = (Integer) repeatFormatNumber.getValue();
                Vector fmtparts = formatmodel.getFormatParts();
                int selectedCol = formatTable.getSelectedColumn();
                if (selectedCol < 0) {
                    selectedCol = formatTable.getColumnCount() - 1;
                }
                for (int r = 1; r <= rep.intValue(); r++) {
                    fmtparts.insertElementAt(fmt2rep, selectedCol);
                }
                formatmodel.setFormatParts(fmtparts);
                updateFormatTable();
                int selectedRow = fileselector.getSelectedRow();
                if ((selectedRow < sourcemodel.getRowCount()) && (selectedRow >= 0)) {
                    String fn = (String) sourcemodel.getValueAt(selectedRow, 0);
                    fn = fn.trim();
                    if (fn != null) {
                        fn = fn.trim();
                        if ((fn != null) && (fn.length() > 0)) {
                            updateDetailsFor(fn);
                        }
                    }
                }
            }
        });
        this.removeFormatButton.addActionListener(new ActionListener() {

            public void actionPerformed(ActionEvent ev) {
                int selectedCol = formatTable.getSelectedColumn();
                if (selectedCol < 0) {
                    return;
                }
                Vector parts = formatmodel.getFormatParts();
                if (parts.size() == 1) {
                    throw new RuntimeException("At least one format column is required.");
                }
                parts.removeElementAt(selectedCol);
                formatmodel.setFormatParts(parts);
                updateFormatTable();
                int selectedRow = fileselector.getSelectedRow();
                if ((selectedRow < sourcemodel.getRowCount()) && (selectedRow >= 0)) {
                    String fn = (String) sourcemodel.getValueAt(selectedRow, 0);
                    fn = fn.trim();
                    if (fn != null) {
                        fn = fn.trim();
                        if ((fn != null) && (fn.length() > 0)) {
                            updateDetailsFor(fn);
                        }
                    }
                }
                System.out.println("The new Column count after remove is " + formatmodel.getColumnCount());
            }
        });
        this.inferButton.addActionListener(new ActionListener() {

            public void actionPerformed(ActionEvent ev) {
                int row = fileselector.getSelectedRow();
                int col = 0;
                String filename = (String) sourcemodel.getValueAt(0, 0);
                Boolean isURL = (Boolean) sourcemodel.getValueAt(0, 1);
                BufferedReader br = null;
                File file = null;
                DataInputStream in = null;
                if (isURL.booleanValue()) {
                    try {
                        URL url2goto = new URL(filename);
                        in = new DataInputStream(url2goto.openStream());
                        System.out.println("READY TO READ FROM URL:" + url2goto);
                    } catch (Exception err) {
                        throw new RuntimeException("Problem constructing URI for " + filename + ".", err);
                    }
                } else {
                    file = new File(filename);
                    if (!file.exists()) {
                        throw new RuntimeException("The file named '" + filename + "' does not exist.");
                    }
                    FileInputStream fstream = null;
                    try {
                        fstream = new FileInputStream(filename);
                        in = new DataInputStream(fstream);
                    } catch (Exception err) {
                        throw new RuntimeException("Problem creating FileInputStream for " + filename + ".", err);
                    }
                }
                br = new BufferedReader(new InputStreamReader(in));
                JTable hdrTable = formatTable.getTableHeader().getTable();
                try {
                    String strLine;
                    int line = 0;
                    int ignorePreHdrLines = ((Integer) preheaderlines.getValue()).intValue();
                    int ignorePostHdrLines = ((Integer) postheaderlines.getValue()).intValue();
                    int numhdr = 0;
                    boolean hasHeaderLine = false;
                    if (hasHeaderLineBox.isSelected()) {
                        hasHeaderLine = true;
                    }
                    if (hasHeaderLine) {
                        numhdr = 1;
                    }
                    String FD = fieldDelimiter.getText();
                    while ((strLine = br.readLine()) != null) {
                        if (line <= (ignorePreHdrLines + numhdr + ignorePostHdrLines)) {
                            System.out.println(strLine);
                        } else {
                            String[] putative_cols = strLine.split(FD);
                            System.out.println("The number of potential columns is " + putative_cols.length);
                            String FMT = "";
                            while (formatTable.getColumnCount() > putative_cols.length) {
                                TableColumn tcol = formatTable.getColumnModel().getColumn(0);
                                formatTable.removeColumn(tcol);
                            }
                            for (int i = 0; i < putative_cols.length; i++) {
                                String fmt = "";
                                try {
                                    Double dummy = new Double(putative_cols[i]);
                                    fmt = "%f";
                                } catch (Exception err) {
                                    fmt = "%s";
                                }
                                FMT = FMT + fmt;
                                formatTable.setValueAt(fmt, 0, i);
                            }
                            System.out.println("The potential format is " + FMT);
                            formatmodel.setFormat(FMT);
                            break;
                        }
                        line++;
                    }
                    in.close();
                } catch (Exception err) {
                    throw new RuntimeException("Problem reading single line from file.", err);
                }
                for (int j = 0; j < formatTable.getColumnCount(); j++) {
                    hdrTable.getColumnModel().getColumn(j).setHeaderValue("" + (j + 1));
                }
                formatTable.repaint();
            }
        });
        Box topbox = Box.createHorizontalBox();
        topbox.add(detailsPanel);
        Box mainbox = Box.createVerticalBox();
        Box setBox = Box.createHorizontalBox();
        setBox.add(this.editPrefsButton);
        jointBox = Box.createHorizontalBox();
        label = new JLabel("Created On:");
        jointBox.add(label);
        this.createdOnText.setPreferredSize(new Dimension(50, 25));
        jointBox.add(this.createdOnText);
        setBox.add(jointBox);
        jointBox = Box.createHorizontalBox();
        label = new JLabel("Created By:");
        jointBox.add(label);
        this.createdByText.setPreferredSize(new Dimension(50, 25));
        jointBox.add(this.createdByText);
        setBox.add(jointBox);
        setBox.add(this.uploadButton);
        setBox.add(this.repositoryView);
        setBox.add(this.nicknameText);
        setBox.add(this.enabledRadio);
        mainbox.add(setBox);
        jointBox = Box.createHorizontalBox();
        label = new JLabel("Comment:");
        jointBox.add(label);
        jointBox.add(new JScrollPane(this.commentTextArea));
        mainbox.add(jointBox);
        mainbox.add(topbox);
        mainbox.add(previewPanel);
        this.add(mainbox);
        this.addSources.addActionListener(new ActionListener() {

            public void actionPerformed(ActionEvent ev) {
                int option = chooser.showOpenDialog(null);
                File[] files = null;
                if (option == JFileChooser.APPROVE_OPTION) {
                    files = chooser.getSelectedFiles();
                    if (files.length > 10) {
                        ((DefaultTableModel) sourcemodel).setRowCount(files.length);
                    } else {
                        ((DefaultTableModel) sourcemodel).setRowCount(10);
                    }
                    for (int i = 0; i < files.length; i++) {
                        sourcemodel.setValueAt(files[i].getAbsolutePath(), i, 0);
                    }
                }
                if (anyNonEmptySources()) {
                    allowFormatParsing(true);
                } else {
                    allowFormatParsing(false);
                }
            }
        });
        this.preview.addActionListener(new ActionListener() {

            public void actionPerformed(ActionEvent ev) {
                FlatFileDOM[] filespecs = new FlatFileDOM[map.size()];
                int k = 0;
                for (int j = 0; j < sourcemodel.getRowCount(); j++) {
                    String fn = (String) sourcemodel.getValueAt(j, 0);
                    if (map.containsKey(fn)) {
                        filespecs[k] = (FlatFileDOM) map.get(fn);
                        k++;
                    }
                }
                Vector hdrs = null;
                Vector types = null;
                for (int j = 0; j < filespecs.length; j++) {
                    DataSetReader rdr = new DataSetReader(filespecs[j]);
                    int rc = rdr.determineRowCount();
                    filespecs[j].setRowCount(rc);
                    if (j == 0) {
                        hdrs = rdr.getHeaders();
                        types = rdr.getTypes();
                    }
                    System.out.println("The number of rows is=" + rc);
                }
                System.out.println("Creating flatfileset");
                FlatFileSet dataset = new FlatFileSet(filespecs);
                System.out.println("Finished sorting!!!");
                for (int j = 0; j < hdrs.size(); j++) {
                    dataset.addColumn((String) hdrs.get(j), (Class) types.get(j));
                }
                System.out.println("Number of headers is=" + hdrs.size());
                System.out.println("The dataset rc is " + dataset.getRowCount());
                System.out.println("The dataset cc is " + dataset.getColumnCount());
                previewPanel.setDataSet(dataset);
                previewPanel.setVerticalScrollIntermittant(true);
                previewPanel.setHorizontalScrollIntermittant(true);
                previewPanel.setEditable(false);
                if (anyNonEmptySources()) {
                    allowFormatParsing(true);
                } else {
                    allowFormatParsing(false);
                }
            }
        });
        allowFormatParsing(false);
        this.formatTable.repaint();
    }

    private class FormatTableModelListener implements TableModelListener {

        public FormatTableModelListener() {
        }

        public void tableChanged(TableModelEvent e) {
            System.out.println("Inside this TableModel Listener 1");
            if (e.getType() == TableModelEvent.UPDATE) {
                int selectedRow = fileselector.getSelectedRow();
                if ((selectedRow < sourcemodel.getRowCount()) && (selectedRow >= 0)) {
                    String fn = (String) sourcemodel.getValueAt(selectedRow, 0);
                    fn = fn.trim();
                    if (fn != null) {
                        fn = fn.trim();
                        if ((fn != null) && (fn.length() > 0)) {
                            System.out.println("Updating details from  format listener...");
                            updateDetailsFor(fn);
                        }
                    }
                }
            }
        }
    }

    public Class getDefaultGraphicalEditorClass() {
        return (FlatFileFrame.class);
    }

    private void updateFormatTable() {
        Vector parts = formatmodel.getFormatParts();
        TableColumnModel cmod = formatTable.getColumnModel();
        while (cmod.getColumnCount() > 0) {
            TableColumn tcol = cmod.getColumn(0);
            cmod.removeColumn(tcol);
        }
        for (int j = 0; j < parts.size(); j++) {
            TableColumn tcol = new TableColumn(j);
            tcol.setHeaderValue(formatmodel.getColumnName(j));
            cmod.addColumn(tcol);
        }
        formatTable.repaint();
    }

    private void updateDetailsFor(String fn) {
        if (!this.NoCallbackChangeMode) {
            System.out.println("Making changes in details");
            FlatFileDOM dom = (FlatFileDOM) this.map.get(fn);
            if (dom != null) {
                dom.setNickname(fn);
                dom.setFilename(fn);
                dom.setPreHeaderLines((String) ((Integer) this.preheaderlines.getValue()).toString());
                dom.setPostHeaderLines((String) ((Integer) this.postheaderlines.getValue()).toString());
                if (this.hasHeaderLineBox.isSelected()) {
                    dom.setHasHeaderLine("true");
                } else {
                    dom.setHasHeaderLine("false");
                }
                dom.setFieldDelimiter(this.fieldDelimiter.getText());
                dom.setRecordDelimiter(this.recordDelimiter.getText());
                System.out.println("Updating details with " + formatmodel.getFormat());
                dom.setFormat(formatmodel.getFormat());
            } else {
                throw new RuntimeException("Details do not exist for " + fn);
            }
        }
    }

    public void showDetailsFor(FlatFileDOM fd) {
        this.NoCallbackChangeMode = true;
        Integer val = new Integer(fd.getPreHeaderLines());
        this.preheaderlines.setValue(val.intValue());
        val = new Integer(fd.getPostHeaderLines());
        this.postheaderlines.setValue(val.intValue());
        String sval = fd.getHasHeaderLine();
        if (sval.equalsIgnoreCase("true")) {
            this.hasHeaderLineBox.setSelected(true);
        } else {
            this.hasHeaderLineBox.setSelected(false);
        }
        this.fieldDelimiter.setText(fd.getFieldDelimiter());
        this.recordDelimiter.setText(fd.getRecordDelimiter());
        System.out.println("The retrieved format is:" + fd.getFormat());
        this.formatmodel.setFormat(fd.getFormat());
        updateFormatTable();
        this.NoCallbackChangeMode = false;
    }

    private class FileSourceTableModel extends DefaultTableModel {

        public FileSourceTableModel(Object[] cnames, int rc) {
            super(cnames, rc);
        }

        public Class getColumnClass(int c) {
            Class out = null;
            if (c == 1) {
                out = Boolean.class;
            } else {
                out = String.class;
            }
            return (out);
        }
    }

    private class FormatTableModel extends AbstractTableModel {

        private String FMT;

        private String[] fmtparts;

        public FormatTableModel() {
            this.setFormat("%s");
        }

        public boolean isCellEditable(int rowIndex, int columnIndex) {
            return (true);
        }

        public Vector getFormatParts() {
            Vector out = new Vector();
            for (int j = 0; j < fmtparts.length; j++) {
                out.add(fmtparts[j]);
            }
            return (out);
        }

        public void setFormatParts(Vector v) {
            this.fmtparts = new String[v.size()];
            this.FMT = "";
            for (int j = 0; j < v.size(); j++) {
                this.fmtparts[j] = (String) v.get(j);
                this.FMT = this.FMT + this.fmtparts[j];
            }
        }

        public int getRowCount() {
            return (1);
        }

        public int getColumnCount() {
            return (this.fmtparts.length);
        }

        public String getColumnName(int col) {
            Integer column = new Integer(col + 1);
            return (column.toString());
        }

        public Class getColumnClass(int col) {
            return (String.class);
        }

        public Object getValueAt(int row, int col) {
            if (col >= this.fmtparts.length) {
                throw new RuntimeException("Column index " + col + " is greater than the current number of  formats " + this.fmtparts.length + ".");
            }
            return (this.fmtparts[col]);
        }

        public void setValueAt(Object obj, int row, int col) {
            String in = (String) obj;
            if (col >= this.fmtparts.length) {
                throw new RuntimeException("Column index " + col + " is greater than the current number of  formats " + this.fmtparts.length + ".");
            }
            Vector parts = this.getFormatParts();
            parts.setElementAt(in, col);
            this.setFormatParts(parts);
            this.fireTableRowsUpdated(0, 0);
        }

        public void setFormat(String fmt) {
            if (fmt == null) {
                this.FMT = "%s";
            } else {
                this.FMT = fmt;
            }
            String[] parts = FMT.split("%");
            if (parts.length < 2) {
                this.FMT = "%s";
                parts = FMT.split("%");
            }
            this.fmtparts = new String[parts.length - 1];
            for (int j = 0; j < parts.length - 1; j++) {
                this.fmtparts[j] = "%" + parts[j + 1];
            }
        }

        public String getFormat() {
            return (this.FMT);
        }
    }

    public void setFormat(String FMT) {
        this.formatmodel.setFormat(FMT);
    }

    public String getFormat() {
        return (this.formatmodel.getFormat());
    }

    public void setRecordDelimiter(String FMT) {
        this.recordDelimiter.setText(FMT);
    }

    public String getRecordDelimiter() {
        return (this.recordDelimiter.getText());
    }

    public void setFieldDelimiter(String FMT) {
        this.fieldDelimiter.setText(FMT);
    }

    public String getFieldDelimiter() {
        return (this.fieldDelimiter.getText());
    }

    public void setPostHeaderLines(String FMT) {
        this.postheaderlines.setValue(new Integer(FMT));
    }

    public String getPostHeaderLines() {
        return (((Integer) this.postheaderlines.getValue()).toString());
    }

    public void setPreHeaderLines(String FMT) {
        this.preheaderlines.setValue(new Integer(FMT));
    }

    public String getPreHeaderLines() {
        return (((Integer) this.preheaderlines.getValue()).toString());
    }

    public void setIsURL(String FMT) {
        if (FMT.equalsIgnoreCase("true")) {
            this.isURLButton.setSelected(true);
        } else {
            this.isURLButton.setSelected(false);
        }
    }

    public String getIsURL() {
        String out = "false";
        if (this.isURLButton.isSelected()) {
            out = "true";
        }
        return (out);
    }

    public void setFilename(String FMT) {
        this.flatfilesource.setText(FMT);
    }

    public String getFilename() {
        return (this.flatfilesource.getText());
    }

    public void setReadHeaders(String FMT) {
        this.setHasHeaderLine(FMT);
    }

    public String getReadHeaders() {
        return (this.getHasHeaderLine());
    }

    public String getHasHeaderLine() {
        String out = "false";
        if (this.hasHeaderLineBox.isSelected()) {
            out = "true";
        }
        return (out);
    }

    public void setHasHeaderLine(String f) {
        if (f.equalsIgnoreCase("true")) {
            this.hasHeaderLineBox.setSelected(true);
        } else if (f.equalsIgnoreCase("false")) {
            this.hasHeaderLineBox.setSelected(false);
        } else {
            throw new RuntimeException("Invalid String value for setHasHeaderLine " + f + ".");
        }
    }

    private class SelectListener implements ActionListener {

        public void actionPerformed(ActionEvent e) {
            repositoryView.setText(repositoryEditor.getCurrentRepository());
            repositoryEditor.setVisible(false);
        }
    }

    private class CloseListener implements ActionListener {

        public void actionPerformed(ActionEvent e) {
            repositoryEditor.setVisible(false);
        }
    }

    private void allowFormatParsing(boolean x) {
        this.inferButton.setEnabled(x);
        this.preview.setEnabled(x);
    }

    public void setEditable(boolean b) {
        this.nicknameText.setEditable(b);
        this.createdByText.setEditable(b);
        this.createdOnText.setEditable(b);
        this.commentTextArea.setEditable(b);
        this.uploadButton.setEnabled(b);
        this.repositoryView.setEnabled(b);
        this.enabledRadio.setEnabled(b);
        String title = this.getTitle();
        int k = title.lastIndexOf("(Read only)");
        if (k == -1) {
            if (b == false) {
                title = title + "(Read only)";
            }
        } else {
            if (b == true) {
                title = title.substring(0, k);
            }
        }
        this.setTitle(title);
    }

    public Vector getFlatFileNames() {
        Vector out = new Vector();
        Set keys = this.map.keySet();
        Iterator iter = keys.iterator();
        while (iter.hasNext()) {
            out.add((String) iter.next());
        }
        return (out);
    }

    public void setComment(String cmt) {
        this.commentTextArea.setText(cmt);
    }

    public String getComment() {
        return (this.commentTextArea.getText());
    }

    public void setCreatedOn(String on) {
        this.createdOnText.setText(on);
    }

    public String getCreatedOn() {
        return (this.createdOnText.getText());
    }

    public void setCreatedBy(String on) {
        this.createdByText.setText(on);
    }

    public String getCreatedBy() {
        return (this.createdByText.getText());
    }

    public void setNickname(String name) {
        this.nicknameText.setText(name);
    }

    public String getNickname() {
        return (this.nicknameText.getText());
    }

    public void setEnabled(String n) {
        boolean isenabled = false;
        if (n == null) {
            throw new IllegalArgumentException("Enabled value can not be null.");
        }
        if (n.equals("0")) {
            isenabled = false;
        }
        if (n.equals("1")) {
            isenabled = true;
        } else {
            throw new IllegalArgumentException("Unrecognized enabled value:" + n + ":");
        }
        this.enabledRadio.setSelected(isenabled);
    }

    public String getEnabled() {
        String out = null;
        if (this.enabledRadio.isSelected()) {
            out = "1";
        } else {
            out = "0";
        }
        return (out);
    }

    public void removeRepositoryListener(RepositoryListener l) {
        this.reposListeners.remove(l);
    }

    public void addRepositoryListener(RepositoryListener l) {
        this.reposListeners.add(l);
    }

    public void beforeTransferStorage() {
    }

    public void afterTransferStorage() {
    }

    public void transferStorageCommands(RepositoryStorage x) {
        this.transferAgent.transferStorageCommands(x);
    }

    public void transferStorage(RepositoryStorage that) {
        System.out.println("WWWWWWWWWWWWWWWWWWWWWWWWWWWWWWWWWWWZ");
        this.transferAgent.transferStorage(that);
    }

    public void beforeCopyStorage() {
    }

    public void afterCopyStorage() {
    }

    public void copyStorageCommands(RepositoryStorage x) {
        this.transferAgent.copyStorageCommands(x);
    }

    public void copyStorage(RepositoryStorage that) {
        System.out.println("WWWWWWWWWWWWWWWWWWWWWWWWWWWWWWWWWWWZ");
        this.transferAgent.copyStorage(that);
    }

    public void setSameFormatAs(FlatFileStorage f) {
        ((FlatFileTransferAgent) this.transferAgent).setSameFormatAs(f);
    }

    private boolean anyNonEmptySources() {
        boolean out = false;
        for (int j = 0; j < this.sourcemodel.getRowCount(); j++) {
            String v = (String) this.sourcemodel.getValueAt(j, 0);
            if (v != null) {
                v = v.trim();
                if (v.length() > 0) {
                    out = true;
                    break;
                }
            }
        }
        return (out);
    }

    public Class getDOMStorageClass() {
        return (FlatFileSetDOM.class);
    }

    public void executeTransfer() {
        this.setEditable(false);
        String repos = this.repositoryView.getText();
        String setName = this.getNickname();
        if (!this.Connection.storageExists(setName)) {
            System.out.println("Creating the storage " + setName + " of class FlatFileXML.");
            boolean success = this.Connection.createStorage(FlatFileStorage.class, setName);
            if (!success) {
                throw new RuntimeException("Failed to create storage of " + FlatFileXML.class + " named " + setName + ".");
            }
        } else {
            Object[] options = { "Ok", "Cancel" };
            int n = JOptionPane.showOptionDialog(FlatFileFrame.this, "Overwrite the existing definition " + setName + " in repository " + repos + "?", "Previously defined storage", JOptionPane.YES_NO_CANCEL_OPTION, JOptionPane.QUESTION_MESSAGE, null, options, options[1]);
            String ans = (String) options[n];
            if (ans.equalsIgnoreCase("Cancel")) {
                System.out.println("Abandoning, don't do anything...");
                return;
            }
        }
        this.backingStorage = (FlatFileStorage) this.Connection.getStorage(setName);
        if (this.backingStorage == null) {
            try {
                boolean success = this.Connection.createStorage(FlatFileStorage.class, setName);
                if (success) {
                    this.backingStorage = (FlatFileStorage) this.Connection.getStorage(setName);
                }
            } catch (Exception err) {
                throw new RuntimeException("Unable to retrieve storage " + setName + " from repository " + repos + ".", err);
            }
        }
        System.out.println("Transfering to backing storage!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!");
        System.out.println("The class is:" + backingStorage.getClass());
        if (this.backingStorage == null) {
            throw new RuntimeException("Retrieved storage is null.");
        }
        this.backingStorage.transferStorage(this);
        this.setEditable(true);
    }

    public Class getStorageTransferAgentClass() {
        return (FlatFileSetTransferAgent.class);
    }

    public void addFlatFile(String storagenickname) {
        if (!this.map.containsKey(storagenickname)) {
            this.map.put(storagenickname, new FlatFileDOM(storagenickname));
            Object[] row = new Object[2];
            row[0] = storagenickname;
            row[1] = new Boolean(false);
            this.sourcemodel.addRow(row);
        }
    }

    public void removeFlatFile(String storagenickname) {
        if (this.map.containsKey(storagenickname)) {
            for (int j = 0; j < this.sourcemodel.getRowCount(); j++) {
                String fn = (String) this.sourcemodel.getValueAt(j, 0);
                if (fn.equals(storagenickname)) {
                    this.sourcemodel.removeRow(j);
                }
            }
            this.map.remove(storagenickname);
        }
    }
}
