package org.rjam;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.Serializable;
import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.lang.reflect.Modifier;
import java.net.HttpURLConnection;
import java.net.InetAddress;
import java.net.URL;
import java.net.URLConnection;
import java.net.UnknownHostException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Properties;
import java.util.TimeZone;
import org.rjam.admin.AdminServer;
import org.rjam.api.IClass;
import org.rjam.api.ICollector;
import org.rjam.api.IComponent;
import org.rjam.api.IInstrumentor;
import org.rjam.api.IMethod;
import org.rjam.api.IReporter;
import org.rjam.base.BaseLogging;
import org.rjam.base.BaseThread;
import org.rjam.base.Logger;
import org.rjam.error.NullCollector;
import org.rjam.error.NullReporter;
import org.rjam.util.Expression;
import org.rjam.xml.Parser;
import org.rjam.xml.Token;

public class Monitor extends BaseLogging implements Serializable {

    public static final String VERSION = "VB02_04";

    public static final String PROP_TARGET_CLASS = "rjamTargetClass";

    public static final long serialVersionUID = 1L;

    public static final String PROP_CONFIG_FILE_NAME = "rjamConfigFile";

    public static final String PROP_LIBRARY_DIR = "rjamLibraryDir";

    public static final String TOKEN_NAME = "Name";

    public static final String TOKEN_DESCRIPTION = "Description";

    public static final String TOKEN_CLASS_PATTERN = "ClassPattern";

    public static final String TOKEN_IGNORE_PATTERN = "IgnorePattern";

    public static final String TOKEN_METHOD_PATTERN = "MethodPattern";

    public static final String TOKEN_COLLECTOR = "Collector";

    public static final String TOKEN_REPORTER = "Reporter";

    public static final String TOKEN_CLASSNAME = "ClassName";

    public static final String TOKEN_PARAMETER = "Parameter";

    public static final String TOKEN_SERVER_NAME = "ServerName";

    public static final String TOKEN_APP_NAME = "ApplicationName";

    public static final String PROP_APP_NAME = "RJam.AppName";

    public static final String TOKEN_VALUE = "Value";

    public static final String TOKEN_MATCH_INTERFACE = "MatchInterface";

    public static final String TOKEN_MATCH_SUPER = "MatchSuper";

    public static final String TOKEN_MAX_SUPER_LEVEL = "MaxSuperLevel";

    public static final String TOKEN_SERVER_ID = "ServerId";

    public static final String TOKEN_DEFAULT_PRIORITY = "DefaultPriority";

    public static final String TOKEN_DEBUG = "Debug";

    public static final String TOKEN_METHOD_CONSTRAINT = "MethodConstraint";

    public static final String TOKEN_CLASS_CONSTRAINT = "ClassConstraint";

    public static final String TOKEN_USE_WRAPPER = "UseWrapper";

    public static final String TOKEN_LIFE_EVENT = "LifeEvent";

    public static final String TOKEN_DISABLE = "Disable";

    public static final String TOKEN_ABSTRACT_PATTERN = "AbstractPattern";

    public static final String TOKEN_CONFIG_URL = "RJamConfigUrl";

    public static final String TOKEN_INSTRUMENTOR = "Instrumentor";

    public static final String DEFAULT_CONFIG_FILE_NAME = "rjam.xml";

    public static final String TOKEN_ADMINISTRATION = "Administration";

    private static List<Monitor> monitors;

    private static Map<String, Monitor> monitorMap = new HashMap<String, Monitor>();

    private static Map<String, IComponent> components = new HashMap<String, IComponent>();

    private static String appName;

    private static Expression globalIgnore;

    private static int serverId = -1;

    private static String serverName;

    private static String shortName;

    private static ICollector errorCollector;

    private String name;

    private String description;

    private String targetIgnore;

    private Expression ignorePattern;

    private List<Expression> abstractPatterns = new ArrayList<Expression>();

    private List<Expression> classPatterns = new ArrayList<Expression>();

    private List<Expression> methodPatterns = new ArrayList<Expression>();

    private List<Expression> lifeEventPatterns = new ArrayList<Expression>();

    private ICollector collector;

    private Token collectorConfig;

    private IReporter reporter;

    private boolean matchInterface = false;

    private boolean matchSuper = false;

    private int maxSuperLevel = 20;

    private boolean debug = false;

    private boolean wrap = false;

    private Map<String, List<String>> methodConstraints = new HashMap<String, List<String>>();

    private List<Expression> classConstraints = new ArrayList<Expression>();

    private boolean enabled = true;

    private static Monitor systemClassMonitor;

    private static boolean waitForGetPropertyPermission = true;

    private static boolean inited = false;

    private static Properties properties;

    private static Token instrumentorConfig;

    private static Logger logger;

    public static String getLifeEventName(String className) {
        String ret = className.replace('.', '_') + "_" + IInstrumentor.VAR_LIFE_EVENT;
        return ret;
    }

    public static String getProperty(String name) {
        return getProperty(name, null);
    }

