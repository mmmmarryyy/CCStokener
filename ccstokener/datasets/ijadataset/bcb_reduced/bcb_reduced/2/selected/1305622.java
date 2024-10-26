package com.sns2Life.utils;

import java.io.IOException;
import java.io.InputStream;
import java.net.URL;
import java.util.Enumeration;
import java.util.HashSet;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;
import java.util.Set;
import com.sns2Life.utils.exception.ServiceNotFoundException;
import com.sns2Life.utils.exception.ClassInstantiationException;

/**
 * <p>
 * ���Ҳ�װ�������Դ�ĸ����ࡣ
 * </p>
 *
 * <p>
 * <code>ClassLoaderUtil</code>���������Դ��Ч��
 * �൱��<code>ClassLoader.loadClass</code>������<code>ClassLoader.getResource</code>������
 * ��<code>ClassLoaderUtil</code>�������ȳ��Դ�<code>Thread.getContextClassLoader()</code>����ȡ��<code>ClassLoader</code>�в�װ�������Դ��
 * ���ַ����������ڶ༶<code>ClassLoader</code>������£��Ҳ��������Դ�������
 * </p>
 *
 * <p>
 * �������������:
 * </p>
 *
 * <ul>
 * <li>
 * ������<code>A</code>�Ǵ�ϵͳ<code>ClassLoader</code>װ���(classpath)
 * </li>
 * <li>
 * ��<code>B</code>��Web Application�е�һ���࣬����servlet�����<code>ClassLoader</code>��̬װ���
 * </li>
 * <li>
 * ��Դ�ļ�<code>C.properties</code>Ҳ��Web Application�У�ֻ��servlet����Ķ�̬<code>ClassLoader</code>�����ҵ���
 * </li>
 * <li>
 * ��<code>B</code>���ù�����<code>A</code>�ķ�����ϣ��ͨ����<code>A</code>ȡ����Դ�ļ�<code>C.properties</code>
 * </li>
 * </ul>
 *
 * <p>
 * �����<code>A</code>ʹ��<code>getClass().getClassLoader().getResource(&quot;C.properties&quot;)</code>��
 * �ͻ�ʧ�ܣ���Ϊϵͳ<code>ClassLoader</code>�����ҵ�����Դ��
 * ����A����ʹ��ClassLoaderUtil.getResource(&quot;C.properties&quot;)���Ϳ����ҵ������Դ��
 * ��ΪClassLoaderUtil����<code>Thread.currentThead().getContextClassLoader()</code>ȡ����servlet�����<code>ClassLoader</code>��
 * �Ӷ��ҵ��������Դ�ļ���
 * </p>
 *
 * <p>
 * ע�⣬<code>Thread.getContextClassLoader()</code>����JDK1.2֮����еģ����ڵͰ汾��JDK��
 * <code>ClassLoaderUtil</code>��Ч���ֱ�ӵ���<code>ClassLoader</code>��ȫ��ͬ��
 * </p>
 *
 * @author Michael Zhou
 * @version $Id: ClassLoaderUtil.java 1196 2004-11-24 01:03:13Z baobao $
 */
public class ClassLoaderUtil {

    /**
     * ȡ�õ�ǰ�̵߳�<code>ClassLoader</code>�����������ҪJDK1.2���߰汾��JDK��֧�֡�
     *
     * @return ��ǰ�̵߳�<code>ClassLoader</code>
     */
    public static ClassLoader getContextClassLoader() {
        return Thread.currentThread().getContextClassLoader();
    }

    /**
     * �ӵ�ǰ�̵߳�<code>ClassLoader</code>װ���ࡣ����JDK1.2���£����൱��<code>Class.forName</code>��
     *
     * @param className Ҫװ�������
     *
     * @return ��װ�����
     *
     * @throws ClassNotFoundException �����û�ҵ�
     */
    public static Class loadClass(String className) throws ClassNotFoundException {
        return loadClass(className, getContextClassLoader());
    }

