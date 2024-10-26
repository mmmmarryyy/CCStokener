package org.fudaa.dodico.crue.io;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.net.URL;
import org.fudaa.ctulu.CtuluLibFile;
import org.fudaa.ctulu.CtuluLog;
import org.fudaa.ctulu.CtuluLogLevel;
import org.fudaa.ctulu.CtuluLogRecord;
import org.fudaa.dodico.crue.common.BusinessMessages;
import org.fudaa.dodico.crue.config.TestCrueConfigMetierLoaderDefault;
import org.fudaa.dodico.crue.io.common.CrueIOResu;
import org.fudaa.dodico.crue.io.log.CrueLOGReaderWriter;

/**
 * @author CANEL Christophe (Genesis)
 *
 */
public class TestCrueLOG extends AbstractIOTestCase {

    protected static final String FICHIER_TEST_XML = "/logs.log.xml";

    public TestCrueLOG() {
        super(FICHIER_TEST_XML);
    }

    public void testLecture() {
        final File logFile = this.getTestFile();
        final CtuluLog log = read(logFile);
        assertCorrect(log);
    }

    private static void assertCorrect(CtuluLog log) {
        assertEquals("Description des logs", log.getDesc());
        final CtuluLogRecord[] logs = log.getRecords().toArray(new CtuluLogRecord[0]);
        assertEquals(4, logs.length);
        assertEquals(CtuluLogLevel.INFO, logs[0].getLevel());
        assertEquals("Ceci est une info", logs[0].getMsg());
        assertEquals(CtuluLogLevel.WARNING, logs[1].getLevel());
        assertEquals("Ceci est un warning", logs[1].getMsg());
        assertEquals(CtuluLogLevel.FATAL, logs[2].getLevel());
        assertEquals("Ceci est une erreur fatale", logs[2].getMsg());
        assertEquals(CtuluLogLevel.ERROR, logs[3].getLevel());
        assertEquals("Ceci est une erreur", logs[3].getMsg());
    }

    public void testEcriture() {
        final File logFile = this.getTestFile();
        File newLogFile = null;
        try {
            newLogFile = File.createTempFile("Test", "CrueLOG");
        } catch (IOException e) {
            e.printStackTrace();
            fail();
        }
        CtuluLog log = read(logFile);
        final CrueLOGReaderWriter writer = new CrueLOGReaderWriter("1.2");
        final CrueIOResu<CtuluLog> resu = new CrueIOResu<CtuluLog>(log);
        log = new CtuluLog(BusinessMessages.RESOURCE_BUNDLE);
        assertTrue(writer.writeXMLMetier(resu, newLogFile, log, TestCrueConfigMetierLoaderDefault.DEFAULT));
        testAnalyser(log);
        log = read(newLogFile);
        assertCorrect(log);
    }

    private File getTestFile() {
        final URL url = TestCrueLOG.class.getResource(FICHIER_TEST_XML);
        final File otfaFile = new File(createTempDir(), "logs.log.xml");
        try {
            CtuluLibFile.copyStream(url.openStream(), new FileOutputStream(otfaFile), true, true);
        } catch (Exception e) {
            e.printStackTrace();
            fail();
        }
        return otfaFile;
    }

    private static CtuluLog read(File logFile) {
        final CrueLOGReaderWriter reader = new CrueLOGReaderWriter("1.2");
        final CtuluLog log = new CtuluLog(BusinessMessages.RESOURCE_BUNDLE);
        final CrueIOResu<CtuluLog> result = reader.readXML(logFile, log, createDefault());
        testAnalyser(log);
        return result.getMetier();
    }

    public void testValide() {
    }
}
