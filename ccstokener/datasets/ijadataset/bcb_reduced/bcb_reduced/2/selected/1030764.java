package eulergui.inputs;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.io.PrintWriter;
import java.lang.reflect.InvocationTargetException;
import java.net.URI;
import java.net.URISyntaxException;
import java.net.URL;
import java.util.ArrayList;
import java.util.Enumeration;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.StringTokenizer;
import java.util.zip.ZipEntry;
import java.util.zip.ZipFile;
import java.util.zip.ZipInputStream;
import deductions.Namespaces;
import jdepend.framework.ClassFileParserWithMethodsAndFields;
import jdepend.framework.JavaClassWithMethodsAndFields;
import edu.mit.simile.producers.bytecode.parser.Clazz;
import edu.mit.simile.producers.bytecode.parser.Packg;
import edu.mit.simile.producers.bytecode.parser.Parser;
import eulergui.util.URLHelper;

/**
 * XMI (eCore) To N3 Converter:
 * just converts XMI "triples" into RDF triples, package namespaces into RDF prefixes;
 * there is no intelligence at all here (like transforming UML primitive types into XSD, etc);
 * the intelligence is in rules downstream.
 * <p/>
 * Caution: when the XMI has several values
 * for one object and the same structuralFeature,
 * it become duplicated triples, that are eliminated by CWM .
 */
public class JavaToN3Converter implements N3Converter {

    static final String ONTOLOGY_URI = "http://simile.mit.edu/2004/09/ontologies/java#";

    static final String CLASS_LABEL = "Class";

    static final String ABSTRACT_CLASS_LABEL = "Abstract_Class";

    static final String INTERFACE_LABEL = "Interface";

    public static HashMap<String, Packg> packageMap = new HashMap<String, Packg>();

    static final String[] ZIP_FILE_TYPES = new String[] { ".zip", ".jar", ".war", ".ear" };

    static boolean isZipFile(File file) {
        boolean result = false;
        final String name = file.getName();
        for (int i = 0; i < ZIP_FILE_TYPES.length; i++) {
            if (name.endsWith(ZIP_FILE_TYPES[i])) {
                result = true;
                break;
            }
        }
        return result;
    }

    static void processJar(URL url, PrintWriter writer) throws IOException, NoSuchFieldException, IllegalAccessException {
        processZipFile(url, writer);
    }

    static void processFile(File file, PrintWriter writer) throws IOException, NoSuchFieldException, IllegalAccessException {
        if (file.isDirectory()) {
            final File[] files = file.listFiles();
            for (int i = 0; i < files.length; i++) {
                System.err.println("Processing folder: " + file.getAbsolutePath());
                processFile(files[i], writer);
            }
        } else if (isZipFile(file)) {
            System.err.println("Processing archive: " + file.getAbsolutePath());
            processZipFile(file, writer);
        } else if (file.getName().endsWith(".class")) {
            System.err.println("Processing class: " + file.getAbsolutePath());
            processClass(file.getAbsolutePath(), new FileInputStream(file), writer);
            processFieldsAndMethods(file.getAbsolutePath(), new FileInputStream(file), writer);
        } else {
            System.err.println("Ignoring file: " + file.getAbsolutePath());
        }
    }

    static void processZipFile(File file, PrintWriter writer) throws IOException, NoSuchFieldException, IllegalAccessException {
        final ZipFile zipFile = new ZipFile(file.getCanonicalFile());
        final Enumeration<?> entries = zipFile.entries();
        while (entries.hasMoreElements()) {
            final ZipEntry entry = (ZipEntry) entries.nextElement();
            if (!entry.isDirectory() && entry.getName().endsWith(".class")) {
                processClass(file.getAbsolutePath(), zipFile.getInputStream(entry), writer);
                processFieldsAndMethods(file.getAbsolutePath(), zipFile.getInputStream(entry), writer);
            }
        }
    }

