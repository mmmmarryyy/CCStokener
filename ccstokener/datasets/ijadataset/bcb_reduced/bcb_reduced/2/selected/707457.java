package de.fzi.kadmos.parser.impl;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.Reader;
import java.net.URL;
import javax.xml.parsers.ParserConfigurationException;
import javax.xml.parsers.SAXParser;
import javax.xml.parsers.SAXParserFactory;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.semanticweb.owlapi.apibinding.OWLManager;
import org.semanticweb.owlapi.model.IRI;
import org.semanticweb.owlapi.model.OWLDataFactory;
import org.semanticweb.owlapi.model.OWLEntity;
import org.semanticweb.owlapi.model.OWLOntology;
import org.semanticweb.owlapi.model.OWLOntologyCreationException;
import org.semanticweb.owlapi.model.OWLOntologyManager;
import org.xml.sax.Attributes;
import org.xml.sax.InputSource;
import org.xml.sax.SAXException;
import org.xml.sax.helpers.DefaultHandler;
import de.fzi.kadmos.api.Alignment;
import de.fzi.kadmos.api.AlignmentFactory;
import de.fzi.kadmos.api.Correspondence;
import de.fzi.kadmos.api.CorrespondenceFactory;
import de.fzi.kadmos.api.MultiplicityException;
import de.fzi.kadmos.api.impl.SimpleAlignmentFactory;
import de.fzi.kadmos.api.impl.SimpleCorrespondenceFactory;
import de.fzi.kadmos.parser.AlignmentParser;
import de.fzi.kadmos.parser.AlignmentParserException;
import de.fzi.kadmos.parser.ParsingLevel;

/**
 * Parser that reads and parses the
 * <a href="http://alignapi.gforge.inria.fr/format.html">INRIA alignment format</a>
 * from an XML representation.<br><br>
 *
 * Instances of this parser can only be obtained by the {@link #getInstance(Reader)} method.
 * Each instance is immutably associated with a particular input source,
 * specified by a {@link Reader}. However, an existing instance can be modified
 * by specifying a particular {@link CorrespondenceFactory}.<br><br>
 *
 * The parser uses the {@link SimpleCorrespondenceFactory} for creating correspondence
 * objects in case no specific {@link CorrespondenceFactory} is set via
 * {@link #setCorrespondenceFactory(CorrespondenceFactory)}.
 *
 * @author Juergen Bock, Matthias Stumpp
 * @version 1.2.0
 * @since 1.0.0
 * @see <a href="http://alignapi.gforge.inria.fr/format.html">http://alignapi.gforge.inria.fr/format.html</a>
 */
public final class INRIAFormatParser implements AlignmentParser {

    /**
     * Logger
     */
    private static final Log logger = LogFactory.getLog(INRIAFormatParser.class);

    /**
     * Bookkeeping of existing instance. There can only be a one
     * instance.
     */
    private static INRIAFormatParser instance;

    /**
     * {@link CorrespondenceFactory} to be used for creating
     * correspondence objects. A default will be set during
     * instance creation, which can be overwritten by calling
     * {@link #setCorespondenceFactory()}.
     */
    private CorrespondenceFactory corrFactory;

    /**
     * {@link CorrespondenceFactory} to be used for creating
     * correspondence objects. A default will be set during
     * instance creation, which can be overwritten by calling
     * {@link #setCorespondenceFactory()}.
     */
    private AlignmentFactory alignFactory;

    /**
     * The parsing level applied while parsing the alignment.
     */
    private ParsingLevel level;

    /**
     * Ontologies to be set in alignment if ontologies of parsing document should not be used.
     */
    private OWLOntology ont1, ont2;

    /**
     * Direct instantiation is disallowed. Use the {@link #getInstance()} 
     * method instead.           
     */
    private INRIAFormatParser() {
        level = ParsingLevel.THREE;
        corrFactory = SimpleCorrespondenceFactory.getInstance();
        alignFactory = SimpleAlignmentFactory.getInstance();
    }

    /**
     * Gets a (new) instance of this parser. For any {@link Reader}
     * instance there can only be one parser instance.
     *
     * @param reader The {@link Reader} providing the input to be parsed.
     * @return Parser instance.
     * @since 1.0.0
     */
    public static INRIAFormatParser getInstance() {
        if (instance == null) instance = new INRIAFormatParser();
        return instance;
    }

