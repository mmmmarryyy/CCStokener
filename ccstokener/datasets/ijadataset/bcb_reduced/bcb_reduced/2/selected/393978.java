package org.personalsmartspace.ipojo.manipulator;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.MalformedURLException;
import java.net.URL;
import java.util.ArrayList;
import java.util.Enumeration;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.TreeMap;
import java.util.Vector;
import java.util.jar.Attributes;
import java.util.jar.JarEntry;
import java.util.jar.JarFile;
import java.util.jar.JarOutputStream;
import java.util.jar.Manifest;
import org.apache.felix.ipojo.metadata.Attribute;
import org.apache.felix.ipojo.metadata.Element;
import org.objectweb.asm.ClassReader;
import org.personalsmartspace.ipojo.manipulation.PSSInnerClassManipulator;
import org.personalsmartspace.ipojo.manipulation.PSSManipulator;
import org.personalsmartspace.ipojo.manipulation.annotations.PSSMetadataCollector;
import org.personalsmartspace.ipojo.xml.parser.PSSParseException;
import org.personalsmartspace.ipojo.xml.parser.PSSSchemaResolver;
import org.personalsmartspace.ipojo.xml.parser.PSSXMLMetadataParser;
import org.xml.sax.InputSource;
import org.xml.sax.SAXException;
import org.xml.sax.SAXParseException;
import org.xml.sax.XMLReader;
import org.xml.sax.helpers.XMLReaderFactory;

/**
 * Pojoization allows creating an iPOJO bundle from a "normal" bundle.
 * @author <a href="mailto:patx.cheevers@intel.com">Felix Project Team</a>
 */
public class PSSPojoization {

    /**
     * iPOJO Imported Package Version.
     */
    public static final String IPOJO_PACKAGE_VERSION = "0.2.1.3";

    /**
     * List of component types.
     */
    private List m_components;

    /**
     * Metadata (in internal format).
     */
    private Element[] m_metadata = new Element[0];

    /**
     * Errors which occur during the manipulation.
     */
    private List m_errors = new ArrayList();

    /**
     * Warnings which occur during the manipulation.
     */
    private List m_warnings = new ArrayList();

    /**
     * Class map (class name, byte[]).
     */
    private Map m_classes = new HashMap();

    /**
     * Referenced packages by the composite.
     */
    private List m_referredPackages;

    /**
     * Flag describing if we need or not compute annotations.
     * By default, compute the annotations.
     */
    private boolean m_ignoreAnnotations;

    /**
     * Flag describing if we need or not use local XSD files
     * (i.e. use the {@link PSSSchemaResolver} or not).
     * If <code>true</code> the local XSD are not used.
     */
    private boolean m_ignoreLocalXSD;

    /**
     * Input jar file.
     */
    private JarFile m_inputJar;

    /**
     * The manipulated directory.
     */
    private File m_dir;

    /**
     * The manifest location.
     */
    private File m_manifest;

    /**
     * Add an error in the error list.
     * @param mes : error message.
     */
    private void error(String mes) {
        System.err.println(mes);
        m_errors.add(mes);
    }

    /**
     * Add a warning in the warning list.
     * @param mes : warning message
     */
    public void warn(String mes) {
        m_warnings.add(mes);
    }

    public List getErrors() {
        return m_errors;
    }

    /**
     * Activates annotation processing.
     */
    public void disableAnnotationProcessing() {
        m_ignoreAnnotations = true;
    }

    /**
     * Activates the entity resolver loading
     * XSD files from the classloader.
     */
    public void setUseLocalXSD() {
        m_ignoreLocalXSD = false;
    }

    /**
     * Manipulates an input bundle.
     * This method creates an iPOJO bundle based on the given metadata file.
     * The original and final bundles must be different.
     * @param in the original bundle.
     * @param out the final bundle.
     * @param metadata the iPOJO metadata input stream.
     */
    public void pojoization(File in, File out, InputStream metadata) {
        parseXMLMetadata(metadata);
        if (m_metadata == null) {
            return;
        }
        try {
            m_inputJar = new JarFile(in);
        } catch (IOException e) {
            error("The input file " + in.getAbsolutePath() + " is not a Jar file");
            return;
        }
        computeDeclaredComponents();
        manipulateJarFile(out);
        for (int i = 0; i < m_components.size(); i++) {
            ComponentInfo ci = (ComponentInfo) m_components.get(i);
            if (!ci.m_isManipulated) {
                error("The component " + ci.m_classname + " is declared but not in the bundle");
            }
        }
    }

