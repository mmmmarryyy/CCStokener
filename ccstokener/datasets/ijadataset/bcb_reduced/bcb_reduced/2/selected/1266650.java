package org.fudaa.dodico.crue.io;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.net.URL;
import java.util.ArrayList;
import java.util.List;
import org.fudaa.ctulu.CtuluLibFile;
import org.fudaa.ctulu.CtuluLog;
import org.fudaa.ctulu.CtuluLogGroup;
import org.fudaa.ctulu.CtuluLogLevel;
import org.fudaa.ctulu.CtuluLogRecord;
import org.fudaa.dodico.crue.common.BusinessMessages;
import org.fudaa.dodico.crue.config.TestCrueConfigMetierLoaderDefault;
import org.fudaa.dodico.crue.io.common.CrueIOResu;
import org.fudaa.dodico.crue.io.ctfa.CrueCTFAReaderWriter;
import org.fudaa.dodico.crue.projet.otfa.OtfaCampagneItem;
import org.fudaa.dodico.crue.projet.otfa.OtfaCampagneLine;
import org.fudaa.dodico.crue.projet.otfa.OtfaCampagneLineResult;
import org.fudaa.dodico.crue.projet.otfa.OtfaCampagneResult;
import org.fudaa.dodico.crue.projet.otfa.OtfaCampagneResultHeader;
import org.joda.time.LocalDateTime;

/**
 * @author CANEL Christophe (Genesis)
 */
public class TestCrueCTFA extends AbstractIOTestCase {

    protected static final String FICHIER_TEST_XML = "/otfa/campagne.ctfa.xml";

    public TestCrueCTFA() {
        super(FICHIER_TEST_XML);
    }

    public void testLecture() {
        final OtfaCampagneResult logsPart = read(this.getTestFile());
        assertCorrect(logsPart);
    }

    public static void assertCorrect(OtfaCampagneResult logsPart) {
        final List<OtfaCampagneLineResult> lines = logsPart.getResults();
        assertHeaderCorrect(logsPart.getHeader());
        assertEquals(2, lines.size());
        assertCorrectLogPartLine1(lines.get(0));
        assertCorrectLogPartLine2(lines.get(1));
    }

    public static void assertHeaderCorrect(OtfaCampagneResultHeader header) {
        assertEquals("Une header.", header.getCommentaire());
        assertEquals(new LocalDateTime(2011, 11, 24, 9, 25), header.getDate());
        assertCorrectLogGroup(header.getGlobalValidation());
    }

    public static void assertCorrectLogPartLine1(OtfaCampagneLineResult line) {
        assertCorrectLine1(line.getInitialLine());
        assertCorrectLogGroup(line.getLogs());
    }

    public static void assertCorrectLogPartLine2(OtfaCampagneLineResult line) {
        assertCorrectLine2(line.getInitialLine());
        assertCorrectLogGroup(line.getLogs());
    }

    public static void assertCorrectLine1(OtfaCampagneLine line) {
        assertEquals(1, line.getIndice());
        assertEquals("Line 1", line.getCommentaire());
        assertCorrectReference(line.getReference());
        assertCorrectCible(line.getCible());
    }

    public static void assertCorrectLine2(OtfaCampagneLine line) {
        assertEquals(2, line.getIndice());
        assertEquals("Line 2", line.getCommentaire());
        assertCorrectReference(line.getReference());
        assertCorrectCible(line.getCible());
    }

    public static void assertCorrectReference(OtfaCampagneItem ref) {
        assertEquals("refFile1.etu.xml", ref.getEtuPath());
        assertEquals("refScenario1", ref.getScenarioNom());
        assertEquals("refCoeur1", ref.getCoeurName());
        assertEquals(true, ref.isLaunchCompute());
    }

    public static void assertCorrectCible(OtfaCampagneItem cible) {
        assertEquals("cibleFile1.etu.xml", cible.getEtuPath());
        assertEquals("cibleScenario1", cible.getScenarioNom());
        assertEquals("cibleCoeur1", cible.getCoeurName());
        assertEquals(false, cible.isLaunchCompute());
    }