    /** {@inheritDoc} */
    @Override
    public void setCorrespondenceFactory(CorrespondenceFactory factory) {
        instance.corrFactory = factory;
    }

    /** {@inheritDoc} */
    @Override
    public void setAlignmentFactory(AlignmentFactory factory) {
        instance.alignFactory = factory;
    }

    /** {@inheritDoc} */
    @Override
    public void setOntology1Document(URL url1) throws IllegalArgumentException {
        if (url1 == null) throw new IllegalArgumentException("Input parameter URL for ontology 1 is null.");
        try {
            ont1 = OWLManager.createOWLOntologyManager().loadOntologyFromOntologyDocument(url1.openStream());
        } catch (IOException e) {
            throw new IllegalArgumentException("Cannot open stream for ontology 1 from given URL.");
        } catch (OWLOntologyCreationException e) {
            throw new IllegalArgumentException("Cannot load ontology 1 from given URL.");
        }
    }

    /** {@inheritDoc} */
    @Override
    public void setOntology2Document(URL url2) throws IllegalArgumentException {
        if (url2 == null) throw new IllegalArgumentException("Input parameter URL for ontology 2 is null.");
        try {
            ont2 = OWLManager.createOWLOntologyManager().loadOntologyFromOntologyDocument(url2.openStream());
        } catch (IOException e) {
            throw new IllegalArgumentException("Cannot open stream for ontology 2 from given URL.");
        } catch (OWLOntologyCreationException e) {
            throw new IllegalArgumentException("Cannot load ontology 2 from given URL.");
        }
    }

    /** {@inheritDoc} */
    @Override
    public void setOntology1Document(InputStream is1) throws IllegalArgumentException {
        if (is1 == null) throw new IllegalArgumentException("Input parameter InputStream for ontology 1 is null.");
        try {
            ont1 = OWLManager.createOWLOntologyManager().loadOntologyFromOntologyDocument(is1);
        } catch (OWLOntologyCreationException e) {
            throw new IllegalArgumentException("Cannot load ontology 1 from given InputStream.");
        }
    }

    /** {@inheritDoc} */
    @Override
    public void setOntology2Document(InputStream is2) throws IllegalArgumentException {
        if (is2 == null) throw new IllegalArgumentException("Input parameter InputStream for ontology 2 is null.");
        try {
            ont2 = OWLManager.createOWLOntologyManager().loadOntologyFromOntologyDocument(is2);
        } catch (OWLOntologyCreationException e) {
            throw new IllegalArgumentException("Cannot load ontology 2 from given InputStream.");
        }
    }

    /** {@inheritDoc} */
    @Override
    public void setOntology1(OWLOntology ont1) throws IllegalArgumentException {
        if (ont1 == null) throw new IllegalArgumentException("Input parameter OWLOntology for ontology 1 is null.");
        this.ont1 = ont1;
    }

    /** {@inheritDoc} */
    @Override
    public void setOntology2(OWLOntology ont2) throws IllegalArgumentException {
        if (ont2 == null) throw new IllegalArgumentException("Input parameter OWLOntology for ontology 2 is null.");
        this.ont2 = ont2;
    }

    /** {@inheritDoc} */
    @Override
    public Alignment parse(File file) throws AlignmentParserException, IllegalArgumentException, FileNotFoundException {
        if (file != null && file.isFile()) {
            BufferedReader reader = new BufferedReader(new FileReader(file));
            INRIAParser parser = new INRIAParser(reader, ont1, ont2);
            return parser.parse(level);
        } else {
            throw new IllegalArgumentException("File is null or file does not exist.");
        }
    }

    /** {@inheritDoc} */
    @Override
    public Alignment parse(String string) throws AlignmentParserException, IllegalArgumentException, FileNotFoundException {
        if (string != null & !string.isEmpty()) {
            BufferedReader reader;
            try {
                URL url = new URL(string);
                reader = new BufferedReader(new InputStreamReader(url.openStream()));
                INRIAParser parser = new INRIAParser(reader, ont1, ont2);
                return parser.parse(level);
            } catch (Exception e1) {
                File file = new File(string);
                reader = new BufferedReader(new FileReader(file));
                INRIAParser parser = new INRIAParser(reader, ont1, ont2);
                return parser.parse(level);
            }
        } else {
            throw new IllegalArgumentException("String is null or empty.");
        }
    }

