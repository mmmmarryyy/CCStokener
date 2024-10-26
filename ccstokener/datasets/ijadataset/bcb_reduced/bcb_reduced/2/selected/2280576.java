package org.jaffa.tools.patternengine;

import java.lang.reflect.*;
import java.util.*;
import java.net.*;
import java.io.*;
import org.webmacro.*;
import org.jaffa.datatypes.DateTime;
import org.jaffa.util.URLHelper;
import org.jaffa.tools.common.SourceDecomposerUtils;
import org.jaffa.tools.common.SourceDecomposerException;
import org.jaffa.util.DefaultEntityResolver;
import org.jaffa.util.DefaultErrorHandler;
import org.jaffa.util.XmlHelper;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.DocumentBuilder;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;
import javax.xml.parsers.ParserConfigurationException;
import org.xml.sax.SAXException;

/** Use this class to perform code generation based on the input pattern meta data file.
 */
public class PatternGenerator {

    private static final String PATTERN_GENERATOR_PROPERTIES_FILE = "patterns/PatternGenerator.properties";

    private static final String WEB_MACRO_PROPERTIES_FILE = "patterns/WebMacro.properties";

    private static final String AUDIT_LOG_FILE = "audit.log";

    private static final String PROPERTY_PROJECT_ROOT_DIRECTORY = "ProjectRootDirectory";

    private static final String PROPERTY_TEMP_META_DIRECTORY = "TempMetaDataDirectory";

    private static final String PROPERTY_LOG_DIRECTORY = "LogDirectory";

    private static final String XML_PATTERN_TEMPLATE = "PatternTemplate";

    private static final String XML_DATE_TIME = "DateTime";

    private static final String XML_PREREQUESITES = "PreRequesites";

    private static final String XML_COMPONENTS = "Components";

    private static final String XML_FILE_NAME = "FileName";

    private static final String XML_PACKAGE = "Package";

    private static final String XML_TEMPLATE = "Template";

    private static final String XML_OVERRIDE = "Override";

    private static final String XML_GENERATE = "Generate";

    private static final String XML_GENERATE_TYPE = "Type";

    private static final String XML_GENERATE_WORKING_DIR = "WorkingDirectory";

    private static final String XML_GENERATE_COMMAND_LINE = "CommandLine";

    private static final String XML_GENERATE_CLASS_NAME = "ClassName";

    private static final String XML_GENERATE_ARGUMENTS = "Arguments";

    private static final String XML_SCRATCHPAD_FOR_WM = "ScratchPad";

    private URL m_patternMetaData = null;

    private Properties m_pgProperties = null;

    private String m_auditFile = null;

    private WebMacro m_wm = null;

    /** Creates new PatternGenerator.
     * @param patternMetaData the pattern meta data file to be used for code generation.
     * @throws PatternGeneratorException if any error occurs.
     */
    public PatternGenerator(URL patternMetaData) throws PatternGeneratorException {
        try {
            doInit(patternMetaData);
        } catch (PatternGeneratorException e) {
            throw e;
        } catch (Exception e) {
            throw new PatternGeneratorException(e);
        }
    }

    /** This method is to be invoked, when done with the code generation. It will free up the resources.
     */
    public void destroy() {
        if (m_wm != null) {
            m_wm.destroy();
            m_wm = null;
        }
        if (m_pgProperties != null) {
            m_pgProperties.clear();
            m_pgProperties = null;
        }
    }

    private void doInit(URL patternMetaData) throws PatternGeneratorException, IOException, WebMacroException, ParserConfigurationException, SAXException, MalformedURLException {
        m_patternMetaData = patternMetaData;
        URL pgUrl = URLHelper.newExtendedURL(PATTERN_GENERATOR_PROPERTIES_FILE);
        m_pgProperties = getProperties(pgUrl);
        String logDir = m_pgProperties.getProperty(PROPERTY_LOG_DIRECTORY);
        if (logDir != null && !logDir.equals("")) {
            m_auditFile = getAbsoluteFileName(logDir, AUDIT_LOG_FILE);
        }
        m_wm = new WM(WEB_MACRO_PROPERTIES_FILE);
    }

