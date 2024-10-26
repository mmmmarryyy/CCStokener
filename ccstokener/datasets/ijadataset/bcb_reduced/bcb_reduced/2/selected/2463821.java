package com.bluebrim.extensibility.shared;

import java.io.*;
import java.lang.reflect.*;
import java.lang.reflect.Proxy;
import java.net.*;
import java.util.*;

/**
 * Helper class for using Service Provider Interfaces (SPI).
 * 
 * @author Markus Persson 2002-02-20
 */
public class CoServices {

    public interface ProviderVisitor {

        public void visit(Object provider);
    }

    private static final String SERVICES_PATH_PREFIX = "META-INF/services/";

    /**
	 * Returns an iterator containing one instance of each of the classes
	 * implementing the given SPI (and apropriately declared in a file in
	 * a META-INF/services directory on the classpath).
	 * 
	 * NOTE: The recommended naming convention is that the name of the service
	 * provider interface ends in "SPI" and that the name of a class implementing
	 * that interface (that is, a Service Provider) ends in "SP" or "Provider".
	 */
    public static Iterator getProviders(Class spi) {
        final Collection providers = new ArrayList();
        eachProvider(spi, new ProviderVisitor() {

            public void visit(Object provider) {
                providers.add(provider);
            }
        });
        return providers.iterator();
    }

    /**
	 * Exceptions resulting from problems with service config files.
	 * 
	 * PENDING: Make details available. Subclass?
	 */
    public static class ConfigException extends RuntimeException {

        public ConfigException(URL url, int lineNo) {
            super("Illegal configuration-file syntax at line " + lineNo + " in " + url);
        }

        public ConfigException(String className, URL url, int lineNo) {
            super("Illegal provider-class name: " + className + " at line " + lineNo + " in " + url);
        }

        public ConfigException(IOException ioe) {
            super("Unexpected I/O problem: " + ioe.getMessage(), ioe);
        }
    }

    public static class ExpandingIterator implements Iterator {

        private Enumeration m_urls;

        private Set m_seen = new HashSet();

        private List m_buf = new ArrayList();

        private Iterator m_names = m_buf.iterator();

        public ExpandingIterator(Enumeration urls) {
            m_urls = urls;
        }

        public boolean hasNext() {
            while (!m_names.hasNext()) {
                if (!m_urls.hasMoreElements()) {
                    return false;
                }
                try {
                    URL url = (URL) m_urls.nextElement();
                    BufferedReader reader = readerFor(url);
                    Set seen = m_seen;
                    List buf = m_buf;
                    int lineNo = 1;
                    buf.clear();
                    String line;
                    while ((line = reader.readLine()) != null) {
                        int comment = line.indexOf('#');
                        if (comment >= 0) {
                            line = line.substring(0, comment);
                        }
                        line = line.trim();
                        int len = line.length();
                        if (len != 0) {
                            if ((line.indexOf(' ') >= 0) || (line.indexOf('\t') >= 0)) {
                                throw new ConfigException(url, lineNo);
                            }
                            if (!Character.isJavaIdentifierStart(line.charAt(0))) {
                                throw new ConfigException(line, url, lineNo);
                            }
                            for (int i = 1; i < len; i++) {
                                char c = line.charAt(i);
                                if ((c != '.') && !Character.isJavaIdentifierPart(c)) {
                                    throw new ConfigException(line, url, lineNo);
                                }
                            }
                            if (!seen.contains(line)) {
                                seen.add(line);
                                buf.add(line);
                            }
                        }
                        lineNo++;
                    }
                    reader.close();
                    m_names = buf.iterator();
                } catch (IOException ioe) {
                    throw new ConfigException(ioe);
                }
            }
            return true;
        }

        private static BufferedReader readerFor(URL url) throws IOException {
            return new BufferedReader(new InputStreamReader(url.openStream(), "utf-8"));
        }

        public Object next() {
            if (hasNext()) {
                return m_names.next();
            } else {
                throw new NoSuchElementException();
            }
        }

        public void remove() {
            throw new UnsupportedOperationException();
        }
    }

    /**
	 * Returns an iterator containing the fully qualified names of all classes
	 * implementing the given SPI (and apropriately declared in a file in
	 * a META-INF/services directory on the classpath).
	 * 
	 * NOTE: The recommended naming convention is that the name of the service
	 * provider interface ends in "SPI" and that the name of a class implementing
	 * that interface (that is, a Service Provider) ends in "SP" or "Provider".
	 */
    public static Iterator getProviderNames(Class spi) {
        String resName = SERVICES_PATH_PREFIX + spi.getName();
        try {
            return new ExpandingIterator(ClassLoader.getSystemResources(resName));
        } catch (IOException ioe) {
            throw new ConfigException(ioe);
        }
    }

    /**
	 * Invocation handler that throws a CoServiceNotAvailableException
	 * whenever a method is invoked on its proxy objects.
	 */
    public static class ExceptionThrower implements InvocationHandler {

        private Class m_spi;

        public ExceptionThrower(Class spi) {
            m_spi = spi;
        }

        public Object invoke(Object proxy, Method method, Object[] args) throws Throwable {
            throw new CoServiceNotAvailableException(m_spi);
        }
    }

    /**
	 * Marker interface to distinguish our throwing proxies from other
	 * dynamic ones.
	 */
    private static interface ThrowProxy {
    }

