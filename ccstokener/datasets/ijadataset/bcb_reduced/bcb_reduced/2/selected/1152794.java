package com.objectwave.tools;

import com.objectwave.classFile.*;
import com.objectwave.exception.ConfigurationException;
import com.objectwave.logging.MessageLog;
import com.objectwave.sourceGenerator.*;
import com.objectwave.sourceModel.*;
import com.objectwave.sourceParser.*;
import com.objectwave.templateMerge.*;
import com.objectwave.utility.FileFinder;
import java.io.*;
import java.lang.reflect.*;
import java.util.*;

/**
 *  Used to add the method signatures from one class to another class.
 *
 * @author  Dave Hoag
 * @version  $Id: GenerateTemplateInto.java,v 2.2 2001/10/19 14:21:00 dave_hoag Exp $
 */
public class GenerateTemplateInto {

    boolean xmlInitialized;

    KnownTemplates templates;

    String miscData;

    TokenProvider tokenProvider;

    /**
	 */
    public GenerateTemplateInto() {
    }

    /**
	 *  Defaults to using JavaClassDef.dat file for template source.
	 *
	 * @param  args java.lang.String[]
	 * @author  Dave Hoag
	 */
    public static void main(String args[]) {
        args = cleanupArgs(args);
        int argIdx = 0;
        if (args.length == 0) {
            System.out.println("Usage: java [-DtemplateXml=fileName -DtokenProvider=fullyQualifiedClassName] [-DdatFile=fName] [-Dmisc=sdsd] [-DuseSource] [-Dheader] [-DuseRealClassName] GenerateTemplateInto fullTargetClassName [sourceClassName]  [templateName(s)]");
            System.out.println("        [-DtemplateXml=fileName -DtokenProvider=fullyQualifiedClassName]: \n           Use an XML file for the template definition - optionally specify token provider class ");
            System.out.println("        [-DdatFile=fName] - Use the specified serialized object dat file - default to JavaClassDef.dat ");
            System.out.println("        [-DuseSource] - The template will need to read in an additional file to provide values - like generating a delegate class. ");
            System.out.println("        [-DdirectPath] - It will use the whole path/filename instead of getting it from the class definition");
            System.out.println("examples\n     : java GenerateTemplateInto <aFile.java> <templates>");
            System.out.println("     : java -DuseSource GenerateTemplateInto com.objectwave.NewClassDelegate com.objectwave.SourceClassToWrap <templates>");
            return;
        }
        String targetClassName = args[argIdx++];
        String targetPackageName = null;
        String sourceFileName = null;
        if (System.getProperty("useSource") != null) {
            sourceFileName = args[argIdx++];
        }
        MergeTemplate[] temps = new MergeTemplate[args.length - argIdx];
        GenerateTemplateInto ex = new GenerateTemplateInto();
        try {
            String templateXml = System.getProperty("templateXml");
            if (templateXml != null) {
                ex.initFromXml(templateXml);
            }
            String datFile = System.getProperty("datFile", "JavaClassDef.dat");
            ex.init(datFile);
            ex.setMiscData(System.getProperty("misc"));
            for (int i = argIdx; i < args.length; ++i) {
                MessageLog.debug(ex, "Getting Template: " + args[i]);
                temps[i - argIdx] = ex.templates.getTemplate(args[i]);
            }
            if (temps.length == 0) {
                MessageLog.warn(ex, "No templates to generate");
                return;
            }
            if (sourceFileName != null) {
                int idx = targetClassName.lastIndexOf('.');
                if (idx > -1) {
                    targetPackageName = targetClassName.substring(0, idx);
                    targetClassName = targetClassName.substring(idx + 1);
                }
                ex.generateTemplates(temps, targetPackageName, targetClassName, sourceFileName);
            } else {
                ex.generateTemplates(temps, targetClassName);
            }
        } catch (Exception e) {
            e.printStackTrace();
            System.exit(1);
        }
        System.exit(0);
    }