    /**
     * Manipulates an input bundle.
     * This method creates an iPOJO bundle based on the given metadata file.
     * The original and final bundles must be different.
     * @param in the original bundle.
     * @param out the final bundle.
     * @param metadataFile the iPOJO metadata file (XML).
     */
    public void pojoization(File in, File out, File metadataFile) {
        if (metadataFile != null) {
            parseXMLMetadata(metadataFile);
        }
        try {
            m_inputJar = new JarFile(in);
        } catch (IOException e) {
            error("The input file " + in.getAbsolutePath() + " is not a Jar file");
            return;
        }
        computeDeclaredComponents();
        manipulateJarFile(out);
        for (int i = 0; i < m_components.size(); i++) {
            ComponentInfo ci = (ComponentInfo) m_components.get(i);
            if (!ci.m_isManipulated) {
                error("The component " + ci.m_classname + " is declared but not in the bundle");
            }
        }
    }

    /**
     * Manipulates an expanded bundles.
     * Classes are in the specified directory.
     * this method allows to update a customized manifest.
     * @param directory the directory containing classes
     * @param metadataFile the metadata file
     * @param manifestFile the manifest file. <code>null</code> to use directory/META-INF/MANIFEST.mf
     */
    public void directoryPojoization(File directory, File metadataFile, File manifestFile) {
        if (metadataFile != null) {
            parseXMLMetadata(metadataFile);
        }
        if (directory.exists() && directory.isDirectory()) {
            m_dir = directory;
        } else {
            error("The directory " + directory.getAbsolutePath() + " does not exist or is not a directory.");
        }
        if (manifestFile != null) {
            if (manifestFile.exists()) {
                m_manifest = manifestFile;
            } else {
                error("The manifest file " + manifestFile.getAbsolutePath() + " does not exist");
            }
        }
        computeDeclaredComponents();
        manipulateDirectory();
        for (int i = 0; i < m_components.size(); i++) {
            ComponentInfo ci = (ComponentInfo) m_components.get(i);
            if (!ci.m_isManipulated) {
                error("The component " + ci.m_classname + " is declared but not in the bundle");
            }
        }
    }

    /**
     * Parse the content of the class to detect annotated classes.
     * @param inC the class to inspect.
     */
    private void computeAnnotations(byte[] inC) {
        ClassReader cr = new ClassReader(inC);
        PSSMetadataCollector xml = new PSSMetadataCollector();
        cr.accept(xml, 0);
        if (xml.isAnnotated()) {
            boolean toskip = false;
            for (int i = 0; !toskip && i < m_metadata.length; i++) {
                if (!m_metadata[i].getName().equals("instance") && m_metadata[i].containsAttribute("name") && m_metadata[i].getAttribute("name").equalsIgnoreCase(xml.getElem().getAttribute("name"))) {
                    toskip = true;
                    warn("The component type " + xml.getElem().getAttribute("name") + " is overriden by the metadata file");
                }
            }
            if (!toskip) {
                if (m_metadata != null && m_metadata.length > 0) {
                    Element[] newElementsList = new Element[m_metadata.length + 1];
                    System.arraycopy(m_metadata, 0, newElementsList, 0, m_metadata.length);
                    newElementsList[m_metadata.length] = xml.getElem();
                    m_metadata = newElementsList;
                } else {
                    m_metadata = new Element[] { xml.getElem() };
                }
                String name = m_metadata[m_metadata.length - 1].getAttribute("classname");
                name = name.replace('.', '/');
                name += ".class";
                ComponentInfo info = new ComponentInfo(name, m_metadata[m_metadata.length - 1]);
                info.m_bytecode = inC;
                m_components.add(info);
            }
        }
    }

