package com.doxological.doxquery;

import java.io.*;
import java.util.*;
import java.net.*;
import com.doxological.doxquery.context.*;
import com.doxological.doxquery.types.*;
import com.doxological.doxquery.utils.XQueryConstants;
import com.doxological.doxquery.grammar.Module;
import com.doxological.doxquery.parser.XQueryParser;

/**
 * <p>The factory class and parsed entity holder for the system.</p>
 *
 * @author John Snelson
 */
public class XQueryEnvironment {

    /**
	 * <p>Override to provide the location of the required entity.</p>
	 *
	 * @author John Snelson
	 */
    public class Resolver {

        /**
		 * Override to resolve a schema from it's target namespace and a location
		 * @param targetNamespace The schema's target namespace. This can be a zero length string if
		 * the schema has no target namespace
		 * @param location This is a location hint for finding the schema. It may be null.
		 * @return an InputStream to the schema, or null if the schema was not resolved.
		 */
        public InputStream resolveSchema(String targetNamespace, String location) {
            return null;
        }

        /**
		 * Override to resolve a module from it's target namespace and a location
		 * @param targetNamespace The module's target namespace. This can be a zero length string if
		 * the module is in the empty namespace
		 * @param location This is a location hint for finding the module. It may be null.
		 * @return an InputStream to the module, or null if the module was not resolved.
		 */
        public InputStream resolveModule(String targetNamespace, String location) {
            return null;
        }
    }

    public Map<String, Resolver> resolvers_ = new HashMap<String, Resolver>();

    public Map<String, Schema> schemas_ = new HashMap<String, Schema>();

    public Map<String, Module> modules_ = new HashMap<String, Module>();

    /**
	 * Simple constructor
	 */
    public XQueryEnvironment() {
        addResolver("url-resolver", new Resolver() {

            private InputStream resolve(String location) {
                try {
                    URL url = new URL(location);
                    return url.openStream();
                } catch (MalformedURLException e) {
                } catch (IOException e) {
                }
                return null;
            }

            public InputStream resolveSchema(String targetNamespace, String location) {
                return (location == null ? null : resolve(location));
            }

            public InputStream resolveModule(String targetNamespace, String location) {
                return (location == null ? null : resolve(location));
            }
        });
        addResolver("classloader-resolver", new Resolver() {

            private InputStream resolve(String location) {
                InputStream result = this.getClass().getClassLoader().getResourceAsStream(location);
                if (result == null) {
                    result = ClassLoader.getSystemResourceAsStream(location);
                }
                return result;
            }

            public InputStream resolveSchema(String targetNamespace, String location) {
                return (location == null ? null : resolve(location));
            }

            public InputStream resolveModule(String targetNamespace, String location) {
                return (location == null ? null : resolve(location));
            }
        });
        addResolver("current-directory-resolver", new Resolver() {

            private InputStream resolve(String location) {
                try {
                    return new FileInputStream(location);
                } catch (FileNotFoundException e) {
                }
                return null;
            }

            public InputStream resolveSchema(String targetNamespace, String location) {
                return (location == null ? null : resolve(location));
            }

            public InputStream resolveModule(String targetNamespace, String location) {
                return (location == null ? null : resolve(location));
            }
        });
        addSchema(XQueryConstants.XMLSCHEMA_NAMESPACE, "com/doxological/doxquery/builtin.schema");
    }

    public Schema getSchema(String targetNamespace) {
        return getSchema(targetNamespace, null);
    }

    public Schema getSchema(String targetNamespace, Collection<String> locations) {
        synchronized (schemas_) {
            Schema result = schemas_.get(targetNamespace);
            if (result != null) return result;
            if (locations != null) {
                result = parseSchema(targetNamespace, locations);
                if (result != null && result.getTargetNamespace() != targetNamespace && !result.getTargetNamespace().equals(targetNamespace)) {
                    throw new XQueryException("XQ0059");
                }
            }
            return result;
        }
    }

    public Collection<String> getSchemaTargetNamespaces() {
        return Collections.unmodifiableCollection(schemas_.keySet());
    }

    public void addSchema(Schema schema) {
        synchronized (schemas_) {
            schemas_.put(schema.getTargetNamespace(), schema);
        }
    }

    public void addSchema(String targetNamespace, String location) {
        synchronized (schemas_) {
            Schema schema = parseSchema(targetNamespace, Collections.singleton(location));
            if (schema != null) addSchema(schema);
        }
    }

