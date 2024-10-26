package org.nodes4knime.molconvert;

import org.knime.core.data.DataTableSpec;
import org.knime.core.node.InvalidSettingsException;
import org.knime.core.node.NodeLogger;
import org.knime.core.node.NodeSettingsRO;
import org.knime.core.node.NodeSettingsWO;
import org.knime.core.node.NotConfigurableException;
import org.knime.core.node.defaultnodesettings.DefaultNodeSettingsPane;
import org.knime.core.node.defaultnodesettings.SettingsModelBoolean;
import org.knime.core.node.defaultnodesettings.SettingsModelFilterString;
import org.knime.core.node.defaultnodesettings.SettingsModelInteger;
import org.knime.core.node.defaultnodesettings.SettingsModelString;
import java.awt.*;
import java.awt.event.*;
import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.util.ArrayList;
import javax.swing.*;

/**
 * This is the model implementation of MolConvert to show
 * proof of concept for remote job submission.
 *
 * @author M Simmons, The Edge
 * Contact: Andrew Lemon, The Edge, andrew@edgesoftwareconsultancy.com
 */
public class MolConvertInNodeDialog extends DefaultNodeSettingsPane {

    /**
	 * New pane for configuring MolConvert node dialog. This is just a
	 * suggestion to demonstrate possible default dialog components.
	 */
    JPanel m_dialogPanelSettings = null;

    JPanel m_dialogPanelServer = null;

    JComboBox m_outputFormats = null;

    JComboBox m_serverPriority = null;

    JScrollPane m_scrollPane = null;

    JTextField m_serverAddress = null;

    JTextField m_serverPort = null;

    JTextField m_serverNFSPath = null;

    JTextField m_serverTempFolder = null;

    JCheckBox m_serverStatus = null;

    JCheckBox m_checkBox_F = null;

    JCheckBox m_checkBox_n = null;

    final String TEST_BUTTON_NAME = "TestButton";

    final String ENABLE_CALCS_CHECKBOX_NAME = "EnableCalcs";

    ButtonGroup m_radioOptionsGroup2D3D = null;

    JRadioButton m_radio_2D = null;

    JRadioButton m_radio_3D = null;

    JCheckBox m_checkbox_removeH = null;

    JCheckBox m_checkbox_2D3D = null;

    String[] listFormatNames = { "mol", "sdf", "smi", "mol2", "pdb" };

    String[] listServerPriority = { "Normal", "Highest", "Lowest" };

    private static final String DEFAULT_SERVER_PRIORITY = "normal";

    private static final String EMPTY_SERVER_ADDDRESS = "http://";

    private static final NodeLogger logger = NodeLogger.getLogger(MolConvertInNodeDialog.class);

    private JPanel createCoordinateCalcRadioPanel() {
        JPanel buttonPanel = new JPanel();
        buttonPanel.setLayout(new BoxLayout(buttonPanel, BoxLayout.X_AXIS));
        buttonPanel.setBorder(BorderFactory.createTitledBorder(BorderFactory.createEtchedBorder(), "Coordinate Calculations"));
        m_checkbox_2D3D = new JCheckBox("Enable coordinate calcs");
        m_checkbox_2D3D.setName(ENABLE_CALCS_CHECKBOX_NAME);
        m_checkbox_2D3D.addActionListener(new MyActionListener());
        buttonPanel.add(m_checkbox_2D3D);
        m_radio_2D = new JRadioButton("2D");
        m_radio_2D.setToolTipText("Calculate 2D Coordinates.");
        buttonPanel.add(m_radio_2D);
        m_radio_3D = new JRadioButton("3D");
        m_radio_3D.setToolTipText("Calculate 3D Coordinates.");
        m_radio_3D.setSelected(true);
        buttonPanel.add(m_radio_3D);
        m_radioOptionsGroup2D3D = new ButtonGroup();
        m_radioOptionsGroup2D3D.add(m_radio_2D);
        m_radioOptionsGroup2D3D.add(m_radio_3D);
        return buttonPanel;
    }