    /**
     * ��ָ���ĵ����ߵ�<code>ClassLoader</code>װ���ࡣ
     *
     * @param className Ҫװ�������
     * @param referrer �������࣬���Ϊ<code>null</code>����÷����൱��<code>Class.forName</code>
     *
     * @return ��װ�����
     *
     * @throws ClassNotFoundException �����û�ҵ�
     */
    public static Class loadClass(String className, Class referrer) throws ClassNotFoundException {
        ClassLoader classLoader = getReferrerClassLoader(referrer);
        return loadClass(className, classLoader);
    }

    /**
     * ��ָ����<code>ClassLoader</code>��װ���ࡣ���δָ��<code>ClassLoader</code>��
     * ���װ��<code>ClassLoaderUtil</code>��<code>ClassLoader</code>��װ�롣
     *
     * @param className Ҫװ�������
     * @param classLoader
     *        ��ָ����<code>ClassLoader</code>��װ���࣬���Ϊ<code>null</code>����ʾ��<code>ClassLoaderUtil</code>���ڵ�class
     *        loader��װ��
     *
     * @return ��װ�����
     *
     * @throws ClassNotFoundException �����û�ҵ�
     */
    public static Class loadClass(String className, ClassLoader classLoader) throws ClassNotFoundException {
        if (className == null) {
            return null;
        }
        if (classLoader == null) {
            return Class.forName(className);
        } else {
            return Class.forName(className, true, classLoader);
        }
    }

    /**
     * ���ձ�׼��jar service�淶������װ��ָ����Ƶ�service�ࡣ
     *
     * <p>
     * ������������class loader�в���<code>META-INF/services/serviceId</code>�ļ���������������Ϊservice����
     * </p>
     *
     * @param serviceId ������
     *
     * @return service class
     *
     * @throws ClassNotFoundException ���service class�Ҳ�������װ��service�ļ�ʧ��
     */
    public static Class loadServiceClass(String serviceId) throws ClassNotFoundException {
        return loadServiceClass(serviceId, getContextClassLoader());
    }

    /**
     * ���ձ�׼��jar service�淶������װ��ָ����Ƶ�service�ࡣ
     *
     * <p>
     * ������������class loader�в���<code>META-INF/services/serviceId</code>�ļ���������������Ϊservice����
     * </p>
     *
     * @param serviceId ������
     * @param referrer �������࣬���Ϊ<code>null</code>����ʾ��<code>ClassLoaderUtil</code>��class loader���ҡ�
     *
     * @return service class
     *
     * @throws ClassNotFoundException ���service class�Ҳ�������װ��service�ļ�ʧ��
     */
    public static Class loadServiceClass(String serviceId, Class referrer) throws ClassNotFoundException {
        ClassLoader classLoader = getReferrerClassLoader(referrer);
        return loadServiceClass(serviceId, classLoader);
    }

    /**
     * ���ձ�׼��jar service�淶������װ��ָ����Ƶ�service�ࡣ
     *
     * <p>
     * ������������class loader�в���<code>META-INF/services/serviceId</code>�ļ���������������Ϊservice����
     * </p>
     *
     * @param serviceId ������
     * @param classLoader
     *        ��ָ��classLoader�в��ң����Ϊ<code>null</code>����ʾ��<code>ClassLoaderUtil</code>��class
     *        loader���ҡ�
     *
     * @return service class
     *
     * @throws ClassNotFoundException ���service class�Ҳ�������װ��service�ļ�ʧ��
     */
    public static Class loadServiceClass(String serviceId, ClassLoader classLoader) throws ClassNotFoundException {
        if (serviceId == null) {
            return null;
        }
        serviceId = "META-INF/services/" + serviceId;
        InputStream istream = getResourceAsStream(serviceId, classLoader);
        if (istream == null) {
            throw new ServiceNotFoundException("Could not find " + serviceId);
        }
        String serviceClassName;
        try {
            serviceClassName = StringUtil.trimToEmpty(StreamUtil.readText(istream, "UTF-8"));
        } catch (IOException e) {
            throw new ServiceNotFoundException("Failed to load " + serviceId, e);
        }
        return ClassLoaderUtil.loadClass(serviceClassName, classLoader);
    }

