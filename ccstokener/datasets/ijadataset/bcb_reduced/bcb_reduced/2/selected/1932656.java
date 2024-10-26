package com.google.gwt.dev.shell;

import com.google.gdt.eclipse.designer.hosted.classloader.GWTDesignTimeVisitor;
import com.google.gwt.core.client.GWTBridge;
import com.google.gwt.core.client.GwtScriptOnly;
import com.google.gwt.core.ext.TreeLogger;
import com.google.gwt.core.ext.UnableToCompleteException;
import com.google.gwt.core.ext.TreeLogger.Type;
import com.google.gwt.core.ext.typeinfo.JArrayType;
import com.google.gwt.core.ext.typeinfo.JClassType;
import com.google.gwt.core.ext.typeinfo.JMethod;
import com.google.gwt.core.ext.typeinfo.JParameter;
import com.google.gwt.core.ext.typeinfo.JPrimitiveType;
import com.google.gwt.core.ext.typeinfo.JType;
import com.google.gwt.core.ext.typeinfo.TypeOracle;
import com.google.gwt.dev.javac.CompilationState;
import com.google.gwt.dev.javac.CompilationUnit;
import com.google.gwt.dev.javac.CompiledClass;
import com.google.gwt.dev.javac.JsniMethod;
import com.google.gwt.dev.jjs.InternalCompilerException;
import com.google.gwt.dev.shell.rewrite.ForceClassVersion15;
import com.google.gwt.dev.shell.rewrite.HasAnnotation;
import com.google.gwt.dev.shell.rewrite.HostedModeClassRewriter;
import com.google.gwt.dev.shell.rewrite.HostedModeClassRewriter.InstanceMethodOracle;
import com.google.gwt.dev.shell.rewrite.HostedModeClassRewriter.SingleJsoImplData;
import com.google.gwt.dev.util.JsniRef;
import com.google.gwt.dev.util.Name;
import com.google.gwt.dev.util.Util;
import com.google.gwt.dev.util.Name.InternalName;
import com.google.gwt.dev.util.Name.SourceOrBinaryName;
import com.google.gwt.dev.util.collect.Lists;
import com.google.gwt.util.tools.Utility;
import org.eclipse.wb.internal.core.utils.reflect.AbstractMethodsImplementorVisitor;
import org.apache.commons.collections.map.AbstractReferenceMap;
import org.apache.commons.collections.map.ReferenceIdentityMap;
import org.apache.commons.collections.map.ReferenceMap;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.lang.reflect.Array;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.net.URL;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.SortedSet;
import java.util.Stack;
import java.util.TreeSet;

/**
 * An isolated {@link ClassLoader} for running all user code. All user files are
 * compiled from source code byte a {@link ByteCodeCompiler}. After compilation,
 * some byte code rewriting is performed to support
 * <code>JavaScriptObject</code> and its subtypes.
 * 
 * TODO: we should refactor this class to move the getClassInfoByDispId,
 * getDispId, getMethodDispatch and putMethodDispatch into a separate entity
 * since they really do not interact with the CompilingClassLoader
 * functionality.
 */
public final class CompilingClassLoader extends ClassLoader implements DispatchIdOracle {

    /**
   * Oracle that can answer questions about {@link DispatchClassInfo
   * DispatchClassInfos}.
   */
    private final class DispatchClassInfoOracle {

        /**
     * Class identifier to DispatchClassInfo mapping.
     */
        private final ArrayList<DispatchClassInfo> classIdToClassInfo = new ArrayList<DispatchClassInfo>();

        /**
     * Binary or source class name to DispatchClassInfo map.
     */
        private final Map<String, DispatchClassInfo> classNameToClassInfo = new HashMap<String, DispatchClassInfo>();

        /**
     * Clears out the contents of this oracle.
     */
        public synchronized void clear() {
            classIdToClassInfo.clear();
            classNameToClassInfo.clear();
        }

        /**
     * Returns the {@link DispatchClassInfo} for a given dispatch id.
     * 
     * @param dispId dispatch id
     * @return DispatchClassInfo for the requested dispatch id
     */
        public synchronized DispatchClassInfo getClassInfoByDispId(int dispId) {
            int classId = extractClassIdFromDispId(dispId);
            return classIdToClassInfo.get(classId);
        }

