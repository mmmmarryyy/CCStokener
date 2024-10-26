package org.merlotxml.merlot;

import java.awt.Color;
import java.awt.Dimension;
import java.awt.Rectangle;
import java.awt.Toolkit;
import java.io.BufferedInputStream;
import java.io.BufferedOutputStream;
import java.io.BufferedReader;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.io.PrintStream;
import java.net.MalformedURLException;
import java.net.URL;
import java.net.URLConnection;
import java.util.ArrayList;
import java.util.Enumeration;
import java.util.HashMap;
import java.util.Hashtable;
import java.util.List;
import java.util.Locale;
import java.util.MissingResourceException;
import java.util.Properties;
import java.util.StringTokenizer;
import javax.swing.ImageIcon;
import org.merlotxml.util.FileUtil;
import org.tolven.logging.TolvenLogger;
import org.w3c.dom.Node;

/**
 * 
 * Settings access for the app
 * 
 * @author Kelly A. Campbell
 * 
 */
public class XMLEditorSettings implements MerlotConstants {

    public static final int LARGE_ICON = 32;

    public static final int SMALL_ICON = 16;

    protected static final String APP_ICON_SMALL = "app.icon.s";

    protected static final String APP_ICON_LARGE = "app.icon.l";

    protected static final String BKG_COLOR = "background.color";

    protected static final String BKG_PICTURE = "background.picture";

    protected static final String FRAME_TITLE = "frame.title";

    protected static final String INSET = "frame.inset";

    protected static final String TOP_DISPLACE = "top.displace";

    protected static final String BOT_DISPLACE = "bot.displace";

    protected static final String DOM_LIAISON = "xml.dom.liaison";

    protected static final String FILTER_NODES = "merlot.filternodes";

    /**
     * Various debugging stuff like opening a certain file quickly instead of
     * making the user walk through the file chooser
     */
    protected static final String DEBUG_MODE = "merlot.debug";

    protected static final String SUPPRESS_ADD = "merlot.filteradds";

    protected static final String WRITE_ATTS = "merlot.write.default-atts";

    protected static final String AUTO_ADD_NODES = "merlot.auto.add.nodes";

    protected static final String EDITOR_LIST = "merlot.editor.classes";

    protected static final String ICON_DIR_PROP = "merlot.icon.dir";

    protected static final String ICON_PROP_FILE = "merlot.icon.props";

    protected static final String MERLOT_ICON_PREFIX = "micon.";

    protected static final String DEFAULT_LF = "merlot.default.lookandfeel";

    protected static final String DEFAULT_LIB = "merlot.default.library";

    protected static final String DEFAULT_EDITOR = "merlot.default.editorclass";

    protected static final String DEFAULT_SCHEMA_EDITOR = "xerlin.default.schema.editorclass";

    protected static final String SPLASH_SCREEN = "trimeditor.splashscreen";

    protected static final String RESOURCE_PACKAGE = "merlot.resource.package";

    protected static final String COUNTRY = "country";

    protected static final String LANGUAGE = "language";

    protected static final String VARIANT = "variant";

    protected static final String MERLOT_GRAPHICS_DIR = "merlot.graphics.dir";

    protected static final String ICON_LOADER = "merlot.iconloader.class";

    protected static final String APPICON_LOADER = "merlot.appiconloader.class";

    protected static final String MERLOT_RESOURCE_PATH = "merlot.resource.path";

    protected static final String UNDO_LIMIT = "undo.limit";

    protected static final String MERLOT_PLUGIN_PATH = "merlot.plugin.path";

    protected static final String MERLOT_PLUGIN_FILE = "merlot.plugin.file";

    protected static final String MERLOT_SNIPPETS_INFO = "merlot.snippets.list";

    private static final String SYS_PROP_PLUGIN_URLS = "org.merlotxml.merlot.pluginURLs";

    private static final String JFILECHOOSER = "merlot.jfilechooser";

    public static final String TRIM_FOLDER = "trim.folder";

    public static String FILESEP = null;

    public static String USER_DIR = null;

    public static String TOLVEN_DIR = "Tolven";

    public static String WORKING_DIR = "TolvenXMLEditor";

    public static File USER_MERLOT_DIR = null;

    protected String _propsFile = "tolvenXMLEditor.properties";

    protected Properties _props = new Properties();

    protected Properties _defaultProps = new Properties();

    protected Properties _userProps = new Properties();

    protected String[] _editorList = new String[0];