    /**
     * ������ͼװ��ָ����Ƶ��࣬����Ҳ������ٰ��ձ�׼��jar service�淶������װ��ָ����Ƶ�service�ࡣ
     *
     * <p>
     * ����service�ķ�������class loader�в���<code>META-INF/services/serviceId</code>�ļ���������������Ϊservice����
     * </p>
     *
     * @param className Ҫװ�������
     * @param serviceId ������
     *
     * @return service class
     *
     * @throws ClassNotFoundException ���service class�Ҳ�������װ��service�ļ�ʧ��
     */
    public static Class loadServiceClass(String className, String serviceId) throws ClassNotFoundException {
        return loadServiceClass(className, serviceId, getContextClassLoader());
    }

    /**
     * ������ͼװ��ָ����Ƶ��࣬����Ҳ������ٰ��ձ�׼��jar service�淶������װ��ָ����Ƶ�service�ࡣ
     *
     * <p>
     * ����service�ķ�������class loader�в���<code>META-INF/services/serviceId</code>�ļ���������������Ϊservice����
     * </p>
     *
     * @param className Ҫװ�������
     * @param serviceId ������
     * @param referrer �������࣬���Ϊ<code>null</code>����ʾ��<code>ClassLoaderUtil</code>��class loader���ҡ�
     *
     * @return service class
     *
     * @throws ClassNotFoundException ���service class�Ҳ�������װ��service�ļ�ʧ��
     */
    public static Class loadServiceClass(String className, String serviceId, Class referrer) throws ClassNotFoundException {
        ClassLoader classLoader = getReferrerClassLoader(referrer);
        return loadServiceClass(className, serviceId, classLoader);
    }

    /**
     * ������ͼװ��ָ����Ƶ��࣬����Ҳ������ٰ��ձ�׼��jar service�淶������װ��ָ����Ƶ�service�ࡣ
     *
     * <p>
     * ����service�ķ�������class loader�в���<code>META-INF/services/serviceId</code>�ļ���������������Ϊservice����
     * </p>
     *
     * @param className Ҫװ�������
     * @param serviceId ������
     * @param classLoader
     *        ��ָ��classLoader�в��ң����Ϊ<code>null</code>����ʾ��<code>ClassLoaderUtil</code>��class
     *        loader���ҡ�
     *
     * @return service class
     *
     * @throws ClassNotFoundException ���service class�Ҳ�������װ��service�ļ�ʧ��
     */
    public static Class loadServiceClass(String className, String serviceId, ClassLoader classLoader) throws ClassNotFoundException {
        try {
            if (className != null) {
                return loadClass(className, classLoader);
            }
        } catch (ClassNotFoundException e) {
        }
        return loadServiceClass(serviceId, classLoader);
    }

    /**
     * ȡ�õ����ߵ�class loader��
     *
     * @param referrer ��������
     *
     * @return �����ߵ�class loader�����referrerΪ<code>null</code>���򷵻�<code>null</code>
     */
    private static ClassLoader getReferrerClassLoader(Class referrer) {
        ClassLoader classLoader = null;
        if (referrer != null) {
            classLoader = referrer.getClassLoader();
            if (classLoader == null) {
                classLoader = ClassLoader.getSystemClassLoader();
            }
        }
        return classLoader;
    }

    /**
     * �ӵ�ǰ�̵߳�<code>ClassLoader</code>װ���ಢʵ��֮��
     *
     * @param className Ҫʵ�������
     *
     * @return ָ�������ʵ��
     *
     * @throws ClassNotFoundException �����û�ҵ�
     * @throws ClassInstantiationException ���ʵ��ʧ��
     */
    public static Object newInstance(String className) throws ClassNotFoundException, ClassInstantiationException {
        return newInstance(loadClass(className));
    }

