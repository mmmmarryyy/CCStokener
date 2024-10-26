package misc;

import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.net.MalformedURLException;
import java.net.URL;
import javax.swing.JEditorPane;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JTextField;
import javax.swing.event.HyperlinkEvent;
import javax.swing.event.HyperlinkListener;
import csimage.MatrixFrame;
import csimage.util.dbg;

/**
 * @author I-Ling
 */
public class SimpleWebPageViewer extends MatrixFrame implements ActionListener, HyperlinkListener {

    private String requestURL;

    private JTextField urlField;

    private JEditorPane htmlPane;

    public static void main(String[] args) {
        new SimpleWebPageViewer();
    }

    public SimpleWebPageViewer() {
        this("http://www.sfu.ca/");
    }

    public SimpleWebPageViewer(String url) {
        this(url, 800, 800);
    }

    public SimpleWebPageViewer(String url, int width, int height) {
        super("Simple Web Page Viewer", width, height);
        createUI(url);
        setVisible(true);
    }

    protected void createUI(String url) {
        requestURL = url;
        JPanel top = new JPanel();
        top.setBackground(Color.lightGray);
        JLabel urlLabel = new JLabel("URL:");
        urlField = new JTextField(30);
        urlField.setText(requestURL);
        urlField.addActionListener(this);
        top.add(urlLabel);
        top.add(urlField);
        getContentPane().add(top, BorderLayout.NORTH);
        try {
            htmlPane = new JEditorPane(requestURL);
            htmlPane.setEditable(false);
            htmlPane.addHyperlinkListener(this);
            JScrollPane scrollPane = new JScrollPane(htmlPane);
            getContentPane().add(scrollPane, BorderLayout.CENTER);
        } catch (IOException e) {
            dbg.sayln("Can't build HTML pane for " + requestURL + ": " + e);
        }
    }

    public String getWebPage(String url) {
        String content = "";
        URL urlObj = null;
        try {
            urlObj = new URL(url);
        } catch (MalformedURLException urlEx) {
            urlEx.printStackTrace();
            throw new Error("URL creation failed.");
        }
        try {
            BufferedReader reader = new BufferedReader(new InputStreamReader(urlObj.openStream()));
            String line;
            while ((line = reader.readLine()) != null) {
                content += line;
            }
        } catch (IOException e) {
            e.printStackTrace();
            throw new Error("Page retrieval failed.");
        }
        return content;
    }

    public void actionPerformed(ActionEvent event) {
        if (event.getSource() == urlField) requestURL = urlField.getText(); else requestURL = "http://www.sfu.ca/~ilina";
        try {
            htmlPane.setPage(new URL(requestURL));
            urlField.setText(requestURL);
        } catch (IOException e) {
            dbg.sayln("Can't follow link to " + requestURL + ": " + e);
        }
    }

    public void hyperlinkUpdate(HyperlinkEvent event) {
        if (event.getEventType() == HyperlinkEvent.EventType.ACTIVATED) {
            try {
                URL url = event.getURL();
                requestURL = url.toExternalForm();
                htmlPane.setPage(url);
                urlField.setText(requestURL);
            } catch (IOException e) {
                dbg.sayln("Can't follow link to " + requestURL + ": " + e);
            }
        }
    }
}
