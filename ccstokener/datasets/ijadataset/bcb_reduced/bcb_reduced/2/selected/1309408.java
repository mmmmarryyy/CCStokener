package ElementDesigner;

import java.io.FileInputStream;
import java.net.URL;
import java.util.Properties;
import javax.swing.JApplet;
import javax.swing.JOptionPane;

/**
 * This class includes the implementation of the properties which are used for
 * the localisation of the application <b>Dia.Logis</b>
 * 
 * @author Michael G�rtner
 * @version 1.3 - 02.2008
 */
public class I18N implements LK_Const {

    private static final long serialVersionUID = 1L;

    public static Properties prop = null;

    /**
     * Creates a new instance of I18N
     * 
     * @param applet
     *            JApplet
     */
    public I18N(JApplet applet) {
        if (prop != null) {
            return;
        }
        String lang = "de";
        try {
            Properties userProperties = new Properties();
            if (applet != null) {
                URL url = new URL(applet.getCodeBase() + xConfigPath + "ElementDesigner.cfg");
                userProperties.load(url.openStream());
            } else {
                userProperties.load(new FileInputStream(xConfigPath + "ElementDesigner.cfg"));
            }
            if (userProperties.containsKey("language")) {
                lang = userProperties.getProperty("language");
            }
        } catch (Exception ex) {
            ex.printStackTrace();
        }
        prop = new Properties();
        try {
            if (applet != null) {
                URL url = new URL(applet.getCodeBase() + xLanguagePath + lang + ".ini");
                prop.load(url.openStream());
            } else {
                prop.load(new FileInputStream(xLanguagePath + lang + ".ini"));
            }
        } catch (Exception ex) {
            ex.printStackTrace();
            try {
                if (applet != null) {
                    URL url = new URL(applet.getCodeBase() + xLanguagePath + "de.ini");
                    prop.load(url.openStream());
                } else {
                    prop.load(new FileInputStream(xLanguagePath + "de.ini"));
                }
            } catch (Exception ex2) {
                JOptionPane.showMessageDialog(null, "Language file languages/de.ini not found.\nPlease run the program from its directory.");
                System.exit(5);
            }
        }
    }

    /** Creates a new instance of I18N */
    public I18N() {
        String lang = "de";
        try {
            Properties userProperties = new Properties();
            userProperties.load(new FileInputStream(xConfigPath + "ElementDesigner.cfg"));
            if (userProperties.containsKey("language")) {
                lang = userProperties.getProperty("language");
            }
        } catch (Exception ex) {
        }
        prop = new Properties();
        try {
            prop.load(new FileInputStream(xLanguagePath + lang + ".ini"));
        } catch (Exception ex) {
            try {
                prop.load(new FileInputStream(xLanguagePath + "de.ini"));
            } catch (Exception ex2) {
                JOptionPane.showMessageDialog(null, "Language file languages/de.ini not found.\nPlease run the program from its directory.");
                System.exit(5);
            }
        }
    }

    public static String getString(String key) {
        return prop.getProperty(key);
    }
}
