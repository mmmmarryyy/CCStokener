package com.oz.lanslim.gui;

import java.awt.Frame;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.io.DataInputStream;
import java.io.IOException;
import java.net.HttpURLConnection;
import java.net.URL;
import javax.swing.JButton;
import javax.swing.JDialog;
import javax.swing.JLabel;
import javax.swing.JOptionPane;
import com.jgoodies.forms.layout.CellConstraints;
import com.jgoodies.forms.layout.FormLayout;
import com.oz.lanslim.Externalizer;
import com.oz.lanslim.SlimLogger;
import com.oz.lanslim.StringConstants;

public class AboutDialog extends JDialog implements ActionListener {

    private static final String VERSION = "1.6.0";

    private static final String AUTHOR_NAME = "Olivier Mourez";

    private static final String AUTHOR_ADDRESS = "mourezwell@users.sourceforge.net";

    private static final String MAIL_LINK = "mailto:";

    private static final String PROJECT_HOME = "http://sourceforge.net/projects/lanslim/";

    private static final String WEB_HOME = "http://lanslim.sourceforge.net";

    public AboutDialog(Frame pParent) {
        super(pParent, true);
        initGUI();
    }

    private void initGUI() {
        try {
            setTitle(Externalizer.getString("LANSLIM.118"));
            FormLayout thisLayout = new FormLayout("max(p;5dlu), max(p;5dlu), max(p;5dlu), max(p;5dlu)", "max(p;5dlu), max(p;5dlu), max(p;5dlu), max(p;5dlu), max(p;5dlu), max(p;5dlu), max(p;5dlu), max(p;5dlu), max(p;5dlu), max(p;5dlu), max(p;5dlu), max(p;5dlu), max(p;5dlu), max(p;5dlu), max(p;5dlu)");
            getContentPane().setLayout(thisLayout);
            setSize(270, 200);
            setResizable(false);
            {
                JLabel versionLabel = new JLabel(Externalizer.getString("LANSLIM.113", VERSION));
                getContentPane().add(versionLabel, new CellConstraints("2, 2, 2, 1, default, default"));
            }
            {
                JLabel authorLabel = new JLabel(Externalizer.getString("LANSLIM.114", AUTHOR_NAME));
                getContentPane().add(authorLabel, new CellConstraints("2, 4, 2, 1, default, default"));
            }
            {
                JLabel questionsLabel = new JLabel(Externalizer.getString("LANSLIM.115"));
                getContentPane().add(questionsLabel, new CellConstraints("2, 6, 2, 1, default, default"));
            }
            {
                JLabel adressLabel = new JLabel(Externalizer.getString("LANSLIM.116"));
                getContentPane().add(adressLabel, new CellConstraints("2, 8, 1, 1, default, default"));
                JLabel adressLink = new SlimHyperLink(AUTHOR_ADDRESS, MAIL_LINK + AUTHOR_ADDRESS);
                getContentPane().add(adressLink, new CellConstraints("3, 8, 1, 1, default, default"));
            }
            {
                JLabel homeLabel = new JLabel(Externalizer.getString("LANSLIM.117"));
                getContentPane().add(homeLabel, new CellConstraints("2, 9, 1, 1, default, default"));
                JLabel homeLink = new SlimHyperLink(PROJECT_HOME);
                getContentPane().add(homeLink, new CellConstraints("3, 9, 1, 1, default, default"));
            }
            {
                JLabel webLabel = new JLabel(Externalizer.getString("LANSLIM.189"));
                getContentPane().add(webLabel, new CellConstraints("2, 10, 1, 1, default, default"));
                JLabel webLink = new SlimHyperLink(WEB_HOME);
                getContentPane().add(webLink, new CellConstraints("3, 10, 1, 1, default, default"));
            }
            {
                JLabel emptyLabel = new JLabel(StringConstants.SPACE);
                getContentPane().add(emptyLabel, new CellConstraints("2, 11, 3, 1, default, default"));
            }
            {
                JLabel checkLabel = new JLabel(Externalizer.getString("LANSLIM.198"));
                getContentPane().add(checkLabel, new CellConstraints("2, 12, 3, 1, default, default"));
            }
            {
                JButton yesButton = new JButton();
                yesButton.setText(Externalizer.getString("LANSLIM.213"));
                yesButton.setActionCommand(AboutActionCommand.YES);
                yesButton.addActionListener(this);
                getContentPane().add(yesButton, new CellConstraints("2, 14, 1, 1, center, center"));
                JButton noButton = new JButton();
                noButton.setText(Externalizer.getString("LANSLIM.214"));
                noButton.setActionCommand(AboutActionCommand.NO);
                noButton.addActionListener(this);
                getContentPane().add(noButton, new CellConstraints("3, 14, 1, 1, center, center"));
            }
        } catch (Exception e) {
            SlimLogger.logException("AboutDialog.initGUI", e);
        }
    }

    public void actionPerformed(ActionEvent e) {
        if (e.getActionCommand().equals(AboutActionCommand.YES)) {
            checkVersion(false);
        }
        setVisible(false);
    }

    public void checkVersion(boolean showOnlyDiff) {
        try {
            DataInputStream di = null;
            byte[] b = new byte[1];
            URL url = new URL("http://lanslim.sourceforge.net/version.txt");
            HttpURLConnection con = (HttpURLConnection) url.openConnection();
            di = new DataInputStream(con.getInputStream());
            StringBuffer lBuffer = new StringBuffer();
            while (-1 != di.read(b, 0, 1)) {
                lBuffer.append(new String(b));
            }
            String lLastStr = lBuffer.toString().trim();
            boolean equals = VERSION.equals(lLastStr);
            String lMessage = Externalizer.getString("LANSLIM.199", VERSION, lLastStr);
            if (!equals) {
                lMessage = lMessage + StringConstants.NEW_LINE + Externalizer.getString("LANSLIM.131") + StringConstants.NEW_LINE;
            }
            if (!equals || !showOnlyDiff) {
                JOptionPane.showMessageDialog(getRootPane().getParent(), lMessage, Externalizer.getString("LANSLIM.118"), JOptionPane.INFORMATION_MESSAGE);
            }
        } catch (NumberFormatException e) {
            JOptionPane.showMessageDialog(getRootPane().getParent(), Externalizer.getString("LANSLIM.200", SlimLogger.shortFormatException(e)), Externalizer.getString("LANSLIM.118"), JOptionPane.WARNING_MESSAGE);
        } catch (IOException e) {
            JOptionPane.showMessageDialog(getRootPane().getParent(), Externalizer.getString("LANSLIM.200", SlimLogger.shortFormatException(e)), Externalizer.getString("LANSLIM.118"), JOptionPane.WARNING_MESSAGE);
        }
    }

    private class AboutActionCommand {

        private static final String YES = "YES";

        private static final String NO = "NO";
    }
}
