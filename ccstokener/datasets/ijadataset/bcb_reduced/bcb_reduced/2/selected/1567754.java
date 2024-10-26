package com.mockturtlesolutions.snifflib.flatfiletools.workbench;

import java.awt.image.BufferedImage;
import java.awt.Image;
import javax.swing.ImageIcon;
import com.mockturtlesolutions.snifflib.reposconfig.database.RepositoryEvent;
import com.mockturtlesolutions.snifflib.guitools.components.DomainNameEvent;
import com.mockturtlesolutions.snifflib.guitools.components.DomainNameListener;
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
import java.util.Collection;
import javax.swing.event.ChangeListener;
import javax.swing.event.ChangeEvent;
import javax.swing.text.Document;
import javax.swing.event.DocumentListener;
import javax.swing.event.DocumentEvent;
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
import com.mockturtlesolutions.snifflib.reposconfig.database.ReposConfigurable;
import com.mockturtlesolutions.snifflib.guitools.components.PrefsConfigFrame;
import java.text.DateFormat;
import java.util.Date;
import java.text.SimpleDateFormat;
import java.lang.reflect.Constructor;
import java.lang.reflect.Constructor;
import javax.swing.border.*;
import javax.swing.BorderFactory;
import java.awt.Color;
import com.mockturtlesolutions.snifflib.reposconfig.database.GraphicalRepositoryEditor;

public class FlatFileSetFrame extends JFrame implements GraphicalRepositoryEditor, FlatFileSetStorage {

    private ReposConfigFrame repositoryEditor;

    protected FlatFileToolsConfig Config;

    protected FlatFileStorageConnectivity Connection;

    private JTable fileselector;

    private JButton uploadflatfileButton;

    private JButton addSources;

    private JButton addSourcesFromStorage;

    private JButton addSourcesFromURL;

    private JButton removeSources;

    private FileSourceTableModel sourcemodel;

    private JSpinner preheaderlines;

    private JSpinner postheaderlines;

    private FlatFileSetStorage backingStorage;

    private JTextArea commentTextArea;

    private JTextArea flatfileCommentArea;

    private JTextField createdOnText;

    private JTextField createdByText;

    protected RepositoryConnectionHandler connectionHandler;

    private PrefsConfigFrame prefsEditor;

    private boolean complete_preview;

    private boolean SHOULD_PULL_FLATFILES_FROM_REPOSITORY;

    private int lastselectedsourcerow;

    private JLabel waitIndicator;

    private JButton copyFormatingToSelected;

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

    private JButton clearButton;

    private JCheckBox hasHeaderLineBox;

    private JRadioButton enabledRadio;

    private JRadioButton flatFileEnabledRadio;

    private IconifiedDomainNameTextField nicknameText;

    private IconifiedDomainNameTextField flatfilenicknameText;

    private IconServer iconServer;

    private FlatFileToolsPrefs Prefs;

    private JComboBox sortProtocols;

    private FlatFileFindNameDialog findSet;

    private FlatFileFindNameDialog findFile;

    private FormatTableModelListener fmtlistener;

