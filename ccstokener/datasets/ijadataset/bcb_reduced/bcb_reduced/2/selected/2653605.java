package org.jiopi.ibean.kernel.context;

import java.io.InputStream;
import java.lang.reflect.Method;
import java.net.URL;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashSet;
import java.util.Stack;
import java.util.List;
import org.apache.commons.beanutils.ConvertUtils;
import org.apache.log4j.Logger;
import org.dom4j.Document;
import org.jiopi.framework.ControlPanel;
import org.jiopi.framework.ModuleConsole;
import org.jiopi.framework.core.JiopiConfigConstants;
import org.jiopi.framework.annotation.module.InstanceType;
import org.jiopi.ibean.kernel.NameVersion;
import org.jiopi.ibean.kernel.Version;
import org.jiopi.ibean.kernel.config.BlueprintAnnotations;
import org.jiopi.ibean.kernel.config.JIOPiConfig;
import org.jiopi.ibean.kernel.config.ModuleAnnotations;
import org.jiopi.ibean.kernel.context.classloader.IBeanClassLoader;
import org.jiopi.ibean.kernel.util.ClassUtil;
import org.jiopi.ibean.kernel.util.ObjectAccessor;
import org.jiopi.ibean.kernel.util.XMLMerger;
import org.jiopi.ibean.kernel.context.classloader.JIOPiJarClassLoader;

/**
 * 
 * 一个Module实例的相关操作
 * 
 * 一个iBeanContext下, ModuleName+version 确定同一个Module运行环境(ClassLoader)
 * 
 * ModuleConsole 的用途是将各种非标准化的JIOPi编程规范 转换为标准的Java调用规范
 * 
 * @version 0.1 2010.4.19
 * @since iBeanKernel0.1 2010.4.19
 *
 */
public class ModuleConsoleImpl implements ModuleConsole {

    private static Logger logger = Logger.getLogger(ModuleConsoleImpl.class);

    private final ContextCentralConsoleKernel ccck;

    private final NameVersion moduleVersion;

    /**
	 * 当前Module使用的ClassLoader
	 */
    public final IBeanClassLoader moduleClassLoader;

    public static final ThreadLocal<ClassLoader> originContextClassLoader = new ThreadLocal<ClassLoader>();

    /**
	 * caller IBeanClassLoader stack
	 */
    private static final ThreadLocal<LinkedHashSet<IBeanClassLoader>> originIBeanClassLoaderStack = new ThreadLocal<LinkedHashSet<IBeanClassLoader>>();

    private static final ThreadLocal<Stack<IBeanClassLoader>> assemblingStack = new ThreadLocal<Stack<IBeanClassLoader>>();

    private final ObjectPool objectPool = new ObjectPool();

    public ModuleConsoleImpl(NameVersion moduleVersion, IBeanClassLoader moduleClassLoader, ContextCentralConsoleKernel ccck) {
        this.moduleClassLoader = moduleClassLoader;
        this.ccck = ccck;
        this.moduleVersion = moduleVersion;
        if (logger.isDebugEnabled()) logger.debug("ModuleConsoleImpl init with ClassLoader " + moduleClassLoader);
    }

    private static void clearOriginContextClassLoader() {
        originContextClassLoader.set(null);
    }

    private static boolean initOriginContextClassLoader() {
        ClassLoader o = originContextClassLoader.get();
        boolean needClean = false;
        if (o == null) {
            needClean = true;
            o = Thread.currentThread().getContextClassLoader();
            originContextClassLoader.set(o);
            Class<?>[] callClasses = ClassUtil.getClassContext();
            LinkedHashSet<IBeanClassLoader> classLoaderStack = new LinkedHashSet<IBeanClassLoader>();
            for (Class<?> c : callClasses) {
                ClassLoader cl = c.getClassLoader();
                if (cl instanceof JIOPiJarClassLoader) {
                    IBeanClassLoader icl = ((JIOPiJarClassLoader) cl).parentClassLoader;
                    if (classLoaderStack.contains(icl)) {
                        classLoaderStack.remove(icl);
                    }
                    classLoaderStack.add(icl);
                }
            }
            originIBeanClassLoaderStack.set(classLoaderStack);
        }
        return needClean;
    }

