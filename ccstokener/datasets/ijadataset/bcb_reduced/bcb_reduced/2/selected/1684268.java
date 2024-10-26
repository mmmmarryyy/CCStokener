package xbrowser.util;

import java.io.*;
import java.net.*;
import java.util.*;
import xbrowser.XRepository;

public final class XResourceManager {

    public XResourceManager(ClassLoader class_loader) {
        myClassLoader = class_loader;
    }

    public void initResourceBundle(String res_bundle_name) {
        try {
            resourceBundle = ResourceBundle.getBundle(res_bundle_name, Locale.getDefault(), myClassLoader);
        } catch (Exception e) {
            XRepository.getLogger().error(this, "An error occured while trying to load the properties file : " + res_bundle_name);
            XRepository.getLogger().error(this, e);
        }
    }

    public URL getResourceURL(String resource_key) {
        return myClassLoader.getResource(resource_key);
    }

    public URL getResourceURL(Object owner, String key) {
        try {
            return getResourceURL(getProperty(owner, key));
        } catch (MissingResourceException e) {
            return null;
        }
    }

    public String getClassName(Object obj) {
        return getClassName(obj.getClass());
    }

    public String getClassName(Class cls) {
        String package_name = cls.getPackage().getName();
        String class_name = cls.getName().substring(package_name.length() + 1);
        return class_name;
    }

    public String getProperty(Object owner, String key) {
        return resourceBundle.getString(getClassName(owner) + "." + key);
    }

    public String getProperty(Class owner, String key) {
        return resourceBundle.getString(getClassName(owner) + "." + key);
    }

    public String getProperty(Object owner, String key, Object arg) {
        String property = getProperty(owner, key);
        Object[] arguments = { arg };
        String result = java.text.MessageFormat.format(property, arguments);
        return result;
    }

    public String getProperty(Object owner, String key, Object arg1, Object arg2) {
        String property = getProperty(owner, key);
        Object[] arguments = { arg1, arg2 };
        String result = java.text.MessageFormat.format(property, arguments);
        return result;
    }

    public Object loadObject(String class_name, String resource) throws Exception {
        return loadObject(class_name, createClassLoaderForResource(resource));
    }

    public Object loadObject(String class_name, ClassLoader cl) throws Exception {
        try {
            Class renderer_class = Class.forName(class_name, true, cl);
            Object obj = renderer_class.newInstance();
            return obj;
        } catch (Exception e) {
            XRepository.getLogger().error(this, "An error occured on loading the object: " + class_name);
            XRepository.getLogger().error(this, e);
            throw e;
        }
    }

    public ClassLoader createClassLoaderForResource(String resource) {
        if (resource == null || resource.trim().equals("")) return myClassLoader;
        LinkedList url_list = new LinkedList();
        StringTokenizer tokenizer = new StringTokenizer(resource, ",;");
        while (tokenizer.hasMoreTokens()) {
            try {
                File file = new File(tokenizer.nextToken());
                url_list.add(file.toURL());
            } catch (Exception e) {
                XRepository.getLogger().warning(this, "An error occured on creating ClassLoader from resources!");
                XRepository.getLogger().warning(this, e);
            }
        }
        URL[] urls = (URL[]) url_list.toArray(new URL[url_list.size()]);
        return (new URLClassLoader(urls, myClassLoader));
    }

    public ClassLoader createBuiltInClassLoader(String built_in_resource, ClassLoader parent) {
        if (built_in_resource == null || built_in_resource.trim().equals("")) return parent;
        LinkedList url_list = new LinkedList();
        StringTokenizer tokenizer = new StringTokenizer(built_in_resource, ",;");
        while (tokenizer.hasMoreTokens()) {
            try {
                File file = new File(tokenizer.nextToken());
                url_list.add(file.toURL());
            } catch (Exception e) {
                XRepository.getLogger().warning(this, "An error occured on creating Built-In-Resource ClassLoader!");
                XRepository.getLogger().warning(this, e);
            }
        }
        if (parent instanceof URLClassLoader) {
            URL[] parent_urls = ((URLClassLoader) parent).getURLs();
            for (int i = 0; i < parent_urls.length; i++) url_list.add(parent_urls[i]);
            URL[] urls = (URL[]) url_list.toArray(new URL[url_list.size()]);
            return (new URLClassLoader(urls, myClassLoader));
        } else {
            URL[] urls = (URL[]) url_list.toArray(new URL[url_list.size()]);
            return (new URLClassLoader(urls, parent));
        }
    }

    public ClassLoader buildClassLoaderForApplet(URL code_base, String archive) {
        LinkedList url_list = new LinkedList();
        url_list.add(code_base);
        if (archive != null) {
            StringTokenizer tokenizer = new StringTokenizer(archive, ",;");
            while (tokenizer.hasMoreTokens()) {
                try {
                    url_list.add(new URL(code_base, tokenizer.nextToken()));
                } catch (Exception e) {
                    XRepository.getLogger().warning(this, "An error occured on building ClassLoader for loading applet!");
                    XRepository.getLogger().warning(this, e);
                }
            }
        }
        URL[] urls = (URL[]) url_list.toArray(new URL[url_list.size()]);
        return (new URLClassLoader(urls));
    }

    public void checkForResourceExistence(String filename) {
        File file = new File(filename);
        if (file.exists()) return;
        URL url = myClassLoader.getResource(filename);
        if (url == null) return;
        InputStream in_stream = null;
        OutputStream out_stream = null;
        try {
            int b;
            in_stream = url.openStream();
            out_stream = new FileOutputStream(file);
            while ((b = in_stream.read()) != -1) out_stream.write(b);
        } catch (Exception ex) {
            XRepository.getLogger().warning(this, ex);
        } finally {
            try {
                in_stream.close();
                out_stream.close();
            } catch (Exception ex2) {
            }
        }
    }

    private ResourceBundle resourceBundle = null;

    private ClassLoader myClassLoader = null;
}
