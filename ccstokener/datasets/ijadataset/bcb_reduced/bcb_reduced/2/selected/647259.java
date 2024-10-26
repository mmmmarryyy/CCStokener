package net.sourceforge.NetProcessor.Core;

import java.*;
import java.io.*;
import java.lang.*;
import java.lang.reflect.*;
import java.net.*;
import java.util.jar.*;

public class JarClassLoader extends URLClassLoader {

    public JarClassLoader(URL url) {
        super(new URL[] { url });
        mUrl = url;
    }

    public String getMainClassName() {
        String name = null;
        try {
            URL url = new URL("jar", "", mUrl + "!/");
            JarURLConnection conn = (JarURLConnection) url.openConnection();
            Attributes attr = conn.getMainAttributes();
            if (attr != null) {
                name = attr.getValue(Attributes.Name.MAIN_CLASS);
            }
        } catch (Exception ex) {
        }
        return name;
    }

    public static IDataAccess createDataAccessFromResource(String jar, String url, String username, String password) {
        return (IDataAccess) createObjectFromResource(jar, "createInstance", IDataAccess.class, new Object[] { url, username, password });
    }

    public static Object createObjectFromResource(String jar, String creatorName, Class creatorType, Object[] parameters) {
        Object result = null;
        try {
            JarClassLoader loader = new JarClassLoader(new URL(jar));
            String mainClass = loader.getMainClassName();
            if (mainClass != null) {
                result = loader.createObjectFromClass(mainClass, creatorName, creatorType, parameters);
            }
        } catch (Exception ex) {
        }
        return result;
    }

    @SuppressWarnings("unchecked")
    private Object createObjectFromClass(String className, String creatorName, Class creatorType, Object[] parameters) {
        Object result = null;
        try {
            Class c = loadClass(className);
            Class<?>[] parameterClasses = new Class<?>[parameters.length];
            Method method = null;
            int modifiers = 0, i;
            for (i = 0; i < parameters.length; i++) {
                parameterClasses[i] = parameters[i].getClass();
            }
            method = c.getMethod(creatorName, parameterClasses);
            method.setAccessible(true);
            modifiers = method.getModifiers();
            if ((method.getReturnType() == creatorType) && Modifier.isStatic(modifiers) && Modifier.isPublic(modifiers)) {
                result = method.invoke(null, parameters);
            }
        } catch (Exception ex) {
        }
        return result;
    }

    private URL mUrl;
}
