package org.openscience.cdkplugin.dadmlbrowser;

import java.awt.BorderLayout;
import java.awt.Dimension;
import java.awt.FlowLayout;
import java.awt.event.*;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.Reader;
import java.net.URI;
import java.net.URL;
import java.net.URLConnection;
import java.util.Date;
import java.util.Enumeration;
import java.util.Properties;
import java.util.Vector;
import javax.swing.AbstractAction;
import javax.swing.JButton;
import javax.swing.JComboBox;
import javax.swing.JFileChooser;
import javax.swing.JLabel;
import javax.swing.JMenu;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JSplitPane;
import javax.swing.JTabbedPane;
import javax.swing.JTable;
import javax.swing.JTextField;
import javax.swing.JTree;
import javax.swing.ListSelectionModel;
import javax.swing.event.*;
import javax.swing.filechooser.FileFilter;
import javax.swing.table.AbstractTableModel;
import javax.swing.tree.DefaultMutableTreeNode;
import org.openscience.cdk.Atom;
import org.openscience.cdk.AtomContainer;
import org.openscience.cdk.ChemFile;
import org.openscience.cdk.ChemModel;
import org.openscience.cdk.ChemObject;
import org.openscience.cdk.ChemSequence;
import org.openscience.cdk.applications.APIVersionTester;
import org.openscience.cdk.applications.plugin.*;
import org.openscience.cdk.applications.swing.SortedTableModel;
import org.openscience.cdk.interfaces.IChemObjectChangeEvent;
import org.openscience.cdk.exception.CDKException;
import org.openscience.cdk.internet.DADMLReader;
import org.openscience.cdk.internet.DADMLResult;
import org.openscience.cdk.io.IChemObjectReader;
import org.openscience.cdk.io.ReaderFactory;
import org.openscience.cdk.tools.LoggingTool;

/**
 * @author Egon Willighagen <egonw@sci.kun.nl>
 */
public class DadmlBrowserPlugin implements ICDKPlugin {

    private final String implementedCDKPluginAPIVersion = "1.14";

    private final String pluginVersion = "0.4";

    private String superdb = "http://jmol.sf.net/super.xml";

    private ICDKEditBus editBus = null;

    private ResultsContentModel channelContent = null;

    private SortedTableModel sortedContent = null;

    private JPanel pluginPanel = null;

    private JTextField textField;

    private ReaderFactory readerFactory = null;

    private LoggingTool logger;

    private JTextField valueText;

    private JComboBox typesBox;

    private Vector indices;

    public DadmlBrowserPlugin() {
        logger = new LoggingTool(this);
        indices = new Vector();
        indices.addElement("CAS-NUMBER");
        indices.addElement("pdbid");
    }

    public void start() {
        readerFactory = new ReaderFactory(5000);
    }

    public void stop() {
    }

    public void setEditBus(ICDKEditBus editBus) {
        this.editBus = editBus;
    }

    public String getName() {
        return "DadmlBrowser";
    }

    public String getAPIVersion() {
        return implementedCDKPluginAPIVersion;
    }

    public String getPluginVersion() {
        return pluginVersion;
    }

    public String getPluginLicense() {
        return "GPL";
    }

    public void setPropertyDirectory(String dir) {
        Properties props = readProperties(dir);
    }

    public JPanel getPluginPanel() {
        if (pluginPanel == null) {
            pluginPanel = createPanel();
        }
        return pluginPanel;
    }

    public void stateChanged(IChemObjectChangeEvent event) {
    }

    private JPanel createPanel() {
        JPanel viewerPanel = new JPanel(new BorderLayout());
        JTabbedPane tabbedPane = new JTabbedPane();
        tabbedPane.add("Index", createBasicPanel());
        tabbedPane.add("URI", createURIPanel());
        viewerPanel.add(tabbedPane, BorderLayout.NORTH);
        viewerPanel.add(createResultPanel(), BorderLayout.CENTER);
        viewerPanel.validate();
        return viewerPanel;
    }

    private JPanel createBasicPanel() {
        JPanel basicPanel = new JPanel(new BorderLayout());
        JPanel northPanel = new JPanel();
        typesBox = new JComboBox(indices);
        JLabel typesLabel = new JLabel("Index: ");
        northPanel.add(typesLabel);
        northPanel.add(typesBox);
        JLabel valueLabel = new JLabel("Value: ");
        valueText = new JTextField(20);
        valueText.addActionListener(new EditAction());
        northPanel.add(valueLabel);
        northPanel.add(valueText);
        JButton openButton = new JButton("Search");
        openButton.addActionListener(new OpenAction());
        northPanel.add(openButton);
        basicPanel.add(northPanel, BorderLayout.NORTH);
        return basicPanel;
    }

    private JPanel createURIPanel() {
        JPanel dirPanel = new JPanel();
        dirPanel.setLayout(new FlowLayout(FlowLayout.RIGHT));
        JButton searchButton = new JButton("Search");
        searchButton.addActionListener(new ActionListener() {

            public void actionPerformed(ActionEvent e) {
                channelContent.cleanTable();
                DADMLReader reader = new DADMLReader(superdb);
                try {
                    URI uri = new URI(textField.getText());
                    Enumeration links = reader.resolveLinks(uri).elements();
                    while (links.hasMoreElements()) {
                        DADMLResult resource = (DADMLResult) links.nextElement();
                        if (channelContent != null) {
                            ChemModel model = new ChemModel();
                            model.setProperty("org.openscience.cdk.internet.DADMLResult", resource);
                            channelContent.addChemModel(model);
                        }
                    }
                } catch (Exception exception) {
                    JOptionPane.showMessageDialog(null, "Exception: " + exception.getMessage());
                    logger.debug(exception);
                }
            }
        });
        dirPanel.add(new JLabel("DADML URL:"));
        textField = new JTextField();
        textField.setColumns(40);
        textField.setText("dadml://");
        dirPanel.add(textField);
        dirPanel.add(searchButton);
        dirPanel.validate();
        return dirPanel;
    }

