package mfinder.config;

import java.beans.IntrospectionException;
import java.beans.PropertyDescriptor;
import java.io.*;
import java.lang.reflect.Constructor;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Modifier;
import java.net.URL;
import java.util.*;
import javax.xml.parsers.DocumentBuilderFactory;
import mfinder.ActionFactory;
import mfinder.impl.DefaultActionFactory;
import mfinder.impl.Injector;
import mfinder.util.ClassUtil;
import mfinder.util.CollectionUtil;
import mfinder.util.StringUtil;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.Node;

/**
 * 启动mfinder容器的入口配置类。通过Configuration类加载mfinder的配置文件（默认为mfinder-1.5.xml）初始化ActionFactory
 * 及加载相应的属性配置，最终得到ActionFactory具体实例。 <p> 如果mfinder.xml中未指明ActionFactory的具体实现类，则默认使用{@link DefaultActionFactory
 * }。 </p> <p> 通常如下使用：
 * <code><blockquote><pre>
 * Configuration config = new Configuration().load(URL url);
 * ActionFactory factory = config.buildActionFactory();
 * factory...
 * </pre></blockquote></code> </p>
 *
 * @see #buildActionFactory()
 */
public class Configuration implements Serializable {

    private static final long serialVersionUID = 1L;

    private static final Logger LOG = LoggerFactory.getLogger(Configuration.class);

    /**
     * 默认xml文件的名称
     */
    public static final String MFINDER_XML = "mfinder-1.5.xml";

    /**
     * 默认xsd文件的名称
     */
    public static final String MFINDER_XSD = "mfinder-1.5.xsd";

    /**
     * 配置文件中表示ActionFactory的标签名
     */
    public static final String ACTION_FACTORY = "action-factory";

    /**
     * 配置文件中表示属性的标签属性
     */
    public static final String PROPERTY = "property";

    /**
     * 配置文件中表示类名称的标签属性
     */
    public static final String CLASS = "class";

    /**
     * 配置文件中表示值的标签属性
     */
    public static final String VALUE = "value";

    /**
     * 配置文件中表示名称的标签属性
     */
    public static final String NAME = "name";

    /**
     * 配置文件中表示拦截器的标签名
     */
    public static final String INTERCEPTOR = "interceptor";

    /**
     * 配置文件中表示拦截栈的标签名
     */
    public static final String INTERCEPTOR_STACK = "interceptor-stack";

    /**
     * 配置文件中表示结果类型的标签名
     */
    public static final String RESULT_TYPE = "result-type";

    /**
     * 配置文件中表示结果对象的标签名
     */
    public static final String RESULT = "result";

    /**
     * 配置文件中表示Action的标签名
     */
    public static final String ACTION = "action";

    /**
     * 配置文件中表示path的标签名
     */
    public static final String PATH = "path";

    /**
     * 配置文件中表示包含其它配置的标签名
     */
    public static final String INCLUDE = "include";

    /**
     * 配置文件中表示文件名称的标签属性
     */
    public static final String FILE = "file";

    /**
     * 配置文件中表示扫描组件的标签名
     */
    public static final String COMPONENT_SCAN = "component-scan";

    /**
     * 配置文件中表示包名称的标签属性
     */
    public static final String PACKAGE = "package";

    /**
     * 配置文件中表示不包含的标签属性
     */
    public static final String EXCLUDE = "exclude";

    private Class<? extends ActionFactory> actionFactoryClass = DefaultActionFactory.class;

    /**
     * ActionFactory的属性
     */
    private Map<String, Object> actionFactoryProperties;

    /**
     * 自动检测的类集合
     */
    private Set<Class<?>> scanComponents;

    /**
     * 排除的包或类
     */
    private Set<String> excludePackages;

    /**
     * interceptors' class or object
     */
    private Set<Object> interceptors;

    /**
     * interceptors' properties
     */
    private Map<Class<?>, Map<String, Object>> interceptorProperties;

    /**
     * interceptorStacks' class or object
     */
    private Set<Object> interceptorStacks;

