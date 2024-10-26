package com.rlsoftwares.virtualdeck.dialogs;

import com.rlsoftwares.util.BrowserControl;
import com.rlsoftwares.util.ResourceUtils;
import com.rlsoftwares.virtualdeck.VirtualDeckGUI;
import com.rlsoftwares.virtualdeck.config.VirtualDeckConfig;
import com.rlsoftwares.virtualdeck.config.ClientConfigurations;
import com.rlsoftwares.virtualdeck.network.VirtualDeckClient;
import com.rlsoftwares.virtualdeck.network.messages.MessageAuthentication;
import java.awt.Cursor;
import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.net.MalformedURLException;
import java.net.URL;
import java.util.ArrayList;
import javax.swing.JOptionPane;

/**
 *
 * @author  Rodrigo
 */
public class Authentication extends javax.swing.JDialog {

    class Host {

        private String ip;

        private String name;

        private Integer port;

        public String getName() {
            return name;
        }

        public void setName(String name) {
            this.name = name;
        }

        public Integer getPort() {
            return port;
        }

        public void setPort(Integer port) {
            this.port = port;
        }

        public String getIp() {
            return ip;
        }

        public void setIp(String ip) {
            this.ip = ip;
        }
    }

    private boolean authenticated;

    private ArrayList<Host> servers = new ArrayList<Host>();

    /** Creates new form Authentication */
    public Authentication(java.awt.Frame parent, boolean modal) {
        super(parent, modal);
        initComponents();
        VirtualDeckGUI virtualDeckGUI = (VirtualDeckGUI) parent;
        String id = virtualDeckGUI.getProfile().getProperty("id");
        String ip = virtualDeckGUI.getProfile().getProperty("target_ip");
        String port = virtualDeckGUI.getProfile().getProperty("target_port");
        if (id != null) {
            txtId.setText(id);
        }
        if (ip != null) {
            txtIP.setText(ip);
        }
        if (port != null) {
            txtPort.setText(port);
        }
        lblLogo.setCursor(new Cursor(Cursor.HAND_CURSOR));
        loadServers();
        inputHandler();
    }

