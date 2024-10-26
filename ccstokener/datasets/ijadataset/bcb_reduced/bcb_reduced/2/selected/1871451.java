package it.uniroma1.dis.omega.gateway;

import it.uniroma1.dis.omega.upnpqos.argument.ArgumentException;
import it.uniroma1.dis.omega.upnpqos.argument.TrafficPolicy;
import it.uniroma1.dis.omega.upnpqos.utils.Validator;
import java.awt.*;
import java.awt.event.*;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.MalformedURLException;
import java.net.URL;
import org.cybergarage.upnp.Argument;
import org.cybergarage.upnp.device.InvalidDescriptionException;
import org.osgi.framework.BundleContext;
import org.osgi.framework.BundleException;

public class OGFrame extends Frame implements WindowListener {

    private static final long serialVersionUID = 1L;

    private static final String DESCRIPTION_FILE_URL = "/description/description.xml";

    private static final String TITLE = "UPnP QoS Omega Gateway";

    private OGDevice ogDev;

    private BundleContext bc;

    private TextArea ta = new TextArea();

    public OGFrame(BundleContext bc) {
        super(TITLE);
        this.bc = bc;
        initGraph();
        setDevice();
        setIcon();
    }

    private void initGraph() {
        addWindowListener(this);
        setSize(100, 100);
        add(ta);
        pack();
        setVisible(true);
    }

    private void setDevice() {
        try {
            File f = new File("og_description.xml");
            try {
                InputStream inputStream = getClass().getResourceAsStream(DESCRIPTION_FILE_URL);
                OutputStream out = new FileOutputStream(f);
                byte buf[] = new byte[1024];
                int len;
                while ((len = inputStream.read(buf)) > 0) out.write(buf, 0, len);
                out.close();
                inputStream.close();
            } catch (IOException e) {
            }
            ogDev = new OGDevice(f);
        } catch (InvalidDescriptionException e) {
            append("\n" + e.toString());
            e.printStackTrace();
        }
    }

    private void setIcon() {
        try {
            Image icon = null;
            String localPath = "/images/Icon16x16.png";
            URL url = getClass().getResource(localPath);
            if (url != null) {
                icon = Toolkit.getDefaultToolkit().createImage(url);
                setIconImage(icon);
            } else {
            }
        } catch (Exception e) {
            append("\n" + e.toString());
        }
    }

    private void test() {
        String xmlUrlStr = "http://localhost:8080/xml/TrafficPolicy1.xml";
        try {
            URL xmlUrl = new URL(xmlUrlStr);
            try {
                String xmlStr = "";
                InputStream is = xmlUrl.openStream();
                byte[] buffer = new byte[1024];
                while (is.read(buffer) > 0) {
                    String str = new String(buffer);
                    xmlStr += str;
                }
                xmlStr = xmlStr.trim();
                xmlStr = "<TrafficPolicy xmlns=\"http://www.upnp.org/schemas/TrafficPolicy.xsd\" " + "xmlns:xsi=\"http://www.w3.org/2001/XMLSchema-instance\" " + "xsi:schemaLocation=\"http://www.upnp.org/schemas/TrafficPolicy.xsd " + "http://www.upnp.org/schemas/qos/TrafficPolicy-v2.xsd\">" + "<AdmissionPolicy>Enabled</AdmissionPolicy>" + "<TrafficImportanceNumber>5</TrafficImportanceNumber>" + "<UserImportanceNumber>45</UserImportanceNumber>" + "<v2>" + "<PolicyHolderId>uuid:2fac1234-31f8-11b4-a222-08002b34c003:urn:upnporg:serviceId:QosPolicyHolder-3b</PolicyHolderId>" + "<PolicyHolderConfigUrl>http://10.0.0.50/ConfigPolicy.html</PolicyHolderConfigUrl>" + "</v2></TrafficPolicy>";
                System.out.println(xmlStr);
                Argument arg = new Argument();
                arg.setValue(xmlStr);
                arg.setName("A_ARG_TYPE_TrafficPolicy");
                try {
                    TrafficPolicy tp = new TrafficPolicy(arg);
                    System.out.println("\n\n\n ALLELUJA!!! \n\n\n" + tp.toString());
                } catch (ArgumentException AE) {
                    AE.printStackTrace();
                }
            } catch (IOException e) {
                e.printStackTrace();
            }
        } catch (MalformedURLException e) {
            e.printStackTrace();
        }
    }

    public OGDevice getOgDevice() {
        return ogDev;
    }

    private void append(String str) {
        ta.setText(ta.getText() + str);
    }

    private void append(URL url) {
        try {
            InputStream is = url.openStream();
            byte[] buffer = new byte[1024];
            while (is.read(buffer) > 0) {
                append(new String(buffer));
            }
        } catch (IOException IOE) {
            append(IOE.toString());
        }
    }

    public void start() {
        ogDev.start();
    }

    public void stop() {
        ogDev.stop();
    }

    public void windowActivated(WindowEvent e) {
    }

    public void windowClosed(WindowEvent e) {
    }

    public void windowClosing(WindowEvent e) {
        ogDev.stop();
        stop();
        try {
            bc.getBundle().stop();
        } catch (BundleException BE) {
        }
    }

    public void windowDeactivated(WindowEvent e) {
    }

    public void windowDeiconified(WindowEvent e) {
    }

    public void windowIconified(WindowEvent e) {
    }

    public void windowOpened(WindowEvent e) {
    }
}
