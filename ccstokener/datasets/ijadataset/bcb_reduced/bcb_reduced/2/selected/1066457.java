package org.lindenb.foafexplorer;

import java.awt.Dimension;
import java.awt.Point;
import java.awt.Toolkit;
import java.io.InputStream;
import java.net.URL;
import javax.swing.JApplet;
import javax.swing.JDialog;
import javax.swing.JFrame;
import javax.swing.UIManager;
import javax.swing.plaf.metal.MetalLookAndFeel;
import org.lindenb.lib.lang.ExceptionPane;

/**
 * @author pierre
 *
 * TODO To change the template for this generated type comment go to
 * Window - Preferences - Java - Code Style - Code Templates
 */
public class FOAFApplet extends JApplet {

    private Main main;

    private Model model;

    private static final long serialVersionUID = 1L;

    public void init() {
        try {
            String version = System.getProperty("java.specification.version");
            if (version != null) {
                if ((10.0 * Double.parseDouble(version)) < 15.0) {
                    throw new RuntimeException("Bad JAVA version : found \"" + version + "\" but expected >= 1.5.\nSee http://java.sun.com/j2se/1.5.0/download.jsp for more informations");
                }
            }
            String s = getParameter("foaf");
            if (s == null) throw new NullPointerException("parameter missing");
            URL url = new URL(getDocumentBase(), s);
            showStatus("Loading " + url.toString() + "... Please WAIT");
            InputStream in = url.openStream();
            this.model = Model.load(in);
            this.main = new Main(this.model) {

                private static final long serialVersionUID = 1L;

                public void activateURL(URL url) {
                    getAppletContext().showDocument(url, "" + System.currentTimeMillis());
                }
            };
            setContentPane(main);
        } catch (Throwable e) {
            setContentPane(new ExceptionPane(e));
        }
    }

    /** @see java.applet.Applet#start() */
    public void start() {
        if (this.model == null) return;
        Point c = this.model.getIndividuals().getCenter();
        this.main.focusOnPoint(c.x, c.y);
    }

    /** @see java.applet.Applet#getAppletInfo() */
    public String getAppletInfo() {
        return "Explores a FOAF network";
    }

    public String[][] getParameterInfo() {
        return new String[][] { { "foaf", "the FOAF file" } };
    }

    /**
 * @param args
 */
    public static void main(String[] args) {
        try {
            UIManager.setLookAndFeel(new MetalLookAndFeel());
            JFrame.setDefaultLookAndFeelDecorated(true);
            JDialog.setDefaultLookAndFeelDecorated(true);
            JFrame frame = new JFrame("FOAFApplet");
            JFrame.setDefaultLookAndFeelDecorated(true);
            JDialog.setDefaultLookAndFeelDecorated(true);
            FOAFApplet m = new FOAFApplet();
            Dimension screen = Toolkit.getDefaultToolkit().getScreenSize();
            frame.setBounds(50, 50, screen.width - 100, screen.height - 100);
            frame.setVisible(true);
            frame.setContentPane(m);
        } catch (Exception e) {
            e.printStackTrace();
        }
    }
}
