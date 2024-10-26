package net.sf.logdistiller.xml;

import java.io.*;
import java.net.URL;
import java.text.ParseException;
import javax.xml.parsers.*;
import junit.framework.*;
import org.apache.commons.io.IOUtils;
import org.apache.commons.lang.StringUtils;
import org.xml.sax.*;
import net.sf.logdistiller.*;
import net.sf.logdistiller.logtypes.*;

public class DOMConfiguratorTestCase extends TestCase {

    private DOMConfigurator dOMConfigurator = null;

    protected void setUp() throws Exception {
        super.setUp();
        dOMConfigurator = new DOMConfigurator();
    }

    protected void tearDown() throws Exception {
        dOMConfigurator = null;
        super.tearDown();
    }

    protected InputStream getConfStream() {
        InputStream in = DOMConfiguratorTestCase.class.getResourceAsStream("wl-conf.xml");
        assertNotNull("load resource wl-conf.xml", in);
        return in;
    }

    protected void checkLogDistiller(LogDistiller ld) {
        assertEquals("number of groups", 6, ld.getGroups().length);
        assertEquals("number of global reports", 4, ld.getOutput().getReports().length);
        assertEquals("output url", "http://logdistiller.sf.net/logs/wl/interactive", ld.getOutput().getUrl());
        assertEquals("LogDistiller global mail report param to", "user@localhost", ld.getOutput().getReports()[2].getParams().get("to"));
        assertEquals("group unknown, mail report param to", "admin@localhost", ld.getGroups()[5].getReports()[2].getParams().get("to"));
        assertEquals("LogDistiller description properties replacement", "Test of LogDistiller value", ld.getDescription());
        assertEquals("number of categories", 2, ld.getCategories().length);
        assertEquals("group description properties replacement", "Test of properties replacement: value", ld.getGroups()[0].getDescription());
    }

    public void testReadNoDtd() throws Exception {
        InputStream in = getConfStream();
        String conf = IOUtils.toString(in, "UTF-8");
        in.close();
        conf = StringUtils.replace(conf, "<!DOCTYPE", "<!--DOCTYPE");
        conf = StringUtils.replace(conf, ".dtd\">", ".dtd\"-->");
        LogDistiller ld = dOMConfigurator.read(new InputSource(new StringReader(conf)));
        checkLogDistiller(ld);
    }

    private static final String XSD_VERSION = "1.0.0";

    private static final String XSD = "xmlns=\"http://logdistiller.sourceforge.net/LOGDISTILLER/" + XSD_VERSION + "\" " + "xmlns:xsi=\"http://www.w3.org/2001/XMLSchema-instance\" " + "xsi:schemaLocation=\"http://logdistiller.sourceforge.net/LOGDISTILLER/" + XSD_VERSION + " http://logdistiller.sourceforge.net/xsd/logdistiller-" + XSD_VERSION + ".xsd\"";

    public void testReadXsd() throws Exception {
        InputStream in = getConfStream();
        String conf = IOUtils.toString(in, "UTF-8");
        conf = StringUtils.replace(conf, "<!DOCTYPE", "<!--DOCTYPE");
        conf = StringUtils.replace(conf, ".dtd\">", ".dtd\"-->");
        conf = StringUtils.replace(conf, "<logdistiller", "<logdistiller " + XSD);
        LogDistiller ld = dOMConfigurator.read(new InputSource(new StringReader(conf)));
        checkLogDistiller(ld);
    }

    public void testRead() throws ParserConfigurationException, SAXException, ParseException, IOException {
        InputStream in = getConfStream();
        LogDistiller ld = dOMConfigurator.read(in);
        in.close();
        checkLogDistiller(ld);
        File tmp = File.createTempFile("logdistiller", "test");
        tmp.delete();
        tmp.mkdir();
        URL url = WeblogicLogEvent.class.getResource("wldomain7.log");
        in = url.openStream();
        assertNotNull("load resource wldomain7.log", in);
        Reader reader = new InputStreamReader(in);
        ld.getOutput().setDirectory(tmp.getAbsolutePath());
        LogDistillation exec = new LogDistillation(ld);
        LogEvent.Factory factory = exec.getLogTypeDescription().newFactory(reader, url.toString());
        exec.begin();
        LogEvent le;
        while ((le = factory.nextEvent()) != null) {
            exec.processLogEvent(le);
        }
        exec.end();
        in.close();
        assertEquals("number of logevents processed", 21, exec.getEventCount());
        final int[] groupEventCount = { 6, 6, 1, 4, 9, 7 };
        for (int i = 0; i < 6; i++) {
            LogDistillation.Group g = exec.getGroups()[i];
            LogDistiller.Group def = g.getDefinition();
            assertEquals("number of logevents in group[id='" + def.getId() + "']", groupEventCount[i], g.getEventCount());
        }
    }
}
