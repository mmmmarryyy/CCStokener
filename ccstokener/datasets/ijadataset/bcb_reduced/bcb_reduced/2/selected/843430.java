package boardgamebox.swingui;

import boardgamebox.swingui.util.ComponentFactory;
import javax.swing.*;
import java.awt.*;
import java.awt.event.ActionListener;
import java.awt.event.ActionEvent;
import java.awt.event.ItemListener;
import java.awt.event.ItemEvent;
import java.util.ArrayList;
import java.util.Map;
import java.net.URL;
import java.net.URLConnection;
import java.io.*;

/**
 * A dialog used to enter parameters for connection to a server
 * $Id: ConnectParametersDialog.java,v 1.3 2009/02/21 16:54:36 larsdam Exp $
 */
public class ConnectParametersDialog extends JDialog {

    private static String serverListUrl = "http://26396067.dk/boardgamebox/servers.txt";

    private static java.util.List<ServerInfo> serverInfos = new ArrayList<ServerInfo>();

    private JPanel fieldPanel = ComponentFactory.createPanel();

    private JLabel serverLabel = ComponentFactory.createLabel("Server");

    private JButton updateServerListButton = ComponentFactory.createSmallButton("Update server list");

    private JComboBox serverComboBox = ComponentFactory.createComboBox();

    private JLabel portLabel = ComponentFactory.createLabel("port");

    private JTextField portTextField = ComponentFactory.createTextField("2005");

    private JLabel userLabel = ComponentFactory.createLabel("User");

    private JTextField userTextField = ComponentFactory.createTextField();

    private JLabel passwordLabel = ComponentFactory.createLabel("Password");

    private JPasswordField passwordPasswordField = ComponentFactory.createPasswordField();

    private JCheckBox createAccountCheckBox = ComponentFactory.createCheckBox("Create account");

    private JPanel buttonsPanel = ComponentFactory.createPanel();

    private JButton okButton = ComponentFactory.createButton("Ok");

    private JButton cancelButton = ComponentFactory.createButton("Cancel");

    private boolean ok;

    public ConnectParametersDialog(Frame jFrame) {
        super(jFrame);
        getContentPane().setLayout(new BorderLayout());
        getContentPane().add(BorderLayout.CENTER, fieldPanel);
        getContentPane().add(BorderLayout.SOUTH, buttonsPanel);
        fieldPanel.setLayout(new GridBagLayout());
        fieldPanel.add(serverLabel, new GridBagConstraints(0, 0, 1, 1, 0.0, 0.0, GridBagConstraints.WEST, GridBagConstraints.NONE, new Insets(2, 4, 2, 4), 0, 0));
        fieldPanel.add(serverComboBox, new GridBagConstraints(1, 0, 1, 1, 1.0, 0.0, GridBagConstraints.WEST, GridBagConstraints.NONE, new Insets(2, 4, 2, 4), 0, 0));
        fieldPanel.add(updateServerListButton, new GridBagConstraints(2, 0, 1, 1, 1.0, 0.0, GridBagConstraints.WEST, GridBagConstraints.VERTICAL, new Insets(2, 4, 2, 4), 0, 0));
        fieldPanel.add(portLabel, new GridBagConstraints(0, 1, 1, 1, 0.0, 0.0, GridBagConstraints.WEST, GridBagConstraints.NONE, new Insets(2, 4, 2, 4), 0, 0));
        fieldPanel.add(portTextField, new GridBagConstraints(1, 1, 1, 1, 1.0, 0.0, GridBagConstraints.WEST, GridBagConstraints.NONE, new Insets(2, 4, 2, 4), 0, 0));
        fieldPanel.add(userLabel, new GridBagConstraints(0, 2, 1, 1, 0.0, 0.0, GridBagConstraints.WEST, GridBagConstraints.NONE, new Insets(2, 4, 2, 4), 0, 0));
        fieldPanel.add(userTextField, new GridBagConstraints(1, 2, 2, 1, 1.0, 0.0, GridBagConstraints.WEST, GridBagConstraints.NONE, new Insets(2, 4, 2, 4), 0, 0));
        fieldPanel.add(passwordLabel, new GridBagConstraints(0, 3, 1, 1, 0.0, 0.0, GridBagConstraints.NORTHWEST, GridBagConstraints.NONE, new Insets(2, 4, 2, 4), 0, 0));
        fieldPanel.add(passwordPasswordField, new GridBagConstraints(1, 3, 2, 1, 1.0, 0.0, GridBagConstraints.NORTHWEST, GridBagConstraints.NONE, new Insets(2, 4, 2, 4), 0, 0));
        fieldPanel.add(createAccountCheckBox, new GridBagConstraints(1, 4, 1, 1, 1.0, 1.0, GridBagConstraints.NORTHWEST, GridBagConstraints.NONE, new Insets(2, 4, 2, 4), 0, 0));
        buttonsPanel.setLayout(new FlowLayout(FlowLayout.RIGHT));
        buttonsPanel.add(okButton);
        buttonsPanel.add(cancelButton);
        portTextField.setColumns(6);
        userTextField.setColumns(20);
        passwordPasswordField.setColumns(20);
        setTitle("Enter connection parameters");
        okButton.addActionListener(okActionListener);
        cancelButton.addActionListener(cancelActionListener);
        setModal(true);
        serverComboBox.setEditable(true);
        serverInfos.add(new ServerInfo("localhost", 2005));
        updateServerComboBox();
        okButton.setMnemonic('o');
        cancelButton.setMnemonic('c');
        createAccountCheckBox.setMnemonic('a');
        serverLabel.setDisplayedMnemonic('s');
        serverLabel.setLabelFor(serverComboBox);
        portLabel.setDisplayedMnemonic('p');
        portLabel.setLabelFor(portTextField);
        userLabel.setDisplayedMnemonic('u');
        userLabel.setLabelFor(userTextField);
        passwordLabel.setDisplayedMnemonic('w');
        passwordLabel.setLabelFor(passwordPasswordField);
        updateServerListButton.addActionListener(new ActionListener() {

            public void actionPerformed(ActionEvent e) {
                updateServerList();
            }
        });
        serverComboBox.addItemListener(new ItemListener() {

            public void itemStateChanged(ItemEvent e) {
                Object o = e.getItem();
                if (o instanceof ServerInfo) {
                    portTextField.setText("" + ((ServerInfo) o).port);
                }
            }
        });
        okButton.setDefaultCapable(true);
        getRootPane().setDefaultButton(okButton);
    }