    /**
     * Manipulate the input bundle.
     * @param out final bundle
     */
    private void manipulateJarFile(File out) {
        manipulateComponents();
        m_referredPackages = getReferredPackages();
        Manifest mf = doManifest();
        FileOutputStream fos = null;
        JarOutputStream jos = null;
        try {
            fos = new FileOutputStream(out);
            jos = new JarOutputStream(fos, mf);
        } catch (FileNotFoundException e1) {
            error("Cannot manipulate the Jar file : the output file " + out.getAbsolutePath() + " is not found");
            return;
        } catch (IOException e) {
            error("Cannot manipulate the Jar file : cannot access to " + out.getAbsolutePath());
            return;
        }
        try {
            Enumeration entries = m_inputJar.entries();
            while (entries.hasMoreElements()) {
                JarEntry curEntry = (JarEntry) entries.nextElement();
                if (m_classes.containsKey(curEntry.getName())) {
                    JarEntry je = new JarEntry(curEntry.getName());
                    byte[] outClazz = (byte[]) m_classes.get(curEntry.getName());
                    if (outClazz != null && outClazz.length != 0) {
                        jos.putNextEntry(je);
                        jos.write(outClazz);
                        jos.closeEntry();
                    } else {
                        jos.putNextEntry(curEntry);
                        InputStream currIn = m_inputJar.getInputStream(curEntry);
                        int c;
                        int i = 0;
                        while ((c = currIn.read()) >= 0) {
                            jos.write(c);
                            i++;
                        }
                        currIn.close();
                        jos.closeEntry();
                    }
                } else {
                    if (!curEntry.getName().equals("META-INF/MANIFEST.MF")) {
                        jos.putNextEntry(curEntry);
                        InputStream currIn = m_inputJar.getInputStream(curEntry);
                        int c;
                        int i = 0;
                        while ((c = currIn.read()) >= 0) {
                            jos.write(c);
                            i++;
                        }
                        currIn.close();
                        jos.closeEntry();
                    }
                }
            }
        } catch (IOException e) {
            error("Cannot manipulate the Jar file : " + e.getMessage());
            return;
        }
        try {
            m_inputJar.close();
            jos.close();
            fos.close();
            jos = null;
            fos = null;
        } catch (IOException e) {
            error("Cannot close the new Jar file : " + e.getMessage());
            return;
        }
    }

    /**
     * Manipulate the input directory.
     */
    private void manipulateDirectory() {
        manipulateComponents();
        m_referredPackages = getReferredPackages();
        Manifest mf = doManifest();
        if (mf == null) {
            error("Cannot found input manifest");
            return;
        }
        Iterator it = m_classes.entrySet().iterator();
        while (it.hasNext()) {
            Map.Entry entry = (Map.Entry) it.next();
            String classname = (String) entry.getKey();
            byte[] clazz = (byte[]) entry.getValue();
            File classFile = new File(m_dir, classname);
            try {
                OutputStream os = new FileOutputStream(classFile);
                os.write(clazz);
                os.close();
            } catch (IOException e) {
                error("Cannot manipulate the file : the output file " + classname + " is not found");
                return;
            }
        }
        if (m_manifest == null) {
            m_manifest = new File(m_dir, "META-INF/MANIFEST.MF");
            if (!m_manifest.exists()) {
                error("Cannot find the manifest file : " + m_manifest.getAbsolutePath());
                return;
            }
        } else {
            if (!m_manifest.exists()) {
                error("Cannot find the manifest file : " + m_manifest.getAbsolutePath());
                return;
            }
        }
        try {
            mf.write(new FileOutputStream(m_manifest));
        } catch (IOException e) {
            error("Cannot write the manifest file : " + e.getMessage());
        }
    }

    /**
     * Manipulate classes of the input Jar.
     */
    private void manipulateComponents() {
        if (!m_ignoreAnnotations) {
            Enumeration entries = getClassFiles();
            while (entries.hasMoreElements()) {
                String curName = (String) entries.nextElement();
                try {
                    byte[] in = getBytecode(curName);
                    computeAnnotations(in);
                } catch (IOException e) {
                    error("Cannot read the class : " + curName);
                    return;
                }
            }
        }
        for (int i = 0; i < m_components.size(); i++) {
            ComponentInfo info = (ComponentInfo) m_components.get(i);
            if (info.m_bytecode == null) {
                try {
                    info.m_bytecode = getBytecode(info.m_classname);
                } catch (IOException e) {
                    error("Cannot extract bytecode for component '" + info.m_classname + "'");
                    return;
                }
            }
            byte[] outClazz = manipulateComponent(info.m_bytecode, info);
            m_classes.put(info.m_classname, outClazz);
            if (!info.m_inners.isEmpty()) {
                for (int k = 0; k < info.m_inners.size(); k++) {
                    String innerCN = (String) info.m_inners.get(k) + ".class";
                    try {
                        byte[] innerClassBytecode = getBytecode(innerCN);
                        manipulateInnerClass(innerClassBytecode, innerCN, info);
                    } catch (IOException e) {
                        error("Cannot manipulate inner class '" + innerCN + "'");
                        return;
                    }
                }
            }
        }
    }

