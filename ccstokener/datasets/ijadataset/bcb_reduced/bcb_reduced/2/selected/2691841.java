package com.anaxima.eslink.server;

import java.io.BufferedInputStream;
import java.io.BufferedOutputStream;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.URL;
import java.text.MessageFormat;
import java.util.MissingResourceException;
import java.util.ResourceBundle;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.ListenerList;
import org.eclipse.core.runtime.Status;
import org.eclipse.core.runtime.jobs.ISchedulingRule;
import org.eclipse.equinox.security.storage.ISecurePreferences;
import org.eclipse.equinox.security.storage.SecurePreferencesFactory;
import org.eclipse.equinox.security.storage.StorageException;
import org.eclipse.jface.dialogs.ErrorDialog;
import org.eclipse.jface.preference.IPreferenceStore;
import org.eclipse.jsch.core.IJSchService;
import org.eclipse.swt.widgets.Shell;
import org.eclipse.ui.IWorkbench;
import org.eclipse.ui.IWorkbenchWindow;
import org.eclipse.ui.plugin.AbstractUIPlugin;
import org.osgi.framework.Bundle;
import org.osgi.framework.BundleContext;
import org.osgi.util.tracker.ServiceTracker;
import com.anaxima.eslink.server.internal.ServerManager;
import com.anaxima.eslink.tools.log.EclipseLogger;
import com.anaxima.eslink.tools.log.ILogger;

/**
 * The main plugin class that will manage the SLlink backend server.
 * 
 * @author Thomas Vater
 */
public class ServerPlugin extends AbstractUIPlugin implements IServerPluginConstants, ISchedulingRule {

    /** Line separator. */
    public static final String LSEP = System.getProperty("line.separator");

    /**
     * The shared instance of the plugin.
     */
    private static ServerPlugin _plugin;

    /**
	 * The logger to use.
	 */
    private static ILogger _logger;

    /**
	 * Our resource bundle.
	 */
    private ResourceBundle _resourceBundle;

    /**
	 * Eslink server manager.
	 */
    private ServerManager _slinkMgr = null;

    /**
     * The path to the Eslink backend catalog.
     */
    private String _backendCatalogPath = null;

    /**
     * Tracker for services.
     */
    private ServiceTracker _serviceTracker;

    /**
	 * List of event listeners for environment change.
	 */
    private ListenerList _envChangeListeners = new ListenerList(ListenerList.EQUALITY);

    /**
     * List of event listeners for session handling.
     */
    private ListenerList _sessionListeners = new ListenerList(ListenerList.EQUALITY);

    /**
	 * Default constructor.
	 */
    public ServerPlugin() {
        super();
        _plugin = this;
        try {
            _resourceBundle = ResourceBundle.getBundle(RESOURCE_BUNDLE);
        } catch (MissingResourceException x) {
            _resourceBundle = null;
        }
    }

    /**
	 * This method is called upon plug-in activation
	 */
    public void start(BundleContext argContext) throws Exception {
        super.start(argContext);
        _serviceTracker = new ServiceTracker(getBundle().getBundleContext(), IJSchService.class.getName(), null);
        _serviceTracker.open();
        _createBackendCatalog(argContext);
    }

    /**
	 * This method is called when the plug-in is stopped
	 */
    public void stop(BundleContext context) throws Exception {
        _serviceTracker.close();
        if (_slinkMgr != null) _slinkMgr.disposeSession();
        super.stop(context);
    }

    /**
	 * Returns the shared instance.
	 */
    public static ServerPlugin getPlugin() {
        return _plugin;
    }

    /**
     * Show a message dialog and log the error.
     * 
     * @param   argTitleKey
     *          Title key to lookup.
     * @param   argMsg 
     *          Message key to lookup.
     * @param   argParams
     *          List of message parameters.
     * @param   argException
     *          Exception to log.
     *          
     * @return  Generated status object.
     */
    public static IStatus showErrorDialog(String argTitleKey, String argMsgKey, Object[] argParams, Throwable argException) {
        String msg = ServerPlugin.getMessage(argMsgKey, argParams);
        String title = ServerPlugin.getMessage(argTitleKey, argParams);
        IStatus status = new Status(IStatus.ERROR, ServerPlugin.PLUGIN_ID, IStatus.OK, msg, argException);
        ErrorDialog.openError(ServerPlugin.getPlugin().getShell(), title, msg, status);
        getLogger().error(msg, argException);
        return status;
    }

    /**
	 * Returns the log object.
	 */
    public static ILogger getLogger() {
        if (_logger == null) {
            if (_plugin == null) {
                _logger = new EclipseLogger(PLUGIN_ID);
            } else {
                _logger = new EclipseLogger(ServerPlugin.getPlugin().getLog(), PLUGIN_ID);
            }
        }
        return _logger;
    }