    /**
	 *  Sometimes I pass -D arguments on the command line by mistake
	 *
	 * @param  args
	 * @return
	 */
    protected static String[] cleanupArgs(String[] args) {
        ArrayList list = new ArrayList();
        for (int i = 0; i < args.length; i++) {
            if (args[i].startsWith("-D")) {
                int idx = args[i].indexOf('=');
                if (idx < 0) {
                    idx = args[i].length();
                }
                String key = args[i].substring(2, idx);
                String value = "";
                if (idx < args[i].length()) {
                    value = args[i].substring(idx + 1);
                }
                MessageLog.debug(new GenerateTemplateInto(), "Setting prop " + key + " to " + value);
                System.setProperty(key, value);
            } else {
                list.add(args[i]);
            }
        }
        String[] result = new String[list.size()];
        list.toArray(result);
        return result;
    }

    /**
	 *  If this is true, ignore any calls to the init methods.
	 *
	 * @param  initialized The new Initialized value
	 */
    public void setInitialized(boolean initialized) {
        xmlInitialized = initialized;
    }

    /**
	 *  MiscData is the value used for the %MISC% token in the ClassInformation
	 *  object.
	 *
	 * @param  str The new MiscData value
	 */
    public void setMiscData(String str) {
        miscData = str;
    }

    /**
	 *  Sets the Templates attribute of the GenerateTemplateInto object
	 *
	 * @param  temps The new Templates value
	 */
    protected void setTemplates(KnownTemplates temps) {
        templates = temps;
    }

    /**
	 *  Gets the MiscData attribute of the GenerateTemplateInto object
	 *
	 * @return  The MiscData value
	 */
    public String getMiscData() {
        return miscData;
    }

    /**
	 *  Use reflection to create the source necessary to create a new file. All we
	 *  can do is copy the method signature.
	 *
	 * @param  className
	 * @param  targetPackageName
	 * @return  The ReaderForClass value
	 * @exception  IOException
	 */
    protected Reader getReaderForClass(String className, String targetPackageName) throws IOException {
        ClassNotFoundException exception;
        try {
            Class c = Class.forName(className);
            return getReaderForClass(c, targetPackageName);
        } catch (ClassNotFoundException ex) {
            exception = ex;
            throw new IOException("The source for " + className + " can not be found.\n" + exception);
        }
    }

    /**
	 *  Use reflection to create the source necessary to create a new file. All we
	 *  can do is copy the method signature.
	 *
	 * @param  clazz
	 * @param  targetPackageName
	 * @return  The ReaderForClass value
	 * @exception  IOException
	 */
    protected Reader getReaderForClass(Class clazz, final String targetPackageName) throws IOException {
        ReflectedClassSource fakeSource = new ReflectedClassSource();
        StringBuffer buffer = fakeSource.getSource(clazz, targetPackageName);
        return new StringReader(buffer.toString());
    }

    /**
	 *  Return a 'reader' that will be able to read Character values that will be
	 *  in the form of a valid java source code file. If the provided file is a
	 *  '.class' file, then we will create a fake '.java' file that will be parsed
	 *  into it's component pieces.
	 *
	 * @param  fileName
	 * @param  targetPackageName
	 * @return  The Reader value
	 * @exception  IOException
	 * @exception  Exception
	 * @see  classFile.ClassFile
	 */
    protected Reader getReader(String fileName, String targetPackageName) throws IOException, Exception {
        File f = new File(fileName);
        if (!f.exists()) {
            return getReaderForClass(fileName, targetPackageName);
        }
        if (f.getName().endsWith(".class")) {
            return createReaderFromClassFile(f);
        } else {
            return new FileReader(f);
        }
    }

