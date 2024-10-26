package de.fzi.kadmos.parser.impl;

import static org.junit.Assert.*;
import java.io.BufferedReader;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileWriter;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.PrintWriter;
import java.io.Reader;
import java.io.StringReader;
import java.io.StringWriter;
import java.net.URI;
import java.net.URL;
import java.util.Enumeration;
import org.jdom.Document;
import org.jdom.Element;
import org.jdom.JDOMException;
import org.jdom.input.SAXBuilder;
import org.jdom.output.XMLOutputter;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Test;
import org.semanticweb.owl.align.Cell;
import org.semanticweb.owlapi.apibinding.OWLManager;
import org.semanticweb.owlapi.model.IRI;
import org.semanticweb.owlapi.model.OWLEntity;
import org.semanticweb.owlapi.model.OWLOntology;
import org.semanticweb.owlapi.model.OWLOntologyManager;
import org.xml.sax.InputSource;
import de.fzi.kadmos.api.Alignment;
import de.fzi.kadmos.api.AlignmentFactory;
import de.fzi.kadmos.api.Correspondence;
import de.fzi.kadmos.api.CorrespondenceFactory;
import de.fzi.kadmos.api.impl.SimpleAlignment;
import de.fzi.kadmos.api.impl.SimpleAlignmentFactory;
import de.fzi.kadmos.api.impl.SimpleCorrespondenceFactory;
import de.fzi.kadmos.mocks.MockAlignment;
import de.fzi.kadmos.mocks.MockAlignmentFactory;
import de.fzi.kadmos.mocks.MockCorrespondence1;
import de.fzi.kadmos.mocks.MockCorrespondence1Factory;
import de.fzi.kadmos.mocks.MockCorrespondence2;
import de.fzi.kadmos.mocks.MockCorrespondence2Factory;
import de.fzi.kadmos.parser.AlignmentParser;
import de.fzi.kadmos.parser.AlignmentParserException;
import de.fzi.kadmos.parser.ParsingLevel;
import de.fzi.kadmos.renderer.AlignmentRenderer;
import de.fzi.kadmos.renderer.impl.INRIAFormatRenderer;

/**
 * Test class for the {@link INRIAFormatParser}.
 * 
 * @author Juergen Bock
 * @author Matthias Stumpp
 *
 */
public class INRIAFormatParserTest {

    private static final String ONTO_IRI = "http://www.w3.org/TR/2003/PR-owl-guide-20031209/wine";

    private static final String ONTO_IRI_3 = "http://www.w3.org/TR/2003/PR-owl-guide-20031215/wine";

    private static final String ONTO_IRI_1 = ONTO_IRI + "#madeFromGrape";

    private static final String ONTO_IRI_2 = ONTO_IRI + "#AlsaceRegion";

    private static URL INRIA_format_no_location = INRIAFormatParserTest.class.getResource("/alignments/INRIA_format_no_location.rdf");

    private static URL INRIA_format_with_location = INRIAFormatParserTest.class.getResource("/alignments/INRIA_format_with_location.rdf");

    private static URL INRIA_format_onto_iri = INRIAFormatParserTest.class.getResource("/ontologies/wine.rdf");

    /**
     * Updates absolute paths in alignment
     */
    @BeforeClass
    public static void updateAlignmentAbsolutePath() {
        absolutizeURL(INRIA_format_with_location);
    }

    /**
     * Resets the parser before a test
     */
    @Before
    public void resetParserBeforeTest() {
        resetParser(INRIAFormatParser.getInstance());
    }

    /**
     * If two instances are requested using the same {@link Reader},
     * the same {@link AlignmentParser} instance is provided. If 
     * different {@link Reader}s are used, different instances
     * are provided.
     * @throws Exception
     */
    @Test
    public final void testInstanceCaching() throws Exception {
        AlignmentParser parser1a = INRIAFormatParser.getInstance();
        AlignmentParser parser1b = INRIAFormatParser.getInstance();
        assertTrue(parser1a == parser1b);
    }

    /**
     * If a non-well-formed XML-String is given as input to the parser,
     * an {@link AlignmentParserException} is thrown.
     * @throws Exception
     */
    @Test(expected = AlignmentParserException.class)
    public final void testDocumentNotWellFormed() throws Exception {
        StringReader reader1 = new StringReader(buildNotWellFormedXMLDocument());
        AlignmentParser parser = INRIAFormatParser.getInstance();
        parser.parse(reader1);
    }

    /**
     * If an XML-String with no root element is given to the parser
     * an {@link AlignmentParserException} is thrown.
     * @throws Exception
     */
    @Test(expected = AlignmentParserException.class)
    public final void testDocumentNoRoot() throws Exception {
        StringReader reader1 = new StringReader(buildNoRootXMLDocument());
        AlignmentParser parser = INRIAFormatParser.getInstance();
        parser.parse(reader1);
    }

    /**
     * If an XML-String with invalid root element is given to the parser
     * an {@link AlignmentParserException} is thrown.
     * @throws Exception
     */
    @Test(expected = AlignmentParserException.class)
    public final void testDocumentNoOntologies() throws Exception {
        StringReader reader1 = new StringReader(buildNoOntologiesXMLDocument());
        AlignmentParser parser = INRIAFormatParser.getInstance();
        parser.parse(reader1);
    }

    /**
     * If an XML-String with no root element is given to the parser
     * an {@link AlignmentParserException} is thrown.
     * @throws Exception
     */
    @Test
    public final void testParseFile() throws Exception {
        File file1 = new File(INRIA_format_no_location.toURI());
        AlignmentParser parser = INRIAFormatParser.getInstance();
        parser.parse(file1);
    }

