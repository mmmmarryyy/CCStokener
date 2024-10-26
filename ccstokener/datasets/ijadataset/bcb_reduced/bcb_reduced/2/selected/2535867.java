package com.caucho.hessian.io;

import com.caucho.hessian.HessianException;
import java.io.InputStream;
import java.net.URL;
import java.lang.ref.SoftReference;
import java.util.Enumeration;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Properties;
import java.util.WeakHashMap;
import java.util.concurrent.ConcurrentHashMap;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * The classloader-specific Factory for returning serialization
 */
public class ContextSerializerFactory {

    private static final Logger log = Logger.getLogger(ContextSerializerFactory.class.getName());

    private static Deserializer OBJECT_DESERIALIZER = new BasicDeserializer(BasicDeserializer.OBJECT);

    private static final WeakHashMap<ClassLoader, SoftReference<ContextSerializerFactory>> _contextRefMap = new WeakHashMap<ClassLoader, SoftReference<ContextSerializerFactory>>();

    private static final ClassLoader _systemClassLoader;

    private static HashMap<String, Serializer> _staticSerializerMap;

    private static HashMap<String, Deserializer> _staticDeserializerMap;

    private static HashMap _staticClassNameMap;

    private ContextSerializerFactory _parent;

    private ClassLoader _loader;

    private final HashSet<String> _serializerFiles = new HashSet<String>();

    private final HashSet<String> _deserializerFiles = new HashSet<String>();

    private final HashMap<String, Serializer> _serializerClassMap = new HashMap<String, Serializer>();

    private final ConcurrentHashMap<String, Serializer> _customSerializerMap = new ConcurrentHashMap<String, Serializer>();

    private final HashMap<Class, Serializer> _serializerInterfaceMap = new HashMap<Class, Serializer>();

    private final HashMap<String, Deserializer> _deserializerClassMap = new HashMap<String, Deserializer>();

    private final HashMap<String, Deserializer> _deserializerClassNameMap = new HashMap<String, Deserializer>();

    private final ConcurrentHashMap<String, Deserializer> _customDeserializerMap = new ConcurrentHashMap<String, Deserializer>();

    private final HashMap<Class, Deserializer> _deserializerInterfaceMap = new HashMap<Class, Deserializer>();

    public ContextSerializerFactory(ContextSerializerFactory parent, ClassLoader loader) {
        if (loader == null) {
            loader = _systemClassLoader;
        }
        _loader = loader;
        init();
    }

    public static ContextSerializerFactory create() {
        return create(Thread.currentThread().getContextClassLoader());
    }

    public static ContextSerializerFactory create(ClassLoader loader) {
        synchronized (_contextRefMap) {
            SoftReference<ContextSerializerFactory> factoryRef = _contextRefMap.get(loader);
            ContextSerializerFactory factory = null;
            if (factoryRef != null) {
                factory = factoryRef.get();
            }
            if (factory == null) {
                ContextSerializerFactory parent = null;
                if (loader != null) {
                    parent = create(loader.getParent());
                }
                factory = new ContextSerializerFactory(parent, loader);
                factoryRef = new SoftReference<ContextSerializerFactory>(factory);
                _contextRefMap.put(loader, factoryRef);
            }
            return factory;
        }
    }

    public ClassLoader getClassLoader() {
        return _loader;
    }

    /**
     * Returns the serializer for a given class.
     */
    public Serializer getSerializer(String className) {
        Serializer serializer = _serializerClassMap.get(className);
        if (serializer == AbstractSerializer.NULL) {
            return null;
        } else {
            return serializer;
        }
    }

    /**
     * Returns a custom serializer the class
     *
     * @param cl the class of the object that needs to be serialized.
     *
     * @return a serializer object for the serialization.
     */
    public Serializer getCustomSerializer(Class cl) {
        Serializer serializer = _customSerializerMap.get(cl.getName());
        if (serializer == AbstractSerializer.NULL) {
            return null;
        } else if (serializer != null) {
            return serializer;
        }
        try {
            Class serClass = Class.forName(cl.getName() + "HessianSerializer", false, cl.getClassLoader());
            Serializer ser = (Serializer) serClass.newInstance();
            _customSerializerMap.put(cl.getName(), ser);
            return ser;
        } catch (ClassNotFoundException e) {
            log.log(Level.ALL, e.toString(), e);
        } catch (Exception e) {
            throw new HessianException(e);
        }
        _customSerializerMap.put(cl.getName(), AbstractSerializer.NULL);
        return null;
    }

