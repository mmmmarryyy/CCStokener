package net.sf.jimo.loader.util;

import java.io.IOException;
import java.io.ObjectInputStream;
import java.lang.reflect.Field;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.net.MalformedURLException;
import java.net.URL;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import net.sf.jimo.common.filtermap.FilteredMap;
import net.sf.jimo.common.resolver.ResolvingMap;
import net.sf.jimo.common.resolver.VariableResolver;
import net.sf.jimo.loader.Loadable;
import net.sf.jimo.loader.Loader;
import net.sf.jimo.loader.LoaderContext;
import net.sf.jimo.loader.LoaderException;
import net.sf.jimo.loader.Messages;
import net.sf.jimo.loader.Loader.Factory;

/**
 * Id $Id: LoaderBase.java 784 2008-01-10 04:45:36Z logicfish $
 * Type LoaderBase
 * 
 * TODO Reduce the type parameters to a single parameter for the register type.
 * Refactor proxies to top level; create new package for this with proxy classes private.
 * 
 * 
 * @version $Rev$
 * @author logicfish
 * @since 0.2
 *
 * @param <ValueType>
 * @param <FilterType>
 * @param <KeyType>
 */
public abstract class LoaderBase<ValueType, FilterType, KeyType extends Map<? extends String, ? extends Object>> implements Loader<ValueType, KeyType> {

    /**
	 * This key specifies which class to initialise if any for an object proxy.
	 */
    public static final String KEY_CLASSNAME = "javaClass";

    abstract class LoadProxy {

        KeyType properties;

        public LoadProxy(KeyType properties) throws LoaderException {
            this.properties = properties;
        }

        abstract ValueType getValue() throws LoaderException;

        abstract ValueType getValueIfCached() throws LoaderException;

        abstract void onRemove(ValueType element) throws LoaderException;

        @SuppressWarnings("unchecked")
        protected ValueType createLoadable() throws LoaderException {
            ValueType newInstance;
            try {
                if (!properties.containsKey(KEY_CLASSNAME)) {
                    throw new LoaderException(Messages.formatMessage("LoaderBase.missingProperty", KEY_CLASSNAME, properties.get(Loadable.KEY_PID)));
                }
                String javaClass = (String) properties.get(KEY_CLASSNAME);
                Class<ValueType> cls = (Class<ValueType>) Class.forName(javaClass);
                newInstance = (ValueType) cls.newInstance();
                if (newInstance instanceof Loadable) {
                    Loadable element = (Loadable) newInstance;
                    element.onLoad(properties, context);
                }
            } catch (InstantiationException e) {
                throw new LoaderException(e);
            } catch (IllegalAccessException e) {
                throw new LoaderException(e);
            } catch (ClassNotFoundException e) {
                throw new LoaderException(e);
            }
            return (ValueType) newInstance;
        }

        @Override
        public boolean equals(Object obj) {
            Object item;
            try {
                item = getValueIfCached();
            } catch (LoaderException e) {
                return false;
            }
            if (item == null) {
                return false;
            }
            return item.equals(obj);
        }
    }

    ;

    class StaticLoadProxy extends LoadProxy {

        ValueType item;

        public StaticLoadProxy(KeyType map) throws LoaderException {
            super(map);
        }

        public StaticLoadProxy(ValueType item, KeyType map) throws LoaderException {
            super(map);
            this.item = item;
        }

        @Override
        ValueType getValue() throws LoaderException {
            if (item == null) {
                item = createLoadable();
            }
            return item;
        }

        @Override
        ValueType getValueIfCached() throws LoaderException {
            return item;
        }

        @Override
        void onRemove(ValueType element) throws LoaderException {
            if (item instanceof Loadable) {
                Loadable loadable = (Loadable) item;
                loadable.onUnload(properties, context);
            }
            this.item = null;
        }
    }

    ;

    class SerialLoadProxy extends LoadProxy {

        ValueType item;

        public SerialLoadProxy(KeyType map) throws LoaderException {
            super(map);
        }

