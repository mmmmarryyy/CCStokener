package org.jiopi.ibean.share;

import java.io.Closeable;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.io.FileOutputStream;
import java.io.BufferedOutputStream;
import java.io.FileInputStream;
import java.io.BufferedInputStream;
import java.io.RandomAccessFile;
import java.net.URL;
import java.net.URLConnection;
import java.nio.channels.FileLock;
import java.util.HashSet;
import java.util.Iterator;
import java.util.Properties;
import java.util.TreeSet;
import java.util.logging.Level;
import org.jiopi.framework.core.JiopiConfigConstants;
import org.jiopi.ibean.loader.log.LoaderLogUtil;

/**
 * 
 * 共享工具类
 * 
 * @since 2010.4.9
 */
public class ShareUtil {

    /**
	 * 处理字符串相关操作
	 *
	 */
    public static class StringUtil {

        /**
		 * 把字符串前面增加0到指定长度
		 * 
		 * @param s
		 * @param maxLen
		 * @return
		 */
        public static String fixToLen(String s, int maxLen) {
            int len = s.length();
            if (len < maxLen) {
                int fit = maxLen - len;
                StringBuilder buffer = new StringBuilder();
                for (int i = 0; i < fit; i++) {
                    buffer.append('0');
                }
                buffer.append(s);
                return buffer.toString();
            }
            return s;
        }

        /**
		 * 将字符串转换为long
		 * 
		 * @param s
		 * @param defaultValue
		 * @throws NumberFormatException s 和 defaultValue 都无法转换为long时抛出
		 * @return
		 */
        public static int getInt(String s, String defaultValue) {
            int ret;
            try {
                ret = Integer.parseInt(s);
            } catch (Exception e) {
                LoaderLogUtil.logExceptionTrace(ShareConstants.shareLogger, Level.WARNING, e);
                ret = Integer.parseInt(defaultValue);
            }
            return ret;
        }

        public static boolean getBoolean(String s, boolean defaultValue) {
            boolean ret = defaultValue;
            try {
                if (s != null) {
                    s = s.trim();
                    ret = Boolean.parseBoolean(s);
                }
            } catch (Exception e) {
                LoaderLogUtil.logExceptionTrace(ShareConstants.shareLogger, Level.WARNING, e);
            }
            return ret;
        }
    }

    /**
	 * 资源处理相关函数
	 * @author User
	 *
	 */
    public static class ResourceUtil {

        /**
		 * 获得程序下载
		 * @return
		 */
        private static volatile String programDir = null;

        public static String getProgramDir() {
            if (programDir == null) {
                synchronized ("programDir".intern()) {
                    if (programDir == null) {
                        Properties[] jiopiProperties = ResourceUtil.getJIOPICascadingConfig();
                        String downloadDirPath = ResourceUtil.getPropertyValue(ShareConstants.IBEAN_DOWNLOAD_DIR, jiopiProperties, null, false);
                        if (downloadDirPath == null) {
                            downloadDirPath = ResourceUtil.getPropertyValue(ShareConstants.IBEAN_WORK_DIR, jiopiProperties, ShareConstants.IBEAN_WORK_DIR_DEFAULT, false);
                        }
                        downloadDirPath = FileUtil.getPathWithSystemProperty(downloadDirPath);
                        programDir = FileUtil.joinPath(downloadDirPath, ShareConstants.PROGRAM_PATH);
                    }
                }
            }
            return programDir;
        }

        /**
		 * 获取iBean的程序安装目录
		 * @return 
		 */
        private static volatile String iBeanProgramDir = null;

        public static String getIBeanProgramDir() {
            if (iBeanProgramDir == null) {
                synchronized ("iBeanProgramDir".intern()) {
                    if (iBeanProgramDir == null) {
                        iBeanProgramDir = FileUtil.joinPath(getProgramDir(), ShareConstants.IBEAN_PROGRAM_PATH);
                    }
                }
            }
            return iBeanProgramDir;
        }

