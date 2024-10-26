package org.xmlcml.cml.schemagen;

import java.io.BufferedReader;
import java.io.FileOutputStream;
import java.io.FileWriter;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.Writer;
import java.net.URL;
import java.util.List;
import nu.xom.Element;
import nu.xom.Elements;
import org.apache.commons.cli.BasicParser;
import org.apache.commons.cli.CommandLine;
import org.apache.commons.cli.CommandLineParser;
import org.apache.commons.cli.HelpFormatter;
import org.apache.commons.cli.Option;
import org.apache.commons.cli.OptionBuilder;
import org.apache.commons.cli.Options;
import org.xmlcml.cml.base.AttributeFactory;
import org.xmlcml.cml.base.AttributeGenerator;
import org.xmlcml.cml.base.CMLAttribute;
import org.xmlcml.cml.base.CMLElementType;
import org.xmlcml.cml.base.CMLRuntimeException;
import org.xmlcml.cml.base.CMLType;
import org.xmlcml.cml.base.CMLUtil;
import org.xmlcml.cml.base.ElementGenerator;
import org.xmlcml.cml.base.SchemaManager;
import org.xmlcml.cml.base.TypeGenerator;
import org.xmlcml.euclid.Util;

/**
 * @author pm286
 *
 */
public class Schemagen implements SchemagenConstants {

    private SchemaManager schemaManager;

    private AttributeGenerator attributeGenerator;

    private ElementGenerator elementGenerator;

    private TypeGenerator typeGenerator;

    private Element schema;

    private String schemaDir;

    private String codeOutDir;

    private String schemaFile = DEFAULT_SCHEMAFILE;

    private String typeDir = DEFAULT_TYPEDIR;

    private String elementDir = DEFAULT_ELEMENTDIR;

    private String attributeDir = DEFAULT_ATTRIBUTEDIR;

    private String elementName;

    private String attributeName;

    @SuppressWarnings("unused")
    private String attributeGroupName;

    private String javaType;

    private String javaGetMethod;

    private String attClassName;

    private String nullAttributeActionString;

    private String summary;

    private String description;

    private CMLAttribute attribute;

    private CMLType type;

    private String attributeVariable;

    /** copy constructor.
	 * @param schemaManager
	 */
    public Schemagen(SchemaManager schemaManager) {
        this.schemaManager = schemaManager;
        init();
    }

    private void init() {
        this.attributeGenerator = schemaManager.getAttributeGenerator();
        this.elementGenerator = schemaManager.getElementGenerator();
        this.typeGenerator = schemaManager.getTypeGenerator();
        schemaFile = Schemagen.DEFAULT_SCHEMAFILE;
        schemaFile = DEFAULT_SCHEMAFILE;
        typeDir = DEFAULT_TYPEDIR;
        elementDir = DEFAULT_ELEMENTDIR;
        attributeDir = DEFAULT_ATTRIBUTEDIR;
    }

    /** called from subclasses.
     * recurses through directories by instantiating new converters
     * @param args
     * @throws Exception
     */
    public void runCommands(String[] args) throws Exception {
        CommandLineParser parser = new BasicParser();
        Options options = getCommandLineOptions();
        CommandLine line = parser.parse(options, args);
        parseCommandLineOptions(line, options);
    }

    /** process arguments.
	 * manages infile, outfile, dict, dir, updatedict
	 * devolves code-specific args to processExtraArgs()
	 * @param args
	 */
    protected void parseCommandLineOptions(CommandLine line, Options options) throws Exception {
        if (line.hasOption(HELP_OPT_NAME) || line.getOptions().length == 0) {
            HelpFormatter formatter = new HelpFormatter();
            formatter.printHelp("schemagen", options);
        } else {
            if (line.hasOption(SCHEMADIR_OPT_NAME)) {
                this.schemaDir = line.getOptionValue(SCHEMADIR_OPT_NAME);
            } else {
                throw new CMLRuntimeException("Must have a schema directory");
            }
            if (line.hasOption(OUTDIR_OPT_NAME)) {
                schemaManager.setOutdir(line.getOptionValue(OUTDIR_OPT_NAME));
            }
            typeGenerator.readAssembleAndIndexSchema(schemaDir + F_S + typeDir);
            attributeGenerator.readAssembleAndIndexSchema(schemaDir + F_S + attributeDir);
            elementGenerator.readAssembleAndIndexElementSchema(schemaDir + F_S + elementDir);
            this.assembleCompleteSchema();
            if (schemaManager.getOutdir() != null) {
                outputSchemas();
            }
            if (line.hasOption(CODE_OUTDIR_OPT_NAME)) {
                this.codeOutDir = line.getOptionValue(CODE_OUTDIR_OPT_NAME);
                writeJavaForElements();
            }
        }
    }