    /** {@inheritDoc} */
    @Override
    public Alignment parse(URL url) throws AlignmentParserException, IllegalArgumentException, IOException {
        if (url != null) {
            BufferedReader reader = new BufferedReader(new InputStreamReader(url.openStream()));
            INRIAParser parser = new INRIAParser(reader, ont1, ont2);
            return parser.parse(level);
        } else {
            throw new IllegalArgumentException("URL is null.");
        }
    }

    /** {@inheritDoc} */
    @Override
    public Alignment parse(Reader reader) throws AlignmentParserException, IllegalArgumentException, IOException {
        if (reader != null) {
            INRIAParser parser = new INRIAParser(reader, ont1, ont2);
            return parser.parse(level);
        } else {
            throw new IllegalArgumentException("Reader is null.");
        }
    }

    /** {@inheritDoc} */
    @Override
    public Alignment parse(InputStream inputStream) throws AlignmentParserException, IllegalArgumentException {
        if (inputStream != null) {
            BufferedReader reader = null;
            reader = new BufferedReader(new InputStreamReader(inputStream));
            INRIAParser parser = new INRIAParser(reader, ont1, ont2);
            return parser.parse(level);
        } else {
            throw new IllegalArgumentException("InputStream is null.");
        }
    }

    /** {@inheritDoc} */
    @Override
    public Alignment parse(InputSource inputSource) throws AlignmentParserException {
        if (inputSource != null) {
            BufferedReader reader = null;
            reader = new BufferedReader(inputSource.getCharacterStream());
            INRIAParser parser = new INRIAParser(reader, ont1, ont2);
            return parser.parse(level);
        } else {
            throw new AlignmentParserException("InputSource is null.");
        }
    }

    /** {@inheritDoc} */
    @Override
    public void setParsingLevel(ParsingLevel level) {
        instance.level = level;
    }

    /**
     * Actual parser class, serving as handler for SAXParser            
     */
    private class INRIAParser extends DefaultHandler {

        /**
         * The reader for this parser.
         */
        private Reader reader;

        /**
         * The ontologies which are currently processed while parsing the XML document.
         */
        private OWLOntology ont1, ont2;

        /**
         * The OWLEntity objects which are currently processed while parsing the XML document.
         */
        private OWLEntity entity1, entity2;

        /**
         * The OWLOntologyManager objects which are currently processed while parsing the XML document.
         */
        private OWLOntologyManager manager1, manager2;

        /**
         * The OWLDataFactory objects which are currently processed while parsing the XML document.
         */
        private OWLDataFactory factory1, factory2;

        /**
         * The correspondence object which is currently processed while parsing the XML document.
         */
        private Correspondence<? extends OWLEntity> corr;

        /**
         * The alignment object which will receive the parsing results.
         */
        private Alignment alignment;

        private ParsingLevel level;

        private float measure_text;

        private String tempText;

        private int ont = 0;

        private boolean alignmentCreated = false;

        private boolean processNext = false;

        private int cellsVisited = 0;

        private int cellsAdded = 0;

        private String ont1_rdf_about_attr;

        private String ont2_rdf_about_attr;

        private INRIAParser(Reader reader) {
            this.reader = reader;
            this.manager1 = OWLManager.createOWLOntologyManager();
            this.manager2 = OWLManager.createOWLOntologyManager();
            this.factory1 = manager1.getOWLDataFactory();
            this.factory2 = manager2.getOWLDataFactory();
        }

        private INRIAParser(Reader reader, OWLOntology ont1, OWLOntology ont2) {
            this.reader = reader;
            this.ont1 = ont1;
            this.ont2 = ont2;
            this.manager1 = OWLManager.createOWLOntologyManager();
            this.manager2 = OWLManager.createOWLOntologyManager();
            this.factory1 = manager1.getOWLDataFactory();
            this.factory2 = manager2.getOWLDataFactory();
        }

        /**
         * Starts the parsing process considering a certain parsing level            
         */
        private Alignment parse(ParsingLevel level) throws AlignmentParserException {
            this.level = level;
            SAXParserFactory spf = SAXParserFactory.newInstance();
            try {
                SAXParser sp = spf.newSAXParser();
                logger.info("Parsing provided document...");
                sp.parse(new InputSource(reader), this);
                logger.info("Parsing finished.");
            } catch (SAXException se) {
                throw new AlignmentParserException(se.getMessage(), se);
            } catch (ParserConfigurationException pce) {
                throw new AlignmentParserException(pce.getMessage(), pce);
            } catch (IOException ie) {
                throw new AlignmentParserException(ie.getMessage(), ie);
            }
            if (alignment == null) {
                throw new AlignmentParserException("Alignment object has not been created because ontologies aren't provided.");
            } else {
                return alignment;
            }
        }