    private void initComponents() {
        jPanel1 = new javax.swing.JPanel();
        jLabel1 = new javax.swing.JLabel();
        txtId = new javax.swing.JTextField();
        btnAuth = new javax.swing.JButton();
        jScrollPane1 = new javax.swing.JScrollPane();
        txtLog = new javax.swing.JTextArea();
        lblLogo = new javax.swing.JLabel();
        jLabel2 = new javax.swing.JLabel();
        cmbServer = new javax.swing.JComboBox();
        txtIP = new javax.swing.JTextField();
        txtPort = new javax.swing.JTextField();
        jLabel3 = new javax.swing.JLabel();
        jLabel4 = new javax.swing.JLabel();
        setDefaultCloseOperation(javax.swing.WindowConstants.DISPOSE_ON_CLOSE);
        java.util.ResourceBundle bundle = java.util.ResourceBundle.getBundle("com/rlsoftwares/virtualdeck/config/virtualdeck");
        setTitle(bundle.getString("authentication"));
        setResizable(false);
        addWindowListener(new java.awt.event.WindowAdapter() {

            public void windowClosed(java.awt.event.WindowEvent evt) {
                formWindowClosed(evt);
            }
        });
        jPanel1.setMinimumSize(new java.awt.Dimension(400, 240));
        jPanel1.setPreferredSize(new java.awt.Dimension(400, 240));
        jLabel1.setFont(new java.awt.Font("Tahoma", 1, 11));
        jLabel1.setText(bundle.getString("nick"));
        btnAuth.setText(bundle.getString("enter"));
        btnAuth.addActionListener(new java.awt.event.ActionListener() {

            public void actionPerformed(java.awt.event.ActionEvent evt) {
                btnAuthActionPerformed(evt);
            }
        });
        jScrollPane1.setHorizontalScrollBarPolicy(javax.swing.ScrollPaneConstants.HORIZONTAL_SCROLLBAR_NEVER);
        jScrollPane1.setVerticalScrollBarPolicy(javax.swing.ScrollPaneConstants.VERTICAL_SCROLLBAR_NEVER);
        txtLog.setColumns(20);
        txtLog.setEditable(false);
        txtLog.setForeground(new java.awt.Color(153, 0, 0));
        txtLog.setLineWrap(true);
        txtLog.setRows(5);
        txtLog.setWrapStyleWord(true);
        jScrollPane1.setViewportView(txtLog);
        lblLogo.setIcon(new javax.swing.ImageIcon(getClass().getResource("/com/rlsoftwares/virtualdeck/icons/rlsoftwares.gif")));
        lblLogo.addMouseListener(new java.awt.event.MouseAdapter() {

            public void mousePressed(java.awt.event.MouseEvent evt) {
                lblLogoMousePressed(evt);
            }
        });
        jLabel2.setFont(new java.awt.Font("Tahoma", 1, 11));
        jLabel2.setText(bundle.getString("server"));
        cmbServer.addItemListener(new java.awt.event.ItemListener() {

            public void itemStateChanged(java.awt.event.ItemEvent evt) {
                cmbServerItemStateChanged(evt);
            }
        });
        txtIP.setText("localhost");
        txtIP.setEnabled(false);
        txtPort.setText("5000");
        txtPort.setEnabled(false);
        jLabel3.setFont(new java.awt.Font("Tahoma", 1, 11));
        jLabel3.setText(bundle.getString("ip"));
        jLabel4.setFont(new java.awt.Font("Tahoma", 1, 11));
        jLabel4.setText(bundle.getString("port"));
        org.jdesktop.layout.GroupLayout jPanel1Layout = new org.jdesktop.layout.GroupLayout(jPanel1);
        jPanel1.setLayout(jPanel1Layout);
        jPanel1Layout.setHorizontalGroup(jPanel1Layout.createParallelGroup(org.jdesktop.layout.GroupLayout.LEADING).add(jPanel1Layout.createSequentialGroup().add(18, 18, 18).add(lblLogo, org.jdesktop.layout.GroupLayout.PREFERRED_SIZE, 124, org.jdesktop.layout.GroupLayout.PREFERRED_SIZE).addPreferredGap(org.jdesktop.layout.LayoutStyle.RELATED).add(jPanel1Layout.createParallelGroup(org.jdesktop.layout.GroupLayout.LEADING).add(jPanel1Layout.createSequentialGroup().add(jLabel2).addContainerGap()).add(org.jdesktop.layout.GroupLayout.TRAILING, jPanel1Layout.createSequentialGroup().add(jPanel1Layout.createParallelGroup(org.jdesktop.layout.GroupLayout.TRAILING).add(org.jdesktop.layout.GroupLayout.LEADING, cmbServer, 0, 219, Short.MAX_VALUE).add(org.jdesktop.layout.GroupLayout.LEADING, jScrollPane1, org.jdesktop.layout.GroupLayout.DEFAULT_SIZE, 219, Short.MAX_VALUE).add(org.jdesktop.layout.GroupLayout.LEADING, jPanel1Layout.createSequentialGroup().add(jPanel1Layout.createParallelGroup(org.jdesktop.layout.GroupLayout.LEADING).add(txtIP, org.jdesktop.layout.GroupLayout.PREFERRED_SIZE, 146, org.jdesktop.layout.GroupLayout.PREFERRED_SIZE).add(jLabel3)).addPreferredGap(org.jdesktop.layout.LayoutStyle.RELATED).add(jPanel1Layout.createParallelGroup(org.jdesktop.layout.GroupLayout.LEADING).add(jPanel1Layout.createSequentialGroup().add(jLabel4).add(36, 36, 36)).add(txtPort, org.jdesktop.layout.GroupLayout.DEFAULT_SIZE, 67, Short.MAX_VALUE))).add(org.jdesktop.layout.GroupLayout.LEADING, jLabel1).add(org.jdesktop.layout.GroupLayout.LEADING, txtId, org.jdesktop.layout.GroupLayout.DEFAULT_SIZE, 219, Short.MAX_VALUE)).add(33, 33, 33)))).add(org.jdesktop.layout.GroupLayout.TRAILING, jPanel1Layout.createSequentialGroup().addContainerGap(206, Short.MAX_VALUE).add(btnAuth, org.jdesktop.layout.GroupLayout.PREFERRED_SIZE, 108, org.jdesktop.layout.GroupLayout.PREFERRED_SIZE).add(86, 86, 86)));
        jPanel1Layout.setVerticalGroup(jPanel1Layout.createParallelGroup(org.jdesktop.layout.GroupLayout.LEADING).add(jPanel1Layout.createSequentialGroup().addContainerGap().add(jPanel1Layout.createParallelGroup(org.jdesktop.layout.GroupLayout.LEADING).add(jPanel1Layout.createSequentialGroup().add(jLabel1).addPreferredGap(org.jdesktop.layout.LayoutStyle.RELATED).add(txtId, org.jdesktop.layout.GroupLayout.PREFERRED_SIZE, org.jdesktop.layout.GroupLayout.DEFAULT_SIZE, org.jdesktop.layout.GroupLayout.PREFERRED_SIZE).addPreferredGap(org.jdesktop.layout.LayoutStyle.RELATED).add(jLabel2).addPreferredGap(org.jdesktop.layout.LayoutStyle.RELATED).add(cmbServer, org.jdesktop.layout.GroupLayout.PREFERRED_SIZE, org.jdesktop.layout.GroupLayout.DEFAULT_SIZE, org.jdesktop.layout.GroupLayout.PREFERRED_SIZE).addPreferredGap(org.jdesktop.layout.LayoutStyle.RELATED, org.jdesktop.layout.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE).add(jPanel1Layout.createParallelGroup(org.jdesktop.layout.GroupLayout.LEADING).add(org.jdesktop.layout.GroupLayout.TRAILING, jPanel1Layout.createSequentialGroup().add(jLabel3).addPreferredGap(org.jdesktop.layout.LayoutStyle.RELATED).add(jPanel1Layout.createParallelGroup(org.jdesktop.layout.GroupLayout.BASELINE).add(txtIP, org.jdesktop.layout.GroupLayout.PREFERRED_SIZE, org.jdesktop.layout.GroupLayout.DEFAULT_SIZE, org.jdesktop.layout.GroupLayout.PREFERRED_SIZE).add(txtPort, org.jdesktop.layout.GroupLayout.PREFERRED_SIZE, org.jdesktop.layout.GroupLayout.DEFAULT_SIZE, org.jdesktop.layout.GroupLayout.PREFERRED_SIZE))).add(org.jdesktop.layout.GroupLayout.TRAILING, jPanel1Layout.createSequentialGroup().add(jLabel4).add(25, 25, 25))).add(8, 8, 8).add(jScrollPane1, org.jdesktop.layout.GroupLayout.PREFERRED_SIZE, 45, org.jdesktop.layout.GroupLayout.PREFERRED_SIZE)).add(lblLogo, org.jdesktop.layout.GroupLayout.PREFERRED_SIZE, 142, org.jdesktop.layout.GroupLayout.PREFERRED_SIZE)).addPreferredGap(org.jdesktop.layout.LayoutStyle.RELATED, 10, Short.MAX_VALUE).add(btnAuth).addContainerGap()));
        getContentPane().add(jPanel1, java.awt.BorderLayout.WEST);
        pack();
    }

