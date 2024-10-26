package net.sourceforge.jxunit;

import junit.extensions.ActiveTestSuite;
import junit.framework.*;
import java.util.*;
import java.io.*;
import java.net.*;
import com.jxml.quick.*;

public class JXTestCase extends TestCase {

    private JXProperties properties = null;

    private String testDirectory = ".";

    private static String CWD;

    private boolean active;

    public JXTestCase(String testName, JXProperties properties) {
        super(testName);
        properties.testCase = this;
        this.properties = properties;
    }

    public JXProperties getProperties() {
        return properties;
    }

    public String getTestDirectory() {
        return testDirectory;
    }

    public String readAll(InputStreamReader reader) throws Throwable {
        StringBuffer sb = new StringBuffer();
        char[] cbuf = new char[1024];
        int i;
        do {
            i = reader.read(cbuf);
            if (i > 0) sb.append(cbuf, 0, i);
        } while (i > 0);
        reader.close();
        String fileValue = sb.toString();
        return fileValue;
    }

    public String read(String fileName) throws Throwable {
        FileReader reader = new FileReader(fileName);
        return readAll(reader);
    }

    public String urlRead(String urlString) throws Throwable {
        URL url = new URL(urlString);
        URLConnection connection = url.openConnection();
        connection.connect();
        InputStream is = connection.getInputStream();
        InputStreamReader reader = new InputStreamReader(is);
        return readAll(reader);
    }

    public String getAbsolutePath(String path, boolean indirect) {
        if (indirect) {
            path = properties.getString(path);
        }
        return getAbsolutePath(testDirectory, path);
    }

    public String getAbsolutePath(String path) {
        return getAbsolutePath(testDirectory, path);
    }

    public static String getAbsolutePath(String testDirectory, String path) {
        File file = new File(path);
        if (file.isAbsolute()) return path;
        file = new File(testDirectory + File.separator + path);
        return file.getAbsolutePath();
    }

    public void setTestDirectory(String testDirectory) {
        this.testDirectory = testDirectory;
    }

    public String getString(String file, String value, boolean indirect) throws Throwable {
        if (value != null && !"".equals(value)) return getStringValue(value, indirect); else return getStringFile(file, indirect);
    }

    public String getStringValue(String value, boolean indirect) throws Throwable {
        if (indirect) {
            value = properties.getString(value);
        }
        return value;
    }

    public String getStringFile(String file, boolean indirect) throws Throwable {
        if (indirect) {
            String newfile = properties.getString(file);
            if ("".equals(newfile)) fail("undefined file: " + file);
            file = newfile;
        }
        if (file.indexOf(":") > 1) return urlRead(file);
        File f = new File(file);
        if (f.isAbsolute()) return read(file);
        String fileName = getAbsolutePath(testDirectory, file);
        return read(fileName);
    }

    public Object getObjectValue(String schema, String schemaClass, String file, String value, boolean indirect) throws Throwable {
        if ((schema == null || "".equals(schema)) && (schemaClass == null || "".equals(schemaClass))) return getObjectNoSchema(file, value, indirect);
        return getObjectSchema(schema, schemaClass, file, value, indirect);
    }

    public Object getObjectSchema(String schema, String schemaClass, String file, String value, boolean indirect) throws Throwable {
        QDoc schemaDoc = null;
        if (schema == null || "".equals(schema)) {
            schemaDoc = QSchemaFactory.create(schemaClass);
        } else {
            String schemaName = getAbsolutePath(testDirectory, schema);
            schemaDoc = Quick.parseSchema(schemaName);
        }
        QDoc valueDoc;
        if (value != null && !"".equals(value)) valueDoc = getValueSchema(schemaDoc, value, indirect); else valueDoc = getFileSchema(schemaDoc, file, indirect);
        return valueDoc.getRoot();
    }

    public QDoc getValueSchema(QDoc schemaDoc, String value, boolean indirect) throws Throwable {
        if (indirect) {
            String newvalue = properties.getString(value);
            if ("".equals(newvalue)) fail("undefined value: " + value);
            value = newvalue;
        }
        return Quick.parseString(schemaDoc, value);
    }

    public QDoc getFileSchema(QDoc schemaDoc, String file, boolean indirect) throws Throwable {
        String fileName = getAbsolutePath(file, indirect);
        return Quick.parse(schemaDoc, fileName);
    }

