package org.openscience.cdkplugin.rssviewer;

import java.awt.BorderLayout;
import java.awt.Dimension;
import java.awt.FlowLayout;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.event.*;
import java.io.File;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.PrintStream;
import java.io.Reader;
import java.io.StringReader;
import java.net.URL;
import java.net.MalformedURLException;
import java.util.Calendar;
import java.util.Enumeration;
import java.util.Hashtable;
import java.util.Properties;
import java.util.Vector;
import javax.swing.AbstractAction;
import javax.swing.JButton;
import javax.swing.JComboBox;
import javax.swing.JDialog;
import javax.swing.JEditorPane;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JMenu;
import javax.swing.JMenuItem;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JSplitPane;
import javax.swing.JTabbedPane;
import javax.swing.JTable;
import javax.swing.JTextField;
import javax.swing.JTree;
import javax.swing.ListSelectionModel;
import javax.swing.SpringLayout;
import javax.swing.event.*;
import javax.swing.table.AbstractTableModel;
import javax.swing.tree.DefaultMutableTreeNode;
import javax.swing.tree.DefaultTreeModel;
import javax.swing.tree.TreeModel;
import org.openscience.cdk.Atom;
import org.openscience.cdk.AtomContainer;
import org.openscience.cdk.ChemFile;
import org.openscience.cdk.ChemModel;
import org.openscience.cdk.ChemSequence;
import org.openscience.cdk.Element;
import org.openscience.cdk.applications.APIVersionTester;
import org.openscience.cdk.applications.plugin.*;
import org.openscience.cdk.applications.swing.SortedTableModel;
import org.openscience.cdk.config.IsotopeFactory;
import org.openscience.cdk.event.ChemObjectChangeEvent;
import org.openscience.cdk.exception.CDKException;
import org.openscience.cdk.geometry.GeometryTools;
import org.openscience.cdk.io.ChemicalRSSReader;
import org.openscience.cdk.tools.LoggingTool;
import org.openscience.cdk.tools.MFAnalyser;
import org.openscience.cdk.tools.manipulator.ChemModelManipulator;

/**
 * Plugin that can read RSS sources and extract molecular content
 * in the CML2 format from it.
 *
 * @author Egon Willighagen <egonw@sci.kun.nl>
 */
public class RSSViewerPlugin implements CDKPluginInterface {

    private final String implementedCDKPluginAPIVersion = "1.8";

    private final String pluginVersion = "0.15";

    private final String newFeedTitle = "New Channel";

    private LoggingTool logger;

    private IsotopeFactory isotopeFactory;

    private CDKEditBus editBus = null;

    private Hashtable channels = null;

    private JTree rssTree = null;

    private RSSContentModel channelContent = null;

    private SortedTableModel sortedContent = null;

    private RSSContentModel aggregatedContent = null;

    private SortedTableModel sortedAggregatedContent = null;

    private JPanel pluginPanel = null;

    private JComboBox elementFilter = null;

    private StatusBar statusBar = null;

    private FeedLoaderThread feedLoadingThread = null;

    private FeedRefresherThread feedRefresherThread = null;

    private String propertyDir = null;

    private DefaultTreeModel treeModel = null;

    private RSSChannelInfoPanel rssChannelInfoPanel = null;

    private RSSItemInfoPanel rssItemInfoPanel = null;

    private RSSItemInfoPanel rssItemInfoPanel2 = null;

    private JTabbedPane tabbedPane = null;

    boolean feedListChanged = false;

    public RSSViewerPlugin() {
        channels = new Hashtable();
        logger = new LoggingTool(this);
        try {
            isotopeFactory = IsotopeFactory.getInstance();
        } catch (Exception exception) {
            logger.error("Could not instantiate IsotopeFactory: ", exception.getMessage());
            logger.debug(exception);
            isotopeFactory = null;
        }
    }

    public void setEditBus(CDKEditBus editBus) {
        this.editBus = editBus;
    }

