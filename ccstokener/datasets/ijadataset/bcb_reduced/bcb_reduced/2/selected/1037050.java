package org.dynalang.mop.impl;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.net.URL;
import java.util.Enumeration;
import java.util.LinkedList;
import java.util.List;
import org.dynalang.mop.BaseMetaobjectProtocol;

/**
 * Provides methods for automatic discovery of all metaobject protocols 
 * listed in the <tt>/META-INF/services/org.dynalang.mop.BaseMetaobjectProtocol</tt>
 * resources of all JAR files for a particular class loader.
 * @author Attila Szegedi
 * @version $Id: $
 */
public class AutoDiscovery {

    private static final String RESOURCE_PATH = "META-INF/services/org.dynalang.mop.BaseMetaobjectProtocol";

    private AutoDiscovery() {
    }

    /**
     * Returns an instance of all metaobject protocol classes that are declared
     * in the <tt>/META-INF/services/org.dynalang.mop.BaseMetaobjectProtocol</tt>
     * resources of all JAR files in the current thread's context class loader's
     * classpath.
     * @return an array of base MOPs. The returned array can be empty. You will
     * probably want to run the return value through 
     * {@link MetaobjectProtocolAdaptor#toMetaobjectProtocols(BaseMetaobjectProtocol[])} 
     * if your code uses the full MOP interface instead of the base one.
     * @throws IOException if there is a problem reading classpath resources
     * @throws InstantiationException if a class declared in a service list can
     * not be instantiated
     * @throws IllegalAccessException if a class declared in a service list is
     * not public or does not have a publicly visible default constructor
     * @throws ClassNotFoundException if a class declared in a service list is
     * not found
     * @throws ClassCastException if a class declared in a service list does 
     * not implement the {@link BaseMetaobjectProtocol} interface.
     */
    public static BaseMetaobjectProtocol[] discoverBaseMetaobjectProtocols() throws IOException, InstantiationException, IllegalAccessException, ClassNotFoundException {
        return discoverBaseMetaobjectProtocols(Thread.currentThread().getContextClassLoader());
    }

    /**
     * Returns an instance of all metaobject protocol classes that are declared
     * in the <tt>/META-INF/services/org.dynalang.mop.BaseMetaobjectProtocol</tt>
     * resources of all JAR files in the specified class loader's classpath.
     * @param cl the class loader within which the MOPs are discovered. null 
     * can be used to denote the {@link ClassLoader#getSystemClassLoader() 
     * system class loader}.
     * @return an array of base MOPs. The returned array can be empty. You will
     * probably want to run the return value through. 
     * {@link MetaobjectProtocolAdaptor#toMetaobjectProtocols(BaseMetaobjectProtocol[])} 
     * if your code uses the full MOP interface instead of the base one.
     * @throws IOException if there is a problem reading classpath resources
     * @throws InstantiationException if a class declared in a service list can
     * not be instantiated
     * @throws IllegalAccessException if a class declared in a service list is
     * not public or does not have a publicly visible default constructor
     * @throws ClassNotFoundException if a class declared in a service list is
     * not found
     * @throws ClassCastException if a class declared in a service list does 
     * not implement the {@link BaseMetaobjectProtocol} interface.
     */
    public static BaseMetaobjectProtocol[] discoverBaseMetaobjectProtocols(ClassLoader cl) throws IOException, InstantiationException, IllegalAccessException, ClassNotFoundException {
        List<BaseMetaobjectProtocol> protocols = new LinkedList<BaseMetaobjectProtocol>();
        Enumeration<URL> urls;
        if (cl == null) {
            urls = ClassLoader.getSystemResources(RESOURCE_PATH);
            cl = ClassLoader.getSystemClassLoader();
        } else {
            urls = cl.getResources(RESOURCE_PATH);
        }
        for (; urls.hasMoreElements(); ) {
            URL url = urls.nextElement();
            InputStream in = url.openStream();
            try {
                BufferedReader r = new BufferedReader(new InputStreamReader(in));
                for (; ; ) {
                    String className = r.readLine();
                    if (className == null) {
                        break;
                    }
                    className = className.trim();
                    if ("".equals(className) || className.startsWith("#")) {
                        continue;
                    }
                    protocols.add((BaseMetaobjectProtocol) Class.forName(className.toString(), true, cl).newInstance());
                }
            } finally {
                in.close();
            }
        }
        return protocols.toArray(new BaseMetaobjectProtocol[protocols.size()]);
    }
}
