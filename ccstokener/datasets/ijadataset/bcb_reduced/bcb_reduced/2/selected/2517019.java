package gpsxml.gui;

import gpsxml.ArchiveHandler;
import gpsxml.TagTreeNode;
import gpsxml.TagTreeNodeInterface;
import gpsxml.action.EditService;
import gpsxml.action.UpdateServiceGroupListener;
import gpsxml.gui.model.ComboBoxDataModel;
import gpsxml.gui.model.ListDataModel;
import gpsxml.io.DefParameterParser;
import gpsxml.io.InitDataSource;
import gpsxml.io.SAXSearchPositionParser;
import gpsxml.io.SearchPositionConnector;
import gpsxml.xml.Service;
import gpsxml.xml.DataSet;
import gpsxml.xml.ModifierTag;
import gpsxml.xml.NodeCreator;
import gpsxml.xml.TMGFF;
import gpsxml.xml.TagInterface;
import java.awt.Dimension;
import java.awt.Font;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.MouseListener;
import java.io.IOException;
import java.net.URL;
import java.util.ArrayList;
import java.util.LinkedList;
import java.util.List;
import javax.swing.BoxLayout;
import javax.swing.JButton;
import javax.swing.JCheckBoxMenuItem;
import javax.swing.JComboBox;
import javax.swing.JComponent;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JList;
import javax.swing.JMenu;
import javax.swing.JMenuItem;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JPopupMenu;
import javax.swing.JScrollPane;
import javax.swing.JTextArea;
import javax.swing.UIManager;
import javax.swing.border.TitledBorder;
import javax.swing.event.ListSelectionEvent;
import javax.swing.event.ListSelectionListener;
import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.ParserConfigurationException;
import javax.xml.parsers.SAXParser;
import javax.xml.parsers.SAXParserFactory;
import org.apache.log4j.Logger;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;
import org.xml.sax.SAXException;

/**
 *  This is the center panel shown in OmicBrowseManager.  It contains a list
 *  of services and buttons to import and export the XML files defining a service.
 * @author PLAYER, Keith Ralph
 */
public class ServicesPanel extends JPanel {

    static Logger logger = Logger.getLogger(gpsxml.gui.ServicesPanel.class.getName());

    private static final String PROPERTIES = "Properties";

    private static final String DESCRIPTION = "Description: ";

    public static final String NEWLINE = System.getProperty("line.separator");

    private static final String SERVICETYPE = "Service type: ";

    private static final String ANNOTATIONTYPE = "Annotation type: ";

    private static final String SERVICEDEPLOYMENTTYPE = "Deployment type: ";

    private static final String SERVERURL = "/gps/searchPosition?";

    private static final String SERVERALIASURL = "/gps/server?serverType=species";

    private static final String LOCAL = "local";

    private ComboBoxDataModel speciesComboBoxModel = new ComboBoxDataModel();

    private ComboBoxDataModel versionComboBoxModel = new ComboBoxDataModel();

    private ComboBoxDataModel serverComboBoxModel = new ComboBoxDataModel(false);

    private ListDataModel serviceListDataModel = new ListDataModel(null);

    private TagTreeNodeInterface serviceTypeTreeNode;

    private TagTreeNodeInterface datasetTreeNode;

    private String serverName;

    private InitDataSource initDataSource;

    private ServiceListCellRenderer serviceListCellRenderer;

    private AssignServiceFrame assignServiceFrame;

    private EditServiceFrame editServiceFrame;

    private JMenuItem editMenuItem = new JMenuItem("Edit");

    private JMenuItem deleteMenuItem = new JMenuItem("Delete");

    private JMenu groupMenuItem = new JMenu("Assign as a menu item");

    private JMenu groupToUserMenuItem = new JMenu("Assign permission to a user group");

    private JCheckBoxMenuItem accessControlMenuItem = new JCheckBoxMenuItem("Protected");

    private JComboBox speciesComboBox = new JComboBox(speciesComboBoxModel);

    private JComboBox versionComboBox = new JComboBox(versionComboBoxModel);

    private JComboBox serverComboBox = new JComboBox(serverComboBoxModel);

    private JList serviceList;

    private JTextArea descriptionTextArea = new JTextArea();

    private JComponent versionPanel;

    private JComponent speciesPanel;

    private JPopupMenu popupMenu;

    private JButton importButton = new JButton("Import");

