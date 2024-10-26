package guie.applet;

import guie.Guie;
import guie.GuieDef;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.net.MalformedURLException;
import java.net.URL;
import javax.swing.JApplet;
import javax.swing.JButton;
import javax.swing.JFrame;
import javax.swing.SwingUtilities;
import javax.swing.ToolTipManager;
import nanoxml.XMLElement;

/**
  * Applet mainly for demonstration purposes.
  * Expected parameter: "guie"  
  * 		   
  * @author Carlos Rueda
  * @version $Id: GuieApplet.java,v 1.3 2008/07/05 22:55:20 carueda Exp $
 */
public class GuieApplet extends JApplet {

    private static final long serialVersionUID = 1L;

    private String guie_filename;

    private GuieDef guieDef;

    private Guie guie;

    private JButton jbutton;

    public void init() {
        guie_filename = getParameter("guie");
        URL guie_url = null;
        try {
            guie_url = new URL(getCodeBase(), guie_filename);
        } catch (MalformedURLException ex) {
            System.out.println(ex.getMessage());
        }
        try {
            BufferedReader br = new BufferedReader(new InputStreamReader(guie_url.openStream()));
            XMLElement xe = new XMLElement();
            xe.parseFromReader(br);
            guieDef = new GuieDef(xe);
            System.out.println("GuieApplet init");
        } catch (Exception ex) {
            System.out.println("Error creating GuieDef: " + ex.getMessage());
            return;
        }
        try {
            SwingUtilities.invokeAndWait(new Runnable() {

                public void run() {
                    jbutton = new JButton("Click here to launch the GuiE demo");
                    getContentPane().add(jbutton);
                    Guie.setLookAndFeel();
                    ToolTipManager.sharedInstance().setDismissDelay(5 * 60 * 1000);
                    guie = new Guie(guieDef, new JFrame());
                    jbutton.addActionListener(new ActionListener() {

                        public void actionPerformed(ActionEvent _) {
                            SwingUtilities.invokeLater(new Runnable() {

                                public void run() {
                                    if (guie != null) {
                                        guie.getFrame().setVisible(true);
                                    }
                                }
                            });
                        }
                    });
                }
            });
        } catch (Exception e) {
            System.out.println("cannot create GUI!");
        }
    }

    public void stop() {
        if (guie != null) {
            guie.getFrame().dispose();
            guie = null;
        }
        System.out.println("GuieApplet stopped");
    }
}