    private Properties getProperties(URL url) throws IOException, PatternGeneratorException {
        Properties properties = new Properties();
        InputStream in = url.openStream();
        properties.load(in);
        in.close();
        if (properties.isEmpty()) throw new PatternGeneratorException("Could not load properties: " + url);
        return properties;
    }

    private String getAbsoluteFileName(String dirName, String fileName) {
        File file = new File(dirName, fileName);
        return file.getAbsolutePath();
    }

    /** This will invoke the Pattern Generation routine.
     * @throws PatternGeneratorException if any error occurs.
     */
    public void processPattern() throws PatternGeneratorException {
        try {
            String patternTemplate = null;
            DocumentBuilderFactory factory = DocumentBuilderFactory.newInstance();
            factory.setValidating(false);
            DocumentBuilder parser = factory.newDocumentBuilder();
            parser.setEntityResolver(new DefaultEntityResolver());
            parser.setErrorHandler(new DefaultErrorHandler());
            Document document = parser.parse(m_patternMetaData.getFile());
            NodeList nodes = document.getElementsByTagName(XML_PATTERN_TEMPLATE);
            if (nodes != null && nodes.getLength() > 0) {
                Node node = nodes.item(0);
                patternTemplate = XmlHelper.getTextTrim(node);
            }
            if (patternTemplate == null || patternTemplate.equals("")) throw new PatternGeneratorException("The " + XML_PATTERN_TEMPLATE + " element is not supplied in the input Meta Data File");
            Context context = getContextWithData();
            String tempFileName = process1(patternTemplate, (new File(m_patternMetaData.getFile())).getName(), context);
            process2(tempFileName, context);
        } catch (PatternGeneratorException e) {
            throw e;
        } catch (Exception e) {
            throw new PatternGeneratorException(e);
        } finally {
            destroy();
        }
    }

    private Context getContextWithData() throws ParserConfigurationException, SAXException, IOException, PatternGeneratorException {
        Map metaDataMap = DomParser.parse(m_patternMetaData.getFile(), true);
        metaDataMap = stripRoot(metaDataMap);
        if (metaDataMap == null) throw new PatternGeneratorException("PatternMetaData file " + m_patternMetaData.getFile() + " is incorrectly formatted");
        Context context = m_wm.getContext();
        for (Iterator itr = metaDataMap.entrySet().iterator(); itr.hasNext(); ) {
            Map.Entry me = (Map.Entry) itr.next();
            context.put(me.getKey(), me.getValue());
        }
        context.put(XML_DATE_TIME, new DateTime());
        context.put(PROPERTY_PROJECT_ROOT_DIRECTORY, m_pgProperties.getProperty(PROPERTY_PROJECT_ROOT_DIRECTORY));
        context.put(XML_SCRATCHPAD_FOR_WM, new HashMap());
        return context;
    }

    private Map stripRoot(Map input) {
        Map output = null;
        Iterator itr = input.values().iterator();
        if (itr.hasNext()) {
            Object obj = itr.next();
            if (obj instanceof Map) output = (Map) obj;
        }
        return output;
    }

    private String process1(String patternTemplate, String patternMetaData, Context context) throws IOException, WebMacroException {
        String tempFileName = getAbsoluteFileName(m_pgProperties.getProperty(PROPERTY_TEMP_META_DIRECTORY), patternMetaData);
        writeWm(patternTemplate, context, tempFileName);
        String log = '\n' + new DateTime().toString() + " - Processing PatternTemplate: " + patternTemplate + " using PatternMetaData: " + patternMetaData + '\n' + "Created temporary file: " + tempFileName;
        write(m_auditFile, log, true);
        return tempFileName;
    }

