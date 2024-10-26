package au.com.farahtek.hudsontracker.configure;

import au.com.farahtek.hudsontracker.HudsonConfig.AuthenticationMode;
import java.awt.Color;
import java.awt.event.ItemEvent;
import java.io.File;
import java.net.MalformedURLException;
import java.net.URL;
import java.net.URLConnection;
import java.text.NumberFormat;
import java.text.ParseException;
import javax.swing.InputVerifier;
import javax.swing.JComponent;
import javax.swing.JOptionPane;
import javax.swing.JTextField;
import au.com.farahtek.hudsontracker.HudsonConfig;
import au.com.farahtek.hudsontracker.HudsonTrackerTray;
import au.com.farahtek.hudsontracker.Main;
import au.com.farahtek.hudsontracker.Messages;
import com.sun.syndication.io.SyndFeedInput;
import com.sun.syndication.io.XmlReader;
import java.awt.event.ItemListener;
import java.util.ArrayList;
import java.util.List;
import javax.swing.DefaultComboBoxModel;

/**
 * UI for configuring hudson tracker.
 * @author  Mike
 */
public class ConfigureFrame extends javax.swing.JFrame {

    private static final long serialVersionUID = 8111867866361405571L;

    private HudsonTrackerTray hudsonTrackerTray = null;

    private List<JComponent> componentsToVerifyOnOk = new ArrayList<JComponent>();

    public ConfigureFrame(HudsonConfig hudsonConfig, HudsonTrackerTray hudsonTrackerTray) {
        this(hudsonConfig);
        this.hudsonTrackerTray = hudsonTrackerTray;
    }

    /** Creates new form ConfigureFrame */
    public ConfigureFrame(HudsonConfig hudsonConfig) {
        initComponents();
        txtRefreshTime.setInputVerifier(new NumericInputVerifier(HudsonConfig.MIN_REFRESH_TIME));
        componentsToVerifyOnOk.add(txtRefreshTime);
        txtRefreshTime.setToolTipText(Messages.getString("ht.config.validation.numberGreaterThan") + HudsonConfig.MIN_REFRESH_TIME);
        txtMaxRssToRead.setInputVerifier(new NumericInputVerifier(HudsonConfig.MIN_RSS_FEEDS_TO_READ));
        componentsToVerifyOnOk.add(txtMaxRssToRead);
        txtMaxRssToRead.setToolTipText("Must be a number greater than " + HudsonConfig.MIN_RSS_FEEDS_TO_READ);
        txtTimeout.setInputVerifier(new NumericInputVerifier(HudsonConfig.MIN_TIME_OUT));
        componentsToVerifyOnOk.add(txtTimeout);
        txtTimeout.setToolTipText("Must be a number greater than " + HudsonConfig.MIN_TIME_OUT);
        txtUserName.setInputVerifier(new StringNotEmptyVerifier());
        txtUserName.setToolTipText("Cannot be empty");
        componentsToVerifyOnOk.add(txtUserName);
        txtPassword.setInputVerifier(new StringNotEmptyVerifier());
        txtPassword.setToolTipText("Cannot be empty");
        componentsToVerifyOnOk.add(txtPassword);
        txtURL.setInputVerifier(new InputVerifier() {

            @Override
            public boolean verify(JComponent comp) {
                JTextField text = (JTextField) comp;
                try {
                    String value = text.getText();
                    new URL(value);
                    text.setBackground(Color.WHITE);
                    text.setToolTipText(Messages.getString("ht.config.validation.validUrl"));
                    return true;
                } catch (MalformedURLException ex) {
                    text.setToolTipText(ex.getLocalizedMessage());
                    text.setBackground(Color.RED);
                    return false;
                }
            }
        });
        componentsToVerifyOnOk.add(txtURL);
        txtURL.setToolTipText("Must be a valid URL");
        populateComponents(hudsonConfig);
        setDefaultCloseOperation(DO_NOTHING_ON_CLOSE);
    }