    @SuppressWarnings("static-access")
    protected Options getCommandLineOptions() {
        Options options = new Options();
        Option helpOpt = new Option(HELP_OPT_NAME, "list all options");
        options.addOption(helpOpt);
        Option schemaDirOpt = OptionBuilder.withArgName(SCHEMADIR_ARG).hasArg().withDescription("directory with schema components").create(SCHEMADIR_OPT_NAME);
        options.addOption(schemaDirOpt);
        Option typeDirOpt = OptionBuilder.withArgName(TYPEDIR_ARG).hasArg().withDescription("directory with type components (relative to schema);" + " default 'types' ").create(TYPEDIR_OPT_NAME);
        options.addOption(typeDirOpt);
        Option attributeDirOpt = OptionBuilder.withArgName(ATTRIBUTEDIR_ARG).hasArg().withDescription("directory with attribute components (relative to schema);" + " default 'attributes' ").create(ATTRIBUTEDIR_OPT_NAME);
        options.addOption(attributeDirOpt);
        Option elementDirOpt = OptionBuilder.withArgName(ELEMENTDIR_ARG).hasArg().withDescription("directory with element components (relative to schema);" + " default 'elements' ").create(ELEMENTDIR_OPT_NAME);
        options.addOption(elementDirOpt);
        Option schemaFileOpt = OptionBuilder.withArgName(SCHEMAFILE_ARG).hasArg().withDescription("output schema filename (default schema.xsd)").create(SCHEMAFILE_OPT_NAME);
        options.addOption(schemaFileOpt);
        Option outdirOpt = OptionBuilder.withArgName(OUTDIR_ARG).hasArg().withDescription("output directory for schemas").create(OUTDIR_OPT_NAME);
        options.addOption(outdirOpt);
        Option codeOutdirOpt = OptionBuilder.withArgName(CODE_OUTDIR_ARG).hasArg().withDescription("output directory for code").create(CODE_OUTDIR_OPT_NAME);
        options.addOption(codeOutdirOpt);
        return options;
    }

    private void assembleCompleteSchema() throws Exception {
        schema = new Element("xsd:schema", XSD_NS);
        Elements elements = elementGenerator.getSchema().getChildElements();
        for (int i = 0; i < elements.size(); i++) {
            Element copy = (Element) elements.get(i).copy();
            schema.appendChild(copy);
        }
        Elements attributes = attributeGenerator.getSchema().getChildElements();
        for (int i = 0; i < attributes.size(); i++) {
            Element copy = (Element) attributes.get(i).copy();
            schema.appendChild(copy);
        }
        Elements types = typeGenerator.getSchema().getChildElements();
        for (int i = 0; i < types.size(); i++) {
            Element copy = (Element) types.get(i).copy();
            schema.appendChild(copy);
        }
    }

    void outputSchemas() throws IOException {
        String typeFile = schemaManager.getOutdir() + F_S + TYPES_XSD;
        FileOutputStream fos = new FileOutputStream(typeFile);
        CMLUtil.debug(typeGenerator.getSchema(), fos, 1);
        fos.close();
        System.out.println("Wrote type schema " + typeFile);
        String attributeFile = schemaManager.getOutdir() + F_S + ATTRIBUTES_XSD;
        fos = new FileOutputStream(attributeFile);
        CMLUtil.debug(attributeGenerator.getSchema(), fos, 1);
        fos.close();
        System.out.println("Wrote attribute schema " + attributeFile);
        String elementFile = schemaManager.getOutdir() + F_S + ELEMENTS_XSD;
        fos = new FileOutputStream(elementFile);
        CMLUtil.debug(elementGenerator.getSchema(), fos, 1);
        fos.close();
        System.out.println("Wrote element schema " + elementFile);
        String schemaFileX = schemaManager.getOutdir() + F_S + schemaFile;
        fos = new FileOutputStream(schemaFileX);
        CMLUtil.debug(schema, fos, 1);
        fos.close();
        System.out.println("Wrote element schema " + schemaFileX);
    }

