package net.sourceforge.ubcdcreator;

import java.awt.BorderLayout;
import java.awt.Dimension;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.Insets;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.io.OutputStreamWriter;
import java.net.URL;
import java.net.URLConnection;
import java.net.URLEncoder;
import javax.swing.BorderFactory;
import javax.swing.JButton;
import javax.swing.JCheckBox;
import javax.swing.JDialog;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JTextArea;
import javax.swing.JTextField;

public class BugPanel extends JDialog implements ActionListener {

    private double bugVer = 1.0;

    private JTextField name = new JTextField();

    private JTextField os = new JTextField();

    private JTextField jre = new JTextField();

    private JTextField email = new JTextField();

    private JTextArea bugdesc = new JTextArea(10, 50);

    private JButton submitBtn = new JButton("Submit Bug");

    private JButton cancelBtn = new JButton("Cancel");

    public BugPanel(JFrame frame) {
        super(frame, "Report a Bug", true);
        String osStr1 = System.getProperty("os.name");
        if (osStr1 == null) osStr1 = "";
        String osStr2 = System.getProperty("os.arch");
        if (osStr1 == null) osStr1 = "";
        String osStr3 = System.getProperty("os.version");
        if (osStr1 == null) osStr1 = "";
        os.setText(osStr1 + " " + osStr2 + " " + osStr3);
        String jreStr1 = System.getProperty("java.vendor");
        if (jreStr1 == null) jreStr1 = "";
        String jreStr2 = System.getProperty("java.version");
        if (jreStr1 == null) jreStr1 = "";
        jre.setText(jreStr1 + " " + jreStr2);
        os.setEditable(false);
        jre.setEditable(false);
        JPanel panel = new JPanel();
        getContentPane().setLayout(new BorderLayout());
        getContentPane().add(panel, BorderLayout.CENTER);
        panel.setLayout(new GridBagLayout());
        GridBagConstraints c = new GridBagConstraints();
        c.fill = GridBagConstraints.HORIZONTAL;
        c.anchor = GridBagConstraints.CENTER;
        c.insets = new Insets(3, 3, 3, 3);
        c.gridheight = 1;
        c.gridwidth = 1;
        c.weighty = 0;
        c.weightx = 1;
        c.gridx = 0;
        c.gridy = 0;
        addRow(new JLabel("Name:"), name, panel, c);
        addRow(new JLabel("Operating System:"), os, panel, c);
        addRow(new JLabel("Java Version:"), jre, panel, c);
        addRow(new JLabel("Email:"), email, panel, c);
        c.gridwidth = 2;
        panel.add(new JLabel("Please describe the problem."), c);
        c.gridy++;
        bugdesc.setBorder(name.getBorder());
        panel.add(new JScrollPane(bugdesc, JScrollPane.VERTICAL_SCROLLBAR_AS_NEEDED, JScrollPane.HORIZONTAL_SCROLLBAR_NEVER), c);
        c.gridy++;
        panel.add(createButtonPanel(), c);
        Dimension d = new Dimension(600, 400);
        setMinimumSize(d);
        setMaximumSize(d);
        setPreferredSize(d);
        pack();
    }

    private void addRow(JLabel label, JTextField field, JPanel panel, GridBagConstraints c) {
        panel.add(label, c);
        c.gridx++;
        panel.add(field, c);
        c.gridy++;
        c.gridx = 0;
    }

    private JPanel createButtonPanel() {
        submitBtn.addActionListener(this);
        cancelBtn.addActionListener(this);
        submitBtn.setActionCommand("submit");
        cancelBtn.setActionCommand("cancel");
        JPanel panel = new JPanel();
        panel.setLayout(new GridBagLayout());
        GridBagConstraints c = new GridBagConstraints();
        c.fill = GridBagConstraints.NONE;
        c.anchor = GridBagConstraints.CENTER;
        c.insets = new Insets(0, 0, 0, 0);
        c.gridheight = 1;
        c.gridwidth = 1;
        c.weighty = 0;
        c.weightx = 1;
        c.gridx = 0;
        c.gridy = 0;
        panel.add(cancelBtn, c);
        c.gridx++;
        panel.add(submitBtn, c);
        return panel;
    }

    public void actionPerformed(ActionEvent e) {
        if ("submit".equals(e.getActionCommand())) {
            try {
                String bugdescStr = bugdesc.getText();
                if (bugdescStr == null || bugdescStr.equals("")) {
                    JOptionPane.showMessageDialog(this, "Problem description not defined.", "Error", JOptionPane.ERROR_MESSAGE);
                    return;
                }
                String data = URLEncoder.encode("ver", "UTF-8") + "=" + URLEncoder.encode(Double.toString(bugVer), "UTF-8");
                data += "&" + URLEncoder.encode("name", "UTF-8") + "=" + URLEncoder.encode(name.getText(), "UTF-8");
                data += "&" + URLEncoder.encode("os", "UTF-8") + "=" + URLEncoder.encode(os.getText(), "UTF-8");
                data += "&" + URLEncoder.encode("jre", "UTF-8") + "=" + URLEncoder.encode(jre.getText(), "UTF-8");
                data += "&" + URLEncoder.encode("email", "UTF-8") + "=" + URLEncoder.encode(email.getText(), "UTF-8");
                data += "&" + URLEncoder.encode("key", "UTF-8") + "=" + URLEncoder.encode("X7pa4yL", "UTF-8");
                data += "&" + URLEncoder.encode("bugdesc", "UTF-8") + "=" + URLEncoder.encode(bugdescStr, "UTF-8");
                URL url = new URL("http://ubcdcreator.sourceforge.net/reportbug.php");
                URLConnection conn = url.openConnection();
                conn.setDoInput(true);
                conn.setDoOutput(true);
                OutputStreamWriter wr = new OutputStreamWriter(conn.getOutputStream());
                wr.write(data);
                wr.flush();
                BufferedReader rd = new BufferedReader(new InputStreamReader(conn.getInputStream()));
                String line;
                while ((line = rd.readLine()) != null) {
                }
                rd.close();
                wr.close();
            } catch (Exception ex) {
            }
            setVisible(false);
        } else if ("cancel".equals(e.getActionCommand())) {
            setVisible(false);
        }
    }
}
