package codec.gen;

import codec.gen.configuration.Configuration;
import codec.gen.configuration.Pipeline;
import codec.gen.configuration.PipelineType;
import codec.gen.configuration.impl.PipelineTypeImpl;
import codec.gen.parser.Asn1Parser;
import codec.gen.parser.SimpleNode;
import javax.xml.bind.JAXBContext;
import javax.xml.bind.JAXBException;
import javax.xml.bind.Marshaller;
import javax.xml.bind.Unmarshaller;
import javax.xml.parsers.SAXParser;
import javax.xml.parsers.SAXParserFactory;
import javax.xml.transform.OutputKeys;
import javax.xml.transform.Source;
import javax.xml.transform.Transformer;
import javax.xml.transform.TransformerFactory;
import javax.xml.transform.URIResolver;
import javax.xml.transform.sax.SAXSource;
import javax.xml.transform.sax.SAXTransformerFactory;
import javax.xml.transform.stream.StreamResult;
import java.io.BufferedReader;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.io.StringReader;
import java.io.StringWriter;
import java.io.Writer;
import java.util.Hashtable;
import java.util.StringTokenizer;
import de.fhg.igd.logging.LogLevel;
import de.fhg.igd.logging.Logger;
import de.fhg.igd.logging.LoggerFactory;
import net.sf.saxon.instruct.TerminationException;
import org.xml.sax.InputSource;
import org.xml.sax.XMLReader;

/**
 * @author Frank Lautenschl�ger
 *
 * Created on 03.03.2005
 *
 * The Main class is the core module of the codec generator. The class provides
 * the main method to configure and invoke the code generation process.
 *
 */
public class Main {

    /**
     * Logger for this class
     */
    private static Logger log = LoggerFactory.getLogger();

    /**
     * JAXBContext for (un)marshaling configuration data
     */
    private static JAXBContext jc = null;

    /**
     * the usage string
     */
    private static String usage = null;

    static {
        usage = "Usage: java codec.gen.Main <ASN.1 module definition> [option]\n";
        usage += " Options:\n";
        usage += "\tconfig=<file>       : if set use <file> for configuration\n";
        usage += "\t                      else use default configuration file (codec.gen.defaultConfiguration.xml)\n";
        usage += "\toutDir=<directory>  : generated files will go into this directory\n";
        usage += "\tpackage=<package>   : generated Java classes will have this <package> name\n";
        usage += "\theaderFile=<file>   : generated Java classes will start with the content of <file> as header\n";
        usage += "\ttype=xml            : the input file is already parsed - incremental mode\n";
        usage += "\n Example: java codec.gen.Main \"scvp v1.asn1\" config=\"myConfig.xml\" /\n";
        usage += "          outDir=bin package=de.fhg.igd.a8\n";
        log.info("Establish the jaxb context: codec.gen.configuration");
        try {
            jc = JAXBContext.newInstance("codec.gen.configuration");
        } catch (JAXBException e) {
            log.caught(LogLevel.ERROR, "unable to establish context", e);
        }
    }

