package com.wrupple.muba.widget.rebind;

import java.io.IOException;
import java.io.InputStream;
import java.io.PrintWriter;
import java.net.URL;
import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.ParserConfigurationException;
import org.w3c.dom.Document;
import org.w3c.dom.NamedNodeMap;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;
import org.xml.sax.InputSource;
import org.xml.sax.SAXException;
import org.xml.sax.SAXParseException;
import com.google.gwt.core.ext.GeneratorContext;
import com.google.gwt.core.ext.TreeLogger;
import com.google.gwt.core.ext.UnableToCompleteException;
import com.google.gwt.core.ext.typeinfo.JClassType;
import com.google.gwt.core.ext.typeinfo.NotFoundException;
import com.google.gwt.core.ext.typeinfo.TypeOracle;
import com.google.gwt.uibinder.rebind.MortalLogger;
import com.google.gwt.uibinder.rebind.UiBinderGenerator;
import com.google.gwt.user.rebind.ClassSourceFileComposerFactory;
import com.google.gwt.user.rebind.SourceWriter;
import com.wrupple.muba.widget.client.control.binder.WidgetTemplate;

public class WidgetterBinder {

    private static final String TEMPLATE_SUFFIX = ".widgets.xml";

    private TypeOracle typeOracle;

    private String typeName;

    private GeneratorContext context;

    private TreeLogger logger;

    private MortalLogger mlogger;

    public WidgetterBinder(TreeLogger logger, GeneratorContext context, String typeName) {
        this.logger = logger;
        this.context = context;
        this.typeName = typeName;
        this.typeOracle = context.getTypeOracle();
        this.mlogger = new MortalLogger(logger);
    }

    public String createWidgetter() {
        try {
            JClassType classType = typeOracle.getType(typeName);
            String templateFile = null;
            try {
                templateFile = deduceTemplateFile(mlogger, classType);
            } catch (UnableToCompleteException e) {
                logger.log(null, "error while trying to deduce the xml Template file", e);
            }
            Document doc = null;
            try {
                doc = parseXmlResource(templateFile);
            } catch (SAXParseException e) {
                e.printStackTrace();
            } catch (UnableToCompleteException e) {
                e.printStackTrace();
            }
            Node widgetterNode = doc.getElementsByTagName("widgetter").item(0);
            String genericType = getGenericType(widgetterNode);
            String factoryType = widgetterNode.getAttributes().getNamedItem("factory").getFirstChild().getNodeValue();
            String serviceBusGetter = widgetterNode.getAttributes().getNamedItem("serviceBus").getFirstChild().getNodeValue();
            String serviceBusKey = widgetterNode.getAttributes().getNamedItem("key").getFirstChild().getNodeValue();
            String factoryInstanceName = "factory";
            SourceWriter source = getSourceWriter(classType, genericType);
            if (source == null) {
                return classType.getParameterizedQualifiedSourceName() + "Impl";
            } else {
                NodeList widgets = doc.getElementsByTagName("widget");
                source.indent();
                source.println("HashMap<String," + genericType + "> map;");
                source.println("public void init(){");
                source.indent();
                source.println("map = new HashMap<String," + genericType + ">(" + widgets.getLength() + ");");
                source.println(factoryType);
                source.println(" ");
                source.println(factoryInstanceName);
                source.println(" = ");
                source.println(serviceBusGetter);
                source.println(".getAndCast(\"" + serviceBusKey + "\");");
                Node node;
                NamedNodeMap att;
                for (int i = 0; i < widgets.getLength(); i++) {
                    node = widgets.item(i);
                    att = node.getAttributes();
                    source.print("map.put(\"");
                    source.print(att.getNamedItem("id").getFirstChild().getNodeValue());
                    source.print("\",");
                    source.print(factoryInstanceName);
                    source.print(".");
                    source.print(att.getNamedItem("creator").getFirstChild().getNodeValue());
                    source.print("());");
                }
                source.outdent();
                source.println();
                source.println("}");
                logger.log(TreeLogger.TRACE, "created widgetter with signature " + genericType);
                source.println("public void add(String id, " + genericType + " widgetCreator) {");
                source.indent();
                checkInitialization(source);
                source.println("map.put(id, widgetCreator);");
                source.outdent();
                source.println("}");
                source.println("public " + genericType + " get(String id) {");
                source.indent();
                checkInitialization(source);
                source.println("return  map.get(id);");
                source.outdent();
                source.println("}");
                source.println();
                source.outdent();
                source.commit(logger);
                return classType.getParameterizedQualifiedSourceName() + "Impl";
            }
        } catch (NotFoundException e) {
            e.printStackTrace();
            return null;
        }
    }