    public static Properties getProperties() {
        return properties;
    }

    public static void setProperties(Properties properties) {
        Monitor.properties = properties;
    }

    /**
	 * This method is to protect against security error
	 * because the application does not have permission to run;
	 * 
	 * @param name
	 * @param defaultValue
	 * @return The value from the System.getProperty (or properties down-loaded from a remote location).
	 */
    public static String getProperty(String name, String def) {
        String ret = null;
        if (properties != null) {
            ret = properties.getProperty(name);
        }
        if (ret == null) {
            try {
                ret = System.getProperty(name);
            } catch (Throwable e) {
                if (waitForGetPropertyPermission) {
                    int cnt = 0;
                    boolean done = false;
                    while (!done) {
                        try {
                            Thread.sleep(1000);
                        } catch (InterruptedException e1) {
                        }
                        try {
                            ret = System.getProperty(name);
                            done = true;
                        } catch (Throwable e2) {
                            if (++cnt > 300) {
                                done = true;
                                waitForGetPropertyPermission = false;
                            }
                        }
                    }
                }
            }
        }
        if (ret == null) {
            ret = def;
        }
        return ret;
    }

    public static String getServerName() {
        if (serverName == null) {
            synchronized (Monitor.class) {
                getMonitors();
                if (serverName == null) {
                    String tmpName = null;
                    try {
                        int cnt = 0;
                        while (tmpName == null && ++cnt < 50) {
                            tmpName = InetAddress.getLocalHost().getHostName();
                            if ("localhost".equals(tmpName)) {
                                tmpName = null;
                                try {
                                    Thread.sleep(1000);
                                } catch (InterruptedException e) {
                                }
                            }
                        }
                        if (tmpName == null) {
                            tmpName = InetAddress.getLocalHost().getHostName();
                        }
                    } catch (UnknownHostException e) {
                        tmpName = "UnknownHostException";
                    }
                    serverName = tmpName;
                }
            }
        }
        return serverName;
    }

    public static String getShortServerName() {
        if (shortName == null) {
            synchronized (Monitor.class) {
                if (shortName == null) {
                    shortName = getServerName();
                    int idx = shortName.indexOf('.');
                    if (idx > 0) {
                        shortName = shortName.substring(0, idx);
                    }
                }
            }
        }
        return shortName;
    }

    public static boolean isIgnored(String className) {
        boolean ret = false;
        if (monitors == null) {
            getMonitors();
        }
        if (globalIgnore != null) {
            ret = globalIgnore.isMatch(className);
        }
        return ret;
    }

    public static Token getInstrumentorConfig() {
        return instrumentorConfig;
    }

    public static void setInstrumentorConfig(Token instrumentorConfig) {
        if (instrumentorConfig == null) {
            if (!inited) {
                synchronized (Monitor.class) {
                    if (!inited) {
                        getMonitors();
                        if (instrumentorConfig == null) {
                            try {
                                Parser p = new Parser("<JavassistInstrumentor><Name>Reporter</Name><ClassName>org.rjam.Instrumentor</ClassName></JavassistInstrumentor>");
                                instrumentorConfig = p.parse();
                            } catch (IOException e) {
                            }
                        }
                    }
                }
            }
        }
        Monitor.instrumentorConfig = instrumentorConfig;
    }

    public static String getAppName() {
        if (appName == null) {
            getMonitors();
            if (appName == null) {
                appName = "UndefinedAppName";
            }
        }
        return appName;
    }

    public static Monitor getMonitor(String name) {
        Map<String, Monitor> map = getMonitorMap();
        Monitor ret = (Monitor) map.get(name);
        return ret;
    }

    public static Map<String, Monitor> getMonitorMap() {
        if (monitors == null) {
            getMonitors();
        }
        return monitorMap;
    }

    /**
	 * getOrCreateComponent take a Token as a parameter and
	 * 1>  IF the component has a class name then we create one
	 * 2>  If not we look for a globally defined component with this name
	 * 3>  IF none exists, it's an error, return null;
	 * 
	 * @param token specifying the requested IComponent
	 * @param register the component if one is created.
	 * 
	 * @return
	 * @throws ClassNotFoundException
	 * @throws InstantiationException
	 * @throws IllegalAccessException
	 */
    public static IComponent getOrCreateComponent(Token token, boolean register) throws ClassNotFoundException, InstantiationException, IllegalAccessException {
        IComponent ret = null;
        Logger logger = Logger.getLogger(Monitor.class);
        Token nameToken = token.getChild(TOKEN_NAME);
        String name = "Default";
        if (nameToken != null) {
            name = nameToken.getValue();
        }
        logger.debug("Enter  getOrCreateComponent name='" + name + "'");
        Token tok = token.getChild(TOKEN_CLASSNAME);
        if (tok != null) {
            logger.debug(name + " has class.  Call createComponent");
            ret = createComponent(token);
            if (ret != null && register) {
                components.put(name, ret);
            }
        } else {
            ret = getComponent(name);
        }
        if (logger.isDebugEnabled()) {
            logger.debug("Exit  getOrCreateComponent name='" + name + "' ret=" + (ret == null ? "null" : ret.getClass().getName()));
        }
        return ret;
    }

