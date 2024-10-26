package ch.sahits.codegen.generator;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.net.MalformedURLException;
import java.net.URL;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Hashtable;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.Vector;
import java.util.Map.Entry;
import javax.wsdl.Definition;
import javax.wsdl.WSDLException;
import javax.wsdl.factory.WSDLFactory;
import javax.wsdl.xml.WSDLReader;
import org.apache.axis2.description.AxisService;
import org.apache.axis2.description.WSDL11ToAxisServiceBuilder;
import org.apache.axis2.util.CommandLineOption;
import org.apache.axis2.util.CommandLineOptionConstants;
import org.apache.axis2.wsdl.codegen.CodeGenConfiguration;
import org.apache.axis2.wsdl.codegen.CodeGenerationEngine;
import org.jdom.JDOMException;
import ch.sahits.codegen.core.util.ILogger;
import ch.sahits.codegen.core.util.LogFactory;
import ch.sahits.codegen.core.util.WorkspaceFragmentProvider;
import ch.sahits.codegen.wizards.IGeneratorWrapper;
import ch.sahits.codegen.xml.WSDLParser;
import ch.sahits.model.IOutputFileModel;
import ch.sahits.model.IOutputFileModelBuilder;
import ch.sahits.model.IWebservice;
import ch.sahits.model.ModelBuilderFactory;

public final class AxisWSDL2JavaWrapper implements IGeneratorWrapper {

    private ILogger logger = LogFactory.getLogger();

    /** File Ending of the MessageRecieferIn only java source file */
    public static final String MESSAGE_RECEIVER_IN_ONLY = "MessageReceiverInOnly.java";

    /** File Ending of the MessageRecieferIn and Out java source file */
    public static final String MESSAGE_RECEIVER_IN_OUT_FRAGMENT = "MessageReceiverInOut.java";

    private static final String SOURCE_DIRECTORY = "src";

    private static final String SS_DEFAULT_PACKAGENAME = "org.apache.axis2";

    private static final String RESOURCES_PATH_FRAGMENT = "resources";

    /** Name of the service.xml file */
    public static final String SERVICE_XML_FILE_NAME = "services.xml";

    /** Filename fragment for the Stup class */
    public static final String SERVICE_STAB_FRAGMENT = "Stub.java";

    /** Model of the webservice */
    private final IWebservice ws;

    /** Output model */
    private IOutputFileModel model = null;

    /** Generate server-side only */
    private boolean isServerside = false;

    /** Flag indicates if the service.xml should be created */
    private boolean isServiceXML = false;

    /** Flag indicating the server-side generation of interfaces */
    private boolean isGenerateServerSideInterface = false;

    /** Generate everything */
    private boolean isGenerateAll = false;

    /** Hashtable of files that are to be deleted after the generation: Key is the file name and value is the full path*/
    private Hashtable<String, String> toBeDeleted = new Hashtable<String, String>();

    /** List of all the artifacts that may be created. Only the file names are noted*/
    private static Vector<String> allArtefactsCreatable = new Vector<String>();

    /**
     * Initialize the generator with the specific model data
     * @param ws Webservice model
     * @param model Data model
     * @throws NullPointerException if either argument is null
     */
    public AxisWSDL2JavaWrapper(final IWebservice ws, IOutputFileModel model) {
        super();
        if (ws == null) {
            throw new NullPointerException("The webservice argument may not be null.");
        }
        if (model == null) {
            throw new NullPointerException("The output file model may not be null");
        }
        this.ws = ws;
        IOutputFileModelBuilder builder = (IOutputFileModelBuilder) ModelBuilderFactory.newBuilder(IOutputFileModel.class);
        this.model = builder.outputFileModel(model).build();
        init();
    }

    /**
	 *  initialize the creatable artefacts
	 * 
	 */
    private void init() {
        if (allArtefactsCreatable.isEmpty()) {
            allArtefactsCreatable.add(SERVICE_XML_FILE_NAME);
            allArtefactsCreatable.add(ws.getServiceName() + "CallbackHandler.java");
            allArtefactsCreatable.add(ws.getServiceName() + SERVICE_STAB_FRAGMENT);
            allArtefactsCreatable.addAll(getServerSide());
            allArtefactsCreatable.addAll(getServerSideInterface());
            allArtefactsCreatable.add("ExtensionMapper.java");
        }
    }

