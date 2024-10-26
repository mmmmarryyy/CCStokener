package dviz.visualAspect;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.lang.reflect.Method;
import java.lang.reflect.Modifier;
import java.net.URL;
import java.util.*;
import dviz.visualAspect.MetaPropertyMap.Type;
import dviz.visualSystem.AnimationSystem;
import dviz.visualSystem.DummyAnimationSystem;
import dviz.visualSystem.impl.AnimationSystemImpl;
import dviz.visualSystem.impl.PropertyQueryable;

/**
 * @author zxq071000
 */
public class DeclarationTranslation {

    private Map<Encapsulate, Object> modelToVisual;

    private Map<Object, Encapsulate> visualToObject;

    /**
	 * @uml.property  name="classLoader"
	 * @uml.associationEnd  
	 */
    private VisualizedClassLoader classLoader;

    private Collection<AnimationSystem> animationSystems;

    private static int UNIQUE_ID = 0;

    private static class Encapsulate {

        public Object obj;

        public Encapsulate(Object o) {
            obj = o;
            if (!(o instanceof IndexObject)) {
                throw new RuntimeException("Is not an index object");
            }
        }

        @Override
        public boolean equals(Object in) {
            return obj == ((Encapsulate) in).obj;
        }

        @Override
        public int hashCode() {
            return ((IndexObject) obj).__getIndex();
        }
    }

    public static DeclarationTranslation createByXML(URL XMLInstruct) throws Exception {
        DeclarationTranslation declarationTranslation = new DeclarationTranslation();
        declarationTranslation.classLoader = new XMLClassLoader(declarationTranslation.new TranslateInvokationHandler(), XMLInstruct);
        Vector<String> igList = getIgnoreList();
        if (igList != null) for (String pre : igList) {
            System.out.println("DeclartionTranslation->createByXML() Ignoring class loading Prefix: " + pre);
            declarationTranslation.classLoader.delegateLoadingOf(pre);
        }
        return declarationTranslation;
    }

    private static Vector<String> getIgnoreList() {
        try {
            URL url = DeclarationTranslation.class.getClassLoader().getResource("ignorelist");
            BufferedReader bufferedReader = new BufferedReader(new InputStreamReader(url.openStream()));
            Vector<String> ret = new Vector<String>();
            String line = null;
            while ((line = bufferedReader.readLine()) != null) {
                ret.add(line);
            }
            return ret;
        } catch (Exception e) {
            return null;
        }
    }

    public static DeclarationTranslation createByAnnotation() throws Exception {
        DeclarationTranslation declarationTranslation = new DeclarationTranslation();
        declarationTranslation.classLoader = new AnnotationClassLoader(declarationTranslation.new TranslateInvokationHandler());
        Vector<String> igList = getIgnoreList();
        if (igList != null) for (String pre : igList) {
            System.out.println("DeclartionTranslation->createByXML() Ignoring class loading Prefix: " + pre);
            declarationTranslation.classLoader.delegateLoadingOf(pre);
        }
        return declarationTranslation;
    }

    private DeclarationTranslation() {
        modelToVisual = Collections.synchronizedMap(new HashMap<Encapsulate, Object>());
        visualToObject = Collections.synchronizedMap(new HashMap<Object, Encapsulate>());
        animationSystems = new TreeSet<AnimationSystem>();
    }

    public Object createKey(Object object) {
        if (!(object instanceof IndexObject)) throw new RuntimeException("Cannot create key for non indexobject");
        ((IndexObject) object).__setIndex(UNIQUE_ID++);
        Object key = new Key(object.getClass().getName());
        visualToObject.put(key, new Encapsulate(object));
        modelToVisual.put(new Encapsulate(object), key);
        return key;
    }

    private boolean isPrimitive(Object o) {
        if (o instanceof Integer || o instanceof Float || o instanceof Boolean || o instanceof Double || o instanceof Long || o instanceof Byte || o instanceof Character || o instanceof Short || o instanceof String) return true;
        return false;
    }

    public class TranslateInvokationHandler implements FunctionInvokeHandler {