    /**
     * If an XML-String with no root element is given to the parser
     * an {@link AlignmentParserException} is thrown.
     * @throws Exception
     */
    @Test
    public final void testParseStringURI() throws Exception {
        AlignmentParser parser = INRIAFormatParser.getInstance();
        parser.parse(INRIA_format_no_location.toString());
    }

    /**
     * If an XML-String with no root element is given to the parser
     * an {@link AlignmentParserException} is thrown.
     * @throws Exception
     */
    @Test
    public final void testParseStringAbsolutePath() throws Exception {
        File file1 = new File(INRIA_format_no_location.toURI());
        AlignmentParser parser = INRIAFormatParser.getInstance();
        parser.parse(file1.getAbsolutePath());
    }

    /**
     * If an XML-String with no root element is given to the parser
     * an {@link AlignmentParserException} is thrown.
     * @throws Exception
     */
    @Test
    public final void testParseURL() throws Exception {
        AlignmentParser parser = INRIAFormatParser.getInstance();
        parser.parse(INRIA_format_no_location);
    }

    /**
     * If an XML-String with no root element is given to the parser
     * an {@link AlignmentParserException} is thrown.
     * @throws Exception
     */
    @Test
    public final void testParseReader() throws Exception {
        StringReader reader1 = new StringReader(buildValidXMLDocument());
        AlignmentParser parser = INRIAFormatParser.getInstance();
        parser.parse(reader1);
    }

    /**
     * If a parser is invoked twice with the same reader,
     * the second call should throw exception
     * @throws Exception
     */
    @Test(expected = AlignmentParserException.class)
    public final void testParseTwiceOneReader() throws Exception {
        StringReader reader1 = new StringReader(buildValidXMLDocument());
        AlignmentParser parser = INRIAFormatParser.getInstance();
        parser.parse(reader1);
        parser.parse(reader1);
    }

    /**
     * If a parser is invoked twice with different readers, 
     * the parser should do the job twice without complaining.
     * @throws Exception
     */
    @Test
    public final void testParseTwiceDifferentReader() throws Exception {
        StringReader reader1 = new StringReader(buildValidXMLDocument());
        StringReader reader2 = new StringReader(buildValidXMLDocument());
        AlignmentParser parser = INRIAFormatParser.getInstance();
        parser.parse(reader1);
        parser.parse(reader2);
    }

    /**
     * If an XML-String with no root element is given to the parser
     * an {@link AlignmentParserException} is thrown.
     * @throws Exception
     */
    @Test
    public final void testParseInputStream() throws Exception {
        FileInputStream inputStream = new FileInputStream(new File(INRIA_format_no_location.toURI()));
        AlignmentParser parser = INRIAFormatParser.getInstance();
        parser.parse(inputStream);
    }

    /**
     * If an XML-String with no root element is given to the parser
     * an {@link AlignmentParserException} is thrown.
     * @throws Exception
     */
    @Test
    public final void testParseInputSourceFromReader() throws Exception {
        StringReader reader = new StringReader(buildValidXMLDocument());
        InputSource inputSource = new InputSource(reader);
        AlignmentParser parser = INRIAFormatParser.getInstance();
        parser.parse(inputSource);
    }

    /**
     * If an {@link AlignmentFactory} is set, the parser should produce
     * {@link Alignment}s of the type, the factory creates.
     * If the {@link AlignmentFactory} is set to a different factory, the parser
     * should consequently produce {@link Alignment}s of the type, that
     * new factory creates.
     * @throws Exception
     */
    @Test
    public final void testCorrectAlignmentFactory() throws Exception {
        StringReader reader1 = new StringReader(buildValidXMLDocument());
        AlignmentParser parser = INRIAFormatParser.getInstance();
        AlignmentFactory factory;
        factory = SimpleAlignmentFactory.getInstance();
        parser.setAlignmentFactory(factory);
        Alignment alignment = parser.parse(reader1);
        assertTrue(alignment instanceof SimpleAlignment);
        reader1 = new StringReader(buildValidXMLDocument());
        factory = new MockAlignmentFactory();
        parser.setAlignmentFactory(factory);
        alignment = parser.parse(reader1);
        assertTrue(alignment instanceof MockAlignment);
    }

    /**
     * If a {@link CorrespondenceFactory} is set, the parser should produce
     * {@link Correspondence}s of the type, the factory generates.
     * If the {@link CorrespondenceFactory} is set to a different factory,
     * the parser should consequently produce {@link Correspondence}s of the type,
     * that new factory creates.
     * @throws Exception
     */
    @Test
    public final void testCorrectCorrespondenceFactory() throws Exception {
        StringReader reader1 = new StringReader(buildValidXMLDocument());
        AlignmentParser parser = INRIAFormatParser.getInstance();
        CorrespondenceFactory factory = new MockCorrespondence1Factory();
        parser.setCorrespondenceFactory(factory);
        Alignment alignment = parser.parse(reader1);
        for (Correspondence<? extends OWLEntity> c : alignment) {
            assertTrue(c instanceof MockCorrespondence1<?>);
        }
        reader1 = new StringReader(buildValidXMLDocument());
        factory = new MockCorrespondence2Factory();
        parser.setCorrespondenceFactory(factory);
        alignment = parser.parse(reader1);
        for (Correspondence<? extends OWLEntity> c : alignment) {
            assertTrue(c instanceof MockCorrespondence2<?>);
        }
    }

