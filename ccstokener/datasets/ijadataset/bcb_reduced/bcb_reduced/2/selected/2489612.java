package edu.unibi.agbi.biodwh.gui.settings;

import java.awt.Color;
import java.awt.Font;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.io.BufferedInputStream;
import java.io.IOException;
import java.net.URL;
import javax.swing.ButtonGroup;
import javax.swing.DefaultComboBoxModel;
import javax.swing.JButton;
import javax.swing.JComboBox;
import javax.swing.JComponent;
import javax.swing.JDialog;
import javax.swing.JLabel;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JRadioButton;
import javax.swing.JTextField;
import net.miginfocom.swing.MigLayout;
import org.jdesktop.jxlayer.JXLayer;
import org.jdesktop.jxlayer.plaf.effect.BufferedImageOpEffect;
import org.jdesktop.jxlayer.plaf.ext.LockableUI;
import org.jdesktop.swingx.JXHyperlink;
import org.jdesktop.swingx.JXPanel;
import com.jhlabs.image.BlurFilter;
import edu.unibi.agbi.biodwh.config.BioDWHSettings;
import edu.unibi.agbi.biodwh.config.IconLibrary;
import edu.unibi.agbi.biodwh.config.ResourceLibrary;
import edu.unibi.agbi.biodwh.config.xml.SettingsWriter;
import edu.unibi.agbi.biodwh.gui.Manager;

/**
 * @author Benjamin Kormeier
 * @version 1.0 19.11.2008
 */
public class ProxySettingsDialog extends MouseAdapter implements ActionListener {

    private JDialog dialog = new JDialog();

    private JPanel root = new JPanel();

    private JXLayer<JComponent> layer = new JXLayer<JComponent>(root);

    private LockableUI blurUI = new LockableUI(new BufferedImageOpEffect(new BlurFilter()));

    private JPanel glass_pane = new JPanel();

    private JRadioButton proxy_on = new JRadioButton(ResourceLibrary.getSettingsResource("settings.proxy.on"));

    private JRadioButton proxy_off = new JRadioButton(ResourceLibrary.getSettingsResource("settings.proxy.off"));

    private ButtonGroup proxy_group = new ButtonGroup();

    private JLabel proxy_http_label = new JLabel(ResourceLibrary.getSettingsResource("settings.proxy.http"));

    private JLabel proxy_ftp = new JLabel(ResourceLibrary.getSettingsResource("settings.proxy.ftp"));

    private JLabel proxy_ftp_class = new JLabel(ResourceLibrary.getSettingsResource("settings.proxy.ftp.class"));

    private DefaultComboBoxModel proxy_ftp_class_model = new DefaultComboBoxModel();

    private JComboBox proxy_ftp_class_box = new JComboBox(proxy_ftp_class_model);

    private JLabel proxy_ftp_class_info = new JLabel(IconLibrary.INFO_ICON_MENU);

    private JTextField proxy_http_field = new JTextField(20);

    private JTextField proxy_ftp_field = new JTextField(20);

    private JTextField proxy_http_port_field = new JTextField(4);

    private JTextField proxy_ftp_port_field = new JTextField(4);

    private JButton apply_button = new JButton(ResourceLibrary.getSettingsResource("settings.proxy.apply"));

    private JButton cancel_button = new JButton(ResourceLibrary.getSettingsResource("settings.proxy.cancel"));

    private String no_proxy_class_item = ResourceLibrary.getSettingsResource("settings.proxy.ftp.class.item");

    public ProxySettingsDialog() {
        dialog = new JDialog(Manager.getFrame());
        init();
        makeGlassPaneInfo();
    }

    public ProxySettingsDialog(JDialog parent) {
        dialog = new JDialog(parent);
        init();
        makeGlassPaneInfo();
    }

    private void init() {
        dialog.setGlassPane(glass_pane);
        dialog.add(layer);
        layer.setUI(blurUI);
        proxy_group.add(proxy_off);
        proxy_group.add(proxy_on);
        proxy_off.addActionListener(this);
        proxy_on.addActionListener(this);
        root.setLayout(new MigLayout("left"));
        root.add(proxy_off, "wrap");
        root.add(proxy_on, "wrap");
        root.add(proxy_http_label);
        root.add(proxy_http_field, "growx");
        root.add(new JLabel(":"));
        root.add(proxy_http_port_field, "wrap");
        root.add(proxy_ftp);
        root.add(proxy_ftp_field, "growx");
        root.add(new JLabel(":"));
        root.add(proxy_ftp_port_field, "wrap");
        root.add(proxy_ftp_class);
        root.add(proxy_ftp_class_box, "growx");
        root.add(proxy_ftp_class_info, "skip 1, wrap");
        addProxyClasses();
        root.add(apply_button, "cell 1 5, center");
        root.add(cancel_button, "cell 1 5, center");
        proxy_ftp_class_info.addMouseListener(this);
        apply_button.addActionListener(this);
        cancel_button.addActionListener(this);
        checkSystemProxy();
        dialog.setTitle("Proxy settings...");
        dialog.pack();
        dialog.setVisible(true);
        dialog.setDefaultCloseOperation(JDialog.DO_NOTHING_ON_CLOSE);
        dialog.setLocationRelativeTo(Manager.getFrame());
    }

