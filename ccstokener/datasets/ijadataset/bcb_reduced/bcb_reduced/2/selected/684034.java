package jahuwaldt.maptools;

import java.io.*;
import java.net.URL;
import java.util.ArrayList;
import java.util.Set;
import java.util.HashSet;
import java.util.Enumeration;
import java.util.Properties;
import javax.swing.JOptionPane;

/**
*  
*  This class returns a specific LayerReader object that can read in the specified file.
*  This class implements a pluggable architecture.  A new LayerReader class can be added
*  by simply creating a subclass of LayerReader, creating a LayerReader.properties file
*  that refers to it, and putting that properties file somewhere in the Java search
*  path.
*
*  <p>  Modified by:  Joseph A. Huwaldt  </p>
*
*  @author  Joseph A. Huwaldt   Date:  April 20, 2000
*  @version June 10, 2004
**/
public class LayerReaderFactory {

    private static final boolean DEBUG = true;

    /**
	* All classloader resources with this name ("datareader.properties") are loaded
	* as .properties definitions and merged together to create a global list of
	* reader handler mappings.
	**/
    private static final String kMappingResName = "LayerReader.properties";

    private static final LayerReader[] kAllReaders;

    static {
        LayerReader[] temp = null;
        try {
            temp = loadResourceList(kMappingResName, getClassLoader());
        } catch (Exception e) {
            if (DEBUG) {
                System.out.println("could not load all" + " [" + kMappingResName + "] mappings:");
                e.printStackTrace(System.out);
            }
        }
        kAllReaders = temp;
    }

    private LayerReaderFactory() {
    }

    /**
	*  Method that attempts to find a LayerReader object that might be able
	*  to read the specified file.  If an appropriate reader can not be found
	*  an IOException is thrown.  If the user cancels the selection dialog
	*  for multiple readers, null is returned.
	**/
    public static LayerReader getReader(File theFile) throws IOException {
        if (!theFile.exists()) throw new IOException("Could not find the file " + theFile.getName() + ".");
        LayerReader[] allReaders = getAllReaders();
        if (allReaders == null || allReaders.length < 1) throw new IOException("There are no readers available at all.");
        ArrayList list = new ArrayList();
        try {
            int numReaders = allReaders.length;
            for (int i = 0; i < numReaders; ++i) {
                LayerReader reader = allReaders[i];
                int canReadFile = reader.canReadData(theFile);
                if (canReadFile == LayerReader.YES) return reader;
                if (canReadFile == LayerReader.MAYBE) list.add(reader);
            }
        } catch (Exception e) {
            e.printStackTrace();
            throw new IOException("An error occured trying to determine the file's type.");
        }
        if (list.size() < 1) throw new IOException("Can not determine the file's type.");
        LayerReader selectedReader = null;
        if (list.size() == 1) selectedReader = (LayerReader) list.get(0); else {
            Object[] possibleValues = list.toArray();
            selectedReader = (LayerReader) JOptionPane.showInputDialog(null, "Choose a format for the file: " + theFile.getName(), "Select Format", JOptionPane.INFORMATION_MESSAGE, null, possibleValues, possibleValues[0]);
        }
        return selectedReader;
    }

    /**
	*  Method that returns a list of all the DataReaader objects found by this
	*  factory during static initialization.
	*  
	*  @return An array of LayerReader objects [can be null if static init failed]
	**/
    public static LayerReader[] getAllReaders() {
        return kAllReaders;
    }

    private static LayerReader[] loadResourceList(final String resourceName, ClassLoader loader) {
        if (loader == null) loader = ClassLoader.getSystemClassLoader();
        final Set result = new HashSet();
        try {
            final Enumeration resources = loader.getResources(resourceName);
            if (resources != null) {
                while (resources.hasMoreElements()) {
                    final URL url = (URL) resources.nextElement();
                    final Properties mapping;
                    InputStream urlIn = null;
                    try {
                        urlIn = url.openStream();
                        mapping = new Properties();
                        mapping.load(urlIn);
                    } catch (IOException ioe) {
                        continue;
                    } finally {
                        if (urlIn != null) try {
                            urlIn.close();
                        } catch (Exception ignore) {
                        }
                    }
                    for (Enumeration keys = mapping.propertyNames(); keys.hasMoreElements(); ) {
                        final String format = (String) keys.nextElement();
                        final String implClassName = mapping.getProperty(format);
                        result.add(loadResource(implClassName, loader));
                    }
                }
            }
        } catch (IOException ignore) {
        }
        return (LayerReader[]) result.toArray(new LayerReader[result.size()]);
    }

    private static Object loadResource(final String className, final ClassLoader loader) {
        if (className == null) throw new IllegalArgumentException("null input: className");
        if (loader == null) throw new IllegalArgumentException("null input: loader");
        final Class cls;
        final Object reader;
        try {
            cls = Class.forName(className, true, loader);
            reader = cls.newInstance();
        } catch (Exception e) {
            throw new RuntimeException("could not load and instantiate" + " [" + className + "]: " + e.getMessage());
        }
        if (!(reader instanceof LayerReader)) throw new RuntimeException("not a jahuwaldt.maptools.LayerReader" + " implementation: " + cls.getName());
        return reader;
    }

    /**
     * This method decides on which classloader is to be used by all resource/class
     * loading in this class. At the very least you should use the current thread's
     * context loader. A better strategy would be to use techniques shown in
     * http://www.javaworld.com/javaworld/javaqa/2003-06/01-qa-0606-load.html 
     */
    private static ClassLoader getClassLoader() {
        return Thread.currentThread().getContextClassLoader();
    }
}
