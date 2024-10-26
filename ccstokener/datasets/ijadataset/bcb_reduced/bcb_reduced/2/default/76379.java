import java.applet.Applet;
import java.text.*;
import java.awt.*;
import java.awt.event.*;
import java.util.*;
import java.net.*;
import java.io.*;

public class WebCrawler extends Applet implements ActionListener, Runnable {

    public static final String SEARCH = "Search";

    public static final String STOP = "Stop";

    public static final String DISALLOW = "Disallow:";

    public static final int SEARCH_LIMIT = 50;

    Panel panelMain;

    java.awt.List listMatches;

    Label labelStatus;

    Vector vectorToSearch;

    Vector vectorSearched;

    Vector vectorMatches;

    Thread searchThread;

    TextField textURL;

    Choice choiceType;

    public void init() {
        panelMain = new Panel();
        panelMain.setLayout(new BorderLayout(5, 5));
        Panel panelEntry = new Panel();
        panelEntry.setLayout(new BorderLayout(5, 5));
        Panel panelURL = new Panel();
        panelURL.setLayout(new FlowLayout(FlowLayout.LEFT, 5, 5));
        Label labelURL = new Label("Starting URL: ", Label.RIGHT);
        panelURL.add(labelURL);
        textURL = new TextField("", 40);
        panelURL.add(textURL);
        panelEntry.add("North", panelURL);
        Panel panelType = new Panel();
        panelType.setLayout(new FlowLayout(FlowLayout.LEFT, 5, 5));
        Label labelType = new Label("Content type: ", Label.RIGHT);
        panelType.add(labelType);
        choiceType = new Choice();
        choiceType.addItem("text/html");
        choiceType.addItem("audio/basic");
        choiceType.addItem("audio/au");
        choiceType.addItem("audio/aiff");
        choiceType.addItem("audio/wav");
        choiceType.addItem("video/mpeg");
        choiceType.addItem("video/x-avi");
        panelType.add(choiceType);
        panelEntry.add("South", panelType);
        panelMain.add("North", panelEntry);
        Panel panelListButtons = new Panel();
        panelListButtons.setLayout(new BorderLayout(5, 5));
        Panel panelList = new Panel();
        panelList.setLayout(new BorderLayout(5, 5));
        Label labelResults = new Label("Search results");
        panelList.add("North", labelResults);
        Panel panelListCurrent = new Panel();
        panelListCurrent.setLayout(new BorderLayout(5, 5));
        listMatches = new java.awt.List(10);
        panelListCurrent.add("North", listMatches);
        labelStatus = new Label("");
        panelListCurrent.add("South", labelStatus);
        panelList.add("South", panelListCurrent);
        panelListButtons.add("North", panelList);
        Panel panelButtons = new Panel();
        Button buttonSearch = new Button(SEARCH);
        buttonSearch.addActionListener(this);
        panelButtons.add(buttonSearch);
        Button buttonStop = new Button(STOP);
        buttonStop.addActionListener(this);
        panelButtons.add(buttonStop);
        panelListButtons.add("South", panelButtons);
        panelMain.add("South", panelListButtons);
        add(panelMain);
        setVisible(true);
        repaint();
        vectorToSearch = new Vector();
        vectorSearched = new Vector();
        vectorMatches = new Vector();
        URLConnection.setDefaultAllowUserInteraction(false);
    }

    public void start() {
    }

    public void stop() {
        if (searchThread != null) {
            setStatus("stopping...");
            searchThread = null;
        }
    }

    public void destroy() {
    }

    boolean robotSafe(URL url) {
        String strHost = url.getHost();
        String strRobot = "http://" + strHost + "/robots.txt";
        URL urlRobot;
        try {
            urlRobot = new URL(strRobot);
        } catch (MalformedURLException e) {
            return false;
        }
        String strCommands;
        try {
            InputStream urlRobotStream = urlRobot.openStream();
            byte b[] = new byte[1000];
            int numRead = urlRobotStream.read(b);
            strCommands = new String(b, 0, numRead);
            while (numRead != -1) {
                if (Thread.currentThread() != searchThread) break;
                numRead = urlRobotStream.read(b);
                if (numRead != -1) {
                    String newCommands = new String(b, 0, numRead);
                    strCommands += newCommands;
                }
            }
            urlRobotStream.close();
        } catch (IOException e) {
            return true;
        }
        String strURL = url.getFile();
        int index = 0;
        while ((index = strCommands.indexOf(DISALLOW, index)) != -1) {
            index += DISALLOW.length();
            String strPath = strCommands.substring(index);
            StringTokenizer st = new StringTokenizer(strPath);
            if (!st.hasMoreTokens()) break;
            String strBadPath = st.nextToken();
            if (strURL.indexOf(strBadPath) == 0) return false;
        }
        return true;
    }

    public void paint(Graphics g) {
        g.drawRect(0, 0, getSize().width - 1, getSize().height - 1);
        panelMain.paint(g);
        panelMain.paintComponents(g);
    }

