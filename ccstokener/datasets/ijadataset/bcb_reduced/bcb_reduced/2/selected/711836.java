package geomss.geom.reader;

import java.io.*;
import java.net.URL;
import java.util.Arrays;
import java.util.Enumeration;
import java.util.Properties;
import javax.swing.JOptionPane;
import javolution.util.FastTable;
import javolution.util.FastSet;
import javolution.util.FastComparator;

/**
*  This class returns a specific {@link PointGeomReader} object that can read in the specified file.
*  This class implements a pluggable architecture.  A new {@link PointGeomReader} class can be added
*  by simply creating a subclass of <code>PointGeomReader</code>, creating a "pointGeomReader.properties" file
*  that refers to it, and putting that properties file somewhere in the Java search path.
*
*  <p>  Modified by:  Joseph A. Huwaldt    </p>
*
*  @author    Joseph A. Huwaldt    Date:  April 14, 2000
*  @version   June 18, 2010
**/
public final class PointGeomReaderFactory {

    /**
	* All classloader resources with this name ("pointGeomReader.properties") are loaded
	* as .properties definitions and merged together to create a global list of
	* reader handler mappings.
	**/
    private static final String MAPPING_RES_NAME = "pointGeomReader.properties";

    private static final PointGeomReader[] _allReaders;

    static {
        PointGeomReader[] temp = null;
        try {
            temp = loadResourceList(MAPPING_RES_NAME, getClassLoader());
        } catch (Exception e) {
            System.out.println("could not load all [" + MAPPING_RES_NAME + "] mappings:");
            e.printStackTrace();
        }
        _allReaders = temp;
    }

    private PointGeomReaderFactory() {
    }

    /**
	*  Method that attempts to find a {@link PointGeomReader} object that might be able
	*  to read the data in the specified input stream.
	*
	*  @param inputStream An input stream to the file that a reader is to be returned for.
	*  @param pathName    The path or file name for the file that a reader is to be returned for.
	*  @return A reader that is appropriate for the specified file, or <code>null</code> if the
	*          user cancels the multiple reader selection dialog.
	*  @throws IOException if an appropraite reader for the file could not be found.
	**/
    public static PointGeomReader getReader(InputStream inputStream, String pathName) throws IOException {
        PointGeomReader[] allReaders = getAllReaders();
        if (allReaders == null || allReaders.length < 1) throw new IOException("There are no readers available for this file.");
        FastTable<PointGeomReader> list = FastTable.newInstance();
        BufferedInputStream input = null;
        try {
            input = new BufferedInputStream(inputStream);
            String name = new File(pathName).getName();
            for (PointGeomReader reader : allReaders) {
                input.mark(1024000);
                int canReadFile = reader.canReadData(name, input);
                if (canReadFile == PointGeomReader.YES) {
                    FastTable.recycle(list);
                    return reader;
                }
                if (canReadFile == PointGeomReader.MAYBE) list.add(reader);
                input.reset();
            }
        } catch (Exception e) {
            FastTable.recycle(list);
            e.printStackTrace();
            throw new IOException("An error occured trying to determine the file's type.");
        }
        if (list.size() < 1) {
            FastTable.recycle(list);
            throw new IOException("Can not determine the file's type.");
        }
        PointGeomReader selectedReader = null;
        if (list.size() == 1) selectedReader = list.get(0); else {
            PointGeomReader[] possibleValues = list.toArray(new PointGeomReader[list.size()]);
            selectedReader = (PointGeomReader) JOptionPane.showInputDialog(null, "Choose a format for the file: " + pathName, "Select Format", JOptionPane.INFORMATION_MESSAGE, null, possibleValues, possibleValues[0]);
        }
        FastTable.recycle(list);
        return selectedReader;
    }

    /**
	*  Method that attempts to find an {@link PointGeomReader} object that might be able
	*  to read the data in the specified file.
	*
	*  @param theFile  The file to find a reader for.
	*  @return A reader that is appropriate for the specified file, or <code>null</code> if the
	*          user cancels the multiple reader selection dialog.
	*  @throws IOException if an appropraite reader for the file could not be found.
	**/
    public static PointGeomReader getReader(File theFile) throws IOException {
        if (!theFile.exists()) throw new IOException("Could not find the file \"" + theFile.getName() + "\".");
        PointGeomReader reader = null;
        FileInputStream input = null;
        try {
            input = new FileInputStream(theFile);
            String name = theFile.getName();
            reader = getReader(input, name);
        } finally {
            if (input != null) input.close();
        }
        return reader;
    }

    /**
	*  Method that returns a list of all the {@link PointGeomReader} objects found by this
	*  factory.
	*  
	*  @return An array of PointGeomReader objects (can be null if static init failed).
	**/
    public static PointGeomReader[] getAllReaders() {
        return _allReaders;
    }

    private static PointGeomReader[] loadResourceList(final String resourceName, ClassLoader loader) {
        if (loader == null) loader = ClassLoader.getSystemClassLoader();
        final FastSet<PointGeomReader> result = FastSet.newInstance();
        try {
            final Enumeration<URL> resources = loader.getResources(resourceName);
            if (resources != null) {
                while (resources.hasMoreElements()) {
                    final URL url = resources.nextElement();
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
        PointGeomReader[] resultArr = result.toArray(new PointGeomReader[result.size()]);
        Arrays.sort(resultArr, FastComparator.DEFAULT);
        FastSet.recycle(result);
        return resultArr;
    }

    private static PointGeomReader loadResource(final String className, final ClassLoader loader) {
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
        if (!(reader instanceof PointGeomReader)) throw new RuntimeException("not a geomss.geom.PointGeomReader" + " implementation: " + cls.getName());
        return (PointGeomReader) reader;
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
