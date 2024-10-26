package net.paoding.analysis.knife;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.lang.reflect.Method;
import java.net.URL;
import java.util.HashMap;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Properties;
import java.util.jar.JarEntry;
import java.util.jar.JarFile;
import net.paoding.analysis.Constants;
import net.paoding.analysis.analyzer.impl.MostWordsModeDictionariesCompiler;
import net.paoding.analysis.analyzer.impl.SortingDictionariesCompiler;
import net.paoding.analysis.dictionary.support.detection.Difference;
import net.paoding.analysis.dictionary.support.detection.DifferenceListener;
import net.paoding.analysis.exception.PaodingAnalysisException;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

/**
 * 
 * @author Zhiliang Wang [qieqie.wang@gmail.com]
 * 
 * @since 2.0.0
 */
public class PaodingMaker {

    public static String DICT_HOME = "D:/OpenSource/paoding-analysis/dic";

    public static final String DEFAULT_PROPERTIES_PATH = "classpath:paoding-analysis.properties";

    private PaodingMaker() {
    }

    private static Log log = LogFactory.getLog(PaodingMaker.class);

    private static ObjectHolder propertiesHolder = new ObjectHolder();

    private static ObjectHolder paodingHolder = new ObjectHolder();

    /**
	 * 
	 * 读取类路径下的paoding-analysis.properties文件，据之获取一个Paoding对象．
	 * <p>
	 * 第一次调用本方法时，从该属性文件中读取配置，并创建一个新的Paoding对象，之后，如果
	 * 属性文件没有变更过，则每次调用本方法都将返回先前创建的Paoding对象。而不重新构建 Paoding对象。
	 * <p>
	 * 
	 * 如果配置文件没有变更，但词典文件有变更。仍然是返回同样的Paoding对象。而且是，只要
	 * 词典文件发生了变更，Paoding对象在一定时间内会收到更新的。所以返回的Paoding对象 一定是最新配置的。
	 * 
	 * 
	 * 
	 * @return
	 */
    public static Paoding make() {
        return make(DEFAULT_PROPERTIES_PATH);
    }

    /**
	 * 读取类指定路径的配置文件(如果配置文件放置在类路径下，则应该加"classpath:"为前缀)，据之获取一个新的Paoding对象．
	 * <p>
	 * 
	 * 第一次调用本方法时，从该属性文件中读取配置，并创建一个新的Paoding对象，之后，如果
	 * 属性文件没有变更过，则每次调用本方法都将返回先前创建的Paoding对象。而不重新构建 Paoding对象。
	 * <p>
	 * 
	 * 如果配置文件没有变更，但词典文件有变更。仍然是返回同样的Paoding对象。而且是，只要
	 * 词典文件发生了变更，Paoding对象在一定时间内会收到更新的。所以返回的Paoding对象 一定是最新配置的。
	 * 
	 * @param propertiesPath
	 * @return
	 */
    public static Paoding make(String propertiesPath) {
        return make(getProperties(propertiesPath));
    }

    /**
	 * 根据给定的属性对象获取一个Paoding对象．
	 * <p>
	 * 
	 * @param properties
	 * @return
	 */
    public static Paoding make(Properties p) {
        postPropertiesLoaded(p);
        return implMake(p);
    }

    public static Properties getProperties() {
        return getProperties(DEFAULT_PROPERTIES_PATH);
    }

    public static Properties getProperties(String path) {
        if (path == null) {
            throw new NullPointerException("path should not be null!");
        }
        try {
            Properties p = (Properties) propertiesHolder.get(path);
            if (p == null || modified(p)) {
                p = loadProperties(new Properties(), path);
                propertiesHolder.set(path, p);
                paodingHolder.remove(path);
                postPropertiesLoaded(p);
                String absolutePaths = p.getProperty("paoding.analysis.properties.files.absolutepaths");
                log.info("config paoding analysis from: " + absolutePaths);
            }
            return p;
        } catch (IOException e) {
            throw new PaodingAnalysisException(e);
        }
    }

    private static boolean modified(Properties p) throws IOException {
        String lastModifieds = p.getProperty("paoding.analysis.properties.lastModifieds");
        String[] lastModifedsArray = lastModifieds.split(";");
        String files = p.getProperty("paoding.analysis.properties.files");
        String[] filesArray = files.split(";");
        for (int i = 0; i < filesArray.length; i++) {
            File file = getFile(filesArray[i]);
            if (file.exists() && !String.valueOf(getFileLastModified(file)).equals(lastModifedsArray[i])) {
                return true;
            }
        }
        return false;
    }

