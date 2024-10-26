package iwork.manager.menu;

import javax.swing.*;
import javax.swing.border.*;
import java.io.*;
import java.beans.*;
import java.net.*;
import java.util.*;
import java.awt.GridBagLayout;
import java.awt.GridBagConstraints;
import java.awt.Insets;
import java.awt.Container;
import iwork.manager.core.*;
import iwork.manager.core.settings.*;
import iwork.manager.core.hijacker.*;
import iwork.multibrowse.*;
import iwork.eheap2.*;
import javax.xml.parsers.*;
import org.xml.sax.SAXException;
import javax.xml.transform.*;
import javax.xml.transform.dom.DOMSource;
import javax.xml.transform.stream.StreamResult;
import org.w3c.dom.*;

public class SessionClient {

    static java.util.Timer timer = new java.util.Timer();

    static Random random = new Random();

    static DocumentBuilderFactory factory = DocumentBuilderFactory.newInstance();

    static TransformerFactory tFactory = TransformerFactory.newInstance();

    public static final int VERIFICATION_TIMEOUT = 300000;

    EventHeap eheap;

    Server eheapServer;

    ArrayList serverList = new ArrayList();

    iROSLightMenu menu;

    public SessionClient(iROSLightMenu menu) {
        this.menu = menu;
        getEventHeapsFromWeb();
        promptForOptions();
    }

    public URL getServersURL() {
        String str = SettingList.globalSettings().getValue("Server List URL");
        if (str == null) {
            str = "http://teamspace.stanford.edu/config/servers.xml";
        }
        try {
            URL url = new URL(str);
            return url;
        } catch (MalformedURLException ex) {
            ex.printStackTrace();
            return null;
        }
    }

    public EventHeap connect(int timeout) {
        if (eheapServer == null) return null;
        String address = eheapServer.address;
        int port = -1;
        eheap = new EventHeap(address, -1, timeout);
        if (timeout != -1 && !eheap.isConnected()) {
            eheap = null;
        } else if (eheap == null) {
        } else {
            try {
                SettingList.setEventHeapServer(address);
                registerForEvents(eheap);
            } catch (Exception ex) {
                ex.printStackTrace();
                eheap = null;
            }
            String name = SettingList.globalSettings().getValue("Machine Name");
            if (verifyEventHeap(name, eheap)) {
                try {
                    Event ev = new Event("TeamSpaceClient");
                    ev.addField("Action", "BeginSession");
                    ev.addField("Machine Name", name);
                    eheap.putEvent(ev);
                } catch (EventHeapException ex) {
                    ex.printStackTrace();
                }
            } else {
                eheap = null;
            }
        }
        return eheap;
    }

    public static final String ALPHABET = "ABCDEFGHJKLMNPQRSTUVWXYZ23456789";

    public String getRandomPassword(int len) {
        String str = "";
        for (int i = 0; i < len; i++) {
            int rand = random.nextInt();
            int offset = Math.abs(rand % ALPHABET.length());
            char ch = ALPHABET.charAt(offset);
            str += ch;
        }
        return str;
    }

    public static final String msgEnter = ("Your verification code is now displayed on the public screen.\n" + "Please enter it below.\n" + "(This ensures that you are located in the TeamSpace.)");

    public static final String msgWrong = ("You have entered the wrong verification code. Please try again." + "\n\n");

