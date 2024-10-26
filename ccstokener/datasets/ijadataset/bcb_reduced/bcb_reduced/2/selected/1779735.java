package self.micromagic.eterna.digester;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.io.Writer;
import java.lang.reflect.Constructor;
import java.lang.reflect.Method;
import java.lang.reflect.Modifier;
import java.net.URL;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.StringTokenizer;
import java.util.zip.DeflaterOutputStream;
import java.util.zip.InflaterInputStream;
import org.apache.commons.collections.ReferenceMap;
import org.apache.commons.digester.Digester;
import org.apache.commons.logging.Log;
import org.dom4j.Document;
import org.dom4j.DocumentHelper;
import org.dom4j.Element;
import org.dom4j.io.XMLWriter;
import org.xml.sax.SAXException;
import self.micromagic.coder.Base64;
import self.micromagic.eterna.share.AttributeManager;
import self.micromagic.eterna.share.EternaFactory;
import self.micromagic.eterna.share.EternaFactoryImpl;
import self.micromagic.eterna.share.EternaInitialize;
import self.micromagic.eterna.share.Factory;
import self.micromagic.eterna.share.ThreadCache;
import self.micromagic.eterna.share.Tool;
import self.micromagic.util.FormatTool;
import self.micromagic.util.ObjectRef;
import self.micromagic.util.StringRef;
import self.micromagic.util.Utility;

/**
 * ����˵��:
 *
 * self.micromagic.eterna.digester.initfiles
 * Ҫ����ȫ�ֳ�ʼ�����ļ��б�
 *
 * self.micromagic.eterna.digester.subinitfiles
 * Ҫ����ȫ�ֳ�ʼ�������ļ��б�,
 * ���ļ��б��еĶ���Ḳ�ǵ�ȫ�ֳ�ʼ�����ļ��б��е�ͬ�����
 *
 * self.micromagic.eterna.digester.initClasses
 * Ҫ����ȫ�ֳ�ʼ���������б�
 *
 * self.micromagic.eterna.digester.loadDefaultConfig
 * ȫ�ֳ�ʼ��ʱ�Ƿ�Ҫ����Ĭ�ϵ�����
 * cp:self/micromagic/eterna/share/eterna_share.xml;cp:eterna_global.xml;
 *
 */
public class FactoryManager {

    public static final Log log = Tool.log;

    /**
    * Ҫ����ȫ�ֳ�ʼ�����ļ��б�.
    */
    public static final String INIT_FILES_PROPERTY = "self.micromagic.eterna.digester.initfiles";

    /**
    * Ҫ����ȫ�ֳ�ʼ�������ļ��б�, ���ļ��б��еĶ���Ḳ�ǵ�ȫ�ֳ�ʼ��
    * ���ļ��б��е�ͬ�����.
    */
    public static final String INIT_SUBFILES_PROPERTY = "self.micromagic.eterna.digester.subinitfiles";

    /**
    * Ҫ����ȫ�ֳ�ʼ���������б�.
    */
    public static final String INIT_CLASSES_PROPERTY = "self.micromagic.eterna.digester.initClasses";

    /**
    * ȫ�ֳ�ʼ��ʱ�Ƿ�Ҫ����Ĭ�ϵ�����.
    */
    public static final String LOAD_DEFAULT_CONFIG = "self.micromagic.eterna.digester.loadDefaultConfig";

    /**
    * ȫ�ֳ�ʼ��ʱҪ�����Ĭ������.
    */
    public static final String DEFAULT_CONFIG_FILE = "cp:self/micromagic/eterna/share/eterna_share.xml;cp:eterna_global.xml;";

    /**
    * ʵ���ʼ�����ļ��б�.
    */
    public static final String CONFIG_INIT_FILES = "initFiles";

    /**
    * ʵ���ʼ���ĸ��ļ��б�.
    */
    public static final String CONFIG_INIT_PARENTFILES = "parentFiles";

    /**
    * ʵ���ʼ���������б�.
    */
    public static final String CONFIG_INIT_NAME = "initConfig";

    /**
    * ʵ���ʼ���ĸ������б�.
    */
    public static final String CONFIG_INIT_PARENTNAME = "parentConfig";

    /**
    * ��ʼ��ʱʹ�õ��̻߳���.
    */
    public static final String ETERNA_INIT_CACHE = "eterna.init.cache";

    /**
    * Ĭ����Ҫ���صĹ���EternaFactory.
    */
    public static final String ETERNA_FACTORY = "self.micromagic.eterna.EternaFactory";

    /**
    * ��ʼ��ʱ�Ƿ���Ҫ�Խű����Խ����﷨���.
    */
    public static final String CHECK_GRAMMER_PROPERTY = "self.micromagic.eterna.digester.checkGrammer";

    private static boolean checkGrammer = true;

    /**
    * ȫ�ֹ���ʵ���id.
    */
    public static final String GLOBAL_INSTANCE_ID = "instance.global";

    private static Document logDocument = null;

    private static Element logs = null;

    private static Map classInstanceMap = new HashMap();

    private static GlobalImpl globalInstance;

    private static Instance current;

    private static Factory currentFactory;

    /**
    * ��ʶ��ǰ�Ƿ��ڳ�ʼ��������
    */
    private static int superInitLevel = 0;

    static {
        globalInstance = new GlobalImpl();
        current = globalInstance;
        try {
            reInitEterna();
            Utility.addMethodPropertyManager(CHECK_GRAMMER_PROPERTY, FactoryManager.class, "setCheckGrammer");
        } catch (Throwable ex) {
            log.error("Error in class init.", ex);
        }
    }

    /**
    * �Ƿ��ڳ�ʼ��������
    */
    public static boolean isSuperInit() {
        return superInitLevel > 0;
    }

    /**
    * ��ʼ�������õ�level�ȼ�, 0Ϊ������ 1Ϊ��һ�� 2Ϊ�ڶ��� ...
    */
    public static int getSuperInitLevel() {
        return superInitLevel;
    }

    /**
    * ��ʼ��ʱ�Ƿ���Ҫ�Խű����Խ����﷨���.
    */
    public static boolean isCheckGrammer() {
        return checkGrammer;
    }

    /**
    * ���ó�ʼ��ʱ�Ƿ���Ҫ�Խű����Խ����﷨���.
    *
    * @param check   ���trueΪ��Ҫ
    */
    public static void setCheckGrammer(String check) {
        checkGrammer = "true".equalsIgnoreCase(check);
    }