    public void addSchema(String resourceName, InputStream stream) {
        synchronized (schemas_) {
            Schema schema = parseSchema(resourceName, stream);
            if (schema != null) addSchema(schema);
        }
    }

    public void removeSchema(String location) {
        synchronized (schemas_) {
            schemas_.remove(location);
        }
    }

    public Schema parseSchema(String targetNamespace, Collection<String> locations) {
        String location = targetNamespace;
        InputStream stream = resolveSchema(targetNamespace, null);
        Iterator<String> i = locations.iterator();
        while (i.hasNext() && stream == null) {
            location = i.next();
            stream = resolveSchema(targetNamespace, location);
        }
        if (stream == null) return null;
        return parseSchema(location, stream);
    }

    public Schema parseSchema(String resourceName, InputStream stream) {
        SchemaParser parser = new SchemaParser(this);
        Schema schema = parser.parse(resourceName, stream);
        return schema;
    }

    public Module getModule(String targetNamespace) {
        return getModule(targetNamespace, null);
    }

    public Module getModule(String targetNamespace, Collection<String> locations) {
        synchronized (modules_) {
            Module result = modules_.get(targetNamespace);
            if (result != null) return result;
            if (locations != null) {
                result = parseModule(targetNamespace, locations, null);
                if (result != null && result.getTargetNamespace() != targetNamespace && !result.getTargetNamespace().equals(targetNamespace)) {
                    throw new XQueryException("XQ0059");
                }
            }
            return result;
        }
    }

    public Collection<String> getModuleTargetNamespaces() {
        return Collections.unmodifiableCollection(modules_.keySet());
    }

    public void addModule(Module module) {
        synchronized (modules_) {
            modules_.put(module.getTargetNamespace(), module);
        }
    }

    public void addModule(String targetNamespace, String location) {
        synchronized (modules_) {
            Module module = parseModule(targetNamespace, Collections.singleton(location), null);
            if (module != null) addModule(module);
        }
    }

    public void addModule(String resourceName, InputStream stream) {
        synchronized (modules_) {
            Module module = parseModule(resourceName, stream, null);
            if (module != null) addModule(module);
        }
    }

    public void removeModule(String location) {
        synchronized (modules_) {
            modules_.remove(location);
        }
    }

    public Module parseModule(String targetNamespace, Collection<String> locations, MutableStaticContext sContext) {
        String location = targetNamespace;
        InputStream stream = resolveModule(targetNamespace, null);
        Iterator<String> i = locations.iterator();
        while (i.hasNext() && stream == null) {
            location = i.next();
            stream = resolveModule(targetNamespace, location);
        }
        if (stream == null) return null;
        return parseModule(location, stream, sContext);
    }

    public Module parseModule(String resourceName, InputStream stream, MutableStaticContext sContext) {
        XQueryParser parser = createParser();
        Module module = parser.parseModule(resourceName, stream, sContext);
        return module;
    }

    public Resolver getResolver(String name) {
        return resolvers_.get(name);
    }

    public Collection<String> getResolverNames() {
        return resolvers_.keySet();
    }

    public void addResolver(String name, Resolver resolver) {
        resolvers_.put(name, resolver);
    }

    public void removeResolver(String name) {
        resolvers_.remove(name);
    }

    private InputStream resolveSchema(String targetNamespace, String location) {
        for (Resolver r : resolvers_.values()) {
            InputStream result = r.resolveSchema(targetNamespace, location);
            if (result != null) return result;
        }
        return null;
    }

    private InputStream resolveModule(String targetNamespace, String location) {
        for (Resolver r : resolvers_.values()) {
            InputStream result = r.resolveModule(targetNamespace, location);
            if (result != null) return result;
        }
        return null;
    }

    public XQueryParser createParser() {
        return new XQueryParser(this);
    }

    public XQueryExpression parse(File file) {
        return createParser().parse(file);
    }

    public XQueryExpression parse(String resourceName, InputStream stream) {
        return createParser().parse(resourceName, stream);
    }

    public XQueryExpression parse(File file, MutableStaticContext sContext) {
        return createParser().parse(file, sContext);
    }

    public XQueryExpression parse(String resourceName, InputStream stream, MutableStaticContext sContext) {
        return createParser().parse(resourceName, stream, sContext);
    }

    public MutableStaticContext createStaticContext() {
        return new DefaultStaticContext(this);
    }
}