    private static Stack<IBeanClassLoader> getAssemblingStack() {
        Stack<IBeanClassLoader> s = assemblingStack.get();
        if (s == null) {
            s = new Stack<IBeanClassLoader>();
            assemblingStack.set(s);
        }
        return s;
    }

    /**
	 * assemble the given Object
	 */
    private void autoAssembling(JIOPiConfig.Controlpanel assemblingConfig, Object obj) {
        if (moduleClassLoader == null) return;
        if (obj instanceof ControlPanelImpl) {
            ControlPanelImpl cpi = (ControlPanelImpl) obj;
            obj = cpi.wrapped;
        }
        try {
            Class<?> objClass = obj.getClass();
            if (objClass == Class.class) objClass = (Class<?>) obj;
            HashMap<String, Object> accessoryPool = new HashMap<String, Object>();
            accessoryPool.put("this", obj);
            if (assemblingConfig != null && assemblingConfig.properties.size() > 0) {
                for (String key : assemblingConfig.properties.keySet()) {
                    String value = assemblingConfig.properties.get(key);
                    try {
                        Class<?> varClass = ObjectAccessor.Accessible.variableClass(obj, key);
                        Object newValue = ConvertUtils.convert(value, varClass);
                        ObjectAccessor.Accessible.setVariable(obj, key, newValue);
                    } catch (Exception e) {
                    }
                }
            }
            if (assemblingConfig != null) socketAccessories(accessoryPool, "this", assemblingConfig);
            ModuleAnnotations.ControlPanel controlPanelConfig = moduleClassLoader.getControlPaneConfig(objClass.getName());
            if (controlPanelConfig != null) {
                if (controlPanelConfig.socketModule.size() > 0) {
                    for (String varName : controlPanelConfig.socketModule.keySet()) {
                        HashMap<String, String> socketModuleConfig = controlPanelConfig.socketModule.get(varName);
                        socketInnerAccessory(obj, varName, socketModuleConfig, assemblingConfig, accessoryPool);
                    }
                }
                if (controlPanelConfig.socketControlpanel.size() > 0) {
                    for (String varName : controlPanelConfig.socketControlpanel.keySet()) {
                        HashMap<String, String> socketControlpanelConfig = controlPanelConfig.socketControlpanel.get(varName);
                        socketInnerAccessory(obj, varName, socketControlpanelConfig, assemblingConfig, accessoryPool);
                    }
                }
            }
        } catch (Exception e) {
            logger.warn("Assembling object error : " + obj + " " + e.getMessage());
        }
    }

    private void socketInnerAccessory(Object obj, String varName, HashMap<String, String> socketModuleConfig, JIOPiConfig.Controlpanel assemblingConfig, HashMap<String, Object> accessoryPool) {
        String id = socketModuleConfig.get("id");
        String module = socketModuleConfig.get("module");
        String version = socketModuleConfig.get("version");
        String configuration = socketModuleConfig.get("configuration");
        if (module != null || id != null) {
            if (assemblingConfig != null && id != null) {
                HashMap<String, String> annotationConfig = assemblingConfig.innerAccessories.get(id);
                if (annotationConfig != null) {
                    String a_module = annotationConfig.get("module");
                    String a_version = annotationConfig.get("version");
                    String a_configuration = annotationConfig.get("configuration");
                    if (a_module != null) module = a_module;
                    if (a_version != null) version = a_version;
                    if (a_configuration != null) configuration = a_configuration;
                }
            }
            if ("this".equals(module)) {
                module = moduleVersion.name;
                version = moduleVersion.version;
            }
            boolean isControlPanel = false;
            Class<?> varClass = null;
            try {
                varClass = ObjectAccessor.Accessible.variableClass(obj, varName);
            } catch (Exception e) {
                return;
            }
            String cpName = varClass.getName();
            if (varClass == org.jiopi.framework.ControlPanel.class) {
                isControlPanel = true;
                cpName = socketModuleConfig.get("controlpanel");
            }
            Version useVersion = null;
            if (isControlPanel) {
                useVersion = new Version(version);
            } else {
                Version blueprintVersion = moduleClassLoader.getBlueprintVersion(varClass);
                if (blueprintVersion == null) {
                    blueprintVersion = new Version(null);
                }
                useVersion = blueprintVersion;
                if (version != null) {
                    Version definedVersion = new Version(version);
                    if (blueprintVersion.isCompatible(definedVersion)) {
                        useVersion = definedVersion;
                    }
                }
            }
            Object innerAccessory = newControlPanel(module, useVersion.version, cpName, configuration, null);
            if (id != null) {
                accessoryPool.put(id, innerAccessory);
            }
            if (assemblingConfig != null && id != null) {
                socketAccessories(accessoryPool, id, assemblingConfig);
            }
            Object innerControlPanel = innerAccessory;
            if (isControlPanel) {
                innerControlPanel = ObjectAccessor.processReturnValue(innerAccessory, varClass);
            }
            socket(obj, innerAccessory);
            initObject(innerAccessory, null);
            Object old = ObjectAccessor.Accessible.variable(obj, varName, null);
            if (old == null) ObjectAccessor.Accessible.setVariable(obj, varName, innerControlPanel);
        } else {
            logger.error("Assembling " + obj.getClass().getName() + "." + varName + " error. module name is not specified");
        }
    }