    /**
    * ���һ����¼SQL��־�Ľڵ�.
    *
    * @param name   SQL���������
    */
    public static synchronized Element createLogNode(String name) {
        if (logDocument == null) {
            logDocument = DocumentHelper.createDocument();
            Element root = logDocument.addElement("eterna");
            logs = root.addElement("logs");
        }
        if (logs.elements().size() > 2048) {
            Iterator itr = logs.elementIterator();
            try {
                for (int i = 0; i < 1536; i++) {
                    itr.next();
                    itr.remove();
                }
            } catch (Exception ex) {
                log.warn("Remove sql log error.", ex);
                logDocument = null;
                return createLogNode(name);
            }
        }
        return logs.addElement(name);
    }

    /**
    * ����¼����־���.
    *
    * @param out     ��־�������
    * @param clear   �Ƿ�Ҫ�������������־
    */
    public static synchronized void printLog(Writer out, boolean clear) throws IOException {
        if (logDocument == null) {
            return;
        }
        XMLWriter writer = new XMLWriter(out);
        writer.write(logDocument);
        writer.flush();
        if (clear) {
            logDocument = null;
            logs = null;
        }
    }

    /**
    * ��õ�ǰ���ڳ�ʼ����Factory.
    * ֻ���ڳ�ʼ��ʱ�Ż᷵��ֵ, ���򷵻�null.
    */
    public static Factory getCurrentFactory() {
        return currentFactory;
    }

    /**
    * ��õ�ǰ���ڳ�ʼ���Ĺ�����������ʵ��.
    * ����ڳ�ʼ��ʱ���򷵻�ȫ�ֵĹ�����������ʵ��.
    */
    public static Instance getCurrentInstance() {
        return current;
    }

    /**
    * ���ȫ�ֵĹ�����������ʵ��.
    */
    public static Instance getGlobalFactoryManager() {
        return globalInstance;
    }

    /**
    * @deprecated
    * @see #getGlobalFactoryManager
    */
    public static Instance getGlobeFactoryManager() {
        return globalInstance;
    }

    /**
    * ��� ������ ���������� ��������ַ�.
    */
    private static String getConfig(String initConfig, String[] parentConfig) {
        List result = new ArrayList();
        if (initConfig != null) {
            parseConfig(initConfig, result);
        }
        if (result.size() == 0) {
            result.add("");
        }
        if (parentConfig != null) {
            for (int i = 0; i < parentConfig.length; i++) {
                if (parentConfig[i] != null) {
                    parseConfig(parentConfig[i], result);
                }
            }
        }
        if (result.size() <= 1 && initConfig == null) {
            return null;
        }
        StringBuffer buf = new StringBuffer();
        Iterator itr = result.iterator();
        while (itr.hasNext()) {
            buf.append(itr.next()).append('|');
        }
        return buf.toString();
    }

    /**
    * ��������.
    *
    * @param config    Ҫ����������
    * @param result    ������Ľ���б�, ���ν����Ľ��ҲҪ�Ž�ȥ
    */
    private static void parseConfig(String config, List result) {
        String temp;
        List tmpSet = new ArrayList();
        if (config != null) {
            StringTokenizer token = new StringTokenizer(resolveLocate(config), ";");
            while (token.hasMoreTokens()) {
                temp = token.nextToken().trim();
                if (temp.length() == 0) {
                    continue;
                }
                tmpSet.add(temp);
            }
        }
        StringBuffer buf = new StringBuffer();
        Iterator itr = tmpSet.iterator();
        while (itr.hasNext()) {
            buf.append(itr.next()).append(';');
        }
        result.add(buf.toString());
    }

    /**
    * ��һ���������л�����.
    *
    * @param f         Ҫ���л�����Ĺ���
    * @param oOut      ���л������
    */
    public static void writeFactory(Factory f, ObjectOutputStream oOut) throws IOException, ConfigurationException {
        oOut.writeUTF(f.getFactoryManager().getId());
        oOut.writeUTF(f.getName());
        oOut.writeUTF(f.getClass().getName());
    }

    /**
    * ͨ�����л����һ������.
    *
    * @param oIn      �����л�������
    * @return   �����л���Ĺ���
    */
    public static Factory readFactory(ObjectInputStream oIn) throws IOException, ConfigurationException {
        String id = oIn.readUTF();
        String fName = oIn.readUTF();
        String cName = oIn.readUTF();
        Instance instance = getFactoryManager(id);
        return instance.getFactory(fName, cName);
    }

    /**
    * ���id��ȡ������������ʵ��.
    *
    * @param id    ������������id
    * @return  ������������ʵ��
    * @throws ConfigurationException    ���û�ж�Ӧid��ʵ��, ���׳����쳣
    */
    public static Instance getFactoryManager(String id) throws ConfigurationException {
        if (GLOBAL_INSTANCE_ID.equals(id)) {
            return globalInstance;
        }
        Instance instance = (Instance) classInstanceMap.get(id);
        if (instance == null) {
            throw new ConfigurationException("Not fount the instance [" + id + "] [" + globalInstance.parseInstanceId(id) + "]");
        }
        return instance;
    }

    /**
    * ���һ���ഴ��������������ʵ��.
    * �Ὣ[����.xml]��Ϊ��������ȡ.
    *
    * @param baseClass    ��ʼ���Ļ���
    */
    public static Instance createClassFactoryManager(Class baseClass) {
        return createClassFactoryManager(baseClass, null);
    }

    /**
    * ���һ���༰���ô���������������ʵ��.
    *
    * @param baseClass    ��ʼ���Ļ���
    * @param initConfig   ��ʼ��������
    */
    public static Instance createClassFactoryManager(Class baseClass, String initConfig) {
        if (!Instance.class.isAssignableFrom(baseClass)) {
            String id = globalInstance.createInstanceId(getConfig(initConfig, null), baseClass.getName());
            Object instance = classInstanceMap.get(id);
            if (instance != null && instance instanceof ClassImpl) {
                ClassImpl ci = (ClassImpl) instance;
                if (ci.baseClass == baseClass) {
                    return ci;
                }
            }
        }
        return createClassFactoryManager(baseClass, null, initConfig, null, false);
    }

    /**
    * ���һ���༰���ô���������������ʵ��.
    *
    * @param baseClass    ��ʼ���Ļ���
    * @param initConfig   ��ʼ��������
    * @param registry     �Ƿ���Ҫ����ע���ʵ��, ��Ϊtrue��Ὣԭ���Ѵ��ڵ�ʵ��ɾ��
    */
    public static Instance createClassFactoryManager(Class baseClass, String initConfig, boolean registry) {
        return createClassFactoryManager(baseClass, null, initConfig, null, registry);
    }