    /**
     * Return a byte array that contains the bytecode of the given classname.
     * @param classname name of a class to be read
     * @return a byte array
     * @throws IOException if the classname cannot be read
     */
    private byte[] getBytecode(final String classname) throws IOException {
        InputStream currIn = null;
        byte[] in = new byte[0];
        try {
            currIn = getInputStream(classname);
            int c;
            while ((c = currIn.read()) >= 0) {
                byte[] in2 = new byte[in.length + 1];
                System.arraycopy(in, 0, in2, 0, in.length);
                in2[in.length] = (byte) c;
                in = in2;
            }
        } finally {
            if (currIn != null) {
                try {
                    currIn.close();
                } catch (IOException e) {
                }
            }
        }
        return in;
    }

    /**
     * Gets an input stream on the given class.
     * This methods manages Jar files and directories.
     * @param classname the class name
     * @return the input stream
     * @throws IOException if the file cannot be read
     */
    private InputStream getInputStream(String classname) throws IOException {
        if (m_inputJar != null) {
            if (!classname.endsWith(".class")) {
                classname += ".class";
            }
            JarEntry je = m_inputJar.getJarEntry(classname);
            if (je == null) {
                throw new IOException("The class " + classname + " connot be found in the input Jar file");
            } else {
                return m_inputJar.getInputStream(je);
            }
        } else {
            File file = new File(m_dir, classname);
            return new FileInputStream(file);
        }
    }

    /**
     * Gets the list of class files.
     * The content of the returned enumeration contains file names.
     * It is possible to get input stream on those file by using the
     * {@link Pojoization#getInputStream(String)} method.
     * @return the list of class files.
     */
    private Enumeration getClassFiles() {
        Vector files = new Vector();
        if (m_inputJar != null) {
            Enumeration enumeration = m_inputJar.entries();
            while (enumeration.hasMoreElements()) {
                JarEntry je = (JarEntry) enumeration.nextElement();
                if (je.getName().endsWith(".class")) {
                    files.add(je.getName());
                }
            }
        } else {
            searchClassFiles(m_dir, files);
        }
        return files.elements();
    }

    /**
     * Navigates across directories to find class files.
     * @param dir the directory to analyze
     * @param classes discovered classes
     */
    private void searchClassFiles(File dir, List classes) {
        File[] files = dir.listFiles();
        for (int i = 0; i < files.length; i++) {
            if (files[i].isDirectory()) {
                searchClassFiles(files[i], classes);
            } else if (files[i].getName().endsWith(".class")) {
                classes.add(computeRelativePath(files[i].getAbsolutePath()));
            }
        }
    }

    /**
     * Computes a relative path for the given absolute path.
     * This methods computes the relative path according to the directory
     * containing classes for the given class path.
     * @param absolutePath the absolute path of the class
     * @return the relative path of the class based on the directory containing
     * classes.
     */
    private String computeRelativePath(String absolutePath) {
        String root = m_dir.getAbsolutePath();
        String path = absolutePath.substring(root.length() + 1);
        return path.replace('\\', '/');
    }

    /**
     * Manipulates an inner class.
     * @param in input bytecode of the inner file to manipulate
     * @param cn the inner class name (ends with .class)
     * @param ci component info of the component owning the inner class
     * @throws IOException the inner class cannot be read
     */
    private void manipulateInnerClass(byte[] in, String cn, ComponentInfo ci) throws IOException {
        String name = ci.m_classname.substring(0, ci.m_classname.length() - 6);
        PSSInnerClassManipulator man = new PSSInnerClassManipulator(name, ci.m_fields);
        byte[] out = man.manipulate(in);
        m_classes.put(cn, out);
    }

    /**
     * Gets the manifest.
     * This method handles Jar and directories.
     * For Jar file, the input jar manifest is returned.
     * For directories, if specified the specifies manifest is returned.
     * Otherwise, try directory/META-INF/MANIFEST.MF
     * @return the Manifest.
     * @throws IOException if the manifest cannot be found
     */
    private Manifest getManifest() throws IOException {
        if (m_inputJar != null) {
            return m_inputJar.getManifest();
        } else {
            if (m_manifest == null) {
                File manFile = new File(m_dir, "META-INF/MANIFEST.MF");
                if (manFile.exists()) {
                    return new Manifest(new FileInputStream(manFile));
                } else {
                    throw new IOException("Cannot find the manifest file : " + manFile.getAbsolutePath());
                }
            } else {
                if (m_manifest.exists()) {
                    return new Manifest(new FileInputStream(m_manifest));
                } else {
                    throw new IOException("Cannot find the manifest file : " + m_manifest.getAbsolutePath());
                }
            }
        }
    }