    private void socketAccessories(HashMap<String, Object> accessoryPool, String toID, JIOPiConfig.Controlpanel assemblingConfig) {
        if (assemblingConfig == null) return;
        Object toObj = accessoryPool.get(toID);
        if (toObj == null) {
            logger.warn("socket to " + toID + " error,Object is not found at Module " + this.moduleVersion);
            return;
        }
        List<Object[]> froms = assemblingConfig.sockets.get(toID);
        if (froms != null) {
            for (Object[] socketConfig : froms) {
                String fromID = (String) socketConfig[0];
                Object[] args = (Object[]) socketConfig[1];
                HashMap<String, String> accessoryConfig = assemblingConfig.accessories.get(fromID);
                if (accessoryConfig == null) {
                    logger.error("socket from " + fromID + " to " + toID + "error,configuration not found at Module " + this.moduleVersion);
                    continue;
                }
                String module = accessoryConfig.get("module");
                String controlpanel = accessoryConfig.get("controlpanel");
                String version = accessoryConfig.get("version");
                String configuration = accessoryConfig.get("configuration");
                String retrieve = accessoryConfig.get("retrieve");
                if (module == null || controlpanel == null) {
                    logger.error("socket from " + fromID + " to " + toID + "error,module or controlpanel must be set at Module " + this.moduleVersion);
                    continue;
                }
                Object accessory = accessoryPool.get(fromID);
                if (accessory == null) {
                    accessory = newControlPanel(module, version, controlpanel, configuration, null);
                    if (!"true".equals(retrieve)) {
                        accessoryPool.put(fromID, accessory);
                        socketAccessories(accessoryPool, fromID, assemblingConfig);
                    }
                    initObject(accessory, null);
                }
                if (args != null && args.length > 0) socket(accessory, toObj, args);
                socket(accessory, toObj);
                socket(toObj, accessory);
            }
        }
    }

    /**
	 * call to.socket(from)
	 * @param from
	 * @param to
	 */
    private static void socket(Object from, Object to, Object... args) {
        Class<?> toClass = to.getClass();
        Class<?> fromClass = from.getClass();
        ClassLoader toClassLoader = toClass.getClassLoader();
        if (toClassLoader != null && toClassLoader instanceof JIOPiJarClassLoader) {
            IBeanClassLoader moduleClassLoader = ((JIOPiJarClassLoader) toClassLoader).parentClassLoader;
            Class<?>[] registerBlueprintClasses = moduleClassLoader.getRegisterBlueprintClasses(toClass);
            if (registerBlueprintClasses != null) {
                HashSet<String> socketMethods = new HashSet<String>();
                for (Class<?> registerBlueprintClasse : registerBlueprintClasses) {
                    ClassLoader blueprintClassLoader = registerBlueprintClasse.getClassLoader();
                    if (blueprintClassLoader != null && blueprintClassLoader instanceof JIOPiJarClassLoader) {
                        BlueprintAnnotations.Interface blueprintConfig = ((JIOPiJarClassLoader) blueprintClassLoader).parentClassLoader.getBlueprintConfig(registerBlueprintClasse.getName());
                        if (blueprintConfig != null) {
                            socketMethods.addAll(blueprintConfig.sockets);
                        }
                    }
                }
                Method[] methods = toClass.getMethods();
                for (Method method : methods) {
                    if (socketMethods.contains(method.getName())) {
                        Class<?>[] parameterTypes = method.getParameterTypes();
                        int totalParameterNum = args.length + 1;
                        if (parameterTypes.length == totalParameterNum && ObjectAccessor.testMatch(fromClass, parameterTypes[0]) > 0) {
                            Object[] newArgs = new Object[totalParameterNum];
                            newArgs[0] = from;
                            for (int i = 0; i < args.length; i++) {
                                newArgs[i + 1] = args[i];
                            }
                            try {
                                method.invoke(to, newArgs);
                            } catch (Exception e) {
                                e.printStackTrace();
                                logger.error("socket from " + fromClass + " to " + toClass + " error", e);
                            }
                        }
                    }
                }
            }
        }
    }