    protected String[] _openFiles = new String[0];

    protected Hashtable<String, ImageIcon> _icons;

    private boolean _showSplash;

    protected static XMLEditorSettings _settings = null;

    protected MerlotSplashScreen _splash = null;

    public HashMap<String, Node> _xmlSniplets = new HashMap<String, Node>();

    private List<String> _recentFileList;

    private static final int NUMBER_RECENT = 6;

    protected static final String RECENT_FILE_PREFIX = "recent.file";

    protected static final String EDITOR_X = "editor.x";

    protected static final String EDITOR_Y = "editor.y";

    protected static final String EDITOR_WIDTH = "editor.width";

    protected static final String EDITOR_HEIGHT = "editor.height";

    protected static final String DESKTOP_WIDTH = "desktop.width";

    protected static final String DESKTOP_HEIGHT = "desktop.height";

    protected static final String SPLIT_PANE_V = "vertical.splitpane.position";

    protected static final String SPLIT_PANE_H = "horizontal.splitpane.position";

    protected static final String EDIT_PANEL_HEIGHT = "edit.panel.height";

    protected static final String EDIT_PANEL_WIDTH = "edit.panel.width";

    protected static final String TREE_PANEL_HEIGHT = "tree.panel.height";

    protected static final String TREE_PANEL_WIDTH = "tree.panel.width";

    protected static final String ATT_PANEL_HEIGHT = "att.panel.height";

    protected static final String ATT_PANEL_WIDTH = "att.panel.width";

    protected static final String COLUMN_WIDTH = ".column.width";

    public XMLEditorSettings(String[] args, boolean showSplash) {
        _settings = this;
        _showSplash = showSplash;
        Properties defaults = getDefaults();
        _recentFileList = new ArrayList<String>(NUMBER_RECENT);
        for (int i = 0; i < NUMBER_RECENT; i++) {
            _recentFileList.add("");
        }
        FILESEP = System.getProperty("file.separator");
        USER_DIR = System.getProperty("user.home");
        USER_MERLOT_DIR = new File(USER_DIR + FILESEP + TOLVEN_DIR + FILESEP + WORKING_DIR);
        _openFiles = parseArgs(args);
        _defaultProps = loadDefPropsFile(defaults);
        _userProps = loadPropsFile(_defaultProps);
        _props = new Properties(_userProps);
        reparseDefines(args);
        MerlotDebug.init(_props);
        if (_showSplash) startSplashScreen();
        initIcons();
    }

    public XMLEditorSettings(String[] args) {
        this(args, true);
    }

    public Properties getDefaults() {
        Properties defaults = new Properties();
        defaults.put(APP_ICON_SMALL, "xerlin16icon.gif");
        defaults.put(APP_ICON_LARGE, "xerlin32icon.gif");
        defaults.put(BKG_PICTURE, "tolven-logo.png");
        defaults.put(BKG_COLOR, "0xffffff");
        defaults.put(FRAME_TITLE, "Tolven Trim Editor");
        defaults.put(INSET, "75");
        defaults.put(TOP_DISPLACE, "0");
        defaults.put(BOT_DISPLACE, "75");
        defaults.put(DOM_LIAISON, "org.merlotxml.util.xml.xerces.DOMLiaison");
        defaults.put(FILTER_NODES, "true");
        defaults.put(DEBUG_MODE, "false");
        defaults.put(SUPPRESS_ADD, "true");
        defaults.put(WRITE_ATTS, "false");
        defaults.put(AUTO_ADD_NODES, "false");
        defaults.put(JFILECHOOSER, "false");
        defaults.put(DEFAULT_LF, "default");
        defaults.put(SPLASH_SCREEN, "tolvensplash.gif");
        defaults.put(RESOURCE_PACKAGE, "org.merlotxml.merlot.resource");
        defaults.put(MERLOT_GRAPHICS_DIR, "images");
        defaults.put(ICON_LOADER, "org.merlotxml.merlot.icons.ImageLoader");
        defaults.put(APPICON_LOADER, "org.merlotxml.merlot.appicons.ImageLoader");
        defaults.put(DEFAULT_EDITOR, "org.merlotxml.merlot.GenericDOMEditor");
        defaults.put(DEFAULT_SCHEMA_EDITOR, "org.merlotxml.merlot.editors.SchemaDOMEditor");
        defaults.put(MERLOT_RESOURCE_PATH, "org.merlotxml.merlot.resource");
        defaults.put(MERLOT_PLUGIN_PATH, "plugins");
        defaults.put(UNDO_LIMIT, "10");
        return defaults;
    }