    /**
     * ��ָ���ĵ����ߵ�<code>ClassLoader</code>װ���ಢʵ��֮��
     *
     * @param className Ҫʵ�������
     * @param referrer �������࣬���Ϊ<code>null</code>�����<code>ClassLoaderUtil</code>���ڵ�class loaderװ��
     *
     * @return ָ�������ʵ��
     *
     * @throws ClassNotFoundException �����û�ҵ�
     * @throws ClassInstantiationException ���ʵ��ʧ��
     */
    public static Object newInstance(String className, Class referrer) throws ClassNotFoundException, ClassInstantiationException {
        return newInstance(loadClass(className, referrer));
    }

    /**
     * ��ָ����<code>ClassLoader</code>��װ���ಢʵ��֮�����δָ��<code>ClassLoader</code>��
     * ���װ��<code>ClassLoaderUtil</code>��<code>ClassLoader</code>��װ�롣
     *
     * @param className Ҫʵ�������
     * @param classLoader
     *        ��ָ����<code>ClassLoader</code>��װ���࣬���Ϊ<code>null</code>����ʾ��<code>ClassLoaderUtil</code>���ڵ�class
     *        loader��װ��
     *
     * @return ָ�������ʵ��
     *
     * @throws ClassNotFoundException �����û�ҵ�
     * @throws ClassInstantiationException ���ʵ��ʧ��
     */
    public static Object newInstance(String className, ClassLoader classLoader) throws ClassNotFoundException, ClassInstantiationException {
        return newInstance(loadClass(className, classLoader));
    }

    /**
     * ����ָ�����ʵ��
     *
     * @param clazz Ҫ����ʵ�����
     *
     * @return ָ�����ʵ��
     *
     * @throws ClassInstantiationException ���ʵ��ʧ��
     */
    private static Object newInstance(Class clazz) throws ClassInstantiationException {
        if (clazz == null) {
            return null;
        }
        try {
            return clazz.newInstance();
        } catch (InstantiationException e) {
            throw new ClassInstantiationException("Failed to instantiate class: " + clazz.getName(), e);
        } catch (IllegalAccessException e) {
            throw new ClassInstantiationException("Failed to instantiate class: " + clazz.getName(), e);
        } catch (Exception e) {
            throw new ClassInstantiationException("Failed to instantiate class: " + clazz.getName(), e);
        }
    }

    /**
     * ���ձ�׼��jar service�淶������װ��ָ����Ƶ�service�ಢʵ��֮��
     *
     * <p>
     * ����service�ķ�������class loader�в���<code>META-INF/services/serviceId</code>�ļ���������������Ϊservice����
     * </p>
     *
     * @param serviceId ������
     *
     * @return ʵ����
     *
     * @throws ClassNotFoundException ���service class�Ҳ�������װ��service�ļ�ʧ��
     * @throws ClassInstantiationException ���ʵ��ʧ��
     */
    public static Object newServiceInstance(String serviceId) throws ClassNotFoundException, ClassInstantiationException {
        return newInstance(loadServiceClass(serviceId));
    }

    /**
     * ���ձ�׼��jar service�淶������װ��ָ����Ƶ�service�ಢʵ��֮��
     *
     * <p>
     * ����service�ķ�������class loader�в���<code>META-INF/services/serviceId</code>�ļ���������������Ϊservice����
     * </p>
     *
     * @param serviceId ������
     * @param referrer �������࣬���Ϊ<code>null</code>����ʾ��<code>ClassLoaderUtil</code>��class loader���ҡ�
     *
     * @return ʵ����
     *
     * @throws ClassNotFoundException ���service class�Ҳ�������װ��service�ļ�ʧ��
     * @throws ClassInstantiationException ���ʵ��ʧ��
     */
    public static Object newServiceInstance(String serviceId, Class referrer) throws ClassNotFoundException, ClassInstantiationException {
        return newInstance(loadServiceClass(serviceId, referrer));
    }

