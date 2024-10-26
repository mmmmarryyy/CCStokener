package ro.wpcs.traser.controllers;

import java.awt.Image;
import java.awt.Toolkit;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.net.URL;
import java.util.Properties;
import javax.swing.Icon;
import javax.swing.ImageIcon;
import javax.swing.JOptionPane;
import org.apache.log4j.Logger;
import org.gui4j.Gui4j;
import org.gui4j.Gui4jController;
import org.gui4j.Gui4jView;
import org.gui4j.exception.Gui4jExceptionHandler;

/**
 * This is a controller for the main application window
 * 
 * @author Alina
 * @date Oct 2, 2008
 */
public final class GUIMainController implements Gui4jController, Gui4jExceptionHandler {

    /** Class logger */
    private static final Logger logger = Logger.getLogger(GUIMainController.class);

    /** the gui4j that's associated with this controller */
    private static Gui4j gui4j;

    /** The main Gui4jView object */
    private static Gui4jView gui4jView;

    /** The xml descriptive file for this view */
    private static final String RESOUCE_NAME = "xml/main.xml";

    /** Singleton instance */
    private static GUIMainController singleton;

    /** The properties for the main window */
    private static final Properties prop = new Properties();

    /** URL for properties file for the main window */
    private static final URL url = ClassLoader.getSystemResource("mainWindow.properties");

    /** Private constructor to prevent any other class from instantiating */
    private GUIMainController() {
        super();
    }

    /**
	 * Confirm dialog
	 * 
	 * @param title
	 *            the dialog's box title
	 * @param message
	 *            the message
	 * @return true/false if the user confirms
	 */
    public static boolean areYouSure(String title, String message) {
        boolean valid = false;
        final int response = JOptionPane.showConfirmDialog(null, message, title, JOptionPane.YES_NO_OPTION);
        if (response == 0) {
            valid = true;
        }
        return valid;
    }

    /**
	 * @return a single instance of GUIMainController
	 */
    public static synchronized GUIMainController getInstance() {
        if (singleton == null) {
            singleton = new GUIMainController();
        }
        return singleton;
    }

    /**
	 * @return The main Gui4jView object
	 */
    public static Gui4jView getView() {
        return gui4jView;
    }

    /**
	 * Sets the visual appearence of the main window
	 */
    public void display() {
        loadWindowProperties();
        final int width = Integer.valueOf((String) prop.get("mainWindow.width"));
        final int height = Integer.valueOf((String) prop.get("mainWindow.height"));
        final boolean center = Boolean.valueOf((String) prop.get("mainWindow.center"));
        final boolean resizable = (Boolean.valueOf((String) prop.get("mainWindow.resizable")));
        gui4jView = createGui4jView();
        gui4jView.maximize(width, height);
        gui4jView.prepare();
        gui4jView.center(center);
        gui4jView.setResizable(resizable);
        gui4jView.show();
    }

    /**
	 * Loads the properties from an external properties file
	 * 
	 * @return the properties object for the main window
	 */
    public static void loadWindowProperties() {
        try {
            prop.load(url.openStream());
        } catch (FileNotFoundException e) {
            logger.error(e);
        } catch (IOException e) {
            logger.error(e);
        }
    }

    /**
	 * Instantiates the gui4jView after specific RESOURCE_NAME
	 */
    private Gui4jView createGui4jView() {
        final Gui4jView view = getGui4j().createView(RESOUCE_NAME, this, getTitle(), false);
        return view;
    }

    /**
	 * @return the main window's title
	 */
    private String getTitle() {
        String title = prop.getProperty("mainWindow.title") + " --- Welcome, " + LoginController.getLoggedUser().getName();
        return title;
    }

    public Icon getImage() {
        URL imageUrl = getClass().getClassLoader().getResource("ro/wpcs/traser/resources/traser_logo.gif");
        logger.info("Main Image: " + imageUrl.toString());
        Image image = Toolkit.getDefaultToolkit().createImage(imageUrl);
        ImageIcon icon = new ImageIcon(image);
        return icon;
    }

    /**
	 * Sets the controller for the gui4j sent as parameter
	 * 
	 * @param gui4j
	 *            represents the gui4j that's associated with this controller
	 */
    public void setGui4j(final Gui4j gui4j) {
        this.gui4j = gui4j;
    }

    /**
	 * @return instance of MenuController associated with this view
	 */
    public MenuController getMenuController() {
        MenuController.getInstance().setGui4j(gui4j);
        return MenuController.getInstance();
    }

    /**
	 * @return an instance of TreeController
	 */
    public TreeController getTreeController() {
        return TreeController.getInstance();
    }

    /**
	 * @return an instance of ProjectController
	 */
    public ProjectController getProjectController() {
        return ProjectController.getInstance();
    }

    /** @return an instance of FileController */
    public FileController getFileController() {
        return FileController.getInstance();
    }

    /** @return an instance of VersionController */
    public VersionController getVersController() {
        return VersionController.getInstance();
    }

    /**
	 * @return always true for making an edit field editable
	 */
    public boolean setEditable() {
        return true;
    }

    @Override
    public boolean onWindowClosing() {
        logger.info("Closing main window");
        gui4jView.close();
        System.exit(0);
        return false;
    }

    @Override
    public void windowClosed() {
    }

    @Override
    public Gui4jExceptionHandler getExceptionHandler() {
        return null;
    }

    @Override
    public Gui4j getGui4j() {
        return gui4j;
    }

    @Override
    public Gui4jExceptionHandler getDelegationExceptionHandler() {
        return null;
    }
}