    public void loadTemplates() {
        _xmlSniplets.clear();
        java.util.List<File> files = getSnippets();
        for (File file : files) {
            try {
                XMLFile tmp = new XMLFile(file, false);
                String fname = file.getName().substring(0, file.getName().lastIndexOf('.'));
                Node clonedNode = tmp.getDocument().getFirstChild();
                _xmlSniplets.put(fname, clonedNode);
            } catch (MerlotException e) {
                e.printStackTrace();
            }
        }
    }

    /**
     * This allows a property to be set/modified Useful within plugin
     * architecture so that some kind of memory can be achieved - i.e. last
     * directory accessed, username/password last used etc.
     */
    public void setProperty(String propertyName, String propertyValue) {
        _props.put(propertyName, propertyValue);
    }

    protected void reparseDefines(String[] args) {
        String a;
        for (int i = 0; i < args.length; i++) {
            a = args[i];
            MerlotDebug.msg("args[" + i + "] = " + a);
            if (a.equals("-d")) {
                if (i + 1 < args.length) {
                    String s = args[++i];
                    String key, val;
                    int x = s.indexOf('=');
                    if (x >= 0) {
                        key = s.substring(0, x).trim();
                        val = s.substring(x + 1).trim();
                        MerlotDebug.msg("setting '" + key + "' to '" + val + "'");
                        _props.put(key, val);
                        if (key.startsWith("merlot.debug")) {
                            MerlotDebug.reloadSettings();
                        }
                    }
                } else {
                    printUsage("-d requires a parameter of the form key=value");
                }
            }
        }
    }

    /**
     * @return An array of file names to open, or an empty array if none were
     *         supplied.
     */
    protected String[] parseArgs(String[] args) {
        java.util.List<String> openList = new ArrayList<String>();
        String a;
        for (int i = 0; i < args.length; i++) {
            a = args[i];
            MerlotDebug.msg("args[" + i + "] = " + a);
            if (a.equals("-f")) {
                if (i + 1 < args.length) {
                    _propsFile = args[++i];
                    MerlotDebug.msg("Props file = " + _propsFile);
                } else {
                    printUsage("-f requires a parameter");
                }
            } else if (a.equals("-d")) {
            } else if (a.equals("-o")) {
                if (i + 1 < args.length) {
                    String s = args[++i];
                    try {
                        FileOutputStream outStream = new FileOutputStream(s, true);
                        PrintStream out = new PrintStream(outStream);
                        TolvenLogger.info("Redirecting output and errors to '" + s + "'", XMLEditorSettings.class);
                        System.setOut(out);
                        System.setErr(out);
                    } catch (IOException ex) {
                        ex.printStackTrace();
                    }
                }
            } else if (a.equals("-h") || a.equals("--help")) {
                printUsage("");
            } else if (a.startsWith("-")) {
                printUsage("Unknown argument: " + a);
            } else {
                openList.add(args[i]);
            }
        }
        return (String[]) openList.toArray(new String[openList.size()]);
    }

    public String[] getOpenFiles() {
        return _openFiles;
    }

    protected void printUsage(String msg) {
        TolvenLogger.error(msg, XMLEditorSettings.class);
        TolvenLogger.error("Usage: merlot [options] <files>", XMLEditorSettings.class);
        TolvenLogger.error(" Options: ", XMLEditorSettings.class);
        TolvenLogger.error("          [-f  properties file] What tolvenXMLEditor.properties to startup with.", XMLEditorSettings.class);
        TolvenLogger.error("          [-d key=value]        Define a property. Example: -d merlot.debug=true", XMLEditorSettings.class);
        TolvenLogger.error("          [-h or --help]        This message.", XMLEditorSettings.class);
        System.exit(-1);
    }

    public Properties loadPropsFile(String propsFile) throws IOException {
        Properties myprops = new Properties();
        InputStream is = getClass().getResourceAsStream(propsFile);
        if (is != null) {
            MerlotDebug.msg("Loading properties from " + propsFile);
            myprops.load(is);
        } else {
            MerlotDebug.msg("Failed to load properties from " + propsFile);
        }
        return myprops;
    }