        /**
     * Returns the dispatch id for a given member reference. Member references
     * can be encoded as: "@class::field" or "@class::method(typesigs)".
     * 
     * @param jsniMemberRef a string encoding a JSNI member to use
     * @return integer encoded as ((classId << 16) | memberId)
     */
        public synchronized int getDispId(String jsniMemberRef) {
            if (jsniMemberRef.equals("toString")) {
                jsniMemberRef = "@java.lang.Object::toString()";
            }
            JsniRef parsed = JsniRef.parse(jsniMemberRef);
            if (parsed == null) {
                logger.log(TreeLogger.WARN, "Malformed JSNI reference '" + jsniMemberRef + "'; expect subsequent failures", new NoSuchFieldError(jsniMemberRef));
                return -1;
            }
            String className = parsed.className();
            DispatchClassInfo dispClassInfo = getClassInfoFromClassName(className);
            if (dispClassInfo != null) {
                String memberName = parsed.memberSignature();
                if (singleJsoImplTypes.contains(canonicalizeClassName(className))) {
                    logger.log(TreeLogger.WARN, "Invalid JSNI reference to SingleJsoImpl interface (" + className + "); consider using a trampoline. " + "Expect subsequent failures.", new NoSuchFieldError(jsniMemberRef));
                    return -1;
                }
                int memberId = dispClassInfo.getMemberId(memberName);
                if (memberId < 0) {
                    if (!className.startsWith("java.")) {
                        logger.log(TreeLogger.WARN, "Member '" + memberName + "' in JSNI reference '" + jsniMemberRef + "' could not be found; expect subsequent failures", new NoSuchFieldError(memberName));
                    }
                }
                return synthesizeDispId(dispClassInfo.getClassId(), memberId);
            }
            logger.log(TreeLogger.WARN, "Class '" + className + "' in JSNI reference '" + jsniMemberRef + "' could not be found; expect subsequent failures", new ClassNotFoundException(className));
            return -1;
        }

        /**
     * Extracts the class id from the dispatch id.
     * 
     * @param dispId
     * @return the classId encoded into this dispatch id
     */
        private int extractClassIdFromDispId(int dispId) {
            return (dispId >> 16) & 0xffff;
        }

        /**
     * Returns the {@link java.lang.Class} instance for a given binary class
     * name. It is important to avoid initializing the class because this would
     * potentially cause initializers to be run in a different order than in web
     * mode. Moreover, we may not have injected all of the JSNI code required to
     * initialize the class.
     * 
     * @param binaryClassName the binary name of a class
     * @return {@link java.lang.Class} instance or null if the given binary
     *         class name could not be found
     */
        private Class<?> getClassFromBinaryName(String binaryClassName) {
            try {
                int dims = 0;
                while (binaryClassName.endsWith("[]")) {
                    dims++;
                    binaryClassName = binaryClassName.substring(0, binaryClassName.length() - 2);
                }
                Class<?> clazz;
                if ("Z".equals(binaryClassName)) {
                    clazz = boolean.class;
                } else if ("B".equals(binaryClassName)) {
                    clazz = byte.class;
                } else if ("C".equals(binaryClassName)) {
                    clazz = char.class;
                } else if ("D".equals(binaryClassName)) {
                    clazz = double.class;
                } else if ("F".equals(binaryClassName)) {
                    clazz = float.class;
                } else if ("I".equals(binaryClassName)) {
                    clazz = int.class;
                } else if ("J".equals(binaryClassName)) {
                    clazz = long.class;
                } else if ("S".equals(binaryClassName)) {
                    clazz = short.class;
                } else if ("V".equals(binaryClassName)) {
                    clazz = void.class;
                } else {
                    clazz = Class.forName(binaryClassName, false, CompilingClassLoader.this);
                }
                if (dims > 0) {
                    return Array.newInstance(clazz, new int[dims]).getClass();
                } else {
                    return clazz;
                }
            } catch (ClassNotFoundException e) {
                return null;
            }
        }

        /**
     * Returns the {@link java.lang.Class} object for a class that matches the
     * source or binary name given.
     * 
     * @param className binary or source name
     * @return {@link java.lang.Class} instance, if found, or null
     */
        private Class<?> getClassFromBinaryOrSourceName(String className) {
            JClassType type = typeOracle.findType(SourceOrBinaryName.toSourceName(className));
            if (type != null) {
                String jniSig = type.getJNISignature();
                jniSig = jniSig.substring(1, jniSig.length() - 1);
                className = InternalName.toBinaryName(jniSig);
            }
            return getClassFromBinaryName(className);
        }

        /**
     * Returns the {@link DispatchClassInfo} associated with the class name.
     * Since we allow both binary and source names to be used in JSNI class
     * references, we need to be able to deal with the fact that multiple
     * permutations of the class name with regards to source or binary forms map
     * on the same {@link DispatchClassInfo}.
     * 
     * @param className binary or source name for a class
     * @return {@link DispatchClassInfo} associated with the binary or source
     *         class name; null if there is none
     */
        private DispatchClassInfo getClassInfoFromClassName(String className) {
            DispatchClassInfo dispClassInfo = classNameToClassInfo.get(className);
            if (dispClassInfo != null) {
                return dispClassInfo;
            }
            Class<?> cls = getClassFromBinaryOrSourceName(className);
            if (cls == null) {
                return null;
            }
            if (classRewriter.isJsoIntf(cls.getName())) {
                cls = getClassFromBinaryName(cls.getName() + "$");
            }
            int classId = classIdToClassInfo.size();
            dispClassInfo = new DispatchClassInfo(cls, classId);
            classIdToClassInfo.add(dispClassInfo);
            classNameToClassInfo.put(className, dispClassInfo);
            return dispClassInfo;
        }

        /**
     * Synthesizes a dispatch identifier for the given class and member ids.
     * 
     * @param classId class index
     * @param memberId member index
     * @return dispatch identifier for the given class and member ids
     */
        private int synthesizeDispId(int classId, int memberId) {
            return (classId << 16) | memberId;
        }
    }

