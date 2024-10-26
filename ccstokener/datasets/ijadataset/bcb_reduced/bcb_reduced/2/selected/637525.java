package org.wayuus.wajas;

import static org.wayuus.wajas.env.ServerProperties.APP_GROUP_PROPERTY_KEY;
import static org.wayuus.wajas.env.ServerProperties.APP_INSTANCE_PROPERTY_KEY;
import static org.wayuus.wajas.env.ServerProperties.APP_NAME_PROPERTY_KEY;
import static org.wayuus.wajas.env.ServerProperties.APP_VERSION_PROPERTY_KEY;
import java.io.FileNotFoundException;
import java.net.URL;
import java.text.MessageFormat;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Properties;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.support.AbstractApplicationContext;
import org.springframework.context.support.ClassPathXmlApplicationContext;
import org.springframework.util.ResourceUtils;
import org.springframework.util.SystemPropertyUtils;
import org.wayuus.wajas.batch.BatchProcessor;
import org.wayuus.wajas.config.WajasServerHandler;
import org.wayuus.wajas.config.WajasServerHandlerSupport;
import org.wayuus.wajas.env.EnvironmentType;
import org.wayuus.wajas.env.ServerLocation;
import org.wayuus.wajas.env.ServerProperties;
import org.wayuus.wajas.utils.DirectoriesCreator;

/**
 * @author St�phane Cusin
 */
public class WajasServer {

    public static final String SEPARATOR = "------------------------------------------------------------------------";

    public static final String WAJAS_SERVER_HANDLER_PROPERTY_KEY = "wajasServerHandler.class";

    public static final String WAJAS_SERVER_HANDLER_FILE = "classpath:META-INF/wajasServer.handler";

    public static final String PARAMETER_PREFIX = "-D";

    protected final Logger loggerTrace = LoggerFactory.getLogger(getClass());

    private String appName;

    private String appGroup;

    private String appInstance;

    private String appTask;

    private DirectoriesCreator directoriesCreator;

    private String baseDir;

    private String logsDir;

    private String dataDir;

    private String hostname;

    private EnvironmentType envType;

    private ServerLocation serverLocation;

    private WajasServerHandler handler;

    private List<String> applicationList = new ArrayList<String>();

    private AbstractApplicationContext applicationContext;

    public WajasServer() {
        directoriesCreator = new DirectoriesCreator();
    }

    public WajasServer(WajasServerHandler handler, DirectoriesCreator directoriesCreator) {
        this.handler = handler;
        this.directoriesCreator = directoriesCreator;
    }

    /**
	 * this method can be called from other classes
	 */
    public void launch(String argv[]) throws Exception {
        if (argv.length == 0) {
            System.out.println("At least one configuration file as parameter is needed!");
            return;
        }
        initHandler();
        for (String location : handler.getConfigLocations()) {
            applicationList.add(location);
        }
        for (String arg : argv) {
            if (arg.startsWith(PARAMETER_PREFIX)) {
                addParameter(arg);
            } else if (initAppName(arg)) {
                addApplicationContext();
            }
        }
        initEnvProperties();
        directoriesCreator.setPaths(new String[] { logsDir, dataDir });
        directoriesCreator.mkDirs();
        handler.getLoggingConfig().init(envType);
        logInfo();
        applicationContext = new ClassPathXmlApplicationContext(applicationList.toArray(new String[0]));
        applicationContext.registerShutdownHook();
        applicationContext.start();
        startBatchProcessor();
    }

    @SuppressWarnings("unchecked")
    private void startBatchProcessor() {
        Map<String, BatchProcessor> batchProcessorMap = applicationContext.getBeansOfType(BatchProcessor.class);
        for (BatchProcessor batchProcessor : batchProcessorMap.values()) {
            batchProcessor.process();
        }
    }

    private boolean initAppName(String arg) {
        if (appName == null) {
            appName = arg.replace('/', '-');
            String[] s = appName.split("-");
            if (s.length >= 1) {
                appGroup = s[0];
            }
            if (s.length >= 2) {
                appInstance = s[1];
            } else {
                appInstance = appGroup;
            }
            if (s.length >= 3) {
                appTask = s[2];
            }
            return true;
        }
        return false;
    }

    private void logInfo() {
        loggerTrace.info(SEPARATOR);
        loggerTrace.info("Wajas Server v" + Version.getImplementationVersion());
        loggerTrace.info("Application: " + appName + " v" + handler.getVersion());
        loggerTrace.info(SEPARATOR);
        loggerTrace.info(MessageFormat.format("{0}={1} {2}={3} {4}={5}", APP_NAME_PROPERTY_KEY, appName, APP_GROUP_PROPERTY_KEY, appGroup, APP_INSTANCE_PROPERTY_KEY, appInstance));
        loggerTrace.info(MessageFormat.format("{0}={1} {2}={3} {4}={5}", ServerProperties.HOSTNAME_PROPERTY_KEY, hostname, ServerProperties.ENV_TYPE_PROPERTY_KEY, envType.getType(), ServerProperties.SERVER_LOCATION_PROPERTY_KEY, serverLocation));
        loggerTrace.info(MessageFormat.format("{0}={1}", ServerProperties.BASEDIR_PROPERTY_KEY, baseDir));
        loggerTrace.info(MessageFormat.format("{0}={1}", ServerProperties.LOGS_DIR_PROPERTY_KEY, logsDir));
        loggerTrace.info(MessageFormat.format("{0}={1}", ServerProperties.DATA_DIR_PROPERTY_KEY, dataDir));
        loggerTrace.info(SEPARATOR);
    }

