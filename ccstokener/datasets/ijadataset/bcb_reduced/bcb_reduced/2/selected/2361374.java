package test.de.offis.semanticmm4u.generators;

import java.io.IOException;
import java.io.InputStream;
import java.net.URL;
import junit.framework.TestCase;
import component_interfaces.semanticmm4u.realization.compositor.provided.IVariable;
import component_interfaces.semanticmm4u.realization.derivation.provided.IDerivator;
import component_interfaces.semanticmm4u.realization.generator.provided.IGenerator;
import component_interfaces.semanticmm4u.realization.generator.provided.IMultimediaPresentation;
import de.offis.semanticmm4u.compositors.variables.operators.complex.MM4UDeserializer;
import de.offis.semanticmm4u.derivation.image_per_page.ImagesPerPage;
import de.offis.semanticmm4u.derivation.image_size.ImageSize;
import de.offis.semanticmm4u.failures.MM4UGeneratorException;
import de.offis.semanticmm4u.failures.media_elements_connectors.MM4UCannotOpenMediaElementsConnectionException;
import de.offis.semanticmm4u.generators.GeneratorToolkit;
import de.offis.semanticmm4u.global.Constants;
import de.offis.semanticmm4u.media_elements_connector.rdf_uri.RDFURIMediaElementsConnector;
import de.offis.semanticmm4u.user_profiles_connector.SimpleUserProfile;

/**
 * This class tests the generators with the new RDFMetadataCollector. 
 */
public class TestMetadataGeneration extends TestCase {

    private static final String TEST_DATA_FILE = Constants.getValue(Constants.CONFIG_MM4U_DATA_PATH) + "DerivationTestData/demo2Testdata.mm4u.xml";

    private static final String OUTPUT_PATH = Constants.getDefaultOutputPath() + "/MetadataGeneratorTest";

    public void testFlash() throws MM4UCannotOpenMediaElementsConnectionException, MM4UGeneratorException, IOException {
        this.runTransformation(GeneratorToolkit.FLASH);
    }

    public void testSMIL() throws MM4UCannotOpenMediaElementsConnectionException, MM4UGeneratorException, IOException {
        this.runTransformation(GeneratorToolkit.SMIL2_0);
        this.runTransformation(GeneratorToolkit.SMIL2_0_BASIC_LANGUAGE_PROFILE);
        this.runTransformation(GeneratorToolkit.SMIL2_1);
        this.runTransformation(GeneratorToolkit.SMIL2_1_MOBILE_PROFILE);
        this.runTransformation(GeneratorToolkit.SMIL2_1_EXTENDED_MOBULE_PROFILE);
    }

    public void testSVG() throws MM4UCannotOpenMediaElementsConnectionException, MM4UGeneratorException, IOException {
        this.runTransformation(GeneratorToolkit.SVG1_2);
        this.runTransformation(GeneratorToolkit.SVG1_2_BASIC);
        this.runTransformation(GeneratorToolkit.SVG1_2_TINY);
    }

    public void testHTML() throws MM4UCannotOpenMediaElementsConnectionException, MM4UGeneratorException, IOException {
        this.runTransformation(GeneratorToolkit.HTML);
        this.runTransformation(GeneratorToolkit.HTML_AND_TIME);
        this.runTransformation(GeneratorToolkit.XHTML);
        this.runTransformation(GeneratorToolkit.XHTML_BASIC);
    }

    private void runTransformation(int outputFormat) throws MM4UCannotOpenMediaElementsConnectionException, IOException, MM4UGeneratorException {
        IDerivator[] derivators = new IDerivator[] { new ImageSize(), new ImagesPerPage() };
        RDFURIMediaElementsConnector connector = new RDFURIMediaElementsConnector();
        connector.openConnection();
        MM4UDeserializer deSerial = new MM4UDeserializer(connector);
        URL url = new URL(TEST_DATA_FILE);
        InputStream inStream = url.openStream();
        deSerial.doDeSerialize(inStream, Constants.getValue("derivation_url"), false);
        IVariable var = deSerial;
        for (int i = 0; i < derivators.length; i++) {
            var = derivators[i].doDerivate(var);
        }
        inStream.close();
        IGenerator myGenerator = GeneratorToolkit.getFactory(outputFormat);
        IMultimediaPresentation presentation = myGenerator.doTransform(var, "TestMetadataGeneration", new SimpleUserProfile());
        presentation.store(OUTPUT_PATH);
    }
}