    /**
   * A ClassLoader that will delegate to a parent ClassLoader and fall back to
   * loading bytecode as resources from an alternate parent ClassLoader.
   */
    private static class MultiParentClassLoader extends ClassLoader {

        private final ClassLoader resources;

        public MultiParentClassLoader(ClassLoader parent, ClassLoader resources) {
            super(parent);
            this.resources = resources;
        }

        @Override
        protected synchronized Class<?> findClass(String name) throws ClassNotFoundException {
            String resourceName = name.replace('.', '/') + ".class";
            URL url = resources.getResource(resourceName);
            if (url == null) {
                throw new ClassNotFoundException();
            }
            byte[] bytes = Util.readURLAsBytes(url);
            return defineClass(name, bytes, 0, bytes.length);
        }
    }

    /**
   * Implements {@link InstanceMethodOracle} on behalf of the
   * {@link HostedModeClassRewriter}. Implemented using {@link TypeOracle}.
   */
    private class MyInstanceMethodOracle implements InstanceMethodOracle {

        private final Map<String, Set<JClassType>> signatureToDeclaringClasses = new HashMap<String, Set<JClassType>>();

        public MyInstanceMethodOracle(Set<JClassType> jsoTypes, JClassType javaLangObject, SingleJsoImplData jsoData) {
            for (JClassType type : jsoTypes) {
                for (JMethod method : type.getMethods()) {
                    if (!method.isStatic()) {
                        assert !method.isAbstract() : "Abstract method in JSO type " + method;
                        add(type, method);
                    }
                }
            }
            for (String intfName : jsoData.getSingleJsoIntfTypes()) {
                JClassType intf = typeOracle.findType(Name.InternalName.toSourceName(intfName));
                JClassType jso = typeOracle.getSingleJsoImpl(intf);
                for (JMethod method : intf.getMethods()) {
                    add(jso, method);
                }
            }
            for (JMethod method : javaLangObject.getMethods()) {
                if (!method.isStatic()) {
                    String signature = createSignature(method);
                    Set<JClassType> declaringClasses = new HashSet<JClassType>();
                    signatureToDeclaringClasses.put(signature, declaringClasses);
                    declaringClasses.add(javaLangObject);
                }
            }
        }

        public String findOriginalDeclaringClass(String desc, String signature) {
            Set<JClassType> declaringClasses = signatureToDeclaringClasses.get(signature);
            assert declaringClasses != null : "No classes for " + signature;
            if (declaringClasses.size() == 1) {
                return createDescriptor(declaringClasses.iterator().next());
            }
            String sourceName = desc.replace('/', '.');
            sourceName = sourceName.replace('$', '.');
            JClassType declaredType = typeOracle.findType(sourceName);
            if (declaringClasses.contains(declaredType)) {
                return desc;
            }
            for (JClassType possibleSupertype : declaringClasses) {
                if (declaredType.isAssignableTo(possibleSupertype)) {
                    return createDescriptor(possibleSupertype);
                }
            }
            throw new IllegalArgumentException("Could not resolve signature '" + signature + "' from class '" + desc + "'");
        }

        /**
     * Record that a given JSO type contains the concrete implementation of a
     * (possibly abstract) method.
     */
        private void add(JClassType type, JMethod method) {
            String signature = createSignature(method);
            Set<JClassType> declaringClasses = signatureToDeclaringClasses.get(signature);
            if (declaringClasses == null) {
                declaringClasses = new HashSet<JClassType>();
                signatureToDeclaringClasses.put(signature, declaringClasses);
            }
            declaringClasses.add(type);
        }

        private String createDescriptor(JClassType type) {
            String jniSignature = type.getJNISignature();
            return jniSignature.substring(1, jniSignature.length() - 1);
        }

        private String createSignature(JMethod method) {
            StringBuffer sb = new StringBuffer(method.getName());
            sb.append('(');
            for (JParameter param : method.getParameters()) {
                sb.append(param.getType().getJNISignature());
            }
            sb.append(')');
            sb.append(method.getReturnType().getJNISignature());
            String signature = sb.toString();
            return signature;
        }
    }

    /**
   * Cook up the data we need to support JSO subtypes that implement interfaces
   * with methods. This includes the set of SingleJsoImpl interfaces actually
   * implemented by a JSO type, the mangled method names, and the names of the
   * Methods that should actually implement the virtual functions.
   * 
   * Given the current implementation of JSO$ and incremental execution of
   * rebinds, it's not possible for Generators to produce additional
   * JavaScriptObject subtypes, so this data can remain static.
   */
    private class MySingleJsoImplData implements SingleJsoImplData {

        private final SortedSet<String> mangledNames = new TreeSet<String>();

        private final Map<String, List<org.objectweb.asm.commons.Method>> mangledNamesToDeclarations = new HashMap<String, List<org.objectweb.asm.commons.Method>>();

        private final Map<String, List<org.objectweb.asm.commons.Method>> mangledNamesToImplementations = new HashMap<String, List<org.objectweb.asm.commons.Method>>();

        private final SortedSet<String> unmodifiableNames = Collections.unmodifiableSortedSet(mangledNames);