    private <T> T newControlPanel(String moduleName, String compatibleVersion, String registerClassName, String configurationID, Class<T> returnType, Object... args) {
        ClassLoader contextClassLoader = Thread.currentThread().getContextClassLoader();
        try {
            Thread.currentThread().setContextClassLoader(originContextClassLoader.get());
            return ccck.accessModuleConsole(moduleName, compatibleVersion).accessControlPanel(registerClassName, configurationID, returnType, false, args);
        } finally {
            Thread.currentThread().setContextClassLoader(contextClassLoader);
        }
    }

    public <T> T accessControlPanel(String registerName, Class<T> returnType, Object... args) {
        String[] registerNameInfo = parseRegisterName(registerName);
        return accessControlPanel(registerNameInfo[0], registerNameInfo[1], returnType, true, args);
    }

    private static void addAssemblingConfigFile(LinkedHashSet<URL> assemblingConfigFiles, URL file) {
        if (file == null || assemblingConfigFiles == null) return;
        if (assemblingConfigFiles.contains(file)) {
            assemblingConfigFiles.remove(file);
        }
        assemblingConfigFiles.add(file);
    }

    public JIOPiConfig.Controlpanel getAssemblingConfig(String registerClassName) {
        String[] registerNameInfo = parseRegisterName(registerClassName);
        return getAssemblingConfig(registerNameInfo[0], registerNameInfo[1]);
    }

    private JIOPiConfig.Controlpanel getAssemblingConfig(String registerClassName, String configurationID) {
        if ("null".equals(configurationID)) throw new RuntimeException("test");
        JIOPiConfig.Controlpanel cpConfigs = null;
        LinkedHashSet<URL> assemblingConfigFiles = new LinkedHashSet<URL>();
        URL contextAssemblingConfigFile = originContextClassLoader.get().getResource(JiopiConfigConstants.ASSEMBLING_FILE);
        if (contextAssemblingConfigFile != null) assemblingConfigFiles.add(contextAssemblingConfigFile);
        LinkedHashSet<IBeanClassLoader> callerModules = originIBeanClassLoaderStack.get();
        for (IBeanClassLoader icl : callerModules) {
            URL assemblingConfigFile = icl.getLocalResource(JiopiConfigConstants.ASSEMBLING_FILE);
            addAssemblingConfigFile(assemblingConfigFiles, assemblingConfigFile);
        }
        Stack<IBeanClassLoader> assemblingStack = getAssemblingStack();
        for (IBeanClassLoader icl : assemblingStack) {
            URL assemblingConfigFile = icl.getLocalResource(JiopiConfigConstants.ASSEMBLING_FILE);
            addAssemblingConfigFile(assemblingConfigFiles, assemblingConfigFile);
        }
        Document assemblingConfigDocument = null;
        if (assemblingConfigFiles.size() > 0) {
            URL[] assemblingConfigURls = assemblingConfigFiles.toArray(new URL[assemblingConfigFiles.size()]);
            for (int i = assemblingConfigURls.length - 1; i > -1; i--) {
                URL url = assemblingConfigURls[i];
                try {
                    InputStream is = url.openStream();
                    Document configDocument = XMLMerger.readDocumentAndCloseStream(is);
                    if (assemblingConfigDocument == null) {
                        assemblingConfigDocument = configDocument;
                    } else {
                        assemblingConfigDocument = XMLMerger.mergeXML(assemblingConfigDocument, configDocument);
                    }
                } catch (Exception e) {
                    logger.warn("load assemblingConfigDocument error " + url, e);
                }
            }
        }
        if (assemblingConfigDocument != null) {
            JIOPiConfig assemblingConfig = new JIOPiConfig(assemblingConfigDocument);
            JIOPiConfig.Module findModuleConfig = assemblingConfig.getModuleConfig(moduleVersion);
            if (findModuleConfig != null) {
                cpConfigs = findModuleConfig.getMatchedControlpanel(moduleClassLoader, registerClassName, configurationID);
            }
        }
        return cpConfigs;
    }

