package org.datanucleus.enhancer;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.lang.reflect.Constructor;
import java.lang.reflect.InvocationTargetException;
import java.net.URL;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import org.datanucleus.ClassLoaderResolver;
import org.datanucleus.NucleusContext;
import org.datanucleus.exceptions.ClassNotResolvedException;
import org.datanucleus.exceptions.NucleusException;
import org.datanucleus.metadata.AbstractClassMetaData;
import org.datanucleus.metadata.ClassMetaData;
import org.datanucleus.metadata.MetaDataManager;
import org.datanucleus.util.Localiser;
import org.datanucleus.util.StringUtils;

/**
 * Class that will enhance a class at runtime called via the ClassTransformer.
 */
public class RuntimeEnhancer {

    /** Message resource */
    protected static final Localiser LOCALISER = Localiser.getInstance("org.datanucleus.enhancer.Localisation", ClassEnhancer.class.getClassLoader());

    private Constructor classEnhancerConstructor;

    /** API adapter to use for enhancement. */
    private String api = "JDO";

    /** Whether to allow generation of the PK when needed. */
    private boolean generatePK = true;

    /** Whether to allow generation of the default constructor when needed. */
    private boolean generateConstructor = true;

    /** Whether to use Detach Listener */
    private boolean detachListener = false;

    /** The symbolic name of the ClassEnhancer to use (default is ASM currently). */
    private String enhancerName = "ASM";

    private ClassLoaderResolver clr;

    private NucleusContext nucleusContext;

    /** if this enhancer is initialized, once initialized settings cannot be changed **/
    private boolean initialized = false;

    private static Class[] CLASS_ENHANCER_CONSTRUCTOR_ARGS_TYPES = new Class[] { ClassMetaData.class, ClassLoaderResolver.class, MetaDataManager.class, byte[].class };

    Map<ClassLoader, EnhancerClassLoader> runtimeLoaderByLoader = new HashMap();

    /**
     *  This classloader is used to load any classes that are necessary during enhancement process, 
     *  and avoid using application classloaders to load classes
     */
    public static class EnhancerClassLoader extends ClassLoader {

        EnhancerClassLoader(ClassLoader loader) {
            super(loader);
        }

        @SuppressWarnings("unchecked")
        protected synchronized Class loadClass(String name, boolean resolve) throws ClassNotFoundException {
            Class c = super.findLoadedClass(name);
            if (c != null) {
                return c;
            }
            if (name.startsWith("java.")) {
                return super.loadClass(name, resolve);
            } else if (name.startsWith("javax.")) {
                return super.loadClass(name, resolve);
            } else if (name.startsWith("org.datanucleus.jpa.annotations") || name.startsWith("org.datanucleus.api.jpa.annotations")) {
                return super.loadClass(name, resolve);
            }
            String resource = StringUtils.replaceAll(name, ".", "/") + ".class";
            try {
                URL url = super.getResource(resource);
                if (url == null) {
                    throw new ClassNotFoundException(name);
                }
                InputStream is = url.openStream();
                try {
                    ByteArrayOutputStream os = new ByteArrayOutputStream();
                    byte[] b = new byte[2048];
                    int count;
                    while ((count = is.read(b, 0, 2048)) != -1) {
                        os.write(b, 0, count);
                    }
                    byte[] bytes = os.toByteArray();
                    return defineClass(name, bytes, 0, bytes.length);
                } finally {
                    if (is != null) {
                        is.close();
                    }
                }
            } catch (SecurityException e) {
                return super.loadClass(name, resolve);
            } catch (IOException e) {
                throw new ClassNotFoundException(name, e);
            }
        }
    }

    public RuntimeEnhancer() {
    }