    /**
     * interceptorStacks' properties
     */
    private Map<Class<?>, Map<String, Object>> interceptorStackProperties;

    /**
     * resultTypes' class or object
     */
    private Set<Object> resultTypes;

    /**
     * resultTypes' properties
     */
    private Map<Class<?>, Map<String, Object>> resultTypeProperties;

    /**
     * results' class or object
     */
    private Set<Object> results;

    /**
     * results' properties
     */
    private Map<Class<?>, Map<String, Object>> resultProperties;

    /**
     * actions' class or object
     */
    private Set<Object> actions;

    /**
     * actions' properties
     */
    private Map<Class<?>, Map<String, Object>> actionProperties;

    /**
     * path - action class map
     */
    private Map<String, Class<?>> pathActions;

    /**
     * path actions' properties
     */
    private Map<String, Map<String, Object>> pathProperties;

    /**
     * Constructor with initialization.
     */
    public Configuration() {
        reset();
    }

    /**
     * initiate or reset the collections.
     */
    protected void reset() {
        actionFactoryProperties = new LinkedHashMap<String, Object>();
        scanComponents = new LinkedHashSet<Class<?>>();
        excludePackages = new HashSet<String>();
        interceptors = new LinkedHashSet<Object>();
        interceptorProperties = new LinkedHashMap<Class<?>, Map<String, Object>>();
        interceptorStacks = new LinkedHashSet<Object>();
        interceptorStackProperties = new LinkedHashMap<Class<?>, Map<String, Object>>();
        resultTypes = new LinkedHashSet<Object>();
        resultTypeProperties = new LinkedHashMap<Class<?>, Map<String, Object>>();
        results = new LinkedHashSet<Object>();
        resultProperties = new LinkedHashMap<Class<?>, Map<String, Object>>();
        actions = new LinkedHashSet<Object>();
        actionProperties = new LinkedHashMap<Class<?>, Map<String, Object>>();
        pathActions = new LinkedHashMap<String, Class<?>>();
        pathProperties = new LinkedHashMap<String, Map<String, Object>>();
    }

    /**
     * 加载默认配置文件{@link #MFINDER_XML}。
     *
     * @return 此配置对象的引用。
     *
     * @throws ConfigurationException 如果发生配置错误。
     */
    public Configuration load() throws ConfigurationException {
        return load(MFINDER_XML);
    }

    /**
     * 从指定的URL对象加载配置。
     *
     * @param url 指定的URL。
     *
     * @return 此配置对象的引用。
     *
     * @throws ConfigurationException 如果发生配置错误。
     */
    public Configuration load(URL url) throws ConfigurationException {
        LOG.info("Configuring from url : " + url.toString());
        try {
            return load(url.openStream(), url.toString());
        } catch (IOException ioe) {
            throw new ConfigurationException("Could not configure from URL : " + url, ioe);
        }
    }

    /**
     * 从指定的资源路径加载配置。
     *
     * @param resource 指定的资源路径。
     *
     * @return 此配置对象的引用。
     *
     * @throws ConfigurationException 如果发生配置错误。
     */
    public Configuration load(String resource) throws ConfigurationException {
        LOG.info("Configuration from resource : " + resource);
        return load(getResource(resource));
    }

    /**
     * 从指定的配置文件加载配置。
     *
     * @param configFile 指定的配置文件。
     *
     * @return 此配置对象的引用。
     *
     * @throws ConfigurationException 如果发生配置错误。
     */
    public Configuration load(File configFile) throws ConfigurationException {
        LOG.info("Configuring from file : " + configFile.getName());
        try {
            return load(new FileInputStream(configFile), configFile.toString());
        } catch (FileNotFoundException fnfe) {
            throw new ConfigurationException("Could not find file : " + configFile, fnfe);
        }
    }

    /**
     * 未完成
     */
    private void validate() {
    }

