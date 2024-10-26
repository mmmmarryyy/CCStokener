package net.taylor.results.pojo.util;

import java.io.File;
import java.io.IOException;
import java.lang.reflect.Constructor;
import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.lang.reflect.Modifier;
import java.net.JarURLConnection;
import java.net.URL;
import java.net.URLConnection;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Date;
import java.util.Enumeration;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.jar.JarFile;
import java.util.zip.ZipEntry;
import javax.xml.datatype.XMLGregorianCalendar;
import org.apache.commons.beanutils.BeanUtils;
import org.apache.commons.lang.StringUtils;
import sun.net.www.protocol.file.FileURLConnection;

/**
 * Provides java reflection related helper methods.
 * 
 * @author Chunyun Zhao
 * @since Dec 21, 2005
 */
public final class ReflectionUtils {

    private ReflectionUtils() {
    }

    /**
	 * Helper method to create a new instance of a class. Throws
	 * FixtureException if anything goes wrong.
	 */
    public static Object newInstance(Class clazz) {
        try {
            return clazz.newInstance();
        } catch (Exception e) {
            throw new RuntimeException("Unable to create the new instance of class:" + clazz.getName(), e);
        }
    }

    public static Class newClass(String clazzName) {
        try {
            return Thread.currentThread().getContextClassLoader().loadClass(clazzName);
        } catch (ClassNotFoundException e) {
            throw new RuntimeException("Class:" + clazzName + " can not be found.");
        }
    }

    public static Class getAttributeType(Object object, String attributeName) {
        Method method = getAttributeMethod(object.getClass(), attributeName);
        return method.getReturnType();
    }

    public static Method getAttributeMethod(Class clazz, String attributeName) {
        try {
            return clazz.getMethod("get" + StringUtils.capitalize(attributeName));
        } catch (Exception e) {
            throw new RuntimeException("Unable to find the attribute:" + attributeName + " in class: " + clazz.getName(), e);
        }
    }

    public static void setObjectAttribute(Object parent, String attributeName, Object source) {
        try {
            if (source == null) {
                Class attributeType = getAttributeType(parent, attributeName);
                if (attributeType.isPrimitive() || isPrimitiveWrapper(attributeType)) {
                    return;
                }
            }
            setProperty(parent, attributeName, source);
        } catch (Exception e) {
            throw new RuntimeException("Unable to set the attribute:" + attributeName + " on object:" + parent + " as:" + source, e);
        }
    }

    private static void setProperty(Object parent, String attributeName, Object source) throws Exception {
        Method[] methods = parent.getClass().getMethods();
        for (Method method : methods) {
            if (method.getName().startsWith("set") && method.getName().length() > 3 && StringUtils.uncapitalize(method.getName().substring(3)).equals(attributeName) && method.getParameterTypes().length == 1 && (source == null || method.getParameterTypes()[0].isAssignableFrom(source.getClass()))) {
                method.invoke(parent, source);
                return;
            }
        }
        BeanUtils.setProperty(parent, attributeName, source);
    }

    public static boolean isPrimitiveWrapper(Class type) {
        if (Boolean.class.equals(type) || Character.class.equals(type) || Byte.class.equals(type) || Short.class.equals(type) || Integer.class.equals(type) || Long.class.equals(type) || Float.class.equals(type) || Double.class.equals(type) || Void.class.equals(type)) {
            return true;
        }
        return false;
    }

    @SuppressWarnings("unchecked")
    public static void attacheObjectToParent(Object parent, String attributeName, Object source) {
        if (parent instanceof List) {
            ((List) parent).add(source);
        } else {
            setObjectAttribute(parent, attributeName, source);
        }
    }

    /**
	 * Gets the attributes of a type.
	 * 
	 * @param type
	 * @return
	 */
    public static Map<String, Class> getAttributes(Class type) {
        Map<String, Class> attributes = new HashMap<String, Class>();
        Method[] methods = type.getMethods();
        for (int i = 0; i < methods.length; i++) {
            if (methods[i].getName().startsWith("get") && !"getClass".equals(methods[i].getName()) && methods[i].getName().length() > 3 && methods[i].getParameterTypes().length == 0) {
                String attributeName = StringUtils.uncapitalize(methods[i].getName().substring(3));
                Class returnType = methods[i].getReturnType();
                if (CollectionBuilderFactory.isCollectionType(returnType)) {
                    attributeName = attributeName + "[0]" + getExtraMultiDimensionalArraySuffix(returnType);
                    returnType = CollectionBuilderFactory.getElementType(returnType);
                }
                attributes.put(attributeName, returnType);
            }
        }
        return attributes;
    }

    /**
	 * Returns the extra [0] suffix(s) for an multi-dimensional array.
	 */
    public static String getExtraMultiDimensionalArraySuffix(Class type) {
        StringBuilder sb = new StringBuilder();
        int i = 0;
        while (type.isArray()) {
            type = type.getComponentType();
            if (++i > 1) {
                sb.append("[0]");
            }
        }
        return sb.toString();
    }

    public static boolean isLowestLevelType(Class type) {
        type = CollectionBuilderFactory.getElementType(type);
        if (type.isPrimitive() || type.getName().startsWith("java.") || type.getName().startsWith("javax.") || isEnumarationPatternClass(type) || type.isEnum()) {
            return true;
        }
        return false;
    }

