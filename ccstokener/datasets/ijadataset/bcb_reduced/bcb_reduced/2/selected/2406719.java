package net.sf.buildbox;

import net.sf.buildbox.parser.LineParser;
import net.sf.buildbox.parser.LookAheadBuffer;
import net.sf.buildbox.parser.LookAheadBufferImpl;
import java.io.*;
import java.net.URL;

public class TestUtils {

    public static File resourceAsFile(Class anyTestClass) throws IOException {
        final String clsUri = anyTestClass.getName().replace('.', '/') + ".class";
        final URL url = anyTestClass.getClassLoader().getResource(clsUri);
        if (url == null) {
            throw new IOException("Probably not a test class: " + anyTestClass.getName());
        }
        final String clsPath = url.getPath();
        final File testClassesRoot = new File(clsPath.substring(0, clsPath.length() - clsUri.length()));
        final File clsFile = new File(testClassesRoot, clsUri);
        if (!clsFile.exists()) {
            throw new FileNotFoundException("Sanity check failed - class is probably inside a JAR: " + anyTestClass.getName());
        }
        return clsFile;
    }

    public static void print(String prefix, LineParser block) {
        System.err.println(prefix + block);
        for (LineParser innerBlock : block.getSubcontexts()) {
            print(prefix + "  ", innerBlock);
        }
    }

    public static LookAheadBuffer createBuffer(URL url) throws IOException {
        final Reader isr = new InputStreamReader(url.openStream());
        final BufferedReader bufferedReader = new BufferedReader(isr);
        return new LookAheadBufferImpl(bufferedReader);
    }
}