        @Override
        public void startElement(String uri, String localName, String qName, Attributes attributes) throws SAXException {
            String attribute;
            try {
                if (qName.equalsIgnoreCase("onto1")) {
                    logger.trace("Processing \"onto1\" element...");
                    ont = 1;
                } else if (qName.equalsIgnoreCase("onto2")) {
                    logger.trace("Processing \"onto2\" element...");
                    ont = 2;
                } else if (qName.equalsIgnoreCase("Ontology")) {
                    logger.trace("Processing \"Ontology\" element...");
                    attribute = attributes.getValue("rdf:about");
                    if (attribute != null) {
                        logger.trace("\"Ontology\" element has an attribute with name \"rdf:about\".");
                        if (ont == 1) {
                            ont1_rdf_about_attr = attribute;
                        } else if (ont == 2) {
                            ont2_rdf_about_attr = attribute;
                        } else {
                            logger.trace("Cannot process attribute with name \"rdf:about\" because ontology is unknown.");
                        }
                    } else {
                        logger.trace("\"Ontology\" element does not have an attribute with name \"rdf:about\".");
                    }
                } else if (qName.equalsIgnoreCase("Cell")) {
                    logger.trace("Processing \"Cell\" element...");
                    processNext = false;
                    cellsVisited++;
                } else if (qName.equalsIgnoreCase("entity1")) {
                    if (alignmentCreated) {
                        if (!processNext) {
                            logger.trace("Processing \"entity1\" element...");
                            attribute = attributes.getValue("rdf:resource");
                            if (attribute != null) {
                                OWLEntity entityTemp = processAttribute(ont1, factory1, attribute);
                                if (entityTemp != null) {
                                    logger.trace("OWLEntity object " + attribute + " for \"entity1\" created.");
                                    entity1 = entityTemp;
                                } else {
                                    if (level.toProcessAPE()) {
                                        throw new AlignmentParserException("Cannot create OWLEntity object " + attribute + " for element \"entity1\".");
                                    } else {
                                        logger.trace("Cannot create OWLEntity object " + attribute + " for element \"entity1\".");
                                        logger.trace("Skipped processing of \"entity1\" element.");
                                        processNext = true;
                                    }
                                }
                            } else {
                                if (level.toProcessAPE()) {
                                    throw new AlignmentParserException("Element \"entity1\" does not have an attribute called \"rdf:resource\".");
                                } else {
                                    logger.trace("Element \"entity1\" does not have an attribute called \"rdf:resource\".");
                                    logger.trace("Skipped processing of \"entity1\" element.");
                                    processNext = true;
                                }
                            }
                        } else {
                            logger.trace("Skipped processing of \"entity1\" element.");
                        }
                    } else {
                        throw new AlignmentParserException("Cannot process \"entity1\" element because Alignment object has not been created.");
                    }
                } else if (qName.equalsIgnoreCase("entity2")) {
                    if (alignmentCreated) {
                        if (!processNext) {
                            logger.trace("Processing \"entity2\" element...");
                            attribute = attributes.getValue("rdf:resource");
                            if (attribute != null) {
                                OWLEntity entityTemp = processAttribute(ont2, factory2, attribute);
                                if (entityTemp != null) {
                                    logger.trace("OWLEntity object " + attribute + " for entity2 created.");
                                    entity2 = entityTemp;
                                } else {
                                    if (level.toProcessAPE()) {
                                        throw new AlignmentParserException("Cannot create OWLEntity for element \"entity2\".");
                                    } else {
                                        logger.trace("Cannot create OWLEntity object " + attribute + " for element \"entity2\".");
                                        logger.trace("Skipped processing of \"entity2\" element.");
                                        processNext = true;
                                    }
                                }
                            } else {
                                if (level.toProcessAPE()) {
                                    throw new AlignmentParserException("Element \"entity2\" does not have an attribute called \"rdf:resource\".");
                                } else {
                                    logger.trace("Element \"entity2\" does not have an attribute called \"rdf:resource\".");
                                    logger.trace("Skipped processing of \"entity2\" element.");
                                    processNext = true;
                                }
                            }
                        } else {
                            logger.trace("Skipped processing of \"entity2\" element.");
                        }
                    } else {
                        throw new AlignmentParserException("Cannot process \"entity2\" element because Alignment object has not been created.");
                    }
                }
            } catch (AlignmentParserException e) {
                throw new SAXException(e.getMessage());
            }
        }

