package com.techstar.framework;

import java.io.IOException;
import java.io.InputStream;
import java.net.MalformedURLException;
import java.net.URL;
import java.util.Properties;

/**
 *用来加载类，ｃｌａｓｓｐａｔｈ下的资源文件，属性文件等。
 *getExtendResource(String relativePath)方法，可以使用../符号来加载classpath外部的资源。
 */
public class ClassLoaderHelper {

    /**
     *加载Java类。 使用全限定类名
     *@paramclassName
     *@return
     */
    public static Class loadClass(String className) {
        try {
            return getClassLoader().loadClass(className);
        } catch (ClassNotFoundException e) {
            throw new RuntimeException("class not found '" + className + "'", e);
        }
    }

    /**
       *得到类加载器
       *@return
       */
    public static ClassLoader getClassLoader() {
        return ClassLoaderHelper.class.getClassLoader();
    }

    /**
       *提供相对于classpath的资源路径，返回文件的输入流
       *@paramrelativePath必须传递资源的相对路径。是相对于classpath的路径。如果需要查找classpath外部的资源，需要使用../来查找
       *@return 文件输入流
     *@throwsIOException
     *@throwsMalformedURLException
       */
    public static InputStream getStream(String relativePath) throws MalformedURLException, IOException {
        if (!relativePath.startsWith("../")) {
            return getClassLoader().getResourceAsStream(relativePath);
        } else {
            return ClassLoaderHelper.getStreamByExtendResource(relativePath);
        }
    }

    /**
       *
       *@paramurl
       *@return
       *@throwsIOException
       */
    public static InputStream getStream(URL url) throws IOException {
        if (url != null) {
            return url.openStream();
        } else {
            return null;
        }
    }

    /**
       *
       *@paramrelativePath必须传递资源的相对路径。是相对于classpath的路径。如果需要查找classpath外部的资源，需要使用../来查找
       *@return
       *@throwsMalformedURLException
       *@throwsIOException
       */
    public static InputStream getStreamByExtendResource(String relativePath) throws MalformedURLException, IOException {
        return ClassLoaderHelper.getStream(ClassLoaderHelper.getExtendResource(relativePath));
    }

    /**
       *提供相对于classpath的资源路径，返回属性对象，它是一个散列表
       *@paramresource
       *@return
       */
    public static Properties getProperties(String resource) {
        Properties properties = new Properties();
        try {
            properties.load(getStream(resource));
        } catch (IOException e) {
            throw new RuntimeException("couldn't load properties file '" + resource + "'", e);
        }
        return properties;
    }

    /**
       *得到本Class所在的ClassLoader的Classpat的绝对路径。
       *URL形式的
       *@return
       */
    public static String getAbsolutePathOfClassLoaderClassPath() {
        URL url = ClassLoaderHelper.getClassLoader().getResource("com/techstar/framework/ClassLoaderHelper.class");
        if (url != null) return url.toString(); else {
            url = ClassLoader.getSystemResource("com/techstar/jhop/cms/engine/helper/ClassLoaderHelper.class");
            return url.toString();
        }
    }

    /**
     *
     *@paramrelativePath 必须传递资源的相对路径。是相对于classpath的路径。如果需要查找classpath外部的资源，需要使用../来查找
     *@return资源的绝对URL
   *@throwsMalformedURLException
     */
    public static URL getExtendResource(String relativePath) throws MalformedURLException {
        if (!relativePath.startsWith("../")) {
            return ClassLoaderHelper.getResource(relativePath);
        }
        String classPathAbsolutePath = ClassLoaderHelper.getAbsolutePathOfClassLoaderClassPath();
        if (relativePath.substring(0, 1).equals("/")) {
            relativePath = relativePath.substring(1);
        }
        String wildcardString = relativePath.substring(0, relativePath.lastIndexOf("../") + 3);
        relativePath = relativePath.substring(relativePath.lastIndexOf("../") + 3);
        int containSum = ClassLoaderHelper.containSum(wildcardString, "../");
        classPathAbsolutePath = ClassLoaderHelper.cutLastString(classPathAbsolutePath, "/", containSum);
        String resourceAbsolutePath = classPathAbsolutePath + relativePath;
        URL resourceAbsoluteURL = new URL(resourceAbsolutePath);
        return resourceAbsoluteURL;
    }

    /**
    *
     *@paramsource
     *@paramdest
     *@return
     */
    private static int containSum(String source, String dest) {
        int containSum = 0;
        int destLength = dest.length();
        while (source.indexOf(dest) != -1) {
            containSum = containSum + 1;
            source = source.substring(destLength);
        }
        return containSum;
    }

    /**
     *
     *@paramsource
     *@paramdest
     *@paramnum
     *@return
     */
    private static String cutLastString(String source, String dest, int num) {
        if (source.startsWith("jar:")) {
            source = source.substring(4, source.length());
            source = source.substring(0, source.lastIndexOf(".jar!"));
        }
        for (int i = 0; i < num; i++) {
            source = source.substring(0, source.lastIndexOf(dest, source.length() - 2) + 1);
        }
        return source;
    }

    /**
     *
     *@paramresource
     *@return
     */
    public static URL getResource(String resource) {
        return ClassLoaderHelper.getClassLoader().getResource(resource);
    }

    public static String getVmPath(String relativePath) {
        try {
            URL url = getExtendResource(relativePath);
            if (url == null) return "";
            String path = url.toString();
            if (path.startsWith("file:/")) {
                path = path.substring(6, path.length());
            } else if (path.startsWith("jar:file:/")) {
                path = path.substring(10, path.length());
            }
            return path;
        } catch (MalformedURLException e) {
            e.printStackTrace();
            return "";
        }
    }

    /**
   *@paramargs
   *@throwsMalformedURLException
   */
    public static void main(String[] args) throws MalformedURLException {
        ClassLoaderHelper.getExtendResource("../../vm/action.vm");
    }
}