    private static Properties loadProperties(Properties p, String path) throws IOException {
        URL url;
        File file;
        String absolutePath;
        InputStream in;
        boolean skipWhenNotExists = false;
        if (path.startsWith("ifexists:")) {
            skipWhenNotExists = true;
            path = path.substring("ifexists:".length());
        }
        if (path.startsWith("classpath:")) {
            path = path.substring("classpath:".length());
            url = getClassLoader().getResource(path);
            if (url == null) {
                if (skipWhenNotExists) {
                    return p;
                }
                throw new FileNotFoundException("Not found " + path + " in classpath.");
            }
            file = new File(url.getFile());
            in = url.openStream();
        } else {
            if (path.startsWith("dic-home:")) {
                File dicHome = new File(getDicHome(p));
                path = path.substring("dic-home:".length());
                file = new File(dicHome, path);
            } else {
                file = new File(path);
            }
            if (skipWhenNotExists && !file.exists()) {
                return p;
            }
            in = new FileInputStream(file);
        }
        absolutePath = file.getAbsolutePath();
        p.load(in);
        in.close();
        String lastModifieds = p.getProperty("paoding.analysis.properties.lastModifieds");
        String files = p.getProperty("paoding.analysis.properties.files");
        String absolutePaths = p.getProperty("paoding.analysis.properties.files.absolutepaths");
        if (lastModifieds == null) {
            p.setProperty("paoding.dic.properties.path", path);
            lastModifieds = String.valueOf(getFileLastModified(file));
            files = path;
            absolutePaths = absolutePath;
        } else {
            lastModifieds = lastModifieds + ";" + getFileLastModified(file);
            files = files + ";" + path;
            absolutePaths = absolutePaths + ";" + absolutePath;
        }
        p.setProperty("paoding.analysis.properties.lastModifieds", lastModifieds);
        p.setProperty("paoding.analysis.properties.files", files);
        p.setProperty("paoding.analysis.properties.files.absolutepaths", absolutePaths);
        String importsValue = p.getProperty("paoding.imports");
        if (importsValue != null) {
            p.remove("paoding.imports");
            String[] imports = importsValue.split(";");
            for (int i = 0; i < imports.length; i++) {
                loadProperties(p, imports[i]);
            }
        }
        return p;
    }

    private static long getFileLastModified(File file) throws IOException {
        String path = file.getPath();
        int jarIndex = path.indexOf(".jar!");
        if (jarIndex == -1) {
            return file.lastModified();
        } else {
            path = path.replaceAll("%20", " ").replaceAll("\\\\", "/");
            jarIndex = path.indexOf(".jar!");
            int protocalIndex = path.indexOf(":");
            String jarPath = path.substring(protocalIndex + ":".length(), jarIndex + ".jar".length());
            File jarPathFile = new File(jarPath);
            JarFile jarFile;
            try {
                jarFile = new JarFile(jarPathFile);
                String entryPath = path.substring(jarIndex + ".jar!/".length());
                JarEntry entry = jarFile.getJarEntry(entryPath);
                return entry.getTime();
            } catch (IOException e) {
                System.err.println("error in handler path=" + path);
                System.err.println("error in handler jarPath=" + jarPath);
                throw e;
            }
        }
    }

    private static String getDicHome(Properties p) {
        setDicHomeProperties(p);
        return p.getProperty("paoding.dic.home.absolute.path");
    }

    private static void postPropertiesLoaded(Properties p) {
        if ("done".equals(p.getProperty("paoding.analysis.postPropertiesLoaded"))) {
            return;
        }
        setDicHomeProperties(p);
        p.setProperty("paoding.analysis.postPropertiesLoaded", "done");
    }

