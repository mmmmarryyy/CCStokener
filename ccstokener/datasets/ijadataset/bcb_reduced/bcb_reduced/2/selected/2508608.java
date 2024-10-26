package javacream.resource;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.MalformedURLException;
import java.net.URL;
import java.util.concurrent.ConcurrentHashMap;

/**
 * ResourceManager - Handles resource reading, writing and caching.
 * 
 * @author Glenn Powell
 *
 */
public class ResourceManager {

    private static ConcurrentHashMap<Class<?>, ResourceHandler<?>> resourceHandlers = new ConcurrentHashMap<Class<?>, ResourceHandler<?>>();

    private static ResourceHandler<?> defaultResourceHandler = new ByteArrayResourceHandler();

    private static ConcurrentHashMap<URL, Object> cache = new ConcurrentHashMap<URL, Object>();

    @SuppressWarnings("unchecked")
    public static <T> ResourceHandler<T> getResourceHandler(Class<T> c) {
        return (ResourceHandler<T>) resourceHandlers.get(c);
    }

    public static <T> void addResourceHandler(Class<T> c, ResourceHandler<T> resourceHandler) {
        resourceHandlers.put(c, resourceHandler);
    }

    public static void removeResourceHandler(Class<?> c) {
        resourceHandlers.remove(c);
    }

    public static ResourceHandler<?> getDefaultResourceHandler() {
        return defaultResourceHandler;
    }

    public static void setDefaultResourceHandler(ResourceHandler<?> defaultResourceHandler) {
        ResourceManager.defaultResourceHandler = defaultResourceHandler;
    }

    public static void clearCache(URL url) {
        cache.remove(url);
    }

    public static void clearCache() {
        cache.clear();
    }

    public static <T> T read(Class<T> clasz, ResourceHandler<?> resourceHandler, InputStream input) throws ResourceException {
        Object resource = resourceHandler.read(input);
        if (resource != null) {
            if (clasz.isAssignableFrom(resource.getClass())) return clasz.cast(resource);
            throw new ResourceException("Invalid class for resource.  Requested: " + clasz.getName() + ".  Returned: " + resource.getClass().getName());
        }
        throw new ResourceException("Error reading resource");
    }

    public static <T> T read(Class<T> clasz, InputStream input) throws ResourceException {
        ResourceHandler<?> resourceHandler = resourceHandlers.get(clasz);
        if (resourceHandler == null) resourceHandler = defaultResourceHandler;
        return read(clasz, resourceHandler, input);
    }

    public static <T> T read(Class<T> clasz, URL url) throws ResourceException {
        try {
            Object resource = cache.get(url);
            if (resource == null) {
                InputStream stream = url.openStream();
                if (stream != null) {
                    T resourceT = read(clasz, stream);
                    cache.put(url, resourceT);
                    return resourceT;
                }
            }
            if (resource != null) {
                if (clasz.isAssignableFrom(resource.getClass())) return clasz.cast(resource);
                throw new ResourceException("Invalid class for resource: '" + url + "'.  Requested: " + clasz.getName() + ".  Returned: " + resource.getClass().getName());
            }
        } catch (IOException e) {
            throw new ResourceException(e);
        }
        throw new ResourceException("Error reading resource: " + url);
    }

    public static <T> T read(Class<T> clasz, String path) throws ResourceException {
        URL url = getURL(path);
        if (url != null) {
            return read(clasz, url);
        } else {
            throw new ResourceException("Error reading resource, invalid path: " + path);
        }
    }

    public static <T> void write(Class<T> clasz, T resource, ResourceHandler<T> resourceHandler, OutputStream output) throws ResourceException {
        resourceHandler.write(resource, output);
    }

    public static <T> void write(Class<T> clasz, T resource, OutputStream output) throws ResourceException {
        ResourceHandler<T> resourceHandler = getResourceHandler(clasz);
        if (resourceHandler != null) write(clasz, resource, resourceHandler, output);
    }

    @SuppressWarnings("unchecked")
    public static <T> void write(T resource, OutputStream output) throws ResourceException {
        write((Class<T>) resource.getClass(), resource, output);
    }

    @SuppressWarnings("unchecked")
    public static <T> void write(T resource, URL url) throws ResourceException {
        try {
            write((Class<T>) resource.getClass(), resource, url.openConnection().getOutputStream());
            cache.put(url, resource);
        } catch (IOException e) {
            throw new ResourceException(e);
        }
    }

    public static <T> void write(T resource, String path) throws ResourceException {
        URL url = getURL(path);
        if (url != null) {
            write(resource, url);
        } else {
            throw new ResourceException("Error writing resource, invalid path: " + path);
        }
    }

    public static InputStream getInputStream(String path) throws ResourceException {
        URL url = getURL(path);
        if (url != null) {
            try {
                return url.openConnection().getInputStream();
            } catch (IOException e) {
                throw new ResourceException(e);
            }
        } else {
            throw new ResourceException("Error obtaining resource, invalid path: " + path);
        }
    }

    public static OutputStream getOutputStream(String path) throws ResourceException {
        URL url = getURL(path);
        if (url != null) {
            try {
                return url.openConnection().getOutputStream();
            } catch (IOException e) {
                throw new ResourceException(e);
            }
        } else {
            throw new ResourceException("Error obtaining resource, invalid path: " + path);
        }
    }

    public static URL getURL(String path) throws ResourceException {
        URL url = Object.class.getResource(path);
        if (url == null) {
            File file = new File(path);
            if (file.exists()) {
                try {
                    url = file.toURI().toURL();
                } catch (MalformedURLException e) {
                    throw new ResourceException(e);
                }
            } else {
                try {
                    url = new URL(path);
                } catch (MalformedURLException e) {
                    throw new ResourceException(e);
                }
            }
        }
        return url;
    }
}