    public Properties loadPropsFile(Properties defaults, String propsFile) {
        Properties myprops = new Properties(defaults);
        URL u = null;
        try {
            u = new URL(propsFile);
        } catch (Exception ex) {
            u = this.getClass().getResource(propsFile);
        }
        if (u != null) {
            MerlotDebug.msg("Loading properties from " + u.toString());
            try {
                InputStream is = u.openStream();
                if (is != null) {
                    myprops.load(is);
                }
            } catch (Exception ex) {
                MerlotDebug.exception(ex);
            }
        } else {
            MerlotDebug.msg("Properties file " + propsFile + " not found. Using internal defaults.");
        }
        return myprops;
    }

    protected Properties loadDefPropsFile(Properties defaults) {
        return loadPropsFile(defaults, _propsFile);
    }

    protected Properties loadPropsFile(Properties defaults) {
        Properties myprops = new Properties(defaults);
        File f = null;
        InputStream is = null;
        try {
            f = new File(USER_MERLOT_DIR, _propsFile);
            is = new FileInputStream(f);
            myprops.load(is);
            is.close();
        } catch (Exception ex) {
            MerlotDebug.msg("Properties file not found. Using internal defaults.");
        }
        Enumeration<Object> e = myprops.keys();
        while (e.hasMoreElements()) {
            String prop = (String) e.nextElement();
            int index = prop.indexOf(RECENT_FILE_PREFIX);
            if (index > -1) {
                String suffix = prop.substring(RECENT_FILE_PREFIX.length());
                try {
                    int number = Integer.parseInt(suffix);
                    if (number >= 0 && number < NUMBER_RECENT) _recentFileList.set(number, (String) myprops.get(prop));
                } catch (NumberFormatException nfe) {
                    MerlotDebug.msg("Malformed recent file property key");
                }
            }
        }
        return myprops;
    }

    public String getProperty(String s) {
        return loadKeys(_props.getProperty(s));
    }

    public Properties getProperties() {
        return _props;
    }

    public Properties getDefaultProperties() {
        return _defaultProps;
    }

    public static XMLEditorSettings getSharedInstance() {
        return _settings;
    }

    /**
     * Gets the background picture specified in the properties if it exists
     * 
     * @return ImageIcon of the background pic or null if not found
     */
    public ImageIcon getBackgroundImage() {
        return (loadImageFromProp(BKG_PICTURE));
    }

    public ImageIcon getAppIconSmall() {
        return (loadImageFromProp(APP_ICON_SMALL));
    }

    public ImageIcon getAppIconLarge() {
        return (loadImageFromProp(APP_ICON_LARGE));
    }

    protected ImageIcon loadImageFromProp(String propname) {
        String filename = getProperty(propname);
        if (filename != null) {
            ImageIcon image = null;
            URL iurl = getClass().getResource("/images/" + filename);
            if (iurl == null) {
                iurl = getClass().getResource("/icons/" + filename);
            }
            if (iurl != null) {
                image = new ImageIcon(iurl);
            }
            return image;
        }
        return null;
    }

    /**
     * Gets the property named 'background.color'
     * 
     * @return the color property or a default color of Black
     *  
     */
    public Color getBackgroundColor() {
        Color bkgColor = null;
        String val = getProperty(BKG_COLOR);
        if (val != null) {
            bkgColor = Color.decode(val);
        }
        if (bkgColor == null) {
            bkgColor = Color.white;
        }
        return bkgColor;
    }

    public int getFrameInset() {
        Integer i = Integer.valueOf(getProperty(INSET));
        return i.intValue();
    }

    public String getFrameTitle() {
        return getProperty(FRAME_TITLE);
    }

    public int getTopDisplacement() {
        Integer i = Integer.valueOf(getProperty(TOP_DISPLACE));
        return i.intValue();
    }

    public int getBottomDisplacement() {
        Integer i = Integer.valueOf(getProperty(BOT_DISPLACE));
        return i.intValue();
    }

    public String getDOMLiaisonClassname() {
        return getProperty(DOM_LIAISON);
    }

    private boolean stringToBoolean(String s) {
        if (s != null) {
            if (s.equalsIgnoreCase("true") || s.equalsIgnoreCase("yes")) {
                return true;
            }
        }
        return false;
    }

    public boolean isFilteringNodes() {
        return stringToBoolean(getProperty(FILTER_NODES));
    }

    public boolean isDebugOn() {
        return debugModeOn();
    }

    public boolean debugModeOn() {
        return stringToBoolean(getProperty(DEBUG_MODE));
    }

