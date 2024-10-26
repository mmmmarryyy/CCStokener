package org.npsnet.v.gui;

import java.awt.*;
import java.awt.event.*;
import java.io.*;
import java.net.*;
import java.util.*;
import java.util.prefs.*;
import javax.naming.*;
import javax.naming.directory.*;
import javax.swing.*;
import javax.swing.border.*;
import javax.swing.event.*;
import javax.swing.table.*;
import org.npsnet.v.kernel.Module;
import org.npsnet.v.kernel.ModuleDestructionEvent;
import org.npsnet.v.kernel.ModuleEvent;
import org.npsnet.v.kernel.ModuleEventListener;
import org.npsnet.v.kernel.ModuleReplacementEvent;
import org.npsnet.v.services.resource.ResourceName;

/**
 * The publish prototype dialog.
 *
 * @author Andrzej Kapolka
 */
public class PublishPrototypeDialog extends JDialog {

    /**
     * The resource component attribute.
     */
    private static final String RC = "rc";

    /**
     * The object class attribute.
     */
    private static final String OBJECT_CLASS = "objectClass";

    /**
     * The RDS resource object class.
     */
    private static final String RDS_RESOURCE = "rdsResource";

    /**
     * The implementation URL attribute.
     */
    private static final String IMPLEMENTATION_URL = "Implementation-URL";

    /**
     * The content type attribute.
     */
    private static final String CONTENT_TYPE = "Content-Type";

    /**
     * The NPSNET-V prototype type.
     */
    private static final String PROTOTYPE_TYPE = "application/x-npsnetv-prototype+xml";

    /**
     * The target module.
     */
    private Module target;

    /**
     * The target listener.
     */
    private ModuleEventListener targetListener;

    /**
     * The upload prototype check box.
     */
    private JCheckBox uploadPrototypeBox;

    /**
     * The prototype URL label.
     */
    private JLabel prototypeURLLabel;

    /**
     * The prototype URL field.
     */
    private HistoryTextField prototypeURLField;

    /**
     * The post metadata check box.
     */
    private JCheckBox postMetadataBox;

    /**
     * The prototype name label.
     */
    private JLabel prototypeNameLabel;

    /**
     * The prototype name field.
     */
    private HistoryTextField prototypeNameField;

    /**
     * The directory URL label.
     */
    private JLabel directoryURLLabel;

    /**
     * The directory URL field.
     */
    private JTextField directoryURLField;

    /**
     * The directory principal label.
     */
    private JLabel directoryPrincipalLabel;

    /**
     * The directory principal field.
     */
    private JTextField directoryPrincipalField;

    /**
     * The directory credentials label.
     */
    private JLabel directoryCredentialsLabel;

    /**
     * The directory credentials field.
     */
    private JTextField directoryCredentialsField;

    /**
     * The table of attributes.
     */
    private JTable attributesTable;

    /**
     * The new attribute button.
     */
    private JButton newAttributeButton;

    /**
     * The delete attribute button.
     */
    private JButton deleteAttributeButton;

    /**
     * Constructor.
     *
     * @param owner the owner of the dialog
     * @param pTarget the target module
     */
    public PublishPrototypeDialog(Frame owner, Module pTarget) {
        super(owner, "Publish Prototype");
        target = pTarget;
        createTargetListener();
        initialize();
    }

    /**
     * Constructor.
     *
     * @param owner the owner of the dialog
     * @param pTarget the target module
     */
    public PublishPrototypeDialog(Dialog owner, Module pTarget) {
        super(owner, "Publish Prototype");
        target = pTarget;
        createTargetListener();
        initialize();
    }

    /**
     * Called when this component is rendered undisplayable.
     */
    public void removeNotify() {
        super.removeNotify();
        target.removeEventListener(ModuleReplacementEvent.class, targetListener);
        target.removeEventListener(ModuleDestructionEvent.class, targetListener);
    }