    private void process2(String tempFileName, Context context) throws PatternGeneratorException, ParserConfigurationException, SAXException, IOException, WebMacroException, SourceDecomposerException {
        DocumentBuilderFactory factory = DocumentBuilderFactory.newInstance();
        factory.setValidating(true);
        DocumentBuilder parser = factory.newDocumentBuilder();
        parser.setEntityResolver(new DefaultEntityResolver());
        parser.setErrorHandler(new DefaultErrorHandler());
        Document document = parser.parse(tempFileName);
        String preReqErrLog = null;
        Node preReq = document.getElementsByTagName(XML_PREREQUESITES).item(0);
        NodeList classes = preReq.getChildNodes();
        if (classes != null) {
            for (int i = 0; i < classes.getLength(); i++) {
                Node clazz = classes.item(i);
                if (clazz.getNodeType() == Node.ELEMENT_NODE) {
                    String requiredClassName = XmlHelper.getTextTrim(clazz);
                    try {
                        Class.forName(requiredClassName);
                    } catch (ClassNotFoundException e) {
                        if (preReqErrLog == null) preReqErrLog = "";
                        preReqErrLog += "Error: PreRequesite Class " + requiredClassName + " not found\n";
                    }
                }
            }
        }
        if (preReqErrLog != null) {
            write(m_auditFile, preReqErrLog, true);
            throw new PatternGeneratorException(preReqErrLog);
        }
        Node components = document.getElementsByTagName(XML_COMPONENTS).item(0);
        NodeList buildNodes = components.getChildNodes();
        for (int i = 0; i < buildNodes.getLength(); i++) {
            Node buildNode = buildNodes.item(i);
            if (buildNode.getNodeType() != Node.ELEMENT_NODE) continue;
            Element build = (Element) buildNode;
            String fileName = XmlHelper.getTextTrim(build.getElementsByTagName(XML_FILE_NAME).item(0));
            String packageName = XmlHelper.getTextTrim(build.getElementsByTagName(XML_PACKAGE).item(0));
            String templateName = XmlHelper.getTextTrim(build.getElementsByTagName(XML_TEMPLATE).item(0));
            String override = XmlHelper.getTextTrim(build.getElementsByTagName(XML_OVERRIDE).item(0));
            boolean fileExists = false;
            context.put(XML_FILE_NAME, fileName);
            context.put(XML_PACKAGE, packageName);
            String dirName = getDirectoryName(m_pgProperties.getProperty(PROPERTY_PROJECT_ROOT_DIRECTORY), packageName);
            fileName = dirName + File.separatorChar + fileName;
            File f = new File(fileName);
            if (f.exists() && f.isFile()) fileExists = true;
            if (!fileExists) {
                createFile(templateName, context, fileName);
                write(m_auditFile, "Created: " + fileName, true);
            } else {
                if (override.equalsIgnoreCase("ask")) override = askUser(fileName);
                if (override.equalsIgnoreCase("no") || override.equalsIgnoreCase("false")) {
                    write(m_auditFile, "Left untouched: " + fileName, true);
                } else if (override.equalsIgnoreCase("yes") || override.equalsIgnoreCase("true")) {
                    createFile(templateName, context, fileName);
                    write(m_auditFile, "Recreated: " + fileName, true);
                } else if (override.equalsIgnoreCase("merge")) {
                    mergeFile(templateName, context, fileName);
                    write(m_auditFile, "Recreated with existing customizations: " + fileName, true);
                } else if (override.equalsIgnoreCase("OverrideIfMarkerPresent")) {
                    if (SourceDecomposerUtils.isJaffaOverwriteMarkerPresent(f)) {
                        createFile(templateName, context, fileName);
                        write(m_auditFile, "Recreated: " + fileName, true);
                    } else {
                        write(m_auditFile, "Left untouched: " + fileName, true);
                    }
                } else if (override.equalsIgnoreCase("OverrideIfMarkerPresentOrCreateTempFileIfMarkerAbsent")) {
                    if (SourceDecomposerUtils.isJaffaOverwriteMarkerPresent(f)) {
                        createFile(templateName, context, fileName);
                        write(m_auditFile, "Recreated: " + fileName, true);
                    } else {
                        String newFileName = fileName + ".new";
                        createFile(templateName, context, newFileName);
                        write(m_auditFile, "Created Temp File: " + newFileName, true);
                    }
                } else {
                    write(m_auditFile, "ERROR: Unknown override option '" + override + "' passed for file " + fileName + ". Nothing done", true);
                    continue;
                }
            }
            processGenerate(build);
        }
    }

