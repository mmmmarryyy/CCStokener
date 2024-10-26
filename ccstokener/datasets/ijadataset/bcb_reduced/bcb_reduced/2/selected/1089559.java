package org.plog4u.webbrowser.internal;

import java.io.File;
import java.io.InputStreamReader;
import java.io.Reader;
import java.net.URL;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import org.eclipse.core.runtime.IConfigurationElement;
import org.eclipse.core.runtime.IExtensionRegistry;
import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.Platform;
import org.eclipse.core.runtime.Status;
import org.eclipse.jface.dialogs.MessageDialog;
import org.eclipse.swt.SWT;
import org.eclipse.swt.browser.Browser;
import org.eclipse.swt.widgets.Display;
import org.eclipse.swt.widgets.Shell;
import org.eclipse.ui.IMemento;
import org.eclipse.ui.XMLMemento;
import org.plog4u.webbrowser.IExternalWebBrowser;
import org.plog4u.webbrowser.IExternalWebBrowserWorkingCopy;
import org.plog4u.webbrowser.IURLMap;
import org.plog4u.webbrowser.IWebBrowser;

/**
 * Utility class for the Web browser tooling.
 */
public class WebBrowserUtil {

    private static List urlMaps;

    private static List lockedFavorites;

    private static List unlockedFavorites;

    private static final String BROWSER_PACKAGE_NAME = "org.eclipse.swt.browser.Browser";

    public static Boolean isInternalBrowserOperational;

    private static List defaultBrowsers2;

    static class DefaultBrowser {

        String name;

        String params;

        String executable;

        String[] locations;

        public DefaultBrowser(String name, String executable, String params, String[] locations) {
            if (name == null) name = "<unknown>"; else if (name.startsWith("%")) name = WebBrowserUIPlugin.getResource(name);
            this.name = name;
            this.executable = executable;
            this.params = params;
            this.locations = locations;
        }

        public String toString() {
            String s = "Browser: " + name + ", " + executable + ", " + params + ", ";
            if (locations != null) {
                int size = locations.length;
                for (int i = 0; i < size; i++) {
                    s += locations[i] + ";";
                }
            }
            return s;
        }
    }

    /**
	 * WebBrowserUtil constructor comment.
	 */
    public WebBrowserUtil() {
        super();
    }

    /**
	 * Returns true if we're running on Windows.
	 *
	 * @return boolean
	 */
    public static boolean isWindows() {
        String os = System.getProperty("os.name");
        if (os != null && os.toLowerCase().indexOf("win") >= 0) return true; else return false;
    }

    /**
	 * Returns true if we're running on linux.
	 *
	 * @return boolean
	 */
    public static boolean isLinux() {
        String os = System.getProperty("os.name");
        if (os != null && os.toLowerCase().indexOf("lin") >= 0) return true; else return false;
    }

    /**
	 * Open a dialog window.
	 *
	 * @param title java.lang.String
	 * @param message java.lang.String
	 */
    public static void openError(String message) {
        Display d = Display.getCurrent();
        if (d == null) d = Display.getDefault();
        Shell shell = d.getActiveShell();
        MessageDialog.openError(shell, WebBrowserUIPlugin.getResource("%errorDialogTitle"), message);
    }

    /**
	 * Open a dialog window.
	 *
	 * @param title java.lang.String
	 * @param message java.lang.String
	 */
    public static void openMessage(String message) {
        Display d = Display.getCurrent();
        if (d == null) d = Display.getDefault();
        Shell shell = d.getActiveShell();
        MessageDialog.openInformation(shell, WebBrowserUIPlugin.getResource("%searchingTaskName"), message);
    }

    /**
	 * Returns a List of all URL maps.
	 *
	 * @return java.util.List
	 */
    public static List getURLMaps() {
        if (urlMaps == null) loadURLMaps();
        return urlMaps;
    }

