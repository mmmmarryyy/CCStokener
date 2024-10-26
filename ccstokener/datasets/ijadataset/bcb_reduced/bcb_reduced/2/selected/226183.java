package org.apache.harmony.sound.utils;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;
import java.net.URL;
import java.security.AccessController;
import java.security.PrivilegedAction;
import java.util.ArrayList;
import java.util.Enumeration;
import java.util.List;
import java.util.Properties;
import org.apache.harmony.luni.util.InputStreamHelper;

public class ProviderService {

    private static Properties devices;

    static {
        devices = new Properties();
        FileInputStream fstream = AccessController.doPrivileged(new PrivilegedAction<FileInputStream>() {

            public FileInputStream run() {
                String soundPropertiesPath = System.getProperty("java.home") + File.separator + "lib" + File.separator + "sound.properties";
                try {
                    return new FileInputStream(soundPropertiesPath);
                } catch (FileNotFoundException e) {
                    return null;
                }
            }
        });
        if (fstream != null) {
            try {
                devices.load(fstream);
            } catch (IOException e) {
            }
        }
    }

    /**
     * this method return information about default device
     * 
     * @param deviceName
     * @return
     */
    public static List<String> getDefaultDeviceDescription(String deviceName) {
        List<String> defaultDevice = new ArrayList<String>();
        String str;
        int index;
        str = devices.getProperty(deviceName);
        if (str == null) {
            return defaultDevice;
        }
        index = str.indexOf("#");
        if (index == -1) {
            defaultDevice.add(str);
            defaultDevice.add(null);
        } else if (index == 0) {
            defaultDevice.add(null);
            defaultDevice.add(str.substring(index + 1));
        } else {
            defaultDevice.add(str.substring(0, index));
            defaultDevice.add(str.substring(index + 1));
        }
        return defaultDevice;
    }

    /**
     * this method return the list of providers
     * 
     * @param providerName
     * @return
     */
    public static List<?> getProviders(String providerName) {
        final String name = providerName;
        return AccessController.doPrivileged(new PrivilegedAction<List<Object>>() {

            public List<Object> run() {
                List<Object> providers = new ArrayList<Object>();
                String className = null;
                byte[] bytes;
                ClassLoader cl = ClassLoader.getSystemClassLoader();
                Enumeration<URL> urls = null;
                try {
                    urls = cl.getResources(name);
                } catch (IOException e) {
                    return providers;
                }
                for (; urls.hasMoreElements(); ) {
                    try {
                        InputStream in = urls.nextElement().openStream();
                        bytes = InputStreamHelper.readFullyAndClose(in);
                    } catch (IOException e) {
                        continue;
                    }
                    String[] astr = new String(bytes).split("\r\n");
                    for (String str : astr) {
                        className = str.trim();
                        if (!className.startsWith("#")) {
                            try {
                                providers.add(Class.forName(className.trim(), true, cl).newInstance());
                            } catch (IllegalAccessException e) {
                            } catch (InstantiationException e) {
                            } catch (ClassNotFoundException e) {
                            }
                        }
                    }
                }
                return providers;
            }
        });
    }

    public static Properties getSoundProperties() {
        return devices;
    }
}
