package org.apache.harmony.x.print.util;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.UnsupportedEncodingException;
import java.net.URL;
import java.security.AccessController;
import java.security.PrivilegedAction;
import java.util.ArrayList;
import java.util.Enumeration;
import java.util.List;

public class FactoryLocator {

    List factoryClasses;

    public FactoryLocator() {
        super();
        factoryClasses = new ArrayList();
    }

    public List getFactoryClasses() {
        return factoryClasses;
    }

    public void lookupAllFactories() throws IOException {
        Enumeration setOfFactories = null;
        ClassLoader classLoader = null;
        InputStream inputStream = null;
        classLoader = (ClassLoader) AccessController.doPrivileged(new PrivilegedAction() {

            public Object run() {
                ClassLoader cl = Thread.currentThread().getContextClassLoader();
                if (cl == null) {
                    cl = ClassLoader.getSystemClassLoader();
                }
                return cl;
            }
        });
        if (classLoader == null) {
            return;
        }
        try {
            setOfFactories = classLoader.getResources("META-INF/services/javax.print.StreamPrintServiceFactory");
        } catch (IOException e) {
            e.printStackTrace();
            throw new IOException("IOException during resource finding");
        }
        try {
            while (setOfFactories.hasMoreElements()) {
                URL url = (URL) setOfFactories.nextElement();
                inputStream = url.openStream();
                getFactoryClasses(inputStream);
            }
        } catch (IOException e1) {
            e1.printStackTrace();
            throw new IOException("IOException during resource reading");
        }
    }

    private void getFactoryClasses(InputStream is) throws IOException {
        BufferedReader factoryNameReader;
        Class factoryClass;
        String name = null;
        try {
            factoryNameReader = new BufferedReader(new InputStreamReader(is, "UTF-8"));
        } catch (UnsupportedEncodingException uee) {
            factoryNameReader = new BufferedReader(new InputStreamReader(is));
        }
        if (factoryNameReader == null) {
            return;
        }
        try {
            while (true) {
                name = factoryNameReader.readLine();
                if (name == null) {
                    return;
                }
                if (name.length() > 0 && name.charAt(0) != '#') {
                    factoryClass = Class.forName(name);
                    factoryClasses.add(factoryClass.newInstance());
                }
            }
        } catch (IOException e) {
            throw new IOException("IOException during reading file");
        } catch (ClassNotFoundException e) {
            throw new IOException("Class" + name + " is not found");
        } catch (InstantiationException e) {
            throw new IOException("Bad instantiation of class" + name);
        } catch (IllegalAccessException e) {
            throw new IOException("Illegal access for class" + name);
        }
    }
}