    public byte[] enhance(final String className, byte[] classdefinition, ClassLoader loader) {
        if (!initialized) {
            initialize();
        }
        EnhancerClassLoader runtimeLoader = runtimeLoaderByLoader.get(loader);
        if (runtimeLoader == null) {
            runtimeLoader = new EnhancerClassLoader(loader);
            runtimeLoaderByLoader.put(loader, runtimeLoader);
        }
        clr.setPrimary(runtimeLoader);
        try {
            Class clazz = null;
            try {
                clazz = clr.classForName(className);
            } catch (ClassNotResolvedException e1) {
                DataNucleusEnhancer.LOGGER.debug(StringUtils.getStringFromStackTrace(e1));
                return null;
            }
            AbstractClassMetaData acmd = nucleusContext.getMetaDataManager().getMetaDataForClass(clazz, clr);
            if (acmd == null) {
                DataNucleusEnhancer.LOGGER.debug("Class " + className + " cannot be enhanced because no metadata has been found.");
                return null;
            }
            ClassEnhancer classEnhancer = null;
            try {
                classEnhancer = (ClassEnhancer) classEnhancerConstructor.newInstance(new Object[] { acmd, clr, nucleusContext.getMetaDataManager(), classdefinition });
            } catch (InvocationTargetException e) {
                e.printStackTrace();
                String msg = LOCALISER.msg("Enhancer.ClassEnhancer.ConstructorError", enhancerName, classEnhancerConstructor.getDeclaringClass().getName(), e.getTargetException());
                DataNucleusEnhancer.LOGGER.error(msg, e);
                return null;
            } catch (Exception e) {
                e.printStackTrace();
                String msg = LOCALISER.msg("Enhancer.ClassEnhancer.ConstructorError", enhancerName, classEnhancerConstructor.getDeclaringClass().getName(), e.getMessage());
                DataNucleusEnhancer.LOGGER.error(msg, e);
                return null;
            }
            List<String> options = new ArrayList<String>();
            if (generatePK) {
                options.add(ClassEnhancer.OPTION_GENERATE_PK);
            }
            if (generateConstructor) {
                options.add(ClassEnhancer.OPTION_GENERATE_DEFAULT_CONSTRUCTOR);
            }
            if (detachListener) {
                options.add(ClassEnhancer.OPTION_GENERATE_DETACH_LISTENER);
            }
            classEnhancer.setOptions(options);
            classEnhancer.enhance();
            return classEnhancer.getClassBytes();
        } catch (Throwable ex) {
            DataNucleusEnhancer.LOGGER.error(StringUtils.getStringFromStackTrace(ex));
        }
        return null;
    }

    @SuppressWarnings("unchecked")
    private synchronized void initialize() {
        nucleusContext = new NucleusContext(api, NucleusContext.ContextType.ENHANCEMENT, null);
        clr = nucleusContext.getClassLoaderResolver(null);
        Class classEnhancerClass;
        String className = null;
        try {
            className = (String) nucleusContext.getPluginManager().getAttributeValueForExtension("org.datanucleus.enhancer.enhancer", new String[] { "name", "api" }, new String[] { enhancerName, api }, "class-name");
            classEnhancerClass = clr.classForName(className, RuntimeEnhancer.class.getClassLoader());
        } catch (Exception e) {
            String msg = LOCALISER.msg("Enhancer.ClassEnhancer.ClassNotFound", enhancerName, className);
            DataNucleusEnhancer.LOGGER.error(msg);
            throw new NucleusException(msg, e);
        } catch (Error e2) {
            String msg = LOCALISER.msg("Enhancer.ClassEnhancer.ClassNotFound", enhancerName, className);
            DataNucleusEnhancer.LOGGER.error(msg);
            throw new NucleusException(msg, e2);
        }
        try {
            classEnhancerConstructor = classEnhancerClass.getConstructor(CLASS_ENHANCER_CONSTRUCTOR_ARGS_TYPES);
        } catch (Exception e) {
            String msg = LOCALISER.msg("Enhancer.ClassEnhancer.ConstructorNotFound", enhancerName, classEnhancerClass.getName(), e.getMessage());
            DataNucleusEnhancer.LOGGER.error(msg, e);
            throw new NucleusException(msg, e);
        } catch (Error e2) {
            String msg = LOCALISER.msg("Enhancer.ClassEnhancer.ConstructorNotFound", enhancerName, classEnhancerClass.getName(), e2.getMessage());
            DataNucleusEnhancer.LOGGER.error(msg, e2);
            throw new NucleusException(msg, e2);
        }
        initialized = true;
    }

    public void setApi(String api) {
        this.api = api;
    }

    public void setEnhancerName(String enhancerName) {
        this.enhancerName = enhancerName;
    }

    public void setGeneratePK(boolean generatePK) {
        this.generatePK = generatePK;
    }

    public void setGenerateConstructor(boolean generateConstructor) {
        this.generateConstructor = generateConstructor;
    }

    public void setDetachListener(Boolean detachListener) {
        this.detachListener = detachListener;
    }
}
