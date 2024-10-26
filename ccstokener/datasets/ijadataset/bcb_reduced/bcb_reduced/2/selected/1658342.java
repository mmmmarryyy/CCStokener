package org.gwt.mosaic.application.rebind;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.PrintWriter;
import java.net.URL;
import java.util.ArrayList;
import java.util.List;
import org.gwt.beansbinding.core.client.AutoBinding;
import org.gwt.beansbinding.core.client.BeanProperty;
import org.gwt.beansbinding.core.client.Binding;
import org.gwt.beansbinding.core.client.Bindings;
import org.gwt.mosaic.actions.client.Action;
import org.gwt.mosaic.actions.client.ActionMap;
import org.gwt.mosaic.actions.client.CommandAction;
import org.gwt.mosaic.application.client.Application;
import org.gwt.mosaic.application.client.CmdAction;
import org.gwt.mosaic.application.client.ParsedElement;
import org.gwt.mosaic.application.client.ResourceConstants;
import org.gwt.mosaic.application.client.util.ApplicationFramework;
import org.gwt.mosaic.xul.client.ElementParser;
import org.gwt.mosaic.xul.client.XULParserService;
import org.gwt.mosaic.xul.client.XULParserServiceAsync;
import org.gwt.mosaic.xul.client.ui.Element;
import com.google.gwt.core.client.GWT;
import com.google.gwt.core.ext.Generator;
import com.google.gwt.core.ext.GeneratorContext;
import com.google.gwt.core.ext.TreeLogger;
import com.google.gwt.core.ext.UnableToCompleteException;
import com.google.gwt.core.ext.typeinfo.JClassType;
import com.google.gwt.core.ext.typeinfo.JMethod;
import com.google.gwt.core.ext.typeinfo.JParameter;
import com.google.gwt.core.ext.typeinfo.JType;
import com.google.gwt.core.ext.typeinfo.NotFoundException;
import com.google.gwt.core.ext.typeinfo.TypeOracle;
import com.google.gwt.user.client.Command;
import com.google.gwt.user.client.Window;
import com.google.gwt.user.client.rpc.AsyncCallback;
import com.google.gwt.user.rebind.ClassSourceFileComposerFactory;
import com.google.gwt.user.rebind.SourceWriter;

public class ElementResourceGenerator extends Generator {

    protected class ParsedElementDescriptor {

        /**
		 * This either holds the name of the file (for server-side parsing)
		 * or the contents of the file (for client-side parsing)
		 */
        public String file;

        public String method;

        public ParsedElement.Types type;

        public ParsedElementDescriptor(String method, String file, ParsedElement.Types type) {
            this.method = method;
            this.file = file;
            this.type = type;
        }
    }

    private List<ParsedElementDescriptor> parsedElementList = new ArrayList<ParsedElementDescriptor>();

    private TreeLogger logger;

    private GeneratorContext context;

    private String typename;

    @Override
    public String generate(TreeLogger logger, GeneratorContext context, String typeName) throws UnableToCompleteException {
        this.logger = logger;
        this.context = context;
        this.typename = typeName;
        JClassType classType = null;
        try {
            classType = context.getTypeOracle().getType(typeName);
        } catch (NotFoundException e) {
            logger.log(TreeLogger.ERROR, "Cannot find class " + typeName, e);
            throw new UnableToCompleteException();
        }
        examine(classType);
        if (parsedElementList.size() > 0) {
            return doGenerate(typeName);
        } else {
            throw new UnableToCompleteException();
        }
    }

    protected void examine(JClassType t) {
        logger.log(TreeLogger.INFO, "Examining " + t.getQualifiedSourceName());
        lookupForParsedElementAnnotations(t);
        logger.log(TreeLogger.INFO, t.getQualifiedSourceName() + " done");
    }

    /**
	 * Determine if the specified method actually holds an annotation regarding {@link ParsedElement}
	 * @param method
	 * 		The method inspected
	 * @return
	 * 		True if the method contains a {@link ParsedElement} annotation; false otherwise
	 * @see {@link ParsedElement}, {@link ResourceConstants}
	 */
    protected boolean isParsedElementMethod(JMethod method) {
        if (!method.isPublic() || !method.isAnnotationPresent(ParsedElement.class)) {
            return false;
        }
        ParsedElement annotation = method.getAnnotation(ParsedElement.class);
        JType returnType = method.getReturnType();
        JParameter[] parameters = method.getParameters();
        if (annotation.type().equals(ParsedElement.Types.SYNC) && returnType.getErasedType().getQualifiedSourceName().equals(Element.class.getName()) && parameters.length == 0) return true;
        if (annotation.type().equals(ParsedElement.Types.ASYNC) && returnType.getErasedType().getQualifiedSourceName().equals("void") && parameters.length == 1 && parameters[0].getType().getErasedType().getQualifiedSourceName().equals(AsyncCallback.class.getName())) return true;
        return false;
    }

