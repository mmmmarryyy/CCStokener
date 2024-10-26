package test.de.offis.semanticmm4u.derivation;

import java.io.IOException;
import java.io.InputStream;
import java.net.URL;
import junit.framework.TestCase;
import component_interfaces.semanticmm4u.realization.compositor.provided.IVariable;
import component_interfaces.semanticmm4u.realization.derivation.provided.IDerivator;
import component_interfaces.semanticmm4u.realization.generator.provided.IGenerator;
import component_interfaces.semanticmm4u.realization.generator.provided.IMultimediaPresentation;
import de.offis.semanticmm4u.compositors.variables.operators.basics.selector.TemporalSelector;
import de.offis.semanticmm4u.compositors.variables.operators.basics.temporal.Parallel;
import de.offis.semanticmm4u.compositors.variables.operators.basics.temporal.Sequential;
import de.offis.semanticmm4u.compositors.variables.operators.complex.MM4UDeserializer;
import de.offis.semanticmm4u.derivation.background.BackgroundImage;
import de.offis.semanticmm4u.derivation.cover_page.CoverPage;
import de.offis.semanticmm4u.derivation.image_per_page.ImagesPerPage;
import de.offis.semanticmm4u.derivation.image_size.ImageSize;
import de.offis.semanticmm4u.derivation.image_subtitle.ImageSubtitle;
import de.offis.semanticmm4u.derivation.location.Location;
import de.offis.semanticmm4u.derivation.metadata_rules.MetadataRules;
import de.offis.semanticmm4u.derivation.ontology.InCity;
import de.offis.semanticmm4u.derivation.page_title.PageTitle;
import de.offis.semanticmm4u.failures.MM4UGeneratorException;
import de.offis.semanticmm4u.failures.media_elements_connectors.MM4UCannotOpenMediaElementsConnectionException;
import de.offis.semanticmm4u.failures.media_elements_connectors.MM4UMediumElementNotFoundException;
import de.offis.semanticmm4u.generators.GeneratorToolkit;
import de.offis.semanticmm4u.global.Constants;
import de.offis.semanticmm4u.media_elements_connector.rdf_uri.RDFURIMediaElementsConnector;
import de.offis.semanticmm4u.user_profiles_connector.SimpleUserProfile;

/**
 * Test cases for the derivators. Important! You have to change the path to the
 * media in the test data
 * 
 * <pre>
 *        &lt;!DOCTYPE MM4U SYSTEM &quot;mm4u.dtd&quot; [
 *  &lt;!ENTITY path 'file:/C:/SemanticMM4U/Data/DerivationTestData/media/'&gt;
 *  ]&gt;
 * </pre>
 * 
 * 
 */
public class TestDerivators extends TestCase {

    private static final String TEST_DATA_PATH = Constants.getValue(Constants.CONFIG_MM4U_DATA_PATH) + "DerivationTestData";

    private static final int OUTPUT_FORMAT = GeneratorToolkit.FLASH;

    private static final String OUTPUT_PATH = Constants.getDefaultOutputPath() + "/TestDerivator_";

    public void testImageSizeDerivation() throws MM4UCannotOpenMediaElementsConnectionException, IOException, MM4UGeneratorException {
        this.runTest(new ImageSize(), "ImageSize", TEST_DATA_PATH + "/TitelTestExport.mm4u.xml");
    }

    public void testImagesPerPageDerivation() throws MM4UCannotOpenMediaElementsConnectionException, MM4UGeneratorException, IOException {
        this.runTest(new ImagesPerPage(), "ImagesPerPage", TEST_DATA_PATH + "/TitelTestExport.mm4u.xml");
    }

    public void testCoverPage() throws MM4UCannotOpenMediaElementsConnectionException, MM4UGeneratorException, IOException {
        this.runTest(new CoverPage(), "CoverPage", TEST_DATA_PATH + "/TitelTestExport.mm4u.xml");
    }

    public void testImageSubtitle() throws MM4UCannotOpenMediaElementsConnectionException, MM4UGeneratorException, IOException {
        this.runTest(new ImageSubtitle(), "ImageSubtitle", TEST_DATA_PATH + "/TitelTestExport.mm4u.xml");
    }

    public void testPageTitle() throws MM4UCannotOpenMediaElementsConnectionException, MM4UGeneratorException, IOException {
        this.runTest(new PageTitle(), "PageTitle", TEST_DATA_PATH + "/TitelTestExport.mm4u.xml");
    }

    public void testBackgroundImage() throws MM4UCannotOpenMediaElementsConnectionException, MM4UGeneratorException, IOException {
        this.runTest(new BackgroundImage(), "BackgroundImage", TEST_DATA_PATH + "/TitelTestExport.mm4u.xml");
    }

