package com.novocode.naf.resource.js;

import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.Reader;
import java.net.URL;
import java.util.ArrayList;
import java.util.Set;
import org.mozilla.javascript.Context;
import org.mozilla.javascript.ImporterTopLevel;
import org.mozilla.javascript.NativeArray;
import org.mozilla.javascript.NativeJavaClass;
import org.mozilla.javascript.NativeJavaObject;
import org.mozilla.javascript.ScriptableObject;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import com.novocode.naf.app.NAFApplication;
import com.novocode.naf.app.NAFException;
import com.novocode.naf.gui.IWindowInstanceWidget;
import com.novocode.naf.model.ModelBinding;
import com.novocode.naf.model.ModelBinding.BoundModelFactory;
import com.novocode.naf.resource.ConfPropertyDescriptor;
import com.novocode.naf.resource.ConfPropertyManager;
import com.novocode.naf.resource.ConfigurableFactory;
import com.novocode.naf.resource.ConfigurableObject;
import com.novocode.naf.resource.NGNode;
import com.novocode.naf.resource.Resource;
import com.novocode.naf.resource.ResourceLoader;
import com.novocode.naf.resource.ResourceManager;

/**
 * Reads and evaluates JavaScript files as NAF resources.
 *
 * @author Stefan Zeiger (szeiger@novocode.com)
 * @since Apr 10, 2008
 * @version $Id: JSResourceLoader.java 423 2008-05-07 15:31:16Z szeiger $
 */
public class JSResourceLoader implements ResourceLoader {

    private static final Logger LOGGER = LoggerFactory.getLogger(JSResourceLoader.class);

    private final ScriptableObject scope;

    private final ThreadLocal<Resource> threadResource = new ThreadLocal<Resource>();

    private final NAFObject nafObject;

    private NAFApplication app;

    public JSResourceLoader() {
        Context cx = Context.enter();
        try {
            scope = new ImporterTopLevel(cx);
            ScriptableObject.defineClass(scope, NAFObject.class);
            nafObject = (NAFObject) cx.newObject(scope, "NAF", new Object[] { this });
            scope.put("NAF", scope, nafObject);
            Object wrappedOut = Context.javaToJS(System.out, scope);
            ScriptableObject.putProperty(scope, "out", wrappedOut);
        } catch (Exception ex) {
            throw new NAFException("Error building JS scope", ex);
        } finally {
            Context.exit();
        }
    }

    public Resource readResource(URL url, ResourceManager resourceManager) throws NAFException {
        Resource resource = new Resource(resourceManager, url);
        InputStream in = null;
        try {
            in = url.openStream();
            Reader rd = new InputStreamReader(in, "UTF-8");
            threadResource.set(resource);
            Context cx = Context.enter();
            try {
                cx.evaluateReader(scope, rd, url.toExternalForm(), 1, null);
            } finally {
                Context.exit();
                threadResource.set(null);
            }
            return resource;
        } catch (Exception ex) {
            throw new NAFException("Error reading JavaScript resource \"" + url.toExternalForm() + "\"", ex);
        } finally {
            if (in != null) try {
                in.close();
            } catch (Exception ignored) {
            }
        }
    }

    void nafDefine(Object spec) throws NAFException {
        Resource resource = threadResource.get();
        if (resource == null) throw new NAFException("define() must only be called synchronously from within readResource()");
        if (LOGGER.isDebugEnabled()) {
            LOGGER.debug("NAF.define: [" + spec.getClass().getName() + "]");
            if (spec instanceof ScriptableObject) dump((ScriptableObject) spec, "  ");
        }
        if (!(spec instanceof ScriptableObject)) throw new NAFException("define() must be called on a JavaScript object");
        Object o = createComponent((ScriptableObject) spec, resource, null);
        resource.setRootObject(o);
    }

    void nafRunMainWindow(Object spec) throws NAFException {
        Resource resource = threadResource.get();
        if (resource == null) throw new NAFException("runMainWindow() must only be called synchronously from within readResource()");
        if (app == null) throw new NAFException("JSResourceLoader needs an associated NAFApplication to perform application functions");
        if (LOGGER.isDebugEnabled()) {
            LOGGER.debug("NAF.runMainWindow: [" + spec.getClass().getName() + "]");
            if (spec instanceof ScriptableObject) dump((ScriptableObject) spec, "  ");
        }
        if (!(spec instanceof ScriptableObject)) throw new NAFException("define() must be called on a JavaScript object");
        Object o = createComponent((ScriptableObject) spec, resource, null);
        if (!(o instanceof IWindowInstanceWidget)) throw new NAFException("NAF.runMainWindow requires an IWindowInstanceWidget");
        app.runMainWindow((IWindowInstanceWidget) o, null);
    }