    private void populateComponents(HudsonConfig hudsonConfig) {
        txtRefreshTime.setText(HudsonConfig.DEFAULT_REFRESH_TIME + "");
        txtMaxRssToRead.setText(HudsonConfig.DEFAULT_RSS_FEEDS_TO_READ + "");
        txtTimeout.setText(HudsonConfig.DEFAULT_TIME_OUT + "");
        cmbAuthMode.setModel(new DefaultComboBoxModel(HudsonConfig.AuthenticationMode.values()));
        cmbAuthMode.addItemListener(new ItemListener() {

            @Override
            public void itemStateChanged(ItemEvent arg0) {
                if (AuthenticationMode.NONE.equals(cmbAuthMode.getSelectedItem())) {
                    lblUsername.setEnabled(false);
                    txtUserName.setEnabled(false);
                    lblPassword.setEnabled(false);
                    txtPassword.setEnabled(false);
                } else {
                    lblUsername.setEnabled(true);
                    txtUserName.setEnabled(true);
                    lblPassword.setEnabled(true);
                    txtPassword.setEnabled(true);
                }
            }
        });
        if (hudsonConfig != null) {
            txtRefreshTime.setText(hudsonConfig.getRefreshTime() + "");
            txtMaxRssToRead.setText(hudsonConfig.getMaxRSSEntriesToRead() + "");
            txtTimeout.setText(hudsonConfig.getTimeout() + "");
            txtURL.setText(hudsonConfig.getHudsonUrl());
            chkEnableSounds.setSelected(hudsonConfig.shouldPlaySounds());
            cmbAuthMode.setSelectedItem(hudsonConfig.getAuthenticationMode());
            txtUserName.setText(hudsonConfig.getUsername());
            txtPassword.setText(hudsonConfig.getPassword());
        }
    }

    private class StringNotEmptyVerifier extends InputVerifier {

        @Override
        public boolean verify(JComponent comp) {
            JTextField textField = (JTextField) comp;
            boolean isValid = textField.isEnabled() == false || textField.getText().length() > 0;
            if (isValid) {
                textField.setBackground(Color.WHITE);
            } else {
                textField.setBackground(Color.RED);
            }
            return isValid;
        }
    }

    private class NumericInputVerifier extends InputVerifier {

        private long minimum;

        public NumericInputVerifier(long minimum) {
            this.minimum = minimum;
        }

        @Override
        public boolean verify(JComponent comp) {
            boolean isValid = true;
            JTextField text = (JTextField) comp;
            String value = text.getText();
            try {
                isValid = value.matches("\\d*") && NumberFormat.getNumberInstance().parse(value).intValue() >= minimum;
            } catch (ParseException ex) {
                isValid = false;
            }
            if (isValid) {
                text.setBackground(Color.WHITE);
            } else {
                text.setBackground(Color.RED);
            }
            return isValid;
        }
    }