    /**
    * ���һ���༰���ô���������������ʵ��.
    *
    * @param baseClass    ��ʼ���Ļ���
    * @param baseObj      �����һ��ʵ��
    * @param initConfig   ��ʼ��������
    * @param registry     �Ƿ���Ҫ����ע���ʵ��, ��Ϊtrue��Ὣԭ���Ѵ��ڵ�ʵ��ɾ��
    */
    public static Instance createClassFactoryManager(Class baseClass, Object baseObj, String initConfig, boolean registry) {
        return createClassFactoryManager(baseClass, baseObj, initConfig, null, registry);
    }

    /**
    * ���һ���༰���ô���������������ʵ��.
    *
    * @param baseClass        ��ʼ���Ļ���
    * @param baseObj          �����һ��ʵ��
    * @param initConfig       ��ʼ��������
    * @param parentConfig     ��ʼ���ĸ�����
    * @param registry         �Ƿ���Ҫ����ע���ʵ��, ��Ϊtrue��Ὣԭ���Ѵ��ڵ�ʵ��ɾ��
    */
    public static Instance createClassFactoryManager(Class baseClass, Object baseObj, String initConfig, String[] parentConfig, boolean registry) {
        Class instanceClass = null;
        if (Instance.class.isAssignableFrom(baseClass)) {
            instanceClass = baseClass;
        }
        return createClassFactoryManager(baseClass, baseObj, initConfig, parentConfig, instanceClass, registry);
    }

    /**
    * ���һ���༰���ô���������������ʵ��.
    *
    * @param baseClass        ��ʼ���Ļ���
    * @param baseObj          �����һ��ʵ��
    * @param initConfig       ��ʼ��������
    * @param parentConfig     ��ʼ���ĸ�����
    * @param instanceClass    ������������ʵ����
    * @param regist           �Ƿ���Ҫ����ע���ʵ��, ��Ϊtrue��Ὣԭ���Ѵ��ڵ�ʵ��ɾ��
    */
    public static synchronized Instance createClassFactoryManager(Class baseClass, Object baseObj, String initConfig, String[] parentConfig, Class instanceClass, boolean regist) {
        Instance instance = null;
        if (instanceClass != null) {
            if (Instance.class.isAssignableFrom(instanceClass)) {
                try {
                    ObjectRef ref = new ObjectRef();
                    Constructor constructor = findConstructor(instanceClass, ref, baseClass, baseObj, initConfig, parentConfig);
                    if (constructor != null) {
                        if (!constructor.isAccessible()) {
                            constructor.setAccessible(true);
                            Object[] params = (Object[]) ref.getObject();
                            instance = (Instance) constructor.newInstance(params);
                            constructor.setAccessible(false);
                        } else {
                            Object[] params = (Object[]) ref.getObject();
                            instance = (Instance) constructor.newInstance(params);
                        }
                    }
                } catch (Throwable ex) {
                    String msg = "Error in createClassFactoryManager, when create special instance class:" + instanceClass + ".";
                    log.error(msg, ex);
                    throw new RuntimeException(msg);
                }
            } else {
                String msg = "Error in createClassFactoryManager, unexpected instance class type:" + instanceClass + ".";
                throw new RuntimeException(msg);
            }
        }
        if (instance == null) {
            if (EternaInitialize.class.isAssignableFrom(baseClass)) {
                try {
                    Method method = baseClass.getDeclaredMethod("autoReloadTime", new Class[0]);
                    Long autoReloadTime;
                    if (!method.isAccessible()) {
                        method.setAccessible(true);
                        autoReloadTime = (Long) method.invoke(baseObj, new Object[0]);
                        method.setAccessible(false);
                    } else {
                        autoReloadTime = (Long) method.invoke(baseObj, new Object[0]);
                    }
                    instance = new AutoReloadImpl(baseClass, baseObj, initConfig, parentConfig, autoReloadTime.longValue());
                } catch (Throwable ex) {
                    log.info("At createClassFactoryManager, when invoke autoReloadTime:" + baseClass + ".");
                }
            }
            if (instance == null) {
                instance = new ClassImpl(baseClass, baseObj, initConfig, parentConfig);
            }
        }
        String id = instance.getId();
        if (!regist) {
            Instance tmp = (Instance) classInstanceMap.get(id);
            if (tmp != null) {
                if (tmp instanceof ClassImpl) {
                    ClassImpl ci = (ClassImpl) tmp;
                    if (ci.baseClass == baseClass) {
                        ci.addInitializedListener(baseObj);
                        return ci;
                    }
                } else {
                    tmp.addInitializedListener(baseObj);
                    return tmp;
                }
            }
        }
        current = instance;
        instance.reInit(null);
        current = globalInstance;
        Instance old = (Instance) classInstanceMap.put(id, instance);
        if (old != null) {
            old.destroy();
        }
        return instance;
    }

    /**
    * �ӹ�����������ʵ������Ѱ��һ�����ʵĹ��캯��.
    *
    * @param params           ����, ������ʱʹ�õĲ���
    * @param baseClass        ��ʼ���Ļ���
    * @param baseObj          �����һ��ʵ��
    * @param initConfig       ��ʼ��������
    * @param parentConfig     ��ʼ���ĸ�����
    * @param instanceClass    ������������ʵ����
    */
    private static Constructor findConstructor(Class instanceClass, ObjectRef params, Class baseClass, Object baseObj, String initConfig, String[] parentConfig) {
        Constructor[] constructors = instanceClass.getDeclaredConstructors();
        Constructor constructor = null;
        Class[] paramTypes = new Class[0];
        CONSTRUCTOR_LOOP: for (int i = 0; i < constructors.length; i++) {
            Constructor tmpC = constructors[i];
            Class[] types = tmpC.getParameterTypes();
            if (types.length >= paramTypes.length && types.length <= 4) {
                Object[] tmpParams = new Object[types.length];
                for (int j = 0; j < types.length; j++) {
                    if (Object.class == types[j]) {
                        tmpParams[j] = baseObj;
                    } else if (Class.class == types[j]) {
                        tmpParams[j] = baseClass;
                    } else if (String.class == types[j]) {
                        tmpParams[j] = initConfig;
                    } else if (String[].class == types[j]) {
                        tmpParams[j] = parentConfig;
                    } else {
                        continue CONSTRUCTOR_LOOP;
                    }
                }
                paramTypes = types;
                constructor = tmpC;
                params.setObject(tmpParams);
            }
        }
        if (constructor == null) {
            log.error("In instance class type:" + instanceClass + ", can't find proper constructor.");
        }
        return constructor;
    }

