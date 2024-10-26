package ac.hiu.j314.vesma;

import ac.hiu.j314.elmve.*;
import java.awt.*;
import java.awt.event.*;
import javax.swing.*;
import javax.swing.event.*;
import java.util.*;
import java.net.*;
import java.io.*;
import org.w3c.dom.*;

public class VesmaLauncher implements ActionListener, ChangeListener {

    JFrame frame;

    Box baseBox;

    JButton okButton;

    JButton cancelButton;

    JCheckBox rmiregistryCB;

    JTextField serverNameTF;

    JComboBox clientClassCB;

    JButton clientRemoveButton;

    JCheckBox emptyCB;

    JTextField roomFileTF;

    JCheckBox bridgeCB;

    JTextField bridgeConfFileTF;

    JTextField bridgeOutIPTF;

    JTextField bridgeInIPTF;

    JButton autoButton;

    JPasswordField ksPasswordPF;

    protected boolean java3d = true;

    protected boolean jmf = true;

    protected boolean jaxp = true;

    protected boolean xj3d = true;

    String confFile;

    Document confDoc;

    protected void check() {
        try {
            Class c = Class.forName("javax.media.j3d.Node");
        } catch (ClassNotFoundException e) {
            java3d = false;
        }
        try {
            Class c = Class.forName("javax.media.Player");
        } catch (ClassNotFoundException e) {
            jmf = false;
        }
        try {
            Class c = Class.forName("javax.xml.parsers.DocumentBuilderFactory");
        } catch (ClassNotFoundException e) {
            jaxp = false;
        }
        try {
            Class c = Class.forName("org.web3d.vrml.lang.VRMLNode");
        } catch (ClassNotFoundException e) {
            xj3d = false;
        }
    }

    protected void makeUI() {
        frame = new JFrame("VesmaLauncher");
        baseBox = Box.createVerticalBox();
        frame.getContentPane().add(baseBox);
        Box box0 = Box.createVerticalBox();
        box0.add(new JLabel(java3d ? "Java3D --- OK!" : "Java3D --- NO!"));
        box0.add(new JLabel(jmf ? "jmf --- OK!" : "jmf --- NO!"));
        box0.add(new JLabel(jaxp ? "jaxp --- OK!" : "jaxp --- NO!"));
        box0.add(new JLabel(xj3d ? "xj3d --- OK!" : "xj3d --- NO!"));
        Box box1 = Box.createHorizontalBox();
        rmiregistryCB = new JCheckBox("rmiregistry:");
        box1.add(rmiregistryCB);
        Box box2 = Box.createHorizontalBox();
        box2.add(new JLabel("Server Name:"));
        serverNameTF = new JTextField();
        box2.add(serverNameTF);
        Box box3 = Box.createHorizontalBox();
        box3.add(new JLabel("Client Class:"));
        clientClassCB = new JComboBox();
        clientClassCB.setEditable(true);
        box3.add(clientClassCB);
        clientRemoveButton = new JButton("rm");
        clientRemoveButton.addActionListener(this);
        box3.add(clientRemoveButton);
        Box box4 = Box.createHorizontalBox();
        emptyCB = new JCheckBox("Disable process of loading VE file:");
        box4.add(emptyCB);
        emptyCB.addChangeListener(this);
        Box box5 = Box.createHorizontalBox();
        box5.add(new JLabel("VE File:"));
        roomFileTF = new JTextField();
        box5.add(roomFileTF);
        Box box6 = Box.createVerticalBox();
        bridgeCB = new JCheckBox("Enable ElmBridge:");
        box6.add(bridgeCB);
        Box box6_1 = Box.createHorizontalBox();
        box6_1.add(new JLabel("conf:"));
        bridgeConfFileTF = new JTextField();
        box6_1.add(bridgeConfFileTF);
        Box box6_2 = Box.createHorizontalBox();
        box6_2.add(new JLabel("out:"));
        bridgeOutIPTF = new JTextField();
        box6_2.add(bridgeOutIPTF);
        autoButton = new JButton("auto");
        autoButton.addActionListener(this);
        box6_2.add(autoButton);
        Box box6_3 = Box.createHorizontalBox();
        box6_3.add(new JLabel("in:"));
        bridgeInIPTF = new JTextField();
        box6_3.add(bridgeInIPTF);
        box6.add(box6_1);
        box6.add(box6_2);
        box6.add(box6_3);
        Box box7 = Box.createHorizontalBox();
        box7.add(new JLabel("keystorePassword"));
        ksPasswordPF = new JPasswordField();
        box7.add(ksPasswordPF);
        Box box8 = Box.createHorizontalBox();
        okButton = new JButton("OK");
        okButton.addActionListener(this);
        box8.add(okButton);
        cancelButton = new JButton("Cancel");
        cancelButton.addActionListener(this);
        box8.add(cancelButton);
        baseBox.add(box0);
        baseBox.add(box1);
        baseBox.add(box2);
        baseBox.add(box3);
        baseBox.add(box4);
        baseBox.add(box5);
        baseBox.add(box6);
        baseBox.add(box7);
        baseBox.add(box8);
    }

