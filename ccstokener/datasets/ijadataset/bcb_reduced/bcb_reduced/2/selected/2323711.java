package de.imedic.webrcp;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileFilter;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.io.Writer;
import java.lang.reflect.Method;
import java.net.ServerSocket;
import java.net.URL;
import java.net.URLClassLoader;
import java.net.URLConnection;
import java.security.Policy;
import javax.jnlp.BasicService;
import javax.jnlp.ServiceManager;
import javax.jnlp.UnavailableServiceException;
import javax.swing.JOptionPane;
import javax.swing.ProgressMonitor;

/**
 * WebRCP - Web Start Application which acts as loader for an Eclipse RCP
 * application.
 * 
 * @author by Daniel Mendler <mendler@imedic.de>
 */
public class WebRCP {

    private static final String ARCH_X86 = "x86";

    private static final String ARCH_PA_RISC = "PA_RISC";

    private static final String ARCH_PPC = "ppc";

    private static final String ARCH_SPARC = "sparc";

    private static final String ARCH_AMD64 = "amd64";

    private static final String ARCH_IA64 = "ia64";

    private static final String WS_WIN32 = "win32";

    private static final String WS_MOTIF = "motif";

    private static final String WS_GTK = "gtk";

    private static final String WS_PHOTON = "photon";

    private static final String WS_CARBON = "carbon";

    private static final String OS_WIN32 = "win32";

    private static final String OS_LINUX = "linux";

    private static final String OS_AIX = "aix";

    private static final String OS_SOLARIS = "solaris";

    private static final String OS_HPUX = "hpux";

    private static final String OS_QNX = "qnx";

    private static final String OS_MACOSX = "macosx";

    private static final String WEBRCP_APPNAME = "webrcp.appName";

    private static final String WEBRCP_APPVERSION = "webrcp.appVersion";

    private static final String WEBRCP_ARCHIVES = "webrcp.archives";

    private static final String WEBRCP_SYSARCHIVES = "webrcp.sysArchives";

    private static final String WEBRCP_SPLASH_SCREEN = "webrcp.splashScreen";

    private static final String WEBRCP_SINGLE_INSTANCE = "webrcp.singleInstance";

    private static final String WEBRCP_BASEURL = "webrcp.baseURL";

    private static final String ECLIPSE_START_TIME = "eclipse.startTime";

    private static final String ECLIPSE_EXITCODE = "eclipse.exitcode";

    private static final String ECLIPSE_EXIT_DATA = "eclipse.exitdata";

    private static final String ECLIPSE_VM = "eclipse.vm";

    private static final String OSGI_INSTALL_AREA = "osgi.install.area";

    private static final String OSGI_CONFIG_AREA = "osgi.configuration.area";

    private static final String OSGI_INSTANCE_AREA = "osgi.instance.area";

    private static final String OSGI_USER_AREA = "osgi.user.area";

    private static final String OSGI_FRAMEWORK = "osgi.framework";

    private static final String OSGI_ARCH = "osgi.arch";

    private static final String OSGI_OS = "osgi.os";

    private static final String OSGI_WS = "osgi.ws";

    private static final int EXITCODE_SUCCESS = 0;

    private static final int EXITCODE_ERROR = 13;

    private static final int EXITCODE_RESTART = 23;

    private static final int EXITCODE_RESTART_ARGS = 24;

    private static final String OSGI_USER_AREA_VALUE = "@user.home";

    private static final String OSGI_INSTANCE_AREA_VALUE = "@user.home/workspace";

    private static final String FRAMEWORK = "org.eclipse.osgi";

    private static final String STARTER_CLASS = "org.eclipse.core.runtime.adaptor.EclipseStarter";

    private static final String CONFIG_DIR = "/configuration/";

    private static final String PLUGINS_DIR = "/plugins/";

    private static final String INSTALL_DIR = "install";

    private static final String VERSION_FILE = "local.version";

    private static final String ARCHIVE_EXT = ".zip";

    private static final int SINGLEINST_PORT = 25975;

    private final String appName = getRequiredProperty(WEBRCP_APPNAME);