    public FlatFileSetFrame() {
        super("Specify Your Flat File Data");
        this.lastselectedsourcerow = -1;
        this.SHOULD_PULL_FLATFILES_FROM_REPOSITORY = false;
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
        this.waitIndicator = new JLabel("X");
        this.waitIndicator.setHorizontalAlignment(JLabel.CENTER);
        this.waitIndicator.setPreferredSize(new Dimension(25, 25));
        this.waitIndicator.setMaximumSize(new Dimension(25, 25));
        this.waitIndicator.setMinimumSize(new Dimension(25, 25));
        this.NoCallbackChangeMode = false;
        this.setSize(new Dimension(1000, 550));
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
        this.previewPanel.setBorder(BorderFactory.createTitledBorder(BorderFactory.createEtchedBorder(), "Preview:"));
        this.chooser = new JFileChooser();
        this.chooser.setMultiSelectionEnabled(true);
        this.clearButton = new JButton("Clear");
        this.clearButton.addActionListener(new ActionListener() {

            public void actionPerformed(ActionEvent ev) {
                NoCallbackChangeMode = true;
                backingStorage = null;
                formatmodel.setFormat(null);
                preheaderlines.setValue(0);
                postheaderlines.setValue(0);
                commentTextArea.setText("");
                flatfileCommentArea.setText("");
                SimpleDateFormat formatter = new SimpleDateFormat("yyyy-MM-dd");
                String formatted_date = formatter.format(new Date());
                createdOnText = new JTextField(formatted_date);
                createdByText = new JTextField(Prefs.getConfigValue("createdby"));
                fieldDelimiter.setText("\\t");
                recordDelimiter.setText("\\n");
                hasHeaderLineBox.setSelected(false);
                enabledRadio.setSelected(true);
                nicknameText.setText("");
                flatfilenicknameText.setText("");
                sortProtocols.setSelectedIndex(0);
                singleFormatText.setText("%s");
                repeatFormatNumber.setValue(new Integer(1));
                sourcemodel.clearFlatFiles();
                previewPanel.setDataSet(new DataSet());
                NoCallbackChangeMode = false;
            }
        });
        this.enabledRadio = new JRadioButton("Enabled:");
        this.enabledRadio.setSelected(true);
        this.enabledRadio.addActionListener(new ActionListener() {

            public void actionPerformed(ActionEvent ev) {
                if (!enabledRadio.isSelected()) {
                    int t = JOptionPane.showConfirmDialog(null, "Note, disabling a storage deprecates it and schedules it for deletion.  Disable this storage?", "Deprecate storage?", JOptionPane.YES_NO_CANCEL_OPTION);
                    if (t != JOptionPane.YES_OPTION) {
                        enabledRadio.setEnabled(false);
                        enabledRadio.setSelected(true);
                        enabledRadio.setEnabled(true);
                    }
                }
            }
        });
        this.flatFileEnabledRadio = new JRadioButton("Enabled:");
        this.flatFileEnabledRadio.setSelected(true);
        this.flatFileEnabledRadio.addActionListener(new ActionListener() {

            public void actionPerformed(ActionEvent ev) {
                if (!flatFileEnabledRadio.isSelected()) {
                    int t = JOptionPane.showConfirmDialog(null, "Note, disabling a storage deprecates it and schedules it for deletion.  Disable this storage?", "Deprecate storage?", JOptionPane.YES_NO_CANCEL_OPTION);
                    if (t != JOptionPane.YES_OPTION) {
                        flatFileEnabledRadio.setEnabled(false);
                        flatFileEnabledRadio.setSelected(true);
                        flatFileEnabledRadio.setEnabled(true);
                    }
                }
            }
        });
        this.editPrefsButton = new JButton("Preferences...");
        this.editPrefsButton.addActionListener(new ActionListener() {

            public void actionPerformed(ActionEvent ev) {
                prefsEditor.setVisible(true);
            }
        });
        this.flatfileCommentArea = new JTextArea(2, 16);
        this.flatfileCommentArea.setToolTipText("A detailed description of this file source.");
        this.flatfileCommentArea.setText(" ");
        Document doc = this.flatfileCommentArea.getDocument();
        doc.addDocumentListener(new DocumentListener() {

            public void changedUpdate(DocumentEvent ev) {
                int selectedRow = fileselector.getSelectedRow();
                if (selectedRow >= 0) {
                    updateDetailsFor(selectedRow);
                }
            }

            public void insertUpdate(DocumentEvent ev) {
                int selectedRow = fileselector.getSelectedRow();
                if (selectedRow >= 0) {
                    updateDetailsFor(selectedRow);
                }
            }

            public void removeUpdate(DocumentEvent ev) {
                int selectedRow = fileselector.getSelectedRow();
                if (selectedRow >= 0) {
                    updateDetailsFor(selectedRow);
                }
            }
        });
        this.commentTextArea = new JTextArea(2, 16);
        this.commentTextArea.setToolTipText("A detailed (possibly formatted) description including guidance to future developers of this set.");
        this.commentTextArea.setText(" ");
        this.iconServer = new IconServer();
        this.iconServer.setConfigFile(this.Prefs.getConfigValue("default", "iconmapfile"));
        this.findSet = new FlatFileFindNameDialog(Config, iconServer);
        this.findSet.setSearchClass(FlatFileSetStorage.class);
        this.nicknameText = new IconifiedDomainNameTextField(findSet, this.iconServer);
        this.findFile = new FlatFileFindNameDialog(Config, iconServer);
        this.findFile.setSearchClass(FlatFileStorage.class);
        this.findFile.addOkListener(new ActionListener() {

            public void actionPerformed(ActionEvent ev) {
                String selectedFile = findFile.getSelectedName();
                if (selectedFile != null) {
                    FlatFileStorage file = (FlatFileStorage) Connection.getStorage(selectedFile);
                    FlatFileDOM dom = new FlatFileDOM();
                    dom.transferStorage(file);
                    sourcemodel.addFlatFile(dom);
                } else {
                }
                if (anyNonEmptySources()) {
                    removeSources.setEnabled(true);
                    allowFormatParsing(true);
                } else {
                    removeSources.setEnabled(false);
                    allowFormatParsing(false);
                }
                fileselector.getSelectionModel().setSelectionInterval(0, 0);
            }
        });
        this.findSet.addOkListener(new ActionListener() {

            public void actionPerformed(ActionEvent ev) {
                String selectedSet = findSet.getSelectedName();
                if (selectedSet != null) {
                    FlatFileSetStorage set = (FlatFileSetStorage) Connection.getStorage(selectedSet);
                    FlatFileSetFrame.this.transferStorage(set);
                } else {
                }
                int N = fileSetSize();
                for (int k = 0; k < N; k++) {
                    FlatFileDOM dom = sourcemodel.getFlatFileDOM(k);
                    String storagenickname = dom.getNickname();
                    FlatFileStorage src = (FlatFileStorage) Connection.getStorage(storagenickname);
                    if (src != null) {
                        dom.transferStorage(src);
                    }
                }
                if (anyNonEmptySources()) {
                    removeSources.setEnabled(true);
                    allowFormatParsing(true);
                } else {
                    removeSources.setEnabled(false);
                    allowFormatParsing(false);
                }
                fileselector.getSelectionModel().setSelectionInterval(0, 0);
            }
        });
        URL url = this.getClass().getResource("images/file_write_icon.png");
        ImageIcon icon = new ImageIcon(url);
        Image im = icon.getImage();
        icon.setImage(im.getScaledInstance(25, -1, Image.SCALE_SMOOTH));
        this.uploadflatfileButton = new JButton(icon);
        this.uploadflatfileButton.setToolTipText("Upload this single file source specification to the repository.");
        this.flatfilenicknameText = new IconifiedDomainNameTextField(findFile, this.iconServer);
        this.flatfilenicknameText.addDomainNameListener(new DomainNameListener() {

            public void actionPerformed(DomainNameEvent ev) {
                IconifiedDomainNameTextField tf = (IconifiedDomainNameTextField) ev.getSource();
                int sr = fileselector.getSelectedRow();
                if (sr >= 0) {
                    FlatFileDOM dom = sourcemodel.getFlatFileDOM(sr);
                    if (dom != null) {
                        dom.setNickname(tf.getText());
                    }
                }
            }
        });
        this.nicknameText.setPreferredSize(new Dimension(200, 25));
        this.nicknameText.setText(this.Prefs.getConfigValue("default", "domainname") + ".");
        this.nicknameText.setNameTextToolTipText("Right click to search the database.");
        url = this.getClass().getResource("images/file_write_icon.png");
        icon = new ImageIcon(url);
        im = icon.getImage();
        icon.setImage(im.getScaledInstance(25, -1, Image.SCALE_SMOOTH));
        this.uploadButton = new JButton(icon);
        this.uploadButton.setToolTipText("Uploads entire set configuration to repository.");
        this.uploadButton.addActionListener(new ActionListener() {

            public void actionPerformed(ActionEvent ev) {
                boolean do_transfer = false;
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
                        JOptionPane.showMessageDialog(null, "Cowardly refusing to upload with an empty buffer name...");
                        return;
                    }
                    if (!domain.equals(usersdomain)) {
                        int s = JOptionPane.showConfirmDialog(null, "If you are not the original author, you may wish to switch the current domain name " + domain + " to \nyour domain name " + usersdomain + ".  Would you like to do this?\n (If you'll be using this domain often, you may want to set it in your preferences.)", "Potential WWW name-space clash!", JOptionPane.YES_NO_CANCEL_OPTION);
                        if (s == JOptionPane.YES_OPTION) {
                            setNickname(usersdomain + "." + name);
                            do_transfer = executeTransfer();
                        }
                        if (s == JOptionPane.NO_OPTION) {
                            do_transfer = executeTransfer();
                        }
                    } else {
                        do_transfer = executeTransfer();
                    }
                } catch (Exception err) {
                    throw new RuntimeException("Problem uploading storage.", err);
                }
                if (do_transfer) {
                    int s = JOptionPane.showConfirmDialog(null, "At this time you may also upload the individual flat file storage specifications supporting this set.\nWould you like to do this? ", "Create/Upload Flat File Specs Too?", JOptionPane.YES_NO_CANCEL_OPTION);
                    if (s == JOptionPane.YES_OPTION) {
                        boolean yes_to_all = false;
                        for (int m = 0; m < sourcemodel.getRowCount(); m++) {
                            FlatFileDOM dom = sourcemodel.getFlatFileDOM(m);
                            String nn = dom.getNickname();
                            if (nn != null) {
                                nn = nn.trim();
                            }
                            if ((nn != null) && (nn.length() != 0)) {
                                if (Connection.storageExists(nn)) {
                                    int t = -1;
                                    if (!yes_to_all) {
                                        Object[] options = { "Yes", "No", "Yes to all" };
                                        t = JOptionPane.showOptionDialog(FlatFileSetFrame.this, "Storage " + nn + " already exists.  Would you like to overwrite?", "Overwrite Flat File Spec?", JOptionPane.YES_NO_CANCEL_OPTION, JOptionPane.QUESTION_MESSAGE, null, options, options[1]);
                                    } else {
                                        t = 0;
                                    }
                                    if (t == 2) {
                                        yes_to_all = true;
                                        t = 0;
                                    }
                                    if (t == 0) {
                                        System.out.println("Overwriting existing storage " + nn);
                                        FlatFileStorage target = (FlatFileStorage) Connection.getStorage(nn);
                                        if (target == null) {
                                            throw new RuntimeException("Storage " + nn + " was indicated to exist but could not be retrieved.");
                                        }
                                        target.transferStorage(dom);
                                    }
                                } else {
                                    Connection.createStorage(FlatFileStorage.class, nn);
                                    FlatFileStorage target = (FlatFileStorage) Connection.getStorage(nn);
                                    target.transferStorage(dom);
                                }
                            }
                        }
                    }
                }
                setEditable(true);
            }
        });
        this.repositoryView = new JButton("default");
        this.repositoryView.addActionListener(new ActionListener() {

            public void actionPerformed(java.awt.event.ActionEvent e) {
                setRepository(repositoryView.getText());
                repositoryEditor.setVisible(true);
            }
        });
        String[] srtprotocol = this.Config.getSplitConfigValue(this.repositoryView.getText(), "sortprotocol");
        this.sortProtocols = new JComboBox(srtprotocol);
        this.sortProtocols.setPrototypeDisplayValue("WWWWWWWWWW");
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
        url = this.getClass().getResource("images/file_save_icon.png");
        icon = new ImageIcon(url);
        im = icon.getImage();
        icon.setImage(im.getScaledInstance(25, -1, Image.SCALE_SMOOTH));
        this.addSources = new JButton(icon);
        this.addSources.setToolTipText("Add a local flat file to the set.");
        url = this.getClass().getResource("images/repository_icon.png");
        icon = new ImageIcon(url);
        im = icon.getImage();
        icon.setImage(im.getScaledInstance(25, -1, Image.SCALE_SMOOTH));
        this.addSourcesFromStorage = new JButton(icon);
        this.addSourcesFromStorage.setToolTipText("Add a previously configured flat file from the repository.");
        this.addSourcesFromURL = new JButton("WWW");
        this.addSourcesFromURL.setToolTipText("Add a (possibly remote) flat file by giving its URL.");
        this.removeSources = new JButton("Remove data");
        this.removeSources.setEnabled(false);
        this.preview = new JButton("Preview");
        this.leastcolumn = new JSpinner();
        this.columns2show = new JSpinner();
        this.leastrow = new JSpinner();
        this.rows2show = new JSpinner();
        Object[] columnNames = new Object[2];
        columnNames[0] = "File Source";
        columnNames[1] = "URL";
        int rowCount = 10;
        this.sourcemodel = new FileSourceTableModel();
        this.fileselector = new JTable(this.sourcemodel);
        TableColumn col = this.fileselector.getColumnModel().getColumn(1);
        int width = 50;
        col.setPreferredWidth(width);
        col.setMinWidth(width);
        col.setMaxWidth(width);
        ListSelectionModel selectModel = this.fileselector.getSelectionModel();
        selectModel.addListSelectionListener(new ListSelectionListener() {

            public void valueChanged(ListSelectionEvent ev) {
                int selectedRow = fileselector.getSelectedRow();
                System.out.println("Number of selected row is:" + selectedRow);
                if (selectedRow >= 0) {
                    int[] multselected = fileselector.getSelectedRows();
                    if (multselected.length == 1) {
                        lastselectedsourcerow = selectedRow;
                        FlatFileDOM dom = sourcemodel.getFlatFileDOM(selectedRow);
                        showDetailsFor(dom);
                    }
                }
            }
        });
        JScrollPane jsp = new JScrollPane(this.fileselector);
        jsp.setPreferredSize(new Dimension(100, 100));
        jsp.setVerticalScrollBarPolicy(JScrollPane.VERTICAL_SCROLLBAR_ALWAYS);
        jsp.setHorizontalScrollBarPolicy(JScrollPane.HORIZONTAL_SCROLLBAR_ALWAYS);
        this.fileselector.addMouseListener(new MouseAdapter() {

            public void mouseClicked(MouseEvent e) {
            }

            public void mousePressed(MouseEvent e) {
                System.out.println("Mouse was pressed.");
                if (e.isPopupTrigger()) {
                    System.out.println("Popup triggered.");
                    int sr = fileselector.getSelectedRow();
                    if (sr >= 0) {
                        String fn = (String) sourcemodel.getValueAt(sr, 0);
                        System.out.println("FILE is " + fn);
                        try {
                            Process p = new ProcessBuilder("firefox", fn).start();
                        } catch (Exception err) {
                            throw new RuntimeException("Unable to open web browser.", err);
                        }
                    }
                }
            }

            public void mouseReleased(MouseEvent e) {
            }
        });
        Box controlBox = Box.createHorizontalBox();
        JPanel buttonPanel = new JPanel(new GridLayout(1, 3));
        buttonPanel.add(this.addSources);
        buttonPanel.add(this.addSourcesFromURL);
        buttonPanel.add(this.addSourcesFromStorage);
        buttonPanel.setPreferredSize(new Dimension(250, 30));
        buttonPanel.setMaximumSize(new Dimension(250, 30));
        buttonPanel.setMinimumSize(new Dimension(250, 30));
        controlBox.add(buttonPanel);
        controlBox.add(this.removeSources);
        controlBox.add(this.preview);
        controlBox.add(this.waitIndicator);
        Box scrollBox = Box.createVerticalBox();
        Box commentBox = Box.createVerticalBox();
        commentBox.add(new JScrollPane(commentTextArea));
        commentBox.setBorder(BorderFactory.createTitledBorder(BorderFactory.createEtchedBorder(), "File set description:"));
        scrollBox.add(commentBox);
        scrollBox.add(controlBox);
        scrollBox.add(jsp);
        JLabel label = new JLabel("Sort protocol:");
        Box protoBox = Box.createHorizontalBox();
        protoBox.add(label);
        protoBox.add(this.sortProtocols);
        protoBox.setPreferredSize(new Dimension(500, 50));
        protoBox.setMaximumSize(new Dimension(500, 50));
        scrollBox.add(protoBox);
        Box srcbox = Box.createHorizontalBox();
        srcbox.add(scrollBox);
        srcbox.add(Box.createVerticalGlue());
        Box detailsbox = Box.createVerticalBox();
        JPanel detailsPanel = new JPanel();
        detailsPanel.setLayout(new GridLayout(1, 2));
        detailsPanel.setBackground(Color.LIGHT_GRAY);
        Box detailsBox = Box.createVerticalBox();
        detailsPanel.setBorder(BorderFactory.createTitledBorder(BorderFactory.createLoweredBevelBorder(), "Selected file source:"));
        Box jointBox = Box.createHorizontalBox();
        jointBox.add(this.uploadflatfileButton);
        jointBox.add(this.flatfilenicknameText);
        jointBox.add(this.flatFileEnabledRadio);
        detailsBox.add(jointBox);
        detailsBox.add(Box.createVerticalGlue());
        Box cBox = Box.createHorizontalBox();
        cBox.add(new JLabel("Comment:"));
        JScrollPane csp = new JScrollPane(this.flatfileCommentArea);
        csp.setVerticalScrollBarPolicy(JScrollPane.VERTICAL_SCROLLBAR_ALWAYS);
        cBox.add(csp);
        cBox.setPreferredSize(new Dimension(100, 200));
        detailsBox.add(cBox);
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
                    updateDetailsFor(selectedRow);
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
                    updateDetailsFor(selectedRow);
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
                    updateDetailsFor(selectedRow);
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
        fsp.setHorizontalScrollBarPolicy(JScrollPane.HORIZONTAL_SCROLLBAR_ALWAYS);
        fsp.setVerticalScrollBarPolicy(JScrollPane.VERTICAL_SCROLLBAR_NEVER);
        fsp.setPreferredSize(new Dimension(300, 50));
        fsp.setMaximumSize(new Dimension(300, 50));
        fsp.setMinimumSize(new Dimension(300, 50));
        jointBox.add(label);
        jointBox.add(fsp);
        detailsBox.add(jointBox);
        detailsBox.add(Box.createVerticalGlue());
        jointBox = Box.createHorizontalBox();
        label = new JLabel("Field Delimiter:");
        this.fieldDelimiter = new JTextField("\\t");
        this.fieldDelimiter.addActionListener(new ActionListener() {

            public void actionPerformed(ActionEvent ev) {
                int selectedRow = fileselector.getSelectedRow();
                if (selectedRow >= 0) {
                    updateDetailsFor(selectedRow);
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
                    updateDetailsFor(selectedRow);
                }
            }
        });
        this.copyFormatingToSelected = new JButton("Copy format");
        this.copyFormatingToSelected.setToolTipText("Copies this formating specification to the selected sources in the set.");
        this.copyFormatingToSelected.addActionListener(new ActionListener() {

            public void actionPerformed(ActionEvent ev) {
                int selectedRow = fileselector.getSelectedRow();
                if (selectedRow >= 0) {
                    selectedRow = lastselectedsourcerow;
                    FlatFileDOM srcDOM = sourcemodel.getFlatFileDOM(selectedRow);
                    if (srcDOM == null) {
                        return;
                    }
                    int[] multselected = fileselector.getSelectedRows();
                    if (multselected.length > 1) {
                        for (int j = 0; j < multselected.length; j++) {
                            if (j != selectedRow) {
                                FlatFileDOM destDOM = (FlatFileDOM) sourcemodel.getFlatFileDOM(multselected[j]);
                                destDOM.setSameFormatAs(srcDOM);
                            }
                        }
                    }
                }
            }
        });
        jointBox.add(label);
        jointBox.add(this.recordDelimiter);
        detailsBox.add(jointBox);
        detailsBox.add(this.copyFormatingToSelected);
        detailsBox.add(Box.createVerticalGlue());
        detailsBox.add(Box.createVerticalGlue());
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
                    updateDetailsFor(selectedRow);
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
                    updateDetailsFor(selectedRow);
                }
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
                        } else {
                            String[] putative_cols = strLine.split(FD);
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
                            }
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
                int selectedRow = fileselector.getSelectedRow();
                if ((selectedRow < sourcemodel.getRowCount()) && (selectedRow >= 0)) {
                    updateDetailsFor(selectedRow);
                }
            }
        });
        Box topbox = Box.createHorizontalBox();
        topbox.add(srcbox);
        topbox.add(detailsPanel);
        Box mainbox = Box.createVerticalBox();
        Box maintenanceBox = Box.createHorizontalBox();
        Box setBox = Box.createHorizontalBox();
        setBox.add(this.clearButton);
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
        mainbox.add(maintenanceBox);
        mainbox.add(topbox);
        mainbox.add(previewPanel);
        this.add(mainbox);
        this.removeSources.addActionListener(new ActionListener() {

            public void actionPerformed(ActionEvent ev) {
                int[] rows = fileselector.getSelectedRows();
                for (int k = 0; k < rows.length; k++) {
                    removeFlatFile(k);
                }
                if (anyNonEmptySources()) {
                    removeSources.setEnabled(true);
                    allowFormatParsing(true);
                } else {
                    removeSources.setEnabled(false);
                    allowFormatParsing(false);
                }
            }
        });
        this.addSources.addActionListener(new ActionListener() {

            public void actionPerformed(ActionEvent ev) {
                int option = chooser.showOpenDialog(null);
                File[] files = null;
                if (option == JFileChooser.APPROVE_OPTION) {
                    files = chooser.getSelectedFiles();
                    int selectedRow = fileselector.getSelectedRow();
                    boolean appendFiles = false;
                    if (selectedRow < 0) {
                        appendFiles = true;
                        selectedRow = fileselector.getRowCount() - 1;
                    }
                    String currentRepos = repositoryView.getText();
                    for (int j = 0; j < files.length; j++) {
                        String fn = files[j].getAbsolutePath();
                        String proposed_nickname = getProposedNickname(currentRepos, fn, false);
                        FlatFileDOM dom = new FlatFileDOM();
                        dom.setNickname(proposed_nickname);
                        dom.setFilename(fn);
                        dom.setEnabled("true");
                        if (appendFiles) {
                            addFlatFile(dom);
                        } else {
                            addFlatFile(selectedRow + j, dom);
                        }
                    }
                }
                if (anyNonEmptySources()) {
                    removeSources.setEnabled(true);
                    allowFormatParsing(true);
                } else {
                    removeSources.setEnabled(false);
                    allowFormatParsing(false);
                }
                sourcemodel.fireTableDataChanged();
                int r = fileselector.getRowCount() - 1;
                fileselector.getSelectionModel().setSelectionInterval(r, r);
            }
        });
        this.addSourcesFromStorage.addActionListener(new ActionListener() {

            public void actionPerformed(ActionEvent ev) {
                findFile.setVisible(true);
            }
        });
        this.addSourcesFromURL.addActionListener(new ActionListener() {

            public void actionPerformed(ActionEvent ev) {
                String yourURL = JOptionPane.showInputDialog("Please input a URL");
                if (yourURL != null) {
                    yourURL = yourURL.trim();
                }
                if ((yourURL != null) && (yourURL.length() > 0)) {
                    String currentRepos = repositoryView.getText();
                    FlatFileDOM dom = new FlatFileDOM();
                    try {
                        try {
                            URL url = new URL(yourURL);
                        } catch (java.net.MalformedURLException err1) {
                            yourURL = "http://" + yourURL;
                            URL url = new URL(yourURL);
                        }
                    } catch (Exception err) {
                        throw new RuntimeException("Invalid URL.", err);
                    }
                    dom.setNickname(getProposedNickname(currentRepos, yourURL, true));
                    dom.setFilename(yourURL);
                    dom.setEnabled("true");
                    dom.setIsURL("true");
                    sourcemodel.addFlatFile(dom);
                } else {
                    return;
                }
                if (anyNonEmptySources()) {
                    removeSources.setEnabled(true);
                    allowFormatParsing(true);
                } else {
                    removeSources.setEnabled(false);
                    allowFormatParsing(false);
                }
                sourcemodel.fireTableDataChanged();
                int r = fileselector.getRowCount() - 1;
                fileselector.getSelectionModel().setSelectionInterval(r, r);
            }
        });
        this.preview.addActionListener(new ActionListener() {

            public void actionPerformed(ActionEvent ev) {
                complete_preview = false;
                waitIndicator.setText("|");
                Thread th = new Thread(new Runnable() {

                    public void run() {
                        while (true) {
                            String txt = waitIndicator.getText();
                            if (txt.equals("-")) {
                                waitIndicator.setText("\\");
                            }
                            if (txt.equals("\\")) {
                                waitIndicator.setText("|");
                            }
                            if (txt.equals("|")) {
                                waitIndicator.setText("/");
                            }
                            if (txt.equals("/")) {
                                waitIndicator.setText("-");
                            }
                            try {
                                Thread.sleep(100);
                            } catch (Exception err) {
                                throw new RuntimeException("Problem waiting in thread.", err);
                            }
                            waitIndicator.repaint();
                            if (complete_preview) {
                                waitIndicator.setText("X");
                                break;
                            }
                        }
                    }
                });
                th.start();
                Thread th2 = new Thread(new Runnable() {

                    public void run() {
                        FlatFileDOM[] filespecs = new FlatFileDOM[sourcemodel.getRowCount()];
                        for (int j = 0; j < sourcemodel.getRowCount(); j++) {
                            filespecs[j] = sourcemodel.getFlatFileDOM(j);
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
                        }
                        FlatFileSet dataset = new FlatFileSet(filespecs);
                        dataset.setSortProtocol(getSortProtocol());
                        for (int j = 0; j < hdrs.size(); j++) {
                            dataset.addColumn((String) hdrs.get(j), (Class) types.get(j));
                        }
                        System.out.println("The dataset rc is " + dataset.getRowCount());
                        previewPanel.setDataSet(dataset);
                        previewPanel.setVerticalScrollIntermittant(true);
                        previewPanel.setHorizontalScrollIntermittant(true);
                        previewPanel.setEditable(false);
                        if (anyNonEmptySources()) {
                            allowFormatParsing(true);
                        } else {
                            allowFormatParsing(false);
                        }
                        complete_preview = true;
                    }
                });
                th2.start();
            }
        });
        allowFormatParsing(false);
        this.formatTable.repaint();
        String last_repos = Prefs.getConfigValue("default", "lastrepository").trim();
        if (this.Config.hasRepository(last_repos)) {
            this.setRepository(last_repos);
        }
    }

    private String getProposedNickname(String currentRepos, String fn, boolean isurl) {
        if (isurl) {
            URL url = null;
            try {
                url = new URL(fn);
            } catch (Exception err) {
                throw new RuntimeException("Problem creating URL.", err);
            }
            System.out.println("Host:" + url.getHost());
            String[] parts = url.getHost().split("\\.");
            String NICKNAMEDOMAIN = parts[parts.length - 1];
            for (int j = parts.length - 2; j > 0; j--) {
                NICKNAMEDOMAIN = NICKNAMEDOMAIN + "." + parts[j];
            }
            System.out.println("NICKNAMEDOMAIN=" + NICKNAMEDOMAIN);
            String PATH = url.getPath();
            PATH = PATH.trim();
            if ((PATH == null) || (PATH.length() == 0)) {
                PATH = "index.html";
            }
            parts = PATH.split("\\.");
            PATH = "";
            for (int j = 0; j < parts.length - 1; j++) {
                if (j == 0) {
                    PATH = parts[j];
                } else {
                    PATH = PATH + "." + parts[j];
                }
            }
            PATH = PATH.replaceAll("/", "_");
            PATH = NICKNAMEDOMAIN + "." + PATH;
            return (PATH);
        } else {
            String NICKNAMEDOMAIN = this.Prefs.getConfigValue("domainname");
            String NICKNAMEFMT = "${BASENAME}";
            File fnfile = new File(fn);
            String basenamevalue = fnfile.getName();
            String[] parts = basenamevalue.split("\\.");
            basenamevalue = "";
            for (int j = 0; j < parts.length - 1; j++) {
                if (j == 0) {
                    basenamevalue = parts[j];
                } else {
                    basenamevalue = basenamevalue + "." + parts[j];
                }
            }
            NICKNAMEFMT = NICKNAMEFMT.replaceAll("\\$\\{BASENAME\\}", basenamevalue);
            return (NICKNAMEDOMAIN + "." + NICKNAMEFMT);
        }
    }

    public void fireRepositoryChanged(RepositoryEvent ev) {
        for (int j = 0; j < this.reposListeners.size(); j++) {
            RepositoryListener l = (RepositoryListener) this.reposListeners.get(j);
            l.setRepository(ev.getRepository());
        }
    }

    private class FormatTableModelListener implements TableModelListener {

        public FormatTableModelListener() {
        }

        public void tableChanged(TableModelEvent e) {
            if (e.getType() == TableModelEvent.UPDATE) {
                int selectedRow = fileselector.getSelectedRow();
                if ((selectedRow < sourcemodel.getRowCount()) && (selectedRow >= 0)) {
                    updateDetailsFor(selectedRow);
                }
            }
        }
    }

    public void beforeTransferStorage() {
        this.SHOULD_PULL_FLATFILES_FROM_REPOSITORY = true;
    }

    public void afterTransferStorage() {
        this.SHOULD_PULL_FLATFILES_FROM_REPOSITORY = false;
        if (fileselector.getRowCount() > -1) {
            fileselector.changeSelection(0, 0, false, false);
        }
        if (anyNonEmptySources()) {
            removeSources.setEnabled(true);
            allowFormatParsing(true);
        } else {
            removeSources.setEnabled(false);
            allowFormatParsing(false);
        }
    }

    public void beforeCopyStorage() {
        this.SHOULD_PULL_FLATFILES_FROM_REPOSITORY = true;
    }

    public void afterCopyStorage() {
        this.SHOULD_PULL_FLATFILES_FROM_REPOSITORY = false;
        if (fileselector.getRowCount() > -1) {
            fileselector.changeSelection(0, 0, false, false);
        }
        if (anyNonEmptySources()) {
            removeSources.setEnabled(true);
            allowFormatParsing(true);
        } else {
            removeSources.setEnabled(false);
            allowFormatParsing(false);
        }
    }

    public void transferStorageCommands(RepositoryStorage x) {
        this.transferAgent.transferStorageCommands(x);
    }

    public void copyStorageCommands(RepositoryStorage x) {
        this.transferAgent.copyStorageCommands(x);
    }

    public void setRepository(String repos) {
        try {
            System.out.println("Setting flatfilesetframe repository to " + repos);
            this.repositoryView.setText(repos);
            this.Connection = (FlatFileStorageConnectivity) this.connectionHandler.getConnection(repos);
            this.findSet.restrictToRepository(repos);
            this.findFile.restrictToRepository(repos);
            this.repositoryEditor.setCurrentRepository(repos);
            Prefs.setConfigValue("default", "lastrepository", repositoryView.getText());
            Prefs.saveConfig();
            RepositoryEvent ev = new RepositoryEvent(this, repos);
            fireRepositoryChanged(ev);
        } catch (Exception err) {
            JOptionPane.showMessageDialog(null, "Unable to connect to repository " + repos + ".  Maintaining connection to 'default' repository instead.");
            setRepository("default");
        }
    }

    public Class getDefaultGraphicalEditorClass() {
        return (FlatFileSetFrame.class);
    }

    public String getRepository() {
        return (this.repositoryView.getText());
    }

    private void updateFormatTable() {
        if (formatmodel != null) {
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
    }

    private void updateDetailsFor(int row) {
        if (!this.NoCallbackChangeMode) {
            FlatFileDOM dom = (FlatFileDOM) this.sourcemodel.getFlatFileDOM(row);
            if (dom != null) {
                dom.setNickname(this.flatfilenicknameText.getText());
                dom.setComment(this.flatfileCommentArea.getText());
                dom.setPreHeaderLines((String) ((Integer) this.preheaderlines.getValue()).toString());
                dom.setPostHeaderLines((String) ((Integer) this.postheaderlines.getValue()).toString());
                if (this.hasHeaderLineBox.isSelected()) {
                    dom.setHasHeaderLine("true");
                } else {
                    dom.setHasHeaderLine("false");
                }
                if (this.flatFileEnabledRadio.isSelected()) {
                    dom.setEnabled("true");
                } else {
                    dom.setEnabled("false");
                }
                dom.setFieldDelimiter(this.fieldDelimiter.getText());
                dom.setRecordDelimiter(this.recordDelimiter.getText());
                dom.setFormat(formatmodel.getFormat());
            }
        }
    }

    public void showDetailsFor(FlatFileDOM fd) {
        this.NoCallbackChangeMode = true;
        this.flatfilenicknameText.setText(fd.getNickname());
        if (fd.getEnabled().equalsIgnoreCase("true")) {
            this.flatFileEnabledRadio.setSelected(true);
        } else {
            this.flatFileEnabledRadio.setSelected(false);
        }
        this.flatfileCommentArea.setText(fd.getComment());
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
        this.formatmodel.setFormat(fd.getFormat());
        updateFormatTable();
        this.NoCallbackChangeMode = false;
    }

    public int fileSetSize() {
        return (sourcemodel.fileSetSize());
    }

    private class FileSourceTableModel extends AbstractTableModel {

        private Vector flatfiledoms;

        public FileSourceTableModel() {
            this.flatfiledoms = new Vector();
        }

        public void addFlatFile(int row, FlatFileDOM dom) {
            this.flatfiledoms.add(row, dom);
            this.fireTableDataChanged();
        }

        public void addFlatFile(FlatFileDOM dom) {
            if (!dom.getEnabled().equalsIgnoreCase("true")) {
                JOptionPane.showMessageDialog(null, "The flat file " + dom.getNickname() + " is disabled and may become unavailable in the future.");
            }
            this.flatfiledoms.add(dom);
            this.fireTableDataChanged();
        }

        public void addFlatFile(int index, String nickname) {
            FlatFileDOM dom = null;
            if (SHOULD_PULL_FLATFILES_FROM_REPOSITORY) {
                FlatFileStorage ff = (FlatFileStorage) Connection.getStorage(nickname);
                dom = new FlatFileDOM();
                dom.transferStorage(ff);
            } else {
                dom = new FlatFileDOM();
                dom.setNickname(nickname);
                dom.setFilename("Untitled");
                dom.setEnabled("true");
            }
            this.flatfiledoms.add(index, dom);
            this.fireTableDataChanged();
        }

        /**
		Appends the specified FlatFileStorage nickname to the end of this list.
		*/
        public void addFlatFile(String nickname) {
            System.out.println("Adding flatfile " + nickname + " to the sourcemodel.");
            FlatFileDOM dom = null;
            if (SHOULD_PULL_FLATFILES_FROM_REPOSITORY) {
                FlatFileStorage ff = (FlatFileStorage) Connection.getStorage(nickname);
                dom = new FlatFileDOM();
                dom.transferStorage(ff);
            } else {
                dom = new FlatFileDOM();
                dom.setNickname(nickname);
                dom.setFilename("Untitled");
                dom.setEnabled("true");
            }
            this.flatfiledoms.add(dom);
            this.fireTableDataChanged();
        }

        /** 
		Inserts all of the elements in the specified collection into this list 
		at the specified position (optional operation).
		*/
        public void addAllFlatFiles(int index, Collection s) {
            Iterator iter = s.iterator();
            while (iter.hasNext()) {
                Object nn = iter.next();
                if (nn instanceof String) {
                    this.addFlatFile(index, (String) nn);
                } else if (nn instanceof FlatFileDOM) {
                    this.addFlatFile(index, (FlatFileDOM) nn);
                } else {
                    throw new RuntimeException("Invalid class for FlatFile Collection element.");
                }
            }
        }

        /**
		Appends all of the FlatFileStorage nicknames in the specified 
		collection to the end of this list, in the order that they are 
		returned by the specified  collection's iterator (optional operation).
		*/
        public void addAllFlatFiles(Collection s) {
            Iterator iter = s.iterator();
            while (iter.hasNext()) {
                Object nn = iter.next();
                if (nn instanceof String) {
                    this.addFlatFile((String) nn);
                } else if (nn instanceof FlatFileDOM) {
                    this.addFlatFile((FlatFileDOM) nn);
                } else {
                    throw new RuntimeException("Invalid class for FlatFile Collection element.");
                }
            }
        }

        public void clearFlatFiles() {
            this.flatfiledoms.clear();
            this.fireTableDataChanged();
        }

        public boolean containsFlatFile(String nickname) {
            boolean out = false;
            int loc = indexOfFlatFile(nickname);
            if (loc >= 0) {
                out = true;
            }
            return (out);
        }

        public int fileSetSize() {
            return (this.getRowCount());
        }

        public boolean containsAllFlatFiles(Collection nicknames) {
            boolean out = false;
            if (nicknames.size() == this.fileSetSize()) {
                Iterator nniter = nicknames.iterator();
                while (nniter.hasNext()) {
                    Object nn = nniter.next();
                    if (nn instanceof String) {
                        boolean t = this.containsFlatFile((String) nn);
                        if (t == false) {
                            break;
                        }
                    } else if (nn instanceof FlatFileDOM) {
                        String n = ((FlatFileDOM) nn).getNickname();
                        boolean t = this.containsFlatFile(n);
                        if (t == false) {
                            break;
                        }
                    } else {
                        throw new RuntimeException("Invalid class for FlatFile Collection element.");
                    }
                }
            }
            return (out);
        }

        public void removeFlatFile(int index) {
            this.flatfiledoms.remove(index);
            this.fireTableDataChanged();
        }

        public void removeFlatFile(String nickname) {
            int index = this.indexOfFlatFile(nickname);
            this.removeFlatFile(index);
            this.fireTableDataChanged();
        }

        public void removeAllFlatFiles(Collection nicknames) {
            Iterator nniter = nicknames.iterator();
            while (nniter.hasNext()) {
                Object nn = nniter.next();
                if (nn instanceof String) {
                    this.removeFlatFile((String) nn);
                } else if (nn instanceof FlatFileDOM) {
                    String n = ((FlatFileDOM) nn).getNickname();
                    this.removeFlatFile(n);
                } else {
                    throw new RuntimeException("Invalid class for FlatFile Collection element.");
                }
            }
            this.fireTableDataChanged();
        }

        public int indexOfFlatFile(String nickname) {
            int out = -1;
            for (int j = 0; j < this.flatfiledoms.size(); j++) {
                FlatFileDOM dom = this.getFlatFileDOM(j);
                String nn = dom.getNickname();
                if (nn.equals(nickname)) {
                    out = j;
                    break;
                }
            }
            return (out);
        }

        public Vector getFlatFileNames() {
            Vector out = new Vector();
            for (int j = 0; j < this.flatfiledoms.size(); j++) {
                FlatFileDOM dom = (FlatFileDOM) this.flatfiledoms.get(j);
                out.add(dom.getNickname());
            }
            return (out);
        }

        public FlatFileDOM getFlatFileDOM(int row) {
            return ((FlatFileDOM) this.flatfiledoms.get(row));
        }

        public void removeRow(int r) {
            flatfiledoms.removeElementAt(r);
            this.fireTableDataChanged();
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

        public boolean isCellEditable(int rowIndex, int columnIndex) {
            return (true);
        }

        public int getRowCount() {
            return (this.flatfiledoms.size());
        }

        public int getColumnCount() {
            return (2);
        }

        public String getColumnName(int col) {
            String out = "";
            if (col == 0) {
                out = "File Source";
            } else if (col == 1) {
                out = "URL";
            } else {
                throw new RuntimeException("Invalid column number.");
            }
            return (out);
        }

        public Object getValueAt(int row, int col) {
            FlatFileDOM rowvals = (FlatFileDOM) this.flatfiledoms.get(row);
            Object out = null;
            if (col == 0) {
                out = rowvals.getFilename();
                System.out.println("The gotten filename was " + out);
            } else if (col == 1) {
                String isurl = rowvals.getIsURL();
                if (isurl.equalsIgnoreCase("true")) {
                    out = new Boolean(true);
                } else {
                    out = new Boolean(false);
                }
            } else {
                throw new RuntimeException("Invalid column number");
            }
            return (out);
        }

        public void setValueAt(Object obj, int row, int col) {
            FlatFileDOM rowvals = (FlatFileDOM) this.flatfiledoms.get(row);
            if (col == 0) {
                rowvals.setFilename((String) obj);
            } else if (col == 1) {
                if (obj instanceof Boolean) {
                    boolean isurl = ((Boolean) obj).booleanValue();
                    if (isurl) {
                        rowvals.setIsURL("true");
                    } else {
                        rowvals.setIsURL("false");
                    }
                } else if (obj instanceof String) {
                    String isurl = (String) obj;
                    if (isurl.equalsIgnoreCase("true")) {
                        rowvals.setIsURL("true");
                    } else {
                        rowvals.setIsURL("false");
                    }
                } else {
                    throw new RuntimeException("Invalid type for IsURL in table.");
                }
            } else {
                throw new RuntimeException("Invalid column number.");
            }
            this.fireTableRowsUpdated(row, row);
        }
    }

    public int indexOfFlatFile(String nickname) {
        return (this.sourcemodel.indexOfFlatFile(nickname));
    }

    public void addFlatFile(String nn) {
        this.sourcemodel.addFlatFile(nn);
    }

    public void addFlatFile(int j, String nn) {
        this.sourcemodel.addFlatFile(j, nn);
    }

    public void addAllFlatFiles(Collection objs) {
        this.sourcemodel.addAllFlatFiles(objs);
    }

    public void addAllFlatFiles(int j, Collection objs) {
        this.sourcemodel.addAllFlatFiles(j, objs);
    }

    public void removeAllFlatFiles(Collection objs) {
        this.sourcemodel.removeAllFlatFiles(objs);
    }

    public void removeFlatFile(String nn) {
        this.sourcemodel.removeFlatFile(nn);
    }

    public void removeFlatFile(int n) {
        this.sourcemodel.removeFlatFile(n);
    }

    public boolean containsAllFlatFiles(Collection objs) {
        return (this.sourcemodel.containsAllFlatFiles(objs));
    }

    public boolean containsFlatFile(String nn) {
        return (this.sourcemodel.containsFlatFile(nn));
    }

    public void clearFlatFiles() {
        this.sourcemodel.clearFlatFiles();
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
            String fmt = "";
            for (int j = 0; j < v.size(); j++) {
                fmt = fmt + (String) v.get(j);
            }
            this.setFormat(fmt);
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
            for (int j = 0; j < this.fmtparts.length; j++) {
                this.fmtparts[j] = "%" + parts[j + 1];
            }
            updateFormatTable();
        }

        public String getFormat() {
            return (this.FMT);
        }
    }

    public String getHasHeaderLine() {
        String out = "false";
        if (this.hasHeaderLineBox.isSelected()) {
            out = "true";
        }
        return (out);
    }

    public void setHasHeaderLine(String f) {
        if ((f.equalsIgnoreCase("true")) || (f.equalsIgnoreCase("1"))) {
            this.hasHeaderLineBox.setSelected(true);
        } else {
            this.hasHeaderLineBox.setSelected(false);
        }
    }

    public String getSortProtocol() {
        String sortproto = (String) this.sortProtocols.getSelectedItem();
        return (sortproto);
    }

    public void setSortProtocol(String proto) {
        this.sortProtocols.setSelectedItem(proto);
    }

    private class SelectListener implements ActionListener {

        public void actionPerformed(ActionEvent e) {
            String newrepos = repositoryEditor.getCurrentRepository();
            setRepository(newrepos);
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
        return (this.sourcemodel.getFlatFileNames());
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
        if ((n.equals("1")) || (n.equals("true"))) {
            isenabled = true;
        }
        this.enabledRadio.setSelected(isenabled);
    }

    public String getEnabled() {
        String out = null;
        if (this.enabledRadio.isSelected()) {
            out = "true";
        } else {
            out = "false";
        }
        return (out);
    }

    public void removeRepositoryListener(RepositoryListener l) {
        this.reposListeners.remove(l);
    }

    public void addRepositoryListener(RepositoryListener l) {
        this.reposListeners.add(l);
    }

    public void transferStorage(RepositoryStorage that) {
        this.transferAgent.transferStorage(that);
    }

    public void copyStorage(RepositoryStorage that) {
        this.transferAgent.copyStorage(that);
    }

    private boolean anyNonEmptySources() {
        boolean out = false;
        if (sourcemodel.fileSetSize() > 0) {
            out = true;
        }
        return (out);
    }

    public Class getDOMStorageClass() {
        return (FlatFileSetDOM.class);
    }

    public boolean executeTransfer() {
        boolean out = false;
        this.setEditable(false);
        String repos = this.repositoryView.getText();
        String setName = this.getNickname();
        if (!this.Connection.storageExists(setName)) {
            boolean success = this.Connection.createStorage(FlatFileSetStorage.class, setName);
            if (!success) {
                throw new RuntimeException("Failed to create storage of " + FlatFileSetXML.class + " named " + setName + ".");
            }
        } else {
            Object[] options = { "Ok", "Cancel" };
            int n = JOptionPane.showOptionDialog(FlatFileSetFrame.this, "Overwrite the existing definition " + setName + " in repository " + repos + "?", "Previously defined storage", JOptionPane.YES_NO_CANCEL_OPTION, JOptionPane.QUESTION_MESSAGE, null, options, options[1]);
            String ans = (String) options[n];
            if (ans.equalsIgnoreCase("Cancel")) {
                return (out);
            }
        }
        this.backingStorage = (FlatFileSetStorage) this.Connection.getStorage(setName);
        if (this.backingStorage == null) {
            try {
                boolean success = this.Connection.createStorage(FlatFileSetStorage.class, setName);
                if (success) {
                    this.backingStorage = (FlatFileSetStorage) this.Connection.getStorage(setName);
                }
            } catch (Exception err) {
                throw new RuntimeException("Unable to retrieve storage " + setName + " from repository " + repos + ".", err);
            }
        }
        if (this.backingStorage == null) {
            throw new RuntimeException("Retrieved storage is null.");
        }
        this.backingStorage.transferStorage(this);
        this.setEditable(true);
        out = true;
        return (out);
    }

    public Class getStorageTransferAgentClass() {
        return (FlatFileSetTransferAgent.class);
    }

    public void addFlatFile(String nickname, int userow) {
        FlatFileDOM dom = new FlatFileDOM();
        dom.setNickname(nickname);
        this.addFlatFile(userow, dom);
    }

    public void addFlatFile(FlatFileDOM dom) {
        this.sourcemodel.addFlatFile(dom);
    }

    public void addFlatFile(int userow, FlatFileDOM dom) {
        this.sourcemodel.addFlatFile(userow, dom);
    }
}