    void packAndShow() {
        frame.pack();
        Dimension d = frame.getSize();
        GraphicsConfiguration gc = frame.getGraphicsConfiguration();
        Rectangle b = gc.getBounds();
        frame.setLocation(b.x + b.width / 2 - d.width / 2, b.y + b.height / 2 - d.height / 2);
        frame.setVisible(true);
        okButton.requestFocus();
    }

    boolean bbb(String s) {
        return (new Boolean(s)).booleanValue();
    }

    protected void loadConfigure() {
        try {
            confFile = System.getProperty("user.home");
            confFile = confFile + W.sepa + ".elm" + W.sepa + "vesma.conf";
            URL url = W.getResource(confFile);
            InputStream is = url.openStream();
            confDoc = W.loadDocumentFromInputStreamDOM(is);
            Element e = confDoc.getDocumentElement();
            Element ee = W.getChildByTagNameDOM(e, "rmiregistry");
            String s = W.getAttrDataDOM(ee, "enable");
            rmiregistryCB.setSelected(bbb(s));
            s = W.getDataDOM(e, "serverName");
            serverNameTF.setText(s);
            ee = W.getChildByTagNameDOM(e, "clients");
            Element eee = null;
            ArrayList<String> tmpAl = new ArrayList<String>();
            String defaultClient = null;
            ArrayList al = W.getChildrenByTagNameDOM(ee, "client");
            Iterator i = al.iterator();
            while (i.hasNext()) {
                eee = (Element) i.next();
                String clientString = W.getDataDOM(eee);
                tmpAl.add(clientString);
                String defaultString = W.getAttrDataDOM(eee, "default");
                if (defaultString.equals("true")) {
                    defaultClient = clientString;
                }
            }
            String ss[] = (String[]) tmpAl.toArray(new String[0]);
            clientClassCB.removeAllItems();
            for (int ii = 0; ii < ss.length; ii++) clientClassCB.addItem(ss[ii]);
            clientClassCB.setSelectedItem(defaultClient);
            ee = W.getChildByTagNameDOM(e, "loadVE");
            s = W.getAttrDataDOM(ee, "enable");
            emptyCB.setSelected(bbb(s) == false);
            s = W.getAttrDataDOM(ee, "file");
            roomFileTF.setText(s);
            ee = W.getChildByTagNameDOM(e, "elmBridge");
            s = W.getAttrDataDOM(ee, "enable");
            bridgeCB.setSelected(bbb(s));
            s = W.getAttrDataDOM(ee, "confFile");
            bridgeConfFileTF.setText(s);
            ee = W.getChildByTagNameDOM(e, "outIPAddress");
            s = W.getDataDOM(ee);
            bridgeOutIPTF.setText(s);
            bridgeInIPTF.setText(W.getIPAddress());
        } catch (Exception ee) {
            makeDefaultConfiguration();
            saveConfiguration();
            System.out.println("A default config file was created.");
        }
        packAndShow();
    }

    protected void makeDefaultConfiguration() {
        rmiregistryCB.setSelected(true);
        serverNameTF.setText("VESMA");
        clientClassCB.removeAllItems();
        clientClassCB.addItem("ac.hiu.j314.vesma.Vesma");
        clientClassCB.addItem("ac.hiu.j314.vesma.Vesma3d");
        clientClassCB.addItem("ac.hiu.j314.vesma.VesmaLight");
        clientClassCB.addItem("ac.hiu.j314.elmve.clients.Elm2DClient");
        clientClassCB.addItem("ac.hiu.j314.elmve.clients.Elm3DClient");
        clientClassCB.addItem("ac.hiu.j314.elmve.clients.ElmLightClient");
        clientClassCB.setSelectedItem("ac.hiu.j314.vesma.Vesma3d");
        emptyCB.setSelected(false);
        roomFileTF.setText("");
        bridgeCB.setSelected(false);
        bridgeConfFileTF.setText("x-res:///ac/hiu/j314/elmve/resources/bridge.conf");
        bridgeOutIPTF.setText("");
        bridgeInIPTF.setText(W.getIPAddress());
    }

