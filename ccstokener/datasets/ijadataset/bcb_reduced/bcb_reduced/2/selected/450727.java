package com.objectwave.tools;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.FileInputStream;
import java.io.InputStream;
import com.objectwave.customClassLoader.MultiClassLoader;
import com.objectwave.classFile.ClassFile;
import com.objectwave.classFile.ChangeClass;

/**
 * Loads class bytes from a file.
 *
 * @author Dave Hoag
 */
public class TraceClassLoader extends MultiClassLoader {

    public TraceClassLoader() {
        loadLocalClasses = true;
    }

    /** */
    public Object getContent(java.net.URL url) throws java.io.IOException {
        return url.getContent();
    }

    /** */
    public InputStream getInputStream(java.net.URL url) throws java.io.IOException {
        return url.openStream();
    }

    protected byte[] loadLocalClassBytes(String fullClassName) {
        return loadClassBytes(fullClassName);
    }

    protected byte[] loadClassBytes(String className) {
        String formattedClassName = formatClassName(className);
        byte result[];
        String fileName = formattedClassName;
        try {
            monitor("Finding class on class path");
            InputStream inStream = locateClass(className);
            if (inStream == null) return null;
            result = new byte[inStream.available()];
            inStream.read(result);
            inStream.close();
            if (className.startsWith("test.")) return result;
            if (className.indexOf(".classFile.") > -1) return result;
            ByteArrayInputStream bin = new ByteArrayInputStream(result);
            ClassFile cf = new ClassFile();
            if (cf.read(bin)) {
                new ChangeClass().injectTrace(cf);
                ByteArrayOutputStream bos = new ByteArrayOutputStream();
                cf.write(bos);
                result = bos.toByteArray();
                System.out.println("Altered class bytes of  " + cf.getClassName());
                return result;
            }
            System.out.println("Failed to alter bytes of " + className);
            return null;
        } catch (Exception e) {
            System.out.println("Failed to alter bytes of " + className + " : " + e);
            return null;
        }
    }
}
