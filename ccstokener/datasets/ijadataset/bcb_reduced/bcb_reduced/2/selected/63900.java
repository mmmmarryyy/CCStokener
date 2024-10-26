package net.sf.joafip.store.service.bytecode.agent;

import java.io.IOException;
import java.io.InputStream;
import java.net.URL;
import java.util.Properties;
import java.util.Set;
import java.util.TreeSet;
import java.util.Map.Entry;
import net.sf.joafip.NotStorableClass;
import net.sf.joafip.asm.ClassReader;
import net.sf.joafip.asm.ClassVisitor;
import net.sf.joafip.asm.ClassWriter;
import net.sf.joafip.logger.JoafipLogger;
import net.sf.joafip.service.ClassLoaderProvider;
import net.sf.joafip.store.entity.bytecode.IMethodMap;
import net.sf.joafip.store.entity.bytecode.MethodMap;
import net.sf.joafip.store.entity.bytecode.agent.EnumTransformationType;
import net.sf.joafip.store.entity.bytecode.agent.PackageNode;

/**
 * 
 * @author luc peuvrier
 * 
 */
@NotStorableClass
public final class PersistableCodeGenerator {

    private static final JoafipLogger LOGGER = JoafipLogger.getLogger(PersistableCodeGenerator.class);

    /** regular expression to split class name */
    private static final String POINT_REGEX = "\\.|\\$";

    private static final ClassLoaderProvider classLoaderProvider = new ClassLoaderProvider();

    /** package to transform manager */
    private final PackageMgr packageMgr = new PackageMgr();

    /** single instance of PersistableCodeGenerator */
    private static PersistableCodeGenerator instance;

    private boolean transformed;

    private EnumTransformationType transformationAttribute;

    /**
	 * 
	 * @return the unique instance of this persistable code generator
	 * @throws IOException
	 */
    public static PersistableCodeGenerator getInstance() throws IOException {
        synchronized (PersistableCodeGenerator.class) {
            if (instance == null) {
                instance = new PersistableCodeGenerator();
            }
        }
        return instance;
    }

    /**
	 * construction:<br>
	 * <ul>
	 * <li>set package/class transformation disabled</li>
	 * <li>load package/class transformation enabled from
	 * joafip_instrumentation.properties file</li>
	 * </ul>
	 * 
	 * @throws IOException
	 */
    private PersistableCodeGenerator() throws IOException {
        super();
        packageMgr.addPackage("net.sf.joafip", EnumTransformationType.NONE);
        packageMgr.addPackage("net.sf.joafip.java.util", EnumTransformationType.STORABLE);
        final URL url = classLoaderProvider.getResource("joafip_instrumentation.properties");
        InputStream inputStream;
        try {
            inputStream = url.openStream();
        } catch (IOException exception) {
            inputStream = null;
        }
        if (inputStream != null) {
            load(inputStream);
        }
    }

    /**
	 * load package/class transformation enabled from properties input stream
	 * 
	 * @param inputStream
	 * @throws IOException
	 */
    private void load(final InputStream inputStream) throws IOException {
        final Properties properties = new Properties();
        properties.load(inputStream);
        final Set<Entry<Object, Object>> entrySet = properties.entrySet();
        for (Entry<Object, Object> entry : entrySet) {
            final String key = (String) entry.getKey();
            final String[] splitedKey = key.split(POINT_REGEX);
            final String first = splitedKey[0];
            final String value = ((String) entry.getValue()).trim();
            if (first.charAt(0) != '@') {
                packageMgr.addClassFromProperties(key);
                classSetup(key, splitedKey, 0, splitedKey.length, value);
            } else if ("@class".equals(first)) {
                packageMgr.addClassFromProperties(key.substring(7));
                classSetup(key, splitedKey, 1, splitedKey.length, value);
            } else if ("@method".equals(first)) {
                final String keyMethodName = splitedKey[splitedKey.length - 1];
                final String methodName;
                if ("*".equals(keyMethodName)) {
                    methodName = null;
                } else {
                    methodName = keyMethodName;
                }
                final EnumTransformationType transformationType = transformationType(keyMethodName, value, false);
                packageMgr.addMethod(splitedKey, 1, splitedKey.length - 1, methodName, transformationType);
            } else {
                throw new IOException("\"" + key + "\" is a bad property key, method:<name>:<flag> expected");
            }
        }
    }