    private void cmbServerItemStateChanged(java.awt.event.ItemEvent evt) {
        inputHandler();
    }

    private void lblLogoMousePressed(java.awt.event.MouseEvent evt) {
        BrowserControl.displayURL(this, VirtualDeckConfig.VIRTUAL_DECK_SITE);
    }

    private void formWindowClosed(java.awt.event.WindowEvent evt) {
        if (!isAuthenticated()) {
            System.exit(0);
        }
    }

    private void inputHandler() {
        boolean enabled = cmbServer.getSelectedIndex() != -1 && (cmbServer.getItemCount() - 1) == cmbServer.getSelectedIndex();
        txtIP.setEnabled(enabled);
        txtPort.setEnabled(enabled);
    }

    private void btnAuthActionPerformed(java.awt.event.ActionEvent evt) {
        if (!canAuth()) return;
        VirtualDeckGUI virtualDeckGUI = (VirtualDeckGUI) getParent();
        VirtualDeckClient c = new VirtualDeckClient(virtualDeckGUI);
        try {
            String server = "";
            Integer port = null;
            if (cmbServer.getSelectedIndex() != -1 && 0 == cmbServer.getSelectedIndex()) {
                server = txtIP.getText();
                port = Integer.parseInt(txtPort.getText());
            } else {
                server = getServers().get(cmbServer.getSelectedIndex() - 1).getIp();
                port = getServers().get(cmbServer.getSelectedIndex() - 1).getPort();
            }
            if (!c.connect(server, port)) {
                c = null;
            }
        } catch (Exception e) {
            c = null;
        }
        if (c != null) {
            virtualDeckGUI.setClient(c);
            MessageAuthentication ma = new MessageAuthentication();
            ma.setNick(txtId.getText());
            ma.setVersion(ClientConfigurations.VERSION_NUMBER);
            c.sendMessage(ma);
            txtLog.setText(ResourceUtils.getMessage(VirtualDeckConfig.TEXT_RESOURCE_PATH, "authenticating"));
            setCursor(new Cursor(Cursor.WAIT_CURSOR));
            virtualDeckGUI.getProfile().setProperty("id", txtId.getText());
            virtualDeckGUI.getProfile().setProperty("target_ip", txtIP.getText());
            virtualDeckGUI.getProfile().setProperty("target_port", txtPort.getText());
        } else {
            txtLog.setText(ResourceUtils.getMessage(VirtualDeckConfig.TEXT_RESOURCE_PATH, "connection_error_host_is_not_responding"));
        }
    }