    public boolean getSuppressAddMenuItems() {
        return stringToBoolean(getProperty(SUPPRESS_ADD));
    }

    public String getEditors() {
        return getProperty(EDITOR_LIST);
    }

    public String getDefaultEditor() {
        return getProperty(DEFAULT_EDITOR);
    }

    public String getDefaultSchemaEditor() {
        return getProperty(DEFAULT_SCHEMA_EDITOR);
    }

    /**
     * Initializes our icon cache in it's own thread
     */
    protected void initIcons() {
        String msg = MerlotResource.getString(UI, "splash.loadingIcons.msg");
        if (_showSplash) showSplashStatus(msg);
        Properties iconProps = null;
        _icons = new Hashtable<String, ImageIcon>();
        String iconfile = getProperty(ICON_PROP_FILE);
        if (iconfile != null) {
            if (iconfile.equals("this")) {
                iconProps = _props;
            } else {
                try {
                    iconProps = new Properties();
                    URL u = this.getClass().getResource(iconfile);
                    if (u == null) {
                        u = new URL(iconfile);
                    }
                    if (u != null) {
                        InputStream is = u.openStream();
                        iconProps.load(is);
                    }
                } catch (Exception ex) {
                    MerlotDebug.exception(ex);
                }
            }
            String prepend = iconProps.getProperty(ICON_DIR_PROP);
            Enumeration<Object> e = iconProps.keys();
            while (e.hasMoreElements()) {
                String s = (String) e.nextElement();
                if (s.startsWith(MERLOT_ICON_PREFIX)) {
                    String filename = iconProps.getProperty(s);
                    if (prepend != null) {
                        filename = prepend + FILESEP + filename;
                    }
                    addIcon(s, filename);
                }
            }
        }
    }

    /**
     * loads an imageicon and adds it to the icons hashtable
     */
    protected void addIcon(String key, String filename) {
        ImageIcon icon = null;
        try {
            String iconkey = key.substring(MERLOT_ICON_PREFIX.length());
            int dot = iconkey.indexOf('.');
            if (dot > 0) {
                icon = loadImageFromProp(filename);
                if (icon != null) {
                    _icons.put(iconkey, icon);
                }
            }
        } catch (Exception ex) {
            MerlotDebug.exception(ex);
        }
    }

    /**
     * This returns an image icon for the given name and size (SMALL, LARGE). If
     * the icon isn't found, null is returned
     */
    public ImageIcon getIcon(String name, int size) {
        String iconsize;
        switch(size) {
            case SMALL_ICON:
                iconsize = "s";
                break;
            case LARGE_ICON:
                iconsize = "l";
                break;
            default:
                iconsize = "s";
                break;
        }
        String iconkey = name + "." + iconsize;
        ImageIcon o = _icons.get(iconkey);
        return (ImageIcon) o;
    }

    public String getLookAndFeel() {
        return getProperty(DEFAULT_LF);
    }

    public String getDefaultLibrary() {
        return getProperty(DEFAULT_LIB);
    }

    public ImageIcon getSplashScreenImage() {
        ImageIcon icon = loadImageFromProp(SPLASH_SCREEN);
        return icon;
    }

    public void startSplashScreen() {
        ImageIcon icon = getSplashScreenImage();
        if (icon != null) {
            MerlotDebug.msg("Showing splash screen");
            _splash = new MerlotSplashScreen(icon);
        }
    }

    public void showSplashStatus(String s) {
        if (_splash != null) {
            _splash.showStatus(s);
        }
    }

    public void closeSplash() {
        MerlotDebug.msg("Closing splash screen");
        if (_splash != null) {
            _splash.close();
            _splash = null;
        }
    }

    public String getResourcePackage() {
        return getProperty(RESOURCE_PACKAGE);
    }

    public Locale getLocale() {
        String country = getProperty(COUNTRY);
        String language = getProperty(LANGUAGE);
        String other = getProperty(VARIANT);
        if (country != null && language != null) {
            Locale l;
            if (other != null) {
                l = new Locale(language, country, other);
            } else {
                l = new Locale(language, country);
            }
            return l;
        }
        return Locale.getDefault();
    }

    public int getUndoLimit() {
        String s = getProperty(UNDO_LIMIT);
        try {
            int ul = Integer.parseInt(s);
            return ul;
        } catch (Exception ex) {
        }
        return 10;
    }