    private final String appVersion = getRequiredProperty(WEBRCP_APPVERSION);

    private final String archives = getRequiredProperty(WEBRCP_ARCHIVES);

    private final String baseURL = getBaseURL();

    private final String arch = determineArch();

    private final String os = determineOS();

    private File installDir, tempDir;

    private File versionFile;

    private UnpackThread unpackThread;

    private boolean override;

    private SplashScreen splashScreen;

    private ServerSocket singleInstSocket;

    private final Runnable hideSplashHandler = new Runnable() {

        public void run() {
            hideSplash();
        }
    };

    private void run(String[] userArgs) {
        if (Boolean.getBoolean(WEBRCP_SINGLE_INSTANCE)) ensureSingleInstance();
        System.setProperty(ECLIPSE_START_TIME, Long.toString(System.currentTimeMillis()));
        tempDir = new File(System.getProperty("java.io.tmpdir"), appName + '-' + System.getProperty("user.name"));
        System.out.println("Creating temporary directory: " + tempDir);
        tempDir.mkdir();
        if (!tempDir.exists() || !tempDir.isDirectory()) fatalError("Temporary Directory", "Temporary directory " + tempDir + " could not be created!");
        installDir = new File(tempDir, INSTALL_DIR);
        System.out.println("Creating installation directory: " + installDir);
        installDir.mkdir();
        if (!installDir.exists() || !installDir.isDirectory()) fatalError("Installation Directory", "Installation directory " + tempDir + " could not be created!");
        versionFile = new File(tempDir, VERSION_FILE);
        String localVersion = readLocalVersion();
        override = (localVersion == null || !appVersion.equals(localVersion));
        System.out.println("New version available: " + override);
        unpackThread = new UnpackThread(installDir, override);
        System.out.println("Downloading archives...");
        processArchives();
        processSysArchives();
        if (override) writeLocalVersion(appVersion);
        showSplash();
        unpackThread.finish();
        launchEclipse(userArgs);
    }

    private void ensureSingleInstance() {
        try {
            singleInstSocket = new ServerSocket(SINGLEINST_PORT);
        } catch (Exception ex) {
            ex.printStackTrace();
            fatalError("Application already running", "There is already an instance of the application running.");
        }
    }

    private void processArchives() {
        String[] archive = archives.split("\\s*,\\s*");
        for (int i = 0; i < archive.length; ++i) {
            File destFile = new File(tempDir, archive[i] + ARCHIVE_EXT);
            if (!destFile.exists() || override) downloadFile(baseURL + archive[i] + ARCHIVE_EXT, destFile);
            unpackThread.addNextFile(destFile);
        }
    }

    private void processSysArchives() {
        String archives = System.getProperty(WEBRCP_SYSARCHIVES);
        if (archives == null) return;
        String[] archive = archives.split("\\s*,\\s*");
        String postfix = '/' + os + '-' + arch + ARCHIVE_EXT;
        for (int i = 0; i < archive.length; ++i) {
            File destFile = new File(tempDir, archive[i] + ARCHIVE_EXT);
            if (!destFile.exists() || override) downloadFile(baseURL + archive[i] + postfix, destFile);
            unpackThread.addNextFile(destFile);
        }
    }

    private void showSplash() {
        String imageName = System.getProperty(WEBRCP_SPLASH_SCREEN);
        if (imageName == null) return;
        File destFile = new File(tempDir, imageName);
        if (!destFile.exists() || override) downloadFile(baseURL + imageName, destFile);
        splashScreen = new SplashScreen(destFile);
    }

    private void hideSplash() {
        if (splashScreen != null) splashScreen.dispose();
    }

    private String determineArch() {
        String arch = System.getProperty("os.arch").toLowerCase();
        if (arch.indexOf("x86") >= 0 || arch.matches("i.86")) return ARCH_X86;
        if (arch.indexOf("ppc") >= 0 || arch.indexOf("power") >= 0) return ARCH_PPC;
        if (arch.indexOf("x86_64") >= 0 || arch.indexOf("amd64") >= 0) return ARCH_AMD64;
        if (arch.indexOf("ia64") >= 0) return ARCH_IA64;
        if (arch.indexOf("risc") >= 0) return ARCH_PA_RISC;
        if (arch.indexOf("sparc") >= 0) return ARCH_SPARC;
        fatalError("Unknown Architecture", "Your system has an unknown architecture: " + arch);
        return null;
    }

