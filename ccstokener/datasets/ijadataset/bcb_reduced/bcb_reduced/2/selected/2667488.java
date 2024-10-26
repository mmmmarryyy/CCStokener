package de.psisystems.dmachinery.jobs;

import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;
import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.net.URL;
import java.util.ArrayList;
import java.util.Enumeration;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Properties;
import java.util.Set;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.junit.Test;
import de.psisystems.dmachinery.BaseTest;
import de.psisystems.dmachinery.core.exeptions.PrintException;
import de.psisystems.dmachinery.io.URLHelper;
import de.psisystems.dmachinery.tasks.PrintJobTransfomHelper;
import de.psisystems.dmachinery.xml.MarshallUtil;
import de.psisystems.dmachinery.xml.Marshallable;

public class SimplePrintJobTest extends BaseTest {

    private static final Log log = LogFactory.getLog(SimplePrintJobTest.class);

    @Test
    public void testConfigurartion() {
        try {
            Enumeration<URL> assemblersToRegister = this.getClass().getClassLoader().getResources("META-INF/PrintAssemblerFactory.properties");
            log.debug("PrintAssemblerFactory " + SimplePrintJobTest.class.getClassLoader().getResource("META-INF/PrintAssemblerFactory.properties"));
            log.debug("ehcache " + SimplePrintJobTest.class.getClassLoader().getResource("ehcache.xml"));
            log.debug("log4j " + this.getClass().getClassLoader().getResource("/log4j.xml"));
            if (log.isDebugEnabled()) {
                while (assemblersToRegister.hasMoreElements()) {
                    URL url = (URL) assemblersToRegister.nextElement();
                    InputStream in = url.openStream();
                    BufferedReader buff = new BufferedReader(new InputStreamReader(in));
                    String line = buff.readLine();
                    while (line != null) {
                        log.debug(line);
                        line = buff.readLine();
                    }
                    buff.close();
                    in.close();
                }
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    @Test
    public void testToXML() {
        Marshallable data = new SimpleTestData("Henry", "Ford", "Elmstreet", "4711", "TheCity");
        Map<String, Object> attributes = new HashMap<String, Object>();
        attributes.put("key1", "value1");
        attributes.put("key2", "value2");
        List<Template> templates = new ArrayList<Template>();
        URL url = getClass().getResource("/de/psisystems/dmachinery/template/samples/pdf/SimplePrintJobTest.pdf");
        assertNotNull(url);
        URL blockurl = getClass().getResource("/de/psisystems/dmachinery/template/samples/blocks/block1.xsl");
        assertNotNull(blockurl);
        Set<Block> blocks = new HashSet<Block>();
        blocks.add(BlockFactory.newBlock("1", "0.1", blockurl));
        Properties properties = new Properties();
        properties.put("prop1", "value1");
        properties.put("prop2", "value2");
        templates.add(TemplateFactory.newTemplate(url, properties, blocks));
        SimplePrintJob simplePrintJob = new SimplePrintJob(attributes, templates, data);
        URL dataDest = URLHelper.createLocalfileURL(getOutputPath() + "/testToXML.xml");
        URL dest = URLHelper.createLocalfileURL(getOutputPath() + "/testToXMLTransfomed.xml");
        try {
            MarshallUtil.toXML(dataDest, simplePrintJob);
        } catch (PrintException e) {
            log.error(e.getMessage());
            fail();
        }
        try {
            PrintJobTransfomHelper.transform(dest, dataDest, null);
        } catch (PrintException e) {
            log.error(e.getMessage());
            fail();
        }
        assertTrue(exist(dataDest));
    }

    @Test
    public void testTestDataToXML() {
        Marshallable data = new SimpleTestData("Henry", "Ford", "Elmstreet", "4711", "TheCity");
        URL dest = URLHelper.createLocalfileURL(getOutputPath() + "/testTestDataToXML.xml");
        try {
            MarshallUtil.toXML(dest, data);
        } catch (PrintException e) {
            log.error(e.getMessage());
            fail();
        }
        assertTrue(exist(dest));
    }
}