        public static void deleteUnfinishedVersionDir(File baseDir) {
            if (!baseDir.isDirectory()) return;
            File[] subFiles = baseDir.listFiles();
            HashSet<String> checkedDir = new HashSet<String>();
            for (File file : subFiles) {
                if (file.isFile()) {
                    String name = file.getName();
                    if (name.startsWith(".ver")) {
                        String getDirName = ResourceUtil.getVersionDirName(name.substring(4));
                        if (getDirName != null) checkedDir.add(getDirName); else file.deleteOnExit();
                    }
                }
            }
            for (File file : subFiles) {
                if (file.isDirectory()) {
                    String dirName = file.getName();
                    if (!checkedDir.contains(dirName)) {
                        FileUtil.deleteFile(file);
                    }
                }
            }
        }

        /**
		 * 检测给定的字符串是否符合版本条件
		 * @param version
		 * @return
		 */
        public static boolean isCorrectVersion(String version) {
            if (version == null) return false;
            return ShareConstants.VERSION_PATTERN.matcher(version).find();
        }

        /**
		 * 根据versionString获得标准version字符串
		 * @param versionString
		 * @return 格式错误返回 null
		 */
        public static String getVersionDirName(String versionString) {
            StringBuilder sb = new StringBuilder();
            int sl = versionString.length();
            int intPosMax = 0;
            switch(sl) {
                case 8:
                case 20:
                    intPosMax = 2;
                    break;
                case 16:
                case 28:
                    intPosMax = 4;
                    break;
            }
            if (intPosMax == 0) {
                return null;
            }
            int beginPos = 0;
            int endPos = 4;
            int nowPos = 0;
            do {
                nowPos++;
                if (nowPos > 1) {
                    sb.append('.');
                }
                String num = versionString.substring(beginPos, endPos);
                if (nowPos <= intPosMax) {
                    sb.append(Integer.parseInt(num, 16));
                } else {
                    sb.append(Long.parseLong(num, 16));
                }
                beginPos = endPos;
                if (nowPos < intPosMax) endPos += 4; else endPos += 12;
            } while (endPos <= sl);
            return sb.toString();
        }

        /**
		 * 返回给定版本的标准长度字符串
		 * @param version  版本号
		 * @param time     版本号的更新时间, 如果小于0,则不添加时间因素
		 * @return 运行时出错返回 null
		 */
        public static String getVersionString(String version, long time) {
            try {
                StringBuilder sb = new StringBuilder();
                String[] vers = version.split("\\.");
                if (vers.length == 4 || vers.length == 2) {
                    for (String ver : vers) {
                        int verNum = Integer.parseInt(ver);
                        sb.append(StringUtil.fixToLen(Integer.toHexString(verNum), 4));
                    }
                }
                if (time > 0) sb.append(StringUtil.fixToLen(Long.toHexString(time / 1000), 12));
                return sb.toString().toLowerCase();
            } catch (Exception e) {
                LoaderLogUtil.logExceptionTrace(ShareConstants.shareLogger, Level.WARNING, e);
            }
            return null;
        }

        /**
		 * 将url对应的资源拷贝到文件中
		 * 
		 * 使用文件锁等待其他下载线程
		 * 
		 * @param url
		 * @param outputFile
		 * @param rewrite 是否覆盖,仅在资源内容大小相同时有效
		 * @throws IOException
		 */
        public static void copyContent(final URL url, final File outputFile, boolean rewrite) throws IOException {
            if (outputFile.exists()) {
                if (outputFile.isDirectory()) return; else if (!rewrite) {
                    int urlContentLength = FileUtil.getURLContentLength(url);
                    long fileLength = outputFile.length();
                    if (fileLength == urlContentLength) return;
                }
            }
            String outputFilePath = outputFile.getAbsolutePath();
            String outputFilePathTemp = outputFilePath + ".tmp";
            File tmpDownloadFile = FileUtil.createNewFile(outputFilePathTemp, false);
            if (!tmpDownloadFile.isFile()) return;
            MyFileLock fl = FileUtil.tryLockTempFile(tmpDownloadFile, 1000, ShareConstants.connectTimeout);
            if (fl != null) {
                try {
                    if (outputFile.isFile()) outputFile.delete();
                    if (!outputFile.isFile()) {
                        OutputStream out = null;
                        InputStream in = null;
                        try {
                            in = FileUtil.getURLInputStream(url);
                            out = new BufferedOutputStream(new FileOutputStream(tmpDownloadFile));
                            IOUtil.copyStreams(in, out);
                        } finally {
                            IOUtil.close(in);
                            IOUtil.close(out);
                        }
                        if (tmpDownloadFile.length() > 0) IOUtil.copyFile(tmpDownloadFile, outputFile);
                    }
                } finally {
                    tmpDownloadFile.delete();
                    fl.release();
                }
            }
        }