        @Override
        public void characters(char[] ch, int start, int length) throws SAXException {
            tempText = new String(ch, start, length);
        }

        @Override
        public void endElement(String uri, String localName, String qName) throws SAXException {
            try {
                if (qName.equalsIgnoreCase("onto1")) {
                    logger.trace("Processing \"onto1\" element.");
                    if (ont == 1 && ont1 == null && tempText != null) {
                        try {
                            ont1 = manager1.loadOntology(IRI.create(tempText));
                            logger.debug("Created OWLOntology object from URI and used it as ontology 1.");
                        } catch (OWLOntologyCreationException e) {
                            throw new AlignmentParserException("Could not create OWLOntology object from " + tempText);
                        }
                    }
                } else if (qName.equalsIgnoreCase("onto2")) {
                    logger.trace("Processing \"onto2\" element.");
                    if (ont == 2 && ont2 == null && tempText != null) {
                        try {
                            ont2 = manager2.loadOntology(IRI.create(tempText));
                            logger.debug("Created OWLOntology object from URI and used it as ontology 2.");
                        } catch (OWLOntologyCreationException e) {
                            throw new AlignmentParserException("Could not create OWLOntology object from " + tempText);
                        }
                    }
                } else if (qName.equalsIgnoreCase("location")) {
                    logger.trace("Processing \"location\" element.");
                    if (ont == 1 && ont1 == null && tempText != null) {
                        try {
                            ont1 = manager1.loadOntologyFromOntologyDocument(IRI.create(tempText));
                            logger.trace("Created OWLOntology object from ontology document and used it as ontology 1.");
                        } catch (OWLOntologyCreationException e) {
                            if (level.toProcessAPE()) {
                                throw new AlignmentParserException("Could not create OWLOntology object from ontology document.");
                            } else {
                                logger.trace("Could not create OWLOntology object from ontology document.");
                            }
                        }
                    } else if (ont == 2 && ont2 == null && tempText != null) {
                        try {
                            ont2 = manager2.loadOntologyFromOntologyDocument(IRI.create(tempText));
                            logger.trace("Created OWLOntology object from ontology document and used it as ontology 2.");
                        } catch (OWLOntologyCreationException e) {
                            if (level.toProcessAPE()) {
                                throw new AlignmentParserException("Could not create OWLOntology object from ontology document.");
                            } else {
                                logger.trace("Could not create OWLOntology object from ontology document.");
                            }
                        }
                    }
                } else if (qName.equalsIgnoreCase("Ontology")) {
                    logger.trace("Processing \"Ontology\" element.");
                    if (ont == 1 && ont1 == null) {
                        if (ont1_rdf_about_attr != null) {
                            try {
                                ont1 = manager1.loadOntology(IRI.create(ont1_rdf_about_attr));
                                logger.trace("Created OWLOntology object from URI " + ont1_rdf_about_attr + " and used it as ontology 1.");
                            } catch (OWLOntologyCreationException e) {
                                throw new AlignmentParserException("Could not create OWLOntology object as ontology 1 from " + ont1_rdf_about_attr);
                            }
                        } else {
                            throw new AlignmentParserException("Could not create OWLOntology object as ontology 1 because neither \"location\" element nor ontology \"rdf:about\" attribute is provided.");
                        }
                    } else if (ont == 2 && ont2 == null) {
                        if (ont2_rdf_about_attr != null) {
                            try {
                                ont2 = manager2.loadOntology(IRI.create(ont2_rdf_about_attr));
                                logger.trace("Created OWLOntology object from URI " + ont2_rdf_about_attr + " and used it as ontology 2.");
                            } catch (OWLOntologyCreationException e) {
                                throw new AlignmentParserException("Could not create OWLOntology object as ontology 2 from " + ont2_rdf_about_attr);
                            }
                        } else {
                            throw new AlignmentParserException("Could not create OWLOntology object as ontology 2 because neither \"location\" element nor ontology \"rdf:about\" attribute is provided.");
                        }
                    }
                } else if (qName.equalsIgnoreCase("Cell")) {
                    if (alignmentCreated) {
                        if (!processNext) {
                            if (entity1 != null && entity2 != null) {
                                corr = instance.corrFactory.createCorrespondence(entity1, entity2, measure_text);
                                if (corr != null) {
                                    try {
                                        alignment.addCorrespondence(corr);
                                        logger.debug("Correspondence object successfully added to Alignment object.");
                                        cellsAdded++;
                                    } catch (MultiplicityException e) {
                                        if (level.toProcessMPE()) {
                                            throw new AlignmentParserException("Correspondence object contains one or two OWLEntity objects contained in another Correspondence object already added to Alignment object.");
                                        } else {
                                            logger.trace("Correspondence object contains one or two OWLEntity objects contained in another Correspondence object already added to Alignment.");
                                            processNext = true;
                                        }
                                    }
                                    entity1 = null;
                                    entity2 = null;
                                } else {
                                    if (level.toProcessAPE()) {
                                        throw new AlignmentParserException("Correspondence object could not be created correctly.");
                                    } else {
                                        logger.debug("Correspondence object could not be created correctly.");
                                        processNext = true;
                                    }
                                }
                            } else {
                                if (level.toProcessAPE()) {
                                    throw new AlignmentParserException("Element \"entity1\" and/or \"entity2\" are null.");
                                } else {
                                    logger.trace("Element \"entity1\" and/or element \"entity2\" are null.");
                                    processNext = true;
                                    entity1 = null;
                                    entity2 = null;
                                }
                            }
                        } else {
                            logger.trace("Skipped processing of \"Cell\" element.");
                        }
                    } else {
                        throw new AlignmentParserException("Cannot process \"Cell\" element because Alignment object has not been created.");
                    }
                } else if (qName.equalsIgnoreCase("measure")) {
                    measure_text = Float.parseFloat(tempText);
                } else if (qName.equalsIgnoreCase("Alignment")) {
                    if (alignmentCreated) {
                        logger.info("Cell elements visited: " + cellsVisited);
                        logger.info("Correspondences added: " + cellsAdded);
                        logger.info(alignment);
                    } else {
                        logger.info("Alignment object has not been created.");
                    }
                }
                if (!alignmentCreated && ont1 != null && ont2 != null) {
                    logger.debug("Create SimpleAlignment object with ontology 1 and 2.");
                    alignment = instance.alignFactory.createAlignment(ont1, ont2);
                    alignmentCreated = true;
                }
                tempText = null;
            } catch (AlignmentParserException e) {
                throw new SAXException(e.getMessage(), e);
            }
        }