    private static void setDicHomeProperties(Properties p) {
        String dicHomeAbsultePath = p.getProperty("paoding.dic.home.absolute.path");
        if (dicHomeAbsultePath != null) {
            return;
        }
        String dicHomeBySystemEnv = null;
        try {
            dicHomeBySystemEnv = getSystemEnv(Constants.ENV_PAODING_DIC_HOME);
        } catch (Error e) {
            log.warn("System.getenv() is not supported in JDK1.4. ");
        }
        String dicHome = getProperty(p, Constants.DIC_HOME);
        if (dicHomeBySystemEnv != null) {
            String first = getProperty(p, Constants.DIC_HOME_CONFIG_FIRST);
            if (first != null && first.equalsIgnoreCase("this")) {
                if (dicHome == null) {
                    dicHome = dicHomeBySystemEnv;
                }
            } else {
                dicHome = dicHomeBySystemEnv;
            }
        }
        if (dicHome == null) {
            File f = new File("dic");
            if (f.exists()) {
                dicHome = "dic/";
            } else {
                URL url = PaodingMaker.class.getClassLoader().getResource("dic");
                if (url != null) {
                    dicHome = "classpath:dic/";
                }
            }
        }
        if (dicHome == null) {
            dicHome = DICT_HOME;
        }
        dicHome = dicHome.replace('\\', '/');
        if (!dicHome.endsWith("/")) {
            dicHome = dicHome + "/";
        }
        p.setProperty(Constants.DIC_HOME, dicHome);
        File dicHomeFile = getFile(dicHome);
        if (!dicHomeFile.exists()) {
            throw new PaodingAnalysisException("not found the dic home dirctory! " + dicHomeFile.getAbsolutePath());
        }
        if (!dicHomeFile.isDirectory()) {
            throw new PaodingAnalysisException("dic home should not be a file, but a directory!");
        }
        p.setProperty("paoding.dic.home.absolute.path", dicHomeFile.getAbsolutePath());
    }

    private static Paoding implMake(final Properties p) {
        Paoding paoding;
        final Object paodingKey;
        String path = p.getProperty("paoding.dic.properties.path");
        if (path != null) {
            paodingKey = path;
        } else {
            paodingKey = p;
        }
        paoding = (Paoding) paodingHolder.get(paodingKey);
        if (paoding != null) {
            return paoding;
        }
        try {
            paoding = createPaodingWithKnives(p);
            final Paoding finalPaoding = paoding;
            String compilerClassName = getProperty(p, Constants.ANALYZER_DICTIONARIES_COMPILER);
            Class compilerClass = null;
            if (compilerClassName != null) {
                compilerClass = Class.forName(compilerClassName);
            }
            if (compilerClass == null) {
                String analyzerMode = getProperty(p, Constants.ANALYZER_MODE);
                if ("most-words".equalsIgnoreCase(analyzerMode) || "default".equalsIgnoreCase(analyzerMode)) {
                    compilerClass = MostWordsModeDictionariesCompiler.class;
                } else {
                    compilerClass = SortingDictionariesCompiler.class;
                }
            }
            final DictionariesCompiler compiler = (DictionariesCompiler) compilerClass.newInstance();
            new Function() {

                public void run() throws Exception {
                    if (compiler.shouldCompile(p)) {
                        Dictionaries dictionaries = readUnCompiledDictionaries(p);
                        Paoding tempPaoding = createPaodingWithKnives(p);
                        setDictionaries(tempPaoding, dictionaries);
                        compiler.compile(dictionaries, tempPaoding, p);
                    }
                    final Dictionaries dictionaries = compiler.readCompliedDictionaries(p);
                    setDictionaries(finalPaoding, dictionaries);
                    String intervalStr = getProperty(p, Constants.DIC_DETECTOR_INTERVAL);
                    int interval = Integer.parseInt(intervalStr);
                    if (interval > 0) {
                        dictionaries.startDetecting(interval, new DifferenceListener() {

                            public void on(Difference diff) throws Exception {
                                dictionaries.stopDetecting();
                                run();
                            }
                        });
                    }
                }
            }.run();
            paodingHolder.set(paodingKey, paoding);
            return paoding;
        } catch (Exception e) {
            throw new PaodingAnalysisException("", e);
        }
    }

