package util.reflactTool;

import java.io.File;
import java.io.FileFilter;
import java.io.IOException;
import java.lang.reflect.Method;
import java.net.JarURLConnection;
import java.net.URL;
import java.net.URLClassLoader;
import java.net.URLDecoder;
import java.util.ArrayList;
import java.util.Enumeration;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;
import java.util.jar.JarEntry;
import java.util.jar.JarFile;

/**
 * 根据properties中配置的路径把jar和配置文件加载到classpath中。
 * 
 * @author jnbzwm
 * 
 */
public final class ExtClasspathLoader {

    /** URLClassLoader的addURL方法 */
    private Method addURL = initAddMethod();

    private URLClassLoader classloader = (URLClassLoader) ClassLoader.getSystemClassLoader();

    private List<File> jarFiles = new ArrayList<File>();

    private Set<Class<?>> loadedClass = new LinkedHashSet<Class<?>>();

    /**
     * 初始化addUrl 方法.
     * 
     * @return 可访问addUrl方法的Method对象
     */
    private Method initAddMethod() {
        try {
            Method add = URLClassLoader.class.getDeclaredMethod("addURL", new Class[] { URL.class });
            add.setAccessible(true);
            return add;
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    /**
     * 加载jar classpath。
     * @throws IOException 
     */
    private void loadClasspath() throws IOException {
        List<String> files = getJarFiles();
        for (String f : files) {
            loadClasspath(f);
        }
        List<String> resFiles = getResFiles();
        for (String r : resFiles) {
            loadResourceDir(r);
        }
        for (File jarFile : jarFiles) {
            loadedClass.addAll(getClassesInJar(jarFile));
        }
    }

    private void loadClasspath(String filepath) {
        File file = new File(filepath);
        loopFiles(file);
    }

    private void loadResourceDir(String filepath) {
        File file = new File(filepath);
        loopDirs(file);
    }

    /**
     * 循环遍历目录，找出所有的资源路径。
     * 
     * @param file
     *            当前遍历文件
     */
    private void loopDirs(File file) {
        if (file.isDirectory()) {
            addURL(file);
            File[] tmps = file.listFiles();
            for (File tmp : tmps) {
                loopDirs(tmp);
            }
        }
    }

    /**
     * 循环遍历目录，找出所有的jar包。
     * 
     * @param file
     *            当前遍历文件
     */
    private void loopFiles(File file) {
        if (file.isDirectory()) {
            File[] tmps = file.listFiles();
            for (File tmp : tmps) {
                loopFiles(tmp);
            }
        } else {
            if (file.getAbsolutePath().endsWith(".jar") || file.getAbsolutePath().endsWith(".zip")) {
                jarFiles.add(file);
                addURL(file);
            }
        }
    }

    /**
     * 通过filepath加载文件到classpath。
     * 
     * @param filePath
     *            文件路径
     * @return URL
     * @throws Exception
     *             异常
     */
    private void addURL(File file) {
        try {
            addURL.invoke(classloader, new Object[] { file.toURI().toURL() });
        } catch (Exception e) {
        }
    }

    /**
     * 从配置文件中得到配置的需要加载到classpath里的路径集合。
     * 
     * @return
     */
    private List<String> getJarFiles() {
        List<String> tmp = new ArrayList<String>();
        tmp.add("C:\\testDir");
        return tmp;
    }

    /**
     * 从配置文件中得到配置的需要加载classpath里的资源路径集合
     * 
     * @return
     */
    private List<String> getResFiles() {
        return new ArrayList<String>();
    }

    private String[] getPackageAllClassName(String classLocation, String packageName) {
        String[] packagePathSplit = packageName.split("[.]");
        String realClassLocation = classLocation;
        int packageLength = packagePathSplit.length;
        for (int i = 0; i < packageLength; i++) {
            realClassLocation = realClassLocation + File.separator + packagePathSplit[i];
        }
        File packeageDir = new File(realClassLocation);
        if (packeageDir.isDirectory()) {
            String[] allClassName = packeageDir.list();
            return allClassName;
        }
        return null;
    }

    private Set<Class<?>> getClassesInJar(File jarFile) throws IOException {
        Set<Class<?>> classes = new LinkedHashSet<Class<?>>();
        JarFile jar;
        String packageName = "";
        boolean recursive = true;
        try {
            jar = new JarFile(jarFile);
            Enumeration<JarEntry> entries = jar.entries();
            while (entries.hasMoreElements()) {
                JarEntry entry = entries.nextElement();
                String name = entry.getName();
                if (name.charAt(0) == '/') {
                    name = name.substring(1);
                }
                int idx = name.lastIndexOf('/');
                if (idx != -1) {
                    packageName = name.substring(0, idx).replace('/', '.');
                }
                if ((idx != -1) || recursive) {
                    if (name.endsWith(".class") && !entry.isDirectory()) {
                        String className = name.substring(packageName.length() + 1, name.length() - 6);
                        try {
                            classes.add(Class.forName(packageName + '.' + className));
                        } catch (ClassNotFoundException e) {
                            log("添加用户自定义视图类错误 找不到此类的.class文件");
                            e.printStackTrace();
                        }
                    }
                }
            }
        } catch (IOException e) {
            log("在扫描用户定义视图时从jar包获取文件出错");
            e.printStackTrace();
        }
        return classes;
    }

    /**
     * 从包package中获取所有的Class
     * 
     * @param pack
     * @return
     */
    private Set<Class<?>> getClasses(Package pack) {
        Set<Class<?>> classes = new LinkedHashSet<Class<?>>();
        boolean recursive = true;
        String packageName = pack.getName();
        String packageDirName = packageName.replace('.', '/');
        Enumeration<URL> dirs;
        try {
            dirs = classloader.getResources(packageDirName);
            while (dirs.hasMoreElements()) {
                URL url = dirs.nextElement();
                String protocol = url.getProtocol();
                if ("file".equals(protocol)) {
                    String filePath = URLDecoder.decode(url.getFile(), "UTF-8");
                    findAndAddClassesInPackageByFile(packageName, filePath, recursive, classes);
                } else if ("jar".equals(protocol)) {
                    JarFile jar;
                    try {
                        jar = ((JarURLConnection) url.openConnection()).getJarFile();
                        Enumeration<JarEntry> entries = jar.entries();
                        while (entries.hasMoreElements()) {
                            JarEntry entry = entries.nextElement();
                            String name = entry.getName();
                            if (name.charAt(0) == '/') {
                                name = name.substring(1);
                            }
                            if (name.startsWith(packageDirName)) {
                                int idx = name.lastIndexOf('/');
                                if (idx != -1) {
                                    packageName = name.substring(0, idx).replace('/', '.');
                                }
                                if ((idx != -1) || recursive) {
                                    if (name.endsWith(".class") && !entry.isDirectory()) {
                                        String className = name.substring(packageName.length() + 1, name.length() - 6);
                                        try {
                                            classes.add(Class.forName(packageName + '.' + className));
                                        } catch (ClassNotFoundException e) {
                                            log("添加用户自定义视图类错误 找不到此类的.class文件");
                                            e.printStackTrace();
                                        }
                                    }
                                }
                            }
                        }
                    } catch (IOException e) {
                        log("在扫描用户定义视图时从jar包获取文件出错");
                        e.printStackTrace();
                    }
                }
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
        return classes;
    }

    /**
     * 以文件的形式来获取包下的所有Class
     * 
     * @param packageName
     * @param packagePath
     * @param recursive
     * @param classes
     */
    private void findAndAddClassesInPackageByFile(String packageName, String packagePath, final boolean recursive, Set<Class<?>> classes) {
        File dir = new File(packagePath);
        if (!dir.exists() || !dir.isDirectory()) {
            log("用户定义包名 " + packageName + " 下没有任何文件");
            return;
        }
        File[] dirfiles = dir.listFiles(new FileFilter() {

            public boolean accept(File file) {
                return (recursive && file.isDirectory()) || (file.getName().endsWith(".class"));
            }
        });
        for (File file : dirfiles) {
            if (file.isDirectory()) {
                findAndAddClassesInPackageByFile(packageName + "." + file.getName(), file.getAbsolutePath(), recursive, classes);
            } else {
                String className = file.getName().substring(0, file.getName().length() - 6);
                try {
                    classes.add(Class.forName(packageName + '.' + className));
                } catch (ClassNotFoundException e) {
                    log("添加用户自定义视图类错误 找不到此类的.class文件");
                    e.printStackTrace();
                }
            }
        }
    }

    public static void main(String[] args) throws ClassNotFoundException, IllegalAccessException, InstantiationException, IOException {
        ExtClasspathLoader loader = new ExtClasspathLoader();
        loader.loadClasspath();
        Class<?> loadClass = ClassLoader.getSystemClassLoader().loadClass("test.TestObject");
        Object newInstance = loadClass.newInstance();
        System.out.println(newInstance.toString());
        URL[] urLs = loader.classloader.getURLs();
        for (URL u : urLs) {
            System.out.println(u);
        }
        Package[] packages = Package.getPackages();
        for (Package p : packages) {
            System.out.println(p);
            if (p.getName().equals("test")) {
                Set<Class<?>> classes = loader.getClasses(p);
                for (Class<?> c : classes) {
                    System.out.println(c);
                }
            }
        }
        log("----------------------");
        for (Class<?> clazz : loader.loadedClass) {
            log(clazz);
        }
    }

    public static void log(Object str) {
        System.out.println(str);
    }
}