    public boolean invoke() {
        createAccountCheckBox.setSelected(false);
        ok = false;
        pack();
        setLocationRelativeTo(getParent());
        userTextField.requestFocus();
        setVisible(true);
        return ok;
    }

    public void setUiSettings(Map<String, Object> settings) {
        Object si = settings.get("serverlist");
        if (si instanceof java.util.List) {
            serverInfos = (java.util.List<ServerInfo>) si;
        }
    }

    public void getUiSettings(Map<String, Object> settings) {
        settings.put("serverlist", serverInfos);
    }

    public String getServer() {
        Object o = serverComboBox.getSelectedItem();
        return o instanceof ServerInfo ? ((ServerInfo) o).server : o.toString();
    }

    public int getPort() {
        try {
            return Integer.parseInt(portTextField.getText());
        } catch (NumberFormatException e) {
            portTextField.setText("0");
            return 0;
        }
    }

    public String getUser() {
        return userTextField.getText();
    }

    public String getPassword() {
        return new String(passwordPasswordField.getPassword());
    }

    public boolean isCreateAccount() {
        return createAccountCheckBox.isSelected();
    }

    private ActionListener okActionListener = new ActionListener() {

        public void actionPerformed(ActionEvent e) {
            ok = true;
            setVisible(false);
        }
    };

    private ActionListener cancelActionListener = new ActionListener() {

        public void actionPerformed(ActionEvent e) {
            setVisible(false);
        }
    };

    private void updateServerList() {
        String listUrl = JOptionPane.showInputDialog(this, "Enter listUrl for the server list file", serverListUrl);
        if (listUrl != null) {
            try {
                ArrayList<ServerInfo> servers = new ArrayList<ServerInfo>();
                URL url = new URL(listUrl);
                URLConnection conn = url.openConnection();
                InputStream in = conn.getInputStream();
                Reader reader = new InputStreamReader(in);
                BufferedReader br = new BufferedReader(reader);
                String line = br.readLine();
                while (line != null && !"---".equals(line)) {
                    String[] s = line.split(":| ");
                    int port = Integer.parseInt(s[1]);
                    servers.add(new ServerInfo(s[0], port));
                    line = br.readLine();
                }
                in.close();
                reader.close();
                br.close();
                serverInfos = servers;
                updateServerComboBox();
            } catch (IOException e) {
                JOptionPane.showMessageDialog(this, "Can not load server list: " + e.getMessage(), "Error", JOptionPane.ERROR_MESSAGE);
            }
        }
    }

    private void updateServerComboBox() {
        serverComboBox.removeAllItems();
        for (ServerInfo serverInfo : serverInfos) {
            serverComboBox.addItem(serverInfo);
        }
        if (serverInfos.size() > 0) {
            serverComboBox.setSelectedIndex(0);
        }
    }

    private static class ServerInfo {

        public final String server;

        public final int port;

        private ServerInfo(String server, int port) {
            this.server = server;
            this.port = port;
        }

        public String toString() {
            return server;
        }
    }
}
