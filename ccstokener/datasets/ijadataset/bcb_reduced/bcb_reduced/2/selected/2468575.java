package de.fzi.kadmos.webutils.core;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileWriter;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.PrintWriter;
import java.io.Reader;
import java.io.StringWriter;
import java.net.HttpURLConnection;
import java.net.MalformedURLException;
import java.net.URL;
import java.net.URLConnection;
import java.util.Iterator;
import java.util.List;
import java.util.Properties;
import javax.annotation.Resource;
import javax.jws.WebService;
import javax.servlet.ServletContext;
import javax.xml.ws.WebServiceContext;
import javax.xml.ws.handler.MessageContext;
import org.semanticweb.owlapi.apibinding.OWLManager;
import org.semanticweb.owlapi.model.IRI;
import org.semanticweb.owlapi.model.OWLOntology;
import org.semanticweb.owlapi.model.OWLOntologyCreationException;
import org.semanticweb.owlapi.model.OWLOntologyManager;
import de.fzi.kadmos.api.algorithm.AlignmentAlgorithmException;
import de.fzi.kadmos.kadmos_webutils.wsdl.KADMOSFault;
import de.fzi.kadmos.kadmos_webutils.wsdl.KADMOSAlignmentService;
import de.fzi.kadmos.parser.AlignmentParser;
import de.fzi.kadmos.parser.AlignmentParserException;
import de.fzi.kadmos.parser.impl.INRIAFormatParser;
import de.fzi.kadmos.renderer.AlignmentRenderer;
import de.fzi.kadmos.renderer.impl.INRIAFormatRenderer;
import de.fzi.kadmos.resources.ArgumentType;
import de.fzi.kadmos.api.Alignment;
import de.fzi.kadmos.api.IncompatibleAlignmentsException;
import de.fzi.mapevo.algorithm.MapEVOAlignmentAlgorithm;
import de.fzi.mappso.align.MapPSOAlignmentAlgorithm;

/**
 * Implementation of the endpoint interface {@link KADMOSAlignmentService}.
 *
 * @author Matthias Stumpp
 * @version 1.2.0
 * @since 1.1.0
 */
@WebService(portName = "KADMOSAlignmentServicePort", serviceName = "KADMOSAlignmentService", targetNamespace = "http://kadmos.fzi.de/kadmos-webutils/wsdl", endpointInterface = "de.fzi.kadmos.kadmos_webutils.wsdl.KADMOSAlignmentService")
public class KADMOSAlignmentServiceImpl implements KADMOSAlignmentService {

    /**
     * File name of the resulting alignment file.
	 */
    private String fileName = "alignment_" + System.currentTimeMillis();

    /**
     * File type of the resulting alignment file.
	 */
    private String fileType = "rdf";

    /**
     * Ontology 1.
	 */
    private static OWLOntology ont1 = null;

    /**
     * Ontology 2.
	 */
    private static OWLOntology ont2 = null;

    /**
     * Logger.
	 */
    protected Log logger = LogFactory.getLog(KADMOSAlignmentServiceImpl.class);

    /**
     * WebServiceContext.
	 */
    @Resource
    private WebServiceContext context;