    /**
    * (����)��ʼ�����еĹ�����������ʵ��.
    */
    public static void reInitEterna() {
        reInitEterna(null);
    }

    /**
    * (����)��ʼ�����еĹ�����������ʵ��.
    *
    * @param msg        ����, ��ʼ������з��ص���Ϣ
    */
    public static synchronized void reInitEterna(StringRef msg) {
        current = globalInstance;
        globalInstance.reInit(msg);
        Iterator itr = classInstanceMap.values().iterator();
        while (itr.hasNext()) {
            Instance instance = (Instance) itr.next();
            current = instance;
            instance.reInit(msg);
        }
        current = globalInstance;
    }

    /**
    * �ӵ�ǰ������������ʵ���л�ȡһ������ʵ��.
    *
    * @param name          ���������
    * @param className     ������ʵ�������
    */
    public static synchronized Factory getFactory(String name, String className) throws ConfigurationException {
        return current.getFactory(name, className);
    }

    /**
    * ��һ������ʵ�����õ���ǰ������������ʵ����.
    *
    * @param name          ���������
    * @param factory       ����ʵ��
    */
    static synchronized void addFactory(String name, Factory factory) throws ConfigurationException {
        current.addFactory(name, factory);
    }

    /**
    * ��ȫ�ֹ���������ʵ���л�ȡһ��EternaFactoryʵ��.
    */
    public static EternaFactory getEternaFactory() throws ConfigurationException {
        return globalInstance.getEternaFactory();
    }

    /**
    * ��ȡ��ʼ���Ļ���.
    */
    public static Map getInitCache() {
        Map cache = (Map) ThreadCache.getInstance().getProperty(ETERNA_INIT_CACHE);
        return cache;
    }

    /**
    * ���ó�ʼ���Ļ���.
    */
    public static void setInitCache(Map cache) {
        if (cache == null) {
            ThreadCache.getInstance().removeProperty(ETERNA_INIT_CACHE);
        } else {
            ThreadCache.getInstance().setProperty(ETERNA_INIT_CACHE, cache);
        }
    }

    /**
    * ���������е�������Ϣ.
    */
    private static String resolveLocate(String locate) {
        return Utility.resolveDynamicPropnames(locate);
    }

    /**
    * ��ݵ�ַ�������ȡ���õ������.
    *
    * @param locate       ���õĵ�ַ
    * @param baseClass    ��ʼ���Ļ���
    */
    private static InputStream getConfigStream(String locate, Class baseClass) throws IOException {
        if (locate.startsWith("cp:")) {
            URL url;
            if (baseClass == null) {
                url = Utility.getContextClassLoader().getResource(locate.substring(3));
            } else {
                url = baseClass.getClassLoader().getResource(locate.substring(3));
            }
            if (url != null) {
                return url.openStream();
            }
            return null;
        } else if (locate.startsWith("http:")) {
            URL url = new URL(locate);
            return url.openStream();
        } else if (locate.startsWith("note:")) {
            return null;
        } else {
            File file = new File(locate);
            return file.isFile() ? new FileInputStream(file) : null;
        }
    }

    /**
    * ��ȡxml�Ľ�����.
    */
    private static Digester createDigester() {
        Digester digester = new Digester();
        URL url = FactoryManager.class.getClassLoader().getResource("self/micromagic/eterna/digester/eterna_1_5.dtd");
        digester.register("eterna", url.toString());
        digester.addRuleSet(new ShareSet());
        digester.addRuleSet(new SQLRuleSet());
        digester.addRuleSet(new SearchRuleSet());
        digester.addRuleSet(new ModelRuleSet());
        digester.addRuleSet(new ViewRuleSet());
        return digester;
    }

    /**
    * FactoryManagerʵ�����Ķ��������.
    */
    public static class ContainObject {

        public final Instance shareInstance;

        public final Object baseObj;

        public final String name;

        public ContainObject(Instance shareInstance, Object baseObj) {
            this.shareInstance = shareInstance;
            this.baseObj = baseObj;
            this.name = "";
        }

        public ContainObject(Instance shareInstance, Object baseObj, String name) {
            this.shareInstance = shareInstance;
            this.baseObj = baseObj;
            this.name = name;
        }
    }

    /**
    * FactoryManager��ʵ��ӿ�.
    */
    public interface Instance {

        /**
       * ��ñ�����������ʵ���id.
       */
        String getId();

        /**
       * ��ñ������������ĳ�ʼ������.
       */
        String getInitConfig();

        /**
       * (����)��ʼ������
       * @param msg  ��ų�ʼ���ķ�����Ϣ
       */
        void reInit(StringRef msg);

        /**
       * �����Զ��������. <p>
       * ��Щ���Ի���(����)��ʼ��ʱ, ���initCache�е�ֵ���и���.
       *
       * @param name   ���Ե����
       * @param attr   ����ֵ
       */
        void setAttribute(String name, Object attr);

        /**
       * �Ƴ��Զ��������.
       *
       * @param name   ���Ե����
       */
        void removeAttribute(String name);

        /**
       * ��ȡ�Զ��������.
       *
       * @param name   ���Ե����
       * @return   ����ֵ
       */
        Object getAttribute(String name);

        /**
       * ���һ����ʼ��������. <p>
       * �˶������ʵ��<code>self.micromagic.eterna.share.EternaInitialize</code>�ӿ�,
       * �����붨��afterEternaInitialize(FactoryManager.Instance)����, �ڳ�ʼ����Ϻ�
       * ����ô˷���.
       *
       * @param obj    ��ʼ��������
       * @see self.micromagic.eterna.share.EternaInitialize
       */
        void addInitializedListener(Object obj);

        /**
       * ���һ������ʵ��.
       *
       * @param name       ����������
       * @param className  ����ʵ������
       * @return   ����ʵ��
       */
        Factory getFactory(String name, String className) throws ConfigurationException;

        /**
       * ���һ������ʵ��.
       *
       * @param name        ����������
       * @param factory     ����ʵ��
       */
        void addFactory(String name, Factory factory) throws ConfigurationException;

        /**
       * ��÷�����Ϊ"eterna"�Ĺ���ʵ��.
       */
        EternaFactory getEternaFactory() throws ConfigurationException;

