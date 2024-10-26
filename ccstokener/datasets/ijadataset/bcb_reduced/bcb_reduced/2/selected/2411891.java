package org.iocframework.hrdt;

import java.io.IOException;
import java.io.InputStream;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.lang.reflect.Modifier;
import java.net.URL;
import java.text.ParseException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.Map.Entry;
import org.iocframework.ArrayMaker;
import org.iocframework.Singleton;
import org.iocframework.Factory.Worker;
import org.iocframework.conf.ExternalConfigException;
import com.taliasplayground.hrdt.objects.BranchValue;
import com.taliasplayground.hrdt.objects.DTArray;
import com.taliasplayground.hrdt.objects.DTObject;
import com.taliasplayground.hrdt.objects.ExpandedName;
import com.taliasplayground.hrdt.parsers.ObjectParser;
import com.taliasplayground.lang.Assert;
import com.taliasplayground.lang.ClassLoaderUtils;
import com.taliasplayground.lang.ClassUtils;
import com.taliasplayground.lang.Pair;
import com.taliasplayground.text.SyntaxException;

/**
 * @author David M. Sledge
 */
public class Staffing {

    private static final Class<?>[] OBJ_RECRUITER_ARG_TYPES = { DTObject.class, Context.class };

    private static final Class<?>[] PROP_RECRUITER_ARG_TYPES = { DTObject.class, ExpandedName.class, Object.class, Context.class };

    public static final ExpandedName OBJ_RECRUITERS = new ExpandedName("objRecruiters");

    public static final ExpandedName PROP_RECRUITERS = new ExpandedName("propRecruiters");

    /**
     * <p>
     * </p>
     * 
     * @author David M. Sledge
     */
    public enum ContextKey {

        OBJ_RECRUITER_MAP, PROP_RECRUITER_MAP, PATH_MAP
    }

    private static Pair<URL, DTObject> loadRecruitersConf(URL url) throws ExternalConfigException, SyntaxException, IOException {
        Assert.notNullArg(url, "resourceName may not be null");
        InputStream is = url.openStream();
        try {
            Object value = ObjectParser.parse(is);
            if (!(value instanceof DTObject)) {
                throw new ExternalConfigException("The global value in " + url + " must be a DTObject");
            }
            return new Pair<URL, DTObject>(url, (DTObject) value);
        } finally {
            is.close();
        }
    }

    private static Set<? extends Pair<URL, DTObject>> loadRecruitersConfs(URL... urls) throws SyntaxException, ExternalConfigException {
        Assert.notNullArg(urls, "'urls' may not be null");
        Set<Pair<URL, DTObject>> set = new LinkedHashSet<Pair<URL, DTObject>>();
        for (URL url : urls) {
            try {
                set.add(loadRecruitersConf(url));
            } catch (IOException e) {
                throw new ExternalConfigException("Unable to load recruiting" + " class map from location [" + url + "]", e);
            }
        }
        return set;
    }

    /**
     * @param recruitingClass
     * @param methodName
     * @return
     * @throws ExternalConfigException
     * @throws NoSuchMethodException
     */
    public static Method getObjectRecruiterMethod(Class<?> recruitingClass, String methodName) throws ExternalConfigException, NoSuchMethodException {
        Assert.notNullArg(recruitingClass, "recruitingClass may not be null.");
        Assert.notNullArg(methodName, "methodName may not be null.");
        Method method = recruitingClass.getMethod(methodName, OBJ_RECRUITER_ARG_TYPES);
        if (!Modifier.isStatic(method.getModifiers())) {
            throw new ExternalConfigException("Method " + method + " is not static");
        }
        return method;
    }

    /**
     * @param recruitingClass
     * @param methodName
     * @return
     * @throws ExternalConfigException
     * @throws NoSuchMethodException
     */
    public static Method getPropertyRecruiterMethod(Class<?> recruitingClass, String methodName) throws ExternalConfigException, NoSuchMethodException {
        Assert.notNullArg(recruitingClass, "recruitingClass may not be null.");
        Assert.notNullArg(methodName, "methodName may not be null.");
        Method method = recruitingClass.getMethod(methodName, PROP_RECRUITER_ARG_TYPES);
        if (!Modifier.isStatic(method.getModifiers())) {
            throw new ExternalConfigException("Method " + method + " is not static");
        }
        return method;
    }