        /**
		 * 从Properties[]数组中按照优先级覆盖原则获取指定属性
		 * 
		 * @param key
		 * @param properties
		 * @param default    默认值
		 * @param highBegin  高优先级在先
		 * @return
		 */
        public static String getPropertyValue(String key, Properties[] properties, String defalut, boolean highBegin) {
            if (properties == null || properties.length < 1) return defalut;
            int begin = 0;
            int add = 1;
            if (!highBegin) {
                begin = properties.length - 1;
                add = -1;
            }
            for (int i = 0; i < properties.length; i++) {
                String value = properties[begin].getProperty(key);
                if (value != null) return value;
                begin += add;
            }
            return defalut;
        }

        /**
		 * 得到JIOPI级联式配置
		 * 
		 * 级联规则:
		 * 由于允许将iBean.jar放在任何一个级别使用(如System级,Context级,或某个应用的内部(OSGi bundle)),
		 * iBean将以 读取iBean.jar的ClassLoader 和 ContextClassLoader进行对比
		 * 以层次级别高的那个作为iBean容器的ContextClassLoader
		 * 不同ContextClassLoader的iBean容器互不相通
		 * 
		 * 配置文件则按级联式规则读取,高层次ClassLoader内获取到的配置文件将覆盖低层次拿到的
		 * 
		 * @return 配置文件列表,最多2个,第一个是高层次的,第二个是低层次的
		 */
        public static Properties[] getJIOPICascadingConfig() {
            ClassLoader jiopiClassLoader = ClassUtil.getClassLoaderByClass(JiopiConfigConstants.class);
            ClassLoader contextClassLoader = Thread.currentThread().getContextClassLoader();
            if (jiopiClassLoader == null && contextClassLoader != null) {
                return new Properties[] { getPropertiesFromClassPath(contextClassLoader, JiopiConfigConstants.CONFIG_FILE, false) };
            } else if (contextClassLoader == null && jiopiClassLoader != null) {
                return new Properties[] { getPropertiesFromClassPath(jiopiClassLoader, JiopiConfigConstants.CONFIG_FILE, false) };
            } else if (jiopiClassLoader == contextClassLoader && jiopiClassLoader != null) {
                return new Properties[] { getPropertiesFromClassPath(jiopiClassLoader, JiopiConfigConstants.CONFIG_FILE, false) };
            } else if (jiopiClassLoader == contextClassLoader && jiopiClassLoader == null) {
                throw new RuntimeException("cannot find configuration file: " + JiopiConfigConstants.CONFIG_FILE + " in class path.");
            }
            ClassLoader high = null;
            ClassLoader low = null;
            int compare = ClassUtil.compareClassLoader(jiopiClassLoader, contextClassLoader);
            if (compare == 0) {
                high = jiopiClassLoader;
                low = contextClassLoader;
            } else {
                high = contextClassLoader;
                low = jiopiClassLoader;
            }
            URL highURL = high.getResource(JiopiConfigConstants.CONFIG_FILE);
            URL lowURL = low.getResource(JiopiConfigConstants.CONFIG_FILE);
            if (highURL == null && lowURL != null) {
                return new Properties[] { getPropertiesFromClassPath(low, JiopiConfigConstants.CONFIG_FILE, false) };
            } else if (lowURL == null && highURL != null) {
                return new Properties[] { getPropertiesFromClassPath(high, JiopiConfigConstants.CONFIG_FILE, false) };
            } else if (lowURL == highURL && highURL == null) {
                throw new RuntimeException("cannot find configuration file: " + JiopiConfigConstants.CONFIG_FILE + " in class path.");
            } else if (highURL.equals(lowURL)) {
                return new Properties[] { getPropertiesFromClassPath(high, JiopiConfigConstants.CONFIG_FILE, false) };
            } else {
                Properties highPro = getPropertiesFromClassPath(high, JiopiConfigConstants.CONFIG_FILE, true);
                Properties lowPro = getPropertiesFromClassPath(low, JiopiConfigConstants.CONFIG_FILE, true);
                if (highPro == null && lowPro != null) return new Properties[] { lowPro }; else if (lowPro == null && highPro != null) return new Properties[] { highPro }; else if (lowPro != null && highPro != null) return new Properties[] { highPro, lowPro }; else throw new RuntimeException("cannot find configuration file: " + JiopiConfigConstants.CONFIG_FILE + " in class path.");
            }
        }

