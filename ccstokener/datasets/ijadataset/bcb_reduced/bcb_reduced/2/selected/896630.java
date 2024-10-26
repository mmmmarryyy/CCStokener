package risk.resources;

import java.awt.Image;
import java.awt.MediaTracker;
import java.awt.Toolkit;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.io.OutputStream;
import java.net.URL;
import java.text.MessageFormat;
import java.util.ArrayList;
import java.util.Locale;
import java.util.Properties;
import java.util.ResourceBundle;
import javax.swing.KeyStroke;
import risk.ui.gui.player.ImagePlayer;

/** This class provides access to all resources needed by JRisk.
 * It uses a {@link java.awt.MediaTracker} to load all images.
 *
 * <pre>
 * CVS information:
 * $Source: /cvsroot/javarisk/version2/src/de/javaRisk/v2/resources/Resources.java,v $
 * $Revision: 1.18 $
 * $Author: sebastiankirsch $
 * </pre>
 * @author Sebastian Kirsch
 * @version 0.93
 * @since 0.7
 * @see de.javaRisk.v2.JRisk
 */
public class Resources {

    /** The <code>ResourceBundle</code> for all messages. */
    private static ResourceBundle MESSAGES = ResourceBundle.getBundle("risk.resources.Messages");

    /** Represents the directory in which all changeable data is stored.
     * @since 0.85
     */
    private static final File STORE = new File("JRiskData");

    /** A static instance to get the <CODE>Class</CODE>.
     * @since 0.81
     */
    private static final Resources res = new Resources();

    /** The <code>Locale</code>s available for this game. */
    public static final Locale[] AVAILABLE_LOCALES = new Locale[] { Locale.ENGLISH, Locale.GERMAN };

    /** Refers to the v2-theme.
     * @since 0.7
     */
    private Image logo;

    /** Refers to the background-picture.
     * @since 0.7
     */
    private Image backGround;

    /** Refers to some system poperties.
     * @since 0.7
     */
    private Properties systemProperties;

    /** Creates an <I>empty</I> instance of Resources.</P>
     * The class is used internally to get other resources.
     * @since 0.81
     */
    private Resources() {
    }

    /** Ensure the STORE-directory to be there.
     *
     * @since 0.85
     */
    private void ensureStoreFolder() {
        STORE.mkdir();
    }

    /**
	 * <p>
	 * Initializes the configured locale-settings.
	 * </p>
	 * 
	 * @since 0.93
	 */
    public static void initLocale() {
        setLocale(parseLocale(getGameSettings().getProperty("locale", "en")));
    }

    /**
	 * <p>
	 * Parses the specified string into a locale.
	 * </p>
	 * 
	 * @param locale
	 *            the <code>String</code> to parse
	 * @return the appropriate <code>Locale</code>
	 * @since 0.93
	 */
    public static Locale parseLocale(String locale) {
        String[] elems = locale.split("_");
        Locale l = new Locale(elems[0], elems.length > 1 ? elems[1] : "", elems.length > 2 ? elems[2] : "");
        return l;
    }

    /**
	 * <p>
	 * Returns the message for the specified key.
	 * </p>
	 * 
	 * @param key
	 *            the key identifying a localizable text
	 * @return the localized message <code>String</code>
	 * @since 0.93
	 */
    public static String getMessage(String key) {
        return MESSAGES.getString(key);
    }

    /**
	 * <p>
	 * Returns the formatted message for the specified key and parameter.
	 * </p>
	 * 
	 * @param key
	 *            the key identifying a localizable pattern
	 * @param parameter
	 *            an <code>Object</code> parameter to use for formatting
	 * @return the localized message <code>String</code>
	 * @since 0.93
	 */
    public static String getMessage(String key, Object parameter) {
        return new MessageFormat(MESSAGES.getString(key)).format(new Object[] { parameter });
    }

    /**
	 * <p>
	 * Returns the formatted message for the specified key and parameters.
	 * </p>
	 * 
	 * @param key
	 *            the key identifying a localizable pattern
	 * @param parameter1
	 *            an <code>Object</code> parameter to use for formatting
	 * @param parameter1
	 *            another <code>Object</code> parameter to use for formatting
	 * @return the localized message <code>String</code>
	 * @since 0.93
	 */
    public static String getMessage(String key, Object parameter1, Object parameter2) {
        return new MessageFormat(MESSAGES.getString(key)).format(new Object[] { parameter1, parameter2 });
    }

    /**
	 * <p>
	 * Returns the formatted message for the specified key and parameters.
	 * </p>
	 * 
	 * @param key
	 *            the key identifying a localizable pattern
	 * @param parameters
	 *            an <code>Object</code> array containing the parameters to
	 *            use for formatting
	 * @return the localized message <code>String</code>
	 * @since 0.93
	 */
    public static String getMessage(String key, Object[] parameters) {
        return new MessageFormat(MESSAGES.getString(key)).format(parameters);
    }