    private void makeGlassPaneInfo() {
        JXPanel content = new JXPanel();
        JXHyperlink close_link = new JXHyperlink();
        JLabel information = new JLabel(IconLibrary.INFO_ICON, JLabel.LEFT);
        Color link_color = new Color(165, 179, 136);
        Font font = new Font(close_link.getFont().getFamily(), Font.BOLD, 14);
        ClassLoader loader = ClassLoader.getSystemClassLoader();
        URL url = loader.getResource("resource/proxy_information.html");
        content.setLayout(new MigLayout("fill"));
        content.setBackground(new Color(0, 0, 0, 70));
        try {
            information.setText(readInformationURL(url));
        } catch (IOException e) {
            e.printStackTrace();
        }
        close_link.setOpaque(false);
        close_link.setFont(font);
        close_link.setText(ResourceLibrary.getManagerResource("glass.about.close"));
        close_link.setForeground(link_color);
        close_link.setClickedColor(link_color);
        content.add(information, "wrap");
        content.add(close_link, "center");
        close_link.addMouseListener(this);
        glass_pane.setOpaque(false);
        glass_pane.setLayout(new MigLayout("fill"));
        glass_pane.add(content, "center");
    }

    private String readInformationURL(URL url) throws IOException {
        BufferedInputStream is = (BufferedInputStream) url.openStream();
        StringBuffer text = new StringBuffer();
        int read = 0;
        while ((read = is.read()) != -1) {
            text.append((char) read);
        }
        is.close();
        return text.toString();
    }

    private void setProxyClassInfo(boolean visible) {
        glass_pane.setVisible(visible);
        blurUI.setLocked(visible);
    }

    private void checkSystemProxy() {
        boolean proxySet = BioDWHSettings.isProxy();
        if (!proxySet) {
            proxy_off.setSelected(true);
            setEnabledProxySettings(false);
        } else {
            proxy_on.setSelected(true);
            setEnabledProxySettings(true);
            proxy_http_field.setText(BioDWHSettings.getProxyHTTP());
            proxy_http_port_field.setText(BioDWHSettings.getProxyHTTP_Port());
            proxy_ftp_field.setText(BioDWHSettings.getProxyFTP());
            proxy_ftp_port_field.setText(BioDWHSettings.getProxyFTP_Port());
            if (BioDWHSettings.useProxyClass()) {
                proxy_ftp_class_model.setSelectedItem(BioDWHSettings.getProxyClass());
            } else {
                proxy_ftp_class_model.setSelectedItem(no_proxy_class_item);
            }
        }
    }

    private void setEnabledProxySettings(boolean enabled) {
        proxy_http_label.setEnabled(enabled);
        proxy_http_field.setEnabled(enabled);
        proxy_http_port_field.setEnabled(enabled);
        proxy_ftp.setEnabled(enabled);
        proxy_ftp_field.setEnabled(enabled);
        proxy_ftp_port_field.setEnabled(enabled);
    }

    private void setSystemProxy() {
        if (proxy_on.isSelected()) {
            BioDWHSettings.setProxy(true);
            BioDWHSettings.setProxyHTTP(proxy_http_field.getText());
            BioDWHSettings.setProxyHTTP_Port(proxy_http_port_field.getText());
            BioDWHSettings.setProxyFTP(proxy_ftp_field.getText());
            BioDWHSettings.setProxyFTP_Port(proxy_ftp_port_field.getText());
            String proxy_class = (String) proxy_ftp_class_model.getSelectedItem();
            if (proxy_class.equals(no_proxy_class_item)) {
                BioDWHSettings.setUseProxyClass(false);
                BioDWHSettings.setProxyClass(null);
            } else {
                BioDWHSettings.setUseProxyClass(true);
                BioDWHSettings.setProxyClass(proxy_class);
            }
        } else {
            BioDWHSettings.setProxy(false);
        }
        try {
            new SettingsWriter();
        } catch (Exception e) {
            JOptionPane.showMessageDialog(dialog, ResourceLibrary.getSettingsResource("settings.proxy.error.save.text") + ResourceLibrary.getSettingsResource("settings.file"), ResourceLibrary.getSettingsResource("settings.proxy.error.save.title"), JOptionPane.ERROR_MESSAGE, IconLibrary.QUESTION_ICON);
        }
    }

    private void addProxyClasses() {
        String[] proxy_classes = ResourceLibrary.getSettingsResource("settings.proxy.classes").split(";");
        proxy_ftp_class_model.addElement(no_proxy_class_item);
        if (proxy_classes != null) for (String proxy_class : proxy_classes) {
            proxy_ftp_class_model.addElement(proxy_class);
        }
    }

    public void actionPerformed(ActionEvent e) {
        String command = e.getActionCommand();
        if (command.equals(ResourceLibrary.getSettingsResource("settings.proxy.on"))) {
            setEnabledProxySettings(true);
            if (BioDWHSettings.useProxyClass()) {
                proxy_ftp_class_model.setSelectedItem(BioDWHSettings.getProxyClass());
            } else {
                proxy_ftp_class_model.setSelectedItem(no_proxy_class_item);
            }
        } else if (command.equals(ResourceLibrary.getSettingsResource("settings.proxy.off"))) setEnabledProxySettings(false); else if (command.equals(ResourceLibrary.getSettingsResource("settings.proxy.apply"))) {
            setSystemProxy();
            JOptionPane.showMessageDialog(dialog, ResourceLibrary.getSettingsResource("settings.proxy.info.save.text"), ResourceLibrary.getSettingsResource("settings.proxy.info.save.title"), JOptionPane.INFORMATION_MESSAGE, IconLibrary.QUESTION_ICON);
            dialog.dispose();
        } else if (command.equals(ResourceLibrary.getSettingsResource("settings.proxy.cancel"))) {
            dialog.dispose();
        }
    }

    /** 
	 * @see java.awt.event.MouseAdapter#mouseClicked(java.awt.event.MouseEvent)
	 */
    @Override
    public void mouseClicked(MouseEvent e) {
        if (e.getSource() instanceof JLabel) {
            setProxyClassInfo(true);
        } else if (e.getSource() instanceof JXHyperlink) {
            setProxyClassInfo(false);
        }
    }
}