    public static IComponent getComponent(String name) {
        IComponent ret = (IComponent) components.get(name);
        return ret;
    }

    public static IComponent createComponent(Token token) throws ClassNotFoundException, InstantiationException, IllegalAccessException {
        IComponent ret = null;
        if (token != null) {
            try {
                Token tok = token.getChild(TOKEN_CLASSNAME);
                Class<?> cls = Class.forName(tok.getValue());
                ret = (IComponent) cls.newInstance();
                ret.configure(token);
            } catch (Throwable e) {
                if (logger != null) {
                    logger.error("Can't create the component from", e);
                } else {
                    System.err.println("Err=" + e);
                }
            }
        }
        return ret;
    }

    private static void evaluateConfig(Token top, Logger logger) throws ClassNotFoundException, InstantiationException, IllegalAccessException {
        if (top != null) {
            List<Token> kids = top.getChildren();
            if (kids != null) {
                for (int idx = 0, sz = kids.size(); idx < sz; idx++) {
                    Token tok = (Token) kids.get(idx);
                    String name = tok.getName();
                    if (name.equalsIgnoreCase("Monitor")) {
                        Monitor monitor = new Monitor(tok);
                        name = monitor.getName();
                        if (monitorMap.containsKey(name)) {
                            logger.error("Monitor named '" + name + "' already exists.");
                        } else {
                            logger.info("Creating Monitor " + monitor);
                            monitors.add(monitor);
                            monitorMap.put(name, monitor);
                        }
                    } else if (name.equalsIgnoreCase(TOKEN_IGNORE_PATTERN)) {
                        String tmp = tok.getValue();
                        if (tmp != null && tmp.length() > 0 && !tmp.equals("null")) {
                            globalIgnore = new Expression(tmp);
                        }
                        logger.info(TOKEN_IGNORE_PATTERN + " = " + tmp);
                    } else if (name.equalsIgnoreCase(TOKEN_SERVER_ID)) {
                        try {
                            serverId = Integer.parseInt(tok.getValue());
                        } catch (Exception ex) {
                            logger.error("Error setting serverId", ex);
                        }
                        logger.info("Setting serverId=" + serverId);
                    } else if (name.equalsIgnoreCase(TOKEN_SERVER_NAME)) {
                        serverName = tok.getValue();
                        logger.info("Setting serverName=" + serverName);
                    } else if (name.equalsIgnoreCase(TOKEN_INSTRUMENTOR)) {
                        instrumentorConfig = tok;
                    } else if (name.equalsIgnoreCase(TOKEN_APP_NAME)) {
                        appName = tok.getValue();
                        logger.info("Setting appName=" + appName);
                    } else if (name.equalsIgnoreCase("Reporter")) {
                        IReporter rep = (IReporter) getOrCreateComponent(tok, true);
                        rep.start();
                    } else if (name.equalsIgnoreCase(TOKEN_DEFAULT_PRIORITY)) {
                        try {
                            BaseThread.setDefaultPriority(Integer.parseInt(tok.getValue()));
                        } catch (Exception ex) {
                            logger.error("Error setting " + TOKEN_DEFAULT_PRIORITY, ex);
                        }
                    } else if (name.equalsIgnoreCase("Logger")) {
                        Logger.configLogger(tok);
                    } else if (name.equalsIgnoreCase(TOKEN_ADMINISTRATION)) {
                        AdminServer.configureAdmin(tok);
                    }
                }
            }
        }
    }

