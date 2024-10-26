package org.fpse.forum;

import java.io.IOException;
import java.io.InputStream;
import java.lang.reflect.Constructor;
import java.lang.reflect.InvocationTargetException;
import java.net.URL;
import java.sql.SQLException;
import java.util.Enumeration;
import java.util.HashMap;
import java.util.Map;
import java.util.Set;
import java.util.jar.Attributes;
import java.util.jar.Manifest;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.fpse.config.AppConfiguration;
import org.fpse.schedular.TaskSchedular;
import org.fpse.schedular.refresh.DefaultEventHandler;
import org.fpse.schedular.refresh.impl.PostCounter;
import org.fpse.store.Storage;
import org.fpse.store.Store;

/**
 * Created on Dec 13, 2006 5:39:04 PM by Ajay
 */
public class ForumFactory {

    private static final Log LOG = LogFactory.getLog(ForumFactory.class);

    private static final Map<String, Forum> FORUMS = new HashMap<String, Forum>();

    private static final DefaultEventHandler DEFAULT_EVENT_HANDLER = new DefaultEventHandler();

    public static Forum createOrFindForum(String name) {
        Forum forum = null;
        synchronized (FORUMS) {
            forum = FORUMS.get(name);
            if (null == forum) {
                forum = createForum0(name);
                FORUMS.put(name, forum);
            }
        }
        return forum;
    }

    @SuppressWarnings("unchecked")
    private static Forum createForum0(String name) throws IllegalArgumentException {
        LOG.info("Creating forum with name: " + name);
        LOG.debug("Locating manifest file.");
        Manifest manifest;
        try {
            manifest = find(name);
        } catch (IOException e) {
            throw new IllegalStateException("Unable to locate the manifest file.", e);
        }
        if (null == manifest) {
            LOG.error("No forum manifest file found with name: " + name);
            throw new IllegalArgumentException("No forum found with name: " + name);
        }
        Attributes attributes = manifest.getMainAttributes();
        String className = attributes.getValue("Config-Class");
        if (null == className) throw new IllegalArgumentException("The config class name is null.");
        Forum forum;
        try {
            Class clazz = Thread.currentThread().getContextClassLoader().loadClass(className);
            if (!ForumConfiguration.class.isAssignableFrom(clazz)) {
                throw new IllegalArgumentException("Bad config class '" + className + "' doesn't follow the interface.");
            }
            Constructor constructor = clazz.getConstructor(new Class[] { String.class });
            ForumConfiguration configuration = (ForumConfiguration) constructor.newInstance(new Object[] { name });
            className = attributes.getValue("Class");
            if (null == className) {
                throw new IllegalArgumentException("The class name is null.");
            }
            clazz = Thread.currentThread().getContextClassLoader().loadClass(className);
            if (!(Forum.class.equals(clazz) || Forum.class.isAssignableFrom(clazz))) {
                throw new IllegalArgumentException("The forum class '" + className + "' doesn't follow the interface.");
            }
            constructor = clazz.getConstructor(new Class[] { String.class, String.class, ForumConfiguration.class });
            Attributes locationAttribute = manifest.getAttributes("URL");
            if (null == locationAttribute) {
                throw new IllegalArgumentException("No URL section found in the manifest.");
            }
            String location = locationAttribute.getValue("Forum");
            forum = (Forum) constructor.newInstance(new Object[] { name, location, configuration });
        } catch (SecurityException e) {
            throw new IllegalStateException(e);
        } catch (ClassNotFoundException e) {
            throw new IllegalStateException(e);
        } catch (InstantiationException e) {
            throw new IllegalStateException(e);
        } catch (IllegalAccessException e) {
            throw new IllegalStateException(e);
        } catch (NoSuchMethodException e) {
            throw new IllegalStateException(e);
        } catch (InvocationTargetException e) {
            throw new IllegalStateException(e);
        }
        LOG.debug("Forum class has been loaded.");
        addConfiguration(manifest, forum);
        DefaultEventHandler handler = DEFAULT_EVENT_HANDLER;
        forum.addListener(handler);
        if (!forum.isDeterministicForum()) {
            PostCounter counter = new PostCounter(forum);
            forum.addListener(counter);
            TaskSchedular.getInstance().add(counter);
        }
        try {
            handler.startSingletonRefreshers(forum);
        } catch (SecurityException e) {
            throw new IllegalStateException(e);
        } catch (ClassNotFoundException e) {
            throw new IllegalStateException(e);
        } catch (InstantiationException e) {
            throw new IllegalStateException(e);
        } catch (IllegalAccessException e) {
            throw new IllegalStateException(e);
        } catch (NoSuchMethodException e) {
            throw new IllegalStateException(e);
        } catch (InvocationTargetException e) {
            throw new IllegalStateException(e);
        }
        try {
            ForumFactory.readFromDatabase(forum);
        } catch (SQLException e) {
            throw new IllegalStateException("Unable to load the forum information.", e);
        }
        LOG.info("Forum created and loaded.");
        return forum;
    }

    private static Manifest find(String name) throws IOException {
        ClassLoader loader = Thread.currentThread().getContextClassLoader();
        Enumeration<URL> e = loader.getResources("org/fpse/forum/Forum");
        while (e.hasMoreElements()) {
            URL url = e.nextElement();
            InputStream in = null;
            try {
                in = url.openStream();
                Manifest manifest = new Manifest(in);
                Attributes attributes = manifest.getMainAttributes();
                String value = attributes.getValue("Id");
                if (name.equals(value)) {
                    LOG.debug("Found the manifest file: " + url);
                    return manifest;
                }
            } finally {
                if (in != null) {
                    try {
                        in.close();
                    } catch (IOException _) {
                    }
                }
            }
        }
        return null;
    }

    private static void readFromDatabase(Forum forum) throws SQLException {
        LOG.debug("Reading topic areas from database");
        Store store = Storage.getInstace().getStore();
        Set<TopicArea> tas = store.load(forum);
        forum.addTopicAreas(tas);
    }

    private static void addConfiguration(Manifest manifest, Forum forum) {
        Attributes attributes = manifest.getAttributes("URL");
        if (null != attributes) {
            String name = AppConfiguration.escape(forum.getName());
            for (Object o : attributes.keySet()) {
                String key = String.valueOf(o);
                String value = attributes.getValue(key);
                key = "forums.forum[@id='" + name + "'].urls." + key;
                AppConfiguration.getInstance().addToBootConfiguration(key, value);
            }
        }
        attributes = manifest.getAttributes("Behavior");
        if (null != attributes) {
            String name = AppConfiguration.escape(forum.getName());
            for (Object o : attributes.keySet()) {
                String key = String.valueOf(o);
                String value = attributes.getValue(key);
                key = "forums.forum[@id='" + name + "']." + key;
                AppConfiguration.getInstance().addToBootConfiguration(key, value);
            }
        }
        attributes = manifest.getAttributes("Refreshers");
        if (null != attributes) {
            String name = AppConfiguration.escape(forum.getName());
            for (Object o : attributes.keySet()) {
                String key = String.valueOf(o);
                String value = attributes.getValue(key);
                key = "forums.forum[@id='" + name + "'].Refreshers." + key;
                AppConfiguration.getInstance().addToBootConfiguration(key, value);
            }
        }
    }
}