    /**
     * 从指定资源获取URL。
     *
     * @param resource 资源文件名。
     *
     * @return URL。
     */
    private static URL getResource(String resource) {
        String name = resource.startsWith("/") ? resource.substring(1) : resource;
        URL url = null;
        ClassLoader classLoader = Thread.currentThread().getContextClassLoader();
        if (classLoader != null) {
            url = classLoader.getResource(name);
        }
        if (url == null) {
            url = Configuration.class.getResource(resource);
        }
        if (url == null) {
            url = Configuration.class.getClassLoader().getResource(name);
        }
        if (url == null) {
            throw new IllegalArgumentException(resource + " not found");
        }
        return url;
    }

    /**
     * 打印分隔符。
     *
     * @param bool 是否打印。
     */
    @SuppressWarnings("UseOfSystemOutOrSystemErr")
    private static void printSeparator(boolean bool) {
        if (bool) System.out.println("--------------------------------------------------------------------------------");
    }

    /**
     * 从指定的InputStream对象中加载配置。
     *
     * @param stream 指定的InputStream。
     * @param resourceName InputStream对象的名称。
     *
     * @return 此配置对象的引用。
     *
     * @throws ConfigurationException 如果发生配置错误。
     */
    protected Configuration load(InputStream stream, String resourceName) throws ConfigurationException {
        try {
            Document doc = DocumentBuilderFactory.newInstance().newDocumentBuilder().parse(stream);
            Element root = doc.getDocumentElement();
            List<Element> list = null;
            int length = 0;
            list = getChildNodesByTagName(root, ACTION_FACTORY);
            if ((length = list.size()) == 1) {
                Element e = list.get(0);
                String cls = e.getAttribute(CLASS);
                if (StringUtil.isNotBlank(cls)) actionFactoryClass = (Class<? extends ActionFactory>) ClassUtil.loadClass(cls);
                LOG.info("Configured SessionFactory : " + cls);
                list = getChildNodesByTagName(e, PROPERTY);
                actionFactoryProperties = parseProperties(actionFactoryClass, list);
            } else if (length > 1) {
                throw new ConfigurationException("More than one <" + ACTION_FACTORY + "> tag in setting.", null);
            }
            scanComponents(root);
            printSeparator(!list.isEmpty());
            parseActionFactoryElements(root);
            list = getChildNodesByTagName(root, INCLUDE);
            Map<String, String> record = new HashMap<String, String>();
            for (Element e : list) {
                parseInclude(resourceName, e.getAttribute(FILE), record);
            }
        } catch (ConfigurationException e) {
            throw e;
        } catch (Exception e) {
            throw new ConfigurationException("Could not configure from inputStream resource : " + resourceName, e);
        }
        return this;
    }

    /**
     * 解析"<component-scan>"并添加自动检索的类。
     *
     * @param root 文档根节点。
     *
     * @throws ClassNotFoundException 如果无法定位类。
     */
    private void scanComponents(Element root) throws ClassNotFoundException {
        List<Element> list = getChildNodesByTagName(root, COMPONENT_SCAN);
        printSeparator(!list.isEmpty());
        char[] sep = { ',', ';' };
        for (Element e : list) {
            String pkg = e.getAttribute(PACKAGE);
            String exclude = e.getAttribute(EXCLUDE);
            LOG.info("Scan components package : [{}], exclude : [{}]", pkg, exclude);
            scanComponents.addAll(ClassUtil.getClasses(CollectionUtil.stringToCollection(pkg, null, sep).toArray(new String[0])));
            if (StringUtil.isNotBlank(exclude)) excludePackages.addAll(CollectionUtil.stringToCollection(exclude, new HashSet<String>(), sep));
        }
    }