    private String determineOS() {
        String os = System.getProperty("os.name").toLowerCase();
        if (os.indexOf("linux") >= 0) return OS_LINUX;
        if (os.indexOf("mac") >= 0) return OS_MACOSX;
        if (os.indexOf("windows") >= 0) return OS_WIN32;
        if (os.indexOf("hp") >= 0 && os.indexOf("ux") >= 0) return OS_HPUX;
        if (os.indexOf("solaris") >= 0) return OS_SOLARIS;
        if (os.indexOf("aix") >= 0) return OS_AIX;
        if (os.indexOf("qnx") >= 0) return OS_QNX;
        fatalError("Unknown Operating System", "Your operating system is unknown: " + os);
        return null;
    }

    private String getWindowSystem() {
        if (os.equals(OS_WIN32)) return WS_WIN32;
        if (os.equals(OS_LINUX)) return WS_GTK;
        if (os.equals(OS_QNX)) return WS_PHOTON;
        if (os.equals(OS_MACOSX)) return WS_CARBON;
        return WS_MOTIF;
    }

    private String getBaseURL() {
        try {
            BasicService service = (BasicService) ServiceManager.lookup("javax.jnlp.BasicService");
            String baseURL = service.getCodeBase().toString();
            System.setProperty(WEBRCP_BASEURL, baseURL);
            return baseURL;
        } catch (UnavailableServiceException ex) {
            ex.printStackTrace();
            fatalError("WebStart Service Error", "Service javax.jnlp.BasicService unvailable: " + ex);
            return null;
        }
    }

    private URL[] getBootPath() throws IOException {
        File pluginDir = new File(installDir, PLUGINS_DIR);
        File[] file = pluginDir.listFiles(new FileFilter() {

            public boolean accept(File file) {
                return (file.getName().equals(FRAMEWORK) || file.getName().startsWith(FRAMEWORK + "_"));
            }
        });
        if (file == null || file.length <= 0) {
            System.err.println("No boot framework found!");
            fatalError("Boot framework not found", "Boot framework " + FRAMEWORK + " could not be found.");
            return null;
        }
        File framework = null;
        String maxVersion = null;
        for (int i = 0; i < file.length; ++i) {
            String name = file[i].getName();
            int index = name.indexOf('_');
            String version = null;
            if (index >= 0) version = name.substring(index + 1);
            if (maxVersion == null || ((Comparable) maxVersion).compareTo(version) < 0) {
                framework = file[i];
                maxVersion = version;
            }
        }
        System.out.println("Framework: " + framework);
        System.setProperty(OSGI_FRAMEWORK, framework.toURL().toString());
        if (framework.isDirectory()) {
            File[] jar = framework.listFiles(new FileFilter() {

                public boolean accept(File file) {
                    return (file.isFile() && file.getName().endsWith(".jar"));
                }
            });
            URL[] bootPath = new URL[jar.length];
            for (int i = 0; i < jar.length; ++i) {
                bootPath[i] = jar[i].toURL();
                System.out.println(bootPath[i] + " added to boot path");
            }
            return bootPath;
        }
        return new URL[] { framework.toURL() };
    }

    private String readLocalVersion() {
        String localVersion = null;
        try {
            BufferedReader in = new BufferedReader(new FileReader(versionFile));
            localVersion = in.readLine();
            in.close();
        } catch (FileNotFoundException ex) {
        } catch (IOException ex) {
            ex.printStackTrace();
        }
        return localVersion;
    }

    private void writeLocalVersion(String newVersion) {
        try {
            Writer out = new FileWriter(versionFile);
            out.write(newVersion);
            out.close();
        } catch (IOException ex) {
            ex.printStackTrace();
        }
    }