    /**
	 * Retrieve the list of server side interface artefacts
	 * @return list of filenames
	 */
    private List<String> getServerSideInterface() {
        Vector<String> v = new Vector<String>();
        v.add(ws.getServiceName() + "SkeletonInterface.java");
        return v;
    }

    /**
	 * Compute a list of serverside classes
	 * @return List of serverside class names
	 */
    private List<String> getServerSide() {
        Vector<String> v = new Vector<String>();
        v.add(ws.getServiceName() + MESSAGE_RECEIVER_IN_OUT_FRAGMENT);
        v.add(ws.getServiceName() + MESSAGE_RECEIVER_IN_ONLY);
        v.add(ws.getServiceName() + "Skeleton.java");
        try {
            List<String> types = new WSDLParser(ws.getWsdlFileName()).getTypes();
            v.addAll(types);
        } catch (JDOMException e) {
            logger.logWarning(e);
        } catch (FileNotFoundException e) {
        } catch (IOException e) {
            logger.logWarning(e);
        }
        return v;
    }

    /**
	 * Compute the bes generate option and define the files that need to be deleted
	 * after the generation
	 * @param outputFilePath the output file that should not be deleted
	 */
    public void calculateOption(String outputFilePath) {
        HashSet<String> set = new HashSet<String>();
        set.add(outputFilePath);
        List<String> files = new Vector<String>();
        files.add(outputFilePath.substring(outputFilePath.lastIndexOf(File.separator) + 1));
        isGenerateAll = generateAll(files);
        if (isGenerateAll) {
            isServerside = true;
            isServiceXML = true;
            isGenerateServerSideInterface = true;
        } else {
            List<String> l = getServerSide();
            for (String file : files) {
                if (l.contains(file)) {
                    isServerside = true;
                    break;
                }
            }
            isServiceXML = files.contains(SERVICE_XML_FILE_NAME);
            if (isServiceXML) {
                isServerside = true;
            }
            l = getServerSideInterface();
            for (String file : files) {
                if (l.contains(file)) {
                    isGenerateServerSideInterface = true;
                    break;
                }
            }
        }
        for (Iterator<String> iterator = allArtefactsCreatable.iterator(); iterator.hasNext(); ) {
            String artefact = iterator.next();
            if (!files.contains(artefact)) {
                toBeDeleted.put(artefact, "");
            }
        }
        fixFilePath2beDeleted();
    }

    /**
	 * Compute the file paths of the entries in <code>toBeDeleted</code>
	 */
    private void fixFilePath2beDeleted() {
        Set<String> toBremoved = new HashSet<String>();
        for (Entry<String, String> entry : toBeDeleted.entrySet()) {
            String s = entry.getKey();
            List<String> paths = getCorretedPath(s);
            if (paths != null && paths.size() == 1) {
                File f = new File(paths.get(0));
                if (!f.exists()) {
                    toBeDeleted.put(s, paths.get(0));
                } else {
                    toBremoved.add(s);
                }
            } else if (paths != null && paths.size() > 1) {
                toBeDeleted.remove(s);
                int i = 1;
                for (String s2 : paths) {
                    s = i + s;
                    File f = new File(s2);
                    if (!f.exists()) {
                        toBeDeleted.put(s, s2);
                        i++;
                    }
                }
            } else {
            }
        }
        for (String s : toBremoved) {
            toBeDeleted.remove(s);
        }
    }