    /**
	 * Return the preferred provider of the service represented by <code>spi</code>.
	 * If no provider at all is found, a dynamic proxy that throws a
	 * CoServiceNotAvailableException whenever it's methods are invoked will be
	 * returned. That case can be recognized by using the isReal method.
	 */
    public static Object getPreferredProvider(Class spi) {
        Object provider = null;
        Iterator providers = getProviders(spi);
        while (providers.hasNext()) {
            provider = providers.next();
        }
        if (provider == null) {
            provider = Proxy.newProxyInstance(spi.getClassLoader(), new Class[] { spi, ThrowProxy.class }, new ExceptionThrower(spi));
        }
        return provider;
    }

    /**
	 * Whether the given object could be a real provider or is just
	 * an exception throwing "proxy".
	 */
    public static boolean isReal(Object provider) {
        return !(ThrowProxy.class.isInstance(provider) && Proxy.isProxyClass(provider.getClass()));
    }

    public static class CycleException extends Exception {

        private CycleException(String message) {
            super(message);
        }
    }

    private static class GraphNode {

        private static final int UNVISITED = 0;

        private static final int IN_PROGRESS = 1;

        private static final int DONE = 2;

        private Object m_payload;

        private Object[] m_prerequisites;

        private int m_state = UNVISITED;

        private int m_seqNum = -1;

        public GraphNode(CoOrderDependent payload) {
            this(payload, payload.getPrerequisites());
        }

        public GraphNode(Object payload, Object[] prerequisites) {
            m_payload = payload;
            m_prerequisites = prerequisites;
        }

        public boolean unvisited() {
            return m_state == UNVISITED;
        }

        public boolean inProgress() {
            return m_state == IN_PROGRESS;
        }

        public Object getPayload() {
            return m_payload;
        }

        public int getSequenceNumber() {
            return m_seqNum;
        }

        public int numberFrom(int nextSeqNum, Map nodeByKey) throws CycleException {
            m_state = IN_PROGRESS;
            if (m_prerequisites != null) {
                for (int i = 0; i < m_prerequisites.length; ++i) {
                    GraphNode node = (GraphNode) nodeByKey.get(m_prerequisites[i]);
                    if (node != null) {
                        if (node.inProgress()) {
                            throw new CycleException("From " + m_payload + " to " + node.getPayload());
                        }
                        if (node.unvisited()) {
                            nextSeqNum = node.numberFrom(nextSeqNum, nodeByKey);
                        }
                    }
                }
            }
            m_state = DONE;
            m_seqNum = nextSeqNum;
            return nextSeqNum + 1;
        }
    }

    /**
	 * Call the vistit method in the visitor once for each class that
	 * implements the interface specified in the spiClass parameter
	 */
    public static void eachProvider(Class spiClass, ProviderVisitor visitor) {
        Iterator providers = CoServices.getProviderNames(spiClass);
        while (providers.hasNext()) {
            String className = (String) providers.next();
            Class cls = null;
            try {
                cls = Class.forName(className);
            } catch (ClassNotFoundException e) {
                System.out.println("Class not found: " + className);
            }
            try {
                Object provider = cls.getConstructor(new Class[] {}).newInstance(new Object[] {});
                visitor.visit(provider);
            } catch (IllegalArgumentException e1) {
                System.out.println("IllegalArgumentException");
                throw new RuntimeException("IllegalArgumentException");
            } catch (SecurityException e1) {
                System.out.println("SecurityException");
                throw new RuntimeException("SecurityException");
            } catch (InstantiationException e1) {
                System.out.println("InstantiationException");
                throw new RuntimeException("InstantiationException");
            } catch (IllegalAccessException e1) {
                System.out.println("IllegalAccessException");
                throw new RuntimeException("IllegalAccessException");
            } catch (InvocationTargetException e1) {
                System.out.println("InvocationTargetException");
                throw new RuntimeException("InvocationTargetException");
            } catch (NoSuchMethodException e1) {
                System.out.println("NoSuchMethodException");
                throw new RuntimeException("NoSuchMethodException");
            }
        }
    }

    /**
	 * Note: For this to work it is required that the interface or class
	 * represented by <code>spi</code> extends or implements CoOrderDependent.
	 * That is, the statement<br/>
	 * <code>CoOrderDependent.class.isAssignableFrom(spi)</code><br/>
	 * should evaluate to true.
	 */
    public static Collection getOrderedProviders(Class spi) throws CycleException {
        if (!CoOrderDependent.class.isAssignableFrom(spi)) {
            throw new IllegalArgumentException(spi + " does not extend CoOrderDependent.");
        }
        return orderDependents(CoServices.getProviders(spi));
    }

    /**
	 * Note: For this to work it is required that the elements returned by
	 * the iterator <code>dependents</code> is an instance of CoOrderDependent.
	 * That is, the statement<br/>
	 * <code>element instanceof CoOrderDependent</code><br/>
	 * should evaluate to true for every element.
	 */
    public static Collection orderDependents(Iterator dependents) throws CycleException {
        Map nodeByKey = new HashMap();
        List nodes = new ArrayList();
        while (dependents.hasNext()) {
            CoOrderDependent dependent = (CoOrderDependent) dependents.next();
            GraphNode node = new GraphNode(dependent, dependent.getPrerequisites());
            nodeByKey.put(dependent.getDependencyKey(), node);
            nodes.add(node);
        }
        Object[] ordered = new Object[nodes.size()];
        int nextSeqNum = 0;
        Iterator nodeIter = nodes.iterator();
        while (nodeIter.hasNext()) {
            GraphNode node = (GraphNode) nodeIter.next();
            if (node.unvisited()) {
                nextSeqNum = node.numberFrom(nextSeqNum, nodeByKey);
            }
            ordered[node.getSequenceNumber()] = node.getPayload();
        }
        return Arrays.asList(ordered);
    }
}
