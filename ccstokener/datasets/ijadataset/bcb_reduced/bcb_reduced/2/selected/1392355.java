package de.str.prettysource.html;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.Reader;
import java.io.StringReader;
import java.io.StringWriter;
import java.io.Writer;
import java.net.URL;
import junit.framework.TestCase;
import org.junit.Assert;
import de.str.prettysource.OutputFormat;

public class BaseTestCase extends TestCase {

    public void assertFormating(OutputFormat formatter, String source, String expected) {
        System.out.println("Formatting: " + source);
        Reader rActual = null;
        Writer writer = new StringWriter();
        Reader rExpected = null;
        try {
            rActual = new InputStreamReader(TestSource2Html.class.getResource("/" + source).openStream(), "UTF-8");
            rExpected = new InputStreamReader(TestSource2Html.class.getResource("/" + expected).openStream(), "UTF-8");
        } catch (Exception e) {
            fail(e.getMessage());
        }
        formatter.format(rActual, writer);
        rActual = new StringReader(writer.toString());
        this.assertResult(rActual, rExpected);
    }

    private void assertResult(Reader rActual, Reader rExpected) {
        BufferedReader brExpected = new BufferedReader(rExpected);
        BufferedReader brActual = new BufferedReader(rActual);
        try {
            while (brExpected.ready()) {
                String expected = brExpected.readLine();
                String actual = brActual.readLine();
                Assert.assertEquals(expected, actual);
            }
        } catch (IOException e) {
            fail(e.getMessage());
        }
    }

    public static void createTestResult(OutputFormat formatter, File in) {
        try {
            URL sourceURL = in.toURI().toURL();
            createTestResult(formatter, sourceURL);
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    public static void createTestResult(OutputFormat formatter, String in) {
        URL sourceURL = TestSource2Html.class.getResource("/" + in);
        createTestResult(formatter, sourceURL);
    }

    public static void createTestResult(OutputFormat formatter, URL url) {
        Reader sourceReader = null;
        Writer targetWriter = null;
        try {
            sourceReader = new InputStreamReader(url.openStream(), "UTF-8");
            String fullPath = url.getPath();
            String name = fullPath.substring(fullPath.lastIndexOf("/") + 1);
            File f = new File("c:/_test/" + name + ".html");
            f.createNewFile();
            targetWriter = new FileWriter(f);
        } catch (Exception e) {
            e.printStackTrace();
        }
        formatter.format(sourceReader, targetWriter);
    }
}