    /**
     * This method starts the code generation process. The
     *
     * @param args See usage
     */
    public static void main(String[] args) {
        if (log.isEnabled(LogLevel.DEBUG)) {
            log.debug("main(String[]) - start");
        }
        Configuration configuration = null;
        if (args.length < 1) {
            log.error("need ASN.1 module definition file to proceed");
            log.error(usage);
            if (log.isEnabled(LogLevel.DEBUG)) {
                log.debug("main(String[]) - end");
            }
            return;
        }
        Hashtable parameters = extractParameters(args);
        if (parameters.containsKey("help")) {
            System.out.println(usage);
            if (log.isEnabled(LogLevel.DEBUG)) {
                log.debug("main(String[]) - end");
            }
            return;
        }
        if (parameters.containsKey("config")) {
            configuration = readConfiguration(new File((String) parameters.get("config")));
        } else {
            configuration = readDefaultConfiguration();
        }
        if (configuration != null) {
            processParameters(configuration, parameters);
        } else {
            if (log.isEnabled(LogLevel.DEBUG)) {
                log.debug("main(String[]) - end");
            }
            return;
        }
        if (!readHeaderFile(configuration)) {
            if (log.isEnabled(LogLevel.DEBUG)) {
                log.debug("main(String[]) - end");
            }
            return;
        }
        File tempConfigurationFile = new File("configuration.tmp");
        if (writeConfiguration(configuration, tempConfigurationFile)) {
            processPipeline(configuration, tempConfigurationFile);
        } else {
            if (log.isEnabled(LogLevel.DEBUG)) {
                log.debug("main(String[]) - end");
            }
            return;
        }
        log.info("Code generation process completed");
        if (log.isEnabled(LogLevel.DEBUG)) {
            log.debug("main(String[]) - end");
        }
    }

    /**
     * This method reads the header file specified by the
     * configuration parameter configuration.headerFile and
     * writes the content to the header element.
     * @param configuration The configuration to be extended
     * @return false if the headerfile could not be embeded into
     * the configuration otherwise true.
     */
    private static boolean readHeaderFile(Configuration configuration) {
        if (log.isEnabled(LogLevel.DEBUG)) {
            log.debug("readHeaderFile(Configuration) - start");
        }
        String headerFile = configuration.getHeaderFile();
        if ((headerFile != null) && !headerFile.trim().equals("")) {
            StringBuffer buf = new StringBuffer();
            File file = new File(headerFile);
            try {
                BufferedReader in = new BufferedReader(new FileReader(file));
                String str;
                while ((str = in.readLine()) != null) {
                    buf.append(str);
                    buf.append("\n");
                }
                in.close();
                configuration.setHeader(buf.toString());
            } catch (IOException e) {
                log.caught(LogLevel.ERROR, "readHeaderFile(Configuration)", e);
                if (log.isEnabled(LogLevel.DEBUG)) {
                    log.debug("readHeaderFile(Configuration) - end");
                }
                return false;
            }
        }
        if (log.isEnabled(LogLevel.DEBUG)) {
            log.debug("readHeaderFile(Configuration) - end");
        }
        return true;
    }

    /**
     * This method reads the configuration from file and builds a corresponding
     * java structure via JAXB.
     *
     * @param configurationFile The configuration file to read from.
     * @return The configuration based on the configuration file.
     */
    private static Configuration readConfiguration(File configurationFile) {
        if (log.isEnabled(LogLevel.DEBUG)) {
            log.debug("readConfiguration(File) - start");
        }
        Configuration configuration = null;
        try {
            log.info("Unmarshal " + configurationFile + " to Java structure");
            Unmarshaller um = jc.createUnmarshaller();
            configuration = (Configuration) um.unmarshal(configurationFile);
        } catch (JAXBException x) {
            log.caught(LogLevel.ERROR, "readConfiguration(File) - unable to unmarshal configuration ", x);
        }
        if (log.isEnabled(LogLevel.DEBUG)) {
            log.debug("readConfiguration(File) - end");
        }
        return configuration;
    }

    /**
     * This method returns the default configuration file
     * which is provided as a resource. (codec/gen/DefaultConfiguration.xml)
     * @return Configuration
     */
    private static Configuration readDefaultConfiguration() {
        if (log.isEnabled(LogLevel.DEBUG)) {
            log.debug("readDefaultConfiguration() - start");
        }
        java.net.URL url = Main.class.getResource("DefaultConfiguration.xml");
        Configuration configuration = null;
        try {
            log.info("Unmarshal default configuration to Java structure");
            Unmarshaller um = jc.createUnmarshaller();
            configuration = (Configuration) um.unmarshal(url.openStream());
        } catch (IOException x) {
            log.caught(LogLevel.ERROR, "readDefaultConfiguration()", x);
        } catch (JAXBException x) {
            log.caught(LogLevel.ERROR, "unable to unmarshal configuration", x);
        }
        if (log.isEnabled(LogLevel.DEBUG)) {
            log.debug("readDefaultConfiguration() - end");
        }
        return configuration;
    }