    /**
     * If an Alignment contains OWLOntologies contained in XML-String
     * an {@link AlignmentParserException} is thrown.
     * @throws Exception
     */
    @Test
    public final void testParseHasOWLOntologies() throws Exception {
        StringReader reader1 = new StringReader(buildValidXMLDocument());
        AlignmentParser parser = INRIAFormatParser.getInstance();
        Alignment alignment = parser.parse(reader1);
        OWLOntologyManager manager1 = OWLManager.createOWLOntologyManager();
        OWLOntologyManager manager2 = OWLManager.createOWLOntologyManager();
        OWLOntology ont1 = manager1.createOntology(IRI.create(ONTO_IRI));
        OWLOntology ont2 = manager2.createOntology(IRI.create(ONTO_IRI));
        assertEquals(ont1, alignment.getOntology1());
        assertEquals(ont2, alignment.getOntology2());
    }

    /**
     * If an Alignment contains OWLEntities with IRIs contained in XML-String
     * an {@link AlignmentParserException} is thrown.
     * @throws Exception
     */
    @Test
    public final void testParseContainsIRI() throws Exception {
        StringReader reader1 = new StringReader(buildValidXMLDocument());
        AlignmentParser parser = INRIAFormatParser.getInstance();
        Alignment alignment = parser.parse(reader1);
        IRI iri1 = IRI.create(ONTO_IRI_1);
        IRI iri2 = IRI.create(ONTO_IRI_1);
        assertTrue(alignment.contains(iri1, iri2));
        iri1 = IRI.create(ONTO_IRI_2);
        iri2 = IRI.create(ONTO_IRI_2);
        assertTrue(alignment.contains(iri1, iri2));
    }

    /**
     * If an Alignment contains OWLEntities with IRIs contained in XML-String
     * an {@link AlignmentParserException} is thrown.
     * @throws Exception
     */
    @Test
    public final void testParseAlignmentParserExceptionLevelONE() throws Exception {
        StringReader reader1 = new StringReader(buildValidXMLDocumentWithUnkownEntities());
        AlignmentParser parser = INRIAFormatParser.getInstance();
        parser.setParsingLevel(ParsingLevel.ONE);
        Alignment alignment = parser.parse(reader1);
        assertEquals(alignment.getCorrespondences().size(), 2);
    }

    /**
     * If an Alignment contains OWLEntities with IRIs contained in XML-String
     * an {@link AlignmentParserException} is thrown.
     * @throws Exception
     */
    @Test
    public final void testParseAlignmentParserExceptionLEVELTWO() throws Exception {
        StringReader reader1 = new StringReader(buildValidXMLDocumentWithUnkownEntities());
        AlignmentParser parser = INRIAFormatParser.getInstance();
        parser.setParsingLevel(ParsingLevel.TWO);
        Alignment alignment = parser.parse(reader1);
        assertEquals(alignment.getCorrespondences().size(), 2);
    }

    /**
     * If an Alignment contains OWLEntities with IRIs contained in XML-String
     * an {@link AlignmentParserException} is thrown.
     * @throws Exception
     */
    @Test(expected = AlignmentParserException.class)
    public final void testParseAlignmentParserExceptionLEVELTHREE() throws Exception {
        StringReader reader1 = new StringReader(buildValidXMLDocumentWithUnkownEntities());
        AlignmentParser parser = INRIAFormatParser.getInstance();
        parser.setParsingLevel(ParsingLevel.THREE);
        parser.parse(reader1);
    }

    /**
     * If an Alignment contains OWLEntities with IRIs contained in XML-String
     * an {@link AlignmentParserException} is thrown.
     * @throws Exception
     */
    @Test
    public final void testParseMultiplicityExceptionLevelONE() throws Exception {
        StringReader reader1 = new StringReader(buildValidXMLDocumentWithMultipleEntitiesSameType());
        AlignmentParser parser = INRIAFormatParser.getInstance();
        parser.setParsingLevel(ParsingLevel.ONE);
        Alignment alignment = parser.parse(reader1);
        assertEquals(alignment.getCorrespondences().size(), 2);
    }

    /**
     * If an Alignment contains OWLEntities with IRIs contained in XML-String
     * an {@link AlignmentParserException} is thrown.
     * @throws Exception
     */
    @Test(expected = AlignmentParserException.class)
    public final void testParseMultiplicityExceptionLEVELTWO() throws Exception {
        StringReader reader1 = new StringReader(buildValidXMLDocumentWithMultipleEntitiesSameType());
        AlignmentParser parser = INRIAFormatParser.getInstance();
        parser.setParsingLevel(ParsingLevel.TWO);
        parser.parse(reader1);
    }

    /**
     * If an Alignment contains OWLEntities with IRIs contained in XML-String
     * an {@link AlignmentParserException} is thrown.
     * @throws Exception
     */
    @Test(expected = AlignmentParserException.class)
    public final void testParseMultiplicityExceptionLEVELTHREE() throws Exception {
        StringReader reader1 = new StringReader(buildValidXMLDocumentWithMultipleEntitiesSameType());
        AlignmentParser parser = INRIAFormatParser.getInstance();
        parser.setParsingLevel(ParsingLevel.THREE);
        parser.parse(reader1);
    }

    /**
     * If an Alignment contains OWLEntities with IRIs contained in XML-String
     * an {@link AlignmentParserException} is thrown.
     */
    @Test
    public final void testParseParseRenderParse() throws Exception {
        StringReader reader1 = new StringReader(buildValidXMLDocument());
        AlignmentParser parser = INRIAFormatParser.getInstance();
        Alignment alignment1 = parser.parse(reader1);
        StringWriter swriter = new StringWriter();
        PrintWriter pwriter = new PrintWriter(swriter);
        AlignmentRenderer renderer = INRIAFormatRenderer.getInstance(pwriter);
        renderer.render(alignment1);
        reader1 = new StringReader(swriter.toString());
        parser = INRIAFormatParser.getInstance();
        Alignment alignment2 = parser.parse(reader1);
        assertEquals(alignment1, alignment2);
    }