        @Override
        ValueType getValue() throws LoaderException {
            if (item == null) {
                item = createLoadable();
                if (item instanceof Loadable) {
                    Loadable ini = (Loadable) item;
                    ini.onLoad(properties, context);
                }
            }
            return item;
        }

        @Override
        ValueType getValueIfCached() throws LoaderException {
            return item;
        }

        @Override
        protected ValueType createLoadable() throws LoaderException {
            try {
                if (!properties.containsKey(Loader.KEY_RESOURCEURI)) {
                    throw new LoaderException(Loader.KEY_RESOURCEURI + " not specified for object.");
                }
                String path = (String) properties.get(Loader.KEY_RESOURCEURI);
                URL url = new URL(path);
                ObjectInputStream objectInputStream = new ObjectInputStream(url.openStream());
                ValueType object = (ValueType) objectInputStream.readObject();
                return object;
            } catch (MalformedURLException e) {
                throw new LoaderException(e);
            } catch (IOException e) {
                throw new LoaderException(e);
            } catch (ClassNotFoundException e) {
                throw new LoaderException(e);
            }
        }

        @Override
        void onRemove(ValueType element) {
        }
    }

    ;

    class DynamicLoadProxy extends LoadProxy {

        public DynamicLoadProxy(KeyType map) throws LoaderException {
            super(map);
        }

        @Override
        ValueType getValue() throws LoaderException {
            return createLoadable();
        }

        @Override
        ValueType getValueIfCached() throws LoaderException {
            return null;
        }

        @Override
        void onRemove(ValueType element) {
        }
    }

    ;

    class FactoryLoadProxy extends LoadProxy {

        ValueType item;

        private Factory<ValueType, KeyType> factory;

        public FactoryLoadProxy(Factory<ValueType, KeyType> factory, KeyType map) throws LoaderException {
            super(map);
            this.factory = factory;
        }

        @Override
        ValueType getValue() throws LoaderException {
            if (item == null) {
                item = createLoadable();
                if (item instanceof Loadable) {
                    ((Loadable) item).onLoad(properties, context);
                }
            }
            return item;
        }

        @Override
        ValueType getValueIfCached() throws LoaderException {
            return item;
        }

        @Override
        protected ValueType createLoadable() throws LoaderException {
            return this.factory.getElement(properties);
        }

        @Override
        void onRemove(ValueType element) throws LoaderException {
            this.factory.releaseElement(element, properties);
            if (item instanceof Loadable) {
                ((Loadable) item).onUnload(properties, context);
            }
        }
    }

    ;

    class StaticReferenceLoadProxy extends LoadProxy {

        ValueType item;

        public StaticReferenceLoadProxy(KeyType map) throws LoaderException {
            super(map);
        }

        public StaticReferenceLoadProxy(ValueType item, KeyType map) throws LoaderException {
            super(map);
            this.item = item;
        }

        @Override
        ValueType getValue() throws LoaderException {
            if (item == null) {
                item = getStaticElement();
            }
            return item;
        }

        @Override
        ValueType getValueIfCached() throws LoaderException {
            return item;
        }

        @SuppressWarnings("unchecked")
        private ValueType getStaticElement() throws LoaderException {
            String className = (String) properties.get(KEY_STATICREFERENCECLASS);
            Class<?> cls;
            try {
                cls = Class.forName(className);
                if (properties.containsKey(KEY_STATICREFERENCEMETHOD)) {
                    String methodName = (String) properties.get(KEY_STATICREFERENCEMETHOD);
                    Method method = cls.getMethod(methodName, new Class<?>[0]);
                    return (ValueType) method.invoke(null, new Object[0]);
                }
                String fieldName = (String) properties.get(KEY_STATICREFERENCEFIELD);
                Field field = cls.getField(fieldName);
                return (ValueType) field.get(null);
            } catch (NoSuchFieldException e) {
                throw new LoaderException(e);
            } catch (ClassNotFoundException e) {
                throw new LoaderException(e);
            } catch (SecurityException e) {
                throw new LoaderException(e);
            } catch (IllegalArgumentException e) {
                throw new LoaderException(e);
            } catch (IllegalAccessException e) {
                throw new LoaderException(e);
            } catch (NoSuchMethodException e) {
                throw new LoaderException(e);
            } catch (InvocationTargetException e) {
                throw new LoaderException(e);
            }
        }

