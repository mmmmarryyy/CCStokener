package org.fudaa.dodico.crue.io;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.net.URL;
import java.util.ArrayList;
import java.util.List;
import org.fudaa.ctulu.CtuluLibFile;
import org.fudaa.ctulu.CtuluLog;
import org.fudaa.dodico.crue.common.BusinessMessages;
import org.fudaa.dodico.crue.comparaison.ExecuteComparaison.ExecuteComparaisonResult;
import org.fudaa.dodico.crue.comparaison.tester.ResultatTest;
import org.fudaa.dodico.crue.config.TestCrueConfigMetierLoaderDefault;
import org.fudaa.dodico.crue.io.common.CrueIOResu;
import org.fudaa.dodico.crue.io.rtfa.CrueRTFAReaderWriter;
import org.fudaa.dodico.crue.projet.otfa.OtfaCampagneLineResult;
import org.fudaa.dodico.crue.projet.otfa.OtfaCampagneResult;
import org.fudaa.dodico.crue.projet.otfa.OtfaCampagneResultHeader;
import org.joda.time.LocalDateTime;
import com.memoire.fu.FuLib;

/**
 * @author CANEL Christophe (Genesis)
 */
public class TestCrueRTFA extends AbstractIOTestCase {

    protected static final String FICHIER_TEST_XML = "/otfa/campagne.rtfa.xml";

    public TestCrueRTFA() {
        super(FICHIER_TEST_XML);
    }

    public void testLecture() {
        final OtfaCampagneResult compPart = read(this.getTestFile());
        assertCorrect(compPart);
    }

    public static void assertCorrect(OtfaCampagneResult compPart) {
        final List<OtfaCampagneLineResult> lines = compPart.getResults();
        assertHeaderCorrect(compPart.getHeader());
        assertEquals(2, lines.size());
        assertCorrectCompPartLine1(lines.get(0));
        assertCorrectCompPartLine2(lines.get(1));
    }

    public static void assertCorrectCompPartLine1(OtfaCampagneLineResult line) {
        final List<ExecuteComparaisonResult> comparisons = line.getComparisonResult();
        TestCrueCTFA.assertCorrectLine1(line.getInitialLine());
        assertEquals(2, comparisons.size());
        assertCorrectCompResult1(comparisons.get(0));
        assertCorrectCompResult2(comparisons.get(1));
    }

    public static void assertCorrectCompPartLine2(OtfaCampagneLineResult line) {
        final List<ExecuteComparaisonResult> comparisons = line.getComparisonResult();
        TestCrueCTFA.assertCorrectLine2(line.getInitialLine());
        assertEquals(2, comparisons.size());
        assertCorrectCompResult1(comparisons.get(0));
        assertCorrectCompResult2(comparisons.get(1));
    }

    public static void assertCorrectCompResult1(ExecuteComparaisonResult result) {
        assertEquals("Comp1", result.getId());
        assertEquals("Comparaison 1", result.getMsg());
        ResultatTest res = result.getRes();
        assertEquals("ref1", res.getPrintA());
        assertEquals("cible1", res.getPrintB());
        assertEquals("msg1", res.getMsg());
    }

    public static void assertCorrectCompResult2(ExecuteComparaisonResult result) {
        assertEquals("Comp2", result.getId());
        assertEquals("Comparaison 2", result.getMsg());
        ResultatTest res = result.getRes();
        assertEquals("ref2", res.getPrintA());
        assertEquals("cible2", res.getPrintB());
        assertEquals("msg2", res.getMsg());
    }

    public void testEcriture() {
        final File rtfaFile = this.getTestFile();
        File newRtfaFile = null;
        try {
            newRtfaFile = File.createTempFile("Test", "CrueRTFA");
        } catch (IOException e) {
            e.printStackTrace();
            fail();
        }
        OtfaCampagneResult compPart = read(rtfaFile);
        final CrueRTFAReaderWriter writer = new CrueRTFAReaderWriter("1.2");
        final CrueIOResu<OtfaCampagneResult> resu = new CrueIOResu<OtfaCampagneResult>(compPart);
        final CtuluLog log = new CtuluLog(BusinessMessages.RESOURCE_BUNDLE);
        assertTrue(writer.writeXMLMetier(resu, newRtfaFile, log, TestCrueConfigMetierLoaderDefault.DEFAULT));
        testAnalyser(log);
        compPart = read(newRtfaFile);
        assertCorrect(compPart);
    }

