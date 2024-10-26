package ch.sahits.codegen.java.wizards;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;
import java.net.MalformedURLException;
import java.net.URL;
import java.util.Properties;
import junit.framework.Assert;
import org.eclipse.core.runtime.CoreException;
import org.jdom.JDOMException;
import org.junit.AfterClass;
import org.junit.Before;
import org.junit.Test;
import ch.sahits.codegen.core.util.Debugging;
import ch.sahits.codegen.test.PropertyFileLoader;
import ch.sahits.codegen.util.ERunAsType;
import ch.sahits.codegen.util.RunAs;
import ch.sahits.test.ComparetorResult;
import ch.sahits.test.FileComparator;

@RunAs(ERunAsType.PLUGIN)
public class HeadlessJavaGeneratorTest extends PropertyFileLoader {

    private static String tempdir;

    private String oracleInputFile;

    private String objectBeanInputFile;

    private String xhtml1ColInptFile;

    private String xhtml2ColInptFile;

    private String wsSQLInput;

    private String wsXMLInput;

    private String csvInputFile;

    private String sqlserverInputfile;

    private String xmlInputFile;

    private static String outDir;

    private static String workspace = "";

    private String[] help;

    private String[] jetemplate;

    private String[] jetemplateASTImp;

    private String[] generateJetemplate;

    private String[] generateAST;

    private String[] generateJetemplateASTRef;

    private String[] serialize;

    private String[] deserializeJetemplate;

    private String[] deserializeJetemplateDBConnection;

    private String[] deserializeJetemplateASTImp;

    private String[] deserializeGenerateJetemplate;

    private String[] deserializeGenerateAST;

    private String[] deserializeGenerateJetemplateASTRef;

    private String[] objectBean;

    private String[] xhtml1Col;

    private String[] xhtml2Col;

    private String[] wsServiceInterface;

    private String[] csvBased;

    private String[] sqlServerParsing;

    private String[] xmlParsing;

    private String[] jetemplateMySQLConnection;

    private String[] jetemplateOracleConnection;

    private String[] daoUniqueLoad;