    private String askUser(String fileName) throws IOException {
        System.out.println("File '" + fileName + "' already exists..");
        System.out.println("1 - Leave It Alone / 2 - Overwrite / 3 - Merge / 4 - OverrideIfMarkerPresent / 5 - OverrideIfMarkerPresentOrCreateTempFileIfMarkerAbsent ?");
        System.out.flush();
        String s;
        BufferedReader in = new BufferedReader(new InputStreamReader(System.in));
        while (true) {
            s = in.readLine();
            if (s.equals("1")) {
                s = "false";
                break;
            } else if (s.equals("2")) {
                s = "true";
                break;
            } else if (s.equals("3")) {
                s = "merge";
                break;
            } else if (s.equals("4")) {
                s = "OverrideIfMarkerPresent";
                break;
            } else if (s.equals("5")) {
                s = "OverrideIfMarkerPresentOrCreateTempFileIfMarkerAbsent";
                break;
            } else {
                System.out.println("Enter either 1, 2, 3, 4 or 5");
            }
        }
        return s;
    }

    private void createFile(String templateName, Context context, String fileName) throws WebMacroException, IOException, PatternGeneratorException {
        writeWm(templateName, context, fileName);
        fixEOL(fileName);
    }

    private void mergeFile(String templateName, Context context, String fileName) throws WebMacroException, IOException, PatternGeneratorException, SourceDecomposerException {
        ByteArrayOutputStream bos = new ByteArrayOutputStream();
        writeWm(templateName, context, bos);
        String newContents = bos.toString();
        String mergedContents = SourceMerge.performMerge(new BufferedReader(new StringReader(newContents)), new BufferedReader(new FileReader(fileName)));
        Writer writer = new BufferedWriter(new FileWriter(fileName));
        writer.write(mergedContents);
        writer.flush();
        writer.close();
        fixEOL(fileName);
    }

    private void fixEOL(String fileName) throws FileNotFoundException, IOException {
        StringWriter sw = new StringWriter();
        BufferedWriter bw = new BufferedWriter(sw);
        BufferedReader br = new BufferedReader(new FileReader(fileName));
        String line;
        while ((line = br.readLine()) != null) {
            bw.write(line);
            bw.newLine();
        }
        br.close();
        bw.close();
        bw = new BufferedWriter(new FileWriter(fileName));
        bw.write(sw.toString());
        bw.close();
    }

    private String getDirectoryName(String rootDir, String packageName) {
        String packageNameToDir = packageName.replace('.', File.separatorChar);
        if (rootDir.endsWith(File.separator)) rootDir = rootDir + packageNameToDir; else rootDir = rootDir + File.separatorChar + packageNameToDir;
        File dir = new File(rootDir);
        if (!dir.isDirectory()) dir.mkdirs();
        return dir.getAbsolutePath();
    }

    private void writeWm(String templateFileName, Context context, String outputFileName) throws WebMacroException, IOException {
        writeWm(templateFileName, context, new FileOutputStream(outputFileName));
    }

    private void writeWm(String templateFileName, Context context, OutputStream outputStream) throws WebMacroException, IOException {
        Template template = m_wm.getTemplate(templateFileName);
        FastWriter out = new FastWriter(m_wm.getBroker(), new BufferedOutputStream(outputStream), FastWriter.SAFE_UNICODE_ENCODING);
        template.write(out, context);
        out.close();
    }

    private void write(String fileName, String message, boolean append) throws IOException {
        if (fileName != null && !fileName.equals("") && message != null && !message.equals("")) {
            PrintWriter out = null;
            if (append) out = new PrintWriter(new FileWriter(fileName, true)); else out = new PrintWriter(new FileWriter(fileName));
            out.println(message);
            out.close();
        }
    }

