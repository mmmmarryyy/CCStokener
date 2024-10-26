package fw4ex.authoride.plugin;

import java.io.File;
import java.net.MalformedURLException;
import java.net.URL;
import java.util.Scanner;
import org.apache.commons.configuration.XMLConfiguration;
import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.Status;
import org.eclipse.jface.preference.PreferenceConverter;
import org.eclipse.jface.resource.ImageDescriptor;
import org.eclipse.swt.graphics.FontData;
import org.eclipse.swt.graphics.RGB;
import org.eclipse.ui.IViewPart;
import org.eclipse.ui.PartInitException;
import org.eclipse.ui.console.ConsolePlugin;
import org.eclipse.ui.console.IConsole;
import org.eclipse.ui.console.IConsoleConstants;
import org.eclipse.ui.console.IConsoleManager;
import org.eclipse.ui.console.IConsoleView;
import org.eclipse.ui.console.MessageConsole;
import org.eclipse.ui.console.MessageConsoleStream;
import org.eclipse.ui.plugin.AbstractUIPlugin;
import org.osgi.framework.Bundle;
import org.osgi.framework.BundleContext;

/**
 * The activator class controls the plug-in life cycle
 */
public class Activator extends AbstractUIPlugin {

    public static final String PLUGIN_ID = "fw4ex.authoride.plugin";

    private static Activator plugin;

    private static Context context;

    public Activator() {
    }

    /**
     * Method executed on plug-in start. <br/>
     * Instantiates the plug-in context and calls its onStart method.
     */
    @Override
    public void start(BundleContext context) throws Exception {
        super.start(context);
        plugin = this;
        XMLConfiguration config = new XMLConfiguration("config.xml");
        Activator.context = new Context(config, getDefault().getStateLocation().toFile().getAbsolutePath());
        Activator.getContext().onStart();
    }

    /**
     * Method executed on plug-in stop. <br/>
     * Calls the context onStop method and disposes all the resources.
     */
    @Override
    public void stop(BundleContext context) throws Exception {
        Activator.getContext().onStop();
        Activator.context = null;
        plugin = null;
        super.stop(context);
    }

    /**
     * Returns the shared instance
     *
     * @return the shared instance
     */
    public static Activator getDefault() {
        return plugin;
    }

    /**
     * Returns the shared context
     *
     * @return the shared context
     */
    public static Context getContext() {
        return context;
    }

    /**
     * Gets a string value from the preferences
     *
     * @param name the preference name
     * @return the corresponding string value
     */
    public static String getPrefString(String name) {
        return getDefault().getPluginPreferences().getString(name);
    }

    /**
     * Sets a string value in the preferences
     *
     * @param name the preference name
     * @param value the preference value
     */
    public static void setPrefString(String name, String value) {
        getDefault().getPluginPreferences().setValue(name, value);
    }

    /**
     * Gets a boolean value from the preferences
     *
     * @param name the preference name
     * @return the corresponding boolean value
     */
    public static Boolean getPrefBool(String name) {
        return getDefault().getPluginPreferences().getBoolean(name);
    }

    /**
     * Sets a boolean value in the preferences
     *
     * @param name the preference name
     * @param value the preference value
     */
    public static void setPrefBool(String name, Boolean value) {
        getDefault().getPluginPreferences().setValue(name, value);
    }

    /**
     * Gets a RGB value from the preferences
     *
     * @param name the preference name
     * @return the corresponding RGB value
     */
    public static RGB getPrefColor(String name) {
        return PreferenceConverter.getColor(getDefault().getPreferenceStore(), name);
    }

    /**
     * Sets a RGB value in the preferences
     *
     * @param name the preference name
     * @param value the preference value
     */
    public static void setPrefColor(String name, RGB value) {
        PreferenceConverter.setValue(getDefault().getPreferenceStore(), name, value);
    }

    /**
     * Gets a Font Data value from the preferences
     *
     * @param name the preference name
     * @return the corresponding Font Data value
     */
    public static FontData getPrefFont(String name) {
        return PreferenceConverter.getFontData(getDefault().getPreferenceStore(), name);
    }