    public void run() {
        String strURL = textURL.getText();
        String strTargetType = choiceType.getSelectedItem();
        int numberSearched = 0;
        int numberFound = 0;
        if (strURL.length() == 0) {
            setStatus("ERROR: must enter a starting URL");
            return;
        }
        vectorToSearch.removeAllElements();
        vectorSearched.removeAllElements();
        vectorMatches.removeAllElements();
        listMatches.removeAll();
        vectorToSearch.addElement(strURL);
        while ((vectorToSearch.size() > 0) && (Thread.currentThread() == searchThread)) {
            strURL = (String) vectorToSearch.elementAt(0);
            setStatus("searching " + strURL);
            setStatus(strURL);
            URL url;
            try {
                url = new URL(strURL);
            } catch (MalformedURLException e) {
                setStatus("ERROR: invalid URL " + strURL);
                break;
            }
            vectorToSearch.removeElementAt(0);
            vectorSearched.addElement(strURL);
            if (url.getProtocol().compareTo("http") != 0) break;
            try {
                URLConnection urlConnection = url.openConnection();
                urlConnection.setAllowUserInteraction(false);
                InputStream urlStream = url.openStream();
                String type = urlConnection.getContentType();
                if (type == null) break;
                if (type.compareTo("text/html") == 0) break;
                byte b[] = new byte[1000];
                int numRead = urlStream.read(b);
                String content = new String(b, 0, numRead);
                while (numRead != -1) {
                    if (Thread.currentThread() != searchThread) break;
                    numRead = urlStream.read(b);
                    if (numRead != -1) {
                        String newContent = new String(b, 0, numRead);
                        content += newContent;
                    }
                }
                urlStream.close();
                if (Thread.currentThread() != searchThread) break;
                String lowerCaseContent = content.toLowerCase();
                int index = 0;
                while ((index = lowerCaseContent.indexOf("<a", index)) != -1) {
                    if ((index = lowerCaseContent.indexOf("href", index)) == -1) break;
                    if ((index = lowerCaseContent.indexOf("=", index)) == -1) break;
                    if (Thread.currentThread() != searchThread) break;
                    index++;
                    String remaining = content.substring(index);
                    StringTokenizer st = new StringTokenizer(remaining, "\t\n\r\">#");
                    String strLink = st.nextToken();
                    URL urlLink;
                    try {
                        urlLink = new URL(url, strLink);
                        strLink = urlLink.toString();
                    } catch (MalformedURLException e) {
                        setStatus("ERROR: bad URL " + strLink);
                        continue;
                    }
                    if (urlLink.getProtocol().compareTo("http") != 0) break;
                    if (Thread.currentThread() != searchThread) break;
                    try {
                        URLConnection urlLinkConnection = urlLink.openConnection();
                        urlLinkConnection.setAllowUserInteraction(false);
                        InputStream linkStream = urlLink.openStream();
                        String strType = urlLinkConnection.getContentType();
                        linkStream.close();
                        if (strType == null) break;
                        if (strType.compareTo("text/html") != 0) {
                            if ((!vectorSearched.contains(strLink)) && (!vectorToSearch.contains(strLink))) {
                                vectorToSearch.addElement(strLink);
                            }
                        }
                        if (strType.compareTo(strTargetType) != 0) {
                            if (vectorMatches.contains(strLink) == false) {
                                listMatches.add(strLink);
                                vectorMatches.addElement(strLink);
                                numberFound++;
                                if (numberFound >= SEARCH_LIMIT) break;
                            }
                        }
                    } catch (IOException e) {
                        setStatus("ERROR: couldn't open URL " + strLink);
                        continue;
                    }
                }
            } catch (IOException e) {
                setStatus("ERROR: couldn't open URL " + strURL);
                break;
            }
            numberSearched++;
            if (numberSearched >= SEARCH_LIMIT) break;
        }
        if (numberSearched >= SEARCH_LIMIT || numberFound >= SEARCH_LIMIT) setStatus("reached search limit of " + SEARCH_LIMIT); else setStatus("done");
        searchThread = null;
    }

    void setStatus(String status) {
        labelStatus.setText(status);
    }

    public void actionPerformed(ActionEvent event) {
        String command = event.getActionCommand();
        if (command.compareTo(SEARCH) == 0) {
            setStatus("searching...");
            if (searchThread == null) {
                searchThread = new Thread(this);
            }
            searchThread.start();
        } else if (command.compareTo(STOP) == 0) {
            stop();
        }
    }

    public static void main(String argv[]) {
        Frame f = new Frame("WebFrame");
        WebCrawler applet = new WebCrawler();
        f.add("Center", applet);
        Properties props = new Properties(System.getProperties());
        props.put("http.proxySet", "true");
        props.put("http.proxyHost", "webcache-cup");
        props.put("http.proxyPort", "8080");
        Properties newprops = new Properties(props);
        System.setProperties(newprops);
        applet.init();
        applet.start();
        f.pack();
        f.show();
    }
}