    private void checkInitialization(SourceWriter source) {
        source.println("if(map == null){");
        source.println("init();");
        source.println("}");
    }

    /**
	 * SourceWriter instantiation. Return null if the resource already exist.
	 * 
	 * @param genericType
	 * 
	 * @return sourceWriter
	 */
    public SourceWriter getSourceWriter(JClassType classType, String genericType) {
        String packageName = classType.getPackage().getName();
        String simpleName = classType.getSimpleSourceName() + "Impl";
        ClassSourceFileComposerFactory composer = new ClassSourceFileComposerFactory(packageName, simpleName);
        composer.addImport("java.util.HashMap");
        composer.addImport("com.wrupple.muba.widget.client.control.binder.Widgetter");
        composer.addImplementedInterface(classType.getSimpleSourceName());
        PrintWriter printWriter = context.tryCreate(logger, packageName, simpleName);
        if (printWriter == null) {
            return null;
        } else {
            SourceWriter sw = composer.createSourceWriter(context, printWriter);
            return sw;
        }
    }

    private static String deduceTemplateFile(MortalLogger logger, JClassType interfaceType) throws UnableToCompleteException {
        String templateName = null;
        WidgetTemplate annotation = interfaceType.getAnnotation(WidgetTemplate.class);
        if (annotation == null) {
            if (interfaceType.getEnclosingType() != null) {
                interfaceType = interfaceType.getEnclosingType();
            }
            return slashify(interfaceType.getQualifiedSourceName()) + TEMPLATE_SUFFIX;
        } else {
            templateName = annotation.value();
            if (!templateName.endsWith(TEMPLATE_SUFFIX)) {
                logger.die("Widget Template file name must end with " + TEMPLATE_SUFFIX);
            }
            String unsuffixed = templateName.substring(0, templateName.lastIndexOf(TEMPLATE_SUFFIX));
            if (!unsuffixed.contains(".")) {
                templateName = slashify(interfaceType.getPackage().getName()) + "/" + templateName;
            } else {
                templateName = slashify(unsuffixed) + TEMPLATE_SUFFIX;
            }
        }
        return templateName;
    }

    private Document parseXmlResource(final String resourcePath) throws SAXParseException, UnableToCompleteException {
        DocumentBuilderFactory factory = DocumentBuilderFactory.newInstance();
        factory.setNamespaceAware(true);
        factory.setExpandEntityReferences(true);
        DocumentBuilder builder;
        try {
            builder = factory.newDocumentBuilder();
        } catch (ParserConfigurationException e) {
            throw new RuntimeException(e);
        }
        try {
            ClassLoader classLoader = UiBinderGenerator.class.getClassLoader();
            URL url = classLoader.getResource(resourcePath);
            if (null == url) {
                die("Unable to find resource: " + resourcePath);
            }
            InputStream stream = url.openStream();
            InputSource input = new InputSource(stream);
            input.setSystemId(url.toExternalForm());
            return builder.parse(input);
        } catch (SAXParseException e) {
            throw e;
        } catch (SAXException e) {
            throw new RuntimeException(e);
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

    public void die(String message) throws UnableToCompleteException {
        mlogger.die(message, (Object) null);
    }

    private static String slashify(String s) {
        return s.replace(".", "/");
    }

    private String getGenericType(Node node) throws NullPointerException {
        if (node.hasAttributes()) {
            node = node.getAttributes().getNamedItem("type");
            if (node == null) {
                throw new NullPointerException("Widgetter does not define a 'type' attribute in 'widgetter' tag");
            } else {
                return node.getFirstChild().getNodeValue();
            }
        } else {
            throw new NullPointerException("Widgetter does not define any attributes in 'widgetter' tag");
        }
    }
}