    private static Token getInitConfig(Logger logger) {
        Token ret = null;
        try {
            String fileName = Monitor.getProperty(PROP_CONFIG_FILE_NAME, DEFAULT_CONFIG_FILE_NAME);
            logger.info("Reading inititial configuration from " + fileName);
            Monitor cl = new Monitor();
            Class<? extends Monitor> cls = cl.getClass();
            ClassLoader loader = cls.getClassLoader();
            if (loader == null) {
                loader = ClassLoader.getSystemClassLoader();
            }
            File sourceFile = null;
            File libraryDir = null;
            String tmp = Monitor.getProperty(PROP_LIBRARY_DIR);
            if (tmp != null) {
                libraryDir = new File(tmp).getCanonicalFile();
            }
            InputStream source = loader.getResourceAsStream(fileName);
            if (source == null) {
                try {
                    sourceFile = new File(fileName).getCanonicalFile();
                    if (sourceFile.exists()) {
                        source = new FileInputStream(sourceFile);
                        if (libraryDir == null) {
                            libraryDir = sourceFile.getParentFile();
                        }
                    }
                } catch (IOException e) {
                    logger.error("Error opening initial property file filename=" + fileName, e);
                }
            }
            if (source == null) {
                logger.error("Configuration file(" + fileName + ") does not exists in the current class path.");
            } else {
                try {
                    BufferedReader in = new BufferedReader(new InputStreamReader(source));
                    Parser parser = new Parser(in);
                    if (libraryDir != null) {
                        logger.info(PROP_LIBRARY_DIR + " set to " + libraryDir.getAbsolutePath());
                        parser.setSourceDir(libraryDir);
                    } else {
                        logger.info(PROP_LIBRARY_DIR + " is not defined");
                    }
                    ret = parser.parse();
                } catch (Exception ex) {
                    logger.error("Can't start Monitor.  Error = ", ex);
                } finally {
                    if (source != null) {
                        try {
                            source.close();
                        } catch (Throwable e) {
                        }
                    }
                }
            }
            String tmpAppName = Monitor.getProperty(TOKEN_APP_NAME);
            if (tmpAppName == null) {
                tmpAppName = Monitor.getProperty(PROP_APP_NAME);
                if (tmpAppName == null) {
                    if (ret != null) {
                        Token child = ret.getChild(TOKEN_APP_NAME);
                        if (child != null) {
                            tmpAppName = child.getValue();
                        }
                    }
                }
            }
            if (tmpAppName == null) {
                tmpAppName = "UnDefinedApplication";
            }
            String urlString = Monitor.getProperty(TOKEN_CONFIG_URL);
            if (urlString == null) {
                if (ret != null) {
                    Token child = ret.getChild(TOKEN_CONFIG_URL);
                    if (child != null) {
                        urlString = child.getValue();
                    }
                }
            }
            if (urlString != null) {
                urlString = urlString + "?type=java&version=" + VERSION + "&" + TOKEN_APP_NAME + "=" + tmpAppName;
                ret = downloadRemoteConfig(ret, urlString, logger, libraryDir);
                properties = downloadRemoteProperties(urlString, logger);
            }
        } catch (Throwable e) {
            logger.error("Error in init config", e);
        }
        return ret;
    }

    private static Properties downloadRemoteProperties(String urlString, Logger logger) throws IOException {
        Properties ret = new Properties();
        String text = downloadString(new URL(urlString), logger);
        if (text != null) {
            String[] parts = text.split("\r\n");
            for (int idx = 0; idx < parts.length; idx++) {
                int pos = parts[idx].indexOf('=');
                if (pos > 0) {
                    String key = parts[idx].substring(0, pos);
                    String val = parts[idx].substring(pos + 1);
                    ret.setProperty(key, val);
                }
            }
        }
        return ret;
    }

    private static Token downloadRemoteConfig(Token local, String urlString, Logger logger, File libraryDir) throws IOException {
        Token ret = local;
        urlString = urlString + "&config=true";
        String text = downloadString(new URL(urlString), logger);
        if (text != null) {
            Parser parser = new Parser(text);
            parser.setSourceDir(libraryDir);
            ret = parser.parse();
        }
        return ret;
    }

    public static String downloadString(URL url, Logger logger) {
        String ret = null;
        URLConnection con = null;
        try {
            con = url.openConnection();
            Class<? extends URLConnection> cls = con.getClass();
            Object[] timeout = new Object[] { new Integer(6000) };
            Method mth = cls.getMethod("setConnectTimeout", new Class[] { int.class });
            if (mth != null) {
                mth.invoke(con, timeout);
            }
            mth = cls.getMethod("setReadTimeout", new Class[] { int.class });
            if (mth != null) {
                mth.invoke(con, timeout);
            }
            con.connect();
            InputStream in = con.getInputStream();
            if (con instanceof HttpURLConnection) {
                HttpURLConnection http = (HttpURLConnection) con;
                int code = http.getResponseCode();
                logger.debug("\tDownload response code = " + code);
                if (code == 200) {
                    int len = con.getContentLength();
                    logger.debug("Downloading content len= " + len);
                    if (len < 0) {
                        len = 1024 * 1024 * 5;
                    }
                    byte[] data = new byte[len];
                    int got = 0;
                    while (got < len) {
                        int gotNow = in.read(data, got, (len - got));
                        if (gotNow < 0) {
                            break;
                        } else {
                            got += gotNow;
                        }
                    }
                    ret = new String(data, 0, got);
                } else if (code == 304) {
                } else {
                    logger.debug("Response code " + code + " indicated a problem. Use local if possible.");
                }
            } else {
            }
        } catch (Throwable e) {
        }
        return ret;
    }

    private static void initSystemClasses() {
        systemClassMonitor = getMonitor("SystemClassMonitor");
        if (systemClassMonitor != null) {
            Class<Thread> c = java.lang.Thread.class;
            try {
                String nm = getCollectorName(c.getName());
                Field col = c.getDeclaredField(nm);
                if (col != null) {
                    Collector clt = (Collector) systemClassMonitor.getCollector();
                    col.set(null, clt);
                }
            } catch (Throwable e) {
            }
        }
    }

    public static String getCollectorName(String className) {
        String ret = getVariableName(className, IInstrumentor.VAR_COLLECTOR);
        return ret;
    }

    public static String getVariableName(String className, String variableName) {
        String ret = className.replace('/', '_').replace('.', '_') + "_" + variableName;
        return ret;
    }