        @Override
        void onRemove(ValueType element) {
        }
    }

    ;

    LoaderContext<ValueType, KeyType> context = new LoaderContext<ValueType, KeyType>() {

        @SuppressWarnings("unchecked")
        @Override
        public <V extends ValueType> Collection<V> getLoadables(String filter) throws LoaderException {
            return (Collection<V>) LoaderBase.this.getLoadables(filter);
        }

        @SuppressWarnings("unchecked")
        @Override
        public Collection<ValueType> getLoadables(String key, KeyType properties) throws LoaderException {
            return LoaderBase.this.getLoadables(key, properties);
        }

        @Override
        public void addLoadable(ValueType element, KeyType properties) throws LoaderException {
            LoaderBase.this.addLoadable(element, properties);
        }

        @Override
        public <T> KeyType getKey(T o) {
            return LoaderBase.this.getKey(o);
        }

        @Override
        public KeyType createKey() {
            return LoaderBase.this.createKey();
        }

        @Override
        public Map<KeyType, ValueType> getMappings(String filter) throws LoaderException {
            return LoaderBase.this.getMappings(filter);
        }

        @Override
        public Map<KeyType, ValueType> getMappings(String filter, KeyType key) throws LoaderException {
            return LoaderBase.this.getMappings(filter, key);
        }

        @Override
        public void addFactory(Factory<ValueType, KeyType> factory, KeyType properties) throws LoaderException {
            LoaderBase.this.addLoadableFactory(properties, factory);
        }

        @Override
        public Collection<ValueType> getLoadables(String[] keys, KeyType[] properties) throws LoaderException {
            return LoaderBase.this.getLoadables(keys, properties);
        }

        @Override
        public Map<KeyType, ValueType> getMappings(String[] keys, KeyType[] properties) throws LoaderException {
            return LoaderBase.this.getMappings(keys, properties);
        }
    };

    protected FilteredMap<Object, FilterType, KeyType> elements;

    private static long nextId = 100;

    public LoaderBase(FilteredMap<Object, FilterType, KeyType> map) {
        elements = map;
    }

    @SuppressWarnings("unchecked")
    @Override
    public void addLoadable(ValueType element, KeyType properties) throws LoaderException {
        Long id = nextId++;
        ((Map<String, Object>) properties).put(Loadable.KEY_SID, new Long(nextId++));
        if (element != null) {
            addStaticItem((ValueType) element, properties);
            return;
        }
        LoadProxy itemProxy;
        if (properties.containsKey(Loader.KEY_DYNAMICINSTANCE) && Boolean.TRUE.equals(properties.get(Loader.KEY_DYNAMICINSTANCE))) {
            itemProxy = addDynamicItem(properties);
        } else if (properties.containsKey(Loader.KEY_SERIALINSTANCE) && Boolean.TRUE.equals(properties.get(Loader.KEY_SERIALINSTANCE))) {
            itemProxy = addSerialItem(properties);
        } else if (properties.containsKey(Loader.KEY_STATICREFERENCECLASS)) {
            itemProxy = addStaticReferenceItem(properties);
        } else if (properties.containsKey(Loader.KEY_FACTORYCLASS)) {
            try {
                Class<?> clazz = Class.forName((String) properties.get(Loader.KEY_FACTORYCLASS));
                itemProxy = addFactoryItem(properties, (Factory<ValueType, KeyType>) clazz.newInstance());
            } catch (InstantiationException e) {
                throw new LoaderException(e);
            } catch (IllegalAccessException e) {
                throw new LoaderException(e);
            } catch (ClassNotFoundException e) {
                throw new LoaderException(e);
            }
        } else {
            itemProxy = addStaticItem((ValueType) element, properties);
        }
        if (properties.containsKey(Loader.KEY_AUTOLOAD)) {
            itemProxy.getValue();
        }
    }