    protected String loadKeys(String str) throws MissingResourceException, UnsupportedOperationException {
        boolean foundkey = false;
        if (str != null) {
            StringBuffer sb = new StringBuffer(str);
            int len = sb.length();
            StringBuffer newsb = new StringBuffer(sb.capacity());
            int i, j = 0, k = 0;
            for (i = 0; i < len; i++) {
                char c = sb.charAt(i);
                if ((c == '{') && (i + 2 < len) && (sb.charAt(i + 1) == '%')) {
                    int endkey = -1;
                    StringBuffer key = new StringBuffer();
                    for (j = i + 2; j + 1 < len && endkey < 0; j++) {
                        if (sb.charAt(j) == '%' && sb.charAt(j + 1) == '}') {
                            endkey = j - 1;
                        } else {
                            key.append(sb.charAt(j));
                        }
                    }
                    if (endkey > 0) {
                        try {
                            String s = getProperty(key.toString());
                            if (s == null) {
                            }
                            newsb.append(s);
                            i = endkey + 2;
                            foundkey = true;
                        } catch (MissingResourceException ex) {
                        }
                    }
                }
                if (!foundkey) {
                    newsb.append(c);
                    k++;
                }
                foundkey = false;
            }
            return newsb.toString();
        }
        return null;
    }

    public boolean useJFileChooser() {
        if (getProperty(JFILECHOOSER).equals("true")) {
            return true;
        }
        int ostype = getOSType();
        switch(ostype) {
            default:
            case SOLARIS:
            case LINUX:
                return true;
            case WINDOWS:
            case MACOS:
                return false;
        }
    }

    public void addRecentFile(File f) {
        if (f == null) return;
        String recentFile;
        recentFile = f.getAbsolutePath();
        addRecentFile(recentFile);
    }

    public void addRecentFile(String fullPath) {
        _recentFileList.remove(fullPath);
        _recentFileList.add(0, fullPath);
        if (_recentFileList.size() > NUMBER_RECENT) {
            _recentFileList.remove(NUMBER_RECENT);
        }
    }

    public List<String> getRecentFiles() {
        return _recentFileList;
    }

    public static final int WINDOWS = 1;

    public static final int SOLARIS = 2;

    public static final int LINUX = 3;

    public static final int MACOS = 4;

    public static int getOSType() {
        String s = System.getProperty("os.name").toLowerCase();
        if (s.indexOf("windows") >= 0) {
            return WINDOWS;
        }
        if (s.indexOf("sunos") >= 0) {
            return SOLARIS;
        }
        if (s.indexOf("linux") >= 0) {
            return LINUX;
        }
        if ((s.indexOf("mac") >= 0) || (s.indexOf("mac os") >= 0)) {
            return MACOS;
        }
        MerlotDebug.msg("[debug] OSType = " + s + " Returning WINDOWS");
        return WINDOWS;
    }

    public void saveProperties(Properties props) {
        File f = new File(USER_DIR + FILESEP + TOLVEN_DIR + FILESEP + WORKING_DIR);
        if (!f.exists() && !f.isDirectory()) f.mkdir();
        f = new File(USER_DIR + FILESEP + TOLVEN_DIR + FILESEP + WORKING_DIR + FILESEP + _propsFile);
        try {
            if (!f.exists()) f.createNewFile();
        } catch (Exception ex) {
            MerlotDebug.msg("Error, unable to save property file");
            return;
        }
        OutputStream out = null;
        try {
            out = new FileOutputStream(f);
            props.store(out, "tolvenXMLEditor.properties");
        } catch (Exception ex) {
            MerlotDebug.msg("Error, unable to save property file");
        } finally {
            if (out != null) {
                try {
                    out.close();
                } catch (java.io.IOException e) {
                }
                out = null;
            }
        }
        if (!props.equals(_userProps)) {
            _userProps.putAll(props);
        }
    }

    /**
     * returns the plugin path property all parsed up into separate directories
     * 
     * @return List containing valid File objects which are directories
     *         containing plugins
     */
    public java.util.List<URL> getPluginFiles() {
        java.util.List<URL> list = new ArrayList<URL>();
        String p = getProperty(MERLOT_PLUGIN_FILE);
        if (p != null) {
            InputStream is = getClass().getResourceAsStream("/" + p);
            BufferedReader br = new BufferedReader(new InputStreamReader(is));
            String line;
            try {
                while ((line = br.readLine()) != null) {
                    URL url = getClass().getResource("/" + line);
                    if (url != null) {
                        list.add(url);
                    }
                }
                br.close();
                is.close();
            } catch (IOException e) {
                MerlotDebug.exception(e);
            }
        }
        return list;
    }

