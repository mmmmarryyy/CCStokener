package org.ops4j.pax.script.internal;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.net.URL;
import java.util.Dictionary;
import java.util.List;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.ops4j.pax.swissbox.extender.BundleObserver;
import org.ops4j.pax.swissbox.extender.BundleURLScanner;
import org.ops4j.pax.swissbox.extender.BundleWatcher;
import org.osgi.framework.Bundle;
import org.osgi.framework.BundleActivator;
import org.osgi.framework.BundleContext;

/**
 * Pax-Script is an extender that automatically registers JSR223 engines (javax.script.ScriptEngineFactory) as OSGi
 * services when a JSR223 JAR is loaded.
 * 
 * SCM: http://scm.ops4j.org/browse/OPS4J/laboratory/users/ceefour/pax-script
 * 
 * Also "jointly developed" with JSR223: https://scripting.dev.java.net/issues/show_bug.cgi?id=34
 * 
 * An effort is undergoing to make all JSR223 engines OSGi-compliant. Right now Groovy engine is directly usable by
 * Pax-Script.
 * 
 * Highly inspired by Apache Sling's SlingScriptAdapterFactory:
 * 
 * http://svn.apache.org/repos/asf/incubator/sling/trunk/scripting/core/src/main/java/org/apache/sling/scripting/core/
 * impl/SlingScriptAdapterFactory.java
 */
public abstract class JavaServiceToOsgiServiceConverter<T> implements BundleActivator {

    private Log log = LogFactory.getLog(JavaServiceToOsgiServiceConverter.class);

    private Class<T> serviceClass;

    public JavaServiceToOsgiServiceConverter(Class<T> serviceClass) {
        this.serviceClass = serviceClass;
    }

    protected Dictionary<String, Object> createProperties(T serviceImplementation) {
        return null;
    }

    /**
     * Called whenever the OSGi framework starts our bundle
     */
    @SuppressWarnings("unchecked")
    public void start(final BundleContext bundleContext) throws Exception {
        log.info("STARTING Java To OSGI Service Converter for " + serviceClass.getName());
        BundleURLScanner scanner = new BundleURLScanner("META-INF/services/", serviceClass.getName(), false);
        BundleObserver<URL> observer = new BundleObserver<URL>() {

            @Override
            public void addingEntries(Bundle bundle, List<URL> urls) {
                log.info(String.format("Bundle %s added with %d factories.", bundle.getSymbolicName(), urls.size()));
                for (URL url : urls) {
                    InputStream inStream = null;
                    String serviceImplementationClassName;
                    try {
                        inStream = url.openStream();
                        serviceImplementationClassName = new BufferedReader(new InputStreamReader(inStream)).readLine();
                        inStream.close();
                    } catch (IOException e) {
                        log.error(e);
                        throw new RuntimeException(e);
                    }
                    log.info(String.format("Instantiating service implementation class: %s", serviceImplementationClassName));
                    try {
                        Class<T> serviceImplementationClass = (Class<T>) bundle.loadClass(serviceImplementationClassName);
                        T serviceImplementation = serviceImplementationClass.newInstance();
                        Dictionary<String, Object> properties = createProperties(serviceImplementation);
                        BundleContext extendedContext = bundle.getBundleContext();
                        extendedContext.registerService(serviceClass.getName(), serviceImplementation, properties);
                        log.info(String.format("Registered %s as a %s service.", serviceImplementationClassName, serviceClass.getName()));
                    } catch (ClassNotFoundException e) {
                        log.error(e);
                        throw new RuntimeException(e);
                    } catch (InstantiationException e) {
                        log.error(e);
                        throw new RuntimeException(e);
                    } catch (IllegalAccessException e) {
                        log.error(e);
                        throw new RuntimeException(e);
                    }
                }
            }

            @Override
            public void removingEntries(Bundle bundle, List<URL> urls) {
                log.info(String.format("Bundle %s removed with %d services.", bundle.getSymbolicName(), urls.size()));
            }
        };
        BundleWatcher<URL> servicesWatcher = new BundleWatcher<URL>(bundleContext, scanner, observer);
        servicesWatcher.start();
        log.info(String.format("Java To OSGI Services Converter for %s started", serviceClass.getName()));
    }

    /**
     * Called whenever the OSGi framework stops our bundle
     */
    public void stop(BundleContext bc) throws Exception {
        log.info(String.format("STOPPING Java To OSGI Services Converter for %s", serviceClass.getName()));
    }
}