    public void start() {
        if (aggregatedContent == null) {
            aggregatedContent = new RSSContentModel();
        }
        if (feedLoadingThread == null) {
            feedLoadingThread = new FeedLoaderThread();
            feedLoadingThread.start();
        }
        if (feedRefresherThread == null) {
            feedRefresherThread = new FeedRefresherThread();
            feedRefresherThread.start();
        }
        fillAggregatedTable();
    }

    public void stop() {
        saveProperties();
        channels.clear();
    }

    public String getName() {
        return "RSS Viewer";
    }

    public String getAPIVersion() {
        return implementedCDKPluginAPIVersion;
    }

    public String getPluginVersion() {
        return pluginVersion;
    }

    public void setPropertyDirectory(String dir) {
        this.propertyDir = dir;
        Enumeration channelEnum = readProperties(dir).elements();
        while (channelEnum.hasMoreElements()) {
            RSSChannel channel = (RSSChannel) channelEnum.nextElement();
            channels.put(channel.getURL(), channel);
        }
    }

    private void saveProperties() {
        File propsFile = new File(propertyDir + System.getProperty("file.separator") + "rssviewer.opml");
        logger.info("Saving RSS feed list");
        logger.debug("User plugin dir: " + propsFile);
        logger.debug("         exists: " + propsFile.exists());
        if (feedListChanged) {
            try {
                OPMLWriter fos = new OPMLWriter(new FileWriter(propsFile));
                fos.writeRSSChannels(channels);
                fos.close();
            } catch (Exception exception) {
                logger.error("Error while saving rssviewer props: ", exception.getMessage());
                logger.debug(exception);
            }
        } else {
            logger.debug("Feed list has not changed; not saving...");
        }
    }

    public JPanel getPluginPanel() {
        if (pluginPanel == null) {
            pluginPanel = createPanel();
        }
        return pluginPanel;
    }