    /**
     * filter the scan components by excludec components。
     *
     * @param scanComponents The scan components。
     * @param excludeComponents The excludec components。
     */
    private static void filterScanComponents(Set<Class<?>> scanComponents, Set<String> excludeComponents) {
        Iterator<Class<?>> it = scanComponents.iterator();
        out: while (it.hasNext()) {
            Class cls = it.next();
            if (cls.isInterface() || Modifier.isAbstract(cls.getModifiers()) || !Modifier.isPublic(cls.getModifiers())) {
                it.remove();
                continue out;
            } else {
                Constructor<?>[] cs = cls.getConstructors();
                if (cs.length == 0) {
                    it.remove();
                    continue out;
                } else {
                    boolean hasEmptyConstructor = false;
                    for (Constructor<?> c : cs) {
                        if (c.getGenericParameterTypes().length == 0) {
                            hasEmptyConstructor = true;
                            break;
                        }
                    }
                    if (!hasEmptyConstructor) {
                        it.remove();
                        continue out;
                    }
                }
            }
            if (!excludeComponents.isEmpty()) {
                String clsPkg = cls.getPackage().getName();
                if (excludeComponents.contains(cls.getName()) || excludeComponents.contains(clsPkg)) {
                    it.remove();
                    continue out;
                } else {
                    for (String exclude : excludeComponents) {
                        if (clsPkg.startsWith(exclude)) {
                            it.remove();
                            continue out;
                        }
                    }
                }
            }
        }
    }

    /**
     * 由<property>标签解析类的注入属性。
     *
     * @param cls 指定的类型。
     * @param propnodes <property>标签集合。
     *
     * @return 可注入属性的映射。
     *
     * @throws IntrospectionException 如果在内省期间发生异常。
     */
    private static Map<String, Object> parseProperties(Class<?> cls, List<Element> propnodes) throws IntrospectionException {
        Map<String, Object> properties = new LinkedHashMap<String, Object>();
        for (Element prop : propnodes) {
            String pName = prop.getAttribute(NAME);
            if (null != properties.put(pName, prop.getAttribute(VALUE))) {
                LOG.warn("Override property [{}] value [{}] in " + cls, pName, prop.getAttribute(VALUE));
            }
        }
        return properties;
    }

    /**
     * 添加包含的配置文件"<include>", 用一个集合映射判断并避免循环引用。
     *
     * @param from 源配置文件名称。
     * @param includeFile 被包含的配置文件名称。
     * @param record 指定的被包含/包含文件的映射。
     */
    private void parseInclude(String from, String includeFile, Map<String, String> record) {
        LOG.info("Load included file : " + includeFile);
        printSeparator(true);
        URL include = getResource(includeFile);
        if (include == null) {
            throw new ConfigurationException("Could not included file : " + include, null);
        }
        InputStream stream = null;
        try {
            stream = include.openStream();
        } catch (IOException e) {
            throw new ConfigurationException("IOException occurs in included file : " + include, e);
        }
        if (record.containsKey(includeFile)) {
            throw new ConfigurationException("Load circular reference file, " + "[" + includeFile + "] included in [" + from + "] and [" + record.get(includeFile) + "]", null);
        }
        record.put(includeFile, from);
        try {
            Document doc = DocumentBuilderFactory.newInstance().newDocumentBuilder().parse(stream);
            Element root = doc.getDocumentElement();
            parseActionFactoryElements(root);
            List<Element> list = getChildNodesByTagName(root, INCLUDE);
            for (Element e : list) {
                parseInclude(includeFile, e.getAttribute(FILE), record);
            }
        } catch (ConfigurationException e) {
            throw e;
        } catch (Exception e) {
            throw new ConfigurationException("Could not load or parse properties from included file : " + include, e);
        } finally {
            if (stream != null) try {
                stream.close();
            } catch (IOException e) {
                LOG.error("Fail to close input stream : " + include, e);
            }
        }
    }