    private JButton exportButton = new JButton("Export");

    private ListTransferHandler listTransferHandler = new ListTransferHandler();

    /**
     * Creates a new instance of ServicesPanel
     */
    public ServicesPanel() {
        ComponentBuilder componentBuilder = new ComponentBuilder();
        componentBuilder.initComponents();
        initListeners();
    }

    /**
     *  Sets the species tree node.  The children of this node should contain
     *  the versions for the given species.
     */
    public void setTreeNode(TagTreeNodeInterface treeNode) {
        speciesComboBoxModel.setTreeNode(treeNode);
    }

    /** Actions sent when the user moves the position of a service through the
     *  GUI.
     */
    public void addServiceListTransferListener(UpdateServiceGroupListener listener) {
        listTransferHandler.addTransferListener(listener);
    }

    /** 
     *  Action occurring when species combo box is selected.
     */
    public void addSpeciesComboBoxActionListener(ActionListener listener) {
        speciesComboBox.addActionListener(listener);
    }

    /** 
     *  Action occurring when version combo box is selected.
     */
    public void addVersionComboBoxActionListener(ActionListener listener) {
        getVersionComboBox().addActionListener(listener);
    }

    /** 
     *  Action occurring when server combo box is selected.
     */
    public void addServerComboBoxActionListener(ActionListener listener) {
        serverComboBox.addActionListener(listener);
    }

    /**
     *  Action occurring when user chooses to import a Service from XML.
     */
    public void addImportDatasetButtonListener(ActionListener listener) {
        importButton.addActionListener(listener);
    }

    /**
     *  Action occurring when user chooses to export a Service to XML.  Main use
     * is for RIKEN to create easy to install services for external users i.e so
     *  they don't have to do much typing to add a new service to OmicBrowse.
     */
    public void addExportDatasetButtonListener(ActionListener listener) {
        exportButton.addActionListener(listener);
    }

    public void addGroupMenuItemActionListener(ActionListener listener) {
        groupMenuItem.addActionListener(listener);
    }

    public void addGroupToUserMenuItemActionListener(ActionListener listener) {
        groupToUserMenuItem.addActionListener(listener);
    }

    public void addAccessControlActionListener(ActionListener listener) {
        accessControlMenuItem.addActionListener(listener);
    }

    public void setGroupMenuEnabled(boolean enabled) {
    }

    public void setGroupToUserMenuEnabled(boolean enabled) {
    }

    public void setAccessControlMenuItemProtected(boolean isProtected) {
        accessControlMenuItem.setSelected(isProtected);
        groupToUserMenuItem.setEnabled(isProtected);
    }

    public TagTreeNodeInterface getServiceTypeTreeNode() {
        return serviceTypeTreeNode;
    }

    /**
     *  Returns the currently select species or null if nothing is selected.
     */
    public String getSpecies() {
        if (speciesComboBox.getSelectedItem() == null) {
            return null;
        }
        return speciesComboBox.getSelectedItem().toString();
    }

    /**
     *  Returns the currently select Version or null if nothing is selected.
     */
    public String getVersion() {
        if (getVersionComboBox().getSelectedItem() == null) {
            return null;
        }
        return getVersionComboBox().getSelectedItem().toString();
    }

    /**
     *  Returns the currently select server or null if nothing is selected.
     *  @return a String with the server, if the first item in the server combo
     *  box is the one selected this method returns the LOCAL constant.
     */
    public String getServer() {
        if (serverComboBox.getSelectedItem() == null) {
            return null;
        }
        String server;
        if (serverComboBox.getSelectedIndex() == 0) {
            server = LOCAL;
        } else {
            server = serverComboBox.getSelectedItem().toString();
        }
        return server;
    }

    public JList getServiceList() {
        return serviceList;
    }

    public void setDatasetTreeNode(TagTreeNodeInterface datasetTreeNode) {
        this.datasetTreeNode = datasetTreeNode;
    }

    /**
     *  Returns the service currently selected in the service list or null.  
     *  @return The Service currently selected or null if nothing is selected.
     */
    public Service getSelectedService() {
        if (serviceList == null) {
            return null;
        }
        int index = serviceList.getSelectedIndex();
        if (index != -1) {
            TagTreeNodeInterface treeNode = serviceListDataModel.getTreeNode().getChild(index);
            return (Service) treeNode.getTagInterface();
        }
        return null;
    }

