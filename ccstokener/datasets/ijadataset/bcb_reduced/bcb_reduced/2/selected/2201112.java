package org.mobicents.diameter.server.bootstrap;

import gnu.getopt.Getopt;
import gnu.getopt.LongOpt;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.net.URL;
import java.util.Properties;
import org.apache.log4j.Logger;
import org.apache.log4j.PropertyConfigurator;
import org.apache.log4j.xml.DOMConfigurator;
import org.jboss.dependency.spi.Controller;
import org.jboss.dependency.spi.ControllerContext;
import org.jboss.kernel.Kernel;
import org.jboss.kernel.plugins.bootstrap.basic.BasicBootstrap;
import org.jboss.kernel.plugins.deployment.xml.BasicXMLDeployer;
import org.jboss.util.StringPropertyReplacer;

/**
 * @author <a href="mailto:ales.justin@jboss.com">Ales Justin</a>
 * @author <a href="mailto:amit.bhayani@jboss.com">amit bhayani</a>
 * @author baranowb
 */
public class Main {

    private static final String HOME_DIR = "DIA_HOME";

    private static final String BOOT_URL = "/conf/bootstrap-beans.xml";

    private static final String LOG4J_URL = "/conf/log4j.properties";

    private static final String LOG4J_URL_XML = "/conf/log4j.xml";

    public static final String DIA_HOME = "dia.home.dir";

    public static final String DIA_BIND_ADDRESS = "dia.bind.address";

    private static int index = 0;

    private Kernel kernel;

    private BasicXMLDeployer kernelDeployer;

    private Controller controller;

    private static Logger logger = Logger.getLogger(Main.class);

    public static void main(String[] args) throws Throwable {
        String homeDir = getHomeDir(args);
        System.setProperty(DIA_HOME, homeDir);
        if (!initLOG4JProperties(homeDir) && !initLOG4JXml(homeDir)) {
            System.err.println("Failed to initialize loggin, no configuration. Defaults are used.");
        } else {
            logger.info("log4j configured");
        }
        URL bootURL = getBootURL(args);
        Main main = new Main();
        main.processCommandLine(args);
        logger.info("Booting from " + bootURL);
        main.boot(bootURL);
    }

    private void processCommandLine(String[] args) {
        String programName = System.getProperty("program.name", "Mobicents Diameter Test Server");
        int c;
        String arg;
        LongOpt[] longopts = { new LongOpt("help", LongOpt.NO_ARGUMENT, null, 'h'), new LongOpt("host", LongOpt.REQUIRED_ARGUMENT, null, 'b') };
        Getopt g = new Getopt("MMS", args, "-:b:h", longopts);
        g.setOpterr(false);
        while ((c = g.getopt()) != -1) {
            switch(c) {
                case 'b':
                    arg = g.getOptarg();
                    System.setProperty(DIA_BIND_ADDRESS, arg);
                    break;
                case 'h':
                    System.out.println("usage: " + programName + " [options]");
                    System.out.println();
                    System.out.println("options:");
                    System.out.println("    -h, --help                    Show this help message");
                    System.out.println("    -b, --host=<host or ip>       Bind address for all Mobicents Media Server services");
                    System.out.println();
                    System.exit(0);
                    break;
                case ':':
                    System.out.println("You need an argument for option " + (char) g.getOptopt());
                    System.exit(0);
                    break;
                case '?':
                    System.out.println("The option '" + (char) g.getOptopt() + "' is not valid");
                    System.exit(0);
                    break;
                default:
                    System.out.println("getopt() returned " + c);
                    break;
            }
        }
        if (System.getProperty(DIA_BIND_ADDRESS) == null) {
            System.setProperty(DIA_BIND_ADDRESS, "127.0.0.1");
        }
    }

    private static boolean initLOG4JProperties(String homeDir) {
        String Log4jURL = homeDir + LOG4J_URL;
        try {
            URL log4jurl = getURL(Log4jURL);
            InputStream inStreamLog4j = log4jurl.openStream();
            Properties propertiesLog4j = new Properties();
            try {
                propertiesLog4j.load(inStreamLog4j);
                PropertyConfigurator.configure(propertiesLog4j);
            } catch (IOException e) {
                e.printStackTrace();
            }
        } catch (Exception e) {
            logger.info("Failed to initialize LOG4J with properties file.");
            return false;
        }
        return true;
    }

    private static boolean initLOG4JXml(String homeDir) {
        String Log4jURL = homeDir + LOG4J_URL_XML;
        try {
            URL log4jurl = getURL(Log4jURL);
            DOMConfigurator.configure(log4jurl);
        } catch (Exception e) {
            logger.info("Failed to initialize LOG4J with xml file.");
            return false;
        }
        return true;
    }

    /**
     * Gets the Media Server Home directory.
     * 
     * @param args
     *            the command line arguments
     * @return the path to the home directory.
     */
    private static String getHomeDir(String args[]) {
        if (System.getenv(HOME_DIR) == null) {
            if (args.length > index) {
                return args[index++];
            } else {
                return ".";
            }
        } else {
            return System.getenv(HOME_DIR);
        }
    }

    /**
     * Gets the URL which points to the boot descriptor.
     * 
     * @param args
     *            command line arguments.
     * @return URL of the boot descriptor.
     */
    private static URL getBootURL(String args[]) throws Exception {
        String bootURL = "${" + DIA_HOME + "}" + BOOT_URL;
        return getURL(bootURL);
    }

    protected void boot(URL bootURL) throws Throwable {
        BasicBootstrap bootstrap = new BasicBootstrap();
        bootstrap.run();
        registerShutdownThread();
        kernel = bootstrap.getKernel();
        kernelDeployer = new BasicXMLDeployer(kernel);
        kernelDeployer.deploy(bootURL);
        kernelDeployer.validate();
        controller = kernel.getController();
        start(kernel, kernelDeployer);
    }

    public void start(Kernel kernel, BasicXMLDeployer kernelDeployer) {
        ControllerContext context = controller.getInstalledContext("MainDeployer");
        if (context != null) {
            MainDeployer deployer = (MainDeployer) context.getTarget();
            deployer.start(kernel, kernelDeployer);
        }
    }

    public static URL getURL(String url) throws Exception {
        url = StringPropertyReplacer.replaceProperties(url, System.getProperties());
        File file = new File(url);
        if (file.exists() == false) {
            throw new IllegalArgumentException("No such file: " + url);
        }
        return file.toURI().toURL();
    }

    protected void registerShutdownThread() {
        Runtime.getRuntime().addShutdownHook(new Thread(new ShutdownThread()));
    }

    private class ShutdownThread implements Runnable {

        public void run() {
            System.out.println("Shutting down");
            kernelDeployer.shutdown();
            kernelDeployer = null;
            kernel.getController().shutdown();
            kernel = null;
        }
    }
}
