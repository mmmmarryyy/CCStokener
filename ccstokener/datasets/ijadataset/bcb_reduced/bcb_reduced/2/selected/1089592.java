package frost.components;

import java.awt.*;
import java.awt.event.*;
import java.io.*;
import java.util.*;
import java.util.logging.*;
import java.net.URL;
import java.net.URLConnection;
import javax.swing.text.html.HTMLDocument;
import javax.swing.text.AbstractDocument;
import org.lobobrowser.html.*;
import org.lobobrowser.html.gui.*;
import org.lobobrowser.html.test.*;
import org.lobobrowser.html.domimpl.NodeImpl;
import org.lobobrowser.html.domimpl.NodeFilter;
import org.lobobrowser.html.domimpl.ElementFilter;
import org.lobobrowser.html.domimpl.HTMLImageElementImpl;
import org.mozilla.javascript.*;
import org.w3c.dom.Node;
import java.util.ArrayList;
import java.beans.PropertyChangeListener;
import java.beans.PropertyChangeEvent;
import javax.swing.*;
import javax.swing.event.*;
import frost.*;

/**
 * Browser Component
 * @author Jantho
 */
public class Browser extends JPanel {

    private static final Logger logger = Logger.getLogger(Browser.class.getName());

    String[] imageExtensions = { ".jpg", ".gif", ".jpeg", ".png", ".bmp" };

    JFrame parent;

    JPanel contentPanel;

    JPanel buttonPanel = new JPanel(new FlowLayout(FlowLayout.LEFT));

    JButton backButton = new JButton("<");

    JButton forwardButton = new JButton(">");

    JButton homeButton = new JButton("~");

    JButton refreshButton = new JButton("R");

    JButton addPageButton = new JButton(": )");

    JButton removePageButton = new JButton(": (");

    JButton closeButton = new JButton("Close Browser");

    JComboBox urlComboBox = new JComboBox();

    JComboBox favComboBox = new JComboBox();

    HtmlPanel htmlPanel = new HtmlPanel();

    SimpleHtmlRendererContext rendererContext = null;

    String name;