    /**
     * An alignment of ontologies where no physical URL is given is parsed.
     * It must contain the same content as the parser of the INRIA Alignment API
     * returns.
     * 
     * @throws Exception
     */
    @Test
    public final void testRealAlignmentNoLocation() throws Exception {
        if (!checkRemoteAccessibility(INRIA_format_no_location)) {
            System.err.println("Warning: Test \"testRealAlignmentNoLocation\" skipped " + "because remote URL not reachable.");
            return;
        }
        Reader reader = new BufferedReader(new InputStreamReader(INRIA_format_no_location.openStream()));
        AlignmentParser parser = INRIAFormatParser.getInstance();
        Alignment kadmosAlignment = parser.parse(reader);
        fr.inrialpes.exmo.align.parser.AlignmentParser inriaParser = new fr.inrialpes.exmo.align.parser.AlignmentParser(0);
        org.semanticweb.owl.align.Alignment inriaAlignment = inriaParser.parse(INRIA_format_no_location.openStream());
        assertEquals(inriaAlignment.getOntology1URI(), kadmosAlignment.getOntology1().getOntologyID().getOntologyIRI().toURI());
        assertEquals(inriaAlignment.getOntology2URI(), kadmosAlignment.getOntology2().getOntologyID().getOntologyIRI().toURI());
        Enumeration<Cell> enumm = inriaAlignment.getElements();
        while (enumm.hasMoreElements()) {
            Cell cell = enumm.nextElement();
            URI ent1URI = cell.getObject1AsURI();
            URI ent2URI = cell.getObject2AsURI();
            assertTrue(kadmosAlignment.contains(IRI.create(ent1URI), IRI.create(ent2URI)));
        }
        for (Correspondence<? extends OWLEntity> corr : kadmosAlignment) {
            URI ent1URI = corr.getEntity1().getIRI().toURI();
            URI ent2URI = corr.getEntity2().getIRI().toURI();
            boolean corrContained = false;
            enumm = inriaAlignment.getElements();
            while (enumm.hasMoreElements()) {
                Cell cell = enumm.nextElement();
                if (cell.getObject1AsURI().equals(ent1URI) && cell.getObject2AsURI().equals(ent2URI)) corrContained = true;
            }
            assertTrue(corrContained);
        }
    }

    /**
     * An alignment of ontologies where a physical URL is given is parsed.
     * It must contain the same content as the parser of the INRIA Alignment API
     * returns.
     * 
     * @throws Exception
     */
    @Test
    public final void testRealAlignmentWithLocation() throws Exception {
        if (!checkRemoteAccessibility(INRIA_format_with_location)) {
            System.err.println("Warning: Test \"testRealAlignmentWithLocation\" skipped " + "because remote URL not reachable.");
            return;
        }
        Reader reader = new BufferedReader(new InputStreamReader(INRIA_format_with_location.openStream()));
        AlignmentParser parser = INRIAFormatParser.getInstance();
        Alignment kadmosAlignment = parser.parse(reader);
        fr.inrialpes.exmo.align.parser.AlignmentParser inriaParser = new fr.inrialpes.exmo.align.parser.AlignmentParser(0);
        org.semanticweb.owl.align.Alignment inriaAlignment = inriaParser.parse(INRIA_format_with_location.openStream());
        Enumeration<Cell> enumm = inriaAlignment.getElements();
        while (enumm.hasMoreElements()) {
            Cell cell = enumm.nextElement();
            URI ent1URI = cell.getObject1AsURI();
            URI ent2URI = cell.getObject2AsURI();
            assertTrue(kadmosAlignment.contains(IRI.create(ent1URI), IRI.create(ent2URI)));
        }
        for (Correspondence<? extends OWLEntity> corr : kadmosAlignment) {
            URI ent1URI = corr.getEntity1().getIRI().toURI();
            URI ent2URI = corr.getEntity2().getIRI().toURI();
            boolean corrContained = false;
            enumm = inriaAlignment.getElements();
            while (enumm.hasMoreElements()) {
                Cell cell = enumm.nextElement();
                if (cell.getObject1AsURI().equals(ent1URI) && cell.getObject2AsURI().equals(ent2URI)) corrContained = true;
            }
            assertTrue(corrContained);
        }
    }

    /**
     * No locations for the ontologies are provided in the parsing document. 
     * This test tests the use of externally provided locations for the ontologies.
     * The external ontology locations are provided as OWLOntology objects.
     */
    @Test
    public final void testMissingOntologySetOntologyOWLOntology() throws Exception {
        StringReader reader1 = new StringReader(buildValidXMLDocumentMissingOntologies());
        AlignmentParser parser = INRIAFormatParser.getInstance();
        OWLOntology ont1 = OWLManager.createOWLOntologyManager().loadOntologyFromOntologyDocument(IRI.create(ONTO_IRI));
        OWLOntology ont2 = OWLManager.createOWLOntologyManager().loadOntologyFromOntologyDocument(IRI.create(ONTO_IRI));
        parser.setOntology1(ont1);
        parser.setOntology2(ont2);
        Alignment alignment = parser.parse(reader1);
        assertTrue(ont1.getOntologyID().getOntologyIRI().toString().equalsIgnoreCase(alignment.getOntology1().getOntologyID().getOntologyIRI().toString()));
        assertTrue(ont2.getOntologyID().getOntologyIRI().toString().equalsIgnoreCase(alignment.getOntology2().getOntologyID().getOntologyIRI().toString()));
    }