    /** write java for elements.
	 * @throws Exception 
	 */
    public void writeJavaForElements() throws Exception {
        System.out.println("============ CODE GENERATION ==============");
        for (String name : elementGenerator.getNameList()) {
            writeJavaForElement(name);
        }
    }

    /** writes java class file for an element.
	 * generates filename (AbstractFooBar.java) from tagName into schemagen.outdir
	 * @param name tagName
	 * @throws Exception
	 */
    void writeJavaForElement(String name) throws Exception {
        CMLElementType elementType = elementGenerator.getElementTypeMap().get(name);
        String filename = this.codeOutDir + F_S + CMLUtil.makeAbstractName(name) + ".java";
        FileWriter w = new FileWriter(filename);
        writeJavaForElement(w, elementType);
        w.close();
        System.out.println("Wrote " + filename);
    }

    /** writes java class for elementClass.
	 * 
	 * @param w writer
	 * @param elementType
	 * @throws IOException
	 */
    void writeJavaForElement(Writer w, CMLElementType elementType) throws IOException {
        String elementName = elementType.getName();
        writePrepreparedText(ELEMENT1, w);
        w.write("// end of part 1\n");
        writeHeaderDocumentation(w, elementType);
        writeClassHeader(w, elementName);
        writeTag(w, elementName);
        writeConstructor(w, elementName);
        List<CMLAttribute> attributeList = elementType.getAttributeList();
        for (CMLAttribute attribute : attributeList) {
            w.write("// attribute:   " + attribute.getLocalName() + "\n\n");
            writeAttributeAccessors(w, attribute, elementName);
        }
        for (CMLElementType childElementType : elementType.getElementTypeList()) {
            w.write("// element:   " + childElementType.getName() + "\n\n");
            writeElementAccessors(w, childElementType);
        }
        Element extension = elementType.getExtension();
        if (extension != null) {
            writeTextContentAccessors(w, elementType);
        }
        if (attributeList.size() > 0) {
            writeAddAttribute(w, attributeList);
        }
        w.write("}\n");
    }

    private void writeHeaderDocumentation(Writer w, CMLElementType elementType) throws IOException {
        w.write("/** CLASS DOCUMENTATION */\n");
    }

    private void writeClassHeader(Writer w, String name) throws IOException {
        w.write("" + "public abstract class " + CMLUtil.makeAbstractName(name) + " extends CMLElement {\n" + "");
    }

    /** write tag.
	 * 
	 * @param w
	 * @param name
	 * @throws IOException
	 */
    public void writeTag(Writer w, String name) throws IOException {
        w.write("" + "    /** local name*/\n" + "    public final static String TAG = \"" + name + "\";\n");
    }

    /** write constructor.
	 * 
	 * @param w
	 * @param name
	 * @throws IOException
	 */
    public void writeConstructor(Writer w, String name) throws IOException {
        String abstractName = CMLUtil.makeAbstractName(name);
        w.write("" + "    /** constructor. */" + "    public " + abstractName + "() {\n" + "        super(\"" + name + "\");\n" + "    }\n" + "/** copy constructor.\n" + "* deep copy using XOM copy()\n" + "* @param old element to copy\n" + "*/\n" + "    public " + abstractName + "(" + abstractName + " old) {\n" + "        super((CMLElement) old);\n" + "    }\n" + "" + "");
    }