    public static void assertCorrectLogGroup(CtuluLogGroup group) {
        final List<CtuluLogGroup> groups = group.getGroups();
        assertCorrectLogGroupSimple(group);
        assertEquals(2, groups.size());
        assertCorrectLogGroupSimple(groups.get(0));
        assertCorrectLogGroupSimple(groups.get(1));
    }

    public static void assertCorrectLogGroupSimple(CtuluLogGroup group) {
        final List<CtuluLog> logs = group.getLogs();
        assertCorrectMainLog(group.getMainLog());
        assertEquals(2, logs.size());
        assertCorrectLog1(logs.get(0));
        assertCorrectLog2(logs.get(1));
    }

    public static void assertCorrectMainLog(CtuluLog log) {
        final CtuluLogRecord[] logs = log.getRecords().toArray(new CtuluLogRecord[0]);
        assertEquals("Main log", log.getDesc());
        assertEquals(2, logs.length);
        assertEquals(CtuluLogLevel.ERROR, logs[0].getLevel());
        assertEquals("Main log error 1.", logs[0].getMsg());
        assertEquals(CtuluLogLevel.ERROR, logs[1].getLevel());
        assertEquals("Main log error 2.", logs[1].getMsg());
    }

    public static void assertCorrectLog1(CtuluLog log) {
        final CtuluLogRecord[] logs = log.getRecords().toArray(new CtuluLogRecord[0]);
        assertEquals("Log 1", log.getDesc());
        assertEquals(2, logs.length);
        assertEquals(CtuluLogLevel.ERROR, logs[0].getLevel());
        assertEquals("Log1 error 1.", logs[0].getMsg());
        assertEquals(CtuluLogLevel.ERROR, logs[1].getLevel());
        assertEquals("Log1 error 2.", logs[1].getMsg());
    }

    public static void assertCorrectLog2(CtuluLog log) {
        final CtuluLogRecord[] logs = log.getRecords().toArray(new CtuluLogRecord[0]);
        assertEquals("Log 2", log.getDesc());
        assertEquals(2, logs.length);
        assertEquals(CtuluLogLevel.ERROR, logs[0].getLevel());
        assertEquals("Log2 error 1.", logs[0].getMsg());
        assertEquals(CtuluLogLevel.ERROR, logs[1].getLevel());
        assertEquals("Log2 error 2.", logs[1].getMsg());
    }

    public void testEcriture() {
        final File ctfaFile = this.getTestFile();
        File newCtfaFile = null;
        try {
            newCtfaFile = File.createTempFile("Test", "CrueCTFA");
        } catch (IOException e) {
            e.printStackTrace();
            fail();
        }
        OtfaCampagneResult logsPart = read(ctfaFile);
        final CrueCTFAReaderWriter writer = new CrueCTFAReaderWriter("1.2");
        final CrueIOResu<OtfaCampagneResult> resu = new CrueIOResu<OtfaCampagneResult>(logsPart);
        final CtuluLog log = new CtuluLog(BusinessMessages.RESOURCE_BUNDLE);
        assertTrue(writer.writeXMLMetier(resu, newCtfaFile, log, TestCrueConfigMetierLoaderDefault.DEFAULT));
        testAnalyser(log);
        logsPart = read(newCtfaFile);
        assertCorrect(logsPart);
    }

    private File getTestFile() {
        final URL url = TestCrueLOG.class.getResource(FICHIER_TEST_XML);
        final File ctfaFile = new File(createTempDir(), "reaultat.ctfa.xml");
        try {
            CtuluLibFile.copyStream(url.openStream(), new FileOutputStream(ctfaFile), true, true);
        } catch (Exception e) {
            e.printStackTrace();
            fail();
        }
        return ctfaFile;
    }

    private static OtfaCampagneResult read(File ctfaFile) {
        final CrueCTFAReaderWriter reader = new CrueCTFAReaderWriter("1.2");
        final CtuluLog log = new CtuluLog(BusinessMessages.RESOURCE_BUNDLE);
        final CrueIOResu<OtfaCampagneResult> result = reader.readXML(ctfaFile, log, createDefault());
        testAnalyser(log);
        return result.getMetier();
    }

    public void testValide() {
    }

