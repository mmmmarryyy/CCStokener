package org.swhite.common.swing;

import java.awt.Component;
import java.awt.Container;
import java.awt.Font;
import java.awt.Frame;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.Insets;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import javax.swing.JButton;
import javax.swing.JDialog;
import javax.swing.JEditorPane;
import javax.swing.JLabel;
import javax.swing.JScrollPane;
import java.io.InputStream;
import java.io.Reader;
import java.net.URL;
import java.util.Properties;

public class AboutDialog extends JDialog implements ActionListener {

    public AboutDialog(Frame owner) {
        super(owner, "About", false);
        int width = 284;
        int height = 328;
        int x = owner.getX() + ((owner.getWidth() - width) / 2);
        int y = owner.getY() + ((owner.getHeight() - height) / 2);
        setSize(width, height);
        setLocation(x, y);
        setResizable(false);
        Container contentPane = getContentPane();
        GridBagLayout layout = new GridBagLayout();
        contentPane.setLayout(layout);
        Properties info = new Properties();
        String credits = new String();
        try {
            URL url = getClass().getResource("/resources/info.properties");
            InputStream in = url.openStream();
            info.load(in);
            in.close();
            url = getClass().getResource("/resources/credits.html");
            in = url.openStream();
            int read;
            while ((read = in.read()) != -1) {
                credits += (char) read;
            }
            in.close();
        } catch (Exception e) {
            e.printStackTrace();
        }
        String name = info.getProperty("app.name", "My Application");
        String version = info.getProperty("app.version", "1.0");
        String copyright = info.getProperty("app.copyright", "Copyright 2003 Steve White");
        String buildNumber = info.getProperty("build.number", "1");
        JLabel versionLabel = new JLabel(name + " v" + version + " (#" + buildNumber + ")");
        versionLabel.setFont(new Font(null, Font.PLAIN, 14));
        add(versionLabel, 0, 0, 2, 1, 0, 0);
        if (credits.equals("") == false) {
            JEditorPane creditsPane = new JEditorPane("text/html", credits);
            creditsPane.setEditable(false);
            JScrollPane scrollPane = new JScrollPane(creditsPane);
            add(scrollPane, 0, 1, 1, 1, 100, 100);
        }
        JLabel copyrightLabel = new JLabel(copyright);
        add(copyrightLabel, 0, 2, 1, 1, 0, 0);
        JButton okayButton = new JButton("Okay");
        okayButton.addActionListener(this);
        add(okayButton, 0, 3, 1, 1, 0, 0);
        setTitle("About " + name);
    }

    public void add(Component c, int x, int y, int w, int h, int weightx, int weighty) {
        GridBagConstraints constraints = new GridBagConstraints();
        constraints.fill = GridBagConstraints.BOTH;
        constraints.anchor = GridBagConstraints.CENTER;
        constraints.weightx = 0;
        constraints.gridx = x;
        constraints.gridy = y;
        constraints.gridwidth = w;
        constraints.gridheight = h;
        constraints.weightx = weightx;
        constraints.weighty = weighty;
        constraints.insets = new Insets(2, 2, 2, 2);
        getContentPane().add(c, constraints);
    }

    public void actionPerformed(ActionEvent event) {
        dispose();
    }
}