    /** write attribute code
	 * 
	 * @param w
	 * @param attribute
	 * @param elementName
	 * @throws IOException
	 */
    public void writeAttributeAccessors(Writer w, CMLAttribute attribute, String elementName) throws IOException {
        this.elementName = elementName;
        attributeGroupName = attribute.getAttributeGroupName();
        attributeName = attribute.getLocalName();
        attClassName = attribute.getJavaShortClassName();
        CMLAttribute specialAttribute = AttributeFactory.createSpecialAttribute(attributeName);
        if (specialAttribute != null) {
            attribute = specialAttribute;
            attClassName = attribute.getClass().getSimpleName();
        }
        javaType = attribute.getJavaType();
        javaGetMethod = attribute.getJavaGetMethod();
        attributeVariable = "_att_" + attributeName.toLowerCase();
        nullAttributeActionString = "return null";
        if (javaType.equals(JAVA_BOOL)) {
            nullAttributeActionString = "throw new CMLRuntimeException(\"boolean attribute is unset: " + attributeName + "\")";
        } else if (javaType.equals(JAVA_INT)) {
            nullAttributeActionString = "throw new CMLRuntimeException(\"int attribute is unset: " + attributeName + "\")";
        } else if (javaType.equals(JAVA_DOUB)) {
            nullAttributeActionString = "return Double.NaN";
        }
        summary = attribute.getSummary();
        description = attribute.getDescription();
        String sgetName = CMLUtil.capitalize(attributeName);
        if (sgetName.equals("Value")) {
            sgetName = "CMLValue";
        }
        writeFooAttributeDeclaration(w, sgetName);
        writeGetFooAttribute(w, sgetName);
        writeGetFoo(w, sgetName);
        writeSetFooString(w, sgetName);
        if (!javaType.equals(JAVA_STRING)) {
            writeSetFoo(w, sgetName);
        }
    }

    /** write attribute writer.
	 * 
	 * @param w
	 * @param setName
	 * @throws IOException
	 */
    public void writeFooAttributeDeclaration(Writer w, String setName) throws IOException {
        w.write("" + "    /** cache */\n" + "    " + attClassName + " " + attributeVariable + " = null;\n" + "");
    }

    /** write attribute writer.
	 * 
	 * @param w
	 * @param setName
	 * @throws IOException
	 */
    public void writeSetFooString(Writer w, String setName) throws IOException {
        w.write("" + "    /** " + summary + "\n" + getFormattedDescription(description) + "    * @param value title value\n" + "    * @throws CMLRuntimeException attribute wrong value/type\n" + "    */\n" + "    public void set" + setName + "(String value) throws CMLRuntimeException {\n" + "        " + attClassName + " att = null;\n" + "        if (" + attributeVariable + " == null) {\n" + "            " + attributeVariable + " = (" + attClassName + ") " + "attributeFactory.getAttribute(" + "\"" + attributeName + "\", \"" + elementName + "\");\n" + "            if (" + attributeVariable + " == null) {\n" + "                throw new CMLRuntimeException(\"BUG: cannot process attributeGroupName : " + attributeName + " probably incompatible attributeGroupName and attributeName\");\n" + "            }\n" + "        }\n" + "        att = new " + attClassName + "(" + attributeVariable + ");\n" + "        super.addRemove(att, value);\n" + "    }\n" + "");
    }

    /** write attribute writer.
	 * 
	 * @param w
	 * @param setName
	 * @throws IOException
	 */
    public void writeSetFoo(Writer w, String setName) throws IOException {
        w.write("" + "    /** " + summary + "\n" + getFormattedDescription(description) + "    * @param value title value\n" + "    * @throws CMLRuntimeException attribute wrong value/type\n" + "    */\n" + "    public void set" + setName + "(" + javaType + " value) throws CMLRuntimeException {\n" + "        if (" + attributeVariable + " == null) {\n" + "            " + attributeVariable + " = (" + attClassName + ") " + "attributeFactory.getAttribute(" + "\"" + attributeName + "\", \"" + elementName + "\");\n" + "           if (" + attributeVariable + " == null) {\n" + "               throw new CMLRuntimeException(\"BUG: cannot process attributeGroupName : " + attributeName + " probably incompatible attributeGroupName and attributeName \");\n" + "            }\n" + "        }\n" + "        " + attClassName + " att = new " + attClassName + "(" + attributeVariable + ");\n" + "        super.addAttribute(att);\n" + "        att.setCMLValue(value);\n" + "    }\n" + "");
    }

    /** write get attribute
	 * @param w
	 * @param getName
	 * @throws IOException
	 */
    public void writeGetFooAttribute(Writer w, String getName) throws IOException {
        w.write("" + "    /** " + summary + "\n" + getFormattedDescription(description) + "    * @return CMLAttribute\n" + "    */\n" + "    public CMLAttribute get" + getName + "Attribute() {\n" + "        return (CMLAttribute) getAttribute(\"" + attributeName + "\");\n" + "    }\n" + "");
    }