    public static Monitor findMonitor(IClass cc) {
        Monitor ret = null;
        List<Monitor> monitors = getMonitors();
        for (int idx = 0, sz = monitors.size(); ret == null && idx < sz; idx++) {
            Monitor tmp = (Monitor) monitors.get(idx);
            if (tmp.matches(cc, 0)) {
                ret = tmp;
            }
        }
        return ret;
    }

    public boolean isWrap() {
        return wrap;
    }

    public void setWrap(boolean wrap) {
        this.wrap = wrap;
    }

    /**
	 * @param method
	 * @return true if the method should be instrumented.
	 */
    public boolean matches(IMethod method) {
        boolean ret = true;
        String mname = method.getName();
        if ((ret = matches(methodPatterns, mname))) {
            List<String> list = methodConstraints.get(mname);
            if (list != null && list.size() > 0) {
                String sig = method.getSignature();
                ret = false;
                for (int idx = 0, sz = list.size(); !ret && idx < sz; idx++) {
                    ret = list.get(idx).equals(sig);
                }
                logDebug("Evaluate Method Constraint sig=" + sig + " ret=" + ret);
            }
        }
        return ret;
    }

    private boolean matches(List<Expression> patterns, String val) {
        boolean ret = false;
        for (int idx = 0, sz = patterns.size(); !ret && idx < sz; idx++) {
            Expression exp = (Expression) patterns.get(idx);
            ret = exp.isMatch(val);
        }
        return ret;
    }

    public boolean matches(IClass cc, int level) {
        boolean ret = false;
        String className = cc.getName();
        boolean debug = isDebugEnabled();
        if (!(cc.isArray() || cc.isEnum() || cc.isPrimitive())) {
            if (!(ret = matches(classPatterns, className)) && isMatchInterface()) {
                IClass[] itf;
                try {
                    itf = cc.getInterfaces();
                    if (itf != null) {
                        for (int idx = 0; !ret && idx < itf.length; idx++) {
                            ret = matches(itf[idx], level + 1);
                        }
                    }
                } catch (Exception e) {
                    logWarn("Ignore: Error getting interfaces e=" + e);
                }
            }
        }
        if (!ret && (isMatchSuper() && level < getMaxSuperLevel())) {
            try {
                IClass sc = cc.getSuperclass();
                if (sc != null && !sc.getName().equals("java.lang.Object")) {
                    ret = matches(sc, level + 1);
                }
            } catch (Exception e) {
                logWarn("Ignore:  Error getting super Class e=" + e);
            }
        }
        if (ret && level == 0 && classConstraints.size() > 0) {
            for (int idx = 0, sz = classConstraints.size(); ret && idx < sz; idx++) {
                Expression exp = (Expression) classConstraints.get(idx);
                ret = !exp.isMatch(className);
                if (debug) {
                    logDebug("Evaluate ClassConstriant -------- idx=" + idx + " className=" + className + " pattern=" + exp + " ret=" + ret);
                }
            }
        }
        if (ret && level == 0 && cc.isAbstract()) {
            if (abstractPatterns.size() > 0) {
                if (debug) {
                    logDebug("\t\tAbstract Class(" + className + "). Shold we instrument?");
                }
                ret = matches(abstractPatterns, className);
            } else {
                if (debug) {
                    logDebug("\t\tAbstract Class(" + className + ") no " + TOKEN_ABSTRACT_PATTERN + " set so instrument it");
                }
            }
        }
        return ret;
    }