    /**
     * Sets a Font Data value in the preferences
     *
     * @param name the preference name
     * @param value the preference value
     */
    public static void setPrefFontData(String name, FontData value) {
        PreferenceConverter.setValue(getDefault().getPreferenceStore(), name, value);
    }

    /**
     * Gets an integer value from the preferences
     *
     * @param name the preference name
     * @return the corresponding integer value
     */
    public static Integer getPrefInt(String name) {
        return getDefault().getPluginPreferences().getInt(name);
    }

    /**
     * Sets a integer value in the preferences
     *
     * @param name the preference name
     * @param value the preference value
     */
    public static void setPrefInt(String name, Integer value) {
        getDefault().getPluginPreferences().setValue(name, value);
    }

    /**
     * Returns an image descriptor for the image file at the given plug-in
     * relative path
     *
     * @param path the path
     * @return the image descriptor
     */
    public static ImageDescriptor getImageDescriptor(String path) {
        return imageDescriptorFromPlugin(PLUGIN_ID, path);
    }

    /**
     * Code inspired by AbstractUIPlugin : imageDescriptorFromPlugin
     *
     * @param path The path relative to the plug-in
     * @return the full path with the plug-in
     */
    public static URL getURLFromPlugin(String path) {
        if (Bundle.ACTIVE != getDefault().getBundle().getState()) {
            return null;
        }
        URL fullPathString = getDefault().getBundle().getEntry(path);
        if (fullPathString == null) {
            try {
                fullPathString = new URL(path);
            } catch (MalformedURLException e) {
                e.printStackTrace();
                return null;
            }
        }
        return fullPathString;
    }

    /**
     * Gets a file content inside the plug-in
     */
    public static String getFileContentFromPlugin(String path) {
        URL url = getURLFromPlugin(path);
        StringBuffer sb = new StringBuffer();
        try {
            Scanner scanner = new Scanner(url.openStream());
            while (scanner.hasNextLine()) {
                String line = scanner.nextLine();
                sb.append(line + "\n");
            }
            scanner.close();
        } catch (Exception e) {
            e.printStackTrace();
            return "";
        }
        return sb.toString();
    }

    public static File getFileFromHomeDir(String relativePath) {
        File f = new File(".");
        return new File(f.getAbsolutePath() + File.separator + relativePath);
    }

    public static IViewPart getView(String viewId) {
        try {
            return getDefault().getWorkbench().getActiveWorkbenchWindow().getActivePage().showView(viewId);
        } catch (PartInitException e) {
            e.printStackTrace();
        }
        return null;
    }

    /**
     * Create a log
     *
     * @param msg the message to log
     * @param status the message status (IStatus)
     */
    public static void log(String msg, int severity, int code) {
        getDefault().getLog().log(new Status(severity, "FW4EX", code, msg, null));
    }

    public static void logInfo(String msg) {
        getDefault().getLog().log(new Status(0, "FW4EX", IStatus.INFO, msg, null));
    }

    /** Retrieves the wanted console. <br/>
     * If it doesn't exist yet, it's created and added to the console manager.
     * @param name The name of the wanted console.
     * @return The wanted console
     */
    private static MessageConsole findConsole(String name) {
        ConsolePlugin plugin = ConsolePlugin.getDefault();
        IConsoleManager conMan = plugin.getConsoleManager();
        IConsole[] existing = conMan.getConsoles();
        for (int i = 0; i < existing.length; i++) {
            if (name.equals(existing[i].getName())) {
                return (MessageConsole) existing[i];
            }
        }
        MessageConsole myConsole = new MessageConsole(name, null);
        conMan.addConsoles(new IConsole[] { myConsole });
        return myConsole;
    }

    /** Prints a message on the plug-in console.
     * @param msg The message to print.
     */
    public static void println(String msg) {
        MessageConsole myConsole = findConsole("FW4EX Console");
        MessageConsoleStream out = myConsole.newMessageStream();
        out.println(msg);
        try {
            IConsoleView view = (IConsoleView) getView(IConsoleConstants.ID_CONSOLE_VIEW);
            view.display(myConsole);
        } catch (Exception e) {
        }
    }
}