        /**
       * ���˹���ʵ����������ڽ���ʱ, ����ô˷���.
       */
        void destroy();
    }

    /**
    * FactoryManager��ʵ��ӿڵĳ���ʵ����, ʵ����һЩ���õķ���.
    */
    public abstract static class AbstractInstance implements Instance {

        private static final Base64 ID_CODER = new Base64("0123456789abcedfghijklmnopqrstuvwxyzABCEDFGHIJKLMNOPQRSTUVWXYZ$_.".toCharArray());

        private static final String CODER_PREFIX = "#ID:";

        protected String prefixName = "";

        protected Map listenerMap = null;

        protected Map instanceMaps = new HashMap();

        protected boolean initialized = false;

        protected Throwable initException = null;

        protected boolean initFactorys = false;

        protected Factory defaultFactory = null;

        protected Instance shareInstance = null;

        protected AttributeManager attrs = new AttributeManager();

        /**
       * �������ʵ�����ʵ��.
       */
        protected void setShareInstance(Instance shareInstance) {
            if (shareInstance == null) {
                this.shareInstance = globalInstance;
            } else {
                this.shareInstance = shareInstance;
            }
        }

        /**
       * �����Զ��������. <p>
       * ��Щ���Ի���(����)��ʼ��ʱ, ���initCache�е�ֵ���и���.
       *
       * @param name   ���Ե����
       * @param attr   ����ֵ
       * @see FactoryManager#getInitCache
       */
        public void setAttribute(String name, Object attr) {
            this.attrs.setAttribute(name, attr);
        }

        /**
       * �Ƴ��Զ��������.
       *
       * @param name   ���Ե����
       */
        public void removeAttribute(String name) {
            this.attrs.removeAttribute(name);
        }

        /**
       * ��ȡ�Զ��������.
       *
       * @param name   ���Ե����
       * @return   ����ֵ
       */
        public Object getAttribute(String name) {
            Object attr = this.attrs.getAttribute(name);
            if (attr == null && this.shareInstance != null) {
                attr = this.shareInstance.getAttribute(name);
            }
            return attr;
        }

        /**
       * ����Instance��id.
       */
        protected String parseInstanceId(String id) {
            if (id != null && id.startsWith(CODER_PREFIX)) {
                try {
                    byte[] buf = ID_CODER.base64ToByteArray(id.substring(CODER_PREFIX.length()));
                    ByteArrayInputStream byteIn = new ByteArrayInputStream(buf);
                    InflaterInputStream in = new InflaterInputStream(byteIn);
                    ByteArrayOutputStream byteOut = new ByteArrayOutputStream(128);
                    Utility.copyStream(in, byteOut);
                    in.close();
                    byte[] result = byteOut.toByteArray();
                    return new String(result, "UTF-8");
                } catch (IOException ex) {
                    throw new Error();
                }
            }
            return id;
        }

        /**
       * ����һ��Instance��id.
       */
        protected String createInstanceId(String configString, String baseName) {
            try {
                String tmp;
                if (configString == null) {
                    tmp = baseName;
                } else {
                    tmp = baseName + "+" + configString;
                }
                if (this.prefixName.length() > 0) {
                    tmp = this.prefixName + "+" + tmp;
                }
                if (tmp.length() < 50) {
                    return tmp;
                }
                ByteArrayOutputStream byteOut = new ByteArrayOutputStream(128);
                DeflaterOutputStream out = new DeflaterOutputStream(byteOut);
                byte[] buf = tmp.getBytes("UTF-8");
                out.write(buf);
                out.close();
                byte[] result = byteOut.toByteArray();
                return CODER_PREFIX + ID_CODER.byteArrayToBase64(result);
            } catch (IOException ex) {
                throw new Error();
            }
        }

        /**
       * �����Ϻõ������ַ�.
       */
        protected String getConfigString(String initConfig, String[] parentConfig) {
            return getConfig(initConfig, parentConfig);
        }

        /**
       * ���ó�ʼ���ĵȼ�.
       */
        protected void setSuperInitLevel(int level) {
            FactoryManager.superInitLevel = level;
        }

        /**
       * ������õ�������.
       */
        protected InputStream getConfigStream(String locate, Class baseClass) throws IOException, ConfigurationException {
            return FactoryManager.getConfigStream(locate, baseClass);
        }

        /**
       * (����)��ʼ������������
       * @param msg  ��ų�ʼ���ķ�����Ϣ
       */
        public void reInit(StringRef msg) {
            synchronized (FactoryManager.class) {
                Map attrs = FactoryManager.getInitCache();
                if (attrs != null) {
                    Iterator itr = attrs.entrySet().iterator();
                    while (itr.hasNext()) {
                        Map.Entry entry = (Map.Entry) itr.next();
                        if (entry.getValue() == null) {
                            this.removeAttribute((String) entry.getKey());
                        } else {
                            this.setAttribute((String) entry.getKey(), entry.getValue());
                        }
                    }
                }
                SameCheckRule.initDealedObjMap();
                Instance oldInstance = FactoryManager.current;
                Factory oldCF = FactoryManager.currentFactory;
                this.destroy();
                FactoryManager.currentFactory = null;
                FactoryManager.current = this;
                this.initialized = false;
                this.initException = null;
                this.instanceMaps.clear();
                this.defaultFactory = null;
                try {
                    ThreadCache.getInstance().setProperty(ConfigurationException.IN_INITIALIZE, "1");
                    this.initializeXML(msg);
                    ConfigurationException.config = null;
                    ConfigurationException.objName = null;
                    this.initializeFactorys();
                    ConfigurationException.objName = null;
                    this.initializeElse();
                    this.initialized = true;
                } catch (Throwable ex) {
                    this.initException = ex;
                    StringBuffer temp = new StringBuffer();
                    if (ConfigurationException.config != null) {
                        temp.append("Config:").append(ConfigurationException.config).append("; ");
                    } else {
                        temp.append("InitConfig:{").append(this.getInitConfig()).append("}; ");
                    }
                    if (ConfigurationException.objName != null) {
                        temp.append("Object:").append(ConfigurationException.objName).append("; ");
                    }
                    temp.append("Message:").append("When " + this.getClass().getName() + " initialize.");
                    log.error(temp.toString(), ex);
                    if (msg != null) {
                        if (msg.getString() != null) {
                            StringBuffer tmpBuf = new StringBuffer();
                            tmpBuf.append(msg.getString());
                            tmpBuf.append(Utility.LINE_SEPARATOR);
                            tmpBuf.append(temp.toString());
                            temp = tmpBuf;
                        }
                        msg.setString(temp.append("[").append(ex.getMessage()).append("]").toString());
                    }
                    ConfigurationException.config = null;
                    ConfigurationException.objName = null;
                } finally {
                    ThreadCache.getInstance().removeProperty(ConfigurationException.IN_INITIALIZE);
                    FactoryManager.currentFactory = oldCF;
                    FactoryManager.current = oldInstance;
                    SameCheckRule.clearDealedObjMap();
                }
            }
        }