    /**
     * 依次添加interceptor、interceptorStack、resultType、result、action。
     *
     * @param root 文档根节点。
     *
     * @throws ClassNotFoundException 如果没有找到具有指定名称的类。
     * @throws IllegalAccessException 如果底层方法不可访问。
     * @throws IntrospectionException 如果在内省期间发生异常。
     * @throws InstantiationException 如果实例化失败。
     */
    private void parseActionFactoryElements(Element root) throws ClassNotFoundException, IllegalAccessException, IntrospectionException, InvocationTargetException {
        List<Element> list = null;
        list = getChildNodesByTagName(root, INTERCEPTOR);
        for (Element e : list) {
            Class cls = ClassUtil.loadClass(e.getAttribute(CLASS));
            LOG.debug("Load Interceptor class : {}", cls);
            if (!interceptors.add(cls)) {
                LOG.warn("Duplicate interceptor class : {}, Override the configuration and properties.", cls);
            }
            Map<String, Object> props = parseProperties(cls, getChildNodesByTagName(e, PROPERTY));
            if (!props.isEmpty()) interceptorProperties.put(cls, props);
        }
        printSeparator(!list.isEmpty());
        list = getChildNodesByTagName(root, INTERCEPTOR_STACK);
        for (Element e : list) {
            Class cls = ClassUtil.loadClass(e.getAttribute(CLASS));
            LOG.debug("Load InterceptorStack class : {}", cls);
            if (!interceptorStacks.add(cls)) {
                LOG.warn("Duplicate InterceptorStack class : {}, Override the configuration and properties.", cls);
            }
            Map<String, Object> props = parseProperties(cls, getChildNodesByTagName(e, PROPERTY));
            if (!props.isEmpty()) interceptorStackProperties.put(cls, props);
        }
        printSeparator(!list.isEmpty());
        list = getChildNodesByTagName(root, RESULT_TYPE);
        for (Element e : list) {
            Class cls = ClassUtil.loadClass(e.getAttribute(CLASS));
            LOG.debug("Load ResultType class : {}", cls);
            if (!resultTypes.add(cls)) {
                LOG.warn("Duplicate ResultType class : {}, Override the configuration and properties.", cls);
            }
            Map<String, Object> props = parseProperties(cls, getChildNodesByTagName(e, PROPERTY));
            if (!props.isEmpty()) resultTypeProperties.put(cls, props);
        }
        printSeparator(!list.isEmpty());
        list = getChildNodesByTagName(root, RESULT);
        for (Element e : list) {
            Class cls = ClassUtil.loadClass(e.getAttribute(CLASS));
            LOG.debug("Load Result class : {}", cls);
            if (!results.add(cls)) {
                LOG.warn("Duplicate Result class : {}, Override the configuration and properties.", cls);
            }
            Map<String, Object> props = parseProperties(cls, getChildNodesByTagName(e, PROPERTY));
            if (!props.isEmpty()) resultProperties.put(cls, props);
        }
        printSeparator(!list.isEmpty());
        list = getChildNodesByTagName(root, ACTION);
        for (Element e : list) {
            List<Element> propnodes = getChildNodesByTagName(e, PROPERTY);
            Class cls = ClassUtil.loadClass(e.getAttribute(CLASS));
            LOG.debug("Load Action class : {}", cls);
            if (!actions.add(cls)) {
                LOG.warn("Duplicate Action class : {}, Override the configuration and properties.", cls);
            }
            Map<String, Object> props = parseProperties(cls, propnodes);
            if (!props.isEmpty()) actionProperties.put(cls, props);
            List<Element> pathnodes = getChildNodesByTagName(e, PATH);
            for (Element path : pathnodes) {
                String pathName = path.getAttribute(NAME);
                pathActions.put(pathName, cls);
                LOG.debug("Load path properties : " + pathName);
                List<Element> pathpropnodes = getChildNodesByTagName(path, PROPERTY);
                Map<String, Object> pathProps = new LinkedHashMap<String, Object>();
                for (Element p : pathpropnodes) {
                    pathProps.put(p.getAttribute(NAME), p.getAttribute(VALUE));
                }
                if (pathProperties.containsKey(pathName)) {
                    LOG.warn("Duplicate Action path configuration : {}, Override the properties.", pathName);
                }
                if (!pathProps.isEmpty()) {
                    pathProperties.put(pathName, pathProps);
                }
            }
        }
        printSeparator(!list.isEmpty());
    }

    /**
     * 获取指定父节点和节点名称的子节点集合。
     *
     * @param parent 指定的父节点。
     * @param name 指定的节点名称。
     *
     * @return 子节点集合。
     */
    private static List<Element> getChildNodesByTagName(Element parent, String name) {
        List<Element> eles = new ArrayList<Element>();
        for (Node child = parent.getFirstChild(); child != null; child = child.getNextSibling()) {
            if (Node.ELEMENT_NODE == child.getNodeType() && name.equals(child.getNodeName())) {
                eles.add((Element) child);
            }
        }
        return eles;
    }