    /**
     * ���ձ�׼��jar service�淶������װ��ָ����Ƶ�service�ಢʵ��֮��
     *
     * <p>
     * ����service�ķ�������class loader�в���<code>META-INF/services/serviceId</code>�ļ���������������Ϊservice����
     * </p>
     *
     * @param serviceId ������
     * @param classLoader
     *        ��ָ��classLoader�в��ң����Ϊ<code>null</code>����ʾ��<code>ClassLoaderUtil</code>��class
     *        loader���ҡ�
     *
     * @return ʵ����
     *
     * @throws ClassNotFoundException ���service class�Ҳ�������װ��service�ļ�ʧ��
     * @throws ClassInstantiationException ���ʵ��ʧ��
     */
    public static Object newServiceInstance(String serviceId, ClassLoader classLoader) throws ClassNotFoundException, ClassInstantiationException {
        return newInstance(loadServiceClass(serviceId, classLoader));
    }

    /**
     * ������ͼװ��ָ����Ƶ��࣬����Ҳ������ٰ��ձ�׼��jar service�淶������װ��ָ����Ƶ�service�ࡣ �ҵ��Ժ�ʵ��֮��
     *
     * <p>
     * ����service�ķ�������class loader�в���<code>META-INF/services/serviceId</code>�ļ���������������Ϊservice����
     * </p>
     *
     * @param className Ҫװ�������
     * @param serviceId ������
     *
     * @return ʵ����
     *
     * @throws ClassNotFoundException ���service class�Ҳ�������װ��service�ļ�ʧ��
     * @throws ClassInstantiationException ���ʵ��ʧ��
     */
    public static Object newServiceInstance(String className, String serviceId) throws ClassNotFoundException, ClassInstantiationException {
        return newInstance(loadServiceClass(className, serviceId));
    }

    /**
     * ������ͼװ��ָ����Ƶ��࣬����Ҳ������ٰ��ձ�׼��jar service�淶������װ��ָ����Ƶ�service�ࡣ �ҵ��Ժ�ʵ��֮��
     *
     * <p>
     * ����service�ķ�������class loader�в���<code>META-INF/services/serviceId</code>�ļ���������������Ϊservice����
     * </p>
     *
     * @param className Ҫװ�������
     * @param serviceId ������
     * @param referrer �������࣬���Ϊ<code>null</code>����ʾ��<code>ClassLoaderUtil</code>��class loader���ҡ�
     *
     * @return ʵ����
     *
     * @throws ClassNotFoundException ���service class�Ҳ�������װ��service�ļ�ʧ��
     * @throws ClassInstantiationException ���ʵ��ʧ��
     */
    public static Object newServiceInstance(String className, String serviceId, Class referrer) throws ClassNotFoundException, ClassInstantiationException {
        return newInstance(loadServiceClass(className, serviceId, referrer));
    }

    /**
     * ������ͼװ��ָ����Ƶ��࣬����Ҳ������ٰ��ձ�׼��jar service�淶������װ��ָ����Ƶ�service�ࡣ �ҵ��Ժ�ʵ��֮��
     *
     * <p>
     * ����service�ķ�������class loader�в���<code>META-INF/services/serviceId</code>�ļ���������������Ϊservice����
     * </p>
     *
     * @param className Ҫװ�������
     * @param serviceId ������
     * @param classLoader
     *        ��ָ��classLoader�в��ң����Ϊ<code>null</code>����ʾ��<code>ClassLoaderUtil</code>��class
     *        loader���ҡ�
     *
     * @return ʵ����
     *
     * @throws ClassNotFoundException ���service class�Ҳ�������װ��service�ļ�ʧ��
     * @throws ClassInstantiationException ���ʵ��ʧ��
     */
    public static Object newServiceInstance(String className, String serviceId, ClassLoader classLoader) throws ClassNotFoundException, ClassInstantiationException {
        return newInstance(loadServiceClass(className, serviceId, classLoader));
    }