    public void testMetadataRules() throws MM4UCannotOpenMediaElementsConnectionException, MM4UGeneratorException, IOException {
        this.runTest(new MetadataRules(), "MetadataRules", TEST_DATA_PATH + "/TitelTestExport.mm4u.xml");
    }

    public void testLocation() throws MM4UCannotOpenMediaElementsConnectionException, MM4UGeneratorException, IOException, MM4UMediumElementNotFoundException {
        this.runLocationTest(new Location(), "Location");
    }

    public void testInCity() throws MM4UCannotOpenMediaElementsConnectionException, MM4UGeneratorException, IOException {
        this.runTest(new InCity(TEST_DATA_PATH + "/TestOntology/Norderney.owl"), "InCity", TEST_DATA_PATH + "/TitelTestExport.mm4u.xml");
    }

    private void runLocationTest(IDerivator testDerivator, String derivatorName) throws MM4UCannotOpenMediaElementsConnectionException, MM4UMediumElementNotFoundException, MM4UGeneratorException, IOException {
        RDFURIMediaElementsConnector connector = new RDFURIMediaElementsConnector();
        connector.openConnection();
        Parallel root = new Parallel();
        Sequential s1 = new Sequential();
        Parallel p1 = new Parallel();
        p1.addVariable(new TemporalSelector(connector.getMediumElement(TEST_DATA_PATH + "/loc_media/-1850540119.jpg"), 0, 5000));
        p1.addVariable(new TemporalSelector(connector.getMediumElement(TEST_DATA_PATH + "/loc_media/-1446145469.jpg"), 0, 5000));
        p1.addVariable(new TemporalSelector(connector.getMediumElement(TEST_DATA_PATH + "/loc_media/187079046.jpg"), 0, 5000));
        p1.addVariable(new TemporalSelector(connector.getMediumElement(TEST_DATA_PATH + "/loc_media/1371871591.jpg"), 0, 5000));
        Parallel p2 = new Parallel();
        p2.addVariable(new TemporalSelector(connector.getMediumElement(TEST_DATA_PATH + "/loc_media/-1087264199.jpg"), 0, 5000));
        p2.addVariable(new TemporalSelector(connector.getMediumElement(TEST_DATA_PATH + "/loc_media/977637398.jpg"), 0, 5000));
        Parallel p3 = new Parallel();
        p3.addVariable(new TemporalSelector(connector.getMediumElement(TEST_DATA_PATH + "/loc_media/-625224529.jpg"), 0, 5000));
        p3.addVariable(new TemporalSelector(connector.getMediumElement(TEST_DATA_PATH + "/loc_media/2070102296.jpg"), 0, 5000));
        Parallel p4 = new Parallel();
        Parallel p5 = new Parallel();
        p5.addVariable(new TemporalSelector(connector.getMediumElement(TEST_DATA_PATH + "/loc_media/-2107403115.jpg"), 0, 5000));
        p5.addVariable(new TemporalSelector(connector.getMediumElement(TEST_DATA_PATH + "/loc_media/-1731934776.jpg"), 0, 5000));
        p5.addVariable(new TemporalSelector(connector.getMediumElement(TEST_DATA_PATH + "/loc_media/-1083683403.jpg"), 0, 5000));
        s1.addVariable(p1);
        s1.addVariable(p2);
        s1.addVariable(p3);
        s1.addVariable(p4);
        s1.addVariable(p5);
        root.addVariable(s1);
        IVariable var = testDerivator.doDerivate(root);
        IGenerator myGenerator = GeneratorToolkit.getFactory(OUTPUT_FORMAT);
        IMultimediaPresentation presentation = myGenerator.doTransform(var, "DerivatorTest", new SimpleUserProfile());
        presentation.store(OUTPUT_PATH + derivatorName);
    }

    private void runTest(IDerivator testDerivator, String derivatorName, String testData) throws MM4UCannotOpenMediaElementsConnectionException, IOException, MM4UGeneratorException {
        RDFURIMediaElementsConnector connector = new RDFURIMediaElementsConnector();
        connector.openConnection();
        MM4UDeserializer deSerial = new MM4UDeserializer(connector);
        URL url = new URL(testData);
        InputStream inStream = url.openStream();
        deSerial.doDeSerialize(inStream, Constants.getValue("derivation_url"), false);
        IVariable var = testDerivator.doDerivate(deSerial);
        inStream.close();
        IGenerator myGenerator = GeneratorToolkit.getFactory(OUTPUT_FORMAT);
        IMultimediaPresentation presentation = myGenerator.doTransform(var, "DerivatorTest", new SimpleUserProfile());
        presentation.store(OUTPUT_PATH + derivatorName);
    }
}