    public void addServiceListListener(ListSelectionListener listener) {
        serviceList.addListSelectionListener(listener);
    }

    public void setListCellRenderer(ServiceListCellRenderer serviceListCellRenderer) {
        serviceList.setCellRenderer(serviceListCellRenderer);
    }

    public void setServiceTypeTreeNode(TagTreeNodeInterface serviceTypeTreeNode) {
        this.serviceTypeTreeNode = serviceTypeTreeNode;
    }

    public void setAssignServiceFrame(AssignServiceFrame assignServiceFrame) {
        this.assignServiceFrame = assignServiceFrame;
    }

    public void setEditServiceFrame(EditServiceFrame editServiceFrame) {
        this.editServiceFrame = editServiceFrame;
    }

    public ListDataModel getServiceListDataModel() {
        return serviceListDataModel;
    }

    public JComponent getVersionPanel() {
        return versionPanel;
    }

    public void setVersionPanel(JComponent versionPanel) {
        this.versionPanel = versionPanel;
    }

    public JComponent getSpeciesPanel() {
        return speciesPanel;
    }

    public JComboBox getVersionComboBox() {
        return versionComboBox;
    }

    public JMenuItem getDeleteMenuItem() {
        return deleteMenuItem;
    }

    public void setSpeciesPanel(JComponent speciesPanel) {
        this.speciesPanel = speciesPanel;
    }

    public void setServerName(String serverName) {
        this.serverName = serverName;
    }

    public void setInitDataSource(InitDataSource initDataSource) {
        this.initDataSource = initDataSource;
    }

    public void addServiceListMouseListener(MouseListener mouseListener) {
        this.serviceList.addMouseListener(mouseListener);
    }

    private void initListeners() {
        serviceList.addListSelectionListener(new SetDescriptionArea());
        editMenuItem.addActionListener(new EditService(this));
        speciesComboBox.addActionListener(new ActionListener() {

            public void actionPerformed(ActionEvent e) {
                serviceList.clearSelection();
                getServiceListDataModel().setTreeNode(null);
                serviceList.setEnabled(false);
                serverComboBox.setEnabled(false);
                Object selectedItem = speciesComboBox.getSelectedItem();
                if (selectedItem instanceof TagTreeNodeInterface) {
                    TagTreeNodeInterface treeNode = (TagTreeNodeInterface) selectedItem;
                    versionComboBoxModel.setTreeNode(treeNode);
                    getVersionComboBox().updateUI();
                    getVersionComboBox().setEnabled(true);
                } else if (selectedItem != null) {
                    getVersionComboBox().setSelectedIndex(getVersionComboBox().getItemCount() - 1);
                }
            }
        });
        getVersionComboBox().addActionListener(new ActionListener() {

            public void actionPerformed(ActionEvent e) {
                if (getVersionComboBox().getSelectedIndex() == -1) {
                    return;
                }
                serviceList.clearSelection();
                getServiceListDataModel().setTreeNode(null);
                serverComboBox.setEnabled(false);
                Object selectedItem = getVersionComboBox().getSelectedItem();
                serviceList.clearSelection();
                if (selectedItem instanceof TagTreeNodeInterface) {
                    serverComboBoxModel.setTreeNode(getServerTreeNode());
                    TagTreeNodeInterface treeNode = (TagTreeNodeInterface) selectedItem;
                    getServiceListDataModel().setTreeNode(treeNode);
                    serviceList.setEnabled(true);
                    serverComboBox.updateUI();
                    serverComboBox.setEnabled(true);
                    serverComboBox.setSelectedIndex(0);
                }
            }
        });
        serverComboBox.addActionListener(new ActionListener() {

            public void actionPerformed(ActionEvent e) {
                Object selectedItem = serverComboBox.getSelectedItem();
                serviceList.clearSelection();
                if (selectedItem != null && selectedItem.toString().equals(LOCAL + " - " + initDataSource.getAliasName())) {
                    TagTreeNodeInterface treeNode = (TagTreeNodeInterface) getVersionComboBox().getSelectedItem();
                    getServiceListDataModel().setTreeNode(treeNode);
                    serviceList.setEnabled(true);
                    serviceList.setTransferHandler(listTransferHandler);
                } else if (selectedItem instanceof TagTreeNodeInterface) {
                    TagTreeNodeInterface treeNode = (TagTreeNodeInterface) selectedItem;
                    getServiceListDataModel().setTreeNode(treeNode);
                    serviceList.setEnabled(true);
                    serviceList.setTransferHandler(null);
                } else {
                    serviceList.clearSelection();
                    getServiceListDataModel().setTreeNode(null);
                    serviceList.setEnabled(false);
                }
            }
        });
        ArchiveHandler.addCloseListener(new ActionListener() {

            public void actionPerformed(ActionEvent e) {
                speciesComboBoxModel.setTreeNode(null);
                versionComboBoxModel.setTreeNode(null);
                serverComboBoxModel.setTreeNode(null);
                serviceListDataModel.setTreeNode(null);
                descriptionTextArea.setText(null);
            }
        });
    }