    /**
     * Returns the deserializer for a given class.
     */
    public Deserializer getDeserializer(String className) {
        Deserializer deserializer = _deserializerClassMap.get(className);
        if (deserializer == AbstractDeserializer.NULL) {
            return null;
        } else {
            return deserializer;
        }
    }

    /**
     * Returns a custom deserializer the class
     *
     * @param cl the class of the object that needs to be deserialized.
     *
     * @return a deserializer object for the deserialization.
     */
    public Deserializer getCustomDeserializer(Class cl) {
        Deserializer deserializer = _customDeserializerMap.get(cl.getName());
        if (deserializer == AbstractDeserializer.NULL) {
            return null;
        } else if (deserializer != null) {
            return deserializer;
        }
        try {
            Class serClass = Class.forName(cl.getName() + "HessianDeserializer", false, cl.getClassLoader());
            Deserializer ser = (Deserializer) serClass.newInstance();
            _customDeserializerMap.put(cl.getName(), ser);
            return ser;
        } catch (ClassNotFoundException e) {
            log.log(Level.ALL, e.toString(), e);
        } catch (Exception e) {
            throw new HessianException(e);
        }
        _customDeserializerMap.put(cl.getName(), AbstractDeserializer.NULL);
        return null;
    }

    /**
     * Initialize the factory
     */
    private void init() {
        if (_parent != null) {
            _serializerFiles.addAll(_parent._serializerFiles);
            _deserializerFiles.addAll(_parent._deserializerFiles);
            _serializerClassMap.putAll(_parent._serializerClassMap);
            _deserializerClassMap.putAll(_parent._deserializerClassMap);
        }
        if (_parent == null) {
            _serializerClassMap.putAll(_staticSerializerMap);
            _deserializerClassMap.putAll(_staticDeserializerMap);
            _deserializerClassNameMap.putAll(_staticClassNameMap);
        }
        HashMap<Class, Class> classMap;
        classMap = new HashMap<Class, Class>();
        initSerializerFiles("META-INF/hessian/serializers", _serializerFiles, classMap, Serializer.class);
        for (Map.Entry<Class, Class> entry : classMap.entrySet()) {
            try {
                Serializer ser = (Serializer) entry.getValue().newInstance();
                if (entry.getKey().isInterface()) {
                    _serializerInterfaceMap.put(entry.getKey(), ser);
                } else {
                    _serializerClassMap.put(entry.getKey().getName(), ser);
                }
            } catch (Exception e) {
                throw new HessianException(e);
            }
        }
        classMap = new HashMap<Class, Class>();
        initSerializerFiles("META-INF/hessian/deserializers", _deserializerFiles, classMap, Deserializer.class);
        for (Map.Entry<Class, Class> entry : classMap.entrySet()) {
            try {
                Deserializer ser = (Deserializer) entry.getValue().newInstance();
                if (entry.getKey().isInterface()) {
                    _deserializerInterfaceMap.put(entry.getKey(), ser);
                } else {
                    _deserializerClassMap.put(entry.getKey().getName(), ser);
                }
            } catch (Exception e) {
                throw new HessianException(e);
            }
        }
    }

    private void initSerializerFiles(String fileName, HashSet<String> fileList, HashMap<Class, Class> classMap, Class type) {
        try {
            ClassLoader classLoader = getClassLoader();
            if (classLoader == null) {
                return;
            }
            Enumeration iter;
            iter = classLoader.getResources(fileName);
            while (iter.hasMoreElements()) {
                URL url = (URL) iter.nextElement();
                if (fileList.contains(url.toString())) {
                    continue;
                }
                fileList.add(url.toString());
                InputStream is = null;
                try {
                    is = url.openStream();
                    Properties props = new Properties();
                    props.load(is);
                    for (Map.Entry entry : props.entrySet()) {
                        String apiName = (String) entry.getKey();
                        String serializerName = (String) entry.getValue();
                        Class apiClass = null;
                        Class serializerClass = null;
                        try {
                            apiClass = Class.forName(apiName, false, classLoader);
                        } catch (ClassNotFoundException e) {
                            log.fine(url + ": " + apiName + " is not available in this context: " + getClassLoader());
                            continue;
                        }
                        try {
                            serializerClass = Class.forName(serializerName, false, classLoader);
                        } catch (ClassNotFoundException e) {
                            log.fine(url + ": " + serializerName + " is not available in this context: " + getClassLoader());
                            continue;
                        }
                        if (!type.isAssignableFrom(serializerClass)) {
                            throw new HessianException(url + ": " + serializerClass.getName() + " is invalid because it does not implement " + type.getName());
                        }
                        classMap.put(apiClass, serializerClass);
                    }
                } finally {
                    if (is != null) {
                        is.close();
                    }
                }
            }
        } catch (RuntimeException e) {
            throw e;
        } catch (Exception e) {
            throw new HessianException(e);
        }
    }