    static void processZipFile(URL url, PrintWriter writer) throws IOException, NoSuchFieldException, IllegalAccessException {
        final ZipInputStream zipStream = new ZipInputStream(url.openStream());
        ZipEntry entry;
        while ((entry = zipStream.getNextEntry()) != null) {
            if (!entry.isDirectory() && entry.getName().endsWith(".class")) {
                final byte[] b = unzipEntry(zipStream);
                processClass(url.toString() + "#" + entry.getName(), new ByteArrayInputStream(b), writer);
                processFieldsAndMethods(url.toString() + "#" + entry.getName(), new ByteArrayInputStream(b), writer);
            }
        }
    }

    private static byte[] unzipEntry(ZipInputStream zipStream) throws IOException {
        final ByteArrayOutputStream streamBuilder = new ByteArrayOutputStream();
        int bytesRead;
        final byte[] tempBuffer = new byte[8192 * 2];
        try {
            while ((bytesRead = zipStream.read(tempBuffer)) != -1) {
                streamBuilder.write(tempBuffer, 0, bytesRead);
            }
            return streamBuilder.toByteArray();
        } catch (final IOException e) {
            e.printStackTrace();
            throw (e);
        }
    }

    static String convertType(int type) {
        if (type == Clazz.CLASS_TYPE) {
            return CLASS_LABEL;
        } else if (type == Clazz.ABSTRACT_CLASS_TYPE) {
            return ABSTRACT_CLASS_LABEL;
        } else if (type == Clazz.INTERFACE_TYPE) {
            return INTERFACE_LABEL;
        } else {
            return null;
        }
    }

    static void processClass(String path, InputStream input, PrintWriter writer) throws IOException {
        try {
            final Clazz clazz = Parser.parse(input, path);
            final Packg packg = clazz.getPackage();
            processPackage(packg, writer);
            final String name = clazz.getName();
            writer.println("<urn:java:" + name + ">");
            final String type = convertType(clazz.getType());
            if (type != null) {
                writer.println("   a :" + type + " ;");
            }
            writer.println("   dc:title \"" + name + "\"@en ;");
            writer.println("   :contained <urn:java:" + packg.getName() + "> ;");
            final String location = clazz.getLocation();
            if (location != null) {
                writer.println("   :located <file://" + location + "> ;");
            }
            final Iterator<?> j = clazz.getDependencies().iterator();
            while (j.hasNext()) {
                final String depname = (String) j.next();
                writer.println("   :uses <urn:java:" + depname + "> ;");
            }
            writer.println(".\n");
        } catch (final ArrayIndexOutOfBoundsException ex) {
            System.err.println("Skipping illegal class '" + path + "'");
        }
    }

    static void processFieldsAndMethods(String path, InputStream input, PrintWriter writer) throws IOException, NoSuchFieldException, IllegalAccessException {
        try {
            final ClassFileParserWithMethodsAndFields parser = new ClassFileParserWithMethodsAndFields();
            final JavaClassWithMethodsAndFields clazz = parser.parse2(input);
            processClassPrototype(clazz, writer);
            for (final Object fieldObj : clazz.getFieldsInfo()) {
                final ClassFileParserWithMethodsAndFields.ClassField field = parser.asClassField(fieldObj);
                processField(clazz, field, writer);
            }
            for (final Object methodObj : clazz.getMethodsInfo()) {
                final ClassFileParserWithMethodsAndFields.ClassMethod method = parser.asClassMethod(methodObj);
                processMethod(clazz, method, writer);
            }
        } catch (final ArrayIndexOutOfBoundsException ex) {
            System.err.println("Skipping illegal class '" + path + "'");
        }
    }

    private static void processClassPrototype(JavaClassWithMethodsAndFields clazz, PrintWriter writer) {
        final String superclassName = clazz.getSuperClassName();
        final String[] interfaces = clazz.getInterfaceNames();
        final boolean hasSuperClass = superclassName != null && !superclassName.isEmpty() && !superclassName.equals("java.lang.Object");
        final boolean hasInterfaces = interfaces.length > 0;
        if (hasSuperClass || hasInterfaces) {
            writer.println("<" + "urn:java:" + clazz.getName() + ">");
            if (hasSuperClass) {
                writer.print("   :extends <" + formatJavaIdentifier(superclassName) + "> ");
                if (hasInterfaces) {
                    writer.println("; ");
                } else {
                    writer.println(".");
                }
            }
            if (hasInterfaces) {
                writer.print("   :implements ");
                for (int i = 0, interfacesLength = interfaces.length; i < interfacesLength; i++) {
                    final String anInterface = interfaces[i];
                    writer.print("<" + formatJavaIdentifier(anInterface) + ">");
                    if (i < interfacesLength - 1) {
                        writer.print(", ");
                    } else {
                        writer.println(". ");
                    }
                }
            }
            writer.println("");
        }
    }