    private JPanel createPanel() {
        JPanel RSSViewerPanel = new JPanel(new BorderLayout());
        rssTree = new JTree(createChannelTree());
        rssTree.addTreeSelectionListener(new RSSChannelTreeListener());
        rssTree.validate();
        JScrollPane treePanel = new JScrollPane(rssTree);
        treePanel.validate();
        rssItemInfoPanel = new RSSItemInfoPanel();
        rssItemInfoPanel2 = new RSSItemInfoPanel();
        channelContent = new RSSContentModel();
        sortedContent = new SortedTableModel(channelContent);
        JTable channelTable = new JTable(sortedContent);
        sortedContent.addMouseListenerToHeaderInTable(channelTable);
        channelTable.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);
        ListSelectionModel rowSM = channelTable.getSelectionModel();
        rowSM.addListSelectionListener(new RSSChannelItemsTableListener(sortedContent, channelContent, rssItemInfoPanel2));
        JScrollPane contentScrollerPane = new JScrollPane(channelTable);
        JPanel contentPane = new JPanel(new BorderLayout());
        contentPane.add(contentScrollerPane, BorderLayout.CENTER);
        contentPane.add(rssItemInfoPanel2, BorderLayout.SOUTH);
        contentPane.validate();
        if (aggregatedContent == null) {
            aggregatedContent = new RSSContentModel();
        }
        sortedAggregatedContent = new SortedTableModel(aggregatedContent);
        JTable aggregatedTable = new JTable(sortedAggregatedContent);
        sortedAggregatedContent.addMouseListenerToHeaderInTable(aggregatedTable);
        aggregatedTable.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);
        ListSelectionModel aggregatedRowSM = aggregatedTable.getSelectionModel();
        aggregatedRowSM.addListSelectionListener(new RSSChannelItemsTableListener(sortedAggregatedContent, aggregatedContent, rssItemInfoPanel));
        JScrollPane scrollableTable = new JScrollPane(aggregatedTable);
        JPanel filterPane = new JPanel();
        JLabel filterName = new JLabel("Filter: contains element ");
        Vector elements = new Vector();
        elements.add("");
        if (isotopeFactory != null) {
            for (int i = 1; i <= 92; i++) {
                try {
                    Element element = isotopeFactory.getElement(i);
                    elements.add(element.getSymbol());
                } catch (Exception exception) {
                    logger.warn("Could not add element: ", exception.getMessage());
                }
            }
        } else {
            elements.add("C");
            elements.add("N");
            elements.add("O");
            elements.add("S");
            elements.add("P");
            elements.add("Fe");
            elements.add("Pt");
            elements.add("Cr");
        }
        elementFilter = new JComboBox(elements);
        JButton applyFilter = new JButton("Apply");
        applyFilter.addActionListener(new ApplyFilterListener());
        filterPane.add(filterName);
        filterPane.add(elementFilter);
        filterPane.add(applyFilter);
        JPanel aggregatedPane = new JPanel(new BorderLayout());
        aggregatedPane.add(filterPane, BorderLayout.NORTH);
        aggregatedPane.add(scrollableTable, BorderLayout.CENTER);
        aggregatedPane.add(rssItemInfoPanel, BorderLayout.SOUTH);
        channelTable.validate();
        rssChannelInfoPanel = new RSSChannelInfoPanel();
        rssChannelInfoPanel.validate();
        tabbedPane = new JTabbedPane();
        tabbedPane.addTab("Channel Info", null, rssChannelInfoPanel, "Displays information of one RSS channel");
        tabbedPane.addTab("Single Channel", null, contentPane, "Displays the content of one RSS channel");
        tabbedPane.addTab("Aggregated", null, aggregatedPane, "Displays the contents of all RSS channels");
        JSplitPane splitter = new JSplitPane(JSplitPane.HORIZONTAL_SPLIT, treePanel, tabbedPane);
        RSSViewerPanel.add(splitter, BorderLayout.CENTER);
        statusBar = new StatusBar();
        RSSViewerPanel.add(statusBar, BorderLayout.SOUTH);
        RSSViewerPanel.validate();
        return RSSViewerPanel;
    }

    ;

    private void fillAggregatedTable() {
        if (aggregatedContent != null) {
            aggregatedContent.cleanTable();
            Enumeration channels = this.channels.elements();
            while (channels.hasMoreElements()) {
                RSSChannel channel = (RSSChannel) channels.nextElement();
                ChemSequence items = channel.getItems();
                if (items == null) {
                    feedLoadingThread.addFeed(aggregatedContent, channel);
                } else {
                    putChemSequenceIntoTable(aggregatedContent, items);
                }
            }
        } else {
        }
    }

    public JPanel getPluginConfigPanel() {
        return null;
    }

    ;

    public JMenu getMenu() {
        JMenu menu = new JMenu();
        JMenuItem item = new JMenuItem("Add a CMLRSS feed...");
        item.addActionListener(new AddChannelEvent());
        menu.add(item);
        item = new JMenuItem("Force reload of feeds");
        item.addActionListener(new ForceChannelReloadEvent());
        menu.add(item);
        return menu;
    }

    ;

    public void stateChanged(ChemObjectChangeEvent event) {
    }

    class RSSContentModel extends AbstractTableModel {

        private Vector models;

        final String[] columnNames = { "title", "date", "chemFormula", "dimension", "inchi" };

        public RSSContentModel() {
            models = new Vector();
        }

        public void setValueAt(Object value, int row, int column) {
            return;
        }

        public void setValueAt(ChemModel model, int row) {
            if (row > getRowCount()) {
                return;
            }
            models.setElementAt(model, row);
            fireTableCellUpdated(row, 1);
        }

        public int getColumnCount() {
            return columnNames.length;
        }

        public int getRowCount() {
            return models.size();
        }

        public String getColumnName(int col) {
            return columnNames[col];
        }

        public Class getColumnClass(int col) {
            Object o = getValueAt(0, col);
            if (o == null) {
                return (new String()).getClass();
            } else {
                return o.getClass();
            }
        }

        public Object getValueAt(int row, int column) {
            if (row > getRowCount() - 1 || column > getColumnCount() - 1) {
                return "Error";
            }
            ChemModel model = (ChemModel) models.elementAt(row);
            if (model == null) {
                return "";
            }
            AtomContainer container = ChemModelManipulator.getAllInOneContainer(model);
            if (column == 0) {
                return model.getProperty(ChemicalRSSReader.RSS_ITEM_TITLE);
            } else if (column == 1) {
                return model.getProperty(ChemicalRSSReader.RSS_ITEM_DATE);
            } else if (column == 2) {
                container = ChemModelManipulator.getAllInOneContainer(model);
                MFAnalyser analyser = new MFAnalyser(container);
                return analyser.getMolecularFormula();
            } else if (column == 3) {
                int dim = 0;
                if (container.getAtomCount() > 0) {
                    if (GeometryTools.has2DCoordinates(container)) {
                        dim += 2;
                    }
                    if (GeometryTools.has3DCoordinates(container)) {
                        dim += 3;
                    }
                    return dim + "D";
                } else {
                    return "";
                }
            } else if (column == 4) {
                return model.getProperty(ChemicalRSSReader.RSS_ITEM_INCHI);
            }
            return "Error";
        }

        public ChemModel getValueAt(int row) {
            return (ChemModel) models.elementAt(row);
        }

        public boolean isCellEditable(int row, int col) {
            return false;
        }

        public void cleanTable() {
            models.clear();
            fireTableDataChanged();
        }

        private void insertBlankRow(int row) {
            models.addElement(null);
            fireTableRowsInserted(row + 1, row + 1);
        }
    }

    private Vector readProperties(String propertyDir) {
        Vector channels = new Vector();
        File opmlFile = new File(propertyDir + System.getProperty("file.separator") + "rssviewer.opml");
        logger.debug(" User props dir: ", propertyDir);
        logger.debug("OPML file found: ", opmlFile.exists());
        if (opmlFile.exists()) {
            try {
                Reader fis = new FileReader(opmlFile);
                OPMLReader opmlReader = new OPMLReader(fis);
                channels = opmlReader.readRSSChannels();
                opmlReader.close();
            } catch (Exception exception) {
                logger.error("Error while reading rssviewer props: ", exception.getMessage());
                logger.debug(exception);
            }
        }
        return channels;
    }

    private TreeModel createChannelTree() {
        DefaultMutableTreeNode root = new DefaultMutableTreeNode("Channels");
        if (treeModel == null) {
            treeModel = new DefaultTreeModel(root);
        }
        if (channels != null) {
            Enumeration channelEnum = channels.elements();
            while (channelEnum.hasMoreElements()) {
                RSSChannel channel = (RSSChannel) channelEnum.nextElement();
                DefaultMutableTreeNode node = new DefaultMutableTreeNode();
                node.setUserObject(new RSSChannelNode(channel));
                root.add(node);
            }
        }
        treeModel.setRoot(root);
        treeModel.reload();
        return treeModel;
    }

    private ChemSequence parseFeed(RSSChannel channel) throws CDKException, IOException {
        URL url = channel.getURL();
        logger.debug("Should load this RSS now: ", url);
        ChemSequence channelItems = null;
        InputStream is = url.openStream();
        InputStreamReader isReader = new InputStreamReader(is);
        ChemicalRSSReader reader = new ChemicalRSSReader(isReader);
        channelItems = (ChemSequence) reader.read(new ChemSequence());
        return channelItems;
    }

    private void parseChannelIntoTable(RSSContentModel channelContent, RSSChannel channel) {
        ChemSequence channelItems = null;
        try {
            channelItems = parseFeed(channel);
        } catch (CDKException exception) {
            logger.error("Error while reading RSS file: ", exception.getMessage());
            logger.debug(exception);
        } catch (IOException exception) {
            logger.error("IOException while reading RSS file: ", exception.getMessage());
            logger.debug(exception);
        }
        if (channelItems != null) {
            logger.debug("channel items are read!");
            channel.setItems(channelItems);
            String channelTitle = (String) channelItems.getProperty(ChemicalRSSReader.RSS_CHANNEL_TITLE);
            if (channelTitle != null && channelTitle.length() > 0 && channel.getTitle().equals(newFeedTitle)) {
                channel.setTitle(channelTitle);
                createChannelTree();
                feedListChanged = true;
            }
            putChemSequenceIntoTable(channelContent, channelItems);
        }
    }

    private void putChemSequenceIntoTable(RSSContentModel content, ChemSequence channelItems) {
        if (channelItems == null) {
            logger.debug("Channel has not been read yet... skip");
            return;
        }
        ChemModel[] models = channelItems.getChemModels();
        logger.debug("#items = ", models.length);
        int itemsAlreadyInTable = content.getRowCount();
        for (int i = 0; i < models.length; i++) {
            ChemModel model = models[i];
            boolean passedFilter = false;
            Atom[] modelAtoms = ChemModelManipulator.getAllInOneContainer(model).getAtoms();
            String mustContainElement = elementFilter.getSelectedItem().toString();
            if (mustContainElement.length() > 0) {
                for (int j = 0; j < modelAtoms.length; j++) {
                    Atom atom = modelAtoms[j];
                    if (atom.getSymbol().equals(mustContainElement)) {
                        passedFilter = true;
                        j = modelAtoms.length;
                    }
                }
            } else {
                passedFilter = true;
            }
            if (passedFilter) {
                int lastLine = content.getRowCount();
                content.insertBlankRow(lastLine);
                content.setValueAt(models[i], lastLine);
            }
        }
    }

    class RSSChannelNode {

        RSSChannel channel = null;

        RSSChannelNode(RSSChannel channel) {
            this.channel = channel;
        }

        public String toString() {
            String stringRepresentation = channel.getURL().toString();
            if (channel.getTitle().length() > 0) {
                stringRepresentation = channel.getTitle();
            }
            return stringRepresentation;
        }

        public RSSChannel getRSSChannel() {
            return channel;
        }
    }

    class RSSChannelTreeListener implements TreeSelectionListener {

        public void valueChanged(TreeSelectionEvent e) {
            DefaultMutableTreeNode node = (DefaultMutableTreeNode) rssTree.getLastSelectedPathComponent();
            if (node == null) return;
            Object nodeInfo = node.getUserObject();
            if (nodeInfo instanceof RSSChannelNode) {
                RSSChannelNode rssNode = (RSSChannelNode) nodeInfo;
                if (tabbedPane.getSelectedIndex() == 0) {
                    rssChannelInfoPanel.setInfo(rssNode.getRSSChannel());
                } else if (tabbedPane.getSelectedIndex() == 1) {
                    channelContent.cleanTable();
                    putChemSequenceIntoTable(channelContent, rssNode.getRSSChannel().getItems());
                }
            }
        }

        private void transferRSSProperty(ChemModel model, String propertyName, int row, int column) {
            Object property = model.getProperty(propertyName);
            if (property != null) {
                channelContent.setValueAt(property, row, column);
                logger.debug("Transfered data: ", property);
            }
        }
    }

    class RSSChannelItemsTableListener implements ListSelectionListener {

        private SortedTableModel sortedModelContent = null;

        private RSSContentModel modelContent = null;

        private RSSItemInfoPanel infoPanel = null;

        public RSSChannelItemsTableListener(SortedTableModel sortedModelContent, RSSContentModel modelContent, RSSItemInfoPanel infoPanel) {
            this.modelContent = modelContent;
            this.sortedModelContent = sortedModelContent;
            this.infoPanel = infoPanel;
        }

        public void valueChanged(ListSelectionEvent e) {
            if (e.getValueIsAdjusting()) return;
            ListSelectionModel lsm = (ListSelectionModel) e.getSource();
            if (lsm.isSelectionEmpty()) {
            } else {
                int selectedRow = lsm.getMinSelectionIndex();
                ChemModel model = modelContent.getValueAt(sortedModelContent.getSortedIndex(selectedRow));
                infoPanel.setInfo(model);
                Object cmlString = model.getProperty(ChemicalRSSReader.RSS_ITEM_SOURCE);
                if (cmlString != null && APIVersionTester.isBiggerOrEqual("1.8", editBus.getAPIVersion())) {
                    try {
                        logger.debug("CMLString: ", cmlString);
                        editBus.showChemFile(new StringReader((String) cmlString));
                        return;
                    } catch (Exception exception) {
                        logger.error("EditBus error: ", exception.getMessage());
                        logger.debug(exception);
                    }
                }
                ChemSequence sequence = new ChemSequence();
                sequence.addChemModel(model);
                ChemFile file = new ChemFile();
                file.addChemSequence(sequence);
                editBus.showChemFile(file);
            }
        }
    }

    class ApplyFilterListener implements ActionListener {

        public ApplyFilterListener() {
        }

        public void actionPerformed(ActionEvent e) {
            fillAggregatedTable();
        }
    }

    class FeedLoaderThread extends Thread {

        private Vector loadQueue;

        public FeedLoaderThread() {
            super("RSSViewer:FeedLoader");
            loadQueue = new Vector();
        }

        public void run() {
            Thread myThread = Thread.currentThread();
            while (feedLoadingThread == myThread) {
                if (loadQueue.isEmpty()) {
                    try {
                        Thread.sleep(250);
                    } catch (InterruptedException e) {
                    }
                } else {
                    FeedLoadJob job = (FeedLoadJob) loadQueue.get(0);
                    loadFeed(job);
                    loadQueue.remove(job);
                    try {
                        Thread.sleep(50);
                    } catch (InterruptedException e) {
                    }
                }
            }
        }

        public void addFeed(RSSContentModel content, RSSChannel channel) {
            loadQueue.add(new FeedLoadJob(content, channel));
        }

        private void loadFeed(FeedLoadJob job) {
            String message = "Loading " + job.channel.getTitle() + "...";
            logger.info(message);
            statusBar.setStatus(message);
            parseChannelIntoTable(job.content, job.channel);
            statusBar.setStatus("");
        }
    }

    class FeedRefresherThread extends Thread {

        private long previousReloadedTime;

        public FeedRefresherThread() {
            super("RSSViewer:FeedRefresher");
            Calendar timer = Calendar.getInstance();
            previousReloadedTime = timer.getTimeInMillis();
        }

        public void run() {
            Thread myThread = Thread.currentThread();
            while (feedRefresherThread == myThread) {
                Calendar timer = Calendar.getInstance();
                logger.debug("Current ms: " + timer.getTimeInMillis());
                logger.debug("Last loaded: " + previousReloadedTime);
                if (timer.getTimeInMillis() - previousReloadedTime < 300000) {
                    try {
                        logger.debug("Sleeping 30 seconds...");
                        Thread.sleep(30000);
                    } catch (InterruptedException e) {
                    }
                } else {
                    logger.debug("Ok, time to reload feeds...");
                    previousReloadedTime = timer.getTimeInMillis();
                    reloadFeeds();
                }
            }
        }

        private void reloadFeeds() {
            logger.info("Reloading all channels...");
            statusBar.setStatus("Reloading all channels...");
            try {
                Thread.sleep(1000);
            } catch (InterruptedException e) {
            }
            aggregatedContent.cleanTable();
            Enumeration channelEnum = channels.elements();
            while (channelEnum.hasMoreElements()) {
                RSSChannel channel = (RSSChannel) channelEnum.nextElement();
                feedLoadingThread.addFeed(aggregatedContent, channel);
            }
        }
    }

    class FeedLoadJob {

        protected RSSContentModel content;

        protected RSSChannel channel;

        public FeedLoadJob(RSSContentModel content, RSSChannel channel) {
            this.content = content;
            this.channel = channel;
        }
    }

    class StatusBar extends JPanel {

        private JLabel status;

        public StatusBar() {
            super();
            status = new JLabel();
            status.setPreferredSize(new Dimension(640, 20));
            add(status);
        }

        public void setStatus(String text) {
            status.setText(text);
        }

        public String getStatus() {
            return status.getText();
        }
    }

    class ForceChannelReloadEvent extends AbstractAction {

        public void actionPerformed(ActionEvent event) {
            fillAggregatedTable();
        }
    }

    class AddChannelEvent extends AbstractAction {

        public void actionPerformed(ActionEvent event) {
            JDialog dialog = new AddChannelDialog(null, "Add RSS Channel");
            dialog.show();
        }
    }

    class AddChannelDialog extends JDialog {

        private JTextField textArea;

        private Dimension dimension;

        private JLabel textCaption;

        public AddChannelDialog(JFrame fr, String title) {
            super(fr, title, true);
            textArea = new JTextField(40);
            textArea.setEditable(true);
            JPanel textViewer = new JPanel(new BorderLayout());
            textViewer.setAlignmentX(LEFT_ALIGNMENT);
            textViewer.add(textArea, BorderLayout.CENTER);
            JPanel buttonPanel = new JPanel();
            buttonPanel.setLayout(new FlowLayout(FlowLayout.RIGHT));
            JButton add = new JButton("Add");
            add.addActionListener(new ActionListener() {

                public void actionPerformed(ActionEvent e) {
                    AddPressed();
                }
            });
            buttonPanel.add(add);
            JButton cancel = new JButton("Cancel");
            cancel.addActionListener(new ActionListener() {

                public void actionPerformed(ActionEvent e) {
                    CancelPressed();
                }
            });
            buttonPanel.add(cancel);
            getRootPane().setDefaultButton(cancel);
            JPanel container = new JPanel();
            container.setLayout(new BorderLayout());
            textCaption = new JLabel("RSS URL");
            container.add(textCaption, BorderLayout.NORTH);
            container.add(textViewer, BorderLayout.CENTER);
            container.add(buttonPanel, BorderLayout.SOUTH);
            container.validate();
            getContentPane().add(container);
            pack();
        }

        public void CancelPressed() {
            this.setVisible(false);
        }

        public void AddPressed() {
            try {
                URL url = new URL(textArea.getText());
                RSSChannel channel = new RSSChannel(url, newFeedTitle);
                ChemSequence channelContent = parseFeed(channel);
                if (channelContent != null) {
                    channel.setTitle((String) channelContent.getProperty(ChemicalRSSReader.RSS_CHANNEL_TITLE));
                    channel.setItems(channelContent);
                    channels.put(url, channel);
                    createChannelTree();
                    feedListChanged = true;
                    this.hide();
                    textArea.setText("");
                } else {
                    JOptionPane.showMessageDialog(this, "The given URL does not contain an RSS feed!");
                }
            } catch (MalformedURLException exception) {
                JOptionPane.showMessageDialog(this, "The given URL is not correct: " + exception.getMessage());
            } catch (Exception exception) {
                JOptionPane.showMessageDialog(this, "Error while loading URL: " + exception.getMessage());
                logger.error("Error while adding URL: ", exception.getMessage());
                logger.debug(exception);
            }
        }
    }

    class FieldTablePanel extends JPanel {

        protected int rows;

        public FieldTablePanel() {
            setLayout(new GridBagLayout());
            rows = 0;
        }

        protected void addField(String labelText, JTextField text) {
            rows++;
            GridBagConstraints constraints = new GridBagConstraints();
            JLabel label = new JLabel(labelText + ": ", JLabel.TRAILING);
            text.setEditable(false);
            label.setLabelFor(text);
            constraints.gridx = 0;
            constraints.gridy = rows;
            constraints.anchor = GridBagConstraints.LINE_START;
            add(label, constraints);
            constraints.gridx = 1;
            constraints.fill = GridBagConstraints.HORIZONTAL;
            add(text, constraints);
        }

        protected void addArea(String labelText, JEditorPane text) {
            rows++;
            GridBagConstraints constraints = new GridBagConstraints();
            JLabel label = new JLabel(labelText + ": ");
            text.setEditable(false);
            label.setLabelFor(text);
            constraints.gridx = 0;
            constraints.gridwidth = 2;
            constraints.gridy = rows;
            constraints.fill = GridBagConstraints.HORIZONTAL;
            add(label, constraints);
            rows++;
            constraints.gridy = rows;
            JScrollPane editorScrollPane = new JScrollPane(text);
            editorScrollPane.setVerticalScrollBarPolicy(JScrollPane.VERTICAL_SCROLLBAR_ALWAYS);
            editorScrollPane.setPreferredSize(new Dimension(250, 145));
            editorScrollPane.setMinimumSize(new Dimension(10, 10));
            add(editorScrollPane, constraints);
        }
    }

    class RSSChannelInfoPanel extends FieldTablePanel {

        private JTextField channelURL;

        private JTextField channelTitle;

        private JTextField channelWebsite;

        private JTextField channelPublisher;

        private JTextField channelCreator;

        public RSSChannelInfoPanel() {
            setLayout(new GridBagLayout());
            channelURL = new JTextField(30);
            addField("RSS Feed", channelURL);
            channelTitle = new JTextField(30);
            addField("Title", channelTitle);
            channelWebsite = new JTextField(30);
            addField("Website", channelWebsite);
            channelPublisher = new JTextField(30);
            addField("Publisher", channelPublisher);
            channelCreator = new JTextField(30);
            addField("Creator", channelCreator);
            validate();
        }

        public void setInfo(RSSChannel channel) {
            channelURL.setText(channel.getURL().toString());
            ChemSequence sequence = channel.getItems();
            if (sequence != null) {
                channelTitle.setText("" + sequence.getProperty(ChemicalRSSReader.RSS_CHANNEL_TITLE));
                channelWebsite.setText("" + sequence.getProperty(ChemicalRSSReader.RSS_CHANNEL_WEBSITE));
                channelPublisher.setText("" + sequence.getProperty(ChemicalRSSReader.RSS_CHANNEL_PUBLISHER));
                channelCreator.setText("" + sequence.getProperty(ChemicalRSSReader.RSS_CHANNEL_CREATOR));
            } else {
                channelTitle.setText("");
                channelWebsite.setText("");
                channelPublisher.setText("");
                channelCreator.setText("");
            }
        }
    }

    class RSSItemInfoPanel extends FieldTablePanel {

        private JTextField itemTitle;

        private JTextField itemLink;

        private JTextField itemCreator;

        private JTextField inchi;

        private JEditorPane itemDescription;

        private int rows;

        public RSSItemInfoPanel() {
            setLayout(new GridBagLayout());
            itemTitle = new JTextField(30);
            addField("Title", itemTitle);
            itemLink = new JTextField(30);
            addField("Link", itemLink);
            itemCreator = new JTextField(30);
            addField("Creator", itemCreator);
            inchi = new JTextField(30);
            addField("INChI", inchi);
            itemDescription = new JEditorPane();
            addArea("Description", itemDescription);
            validate();
        }

        public void setInfo(ChemModel model) {
            if (model != null) {
                if (model.getProperty(ChemicalRSSReader.RSS_ITEM_TITLE) != null) itemTitle.setText("" + model.getProperty(ChemicalRSSReader.RSS_ITEM_TITLE));
                if (model.getProperty(ChemicalRSSReader.RSS_ITEM_LINK) != null) itemLink.setText("" + model.getProperty(ChemicalRSSReader.RSS_ITEM_LINK));
                if (model.getProperty(ChemicalRSSReader.RSS_ITEM_CREATOR) != null) itemCreator.setText("" + model.getProperty(ChemicalRSSReader.RSS_ITEM_CREATOR));
                if (model.getProperty(ChemicalRSSReader.RSS_ITEM_INCHI) != null) inchi.setText("" + model.getProperty(ChemicalRSSReader.RSS_ITEM_INCHI));
                if (model.getProperty(ChemicalRSSReader.RSS_ITEM_DESCRIPTION) != null) itemDescription.setText("" + model.getProperty(ChemicalRSSReader.RSS_ITEM_DESCRIPTION));
            }
        }
    }
}