        private final Set<String> unmodifiableIntfNames = Collections.unmodifiableSet(singleJsoImplTypes);

        public MySingleJsoImplData() {
            typeLoop: for (JClassType type : typeOracle.getSingleJsoImplInterfaces()) {
                assert type.isInterface() == type : "Expecting interfaces only";
                for (JMethod intfMethod : type.getOverridableMethods()) {
                    assert intfMethod.isAbstract() : "Expecting only abstract methods";
                    JClassType implementingType = typeOracle.getSingleJsoImpl(intfMethod.getEnclosingType());
                    if (implementingType == null) {
                        continue typeLoop;
                    }
                    singleJsoImplTypes.add(canonicalizeClassName(getBinaryName(type)));
                    String mangledName = getBinaryName(type).replace('.', '_') + "_" + intfMethod.getName();
                    mangledNames.add(mangledName);
                    JMethod implementingMethod;
                    while ((implementingMethod = findOverloadUsingErasure(implementingType, intfMethod)) == null) {
                        implementingType = implementingType.getSuperclass();
                    }
                    {
                        String decl = getBinaryOrPrimitiveName(intfMethod.getReturnType().getErasedType()) + " " + intfMethod.getName() + "(";
                        for (JParameter param : intfMethod.getParameters()) {
                            decl += ",";
                            decl += getBinaryOrPrimitiveName(param.getType().getErasedType());
                        }
                        decl += ")";
                        org.objectweb.asm.commons.Method declaration = org.objectweb.asm.commons.Method.getMethod(decl);
                        addToMap(mangledNamesToDeclarations, mangledName, declaration);
                    }
                    {
                        String returnName = getBinaryOrPrimitiveName(implementingMethod.getReturnType().getErasedType());
                        String jsoName = getBinaryOrPrimitiveName(implementingType);
                        String decl = returnName + " " + intfMethod.getName() + "$ (" + jsoName;
                        for (JParameter param : implementingMethod.getParameters()) {
                            decl += ",";
                            decl += getBinaryOrPrimitiveName(param.getType().getErasedType());
                        }
                        decl += ")";
                        org.objectweb.asm.commons.Method toImplement = org.objectweb.asm.commons.Method.getMethod(decl);
                        addToMap(mangledNamesToImplementations, mangledName, toImplement);
                    }
                }
            }
            if (logger.isLoggable(Type.SPAM)) {
                TreeLogger dumpLogger = logger.branch(Type.SPAM, "SingleJsoImpl method mappings");
                for (Map.Entry<String, List<org.objectweb.asm.commons.Method>> entry : mangledNamesToImplementations.entrySet()) {
                    dumpLogger.log(Type.SPAM, entry.getKey() + " -> " + entry.getValue());
                }
            }
        }

        public List<org.objectweb.asm.commons.Method> getDeclarations(String mangledName) {
            List<org.objectweb.asm.commons.Method> toReturn = mangledNamesToDeclarations.get(mangledName);
            return toReturn == null ? null : Collections.unmodifiableList(toReturn);
        }

        public List<org.objectweb.asm.commons.Method> getImplementations(String mangledName) {
            List<org.objectweb.asm.commons.Method> toReturn = mangledNamesToImplementations.get(mangledName);
            return toReturn == null ? toReturn : Collections.unmodifiableList(toReturn);
        }

        public SortedSet<String> getMangledNames() {
            return unmodifiableNames;
        }

        public Set<String> getSingleJsoIntfTypes() {
            return unmodifiableIntfNames;
        }

        /**
     * Assumes that the usual case is a 1:1 mapping.
     */
        private <K, V> void addToMap(Map<K, List<V>> map, K key, V value) {
            List<V> list = map.get(key);
            if (list == null) {
                map.put(key, Lists.create(value));
            } else {
                List<V> maybeOther = Lists.add(list, value);
                if (maybeOther != list) {
                    map.put(key, maybeOther);
                }
            }
        }

        /**
     * Looks for a concrete implementation of <code>intfMethod</code> in
     * <code>implementingType</code>.
     */
        private JMethod findOverloadUsingErasure(JClassType implementingType, JMethod intfMethod) {
            int numParams = intfMethod.getParameters().length;
            JType[] erasedTypes = new JType[numParams];
            for (int i = 0; i < numParams; i++) {
                erasedTypes[i] = intfMethod.getParameters()[i].getType().getErasedType();
            }
            outer: for (JMethod method : implementingType.getOverloads(intfMethod.getName())) {
                JParameter[] params = method.getParameters();
                if (params.length != numParams) {
                    continue;
                }
                for (int i = 0; i < numParams; i++) {
                    if (params[i].getType().getErasedType() != erasedTypes[i]) {
                        continue outer;
                    }
                }
                return method;
            }
            return null;
        }
    }

    /**
   * The names of the bridge classes.
   */
    private static final Map<String, Class<?>> BRIDGE_CLASS_NAMES = new HashMap<String, Class<?>>();

    /**
   * The set of classes exposed into user space that actually live in hosted
   * space (thus, they bridge across the spaces).
   */
    private static final Class<?>[] BRIDGE_CLASSES = new Class<?>[] { ShellJavaScriptHost.class, GWTBridge.class };