    /**
     * 注入属性至指定的对象，并去除不支持的属性。
     *
     * @param obj 指定的对象。
     * @param properties 注入属性映射集合。
     * @param removeUnsupported 是否去除不支持的属性。
     *
     * @throws IntrospectionException 如果在内省期间发生异常。
     * @throws IllegalAccessException 如果底层方法不可访问。
     * @throws InvocationTargetException 如果底层方法抛出异常。
     */
    private static void injectProperties(Object obj, Map<String, Object> properties, boolean removeUnsupported) throws IntrospectionException, IllegalAccessException, InvocationTargetException {
        String cls = obj.getClass().getName();
        Map<String, PropertyDescriptor> supports = Injector.getSupportedProperties(obj.getClass());
        Iterator<Map.Entry<String, Object>> it = properties.entrySet().iterator();
        while (it.hasNext()) {
            Map.Entry<String, Object> e = it.next();
            String pName = e.getKey();
            PropertyDescriptor pd = supports.get(pName);
            if (pd == null) {
                LOG.error("Not supported property [{}] in [{}]", pName, cls);
                if (removeUnsupported) it.remove();
            } else {
                Object value = e.getValue();
                if (value instanceof String) {
                    value = Injector.stringToObject((String) value, pd.getPropertyType());
                }
                pd.getWriteMethod().invoke(obj, value);
            }
        }
    }

    /**
     * 创建新对象。
     *
     * @param factory 指定的<code>ActionFactory</code>。
     * @param obj 传入的对象。
     *
     * @return 如果传入的对象为<code>String</code>或<code>Class</code>，返回由<code>ActionFactory</code>创建的实例对象；否则直接返回传入的对象。
     *
     * @throws ClassNotFoundException 如果没有找到具有指定名称的类。
     */
    private static Object newInstance(ActionFactory factory, Object obj) throws ClassNotFoundException {
        if (obj instanceof Class) {
            obj = factory.getObjectFactory().newInstance((Class) obj);
        } else if (obj instanceof String) {
            obj = factory.getObjectFactory().newInstance(ClassUtil.loadClass((String) obj));
        }
        return obj;
    }

