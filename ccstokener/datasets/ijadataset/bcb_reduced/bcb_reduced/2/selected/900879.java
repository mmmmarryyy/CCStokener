package seismosurfer.app;

import java.applet.Applet;
import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Dimension;
import java.io.IOException;
import java.net.MalformedURLException;
import java.net.URL;
import java.util.Properties;
import javax.help.CSH;
import javax.help.HelpBroker;
import javax.help.HelpSet;
import javax.help.HelpSetException;
import javax.swing.JApplet;
import javax.swing.JLabel;
import javax.swing.JMenu;
import javax.swing.JMenuBar;
import javax.swing.JMenuItem;
import javax.swing.MenuElement;
import javax.swing.SwingConstants;
import seismosurfer.gui.HelpTool;
import seismosurfer.http.HttpGateway;
import seismosurfer.http.LayerUtil;
import seismosurfer.http.URLUtil;
import seismosurfer.util.ClientRegistry;
import seismosurfer.util.SeismoException;
import com.bbn.openmap.InformationDelegator;
import com.bbn.openmap.gui.BasicMapPanel;
import com.bbn.openmap.gui.Tool;
import com.bbn.openmap.gui.ToolPanel;
import com.bbn.openmap.util.Debug;
import com.bbn.openmap.util.PropUtils;

/**
 * <p>
 * A <code>JApplet</code> that loads, creates and initializes Seismo-Surfer
 * components, properties, files and constraints.
 * 
 * @see #init()
 * @see SeismoSurfer
 * @see com.bbn.openmap.app.OpenMap
 * @see javax.swing.JApplet
 */
public class SeismoApplet extends JApplet {

    private static final long serialVersionUID = -4846803551577805217L;

    private String[] constrNames;

    /**
     * A reference to SeismoSurfer`s high level application component
     * <code>OpenMap</code>.
     */
    protected SeismoSurfer seismoSurfer;

    /**
     * The Properties applet parameter.
     */
    public static final String PropertiesParameter = "PROPERTIES";

    /**
     * The Map applet parameter.
     */
    public static final String MapParameter = "mapPath";

    /**
     * The properties file path. Contains the relative path to the properties
     * path.
     */
    protected String propertiesPath;

    /**
     * The path to the shape file which is used as the map (political
     * boundaries) for the application.
     */
    protected String mapPath;

    private JLabel SSLab;

    /**
     * Default constructor.
     */
    public SeismoApplet() {
    }

    /**
     * Called when this applet is loaded into the browser. The applet`s
     * initialization is done here. This includes: reading applet parameters,
     * loading shape files, creating, initializing and adding to the BeanContext
     * components, setting constraints e.t.c.
     * 
     * @see seismosurfer.http.HttpGateway#init(Applet)
     * @see seismosurfer.http.HttpGateway#loadQuakeMaxMinData()
     * @see seismosurfer.http.LayerUtil#loadShapeFile(String)
     * @see SeismoSurfer
     * @see #setConstraints()
     * @see com.bbn.openmap.app.OpenMap
     */
    public void init() {
        HttpGateway.init(this);
        HttpGateway.loadQuakeMaxMinData();
        readAppletParameters();
        String localFilePath = LayerUtil.loadShapeFile(mapPath);
        loadProperties();
        createHelpSet();
        seismoSurfer = SeismoSurfer.createSeismo(localFilePath);
        ClientRegistry.setMapBean(seismoSurfer.getMapPanel().getMapBean());
        setFieldLevelHelpButton();
        setConstraints();
        this.getContentPane().setBackground(Color.WHITE);
        SSLab = new JLabel("Seismo-Surfer applet loaded.", SwingConstants.CENTER);
        this.getContentPane().add(SSLab, BorderLayout.CENTER);
        this.setSize(new Dimension(200, 100));
        seismoSurfer.getMapHandler().add(this);
        setHelpIDString();
    }

    /**
     * Sets the help id string to some of the applet components.
     * This is needed for the context-sensitive help to work.
     */
    protected void setHelpIDString() {
        BasicMapPanel map = (BasicMapPanel) seismoSurfer.getMapPanel();
        CSH.setHelpIDString(map, "map");
        InformationDelegator info = (InformationDelegator) map.getMapComponentByType(InformationDelegator.class);
        CSH.setHelpIDString(info, "infoBar");
    }

    /**
     * Adds the help tool, used in context-sensitive help to
     * the tools panel.
     *
     */
    protected void setFieldLevelHelpButton() {
        ToolPanel tp = ClientRegistry.getToolPanel();
        Tool helpTool = new HelpTool();
        tp.add(helpTool);
    }

    /**
     * Reads the properties and map file path.
     */
    public void readAppletParameters() {
        propertiesPath = getParameter(PropertiesParameter);
        mapPath = getParameter(MapParameter);
    }