    /**
     * ��<code>ClassLoader</code>ȡ������resource URL��������˳�����:
     *
     * <ol>
     * <li>
     * �ڵ�ǰ�̵߳�<code>ClassLoader</code>�в��ҡ�
     * </li>
     * <li>
     * ��װ���Լ���<code>ClassLoader</code>�в��ҡ�
     * </li>
     * <li>
     * ͨ��<code>ClassLoader.getSystemResource</code>�������ҡ�
     * </li>
     * </ol>
     *
     *
     * @param resourceName Ҫ���ҵ���Դ�������&quot;/&quot;�ָ��ı�ʶ���ַ�
     *
     * @return resource��URL���飬���û�ҵ����򷵻ؿ����顣�����б�֤�����ظ���URL��
     */
    public static URL[] getResources(String resourceName) {
        LinkedList urls = new LinkedList();
        boolean found = false;
        found = getResources(urls, resourceName, getContextClassLoader(), false);
        if (!found) {
            getResources(urls, resourceName, ClassLoaderUtil.class.getClassLoader(), false);
        }
        if (!found) {
            getResources(urls, resourceName, null, true);
        }
        return getDistinctURLs(urls);
    }

    /**
     * ��ָ��������������<code>ClassLoader</code>ȡ������resource URL��
     *
     * @param resourceName Ҫ���ҵ���Դ�������&quot;/&quot;�ָ��ı�ʶ���ַ�
     * @param referrer �������࣬���Ϊ<code>null</code>����ʾ��<code>ClassLoaderUtil</code>��class loader����
     *
     * @return resource��URL���飬���û�ҵ����򷵻ؿ����顣�����б�֤�����ظ���URL��
     */
    public static URL[] getResources(String resourceName, Class referrer) {
        ClassLoader classLoader = getReferrerClassLoader(referrer);
        LinkedList urls = new LinkedList();
        getResources(urls, resourceName, classLoader, classLoader == null);
        return getDistinctURLs(urls);
    }

    /**
     * ��ָ����<code>ClassLoader</code>��ȡ������resource URL�����δָ��<code>ClassLoader</code>��
     * ���װ��<code>ClassLoaderUtil</code>��<code>ClassLoader</code>��ȡ������resource URL��
     *
     * @param resourceName Ҫ���ҵ���Դ�������&quot;/&quot;�ָ��ı�ʶ���ַ�
     * @param classLoader ��ָ����<code>ClassLoader</code>�в���
     *
     * @return resource��URL���飬���û�ҵ����򷵻ؿ����顣�����б�֤�����ظ���URL��
     */
    public static URL[] getResources(String resourceName, ClassLoader classLoader) {
        LinkedList urls = new LinkedList();
        getResources(urls, resourceName, classLoader, classLoader == null);
        return getDistinctURLs(urls);
    }

    /**
     * ��ָ��class loader�в���ָ����Ƶ�resource���������ҵ���resource��URL����ָ���ļ����С�
     *
     * @param urlSet ���resource URL�ļ���
     * @param resourceName ��Դ��
     * @param classLoader ��װ����
     * @param sysClassLoader �Ƿ���system class loaderװ����Դ
     *
     * @return ����ҵ����򷵻�<code>true</code>
     */
    private static boolean getResources(List urlSet, String resourceName, ClassLoader classLoader, boolean sysClassLoader) {
        if (resourceName == null) {
            return false;
        }
        Enumeration i = null;
        try {
            if (classLoader != null) {
                i = classLoader.getResources(resourceName);
            } else if (sysClassLoader) {
                i = ClassLoader.getSystemResources(resourceName);
            }
        } catch (IOException e) {
        }
        if ((i != null) && i.hasMoreElements()) {
            while (i.hasMoreElements()) {
                urlSet.add(i.nextElement());
            }
            return true;
        }
        return false;
    }

    /**
     * ȥ��URL�б��е��ظ��
     *
     * @param urls URL�б�
     *
     * @return ���ظ���URL���飬���urlsΪ<code>null</code>���򷵻ؿ�����
     */
    private static URL[] getDistinctURLs(LinkedList urls) {
        if ((urls == null) || (urls.size() == 0)) {
            return new URL[0];
        }
        Set urlSet = new HashSet(urls.size());
        for (Iterator i = urls.iterator(); i.hasNext(); ) {
            URL url = (URL) i.next();
            if (urlSet.contains(url)) {
                i.remove();
            } else {
                urlSet.add(url);
            }
        }
        return (URL[]) urls.toArray(new URL[urls.size()]);
    }

