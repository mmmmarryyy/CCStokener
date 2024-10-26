package net.sf.lavabeans.util;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.PrintWriter;
import java.io.StringWriter;
import java.net.MalformedURLException;
import java.net.URL;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

/**
 * The ResourceLoader loads for resources by trying each of the following mechanisms:
 * <ul>
 * <li>Treating the resourceID as a file name and opening it in the file system
 * <li>Loading a class resource with the specified resourceID
 * <li>Loading a system resource with the specified resourceID
 * </ul>  
 */
public class ResourceLoader {

    private static Log LOG = LogFactory.getLog(ResourceLoader.class);

    private static ResourceFinder[] resourceFinders = { new FileSystem(), new URLResource(), new ClassResource(), new SystemResource() };

    /**
	 * @return true if the specified resource can be loaded.
	 */
    public static boolean exists(Class cls, String resourceId) {
        LOG.debug("Attempting to load resource " + resourceId);
        resourceId = resourceId.replace('\\', '/');
        for (int i = 0; i < resourceFinders.length; ++i) {
            LOG.debug("\tTrying resource finder " + resourceFinders[i]);
            InputStream is = resourceFinders[i].openResource(cls, resourceId);
            if (is != null) {
                LOG.debug("\tResource found");
                try {
                    is.close();
                } catch (IOException e) {
                }
                return true;
            }
        }
        return false;
    }

    /**
	 * Opens an InputStream by trying each of the defined ResourceFinders.
	 */
    public static InputStream openInputStream(Class cls, String resourceID) {
        LOG.debug("Attempting to load resource " + resourceID);
        resourceID = resourceID.replace('\\', '/');
        for (int i = 0; i < resourceFinders.length; ++i) {
            LOG.debug("\tTrying resource finder " + resourceFinders[i]);
            InputStream is = resourceFinders[i].openResource(cls, resourceID);
            if (is != null) {
                LOG.debug("\tResource found");
                return is;
            }
        }
        throw new RuntimeException("Resource '" + resourceID + "' not found for " + cls);
    }

    public static String openTextResource(Class cls, String resourceId, String encoding) {
        try {
            BufferedReader reader = new BufferedReader(new InputStreamReader(openInputStream(cls, resourceId), encoding));
            StringWriter writer = new StringWriter();
            PrintWriter print = new PrintWriter(writer);
            String line;
            while ((line = reader.readLine()) != null) {
                print.println(line);
            }
            print.flush();
            return writer.toString();
        } catch (IOException x) {
            throw new RuntimeException(x);
        }
    }

    public interface ResourceFinder {

        /**
		 * @return null if the resource cannot be found
		 */
        public InputStream openResource(Class cls, String resourceID);
    }

    static class FileSystem implements ResourceFinder {

        public InputStream openResource(Class cls, String resourceID) {
            File file = new File(resourceID);
            if (!file.exists() || !file.isFile()) return null;
            try {
                return new FileInputStream(file);
            } catch (FileNotFoundException x) {
                LOG.warn("Unable to open existing file as resource" + x);
                return null;
            }
        }
    }

    static class ClassResource implements ResourceFinder {

        public InputStream openResource(Class cls, String resourceID) {
            return cls.getResourceAsStream(resourceID);
        }
    }

    static class SystemResource implements ResourceFinder {

        public InputStream openResource(Class cls, String resourceID) {
            return cls.getClassLoader().getResourceAsStream(resourceID);
        }
    }

    static class URLResource implements ResourceFinder {

        public InputStream openResource(Class cls, String resourceID) {
            try {
                URL url = new URL(resourceID);
                return url.openStream();
            } catch (MalformedURLException e) {
                LOG.debug("Unable to open url " + resourceID, e);
                return null;
            } catch (IOException e) {
                LOG.error("Unable to read url " + resourceID, e);
                return null;
            }
        }
    }
}
