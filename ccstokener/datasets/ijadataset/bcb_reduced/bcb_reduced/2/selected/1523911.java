package ch.sahits.codegen.wizards;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;
import java.net.MalformedURLException;
import java.net.URL;
import java.util.Properties;
import junit.framework.Assert;
import org.eclipse.core.resources.ResourcesPlugin;
import org.eclipse.core.runtime.CoreException;
import org.jdom.JDOMException;
import org.junit.AfterClass;
import org.junit.Before;
import org.junit.Test;
import ch.sahits.codegen.test.PropertyFileLoader;
import ch.sahits.codegen.util.ERunAsType;
import ch.sahits.codegen.util.RunAs;
import ch.sahits.test.ComparetorResult;
import ch.sahits.test.FileComparator;

/**
 * Test class for the generation of general artifacts.
 * This Test class represents an integration test.
 * @author Andi Hotz, Sahits GmbH
 * @since 2.1.0
 */
@RunAs(ERunAsType.PLUGIN)
public class HeadlessArtefactGeneratorTest extends PropertyFileLoader {

    private static String tempdir;

    private String oracleInputFile;

    private String serializedSQLs;

    private static String outDir;

    private String wsXMLInput;

    private String phpInputFile;

    private String[] help;

    private String[] sqls;

    private String[] sqlsSerialize;

    private String[] sqlsDeserialize;

    private String[] wsServices;

    private String[] hibernate;

    private String[] wsdl;

    private String[] inoutreciever;

    private String[] stub;

    private String[] phpXMLTemplate;

    /**
	 * Initialize the members from property file
	 */
    public HeadlessArtefactGeneratorTest() {
        Properties prop = loadFile("fragments/headlessgeneral.properties");
        tempdir = prop.getProperty("tempdir");
        String workspace = ResourcesPlugin.getWorkspace().getRoot().getLocationURI().getPath();
        outDir = workspace + tempdir + File.separator;
        oracleInputFile = prop.getProperty("inputfile");
        serializedSQLs = prop.getProperty("serialized_sql");
        wsXMLInput = prop.getProperty("wsXML");
        phpInputFile = prop.getProperty("testPluginDir") + "/fragments/phpExampleTemplate.xml";
    }

    @Override
    protected Properties loadFile(String fileName) {
        Properties prop = new Properties();
        try {
            URL url = new File(fileName).toURI().toURL();
            final InputStream input = url.openStream();
            prop.load(input);
        } catch (MalformedURLException e) {
            e.printStackTrace();
        } catch (IOException e) {
            e.printStackTrace();
        }
        return prop;
    }

    /**
	 * Initialize the parameter strings
	 * @throws Exception
	 */
    @Before
    public void setUp() throws Exception {
        help = new String[] { "help" };
        sqls = new String[] { "-o", tempdir, "-f", "sqls.sql", "-m", "WDB", "-p", "ORACLE", "-i", oracleInputFile, "-g", "ch.sahits.codegen@jet_templates/selectSQL.sqljet" };
        sqlsSerialize = new String[] { "-s", outDir + "serializedSqls.sql.xml", "-o", tempdir, "-f", "sqls.sql", "-m", "WDB", "-p", "ORACLE", "-i", oracleInputFile, "-g", "ch.sahits.codegen@jet_templates/selectSQL.sqljet" };
        sqlsDeserialize = new String[] { "-l", serializedSQLs };
        wsServices = new String[] { "-o", tempdir, "-f", "services.xml", "-m", "WODB", "-i", wsXMLInput, "-g", "ch.sahits.codegen.java.examples@ch.sahits.codegen.generator.Axis2ServiceXMLGenerator" };
        hibernate = new String[] { "-o", tempdir, "-f", "Bar.xml", "-m", "WDB", "-p", "ORACLE", "-i", oracleInputFile, "-g", "ch.sahits.codegen@ch.sahits.codegen.generator.HibernateConfigurationCreator", "-dbn", "name", "-dbh", "localhost", "-dbp", "1521", "-dbu", "sys", "-dbpass", "god", "-dbt", "person", "-dbs", "HR" };
        wsdl = new String[] { "-o", tempdir, "-f", "LoginService.wsdl", "-m", "WODB", "-i", wsXMLInput, "-g", "ch.sahits.codegen.java.examples@ch.sahits.codegen.generator.WSDLGenerator" };
        inoutreciever = new String[] { "-o", tempdir, "-f", "LoginServiceMessageReceiverInOut.java", "-m", "WODB", "-i", wsXMLInput, "-g", "ch.sahits.codegen.java.examples@ch.sahits.codegen.generator.Axis2MessageInOutReciever" };
        stub = new String[] { "-o", tempdir, "-f", "LoginServiceStub.java", "-m", "WODB", "-i", wsXMLInput, "-g", "ch.sahits.codegen.java.examples@ch.sahits.codegen.generator.Axis2ServiceStubGenerator" };
        phpXMLTemplate = new String[] { "-o", tempdir, "-f", "Bar.php", "-m", "WDB", "-p", "MYSQL", "-i", phpInputFile, "-g", "ch.sahits.codegen@ch.sahits.codegen.php.PHPCodeGenerator", "-dbn", "eclipse", "-dbh", "localhost", "-dbp", "3306", "-dbu", "root", "-dbpass", "", "-dbt", "USER2", "-dbs", "HR" };
    }