    /** write get attribute
	 * @param w
	 * @param getName
	 * @throws IOException
	 */
    public void writeGetFoo(Writer w, String getName) throws IOException {
        w.write("" + "    /** " + summary + "\n" + getFormattedDescription(description) + "    * @return " + javaType + "\n" + "    */\n" + "    public " + javaType + " get" + getName + "() {\n" + "        " + attClassName + " att = (" + attClassName + ") this.get" + getName + "Attribute();\n" + "        if (att == null) {\n" + "            " + nullAttributeActionString + ";\n" + "        }\n" + "        return att." + javaGetMethod + "();\n" + "    }\n" + "");
    }

    /** write get attribute
	 * @param w
	 * @param elementType
	 * @throws IOException
	 */
    public void writeTextContentAccessors(Writer w, CMLElementType elementType) throws IOException {
        attributeName = CMLXSD_XMLCONTENT;
        attributeVariable = CMLXSD_XMLCONTENT;
        type = elementType.getSimpleContentType();
        attribute = AttributeFactory.createCMLAttribute(attributeName, type);
        javaType = attribute.getJavaType();
        javaGetMethod = attribute.getJavaGetMethod();
        attClassName = attribute.getJavaShortClassName();
        nullAttributeActionString = "return null";
        if (javaType.equals("int")) {
            nullAttributeActionString = "throw new CMLRuntimeException(\"int attribute is unset: " + attributeName + "\")";
        } else if (javaType.equals("double")) {
            nullAttributeActionString = "return Double.NaN";
        }
        summary = type.getSummary();
        description = type.getDescription();
        writeXMLContentDeclaration(w, "");
        writeGetXMLContent(w, "");
        writeSetXMLContentString(w, CMLXSD_XMLCONTENT);
        if (!javaType.equals(JAVA_STRING)) {
            writeSetXMLContent(w, CMLXSD_XMLCONTENT);
        }
    }

    /** write writeXML
	 * @param w
	 * @param setName
	 * @throws IOException
	 */
    public void writeXMLContentDeclaration(Writer w, String setName) throws IOException {
        w.write("" + "    " + attClassName + " " + attributeVariable + ";\n" + "");
    }

    /** write set content
	 * @param w
	 * @param setName
	 * @throws IOException
	 */
    public void writeSetXMLContentString(Writer w, String setName) throws IOException {
        w.write("" + "    /** " + summary + "\n" + getFormattedDescription(description) + "    * @param value title value\n" + "    * @throws CMLRuntimeException attribute wrong value/type\n" + "    */\n" + "    public void setXMLContent(String value) throws CMLRuntimeException {\n" + "        if (" + attributeVariable + " == null) {\n" + "            " + attributeVariable + " = new " + attClassName + "(\"" + CMLXSD_XMLCONTENT + "\");\n" + "        }\n" + "        " + attributeVariable + ".setCMLValue(value);\n" + "        String attval = " + attributeVariable + ".getValue();\n" + "        this.removeChildren();\n" + "        this.appendChild(attval);\n" + "    }\n" + "");
    }

    /** set xml content.
	 * 
	 * @param w
	 * @param setName
	 * @throws IOException
	 */
    public void writeSetXMLContent(Writer w, String setName) throws IOException {
        w.write("" + "    /** " + summary + "\n" + getFormattedDescription(description) + "    * @param value title value\n" + "    * @throws CMLRuntimeException attribute wrong value/type\n" + "    */\n" + "    public void setXMLContent(" + javaType + " value) throws CMLRuntimeException {\n" + "        if (" + attributeVariable + " == null) {\n" + "            " + attributeVariable + " = new " + attClassName + "(\"" + CMLXSD_XMLCONTENT + "\");\n" + "        }\n" + "        " + attributeVariable + ".setCMLValue(value);\n" + "        String attval = (String)" + attributeVariable + ".getValue();\n" + "        this.removeChildren();\n" + "        this.appendChild(attval);\n" + "    }\n" + "");
    }

