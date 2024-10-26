package com.code316.flash;

import java.applet.Applet;
import java.awt.Graphics;
import com.code316.flash.xml.SimpleXmlDeckBuilder;
import java.awt.List;
import java.awt.Button;
import java.awt.event.ActionListener;
import java.awt.event.ActionEvent;
import java.awt.event.ItemEvent;
import java.awt.event.ItemListener;
import java.awt.Label;
import java.awt.BorderLayout;
import java.util.StringTokenizer;
import java.util.Hashtable;
import java.util.Properties;
import java.io.IOException;
import java.net.MalformedURLException;
import java.net.URL;
import java.net.HttpURLConnection;
import java.net.URLConnection;
import java.io.InputStream;
import java.awt.Insets;
import java.awt.TextArea;

public class ViewerApplet extends Applet {

    private Deck deck;

    private List deckList = new List();

    private Button btnCreate = new Button("Start Session");

    private Label lblMessage = new Label();

    private TextArea messageBox = new TextArea(4, 50);

    class Handler implements ActionListener, ItemListener {

        public void itemStateChanged(ItemEvent ie) {
        }

        public void actionPerformed(ActionEvent ae) {
            if (ae.getSource() == btnCreate) {
                createViewer();
            }
        }
    }

    private Hashtable deckCodes = new Hashtable();

    private Properties properties = new Properties();

    private boolean connectionOk(URLConnection connection) throws IOException {
        return true;
    }

    private String createDeckUrl(String[] deckNames) {
        int count = deckNames.length;
        StringBuffer list = new StringBuffer();
        for (int i = 0; i < count; i++) {
            String deckCode = (String) deckCodes.get(deckNames[i]);
            list.append("name=").append(deckCode);
            if (i != count - 1) {
                list.append("&");
            }
        }
        String deckUrl = getCodeBase() + properties.getProperty("deck.file");
        deckUrl = deckUrl + "?" + list;
        return deckUrl;
    }

    /**
 * 
 */
    private void createViewer() {
        String[] names = deckList.getSelectedItems();
        if (names.length == 0) {
            setMessage("No objectives selected.");
            return;
        }
        setMessage("");
        setMessage("creating deck request");
        String deckUrl = createDeckUrl(names);
        boolean error = true;
        try {
            setMessage("fetching deck");
            URL url = new URL(deckUrl);
            URLConnection connection = url.openConnection();
            connection.connect();
            InputStream in = url.openStream();
            SimpleXmlDeckBuilder builder = new SimpleXmlDeckBuilder();
            Deck deck = builder.build(in);
            DeckViewer viewer = new DeckViewer(deck);
            Insets insets = getInsets();
            viewer.setSize(viewer.getSize().width + insets.left + insets.right, viewer.getSize().height + insets.top + insets.bottom);
            viewer.show();
            viewer.setVisible(true);
            setMessage("viewer loaded");
            error = false;
        } catch (MalformedURLException e) {
            setMessage("deck not loaded:invalid url:" + deckUrl);
        } catch (IOException e) {
            setMessage("deck not loaded:" + e);
        } catch (Exception e) {
            setMessage("deck not loaded:" + e);
            e.printStackTrace();
        } finally {
            if (error) {
                String message = "Deck not retreived.\n" + "Please log in.\n" + "If the problem persists, please " + "contact flash@code316.com and include any relevant information" + "screenshots, username, etc.";
                setMessage(message);
            }
        }
    }

    /**
     * Returns information about this applet.
     * @return a string of information about this applet
     */
    public String getAppletInfo() {
        return "ViewerApplet\n" + "\n" + "\n" + "";
    }

    public void init() {
        super.init();
        loadConfig();
        setLayout(new BorderLayout());
        add("North", messageBox);
        add("Center", deckList);
        add("South", btnCreate);
        loadDeckNames();
        Thread.currentThread().setName("applet");
        Handler handler = new Handler();
        btnCreate.addActionListener(handler);
        deckList.addItemListener(handler);
        deckList.setMultipleMode(true);
    }

    private void loadConfig() {
        try {
            String configFile = getCodeBase() + "viewerapplet.config.txt";
            URL url = new URL(configFile);
            properties.load(url.openStream());
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    private void loadDeckNames() {
        String deckNames = properties.getProperty("deck.names");
        if (deckNames == null) {
            System.out.println("deck.names parameter is missing");
        }
        StringTokenizer names = new StringTokenizer(deckNames, "+");
        while (names.hasMoreTokens()) {
            String name = names.nextToken();
            int pos = name.indexOf("|");
            if (pos == -1) {
                System.out.println("malformed name list");
                return;
            }
            String deckName = name.substring(0, pos).trim();
            String deckCode = name.substring(pos + 1).trim();
            deckCodes.put(deckName, deckCode);
            deckList.add(deckName);
        }
    }

    /**
     * Paints the applet.
     * If the applet does not need to be painted (e.g. if it is only a container for other
     * awt components) then this method can be safely removed.
     * 
     * @param g  the specified Graphics window
     * @see #update
     */
    public void paint(Graphics g) {
        super.paint(g);
    }

    /**
 * 
 * @param msg java.lang.String
 */
    private void setMessage(String msg) {
        messageBox.append(msg + "\n");
    }
}