    /**
     * No locations for the ontologies are provided in the parsing document. 
     * This test tests the use of externally provided locations for the ontologies.
     * The external ontology locations are provided as URL objects.
     */
    @Test
    public final void testMissingOntologySetOntologyURL() throws Exception {
        StringReader reader1 = new StringReader(buildValidXMLDocumentMissingOntologies());
        AlignmentParser parser = INRIAFormatParser.getInstance();
        OWLOntology ont1 = OWLManager.createOWLOntologyManager().loadOntologyFromOntologyDocument(INRIA_format_onto_iri.openStream());
        OWLOntology ont2 = OWLManager.createOWLOntologyManager().loadOntologyFromOntologyDocument(INRIA_format_onto_iri.openStream());
        parser.setOntology1Document(INRIA_format_onto_iri);
        parser.setOntology2Document(INRIA_format_onto_iri);
        Alignment alignment = parser.parse(reader1);
        assertTrue(ont1.getOntologyID().getOntologyIRI().toString().equalsIgnoreCase(alignment.getOntology1().getOntologyID().getOntologyIRI().toString()));
        assertTrue(ont2.getOntologyID().getOntologyIRI().toString().equalsIgnoreCase(alignment.getOntology2().getOntologyID().getOntologyIRI().toString()));
    }

    /**
     * No locations for the ontologies are provided in the parsing document. 
     * This test tests the use of externally provided locations for the ontologies.
     * The external ontology locations are provided as InputStream objects.
     */
    @Test
    public final void testMissingOntologySetOntologyInputStream() throws Exception {
        StringReader reader1 = new StringReader(buildValidXMLDocumentMissingOntologies());
        AlignmentParser parser = INRIAFormatParser.getInstance();
        OWLOntology ont1 = OWLManager.createOWLOntologyManager().loadOntologyFromOntologyDocument(INRIA_format_onto_iri.openStream());
        OWLOntology ont2 = OWLManager.createOWLOntologyManager().loadOntologyFromOntologyDocument(INRIA_format_onto_iri.openStream());
        parser.setOntology1Document(INRIA_format_onto_iri.openStream());
        parser.setOntology2Document(INRIA_format_onto_iri.openStream());
        Alignment alignment = parser.parse(reader1);
        assertTrue(ont1.getOntologyID().getOntologyIRI().toString().equalsIgnoreCase(alignment.getOntology1().getOntologyID().getOntologyIRI().toString()));
        assertTrue(ont2.getOntologyID().getOntologyIRI().toString().equalsIgnoreCase(alignment.getOntology2().getOntologyID().getOntologyIRI().toString()));
    }

    /**
     * Resovable locations for the ontologies are provided in the document to be parsed. 
     * This test tests if externally provided ontologies are used instead of using the locations
     * given in the document to be parsed.
     */
    @Test
    public final void testValidOntologySetOntologyOWLOntology() throws Exception {
        StringReader reader1 = new StringReader(buildValidXMLDocument());
        AlignmentParser parser = INRIAFormatParser.getInstance();
        OWLOntology ont1 = OWLManager.createOWLOntologyManager().loadOntologyFromOntologyDocument(IRI.create(ONTO_IRI_3));
        OWLOntology ont2 = OWLManager.createOWLOntologyManager().loadOntologyFromOntologyDocument(IRI.create(ONTO_IRI_3));
        parser.setOntology1(ont1);
        parser.setOntology2(ont2);
        Alignment alignment = parser.parse(reader1);
        assertTrue(ont1.getOntologyID().getOntologyIRI().toString().equalsIgnoreCase(alignment.getOntology1().getOntologyID().getOntologyIRI().toString()));
        assertTrue(ont2.getOntologyID().getOntologyIRI().toString().equalsIgnoreCase(alignment.getOntology2().getOntologyID().getOntologyIRI().toString()));
    }

    /**
     * The ontologies in the document to be parsed are provided but have unresolvable locations. 
     * This test tests if externally provided ontologies are used instead of trying to parse the ontology locations
     * in the document to be parsed.
     */
    @Test
    public final void testUnresolvableLocationOntologySetOntologyOWLOntology() throws Exception {
        StringReader reader1 = new StringReader(buildValidXMLDocumentUnresolvableOntologyLocation());
        AlignmentParser parser = INRIAFormatParser.getInstance();
        OWLOntology ont1 = OWLManager.createOWLOntologyManager().loadOntologyFromOntologyDocument(IRI.create(ONTO_IRI));
        OWLOntology ont2 = OWLManager.createOWLOntologyManager().loadOntologyFromOntologyDocument(IRI.create(ONTO_IRI));
        parser.setOntology1(ont1);
        parser.setOntology2(ont2);
        Alignment alignment = parser.parse(reader1);
        assertTrue(ont1.getOntologyID().getOntologyIRI().toString().equalsIgnoreCase(alignment.getOntology1().getOntologyID().getOntologyIRI().toString()));
        assertTrue(ont2.getOntologyID().getOntologyIRI().toString().equalsIgnoreCase(alignment.getOntology2().getOntologyID().getOntologyIRI().toString()));
    }

    /**
     * Creates an XML-String which is not well-formed due to two
     * root elements.
     * @return Non-well-formed XML-String.
     */
    private static String buildNotWellFormedXMLDocument() {
        return "<?xml version='1.0' encoding='utf-8' standalone='no'?>" + "<Alignment1></Alignment1>" + "<Alignment2></Alignment2>";
    }

