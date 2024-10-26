package com.intel.gpe.gridbeans.povray.plugin;

import java.awt.BorderLayout;
import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.PrintWriter;
import java.io.StringWriter;
import java.net.URL;
import java.net.URLConnection;
import javax.swing.JEditorPane;
import javax.swing.JScrollPane;
import javax.swing.JTabbedPane;
import javax.swing.JTextPane;
import com.intel.gpe.client2.Client;
import com.intel.gpe.client2.defaults.preferences.INode;
import com.intel.gpe.gridbeans.plugins.GridBeanPanel;

/**
 * @version $Id: InfoPanel.java,v 1.11 2007/02/22 14:40:20 dizhigul Exp $
 * @author Denis Zhigula
 */
public class InfoPanel extends GridBeanPanel {

    private JTabbedPane top;

    private JTextPane legalText, aboutText;

    public InfoPanel(Client client, INode node) {
        super(client, "Info", node);
        this.parent2 = client;
        buildComponents();
    }

    private void buildComponents() {
        top = new JTabbedPane();
        setLayout(new BorderLayout());
        legalText = new JTextPane();
        aboutText = new JTextPane();
        String legalFilename = "";
        String aboutFilename = "";
        try {
            URL legal = InfoPanel.class.getResource("povlegal.txt");
            legalText.setContentType("text");
            legalText.setText(readURL(legal));
            legalText.setEditable(false);
        } catch (IOException e) {
            parent2.getMessageAdapter().showException("Error creating plugin info panel.\nCould not read " + legalFilename, e);
        }
        try {
            URL about = InfoPanel.class.getResource("about.rtf");
            JEditorPane rtfPane = new JEditorPane();
            aboutText.setContentType("text/rtf");
            aboutText.setText(readURL(about));
            aboutText.setEditable(false);
        } catch (IOException e) {
            parent2.getMessageAdapter().showException("Error creating plugin info panel.\nCould not read " + aboutFilename, e);
        }
        top.add("Legal", new JScrollPane(legalText));
        top.add("About", new JScrollPane(aboutText));
        add(top, BorderLayout.CENTER);
    }

    private String readURL(URL url) throws IOException {
        URLConnection uc = url.openConnection();
        InputStream content = (InputStream) uc.getInputStream();
        BufferedReader in = new BufferedReader(new InputStreamReader(content));
        String line;
        StringWriter sw = new StringWriter();
        PrintWriter pw = new PrintWriter(sw);
        while ((line = in.readLine()) != null) {
            pw.println(line);
        }
        return sw.toString();
    }
}
