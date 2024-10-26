package com.daffodilwoods.daffodildb.utils.parser;

import java.io.ObjectInputStream;
import java.util.HashMap;
import java.net.URL;
import com.daffodilwoods.database.resource.*;
import java.io.*;

public class DaffodilClassLoader extends ClassLoader {

    HashMap mapping = new HashMap();

    HashMap obfuscateMapping = new HashMap();

    HashMap qualifiedMapping = new HashMap();

    private DaffodilClassLoader() throws DException {
        intialialiseObfuscatedHashMap();
    }

    protected Class findClass(String onlyClassName) throws ClassNotFoundException {
        Object originalFullClassName = mapping.get(onlyClassName);
        if (originalFullClassName == null) {
            throw new ClassNotFoundException(onlyClassName + " not found, converted class " + originalFullClassName);
        }
        String obfuscatedClassName = (String) obfuscateMapping.get(originalFullClassName);
        if (obfuscatedClassName == null) {
            obfuscatedClassName = (String) originalFullClassName;
        }
        return Class.forName(obfuscatedClassName);
    }

    private void intialialiseObfuscatedHashMap() throws DException {
        URL url = getClass().getResource("classes.obj");
        if (url == null) {
            throw new DException("DSE0", new Object[] { "Classes.obj file is missing in classpath." });
        }
        ObjectInputStream inputStream = null;
        try {
            inputStream = new ObjectInputStream(new BufferedInputStream(url.openStream()));
            mapping = (HashMap) inputStream.readObject();
            inputStream.close();
        } catch (ClassNotFoundException ex) {
            throw new DException("DSE0", new Object[] { ex });
        } catch (IOException ex) {
            throw new DException("DSE0", new Object[] { ex });
        }
        url = getClass().getResource("AB.obj");
        if (url != null) {
            try {
                inputStream = new ObjectInputStream(new BufferedInputStream(url.openStream()));
                obfuscateMapping = (HashMap) inputStream.readObject();
                inputStream.close();
            } catch (ClassNotFoundException ex) {
                throw new DException("DSE0", new Object[] { ex });
            } catch (IOException ex) {
                throw new DException("DSE0", new Object[] { ex });
            }
        }
        if (mapping == null) {
            mapping = new HashMap();
        }
        if (obfuscateMapping == null) {
            obfuscateMapping = new HashMap();
        }
        initialiseQualifiedMapping();
    }

    private void initialiseQualifiedMapping() {
        Object[] obj = obfuscateMapping.keySet().toArray();
        int length = obj.length;
        for (int i = 0; i < length; i++) {
            Object temp = obfuscateMapping.get(obj[i]);
            qualifiedMapping.put(temp, obj[i]);
        }
    }

    static DaffodilClassLoader daffodilClassloader;

    public static synchronized DaffodilClassLoader getInstance() throws DException {
        if (daffodilClassloader == null) daffodilClassloader = new DaffodilClassLoader();
        return daffodilClassloader;
    }

    public String getOfuscatedName(String qualifiedName) {
        String ste = (String) obfuscateMapping.get(qualifiedName);
        return ste == null ? qualifiedName : ste;
    }

    public String getQualifiedName(String ofuscatedName) {
        String ste = (String) qualifiedMapping.get(ofuscatedName);
        return ste == null ? ofuscatedName : ste;
    }

    protected Class loadClass(String name, boolean resolve) throws ClassNotFoundException {
        Class c = findLoadedClass(name);
        if (c == null) {
            synchronized (name.intern()) {
                c = findClass(name);
            }
        }
        if (resolve) {
            resolveClass(c);
        }
        return c;
    }
}