    private void init() {
        urlComboBox.setEditable(true);
        rendererContext = new SimpleHtmlRendererContext(htmlPanel, new SimpleUserAgentContext());
        htmlPanel.addSelectionChangeListener(new SelectionChangeListener() {

            public void selectionChanged(final SelectionChangeEvent e) {
                Runnable addURL = new Runnable() {

                    public void run() {
                        try {
                            Thread.currentThread().sleep(1000);
                        } catch (Exception exc) {
                            System.out.println("Error sleeping in Browser.java htmlPanel selectionchangelistener (perhaps it needs some Ambien): " + exc);
                        }
                        String url = rendererContext.getCurrentURL();
                        boolean exists = false;
                        for (int i = 0; i < urlComboBox.getItemCount(); i++) {
                            if (((String) urlComboBox.getItemAt(i)).equals(url)) {
                                exists = true;
                                urlComboBox.setSelectedItem(url);
                            }
                        }
                        if (!exists) {
                            int i = urlComboBox.getSelectedIndex();
                            if (i == -1 || urlComboBox.getItemCount() == 0) {
                                i = 0;
                            } else {
                                i++;
                            }
                            urlComboBox.insertItemAt(url, i);
                            urlComboBox.setSelectedItem(url);
                        }
                    }
                };
                new Thread(addURL).start();
            }
        });
        backButton.addActionListener(new java.awt.event.ActionListener() {

            public void actionPerformed(final ActionEvent e) {
                int i = urlComboBox.getSelectedIndex();
                if (i > 0) {
                    i--;
                    setPage(urlComboBox.getItemAt(i).toString());
                }
            }
        });
        forwardButton.addActionListener(new java.awt.event.ActionListener() {

            public void actionPerformed(final ActionEvent e) {
                int i = urlComboBox.getSelectedIndex();
                if (i < urlComboBox.getItemCount() - 1) {
                    i++;
                    setPage(urlComboBox.getItemAt(i).toString());
                }
            }
        });
        homeButton.addActionListener(new java.awt.event.ActionListener() {

            public void actionPerformed(final ActionEvent e) {
                setPage(MainFrame.getInstance().getSettingsClass().getValue(SettingsClass.BROWSER_HOME));
            }
        });
        refreshButton.addActionListener(new java.awt.event.ActionListener() {

            public void actionPerformed(final ActionEvent e) {
                setPage((String) urlComboBox.getSelectedItem());
            }
        });
        addPageButton.addActionListener(new java.awt.event.ActionListener() {

            public void actionPerformed(final ActionEvent e) {
                JFrame prompt = new JFrame();
                Object title = JOptionPane.showInputDialog(prompt, "Enter URL name:");
                final String url = rendererContext.getCurrentURL();
                boolean exists = false;
                for (int i = 0; i < favComboBox.getItemCount(); i++) {
                    if (((String) favComboBox.getItemAt(i)).equals(url)) {
                        exists = true;
                        favComboBox.setSelectedItem(url);
                    }
                }
                if (!exists) {
                    favComboBox.addItem(title.toString() + "\t" + url);
                    favComboBox.setSelectedItem(url);
                    writeSettings(new File("browser.ini"));
                }
            }
        });
        removePageButton.addActionListener(new java.awt.event.ActionListener() {

            public void actionPerformed(final ActionEvent e) {
                favComboBox.removeItem(rendererContext.getCurrentURL());
                writeSettings(new File("browser.ini"));
            }
        });
        closeButton.addActionListener(new java.awt.event.ActionListener() {

            public void actionPerformed(final ActionEvent e) {
                MainFrame.getInstance().removeTab(name);
            }
        });
        urlComboBox.addActionListener(new java.awt.event.ActionListener() {

            public void actionPerformed(final ActionEvent e) {
                if (e.getActionCommand().equals("comboBoxEdited") || e.getActionCommand().equals("comboBoxChanged")) {
                    setPage((String) urlComboBox.getSelectedItem());
                }
            }
        });
        favComboBox.addActionListener(new java.awt.event.ActionListener() {

            public void actionPerformed(final ActionEvent e) {
            }
        });
        contentPanel = this;
        contentPanel.setLayout(new BorderLayout());
        buttonPanel.add(backButton);
        buttonPanel.add(forwardButton);
        buttonPanel.add(homeButton);
        buttonPanel.add(refreshButton);
        buttonPanel.add(addPageButton);
        buttonPanel.add(closeButton);
        contentPanel.add(htmlPanel, BorderLayout.CENTER);
        contentPanel.add(buttonPanel, BorderLayout.NORTH);
        contentPanel.add(urlComboBox, BorderLayout.SOUTH);
        readSettings(new File("browser.ini"));
    }

    String makeStartPage() {
        String html = new String();
        html = "<html><body>";
        for (int i = 0; i < favComboBox.getItemCount(); i++) {
            html = html + "<a href=\"" + (String) favComboBox.getItemAt(i) + "\">" + (String) favComboBox.getItemAt(i) + "</a><br>";
        }
        html = html + "</body></html>";
        return html;
    }

    public void setPage(String url) {
        System.out.println("SetPage(" + url + ")");
        if (url != null) {
            if (!url.startsWith("http://")) {
                url = "http://" + url;
            }
            boolean exists = false;
            for (int i = 0; i < urlComboBox.getItemCount(); i++) {
                if (((String) urlComboBox.getItemAt(i)).equals(url)) {
                    exists = true;
                    urlComboBox.setSelectedItem(url);
                }
            }
            if (!exists) {
                int i = urlComboBox.getSelectedIndex();
                if (i == -1 || urlComboBox.getItemCount() == 0) {
                    i = 0;
                } else {
                    i++;
                }
                urlComboBox.insertItemAt(url, i);
                urlComboBox.setSelectedItem(url);
            }
            boolean image = false;
            for (final String element : imageExtensions) {
                if (url.endsWith(element)) {
                    image = true;
                }
            }
            try {
                if (image) {
                    final String html = "<html><img src=\"" + url + "\"></html>";
                } else {
                    final String furl = url;
                    Runnable loadPage = new Runnable() {

                        public void run() {
                            try {
                                System.out.println("Setting page on Cobra");
                                SimpleHtmlRendererContext rendererContext = new SimpleHtmlRendererContext(htmlPanel, new SimpleUserAgentContext());
                                int nodeBaseEnd = furl.indexOf("/", 10);
                                if (nodeBaseEnd == -1) nodeBaseEnd = furl.length();
                                String nodeBase = furl.substring(0, nodeBaseEnd);
                                InputStream pageStream = new URL(furl).openStream();
                                BufferedReader pageStreamReader = new BufferedReader(new InputStreamReader(pageStream));
                                String pageContent = "";
                                String line;
                                while ((line = pageStreamReader.readLine()) != null) pageContent += line;
                                pageContent = borderImages(pageContent, nodeBase);
                                htmlPanel.setHtml(pageContent, furl, rendererContext);
                            } catch (Exception e) {
                                System.out.println("Error loading page " + furl + " : " + e);
                            }
                        }
                    };
                    new Thread(loadPage).start();
                }
            } catch (final Throwable exception) {
                System.out.println("Error in Browser.setPage(): " + exception);
            }
        }
    }

