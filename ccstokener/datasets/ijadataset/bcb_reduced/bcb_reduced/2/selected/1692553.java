package geomss.geom.reader;

import java.io.*;
import java.net.URL;
import java.util.Arrays;
import java.util.Enumeration;
import java.util.Properties;
import java.util.ResourceBundle;
import javax.swing.JOptionPane;
import javolution.util.FastTable;
import javolution.util.FastSet;
import javolution.util.FastComparator;

/**
*  This class returns a specific {@link GeomReader} object that can read in the specified file.
*  This class implements a pluggable architecture.  A new {@link GeomReader} class can be added
*  by simply creating a subclass of <code>GeomReader</code>, creating a "GeomReader.properties" file
*  that refers to it, and putting that properties file somewhere in the Java search path.
*  All "GeomReader.properties" files that are found are merged together to create a global
*  list of reader/handler mappings.
*
*  <p>  Modified by:  Joseph A. Huwaldt    </p>
*
*  @author    Joseph A. Huwaldt    Date:  April 14, 2000
*  @version   August 9, 2011
**/
public final class GeomReaderFactory {

    /**
	*  The resource bundle for this package.
	**/
    private static final ResourceBundle RESOURCES = ResourceBundle.getBundle("geomss.geom.reader.GeomReaderFactoryResources", java.util.Locale.getDefault());

    /**
	* All classloader resources with this name ("GeomReader.properties") are loaded
	* as .properties definitions and merged together to create a global list of
	* reader handler mappings.
	**/
    private static final String MAPPING_RES_NAME = "GeomReader.properties";

    private static final GeomReader[] _allReaders;

    static {
        GeomReader[] temp = null;
        try {
            temp = loadResourceList(MAPPING_RES_NAME, getClassLoader());
        } catch (Exception e) {
            System.out.println(RESOURCES.getString("couldntLoadMappings").replace("<MAPPINGRES/>", MAPPING_RES_NAME));
            e.printStackTrace();
        }
        _allReaders = temp;
    }

    private GeomReaderFactory() {
    }

    /**
	*  Method that attempts to find an {@link GeomReader} object that might be able
	*  to read the data in the specified file.
	*
	*  @param theFile  The file to find a reader for.
	*  @return A reader that is appropriate for the specified file, or <code>null</code> if the
	*          user cancels the multiple reader selection dialog.
	*  @throws IOException if an appropraite reader for the file could not be found.
	**/
    public static GeomReader getReader(File theFile) throws IOException {
        if (!theFile.exists()) throw new IOException(RESOURCES.getString("missingFileErr").replace("<FILENAME/>", theFile.getName()));
        GeomReader[] allReaders = getAllReaders();
        if (allReaders == null || allReaders.length < 1) throw new IOException(RESOURCES.getString("noReadersErr"));
        FastTable<GeomReader> list = FastTable.newInstance();
        try {
            for (GeomReader reader : allReaders) {
                int canReadFile = reader.canReadData(theFile);
                if (canReadFile == GeomReader.YES) {
                    FastTable.recycle(list);
                    return reader;
                }
                if (canReadFile == GeomReader.MAYBE) list.add(reader);
            }
        } catch (Exception e) {
            FastTable.recycle(list);
            e.printStackTrace();
            throw new IOException(RESOURCES.getString("detFileTypeErr"));
        }
        if (list.size() < 1) {
            FastTable.recycle(list);
            throw new IOException(RESOURCES.getString("fileTypeErr"));
        }
        GeomReader selectedReader = null;
        if (list.size() == 1) selectedReader = list.get(0); else {
            GeomReader[] possibleValues = list.toArray(new GeomReader[list.size()]);
            selectedReader = (GeomReader) JOptionPane.showInputDialog(null, RESOURCES.getString("chooseFormatMsg").replace("<FILENAME/>", theFile.getName()), RESOURCES.getString("chooseFormatTitle"), JOptionPane.INFORMATION_MESSAGE, null, possibleValues, possibleValues[0]);
        }
        FastTable.recycle(list);
        return selectedReader;
    }

    /**
	*  Method that returns a list of all the {@link GeomReader} objects found by this
	*  factory.
	*  
	*  @return An array of GeomReader objects (can be <code>null</code> if static initialization failed).
	**/
    public static GeomReader[] getAllReaders() {
        return _allReaders;
    }

    private static GeomReader[] loadResourceList(final String resourceName, ClassLoader loader) {
        if (loader == null) loader = ClassLoader.getSystemClassLoader();
        final FastSet<GeomReader> result = FastSet.newInstance();
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
                        if (urlIn != null) urlIn.close();
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
        GeomReader[] resultArr = result.toArray(new GeomReader[result.size()]);
        Arrays.sort(resultArr, FastComparator.DEFAULT);
        FastSet.recycle(result);
        return resultArr;
    }

    private static GeomReader loadResource(final String className, final ClassLoader loader) {
        if (className == null) throw new IllegalArgumentException("null input: className");
        if (loader == null) throw new IllegalArgumentException("null input: loader");
        final Class cls;
        final Object reader;
        try {
            cls = Class.forName(className, true, loader);
            reader = cls.newInstance();
        } catch (Exception e) {
            throw new RuntimeException(RESOURCES.getString("couldntLoadClass").replace("<CLASSNAME/>", className) + " " + e.getMessage());
        }
        if (!(reader instanceof GeomReader)) throw new RuntimeException(RESOURCES.getString("notGeomReader").replace("<CLASSNAME/>", cls.getName()));
        return (GeomReader) reader;
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