        /**
		 * 从一个URL表示的properties文件获取Properties对象
		 * @param url
		 * @return
		 * @throws IOException
		 */
        public static Properties getPropertiesFormURL(URL url) throws IOException {
            Properties props = new Properties();
            InputStream in = FileUtil.getURLInputStream(url);
            try {
                props.load(in);
            } finally {
                IOUtil.close(in);
            }
            return props;
        }

        /**
		 * 从指定classLoader的ClassPath中载入Properties文件对象
		 * @param classLoader
		 * @param name
		 * @param swallow
		 * @return
		 */
        public static Properties getPropertiesFromClassPath(ClassLoader classLoader, String name, boolean swallow) {
            Properties props = new Properties();
            InputStream in = null;
            try {
                in = classLoader.getResourceAsStream(name);
                if (in != null) {
                    props.load(in);
                } else {
                    if (swallow) return null; else throw new RuntimeException("cannot find configuration file: " + name + " in class path.");
                }
            } catch (Exception e) {
                if (swallow) return null; else throw new RuntimeException(e);
            } finally {
                IOUtil.close(in);
            }
            return props;
        }
    }

    /**
	 * 处理和IO流相关的函数
	 * @author User
	 *
	 */
    public static class IOUtil {

        public static void copyFile(final File from, final File to) throws IOException {
            InputStream in = null;
            OutputStream out = null;
            try {
                in = new BufferedInputStream(new FileInputStream(from));
                out = new BufferedOutputStream(new FileOutputStream(to));
                IOUtil.copyStreams(in, out);
            } finally {
                IOUtil.close(in);
                IOUtil.close(out);
            }
        }

        /**
		 * 流复制
		 * 
		 * @param in
		 * @param out
		 * @throws IOException
		 */
        public static void copyStreams(final InputStream in, final OutputStream out) throws IOException {
            copyStreams(in, out, 4096);
        }

        /**
		 * 流复制
		 * @param in
		 * @param out
		 * @param buffersize
		 * @throws IOException
		 */
        private static void copyStreams(final InputStream in, final OutputStream out, final int buffersize) throws IOException {
            final byte[] bytes = new byte[buffersize];
            int bytesRead = in.read(bytes);
            while (bytesRead > -1) {
                out.write(bytes, 0, bytesRead);
                bytesRead = in.read(bytes);
            }
        }

        /**
		 * 关闭可关闭对象
		 * 
		 * @param c
		 */
        public static void close(Closeable c) {
            if (c != null) {
                try {
                    c.close();
                } catch (Exception e) {
                    LoaderLogUtil.logExceptionTrace(ShareConstants.shareLogger, Level.WARNING, e);
                }
            }
        }
    }

    /**
	 * 定义和Class,ClassLoader相关的函数
	 * @author User
	 *
	 */
    public static class ClassUtil {

        /**
		 * 根据jiopiClassLoader 和 contextClassLoader确定当前应使用的iBeanContextClassLoader
		 * @param jiopiClassLoader
		 * @return
		 */
        public static ClassLoader getJIOPIContextClassLoader(ClassLoader jiopiClassLoader) {
            ClassLoader contextClassLoader = Thread.currentThread().getContextClassLoader();
            if (jiopiClassLoader == contextClassLoader && contextClassLoader != null) {
                return contextClassLoader;
            } else if (jiopiClassLoader == null && contextClassLoader == null) {
                return null;
            } else if (jiopiClassLoader == null) {
                return contextClassLoader;
            } else if (contextClassLoader == null) {
                return jiopiClassLoader;
            } else {
                ClassLoader high = null;
                int compare = ClassUtil.compareClassLoader(jiopiClassLoader, contextClassLoader);
                if (compare == 0) {
                    high = jiopiClassLoader;
                } else {
                    high = contextClassLoader;
                }
                return high;
            }
        }