    private JPanel createOptionsPanel1() {
        JPanel panel = new JPanel();
        panel.setLayout(new GridBagLayout());
        GridBagConstraints gbc = new GridBagConstraints();
        gbc.fill = GridBagConstraints.BOTH;
        gbc.gridx = 0;
        gbc.gridy = 0;
        panel.setBorder(BorderFactory.createTitledBorder(BorderFactory.createEtchedBorder(), "Options"));
        m_checkBox_F = new JCheckBox("Remove Small Fragments");
        m_checkBox_F.setToolTipText("Removed small fragments, leaving largest.");
        gbc.weighty = 0.2;
        gbc.gridx = 0;
        gbc.gridy = 3;
        panel.add(m_checkBox_F, gbc);
        JPanel radioPanelCoordinate = createCoordinateCalcRadioPanel();
        gbc.weighty = 0.2;
        gbc.gridx = 0;
        gbc.gridy = 5;
        panel.add(radioPanelCoordinate, gbc);
        return panel;
    }

    private JPanel createParametersPanel() {
        JPanel panel = new JPanel();
        panel.setLayout(new GridBagLayout());
        panel.setBorder(BorderFactory.createTitledBorder(BorderFactory.createEtchedBorder(), "Output Format"));
        GridBagConstraints gbc = new GridBagConstraints();
        m_outputFormats = new JComboBox(listFormatNames);
        m_scrollPane = new JScrollPane(m_outputFormats);
        gbc.fill = GridBagConstraints.LINE_START;
        gbc.weighty = 0.0;
        gbc.gridx = 0;
        gbc.gridy = 0;
        panel.add(m_scrollPane, gbc);
        return panel;
    }

    class MyActionListener implements ActionListener {

        public void actionPerformed(ActionEvent e) {
            if (e.getSource() instanceof JButton) {
                JButton button = (JButton) e.getSource();
                if (button.getName().equalsIgnoreCase(TEST_BUTTON_NAME)) {
                    boolean status = serverOK(m_serverAddress.getText(), m_serverPort.getText());
                    if (m_serverStatus != null) {
                        m_serverStatus.setSelected(status);
                    }
                }
            }
            if (e.getSource() instanceof JCheckBox) {
                JCheckBox checkBox = (JCheckBox) e.getSource();
                if (checkBox.getName().equalsIgnoreCase(ENABLE_CALCS_CHECKBOX_NAME)) {
                    m_radio_2D.setEnabled(m_checkbox_2D3D.isSelected());
                    m_radio_3D.setEnabled(m_checkbox_2D3D.isSelected());
                }
            }
        }
    }