    private void downloadFile(String url, File destFile) {
        try {
            System.out.println("Downloading " + url + " to " + destFile + "...");
            destFile.getParentFile().mkdirs();
            OutputStream out = new FileOutputStream(destFile);
            URLConnection conn = new URL(url).openConnection();
            InputStream in = conn.getInputStream();
            int totalSize = conn.getContentLength(), downloadedSize = 0, size;
            byte[] buffer = new byte[32768];
            ProgressMonitor pm = new ProgressMonitor(null, "Downloading " + url, "", 0, totalSize);
            pm.setMillisToDecideToPopup(100);
            pm.setMillisToPopup(500);
            boolean canceled = false;
            while ((size = in.read(buffer)) > 0 && !(canceled = pm.isCanceled())) {
                out.write(buffer, 0, size);
                pm.setProgress(downloadedSize += size);
                pm.setNote((100 * downloadedSize / totalSize) + "% finished");
            }
            in.close();
            out.close();
            if (canceled) {
                destFile.delete();
                fatalError("Starting canceled", "Downloading canceled. Exiting...");
            }
            pm.close();
        } catch (IOException ex) {
            ex.printStackTrace();
            destFile.delete();
            fatalError("Download Error", "Couldn't download file " + url + ": " + ex);
        }
    }

    private void launchEclipse(String[] userArgs) {
        try {
            Policy.setPolicy(new AllPermissionPolicy());
            URLClassLoader classLoader = new URLClassLoader(getBootPath());
            Class starterClass = classLoader.loadClass(STARTER_CLASS);
            Method starterMethod = starterClass.getDeclaredMethod("run", new Class[] { String[].class, Runnable.class });
            String installArea = installDir.toURL().toString();
            System.setProperty(ECLIPSE_VM, "");
            System.setProperty(OSGI_INSTALL_AREA, installArea);
            System.setProperty(OSGI_CONFIG_AREA, installArea + CONFIG_DIR);
            System.setProperty(OSGI_INSTANCE_AREA, OSGI_INSTANCE_AREA_VALUE);
            System.setProperty(OSGI_USER_AREA, OSGI_USER_AREA_VALUE);
            System.setProperty(OSGI_OS, os);
            System.setProperty(OSGI_ARCH, arch);
            System.setProperty(OSGI_WS, getWindowSystem());
            String[] args = userArgs;
            for (; ; ) {
                System.out.println("Invoking starter...");
                starterMethod.invoke(starterClass, new Object[] { args, hideSplashHandler });
                String exitCodeStr = System.getProperty(ECLIPSE_EXITCODE);
                int exitCode = exitCodeStr != null ? Integer.parseInt(exitCodeStr) : -1;
                switch(exitCode) {
                    case EXITCODE_RESTART_ARGS:
                        String newCmdline = System.getProperty(ECLIPSE_EXIT_DATA);
                        if (newCmdline != null) args = newCmdline.split("\\s+");
                        System.out.println("Restarting Eclipse with new command-line: " + newCmdline);
                        continue;
                    case EXITCODE_RESTART:
                        System.out.println("Restarting Eclipse...");
                        continue;
                    case EXITCODE_SUCCESS:
                        System.out.println("Eclipse successfully exited...");
                        return;
                    default:
                        System.err.println("Eclipse exited with error: " + exitCode);
                        hideSplash();
                        fatalError("Startup Error", "Eclipse starter returned exit code: " + exitCode + "\nMore informations are in the log file");
                        return;
                }
            }
        } catch (Exception ex) {
            ex.printStackTrace();
            hideSplash();
            fatalError("Startup Error", "Could not launch eclipse: " + ex);
        }
    }

    private void fatalError(String title, String message) {
        JOptionPane.showMessageDialog(null, message, title, JOptionPane.ERROR_MESSAGE);
        System.exit(1);
    }

    private String getRequiredProperty(String key) {
        String value = System.getProperty(key);
        if (value != null) return value;
        fatalError("Missing System Property", key + " is required");
        return null;
    }

    public static void main(String[] args) {
        new WebRCP().run(args);
        System.exit(0);
    }
}