    /**
	 * Load the url map extension point.
	 */
    private static void loadURLMaps() {
        Trace.trace(Trace.FINEST, "->- Loading .urlMap extension point ->-");
        IExtensionRegistry registry = Platform.getExtensionRegistry();
        IConfigurationElement[] cf = registry.getConfigurationElementsFor(WebBrowserUIPlugin.PLUGIN_ID, "urlMap");
        int size = cf.length;
        urlMaps = new ArrayList(size);
        for (int i = 0; i < size; i++) {
            try {
                IURLMap mapper = (IURLMap) cf[i].createExecutableExtension("class");
                urlMaps.add(mapper);
                Trace.trace(Trace.FINEST, "  Loaded url map: " + cf[i].getAttribute("id"));
            } catch (Throwable t) {
                Trace.trace(Trace.SEVERE, "  Could not load url map: " + cf[i].getAttribute("id"), t);
            }
        }
        Trace.trace(Trace.FINEST, "-<- Done loading .urlMap extension point -<-");
    }

    /**
	 * Returns a List of all unlocked favorites.
	 *
	 * @return java.util.List
	 */
    public static List getUnlockedFavorites() {
        if (unlockedFavorites == null) loadFavorites();
        return unlockedFavorites;
    }

    /**
	 * Returns a List of all locked favorites.
	 *
	 * @return java.util.List
	 */
    public static List getLockedFavorites() {
        if (lockedFavorites == null) loadFavorites();
        return lockedFavorites;
    }

    /**
	 * Load the favorites extension point.
	 */
    private static void loadFavorites() {
        Trace.trace(Trace.FINEST, "->- Loading .favorites extension point ->-");
        IExtensionRegistry registry = Platform.getExtensionRegistry();
        IConfigurationElement[] cf = registry.getConfigurationElementsFor(WebBrowserUIPlugin.PLUGIN_ID, "favorites");
        int size = cf.length;
        unlockedFavorites = new ArrayList(size);
        lockedFavorites = new ArrayList(size);
        for (int i = 0; i < size; i++) {
            try {
                Favorite f = new Favorite(cf[i].getAttribute("name"), cf[i].getAttribute("url"));
                String locked = cf[i].getAttribute("locked");
                if (!"true".equals(locked)) unlockedFavorites.add(f); else lockedFavorites.add(f);
                Trace.trace(Trace.FINEST, "  Loaded favorite: " + cf[i].getAttribute("id"));
            } catch (Throwable t) {
                Trace.trace(Trace.SEVERE, "  Could not load favorite: " + cf[i].getAttribute("id"), t);
            }
        }
        Trace.trace(Trace.FINEST, "-<- Done loading .favorites extension point -<-");
    }

    /**
	 * Returns whether it should be possible to use the internal browser or not, based on whether or not
	 * the org.eclipse.swt.Browser class can be found/loaded. If it can it means is is supported on the platform in which
	 * this plugin is running. If not, disable the ability to use the internal browser.
	 *
	 * @return boolean
	 */
    public static boolean canUseInternalWebBrowser() {
        try {
            Class clazz = Class.forName(BROWSER_PACKAGE_NAME);
            if (clazz != null) return true;
        } catch (ClassNotFoundException e) {
        }
        return false;
    }

    /**
	 * This method checks to see if it can new up a new Browser. If the SWT widget can not be bound
	 * to the particular operating system it throws an SWTException. We catch that and set a boolean
	 * flag which represents whether or not we were successfully able to create a Browser instance or not.
	 * If not, don't bother adding the Internal Web Browser that uses this widget. Designed to be attemped
	 * only once and the flag set used throughout.
	 * 
	 * @return boolean
	 */
    public static boolean isInternalBrowserOperational() {
        if (isInternalBrowserOperational != null) return isInternalBrowserOperational.booleanValue();
        try {
            new Browser(new Shell(Display.getCurrent()), SWT.NONE);
            isInternalBrowserOperational = new Boolean(true);
        } catch (Throwable t) {
            WebBrowserUIPlugin.getInstance().getLog().log(new Status(IStatus.WARNING, WebBrowserUIPlugin.PLUGIN_ID, 0, "Internal browser is not operational", t));
            isInternalBrowserOperational = new Boolean(false);
        }
        return isInternalBrowserOperational.booleanValue();
    }