    void hyperlink_actionPerformed(final HyperlinkEvent e) {
        if (e.getEventType() == HyperlinkEvent.EventType.ACTIVATED) {
            setPage(e.getURL().toString());
        }
    }

    void readSettings(final File file) {
        final Vector<String> favs = readLines(file);
        for (int i = 0; i < favs.size(); i++) {
            favComboBox.addItem(favs.elementAt(i));
        }
    }

    void writeSettings(final File file) {
        String output = new String();
        for (int i = 0; i < favComboBox.getItemCount(); i++) {
            output = output + (String) favComboBox.getItemAt(i) + "\r\n";
        }
        writeFile(output, file);
    }

    /**
     * Reads file and returns a Vector of lines
     */
    Vector<String> readLines(final File file) {
        return readLines(file.getPath());
    }

    Vector<String> readLines(final String path) {
        BufferedReader f;
        String line;
        line = "";
        final Vector<String> data = new Vector<String>();
        try {
            f = new BufferedReader(new FileReader(path));
            while ((line = f.readLine()) != null) {
                data.add(line.trim());
            }
            f.close();
        } catch (final IOException e) {
            logger.log(Level.SEVERE, "Read Error: " + path, e);
        }
        return data;
    }

    /**
     * Writes a file "file" to "path"
     */
    void writeFile(final String content, final File file) {
        writeFile(content, file.getPath());
    }

    void writeFile(final String content, final String filename) {
        FileWriter f1;
        try {
            f1 = new FileWriter(filename);
            f1.write(content);
            f1.close();
        } catch (final IOException e) {
            logger.log(Level.SEVERE, "Write Error: " + filename, e);
        }
    }

    /**Constructor*/
    public Browser(final JFrame parent) {
        this.parent = parent;
        init();
    }

    public Browser(final JFrame parent, String tabName) {
        this.parent = parent;
        name = tabName;
        init();
    }

    private String borderImages(String content, final String nodeBase) {
        ArrayList<String> imgTags = new ArrayList<String>();
        for (int i = 0; i < content.length() - 4; i++) {
            if (content.substring(i, i + 4).equals("<img")) {
                for (int j = i; j < content.length(); j++) if (content.charAt(j) == '>') {
                    imgTags.add(content.substring(i, j));
                    break;
                }
            }
        }
        final ArrayList<String> imgUrls = new ArrayList<String>();
        for (String imgtag : imgTags) {
            int start = imgtag.indexOf("src=\"") + 5;
            int end = imgtag.indexOf("\"", start);
            imgUrls.add(imgtag.substring(start, end));
        }
        ArrayList<Thread> imgPreloaders = new ArrayList<Thread>();
        final ArrayList<String> imgNotFound = new ArrayList<String>();
        for (final String url : imgUrls) {
            Runnable preloadImage = new Runnable() {

                public void run() {
                    try {
                        URLConnection connection = new URL(nodeBase + url).openConnection();
                        connection.setConnectTimeout(100);
                        connection.getContent();
                        imgUrls.remove(url);
                    } catch (Exception e) {
                    }
                }
            };
            imgPreloaders.add(new Thread(preloadImage));
        }
        for (Thread t : imgPreloaders) t.start();
        for (Thread t : imgPreloaders) try {
            t.join(500);
        } catch (Exception e) {
            System.out.println("Error in Browser: borderImages: " + e);
        }
        for (String imgUrl : imgUrls) content = content.replaceAll(imgUrl + "\"", imgUrl + "\" border=3 ");
        return content;
    }
}