    private <T> T accessControlPanel(String registerClassName, String configurationID, Class<T> returnType, boolean autoInit, Object... args) {
        boolean needClear = initOriginContextClassLoader();
        ClassLoader contextClassLoader = Thread.currentThread().getContextClassLoader();
        try {
            T instance = null;
            Thread.currentThread().setContextClassLoader(moduleClassLoader);
            getAssemblingStack().push(moduleClassLoader);
            JIOPiConfig.Controlpanel assemblingConfig = getAssemblingConfig(registerClassName, configurationID);
            if (assemblingConfig != null && assemblingConfig.constructor.length > 0 && (args == null || args.length < 1)) {
                args = assemblingConfig.constructor;
            }
            Class<?> c = moduleClassLoader.loadModuleClass(registerClassName, true);
            ModuleAnnotations.ControlPanel.Instantiation instantiation = moduleClassLoader.getInstantiationConfig(c.getName());
            if (instantiation != null) {
                String cacheName = null;
                if (InstanceType.SINGLETON.name().equals(instantiation.instancetype)) {
                    cacheName = c.getName();
                } else if (InstanceType.CONFIGURATION_SINGLETON.name().equals(instantiation.instancetype)) {
                    cacheName = mergeRegisterName(c.getName(), configurationID);
                }
                if (cacheName != null) {
                    instance = (T) objectPool.get(cacheName, returnType);
                    if (instance == null) {
                        synchronized (c) {
                            instance = (T) objectPool.get(cacheName, returnType);
                            if (instance == null) {
                                instance = instanceControlPanel(c, instantiation, returnType, assemblingConfig, autoInit, args);
                                objectPool.put(cacheName, instance);
                            }
                        }
                    }
                } else {
                    instance = instanceControlPanel(c, instantiation, returnType, assemblingConfig, autoInit, args);
                }
            } else {
                instance = ObjectAccessor.constructor(c, returnType, args);
                autoAssembling(assemblingConfig, instance);
            }
            return instance;
        } catch (ClassNotFoundException e) {
            throw new RuntimeException(e);
        } finally {
            Thread.currentThread().setContextClassLoader(contextClassLoader);
            if (needClear) clearOriginContextClassLoader();
            getAssemblingStack().pop();
        }
    }

    private <T> T instanceControlPanel(Class<?> c, ModuleAnnotations.ControlPanel.Instantiation instantiation, Class<T> returnType, JIOPiConfig.Controlpanel assemblingConfig, boolean autoInit, Object... args) {
        T instance = null;
        if (instantiation.factoryMethod != null && instantiation.factoryMethod.length() > 0) {
            instance = ObjectAccessor.method(c, instantiation.factoryMethod, returnType, args);
        } else {
            instance = ObjectAccessor.constructor(c, returnType, args);
        }
        autoAssembling(assemblingConfig, instance);
        if (autoInit) initObject(instance, instantiation);
        return instance;
    }

    public <T> T accessControlPanelStrict(String registerName, Class<T> returnType, Object[] parameterTypes, Object... args) {
        String[] registerNameInfo = parseRegisterName(registerName);
        return accessControlPanelStrict(registerNameInfo[0], registerNameInfo[1], returnType, parameterTypes, true, args);
    }