    /**
     * Gets constraints names. These are the menu or menu items names that are
     * disabled for a user role.
     * 
     * @return An array of Strings which contains the constraints names.
     */
    protected String[] getConstraintsNames() {
        return constrNames;
    }

    /**
     * Reads the constraints names from the server and disables the respective
     * menus and menu items.
     * 
     * @see seismosurfer.http.HttpGateway#getConstraintsNames()
     * @see #setMenuConstraints(JMenu)
     */
    protected void setConstraints() {
        constrNames = HttpGateway.getConstraintsNames();
        JMenuBar menuBar = seismoSurfer.getMapPanel().getMapMenuBar();
        MenuElement[] menus = menuBar.getSubElements();
        for (int i = 0; i < menus.length; i++) {
            boolean menuDisabled = false;
            if (menus[i] instanceof JMenu) {
                JMenu jMenu = (JMenu) menus[i];
                for (int k = 0; k < constrNames.length; k++) {
                    if (constrNames[k].toString().equals(jMenu.getText())) {
                        System.out.println("Menu disabled: " + jMenu.getText());
                        menuBar.getMenu(i).setEnabled(false);
                        menuDisabled = true;
                    }
                }
                if (!menuDisabled) {
                    setMenuConstraints(jMenu);
                }
            }
        }
    }

    /**
     * Sets constraints to a menu. Disables the given menu or the submenus and
     * menu items it contains according to the constraints names.
     * 
     * @param menu
     *            the JMenu in which the constraints will be placed
     */
    protected void setMenuConstraints(JMenu menu) {
        int itemCount = menu.getMenuComponentCount();
        for (int i = 0; i < itemCount; i++) {
            if (menu.getMenuComponent(i) instanceof JMenu) {
                JMenu subMenu = (JMenu) menu.getMenuComponent(i);
                boolean subMenuDisabled = false;
                for (int k = 0; k < getConstraintsNames().length; k++) {
                    if (getConstraintsNames()[k].toString().equals(subMenu.getText())) {
                        System.out.println("Menu Item disabled: " + subMenu.getText());
                        menu.getItem(i).setEnabled(false);
                        subMenuDisabled = true;
                    }
                }
                if (!subMenuDisabled) {
                    setMenuConstraints((JMenu) menu.getMenuComponent(i));
                }
            } else if (menu.getMenuComponent(i) instanceof JMenuItem) {
                JMenuItem jMenuItem = (JMenuItem) menu.getMenuComponent(i);
                for (int k = 0; k < getConstraintsNames().length; k++) {
                    if (getConstraintsNames()[k].toString().equals(jMenuItem.getText())) {
                        System.out.println("Menu Item disabled: " + jMenuItem.getText());
                        menu.getItem(i).setEnabled(false);
                    }
                }
            }
        }
    }

    /**
     * Gets the properties file URL given its relative (to this context) path.
     * 
     * @param relativePath
     *            path relative to this context (web application)
     * @return the absolute URL pointing to the properties file
     */
    public URL getPropertiesURL(String relativePath) {
        URL url = null;
        try {
            url = new URL(this.getCodeBase(), relativePath);
            return url;
        } catch (MalformedURLException e) {
            throw new SeismoException(e);
        }
    }

    /**
     * Loads the application properties to a 
     * {@link  seismosurfer.util.ClientRegistry  registry} object
     * to make them easily available from all classes.
     * 
     * @see seismosurfer.util.ClientRegistry
     */
    protected void loadProperties() {
        try {
            URL url = getPropertiesURL(propertiesPath);
            Properties p = new Properties();
            PropUtils.loadProperties(p, url.openStream());
            ClientRegistry.setOpenmapProperties(p);
        } catch (IOException ex) {
            throw new SeismoException(ex);
        }
    }

    /**
     * Creates the main JavaHelp objects needed for the 
     * online help system. See also, the JavaHelp documentation.
     *
     */
    protected void createHelpSet() {
        try {
            ClassLoader loader = this.getClass().getClassLoader();
            Properties p = ClientRegistry.getOpenmapProperties();
            String helpSetPath = p.getProperty("seismosurfer.help.helpsetpath");
            URL url = new URL(URLUtil.getAppletURL(), helpSetPath);
            Debug.output("helpset url=" + url);
            HelpSet hs = new HelpSet(loader, url);
            HelpBroker hb = hs.createHelpBroker();
            ClientRegistry.setHelpBroker(hb);
        } catch (HelpSetException ex) {
            throw new SeismoException(ex);
        } catch (MalformedURLException ex) {
            throw new SeismoException(ex);
        }
    }

    /**
     * Start the applet.
     */
    public void start() {
        super.start();
    }

    /**
     * Stop the applet.
     */
    public void stop() {
        super.stop();
    }

    /**
     * Destroy the applet.
     */
    public void destroy() {
        super.destroy();
    }
}