    /**
     * @param qualifiedMethodName
     * @return
     * @throws ParseException
     * @throws ClassNotFoundException
     * @throws NoSuchMethodException
     * @throws ExternalConfigException
     */
    public static Method stringToObjectRecruiterMethod(String qualifiedMethodName) throws ParseException, ClassNotFoundException, NoSuchMethodException, ExternalConfigException {
        int methodNameIndex = qualifiedMethodName.lastIndexOf('.');
        if (methodNameIndex == -1) {
            throw new ParseException("The method name " + qualifiedMethodName + " is not fully qualified.", methodNameIndex);
        }
        String methodName = qualifiedMethodName.substring(methodNameIndex + 1);
        Class<?> recruitingClass = ClassUtils.getClass(Staffing.class, qualifiedMethodName.substring(0, methodNameIndex));
        return getObjectRecruiterMethod(recruitingClass, methodName);
    }

    /**
     * @param qualifiedMethodName
     * @return
     * @throws ParseException
     * @throws ClassNotFoundException
     * @throws NoSuchMethodException
     * @throws ExternalConfigException
     */
    public static Method stringToPropertyRecruiterMethod(String qualifiedMethodName) throws ParseException, ClassNotFoundException, NoSuchMethodException, ExternalConfigException {
        int methodNameIndex = qualifiedMethodName.lastIndexOf('.');
        if (methodNameIndex == -1) {
            throw new ParseException("The method name " + qualifiedMethodName + " is not fully qualified.", methodNameIndex);
        }
        String methodName = qualifiedMethodName.substring(methodNameIndex + 1);
        Class<?> recruitingClass = ClassUtils.getClass(Staffing.class, qualifiedMethodName.substring(0, methodNameIndex));
        return getPropertyRecruiterMethod(recruitingClass, methodName);
    }

    public static Map<? extends ExpandedName, ?> getPropertyRecruiters(DTObject recruitConf, PathStack pathStack) throws ParseException, ClassNotFoundException, NoSuchMethodException, ExternalConfigException {
        if (recruitConf == null) {
            return Collections.emptyMap();
        }
        Map<ExpandedName, Object> propTmplts = new HashMap<ExpandedName, Object>();
        for (Entry<? extends ExpandedName, ?> entry : recruitConf.getPropertiesSet()) {
            ExpandedName expName = entry.getKey();
            Object value = entry.getValue();
            if (value instanceof BranchValue) {
                propTmplts.put(expName, new Pair<PathStack, BranchValue>(new PathStack(pathStack), (BranchValue) value));
            } else if (value instanceof String) {
                propTmplts.put(expName, stringToPropertyRecruiterMethod((String) value));
            } else {
                throw new ExternalConfigException(pathStack.toString() + ":  the value of the '" + expName + "' property must" + " be either a string representing the fully qualified" + " name of the recruiting method (package.Class.method" + "), a DTArray, or a DTObject; found " + value + (value == null ? "" : " of type " + value.getClass()));
            }
        }
        return propTmplts;
    }

    public static Map<? extends ExpandedName, ?> getObjectRecruiters(DTObject recruitConf, PathStack pathStack) throws ParseException, ClassNotFoundException, NoSuchMethodException, ExternalConfigException {
        if (recruitConf == null) {
            return Collections.emptyMap();
        }
        Map<ExpandedName, Object> propTmplts = new HashMap<ExpandedName, Object>();
        for (Entry<? extends ExpandedName, ?> entry : recruitConf.getPropertiesSet()) {
            ExpandedName expName = entry.getKey();
            Object value = entry.getValue();
            if (value instanceof BranchValue) {
                propTmplts.put(expName, new Pair<PathStack, BranchValue>(new PathStack(pathStack), (BranchValue) value));
            } else if (value instanceof String) {
                propTmplts.put(expName, stringToObjectRecruiterMethod((String) value));
            } else {
                throw new ExternalConfigException(pathStack.toString() + ":  the value of the '" + expName + "' property must" + " be either a string representing the fully qualified" + " name of the recruiting method (package.Class.method" + "), a DTArray, or a DTObject; found " + value + (value == null ? "" : " of type " + value.getClass()));
            }
        }
        return propTmplts;
    }

    public static Pair<Map<? extends ExpandedName, ?>, Map<? extends ExpandedName, ?>> loadRecruitersInClasspath(String recruiterConfLoc) throws ExternalConfigException, ParseException, ClassNotFoundException, NoSuchMethodException, IOException {
        List<? extends URL> urlList = ClassLoaderUtils.DEFAULT_IMPL.getResources(recruiterConfLoc, Staffing.class);
        return loadRecruiters(urlList.toArray(new URL[urlList.size()]));
    }