    public static String getDefaultValue(Class type) {
        if (isEnumarationPatternClass(type)) {
            StringBuilder sb = new StringBuilder();
            Field[] fields = type.getFields();
            for (int i = 0; i < fields.length; i++) {
                if (type.equals(fields[i].getType()) && Modifier.isFinal(fields[i].getModifiers()) && Modifier.isPublic(fields[i].getModifiers()) && Modifier.isStatic(fields[i].getModifiers())) {
                    try {
                        sb.append(fields[i].getName()).append(";");
                    } catch (IllegalArgumentException e) {
                    }
                }
            }
            return sb.toString();
        } else if (type.isEnum()) {
            StringBuilder sb = new StringBuilder();
            try {
                Object[] array = (Object[]) type.getMethod("values").invoke(null);
                for (int i = 0; i < array.length; i++) {
                    sb.append(array[i]).append(";");
                }
            } catch (Exception e) {
            }
            return sb.toString();
        } else if (Calendar.class.isAssignableFrom(type) || XMLGregorianCalendar.class.isAssignableFrom(type)) {
            return "${calendar.after(0)}";
        } else if (Date.class.isAssignableFrom(type)) {
            return "${date.after(0)}";
        } else if (Number.class.isAssignableFrom(type) || "long".equals(type.getName()) || "int".equals(type.getName()) || "short".equals(type.getName()) || "float".equals(type.getName()) || "double".equals(type.getName())) {
            return "0";
        } else if (String.class.equals(type)) {
            return "String";
        } else if (Boolean.class.equals(type) || "boolean".equals(type.getName())) {
            return "true";
        }
        return type.getName();
    }

    /**
	 * Returns true if it is an enumeration type.
	 * 
	 * @param type
	 * @return
	 */
    public static boolean isEnumarationPatternClass(Class type) {
        try {
            Constructor[] constructors = type.getConstructors();
            for (int i = 0; i < constructors.length; i++) {
                if (Modifier.isPublic(constructors[i].getModifiers())) {
                    return false;
                }
            }
            type.getMethod("fromString", new Class[] { String.class });
            return true;
        } catch (SecurityException e) {
        } catch (NoSuchMethodException e) {
        }
        return false;
    }

    /**
	 * Finds all the concrete subclasses for given class in the the SAME JAR
	 * file where the baseClass is loaded from.
	 * 
	 * @param baseClass
	 *            the base class
	 */
    public static Class[] findSubClasses(Class baseClass) {
        String packagePath = "/" + baseClass.getPackage().getName().replace('.', '/');
        URL url = baseClass.getResource(packagePath);
        if (url == null) {
            return new Class[0];
        }
        List<Class> derivedClasses = new ArrayList<Class>();
        try {
            URLConnection connection = url.openConnection();
            if (connection instanceof JarURLConnection) {
                JarFile jarFile = ((JarURLConnection) connection).getJarFile();
                Enumeration e = jarFile.entries();
                while (e.hasMoreElements()) {
                    ZipEntry entry = (ZipEntry) e.nextElement();
                    String entryName = entry.getName();
                    if (entryName.endsWith(".class")) {
                        String clazzName = entryName.substring(0, entryName.length() - 6);
                        clazzName = clazzName.replace('/', '.');
                        try {
                            Class clazz = Class.forName(clazzName);
                            if (isConcreteSubclass(baseClass, clazz)) {
                                derivedClasses.add(clazz);
                            }
                        } catch (Throwable ignoreIt) {
                        }
                    }
                }
            } else if (connection instanceof FileURLConnection) {
                File file = new File(url.getFile());
                File[] files = file.listFiles();
                for (int i = 0; i < files.length; i++) {
                    String filename = files[i].getName();
                    if (filename.endsWith(".class")) {
                        filename = filename.substring(0, filename.length() - 6);
                        String clazzname = baseClass.getPackage().getName() + "." + filename;
                        try {
                            Class clazz = Class.forName(clazzname);
                            if (isConcreteSubclass(baseClass, clazz)) {
                                derivedClasses.add(clazz);
                            }
                        } catch (Throwable ignoreIt) {
                        }
                    }
                }
            }
        } catch (IOException ignoreIt) {
        }
        return derivedClasses.toArray(new Class[derivedClasses.size()]);
    }

    /**
	 * Returns the first public method that matches method name.
	 */
    public static Method findMethodByName(Class type, String methodName) {
        Method[] methods = type.getMethods();
        for (int i = 0; i < methods.length; i++) {
            Method method = methods[i];
            if (method.getName().equals(methodName) && Modifier.isPublic(method.getModifiers())) {
                return method;
            }
        }
        return null;
    }

    private static boolean isConcreteSubclass(Class<?> baseClass, Class<?> clazz) {
        return baseClass.isAssignableFrom(clazz) && !baseClass.equals(clazz) && !clazz.isInterface() && !Modifier.isAbstract(clazz.getModifiers());
    }

    public static boolean isAbstractType(Class type) {
        return (type.isInterface() || Modifier.isAbstract(type.getModifiers())) && !type.isPrimitive();
    }
}