    /** set xml content.
	 * 
	 * @param w
	 * @param getName
	 * @throws IOException
	 */
    public void writeGetXMLContent(Writer w, String getName) throws IOException {
        w.write("" + "    /** " + summary + "\n" + getFormattedDescription(description) + "    * @return " + javaType + "\n" + "    */\n" + "    public " + javaType + " getXMLContent() {\n" + "        String content = this.getValue();\n" + "        if (" + attributeVariable + " == null) {\n" + "            " + attributeVariable + " = new " + attClassName + "(\"" + CMLXSD_XMLCONTENT + "\");\n" + "        }\n" + "        " + attributeVariable + ".setCMLValue(content);\n" + "        return " + attributeVariable + "." + javaGetMethod + "();\n" + "    }\n" + "");
    }

    /** element accessors.
	 * @param w
	 * @param elementType
	 * @throws IOException
	 */
    public void writeElementAccessors(Writer w, CMLElementType elementType) throws IOException {
        writeAddFoo(w, elementType);
        writeGetFooElements(w, elementType);
    }

    /** write get foo.
	 * 
	 * @param w
	 * @param elementType
	 * @throws IOException
	 */
    public void writeGetFooElements(Writer w, CMLElementType elementType) throws IOException {
        String name = elementType.getName();
        String cmlName = CMLUtil.makeCMLName(name);
        String capName = CMLUtil.capitalize(name);
        w.write("" + "    /** " + summary + "\n" + getFormattedDescription(description) + "    * @return CMLElements<" + cmlName + ">\n" + "    */\n" + "    public CMLElements<" + cmlName + "> get" + capName + "Elements() {\n" + "        Elements elements = this.getChildElements(\"" + name + "\", CML_NS);\n" + "        return new CMLElements<" + cmlName + ">(elements);\n" + "    }\n" + "");
    }

    /** write add element.
	 * 
	 * @param w
	 * @param elementType
	 * @throws IOException
	 */
    public void writeAddFoo(Writer w, CMLElementType elementType) throws IOException {
        String name = elementType.getName();
        w.write("" + "    /** " + summary + "\n" + getFormattedDescription(description) + "    * @param " + name + " child to add\n" + "    */\n" + "    public void add" + CMLUtil.capitalize(name) + "(" + CMLUtil.makeAbstractName(name) + " " + name + ") {\n" + "        " + name + ".detach();\n" + "        this.appendChild(" + name + ");\n" + "    }\n" + "");
    }

    /** add attribute.
	 * @param w
	 * @param attributeList
	 * @throws IOException 
	 */
    public void writeAddAttribute(Writer w, List<CMLAttribute> attributeList) throws IOException {
        w.write("" + "    /** overrides addAttribute(Attribute)\n" + "     * reroutes calls to setFoo()\n" + "     * @param att  attribute\n" + "    */\n" + "    public void addAttribute(Attribute att) {\n" + "        String name = att.getLocalName();\n" + "        String value = att.getValue();\n" + "        if (name == null) {\n" + "");
        for (CMLAttribute attribute : attributeList) {
            String attName = attribute.getLocalName();
            String prefix = (attName.equals("value")) ? "CML" : S_EMPTY;
            w.write("" + "        } else if (name.equals(\"" + attName + "\")) {\n" + "            set" + prefix + CMLUtil.capitalize(attName) + "(value);\n");
        }
        w.write("" + "	     } else {\n" + "            super.addAttribute(att);\n" + "        }\n" + "    }\n" + "");
    }

    private static void writePrepreparedText(String filename, Writer w) throws IOException {
        URL url = Util.getResource(SCHEMAGEN_DIR + filename);
        BufferedReader bReader = new BufferedReader(new InputStreamReader(url.openStream()));
        while (true) {
            String line = bReader.readLine();
            if (line == null) {
                break;
            }
            w.write(line + "\n");
        }
        bReader.close();
    }

    private static String getFormattedDescription(String description) throws IOException {
        String s = S_EMPTY;
        if (description != null) {
            String[] ss = description.split("\\n");
            for (String sss : ss) {
                s += "    * " + sss + "\n";
            }
        }
        return s;
    }

    /**
	 * @param args
	 */
    public static void main(String[] args) {
        SchemaManager schemaManager = new SchemaManager();
        Schemagen g = new Schemagen(schemaManager);
        try {
            g.runCommands(args);
        } catch (Exception e) {
            e.printStackTrace();
            System.err.println("EXCEPTION in XMLConverter " + e);
        }
    }
}