    /**
     * Creates an XML-String with no root element.
     * @return XML-String with no root element.
     */
    private static String buildNoRootXMLDocument() {
        return "<?xml version='1.0' encoding='utf-8' standalone='no'?>";
    }

    /**
     * Creates an XML-String with a root element other than rdf:RDF.
     * @return XML-String with invalid root element.
     */
    private static String buildNoOntologiesXMLDocument() {
        return "<?xml version='1.0' encoding='utf-8' standalone='no'?>" + "<rdf:RDF xmlns:rdf='http://www.w3.org/1999/02/22-rdf-syntax-ns#'></rdf:RDF>";
    }

    /**
     * Creates a valid INRIA formated XML-String.
     * @return INRIA formated XML-String.
     */
    private static String buildValidXMLDocument() {
        return "<?xml version='1.0' encoding='utf-8' standalone='no'?>" + "<rdf:RDF xmlns='http://knowledgeweb.semanticweb.org/heterogeneity/alignment#' " + "xmlns:rdf='http://www.w3.org/1999/02/22-rdf-syntax-ns#' " + "xmlns:xsd='http://www.w3.org/2001/XMLSchema#' " + "xmlns:align='http://knowledgeweb.semanticweb.org/heterogeneity/alignment#'>" + "<Alignment><xml>yes</xml><level>0</level><type>11</type>" + "<onto1>" + "<Ontology rdf:about=\"http://www.w3.org/TR/2003/PR-owl-guide-20031209/wine\">" + "<location></location>" + "<formalism><Formalism align:name=\"OWL2.0\" align:uri=\"http://www.w3.org/2002/07/owl#\"/></formalism>" + "</Ontology>" + "</onto1>" + "<onto2>" + "<Ontology rdf:about=\"http://www.w3.org/TR/2003/PR-owl-guide-20031209/wine\">" + "<location></location>" + "<formalism><Formalism align:name=\"OWL2.0\" align:uri=\"http://www.w3.org/2002/07/owl#\"/></formalism>" + "</Ontology>" + "</onto2>" + "<map><Cell>" + "<entity1 rdf:resource='http://www.w3.org/TR/2003/PR-owl-guide-20031209/wine#madeFromGrape'/>" + "<entity2 rdf:resource='http://www.w3.org/TR/2003/PR-owl-guide-20031209/wine#madeFromGrape'/>" + "<relation>=</relation>" + "<measure rdf:datatype='http://www.w3.org/2001/XMLSchema#float'>1.0</measure>" + "</Cell></map>" + "<map><Cell>" + "<entity1 rdf:resource='http://www.w3.org/TR/2003/PR-owl-guide-20031209/wine#AlsaceRegion'/>" + "<entity2 rdf:resource='http://www.w3.org/TR/2003/PR-owl-guide-20031209/wine#AlsaceRegion'/>" + "<relation>=</relation>" + "<measure rdf:datatype='http://www.w3.org/2001/XMLSchema#float'>1.0</measure>" + "</Cell></map>" + "</Alignment></rdf:RDF>";
    }

    /**
     * Creates a valid INRIA formated XML-String.
     * @return INRIA formated XML-String.
     */
    private static String buildValidXMLDocumentWithUnkownEntities() {
        return "<?xml version='1.0' encoding='utf-8' standalone='no'?>" + "<rdf:RDF xmlns='http://knowledgeweb.semanticweb.org/heterogeneity/alignment#' " + "xmlns:rdf='http://www.w3.org/1999/02/22-rdf-syntax-ns#' " + "xmlns:xsd='http://www.w3.org/2001/XMLSchema#' " + "xmlns:align='http://knowledgeweb.semanticweb.org/heterogeneity/alignment#'>" + "<Alignment><xml>yes</xml><level>0</level><type>11</type>" + "<onto1>" + "<Ontology rdf:about=\"http://www.w3.org/TR/2003/PR-owl-guide-20031209/wine\">" + "<location></location>" + "<formalism><Formalism align:name=\"OWL2.0\" align:uri=\"http://www.w3.org/2002/07/owl#\"/></formalism>" + "</Ontology>" + "</onto1>" + "<onto2>" + "<Ontology rdf:about=\"http://www.w3.org/TR/2003/PR-owl-guide-20031209/wine\">" + "<location></location>" + "<formalism><Formalism align:name=\"OWL2.0\" align:uri=\"http://www.w3.org/2002/07/owl#\"/></formalism>" + "</Ontology>" + "</onto2>" + "<map><Cell>" + "<entity1 rdf:resource='http://www.w3.org/TR/2003/PR-owl-guide-20031209/wine#madeFromGrape'/>" + "<entity2 rdf:resource='http://www.w3.org/TR/2003/PR-owl-guide-20031209/wine#madeFromGrape'/>" + "<relation>=</relation>" + "<measure rdf:datatype='http://www.w3.org/2001/XMLSchema#float'>1.0</measure>" + "</Cell></map>" + "<map><Cell>" + "<entity1 rdf:resource='http://www.w3.org/TR/2003/PR-owl-guide-20031209/wine#AlsaceRegion'/>" + "<entity2 rdf:resource='http://www.w3.org/TR/2003/PR-owl-guide-20031209/wine#AlsaceRegion'/>" + "<relation>=</relation>" + "<measure rdf:datatype='http://www.w3.org/2001/XMLSchema#float'>1.0</measure>" + "</Cell></map>" + "<map><Cell>" + "<entity1 rdf:resource='http://www.w3.org/TR/2003/PR-owl-guide-20031209/wine#GreeceRegion'/>" + "<entity2 rdf:resource='http://www.w3.org/TR/2003/PR-owl-guide-20031209/wine#GreeceRegion'/>" + "<relation>=</relation>" + "<measure rdf:datatype='http://www.w3.org/2001/XMLSchema#float'>1.0</measure>" + "</Cell></map>" + "</Alignment></rdf:RDF>";
    }

