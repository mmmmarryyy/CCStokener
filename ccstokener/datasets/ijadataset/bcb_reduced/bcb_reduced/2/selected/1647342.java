package org.fudaa.dodico.crue.io;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.net.URL;
import java.util.Collection;
import java.util.Iterator;
import java.util.List;
import java.util.logging.Level;
import java.util.logging.Logger;
import org.fudaa.ctulu.CtuluLibFile;
import org.fudaa.ctulu.CtuluLog;
import org.fudaa.dodico.crue.common.BusinessMessages;
import org.fudaa.dodico.crue.config.TestCrueConfigMetierLoaderDefault;
import org.fudaa.dodico.crue.io.common.CrueIOResu;
import org.fudaa.dodico.crue.io.conf.CrueCONFReaderWriter;
import org.fudaa.dodico.crue.projet.coeur.CoeurConfig;
import org.fudaa.dodico.crue.projet.coeur.CoeurManager;
import org.fudaa.dodico.crue.projet.conf.Configuration;
import org.fudaa.dodico.crue.projet.conf.Option;
import org.fudaa.dodico.crue.projet.conf.SiteConfiguration;
import org.fudaa.dodico.crue.projet.conf.SiteOption;
import org.fudaa.dodico.crue.projet.conf.UserConfiguration;
import org.fudaa.dodico.crue.projet.conf.UserOption;

/**
 * @author CANEL Christophe (Genesis)
 */
public class TestCrueCONF extends AbstractIOTestCase {

    protected static final String CONFIG_SITE = "/FudaaCrue_Site.xml";

    protected static final String CONFIG_USER = "/FudaaCrue_User.xml";

    /**
   * Test du fichier inclus dans l'application
   */
    protected static final String CONFIG_SITE_OFFICIEL = "/FudaaCrue_Site_officiel.xml";

    public TestCrueCONF() {
        super(CONFIG_SITE);
    }

    public void testSiteLecture() {
        final File coeurFile = extractSiteFile();
        final Configuration config = read(CONFIG_SITE);
        assertCorrect(config, coeurFile.getParentFile());
    }

    public void testSiteUser() {
        final Configuration config = read(CONFIG_USER);
        assertUserCorrect(config.getUser());
        assertNull(config.getSite());
    }

    public void testSiteOfficelLecture() {
        final Configuration config = read(CONFIG_SITE_OFFICIEL);
        assertNotNull(config);
    }

    private static void assertCorrect(Configuration config, File baseDir) {
        assertSiteCorrect(config.getSite(), baseDir);
        assertUserCorrect(config.getUser());
    }

    private static void assertSiteCorrect(SiteConfiguration config, File baseDir) {
        assertCorrect(config.getCoeurs(), baseDir);
        final List<SiteOption> options = config.getOptions();
        SiteOption option = options.get(0);
        assertEquals("SiteNom1", option.getId());
        assertEquals("Commentaire de l'option SiteNom1.", option.getCommentaire());
        assertEquals("SiteValeur1", option.getValeur());
        assertEquals(true, option.isUserVisible());
        option = options.get(1);
        assertEquals("SiteNom2", option.getId());
        assertEquals("Commentaire de l'option SiteNom2.", option.getCommentaire());
        assertEquals("SiteValeur2", option.getValeur());
        assertEquals(false, option.isUserVisible());
    }