        @Override
        public void handleFunctionCall(Invokation invokation) {
            HashMap<String, Object> params = new HashMap<String, Object>();
            for (String key : invokation.getParameters().keySet()) {
                Object obj = invokation.getParameters().get(key);
                if (!isPrimitive(obj)) {
                    obj = modelToVisual.get(new Encapsulate(obj));
                    if (obj == null) throw new RuntimeException("DeclartionTranslation->TranslateInvokationHandler() cannot find corresponding key for \n\t" + invokation.toString());
                }
                params.put(key, obj);
            }
            if (invokation.getType() == Invokation.TYPE_CONSTRUCTOR) {
                Object context = invokation.getContext();
                Object key = createKey(context);
                HashMap<String, MetaPropertyMap> propertyList = classLoader.getClazzPropertyMap().get(context.getClass().getName());
                String clazzType = invokation.visualObject;
                if (clazzType == null || clazzType.trim().length() == 0) {
                    clazzType = context.getClass().getSimpleName();
                }
                if (clazzType == null) throw new RuntimeException("Annonymous class with no class mapping definition");
                for (AnimationSystem animationSystem : animationSystems) {
                    boolean atr = false;
                    if (invokation.getConstructType().equalsIgnoreCase("main")) atr = true;
                    animationSystem.createObject(key, clazzType, params, propertyList, atr);
                }
            } else if (invokation.getType() == Invokation.TYPE_METHOD) {
                Object context = invokation.getContext();
                Object key = modelToVisual.get(new Encapsulate(context));
                String actions = invokation.getActionName();
                for (AnimationSystem animationSystem : animationSystems) {
                    animationSystem.invokeAction(key, actions, params);
                }
            }
        }
    }

    public void addAnimationSystem(AnimationSystem system) {
        if (system instanceof AnimationSystemImpl) {
            ((AnimationSystemImpl) system).getPropertyQuery().add(new TranslatePropertyQuery());
        } else {
            ((DummyAnimationSystem) system).propertyQueryable = new TranslatePropertyQuery();
        }
        animationSystems.add(system);
    }

    private Object transformArgument(Object ino) {
        if (isPrimitive(ino)) {
            return ino;
        } else {
            Object ano = modelToVisual.get(new Encapsulate(ino));
            if (ano == null) throw new RuntimeException("Property Not Mapped to Visual Object" + ino.getClass().getName());
            return ano;
        }
    }

    public class TranslatePropertyQuery implements PropertyQueryable {

        HashMap<Encapsulate, HashMap<String, Object>> propertyCache;

        public TranslatePropertyQuery() {
            propertyCache = new HashMap<Encapsulate, HashMap<String, Object>>();
        }

        @Override
        public Object queryProperty(Object key, MetaPropertyMap property) throws Exception {
            Encapsulate objRef = visualToObject.get(key);
            if (objRef == null) {
                throw new RuntimeException("Error object doesn't exist");
            }
            Object obj = objRef.obj;
            Object prop = AnnotationHelper.invokeFunction(obj, property.functionName);
            HashMap<String, Object> propCache = propertyCache.get(objRef);
            if (propCache == null) {
                propCache = new HashMap<String, Object>();
                propertyCache.put(objRef, propCache);
            }
            Object cacheValue = propCache.get(property.functionName);
            if (cacheValue != null && cacheValue.equals(prop)) return null;
            if (obj.getClass().getName().toUpperCase().indexOf("PREDATOR") != -1) {
                System.currentTimeMillis();
            }
            propCache.put(property.functionName, prop);
            return prop;
        }

        @Override
        public int getPriority() {
            return HIGH;
        }
    }

    public void translateInvoke(String clazz, String method, Object param[]) throws Exception {
        Class clz = classLoader.loadClass(clazz);
        Method mthd[] = clz.getMethods();
        Method target = null;
        for (Method m : mthd) {
            if (Modifier.isStatic(m.getModifiers()) && m.getName().equalsIgnoreCase(method)) {
                target = m;
                break;
            }
        }
        if (target == null) throw new Exception("Method not found");
        target.invoke(null, param);
    }
}