    /**
     * Creates the target listener object.
     */
    private void createTargetListener() {
        targetListener = new ModuleEventListener() {

            public void moduleEventFired(ModuleEvent me) {
                if (me instanceof ModuleReplacementEvent) {
                    target.removeEventListener(ModuleReplacementEvent.class, targetListener);
                    target.removeEventListener(ModuleDestructionEvent.class, targetListener);
                    target = ((ModuleReplacementEvent) me).getReplacementModule();
                } else {
                    dispose();
                }
            }
        };
    }

    /**
     * Initializes this dialog.
     */
    private void initialize() {
        JPanel inputPanel = new JPanel(new GridBagLayout());
        GridBagConstraints nameConstraints = new GridBagConstraints(), valueConstraints = new GridBagConstraints();
        nameConstraints.anchor = GridBagConstraints.WEST;
        nameConstraints.weightx = 0.05;
        nameConstraints.insets = new Insets(4, 0, 4, 0);
        valueConstraints.anchor = GridBagConstraints.WEST;
        valueConstraints.weightx = 0.95;
        valueConstraints.insets = new Insets(4, 0, 4, 0);
        valueConstraints.gridwidth = GridBagConstraints.REMAINDER;
        valueConstraints.fill = GridBagConstraints.HORIZONTAL;
        uploadPrototypeBox = new JCheckBox("Upload Prototype to Web Server", true);
        uploadPrototypeBox.addActionListener(new ActionListener() {

            public void actionPerformed(ActionEvent ae) {
                boolean enabled = uploadPrototypeBox.isSelected();
                prototypeURLLabel.setEnabled(enabled);
                prototypeURLField.setEnabled(enabled);
            }
        });
        inputPanel.add(uploadPrototypeBox, valueConstraints);
        JPanel webServerOptionPanel = new JPanel(new GridBagLayout());
        prototypeURLLabel = new JLabel("Prototype URL:");
        prototypeURLField = new HistoryTextField(getClass().getName() + ".prototypeURL");
        webServerOptionPanel.add(prototypeURLLabel, nameConstraints);
        webServerOptionPanel.add(prototypeURLField, valueConstraints);
        webServerOptionPanel.setBorder(new TitledBorder("Web Server Options"));
        inputPanel.add(webServerOptionPanel, valueConstraints);
        postMetadataBox = new JCheckBox("Post Metadata to Directory Server", true);
        postMetadataBox.addActionListener(new ActionListener() {

            public void actionPerformed(ActionEvent ae) {
                boolean enabled = postMetadataBox.isSelected();
                prototypeNameLabel.setEnabled(enabled);
                prototypeNameField.setEnabled(enabled);
                directoryURLLabel.setEnabled(enabled);
                directoryURLField.setEnabled(enabled);
                directoryPrincipalLabel.setEnabled(enabled);
                directoryPrincipalField.setEnabled(enabled);
                directoryCredentialsLabel.setEnabled(enabled);
                directoryCredentialsField.setEnabled(enabled);
                attributesTable.setEnabled(enabled);
                newAttributeButton.setEnabled(enabled);
                deleteAttributeButton.setEnabled(enabled && (attributesTable.getSelectedRow() != -1));
            }
        });
        inputPanel.add(postMetadataBox, valueConstraints);
        JPanel directoryServerOptionPanel = new JPanel(new GridBagLayout());
        prototypeNameLabel = new JLabel("Prototype Name:");
        prototypeNameField = new HistoryTextField(getClass().getName() + ".prototypeName");
        directoryServerOptionPanel.add(prototypeNameLabel, nameConstraints);
        directoryServerOptionPanel.add(prototypeNameField, valueConstraints);
        Preferences prefs = Preferences.userNodeForPackage(getClass());
        directoryURLLabel = new JLabel("Directory URL:");
        directoryURLField = new JTextField(prefs.get("directoryURL", ""));
        directoryServerOptionPanel.add(directoryURLLabel, nameConstraints);
        directoryServerOptionPanel.add(directoryURLField, valueConstraints);
        directoryPrincipalLabel = new JLabel("Principal:");
        directoryPrincipalField = new JTextField(prefs.get("directoryPrincipal", ""));
        directoryServerOptionPanel.add(directoryPrincipalLabel, nameConstraints);
        directoryServerOptionPanel.add(directoryPrincipalField, valueConstraints);
        directoryCredentialsLabel = new JLabel("Credentials:");
        directoryCredentialsField = new JPasswordField(prefs.get("directoryCredentials", ""));
        directoryServerOptionPanel.add(directoryCredentialsLabel, nameConstraints);
        directoryServerOptionPanel.add(directoryCredentialsField, valueConstraints);
        JPanel attributesPanel = new JPanel(new BorderLayout());
        Preferences attrPrefs = prefs.node("attributes");
        String[] keys = new String[0];
        try {
            keys = attrPrefs.keys();
        } catch (BackingStoreException bse) {
        }
        Object[][] attrs = new Object[keys.length][2];
        for (int i = 0; i < keys.length; i++) {
            attrs[i][0] = keys[i];
            attrs[i][1] = attrPrefs.get(keys[i], "");
        }
        attributesTable = new JTable(new DefaultTableModel(attrs, new Object[] { "Name", "Value" }));
        attributesTable.setColumnSelectionAllowed(false);
        attributesTable.getSelectionModel().setSelectionMode(ListSelectionModel.SINGLE_SELECTION);
        attributesTable.getSelectionModel().addListSelectionListener(new ListSelectionListener() {

            public void valueChanged(ListSelectionEvent lse) {
                deleteAttributeButton.setEnabled((attributesTable.getSelectedRow() != -1) && postMetadataBox.isSelected());
            }
        });
        attributesPanel.add(attributesTable.getTableHeader(), BorderLayout.NORTH);
        attributesPanel.add(attributesTable, BorderLayout.CENTER);
        JPanel attributesButtonPanel = new JPanel(new FlowLayout(FlowLayout.RIGHT));
        newAttributeButton = new JButton("New");
        newAttributeButton.addActionListener(new ActionListener() {

            public void actionPerformed(ActionEvent ae) {
                DefaultTableModel dtm = (DefaultTableModel) attributesTable.getModel();
                dtm.addRow(new Object[] { "", "" });
                attributesTable.grabFocus();
                attributesTable.setRowSelectionInterval(dtm.getRowCount() - 1, dtm.getRowCount() - 1);
                attributesTable.editCellAt(dtm.getRowCount() - 1, 0);
            }
        });
        attributesButtonPanel.add(newAttributeButton);
        deleteAttributeButton = new JButton("Delete");
        deleteAttributeButton.addActionListener(new ActionListener() {

            public void actionPerformed(ActionEvent ae) {
                DefaultTableModel dtm = (DefaultTableModel) attributesTable.getModel();
                dtm.removeRow(attributesTable.getSelectedRow());
            }
        });
        deleteAttributeButton.setEnabled(false);
        attributesButtonPanel.add(deleteAttributeButton);
        attributesPanel.add(attributesButtonPanel, BorderLayout.SOUTH);
        attributesPanel.setBorder(new TitledBorder("Attributes"));
        directoryServerOptionPanel.add(attributesPanel, valueConstraints);
        directoryServerOptionPanel.setBorder(new TitledBorder("Directory Server Options"));
        inputPanel.add(directoryServerOptionPanel, valueConstraints);
        getContentPane().add(new JScrollPane(inputPanel), BorderLayout.CENTER);
        JPanel jp = new JPanel();
        JButton okButton = new JButton("OK"), cancelButton = new JButton("Cancel");
        jp.setLayout(new FlowLayout(FlowLayout.RIGHT));
        jp.add(okButton);
        jp.add(cancelButton);
        getContentPane().add(jp, BorderLayout.SOUTH);
        okButton.addActionListener(new ActionListener() {

            public void actionPerformed(ActionEvent ae) {
                publish();
                dispose();
            }
        });
        cancelButton.addActionListener(new ActionListener() {

            public void actionPerformed(ActionEvent ae) {
                dispose();
            }
        });
        setDefaultCloseOperation(DISPOSE_ON_CLOSE);
        target.addEventListener(ModuleReplacementEvent.class, targetListener);
        target.addEventListener(ModuleDestructionEvent.class, targetListener);
        setSize(prefs.getInt("PublishPrototypeDialog.width", 512), prefs.getInt("PublishPrototypeDialog.height", 384));
        addComponentListener(new ComponentAdapter() {

            public void componentResized(ComponentEvent ce) {
                Preferences prefs = Preferences.userNodeForPackage(getClass());
                prefs.putInt("PublishPrototypeDialog.width", getWidth());
                prefs.putInt("PublishPrototypeDialog.height", getHeight());
            }
        });
    }