        /**
		 * 比较给定的两个ClassLoader的载入关系
		 * 
		 * 如果 b 的ClassLoader类是由 a 载入(直接/间接)的,则返回 1 否则返回 0
		 * 如果两者不存在载入关系,则返回 -1
		 * @param a
		 * @param b
		 * @return
		 */
        public static int compareClassLoader(ClassLoader a, ClassLoader b) {
            if (isLoaderClass(a, b)) return 1; else if (isLoaderClass(b, a)) return 0; else return -1;
        }

        /**
		 * 判断 son 的类是否是 从 father 的ClassLoader(或其载入的其他ClassLoader) 载入的
		 * @param son
		 * @param father
		 * @return
		 */
        public static boolean isLoaderClass(ClassLoader father, ClassLoader son) {
            if (son == null || father == null) return false;
            boolean find = false;
            do {
                son = son.getClass().getClassLoader();
                if (son == father) {
                    find = true;
                    break;
                }
            } while (son != null);
            return find;
        }

        /**
		 * 得到给定类的CLassLoad
		 * @param c
		 * @return
		 */
        public static ClassLoader getClassLoaderByClass(Class<?> c) {
            ClassLoader cl = c.getClassLoader();
            if (cl == null) cl = ClassLoader.getSystemClassLoader();
            return cl;
        }
    }

    /**
	 * 定义和文件,文件夹,URL处理相关的工具函数
	 *
	 */
    public static class FileUtil {

        /**
		 * 递归删除指定文件/文件夹
		 * @param file
		 */
        public static void deleteFile(File file) {
            if (file.exists()) {
                if (!file.isDirectory()) {
                    file.delete();
                } else {
                    File[] children = file.listFiles();
                    if (children != null && children.length > 0) {
                        for (File child : children) {
                            deleteFile(child);
                        }
                    }
                    file.delete();
                }
            }
        }

        /**
		 * 对指定的临时文件进行加锁
		 * 如果被锁定文件已经被其他线程/进程锁定,则等待
		 * 每隔intervalTime进行一次检测
		 * 如果timeOut时间内被锁定文件无文件大小变化,则返回null
		 * 如果待锁定文件被删除,立即返回null
		 * 
		 * @param lockFile  要加锁的文件
		 * @param intervalTime  未锁定后下次检测的时间间隔，单位:毫秒
		 * @param timeOut       待锁定文件允许的最长无变化时间，单位:毫秒
		 * @return
		 * @throws IOException 
		 */
        public static MyFileLock tryLockTempFile(File lockFile, long intervalTime, long timeOut) throws IOException {
            if (!lockFile.isFile()) return null;
            FileLock fl = null;
            long fileSize = lockFile.length();
            long checkTime = System.currentTimeMillis();
            String fileLockPath = lockFile.getAbsolutePath() + ".lock";
            File fileLockFile = FileUtil.createNewFile(fileLockPath, false);
            if (!fileLockFile.isFile()) return null;
            RandomAccessFile raf = new RandomAccessFile(fileLockPath, "rw");
            do {
                fl = raf.getChannel().tryLock();
                if (fl == null) {
                    try {
                        Thread.sleep(intervalTime);
                    } catch (InterruptedException e) {
                        LoaderLogUtil.logExceptionTrace(ShareConstants.shareLogger, Level.WARNING, e);
                        return null;
                    }
                    if (lockFile.isFile()) {
                        long newFileSize = lockFile.length();
                        if (newFileSize == fileSize) {
                            if (System.currentTimeMillis() - checkTime > timeOut) {
                                break;
                            }
                        } else {
                            fileSize = newFileSize;
                            checkTime = System.currentTimeMillis();
                        }
                    } else {
                        break;
                    }
                }
            } while (fl == null);
            if (fl == null) {
                IOUtil.close(raf);
                return null;
            } else {
                MyFileLock mfl = new MyFileLock(raf, fl, fileLockFile);
                return mfl;
            }
        }