    /**
	 * Following the inspection of a {@link JClasssType} instance, all its methods are
	 * inspected for {@link ParsedElement} valid methods. 
	 * @param type
	 * @return
	 */
    protected List<JMethod> getParsableElementMethods(JClassType type) {
        List<JMethod> list = new ArrayList<JMethod>();
        JMethod[] methods = type.getOverridableMethods();
        if (methods != null) {
            for (JMethod method : methods) {
                if (isParsedElementMethod(method)) {
                    list.add(method);
                }
            }
        }
        return list;
    }

    protected void lookupForParsedElementAnnotations(JClassType t) {
        List<JMethod> methods = getParsableElementMethods(t);
        if (methods != null) {
            for (JMethod method : methods) {
                ParsedElement elementAnnotation = method.getAnnotation(ParsedElement.class);
                if (elementAnnotation.type() == ParsedElement.Types.SYNC) {
                    try {
                        String contents = "";
                        URL url = getClass().getClassLoader().getResource(elementAnnotation.file());
                        BufferedReader reader = new BufferedReader(new InputStreamReader(url.openStream()));
                        String line = null;
                        while ((line = reader.readLine()) != null) {
                            contents += line;
                        }
                        reader.close();
                        ParsedElementDescriptor elementDescriptor = new ParsedElementDescriptor(method.getName(), contents.replaceAll("\"", "'"), elementAnnotation.type());
                        this.parsedElementList.add(elementDescriptor);
                    } catch (IOException e) {
                        e.printStackTrace();
                    }
                } else {
                    ParsedElementDescriptor elementDescriptor = new ParsedElementDescriptor(method.getName(), elementAnnotation.file(), elementAnnotation.type());
                    this.parsedElementList.add(elementDescriptor);
                }
            }
        }
    }

    private String doGenerate(String typeName) throws UnableToCompleteException {
        TypeOracle typeOracle = context.getTypeOracle();
        try {
            JClassType type = typeOracle.getType(typeName);
            String packageName = type.getPackage().getName();
            String simpleClassName = type.getSimpleSourceName();
            String className = simpleClassName + "Wrapper";
            String qualifiedBeanClassName = packageName + "." + className;
            SourceWriter sourceWriter = getSourceWriter(packageName, className, type);
            if (sourceWriter == null) {
                return qualifiedBeanClassName;
            }
            sourceWriter.println("private ElementParser generator = GWT.create (ElementParser.class);");
            sourceWriter.println("private XULParserServiceAsync service = GWT.create (XULParserService.class);");
            sourceWriter.println();
            for (ParsedElementDescriptor descriptor : parsedElementList) {
                if (descriptor.type == ParsedElement.Types.SYNC) {
                    sourceWriter.println("public Element " + descriptor.method + "() {");
                    sourceWriter.indent();
                    sourceWriter.println("return generator.parse (\"" + descriptor.file + "\");");
                    sourceWriter.outdent();
                    sourceWriter.println("}\n");
                } else {
                    sourceWriter.println("public void " + descriptor.method + "(AsyncCallback<Element> callback) {");
                    sourceWriter.indent();
                    sourceWriter.println("service.parse (\"" + descriptor.file + "\", callback);");
                    sourceWriter.outdent();
                    sourceWriter.println("}\n");
                }
            }
            sourceWriter.commit(logger);
            return qualifiedBeanClassName;
        } catch (Exception e) {
            logger.log(TreeLogger.ERROR, "Unable to generate code for " + typeName, e);
            throw new UnableToCompleteException();
        }
    }

    protected SourceWriter getSourceWriter(String packageName, String className, JClassType superType) {
        PrintWriter printWriter = context.tryCreate(logger, packageName, className);
        if (printWriter == null) {
            return null;
        }
        ClassSourceFileComposerFactory composerFactory = new ClassSourceFileComposerFactory(packageName, className);
        composerFactory.addImport(typename);
        composerFactory.addImport(Element.class.getName());
        composerFactory.addImport(GWT.class.getName());
        composerFactory.addImport(AsyncCallback.class.getName());
        composerFactory.addImport(ElementParser.class.getName());
        composerFactory.addImport(XULParserService.class.getName());
        composerFactory.addImport(XULParserServiceAsync.class.getName());
        composerFactory.addImplementedInterface(typename);
        return composerFactory.createSourceWriter(context, printWriter);
    }
}