    private Object createComponent(ScriptableObject spec, Resource resource, ConfigurableObject parent) throws NAFException {
        Class<?> clazz = findClassFor(spec);
        Object o = createDirectComponent(clazz, spec, resource, parent, false);
        if (o instanceof NGNode) {
            String id = ((NGNode) o).getID();
            if (id != null) resource.putObject(id, o);
        }
        return o;
    }

    private Class<?> findClassFor(ScriptableObject spec) {
        Object cls = spec.get("$", nafObject);
        Class<?> clazz;
        if (cls instanceof String) {
            try {
                clazz = Class.forName((String) cls);
            } catch (Exception ex) {
                throw new NAFException("Error finding named class " + cls, ex);
            }
        } else if (cls instanceof NativeJavaClass) clazz = ((NativeJavaClass) cls).getClassObject(); else throw new NAFException("Object has no \"$\" property to determine the class to instantiate: " + spec);
        return clazz;
    }

    private String findClassNameFor(ScriptableObject spec) {
        Object cls = spec.get("$", nafObject);
        if (cls instanceof String) return (String) cls; else return null;
    }

    private void addDirectComponent(ConfPropertyDescriptor pd, ScriptableObject spec, Resource resource, ConfigurableObject target) throws NAFException {
        Object ch;
        Class<? extends ConfigurableFactory> facClass = pd.getFactory();
        if (facClass != null) {
            ConfigurableFactory fac = createDirectComponent(facClass, spec, resource, target, true);
            ch = fac.createInstance();
            if (ch instanceof ConfigurableObject) initObject(((ConfigurableObject) ch), spec, resource, target, false);
        } else {
            if (pd.isMap()) {
                ch = pd.getValue(target);
                for (Object id : spec.getIds()) {
                    if (!(id instanceof String)) continue;
                    String name = (String) id;
                    if (name.equals("ch") || name.equals("$")) continue;
                    Object val = spec.get(name, nafObject);
                    ch = pd.addMapValue(target, resource, ch, name, val);
                }
            } else ch = createDirectComponent(pd.getInstanceType(), spec, resource, target, false);
        }
        pd.addCompound(target, ch);
    }

    private <T> T createDirectComponent(Class<T> clazz, ScriptableObject spec, Resource resource, ConfigurableObject parent, boolean ignoreUnknownElements) throws NAFException {
        T co;
        try {
            co = clazz.newInstance();
        } catch (Exception ex) {
            throw new NAFException("Error instantiating element object from class " + clazz.getName(), ex);
        }
        if (co instanceof ConfigurableObject) initObject(((ConfigurableObject) co), spec, resource, parent, ignoreUnknownElements);
        return co;
    }

    private void addChild(ConfigurableObject o, Resource resource, ConfPropertyManager propertyManager, Object val, boolean ignoreUnknownElements) {
        ConfPropertyDescriptor pd;
        if (val instanceof NativeJavaClass) {
            Class<?> ncls = ((NativeJavaClass) val).getClassObject();
            if ((pd = propertyManager.getNestedElementsPropertyDescriptor()) != null) {
                pd.addCompound(o, createDirectComponent(ncls, null, resource, o, false));
            } else if (!ignoreUnknownElements) throw new NAFException("Unrecognized child object [class " + ncls.getName() + "] in " + o.getClass().getName());
        } else if (val instanceof ScriptableObject) {
            String chcls = findClassNameFor((ScriptableObject) val);
            if ((pd = propertyManager.getElementPropertyDescriptor(chcls)) != null) addDirectComponent(pd, (ScriptableObject) val, resource, o); else {
                if ((pd = propertyManager.getNestedElementsPropertyDescriptor()) != null) {
                    pd.addCompound(o, createComponent((ScriptableObject) val, resource, o));
                } else if (!ignoreUnknownElements) throw new NAFException("Unrecognized child object " + chcls + " in " + o.getClass().getName());
            }
        } else if (val instanceof String) {
            if ((pd = propertyManager.getAttributePropertyDescriptor(":text", '$')) != null) pd.addCompound(o, val);
        } else throw new NAFException("Illegal value in \"ch\" array: " + val);
    }