        /**
       * ���xml�������г�ʼ��. <p>
       * ��xml��������������г�ʼ��, ������ͨ��
       * <code>createDigester()</code>�������.
       *
       * @param msg  ��ų�ʼ���ķ�����Ϣ
       *
       * @see #createDigester()
       */
        protected abstract void initializeXML(StringRef msg) throws Throwable;

        /**
       * ����������xml�������г�ʼ��.
       *
       * @param config       ������Ϣ
       * @param baseClass    ��ʼ��ʹ�õĻ���
       * @param digester     ��ʼ���Ľ�����
       *
       * @throws IOException               ���xml��ʱ���ֵ��쳣
       * @throws ConfigurationException    ��ʼ��ʱ���ֵ��쳣
       * @throws SAXException              ����xmlʱ���ֵ��쳣
       */
        protected void dealXML(String config, Class baseClass, Digester digester) throws IOException, ConfigurationException, SAXException {
            StringTokenizer token = new StringTokenizer(resolveLocate(config), ";");
            while (token.hasMoreTokens()) {
                String temp = token.nextToken().trim();
                if (temp.length() == 0) {
                    continue;
                }
                ConfigurationException.config = temp;
                ConfigurationException.objName = null;
                InputStream is = this.getConfigStream(temp, baseClass);
                if (is != null) {
                    log.debug("The XML locate is \"" + temp + "\".");
                    digester.parse(is);
                    is.close();
                } else if (!temp.startsWith("note:")) {
                    log.info("The XML locate \"" + temp + "\" not avilable.");
                }
            }
        }

        /**
       * ��ʼ����ɺ�, ����ʣ������.
       * ��֪ͨ������.
       */
        protected void initializeElse() throws ConfigurationException {
            if (this.listenerMap != null) {
                this.callAfterEternaInitialize(this.listenerMap.keySet());
            }
        }

        /**
       * ���һ����ʼ��������.
       */
        public void addInitializedListener(Object obj) {
            if (obj == null) {
                return;
            }
            Class theClass;
            if (obj instanceof Class) {
                theClass = (Class) obj;
            } else {
                theClass = obj.getClass();
            }
            if (!EternaInitialize.class.isAssignableFrom(theClass)) {
                return;
            }
            try {
                Method method = theClass.getDeclaredMethod("afterEternaInitialize", new Class[] { Instance.class });
                if (this.listenerMap == null) {
                    synchronized (this) {
                        if (this.listenerMap == null) {
                            this.listenerMap = new ReferenceMap(ReferenceMap.WEAK, ReferenceMap.HARD, 2, 0.75f);
                        }
                    }
                }
                if (Modifier.isStatic(method.getModifiers())) {
                    this.listenerMap.put(theClass, Boolean.TRUE);
                } else {
                    this.listenerMap.put(obj, Boolean.TRUE);
                }
            } catch (NoSuchMethodException ex) {
                log.warn("The class [" + theClass + "] isn't InitializedListener.");
            } catch (Exception ex) {
                log.error("The class [" + theClass + "] isn't InitializedListener.", ex);
            }
        }

        /**
       * ��ʼ����ɺ�, ֪ͨ���еļ�����.
       */
        protected void callAfterEternaInitialize(Object obj) throws ConfigurationException {
            if (obj == null) {
                return;
            }
            Class theClass;
            Object[] objs;
            if (obj instanceof Class) {
                theClass = (Class) obj;
                objs = null;
            } else if (obj instanceof Collection) {
                Iterator itr = ((Collection) obj).iterator();
                while (itr.hasNext()) {
                    this.callAfterEternaInitialize(itr.next());
                }
                return;
            } else {
                theClass = obj.getClass();
                if (theClass.isArray()) {
                    objs = (Object[]) obj;
                    theClass = theClass.getComponentType();
                } else {
                    objs = new Object[] { obj };
                }
            }
            if (!EternaInitialize.class.isAssignableFrom(theClass)) {
                return;
            }
            try {
                Method method = theClass.getDeclaredMethod("afterEternaInitialize", new Class[] { Instance.class });
                boolean aFlag = method.isAccessible();
                if (!aFlag) {
                    method.setAccessible(true);
                }
                Object[] params = new Object[] { this };
                if (Modifier.isStatic(method.getModifiers())) {
                    method.invoke(null, params);
                } else if (objs != null) {
                    for (int i = 0; i < objs.length; i++) {
                        Object baseObj = objs[i];
                        if (baseObj != null) {
                            method.invoke(baseObj, params);
                        }
                    }
                }
                if (!aFlag) {
                    method.setAccessible(false);
                }
            } catch (NoSuchMethodException ex) {
                log.warn("Not found method initializeElse, when invoke init:" + theClass + ".");
            } catch (Exception ex) {
                if (ex instanceof ConfigurationException) {
                    throw (ConfigurationException) ex;
                }
                log.error("At initializeElse, when invoke init:" + theClass + ".", ex);
            }
        }

        /**
       * ����һ����ʼ���õ�xml��������.
       */
        protected Digester createDigester() {
            return FactoryManager.createDigester();
        }

        /**
       * ��ʼ��ָ���Ĺ���.
       *
       * @param factory   ���ʼ���Ĺ���
       */
        protected void initFactory(Factory factory) throws ConfigurationException {
            Factory shareFactory = null;
            if (this.shareInstance != null) {
                try {
                    String fName = factory.getName();
                    String cName = factory.getClass().getName();
                    shareFactory = this.shareInstance.getFactory(fName, cName);
                } catch (Exception ex) {
                }
            }
            factory.initialize(this, shareFactory);
        }

        /**
       * ���һ������map.
       *
       * @param name  ����������
       * @return  ����map
       */
        protected Map getFactoryMap(String name, boolean mustExists) throws ConfigurationException {
            Map map = (Map) this.instanceMaps.get(name);
            if (map == null && mustExists) {
                throw new ConfigurationException("Not found the factory name:" + name + ".");
            }
            return map;
        }

