package cn.lzh.common.util;

import java.io.File;
import java.io.FileFilter;
import java.io.IOException;
import java.lang.reflect.Method;
import java.net.JarURLConnection;
import java.net.URL;
import java.net.URLDecoder;
import java.util.Enumeration;
import java.util.HashMap;
import java.util.LinkedHashSet;
import java.util.Map;
import java.util.Set;
import java.util.jar.JarEntry;
import java.util.jar.JarFile;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

/**
 * �෴�乤��
 * @author <a href="mailto:sealinglip@gmail.com">Sealinglip</a>
 * 2007-3-2
 */
public class ClassUtils {

    private static final Log log = LogFactory.getLog(ClassUtils.class);

    private static Map<Class<?>, Class<?>> primitiveAndWrapperCls;

    static {
        primitiveAndWrapperCls = new HashMap<Class<?>, Class<?>>();
        primitiveAndWrapperCls.put(boolean.class, Boolean.class);
        primitiveAndWrapperCls.put(byte.class, Byte.class);
        primitiveAndWrapperCls.put(char.class, Character.class);
        primitiveAndWrapperCls.put(short.class, Short.class);
        primitiveAndWrapperCls.put(int.class, Integer.class);
        primitiveAndWrapperCls.put(long.class, Long.class);
        primitiveAndWrapperCls.put(float.class, Float.class);
        primitiveAndWrapperCls.put(double.class, Double.class);
    }

    /**
	 * ����ĳ�����з�����Ͳ������ͺ�ָ������ƥ��Ĺ��з����������?��ĺͼ̳еĹ��з�����
	 * @param clazz
	 * @param methodName
	 * @param argTypes
	 * @return
	 * @throws SecurityException
	 * @throws NoSuchMethodException
	 */
    public static Method getPublicMethod(Class<?> clazz, String methodName, Class<?>... argTypes) throws SecurityException, NoSuchMethodException {
        try {
            return clazz.getMethod(methodName, argTypes);
        } catch (NoSuchMethodException e) {
            if (argTypes.length > 0) {
                Method m = findMethod(clazz.getMethods(), methodName, argTypes);
                if (m == null) {
                    throw e;
                } else {
                    return m;
                }
            } else {
                throw e;
            }
        }
    }

    /**
	 * ����ĳ�����з�����Ͳ������ͺ�ָ������ƥ��ķ����������?��ĺ͸����ж�������з�����
	 * @param clazz
	 * @param methodName
	 * @param argTypes
	 * @return
	 * @throws SecurityException
	 * @throws NoSuchMethodException
	 */
    public static Method getAnyMethod(Class<?> clazz, String methodName, Class<?>... argTypes) throws SecurityException, NoSuchMethodException {
        try {
            return clazz.getMethod(methodName, argTypes);
        } catch (NoSuchMethodException e) {
            do {
                Method m = findMethod(clazz.getDeclaredMethods(), methodName, argTypes);
                if (m != null) {
                    return m;
                }
                if (clazz.getSuperclass().equals(Object.class)) {
                    throw e;
                } else {
                    clazz = clazz.getSuperclass();
                }
            } while (true);
        }
    }

    /**
	 * �ڷ���������Ѱ��ƥ��ķ���
	 * @param methods
	 * @param methodName
	 * @param argTypes
	 * @return
	 */
    private static Method findMethod(Method[] methods, String methodName, Class<?>... argTypes) {
        for (Method method : methods) {
            if (!method.getName().equals(methodName)) {
                continue;
            }
            Class<?>[] parameterTypes = method.getParameterTypes();
            int argTypeLength = argTypes == null ? 0 : argTypes.length;
            if (parameterTypes.length != argTypeLength) {
                continue;
            }
            if (parameterTypes.length > 0) {
                for (int i = 0, in = parameterTypes.length; i < in; i++) {
                    if (!compatible(parameterTypes[i], argTypes[i])) {
                        continue;
                    }
                }
            }
            return method;
        }
        return null;
    }

    /**
	 * �ж����������Ƿ�ƥ��
	 * @param parameterType
	 * @param argType
	 * @return
	 */
    private static boolean compatible(Class<?> parameterType, Class<?> argType) {
        if (argType == null) {
            return !parameterType.isPrimitive();
        } else {
            if (parameterType == argType || parameterType.isAssignableFrom(argType)) {
                return true;
            }
            if (parameterType.isPrimitive()) {
                return argType.equals(primitiveAndWrapperCls.get(parameterType));
            }
            if (argType.isPrimitive()) {
                return parameterType.equals(primitiveAndWrapperCls.get(argType));
            }
        }
        return false;
    }

    /**
	 * �Ӱ�package�л�ȡ���е�Class
	 * 
	 * @param packageName
	 * @return
	 * @throws IOException 
	 */
    public static Set<Class<?>> getClasses(String packageName) throws IOException {
        Set<Class<?>> classes = new LinkedHashSet<Class<?>>();
        boolean recursive = true;
        String packageDirName = packageName.replace('.', '/');
        Enumeration<URL> dirs;
        dirs = Thread.currentThread().getContextClassLoader().getResources(packageDirName);
        while (dirs.hasMoreElements()) {
            URL url = dirs.nextElement();
            String protocol = url.getProtocol();
            if ("file".equals(protocol)) {
                String filePath = URLDecoder.decode(url.getFile(), "UTF-8");
                findAndAddClassesInPackageByFile(packageName, filePath, recursive, classes);
            } else if ("jar".equals(protocol)) {
                JarFile jar;
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
                                    log.error("����û��Զ�����ͼ����� �Ҳ��������.class�ļ�", e);
                                }
                            }
                        }
                    }
                }
            }
        }
        return classes;
    }

    /**
	 * ���ļ�����ʽ����ȡ���µ�����Class
	 * 
	 * @param packageName
	 * @param packagePath
	 * @param recursive
	 * @param classes
	 */
    private static void findAndAddClassesInPackageByFile(String packageName, String packagePath, final boolean recursive, Set<Class<?>> classes) {
        File dir = new File(packagePath);
        if (!dir.exists() || !dir.isDirectory()) {
            log.warn("�û�������� " + packageName + " ��û���κ��ļ�");
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
                    log.error("����û��Զ�����ͼ����� �Ҳ��������.class�ļ�", e);
                }
            }
        }
    }
}