    private void initEnvProperties() {
        System.setProperty(APP_NAME_PROPERTY_KEY, appName);
        System.setProperty(APP_GROUP_PROPERTY_KEY, appGroup);
        System.setProperty(APP_INSTANCE_PROPERTY_KEY, appInstance);
        System.setProperty(APP_VERSION_PROPERTY_KEY, handler.getVersion());
        hostname = ServerProperties.getHostName();
        envType = ServerProperties.getEnvironmentType();
        serverLocation = ServerProperties.getServerLocation();
        baseDir = ServerProperties.getBaseDir();
        logsDir = handler.getLogsDir();
        logsDir = replaceParams(logsDir, System.getProperties());
        System.setProperty(ServerProperties.LOGS_DIR_PROPERTY_KEY, logsDir);
        dataDir = handler.getDataDir();
        dataDir = replaceParams(dataDir, System.getProperties());
        System.setProperty(ServerProperties.DATA_DIR_PROPERTY_KEY, dataDir);
        for (Map.Entry<String, String> entry : handler.getEnvironmentProperties().entrySet()) {
            String value = replaceParams(entry.getValue(), System.getProperties());
            System.setProperty(entry.getKey(), value);
        }
    }

    private String replaceParams(String command, Properties params) {
        StringBuffer newCommand = new StringBuffer();
        Pattern pattern = Pattern.compile("\\$\\{([^}:]+):?([^}]*)\\}");
        Matcher m = pattern.matcher(command);
        while (m.find()) {
            String key = m.group(1);
            String value = params.getProperty(key);
            if (value != null) m.appendReplacement(newCommand, params.getProperty(m.group(1)));
        }
        m.appendTail(newCommand);
        return newCommand.toString();
    }

    private void addApplicationContext() {
        String fileName = "application/" + appGroup + "/applicationContext-" + appInstance;
        if (appTask != null) fileName += "-" + appTask;
        fileName += ".xml";
        applicationList.add(fileName);
    }

    private void addParameter(String arg) {
        try {
            String[] s = arg.substring(2).split("=");
            System.setProperty(s[0], s[1]);
        } catch (Exception e) {
            System.out.println("Invalid parameter: " + arg);
        }
    }

    private void initHandler() {
        if (handler == null) {
            try {
                String resolvedLocation = SystemPropertyUtils.resolvePlaceholders(WAJAS_SERVER_HANDLER_FILE);
                URL url = ResourceUtils.getURL(resolvedLocation);
                Properties props = new Properties();
                props.load(url.openStream());
                String clazzName = props.getProperty(WAJAS_SERVER_HANDLER_PROPERTY_KEY);
                if (clazzName != null) {
                    handler = (WajasServerHandler) Class.forName(clazzName).newInstance();
                }
            } catch (FileNotFoundException fnfe) {
            } catch (Exception e) {
                System.out.println("Load wajasServerHandler error.");
                e.printStackTrace();
            }
            if (handler == null) handler = new WajasServerHandlerSupport();
        }
    }

    /**
	 * @see java.lang.Object#toString()
	 */
    @Override
    public String toString() {
        return "Wajas Server " + Version.getImplementationVersion();
    }

    /**
	 * @param args
	 */
    public static void main(String[] args) {
        if (args.length <= 0) {
            return;
        }
        try {
            new WajasServer().launch(args);
        } catch (Exception e) {
            System.err.print("Launch error: ");
            e.printStackTrace();
            System.exit(-1);
        }
    }

    /**
	 * @return the appName
	 */
    public String getAppName() {
        return appName;
    }

    /**
	 * @return the appGroup
	 */
    public String getAppGroup() {
        return appGroup;
    }

    public String getAppInstance() {
        return appInstance;
    }

    public String getAppVersion() {
        return handler.getVersion();
    }

    /**
	 * @return the logsDir
	 */
    public String getLogsDir() {
        return logsDir;
    }

    /**
	 * @return the dataDir
	 */
    public String getDataDir() {
        return dataDir;
    }

    /**
	 * @return the envType
	 */
    public EnvironmentType getEnvType() {
        return envType;
    }

    /**
	 * @return the serverLocation
	 */
    public ServerLocation getServerLocation() {
        return serverLocation;
    }

    public WajasServerHandler getWajasServerHandler() {
        return handler;
    }

    public AbstractApplicationContext getApplicationContext() {
        return applicationContext;
    }
}