        /**
       * ��ʼ�����еĹ���.
       */
        protected void initializeFactorys() throws ConfigurationException {
            this.initFactorys = true;
            try {
                Iterator itr1 = this.instanceMaps.values().iterator();
                while (itr1.hasNext()) {
                    Map temp = (Map) itr1.next();
                    Iterator itr2 = temp.values().iterator();
                    while (itr2.hasNext()) {
                        this.initFactory((Factory) itr2.next());
                    }
                }
            } finally {
                this.initFactorys = false;
            }
        }

        /**
       * ���һ������ʵ��.
       *
       * @param name       ����������
       * @param className  ����ʵ������
       * @return   ����ʵ��
       */
        public Factory getFactory(String name, String className) throws ConfigurationException {
            Map map = this.getFactoryMap(name, !this.initialized);
            if (map == null && this.shareInstance != null) {
                return this.shareInstance.getFactory(name, className);
            }
            Factory factory = (Factory) map.get(className);
            if (this.initFactorys) {
                this.initFactory(factory);
            }
            if (!this.initialized) {
                FactoryManager.currentFactory = factory;
            }
            return factory;
        }

        /**
       * ���һ������ʵ��.
       *
       * @param name        ����������
       * @param factory     ����ʵ��
       */
        public void addFactory(String name, Factory factory) throws ConfigurationException {
            factory.setName(name);
            if (this.initialized) {
                this.initFactory(factory);
            } else {
                FactoryManager.currentFactory = factory;
            }
            Map map = (Map) this.instanceMaps.get(name);
            if (map == null) {
                map = new HashMap();
                this.instanceMaps.put(name, map);
            }
            map.put(factory.getClass().getName(), factory);
        }

        /**
       * ��÷�����Ϊ"eterna"�Ĺ���ʵ��.
       */
        public EternaFactory getEternaFactory() throws ConfigurationException {
            if (this.defaultFactory == null) {
                this.defaultFactory = this.getFactory(ETERNA_FACTORY, EternaFactoryImpl.class.getName());
            }
            return (EternaFactory) this.defaultFactory;
        }

        /**
       * ���˹���ʵ����������ڽ���ʱ, ����ô˷���.
       */
        public void destroy() {
            Iterator itr1 = this.instanceMaps.values().iterator();
            while (itr1.hasNext()) {
                Map temp = (Map) itr1.next();
                Iterator itr2 = temp.values().iterator();
                while (itr2.hasNext()) {
                    ((Factory) itr2.next()).destroy();
                }
            }
        }
    }

    /**
    * ȫ��FactoryManager��ʵ���ʵ����.
    */
    private static class GlobalImpl extends AbstractInstance implements Instance {

        public String getId() {
            return GLOBAL_INSTANCE_ID;
        }

        public String getInitConfig() {
            String initFiles = Utility.getProperty(INIT_FILES_PROPERTY);
            String subFiles = Utility.getProperty(INIT_SUBFILES_PROPERTY);
            String[] parentConfig = null;
            if (subFiles != null) {
                if (initFiles != null) {
                    parentConfig = new String[] { initFiles };
                }
                initFiles = subFiles;
            }
            return getConfig(initFiles, parentConfig);
        }

        public void setShareInstance(Instance shareInstance) {
        }

        protected void initializeXML(StringRef msg) throws Throwable {
            Digester digester = this.createDigester();
            try {
                String temp = Utility.getProperty(INIT_SUBFILES_PROPERTY);
                if (temp != null) {
                    this.dealXML(temp, null, digester);
                    FactoryManager.superInitLevel = 1;
                }
                String filenames = Utility.getProperty(INIT_FILES_PROPERTY);
                if (filenames == null) {
                    log.warn("The property " + INIT_FILES_PROPERTY + " not found.");
                } else {
                    this.dealXML(filenames, null, digester);
                    FactoryManager.superInitLevel += 1;
                }
                temp = Utility.getProperty(LOAD_DEFAULT_CONFIG);
                if (temp == null || "true".equalsIgnoreCase(temp)) {
                    temp = DEFAULT_CONFIG_FILE;
                    this.dealXML(temp, null, digester);
                }
            } finally {
                FactoryManager.superInitLevel = 0;
            }
        }

        protected void initializeElse() throws ConfigurationException {
            Class[] initClasses;
            String classNames = Utility.getProperty(INIT_CLASSES_PROPERTY);
            if (classNames == null) {
                initClasses = new Class[0];
            } else {
                StringTokenizer token = new StringTokenizer(classNames, ";");
                initClasses = new Class[token.countTokens()];
                String temp;
                int index = 0;
                while (token.hasMoreTokens()) {
                    temp = token.nextToken().trim();
                    if (temp.length() == 0) {
                        continue;
                    }
                    try {
                        initClasses[index] = Class.forName(temp);
                    } catch (Exception ex) {
                        log.warn("At initializeElse, when loadClass:" + temp + ".", ex);
                        initClasses[index] = null;
                    }
                    index++;
                }
            }
            for (int i = 0; i < initClasses.length; i++) {
                if (initClasses[i] == null) {
                    continue;
                }
                this.addInitializedListener(initClasses[i]);
            }
            super.initializeElse();
        }
    }

    /**
    * �������FactoryManager��ʵ���ʵ����.
    */
    private static class ClassImpl extends AbstractInstance implements Instance {

        protected String instanceId = null;

        protected String initConfig;

        protected String[] parentConfig;

        protected Class baseClass;

        public ClassImpl(Class baseClass, Object baseObj, String initConfig, String[] parentConfig) {
            this.baseClass = baseClass;
            this.initConfig = initConfig;
            this.parentConfig = parentConfig;
            if (baseObj instanceof ContainObject) {
                ContainObject co = (ContainObject) baseObj;
                this.setShareInstance(co.shareInstance);
                this.addInitializedListener(co.baseObj);
                this.prefixName = co.name;
            } else {
                this.setShareInstance(null);
                this.addInitializedListener(baseObj);
            }
        }

        public String getId() {
            if (this.instanceId == null) {
                String conf = getConfig(this.initConfig, this.parentConfig);
                String baseName = this.baseClass.getName();
                this.instanceId = this.createInstanceId(conf, baseName);
            }
            return this.instanceId;
        }