    private void initObject(ConfigurableObject o, ScriptableObject spec, Resource resource, ConfigurableObject parent, boolean ignoreUnknownElements) throws NAFException {
        ConfPropertyManager propertyManager = ConfPropertyManager.forClass(o.getClass());
        if (propertyManager == null) return;
        Set<String> required = propertyManager.getRequiredCopy();
        ConfPropertyDescriptor pd = propertyManager.getAttributePropertyDescriptor(":resource", '$');
        if (pd != null) pd.setValue(o, resource);
        pd = propertyManager.getAttributePropertyDescriptor(":parent", '$');
        if (pd != null) pd.setValue(o, parent);
        if (spec == null) return;
        pd = propertyManager.getAttributePropertyDescriptor(":elementName", '$');
        if (pd != null) pd.setValue(o, spec.getClassName());
        for (Object id : spec.getIds()) {
            if (!(id instanceof String)) continue;
            String name = (String) id;
            Object val = spec.get(name, nafObject);
            if ("ch".equals(name)) {
                if (val instanceof NativeArray) {
                    NativeArray a = (NativeArray) val;
                    int len = (int) a.getLength();
                    for (int i = 0; i < len; i++) {
                        Object chval = a.get(i, nafObject);
                        addChild(o, resource, propertyManager, chval, ignoreUnknownElements);
                    }
                } else addChild(o, resource, propertyManager, val, ignoreUnknownElements);
            } else if ((pd = propertyManager.getAttributePropertyDescriptor(name, '$')) != null) {
                if (pd.isCompound()) {
                    Object map = pd.getValue(o);
                    Object jval = val;
                    if (pd.getInstanceType() == ModelBinding.class) {
                        ModelBinding mb = new ModelBinding();
                        mb.setType(pd.removePrefixFrom(name));
                        mb.setBoundModelFactory(new JSBoundModelFactory(val, scope, nafObject));
                        jval = mb;
                    } else if (jval instanceof NativeJavaObject) jval = ((NativeJavaObject) jval).unwrap();
                    map = pd.addMapValue(o, resource, map, pd.removePrefixFrom(name), jval);
                    pd.addCompound(o, map);
                } else {
                    Class<?> propType = pd.getPropertyClass();
                    if (val == null || val instanceof String) pd.setSimple(o, (String) val); else if (propType == Integer.TYPE || propType == Integer.class) {
                        if (val instanceof Integer) pd.setValue(o, val);
                        if (val instanceof Number) pd.setValue(o, ((Number) val).intValue()); else pd.setSimple(o, val.toString());
                    } else if (propType == Double.TYPE || propType == Double.class) {
                        if (val instanceof Double) pd.setValue(o, val);
                        if (val instanceof Number) pd.setValue(o, ((Number) val).doubleValue()); else pd.setSimple(o, val.toString());
                    } else if (propType == Boolean.TYPE || propType == Boolean.class) {
                        if (val instanceof Boolean) pd.setValue(o, val); else pd.setSimple(o, val.toString());
                    } else if (propType == BoundModelFactory.class) {
                        pd.setValue(o, new JSBoundModelFactory(val, scope, nafObject));
                    } else pd.setSimple(o, val.toString());
                    if (required != null) required.remove(pd.getXMLPropertyName());
                }
            }
        }
        if (required != null && required.size() > 0) {
            String s = new ArrayList<String>(required).toString();
            throw new NAFException("Required attributes missing for " + o.getClass().getName() + ": " + s);
        }
    }

    public void setApplication(NAFApplication app) {
        this.app = app;
    }

    public static void addTo(NAFApplication app) {
        ResourceManager rm = app.getResourceManager();
        JSResourceLoader l = new JSResourceLoader();
        l.setApplication(app);
        rm.setLoaderForSuffix("js", l);
    }

    private static void dump(ScriptableObject o, String indent) {
        for (Object id : o.getIds()) {
            Object val = (id instanceof Integer) ? o.get((Integer) id, o) : o.get((String) id, o);
            if (val instanceof ScriptableObject) {
                LOGGER.debug(indent + id + ": [" + val.getClass().getName() + "]");
                dump((ScriptableObject) val, indent + "  ");
            } else {
                LOGGER.debug(indent + id + ": [" + val.getClass().getName() + "] " + val);
            }
        }
    }
}
