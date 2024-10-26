package ca.nrc.iit.johnnyvon.launch;

import ca.nrc.iit.johnnyvon.engine.*;
import ca.nrc.iit.johnnyvon.gui.*;
import java.io.BufferedReader;
import java.io.InputStream;
import java.io.IOException;
import java.io.StringReader;
import java.io.Reader;
import javax.swing.JFrame;
import java.awt.event.WindowAdapter;
import java.awt.event.WindowEvent;
import java.util.Properties;
import java.net.URL;

/** An application launcher for JohnnyVon.  Reads command line parameters,
 * then tries to load the input from a default location, lastly falling back
 * onto a compiled-in default configuration.  
 *
 * @author <a href="mailto:rob@infinitepigeons.org">Rob Ewaschuk</a>, 
 * <a href="mailto:arnold.smith@nrc.ca">Arnold Smith</a>, 
 * <a href="mailto:peter.turney@nrc.ca">Peter Turney</a>
 * @version 2.0  Copyright &copy; 2002-2004 National Research Council Canada
 */
public class JohnnyVonApplication {

    public static void main(String[] args) {
        try {
            Properties properties;
            boolean stdin = false;
            if (args.length >= 1 && (args[0].equals("--read"))) {
                System.out.print("Reading data from standard in..");
                stdin = true;
                properties = new Properties();
                properties.load(System.in);
            } else {
                URL url = JohnnyVonApplication.class.getClassLoader().getResource("support/input.txt");
                if (url != null) {
                    properties = new Properties();
                    properties.load(url.openStream());
                } else {
                    System.out.println("Using default data.  Run with --read to read data.");
                    properties = JohnnyVonDisplay.DEFAULTS;
                }
            }
            JFrame frame = new JohnnyVonDisplay(properties, new JohnnyVonDisplay.Closer() {

                public void close(JohnnyVonDisplay display) {
                    System.exit(0);
                }
            });
            if (stdin) System.out.println(".done.");
            frame.addWindowListener(new WindowAdapter() {

                public void windowClosing(WindowEvent we) {
                    System.exit(0);
                }
            });
        } catch (Exception e) {
            System.out.println("Failed.  See below for details. (" + e + ")");
            e.printStackTrace();
        }
    }
}