    /**
     * 由此<code>Configuration</code>对象中的配置属性创建一个新的<code>ActionFactory</code>对象。 此<code>Configuration</code>对象中配置属性的变更不影响已生成的<code>ActionFactory</code>对象。
     *
     * @param <T>
     * <code>ActionFactory</code>的类型。
     *
     * @return 生成的<code>ActionFactory</code>对象。
     *
     * @throws ConfigurationException 如果发生无效的配置。
     */
    public <T extends ActionFactory<?>> T buildActionFactory() throws ConfigurationException {
        ActionFactory factory = createActionFactory(actionFactoryClass, actionFactoryProperties);
        printSeparator(!actionFactoryProperties.isEmpty());
        filterScanComponents(scanComponents, excludePackages);
        if (!scanComponents.isEmpty()) {
            LOG.debug("Checking auto scan classes as follows :");
            Iterator<Class<?>> it = scanComponents.iterator();
            while (it.hasNext()) {
                LOG.debug(it.next().toString());
            }
            LOG.debug("Finish check auto scan classes : " + scanComponents.size());
        } else {
            LOG.debug("No auto scan classes");
        }
        printSeparator(true);
        try {
            if (factory instanceof DefaultActionFactory) {
                DefaultActionFactory defaultFactory = (DefaultActionFactory) factory;
                Set<Class> specified = new HashSet<Class>();
                for (Object obj : interceptors) {
                    obj = newInstance(factory, obj);
                    Map<String, Object> props = interceptorProperties.get(obj.getClass());
                    if (props != null) {
                        injectProperties(obj, props, true);
                    }
                    defaultFactory.addInterceptors(obj);
                    specified.add(obj.getClass());
                }
                for (Class<?> cls : scanComponents) {
                    if (!specified.contains(cls)) defaultFactory.addInterceptors(cls);
                }
                specified.clear();
                printSeparator(!defaultFactory.getInterceptors().isEmpty());
                for (Object obj : interceptorStacks) {
                    obj = newInstance(factory, obj);
                    Map<String, Object> props = interceptorStackProperties.get(obj.getClass());
                    if (props != null) {
                        injectProperties(obj, props, true);
                    }
                    defaultFactory.addInterceptorStacks(obj);
                    specified.add(obj.getClass());
                }
                for (Class<?> cls : scanComponents) {
                    if (!specified.contains(cls)) defaultFactory.addInterceptorStacks(cls);
                }
                specified.clear();
                printSeparator(!defaultFactory.getInterceptorStacks().isEmpty());
                for (Object obj : resultTypes) {
                    obj = newInstance(factory, obj);
                    Map<String, Object> props = resultTypeProperties.get(obj.getClass());
                    if (props != null) {
                        injectProperties(obj, props, true);
                    }
                    defaultFactory.addResultTypes(obj);
                    specified.add(obj.getClass());
                }
                for (Class<?> cls : scanComponents) {
                    if (!specified.contains(cls)) defaultFactory.addResultTypes(cls);
                }
                specified.clear();
                printSeparator(!defaultFactory.getResultTypes().isEmpty());
                for (Object obj : results) {
                    obj = newInstance(factory, obj);
                    Map<String, Object> props = resultProperties.get(obj.getClass());
                    if (props != null) {
                        injectProperties(obj, props, true);
                    }
                    defaultFactory.addResults(obj);
                    specified.add(obj.getClass());
                }
                for (Class<?> cls : scanComponents) {
                    if (!specified.contains(cls)) defaultFactory.addResults(cls);
                }
                specified.clear();
                printSeparator(!defaultFactory.getResults().isEmpty());
                for (Object obj : actions) {
                    obj = newInstance(factory, obj);
                    Map<String, Object> props = actionProperties.get(obj.getClass());
                    if (props != null) {
                        injectProperties(obj, props, true);
                        Injector.putClassProperties(obj.getClass(), props);
                    }
                    defaultFactory.addActions(obj);
                    specified.add(obj.getClass());
                }
                for (Class<?> cls : scanComponents) {
                    if (!specified.contains(cls)) defaultFactory.addActions(cls);
                }
                specified.clear();
                printSeparator(!defaultFactory.getActions().isEmpty());
                for (Map.Entry<String, Map<String, Object>> e : pathProperties.entrySet()) {
                    String pathName = e.getKey();
                    Class pathActionClass = pathActions.get(pathName);
                    Map<String, Object> allProps = new LinkedHashMap<String, Object>();
                    allProps.putAll(actionProperties.get(pathActionClass));
                    allProps.putAll(e.getValue());
                    Injector.putActionProperties(pathActionClass, pathName, allProps);
                }
            }
        } catch (ConfigurationException e) {
            throw e;
        } catch (Exception e) {
            throw new ConfigurationException(e);
        }
        return (T) factory;
    }

    /**
     * 由指定的ActionFactory类型和属性集合创建ActionFactory的对象实例,可用于子类继承以覆写。
     * 如果存在，默认优先调用&lt;init&gt;(java.util.Map)的构造函数。
     *
     * @param <T> ActionFactory特定类型。
     * @param actionFactoryClass 指定的ActionFactory类型。
     * @param actionFactoryProperties ActionFactory的属性集合。
     *
     * @return ActionFactory的对象实例。
     *
     * @throws ConfigurationException 如果发生任何构造异常。
     */
    protected <T extends ActionFactory<?>> T createActionFactory(Class<? extends ActionFactory> actionFactoryClass, Map<String, Object> actionFactoryProperties) throws ConfigurationException {
        ActionFactory factory = null;
        Constructor<? extends ActionFactory> con = null;
        try {
            con = actionFactoryClass.getDeclaredConstructor(Map.class);
        } catch (NoSuchMethodException ex) {
            LOG.info("No constructor {}, use {}.<init>(). ", ex.getLocalizedMessage(), actionFactoryClass.getName());
        }
        try {
            factory = con == null ? actionFactoryClass.newInstance() : con.newInstance(actionFactoryProperties);
            afterActionFactoryCreation(factory);
        } catch (ConfigurationException e) {
            throw e;
        } catch (Exception e) {
            throw new ConfigurationException(e);
        }
        return (T) factory;
    }