    /**
	 * Test the print out on the console. Visual verification
	 */
    public void testPrintUsage() {
        try {
            new HeadlessArtefactGenerator().run(help);
        } catch (FileNotFoundException e) {
            Assert.fail(e.getMessage());
        } catch (IOException e) {
            Assert.fail(e.getMessage());
        } catch (ClassNotFoundException e) {
            Assert.fail(e.getMessage());
        } catch (CoreException e) {
            Assert.fail(e.getMessage());
        } catch (JDOMException e) {
            Assert.fail("No serialisation should take place");
        }
    }

    /**
	 * Test the generation of the sql scipt
	 */
    @Test
    public void testGenerationSQL() {
        try {
            new HeadlessArtefactGenerator().run(sqls);
        } catch (FileNotFoundException e1) {
            Assert.fail(e1.getMessage());
        } catch (IOException e1) {
            Assert.fail(e1.getMessage());
        } catch (ClassNotFoundException e1) {
            Assert.fail(e1.getMessage());
        } catch (CoreException e1) {
            Assert.fail(e1.getMessage());
        } catch (JDOMException e) {
            Assert.fail("No serialisation should take place");
        }
        String outputFile = outDir + "sqls.sql";
        ComparetorResult expected = ComparetorResult.TRUE;
        try {
            FileComparator comp = new FileComparator(outputFile);
            ComparetorResult actual = comp.equals("fragments/sqls.test");
            Assert.assertEquals(expected, actual);
        } catch (FileNotFoundException e) {
            e.printStackTrace();
            Assert.fail(e.getMessage());
        } catch (IOException e) {
            Assert.fail(e.getMessage());
        }
    }

    /**
	 * Test the generation of a serialized configuration
	 */
    @Test
    public void testSerializeSQL() {
        try {
            new HeadlessArtefactGenerator().run(sqlsSerialize);
        } catch (FileNotFoundException e1) {
            e1.printStackTrace();
            Assert.fail(e1.getMessage());
        } catch (IOException e1) {
            Assert.fail(e1.getMessage());
        } catch (ClassNotFoundException e1) {
            Assert.fail(e1.getMessage());
        } catch (CoreException e1) {
            Assert.fail(e1.getMessage());
        } catch (JDOMException e1) {
            Assert.fail(e1.getMessage());
        }
        String outputFile = outDir + "serializedSqls.sql.xml";
        ComparetorResult expected = ComparetorResult.TRUE;
        try {
            FileComparator comp = new FileComparator(outputFile);
            ComparetorResult actual = comp.equals(serializedSQLs);
            Assert.assertEquals(expected, actual);
        } catch (FileNotFoundException e) {
            e.printStackTrace();
            Assert.fail(e.getMessage());
        } catch (IOException e) {
            Assert.fail(e.getMessage());
        }
    }