    /**
     * Writes the given Configuration to the specified file.
     * @param configuration The configuration which should be written
     * @param configurationFile The target file of the write process
     * @return true if the configuration could be written to the file
     */
    private static boolean writeConfiguration(Configuration configuration, File configurationFile) {
        if (log.isEnabled(LogLevel.DEBUG)) {
            log.debug("writeConfiguration(Configuration, File) - start");
        }
        boolean succesful = false;
        try {
            log.info("Marshal Java structure to " + configurationFile);
            Marshaller marshaller = jc.createMarshaller();
            marshaller.marshal(configuration, new FileOutputStream(configurationFile));
            succesful = true;
        } catch (FileNotFoundException fnfex) {
            log.caught(LogLevel.ERROR, "writeConfiguration(Configuration, File)", fnfex);
        } catch (JAXBException x) {
            log.caught(LogLevel.ERROR, "unable to marshal configuration", x);
        }
        if (log.isEnabled(LogLevel.DEBUG)) {
            log.debug("writeConfiguration(Configuration, File) - end");
        }
        return succesful;
    }

    /**
     * This method initializes the XSL transformer,
     * builds up the filter chain and transforms
     * the XML string.
     *
     * @param configuration The configuration required to initialize
     * the filter chain.
     * @param configurationFile The handle to the configuration file which is
     * passed to the XSL filter as parameter.
     * @return true if the pipeline could processed successfully.
     */
    private static boolean processPipeline(Configuration configuration, File configurationFile) {
        if (log.isEnabled(LogLevel.DEBUG)) {
            log.debug("processPipeline(Configuration, File) - start");
        }
        boolean succesful = false;
        Pipeline pipeline = (Pipeline) configuration.getPipeline();
        File sourceFile = new File(pipeline.getSource().getFile());
        InputSource input = null;
        if (pipeline.getSource().getType().equals("ASN1")) {
            log.info("Processing ASN.1 file: " + sourceFile.getAbsolutePath());
            SimpleNode root = Asn1Parser.parse(sourceFile);
            if (root == null) {
                if (log.isEnabled(LogLevel.DEBUG)) {
                    log.debug("processPipeline(Configuration, File) - end");
                }
                return false;
            }
            input = new InputSource(new StringReader(createXMLfromAST(root)));
        } else {
            log.info("Processing XML file: " + sourceFile.getAbsolutePath());
            try {
                input = new InputSource(new FileReader(sourceFile));
            } catch (FileNotFoundException e) {
                log.caught(LogLevel.ERROR, "processPipeline(Configuration, File) :" + e.getMessage(), e);
                return false;
            }
        }
        try {
            SAXParserFactory spf = SAXParserFactory.newInstance();
            spf.setNamespaceAware(true);
            SAXParser parser = spf.newSAXParser();
            XMLReader reader = parser.getXMLReader();
            SAXTransformerFactory stf = (SAXTransformerFactory) TransformerFactory.newInstance();
            URIResolver resolver = new ClasspathURIResolver(stf.getURIResolver());
            stf.setURIResolver(resolver);
            net.sf.saxon.Filter filter = null;
            XMLReader parent = reader;
            for (int i = 0; i < pipeline.getFilter().size(); i++) {
                PipelineType.FilterType filterType = ((PipelineType.FilterType) pipeline.getFilter().get(i));
                if (!filterType.isDisabled()) {
                    log.info("attach filter: " + filterType.getId() + " (" + filterType.getXsl() + ")");
                    Source xslSource = resolver.resolve(filterType.getXsl(), null);
                    filter = (net.sf.saxon.Filter) stf.newXMLFilter(xslSource);
                    filter.setParent(parent);
                    parent = filter;
                    filter.getTransformer().setParameter("configFile", replacePathSeparator(configurationFile));
                    filter.getTransformer().setParameter("filterID", filterType.getId());
                    filter.getTransformer().setParameter("filterDescription", filterType.getDescription());
                }
            }
            Writer writer = null;
            if (pipeline.getSink() != null) {
                File ouputFile = new File(pipeline.getSink().getFile());
                writer = new FileWriter(ouputFile);
            } else {
                writer = new StringWriter();
            }
            StreamResult result = new StreamResult(writer);
            Transformer transformer = stf.newTransformer();
            transformer.setOutputProperty(OutputKeys.ENCODING, "ISO-8859-1");
            transformer.setOutputProperty(OutputKeys.METHOD, "xml");
            transformer.setOutputProperty(OutputKeys.OMIT_XML_DECLARATION, "no");
            transformer.setOutputProperty(OutputKeys.INDENT, "yes");
            transformer.setOutputProperty(OutputKeys.MEDIA_TYPE, "text/xml");
            transformer.setOutputProperty(OutputKeys.STANDALONE, "yes");
            log.info("Processing pipeline");
            SAXSource transformSource = new SAXSource(parent, input);
            transformer.transform(transformSource, result);
            if (log.isEnabled(LogLevel.DEBUG)) {
                if (pipeline.getSink() != null) {
                    log.debug("processPipeline(Configuration, File) output:\n" + pipeline.getSink().getFile());
                } else {
                    log.debug("processPipeline(Configuration, File) output:\n" + ((StringWriter) writer).getBuffer());
                }
            }
            succesful = true;
        } catch (TerminationException e) {
            if (log.isEnabled(LogLevel.DEBUG)) {
                log.caught(LogLevel.DEBUG, "processPipeline(Configuration, File)", e);
            }
            succesful = false;
        } catch (Exception e) {
            log.caught(LogLevel.ERROR, "processPipeline(Configuration, File) :" + e.getMessage(), e);
            succesful = false;
        }
        if (log.isEnabled(LogLevel.DEBUG)) {
            log.debug("processPipeline(Configuration, File) - end");
        }
        return succesful;
    }