    /**
     * @param args the command line arguments
     */
    public static void main(String args[]) {
        java.awt.EventQueue.invokeLater(new Runnable() {

            public void run() {
                new Authentication(new javax.swing.JFrame(), true).setVisible(true);
            }
        });
    }

    private boolean canAuth() {
        try {
            if (txtId.getText().trim().length() == 0) throw new Exception();
        } catch (Exception e) {
            JOptionPane.showMessageDialog(this, ResourceUtils.getMessage(VirtualDeckConfig.TEXT_RESOURCE_PATH, "invalid_id"), ResourceUtils.getMessage(VirtualDeckConfig.TEXT_RESOURCE_PATH, "warn"), JOptionPane.WARNING_MESSAGE);
            return false;
        }
        return true;
    }

    public void updateVersion() {
        if (JOptionPane.showConfirmDialog(this, ResourceUtils.getMessage(VirtualDeckConfig.TEXT_RESOURCE_PATH, "game_update_is_necessary_wish_you_download_new_version_now"), ResourceUtils.getMessage(VirtualDeckConfig.TEXT_RESOURCE_PATH, "question"), JOptionPane.YES_NO_OPTION, JOptionPane.QUESTION_MESSAGE) == JOptionPane.YES_OPTION) {
            BrowserControl.displayURL(this, VirtualDeckConfig.UPDATE_URL);
        }
        authenticationFailed(ResourceUtils.getMessage(VirtualDeckConfig.TEXT_RESOURCE_PATH, "game_update_is_necessary"));
    }

    public void authenticationFailed(String problem) {
        VirtualDeckGUI vdg = (VirtualDeckGUI) getParent();
        vdg.getClient().disconnect();
        txtLog.setText(problem);
        setCursor(new Cursor(Cursor.DEFAULT_CURSOR));
    }

    public boolean isAuthenticated() {
        return authenticated;
    }

    public void setAuthenticated(boolean authenticated) {
        this.authenticated = authenticated;
    }

    public ArrayList<Host> getServers() {
        return servers;
    }

    public void setServers(ArrayList<Host> servers) {
        this.servers = servers;
    }

    private void loadServers() {
        try {
            URL url = new URL(VirtualDeckConfig.SERVERS_URL);
            cmbServer.addItem("Local");
            BufferedReader in = new BufferedReader(new InputStreamReader(url.openStream()));
            String str;
            if (in.readLine().equals("[list]")) {
                while ((str = in.readLine()) != null) {
                    String[] host_line = str.split(";");
                    Host h = new Host();
                    h.setIp(host_line[0]);
                    h.setPort(Integer.parseInt(host_line[1]));
                    h.setName(host_line[2]);
                    getServers().add(h);
                    cmbServer.addItem(h.getName());
                }
            }
            in.close();
        } catch (MalformedURLException e) {
        } catch (IOException e) {
        }
    }

    private javax.swing.JButton btnAuth;

    private javax.swing.JComboBox cmbServer;

    private javax.swing.JLabel jLabel1;

    private javax.swing.JLabel jLabel2;

    private javax.swing.JLabel jLabel3;

    private javax.swing.JLabel jLabel4;

    private javax.swing.JPanel jPanel1;

    private javax.swing.JScrollPane jScrollPane1;

    private javax.swing.JLabel lblLogo;

    private javax.swing.JTextField txtIP;

    private javax.swing.JTextField txtId;

    private javax.swing.JTextArea txtLog;

    private javax.swing.JTextField txtPort;
}