    private <T> T accessControlPanelStrict(String registerClassName, String configurationID, Class<T> returnType, Object[] parameterTypes, boolean autoInit, Object... args) {
        boolean needClear = initOriginContextClassLoader();
        ClassLoader contextClassLoader = Thread.currentThread().getContextClassLoader();
        try {
            T instance = null;
            Thread.currentThread().setContextClassLoader(moduleClassLoader);
            getAssemblingStack().push(moduleClassLoader);
            JIOPiConfig.Controlpanel assemblingConfig = getAssemblingConfig(registerClassName, configurationID);
            if (assemblingConfig != null && assemblingConfig.constructor.length > 0 && args == null || args.length < 1) {
                args = assemblingConfig.constructor;
            }
            Class<?> c = moduleClassLoader.loadModuleClass(registerClassName, true);
            ModuleAnnotations.ControlPanel.Instantiation instantiation = moduleClassLoader.getInstantiationConfig(c.getName());
            if (instantiation != null) {
                String cacheName = null;
                if (InstanceType.SINGLETON.name().equals(instantiation.instancetype)) {
                    cacheName = c.getName();
                } else if (InstanceType.CONFIGURATION_SINGLETON.name().equals(instantiation.instancetype)) {
                    cacheName = mergeRegisterName(c.getName(), configurationID);
                }
                if (cacheName != null) {
                    instance = (T) objectPool.get(cacheName, returnType);
                    if (instance == null) {
                        synchronized (c) {
                            instance = (T) objectPool.get(cacheName, returnType);
                            if (instance == null) {
                                instance = instanceControlPanelStrict(c, instantiation, parameterTypes, returnType, assemblingConfig, autoInit, args);
                                objectPool.put(cacheName, instance);
                            }
                        }
                    }
                } else {
                    instance = instanceControlPanelStrict(c, instantiation, parameterTypes, returnType, assemblingConfig, autoInit, args);
                }
            } else {
                instance = ObjectAccessor.strictConstructor(c, parameterTypes, returnType, args);
                autoAssembling(assemblingConfig, instance);
            }
            return instance;
        } catch (ClassNotFoundException e) {
            throw new RuntimeException(e);
        } finally {
            Thread.currentThread().setContextClassLoader(contextClassLoader);
            if (needClear) clearOriginContextClassLoader();
            getAssemblingStack().pop();
        }
    }

    private <T> T instanceControlPanelStrict(Class<?> c, ModuleAnnotations.ControlPanel.Instantiation instantiation, Object[] parameterTypes, Class<T> returnType, JIOPiConfig.Controlpanel assemblingConfig, boolean autoInit, Object... args) {
        T instance = null;
        if (instantiation.factoryMethod != null && instantiation.factoryMethod.length() > 0) {
            instance = ObjectAccessor.strictMethod(c, instantiation.factoryMethod, returnType, parameterTypes, args);
        } else {
            instance = ObjectAccessor.strictConstructor(c, parameterTypes, returnType, args);
        }
        autoAssembling(assemblingConfig, instance);
        if (autoInit) initObject(instance, instantiation);
        return instance;
    }

    private static void initObject(Object instance, ModuleAnnotations.ControlPanel.Instantiation instantiation) {
        if (instantiation == null) {
            Class<?> instanceClass = instance.getClass();
            ClassLoader objClassLoader = instanceClass.getClassLoader();
            if (objClassLoader != null && objClassLoader instanceof JIOPiJarClassLoader) {
                instantiation = ((JIOPiJarClassLoader) objClassLoader).parentClassLoader.getInstantiationConfig(instanceClass.getName());
            }
        }
        if (instantiation != null && instantiation.initMethod != null && instantiation.initMethod.length() > 0) {
            if (instance instanceof ControlPanelImpl) {
                ControlPanelImpl cpi = (ControlPanelImpl) instance;
                ObjectAccessor.method(cpi.wrapped, instantiation.initMethod, null);
            } else {
                ObjectAccessor.method(instance, instantiation.initMethod, null);
            }
        }
    }

    private final HashSet<String> initedClass = new HashSet<String>();

    public ControlPanelImpl accessStaticControlPanel(String registerName) {
        String[] registerNameInfo = parseRegisterName(registerName);
        boolean needClear = initOriginContextClassLoader();
        ClassLoader contextClassLoader = Thread.currentThread().getContextClassLoader();
        try {
            Thread.currentThread().setContextClassLoader(moduleClassLoader);
            getAssemblingStack().push(moduleClassLoader);
            Class<?> c = moduleClassLoader.loadModuleClass(registerNameInfo[0], true);
            if (!initedClass.contains(c.getName())) {
                JIOPiConfig.Controlpanel assemblingConfig = getAssemblingConfig(registerNameInfo[0], registerNameInfo[1]);
                if (assemblingConfig != null) autoAssembling(assemblingConfig, c);
                initedClass.add(c.getName());
            }
            return new ControlPanelImpl(c);
        } catch (ClassNotFoundException e) {
            throw new RuntimeException(e);
        } finally {
            Thread.currentThread().setContextClassLoader(contextClassLoader);
            if (needClear) clearOriginContextClassLoader();
            getAssemblingStack().pop();
        }
    }

