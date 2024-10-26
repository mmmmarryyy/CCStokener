package onepoint.project.applet;

import java.io.IOException;
import java.lang.reflect.InvocationTargetException;
import java.net.JarURLConnection;
import java.net.URL;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.TreeMap;
import java.util.jar.Attributes;
import java.util.jar.Manifest;
import javax.swing.SwingUtilities;
import onepoint.express.XChoice;
import onepoint.express.XComponent;
import onepoint.express.XDisplay;
import onepoint.express.XExtendedComponent;
import onepoint.express.XReflectionProxy;
import onepoint.express.XUndoHandler;
import onepoint.express.applet.XExpressApplet;
import onepoint.log.XLog;
import onepoint.log.XLogFactory;
import onepoint.project.modules.project.components.OpGanttValidator;
import onepoint.project.modules.project_costs.validators.OpCostTypeDescription;
import onepoint.project.modules.project_planning.components.OpProjectComponent;
import onepoint.project.util.OpHashProvider;
import onepoint.project.util.OpProjectCalendar;
import onepoint.project.util.OpProjectConstants;
import onepoint.project.validators.OpProjectValidator;
import onepoint.service.XBinaryClient;
import onepoint.service.XMessage;
import onepoint.util.XBase64;
import onepoint.util.XCalendar;
import onepoint.util.XCookieManager;

/**
 * Applet used for the expander application.
 */
public class OpOpenApplet extends XExpressApplet {

    /**
    * This class logger.
    */
    private static final XLog logger = XLogFactory.getLogger(OpOpenApplet.class);

    /**
    * Default war name (in case the server doesn't send an explicit one).
    */
    private static final String DEFAULT_CONTEXT_PATH = "opproject";

    private String version;

    private Date build;

    private boolean initialized = false;

    /**
    * Registers project proxies.
    */
    protected void initialize() {
        Class[] classes = { OpGanttValidator.class, OpProjectComponent.class, OpHashProvider.class, Set.class, Map.class, List.class, Iterator.class, java.io.File.class, TreeMap.class, OpProjectCalendar.class, OpProjectValidator.class, OpCostTypeDescription.class, XUndoHandler.class, XChoice.class };
        XComponent.registerProxy(new XReflectionProxy(classes));
        XCalendar.register(new OpProjectCalendar());
    }

    /**
    *
    */
    public OpOpenApplet() {
        super();
        if (!initialized) {
            initialize();
            initialized = true;
        }
    }

    /**
    * @pre
    * @post
    */
    private void getManifestInfo() {
        Manifest mf = getManifest();
        if (mf != null) {
            Attributes attr = mf.getAttributes("Implementation");
            if (attr != null) {
                version = attr.getValue(Attributes.Name.IMPLEMENTATION_VERSION);
            }
            attr = mf.getMainAttributes();
            if (attr != null) {
                String buildString = attr.getValue("Build-Date");
                if (buildString != null) {
                    SimpleDateFormat df = new SimpleDateFormat("yyyyMMdd");
                    try {
                        build = df.parse(buildString);
                    } catch (ParseException exc) {
                    }
                }
            }
        } else {
            version = OpOpenApplet.class.getPackage().getImplementationVersion();
        }
    }

    /**
    * @see onepoint.express.applet.XExpressApplet#getAppletPath()
    */
    @Override
    protected String getAppletPath() {
        String path = super.getAppletPath();
        if (path == null) {
            path = "/" + DEFAULT_CONTEXT_PATH + "/service";
        }
        return path;
    }

    /**
    * @see onepoint.express.applet.XExpressApplet#getParameters()
    */
    @Override
    protected HashMap getParameters() {
        HashMap parameters = super.getParameters();
        String runLevel = getParameter(OpProjectConstants.RUN_LEVEL);
        if (runLevel != null) {
            parameters.put(OpProjectConstants.RUN_LEVEL, runLevel);
        }
        String startForm = getParameter(OpProjectConstants.START_FORM);
        String autoLoginStartForm = getParameter(OpProjectConstants.AUTO_LOGIN_START_FORM);
        getClient().setVariable(OpProjectConstants.START_FORM, startForm);
        getClient().setVariable(OpProjectConstants.AUTO_LOGIN_START_FORM, autoLoginStartForm);
        if (startForm != null) {
            parameters.put(OpProjectConstants.START_FORM, startForm);
        }
        if (autoLoginStartForm != null) {
            parameters.put(OpProjectConstants.AUTO_LOGIN_START_FORM, autoLoginStartForm);
        }
        String params = getParameter(OpProjectConstants.PARAMETERS);
        if (params != null) {
            parameters.put(OpProjectConstants.PARAMETERS, params);
        }
        return parameters;
    }

    /**
    * @throws IOException
    * @see onepoint.express.XViewer#showStartForm(java.util.Map)
    */
    public void showForm(String location) throws IOException {
        getDisplay().showForm(location);
    }

    /**
    * @throws IOException
    * @see onepoint.express.XViewer#showStartForm(java.util.Map)
    */
    public void showForm(String location, Map parameters) throws IOException {
        getDisplay().showForm(location, parameters);
    }

    public void showMainForm(int group, int pos) throws IOException {
        showMainForm(group, pos, null);
    }