    /**
     * Creates a valid INRIA formated XML-String.
     * @return INRIA formated XML-String.
     */
    private static String buildValidXMLDocumentWithMultipleEntitiesSameType() {
        return "<?xml version='1.0' encoding='utf-8' standalone='no'?>" + "<rdf:RDF xmlns='http://knowledgeweb.semanticweb.org/heterogeneity/alignment#' " + "xmlns:rdf='http://www.w3.org/1999/02/22-rdf-syntax-ns#' " + "xmlns:xsd='http://www.w3.org/2001/XMLSchema#' " + "xmlns:align='http://knowledgeweb.semanticweb.org/heterogeneity/alignment#'>" + "<Alignment><xml>yes</xml><level>0</level><type>11</type>" + "<onto1>" + "<Ontology rdf:about=\"http://www.w3.org/TR/2003/PR-owl-guide-20031209/wine\">" + "<location></location>" + "<formalism><Formalism align:name=\"OWL2.0\" align:uri=\"http://www.w3.org/2002/07/owl#\"/></formalism>" + "</Ontology>" + "</onto1>" + "<onto2>" + "<Ontology rdf:about=\"http://www.w3.org/TR/2003/PR-owl-guide-20031209/wine\">" + "<location></location>" + "<formalism><Formalism align:name=\"OWL2.0\" align:uri=\"http://www.w3.org/2002/07/owl#\"/></formalism>" + "</Ontology>" + "</onto2>" + "<map><Cell>" + "<entity1 rdf:resource='http://www.w3.org/TR/2003/PR-owl-guide-20031209/wine#madeFromGrape'/>" + "<entity2 rdf:resource='http://www.w3.org/TR/2003/PR-owl-guide-20031209/wine#madeFromGrape'/>" + "<relation>=</relation>" + "<measure rdf:datatype='http://www.w3.org/2001/XMLSchema#float'>1.0</measure>" + "</Cell></map>" + "<map><Cell>" + "<entity1 rdf:resource='http://www.w3.org/TR/2003/PR-owl-guide-20031209/wine#AlsaceRegion'/>" + "<entity2 rdf:resource='http://www.w3.org/TR/2003/PR-owl-guide-20031209/wine#AlsaceRegion'/>" + "<relation>=</relation>" + "<measure rdf:datatype='http://www.w3.org/2001/XMLSchema#float'>1.0</measure>" + "</Cell></map>" + "<map><Cell>" + "<entity1 rdf:resource='http://www.w3.org/TR/2003/PR-owl-guide-20031209/wine#AlsaceRegion'/>" + "<entity2 rdf:resource='http://www.w3.org/TR/2003/PR-owl-guide-20031209/wine#AlsaceRegion'/>" + "<relation>=</relation>" + "<measure rdf:datatype='http://www.w3.org/2001/XMLSchema#float'>1.0</measure>" + "</Cell></map>" + "</Alignment></rdf:RDF>";
    }

    /**
     * Creates an INRIA formated XML-String without providing the location of the ontologies.
     * @return INRIA formated XML-String.
     */
    private static String buildValidXMLDocumentMissingOntologies() {
        return "<?xml version='1.0' encoding='utf-8' standalone='no'?>" + "<rdf:RDF xmlns='http://knowledgeweb.semanticweb.org/heterogeneity/alignment#' " + "xmlns:rdf='http://www.w3.org/1999/02/22-rdf-syntax-ns#' " + "xmlns:xsd='http://www.w3.org/2001/XMLSchema#' " + "xmlns:align='http://knowledgeweb.semanticweb.org/heterogeneity/alignment#'>" + "<Alignment><xml>yes</xml><level>0</level><type>11</type>" + "<onto1>" + "<Ontology>" + "<location></location>" + "<formalism><Formalism align:name=\"OWL2.0\" align:uri=\"http://www.w3.org/2002/07/owl#\"/></formalism>" + "</Ontology>" + "</onto1>" + "<onto2>" + "<Ontology>" + "<location></location>" + "<formalism><Formalism align:name=\"OWL2.0\" align:uri=\"http://www.w3.org/2002/07/owl#\"/></formalism>" + "</Ontology>" + "</onto2>" + "<map><Cell>" + "<entity1 rdf:resource='http://www.w3.org/TR/2003/PR-owl-guide-20031209/wine#madeFromGrape'/>" + "<entity2 rdf:resource='http://www.w3.org/TR/2003/PR-owl-guide-20031209/wine#madeFromGrape'/>" + "<relation>=</relation>" + "<measure rdf:datatype='http://www.w3.org/2001/XMLSchema#float'>1.0</measure>" + "</Cell></map>" + "<map><Cell>" + "<entity1 rdf:resource='http://www.w3.org/TR/2003/PR-owl-guide-20031209/wine#AlsaceRegion'/>" + "<entity2 rdf:resource='http://www.w3.org/TR/2003/PR-owl-guide-20031209/wine#AlsaceRegion'/>" + "<relation>=</relation>" + "<measure rdf:datatype='http://www.w3.org/2001/XMLSchema#float'>1.0</measure>" + "</Cell></map>" + "</Alignment></rdf:RDF>";
    }