    /**
     *  Removes the species/version from the dataset with the given name.
     *  @param objectSetName the name of the object set to remove.
     */
    public void removeDatasetsSpeciesVersion(String objectSetName) {
        NodeCreator nodeCreator = new NodeCreator();
        String species = getSpecies();
        String version = getVersion();
        if ((species == null) || (version == null) || species.equals("") || version.equals("")) {
            return;
        }
        TagTreeNodeInterface childTreeNode = datasetTreeNode.getChild(objectSetName);
        if (childTreeNode == null) {
            return;
        }
        DataSet dataset = (DataSet) childTreeNode.getOldTagInterface();
        Node datasetNode = dataset.getNode();
        TagTreeNodeInterface speciesTreeNode = childTreeNode.getChild(species);
        if (speciesTreeNode == null) {
            return;
        }
        speciesTreeNode.removeChild(version);
        Node speciesNode = dataset.getSpeciesNode(datasetNode, species);
        Node versionNode = dataset.getVersionNode(speciesNode, version);
        nodeCreator.removeNode(versionNode);
        if (speciesTreeNode.getChildList().size() == 0) {
            nodeCreator.removeNode(speciesNode);
            childTreeNode.removeChild(species);
        }
    }

    public void setImportDatasetButtonEnabled(boolean enable) {
        importButton.setEnabled(enable);
    }

    public void setExportDatasetButtonEnabled(boolean enable) {
        exportButton.setEnabled(enable);
    }

    private void setUpDescriptionPanel() {
        descriptionTextArea.setFont(new Font("Arial", Font.PLAIN, 11));
        descriptionTextArea.setColumns(30);
        descriptionTextArea.setLineWrap(true);
        descriptionTextArea.setOpaque(false);
        descriptionTextArea.setRows(10);
    }

    public int getSelectedServiceType() {
        Service service = getSelectedService();
        if (service != null) {
            ArrayList<TagTreeNodeInterface> treeNodeList = new ArrayList<TagTreeNodeInterface>();
            TagTreeNodeInterface serviceTypeTreeNode = getServiceTypeTreeNode();
            if (serviceTypeTreeNode != null) {
                boolean found = getServiceType(treeNodeList, serviceTypeTreeNode, service);
                if (found == true) {
                    int size = treeNodeList.size();
                    String description = treeNodeList.get(size - 1).getName();
                    if (description.equals(DefParameterParser.DAS1)) {
                        return DefParameterParser.DAS1INT;
                    } else if (description.equals(DefParameterParser.EXTERNAL)) {
                        return DefParameterParser.EXTERNALINT;
                    } else if (description.equals(DefParameterParser.GENEEXPRESSION)) {
                        return DefParameterParser.GENEEXPRESSIONINT;
                    } else if (description.equals(DefParameterParser.LOCAL)) {
                        return DefParameterParser.LOCALINT;
                    } else if (description.equals(DefParameterParser.LOCUS)) {
                        return DefParameterParser.LOCUSINT;
                    } else if (description.equals(DefParameterParser.TILINGARRAY)) {
                        return DefParameterParser.TILINGARRAYINT;
                    } else if (description.equals(DefParameterParser.TILINGARRAYANDGENE)) {
                        return DefParameterParser.TILINGARRAYANDGENEINT;
                    }
                }
            }
        }
        return -1;
    }