    private static void processMethod(JavaClassWithMethodsAndFields clazz, ClassFileParserWithMethodsAndFields.ClassMethod method, PrintWriter writer) {
        String methodName = null;
        try {
            methodName = method.getName().replaceAll("[<>]", "");
        } catch (final InvocationTargetException e) {
            e.printStackTrace();
        } catch (final NoSuchMethodException e) {
            e.printStackTrace();
        } catch (final IllegalAccessException e) {
            e.printStackTrace();
        }
        if (methodName != null) {
            writer.println("<" + "urn:java:" + clazz.getName() + ">");
            final String methodId = "urn:java:" + clazz.getName() + "#" + methodName;
            writer.println("   :hasMethod <" + methodId + "> .");
            writer.println("<" + methodId + ">");
            writer.println("   " + "a " + ":method" + ";");
            writer.println("   " + ":hasName \"" + methodName + "\";");
            String methodDescriptor = null;
            try {
                methodDescriptor = method.getDescriptor();
                final String returnType = getReturnType(methodDescriptor);
                writer.println("   " + ":hasReturnType <" + returnType + ">" + ";");
                final List<String> parameters = getParameters(methodDescriptor);
                if (!parameters.isEmpty()) {
                    writer.print("   " + ":hasParameter ");
                    boolean first = true;
                    for (final Iterator<String> iterator = parameters.iterator(); iterator.hasNext(); ) {
                        final String parameter = iterator.next();
                        if (!first) {
                            writer.print("                 ");
                        }
                        writer.print("[ :hasType <" + parameter + "> ]");
                        if (iterator.hasNext()) {
                            writer.print(",");
                        }
                        writer.println("");
                        first = false;
                    }
                }
                writer.println(" .\n");
            } catch (final InvocationTargetException e) {
                e.printStackTrace();
            } catch (final NoSuchMethodException e) {
                e.printStackTrace();
            } catch (final IllegalAccessException e) {
                e.printStackTrace();
            }
            writer.println("");
        }
    }

    private static String getReturnType(String methodDescriptor) {
        final String returnTypeString = methodDescriptor.substring(methodDescriptor.indexOf(")") + 2);
        if (returnTypeString == null || returnTypeString.isEmpty()) {
            return "urn:java:void";
        } else {
            return formatJavaIdentifier(returnTypeString);
        }
    }

    private static List<String> getParameters(String methodDescriptor) {
        final List<String> parameterTypes = new ArrayList<String>();
        final String parameters = methodDescriptor.substring(1, methodDescriptor.indexOf(")"));
        final StringTokenizer tok = new StringTokenizer(parameters, ";", false);
        while (tok.hasMoreTokens()) {
            String s = tok.nextToken();
            if (s.startsWith("[")) {
                s = s.substring(1);
            }
            final String refinedParameter = formatJavaIdentifier(s);
            parameterTypes.add(refinedParameter);
        }
        return parameterTypes;
    }

    private static String formatJavaIdentifier(String s) {
        if ((s.isEmpty())) {
            return "";
        }
        if (s.endsWith(";")) {
            s = s.substring(0, s.lastIndexOf(";"));
        }
        if (s.startsWith("[")) {
            s = s.substring(1);
        }
        if (s.startsWith("Z") || s.startsWith("I") || s.startsWith("V") || s.startsWith("L")) {
            s = s.substring(1);
        }
        final String s1 = s.replaceAll("/", ".");
        return "urn:java:" + s1;
    }