    @Override
    public Collection<ValueType> getLoadables(String filter) throws LoaderException {
        Collection<Object> collection;
        List<ValueType> results = new ArrayList<ValueType>();
        try {
            collection = elements.get(elements.createFilter(filter));
        } catch (Exception e) {
            throw new LoaderException(e);
        }
        for (Iterator<Object> iterator = collection.iterator(); iterator.hasNext(); ) {
            Object next = iterator.next();
            if (next instanceof LoaderBase.LoadProxy) {
                LoadProxy proxy = (LoadProxy) next;
                results.add(proxy.getValue());
            } else {
                results.add((ValueType) next);
            }
        }
        return results;
    }

    @SuppressWarnings("unchecked")
    @Override
    public Collection<ValueType> getLoadables(String key, KeyType properties) throws LoaderException {
        Collection<Object> collection;
        List<ValueType> results = new ArrayList<ValueType>();
        try {
            collection = elements.get(key, properties);
        } catch (Exception e) {
            throw new LoaderException(e);
        }
        for (Iterator<Object> iterator = collection.iterator(); iterator.hasNext(); ) {
            Object next = iterator.next();
            if (next instanceof LoaderBase.LoadProxy) {
                LoadProxy proxy = (LoadProxy) next;
                results.add(proxy.getValue());
            } else {
                results.add((ValueType) next);
            }
        }
        return results;
    }

    protected Map<KeyType, ValueType> getMappings(String filter) throws LoaderException {
        Map<KeyType, Object> collection;
        Map<KeyType, ValueType> results = new HashMap<KeyType, ValueType>();
        try {
            collection = elements.getMappings(elements.createFilter(filter));
        } catch (Exception e) {
            throw new LoaderException(e);
        }
        for (Iterator<Entry<KeyType, Object>> iterator = collection.entrySet().iterator(); iterator.hasNext(); ) {
            Entry<KeyType, Object> next = iterator.next();
            if (next.getValue() instanceof LoaderBase.LoadProxy) {
                LoadProxy proxy = (LoadProxy) next.getValue();
                results.put(next.getKey(), proxy.getValue());
            } else {
                results.put(next.getKey(), (ValueType) next.getValue());
            }
        }
        return results;
    }

    protected Map<KeyType, ValueType> getMappings(String key, KeyType properties) throws LoaderException {
        Map<KeyType, Object> collection;
        Map<KeyType, ValueType> results = new HashMap<KeyType, ValueType>();
        try {
            collection = elements.getMappings(key, properties);
        } catch (Exception e) {
            throw new LoaderException(e);
        }
        for (Iterator<Entry<KeyType, Object>> iterator = collection.entrySet().iterator(); iterator.hasNext(); ) {
            Entry<KeyType, Object> next = iterator.next();
            if (next.getValue() instanceof LoaderBase.LoadProxy) {
                LoadProxy proxy = (LoadProxy) next.getValue();
                results.put(next.getKey(), proxy.getValue());
            } else {
                results.put(next.getKey(), (ValueType) next.getValue());
            }
        }
        return results;
    }

    protected Map<KeyType, ValueType> getMappings(String[] keys, KeyType[] properties) throws LoaderException {
        Map<KeyType, Object> collection;
        Map<KeyType, ValueType> results = new HashMap<KeyType, ValueType>();
        try {
            collection = elements.getMappings(keys, properties);
        } catch (Exception e) {
            throw new LoaderException(e);
        }
        for (Iterator<Entry<KeyType, Object>> iterator = collection.entrySet().iterator(); iterator.hasNext(); ) {
            Entry<KeyType, Object> next = iterator.next();
            if (next.getValue() instanceof LoaderBase.LoadProxy) {
                LoadProxy proxy = (LoadProxy) next.getValue();
                results.put(next.getKey(), proxy.getValue());
            } else {
                results.put(next.getKey(), (ValueType) next.getValue());
            }
        }
        return results;
    }