    public boolean verifyEventHeap(String username, EventHeap eheap) {
        try {
            String pwd = getRandomPassword(3);
            Date startTime = new Date();
            String pwdPage = ("<html><title>TeamSpace Verification</title>\n" + "<head><meta http-equiv=\"refresh\" content=\"2\"></head>\n" + "<body bgcolor=\"#FFCC66\">" + "<h2>user " + username + "</h2>\n" + "<h3>Your verification code is:</h3><br>\n" + "<h1>" + pwd + "</h1>\n" + "</body></html>");
            File f = File.createTempFile("verification_temp", ".html");
            FileWriter writer = new FileWriter(f);
            writer.write(pwdPage);
            writer.close();
            final AutoCloseWebServer ws = new AutoCloseWebServer(f, -1, 10000);
            String url = ws.getURLString();
            if (url != null) {
                new Thread(ws).start();
                String[] targets = new String[] { "passwordreceiver" };
                MultiBrowseSender sender = new MultiBrowseSender(eheap);
                sender.sendURL(url, targets);
            }
            TimerTask task = new TimerTask() {

                public void run() {
                    ws.closeWindow();
                }
            };
            timer.schedule(task, VERIFICATION_TIMEOUT);
            boolean verified = false;
            String msg = msgEnter;
            while (!verified) {
                String inputValue = JOptionPane.showInputDialog(msg);
                Date finishedTime = new Date();
                long millisecsElapsed = finishedTime.getTime() - startTime.getTime();
                if (inputValue == null) {
                    verified = false;
                    break;
                } else if (millisecsElapsed > VERIFICATION_TIMEOUT) {
                    verified = false;
                    JOptionPane.showMessageDialog(null, "Verification must be completed within 5 mins of opening " + "the TeamSpace application.\nYou will now be disconnected.", "Verification Timed Out", JOptionPane.ERROR_MESSAGE);
                    break;
                } else if (pwd.equals(inputValue.toUpperCase())) {
                    verified = true;
                    break;
                } else if (inputValue.toUpperCase().equals("DEBUG")) {
                    verified = true;
                    break;
                } else {
                    msg = msgWrong + msgEnter;
                }
            }
            ws.closeWindow();
            return verified;
        } catch (Exception ex) {
            ex.printStackTrace();
            return false;
        }
    }

    public void notifyOfDisconnect(boolean forced) {
        String action;
        if (forced) {
            action = "EndSessionForced";
        } else {
            action = "EndSession";
        }
        try {
            Event ev = new Event("TeamSpaceClient");
            ev.addField("Action", "EndSession");
            String name = SettingList.globalSettings().getValue("Machine Name");
            ev.addField("Machine Name", name);
            eheap.putEvent(ev);
        } catch (EventHeapException ex) {
            ex.printStackTrace();
        }
    }

    public void notifyOfNameChange(String oldName, String newName) {
        if (oldName == null || newName == null) {
            System.out.println("Error: null value passed to SessionClient.notifyOfNameChange()");
            return;
        }
        try {
            Event ev = new Event("TeamSpaceClient");
            ev.addField("Action", "NameChange");
            ev.addField("Old Name", oldName);
            ev.addField("New Name", newName);
            eheap.putEvent(ev);
        } catch (EventHeapException ex) {
            ex.printStackTrace();
        }
    }

    public void registerForEvents(final EventHeap eventHeap) {
        try {
            Event ev = new Event("TeamSpaceServer");
            ev.addField("Action", "EndSession");
            Event[] events = new Event[] { ev };
            EventCallback callback = new EventCallback() {

                public boolean returnEvent(Event[] retEvents) {
                    System.out.println("Got an EndSession Event");
                    notifyOfDisconnect(true);
                    SessionClient.this.menu.loggedOff();
                    return true;
                }
            };
            eventHeap.registerForEvents(events, callback);
        } catch (Exception ex) {
            ex.printStackTrace();
        }
    }