    /**
     * Create the manifest.
     * Set the bundle imports and iPOJO-components clauses
     * @return the generated manifest.
     */
    private Manifest doManifest() {
        Manifest mf = null;
        try {
            mf = getManifest();
        } catch (IOException e) {
            error("Cannot get the manifest : " + e.getMessage());
            return null;
        }
        Attributes att = mf.getMainAttributes();
        setImports(att);
        setPOJOMetadata(att);
        setCreatedBy(att);
        return mf;
    }

    /**
     * Manipulate a component class.
     * @param in : the byte array of the class to manipulate
     * @param ci : attached component info (containing metadata and manipulation metadata)
     * @return the generated class (byte array)
     */
    private byte[] manipulateComponent(byte[] in, ComponentInfo ci) {
        PSSManipulator man = new PSSManipulator();
        try {
            byte[] out = man.manipulate(in);
            ci.detectMissingFields(man.getFields());
            ci.m_componentMetadata.addElement(man.getManipulationMetadata());
            ci.m_isManipulated = true;
            ci.m_inners = man.getInnerClasses();
            ci.m_fields = man.getFields().keySet();
            return out;
        } catch (IOException e) {
            error("Cannot manipulate the class " + ci.m_classname + " : " + e.getMessage());
            return null;
        }
    }

    /**
     * Return the list of "concrete" component.
     */
    private void computeDeclaredComponents() {
        List componentClazzes = new ArrayList();
        for (int i = 0; i < m_metadata.length; i++) {
            String name = m_metadata[i].getAttribute("classname");
            if (name != null) {
                name = name.replace('.', '/');
                name += ".class";
                componentClazzes.add(new ComponentInfo(name, m_metadata[i]));
            }
        }
        m_components = componentClazzes;
    }

    /**
     * Component Info.
     * Represent a component type to be manipulated or already manipulated.
     * @author <a href="mailto:felix-dev@incubator.apache.org">Felix Project Team</a>
     */
    private class ComponentInfo {

        /**
         * Component Type metadata.
         */
        Element m_componentMetadata;

        /**
         * Component Type implementation class.
         */
        String m_classname;

        /**
         * Is the class already manipulated.
         */
        boolean m_isManipulated;

        /**
         * List of inner classes of the implementation class.
         */
        List m_inners;

        /**
         * Set of fields of the implementation class.
         */
        Set m_fields;

        /**
         * Initial (unmodified) bytecode of the component's class.
         * May be null !!
         */
        byte[] m_bytecode;

        /**
         * Constructor.
         * @param cn : class name
         * @param met : component type metadata
         */
        ComponentInfo(String cn, Element met) {
            this.m_classname = cn;
            this.m_componentMetadata = met;
            m_isManipulated = false;
        }

        /**
         * Detects missing fields.
         * If a referenced field does not exist in the class
         * the method throws an error breaking the build process.
         * @param fields : field found in the manipulated class
         */
        void detectMissingFields(Map fields) {
            List list = new ArrayList();
            computeReferredFields(list, m_componentMetadata);
            for (int i = 0; i < list.size(); i++) {
                if (!fields.containsKey(list.get(i))) {
                    error("The field " + list.get(i) + " is referenced in the " + "metadata but does not exist in the " + m_classname + " class");
                }
            }
        }

        /**
         * Looks for 'field' attribute in the given metadata.
         * @param list : discovered field (accumulator)
         * @param metadata : metadata to inspect
         */
        private void computeReferredFields(List list, Element metadata) {
            String field = metadata.getAttribute("field");
            if (field != null && !list.contains(field)) {
                list.add(field);
            }
            for (int i = 0; i < metadata.getElements().length; i++) {
                computeReferredFields(list, metadata.getElements()[i]);
            }
        }
    }