    private static final boolean CLASS_DUMP = Boolean.getBoolean("gwt.dev.classDump");

    private static final String CLASS_DUMP_PATH = System.getProperty("gwt.dev.classDumpPath", "rewritten-classes");

    private static boolean emmaAvailable = false;

    private static EmmaStrategy emmaStrategy;

    /**
   * Caches the byte code for {@link JavaScriptHost}.
   */
    private static byte[] javaScriptHostBytes;

    static {
        for (Class<?> c : BRIDGE_CLASSES) {
            BRIDGE_CLASS_NAMES.put(c.getName(), c);
        }
        try {
            Class<?> emmaBridge = Class.forName(EmmaStrategy.EMMA_RT_CLASSNAME, false, Thread.currentThread().getContextClassLoader());
            BRIDGE_CLASS_NAMES.put(EmmaStrategy.EMMA_RT_CLASSNAME, emmaBridge);
            emmaAvailable = true;
        } catch (ClassNotFoundException ignored) {
        }
        emmaStrategy = EmmaStrategy.get(emmaAvailable);
    }

    private static void classDump(String name, byte[] bytes) {
        String packageName, className;
        int pos = name.lastIndexOf('.');
        if (pos < 0) {
            packageName = "";
            className = name;
        } else {
            packageName = name.substring(0, pos);
            className = name.substring(pos + 1);
        }
        File dir = new File(CLASS_DUMP_PATH + File.separator + packageName.replace('.', File.separatorChar));
        if (!dir.exists()) {
            dir.mkdirs();
        }
        File file = new File(dir, className + ".class");
        FileOutputStream fileOutput = null;
        try {
            fileOutput = new FileOutputStream(file);
            fileOutput.write(bytes);
            fileOutput.close();
        } catch (IOException e) {
            e.printStackTrace();
        } finally {
            if (fileOutput != null) {
                try {
                    fileOutput.close();
                } catch (IOException e) {
                }
            }
        }
    }

    /**
   * Magic: {@link JavaScriptHost} was never compiled because it's a part of the
   * hosted mode infrastructure. However, unlike {@link #BRIDGE_CLASSES},
   * {@code JavaScriptHost} needs a separate copy per inside the ClassLoader for
   * each module.
   */
    private static void ensureJavaScriptHostBytes(TreeLogger logger) throws UnableToCompleteException {
        if (javaScriptHostBytes != null) {
            return;
        }
        String className = JavaScriptHost.class.getName();
        try {
            String path = className.replace('.', '/') + ".class";
            ClassLoader cl = Thread.currentThread().getContextClassLoader();
            URL url = cl.getResource(path);
            if (url != null) {
                javaScriptHostBytes = getClassBytesFromStream(url.openStream());
            } else {
                logger.log(TreeLogger.ERROR, "Could not find required bootstrap class '" + className + "' in the classpath", null);
                throw new UnableToCompleteException();
            }
        } catch (IOException e) {
            logger.log(TreeLogger.ERROR, "Error reading class bytes for " + className, e);
            throw new UnableToCompleteException();
        }
    }

    private static byte[] getClassBytesFromStream(InputStream is) throws IOException {
        try {
            byte classBytes[] = new byte[is.available()];
            int read = 0;
            while (read < classBytes.length) {
                read += is.read(classBytes, read, classBytes.length - read);
            }
            return classBytes;
        } finally {
            Utility.close(is);
        }
    }

    /**
   * The set of units whose JSNI has already been injected.
   */
    private Set<CompilationUnit> alreadyInjected = new HashSet<CompilationUnit>();

    private final HostedModeClassRewriter classRewriter;

    private CompilationState compilationState;

    private final DispatchClassInfoOracle dispClassInfoOracle = new DispatchClassInfoOracle();

    private Class<?> gwtClass, javaScriptHostClass;

    /**
   * Used by {@link #findClass(String)} to prevent reentrant JSNI injection.
   */
    private boolean isInjectingClass = false;

    private final TreeLogger logger;

    private final Set<String> scriptOnlyClasses = new HashSet<String>();

    private ClassLoader scriptOnlyClassLoader;

    private ShellJavaScriptHost shellJavaScriptHost;

    private final Set<String> singleJsoImplTypes = new HashSet<String>();

    /**
   * Used by {@link #findClass(String)} to prevent reentrant JSNI injection.
   */
    private Stack<CompilationUnit> toInject = new Stack<CompilationUnit>();

    private final TypeOracle typeOracle;

    @SuppressWarnings("unchecked")
    private final Map<Object, Object> weakJavaWrapperCache = new ReferenceIdentityMap(AbstractReferenceMap.WEAK, AbstractReferenceMap.WEAK);

    @SuppressWarnings("unchecked")
    private final Map<Long, Object> weakJsoCache = new ReferenceMap(AbstractReferenceMap.HARD, AbstractReferenceMap.WEAK);

    /**
	 * In theory {@link CompilingClassLoader} is designed to be disconnected from any other {@link ClassLoader},
	 * so uses <code>null</code> as parent {@link ClassLoader}. But for Designer we need to include other
	 * {@link ClassLoader}, such as CGLib to support work with abstract classes.
	 */
    public static ClassLoader parentClassLoader = null;