    protected void saveConfiguration() {
        try {
            FileOutputStream fos = new FileOutputStream(confFile);
            confDoc = W.makeEmptyDocumentDOM();
            Element e = W.makeElementDOM(confDoc, "conf");
            W.addChildDOM(confDoc, e);
            W.addLineFeedDOM(confDoc, e);
            Element ee = W.makeElementDOM(confDoc, "rmiregistry");
            boolean b = rmiregistryCB.isSelected();
            W.addAttrDOM(confDoc, ee, "enable", (b ? "true" : "false"));
            W.addChildDOM(e, ee);
            W.addLineFeedDOM(confDoc, e);
            ee = W.addDataDOM(confDoc, e, "serverName", serverNameTF.getText());
            W.addChildDOM(e, ee);
            W.addLineFeedDOM(confDoc, e);
            ee = W.makeElementDOM(confDoc, "clients");
            W.addLineFeedDOM(confDoc, ee);
            String s = (String) clientClassCB.getSelectedItem();
            Element eee = null;
            boolean tmpFlag = false;
            for (int i = 0; i < clientClassCB.getItemCount(); i++) {
                String ss = (String) clientClassCB.getItemAt(i);
                if (ss.equals("")) continue;
                eee = W.makeElementDOM(confDoc, "client");
                if (ss.equals(s)) {
                    W.addAttrDOM(confDoc, eee, "default", "true");
                    tmpFlag = true;
                }
                W.addDataDOM(confDoc, eee, ss);
                W.addChildDOM(ee, eee);
                W.addLineFeedDOM(confDoc, ee);
            }
            if (tmpFlag == false) {
                if (!s.equals("")) {
                    eee = W.makeElementDOM(confDoc, "client");
                    W.addAttrDOM(confDoc, eee, "default", "true");
                    W.addDataDOM(confDoc, eee, s);
                    W.addChildDOM(ee, eee);
                    W.addLineFeedDOM(confDoc, ee);
                }
            }
            W.addChildDOM(e, ee);
            W.addLineFeedDOM(confDoc, e);
            ee = W.makeElementDOM(confDoc, "loadVE");
            b = emptyCB.isSelected();
            W.addAttrDOM(confDoc, ee, "enable", (b ? "false" : "true"));
            W.addAttrDOM(confDoc, ee, "file", roomFileTF.getText());
            W.addChildDOM(e, ee);
            W.addLineFeedDOM(confDoc, e);
            ee = W.makeElementDOM(confDoc, "elmBridge");
            b = bridgeCB.isSelected();
            W.addAttrDOM(confDoc, ee, "enable", (b ? "true" : "false"));
            W.addAttrDOM(confDoc, ee, "confFile", bridgeConfFileTF.getText());
            W.addChildDOM(e, ee);
            W.addLineFeedDOM(confDoc, e);
            ee = W.addDataDOM(confDoc, e, "outIPAddress", bridgeOutIPTF.getText());
            W.addChildDOM(e, ee);
            W.addLineFeedDOM(confDoc, e);
            W.saveDocumentToOutputStreamDOM(confDoc, fos);
        } catch (Exception e) {
            e.printStackTrace();
            return;
        }
    }

    public void actionPerformed(ActionEvent ae) {
        if (ae.getSource() == okButton) {
            saveConfiguration();
            startElmRMIRegistry();
            startElmBridge();
            startElmVE();
        } else if (ae.getSource() == autoButton) {
            setOuterIP();
        } else if (ae.getSource() == cancelButton) {
            cancel();
        } else if (ae.getSource() == clientRemoveButton) {
            int i = clientClassCB.getSelectedIndex();
            if (i != -1) clientClassCB.removeItemAt(i); else clientClassCB.setSelectedItem("");
        }
    }

    public void stateChanged(ChangeEvent ce) {
        if (emptyCB.isSelected()) roomFileTF.setEditable(false); else roomFileTF.setEditable(true);
    }

    protected void startElmRMIRegistry() {
        if (!rmiregistryCB.isSelected()) return;
        ElmRMIRegistry.main(null);
    }

    protected void startElmBridge() {
        if (!bridgeCB.isSelected()) return;
        ArrayList<String> al = new ArrayList<String>();
        String confFile = bridgeConfFileTF.getText();
        if (!confFile.equals("")) {
            al.add("--conf");
            al.add(confFile);
        }
        String outIP = bridgeOutIPTF.getText();
        if (!outIP.equals("")) {
            al.add("--outer");
            al.add(outIP);
        }
        String inIP = bridgeInIPTF.getText();
        if (!inIP.equals("")) {
            al.add("--inner");
            al.add(inIP);
        }
        String args[] = (String[]) al.toArray(new String[0]);
        try {
            ElmBridge eb = new ElmBridge(args);
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    protected void startElmVE() {
        ElmVE.main(makeArgs());
        frame.dispose();
    }

    protected String[] makeArgs() {
        ArrayList<String> al = new ArrayList<String>();
        al.add("--name");
        al.add(serverNameTF.getText());
        al.add("--client");
        al.add((String) clientClassCB.getSelectedItem());
        if (emptyCB.isSelected()) {
            al.add("--empty");
        } else {
            String rf = roomFileTF.getText();
            rf = rf.trim();
            if (!rf.equals("")) {
                al.add("--room");
                al.add(rf);
            }
        }
        String pw = new String(ksPasswordPF.getPassword());
        pw = pw.trim();
        if (!pw.equals("")) {
            al.add("--keystorepassword");
            al.add(pw);
        }
        return (String[]) al.toArray(new String[0]);
    }

    protected void cancel() {
        System.exit(0);
    }

    protected void setOuterIP() {
        try {
            URL url = new URL("http://elm-ve.sf.net/ipCheck/ipCheck.cgi");
            InputStreamReader isr = new InputStreamReader(url.openStream());
            BufferedReader br = new BufferedReader(isr);
            String ip = br.readLine();
            ip = ip.trim();
            bridgeOutIPTF.setText(ip);
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    public static void main(String args[]) {
        VesmaLauncher launcher = new VesmaLauncher();
        launcher.check();
        launcher.makeUI();
        launcher.loadConfigure();
    }
}