    public static List getExternalBrowserPaths() {
        List paths = new ArrayList();
        Iterator iterator = BrowserManager.getInstance().getWebBrowsers().iterator();
        while (iterator.hasNext()) {
            IWebBrowser wb = (IWebBrowser) iterator.next();
            if (wb instanceof IExternalWebBrowser) {
                IExternalWebBrowser ext = (IExternalWebBrowser) wb;
                paths.add(ext.getLocation().toLowerCase());
            }
        }
        return paths;
    }

    public static void addFoundBrowsers(List list) {
        List paths = getExternalBrowserPaths();
        Iterator iterator = getDefaultBrowsers().iterator();
        while (iterator.hasNext()) {
            DefaultBrowser browser2 = (DefaultBrowser) iterator.next();
            if (browser2.locations != null) {
                int size = browser2.locations.length;
                for (int j = 0; j < size; j++) {
                    String location = browser2.locations[j];
                    if (!paths.contains(location.toLowerCase())) {
                        try {
                            File f = new File(location);
                            if (f.exists()) {
                                ExternalWebBrowser browser = new ExternalWebBrowser();
                                browser.name = browser2.name;
                                browser.location = location;
                                browser.parameters = browser2.params;
                                list.add(browser);
                                BrowserManager.getInstance().addBrowser(browser);
                                j += size;
                            }
                        } catch (Exception e) {
                        }
                    }
                }
            }
        }
    }

    /**
	 * Create an external Web browser if the file matches the default (known) browsers.
	 * @param file
	 * @return
	 */
    public static IExternalWebBrowserWorkingCopy createExternalBrowser(File file) {
        if (file == null || !file.isFile()) return null;
        String executable = file.getName();
        Iterator iterator = getDefaultBrowsers().iterator();
        while (iterator.hasNext()) {
            DefaultBrowser db = (DefaultBrowser) iterator.next();
            if (executable.equals(db.executable)) {
                IExternalWebBrowserWorkingCopy browser = BrowserManager.getInstance().createExternalWebBrowser();
                browser.setName(db.name);
                browser.setLocation(file.getAbsolutePath());
                browser.setParameters(db.params);
                return browser;
            }
        }
        return null;
    }

    protected static List getDefaultBrowsers() {
        if (defaultBrowsers2 != null) return defaultBrowsers2;
        Reader reader = null;
        defaultBrowsers2 = new ArrayList();
        try {
            URL url = WebBrowserUIPlugin.getInstance().getBundle().getEntry("defaultBrowsers.xml");
            URL url2 = Platform.resolve(url);
            reader = new InputStreamReader(url2.openStream());
            IMemento memento = XMLMemento.createReadRoot(reader);
            IMemento[] children = memento.getChildren("browser");
            if (children != null) {
                int size = children.length;
                for (int i = 0; i < size; i++) {
                    IMemento child = children[i];
                    String name = child.getString("name");
                    String executable = child.getString("executable");
                    String params = child.getString("params");
                    List locations = new ArrayList();
                    IMemento[] locat = child.getChildren("location");
                    if (locat != null) {
                        int size2 = locat.length;
                        for (int j = 0; j < size2; j++) locations.add(locat[j].getTextData());
                    }
                    String[] loc = new String[locations.size()];
                    locations.toArray(loc);
                    DefaultBrowser db = new DefaultBrowser(name, executable, params, loc);
                    Trace.trace(Trace.CONFIG, "Default browser: " + db);
                    defaultBrowsers2.add(db);
                }
            }
        } catch (Exception e) {
            Trace.trace(Trace.SEVERE, "Error loading default browsers", e);
        } finally {
            try {
                reader.close();
            } catch (Exception e) {
            }
        }
        return defaultBrowsers2;
    }
}