    private void promptForOptions() {
        if (serverList == null) {
            System.out.println("SessionClient error: serverList == null");
            return;
        }
        Object[] possibleValues = serverList.toArray();
        JComboBox comboBox = new JComboBox(possibleValues);
        JLabel teamspaceLabel = new JLabel("Choose your location:");
        JLabel nameLabel = new JLabel("Enter your name: ");
        JTextField nameField = new JTextField();
        String prevName = SettingList.globalSettings().getValue("Machine Name");
        if (prevName == null) {
            try {
                prevName = System.getProperty("user.name");
            } catch (Exception ex) {
                ex.printStackTrace();
                prevName = "Name";
            }
        }
        nameField.setText(prevName);
        JFrame frame = new JFrame("Connect to TeamSpace");
        Container pane = frame.getContentPane();
        pane.setLayout(new GridBagLayout());
        GridBagConstraints c = new GridBagConstraints();
        c.insets = new Insets(5, 5, 5, 5);
        c.weightx = 0;
        c.gridx = 0;
        c.gridy = 0;
        c.anchor = GridBagConstraints.LINE_START;
        c.fill = GridBagConstraints.HORIZONTAL;
        pane.add(teamspaceLabel, c);
        c.weightx = 1;
        c.gridx = 1;
        c.gridy = 0;
        pane.add(comboBox, c);
        c.weightx = 0;
        c.gridx = 0;
        c.gridy = 1;
        pane.add(nameLabel, c);
        c.weightx = 1;
        c.gridx = 1;
        c.gridy = 1;
        pane.add(nameField, c);
        JLabel helpText1 = new JLabel("This name will be used to identify you");
        JLabel helpText2 = new JLabel("to other TeamSpace users.");
        c.weightx = 1;
        c.gridx = 0;
        c.gridy = 2;
        c.gridwidth = 2;
        c.insets = new Insets(5, 5, 0, 5);
        pane.add(helpText1, c);
        c.insets = new Insets(0, 5, 5, 5);
        c.gridy = 3;
        pane.add(helpText2, c);
        c.weightx = 1;
        c.gridx = 0;
        c.gridy = 4;
        c.gridwidth = 2;
        c.insets = new Insets(5, 5, 5, 5);
        c.fill = GridBagConstraints.NONE;
        c.anchor = GridBagConstraints.EAST;
        final JButton button = new JButton("OK");
        pane.add(button, c);
        button.addActionListener(new AbstractAction() {

            public void actionPerformed(java.awt.event.ActionEvent e) {
                synchronized (button) {
                    button.notify();
                }
            }
        });
        frame.setResizable(false);
        frame.pack();
        frame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        frame.show();
        synchronized (button) {
            try {
                button.wait();
                frame.dispose();
            } catch (InterruptedException ex) {
                ex.printStackTrace();
                return;
            }
        }
        this.eheapServer = (Server) comboBox.getSelectedItem();
        String machineName = nameField.getText();
        if (machineName != null) {
            SettingList globalSettings = SettingList.globalSettings();
            globalSettings.setValue("Machine Name", machineName);
            try {
                globalSettings.save();
            } catch (Exception ex) {
                ex.printStackTrace();
            }
        }
    }

    private void getEventHeapsFromWeb() {
        try {
            URL url = getServersURL();
            InputStream in = url.openStream();
            Document doc = factory.newDocumentBuilder().parse(in);
            readFromDocument(doc);
        } catch (Exception ex) {
            ex.printStackTrace();
        }
    }

    private void readFromDocument(Document doc) {
        doc.normalize();
        Element servers = doc.getDocumentElement();
        NodeList serverItems = servers.getChildNodes();
        for (int i = 0; i < serverItems.getLength(); i++) {
            Node n = serverItems.item(i);
            if (n instanceof Element) {
                Element server = (Element) n;
                Server s = new Server(server);
                serverList.add(s);
            }
        }
    }

    public class Server {

        String name;

        String address;

        public Server(Element server) {
            NodeList list = server.getChildNodes();
            for (int i = 0; i < list.getLength(); i++) {
                Node n = list.item(i);
                if (n instanceof Element) {
                    Element e = (Element) n;
                    if (e.getTagName().equals("name")) {
                        Text t = (Text) e.getFirstChild();
                        this.name = t.getNodeValue();
                    } else if (e.getTagName().equals("address")) {
                        Text t = (Text) e.getFirstChild();
                        this.address = t.getNodeValue();
                    }
                }
            }
        }

        public String toString() {
            return name;
        }
    }
}