        public String getInitConfig() {
            String tmp = getConfig(this.initConfig, this.parentConfig);
            if (tmp == null) {
                tmp = "cp:" + this.baseClass.getName().replace('.', '/') + ".xml";
            }
            return tmp;
        }

        protected void initializeXML(StringRef msg) throws Throwable {
            ClassLoader oldCL = Thread.currentThread().getContextClassLoader();
            Thread.currentThread().setContextClassLoader(this.baseClass.getClassLoader());
            try {
                Digester digester = this.createDigester();
                String filenames = this.initConfig == null ? "cp:" + this.baseClass.getName().replace('.', '/') + ".xml" : this.initConfig;
                this.dealXML(filenames, this.baseClass, digester);
                if (this.parentConfig != null) {
                    for (int i = 0; i < this.parentConfig.length; i++) {
                        if (this.parentConfig[i] != null) {
                            FactoryManager.superInitLevel = i + 1;
                            try {
                                this.dealXML(this.parentConfig[i], this.baseClass, digester);
                            } finally {
                                FactoryManager.superInitLevel = 0;
                            }
                        }
                    }
                }
            } finally {
                Thread.currentThread().setContextClassLoader(oldCL);
            }
        }
    }

    /**
    * �������FactoryManager��ʵ���ʵ����, ͬʱ���������Ƿ��и���,
    * �����¹���Զ����³�ʼ��.
    */
    private static class AutoReloadImpl extends ClassImpl implements Instance {

        private long preInitTime;

        private long preCheckTime;

        private long autoReloadTime;

        private ConfigMonitor[] monitors = null;

        private boolean atInitialize = false;

        public AutoReloadImpl(Class baseClass, Object baseObj, String initConfig, String[] parentConfig, long autoReloadTime) {
            super(baseClass, baseObj, initConfig, parentConfig);
            List tempList = new LinkedList(this.getFiles(initConfig));
            if (parentConfig != null) {
                for (int i = 0; i < parentConfig.length; i++) {
                    if (parentConfig[i] != null) {
                        tempList.addAll(this.getFiles(parentConfig[i]));
                    }
                }
            }
            if (tempList.size() > 0) {
                this.monitors = new ConfigMonitor[tempList.size()];
                tempList.toArray(this.monitors);
            }
            this.autoReloadTime = autoReloadTime < 200 ? 200 : autoReloadTime;
        }

        private ConfigMonitor parseFileName(String fileName, URL url) {
            File file = new File(fileName);
            if (file.isFile()) {
                return new ConfigMonitor(file);
            }
            if (url != null) {
                return new ConfigMonitor(url);
            }
            return null;
        }

        private List getFiles(String config) {
            ConfigMonitor temp;
            List result = new ArrayList();
            if (config == null) {
                URL url = this.baseClass.getClassLoader().getResource(this.baseClass.getName().replace('.', '/') + ".xml");
                if (url != null && "file".equals(url.getProtocol())) {
                    temp = this.parseFileName(url.getFile(), url);
                    if (temp != null) {
                        result.add(temp);
                    }
                }
            } else {
                StringTokenizer token = new StringTokenizer(resolveLocate(config), ";");
                while (token.hasMoreTokens()) {
                    String tStr = token.nextToken().trim();
                    if (tStr.length() == 0) {
                        continue;
                    }
                    if (tStr.startsWith("cp:")) {
                        URL url = this.baseClass.getClassLoader().getResource(tStr.substring(3));
                        if (url != null && "file".equals(url.getProtocol())) {
                            temp = this.parseFileName(url.getFile(), url);
                            if (temp != null) {
                                result.add(temp);
                            }
                        }
                    } else if (tStr.startsWith("http:")) {
                        try {
                            result.add(new URL(tStr));
                        } catch (IOException ex) {
                        }
                    } else {
                        temp = this.parseFileName(tStr, null);
                        if (temp != null) {
                            result.add(temp);
                        }
                    }
                }
            }
            return result;
        }

        public void reInit(StringRef msg) {
            this.atInitialize = true;
            super.reInit(msg);
            this.atInitialize = false;
        }

        protected void initializeElse() throws ConfigurationException {
            super.initializeElse();
            long time = System.currentTimeMillis();
            if (this.preInitTime < time) {
                this.preInitTime = time;
                this.preCheckTime = this.preInitTime;
            }
        }

        private void checkReload() {
            if (this.atInitialize) {
                synchronized (this) {
                    if (this.atInitialize) {
                        return;
                    }
                }
            }
            if (System.currentTimeMillis() - this.autoReloadTime > this.preCheckTime && this.monitors != null) {
                boolean needReload = false;
                for (int i = 0; i < this.monitors.length; i++) {
                    long lm = this.monitors[i].getLastModified();
                    if (lm > this.preInitTime) {
                        needReload = true;
                        this.preInitTime = lm;
                        break;
                    }
                }
                if (needReload) {
                    synchronized (this) {
                        if (System.currentTimeMillis() - this.autoReloadTime > this.preCheckTime) {
                            StringRef sr = new StringRef();
                            this.reInit(sr);
                            if (log.isInfoEnabled()) {
                                log.info("Auto reload at time:" + FormatTool.getCurrentDatetimeString() + ". with message:");
                                log.info(sr.toString());
                            }
                        }
                    }
                }
                this.preCheckTime = System.currentTimeMillis();
            }
        }

        public Factory getFactory(String name, String className) throws ConfigurationException {
            this.checkReload();
            return super.getFactory(name, className);
        }

        public EternaFactory getEternaFactory() throws ConfigurationException {
            this.checkReload();
            return super.getEternaFactory();
        }
    }

    /**
    * ���ø��µļ����.
    */
    private static class ConfigMonitor {

        private File configFile = null;

        private URL configURL = null;

        private boolean valid = true;

        public ConfigMonitor(File configFile) {
            this.configFile = configFile;
        }

        public ConfigMonitor(URL configURL) {
            this.configURL = configURL;
        }

        public boolean isValid() {
            return this.valid;
        }

        public long getLastModified() {
            if (this.valid) {
                try {
                    if (this.configFile == null) {
                        return this.configURL.openConnection().getLastModified();
                    } else {
                        return this.configFile.lastModified();
                    }
                } catch (Throwable ex) {
                    StringBuffer buf = new StringBuffer(128);
                    buf.append("Error in check configFile:[").append(this.configFile).append("], configURL:[").append(this.configURL).append("].");
                    log.error(buf, ex);
                    this.configFile = null;
                    this.configURL = null;
                    this.valid = false;
                }
            }
            return 0L;
        }
    }
}