    private static void assertCorrect(List<CoeurConfig> coeurs, File baseDir) {
        CoeurManager coeurManager = new CoeurManager(baseDir, coeurs);
        assertEquals(3, coeurs.size());
        CoeurConfig coeur = coeurs.get(0);
        assertEquals("CRUE10.1", coeur.getName());
        assertEquals("Coeur Crue10.1", coeur.getComment());
        assertEquals("1.1.1", coeur.getXsdVersion());
        assertEquals(coeur.getName(), coeurManager.getCoeurConfig(coeur.getName()).getName());
        try {
            assertEquals(new File(baseDir, "Crue10.1").getCanonicalPath(), coeur.getCoeurFolder().getCanonicalPath());
        } catch (IOException e) {
            fail(e.getMessage());
        }
        assertEquals(true, coeur.isUsedByDefault());
        coeur = coeurs.get(1);
        assertEquals("CRUE10.2a", coeur.getName());
        assertEquals("Coeur Crue10.2a", coeur.getComment());
        assertEquals("1.2", coeur.getXsdVersion());
        assertEquals(new File(baseDir, "Crue10.2a").getAbsolutePath(), coeur.getCoeurFolder().getAbsolutePath());
        assertEquals(false, coeur.isUsedByDefault());
        coeur = coeurs.get(2);
        assertEquals("CRUE10.2b", coeur.getName());
        assertEquals("Coeur Crue10.2b", coeur.getComment());
        assertEquals("1.2", coeur.getXsdVersion());
        assertEquals(new File(baseDir, "Crue10.2b").getAbsolutePath(), coeur.getCoeurFolder().getAbsolutePath());
        assertEquals(false, coeur.isUsedByDefault());
    }

    private File extractSiteFile() {
        final URL url = TestCrueCONF.class.getResource(CONFIG_SITE);
        final File confFile = new File(createTempDir(), "FudaaCrue_Site.xml");
        try {
            CtuluLibFile.copyStream(url.openStream(), new FileOutputStream(confFile), true, true);
        } catch (Exception e) {
            Logger.getLogger(TestCrueCONF.class.getName()).log(Level.SEVERE, "erreur while extracting FudaaCrue_Site.xml", e);
            fail(e.getMessage());
        }
        return confFile;
    }

    /**
   * @param config
   */
    private static void assertUserCorrect(UserConfiguration config) {
        final Collection<UserOption> options = config.getOptions();
        Iterator<UserOption> iterator = options.iterator();
        Option option = iterator.next();
        assertEquals("UserNom1", option.getId());
        assertEquals("Commentaire de l'option UserNom1.", option.getCommentaire());
        assertEquals("UserValeur1", option.getValeur());
        option = iterator.next();
        assertEquals("UserNom2", option.getId());
        assertEquals("Commentaire de l'option UserNom2.", option.getCommentaire());
        assertEquals("UserValeur2", option.getValeur());
    }

    public void testUserEcriture() {
        File newConfFile = null;
        try {
            newConfFile = File.createTempFile("Test", "CrueCONF");
        } catch (IOException e) {
            Logger.getLogger(TestCrueCONF.class.getName()).log(Level.SEVERE, "can't create temp file", e);
            fail(e.getMessage());
        }
        Configuration config = read(CONFIG_USER);
        final CrueCONFReaderWriter writer = new CrueCONFReaderWriter("1.2");
        final CrueIOResu<Configuration> resu = new CrueIOResu<Configuration>(config);
        final CtuluLog log = new CtuluLog(BusinessMessages.RESOURCE_BUNDLE);
        assertTrue(writer.writeXMLMetier(resu, newConfFile, log, TestCrueConfigMetierLoaderDefault.DEFAULT));
        testAnalyser(log);
        config = read(newConfFile);
        assertUserCorrect(config.getUser());
        assertNull(config.getSite());
    }

    private static Configuration read(String resource) {
        final CrueCONFReaderWriter reader = new CrueCONFReaderWriter("1.2");
        final CtuluLog log = new CtuluLog(BusinessMessages.RESOURCE_BUNDLE);
        final CrueIOResu<Configuration> result = reader.readXML(resource, log, null);
        testAnalyser(log);
        return result.getMetier();
    }

    private static Configuration read(File resource) {
        final CrueCONFReaderWriter reader = new CrueCONFReaderWriter("1.2");
        final CtuluLog log = new CtuluLog(BusinessMessages.RESOURCE_BUNDLE);
        final CrueIOResu<Configuration> result = reader.readXML(resource, log, null);
        testAnalyser(log);
        return result.getMetier();
    }

    public void testValide() {
    }
}