    /**
	 * @param  els
	 * @param  targetClassName
	 * @return  The TargetDef value
	 * @exception  IOException
	 * @exception  FileNotFoundException
	 */
    protected JavaClassDef getTargetDef(ClassElement[] els, String targetClassName) throws IOException, FileNotFoundException {
        File f = new File(targetClassName);
        ClassElement[] newOnes = null;
        if (f.exists()) {
            SourceCodeReader rdr = new SourceCodeReader(new FileReader(f));
            newOnes = rdr.parseSource();
        } else {
            int idx = targetClassName.lastIndexOf('.');
            String targetPackageName = null;
            if (idx > -1) {
                targetPackageName = targetClassName.substring(0, idx);
                targetClassName = targetClassName.substring(idx + 1);
            }
            Vector temp = new Vector();
            ClassSpec spec = new ClassSpec();
            newOnes = new ClassElement[1];
            newOnes[0] = spec;
            if (els.length != 0) {
                int i = 0;
                while (!(els[i] instanceof ClassSpec)) {
                    temp.addElement(els[i]);
                    i++;
                }
                newOnes = new ClassElement[temp.size() + 1];
                temp.copyInto(newOnes);
                ClassSpec original = (ClassSpec) els[i];
                if (original.isInterfaceDef()) {
                    spec.setImplementors(new String[] { original.getClassName() });
                }
            }
            spec.setClassName(targetClassName);
            if (targetPackageName == null) {
                spec.setFullText("\npublic class " + targetClassName + " { ");
            } else {
                ClassElement[] addPackage = new ClassElement[newOnes.length + 1];
                System.arraycopy(newOnes, 0, addPackage, 1, newOnes.length);
                PackageDef def = new PackageDef();
                def.setFullText("package " + targetPackageName + ';');
                addPackage[0] = def;
                newOnes = addPackage;
            }
            spec.setTrailText("\n}");
            spec.setChildElements(new ClassElement[0]);
            newOnes[newOnes.length - 1] = spec;
        }
        JavaClassDef def = new JavaClassDef(newOnes);
        return def;
    }

    /**
	 * @return  The TokenProvider value
	 */
    protected TokenProvider getTokenProvider() {
        if (tokenProvider == null) {
            String className = System.getProperty("tokenProvider");
            if (className != null) {
                try {
                    Class tokenClass = Class.forName(className);
                    tokenProvider = (TokenProvider) tokenClass.newInstance();
                } catch (Exception ex) {
                    MessageLog.debug(this, "Failed to get configured token provider " + className + " , using default. ", ex);
                }
            }
            if (tokenProvider == null) {
                tokenProvider = new ClassInformation();
            }
        }
        return tokenProvider;
    }

    /**
	 *  Load in the templates from the specified file name. This will look for a
	 *  local file. If no file is found, it will look for the file as a resource.
	 *
	 * @param  fileName
	 * @exception  Exception
	 */
    public void init(String fileName) throws Exception {
        if (xmlInitialized) {
            return;
        }
        templates = null;
        if (new File(fileName).exists()) {
            try {
                MessageLog.debug(this, "Reading templates from file " + fileName);
                templates = KnownTemplates.readFile(fileName);
            } catch (Throwable t) {
                System.err.println(t);
            }
        }
        if (templates == null) {
            try {
                MessageLog.debug(this, "Reading templates " + fileName + "  as resource.");
                InputStream str = this.getClass().getResourceAsStream(fileName);
                templates = KnownTemplates.readStream(str);
            } catch (Exception e) {
                MessageLog.debug(this, "Failed to get resource as stream. " + e);
                throw e;
            }
        }
        KnownTemplates.setDefaultInstance(templates);
        setInitialized(true);
    }