    /**
     * Returns the resource string with the given key in
     * the resource bundle. If there isn't any value under
     * the given key, the key is returned.
     *
     * @param	argKey The resource key.
     * @return	The resource string.
     */
    public static String getMessage(String argKey) {
        if (_plugin == null) {
            return argKey;
        }
        ResourceBundle bundle = ServerPlugin.getPlugin().getResourceBundle();
        if (bundle == null) return argKey;
        try {
            return bundle.getString(argKey);
        } catch (MissingResourceException e) {
            return argKey;
        }
    }

    /**
     * Returns the formatted message for the given key in
     * the resource bundle. 
     *
     * @param	argKey The resource key.
     * @param	argArgs The message arguments.
     * @return	The resource string.
     */
    public static String getMessage(String argKey, Object[] argArgs) {
        return MessageFormat.format(getMessage(argKey), argArgs);
    }

    /**
     * Returns the formatted message for the given key in
     * the resource bundle. 
     *
     * @param	argKey The resource key.
     * @param	argArg1 The first message argument.
     * @param	argArg2 The second message argument.
     * @param	argArg3 The third message argument.
     * @return	The resource string.
     */
    public static String getMessage(String argKey, Object argArg1, Object argArg2, Object argArg3) {
        return MessageFormat.format(getMessage(argKey), new Object[] { argArg1, argArg2, argArg3 });
    }

    /**
     * Returns the formatted message for the given key in
     * the resource bundle. 
     *
     * @param	argKey The resource key.
     * @param	argArg1 The first message argument.
     * @param	argArg2 The second message argument.
     * @return	The resource string.
     */
    public static String getMessage(String argKey, Object argArg1, Object argArg2) {
        return MessageFormat.format(getMessage(argKey), new Object[] { argArg1, argArg2 });
    }

    /**
     * Returns the formatted message for the given key in
     * the resource bundle. 
     *
     * @param	argKey The resource key.
     * @param	argArg1 The single message argument.
     * @return	The resource string.
     */
    public static String getMessage(String argKey, Object argArg1) {
        return MessageFormat.format(getMessage(argKey), new Object[] { argArg1 });
    }

    /**
	 * Returns the plugin's resource bundle,
	 */
    public ResourceBundle getResourceBundle() {
        return _resourceBundle;
    }

    /**
     * Returns the current command facade for a Eslink server session.
     * If there is no session running, this method will start a new session.
     */
    public ICommandFacade getCommandFacade() throws CoreException {
        IPreferenceStore store = ServerPlugin.getPlugin().getPreferenceStore();
        String sasExe = store.getString(IServerPluginConstants.PREFKEY_SASEXE);
        String sasCfg = store.getString(IServerPluginConstants.PREFKEY_SASCFG);
        boolean altLogEnabled = store.getBoolean(IServerPluginConstants.PREFKEY_ALTLOG_ENABLED);
        String altLog = store.getString(IServerPluginConstants.PREFKEY_ALTLOG);
        boolean debugLogEnabled = store.getBoolean(IServerPluginConstants.PREFKEY_DEBUGLOG_ENABLED);
        String debugLog = store.getString(IServerPluginConstants.PREFKEY_DEBUGLOG);
        boolean sshEnabled = store.getBoolean(IServerPluginConstants.PREFKEY_SSH_ENABLED);
        String sshHost = store.getString(IServerPluginConstants.PREFKEY_SSH_HOST);
        int sshPort = store.getInt(IServerPluginConstants.PREFKEY_SSH_PORT);
        boolean ignoreMessage = store.getBoolean(IServerPluginConstants.PREFKEY_SSH_IGNORE_MESSAGES);
        String sshTempDir = store.getString(IServerPluginConstants.PREFKEY_SSH_TEMPDIR);
        String sshBinFolder = store.getString(IServerPluginConstants.PREFKEY_SSH_BINFOLDER);
        ISecurePreferences root = SecurePreferencesFactory.getDefault();
        ISecurePreferences node = root.node(IServerPluginConstants.PREFKEY_SSH_SECSTORE);
        String sshUser = null;
        String sshPassword = null;
        try {
            sshUser = node.get("user", "");
            sshPassword = node.get("password", "");
        } catch (StorageException e) {
            ServerPlugin.getLogger().error("Error retrieving secure information.", e);
        }
        if (sasExe == null || sasExe.length() == 0) {
            throw new CoreException(new Status(IStatus.ERROR, ServerPlugin.PLUGIN_ID, IStatus.OK, ServerPlugin.getMessage("eslinkserver.error.config"), null));
        }
        if (_slinkMgr == null) _slinkMgr = ServerManager.getInstance();
        _slinkMgr.setSasExe(sasExe);
        _slinkMgr.setSasCfg(sasCfg);
        _slinkMgr.setAltLog((altLogEnabled) ? altLog : null);
        _slinkMgr.setDebugLog((debugLogEnabled) ? debugLog : null);
        _slinkMgr.setSclPath(_backendCatalogPath);
        if (sshEnabled) {
            SshConnectionParameters sshParameters = new SshConnectionParameters();
            sshParameters.setHost(sshHost);
            sshParameters.setPort(sshPort);
            sshParameters.setIgnoreMessages(ignoreMessage);
            sshParameters.setRemoteTempDir(sshTempDir);
            sshParameters.setRemoteBinFolder(sshBinFolder);
            sshParameters.setUser(sshUser);
            sshParameters.setPassword(sshPassword);
            _slinkMgr.setSshParameters(sshParameters);
        } else {
            _slinkMgr.setSshParameters(null);
        }
        return _slinkMgr.getCommandFacade();
    }