    public Monitor(Token token) {
        Token tmp = token.getChild(TOKEN_NAME);
        if (tmp != null) {
            setName(tmp.getValue());
        }
        if ((tmp = token.getChild(TOKEN_DESCRIPTION)) != null) {
            setDescription(tmp.getValue());
        }
        if ((tmp = token.getChild(TOKEN_IGNORE_PATTERN)) != null) {
            setTargetIgnore(tmp.getValue());
        }
        setPattern(token, classPatterns, TOKEN_CLASS_PATTERN);
        setPattern(token, methodPatterns, TOKEN_METHOD_PATTERN);
        setPattern(token, abstractPatterns, TOKEN_ABSTRACT_PATTERN);
        if ((tmp = token.getChild(TOKEN_REPORTER)) != null) {
            try {
                setReporter((IReporter) getOrCreateComponent(tmp, false));
                if (reporter == null) {
                    logError("Defined reporter is not availible. reporter=" + tmp);
                }
            } catch (Exception e) {
                logError("Can't create Reporter ", e);
            }
        } else {
            logError("No reporter defined for Monitor " + getName());
        }
        if ((tmp = token.getChild(TOKEN_COLLECTOR)) != null) {
            collectorConfig = tmp;
            if ((tmp = tmp.getChild(TOKEN_CLASSNAME)) != null) {
                setCollector(tmp.getValue());
            }
        }
        if ((tmp = token.getChild(TOKEN_MATCH_INTERFACE)) != null) {
            setMatchInterface(Boolean.valueOf(tmp.getValue()).booleanValue());
        }
        if ((tmp = token.getChild(TOKEN_MATCH_SUPER)) != null) {
            setMatchSuper(Boolean.valueOf(tmp.getValue()).booleanValue());
            if ((tmp = token.getChild(TOKEN_MAX_SUPER_LEVEL)) != null) {
                setMaxSuperLevel(Integer.parseInt(tmp.getValue()));
            }
        }
        if ((tmp = token.getChild(TOKEN_DEBUG)) != null) {
            setDebug(Boolean.valueOf(tmp.getValue()).booleanValue());
        }
        List<Token> kids = token.getChildren(TOKEN_CLASS_CONSTRAINT);
        if (kids != null) {
            for (int kidx = 0, sz = kids.size(); kidx < sz; kidx++) {
                tmp = (Token) kids.get(kidx);
                String val = tmp.getValue();
                if (val != null && val.length() > 0) {
                    classConstraints.add(new Expression(val));
                }
            }
        }
        kids = token.getChildren(TOKEN_METHOD_CONSTRAINT);
        if (kids != null) {
            for (int kidx = 0, sz = kids.size(); kidx < sz; kidx++) {
                tmp = (Token) kids.get(kidx);
                String val = tmp.getValue();
                if (val != null) {
                    String parts[] = val.split(",");
                    if (parts.length > 1) {
                        List<String> list = methodConstraints.get(parts[0]);
                        if (list == null) {
                            list = new ArrayList<String>();
                            methodConstraints.put(parts[0], list);
                        }
                        for (int idx = 1; idx < parts.length; idx++) {
                            list.add(parts[idx]);
                        }
                    }
                }
            }
        }
        if ((tmp = token.getChild(TOKEN_USE_WRAPPER)) != null) {
            String str = tmp.getValue();
            if (str != null) {
                wrap = str.toLowerCase().startsWith("t");
            }
            logInfo(TOKEN_USE_WRAPPER + " set to " + wrap);
        }
        if ((tmp = token.getChild(TOKEN_LIFE_EVENT)) != null) {
            String str = tmp.getValue();
            if (str != null && str.length() > 0) {
                try {
                    lifeEventPatterns.add(new Expression(str));
                    logInfo("Added " + TOKEN_LIFE_EVENT + " Expression as " + str);
                } catch (Exception e) {
                    logError("Can't add " + TOKEN_LIFE_EVENT + " Expression = '" + str + "'");
                }
            }
        }
        if ((tmp = token.getChild(TOKEN_DISABLE)) != null) {
            setEnabled(!(Boolean.valueOf(tmp.getValue()).booleanValue()));
        }
        logInfo("Enabled=" + isEnabled());
    }

    private void setPattern(Token token, List<Expression> target, String type) {
        List<Token> tok = token.getChildren(type);
        if (tok != null) {
            for (int idx = 0, sz = tok.size(); idx < sz; idx++) {
                Token t = (Token) tok.get(idx);
                String tmp = t.getValue();
                if (tmp != null) {
                    tmp = tmp.trim();
                    if (tmp.length() > 0) {
                        target.add(new Expression(tmp));
                    }
                }
            }
        }
    }

    public ICollector getCollector() {
        if (collector == null) {
            synchronized (this) {
                if (collector == null) {
                    collector = new Collector();
                    collector.setMonitor(this);
                    collector.setEnabled(this.enabled);
                }
            }
        }
        return collector;
    }

    public void setCollector(String value) {
        if (value != null && value.length() > 0) {
            try {
                Class<?> collectorClass = Class.forName(value);
                collector = (ICollector) collectorClass.newInstance();
                collector.setMonitor(this);
                collector.setEnabled(isEnabled());
                collector.configure(getCollectorConfig());
            } catch (Exception e) {
                logError("Can't set collector", e);
            }
        }
    }

    public Monitor() {
        super();
    }

    public Token getCollectorConfig() {
        return collectorConfig;
    }

    public Expression getIgnorePattern() {
        if (ignorePattern == null && targetIgnore != null) {
            synchronized (this) {
                if (ignorePattern == null) {
                    ignorePattern = new Expression(targetIgnore);
                }
            }
        }
        return ignorePattern;
    }

    public String getDescription() {
        return description;
    }