    /**
     * Set the create-by in the manifest.
     * @param att : manifest attribute.
     */
    private void setCreatedBy(Attributes att) {
        String prev = att.getValue("Created-By");
        if (prev == null) {
            att.putValue("Created-By", " PSS iPOJO " + IPOJO_PACKAGE_VERSION);
        } else {
            if (prev.indexOf("iPOJO") == -1) {
                att.putValue("Created-By", prev + " & PSS iPOJO " + IPOJO_PACKAGE_VERSION);
            }
        }
    }

    /**
     * Add imports to the given manifest attribute list. This method add ipojo imports and handler imports (if needed).
     * @param att : the manifest attribute list to modify.
     */
    private void setImports(Attributes att) {
        Map imports = parseHeader(att.getValue("Import-Package"));
        Map ver = new TreeMap();
        ver.put("version", IPOJO_PACKAGE_VERSION);
        if (!imports.containsKey("org.personalsmartspace.ipojo")) {
            imports.put("org.personalsmartspace.ipojo", new TreeMap());
        }
        if (!imports.containsKey("org.personalsmartspace.ipojo.architecture")) {
            imports.put("org.personalsmartspace.ipojo.architecture", new TreeMap());
        }
        if (!imports.containsKey("org.osgi.service.cm")) {
            Map verCM = new TreeMap();
            verCM.put("version", "1.2");
            imports.put("org.osgi.service.cm", verCM);
        }
        if (!imports.containsKey("org.osgi.service.log")) {
            Map verCM = new TreeMap();
            verCM.put("version", "1.3");
            imports.put("org.osgi.service.log", verCM);
        }
        for (int i = 0; i < m_referredPackages.size(); i++) {
            String pack = (String) m_referredPackages.get(i);
            imports.put(pack, new TreeMap());
        }
        att.putValue("Import-Package", printClauses(imports, "resolution:"));
    }

    /**
     * Add iPOJO-Components to the given manifest attribute list. This method add the iPOJO-Components header and its value (according to the metadata) to the manifest.
     * @param att : the manifest attribute list to modify.
     */
    private void setPOJOMetadata(Attributes att) {
        StringBuffer meta = new StringBuffer();
        for (int i = 0; i < m_metadata.length; i++) {
            meta.append(buildManifestMetadata(m_metadata[i], new StringBuffer()));
        }
        if (meta.length() != 0) {
            att.putValue("iPOJO-Components", meta.toString());
        }
    }

    /**
     * Standard OSGi header parser. This parser can handle the format clauses ::= clause ( ',' clause ) + clause ::= name ( ';' name ) (';' key '=' value )
     * This is mapped to a Map { name => Map { attr|directive => value } }
     *
     * @param value : String to parse.
     * @return parsed map.
     */
    public Map parseHeader(String value) {
        if (value == null || value.trim().length() == 0) {
            return new HashMap();
        }
        Map result = new HashMap();
        PSSQuotedTokenizer qt = new PSSQuotedTokenizer(value, ";=,");
        char del;
        do {
            boolean hadAttribute = false;
            Map clause = new HashMap();
            List aliases = new ArrayList();
            aliases.add(qt.nextToken());
            del = qt.getSeparator();
            while (del == ';') {
                String adname = qt.nextToken();
                if ((del = qt.getSeparator()) != '=') {
                    if (hadAttribute) {
                        throw new IllegalArgumentException("Header contains name field after attribute or directive: " + adname + " from " + value);
                    }
                    aliases.add(adname);
                } else {
                    String advalue = qt.nextToken();
                    clause.put(adname, advalue);
                    del = qt.getSeparator();
                    hadAttribute = true;
                }
            }
            for (Iterator i = aliases.iterator(); i.hasNext(); ) {
                result.put(i.next(), clause);
            }
        } while (del == ',');
        return result;
    }

    /**
     * Print a standard Map based OSGi header.
     *
     * @param exports : map { name => Map { attribute|directive => value } }
     * @param allowedDirectives : list of allowed directives.
     * @return the clauses
     */
    public String printClauses(Map exports, String allowedDirectives) {
        StringBuffer sb = new StringBuffer();
        String del = "";
        for (Iterator i = exports.entrySet().iterator(); i.hasNext(); ) {
            Map.Entry entry = (Map.Entry) i.next();
            String name = (String) entry.getKey();
            Map map = (Map) entry.getValue();
            sb.append(del);
            sb.append(name);
            for (Iterator j = map.entrySet().iterator(); j.hasNext(); ) {
                Map.Entry entry2 = (Map.Entry) j.next();
                String key = (String) entry2.getKey();
                if (key.endsWith(":") && allowedDirectives.indexOf(key) < 0) {
                    continue;
                }
                String value = (String) entry2.getValue();
                sb.append(";");
                sb.append(key);
                sb.append("=");
                boolean dirty = value.indexOf(',') >= 0 || value.indexOf(';') >= 0;
                if (dirty) {
                    sb.append("\"");
                }
                sb.append(value);
                if (dirty) {
                    sb.append("\"");
                }
            }
            del = ", ";
        }
        return sb.toString();
    }