    private JPanel createServerPanel() {
        JPanel panel = new JPanel();
        panel.setLayout(new GridBagLayout());
        GridBagConstraints gbc = new GridBagConstraints();
        gbc.fill = GridBagConstraints.BOTH;
        panel.setBorder(BorderFactory.createTitledBorder(BorderFactory.createEtchedBorder(), "Compute Server"));
        JLabel serverAddressLabel = new JLabel("Server Address:");
        gbc.gridx = 0;
        gbc.gridy = 0;
        gbc.weighty = 0.2;
        panel.add(serverAddressLabel, gbc);
        m_serverAddress = new JTextField(MolConvertInNodeDialog.EMPTY_SERVER_ADDDRESS);
        m_serverAddress.setPreferredSize(new Dimension(130, 20));
        m_serverAddress.setToolTipText("enter a valid url");
        gbc.gridx = 1;
        gbc.gridy = 0;
        gbc.insets = new Insets(10, 10, 10, 10);
        gbc.weighty = 0.2;
        panel.add(m_serverAddress, gbc);
        JLabel serverPortLabel = new JLabel("Server Port:");
        gbc.gridx = 0;
        gbc.gridy = 1;
        gbc.weighty = 0.2;
        panel.add(serverPortLabel, gbc);
        m_serverPort = new JTextField("80");
        m_serverPort.setPreferredSize(new Dimension(10, 20));
        m_serverPort.setToolTipText("enter a valid server port");
        gbc.gridx = 1;
        gbc.gridy = 1;
        gbc.weighty = 0.2;
        panel.add(m_serverPort, gbc);
        JLabel serverNFSPathLabel = new JLabel("Server share Path:");
        gbc.gridx = 0;
        gbc.gridy = 2;
        gbc.weighty = 0.2;
        panel.add(serverNFSPathLabel, gbc);
        m_serverNFSPath = new JTextField(MolConvertInNodeModel.DEFAULT_SERVER_NFS_PATH);
        m_serverNFSPath.setPreferredSize(new Dimension(10, 20));
        m_serverNFSPath.setToolTipText("enter server nfs share path");
        gbc.gridx = 1;
        gbc.gridy = 2;
        gbc.weighty = 0.2;
        panel.add(m_serverNFSPath, gbc);
        JLabel serverTempFolderLabel = new JLabel("Server temp folder name:");
        gbc.gridx = 0;
        gbc.gridy = 3;
        gbc.weighty = 0.2;
        panel.add(serverTempFolderLabel, gbc);
        m_serverTempFolder = new JTextField(MolConvertInNodeModel.DEFAULT_SERVER_TEMP_FOLDER);
        m_serverTempFolder.setPreferredSize(new Dimension(10, 20));
        m_serverTempFolder.setToolTipText("enter server temp folder name (not its path)");
        gbc.gridx = 1;
        gbc.gridy = 3;
        gbc.weighty = 0.2;
        panel.add(m_serverTempFolder, gbc);
        JButton serverTestButton = new JButton("Test Server Connection");
        serverTestButton.setToolTipText("use to test server is alive");
        serverTestButton.setName(TEST_BUTTON_NAME);
        serverTestButton.addActionListener(new MyActionListener());
        gbc.fill = GridBagConstraints.CENTER;
        gbc.gridx = 0;
        gbc.gridy = 4;
        gbc.weighty = 0.2;
        gbc.insets = new Insets(10, 10, 10, 10);
        panel.add(serverTestButton, gbc);
        gbc.gridx = 1;
        gbc.gridy = 4;
        gbc.weighty = 0.2;
        m_serverStatus = new JCheckBox("Alive");
        m_serverStatus.setSelected(false);
        m_serverStatus.setEnabled(false);
        panel.add(m_serverStatus, gbc);
        JLabel serverPriorityLabel = new JLabel("Server Priority");
        gbc.gridx = 0;
        gbc.gridy = 5;
        gbc.weighty = 0.2;
        gbc.insets = new Insets(10, 10, 10, 10);
        panel.add(serverPriorityLabel, gbc);
        m_serverPriority = new JComboBox(listServerPriority);
        m_serverPriority.setToolTipText("sets the job priority on the server");
        JScrollPane serverPriorityScrollPane = new JScrollPane(m_serverPriority);
        gbc.gridx = 1;
        gbc.gridy = 5;
        gbc.weighty = 0.2;
        gbc.insets = new Insets(10, 10, 10, 10);
        panel.add(serverPriorityScrollPane, gbc);
        return panel;
    }

    private boolean serverOK(String serverAddress, String serverPort) {
        boolean status = false;
        String serverString = serverAddress + ":" + serverPort + MolConvertInNodeModel.SERVER_WSDL_PATH;
        System.out.println("connecting to " + serverString + "...");
        try {
            java.net.URL url = new java.net.URL(serverString);
            try {
                java.net.HttpURLConnection connection = (java.net.HttpURLConnection) url.openConnection();
                status = readContents(connection);
                if (status) {
                    JOptionPane.showMessageDialog(this.getPanel(), "Connection to Server is OK");
                }
            } catch (Exception connEx) {
                JOptionPane.showMessageDialog(this.getPanel(), connEx.getMessage());
                logger.error(connEx.getMessage());
            }
        } catch (java.net.MalformedURLException urlEx) {
            JOptionPane.showMessageDialog(this.getPanel(), urlEx.getMessage());
            logger.error(urlEx.getMessage());
        }
        return status;
    }

    private static boolean readContents(java.net.HttpURLConnection connection) throws Exception {
        BufferedReader in = null;
        boolean status = false;
        in = new BufferedReader(new InputStreamReader(connection.getInputStream()));
        String inputLine;
        while ((inputLine = in.readLine()) != null) {
            System.out.println(inputLine);
        }
        status = true;
        if (in != null) {
            in.close();
        }
        return status;
    }

    private static String SELECTFIELDS_CAPTION = "Select fields to use:";

    private static String LIST1_NAME = "availableList";