    /**
     * This method adds command line parameters to the configuration file.
     * This is necessary to access the parameters during the
     * XSL transformation process.
     * @param configuration The configuration to adjust.
     * @param parameters Parameters from command line
     */
    private static void processParameters(Configuration configuration, Hashtable parameters) {
        if (log.isEnabled(LogLevel.DEBUG)) {
            log.debug("processParameters(Configuration, Hashtable) - start");
        }
        log.info("Embed command line parameters to configuration");
        if (parameters.containsKey("in")) {
            if (configuration.getPipeline().getSource() == null) {
                configuration.getPipeline().setSource(new PipelineTypeImpl.SourceTypeImpl());
            }
            configuration.getPipeline().getSource().setFile((String) parameters.get("in"));
            if (parameters.containsKey("type") && ((String) parameters.get("type")).equalsIgnoreCase("XML")) {
                configuration.getPipeline().getSource().setType("XML");
            }
        }
        if (parameters.containsKey("outdir")) {
            configuration.setOutputDirectory((String) parameters.get("outdir"));
        }
        if (parameters.containsKey("package")) {
            configuration.setPackage((String) parameters.get("package"));
        }
        if (parameters.containsKey("headerFile")) {
            configuration.setHeaderFile((String) parameters.get("headerFile"));
        }
        if (log.isEnabled(LogLevel.DEBUG)) {
            log.debug("processParameters(Configuration, Hashtable) - end");
        }
    }

