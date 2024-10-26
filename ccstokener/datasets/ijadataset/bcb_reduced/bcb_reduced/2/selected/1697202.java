package PRISM.VRWGui;

import java.io.BufferedInputStream;
import java.io.FileInputStream;
import java.io.InputStream;
import java.net.URL;
import java.util.StringTokenizer;
import javax.swing.JApplet;
import javax.swing.JButton;
import javax.swing.JLabel;
import netscape.javascript.JSObject;
import vrml.external.Browser;
import PRISM.RobotCtrl.EnvironmentMap;
import PRISM.VRW.VRWClient;
import PRISM.VRW.VRWReceiver;
import PRISM.VRW.VRWWRLRenderer;

public class VRWConsole extends JApplet {

    VRWClient m_VRWClient = null;

    VRWConsoleFrame m_frmVRWConsole = null;

    VRWWRLRenderer m_wrlRenderer = null;

    Browser m_browser = null;

    String m_strWorld = null;

    String m_strHost = null;

    JButton button;

    Thread windowThread;

    JLabel label;

    boolean pleaseShow = false;

    boolean shouldInitialize = true;

    JSObject jsobj;

    Object msgArray[] = new Object[1];

    static VRWConsole refApplet = null;

    VRWImageRetriever imageRetriever = new VRWImageRetriever();

    class VRWJSSender implements VRWReceiver {

        public void notify(String cmd) {
            try {
                msgArray[0] = cmd;
                jsobj.call("sendVRWMsg", msgArray);
            } catch (Throwable t) {
                System.out.println("Exception calling java script method sendVRWMsg() " + t.getMessage());
            }
        }
    }

    public URL getURL(String filename) {
        URL url = null;
        try {
            url = new URL(getCodeBase(), filename);
        } catch (java.net.MalformedURLException e) {
            System.out.println("Couldn't create image: badly specified URL");
            return null;
        }
        return url;
    }

    public void init() {
        System.out.println("Init applet...");
        int port = Integer.parseInt(getParameter("port"));
        int useUDP = Integer.parseInt(getParameter("udp"));
        boolean bUseUDP = false;
        if (useUDP > 0) bUseUDP = true;
        m_strWorld = getParameter("world");
        m_strHost = this.getCodeBase().getHost();
        try {
            new EnvironmentMap(getParameter("vrwmap"));
        } catch (Throwable t) {
            System.out.println(t.getMessage());
        }
        URL urlExperiment = null;
        InputStream expStream = null;
        try {
            String strPathExperiment = getParameter("experiment");
            if (strPathExperiment.length() > 0) {
                urlExperiment = new URL(getCodeBase(), strPathExperiment);
                expStream = urlExperiment.openStream();
            }
        } catch (java.net.MalformedURLException e) {
            System.out.println("Couldn't open url experiment: badly specified URL " + e.getMessage());
        } catch (Throwable t) {
            System.out.println("Couldn't open url experiment: " + t.getMessage());
        }
        try {
            System.out.println("Creating client, logging to " + m_strWorld);
            m_VRWClient = new VRWClient(m_strHost, port, true, bUseUDP);
            m_VRWClient.setInApplet(true);
            m_VRWClient.login(m_strWorld);
        } catch (java.io.IOException e) {
            System.out.println("IOException creating the VRWClient");
        }
        try {
            jsobj = JSObject.getWindow(this);
        } catch (Throwable t) {
            System.out.println("Exception getting Java Script Interface: " + t.getMessage());
        }
        refApplet = this;
        m_frmVRWConsole = new VRWConsoleFrame();
        m_frmVRWConsole.setTitle("VRW Client Console");
        m_frmVRWConsole.pack();
        m_frmVRWConsole.setSize(Math.max(300, m_frmVRWConsole.getSize().width), Math.max(200, m_frmVRWConsole.getSize().height));
        if (expStream != null) {
            System.out.println("Passing experiment stream to VRWConsoleFrame");
            m_frmVRWConsole.loadExperiment(expStream);
        }
        m_frmVRWConsole.setVisible(true);
    }

    public static synchronized VRWConsole getConsole() {
        if (refApplet == null) {
            System.out.println("ERROR, VRWConsole not initialised !");
        }
        return refApplet;
    }

    public void start() {
        VRWConsoleUpdater updaterC = new VRWConsoleUpdater((VRWConsoleFrame) m_frmVRWConsole);
        m_VRWClient.register_observer(updaterC);
        VRWJSSender jscomm = new VRWJSSender();
        m_VRWClient.register_receiver(jscomm);
    }

    public void stop() {
    }

    public static void main(String args[]) {
        new VRWImageRetriever();
        if (args.length < 2) {
            System.out.println("Usage: java VRWConsole <hostname> <world> [port]");
            return;
        }
        VRWClient m_VRWClient = null;
        int port = 4445;
        boolean useUDP = true;
        if (args.length == 3) {
            port = Integer.parseInt(args[2]);
            if (port == 4447) useUDP = false;
        }
        try {
            m_VRWClient = new VRWClient(args[0], port, true, useUDP);
        } catch (java.io.IOException e) {
            System.out.println(e.getMessage());
            System.out.println("Exception thrown while creating VRWClient");
            System.out.println("Exit");
            return;
        }
        String mapFile = "";
        StringBuffer strBuf = new StringBuffer();
        try {
            StringTokenizer t = new StringTokenizer(args[1], ".");
            mapFile = t.nextToken() + ".map";
            InputStream in = new BufferedInputStream(new FileInputStream(mapFile));
            int c;
            while ((c = in.read()) != -1) strBuf.append((char) c);
            in.close();
            new EnvironmentMap(strBuf.toString());
        } catch (Throwable t) {
            System.out.println("Impossible loading environment map " + mapFile);
            System.out.println(t.getMessage());
        }
        VRWConsoleFrame window = new VRWConsoleFrame();
        VRWConsoleUpdater updater = new VRWConsoleUpdater(window);
        m_VRWClient.register_observer(updater);
        m_VRWClient.login(args[1]);
        window.setTitle("VRW Client Console");
        window.pack();
        window.setVisible(true);
    }
}