        public static int getURLContentLength(URL url) throws IOException {
            URLConnection con = url.openConnection();
            con.setConnectTimeout(ShareConstants.connectTimeout);
            con.setReadTimeout(ShareConstants.connectTimeout);
            con.setUseCaches(false);
            con.connect();
            return con.getContentLength();
        }

        /**
		 * 获得给定URL的数据读取流
		 * 
		 * @param url
		 * @return
		 * @throws IOException 
		 */
        public static InputStream getURLInputStream(URL url) throws IOException {
            URLConnection con = url.openConnection();
            con.setConnectTimeout(ShareConstants.connectTimeout);
            con.setReadTimeout(ShareConstants.connectTimeout);
            con.setUseCaches(false);
            con.connect();
            return con.getInputStream();
        }

        /**
		 * 
		 * 获得指定程序目录中的所有可用版本目录名,按版本从高到低排序<br/>
		 * 如果该目录不存在,或者目录下没有可用版本,返回null
		 * 
		 * @param programPath
		 * @return String[]
		 */
        public static String[] getVersionsDirName(String programDirPath) {
            File programDir = confirmDir(programDirPath, false);
            if (programDir == null) {
                return null;
            }
            File[] subFiles = programDir.listFiles();
            String[] rt = null;
            if (subFiles.length > 0) {
                TreeSet<String> dirNames = new TreeSet<String>();
                for (int i = 0; i < subFiles.length; i++) {
                    if (subFiles[i].isFile()) {
                        String dirName = subFiles[i].getName();
                        if (dirName.startsWith(".ver")) {
                            String getDirName = ResourceUtil.getVersionDirName(dirName.substring(4));
                            if (getDirName != null) dirNames.add(getDirName);
                        }
                    }
                }
                int length = dirNames.size();
                if (length > 0) {
                    rt = new String[length];
                    Iterator<String> it = dirNames.iterator();
                    for (int i = 0; i < length; i++) {
                        int ri = length - 1 - i;
                        if (it.hasNext()) {
                            rt[ri] = it.next();
                        }
                    }
                }
            }
            return rt;
        }

        /**
		 * 
		 * 创建指定文件
		 * 
		 * 如果存在同名目录,或者创建文件失败,或删除已存在文件失败(deleteExists=true),返回null
		 * 
		 * @param filePath
		 * @param deleteExists 是否删除已经存在的文件
		 * @return 新文件的File对象
		 * @throws IOException 
		 */
        public static File createNewFile(String filePath, boolean deleteExists) throws IOException {
            File newFile = new File(filePath);
            if (newFile.exists()) {
                if (newFile.isFile() && deleteExists) {
                    newFile.delete();
                    if (newFile.exists()) {
                        return null;
                    }
                } else if (newFile.isDirectory()) {
                    return null;
                } else if (newFile.isFile()) {
                    return newFile;
                }
            }
            File dir = confirmDir(newFile.getParent(), true);
            if (dir != null) {
                newFile.createNewFile();
                if (newFile.isFile()) return newFile;
            }
            return null;
        }

        /**
		 * 将以$开头的路径认为是包含系统环境变量
		 * 并将系统变量替换为具体值
		 * 
		 * @param path
		 * @return
		 */
        public static String getPathWithSystemProperty(String path) {
            path = FileUtil.standardizeFileSeparator(path);
            if (path.charAt(0) == '$') {
                int begin = path.indexOf('{');
                if (begin < 0) return path; else begin = begin + 1;
                int end = path.indexOf('}', begin);
                if (end < 0) return path;
                String systemPropertyName = path.substring(begin, end);
                String systemProperty = System.getProperty(systemPropertyName);
                if (systemProperty == null) systemProperty = "";
                systemProperty = FileUtil.standardizeFileSeparator(systemProperty);
                systemProperty = FileUtil.correctDirPath(systemProperty);
                path = path.replaceFirst("\\$\\{" + systemPropertyName + "\\}", systemProperty);
            }
            return path;
        }