    /** Returns true if a service type was found that matches the java classes
     *  in the given CreateDataSource. 
     *  @param treeNodeList the list of TreeNodes found. The elements are 
     *  ordered as Service, Annotation, ServiceDeployment. Size of the returned 
     *  list is 0 <= size <= 3.
     *  @param treeNode the current TreeNode corresponding to service types defined
     *  in defParam.xml
     *  @param service the java classes defined in this object (and its
     *  ModifierTag if it exists) are used to find a matching service type.
     *  @return boolean value returning true if a service type was found.
     *
     */
    public boolean getServiceType(ArrayList<TagTreeNodeInterface> treeNodeList, TagTreeNodeInterface treeNode, Service service) {
        TagInterface tag = treeNode.getTagInterface();
        if (tag != null) {
            Service currentDataSource = (Service) tag;
            String currentDefinedClass = currentDataSource.getDefinedClass();
            String definedClass = service.getDefinedClass();
            if (currentDefinedClass != null && definedClass != null) {
                if (currentDefinedClass.equals(definedClass)) {
                    ModifierTag currentModifierTag = currentDataSource.getModifierTag();
                    ModifierTag modifierTag = service.getModifierTag();
                    if (currentModifierTag == null && modifierTag == null) {
                        treeNodeList.add(0, treeNode);
                        return true;
                    }
                    if (currentModifierTag != null && modifierTag != null) {
                        if (currentModifierTag.getModifierClass().equals(modifierTag.getModifierClass())) {
                            treeNodeList.add(0, treeNode);
                            return true;
                        }
                    }
                }
            }
        }
        List<TagTreeNodeInterface> childList = treeNode.getChildList();
        for (TagTreeNodeInterface childTreeNode : childList) {
            boolean found = getServiceType(treeNodeList, childTreeNode, service);
            if (found == true) {
                if (!treeNode.getName().equals("root")) {
                    treeNodeList.add(0, treeNode);
                }
                return true;
            }
        }
        return false;
    }

    private TagTreeNodeInterface getServerTreeNode() {
        String species = speciesComboBox.getSelectedItem().toString();
        String version = getVersionComboBox().getSelectedItem().toString();
        SearchPositionConnector searchPositionConnector = new SearchPositionConnector();
        TagTreeNode root = new TagTreeNode("root");
        getChildNode(root, LOCAL + " - " + initDataSource.getAliasName());
        for (String server : initDataSource.getParents()) {
            String serverURL = server + SERVERURL;
            URL url = searchPositionConnector.getXMLStream(serverURL, species, version);
            SAXSearchPositionParser handler;
            SAXParserFactory parserFactory = SAXParserFactory.newInstance();
            handler = new SAXSearchPositionParser();
            String parentAlias = null;
            try {
                SAXParser saxParser = parserFactory.newSAXParser();
                saxParser.parse(url.openStream(), handler);
                LinkedList<TMGFF> tagList = handler.getInformation();
                for (TMGFF tag : tagList) {
                    String serverName = tag.getServer();
                    if (serverName == null) {
                        if (parentAlias == null) {
                            parentAlias = getParentAlias(server);
                        }
                        serverName = parentAlias;
                    }
                    getChildNode(root, serverName).addChild(new TagTreeNode(tag.getSource()));
                }
            } catch (SAXException ex) {
                logger.error("Error reading xml from SearchPosition servlet url:" + url, ex);
            } catch (ParserConfigurationException ex) {
                logger.error("Error reading xml from SearchPosition servlet url:" + url, ex);
            } catch (IOException ex) {
                JOptionPane.showMessageDialog(ServicesPanel.this, "Could not connect to:" + serverURL, "Server connection error(IOException)", JOptionPane.ERROR_MESSAGE);
            } catch (NullPointerException ex) {
                JOptionPane.showMessageDialog(ServicesPanel.this, "Could not connect to:" + serverURL, "Server connection error", JOptionPane.ERROR_MESSAGE);
            }
        }
        return root;
    }

    private TagTreeNodeInterface getChildNode(TagTreeNodeInterface parent, String key) {
        TagTreeNodeInterface child = parent.getChild(key);
        if (child == null) {
            child = new TagTreeNode(key);
            parent.addChild(child);
        }
        return child;
    }

