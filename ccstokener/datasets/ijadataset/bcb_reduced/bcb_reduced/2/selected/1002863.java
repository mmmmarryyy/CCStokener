package net.sf.persistant.util;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import java.io.IOException;
import java.io.InputStream;
import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.lang.reflect.ParameterizedType;
import java.lang.reflect.Type;
import java.net.URL;
import java.util.*;

/**
 * <p>
 * <code>MapUtils</code> provides basic utility methods for creating and manipulating collections.
 * </p>
 */
public final class CollectionUtils {

    private static Logger LOGGER = LoggerFactory.getLogger(CollectionUtils.class);

    /**
     * <p>
     * Construct a {@link CollectionUtils} instance.
     * </p>
     */
    private CollectionUtils() {
        super();
    }

    /**
     * <p>
     * Construct a map by extracting the keys and values from the objects in a given collection.
     * </p>
     *
     * @param objects the collection of objects from which keys and values will be extracted.
     * @param keyExtractor the key extractor.
     * @param valueExtractor the value extractor.
     * @return {@link Map} of key objects to value objects, never <code>null</code>.
     */
    public static <O, K, V> Map<K, V> toMap(final Collection<O> objects, final Extractor<K, O> keyExtractor, final Extractor<V, O> valueExtractor) {
        assert null != keyExtractor : "The [keyExtractor] argument cannot be null.";
        assert null != valueExtractor : "The [valueExtractor] argument cannot be null.";
        final Map<K, V> result;
        if (null == objects || objects.isEmpty()) {
            result = Collections.emptyMap();
        } else {
            result = new HashMap<K, V>(objects.size());
            for (final O nextObject : objects) {
                final K key = keyExtractor.extract(nextObject);
                final V value = valueExtractor.extract(nextObject);
                result.put(key, value);
            }
        }
        if (LOGGER.isDebugEnabled()) {
            LOGGER.debug(String.format("Created map %s from collection %s.", result, objects));
        }
        return result;
    }

    /**
     * <p>
     * Construct a map by extracting the <em>keys</em> from the objects in a given collection, using the objects in the 
     * collection themselves at the map <em>values</em>.
     * </p>
     *
     * @param objects the collection of objects from which keys and values will be extracted.
     * @param keyExtractor the key extractor.
     * @return {@link Map} of key objects to value objects, never <code>null</code>.
     */
    public static <K, O> Map<K, O> toMap(final Collection<O> objects, final Extractor<K, O> keyExtractor) {
        return toMap(objects, keyExtractor, new Extractor<O, O>() {

            public O extract(final O object) {
                return object;
            }
        });
    }

    /**
     * <p>
     * Convert a collection of {@link KeyValue} objects into a map. Handy for type-safe static
     * construction of maps.
     * </p>
     *
     * @param keyValues the keys and values.
     * @return {@link Map} of keys and values.
     */
    public static <K, V> Map<K, V> toMap(final KeyValue<K, V>... keyValues) {
        final int size = null != keyValues ? keyValues.length : 0;
        final Map<K, V> result = new HashMap<K, V>(size);
        for (final KeyValue<K, V> keyValue : keyValues) {
            result.put(keyValue.getKey(), keyValue.getValue());
        }
        return result;
    }

    /**
     * <p>
     * Combine zero or more collections, extracted from the objects in a given collection, into a single output
     * collection.
     * </p>
     * 
     * @param objects the objects from which the source collections are to be extracted.
     * @param extractor the extractor which will extract the source collections from the objects.
     * @return {@link Collection} of {@link Object} instances from all of the input collections.
     */
    public static <I, O> Collection<O> combine(final Collection<I> objects, final Extractor<Collection<O>, I> extractor) {
        final Collection<O> result;
        if (objects.isEmpty()) {
            result = Collections.emptyList();
        } else {
            result = new ArrayList<O>();
            for (final I object : objects) {
                final Collection<O> nextCollection = extractor.extract(object);
                result.addAll(nextCollection);
            }
        }
        return result;
    }