    private void classSetup(final String key, final String[] splitedKey, final int beginIndex, final int endIndex, final String value) throws IOException {
        final EnumTransformationType transformationType = transformationType(key, value, true);
        packageMgr.addPackage(splitedKey, beginIndex, endIndex, transformationType);
    }

    private EnumTransformationType transformationType(final String key, final String value, final boolean forClass) throws IOException {
        final EnumTransformationType transformationType;
        if ("off".equals(value)) {
            transformationType = EnumTransformationType.NONE;
        } else if (forClass && "on".equals(value)) {
            transformationType = EnumTransformationType.STORABLE;
        } else if (!forClass && "on".equals(value)) {
            transformationType = null;
        } else if (forClass && "storable".equals(value)) {
            transformationType = EnumTransformationType.STORABLE;
        } else if ("access".equals(value)) {
            transformationType = EnumTransformationType.STORABLE_ACCESS;
        } else if ("no_storable_access".equals(value)) {
            transformationType = EnumTransformationType.NO_STORABLE_ACCESS;
        } else {
            throw new IOException("\"" + value + "\" is a bad property value for key \"" + key + "\"");
        }
        return transformationType;
    }

    /**
	 * generate transformed byte code of original byte code
	 * 
	 * @param className
	 * @param originalCode
	 *            the bytecode of the class to be transform.
	 * @param off
	 *            the start offset of the class data.
	 * @param len
	 *            the length of the class data.
	 * @return transformed code
	 */
    public byte[] generate(final String className, final byte[] originalCode, final int off, final int len) {
        final EnumTransformationType transformationTypeFromProperties = transformAttribute(className);
        final ClassReader classReader = new ClassReader(originalCode, off, len);
        final IMethodMap methodMap = new MethodMap();
        final Set<String> syntheticFieldSet = new TreeSet<String>();
        final ClassVisitorForStorable classVisitorForStorable = new ClassVisitorForStorable();
        classVisitorForStorable.setMethodMap(methodMap, syntheticFieldSet);
        classReader.accept(classVisitorForStorable, ClassReader.SKIP_DEBUG | ClassReader.SKIP_FRAMES);
        transformationAttribute = classVisitorForStorable.getTransformationType();
        if (transformationAttribute == null) {
            transformationAttribute = transformationTypeFromProperties;
        }
        final String[] splitedClassName = className.split(POINT_REGEX);
        final PackageNode node = packageMgr.addPackage(splitedClassName, 0, splitedClassName.length, transformationAttribute);
        byte[] code = null;
        if (EnumTransformationType.NONE.equals(transformationAttribute) && !methodMap.hasMethodToTransform()) {
            transformed = false;
        } else {
            transformed = true;
        }
        if (transformed) {
            final ClassWriter classWriter = new AgentClassWriter(ClassWriter.COMPUTE_FRAMES);
            final ClassVisitor classVisitor = new ClassVisitorForPersistable(classWriter, methodMap, syntheticFieldSet, transformationAttribute, node);
            classReader.accept(classVisitor, 0);
            code = classWriter.toByteArray();
        }
        return code;
    }

    public boolean isTransformed() {
        return transformed;
    }

    public EnumTransformationType getTransformationAttribute() {
        return transformationAttribute;
    }

    /**
	 * 
	 * @param internalClassNameParam
	 * @return transform attribute
	 */
    EnumTransformationType transformAttribute(final String internalClassNameParam) {
        final String internalClassName = internalClassNameParam.replace('/', '.');
        final EnumTransformationType transformAttribute;
        final String[] split = internalClassName.split(POINT_REGEX);
        if (split.length == 1) {
            transformAttribute = EnumTransformationType.STORABLE;
        } else {
            transformAttribute = packageMgr.transformAttribute(split);
        }
        if (LOGGER.debugEnabled) {
            LOGGER.debug("transform=" + transformAttribute + " for " + internalClassName);
        }
        return transformAttribute;
    }

    public String[] classFromProperties() {
        return packageMgr.classFromProperties();
    }
}