    /**
	 * Test the generation of a sqls.sql from deserializing a configuration
	 */
    @Test
    public void testDeSerializeSQL() {
        try {
            new HeadlessArtefactGenerator().run(sqlsDeserialize);
        } catch (FileNotFoundException e1) {
            e1.printStackTrace();
            Assert.fail(e1.getMessage());
        } catch (IOException e1) {
            Assert.fail(e1.getMessage());
        } catch (ClassNotFoundException e1) {
            Assert.fail(e1.getMessage());
        } catch (CoreException e1) {
            Assert.fail(e1.getMessage());
        } catch (JDOMException e1) {
            Assert.fail(e1.getMessage());
        }
        String outputFile = outDir + "sqls.sql";
        ComparetorResult expected = ComparetorResult.TRUE;
        try {
            FileComparator comp = new FileComparator(outputFile);
            ComparetorResult actual = comp.equals("fragments/sqls.test");
            Assert.assertEquals(expected, actual);
        } catch (FileNotFoundException e) {
            e.printStackTrace();
            Assert.fail(e.getMessage());
        } catch (IOException e) {
            Assert.fail(e.getMessage());
        }
    }

    @Test
    public void testGenerationHibernateXML() {
        try {
            new HeadlessArtefactGenerator().run(hibernate);
        } catch (FileNotFoundException e1) {
            Assert.fail(e1.getMessage());
        } catch (IOException e1) {
            Assert.fail(e1.getMessage());
        } catch (ClassNotFoundException e1) {
            Assert.fail(e1.getMessage());
        } catch (CoreException e1) {
            Assert.fail(e1.getMessage());
        } catch (JDOMException e) {
            Assert.fail("No serialisation should take place");
        }
        String outputFile = outDir + "Bar.xml";
        ComparetorResult expected = ComparetorResult.TRUE;
        try {
            FileComparator comp = new FileComparator(outputFile);
            ComparetorResult actual = comp.equals("fragments/headless_test/Bar1.xml");
            Assert.assertEquals(expected, actual);
        } catch (FileNotFoundException e) {
            e.printStackTrace();
            Assert.fail(e.getMessage());
        } catch (IOException e) {
            Assert.fail(e.getMessage());
        }
    }

    @Test
    public void testGenerationWSDL() {
        try {
            new HeadlessArtefactGenerator().run(wsdl);
        } catch (FileNotFoundException e1) {
            Assert.fail(e1.getMessage());
        } catch (IOException e1) {
            Assert.fail(e1.getMessage());
        } catch (ClassNotFoundException e1) {
            Assert.fail(e1.getMessage());
        } catch (CoreException e1) {
            Assert.fail(e1.getMessage());
        } catch (JDOMException e) {
            Assert.fail("No serialisation should take place");
        }
        String outputFile = outDir + "LoginService.wsdl";
        ComparetorResult expected = ComparetorResult.TRUE;
        try {
            FileComparator comp = new FileComparator(outputFile);
            ComparetorResult actual = comp.equals("fragments/headless_test/Bar2.wsdl");
            Assert.assertEquals(expected, actual);
        } catch (FileNotFoundException e) {
            e.printStackTrace();
            Assert.fail(e.getMessage());
        } catch (IOException e) {
            Assert.fail(e.getMessage());
        }
    }

    @Test
    public void testGenerationServicesXML() {
        try {
            new HeadlessArtefactGenerator().run(wsServices);
        } catch (FileNotFoundException e1) {
            Assert.fail(e1.getMessage());
        } catch (IOException e1) {
            Assert.fail(e1.getMessage());
        } catch (ClassNotFoundException e1) {
            Assert.fail(e1.getMessage());
        } catch (CoreException e1) {
            Assert.fail(e1.getMessage());
        } catch (JDOMException e) {
            Assert.fail("No serialisation should take place");
        }
        String outputFile = outDir + "services.xml";
        ComparetorResult expected = ComparetorResult.TRUE;
        try {
            FileComparator comp = new FileComparator(outputFile);
            ComparetorResult actual = comp.equals("fragments/headless_test/services.xml");
            Assert.assertEquals(expected, actual);
        } catch (FileNotFoundException e) {
            e.printStackTrace();
            Assert.fail(e.getMessage());
        } catch (IOException e) {
            Assert.fail(e.getMessage());
        }
    }