    /**
     * Parse the XML metadata from the given file.
     * @param metadataFile the metadata file
     */
    private void parseXMLMetadata(File metadataFile) {
        try {
            InputStream stream = null;
            URL url = metadataFile.toURL();
            if (url == null) {
                warn("Cannot find the metadata file : " + metadataFile.getAbsolutePath());
                m_metadata = new Element[0];
            } else {
                stream = url.openStream();
                parseXMLMetadata(stream);
            }
        } catch (MalformedURLException e) {
            error("Cannot open the metadata input stream from " + metadataFile.getAbsolutePath() + ": " + e.getMessage());
            m_metadata = null;
        } catch (IOException e) {
            error("Cannot open the metadata input stream: " + metadataFile.getAbsolutePath() + ": " + e.getMessage());
            m_metadata = null;
        }
    }

    /**
     * Parses XML Metadata.
     * @param stream metadata input stream.
     */
    private void parseXMLMetadata(InputStream stream) {
        Element[] meta = null;
        try {
            XMLReader parser = XMLReaderFactory.createXMLReader();
            PSSXMLMetadataParser handler = new PSSXMLMetadataParser();
            parser.setContentHandler(handler);
            parser.setFeature("http://xml.org/sax/features/validation", true);
            parser.setFeature("http://apache.org/xml/features/validation/schema", true);
            parser.setErrorHandler(handler);
            if (!m_ignoreLocalXSD) {
                parser.setEntityResolver(new PSSSchemaResolver());
            }
            InputSource is = new InputSource(stream);
            parser.parse(is);
            meta = handler.getMetadata();
            stream.close();
        } catch (IOException e) {
            error("Cannot open the metadata input stream: " + e.getMessage());
        } catch (PSSParseException e) {
            error("Parsing error when parsing the XML file: " + e.getMessage());
        } catch (SAXParseException e) {
            error("Error during metadata parsing at line " + e.getLineNumber() + " : " + e.getMessage());
        } catch (SAXException e) {
            error("Parsing error when parsing (Sax Error) the XML file: " + e.getMessage());
        }
        if (meta == null || meta.length == 0) {
            warn("Neither component types, nor instances in the metadata");
        }
        m_metadata = meta;
    }

    /**
     * Get packages referenced by component.
     * @return the list of referenced packages.
     */
    private List getReferredPackages() {
        List referred = new ArrayList();
        for (int i = 0; i < m_metadata.length; i++) {
            Element[] elems = m_metadata[i].getElements();
            for (int j = 0; j < elems.length; j++) {
                String att = elems[j].getAttribute("specification");
                if (att != null) {
                    int last = att.lastIndexOf('.');
                    if (last != -1) {
                        referred.add(att.substring(0, last));
                    }
                }
            }
        }
        return referred;
    }

    /**
     * Generate manipulation metadata.
     * @param element : actual element.
     * @param actual : actual manipulation metadata.
     * @return : given manipulation metadata + manipulation metadata of the given element.
     */
    private StringBuffer buildManifestMetadata(Element element, StringBuffer actual) {
        StringBuffer result = new StringBuffer();
        if (element.getNameSpace() == null) {
            result.append(actual + element.getName() + " { ");
        } else {
            result.append(actual + element.getNameSpace() + ":" + element.getName() + " { ");
        }
        Attribute[] atts = element.getAttributes();
        for (int i = 0; i < atts.length; i++) {
            Attribute current = (Attribute) atts[i];
            if (current.getNameSpace() == null) {
                result.append("$" + current.getName() + "=\"" + current.getValue() + "\" ");
            } else {
                result.append("$" + current.getNameSpace() + ":" + current.getName() + "=\"" + current.getValue() + "\" ");
            }
        }
        Element[] elems = element.getElements();
        for (int i = 0; i < elems.length; i++) {
            result = buildManifestMetadata(elems[i], result);
        }
        result.append("}");
        return result;
    }

    public List getWarnings() {
        return m_warnings;
    }
}