    public Object getObjectNoSchema(String file, String value, boolean indirect) throws Throwable {
        if (value != null && !"".equals(value)) return getObjectValueNoSchema(value, indirect);
        return getStringFile(file, indirect);
    }

    public Object getObjectValueNoSchema(String value, boolean indirect) throws Throwable {
        if (indirect) return properties.getString(value);
        return value;
    }

    protected void runTest() throws Throwable {
        try {
            String jxuSchema = properties.getString("jxuSchemaName");
            QDoc schema = getSchema(jxuSchema);
            if (schema == null) {
                fail("Unable to locate jxu.qiml from " + testDirectory);
            }
            String testName = testDirectory + File.separator + "test.jxu";
            QDoc test = Quick.parse(schema, testName);
            JXDo steps = (JXDo) test.getRoot();
            steps.eval(this);
        } catch (QPE qpe) {
            QPE.display(qpe);
            throw qpe;
        }
    }

    private static QDoc getSchema(String name) throws Throwable {
        try {
            QDoc schema = null;
            if (!"".equals(name)) schema = Quick.parseSchema(name); else schema = CreateJxu.createSchema();
            return schema;
        } catch (Exception pe) {
            QPE.display(pe);
            throw new Error(pe.toString());
        }
    }

    public static Test suite() throws Throwable {
        com.jxml.protocol.Protocol.addJXMLProtocolPackage();
        CWD = (new File(".")).getAbsoluteFile().getParent();
        TestSuite suite = new TestSuite();
        buildSuite(CWD, suite);
        return suite;
    }

    public static void buildTest(String cwd, TestSuite suite) throws Throwable {
        File jxucFile = new File(cwd + File.separator + "test.jxuc");
        JXProperties properties = new JXProperties();
        properties.put("/", File.separator);
        properties.put(".", CWD);
        properties.put("testDirectory", cwd);
        int i, s;
        if (jxucFile.exists()) {
            QDoc schema = CreateJxuc.createSchema();
            if (schema == null) {
                System.err.println("Unable to locate jxuc.qiml from " + cwd);
                throw new Error("missing schema");
            }
            QDoc configDoc = null;
            try {
                configDoc = Quick.parse(schema, jxucFile.toString());
            } catch (QPE pe) {
                QPE.display(pe);
                throw new Error(pe.toString());
            }
            JXConfig config = (JXConfig) configDoc.getRoot();
            config.setup(cwd, properties);
        } else {
            properties.put("jxuSchemaName", "classpath:///net/sourceforge/jxunit/jxu.qiml");
        }
        List candidateFiles = (List) properties.get("candidateFiles");
        if (candidateFiles == null) {
            JXTestCase tc = new JXTestCase(cwd, properties);
            tc.setTestDirectory(cwd);
            suite.addTest(tc);
            return;
        }
        s = candidateFiles.size();
        String absDir = properties.getString("absDir");
        if (properties.get("active").equals("true")) {
            TestSuite aSuite = new ActiveTestSuite();
            suite.addTest(aSuite);
            suite = aSuite;
        }
        for (i = 0; i < s; ++i) {
            JXProperties newProperties = new JXProperties(properties);
            String dataFileName = (String) candidateFiles.get(i);
            newProperties.put("dataFileName", dataFileName);
            String absDataFileName = absDir + File.separator + dataFileName;
            newProperties.put("absDataFileName", absDataFileName);
            JXTestCase tc = new JXTestCase(absDataFileName, newProperties);
            tc.setTestDirectory(cwd);
            suite.addTest(tc);
        }
    }

    public static void buildSuite(String cwd, TestSuite suite) throws Throwable {
        try {
            File pathName = new File(cwd + File.separator + "test.jxu");
            if (pathName.exists()) {
                buildTest(cwd, suite);
            }
            pathName = new File(cwd);
            String fileNames[] = pathName.list();
            for (int j = 0; j < fileNames.length; ++j) {
                File f = new File(pathName.getPath(), fileNames[j]);
                if (f.isDirectory()) {
                    buildSuite(f.getAbsolutePath(), suite);
                }
            }
        } catch (QPE pe) {
            QPE.display(pe);
            throw pe;
        } catch (Throwable e) {
            e.printStackTrace();
            throw e;
        }
    }
}
