package org.fudaa.dodico.crue.io;

import java.io.File;
import java.io.FileOutputStream;
import java.net.URL;
import org.fudaa.ctulu.CtuluLibFile;
import org.fudaa.ctulu.CtuluLog;
import org.fudaa.dodico.crue.common.BusinessMessages;
import org.fudaa.dodico.crue.config.TestCrueConfigMetierLoaderDefault;
import org.fudaa.dodico.crue.io.common.CrueIOResu;
import org.fudaa.dodico.crue.io.otfa.CrueOTFAReaderWriter;
import org.fudaa.dodico.crue.projet.otfa.OtfaCampagne;
import org.fudaa.dodico.crue.projet.otfa.OtfaCampagneItem;
import org.fudaa.dodico.crue.projet.otfa.OtfaCampagneLine;
import org.joda.time.LocalDateTime;

/**
 * @author CANEL Christophe (Genesis)
 *
 */
public class TestCrueOTFA extends AbstractIOTestCase {

    protected static final String FICHIER_TEST_XML = "/otfa/campagne.otfa.xml";

    protected static final String REPERTOIRe_FICHIER_TEST_XML = "/";

    public TestCrueOTFA() {
        super(FICHIER_TEST_XML);
    }

    public void testLecture() {
        final File otfaFile = this.getTestFile();
        final OtfaCampagne campagne = read(otfaFile);
        assertOtfaCampagne(campagne, otfaFile.getParentFile());
    }

    public void testEcriture() {
        final File otfaFile = this.getTestFile();
        final File newOtfaFile = new File(otfaFile.getParentFile(), "campagne.new.otfa.xml");
        OtfaCampagne campagne = read(otfaFile);
        final CrueOTFAReaderWriter writer = new CrueOTFAReaderWriter("1.2");
        final CrueIOResu<OtfaCampagne> resu = new CrueIOResu<OtfaCampagne>(campagne);
        final CtuluLog log = new CtuluLog(BusinessMessages.RESOURCE_BUNDLE);
        assertTrue(writer.writeXMLMetier(resu, newOtfaFile, log, TestCrueConfigMetierLoaderDefault.DEFAULT));
        testAnalyser(log);
        campagne = read(newOtfaFile);
        assertOtfaCampagne(campagne, newOtfaFile.getParentFile());
    }

    private File getTestFile() {
        final URL url = TestCrueOTFA.class.getResource(FICHIER_TEST_XML);
        final File otfaFile = new File(createTempDir(), "campagne.otfa.xml");
        try {
            CtuluLibFile.copyStream(url.openStream(), new FileOutputStream(otfaFile), true, true);
        } catch (Exception e) {
            e.printStackTrace();
            fail();
        }
        return otfaFile;
    }

    private static OtfaCampagne read(File otfaFile) {
        final CrueOTFAReaderWriter reader = new CrueOTFAReaderWriter("1.2", otfaFile);
        final CtuluLog log = new CtuluLog(BusinessMessages.RESOURCE_BUNDLE);
        final CrueIOResu<OtfaCampagne> result = reader.readXML(otfaFile, log, createDefault());
        testAnalyser(log);
        return result.getMetier();
    }

    private static void assertOtfaCampagne(OtfaCampagne campagne, File otfaDir) {
        assertEquals("Commentaire du fichier OTFA", campagne.getCommentaire());
        assertEquals("User", campagne.getAuteurCreation());
        assertEquals(new LocalDateTime(2011, 4, 20, 8, 45, 0), campagne.getDateCreation());
        assertEquals("User modif", campagne.getAuteurModification());
        assertEquals(new LocalDateTime(2011, 4, 22, 8, 45, 0), campagne.getDateModification());
        assertEquals(true, campagne.getReferenceOptions().isEffacerRunAvant());
        assertEquals(false, campagne.getReferenceOptions().isEffacerRunApres());
        assertEquals(false, campagne.getCibleOptions().isEffacerRunAvant());
        assertEquals(true, campagne.getCibleOptions().isEffacerRunApres());
        assertEquals(false, campagne.getComparaisonOptions().isComparerLesCR());
        assertEquals(getLine1(otfaDir), campagne.getLines().get(0));
        assertEquals(getLine2(otfaDir), campagne.getLines().get(1));
    }

    private static OtfaCampagneLine getLine1(File otfaDir) {
        OtfaCampagneLine line = new OtfaCampagneLine();
        line.setIndice(1);
        line.setCommentaire("Commentaire de la LigneCampagne ");
        OtfaCampagneItem item = new OtfaCampagneItem();
        item.setEtuPath("/v10.1/Etu3-0");
        item.setScenarioNom("Sc_M3-0_c9");
        item.setCoeurName("Crue9");
        item.setLaunchCompute(true);
        line.setReference(item);
        item = new OtfaCampagneItem();
        item.setEtuPath("C:/etudes/v10.2/Etu3-0");
        item.setScenarioNom("Sc_M3-0_c9");
        item.setCoeurName("Crue10.1");
        item.setLaunchCompute(true);
        line.setCible(item);
        return line;
    }

    private static OtfaCampagneLine getLine2(File otfaDir) {
        OtfaCampagneLine line = new OtfaCampagneLine();
        line.setIndice(2);
        line.setCommentaire("Commentaire de la LigneCampagne ");
        OtfaCampagneItem item = new OtfaCampagneItem();
        item.setEtuPath("/v10.1/Etu3-0");
        item.setScenarioNom("Sc_M3-0_c9c10");
        item.setCoeurName("Crue10.1");
        item.setLaunchCompute(false);
        line.setReference(item);
        item = new OtfaCampagneItem();
        item.setEtuPath("C:/etudes/v10.2/Etu3-0");
        item.setScenarioNom("Sc_M3-0_c9c10");
        item.setCoeurName("Crue10.2");
        item.setLaunchCompute(false);
        line.setCible(item);
        return line;
    }

    private static void assertEquals(OtfaCampagneLine expected, OtfaCampagneLine actual) {
        assertEquals(expected.getIndice(), actual.getIndice());
        assertEquals(expected.getCommentaire(), actual.getCommentaire());
        assertEquals(expected.getReference(), actual.getReference());
        assertEquals(expected.getCible(), actual.getCible());
    }

    private static void assertEquals(OtfaCampagneItem expected, OtfaCampagneItem actual) {
        assertEquals(expected.getEtuPath(), actual.getEtuPath());
        assertEquals(expected.getScenarioNom(), actual.getScenarioNom());
        assertEquals(expected.getCoeurName(), actual.getCoeurName());
        assertEquals(expected.isLaunchCompute(), actual.isLaunchCompute());
    }

    public void testValide() {
    }
}
