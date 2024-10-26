package org.coos.messaging;

import org.coos.messaging.COContainer;
import org.coos.messaging.ConnectingException;
import org.coos.messaging.Plugin;
import org.coos.messaging.util.Log;
import org.coos.messaging.util.LogFactory;
import org.coos.util.macro.MacroSubstituteReader;
import java.io.ByteArrayInputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.net.URISyntaxException;
import java.net.URL;
import java.util.ArrayList;
import java.util.Enumeration;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Properties;
import java.util.Set;
import java.util.StringTokenizer;
import java.util.jar.JarEntry;
import java.util.jar.JarFile;

public class COOSBootstrapHelper {

    private static final String COOS_CONFIG_FILE = "/coos.xml";

    private static final String CONFIG_FILE_PATTERN = "plugin.*\\.xml";

    private static final String COOS_CONFIG_PATH = "/org/coos/config";

    private static final String COOS_CONFIG_PATH_WTH_SLHS = "org/coos/config";

    private static final Log logger = LogFactory.getLog(COOSBootstrapHelper.class);

    private static List<URL> pluginConfigFiles;

    public static COOS startCOOS(String configDir, COContainer container) throws Exception {
        logger.info("Coos starting");
        InputStream is = null;
        try {
            is = new FileInputStream(configDir + COOS_CONFIG_FILE);
        } catch (Exception e1) {
        }
        if (is != null) {
            logger.info("Using provided COOS config");
        } else {
            is = COOSBootstrapHelper.class.getResourceAsStream(COOS_CONFIG_PATH + COOS_CONFIG_FILE);
            logger.info("Using COOS included default configuration");
        }
        is = substitute(is);
        COOS coos = COOSFactory.createCOOS(is, container);
        coos.start();
        return coos;
    }

    public static Plugin[] startPlugins(String configDir, COContainer container) throws Exception {
        InputStream is = null;
        pluginConfigFiles = new ArrayList<URL>();
        String[] files = getResources();
        for (int i = 0; i < files.length; i++) {
            if (files[i].matches(CONFIG_FILE_PATTERN)) {
                addPluginConfigFiles(COOSBootstrapHelper.class.getResource(COOS_CONFIG_PATH + "/" + files[i]));
            }
        }
        File dir = new File(configDir);
        if (dir.isDirectory()) {
            File[] fileList = dir.listFiles();
            for (int i = 0; i < fileList.length; i++) {
                if (fileList[i].getName().matches(CONFIG_FILE_PATTERN)) {
                    addPluginConfigFiles(fileList[i].toURI().toURL());
                }
            }
        }
        if (!pluginConfigFiles.isEmpty()) {
            StringBuffer buf = new StringBuffer();
            for (URL conf : pluginConfigFiles) {
                buf.append(conf);
                buf.append("\n");
            }
            logger.info("Starting COOS plugins defined in: \n" + buf.toString());
        }
        Plugin[] plugins = null;
        ArrayList<Plugin> pluginArray = new ArrayList<Plugin>();
        for (Iterator<URL> iterator = pluginConfigFiles.iterator(); iterator.hasNext(); ) {
            URL url = iterator.next();
            is = url.openStream();
            is = substitute(is);
            plugins = PluginFactory.createPlugins(is, container);
            for (int i = 0; i < plugins.length; i++) {
                pluginArray.add(plugins[i]);
            }
        }
        plugins = pluginArray.toArray(new Plugin[0]);
        Plugin[] sortedPlugins = new Plugin[plugins.length];
        int lowest = Integer.MAX_VALUE;
        int lowestIdx = 0;
        for (int i = 0; i < sortedPlugins.length; i++) {
            for (int j = 0; j < plugins.length; j++) {
                if ((plugins[j] != null) && (plugins[j].getStartLevel() < lowest)) {
                    lowest = plugins[j].getStartLevel();
                    lowestIdx = j;
                }
            }
            sortedPlugins[i] = plugins[lowestIdx];
            plugins[lowestIdx] = null;
            lowest = Integer.MAX_VALUE;
        }
        plugins = sortedPlugins;
        for (int i = 0; i < plugins.length; i++) {
            Plugin plugin = plugins[i];
            try {
                plugin.connect();
            } catch (ConnectingException e) {
                logger.error("ConnectingException ignored", e);
            }
        }
        return plugins;
    }

    private static void addPluginConfigFiles(URL url) {
        for (URL savedUrl : pluginConfigFiles) {
            String urlPath = url.getPath();
            String urlFileName = urlPath.substring(urlPath.lastIndexOf('/') + 1);
            String savedUrlPath = savedUrl.getPath();
            String savedUrlFileName = savedUrlPath.substring(savedUrlPath.lastIndexOf('/') + 1);
            if (urlFileName.equals(savedUrlFileName)) {
                logger.debug("Overwriting config file: " + savedUrl + " --> " + url);
                pluginConfigFiles.remove(savedUrl);
                pluginConfigFiles.add(url);
                return;
            }
        }
        logger.debug("Adding config file: " + url);
        pluginConfigFiles.add(url);
    }

    private static String[] getResources() throws IOException, URISyntaxException {
        Set<String> result = new HashSet<String>();
        Properties prop = System.getProperties();
        String classpath = prop.getProperty("java.class.path");
        logger.debug("java class path: " + classpath);
        String sfclasspath = prop.getProperty("surefire.test.class.path");
        logger.debug("surefire class path: " + sfclasspath);
        if (sfclasspath != null) {
            classpath = sfclasspath;
        }
        String delim = ":";
        if (prop.getProperty("os.name").contains("Windows")) {
            delim = ";";
        }
        StringTokenizer tokenizer = new StringTokenizer(classpath, delim);
        while (tokenizer.hasMoreElements()) {
            String fileStr = tokenizer.nextToken();
            File dir = new File(fileStr + COOS_CONFIG_PATH);
            if (dir.isDirectory()) {
                if (dir.getAbsolutePath().replace('\\', '/').matches(".*/classes.*")) {
                    logger.debug("Looking for plugin config files in dir: " + dir);
                    String[] resources = dir.list();
                    for (String res : resources) {
                        logger.debug("Found: " + res);
                        result.add(res);
                    }
                }
            } else if (fileStr.trim().endsWith("jar")) {
                logger.debug("Looking for plugin config files in jar: " + dir);
                dir = new File(fileStr);
                String jarPath = dir.getAbsolutePath();
                JarFile jar = new JarFile(jarPath);
                Enumeration<JarEntry> entries = jar.entries();
                while (entries.hasMoreElements()) {
                    String entry = entries.nextElement().getName();
                    if (entry.startsWith(COOS_CONFIG_PATH_WTH_SLHS)) {
                        int resourceIdx = entry.lastIndexOf("/");
                        if (resourceIdx >= 0) {
                            entry = entry.substring(resourceIdx + 1);
                        }
                        logger.debug("Found: " + entry);
                        result.add(entry);
                    }
                }
            }
        }
        return result.toArray(new String[result.size()]);
    }

    private static InputStream substitute(InputStream is) throws IOException {
        InputStreamReader isr = new InputStreamReader(is);
        MacroSubstituteReader msr = new MacroSubstituteReader(isr);
        String substituted = msr.substituteMacros();
        is = new ByteArrayInputStream(substituted.getBytes());
        return is;
    }
}