    /** This method is called from within the constructor to
     * initialize the form.
     * WARNING: Do NOT modify this code. The content of this method is
     * always regenerated by the Form Editor.
     */
    @SuppressWarnings("unchecked")
    private void initComponents() {
        java.awt.GridBagConstraints gridBagConstraints;
        lblHeading = new javax.swing.JLabel();
        lblUrl = new javax.swing.JLabel();
        txtURL = new javax.swing.JTextField();
        testConnectionButton = new javax.swing.JButton();
        lblRefreshTime = new javax.swing.JLabel();
        txtRefreshTime = new javax.swing.JTextField();
        lblMaxRssToRead = new javax.swing.JLabel();
        txtMaxRssToRead = new javax.swing.JTextField();
        lblEnableSounds = new javax.swing.JLabel();
        chkEnableSounds = new javax.swing.JCheckBox();
        cancelButton = new javax.swing.JButton();
        okButton = new javax.swing.JButton();
        lblConnectionTimeout = new javax.swing.JLabel();
        txtTimeout = new javax.swing.JTextField();
        lblUsername = new javax.swing.JLabel();
        txtUserName = new javax.swing.JTextField();
        lblPassword = new javax.swing.JLabel();
        txtPassword = new javax.swing.JPasswordField();
        cmbAuthMode = new javax.swing.JComboBox();
        lblAuthMode = new javax.swing.JLabel();
        setDefaultCloseOperation(javax.swing.WindowConstants.EXIT_ON_CLOSE);
        getContentPane().setLayout(new java.awt.GridBagLayout());
        lblHeading.setFont(new java.awt.Font("Tahoma", 1, 11));
        lblHeading.setText("Configure hudsonTracker");
        gridBagConstraints = new java.awt.GridBagConstraints();
        gridBagConstraints.anchor = java.awt.GridBagConstraints.NORTHWEST;
        gridBagConstraints.insets = new java.awt.Insets(0, 8, 0, 0);
        getContentPane().add(lblHeading, gridBagConstraints);
        lblUrl.setText("Hudson URL:");
        gridBagConstraints = new java.awt.GridBagConstraints();
        gridBagConstraints.gridx = 0;
        gridBagConstraints.gridy = 1;
        gridBagConstraints.anchor = java.awt.GridBagConstraints.EAST;
        gridBagConstraints.insets = new java.awt.Insets(0, 8, 0, 5);
        getContentPane().add(lblUrl, gridBagConstraints);
        txtURL.setText("http://localhost:8080");
        gridBagConstraints = new java.awt.GridBagConstraints();
        gridBagConstraints.gridx = 1;
        gridBagConstraints.gridy = 1;
        gridBagConstraints.fill = java.awt.GridBagConstraints.HORIZONTAL;
        gridBagConstraints.weightx = 1.0;
        getContentPane().add(txtURL, gridBagConstraints);
        testConnectionButton.setText("Test");
        testConnectionButton.addActionListener(new java.awt.event.ActionListener() {

            public void actionPerformed(java.awt.event.ActionEvent evt) {
                testConnectionButtonActionPerformed(evt);
            }
        });
        gridBagConstraints = new java.awt.GridBagConstraints();
        gridBagConstraints.gridx = 2;
        gridBagConstraints.gridy = 1;
        getContentPane().add(testConnectionButton, gridBagConstraints);
        lblRefreshTime.setText("Refresh time (ms):");
        gridBagConstraints = new java.awt.GridBagConstraints();
        gridBagConstraints.gridx = 0;
        gridBagConstraints.gridy = 6;
        gridBagConstraints.anchor = java.awt.GridBagConstraints.EAST;
        gridBagConstraints.insets = new java.awt.Insets(0, 8, 0, 5);
        getContentPane().add(lblRefreshTime, gridBagConstraints);
        txtRefreshTime.setText("60000");
        gridBagConstraints = new java.awt.GridBagConstraints();
        gridBagConstraints.gridx = 1;
        gridBagConstraints.gridy = 6;
        gridBagConstraints.gridwidth = 2;
        gridBagConstraints.fill = java.awt.GridBagConstraints.HORIZONTAL;
        gridBagConstraints.weightx = 1.0;
        gridBagConstraints.insets = new java.awt.Insets(0, 0, 0, 8);
        getContentPane().add(txtRefreshTime, gridBagConstraints);
        lblMaxRssToRead.setText("Max RSS entries to read: ");
        gridBagConstraints = new java.awt.GridBagConstraints();
        gridBagConstraints.gridx = 0;
        gridBagConstraints.gridy = 7;
        gridBagConstraints.anchor = java.awt.GridBagConstraints.EAST;
        gridBagConstraints.insets = new java.awt.Insets(0, 8, 0, 5);
        getContentPane().add(lblMaxRssToRead, gridBagConstraints);
        txtMaxRssToRead.setText("1000");
        gridBagConstraints = new java.awt.GridBagConstraints();
        gridBagConstraints.gridx = 1;
        gridBagConstraints.gridy = 7;
        gridBagConstraints.gridwidth = 2;
        gridBagConstraints.fill = java.awt.GridBagConstraints.HORIZONTAL;
        gridBagConstraints.weightx = 1.0;
        gridBagConstraints.insets = new java.awt.Insets(0, 0, 0, 8);
        getContentPane().add(txtMaxRssToRead, gridBagConstraints);
        lblEnableSounds.setText("Enable Sounds: ");
        gridBagConstraints = new java.awt.GridBagConstraints();
        gridBagConstraints.gridx = 0;
        gridBagConstraints.gridy = 8;
        gridBagConstraints.anchor = java.awt.GridBagConstraints.EAST;
        gridBagConstraints.insets = new java.awt.Insets(0, 8, 0, 5);
        getContentPane().add(lblEnableSounds, gridBagConstraints);
        chkEnableSounds.setSelected(true);
        gridBagConstraints = new java.awt.GridBagConstraints();
        gridBagConstraints.gridx = 1;
        gridBagConstraints.gridy = 8;
        gridBagConstraints.fill = java.awt.GridBagConstraints.HORIZONTAL;
        getContentPane().add(chkEnableSounds, gridBagConstraints);
        cancelButton.setText("Cancel");
        cancelButton.addActionListener(new java.awt.event.ActionListener() {

            public void actionPerformed(java.awt.event.ActionEvent evt) {
                cancelButtonActionPerformed(evt);
            }
        });
        gridBagConstraints = new java.awt.GridBagConstraints();
        gridBagConstraints.gridx = 2;
        gridBagConstraints.gridy = 9;
        gridBagConstraints.insets = new java.awt.Insets(0, 0, 0, 8);
        getContentPane().add(cancelButton, gridBagConstraints);
        okButton.setText("OK");
        okButton.addActionListener(new java.awt.event.ActionListener() {

            public void actionPerformed(java.awt.event.ActionEvent evt) {
                okButtonActionPerformed(evt);
            }
        });
        gridBagConstraints = new java.awt.GridBagConstraints();
        gridBagConstraints.gridx = 1;
        gridBagConstraints.gridy = 9;
        gridBagConstraints.anchor = java.awt.GridBagConstraints.EAST;
        getContentPane().add(okButton, gridBagConstraints);
        lblConnectionTimeout.setText("Connection Timeout (ms):");
        gridBagConstraints = new java.awt.GridBagConstraints();
        gridBagConstraints.gridx = 0;
        gridBagConstraints.gridy = 5;
        gridBagConstraints.anchor = java.awt.GridBagConstraints.EAST;
        gridBagConstraints.insets = new java.awt.Insets(0, 8, 0, 5);
        getContentPane().add(lblConnectionTimeout, gridBagConstraints);
        gridBagConstraints = new java.awt.GridBagConstraints();
        gridBagConstraints.gridx = 1;
        gridBagConstraints.gridy = 5;
        gridBagConstraints.gridwidth = 2;
        gridBagConstraints.fill = java.awt.GridBagConstraints.HORIZONTAL;
        gridBagConstraints.weightx = 1.0;
        gridBagConstraints.insets = new java.awt.Insets(0, 0, 0, 8);
        getContentPane().add(txtTimeout, gridBagConstraints);
        lblUsername.setText("User Name:");
        lblUsername.setEnabled(false);
        gridBagConstraints = new java.awt.GridBagConstraints();
        gridBagConstraints.gridx = 0;
        gridBagConstraints.gridy = 3;
        gridBagConstraints.anchor = java.awt.GridBagConstraints.EAST;
        gridBagConstraints.insets = new java.awt.Insets(0, 8, 0, 5);
        getContentPane().add(lblUsername, gridBagConstraints);
        txtUserName.setEnabled(false);
        gridBagConstraints = new java.awt.GridBagConstraints();
        gridBagConstraints.gridx = 1;
        gridBagConstraints.gridy = 3;
        gridBagConstraints.gridwidth = 2;
        gridBagConstraints.fill = java.awt.GridBagConstraints.HORIZONTAL;
        gridBagConstraints.weightx = 1.0;
        gridBagConstraints.insets = new java.awt.Insets(0, 0, 0, 8);
        getContentPane().add(txtUserName, gridBagConstraints);
        lblPassword.setText("Password:");
        lblPassword.setEnabled(false);
        gridBagConstraints = new java.awt.GridBagConstraints();
        gridBagConstraints.gridx = 0;
        gridBagConstraints.gridy = 4;
        gridBagConstraints.anchor = java.awt.GridBagConstraints.EAST;
        gridBagConstraints.insets = new java.awt.Insets(0, 8, 0, 5);
        getContentPane().add(lblPassword, gridBagConstraints);
        txtPassword.setEnabled(false);
        gridBagConstraints = new java.awt.GridBagConstraints();
        gridBagConstraints.gridx = 1;
        gridBagConstraints.gridy = 4;
        gridBagConstraints.gridwidth = 2;
        gridBagConstraints.fill = java.awt.GridBagConstraints.HORIZONTAL;
        gridBagConstraints.weightx = 1.0;
        gridBagConstraints.insets = new java.awt.Insets(0, 0, 0, 8);
        getContentPane().add(txtPassword, gridBagConstraints);
        cmbAuthMode.setModel(new javax.swing.DefaultComboBoxModel(new String[] { "Item 1", "Item 2", "Item 3", "Item 4" }));
        gridBagConstraints = new java.awt.GridBagConstraints();
        gridBagConstraints.gridx = 1;
        gridBagConstraints.gridy = 2;
        gridBagConstraints.gridwidth = 2;
        gridBagConstraints.fill = java.awt.GridBagConstraints.HORIZONTAL;
        gridBagConstraints.weightx = 1.0;
        gridBagConstraints.insets = new java.awt.Insets(0, 0, 0, 8);
        getContentPane().add(cmbAuthMode, gridBagConstraints);
        lblAuthMode.setText("Authentication Mode:");
        gridBagConstraints = new java.awt.GridBagConstraints();
        gridBagConstraints.gridx = 0;
        gridBagConstraints.gridy = 2;
        gridBagConstraints.anchor = java.awt.GridBagConstraints.EAST;
        gridBagConstraints.insets = new java.awt.Insets(0, 8, 0, 5);
        getContentPane().add(lblAuthMode, gridBagConstraints);
        java.awt.Dimension screenSize = java.awt.Toolkit.getDefaultToolkit().getScreenSize();
        setBounds((screenSize.width - 644) / 2, (screenSize.height - 351) / 2, 644, 351);
    }