    private void showMainForm(int group, int pos, String mainLocation) throws IOException {
        XComponent dockFrame = XDisplay.findFrame("DockFrame");
        XComponent form = (XComponent) dockFrame.getChild(0);
        XExtendedComponent box = (XExtendedComponent) form.findComponent("NavigationBox");
        box.requestFocus();
        XComponent main_frame = XDisplay.findFrame("MainFrame");
        if (mainLocation != null) {
            main_frame.showForm(mainLocation);
        }
        box.deselectNavigationItems();
        box.selectNavigationItem(group, pos);
        dockFrame.refreshForm();
    }

    /**
    * @throws IOException
    * @see onepoint.express.XViewer#showStartForm(java.util.Map)
    */
    @Override
    public void showStartForm(Map parameters) throws IOException {
        String startForm = (String) parameters.get(OpProjectConstants.START_FORM);
        if (startForm != null) {
            getClient().setVariable(OpProjectConstants.START_FORM, startForm);
        }
        String runLevel = (String) parameters.get(OpProjectConstants.RUN_LEVEL);
        boolean success = false;
        if (runLevel != null && Byte.parseByte(runLevel) == OpProjectConstants.SUCCESS_RUN_LEVEL) {
            success = autoLogin();
            if (success) {
                startForm = (String) parameters.get(OpProjectConstants.AUTO_LOGIN_START_FORM);
            }
        }
        if (startForm == null) {
            if (getClient().getVariable(OpProjectConstants.START_FORM) != null) {
                if (success) {
                    startForm = (String) getClient().getVariable(OpProjectConstants.AUTO_LOGIN_START_FORM);
                } else {
                    startForm = (String) getClient().getVariable(OpProjectConstants.START_FORM);
                }
            } else {
                throw new UnsupportedOperationException("No start form defined");
            }
        }
        if (parameters.get("query") != null) {
            parameters.put("restore", Boolean.FALSE);
        }
        getDisplay().showForm(startForm, parameters);
    }

    /**
    * Automatically log-in the user if the auto-login cookie is set.
    *
    * @return the form to redirect to.
    */
    private boolean autoLogin() {
        XBinaryClient client = (XBinaryClient) getClient();
        String value = client.getCookieValue(XCookieManager.AUTO_LOGIN);
        if (value != null) {
            value = value.indexOf('"') != -1 ? value.replaceAll("\"", "") : value;
            String logindata = XBase64.decodeToString(value);
            XMessage request = new XMessage();
            request.setAction(OpProjectConstants.SIGNON_ACTION);
            int idxDelim = logindata.indexOf(' ');
            request.setArgument(OpProjectConstants.LOGIN_PARAM, logindata.substring(0, idxDelim));
            if (logindata.length() - idxDelim > 1) {
                request.setArgument(OpProjectConstants.PASSWORD_PARAM, logindata.substring(idxDelim + 1));
            }
            request.setVariable(OpProjectConstants.CLIENT_TIMEZONE, OpProjectCalendar.CLIENT_TIMEZONE);
            try {
                XMessage response = client.invokeMethod(request);
                if (response.getError() == null) {
                    final OpProjectCalendar calendar = (OpProjectCalendar) client.getVariable(OpProjectConstants.CALENDAR);
                    if (!SwingUtilities.isEventDispatchThread()) {
                        try {
                            SwingUtilities.invokeAndWait(new Runnable() {

                                public void run() {
                                    XDisplay.getDefaultDisplay().setCalendar(calendar);
                                    XCalendar.setDefaultCalendar(calendar);
                                }
                            });
                        } catch (InterruptedException e) {
                            logger.error("Cannot set default calendar");
                        } catch (InvocationTargetException e) {
                            logger.error("Cannot set default calendar", e);
                        }
                    } else {
                        XDisplay.getDefaultDisplay().setCalendar(calendar);
                        XCalendar.setDefaultCalendar(calendar);
                    }
                    return true;
                }
            } catch (IOException e) {
                return false;
            }
        }
        return false;
    }

    @Override
    public void destroy() {
        super.destroy();
        logger.info(getClass().getName() + " destroyed");
    }

    @Override
    public void init() {
        super.init();
        logger.info(getClass().getName() + " initialized");
    }

    @Override
    public void start() {
        super.start();
        getManifestInfo();
        logger.info(getClass().getName() + " started, version: " + (version == null ? "unknown" : version) + " build: " + (build == null ? "unknown" : new SimpleDateFormat("yyyyMMdd").format(build)));
    }

    @Override
    public void stop() {
        synchronized (XExpressApplet.class) {
            if (getInstances() == 1) {
                logger.info(getClass().getName() + " signing off");
                XMessage request = new XMessage();
                request.setAction(OpProjectConstants.SIGNOFF_ACTION);
                request.setArgument("disconnect", Boolean.TRUE);
                XBinaryClient client = (XBinaryClient) getClient();
                try {
                    client.invokeMethod(request);
                } catch (IOException e) {
                    e.printStackTrace();
                }
            }
        }
        super.stop();
        logger.info(getClass().getName() + " stopped");
    }

    /**
    * Returns the manifest this class is in
    *
    * @return the manifest this class is in
    */
    private static Manifest getManifest() {
        try {
            URL url = OpOpenApplet.class.getResource(OpOpenApplet.class.getSimpleName() + ".class");
            if (url != null) {
                JarURLConnection jconn = (JarURLConnection) url.openConnection();
                Manifest mf = jconn.getManifest();
                return mf;
            } else {
                logger.warn("could not read manifest file!");
            }
        } catch (IOException exc) {
        } catch (ClassCastException exc) {
        }
        return null;
    }
}