        /**
         * Creates a new instance of OWLEntity for a given rdf:ressource
         * @param ontology The OWLOntology potentially containing OWLEntity for a given rdf:ressource.
         * @param factory The OWLDataFactory used to create an instance of OWLEntity.
         * @param attribute The attribute an instance of OWLEntity to be created from.
         * @return Parser instance.
         * @since 1.0
         */
        private OWLEntity processAttribute(OWLOntology ontology, OWLDataFactory factory, String attribute) throws AlignmentParserException {
            OWLEntity entity = null;
            if (ontology.containsAnnotationPropertyInSignature(IRI.create(attribute))) {
                entity = factory.getOWLAnnotationProperty(IRI.create(attribute));
            } else if (ontology.containsClassInSignature(IRI.create(attribute))) {
                entity = factory.getOWLClass(IRI.create(attribute));
            } else if (ontology.containsDataPropertyInSignature(IRI.create(attribute))) {
                entity = factory.getOWLDataProperty(IRI.create(attribute));
            } else if (ontology.containsDatatypeInSignature(IRI.create(attribute))) {
                entity = factory.getOWLDatatype(IRI.create(attribute));
            } else if (ontology.containsIndividualInSignature(IRI.create(attribute))) {
                entity = factory.getOWLNamedIndividual(IRI.create(attribute));
            } else if (ontology.containsObjectPropertyInSignature(IRI.create(attribute))) {
                entity = factory.getOWLObjectProperty(IRI.create(attribute));
            } else {
                if (level.toProcessAPE()) {
                    throw new AlignmentParserException("Didn't find " + attribute + " in ontology " + ontology);
                } else {
                    logger.trace("Didn't find " + attribute + " in ontology " + ontology);
                    processNext = true;
                }
            }
            return entity;
        }
    }
}