    private static void processField(JavaClassWithMethodsAndFields clazz, ClassFileParserWithMethodsAndFields.ClassField field, PrintWriter writer) {
        String fieldName = null;
        try {
            fieldName = field.getName();
        } catch (final InvocationTargetException e) {
            e.printStackTrace();
        } catch (final NoSuchMethodException e) {
            e.printStackTrace();
        } catch (final IllegalAccessException e) {
            e.printStackTrace();
        }
        if (fieldName != null) {
            writer.println("<" + "urn:java:" + clazz.getName() + ">");
            final String fieldId = "urn:java:" + clazz.getName() + "#" + fieldName;
            writer.println("   :hasField <" + fieldId + "> .");
            writer.println("<" + fieldId + ">");
            writer.println("   " + "a " + ":field" + ";");
            writer.println("   " + ":hasName \"" + fieldName + "\";");
            String fieldType = null;
            try {
                fieldType = field.getDescriptor();
                writer.println("   " + ":hasType <" + formatJavaIdentifier(fieldType) + ">" + ".");
            } catch (final InvocationTargetException e) {
                e.printStackTrace();
            } catch (final NoSuchMethodException e) {
                e.printStackTrace();
            } catch (final IllegalAccessException e) {
                e.printStackTrace();
            }
            writer.println("");
        }
    }

    static void processPackage(Packg packg, PrintWriter writer) throws IOException {
        final Packg parent = packg.getParent();
        if (parent != null) {
            processPackage(parent, writer);
        }
        final String name = packg.getName();
        if (!packageMap.containsKey(name)) {
            writer.println("<urn:java:" + name + ">");
            writer.println("   a  :Package ;");
            writer.println("   dc:title \"" + name + "\"@en ;");
            if (parent != null) {
                writer.println("   :contained <urn:java:" + parent.getName() + "> ;");
            }
            writer.println(".\n");
            packageMap.put(name, packg);
        }
    }

    /**
     * @see eulergui.inputs.N3Converter#loadURIAndTranslateToN3(org.eclipse.emf.common.util.URI, java.io.OutputStream)
     */
    @Override
    public void loadURIAndTranslateToN3(URI uri, OutputStream outputStream) {
        final URI uri_java = uri;
        loadJavaClassesTreeAndTranslateToN3(uri_java, outputStream);
    }

    @Override
    public void loadURLAndTranslateToN3(URL url, OutputStream outputStream) {
        loadJavaJarAndTranslateToN3(url, outputStream);
    }

    /**
     * load eCore URI and translate To N3
     */
    public static void loadJavaClassesTreeAndTranslateToN3(URI uri, OutputStream outputStream) {
        final PrintWriter writer = new PrintWriter(outputStream);
        writer.println(Namespaces.declareImportantPrefixes() + "@prefix :      <" + ONTOLOGY_URI + "> .\n");
        try {
            processFile(new File(uri), writer);
        } catch (final IOException e) {
            e.printStackTrace();
        } catch (final IllegalAccessException e) {
            e.printStackTrace();
        } catch (final NoSuchFieldException e) {
            e.printStackTrace();
        }
        writer.println("#end of file");
        writer.flush();
        writer.close();
    }

    public static void loadJavaJarAndTranslateToN3(URL url, OutputStream outputStream) {
        final PrintWriter writer = new PrintWriter(outputStream);
        writer.println(Namespaces.declareImportantPrefixes() + "@prefix :      <" + ONTOLOGY_URI + "> .\n");
        try {
            processJar(url, writer);
        } catch (final IOException e) {
            e.printStackTrace();
        } catch (final NoSuchFieldException e) {
            e.printStackTrace();
        } catch (final IllegalAccessException e) {
            e.printStackTrace();
        }
        writer.println("#end of file");
        writer.flush();
        writer.close();
    }

    public static synchronized File getLocalN3FromJava(String javaUrl, File n3File) {
        return getN3FromJava(javaUrl, n3File);
    }

    public static File getN3FromJava(final String uriString, File n3File) {
        try {
            final URI uri = new URI(uriString);
            if (URLHelper.isDirectory(uri)) {
                loadJavaClassesTreeAndTranslateToN3(uri, new FileOutputStream(n3File));
            } else {
                loadJavaJarAndTranslateToN3(uri.toURL(), new FileOutputStream(n3File));
            }
        } catch (final FileNotFoundException e) {
            e.printStackTrace();
        } catch (final URISyntaxException e) {
            e.printStackTrace();
        } catch (final IOException e) {
            e.printStackTrace();
        }
        return n3File;
    }
}