    public void setDescription(String description) {
        this.description = description;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String toString() {
        return "Monitor: " + getName() + " Class=" + getTargetClass() + " Method=" + getTargetMethod() + " Collector=" + getCollector().getClass().getName();
    }

    private String getTargetClass() {
        StringBuffer ret = new StringBuffer();
        for (int idx = 0, sz = classPatterns.size(); idx < sz; idx++) {
            if (idx > 0) {
                ret.append("|");
            }
            ret.append(classPatterns.get(idx).toString());
        }
        return ret.toString();
    }

    private String getTargetMethod() {
        StringBuffer ret = new StringBuffer();
        for (int idx = 0, sz = methodPatterns.size(); idx < sz; idx++) {
            if (idx > 0) {
                ret.append("|");
            }
            ret.append(methodPatterns.get(idx).toString());
        }
        return ret.toString();
    }

    public int hashCode() {
        return toString().hashCode();
    }

    public IReporter getReporter() {
        if (reporter == null) {
            synchronized (this) {
                if (reporter == null) {
                    if ((reporter = (IReporter) getComponent("LogReporter")) == null) {
                        reporter = new NullReporter();
                        logError("Can't find any reporter.  Using NullReporter instead.");
                    } else {
                        logError("No reporter set.  Using LogReporter instead.");
                    }
                }
            }
        }
        if (!reporter.isRunning()) {
            reporter.start();
        }
        return reporter;
    }

    public void setReporter(IReporter reporter) {
        this.reporter = reporter;
    }

    public List<Expression> getClassPatterns() {
        return classPatterns;
    }

    public void setClassPatterns(List<Expression> classPatterns) {
        this.classPatterns = classPatterns;
    }

    public List<Expression> getMethodPatterns() {
        return methodPatterns;
    }

    public void setMethodPatterns(List<Expression> methodPatterns) {
        this.methodPatterns = methodPatterns;
    }

    public List<Expression> getAbstractPatterns() {
        return abstractPatterns;
    }

    public void setAbstractPatterns(List<Expression> abstractPatterns) {
        this.abstractPatterns = abstractPatterns;
    }

    public void setIgnorePattern(Expression ignorePattern) {
        this.ignorePattern = ignorePattern;
    }

    public void setCollector(ICollector collector) {
        this.collector = collector;
    }

    public void setCollectorConfig(Token collectorConfig) {
        this.collectorConfig = collectorConfig;
    }

    public boolean isMatchInterface() {
        return matchInterface;
    }

    public void setMatchInterface(boolean matchInterface) {
        this.matchInterface = matchInterface;
    }

    public boolean isMatchSuper() {
        return matchSuper;
    }

    public void setMatchSuper(boolean matchSuper) {
        this.matchSuper = matchSuper;
    }

    public int getMaxSuperLevel() {
        return maxSuperLevel;
    }

    public void setMaxSuperLevel(int maxSuperLevel) {
        this.maxSuperLevel = maxSuperLevel;
    }

    public String getTargetIgnore() {
        return targetIgnore;
    }

    public void setTargetIgnore(String targetIgnore) {
        this.targetIgnore = targetIgnore;
    }

    public static String getAppNAmeFromSystemProperties() {
        String ret = Monitor.getProperty(PROP_APP_NAME);
        if (ret == null) {
            ret = Monitor.getProperty(TOKEN_APP_NAME);
        }
        return ret;
    }

    public static int getServerId() {
        if (serverId < 0) {
            String tmp = Monitor.getProperty(TOKEN_SERVER_ID);
            if (tmp != null && tmp.length() > 0) {
                try {
                    serverId = Integer.parseInt(tmp);
                } catch (Exception ex) {
                }
            }
            if (serverId < 0) {
                serverId = 0;
            }
        }
        return serverId;
    }

    public static void setServerId(int serverId) {
        Monitor.serverId = serverId;
    }

    public boolean isDebug() {
        return debug;
    }

    public void setDebug(boolean debug) {
        this.debug = debug;
    }

    public List<Expression> getLifeEventPatterns() {
        return lifeEventPatterns;
    }

    public void setLifeEventPatterns(List<Expression> lifeEventPatterns) {
        this.lifeEventPatterns = lifeEventPatterns;
    }

    public static boolean isWaitForGetPropertyPermission() {
        return waitForGetPropertyPermission;
    }

    public static void setWaitForGetPropertyPermission(boolean waitForGetPropertyPermission) {
        Monitor.waitForGetPropertyPermission = waitForGetPropertyPermission;
    }

    public Map<String, List<String>> getMethodConstraints() {
        return methodConstraints;
    }

    public void setMethodConstraints(Map<String, List<String>> methodConstraints) {
        this.methodConstraints = methodConstraints;
    }

    public List<Expression> getClassConstraints() {
        return classConstraints;
    }

    public void setClassConstraints(List<Expression> classConstraints) {
        this.classConstraints = classConstraints;
    }

    public void logDebug(String msg) {
        super.logDebug(getName() + ":" + msg);
    }

    public void logError(String msg, Throwable error) {
        super.logError(getName() + ":" + msg, error);
    }

    public void logError(String msg) {
        super.logError(getName() + ":" + msg);
    }

    public void logInfo(String msg) {
        super.logInfo(getName() + ":" + msg);
    }

    public void logWarn(String msg) {
        super.logWarn(getName() + ":" + msg);
    }

    private static void fixMissingCollector(Class<?> clazz, ICollector collector, Logger logger) {
        String colName = getCollectorName(clazz.getName());
        logger.debug("Looking for static collector field name='" + colName + "'");
        try {
            Field col = clazz.getDeclaredField(colName);
            if (col.get(null) == null) {
                col.set(null, collector);
            } else {
                logger.debug("colector " + colName + " was already set.");
            }
        } catch (Throwable e) {
            logger.error("Can't get / set " + colName, e);
            Field fieldlist[] = clazz.getDeclaredFields();
            for (int i = 0; i < fieldlist.length; i++) {
                Field fld = fieldlist[i];
                logger.error("name= " + fld.getName() + " declaring class = " + fld.getDeclaringClass() + " type= " + fld.getType() + " modifiers = " + Modifier.toString(fld.getModifiers()));
            }
        }
    }

    public static long genObjectId(Class<?> clazz, String name, ICollector collector) {
        long ret = 0;
        String cls = null;
        if (clazz == null) {
            Logger logger = Logger.getLogger(Monitor.class);
            logger.error("\n\n\n\nclazz is null in genObjectId.  Can't use it??? name=" + name + "\n\n\n");
            cls = "UnKNown";
        } else {
            cls = clazz.getName();
        }
        if (collector == null) {
            Logger logger = Logger.getLogger(Monitor.class);
            logger.error("Collector is null in genObjectId. " + "cls=" + cls + " name=" + name);
            if ((collector = getCollector(name)) == null) {
                logger.error("Can't find Collector for " + "cls=" + cls + " name=" + name + " Using error collector.");
                collector = getErrorCollector();
            }
            fixMissingCollector(clazz, collector, logger);
            ret = collector.getNextObjectId();
        } else {
            ret = collector.getNextObjectId();
        }
        return ret;
    }

    private static ICollector getErrorCollector() {
        if (errorCollector == null) {
            synchronized (Monitor.class) {
                if (errorCollector == null) {
                    errorCollector = new NullCollector();
                }
            }
        }
        return errorCollector;
    }

    /**
	 * 
	 * @param monitorName
	 * @return The collector for the specified Monitor
	 */
    public static ICollector getCollector(String name) {
        ICollector ret = null;
        Monitor monitor = getMonitor(name);
        if (monitor == null) {
            Logger logger = Logger.getLogger(Monitor.class);
            logger.error("Monitor " + name + " does not exists using errorColector instead.");
            ret = getErrorCollector();
        } else {
            if ((ret = monitor.getCollector()) == null) {
                Logger logger = Logger.getLogger(Monitor.class);
                logger.error("Monitor " + name + " does not have a collector using errorColector instead.");
                ret = getErrorCollector();
            }
        }
        return ret;
    }

    public boolean isLifeEvent(IClass cc) {
        boolean ret = false;
        if (lifeEventPatterns != null) {
            String className = cc.getName();
            for (int idx = 0, sz = lifeEventPatterns.size(); !ret && idx < sz; idx++) {
                Expression exp = (Expression) lifeEventPatterns.get(idx);
                ret = exp.isMatch(className);
            }
        }
        return ret;
    }

    public boolean isEnabled() {
        boolean ret = this.enabled;
        if (collector != null) {
            ret = collector.isEnabled();
        }
        return ret;
    }

    public void setEnabled(boolean b) {
        this.enabled = b;
        if (collector != null) {
            collector.setEnabled(b);
        }
    }

    public boolean isRequiresData(IClass cc) {
        boolean ret = false;
        if (collector != null) {
            ret = collector.isRequiresData();
        }
        return ret;
    }

    public static List<Monitor> getMonitors() {
        if (!inited) {
            synchronized (Monitor.class) {
                if (!inited) {
                    monitors = new ArrayList<Monitor>();
                    logger = Logger.getLogger(Monitor.class);
                    logger.info("Initilizing Version:" + VERSION);
                    Date date = new Date();
                    SimpleDateFormat fmt = new SimpleDateFormat("mm-dd-yyyy MM:hh:ss.SSS zzz");
                    logger.info("Time:" + fmt.format(date));
                    TimeZone tz = TimeZone.getDefault();
                    logger.info("TimeZone:" + tz.getID() + " offset=" + tz.getRawOffset());
                    try {
                        Token top = getInitConfig(logger);
                        evaluateConfig(top, logger);
                    } catch (Exception ex) {
                        logger.error("Can't start Monitor.  Error = ", ex);
                    } finally {
                        String tmp = Monitor.getProperty(PROP_APP_NAME);
                        if (tmp != null) {
                            appName = tmp;
                            logger.info("Setting appName from System.properties(" + PROP_APP_NAME + ") appName=" + appName);
                        } else {
                            tmp = Monitor.getProperty(TOKEN_APP_NAME);
                            if (tmp != null) {
                                appName = tmp;
                                logger.info("Setting appName from System.properties(" + TOKEN_APP_NAME + ") appName=" + appName);
                            }
                        }
                    }
                    initSystemClasses();
                    inited = true;
                }
            }
        }
        return monitors;
    }

    public static IInstrumentor getInstrumentor() {
        IInstrumentor ret = null;
        if (instrumentorConfig != null) {
            try {
                ret = (IInstrumentor) createComponent(instrumentorConfig);
            } catch (Throwable e) {
            }
        }
        return ret;
    }
}