    /**
     * 用于子类继承, 在初始化ActionFactory前执行设置其一些特定的操作。 默认情况下不做任何处理。
     *
     * @param factory 未初始化属性的{@link ActionFactory}。
     */
    protected void afterActionFactoryCreation(ActionFactory<?> factory) {
    }

    /**
     * 返回ActionFactory。
     *
     * @param <T> Action工厂对象的类型。
     *
     * @return ActionFactory。
     *
     * @deprecated 由<code>{@link #buildActionFactory()}</code>取代。
     */
    @Deprecated
    public <T extends ActionFactory<?>> T getFactory() {
        return buildActionFactory();
    }

    /**
     * 设置ActionFactory的类型。
     *
     * @param actionFactoryClass 指定的ActionFactory类型。
     *
     * @return 此配置对象的引用。
     */
    public Configuration setActionFactoryClass(Class<? extends ActionFactory> actionFactoryClass) {
        this.actionFactoryClass = actionFactoryClass;
        return this;
    }

    /**
     * 添加ActionFactory的属性映射集合。
     *
     * @param actionFactoryProperties ActionFactory的属性映射集合。
     *
     * @return 此配置对象的引用。
     *
     * @see DefaultActionFactory
     */
    public Configuration addActionFactoryProperties(Map<String, Object> actionFactoryProperties) {
        this.actionFactoryProperties.putAll(actionFactoryProperties);
        return this;
    }

    /**
     * 添加Action集合。
     *
     * @param actions Action集合。
     *
     * @return 此配置对象的引用。
     */
    public Configuration addActions(Collection<Object> actions) {
        this.actions.addAll(actions);
        return this;
    }

    /**
     * 添加拦截栈集合。
     *
     * @param interceptorStacks 拦截栈集合。
     *
     * @return 此配置对象的引用。
     */
    public Configuration addInterceptorStacks(Collection<Object> interceptorStacks) {
        this.interceptorStacks.addAll(interceptorStacks);
        return this;
    }

    /**
     * 添加拦截器集合。
     *
     * @param interceptors 拦截器集合。
     *
     * @return 此配置对象的引用。
     */
    public Configuration addInterceptors(Collection<Object> interceptors) {
        this.interceptors.addAll(interceptors);
        return this;
    }

    /**
     * 添加结果类型集合。
     *
     * @param resultTypes 结果类型集合。
     *
     * @return 此配置对象的引用。
     */
    public Configuration addResultTypes(Collection<Object> resultTypes) {
        this.resultTypes.addAll(resultTypes);
        return this;
    }

    /**
     * 添加结果对象集合。
     *
     * @param results 结果对象集合。
     *
     * @return 此配置对象的引用。
     */
    public Configuration addResults(Collection<Object> results) {
        this.results.addAll(results);
        return this;
    }

    /**
     * 添加排除的包名。
     *
     * @param excludePackages 排除的包名。
     *
     * @return 此配置对象的引用。
     */
    public Configuration addExcludeComponents(Collection<String> excludePackages) {
        this.excludePackages.addAll(excludePackages);
        return this;
    }

    /**
     * 添加自动检测的类集合。
     *
     * @param scanComponents 自动检测的类集合。
     *
     * @return 此配置对象的引用。
     */
    public Configuration addScanComponents(Collection<Class<?>> scanComponents) {
        this.scanComponents.addAll(scanComponents);
        return this;
    }

    /**
     * 添加指定路径Action的属性集合。
     *
     * @param pathProperties 指定路径Action的属性集合。
     *
     * @return 此配置对象的引用。
     */
    public Configuration addPathProperties(Map<String, Map<String, Object>> pathProperties) {
        this.pathProperties.putAll(pathProperties);
        return this;
    }
}