    /**
     * <p>
     * ��<code>ClassLoader</code>ȡ��resource URL��������˳�����:
     * </p>
     *
     * <ol>
     * <li>
     * �ڵ�ǰ�̵߳�<code>ClassLoader</code>�в��ҡ�
     * </li>
     * <li>
     * ��װ���Լ���<code>ClassLoader</code>�в��ҡ�
     * </li>
     * <li>
     * ͨ��<code>ClassLoader.getSystemResource</code>�������ҡ�
     * </li>
     * </ol>
     *
     *
     * @param resourceName Ҫ���ҵ���Դ�������&quot;/&quot;�ָ��ı�ʶ���ַ�
     *
     * @return resource��URL
     */
    public static URL getResource(String resourceName) {
        if (resourceName == null) {
            return null;
        }
        ClassLoader classLoader = null;
        URL url = null;
        classLoader = getContextClassLoader();
        if (classLoader != null) {
            url = classLoader.getResource(resourceName);
            if (url != null) {
                return url;
            }
        }
        classLoader = ClassLoaderUtil.class.getClassLoader();
        if (classLoader != null) {
            url = classLoader.getResource(resourceName);
            if (url != null) {
                return url;
            }
        }
        return ClassLoader.getSystemResource(resourceName);
    }

    /**
     * ��ָ��������������<code>ClassLoader</code>ȡ��resource URL��
     *
     * @param resourceName Ҫ���ҵ���Դ�������&quot;/&quot;�ָ��ı�ʶ���ַ�
     * @param referrer �������࣬���Ϊ<code>null</code>����ʾ��<code>ClassLoaderUtil</code>��class loader���ҡ�
     *
     * @return resource URL�����û�ҵ����򷵻�<code>null</code>
     */
    public static URL getResource(String resourceName, Class referrer) {
        if (resourceName == null) {
            return null;
        }
        ClassLoader classLoader = getReferrerClassLoader(referrer);
        return (classLoader == null) ? ClassLoaderUtil.class.getClassLoader().getResource(resourceName) : classLoader.getResource(resourceName);
    }

    /**
     * ��ָ����<code>ClassLoader</code>ȡ��resource URL��
     *
     * @param resourceName Ҫ���ҵ���Դ�������&quot;/&quot;�ָ��ı�ʶ���ַ�
     * @param classLoader
     *        ��ָ��classLoader�в��ң����Ϊ<code>null</code>����ʾ��<code>ClassLoaderUtil</code>��class
     *        loader���ҡ�
     *
     * @return resource URL�����û�ҵ����򷵻�<code>null</code>
     */
    public static URL getResource(String resourceName, ClassLoader classLoader) {
        if (resourceName == null) {
            return null;
        }
        return (classLoader == null) ? ClassLoaderUtil.class.getClassLoader().getResource(resourceName) : classLoader.getResource(resourceName);
    }

    /**
     * ��<code>ClassLoader</code>ȡ��resource����������
     * �൱��<code>getResource(resourceName).openStream()</code>��
     *
     * @param resourceName Ҫ���ҵ���Դ�������"/"�ָ��ı�ʶ���ַ�
     *
     * @return resource��������
     */
    public static InputStream getResourceAsStream(String resourceName) {
        URL url = getResource(resourceName);
        try {
            if (url != null) {
                return url.openStream();
            }
        } catch (IOException e) {
        }
        return null;
    }

    /**
     * ��<code>ClassLoader</code>ȡ��resource���������� �൱��<code>getResource(resourceName,
     * referrer).openStream()</code>��
     *
     * @param resourceName Ҫ���ҵ���Դ�������"/"�ָ��ı�ʶ���ַ�
     * @param referrer �������࣬���Ϊ<code>null</code>����ʾ��<code>ClassLoaderUtil</code>��class loader���ҡ�
     *
     * @return resource��������
     */
    public static InputStream getResourceAsStream(String resourceName, Class referrer) {
        URL url = getResource(resourceName, referrer);
        try {
            if (url != null) {
                return url.openStream();
            }
        } catch (IOException e) {
        }
        return null;
    }

