package jahuwaldt.maptools;

import java.lang.reflect.Constructor;
import java.lang.reflect.InvocationTargetException;
import java.net.URL;
import java.io.InputStream;
import java.io.IOException;
import java.util.Map;
import java.util.HashMap;
import java.util.Properties;
import java.util.Enumeration;
import javax.swing.JOptionPane;
import jahuwaldt.swing.AppUtilities;
import jahuwaldt.maptools.sdts.SDTS_DLGAttribute;

/**
*  A factory class that returns a DLGArea object type
*  that is appropriate for the supplied major (catagory)
*  and minor USGS codes.
*
*  <p>  Modified by:  Joseph A. Huwaldt  </p>
*
*  @author  Joseph A. Huwaldt   Date:  May 25, 2000
*  @version July 11, 2004
**/
public class DLGAreaFactory {

    private static final Class[] kElementArgsClass = { int.class, SDTS_DLGAttribute[].class, UTMCoord.class, DLGLine[].class, boolean[].class };

    private static Map kElementMap;

    /**
	* All classloader resources with this name ("DLGArea.properties") are loaded
	* as .properties definitions and merged together to create a global list of
	* reader handler mappings.
	**/
    public static final String kMappingResName = "DLGArea.properties";

    static {
        Map temp = null;
        try {
            temp = loadResourceMap(kMappingResName, getClassLoader());
        } catch (Exception e) {
            System.out.println("could not load all" + " [" + kMappingResName + "] mappings:");
            e.printStackTrace(System.out);
        }
        kElementMap = temp;
    }

    private DLGAreaFactory() {
    }

    /**
	*  Return an instance of a specific DLGArea type that is
	*  appropriate for the input major and minor USGS codes.
	*
	*  @param  id          The ID number assigned to this element.
	*  @param  majorCode   The USGS major code assigned to this area.
	*  @param  minorCode   The USGS minor codes assigned to this area.
	*  @param  attrib      A list of label/value pair attributes assigned to
	*                      this element.
	*  @param  center     A UTM map coordinate located inside the area
	*                     (usually near the center).
	*  @param  lines      A list of Line objects that define the boundary
	*                     of this area.
	*  @param  areaLR     An array of flags indicating if this area is to 
	*                     the left (false) or right (true) of each bounding
	*                     line segment.
	*  @return A DLGArea object that is appropriate for the specified major and minor codes.
	*          If an specific DLGArea is not availbe, a generic area object is returned.
	**/
    public static DLGArea getInstance(int id, int majorCode, int minorCode, SDTS_DLGAttribute[] attrib, UTMCoord center, DLGLine[] lines, boolean[] areaLR) {
        DLGArea element = null;
        if (minorCode == 0) element = new DLGANull(id, majorCode, attrib, center, lines, areaLR); else {
            if (kElementMap != null) {
                String code = String.valueOf(majorCode) + String.valueOf(minorCode);
                String className = (String) kElementMap.get(code);
                if (className != null) {
                    try {
                        Object[] initArgs = new Object[] { new Integer(id), attrib, center, lines, areaLR };
                        element = (DLGArea) loadResource(className, getClassLoader(), initArgs);
                    } catch (ClassNotFoundException e) {
                        JOptionPane.showMessageDialog(null, className, "Could not load the following class:", JOptionPane.ERROR_MESSAGE);
                        e.printStackTrace();
                    } catch (Exception e) {
                        e.printStackTrace();
                        AppUtilities.showException(null, "Unexpected Error", "Copy the following message and e-mail it to the author:", e);
                    }
                }
            }
            if (element == null) element = new DLGArea(id, majorCode, minorCode, attrib, center, lines, areaLR);
        }
        return element;
    }

    /**
	* Loads a Map of the class names that is a union of *all* resources named
	* 'resourceName' as seen by 'loader'. Null 'loader' is equivalent to the
	* application loader.
	**/
    private static Map loadResourceMap(final String resourceName, ClassLoader loader) {
        if (loader == null) loader = ClassLoader.getSystemClassLoader();
        final Map result = new HashMap();
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
                    result.putAll(mapping);
                }
            }
        } catch (IOException ignore) {
        }
        return result;
    }

    /**
	*  Loads and initializes a single resource for a given class name via a given classloader.
	*
	*  @param  className  The name of the class to be loaded.
	*  @param  loader     The class loader to use.
	*  @param  initArgs   The arguments to be passed to the constructor for the class being loaded.
	**/
    private static Object loadResource(final String className, final ClassLoader loader, Object[] initArgs) throws ClassNotFoundException, NoSuchMethodException, InstantiationException, IllegalAccessException, InvocationTargetException {
        if (className == null) throw new IllegalArgumentException("null input: className");
        if (loader == null) throw new IllegalArgumentException("null input: loader");
        final Class cls = Class.forName(className, true, loader);
        Constructor constructor = cls.getConstructor(kElementArgsClass);
        Object element = constructor.newInstance(initArgs);
        return element;
    }

    /**
	* This method decides on which classloader is to be used by all resource/class
	* loading in this class. At the very least you should use the current thread's
	* context loader. A better strategy would be to use techniques shown in
	* http://www.javaworld.com/javaworld/javaqa/2003-06/01-qa-0606-load.html 
	**/
    private static ClassLoader getClassLoader() {
        return Thread.currentThread().getContextClassLoader();
    }
}