    private static String LIST2_NAME = "chosenList";

    private static String ADDSINGLEBUTTON_NAME = "addSingleButton";

    private static String ADDALLBUTTON_NAME = "addAllButton";

    private static String REMOVESINGLEBUTTON_NAME = "removeSingleButton";

    private static String REMOVEALLBUTTON_NAME = "removeALLButton";

    private static String FIELDPICKERPANEL_NAME = "FieldPickerPanel";

    private static String ADDSINGLE_CAPTION = ">";

    private static String ADDALL_CAPTION = ">>";

    private static String REMOVESINGLE_CAPTION = "<";

    private static String REMOVEALL_CAPTION = "<<";

    private JList m_list1;

    private JList m_list2;

    private DefaultListModel m_listModel1;

    private DefaultListModel m_listModel2;

    /**
	 * listen for mouse events
	 */
    class NodeDialogMouseListener implements MouseListener {

        public void mouseReleased(MouseEvent e) {
        }

        public void mouseEntered(MouseEvent e) {
        }

        public void mouseExited(MouseEvent e) {
        }

        public void mouseClicked(MouseEvent e) {
            try {
                Object aControl = e.getSource();
                if (aControl instanceof JButton) {
                    JButton button = (JButton) e.getSource();
                    if (button.getName() == ADDSINGLEBUTTON_NAME) {
                        int index = m_list1.getSelectedIndex();
                        if (index > -1) {
                            String elem = (String) m_listModel1.getElementAt(index);
                            m_listModel1.removeElementAt(index);
                            m_listModel2.addElement(elem);
                        }
                    }
                    if (button.getName() == REMOVESINGLEBUTTON_NAME) {
                        int index = m_list2.getSelectedIndex();
                        if (index > -1) {
                            String elem = (String) m_listModel2.getElementAt(index);
                            m_listModel2.removeElementAt(index);
                            m_listModel1.addElement(elem);
                        }
                    }
                    if (button.getName() == ADDALLBUTTON_NAME) {
                        for (int index = 0; index < m_listModel1.getSize(); index++) {
                            String elem = (String) m_listModel1.getElementAt(index);
                            m_listModel2.addElement(elem);
                        }
                        m_listModel1.removeAllElements();
                    }
                    if (button.getName() == REMOVEALLBUTTON_NAME) {
                        for (int index = 0; index < m_listModel2.getSize(); index++) {
                            String elem = (String) m_listModel2.getElementAt(index);
                            m_listModel1.addElement(elem);
                        }
                        m_listModel2.removeAllElements();
                    }
                }
                if (aControl instanceof JList) {
                    JList list = (JList) aControl;
                    if (e.getClickCount() == 2) {
                        int index = list.locationToIndex(e.getPoint());
                        if (list.getName() == LIST1_NAME) {
                            String elem = (String) m_listModel1.getElementAt(index);
                            m_listModel1.remove(index);
                            m_listModel2.addElement(elem);
                            if (index < m_listModel1.getSize()) {
                                list.setSelectedIndex(index);
                            } else {
                                list.setSelectedIndex(m_listModel1.getSize() - 1);
                            }
                        }
                        if (list.getName() == LIST2_NAME) {
                            String elem = (String) m_listModel2.getElementAt(index);
                            m_listModel2.remove(index);
                            m_listModel1.addElement(elem);
                        }
                    }
                }
            } catch (Exception ex) {
                logger.error("NodeDialogMouseListener error", ex);
                ex.printStackTrace();
            }
        }

        public void mousePressed(MouseEvent e) {
        }
    }