    private String getParentAlias(String url) {
        String serverAlias = null;
        try {
            DocumentBuilder documentBuilder = DocumentBuilderFactory.newInstance().newDocumentBuilder();
            Document serverDoc = documentBuilder.parse(url + SERVERALIASURL);
            NodeList parentServer = serverDoc.getElementsByTagName("LocalServer");
            if (parentServer.getLength() > 0) serverAlias = ((Element) parentServer.item(0)).getAttribute("name");
        } catch (ParserConfigurationException e) {
            logger.warn("Parser configuration url:" + url, e);
        } catch (SAXException e) {
            logger.warn(" url:" + url, e);
        } catch (IOException e) {
            logger.warn(" url:" + url, e);
        }
        if (serverAlias == null) serverAlias = "???";
        return serverAlias;
    }

    public void updateServerComboBox() {
        if (getVersionComboBox().isEnabled()) {
            getVersionComboBox().setSelectedIndex(getVersionComboBox().getSelectedIndex());
        }
    }

    /**
     * The logic for creating ServicesPanel GUI components should be 
     *  in one real class with listener logic e.t.c. in a parent controller class.
     */
    private class ComponentBuilder {

        private static final String LISTTITLE = "Datasets assigned as service on the axis";

        private static final int LARGECOMBOBOXWIDTH = 160;

        private static final int SMALLCOMBOBOXWIDTH = 80;

        private static final int COMBOBOXHEIGHT = 20;

        private static final int BUTTONWIDTH = 80;

        private static final int BUTTONHEIGHT = 20;

        private void initComponents() {
            ComponentBuilder componentBuilder = new ComponentBuilder();
            JComponent comboBoxPanel = getComboBoxComponent();
            JComponent buttonPanel = getButtonsComponent();
            JComponent objectsPanel = getServiceListComponent();
            JComponent topPanel = BoxPanelBuilder.getRigidPanel(BoxLayout.Y_AXIS, 5, 10, 0, comboBoxPanel, buttonPanel, objectsPanel);
            setUpDescriptionPanel();
            JScrollPane scrollPane = new JScrollPane();
            scrollPane.setViewportView(descriptionTextArea);
            scrollPane.setMaximumSize(new Dimension(220, 150));
            scrollPane.setMinimumSize(new Dimension(160, 150));
            scrollPane.setBorder(null);
            getVersionComboBox().setEnabled(false);
            serverComboBox.setEnabled(false);
            importButton.setEnabled(false);
            exportButton.setEnabled(false);
            BoxPanelBuilder.getRigidPanel(ServicesPanel.this, BoxLayout.Y_AXIS, 0, 5, 5, topPanel, scrollPane);
            PanelUtilities.setTitledBorder(ServicesPanel.this, LISTTITLE, TitledBorder.LEFT);
            popupMenu = new JPopupMenu();
            getPopupMenu().add(groupMenuItem);
            getPopupMenu().add(editMenuItem);
            getPopupMenu().add(getDeleteMenuItem());
            getPopupMenu().addSeparator();
            getPopupMenu().add(accessControlMenuItem);
            getPopupMenu().add(groupToUserMenuItem);
        }

        public JComponent getButtonsComponent() {
            importButton.setPreferredSize(new Dimension(BUTTONWIDTH, BUTTONHEIGHT));
            exportButton.setPreferredSize(new Dimension(BUTTONWIDTH, BUTTONHEIGHT));
            JPanel panel = BoxPanelBuilder.getPlainGluePanel(BoxLayout.X_AXIS, importButton, exportButton);
            return panel;
        }

        private JComponent getComboBoxComponent() {
            JLabel speciesLabel = new JLabel("species");
            JLabel versionLabel = new JLabel("version");
            speciesLabel.setFont(new Font("Arial", Font.PLAIN, 12));
            versionLabel.setFont(new Font("Arial", Font.PLAIN, 12));
            JLabel serverLabel = new JLabel("Server");
            speciesComboBox.setMaximumSize(new Dimension(SMALLCOMBOBOXWIDTH, COMBOBOXHEIGHT));
            speciesComboBox.setPreferredSize(new Dimension(SMALLCOMBOBOXWIDTH, COMBOBOXHEIGHT));
            getVersionComboBox().setMaximumSize(new Dimension(SMALLCOMBOBOXWIDTH, COMBOBOXHEIGHT));
            getVersionComboBox().setPreferredSize(new Dimension(SMALLCOMBOBOXWIDTH, COMBOBOXHEIGHT));
            serverComboBox.setMaximumSize(new Dimension(LARGECOMBOBOXWIDTH, COMBOBOXHEIGHT));
            serverComboBox.setPreferredSize(new Dimension(LARGECOMBOBOXWIDTH, COMBOBOXHEIGHT));
            speciesPanel = BoxPanelBuilder.getPlainRigidPanel(BoxLayout.X_AXIS, new Dimension(5, 0), speciesLabel, speciesComboBox);
            versionPanel = BoxPanelBuilder.getPlainRigidPanel(BoxLayout.X_AXIS, new Dimension(5, 0), versionLabel, getVersionComboBox());
            JComponent serverPanel = BoxPanelBuilder.getPlainGluePanel(BoxLayout.X_AXIS, serverLabel, serverComboBox);
            return serverPanel;
        }