    private JPanel createResultPanel() {
        JPanel browserPanel = new JPanel(new BorderLayout());
        channelContent = new ResultsContentModel();
        sortedContent = new SortedTableModel(channelContent);
        JTable channelTable = new JTable(sortedContent);
        sortedContent.addMouseListenerToHeaderInTable(channelTable);
        channelTable.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);
        ListSelectionModel rowSM = channelTable.getSelectionModel();
        rowSM.addListSelectionListener(new ItemsTableListener(sortedContent, channelContent));
        channelTable.validate();
        JScrollPane contentPane = new JScrollPane(channelTable);
        contentPane.validate();
        browserPanel.add(contentPane, BorderLayout.CENTER);
        browserPanel.validate();
        return browserPanel;
    }

    ;

    public JPanel getPluginConfigPanel() {
        return null;
    }

    ;

    public JMenu getMenu() {
        return null;
    }

    ;

    private Properties readProperties(String directory) {
        Properties props = null;
        File uhome = new File(System.getProperty("user.home"));
        File propsFile = new File(directory + "/dirbrowser.props");
        logger.debug("User plugin dir: " + propsFile);
        logger.debug("         exists: " + propsFile.exists());
        if (propsFile.exists()) {
            try {
                FileInputStream fis = new FileInputStream(propsFile);
                props = new Properties();
                props.load(fis);
                fis.close();
            } catch (Exception exception) {
                logger.error("Error while reading dirbrowser props: " + exception.toString());
            }
        }
        return props;
    }

    class ItemsTableListener implements ListSelectionListener {

        private SortedTableModel sortedModelContent = null;

        private ResultsContentModel modelContent = null;

        public ItemsTableListener(SortedTableModel sortedModelContent, ResultsContentModel modelContent) {
            this.modelContent = modelContent;
            this.sortedModelContent = sortedModelContent;
        }

        public void valueChanged(ListSelectionEvent e) {
            if (e.getValueIsAdjusting()) return;
            ListSelectionModel lsm = (ListSelectionModel) e.getSource();
            if (lsm.isSelectionEmpty()) {
            } else {
                int selectedRow = lsm.getMinSelectionIndex();
                ChemModel model = modelContent.getValueAt(sortedModelContent.getSortedIndex(selectedRow));
                DADMLResult resource = (DADMLResult) model.getProperty("org.openscience.cdk.internet.DADMLResult");
                URL url = resource.getURL();
                try {
                    URLConnection connection = url.openConnection();
                    InputStreamReader input = new InputStreamReader(connection.getInputStream());
                    if (APIVersionTester.isBiggerOrEqual("1.8", editBus.getAPIVersion())) {
                        try {
                            editBus.showChemFile(input);
                            return;
                        } catch (Exception exception) {
                            logger.error("EditBus error: ", exception.getMessage());
                            logger.debug(exception);
                        }
                    }
                    IChemObjectReader reader = readerFactory.createReader(input);
                    ChemFile chemFile = (ChemFile) reader.read(new ChemFile());
                    editBus.showChemFile(chemFile);
                } catch (FileNotFoundException exception) {
                    String error = "Resource not found: " + url;
                    logger.error(error);
                    JOptionPane.showMessageDialog(null, error);
                    return;
                } catch (Exception exception) {
                    String error = "Error while reading file: " + exception.getMessage();
                    logger.error(error);
                    logger.debug(exception);
                    JOptionPane.showMessageDialog(null, error);
                    return;
                }
                logger.warn("Not displaying model with unknown content");
            }
        }
    }

    class OpenAction extends AbstractAction {

        OpenAction() {
            super("Open");
            logger.debug("open defined");
        }

        public void actionPerformed(ActionEvent event) {
            logger.debug("open action performed");
            channelContent.cleanTable();
            String index = (String) typesBox.getSelectedItem();
            String value = valueText.getText();
            DADMLReader reader = new DADMLReader(superdb);
            try {
                URI uri = new URI("dadml://any/" + index + "?" + value);
                Enumeration links = reader.resolveLinks(uri).elements();
                while (links.hasMoreElements()) {
                    DADMLResult resource = (DADMLResult) links.nextElement();
                    if (channelContent != null) {
                        ChemModel model = new ChemModel();
                        model.setProperty("org.openscience.cdk.internet.DADMLResult", resource);
                        channelContent.addChemModel(model);
                    }
                }
            } catch (Exception exception) {
                JOptionPane.showMessageDialog(null, "Exception: " + exception.getMessage());
                logger.debug(exception);
            }
        }
    }

    class EditAction extends AbstractAction {

        EditAction() {
            super("Edit");
            logger.debug("edit action defined");
        }

        public void actionPerformed(ActionEvent e) {
            logger.debug("edit action performed");
        }
    }
}