    public CompilingClassLoader(TreeLogger logger, CompilationState compilationState, ShellJavaScriptHost javaScriptHost) throws UnableToCompleteException {
        super(parentClassLoader);
        this.logger = logger;
        this.compilationState = compilationState;
        this.shellJavaScriptHost = javaScriptHost;
        this.typeOracle = compilationState.getTypeOracle();
        setDefaultAssertionStatus(true);
        ensureJavaScriptHostBytes(logger);
        JClassType jsoType = typeOracle.findType(JsValueGlue.JSO_CLASS);
        if (jsoType != null) {
            Set<JClassType> jsoTypes = new HashSet<JClassType>();
            JClassType[] jsoSubtypes = jsoType.getSubtypes();
            Collections.addAll(jsoTypes, jsoSubtypes);
            jsoTypes.add(jsoType);
            Set<String> jsoTypeNames = new HashSet<String>();
            Map<String, List<String>> jsoSuperTypes = new HashMap<String, List<String>>();
            for (JClassType type : jsoTypes) {
                List<String> types = new ArrayList<String>();
                types.add(getBinaryName(type.getSuperclass()));
                for (JClassType impl : type.getImplementedInterfaces()) {
                    types.add(getBinaryName(impl));
                }
                String binaryName = getBinaryName(type);
                jsoTypeNames.add(binaryName);
                jsoSuperTypes.put(binaryName, types);
            }
            SingleJsoImplData singleJsoImplData = new MySingleJsoImplData();
            MyInstanceMethodOracle mapper = new MyInstanceMethodOracle(jsoTypes, typeOracle.getJavaLangObject(), singleJsoImplData);
            classRewriter = new HostedModeClassRewriter(jsoTypeNames, jsoSuperTypes, singleJsoImplData, mapper);
        } else {
            classRewriter = null;
        }
    }

    /**
   * Retrieves the mapped JSO for a given unique id, provided the id was
   * previously cached and the JSO has not been garbage collected.
   * 
   * Instantiations: changed to long
   * 
   * @param uniqueId the previously stored unique id
   * @return the mapped JSO, or <code>null</code> if the id was not previously
   *         mapped or if the JSO has been garbage collected
   */
    public Object getCachedJso(long uniqueId) {
        return weakJsoCache.get(uniqueId);
    }

    /**
   * Returns the {@link DispatchClassInfo} for a given dispatch id.
   * 
   * @param dispId dispatch identifier
   * @return {@link DispatchClassInfo} for a given dispatch id or null if one
   *         does not exist
   */
    public DispatchClassInfo getClassInfoByDispId(int dispId) {
        return dispClassInfoOracle.getClassInfoByDispId(dispId);
    }

    /**
   * Returns the dispatch id for a JSNI member reference.
   * 
   * @param jsniMemberRef a JSNI member reference
   * @return dispatch id or -1 if the JSNI member reference could not be found
   */
    public int getDispId(String jsniMemberRef) {
        return dispClassInfoOracle.getDispId(jsniMemberRef);
    }

    /**
   * Retrieves the mapped wrapper for a given Java Object, provided the wrapper
   * was previously cached and has not been garbage collected.
   * 
   * @param javaObject the Object being wrapped
   * @return the mapped wrapper, or <code>null</code> if the Java object mapped
   *         or if the wrapper has been garbage collected
   */
    public Object getWrapperForObject(Object javaObject) {
        return weakJavaWrapperCache.get(javaObject);
    }

    /**
   * Weakly caches a given JSO by unique id. A cached JSO can be looked up by
   * unique id until it is garbage collected.
   * 
   * Instantiations: changed to long.
   * 
   * @param uniqueId a unique id associated with the JSO
   * @param jso the value to cache
   */
    public void putCachedJso(long uniqueId, Object jso) {
        weakJsoCache.put(uniqueId, jso);
    }

    /**
   * Weakly caches a wrapper for a given Java Object.
   * 
   * @param javaObject the Object being wrapped
   * @param wrapper the mapped wrapper
   */
    public void putWrapperForObject(Object javaObject, Object wrapper) {
        weakJavaWrapperCache.put(javaObject, wrapper);
    }

    @Override
    protected synchronized Class<?> findClass(String className) throws ClassNotFoundException {
        if (className == null) {
            throw new ClassNotFoundException("null class name", new NullPointerException());
        }
        if (scriptOnlyClasses.contains(className)) {
            throw new ClassNotFoundException();
        }
        if (BRIDGE_CLASS_NAMES.containsKey(className)) {
            return BRIDGE_CLASS_NAMES.get(className);
        }
        byte[] classBytes = findClassBytes(className);
        if (classBytes == null) {
            throw new ClassNotFoundException(className);
        }
        if (HasAnnotation.hasAnnotation(classBytes, GwtScriptOnly.class)) {
            scriptOnlyClasses.add(className);
            maybeInitializeScriptOnlyClassLoader();
            return Class.forName(className, true, scriptOnlyClassLoader);
        }
        boolean localInjection;
        if (!isInjectingClass) {
            localInjection = isInjectingClass = true;
        } else {
            localInjection = false;
        }
        Class<?> newClass = defineClass(className, classBytes, 0, classBytes.length);
        if (className.equals(JavaScriptHost.class.getName())) {
            javaScriptHostClass = newClass;
            updateJavaScriptHost();
        }
        if (!classRewriter.isJsoIntf(className)) {
            CompilationUnit unit = getUnitForClassName(canonicalizeClassName(className));
            if (unit != null) {
                toInject.push(unit);
            }
        }
        if (localInjection) {
            try {
                while (toInject.size() > 0) {
                    CompilationUnit unit = toInject.remove(0);
                    if (!alreadyInjected.contains(unit)) {
                        injectJsniMethods(unit);
                        alreadyInjected.add(unit);
                    }
                }
            } finally {
                isInjectingClass = false;
            }
        }
        if (className.equals("com.google.gwt.core.client.GWT")) {
            gwtClass = newClass;
            updateGwtClass();
        }
        return newClass;
    }