    /**
	 * Retrieve the package name of the client side classes
	 * @param type for which the namespace should be looked up
	 * @return package name of the client side package
	 */
    private String getNamespaceBasedPackageName(String type) {
        try {
            String namespace = new WSDLParser(ws.getWsdlFileName()).getTargetNamespace(type);
            String host = namespace;
            if (namespace.indexOf("/") >= 0) {
                namespace = namespace.substring(namespace.indexOf("//") + 2);
                host = namespace.substring(0, namespace.indexOf("/"));
            }
            host = host.replace('.', '/');
            String[] parts = host.split("/");
            String result = "";
            for (int i = parts.length - 1; i >= 0; i--) {
                result += parts[i];
                if (i > 0) {
                    result += "/";
                }
            }
            if (namespace.indexOf("/") >= 0) {
                String tail = namespace.substring(namespace.indexOf("/"));
                result = result + tail;
            }
            result = result.replace('/', '.');
            if (result.endsWith(".")) {
                result = result.substring(0, result.lastIndexOf("."));
            }
            return result;
        } catch (JDOMException e) {
            return SS_DEFAULT_PACKAGENAME;
        } catch (IOException e) {
            return SS_DEFAULT_PACKAGENAME;
        }
    }

    /**
	 * Fix the path of a class to be the absolute path
	 * @param storeLocation Store location defined by the wizard
	 * @param workspacePath Path to the current workspace
	 * @param packageName package name of the package the class is defined in
	 * @param fileName Java file name of the class 
	 * @return absolute path to the file
	 */
    private String replaceServerSideClass(String storeLocation, String workspacePath, String packageName, String fileName) {
        String packagePathFragment = packageName.replace('.', File.separatorChar);
        fileName = File.separator + packagePathFragment + File.separator + fileName;
        fileName = workspacePath + storeLocation + File.separator + SOURCE_DIRECTORY + fileName;
        return fileName;
    }

    /**
	 * Retrieve the package name of the server side packages
	 * @return Server side package name
	 */
    private String getServerSidePackage() {
        if (ws.getPackageName() == null || ws.getPackageName().equals("")) {
            return SS_DEFAULT_PACKAGENAME;
        }
        return ws.getPackageName();
    }

    /**
	 * Check the list of files against the list of all artifacts
	 * @param files List of filenames
	 * @return false if at least one file is not on the list
	 */
    private boolean generateAll(List<String> files) {
        for (Iterator<String> iterator = allArtefactsCreatable.iterator(); iterator.hasNext(); ) {
            String f = iterator.next();
            if (!files.contains(f)) {
                return false;
            }
        }
        return true;
    }

    /**
	 * Remove the undesired artifacts
	 */
    public void cleanup() {
        String workspacePath = WorkspaceFragmentProvider.getAbsolutWorkspacePath();
        File f;
        for (Entry<String, String> e : toBeDeleted.entrySet()) {
            f = new File(e.getValue());
            f.delete();
        }
        String packagePathFragment = getServerSidePackage().replace('.', File.separatorChar);
        String ssPackage = workspacePath + model.getStoreLocation() + File.separator + SOURCE_DIRECTORY + File.separator + packagePathFragment;
        f = new File(ssPackage);
        if (f.isDirectory() && f.list().length == 0) {
            f.delete();
            f = f.getParentFile();
            while (!f.getName().equals(SOURCE_DIRECTORY)) {
                if (f.isDirectory() && f.list().length == 0) {
                    f.delete();
                    f = f.getParentFile();
                } else {
                    break;
                }
            }
        }
        try {
            WSDLParser parser = new WSDLParser(ws.getWsdlFileName());
            List<String> types = parser.getTypes();
            for (String type : types) {
                String packageName = getNamespaceBasedPackageName(type);
                String path = workspacePath + model.getStoreLocation() + File.separator + SOURCE_DIRECTORY + File.separator + packageName.replace('.', File.separatorChar);
                f = new File(path);
                if (f.isDirectory() && f.list().length == 0) {
                    f.delete();
                    f = f.getParentFile();
                    while (!f.getName().equals(SOURCE_DIRECTORY)) {
                        if (f.isDirectory() && f.list().length == 0) {
                            f.delete();
                            f = f.getParentFile();
                        } else {
                            return;
                        }
                    }
                }
            }
        } catch (JDOMException e1) {
            e1.printStackTrace();
        } catch (IOException e1) {
            e1.printStackTrace();
        }
    }