    /**
     * This method reads the argumentList, extracts the key/value pairs
     * and stores them in a Hashtable which is returned. The first argument
     * (ASN.1 module file) is required and therefore has no key. This argument
     * is stored using the key "in".
     * @param argumentList The command line arguments
     * @return The Hashtable containing the command line argument
     */
    private static Hashtable extractParameters(String[] argumentList) {
        if (log.isEnabled(LogLevel.DEBUG)) {
            log.debug("extractParameters(String[]) - start");
        }
        Hashtable parameters = new Hashtable();
        if (argumentList.length > 1) {
            if (argumentList[0].toUpperCase().equals("HELP")) {
                parameters.put("help", Boolean.TRUE);
            } else {
                parameters.put("in", argumentList[0]);
            }
            for (int i = 1; i < argumentList.length; i++) {
                StringTokenizer tokenizer = new StringTokenizer(argumentList[i], "=");
                if (tokenizer.countTokens() > 1) {
                    String key = tokenizer.nextToken();
                    String value = tokenizer.nextToken();
                    StringTokenizer tokenizer2 = new StringTokenizer(value, ",");
                    if (tokenizer2.countTokens() == 1) {
                        parameters.put(key, value);
                    } else if (tokenizer2.countTokens() > 2) {
                        String[] values = new String[tokenizer2.countTokens()];
                        for (int j = 0; j < values.length; j++) {
                            values[j] = tokenizer2.nextToken();
                        }
                        parameters.put(key, values);
                    }
                } else if (tokenizer.countTokens() == 1) {
                    parameters.put(tokenizer.nextToken(), Boolean.TRUE);
                }
            }
        }
        if (log.isEnabled(LogLevel.DEBUG)) {
            log.debug("extractParameters(String[]) - end");
        }
        return parameters;
    }

    /**
     * This method returns a XML representation
     * of the given abstract syntax tree.
     *
     * @param node The root node of the AST
     * @return xml generated from abstract syntax tree
     */
    private static String createXMLfromAST(SimpleNode node) {
        if (log.isEnabled(LogLevel.DEBUG)) {
            log.debug("createXMLfromAST(SimpleNode) - start");
        }
        StringWriter sw = null;
        try {
            sw = new StringWriter();
            node.dump(sw);
            String returnString = sw.toString();
            if (log.isEnabled(LogLevel.DEBUG)) {
                log.debug("createXMLfromAST(SimpleNode) - end");
            }
            return returnString;
        } catch (IOException e) {
            log.caught(LogLevel.ERROR, "could not create XML from abstract syntax tree", e);
            if (log.isEnabled(LogLevel.DEBUG)) {
                log.debug("createXMLfromAST(SimpleNode) - end");
            }
            return null;
        } finally {
            if (sw != null) {
                try {
                    sw.close();
                } catch (IOException e) {
                    log.caught(LogLevel.ERROR, "createXMLfromAST(SimpleNode)", e);
                }
            }
        }
    }

    /**
     * Replaces the System path separator by / and prepend a slash
     * if nessecary.
     * @param file relative or absolute path to convert
     * @return absolute path
     */
    private static String replacePathSeparator(File file) {
        if (log.isEnabled(LogLevel.DEBUG)) {
            log.debug("replacePathSeparator(File) - start");
        }
        String absolutePath = file.getAbsolutePath();
        String result = null;
        if (File.separatorChar != '/') {
            StringTokenizer st = new StringTokenizer(absolutePath, File.separator);
            StringBuffer buffer = new StringBuffer();
            while (st.hasMoreTokens()) {
                String token = st.nextToken();
                buffer.append("/");
                buffer.append(token);
            }
            result = buffer.append('/').toString();
        } else {
            if (absolutePath.endsWith("/")) {
                result = absolutePath;
            } else {
                result = absolutePath + '/';
            }
        }
        if (log.isEnabled(LogLevel.DEBUG)) {
            log.debug("replacePathSeparator " + file + " -> " + result);
            log.debug("replacePathSeparator(File) - end");
        }
        return result;
    }
}