    /**
     * returns the plugin path property all parsed up into separate directories
     * 
     * @return List containing valid File objects which are directories
     *         containing plugins
     */
    public java.util.List<File> getPluginPath() {
        String p = getProperty(MERLOT_PLUGIN_PATH);
        if (p != null) {
            StringTokenizer tok = new StringTokenizer(p, ":;");
            List<File> list = new ArrayList<File>();
            while (tok.hasMoreTokens()) {
                String s = tok.nextToken();
                File f = new File(s);
                if (f.exists() && f.canRead() && f.isDirectory()) {
                    list.add(f);
                }
            }
            return list;
        }
        return null;
    }

    /**
     * @return A list of URLs that identify plugins that should be made
     *         available
     */
    public java.util.List<URL> getPluginURLs() throws MalformedURLException {
        java.util.List<URL> rtn = new ArrayList<URL>();
        String s = getProperty(SYS_PROP_PLUGIN_URLS);
        if (s != null) {
            StringTokenizer tok = new StringTokenizer(s, " ");
            while (tok.hasMoreTokens()) {
                rtn.add(new URL(tok.nextToken()));
            }
        }
        return rtn;
    }

    private File createSnippetsDir() {
        File userSnippetsDir = new File(XMLEditorSettings.USER_MERLOT_DIR, "snippets");
        if (!userSnippetsDir.exists()) {
            userSnippetsDir.mkdirs();
        }
        return userSnippetsDir;
    }

    private File downloadSnippet(URLConnection connection, File cacheFile) {
        try {
            BufferedInputStream in = new BufferedInputStream(connection.getInputStream());
            File outFile = File.createTempFile("merlotsnippet", ".jar");
            BufferedOutputStream out = new BufferedOutputStream(new FileOutputStream(outFile));
            int i;
            while ((i = in.read()) != -1) {
                out.write(i);
            }
            out.flush();
            out.close();
            in.close();
            FileUtil.copyFile(outFile, cacheFile);
            outFile.delete();
            return cacheFile;
        } catch (IOException ex) {
            MerlotDebug.exception(ex);
        }
        return null;
    }

    /**
     * @return A list of URLs that identify plugins that should be made
     *         available
     * @throws IOException 
     */
    public java.util.List<File> getSnippets() {
        java.util.List<File> rtn = new ArrayList<File>();
        String s = getProperty(MERLOT_SNIPPETS_INFO);
        if (s != null) {
            File userSnippetsDir = createSnippetsDir();
            InputStream is = getClass().getResourceAsStream("/" + s);
            BufferedReader br = new BufferedReader(new InputStreamReader(is));
            String line;
            try {
                while ((line = br.readLine()) != null) {
                    URL url = getClass().getResource("/" + line);
                    URLConnection connection = url.openConnection();
                    File cache = new File(userSnippetsDir, line);
                    File snippet = downloadSnippet(connection, cache);
                    if (snippet.exists()) {
                        rtn.add(snippet);
                    }
                }
                br.close();
                is.close();
            } catch (IOException e) {
                MerlotDebug.exception(e);
            }
        }
        return rtn;
    }

    public Rectangle getEditorBounds() {
        int x = getSavedEditorTopLeftXCoordinate();
        int y = getSavedEditorTopLeftYCoordinate();
        int width = getSavedEditorWidth();
        int height = getSavedEditorHeight();
        if (x == -1 || y == -1 || width == -1 || height == -1) return null;
        return new Rectangle(x, y, width, height);
    }

    public Dimension getDeskTopDimension() {
        int width = getSavedDesktopWidth();
        int height = getSavedDesktopHeight();
        if (width == -1 || height == -1) return null;
        return new Dimension(width, height);
    }

    public Dimension getEditPanelDimension() {
        int width = getSavedEditPanelWidth();
        int height = getSavedEditPanelHeight();
        if (width == -1 || height == -1) return null;
        return new Dimension(width, height);
    }

    public Dimension getTreePanelDimension() {
        int width = getSavedTreePanelWidth();
        int height = getSavedTreePanelHeight();
        if (width == -1 || height == -1) return null;
        return new Dimension(width, height);
    }

    public Dimension getAttributePanelDimension() {
        int width = getSavedAttributePanelWidth();
        int height = getSavedAttributePanelHeight();
        if (width == -1 || height == -1) return null;
        return new Dimension(width, height);
    }