    /**
	 * Cleanup the resource folder by moving the service.xml, and deleting the folder with the
	 * containing *.wsdl file. Also delete the build.xml
	 * @param workspacePath Path to the workspace
	 */
    private void cleanupResourcesFolder(String workspacePath) {
        File f = new File(workspacePath + model.getStoreLocation() + File.separator + "build.xml");
        f.exists();
        f.delete();
        if (!toBeDeleted.contains(SERVICE_XML_FILE_NAME)) {
            String fromPath = workspacePath + model.getStoreLocation() + File.separator + RESOURCES_PATH_FRAGMENT + File.separator + SERVICE_XML_FILE_NAME;
            String toPath = workspacePath + model.getStoreLocation() + File.separator + SERVICE_XML_FILE_NAME;
            f = new File(fromPath);
            f.renameTo(new File(toPath));
            String wsdlFile = workspacePath + model.getStoreLocation() + File.separator + RESOURCES_PATH_FRAGMENT + File.separator + ws.getWsdlFileName().substring(ws.getWsdlFileName().lastIndexOf(File.separator) + 1);
            f = new File(wsdlFile);
            f.delete();
            f = new File(workspacePath + model.getStoreLocation() + File.separator + RESOURCES_PATH_FRAGMENT);
            f.delete();
        }
    }

    /**
	 * Generate all output that is needed
	 */
    public void generate() {
        boolean synchrounous = true;
        Map<String, CommandLineOption> optionMap = new HashMap<String, CommandLineOption>();
        optionMap.put(CommandLineOptionConstants.WSDL2JavaConstants.WSDL_LOCATION_URI_OPTION, new CommandLineOption(CommandLineOptionConstants.WSDL2JavaConstants.WSDL_LOCATION_URI_OPTION, getStringArray(ws.getWsdlFileName())));
        if (!synchrounous) {
            optionMap.put(CommandLineOptionConstants.WSDL2JavaConstants.CODEGEN_ASYNC_ONLY_OPTION, new CommandLineOption(CommandLineOptionConstants.WSDL2JavaConstants.CODEGEN_ASYNC_ONLY_OPTION, new String[0]));
        }
        if (synchrounous) {
            optionMap.put(CommandLineOptionConstants.WSDL2JavaConstants.CODEGEN_SYNC_ONLY_OPTION, new CommandLineOption(CommandLineOptionConstants.WSDL2JavaConstants.CODEGEN_SYNC_ONLY_OPTION, new String[0]));
        }
        if (isServerside) {
            optionMap.put(CommandLineOptionConstants.WSDL2JavaConstants.SERVER_SIDE_CODE_OPTION, new CommandLineOption(CommandLineOptionConstants.WSDL2JavaConstants.SERVER_SIDE_CODE_OPTION, new String[0]));
            if (isServiceXML) {
                optionMap.put(CommandLineOptionConstants.WSDL2JavaConstants.GENERATE_SERVICE_DESCRIPTION_OPTION, new CommandLineOption(CommandLineOptionConstants.WSDL2JavaConstants.GENERATE_SERVICE_DESCRIPTION_OPTION, new String[0]));
            }
            if (isGenerateAll) {
                optionMap.put(CommandLineOptionConstants.WSDL2JavaConstants.GENERATE_ALL_OPTION, new CommandLineOption(CommandLineOptionConstants.WSDL2JavaConstants.GENERATE_ALL_OPTION, new String[0]));
            }
        }
        if (ws.getPackageName() != null) {
            optionMap.put(CommandLineOptionConstants.WSDL2JavaConstants.PACKAGE_OPTION, new CommandLineOption(CommandLineOptionConstants.WSDL2JavaConstants.PACKAGE_OPTION, getStringArray(ws.getPackageName())));
        } else {
            optionMap.put(CommandLineOptionConstants.WSDL2JavaConstants.PACKAGE_OPTION, new CommandLineOption(CommandLineOptionConstants.WSDL2JavaConstants.PACKAGE_OPTION, getStringArray("")));
        }
        optionMap.put(CommandLineOptionConstants.WSDL2JavaConstants.STUB_LANGUAGE_OPTION, new CommandLineOption(CommandLineOptionConstants.WSDL2JavaConstants.STUB_LANGUAGE_OPTION, getStringArray("java")));
        String outputLocation = WorkspaceFragmentProvider.getAbsolutWorkspacePath() + model.getStoreLocation();
        optionMap.put(CommandLineOptionConstants.WSDL2JavaConstants.OUTPUT_LOCATION_OPTION, new CommandLineOption(CommandLineOptionConstants.WSDL2JavaConstants.OUTPUT_LOCATION_OPTION, getStringArray(outputLocation)));
        optionMap.put(CommandLineOptionConstants.WSDL2JavaConstants.DATA_BINDING_TYPE_OPTION, new CommandLineOption(CommandLineOptionConstants.WSDL2JavaConstants.DATA_BINDING_TYPE_OPTION, getStringArray(ws.getDatabinding())));
        if (ws.getServiceURL() != null) {
            optionMap.put(CommandLineOptionConstants.WSDL2JavaConstants.PORT_NAME_OPTION, new CommandLineOption(CommandLineOptionConstants.WSDL2JavaConstants.PORT_NAME_OPTION, getStringArray(ws.getServiceURL())));
        }
        if (ws.getServiceName() != null) {
            optionMap.put(CommandLineOptionConstants.WSDL2JavaConstants.SERVICE_NAME_OPTION, new CommandLineOption(CommandLineOptionConstants.WSDL2JavaConstants.SERVICE_NAME_OPTION, getStringArray(ws.getServiceName())));
        }
        if (!ws.getNs2packageMapping().isEmpty()) {
            String ns2packageMappings = convertToCommaSeparatedString(ws.getNs2packageMapping());
            optionMap.put(CommandLineOptionConstants.WSDL2JavaConstants.NAME_SPACE_TO_PACKAGE_OPTION, new CommandLineOption(CommandLineOptionConstants.WSDL2JavaConstants.NAME_SPACE_TO_PACKAGE_OPTION, getStringArray(ns2packageMappings)));
        }
        if (isGenerateServerSideInterface) {
            optionMap.put(CommandLineOptionConstants.WSDL2JavaConstants.SERVER_SIDE_INTERFACE_OPTION, new CommandLineOption(CommandLineOptionConstants.WSDL2JavaConstants.SERVER_SIDE_INTERFACE_OPTION, new String[0]));
        }
        try {
            AxisService service = getAxisService(ws.getWsdlFileName());
            CodeGenConfiguration codegenConfig = new CodeGenConfiguration(optionMap);
            codegenConfig.addAxisService(service);
            Definition wsdlDefinition;
            wsdlDefinition = getWsdlDefinition(ws.getWsdlFileName());
            codegenConfig.setWsdlDefinition(wsdlDefinition);
            codegenConfig.setBaseURI(getBaseUri(ws.getWsdlFileName()));
            new CodeGenerationEngine(codegenConfig).generate();
            String workspacePath = WorkspaceFragmentProvider.getAbsolutWorkspacePath();
            cleanupResourcesFolder(workspacePath);
        } catch (FileNotFoundException e) {
            throw new RuntimeException(e);
        } catch (Exception e) {
            logger.log(e);
        }
    }