    private void cancelButtonActionPerformed(java.awt.event.ActionEvent evt) {
        this.setVisible(false);
        this.dispose();
    }

    private void okButtonActionPerformed(java.awt.event.ActionEvent evt) {
        boolean isValid = true;
        for (JComponent component : componentsToVerifyOnOk) {
            if (!component.getInputVerifier().verify(component)) {
                isValid = false;
            }
        }
        if (!isValid) {
            return;
        }
        File applicationPropertiesFile = new File(Main.APPLICATION_PROPERTIES_FILENAME);
        try {
            HudsonConfig hudsonConfig = new HudsonConfig();
            hudsonConfig.setHudsonUrl(txtURL.getText());
            hudsonConfig.setMaxRSSEntriesToRead(txtMaxRssToRead.getText());
            hudsonConfig.setRefreshTime(txtRefreshTime.getText());
            hudsonConfig.setTimeout(txtTimeout.getText());
            hudsonConfig.setShouldPlaySounds(chkEnableSounds.isSelected());
            hudsonConfig.setUsername(txtUserName.getText());
            hudsonConfig.setPassword(new String(txtPassword.getPassword()));
            hudsonConfig.setAuthenticationMode((AuthenticationMode) cmbAuthMode.getSelectedItem());
            hudsonConfig.writeConfigToFile(applicationPropertiesFile);
            this.setVisible(false);
            this.dispose();
            if (hudsonTrackerTray != null) {
                hudsonTrackerTray.setHudsonRssConfig(hudsonConfig);
            } else {
                Main.main(null);
            }
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    private void testConnectionButtonActionPerformed(java.awt.event.ActionEvent evt) {
        if (!txtURL.getInputVerifier().verify(txtURL)) {
            return;
        }
        SyndFeedInput input = new SyndFeedInput();
        try {
            URL url = new URL(txtURL.getText() + "/rssAll");
            URLConnection urlConnection = url.openConnection();
            urlConnection.setConnectTimeout(5000);
            input.build(new XmlReader(urlConnection));
        } catch (Exception e) {
            JOptionPane.showMessageDialog(this, Messages.getString("ht.config.testUrl.failed") + txtURL.getText() + "/rssAll" + "'\n" + e.getLocalizedMessage(), Messages.getString("ht.config.testUrl.failed.title"), JOptionPane.ERROR_MESSAGE);
            e.printStackTrace();
            return;
        }
        JOptionPane.showMessageDialog(this, Messages.getString("ht.config.testUrl.success") + txtURL.getText() + "/rssAll", Messages.getString("ht.config.testUrl.success.title"), JOptionPane.INFORMATION_MESSAGE);
    }

    private javax.swing.JButton cancelButton;

    private javax.swing.JCheckBox chkEnableSounds;

    private javax.swing.JComboBox cmbAuthMode;

    private javax.swing.JLabel lblAuthMode;

    private javax.swing.JLabel lblConnectionTimeout;

    private javax.swing.JLabel lblEnableSounds;

    private javax.swing.JLabel lblHeading;

    private javax.swing.JLabel lblMaxRssToRead;

    private javax.swing.JLabel lblPassword;

    private javax.swing.JLabel lblRefreshTime;

    private javax.swing.JLabel lblUrl;

    private javax.swing.JLabel lblUsername;

    private javax.swing.JButton okButton;

    private javax.swing.JButton testConnectionButton;

    private javax.swing.JTextField txtMaxRssToRead;

    private javax.swing.JPasswordField txtPassword;

    private javax.swing.JTextField txtRefreshTime;

    private javax.swing.JTextField txtTimeout;

    private javax.swing.JTextField txtURL;

    private javax.swing.JTextField txtUserName;
}