    /**
	 * {@inheritDoc}
	 *
	 * Implementation of the endpoint service interface {@link KADMOSAlignmentService}.
	 */
    @Override
    public String align(String ontology1, String ontology2, String inputAlignment, List<ArgumentType> parameters) throws KADMOSFault {
        if (ontology1 == null || ontology1.isEmpty()) throw new KADMOSFault("Parameter \"ontology 1\" must be provided!");
        if (ontology2 == null || ontology2.isEmpty()) throw new KADMOSFault("Parameter \"ontology 2\" must be provided!");
        OWLOntologyManager manager = OWLManager.createOWLOntologyManager();
        if (!isRemoteAccessibility(ontology1)) throw new KADMOSFault("Ontology given by parameter \"ontology 1\" is not accessible");
        try {
            ont1 = manager.loadOntologyFromOntologyDocument(IRI.create(ontology1));
        } catch (OWLOntologyCreationException e) {
            logger.debug(e.getMessage());
            throw new KADMOSFault("Cannot create \"ontology 1\" from parameter \"ontology 1\"");
        }
        if (!isRemoteAccessibility(ontology2)) throw new KADMOSFault("Ontology given by parameter \"ontology 2\" is not accessible");
        try {
            ont2 = manager.loadOntologyFromOntologyDocument(IRI.create(ontology2));
        } catch (OWLOntologyCreationException e) {
            logger.debug(e.getMessage());
            throw new KADMOSFault("Cannot create \"ontology 2\" from parameter \"ontology 2\"");
        }
        logger.info("URI of Ontology 1: " + ontology1);
        logger.info("URI of Ontology 2: " + ontology2);
        Alignment inputAlignmentTmp = null;
        if (inputAlignment != null) {
            if (inputAlignment.isEmpty()) throw new KADMOSFault("Parameter \"inputAlignment\" is empty!");
            URL url;
            try {
                url = new URL(inputAlignment);
            } catch (MalformedURLException e) {
                logger.debug(e.getMessage());
                throw new KADMOSFault("Provided URL of input alignment is malformed!");
            }
            Reader reader;
            try {
                reader = new BufferedReader(new InputStreamReader(url.openStream()));
            } catch (IOException e) {
                logger.debug(e.getMessage());
                throw new KADMOSFault("Cannot read in alignment provided!");
            }
            AlignmentParser parser = INRIAFormatParser.getInstance();
            try {
                inputAlignmentTmp = parser.parse(reader);
            } catch (AlignmentParserException e) {
                logger.debug(e.getMessage());
                throw new KADMOSFault("Cannot parse alignment from provided URL!");
            } catch (IllegalArgumentException e) {
                logger.debug(e.getMessage());
                throw new KADMOSFault("Cannot parse alignment from provided URL!");
            } catch (IOException e) {
                logger.debug(e.getMessage());
                throw new KADMOSFault("Cannot parse alignment from provided URL!");
            }
        }
        Properties properties = buildProperties(parameters);
        AlgorithmType algoType = AlgorithmType.MAPPSO;
        String algoTypeProp = properties.getProperty("algorithm");
        logger.info(algoTypeProp);
        if ((algoTypeProp != null) && AlgorithmType.contains(algoTypeProp)) {
            algoType = AlgorithmType.valueOf(algoTypeProp);
        }
        logger.info("Type of Algorithm used for alignment processing: " + algoType);
        logger.info("Start processing of alignment algorithm...");
        Alignment alignment = null;
        try {
            switch(algoType) {
                case MAPPSO:
                    logger.debug("Processing MapPSO starts...");
                    MapPSOAlignmentAlgorithm mapPSOAlgo = new MapPSOAlignmentAlgorithm();
                    mapPSOAlgo.setParameters(properties);
                    if (inputAlignmentTmp != null) {
                        alignment = mapPSOAlgo.align(ont1, ont2, inputAlignmentTmp);
                    } else {
                        alignment = mapPSOAlgo.align(ont1, ont2);
                    }
                    logger.debug("Processing MapPSO finished.");
                    break;
                case MAPEVO:
                    logger.debug("Processing MapEVO starts...");
                    MapEVOAlignmentAlgorithm mapEVOAlgo = new MapEVOAlignmentAlgorithm();
                    mapEVOAlgo.setParameters(properties);
                    alignment = mapEVOAlgo.align(ont1, ont2);
                    logger.debug("Processing MapEVO finished.");
                    break;
            }
        } catch (AlignmentAlgorithmException e) {
            logger.debug(e.getMessage());
            throw new KADMOSFault("An exception has been occured during alignment algorithm processing.", e);
        } catch (IncompatibleAlignmentsException e) {
            logger.debug(e.getMessage());
            throw new KADMOSFault("The input alignment provided is incompatible with the alignment algorithm");
        }
        logger.info("Finished processing of alignment algorithm.");
        logger.info("Preparing alignment to return...");
        StringWriter renderingBuffer = null;
        try {
            renderingBuffer = new StringWriter();
            PrintWriter writer = new PrintWriter(renderingBuffer);
            AlignmentRenderer renderer = INRIAFormatRenderer.getInstance(writer);
            renderer.render(alignment);
        } catch (IOException e) {
            logger.debug(e.getMessage());
            throw new KADMOSFault("An exception occured while preparing alignment for printing.");
        }
        String url = makeRemoteAccessibleFileFromAlignment(renderingBuffer.toString(), fileName, fileType);
        return url;
    }

    /**
	 * Returns real path of the ServletContext.
	 *
	 * @return String representing real path of the ServletContext.
	 * @throws KADMOSFault Exception if ServletContest in unknown.
	 */
    protected String getServletContextRealPath() throws KADMOSFault {
        if (context == null) throw new KADMOSFault("ServletContext is unknown.");
        ServletContext servletContext = (ServletContext) context.getMessageContext().get(MessageContext.SERVLET_CONTEXT);
        String servletContextRealPath = servletContext.getRealPath("/");
        return servletContextRealPath;
    }

    /**
	 * Checks whether a file represented by an URL is accessible.
	 *
	 * @param  URL of file to be checked.
	 * @return Boolean, true is file exists, false otherwise.
	 */
    protected boolean isRemoteAccessibility(String url) {
        try {
            URL targetURL = new URL(url);
            URLConnection urlConnection = targetURL.openConnection();
            urlConnection.setReadTimeout(10000);
            urlConnection.connect();
            if (urlConnection instanceof HttpURLConnection) {
                int responseCode = ((HttpURLConnection) urlConnection).getResponseCode();
                if (responseCode == 200) return true;
                return false;
            }
            return true;
        } catch (IOException e) {
            return false;
        }
    }