    public static Pair<Map<? extends ExpandedName, ?>, Map<? extends ExpandedName, ?>> loadRecruiters(URL... urls) throws ExternalConfigException, ParseException, ClassNotFoundException, NoSuchMethodException {
        Map<ExpandedName, Object> objRecruiters = new HashMap<ExpandedName, Object>();
        Map<ExpandedName, Object> propRecruiters = new HashMap<ExpandedName, Object>();
        for (Pair<URL, DTObject> pair : loadRecruitersConfs(urls)) {
            PathStack pStack = new PathStack(pair.get1().toString());
            DTObject values = pair.get2();
            Object value = values.get(OBJ_RECRUITERS);
            if (value != null) {
                if (!(value instanceof DTObject)) {
                    throw new ExternalConfigException();
                }
                objRecruiters.putAll(getObjectRecruiters((DTObject) value, pStack));
            }
            value = values.get(PROP_RECRUITERS);
            if (value != null) {
                if (!(value instanceof DTObject)) {
                    throw new ExternalConfigException();
                }
                propRecruiters.putAll(getPropertyRecruiters((DTObject) value, pStack));
            }
        }
        return new Pair<Map<? extends ExpandedName, ?>, Map<? extends ExpandedName, ?>>(objRecruiters, propRecruiters);
    }

    /**
     * @param value
     * @param context
     * @return
     * @throws ExternalConfigException
     */
    @SuppressWarnings("unchecked")
    private static Worker hireWorker(Object value, Context context) throws ExternalConfigException {
        Worker worker;
        if (value instanceof DTObject) {
            DTObject jobDescr = (DTObject) value;
            ExpandedName[] expNames = jobDescr.getNames().toArray(new ExpandedName[jobDescr.getSize()]);
            for (int i = expNames.length - 1; i >= 0; i--) {
                ExpandedName expName = expNames[i];
                if (expName.getNs() != null) {
                    value = jobDescr.remove(expName);
                    return delegateHiring(jobDescr, expName, value, context);
                }
            }
            return delegateHiring(jobDescr, context);
        }
        if (value instanceof DTArray) {
            DTArray dtArray = (DTArray) value;
            Worker[] workers = new Worker[dtArray.getLength()];
            int index = 0;
            for (Object object : dtArray) {
                workers[index++] = context.hireWorker(object);
            }
            worker = new ArrayMaker(workers, ((Map<? extends BranchValue, ? extends String>) context.get(Staffing.ContextKey.PATH_MAP)).get(dtArray));
            return worker;
        }
        worker = new Singleton(value, null);
        return worker;
    }

    /**
     * @param jobDescr
     * @param context
     * @return
     * @throws ExternalConfigException
     */
    @SuppressWarnings("unchecked")
    private static Worker delegateHiring(DTObject jobDescr, Context context) throws ExternalConfigException {
        ExpandedName eName = jobDescr.getClassName();
        Object recruiter = ((Map<? extends ExpandedName, ?>) context.get(ContextKey.OBJ_RECRUITER_MAP)).get(eName);
        Map<BranchValue, String> pathMap = (Map<BranchValue, String>) context.get(ContextKey.PATH_MAP);
        if (recruiter instanceof Pair<?, ?>) {
            Pair<PathStack, BranchValue> pair = (Pair<PathStack, BranchValue>) recruiter;
            return context.hireWorker(JobDescriptionExpander.expandObjJobDescription(jobDescr, pathMap, pair.get2(), pair.get1().getResourceString()));
        }
        if (recruiter instanceof Method) {
            try {
                return (Worker) ((Method) recruiter).invoke(null, jobDescr, context);
            } catch (RuntimeException e) {
                throw e;
            } catch (IllegalAccessException e) {
                throw new ExternalConfigException(e);
            } catch (InvocationTargetException e) {
                Throwable t = e.getCause();
                if (t instanceof RuntimeException) {
                    throw (RuntimeException) t;
                }
                if (t instanceof ExternalConfigException) {
                    throw (ExternalConfigException) t;
                }
                throw new ExternalConfigException(t);
            }
        }
        throw new ExternalConfigException(pathMap.get(jobDescr) + ":  No recruiter found for " + eName);
    }