    public static OtfaCampagneResult createResultLogsPart() {
        final OtfaCampagneResult part = new OtfaCampagneResult();
        part.setHeader(createHeaderPart());
        part.setResults(createResultLogsPartLines());
        return part;
    }

    public static OtfaCampagneResultHeader createHeaderPart() {
        final OtfaCampagneResultHeader header = new OtfaCampagneResultHeader();
        header.setCommentaire("Une header.");
        header.setDate(new LocalDateTime(2011, 11, 24, 9, 25));
        header.setGlobalValidation(createLogGroup());
        return header;
    }

    public static List<OtfaCampagneLineResult> createResultLogsPartLines() {
        final List<OtfaCampagneLineResult> lines = new ArrayList<OtfaCampagneLineResult>();
        lines.add(createResultLogsPartLine1());
        lines.add(createResultLogsPartLine2());
        return lines;
    }

    public static OtfaCampagneLineResult createResultLogsPartLine1() {
        final OtfaCampagneLineResult line = new OtfaCampagneLineResult();
        line.setInitialLine(createCampagneLine1());
        line.setLogs(createLogGroup());
        return line;
    }

    public static OtfaCampagneLineResult createResultLogsPartLine2() {
        final OtfaCampagneLineResult line = new OtfaCampagneLineResult();
        line.setInitialLine(createCampagneLine2());
        line.setLogs(createLogGroup());
        return line;
    }

    public static OtfaCampagneLine createCampagneLine1() {
        final OtfaCampagneLine line = new OtfaCampagneLine();
        line.setIndice(1);
        line.setCommentaire("Line 1");
        line.setReference(createCampagneItemRef());
        line.setCible(createCampagneItemCible());
        return line;
    }

    public static OtfaCampagneLine createCampagneLine2() {
        final OtfaCampagneLine line = new OtfaCampagneLine();
        line.setIndice(2);
        line.setCommentaire("Line 2");
        line.setReference(createCampagneItemRef());
        line.setCible(createCampagneItemCible());
        return line;
    }

    public static OtfaCampagneItem createCampagneItemRef() {
        final OtfaCampagneItem ref = new OtfaCampagneItem();
        ref.setEtuPath("refFile1.etu.xml");
        ref.setScenarioNom("refScenario1");
        ref.setCoeurName("refCoeur1");
        ref.setLaunchCompute(true);
        return ref;
    }

    public static OtfaCampagneItem createCampagneItemCible() {
        final OtfaCampagneItem cible = new OtfaCampagneItem();
        cible.setEtuPath("cibleFile1.etu.xml");
        cible.setScenarioNom("cibleScenario1");
        cible.setCoeurName("cibleCoeur1");
        cible.setLaunchCompute(false);
        return cible;
    }

    public static CtuluLogGroup createLogGroup() {
        final CtuluLogGroup group = createLogGroupSimple();
        group.addGroup(createLogGroupSimple());
        group.addGroup(createLogGroupSimple());
        return group;
    }

    public static CtuluLogGroup createLogGroupSimple() {
        final CtuluLogGroup group = new CtuluLogGroup(BusinessMessages.RESOURCE_BUNDLE);
        group.getLogs().clear();
        group.setMainAnalyze(createMainLog());
        group.addLog(createLog1());
        group.addLog(createLog2());
        return group;
    }

    public static CtuluLog createMainLog() {
        final CtuluLog log = new CtuluLog(BusinessMessages.RESOURCE_BUNDLE);
        log.setDesc("Main log");
        log.addError("Main log error 1.");
        log.addError("Main log error 2.");
        return log;
    }

    public static CtuluLog createLog1() {
        final CtuluLog log = new CtuluLog(BusinessMessages.RESOURCE_BUNDLE);
        log.setDesc("Log 1");
        log.addError("Log1 error 1.");
        log.addError("Log1 error 2.");
        return log;
    }

    public static CtuluLog createLog2() {
        final CtuluLog log = new CtuluLog(BusinessMessages.RESOURCE_BUNDLE);
        log.setDesc("Log 2");
        log.addError("Log2 error 1.");
        log.addError("Log2 error 2.");
        return log;
    }
}
