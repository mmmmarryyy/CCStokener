package org.jul.dcl.classpath;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.net.URL;
import java.util.ArrayList;
import java.util.List;
import java.util.jar.JarEntry;
import java.util.jar.JarInputStream;
import org.jul.common.ExceptionHandler;
import org.jul.dcl.fetcher.*;

class JarFinder extends ClassFinder {

    private static final int PACKET_SIZE = 4096;

    @Override
    protected List<ClassFetcher> find0(ExceptionHandler handler) {
        List<ClassFetcher> classes = new ArrayList<ClassFetcher>();
        try {
            JarInputStream jarStream = new JarInputStream(url.openStream());
            JarEntry entry;
            try {
                while ((entry = jarStream.getNextJarEntry()) != null) {
                    if (!entry.isDirectory() && entry.getName().endsWith(CLASS_FILE_EXTENSION)) {
                        ByteArrayOutputStream output = new ByteArrayOutputStream();
                        byte[] buffer = new byte[PACKET_SIZE];
                        int bytesRead;
                        while ((bytesRead = jarStream.read(buffer, 0, buffer.length)) != -1) {
                            output.write(buffer, 0, bytesRead);
                        }
                        byte[] classData = output.toByteArray();
                        classes.add(ClassFetcher.getFetcher(url, classData));
                    }
                }
            } finally {
                jarStream.close();
            }
        } catch (UnsupportedSourceException e) {
            handler.handle(e);
        } catch (IOException e) {
            handler.handle(e);
        }
        return classes;
    }

    public JarFinder(URL url) {
        super(url);
    }
}
