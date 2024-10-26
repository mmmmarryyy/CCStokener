package alto.io;

import java.lang.reflect.Method;
import java.lang.reflect.InvocationTargetException;
import java.io.IOException;
import java.io.InputStream;
import java.net.URL;
import java.util.StringTokenizer;

/**
 * This class is a package resource service for
 * <code>"/META-INF/services/alto.io.Tools"</code>.  
 * 
 * The file is a properties map from an abstract base class name to
 * its implementation class name.
 *
 * The map source (LHS) abstract base class has a public static method
 * named "SInit" that takes an argument in the abstract base class.
 * 
 * The map target (RHS) implementation class is a subclass of the
 * abstract base class with a public, simple constructor (no
 * arguments).  The target is instantiated and passed to an invocation
 * of the source SInit function.  
 * 
 * The base class implements initialization for targets implementing
 * {@link alto.lang.Init}.  This permits the base class to perform
 * setup operations after construction of the instance, and before
 * (and after) its initialization.
 * 
 * This initialization process occurs in the class static
 * initialization of this class, {@link alto.io.Tools}.  Any
 * exceptions in this process cause the intialization of this class,
 * {@link alto.io.Tools}, to fail.
 * 
 * This initialization process can be invoked (as from within a try
 * block) by calling the {@link #SInit()} function in this class.
 * 
 * A default list of abstract base classes for alto are defined by
 * code in this class.  Alternatively, a list may be defined in a
 * property named "alto.io.Tools.classes" having a comma separated
 * list of names for {@link
 * java.lang.Class#forName(java.lang.String)}.  Note that the order of
 * initialization starts with reference, file manager, and then type
 * and keys.
 * 
 * @author jdp
 */
public final class Tools extends java.util.Properties {

    public static final String Resource = "/META-INF/services/alto.io.Tools";

    private static final String[] Classes = { "alto.sys.FileManager", "alto.lang.Type$Tools" };

    public static final Tools Instance = new Tools();

    /**
     * Initialize by causing this class to be loaded and initialized.
     * Equivalent to any other mention of this class.
     */
    public static final void SInit() {
        return;
    }

    public Tools() {
        super();
        URL url = this.getClass().getResource(Resource);
        if (null != url) {
            try {
                InputStream in = url.openStream();
                try {
                    super.load(in);
                } finally {
                    in.close();
                }
            } catch (IOException exc) {
                throw new alto.sys.Error.State(Resource, exc);
            }
        }
        String[] classes = Classes;
        {
            String test = this.getProperty("alto.io.Tools.classes");
            if (null != test) {
                StringTokenizer strtok = new StringTokenizer(test, ", \r\n\t;:-+|!");
                int count = strtok.countTokens();
                String[] re = new String[count];
                for (int cc = 0; cc < count; cc++) {
                    re[cc] = strtok.nextToken();
                }
                classes = re;
            }
        }
        for (String from : classes) {
            try {
                Class fromClass = Class.forName(from);
                String to = this.getProperty(from);
                if (null != to) {
                    try {
                        Class toClass = Class.forName(to);
                        if (fromClass.isAssignableFrom(toClass)) {
                            try {
                                Method sinit = fromClass.getMethod("SInit", fromClass);
                                Object instance = toClass.newInstance();
                                sinit.invoke(null, instance);
                            } catch (NoSuchMethodException exc) {
                                throw new alto.sys.Error.State(to, exc);
                            } catch (InstantiationException exc) {
                                throw new alto.sys.Error.State(to, exc);
                            } catch (IllegalAccessException exc) {
                                throw new alto.sys.Error.State(to, exc);
                            } catch (InvocationTargetException exc) {
                                throw new alto.sys.Error.State(to, exc);
                            }
                        } else throw new alto.sys.Error.State(toClass + " is not a subclass of " + fromClass);
                    } catch (ClassNotFoundException exc) {
                        throw new alto.sys.Error.State(to, exc);
                    }
                }
            } catch (ClassNotFoundException exc) {
                throw new alto.sys.Error.State(from, exc);
            }
        }
    }
}
