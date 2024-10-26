package org.apache.harmony.jpda.tests.jdwp.Events;

import java.io.IOException;
import java.io.InputStream;
import java.net.URL;
import org.apache.harmony.jpda.tests.framework.LogWriter;
import org.apache.harmony.jpda.tests.framework.TestErrorException;
import org.apache.harmony.jpda.tests.share.JPDADebuggeeSynchronizer;
import org.apache.harmony.jpda.tests.share.SyncDebuggee;

/**
 * Debuggee for ClassUnloadTest unit test.
 */
public class ClassUnloadDebuggee extends SyncDebuggee {

    public static final String TESTED_CLASS_NAME = "org.apache.harmony.jpda.tests.jdwp.Events.ClassUnloadTestedClass";

    public static final int ARRAY_SIZE_FOR_MEMORY_STRESS = 1000000;

    public static volatile boolean classUnloaded = false;

    public static void main(String[] args) {
        runDebuggee(ClassUnloadDebuggee.class);
    }

    public void run() {
        logWriter.println("--> ClassUnloadDebuggee started");
        logWriter.println("--> Load and prepare tested class");
        CustomLoader loader = new CustomLoader(logWriter);
        Class cls = null;
        try {
            cls = Class.forName(TESTED_CLASS_NAME, true, loader);
            logWriter.println("--> Tested class loaded: " + cls);
        } catch (Exception e) {
            logWriter.println("--> Unable to load tested class: " + e);
            throw new TestErrorException(e);
        }
        synchronizer.sendMessage(JPDADebuggeeSynchronizer.SGNL_READY);
        synchronizer.receiveMessage(JPDADebuggeeSynchronizer.SGNL_CONTINUE);
        logWriter.println("--> Erase references to loaded class and its class loader");
        classUnloaded = false;
        cls = null;
        loader = null;
        logWriter.println("--> Create memory stress and start gc");
        createMemoryStress(1000000, ARRAY_SIZE_FOR_MEMORY_STRESS);
        System.gc();
        String status = (classUnloaded ? "UNLOADED" : "LOADED");
        logWriter.println("--> Class status after memory stress: " + status);
        synchronizer.sendMessage(status);
        synchronizer.receiveMessage(JPDADebuggeeSynchronizer.SGNL_CONTINUE);
        logWriter.println("--> ClassUnloadDebuggee finished");
    }

    protected void createMemoryStress(int arrayLength_0, int arrayLength_1) {
        Runtime currentRuntime = Runtime.getRuntime();
        long freeMemory = currentRuntime.freeMemory();
        logWriter.println("--> Debuggee: createMemoryStress: freeMemory (bytes) before memory stress = " + freeMemory);
        long[][] longArrayForCreatingMemoryStress = null;
        int i = 0;
        try {
            longArrayForCreatingMemoryStress = new long[arrayLength_0][];
            for (; i < longArrayForCreatingMemoryStress.length; i++) {
                longArrayForCreatingMemoryStress[i] = new long[arrayLength_1];
            }
            logWriter.println("--> Debuggee: createMemoryStress: NO OutOfMemoryError!!!");
        } catch (OutOfMemoryError outOfMem) {
            longArrayForCreatingMemoryStress = null;
            logWriter.println("--> Debuggee: createMemoryStress: OutOfMemoryError!!!");
        }
        freeMemory = currentRuntime.freeMemory();
        logWriter.println("--> Debuggee: createMemoryStress: freeMemory after creating memory stress = " + freeMemory);
        longArrayForCreatingMemoryStress = null;
    }

    /**
     * Custom class loader to be used for tested class.
     * It will be collected and finalized when tested class is unloaded.
     */
    static class CustomLoader extends ClassLoader {

        private LogWriter logWriter;

        public CustomLoader(LogWriter writer) {
            this.logWriter = writer;
        }

        public Class<?> loadClass(String name) throws ClassNotFoundException {
            if (TESTED_CLASS_NAME.equals(name)) {
                return findClass(name);
            }
            return getParent().loadClass(name);
        }

        public Class<?> findClass(String name) throws ClassNotFoundException {
            try {
                logWriter.println("-->> CustomClassLoader: Find class: " + name);
                String res = name.replace('.', '/') + ".class";
                URL url = getResource(res);
                logWriter.println("-->> CustomClassLoader: Found class file: " + res);
                InputStream is = url.openStream();
                int size = 1024;
                byte bytes[] = new byte[size];
                int len = loadClassData(is, bytes, size);
                logWriter.println("-->> CustomClassLoader: Loaded class bytes: " + len);
                Class cls = defineClass(name, bytes, 0, len);
                logWriter.println("-->> CustomClassLoader: Defined class: " + cls);
                return cls;
            } catch (Exception e) {
                throw new ClassNotFoundException("Cannot load class: " + name, e);
            }
        }

        private int loadClassData(InputStream in, byte[] raw, int size) throws IOException {
            int len = in.read(raw);
            if (len >= size) throw new IOException("Class file is too big: " + len);
            in.close();
            return len;
        }

        protected void finalize() throws Throwable {
            logWriter.println("-->> CustomClassLoader: Class loader finalized => tested class UNLOADED");
            ClassUnloadDebuggee.classUnloaded = true;
        }
    }
}

/**
 * Internal class used in ClassUnloadTest
 */
class ClassUnloadTestedClass {
}