    /**
     * Publishes the prototype based on the information specified by
     * the user.
     */
    private void publish() {
        if (uploadPrototypeBox.isSelected()) {
            try {
                URL url = new URL(prototypeURLField.getText());
                prototypeURLField.recordText();
                PrintStream ps;
                HttpURLConnection huc = null;
                if (url.getProtocol().equals("file")) {
                    ps = new PrintStream(new FileOutputStream(url.getFile()));
                } else {
                    URLConnection urlc = url.openConnection();
                    urlc.setDoOutput(true);
                    if (urlc instanceof HttpURLConnection) {
                        huc = ((HttpURLConnection) urlc);
                        huc.setRequestMethod("PUT");
                    }
                    ps = new PrintStream(urlc.getOutputStream());
                }
                target.writePrototype(ps);
                if (huc != null) {
                    huc.getResponseCode();
                }
            } catch (Exception e) {
                JOptionPane.showMessageDialog(this, e, "Error Uploading Prototype", JOptionPane.ERROR_MESSAGE);
            }
        }
        if (postMetadataBox.isSelected()) {
            try {
                Hashtable env = new Hashtable();
                env.put(Context.INITIAL_CONTEXT_FACTORY, "com.sun.jndi.ldap.LdapCtxFactory");
                env.put(Context.PROVIDER_URL, directoryURLField.getText());
                env.put(Context.SECURITY_PRINCIPAL, directoryPrincipalField.getText());
                env.put(Context.SECURITY_CREDENTIALS, directoryCredentialsField.getText());
                InitialDirContext dirContext = new InitialDirContext(env);
                Preferences prefs = Preferences.userNodeForPackage(getClass());
                prefs.put("directoryURL", directoryURLField.getText());
                prefs.put("directoryPrincipal", directoryPrincipalField.getText());
                prefs.put("directoryCredentials", directoryCredentialsField.getText());
                BasicAttributes attr = new BasicAttributes();
                attr.put(OBJECT_CLASS, RDS_RESOURCE);
                attr.put(CONTENT_TYPE, PROTOTYPE_TYPE);
                if (uploadPrototypeBox.isSelected()) {
                    attr.put(IMPLEMENTATION_URL, prototypeURLField.getText());
                }
                DefaultTableModel dtm = (DefaultTableModel) attributesTable.getModel();
                Preferences attrPrefs = prefs.node("attributes");
                for (int i = 0; i < dtm.getRowCount(); i++) {
                    attr.put((String) dtm.getValueAt(i, 0), (String) dtm.getValueAt(i, 1));
                    attrPrefs.put((String) dtm.getValueAt(i, 0), (String) dtm.getValueAt(i, 1));
                }
                NameParser np = dirContext.getNameParser("");
                Enumeration comps = new ResourceName(prototypeNameField.getText()).getAll();
                prototypeNameField.recordText();
                Name resourcedn = np.parse("");
                while (comps.hasMoreElements()) {
                    resourcedn.add(RC + "=" + (String) comps.nextElement());
                    try {
                        dirContext.createSubcontext(resourcedn);
                    } catch (NameAlreadyBoundException nabe) {
                    }
                }
                dirContext.modifyAttributes(resourcedn, DirContext.REPLACE_ATTRIBUTE, attr);
            } catch (Exception e) {
                JOptionPane.showMessageDialog(this, e, "Error Posting Metadata", JOptionPane.ERROR_MESSAGE);
            }
        }
    }
}