    private static Paoding createPaodingWithKnives(Properties p) throws Exception {
        Paoding paoding = new Paoding();
        final Map knifeMap = new HashMap();
        final List knifeList = new LinkedList();
        final List functions = new LinkedList();
        Iterator iter = p.entrySet().iterator();
        while (iter.hasNext()) {
            Map.Entry e = (Map.Entry) iter.next();
            final String key = (String) e.getKey();
            final String value = (String) e.getValue();
            int index = key.indexOf(Constants.KNIFE_CLASS);
            if (index == 0 && key.length() > Constants.KNIFE_CLASS.length()) {
                final int end = key.indexOf('.', Constants.KNIFE_CLASS.length());
                if (end == -1) {
                    Class clazz = Class.forName(value);
                    Knife knife = (Knife) clazz.newInstance();
                    knifeList.add(knife);
                    knifeMap.put(key, knife);
                    log.info("add knike: " + value);
                } else {
                    functions.add(new Function() {

                        public void run() throws Exception {
                            String knifeName = key.substring(0, end);
                            Object obj = knifeMap.get(knifeName);
                            if (!obj.getClass().getName().equals("org.springframework.beans.BeanWrapperImpl")) {
                                Class beanWrapperImplClass = Class.forName("org.springframework.beans.BeanWrapperImpl");
                                Method setWrappedInstance = beanWrapperImplClass.getMethod("setWrappedInstance", new Class[] { Object.class });
                                Object beanWrapperImpl = beanWrapperImplClass.newInstance();
                                setWrappedInstance.invoke(beanWrapperImpl, new Object[] { obj });
                                knifeMap.put(knifeName, beanWrapperImpl);
                                obj = beanWrapperImpl;
                            }
                            String propertyName = key.substring(end + 1);
                            Method setPropertyValue = obj.getClass().getMethod("setPropertyValue", new Class[] { String.class, Object.class });
                            setPropertyValue.invoke(obj, new Object[] { propertyName, value });
                        }
                    });
                }
            }
        }
        for (Iterator iterator = functions.iterator(); iterator.hasNext(); ) {
            Function function = (Function) iterator.next();
            function.run();
        }
        paoding.setKnives(knifeList);
        return paoding;
    }

    private static Dictionaries readUnCompiledDictionaries(Properties p) {
        String skipPrefix = getProperty(p, Constants.DIC_SKIP_PREFIX);
        String noiseCharactor = getProperty(p, Constants.DIC_NOISE_CHARACTOR);
        String noiseWord = getProperty(p, Constants.DIC_NOISE_WORD);
        String unit = getProperty(p, Constants.DIC_UNIT);
        String confucianFamilyName = getProperty(p, Constants.DIC_CONFUCIAN_FAMILY_NAME);
        String combinatorics = getProperty(p, Constants.DIC_FOR_COMBINATORICS);
        String charsetName = getProperty(p, Constants.DIC_CHARSET);
        Dictionaries dictionaries = new FileDictionaries(getDicHome(p), skipPrefix, noiseCharactor, noiseWord, unit, confucianFamilyName, combinatorics, charsetName);
        return dictionaries;
    }

    private static void setDictionaries(Paoding paoding, Dictionaries dictionaries) {
        Knife[] knives = paoding.getKnives();
        for (int i = 0; i < knives.length; i++) {
            Knife knife = (Knife) knives[i];
            if (knife instanceof DictionariesWare) {
                ((DictionariesWare) knife).setDictionaries(dictionaries);
            }
        }
    }

    private static File getFile(String path) {
        File file;
        URL url;
        if (path.startsWith("classpath:")) {
            path = path.substring("classpath:".length());
            url = getClassLoader().getResource(path);
            final boolean fileExist = url != null;
            file = new File(fileExist ? url.getFile() : path) {

                private static final long serialVersionUID = 4009013298629147887L;

                public boolean exists() {
                    return fileExist;
                }
            };
        } else {
            file = new File(path);
        }
        return file;
    }

    private static ClassLoader getClassLoader() {
        ClassLoader loader = Thread.currentThread().getContextClassLoader();
        if (loader == null) {
            loader = PaodingMaker.class.getClassLoader();
        }
        return loader;
    }

    private static String getProperty(Properties p, String name) {
        return Constants.getProperty(p, name);
    }

    private static class ObjectHolder {

        private ObjectHolder() {
        }

        private Map objects = new HashMap();

        public Object get(Object name) {
            return objects.get(name);
        }

        public void set(Object name, Object object) {
            objects.put(name, object);
        }

        public void remove(Object name) {
            objects.remove(name);
        }
    }

    private static interface Function {

        public void run() throws Exception;
    }

    private static String getSystemEnv(String name) {
        try {
            return System.getenv(name);
        } catch (Error error) {
            String osName = System.getProperty("os.name").toLowerCase();
            try {
                String cmd;
                if (osName.indexOf("win") != -1) {
                    cmd = "cmd /c SET";
                } else {
                    cmd = "/usr/bin/printenv";
                }
                Process process = Runtime.getRuntime().exec(cmd);
                InputStreamReader isr = new InputStreamReader(process.getInputStream());
                BufferedReader br = new BufferedReader(isr);
                String line;
                while ((line = br.readLine()) != null && line.startsWith(name)) {
                    int index = line.indexOf(name + "=");
                    if (index != -1) {
                        return line.substring(index + name.length() + 1);
                    }
                }
            } catch (Exception e) {
                log.warn("unable to read env from os．" + e.getMessage(), e);
            }
        }
        return null;
    }
}