        /**
		 * 将一组File转换为URL[]
		 * @param paths
		 * @return
		 */
        public static URL[] toURL(File[] paths) {
            URL[] urls = new URL[paths.length];
            int i = 0;
            for (File path : paths) {
                urls[i] = toURL(path.getAbsolutePath());
                i++;
            }
            return urls;
        }

        /**
		 * 将路径转为url
		 * 
		 * @param s 路径
		 * @return url
		 */
        public static URL toURL(String s) {
            URL url = getURL(s, null, true);
            if (url == null) {
                File testFile = new File(s);
                if (testFile.isDirectory()) {
                    if (!s.endsWith("/")) {
                        s = s + "/";
                    }
                }
                if (!s.startsWith("/")) {
                    s = "/" + s;
                }
                s = ShareConstants.FILE_PROTOCOL + "://" + s;
                url = getURL(s, null, false);
            }
            if (url == null) {
                throw new RuntimeException("cannot change " + s + " to url.");
            }
            return url;
        }

        /**
		 * 将路径转换为URL
		 * @param url 需要转换的url
		 * @param defaultValue defaultValue 默认的值
		 * @param swallow 屏蔽异常
		 * @return
		 */
        public static URL getURL(String url, URL defaultValue, boolean swallow) {
            URL ret = defaultValue;
            if (url != null) {
                try {
                    ret = new URL(url);
                } catch (Exception e) {
                    if (!swallow) {
                        LoaderLogUtil.logExceptionTrace(ShareConstants.shareLogger, Level.WARNING, e);
                        throw new RuntimeException(e);
                    }
                }
            }
            return ret;
        }

        /**
		 * 确保给定的路径是一个文件夹
		 * 如果不存在,则创建文件夹
		 * 
		 * 如果创建不成功，或者存在同名文件,则返回Null
		 * 
		 * @param path
		 * @param makeDir 是否创建目录,如果不存在
		 * @return 文件夹的File对象
		 */
        public static File confirmDir(String path, boolean makeDir) {
            File dir = new File(path);
            if (!dir.exists() && makeDir) {
                dir.mkdirs();
            }
            if (dir.isDirectory()) {
                return dir;
            }
            return null;
        }

        /**
		 * 链接给定的路径
		 * @param path 若干路径段
		 * @return
		 */
        public static String joinPath(String... path) {
            if (path == null || path.length < 1) return "";
            String joined = path[0];
            for (int i = 1; i < path.length; i++) {
                joined = correctDirPath(joined) + correctFilePath(path[i]);
            }
            return correctDirPath(joined);
        }

        /**
		 * 将目录路径转换为标准格式,即无/结尾的形式
		 * 
		 * @param path
		 * @return
		 */
        public static String correctDirPath(String path) {
            path = standardizeFileSeparator(path);
            if (!path.endsWith("/")) {
                return path;
            }
            return path.substring(0, path.length() - 1);
        }

        /**
		 * 将单文件路径转换为标准格式,即以 / 开头的文件
		 * 
		 * @param path
		 * @return
		 */
        public static String correctFilePath(String path) {
            path = standardizeFileSeparator(path);
            if (path.charAt(0) == '/') {
                return path;
            }
            return "/" + path;
        }

        /**
		 * 将路径中的目录分隔符统一转换为 '/'
		 * @param path
		 * @return
		 */
        public static String standardizeFileSeparator(String path) {
            String fileSeparator = System.getProperty("file.separator");
            fileSeparator = fileSeparator.replaceAll("\\\\", "\\\\\\\\");
            path = path.replaceAll(fileSeparator, "/");
            return path;
        }
    }

    public static class MyFileLock {

        private final RandomAccessFile myRaf;

        private final FileLock myFl;

        private final File lockFile;

        public MyFileLock(RandomAccessFile raFile, FileLock l, File f) {
            if (raFile == null || l == null || f == null) throw new IllegalArgumentException();
            this.myRaf = raFile;
            this.myFl = l;
            this.lockFile = f;
        }

        public void release() throws IOException {
            try {
                myFl.release();
                lockFile.deleteOnExit();
            } finally {
                IOUtil.close(myRaf);
            }
        }
    }
}