    /**
	 * public method to get loaded wsdl Definition
	 * @param filepath path to the WSDL file
	 * @return WSDL definition object
	 * @throws WSDLException 
	 */
    protected Definition getWsdlDefinition(String filepath) throws WSDLException {
        WSDLReader reader = WSDLFactory.newInstance().newWSDLReader();
        Definition wsdlDefinition = reader.readWSDL(filepath);
        return wsdlDefinition;
    }

    /**
	 * Convert the hashmap into a property list, that is comma separated
	 * @param ns2packageMapping hasmap to be processed
	 * @return comma separated string
	 */
    private String convertToCommaSeparatedString(HashMap<String, String> ns2packageMapping) {
        String s = "";
        if (!ns2packageMapping.isEmpty()) {
            for (Iterator<Entry<String, String>> iterator = ns2packageMapping.entrySet().iterator(); iterator.hasNext(); ) {
                Entry<String, String> entry = iterator.next();
                s += entry.getKey() + "=" + entry.getValue() + ",";
            }
            s = s.substring(0, s.lastIndexOf(","));
        }
        return s;
    }

    /**
     * Converts a single String into a String Array
     * 
     * @param value a single string
     * @return an array containing only one element
     */
    private String[] getStringArray(String value) {
        String[] values = new String[1];
        values[0] = value;
        return values;
    }