    /**
     * @param jobDescr
     * @param propertyName
     * @param value
     * @param context
     * @return
     * @throws ExternalConfigException
     */
    @SuppressWarnings("unchecked")
    private static Worker delegateHiring(DTObject jobDescr, ExpandedName propertyName, Object value, Context context) throws ExternalConfigException {
        Object recruiter = ((Map<? extends ExpandedName, ?>) context.get(ContextKey.PROP_RECRUITER_MAP)).get(propertyName);
        Map<BranchValue, String> pathMap = (Map<BranchValue, String>) context.get(ContextKey.PATH_MAP);
        if (recruiter instanceof Pair<?, ?>) {
            Pair<PathStack, BranchValue> pair = (Pair<PathStack, BranchValue>) recruiter;
            return context.hireWorker(JobDescriptionExpander.expandPropJobDescription(jobDescr, propertyName, value, pathMap, pair.get2(), pair.get1().getResourceString()));
        }
        if (recruiter instanceof Method) {
            try {
                return (Worker) ((Method) recruiter).invoke(null, jobDescr, propertyName, value, context);
            } catch (RuntimeException e) {
                throw e;
            } catch (IllegalAccessException e) {
                throw new ExternalConfigException(e);
            } catch (InvocationTargetException e) {
                Throwable t = e.getCause();
                if (t instanceof RuntimeException) {
                    throw (RuntimeException) t;
                }
                if (t instanceof ExternalConfigException) {
                    throw (ExternalConfigException) t;
                }
                throw new ExternalConfigException(t);
            }
        }
        throw new ExternalConfigException(pathMap.get(jobDescr) + ":  No recruiter found for " + propertyName);
    }

    /**
     * <p>
     * </p>
     * 
     * @author David M. Sledge
     */
    public static class Context {

        private final Map<Object, List<Object>> map = new HashMap<Object, List<Object>>();

        /**
         * <p>
         * </p>
         * 
         * @param conditions
         */
        public Context(Map<?, ?> conditions) {
            if (conditions != null) {
                for (Entry<?, ?> entry : conditions.entrySet()) {
                    Object key = entry.getKey();
                    List<Object> stack = map.get(key);
                    if (stack == null) {
                        stack = new ArrayList<Object>();
                        map.put(key, stack);
                    }
                    stack.add(entry.getValue());
                }
            }
        }

        /**
         * <p>
         * </p>
         */
        public Context() {
        }

        /**
         * <p>
         * </p>
         * 
         * @param jobDescr
         * @param key
         * @param condition
         * @return
         * @throws ExternalConfigException
         * @throws Throwable
         */
        public Worker hireWorker(Object jobDescr, Object key, Object condition) throws ExternalConfigException {
            Map<Object, Object> conditions = new HashMap<Object, Object>();
            conditions.put(key, condition);
            return hireWorker(jobDescr, conditions);
        }

        public Worker hireWorker(Object jobDescr) throws ExternalConfigException {
            return hireWorker(jobDescr, null);
        }

        /**
         * <p>
         * </p>
         * 
         * @param jobDescr
         * @param conditions
         * @return
         * @throws ExternalConfigException
         * @throws Throwable
         */
        public Worker hireWorker(Object jobDescr, Map<?, ?> conditions) throws ExternalConfigException {
            if (conditions != null) {
                for (Entry<?, ?> entry : conditions.entrySet()) {
                    Object key = entry.getKey();
                    List<Object> stack = map.get(key);
                    if (stack == null) {
                        stack = new ArrayList<Object>();
                        map.put(key, stack);
                    }
                    stack.add(entry.getValue());
                }
            }
            try {
                return Staffing.hireWorker(jobDescr, this);
            } finally {
                if (conditions != null) {
                    for (Object key : conditions.keySet()) {
                        List<Object> stack = map.get(key);
                        stack.remove(stack.size() - 1);
                        if (stack.size() == 0) {
                            map.remove(key);
                        }
                    }
                }
            }
        }

        /**
         * <p>
         * </p>
         * 
         * @param key
         * @return
         */
        public Object get(Object key) {
            List<Object> stack = map.get(key);
            return stack == null || stack.size() == 0 ? null : stack.get(stack.size() - 1);
        }

        public List<?> getStack(Object key) {
            List<?> list = map.get(key);
            return list == null ? null : Collections.unmodifiableList(list);
        }

        /**
         * <p>
         * </p>
         * 
         * @param key
         * @return
         */
        public boolean containsKey(Object key) {
            return map.containsKey(key);
        }

        /**
         * <p>
         * </p>
         * 
         * @return
         */
        public Map<?, ? extends List<?>> getConditions() {
            Map<Object, List<?>> newMap = new HashMap<Object, List<?>>();
            for (Entry<Object, List<Object>> entry : map.entrySet()) {
                newMap.put(entry.getKey(), Collections.unmodifiableList(entry.getValue()));
            }
            return Collections.unmodifiableMap(newMap);
        }
    }
}
