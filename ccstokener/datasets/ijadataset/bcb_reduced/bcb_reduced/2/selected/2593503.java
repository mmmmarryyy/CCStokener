package org.chartsy.main.actions;

import java.util.ArrayList;
import java.util.Calendar;
import java.util.List;
import javax.swing.JOptionPane;
import org.apache.http.HttpEntity;
import org.apache.http.HttpResponse;
import org.apache.http.NameValuePair;
import org.apache.http.client.entity.UrlEncodedFormEntity;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.message.BasicNameValuePair;
import org.apache.http.util.EntityUtils;
import org.chartsy.main.managers.ProxyManager;
import org.chartsy.main.util.DesktopUtil;
import org.chartsy.main.util.StringUtil;
import org.openide.util.NbBundle;

/**
 *
 * @author Viorel
 */
public class RegisterPanel extends javax.swing.JPanel {

    public RegisterPanel() {
        initComponents();
        messageLbl.setVisible(false);
    }

    public boolean register() {
        if (usernameTxt.getText().isEmpty()) {
            JOptionPane.showMessageDialog(this, NbBundle.getMessage(RegisterPanel.class, "RegisterPanel.emptyUsername.message"), NbBundle.getMessage(RegisterPanel.class, "RegisterPanel.emptyUsername.title"), JOptionPane.WARNING_MESSAGE);
            return false;
        }
        if (passwordTxt.getPassword().length == 0) {
            JOptionPane.showMessageDialog(this, NbBundle.getMessage(RegisterPanel.class, "RegisterPanel.emptyPassword.message"), NbBundle.getMessage(RegisterPanel.class, "RegisterPanel.emptyPassword.title"), JOptionPane.WARNING_MESSAGE);
            return false;
        }
        String username = usernameTxt.getText();
        String password = StringUtil.md5(new String(passwordTxt.getPassword()));
        checkMrSwingRegistration(username, password);
        checkStockScanPRORegistration(username, password);
        checkChartsyRegistration(username, password);
        return RegisterAction.preferences.getBoolean("registred", false);
    }

    private void checkChartsyRegistration(String username, String password) {
        HttpPost post = new HttpPost(NbBundle.getMessage(RegisterPanel.class, "RegisterPanel.chartsyRegisterURL"));
        String message = "";
        try {
            List<NameValuePair> nvps = new ArrayList<NameValuePair>();
            nvps.add(new BasicNameValuePair("username", username));
            nvps.add(new BasicNameValuePair("password", password));
            post.setEntity(new UrlEncodedFormEntity(nvps));
            HttpResponse response = ProxyManager.httpClient.execute(post);
            HttpEntity entity = response.getEntity();
            if (entity != null) {
                String[] lines = EntityUtils.toString(entity).split("\n");
                if (lines[0].equals("OK")) {
                    RegisterAction.preferences.putBoolean("registred", true);
                    RegisterAction.preferences.put("name", lines[1]);
                    RegisterAction.preferences.put("email", lines[2]);
                    RegisterAction.preferences.put("date", String.valueOf(Calendar.getInstance().getTimeInMillis()));
                    RegisterAction.preferences.put("username", username);
                    RegisterAction.preferences.put("password", new String(passwordTxt.getPassword()));
                    if (lines[1] != null && !lines[1].isEmpty()) {
                        message = NbBundle.getMessage(RegisterPanel.class, "RegisterPanel.registerDone.withUsername.message", lines[1]);
                    } else {
                        message = NbBundle.getMessage(RegisterPanel.class, "RegisterPanel.registerDone.noUsername.message");
                    }
                } else {
                    message = NbBundle.getMessage(RegisterPanel.class, "RegisterPanel.registerAuthError.message");
                }
                EntityUtils.consume(entity);
            }
        } catch (Exception ex) {
            message = NbBundle.getMessage(RegisterPanel.class, "RegisterPanel.registerConnectionError.message");
        } finally {
            post.abort();
        }
        messageLbl.setText(message);
        messageLbl.setVisible(true);
    }

    private void checkMrSwingRegistration(String username, String password) {
    }

    private void checkStockScanPRORegistration(String username, String password) {
    }