    /**
	 *  Generate the templates into the body or header of the target class.
	 *
	 * @param  templateName java.lang.String
	 * @param  targetPackageName
	 * @param  targetClassName
	 * @param  fileName
	 * @exception  FileNotFoundException
	 * @exception  IOException
	 * @exception  Exception
	 * @author  Dave Hoag
	 */
    public void generateTemplates(MergeTemplate[] templateName, String targetPackageName, String targetClassName, String fileName) throws FileNotFoundException, IOException, Exception {
        Reader javaSource = getReader(fileName, targetPackageName);
        SourceCodeReader rdr = new SourceCodeReader(javaSource);
        ClassElement[] els = rdr.parseSource();
        JavaClassDef def = getTargetDef(els, targetClassName);
        if (System.getProperty("header") == null) {
            JavaClassDef sourceDef = new JavaClassDef(els);
            ClassBodyGenerator generator = new ClassBodyGenerator(def.getMainClass());
            generator.setMiscData(getMiscData());
            ClassSpec spec = sourceDef.getMainClass().getClassSpec();
            if (System.getProperty("useRealClassName") != null) {
                spec.setClassName(targetClassName);
            }
            for (int i = 0; i < templateName.length; i++) {
                final boolean overwrite = true;
                MessageLog.debug(this, "Generating template " + templateName[i]);
                generator.generateTemplate(templateName[i], spec, overwrite);
            }
        } else {
            ClassHeaderGenerator generator = new ClassHeaderGenerator(def);
            generator.setMiscData(getMiscData());
            for (int i = 0; i < templateName.length; i++) {
                MessageLog.debug(this, "Generating header template " + templateName[i]);
                generator.generateTemplate(templateName[i]);
            }
        }
        ClassElement[] elements = def.getOriginalElements();
        System.out.println("Writing file " + def.getMainClass().getClassName() + ".java");
        SourceCodeGenerator.writeClassElements(elements, def.getMainClass().getClassName() + ".java");
    }

    /**
	 * @param  templateName
	 * @param  targetClassName
	 * @exception  FileNotFoundException
	 * @exception  IOException
	 */
    public void generateTemplates(MergeTemplate[] templateName, String targetClassName) throws FileNotFoundException, IOException {
        ClassElement[] els = new ClassElement[0];
        JavaClassDef def = getTargetDef(els, targetClassName);
        if (System.getProperty("header") == null) {
            ClassBodyGenerator generator = new ClassBodyGenerator(def.getMainClass());
            generator.setMiscData(getMiscData());
            for (int i = 0; i < templateName.length; i++) {
                final boolean overwrite = true;
                generator.generateTemplate(templateName[i], overwrite);
            }
        } else {
            ClassHeaderGenerator generator = new ClassHeaderGenerator(def);
            generator.setMiscData(getMiscData());
            for (int i = 0; i < templateName.length; i++) {
                try {
                    generator.generateTemplate(templateName[i]);
                } catch (IOException ex) {
                    System.out.println(ex);
                }
            }
        }
        ClassElement[] elements = def.getOriginalElements();
        if (System.getProperty("directPath") == null) {
            SourceCodeGenerator.writeClassElements(elements, def.getMainClass().getClassName() + ".java");
        } else {
            SourceCodeGenerator.writeClassElements(elements, targetClassName);
        }
    }

    /**
	 * @param  xmlFileName
	 * @exception  java.net.MalformedURLException
	 * @exception  ConfigurationException
	 * @exception  IOException
	 */
    public void initFromXml(final String xmlFileName) throws java.net.MalformedURLException, ConfigurationException, IOException {
        if (xmlInitialized) {
            return;
        }
        templates = null;
        MergeTemplateWriter.setTokenList(getTokenProvider().getKnownTokens());
        java.net.URL url = new FileFinder().getUrl(getTokenProvider().getClass(), xmlFileName);
        InputStreamReader xmlFileReader = new InputStreamReader(url.openStream());
        KnownTemplates temps = MergeTemplateWriter.importFromXML(xmlFileReader);
        xmlFileReader.close();
        KnownTemplates.setDefaultInstance(temps);
        setTemplates(temps);
        setInitialized(true);
    }

    /**
	 * @param  file
	 * @return
	 * @exception  IOException
	 * @exception  Exception
	 */
    protected Reader createReaderFromClassFile(final File file) throws IOException, Exception {
        FileInputStream fi = new FileInputStream(file);
        ClassFile cf = new ClassFile();
        cf.debug = false;
        cf.dumpConstants = false;
        if (!cf.read(fi)) {
            throw new IOException("Unable to read class file: " + file.getName());
        }
        ByteArrayOutputStream stream = new ByteArrayOutputStream();
        cf.display(new PrintStream(stream));
        return new StringReader(stream.toString());
    }
}