    /**
     * ��<code>ClassLoader</code>ȡ��resource���������� �൱��<code>getResource(resourceName,
     * classLoader).openStream()</code>��
     *
     * @param resourceName Ҫ���ҵ���Դ�������"/"�ָ��ı�ʶ���ַ�
     * @param classLoader
     *        ��ָ��classLoader�в��ң����Ϊ<code>null</code>����ʾ��<code>ClassLoaderUtil</code>��class
     *        loader���ҡ�
     *
     * @return resource��������
     */
    public static InputStream getResourceAsStream(String resourceName, ClassLoader classLoader) {
        URL url = getResource(resourceName, classLoader);
        try {
            if (url != null) {
                return url.openStream();
            }
        } catch (IOException e) {
        }
        return null;
    }

    /**
     * �ӵ�ǰ�̵߳�<code>ClassLoader</code>�в���ָ����Ƶ��ࡣ
     *
     * @param className Ҫ���ҵ�����
     *
     * @return URL���飬�о���ϵͳ�����п��ҵ���ͬ���࣬���δ�ҵ����򷵻�һ��������
     */
    public static URL[] whichClasses(String className) {
        return getResources(ClassUtil.getClassNameAsResource(className));
    }

    /**
     * �ӵ�ǰ�̵߳�<code>ClassLoader</code>�в���ָ����Ƶ��ࡣ
     *
     * @param className Ҫ���ҵ�����
     * @param referrer �������࣬���Ϊ<code>null</code>����ʾ��<code>ClassLoaderUtil</code>��class loader���ҡ�
     *
     * @return URL���飬�о���ϵͳ�����п��ҵ���ͬ���࣬���δ�ҵ����򷵻�һ��������
     */
    public static URL[] whichClasses(String className, Class referrer) {
        return getResources(ClassUtil.getClassNameAsResource(className), referrer);
    }

    /**
     * �ӵ�ǰ�̵߳�<code>ClassLoader</code>�в���ָ����Ƶ��ࡣ
     *
     * @param className Ҫ���ҵ�����
     * @param classLoader
     *        ��ָ��classLoader�в��ң����Ϊ<code>null</code>����ʾ��<code>ClassLoaderUtil</code>��class
     *        loader���ҡ�
     *
     * @return URL���飬�о���ϵͳ�����п��ҵ���ͬ���࣬���δ�ҵ����򷵻�һ��������
     */
    public static URL[] whichClasses(String className, ClassLoader classLoader) {
        return getResources(ClassUtil.getClassNameAsResource(className), classLoader);
    }

    /**
     * �ӵ�ǰ�̵߳�<code>ClassLoader</code>�в���ָ����Ƶ��ࡣ
     *
     * @param className Ҫ���ҵ�����
     *
     * @return ���ļ���URL�����δ�ҵ����򷵻�<code>null</code>
     */
    public static URL whichClass(String className) {
        return getResource(ClassUtil.getClassNameAsResource(className));
    }

    /**
     * �ӵ�ǰ�̵߳�<code>ClassLoader</code>�в���ָ����Ƶ��ࡣ
     *
     * @param className Ҫ���ҵ�����
     * @param referrer �������࣬���Ϊ<code>null</code>����ʾ��<code>ClassLoaderUtil</code>��class loader���ҡ�
     *
     * @return ���ļ���URL�����δ�ҵ����򷵻�<code>null</code>
     */
    public static URL whichClass(String className, Class referrer) {
        return getResource(ClassUtil.getClassNameAsResource(className), referrer);
    }

    /**
     * �ӵ�ǰ�̵߳�<code>ClassLoader</code>�в���ָ����Ƶ��ࡣ
     *
     * @param className Ҫ���ҵ�����
     * @param classLoader
     *        ��ָ��classLoader�в��ң����Ϊ<code>null</code>����ʾ��<code>ClassLoaderUtil</code>��class
     *        loader���ҡ�
     *
     * @return ���ļ���URL�����δ�ҵ����򷵻�<code>null</code>
     */
    public static URL whichClass(String className, ClassLoader classLoader) {
        return getResource(ClassUtil.getClassNameAsResource(className), classLoader);
    }
}