    /**
     * Returns the stored width of the column called name
     */
    public int getColumnWidth(String name) {
        return getIntegerProperty(name + COLUMN_WIDTH);
    }

    public void saveUserProperties() {
        for (int i = 0; i < _recentFileList.size(); i++) {
            _userProps.put(RECENT_FILE_PREFIX + i, _recentFileList.get(i));
        }
        saveEditorPosition();
        saveProperties(_userProps);
    }

    public void setUserProperty(String property, String value) {
        if (property != null && value != null) {
            _userProps.put(property, value);
        }
    }

    private void saveEditorPosition() {
        XMLEditorFrame frame = XMLEditorFrame.getSharedInstance();
        Rectangle bounds = frame.getBounds();
        Dimension screenSize = Toolkit.getDefaultToolkit().getScreenSize();
        if (bounds.x < screenSize.width) _userProps.put(EDITOR_X, Integer.toString(bounds.x));
        if (bounds.y < screenSize.height) _userProps.put(EDITOR_Y, Integer.toString(bounds.y));
        _userProps.put(EDITOR_WIDTH, Integer.toString(bounds.width));
        _userProps.put(EDITOR_HEIGHT, Integer.toString(bounds.height));
        Dimension d = frame.getDesktopPane().getSize();
        _userProps.put(DESKTOP_WIDTH, Integer.toString(d.width));
        _userProps.put(DESKTOP_HEIGHT, Integer.toString(d.height));
    }

    protected void saveSplitPaneSetup(XMLEditorDocUI ui) {
        int h = ui._splitPane.getDividerLocation();
        _userProps.put(SPLIT_PANE_H, Integer.toString(h));
        Dimension de = ui._editPanel.getSize();
        Dimension dt = ui._treePanel.getSize();
        _userProps.put(EDIT_PANEL_WIDTH, Integer.toString(de.width));
        _userProps.put(EDIT_PANEL_HEIGHT, Integer.toString(de.height));
        _userProps.put(TREE_PANEL_WIDTH, Integer.toString(dt.width));
        _userProps.put(TREE_PANEL_HEIGHT, Integer.toString(dt.height));
        int cols = ui._table.getColumnCount();
        for (int i = 0; i < cols; i++) {
            String name = ui._table.getColumnName(i);
            _userProps.put(name.toLowerCase() + COLUMN_WIDTH, Integer.toString(ui.getTreeTableColumnWidth(name)));
        }
    }

    private int getSavedEditorTopLeftXCoordinate() {
        return getIntegerProperty(EDITOR_X);
    }

    private int getSavedEditorTopLeftYCoordinate() {
        return getIntegerProperty(EDITOR_Y);
    }

    private int getSavedEditorWidth() {
        return getIntegerProperty(EDITOR_WIDTH);
    }

    private int getSavedEditorHeight() {
        return getIntegerProperty(EDITOR_HEIGHT);
    }

    private int getSavedDesktopHeight() {
        return getIntegerProperty(DESKTOP_HEIGHT);
    }

    private int getSavedDesktopWidth() {
        return getIntegerProperty(DESKTOP_WIDTH);
    }

    private int getSavedEditPanelHeight() {
        return getIntegerProperty(EDIT_PANEL_HEIGHT);
    }

    private int getSavedEditPanelWidth() {
        return getIntegerProperty(EDIT_PANEL_WIDTH);
    }

    private int getSavedTreePanelHeight() {
        return getIntegerProperty(TREE_PANEL_HEIGHT);
    }

    private int getSavedTreePanelWidth() {
        return getIntegerProperty(TREE_PANEL_WIDTH);
    }

    private int getSavedAttributePanelHeight() {
        return getIntegerProperty(ATT_PANEL_HEIGHT);
    }

    private int getSavedAttributePanelWidth() {
        return getIntegerProperty(ATT_PANEL_WIDTH);
    }

    public int getHorizontalSplitPanePercentage() {
        return getIntegerProperty(SPLIT_PANE_H);
    }

    private int getIntegerProperty(String property) {
        String s = getProperty(property);
        if (s == null || s.length() == 0) return -1;
        try {
            return Integer.parseInt(s);
        } catch (NumberFormatException nfe) {
            MerlotDebug.msg("Trying to get property " + property + " Couldn't convert " + s + " to an integer");
        }
        return -1;
    }
}