    public Collection<ValueType> getLoadables(String[] keys, KeyType[] properties) throws LoaderException {
        Collection<Object> collection;
        List<ValueType> results = new ArrayList<ValueType>();
        try {
            collection = elements.get(keys, properties);
        } catch (Exception e) {
            throw new LoaderException(e);
        }
        for (Iterator<Object> iterator = collection.iterator(); iterator.hasNext(); ) {
            Object next = iterator.next();
            if (next instanceof LoaderBase.LoadProxy) {
                LoadProxy proxy = (LoadProxy) next;
                results.add(proxy.getValue());
            } else {
                results.add((ValueType) next);
            }
        }
        return results;
    }

    public Collection<ValueType> removeLoadables(String filter) throws LoaderException {
        try {
            return flush(elements.remove(elements.createFilter(filter)));
        } catch (Exception e) {
            throw new LoaderException(e);
        }
    }

    @SuppressWarnings("unchecked")
    @Override
    public <T> KeyType getKey(T o) {
        for (FilteredMap.Entry<Object, Object> entry : elements.entrySet()) {
            Object object = entry.getValue();
            if (object instanceof LoaderBase.LoadProxy) {
                LoadProxy element = (LoadProxy) object;
                try {
                    ValueType value = element.getValueIfCached();
                    if (o.equals(value)) {
                        return (KeyType) entry.getKey();
                    }
                } catch (LoaderException e) {
                    e.printStackTrace(System.err);
                }
            } else if (o.equals(object)) {
                return (KeyType) entry.getKey();
            }
        }
        return null;
    }

    private Collection<ValueType> flush(Collection<Object> flush) {
        ArrayList<ValueType> result = new ArrayList<ValueType>();
        for (Object object : flush) {
            if (object instanceof LoaderBase.LoadProxy) {
                LoadProxy element = (LoadProxy) object;
                try {
                    ValueType value = element.getValue();
                    element.onRemove(value);
                    result.add(value);
                } catch (LoaderException e) {
                    e.printStackTrace(System.err);
                }
            }
        }
        return result;
    }

    protected LoadProxy addStaticItem(ValueType item, KeyType properties) throws LoaderException {
        LoadProxy instance = new StaticLoadProxy(item, properties);
        elements.put(instance.properties, instance);
        return instance;
    }

    protected LoadProxy addDynamicItem(KeyType properties) throws LoaderException {
        LoadProxy instance = new DynamicLoadProxy(properties);
        elements.put(instance.properties, instance);
        return instance;
    }

    protected LoadProxy addSerialItem(KeyType propertiesMap) throws LoaderException {
        LoadProxy instance = new SerialLoadProxy(propertiesMap);
        elements.put(instance.properties, instance);
        return instance;
    }

    protected LoadProxy addStaticReferenceItem(KeyType properties) throws LoaderException {
        LoadProxy instance = new StaticReferenceLoadProxy(properties);
        elements.put(instance.properties, instance);
        return instance;
    }

    protected LoadProxy addFactoryItem(KeyType properties, Factory<ValueType, KeyType> factory) throws LoaderException {
        LoadProxy instance = new FactoryLoadProxy(factory, properties);
        elements.put(instance.properties, instance);
        return instance;
    }

    @Override
    public void addLoadableFactory(KeyType properties, Loader.Factory<ValueType, KeyType> factory) throws LoaderException {
        addFactoryItem(properties, factory);
    }

    @SuppressWarnings("unchecked")
    @Override
    public KeyType createKey() {
        Map<String, Object> map = new HashMap<String, Object>();
        Map<String, Object> propertiesMap = new ResolvingMap<String, Object>(map, new VariableResolver<Object>(map));
        return (KeyType) propertiesMap;
    }
}