    /**
	 * create the lowest panel comprising the field picker listboxes etc
	 */
    private JPanel createBottomPanel() {
        JPanel panel = new JPanel();
        panel.setBorder(BorderFactory.createTitledBorder(BorderFactory.createEtchedBorder(), SELECTFIELDS_CAPTION));
        panel.setName(FIELDPICKERPANEL_NAME);
        panel.setLayout(new BoxLayout(panel, BoxLayout.LINE_AXIS));
        DefaultListModel listModel1 = new DefaultListModel();
        m_list1 = new JList(listModel1);
        m_listModel1 = (DefaultListModel) m_list1.getModel();
        JScrollPane scroll1 = new JScrollPane(m_list1);
        m_list1.setVisibleRowCount(10);
        m_list1.setFixedCellHeight(20);
        m_list1.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);
        m_list1.setBorder(BorderFactory.createTitledBorder(BorderFactory.createEtchedBorder(), "Select"));
        DefaultListModel listModel2 = new DefaultListModel();
        m_list2 = new JList(listModel2);
        m_listModel2 = (DefaultListModel) m_list2.getModel();
        JScrollPane scroll2 = new JScrollPane(m_list2);
        m_list2.setVisibleRowCount(10);
        m_list2.setFixedCellHeight(20);
        m_list2.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);
        m_list2.setBorder(BorderFactory.createTitledBorder(BorderFactory.createEtchedBorder(), "Deselect"));
        panel.add(scroll1);
        panel.add(Box.createRigidArea(new Dimension(5, 0)));
        JPanel buttonPanel = createButtonPanel();
        panel.add(buttonPanel);
        panel.add(Box.createRigidArea(new Dimension(5, 0)));
        panel.add(scroll2);
        panel.add(Box.createRigidArea(new Dimension(10, 0)));
        panel.add(buttonPanel);
        panel.add(Box.createRigidArea(new Dimension(5, 0)));
        panel.add(scroll2);
        panel.add(Box.createRigidArea(new Dimension(10, 0)));
        panel.setOpaque(true);
        return panel;
    }

    /**
	 * creates the panel on which we create the field picking buttons
	 */
    private JPanel createButtonPanel() {
        JPanel buttonsPanel = new JPanel();
        buttonsPanel.setLayout(new GridLayout(4, 1, 5, 5));
        JButton addSingleButton = new JButton(ADDSINGLE_CAPTION);
        addSingleButton.setName(ADDSINGLEBUTTON_NAME);
        addSingleButton.addMouseListener(new NodeDialogMouseListener());
        buttonsPanel.add(addSingleButton);
        JButton addAllButton = new JButton(ADDALL_CAPTION);
        addAllButton.setName(ADDALLBUTTON_NAME);
        addAllButton.addMouseListener(new NodeDialogMouseListener());
        buttonsPanel.add(addAllButton);
        JButton removeSingleButton = new JButton(REMOVESINGLE_CAPTION);
        removeSingleButton.setName(REMOVESINGLEBUTTON_NAME);
        removeSingleButton.addMouseListener(new NodeDialogMouseListener());
        buttonsPanel.add(removeSingleButton);
        JButton removeAllButton = new JButton(REMOVEALL_CAPTION);
        removeAllButton.setName(REMOVEALLBUTTON_NAME);
        removeAllButton.addMouseListener(new NodeDialogMouseListener());
        buttonsPanel.add(removeAllButton);
        return buttonsPanel;
    }

    /**
	 * clears the underlying listmodels that hold the list data
	 */
    private void clearListModels() {
        if (m_listModel1 != null) {
            m_listModel1.clear();
        }
        if (m_listModel2 != null) {
            m_listModel2.clear();
        }
    }

    /**
	 * load the file's field names into the first listbox
	 */
    private void loadFieldsIntoListModel(String[] fields) {
        if ((fields == null) || (fields.length == 0)) {
            return;
        }
        try {
            clearListModels();
            if (fields != null) {
                for (int elem = 0; elem < fields.length; elem++) {
                    m_listModel2.addElement(fields[elem]);
                }
            }
        } catch (Exception ex) {
            logger.error("loadFields error", ex);
            ex.printStackTrace();
        }
    }

    @SuppressWarnings("unchecked")
    protected MolConvertInNodeDialog() {
        super();
        super.removeTab("Options");
        JPanel bottomPanel = createBottomPanel();
        int prefHeight = 250;
        bottomPanel.setMinimumSize(new Dimension(300, prefHeight));
        bottomPanel.setMaximumSize(new Dimension(750, prefHeight));
        bottomPanel.setPreferredSize(new Dimension(500, prefHeight));
        super.addTab("Input Columns", bottomPanel);
        m_dialogPanelSettings = new JPanel();
        m_dialogPanelSettings.setLayout(new GridBagLayout());
        GridBagConstraints gc = new GridBagConstraints();
        gc.gridx = 0;
        gc.gridy = 0;
        gc.fill = GridBagConstraints.HORIZONTAL;
        JPanel paramsPanel = createParametersPanel();
        m_dialogPanelSettings.add(paramsPanel, gc);
        JPanel panel = createOptionsPanel1();
        gc.fill = GridBagConstraints.NONE;
        gc.gridy = 1;
        m_dialogPanelSettings.add(panel, gc);
        super.addTab("MolConvert Settings", m_dialogPanelSettings);
        m_dialogPanelServer = new JPanel();
        m_dialogPanelServer.setLayout(new GridBagLayout());
        gc = new GridBagConstraints();
        gc.gridx = 0;
        gc.gridy = 0;
        gc.fill = GridBagConstraints.HORIZONTAL;
        JPanel serverPanel = createServerPanel();
        m_dialogPanelServer.add(serverPanel, gc);
        super.addTab("MolConvert Server", m_dialogPanelServer);
    }

    /**
	 * (non-Javadoc)
	 *
	 * @see org.knime.core.node.defaultnodesettings.DefaultNodeSettingsPane#loadAdditionalSettingsFrom(org.knime.core.node.NodeSettingsRO,
	 *      org.knime.core.data.DataTableSpec[])
	 */
    public void loadAdditionalSettingsFrom(final NodeSettingsRO settings, final DataTableSpec[] specs) throws NotConfigurableException {
        if (specs != null && specs.length > 0) {
            ArrayList<String> fieldArr = new ArrayList<String>();
            for (int col = 0; col < specs[0].getNumColumns(); col++) {
                fieldArr.add(specs[0].getColumnSpec(col).getName());
            }
            String fields[] = fieldArr.toArray(new String[fieldArr.size()]);
            this.loadFieldsIntoListModel(fields);
        }
        if (settings.containsKey(MolConvertInNodeModel.CFGKEY_OPTION_OUTFMT)) {
            this.m_outputFormats.setSelectedItem(settings.getString(MolConvertInNodeModel.CFGKEY_OPTION_OUTFMT, ""));
        }
        if (settings.containsKey(MolConvertInNodeModel.CFGKEY_OPTION_F)) {
            this.m_checkBox_F.setSelected(settings.getBoolean(MolConvertInNodeModel.CFGKEY_OPTION_F, false));
        }
        if (settings.containsKey(MolConvertInNodeModel.CFGKEY_OPTION_2_OR_3)) {
            this.m_checkbox_2D3D.setSelected(settings.getBoolean(MolConvertInNodeModel.CFGKEY_OPTION_2_OR_3, false));
        }
        if (settings.containsKey(MolConvertInNodeModel.CFGKEY_OPTION_2)) {
            this.m_radio_2D.setSelected(settings.getBoolean(MolConvertInNodeModel.CFGKEY_OPTION_2, false));
        }
        if (settings.containsKey(MolConvertInNodeModel.CFGKEY_OPTION_3)) {
            this.m_radio_3D.setSelected(settings.getBoolean(MolConvertInNodeModel.CFGKEY_OPTION_3, false));
        }
        if (settings.containsKey(MolConvertInNodeModel.CFGKEY_OPTION_SERVER_ADDRESS)) {
            this.m_serverAddress.setText(settings.getString(MolConvertInNodeModel.CFGKEY_OPTION_SERVER_ADDRESS, MolConvertInNodeModel.DEFAULT_SERVER_ADDRESS));
        }
        if (settings.containsKey(MolConvertInNodeModel.CFGKEY_OPTION_SERVER_PORT)) {
            int port = 80;
            try {
                port = settings.getInt(MolConvertInNodeModel.CFGKEY_OPTION_SERVER_PORT);
            } catch (InvalidSettingsException ex) {
                System.out.println("invalid setting, using default server port");
                logger.error("invalid settings value of server port. Using default.");
            }
            this.m_serverPort.setText(String.valueOf(port));
        }
        if (settings.containsKey(MolConvertInNodeModel.CFGKEY_OPTION_SERVER_NFS_PATH)) {
            this.m_serverNFSPath.setText(settings.getString(MolConvertInNodeModel.CFGKEY_OPTION_SERVER_NFS_PATH, MolConvertInNodeModel.DEFAULT_SERVER_NFS_PATH));
        }
        if (settings.containsKey(MolConvertInNodeModel.CFGKEY_OPTION_SERVER_TEMP_FOLDER)) {
            this.m_serverTempFolder.setText(settings.getString(MolConvertInNodeModel.CFGKEY_OPTION_SERVER_TEMP_FOLDER, MolConvertInNodeModel.DEFAULT_SERVER_TEMP_FOLDER));
        }
        if (settings.containsKey(MolConvertInNodeModel.CFGKEY_OPTION_SERVER_PRIORITY)) {
            this.m_serverPriority.setSelectedItem(settings.getString(MolConvertInNodeModel.CFGKEY_OPTION_SERVER_PRIORITY, DEFAULT_SERVER_PRIORITY));
        }
    }

    /**
	 * (non-Javadoc)
	 *
	 * @see org.knime.core.node.defaultnodesettings.DefaultNodeSettingsPane#saveAdditionalSettingsTo(org.knime.core.node.NodeSettingsWO)
	 */
    public void saveAdditionalSettingsTo(final NodeSettingsWO settings) throws InvalidSettingsException {
        if (m_serverAddress.getText().equals(EMPTY_SERVER_ADDDRESS)) {
            throw new InvalidSettingsException("empty server address! Please configure.");
        }
        String out = (String) m_outputFormats.getSelectedItem();
        SettingsModelString outFormat = new SettingsModelString(MolConvertInNodeModel.CFGKEY_OPTION_OUTFMT, out);
        outFormat.saveSettingsTo(settings);
        SettingsModelBoolean optF = new SettingsModelBoolean(MolConvertInNodeModel.CFGKEY_OPTION_F, m_checkBox_F.isSelected());
        optF.saveSettingsTo(settings);
        SettingsModelBoolean opt2or3 = new SettingsModelBoolean(MolConvertInNodeModel.CFGKEY_OPTION_2_OR_3, m_checkbox_2D3D.isSelected());
        opt2or3.saveSettingsTo(settings);
        SettingsModelBoolean opt2 = new SettingsModelBoolean(MolConvertInNodeModel.CFGKEY_OPTION_2, m_radio_2D.isSelected());
        opt2.saveSettingsTo(settings);
        SettingsModelBoolean opt3 = new SettingsModelBoolean(MolConvertInNodeModel.CFGKEY_OPTION_3, m_radio_3D.isSelected());
        opt3.saveSettingsTo(settings);
        SettingsModelString serverAddress = new SettingsModelString(MolConvertInNodeModel.CFGKEY_OPTION_SERVER_ADDRESS, m_serverAddress.getText());
        serverAddress.saveSettingsTo(settings);
        SettingsModelInteger serverPort = new SettingsModelInteger(MolConvertInNodeModel.CFGKEY_OPTION_SERVER_PORT, Integer.parseInt(m_serverPort.getText()));
        serverPort.saveSettingsTo(settings);
        SettingsModelString serverNFSPath = new SettingsModelString(MolConvertInNodeModel.CFGKEY_OPTION_SERVER_NFS_PATH, m_serverNFSPath.getText());
        serverNFSPath.saveSettingsTo(settings);
        SettingsModelString serverTempFolder = new SettingsModelString(MolConvertInNodeModel.CFGKEY_OPTION_SERVER_TEMP_FOLDER, m_serverTempFolder.getText());
        serverTempFolder.saveSettingsTo(settings);
        String priority = (String) m_serverPriority.getSelectedItem();
        SettingsModelString serverPriority = new SettingsModelString(MolConvertInNodeModel.CFGKEY_OPTION_SERVER_PRIORITY, priority);
        serverPriority.saveSettingsTo(settings);
        String[] fields = null;
        if (m_listModel2.getSize() > 0) {
            fields = new String[m_listModel2.getSize()];
            for (int elem = 0; elem < m_listModel2.getSize(); elem++) {
                fields[elem] = (String) m_listModel2.get(elem);
            }
            SettingsModelFilterString fieldschosen = new SettingsModelFilterString(MolConvertInNodeModel.CFGKEY_FIELDS, fields, new String[0]);
            fieldschosen.saveSettingsTo(settings);
        }
    }
}