    public IBeanClassLoader getClassLoader() {
        return moduleClassLoader;
    }

    public <T> T operate(String registerName, Class<T> returnType, Object... args) {
        String[] registerNameInfo = parseRegisterName(registerName);
        String registerClassName = registerNameInfo[0];
        String replaceName = moduleClassLoader.getRegisterValue(registerClassName);
        if (replaceName != null) registerClassName = replaceName;
        String[] names = splitRegisterName(registerClassName);
        if (names == null) throw new IllegalArgumentException("registerName : " + registerName + " is not a definer of method");
        ControlPanelImpl cpi = accessStaticControlPanel(names[0]);
        if (ObjectAccessor.containsStaticMethod((Class<?>) cpi.wrapped, names[1], args)) {
            return cpi.operate(names[1], returnType, args);
        } else {
            ControlPanel cp = accessControlPanel(mergeRegisterName(names[0], registerNameInfo[1]), ControlPanel.class);
            return cp.operate(names[1], returnType, args);
        }
    }

    public <T> T operateStrict(String registerName, Class<T> returnType, Object[] parameterTypes, Object... args) {
        String[] registerNameInfo = parseRegisterName(registerName);
        String registerClassName = registerNameInfo[0];
        String replaceName = moduleClassLoader.getRegisterValue(registerClassName);
        if (replaceName != null) registerClassName = replaceName;
        String[] names = splitRegisterName(registerClassName);
        if (names == null) throw new IllegalArgumentException("registerName : " + registerName + " is not a definer of method");
        ControlPanelImpl cpi = accessStaticControlPanel(names[0]);
        if (ObjectAccessor.containsStaticMethod((Class<?>) cpi.wrapped, names[1], args)) {
            return cpi.operateStrict(names[1], returnType, parameterTypes, args);
        } else {
            ControlPanel cp = accessControlPanel(mergeRegisterName(names[0], registerNameInfo[1]), ControlPanel.class);
            return cp.operateStrict(names[1], returnType, parameterTypes, args);
        }
    }

    public <T> T staticVariable(String registerName, Class<T> returnType) {
        String replaceName = moduleClassLoader.getRegisterValue(registerName);
        if (replaceName != null) registerName = replaceName;
        String[] names = splitRegisterName(registerName);
        if (names == null) throw new IllegalArgumentException("registerName : " + registerName + " is not a definer of static variable");
        return accessStaticControlPanel(names[0]).variable(names[1], returnType);
    }

    /**
	 * 将注册名以最后一个点分为 类名 和 方法名/变量名 
	 * @param registerName
	 * @return 如果无法拆分，返回null
	 */
    private static String[] splitRegisterName(String registerName) {
        if (registerName == null) return null;
        int pos = registerName.lastIndexOf('.');
        if (pos < 0) return null;
        String[] split = new String[2];
        split[0] = registerName.substring(0, pos).trim();
        split[1] = registerName.substring(pos + 1).trim();
        for (String s : split) {
            if (s.length() == 0) return null;
        }
        return split;
    }

    /**
	 * 将注册名 ? 后的 配置文件名解析出来
	 * @param registerName
	 * @return
	 */
    public static String[] parseRegisterName(String registerName) {
        int pos = registerName.lastIndexOf('?');
        String[] split = new String[2];
        if (pos < 0) {
            split[0] = registerName;
        } else {
            split[0] = registerName.substring(0, pos).trim();
            split[1] = registerName.substring(pos + 1).trim();
            if (split[1].length() == 0) split[1] = null;
        }
        return split;
    }

    public static String mergeRegisterName(String name, String config) {
        if (config != null) name = name + "?" + config;
        return name;
    }

    public void refreshResource() {
    }
}