        /** Return a component for the list of species.
         *  @return a JComponent with the list of species.
         */
        private JComponent getServiceListComponent() {
            serviceList = new JList(serviceListDataModel);
            JComponent objectsPanel = PanelUtilities.getListComponent(serviceList);
            return objectsPanel;
        }
    }

    private class SetDescriptionArea implements ListSelectionListener {

        public void valueChanged(ListSelectionEvent e) {
            int index = serviceList.getSelectedIndex();
            if (index == -1) {
                descriptionTextArea.setText("");
                return;
            }
            TagTreeNodeInterface treeNode = getServiceListDataModel().getTreeNode().getChild(index);
            TagInterface nodeAccessor = treeNode.getTagInterface();
            if (nodeAccessor != null) {
                Service service = (Service) nodeAccessor;
                setDescriptionTextArea(service);
            }
        }

        private void setDescriptionTextArea(Service service) {
            StringBuffer stringBuffer = new StringBuffer();
            String description = service.getDescription();
            if (description != null && !description.equals("")) {
                stringBuffer.append(DESCRIPTION + service.getDescription());
                stringBuffer.append(NEWLINE);
            }
            if (getServiceTypeTreeNode() != null) {
                ArrayList<TagTreeNodeInterface> treeNodeList = new ArrayList<TagTreeNodeInterface>();
                boolean found = getServiceType(treeNodeList, getServiceTypeTreeNode(), service);
                if (found == true) {
                    stringBuffer.append(SERVICETYPE + treeNodeList.get(0).getName());
                    stringBuffer.append(NEWLINE);
                    int size = treeNodeList.size();
                    if (size == 2 || size == 3) {
                        stringBuffer.append(ANNOTATIONTYPE + treeNodeList.get(1).getName());
                        stringBuffer.append(NEWLINE);
                    }
                    if (size == 3) {
                        stringBuffer.append(SERVICEDEPLOYMENTTYPE + treeNodeList.get(2).getName());
                        stringBuffer.append(NEWLINE);
                    }
                }
            }
            ModifierTag modifierTag = service.getModifierTag();
            if (modifierTag != null) {
                stringBuffer.append(NEWLINE);
                stringBuffer.append(modifierTag.toString());
            }
            String propertyList = service.getPropertyList().getPropertyList();
            if (propertyList != null && !propertyList.equals("")) {
                stringBuffer.append(NEWLINE);
                stringBuffer.append(PROPERTIES);
                stringBuffer.append(NEWLINE);
                stringBuffer.append(propertyList);
            }
            descriptionTextArea.setText(stringBuffer.toString());
        }
    }

    public void clearComboBox() {
        try {
            serverComboBox.setEnabled(false);
            versionComboBox.setEnabled(false);
            speciesComboBox.setSelectedIndex(-1);
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    public JMenu getGroupMenuItem() {
        return groupMenuItem;
    }

    public JMenu getGroupToUserMenuItem() {
        return groupToUserMenuItem;
    }

    public void createAndShowGUI() {
        try {
            UIManager.setLookAndFeel(UIManager.getSystemLookAndFeelClassName());
        } catch (Exception e) {
        }
        JFrame frame = new JFrame();
        frame.setTitle("ViewServices");
        frame.add(this);
        frame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        frame.pack();
        frame.setVisible(true);
    }

    public static void main(String[] args) {
        final ServicesPanel servicesPanel = new ServicesPanel();
        javax.swing.SwingUtilities.invokeLater(new Runnable() {

            public void run() {
                servicesPanel.createAndShowGUI();
            }
        });
    }

    public EditServiceFrame getEditServiceFrame() {
        return editServiceFrame;
    }

    public JPopupMenu getPopupMenu() {
        return popupMenu;
    }
}
