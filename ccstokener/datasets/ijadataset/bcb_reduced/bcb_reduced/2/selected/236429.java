package com.iver.core.preferences.network;

import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.net.URL;
import javax.swing.ImageIcon;
import javax.swing.JLabel;
import javax.swing.JPanel;
import com.iver.andami.PluginServices;
import com.iver.andami.preferences.AbstractPreferencePage;
import com.iver.andami.ui.mdiFrame.JToolBarButton;

/**
 * General network connection page.
 *
 * @author jaume dominguez faus - jaume.dominguez@iver.es
 *
 */
public class NetworkPage extends AbstractPreferencePage {

    private ImageIcon icon;

    private JLabel lblNetworkStatus;

    private JToolBarButton btnRefresh;

    protected static String id;

    public NetworkPage() {
        super();
        id = this.getClass().getName();
        icon = new ImageIcon(this.getClass().getClassLoader().getResource("images/network.png"));
        lblNetworkStatus = new JLabel();
        lblNetworkStatus.setFont(new java.awt.Font("Arial", java.awt.Font.BOLD, 12));
        lblNetworkStatus.setText(PluginServices.getText(this, "optinos.network.click_to_test_connection"));
        JPanel aux = new JPanel();
        aux.add(getBtnCheckConnection());
        addComponent(PluginServices.getText(this, "options.network.status") + ":", lblNetworkStatus);
        addComponent("", aux);
    }

    private JToolBarButton getBtnCheckConnection() {
        if (btnRefresh == null) {
            btnRefresh = new JToolBarButton(PluginServices.getText(this, "test_now"));
            btnRefresh.addActionListener(new ActionListener() {

                public void actionPerformed(java.awt.event.ActionEvent e) {
                    refreshStatus();
                }

                ;
            });
        }
        return btnRefresh;
    }

    private void refreshStatus() {
        boolean connected = isConnected();
        ImageIcon statusIcon;
        String statusText;
        if (connected) {
            statusIcon = new ImageIcon(this.getClass().getClassLoader().getResource("images/kde-network-online.png"));
            statusText = PluginServices.getText(this, "online");
        } else {
            statusIcon = new ImageIcon(this.getClass().getClassLoader().getResource("images/kde-network-offline.png"));
            statusText = PluginServices.getText(this, "offline");
        }
        lblNetworkStatus.setIcon(statusIcon);
        lblNetworkStatus.setText(statusText);
    }

    private boolean isConnected() {
        try {
            URL url = new URL("http://www.google.com");
            url.openConnection();
            url.openStream();
            return true;
        } catch (Exception e) {
            return false;
        }
    }

    public String getID() {
        return id;
    }

    public String getTitle() {
        return PluginServices.getText(this, "pref.network");
    }

    public JPanel getPanel() {
        return this;
    }

    public void initializeValues() {
    }

    public void storeValues() {
    }

    public void initializeDefaults() {
    }

    class ActionHandler implements ActionListener {

        public void actionPerformed(ActionEvent evt) {
        }
    }

    public ImageIcon getIcon() {
        return icon;
    }

    public boolean isValueChanged() {
        return hasChanged();
    }

    public void setChangesApplied() {
        setChanged(false);
    }
}