    /**
     * <p>
     * Returns the localized mnemonic char for the specified key.
     * </p>
     * 
     * @param key
     *            the key identifying a localizable mnemonic
     * @return the localized mnemonic <code>char</code>
     * @since 0.93
     */
    public static char getMnemonic(String key) {
        return MESSAGES.getString(key).charAt(0);
    }

    /**
     * <p>
     * Returns the localized accelerator key stroke for the specified key.
     * </p>
     * 
     * @param key
     *            the key identifying a localizable accelerator
     * @return the localized accelerator <code>KeyStroke</code>
     * @since 0.93
     */
    public static KeyStroke getAccelerator(String key) {
        return KeyStroke.getKeyStroke(MESSAGES.getString(key));
    }

    /**
	 * <p>
	 * Sets the locale to use throughout the application.
	 * </p>
	 * 
	 * @param l
	 *            the new default <code>Locale</code>
	 * @since 0.93
	 */
    public static void setLocale(Locale l) {
        if (l == null) throw new NullPointerException("The Locale cannot be null!");
        if (Locale.getDefault().equals(l)) return;
        Locale.setDefault(l);
        MESSAGES = ResourceBundle.getBundle("risk.resources.Messages", l);
    }

    /**
	 * Returns all registered classes of <CODE>AiPlayer</CODE>s.
	 * </P>
	 * A <CODE>java.util.Properties</CODE> object will be returned, carrying
	 * the class names as keys and descriptions as values.
	 * 
	 * @return <CODE>Properties</CODE> containing class names and descriptions
	 *         of all <CODE>AiPlayers</CODE>
	 * @see de.javaRisk.v2.game.AiPlayer
	 * @since 0.81
	 */
    public static Properties getAiPlayers() {
        Properties props = new Properties();
        try {
            URL url = res.getClass().getResource("AiPlayers.properties");
            if (url == null) return null;
            InputStream in = url.openStream();
            props.load(in);
            in.close();
        } catch (IOException ioe) {
            return null;
        }
        return props;
    }

    /** Returns the game properties that specify individual settings like the
     *  HTTP-Proxy or the URL for the JRiskNet.</P>
     *
     * @return  <CODE>Properties</CODE> containing the individual game-settings
     * @since   0.9
     */
    public static Properties getGameSettings() {
        Properties props, defaults = new Properties();
        try {
            URL url = res.getClass().getResource("system.properties");
            if (url == null) defaults = null; else {
                InputStream in = url.openStream();
                defaults.load(in);
                in.close();
            }
        } catch (IOException ioe) {
            defaults = null;
        }
        props = new Properties(defaults);
        res.ensureStoreFolder();
        try {
            URL url = STORE.toURL();
            InputStream in = new URL(url.toString() + "/system.properties").openStream();
            props.load(in);
            in.close();
        } catch (IOException ioe) {
            return defaults;
        }
        return props;
    }

    /** Store the specified <CODE>Properties</CODE> as individual game-settings
     *  in the data-folder.</P>
     *
     * @param   settings
     *          an instance of <CODE>Properties</CODE> containing the
     *          inidividual settings
     * @since   0.9
     */
    public static void storeGameSettings(Properties settings) {
        res.ensureStoreFolder();
        File f = new File(STORE, "system.properties");
        try {
            OutputStream out = new FileOutputStream(f);
            settings.store(out, "The individual game settings");
            out.close();
        } catch (IOException ioe) {
        }
    }

    /** Returns the stored <CODE>ImagePlayer</CODE>s that is a pattern for the human player.</P>
     *
     * @return  the stored <CODE>ImagePlayer</CODE>
     * @since   0.85
     */
    public static ImagePlayer getStoredHumanPlayer() {
        res.ensureStoreFolder();
        File[] files = STORE.listFiles();
        ImagePlayer player = null;
        for (int i = 0; i < files.length; i++) {
            try {
                if (!files[i].getName().substring(files[i].getName().lastIndexOf('.')).equalsIgnoreCase(".human")) continue;
                ObjectInputStream in = new ObjectInputStream(new FileInputStream(files[i]));
                player = (ImagePlayer) in.readObject();
                in.close();
                break;
            } catch (IndexOutOfBoundsException ioobe) {
                continue;
            } catch (IOException ioe) {
                continue;
            } catch (ClassNotFoundException cnfe) {
                continue;
            }
        }
        return player;
    }