    @Test
    public void testGenerationMessageInOutReciever() {
        try {
            new HeadlessArtefactGenerator().run(inoutreciever);
        } catch (FileNotFoundException e1) {
            Assert.fail(e1.getMessage());
        } catch (IOException e1) {
            Assert.fail(e1.getMessage());
        } catch (ClassNotFoundException e1) {
            Assert.fail(e1.getMessage());
        } catch (CoreException e1) {
            Assert.fail(e1.getMessage());
        } catch (JDOMException e) {
            Assert.fail("No serialisation should take place");
        }
        String outputFile = outDir + "src" + File.separator + "org" + File.separator + "apache" + File.separator + "axis2" + File.separator + "LoginServiceMessageReceiverInOut.java";
        ComparetorResult expected = ComparetorResult.TRUE;
        try {
            FileComparator comp = new FileComparator(outputFile);
            ComparetorResult actual = comp.equals("fragments/headless_test/LoginServiceMessageReceiverInOut.java");
            Assert.assertEquals(expected, actual);
        } catch (FileNotFoundException e) {
            e.printStackTrace();
            Assert.fail(e.getMessage());
        } catch (IOException e) {
            Assert.fail(e.getMessage());
        }
    }

    @Test
    public void testGenerationServersideStub() {
        try {
            new HeadlessArtefactGenerator().run(stub);
        } catch (FileNotFoundException e1) {
            Assert.fail(e1.getMessage());
        } catch (IOException e1) {
            Assert.fail(e1.getMessage());
        } catch (ClassNotFoundException e1) {
            Assert.fail(e1.getMessage());
        } catch (CoreException e1) {
            Assert.fail(e1.getMessage());
        } catch (JDOMException e) {
            Assert.fail("No serialisation should take place");
        }
        String outputFile = outDir + "src" + File.separator + "org" + File.separator + "apache" + File.separator + "axis2" + File.separator + "LoginServiceStub.java";
        ComparetorResult expected = ComparetorResult.TRUE;
        try {
            FileComparator comp = new FileComparator(outputFile);
            ComparetorResult actual = comp.equals("fragments/headless_test/LoginServiceStub.java");
            Assert.assertEquals(expected, actual);
        } catch (FileNotFoundException e) {
            e.printStackTrace();
            Assert.fail(e.getMessage());
        } catch (IOException e) {
            Assert.fail(e.getMessage());
        }
    }

    @Test
    public void testGenerationPHPfromXML() {
        try {
            new HeadlessArtefactGenerator().run(phpXMLTemplate);
        } catch (FileNotFoundException e1) {
            Assert.fail(e1.getMessage());
        } catch (IOException e1) {
            Assert.fail(e1.getMessage());
        } catch (ClassNotFoundException e1) {
            Assert.fail(e1.getMessage());
        } catch (CoreException e1) {
            Assert.fail(e1.getMessage());
        } catch (JDOMException e) {
            Assert.fail("No serialisation should take place");
        }
        String outputFile = outDir + "Bar.php";
        ComparetorResult expected = ComparetorResult.TRUE;
        try {
            FileComparator comp = new FileComparator(outputFile);
            ComparetorResult actual = comp.equals("fragments/headless_test/Bar6.php");
            Assert.assertEquals(expected, actual);
        } catch (FileNotFoundException e) {
            e.printStackTrace();
            Assert.fail(e.getMessage());
        } catch (IOException e) {
            Assert.fail(e.getMessage());
        }
    }

    /**
	 * Remove all the files that where generated during the test
	 */
    @AfterClass
    public static void shutdown() {
        String outputFile = outDir + "sqls.sql";
        File f = new File(outputFile);
        f.deleteOnExit();
        outputFile = outDir + "serializedSqls.sql.xml";
        f = new File(outputFile);
        f.deleteOnExit();
        outputFile = outDir + "LoginService.wsdl";
        f = new File(outputFile);
        f.deleteOnExit();
        outputFile = outDir + "services.xml";
        f = new File(outputFile);
        f.deleteOnExit();
        outputFile = outDir + "Bar.xml";
        f = new File(outputFile);
        f.deleteOnExit();
        outputFile = outDir + "Bar.php";
        f = new File(outputFile);
        f.deleteOnExit();
        outputFile = outDir + "src" + File.separator + "org" + File.separator + "apache" + File.separator + "axis2" + File.separator + "LoginServiceMessageReceiverInOut.java";
        f = new File(outputFile);
        f.deleteOnExit();
        outputFile = outDir + "src" + File.separator + "org" + File.separator + "apache" + File.separator + "axis2" + File.separator + "LoginServiceStub.java";
        f = new File(outputFile);
        f.deleteOnExit();
    }
}