    private static void addBasic(Class cl, String typeName, int type) {
        _staticSerializerMap.put(cl.getName(), new BasicSerializer(type));
        Deserializer deserializer = new BasicDeserializer(type);
        _staticDeserializerMap.put(cl.getName(), deserializer);
        _staticClassNameMap.put(typeName, deserializer);
    }

    static {
        _staticSerializerMap = new HashMap();
        _staticDeserializerMap = new HashMap();
        _staticClassNameMap = new HashMap();
        addBasic(void.class, "void", BasicSerializer.NULL);
        addBasic(Boolean.class, "boolean", BasicSerializer.BOOLEAN);
        addBasic(Byte.class, "byte", BasicSerializer.BYTE);
        addBasic(Short.class, "short", BasicSerializer.SHORT);
        addBasic(Integer.class, "int", BasicSerializer.INTEGER);
        addBasic(Long.class, "long", BasicSerializer.LONG);
        addBasic(Float.class, "float", BasicSerializer.FLOAT);
        addBasic(Double.class, "double", BasicSerializer.DOUBLE);
        addBasic(Character.class, "char", BasicSerializer.CHARACTER_OBJECT);
        addBasic(String.class, "string", BasicSerializer.STRING);
        addBasic(Object.class, "object", BasicSerializer.OBJECT);
        addBasic(java.util.Date.class, "date", BasicSerializer.DATE);
        addBasic(boolean.class, "boolean", BasicSerializer.BOOLEAN);
        addBasic(byte.class, "byte", BasicSerializer.BYTE);
        addBasic(short.class, "short", BasicSerializer.SHORT);
        addBasic(int.class, "int", BasicSerializer.INTEGER);
        addBasic(long.class, "long", BasicSerializer.LONG);
        addBasic(float.class, "float", BasicSerializer.FLOAT);
        addBasic(double.class, "double", BasicSerializer.DOUBLE);
        addBasic(char.class, "char", BasicSerializer.CHARACTER);
        addBasic(boolean[].class, "[boolean", BasicSerializer.BOOLEAN_ARRAY);
        addBasic(byte[].class, "[byte", BasicSerializer.BYTE_ARRAY);
        addBasic(short[].class, "[short", BasicSerializer.SHORT_ARRAY);
        addBasic(int[].class, "[int", BasicSerializer.INTEGER_ARRAY);
        addBasic(long[].class, "[long", BasicSerializer.LONG_ARRAY);
        addBasic(float[].class, "[float", BasicSerializer.FLOAT_ARRAY);
        addBasic(double[].class, "[double", BasicSerializer.DOUBLE_ARRAY);
        addBasic(char[].class, "[char", BasicSerializer.CHARACTER_ARRAY);
        addBasic(String[].class, "[string", BasicSerializer.STRING_ARRAY);
        addBasic(Object[].class, "[object", BasicSerializer.OBJECT_ARRAY);
        Deserializer objectDeserializer = new JavaDeserializer(Object.class);
        _staticDeserializerMap.put("object", objectDeserializer);
        _staticClassNameMap.put("object", objectDeserializer);
        _staticSerializerMap.put(Class.class.getName(), new ClassSerializer());
        _staticDeserializerMap.put(Number.class.getName(), new BasicDeserializer(BasicSerializer.NUMBER));
        _staticSerializerMap.put(java.sql.Date.class.getName(), new SqlDateSerializer());
        _staticSerializerMap.put(java.sql.Time.class.getName(), new SqlDateSerializer());
        _staticSerializerMap.put(java.sql.Timestamp.class.getName(), new SqlDateSerializer());
        _staticDeserializerMap.put(java.sql.Date.class.getName(), new SqlDateDeserializer(java.sql.Date.class));
        _staticDeserializerMap.put(java.sql.Time.class.getName(), new SqlDateDeserializer(java.sql.Time.class));
        _staticDeserializerMap.put(java.sql.Timestamp.class.getName(), new SqlDateDeserializer(java.sql.Timestamp.class));
        _staticDeserializerMap.put(StackTraceElement.class.getName(), new StackTraceElementDeserializer());
        ClassLoader systemClassLoader = null;
        try {
            systemClassLoader = ClassLoader.getSystemClassLoader();
        } catch (Exception e) {
        }
        _systemClassLoader = systemClassLoader;
    }
}