    /**
	 * Creates a remotely accessible file of a String representing an alignment.
	 *
	 * @param alignment String representing an alignment previously created.
	 * @param fileName Name of the file to be returned.
	 * @param fileType Type of the file to be returned.
	 * @return String String representing the URI pointing to the remotely accessible file containing the alignment.
	 * @throws KADMOSFault Exception thrown during file creation.
	 */
    protected String makeRemoteAccessibleFileFromAlignment(String alignment, String fileName, String fileType) throws KADMOSFault {
        File tempFile = makeFileFromAlignment(alignment, fileName, fileType);
        String url = getAlignmentURLPrefix() + File.separator + tempFile.getName();
        if (!isRemoteAccessibility(url)) throw new KADMOSFault("Could not create alignment file.");
        return url;
    }

    /**
	 * Creates a file of a String representing an alignment.
	 *
	 * @param alignment String representing an alignment previously created.
	 * @param fileName Name of the file to be returned.
	 * @param fileType Type of the file to be returned.
	 * @return File File containing the alignment.
	 * @throws KADMOSFault Exception thrown during file creation.
	 */
    protected File makeFileFromAlignment(String alignment, String fileName, String fileType) throws KADMOSFault {
        if (alignment == null) throw new KADMOSFault("Missing alignment while creating alignment file.");
        if (fileName == null || fileName.isEmpty()) throw new KADMOSFault("Missing fileName while creating alignment file.");
        if (fileType == null || fileType.isEmpty()) throw new KADMOSFault("Missing fileType while creating alignment file.");
        File tempFile = null;
        try {
            tempFile = File.createTempFile(fileName, "." + fileType, getTempDir());
            BufferedWriter out = new BufferedWriter(new FileWriter(tempFile));
            out.write(alignment);
            out.close();
        } catch (IOException e) {
            throw new KADMOSFault(e.getMessage());
        }
        if (tempFile == null || !tempFile.exists()) throw new KADMOSFault("File containing alignment could not be created.");
        return tempFile;
    }

    /**
	 * Returns URL to be prefixed to created alignment files in order to make them remotely accessible.
	 *
	 * @return String URL to be prefixed to created alignment files in order to make them remotely accessible.
	 * @throws KADMOSFault Exception thrown during creating URLPrefix.
	 */
    protected String getAlignmentURLPrefix() throws KADMOSFault {
        Properties properties = new Properties();
        try {
            properties.load(new FileInputStream(getServletContextRealPath() + "/endpoint.properties"));
        } catch (FileNotFoundException e) {
            throw new KADMOSFault("No endpoint.properties file found.");
        } catch (IOException e) {
            throw new KADMOSFault("No endpoint.properties file found.");
        }
        String host = properties.getProperty("host");
        String port = properties.getProperty("port");
        String context = properties.getProperty("endPointServicePath");
        String alignURLPrefix = "http://";
        if (host.isEmpty()) {
            logger.debug("Property \"host\" is empty.");
            throw new KADMOSFault("Property \"host\" is empty.");
        }
        alignURLPrefix += host;
        if (!port.isEmpty()) alignURLPrefix += ":" + port;
        if (!context.isEmpty()) alignURLPrefix += "/" + context;
        alignURLPrefix += "/alignments";
        logger.info("URL pattern for remote access of alignment is: " + alignURLPrefix);
        return alignURLPrefix;
    }

    /**
	 * Returns a directory for storing files containing alignments to be computed. 
	 * @return File directory for storing files containing alignments to be computed.
	 * @throws KADMOSFault Exception thrown if directory for storing files containing alignments has not been created 
	 *         and can't be created.
	 */
    private File getTempDir() throws KADMOSFault {
        File tempDir = new File(getServletContextRealPath() + "/alignments");
        if (!tempDir.isDirectory()) {
            if (!tempDir.mkdir()) {
                logger.debug("Directory for storing files containing alignments has not been created and can't be created, too.");
                throw new KADMOSFault("Directory for storing files containing alignments has not been created and can't be created, too.");
            }
        }
        logger.info("Alignments will be written to: " + tempDir.toString());
        return tempDir;
    }

    /**
	 * Creates an instance of {@link java.util.Properties} from a list of {@link ArgumentType}.
	 * @param parameters List of ArgumentType instances.
	 * @return Instance of java.util.Properties.
	 */
    private Properties buildProperties(List<ArgumentType> parameters) {
        logger.info("Processing properties...");
        Properties properties = new Properties();
        if (parameters == null || parameters.isEmpty()) {
            return properties;
        }
        Iterator<ArgumentType> argTypes = (Iterator<ArgumentType>) parameters.iterator();
        ArgumentType argType = null;
        while (argTypes.hasNext()) {
            argType = argTypes.next();
            properties.setProperty(argType.getName(), argType.getValue());
        }
        logger.info("Properties rebuild.");
        return properties;
    }
}