    /**
     * Retrive the URI of the directory of the WSDL file
     * @param wsdlURI
     * @return path to the WSDL
     */
    public String getBaseUri(String wsdlURI) {
        try {
            URL url;
            if (wsdlURI.indexOf("://") == -1) {
                url = new URL("file", "", wsdlURI);
            } else {
                url = new URL(wsdlURI);
            }
            String baseUri;
            if ("file".equals(url.getProtocol())) {
                baseUri = new File(url.getFile()).getParentFile().toURI().toURL().toExternalForm();
            } else {
                baseUri = url.toExternalForm().substring(0, url.toExternalForm().lastIndexOf("/"));
            }
            return baseUri;
        } catch (MalformedURLException e) {
            throw new RuntimeException(e);
        }
    }

    /**
     * Reads the WSDL Object Model from the given location.
     * 
     * @param wsdlURI the filesystem location (full path) of the WSDL file to read in.
     * @return the WSDLDescription object containing the WSDL Object Model of the given WSDL file
     * @throws IOException on errors reading the WSDL file
     */
    public AxisService getAxisService(String wsdlURI) throws IOException {
        URL url;
        if (wsdlURI.indexOf("://") == -1) {
            url = new URL("file", "", wsdlURI);
        } else {
            url = new URL(wsdlURI);
        }
        WSDL11ToAxisServiceBuilder builder = new WSDL11ToAxisServiceBuilder(url.openConnection().getInputStream());
        builder.setDocumentBaseUri(url.toString());
        builder.setBaseUri(getBaseUri(wsdlURI));
        builder.setCodegen(true);
        return builder.populateService();
    }

    /**
     * Retrieve the output path for an output fragment normaly there is only one path but there may be more
     * @param outputFile last fragment of the absolut path
     * @return absolut path to the file or null if the path could not be retrieved
     */
    public List<String> getCorretedPath(String outputFile) {
        String storeLocation = model.getStoreLocation();
        String workspacePath = WorkspaceFragmentProvider.getAbsolutWorkspacePath();
        String serversidePackageName = getServerSidePackage();
        LinkedList<String> result = new LinkedList<String>();
        if (outputFile.equals(SERVICE_XML_FILE_NAME)) {
            String s2 = workspacePath + File.separator + storeLocation + File.separator + outputFile;
            result.add(s2);
            return result;
        }
        if (outputFile.endsWith("Skeleton.java") || outputFile.endsWith("Stub.java") || outputFile.endsWith(MESSAGE_RECEIVER_IN_OUT_FRAGMENT) || outputFile.endsWith(MESSAGE_RECEIVER_IN_OUT_FRAGMENT) || outputFile.endsWith(MESSAGE_RECEIVER_IN_ONLY)) {
            String s2 = replaceServerSideClass(storeLocation, workspacePath, serversidePackageName, outputFile);
            result.add(s2);
            return result;
        }
        if (outputFile.endsWith("ExtensionMapper.java")) {
            try {
                WSDLParser parser = new WSDLParser(ws.getWsdlFileName());
                List<String> types = parser.getTypes();
                for (String type : types) {
                    String packageName = getNamespaceBasedPackageName(type);
                    String s2 = replaceServerSideClass(storeLocation, workspacePath, packageName, outputFile);
                    if (!result.contains(s2)) {
                        result.add(s2);
                    }
                }
            } catch (JDOMException e) {
                e.printStackTrace();
                return null;
            } catch (IOException e) {
                e.printStackTrace();
                return null;
            }
            return result;
        }
        try {
            WSDLParser parser = new WSDLParser(ws.getWsdlFileName());
            List<String> types = parser.getTypes();
            for (String type : types) {
                if (outputFile.endsWith(type)) {
                    String packageName = getNamespaceBasedPackageName(outputFile);
                    String s2 = replaceServerSideClass(storeLocation, workspacePath, packageName, outputFile) + ".java";
                    result.add(s2);
                    return result;
                }
            }
        } catch (JDOMException e) {
            e.printStackTrace();
            return null;
        } catch (IOException e) {
            e.printStackTrace();
            return null;
        }
        return null;
    }
}