    private void processGenerate(Element build) throws IOException {
        NodeList list = build.getElementsByTagName(XML_GENERATE);
        if (list != null && list.getLength() > 0) {
            Element generate = (Element) list.item(0);
            String type = XmlHelper.getTextTrim(generate.getElementsByTagName(XML_GENERATE_TYPE).item(0));
            if ("OS".equalsIgnoreCase(type)) {
                String workingDir = XmlHelper.getTextTrim(generate.getElementsByTagName(XML_GENERATE_WORKING_DIR).item(0));
                String command = XmlHelper.getTextTrim(generate.getElementsByTagName(XML_GENERATE_COMMAND_LINE).item(0));
                Process process = Runtime.getRuntime().exec(command, null, new File(workingDir));
                try {
                    process.waitFor();
                } catch (InterruptedException e) {
                }
                write(m_auditFile, "OS: Executed " + command + " with the working dir " + workingDir, true);
            } else if ("JAVA".equalsIgnoreCase(type)) {
                String className = XmlHelper.getTextTrim(generate.getElementsByTagName(XML_GENERATE_CLASS_NAME).item(0));
                String arguments = XmlHelper.getTextTrim(generate.getElementsByTagName(XML_GENERATE_ARGUMENTS).item(0));
                try {
                    Class clazz = Class.forName(className);
                    Method main = clazz.getMethod("main", new Class[] { String[].class });
                    int mod = main.getModifiers();
                    if (Modifier.isStatic(mod)) {
                        StringTokenizer stz = new StringTokenizer(arguments, " ");
                        String[] argumentArray = new String[stz.countTokens()];
                        int i = -1;
                        while (stz.hasMoreTokens()) argumentArray[++i] = stz.nextToken();
                        main.invoke(null, new Object[] { argumentArray });
                        write(m_auditFile, "JAVA: Executed class  " + className + ", using the arguments " + arguments, true);
                    } else {
                        write(m_auditFile, "JAVA Error: The 'public static void main(String[] args)' method not found for the class " + className, true);
                    }
                } catch (ClassNotFoundException e) {
                    write(m_auditFile, "JAVA Error: Class " + className + " not found", true);
                } catch (NoSuchMethodException e) {
                    write(m_auditFile, "JAVA Error: The 'public static void main(String[] args)' method not found for the class " + className, true);
                } catch (IllegalAccessException e) {
                    write(m_auditFile, "JAVA Error: IllegalAccessException thrown while executing class " + className, true);
                } catch (InvocationTargetException e) {
                    write(m_auditFile, "JAVA Error: InvocationTargetException thrown while executing class " + className, true);
                }
            } else {
                write(m_auditFile, "Error: UnSupported Generate Type " + type, true);
            }
        }
    }

    /** This will create an instance of the PatternGenerator, passing the URL corresponding to the input argument.
     * It will then invoke the 'processPattern()' method.
     * If the argument is a directory, then the above process will be invoked for each file in the directory.
     * If the '-r' argument is passed, then it will recursively scan the directory for files, and invoke the pattern generation for each file.
     * @param args This expects at least one argument to be passed in. This should represent the patternMetaData file, relative to the classpath. If a directory of passed, then the optional second argument determines, if a recursive search is to be performed on the directory.
     */
    public static void main(String[] args) {
        if (args.length < 1 || args.length > 2 || (args.length == 2 && !args[1].equals("-r"))) usage();
        File dir = new File(args[0]);
        if (dir.exists() && dir.isDirectory()) {
            executePatternGenerator(dir, args.length == 2 ? true : false);
        } else {
            try {
                URL url = URLHelper.newExtendedURL(args[0]);
                File f = new File(url.getFile());
                if (f.exists() && f.isDirectory()) executePatternGenerator(f, args.length == 2 ? true : false); else executePatternGenerator(args[0]);
            } catch (MalformedURLException e) {
                e.printStackTrace();
                System.out.println("The pattern meta data file not found: " + args[0]);
            }
        }
    }

    private static void executePatternGenerator(File dir, boolean recursive) {
        File[] files = dir.listFiles();
        for (int i = 0; i < files.length; i++) {
            System.out.flush();
            File file = files[i];
            if (file.isFile()) executePatternGenerator(file.getAbsolutePath()); else if (file.isDirectory() && recursive) executePatternGenerator(file, recursive);
        }
    }

    private static void executePatternGenerator(String resourceName) {
        PatternGenerator pg = null;
        try {
            URL patternMetaData = URLHelper.newExtendedURL(resourceName);
            System.out.println("Processing the pattern meta data file: " + resourceName + '(' + patternMetaData + ')');
            pg = new PatternGenerator(patternMetaData);
            pg.processPattern();
        } catch (MalformedURLException e) {
            System.out.println("The pattern meta data file not found: " + resourceName);
            e.printStackTrace();
        } catch (Exception e) {
            e.printStackTrace();
        } finally {
            if (pg != null) pg.destroy();
        }
    }

    private static void usage() {
        System.err.println("Usage: PatternGenerator <PatternMetaDataFileName relative to ClassPath>");
        System.out.println("Alternately use: PatternGenerator <a directory with PatternMetaData files> [<-r>]");
        System.exit(1);
    }
}