    /**
     * <p>
     * Load zero or more <code>.properties</code> files into a combined {@link Properties} instance. If any properties
     * occur in more than one file, the values are concatenated using a comma.
     * </p>
     * 
     * @param urls the <code>.properties</code> file URLs.
     * @return {@link Properties} instance.
     * @throws IOException if an error occurs while loading any of the files.
     */
    public static Properties loadAllProperties(final Enumeration<URL> urls) throws IOException {
        final Properties allProperties = new Properties();
        while (urls.hasMoreElements()) {
            final URL url = urls.nextElement();
            final Properties properties = new Properties();
            final InputStream inputStream = url.openStream();
            try {
                properties.load(inputStream);
            } finally {
                inputStream.close();
            }
            for (final Enumeration<?> enNames = properties.propertyNames(); enNames.hasMoreElements(); ) {
                final String name = (String) enNames.nextElement();
                final String value;
                if (!allProperties.containsKey(name)) {
                    value = properties.getProperty(name);
                } else {
                    value = allProperties.getProperty(name) + ", " + properties.getProperty(name);
                }
                allProperties.setProperty(name, value);
            }
        }
        if (LOGGER.isDebugEnabled()) {
            LOGGER.debug("Loaded the following properties:");
            for (final Enumeration<?> enNames = allProperties.propertyNames(); enNames.hasMoreElements(); ) {
                final String name = (String) enNames.nextElement();
                final String value = allProperties.getProperty(name);
                LOGGER.debug(String.format("%s=%s", name, value));
            }
        }
        return allProperties;
    }

    /**
     * <p>
     * Get the element type
     * Get the component type, if any, for a given type. If the type is an array, the array component type is returned.
     * If it refers to a collection, the parameterized collection element type is returned. Otherwise, the type is
     * returned unchanged.
     * </p>
     *
     * @param type the type whose component type is to be retrieved.
     * @return {@link Type} component type.
     * @throws IllegalStateException if the type is a collection without an element type parameter.
     */
    public static Type getComponentType(final Type type) {
        final Type result;
        if (type instanceof Class) {
            final Class<?> clazz = (Class) type;
            if (clazz.isArray()) {
                result = clazz.getComponentType();
            } else if (Collection.class.isAssignableFrom(clazz) || Iterator.class.isAssignableFrom(clazz) || Enumeration.class.isAssignableFrom(clazz)) {
                throw new IllegalStateException(String.format("Unable to determine component type for %s because no type parameters were supplied.", clazz));
            } else {
                result = clazz;
            }
        } else if (type instanceof ParameterizedType) {
            final ParameterizedType parameterizedType = (ParameterizedType) type;
            final Class<?> clazz = (Class) parameterizedType.getRawType();
            if (!Collection.class.isAssignableFrom(clazz) && !Iterator.class.isAssignableFrom(clazz) && !Enumeration.class.isAssignableFrom(clazz)) {
                throw new IllegalStateException(String.format("Unable to determine component type for %s because it is not a supported collection class.", parameterizedType));
            } else {
                final Type[] typeArguments = parameterizedType.getActualTypeArguments();
                if (1 != typeArguments.length) {
                    throw new IllegalStateException(String.format("Unable to determine component type for %s because it defines the incorrect number of type arguments.", parameterizedType));
                }
                result = typeArguments[0];
            }
        } else {
            throw new IllegalStateException(String.format("Unable to determine component type for %s because it is not a raw class nor a parameterized type.", type));
        }
        return result;
    }

    /**
     * <p>
     * Get the component type for a given field.
     * </p>
     *
     * @param field the field whose component type is to be returned.
     * @return {@link Type} component type.
     * @see #getComponentType(Type) 
     */
    public static Type getComponentTypeForField(final Field field) {
        return getComponentType(field.getGenericType());
    }

    /**
     * <p>
     * Get the component type for a given method's return type.
     * </p>
     *
     * @param method the method whose return component type is to be returned.
     * @return {@link Type} component type.
     * @see #getComponentType(Type)
     */
    public static Type getComponentTypeForMethodReturnType(final Method method) {
        return getComponentType(method.getGenericReturnType());
    }
}