    public HeadlessJavaGeneratorTest() {
        Properties prop = loadFile("fragments/headlessgeneral.properties");
        tempdir = prop.getProperty("tempdir");
        outDir = tempdir + File.separator + "src";
        oracleInputFile = prop.getProperty("inputfile");
        objectBeanInputFile = prop.getProperty("inputfile2");
        xhtml1ColInptFile = prop.getProperty("inputfile3");
        xhtml2ColInptFile = prop.getProperty("inputfile4");
        wsSQLInput = prop.getProperty("wsSQL");
        wsXMLInput = prop.getProperty("wsXML");
        csvInputFile = prop.getProperty("inputfile5");
        sqlserverInputfile = prop.getProperty("inputfile6");
        xmlInputFile = prop.getProperty("inputfile7");
        workspace = prop.getProperty("workspace");
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

    @Before
    public void setUp() throws Exception {
        help = new String[] { "help" };
        jetemplate = new String[] { "-src", outDir, "-pk", "ch.sahits", "-cl", "Foo", "-mod", "PUB", "-m", "JT", "-i", "WDB", "-f", oracleInputFile, "-j", "ch.sahits.codegen.java@jet_templates/dbbean.javajet", "-p", "ORACLE" };
        jetemplateASTImp = new String[] { "-src", outDir, "-pk", "ch.sahits", "-cl", "Foo", "-mod", "PUB", "-m", "JTA", "-i", "WDB", "-f", oracleInputFile, "-j", "ch.sahits.codegen.java@jet_templates/dbbean.javajet", "-g", "ch.sahits.codegen.java@ch.sahits.codegen.java.generator.ast.JETASTGenerator", "-p", "ORACLE" };
        generateJetemplate = new String[] { "-src", outDir, "-pk", "ch.sahits", "-cl", "Foo", "-mod", "PUB", "-m", "GT", "-i", "WDB", "-f", oracleInputFile, "-g", "ch.sahits.codegen.java@ch.sahits.codegen.java.generator.jettemplate.InitializableDbBeanGenerator", "-p", "ORACLE" };
        generateAST = new String[] { "-src", outDir, "-pk", "ch.sahits", "-cl", "Foo", "-mod", "PUB", "-m", "GA", "-i", "WDB", "-f", oracleInputFile, "-g", "ch.sahits.codegen.java@ch.sahits.codegen.java.generator.ast.PureASTOracleDAOGenerator", "-p", "ORACLE", "-dbn", "name", "-dbh", "localhost", "-dbp", "1521", "-dbu", "sys", "-dbpass", "good", "-dbt", "person", "-dbs", "HR" };
        generateJetemplateASTRef = new String[] { "-src", outDir, "-pk", "ch.sahits", "-cl", "Foo", "-mod", "PUB", "-m", "GTA", "-i", "WDB", "-f", oracleInputFile, "-g", "ch.sahits.codegen.java@ch.sahits.codegen.java.generator.ast.ASTBeanWithDelete", "-r", "ch.sahits.codegen.java@ch.sahits.codegen.java.generator.ast.CRUDOracleReference", "-p", "ORACLE", "-dbn", "name", "-dbh", "localhost", "-dbp", "1521", "-dbu", "sys", "-dbpass", "good", "-dbt", "person", "-dbs", "HR" };
        deserializeJetemplate = new String[] { "-l", "fragments/headless_test/serializedFoo1.xml" };
        deserializeJetemplateASTImp = new String[] { "-l", "fragments/headless_test/serializedFoo2.xml" };
        deserializeGenerateJetemplate = new String[] { "-l", "fragments/headless_test/serializedFoo3.xml" };
        deserializeGenerateAST = new String[] { "-l", "fragments/headless_test/serializedFoo4.xml" };
        deserializeGenerateJetemplateASTRef = new String[] { "-l", "fragments/headless_test/serializedFoo5.xml" };
        objectBean = new String[] { "-src", outDir, "-pk", "ch.sahits", "-cl", "Taz", "-mod", "PUB", "-m", "JT", "-i", "WODB", "-f", objectBeanInputFile, "-j", "ch.sahits.codegen.java@jet_templates/dbbean.javajet" };
        xhtml1Col = new String[] { "-src", outDir, "-pk", "ch.sahits", "-cl", "Taz", "-mod", "PUB", "-m", "JT", "-i", "WODB", "-f", xhtml1ColInptFile, "-j", "ch.sahits.codegen.java.gui@jet_templates/swtgui.javajet" };
        xhtml2Col = new String[] { "-src", outDir, "-pk", "ch.sahits", "-cl", "Taz", "-mod", "PUB", "-m", "JT", "-i", "WODB", "-f", xhtml2ColInptFile, "-j", "ch.sahits.codegen.java.gui@jet_templates/swtgui.javajet" };
        wsServiceInterface = new String[] { "-src", outDir, "-pk", "ch.sahits", "-cl", "Taz", "-mod", "PUB", "-m", "JT", "-i", "WDB", "-f", wsSQLInput, "-j", "ch.sahits.codegen.java.examples@jet_templates/loginServiceInterface.javajet", "-p", "MYSQL" };
        csvBased = new String[] { "-src", outDir, "-pk", "ch.sahits", "-cl", "Taz", "-mod", "PUB", "-m", "JT", "-i", "WODB", "-f", csvInputFile, "-j", "ch.sahits.codegen.java@jet_templates/dbbean.javajet" };
        sqlServerParsing = new String[] { "-src", outDir, "-pk", "ch.sahits", "-cl", "Taz", "-mod", "PUB", "-m", "JT", "-i", "WDB", "-f", sqlserverInputfile, "-j", "ch.sahits.codegen.java@jet_templates/dbbean.javajet", "-p", "SQLSERVER" };
        xmlParsing = new String[] { "-src", outDir, "-pk", "ch.sahits", "-cl", "Taz", "-mod", "PUB", "-m", "JT", "-i", "WODB", "-f", xmlInputFile, "-j", "ch.sahits.codegen.java@jet_templates/dbbean.javajet" };
        deserializeJetemplateDBConnection = new String[] { "-l", "fragments/headless_test/serializedJetemplateMySQLConnection.xml" };
        jetemplateMySQLConnection = new String[] { "-src", outDir, "-pk", "ch.sahits", "-cl", "Bar", "-mod", "PUB", "-m", "JT", "-i", "DBC", "-j", "ch.sahits.codegen.java@jet_templates/dbbean.javajet", "-p", "MYSQL", "-dbn", "cdcol", "-dbh", "localhost", "-dbp", "3306", "-dbu", "root", "-dbpass", "", "-dbt", "cds", "-dbs", "HR" };
        jetemplateOracleConnection = new String[] { "-src", outDir, "-pk", "ch.sahits", "-cl", "Bar", "-mod", "PUB", "-m", "JT", "-i", "DBC", "-j", "ch.sahits.codegen.java@jet_templates/dbbean.javajet", "-p", "ORACLE", "-dbn", "HRM", "-dbh", "localhost", "-dbp", "1521", "-dbu", "HRM", "-dbpass", "", "-dbt", "CODE_TAB", "-dbs", "HRM" };
        daoUniqueLoad = new String[] { "-src", outDir, "-pk", "ch.sahits", "-cl", "Bar", "-mod", "PUB", "-inter", "ch.sahits.ws.ILoginWebservice", "-m", "JTA", "-i", "WDB", "-f", wsSQLInput, "-j", "ch.sahits.codegen.java.examples@jet_templates/daoInsertUnique.javajet", "-g", "ch.sahits.codegen.java.examples@ch.sahits.codegen.example.generator.ast.DAOGenerator", "-p", "MYSQL", "-dbn", "name", "-dbh", "localhost", "-dbp", "3306", "-dbu", "root", "-dbpass", "god", "-dbt", "person", "-dbs", "HR" };
    }

    /**
	 * Test the print out on the console. Visual verification
	 */
    public void testPrintUsage() {
        try {
            new HeadlessJavaGenerator().run(help);
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
	 * Test the generation of the db bean with getter and setter
	 */
    @Test
    public void testGenerationJETemplate() {
        try {
            new HeadlessJavaGenerator().run(jetemplate);
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
        String outputFile = workspace + outDir + File.separator + "ch" + File.separator + "sahits" + File.separator + "Foo.java";
        ComparetorResult expected = ComparetorResult.TRUE;
        try {
            FileComparator comp = new FileComparator(outputFile);
            ComparetorResult actual = comp.equals("fragments/headless_test/Foo1.java");
            Assert.assertEquals(expected, actual);
        } catch (FileNotFoundException e) {
            e.printStackTrace();
            Assert.fail(e.getMessage());
        } catch (IOException e) {
            Assert.fail(e.getMessage());
        }
    }

    /**
	 * Test the generation of the db bean with getter and setter
	 */
    @Test
    public void testGenerationJETemplateASTImp() {
        try {
            new HeadlessJavaGenerator().run(jetemplateASTImp);
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
        String outputFile = workspace + outDir + File.separator + "ch" + File.separator + "sahits" + File.separator + "Foo.java";
        ComparetorResult expected = ComparetorResult.TRUE;
        try {
            FileComparator comp = new FileComparator(outputFile);
            ComparetorResult actual = comp.equals("fragments/headless_test/Foo2.java");
            Assert.assertEquals(expected, actual);
        } catch (FileNotFoundException e) {
            e.printStackTrace();
            Assert.fail(e.getMessage());
        } catch (IOException e) {
            Assert.fail(e.getMessage());
        }
    }

    /**
	 * Test the generation of the db bean with getter and setter
	 */
    @Test
    public void testGenerationGenerateJET() {
        try {
            new HeadlessJavaGenerator().run(generateJetemplate);
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
        String outputFile = workspace + outDir + File.separator + "ch" + File.separator + "sahits" + File.separator + "Foo.java";
        ComparetorResult expected = ComparetorResult.TRUE;
        try {
            FileComparator comp = new FileComparator(outputFile);
            ComparetorResult actual = comp.equals("fragments/headless_test/Foo3.java");
            Assert.assertEquals(expected, actual);
        } catch (FileNotFoundException e) {
            e.printStackTrace();
            Assert.fail(e.getMessage());
        } catch (IOException e) {
            Assert.fail(e.getMessage());
        }
    }

    /**
	 * Test the generation of the db bean with getter and setter
	 */
    @Test
    public void testGenerationGenerateAST() {
        try {
            new HeadlessJavaGenerator().run(generateAST);
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
        String outputFile = workspace + outDir + File.separator + "ch" + File.separator + "sahits" + File.separator + "Foo.java";
        ComparetorResult expected = ComparetorResult.TRUE;
        try {
            FileComparator comp = new FileComparator(outputFile);
            ComparetorResult actual = comp.equals("fragments/headless_test/Foo4.java");
            Assert.assertEquals(expected, actual);
        } catch (FileNotFoundException e) {
            e.printStackTrace();
            Assert.fail(e.getMessage());
        } catch (IOException e) {
            Assert.fail(e.getMessage());
        }
    }

    /**
	 * Test the generation of the db bean with getter and setter
	 */
    @Test
    public void testGenerationGenerateJetemplateASTRefImpl() {
        Debugging.setForceOverride(true);
        try {
            new HeadlessJavaGenerator().run(generateJetemplateASTRef);
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
        String outputFile = workspace + outDir + File.separator + "ch" + File.separator + "sahits" + File.separator + "Foo.java";
        ComparetorResult expected = ComparetorResult.TRUE;
        try {
            FileComparator comp = new FileComparator(outputFile);
            ComparetorResult actual = comp.equals("fragments/headless_test/Foo5.java");
            Assert.assertEquals(expected, actual);
        } catch (FileNotFoundException e) {
            e.printStackTrace();
            Assert.fail(e.getMessage());
        } catch (IOException e) {
            Assert.fail(e.getMessage());
        }
    }

    /**
	 * Test the generation of a Foo.java from deserializing a configuration
	 */
    @Test
    public void testDeSerializeJetemplate() {
        try {
            new HeadlessJavaGenerator().run(deserializeJetemplate);
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
        String outputFile = workspace + outDir + File.separator + "ch" + File.separator + "sahits" + File.separator + "Foo.java";
        ComparetorResult expected = ComparetorResult.TRUE;
        try {
            FileComparator comp = new FileComparator(outputFile);
            ComparetorResult actual = comp.equals("fragments/headless_test/Foo1.java");
            Assert.assertEquals(expected, actual);
        } catch (FileNotFoundException e) {
            e.printStackTrace();
            Assert.fail(e.getMessage());
        } catch (IOException e) {
            Assert.fail(e.getMessage());
        }
    }

    /**
	 * Test the generation of a Foo.java from deserializing a configuration
	 */
    @Test
    public void testDeSerializeJetemplateAST() {
        try {
            new HeadlessJavaGenerator().run(deserializeJetemplateASTImp);
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
        String outputFile = workspace + outDir + File.separator + "ch" + File.separator + "sahits" + File.separator + "Foo.java";
        ComparetorResult expected = ComparetorResult.TRUE;
        try {
            FileComparator comp = new FileComparator(outputFile);
            ComparetorResult actual = comp.equals("fragments/headless_test/Foo2.java");
            Assert.assertEquals(expected, actual);
        } catch (FileNotFoundException e) {
            e.printStackTrace();
            Assert.fail(e.getMessage());
        } catch (IOException e) {
            Assert.fail(e.getMessage());
        }
    }

    /**
	 * Test the generation of a Foo.java from deserializing a configuration
	 */
    @Test
    public void testDeSerializeGenerateJetemplate() {
        try {
            new HeadlessJavaGenerator().run(deserializeGenerateJetemplate);
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
        String outputFile = workspace + outDir + File.separator + "ch" + File.separator + "sahits" + File.separator + "Foo.java";
        ComparetorResult expected = ComparetorResult.TRUE;
        try {
            FileComparator comp = new FileComparator(outputFile);
            ComparetorResult actual = comp.equals("fragments/headless_test/Foo3.java");
            Assert.assertEquals(expected, actual);
        } catch (FileNotFoundException e) {
            e.printStackTrace();
            Assert.fail(e.getMessage());
        } catch (IOException e) {
            Assert.fail(e.getMessage());
        }
    }

    /**
	 * Test the generation of a Foo.java from deserializing a configuration
	 */
    @Test
    public void testDeSerializeGenerateAST() {
        try {
            new HeadlessJavaGenerator().run(deserializeGenerateAST);
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
        String outputFile = workspace + outDir + File.separator + "ch" + File.separator + "sahits" + File.separator + "Foo.java";
        ComparetorResult expected = ComparetorResult.TRUE;
        try {
            FileComparator comp = new FileComparator(outputFile);
            ComparetorResult actual = comp.equals("fragments/headless_test/Foo4.java");
            Assert.assertEquals(expected, actual);
        } catch (FileNotFoundException e) {
            e.printStackTrace();
            Assert.fail(e.getMessage());
        } catch (IOException e) {
            Assert.fail(e.getMessage());
        }
    }

    /**
	 * Test the generation of a Foo.java from deserializing a configuration
	 */
    @Test
    public void testDeSerializeGenerateJetemplateASTRef() {
        try {
            new HeadlessJavaGenerator().run(deserializeGenerateJetemplateASTRef);
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
        String outputFile = workspace + outDir + File.separator + "ch" + File.separator + "sahits" + File.separator + "Foo.java";
        ComparetorResult expected = ComparetorResult.TRUE;
        try {
            FileComparator comp = new FileComparator(outputFile);
            ComparetorResult actual = comp.equals("fragments/headless_test/Foo5.java");
            Assert.assertEquals(expected, actual);
        } catch (FileNotFoundException e) {
            e.printStackTrace();
            Assert.fail(e.getMessage());
        } catch (IOException e) {
            Assert.fail(e.getMessage());
        }
    }

    /**
	 * Test the generation of the db bean with getter and setter
	 */
    @Test
    public void testGenerationObjectBean() {
        try {
            new HeadlessJavaGenerator().run(objectBean);
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
        String outputFile = workspace + outDir + File.separator + "ch" + File.separator + "sahits" + File.separator + "Taz.java";
        ComparetorResult expected = ComparetorResult.TRUE;
        try {
            FileComparator comp = new FileComparator(outputFile);
            ComparetorResult actual = comp.equals("fragments/headless_test/Taz1.java");
            Assert.assertEquals(expected, actual);
        } catch (FileNotFoundException e) {
            e.printStackTrace();
            Assert.fail(e.getMessage());
        } catch (IOException e) {
            Assert.fail(e.getMessage());
        }
    }

    /**
	 * Test the generation of the db bean with getter and setter
	 */
    @Test
    public void testGenerationSWTCol1() {
        try {
            new HeadlessJavaGenerator().run(xhtml1Col);
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
        String outputFile = workspace + outDir + File.separator + "ch" + File.separator + "sahits" + File.separator + "Taz.java";
        ComparetorResult expected = ComparetorResult.TRUE;
        try {
            FileComparator comp = new FileComparator(outputFile);
            ComparetorResult actual = comp.equals("fragments/headless_test/Taz2.java");
            Assert.assertEquals(expected, actual);
        } catch (FileNotFoundException e) {
            e.printStackTrace();
            Assert.fail(e.getMessage());
        } catch (IOException e) {
            Assert.fail(e.getMessage());
        }
    }

    @Test
    public void testGenerationSWTCol2() {
        try {
            new HeadlessJavaGenerator().run(xhtml2Col);
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
        String outputFile = workspace + outDir + File.separator + "ch" + File.separator + "sahits" + File.separator + "Taz.java";
        ComparetorResult expected = ComparetorResult.TRUE;
        try {
            FileComparator comp = new FileComparator(outputFile);
            ComparetorResult actual = comp.equals("fragments/headless_test/Taz3.java");
            Assert.assertEquals(expected, actual);
        } catch (FileNotFoundException e) {
            e.printStackTrace();
            Assert.fail(e.getMessage());
        } catch (IOException e) {
            Assert.fail(e.getMessage());
        }
    }

    @Test
    public void testGenerationWSInterface() {
        try {
            new HeadlessJavaGenerator().run(wsServiceInterface);
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
        String outputFile = workspace + outDir + File.separator + "ch" + File.separator + "sahits" + File.separator + "Taz.java";
        ComparetorResult expected = ComparetorResult.TRUE;
        try {
            FileComparator comp = new FileComparator(outputFile);
            ComparetorResult actual = comp.equals("fragments/headless_test/Taz4.java");
            Assert.assertEquals(expected, actual);
        } catch (FileNotFoundException e) {
            e.printStackTrace();
            Assert.fail(e.getMessage());
        } catch (IOException e) {
            Assert.fail(e.getMessage());
        }
    }

    @Test
    public void testGenerationCSVParsing() {
        try {
            new HeadlessJavaGenerator().run(csvBased);
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
        String outputFile = workspace + outDir + File.separator + "ch" + File.separator + "sahits" + File.separator + "Taz.java";
        ComparetorResult expected = ComparetorResult.TRUE;
        try {
            FileComparator comp = new FileComparator(outputFile);
            ComparetorResult actual = comp.equals("fragments/headless_test/Taz5.java");
            Assert.assertEquals(expected, actual);
        } catch (FileNotFoundException e) {
            e.printStackTrace();
            Assert.fail(e.getMessage());
        } catch (IOException e) {
            Assert.fail(e.getMessage());
        }
    }

    @Test
    public void testGenerationSQLServerParsing() {
        try {
            new HeadlessJavaGenerator().run(sqlServerParsing);
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
        String outputFile = workspace + outDir + File.separator + "ch" + File.separator + "sahits" + File.separator + "Taz.java";
        ComparetorResult expected = ComparetorResult.TRUE;
        try {
            FileComparator comp = new FileComparator(outputFile);
            ComparetorResult actual = comp.equals("fragments/headless_test/Taz6.java");
            Assert.assertEquals(expected, actual);
        } catch (FileNotFoundException e) {
            e.printStackTrace();
            Assert.fail(e.getMessage());
        } catch (IOException e) {
            Assert.fail(e.getMessage());
        }
    }

    @Test
    public void testGenerationXMLParsing() {
        try {
            new HeadlessJavaGenerator().run(xmlParsing);
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
        String outputFile = workspace + outDir + File.separator + "ch" + File.separator + "sahits" + File.separator + "Taz.java";
        ComparetorResult expected = ComparetorResult.TRUE;
        try {
            FileComparator comp = new FileComparator(outputFile);
            ComparetorResult actual = comp.equals("fragments/headless_test/Taz7.java");
            Assert.assertEquals(expected, actual);
        } catch (FileNotFoundException e) {
            e.printStackTrace();
            Assert.fail(e.getMessage());
        } catch (IOException e) {
            Assert.fail(e.getMessage());
        }
    }

    /**
	 * Test the generation of the db bean with getter and setter
	 */
    @Test
    public void testGenerationDeSerialzeJetemplateMySQLConnection() {
        try {
            new HeadlessJavaGenerator().run(deserializeJetemplateDBConnection);
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
        } catch (RuntimeException e) {
            Assert.fail("The connection could probably not be established: Check the database and password in the serialized file");
        }
        String outputFile = workspace + outDir + File.separator + "ch" + File.separator + "sahits" + File.separator + "Taz.java";
        ComparetorResult expected = ComparetorResult.TRUE;
        try {
            FileComparator comp = new FileComparator(outputFile);
            ComparetorResult actual = comp.equals("fragments/headless_test/Taz9.java");
            Assert.assertEquals(expected, actual);
        } catch (FileNotFoundException e) {
            e.printStackTrace();
            Assert.fail(e.getMessage());
        } catch (IOException e) {
            Assert.fail(e.getMessage());
        }
    }

    /**
	 * Test the generation of the db bean with getter and setter
	 */
    @Test
    public void testGenerationJetemplateMySQLConnection() {
        try {
            new HeadlessJavaGenerator().run(jetemplateMySQLConnection);
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
        } catch (RuntimeException e) {
            Assert.fail("The connection could probably not be established: Check the database and password in the serialized file");
        }
        String outputFile = workspace + outDir + File.separator + "ch" + File.separator + "sahits" + File.separator + "Bar.java";
        ComparetorResult expected = ComparetorResult.TRUE;
        try {
            FileComparator comp = new FileComparator(outputFile);
            ComparetorResult actual = comp.equals("fragments/headless_test/Bar3.java");
            Assert.assertEquals(expected, actual);
        } catch (FileNotFoundException e) {
            e.printStackTrace();
            Assert.fail(e.getMessage());
        } catch (IOException e) {
            Assert.fail(e.getMessage());
        }
    }

    /**
	 * Test the generation of the db bean with getter and setter
	 */
    @Test
    public void testGenerationJetemplateOracleConnection() {
        try {
            new HeadlessJavaGenerator().run(jetemplateOracleConnection);
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
        } catch (RuntimeException e) {
            Assert.fail("The connection could probably not be established: Check the database and password in the serialized file");
        }
        String outputFile = workspace + outDir + File.separator + "ch" + File.separator + "sahits" + File.separator + "Bar.java";
        ComparetorResult expected = ComparetorResult.TRUE;
        try {
            FileComparator comp = new FileComparator(outputFile);
            ComparetorResult actual = comp.equals("fragments/headless_test/Bar4.java");
            Assert.assertEquals(expected, actual);
        } catch (FileNotFoundException e) {
            e.printStackTrace();
            Assert.fail(e.getMessage());
        } catch (IOException e) {
            Assert.fail(e.getMessage());
        }
    }

    /**
	 * Test the generation of the db bean with getter and setter
	 */
    @Test
    public void testGenerationDAOUniqueLoad() {
        try {
            new HeadlessJavaGenerator().run(daoUniqueLoad);
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
        } catch (RuntimeException e) {
            Assert.fail("The connection could probably not be established: Check the database and password in the serialized file");
        }
        String outputFile = workspace + outDir + File.separator + "ch" + File.separator + "sahits" + File.separator + "Bar.java";
        ComparetorResult expected = ComparetorResult.TRUE;
        try {
            FileComparator comp = new FileComparator(outputFile);
            ComparetorResult actual = comp.equals("fragments/headless_test/Bar5.java");
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
        String outputFile = workspace + outDir + File.separator + "ch" + File.separator + "sahits" + File.separator + "Foo.java";
        File f = new File(outputFile);
        f.deleteOnExit();
        outputFile = workspace + outDir + File.separator + "ch" + File.separator + "sahits" + File.separator + "Taz.java";
        f = new File(outputFile);
        f.deleteOnExit();
        outputFile = workspace + outDir + File.separator + "ch" + File.separator + "sahits" + File.separator + "Bar.java";
        f = new File(outputFile);
        f.deleteOnExit();
    }
}