    /** Store the specified <CODE>ImagePlayer</CODE> as pattern for the human player.</P>
     *
     * @param player the <CODE>ImagePlayer</CODE> to store
     * @since 0.85
     */
    public static void storeHumanPlayer(ImagePlayer player) {
        res.ensureStoreFolder();
        File[] files = STORE.listFiles();
        for (int i = 0; i < files.length; i++) {
            try {
                if (files[i].getName().substring(files[i].getName().lastIndexOf('.')).equalsIgnoreCase(".human")) files[i].delete();
            } catch (IndexOutOfBoundsException ioobe) {
                continue;
            }
        }
        File f = new File(STORE, player.getName() + ".human");
        try {
            ObjectOutputStream out = new ObjectOutputStream(new FileOutputStream(f));
            out.writeObject(player);
            out.close();
        } catch (IOException ioe) {
        }
    }

    /** Returns all stored <CODE>ImagePlayer</CODE>s that could be used as
     *  computer enemies.</P>
     *
     * @return  a <CODE>ArrayList</CODE> containing all stored computer enemies
     * @see de.javaRisk.v2.gui.initialGUI.DialogAiPlayerManagement
     * @since 0.85
     */
    public static ArrayList getStoredAiPlayers() {
        res.ensureStoreFolder();
        File[] files = STORE.listFiles();
        ArrayList aL = new ArrayList(Math.max(0, files.length - 1));
        ObjectInputStream in;
        for (int i = 0; i < files.length; i++) {
            try {
                if (!files[i].getName().substring(files[i].getName().lastIndexOf('.')).equalsIgnoreCase(".ai")) continue;
                in = new ObjectInputStream(new FileInputStream(files[i]));
                aL.add(in.readObject());
                in.close();
            } catch (IndexOutOfBoundsException ioobe) {
                continue;
            } catch (IOException ioe) {
                continue;
            } catch (ClassNotFoundException cnfe) {
                continue;
            }
        }
        return aL;
    }

    /** Store the specified <CODE>ImagePlayer</CODE> to make it accessible for further games.</P>
     *
     * @param player the <CODE>ImagePlayer</CODE> to store
     * @see de.javaRisk.v2.gui.initialGUI.DialogAiPlayerManagement
     * @since 0.85
     */
    public static void storeAiPlayer(ImagePlayer player) {
        res.ensureStoreFolder();
        File f = new File(STORE, player.getName() + ".ai");
        try {
            ObjectOutputStream out = new ObjectOutputStream(new FileOutputStream(f));
            out.writeObject(player);
            out.close();
        } catch (IOException ioe) {
        }
    }

    /** Store the specified <CODE>ImagePlayer</CODE> to make it accessible for further games.</P>
     *
     * @param player the <CODE>ImagePlayer</CODE> to store
     * @see de.javaRisk.v2.gui.initialGUI.DialogAiPlayerManagement
     * @since 0.85
     */
    public static void deleteAiPlayer(ImagePlayer player) {
        if (player == null) return;
        new File(STORE, player.getName() + ".ai").delete();
    }

    /** Creates a new instance of Resources.
     * The <CODE>javax.swing.Frame</CODE> has to be provided because <CODE>Resources</CODE> needs a <CODE>java.awt.Component</CODE>.
     * The <CODE>javax.swing.JLabel</CODE> will display the current loading status.
     * @param frame The <CODE>JFrame</CODE> that will use the resources
     * @param status a JLabel that will display the loading status
     * @since 0.7
     */
    public Resources(javax.swing.JFrame frame, javax.swing.JLabel status) {
        if (status != null) status.setText(MESSAGES.getString("status.load.logo"));
        MediaTracker mTracker = new MediaTracker(frame);
        logo = Toolkit.getDefaultToolkit().getImage(this.getClass().getResource("graphic/JRisk_logo.jpg"));
        mTracker.addImage(logo, 0);
        backGround = Toolkit.getDefaultToolkit().getImage(this.getClass().getResource("graphic/welcome.jpg"));
        mTracker.addImage(backGround, 1);
        try {
            mTracker.waitForID(0);
            if (status != null) status.setText(MESSAGES.getString("status.load.background"));
            mTracker.waitForID(1);
        } catch (InterruptedException ie) {
            ie.printStackTrace();
        }
        try {
            if (status != null) status.setText(MESSAGES.getString("status.load.properties"));
            systemProperties = new Properties();
            systemProperties.load(this.getClass().getResource("system.properties").openStream());
        } catch (NullPointerException npe) {
            systemProperties = null;
        } catch (FileNotFoundException fnf) {
            systemProperties = null;
        } catch (Exception e) {
            systemProperties = null;
            e.printStackTrace();
        }
        if (status != null) status.setText("");
    }

    /** Get the JRisk-logo.
     * @return returns an Image being the JRisk-logo
     * @since 0.7
     */
    public Image getLogo() {
        return logo;
    }

    /** Get the JRisk-background.
     * @return returns an Image being the JRisk-background
     * @since 0.7
     */
    public Image getBackground() {
        return backGround;
    }

    /** Get the JRisk-system-properties.
     * @return the JRisk-system-properties.
     * @since 0.7
     */
    public Properties getSystemProperties() {
        return systemProperties;
    }
}