    void clear() {
        shellJavaScriptHost = null;
        scriptOnlyClasses.clear();
        scriptOnlyClassLoader = null;
        updateJavaScriptHost();
        weakJsoCache.clear();
        weakJavaWrapperCache.clear();
        dispClassInfoOracle.clear();
    }

    /**
   * Convert a binary class name into a resource-like name.
   */
    private String canonicalizeClassName(String className) {
        String lookupClassName = className.replace('.', '/');
        if (classRewriter != null && classRewriter.isJsoImpl(className)) {
            lookupClassName = lookupClassName.substring(0, lookupClassName.length() - 1);
        }
        return lookupClassName;
    }

    private byte[] findClassBytes(String className) {
        if (JavaScriptHost.class.getName().equals(className)) {
            return javaScriptHostBytes;
        }
        if (classRewriter != null && classRewriter.isJsoIntf(className)) {
            byte[] newBytes = classRewriter.writeJsoIntf(className);
            if (CLASS_DUMP) {
                classDump(className, newBytes);
            }
            return newBytes;
        }
        String lookupClassName = canonicalizeClassName(className);
        CompiledClass compiledClass = compilationState.getClassFileMap().get(lookupClassName);
        CompilationUnit unit = (compiledClass == null) ? getUnitForClassName(lookupClassName) : compiledClass.getUnit();
        if (emmaAvailable) {
            List<JsniMethod> jsniMethods = (unit == null) ? null : unit.getJsniMethods();
            if (unit != null && !unit.isSuperSource() && !unit.isGenerated() && unit.hasAnonymousClasses() && jsniMethods != null && jsniMethods.size() > 0 && !unit.createdClassMapping()) {
                if (!unit.constructAnonymousClassMappings(logger)) {
                    logger.log(TreeLogger.ERROR, "Our heuristic for mapping anonymous classes between compilers " + "failed. Unsafe to continue because the wrong jsni code " + "could end up running. className = " + className);
                    return null;
                }
            }
        }
        byte classBytes[] = null;
        if (compiledClass != null) {
            classBytes = compiledClass.getBytes();
            if (!compiledClass.getUnit().isSuperSource()) {
                classBytes = emmaStrategy.getEmmaClassBytes(classBytes, lookupClassName, compiledClass.getUnit().getLastModified());
            } else {
                logger.log(TreeLogger.SPAM, "no emma instrumentation for " + lookupClassName + " because it is from super-source");
            }
        } else if (emmaAvailable) {
            if (typeHasCompilationUnit(lookupClassName) && CompilationUnit.isClassnameGenerated(className)) {
                logger.log(TreeLogger.DEBUG, "EmmaStrategy: loading " + lookupClassName + " from disk even though TypeOracle does not know about it");
                classBytes = emmaStrategy.getEmmaClassBytes(null, lookupClassName, 0);
            }
        }
        if (classBytes != null && classRewriter != null) {
            Map<String, String> anonymousClassMap = Collections.emptyMap();
            if (unit != null) {
                anonymousClassMap = unit.getAnonymousClassMap();
            }
            byte[] newBytes = classRewriter.rewrite(typeOracle, className, classBytes, anonymousClassMap);
            if (CLASS_DUMP) {
                if (!Arrays.equals(classBytes, newBytes)) {
                    classDump(className, newBytes);
                }
            }
            classBytes = newBytes;
        }
        if (classBytes != null) {
            if (m_nonAbstractClasses.contains(className)) {
                org.objectweb.asm.ClassReader classReader = new org.objectweb.asm.ClassReader(classBytes);
                AbstractMethodsImplementorVisitor rewriter = new AbstractMethodsImplementorVisitor(className);
                classReader.accept(rewriter, 0);
                classBytes = rewriter.toByteArray();
            }
            if (!className.startsWith("com.google.") && !className.startsWith("com.extjs.") && !className.startsWith("com.smartgwt.")) {
                org.objectweb.asm.ClassReader classReader = new org.objectweb.asm.ClassReader(classBytes);
                GWTDesignTimeVisitor rewriter = new GWTDesignTimeVisitor();
                classReader.accept(rewriter, 0);
                classBytes = rewriter.toByteArray();
            }
            {
                if (Double.parseDouble(System.getProperty("java.class.version")) < org.objectweb.asm.Opcodes.V1_6) {
                    org.objectweb.asm.ClassWriter writer = new org.objectweb.asm.ClassWriter(0);
                    org.objectweb.asm.ClassAdapter v = new ForceClassVersion15(writer);
                    new org.objectweb.asm.ClassReader(classBytes).accept(v, 0);
                    classBytes = writer.toByteArray();
                }
            }
        }
        return classBytes;
    }

