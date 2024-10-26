package com.lonelytaste.narafms.core;

import java.io.IOException;
import java.io.InputStream;
import java.net.URL;
import java.security.AccessController;
import java.security.PrivilegedAction;
import java.util.Enumeration;
import java.util.Properties;

/**
 * <p> Title: [名称]</p>
 * <p> Description: [描述]</p>
 * <p> Created on May 12, 2009</p>
 * <p> Copyright: Copyright (c) 2009</p>
 * <p> Company: </p>
 * @author 苏红胜 - mrsuhongsheng@gmail.com
 * @version 1.0
 */
class NaraFMSFactory {

    protected static String getSystemProperty(final String key, final String def) throws SecurityException {
        return (String) AccessController.doPrivileged(new PrivilegedAction() {

            public Object run() {
                return System.getProperty(key, def);
            }
        });
    }

    protected static final Properties getConfigurationFile(ClassLoader classLoader, String fileName) {
        Properties props;
        double priority;
        URL propsUrl;
        props = null;
        priority = 0.0D;
        propsUrl = null;
        Enumeration urls = getResources(classLoader, fileName);
        if (urls == null) return null;
        try {
            do {
                if (!urls.hasMoreElements()) break;
                URL url = (URL) urls.nextElement();
                Properties newProps = getProperties(url);
                if (newProps != null) if (props == null) {
                    propsUrl = url;
                    props = newProps;
                    String priorityStr = props.getProperty("priority");
                    priority = 0.0D;
                    if (priorityStr != null) priority = Double.parseDouble(priorityStr);
                } else {
                    String newPriorityStr = newProps.getProperty("priority");
                    double newPriority = 0.0D;
                    if (newPriorityStr != null) newPriority = Double.parseDouble(newPriorityStr);
                    if (newPriority > priority) {
                        propsUrl = url;
                        props = newProps;
                        priority = newPriority;
                    }
                }
            } while (true);
        } catch (SecurityException e) {
            e.printStackTrace();
        }
        return props;
    }

    protected static Enumeration getResources(final ClassLoader loader, final String name) {
        PrivilegedAction action = new PrivilegedAction() {

            public Object run() {
                try {
                    if (loader != null) return loader.getResources(name);
                    return ClassLoader.getSystemResources(name);
                } catch (IOException e) {
                    e.printStackTrace();
                }
                return null;
            }
        };
        Object result = AccessController.doPrivileged(action);
        return (Enumeration) result;
    }

    protected static Properties getProperties(final URL url) {
        PrivilegedAction action = new PrivilegedAction() {

            public Object run() {
                Properties props = null;
                try {
                    InputStream stream;
                    stream = url.openStream();
                    props = new Properties();
                    props.load(stream);
                    stream.close();
                } catch (IOException e) {
                    e.printStackTrace();
                }
                return props;
            }
        };
        return (Properties) AccessController.doPrivileged(action);
    }
}
