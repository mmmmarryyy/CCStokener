package org.jargo;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.lang.reflect.Constructor;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Modifier;
import java.net.URL;
import java.security.AccessController;
import java.security.PrivilegedActionException;
import java.security.PrivilegedExceptionAction;
import java.util.Enumeration;
import java.util.List;
import org.jargo.deploy.Deployer;

/**
 * Provides access to jargo components.
 * 
 * @author Leon van Zantvoort
 */
public abstract class ComponentApplicationContext implements Deployer {

    private static class LazyHolder {

        static final ComponentApplicationContext ctx;

        static {
            try {
                try {
                    Constructor constructor = AccessController.doPrivileged(new PrivilegedExceptionAction<Constructor>() {

                        public Constructor run() throws Exception {
                            String path = "META-INF/services/" + ComponentApplicationContext.class.getName();
                            ClassLoader loader = Thread.currentThread().getContextClassLoader();
                            final Enumeration<URL> urls;
                            if (loader == null) {
                                urls = ComponentApplicationContext.class.getClassLoader().getResources(path);
                            } else {
                                urls = loader.getResources(path);
                            }
                            while (urls.hasMoreElements()) {
                                URL url = urls.nextElement();
                                BufferedReader reader = new BufferedReader(new InputStreamReader(url.openStream()));
                                try {
                                    String className = null;
                                    while ((className = reader.readLine()) != null) {
                                        final String name = className.trim();
                                        if (!name.startsWith("#") && !name.startsWith(";") && !name.startsWith("//")) {
                                            final Class<?> cls;
                                            if (loader == null) {
                                                cls = Class.forName(name);
                                            } else {
                                                cls = Class.forName(name, true, loader);
                                            }
                                            int m = cls.getModifiers();
                                            if (ComponentApplicationContext.class.isAssignableFrom(cls) && !Modifier.isAbstract(m) && !Modifier.isInterface(m)) {
                                                Constructor constructor = cls.getDeclaredConstructor();
                                                if (!Modifier.isPublic(constructor.getModifiers())) {
                                                    constructor.setAccessible(true);
                                                }
                                                return constructor;
                                            } else {
                                                throw new ClassCastException(cls.getName());
                                            }
                                        }
                                    }
                                } finally {
                                    reader.close();
                                }
                            }
                            throw new ComponentApplicationException("No " + "ComponentApplicationContext implementation " + "found.");
                        }
                    });
                    ctx = (ComponentApplicationContext) constructor.newInstance();
                } catch (PrivilegedActionException e) {
                    throw e.getException();
                } catch (InvocationTargetException e) {
                    throw e.getTargetException();
                }
            } catch (ComponentApplicationException e) {
                throw e;
            } catch (Error e) {
                throw e;
            } catch (Throwable t) {
                throw new ComponentApplicationException(t);
            }
        }
    }

    /**
     * Factory method for obtaining a {@code ComponentApplicationContext} 
     * instance.
     */
    public static ComponentApplicationContext instance() throws ComponentApplicationException {
        try {
            try {
                return LazyHolder.ctx.resolveInstance();
            } catch (ExceptionInInitializerError e) {
                try {
                    throw e.getException();
                } catch (ComponentApplicationException e2) {
                    throw e2;
                } catch (Error e2) {
                    throw e2;
                } catch (Throwable t) {
                    throw new ComponentApplicationException(t);
                }
            }
        } catch (ComponentApplicationException e) {
            throw e;
        } catch (RuntimeException e) {
            throw e;
        } catch (Error e) {
            throw e;
        }
    }

    /**
     * Subclasses can override this method to have control over which instance
     * is to be returned by {@link instance}.
     */
    protected ComponentApplicationContext resolveInstance() throws ComponentApplicationException {
        return this;
    }

    /**
     * Returns an immutable list of {@code ComponentFactory} objects for all 
     * registered components.
     */
    public abstract List<ComponentFactory<?>> getComponentFactories();

    /**
     * Returns an immutable list of {@code ComponentFactory} objects for 
     * component instances that match the specified {@code type}.
     */
    public abstract <T> List<ComponentFactory<? extends T>> getComponentFactoriesForType(Class<T> type);

    /**
     * Looks up the {@code ComponentFactory} for the specified 
     * {@code componentName}.
     *
     * @throws ComponentNotFoundException if no component is registered for the
     * specified {@code componentName}.
     */
    public abstract ComponentFactory<?> getComponentFactory(String componentName) throws ComponentNotFoundException;

    /**
     * <p>Returns {@code true} if a component exists for the specified
     * {@code componentName}.</p>
     *
     * @param componentName name of the component.
     * @return {@code true} if component exists, or {@code false} otherwise.
     */
    public abstract boolean exists(String componentName);

    /**
     * Returns the call stack of all references being invoked by the this 
     * thread.
     */
    public abstract List<ComponentReference<?>> referenceStack();
}