    @SuppressWarnings("unchecked")
    private void initComponents() {
        topLbl = new javax.swing.JLabel();
        usernameLbl = new javax.swing.JLabel();
        passwordLbl = new javax.swing.JLabel();
        usernameTxt = new javax.swing.JTextField();
        passwordTxt = new javax.swing.JPasswordField();
        messageLbl = new javax.swing.JLabel();
        topLbl.setText(org.openide.util.NbBundle.getMessage(RegisterPanel.class, "RegisterPanel.topLbl.text"));
        topLbl.setVerticalAlignment(javax.swing.SwingConstants.TOP);
        topLbl.setCursor(new java.awt.Cursor(java.awt.Cursor.HAND_CURSOR));
        topLbl.addMouseListener(new java.awt.event.MouseAdapter() {

            public void mouseClicked(java.awt.event.MouseEvent evt) {
                topLblMouseClicked(evt);
            }
        });
        usernameLbl.setFont(new java.awt.Font("Tahoma", 1, 11));
        usernameLbl.setLabelFor(usernameTxt);
        usernameLbl.setText(org.openide.util.NbBundle.getMessage(RegisterPanel.class, "RegisterPanel.usernameLbl.text"));
        passwordLbl.setFont(new java.awt.Font("Tahoma", 1, 11));
        passwordLbl.setLabelFor(passwordTxt);
        passwordLbl.setText(org.openide.util.NbBundle.getMessage(RegisterPanel.class, "RegisterPanel.passwordLbl.text"));
        usernameTxt.setText(org.openide.util.NbBundle.getMessage(RegisterPanel.class, "RegisterPanel.usernameTxt.text"));
        passwordTxt.setText(org.openide.util.NbBundle.getMessage(RegisterPanel.class, "RegisterPanel.passwordTxt.text"));
        messageLbl.setText(org.openide.util.NbBundle.getMessage(RegisterPanel.class, "RegisterPanel.messageLbl.text"));
        messageLbl.setVerticalAlignment(javax.swing.SwingConstants.TOP);
        javax.swing.GroupLayout layout = new javax.swing.GroupLayout(this);
        this.setLayout(layout);
        layout.setHorizontalGroup(layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING).addGroup(javax.swing.GroupLayout.Alignment.TRAILING, layout.createSequentialGroup().addContainerGap().addGroup(layout.createParallelGroup(javax.swing.GroupLayout.Alignment.TRAILING).addComponent(messageLbl, javax.swing.GroupLayout.Alignment.LEADING, javax.swing.GroupLayout.DEFAULT_SIZE, 455, Short.MAX_VALUE).addComponent(topLbl, javax.swing.GroupLayout.Alignment.LEADING, javax.swing.GroupLayout.DEFAULT_SIZE, 455, Short.MAX_VALUE).addGroup(javax.swing.GroupLayout.Alignment.LEADING, layout.createSequentialGroup().addGroup(layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING).addComponent(passwordLbl).addComponent(usernameLbl)).addGap(32, 32, 32).addGroup(layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING).addComponent(usernameTxt, javax.swing.GroupLayout.DEFAULT_SIZE, 362, Short.MAX_VALUE).addComponent(passwordTxt, javax.swing.GroupLayout.DEFAULT_SIZE, 362, Short.MAX_VALUE)))).addContainerGap()));
        layout.setVerticalGroup(layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING).addGroup(layout.createSequentialGroup().addContainerGap().addComponent(topLbl, javax.swing.GroupLayout.PREFERRED_SIZE, 52, javax.swing.GroupLayout.PREFERRED_SIZE).addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.UNRELATED).addGroup(layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING).addGroup(layout.createSequentialGroup().addGap(32, 32, 32).addComponent(passwordLbl)).addGroup(layout.createSequentialGroup().addGroup(layout.createParallelGroup(javax.swing.GroupLayout.Alignment.BASELINE).addComponent(usernameTxt, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE).addComponent(usernameLbl)).addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.UNRELATED).addComponent(passwordTxt, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE))).addGap(18, 18, 18).addComponent(messageLbl).addContainerGap(javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE)));
    }

    private void topLblMouseClicked(java.awt.event.MouseEvent evt) {
        DesktopUtil.browseAndWarn(NbBundle.getMessage(RegisterPanel.class, "RegisterPanel.signupURL"), this);
    }

    private javax.swing.JLabel messageLbl;

    private javax.swing.JLabel passwordLbl;

    private javax.swing.JPasswordField passwordTxt;

    private javax.swing.JLabel topLbl;

    private javax.swing.JLabel usernameLbl;

    private javax.swing.JTextField usernameTxt;
}