    private String getBinaryName(JClassType type) {
        String name = type.getPackage().getName() + '.';
        name += type.getName().replace('.', '$');
        return name;
    }

    private String getBinaryOrPrimitiveName(JType type) {
        JArrayType asArray = type.isArray();
        JClassType asClass = type.isClassOrInterface();
        JPrimitiveType asPrimitive = type.isPrimitive();
        if (asClass != null) {
            return getBinaryName(asClass);
        } else if (asPrimitive != null) {
            return asPrimitive.getQualifiedSourceName();
        } else if (asArray != null) {
            JType componentType = asArray.getComponentType();
            return getBinaryOrPrimitiveName(componentType) + "[]";
        } else {
            throw new InternalCompilerException("Cannot create binary name for " + type.getQualifiedSourceName());
        }
    }

    /**
   * Returns the compilationUnit corresponding to the className. For nested
   * classes, the unit corresponding to the top level type is returned.
   * 
   * Since a file might have several top-level types, search using classFileMap.
   */
    public CompilationUnit getUnitForClassName(String className) {
        String mainTypeName = className;
        int index = mainTypeName.length();
        CompiledClass cc = null;
        while (cc == null && index != -1) {
            mainTypeName = mainTypeName.substring(0, index);
            cc = compilationState.getClassFileMap().get(mainTypeName);
            index = mainTypeName.lastIndexOf('$');
        }
        return cc == null ? null : cc.getUnit();
    }

    private void injectJsniMethods(CompilationUnit unit) {
        if (unit == null || unit.getJsniMethods() == null) {
            return;
        }
        shellJavaScriptHost.createNativeMethods(logger, unit.getJsniMethods(), this);
    }

    private void maybeInitializeScriptOnlyClassLoader() {
        if (scriptOnlyClassLoader == null) {
            scriptOnlyClassLoader = new MultiParentClassLoader(this, Thread.currentThread().getContextClassLoader());
        }
    }

    private boolean typeHasCompilationUnit(String className) {
        return getUnitForClassName(className) != null;
    }

    /**
   * Tricky one, this. Reaches over into this modules's JavaScriptHost class and
   * sets its static 'host' field to our module space.
   * 
   * @see JavaScriptHost
   */
    private void updateGwtClass() {
        if (gwtClass == null) {
            return;
        }
        Throwable caught;
        try {
            GWTBridgeImpl bridge;
            if (shellJavaScriptHost == null) {
                bridge = null;
            } else {
                bridge = new GWTBridgeImpl(shellJavaScriptHost);
            }
            final Class<?>[] paramTypes = new Class[] { GWTBridge.class };
            Method setBridgeMethod = gwtClass.getDeclaredMethod("setBridge", paramTypes);
            setBridgeMethod.setAccessible(true);
            setBridgeMethod.invoke(gwtClass, new Object[] { bridge });
            return;
        } catch (SecurityException e) {
            caught = e;
        } catch (NoSuchMethodException e) {
            caught = e;
        } catch (IllegalArgumentException e) {
            caught = e;
        } catch (IllegalAccessException e) {
            caught = e;
        } catch (InvocationTargetException e) {
            caught = e.getTargetException();
        }
        throw new RuntimeException("Error initializing GWT bridge", caught);
    }

    /**
   * Tricky one, this. Reaches over into this modules's JavaScriptHost class and
   * sets its static 'host' field to our module space.
   * 
   * @see JavaScriptHost
   */
    private void updateJavaScriptHost() {
        if (javaScriptHostClass == null) {
            return;
        }
        Throwable caught;
        try {
            final Class<?>[] paramTypes = new Class[] { ShellJavaScriptHost.class };
            Method setHostMethod = javaScriptHostClass.getMethod("setHost", paramTypes);
            setHostMethod.invoke(javaScriptHostClass, new Object[] { shellJavaScriptHost });
            return;
        } catch (SecurityException e) {
            caught = e;
        } catch (NoSuchMethodException e) {
            caught = e;
        } catch (IllegalArgumentException e) {
            caught = e;
        } catch (IllegalAccessException e) {
            caught = e;
        } catch (InvocationTargetException e) {
            caught = e.getTargetException();
        }
        throw new RuntimeException("Error initializing JavaScriptHost", caught);
    }

    /**
	 * {@link Set} of classes names that should be made non-abstract.
	 */
    private final Set<String> m_nonAbstractClasses = new TreeSet<String>();

    /**
	 * Adds the name of class that should be made non-abstract.
	 */
    public void addNonAbstractClass(String className) {
        m_nonAbstractClasses.add(className);
    }

    public DispatchClassInfo getClassInfoFromClassName(String className) {
        return dispClassInfoOracle.getClassInfoFromClassName(className);
    }
}