    private File getTestFile() {
        final URL url = TestCrueLOG.class.getResource(FICHIER_TEST_XML);
        final File ctfaFile = new File(createTempDir(), "resultat.rtfa.xml");
        try {
            CtuluLibFile.copyStream(url.openStream(), new FileOutputStream(ctfaFile), true, true);
        } catch (Exception e) {
            e.printStackTrace();
            fail();
        }
        return ctfaFile;
    }

    private static OtfaCampagneResult read(File rtfaFile) {
        final CrueRTFAReaderWriter reader = new CrueRTFAReaderWriter("1.2");
        final CtuluLog log = new CtuluLog(BusinessMessages.RESOURCE_BUNDLE);
        final CrueIOResu<OtfaCampagneResult> result = reader.readXML(rtfaFile, log, createDefault());
        testAnalyser(log);
        return result.getMetier();
    }

    public static void main(String[] args) {
        OtfaCampagneResult part = createResultCompPart();
        File newRtfaFile = new File(FuLib.getUserHome(), "resultat.rtfa.xml");
        System.err.println(newRtfaFile.getAbsolutePath());
        final CrueRTFAReaderWriter writer = new CrueRTFAReaderWriter("1.2");
        final CrueIOResu<OtfaCampagneResult> resu = new CrueIOResu<OtfaCampagneResult>(part);
        final CtuluLog log = new CtuluLog(BusinessMessages.RESOURCE_BUNDLE);
        assertTrue(writer.writeXMLMetier(resu, newRtfaFile, log, TestCrueConfigMetierLoaderDefault.DEFAULT));
    }

    public void testValide() {
    }

    public static OtfaCampagneResult createResultCompPart() {
        final OtfaCampagneResult part = new OtfaCampagneResult();
        part.setHeader(TestCrueCTFA.createHeaderPart());
        part.setResults(createResultLogsCompLines());
        return part;
    }

    public static List<OtfaCampagneLineResult> createResultLogsCompLines() {
        final List<OtfaCampagneLineResult> lines = new ArrayList<OtfaCampagneLineResult>();
        lines.add(createResultCompPartLine1());
        lines.add(createResultCompPartLine2());
        return lines;
    }

    public static OtfaCampagneLineResult createResultCompPartLine1() {
        final OtfaCampagneLineResult line = new OtfaCampagneLineResult();
        line.setInitialLine(TestCrueCTFA.createCampagneLine1());
        line.setComparisonResult(createComparaisonResults());
        return line;
    }

    public static OtfaCampagneLineResult createResultCompPartLine2() {
        final OtfaCampagneLineResult line = new OtfaCampagneLineResult();
        line.setInitialLine(TestCrueCTFA.createCampagneLine2());
        line.setComparisonResult(createComparaisonResults());
        return line;
    }

    public static List<ExecuteComparaisonResult> createComparaisonResults() {
        List<ExecuteComparaisonResult> results = new ArrayList<ExecuteComparaisonResult>();
        results.add(createComparaisonResult1());
        results.add(createComparaisonResult2());
        return results;
    }

    public static ExecuteComparaisonResult createComparaisonResult1() {
        ExecuteComparaisonResult result = new ExecuteComparaisonResult(null);
        result.setId("Comp1");
        result.setMsg("Comparaison 1");
        result.setLog(TestCrueCTFA.createMainLog());
        ResultatTest resTest = new ResultatTest("ref1", "cible1", "msg1");
        resTest.setPrintA("ref1");
        resTest.setPrintB("cible1");
        result.setRes(resTest);
        return result;
    }

    public static void assertHeaderCorrect(OtfaCampagneResultHeader header) {
        assertEquals("Une header.", header.getCommentaire());
        assertEquals(new LocalDateTime(2011, 11, 24, 9, 25), header.getDate());
    }

    public static ExecuteComparaisonResult createComparaisonResult2() {
        ExecuteComparaisonResult result = new ExecuteComparaisonResult(null);
        result.setId("Comp2");
        result.setMsg("Comparaison 2");
        result.setLog(TestCrueCTFA.createMainLog());
        result.setLog(TestCrueCTFA.createMainLog());
        ResultatTest resTest = new ResultatTest("ref2", "cible2", "msg2");
        resTest.setPrintA("ref2");
        resTest.setPrintB("cible2");
        result.setRes(resTest);
        return result;
    }
}