    /**
     * Creates a INRIA formated XML-String with a unresolvable location of ontologies.
     * @return INRIA formated XML-String.
     */
    private static String buildValidXMLDocumentUnresolvableOntologyLocation() {
        return "<?xml version='1.0' encoding='utf-8' standalone='no'?>" + "<rdf:RDF xmlns='http://knowledgeweb.semanticweb.org/heterogeneity/alignment#' " + "xmlns:rdf='http://www.w3.org/1999/02/22-rdf-syntax-ns#' " + "xmlns:xsd='http://www.w3.org/2001/XMLSchema#' " + "xmlns:align='http://knowledgeweb.semanticweb.org/heterogeneity/alignment#'>" + "<Alignment><xml>yes</xml><level>0</level><type>11</type>" + "<onto1>" + "<Ontology rdf:about=\"http://www.w3.org\">" + "<location></location>" + "<formalism><Formalism align:name=\"OWL2.0\" align:uri=\"http://www.w3.org/2002/07/owl#\"/></formalism>" + "</Ontology>" + "</onto1>" + "<onto2>" + "<Ontology rdf:about=\"http://www.w3.org\">" + "<location></location>" + "<formalism><Formalism align:name=\"OWL2.0\" align:uri=\"http://www.w3.org/2002/07/owl#\"/></formalism>" + "</Ontology>" + "</onto2>" + "<map><Cell>" + "<entity1 rdf:resource='http://www.w3.org/TR/2003/PR-owl-guide-20031209/wine#madeFromGrape'/>" + "<entity2 rdf:resource='http://www.w3.org/TR/2003/PR-owl-guide-20031209/wine#madeFromGrape'/>" + "<relation>=</relation>" + "<measure rdf:datatype='http://www.w3.org/2001/XMLSchema#float'>1.0</measure>" + "</Cell></map>" + "<map><Cell>" + "<entity1 rdf:resource='http://www.w3.org/TR/2003/PR-owl-guide-20031209/wine#AlsaceRegion'/>" + "<entity2 rdf:resource='http://www.w3.org/TR/2003/PR-owl-guide-20031209/wine#AlsaceRegion'/>" + "<relation>=</relation>" + "<measure rdf:datatype='http://www.w3.org/2001/XMLSchema#float'>1.0</measure>" + "</Cell></map>" + "</Alignment></rdf:RDF>";
    }

    /**
     * Helper method to check, whether URL is resolvable.
     * In case remote URLs are used in tests, the tests may be skipped
     * if the URLs are not resolvable
     * (e.g. because there is no internet connection).
     * @param url URL to be checked.
     * @return <code>true</code> iff content can be retrieved from URL,
     *         <code>false</code> otherwise.
     */
    private boolean checkRemoteAccessibility(URL url) {
        try {
            url.openConnection().getContent();
            return true;
        } catch (IOException e) {
            return false;
        }
    }

    /**
     * Resets the alignment parser to its initial state.
     * @param AlignmentParser to be reset.
     */
    private void resetParser(AlignmentParser parser) {
        parser.setParsingLevel(ParsingLevel.THREE);
        parser.setCorrespondenceFactory(SimpleCorrespondenceFactory.getInstance());
        parser.setAlignmentFactory(SimpleAlignmentFactory.getInstance());
    }

    /**
     * Adjusts the absolute URL of local file.
     */
    private static void absolutizeURL(URL resource) {
        try {
            File file = new File(resource.getFile());
            boolean rewrite = false;
            if (file.exists()) {
                SAXBuilder builder = new SAXBuilder();
                URL ontologiesURL = INRIAFormatParserTest.class.getResource("/ontologies");
                if (ontologiesURL == null) {
                    return;
                }
                Document document = (Document) builder.build(file);
                Element root = document.getRootElement();
                if (root != null) {
                    Element alignment = (Element) root.getChildren().get(0);
                    if (alignment.getName() == "Alignment") {
                        Element onto1 = (Element) alignment.getChildren().get(3);
                        if (onto1.getName() == "onto1") {
                            Element ontology = (Element) onto1.getChildren().get(0);
                            if (ontology.getName() == "Ontology") {
                                Element onto1_location = (Element) ontology.getChildren().get(0);
                                if (onto1_location.getName() == "location") {
                                    String onto1_location_string = onto1_location.getText();
                                    if (onto1_location_string != null) {
                                        File onto1_location_file = new File(onto1_location_string);
                                        if (onto1_location_file != null) {
                                            onto1_location.setText(ontologiesURL.toString() + "/" + onto1_location_file.getName());
                                            rewrite = true;
                                        }
                                    }
                                }
                            }
                        }
                        Element onto2 = (Element) alignment.getChildren().get(4);
                        if (onto2.getName() == "onto2") {
                            Element ontology = (Element) onto2.getChildren().get(0);
                            if (ontology.getName() == "Ontology") {
                                Element onto2_location = (Element) ontology.getChildren().get(0);
                                if (onto2_location.getName() == "location") {
                                    String onto2_location_string = onto2_location.getText();
                                    if (onto2_location_string != null) {
                                        File onto2_location_file = new File(onto2_location_string);
                                        if (onto2_location_file != null) {
                                            onto2_location.setText(ontologiesURL.toString() + "/" + onto2_location_file.getName());
                                            rewrite = true;
                                        }
                                    }
                                }
                            }
                        }
                    }
                }
                if (rewrite) {
                    String des = new XMLOutputter().outputString(document);
                    FileWriter fileWriter = new FileWriter(file);
                    fileWriter.write(des);
                    fileWriter.close();
                }
            } else {
                System.out.println("File does not exist");
            }
        } catch (IOException ex) {
            return;
        } catch (JDOMException e) {
            return;
        }
    }
}