    /**
	 * Remove environment change listener.
	 */
    public void removeEnvironmentChangeListener(IEnvironmentChangeListener argListener) {
        _envChangeListeners.remove(argListener);
    }

    /**
	 * Add environment change listener.
	 */
    public void addEnvironmentChangeListener(IEnvironmentChangeListener argListener) {
        _envChangeListeners.add(argListener);
    }

    /**
	 * Fire environment change event.
	 */
    public void fireEnvironmentChangeEvent(EnvironmentChangeEvent argEvent) {
        Object[] listeners = _envChangeListeners.getListeners();
        for (int i = 0; i < listeners.length; i++) {
            ((IEnvironmentChangeListener) listeners[i]).environmentChanged(argEvent);
        }
    }

    /**
     * Remove server session listener.
     */
    public void removeServerSessionListener(IEslinkServerSessionListener argListener) {
        _sessionListeners.remove(argListener);
    }

    /**
     * Add server session change listener.
     */
    public void addServerSessionListener(IEslinkServerSessionListener argListener) {
        _sessionListeners.add(argListener);
    }

    /**
     * Fire environment change event.
     */
    public void fireServerSessionEvent(ServerSessionEvent argEvent) {
        Object[] listeners = _sessionListeners.getListeners();
        for (int i = 0; i < listeners.length; i++) {
            ((IEslinkServerSessionListener) listeners[i]).sessionStateChanged(argEvent);
        }
    }

    /**
     * Get the workbench shell.
     */
    public Shell getShell() {
        Shell ret = null;
        IWorkbench wb = this.getWorkbench();
        if (wb != null) {
            IWorkbenchWindow wbWin = wb.getActiveWorkbenchWindow();
            if (wbWin != null) ret = wbWin.getShell();
        }
        return ret;
    }

    /**
     * Copy the Eslink backend catalog into the plugins scratch area.
     */
    private void _createBackendCatalog(BundleContext argContext) throws IOException {
        File targetDir = argContext.getDataFile("/slinksrv/");
        targetDir.mkdirs();
        _backendCatalogPath = targetDir.getAbsolutePath();
        File targetFile = new File(targetDir, "eslinksrv.cpo");
        Bundle bundle = argContext.getBundle();
        URL url = bundle.getEntry("/sasbin/slinksrv/eslinksrv.cpo");
        InputStream in = new BufferedInputStream(url.openStream());
        OutputStream out = new BufferedOutputStream(new FileOutputStream(targetFile));
        int c;
        while ((c = in.read()) != -1) out.write(c);
        out.close();
        in.close();
    }

    /**
     * This implementation of a scheduling rule containment says that
     * the Eslink server only contains itself.
     * <p>
     * @see org.eclipse.core.runtime.jobs.ISchedulingRule#contains(org.eclipse.core.runtime.jobs.ISchedulingRule)
     */
    public boolean contains(ISchedulingRule argRule) {
        return argRule == this;
    }

    /**
     * The Eslink server scheduling rule conflicts with itself. Thus
     * only one client at a time may access the server.
     * @see org.eclipse.core.runtime.jobs.ISchedulingRule#isConflicting(org.eclipse.core.runtime.jobs.ISchedulingRule)
     */
    public boolean isConflicting(ISchedulingRule argRule) {
        return argRule == this;
    }

    /**
     * Return JSch service interface. 
     */
    public IJSchService getJSchService() {
        return (IJSchService) _serviceTracker.getService();
    }
}
